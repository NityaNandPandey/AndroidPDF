
//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementReader;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.FreeTextCacheStruct;
import com.pdftron.pdf.model.FreeTextInfo;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.InlineEditText;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.AutoScrollEditText;
import com.pdftron.sdf.Obj;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * A tool for creating free text annotation
 */
@Keep
public class FreeTextCreate extends Tool implements DialogAnnotNote.DialogAnnotNoteListener,
    InlineEditText.InlineEditTextListener, TextWatcher {
    private static final String TAG = FreeTextCreate.class.getName();

    final private static int THRESHOLD_FROM_PAGE_EDGE = 3; // in page space
    final private static int MINIMUM_BBOX_WIDTH = 50; // in page space

    protected PointF mTargetPointCanvasSpace;
    protected com.pdftron.pdf.Point mTargetPointPageSpace;
    protected int mPageNum;
    private int mTextColor;
    private float mTextSize;

    private int mStrokeColor;
    private float mThickness;
    private int mFillColor;
    private float mOpacity;
    private String mPDFTronFontName;
    private int mCurrentEditMode;
    private boolean mUpdateEditMode;

    private InlineEditText mInlineEditText;
    private int mAnnotButtonPressed;
    private long mStoredTimeStamp;
    protected boolean mOnUpOccured;
    private float mStoredPointX = 0;
    private float mStoredPointY = 0;
    private String mCacheFileName;
    private DialogFreeTextNote mDialogFreeTextNote;

    /**
     * Class constructor
     */
    public FreeTextCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();
        mTargetPointCanvasSpace = new PointF(0, 0);
        mTargetPointPageSpace = new com.pdftron.pdf.Point(0, 0);
        mStoredTimeStamp = System.currentTimeMillis();

        mCacheFileName = ((ToolManager) ctrl.getToolManager()).getFreeTextCacheFileName();
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.TEXT_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_FreeText;
    }

    /**
     * The overload implementation of {@link Tool#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return true;
    }

    /**
     * The overload implementation of {@link Tool#setupAnnotProperty(int, float, float, int, String, String)}.
     */
    @Override
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdftronFontName, int textColor, float textSize) {
        mStrokeColor = color;
        mThickness = thickness;
        mTextColor = textColor;
        mTextSize = (int) textSize;
        mOpacity = opacity;
        mFillColor = fillColor;
        mPDFTronFontName = pdftronFontName;

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt(getTextColorKey(getCreateAnnotType()), mTextColor);
        editor.putFloat(getOpacityKey(getCreateAnnotType()), mOpacity);
        editor.putFloat(getTextSizeKey(getCreateAnnotType()), mTextSize);
        editor.putInt(getColorFillKey(getCreateAnnotType()), mFillColor);
        editor.putInt(getColorKey(getCreateAnnotType()), mStrokeColor);
        editor.putFloat(getThicknessKey(getCreateAnnotType()), mThickness);
        editor.putString(getFontKey(getCreateAnnotType()), mPDFTronFontName);

        editor.apply();
    }

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);
        if (mInlineEditText == null || !mInlineEditText.isEditing()) {
            initTextStyle();
            mAnnotPushedBack = false;
            setTargetPoints(new PointF(e.getX(), e.getY()));
        }
        mOnUpOccured = false;

        return super.onDown(e);
    }

    /**
     * The overload implementation of {@link Tool#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        //noinspection RedundantIfStatement
        if (mAllowTwoFingerScroll) {
            return false;
        }

        // Avoid scrolling the page.
        return true;
    }

    /**
     * The overload implementation of {@link Tool#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        // hide edit text during scaling
        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            saveAndQuitInlineEditText(true);
        }
        return super.onScaleBegin(x, y);
    }

    /**
     * The overload implementation of {@link Tool#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        if (mAllowTwoFingerScroll) {
            mAllowTwoFingerScroll = false;
        }
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        // With a fling motion, onUp is called twice. We want
        // to ignore the second call
        if (mOnUpOccured) {
            return false;
        }
        mOnUpOccured = true;

        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            saveAndQuitInlineEditText(false);
            return false;
        }

        // We are scrolling
        if (mAllowTwoFingerScroll) {
            mAllowTwoFingerScroll = false;
            return false;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PAGE_SLIDING) {
            return false;
        }

        // If we are just up from fling or pinch, do not add new note
        if (priorEventMode == PDFViewCtrl.PriorEventMode.FLING ||
            priorEventMode == PDFViewCtrl.PriorEventMode.PINCH) {
            // don't let scroll the page
            return true;
        }

        // If annotation was already pushed back, avoid re-entry due to fling motion
        // but allow when creating multiple strokes.
        if (mAnnotPushedBack && mForceSameNextToolMode) {
            return true;
        }

        setTargetPoints(new PointF(e.getX(), e.getY()));
        mStoredPointX = e.getX();
        mStoredPointY = e.getY();
        if (mPageNum >= 1) {
            // prevents creating annotation outside of page bounds

            // if tap on the same kind, select the annotation instead of create a new one
            boolean shouldCreate = true;
            int x = (int) e.getX();
            int y = (int) e.getY();
            ArrayList<Annot> annots = mPdfViewCtrl.getAnnotationListAt(x, y, x, y);
            int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
            try {
                for (Annot annot : annots) {
                    if (annot.getType() == Annot.e_FreeText) {
                        shouldCreate = false;
                        // force ToolManager to select the annotation
                        setCurrentDefaultToolModeHelper(getToolMode());
                        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                        toolManager.selectAnnot(annot, page);
                        break;
                    }
                }
            } catch (PDFNetException ex) {
                ex.printStackTrace();
            }

            if (shouldCreate) {
                createFreeText();
                return true;
            }
        }

        return false;
    }

    private static com.pdftron.pdf.Rect getRectUnion(com.pdftron.pdf.Rect rect1, com.pdftron.pdf.Rect rect2) {
        com.pdftron.pdf.Rect rectUnion = null;
        try {
            rectUnion = new com.pdftron.pdf.Rect();
            rectUnion.setX1(Math.min(rect1.getX1(), rect2.getX1()));
            rectUnion.setY1(Math.min(rect1.getY1(), rect2.getY1()));
            rectUnion.setX2(Math.max(rect1.getX2(), rect2.getX2()));
            rectUnion.setY2(Math.max(rect1.getY2(), rect2.getY2()));
        } catch (PDFNetException e) {
            e.printStackTrace();
        }

        return rectUnion;
    }

    public static com.pdftron.pdf.Rect getTextBBoxOnPage(PDFViewCtrl pdfViewCtrl, int pageNum, com.pdftron.pdf.Point targetPoint) {
        try {
            Page page = pdfViewCtrl.getDoc().getPage(pageNum);
            com.pdftron.pdf.Rect pageCropOnClientF = page.getBox(pdfViewCtrl.getPageBox());
            pageCropOnClientF.normalize();

            // point is somehow outside of the page bounding box
            if (targetPoint.x < pageCropOnClientF.getX1()) {
                targetPoint.x = (float) pageCropOnClientF.getX1();
            }
            if (targetPoint.y < pageCropOnClientF.getY1()) {
                targetPoint.y = (float) pageCropOnClientF.getY1();
            }
            if (targetPoint.x > pageCropOnClientF.getX2()) {
                targetPoint.x = (float) pageCropOnClientF.getX2();
            }
            if (targetPoint.y > pageCropOnClientF.getY2()) {
                targetPoint.y = (float) pageCropOnClientF.getY2();
            }

            // determine what the bounds are based on the rotation
            int pageRotation = pdfViewCtrl.getDoc().getPage(pageNum).getRotation();
            int viewRotation = pdfViewCtrl.getPageRotation();
            int annotRotation = ((pageRotation + viewRotation) % 4) * 90;

            double left, top, right, bottom;
            left = targetPoint.x;
            top = targetPoint.y;
            if (annotRotation == 0) {
                right = pageCropOnClientF.getX2();
                bottom = pageCropOnClientF.getY1();
            } else if (annotRotation == 90) {
                right = pageCropOnClientF.getX2();
                bottom = pageCropOnClientF.getY2();
            } else if (annotRotation == 180) {
                right = pageCropOnClientF.getX1();
                bottom = pageCropOnClientF.getY2();
            } else {
                right = pageCropOnClientF.getX1();
                bottom = pageCropOnClientF.getY1();
            }

            if (Math.abs(right - left) < THRESHOLD_FROM_PAGE_EDGE) {
                left = right - THRESHOLD_FROM_PAGE_EDGE;
            }
            if (Math.abs(top - bottom) < THRESHOLD_FROM_PAGE_EDGE) {
                top = bottom + THRESHOLD_FROM_PAGE_EDGE;
            }

            com.pdftron.pdf.Rect rect = new com.pdftron.pdf.Rect(left, top, right, bottom);
            rect.normalize();
            return rect;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Initializes the free text.
     *
     * @param point The new target point
     */
    @SuppressWarnings("WeakerAccess")
    public void initFreeText(PointF point) {
        mAnnotPushedBack = false;
        initTextStyle();
        setTargetPoints(point);
        createFreeText();
    }

    /**
     * The overload implementation of {@link Tool#onClose()}.
     */
    @Override
    public void onClose() {
        super.onClose();

        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            saveAndQuitInlineEditText(false);
        }

        if (mDialogFreeTextNote != null && mDialogFreeTextNote.isShowing()) {
            // force to save the content
            mAnnotButtonPressed = DialogInterface.BUTTON_POSITIVE;
            prepareDialogFreeTextNoteDismiss();
            mDialogFreeTextNote.dismiss();
        }

        unsetAnnot();

        // hide soft keyboard
        InputMethodManager imm = (InputMethodManager) mPdfViewCtrl.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mPdfViewCtrl.getRootView().getWindowToken(), 0);
        }
    }

    /**
     * The overload implementation of {@link DialogAnnotNote.DialogAnnotNoteListener#onAnnotButtonPressed(int)}.
     */
    @Override
    public void onAnnotButtonPressed(int button) {
        mAnnotButtonPressed = button;
    }

    protected void createFreeText() {
        try {
            mAnnotPushedBack = true;

            if (mPageNum < 1) {
                mPageNum = mPdfViewCtrl.getCurrentPage();
            }

            // get last used free text editing preference and use that for creating the annot
            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            mCurrentEditMode = settings.getInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, ANNOTATION_FREE_TEXT_PREFERENCE_EDITING_DEFAULT);
            String cacheStr = null;
            if (Utils.cacheFileExists(mPdfViewCtrl.getContext(), mCacheFileName)) {
                JSONObject cacheJson = Utils.retrieveToolCache(mPdfViewCtrl.getContext(), mCacheFileName);
                if (cacheJson != null) {
                    cacheStr = cacheJson.getString("contents");
                }
            }
            // if phone in landscape mode, the default Android full screen keyboard will appear, so use the popup method
            if (!Utils.isTablet(mPdfViewCtrl.getContext()) &&
                mPdfViewCtrl.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                fallbackDialog(cacheStr, true);
            } else if (mCurrentEditMode == ANNOTATION_FREE_TEXT_PREFERENCE_DIALOG) {
                fallbackDialog(cacheStr, false);
            } else {
                inlineTextEditing(cacheStr);
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex, "createFreeText");
        }
    }

    /**
     * The overload implementation of {@link InlineEditText.InlineEditTextListener#getInlineEditTextPosition()}.
     */
    @Override
    public RectF getInlineEditTextPosition() {
        // position edit text such that the upper left corner (upper right
        // corner if RTL) aligns with where the user tapped and the lower right
        // corner (lower left corner if RTL) is the bottom right corner of the
        // page or PDFView, whichever is smaller.
        int left, top, right, bottom;
        left = right = Math.round(mTargetPointCanvasSpace.x); // points contain PDFView Scroll
        top = Math.round(mTargetPointCanvasSpace.y);

        // get PDFView's height and width
        int viewBottom = mPdfViewCtrl.getHeight() + mPdfViewCtrl.getScrollY();
        int viewRight = mPdfViewCtrl.getWidth() + mPdfViewCtrl.getScrollX();
        int viewLeft = mPdfViewCtrl.getScrollX();

        // get page right and bottom edge
        RectF pageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mPageNum);
        if (pageCropOnClientF != null) {
            int pageRight = Math.round(pageCropOnClientF.right);
            int pageLeft = Math.round(pageCropOnClientF.left);
            int pageBottom = Math.round(pageCropOnClientF.bottom);

            if (mPdfViewCtrl.getRightToLeftLanguage()) {
                left = viewLeft > pageLeft ? viewLeft : pageLeft;
            } else {
                right = viewRight < pageRight ? viewRight : pageRight;
            }
            bottom = viewBottom < pageBottom ? viewBottom : pageBottom;

        } else {
            // if we can't get the page bottom and right/left edge, we default
            // to using the pdfView's bottom and right/left edge
            if (mPdfViewCtrl.getRightToLeftLanguage()) {
                left = viewLeft;
            } else {
                right = viewRight;
            }
            bottom = viewBottom;
        }

        return new RectF(left, top, right, bottom);
    }

    /**
     * The overload implementation of {@link Tool#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            saveAndQuitInlineEditText(false);
        }
    }

    /**
     * The overload implementation of {@link InlineEditText.InlineEditTextListener#toggleToFreeTextDialog(String)}.
     */
    @Override
    public void toggleToFreeTextDialog(String interimText) {
        mCurrentEditMode = ANNOTATION_FREE_TEXT_PREFERENCE_DIALOG;
        mUpdateEditMode = true;
        fallbackDialog(interimText, false);
        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            mInlineEditText.getEditText().setCursorVisible(false);
        }
    }

    private void setNextToolMode() {
        if (mAnnot != null && (((ToolManager) mPdfViewCtrl.getToolManager()).isAutoSelectAnnotation() || !mForceSameNextToolMode)) {
            mNextToolMode = ToolMode.ANNOT_EDIT;
            setCurrentDefaultToolModeHelper(getToolMode());
        } else if (mForceSameNextToolMode) {
            mNextToolMode = getToolMode();
        } else {
            mNextToolMode = ToolMode.PAN;
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, null);
            toolManager.setTool(tool);
        }
    }

    private void setTargetPoints(PointF point) {
        mPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(point.x, point.y);

        // set the target point in both canvas and page space
        mTargetPointCanvasSpace.x = point.x + mPdfViewCtrl.getScrollX();
        mTargetPointCanvasSpace.y = point.y + mPdfViewCtrl.getScrollY();

        double[] pagePt = mPdfViewCtrl.convScreenPtToPagePt(point.x, point.y, mPageNum);
        mTargetPointPageSpace = new com.pdftron.pdf.Point((int) pagePt[0], (int) pagePt[1]);

        mStoredPointX = point.x;
        mStoredPointY = point.y;
    }

    /**
     * The overload implementation of {@link Tool#onPageTurning(int, int)}.
     */
    @Override
    public void onPageTurning(int old_page, int cur_page) {
        super.onPageTurning(old_page, cur_page);
        saveAndQuitInlineEditText(false);
    }

    protected void saveAndQuitInlineEditText(boolean immediateEditTextRemoval) {
        if (mInlineEditText != null) {
            String text = mInlineEditText.getContents();
            if (!TextUtils.isEmpty(text)) {
                boolean shouldUnlock = false;
                try {
                    mPdfViewCtrl.docLock(true);
                    shouldUnlock = true;
                    mInlineEditText.close(immediateEditTextRemoval);
                    createAnnot(text);
                    raiseAnnotationAddedEvent(mAnnot, mAnnotPageNum);
                    Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);

                    if (!immediateEditTextRemoval) {
                        addOldTools();
                    }
                } catch (Exception e) {
                    ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(e.getMessage());
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                    mInlineEditText.removeView();
                } finally {
                    if (shouldUnlock) {
                        mPdfViewCtrl.docUnlock();
                    }

                    if (immediateEditTextRemoval) {
                        mInlineEditText = null;
                    }
                    // set the tool mode
                    setNextToolMode();
                }
            } else {
                quitInlineEditText();
            }
        }

        // save new edit mode in settings
        if (mUpdateEditMode) {
            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, mCurrentEditMode);
            editor.apply();
        }
    }

    private void quitInlineEditText() {
        mInlineEditText.close(true);
        mInlineEditText = null;

        // reset the tool mode
        setNextToolMode();
    }

    /**
     * @return True if it is inline edit text and
     */
    protected boolean isFreeTextEditing() {
        if (mInlineEditText != null) {
            return mInlineEditText.isEditing();
        }
        return false;
    }

    private void inlineTextEditing(String interimText) {
        // override next tool mode
        setNextToolModeHelper(ToolMode.PAN);
        mNextToolMode = getToolMode();
        // reset onUp here because we will call onUp in inline box later
        mOnUpOccured = false;

        // create inline edit text
        mInlineEditText = new InlineEditText(mPdfViewCtrl, null, mPageNum, mTargetPointPageSpace, this);
        mInlineEditText.addTextWatcher(this);
        // change style of text to match style of free text annot
        // font size
        mInlineEditText.setTextSize((int) mTextSize);
        // keyboard shortcut
        mInlineEditText.getEditText().setAutoScrollEditTextListener(new AutoScrollEditText.AutoScrollEditTextListener() {
            @Override
            public boolean onKeyUp(int keyCode, KeyEvent event) {
                if (ShortcutHelper.isCommitText(keyCode, event)) {
                    // if DialogFreeTextNote is open then it swallows keys event, hence we only handle
                    // inline edit
                    saveAndQuitInlineEditText(false);
                    // hide soft keyboard
                    InputMethodManager imm = (InputMethodManager) mPdfViewCtrl.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(mPdfViewCtrl.getRootView().getWindowToken(), 0);
                    }
                }
                return true;
            }
        });

        // font color
        int textColor = Utils.getPostProcessedColor(mPdfViewCtrl, mTextColor);
        int r = Color.red(textColor);
        int g = Color.green(textColor);
        int b = Color.blue(textColor);
        int opacity = (int) (mOpacity * 0xFF);
        int fontColor = Color.argb(opacity, r, g, b);
        mInlineEditText.setTextColor(fontColor);

        // background color
        mInlineEditText.setBackgroundColor(Color.TRANSPARENT);

        // set contents if necessary
        if (interimText != null) {
            mInlineEditText.setContents(interimText);
        }
    }

    private void fallbackDialog(String interimText, boolean disableToggleButton) {
        boolean enableSave = true;
        if (interimText == null) {
            interimText = "";
            enableSave = false;
        }

        mDialogFreeTextNote = new DialogFreeTextNote(mPdfViewCtrl, interimText, enableSave);
        mDialogFreeTextNote.addTextWatcher(this);
        mDialogFreeTextNote.setAnnotNoteListener(this);
        mDialogFreeTextNote.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                prepareDialogFreeTextNoteDismiss();
            }
        });

        mDialogFreeTextNote.show();
        if (disableToggleButton) {
            mDialogFreeTextNote.disableToggleButton();
        }
    }

    private void prepareDialogFreeTextNoteDismiss() {
        if (mPdfViewCtrl == null || mDialogFreeTextNote == null) {
            return;
        }

        if (mAnnotButtonPressed == DialogInterface.BUTTON_POSITIVE) {
            boolean shouldUnlock = false;
            try {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                if (!TextUtils.isEmpty(mDialogFreeTextNote.getNote())) {
                    createAnnot(mDialogFreeTextNote.getNote());
                    raiseAnnotationAddedEvent(mAnnot, mAnnotPageNum);
                    Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);
                }

            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }

            if (mInlineEditText != null && mInlineEditText.isEditing()) {
                quitInlineEditText();
            } else {
                // set the next tool mode
                setNextToolMode();
            }

            // save new edit mode in settings
            if (mUpdateEditMode) {
                SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, mCurrentEditMode);
                editor.apply();
            }
        } else if (mAnnotButtonPressed == DialogInterface.BUTTON_NEUTRAL) {
            mCurrentEditMode = ANNOTATION_FREE_TEXT_PREFERENCE_INLINE;
            mUpdateEditMode = true;
            if (mInlineEditText != null) {
                mInlineEditText.setContents(mDialogFreeTextNote.getNote());

                // show keyboard
                Utils.showSoftKeyboard(mPdfViewCtrl.getContext(), mInlineEditText.getEditText());
            } else {
                inlineTextEditing(mDialogFreeTextNote.getNote());
            }
        } else {
            if (mInlineEditText != null && mInlineEditText.isEditing()) {
                quitInlineEditText();
            } else {
                // set the next tool mode
                setNextToolMode();
            }

            // save new edit mode in settings
            if (mUpdateEditMode) {
                SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, mCurrentEditMode);
                editor.apply();
            }

            if (!TextUtils.isEmpty(mDialogFreeTextNote.getNote())) {
                Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);
            }
        }
        mAnnotButtonPressed = 0;
    }

    protected void createAnnot(String contents) throws PDFNetException, JSONException {
        // set edit text bounding box
        Rect bbox = getTextBBoxOnPage(mPdfViewCtrl, mPageNum, mTargetPointPageSpace);
        if (bbox == null) {
            return;
        }

        FreeText freeText = FreeText.create(mPdfViewCtrl.getDoc(), bbox);

        freeText.setContents(contents);
        boolean isRightToLeft = Utils.isRightToLeftString(contents);
        if (isRightToLeft) {
            freeText.setQuaddingFormat(2); // right justification
        }
        freeText.setFontSize(mTextSize);
        if (mFillColor == Color.TRANSPARENT) {
            freeText.setColor(new ColorPt(0, 0, 0, 0), 0);
        } else {
            ColorPt colorPt = Utils.color2ColorPt(mFillColor);
            freeText.setColor(colorPt, 3);
        }
        freeText.setOpacity(mOpacity);

        // Set border style and color
        float thickness = mThickness;

        if (mStrokeColor == Color.TRANSPARENT) {
            freeText.setLineColor(new ColorPt(0, 0, 0, 0), 0);
            thickness = 0;
            freeText.getSDFObj().putNumber(PDFTRON_THICKNESS, mThickness);
        } else {
            freeText.setLineColor(Utils.color2ColorPt(mStrokeColor), 3);
        }

        Annot.BorderStyle border = freeText.getBorderStyle();
        border.setWidth(thickness);
        freeText.setBorderStyle(border);
        ColorPt color = Utils.color2ColorPt(mTextColor);
        freeText.setTextColor(color, 3);
        freeText.refreshAppearance();
        FreeTextInfo.setFont(mPdfViewCtrl, freeText, mPDFTronFontName);

        setAuthor(freeText);

        bbox = getFreeTextBBox(freeText, isRightToLeft);
        freeText.resize(bbox);

        Page page = mPdfViewCtrl.getDoc().getPage(mPageNum);
        page.annotPushBack(freeText);

        // rotate the annotation based on the users perspective, so
        // that it always faces down towards the user
        int pageRotation = mPdfViewCtrl.getDoc().getPage(mPageNum).getRotation();
        int viewRotation = mPdfViewCtrl.getPageRotation();
        int annotRotation = ((pageRotation + viewRotation) % 4) * 90;
        freeText.setRotation(annotRotation);

        setExtraFreeTextProps(freeText, bbox);

        freeText.refreshAppearance();

        setAnnot(freeText, mPageNum);
        buildAnnotBBox();

        mPdfViewCtrl.update(mAnnot, mPageNum);

        Pattern pattern = Pattern.compile("([A-Za-z0-9])\\w+");
        boolean showToast = !pattern.matcher(contents).matches();
        // add to pending font list if pdfnet font is not loaded
        ((ToolManager) mPdfViewCtrl.getToolManager()).addPendingFreeText(new FreeTextInfo(page.getNumAnnots() - 1, page.getIndex(), mPDFTronFontName), showToast);
    }

    protected Rect getFreeTextBBox(FreeText freeText, boolean isRightToLeft) throws PDFNetException {
        return calcFreeTextBBox(mPdfViewCtrl, freeText, mPageNum,
            isRightToLeft, mTargetPointPageSpace);
    }

    protected void setExtraFreeTextProps(FreeText freetext, Rect bbox) throws PDFNetException {
        // used for advanced freetext type such as callout annotation
    }

    public static Rect calcFreeTextBBox(PDFViewCtrl pdfViewCtrl, FreeText freeText, int pageNum,
                                        boolean isRightToLeft, com.pdftron.pdf.Point targetPoint) throws PDFNetException {
        // Get the annotation's content stream and iterate through elements to union
        // their bounding boxes
        Obj contentStream = freeText.getSDFObj().findObj("AP").findObj("N");
        ElementReader er = new ElementReader();
        Rect unionRect = null;
        Element element;

        er.begin(contentStream);
        for (element = er.next(); element != null; element = er.next()) {
            Rect rect = element.getBBox();
            if (rect != null && element.getType() == Element.e_text) {
                if (unionRect == null) {
                    unionRect = rect;
                }
                unionRect = getRectUnion(rect, unionRect);
            }
        }
        er.end();
        er.destroy();

        // get the page rotation from the users perspective
        int pageRotation = pdfViewCtrl.getDoc().getPage(pageNum).getRotation();
        int viewRotation = pdfViewCtrl.getPageRotation();
        int annotRotation = ((pageRotation + viewRotation) % 4) * 90;

        double xDist = 0;
        double yDist = 0;
        double left, bottom, right, top;
        if (unionRect != null) {
            unionRect.normalize();
            // position the unionRect in the correct location on the
            // page based on the pages rotation from the users perspective

            // get the height/width of the unionRect and swap as necessary
            // for rotation
            if (annotRotation == 90 || annotRotation == 270) {
                xDist = unionRect.getHeight();
                yDist = unionRect.getWidth();
            } else {
                xDist = unionRect.getWidth();
                yDist = unionRect.getHeight();
            }
        }

        // grow the edit text to ensure all text is visible
        if (xDist == 0 || yDist == 0) {
            xDist = 60;
            yDist = 60;
        } else {
            xDist += 25;
            yDist += 5;
        }

        // add or subtract the height and width from the target point
        // as necessary based on rotation. This way the edit text will grow
        // in the correct direct in relation to the target point (where the user
        // taps down on the screen)
        if (annotRotation == 90) {
            if (pdfViewCtrl.getRightToLeftLanguage()) {
                xDist *= -1;
            } else {
                yDist *= -1;
            }
        } else if (annotRotation == 270) {
            if (pdfViewCtrl.getRightToLeftLanguage()) {
                yDist *= -1;
            } else {
                xDist *= -1;
            }
        } else if (annotRotation == 180) {
            xDist *= -1; // multiply by -1, so that the bbox grows downwards
            yDist *= -1;
        }

        // set the bounding box using the target point and
        // size of the text
        left = targetPoint.x - (isRightToLeft ? xDist : 0);
        top = targetPoint.y;
        right = targetPoint.x + (isRightToLeft ? 0 : xDist);
        bottom = targetPoint.y - yDist;

        // normalize the bounding box
        Rect rect = new Rect(left, top, right, bottom);
        rect.normalize();
        left = rect.getX1();
        top = rect.getY1();
        right = rect.getX2();
        bottom = rect.getY2();

        // Let's make sure we do not go beyond page borders
        Page page = pdfViewCtrl.getDoc().getPage(pageNum);
        com.pdftron.pdf.Rect pageCropOnClientF = page.getBox(pdfViewCtrl.getPageBox());
        pageCropOnClientF.normalize();

        if (left < pageCropOnClientF.getX1()) {
            left = pageCropOnClientF.getX1();
        }
        if (top < pageCropOnClientF.getY1()) {
            top = pageCropOnClientF.getY1();
        }
        if (right > pageCropOnClientF.getX2()) {
            right = pageCropOnClientF.getX2();
        }
        if (bottom > pageCropOnClientF.getY2()) {
            bottom = pageCropOnClientF.getY2();
        }

        // and that we have a visible bounding box when
        // inserting the free text close to the border
        if (Math.abs(pageCropOnClientF.getY1() - top) < THRESHOLD_FROM_PAGE_EDGE) {
            top = pageCropOnClientF.getY1() + THRESHOLD_FROM_PAGE_EDGE;
        }
        if (Math.abs(pageCropOnClientF.getX2() - right) < THRESHOLD_FROM_PAGE_EDGE) {
            right = pageCropOnClientF.getX2() - THRESHOLD_FROM_PAGE_EDGE;
        }

        if (right - left < MINIMUM_BBOX_WIDTH) {
            right = left + MINIMUM_BBOX_WIDTH;
        }
        if (right > pageCropOnClientF.getX2()) {
            right = pageCropOnClientF.getX2();
            left = right - MINIMUM_BBOX_WIDTH;
        }
        return new Rect(left, top, right, bottom);
    }

    /**
     * The overload implementation of {@link Tool#onRenderingFinished()}.
     */
    @Override
    public void onRenderingFinished() {
        super.onRenderingFinished();

        if (mInlineEditText != null && mInlineEditText.delayViewRemoval()) {
            mInlineEditText.removeView();
            mInlineEditText = null;
        }
    }

    /**
     * The overload implementation of {@link TextWatcher#beforeTextChanged(CharSequence, int, int, int)}.
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    /**
     * The overload implementation of {@link TextWatcher#onTextChanged(CharSequence, int, int, int)}.
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        long currentTimeStamp = System.currentTimeMillis();
        if (currentTimeStamp - mStoredTimeStamp > 3000) {
            mStoredTimeStamp = currentTimeStamp;
            if (s != null && s.length() > 0) {
                FreeTextCacheStruct freeTextCacheStruct = new FreeTextCacheStruct();
                freeTextCacheStruct.contents = s.toString();
                freeTextCacheStruct.pageNum = mPageNum;
                freeTextCacheStruct.x = mStoredPointX;
                freeTextCacheStruct.y = mStoredPointY;
                AnnotUtils.saveFreeTextCache(freeTextCacheStruct, mPdfViewCtrl);
            }
        }
    }

    /**
     * The overload implementation of {@link TextWatcher#afterTextChanged(Editable)}.
     */
    @Override
    public void afterTextChanged(Editable s) {

    }

    private void initTextStyle() {
        Context context = mPdfViewCtrl.getContext();
        SharedPreferences settings = Tool.getToolPreferences(context);
        mTextColor = settings.getInt(getTextColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultTextColor(context));
        mTextSize = settings.getFloat(getTextSizeKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultTextSize(context));
        mStrokeColor = settings.getInt(getColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultColor(context, Annot.e_FreeText));
        mThickness = settings.getFloat(getThicknessKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultThickness(context, Annot.e_FreeText));
        mFillColor = settings.getInt(getColorFillKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultFillColor(context, Annot.e_FreeText));
        mOpacity = settings.getFloat(getOpacityKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultOpacity(context, Annot.e_FreeText));
        mPDFTronFontName = settings.getString(getFontKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultFont(mPdfViewCtrl.getContext(), Annot.e_FreeText));
    }
}
