package com.pdftron.pdf.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;

import com.pdftron.pdf.utils.Utils;

/**
 * A CoordinatorLayout that ignores top windows inset
 */
public class IgnoreTopInsetCoordinatorLayout extends CoordinatorLayout {

    public IgnoreTopInsetCoordinatorLayout(Context context) {
        this(context, null);
    }

    public IgnoreTopInsetCoordinatorLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IgnoreTopInsetCoordinatorLayout(Context context, AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (Utils.isLollipop() && ViewCompat.getFitsSystemWindows(this)) {
            // Remove stable-layout system-ui flag.
            int visibility = getSystemUiVisibility();
            visibility &= ~View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            setSystemUiVisibility(visibility);
        }
        setStatusBarBackground(null);
    }

    /**
     * @deprecated
     * Overload implementation of {@link CoordinatorLayout#fitSystemWindows(Rect)}
     * It sets top insets to 0
     */
    @SuppressWarnings("deprecation")
    @Override
    protected boolean fitSystemWindows(Rect insets) {
        if (!Utils.isLollipop()) {
            insets.top = 0;
        }
        return super.fitSystemWindows(insets);
    }

    /**
     * Overload implementation of {@link CoordinatorLayout#dispatchApplyWindowInsets(WindowInsets)}
     * It ignores top windows inset.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        return super.dispatchApplyWindowInsets(insets.replaceSystemWindowInsets(
            insets.getSystemWindowInsetLeft(), 0,
            insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
    }
}
