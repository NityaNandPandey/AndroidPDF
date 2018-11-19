//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.common.Matrix2D;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFDraw;
import com.pdftron.pdf.PDFRasterizer;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.Rect;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.ObjSet;
import android.graphics.Bitmap;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

public class PDFDrawSample extends PDFNetSample {

    public PDFDrawSample() {
        setTitle(R.string.sample_pdfdraw_title);
        setDescription(R.string.sample_pdfdraw_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        printHeader(outputListener);
        
        try {
            // Optional: Set ICC color profiles to fine tune color conversion
            // for PDF 'device' color spaces...

            // PDFNet.setResourcesPath("../../../resources");
            // PDFNet.setColorManagement();
            // PDFNet.setDefaultDeviceCMYKProfile("D:/Misc/ICC/USWebCoatedSWOP.icc");
            // PDFNet.setDefaultDeviceRGBProfile("AdobeRGB1998.icc"); // will search in PDFNet resource folder.

            // ----------------------------------------------------
            // Optional: Set predefined font mappings to override default font
            // substitution for documents with missing fonts...

            // PDFNet.addFontSubst("StoneSans-Semibold", "C:/WINDOWS/Fonts/comic.ttf");
            // PDFNet.addFontSubst("StoneSans", "comic.ttf"); // search for 'comic.ttf' in PDFNet resource folder.
            // PDFNet.addFontSubst(PDFNet.e_Identity, "C:/WINDOWS/Fonts/arialuni.ttf");
            // PDFNet.addFontSubst(PDFNet.e_Japan1, "C:/Program Files/Adobe/Acrobat 7.0/Resource/CIDFont/KozMinProVI-Regular.otf");
            // PDFNet.addFontSubst(PDFNet.e_Japan2, "c:/myfonts/KozMinProVI-Regular.otf");
            // PDFNet.addFontSubst(PDFNet.e_Korea1, "AdobeMyungjoStd-Medium.otf");
            // PDFNet.addFontSubst(PDFNet.e_CNS1, "AdobeSongStd-Light.otf");
            // PDFNet.addFontSubst(PDFNet.e_GB1, "AdobeMingStd-Light.otf");

            PDFDraw draw = new PDFDraw(); // PDFDraw class is used to rasterize PDF pages.
            ObjSet hint_set = new ObjSet();



            // --------------------------------------------------------------------------------
            // The standard library does not include support for exporting PNG/TIFF.
            // The examples 1, 4, 5 and 6 use PDFDraw and tries to export pages to those
            // formats, thus it will fail if using the Standard library.
            // Please, uncomment parts of the code below if using the Full libraries.
            // --------------------------------------------------------------------------------
            
            
            // --------------------------------------------------------------------------------
            // Example 1) Convert the first page to PNG and TIFF at 92 DPI.
            // A three step tutorial to convert PDF page to an image.
            outputListener.println("Example 1:");
            try {
                // A) Open the PDF document.
                PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "tiger.pdf"));

                // Initialize the security handler, in case the PDF is encrypted.
                doc.initSecurityHandler();

                // B) The output resolution is set to 92 DPI.
                draw.setDPI(92);

                // C) Rasterize the first page in the document and save the result as PNG.
                Page pg = doc.getPage(1);
//                draw.export(pg, Utils.createExternalFile("tiger_92dpi.png").getAbsolutePath());
//                addToFileList("tiger_92dpi.png");
//                outputListener.println("Example 1: " + "tiger_92dpi.png" + ". Done.");

                // Export the same page as TIFF
//                draw.export(pg, Utils.createExternalFile("tiger_92dpi.tif").getAbsolutePath(), "TIFF");
//                addToFileList("tiger_92dpi.tif");

                doc.close();
            } catch (Exception e) {
                outputListener.println(e.getStackTrace());
            }

            // --------------------------------------------------------------------------------
            // Example 2) Convert the all pages in a given document to JPEG at
            // 72 DPI.
            outputListener.println("Example 2:");
            try {
                PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "newsletter.pdf"));
                // Initialize the security handler, in case the PDF is encrypted.
                doc.initSecurityHandler();

                draw.setDPI(72); // Set the output resolution is to 72 DPI.

