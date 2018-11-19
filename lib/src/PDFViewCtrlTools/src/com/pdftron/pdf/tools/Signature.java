package com.pdftron.pdf.tools;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.pdftron.common.Matrix2D;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementReader;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.Image;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFDraw;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageSet;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.Stamper;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.SignaturePickerDialog;
import com.pdftron.pdf.utils.SignaturePickerDialog.SignaturePickerDialogListener;
import com.pdftron.pdf.utils.StampManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;

import java.io.File;
import java.util.ArrayList;

/**
 * This class is for creating signature annotation.
 */
@Keep
public class Signature extends Tool implements
    SignaturePickerDialogListener {

    /**
     * Custom identifier added to the signature stamp added with this tool.
     */
    public static String SIGNATURE_ANNOTATION_ID = "pdftronSignatureStamp";

    private static String SIGNATURE_TEMP_FILE = "SignatureTempFile.jpg";

    /**
     * The signature field has no appearance set.
     */
    private static final int SIG_APPEARANCE_EMPTY = 0;
    /**
     * The signature field has paths as its appearance.
     */
    private static final int SIG_APPEARANCE_PATHS = 1;
    /**
     * The signature field has an image as its appearance.
     */
    private static final int SIG_APPEARANCE_IMAGE = 2;

    private PointF mTargetPoint = null;
    private int mTargetPageNum;

    private boolean mShouldSign;

    private Widget mWidget;

    private boolean mMenuBeingShown;

    private int mColor;
    private float mStrokeThickness;

    private boolean mShowDefaultSignature = true;
    private int mQuickMenuAnalyticType = AnalyticsHandlerAdapter.QUICK_MENU_TYPE_TOOL_SELECT;

    SignaturePickerDialog mSignaturePicker;

    /**
     * Class constructor
     */
    public Signature(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();

        mShouldSign = false;

        // Set signature color
        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        mColor = settings.getInt(getColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultColor(mPdfViewCtrl.getContext(), getCreateAnnotType()));
        mStrokeThickness = settings.getFloat(getThicknessKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultThickness(mPdfViewCtrl.getContext(), getCreateAnnotType()));
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.SIGNATURE;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mSignaturePicker != null) {
            mSignaturePicker.updateLayout();
        }
    }

    public boolean isShowDefaulSignature() {
        return mShowDefaultSignature;
    }

    public void setShowDefaulSignature(boolean showDefaulSignature) {
        this.mShowDefaultSignature = showDefaulSignature;
    }

    /**
     * The overload implementation of {@link Tool#onQuickMenuClicked(QuickMenuItem)}.
     */
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        if (super.onQuickMenuClicked(menuItem)) {
            return true;
        }

        mMenuBeingShown = false;
        safeSetNextToolMode();

        if (menuItem.getItemId() == R.id.qm_use_saved_sig) {
            Page page = StampManager.getInstance().getDefaultSignature(mPdfViewCtrl.getContext());
            if (mWidget != null) {
                addSignatureStampToWidget(page);
            } else {
                addSignatureStamp(page);
            }
            mTargetPoint = null;

        } else if (menuItem.getItemId() == R.id.qm_new_signature) {
            showSignaturePickerDialog();

        } else if (menuItem.getItemId() == R.id.qm_delete) {
            boolean shouldUnlock = false;
            try {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                raiseAnnotationPreRemoveEvent(mAnnot, mAnnotPageNum);
                mWidget.getSDFObj().erase("AP");
                mWidget.refreshAppearance();

                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                raiseAnnotationRemovedEvent(mAnnot, mAnnotPageNum);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }
            unsetAnnot();
        }
        return true;
    }

    /**
     * Gets the signature appearance type.
     * <p>
     * Note: this method read-locks the document internally.
     *
     * @return the signature appearance type. It can be one of the following:
     * <ul>
     * <li>{@link #SIG_APPEARANCE_EMPTY}</li>
     * <li>{@link #SIG_APPEARANCE_IMAGE}</li>
     * <li>{@link #SIG_APPEARANCE_PATHS}</li>
     * </ul>
     */
    private int getAnnotSignatureType() {
        int elementType = SIG_APPEARANCE_EMPTY;
        boolean shouldUnlockRead = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread safe.
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            Obj app = mAnnot.getAppearance();
            if (app != null) {
                ElementReader reader = new ElementReader();
                Element formElement = getFirstElementUsingReader(reader, app, Element.e_form);
                if (formElement != null) {
                    Obj o = formElement.getXObject();
                    Element element = getFirstElementUsingReader(reader, o, Element.e_path);
                    if (element != null) {
                        elementType = SIG_APPEARANCE_PATHS;
                    } else {
                        element = getFirstElementUsingReader(reader, o, Element.e_image);
                        if (element != null) {
                            elementType = SIG_APPEARANCE_IMAGE;
                        }
                    }
                }
            }

        } catch (Exception e) {
            elementType = SIG_APPEARANCE_EMPTY;
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        return elementType;
    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (mAnnot != null) { // when tap on signature form field
            if (mJustSwitchedFromAnotherTool) {
                mJustSwitchedFromAnotherTool = false;

                // At this point we know we are looking for the signature field.
                mWidget = null;
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    if (mAnnot.getType() == Annot.e_Widget) {
                        mWidget = new Widget(mAnnot);
                    }
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }

                if (mWidget != null) {
                    // Does the widget already have an appearance?
                    int annotSignatureType = getAnnotSignatureType();
                    if (annotSignatureType == SIG_APPEARANCE_IMAGE || annotSignatureType == SIG_APPEARANCE_PATHS) {
                        QuickMenu quickMenu = new QuickMenu(mPdfViewCtrl);
                        quickMenu.initMenuEntries(R.menu.annot_general);
                        mQuickMenuAnalyticType = AnalyticsHandlerAdapter.QUICK_MENU_TYPE_ANNOTATION_SELECT;
                        int x = (int) (e.getX() + 0.5);
                        int y = (int) (e.getY() + 0.5);
                        RectF anchor = new RectF(x - 5, y, x + 5, y + 1);
                        showMenu(anchor, quickMenu);
                        mMenuBeingShown = true;
                        return true;
                    }

                    // Does a custom signature already exist?
                    if (mShowDefaultSignature && StampManager.getInstance().hasDefaultSignature(mPdfViewCtrl.getContext())) {
                        // Show the menu for saved signature or new one.
                        QuickMenu quickMenu = new QuickMenu(mPdfViewCtrl);
                        quickMenu.initMenuEntries(R.menu.sig);
                        mQuickMenuAnalyticType = AnalyticsHandlerAdapter.QUICK_MENU_TYPE_TOOL_SELECT;
                        int x = (int) (e.getX() + 0.5);
                        int y = (int) (e.getY() + 0.5);
                        RectF anchor = new RectF(x - 5, y, x + 5, y + 1);
                        showMenu(anchor, quickMenu);
                        mMenuBeingShown = true;
                        return true;
                    } else {
                        // Just show the signature picker dialog.
                        showSignaturePickerDialog();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {

        // Deal with touches outside the quick menu when it is being shown.
        if (mMenuBeingShown) {
            safeSetNextToolMode();
            mTargetPoint = null;
            mMenuBeingShown = false;
            return true;
        }

        // If onUp() was fired due to a fling motion, return.
        if (mTargetPoint != null) {
            return false;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PINCH ||
            priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING ||
            priorEventMode == PDFViewCtrl.PriorEventMode.FLING) {
            return false;
        }

        // if tap on the same kind, select the annotation instead of create a new one
        boolean shouldCreate = true;
        int x = (int) e.getX();
        int y = (int) e.getY();
        ArrayList<Annot> annots = mPdfViewCtrl.getAnnotationListAt(x, y, x, y);
        int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);

        setCurrentDefaultToolModeHelper(getToolMode());

        try {
            for (Annot annot : annots) {
                if (annot.isValid() && annot.getType() == Annot.e_Stamp) {
                    shouldCreate = false;
                    // force ToolManager to select the annotation
                    ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                    toolManager.selectAnnot(annot, page);
                    break;
                }
            }
        } catch (PDFNetException ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }

        if (shouldCreate && page > 0) {
            createSignature(e.getX(), e.getY());
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds signature stamp to widget.
     *
     * @param page The page
     */
    protected void addSignatureStampToWidget(Page page) {
        PDFDraw pdfDraw = null;
        try {
            String sigTempFilePath = mPdfViewCtrl.getContext().getFilesDir().getAbsolutePath() + "/" + SIGNATURE_TEMP_FILE;

            Rect cropBox = page.getCropBox();
            int width = (int) cropBox.getWidth();
            int height = (int) cropBox.getHeight();

            pdfDraw = new PDFDraw();
            pdfDraw.setPageTransparent(true);
            pdfDraw.setImageSize(width, height, true);
            pdfDraw.export(page, sigTempFilePath, "jpeg");

            setImageAsAppearance(sigTempFilePath);

            File sigTempFile = new File(sigTempFilePath);
            if (sigTempFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                sigTempFile.delete();
            }

            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
            raiseAnnotationAddedEvent(mAnnot, mAnnotPageNum);
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (pdfDraw != null) {
                try {
                    pdfDraw.destroy();
                } catch (PDFNetException ignored) {
                }
            }
        }
        unsetAnnot();
    }

    private void setImageAsAppearance(String fullFileName) {
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            PDFDoc doc = mPdfViewCtrl.getDoc();

            // Add the signature appearance
            ElementWriter writer = new ElementWriter();
            ElementBuilder builder = new ElementBuilder();
            writer.begin(doc, true);

            Image sigImg = Image.create(doc, fullFileName);

            Rect widgetRect = mWidget.getRect();
            double widgetWidth = widgetRect.getWidth();
            double widgetHeight = widgetRect.getHeight();

            Matrix2D mtx = getSignatureMatrix(sigImg);

            Element element = builder.createImage(sigImg, mtx);
            writer.writePlacedElement(element);

            Obj obj = writer.end();
            obj.putRect("BBox", 0, 0, widgetWidth, widgetHeight);
            obj.putName("Subtype", "Form");
            obj.putName("Type", "XObject");
            writer.begin(doc);
            element = builder.createForm(obj);
            writer.writePlacedElement(element);
            obj = writer.end();
            obj.putRect("BBox", 0, 0, widgetWidth, widgetHeight);
            obj.putName("Subtype", "Form");
            obj.putName("Type", "XObject");

            mWidget.setAppearance(obj);
            mWidget.refreshAppearance();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private Matrix2D getSignatureMatrix(Image sigImg) throws PDFNetException {
        // TODO This should be moved to core
        double imageWidth = sigImg.getImageWidth(), imageHeight = sigImg.getImageHeight();

        int pageNum = mAnnotPageNum;
        if (pageNum < 1) {
            pageNum = mPdfViewCtrl.getCurrentPage();
        }
        int pageRotation = mPdfViewCtrl.getDoc().getPage(pageNum).getRotation();
        double imageRotWidth = imageWidth, imageRotHeight = imageHeight;
        if (pageRotation == Page.e_90 || pageRotation == Page.e_270) {
            // Swap the image width and height for purposes of most calculations
            // if the page is rotated since we want the image to look normally oriented to users
            //noinspection SuspiciousNameCombination
            imageRotHeight = imageWidth;
            //noinspection SuspiciousNameCombination
            imageRotWidth = imageHeight;
        }

        // Create the signature appearance

        Rect widgetRect = mWidget.getRect();
        double widgetWidth = widgetRect.getWidth();
        double widgetHeight = widgetRect.getHeight();

        double widthRatio = widgetWidth / imageRotWidth;      // The scale needed to fit width
        double heightRatio = widgetHeight / imageRotHeight;    // The scale needed to fit height

        // Since the smallest ratio represents the dimension that clamps the image size
        // we can use this as our factor in order to convert image dimensions to widget space
        double imageToWidgetScale = Math.min(widthRatio, heightRatio);

        // Now, we want to calculate the horizontal or vertical translation (we should only
        // need one of them). The image will be scaled by the smallest of the the ratios
        // between the widgets and images width or height
        // We calculate the scaling in page space, which is half of the width or height
        // difference between the widget and scaled image.
        double horzTranslate = (widgetWidth - (imageRotWidth * imageToWidgetScale)) / 2;
        double vertTranslate = (widgetHeight - (imageRotHeight * imageToWidgetScale)) / 2;

        // The matrix needed to center the image within the widget
        Matrix2D translation_center_matrix = new Matrix2D(1, 0, 0, 1, horzTranslate, vertTranslate);

        // for calculation of the rotation and scale matrices we use the original width and
        // height. This is because the rotation is applied to these values in our matrix order.
        double imageScaledWidth = imageWidth * imageToWidgetScale;
        double imageScaledHeight = imageHeight * imageToWidgetScale;
        Matrix2D rotationMtx = new Matrix2D(1, 0, 0, 1, 0, 0);
        if (pageRotation == Page.e_90) {
            rotationMtx = new Matrix2D(0, 1, -1, 0, imageScaledHeight, 0);
        } else if (pageRotation == Page.e_180) {
            rotationMtx = new Matrix2D(-1, 0, 0, -1, imageScaledWidth, imageScaledHeight);
        } else if (pageRotation == Page.e_270) {
            rotationMtx = new Matrix2D(0, -1, 1, 0, 0, imageScaledWidth);
        }
        Matrix2D scaleMtx = new Matrix2D(imageScaledWidth, 0, 0, imageScaledHeight, 0, 0);

        return translation_center_matrix.multiply(rotationMtx.multiply(scaleMtx));
    }

    private void showSignaturePickerDialog() {
        mNextToolMode = getToolMode();
        mSignaturePicker = new SignaturePickerDialog(mPdfViewCtrl.getContext(), Utils.getPostProcessedColor(mPdfViewCtrl, mColor), mStrokeThickness);
        Activity activity = ((ToolManager) mPdfViewCtrl.getToolManager()).getCurrentActivity();
        if (activity == null) {
            Log.e(Signature.class.getName(), "ToolManager is not attached to with an Activity");
            return;
        }
        mSignaturePicker.setOwnerActivity(activity);
        if (onInterceptDialogEvent(mSignaturePicker)) {
            return;
        }
        mSignaturePicker.setDialogListener(this);
        mSignaturePicker.setMakeDefaultCheckboxVisibility(mShowDefaultSignature ? View.VISIBLE : View.GONE);
        mSignaturePicker.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (mShouldSign) {
                    Page page = StampManager.getInstance().createSignature(mPdfViewCtrl.getContext(),
                        mSignaturePicker.getSignatureBoundingBox(),
                        mSignaturePicker.getPaths(),
                        mColor,
                        mStrokeThickness,
                        mSignaturePicker.shouldOverwriteOldSignature());

                    if (mWidget != null) {
                        addSignatureStampToWidget(page);
                    } else {
                        addSignatureStamp(page);
                    }
                    mShouldSign = false;
                }

                mTargetPoint = null;
                safeSetNextToolMode();
            }
        });
        mSignaturePicker.show();
        mSignaturePicker.setCanceledOnTouchOutside(false);
    }

    /**
     * The overload implementation of {@link SignaturePickerDialogListener#signatureDone(boolean)}.
     */
    @Override
    public void signatureDone(boolean shouldSign) {
        mShouldSign = shouldSign;
    }

    /**
     * The overload implementation of {@link SignaturePickerDialogListener#popupDismissed(AnnotStyleDialogFragment)}.
     *
     * @param popupWindow The popup window
     */
    @Override
    public void popupDismissed(AnnotStyleDialogFragment popupWindow) {
        ToolStyleConfig.getInstance().saveAnnotStyle(mPdfViewCtrl.getContext(), popupWindow.getAnnotStyle(), "");
        int color = popupWindow.getAnnotStyle().getColor();
        float thickness = popupWindow.getAnnotStyle().getThickness();
        editColor(color);
        editThickness(thickness);
    }

    private void editColor(int color) {
        mColor = color;

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(getColorKey(getCreateAnnotType()), color);
        editor.apply();
    }

    private void editThickness(float thickness) {
        mStrokeThickness = thickness;

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(getThicknessKey(getCreateAnnotType()), thickness);
        editor.apply();
    }

    private void addSignatureStamp(Page stampPage) {
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            PDFDoc doc = mPdfViewCtrl.getDoc();
            Rect stampRect = stampPage.getCropBox();
            Page page = doc.getPage(mTargetPageNum);
            Rect pageCropBox = page.getCropBox();
            Rect pageViewBox = page.getBox(mPdfViewCtrl.getPageBox());

            int viewRotation = mPdfViewCtrl.getPageRotation();
            int pageRotation = page.getRotation();

            // If the page itself is rotated, we want to "rotate" width and height as well
            double pageWidth = pageViewBox.getWidth();
            if (pageRotation == Page.e_90 || pageRotation == Page.e_270) {
                pageWidth = pageViewBox.getHeight();
            }
            double pageHeight = pageViewBox.getHeight();
            if (pageRotation == Page.e_90 || pageRotation == Page.e_270) {
                pageHeight = pageViewBox.getWidth();
            }

            double maxWidth = 200;
            double maxHeight = 200;

            if (pageWidth < maxWidth) {
                maxWidth = pageWidth;
            }
            if (pageHeight < maxHeight) {
                maxHeight = pageHeight;
            }
            double stampWidth = stampRect.getWidth();
            double stampHeight = stampRect.getHeight();

            // if the viewer rotates pages, we want to treat it as if it's the stamp that's rotated
            if (viewRotation == Page.e_90 || viewRotation == Page.e_270) {
                double temp = stampWidth;
                //noinspection SuspiciousNameCombination
                stampWidth = stampHeight;
                stampHeight = temp;
            }

            double scaleFactor = Math.min(maxWidth / stampWidth, maxHeight / stampHeight);
            stampWidth *= scaleFactor;
            stampHeight *= scaleFactor;

            Stamper stamper = new Stamper(Stamper.e_absolute_size, stampWidth, stampHeight);
            stamper.setAlignment(Stamper.e_horizontal_left, Stamper.e_vertical_bottom);
            stamper.setAsAnnotation(true);

            Matrix2D mtx = page.getDefaultMatrix();// This matrix takes into account page rotation and crop box
            Point pt = mtx.multPoint(mTargetPoint.x, mTargetPoint.y);

            double xPos = pt.x - (stampWidth / 2);
            double yPos = pt.y - (stampHeight / 2);

            // Note, stamper stamps relative to the CropBox,
            // i.e. (0, 0) is the bottom right corner of crop box.
            double leftEdge = pageViewBox.getX1() - pageCropBox.getX1();
            double bottomEdge = pageViewBox.getY1() - pageCropBox.getY1();

            if (xPos > leftEdge + pageWidth - stampWidth) {
                xPos = leftEdge + pageWidth - stampWidth;
            }
            if (xPos < leftEdge) {
                xPos = leftEdge;
            }

            if (yPos > bottomEdge + pageHeight - stampHeight) {
                yPos = bottomEdge + pageHeight - stampHeight;
            }
            if (yPos < bottomEdge) {
                yPos = bottomEdge;
            }

            stamper.setPosition(xPos, yPos);

            int stampRotation = (4 - viewRotation) % 4; // 0 = 0, 90 = 1; 180 = 2, and 270 = 3
            stamper.setRotation(stampRotation * 90.0);
            stamper.stampPage(doc, stampPage, new PageSet(mTargetPageNum));

            int numAnnots = page.getNumAnnots();
            Annot annot = page.getAnnot(numAnnots - 1);
            Obj obj = annot.getSDFObj();
            obj.putString(SIGNATURE_ANNOTATION_ID, "");

            buildAnnotBBox();

            mPdfViewCtrl.update(annot, mTargetPageNum);
            raiseAnnotationAddedEvent(annot, mTargetPageNum);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected Element getFirstElementUsingReader(ElementReader reader, Obj obj, int type) {
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            if (obj != null) {
                reader.begin(obj);
                try {
                    Element element;
                    while ((element = reader.next()) != null) {
                        if (element.getType() == type) {
                            return element;
                        }
                    }
                } finally {
                    reader.end();
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        return null;
    }

    /**
     * Sets the target point.
     *
     * @param point The target point
     */
    public void setTargetPoint(PointF point) {
        createSignature(point.x, point.y);

        safeSetNextToolMode();
    }

    private void createSignature(float ptx, float pty) {
        setCurrentDefaultToolModeHelper(getToolMode());
        // Gets the target point (in page space) and page.
        mTargetPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(ptx, pty);
        double[] pts = mPdfViewCtrl.convScreenPtToPagePt(ptx, pty, mTargetPageNum);
        mTargetPoint = new PointF();
        mTargetPoint.x = (float) pts[0];
        mTargetPoint.y = (float) pts[1];

        // Does a custom signature already exist?
        if (mShowDefaultSignature && StampManager.getInstance().hasDefaultSignature(mPdfViewCtrl.getContext())) {
            // Show the menu for saved signature or new one.
            QuickMenu quickMenu = new QuickMenu(mPdfViewCtrl);
            quickMenu.initMenuEntries(R.menu.sig);
            mQuickMenuAnalyticType = AnalyticsHandlerAdapter.QUICK_MENU_TYPE_TOOL_SELECT;
            int x = (int) (ptx + 0.5);
            int y = (int) (pty + 0.5);
            RectF anchor = new RectF(x - 5, y, x + 5, y + 1);
            showMenu(anchor, quickMenu);
            mMenuBeingShown = true;
        } else {
            // Just show the signature picker.
            showSignaturePickerDialog();
        }
    }

    private void safeSetNextToolMode() {
        if (mForceSameNextToolMode) {
            mNextToolMode = mCurrentDefaultToolMode;
        } else {
            if (mSignaturePicker != null && mSignaturePicker.isShowing()) {
                mNextToolMode = getToolMode();
            } else {
                mNextToolMode = ToolMode.PAN;
            }
        }
    }

    @Override
    protected int getQuickMenuAnalyticType() {
        return mQuickMenuAnalyticType;
    }

    @Override
    public void onChangeAnnotThickness(float thickness, boolean done) {
        if (done) {
            editThickness(thickness);
        }
    }

    @Override
    public void onChangeAnnotTextSize(float textSize, boolean done) {

    }

    @Override
    public void onChangeAnnotTextColor(int textColor) {

    }

    @Override
    public void onChangeAnnotOpacity(float opacity, boolean done) {

    }

    @Override
    public void onChangeAnnotStrokeColor(int color) {
        editColor(color);
    }

    @Override
    public void onChangeAnnotFillColor(int color) {

    }

    @Override
    public void onChangeAnnotIcon(String icon) {

    }

    @Override
    public void onChangeAnnotFont(FontResource font) {

    }

    @Override
    public void onChangeRulerProperty(RulerItem rulerItem) {

    }
}
