//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.asynctask;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.TextExtractor;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;

/**
 * A class that asynchronously extracts the text of the current page
 */
public class GetTextInPageTask extends AsyncTask<Void, Void, String> {

    private TextExtractor mTextExtractor = new TextExtractor();
    private Callback mCallback;

    /**
     * Callback interface invoked when text in the current page is prepared.
     */
    public interface Callback {
        /**
         * Called when the text in the current page has been prepared.
         *
         * @param text The prepared text
         */
        void getText(String text);
    }

    /**
     * Class constructor
     *
     * @param pdfViewCtrl The {@link com.pdftron.pdf.PDFViewCtrl}
     */
    public GetTextInPageTask(@NonNull PDFViewCtrl pdfViewCtrl) {
        boolean shouldUnlockRead = false;
        try {
            PDFDoc pdfDoc = pdfViewCtrl.getDoc();
            if (pdfDoc != null) {
                pdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                mTextExtractor.begin(pdfDoc.getPage(pdfViewCtrl.getCurrentPage()));
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                pdfViewCtrl.docUnlockRead();
            }
        }
    }

    /**
     * Sets the callback listener.
     *
     * Sets the callback to null when the task is cancelled.
     *
     * @param callback The callback when the task is finished
     */
    public void setCallback(@Nullable Callback callback) {
        mCallback = callback;
    }

    /**
     * The overloaded implementation of {@link android.os.AsyncTask#doInBackground(Object[])}.
     **/
    @Override
    protected String doInBackground(Void... voids) {
        try {
            return mTextExtractor.getAsText();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return null;
        }
    }

    /**
     * The overloaded implementation of {@link android.os.AsyncTask#onPostExecute(Object)}.
     **/
    @Override
    protected void onPostExecute(String text) {
        super.onPostExecute(text);
        if (mCallback != null) {
            mCallback.getText(text);
        }
    }
}
