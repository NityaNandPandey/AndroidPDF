//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.model;

import com.pdftron.pdf.model.FileInfo;

import java.io.File;

public class FileHeader extends FileInfo {

    private boolean collapsed = false;

    public FileHeader(int type, File file) {
        super(type, file);
        collapsed = false;
    }

    public boolean getCollapsed(){
        return collapsed;
    }

    public void setCollapsed(boolean isCollapsed){
        collapsed = isCollapsed;
    }
}
