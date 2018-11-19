//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * A seek bar that can reverse in RTL mode
 */
public class MirrorSeekBar extends AppCompatSeekBar {

    boolean mIsReversed = false;
    Drawable mDrawable = null;

    /**
     * Class constructor
     */
    public MirrorSeekBar(Context context) {
        super(context);
        mIsReversed = false;
    }

    /**
     * Class constructor
     */
    public MirrorSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIsReversed = false;
    }

    /**
     * Class constructor
     */
    public MirrorSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mIsReversed = false;
    }

    /**
     * @return True if reversed
     */
    public boolean isReversed() {
        return mIsReversed;
    }

    /**
     * Sets whether the seek bar should be reversed or not.
     *
     * @param isReversed True if should be reversed
     */
    public void setReversed(boolean isReversed) {
        mIsReversed = isReversed;
        if (isReversed) {
            mDrawable = getBackground();
            setBackground(null);
        } else if (mDrawable != null){
            setBackground(mDrawable);
        }
        invalidate();
        refreshDrawableState();
    }

    /**
     * The overload implementation of {@link AppCompatSeekBar#onDraw(Canvas)}.
     *
     * @param canvas The canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (mIsReversed) {
            float px = this.getWidth() / 2.0f;
            float py = this.getHeight() / 2.0f;
            canvas.scale(-1, 1, px, py);
        }
        super.onDraw(canvas);
    }

    /**
     * The overload implementation of {@link AppCompatSeekBar#onTouchEvent(MotionEvent)}.
     *
     * @param event The touch event
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mIsReversed) {
            event.setLocation(this.getWidth() - event.getX(), event.getY());
        }

        return super.onTouchEvent(event);
    }
}
