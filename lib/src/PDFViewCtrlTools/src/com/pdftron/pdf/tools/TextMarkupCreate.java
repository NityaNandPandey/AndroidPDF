//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.QuadPoint;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.annots.Popup;
import com.pdftron.pdf.annots.TextMarkup;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.PathPool;
import com.pdftron.pdf.utils.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * This class is the base class for all text markup creation tools.
 */
@Keep
abstract public class TextMarkupCreate extends Tool {

    int mLoupeWidth;
    int mLoupeHeight;
    int mLoupeMargin;
    float mLoupeThickness;
    float mLoupeShadowFactor;
    float mLoupeArrowHeight;
    boolean mDrawingLoupe;
    RectF mLoupeBBox;
    Path mLoupePath;
    Path mLoupeShadowPath;
    Canvas mCanvas;
    Bitmap mBitmap;
    RectF mSrcRectF;
    RectF mDesRectF;
    Matrix mMatrix;

    boolean mDrawLoupe;

    PointF mPressedPoint;
    Rect mInvalidateBBox;

    float mOpacity;
    int mColor;
    float mThickness;

    Paint mPaint;

    Path mSelPath;
    RectF mTempRect;
    RectF mSelBBox;
    PointF mStationPt;

    boolean mOnUpCalled;

    protected boolean mIsPointOutsidePage;

    /**
     * Class constructor
     */
    public TextMarkupCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mLoupeWidth = (int) this.convDp2Pix(150);
        mLoupeMargin = (int) this.convDp2Pix(5);

        mSelPath = new Path();
        mTempRect = new RectF();
        mSelBBox = new RectF();
        mStationPt = new PointF();

