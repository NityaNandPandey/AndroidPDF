//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import com.pdftron.pdf.utils.Utils;

/**
 * This class is responsible for handing insects when the view is toolbar.
 */
public abstract class InsectHandlerToolbar extends FrameLayout {

    private Object mLastInsets;

    /**
     * Class constructor
     */
    public InsectHandlerToolbar(@NonNull Context context) {
        super(context);
    }

    /**
     * Class constructor
     */
    public InsectHandlerToolbar(@NonNull Context context,
                                @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Class constructor
     */
    public InsectHandlerToolbar(@NonNull Context context,
                                @Nullable AttributeSet attrs,
                                int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Class constructor
     */
    @SuppressWarnings("unused")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public InsectHandlerToolbar(@NonNull Context context,
                                @Nullable AttributeSet attrs,
                                int defStyleAttr,
                                int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * The overloaded implementation of {@link FrameLayout#onAttachedToWindow()}.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (ViewCompat.getFitsSystemWindows(this) && mLastInsets == null) {
            // We want insets but have not received any yet, so request them.
            ViewCompat.requestApplyInsets(this);
        }
    }

    /**
     * The overloaded implementation of {@link FrameLayout#fitSystemWindows(Rect)}.
     */
    @SuppressWarnings("deprecation")
    @Override
    protected boolean fitSystemWindows(Rect insets) {
        if (Utils.isLollipop()) {
            return super.fitSystemWindows(insets);
        } else {
            if (ViewCompat.getFitsSystemWindows(this) && getLayoutParams() instanceof MarginLayoutParams) {
                MarginLayoutParams mlp = (MarginLayoutParams) getLayoutParams();

                if (insets != null
                    && (mlp.leftMargin != insets.left || mlp.topMargin != insets.top
                    || mlp.rightMargin != insets.right)) {
                    // System window insets have changed (ignoring changes to the bottom inset).
                    mlp.setMargins(insets.left, insets.top, insets.right, 0);

                    // We need to requestLayout, but calling it at the wrong time can cause the
                    // View to enter a bad state.
                    post(new Runnable() {
                        @Override
                        public void run() {
                            requestLayout();
                        }
                    });
                }

                if (mLastInsets == null) {
                    mLastInsets = new Rect();
                }
                ((Rect) mLastInsets).set(insets);

                return true;
            }
            return false;
        }
    }

    /**
     * The overloaded implementation of {@link FrameLayout#onApplyWindowInsets(WindowInsets)}.
     * Apply all the system window insets except the bottom as margins.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (getFitsSystemWindows() && getLayoutParams() instanceof MarginLayoutParams) {
            final MarginLayoutParams mlp = (MarginLayoutParams) getLayoutParams();
            if (insets != null
                && (mlp.leftMargin != insets.getSystemWindowInsetLeft()
                || mlp.topMargin != insets.getSystemWindowInsetTop()
                || mlp.rightMargin != insets.getSystemWindowInsetRight())) {
                // System window insets have changed (ignoring changes to the bottom inset).
                mlp.setMargins(insets.getSystemWindowInsetLeft(),
                    insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);

                requestLayout();
            }
        }
        mLastInsets = insets;

        return insets;
    }
}
