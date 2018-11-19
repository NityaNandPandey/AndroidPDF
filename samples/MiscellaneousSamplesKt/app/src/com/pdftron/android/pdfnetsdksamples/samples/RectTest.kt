//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.PageIterator
import com.pdftron.pdf.Rect
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

class RectTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_rect_title)
        setDescription(R.string.sample_rect_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        try
        // Test  - Adjust the position of content within the page.
        {
            mOutputListener!!.println("_______________________________________________")
            mOutputListener!!.println("Opening the input pdf...")

            val input_doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "tiger.pdf")!!.absolutePath)
            input_doc.initSecurityHandler()

            val pg_itr1 = input_doc.pageIterator

            val media_box = pg_itr1.next()!!.getMediaBox()

            media_box.x1 = media_box.x1 - 200    // translate the page 200 units (1 uint = 1/72 inch)
            media_box.x2 = media_box.x2 - 200

            media_box.update()

            input_doc.save(Utils.createExternalFile("tiger_shift.pdf").absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            mFileList.add(File(input_doc.fileName).name)
            input_doc.close()
            mOutputListener!!.println("Done. Result saved in tiger_shift...")
        } catch (e: Exception) {
            mOutputListener!!.println(e.stackTrace)
        }

        for (file in mFileList) {
            addToFileList(file)
        }
        printFooter(outputListener)
    }

    companion object {

        private var mOutputListener: OutputListener? = null

        private val mFileList = ArrayList<String>()
    }

}