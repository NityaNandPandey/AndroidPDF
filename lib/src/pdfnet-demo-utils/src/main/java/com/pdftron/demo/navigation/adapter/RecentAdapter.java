//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;

import com.pdftron.demo.R;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.FavoriteFilesManager;
import com.pdftron.pdf.utils.FileInfoManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;

import java.io.File;
import java.util.ArrayList;


public class RecentAdapter extends BaseFileAdapter<FileInfo> {

    public RecentAdapter(Context context, ArrayList<FileInfo> objects, Object objectsLock,
                         int spanCount, BaseFileAdapter.AdapterListener adapterListener,
                         ViewHolderBindListener bindListener) {
        super(context, objects, objectsLock, spanCount, adapterListener, bindListener);
    }

    @Override
    public CharSequence getFileDescription(FileInfo file) {
        Context context = getContext();
        if (context == null) {
            return null;
        }

        CharSequence description = null;
        if (getSpanCount() == 0 || Utils.isTablet(context)) {
            if (file.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                SpannableStringBuilder builder = new SpannableStringBuilder();
                // Italicize the leading "External File" substring
                builder.append(getResources().getString(R.string.file_type_external_file));
                builder.setSpan(new StyleSpan(Typeface.ITALIC), 0, builder.length(), 0);

                String documentPath = Utils.getUriDocumentPath(Uri.parse(file.getAbsolutePath()));
                if (!Utils.isNullOrEmpty(documentPath)) {
                    int lastSeparator = documentPath.lastIndexOf(File.separatorChar);
                    int lastColon = documentPath.lastIndexOf(':');

                    if (lastSeparator - 1 >= 0 && lastSeparator > lastColon && lastSeparator + 1 < documentPath.length()) {
                        // There is content before and after this separator, and it appears after the last colon
                        // Truncate at last separator (exclusive)
                        documentPath = documentPath.substring(0, lastSeparator);
                    } else if (lastColon - 1 >= 0 && lastColon + 1 < documentPath.length()) {
                        // There is content before and after this colon-separator
                        // Truncate at last colon (inclusive)
                        documentPath = documentPath.substring(0, lastColon + 1);
                    }

                    builder.append(" ");
                    builder.append(documentPath);
                }
                description = builder;
            } else {
                description = file.getParentDirectoryPath();
            }
        }

        return description;
    }

    @Override
    public int getFileType(FileInfo file) {
        int type = file.getFileType();
        Context context = getContext();
        if (context == null) {
            return type;
        }

        if (type == BaseFileInfo.FILE_TYPE_EXTERNAL) {
            // Check if external file is a directory
            String mimeType = ExternalFileInfo.getUriMimeType(context, file.getAbsolutePath());
            if (!Utils.isNullOrEmpty(mimeType) && DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                type = BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER;
            }
        }
        return type;
    }

    @Override
    public boolean isFavoriteFile(int position, FileInfo fileInfo) {
        Context context = getContext();
        return context != null && getFavoriteFilesManager().containsFile(context, fileInfo);
    }

    protected FileInfoManager getFavoriteFilesManager() {
        return FavoriteFilesManager.getInstance();
    }
}
