//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.pdf.ColorPt
import com.pdftron.pdf.Font
import com.pdftron.pdf.Image
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.PageSet
import com.pdftron.pdf.Rect
import com.pdftron.pdf.Stamper
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

/**
 * The following sample shows how to add new content (or watermark) PDF pages
 * using 'com.pdftron.pdf.Stamper' utility class.
 *
 * Stamper can be used to PDF pages with text, images, or with other PDF content
 * in only a few lines of code. Although Stamper is very simple to use compared
 * to ElementBuilder/ElementWriter it is not as powerful or flexible. In case you
 * need full control over PDF creation use ElementBuilder/ElementWriter to add
 * new content to existing PDF pages as shown in the ElementBuilder sample project.
 */
class StamperTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_stamper_title)
        setDescription(R.string.sample_stamper_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)
        val input_filename = "newsletter.pdf"

        //--------------------------------------------------------------------------------
        // Example 1) Add text stamp to all pages, then remove text stamp from odd pages.
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + input_filename)!!.absolutePath)
            doc.initSecurityHandler()
            val s = Stamper(Stamper.e_relative_scale, 0.5, 0.5)
            s.setAlignment(Stamper.e_horizontal_center, Stamper.e_vertical_center)
            val red = ColorPt(1.0, 0.0, 0.0) // set text color to red
            s.setFontColor(red)
            val msg = "If you are reading this\nthis is an even page"
            val ps = PageSet(1, doc.pageCount)
            s.stampText(doc, msg, ps)
            //delete all text stamps in even pages
            val ps1 = PageSet(1, doc.pageCount, PageSet.e_odd)
            Stamper.deleteStamps(doc, ps1)

            doc.save(Utils.createExternalFile("$input_filename.ex1.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        //--------------------------------------------------------------------------------
        // Example 2) Add Image stamp to first 2 pages.
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + input_filename)!!.absolutePath)
            doc.initSecurityHandler()

            val s = Stamper(Stamper.e_relative_scale, .05, .05)
            val img = Image.create(doc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "peppers.jpg")!!.absolutePath)
            s.setSize(Stamper.e_relative_scale, 0.5, 0.5)
            //set position of the image to the center, left of PDF pages
            s.setAlignment(Stamper.e_horizontal_left, Stamper.e_vertical_center)
            val pt = ColorPt(0.0, 0.0, 0.0, 0.0)
            s.setFontColor(pt)
            s.setRotation(180.0)
            s.setAsBackground(false)
            //only stamp first 2 pages
            val ps = PageSet(1, 2)
            s.stampImage(doc, img, ps)

            doc.save(Utils.createExternalFile("$input_filename.ex2.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        //--------------------------------------------------------------------------------
        // Example 3) Add Page stamp to all pages.
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + input_filename)!!.absolutePath)
            doc.initSecurityHandler()

            val fish_doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "fish.pdf")!!.absolutePath)
            fish_doc.initSecurityHandler()

            val s = Stamper(Stamper.e_relative_scale, 0.5, 0.5)
            val src_page = fish_doc.getPage(1)
            val page_one_crop = src_page.cropBox
            // set size of the image to 10% of the original while keep the old aspect ratio
            s.setSize(Stamper.e_absolute_size, page_one_crop.width * 0.1, -1.0)
            s.setOpacity(0.4)
            s.setRotation(-67.0)
            //put the image at the bottom right hand corner
            s.setAlignment(Stamper.e_horizontal_right, Stamper.e_vertical_bottom)
            val ps = PageSet(1, doc.pageCount)
            s.stampPage(doc, src_page, ps)

            doc.save(Utils.createExternalFile("$input_filename.ex3.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            fish_doc.close()
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        //--------------------------------------------------------------------------------
        // Example 4) Add Image stamp to first 20 odd pages.
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + input_filename)!!.absolutePath)
            doc.initSecurityHandler()

            val s = Stamper(Stamper.e_absolute_size, 20.0, 20.0)
            s.setOpacity(1.0)
            s.setRotation(45.0)
            s.setAsBackground(true)
            s.setPosition(30.0, 40.0)
            val img = Image.create(doc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "peppers.jpg")!!.absolutePath)
            val ps = PageSet(1, 20, PageSet.e_odd)
            s.stampImage(doc, img, ps)

            doc.save(Utils.createExternalFile("$input_filename.ex4.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        //--------------------------------------------------------------------------------
        // Example 5) Add text stamp to first 20 even pages
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + input_filename)!!.absolutePath)
            doc.initSecurityHandler()

            val s = Stamper(Stamper.e_relative_scale, .05, .05)
            s.setPosition(0.0, 0.0)
            s.setOpacity(0.7)
            s.setRotation(90.0)
            s.setSize(Stamper.e_font_size, 80.0, -1.0)
            s.setTextAlignment(Stamper.e_align_center)
            val ps = PageSet(1, 20, PageSet.e_even)
            s.stampText(doc, "Goodbye\nMoon", ps)

            doc.save(Utils.createExternalFile("$input_filename.ex5.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        //--------------------------------------------------------------------------------
        // Example 6) Add first page as stamp to all even pages
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + input_filename)!!.absolutePath)
            doc.initSecurityHandler()

            val fish_doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "fish.pdf")!!.absolutePath)
            fish_doc.initSecurityHandler()

            val s = Stamper(Stamper.e_relative_scale, 0.3, 0.3)
            s.setOpacity(1.0)
            s.setRotation(270.0)
            s.setAsBackground(true)
            s.setPosition(0.5, 0.5, true)
            s.setAlignment(Stamper.e_horizontal_left, Stamper.e_vertical_bottom)
            val page_one = fish_doc.getPage(1)
            val ps = PageSet(1, doc.pageCount, PageSet.e_even)
            s.stampPage(doc, page_one, ps)

            doc.save(Utils.createExternalFile("$input_filename.ex6.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            fish_doc.close()
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        //--------------------------------------------------------------------------------
        // Example 7) Add image stamp at top right corner in every pages
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + input_filename)!!.absolutePath)
            doc.initSecurityHandler()

            val s = Stamper(Stamper.e_relative_scale, .1, .1)
            s.setOpacity(0.8)
            s.setRotation(135.0)
            s.setAsBackground(false)
            s.showsOnPrint(false)
            s.setAlignment(Stamper.e_horizontal_left, Stamper.e_vertical_top)
            s.setPosition(10.0, 10.0)

            val img = Image.create(doc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "peppers.jpg")!!.absolutePath)
            val ps = PageSet(1, doc.pageCount, PageSet.e_all)
            s.stampImage(doc, img, ps)

            doc.save(Utils.createExternalFile("$input_filename.ex7.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        //--------------------------------------------------------------------------------
        // Example 8) Add Text stamp to first 2 pages, and image stamp to first page.
        //          Because text stamp is set as background, the image is top of the text
        //          stamp. Text stamp on the first page is not visible.
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + input_filename)!!.absolutePath)
            doc.initSecurityHandler()

            val s = Stamper(Stamper.e_relative_scale, 0.07, -0.1)
            s.setAlignment(Stamper.e_horizontal_right, Stamper.e_vertical_bottom)
            s.setAlignment(Stamper.e_horizontal_center, Stamper.e_vertical_top)
            s.setFont(Font.create(doc, Font.e_courier, true))
            val red = ColorPt(1.0, 0.0, 0.0, 0.0)
            s.setFontColor(red) //set color to red
            s.setTextAlignment(Stamper.e_align_right)
            s.setAsBackground(true) //set text stamp as background
            val ps = PageSet(1, 2)
            s.stampText(doc, "This is a title!", ps)

            val img = Image.create(doc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "peppers.jpg")!!.absolutePath)
            s.setAsBackground(false) // set image stamp as foreground
            val first_page_ps = PageSet(1)
            s.stampImage(doc, img, first_page_ps)

            doc.save(Utils.createExternalFile("$input_filename.ex8.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return
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
    //---------------------------------------------------------------------------------------
    // The following sample shows how to add new content (or watermark) PDF pages
    // using 'pdftron.PDF.Stamper' utility class.
    //
    // Stamper can be used to PDF pages with text, images, or with other PDF content
    // in only a few lines of code. Although Stamper is very simple to use compared
    // to ElementBuilder/ElementWriter it is not as powerful or flexible. In case you
    // need full control over PDF creation use ElementBuilder/ElementWriter to add
    // new content to existing PDF pages as shown in the ElementBuilder sample project.
    //---------------------------------------------------------------------------------------

}