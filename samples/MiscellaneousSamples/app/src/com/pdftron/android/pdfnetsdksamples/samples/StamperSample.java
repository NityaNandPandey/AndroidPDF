//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.Image;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageSet;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.Stamper;
import com.pdftron.sdf.SDFDoc;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

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
public class StamperSample extends PDFNetSample {
    
    public StamperSample() {
        setTitle(R.string.sample_stamper_title);
        setDescription(R.string.sample_stamper_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        printHeader(outputListener);

        String input_filename = "newsletter.pdf";
        
        //--------------------------------------------------------------------------------
        // Example 1) Add text stamp to all pages, then remove text stamp from odd pages. 
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + input_filename));
            doc.initSecurityHandler();
            Stamper s = new Stamper(Stamper.e_relative_scale, 0.5, 0.5);
            s.setAlignment(Stamper.e_horizontal_center, Stamper.e_vertical_center);
            ColorPt red = new ColorPt(1, 0, 0); // set text color to red
            s.setFontColor(red);
            String msg = "If you are reading this\nthis is an even page";
            PageSet ps = new PageSet(1, doc.getPageCount());
            s.stampText(doc, msg, ps);
            // delete all text stamps in even pages
            PageSet ps1 = new PageSet(1, doc.getPageCount(), PageSet.e_odd);
            Stamper.deleteStamps(doc, ps1);

