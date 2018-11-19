//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.Rect;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

public class RectSample extends PDFNetSample {

    public RectSample() {
        setTitle(R.string.sample_rect_title);
        setDescription(R.string.sample_rect_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        printHeader(outputListener);
        
        try // Test - Adjust the position of content within the page.
        {
            outputListener.println("--------------------");
            outputListener.println("Opening the input pdf...");

            PDFDoc input_doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "tiger.pdf"));
            input_doc.initSecurityHandler();

            PageIterator pg_itr1 = input_doc.getPageIterator();

            Rect media_box = ((Page) (pg_itr1.next())).getMediaBox();

            media_box.setX1(media_box.getX1() - 200); // translate the page 200 units (1 uint = 1/72 inch)
            media_box.setX2(media_box.getX2() - 200);

            media_box.update();

            input_doc.save(Utils.createExternalFile("tiger_shift.pdf").getAbsolutePath(), 0, null);
            input_doc.close();
            addToFileList("tiger_shift.pdf");
            outputListener.println("Result saved in tiger_shift...");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }
        
        printFooter(outputListener);
    }

}
