package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.KeyEvent;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.utils.DrawingUtils;

/**
 * An EditText that can auto scroll
 */
public class AutoScrollEditText extends AppCompatEditText {

    private AutoScrollEditTextListener mListener;

    private AnnotViewImpl mAnnoViewImpl;

    /**
     * Listener interface for key up event
     */
    public interface AutoScrollEditTextListener {
        /**
         * This method will be invoked when user released a key
         * @param keyCode Released key code
         * @param event The key event
         * @return true then intercept the key up event, false otherwise
         */
        boolean onKeyUp(int keyCode, KeyEvent event);
    }

    public AutoScrollEditText(Context context) {
        super(context);
    }

    public AutoScrollEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoScrollEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Sets auto scroll edit text listener
     * @param listener The listener
     */
    public void setAutoScrollEditTextListener(AutoScrollEditTextListener listener) {
        mListener = listener;
    }

    /**
     * Overload implementation of {@link android.widget.EditText#onKeyUp(int, KeyEvent)}
     * It invokes the auto scroll edit text listener key up event
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mListener != null && mListener.onKeyUp(keyCode, event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mAnnoViewImpl != null) {
            mAnnoViewImpl.mPt1.set(getLeft(), getTop());
            mAnnoViewImpl.mPt2.set(getRight(), getBottom());
            updatePadding(getLeft(), getTop(), getRight(), getBottom());
        }
    }

    @Override
    protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert);

        if (mAnnoViewImpl != null) {
            mAnnoViewImpl.mPt1.set(getScrollX(), getScrollY());
            mAnnoViewImpl.mPt2.set(getScrollX() + getWidth(), getScrollY() + getHeight());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mAnnoViewImpl != null) {
            DrawingUtils.drawRectangle(canvas,
                mAnnoViewImpl.mPt1, mAnnoViewImpl.mPt2,
                mAnnoViewImpl.mThicknessDraw,
                mAnnoViewImpl.mFillColor, mAnnoViewImpl.mStrokeColor,
                mAnnoViewImpl.mFillPaint, mAnnoViewImpl.mPaint);
        }

        super.onDraw(canvas);
    }

    private void updatePadding(int left, int top, int right, int bottom) {
        if (mAnnoViewImpl == null) {
            return;
        }
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

    public void setAnnotStyle(PDFViewCtrl pdfViewCtrl, AnnotStyle annotStyle) {
        mAnnoViewImpl = new AnnotViewImpl(pdfViewCtrl, annotStyle);
        mAnnoViewImpl.setZoom(pdfViewCtrl.getZoom());
    }
}
