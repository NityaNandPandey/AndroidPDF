//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.SparseArray;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.annots.Ink;
import com.pdftron.pdf.annots.Line;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * This class manages annotation indicators
 */
public class AnnotIndicatorManger {

    /**
     * Page is in a normal state that is "good" for drawing annotation indicators.
     */
    public static final int STATE_IS_NORMAL = 0;
    /**
     * Page is being zoomed.
     */
    public static final int STATE_IS_ZOOMING = 1;
    /**
     * Page has been flung. Only used in continuous modes.
     */
    public static final int STATE_IS_FLUNG = 2;

    private static final int INDICATOR_ICON_SIZE = 8;

    final private Object mPrepareAnnotIndicatorLock = new Object();
    final private List<Integer> mAnnotIndicatorTaskList = new ArrayList<>();
    final private SparseArray<List<AnnotIndicator>> mAnnotIndicators = new SparseArray<>();
    final private Paint mAnnotInsideIndicatorPaint = new Paint();
    final private Paint mAnnotOutsideIndicatorPaint = new Paint();

    private int mState = STATE_IS_NORMAL;
    private Bitmap mInsideIndicatorBitmap;
    private Bitmap mOutsideIndicatorBitmap;
    private Bitmap mCustomBitmap;
    private int mIndicatorIconSize;
    private PDFViewCtrl.PagePresentationMode mLastPagePresentationMode;
    private WeakReference<Context> mContextRef;
    private WeakReference<PDFViewCtrl> mPdfViewCtrlRef;
    private WeakReference<ToolManager> mToolManagerRef;
    private boolean shouldRunAgain = true;
    private boolean mCanceled;

