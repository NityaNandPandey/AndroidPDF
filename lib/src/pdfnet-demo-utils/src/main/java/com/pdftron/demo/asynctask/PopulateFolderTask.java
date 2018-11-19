//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.asynctask;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;

import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.FileInfo;
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

/**
 * Populates asynchronously the file info list for internal Folder
 */
public class PopulateFolderTask extends CustomAsyncTask<Void, Void, Void> {

    @SuppressWarnings("unused")
    private static final String TAG = PopulateFolderTask.class.getName();

    final private LruCache<String, Boolean> mSdCardFolderCache;
    private File mCurrentFolder;
    private final List<FileInfo> mOriginalFileInfoList;
    private final Object mOriginalFileInfoListLock;
    private final Comparator<FileInfo> mSortMode;
    private boolean mAcceptFiles;
    private boolean mAcceptSubdirectories;
    private boolean mAcceptSdCard;
    private Callback mCallback;

    private Set<String> mSuffixSet = new HashSet<>();
    private boolean mEmulatedExist;
    private File mStorageDirectory;

    /**
     * @param context              The context
     * @param folder               The root folder; null if it is the root of storage
     * @param fileInfoList         The original list of file info
     * @param fileInfoListLock     The lock for the original file info list
     * @param sortMode             The sort mode for comparison
     * @param acceptFiles          False if only folders should be populated; True otherwise
     * @param acceptSubdirectories True if should include sub-directories as hidden; False otherwise
     * @param acceptSdCard         True if SD card folders/files should be populated
     * @param sdCardFolderCache    The SD Card folder cache
     * @param callback             The callback
     */
    public PopulateFolderTask(
        @NonNull Context context,
        @NonNull File folder,
        @NonNull List<FileInfo> fileInfoList,
        @NonNull Object fileInfoListLock,
        Comparator<FileInfo> sortMode,
        boolean acceptFiles,
        boolean acceptSubdirectories,
        boolean acceptSdCard,
        @Nullable LruCache<String, Boolean> sdCardFolderCache,
        Callback callback
    ) {

        super(context);
        mCurrentFolder = folder;
        mOriginalFileInfoList = fileInfoList;
        mOriginalFileInfoListLock = fileInfoListLock;
        mSortMode = sortMode;
        mAcceptFiles = acceptFiles;
        mAcceptSubdirectories = acceptSubdirectories;
        mAcceptSdCard = acceptSdCard;
        mSdCardFolderCache = sdCardFolderCache;
        mCallback = callback;

    }

    @Override
    protected void onPreExecute(
    ) {

        if (mCallback != null) {
            mCallback.onPopulateFolderTaskStarted();
        }

    }

    @Override
    protected Void doInBackground(
        Void... params
    ) {

        if (mCurrentFolder == null || !mCurrentFolder.isDirectory()) {
            return null;
        }

        mSuffixSet.addAll(Arrays.asList(Constants.FILE_NAME_EXTENSIONS_VALID));
        File emulate = new File("/storage/emulated");
        mEmulatedExist = emulate.exists();
        mStorageDirectory = Environment.getExternalStorageDirectory();

        if (Utils.isLollipop()) {
            File storageParent = mStorageDirectory.getParentFile();
            if (storageParent != null && mCurrentFolder.equals(storageParent)) {
                // let's skip the emulated folder to avoid same file access from
                // different paths (i.e. emulated/0 vs. emulated/legacy)
                File folderParent = mCurrentFolder.getParentFile();
                if (folderParent != null && (!folderParent.getAbsolutePath().equalsIgnoreCase("/"))) {
                    mCurrentFolder = folderParent;
                }
            }

            if (mAcceptSdCard && Utils.isLollipop() && mCurrentFolder != null && mSdCardFolderCache != null) {
                synchronized (mSdCardFolderCache) {
                    String path = mCurrentFolder.getAbsolutePath();
                    if (mSdCardFolderCache.get(path) == null) {
                        mSdCardFolderCache.put(path, Utils.isSdCardFile(getContext(), mCurrentFolder));
                    }
                }
            }
        }

        if (isCancelled()) {
            return null;
        }

        List<FileInfo> fileInfoList = new ArrayList<>();

        traverseFiles(mCurrentFolder, fileInfoList, false);

        if (isCancelled()) {
            return null;
        }

        MiscUtils.sortFileInfoList(fileInfoList, mSortMode);

        if (isCancelled()) {
            return null;
        }

        synchronized (mOriginalFileInfoListLock) {
            mOriginalFileInfoList.clear();
            mOriginalFileInfoList.addAll(fileInfoList);
        }

        publishProgress();

        if (!mAcceptSubdirectories) {
            return null;
        }

        // for facilitating recursive search we recursively travers over all
        // subdirectories and add them to the list of files, but hide them so
        // that the file viewer doesn't show them

        ArrayList<FileInfo> hiddenFileList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            if (isCancelled()) {
                return null;
            }

            File file = fileInfo.getFile();
            if (file.isDirectory()) {
                traverseFiles(file, hiddenFileList, true);
            }
        }

