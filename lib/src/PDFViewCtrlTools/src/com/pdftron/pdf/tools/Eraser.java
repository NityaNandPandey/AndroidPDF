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
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Ink;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;


/**
 * This class is for erasing annotation. There are two mode provided,
 * can use as a ink eraser as well as a universal annotation eraser.
 */
@SuppressWarnings("WeakerAccess")
@Keep
public class Eraser extends SimpleShapeCreate {

    private boolean mIsStartPointOutsidePage;

    /**
     * This interface can be used to monitor eraser event
     */
    public interface EraserListener {
        void strokeErased();
    }

    private LinkedList<Ink> mInkList;

    enum EraserType {
        INK_ERASER,
        ANNOTATION_ERASER
    }

    private EraserType mEraserType;

    private Path mPath;
    private LinkedList<PointF> mStrokePoints;
    private LinkedList<LinkedList<PointF>> mStrokes;
    private LinkedList<Path> mPaths;
    private LinkedList<Annot> mAnnotList; // used when type is ANNOTATION_ERASER

    private PointF mPt1BBox, mPt2BBox;

    private Point mCurrentPt, mPrevPt;

    private Paint.Join oldStrokeJoin;
    private Paint.Cap oldStrokeCap;

    private EraserListener mListener;

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 1;

    private float mEraserHalfThickness;

    private boolean mDoUpdate = false;
    private boolean mInitializedOnMove;

    /**
     * Class constructor
     */
    @SuppressWarnings("unused")
    public Eraser(@NonNull PDFViewCtrl ctrl) {
        this(ctrl, EraserType.INK_ERASER);
    }

    /**
     * Class constructor
     */
    public Eraser(@NonNull PDFViewCtrl ctrl, EraserType eraserType) {
        super(ctrl);
        mEraserType = eraserType;

        mNextToolMode = ToolMode.INK_ERASER;

        mPath = new Path();
        mStrokePoints = new LinkedList<>();
        mStrokes = new LinkedList<>();
        mPaths = new LinkedList<>();
        mAnnotList = new LinkedList<>();

        mInkList = new LinkedList<>();

        mPt1BBox = new PointF(0, 0);
        mPt2BBox = new PointF(0, 0);

        mCurrentPt = new Point(0, 0);
        mPrevPt = new Point(0, 0);

        oldStrokeJoin = mPaint.getStrokeJoin();
        oldStrokeCap = mPaint.getStrokeCap();
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        if (mEraserType == EraserType.ANNOTATION_ERASER) {
            mEraserHalfThickness = 2;
        } else {
            mEraserHalfThickness = settings.getFloat(getThicknessKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultThickness(mPdfViewCtrl.getContext(), getCreateAnnotType()));
        }
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.INK_ERASER;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER;
    }

    /**
     * Sets the {@link EraserListener} listener.
     *
     * @param listener The listener
     */
    public void setListener(EraserListener listener) {
        mListener = listener;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#setupAnnotProperty(int, float, float, int, String, String)}.
     */
    @Override
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName) {
        mEraserHalfThickness = thickness / 2;

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(getThicknessKey(getCreateAnnotType()), mEraserHalfThickness);
        editor.apply();
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        // We want to customize the paint
        // thus, do not call super.onDown
        // The first touch-down point
        mPt1.x = e.getX() + mPdfViewCtrl.getScrollX();
        mPt1.y = e.getY() + mPdfViewCtrl.getScrollY();

        // The moving point that is the same with the touch-down point initially
        mPt2.set(mPt1);

        // Remembers which page is touched initially and that is the page where
        // the annotation is going to reside on.
        mDownPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(e.getX(), e.getY());
        if (mDownPageNum < 1) {
            mIsStartPointOutsidePage = true;
            mDownPageNum = mPdfViewCtrl.getCurrentPage();
        } else {
            mIsStartPointOutsidePage = false;
        }
        if (mDownPageNum >= 1) {
            mPageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mDownPageNum);
        }
        mThickness = mEraserHalfThickness * 2;
        float zoom = (float) mPdfViewCtrl.getZoom();
        mThicknessDraw = mThickness * zoom;
        mPaint.setStrokeWidth(mThicknessDraw);
        mPaint.setColor(Color.LTGRAY);
        mPaint.setAlpha((int) (255 * 0.7));