                // Use optional encoder parameter to specify JPEG quality.
                Obj encoder_param = hint_set.createDict();
                encoder_param.putNumber("Quality", 80);

                // Traverse all pages in the document.
                for (PageIterator itr = doc.getPageIterator(); itr.hasNext();) {
                    Page current = (Page) (itr.next());
                    String filename = "newsletter" + current.getIndex() + ".jpg";
                    outputListener.println(filename);
                    draw.export(current, Utils.createExternalFile(filename).getAbsolutePath(), "JPEG", encoder_param);
                    addToFileList(filename);
                    outputListener.println("Example 2: " + filename + ". Done.");
                }

                doc.close();
            } catch (Exception e) {
                outputListener.println(e.getStackTrace());
            }

            // Examples 3-5
            try {
                // Common code for remaining samples.
                PDFDoc tiger_doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "tiger.pdf"));
                // Initialize the security handler, in case the PDF is/ encrypted.
                tiger_doc.initSecurityHandler();
                Page page = (Page) (tiger_doc.getPageIterator().next());

                // --------------------------------------------------------------------------------
                // Example 3) Convert the first page to raw bitmap. Also, rotate the
                // page 90 degrees and save the result as RAW.
                outputListener.println("Example 3:");
                draw.setDPI(100); // Set the output resolution is to 100 DPI.
                draw.setRotate(Page.e_90); // Rotate all pages 90 degrees
                                           // clockwise.

                Bitmap image = draw.getBitmap(page);
                int width = image.getWidth(), height = image.getHeight();
                int[] arr = new int[width * height];

                image.getPixels(arr, 0, width, 0, 0, width, height);

                // Save the raw RGB data to disk.


                draw.setRotate(Page.e_0); // Disable image rotation for remaining samples.
                outputListener.println("Example 3: Done.");
                
                // --------------------------------------------------------------------------------
                // Example 4) Convert PDF page to a fixed image size. Also illustrates some
                // other features in PDFDraw class such as rotation, image stretching, exporting
                // to grayscale, or monochrome.
                outputListener.println("Example 4:");
                // Initialize render 'gray_hint' parameter, that is used to control the
                // rendering process. In this case we tell the rasterizer to export the image as
                // 1 Bit Per Component (BPC) image.
                Obj mono_hint = hint_set.createDict();
                mono_hint.putNumber("BPC", 1);

                // SetImageSize can be used instead of SetDPI() to adjust page scaling
                // dynamically so that given image fits into a buffer of given dimensions.
                draw.setImageSize(1000, 1000); // Set the output image to be 1000 wide and 1000 pixels tall
//                draw.export(page, Utils.createExternalFile("tiger_1000x1000.png").getAbsolutePath(), "PNG", mono_hint);
//                addToFileList("tiger_1000x1000.png");
//                outputListener.println("Example 4: tiger_1000x1000.png. Done.");

                draw.setImageSize(200, 400); // Set the output image to be 200 wide and 300 pixels tall
                draw.setRotate(Page.e_180); // Rotate all pages 90 degrees clockwise.
                // 'gray_hint' tells the rasterizer to export the image as grayscale.
                Obj gray_hint = hint_set.createDict();
                gray_hint.putName("ColorSpace", "Gray");

//                draw.export(page,Utils.createExternalFile("tiger_200x400_rot180.png").getAbsolutePath(), "PNG", gray_hint);
//                addToFileList("tiger_200x400_rot180.png");
//                outputListener.println("Example 4: tiger_200x400_rot180.png. Done.");

                draw.setImageSize(400, 200, false); // The third parameter sets 'preserve-aspect-ratio' to false.
                draw.setRotate(Page.e_0); // Disable image rotation.
                draw.export(page, Utils.createExternalFile("tiger_400x200_stretch.jpg").getAbsolutePath(), "JPEG");
                addToFileList("tiger_400x200_stretch.jpg");
                outputListener.println("Example 4: tiger_400x200_stretch.jpg. Done.");

                // --------------------------------------------------------------------------------
                // Example 5) Zoom into a specific region of the page and rasterize the
                // area at 200 DPI and as a thumbnail (i.e. a 50x50 pixel image).
                outputListener.println("Example 5:");
                Rect zoom_rect = new Rect(216, 522, 330, 600);
                page.setCropBox(zoom_rect); // Set the page crop box.

                // Select the crop region to be used for drawing.
                draw.setPageBox(Page.e_crop);
                draw.setDPI(900); // Set the output image resolution to 900 DPI.