        for (FileInfo fileInfo : hiddenFileList) {
            if (isCancelled()) {
                return null;
            }

            fileInfo.setHidden(true);
            fileInfoList.add(fileInfo);
        }

        MiscUtils.sortFileInfoList(fileInfoList, mSortMode);

        if (isCancelled()) {
            return null;
        }

        synchronized (mOriginalFileInfoListLock) {
            mOriginalFileInfoList.clear();
            mOriginalFileInfoList.addAll(fileInfoList);
        }

        return null;

    }

    @Override
    protected void onProgressUpdate(
        Void... values
    ) {

        if (mCallback != null) {
            mCallback.onPopulateFolderTaskProgressUpdated(mCurrentFolder);
        }

    }


    @Override
    protected void onPostExecute(
        Void result
    ) {

        if (mCallback != null) {
            mCallback.onPopulateFolderTaskFinished();
        }

    }

    private void traverseFiles(
        @Nullable File folder,
        @NonNull List<FileInfo> outputList,
        boolean isRecursive
    ) {

        if (folder == null || !folder.isDirectory() || isCancelled()) {
            return;
        }

        List<File> files = new ArrayList<>();
        File[] fileArray;
        boolean shouldCheckParentAsStorage = false;

        // For API19+, we can get the external SD card directory from system API
        // here, we add the internal storage dir and external storage dir
        if (Utils.isLollipop() && folder.getAbsolutePath().equalsIgnoreCase("/storage")) {
            // get internal storage and external SD card directory
            files.add(mStorageDirectory);
            fileArray = Utils.getExternalFilesDirs(getContext(), null);
            shouldCheckParentAsStorage = true;
        } else {
            fileArray = folder.listFiles();
        }

        if (fileArray == null) {
            return;
        }

        for (File file : fileArray) {
            if (isCancelled()) {
                return;
            }

            if (shouldCheckParentAsStorage) {
                boolean canAdd = true;
                while (file != null
                    && file.getParentFile() != null
                    && !file.getParentFile().getAbsolutePath().equalsIgnoreCase("/storage")
                    && !file.getParentFile().getAbsolutePath().equalsIgnoreCase("/")) {
                    file = file.getParentFile();
                    if (file.equals(mStorageDirectory)) {
                        // we want the internal storage dir from system API instead
                        canAdd = false;
                        break;
                    }
                    if (!mAcceptSdCard && Utils.isSdCardFile(getContext(), file)) {
                        canAdd = false;
                        break;
                    }
                }
                if (!canAdd) {
                    continue;
                }
            }

            if (accept(file)) {
                files.add(file);
            }
        }

        for (File file : files) {
            if (isCancelled()) {
                return;
            }

            if (file.isDirectory()) {
                outputList.add(new FileInfo(BaseFileInfo.FILE_TYPE_FOLDER, file));
                if (isRecursive) {
                    traverseFiles(file, outputList, true);
                }
            } else {
                outputList.add(new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file));
            }
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

        if (!mAcceptFiles) {
            return false;
        }

        String name = file.getName();
        String ext = Utils.getExtension(name);
        return mSuffixSet.contains(ext) && file.canRead();

    }

    /**
     * Callback interface invoked regarding {@link PopulateFolderTask}.
     */
    public interface Callback {

        /**
         * Called when the task has started.
         */
        void onPopulateFolderTaskStarted();

        /**
         * Called when the task has updated file info list,
         * after this callback the recursive folders/files will be populated.
         *
         * @param currentFolder The current folder
         */
        void onPopulateFolderTaskProgressUpdated(File currentFolder);

        /**
         * Called when the task has been terminated.
         */
        void onPopulateFolderTaskFinished();

    }
}

