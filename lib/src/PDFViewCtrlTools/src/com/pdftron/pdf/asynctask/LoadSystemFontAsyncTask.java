//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.asynctask;

import android.content.Context;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFNet;
import com.pdftron.pdf.utils.CustomAsyncTask;

import java.util.ArrayList;

/**
 * An async task for loading system fonts only
 */
public class LoadSystemFontAsyncTask extends CustomAsyncTask<Void, Void, Void> {


    /**
     * A interface for listening finish event
     */
    public interface onFinishListener {
        /**
         * This method is invoked when async task is finished
         */
        void onFinish();
    }

    private ArrayList<onFinishListener> mListeners;


    public LoadSystemFontAsyncTask(Context context) {
        super(context);
    }

    public void addFinishCallback(onFinishListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<>();
        }
        mListeners.add(listener);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if (mListeners != null) {
            mListeners.clear();
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            PDFNet.getSystemFontList();
        } catch (PDFNetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (mListeners != null) {
            for (onFinishListener onFinishListener : mListeners) {
                onFinishListener.onFinish();
            }
            mListeners.clear();
        }
    }

}
