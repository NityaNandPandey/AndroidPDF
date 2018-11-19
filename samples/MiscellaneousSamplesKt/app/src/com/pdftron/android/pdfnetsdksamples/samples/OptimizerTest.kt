//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.pdf.Flattener
import com.pdftron.pdf.Optimizer
import com.pdftron.pdf.PDFDoc
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

/**
 * The following sample illustrates how to reduce PDF file size using 'com.pdftron.pdf.Optimizer' class.
 * The sample also shows how to simplify and optimize PDF documents for viewing on mobile devices
 * and on the Web using 'com.pdftron.pdf.Flattener'.
 *
 *
 *
 * Note: Both 'Optimizer' and 'Flattener' are separately licensable add-on options to the core
 * PDFNet license.
 *
 *
 *
 * 'com.pdftron.pdf.Optimizer' can be used to optimize PDF documents by reducing the file size, removing
 * redundant information, and compressing data streams using the latest in image compression
 * technology.
 *
 * PDF Optimizer can compress and shrink PDF file size with the following operations:
 *
 *  * Remove duplicated fonts, images, ICC profiles, and any other data stream.
 *  * Optionally convert high-quality or print-ready PDF files to small, efficient and web-ready PDF.
 *  * Optionally down-sample large images to a given resolution.
 *  * Optionally compress or recompress PDF images using JBIG2 and JPEG2000 compression formats.
 *  * Compress uncompressed streams and remove unused PDF objects.
 *
 *
 *
 *
 * 'com.pdftron.pdf.Flattener' can be used to speed-up PDF rendering on mobile devices and on the Web by
 * simplifying page content (e.g. flattening complex graphics into images) while maintaining vector
 * text whenever possible.
 *
 *
 *
 * Flattener can also be used to simplify process of writing custom converters from PDF to other
 * formats. In this case, Flattener can be used as first step in the conversion pipeline to reduce
 * any PDF to a very simple representation (e.g. vector text on top of a background image).
 */
class OptimizerTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_optimizer_title)
        setDescription(R.string.sample_optimizer_description)

        // The standard library does not include the Optimizer and Flattener features.
        // If using the full library, please comment out the following call to enable the sample.
        // DisableRun();
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)
        val input_filename = "newsletter.pdf"
        val input_filename2 = "newsletter_opt1.pdf"
        val input_filename3 = "newsletter_opt2.pdf"
        val input_filename4 = "newsletter_opt3.pdf"

        //--------------------------------------------------------------------------------
        // Example 1) Optimize a PDF.
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + input_filename)!!.absolutePath)
            doc.initSecurityHandler()
            Optimizer.optimize(doc)
            doc.save(Utils.createExternalFile(input_filename2).absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        //--------------------------------------------------------------------------------
        // Example 2) Reduce image quality and use jpeg compression for
        // non monochrome images.
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + input_filename)!!.absolutePath)
            doc.initSecurityHandler()

            val image_settings = Optimizer.ImageSettings()

            // low quality jpeg compression
            image_settings.setCompressionMode(Optimizer.ImageSettings.e_jpeg)
            image_settings.setQuality(1)

            // Set the output dpi to be standard screen resolution
            image_settings.setImageDPI(144.0, 96.0)

            // this option will recompress images not compressed with
            // jpeg compression and use the result if the new image
            // is smaller.
            image_settings.forceRecompression(true)

            // this option is not commonly used since it can
            // potentially lead to larger files.  It should be enabled
            // only if the output compression specified should be applied
            // to every image of a given type regardless of the output image size
            //image_settings.forceChanges(true);

            val opt_settings = Optimizer.OptimizerSettings()
            opt_settings.setColorImageSettings(image_settings)
            opt_settings.setGrayscaleImageSettings(image_settings)

            Optimizer.optimize(doc, opt_settings)

            doc.save(Utils.createExternalFile(input_filename3).absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        //--------------------------------------------------------------------------------
        // Example 3) Use monochrome image settings and default settings
        // for color and grayscale images.
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + input_filename)!!.absolutePath)
            doc.initSecurityHandler()

            val mono_image_settings = Optimizer.MonoImageSettings()
            mono_image_settings.setCompressionMode(Optimizer.MonoImageSettings.e_jbig2)
            mono_image_settings.forceRecompression(true)
            val opt_settings = Optimizer.OptimizerSettings()
            opt_settings.setMonoImageSettings(mono_image_settings)

            Optimizer.optimize(doc, opt_settings)

            doc.save(Utils.createExternalFile(input_filename4).absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        // ----------------------------------------------------------------------
        // Example 4) Use Flattener to simplify content in this document
        // using default settings
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "TigerText.pdf")!!.absolutePath)
            doc.initSecurityHandler()

            val fl = Flattener()

            // The following lines can increase the resolution of background
            // images.
            //fl.setDPI(300);
            //fl.setMaximumImagePixels(5000000);

            // This line can be used to output Flate compressed background
            // images rather than DCTDecode compressed images which is the default
            //fl.setPreferJPG(false);

            // In order to adjust thresholds for when text is Flattened
            // the following function can be used.
            //fl.setThreshold(Flattener.e_keep_most);

            // We use e_fast option here since it is usually preferable
            // to avoid Flattening simple pages in terms of size and
            // rendering speed. If the desire is to simplify the
            // document for processing such that it contains only text and
            // a background image e_simple should be used instead.
            fl.Process(doc, Flattener.e_fast)

            doc.save(Utils.createExternalFile("TigerText_flatten.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
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
    // The following sample illustrates how to reduce PDF file size using 'pdftron.PDF.Optimizer'.
    // The sample also shows how to simplify and optimize PDF documents for viewing on mobile devices
    // and on the Web using 'pdftron.PDF.Flattener'.
    //
    // @note Both 'Optimizer' and 'Flattener' are separately licensable add-on options to the core PDFNet license.
    //
    // ----
    //
    // 'pdftron.PDF.Optimizer' can be used to optimize PDF documents by reducing the file size, removing
    // redundant information, and compressing data streams using the latest in image compression technology.
    //
    // PDF Optimizer can compress and shrink PDF file size with the following operations:
    // - Remove duplicated fonts, images, ICC profiles, and any other data stream.
    // - Optionally convert high-quality or print-ready PDF files to small, efficient and web-ready PDF.
    // - Optionally down-sample large images to a given resolution.
    // - Optionally compress or recompress PDF images using JBIG2 and JPEG2000 compression formats.
    // - Compress uncompressed streams and remove unused PDF objects.
    //
    // 'pdftron.PDF.Flattener' can be used to speed-up PDF rendering on mobile devices and on the Web by
    // simplifying page content (e.g. flattening complex graphics into images) while maintaining vector text
    // whenever possible.
    //
    // Flattener can also be used to simplify process of writing custom converters from PDF to other formats.
    // In this case, Flattener can be used as first step in the conversion pipeline to reduce any PDF to a
    // very simple representation (e.g. vector text on top of a background image).
    //---------------------------------------------------------------------------------------

}