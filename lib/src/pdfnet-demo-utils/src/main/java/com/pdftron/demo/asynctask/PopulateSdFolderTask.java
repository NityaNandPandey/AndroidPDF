//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.asynctask;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Populates asynchronously the file info list for SD Card
 */
public class PopulateSdFolderTask extends CustomAsyncTask<Void, PublishState, Void> {

    public static final String TAG = PopulateSdFolderTask.class.getName();
    private static final boolean DEBUG = false;

    private final List<ExternalFileInfo> mOriginalFileInfoList;
    private final Object mOriginalFileInfoListLock;
    private List<ExternalFileInfo> mRootList = new ArrayList<>();
    private boolean mForceReloadRoots;
    private ExternalFileInfo mCurrentRoot;
    private ExternalFileInfo mCurrentFolder;
    private Comparator<ExternalFileInfo> mSortMode;
    private String mSavedFolderUri;
    private String mSavedLeafUri;
    private boolean mAcceptFiles;
    private boolean mAcceptSubdirectories;
    private Callback mCallback;

    private ExternalFileInfo mSavedRoot;
    private ExternalFileInfo mSavedFolder;
    private ExternalFileInfo mSavedLeaf;

    /**
     * @param context              The context
     * @param fileInfoList         The original list of external file info
     * @param fileInfoListLock     The lock for the original external file info list
     * @param rootList             The root directories
     * @param currentRoot          The current root
     * @param currentFolder        The current folder
     * @param sortMode             The sort mode for comparison
     * @param forceReloadRoots     True if should reload root list
     * @param savedFolderUri       The saved folder URI; null if no folder has been saved
     * @param savedLeafUri         The saved leaf URI; null if no leaf has been saved
     * @param acceptFiles          False if only folders should be populated; True otherwise
     * @param acceptSubdirectories True if should include sub-directories as hidden; False otherwise
     * @param callback             The callback
     */
    public PopulateSdFolderTask(
        @NonNull Context context,
        @NonNull ArrayList<ExternalFileInfo> fileInfoList,
        @NonNull Object fileInfoListLock,
        @Nullable List<ExternalFileInfo> rootList,
        boolean forceReloadRoots,
        @Nullable ExternalFileInfo currentRoot,
        @Nullable ExternalFileInfo currentFolder,
        Comparator<ExternalFileInfo> sortMode,
        @Nullable String savedFolderUri,
        @Nullable String savedLeafUri,
        boolean acceptFiles,
        boolean acceptSubdirectories,
        Callback callback
    ) {

        super(context);
        mOriginalFileInfoList = fileInfoList;
        mOriginalFileInfoListLock = fileInfoListLock;
        if (rootList != null) {
            mRootList.addAll(rootList);
        }
        mForceReloadRoots = forceReloadRoots;
        mCurrentRoot = currentRoot == null ? null : currentRoot.clone();
        mCurrentFolder = currentFolder == null ? null : currentFolder.clone();
        mSortMode = sortMode;
        mSavedFolderUri = savedFolderUri;
        mSavedLeafUri = savedLeafUri;
        mAcceptFiles = acceptFiles;
        mAcceptSubdirectories = acceptSubdirectories;
        mCallback = callback;

    }

    @Override
    protected void onPreExecute(
    ) {

        if (mCallback != null) {
            mCallback.onPopulateSdFilesTaskStarted();
        }

    }

