//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.pdftron.demo.R;
import com.pdftron.demo.navigation.adapter.BaseFileAdapter;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;


public class SimpleRecyclerFastScroller extends LinearLayout {
    private View bubble;
    private View handle;
    private static final int HANDLE_ANIMATION_DURATION = 1000;
    private static final String Tag = SimpleRecyclerFastScroller.class.getName();
    private static final String SCALE_X = "scaleX";
    private static final String SCALE_Y = "scaleY";
    private static final String ALPHA = "alpha";
    private int height;
    private AnimatorSet currentAnimator = null;
    private boolean disabled = false;
    private boolean isTouching = false;
    private int maxVisibleRange = -1;
    private int mSpanCount = 0;

    private SimpleRecyclerView recyclerView;

    private final ScrollListener scrollListener = new ScrollListener();

    private static final int HANDLE_HIDE_DELAY = 3000;
    private static final int TRACK_SNAP_RANGE = 5;

    private final HandleHider handleHider = new HandleHider();


    public SimpleRecyclerFastScroller(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialise(context);
    }


    public SimpleRecyclerFastScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialise(context);
    }

    private void initialise(Context context) {
        setOrientation(HORIZONTAL);
        setClipChildren(false);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.fastscroller, this);
        bubble = findViewById(R.id.fastscroller_bubble);
        handle = findViewById(R.id.fastscroller_handle);
        handle.setVisibility(INVISIBLE);
    }

    private void showHandle() {
        AnimatorSet animatorSet = new AnimatorSet();
        handle.setPivotX(handle.getWidth());
        handle.setPivotY(handle.getHeight());
        handle.setVisibility(VISIBLE);
        Animator growerX = ObjectAnimator.ofFloat(handle, SCALE_X, 0f, 1f).setDuration(HANDLE_ANIMATION_DURATION);
        Animator growerY = ObjectAnimator.ofFloat(handle, SCALE_Y, 0f, 1f).setDuration(HANDLE_ANIMATION_DURATION);
        Animator alpha = ObjectAnimator.ofFloat(handle, ALPHA, 0f, 1f).setDuration(HANDLE_ANIMATION_DURATION);
        animatorSet.playTogether(growerX, growerY, alpha);
        animatorSet.start();
    }

    private void hideHandle() {
        currentAnimator = new AnimatorSet();
        handle.setPivotX(handle.getWidth());
        handle.setPivotY(handle.getHeight());
        Animator shrinkerX = ObjectAnimator.ofFloat(handle, SCALE_X, 1f, 0f).setDuration(HANDLE_ANIMATION_DURATION);
        Animator shrinkerY = ObjectAnimator.ofFloat(handle, SCALE_Y, 1f, 0f).setDuration(HANDLE_ANIMATION_DURATION);
        Animator alpha = ObjectAnimator.ofFloat(handle, ALPHA, 1f, 0f).setDuration(HANDLE_ANIMATION_DURATION);
        currentAnimator.playTogether(shrinkerX, shrinkerY, alpha);
        currentAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                handle.setVisibility(INVISIBLE);
                currentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                handle.setVisibility(INVISIBLE);
                currentAnimator = null;
            }
        });
        currentAnimator.start();
    }


    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        height = h;
        maxVisibleRange = -1;
    }

    private void setPosition(float y) {
        float position = y / height;
        int bubbleHeight = bubble.getHeight();
        bubble.setY(getValueInRange(0, height - bubbleHeight, (int) ((height - bubbleHeight) * position)));
        int handleHeight = handle.getHeight();
        handle.setY(getValueInRange(0, height - handleHeight, (int) ((height - handleHeight) * position)));
    }

    private int getValueInRange(int min, int max, int value) {
        int minimum = Math.max(min, value);
        return Math.min(minimum, max);
    }


    public void setRecyclerView(SimpleRecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        recyclerView.addOnScrollListener(scrollListener);
    }

    private class ScrollListener extends SimpleRecyclerView.OnScrollListener {

        boolean isScrolled = false;

        @Override
        public void onScrolled(RecyclerView rv, int dx, int dy) {
            chechSpanCount();
            int visibleRange = recyclerView.getChildCount();
            int itemCount = recyclerView.getAdapter().getItemCount();
            maxVisibleRange = (visibleRange > maxVisibleRange) ? visibleRange : maxVisibleRange;

            if (itemCount <= maxVisibleRange * 3 || isTouching) {
                return;
            }

            if (dy != 0 || (isScrolled && dy != 0)) {
                getHandler().removeCallbacks(handleHider);
                if (handle.getVisibility() == INVISIBLE) {
                    showHandle();
                }
            }
            View firstVisibleView = recyclerView.getChildAt(0);
            int firstVisiblePosition = recyclerView.getChildPosition(firstVisibleView);
            int med = firstVisiblePosition + visibleRange / 2;
            int position = 0;

            position = firstVisiblePosition + med * visibleRange / itemCount;
            float proportion = (float) position / (float) itemCount;

            setPosition(height * proportion);
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                getHandler().postDelayed(handleHider, HANDLE_HIDE_DELAY);
                isScrolled = true;
            }
            ;
        }

    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        chechSpanCount();
        if (disabled || recyclerView == null || recyclerView.getLayoutParams() == null) {
            return super.onTouchEvent(event);
        }
        int visibleRange = recyclerView.getChildCount();
        int itemCount = recyclerView.getAdapter().getItemCount();
        maxVisibleRange = (visibleRange > maxVisibleRange) ? visibleRange : maxVisibleRange;
        if (itemCount <= maxVisibleRange * 3) {
            return super.onTouchEvent(event);

        }
        if (!disabled && recyclerView != null && recyclerView.getLayoutParams() != null &&
                (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE)) {
            isTouching = true;
            setPosition(event.getY());

            if (currentAnimator != null) {
                currentAnimator.cancel();
            }
            getHandler().removeCallbacks(handleHider);
            if (handle.getVisibility() == INVISIBLE) {
                showHandle();
            }
            setRecyclerViewPosition(event.getY());
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            isTouching = false;
            getHandler().postDelayed(handleHider, HANDLE_HIDE_DELAY);
            return true;
        }
        return super.onTouchEvent(event);
    }


    private class HandleHider implements Runnable {
        @Override
        public void run() {
            hideHandle();
        }
    }

    private void setRecyclerViewPosition(float y) {
        if (recyclerView != null) {
            int itemCount = recyclerView.getAdapter().getItemCount();
            float proportion;
            if (bubble.getY() == 0) {
                proportion = 0f;
            } else if (bubble.getY() + bubble.getHeight() >= height - TRACK_SNAP_RANGE) {
                proportion = 1f;
            } else {
                proportion = y / (float) height;
            }
            int targetPos = getValueInRange(0, itemCount - 1, (int) (proportion * (float) itemCount));
            recyclerView.scrollToPosition(targetPos);
        }
    }

    private void chechSpanCount(){
        if(((BaseFileAdapter)recyclerView.getAdapter()).getSpanCount() != mSpanCount){
            mSpanCount = ((BaseFileAdapter)recyclerView.getAdapter()).getSpanCount();
            maxVisibleRange = -1;
        }
    }

}
