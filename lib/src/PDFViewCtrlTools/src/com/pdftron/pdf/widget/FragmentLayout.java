package com.pdftron.pdf.widget;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.util.AttributeSet;
import android.view.DispatchFairInsetsFrameLayout;
import android.view.View;

public class FragmentLayout extends DispatchFairInsetsFrameLayout {
    private WindowInsetsCompat mLastInsets = null;

    public FragmentLayout(Context context) {
        this(context, null);
    }

    public FragmentLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FragmentLayout(Context context, AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        ViewCompat.setOnApplyWindowInsetsListener(this, new android.support.v4.view.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
                if (insets != null && mLastInsets != insets) {
                    final int childCount = getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        final View child = getChildAt(i);
                        // Child views that fitSystemWindows are responsible for handling insets.
                        if (child != null && !ViewCompat.getFitsSystemWindows(child)) {
                            // Child view does not fitSystemWindows, so apply insets as margins.
                            applyMarginInsets(child, insets);
                        }
                    }
                    mLastInsets = insets;
                }
                return insets;
            }
        });
    }

    private void applyMarginInsets(@NonNull View view, @NonNull WindowInsetsCompat insets) {
        if (view.getLayoutParams() != null && view.getLayoutParams() instanceof MarginLayoutParams) {
            // Child view does not fitSystemWindows, so apply insets as margins.
            MarginLayoutParams mlp = (MarginLayoutParams) view.getLayoutParams();

            if (mlp.leftMargin != insets.getSystemWindowInsetLeft()
                    || mlp.topMargin != insets.getSystemWindowInsetTop()
                    || mlp.rightMargin != insets.getSystemWindowInsetRight()
                    || mlp.bottomMargin != insets.getSystemWindowInsetBottom()) {
                // Update child view inset-margins.
                mlp.setMargins(insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom());

                view.requestLayout();
            }
        }
    }
}
