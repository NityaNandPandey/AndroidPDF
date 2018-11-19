//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementBuilder
import com.pdftron.pdf.ElementWriter
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.PageIterator
import com.pdftron.pdf.Rect
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

//-----------------------------------------------------------------------------------
//The sample illustrates how multiple pages can be combined/imposed
//using PDFNet. Page imposition can be used to arrange/order pages
//prior to printing or to assemble a 'master' page from several 'source'
//pages. Using PDFNet API it is possible to write applications that can
//re-order the pages such that they will display in the correct order
//when the hard copy pages are compiled and folded correctly.
//-----------------------------------------------------------------------------------

class ImpositionTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_imposition_title)
        setDescription(R.string.sample_imposition_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        try {
            mOutputListener!!.println("-------------------------------------------------")
            mOutputListener!!.println("Opening the input pdf...")

            val filein = Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath
            val fileout = Utils.createExternalFile("newsletter_booklet.pdf").absolutePath

            val in_doc = PDFDoc(filein)
            in_doc.initSecurityHandler()

            // Create a list of pages to import from one PDF document to another.
            val copy_pages = arrayOfNulls<Page>(in_doc.pageCount)
            var j = 0
            val itr = in_doc.pageIterator
            while (itr.hasNext()) {
                copy_pages[j] = itr.next()
                j++
            }

            val new_doc = PDFDoc()
            val imported_pages = new_doc.importPages(copy_pages)

            // Paper dimension for A3 format in points. Because one inch has
            // 72 points, 11.69 inch 72 = 841.69 points
            val media_box = Rect(0.0, 0.0, 1190.88, 841.69)
            val mid_point = media_box.width / 2

            val builder = ElementBuilder()
            val writer = ElementWriter()

            var i = 0
            while (i < imported_pages.size) {
                // Create a blank new A3 page and place on it two pages from the input document.
                val new_page = new_doc.pageCreate(media_box)
                writer.begin(new_page)

                // Place the first page
                var src_page = imported_pages[i]
                var element = builder.createForm(src_page)

                var sc_x = mid_point / src_page.pageWidth
                var sc_y = media_box.height / src_page.pageHeight
                var scale = if (sc_x < sc_y) sc_x else sc_y // min(sc_x, sc_y)
                element.gState.setTransform(scale, 0.0, 0.0, scale, 0.0, 0.0)
                writer.writePlacedElement(element)

                // Place the second page
                ++i
                if (i < imported_pages.size) {
                    src_page = imported_pages[i]
                    element = builder.createForm(src_page)
                    sc_x = mid_point / src_page.pageWidth
                    sc_y = media_box.height / src_page.pageHeight
                    scale = if (sc_x < sc_y) sc_x else sc_y // min(sc_x, sc_y)
                    element.gState.setTransform(scale, 0.0, 0.0, scale, mid_point, 0.0)
                    writer.writePlacedElement(element)
                }

                writer.end()
                new_doc.pagePushBack(new_page)
                ++i
            }

            new_doc.save(fileout, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(new_doc.fileName).name)
            new_doc.close()
            in_doc.close()
            mOutputListener!!.println("Done. Result saved in newsletter_booklet.pdf...")
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
    }

}