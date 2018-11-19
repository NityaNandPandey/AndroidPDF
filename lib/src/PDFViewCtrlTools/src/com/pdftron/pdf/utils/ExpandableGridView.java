//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.GridView;

import com.pdftron.pdf.tools.R;

import org.w3c.dom.Attr;

/**
 * A grid view that is expandable
 */
public class ExpandableGridView extends GridView {

    private boolean mExpanded;

    public ExpandableGridView(Context context) {
        super(context);
        init(null, 0);
    }

    public ExpandableGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ExpandableGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(null, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ExpandableGridView, defStyle, 0);
        setExpanded(a.getBoolean(R.styleable.ExpandableGridView_expanded, false));
        a.recycle();
    }

    /**
     * Overload implementation of {@link GridView#onMeasure(int, int)}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isExpanded()) {
            // Calculate entire height by providing a very large height hint.
            // View.MEASURED_SIZE_MASK represents the largest height possible.
            int expandSpec = MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK, MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, expandSpec);
        }
        else
        {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * Sets whether this grid view is expandable
     * @param expanded true then expandable, false otherwise
     */
    public void setExpanded(boolean expanded) {
        mExpanded = expanded;
    }

    /**
     * Whether this grid view is expandable
     * @return true then expandable, false otherwise
     */
    public boolean isExpanded() {
        return mExpanded;
    }

    /**
     * Overload implementation of {@link GridView#hasFocus()}
     * @return true
     */
    @Override
    public boolean hasFocus() {
        return true;
    }

    /**
     * Whether this grid view is focused
     * @return true
     */
    @Override
    public boolean isFocused() {
        return true;
    }

    /**
     * Whether this grid view has windows focus
     * @return true
     */
    @Override
    public boolean hasWindowFocus() {
        return true;
    }
}
