//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import java.io.IOException;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.Highlights;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.TextSearch;
import com.pdftron.pdf.TextSearchResult;
import com.pdftron.sdf.SDFDoc;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

public class TextSearchSample extends PDFNetSample {

    public TextSearchSample() {
        setTitle(R.string.sample_textsearch_title);
        setDescription(R.string.sample_textsearch_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        printHeader(outputListener);
        
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "credit card numbers.pdf"));
            doc.initSecurityHandler();

            TextSearch txt_search = new TextSearch();
            int mode = TextSearch.e_whole_word | TextSearch.e_page_stop;

            String pattern = "joHn sMiTh";

            // PDFDoc doesn't allow simultaneous access from different threads.
            // If this document could be used from other threads (e.g., the rendering
            // thread inside PDFView/PDFViewCtrl, if used), it is good practice to lock it.
            // Notice: don't forget to call doc.Unlock() to avoid deadlock.
            doc.lock();

            // call Begin() method to initialize the text search.
            txt_search.begin(doc, pattern, mode, -1, -1);

            int step = 0;

            // call Run() method iteratively to find all matching instances.
            while (true) {
                TextSearchResult result = txt_search.run();

                if (result.getCode() == TextSearchResult.e_found) {
                    if (step == 0) {
                        // step 0: found "John Smith"
                        // note that, here, 'ambient_string' and 'hlts' are not
                        // written to, as 'e_ambient_string' and 'e_highlight' are not set.
                        outputListener.println(result.getResultStr() + "'s credit card number is:");

                        // now switch to using regular expressions to find
                        // John's credit card number
                        mode = txt_search.getMode();
                        mode |= TextSearch.e_reg_expression | TextSearch.e_highlight;
                        txt_search.setMode(mode);
                        String new_pattern = "\\d{4}-\\d{4}-\\d{4}-\\d{4}"; // or "(\\d{4}-){3}\\d{4}"
                        txt_search.setPattern(new_pattern);

                        step = step + 1;
                    } else if (step == 1) {
                        // step 1: found John's credit card number
                        outputListener.println("  " + result.getResultStr());

                        // note that, here, 'hlts' is written to, as
                        // 'e_highlight' has been set.
                        // output the highlight info of the credit card number
                        Highlights hlts = result.getHighlights();
                        hlts.begin(doc);
                        while (hlts.hasNext()) {
                            outputListener.println("The current highlight is from page: " + hlts.getCurrentPageNumber());
                            hlts.next();
                        }

                        // see if there is an AMEX card number
                        String new_pattern = "\\d{4}-\\d{6}-\\d{5}";
                        txt_search.setPattern(new_pattern);

                        step = step + 1;
                    } else if (step == 2) {
                        // found an AMEX card number
                        outputListener.println("\nThere is an AMEX card number: ");
                        outputListener.println("  " + result.getResultStr());

                        // change mode to find the owner of the credit card;
                        // supposedly, the owner's name proceeds the number
                        mode = txt_search.getMode();
                        mode |= TextSearch.e_search_up;
                        txt_search.setMode(mode);
                        String new_pattern = "[A-z]++ [A-z]++";
                        txt_search.setPattern(new_pattern);

                        step = step + 1;
                    } else if (step == 3) {
                        // found the owner's name of the AMEX card
                        outputListener.println("Is the owner's name:");
                        outputListener.println("  " + result.getResultStr() + "?");

                        // add a link annotation based on the location of the
                        // found instance
                        Highlights hlts = result.getHighlights();
                        hlts.begin(doc);
                        while (hlts.hasNext()) {
                            Page cur_page = doc.getPage(hlts.getCurrentPageNumber());
                            double[] q = hlts.getCurrentQuads();
                            int quad_count = q.length / 8;
                            for (int i = 0; i < quad_count; ++i) {
                                // assume each quad is an axis-aligned rectangle
                                int offset = 8 * i;
                                double x1 = Math.min(Math.min(Math.min(q[offset + 0], q[offset + 2]), q[offset + 4]), q[offset + 6]);
                                double x2 = Math.max(Math.max(Math.max(q[offset + 0], q[offset + 2]), q[offset + 4]), q[offset + 6]);
                                double y1 = Math.min(Math.min(Math.min(q[offset + 1], q[offset + 3]), q[offset + 5]), q[offset + 7]);
                                double y2 = Math.max(Math.max(Math.max(q[offset + 1], q[offset + 3]), q[offset + 5]), q[offset + 7]);
                                com.pdftron.pdf.annots.Link hyper_link = com.pdftron.pdf.annots.Link
                                        .create(doc, new Rect(x1, y1, x2, y2), Action.createURI(doc, "http://www.pdftron.com"));
                                cur_page.annotPushBack(hyper_link);
                            }
                            hlts.next();
                        }
                        doc.save(Utils.createExternalFile("credit card numbers_linked.pdf").getAbsolutePath(), SDFDoc.e_linearized, null);
                        addToFileList("credit card numbers_linked.pdf");
                        break;
                    }
                } else if (result.getCode() == TextSearchResult.e_page) {
                    // you can update your UI here, if needed
                } else {
                    break;
                }
            }

            doc.unlock();
            doc.close();
        } catch (PDFNetException e) {
            outputListener.println(e.getStackTrace());
        } catch (IOException e) {
            outputListener.println(e.getStackTrace());
        }
        
        printFooter(outputListener);

    }

}
