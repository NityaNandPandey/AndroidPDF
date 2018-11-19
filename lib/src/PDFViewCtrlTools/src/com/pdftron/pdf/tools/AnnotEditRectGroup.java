//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is for selecting a group of annotations
 */
@Keep
public class AnnotEditRectGroup extends AnnotEdit {

    private static final String TAG = AnnotEditRectGroup.class.getName();
    private HashMap<Annot, Integer> mSelectedAnnotsMap = new HashMap<>();
    // Touch-down point and moving point:
    protected PointF mPt1 = new PointF(0, 0);
    protected PointF mPt2 = new PointF(0, 0);
    private Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF mSelectedArea = new RectF();
    private int mDownPageNum;
    private boolean mResizeAnnots;

    /**
     * Class constructor
     */
    public AnnotEditRectGroup(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();
        mFillPaint.setStyle(Paint.Style.FILL);

        TypedArray a = ctrl.getContext().obtainStyledAttributes(null, R.styleable.RectGroupAnnotEdit, R.attr.rect_group_annot_edit_style, R.style.RectGroupAnnotEdit);
        try {
            int color = a.getColor(R.styleable.RectGroupAnnotEdit_fillColor, Color.BLUE);
            float opacity = a.getFloat(R.styleable.RectGroupAnnotEdit_fillOpacity, 0.38f);
            mFillPaint.setColor(color);
            mFillPaint.setAlpha((int) (opacity * 255));
        } finally {
            a.recycle();
        }
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.ANNOT_EDIT_RECT_GROUP;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);
        if (mEffCtrlPtId == e_unknown) {
            // The first touch-down point
            mPt1.x = e.getX() + mPdfViewCtrl.getScrollX();
            mPt1.y = e.getY() + mPdfViewCtrl.getScrollY();

            // Remembers which page is touched initially and that is the page where
            // the annotation is going to reside on.
            mDownPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(e.getX(), e.getY());
            if (mDownPageNum < 1) {
                mDownPageNum = mPdfViewCtrl.getCurrentPage();
            }

            // The moving point that is the same with the touch-down point initiallyAnnotationTool
            mPt2.set(mPt1);
            mResizeAnnots = false;
            mSelectedArea.setEmpty();
        }
        if (mDownPageNum >= 1) {
            mPageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mDownPageNum);
            Utils.snapPointToRect(mPt1, mPageCropOnClientF);
        }
        return false;
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

        super.onMove(e1, e2, x_dist, y_dist);
        if (mEffCtrlPtId != e_unknown) {
            mResizeAnnots = true;
            return true;
        }

        mAllowTwoFingerScroll = e1.getPointerCount() == 2 || e2.getPointerCount() == 2;

        // check to see whether use finger to scroll or not
        mAllowOneFingerScrollWithStylus = mStylusUsed && e2.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS;

        if (mAllowTwoFingerScroll || mAllowOneFingerScrollWithStylus) {
            mPdfViewCtrl.setBuiltInPageSlidingEnabled(true);
        } else {
            mPdfViewCtrl.setBuiltInPageSlidingEnabled(false);
        }

        if (mAllowTwoFingerScroll) {
            return false;
        }

        if (mAllowOneFingerScrollWithStylus) {
            return false;
        }

        // While moving, update the moving point so that a rubber band can be shown to
        // indicate the bounding box of the resulting annotation.
        float x = mPt2.x;
        float y = mPt2.y;
        mPt2.x = e2.getX() + mPdfViewCtrl.getScrollX();
        mPt2.y = e2.getY() + mPdfViewCtrl.getScrollY();

        Utils.snapPointToRect(mPt2, mPageCropOnClientF);

        float min_x = Math.min(Math.min(x, mPt2.x), mPt1.x);
        float max_x = Math.max(Math.max(x, mPt2.x), mPt1.x);
        float min_y = Math.min(Math.min(y, mPt2.y), mPt1.y);
        float max_y = Math.max(Math.max(y, mPt2.y), mPt1.y);

        mPdfViewCtrl.invalidate((int) min_x, (int) min_y, (int) Math.ceil(max_x), (int) Math.ceil(max_y));
        return true;
    }

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

        if (hasAnnotSelected() && mResizeAnnots) {
            mResizeAnnots = false;
            return super.onUp(e, priorEventMode);
        }

        if (mPt1.equals(mPt2)) {
            return true;
        }

        // In stylus mode, ignore finger input
        mAllowOneFingerScrollWithStylus = mStylusUsed && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS;
        if (mAllowOneFingerScrollWithStylus) {
            return true;
        }

        if (!mSelectedArea.isEmpty()) {
            return skipOnUpPriorEvent(priorEventMode);
        }

        setCurrentDefaultToolModeHelper(getToolMode());

        try {
            float min_x = Math.min(mPt1.x, mPt2.x);
            float max_x = Math.max(mPt1.x, mPt2.x);
            float min_y = Math.min(mPt1.y, mPt2.y);
            float max_y = Math.max(mPt1.y, mPt2.y);
            mPt1.x = min_x;
            mPt1.y = min_y;
            mPt2.x = max_x;
            mPt2.y = max_y;

            RectF pageRect = getShapeBBox();

            ArrayList<Annot> annotsInPage = mPdfViewCtrl.getAnnotationsOnPage(mDownPageNum);
            RectF selectedRectInPage = new RectF();
            mSelectedAnnotsMap.clear();
            mAnnotIsTextMarkup = false;
            // search for selected annotations in page
            for (Annot annot : annotsInPage) {
                com.pdftron.pdf.Rect annotRect = mPdfViewCtrl.getPageRectForAnnot(annot, mDownPageNum);
                RectF annotPageRect = new RectF((float) annotRect.getX1(), (float) annotRect.getY1(), (float) annotRect.getX2(), (float) annotRect.getY2());
                if (RectF.intersects(pageRect, annotPageRect) && hasPermission(annot, ANNOT_PERMISSION_SELECTION) && isAnnotSupportEdit(annot)) {
                    if (!isAnnotSupportResize(annot)) {
                        mAnnotIsTextMarkup = true;
                    }
                    mSelectedAnnotsMap.put(annot, mDownPageNum);
                    selectedRectInPage.union(annotPageRect);
                }
            }

            // If there is only one annotation, let the other tool handles it
            if (hasAnnotSelected() && mSelectedAnnotsMap.size() == 1) {
                ArrayList<Annot> entry = new ArrayList<>(mSelectedAnnotsMap.keySet());
                mAnnot = entry.get(0);
                mAnnotPageNum = mDownPageNum;
                buildAnnotBBox();
                ((ToolManager) mPdfViewCtrl.getToolManager()).selectAnnot(mAnnot, mDownPageNum);
                return false;
            }

            // Sets annotations area and control points
            if (!selectedRectInPage.isEmpty()) {
                double[] pts1 = mPdfViewCtrl.convPagePtToScreenPt((double) selectedRectInPage.left, (double) selectedRectInPage.top, mDownPageNum);
                double[] pts2 = mPdfViewCtrl.convPagePtToScreenPt((double) selectedRectInPage.right, (double) selectedRectInPage.bottom, mDownPageNum);
                int scrollX = mPdfViewCtrl.getScrollX();
                int scrollY = mPdfViewCtrl.getScrollY();
                mSelectedArea = new RectF((float) pts1[0] + scrollX, (float) pts2[1] + scrollY, (float) pts2[0] + scrollX, (float) pts1[1] + scrollY);
                mAnnotPageNum = mDownPageNum;
                setCtrlPts();
                showMenu(getAnnotRect());
            }
        } catch (PDFNetException ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }

        // If there is no selected annotations, go back to Pan tool
        Rect dirtyRect = new Rect((int) mPt1.x, (int) mPt1.y, (int) mPt2.x, (int) mPt2.y);
        if (!hasAnnotSelected()) {
            resetPts();
            setNextToolModeHelper(ToolMode.PAN);
        } else {
            setNextToolModeHelper((ToolMode) getToolMode());
        }

        mPdfViewCtrl.invalidate(dirtyRect);
        return skipOnUpPriorEvent(priorEventMode);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        PointF pt = new PointF(e.getX() + mPdfViewCtrl.getScrollX(), e.getY() + mPdfViewCtrl.getScrollY());
        if (mSelectedArea.contains(pt.x, pt.y)) {
            showMenu(getAnnotRect());
        } else {
            backToPan();
        }
        return false;
    }

    /**
     * The overload implementation of {@link AnnotEdit#onPageTurning(int, int)}.
     */
    @Override
    public void onPageTurning(
        int old_page,
        int cur_page) {

        super.onPageTurning(old_page, cur_page);
        backToPan();

    }

    private void backToPan() {
        resetPts();
        mSelectedArea.setEmpty();
        mSelectedAnnotsMap.clear();
        mEffCtrlPtId = e_unknown;
        mAnnot = null;
        closeQuickMenu();
        setNextToolModeHelper(ToolMode.PAN);
    }

    @Override
    protected int getMenuResByAnnot(Annot annot) {
        return R.menu.annot_group;
    }

    /**
     * The overload implementation of {@link Tool#deleteAnnot()}.
     * Deletes a set of annotations.
     */
    @Override
    protected void deleteAnnot() {

        if (mPdfViewCtrl == null) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            PDFDoc pdfDoc = mPdfViewCtrl.getDoc();
            raiseAnnotationPreRemoveEvent(mSelectedAnnotsMap);

            for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
                Annot annot = entry.getKey();
                if (annot == null) {
                    continue;
                }
                int annotPageNum = entry.getValue();
                Page page = pdfDoc.getPage(annotPageNum);
                page.annotRemove(annot);
                mPdfViewCtrl.update(annot, annotPageNum);
            }

            raiseAnnotationRemovedEvent(mSelectedAnnotsMap);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        backToPan();

    }

    /**
     * The overload implementation of {@link Tool#flattenAnnot()}.
     * Flatten a set of annotations.
     */
    @Override
    protected void flattenAnnot() {

        if (mPdfViewCtrl == null) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            PDFDoc pdfDoc = mPdfViewCtrl.getDoc();
            raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);

            for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
                Annot annot = entry.getKey();
                if (annot == null) {
                    continue;
                }
                int annotPageNum = entry.getValue();
                Page page = pdfDoc.getPage(annotPageNum);
                annot.flatten(page);
                mPdfViewCtrl.update(annot, annotPageNum);
            }

            raiseAnnotationModifiedEvent(mSelectedAnnotsMap);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        backToPan();

    }

    @Override
    protected RectF getAnnotRect() {
        RectF annotsRect = new RectF(mSelectedArea);
        annotsRect.offset(-mPdfViewCtrl.getScrollX(), -mPdfViewCtrl.getScrollY());
        return annotsRect;
    }

    /**
     * Edits selected annotations size
     *
     * @param priorEventMode prior event mode
     * @return true if successfully modified annotations size, false otherwise
     */
    @Override
    protected boolean editAnnotSize(PDFViewCtrl.PriorEventMode priorEventMode) {
        float x1 = mCtrlPts[e_ul].x;
        float y1 = mCtrlPts[e_ul].y;
        float x2 = mCtrlPts[e_lr].x;
        float y2 = mCtrlPts[e_lr].y;

        RectF ctrlRect = new RectF(x1, y1, x2, y2);
        // skip calculate annots size if there is no movement or resize
        if (ctrlRect.equals(mSelectedArea)) {
            return true;
        }
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            raiseAnnotationPreModifyEvent(mSelectedAnnotsMap);
            updateSelectedAnnotSize();
            raiseAnnotationModifiedEvent(mSelectedAnnotsMap);

        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
        return true;
    }

    private void updateSelectedAnnotSize() throws PDFNetException {
        int scrollX = mPdfViewCtrl.getScrollX();
        int scrollY = mPdfViewCtrl.getScrollY();
        // current control point location
        float x1 = mCtrlPts[e_ul].x - scrollX;
        float y1 = mCtrlPts[e_ul].y - scrollY;
        float x2 = mCtrlPts[e_lr].x - scrollX;
        float y2 = mCtrlPts[e_lr].y - scrollY;
        RectF selectedArea = new RectF(mSelectedArea);
        selectedArea.offset(-scrollX, -scrollY);

        double[] ctrlPagePt1 = mPdfViewCtrl.convScreenPtToPagePt(x1, y1, mDownPageNum);
        double[] ctrlPagePt2 = mPdfViewCtrl.convScreenPtToPagePt(x2, y2, mDownPageNum);
        double[] selectedPagePt1 = mPdfViewCtrl.convScreenPtToPagePt(selectedArea.left, selectedArea.top, mDownPageNum);
        double[] selectedPagePt2 = mPdfViewCtrl.convScreenPtToPagePt(selectedArea.right, selectedArea.bottom, mDownPageNum);

        float offsetX = (float) ctrlPagePt1[0];
        float offsetY = (float) ctrlPagePt2[1];
        float scaleX = (float) ((ctrlPagePt2[0] - ctrlPagePt1[0]) / (selectedPagePt2[0] - selectedPagePt1[0]));
        float scaleY = (float) ((ctrlPagePt2[1] - ctrlPagePt1[1]) / (selectedPagePt2[1] - selectedPagePt1[1]));
        RectF nextSelectArea = new RectF();
        for (Map.Entry<Annot, Integer> entry : mSelectedAnnotsMap.entrySet()) {
            Annot annot = entry.getKey();
            int annotPageNum = entry.getValue();

            if (!isAnnotSupportResize(annot)) {
                continue;
            }

            com.pdftron.pdf.Rect rect = mPdfViewCtrl.getPageRectForAnnot(annot, annotPageNum);
            rect.normalize();
            RectF annotRect = new RectF((float) rect.getX1(), (float) rect.getY1(), (float) rect.getX2(), (float) rect.getY2());
            annotRect.offset((float) -selectedPagePt1[0], (float) -selectedPagePt2[1]);

            com.pdftron.pdf.Rect newAnnotRect = new com.pdftron.pdf.Rect(
                (double) annotRect.left * scaleX + offsetX,
                (double) annotRect.top * scaleY + offsetY,
                (double) annotRect.right * scaleX + offsetX,
                (double) annotRect.bottom * scaleY + offsetY);
            newAnnotRect.normalize();
            // It is possible during viewing that GetRect does not return the most accurate bounding box
            // of what is actually rendered, to obtain the correct behavior when resizing/moving, we
            // need to call refreshAppearance before resize
            if (annot.getType() != Annot.e_Stamp && annot.getType() != Annot.e_Text) {
                annot.refreshAppearance();
            }
            annot.resize(newAnnotRect);
            // We do not want to call refreshAppearance for stamps
            // to not alter their original appearance.
            if (annot.getType() != Annot.e_Stamp) {
                AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), annot);
            }

            mPdfViewCtrl.update(annot, annotPageNum);

            com.pdftron.pdf.Rect newAnnotScreenRect = mPdfViewCtrl.getScreenRectForAnnot(annot, annotPageNum);
            nextSelectArea.union(new RectF((float) newAnnotScreenRect.getX1(), (float) newAnnotScreenRect.getY1(), (float) newAnnotScreenRect.getX2(), (float) newAnnotScreenRect.getY2()));
        }

        mPdfViewCtrl.update(new com.pdftron.pdf.Rect(selectedArea.left, selectedArea.top, selectedArea.right, selectedArea.bottom));

        nextSelectArea.offset(scrollX, scrollY);
        mSelectedArea.set(nextSelectArea);
        mPt1.set(mSelectedArea.left, mSelectedArea.top);
        mPt2.set(mSelectedArea.right, mSelectedArea.bottom);
        setCtrlPts();
    }

    private boolean isAnnotSupportEdit(Annot annot) throws PDFNetException {
        return annot.isValid() && (annot.getType() != Annot.e_Widget && annot.getType() != Annot.e_Link || isMadeByPDFTron(annot));
    }

    private boolean isAnnotSupportResize(Annot annot) throws PDFNetException {
        return annot.getType() != Annot.e_Highlight
            && annot.getType() != Annot.e_StrikeOut
            && annot.getType() != Annot.e_Underline
            && annot.getType() != Annot.e_Squiggly
            && annot.getType() != Annot.e_Text
            && annot.getType() != Annot.e_Stamp;
    }

    private void resetPts() {
        mPt1.set(0, 0);
        mPt2.set(0, 0);
        mBBox.setEmpty();
    }

    private RectF getShapeBBox() {
        // Computes the bounding box of the rubber band in page space.
        double[] pts1;
        double[] pts2;
        pts1 = mPdfViewCtrl.convScreenPtToPagePt(mPt1.x - mPdfViewCtrl.getScrollX(), mPt1.y - mPdfViewCtrl.getScrollY(), mDownPageNum);
        pts2 = mPdfViewCtrl.convScreenPtToPagePt(mPt2.x - mPdfViewCtrl.getScrollX(), mPt2.y - mPdfViewCtrl.getScrollY(), mDownPageNum);
        RectF rect;
        try {
            rect = new RectF((float) pts1[0], (float) pts2[1], (float) pts2[0], (float) pts1[1]);
            return rect;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected RectF getScreenRect(com.pdftron.pdf.Rect screen_rect) {
        if (hasAnnotSelected() && mSelectedAnnotsMap.size() == 1) {
            return super.getScreenRect(screen_rect);
        }
        return new RectF(mSelectedArea);
    }

    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        // We are scrolling
        if (mAllowTwoFingerScroll) {
            return;
        }

        if (!mSelectedArea.isEmpty()) {
            super.onDraw(canvas, tfm);
        } else {
            int min_x = (int) Math.min(mPt1.x, mPt2.x);
            int max_x = (int) Math.max(mPt1.x, mPt2.x);
            int min_y = (int) Math.min(mPt1.y, mPt2.y);
            int max_y = (int) Math.max(mPt1.y, mPt2.y);

            Rect fillRect = new Rect(min_x, min_y, max_x, max_y);
            if (!fillRect.isEmpty()) {
                canvas.drawRect(fillRect, mFillPaint);
            }
        }
    }


    /**
     * The overload implementation of {@link AnnotEdit#hasAnnotSelected()}.
     */
    @Override
    protected boolean hasAnnotSelected() {

        return !mSelectedAnnotsMap.isEmpty();

    }
}
