//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.utils;

import android.os.FileObserver;

import com.pdftron.demo.navigation.callbacks.FileManagementListener;

/** @hide */
public class RecursiveFileObserver extends FileObserver {

    public static int CHANGES_ONLY = FileObserver.CREATE |
        FileObserver.DELETE | FileObserver.MOVED_FROM | FileObserver.MOVED_TO;
    private static final String TAG = RecursiveFileObserver.class.getName();

    private FileManagementListener listener;

    private String mAbsolutePath;
    @SuppressWarnings("unused")
    public RecursiveFileObserver(String path, FileManagementListener listener) {
        this(path, ALL_EVENTS, listener);
    }

    public RecursiveFileObserver(String path, int mask, FileManagementListener listener) {
        super(path, mask);
        this.listener = listener;
        mAbsolutePath = path;
    }

    @Override
    public void onEvent(int event, String path) {
        if (mAbsolutePath != null && path != null) {
            path = mAbsolutePath.concat("/").concat(path);
            String[] dir = path.split("/");
            if (dir[dir.length - 1].equals("null")) {
                return;
            }
        }
        listener.onFileChanged(path, event);
    }

}
