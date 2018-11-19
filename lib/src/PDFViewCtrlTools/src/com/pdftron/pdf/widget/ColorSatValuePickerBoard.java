//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader.TileMode;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.pdftron.pdf.controls.ColorPickerView;
import com.pdftron.pdf.tools.R;

/**
 * A picker board for picking color HSV with a given Hue value.
 */
public class ColorSatValuePickerBoard extends View {

    private static final String TAG = ColorSatValuePickerBoard.class.getName();
    private Paint mColorBoardPaint;
    private Paint mColorPointerPaint;

    private Bitmap mColorBoardBitmap;

    private int mColorPointerRadius;

    private OnHSVChangeListener mSatValueChangeListener;

    private float[] mColorHSV = new float[] {0f, 1f, 1f};

    public ColorSatValuePickerBoard(Context context) {
        super(context);
        init(null, R.attr.color_picker_style);
    }

    public ColorSatValuePickerBoard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, R.attr.color_picker_style);
    }

    public ColorSatValuePickerBoard(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle){
        mColorBoardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mColorBoardPaint.setDither(true);

        mColorPointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mColorPointerPaint.setStyle(Paint.Style.STROKE);
        int strokeWidth = getContext().getResources().getDimensionPixelOffset(R.dimen.padding_xsmall);
        mColorPointerPaint.setStrokeWidth(strokeWidth);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ColorSatValuePickerBoard, defStyle, R.style.ColorPickerStyle);
        try {

            mColorPointerRadius = a.getDimensionPixelOffset(R.styleable.ColorSatValuePickerBoard_value_pointer_radius,2);

            int valueRingColor = a.getColor(R.styleable.ColorSatValuePickerBoard_ring_color, Color.WHITE);
            mColorPointerPaint.setColor(valueRingColor);

            int valueRingShadow = a.getDimensionPixelOffset(R.styleable.ColorSatValuePickerBoard_ring_shadow_radius, 0);
            mColorPointerPaint.setShadowLayer(valueRingShadow, 5, 5, Color.BLACK);
        }finally {
            a.recycle();
        }
    }

    /**
     * Overload implementation of {@link View#onSizeChanged(int, int, int, int)}
     * @param width Layout width
     * @param height Layout height
     * @param oldw Layout old width
     * @param oldh Layout old height
     */
    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        mColorBoardBitmap = createColorWheelBitmap(width, height);
    }


    /**
     * Overload implementation of {@link View#onDraw(Canvas)}
     * It draws the color picker board
     */
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mColorBoardBitmap, 0, 0, null);


        float saturation = mColorHSV[1];
        float value = mColorHSV[2];

        float x = getWidth() * saturation;
        float y = getHeight() * (1 - value);

        canvas.drawCircle(x, y, mColorPointerRadius, mColorPointerPaint);
    }

    /**
     * Creates a color picker board bitmap
     * @param width Width of the bitmap
     * @param height Height of the bitmap
     * @return A color picker board bitmap
     */
    protected Bitmap createColorWheelBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        int hueOnlyColor = Color.HSVToColor(new float[]{mColorHSV[0], 1f, 1f});

        LinearGradient satGradient = new LinearGradient(0, 0, width, 0, Color.WHITE, hueOnlyColor, TileMode.CLAMP);
        LinearGradient valueGradient = new LinearGradient(0, 0, 0, height, Color.WHITE, Color.BLACK, TileMode.CLAMP);
        ComposeShader composeShader = new ComposeShader(satGradient, valueGradient, PorterDuff.Mode.MULTIPLY);
        mColorBoardPaint.setShader(composeShader);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawRect(0, 0, width, height, mColorBoardPaint);
        return bitmap;
    }


    /**
     * Overload implementation of {@link View#onTouchEvent(MotionEvent)}
     * If the touching point is inside the color picker board, it updates the color SV value.
     * @param event motion event
     * @return true if the touching point is inside the color picker board
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                float x = event.getX();
                float y = event.getY();
                if (x >= 0 && x <= getWidth() && y >= 0 && y <= getHeight()) {
                    mColorHSV[1] = x / getWidth();
                    mColorHSV[2] = 1 - (y / getHeight());
                    invalidate();
                    if (mSatValueChangeListener != null) {
                        mSatValueChangeListener.onHSVChanged(mColorHSV);
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP: {
                if (mSatValueChangeListener != null) {
                    mSatValueChangeListener.OnColorChanged(this, 0);
                }
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * Sets the hue value of the color. It will update the color picker board based on the hue value.
     * @param hue Color hue value
     */
    public void setHue(float hue) {
        mColorHSV[0] = hue;
        if (getWidth() > 0 && getHeight() > 0) {
            mColorBoardBitmap = createColorWheelBitmap(getWidth(), getHeight());
        }
        invalidate();
    }

    /**
     * Sets color. It will update the color picker board based on the hue value.
     * @param color The color
     */
    public void setColor(@ColorInt int color) {
        Color.colorToHSV(color, mColorHSV);
        if (getWidth() > 0 && getHeight() > 0) {
            mColorBoardBitmap = createColorWheelBitmap(getWidth(), getHeight());
        }
        invalidate();
    }

    /**
     * Overload implementation of {@link View#onSaveInstanceState()}
     * @return saved instance
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putFloatArray("color", mColorHSV);
        state.putParcelable("super", super.onSaveInstanceState());
        return state;
    }

    /**
     * Overload implementation of {@link View#onRestoreInstanceState(Parcelable)}
     * @param state previously saved instance state
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mColorHSV = bundle.getFloatArray("color");
            super.onRestoreInstanceState(bundle.getParcelable("super"));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    /**
     * Set color SV value changes listener
     * @param listener The listener
     */
    public void setOnSaturationValueChangelistener(OnHSVChangeListener listener) {
        mSatValueChangeListener = listener;
    }

    /**
     * Listener interface for color HSV value changes event
     */
    public interface OnHSVChangeListener extends ColorPickerView.OnColorChangeListener {
        /**
         * The method will be invoked when there is color HSV value changes
         * @param hsv The color HSV value
         */
        void onHSVChanged(float[] hsv);
    }
}
