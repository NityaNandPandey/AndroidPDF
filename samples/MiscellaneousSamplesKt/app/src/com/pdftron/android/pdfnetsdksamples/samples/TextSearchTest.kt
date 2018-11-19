//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.common.PDFNetException
import com.pdftron.pdf.Action
import com.pdftron.pdf.Highlights
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.Rect
import com.pdftron.pdf.TextSearch
import com.pdftron.pdf.TextSearchResult
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

class TextSearchTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_textsearch_title)
        setDescription(R.string.sample_textsearch_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "credit card numbers.pdf")!!.absolutePath)
            doc.initSecurityHandler()

            val txt_search = TextSearch()
            var mode = TextSearch.e_whole_word or TextSearch.e_page_stop

            val pattern = "joHn sMiTh"

            //PDFDoc doesn't allow simultaneous access from different threads. If this
            //document could be used from other threads (e.g., the rendering thread inside
            //PDFView/PDFViewCtrl, if used), it is good practice to lock it.
            //Notice: don't forget to call doc.Unlock() to avoid deadlock.
            doc.lock()

            //call Begin() method to initialize the text search.
            txt_search.begin(doc, pattern, mode, -1, -1)

            var step = 0

            //call Run() method iteratively to find all matching instances.
            while (true) {
                val result = txt_search.run()

                if (result.code == TextSearchResult.e_found) {
                    if (step == 0) {
                        //step 0: found "John Smith"
                        //note that, here, 'ambient_string' and 'hlts' are not written to,
                        //as 'e_ambient_string' and 'e_highlight' are not set.
                        mOutputListener!!.println(result.resultStr + "'s credit card number is:")

                        //now switch to using regular expressions to find John's credit card number
                        mode = txt_search.mode
                        mode = mode or (TextSearch.e_reg_expression or TextSearch.e_highlight)
                        txt_search.mode = mode
                        val new_pattern = "\\d{4}-\\d{4}-\\d{4}-\\d{4}" //or "(\\d{4}-){3}\\d{4}"
                        txt_search.setPattern(new_pattern)

                        step = step + 1
                    } else if (step == 1) {
                        //step 1: found John's credit card number
                        mOutputListener!!.println("  " + result.resultStr)

                        //note that, here, 'hlts' is written to, as 'e_highlight' has been set.
                        //output the highlight info of the credit card number
                        val hlts = result.highlights
                        hlts.begin(doc)
                        while (hlts.hasNext()) {
                            mOutputListener!!.println("The current highlight is from page: " + hlts.currentPageNumber)
                            hlts.next()
                        }

                        //see if there is an AMEX card number
                        val new_pattern = "\\d{4}-\\d{6}-\\d{5}"
                        txt_search.setPattern(new_pattern)

                        step = step + 1
                    } else if (step == 2) {
                        //found an AMEX card number
                        mOutputListener!!.println("\nThere is an AMEX card number: ")
                        mOutputListener!!.println("  " + result.resultStr)

                        //change mode to find the owner of the credit card; supposedly, the owner's
                        //name proceeds the number
                        mode = txt_search.mode
                        mode = mode or TextSearch.e_search_up
                        txt_search.mode = mode
                        val new_pattern = "[A-z]++ [A-z]++"
                        txt_search.setPattern(new_pattern)

                        step = step + 1
                    } else if (step == 3) {
                        //found the owner's name of the AMEX card
                        mOutputListener!!.println("Is the owner's name:")
                        mOutputListener!!.println("  " + result.resultStr + "?")

                        //add a link annotation based on the location of the found instance
                        val hlts = result.highlights
                        hlts.begin(doc)
                        while (hlts.hasNext()) {
                            val cur_page = doc.getPage(hlts.currentPageNumber)
                            val q = hlts.currentQuads
                            val quad_count = q.size / 8
                            for (i in 0 until quad_count) {
                                //assume each quad is an axis-aligned rectangle
                                val offset = 8 * i
                                val x1 = Math.min(Math.min(Math.min(q[offset + 0], q[offset + 2]), q[offset + 4]), q[offset + 6])
                                val x2 = Math.max(Math.max(Math.max(q[offset + 0], q[offset + 2]), q[offset + 4]), q[offset + 6])
                                val y1 = Math.min(Math.min(Math.min(q[offset + 1], q[offset + 3]), q[offset + 5]), q[offset + 7])
                                val y2 = Math.max(Math.max(Math.max(q[offset + 1], q[offset + 3]), q[offset + 5]), q[offset + 7])
                                val hyper_link = com.pdftron.pdf.annots.Link.create(doc, Rect(x1, y1, x2, y2), Action.createURI(doc, "http://www.pdftron.com"))
                                cur_page.annotPushBack(hyper_link)
                            }
                            hlts.next()
                        }
                        doc.save(Utils.createExternalFile("credit card numbers_linked.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
                        mFileList.add(File(doc.fileName).name)
                        break
                    }
                } else if (result.code == TextSearchResult.e_page) {
                    //you can update your UI here, if needed
                } else {
                    break
                }
            }

            doc.unlock()
            doc.close()
        } catch (e: PDFNetException) {
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