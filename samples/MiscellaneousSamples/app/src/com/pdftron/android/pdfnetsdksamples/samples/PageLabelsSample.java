//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PageLabel;
import com.pdftron.sdf.SDFDoc;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

/**
 * The sample illustrates how to work with PDF page labels.
 *
 * PDF page labels can be used to describe a page. This is used to
 * allow for non-sequential page numbering or the addition of arbitrary
 * labels for a page (such as the inclusion of Roman numerals at the
 * beginning of a book). PDFNet PageLabel object can be used to specify
 * the numbering style to use (for example, upper- or lower-case Roman,
 * decimal, and so forth), the starting number for the first page,
 * and an arbitrary prefix to be pre-appended to each number (for
 * example, "A-" to generate "A-1", "A-2", "A-3", and so forth.)
 */
public class PageLabelsSample extends PDFNetSample {

    public PageLabelsSample() {
        setTitle(R.string.sample_pagelabels_title);
        setDescription(R.string.sample_pagelabels_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        printHeader(outputListener);

        try {
            // -----------------------------------------------------------
            // Example 1: Add page labels to an existing or newly created PDF
            // document.
            // -----------------------------------------------------------
            {
                PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "newsletter.pdf"));
                doc.initSecurityHandler();

                // Create a page labeling scheme that starts with the first page in
                // the document (page 1) and is using uppercase roman numbering style.
                doc.setPageLabel(1, PageLabel.create(doc, PageLabel.e_roman_uppercase, "My Prefix ", 1));

                // Create a page labeling scheme that starts with the fourth page in
                // the document and is using decimal arabic numbering style.
                // Also the numeric portion of the first label should start with number
                // 4 (otherwise the first label would be "My Prefix 1").
                PageLabel L2 = PageLabel.create(doc, PageLabel.e_decimal, "My Prefix ", 4);
                doc.setPageLabel(4, L2);

                // Create a page labeling scheme that starts with the seventh page in
                // the document and is using alphabetic numbering style. The numeric
                // portion of the first label should start with number 1.
                PageLabel L3 = PageLabel.create(doc, PageLabel.e_alphabetic_uppercase, "My Prefix ", 1);
                doc.setPageLabel(7, L3);

                doc.save(Utils.createExternalFile("newsletter_with_pagelabels.pdf").getAbsolutePath(), SDFDoc.e_linearized, null);
                doc.close();
                addToFileList("newsletter_with_pagelabels.pdf");
                outputListener.println("Done. Result saved in newsletter_with_pagelabels.pdf...");
            }

            // -----------------------------------------------------------
            // Example 2: Read page labels from an existing PDF document.
            // -----------------------------------------------------------
            {
                PDFDoc doc = new PDFDoc(Utils.createExternalFile("newsletter_with_pagelabels.pdf").getAbsolutePath());
                doc.initSecurityHandler();

                PageLabel label;
                int page_num = doc.getPageCount();
                for (int i = 1; i <= page_num; ++i) {
                    outputListener.println("Page number: " + i);
                    label = doc.getPageLabel(i);
                    if (label.isValid()) {
                        outputListener.println(" Label: " + label.getLabelTitle(i));
                    } else {
                        outputListener.println(" No Label.");
                    }
                }
                doc.close();
            }

            // -----------------------------------------------------------
            // Example 3: Modify page labels from an existing PDF document.
            // -----------------------------------------------------------
            {
                PDFDoc doc = new PDFDoc(Utils.createExternalFile("newsletter_with_pagelabels.pdf").getAbsolutePath());
                doc.initSecurityHandler();

                // Remove the alphabetic labels from example 1.
                doc.removePageLabel(7);

                // Replace the Prefix in the decimal lables (from example 1).
                PageLabel label = doc.getPageLabel(4);
                if (label.isValid()) {
                    label.setPrefix("A");
                    label.setStart(1);
                }

                // Add a new label
                PageLabel new_label = PageLabel.create(doc, PageLabel.e_decimal, "B", 1);
                doc.setPageLabel(10, new_label); // starting from page 10.

                doc.save(Utils.createExternalFile("newsletter_with_pagelabels_modified.pdf").getAbsolutePath(), SDFDoc.e_linearized, null);
                addToFileList("newsletter_with_pagelabels_modified.pdf");
                outputListener.println("Done. Result saved in newsletter_with_pagelabels_modified.pdf...");

                int page_num = doc.getPageCount();
                for (int i = 1; i <= page_num; ++i) {
                    outputListener.print("Page number: " + i);
                    label = doc.getPageLabel(i);
                    if (label.isValid()) {
                        outputListener.println(" Label: " + label.getLabelTitle(i));
                    } else {
                        outputListener.println(" No Label.");
                    }
                }
                doc.close();
            }

            // -----------------------------------------------------------
            // Example 4: Delete all page labels in an existing PDF document.
            // -----------------------------------------------------------
            {
                PDFDoc doc = new PDFDoc(Utils.createExternalFile("newsletter_with_pagelabels.pdf").getAbsolutePath());
                doc.getRoot().erase("PageLabels");
                // ...
                doc.close();
            }
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }
        
        printFooter(outputListener);

    }

}
