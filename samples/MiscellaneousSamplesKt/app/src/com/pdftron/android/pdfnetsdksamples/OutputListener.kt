//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples

interface OutputListener {
    fun print(output: String?)
    fun print()
    fun println(outpout: String?)
    fun println()
    fun println(stackTrace: Array<StackTraceElement>)

}