            doc.save(Utils.createExternalFile(input_filename + ".ex1.pdf").getAbsolutePath(),
                    SDFDoc.e_linearized, null);
            addToFileList(input_filename + ".ex1.pdf");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        //--------------------------------------------------------------------------------
        // Example 2) Add Image stamp to first 2 pages. 
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + input_filename));
            doc.initSecurityHandler();

            Stamper s = new Stamper(Stamper.e_relative_scale, .05, .05);
            Image img = Image.create(doc, Utils.getAssetTempFile(INPUT_PATH + "peppers.jpg").getAbsolutePath());
            s.setSize(Stamper.e_relative_scale, 0.5, 0.5);
            // set position of the image to the center, left of PDF pages
            s.setAlignment(Stamper.e_horizontal_left, Stamper.e_vertical_center);
            ColorPt pt = new ColorPt(0, 0, 0, 0);
            s.setFontColor(pt);
            s.setRotation(180);
            s.setAsBackground(false);
            // only stamp first 2 pages
            PageSet ps = new PageSet(1, 2);
            s.stampImage(doc, img, ps);

            doc.save(Utils.createExternalFile(input_filename + ".ex2.pdf").getAbsolutePath(),
                    SDFDoc.e_linearized, null);
            addToFileList(input_filename + ".ex2.pdf");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        //--------------------------------------------------------------------------------
        // Example 3) Add Page stamp to all pages. 
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + input_filename));
            doc.initSecurityHandler();

            PDFDoc fish_doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "fish.pdf"));
            fish_doc.initSecurityHandler();

            Stamper s = new Stamper(Stamper.e_relative_scale, 0.5, 0.5);
            Page src_page = fish_doc.getPage(1);
            Rect page_one_crop = src_page.getCropBox();
            // set size of the image to 10% of the original while keep the old
            // aspect ratio
            s.setSize(Stamper.e_absolute_size, page_one_crop.getWidth() * 0.1, -1);
            s.setOpacity(0.4);
            s.setRotation(-67);
            // put the image at the bottom right hand corner
            s.setAlignment(Stamper.e_horizontal_right, Stamper.e_vertical_bottom);
            PageSet ps = new PageSet(1, doc.getPageCount());
            s.stampPage(doc, src_page, ps);

            doc.save(Utils.createExternalFile(input_filename + ".ex3.pdf").getAbsolutePath(),
                    SDFDoc.e_linearized, null);
            addToFileList(input_filename + ".ex3.pdf");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        //--------------------------------------------------------------------------------
        // Example 4) Add Image stamp to first 20 odd pages.
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + input_filename));
            doc.initSecurityHandler();

            Stamper s = new Stamper(Stamper.e_absolute_size, 20, 20);
            s.setOpacity(1);
            s.setRotation(45);
            s.setAsBackground(true);
            s.setPosition(30, 40);
            Image img = Image.create(doc, Utils.getAssetTempFile(INPUT_PATH + "peppers.jpg").getAbsolutePath());
            PageSet ps = new PageSet(1, 20, PageSet.e_odd);
            s.stampImage(doc, img, ps);

            doc.save(Utils.createExternalFile(input_filename + ".ex4.pdf").getAbsolutePath(),
                    SDFDoc.e_linearized, null);
            addToFileList(input_filename + ".ex4.pdf");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        //--------------------------------------------------------------------------------
        // Example 5) Add text stamp to first 20 even pages
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + input_filename));
            doc.initSecurityHandler();

            Stamper s = new Stamper(Stamper.e_relative_scale, .05, .05);
            s.setPosition(0, 0);
            s.setOpacity(0.7);
            s.setRotation(90);
            s.setSize(Stamper.e_font_size, 80, -1);
            s.setTextAlignment(Stamper.e_align_center);
            PageSet ps = new PageSet(1, 20, PageSet.e_even);
            s.stampText(doc, "Goodbye\nMoon", ps);

            doc.save(Utils.createExternalFile(input_filename + ".ex5.pdf").getAbsolutePath(),
                    SDFDoc.e_linearized, null);
            addToFileList(input_filename + ".ex5.pdf");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        //--------------------------------------------------------------------------------
        // Example 6) Add first page as stamp to all even pages
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + input_filename));
            doc.initSecurityHandler();

            PDFDoc fish_doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "fish.pdf"));
            fish_doc.initSecurityHandler();

            Stamper s = new Stamper(Stamper.e_relative_scale, 0.3, 0.3);
            s.setOpacity(1);
            s.setRotation(270);
            s.setAsBackground(true);
            s.setPosition(0.5, 0.5, true);
            s.setAlignment(Stamper.e_horizontal_left, Stamper.e_vertical_bottom);
            Page page_one = fish_doc.getPage(1);
            PageSet ps = new PageSet(1, doc.getPageCount(), PageSet.e_even);
            s.stampPage(doc, page_one, ps);

            doc.save(Utils.createExternalFile(input_filename + ".ex6.pdf").getAbsolutePath(),
                    SDFDoc.e_linearized, null);
            addToFileList(input_filename + ".ex6.pdf");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        //--------------------------------------------------------------------------------
        // Example 7) Add image stamp at top right corner in every pages
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + input_filename));
            doc.initSecurityHandler();

            Stamper s = new Stamper(Stamper.e_relative_scale, .1, .1);
            s.setOpacity(0.8);
            s.setRotation(135);
            s.setAsBackground(false);
            s.showsOnPrint(false);
            s.setAlignment(Stamper.e_horizontal_left, Stamper.e_vertical_top);
            s.setPosition(10, 10);

            Image img = Image.create(doc, Utils.getAssetTempFile(INPUT_PATH + "peppers.jpg").getAbsolutePath());
            PageSet ps = new PageSet(1, doc.getPageCount(), PageSet.e_all);
            s.stampImage(doc, img, ps);

            doc.save(Utils.createExternalFile(input_filename + ".ex7.pdf").getAbsolutePath(),
                    SDFDoc.e_linearized, null);
            addToFileList(input_filename + ".ex7.pdf");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        //--------------------------------------------------------------------------------
        // Example 8) Add Text stamp to first 2 pages, and image stamp to first page.
        //            Because text stamp is set as background, the image is top of the text
        //            stamp. Text stamp on the first page is not visible.
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + input_filename));
            doc.initSecurityHandler();

            Stamper s = new Stamper(Stamper.e_relative_scale, 0.07, -0.1);
            s.setAlignment(Stamper.e_horizontal_right, Stamper.e_vertical_bottom);
            s.setAlignment(Stamper.e_horizontal_center, Stamper.e_vertical_top);
            s.setFont(Font.create(doc, Font.e_courier, true));
            ColorPt red = new ColorPt(1, 0, 0, 0);
            s.setFontColor(red); // set color to red
            s.setTextAlignment(Stamper.e_align_right);
            s.setAsBackground(true); // set text stamp as background
            PageSet ps = new PageSet(1, 2);
            s.stampText(doc, "This is a title!", ps);

            Image img = Image.create(doc, Utils.getAssetTempFile(INPUT_PATH + "peppers.jpg").getAbsolutePath());
            s.setAsBackground(false); // set image stamp as foreground
            PageSet first_page_ps = new PageSet(1);
            s.stampImage(doc, img, first_page_ps);

            doc.save(Utils.createExternalFile(input_filename + ".ex8.pdf").getAbsolutePath(),
                    SDFDoc.e_linearized, null);
            addToFileList(input_filename + ".ex8.pdf");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }
        
        printFooter(outputListener);
    }

}
