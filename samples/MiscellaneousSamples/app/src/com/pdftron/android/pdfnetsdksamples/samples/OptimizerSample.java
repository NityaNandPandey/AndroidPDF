//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.pdf.Flattener;
import com.pdftron.pdf.Optimizer;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.sdf.SDFDoc;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

/**
 * The following sample illustrates how to reduce PDF file size using 'com.pdftron.pdf.Optimizer' class.
 * The sample also shows how to simplify and optimize PDF documents for viewing on mobile devices
 * and on the Web using 'com.pdftron.pdf.Flattener'.
 *
 * <p>
 * Note: Both 'Optimizer' and 'Flattener' are separately licensable add-on options to the core
 * PDFNet license.
 *
 * <p>
 * 'com.pdftron.pdf.Optimizer' can be used to optimize PDF documents by reducing the file size, removing
 * redundant information, and compressing data streams using the latest in image compression
 * technology.
 *
 * PDF Optimizer can compress and shrink PDF file size with the following operations:
 * <ul>
 * <li>Remove duplicated fonts, images, ICC profiles, and any other data stream.
 * <li>Optionally convert high-quality or print-ready PDF files to small, efficient and web-ready PDF.
 * <li>Optionally down-sample large images to a given resolution.
 * <li>Optionally compress or recompress PDF images using JBIG2 and JPEG2000 compression formats.
 * <li>Compress uncompressed streams and remove unused PDF objects.
 * </ul>
 *
 * <p>
 * 'com.pdftron.pdf.Flattener' can be used to speed-up PDF rendering on mobile devices and on the Web by
 * simplifying page content (e.g. flattening complex graphics into images) while maintaining vector
 * text whenever possible.
 *
 * <p>
 * Flattener can also be used to simplify process of writing custom converters from PDF to other
 * formats. In this case, Flattener can be used as first step in the conversion pipeline to reduce
 * any PDF to a very simple representation (e.g. vector text on top of a background image).
 */
public class OptimizerSample extends PDFNetSample {

    public OptimizerSample() {
        setTitle(R.string.sample_optimizer_title);
        setDescription(R.string.sample_optimizer_description);
        
        // The standard library does not include the Optimizer and Flattener features.
        // If using the full library, please comment out the following call to enable the sample.
        // DisableRun();
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        printHeader(outputListener);
        
        if (!isEnabled()) {
            outputListener.println("Sample is disabled. Please, check source code for more information.");
            printFooter(outputListener);
            return;
        }
        
        String input_filename = "newsletter.pdf";
        
        //--------------------------------------------------------------------------------
        // Example 1) Optimize a PDF. 
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + input_filename));
            doc.initSecurityHandler();
            Optimizer.optimize(doc);
            doc.save(Utils.createExternalFile(input_filename + "_opt1.pdf").getAbsolutePath(),
                    SDFDoc.e_linearized, null);
            addToFileList(input_filename + "_opt1.pdf");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        //--------------------------------------------------------------------------------
        // Example 2) Reduce image quality and use jpeg compression for
        // non monochrome images.
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + input_filename));
            doc.initSecurityHandler();

            Optimizer.ImageSettings image_settings = new Optimizer.ImageSettings();

            // low quality jpeg compression
            image_settings.setCompressionMode(Optimizer.ImageSettings.e_jpeg);
            image_settings.setQuality(1);

            // Set the output dpi to be standard screen resolution
            image_settings.setImageDPI(144, 96);

            // this option will recompress images not compressed with
            // jpeg compression and use the result if the new image
            // is smaller.
            image_settings.forceRecompression(true);

            // this option is not commonly used since it can
            // potentially lead to larger files. It should be enabled
            // only if the output compression specified should be applied
            // to every image of a given type regardless of the output image
            // size
            // image_settings.forceChanges(true);

            Optimizer.OptimizerSettings opt_settings = new Optimizer.OptimizerSettings();
            opt_settings.setColorImageSettings(image_settings);
            opt_settings.setGrayscaleImageSettings(image_settings);

            Optimizer.optimize(doc, opt_settings);

            doc.save(Utils.createExternalFile(input_filename + "_opt2.pdf").getAbsolutePath(),
                    SDFDoc.e_linearized, null);
            addToFileList(input_filename + "_opt2.pdf");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }
        
        //--------------------------------------------------------------------------------
        // Example 3) Use monochrome image settings and default settings
        // for color and grayscale images. 
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + input_filename));
            doc.initSecurityHandler();

            Optimizer.MonoImageSettings mono_image_settings = new Optimizer.MonoImageSettings();
            mono_image_settings.setCompressionMode(Optimizer.MonoImageSettings.e_jbig2);
            mono_image_settings.forceRecompression(true);
            Optimizer.OptimizerSettings opt_settings = new Optimizer.OptimizerSettings();
            opt_settings.setMonoImageSettings(mono_image_settings);

            Optimizer.optimize(doc, opt_settings);

            doc.save(Utils.createExternalFile(input_filename + "_opt3.pdf").getAbsolutePath(),
                    SDFDoc.e_linearized, null);
            addToFileList(input_filename + "_opt3.pdf");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // ----------------------------------------------------------------------
        // Example 4) Use Flattener to simplify content in this document
        // using default settings
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "TigerText.pdf"));
            doc.initSecurityHandler();

            Flattener fl = new Flattener();

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
            fl.Process(doc,Flattener.e_fast);

            doc.save(Utils.createExternalFile("TigerText_flatten.pdf").getAbsolutePath(), SDFDoc.e_linearized, null);
            addToFileList("TigerText_flatten.pdf");

        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        printFooter(outputListener);
    }

}
