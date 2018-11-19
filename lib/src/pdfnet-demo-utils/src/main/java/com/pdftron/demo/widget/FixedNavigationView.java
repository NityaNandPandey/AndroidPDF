package com.pdftron.demo.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.design.widget.NavigationView;
import android.support.v4.view.WindowInsetsCompat;
import android.util.AttributeSet;
import android.view.View;

public class FixedNavigationView extends NavigationView {

    public FixedNavigationView(Context context) {
        this(context, null);
    }

    public FixedNavigationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FixedNavigationView(Context context, AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * The {@link NavigationView}'s implementation of this method does not consider the
     * bottom system window inset, so to avoid the content being obscured padding is added manually.
     */
    @SuppressLint("RestrictedApi")
    @Override
    protected void onInsetsChanged(WindowInsetsCompat insets) {
        final int height = getHeight();
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            // Pad children that touch the parent's bottom.
            if (child.getBottom() == height) {
                child.setPadding(child.getPaddingLeft(), child.getPaddingTop(),
                        child.getPaddingRight(), insets.getSystemWindowInsetBottom());
            }
        }
        super.onInsetsChanged(insets);
    }
}
