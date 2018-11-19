//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.sdf.SDFDoc;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

public class PDFPageSample extends PDFNetSample {

    public PDFPageSample() {
        setTitle(R.string.sample_pdfpage_title);
        setDescription(R.string.sample_pdfpage_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        printHeader(outputListener);

        // Sample 1 - Split a PDF document into multiple pages
        try {
            outputListener.println("--------------------");
            outputListener.println("Sample 1 - Delete every second page...");
            outputListener.println("Opening the input pdf...");
            PDFDoc in_doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "newsletter.pdf"));
            in_doc.initSecurityHandler();

            int page_num = in_doc.getPageCount();
            for (int i = 1; i <= page_num; ++i) {
                PDFDoc new_doc = new PDFDoc();
                new_doc.insertPages(0, in_doc, i, i, PDFDoc.e_none, null);
                new_doc.save(Utils.createExternalFile("newsletter_split_page_" + i + ".pdf").getAbsolutePath(), 
                        SDFDoc.e_remove_unused, null);
                outputListener.println("Done. Result saved in newsletter_split_page_" + i + ".pdf");
                addToFileList("newsletter_split_page_" + i + ".pdf");
                new_doc.close();
            }
            in_doc.close();
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // Sample 2 - Merge several PDF documents into one
        try {
            outputListener.println("--------------------");
            outputListener.println("Sample 2 - Merge several PDF documents into one...");
            PDFDoc new_doc = new PDFDoc();
            new_doc.initSecurityHandler();

            int page_num = 15;
            for (int i = 1; i <= page_num; ++i) {
                outputListener.println("Opening newsletter_split_page_" + i + ".pdf");
                PDFDoc in_doc = new PDFDoc(Utils.createExternalFile("newsletter_split_page_" + i + ".pdf").getAbsolutePath());
                new_doc.insertPages(i, in_doc, 1, in_doc.getPageCount(), PDFDoc.e_none, null);
                in_doc.close();
            }
            new_doc.save(Utils.createExternalFile("newsletter_merge_pages.pdf").getAbsolutePath(), SDFDoc.e_remove_unused, null);
            outputListener.println("Done. Result saved in newsletter_merge_pages.pdf...");
            addToFileList("newsletter_merge_pages.pdf");
            new_doc.close();
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // Sample 3 - Delete every second page
        try {
            outputListener.println("--------------------");
            outputListener.println("Sample 3 - Delete every second page...");
            outputListener.println("Opening the input pdf...");
            PDFDoc in_doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "newsletter.pdf"));
            in_doc.initSecurityHandler();

            int page_num = in_doc.getPageCount();
            while (page_num >= 1) {
                PageIterator itr = in_doc.getPageIterator(page_num);
                in_doc.pageRemove(itr);
                page_num -= 2;
            }

            in_doc.save(Utils.createExternalFile("newsletter_page_remove.pdf").getAbsolutePath(), 0, null);
            outputListener.println("Done. Result saved in newsletter_page_remove.pdf...");
            addToFileList("newsletter_page_remove.pdf");

            // Close the open document to free up document memory sooner than waiting for the
            // garbage collector
            in_doc.close();
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // Sample 4 - Inserts a page from one document at different
        // locations within another document
        try {
            outputListener.println("--------------------");
            outputListener.println("Sample 4 - Insert a page at different locations...");
            outputListener.println("Opening the input pdf...");

            PDFDoc in1_doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "newsletter.pdf"));
            in1_doc.initSecurityHandler();

            PDFDoc in2_doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "fish.pdf"));
            in2_doc.initSecurityHandler();

            PageIterator src_page_itr = in2_doc.getPageIterator();
            Page src_page = (Page) (src_page_itr.next());

            PageIterator dst_page_itr = in1_doc.getPageIterator();
            int page_num = 1;
            while (dst_page_itr.hasNext()) {
                if (page_num++ % 3 == 0) {
                    in1_doc.pageInsert(dst_page_itr, src_page);
                }
                dst_page_itr.next();
            }

            in1_doc.save(Utils.createExternalFile("newsletter_page_insert.pdf").getAbsolutePath(), 0, null);
            outputListener.println("Done. Result saved in newsletter_page_insert.pdf...");
            addToFileList("newsletter_page_insert.pdf");

            // Close the open documents to free up document memory sooner than waiting for the
            // garbage collector
            in1_doc.close();
            in2_doc.close();
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // Sample 5 - Replicate pages within a single document
        try {
            outputListener.println("--------------------");
            outputListener.println("Sample 5 - Replicate pages within a single document...");
            outputListener.println("Opening the input pdf...");
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "newsletter.pdf"));
            doc.initSecurityHandler();

            // Replicate the cover page three times (copy page #1 and place it before the
            // seventh page in the document page sequence)
            Page cover = (Page) (doc.getPage(1));
            PageIterator p7 = doc.getPageIterator(7);
            doc.pageInsert(p7, cover);
            doc.pageInsert(p7, cover);
            doc.pageInsert(p7, cover);

            // Replicate the cover page two more times by placing it before and after
            // existing pages.
            doc.pagePushFront(cover);
            doc.pagePushBack(cover);

            doc.save(Utils.createExternalFile("newsletter_page_clone.pdf").getAbsolutePath(), 0, null);
            outputListener.println("Done. Result saved in newsletter_page_clone.pdf...");
            addToFileList("newsletter_page_clone.pdf");

            // Close the open document to free up document memory sooner than waiting for the
            // garbage collector
            doc.close();
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // Sample 6 - Use ImportPages() in order to copy multiple pages at once
        // in order to preserve shared resources between pages (e.g. images, fonts,
        // colorspaces, etc.)
        try {
            outputListener.println("--------------------");
            outputListener.println("Sample 6 - Preserving shared resources using ImportPages...");
            outputListener.println("Opening the input pdf...");
            PDFDoc in_doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "newsletter.pdf"));
            in_doc.initSecurityHandler();

            PDFDoc new_doc = new PDFDoc();

            Page[] copy_pages = new Page[in_doc.getPageCount()];
            int j = 0;
            for (PageIterator itr = in_doc.getPageIterator(); itr.hasNext(); j++) {
                copy_pages[j] = (Page) (itr.next());
            }

            Page[] imported_pages = new_doc.importPages(copy_pages);
            for (int i = 0; i < imported_pages.length; ++i) {
                new_doc.pagePushFront(imported_pages[i]); // Order pages in reverse order.
                // Use pushBackPage() if you would like to preserve the same order.
            }

            new_doc.save(Utils.createExternalFile("newsletter_import_pages.pdf").getAbsolutePath(), 0, null);
            addToFileList("newsletter_import_pages.pdf");

            // Close the open documents to free up document memory sooner than waiting for the
            // garbage collector
            in_doc.close();
            new_doc.close();

            outputListener.println("Done. Result saved in newsletter_import_pages.pdf...");
            outputListener.println("Note that the output file size is less than half the size");
            outputListener.println("of the file produced using individual page copy operations");
            outputListener.println("between two documents");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        printFooter(outputListener);
    }

}
