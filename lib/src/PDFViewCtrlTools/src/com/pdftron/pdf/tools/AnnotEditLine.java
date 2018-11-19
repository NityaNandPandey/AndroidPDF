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
import com.pdftron.pdf.annots.Line;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.Utils;

/**
 * This class is responsible for editing a selected line or arrow, e.g., moving and resizing.
 */
@SuppressWarnings("WeakerAccess")
@Keep
public class AnnotEditLine extends AnnotEdit {

    private Line mLine = null;
    private RectF mTempRect;
    private Path mPath;

    private final int e_start_point = 0;
    private final int e_end_point = 1;

    /**
     * Class constructor
     */
    public AnnotEditLine(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        CTRL_PTS_CNT = 2;
        mCtrlPts = new PointF[CTRL_PTS_CNT];
        mCtrlPtsOnDown = new PointF[CTRL_PTS_CNT];
        for (int i = 0; i < 2; ++i) {
            mCtrlPts[i] = new PointF();
            mCtrlPtsOnDown[i] = new PointF();
        }

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mBBox = new RectF();
        mTempRect = new RectF();
        mPath = new Path();
        mModifiedAnnot = false;
        mCtrlPtsSet = false;
        mScaled = false;
    }

    /**
     * The overload implementation of {@link Tool#onCreate()}.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (mAnnot != null) {
            mHasSelectionPermission = hasPermission(mAnnot, ANNOT_PERMISSION_SELECTION);
            mHasMenuPermission = hasPermission(mAnnot, ANNOT_PERMISSION_MENU);

            try {
                mLine = new Line(mAnnot);
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }


            // Remember the page bounding box in client space; this is used to ensure while
            // moving/resizing, the widget doesn't go beyond the page boundary.
            mPageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mAnnotPageNum);

        }
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.ANNOT_EDIT_LINE;
    }

    @Override
    protected void setCtrlPts() {
        if (onInterceptAnnotationHandling(mAnnot)) {
            return;
        }

        mCtrlPtsSet = true;

        try {
            float x1 = (float) mLine.getStartPoint().x;
            float y1 = (float) mLine.getStartPoint().y;
            float x2 = (float) mLine.getEndPoint().x;
            float y2 = (float) mLine.getEndPoint().y;

            float sx = mPdfViewCtrl.getScrollX();
            float sy = mPdfViewCtrl.getScrollY();

            // Start point
            double[] pts = mPdfViewCtrl.convPagePtToScreenPt(x1, y1, mAnnotPageNum);
            mCtrlPts[e_start_point].x = (float) pts[0] + sx;
            mCtrlPts[e_start_point].y = (float) pts[1] + sy;

            // End point
            pts = mPdfViewCtrl.convPagePtToScreenPt(x2, y2, mAnnotPageNum);
            mCtrlPts[e_end_point].x = (float) pts[0] + sx;
            mCtrlPts[e_end_point].y = (float) pts[1] + sy;

            // Compute the bounding box
            mBBox.left = Math.min(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x) - mCtrlRadius;
            mBBox.top = Math.min(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y) - mCtrlRadius;
            mBBox.right = Math.max(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x) + mCtrlRadius;
            mBBox.bottom = Math.max(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y) + mCtrlRadius;

            addAnnotView();
            updateAnnotView();

            for (int i = 0; i < 2; ++i) {
                mCtrlPtsOnDown[i].set(mCtrlPts[i]);
            }

        } catch (PDFNetException e) {
            mCtrlPtsSet = false;
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    @Override
    protected void updateCtrlPts(boolean translate, float left, float right, float top, float bottom, RectF which) {
        setCtrlPts();
    }

    @Override
    protected boolean canAddAnnotView(Annot annot, AnnotStyle annotStyle) {
        if (!((ToolManager) mPdfViewCtrl.getToolManager()).isRealTimeAnnotEdit()) {
            return false;
        }
        if (annotStyle.hasAppearance()) {
            return false;
        }
        if (annotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW ||
            annotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_RULER) {
            return true;
        }
        if (annotStyle.getAnnotType() == Annot.e_Line) {
            try {
                return AnnotUtils.isSimpleLine(annot);
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    protected void updateAnnotView() {
        if (mAnnotView != null && mAnnotView.getDrawingView() != null) {
            int xOffset = mPdfViewCtrl.getScrollX();
            int yOffset = mPdfViewCtrl.getScrollY();
            PointF start = new PointF(mCtrlPts[e_start_point].x - xOffset,
                mCtrlPts[e_start_point].y - yOffset);
            PointF end = new PointF(mCtrlPts[e_end_point].x - xOffset,
                mCtrlPts[e_end_point].y - yOffset);
            if (mAnnotView.getDrawingView().getVertices().size() == 2) {
                mAnnotView.getDrawingView().getVertices().set(0, start);
                mAnnotView.getDrawingView().getVertices().set(1, end);
            } else {
                mAnnotView.getDrawingView().setVertices(start, end);
            }
            mAnnotView.layout(xOffset,
                yOffset,
                xOffset + mPdfViewCtrl.getWidth(),
                yOffset + mPdfViewCtrl.getHeight());
            mAnnotView.getDrawingView().invalidate();
        }
    }

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        super.onDraw(canvas, tfm);

        float left = mCtrlPts[e_start_point].x;
        float top = mCtrlPts[e_end_point].y;
        float right = mCtrlPts[e_end_point].x;
        float bottom = mCtrlPts[e_start_point].y;

        if (mAnnot != null) {
            PathEffect pathEffect = mPaint.getPathEffect();
            if (mModifiedAnnot) {
                mPaint.setColor(mPdfViewCtrl.getResources().getColor(R.color.tools_annot_edit_line_shadow));
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setPathEffect(mDashPathEffect);
                // Bug in drawLine: https://code.google.com/p/android/issues/detail?id=29944
                // Need to use drawPath instead.
                // canvas.drawLine(right, top, left, bottom, mPaint);
                mPath.reset();
                mPath.moveTo(right, top);
                mPath.lineTo(left, bottom);
                canvas.drawPath(mPath, mPaint);
            }

            if (!mHasSelectionPermission) {
                mPaint.setColor(mPdfViewCtrl.getResources().getColor(R.color.tools_annot_edit_line_shadow_no_permission));
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(2);
//                mPaint.setPathEffect(mDashPathEffect);
                canvas.drawRect(right, top, left, bottom, mPaint);
            }

            // reset path effect
            mPaint.setPathEffect(pathEffect);

            DrawingUtils.drawCtrlPtsLine(mPdfViewCtrl.getResources(), canvas, mPaint,
                mCtrlPts[e_start_point], mCtrlPts[e_end_point], mCtrlRadius, mHasMenuPermission);
        }
    }

    /**
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        super.onUp(e, priorEventMode);

        mNextToolMode = ToolMode.ANNOT_EDIT_LINE;
        mScaled = false;

        if (!mHasMenuPermission) {
            if (mAnnot != null) {
                showMenu(getAnnotRect());
            }
        }

        if (mAnnot != null
            && (mModifiedAnnot
            || !mCtrlPtsSet
            || priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING
            || priorEventMode == PDFViewCtrl.PriorEventMode.PINCH
            || priorEventMode == PDFViewCtrl.PriorEventMode.DOUBLE_TAP)) {

            if (!mCtrlPtsSet) {
                setCtrlPts();
            }

            resizeLine(priorEventMode);

            showMenu(getAnnotRect());

            // Don't let the main view scroll
            return priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING || priorEventMode == PDFViewCtrl.PriorEventMode.FLING;

        } else {
            return false;
        }
    }

    private void resizeLine(PDFViewCtrl.PriorEventMode priorEventMode) {
        if (mAnnot == null || onInterceptAnnotationHandling(mAnnot)) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            if (mModifiedAnnot) {
                mModifiedAnnot = false;
                raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

                // Compute the new annotation position
                float x1 = mCtrlPts[e_start_point].x - mPdfViewCtrl.getScrollX();
                float y1 = mCtrlPts[e_start_point].y - mPdfViewCtrl.getScrollY();
                float x2 = mCtrlPts[e_end_point].x - mPdfViewCtrl.getScrollX();
                float y2 = mCtrlPts[e_end_point].y - mPdfViewCtrl.getScrollY();
                double[] pts1, pts2;
                pts1 = mPdfViewCtrl.convScreenPtToPagePt(x1, y1, mAnnotPageNum);
                pts2 = mPdfViewCtrl.convScreenPtToPagePt(x2, y2, mAnnotPageNum);
                com.pdftron.pdf.Rect new_annot_rect = new com.pdftron.pdf.Rect(pts1[0], pts1[1], pts2[0], pts2[1]);
                new_annot_rect.normalize();

                // Compute the old annotation position in screen space for update
                double[] pts1_old, pts2_old;
                com.pdftron.pdf.Rect r = mAnnot.getRect();
                pts1_old = mPdfViewCtrl.convPagePtToScreenPt(r.getX1(), r.getY1(), mAnnotPageNum);
                pts2_old = mPdfViewCtrl.convPagePtToScreenPt(r.getX2(), r.getY2(), mAnnotPageNum);
                com.pdftron.pdf.Rect old_update_rect = new com.pdftron.pdf.Rect(pts1_old[0], pts1_old[1], pts2_old[0], pts2_old[1]);
                old_update_rect.normalize();

                mAnnot.resize(new_annot_rect);

                Line line = new Line(mAnnot);

                RulerItem rulerItem = RulerItem.getRulerItem(mAnnot);
                if (null != rulerItem) {
                    String label = RulerCreate.getRulerLabel(rulerItem, pts1[0], pts1[1], pts2[0], pts2[1]);
                    line.setContents(label);
                }

                line.setStartPoint(new Point(pts1[0], pts1[1]));
                line.setEndPoint(new Point(pts2[0], pts2[1]));

                mAnnot.refreshAppearance();
                buildAnnotBBox();
                mPdfViewCtrl.update(old_update_rect);   // Update the old position
                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);
            } else if (priorEventMode == PDFViewCtrl.PriorEventMode.PINCH || priorEventMode == PDFViewCtrl.PriorEventMode.DOUBLE_TAP) {
                setCtrlPts();
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);

        float x = e.getX() + mPdfViewCtrl.getScrollX();
        float y = e.getY() + mPdfViewCtrl.getScrollY();

        // Check if any control point is hit
        mEffCtrlPtId = e_unknown;
        float thresh = mCtrlRadius * 2.25f;
        float shortest_dist = -1;
        for (int i = 0; i < 2; ++i) {
            float s = mCtrlPts[i].x;
            float t = mCtrlPts[i].y;

            float dist = (x - s) * (x - s) + (y - t) * (y - t);
            dist = (float) Math.sqrt(dist);
            if (dist <= thresh && (dist < shortest_dist || shortest_dist < 0)) {
                mEffCtrlPtId = i;
                shortest_dist = dist;
            }

            mCtrlPtsOnDown[i].set(mCtrlPts[i]);
        }

        // Check if hit within the line without hitting any control point.
        if (mEffCtrlPtId < 0) {
            if (pointToLineDistance(x, y)) {
                mEffCtrlPtId = e_moving; // Indicating moving mode;
            }
        }

        // Re-compute the page bounding box on screen, since the zoom
        // factor may have been changed.
        if (mAnnot != null) {
            mPageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mAnnotPageNum);
        }

        if (mAnnot != null) {
            if (!isInsideAnnot(e) && mEffCtrlPtId < 0) {
                unsetAnnot();
                mNextToolMode = mCurrentDefaultToolMode;
                setCtrlPts();
                // Draw away the edit widget
                mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));
            }
        }

        return false;
    }

    @Override
    protected boolean isInsideAnnot(MotionEvent e) {
        int x = (int) (e.getX() + 0.5);
        int y = (int) (e.getY() + 0.5);
        // line is a special case where you can have a large area and not touching the line at all
        // so we do a more precise hit test here
        Annot tempAnnot = mPdfViewCtrl.getAnnotationAt(x, y);
        return (mAnnot != null && mAnnot.equals(tempAnnot));
    }

    private boolean pointToLineDistance(double x, double y) {
        double lineXDist = mCtrlPts[e_end_point].x - mCtrlPts[e_start_point].x;
        double lineYDist = mCtrlPts[e_end_point].y - mCtrlPts[e_start_point].y;

        double squaredDist = (lineXDist * lineXDist) + (lineYDist * lineYDist);

        double distRatio = ((x - mCtrlPts[e_start_point].x) * lineXDist + (y - mCtrlPts[e_start_point].y) * lineYDist) / squaredDist;

        if (distRatio < 0) {
            distRatio = 0;  // This way, we will compare against e_start_point
        }
        if (distRatio > 1) {
            distRatio = 0;  // This way, we will compare against e_end_point
        }

        double dx = mCtrlPts[e_start_point].x - x + distRatio * lineXDist;
        double dy = mCtrlPts[e_start_point].y - y + distRatio * lineYDist;

        double dist = (dx * dx) + (dy * dy);

        return dist < (mCtrlRadius * mCtrlRadius * 4f);
    }

    private void boundCornerCtrlPts(float ox, float oy, boolean translate) {
        if (mPageCropOnClientF != null) {

            if (translate) {
                float max_x = Math.max(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x);
                float min_x = Math.min(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x);
                float max_y = Math.max(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y);
                float min_y = Math.min(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y);

                float shift_x = 0, shift_y = 0;
                if (min_x < mPageCropOnClientF.left) {
                    shift_x = mPageCropOnClientF.left - min_x;
                }
                if (min_y < mPageCropOnClientF.top) {
                    shift_y = mPageCropOnClientF.top - min_y;
                }
                if (max_x > mPageCropOnClientF.right) {
                    shift_x = mPageCropOnClientF.right - max_x;
                }
                if (max_y > mPageCropOnClientF.bottom) {
                    shift_y = mPageCropOnClientF.bottom - max_y;
                }

                mCtrlPts[e_start_point].x += shift_x;
                mCtrlPts[e_start_point].y += shift_y;
                mCtrlPts[e_end_point].x += shift_x;
                mCtrlPts[e_end_point].y += shift_y;
            } else {

                // Bounding along x-axis
                if (mCtrlPts[e_start_point].x > mPageCropOnClientF.right && ox > 0) {
                    mCtrlPts[e_start_point].x = mPageCropOnClientF.right;
                } else if (mCtrlPts[e_start_point].x < mPageCropOnClientF.left && ox < 0) {
                    mCtrlPts[e_start_point].x = mPageCropOnClientF.left;
                } else if (mCtrlPts[e_end_point].x > mPageCropOnClientF.right && ox > 0) {
                    mCtrlPts[e_end_point].x = mPageCropOnClientF.right;
                } else if (mCtrlPts[e_end_point].x < mPageCropOnClientF.left && ox < 0) {
                    mCtrlPts[e_end_point].x = mPageCropOnClientF.left;
                }

                // Bounding along y-axis
                if (mCtrlPts[e_start_point].y < mPageCropOnClientF.top && oy < 0) {
                    mCtrlPts[e_start_point].y = mPageCropOnClientF.top;
                } else if (mCtrlPts[e_start_point].y > mPageCropOnClientF.bottom && oy > 0) {
                    mCtrlPts[e_start_point].y = mPageCropOnClientF.bottom;
                } else if (mCtrlPts[e_end_point].y < mPageCropOnClientF.top && oy < 0) {
                    mCtrlPts[e_end_point].y = mPageCropOnClientF.top;
                } else if (mCtrlPts[e_end_point].y > mPageCropOnClientF.bottom && oy > 0) {
                    mCtrlPts[e_end_point].y = mPageCropOnClientF.bottom;
                }
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        if (mScaled) {
            // Scaled and if while moving, disable moving to avoid complications.
            return false;
        }
        if (!mHasSelectionPermission) {
            // does not have permission to modify annotation
            return false;
        }

        if (mEffCtrlPtId != e_unknown) {
            float totalMoveX = e2.getX() - e1.getX();
            float totalMoveY = e2.getY() - e1.getY();

            mTempRect.set(mBBox);

            if (mEffCtrlPtId == e_moving) {
                for (int i = 0; i < 2; ++i) {
                    mCtrlPts[i].x = mCtrlPtsOnDown[i].x + totalMoveX;
                    mCtrlPts[i].y = mCtrlPtsOnDown[i].y + totalMoveY;
                }
                boundCornerCtrlPts(totalMoveX, totalMoveY, true);

                // Compute the bounding box
                mBBox.left = Math.min(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x) - mCtrlRadius;
                mBBox.top = Math.min(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y) - mCtrlRadius;
                mBBox.right = Math.max(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x) + mCtrlRadius;
                mBBox.bottom = Math.max(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y) + mCtrlRadius;

                mModifiedAnnot = true;

            } else {
                boolean valid = false;
                switch (mEffCtrlPtId) {
                    case e_start_point:
                        mCtrlPts[e_start_point].x = mCtrlPtsOnDown[e_start_point].x + totalMoveX;
                        mCtrlPts[e_start_point].y = mCtrlPtsOnDown[e_start_point].y + totalMoveY;
                        valid = true;
                        break;
                    case e_end_point:
                        mCtrlPts[e_end_point].x = mCtrlPtsOnDown[e_end_point].x + totalMoveX;
                        mCtrlPts[e_end_point].y = mCtrlPtsOnDown[e_end_point].y + totalMoveY;
                        valid = true;
                        break;
                }
                mModifiedAnnot = true;

                if (valid) {
                    boundCornerCtrlPts(totalMoveX, totalMoveY, false);

                    // Compute the bounding box
                    mBBox.left = Math.min(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x) - mCtrlRadius;
                    mBBox.top = Math.min(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y) - mCtrlRadius;
                    mBBox.right = Math.max(mCtrlPts[e_start_point].x, mCtrlPts[e_end_point].x) + mCtrlRadius;
                    mBBox.bottom = Math.max(mCtrlPts[e_start_point].y, mCtrlPts[e_end_point].y) + mCtrlRadius;

                    mModifiedAnnot = true;
                }
            }

            float min_x = Math.min(mTempRect.left, mBBox.left);
            float max_x = Math.max(mTempRect.right, mBBox.right);
            float min_y = Math.min(mTempRect.top, mBBox.top);
            float max_y = Math.max(mTempRect.bottom, mBBox.bottom);
            mPdfViewCtrl.invalidate((int) min_x - 1, (int) min_y - 1, (int) Math.ceil(max_x) + 1, (int) Math.ceil(max_y) + 1);

            updateAnnotView();

            return true;
        }
        return false;
    }


    /**
     * Returns which effective control point is closest to the specified coordinate.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The effective control point which can be one of this
     * -1 : unspecified
     * 0 : the first point,
     * 1 : the second point,
     * 2 : moving mode
     */
    public int getEffectCtrlPointId(float x, float y) {
        int effCtrlPtId = -1;
        float thresh = mCtrlRadius * 2.25f;
        float shortest_dist = -1;
        for (int i = 0; i < 2; ++i) {
            float s = mCtrlPts[i].x;
            float t = mCtrlPts[i].y;

            float dist = (x - s) * (x - s) + (y - t) * (y - t);
            dist = (float) Math.sqrt(dist);
            if (dist <= thresh && (dist < shortest_dist || shortest_dist < 0)) {
                effCtrlPtId = i;
                shortest_dist = dist;
            }
        }

        // Check if hit within the line without hitting any control point.
        if (effCtrlPtId < 0 && pointToLineDistance(x, y)) {
            effCtrlPtId = 2; // Indicating moving mode;
        }
        return effCtrlPtId;
    }
}
