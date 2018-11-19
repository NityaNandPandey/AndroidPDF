//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.webkit.WebView;

/**
 * WebView for Reflow.
 */
public class ReflowWebView extends WebView {

    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;

    /**
     * Callback interface to be invoked when a gesture occurs.
     */
    public interface ReflowWebViewCallback {

        /**
         * Called when a scale gesture begins.
         *
         * @param detector The {@link ScaleGestureDetector}
         *
         * @return True if handled
         */
        boolean onReflowWebViewScaleBegin(ScaleGestureDetector detector);

        /**
         * Called when user scales.
         *
         * @param detector The {@link ScaleGestureDetector}
         *
         * @return True if handled
         */
        boolean onReflowWebViewScale(ScaleGestureDetector detector);

        /**
         * Called when a scale gesture ends.
         *
         * @param detector The {@link ScaleGestureDetector}
         */
        void onReflowWebViewScaleEnd(ScaleGestureDetector detector);

        /**
         * Called when a tap occurs with the up event.
         *
         * @param event The {@link MotionEvent}
         */
        void onReflowWebViewSingleTapUp(MotionEvent event);
    }

    private ReflowWebViewCallback mCallback;

    /**
     * Sets the {@link ReflowWebViewCallback} listener
     *
     * @param listener The listener
     */
    public void setListener(ReflowWebViewCallback listener) {
        mCallback = listener;
    }

    /**
     * Class constructor
     */
    public ReflowWebView(Context context) {
        super(context);
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(getContext(), new TapListener());
    }

    /**
     * Class constructor
     */
    public ReflowWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(getContext(), new TapListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(ev);
        }
        if (mScaleGestureDetector != null) {
            mScaleGestureDetector.onTouchEvent(ev);
        }

        return true;
    }

    private class TapListener implements GestureDetector.OnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            if (mCallback != null) {
                mCallback.onReflowWebViewSingleTapUp(event);
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                                float distanceY) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            return true;
        }

        @Override
        public void onShowPress(MotionEvent event) {
        }

        @Override
        public void onLongPress(MotionEvent event) {
        }
    }

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return mCallback == null || mCallback.onReflowWebViewScaleBegin(detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return mCallback == null || mCallback.onReflowWebViewScale(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mCallback != null) {
                mCallback.onReflowWebViewScaleEnd(detector);
            }
        }
    }
}
