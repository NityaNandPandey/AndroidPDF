//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.asynctask;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.pdftron.demo.utils.FileManager;
import com.pdftron.pdf.model.FileInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Updates asynchronously file info list for All Documents after file are
 * deleted/added from/to file info list
 */
public class UpdateAllFilesTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = UpdateAllFilesTask.class.getName();
    private static final boolean DEBUG = false;

    private final List<FileInfo> mOriginalFileInfoList;
    private final Object mOriginalFileInfoListLock;
    private final Object mUpdateFileInfoListLock;
    private final Object mCacheLock;
    private final Comparator<FileInfo> mSortMode;
    private final List<FileInfo> mDeletedFiles;
    private final List<FileInfo> mAddedFiles;
    private Callback mCallback;

    private boolean mIsChanged;

    /**
     * @param fileInfoList           The original list of file info
     * @param fileInfoListLock       The lock for the original file info list
     * @param cacheLock              The cache lock (See {@link com.pdftron.demo.navigation.FileBrowserViewFragment#mCacheLock})
     * @param updateFileInfoListLock The lock for updating file info list
     * @param sortMode               The sort mode for comparison
     * @param callback               The callback
     */
    public UpdateAllFilesTask(
        @NonNull ArrayList<FileInfo> fileInfoList,
        @NonNull Object fileInfoListLock,
        @NonNull Object cacheLock,
        @NonNull Object updateFileInfoListLock,
        Comparator<FileInfo> sortMode,
        List<FileInfo> deletedFiles,
        List<FileInfo> addedFiles,
        Callback callback
    ) {

        if (DEBUG) Log.d(TAG, "constructor");
        mOriginalFileInfoList = fileInfoList;
        mUpdateFileInfoListLock = updateFileInfoListLock;
        mOriginalFileInfoListLock = fileInfoListLock;
        mCacheLock = cacheLock;
        mSortMode = sortMode;
        mDeletedFiles = new ArrayList<>(deletedFiles);
        mAddedFiles = new ArrayList<>(addedFiles);
        mCallback = callback;

    }

    @Override
    protected void onPreExecute(
    ) {

        if (mCallback != null) {
            mCallback.onUpdateAllFilesTaskStarted();
        }

    }

    @Override
    protected Void doInBackground(
        Void... params
    ) {

        synchronized (mUpdateFileInfoListLock) {
            ArrayList<FileInfo> fileInfoList;
            synchronized (mOriginalFileInfoListLock) {
                fileInfoList = new ArrayList<>(mOriginalFileInfoList);
            }

            ArrayList<FileInfo> filesRemoved = new ArrayList<>();
            if (!mDeletedFiles.isEmpty()) {
                for (FileInfo file : mDeletedFiles) {
                    if (isCancelled()) {
                        return null;
                    }
                    if (fileInfoList.contains(file)) {
                        filesRemoved.add(file);
                    }
                }
            }
            fileInfoList.removeAll(filesRemoved);

            if (!mAddedFiles.isEmpty()) {
                // similar to bubble sort, add the new files to the right
                // positions according to sort method
                for (FileInfo newFileInfo : mAddedFiles) {
                    if (isCancelled()) {
                        return null;
                    }
                    for (int i = 0, count = fileInfoList.size(); i < count; ++i) {
                        FileInfo f = fileInfoList.get(i);
                        if (mSortMode.compare(f, newFileInfo) > 0) {
                            fileInfoList.add(i, newFileInfo);
                            break;
                        }
                    }
                }
            }

            mIsChanged = !filesRemoved.isEmpty() || !mAddedFiles.isEmpty();

            if (isCancelled()) {
                return null;
            }

            if (mIsChanged) {
                synchronized (mOriginalFileInfoListLock) {
                    mOriginalFileInfoList.clear();
                    mOriginalFileInfoList.addAll(fileInfoList);
                }

                if (isCancelled()) {
                    return null;
                }

                // save cache
                synchronized (mCacheLock) {
                    FileManager.saveCache(fileInfoList);
                }
            }

            return null;
        }

    }

    @Override
    protected void onPostExecute(
        Void result
    ) {

        if (mCallback != null) {
            mCallback.onUpdateAllFilesTaskFinished(this, mIsChanged);
        }

    }

    /**
     * Callback interface invoked regarding {@link UpdateAllFilesTask}.
     */
    public interface Callback {

        /**
         * Called when the task has started.
         */
        void onUpdateAllFilesTaskStarted();

        /**
         * Called when the task has been terminated.
         *
         * @param task The current task object
         * @param isChanged True if the original file info list has been changed
         */
        void onUpdateAllFilesTaskFinished(UpdateAllFilesTask task, boolean isChanged);

    }
}

