package com.pdftron.pdf.tools;

import android.graphics.PointF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;

import java.util.ArrayList;

@Keep
public abstract class SimpleTapShapeCreate extends SimpleShapeCreate {

    private static final int ICON_SIZE = 20;

    protected int mIconSize = ICON_SIZE;

    /**
     * Class constructor
     */
    public SimpleTapShapeCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mAnnotPushedBack = false;
        return super.onDown(e);
    }

    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {

        // With a fling motion, onUp is called twice. We want
        // to ignore the second call
        if (mAnnotPushedBack) {
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

        boolean shouldCreate = true;
        int x = (int) e.getX();
        int y = (int) e.getY();
        ArrayList<Annot> annots = mPdfViewCtrl.getAnnotationListAt(x, y, x, y);
        int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
        try {
            for (Annot annot : annots) {
                if (annot.isValid() && annot.getType() == getCreateAnnotType()) {
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
            mPt2 = new PointF(e.getX(), e.getY());
            addAnnotation();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the target point.
     *
     * @param point The target point
     * @param createAnnot True if should create annot
     */
    public void setTargetPoint(PointF point, boolean createAnnot) {
        mPt2.x = point.x;
        mPt2.y = point.y;
        mDownPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(point.x, point.y);

        if (createAnnot) {
            addAnnotation();
        }
    }

    /**
     * The implementation will provide UI for annotation creation
     */
    abstract public void addAnnotation();

    protected boolean createAnnotation(PointF targetPoint, int pageNum) {
        boolean success = false;
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            com.pdftron.pdf.Rect rect = getBBox(targetPoint, pageNum);
            if (rect != null) {
                Annot annot = createMarkup(mPdfViewCtrl.getDoc(), rect);
                Page page = mPdfViewCtrl.getDoc().getPage(pageNum);
                if (annot != null && page != null) {
                    annot.refreshAppearance();
                    page.annotPushBack(annot);
                    mAnnotPushedBack = true;
                    setAnnot(annot, pageNum);
                    buildAnnotBBox();
                    mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                    raiseAnnotationAddedEvent(mAnnot, mAnnotPageNum);
                    success = true;
                }
            }
        } catch (Exception ex) {
            mNextToolMode = ToolManager.ToolMode.PAN;
            ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(ex.getMessage());
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
            onCreateMarkupFailed(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
            clearTargetPoint();
            setNextToolModeHelper();
        }
        return success;
    }

    protected Rect getBBox(PointF targetPoint, int pageNum) throws PDFNetException {
        if (targetPoint == null) {
            return null;
        }
        double[] pts = mPdfViewCtrl.convScreenPtToPagePt(targetPoint.x, targetPoint.y, pageNum);
        return new com.pdftron.pdf.Rect(pts[0], pts[1], pts[0] + mIconSize, pts[1] + mIconSize);
    }

    protected void setNextToolModeHelper() {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager.isAutoSelectAnnotation() || !mForceSameNextToolMode) {
            mNextToolMode = getDefaultNextTool();
        } else {
            mNextToolMode = getToolMode();
        }
    }
}
