//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.asynctask;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Constants;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.pdftron.pdf.model.BaseFileInfo.FILE_TYPE_FOLDER;

/**
 * Populates asynchronously the file info list for All Documents
 */
public class PopulateAllFilesTask extends CustomAsyncTask<Void, Void, Void> {

    private static final String TAG = PopulateAllFilesTask.class.getName();
    private static final boolean DEBUG = false;

    private File mRootFolder;
    private final List<FileInfo> mOriginalFileInfoList;
    private final Object mOriginalFileInfoListLock;
    private final Comparator<FileInfo> mSortMode;
    private boolean mUpdateProgress;
    private Callback mCallback;

    private List<FileInfo> mFileInfoList = new ArrayList<>();
    private Set<String> mSuffixSet = new HashSet<>();
    private boolean mEmulatedExist;

    /**
     * @param context          The context
     * @param rootFolder       The root folder; null if it is the root of storage
     * @param fileInfoList     The original list of file info
     * @param fileInfoListLock The lock for the original file info list
     * @param sortMode         The sort mode for comparison
     * @param updateProgress   Whether update progress or not
     * @param callback         The callback
     */
    public PopulateAllFilesTask(
        @NonNull Context context,
        @Nullable File rootFolder,
        @NonNull List<FileInfo> fileInfoList,
        @NonNull Object fileInfoListLock,
        Comparator<FileInfo> sortMode,
        boolean updateProgress,
        Callback callback
    ) {

        super(context);
        if (DEBUG) Log.d(TAG, "constructor");
        mRootFolder = rootFolder;
        mOriginalFileInfoList = fileInfoList;
        mOriginalFileInfoListLock = fileInfoListLock;
        mSortMode = sortMode;
        mUpdateProgress = updateProgress;
        mCallback = callback;

    }

    @Override
    protected void onPreExecute(
    ) {

        if (DEBUG) Log.d(TAG, "start populate file info list task");

        if (mCallback != null) {
            mCallback.onPopulateAllFilesTaskStarted();
        }

    }

    @Override
    protected Void doInBackground(
        Void... params
    ) {

        mSuffixSet.addAll(Arrays.asList(Constants.FILE_NAME_EXTENSIONS_VALID));
        File emulate = new File("/storage/emulated");
        mEmulatedExist = emulate.exists();

        if (DEBUG) Log.d(TAG, "get root folders");
        List<FileInfo> rootFolders = getRootFolders();
        for (FileInfo folderInfo : rootFolders) {
            if (isCancelled()) {
                return null;
            }
            File folder = folderInfo.getFile();
            if (DEBUG) Log.d(TAG, "traverse " + folder.getName());
            traverseFiles(folder);
        }

        if (isCancelled()) {
            return null;
        }

        MiscUtils.sortFileInfoList(mFileInfoList, mSortMode);

        synchronized (mOriginalFileInfoListLock) {
            mOriginalFileInfoList.clear();
            mOriginalFileInfoList.addAll(mFileInfoList);
        }

        return null;

    }

    @Override
    protected void onProgressUpdate(
        Void... values
    ) {

        if (mCallback != null) {
            mCallback.onPopulateAllFilesTaskProgressUpdated();
        }

    }


    @Override
    protected void onPostExecute(
        Void result
    ) {

        if (mCallback != null) {
            mCallback.onPopulateAllFilesTaskFinished();
        }

    }

    /**
     * @return The internal storage and external SD card directory
     */
    private List<FileInfo> getRootFolders(
    ) {

        List<FileInfo> rootFolders = new ArrayList<>();
        File storageDirectory = Environment.getExternalStorageDirectory();
        if (mRootFolder == null) {
            // For API19+, we can get the external SD card directory from system API
            // here, we add the internal storage dir and external storage dir
            if (Utils.isLollipop()) {
                rootFolders.add(new FileInfo(FILE_TYPE_FOLDER, storageDirectory));
                File[] rootDirs;
                {
                    Context context = getContext();
                    if (context == null) {
                        return rootFolders;
                    }
                    rootDirs = context.getExternalFilesDirs(null);
                }
                for (File file : rootDirs) {
                    boolean canAdd = true;
                    while (file != null) {
                        file = file.getParentFile();
                        if (file == null) {
                            break;
                        }
                        String path = file.getAbsolutePath();
                        if (path.equalsIgnoreCase("/storage")
                            || path.equalsIgnoreCase("/")) {
                            break;
                        }
                        if (file.equals(storageDirectory)) {
                            // we want the internal storage dir from system API instead
                            canAdd = false;
                            break;
                        }
                    }

                    if (canAdd) {
                        rootFolders.add(new FileInfo(FILE_TYPE_FOLDER, file));
                    }
                }
            } else {
                File rootDir = storageDirectory;
                while (rootDir != null && rootDir.getParentFile() != null && !rootDir.getParentFile().getAbsolutePath().equalsIgnoreCase("/")) {
                    rootDir = rootDir.getParentFile();
                }
                rootFolders.add(new FileInfo(FILE_TYPE_FOLDER, rootDir));
            }
        } else {
            rootFolders.add(new FileInfo(FILE_TYPE_FOLDER, mRootFolder));
        }

        MiscUtils.sortFileInfoList(rootFolders, mSortMode);

        return rootFolders;

    }

    private void traverseFiles(
        @Nullable File folder
    ) {

        if (folder == null || !folder.isDirectory() || isCancelled()) {
            return;
        }

        try {
            File[] files = folder.listFiles();
            if (files != null) {
                ArrayList<FileInfo> folderInfoList = new ArrayList<>();
                ArrayList<FileInfo> fileInfoList = new ArrayList<>();
                for (File file : files) {
                    if (accept(file)) {
                        if (file.isDirectory()) {
                            folderInfoList.add(new FileInfo(FILE_TYPE_FOLDER, file));
                        } else {
                            fileInfoList.add(new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file));
                        }
                    }
                }

                if (isCancelled()) {
                    return;
                }

                if (!fileInfoList.isEmpty()) {
                    MiscUtils.sortFileInfoList(fileInfoList, mSortMode);
                    mFileInfoList.addAll(fileInfoList);
                    if (mUpdateProgress) {
                        synchronized (mOriginalFileInfoListLock) {
                            mOriginalFileInfoList.clear();
                            mOriginalFileInfoList.addAll(mFileInfoList);
                        }
                        publishProgress();
                    }
                }

                MiscUtils.sortFileInfoList(folderInfoList, mSortMode);
                for (FileInfo folderInfo : folderInfoList) {
                    traverseFiles(folderInfo.getFile());
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

    }

    private boolean accept(
        @Nullable File file
    ) {

        if (file == null || file.isHidden()) {
            return false;
        }

        if (!Utils.isLollipop()) {
            String path = file.getAbsolutePath();
            // workaround issue where same file shows up multiple times
            if (path.contains("/emulated/legacy/")
                || (mEmulatedExist && path.contains("/storage/sdcard0/"))) {
                return false;
            }
        }

        if (file.isDirectory()) {
            return true;
        }

        String name = file.getName();
        String ext = Utils.getExtension(name);
        return mSuffixSet.contains(ext) && file.canRead();

    }

    /**
     * Callback interface invoked regarding {@link PopulateAllFilesTask}.
     */
    public interface Callback {

        /**
         * Called when the task has started.
         */
        void onPopulateAllFilesTaskStarted();

        /**
         * Called when the task has updated file info list.
         */
        void onPopulateAllFilesTaskProgressUpdated();

        /**
         * Called when the task has been terminated.
         */
        void onPopulateAllFilesTaskFinished();

    }
}

