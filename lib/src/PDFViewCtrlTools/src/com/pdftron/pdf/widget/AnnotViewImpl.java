package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.utils.Utils;

public class AnnotViewImpl {

    public AnnotStyle mAnnotStyle;

    public PDFViewCtrl mPdfViewCtrl;

    public PointF mPt1 = new PointF(0, 0);
    public PointF mPt2 = new PointF(0, 0);

    public Paint mPaint;
    public Paint mFillPaint;
    public Paint mCtrlPtsPaint;
    public float mThickness;
    public float mThicknessReserve;
    public float mThicknessDraw;
    public int mStrokeColor;
    public int mFillColor;
    public float mOpacity;
    public double mZoom = 1.0;
    public float mCtrlRadius;
    public boolean mHasSelectionPermission = true;
    public PointF[] mCtrlPts;

    public boolean mCanDrawCtrlPts = true;

    public AnnotViewImpl(Context context) {
        init(context);
    }

    public AnnotViewImpl(PDFViewCtrl pdfViewCtrl, AnnotStyle annotStyle) {
        init(pdfViewCtrl.getContext());

        setAnnotStyle(pdfViewCtrl, annotStyle);
    }

    public void init(Context context) {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.MITER);
        mPaint.setStrokeCap(Paint.Cap.BUTT);
        mFillPaint = new Paint(mPaint);
        mFillPaint.setStyle(Paint.Style.FILL);
        mFillPaint.setColor(Color.TRANSPARENT);
        mCtrlPtsPaint = new Paint(mPaint);
        mThicknessDraw = 1.0f;
        mOpacity = 1.0f;
        mCtrlRadius = Utils.convDp2Pix(context,7.5f);
    }

    public void setAnnotStyle(PDFViewCtrl pdfViewCtrl, AnnotStyle annotStyle) {
        mPdfViewCtrl = pdfViewCtrl;
        mAnnotStyle = annotStyle;

        mStrokeColor = annotStyle.getColor();
        mFillColor = annotStyle.getFillColor();
        mThickness = mThicknessReserve = annotStyle.getThickness();
        mOpacity = annotStyle.getOpacity();

        mPaint.setColor(Utils.getPostProcessedColor(mPdfViewCtrl, mStrokeColor));
        mFillPaint.setColor(Utils.getPostProcessedColor(mPdfViewCtrl, mFillColor));

        mPaint.setAlpha((int) (255 * mOpacity));
        mFillPaint.setAlpha((int) (255 * mOpacity));

        updateColor(mStrokeColor);
    }

    public void updateColor(int color) {
        mStrokeColor = color;
        mPaint.setColor(Utils.getPostProcessedColor(mPdfViewCtrl, mStrokeColor));
        updateOpacity(mOpacity);

        updateThickness(mThicknessReserve);
    }

    public void updateFillColor(int color) {
        mFillColor = color;
        mFillPaint.setColor(Utils.getPostProcessedColor(mPdfViewCtrl, mFillColor));
        updateOpacity(mOpacity);
    }

    public void updateThickness(float thickness) {
        mThickness = mThicknessReserve = thickness;
        if (mStrokeColor == Color.TRANSPARENT) {
            mThickness = 1.0f;
        } else {
            mThickness = thickness;
        }
        mThicknessDraw = (float) mZoom * mThickness;
        mPaint.setStrokeWidth(mThicknessDraw);
    }

    public void updateOpacity(float opacity) {
        mOpacity = opacity;
        mPaint.setAlpha((int) (255 * mOpacity));
        mFillPaint.setAlpha((int) (255 * mOpacity));
    }

    public void updateRulerItem(RulerItem rulerItem) {
        mAnnotStyle.setRulerItem(rulerItem);
    }

    public void setZoom(double zoom) {
        mZoom = zoom;
        mThicknessDraw = (float) mZoom * mThickness;
        mPaint.setStrokeWidth(mThicknessDraw);
    }

    public void setHasPermission(boolean hasPermission) {
        mHasSelectionPermission = hasPermission;
    }

    public void setCtrlPts(PointF[] pts) {
        mCtrlPts = pts;
    }

    public void removeCtrlPts() {
        mCanDrawCtrlPts = false;
    }
}
