//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.controls.OnToolbarStateUpdateListener;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.PointFPool;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * This class is the base class for several shape creation classes that need clicks to determine
 * vertices vs drag and drop, e.g., Polyline, Polygon, Cloud and etc.
 */
@Keep
public abstract class AdvancedShapeCreate extends SimpleShapeCreate {

    protected Path mPath = new Path();

    private ArrayList<Point> mPagePoints = new ArrayList<>();
    private Stack<Snapshot> mUndoStack = new Stack<>();
    private Stack<Snapshot> mRedoStack = new Stack<>();

    private int mPageNum = -1;
    private boolean mIsEditToolbarShown;
    private boolean mIsStartPointOutsidePage;
    private boolean mScrollEventOccurred = true;
    private boolean mLastOnDownPointRemoved;
    private boolean mOnDownPointAdded;
    private boolean mFlinging;
    private boolean mIsScaleBegun;

    private OnToolbarStateUpdateListener mOnToolbarStateUpdateListener;
    private OnEditToolbarListener mOnEditToolbarListener;

    /**
     * Callback interface invoked when the edit toolbar should be shown/closed.
     */
    public interface OnEditToolbarListener {

        /**
         * Shows the edit toolbar
         *
         * @param toolMode The tool mode that should be selected when open edit toolbar
         * @param annot    The selected annotation
         */
        void showEditToolbar(@NonNull ToolMode toolMode, @Nullable Annot annot);

        /**
         * Closes the edit toolbar
         */
        void closeEditToolbar();

    }

