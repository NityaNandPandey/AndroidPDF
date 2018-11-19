//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation.adapter;

import android.content.Context;
import android.os.AsyncTask;

import com.pdftron.demo.model.FileHeader;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.FavoriteFilesManager;
import com.pdftron.pdf.utils.FileInfoManager;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;

import java.util.ArrayList;
import java.util.ListIterator;

public class LocalFileAdapter extends BaseFileAdapter<FileInfo> {
    private static final String TAG = LocalFileAdapter.class.getName();
    private AsyncTask mAddListHeaderAsyncTask = null;
    private OnReloadListener mReloadListener = null;

    public interface OnReloadListener {
        void onReload();
    }

    public LocalFileAdapter(Context context, ArrayList<FileInfo> objects, Object objectsLock,
                            int spanCount,
                            AdapterListener adapterListener, ViewHolderBindListener bindListener) {
        super(context, objects, objectsLock, spanCount, adapterListener, bindListener);

    }

    @Override
    public boolean isHeader(int position) {
        ArrayList<FileInfo> files = getItems();
        if (files == null || position < 0 || position >= files.size()) {
            return false;
        }
        FileInfo fileInfo = files.get(position);
        return fileInfo.isHeader();
    }

    public void removeListHeaders() {
        synchronized (getListLock()) {
            ArrayList<FileInfo> files = getItems();
            if (files == null || files.size() < 1) {
                return;
            }
            // Remove any existing headers
            ListIterator<FileInfo> iterator = files.listIterator();
            while (iterator.hasNext()) {
                FileInfo fileInfo = iterator.next();
                if (fileInfo.isHeader()) {
                    // Remove the header from the list
                    iterator.remove();
                }
            }
        }
    }

    public void addListHeaders() {
        ArrayList<FileInfo> files = getItems();
        if (files == null || files.size() < 1) {
            return;
        }

        ListIterator<FileInfo> iterator = files.listIterator();

        // Determine where section headers should be placed to separate different directories
        int sectionFirstPosition = 0;
        FileHeader currentHeader = null;
        while (iterator.hasNext()) {
            FileInfo fileInfo = iterator.next();
            if (currentHeader == null || !currentHeader.getAbsolutePath().equals(fileInfo.getParentDirectoryPath())) {
                sectionFirstPosition = iterator.previousIndex();
                boolean found = false;
                FileHeader header = null;
                // find existing header in mOriginal headers
                if (mOriginalHeaders.containsKey(fileInfo.getParentDirectoryPath())) {
                    FileHeader mOriHeader = mOriginalHeaders.get(fileInfo.getParentDirectoryPath());
                    if (mOriHeader != null) {
                        header = mOriHeader;
                        String title = header.getAbsolutePath();

                        if (headerChildMap.containsKey(title)) {
                            headerChildMap.get(title).clear();
                        } else {
                            headerChildMap.put(title, new ArrayList<FileInfo>());
                        }
                        found = true;
                    }
                }

                if (!found) {
                    // Add header

                    if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                        header = new FileHeader(BaseFileInfo.FILE_TYPE_FOLDER, fileInfo.getFile().getParentFile());
                    } else {
                        if (mReloadListener != null) {
                            mReloadListener.onReload();
                        }
                        return;
                    }
                    mOriginalHeaders.put(header.getAbsolutePath(), header);
                }
                currentHeader = header;
                header.setHeader(true);
                header.setSectionFirstPos(sectionFirstPosition);

                iterator.previous(); // Move backwards one item
                iterator.add(header);
                fileInfo = iterator.next(); // Move forward again
            }
            if (currentHeader.getCollapsed()) {
                String title = currentHeader.getAbsolutePath();
                headerChildMap.get(title).add(fileInfo);
                iterator.remove();
            } else {
                fileInfo.setHeader(false);
                fileInfo.setSectionFirstPos(sectionFirstPosition);
            }
        }
    }

    public void updateListHeaders() {
        // Remove and then re-add headers
        removeListHeaders();
        addListHeaders();
    }

    public boolean isAddingHeaders() {
        if (mAddListHeaderAsyncTask == null || mAddListHeaderAsyncTask.isCancelled()
            || mAddListHeaderAsyncTask.getStatus() == AsyncTask.Status.FINISHED) {
            return false;
        }
        if (mAddListHeaderAsyncTask.getStatus() == AsyncTask.Status.PENDING
            || mAddListHeaderAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
            return true;
        }
        return false;
    }

    protected FileInfoManager getFileInfoManager() {
        return FavoriteFilesManager.getInstance();
    }

    @Override
    public boolean isFavoriteFile(int position, FileInfo fileInfo) {
        Context context = getContext();
        return context != null && getFileInfoManager().containsFile(context, fileInfo);
    }

    public void setOnReloadListener(OnReloadListener listener) {
        mReloadListener = listener;
    }
}
