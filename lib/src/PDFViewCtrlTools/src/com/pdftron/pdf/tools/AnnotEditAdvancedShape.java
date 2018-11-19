//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.annots.PolyLine;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.Utils;

/**
 * This class is responsible for editing a selected advanced shape such as polyline, polygon and cloud.
 */
@SuppressWarnings("WeakerAccess")
@Keep
public class AnnotEditAdvancedShape extends AnnotEdit {

    public static final int CALLOUT_END_POINT_INDEX = 10;
    public static final int CALLOUT_KNEE_POINT_INDEX = 9;
    public static final int CALLOUT_START_POINT_INDEX = 8;

    private static final int SIDE_X1Y1_X1Y2 = 1;
    private static final int SIDE_X1Y1_X2Y1 = 2;
    private static final int SIDE_X1Y2_X2Y2 = 3;
    private static final int SIDE_X2Y1_X2Y2 = 4;

    PolyLine mPoly;
    FreeText mCallout;
    private Path mPath = new Path();

    /**
     * Class constructor
     */
    public AnnotEditAdvancedShape(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        setOriginalCtrlPtsDisabled(true);
    }

    /**
     * The overload implementation of {@link AnnotEdit#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.ANNOT_EDIT_ADVANCED_SHAPE;
    }

    /**
     * The overload implementation of {@link AnnotEdit#onCreate()}.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (mAnnot != null) {
            try {
                if (mAnnot.getType() == Annot.e_Polygon ||
                    mAnnot.getType() == Annot.e_Polyline) {
                    mPoly = new PolyLine(mAnnot);
                    if (mPoly.isValid()) {
                        CTRL_PTS_CNT = mPoly.getVertexCount();
                    }
                } else if (mAnnot.getType() == Annot.e_FreeText) {
                    mCallout = new FreeText(mAnnot);
                    if (AnnotUtils.isCallout(mAnnot)) {
                        setOriginalCtrlPtsDisabled(false);
                        int count = 0;
                        if (mCallout.getCalloutLinePoint1() != null) {
                            count++;
                        }
                        if (mCallout.getCalloutLinePoint2() != null) {
                            count++;
                        }
                        if (mCallout.getCalloutLinePoint3() != null) {
                            count++;
                        }
                        CTRL_PTS_CNT = RECTANGULAR_CTRL_PTS_CNT + count;
                    }
                }
                if (CTRL_PTS_CNT > RECTANGULAR_CTRL_PTS_CNT) {
                    mCtrlPts = new PointF[CTRL_PTS_CNT];
                    mCtrlPtsOnDown = new PointF[CTRL_PTS_CNT];
                    for (int i = 0; i < CTRL_PTS_CNT; ++i) {
                        mCtrlPts[i] = new PointF();
                        mCtrlPtsOnDown[i] = new PointF();
                    }
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * The overload implementation of {@link AnnotEdit#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1,
                          MotionEvent e2,
                          float x_dist,
                          float y_dist) {
        if (!super.onMove(e1, e2, x_dist, y_dist)) {
            return false;
        }

        if (mEffCtrlPtId >= 0) {
            if (mCallout != null && mEffCtrlPtId < RECTANGULAR_CTRL_PTS_CNT) {
                return true;
            }
            RectF tempRect = new RectF(mBBox);
            float totalMoveX = e2.getX() - e1.getX();
            float totalMoveY = e2.getY() - e1.getY();
            float thresh = 2f * mCtrlRadius;
            float left = mBBoxOnDown.left + mCtrlRadius;
            float right = mBBoxOnDown.right - mCtrlRadius;
            float top = mBBoxOnDown.top + mCtrlRadius;
            float bottom = mBBoxOnDown.bottom - mCtrlRadius;

            float newX = mCtrlPtsOnDown[mEffCtrlPtId].x + totalMoveX;
            float newY = mCtrlPtsOnDown[mEffCtrlPtId].y + totalMoveY;
            boolean valid = true;
            for (int i = 0; i < CTRL_PTS_CNT; ++i) {
                if (i != mEffCtrlPtId) {
                    if (Math.abs(newX - mCtrlPtsOnDown[i].x) < thresh
                        && Math.abs(newY - mCtrlPtsOnDown[i].y) < thresh) {
                        valid = false;
                        break;
                    }
                }
            }
            if (mCallout != null && mEffCtrlPtId == CALLOUT_END_POINT_INDEX) {
                // for callout disable editing of end point
                valid = false;
            }
            if (valid) {
                mCtrlPts[mEffCtrlPtId].x = newX;
                mCtrlPts[mEffCtrlPtId].y = newY;

                if (mCallout != null && mEffCtrlPtId == CALLOUT_KNEE_POINT_INDEX) {
                    snapCalloutPoint();
                }

                left = Math.min(left, newX);
                right = Math.max(right, newX);
                top = Math.min(top, newY);
                bottom = Math.max(bottom, newY);
                updateCtrlPts(false, left, right, top, bottom, mBBox);
                mModifiedAnnot = true;

                float min_x = Math.min(tempRect.left, mBBox.left);
                float max_x = Math.max(tempRect.right, mBBox.right);
                float min_y = Math.min(tempRect.top, mBBox.top);
                float max_y = Math.max(tempRect.bottom, mBBox.bottom);
                mPdfViewCtrl.invalidate((int) min_x - 1, (int) min_y - 1, (int) Math.ceil(max_x) + 1, (int) Math.ceil(max_y) + 1);
            }
        }
        return true;
    }

    /**
     * The overload implementation of {@link AnnotEdit#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas,
                       Matrix tfm) {
        super.onDraw(canvas, tfm);

        if (mAnnot == null || (mHideCtrlPts && mCallout == null)) {
            return;
        }

        PathEffect pathEffect = mPaint.getPathEffect();
        mPaint.setColor(mPdfViewCtrl.getResources().getColor(R.color.tools_annot_edit_line_shadow));
        mPaint.setStyle(Paint.Style.STROKE);
//            mPaint.setPathEffect(mDashPathEffect);

        mPath.reset();

        if (mCallout != null) {
            try {
                mPath.moveTo(mCtrlPts[CALLOUT_START_POINT_INDEX].x, mCtrlPts[CALLOUT_START_POINT_INDEX].y);
                mPath.lineTo(mCtrlPts[CALLOUT_KNEE_POINT_INDEX].x, mCtrlPts[CALLOUT_KNEE_POINT_INDEX].y);
                mPath.lineTo(mCtrlPts[CALLOUT_END_POINT_INDEX].x, mCtrlPts[CALLOUT_END_POINT_INDEX].y);
                canvas.drawPath(mPath, mPaint);
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        } else if (mModifiedAnnot && mEffCtrlPtId >= 0) {
            try {
                if (mEffCtrlPtId != 0) {
                    mPath.moveTo(mCtrlPtsOnDown[mEffCtrlPtId - 1].x, mCtrlPtsOnDown[mEffCtrlPtId - 1].y);
                    mPath.lineTo(mCtrlPts[mEffCtrlPtId].x, mCtrlPts[mEffCtrlPtId].y);
                } else if (mAnnot.getType() == Annot.e_Polygon) {
                    mPath.moveTo(mCtrlPtsOnDown[CTRL_PTS_CNT - 1].x, mCtrlPtsOnDown[CTRL_PTS_CNT - 1].y);
                    mPath.lineTo(mCtrlPts[mEffCtrlPtId].x, mCtrlPts[mEffCtrlPtId].y);
                } else {
                    mPath.moveTo(mCtrlPts[mEffCtrlPtId].x, mCtrlPts[mEffCtrlPtId].y);
                }

                if (mEffCtrlPtId != CTRL_PTS_CNT - 1) {
                    mPath.lineTo(mCtrlPtsOnDown[mEffCtrlPtId + 1].x, mCtrlPtsOnDown[mEffCtrlPtId + 1].y);
                } else if (mAnnot.getType() == Annot.e_Polygon) {
                    mPath.lineTo(mCtrlPtsOnDown[0].x, mCtrlPtsOnDown[0].y);
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
            canvas.drawPath(mPath, mPaint);
        }

        // reset path effect
        mPaint.setPathEffect(pathEffect);

        mPaint.setStrokeWidth(1);

        // skip the last ctrl point for callout
        if (!mHideCtrlPts) {
            DrawingUtils.drawCtrlPtsAdvancedShape(mPdfViewCtrl.getResources(), canvas,
                mPaint, mCtrlPts, mCtrlRadius, mHasSelectionPermission, mCallout != null);
        }
    }

    /**
     * The overload implementation of {@link AnnotEdit#setCtrlPts()}.
     */
    @Override
    protected void setCtrlPts() {
        super.setCtrlPts();

        if (mPdfViewCtrl == null ||
            (mPoly == null && mCallout == null) ||
            onInterceptAnnotationHandling(mAnnot)) {
            return;
        }

        addAnnotView();

        float x, y;
        double[] pts;
        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();
        boolean isContinuous = mPdfViewCtrl.isContinuousPagePresentationMode(mPdfViewCtrl.getPagePresentationMode());
        PointF[] vertices = new PointF[CTRL_PTS_CNT];
        try {
            if (mPoly != null) {
                for (int i = 0; i < CTRL_PTS_CNT; ++i) {
                    Point pagePoint = mPoly.getVertex(i);
                    if (isContinuous) {
                        pts = mPdfViewCtrl.convPagePtToScreenPt(pagePoint.x, pagePoint.y, mAnnotPageNum);
                        x = (float) pts[0] + sx;
                        y = (float) pts[1] + sy;
                    } else {
                        pts = mPdfViewCtrl.convPagePtToHorizontalScrollingPt(pagePoint.x, pagePoint.y, mAnnotPageNum);
                        x = (float) pts[0];
                        y = (float) pts[1];
                    }
                    mCtrlPts[i].x = mCtrlPtsOnDown[i].x = x;
                    mCtrlPts[i].y = mCtrlPtsOnDown[i].y = y;
                    vertices[i] = new PointF(mCtrlPts[i].x - sx, mCtrlPts[i].y - sy);
                }
            } else if (mCallout != null) {
                Point pt1 = mCallout.getCalloutLinePoint1();
                Point pt2 = mCallout.getCalloutLinePoint2();
                Point pt3 = mCallout.getCalloutLinePoint3();
                int i = RECTANGULAR_CTRL_PTS_CNT;
                if (pt1 != null) {
                    setCalloutPoint(pt1, i, vertices, sx, sy);
                    i++;
                }
                if (pt2 != null) {
                    setCalloutPoint(pt2, i, vertices, sx, sy);
                    i++;
                }
                if (pt3 != null) {
                    setCalloutPoint(pt3, i, vertices, sx, sy);
                }
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        if (mAnnotView != null && mAnnotView.getDrawingView() != null) {
            mAnnotView.getDrawingView().setVertices(vertices);
            mAnnotView.getDrawingView().setPageNum(mAnnotPageNum);
        }
        setBBoxFromAllVertices();
    }

    private void setCalloutPoint(Point pt, int i, PointF[] vertices, float sx, float sy) {
        double[] pts = mPdfViewCtrl.convPagePtToScreenPt(pt.x, pt.y, mAnnotPageNum);
        float x = (float) pts[0] + sx;
        float y = (float) pts[1] + sy;
        mCtrlPts[i].x = mCtrlPtsOnDown[i].x = x;
        mCtrlPts[i].y = mCtrlPtsOnDown[i].y = y;
        vertices[i] = new PointF((float) pts[0], (float) pts[1]);
    }

    private void setBBoxFromAllVertices() {
        if (mCallout != null) {
            // for callout, we need the bbox adjust for stroke thickness and joints
            // thus we will obtain from core after refreshAppearance instead
            return;
        }
        float x, y;
        float left, right, top, bottom;
        left = right = mCtrlPts[0].x;
        top = bottom = mCtrlPts[0].y;
        for (int i = 0; i < CTRL_PTS_CNT; ++i) {
            x = mCtrlPts[i].x;
            y = mCtrlPts[i].y;
            if (x < left) {
                left = x;
            }
            if (x > right) {
                right = x;
            }
            if (y < top) {
                top = y;
            }
            if (y > bottom) {
                bottom = y;
            }
        }
        mBBox.left = left - mCtrlRadius;
        mBBox.top = top - mCtrlRadius;
        mBBox.right = right + mCtrlRadius;
        mBBox.bottom = bottom + mCtrlRadius;

        updateAnnotView();
    }

    /**
     * The overload implementation of {@link AnnotEdit#updateCtrlPts(boolean, float, float, float, float, RectF)}.
     */
    @Override
    protected void updateCtrlPts(boolean translate,
                                 float left,
                                 float right,
                                 float top,
                                 float bottom,
                                 RectF which) {
        super.updateCtrlPts(translate, left, right, top, bottom, which);

        int xOffset = mPdfViewCtrl.getScrollX();
        int yOffset = mPdfViewCtrl.getScrollY();

        if (mEffCtrlPtId == e_moving) {
            float w = mBBox.left - mBBoxOnDown.left;
            float h = mBBox.top - mBBoxOnDown.top;
            for (int i = 0; i < CTRL_PTS_CNT; ++i) {
                mCtrlPts[i].x = mCtrlPtsOnDown[i].x + w;
                mCtrlPts[i].y = mCtrlPtsOnDown[i].y + h;

                if (mAnnotView != null && mAnnotView.getDrawingView() != null) {
                    mAnnotView.getDrawingView().getVertices().set(i,
                        new PointF(mCtrlPts[i].x - xOffset, mCtrlPts[i].y - yOffset));
                    mAnnotView.getDrawingView().getCtrlPts()[i].x = mCtrlPts[i].x - xOffset;
                    mAnnotView.getDrawingView().getCtrlPts()[i].y = mCtrlPts[i].y - yOffset;
                }
            }
            updateAnnotView();
        } else if (mEffCtrlPtId >= 0) {
            float totalMoveX = mCtrlPts[mEffCtrlPtId].x - mCtrlPtsOnDown[mEffCtrlPtId].x;
            float totalMoveY = mCtrlPts[mEffCtrlPtId].y - mCtrlPtsOnDown[mEffCtrlPtId].y;
            float x = mCtrlPts[mEffCtrlPtId].x = mCtrlPtsOnDown[mEffCtrlPtId].x + totalMoveX;
            float y = mCtrlPts[mEffCtrlPtId].y = mCtrlPtsOnDown[mEffCtrlPtId].y + totalMoveY;
            mCtrlPts[mEffCtrlPtId].x = Math.max(mPageCropOnClientF.left,
                Math.min(mPageCropOnClientF.right, x));
            mCtrlPts[mEffCtrlPtId].y = Math.max(mPageCropOnClientF.top,
                Math.min(mPageCropOnClientF.bottom, y));

            if (mAnnotView != null && mAnnotView.getDrawingView() != null) {
                mAnnotView.getDrawingView().getVertices().set(mEffCtrlPtId,
                    new PointF(mCtrlPts[mEffCtrlPtId].x - xOffset, mCtrlPts[mEffCtrlPtId].y - yOffset));
                mAnnotView.getDrawingView().getCtrlPts()[mEffCtrlPtId].x = mCtrlPts[mEffCtrlPtId].x - xOffset;
                mAnnotView.getDrawingView().getCtrlPts()[mEffCtrlPtId].y = mCtrlPts[mEffCtrlPtId].y - yOffset;
            }

            // update BBox
            // check if this vertex is on bounding box, if so, we may need to loop through
            // all vertices to obtain the new bounding box
            boolean updateFromAllVertices = x != mCtrlPts[mEffCtrlPtId].x || y != mCtrlPts[mEffCtrlPtId].y;
            if (Math.abs(mCtrlPtsOnDown[mEffCtrlPtId].x - mCtrlRadius - mBBoxOnDown.left) < 1) {
                updateFromAllVertices = true;
            }
            if (Math.abs(mCtrlPtsOnDown[mEffCtrlPtId].x + mCtrlRadius - mBBoxOnDown.right) < 1) {
                updateFromAllVertices = true;
            }
            if (Math.abs(mCtrlPtsOnDown[mEffCtrlPtId].y - mCtrlRadius - mBBoxOnDown.top) < 1) {
                updateFromAllVertices = true;
            }
            if (Math.abs(mCtrlPtsOnDown[mEffCtrlPtId].y + mCtrlRadius - mBBoxOnDown.bottom) < 1) {
                updateFromAllVertices = true;
            }
            if (updateFromAllVertices) {
                setBBoxFromAllVertices();
            } else {
                which.left = left - mCtrlRadius;
                which.top = top - mCtrlRadius;
                which.right = right + mCtrlRadius;
                which.bottom = bottom + mCtrlRadius;

                updateAnnotView();
            }
        }
    }

    private void snapCalloutPoint() {
        double ax = mCtrlPts[CALLOUT_KNEE_POINT_INDEX].x;
        double ay = mCtrlPts[CALLOUT_KNEE_POINT_INDEX].y;

        double x1 = mContentBox.left + mCtrlRadius;
        double y1 = mContentBox.bottom - mCtrlRadius;
        double x2 = mContentBox.right - mCtrlRadius;
        double y2 = mContentBox.top + mCtrlRadius;

        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;

        double[] distances = new double[]{
            Math.abs(Utils.calcDistance(x1, midY, ax, ay)),
            Math.abs(Utils.calcDistance(midX, y1, ax, ay)),
            Math.abs(Utils.calcDistance(midX, y2, ax, ay)),
            Math.abs(Utils.calcDistance(x2, midY, ax, ay))
        };

        double x = -1;
        double y = -1;
        int minIndex = Utils.findMinIndex(distances);
        switch (minIndex + 1) {
            case SIDE_X1Y1_X1Y2:
                x = x1;
                y = midY;
                break;
            case SIDE_X1Y1_X2Y1:
                x = midX;
                y = y1;
                break;
            case SIDE_X1Y2_X2Y2:
                x = midX;
                y = y2;
                break;
            case SIDE_X2Y1_X2Y2:
                x = x2;
                y = midY;
                break;
        }

        if (x >= 0 && y >= 0) {
            mCtrlPts[CALLOUT_END_POINT_INDEX].x = (float) x;
            mCtrlPts[CALLOUT_END_POINT_INDEX].y = (float) y;
        }
    }

    /**
     * The overload implementation of {@link AnnotEdit#updateAnnot()}.
     */
    @Override
    protected void updateAnnot()
        throws PDFNetException {

        if (mAnnot == null || onInterceptAnnotationHandling(mAnnot)
            || mPdfViewCtrl == null || mCtrlPts == null || (mPoly == null && mCallout == null)
            || mEffCtrlPtId == e_unknown) {
            return;
        }

        Rect oldUpdateRect = getOldAnnotScreenPosition();

        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();
        if (mEffCtrlPtId == e_moving) {
            float deltaX = mBBox.left - mBBoxOnDown.left;
            float deltaY = mBBox.top - mBBoxOnDown.top;
            float x, y;
            double[] pts;
            boolean isContinuous = mPdfViewCtrl.isContinuousPagePresentationMode(mPdfViewCtrl.getPagePresentationMode());
            if (mPoly != null) {
                for (int i = 0; i < CTRL_PTS_CNT; ++i) {
                    Point pagePoint = mPoly.getVertex(i);
                    if (isContinuous) {
                        pts = mPdfViewCtrl.convPagePtToScreenPt(pagePoint.x, pagePoint.y, mAnnotPageNum);
                        x = (float) pts[0] + sx;
                        y = (float) pts[1] + sy;
                    } else {
                        pts = mPdfViewCtrl.convPagePtToHorizontalScrollingPt(pagePoint.x, pagePoint.y, mAnnotPageNum);
                        x = (float) pts[0];
                        y = (float) pts[1];
                    }
                    double[] pt = mPdfViewCtrl.convScreenPtToPagePt(
                        x + deltaX - sx,
                        y + deltaY - sy, mAnnotPageNum);
                    mPoly.setVertex(i, new Point(pt[0], pt[1]));
                }
            } else if (mCallout != null) {
                super.updateAnnot();
            }
        } else {
            double[] pt = mPdfViewCtrl.convScreenPtToPagePt(
                mCtrlPts[mEffCtrlPtId].x - sx,
                mCtrlPts[mEffCtrlPtId].y - sy, mAnnotPageNum);
            if (mPoly != null) {
                mPoly.setVertex(mEffCtrlPtId, new Point(pt[0], pt[1]));
            } else if (mCallout != null) {
                if (mEffCtrlPtId < RECTANGULAR_CTRL_PTS_CNT) {
                    super.updateAnnot();
                } else {
                    // save content rect
                    Rect contentRect = mCallout.getContentRect();
                    if (mEffCtrlPtId == CALLOUT_START_POINT_INDEX) {
                        mCallout.setCalloutLinePoints(new Point(pt[0], pt[1]),
                            mCallout.getCalloutLinePoint2(), mCallout.getCalloutLinePoint3());
                    } else if (mEffCtrlPtId == CALLOUT_KNEE_POINT_INDEX) {
                        double[] pt2 = mPdfViewCtrl.convScreenPtToPagePt(
                            mCtrlPts[CALLOUT_END_POINT_INDEX].x - sx,
                            mCtrlPts[CALLOUT_END_POINT_INDEX].y - sy, mAnnotPageNum);

                        mCallout.setCalloutLinePoints(mCallout.getCalloutLinePoint1(),
                            new Point(pt[0], pt[1]), new Point(pt2[0], pt2[1]));
                    }
                    mCallout.setRect(contentRect);
                    mCallout.setContentRect(contentRect);
                    mCallout.refreshAppearance();
                    setCtrlPts();
                }
            }
        }

        boolean shouldUpdateBBox = !mBBox.equals(mBBoxOnDown);
        if (shouldUpdateBBox) {
            Rect newAnnotRect = getNewAnnotPagePosition();
            if (newAnnotRect == null || oldUpdateRect == null) {
                return;
            }
            if (mPoly != null) {
                mPoly.setRect(newAnnotRect);
            }
        }
        mAnnot.refreshAppearance();
        if (shouldUpdateBBox) {
            buildAnnotBBox();
            mPdfViewCtrl.update(oldUpdateRect);
        }
        mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
    }

    @Override
    protected void adjustExtraFreeTextProps(Rect oldContentRect, Rect newContentRect) {
        super.adjustExtraFreeTextProps(oldContentRect, newContentRect);
        if (mCallout == null || oldContentRect == null || newContentRect == null) {
            return;
        }

        try {
            Point pt2 = mCallout.getCalloutLinePoint2();
            Point pt3 = mCallout.getCalloutLinePoint3();
            // find which side the callout point is on

            Point p1 = Utils.calcIntersection(pt2.x, pt2.y, pt3.x, pt3.y,
                oldContentRect.getX1(), oldContentRect.getY1(), oldContentRect.getX1(), oldContentRect.getY2());
            Point p2 = Utils.calcIntersection(pt2.x, pt2.y, pt3.x, pt3.y,
                oldContentRect.getX1(), oldContentRect.getY1(), oldContentRect.getX2(), oldContentRect.getY1());
            Point p3 = Utils.calcIntersection(pt2.x, pt2.y, pt3.x, pt3.y,
                oldContentRect.getX1(), oldContentRect.getY2(), oldContentRect.getX2(), oldContentRect.getY2());
            Point p4 = Utils.calcIntersection(pt2.x, pt2.y, pt3.x, pt3.y,
                oldContentRect.getX2(), oldContentRect.getY1(), oldContentRect.getX2(), oldContentRect.getY2());

            double x, y;
            int which = 0;
            boolean found = false;
            if (p1 != null) {
                x = p1.x;
                y = p1.y;
                found = Utils.isSamePoint(pt3.x, pt3.y, x, y);
                if (found) {
                    which = SIDE_X1Y1_X1Y2;
                }
            }
            if (!found && p2 != null) {
                x = p2.x;
                y = p2.y;
                found = Utils.isSamePoint(pt3.x, pt3.y, x, y);
                if (found) {
                    which = SIDE_X1Y1_X2Y1;
                }
            }
            if (!found && p3 != null) {
                x = p3.x;
                y = p3.y;
                found = Utils.isSamePoint(pt3.x, pt3.y, x, y);
                if (found) {
                    which = SIDE_X1Y2_X2Y2;
                }
            }
            if (!found && p4 != null) {
                x = p4.x;
                y = p4.y;
                found = Utils.isSamePoint(pt3.x, pt3.y, x, y);
                if (found) {
                    which = SIDE_X2Y1_X2Y2;
                }
            }
            if (found) {
                double newX = -1;
                double newY = -1;
                switch (which) {
                    case SIDE_X1Y1_X1Y2:
                        newX = newContentRect.getX1();
                        newY = (newContentRect.getY1() + newContentRect.getY2()) / 2;
                        break;
                    case SIDE_X1Y1_X2Y1:
                        newX = (newContentRect.getX1() + newContentRect.getX2()) / 2;
                        newY = newContentRect.getY1();
                        break;
                    case SIDE_X1Y2_X2Y2:
                        newX = (newContentRect.getX1() + newContentRect.getX2()) / 2;
                        newY = newContentRect.getY2();
                        break;
                    case SIDE_X2Y1_X2Y2:
                        newX = newContentRect.getX2();
                        newY = (newContentRect.getY1() + newContentRect.getY2()) / 2;
                        break;
                }
                if (newX >= 0 && newY >= 0) {
                    mCallout.setCalloutLinePoints(
                        mCallout.getCalloutLinePoint1(),
                        mCallout.getCalloutLinePoint2(),
                        new Point(newX, newY)
                    );
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected boolean canAddAnnotView(Annot annot, AnnotStyle annotStyle) {
        if (!((ToolManager) mPdfViewCtrl.getToolManager()).isRealTimeAnnotEdit()) {
            return false;
        }
        //noinspection SimplifiableIfStatement
        if (annotStyle.hasAppearance()) {
            return false;
        }
        return annotStyle.getAnnotType() == Annot.e_Polyline ||
            annotStyle.getAnnotType() == Annot.e_Polygon ||
            annotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD;
    }

    protected void updateAnnotView() {
        if (mAnnotView != null && mAnnotView.getDrawingView() != null) {
            int xOffset = mPdfViewCtrl.getScrollX();
            int yOffset = mPdfViewCtrl.getScrollY();
            mAnnotView.layout(xOffset,
                yOffset,
                xOffset + mPdfViewCtrl.getWidth(),
                yOffset + mPdfViewCtrl.getHeight());
            mAnnotView.getDrawingView().invalidate();
        }
    }
}