        mOnUpCalled = false;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);

        mDrawLoupe = false;
        mDrawingLoupe = false;

        mSrcRectF = new RectF();
        mDesRectF = new RectF();
        mMatrix = new Matrix();
        mLoupeBBox = new RectF();
        mPressedPoint = new PointF();
        mInvalidateBBox = new Rect();

        mLoupeHeight = mLoupeWidth / 2;
        mLoupeArrowHeight = (float) mLoupeHeight / 4;
        mLoupeThickness = (float) mLoupeMargin / 4;
        mLoupeShadowFactor = mLoupeThickness * 4;
        mBitmap = Bitmap.createBitmap(mLoupeWidth - mLoupeMargin * 2, mLoupeHeight - mLoupeMargin * 2, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas();
        mCanvas.setBitmap(mBitmap);

        //create the loupe stroke path
        mLoupePath = new Path();
        mLoupeShadowPath = new Path();
        float co = mLoupeMargin;

        //outer loop with arrow
        mLoupePath.moveTo(0, -co - mLoupeArrowHeight); //lower-left after round corner
        mLoupePath.rLineTo(0, -(mLoupeHeight - 2 * co));
        mLoupePath.rQuadTo(0, -co, co, -co);
        mLoupePath.rLineTo(mLoupeWidth - 2 * co, 0);
        mLoupePath.rQuadTo(co, 0, co, co);
        mLoupePath.rLineTo(0, mLoupeHeight - 2 * co);
        mLoupePath.rQuadTo(0, co, -co, co);
        mLoupePath.rLineTo(-(mLoupeWidth - 2 * co - mLoupeArrowHeight) / 2, 0);
        mLoupePath.rLineTo(-mLoupeArrowHeight / 2, mLoupeArrowHeight / 2);
        mLoupePath.rLineTo(-mLoupeArrowHeight / 2, -mLoupeArrowHeight / 2);
        mLoupePath.rLineTo(-(mLoupeWidth - 2 * co - mLoupeArrowHeight) / 2, 0);
        mLoupePath.rQuadTo(-co, 0, -co, -co);
        mLoupePath.close();

        mLoupeShadowPath.set(mLoupePath);

        //inner loop
        mLoupePath.moveTo(mLoupeMargin, -mLoupeMargin - co - mLoupeArrowHeight);
        mLoupePath.rLineTo(0, -(mLoupeHeight - 2 * co - 2 * mLoupeMargin));
        mLoupePath.rQuadTo(0, -co, co, -co);
        mLoupePath.rLineTo(mLoupeWidth - 2 * co - 2 * mLoupeMargin, 0);
        mLoupePath.rQuadTo(co, 0, co, co);
        mLoupePath.rLineTo(0, mLoupeHeight - 2 * co - 2 * mLoupeMargin);
        mLoupePath.rQuadTo(0, co, -co, co);
        mLoupePath.rLineTo(-mLoupeWidth + 2 * co + 2 * mLoupeMargin, 0);
        mLoupePath.rQuadTo(-co, 0, -co, -co);
        mLoupePath.close();

        mLoupePath.setFillType(Path.FillType.EVEN_ODD);
        mNextToolMode = getToolMode();
    }

    /**
     * Returns annotation index for adding markup.
     *
     * @param page The page
     * @return The annotation index
     */
    protected static int getAnnotIndexForAddingMarkup(Page page) {
        int index = 0;
        boolean foundMarkupAnnot = false;

        try {
            for (index = page.getNumAnnots() - 1; index > 0; --index) {
                int type = page.getAnnot(index).getType();
                if (type == com.pdftron.pdf.Annot.e_Highlight ||
                    type == com.pdftron.pdf.Annot.e_Underline ||
                    type == com.pdftron.pdf.Annot.e_Squiggly ||
                    type == com.pdftron.pdf.Annot.e_StrikeOut ||
                    type == com.pdftron.pdf.Annot.e_Link ||
                    type == com.pdftron.pdf.Annot.e_Widget) {
                    foundMarkupAnnot = true;
                    break;
                }
            }
        } catch (PDFNetException ex) {

        }

        if (foundMarkupAnnot) {
            ++index;
        }
        return index;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Unknown;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    abstract public ToolManager.ToolModeBase getToolMode();

    /**
     * The overload implementation of {@link Tool#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return true;
    }

    /**
     * The overload implementation of {@link Tool#setupAnnotProperty(int, float, float, int, String, String)}.
     */
    @Override
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName) {
        mColor = color;
        mOpacity = opacity;
        mThickness = thickness;

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt(getColorKey(getCreateAnnotType()), mColor);
        editor.putFloat(getOpacityKey(getCreateAnnotType()), mOpacity);
        editor.putFloat(getThicknessKey(getCreateAnnotType()), mThickness);

        editor.apply();
    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (mForceSameNextToolMode) {
            // If annotation was already pushed back, avoid re-entry due to single tap
            if (mAnnotPushedBack) {
                return true;
            }

            int x = (int) (e.getX() + 0.5);
            int y = (int) (e.getY() + 0.5);

            Annot tempAnnot = mPdfViewCtrl.getAnnotationAt(x, y);
            int page = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);

            setCurrentDefaultToolModeHelper(getToolMode());

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
        mOnUpCalled = true;

        mColor = 0xFFFF00;
        mOpacity = 1.0f;
        mThickness = 1.0f;

        mAnnotPushedBack = false;

        float x = e.getX() + mPdfViewCtrl.getScrollX();
        float y = e.getY() + mPdfViewCtrl.getScrollY();
        mPressedPoint.x = x;
        mPressedPoint.y = y;

        mStationPt.set(x, y);

        //show loupe
        setLoupeInfo(e.getX(), e.getY());
        mInvalidateBBox.left = (int) mLoupeBBox.left - (int) Math.ceil(mLoupeShadowFactor) - 1;
        mInvalidateBBox.top = (int) mLoupeBBox.top - 1;
        mInvalidateBBox.right = (int) Math.ceil(mLoupeBBox.right) + (int) Math.ceil(mLoupeShadowFactor) + 1;
        mInvalidateBBox.bottom = (int) Math.ceil(mLoupeBBox.bottom) + (int) Math.ceil(1.5f * mLoupeShadowFactor) + 1;
        mPdfViewCtrl.invalidate(mInvalidateBBox);

        int page = mPdfViewCtrl.getPageNumberFromScreenPt(e.getX(), e.getY());
        if (page < 1) {
            mIsPointOutsidePage = true;
        } else {
            mIsPointOutsidePage = false;
        }

        return false;
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
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        // ignore if from two finger scrolling
        if (mAllowTwoFingerScroll) {
            doneTwoFingerScrolling();
            return false;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PAGE_SLIDING) {
            return false;
        }

        // If annotation was already pushed back, avoid re-entry due to fling motion.
        if (mAnnotPushedBack && mForceSameNextToolMode) {
            return true;
        }

        // In stylus mode, ignore finger input
        mAllowOneFingerScrollWithStylus = mStylusUsed && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS;
        if (mAllowOneFingerScrollWithStylus) {
            return true;
        }

        if (mOnUpCalled) {

            mOnUpCalled = false;

            if (!mPdfViewCtrl.hasSelection()) {
                try {
                    if (!((ToolManager) mPdfViewCtrl.getToolManager()).isQuickMenuJustClosed()) {
                        // if no selection yet, let's see if it is OK tap to markup
                        int x = (int) (e.getX() + 0.5);
                        int y = (int) (e.getY() + 0.5);
                        if (!hasAnnot(x, y)) {
                            float sx = mPdfViewCtrl.getScrollX();
                            float sy = mPdfViewCtrl.getScrollY();
                            mPressedPoint.x = e.getX() + sx;
                            mPressedPoint.y = e.getY() + sy;

                            selectText(mStationPt.x - sx, mStationPt.y - sy, e.getX(), e.getY(), true);
                            mPdfViewCtrl.invalidate(mInvalidateBBox);
                        }
                    }
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                }
            }

            mPdfViewCtrl.invalidate(); //always needed to draw away the previous loupe even if there is not any selection.
            createTextMarkup();
        }

        return skipOnUpPriorEvent(priorEventMode);
    }

    protected void createTextMarkup() {
        if (!mPdfViewCtrl.hasSelection()) {
            return;
        }
        int sel_pg_begin = mPdfViewCtrl.getSelectionBeginPage();
        int sel_pg_end = mPdfViewCtrl.getSelectionEndPage();

        class AnnotUpdateInfo {
            Annot mAnnot;
            int mPageNum;
            com.pdftron.pdf.Rect mRect;

            public AnnotUpdateInfo(Annot annot, int pageNum, com.pdftron.pdf.Rect rect) {
                mAnnot = annot;
                mPageNum = pageNum;
                mRect = rect;
            }
        }

        LinkedList<AnnotUpdateInfo> updateInfoList = new LinkedList<>();

        boolean shouldUnlock = false;
        try {
            //setNextToolModeHelper(ToolMode.ANNOT_EDIT_TEXT_MARKUP);
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (toolManager.isAutoSelectAnnotation() || !mForceSameNextToolMode) {
                mNextToolMode = ToolMode.ANNOT_EDIT_TEXT_MARKUP;
            } else {
                mNextToolMode = getToolMode();
            }

            setCurrentDefaultToolModeHelper(getToolMode());
            // add UI to drawing list
            addOldTools();

            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            PDFDoc doc = mPdfViewCtrl.getDoc();
            for (int pg = sel_pg_begin; pg <= sel_pg_end; ++pg) {
                PDFViewCtrl.Selection sel = mPdfViewCtrl.getSelection(pg);
                double[] quads = sel.getQuads();
                int sz = quads.length / 8;
                if (sz == 0) {
                    continue;
                }

                Point p1 = new Point();
                Point p2 = new Point();
                Point p3 = new Point();
                Point p4 = new Point();
                QuadPoint qp = new QuadPoint(p1, p2, p3, p4);

                com.pdftron.pdf.Rect bbox = new com.pdftron.pdf.Rect(quads[0], quads[1], quads[4], quads[5]); //just use the first quad to temporarily populate the bbox
                Annot tm = createMarkup(doc, bbox);

                Context context = mPdfViewCtrl.getContext();
                SharedPreferences settings = Tool.getToolPreferences(context);
                mColor = settings.getInt(getColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultColor(context, getCreateAnnotType()));
                mOpacity = settings.getFloat(getOpacityKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultOpacity(context, getCreateAnnotType()));
                mThickness = settings.getFloat(getThicknessKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultThickness(context, getCreateAnnotType()));

                boolean useAdobeHack = toolManager.isTextMarkupAdobeHack();

                int k = 0;
                // left, right, top, bottom
                double left = 0;
                double right = 0;
                double top = 0;
                double bottom = 0;
                for (int i = 0; i < sz; ++i, k += 8) {
                    p1.x = quads[k];
                    p1.y = quads[k + 1];

                    p2.x = quads[k + 2];
                    p2.y = quads[k + 3];

                    p3.x = quads[k + 4];
                    p3.y = quads[k + 5];

                    p4.x = quads[k + 6];
                    p4.y = quads[k + 7];

                    if (useAdobeHack) {
                        qp.p1 = p4;
                        qp.p2 = p3;
                        qp.p3 = p1;
                        qp.p4 = p2;
                    } else {
                        qp.p1 = p1;
                        qp.p2 = p2;
                        qp.p3 = p3;
                        qp.p4 = p4;
                    }
                    if (tm != null && tm instanceof TextMarkup) {
                        ((TextMarkup) tm).setQuadPoint(i, qp);
                    } else {
                        // find rect for non TextMarkup annotation
                        if (0 == left) {
                            left = p1.x;
                        } else {
                            left = Math.min(left, p1.x);
                        }
                        right = Math.max(right, p2.x);
                        if (0 == top) {
                            top = p1.y;
                        } else {
                            top = Math.min(top, p1.y);
                        }
                        bottom = Math.max(bottom, p3.y);
                        setAnnotRect(tm, new com.pdftron.pdf.Rect(left, top, right, bottom), pg);
                    }
                }

                if (tm != null) {
                    ColorPt colorPt = Utils.color2ColorPt(mColor);
                    tm.setColor(colorPt, 3);
                    // current tm is Markup
                    if (tm instanceof Markup) {
                        ((Markup) tm).setOpacity(mOpacity);
                        setAuthor((Markup) tm);
                        if (((ToolManager) mPdfViewCtrl.getToolManager()).isCopyAnnotatedTextToNoteEnabled()) {
                            // create note from selected text
                            try {
                                Popup p = Popup.create(mPdfViewCtrl.getDoc(), tm.getRect());
                                p.setParent(tm);
                                ((Markup) tm).setPopup(p);
                                p.setContents(sel.getAsUnicode());
                                Utils.setTextCopy(tm);
                            } catch (PDFNetException ex) {
                                AnalyticsHandlerAdapter.getInstance().sendException(ex);
                            }
                        }
                    }

                    if (tm.getType() != com.pdftron.pdf.Annot.e_Highlight) {
                        com.pdftron.pdf.Annot.BorderStyle bs = tm.getBorderStyle();
                        bs.setWidth(mThickness);
                        tm.setBorderStyle(bs);
                    }


                    Page page = mPdfViewCtrl.getDoc().getPage(pg);
                    int index = getAnnotIndexForAddingMarkup(page);
                    page.annotInsert(index, tm);
                    tm.refreshAppearance();

                    mAnnotPushedBack = true;
                    setAnnot(tm, pg);
                    buildAnnotBBox();


                    //compute the bbox of the annotation in screen space
                    com.pdftron.pdf.Rect ur = AnnotUtils.computeAnnotInbox(mPdfViewCtrl, tm, pg);
                    updateInfoList.add(new AnnotUpdateInfo(tm, pg, ur));
                }
            }


            //clear existing selections
            if (!mSelPath.isEmpty()) {
                mSelPath.reset();
            }
            mPdfViewCtrl.clearSelection();

            // make sure to raise add event after mPdfViewCtrl.update
            HashMap<Annot, Integer> annots = new HashMap<>();
            Iterator<AnnotUpdateInfo> itr = updateInfoList.iterator();
            while (itr.hasNext()) {
                AnnotUpdateInfo updateInfo = itr.next();
                com.pdftron.pdf.Rect rect = updateInfo.mRect;
                mPdfViewCtrl.update(rect);
                Annot annot = updateInfo.mAnnot;
                int pageNum = updateInfo.mPageNum;
                if (annot != null) {
                    annots.put(annot, pageNum);
                }
            }
            raiseAnnotationAddedEvent(annots);

            mPdfViewCtrl.invalidate(); //always needed to draw away the previous loupe even if there is not any selection.

            //after highlighting, register a custom callback, in which will
            //switch to pan tool.
            //mPdfViewCtrl.postToolOnCustomEvent(null);
        } catch (Exception ex) {
            ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(ex.getMessage());
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

    }

    /**
     * Sets rectangle to annotation
     *
     * @param annot   The annotation
     * @param rect    The rectangle area to set to annotation
     * @param pageNum The page number of annotation
     */
    protected void setAnnotRect(@Nullable Annot annot, com.pdftron.pdf.Rect rect, int pageNum) throws PDFNetException {
        if (annot == null) {
            return;
        }
        annot.setRect(rect);
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

        mDrawLoupe = true;

        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();
        mPressedPoint.x = e2.getX() + sx;
        mPressedPoint.y = e2.getY() + sy;

        selectText(mStationPt.x - sx, mStationPt.y - sy, e2.getX(), e2.getY(), false);
        mPdfViewCtrl.invalidate(mInvalidateBBox);
        return true;
    }

    /**
     * The overload implementation of {@link Tool#onDraw(Canvas, Matrix)}.
     */
    // It is OK to suppress lint warning (isHardwareAccelerated) since
    // PDFViewCtrl deals with it internally.
    @SuppressLint("NewApi")
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        if (mAllowTwoFingerScroll) {
            return;
        }

        if (mIsPointOutsidePage) {
            return;
        }

        if (mAllowOneFingerScrollWithStylus) {
            return;
        }

        if (!mDrawingLoupe) {
            super.onDraw(canvas, tfm);
        }

        if (mOnUpCalled) {

            //check if need to draw loupe
            boolean draw_loupe = false;

            if (!mDrawingLoupe) // prevent recursive calling
            {
                // show loupe either when being long pressed or a selection widget
                // is effective
                mDrawingLoupe = true;

                // make the drawn portion half the size of the loupe so as to
                // achieve magnifying effect
                float left = mPressedPoint.x - mLoupeBBox.width() / 6;
                float top = mPressedPoint.y - mLoupeBBox.height() / 6;
                float right = left + mLoupeBBox.width() / 3;
                float bottom = top + mLoupeBBox.height() / 3;

                mSrcRectF.set(left, top, right, bottom);
                mDesRectF.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
                mMatrix.setRectToRect(mSrcRectF, mDesRectF, Matrix.ScaleToFit.CENTER);

                mCanvas.save();
                mCanvas.setMatrix(mMatrix);
                mPdfViewCtrl.draw(mCanvas);
                mCanvas.restore();

                mDrawingLoupe = false;
                draw_loupe = true;
            }

            if (!mDrawLoupe) {
                draw_loupe = false;
            }

            //draw the selection
            if (!mSelPath.isEmpty()) {
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(Color.rgb(0, 100, 175));
                mPaint.setAlpha(127);
                canvas.drawPath(mSelPath, mPaint);
            }

            //draw loupe content
            if (draw_loupe) {
                //shadow
                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setColor(Color.BLACK);
                mPaint.setStrokeWidth(0);
                mLoupeShadowPath.offset(mLoupeBBox.left, mLoupeBBox.bottom);
                mPaint.setShadowLayer(mLoupeShadowFactor - 1, 0.0F, mLoupeShadowFactor / 2, 0x96000000);
                boolean ha = mPdfViewCtrl.isHardwareAccelerated();
                if (ha) {
                    Path p = PathPool.getInstance().obtain();
                    p.addPath(mLoupeShadowPath);
                    canvas.drawPath(p, mPaint);
                    PathPool.getInstance().recycle(p);
                } else {
                    canvas.drawPath(mLoupeShadowPath, mPaint);
                }
                mPaint.clearShadowLayer();
                mLoupeShadowPath.offset(-mLoupeBBox.left, -mLoupeBBox.bottom);

                //magnified bitmap
                canvas.drawBitmap(mBitmap, mLoupeBBox.left + mLoupeMargin, mLoupeBBox.top + mLoupeMargin, null);

                //outer and inner boundaries
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(Color.WHITE);
                mLoupePath.offset(mLoupeBBox.left, mLoupeBBox.bottom);
                if (ha) {
                    Path p = PathPool.getInstance().obtain();
                    p.addPath(mLoupePath);
                    p.setFillType(Path.FillType.EVEN_ODD);
                    canvas.drawPath(p, mPaint);
                    PathPool.getInstance().recycle(p);
                } else {
                    canvas.drawPath(mLoupePath, mPaint);
                }

                mPaint.setStyle(Paint.Style.STROKE);
                mPaint.setColor(Color.BLACK);
                mPaint.setStrokeWidth(mLoupeThickness);
                if (ha) {
                    Path p = PathPool.getInstance().obtain();
                    p.addPath(mLoupePath);
                    p.setFillType(Path.FillType.EVEN_ODD);
                    canvas.drawPath(p, mPaint);
                    PathPool.getInstance().recycle(p);
                } else {
                    canvas.drawPath(mLoupePath, mPaint);
                }
                mLoupePath.offset(-mLoupeBBox.left, -mLoupeBBox.bottom);
            }
        }
    }

    private void setLoupeInfo(float touch_x, float touch_y) {
        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();

        // note that the bbox of the loupe has to take into account the
        // following factors:
        // 1. loupe arrow
        // 2. boundary line thickness
        float left = touch_x + sx - (float) mLoupeWidth / 2 - mLoupeThickness / 2;
        float right = left + mLoupeWidth + mLoupeThickness;
        float top = touch_y + sy - mLoupeHeight * 1.45f - mLoupeArrowHeight - mLoupeThickness / 2;
        float bottom = top + mLoupeHeight + mLoupeArrowHeight + mLoupeThickness;

        mLoupeBBox.set(left, top, right, bottom);
    }

    /**
     * Selects text in the specified rectangle.
     *
     * @param x1     The x coordinate at one of the end point of the rectangle
     * @param y1     The y coordinate at one of the end point of the rectangle
     * @param x2     The x coordinate at another point
     * @param y2     The y coordinate at another point
     * @param byRect True if should select by rectangle;
     *               false if should select by struct with smart snapping
     * @return True if some text was selected
     */
    protected boolean selectText(float x1, float y1, float x2, float y2, boolean byRect) {
        if (byRect) {
            float delta = 0.01f;
            x2 += delta;
            y2 += delta;
            delta *= 2;
            x1 = x2 - delta >= 0 ? x2 - delta : 0;
            y1 = y2 - delta >= 0 ? y2 - delta : 0;
        }
        boolean result = false;
        //clear pre-selected content

        boolean had_sel = !mSelPath.isEmpty();
        mSelPath.reset();

        //select text
        boolean shouldUnlock = false;
        try {
            //locks the document first as accessing annotation/doc information isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            if (byRect) {
                result = mPdfViewCtrl.selectByRect(x1, y1, x2, y2);
            } else {
                result = mPdfViewCtrl.selectByStructWithSmartSnapping(x1, y1, x2, y2);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        //update the bounding box that should include:
        //(1)previous selection bbox and loupe bbox
        //(2)current selection bbox and loupe bbox
        if (had_sel) {
            mTempRect.set(mSelBBox);
        }
        populateSelectionResult();
        if (!had_sel) {
            mTempRect.set(mSelBBox);
        } else {
            mTempRect.union(mSelBBox);
        }

        mTempRect.union(mLoupeBBox);
        setLoupeInfo(x2, y2);
        mTempRect.union(mLoupeBBox);

        mInvalidateBBox.left = (int) mTempRect.left - (int) Math.ceil(mLoupeShadowFactor) - 1;
        mInvalidateBBox.top = (int) mTempRect.top - 1;
        mInvalidateBBox.right = (int) Math.ceil(mTempRect.right) + (int) Math.ceil(mLoupeShadowFactor) + 1;
        mInvalidateBBox.bottom = (int) Math.ceil(mTempRect.bottom) + (int) Math.ceil(1.5f * mLoupeShadowFactor) + 1;
        return result;
    }

    private void populateSelectionResult() {
        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();
        int sel_pg_begin = mPdfViewCtrl.getSelectionBeginPage();
        int sel_pg_end = mPdfViewCtrl.getSelectionEndPage();
        float min_x = 1E10f, min_y = 1E10f, max_x = 0, max_y = 0;
        boolean has_sel = false;

        // loop through the pages that have text selection, and construct 'mSelPath' for highlighting.
        // NOTE: android has a bug that if hardware acceleration is turned on and the path is too big,
        // it may not get rendered. See http://code.google.com/p/android/issues/detail?id=24023
        for (int pg = sel_pg_begin; pg <= sel_pg_end; ++pg) {
            PDFViewCtrl.Selection sel = mPdfViewCtrl.getSelection(pg); // each Selection may have multiple quads
            double[] quads = sel.getQuads();
            double[] pts;
            int sz = quads.length / 8; // each quad has eight numbers (x0, y0), ... (x3, y3)

            if (sz == 0) {
                continue;
            }
            int k = 0;
            float x, y;
            for (int i = 0; i < sz; ++i, k += 8) {
                has_sel = true;

                pts = mPdfViewCtrl.convPagePtToScreenPt(quads[k], quads[k + 1], pg);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
                mSelPath.moveTo(x, y);
                min_x = min_x > x ? x : min_x;
                max_x = max_x < x ? x : max_x;
                min_y = min_y > y ? y : min_y;
                max_y = max_y < y ? y : max_y;

                if (pg == sel_pg_begin && i == 0) {
                    // set the start point of the first selection widget that is based
                    // on the first quad point.
                    // mSelWidgets[0].mStrPt.set(x-mTSWidgetThickness/2, y);
                    // x -= mTSWidgetThickness + mTSWidgetRadius;
                    min_x = min_x > x ? x : min_x;
                    max_x = max_x < x ? x : max_x;
                }

                pts = mPdfViewCtrl.convPagePtToScreenPt(quads[k + 2], quads[k + 3], pg);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
                mSelPath.lineTo(x, y);
                min_x = min_x > x ? x : min_x;
                max_x = max_x < x ? x : max_x;
                min_y = min_y > y ? y : min_y;
                max_y = max_y < y ? y : max_y;

                if (pg == sel_pg_end && i == sz - 1) {
                    // set the end point of the second selection widget that is based
                    // on the last quad point.
                    // mSelWidgets[1].mEndPt.set(x+mTSWidgetThickness/2, y);
                    // x += mTSWidgetThickness + mTSWidgetRadius;
                    // y += mTSWidgetRadius * 2;
                    min_x = min_x > x ? x : min_x;
                    max_x = max_x < x ? x : max_x;
                    min_y = min_y > y ? y : min_y;
                    max_y = max_y < y ? y : max_y;
                }

                pts = mPdfViewCtrl.convPagePtToScreenPt(quads[k + 4], quads[k + 5], pg);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
                mSelPath.lineTo(x, y);
                min_x = min_x > x ? x : min_x;
                max_x = max_x < x ? x : max_x;
                min_y = min_y > y ? y : min_y;
                max_y = max_y < y ? y : max_y;

                if (pg == sel_pg_end && i == sz - 1) {
                    // set the start point of the second selection widget that is based
                    // on the last quad point.
                    // mSelWidgets[1].mStrPt.set(x+mTSWidgetThickness/2, y);
                    // x += mTSWidgetThickness + mTSWidgetRadius;
                    min_x = min_x > x ? x : min_x;
                    max_x = max_x < x ? x : max_x;
                }

                pts = mPdfViewCtrl.convPagePtToScreenPt(quads[k + 6], quads[k + 7], pg);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
                mSelPath.lineTo(x, y);
                min_x = min_x > x ? x : min_x;
                max_x = max_x < x ? x : max_x;
                min_y = min_y > y ? y : min_y;
                max_y = max_y < y ? y : max_y;

                if (pg == sel_pg_begin && i == 0) {
                    // set the end point of the first selection widget that is based
                    // on the first quad point.
                    // mSelWidgets[0].mEndPt.set(x-mTSWidgetThickness/2, y);
                    // x -= mTSWidgetThickness + mTSWidgetRadius;
                    // y -= mTSWidgetRadius * 2;
                    min_x = min_x > x ? x : min_x;
                    max_x = max_x < x ? x : max_x;
                    min_y = min_y > y ? y : min_y;
                    max_y = max_y < y ? y : max_y;
                }

                mSelPath.close();
            }
        }

        if (has_sel) {
            mSelBBox.set(min_x, min_y, max_x, max_y);
        }
    }

    /**
     * The overload implementation of {@link Tool#doneTwoFingerScrolling()}.
     */
    @Override
    protected void doneTwoFingerScrolling() {
        super.doneTwoFingerScrolling();

        mDrawLoupe = false;
        // We are up from scrolling
        if (mPdfViewCtrl.hasSelection()) {
            //clear existing selections
            if (!mSelPath.isEmpty()) {
                mSelPath.reset();
            }
            mPdfViewCtrl.clearSelection();
        }
        mPdfViewCtrl.invalidate(); //always needed to draw away the previous loupe even if there is not any selection.
    }

    private boolean hasAnnot(int x, int y) {
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            Annot annot = mPdfViewCtrl.getAnnotationAt(x, y);
            if (annot != null && annot.isValid()) {
                return true;
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        return false;
    }

    /**
     * Creates a text markup.
     *
     * @param doc  The PDF doc
     * @param bbox The bounding box to create a markup
     * @return The created text markup
     * @throws PDFNetException
     */
    protected abstract Annot createMarkup(PDFDoc doc, com.pdftron.pdf.Rect bbox) throws PDFNetException;

    /**
     * When quick menu clicked, creates text markup annotation
     *
     * @param menuItem The clicked menu item.
     * @return true if handled, else otherwise
     */
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        safeSetNextToolMode(getCurrentDefaultToolMode());
        Bundle bundle = new Bundle();
        bundle.putStringArray(KEYS, new String[]{"menuItemId"});
        bundle.putInt("menuItemId", menuItem.getItemId());
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return true;
        }
        // create text markup annotation
        createTextMarkup();

        return true;
    }
}
