//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
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
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.PathPool;
import com.pdftron.pdf.utils.PointFPool;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

/**
 * This class is for creating a free highlighter annotation.
 */
@Keep
public class FreeHighlighterCreate extends SimpleShapeCreate {

    private static final String TAG = FreeHighlighterCreate.class.getName();
    private static boolean sDebug;

    private static final float TOUCH_TOLERANCE = 1;
    private static final float BLEND_OPACITY = .8f;

    private ArrayList<PointF> mScreenStrokePoints = new ArrayList<>();
    private ArrayList<PointF> mCanvasStrokePoints = new ArrayList<>();

    private int mPageForFreehandAnnot;
    private boolean mIsStartPointOutsidePage;
    private float mOriginalX, mOriginalY; // the original point in the view space
    private float mMinPointX, mMinPointY, mMaxpointX, mMaxPointY;

    /**
     * Class constructor
     */
    @SuppressWarnings("WeakerAccess")
    public FreeHighlighterCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mNextToolMode = getToolMode();
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.FREE_HIGHLIGHTER;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#setupAnnotProperty(int, float, float, int, String, String)}.
     */
    @Override
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName) {
        // if the stroke has a different style than the previous stroke, update the paint style
        if (mStrokeColor != color || mOpacity != opacity || mThickness != thickness) {
            super.setupAnnotProperty(color, opacity, thickness, fillColor, icon, pdfTronFontName);

            float zoom = (float) mPdfViewCtrl.getZoom();
            mThicknessDraw = mThickness * zoom;
            mPaint.setStrokeWidth(mThicknessDraw);
            color = Utils.getPostProcessedColor(mPdfViewCtrl, mStrokeColor);
            mPaint.setColor(color);
            mPaint.setAlpha((int) (255 * mOpacity * BLEND_OPACITY));
        }
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);

        mOriginalX = mMinPointX = mMaxpointX = mPt1.x;
        mOriginalY = mMinPointY = mMaxPointY = mPt1.y;

        mPageForFreehandAnnot = mPdfViewCtrl.getPageNumberFromScreenPt(e.getX(), e.getY());
        mIsStartPointOutsidePage = mPageForFreehandAnnot < 1;
        if (mIsStartPointOutsidePage) {
            return false;
        }
        // Skip if first point is outside page limits
        if (mPageCropOnClientF != null) {
            if (mPt1.x < mPageCropOnClientF.left ||
                mPt1.x > mPageCropOnClientF.right ||
                mPt1.y < mPageCropOnClientF.top ||
                mPt1.y > mPageCropOnClientF.bottom) {
                setNextToolModeHelper(ToolMode.ANNOT_EDIT);
                return false;
            }
        }

        resetCurrentPaths();
        mCanvasStrokePoints.add(PointFPool.getInstance().obtain(mPt1.x, mPt1.y));
        PointF screenPoint = PointFPool.getInstance().obtain(e.getX(), e.getY());
        mScreenStrokePoints.add(screenPoint);

        mAnnotPushedBack = false;

        return false;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        if (mAllowTwoFingerScroll) {
            return false;
        }

        if (mAllowOneFingerScrollWithStylus) {
            return false;
        }

        if (mIsStartPointOutsidePage) {
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

        float dx = Math.abs(canvasX - mOriginalX);
        float dy = Math.abs(canvasY - mOriginalY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mOriginalX = canvasX;
            mOriginalY = canvasY;

            mScreenStrokePoints.add(PointFPool.getInstance().obtain(x, y));
            mCanvasStrokePoints.add(PointFPool.getInstance().obtain(canvasX, canvasY));

            mMinPointX = Math.min(canvasX, mMinPointX);
            mMinPointY = Math.min(canvasY, mMinPointY);
            mMaxpointX = Math.max(canvasX, mMaxpointX);
            mMaxPointY = Math.max(canvasY, mMaxPointY);

            int minX = (int) (mMinPointX - mThicknessDraw);
            int maxX = (int) Math.ceil(mMinPointY + mThicknessDraw);
            int minY = (int) (mMaxpointX - mThicknessDraw);
            int maxY = (int) Math.ceil(mMaxPointY + mThicknessDraw);

            mPdfViewCtrl.invalidate(minX, minY, maxX, maxY);
        }
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        // We are scrolling
        if (mAllowTwoFingerScroll) {
            doneTwoFingerScrolling();
            return false;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PAGE_SLIDING) {
            return false;
        }

        // If both start point and end point are the same, we don't push back the annotation
        if (mPt1.x == mPt2.x && mPt1.y == mPt2.y) {
            resetPts();
            return true;
        }

        // If annotation was already pushed back, avoid re-entry due to fling motion
        // but allow when creating multiple strokes.
        if (mAnnotPushedBack && mForceSameNextToolMode) {
            return true;
        }

        if (mIsStartPointOutsidePage) {
            return false;
        }

        // In stylus mode, ignore finger input
        mAllowOneFingerScrollWithStylus = mStylusUsed && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS;
        if (mAllowOneFingerScrollWithStylus) {
            return true;
        }

        float x = e.getX();
        float y = e.getY();
        processMotionPoint(x, y);

        mPdfViewCtrl.invalidate(); // to handle when there is no move event between down and up
        createFreeHighlighter();
        resetCurrentPaths();

        setNextToolModeHelper();
        setCurrentDefaultToolModeHelper(getToolMode());
        // add UI to drawing list
        addOldTools();

        mAnnotPushedBack = true;

        return skipOnUpPriorEvent(priorEventMode);
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

        return true;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        if (mIsStartPointOutsidePage) {
            return;
        }

        mPaint.setAlpha((int) (255 * mOpacity * BLEND_OPACITY));

        // draw current stroke
        Path path = createPathFromCanvasPts(mCanvasStrokePoints);
        if (mPdfViewCtrl.isMaintainZoomEnabled()) {
            canvas.save();
            try {
                canvas.translate(0, -mPdfViewCtrl.getScrollYOffsetInTools(mPageForFreehandAnnot));
                canvas.drawPath(path, mPaint);
            } finally {
                canvas.restore();
            }
        } else {
            canvas.drawPath(path, mPaint);
        }
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

    private void createFreeHighlighter() {
        ArrayList<Point> pagePoints = new ArrayList<>(mScreenStrokePoints.size());
        for (PointF point : mScreenStrokePoints) {
            double[] pts = mPdfViewCtrl.convScreenPtToPagePt(point.x, point.y, mPageForFreehandAnnot);
            Point p = new Point();
            p.x = pts[0];
            p.y = pts[1];
            pagePoints.add(p);
        }

        Rect annotRect = Utils.getBBox(pagePoints);
        if (annotRect == null) {
            return;
        }
        annotRect.inflate(mThickness);

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            Ink ink = Ink.create(mPdfViewCtrl.getDoc(), annotRect);
            setStyle(ink);

            int pointIdx = 0;
            for (Point point : pagePoints) {
                ink.setPoint(0, pointIdx++, point);
            }

            ink.refreshAppearance();
            buildAnnotBBox();

            setAnnot(ink, mPageForFreehandAnnot); // now mAnnot = ink

            Page page = mPdfViewCtrl.getDoc().getPage(mPageForFreehandAnnot);
            page.annotPushBack(mAnnot);

            mPdfViewCtrl.update(mAnnot, mAnnotPageNum); // Update the region where the annotation occupies.
            buildAnnotBBox();

            raiseAnnotationAddedEvent(mAnnot, mAnnotPageNum);
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

    private void setStyle(Ink ink) {
        try {
            ink.setBlendMode(Ink.BlendMode.MULTIPLY);

            boolean inkSmoothing = false;
            if (!mIsStylus) {
                inkSmoothing = ((ToolManager) mPdfViewCtrl.getToolManager()).isInkSmoothingEnabled();
            }
            ink.setSmoothing(inkSmoothing);

            ColorPt color = Utils.color2ColorPt(mStrokeColor);
            ink.setColor(color, 3);
            ink.setOpacity(mOpacity);
            Annot.BorderStyle bs = ink.getBorderStyle();
            bs.setWidth(mThickness);
            ink.setBorderStyle(bs);
            setAuthor(ink);
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void resetCurrentPaths() {
        PointFPool.getInstance().recycle(mScreenStrokePoints);
        mScreenStrokePoints.clear();
        PointFPool.getInstance().recycle(mCanvasStrokePoints);
        mCanvasStrokePoints.clear();
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }
}
