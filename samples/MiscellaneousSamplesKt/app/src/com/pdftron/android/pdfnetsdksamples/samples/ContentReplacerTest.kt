//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.pdf.ContentReplacer
import com.pdftron.pdf.Image
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.Rect
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

class ContentReplacerTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_contentreplacer_title)
        setDescription(R.string.sample_contentreplacer_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        // The first step in every application using PDFNet is to initialize the
        // library and set the path to common PDF resources. The library is usually
        // initialized only once, but calling Initialize() multiple times is also fine.

        //--------------------------------------------------------------------------------
        // Example 1) Update a business card template with personalized info

        try {
            mOutputListener!!.println("Opening the input file...")

            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "BusinessCardTemplate.pdf")!!.absolutePath)
            doc.initSecurityHandler()

            val replacer = ContentReplacer()
            val page = doc.getPage(1)
            // first, replace the image on the first page
            val img = Image.create(doc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "peppers.jpg")!!.absolutePath)
            replacer.addImage(page.mediaBox, img.sdfObj)
            // next, replace the text place holders on the second page
            replacer.addString("NAME", "John Smith")
            replacer.addString("QUALIFICATIONS", "Philosophy Doctor")
            replacer.addString("JOB_TITLE", "Software Developer")
            replacer.addString("ADDRESS_LINE1", "#100 123 Software Rd")
            replacer.addString("ADDRESS_LINE2", "Vancouver, BC")
            replacer.addString("PHONE_OFFICE", "604-730-8989")
            replacer.addString("PHONE_MOBILE", "604-765-4321")
            replacer.addString("EMAIL", "info@pdftron.com")
            replacer.addString("WEBSITE_URL", "http://www.pdftron.com")
            // finally, apply
            replacer.process(page)

            doc.save(Utils.createExternalFile("BusinessCard.pdf").absolutePath, SDFDoc.SaveMode.REMOVE_UNUSED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done. Result saved in BusinessCard.pdf")
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        //--------------------------------------------------------------------------------
        // Example 2) Replace text in a region with new text

        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath)
            doc.initSecurityHandler()

            val replacer = ContentReplacer()
            val page = doc.getPage(1)
            val target_region = page.mediaBox
            val replacement_text = "hello hello hello hello hello hello hello hello hello hello"
            replacer.addText(target_region, replacement_text)
            replacer.process(page)

            doc.save(Utils.createExternalFile("ContentReplaced.pdf").absolutePath, SDFDoc.SaveMode.REMOVE_UNUSED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done. Result saved in ContentReplaced.pdf")
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        mOutputListener!!.println("Done.")

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