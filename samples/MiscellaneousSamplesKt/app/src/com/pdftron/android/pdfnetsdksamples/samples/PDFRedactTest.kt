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
import com.pdftron.pdf.Rect
import com.pdftron.pdf.Redactor
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

/**
 * PDF Redactor is a separately licensable Add-on that offers options to remove
 * (not just covering or obscuring) content within a region of PDF.
 * With printed pages, redaction involves blacking-out or cutting-out areas of
 * the printed page. With electronic documents that use formats such as PDF,
 * redaction typically involves removing sensitive content within documents for
 * safe distribution to courts, patent and government institutions, the media,
 * customers, vendors or any other audience with restricted access to the content.
 *
 * The redaction process in PDFNet consists of two steps:
 *
 * a) Content identification: A user applies redact annotations that specify the
 * pieces or regions of content that should be removed. The content for redaction
 * can be identified either interactively (e.g. using 'com.pdftron.pdf.PDFViewCtrl'
 * as shown in PDFView sample) or programmatically (e.g. using 'com.pdftron.pdf.TextSearch'
 * or 'com.pdftron.pdf.TextExtractor'). Up until the next step is performed, the user
 * can see, move and redefine these annotations.
 * b) Content removal: Using 'com.pdftron.pdf.Redactor.Redact()' the user instructs
 * PDFNet to apply the redact regions, after which the content in the area specified
 * by the redact annotations is removed. The redaction function includes number of
 * options to control the style of the redaction overlay (including color, text,
 * font, border, transparency, etc.).
 *
 * PDFTron Redactor makes sure that if a portion of an image, text, or vector graphics
 * is contained in a redaction region, that portion of the image or path data is
 * destroyed and is not simply hidden with clipping or image masks. PDFNet API can also
 * be used to review and remove metadata and other content that can exist in a PDF
 * document, including XML Forms Architecture (XFA) content and Extensible Metadata
 * Platform (XMP) content.
 */

class PDFRedactTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_pdfredact_title)
        setDescription(R.string.sample_pdfredact_description)

        // The standard library does not include the Redaction feature.
        // If using the full library, please comment out the following
        // call.
        // DisableRun();
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)
        // Relative paths to folders containing test files.

        try {
            val vec = arrayOfNulls<Redactor.Redaction>(6)
            vec[0] = Redactor.Redaction(1, Rect(0.0, 0.0, 600.0, 600.0), false, "Top Secret")
            vec[1] = Redactor.Redaction(2, Rect(0.0, 0.0, 100.0, 100.0), false, "foo")
            vec[2] = Redactor.Redaction(2, Rect(100.0, 100.0, 200.0, 200.0), false, "bar")
            vec[3] = Redactor.Redaction(2, Rect(300.0, 300.0, 400.0, 400.0), false, "")
            vec[4] = Redactor.Redaction(2, Rect(500.0, 500.0, 600.0, 600.0), false, "")
            vec[5] = Redactor.Redaction(3, Rect(0.0, 0.0, 700.0, 20.0), false, "")

            redact(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath, Utils.createExternalFile("redacted.pdf").absolutePath, vec)
            mFileList.add("redacted.pdf")
            mOutputListener!!.println("Done...")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        for (file in mFileList) {
            addToFileList(file)
        }
        printFooter(outputListener)
    }

    companion object {

        private var mOutputListener: OutputListener? = null

        private val mFileList = ArrayList<String>()

        fun redact(input: String, output: String, vec: Array<Redactor.Redaction?>) {
            try {
                val doc = PDFDoc(input)
                if (doc.initSecurityHandler()) {
                    val app = Redactor.Appearance()
                    app.redactionOverlay = true
                    app.border = false
                    app.showRedactedContentRegions = true

                    Redactor.redact(doc, vec, app, false, true)
                    doc.save(output, SDFDoc.SaveMode.REMOVE_UNUSED, null)
                    mFileList.add(File(doc.fileName).name)
                    doc.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

}