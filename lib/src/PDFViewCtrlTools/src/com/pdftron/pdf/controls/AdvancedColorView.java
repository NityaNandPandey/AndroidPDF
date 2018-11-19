//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.ColorHuePickerWheel;
import com.pdftron.pdf.widget.ColorSatValuePickerBoard;

/**
 * A ConstraintLayout to show color picker board includes {@link ColorHuePickerWheel}
 * and {@link ColorSatValuePickerBoard}.
 */
public class AdvancedColorView extends ConstraintLayout implements
    ColorHuePickerWheel.OnHueChangeListener,
    ColorSatValuePickerBoard.OnHSVChangeListener {

    private ColorHuePickerWheel mColorHuePicker;
    private ColorSatValuePickerBoard mColorSaturationPicker;
    private ImageView mPrevColorImage;
    private ImageView mCurrColorImage;

    private EditText mColorEditText;
    private @ColorInt
    int mColor;

    private ColorPickerView.OnColorChangeListener mColorChangeListener;

    /**
     * Class constructor
     */
    public AdvancedColorView(Context context) {
        super(context);
        init();
    }
    /**
     * Class constructor
     */
    public AdvancedColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    /**
     * Class constructor
     */
    public AdvancedColorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.color_picker_layout_advanced, this);
        mColorHuePicker = findViewById(R.id.color_hue_picker);
        mColorEditText = findViewById(R.id.color_edit_text);
        mColorEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                return onColorEditTextActionChanged(textView, i, keyEvent);
            }
        });
        mColorEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                onColorEditTextFocusChanged(view, b);
            }
        });

        mColorSaturationPicker = findViewById(R.id.color_saturation_picker);

        mPrevColorImage = findViewById(R.id.prev_color);
        mCurrColorImage = findViewById(R.id.curr_color);

        mColorHuePicker.setOnHueChangeListener(this);
        mColorSaturationPicker.setOnSaturationValueChangelistener(this);
    }

    /**
     * Initialize color
     *
     * @param color color
     */
    public void setSelectedColor(@ColorInt int color) {
        Drawable preColor = mPrevColorImage.getBackground();
        preColor.mutate();
        preColor.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        setColor(color);
    }

    private void setColor(@ColorInt int color) {
        mColor = color;
        mColorHuePicker.setColor(color);
        mColorEditText.setText(Utils.getColorHexString(color));
        mColorSaturationPicker.setColor(color);
        updateCurrColorPreview(color);
    }

    private void updateCurrColorPreview(@ColorInt int color) {
        Drawable currColor = mCurrColorImage.getBackground();
        currColor.mutate();
        currColor.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private boolean onColorEditTextActionChanged(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            Utils.hideSoftKeyboard(getContext(), mColorEditText);
            mColorEditText.clearFocus();
            return true;
        }
        return false;
    }

    private void onColorEditTextFocusChanged(View v, boolean hasFocus) {
        if (mColorEditText == null || Utils.isNullOrEmpty(mColorEditText.getText().toString())) {
            return;
        }
        if (!hasFocus) {
            try {
                int color = Color.parseColor(mColorEditText.getText().toString());
                setColor(color);
                invokeColorChangeListener();
            } catch (IllegalArgumentException e) {
                mColorEditText.setText(Utils.getColorHexString(mColor));
                CommonToast.showText(getContext(), R.string.error_illegal_color, Toast.LENGTH_SHORT);
            }
        }
    }

    private void invokeColorChangeListener() {
        if (mColorChangeListener != null) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_COLOR_PICKER,
                String.format("color selected %s", Utils.getColorHexString(mColor)),
                AnalyticsHandlerAdapter.LABEL_WHEEL);
            mColorChangeListener.OnColorChanged(this, mColor);
        }
    }

    /**
     * Gets color
     * @return color
     */
    public int getColor() {
        return mColor;
    }

    /**
     * This method invoked when {@link ColorSatValuePickerBoard} changed color
     *
     * @param hsv hsv color
     */
    @Override
    public void onHSVChanged(float[] hsv) {
        mColor = Color.HSVToColor(hsv);
        mColorEditText.setText(Utils.getColorHexString(mColor));
        updateCurrColorPreview(mColor);
    }

    /**
     * This method invoked when {@link ColorHuePickerWheel} changed color hue value
     *
     * @param newHue new hue value
     */
    @Override
    public void onHueChanged(float newHue) {
        float[] hsv = new float[3];
        Color.colorToHSV(mColor, hsv);
        hsv[0] = newHue;
        mColor = Color.HSVToColor(hsv);
        mColorSaturationPicker.setHue(newHue);
        mColorEditText.setText(Utils.getColorHexString(mColor));
        updateCurrColorPreview(mColor);
    }

    /**
     * Sets color change listener. listener will be invoked when there is color change event
     *
     * @param listener color change listener
     */
    public void setOnColorChangeListener(ColorPickerView.OnColorChangeListener listener) {
        mColorChangeListener = listener;
    }

    @Override
    public void OnColorChanged(View view, int color) {
        invokeColorChangeListener();
    }
}