        // Skip if first point is outside page limits
        if (mPageCropOnClientF != null) {
            if (mPt1.x < mPageCropOnClientF.left ||
                    mPt1.x > mPageCropOnClientF.right ||
                    mPt1.y < mPageCropOnClientF.top ||
                    mPt1.y > mPageCropOnClientF.bottom) {
                mInitializedOnMove = false;
                setNextToolModeHelper(ToolMode.PAN);
                return false;
            }
        }

        mInitializedOnMove = true;

        mPt1BBox.set(mPt1);
        mPt2BBox.set(mPt2);

        mPath.moveTo(mPt1.x, mPt1.y);
        mX = mPt1.x;
        mY = mPt1.y;
        mStrokePoints = new LinkedList<>();
        mStrokePoints.add(new PointF(mPt1.x, mPt1.y));

        // Points to be used for erasing
        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();

        mCurrentPt.x = mPt1.x - sx;
        mCurrentPt.y = mPt1.y - sy;
        mPrevPt.x = mPt1.x - sx;
        mPrevPt.y = mPt1.y - sy;

        return false;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        if (mAllowTwoFingerScroll) {
            mPath.reset();
            return false;
        }

        if (!mInitializedOnMove) {
            return false;
        }

        float x = e2.getX() + mPdfViewCtrl.getScrollX();
        float y = e2.getY() + mPdfViewCtrl.getScrollY();

