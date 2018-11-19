//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.ReflowProcessor;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides convenient methods for interacting with {@link com.pdftron.pdf.controls.ReflowPagerAdapter} class
 * and takes care of throwing an exception if {@link com.pdftron.pdf.controls.ReflowPagerAdapter} is not set up.
 */
public class ReflowControl extends ViewPager implements ReflowPagerAdapter.ReflowPagerAdapterCallback {

    private static final String TAG = ReflowControl.class.getName();
    private static final String THROW_MESSAGE = "No PDF document has been set. Call setup(PDFDoc) or setup(PDFDoc, OnPostProcessColorListener) first.";

    private ReflowPagerAdapter mReflowPagerAdapter;
    Context mContext;

    /**
     * Class constructor
     */
    public ReflowControl(Context context) {
        this(context, null);
    }

    /**
     * Class constructor
     */
    public ReflowControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initializeReflowProcessor();
    }

    /**
     * Setups the reflow control
     * @param pdfDoc The PDF doc
     */
    public void setup(@NonNull PDFDoc pdfDoc) {
        setup(pdfDoc, null);
    }

    /**
     * Setups the reflow control
     *
     * @param pdfDoc The PDF doc
     * @param listener The listener for post processing colors
     */
    public void setup(@NonNull PDFDoc pdfDoc, OnPostProcessColorListener listener) {
        mReflowPagerAdapter = new ReflowPagerAdapter(this, mContext, pdfDoc);
        mReflowPagerAdapter.setListener(this);
        mReflowPagerAdapter.setOnPostProcessColorListener(listener);
        setAdapter(mReflowPagerAdapter);
    }

    /**
     * Checks whether the reflow control is ready
     *
     * @return True if the reflow control is ready
     */
    public boolean isReady() {
        return mReflowPagerAdapter != null;
    }

    /**
     * Notifies that pages are modified
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void notifyPagesModified() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.onPagesModified();
        }
    }

    /**
     * Resets the reflow control
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void reset() throws PDFNetException {
        initializeReflowProcessor();
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Cleans up.
     */
    public void cleanUp() {
        if (mReflowPagerAdapter != null) {
            mReflowPagerAdapter.cleanup();
        }
    }

    private void initializeReflowProcessor() {
        if (!ReflowProcessor.isInitialized()) {
            ReflowProcessor.initialize();
        }
    }

    /**
     * Sets the text size.
     *
     * @param textSizeInPercent The text size. The possible values are
     *                         5, 10, 25, 50, 75, 100, 125, 150, 200, 40, 800, 1600
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void setTextSizeInPercent(int textSizeInPercent) throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setTextSizeInPercent(textSizeInPercent);
        }
    }

    /**
     * Returns the text size.
     *
     * @return The text size ranging from 0 to 100
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public int getTextSizeInPercent() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.getTextSizeInPercent();
        }
    }

    /**
     * Sets the current page.
     *
     * @param pageNum The page number (starts from 1)
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void setCurrentPage(int pageNum) throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setCurrentPage(pageNum);
        }
    }

    /**
     * Gets the current page.
     *
     * @return The page number
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public int getCurrentPage() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.getCurrentPage();
        }
    }

    /**
     * Sets reflow in day mode.
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void setDayMode() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setDayMode();
        }
    }

    /**
     * Sets reflow in night mode.
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void setNightMode() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setNightMode();
        }
    }

    /**
     * Sets reflow in custom color mode.
     *
     * @param backgroundColorMode The background color
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void setCustomColorMode(int backgroundColorMode) throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setCustomColorMode(backgroundColorMode);
        }
    }

    /**
     * Checks whether reflow is in day mode.
     *
     * @return True if reflow is in day mode
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    @SuppressWarnings("unused")
    public boolean isDayMode() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.isDayMode();
        }
    }

    /**
     * Checks whether reflow is in night mode.
     *
     * @return True if reflow is in night mode
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    @SuppressWarnings("unused")
    public boolean isNightMode() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.isNightMode();
        }
    }

    /**
     * Checks whether reflow is in custom color mode.
     *
     * @return True if reflow is in custom color mode
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    @SuppressWarnings("unused")
    public boolean isCustomColorMode() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.isCustomColorMode();
        }
    }

    /**
     * Sets right-to-left mode.
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void setRightToLeftDirection(boolean isRtlMode) throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setRightToLeftDirection(isRtlMode);
        }
    }

    /**
     * Checks whether right-to-left mode is enabled.
     *
     * @return True if right-to-left mode is enabled
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    @SuppressWarnings("unused")
    public boolean isRightToLeftDirection() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.isRightToLeftDirection();
        }
    }

    /**
     * Enables turn page on tap.
     *
     * @param enabled True if should turn page on tap
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void enableTurnPageOnTap(boolean enabled) throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.enableTurnPageOnTap(enabled);
        }
    }

    /**
     * Zooms in.
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void zoomIn() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.zoomIn();
        }
    }

    /**
     * Zooms out.
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void zoomOut() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.zoomOut();
        }
    }

    /**
     * Checks whether an internal link is clicked.
     *
     * @return True if an internal link is clicked
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public boolean isInternalLinkClicked() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            return mReflowPagerAdapter.isInternalLinkClicked();
        }
    }

    /**
     * Resets that an internal link is clicked.
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    public void resetInternalLinkClicked() throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.resetInternalLinkClicked();
        }
    }

    /**
     * Sets the post process color listener
     *
     * @param listener The listener to add
     *
     * @throws PDFNetException if ReflowControl has not been set up.
     * See {@link #setup(PDFDoc)} and {@link #setup(PDFDoc, OnPostProcessColorListener)}.
     */
    @SuppressWarnings("unused")
    public void setOnPostProcessColorListener(OnPostProcessColorListener listener) throws PDFNetException {
        if (mReflowPagerAdapter == null) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            Log.e(TAG, name + ": " + THROW_MESSAGE);
            throw new PDFNetException("", 0, TAG, name, THROW_MESSAGE);
        } else {
            mReflowPagerAdapter.setOnPostProcessColorListener(listener);
        }
    }


    /**
     * Adds a listener that will be invoked by {@link OnReflowTapListener}.
     * <p>Components that add a listener should take care to remove it when finished.
     * Other components that take ownership of a view may call {@link #clearReflowOnTapListeners()}
     * to remove all attached listeners.</p>
     *
     * @param listener listener to add
     */
    public void addReflowOnTapListener(OnReflowTapListener listener) {
        if (mOnTapListeners == null) {
            mOnTapListeners = new ArrayList<>();
        }
        if (!mOnTapListeners.contains(listener)) {
            mOnTapListeners.add(listener);
        }
    }

    /**
     * Removes a listener that was previously added via
     * {@link #addReflowOnTapListener(OnReflowTapListener)}.
     *
     * @param listener listener to remove
     */
    public void removeReflowOnTapListener(OnReflowTapListener listener) {
        if (mOnTapListeners != null) {
            mOnTapListeners.remove(listener);
        }
    }

    /**
     * Remove all listeners that are notified of any callback from OnTapListener.
     */
    public void clearReflowOnTapListeners() {
        if (mOnTapListeners != null) {
            mOnTapListeners.clear();
        }
    }

    /**
     * Handles when a single tap up event happens
     *
     * @param event The motion event
     */
    @Override
    public void onReflowPagerSingleTapUp(MotionEvent event) {
        if (mOnTapListeners != null) {
            for (OnReflowTapListener listener : mOnTapListeners) {
                listener.onReflowSingleTapUp(event);
            }
        }
    }

    /**
     * Callback interface to be invoked to get the processed color
     */
    public interface OnPostProcessColorListener {
        /**
         * The implementation should return the post-processed color.
         *
         * @param cp The original color
         *
         * @return The output color after post processing
         */
        ColorPt getPostProcessedColor(ColorPt cp);
    }

    /**
     * Callback interface to be invoked when a single tap up gesture occurs.
     */
    public interface OnReflowTapListener {

        /**
         * Called when a single tap up gesture occurred.
         *
         * @param event The motion event
         */
        void onReflowSingleTapUp(MotionEvent event);
    }

    private List<OnReflowTapListener> mOnTapListeners;
}
