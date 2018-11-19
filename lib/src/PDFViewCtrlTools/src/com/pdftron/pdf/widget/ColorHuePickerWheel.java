package com.pdftron.pdf.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.pdftron.pdf.controls.ColorPickerView;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

/**
 * A Color Wheel for picking hue value of the color.
 */
public class ColorHuePickerWheel extends View {

    private static final String TAG = ColorHuePickerWheel.class.getName();
    private Paint mColorWheelPaint;
    private Paint mTransparentInnerCirclePaint;


    private Paint mColorPointerPaint;

    private Bitmap mColorWheelBitmap;

    private int mColorWheelRadius;

    private int mWheelWidth;
    private int mColorPointerRadius;

    private OnHueChangeListener mHueChangeListener;

    // selected hue
    private float mColorHue = 0f;

    public ColorHuePickerWheel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    public ColorHuePickerWheel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, R.attr.color_picker_style);
    }

    public ColorHuePickerWheel(Context context) {
        super(context);
        init(null, R.attr.color_picker_style);
    }

    private void init(AttributeSet attrs, int defStyle) {

        mColorPointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mColorPointerPaint.setStyle(Style.STROKE);
        int strokeWidth = getContext().getResources().getDimensionPixelOffset(R.dimen.padding_xsmall);
        mColorPointerPaint.setStrokeWidth(strokeWidth);

        mColorWheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mColorWheelPaint.setDither(true);

        mTransparentInnerCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTransparentInnerCirclePaint.setColor(Utils.getThemeAttrColor(getContext(), android.R.attr.colorBackground));

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ColorHuePickerWheel, defStyle, R.style.ColorPickerStyle);
        try {
            mWheelWidth = a.getDimensionPixelOffset(R.styleable.ColorHuePickerWheel_wheel_width, -1);
            mColorPointerRadius = a.getDimensionPixelOffset(R.styleable.ColorHuePickerWheel_value_pointer_radius, 2);

            int valueRingColor = a.getColor(R.styleable.ColorHuePickerWheel_ring_color, Color.WHITE);
            mColorPointerPaint.setColor(valueRingColor);

            int valueRingShadow = a.getDimensionPixelOffset(R.styleable.ColorHuePickerWheel_ring_shadow_radius, 0);
            mColorPointerPaint.setShadowLayer(valueRingShadow, 5, 5, Color.BLACK);
        }finally {
            a.recycle();
        }

    }

    /**
     * Overload implementation {@link View#onMeasure(int, int)}.
     * Make view to be a square
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.min(widthSize, heightSize);
        setMeasuredDimension(size, size);
    }

    /**
     * Overload implementation of {@link View#onDraw(Canvas)}
     */
    @Override
    protected void onDraw(Canvas canvas) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // drawing color wheel

        canvas.drawBitmap(mColorWheelBitmap, centerX - mColorWheelRadius, centerY - mColorWheelRadius, null);

        // draw color pointer
        float hueAngle = (float) Math.toRadians(mColorHue);
        int pointerPos = mColorWheelRadius - mWheelWidth / 2;
        int colorPointX = (int) (-Math.cos(hueAngle) * pointerPos) + centerX;
        int colorPointY = (int) (-Math.sin(hueAngle) * pointerPos) + centerY;
        canvas.drawCircle(colorPointX, colorPointY, mColorPointerRadius, mColorPointerPaint);
    }

    /**
     * Overload implementation of {@link View#onSizeChanged(int, int, int, int)}
     * @param width layout width
     * @param height layout height
     * @param oldw old width
     * @param oldh old height
     */
    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        mColorWheelRadius = width / 2 - getPaddingLeft();

        mColorWheelBitmap = createColorWheelBitmap(mColorWheelRadius * 2, mColorWheelRadius * 2);
    }

    /**
     * Creates a hue color wheel bitmap.
     * @param width width of color wheel
     * @param height height of color wheel
     * @return Hue color wheel bitmap
     */
    protected Bitmap createColorWheelBitmap(int width, int height) {

        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);

        int colorCount = 12;
        int colorAngleStep = 360 / 12;
        int colors[] = new int[colorCount + 1];
        float hsv[] = new float[] { 0f, 1f, 1f };
        for (int i = 0; i < colors.length; i++) {
            hsv[0] = (i * colorAngleStep + 180) % 360;
            colors[i] = Color.HSVToColor(hsv);
        }
        colors[colorCount] = colors[0];

        SweepGradient sweepGradient = new SweepGradient(width / 2, height / 2, colors, null);

        mColorWheelPaint.setShader(sweepGradient);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawCircle(width / 2, height / 2, mColorWheelRadius, mColorWheelPaint);
        if (mWheelWidth >= 0) {
            canvas.drawCircle(width / 2, height / 2, mColorWheelRadius - mWheelWidth, mTransparentInnerCirclePaint);
        }
        return bitmap;

    }


    /**
     * Overload implementation of {@link View#onTouchEvent(MotionEvent)}
     * If the touch point is inside color wheel, change the selected color hue value
     * @param event motion event
     * @return true if changed color hue value
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                int x = (int) event.getX();
                int y = (int) event.getY();
                int cx = x - getWidth() / 2;
                int cy = y - getHeight() / 2;
                double d = Math.sqrt(cx * cx + cy * cy);
                if (d <= mColorWheelRadius && d > mColorWheelRadius - mWheelWidth) {
                    mColorHue = (float) (Math.toDegrees(Math.atan2(cy, cx)) + 180f);
                    invalidate();
                    if (mHueChangeListener != null) {
                        mHueChangeListener.onHueChanged(mColorHue);
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_UP:{
                if (mHueChangeListener != null) {
                    mHueChangeListener.OnColorChanged(this, 0);
                }
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * Set color to color wheel. The color wheel will update the hue value of the color.
     * @param color color
     */
    public void setColor(@ColorInt int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        mColorHue = hsv[0];
        invalidate();
    }

    /**
     * Overload implementation of {@link View#onSaveInstanceState()}
     * @return saved instance
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putFloat("hue", mColorHue);
        state.putParcelable("super", super.onSaveInstanceState());
        return state;
    }


    /**
     * Overload implementation of {@link View#onRestoreInstanceState(Parcelable)}
     * @param state Previously saved instance
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mColorHue = bundle.getFloat("hue");
            super.onRestoreInstanceState(bundle.getParcelable("super"));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    /**
     * Set hue change listener
     * @param listener The listener
     */
    public void setOnHueChangeListener(OnHueChangeListener listener) {
        mHueChangeListener = listener;
    }
    /**
     * Listening for hue changes
     */
    public interface OnHueChangeListener extends ColorPickerView.OnColorChangeListener{
        /**
         * This method is invoked when there is hue changes by touch event
         * @param newHue new hue value of color
         */
        void onHueChanged(float newHue);
    }

}