        // Don't allow the annotation to go beyond the page
        if (mPageCropOnClientF != null) {
            if (x < mPageCropOnClientF.left) {
                x = mPageCropOnClientF.left;
            } else if (x > mPageCropOnClientF.right) {
                x = mPageCropOnClientF.right;
            }
            if (y < mPageCropOnClientF.top) {
                y = mPageCropOnClientF.top;
            } else if (y > mPageCropOnClientF.bottom) {
                y = mPageCropOnClientF.bottom;
            }
        }

        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (mEraserType == EraserType.ANNOTATION_ERASER || dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;

            mStrokePoints.add(new PointF(x, y));

            float sx = mPdfViewCtrl.getScrollX();
            float sy = mPdfViewCtrl.getScrollY();

            mCurrentPt.x = x - sx;
            mCurrentPt.y = y - sy;

            mPt1.x = Math.min(Math.min(x, mPt1.x), mPt1.x);
            mPt1.y = Math.min(Math.min(y, mPt1.y), mPt1.y);
            mPt2.x = Math.max(Math.max(x, mPt2.x), mPt2.x);
            mPt2.y = Math.max(Math.max(y, mPt2.y), mPt2.y);

            mPt1BBox.x = Math.min(Math.min(mPt1.x, mPt1BBox.x), mPt1BBox.x);
            mPt1BBox.y = Math.min(Math.min(mPt1.y, mPt1BBox.y), mPt1BBox.y);
            mPt2BBox.x = Math.max(Math.max(mPt2.x, mPt2BBox.x), mPt2BBox.x);
            mPt2BBox.y = Math.max(Math.max(mPt2.y, mPt2BBox.y), mPt2BBox.y);

            float min_x = mPt1BBox.x - mThicknessDraw;
            float max_x = mPt2BBox.x + mThicknessDraw;
            float min_y = mPt1BBox.y - mThicknessDraw;
            float max_y = mPt2BBox.y + mThicknessDraw;

            mPdfViewCtrl.invalidate((int) min_x, (int) min_y, (int) Math.ceil(max_x), (int) Math.ceil(max_y));

            // Erase
            double[] pt1points = mPdfViewCtrl.convScreenPtToPagePt(mPrevPt.x, mPrevPt.y, mDownPageNum);
            double[] pt2points = mPdfViewCtrl.convScreenPtToPagePt(mCurrentPt.x, mCurrentPt.y, mDownPageNum);
            Point pdfPt1 = new Point(pt1points[0], pt1points[1]);
            Point pdfPt2 = new Point(pt2points[0], pt2points[1]);

            boolean shouldUnlock = false;
            try {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                Page page = mPdfViewCtrl.getDoc().getPage(mDownPageNum);
                switch (mEraserType) {
                    case INK_ERASER:
                        int annot_num = page.getNumAnnots();
                        for (int i = annot_num - 1; i >= 0; --i) {
                            Annot annot = page.getAnnot(i);
                            if (!annot.isValid())
                                continue;
                            if (annot.getType() == Annot.e_Ink) {
                                Ink ink = new Ink(annot);
                                if (ink.erase(pdfPt1, pdfPt2, mEraserHalfThickness)) {
                                    if (!mInkList.contains(ink)) {
                                        mInkList.add(ink);
                                        mDoUpdate = true;
                                    }
                                }
                            }
                        }
                        break;
                    case ANNOTATION_ERASER:
                        ArrayList<Annot> annots = mPdfViewCtrl.getAnnotationListAt(
                                (int) mPrevPt.x, (int) mPrevPt.y, (int) mCurrentPt.x, (int) mCurrentPt.y);
                        for (Annot annot : annots) {
                            if (!mAnnotList.contains(annot)) {
                                mAnnotList.add(annot);
                                mDoUpdate = true;
                            }
                        }
                        break;
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
                mPrevPt.x = mCurrentPt.x;
                mPrevPt.y = mCurrentPt.y;
            }
        }

        return true;
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

        if (!mInitializedOnMove) {
            return false;
        }

        mPaths.add(mPath);
        mPath = new Path();

        boolean shouldUnlock = false;
        try {
            setNextToolModeHelper(ToolMode.PAN);

            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (mStrokePoints.size() == 1) {
                // Erase
                double[] pt1points = mPdfViewCtrl.convScreenPtToPagePt(mPrevPt.x, mPrevPt.y, mDownPageNum);
                Point pdfPt1 = new Point(pt1points[0], pt1points[1]);

                try {
                    switch (mEraserType) {
                        case INK_ERASER:
                            Page page = mPdfViewCtrl.getDoc().getPage(mDownPageNum);
                            int annot_num = page.getNumAnnots();
                            for (int i = annot_num - 1; i >= 0; --i) {
                                Annot annot = page.getAnnot(i);
                                if (!annot.isValid())
                                    continue;
                                if (annot.getType() == Annot.e_Ink) {
                                    Ink ink = new Ink(annot);
                                    if (ink.erase(pdfPt1, pdfPt1, mEraserHalfThickness)) {
                                        if (!mInkList.contains(ink)) {
                                            mInkList.add(ink);
                                            mDoUpdate = true;
                                        }
                                    }
                                }
                            }
                            break;
                        case ANNOTATION_ERASER:
                            ArrayList<Annot> annots = mPdfViewCtrl.getAnnotationListAt(
                                    (int) mPrevPt.x, (int) mPrevPt.y, (int) mPrevPt.x, (int) mPrevPt.y);
                            for (Annot annot : annots) {
                                if (!mAnnotList.contains(annot)) {
                                    mAnnotList.add(annot);
                                    mDoUpdate = true;
                                }
                            }
                            break;
                    }
                } catch (Exception ignored) {

                }
            }

            if (commitAnnotation()) {
                // add UI to drawing list
                addOldTools();
            }

            if (!mForceSameNextToolMode) {
                mPaint.setStrokeJoin(oldStrokeJoin);
                mPaint.setStrokeCap(oldStrokeCap);
            }

        } catch (Exception ex) {
            mNextToolMode = ToolMode.PAN;
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
            mStrokePoints.clear();
            mStrokes.clear();
            mInkList.clear();
            mAnnotList.clear();
        }

        if (mDoUpdate) {
            if (mListener != null) {
                mListener.strokeErased();
            }
        }

        if (mForceSameNextToolMode) {
            erasePaths();
        }

        mDoUpdate = false;

        return skipOnUpPriorEvent(priorEventMode);
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#createMarkup(PDFDoc, Rect)}.
     */
    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        return null;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#doneTwoFingerScrolling()}.
     */
    @Override
    protected void doneTwoFingerScrolling() {
        super.doneTwoFingerScrolling();

        erasePaths();
    }

    /**
     * Erases paths.
     */
    protected void erasePaths() {
        mPaths.clear();
        mPath.reset();

        float min_x = mPt1BBox.x - mThicknessDraw;
        float max_x = mPt2BBox.x + mThicknessDraw;
        float min_y = mPt1BBox.y - mThicknessDraw;
        float max_y = mPt2BBox.y + mThicknessDraw;

        mPdfViewCtrl.invalidate((int) min_x, (int) min_y, (int) Math.ceil(max_x), (int) Math.ceil(max_y));
    }

    /**
     * Commits all changes to the annotation.
     *
     * @return True if needs render
     */
    protected boolean commitAnnotation() {
        boolean needsRender = false;
        try {
            Page page = mPdfViewCtrl.getDoc().getPage(mDownPageNum);
            switch (mEraserType) {
                case INK_ERASER:
                    mAnnotPageNum = mDownPageNum;
                    HashMap<Annot, Integer> annots = new HashMap<>();
                    boolean annotsRemoved = true;
                    for (Ink ink : mInkList) {
                        if (ink.getPathCount() != 0) {
                            for (int i = 0; i < ink.getPathCount(); i++) {
                                if (ink.getPointCount(i) > 0) {
                                    annotsRemoved = false;
                                    break;
                                }
                            }
                        }
                        annots.put(ink, mAnnotPageNum);
                    }
                    if (annotsRemoved) {
                        raiseAnnotationPreRemoveEvent(annots);
                    } else {
                        raiseAnnotationPreModifyEvent(annots);
                    }

                    for (Ink ink : mInkList) {
                        Rect bbox = ink.getRect();
                        if (ink.getPathCount() == 0) {
                            page.annotRemove(ink);
                        } else {
                            boolean annotRemoved = true;
                            for (int i = 0; i < ink.getPathCount(); i++) {
                                if (ink.getPointCount(i) > 0) {
                                    annotRemoved = false;
                                    break;
                                }
                            }
                            if (annotRemoved) {
                                page.annotRemove(ink);
                            } else {
                                ink.refreshAppearance();
                            }
                        }
                        double[] screenPt1 = mPdfViewCtrl.convPagePtToScreenPt(bbox.getX1(), bbox.getY1(), mDownPageNum);
                        double[] screenPt2 = mPdfViewCtrl.convPagePtToScreenPt(bbox.getX2(), bbox.getY2(), mDownPageNum);
                        Rect screenBox = new Rect(screenPt1[0], screenPt1[1], screenPt2[0], screenPt2[1]);
                        mPdfViewCtrl.update(screenBox);
                        needsRender = true;
                    }

                    if (annotsRemoved) {
                        raiseAnnotationRemovedEvent(annots);
                    } else {
                        raiseAnnotationModifiedEvent(annots);
                    }
                    break;
                case ANNOTATION_ERASER:
                    for (Annot annot : mAnnotList) {
                        raiseAnnotationPreRemoveEvent(annot, mAnnotPageNum); // shouldn't we call it all together instead of one by one
                        page.annotRemove(annot);
                        Rect bbox = annot.getRect();
                        double[] screenPt1 = mPdfViewCtrl.convPagePtToScreenPt(bbox.getX1(), bbox.getY1(), mDownPageNum);
                        double[] screenPt2 = mPdfViewCtrl.convPagePtToScreenPt(bbox.getX2(), bbox.getY2(), mDownPageNum);
                        Rect screenBox = new Rect(screenPt1[0], screenPt1[1], screenPt2[0], screenPt2[1]);
                        mPdfViewCtrl.update(screenBox);
                        needsRender = true;
                        raiseAnnotationRemovedEvent(annot, mAnnotPageNum);
                    }
                    break;
            }

        } catch (Exception ignored) {

        }
        return needsRender;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        if (mAllowTwoFingerScroll || mIsStartPointOutsidePage) {
            return;
        }
        for (Path p : mPaths) {
            canvas.drawPath(p, mPaint);
        }
        canvas.drawPath(mPath, mPaint);
    }
}
