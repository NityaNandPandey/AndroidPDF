//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * A custom view pager that can toggle off swipe event.
 */
public class CustomViewPager extends ViewPager {

    private boolean mIsSwippingEnabled = true;

    public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Sets whether this view pager can be swiped
     * @param enabled true then this view pager can be swiped, false otherwise
     */
    public void setSwippingEnabled(boolean enabled) {
        mIsSwippingEnabled = enabled;
    }

    /**
     * Overload implementation of {@link ViewPager#onInterceptTouchEvent(MotionEvent)}
     * @param ev motion event
     * @return true if this view pager can be swipe, false otherwise
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mIsSwippingEnabled && super.onInterceptTouchEvent(ev);
    }

    /**
     * Overload implementation of {@link ViewPager#onTouchEvent(MotionEvent)}
     * @param ev motion event
     * @return true if this view pager can be swipe, false otherwise
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mIsSwippingEnabled && super.onTouchEvent(ev);
    }
}
