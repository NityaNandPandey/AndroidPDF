//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.asynctask;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.pdftron.demo.model.FileHeader;
import com.pdftron.demo.utils.CacheUtils;
import com.pdftron.demo.utils.FileManager;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Saves asynchronously file info list and file header list into cache
 */
public class SaveFileListCacheTask extends AsyncTask<Void, Void, Void> {

    private final List<FileInfo> mOriginalFileInfoList;
    private final Object mOriginalFileInfoListLock;
    private final Object mCacheLock;
    private final HashMap<String, FileHeader> mFileHeaders;

    /**
     * @param fileInfoList     The original list of file info
     * @param fileInfoListLock The lock for the original file info list
     * @param cacheLock        The cache lock (See {@link com.pdftron.demo.navigation.FileBrowserViewFragment#mCacheLock})
     * @param fileHeaders      The file header list
     */
    public SaveFileListCacheTask(
        @NonNull List<FileInfo> fileInfoList,
        @NonNull Object fileInfoListLock,
        @NonNull Object cacheLock,
        HashMap<String, FileHeader> fileHeaders
    ) {

        mOriginalFileInfoList = fileInfoList;
        mOriginalFileInfoListLock = fileInfoListLock;
        mCacheLock = cacheLock;
        // there is no lock for file headers, so we just copy items
        mFileHeaders = new HashMap<>(fileHeaders);

    }

    @Override
    protected Void doInBackground(
        Void... params
    ) {

        ArrayList<FileInfo> fileInfoList;
        synchronized (mOriginalFileInfoListLock) {
            fileInfoList = new ArrayList<>(mOriginalFileInfoList);
        }

        if (fileInfoList.size() == 0 || isCancelled()) {
            return null;
        }

        // save file list
        try {
            synchronized (mCacheLock) {
                FileManager.saveCache(fileInfoList);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        if (isCancelled()) {
            return null;
        }

        // save header list
        if (mFileHeaders != null && !mFileHeaders.isEmpty()) {
            synchronized (mCacheLock) {
                CacheUtils.writeObjectFile(CacheUtils.CACHE_HEADER_LIST_OBJECT, mFileHeaders);
            }
        }

        return null;

    }

}

