//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Ink;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.controls.OnToolbarStateUpdateListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.InkItem;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.PathPool;
import com.pdftron.pdf.utils.PointFPool;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.sdf.Obj;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

/**
 * This class is for creating a free hand annotation.
 */
@Keep
public class FreehandCreate extends SimpleShapeCreate {

    private static final String TAG = FreehandCreate.class.getName();
    private static boolean sDebug;

    // set to 0 or <0 to disable this timer completely. Ink will then save as soon as the stroke is finished.
    private static final int SAVE_INK_INTERVAL = 2000; // 2 sec

    // When in stylus ink mode, we will also save and start a new annotation if the user draws far enough from the current ink.
    // set to 0 or <0 in order to disable this. If you disable it, every stroke will count as the same annotation.
    private static final double SAVE_INK_MARGIN = 200;

    private static final float ERASER_OPACITY = 0.7f;
    private static final int ERASER_COLOR = Color.LTGRAY;

    private boolean mIsInTimedMode;
    private boolean mTimedModeEnabled;

    // Stylus timer
    private Handler mInkSavingHandler = new Handler(Looper.getMainLooper());
    private Runnable mTickInkSavingCallback = new Runnable() {
        @Override
        public void run() {
            endInkSavingTimer();
        }
    };

    private Paint.Join oldStrokeJoin;
    private Paint.Cap oldStrokeCap;
    private Paint mEraserPaint;
    private EraserItem mEraserItem;
    private boolean mEraserSelected;

    private boolean mMultiStrokeMode;
    private ArrayList<InkItem> mInks = new ArrayList<>();

    private ArrayList<PointF> mCurrentScreenStroke = new ArrayList<>();
    private ArrayList<PointF> mCurrentCanvasStroke = new ArrayList<>();

    private boolean mIsFirstPointNotOnPage;
    private boolean mFlinging;
    private boolean mIsScaleBegun;
    private boolean mIsStartPointOutsidePage;

    private boolean mSetupNewInkItem;

    private com.pdftron.pdf.annots.Ink mEditInkAnnot;

    private float mPrevX, mPrevY; // previous point in the view space
    private static final float TOUCH_TOLERANCE = 1;

    private boolean mScrollEventOccurred = true;
    private boolean mEditingAnnot;
    private boolean mRegisteredDownEvent;
    private boolean mIsStrokeDrawing;

    private Stack<StrokeSnapshot> mStrokesUndoStack;
    private Stack<StrokeSnapshot> mStrokesRedoStack;

    private OnToolbarStateUpdateListener mOnToolbarStateUpdateListener;

    /**
     * Class constructor
     */
    @SuppressWarnings("WeakerAccess")
    public FreehandCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = ToolMode.INK_CREATE;

        oldStrokeJoin = mPaint.getStrokeJoin();
        oldStrokeCap = mPaint.getStrokeCap();
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mEraserPaint = new Paint();
        mEraserPaint.setAntiAlias(true);
        mEraserPaint.setStyle(Paint.Style.STROKE);
        mEraserPaint.setStrokeJoin(Paint.Join.ROUND);
        mEraserPaint.setStrokeCap(Paint.Cap.ROUND);
        mEraserPaint.setColor(ERASER_COLOR);
        mEraserPaint.setAlpha((int) (255 * ERASER_OPACITY));

        mStrokesUndoStack = new Stack<>();
        mStrokesRedoStack = new Stack<>();

