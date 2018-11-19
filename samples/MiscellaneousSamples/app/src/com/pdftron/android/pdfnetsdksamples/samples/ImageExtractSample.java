//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import java.util.ArrayList;

import com.pdftron.common.Matrix2D;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementReader;
import com.pdftron.pdf.Image;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.sdf.DictIterator;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.SDFDoc;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

///-----------------------------------------------------------------------------------
/// This sample illustrates one approach to PDF image extraction 
/// using PDFNet.
/// 
/// Note: Besides direct image export, you can also convert PDF images 
/// to Java image, or extract uncompressed/compressed image data directly 
/// using element.GetImageData() (e.g. as illustrated in ElementReaderAdv 
/// sample project).
///-----------------------------------------------------------------------------------

public class ImageExtractSample extends PDFNetSample {

    private static OutputListener mOutputListener;
    private static ArrayList<String> mFiles = new ArrayList<String>();
    
    public ImageExtractSample() {
        setTitle(R.string.sample_imageextract_title);
        setDescription(R.string.sample_imageextract_description);
        
        // The standard library does not support exporting to 
        // PNG/TIFF formats, thus trying to export the PDF to
        // PNG or TIFF will fail. Please, comment out this call
        // if using the full library.
        // DisableRun();
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        mOutputListener = outputListener;
        mFiles.clear();
        
        printHeader(outputListener);
        
        if (!isEnabled()) {
            outputListener.println("Sample is disabled. Please, check source code for more information.");
            printFooter(outputListener);
            return;
        }
        
        // Example 1:
        // Extract images by traversing the display list for
        // every page. With this approach it is possible to obtain
        // image positioning information and DPI.
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "newsletter.pdf"));
            doc.initSecurityHandler();

            ElementReader reader = new ElementReader();
            // Read every page
            for (PageIterator itr = doc.getPageIterator(); itr.hasNext();) {
                reader.begin((Page) (itr.next()));
                ImageExtract(reader);
                reader.end();
            }

            doc.close();
            outputListener.println("Done example 1...");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // Example 2:
        // Extract images by scanning the low-level document.
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "newsletter.pdf"));

            doc.initSecurityHandler();
            image_counter = 0;

            SDFDoc cos_doc = doc.getSDFDoc();
            long num_objs = cos_doc.xRefSize();
            for (int i = 1; i < num_objs; ++i) {
                Obj obj = cos_doc.getObj(i);
                if (obj != null && !obj.isFree() && obj.isStream()) {
                    // Process only images
                    DictIterator itr = obj.find("Type");
                    if (!itr.hasNext() || !itr.value().getName().equals("XObject"))
                        continue;

                    itr = obj.find("Subtype");
                    if (!itr.hasNext() || !itr.value().getName().equals("Image"))
                        continue;

                    Image image = new Image(obj);

                    outputListener.println("\n-. Image: " + (++image_counter));
                    outputListener.println("\n    Width: " + image.getImageWidth());
                    outputListener.println("\n    Height: " + image.getImageHeight());
                    outputListener.println("\n    BPC: " + image.getBitsPerComponent());

                    String fname = "image_extract2_" + image_counter;
                    //String path = Utils.createExternalFile(fname).getAbsolutePath();
                    //image.export(path);

                    // String path = fname + ".tif";
                    // image.exportAsTiff(path);

                    //String path = fname + ".png";
                    String path = Utils.createExternalFile(fname + ".png").getAbsolutePath();
                    image.exportAsPng(path);
                    mFiles.add(fname + ".png");
                }
            }

            doc.close();
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }
        
        for (String file : mFiles) {
            addToFileList(file);
        }

        printFooter(outputListener);
    }
    
    static int image_counter = 0;

    static void ImageExtract(ElementReader reader) throws PDFNetException {
        Element element;
        while ((element = reader.next()) != null) {
            switch (element.getType()) {
            case Element.e_image:
            case Element.e_inline_image: {
                mOutputListener.println("\n--> Image: " + (++image_counter));
                mOutputListener.println("\n    Width: " + element.getImageWidth());
                mOutputListener.println("\n    Height: " + element.getImageHeight());
                mOutputListener.println("\n    BPC: " + element.getBitsPerComponent());

                Matrix2D ctm = element.getCTM();
                double x2 = 1, y2 = 1;
                ctm.multPoint(x2, y2);
                mOutputListener.println("\n    Coords: x1=" + ctm.getH() + ", y1=" + ctm.getV() + ", x2=" + x2 + ", y2=" + y2);

                if (element.getType() == Element.e_image) {
                    Image image = new Image(element.getXObject());

                    String fname = "image_extract1_" + image_counter;

                    //image.export(Utils.createExternalFile(fname).getAbsolutePath());

                    // String path2 = Utils.createExternalFile(fname + ".tif").getAbsolutePath();
                    // image.exportAsTiff(path2);
                    // mFiles.add(fname + ".tif");

                    String path3 = Utils.createExternalFile(fname + ".png").getAbsolutePath();
                    image.exportAsPng(path3);
                    mFiles.add(fname + ".png");
                }
            }
                break;
            case Element.e_form: // Process form XObjects
                reader.formBegin();
                ImageExtract(reader);
                reader.end();
                break;
            }
        }
    }

}