    public AnnotIndicatorManger(ToolManager toolManager) {
        mToolManagerRef = new WeakReference<>(toolManager);
        if (toolManager.getPDFViewCtrl() == null) {
            throw new NullPointerException("PDFfViewCtrl can't be null");
        }
        Context context = toolManager.getPDFViewCtrl().getContext();
        mContextRef = new WeakReference<>(context);
        mPdfViewCtrlRef = new WeakReference<>(toolManager.getPDFViewCtrl());

        mInsideIndicatorBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.indicator_inside);
        mIndicatorIconSize =  (int) Utils.convDp2Pix(context, INDICATOR_ICON_SIZE);
        mAnnotInsideIndicatorPaint.setAlpha((int) (.7 * 255));
        mOutsideIndicatorBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.indicator_outside);
        mAnnotOutsideIndicatorPaint.setAlpha((int) (.7 * 255));
        mAnnotOutsideIndicatorPaint.setColor(context.getResources().getColor(R.color.dark_gray));

        (new Thread(new Runnable() {
            public void run() {
                while (shouldRunAgain && mContextRef.get() != null) {
                    execute();
                    mCanceled = false;
                }
            }
        })).start();
    }

    /**
     * Draws annotation indicators onto the specified canvas.
     *
     * @param canvas The canvas
     */
    @UiThread
    public void drawAnnotIndicators(Canvas canvas) {
        PDFViewCtrl pdfViewCtrl = mPdfViewCtrlRef.get();
        if (pdfViewCtrl == null) {
            return;
        }

        if (mLastPagePresentationMode != pdfViewCtrl.getPagePresentationMode()) {
            mLastPagePresentationMode = pdfViewCtrl.getPagePresentationMode();
            reset(true);
        }

        for (int pageNum : pdfViewCtrl.getVisiblePagesInTransition()) {
            List<AnnotIndicator> annotIndicators;
            synchronized (mAnnotIndicators) {
                annotIndicators = mAnnotIndicators.get(pageNum);
            }
            if (annotIndicators == null
                && mState != STATE_IS_FLUNG
                && mState != STATE_IS_ZOOMING) {
                makeAnnotIndicators(pageNum);
            }

            if (annotIndicators != null) {
                for (AnnotIndicator annotIndicator : annotIndicators) {
                    ColorFilter filter = new PorterDuffColorFilter(annotIndicator.color, PorterDuff.Mode.SRC_IN);
                    mAnnotInsideIndicatorPaint.setColorFilter(filter);
                    RectF locRect = new RectF(
                        annotIndicator.x,
                        annotIndicator.y - mIndicatorIconSize,
                        annotIndicator.x + mIndicatorIconSize,
                        annotIndicator.y);
                    if (pdfViewCtrl.isMaintainZoomEnabled()) {
                        canvas.save();
                        try {
                            int dx = pdfViewCtrl.getScrollXOffsetInTools(pageNum);
                            int dy = (pdfViewCtrl.isCurrentSlidingCanvas(pageNum) ? 0
                                : pdfViewCtrl.getSlidingScrollY()) - pdfViewCtrl.getScrollYOffsetInTools(pageNum);
                            canvas.translate(dx, dy);
                            if (mCustomBitmap != null) {
                                canvas.drawBitmap(mCustomBitmap, null, locRect, null);
                            } else {
                                canvas.drawBitmap(mOutsideIndicatorBitmap, null, locRect, mAnnotOutsideIndicatorPaint);
                                canvas.drawBitmap(mInsideIndicatorBitmap, null, locRect, mAnnotInsideIndicatorPaint);
                                canvas.drawBitmap(mOutsideIndicatorBitmap, null, locRect, mAnnotInsideIndicatorPaint);
                            }
                        } finally {
                            canvas.restore();
                        }
                    } else {
                        if (mCustomBitmap != null) {
                            canvas.drawBitmap(mCustomBitmap, null, locRect, null);
                        } else {
                            canvas.drawBitmap(mOutsideIndicatorBitmap, null, locRect, mAnnotOutsideIndicatorPaint);
                            canvas.drawBitmap(mInsideIndicatorBitmap, null, locRect, mAnnotInsideIndicatorPaint);
                            canvas.drawBitmap(mOutsideIndicatorBitmap, null, locRect, mAnnotInsideIndicatorPaint);
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets the custom indicator icon.
     * Pass null if you want the default indicator icon.
     *
     * @param icon The custom indicator icon
     */
    @SuppressWarnings("unused")
    public void setCustomIndicatorBitmap(@Nullable Bitmap icon) {
        mCustomBitmap = icon;
    }

    /**
     * Sets the custom indicator icon.
     * Pass null if you want the default indicator icon.
     *
     * @param drawableId The drawable ID of the custom indicator icon
     */
    @SuppressWarnings("unused")
    public void setCustomIndicatorBitmap(@DrawableRes int drawableId) {
        Context context = mContextRef.get();
        if (context != null) {
            mCustomBitmap = BitmapFactory.decodeResource(context.getResources(), drawableId);
        }
    }

    /**
     * Sets the size of indicator icons.
     *
     * @param size The size of indicator icons
     */
    @SuppressWarnings("unused")
    public void setIndicatorIconSize(@IntRange(from=0) int size) {
        mIndicatorIconSize = size;
    }

    /**
     * Updates state.
     *
     * @param state The state that can be one of
     *              {@link #STATE_IS_NORMAL},
     *              {@link #STATE_IS_ZOOMING},
     *              {@link #STATE_IS_FLUNG}
     */
    public void updateState(int state) {
        mState = state;
    }

    /**
     * Resets annotation indicators
     *
     * @param all True if all annotation indicators should be reset,
     *            False if only visible annotation indicators should be reset
     */
    public void reset(boolean all) {
        PDFViewCtrl pdfViewCtrl = mPdfViewCtrlRef.get();
        if (pdfViewCtrl == null) {
            return;
        }

        if (all) {
            synchronized (mAnnotIndicators) {
                mAnnotIndicators.clear();
            }
            synchronized (mAnnotIndicatorTaskList) {
                mAnnotIndicatorTaskList.clear();
            }

            mCanceled = true;
            synchronized (mPrepareAnnotIndicatorLock) {
                mPrepareAnnotIndicatorLock.notifyAll();
            }
        } else {
            for (int pageNum : pdfViewCtrl.getVisiblePagesInTransition()) {
                synchronized (mAnnotIndicators) {
                    mAnnotIndicators.remove(pageNum);
                }
                synchronized (mAnnotIndicatorTaskList) {
                    mAnnotIndicatorTaskList.remove((Integer) pageNum);
                }
            }
        }
    }

    /**
     * Cleans up resources.
     */
    public void cleanup() {
        shouldRunAgain = false;
        mCanceled = true;
        synchronized (mPrepareAnnotIndicatorLock) {
            mPrepareAnnotIndicatorLock.notifyAll();
        }
    }

    @UiThread
    private void makeAnnotIndicators(int pageNum) {
        PDFViewCtrl pdfViewCtrl = mPdfViewCtrlRef.get();
        if (pdfViewCtrl == null) {
            return;
        }

        boolean visible = false;
        for (int i : pdfViewCtrl.getVisiblePages()) {
            if (i == pageNum) {
                visible = true;
                break;
            }
        }

        synchronized (mAnnotIndicatorTaskList) {
            if (mAnnotIndicatorTaskList.contains(pageNum)) {
                if (mAnnotIndicatorTaskList.get(0) == pageNum) {
                    // already in process
                    return;
                }

                if (!visible) {
                    // no need to change the order
                    return;
                }

                // if it is visible then put it in a high priority
                mAnnotIndicatorTaskList.remove((Integer) pageNum);
            }

            if (visible) {
                // high priority
                mAnnotIndicatorTaskList.add(0, pageNum);
            } else {
                // low priority
                mAnnotIndicatorTaskList.add(pageNum);
            }
        }
        synchronized (mPrepareAnnotIndicatorLock) {
            mPrepareAnnotIndicatorLock.notifyAll();
        }
    }

    private void execute() {
        Integer pageNum = null;

        while (!isCanceled()) {
            try {
                while (!isCanceled()) {
                    synchronized (mAnnotIndicatorTaskList) {
                        if (!mAnnotIndicatorTaskList.isEmpty()) {
                            pageNum = mAnnotIndicatorTaskList.get(0);
                            break;
                        }
                    }
                    synchronized (mPrepareAnnotIndicatorLock) {
                        mPrepareAnnotIndicatorLock.wait();
                    }
                }

                if (isCanceled()) {
                    return;
                }

                prepareAnnotIndicators(pageNum);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void prepareAnnotIndicators(final Integer pageNumI) {
        PDFViewCtrl pdfViewCtrl = mPdfViewCtrlRef.get();
        if (pdfViewCtrl == null) {
            return;
        }

        final int pageNum = pageNumI;
        boolean shouldUnlockRead = false;
        try {
            pdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            PDFDoc doc = pdfViewCtrl.getDoc();
            if (doc == null) {
                return;
            }
            final List<AnnotIndicator> annotIndicatorList = new ArrayList<>();
            ArrayList<Annot> annots = pdfViewCtrl.getAnnotationsOnPage(pageNum);
            Page page = doc.getPage(pageNum);
            if (page != null && page.isValid()) {
                for (Annot annot : annots) {
                    if (isCanceled()) {
                        return;
                    }

                    synchronized (mAnnotIndicatorTaskList) {
                        if (!mAnnotIndicatorTaskList.contains(pageNumI)) {
                            // no need more processing since it's been removed
                            return;
                        }
                    }

                    try {
                        if (annot == null || !annot.isValid()) {
                            continue;
                        }
                        if (!shouldShowIndicator(annot)) {
                            continue;
                        }

                        if (isCanceled()) {
                            return;
                        }

                        addAnnot(pdfViewCtrl, annotIndicatorList, annot, page, pageNum);
                    } catch (PDFNetException ignored) {
                        // this annotation has some problem, let's skip it and continue with others
                    }
                }
            }

            boolean invalidate = false;
            synchronized (mAnnotIndicatorTaskList) {
                if (mAnnotIndicatorTaskList.contains(pageNum)) {
                    mAnnotIndicatorTaskList.remove(pageNumI);
                    invalidate = true;
                }
            }
            if (invalidate) {
                pdfViewCtrl.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isCanceled()) {
                            return;
                        }
                        PDFViewCtrl pdfViewCtrl = mPdfViewCtrlRef.get();
                        if (pdfViewCtrl == null) {
                            return;
                        }
                        synchronized (mAnnotIndicators) {
                            mAnnotIndicators.put(pageNum, annotIndicatorList);
                        }
                        pdfViewCtrl.invalidate();
                    }
                });
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }
    }

    private void addAnnot(PDFViewCtrl pdfViewCtrl,
                          List<AnnotIndicator> annotList,
                          Annot annot,
                          Page page,
                          int pageNum) throws PDFNetException {
        com.pdftron.pdf.Rect rect = pdfViewCtrl.getPageRectForAnnot(annot, pageNum);
        double[] point;
        double x1, y1, x2, y2;
        x1 = rect.getX1();
        x2 = rect.getX2();
        y1 = rect.getY1();
        y2 = rect.getY2();
        double x = x2; // right
        double y = y2; // top
        int rotation = page.getRotation();
        if (rotation == Page.e_90 || rotation == Page.e_180) {
            x = x1;
        }
        if (rotation == Page.e_270 || rotation == Page.e_180) {
            y = y1;
        }
        int type = annot.getType();
        switch (type) {
            case Annot.e_Line:
                Line line = new Line(annot);
                Point ptStart = line.getStartPoint();
                Point ptEnd = line.getEndPoint();

                switch (page.getRotation()) {
                    case Page.e_90:
                        if (ptStart.x < ptEnd.x) {
                            y = ptStart.y;
                        } else {
                            y = ptEnd.y;
                        }
                        break;
                    case Page.e_180:
                        if (ptStart.y < ptEnd.y) {
                            x = ptStart.x;
                        } else {
                            x = ptEnd.x;
                        }
                        break;
                    case Page.e_270:
                        if (ptStart.x > ptEnd.x) {
                            y = ptStart.y;
                        } else {
                            y = ptEnd.y;
                        }
                        break;
                    default:
                        if (ptStart.y > ptEnd.y) {
                            x = ptStart.x;
                        } else {
                            x = ptEnd.x;
                        }
                        break;
                }
                break;
            case Annot.e_Circle:
                double c1, c2, r1, r2, thresh, temp;
                c1 = (x1 + x2) / 2.;
                c2 = (y1 + y2) / 2.;
                r1 = Math.abs(x1 - x2) * Math.abs(x1 - x2) / 4.;
                r2 = Math.abs(y1 - y2) * Math.abs(y1 - y2) / 4.;

                switch (page.getRotation()) {
                    case Page.e_90:
                        thresh = Math.min(x1, x2) + Math.abs(x1 - x2) * .15;
                        x = thresh;
                        temp = (x - c1) * (x - c1) / r1;
                        temp = (1 - temp) * r2;
                        y = c2 + Math.sqrt(temp);
                        break;
                    case Page.e_180:
                        thresh = Math.min(y1, y2) + Math.abs(y1 - y2) * .15;
                        y = thresh;
                        temp = (y - c2) * (y - c2) / r2;
                        temp = (1 - temp) * r1;
                        x = c1 - Math.sqrt(temp);
                        break;
                    case Page.e_270:
                        thresh = Math.min(x1, x2) + Math.abs(x1 - x2) * .85;
                        x = thresh;
                        temp = (x - c1) * (x - c1) / r1;
                        temp = (1 - temp) * r2;
                        y = c2 - Math.sqrt(temp);
                        break;
                    default:
                        thresh = Math.min(y1, y2) + Math.abs(y1 - y2) * .85;
                        y = thresh;
                        temp = (y - c2) * (y - c2) / r2;
                        temp = (1 - temp) * r1;
                        x = Math.sqrt(temp) + c1;
                        break;
                }
                break;
            case Annot.e_Ink:
                Ink ink = new Ink(annot);
                switch (page.getRotation()) {
                    case Page.e_90:
                        thresh = Math.min(x1, x2) + Math.abs(x1 - x2) * .15;
                        y = y1;
                        break;
                    case Page.e_180:
                        thresh = Math.min(y1, y2) + Math.abs(y1 - y2) * .15;
                        x = x2;
                        break;
                    case Page.e_270:
                        thresh = Math.min(x1, x2) + Math.abs(x1 - x2) * .85;
                        y = y2;
                        break;
                    default:
                        thresh = Math.min(y1, y2) + Math.abs(y1 - y2) * .85;
                        x = x1;
                        break;
                }

                for (int pathIndex = 0, cnt = ink.getPathCount(); pathIndex < cnt; ++pathIndex) {
                    if (isCanceled()) {
                        return;
                    }

                    int pointCount = ink.getPointCount(pathIndex);
                    for (int pointIndex = 0; pointIndex < pointCount; ++pointIndex) {
                        if (isCanceled()) {
                            return;
                        }

                        Point p = ink.GetPoint(pathIndex, pointIndex);

                        switch (page.getRotation()) {
                            case Page.e_90:
                                if (p.x < thresh) {
                                    if (p.y > y) {
                                        x = p.x;
                                        y = p.y;
                                    }
                                }
                                break;
                            case Page.e_180:
                                if (p.y < thresh) {
                                    if (p.x < x) {
                                        x = p.x;
                                        y = p.y;
                                    }
                                }
                                break;
                            case Page.e_270:
                                if (p.x > thresh) {
                                    if (p.y < y) {
                                        x = p.x;
                                        y = p.y;
                                    }
                                }
                                break;
                            default:
                                if (p.y > thresh) {
                                    if (p.x > x) {
                                        x = p.x;
                                        y = p.y;
                                    }
                                }
                                break;
                        }
                    }
                }
                break;
        }

        if (pdfViewCtrl.isContinuousPagePresentationMode(pdfViewCtrl.getPagePresentationMode())) {
            point = pdfViewCtrl.convPagePtToScreenPt(x, y, pageNum);
            x = point[0] + pdfViewCtrl.getScrollX();
            y = point[1] + pdfViewCtrl.getScrollY();
        } else {
            point = pdfViewCtrl.convPagePtToHorizontalScrollingPt(x, y, pageNum);
            x = point[0];
            y = point[1];
        }

        if (isCanceled()) {
            return;
        }

        annotList.add(new AnnotIndicator(pageNum, Utils.colorPt2color(annot.getColorAsRGB()), (float) x, (float) y));
    }

    private boolean shouldShowIndicator(Annot annot) {
        if (annot == null) {
            return false;
        }
        ToolManager toolManager = mToolManagerRef.get();
        if (toolManager == null) {
            return false;
        }

        try {
            int type = annot.getType();
            if (!annot.isValid()
                || !annot.isMarkup()
                || annot.getFlag(Annot.e_hidden)
                || type == Annot.e_FreeText
                || type == Annot.e_Text) {
                return false;
            }

            if (AnnotUtils.isRuler(annot)) {
                return false;
            }

            if (toolManager.getAnnotManager() != null) {
                return toolManager.getAnnotManager().shouldShowIndicator(annot);
            }

            Markup markup = new Markup(annot);
            String note = markup.getContents();
            if (note == null || note.equals("")) {
                return false;
            }

            if (Utils.isTextCopy(markup)) {
                return false;
            }
        } catch (Exception ignored) {
            return false;
        }

        return true;
    }

    private boolean isCanceled() {
        return mCanceled || mContextRef.get() == null;
    }

    private class AnnotIndicator {
        int pageNum;
        int color;
        float x, y;

        AnnotIndicator(int pageNum, int color, float x, float y) {
            this.pageNum = pageNum;
            this.color = color;
            this.x = x;
            this.y = y;
        }
    }
}
