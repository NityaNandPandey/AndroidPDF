//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation.callbacks;

import android.net.Uri;

import java.io.File;

public interface FilePickerCallbacks {
    /**
     * A file (item) was selected (single tap) from the list.
     */
    void onFileSelected(File file, String password);
    void onFolderSelected(String absolutePath);
    void onExternalFileSelected(String fileUri, String password);
    void onExternalFolderSelected(String fileUri);
    void onEditUriSelected(String fileUri);
    void onOfficeUriSelected(Uri fileUri);
}
