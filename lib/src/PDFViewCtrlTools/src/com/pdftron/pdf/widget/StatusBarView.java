package com.pdftron.pdf.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;
import android.view.animation.Interpolator;

import com.pdftron.pdf.tools.R;

/**
 * A Status bar view that can show/ hide based on full screen flags.
 */
public class StatusBarView extends View implements
        View.OnSystemUiVisibilityChangeListener {

    private static final String TAG = StatusBarView.class.getName();

    private Interpolator mShowInterpolator = null;
    private Interpolator mHideInterpolator = null;
    private int mShowHideDuration = 0;

    private WindowInsetsCompat mLastInsets = null;
    private int mLastVisibility = 0;

    public StatusBarView(Context context) {
        this(context, null);
    }

    public StatusBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.StatusBarView, defStyleAttr,
                R.style.StatusBarView);

        Drawable background;
        try {
            ViewCompat.setFitsSystemWindows(this, a.getBoolean(R.styleable.StatusBarView_android_fitsSystemWindows, false));
            background = a.getDrawable(R.styleable.StatusBarView_android_background);
        } finally {
            a.recycle();
        }

        ViewCompat.setBackground(this, background);

        mShowInterpolator = new LinearOutSlowInInterpolator();
        mHideInterpolator = new FastOutLinearInInterpolator();

        mShowHideDuration = getResources().getInteger(R.integer.system_bars_enter_exit_duration);

        mLastVisibility = ViewCompat.getWindowSystemUiVisibility(this);
        setOnSystemUiVisibilityChangeListener(this);

        ViewCompat.setOnApplyWindowInsetsListener(this, new android.support.v4.view.OnApplyWindowInsetsListener() {
            /**
             * This is the ViewCompat version of {@link View#onApplyWindowInsets(WindowInsets)}, which is
             * only executed for Android version 21 and up (Lollipop).
             */
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
                if (mLastInsets == null ||
                        (insets != null && insets.getSystemWindowInsetTop() != mLastInsets.getSystemWindowInsetTop())) {
                    mLastInsets = insets;
                    requestLayout();
                }
                return insets;
            }
        });
    }

    /**
     * Override the height {@link android.view.View.MeasureSpec measure spec} to always measure this
     * View exactly using the top system window inset if available,
     * otherwise measure with a zero height.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = mLastInsets != null ? mLastInsets.getSystemWindowInsetTop() : 0;
        int newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

        super.onMeasure(widthMeasureSpec, newHeightMeasureSpec);
    }

    /**
     * Overload implementation of {@link android.view.View.OnSystemUiVisibilityChangeListener#onSystemUiVisibilityChange(int)}
     * Show this status bar is system ui is visible, hide otherwise.
     * @param visibility System UI visibility
     */
    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        int diff = visibility ^ mLastVisibility;
        if ((diff & View.SYSTEM_UI_FLAG_FULLSCREEN) == View.SYSTEM_UI_FLAG_FULLSCREEN) {
            // Cancel any ongoing animations.
            ViewCompat.animate(this).cancel();

            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == View.SYSTEM_UI_FLAG_FULLSCREEN) {
                // Now in fullscreen mode.
                hide();
            } else {
                // No longer in fullscreen mode.
                show();
            }
        }
        mLastVisibility = visibility;
    }

    private void hide() {
        ViewCompat.animate(this)
                .alpha(0.0f)
                .setDuration(mShowHideDuration)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        setVisibility(View.GONE);
                        setAlpha(1.0f);
                    }
                })
                .setInterpolator(mHideInterpolator)
                .withLayer();
    }

    private void show() {
        if (getVisibility() != View.VISIBLE) {
            setAlpha(0.0f);
            setVisibility(View.VISIBLE);
        }
        ViewCompat.animate(this)
                .alpha(1.0f)
                .setDuration(mShowHideDuration)
                .setInterpolator(mShowInterpolator)
                .withLayer();
    }
}