//                draw.export(page,Utils.createExternalFile("tiger_zoom_900dpi.png").getAbsolutePath(), "PNG");
//                addToFileList("tiger_zoom_900dpi.png");
//                outputListener.println("Example 5: tiger_zoom_900dpi.png. Done.");

                draw.setImageSize(50, 50); // Set the thumbnail to be 50x50/ pixel image.
//                draw.export(page, Utils.createExternalFile("tiger_zoom_50x50.png").getAbsolutePath(), "PNG");
//                addToFileList("tiger_zoom_50x50.png");
//                outputListener.println("Example 5: tiger_zoom_50x50.png. Done.");

                tiger_doc.close();
            } catch (Exception e) {
                outputListener.println(e.getStackTrace());
            }

            Obj cmyk_hint = hint_set.createDict();
            cmyk_hint.putName("ColorSpace", "CMYK");

/*
            // --------------------------------------------------------------------------------
            // Example 6) Convert the first PDF page to CMYK TIFF at 92 DPI.
            // A three step tutorial to convert PDF page to an image
            outputListener.println("Example 6:");
            try {
                // A) Open the PDF document.
                PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "tiger.pdf"));
                // Initialize the security handler, in case the PDF is encrypted.
                doc.initSecurityHandler();

                // B) The output resolution is set to 92 DPI.
                draw.setDPI(92);

                // C) Rasterize the first page in the document and save the result as TIFF.
                Page pg = doc.getPage(1);
                draw.export(pg, Utils.createExternalFile("out1.tif").getAbsolutePath(), "TIFF", cmyk_hint);
                addToFileList("out1.tif");
                outputListener.println("Example 6: Result saved in out1.tif");
            } catch (Exception e) {
                outputListener.println(e.getStackTrace());
            }
*/

            //--------------------------------------------------------------------------------
            // Example 8) PDFRasterizer can be used for more complex rendering tasks, such as
            // strip by strip or tiled document rendering. In particular, it is useful for
            // cases where you cannot simply modify the page crop box (interactive viewing,
            // parallel rendering). This example shows how you can rasterize the south-west
            // quadrant of a page.
            outputListener.println("Example 7:");
            try {
                // A) Open the PDF document.
                PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "tiger.pdf"));
                // Initialize the security handler, in case the PDF is encrypted.
                doc.initSecurityHandler();

                // B) Get the page matrix
                Page pg = doc.getPage(1);
                int box = Page.e_crop;
                Matrix2D mtx = pg.getDefaultMatrix(true, box, 0);
                // We want to render a quadrant, so use half of width and height
                double pg_w = pg.getPageWidth(box) / 2;
                double pg_h = pg.getPageHeight(box) / 2;

                // C) Scale matrix from PDF space to buffer space
                double dpi = 96.0;
                double scale = dpi / 72.0; // PDF space is 72 dpi
                double buf_w = Math.floor(scale * pg_w);
                double buf_h = Math.floor(scale * pg_h);
                int bytes_per_pixel = 4; // BGRA buffer
                mtx.translate(0, -pg_h); // translate by '-pg_h' since we want south-west quadrant
                mtx = (new Matrix2D(scale, 0, 0, scale, 0, 0)).multiply(mtx);

                // D) Rasterize page into memory buffer, according to our parameters
                PDFRasterizer rast = new PDFRasterizer();
                byte[] buf = rast.rasterize(pg, (int) buf_w, (int) buf_h, (int) buf_w * bytes_per_pixel, bytes_per_pixel, true, mtx, null);

                outputListener.println("Example 7: Successfully rasterized to memory buffer.");
            }
            catch (Exception e) {
                outputListener.println(e.getStackTrace());
            }

        } catch (PDFNetException e) {
            outputListener.println(e.getStackTrace());
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        printFooter(outputListener);
    }

}
