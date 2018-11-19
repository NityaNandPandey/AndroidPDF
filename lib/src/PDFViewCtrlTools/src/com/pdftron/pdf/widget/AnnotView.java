package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.tools.R;

public class AnnotView extends RelativeLayout {

    private View mView;
    private AnnotTextView mTextView;
    private AnnotDrawingView mDrawingView;

    private boolean mDelayViewRemoval;

    public AnnotView(Context context) {
        this(context, null);
    }

    public AnnotView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnnotView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setAnnotStyle(PDFViewCtrl pdfViewCtrl, AnnotStyle annotStyle) {
        mDrawingView.setAnnotStyle(pdfViewCtrl, annotStyle);
        mTextView.setAnnotStyle(pdfViewCtrl, annotStyle);

        if (annotStyle.getAnnotType() == Annot.e_FreeText) {
            mTextView.setVisibility(View.VISIBLE);
            mDrawingView.setVisibility(ViewGroup.GONE);
        } else {
            mTextView.setVisibility(View.GONE);
            mDrawingView.setVisibility(ViewGroup.VISIBLE);
        }
    }

    public void setDelayViewRemoval(boolean delayViewRemoval) {
        mDelayViewRemoval = delayViewRemoval;
        if (delayViewRemoval) {
            mTextView.removeCtrlPts();
            mDrawingView.removeCtrlPts();
        }
    }

    public boolean isDelayViewRemoval() {
        return mDelayViewRemoval;
    }

    public AnnotDrawingView getDrawingView() {
        return mDrawingView;
    }

    public AnnotTextView getTextView() {
        return mTextView;
    }

    public void setZoom(double zoom) {
        mDrawingView.setZoom(zoom);
        mTextView.setZoom(zoom);
    }

    public void setHasPermission(boolean hasPermission) {
        mDrawingView.setHasPermission(hasPermission);
        mTextView.setHasPermission(hasPermission);
    }

    public void setCtrlPts(PointF[] pts) {
        mDrawingView.setCtrlPts(pts);
        mTextView.setCtrlPts(pts);
    }

    public void updateTextColor(int textColor) {
        mTextView.updateTextColor(textColor);
    }

    public void updateTextSize(float textSize) {
        mTextView.updateTextSize(textSize);
    }

    public void updateColor(int color) {
        mDrawingView.updateColor(color);
        mTextView.updateColor(color);
    }

    public void updateFillColor(int color) {
        mDrawingView.updateFillColor(color);
        mTextView.updateFillColor(color);
    }

    public void updateThickness(float thickness) {
        mDrawingView.updateThickness(thickness);
        mTextView.updateThickness(thickness);
    }

    public void updateOpacity(float opacity) {
        mDrawingView.updateOpacity(opacity);
        mTextView.updateOpacity(opacity);
    }

    public void updateIcon(String icon) {
        mDrawingView.updateIcon(icon);
    }

    public void updateFont(FontResource font) {
        mTextView.updateFont(font);
    }

    public void updateRulerItem(RulerItem rulerItem) {
        mDrawingView.updateRulerItem(rulerItem);
    }

    private void init() {
        mView = LayoutInflater.from(getContext()).inflate(R.layout.annot_view_layout, null);
        mTextView = mView.findViewById(R.id.text_view);
        mDrawingView = mView.findViewById(R.id.drawing_view);

        addView(mView);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        mView.layout(0, 0, r - l, b - t);
        mTextView.layout(0, 0, r - l, b - t);
        mDrawingView.layout(0, 0, r - l, b - t);
    }
}
