package com.pdftron.pdf.tools;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.annots.Line;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

/**
 * A tool for creating callout annotation
 */
@Keep
public class CalloutCreate extends FreeTextCreate {

    private static int THRESHOLD = 40; // page space

    private Point mStart;
    private Point mKnee;
    private Point mEnd;
    private Rect mContentRect;

    /**
     * Class constructor
     */
    public CalloutCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.CALLOUT_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT;
    }

    @Override
    protected void createFreeText() {
        try {
            preCreateCallout();

            // creates empty callout
            boolean shouldUnlock = false;
            try {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                createAnnot("");
                raiseAnnotationAddedEvent(mAnnot, mAnnotPageNum);
            } catch (Exception e) {
                ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(e.getMessage());
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }

            // enter edit mode
            mNextToolMode = ToolManager.ToolMode.ANNOT_EDIT_ADVANCED_SHAPE;
            setCurrentDefaultToolModeHelper(getToolMode());
            mUpFromCalloutCreate = mOnUpOccured;
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex, "CalloutCreate::createFreeText");
        }
    }

    private void preCreateCallout() throws PDFNetException {
        if (mTargetPointPageSpace == null) {
            return;
        }
        Rect pageRect = Utils.getPageRect(mPdfViewCtrl, mPageNum);
        if (pageRect != null) {
            pageRect.normalize();
            // start point with arrow is target point
            mStart = new Point(mTargetPointPageSpace.x, mTargetPointPageSpace.y);
            mKnee = calcCalloutKneePt(pageRect);
            mEnd = calcCalloutEndPt(pageRect);
            mContentRect = calcCalloutContentRect(pageRect);
        }
    }

    @Override
    protected Rect getFreeTextBBox(FreeText freeText, boolean isRightToLeft) throws PDFNetException {
        return mContentRect;
    }

    @Override
    protected void setExtraFreeTextProps(FreeText freetext, Rect bbox) throws PDFNetException {
        super.setExtraFreeTextProps(freetext, bbox);

        if (mStart == null || mKnee == null || mEnd == null) {
            return;
        }

        freetext.setCalloutLinePoints(mStart, mKnee, mEnd);

        freetext.setEndingStyle(Line.e_OpenArrow);

        freetext.setContentRect(bbox);
        freetext.setIntentName(FreeText.e_FreeTextCallout);
    }

    private Point calcCalloutKneePt(Rect pageRect) throws PDFNetException {
        double x, y;
        double pageX1 = pageRect.getX1();
        double pageX2 = pageRect.getX2();
        double pageMidX = (pageX1 + pageX2) / 2;
        double pageY1 = pageRect.getY1();
        double pageY2 = pageRect.getY2();
        double pageMidY = (pageY1 + pageY2) / 2;

        if (mStart.x > pageMidX) {
            if (mStart.y > pageMidY) {
                x = mStart.x - THRESHOLD;
                y = mStart.y - THRESHOLD;
            } else {
                x = mStart.x - THRESHOLD;
                y = mStart.y + THRESHOLD;
            }
        } else {
            if (mStart.y > pageMidY) {
                x = mStart.x + THRESHOLD;
                y = mStart.y - THRESHOLD;
            } else {
                x = mStart.x + THRESHOLD;
                y = mStart.y + THRESHOLD;
            }
        }
        return new Point(x, y);
    }

    private Point calcCalloutEndPt(Rect pageRect) throws PDFNetException {
        double x = mStart.x > mKnee.x ? Math.min(mKnee.x - THRESHOLD, pageRect.getX2()) : Math.max(mKnee.x + THRESHOLD, pageRect.getX1());
        double y = mKnee.y;

        return new Point(x, y);
    }

    private Rect calcCalloutContentRect(Rect pageRect) throws PDFNetException {
        double x1 = mEnd.x > mKnee.x ? mEnd.x : Math.max(mEnd.x - THRESHOLD * 2, pageRect.getX1());
        double y1 = Math.max(mEnd.y - THRESHOLD / 2, pageRect.getY1());
        double x2 = mEnd.x > mKnee.x ? Math.min(x1 + THRESHOLD * 2, pageRect.getX2()) : mEnd.x;
        double y2 = Math.min(y1 + THRESHOLD, pageRect.getY2());
        return new Rect(x1, y1, x2, y2);
    }
}
