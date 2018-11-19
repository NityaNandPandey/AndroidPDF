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
import com.pdftron.pdf.PDFNet
import com.pdftron.pdf.pdfa.PDFACompliance

import java.io.File
import java.util.ArrayList

class PDFATest : PDFNetSample() {
    init {
        setTitle(R.string.sample_pdfa_title)
        setDescription(R.string.sample_pdfa_description)

        // The standard library does not include PDF/A validation/conversion,
        // thus this sample will fail. Please, comment out this call
        // if using the full libraries.
        // DisableRun();
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)
        try {
            PDFNet.setColorManagement(PDFNet.e_lcms) // Required for proper PDF/A validation and conversion.

            //-----------------------------------------------------------
            // Example 1: PDF/A Validation
            //-----------------------------------------------------------
            var filename = "newsletter.pdf"
            var pdf_a = PDFACompliance(false, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + filename)!!.absolutePath, null, PDFACompliance.e_Level1B, null, 10)
            printResults(pdf_a, filename)
            pdf_a.destroy()

            //-----------------------------------------------------------
            // Example 2: PDF/A Conversion
            //-----------------------------------------------------------
            filename = "fish.pdf"
            pdf_a = PDFACompliance(true, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + filename)!!.absolutePath, null, PDFACompliance.e_Level1B, null, 10)
            filename = Utils.createExternalFile("pdfa.pdf").absolutePath
            pdf_a.saveAs(filename, true)
            pdf_a.destroy()
            mFileList.add("pdf_a.pdf")

            // Re-validate the document after the conversion...
            pdf_a = PDFACompliance(false, filename, null, PDFACompliance.e_Level1B, null, 10)
            printResults(pdf_a, filename)
            pdf_a.destroy()

        } catch (e: PDFNetException) {
            mOutputListener!!.println(e.message)
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


        internal fun printResults(pdf_a: PDFACompliance, filename: String) {
            try {
                val err_cnt = pdf_a.errorCount
                mOutputListener!!.print(filename)
                if (err_cnt == 0) {
                    mOutputListener!!.print(": OK.\n")
                } else {
                    mOutputListener!!.println(" is NOT a valid PDF/A file.")
                    for (i in 0 until err_cnt) {
                        val c = pdf_a.getError(i)
                        mOutputListener!!.println(" - e_PDFA" + c + ": " + PDFACompliance.getPDFAErrorMessage(c) + ".")
                        if (true) {
                            val num_refs = pdf_a.getRefObjCount(c)
                            if (num_refs > 0) {
                                mOutputListener!!.print("   Objects:")
                                var j = 0
                                while (j < num_refs) {
                                    mOutputListener!!.print(pdf_a.getRefObj(c, j).toString())
                                    if (++j != num_refs) mOutputListener!!.print(", ")
                                }
                                mOutputListener!!.println()
                            }
                        }
                    }
                    mOutputListener!!.println()
                }
            } catch (e: PDFNetException) {
                mOutputListener!!.println(e.message)
            }

        }
    }

}