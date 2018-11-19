//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.sdf.Obj;

import java.util.HashMap;

/**
 * Used for creating Rectangle link
 */
@Keep
public class RectLinkCreate extends RectCreate {

    /**
     * Class constructor
     */
    public RectLinkCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = ToolMode.RECT_LINK;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.RECT_LINK;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Link;
    }

    /**
     * The overload implementation of {@link RectCreate#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
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
            mPt1.set(0, 0);
            mPt2.set(0, 0);
            return true;
        }

        // If annotation was already pushed back, avoid re-entry due to fling motion
        // but allow when creating multiple strokes.
        if (mAnnotPushedBack && mForceSameNextToolMode) {
            return true;
        }

        // If all points are outside of the page, we don't push back the annotation
        if (mIsAllPointsOutsidePage) {
            return true;
        }
        try {
            if (!mForceSameNextToolMode) {
                mNextToolMode = ToolMode.PAN;
            } else {
                mNextToolMode = getToolMode();
            }
            setCurrentDefaultToolModeHelper(getToolMode());
            // add UI to drawing list
//            addOldTools();

            com.pdftron.pdf.Rect rect = getShapeBBox();
            if (rect != null) {
                HashMap<Rect, Integer> annotInfo = new HashMap<>();
                annotInfo.put(rect, mDownPageNum);
                DialogLinkEditor linkEditorDialog = new DialogLinkEditor(mPdfViewCtrl, this, annotInfo);
                linkEditorDialog.setColor(mStrokeColor);
                linkEditorDialog.setThickness(mThickness);
                linkEditorDialog.show();
                mPt1.set(0, 0);
                mPt2.set(0, 0);
                mAnnotPushedBack = true;
            }

        } catch (Exception ex) {
            mNextToolMode = ToolMode.PAN;
            ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(ex.getMessage());
            AnalyticsHandlerAdapter.getInstance().sendException(ex);

        }

        return skipOnUpPriorEvent(priorEventMode);
    }

    /**
     * The overload implementation of {@link RectCreate#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        unsetAnnot();
        return super.onSingleTapConfirmed(e);
    }

    @Override
    public boolean onLongPress(MotionEvent e) {
        unsetAnnot();
        if (mForceSameNextToolMode) {

            int x = (int) (e.getX() + 0.5);
            int y = (int) (e.getY() + 0.5);
            selectAnnot(x, y);

            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;

                // If hit a link, do the link action
                if (mAnnot != null && mAnnot.getType() == Annot.e_Link) {
                    if (isMadeByPDFTron(mAnnot)) {
                        mNextToolMode = ToolMode.ANNOT_EDIT;
                        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                        ToolManager.Tool tool = toolManager.createTool(mNextToolMode, null);
                        tool.onLongPress(e);

                        if (mNextToolMode != tool.getNextToolMode()) {
                            mNextToolMode = tool.getNextToolMode();
                        } else {
                            mNextToolMode = getToolMode();
                        }
                    }
                }
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
        }
        return false;
    }

    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        // We are scrolling
        super.onDraw(canvas, tfm);

    }

    private void selectAnnot(int x, int y) {
        mAnnotPageNum = 0;
        // Since find text locks the document, cancel it to release the document.
        mPdfViewCtrl.cancelFindText();
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            Annot a = mPdfViewCtrl.getAnnotationAt(x, y);

            if (a != null && a.isValid()) {
                setAnnot(a, mPdfViewCtrl.getPageNumberFromScreenPt(x, y));
                buildAnnotBBox();
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex, "RectLinCreate failed to selectAnnot");
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }
}
