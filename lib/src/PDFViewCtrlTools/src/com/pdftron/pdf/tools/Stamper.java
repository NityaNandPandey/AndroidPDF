//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.net.Uri;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import com.pdftron.common.Matrix2D;
import com.pdftron.common.PDFNetException;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Image;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageSet;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.ObjSet;

import java.util.ArrayList;

/**
 * This class is for creating stamp annotation.
 */
@Keep
public class Stamper extends Tool {

    public final static String STAMPER_ROTATION_ID = "pdftronImageStampRotation";
    private final static String IMAGE_STAMPER_MOST_RECENTLY_USED_FILES = "image_stamper_most_recently_used_files";

    @SuppressWarnings("WeakerAccess")
    protected PointF mTargetPoint;

    /**
     * Class constructor
     */
    public Stamper(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.STAMPER;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Stamp;
    }

    /**
     * The overload implementation of {@link Tool#onCreate()}.
     */
    @Override
    public void onCreate() {

    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (mAnnot != null) {
            mTargetPoint = new PointF(e.getX(), e.getY());
            addStamp();
        }
        return true;
    }

    /**
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
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
            mTargetPoint = new PointF(e.getX(), e.getY());
            addStamp();
            return true;
        } else {
            return false;
        }
    }

    /**
     * The overload implementation of {@link Tool#onQuickMenuClicked(QuickMenuItem)}.
     */
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        if (super.onQuickMenuClicked(menuItem)) {
            return true;
        }
        if (menuItem.getItemId() == R.id.qm_image_stamper) {
            addStamp();
        }
        return true;
    }

    /**
     * Sets the target point.
     *
     * @param targetPoint      The target point
     * @param createImageStamp True if should create image stamp
     */
    public void setTargetPoint(PointF targetPoint, boolean createImageStamp) {
        mTargetPoint = targetPoint;

        if (createImageStamp) {
            addStamp();
        }

        safeSetNextToolMode();
    }

    /**
     * Called when user attempts to add a stamp at {@code mTargetPoint}.
     */
    protected void addStamp() {

        if (mTargetPoint == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(
                new Exception("target point is not specified."));
            return;
        }

        ((ToolManager) (mPdfViewCtrl.getToolManager())).onImageStamperSelected(mTargetPoint);

    }

    /**
     * adds image stamp from clipboard in the specific location.
     *
     * @param targetPoint The target point to add image stamp
     */
    public void addStampFromClipboard(@Nullable PointF targetPoint) {
        if (targetPoint == null) {
            // TODO set target point as the center of current view
            return;
        }
        mTargetPoint = targetPoint;

        if (mPdfViewCtrl == null || mPdfViewCtrl.getContext() == null) {
            return;
        }

        Context context = mPdfViewCtrl.getContext();
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ContentResolver cr = context.getContentResolver();
        if (clipboard == null || cr == null) {
            return;
        }
        ClipData clip = clipboard.getPrimaryClip();

        if (clip != null) {
            ClipData.Item item = clip.getItemAt(0);
            Uri pasteUri = item.getUri();
            if (Utils.isImageFile(cr, pasteUri)) {
                try {
                    createImageStamp(pasteUri, 0, null);
                } catch (SecurityException ignored) {

                }
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#clearTargetPoint()}.
     */
    @Override
    public void clearTargetPoint() {
        mTargetPoint = null;
    }

    /**
     * Creates an image stamp.
     *
     * @param uri           The URI
     * @param imageRotation The image rotation (e.g. 0, 90, 180, 270)
     * @param filePath      The file path
     * @return True if an image stamp is created
     */
    @SuppressWarnings("SuspiciousNameCombination")
    public boolean createImageStamp(Uri uri, int imageRotation, String filePath) {
        boolean shouldUnlock = false;
        SecondaryFileFilter filter = null;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            PDFDoc doc = mPdfViewCtrl.getDoc();

            // Another method to compress the image - resample the bitmap
           /* ByteArrayOutputStream out = new ByteArrayOutputStream();
            final BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inDensity = 10;
            bitmapOptions.inTargetDensity = 1;

            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
            Bitmap decoded = BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()), null, bitmapOptions);
            decoded.setDensity(Bitmap.DENSITY_NONE);
            decoded = Bitmap.createBitmap(decoded);

            Image img = Image.create(doc.getSDFDoc(), decoded);
            */

            // create a filter to pass the image into core
            filter = new SecondaryFileFilter(mPdfViewCtrl.getContext(), uri);

            // set encoder hints
            ObjSet hintSet = new ObjSet();
            Obj encoderHints = hintSet.createArray();
            encoderHints.pushBackName("JPEG");
            encoderHints.pushBackName("Quality");
            encoderHints.pushBackNumber(85);

            // create an image
            Image img = Image.create(doc.getSDFDoc(), filter, encoderHints);

            int pageNum;
            if (mTargetPoint != null) {
                pageNum = mPdfViewCtrl.getPageNumberFromScreenPt(mTargetPoint.x, mTargetPoint.y);
                if (pageNum <= 0) {
                    pageNum = mPdfViewCtrl.getCurrentPage();
                }
            } else {
                pageNum = mPdfViewCtrl.getCurrentPage();
            }

            if (pageNum <= 0) {
                return false;
            }

            /////////////////   set stamp size    ////////////////
            Page page = doc.getPage(pageNum);
            int viewRotation = mPdfViewCtrl.getPageRotation();

            Rect pageViewBox = page.getBox(mPdfViewCtrl.getPageBox());
            Rect pageCropBox = page.getCropBox();
            int pageRotation = page.getRotation();

            // get screen height and width
            android.graphics.Point size = new android.graphics.Point();
            Utils.getDisplaySize(mPdfViewCtrl.getContext(), size);
            int screenWidth = size.x < size.y ? size.x : size.y;
            int screenHeight = size.x < size.y ? size.y : size.x;

            // calculate the max image size based off of screen width and height
            double maxImageHeightPixels = screenHeight * 0.25;
            double maxImageWidthPixels = screenWidth * 0.25;

            // convert the max image size in pixels to page units
            double[] point1 = mPdfViewCtrl.convScreenPtToPagePt(0, 0, pageNum);
            double[] point2 = mPdfViewCtrl.convScreenPtToPagePt(20, 20, pageNum);

            double pixelsToPageRatio = Math.abs(point1[0] - point2[0]) / 20;
            double maxImageHeightPage = maxImageHeightPixels * pixelsToPageRatio;
            double maxImageWidthPage = maxImageWidthPixels * pixelsToPageRatio;

            // scale stamp
            double stampWidth = img.getImageWidth();
            double stampHeight = img.getImageHeight();
            if (imageRotation == 90 || imageRotation == 270) {
                double temp = stampWidth;
                stampWidth = stampHeight;
                stampHeight = temp;
            }

            double pageWidth = pageViewBox.getWidth();
            double pageHeight = pageViewBox.getHeight();
            if (pageRotation == Page.e_90 || pageRotation == Page.e_270) {
                double temp = pageWidth;
                pageWidth = pageHeight;
                pageHeight = temp;
            }

            // if page width or height is smaller than the desired image size,
            // set desired image size to the page width or height
            if (pageWidth < maxImageWidthPage) {
                maxImageWidthPage = pageWidth;
            }
            if (pageHeight < maxImageHeightPage) {
                maxImageHeightPage = pageHeight;
            }

            double scaleFactor = Math.min(maxImageWidthPage / stampWidth, maxImageHeightPage / stampHeight);
            stampWidth *= scaleFactor;
            stampHeight *= scaleFactor;

            // Stamp width and height are relative to the view rotation, not screen rotation
            if (viewRotation == Page.e_90 || viewRotation == Page.e_270) {
                double temp = stampWidth;
                stampWidth = stampHeight;
                stampHeight = temp;
            }

            com.pdftron.pdf.Stamper stamper = new com.pdftron.pdf.Stamper(com.pdftron.pdf.Stamper.e_absolute_size, stampWidth, stampHeight);

            //////////////////   set stamp position   //////////////////////
            if (mTargetPoint != null) {
                // get target point in page coordinates
                double[] pageTarget = mPdfViewCtrl.convScreenPtToPagePt(mTargetPoint.x, mTargetPoint.y, pageNum);
                Matrix2D mtx = page.getDefaultMatrix();
                Point pageTargetPoint = mtx.multPoint(pageTarget[0], pageTarget[1]);

                // set position to be relative to bottom-left hand corner of document
                stamper.setAlignment(com.pdftron.pdf.Stamper.e_horizontal_left, com.pdftron.pdf.Stamper.e_vertical_bottom);

                // move the page target so that the middle of the image aligns with the
                // location tapped by the user.
                pageTargetPoint.x = pageTargetPoint.x - (stampWidth / 2);
                pageTargetPoint.y = pageTargetPoint.y - (stampHeight / 2);

                // get page height and width to determine if entire image will fit on page
                // if not, move image appropriately so that it will
                double leftEdge = pageViewBox.getX1() - pageCropBox.getX1();
                double bottomEdge = pageViewBox.getY1() - pageCropBox.getY1();
                if (pageTargetPoint.x > leftEdge + pageWidth - stampWidth) {
                    pageTargetPoint.x = leftEdge + pageWidth - stampWidth;
                }
                if (pageTargetPoint.x < leftEdge) {
                    pageTargetPoint.x = leftEdge;
                }

                if (pageTargetPoint.y > bottomEdge + pageHeight - stampHeight) {
                    pageTargetPoint.y = bottomEdge + pageHeight - stampHeight;
                }
                if (pageTargetPoint.y < bottomEdge) {
                    pageTargetPoint.y = bottomEdge;
                }

                stamper.setPosition(pageTargetPoint.x, pageTargetPoint.y);
            } else {
                // stamp image in middle of screen if target point returns null
                stamper.setPosition(0, 0);
            }

            // set as annotation
            stamper.setAsAnnotation(true);

            // set image rotation
            int stampRotation = (4 - viewRotation) % 4; // 0 = 0, 90 = 1; 180 = 2, and 270 = 3
            stamper.setRotation(stampRotation * 90.0 + imageRotation);

            // stamp image
            stamper.stampImage(doc, img, new PageSet(pageNum));

            // update PDF to show stamp
            int numAnnots = page.getNumAnnots();
            Annot annot = page.getAnnot(numAnnots - 1);
            Obj obj = annot.getSDFObj();
            obj.putNumber(com.pdftron.pdf.tools.Stamper.STAMPER_ROTATION_ID, 0);

            buildAnnotBBox();

            // Note: we don't select the annotation after finished with stamper
            // creation, so shouldn't set mAnnot

            mPdfViewCtrl.update(annot, pageNum);
            raiseAnnotationAddedEvent(annot, pageNum);

            // send analytics about the file chosen
            // if it is a "recently used" file, send event and if not,
            // save file to "recently used" file list. A "recently used"
            // file is determined as the last 6 files used.
            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            String recentlyUsedFiles = settings.getString(IMAGE_STAMPER_MOST_RECENTLY_USED_FILES, "");
            String[] recentlyUsedFilesArray = recentlyUsedFiles.split(" ");
            Boolean recentlyUsed = false;

            if (filePath != null) {
                for (String recentlyUsedFile : recentlyUsedFilesArray) {
                    if (filePath.equals(recentlyUsedFile)) {
                        recentlyUsed = true;
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_QUICKTOOL, "stamper recent file");
                    }
                }

                if (!recentlyUsed) {
                    int length = recentlyUsedFilesArray.length < 6 ? recentlyUsedFilesArray.length : 5;
                    StringBuilder str = new StringBuilder(filePath);
                    for (int i = 0; i < length; i++) {
                        str.append(" ").append(recentlyUsedFilesArray[i]);
                    }

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(IMAGE_STAMPER_MOST_RECENTLY_USED_FILES, str.toString());
                    editor.apply();
                }
            }
            return true;
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return false;
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
            Utils.closeQuietly(filter);
            // reset target point
            mTargetPoint = null;
            safeSetNextToolMode();
        }
    }

    /**
     * Safely sets the next tool mode.
     */
    @SuppressWarnings("WeakerAccess")
    protected void safeSetNextToolMode() {
        if (mForceSameNextToolMode) {
            mNextToolMode = mCurrentDefaultToolMode;
        } else {
            mNextToolMode = ToolMode.PAN;
        }
    }
}