    @Override
    protected Void doInBackground(
        Void... params
    ) {

        if (!Utils.isLollipop()) {
            return null;
        }

        if (mForceReloadRoots || mRootList.isEmpty()) {
            createRoots();
        }

        if (isCancelled()) {
            return null;
        }

        // Build the specified folders now that the roots are loaded
        if (!Utils.isNullOrEmpty(mSavedFolderUri) || !Utils.isNullOrEmpty(mSavedLeafUri)) {
            buildSavedFolder();
            publishProgress(PublishState.SAVED_FOLDER_BUILT);

            // if loaded from specified folder, the list of files should be updated
            if (mSavedRoot != null && mSavedFolder != null) {
                mCurrentRoot = mSavedRoot;
                mCurrentFolder = mSavedFolder;
            }
        }

        if (isCancelled()) {
            return null;
        }

        List<ExternalFileInfo> fileInfoList = new ArrayList<>();

        List<ExternalFileInfo> currentFolders = mCurrentFolder != null
            ? mCurrentFolder.listFiles() : mRootList;
        for (ExternalFileInfo file : currentFolders) {
            if (accept(file)) {
                fileInfoList.add(file);
            }
        }
        Collections.sort(fileInfoList, mSortMode);

        if (isCancelled()) {
            return null;
        }

        synchronized (mOriginalFileInfoListLock) {
            mOriginalFileInfoList.clear();
            mOriginalFileInfoList.addAll(fileInfoList);
        }

        publishProgress(PublishState.FILE_LIST_CREATED);

        if (!mAcceptSubdirectories) {
            return null;
        }

        // for facilitating recursive search we recursively travers over all
        // subdirectories and add them to the document list, but hide them so
        // that the file viewer doesn't show them

        ArrayList<ExternalFileInfo> hiddenFileList = new ArrayList<>();
        for (ExternalFileInfo fileInfo : fileInfoList) {
            if (isCancelled()) {
                return null;
            }

            if (fileInfo.isDirectory()) {
                traverseFiles(fileInfo, hiddenFileList);
            }
        }

        for (ExternalFileInfo fileInfo : hiddenFileList) {
            fileInfo.setHidden(true);
            fileInfoList.add(fileInfo);
        }

        if (isCancelled()) {
            return null;
        }

        Collections.sort(fileInfoList, mSortMode);

        synchronized (mOriginalFileInfoListLock) {
            mOriginalFileInfoList.clear();
            mOriginalFileInfoList.addAll(fileInfoList);
        }

        return null;

    }

    @Override
    protected void onProgressUpdate(
        PublishState... values
    ) {

        if (mCallback != null) {
            if (values[0] == PublishState.FILE_LIST_CREATED) {
                mCallback.onPopulateSdFilesTaskProgressUpdated(mRootList);
            } else if (values[0] == PublishState.SAVED_FOLDER_BUILT) {
                mCallback.onPopulateSdFilesTaskProgressUpdated(mSavedRoot, mSavedFolder, mSavedLeaf);
            }
        }

    }

    @Override
    protected void onPostExecute(
        Void result
    ) {

        if (mCallback != null) {
            mCallback.onPopulateSdFilesTaskFinished();
        }

    }

