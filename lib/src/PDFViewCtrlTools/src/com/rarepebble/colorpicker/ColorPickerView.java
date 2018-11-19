/*
 * Copyright (C) 2015 Martin Stone
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rarepebble.colorpicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.pdftron.pdf.tools.R;

public class ColorPickerView extends FrameLayout implements ObservableColor.Observer {

	private final ObservableColor observableColor = new ObservableColor(0);
	private ColorPickerViewListener mListener = null;

	@Override
	public void updateColor(ObservableColor observableColor) {
		if (mListener != null)
			mListener.onColorChanged(observableColor.getColor());
	}

	public interface ColorPickerViewListener {
		void onColorChanged(int color);
	}

	public ColorPickerView(Context context) {
		this(context, null);
	}

	public ColorPickerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.view_colorpicker, this);

		HueSatView hueSatView = (HueSatView)findViewById(R.id.hueSatView);
		hueSatView.observeColor(observableColor);

		ValueView valueView = (ValueView)findViewById(R.id.valueView);
		valueView.observeColor(observableColor);

		observableColor.addObserver(this);

		applyAttributes(attrs);

		// We get all our state saved and restored for free,
		// thanks to the EditText and its listeners!
	}

	void applyAttributes(AttributeSet attrs) {
		if (attrs != null) {
			TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ColorPicker, 0, 0);
			showAlpha(a.getBoolean(R.styleable.ColorPicker_colorpicker_showAlpha, true));
			showHex(a.getBoolean(R.styleable.ColorPicker_colorpicker_showHex, true));
		}
	}

	public void setListener(ColorPickerViewListener listener) {
		mListener = listener;
	}

	/** Returns the color selected by the user */
	public int getColor() {
		return observableColor.getColor();
	}

	/** Sets the original color swatch and the current color to the specified value. */
	public void setColor(int color) {
		setOriginalColor(color);
		setCurrentColor(color);
	}

	/** Sets the original color swatch without changing the current color. */
	public void setOriginalColor(int color) {

	}

	/** Updates the current color without changing the original color swatch. */
	public void setCurrentColor(int color) {
		observableColor.updateColor(color, null);

	}

	public void showAlpha(boolean showAlpha) {

	}

	public void showHex(boolean showHex) {

	}
}