        mTimedModeEnabled = true;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.INK_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Ink;
    }

    /**
     * Sets multiple stroke mode.
     *
     * @param mode True if multiple stroke mode is enabled
     */
    @SuppressWarnings("SameParameterValue")
    public void setMultiStrokeMode(boolean mode) {
        mMultiStrokeMode = mode;
    }

    /**
     * Sets time mode.
     *
     * @param enabled True if time mode is enabled
     */
    @SuppressWarnings("SameParameterValue")
    public void setTimedModeEnabled(boolean enabled) {
        mTimedModeEnabled = enabled;
    }

    /**
     * Sets the {@link OnToolbarStateUpdateListener} listener.
     *
     * @param listener the {@link OnToolbarStateUpdateListener} listener
     */
    public void setOnToolbarStateUpdateListener(OnToolbarStateUpdateListener listener) {
        mOnToolbarStateUpdateListener = listener;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#setupAnnotProperty(int, float, float, int, String, String)}.
     */
    @Override
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName) {
        // if the stroke has a different style than the previous stroke, create a
        // new ink and update the paint style
        if (mStrokeColor != color || mOpacity != opacity || mThickness != thickness) {
            super.setupAnnotProperty(color, opacity, thickness, fillColor, icon, pdfTronFontName);

            float zoom = (float) mPdfViewCtrl.getZoom();
            mThicknessDraw = mThickness * zoom;
            mPaint.setStrokeWidth(mThicknessDraw);
            color = Utils.getPostProcessedColor(mPdfViewCtrl, mStrokeColor);
            mPaint.setColor(color);
            mPaint.setAlpha((int) (255 * mOpacity));
        }
        mEraserSelected = false;
    }

    /**
     * Setups eraser property.
     *
     * @param halfThickness The thickness in half
     */
    public void setupEraserProperty(float halfThickness) {
        resetCurrentPaths();

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(getThicknessKey(AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER), halfThickness);
        editor.apply();

        float zoom = (float) mPdfViewCtrl.getZoom();
        int eraserThickness = (int) (halfThickness * 2 * zoom);
        mEraserPaint.setStrokeWidth(eraserThickness);

        mEraserSelected = true;
        mEraserItem = new EraserItem(mEraserPaint, halfThickness);
    }

    /**
     * Initializes the ink item based on the specified annotation.
     *
     * @param inkAnnot The ink annotation
     */
    public void setInitInkItem(Annot inkAnnot) {
        try {
            if (inkAnnot == null || inkAnnot.getType() != Annot.e_Ink) {
                return;
            }
        } catch (PDFNetException e) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            mEditingAnnot = true;

            // get ink annot's ink list
            mEditInkAnnot = new Ink(inkAnnot);
            Page page = mEditInkAnnot.getPage();

            // get ink annot's appearance
            // color
            ColorPt colorPt = mEditInkAnnot.getColorAsRGB();
            int color = Utils.colorPt2color(colorPt);

            // opacity
            Markup m = new Markup(mEditInkAnnot);
            float opacity = (float) m.getOpacity();

            // thickness
            float thickness = (float) mEditInkAnnot.getBorderStyle().getWidth();
            setupAnnotProperty(color, opacity, thickness, color, null, null);

            // draw ink annot in UI and hide actual annot
            InkItem curInkItem = setupNewInkItem();
            setupInkItem(mEditInkAnnot, curInkItem);
            updateLastUndoSnapshot();

            mPdfViewCtrl.hideAnnotation(mEditInkAnnot);
            mPdfViewCtrl.update(mEditInkAnnot, page.getIndex());

            mPdfViewCtrl.invalidate();

            // update undo/redo and eraser buttons
            if (mOnToolbarStateUpdateListener != null) {
                mOnToolbarStateUpdateListener.onToolbarStateUpdated();
            }

            mEraserSelected = false;
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * Sets the initial ink item. This method will not lock internally and requires a lock outside of it.
     */
    public static void setupInkItem(com.pdftron.pdf.annots.Ink ink, InkItem item) throws PDFNetException {
        Page page = ink.getPage();
        Obj annotObj = ink.getSDFObj();
        Obj inkList = annotObj.findObj("InkList");
        item.mPageStrokes = createPageStrokesFromArrayObj(inkList);
        if (item.mPageStrokes.size() > 0) {
            item.mPageStrokePoints = item.mPageStrokes.get(item.mPageStrokes.size() - 1);
        } else {
            item.mPageStrokePoints = new ArrayList<>();
        }
        item.mPageForFreehandAnnot = page.getIndex();
        item.mDirtyPaths = true;
        item.mDirtyDrawingPts = true;
    }

    private InkItem getCurrentInkItem() {
        if (mEraserSelected) {
            return mEraserItem;
        }

        if (mInks.size() > 0) {
            return mInks.get(mInks.size() - 1);
        }

        return null;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);
        mScrollEventOccurred = false;

        mDownPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(e.getX(), e.getY());
        mIsStartPointOutsidePage = mDownPageNum < 1;
        if (mIsStartPointOutsidePage) {
            return false;
        }


        if (mAllowTwoFingerScroll) {
            mRegisteredDownEvent = false;
            return false;
        } else {
            mRegisteredDownEvent = true;
        }

        try {
            if (mEditingAnnot && mDownPageNum != mEditInkAnnot.getPage().getIndex()) {
                return false;
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }

        if (mTimedModeEnabled) {
            if (!mIsStylus && mIsInTimedMode) {
                endInkSavingTimer();
                return false; // once finger touches when in stylus mode, don't do anything
            } else if (mIsStylus && mJustSwitchedFromAnotherTool) {
                mIsInTimedMode = true;
                mMultiStrokeMode = true;
            }

            if (mIsInTimedMode) {
                stopInkSavingTimer();
            }

            if (mIsStylus && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
                // once in stylus mode, ignore finger touch
                return false;
            }
        }

        // Skip if first point is outside page limits
        if (mPageCropOnClientF != null) {
            if (mPt1.x < mPageCropOnClientF.left ||
                mPt1.x > mPageCropOnClientF.right ||
                mPt1.y < mPageCropOnClientF.top ||
                mPt1.y > mPageCropOnClientF.bottom) {
                if (!mMultiStrokeMode) {
                    setNextToolModeHelper(ToolMode.ANNOT_EDIT);
                } else {
                    mIsFirstPointNotOnPage = true;
                }
                return false;
            } else {
                mIsFirstPointNotOnPage = false;
            }
        }

        if (mEraserSelected) {
            mEraserItem.mPageForFreehandAnnot = mDownPageNum;
        } else {
            mSetupNewInkItem = true;
            setupNewInkItem();
            if (sDebug) Log.d(TAG, "setup new stroke");
            mIsStrokeDrawing = true;
        }

        InkItem curInkItem = getCurrentInkItem();
        if (curInkItem == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Current ink item is null"));
            return false;
        }
        if (curInkItem.mIsFirstPath) {
            curInkItem.mPageForFreehandAnnot = mDownPageNum;
            curInkItem.mIsFirstPath = false;
        }

        if (mMultiStrokeMode) {
            // Skip if paths are being added to different pages
            if (curInkItem.mPageForFreehandAnnot != mDownPageNum) {
                if (mIsInTimedMode) {
                    endInkSavingTimer();
                }
                return false;
            }
        }

        //noinspection ConstantConditions
        if (mInks.size() > 1 && mIsInTimedMode && SAVE_INK_MARGIN > 0 && !mEraserSelected) {
            // check distance of pointer point from current bounding box.
            try {
                int index = mInks.size() - 2;
                Rect rect = getInkItemBBox(mInks.get(index));
                if (rect != null) {
                    double[] pt1 = mPdfViewCtrl.convScreenPtToPagePt(e.getX(), e.getY(), curInkItem.mPageForFreehandAnnot);
                    rect.inflate(SAVE_INK_MARGIN);
                    if (!rect.contains(pt1[0], pt1[1])) {
                        endInkSavingTimer();
                        return false;
                    }
                }
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        }

        curInkItem.mPageStrokePoints = new ArrayList<>();

        mPrevX = mPt1.x;
        mPrevY = mPt1.y;

        resetCurrentPaths();
        mCurrentScreenStroke.add(PointFPool.getInstance().obtain(e.getX(), e.getY()));
        mCurrentCanvasStroke.add(PointFPool.getInstance().obtain(mPt1.x, mPt1.y));

        return false;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        if (mIsStartPointOutsidePage) {
            return false;
        }

        // if we were still scrolling and flinging when the onDown event was called
        if (!mRegisteredDownEvent) {
            return false;
        }

        // We are scrolling
        if (mAllowTwoFingerScroll) {
            return false;
        }

        if (mAllowOneFingerScrollWithStylus) {
            return false;
        }

        try {
            if (mEditingAnnot && mDownPageNum != mEditInkAnnot.getPage().getIndex()) {
                return false;
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }

        // If free hand started out of the page boundaries or the page we are adding the path is
        // different from the page where we started, we just skip
        InkItem curInkItem = getCurrentInkItem();
        if (curInkItem == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Current ink item is null"));
            return false;
        }
        if (mMultiStrokeMode && !mEraserSelected &&
            (mIsFirstPointNotOnPage || curInkItem.mPageForFreehandAnnot != mDownPageNum)) {
            return false;
        }

        final int historySize = e2.getHistorySize();
        final int pointerCount = e2.getPointerCount();

        // Loop through all intermediate points
        // During moving, update the free hand path and the bounding box. Note that for the
        // bounding box, we need to include the previous bounding box in the new bounding box
        // so that the previously drawn free hand will go away.
        for (int h = 0; h < historySize; h++) {
            if (pointerCount >= 1) {

                float x = e2.getHistoricalX(0, h);
                float y = e2.getHistoricalY(0, h);

                processMotionPoint(x, y);
            }
        }
        float x = e2.getX();
        float y = e2.getY();
        processMotionPoint(x, y);

        return true;
    }

    private void processMotionPoint(float x, float y) {
        int sx = mPdfViewCtrl.getScrollX();
        int sy = mPdfViewCtrl.getScrollY();
        float canvasX = x + sx;
        float canvasY = y + sy;

        // Don't allow the annotation to go beyond the page
        if (mPageCropOnClientF != null) {
            if (canvasX < mPageCropOnClientF.left) {
                canvasX = mPageCropOnClientF.left;
                x = canvasX - sx;
            } else if (canvasX > mPageCropOnClientF.right) {
                canvasX = mPageCropOnClientF.right;
                x = canvasX - sx;
            }
            if (canvasY < mPageCropOnClientF.top) {
                canvasY = mPageCropOnClientF.top;
                y = canvasY - sy;
            } else if (canvasY > mPageCropOnClientF.bottom) {
                canvasY = mPageCropOnClientF.bottom;
                y = canvasY - sy;
            }
        }

        float dx = Math.abs(canvasX - mPrevX);
        float dy = Math.abs(canvasY - mPrevY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPrevX = canvasX;
            mPrevY = canvasY;

            mCurrentScreenStroke.add(PointFPool.getInstance().obtain(x, y));
            mCurrentCanvasStroke.add(PointFPool.getInstance().obtain(canvasX, canvasY));
            mPt1.x = Math.min(canvasX, mPt1.x);
            mPt1.y = Math.min(canvasY, mPt1.y);
            mPt2.x = Math.max(canvasX, mPt2.x);
            mPt2.y = Math.max(canvasY, mPt2.y);

            float min_x = mPt1.x - mThicknessDraw;
            float max_x = mPt1.y + mThicknessDraw;
            float min_y = mPt2.x - mThicknessDraw;
            float max_y = mPt2.y + mThicknessDraw;

            mPdfViewCtrl.invalidate((int) min_x, (int) min_y, (int) Math.ceil(max_x), (int) Math.ceil(max_y));
        }
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {

        if (mIsStartPointOutsidePage) {
            return false;
        }

        // if we were still scrolling and flinging when the onDown event was called
        if (!mRegisteredDownEvent) {
            removeLastInkItem();
            return false;
        }
        boolean savedPoints = false;

        // we are flinging
        if (priorEventMode == PDFViewCtrl.PriorEventMode.FLING) {
            mFlinging = true;
        }

        // We are scrolling
        if (mAllowTwoFingerScroll) {
            doneTwoFingerScrolling();
            mScrollEventOccurred = true;
            removeLastInkItem();
            return false;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PAGE_SLIDING) {
            removeLastInkItem();
            return false;
        }

        if (mAllowOneFingerScrollWithStylus) {
            doneOneFingerScrollingWithStylus();
            mScrollEventOccurred = true;
            removeLastInkItem();
            return false;
        }

        // if the user is scrolling and fires a fling event,
        // onUp will be called twice - the first call will be the fling event
        // on the second call pass the event to the PDFViewCtrl to prevent
        // saving an ink dot.
        if (mScrollEventOccurred) {
            mScrollEventOccurred = false;
            removeLastInkItem();
            return false;
        }

        if (mIsStylus && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            removeLastInkItem();
            return false;
        }

        if (mStylusUsed && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            removeLastInkItem();
            return false;
        }

        // If free hand started out of the page boundaries or the page we are adding the path is
        // different from the page where we started, we just skip
        InkItem curInkItem = getCurrentInkItem();
        if (curInkItem == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Current ink item is null"));
            removeLastInkItem();
            return false;
        }
        if (mMultiStrokeMode &&
            mIsFirstPointNotOnPage || (!mEraserSelected && curInkItem.mPageForFreehandAnnot != mDownPageNum)) {
            removeLastInkItem();
            return false;
        }

        // If annotation was already pushed back, avoid re-entry due to fling motion.
        if (mAnnotPushedBack) {
            mAnnotPushedBack = false;
            removeLastInkItem();
            return false;
        }

        try {
            if (mEditingAnnot && mDownPageNum != mEditInkAnnot.getPage().getIndex()) {
                return false;
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }

        float x = e.getX();
        float y = e.getY();
        processMotionPoint(x, y);

        // Check if the path is too small and not in multi stroke mode
        if (mMultiStrokeMode ||
            (Math.abs(mPt2.x - mPt1.x) > START_DRAWING_THRESHOLD) ||
            (Math.abs(mPt2.y - mPt1.y) > START_DRAWING_THRESHOLD)) {

            // Handle dot: add a point to a dot to make it slightly larger
            if (mCurrentScreenStroke.size() == 1) {
                PointF tempPt = PointFPool.getInstance().obtain(mCurrentScreenStroke.get(0).x + START_DRAWING_THRESHOLD / 2, mCurrentScreenStroke.get(0).y + START_DRAWING_THRESHOLD / 2);
                mCurrentScreenStroke.add(tempPt);
                mCurrentCanvasStroke.add(PointFPool.getInstance().obtain(tempPt.x + mPdfViewCtrl.getScrollX(), tempPt.y + mPdfViewCtrl.getScrollY()));
            }

            // save information from the current stroke: page points, drawing points and the path
            // save path
            Path path = createPathFromCanvasPts(mCurrentCanvasStroke);
            curInkItem.mPaths.add(path);
            mCurrentCanvasStroke.clear();

            // save page points
            for (PointF point : mCurrentScreenStroke) {
                // save all points to page space
                addPageStrokeList(curInkItem, point.x, point.y);
                savedPoints = true;
            }
            curInkItem.mPageStrokes.add(curInkItem.mPageStrokePoints);

            // save drawing strokes
            curInkItem.mDrawingStrokes = createDrawingStrokesFromPageStrokes(mPdfViewCtrl, curInkItem.mPageStrokes,
                curInkItem.mStylusUsed, curInkItem.mPageForFreehandAnnot);
            curInkItem.mDirtyDrawingPts = true;

            if (!mMultiStrokeMode) {
                setNextToolModeHelper(ToolMode.ANNOT_EDIT);
                commit(curInkItem);
            }

            if (mEraserSelected) {
                processEraser();
            } else {
                mStrokesRedoStack.clear();
            }

            mAnnotPushedBack = true;

            if (mIsInTimedMode) {
                resetInkSavingTimer();
            }
        }

        // if it doesn't reach to this point an extra ink item has been created,
        // call removeLastInkItem to remove that extra ink item
        mSetupNewInkItem = false; // avoid removing the real ink in push back
        if (mIsStrokeDrawing) {
            updateLastUndoSnapshot();
            mIsStrokeDrawing = false;
        }

        if (mOnToolbarStateUpdateListener != null) {
            mOnToolbarStateUpdateListener.onToolbarStateUpdated();
        }

        mPdfViewCtrl.invalidate(); // to handle when there is no move event between down and up

        if (mIsStylus) {
            raiseStylusUsedFirstTimeEvent();
        }

        return savedPoints || skipOnUpPriorEvent(priorEventMode);
    }

    private void removeLastInkItem() {
        if (mSetupNewInkItem && mInks != null) {
            int sz = mInks.size();
            if (sz > 0) {
                // this one is added in onDown but never used
                mInks.remove(sz - 1);
                removeLastUndoSnapshot();
                if (sDebug) Log.d(TAG, "remove the last stroke: " + (sz - 1));
            }
        }
        mSetupNewInkItem = false;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        super.onFlingStop();
        if (mAllowOneFingerScrollWithStylus) {
            doneOneFingerScrollingWithStylus();
        }
        mFlinging = false;
        mIsScaleBegun = false;
        mPdfViewCtrl.invalidate();
        return false;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#doneTwoFingerScrolling()}.
     */
    @Override
    protected void doneTwoFingerScrolling() {
        mCurrentScreenStroke.clear();
        mCurrentCanvasStroke.clear();
        super.doneTwoFingerScrolling();
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#doneOneFingerScrollingWithStylus()}.
     */
    @Override
    protected void doneOneFingerScrollingWithStylus() {
        mCurrentScreenStroke.clear();
        mCurrentCanvasStroke.clear();
        super.doneOneFingerScrollingWithStylus();
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        mIsScaleBegun = true;
        return super.onScaleBegin(x, y);
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#createMarkup(PDFDoc, Rect)}.
     */
    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        return null;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        onDoubleTapEvent(e);
        return true;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDoubleTapEvent(MotionEvent)}.
     */
    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        // fast writing is sometimes treated as double tap and onMove is never triggered
        // let's handle the movement properly here
        if (e.getAction() == MotionEvent.ACTION_MOVE) {
            onMove(e, e, 0, 0);
        }

        // onDown and onUp would be called in normal way, so no worries about these events
        // It seems the order of events when a double tap happens is like this:
        //   Down
        //   Move
        //   Up
        //   Double Tap (Down)
        //   Down
        //   Double Tap (Move)
        //   Double Tap (Up)
        //   Up

        return true;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        // during sliding drawing strokes cannot be reliably calculated
        if ((mFlinging && mIsScaleBegun) || mPdfViewCtrl.isSlidingWhileZoomed()) {
            return;
        }

        DrawingUtils.drawInk(mPdfViewCtrl, canvas, mInks, mFlinging);

        if (mIsStartPointOutsidePage) {
            return;
        }

        // draw current stroke
        InkItem curInkItem = getCurrentInkItem();
        if (curInkItem != null) {
            Paint currentStrokePaint = mEraserSelected ? mEraserPaint : curInkItem.mPaint;
            Path path = createPathFromCanvasPts(mCurrentCanvasStroke);
            if (mPdfViewCtrl.isMaintainZoomEnabled()) {
                canvas.save();
                try {
                    canvas.translate(0, -mPdfViewCtrl.getScrollYOffsetInTools(curInkItem.mPageForFreehandAnnot));
                    canvas.drawPath(path, currentStrokePaint);
                } finally {
                    canvas.restore();
                }
            } else {
                canvas.drawPath(path, currentStrokePaint);
            }
        }
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return true;
    }

    private Path createPathFromCanvasPts(ArrayList<PointF> points) {
        Path path = PathPool.getInstance().obtain();
        if (points.size() <= 1) {
            return path;
        }

        if (mIsStylus) {
            path.moveTo(points.get(0).x, points.get(0).y);
            for (PointF point : points) {
                path.lineTo(point.x, point.y);
            }
        } else {
            // calculate points
            double[] inputLine = new double[(points.size() * 2)];
            for (int i = 0, cnt = points.size(); i < cnt; i++) {
                inputLine[i * 2] = points.get(i).x;
                inputLine[i * 2 + 1] = points.get(i).y;
            }

            double[] currentBeizerPts;
            try {
                currentBeizerPts = Ink.getBezierControlPoints(inputLine);
            } catch (Exception e) {
                return path;
            }

            path.moveTo((float) currentBeizerPts[0], (float) currentBeizerPts[1]);
            for (int i = 2, cnt = currentBeizerPts.length; i < cnt; i += 6) {
                path.cubicTo((float) currentBeizerPts[i], (float) currentBeizerPts[i + 1], (float) currentBeizerPts[i + 2],
                    (float) currentBeizerPts[i + 3], (float) currentBeizerPts[i + 4], (float) currentBeizerPts[i + 5]);
            }
        }
        return path;
    }

    public static PointF convPagePtToDrawingPt(PDFViewCtrl pdfViewCtrl, float pageX, float pageY, int pageNum,
                                               @Nullable PointF offset, float scaleX, float scaleY) {
        float left = 0;
        float top = 0;
        if (null != offset) {
            left = offset.x;
            top = offset.y;
        }

        float sx = pdfViewCtrl.getScrollX();
        float sy = pdfViewCtrl.getScrollY();
        float x, y;
        double[] pts;
        if (pdfViewCtrl.isContinuousPagePresentationMode(pdfViewCtrl.getPagePresentationMode())) {
            pts = pdfViewCtrl.convPagePtToScreenPt(pageX, pageY, pageNum);
            x = (float) pts[0] + sx;
            y = (float) pts[1] + sy;
        } else {
            pts = pdfViewCtrl.convPagePtToHorizontalScrollingPt(pageX, pageY, pageNum);
            x = (float) pts[0];
            y = (float) pts[1];
        }
        return new PointF(Math.max((x - left) * scaleX, 0), Math.max((y - top) * scaleY, 0));
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onClose()}.
     */
    @Override
    public void onClose() {
        super.onClose();
        commitAnnotation();
        unsetAnnot();
    }

    /**
     * Commits all changes to the annotation.
     * <p>
     * <div class="warning">
     * Before undo/redo, you should ensure there is no commit left to annotations.
     * </div>
     */
    @SuppressWarnings("WeakerAccess")
    public void commitAnnotation() {
        if (mIsInTimedMode) {
            endInkSavingTimer();
        } else {
            commit();
        }
    }

    private void commit() {
        synchronized (this) {
            int curColor = 0;
            float curOpacity = 0f;
            float curThickness = 0f;
            int curPageForFreehandAnnot = 0;

            if (mEditingAnnot && mInks.size() == 0) {
                // everything is removed
                boolean shouldUnlock = false;
                try {
                    mPdfViewCtrl.docLock(true);
                    shouldUnlock = true;

                    Page page = mEditInkAnnot.getPage();
                    mPdfViewCtrl.showAnnotation(mEditInkAnnot);
                    raiseAnnotationPreRemoveEvent(mEditInkAnnot, page.getIndex());
                    page.annotRemove(mEditInkAnnot);

                    mPdfViewCtrl.update(mEditInkAnnot, page.getIndex()); // Update the region where the annotation occupies.
                    raiseAnnotationRemovedEvent(mEditInkAnnot, page.getIndex());
                } catch (Exception ex) {
                    mNextToolMode = ToolMode.PAN;
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                } finally {
                    if (shouldUnlock) {
                        mPdfViewCtrl.docUnlock();
                    }
                }
            } else {
                // add UI to drawing list
                addOldTools();

                List<InkItem> inkItems = new ArrayList<>();
                for (int i = 0, count = mInks.size(); i < count; ++i) {
                    InkItem inkItem = mInks.get(i);
                    if (inkItem.mPageStrokes.isEmpty()) {
                        continue;
                    }
                    boolean hasSameAttr = inkItem.mPageForFreehandAnnot == curPageForFreehandAnnot &&
                        inkItem.mColor == curColor && inkItem.mOpacity == curOpacity && inkItem.mThickness == curThickness;

                    if (i == 0 || hasSameAttr) {
                        inkItems.add(inkItem);
                    }

                    if (i != 0 && !hasSameAttr) {
                        // at each step commit all ink items that have the same attribute
                        commitAnnotationWithSameAttr(inkItems);
                        inkItems.clear();
                        inkItems.add(inkItem);
                    }

                    if (i != count - 1 && !hasSameAttr) {
                        curColor = inkItem.mColor;
                        curOpacity = inkItem.mOpacity;
                        curThickness = inkItem.mThickness;
                        curPageForFreehandAnnot = inkItem.mPageForFreehandAnnot;
                    }
                }
                if (inkItems.size() > 0) {
                    // at each step commit all ink items that have the same attribute
                    commitAnnotationWithSameAttr(inkItems);
                }
            }

            mPaint.setStrokeJoin(oldStrokeJoin);
            mPaint.setStrokeCap(oldStrokeCap);
        }
    }

    private void commit(InkItem inkItem) {
        // add UI to drawing list
        addOldTools();
        List<InkItem> inkItems = new ArrayList<>();
        inkItems.add(inkItem);
        commitAnnotationWithSameAttr(inkItems);
    }

    private void commitAnnotationWithSameAttr(List<InkItem> inkItems) {
        // check if all ink items in the given list have already been committed, if so return
        boolean allCommitted = true;
        InkItem oneInkItem = null;
        for (InkItem inkItem : inkItems) {
            oneInkItem = inkItem;
            if (!inkItem.mCommittedAnnot) {
                allCommitted = false;
                break;
            }
        }
        if (allCommitted) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            Rect annotRect = getInkItemBBox(inkItems);
            if (annotRect == null) {
                return;
            }

            Ink ink;
            if (mEditingAnnot) {
                // after setRect we are missing the rectangle information of the original annotation,
                // so let's raise pre modification event right now, though it would be possible that
                // it is a remove action instead of modification action
                // Note that in raiseAnnotationsPreModifyEvent we only set the information that may
                // be used in raiseAnnotationsModifiedEvent later
                raiseAnnotationPreModifyEvent(mEditInkAnnot, oneInkItem.mPageForFreehandAnnot);

                ink = mEditInkAnnot;

                if (annotRect.getX1() != 0.0 || annotRect.getX1() != 0.0
                    || annotRect.getX1() != 0.0 || annotRect.getX1() != 0.0) {
                    // if all strokes are eliminated by the eraser (so will be considered as a
                    // remove action) we want to keep the original rectangle info which may be used
                    // in undo/redo
                    ink.setRect(annotRect);
                }
                Obj annotObj = mEditInkAnnot.getSDFObj();
                annotObj.erase("InkList");
            } else {
                ink = Ink.create(mPdfViewCtrl.getDoc(), annotRect);
            }

            boolean inkSmoothing = false;
            if (!mIsStylus) {
                inkSmoothing = ((ToolManager) mPdfViewCtrl.getToolManager()).isInkSmoothingEnabled();
            }
            ink.setSmoothing(inkSmoothing);

            int pathIdx = 0;
            for (InkItem inkItem : inkItems) {
                ListIterator<ArrayList<PointF>> itrPaths = inkItem.mPageStrokes.listIterator(0);
                while (itrPaths.hasNext()) {
                    ArrayList<PointF> path = itrPaths.next();
                    ListIterator<PointF> itr = path.listIterator(0);
                    Point p = new Point();
                    int pointIdx = 0;
                    while (itr.hasNext()) {
                        PointF p_ = itr.next();
                        p.x = p_.x;
                        p.y = p_.y;
                        ink.setPoint(pathIdx, pointIdx++, p);
                    }
                    pathIdx++;
                }
            }

            setStyle(ink, oneInkItem);

            ink.refreshAppearance();
            buildAnnotBBox();

            setAnnot(ink, oneInkItem.mPageForFreehandAnnot); // now mAnnot = ink

            boolean annotRemoved = true;
            if (mEditingAnnot) {
                for (int i = 0, cnt = ink.getPathCount(); i < cnt; ++i) {
                    if (ink.getPointCount(i) > 0) {
                        annotRemoved = false;
                        break;
                    }
                }
                mPdfViewCtrl.showAnnotation(mAnnot);
                if (annotRemoved) {
                    raiseAnnotationPreRemoveEvent(mAnnot, mAnnotPageNum);
                    Page page = mPdfViewCtrl.getDoc().getPage(oneInkItem.mPageForFreehandAnnot);
                    page.annotRemove(mAnnot);
                }
            } else {
                Page page = mPdfViewCtrl.getDoc().getPage(oneInkItem.mPageForFreehandAnnot);
                page.annotPushBack(mAnnot);
            }

            for (InkItem inkItem : inkItems) {
                inkItem.mCommittedAnnot = true;
            }

            mPdfViewCtrl.update(mAnnot, mAnnotPageNum); // Update the region where the annotation occupies.
            buildAnnotBBox();

            if (mEditingAnnot) {
                if (annotRemoved) {
                    raiseAnnotationRemovedEvent(mAnnot, mAnnotPageNum);
                } else {
                    raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);
                }
            } else {
                raiseAnnotationAddedEvent(mAnnot, mAnnotPageNum);
            }
        } catch (Exception e) {
            mNextToolMode = ToolMode.PAN;
            ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(e.getMessage());
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        for (InkItem ink : mInks) {
            ink.mDirtyDrawingPts = true;
        }
    }

    private Rect getInkItemBBox(List<InkItem> inkItems) {
        if (inkItems.isEmpty()) {
            return null;
        }

        float min_x = Float.MAX_VALUE;
        float min_y = Float.MAX_VALUE;
        float max_x = Float.MIN_VALUE;
        float max_y = Float.MIN_VALUE;

        float thickness = -1f;
        for (InkItem inkItem : inkItems) {
            if (thickness == -1f) {
                thickness = inkItem.mThickness;
            } else if (thickness != inkItem.mThickness) {
                AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("The list of ink items should have the same thickness"));
            }
            for (ArrayList<PointF> pageStroke : inkItem.mPageStrokes) {
                for (PointF point : pageStroke) {
                    min_x = Math.min(min_x, point.x);
                    max_x = Math.max(max_x, point.x);
                    min_y = Math.min(min_y, point.y);
                    max_y = Math.max(max_y, point.y);
                }
            }
        }

        try {
            if (min_x == Float.MAX_VALUE && min_y == Float.MAX_VALUE
                && max_x == Float.MIN_VALUE && max_y == Float.MIN_VALUE) {
                // no stroke
                return new Rect(0.0, 0.0, 0.0, 0.0);
            } else {
                Rect rect = new Rect(min_x, min_y, max_x, max_y);
                rect.normalize();
                rect.inflate(thickness);
                return rect;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Rect getInkItemBBox(InkItem inkItem) {
        if (inkItem.mPageStrokes.isEmpty()) {
            return null;
        }

        float min_x = Float.MAX_VALUE;
        float min_y = Float.MAX_VALUE;
        float max_x = Float.MIN_VALUE;
        float max_y = Float.MIN_VALUE;

        for (ArrayList<PointF> pageStroke : inkItem.mPageStrokes) {
            for (PointF point : pageStroke) {
                min_x = Math.min(min_x, point.x);
                max_x = Math.max(max_x, point.x);
                min_y = Math.min(min_y, point.y);
                max_y = Math.max(max_y, point.y);
            }
        }

        Rect rect;
        try {
            rect = new Rect(min_x, min_y, max_x, max_y);
            rect.normalize();
            rect.inflate(inkItem.mThickness);
            return rect;
        } catch (Exception e) {
            return null;
        }
    }

    private void processEraser() {
        StrokeSnapshot strokeSnapshot = new StrokeSnapshot();

        // for each ink on the same page as the eraser stroke,
        // erase the points touched by the eraser stroke
        boolean eraserTouchedAnyStroke = false;
        for (int index = 0, count = mInks.size(); index < count; ++index) {
            InkItem inkItem = mInks.get(index);
            InkItem copiedInkItem = new InkItem(inkItem);
            // check if ink is on the same page as the eraser stroke,
            // if not, continue
            if (mEraserItem.mPageForFreehandAnnot != inkItem.mPageForFreehandAnnot) {
                continue;
            }

            // if there are no points in the current ink, then there is nothing
            // to erase
            if (inkItem.mPageStrokes.isEmpty()) {
                continue;
            }

            try {
                Obj strokesArray = mPdfViewCtrl.getDoc().createIndirectArray();

                // for every page stroke
                for (ArrayList<PointF> pageStroke : inkItem.mPageStrokes) {
                    Obj strokeArray = strokesArray.pushBackArray();

                    // for every point in the page stroke
                    int pointIndex = 0;
                    for (PointF point : pageStroke) {
                        while (strokeArray.size() < (pointIndex + 1) * 2) {
                            strokeArray.pushBackNumber(0);
                            strokeArray.pushBackNumber(0);
                        }

                        strokeArray.getAt(pointIndex * 2).setNumber(point.x);
                        strokeArray.getAt(pointIndex * 2 + 1).setNumber(point.y);
                        pointIndex++;
                    }
                }
                // for every eraser point, erase the points the necessary points
                // in the strokes array
                Point prevPt = null;
                Point currPt;

                // calculate the ink rect
                Rect annotRect = getInkItemBBox(inkItem);
                // can't erase without the annots bbox
                if (annotRect == null) {
                    continue;
                }

                boolean eraserTouchedThisStroke = false;
                for (ArrayList<PointF> eraserStroke : mEraserItem.mPageStrokes) {
                    for (PointF eraserPoint : eraserStroke) {
                        if (prevPt != null) {
                            currPt = new Point(eraserPoint.x, eraserPoint.y);
                            boolean erasedPoints = Ink.erasePoints(strokesArray, annotRect, prevPt, currPt, mEraserItem.mThickness);
                            prevPt = currPt;

                            if (erasedPoints) {
                                eraserTouchedThisStroke = true;
                            }
                        } else {
                            prevPt = new Point(eraserPoint.x, eraserPoint.y);
                        }
                    }
                }

                if (eraserTouchedThisStroke) {
                    if (!eraserTouchedAnyStroke) {
                        strokeSnapshot.setPageNum(inkItem.mPageForFreehandAnnot);
                    }
                    eraserTouchedAnyStroke = true;

                    inkItem.mDirtyPaths = true;
                    inkItem.mDirtyDrawingPts = true;
                    // if ink is changed, it should be committed later
                    inkItem.mCommittedAnnot = false;

                    // use the new strokes array to update the page strokes and beizer points
                    inkItem.mPageStrokes = createPageStrokesFromArrayObj(strokesArray);
                    if (inkItem.mPageStrokes.size() > 0) {
                        inkItem.mPageStrokePoints = inkItem.mPageStrokes.get(inkItem.mPageStrokes.size() - 1);
                    } else {
                        inkItem.mPageStrokePoints = new ArrayList<>();
                    }
                }
                strokeSnapshot.add(index, copiedInkItem, new InkItem(inkItem));
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        // if the eraser does not modify any of the ink strokes,
        // delete the copy of all inks
        if (eraserTouchedAnyStroke) {
            takeUndoSnapshot(strokeSnapshot);
            mStrokesRedoStack.clear();
        }

        // invalidate area touched by eraser
        mPdfViewCtrl.invalidate();

        // clear all points in current eraser path
        resetCurrentPaths();
        mEraserItem = new EraserItem(mEraserItem.mPaint, mEraserItem.mThickness);
    }

    public static ArrayList<ArrayList<PointF>> createDrawingStrokesFromPageStrokes(PDFViewCtrl pdfViewCtrl,
                                                                                   ArrayList<ArrayList<PointF>> pathStrokes, boolean isStylus, int annotPage) {
        return createDrawingStrokesFromPageStrokes(pdfViewCtrl, pathStrokes, isStylus, annotPage, null, 1, 1);
    }

    public static ArrayList<ArrayList<PointF>> createDrawingStrokesFromPageStrokes(PDFViewCtrl pdfViewCtrl,
                                                                                   ArrayList<ArrayList<PointF>> pathStrokes, boolean isStylus, int annotPage,
                                                                                   @Nullable PointF offset, float scaleX, float scaleY) {
        ArrayList<ArrayList<PointF>> drawingStrokes = new ArrayList<>();
        if (pathStrokes.isEmpty()) {
            return drawingStrokes;
        }

        if (isStylus) {
            for (ArrayList<PointF> pagePoints : pathStrokes) {
                ArrayList<PointF> drawingPoints = new ArrayList<>();
                for (PointF pagePoint : pagePoints) {
                    PointF p = convPagePtToDrawingPt(pdfViewCtrl, pagePoint.x, pagePoint.y, annotPage, offset, scaleX, scaleY);
                    drawingPoints.add(p);
                }
                drawingStrokes.add(drawingPoints);
            }
        } else {
            for (ArrayList<PointF> pagePoints : pathStrokes) {
                // calculate points
                double[] inputLine = new double[(pagePoints.size() * 2)];
                for (int i = 0, cnt = pagePoints.size(); i < cnt; i++) {
                    inputLine[i * 2] = pagePoints.get(i).x;
                    inputLine[i * 2 + 1] = pagePoints.get(i).y;
                }

                double[] biezerCtrlPts = null;
                try {
                    biezerCtrlPts = Ink.getBezierControlPoints(inputLine);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }

                if (biezerCtrlPts == null) {
                    break;
                }

                ArrayList<PointF> drawingPts = new ArrayList<>();
                for (int i = 0, cnt = biezerCtrlPts.length; i < cnt; i += 2) {
                    PointF p = convPagePtToDrawingPt(pdfViewCtrl, (float) biezerCtrlPts[i], (float) biezerCtrlPts[i + 1], annotPage, offset, scaleX, scaleY);
                    drawingPts.add(p);
                }
                drawingStrokes.add(drawingPts);
            }
        }
        return drawingStrokes;
    }

    private static ArrayList<ArrayList<PointF>> createPageStrokesFromArrayObj(Obj strokesArray) throws PDFNetException {
        ArrayList<ArrayList<PointF>> pageStrokes = new ArrayList<>();
        if (!strokesArray.isArray()) {
            return pageStrokes;
        }

        for (long i = 0, cnt = strokesArray.size(); i < cnt; i++) {
            Obj strokeArray = strokesArray.getAt((int) i);
            if (strokeArray.isArray()) {
                ArrayList<PointF> pageStroke = new ArrayList<>();
                for (long j = 0, count = strokeArray.size(); j < count; j += 2) {
                    float x = (float) strokeArray.getAt((int) j).getNumber();
                    float y = (float) strokeArray.getAt((int) j + 1).getNumber();
                    PointF p = new PointF(x, y);
                    pageStroke.add(p);
                }
                pageStrokes.add(pageStroke);
            }
        }
        return pageStrokes;
    }

    private void raiseStylusUsedFirstTimeEvent() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mPdfViewCtrl.getContext());
        boolean setStylusHasBeenAsked = pref.getBoolean("pref_set_stylus_as_default_has_been_asked", false);
        if (!setStylusHasBeenAsked) {
            ((ToolManager) (mPdfViewCtrl.getToolManager())).onFreehandStylusUsedFirstTime();
            final SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("pref_set_stylus_as_default_has_been_asked", true);
            editor.apply();
        }
    }

    void setStyle(Markup annot, InkItem inkItem) {
        try {
            ColorPt color = Utils.color2ColorPt(inkItem.mColor);
            annot.setColor(color, 3);

            annot.setOpacity(inkItem.mOpacity);

            Annot.BorderStyle bs = annot.getBorderStyle();
            bs.setWidth(inkItem.mThickness);
            annot.setBorderStyle(bs);

            setAuthor(annot);

        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Clears all strokes.
     */
    public void clearStrokes() {
        mStrokesRedoStack.clear();
        // undo snapshot, we need to do this before clear ink list
        StrokeSnapshot strokeSnapshot = new StrokeSnapshot();
        // get all ink Item in undoSnapshot list
        for (int i = 0, cnt = mInks.size(); i < cnt; i++) {
            strokeSnapshot.add(i, new InkItem(mInks.get(i)), null);
        }
        // get last strokeSnapshot page number
        if (!mStrokesUndoStack.isEmpty()) {
            StrokeSnapshot lastSnapShot = mStrokesUndoStack.peek();
            strokeSnapshot.setPageNum(lastSnapShot.getPageNum());
        }

        mStrokesUndoStack.push(strokeSnapshot);

        mInks.clear();
        mPdfViewCtrl.invalidate();
        if (sDebug)
            Log.d(TAG, "after stroke snapshot: # stack: " + mStrokesUndoStack.size() + ", # inks: " + mInks.size());
    }

    private InkItem setupNewInkItem() {
        resetCurrentPaths();
        InkItem inkItem = new InkItem(mPaint, mStrokeColor, mOpacity, mThickness, mIsStylus);
        mInks.add(inkItem);
        takeUndoSnapshot();
        return inkItem;
    }

    private void addPageStrokeList(InkItem inkItem, float x, float y) {
        // convert screen pt to page point before adding to stroke list
        double[] pts = mPdfViewCtrl.convScreenPtToPagePt(x, y, inkItem.mPageForFreehandAnnot);
        inkItem.mPageStrokePoints.add(new PointF((float) pts[0], (float) pts[1]));
    }

    private void resetCurrentPaths() {
        PointFPool.getInstance().recycle(mCurrentScreenStroke);
        mCurrentScreenStroke.clear();
        PointFPool.getInstance().recycle(mCurrentCanvasStroke);
        mCurrentCanvasStroke.clear();
    }

    // Timer Utilities
    private void resetInkSavingTimer() {
        stopInkSavingTimer();
        if (mInkSavingHandler != null) {
            mInkSavingHandler.postDelayed(mTickInkSavingCallback, SAVE_INK_INTERVAL);
        }
    }

    private void stopInkSavingTimer() {
        if (mInkSavingHandler != null) {
            mInkSavingHandler.removeCallbacksAndMessages(null);
        }
    }

    private void endInkSavingTimer() {
        if (sDebug) Log.d(TAG, "end ink saving timer");
        stopInkSavingTimer();
        commit();
        mIsInTimedMode = false;
        mNextToolMode = ToolMode.PAN;
    }

    /**
     * Undoes the last stroke.
     */
    public void undoStroke() {
        if (!mStrokesUndoStack.empty()) {
            StrokeSnapshot strokeSnapshot = mStrokesUndoStack.pop();
            mStrokesRedoStack.push(strokeSnapshot);
            Rect strokeSnapShotRect = null;
            for (StrokeSnapshot.InkInfo inkInfo : strokeSnapshot.mItems) {
                Rect snapShotRect;
                if (inkInfo.mInkIndex < 0) {
                    AnalyticsHandlerAdapter.getInstance().sendException(
                        new Exception("index:" + inkInfo.mInkIndex
                            + " inks size:" + mInks.size()));
                    continue;
                }

                InkItem oldInkItem = inkInfo.mOldInkItem;
                if (oldInkItem == null) { // undo addition
                    if (inkInfo.mInkIndex >= mInks.size()) {
                        AnalyticsHandlerAdapter.getInstance().sendException(
                            new Exception("index:" + inkInfo.mInkIndex
                                + " inks size:" + mInks.size()));
                        continue;
                    }
                    InkItem inkItem = mInks.get(inkInfo.mInkIndex);
                    snapShotRect = getInkItemBBox(inkItem);
                    mInks.remove(inkInfo.mInkIndex);
                } else {
                    if (inkInfo.mInkIndex < mInks.size()) { // undo modification
                        mInks.set(inkInfo.mInkIndex, new InkItem(oldInkItem));
                    } else { // undo deletion
                        mInks.add(new InkItem(oldInkItem));
                    }

                    InkItem inkItem = mInks.get(inkInfo.mInkIndex);
                    snapShotRect = getInkItemBBox(inkItem);
                    inkItem.mDirtyPaths = true;
                    inkItem.mDirtyDrawingPts = true;
                }
                if (strokeSnapShotRect == null) {
                    strokeSnapShotRect = snapShotRect;
                }
            }

            if (strokeSnapShotRect != null && strokeSnapshot.mItems.size() == 1 && strokeSnapshot.getPageNum() > 0) {
                ViewerUtils.animateUndoRedo(mPdfViewCtrl, strokeSnapShotRect, strokeSnapshot.getPageNum());
            }
        }

        if (sDebug)
            Log.d(TAG, "after undo: # stack: " + mStrokesUndoStack.size() + ", # inks: " + mInks.size());

        resetCurrentPaths();
        mPdfViewCtrl.invalidate();
    }

    /**
     * Redoes the last undo.
     */
    public void redoStroke() {
        if (!mStrokesRedoStack.empty()) {
            StrokeSnapshot strokeSnapshot = mStrokesRedoStack.pop();
            mStrokesUndoStack.push(strokeSnapshot);
            Rect strokeSnapShotRect = null;
            for (int i = strokeSnapshot.mItems.size() - 1; i >= 0; i--) {
                Rect snapShotRect = null;
                StrokeSnapshot.InkInfo inkInfo = strokeSnapshot.mItems.get(i);

                if (sDebug) Log.d(TAG, "redoStroke processing index: " + inkInfo.mInkIndex);

                InkItem oldInkItem = inkInfo.mOldInkItem;
                InkItem newInkItem = inkInfo.mNewInkItem;

                boolean removed = false;

                if (oldInkItem == null) {
                    // addition
                    int sz = mInks.size();
                    if (sz <= inkInfo.mInkIndex) {
                        if (sz != inkInfo.mInkIndex) {
                            for (int j = sz; j < inkInfo.mInkIndex; ++j) {
                                setupNewInkItem();
                            }
                            AnalyticsHandlerAdapter.getInstance().sendException(
                                new Exception("some inks are missing"));
                        }
                        mInks.add(inkInfo.mInkIndex, new InkItem(newInkItem));
                    } else {
                        mInks.set(inkInfo.mInkIndex, new InkItem(newInkItem));
                    }
                } else {
                    if (newInkItem != null) {
                        // modification
                        mInks.set(inkInfo.mInkIndex, new InkItem(newInkItem));
                    } else {
                        // removal
                        InkItem inkItem = mInks.get(inkInfo.mInkIndex);
                        snapShotRect = getInkItemBBox(inkItem);
                        mInks.remove(inkInfo.mInkIndex);
                        removed = true;
                    }
                }

                if (!removed) {
                    InkItem inkItem = mInks.get(inkInfo.mInkIndex);
                    snapShotRect = getInkItemBBox(inkItem);
                    inkItem.mDirtyPaths = true;
                    inkItem.mDirtyDrawingPts = true;
                }

                if (strokeSnapShotRect == null) {
                    strokeSnapShotRect = snapShotRect;
                }
            }
            if (strokeSnapShotRect != null && strokeSnapshot.mItems.size() == 1) {
                ViewerUtils.animateUndoRedo(mPdfViewCtrl, strokeSnapShotRect, strokeSnapshot.getPageNum());
            }
        }

        if (sDebug)
            Log.d(TAG, "after redo: # stack: " + mStrokesUndoStack.size() + ", # inks: " + mInks.size());

        resetCurrentPaths();
        mPdfViewCtrl.invalidate();
    }

    /**
     * @return True if can undo the last stroke
     */
    public boolean canUndoStroke() {
        return !mStrokesUndoStack.empty();
    }

    /**
     * @return True if can redo the last undo
     */
    public boolean canRedoStroke() {
        return !mStrokesRedoStack.empty();
    }

    /**
     * @return True if can erase any strokes
     */
    public boolean canEraseStroke() {
        return !mInks.isEmpty();
    }

    private void takeUndoSnapshot() {
        if (mInks.isEmpty()) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("where is last ink?"));
            return;
        }

        StrokeSnapshot strokeSnapshot = new StrokeSnapshot();
        int index = mInks.size() - 1;
        strokeSnapshot.add(index, null, new InkItem(mInks.get(index)));
        strokeSnapshot.setPageNum(mDownPageNum);
        mStrokesUndoStack.push(strokeSnapshot);

        if (sDebug)
            Log.d(TAG, "after stroke snapshot: # stack: " + mStrokesUndoStack.size() + ", # inks: " + mInks.size());
    }

    private void takeUndoSnapshot(StrokeSnapshot strokeSnapshot) {
        if (strokeSnapshot != null) {
            mStrokesUndoStack.push(strokeSnapshot);
        }

        if (sDebug)
            Log.d(TAG, "after eraser snapshot: # stack: " + mStrokesUndoStack.size() + ", # inks: " + mInks.size());
    }

    private void removeLastUndoSnapshot() {
        if (mStrokesUndoStack.isEmpty()) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("where is last undo stack?"));
            return;
        }

        mStrokesUndoStack.pop();
    }

    private void updateLastUndoSnapshot() {
        if (mInks.isEmpty()) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("where is last ink?"));
            return;
        }

        if (mStrokesUndoStack.isEmpty()) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("where is last undo stack?"));
            return;
        }

        mStrokesUndoStack.pop();
        takeUndoSnapshot();
    }

    private class EraserItem extends InkItem {
        EraserItem(Paint paint, float thickness) {
            super(paint, thickness, mIsStylus);
        }
    }

    private class StrokeSnapshot {
        void add(int index, InkItem oldInkItem, InkItem newInkItem) {
            mItems.add(new InkInfo(index, oldInkItem, newInkItem));
        }

        void setPageNum(int pageNum) {
            mPageNum = pageNum;
        }

        int getPageNum() {
            return mPageNum;
        }

        class InkInfo {
            InkInfo(int index, InkItem oldInkItem, InkItem newInkItem) {
                mInkIndex = index;
                mOldInkItem = oldInkItem;
                mNewInkItem = newInkItem;
            }

            InkItem mOldInkItem, mNewInkItem;
            int mInkIndex;
        }

        List<InkInfo> mItems = new ArrayList<>();
        int mPageNum;
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }
}
