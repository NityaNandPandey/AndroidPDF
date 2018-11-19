package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.asynctask.LoadFontAsyncTask;
import com.pdftron.pdf.config.ToolConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

import static com.pdftron.pdf.tools.AnnotEdit.e_lm;
import static com.pdftron.pdf.tools.AnnotEdit.e_ml;

public class AnnotTextView extends AppCompatTextView {

    private AnnotViewImpl mAnnoViewImpl;

    private float mTextSize = 12;
    private int mTextColor;

    public AnnotTextView(Context context) {
        this(context, null);
    }

    public AnnotTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnnotTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        mAnnoViewImpl = new AnnotViewImpl(context);
    }

    public void setAnnotStyle(PDFViewCtrl pdfViewCtrl, AnnotStyle annotStyle) {
        mAnnoViewImpl.setAnnotStyle(pdfViewCtrl, annotStyle);

        mTextSize = annotStyle.getTextSize();
        mTextColor = annotStyle.getTextColor();
        updateTextColor(mTextColor);

        setText(annotStyle.getTextContent());

        loadFont();
    }

    private void loadFont() {
        ArrayList<FontResource> fonts = ToolConfig.getInstance().getFontList();
        if (null == fonts || fonts.size() == 0) {
            ToolManager toolManager = (ToolManager) mAnnoViewImpl.mPdfViewCtrl.getToolManager();
            LoadFontAsyncTask fontAsyncTask = new LoadFontAsyncTask(getContext(), toolManager.getFreeTextFonts());
            fontAsyncTask.setCallback(new LoadFontAsyncTask.Callback() {
                @Override
                public void onFinish(ArrayList<FontResource> fonts) {
                    FontResource font = getMatchingFont(fonts);
                    updateFont(font);
                }
            });
            fontAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            FontResource font = getMatchingFont(fonts);
            updateFont(font);
        }
    }

    private FontResource getMatchingFont(ArrayList<FontResource> fonts) {
        AnnotStyle annotStyle = mAnnoViewImpl.mAnnotStyle;
        for (FontResource font : fonts) {
            if (font.equals(annotStyle.getFont())) {
                annotStyle.getFont().setFilePath(font.getFilePath());
            }
        }
        return annotStyle.getFont();
    }

    public void setZoom(double zoom) {
        mAnnoViewImpl.setZoom(zoom);

        updatePadding();

        float font_sz = mTextSize * (float) mAnnoViewImpl.mZoom;
        setTextSize(TypedValue.COMPLEX_UNIT_PX, font_sz);
    }

    public void setHasPermission(boolean hasPermission) {
        mAnnoViewImpl.setHasPermission(hasPermission);
    }

    public void setCtrlPts(PointF[] pts) {
        mAnnoViewImpl.setCtrlPts(pts);
    }

    public void updateTextColor(int textColor) {
        mTextColor = textColor;
        int color = Utils.getPostProcessedColor(mAnnoViewImpl.mPdfViewCtrl, mTextColor);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int opacity = (int) (mAnnoViewImpl.mOpacity * 255);
        color = Color.argb(opacity, r, g, b);
        setTextColor(color);
    }

    public void updateTextSize(float textSize) {
        mTextSize = textSize;
        float font_sz = mTextSize * (float) mAnnoViewImpl.mZoom;
        setTextSize(TypedValue.COMPLEX_UNIT_PX, font_sz);
    }

    public void updateColor(int color) {
        mAnnoViewImpl.updateColor(color);
        updatePadding();
        invalidate();
    }

    public void updateFillColor(int color) {
        mAnnoViewImpl.updateFillColor(color);
        invalidate();
    }

    public void updateThickness(float thickness) {
        mAnnoViewImpl.updateThickness(thickness);
        updatePadding();
        invalidate();
    }

    public void updateOpacity(float opacity) {
        mAnnoViewImpl.updateOpacity(opacity);
        updateTextColor(mTextColor);
    }

    public void updateFont(FontResource font) {
        if (null == font || Utils.isNullOrEmpty(font.getFilePath())) {
            return;
        }
        mAnnoViewImpl.mAnnotStyle.setFont(font);

        try {
            Typeface typeFace = Typeface.createFromFile(font.getFilePath());
            setTypeface(typeFace);
        } catch (Exception ignored) { // when font not found

        }
    }

    public void removeCtrlPts() {
        mAnnoViewImpl.removeCtrlPts();
        invalidate();
    }

    private void updatePadding() {
        updatePadding(getLeft(), getTop(), getRight(), getBottom());
    }

    private void updatePadding(int left, int top, int right, int bottom) {
        int paddingH = (int) (mAnnoViewImpl.mThicknessDraw * 2 + 0.5);
        int paddingV = (int) (mAnnoViewImpl.mThicknessDraw * 2 + 0.5);
        if (paddingH > ((right - left) / 2)) {
            paddingH = (int) (mAnnoViewImpl.mThicknessDraw + 0.5);
        }
        if (paddingV > ((bottom - top) / 2)) {
            paddingV = (int) (mAnnoViewImpl.mThicknessDraw + 0.5);
        }
        setPadding(paddingH, paddingV, paddingH, paddingV);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        mAnnoViewImpl.mPt1.set(left, top);
        mAnnoViewImpl.mPt2.set(right, bottom);

        updatePadding();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        DrawingUtils.drawRectangle(canvas,
            mAnnoViewImpl.mPt1, mAnnoViewImpl.mPt2,
            mAnnoViewImpl.mThicknessDraw,
            mAnnoViewImpl.mFillColor, mAnnoViewImpl.mStrokeColor,
            mAnnoViewImpl.mFillPaint, mAnnoViewImpl.mPaint);

        if (mAnnoViewImpl.mCanDrawCtrlPts) {
            try {
                DrawingUtils.drawCtrlPts(getContext().getResources(), canvas,
                    mAnnoViewImpl.mCtrlPtsPaint, mAnnoViewImpl.mPt1, mAnnoViewImpl.mPt2,
                    mAnnoViewImpl.mCtrlPts[e_lm], mAnnoViewImpl.mCtrlPts[e_ml], mAnnoViewImpl.mCtrlRadius,
                    mAnnoViewImpl.mHasSelectionPermission, false);
            } catch (Exception ignored) {
            }
        }

        super.onDraw(canvas);
    }
}
