//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.ColorPickerGridViewAdapter;
import com.pdftron.pdf.utils.ExpandableGridView;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A {@link ExpandableGridView} shows a list of standard preset colors.
 */
public class PresetColorGridView extends ExpandableGridView {
    private static final String TAG = PresetColorGridView.class.getName();
    private ColorPickerGridViewAdapter mAdapter;
    private int mTransparentColorPosition = -1;

    /**
     * Class constructor
     */
    public PresetColorGridView(Context context) {
        super(context);
        init(null, R.attr.preset_colors_style);
    }

    /**
     * Class constructor
     */
    public PresetColorGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, R.attr.preset_colors_style);
    }

    /**
     * Class constructor
     */
    public PresetColorGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PresetColorGridView, defStyle, R.style.PresetColorGridViewStyle);
        try {
            // preset colors list
            int presetColorRef = a.getResourceId(R.styleable.PresetColorGridView_color_list, -1);
            if (presetColorRef != -1) {
                String[] colors = getContext().getResources().getStringArray(presetColorRef);
                if (colors.length > 0) {
                    ArrayList<String> colorlist = new ArrayList<>(Arrays.asList(colors));
                    mAdapter = new ColorPickerGridViewAdapter(getContext(), colorlist);
//                    int cellSize = getContext().getResources().getDimensionPixelSize(R.dimen.quick_menu_button_size);
//                    mAdapter.setCellSize(cellSize, cellSize);
                    setAdapter(mAdapter);
                }
            }

            // spacing
            int hSpacing = a.getDimensionPixelOffset(R.styleable.PresetColorGridView_android_horizontalSpacing, 0);
            setHorizontalSpacing(hSpacing);
            int vSpacing = a.getDimensionPixelOffset(R.styleable.PresetColorGridView_android_verticalSpacing, 0);
            setVerticalSpacing(vSpacing);

            // column width
            int columnWidth = a.getDimensionPixelOffset(R.styleable.PresetColorGridView_android_columnWidth, -1);
            if (columnWidth > 0) {
                setColumnWidth(columnWidth);
            }

            // stretchMode
            int index = a.getInt(R.styleable.PresetColorGridView_android_stretchMode, STRETCH_COLUMN_WIDTH);
            if (index >= 0) {
                setStretchMode(index);
            }

            // num columns
            int numColumns = a.getInt(R.styleable.PresetColorGridView_android_numColumns, 1);
            setNumColumns(numColumns);

            // gravity
            index = a.getInt(R.styleable.PresetColorGridView_android_gravity, -1);
            if (index >= 0) {
                setGravity(index);
            }

        }finally {
            a.recycle();
        }
    }

    /**
     * Sets selected color. If color matches any color inside color grid, it will show a white check mark.
     * @param color the color
     */
    public void setSelectedColor(@ColorInt int color) {
        if (color == Color.TRANSPARENT) {
            mAdapter.setSelected(ColorPickerGridViewAdapter.TYPE_TRANSPARENT);
        } else {
            mAdapter.setSelected(Utils.getColorHexString(color));
        }
    }

    /**
     * Overrides implementation of {@link ExpandableGridView#getAdapter()}
     * @return adapter
     */
    @Override
    public ColorPickerGridViewAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Sets selected color. If color matches any color inside color grid, it will show a white check mark.
     * @param colorSrc the color source in string format
     */
    public void setSelectedColor(String colorSrc) {
        mAdapter.setSelected(colorSrc);
    }

    /**
     * Whether to show transparent grid item
     * @param showTransparent true then show transparent item, false otherwise
     */
    public void showTransparentColor(boolean showTransparent) {
        if (showTransparent && mTransparentColorPosition > 0) {
            mAdapter.addItem(mTransparentColorPosition, ColorPickerGridViewAdapter.TYPE_TRANSPARENT);
            mTransparentColorPosition = -1;
        } else if(!showTransparent) {
            mTransparentColorPosition = mAdapter.removeItem(ColorPickerGridViewAdapter.TYPE_TRANSPARENT);
        }
    }
}