    /**
     * Creates all root levels. If current root is not available any more, remove it.
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void createRoots(
    ) {

        // generate list of root level URIs
        mRootList.clear();
        ContentResolver cr = Utils.getContentResolver(getContext());
        if (cr == null) {
            return;
        }
        List<UriPermission> permissionList = cr.getPersistedUriPermissions();
        for (UriPermission uriPermission : permissionList) {
            if (Utils.getUriType(uriPermission.getUri()) == Utils.URI_TYPE_TREE) {
                Context context = getContext();
                if (context == null) {
                    return;
                }
                ExternalFileInfo rootFile = new ExternalFileInfo(context, null, uriPermission.getUri());
                if (!rootFile.exists()) {
                    // document has been moved or deleted, so release its permissions
                    if (DEBUG) Log.d(TAG, "File does not exist: " + uriPermission.getUri());
                    cr.releasePersistableUriPermission(uriPermission.getUri(),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } else {
                    mRootList.add(rootFile);
                }
            }
        }

        if (isCancelled()) {
            return;
        }

        // check if the current root is still available, otherwise remove it
        if (mCurrentRoot != null) {
            boolean found = false;
            for (ExternalFileInfo file : mRootList) {
                if (file.getUri().getPath().equals(mCurrentRoot.getUri().getPath())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // current root is no longer available
                if (mCallback != null) {
                    mCallback.onCurrentRootRemoved();
                }
            }
        }

    }

    private void traverseFiles(
        @Nullable ExternalFileInfo currentFolder,
        @NonNull List<ExternalFileInfo> outputList
    ) {

        if (currentFolder == null) {
            return;
        }

        // beneath the current level folder
        for (ExternalFileInfo file : currentFolder.listFiles()) {
            // if task was cancelled, return as soon as possible
            if (isCancelled()) {
                return;
            }
            if (accept(file)) {
                outputList.add(file);
                if (file.isDirectory()) {
                    traverseFiles(file, outputList);
                }
            }
        }

    }

    private boolean accept(
        @Nullable ExternalFileInfo file
    ) {

        return file != null &&
            file.exists() &&
            !file.isHidden() &&
            !file.getFileName().startsWith(".") &&
            (
                file.isDirectory() ||
                    (mAcceptFiles && Utils.isMimeTypeHandled(file.getType().toLowerCase()))
            );

    }

    private void buildSavedFolder(
    ) {

        Uri folderUri = null;
        Uri leafUri = null;
        String folderTreePath = "";
        String leafTreePath = "";
        if (!Utils.isNullOrEmpty(mSavedFolderUri)) {
            folderUri = Uri.parse(mSavedFolderUri);
            folderTreePath = Utils.getUriTreePath(folderUri);
        }
        if (!Utils.isNullOrEmpty(mSavedLeafUri)) {
            leafUri = Uri.parse(mSavedLeafUri);
            leafTreePath = Utils.getUriTreePath(leafUri);
        }

        // ensure that at least one is non-empty
        if (Utils.isNullOrEmpty(folderTreePath) && Utils.isNullOrEmpty(leafTreePath)) {
            // no treePath is available, won't be able to determine root
            return;
        }

        String savedTreePath;
        if (!Utils.isNullOrEmpty(folderTreePath) && !Utils.isNullOrEmpty(leafTreePath)) {
            // TreePaths must be the same
            if (!folderTreePath.equals(leafTreePath)) {
                // error: treePaths do not match
                return;
            }
            savedTreePath = folderTreePath;
        } else if (!Utils.isNullOrEmpty(folderTreePath)) {
            savedTreePath = folderTreePath;
        } else {
            savedTreePath = leafTreePath;
        }

        // find the root from the list
        for (ExternalFileInfo root : mRootList) {
            String treePath = root.getTreePath();
            if (treePath.equals(savedTreePath)) {
                mSavedRoot = root;
                break;
            }
        }
        if (mSavedRoot == null) {
            // root not found (might not exist)
            return;
        }

        // build the saved current folder
        if (folderUri != null) {
            mSavedFolder = mSavedRoot.buildTree(folderUri);
        }

        // build the saved leaf file, for the breadcrumb bar
        if (leafUri != null) {
            if (mSavedFolder != null) {
                if (folderUri != null && folderUri.equals(leafUri)) {
                    // the folder and leaf are the same
                    mSavedLeaf = mSavedFolder;
                } else {
                    // start building the leaf file from the already-built folder
                    mSavedLeaf = mSavedFolder.buildTree(leafUri);
                }
            } else {
                // build from the root
                mSavedLeaf = mSavedRoot.buildTree(leafUri);
            }
        }

    }

    /**
     * Callback interface invoked regarding {@link PopulateSdFolderTask}.
     */
    public interface Callback {

        /**
         * Called when the task has started.
         */
        void onPopulateSdFilesTaskStarted();

        /**
         * Called when the current root is no longer available
         */
        void onCurrentRootRemoved();

        /**
         * Called when the task has built special saved folder.
         *
         * @param savedRoot   The saved root
         * @param savedFolder The saved folder
         * @param savedLeaf   The saved leaf
         */
        void onPopulateSdFilesTaskProgressUpdated(ExternalFileInfo savedRoot,
                                                  ExternalFileInfo savedFolder,
                                                  ExternalFileInfo savedLeaf);

        /**
         * Called when the task has updated file info list.
         *
         * @param rootList The populated root list
         */
        void onPopulateSdFilesTaskProgressUpdated(List<ExternalFileInfo> rootList);

        /**
         * Called when the task has been terminated.
         */
        void onPopulateSdFilesTaskFinished();

    }
}

enum PublishState {
    FILE_LIST_CREATED,
    SAVED_FOLDER_BUILT
}

