package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.annots.Ink;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.InkItem;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.tools.FreehandCreate;
import com.pdftron.pdf.tools.RulerCreate;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;

import static com.pdftron.pdf.tools.AnnotEdit.e_lm;
import static com.pdftron.pdf.tools.AnnotEdit.e_ml;

public class AnnotDrawingView extends AppCompatImageView {

    private AnnotViewImpl mAnnoViewImpl;

    private RectF mOval = new RectF();
    private int mPageNum;

    private PointF mPt3 = new PointF(0, 0);
    private PointF mPt4 = new PointF(0, 0);
    private PointF mPt5 = new PointF(0, 0);
    private PointF mPt6 = new PointF(0, 0);

    private Path mOnDrawPath = new Path();

    private String mIcon;

    private ArrayList<PointF> mVertices = new ArrayList<>();
    private PointF[] mCtrlPts;

    private ArrayList<InkItem> mInks = new ArrayList<>();
    private PointF mInkOffset;
    private float mInkWidthScreen;
    private float mInkHeightScreen;
    private float mInkScaleWidthScreen;
    private float mInkScaleHeightScreen;
    private boolean mRecalculate;
    private boolean mInitLayoutSet;

    public AnnotDrawingView(Context context) {
        this(context, null);
    }

    public AnnotDrawingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnnotDrawingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        mAnnoViewImpl = new AnnotViewImpl(context);
    }

    public void setAnnotStyle(PDFViewCtrl pdfViewCtrl, AnnotStyle annotStyle) {
        mAnnoViewImpl.setAnnotStyle(pdfViewCtrl, annotStyle);
        mIcon = annotStyle.getIcon();
        if (!Utils.isNullOrEmpty(mIcon)) {
            setImageDrawable(annotStyle.getIconDrawable(getContext()));
        }
    }

    public void initInkItem(Annot inkAnnot, PointF offset) {
        if (mAnnoViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Ink) {
            try {
                InkItem item = new InkItem(mAnnoViewImpl.mPaint, mAnnoViewImpl.mStrokeColor, mAnnoViewImpl.mOpacity, mAnnoViewImpl.mThickness, false);
                mInks.add(item);
                Ink ink = new Ink(inkAnnot);
                com.pdftron.pdf.Rect rect = inkAnnot.getRect();
                rect.normalize();
                FreehandCreate.setupInkItem(ink, item);
                mInkOffset = offset;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updateColor(int color) {
        mAnnoViewImpl.updateColor(color);
        if (!mInks.isEmpty()) {
            for (InkItem inkItem : mInks) {
                inkItem.mPaint = mAnnoViewImpl.mPaint;
                inkItem.mColor = color;
            }
        }
        invalidate();
    }

    public void updateFillColor(int color) {
        mAnnoViewImpl.updateFillColor(color);
        invalidate();
    }

    public void updateThickness(float thickness) {
        mAnnoViewImpl.updateThickness(thickness);
        if (!mInks.isEmpty()) {
            for (InkItem inkItem : mInks) {
                inkItem.mPaint = mAnnoViewImpl.mPaint;
                inkItem.mThickness = thickness;
            }
        }
        invalidate();
    }

    public void updateOpacity(float opacity) {
        mAnnoViewImpl.updateOpacity(opacity);
        if (!Utils.isNullOrEmpty(mIcon)) {
            updateIcon(mIcon);
        } else {
            if (!mInks.isEmpty()) {
                for (InkItem inkItem : mInks) {
                    inkItem.mPaint = mAnnoViewImpl.mPaint;
                    inkItem.mOpacity = opacity;
                }
            }
            invalidate();
        }
    }

    public void updateRulerItem(RulerItem rulerItem) {
        mAnnoViewImpl.updateRulerItem(rulerItem);
        invalidate();
    }

    public void setZoom(double zoom) {
        mAnnoViewImpl.setZoom(zoom);
    }

    public void setHasPermission(boolean hasPermission) {
        mAnnoViewImpl.setHasPermission(hasPermission);
    }

    public void setCtrlPts(PointF[] pts) {
        mAnnoViewImpl.setCtrlPts(pts);
    }

    public void setPageNum(int pageNum) {
        mPageNum = pageNum;
    }

    public void setVertices(PointF... points) {
        mVertices.clear();
        if (points != null) {
            mVertices.addAll(Arrays.asList(points));
            mCtrlPts = points;
        }
    }

    public ArrayList<PointF> getVertices() {
        return mVertices;
    }

    public PointF[] getCtrlPts() {
        return mCtrlPts;
    }

    public void updateIcon(String icon) {
        mIcon = icon;
        if (!Utils.isNullOrEmpty(mIcon)) {
            setImageDrawable(AnnotStyle.getIconDrawable(getContext(), icon,
                mAnnoViewImpl.mStrokeColor, mAnnoViewImpl.mOpacity));
        }
    }

    public void removeCtrlPts() {
        mAnnoViewImpl.removeCtrlPts();
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mAnnoViewImpl.mPt1.set(left, top);
        mAnnoViewImpl.mPt2.set(right, bottom);

        mRecalculate = false;

        if (right - left > 0 && bottom - top > 0) {
            if (!mInitLayoutSet) {
                mInkWidthScreen = right - left;
                mInkHeightScreen = bottom - top;
                mInkScaleWidthScreen = mInkWidthScreen;
                mInkScaleHeightScreen = mInkHeightScreen;
                mInitLayoutSet = true;
                mRecalculate = true;
            } else if (changed) {
                mInkScaleWidthScreen = right - left;
                mInkScaleHeightScreen = bottom - top;
                mRecalculate = true;
            }
        }
    }

    private void drawCtrlPts(Canvas canvas) {
        if (!mAnnoViewImpl.mCanDrawCtrlPts) {
            return;
        }
        if (mAnnoViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Line ||
            mAnnoViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW ||
            mAnnoViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_RULER ) {
            DrawingUtils.drawCtrlPtsLine(getContext().getResources(), canvas,
                mAnnoViewImpl.mCtrlPtsPaint, mVertices.get(0), mVertices.get(1),
                mAnnoViewImpl.mCtrlRadius, mAnnoViewImpl.mHasSelectionPermission);
        } else if (mAnnoViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Polyline ||
            mAnnoViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Polygon ||
            mAnnoViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD) {
            try {
                DrawingUtils.drawCtrlPtsAdvancedShape(getContext().getResources(), canvas,
                    mAnnoViewImpl.mCtrlPtsPaint, mCtrlPts, mAnnoViewImpl.mCtrlRadius,
                    mAnnoViewImpl.mHasSelectionPermission, false);
            } catch (Exception ignored) {}
        } else {
            try {
                DrawingUtils.drawCtrlPts(getContext().getResources(), canvas,
                    mAnnoViewImpl.mCtrlPtsPaint, mAnnoViewImpl.mPt1, mAnnoViewImpl.mPt2,
                    mAnnoViewImpl.mCtrlPts[e_lm], mAnnoViewImpl.mCtrlPts[e_ml], mAnnoViewImpl.mCtrlRadius,
                    mAnnoViewImpl.mHasSelectionPermission, false);
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() != null) {
            super.onDraw(canvas);
            return;
        }
        if (mAnnoViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Square) {
            DrawingUtils.drawRectangle(canvas,
                mAnnoViewImpl.mPt1, mAnnoViewImpl.mPt2,
                mAnnoViewImpl.mThicknessDraw,
                mAnnoViewImpl.mFillColor, mAnnoViewImpl.mStrokeColor,
                mAnnoViewImpl.mFillPaint, mAnnoViewImpl.mPaint);
        } else if (mAnnoViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Circle) {
            DrawingUtils.drawOval(canvas,
                mAnnoViewImpl.mPt1, mAnnoViewImpl.mPt2,
                mAnnoViewImpl.mThicknessDraw,
                    mOval,
                mAnnoViewImpl.mFillColor, mAnnoViewImpl.mStrokeColor,
                mAnnoViewImpl.mFillPaint, mAnnoViewImpl.mPaint);
        } else if (mAnnoViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Line) {
            DrawingUtils.drawLine(canvas, mVertices.get(0), mVertices.get(1), mAnnoViewImpl.mPaint);
        } else if (mAnnoViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW) {
            DrawingUtils.calcArrow(mVertices.get(0), mVertices.get(1),
                mPt3, mPt4, mAnnoViewImpl.mThickness, mAnnoViewImpl.mZoom);
            DrawingUtils.drawArrow(canvas, mVertices.get(0), mVertices.get(1),
                mPt3, mPt4, mOnDrawPath, mAnnoViewImpl.mPaint);
        } else if (mAnnoViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_RULER) {
            DrawingUtils.calcRuler(mVertices.get(0), mVertices.get(1),
                mPt3, mPt4, mPt5, mPt6, mAnnoViewImpl.mThickness, mAnnoViewImpl.mZoom);

            // calc distance
            double[] pts1, pts2;
            pts1 = mAnnoViewImpl.mPdfViewCtrl.convScreenPtToPagePt(mVertices.get(0).x, mVertices.get(0).y, mPageNum);
            pts2 = mAnnoViewImpl.mPdfViewCtrl.convScreenPtToPagePt(mVertices.get(1).x, mVertices.get(1).y, mPageNum);

            String text = RulerCreate.getRulerLabel(mAnnoViewImpl.mAnnotStyle.getRulerItem(), pts1[0], pts1[1], pts2[0], pts2[1]);

            DrawingUtils.drawRuler(canvas, mVertices.get(0), mVertices.get(1),
                mPt3, mPt4, mPt5, mPt6, mOnDrawPath, mAnnoViewImpl.mPaint,
                text, mAnnoViewImpl.mZoom);
        } else if (mAnnoViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Polyline) {
            DrawingUtils.drawPolyline(mAnnoViewImpl.mPdfViewCtrl, mPageNum,
                canvas, mVertices, mOnDrawPath, mAnnoViewImpl.mPaint, mAnnoViewImpl.mStrokeColor);
        } else if (mAnnoViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Polygon) {
            DrawingUtils.drawPolygon(mAnnoViewImpl.mPdfViewCtrl, mPageNum,
                canvas, mVertices, mOnDrawPath, mAnnoViewImpl.mPaint, mAnnoViewImpl.mStrokeColor,
                mAnnoViewImpl.mFillPaint, mAnnoViewImpl.mFillColor);
        } else if (mAnnoViewImpl.mAnnotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD) {
            DrawingUtils.drawCloud(mAnnoViewImpl.mPdfViewCtrl, mPageNum, canvas,
                mVertices, mOnDrawPath, mAnnoViewImpl.mPaint, mAnnoViewImpl.mStrokeColor,
                mAnnoViewImpl.mFillPaint, mAnnoViewImpl.mFillColor, mAnnoViewImpl.mAnnotStyle.getBorderEffectIntensity());
        } else if (mAnnoViewImpl.mAnnotStyle.getAnnotType() == Annot.e_Ink) {
            DrawingUtils.drawInk(mAnnoViewImpl.mPdfViewCtrl, canvas, mInks, false,
                mInkOffset, mInkScaleWidthScreen / mInkWidthScreen, mInkScaleHeightScreen / mInkHeightScreen, mRecalculate);
        }

        drawCtrlPts(canvas);
    }
}
