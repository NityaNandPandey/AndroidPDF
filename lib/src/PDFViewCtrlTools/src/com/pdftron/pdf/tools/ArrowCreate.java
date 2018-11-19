//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Line;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.DrawingUtils;

/**
 * This class is for creating an arrow.
 */
@Keep
public class ArrowCreate extends SimpleShapeCreate {

    protected PointF mPt3, mPt4;
    protected Path mOnDrawPath = new Path();

    protected double mZoom;

    /**
     * Class constructor
     */
    public ArrowCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mNextToolMode = getToolMode();

        mPt3 = new PointF(0, 0);
        mPt4 = new PointF(0, 0);

        mZoom = ctrl.getZoom();
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.ARROW_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        if(super.onDown(e)){
            return true;
        }

        mZoom = mPdfViewCtrl.getZoom();

        calculateEndingStyle();

        return false;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        // We are scrolling
        if (mAllowTwoFingerScroll) {
            return false;
        }
        if (mAllowOneFingerScrollWithStylus) {
            return false;
        }
        // During moving, update the arrow shape and the bounding box. note that for the
        // bounding box, we need to include the previous bounding box in the new bounding box
        // so that the previously drawn arrow will go away.
        float min_x = Math.min(Math.min(Math.min(mPt1.x, mPt2.x), mPt3.x), mPt4.x);
        float max_x = Math.max(Math.max(Math.max(mPt1.x, mPt2.x), mPt3.x), mPt4.x);
        float min_y = Math.min(Math.min(Math.min(mPt1.y, mPt2.y), mPt3.y), mPt4.y);
        float max_y = Math.max(Math.max(Math.max(mPt1.y, mPt2.y), mPt3.y), mPt4.y);

        mPt2.x = e2.getX() + mPdfViewCtrl.getScrollX();
        mPt2.y = e2.getY() + mPdfViewCtrl.getScrollY();

        // Don't allow the annotation to go beyond the page
        if (mPageCropOnClientF != null) {
            if (mPt2.x < mPageCropOnClientF.left) {
                mPt2.x = mPageCropOnClientF.left;
            } else if (mPt2.x > mPageCropOnClientF.right) {
                mPt2.x = mPageCropOnClientF.right;
            }
            if (mPt2.y < mPageCropOnClientF.top) {
                mPt2.y = mPageCropOnClientF.top;
            } else if (mPt2.y > mPageCropOnClientF.bottom) {
                mPt2.y = mPageCropOnClientF.bottom;
            }
        }

        calculateEndingStyle();

        min_x = Math.min(Math.min(min_x, mPt3.x), mPt4.x) - mThicknessDraw;
        max_x = Math.max(Math.max(max_x, mPt3.x), mPt4.x) + mThicknessDraw;
        min_y = Math.min(Math.min(min_y, mPt3.y), mPt4.y) - mThicknessDraw;
        max_y = Math.max(Math.max(max_y, mPt3.y), mPt4.y) + mThicknessDraw;
        mPdfViewCtrl.invalidate((int) min_x, (int) min_y, (int) Math.ceil(max_x), (int) Math.ceil(max_y));
        return true;
    }

    protected void calculateEndingStyle() {
        DrawingUtils.calcArrow(mPt1, mPt2, mPt3, mPt4, mThickness, mZoom);
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#doneTwoFingerScrolling()}.
     */
    @Override
    protected void doneTwoFingerScrolling() {
        super.doneTwoFingerScrolling();

        mPt2.set(mPt1);
        mPt3.set(mPt1);
        mPt4.set(mPt1);
        mPdfViewCtrl.invalidate();
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#createMarkup(PDFDoc, Rect)}.
     */
    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        Line line = Line.create(doc, bbox);
        line.setEndStyle(com.pdftron.pdf.annots.Line.e_OpenArrow);
        return line;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#resetPts()}.
     */
    @Override
    protected void resetPts() {
        mPt1.set(0,0);
        mPt2.set(0,0);
        mPt3.set(0,0);
        mPt4.set(0,0);
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#getDefaultNextTool()}.
     */
    @Override
    protected ToolMode getDefaultNextTool() {
        return ToolMode.ANNOT_EDIT_LINE;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onCreateMarkupFailed(Exception)}.
     */
    @Override
    protected void onCreateMarkupFailed(Exception e) {
        float min_x = Math.min(Math.min(Math.min(mPt1.x, mPt2.x), mPt3.x), mPt4.x) - mThicknessDraw;
        float max_x = Math.max(Math.max(Math.max(mPt1.x, mPt2.x), mPt3.x), mPt4.x) + mThicknessDraw;
        float min_y = Math.min(Math.min(Math.min(mPt1.y, mPt2.y), mPt3.y), mPt4.y) - mThicknessDraw;
        float max_y = Math.max(Math.max(Math.max(mPt1.y, mPt2.y), mPt3.y), mPt4.y) + mThicknessDraw;
        mPdfViewCtrl.postInvalidate((int) min_x, (int) min_y, (int) Math.ceil(max_x), (int) Math.ceil(max_y));
    }



    /**
     * The overload implementation of {@link SimpleShapeCreate#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        // We are scrolling
        if (mAllowTwoFingerScroll) {
            return;
        }
        if (mIsAllPointsOutsidePage) {
            return;
        }

        DrawingUtils.drawArrow(canvas, mPt1, mPt2,
                mPt3, mPt4, mOnDrawPath, mPaint);
    }
}
