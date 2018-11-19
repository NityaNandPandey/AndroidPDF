//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pdftron.common.RecentlyUsedCache;
import com.pdftron.pdf.model.FileInfo;

import java.util.List;

/**
 * Singleton class to manage recent files
 */
public class RecentFilesManager  extends FileInfoManager {

    /**
     * The maximum number of files that can be added to recent list
     */
    public static final int MAX_NUM_RECENT_FILES = 50;

    private static final String KEY_PREFS_RECENT_FILES = "prefs_recent_files";

    protected RecentFilesManager() {
        super(KEY_PREFS_RECENT_FILES, MAX_NUM_RECENT_FILES);
    }

    private static class LazyHolder {
        private static final RecentFilesManager INSTANCE = new RecentFilesManager();
    }

    public static RecentFilesManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    public void clearFiles(@NonNull Context context) {
        super.clearFiles(context);
        RecentlyUsedCache.resetCache();
    }

    @Override
    public boolean removeFile(@Nullable Context context, @Nullable FileInfo fileToRemove) {
        if (context == null || fileToRemove == null) {
            return false;
        }
        if (super.removeFile(context, fileToRemove)) {
            RecentlyUsedCache.removeDocument(fileToRemove.getAbsolutePath());
            return true;
        }
        return false;
    }

    @NonNull
    @Override
    public List<FileInfo> removeFiles(@Nullable Context context, @Nullable List<FileInfo> filesToRemove) {
        List<FileInfo> filesRemoved = super.removeFiles(context, filesToRemove);
        if (context == null) {
            return filesRemoved;
        }
        for (FileInfo fileRemoved : filesRemoved) {
            if (fileRemoved != null) {
                RecentlyUsedCache.removeDocument(fileRemoved.getAbsolutePath());
            }
        }
        return filesRemoved;
    }

    @Override
    public boolean updateFile(@NonNull Context context, @Nullable FileInfo oldFile, @Nullable FileInfo newFile) {
        if (oldFile == null || newFile == null) {
            return false;
        }
        if (super.updateFile(context, oldFile, newFile)) {
            RecentlyUsedCache.renameDocument(oldFile.getAbsolutePath(), newFile.getAbsolutePath());
            return true;
        }
        return false;
    }
}