    /**
     * Class constructor
     */
    @SuppressWarnings("WeakerAccess")
    public AdvancedShapeCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        if (mOnToolbarStateUpdateListener != null) {
            mOnToolbarStateUpdateListener.onToolbarStateUpdated();
        }
    }

    /**
     * create markup annotation, called in {@link #onClose()}
     *
     * @param doc        PDF Document
     * @param pagePoints points where user clicked on in page coordination
     * @return Markup annotation
     * @throws PDFNetException PDFNet exception
     */
    protected abstract Annot createMarkup(PDFDoc doc, ArrayList<Point> pagePoints) throws PDFNetException;

    /**
     * draw markup annotation, called in {@link #onDraw(Canvas, Matrix)}
     */
    protected abstract void drawMarkup(@NonNull Canvas canvas, Matrix tfm, ArrayList<PointF> canvasPoints);

    /**
     * The overload implementation of {@link SimpleShapeCreate#setupAnnotProperty(int, float, float, int, String, String)}.
     */
    @Override
    public void setupAnnotProperty(int strokeColor, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName) {
        if (mPdfViewCtrl == null || mPaint == null || mFillPaint == null) {
            return;
        }

        if (mStrokeColor != strokeColor || mOpacity != opacity || mThickness != thickness || mFillColor != fillColor) {
            super.setupAnnotProperty(strokeColor, opacity, thickness, fillColor, icon, pdfTronFontName);
            int color = Utils.getPostProcessedColor(mPdfViewCtrl, strokeColor);
            mPaint.setColor(color);
            mPaint.setAlpha((int) (255 * opacity));
            // set stroke width when draw the annotation because zoom can be changed

            if (mHasFill) {
                mFillPaint.setColor(Utils.getPostProcessedColor(mPdfViewCtrl, fillColor));
                mFillPaint.setAlpha((int) (255 * opacity));
            }

            mPdfViewCtrl.invalidate();
        }
    }

    private void showEditToolbar() {

        if (!mIsEditToolbarShown) {
            mIsEditToolbarShown = true;
            if (mOnEditToolbarListener != null) {
                ToolMode toolMode = ToolManager.getDefaultToolMode(getToolMode());
                mOnEditToolbarListener.showEditToolbar(toolMode, null);
            }
        }

    }

    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);

        mScrollEventOccurred = false;
        mOnDownPointAdded = false;

        int pageNum = mPdfViewCtrl.getPageNumberFromScreenPt(e.getX(), e.getY());
        mIsStartPointOutsidePage = pageNum < 1;
        if (mIsStartPointOutsidePage) {
            return false;
        }

        if (mPageNum == -1) {
            mPageNum = pageNum;
        } else if (mPageNum != pageNum) {
            // if not the same page commit the last annotation and start a new
            // annotation in the new page
            commit();
            mPageNum = pageNum;
            // reset points and stacks
            mPagePoints.clear();
            mUndoStack.clear();
            mRedoStack.clear();
            if (mOnToolbarStateUpdateListener != null) {
                mOnToolbarStateUpdateListener.onToolbarStateUpdated();
            }
        }

        if (mStylusUsed && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            return false;
        }

        mLastOnDownPointRemoved = false;
        mOnDownPointAdded = true;
        double[] pts = mPdfViewCtrl.convScreenPtToPagePt(e.getX(), e.getY(), mPageNum);
        Point pagePoint;
        pagePoint = new Point(pts[0], pts[1]);
        mPagePoints.add(pagePoint);

        mPdfViewCtrl.invalidate();

        return false;
    }

    @Override
    public boolean onPointerDown(MotionEvent e) {
        removeLastOnDownPoint();
        mPdfViewCtrl.invalidate();
        return false;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1,
                          MotionEvent e2,
                          float x_dist,
                          float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        // We are scrolling
        if (mAllowTwoFingerScroll) {
            removeLastOnDownPoint();
            return false;
        }

        if (mIsStartPointOutsidePage) {
            return false;
        }

        if (mAllowOneFingerScrollWithStylus) {
            return false;
        }

        int pageNum = mPdfViewCtrl.getPageNumberFromScreenPt(e2.getX(), e2.getY());
        if (pageNum != mPageNum) {
            // if not the same page do nothing
            return false;
        }

        if (!mOnDownPointAdded || mLastOnDownPointRemoved) {
            return false;
        }

        showEditToolbar();

        double[] pts = mPdfViewCtrl.convScreenPtToPagePt(e2.getX(), e2.getY(), mPageNum);
        int size = mPagePoints.size();
        Point pagePoint;
        if (size == 1) {
            pushNewPointToStack(mPagePoints.get(0));
            pagePoint = new Point(pts[0], pts[1]);
            mPagePoints.add(pagePoint);
        } else {
            pagePoint = mPagePoints.get(mPagePoints.size() - 1);
            pagePoint.x = pts[0];
            pagePoint.y = pts[1];
        }

        mPdfViewCtrl.invalidate();

        return true;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e,
                        PDFViewCtrl.PriorEventMode priorEventMode) {
        // we are flinging
        if (priorEventMode == PDFViewCtrl.PriorEventMode.FLING) {
            mFlinging = true;
        }

        if (mAllowTwoFingerScroll) {
            doneTwoFingerScrolling();
            mScrollEventOccurred = true;
            removeLastOnDownPoint();
            return false;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PAGE_SLIDING) {
            removeLastOnDownPoint();
            return false;
        }

        if (mAllowOneFingerScrollWithStylus) {
            doneOneFingerScrollingWithStylus();
            mScrollEventOccurred = true;
            removeLastOnDownPoint();
            return true;
        }

        if (mScrollEventOccurred) {
            mScrollEventOccurred = false;
            removeLastOnDownPoint();
            return false;
        }

        if (mIsStylus && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            removeLastOnDownPoint();
            return false;
        }

        if (mStylusUsed && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            removeLastOnDownPoint();
            return false;
        }

        int pageNum = mPdfViewCtrl.getPageNumberFromScreenPt(e.getX(), e.getY());
        if (pageNum != mPageNum) {
            // if not the same page do nothing
            removeLastOnDownPoint();
            return false;
        }

        if (mIsStartPointOutsidePage) {
            return false;
        }

        if (!mOnDownPointAdded || mLastOnDownPointRemoved) {
            return false;
        }

        if (canSelectTool(e)) {
            return false;
        }

        showEditToolbar();

        double[] pts = mPdfViewCtrl.convScreenPtToPagePt(e.getX(), e.getY(), mPageNum);
        int size = mPagePoints.size();
        boolean addToStack = true;
        if (size > 1 && !mUndoStack.isEmpty()) {
            Snapshot lastSnapshot = mUndoStack.peek();
            if (!lastSnapshot.isRemoved() && lastSnapshot.mPagePoints.size() > 0) {
                Point lastPoint = lastSnapshot.mPagePoints.get(lastSnapshot.mPagePoints.size() - 1);
                if (lastPoint.x == pts[0] && lastPoint.y == pts[1]) {
                    addToStack = false;
                }
            }
        }
        if (addToStack) {
            Point pagePoint = mPagePoints.get(size - 1);
            pagePoint.x = pts[0];
            pagePoint.y = pts[1];
            pushNewPointToStack(pagePoint);
        }

        mPdfViewCtrl.invalidate();

        return skipOnUpPriorEvent(priorEventMode);
    }

    /**
     * Selects a tool if there is any annot there
     *
     * @param e The motion event
     * @return True if an annot has been selected; False otherwise
     */
    private boolean canSelectTool(
        MotionEvent e) {

        if (mIsEditToolbarShown) {
            return false;
        }

        ToolMode toolMode = ToolManager.getDefaultToolMode(getToolMode());
        if (mForceSameNextToolMode &&
            toolMode != ToolMode.INK_CREATE &&
            toolMode != ToolMode.INK_ERASER &&
            toolMode != ToolMode.TEXT_ANNOT_CREATE) {

            setCurrentDefaultToolModeHelper(toolMode);

            int x = (int) (e.getX() + 0.5);
            int y = (int) (e.getY() + 0.5);
            Annot tempAnnot = mPdfViewCtrl.getAnnotationAt(x, y);
            int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
            try {
                if (null != tempAnnot && tempAnnot.isValid()) {
                    ((ToolManager) mPdfViewCtrl.getToolManager()).selectAnnot(tempAnnot, page);
                    return true;
                } else {
                    mNextToolMode = getToolMode();
                }
            } catch (PDFNetException ignored) {
            }
        }
        return false;

    }


    @Override
    public void onPageTurning(int old_page, int cur_page) {

        super.onPageTurning(old_page, cur_page);
        if (cur_page != mPageNum) {
            if (mIsEditToolbarShown && mOnEditToolbarListener != null) {
                mOnEditToolbarListener.closeEditToolbar();
            }
        }

    }

    private void pushNewPointToStack(Point pagePoint) {
        if (mUndoStack == null || mRedoStack == null) {
            return;
        }
        mUndoStack.push(new Snapshot(pagePoint));
        mRedoStack.clear();
        if (mOnToolbarStateUpdateListener != null) {
            mOnToolbarStateUpdateListener.onToolbarStateUpdated();
        }
    }

    private void removeLastOnDownPoint() {
        if (mOnDownPointAdded && !mLastOnDownPointRemoved) {
            mLastOnDownPointRemoved = true;
            int size = mPagePoints.size();
            if (size > 0) {
                mPagePoints.remove(size - 1);
                mPdfViewCtrl.invalidate();
            }
        }
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        // do nothing when double tap
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        // do nothing when double tap
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        // don't let select an annotation
        return true;
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
     * The overload implementation of {@link SimpleShapeCreate#onScaleEnd(float, float)}.
     */
    @Override
    public boolean onScaleEnd(float x, float y) {
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.invalidate();
        }
        return super.onScaleEnd(x, y);
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
     * The overload implementation of {@link SimpleShapeCreate#createMarkup(PDFDoc, Rect)}.
     */
    protected Annot createMarkup(@NonNull PDFDoc doc,
                                 Rect bbox) throws PDFNetException {
        return null;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas,
                       Matrix tfm) {
        if (!mIsEditToolbarShown || mPaint == null || mPageNum == -1) {
            return;
        }

        // during sliding drawing strokes cannot be reliably calculated
        if (mPdfViewCtrl == null || mPdfViewCtrl.isSlidingWhileZoomed() || (mFlinging && mIsScaleBegun)) {
            return;
        }

        ArrayList<PointF> canvasPoints = getCanvasPoints();
        if (canvasPoints.size() < 1) {
            return;
        }

        float zoom = (float) mPdfViewCtrl.getZoom();
        mThicknessDraw = mThickness * zoom;
        mPaint.setStrokeWidth(mThicknessDraw);

        if (canvasPoints.size() == 1) {
            PointF point = canvasPoints.get(0);
            if (mHasFill && mFillColor != Color.TRANSPARENT) {
                canvas.drawPoint(point.x, point.y, mFillPaint);
            }
            if (mStrokeColor != Color.TRANSPARENT) {
                canvas.drawPoint(point.x, point.y, mPaint);
            }
            return;
        }

        drawMarkup(canvas, tfm, canvasPoints);
    }

    @NonNull
    private ArrayList<PointF> getCanvasPoints() {
        ArrayList<PointF> canvasPoints = new ArrayList<>();
        if (mPdfViewCtrl == null || mPagePoints == null || mPagePoints.isEmpty()) {
            return canvasPoints;
        }

        float x, y;
        double[] pts;
        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();
        boolean isContinuous = mPdfViewCtrl.isContinuousPagePresentationMode(mPdfViewCtrl.getPagePresentationMode());
        for (Point pagePoint : mPagePoints) {
            if (isContinuous) {
                pts = mPdfViewCtrl.convPagePtToScreenPt(pagePoint.x, pagePoint.y, mPageNum);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
            } else {
                pts = mPdfViewCtrl.convPagePtToHorizontalScrollingPt(pagePoint.x, pagePoint.y, mPageNum);
                x = (float) pts[0];
                y = (float) pts[1];
            }
            canvasPoints.add(PointFPool.getInstance().obtain(x, y));
        }

        return canvasPoints;
    }

    /**
     * @return True if can clear the vertices
     */
    public boolean canClear() {
        return mPagePoints != null && mPagePoints.size() > 0;
    }

    /**
     * Clears the vertices.
     */
    public void clear() {
        if (mPdfViewCtrl == null || mUndoStack == null || mRedoStack == null || mPagePoints == null) {
            return;
        }

        mUndoStack.push(new Snapshot(mPagePoints, true));
        mRedoStack.clear();
        mPagePoints.clear();

        mPdfViewCtrl.invalidate();
    }

    /**
     * @return True if can undo the last action
     */
    public boolean canUndo() {
        return mUndoStack != null && !mUndoStack.isEmpty();
    }

    /**
     * Undoes the last action.
     */
    public void undo() {
        if (mPdfViewCtrl == null || mUndoStack == null || mRedoStack == null || mUndoStack.isEmpty()) {
            return;
        }

        Snapshot snapshot = mUndoStack.pop();
        mRedoStack.push(snapshot);
        if (snapshot.isRemoved()) {
            mPagePoints.addAll(snapshot.mPagePoints);
        } else {
            mPagePoints.removeAll(snapshot.mPagePoints);
        }

        mPdfViewCtrl.invalidate();
    }

    /**
     * @return True if can redo the last undo
     */
    public boolean canRedo() {
        return mRedoStack != null && !mRedoStack.isEmpty();
    }

    /**
     * Redoes the last undo.
     */
    public void redo() {
        if (mPdfViewCtrl == null || mUndoStack == null || mRedoStack == null || mRedoStack.isEmpty()) {
            return;
        }

        Snapshot snapshot = mRedoStack.pop();
        mUndoStack.push(snapshot);
        if (snapshot.isRemoved()) {
            mPagePoints.removeAll(snapshot.mPagePoints);
        } else {
            mPagePoints.addAll(snapshot.mPagePoints);
        }

        mPdfViewCtrl.invalidate();
    }

    /**
     * Commits creating the annotation.
     */
    public void commit() {
        if (mPdfViewCtrl == null) {
            return;
        }

        if (mPageNum == -1 || mPagePoints.isEmpty()) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            Annot markup = createMarkup(mPdfViewCtrl.getDoc(), mPagePoints);

            setStyle(markup);
            markup.refreshAppearance();
            buildAnnotBBox();

            setAnnot(markup, mPageNum); // now mAnnot = ink

            Page page = mPdfViewCtrl.getDoc().getPage(mPageNum);
            page.annotPushBack(mAnnot);

            mPdfViewCtrl.update(mAnnot, mAnnotPageNum); // Update the region where the annotation occupies.
            buildAnnotBBox();

            raiseAnnotationAddedEvent(mAnnot, mAnnotPageNum);
        } catch (Exception ex) {
            mNextToolMode = ToolMode.PAN;
            ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(ex.getMessage());
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
            onCreateMarkupFailed(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * @return the page number that the annotation is created on
     */
    public int getPageNum() {
        return mPageNum;
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
     * Sets the {@link OnEditToolbarListener} listener.
     *
     * @param listener the {@link OnEditToolbarListener} listener
     */
    public void setOnEditToolbarListener(OnEditToolbarListener listener) {
        mOnEditToolbarListener = listener;
    }

    private static class Snapshot {
        ArrayList<Point> mPagePoints = new ArrayList<>();
        boolean mIsRemoved;

        Snapshot(Point pagePoint) {
            mPagePoints.add(pagePoint);
        }

        Snapshot(List<Point> pagePoints, boolean isRemoved) {
            mPagePoints.addAll(pagePoints);
            mIsRemoved = isRemoved;
        }

        boolean isRemoved() {
            return mIsRemoved;
        }
    }
}
