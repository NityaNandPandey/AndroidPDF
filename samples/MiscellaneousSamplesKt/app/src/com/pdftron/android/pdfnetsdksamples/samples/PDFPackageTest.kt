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
import com.pdftron.filters.Filter
import com.pdftron.pdf.ColorPt
import com.pdftron.pdf.ColorSpace
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementBuilder
import com.pdftron.pdf.ElementWriter
import com.pdftron.pdf.FileSpec
import com.pdftron.pdf.Font
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.Rect
import com.pdftron.sdf.NameTree
import com.pdftron.sdf.NameTreeIterator
import com.pdftron.sdf.Obj
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

//-----------------------------------------------------------------------------------
//This sample illustrates how to create, extract, and manipulate PDF Portfolios
//(a.k.a. PDF Packages) using PDFNet SDK.
//-----------------------------------------------------------------------------------

class PDFPackageTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_pdfpackage_title)
        setDescription(R.string.sample_pdfpackage_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        // Create a PDF Package.
        try {
            val doc = PDFDoc()
            addPackage(doc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "numbered.pdf")!!.absolutePath, "My File 1")
            addPackage(doc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath, "My Newsletter...")
            addPackage(doc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "peppers.jpg")!!.absolutePath, "An image")
            addCoverPage(doc)
            doc.save(Utils.createExternalFile("package.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Extract parts from a PDF Package.
        try {
            val doc = PDFDoc(Utils.createExternalFile("package.pdf").absolutePath)
            doc.initSecurityHandler()

            val files = NameTree.find(doc.sdfDoc, "EmbeddedFiles")
            if (files.isValid) {
                // Traverse the list of embedded files.
                val i = files.iterator
                var counter = 0
                while (i.hasNext()) {
                    val entry_name = i.key().asPDFText
                    mOutputListener!!.println("Part: $entry_name")

                    val file_spec = FileSpec(i.value())
                    val stm = file_spec.fileData
                    if (stm != null) {
                        val fname = Utils.createExternalFile("extract_").absolutePath + counter
                        stm.writeToFile(fname, false)
                    }
                    i.next()
                    ++counter
                }
            }
            doc.close()
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

        @Throws(PDFNetException::class)
        internal fun addPackage(doc: PDFDoc, file: String, desc: String) {
            val files = NameTree.create(doc.sdfDoc, "EmbeddedFiles")
            val fs = FileSpec.create(doc, file, true)
            files.put(file.toByteArray(), fs.sdfObj)
            fs.sdfObj.putText("Desc", desc)

            var collection: Obj? = doc.root.findObj("Collection")
            if (collection == null) collection = doc.root.putDict("Collection")

            // You could here manipulate any entry in the Collection dictionary.
            // For example, the following line sets the tile mode for initial view mode
            // Please refer to section '2.3.5 Collections' in PDF Reference for details.
            collection!!.putName("View", "T")
        }

        @Throws(PDFNetException::class)
        internal fun addCoverPage(doc: PDFDoc) {
            // Here we dynamically generate cover page (please see ElementBuilder
            // sample for more extensive coverage of PDF creation API).
            val page = doc.pageCreate(Rect(0.0, 0.0, 200.0, 200.0))

            val b = ElementBuilder()
            val w = ElementWriter()
            w.begin(page)
            val font = Font.create(doc.sdfDoc, Font.e_helvetica)
            w.writeElement(b.createTextBegin(font, 12.0))
            val e = b.createTextRun("My PDF Collection")
            e.setTextMatrix(1.0, 0.0, 0.0, 1.0, 50.0, 96.0)
            e.gState.fillColorSpace = ColorSpace.createDeviceRGB()
            e.gState.fillColor = ColorPt(1.0, 0.0, 0.0)
            w.writeElement(e)
            w.writeElement(b.createTextEnd())
            w.end()
            doc.pagePushBack(page)

            // Alternatively we could import a PDF page from a template PDF document
            // (for an example please see PDFPage sample project).
            // ...
        }
    }

}