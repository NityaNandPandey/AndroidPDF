//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Annot.BorderStyle;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

/**
 * This class is the base class for several shape creation classes,
 * e.g., LineCreate, OvalCreate, etc.
 */
@Keep
public abstract class SimpleShapeCreate extends Tool {

    private static final String TAG = SimpleShapeCreate.class.getName();
    protected PointF mPt1, mPt2;    // Touch-down point and moving point
    protected Paint mPaint;
    protected Paint mFillPaint;
    protected int mDownPageNum;
    protected RectF mPageCropOnClientF;
    protected float mThickness;
    protected float mThicknessDraw;
    protected int mStrokeColor;
    protected int mFillColor;
    protected float mOpacity;
    protected boolean mIsAllPointsOutsidePage;
    protected boolean mHasFill;

    protected final int START_DRAWING_THRESHOLD = 5;

    /**
     * Class constructor
     */
    public SimpleShapeCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mPt1 = new PointF(0, 0);
        mPt2 = new PointF(0, 0);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLUE);
        mPaint.setStyle(Paint.Style.STROKE);
        mFillPaint = new Paint(mPaint);
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(Color.TRANSPARENT);
        mThickness = 1.0f;
        mThicknessDraw = 1.0f;
        mHasFill = false;
    }

    /**
     * The overload interface of {@link Tool#getToolMode()} ()}.
     */
    @Override
    abstract public ToolManager.ToolModeBase getToolMode();

    @Override
    abstract public int getCreateAnnotType();


    /**
     * The overload implementation of {@link Tool#setupAnnotProperty(int, float, float, int, String, String)}.
     */
    @Override
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName) {
        mStrokeColor = color;
        mFillColor = fillColor;
        mOpacity = opacity;
        mThickness = thickness;

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(getColorKey(getCreateAnnotType()), mStrokeColor);
        editor.putInt(getColorFillKey(getCreateAnnotType()), mFillColor);
        editor.putFloat(getOpacityKey(getCreateAnnotType()), mOpacity);
        editor.putFloat(getThicknessKey(getCreateAnnotType()), mThickness);
        editor.apply();
    }

    /**
     * The overload implementation of {@link Tool#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return true;
    }

    /**
     * The overload implementation of {@link Tool#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mPdfViewCtrl.invalidate();
    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        ToolMode toolMode = ToolManager.getDefaultToolMode(getToolMode());
        if (mForceSameNextToolMode &&
            toolMode != ToolMode.INK_CREATE &&
            toolMode != ToolMode.INK_ERASER &&
            toolMode != ToolMode.TEXT_ANNOT_CREATE) {

            int x = (int) (e.getX() + 0.5);
            int y = (int) (e.getY() + 0.5);

            Annot tempAnnot = mPdfViewCtrl.getAnnotationAt(x, y);
            int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);

            setCurrentDefaultToolModeHelper(toolMode);
            try {
                if (null != tempAnnot && tempAnnot.isValid()) {
                    ((ToolManager) mPdfViewCtrl.getToolManager()).selectAnnot(tempAnnot, page);
                } else {
                    mNextToolMode = getToolMode();
                }
            } catch (PDFNetException ignored) {
            }

            return false;
        }
        return super.onSingleTapConfirmed(e);
    }

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);
        // The first touch-down point
        mPt1.x = e.getX() + mPdfViewCtrl.getScrollX();
        mPt1.y = e.getY() + mPdfViewCtrl.getScrollY();

        // Remembers which page is touched initially and that is the page where
        // the annotation is going to reside on.
        mDownPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(e.getX(), e.getY());
        if (mDownPageNum < 1) {
            mIsAllPointsOutsidePage = true;
            mDownPageNum = mPdfViewCtrl.getCurrentPage();
        } else {
            mIsAllPointsOutsidePage = false;
        }
        if (mDownPageNum >= 1) {
            mPageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mDownPageNum);
            Utils.snapPointToRect(mPt1, mPageCropOnClientF);
        }

        // The moving point that is the same with the touch-down point initially
        mPt2.set(mPt1);

        // Query for the default thickness and color, which are to be used when the
        // annotation is created in the derived classes.
        Context context = mPdfViewCtrl.getContext();
        SharedPreferences settings = Tool.getToolPreferences(context);
        mThickness = settings.getFloat(getThicknessKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultThickness(context, getCreateAnnotType()));
        mStrokeColor = settings.getInt(getColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultColor(context, getCreateAnnotType()));
        mFillColor = settings.getInt(getColorFillKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultFillColor(context, getCreateAnnotType()));
        mOpacity = settings.getFloat(getOpacityKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultOpacity(context, getCreateAnnotType()));

        float zoom = (float) mPdfViewCtrl.getZoom();
        mThicknessDraw = mThickness * zoom;
        mPaint.setStrokeWidth(mThicknessDraw);
        int color = Utils.getPostProcessedColor(mPdfViewCtrl, mStrokeColor);
        mPaint.setColor(color);
        mPaint.setAlpha((int) (255 * mOpacity));
        if (mHasFill) {
            mFillPaint.setColor(Utils.getPostProcessedColor(mPdfViewCtrl, mFillColor));
            mFillPaint.setAlpha((int) (255 * mOpacity));
        }

        mAnnotPushedBack = false;

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        if (mAllowTwoFingerScroll) {
            return false;
        }

        if (mAllowOneFingerScrollWithStylus) {
            return false;
        }

        if (mIsAllPointsOutsidePage) {
            // if any points was inside the page, it is OK to create the annot
            if (mPdfViewCtrl.getPageNumberFromScreenPt(e2.getX(), e2.getY()) >= 1) {
                mIsAllPointsOutsidePage = false;
            }
        }

        // While moving, update the moving point so that a rubber band can be shown to
        // indicate the bounding box of the resulting annotation.
        float x = mPt2.x;
        float y = mPt2.y;
        mPt2.x = e2.getX() + mPdfViewCtrl.getScrollX();
        mPt2.y = e2.getY() + mPdfViewCtrl.getScrollY();

        Utils.snapPointToRect(mPt2, mPageCropOnClientF);

        float min_x = Math.min(Math.min(x, mPt2.x), mPt1.x) - mThicknessDraw;
        float max_x = Math.max(Math.max(x, mPt2.x), mPt1.x) + mThicknessDraw;
        float min_y = Math.min(Math.min(y, mPt2.y), mPt1.y) - mThicknessDraw;
        float max_y = Math.max(Math.max(y, mPt2.y), mPt1.y) + mThicknessDraw;

        mPdfViewCtrl.invalidate((int) min_x, (int) min_y, (int) Math.ceil(max_x), (int) Math.ceil(max_y));
        return true;
    }

    /**
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        super.onUp(e, priorEventMode);

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
            resetPts();
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

        // In stylus mode, ignore finger input
        mAllowOneFingerScrollWithStylus = mStylusUsed && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS;
        if (mAllowOneFingerScrollWithStylus) {
            return true;
        }

        setNextToolModeHelper();
        setCurrentDefaultToolModeHelper(getToolMode());
        // add UI to drawing list
        addOldTools();

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            com.pdftron.pdf.Rect rect = getShapeBBox();
            if (rect != null) {
                Annot markup = createMarkup(mPdfViewCtrl.getDoc(), rect);
                setStyle(markup);

                markup.refreshAppearance();

                Page page = mPdfViewCtrl.getDoc().getPage(mDownPageNum);
                if (page != null) {
                    page.annotPushBack(markup);
                    mAnnotPushedBack = true;
                    setAnnot(markup, mDownPageNum);
                    buildAnnotBBox();
                    mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                    raiseAnnotationAddedEvent(mAnnot, mAnnotPageNum);
                }
            }
        } catch (Exception ex) {
            mNextToolMode = ToolMode.PAN;
            ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(ex.getMessage());
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
            onCreateMarkupFailed(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        return skipOnUpPriorEvent(priorEventMode);
    }

    /**
     * The overload implementation of {@link Tool#doneTwoFingerScrolling()}.
     */
    @Override
    protected void doneTwoFingerScrolling() {
        super.doneTwoFingerScrolling();

        mPt2.set(mPt1);
        mPdfViewCtrl.invalidate();
    }


    /**
     * The overload implementation of {@link Tool#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        if (mAllowTwoFingerScroll) {
            doneTwoFingerScrolling();
        }
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        // In the new version we allow scaling during annotation creation
        return false;
    }

    /**
     * @return The shape bounding box of the rubber band in page space
     */
    protected com.pdftron.pdf.Rect getShapeBBox() {
        // Computes the bounding box of the rubber band in page space.
        double[] pts1;
        double[] pts2;
        pts1 = mPdfViewCtrl.convScreenPtToPagePt(mPt1.x - mPdfViewCtrl.getScrollX(), mPt1.y - mPdfViewCtrl.getScrollY(), mDownPageNum);
        pts2 = mPdfViewCtrl.convScreenPtToPagePt(mPt2.x - mPdfViewCtrl.getScrollX(), mPt2.y - mPdfViewCtrl.getScrollY(), mDownPageNum);
        com.pdftron.pdf.Rect rect;
        try {
            rect = new com.pdftron.pdf.Rect(pts1[0], pts1[1], pts2[0], pts2[1]);
            return rect;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Sets the style of the specified annotations as the current style
     *
     * @param annot The annotation
     */
    protected void setStyle(Annot annot) {
        setStyle(annot, mHasFill);
    }

    /**
     * Sets the style of the specified annotations as the current style
     *
     * @param annot   The annotation
     * @param hasFill True if has fill property
     */
    protected void setStyle(Annot annot, boolean hasFill) {
        try {
            ColorPt color = Utils.color2ColorPt(mStrokeColor);
            annot.setColor(color, 3);

            if (hasFill && annot instanceof Markup) {
                color = Utils.color2ColorPt(mFillColor);
                if (mFillColor == Color.TRANSPARENT) {
                    ((Markup) annot).setInteriorColor(color, 0);
                } else {
                    ((Markup) annot).setInteriorColor(color, 3);
                }
            }

            if (annot instanceof Markup) {
                ((Markup) annot).setOpacity(mOpacity);
                setAuthor((Markup) annot);
            }

            BorderStyle bs = annot.getBorderStyle();
            if (mHasFill && mStrokeColor == Color.TRANSPARENT) {
                bs.setWidth(0);
            } else {
                bs.setWidth(mThickness);
            }
            annot.setBorderStyle(bs);
        } catch (PDFNetException ignored) {

        }
    }

    /**
     * create markup annotation, called in {@link #onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}
     *
     * @param doc  PDF Document
     * @param bbox bounding box
     * @return Markup annotation
     * @throws PDFNetException PDFNet exception
     */
    protected abstract Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException;

    /**
     * reset drawing pts
     */
    protected void resetPts() {
        mPt1.set(0, 0);
        mPt2.set(0, 0);
    }

    /**
     * set next tool mode helper
     */
    @SuppressWarnings("WeakerAccess")
    protected void setNextToolModeHelper() {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager.isAutoSelectAnnotation() || !mForceSameNextToolMode) {
            mNextToolMode = getDefaultNextTool();
        } else {
            mNextToolMode = getToolMode();
        }
    }

    /**
     * Gets alternative next tool mode if next tool is not current tool
     * By default, it is ToolMode.ANNOT_EDIT, if the alternative tool of subclass
     * is not ToolMode.ANNOT_EDIT, then the subclass can override this method.
     *
     * @return alternative next tool mode
     */
    protected ToolMode getDefaultNextTool() {
        return ToolMode.ANNOT_EDIT;
    }

    /**
     * Called when creating markup annotaiton failed
     *
     * @param e
     */
    protected void onCreateMarkupFailed(Exception e) {
        // do nothing by default
        Log.e(TAG, "onCreateMarkupFailed", e);
    }
}
