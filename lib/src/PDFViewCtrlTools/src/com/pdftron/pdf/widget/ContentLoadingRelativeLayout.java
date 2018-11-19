//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

/**
 * A Relative layout for loading content
 */
public class ContentLoadingRelativeLayout extends RelativeLayout {

    private static final int MIN_SHOW_TIME = 500; // ms
    private static final int MIN_DELAY = 500; // ms

    private long mStartTime = -1;

    private boolean mPostedHide = false;

    private boolean mPostedShow = false;

    private boolean mDismissed = false;

    private boolean mAnimateHide = true;

    private boolean mAnimateShow = true;

    private final Runnable mDelayedHide = new Runnable() {
        @Override
        public void run() {
            mPostedHide = false;
            mStartTime = -1;
            if (mAnimateHide) {
                startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
            }
            setVisibility(View.GONE);
            mPostedShow = false;
        }
    };

    private final Runnable mDelayedShow = new Runnable() {
        @Override
        public void run() {
            mPostedShow = false;
            if (!mDismissed) {
                mStartTime = System.currentTimeMillis();
                if (mAnimateShow) {
                    startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
                }
                setVisibility(View.VISIBLE);
                mPostedHide = false;
            }
        }
    };

    public ContentLoadingRelativeLayout(Context context) {
        super(context);
    }

    public ContentLoadingRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContentLoadingRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }



    /**
     * Overload implementation of {@link RelativeLayout#onAttachedToWindow()}
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks();
    }

    /**
     * Overload implementation of {@link RelativeLayout#onDetachedFromWindow()}
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks();
    }

    private void removeCallbacks() {
        removeCallbacks(mDelayedHide);
        removeCallbacks(mDelayedShow);
    }

    /**
     * Hide the progress view if it is visible. The progress view will not be
     * hidden until it has been shown for at least a minimum show time. If the
     * progress view was not yet visible, cancels showing the progress view.
     * @param forceHide Whether force the view to hide
     */
    public void hide(boolean forceHide) {
        hide(forceHide, true);
    }

    /**
     * Hide the progress view if it is visible. The progress view will not be
     * hidden until it has been shown for at least a minimum show time. If the
     * progress view was not yet visible, cancels showing the progress view.
     * @param forceHide Whether force the view to hide
     * @param animate Whether start fade out animation when hiding
     */
    public void hide(boolean forceHide, boolean animate) {
        mDismissed = true;
        removeCallbacks(mDelayedShow);
        mAnimateHide = animate;
        if (forceHide) {
            if (mAnimateHide) {
                startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
            }
            setVisibility(View.GONE);
            mPostedShow = false;
        } else {
            long diff = System.currentTimeMillis() - mStartTime;
            if (diff >= MIN_SHOW_TIME || mStartTime == -1) {
                // The progress spinner has been shown long enough
                // OR was not shown yet. If it wasn't shown yet,
                // it will just never be shown.
                if (mAnimateHide) {
                    startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
                }
                setVisibility(View.GONE);
                mPostedShow = false;
            } else {
                // The progress spinner is shown, but not long enough,
                // so put a delayed message in to hide it when its been
                // shown long enough.
                if (!mPostedHide) {
                    postDelayed(mDelayedHide, MIN_SHOW_TIME - diff);
                    mPostedHide = true;
                }
            }
        }
    }

    /**
     * Show the progress view after waiting for a minimum delay. If
     * during that time, hide() is called, the view is never made visible.
     */
    public void show() {
        show(false, true, true);
    }

    /**
     * Show the progress view after waiting for a minimum delay. If
     * during that time, hide() is called, the view is never made visible.
     * @param forceShow Whether to force the view to show instantly.
     * @param delay Whether to wait the progress view for a minimum delay.
     * @param animate Whether start fade in animation when showing the progress view
     */
    public void show(boolean forceShow, boolean delay, boolean animate) {
        // Reset the start time.
        mStartTime = -1;
        mDismissed = false;
        removeCallbacks(mDelayedHide);
        mAnimateShow = animate;
        if (forceShow) {
            if (mAnimateShow) {
                startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in));
            }
            setVisibility(View.VISIBLE);
            mPostedShow = false;
        } else {
            if (!mPostedShow) {
                if (delay) {
                    postDelayed(mDelayedShow, MIN_DELAY);
                } else {
                    post(mDelayedShow);
                }
                mPostedShow = true;
            }
        }
    }
}
