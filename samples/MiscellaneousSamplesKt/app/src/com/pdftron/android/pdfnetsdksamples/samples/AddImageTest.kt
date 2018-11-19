//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.common.Matrix2D
import com.pdftron.common.PDFNetException
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementBuilder
import com.pdftron.pdf.ElementWriter
import com.pdftron.pdf.Font
import com.pdftron.pdf.Image
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.Rect
import com.pdftron.sdf.Obj
import com.pdftron.sdf.ObjSet
import com.pdftron.sdf.SDFDoc
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils

import java.io.File
import java.util.ArrayList

class AddImageTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_addimage_title)
        setDescription(R.string.sample_addimage_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        try {
            mOutputListener!!.println("-------------------------------------------------")

            val doc = PDFDoc()

            val f = ElementBuilder()        // Used to build new Element objects
            val writer = ElementWriter()    // Used to write Elements to the page

            var page = doc.pageCreate()    // Start a new page
            writer.begin(page)        // Begin writing to this page

            // ----------------------------------------------------------
            // Add JPEG image to the output file
            var img = Image.create(doc.sdfDoc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "peppers.jpg")!!.absolutePath)
            var element = f.createImage(img, 50.0, 500.0, (img.imageWidth / 2).toDouble(), (img.imageHeight / 2).toDouble())
            writer.writePlacedElement(element)

            // ----------------------------------------------------------
            // Add a PNG image to the output file
            img = Image.create(doc.sdfDoc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "butterfly.png")!!.absolutePath)
            element = f.createImage(img, Matrix2D(100.0, 0.0, 0.0, 100.0, 300.0, 500.0))
            writer.writePlacedElement(element)

            // ----------------------------------------------------------
            // Add a GIF image to the output file
            img = Image.create(doc.sdfDoc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "pdfnet.gif")!!.absolutePath)
            element = f.createImage(img, Matrix2D(img.imageWidth.toDouble(), 0.0, 0.0, img.imageHeight.toDouble(), 50.0, 350.0))
            writer.writePlacedElement(element)

            // ----------------------------------------------------------
            // Add a TIFF image to the output file
            img = Image.create(doc.sdfDoc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "grayscale.tif")!!.absolutePath)
            element = f.createImage(img, Matrix2D(img.imageWidth.toDouble(), 0.0, 0.0, img.imageHeight.toDouble(), 10.0, 50.0))
            writer.writePlacedElement(element)

            writer.end()           // Save the page
            doc.pagePushBack(page) // Add the page to the document page sequence

            // ----------------------------------------------------------
            // Embed a monochrome TIFF. Compress the image using lossy JBIG2 filter.

            page = doc.pageCreate(Rect(0.0, 0.0, 612.0, 794.0))
            writer.begin(page)    // begin writing to this page

            // Note: encoder hints can be used to select between different compression methods.
            // For example to instruct PDFNet to compress a monochrome image using JBIG2 compression.
            val hint_set = ObjSet()
            val enc = hint_set.createArray()  // Initilaize encoder 'hint' parameter
            enc.pushBackName("JBIG2")
            enc.pushBackName("Lossy")

            img = Image.create(doc.sdfDoc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "multipage.tif")!!.absolutePath)
            element = f.createImage(img, Matrix2D(612.0, 0.0, 0.0, 794.0, 0.0, 0.0))
            writer.writePlacedElement(element)

            writer.end()           // Save the page
            doc.pagePushBack(page) // Add the page to the document page sequence

            // ----------------------------------------------------------
            // Add a JPEG2000 (JP2) image to the output file

            // Create a new page
            page = doc.pageCreate()
            writer.begin(page)    // Begin writing to the page

            // Embed the image.
            img = Image.create(doc.sdfDoc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "palm.jp2")!!.absolutePath)

            // Position the image on the page.
            element = f.createImage(img, Matrix2D(img.imageWidth.toDouble(), 0.0, 0.0, img.imageHeight.toDouble(), 96.0, 80.0))
            writer.writePlacedElement(element)

            // Write 'JPEG2000 Sample' text string under the image.
            writer.writeElement(f.createTextBegin(Font.create(doc.sdfDoc, Font.e_times_roman), 32.0))
            element = f.createTextRun("JPEG2000 Sample")
            element.setTextMatrix(1.0, 0.0, 0.0, 1.0, 190.0, 30.0)
            writer.writeElement(element)
            writer.writeElement(f.createTextEnd())

            writer.end()    // Finish writing to the page
            doc.pagePushBack(page)

            // ----------------------------------------------------------
            // doc.Save((Utils.createExternalFile("addimage.pdf").getAbsolutePath()).c_str(), Doc.e_remove_unused, 0);
            doc.save(Utils.createExternalFile("addimage.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done. Result saved in addimage.pdf...")
        } catch (e: PDFNetException) {
            e.printStackTrace()
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