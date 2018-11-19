//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.common.Matrix2D
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.PDFDraw
import com.pdftron.pdf.PDFRasterizer
import com.pdftron.pdf.Page
import com.pdftron.pdf.PageIterator
import com.pdftron.pdf.Rect
import com.pdftron.sdf.Obj
import com.pdftron.sdf.ObjSet

import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.util.ArrayList

class PDFDrawTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_pdfdraw_title)
        setDescription(R.string.sample_pdfdraw_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)
        try {
            // The first step in every application using PDFNet is to initialize the
            // library and set the path to common PDF resources. The library is usually
            // initialized only once, but calling Initialize() multiple times is also fine.

            // Optional: Set ICC color profiles to fine tune color conversion
            // for PDF 'device' color spaces...

            //PDFNet.setResourcesPath("../../../resources");
            //PDFNet.setColorManagement();
            //PDFNet.setDefaultDeviceCMYKProfile("D:/Misc/ICC/USWebCoatedSWOP.icc");
            //PDFNet.setDefaultDeviceRGBProfile("AdobeRGB1998.icc"); // will search in PDFNet resource folder.

            // ----------------------------------------------------
            // Optional: Set predefined font mappings to override default font
            // substitution for documents with missing fonts...

            // PDFNet.addFontSubst("StoneSans-Semibold", "C:/WINDOWS/Fonts/comic.ttf");
            // PDFNet.addFontSubst("StoneSans", "comic.ttf");  // search for 'comic.ttf' in PDFNet resource folder.
            // PDFNet.addFontSubst(PDFNet.e_Identity, "C:/WINDOWS/Fonts/arialuni.ttf");
            // PDFNet.addFontSubst(PDFNet.e_Japan1, "C:/Program Files/Adobe/Acrobat 7.0/Resource/CIDFont/KozMinProVI-Regular.otf");
            // PDFNet.addFontSubst(PDFNet.e_Japan2, "c:/myfonts/KozMinProVI-Regular.otf");
            // PDFNet.addFontSubst(PDFNet.e_Korea1, "AdobeMyungjoStd-Medium.otf");
            // PDFNet.addFontSubst(PDFNet.e_CNS1, "AdobeSongStd-Light.otf");
            // PDFNet.addFontSubst(PDFNet.e_GB1, "AdobeMingStd-Light.otf");

            val draw = PDFDraw()  // PDFDraw class is used to rasterize PDF pages.
            val hint_set = ObjSet()

            //--------------------------------------------------------------------------------
            // Example 1) Convert the first page to PNG and TIFF at 92 DPI.
            // A three step tutorial to convert PDF page to an image.
            try {
                // A) Open the PDF document.
                val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "tiger.pdf")!!.absolutePath)

                // Initialize the security handler, in case the PDF is encrypted.
                doc.initSecurityHandler()

                // B) The output resolution is set to 92 DPI.
                draw.setDPI(92.0)

                // C) Rasterize the first page in the document and save the result as PNG.
                val pg = doc.getPage(1)
                draw.export(pg, Utils.createExternalFile("tiger_92dpi.png").absolutePath)
                mFileList.add("tiger_92dpi.png")

                mOutputListener!!.println("Example 1: tiger_92dpi.png")

                // Export the same page as TIFF
                draw.export(pg, Utils.createExternalFile("tiger_92dpi.tif").absolutePath, "TIFF")
                mFileList.add("tiger_92dpi.tif")
                doc.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            //--------------------------------------------------------------------------------
            // Example 2) Convert the all pages in a given document to JPEG at 72 DPI.
            try {
                mOutputListener!!.println("Example 2:")
                val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath)
                // Initialize the security handler, in case the PDF is encrypted.
                doc.initSecurityHandler()

                draw.setDPI(72.0) // Set the output resolution is to 72 DPI.

                // Use optional encoder parameter to specify JPEG quality.
                val encoder_param = hint_set.createDict()
                encoder_param.putNumber("Quality", 80.0)

                // Traverse all pages in the document.
                val itr = doc.pageIterator
                while (itr.hasNext()) {
                    val current = itr.next()
                    val filename = "newsletter" + current!!.getIndex() + ".jpg"
                    mOutputListener!!.println(filename)
                    draw.export(current, Utils.createExternalFile(filename).absolutePath, "JPEG", encoder_param)
                }

                doc.close()
                mOutputListener!!.println("Done.")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Examples 3-5
            try {
                // Common code for remaining samples.
                val tiger_doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "tiger.pdf")!!.absolutePath)
                // Initialize the security handler, in case the PDF is encrypted.
                tiger_doc.initSecurityHandler()
                val page = tiger_doc.pageIterator.next()

                //--------------------------------------------------------------------------------
                // Example 3) Convert the first page to raw bitmap. Also, rotate the
                // page 90 degrees and save the result as RAW.
                draw.setDPI(100.0) // Set the output resolution is to 100 DPI.
                draw.setRotate(Page.e_90)  // Rotate all pages 90 degrees clockwise.

                // create a Java image
                val image = draw.getBitmap(page)

                //
                val width = image.width
                val height = image.height
                val arr = IntArray(width * height)
                image.getPixels(arr, 0, width, 0, 0, width, height)
                // pg.grabPixels();

                // convert to byte array
                val byteBuffer = ByteBuffer.allocate(arr.size * 4)
                val intBuffer = byteBuffer.asIntBuffer()
                intBuffer.put(arr)
                val rawByteArray = byteBuffer.array()
                // finally write the file
                try {
                    val fos = FileOutputStream(Utils.createExternalFile("tiger_100dpi_rot90.raw").absolutePath)
                    fos.write(rawByteArray)
                } catch (e: Exception) {
                }

                mOutputListener!!.println("Example 3: tiger_100dpi_rot90.raw")

                draw.setRotate(Page.e_0)  // Disable image rotation for remaining samples.

                //--------------------------------------------------------------------------------
                // Example 4) Convert PDF page to a fixed image size. Also illustrates some
                // other features in PDFDraw class such as rotation, image stretching, exporting
                // to grayscale, or monochrome.

                // Initialize render 'gray_hint' parameter, that is used to control the
                // rendering process. In this case we tell the rasterizer to export the image as
                // 1 Bit Per Component (BPC) image.
                val mono_hint = hint_set.createDict()
                mono_hint.putNumber("BPC", 1.0)

                // SetImageSize can be used instead of SetDPI() to adjust page  scaling
                // dynamically so that given image fits into a buffer of given dimensions.
                draw.setImageSize(1000, 1000)        // Set the output image to be 1000 wide and 1000 pixels tall

                draw.export(page, Utils.createExternalFile("tiger_1000x1000.png").absolutePath, "PNG", mono_hint)
                mOutputListener!!.println("Example 4: tiger_1000x1000.png")

                draw.setImageSize(200, 400) // Set the output image to be 200 wide and 300 pixels tall
                draw.setRotate(Page.e_180) // Rotate all pages 90 degrees clockwise.

                // 'gray_hint' tells the rasterizer to export the image as grayscale.
                val gray_hint = hint_set.createDict()
                gray_hint.putName("ColorSpace", "Gray")

                draw.export(page, Utils.createExternalFile("tiger_200x400_rot180.png").absolutePath, "PNG", gray_hint)
                mOutputListener!!.println("Example 4: tiger_200x400_rot180.png")

                draw.setImageSize(400, 200, false)  // The third parameter sets 'preserve-aspect-ratio' to false.
                draw.setRotate(Page.e_0)    // Disable image rotation.
                draw.export(page, Utils.createExternalFile("tiger_400x200_stretch.jpg").absolutePath, "JPEG")
                mFileList.add("tiger_400x200_stretch.jpg")
                mOutputListener!!.println("Example 4: tiger_400x200_stretch.jpg")

                //--------------------------------------------------------------------------------
                // Example 5) Zoom into a specific region of the page and rasterize the
                // area at 200 DPI and as a thumbnail (i.e. a 50x50 pixel image).
                val zoom_rect = Rect(216.0, 522.0, 330.0, 600.0)
                page!!.setCropBox(zoom_rect)    // Set the page crop box.

                // Select the crop region to be used for drawing.
                draw.setPageBox(Page.e_crop)
                draw.setDPI(900.0)  // Set the output image resolution to 900 DPI.
                draw.export(page, Utils.createExternalFile("tiger_zoom_900dpi.png").absolutePath, "PNG")
                mFileList.add("tiger_zoom_900dpi.png")
                mOutputListener!!.println("Example 5: tiger_zoom_900dpi.png")

                // -------------------------------------------------------------------------------
                // Example 6)
                draw.setImageSize(50, 50)       // Set the thumbnail to be 50x50 pixel image.
                draw.export(page, Utils.createExternalFile("tiger_zoom_50x50.png").absolutePath, "PNG")
                mFileList.add("tiger_zoom_50x50.png")
                mOutputListener!!.println("Example 6: tiger_zoom_50x50.png")

                tiger_doc.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val cmyk_hint = hint_set.createDict()
            cmyk_hint.putName("ColorSpace", "CMYK")

            //--------------------------------------------------------------------------------
            // Example 7) Convert the first PDF page to CMYK TIFF at 92 DPI.
            // A three step tutorial to convert PDF page to an image
            try {
                // A) Open the PDF document.
                val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "tiger.pdf")!!.absolutePath)
                // Initialize the security handler, in case the PDF is encrypted.
                doc.initSecurityHandler()

                // B) The output resolution is set to 92 DPI.
                draw.setDPI(92.0)

                // C) Rasterize the first page in the document and save the result as TIFF.
                val pg = doc.getPage(1)
                draw.export(pg, Utils.createExternalFile("out1.tif").absolutePath, "TIFF", cmyk_hint)
                mFileList.add("out1.tif")
                mOutputListener!!.println("Example 7: out1.tif")
                doc.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            //--------------------------------------------------------------------------------
            // Example 8) PDFRasterizer can be used for more complex rendering tasks, such as
            // strip by strip or tiled document rendering. In particular, it is useful for
            // cases where you cannot simply modify the page crop box (interactive viewing,
            // parallel rendering).  This example shows how you can rasterize the south-west
            // quadrant of a page.
            try {
                // A) Open the PDF document.
                val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "tiger.pdf")!!.absolutePath)
                // Initialize the security handler, in case the PDF is encrypted.
                doc.initSecurityHandler()

                // B) Get the page matrix
                val pg = doc.getPage(1)
                val box = Page.e_crop
                var mtx = pg.getDefaultMatrix(true, box, 0)
                // We want to render a quadrant, so use half of width and height
                val pg_w = pg.getPageWidth(box) / 2
                val pg_h = pg.getPageHeight(box) / 2

                // C) Scale matrix from PDF space to buffer space
                val dpi = 96.0
                val scale = dpi / 72.0 // PDF space is 72 dpi
                val buf_w = Math.floor(scale * pg_w)
                val buf_h = Math.floor(scale * pg_h)
                val bytes_per_pixel = 4 // BGRA buffer
                mtx.translate(0.0, -pg_h) // translate by '-pg_h' since we want south-west quadrant
                mtx = Matrix2D(scale, 0.0, 0.0, scale, 0.0, 0.0).multiply(mtx)

                // D) Rasterize page into memory buffer, according to our parameters
                val rast = PDFRasterizer()
                val buf = rast.rasterize(pg, buf_w.toInt(), buf_h.toInt(), buf_w.toInt() * bytes_per_pixel, bytes_per_pixel, true, mtx, null)

                mOutputListener!!.println("Example 8: Successfully rasterized into memory buffer.")
                doc.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            //--------------------------------------------------------------------------------
            // Example 9) Export raster content to PNG using different image smoothing settings.
            try {
                val text_doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "lorem_ipsum.pdf")!!.absolutePath)
                text_doc.initSecurityHandler()

                draw.setImageSmoothing(false, false)
                var filename = "raster_text_no_smoothing.png"
                draw.export(text_doc.pageIterator.next(), Utils.createExternalFile(filename).absolutePath)
                mOutputListener!!.println("Example 9 a): $filename. Done.")

                filename = "raster_text_smoothed.png"
                draw.setImageSmoothing(true, false /*default quality bilinear resampling*/)
                draw.export(text_doc.pageIterator.next(), Utils.createExternalFile(filename).absolutePath)
                mOutputListener!!.println("Example 9 b): $filename. Done.")

                filename = "raster_text_high_quality.png"
                draw.setImageSmoothing(true, true /*high quality area resampling*/)
                draw.export(text_doc.pageIterator.next(), Utils.createExternalFile(filename).absolutePath)
                mOutputListener!!.println("Example 9 c): $filename. Done.")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            //--------------------------------------------------------------------------------
            // Example 10) Export separations directly, without conversion to an output colorspace
            try {
                val separation_doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "op_blend_test.pdf")!!.absolutePath)
                separation_doc.initSecurityHandler()

                val separation_hint = hint_set.createDict()
                separation_hint.putName("ColorSpace", "Separation")
                draw.setDPI(96.0)
                draw.setImageSmoothing(true, true)
                // set overprint preview to always on
                draw.setOverprint(1)

                var filename = "merged_separations.png"
                draw.export(separation_doc.getPage(1), Utils.createExternalFile(filename).absolutePath, "PNG")
                mOutputListener!!.println("Example 10 a): $filename. Done.")

                filename = "separation"
                draw.export(separation_doc.getPage(1), Utils.createExternalFile(filename).absolutePath, "PNG", separation_hint)
                mOutputListener!!.println("Example 10 b): " + filename + "_[ink].png. Done.")

                filename = "separation_NChannel.tif"
                draw.export(separation_doc.getPage(1), Utils.createExternalFile(filename).absolutePath, "TIFF", separation_hint)
                mOutputListener!!.println("Example 10 c): $filename. Done.")
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Calling Terminate when PDFNet is no longer in use is a good practice, but
            // is not required.
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