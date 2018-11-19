//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import com.pdftron.pdf.Action;
import com.pdftron.pdf.ActionParameter;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.QuadPoint;
import com.pdftron.pdf.annots.Link;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;

/**
 * A tool for handling single tap on {@link Link} annotation
 */
@Keep
public class LinkAction extends Tool {

    private Link mLink;
    private Paint mPaint;

    /**
     * Class constructor
     */
    public LinkAction(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(ctrl.getResources().getColor(R.color.tools_link));
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.LINK_ACTION;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Unknown;
    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (mAnnot != null) {
            mNextToolMode = ToolMode.LINK_ACTION;
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                mLink = new Link(mAnnot);
                mPdfViewCtrl.invalidate();
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
        } else {
            mNextToolMode = ToolMode.PAN;
        }
        return false;
    }

    private boolean handleLink() {
        if (onInterceptAnnotationHandling(mAnnot)) {
            return true;
        }
        if (mLink == null) {
            return false;
        }

        boolean shouldUnlockRead = false;
        boolean shouldUnlock = false;
        mNextToolMode = mCurrentDefaultToolMode;
        Action a;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            a = mLink.getAction();
            if (a != null) {
                if (a.needsWriteLock()) {
                    mPdfViewCtrl.docUnlockRead();
                    shouldUnlockRead = false;
                    mPdfViewCtrl.docLock(true);
                    shouldUnlock = true;
                }
                ActionParameter action_param = new ActionParameter(a);
                executeAction(action_param);
                mPdfViewCtrl.invalidate();  // Draw away the highlight.
                if (shouldUnlock) {
                    raiseAnnotationActionEvent();
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
        return true;
    }

    /**
     * The overload implementation of {@link Tool#onPostSingleTapConfirmed()}.
     */
    @Override
    public void onPostSingleTapConfirmed() {
        handleLink();
    }

    /**
     * The overload implementation of {@link Tool#onLongPress(MotionEvent)}.
     */
    @Override
    public boolean onLongPress(MotionEvent e) {
        if (mAnnot != null) {
            mNextToolMode = ToolMode.LINK_ACTION;
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                mLink = new Link(mAnnot);
                mPdfViewCtrl.invalidate();
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
        } else {
            mNextToolMode = ToolMode.PAN;
        }

        mAvoidLongPressAttempt = true;

        handleLink();

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        try {
            int qn = mLink.getQuadPointCount();
            float sx = mPdfViewCtrl.getScrollX();
            float sy = mPdfViewCtrl.getScrollY();
            for (int i = 0; i < qn; ++i) {
                QuadPoint qp = mLink.getQuadPoint(i);
                float x1 = (float) Math.min(Math.min(Math.min(qp.p1.x, qp.p2.x), qp.p3.x), qp.p4.x);
                float y2 = (float) Math.min(Math.min(Math.min(qp.p1.y, qp.p2.y), qp.p3.y), qp.p4.y);
                float x2 = (float) Math.max(Math.max(Math.max(qp.p1.x, qp.p2.x), qp.p3.x), qp.p4.x);
                float y1 = (float) Math.max(Math.max(Math.max(qp.p1.y, qp.p2.y), qp.p3.y), qp.p4.y);
                double[] pts = mPdfViewCtrl.convPagePtToScreenPt(x1, y1, mAnnotPageNum);
                float left = (float) pts[0] + sx;
                float top = (float) pts[1] + sy;
                pts = mPdfViewCtrl.convPagePtToScreenPt(x2, y2, mAnnotPageNum);
                float right = (float) pts[0] + sx;
                float bottom = (float) pts[1] + sy;

                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setAlpha(128);
                canvas.drawRect(left, top, right, bottom, mPaint);

                float len = Math.min(right - left, bottom - top);
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setStrokeWidth(Math.max(len / 15, 2));
                mPaint.setAlpha(255);
                canvas.drawRect(left, top, right, bottom, mPaint);
            }
        } catch (Exception ignored) {

        }
    }
}
