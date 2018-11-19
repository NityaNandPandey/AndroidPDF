//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples;

public interface OutputListener {
    void print(String output);
    void print();
    void println(String outpout);
    void println();
    void println(StackTraceElement[] stackTrace);
    
}
