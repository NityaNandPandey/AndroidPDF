//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.asynctask;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.pdf.model.FileInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sort asynchronously file info list
 */
public class SortAllFilesTask extends AsyncTask<Void, Void, Void> {

    private final List<FileInfo> mOriginalFileInfoList;
    private final Object mOriginalFileInfoListLock;
    private final Comparator<FileInfo> mSortMode;
    private Callback mCallback;

    /**
     * @param fileInfoList     The original list of file info
     * @param fileInfoListLock The lock for the original file info list
     * @param sortMode         The sort mode for comparison
     * @param callback         The callback
     */
    public SortAllFilesTask(
        @NonNull List<FileInfo> fileInfoList,
        @NonNull Object fileInfoListLock,
        Comparator<FileInfo> sortMode,
        Callback callback
    ) {

        mOriginalFileInfoList = fileInfoList;
        mOriginalFileInfoListLock = fileInfoListLock;
        mSortMode = sortMode;
        mCallback = callback;

    }

    @Override
    protected void onPreExecute(
    ) {

        if (mCallback != null) {
            mCallback.onSortAllFilesTaskStarted();
        }

    }

    @Override
    protected Void doInBackground(
        Void... params
    ) {

        ArrayList<FileInfo> list;
        synchronized (mOriginalFileInfoListLock) {
            list = new ArrayList<>(mOriginalFileInfoList);
        }

        if (isCancelled()) {
            return null;
        }

        MiscUtils.sortFileInfoList(list, mSortMode);

        if (isCancelled()) {
            return null;
        }

        synchronized (mOriginalFileInfoListLock) {
            mOriginalFileInfoList.clear();
            mOriginalFileInfoList.addAll(list);
        }

        return null;
    }

    @Override
    protected void onPostExecute(
        Void result
    ) {

        if (mCallback != null) {
            mCallback.onSortAllFilesTaskFinished();
        }

    }

    /**
     * Callback interface invoked regarding {@link SortAllFilesTask}.
     */
    public interface Callback {

        /**
         * Called when the task has started.
         */
        void onSortAllFilesTaskStarted();

        /**
         * Called when the task has been terminated.
         */
        void onSortAllFilesTaskFinished();

    }

}

