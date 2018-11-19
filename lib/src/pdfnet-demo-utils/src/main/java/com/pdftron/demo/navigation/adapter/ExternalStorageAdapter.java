//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation.adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.SparseArray;

import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.FavoriteFilesManager;
import com.pdftron.pdf.utils.FileInfoManager;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;

import java.util.ArrayList;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ExternalStorageAdapter extends BaseFileAdapter<ExternalFileInfo> {

    private SparseArray<FileInfo> mFileInfoList;

    public ExternalStorageAdapter(Context context, ArrayList<ExternalFileInfo> objects, Object objectsLock,
                                  int spanCount,
                                  AdapterListener adapterListener, ViewHolderBindListener bindListener) {
        super(context, objects, objectsLock, spanCount, adapterListener, bindListener);

        mFileInfoList = new SparseArray<>();
        setShowInfoButton(true);
        setShowFavoriteIndicator(true);
    }

    @Override
    public int getFileType(ExternalFileInfo file) {
        int type = file.getFileType();
        if (type == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER && file.getParent() == null) {
            type = BaseFileInfo.FILE_TYPE_EXTERNAL_ROOT;
        }
        return type;
    }

    @Override
    public CharSequence getFileDescription(ExternalFileInfo file) {
        String description;

        if (isInSearchMode()) {
            description = file.getParentRelativePath() + "/" + file.getFileName();
        } else {
            description = file.getModifiedDate();
        }
        return description;
    }

    protected FileInfoManager getFileInfoManager() {
        return FavoriteFilesManager.getInstance();
    }

    @Override
    protected boolean isFavoriteFile(int position, ExternalFileInfo file) {
        // Cache the FileInfo objects for re-use later
        FileInfo tempInfo = mFileInfoList.get(position);
        if (tempInfo != null && !tempInfo.getAbsolutePath().equals(file.getAbsolutePath())) {
            // Name or Uri has changed
            // Indicate that tempInfo should be created again
            tempInfo = null;
        }
        // Create FileInfo if necessary
        if (tempInfo == null) {
            tempInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, file.getAbsolutePath(), file.getFileName(), false, 1);
            mFileInfoList.put(position, tempInfo);
        }

       Context context = getContext();
       return context != null && getFileInfoManager().containsFile(context, tempInfo);
    }
}
