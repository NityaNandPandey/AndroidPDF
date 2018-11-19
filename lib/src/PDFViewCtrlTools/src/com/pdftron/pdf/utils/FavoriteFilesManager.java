//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

/**
 * Singleton class to manage favorite files
 */
public class FavoriteFilesManager extends FileInfoManager {

    private static final int MAX_NUM_FAVORITE_FILES = 50;

    private static final String KEY_PREFS_FAVORITE_FILES = "prefs_favorite_files";

    protected FavoriteFilesManager() {
        super(KEY_PREFS_FAVORITE_FILES, MAX_NUM_FAVORITE_FILES);
    }

    private static class LazyHolder {
        private static final FavoriteFilesManager INSTANCE = new FavoriteFilesManager();
    }

    public static FavoriteFilesManager getInstance() {
        return LazyHolder.INSTANCE;
    }
}
