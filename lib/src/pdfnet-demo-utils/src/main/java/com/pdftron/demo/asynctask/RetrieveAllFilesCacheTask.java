//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.asynctask;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.pdftron.demo.utils.FileManager;
import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.pdf.model.FileInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Retrieve asynchronously file info list from cache
 */
public class RetrieveAllFilesCacheTask extends AsyncTask<Void, Void, Void> {

    private final List<FileInfo> mOriginalFileInfoList;
    private final Object mOriginalFileInfoListLock;
    private final Object mCacheLock;
    private Comparator<FileInfo> mSortMode;
    private Callback mCallback;

    /**
     * @param fileInfoList     The original list of file info
     * @param fileInfoListLock The lock for the original file info list
     * @param cacheLock        The cache lock (See {@link com.pdftron.demo.navigation.FileBrowserViewFragment#mCacheLock})
     * @param sortMode         The sort mode for comparison
     * @param callback         The callback
     */
    public RetrieveAllFilesCacheTask(
        @NonNull List<FileInfo> fileInfoList,
        @NonNull Object fileInfoListLock,
        @NonNull Object cacheLock,
        Comparator<FileInfo> sortMode,
        Callback callback
    ) {

        mOriginalFileInfoList = fileInfoList;
        mOriginalFileInfoListLock = fileInfoListLock;
        mCacheLock = cacheLock;
        mSortMode = sortMode;
        mCallback = callback;

    }

    @Override
    protected Void doInBackground(
        Void... params
    ) {

        ArrayList<FileInfo> list;
        synchronized (mCacheLock) {
            list = FileManager.retrieveCache();
        }

        if (isCancelled()) {
            return null;
        }

        if (list != null) {
            MiscUtils.sortFileInfoList(list, mSortMode);
        }

        if (isCancelled()) {
            return null;
        }

        synchronized (mOriginalFileInfoListLock) {
            mOriginalFileInfoList.clear();
            if (list != null) {
                mOriginalFileInfoList.addAll(list);
            }
        }

        return null;

    }

    @Override
    protected void onPostExecute(
        Void result
    ) {

        if (mCallback != null) {
            mCallback.onRetrieveFileInfoListTaskFinished();
        }

    }

    /**
     * Callback interface invoked regarding {@link RetrieveAllFilesCacheTask}.
     */
    public interface Callback {

        /**
         * Called when the task has been terminated.
         */
        void onRetrieveFileInfoListTaskFinished();

    }
}

