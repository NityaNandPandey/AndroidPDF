//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.asynctask;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.google.gson.reflect.TypeToken;
import com.pdftron.demo.model.FileHeader;
import com.pdftron.demo.utils.CacheUtils;

import java.util.HashMap;

/**
 * Retrieve asynchronously file header list from cache
 */
public class RetrieveAllFilesHeaderCacheTask extends AsyncTask<Void, Void, HashMap<String, FileHeader>> {

    private final Object mCacheLock;
    private Callback mCallback;

    /**
     * @param cacheLock The cache lock (See {@link com.pdftron.demo.navigation.FileBrowserViewFragment#mCacheLock})
     * @param callback  The callback
     */
    public RetrieveAllFilesHeaderCacheTask(
        @NonNull Object cacheLock,
        Callback callback
    ) {

        mCacheLock = cacheLock;
        mCallback = callback;

    }

    @Override
    protected HashMap<String, FileHeader> doInBackground(
        Void... params
    ) {

        synchronized (mCacheLock) {
            return CacheUtils.readObjectFile(CacheUtils.CACHE_HEADER_LIST_OBJECT,
                new TypeToken<HashMap<String, FileHeader>>() {
                }.getType());
        }

    }

    @Override
    protected void onPostExecute(
        HashMap<String, FileHeader> result
    ) {

        if (mCallback != null) {
            mCallback.onRetrieveAllFilesHeaderCacheTaskFinished(result);
        }

    }

    /**
     * Callback interface invoked regarding {@link RetrieveAllFilesHeaderCacheTask}.
     */
    public interface Callback {

        /**
         * Called when the task has been terminated.
         */
        void onRetrieveAllFilesHeaderCacheTaskFinished(HashMap<String, FileHeader> result);

    }
}

