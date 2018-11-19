//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.support.v4.app.DialogFragment;

/**
 * A base class for all navigation list dialog fragment in {@link com.pdftron.pdf.dialog.BookmarksDialogFragment}.
 */
public class NavigationListDialogFragment extends DialogFragment {

    protected AnalyticsEventListener mAnalyticsEventListener;

    /**
     * Callback interface to be invoked when an analytic event action happens.
     */
    public interface AnalyticsEventListener {
        /**
         * Called when an analytic event action happens
         */
        void onEventAction();
    }

    /**
     * Sets the listener to {@link AnalyticsEventListener}
     *
     * @param listener The listener
     */
    public void setAnalyticsEventListener(AnalyticsEventListener listener) {
        mAnalyticsEventListener = listener;
    }

    /**
     * Should be called when an event happens that should set "noaction=false"
     * when sending event in analytics handler.
     * See {@link com.pdftron.pdf.utils.AnalyticsParam#noActionParam(boolean}}.
     */
    protected void onEventAction() {
        if (mAnalyticsEventListener != null) {
            mAnalyticsEventListener.onEventAction();
        }
    }
}
