//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementReader;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.TextExtractor;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

public class TextExtractSample extends PDFNetSample {

    private static OutputListener mOutputListener;
    
    public TextExtractSample() {
        setTitle(R.string.sample_textextract_title);
        setDescription(R.string.sample_textextract_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        mOutputListener = outputListener;
        
        printHeader(outputListener);
        

        boolean example1_basic     = true;
        boolean example2_xml       = true;
        boolean example3_wordlist  = true;
        boolean example4_advanced  = true;
        boolean example5_low_level = false;

        // Sample code showing how to use high-level text extraction APIs.
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "newsletter.pdf"));
            doc.initSecurityHandler();

            Page page = doc.getPage(1);
            if (page == null) {
                outputListener.println("Page not found.");
            }

            TextExtractor txt = new TextExtractor();
            txt.begin(page); // Read the page.
            // Other options you may want to consider...
            // txt.Begin(page, 0, TextExtractor.ProcessingFlags.e_no_dup_remove);
            // txt.Begin(page, 0, TextExtractor.ProcessingFlags.e_remove_hidden_text);
            // ...

            // Example 1. Get all text on the page in a single string.
            // Words will be separated with space or new line characters.
            if (example1_basic) {
                // Get the word count.
                outputListener.println("Word Count: " + txt.getWordCount());

                outputListener.println("\n\n- GetAsText --------------------------\n" + txt.getAsText());
                outputListener.println("-----------------------------------------------------------");
            }

            // Example 2. Get XML logical structure for the page.
            if (example2_xml) {
                String text = txt.getAsXML(TextExtractor.e_words_as_elements
                        | TextExtractor.e_output_bbox
                        | TextExtractor.e_output_style_info);
                outputListener.println("\n\n- GetAsXML  --------------------------\n" + text);
                outputListener.println("-----------------------------------------------------------");
            }

            // Example 3. Extract words one by one.
            if (example3_wordlist) {
                TextExtractor.Word word;
                for (TextExtractor.Line line = txt.getFirstLine(); line.isValid(); line = line.getNextLine()) {
                    for (word = line.getFirstWord(); word.isValid(); word = word.getNextWord()) {
                        outputListener.println(word.getString());
                    }
                }
                outputListener.println("-----------------------------------------------------------");
            }

            // Example 4. A more advanced text extraction example.
            // The output is XML structure containing paragraphs, lines, words,
            // as well as style and positioning information.
            if (example4_advanced) {
                Rect bbox;
                int cur_flow_id = -1, cur_para_id = -1;

                TextExtractor.Line line;
                TextExtractor.Word word;
                TextExtractor.Style s, line_style;

                // For each line on the page...
                for (line = txt.getFirstLine(); line.isValid(); line = line.getNextLine()) {
                    if (line.getNumWords() == 0)
                        continue;
                    if (cur_flow_id != line.getFlowID()) {
                        if (cur_flow_id != -1) {
                            if (cur_para_id != -1) {
                                cur_para_id = -1;
                                outputListener.println("</Para>");
                            }
                            outputListener.println("</Flow>");
                        }
                        cur_flow_id = line.getFlowID();
                        outputListener.println("<Flow id=\"" + cur_flow_id + "\">");
                    }

                    if (cur_para_id != line.getParagraphID()) {
                        if (cur_para_id != -1)
                            outputListener.println("</Para>");
                        cur_para_id = line.getParagraphID();
                        outputListener.println("<Para id=\"" + cur_para_id + "\">");
                    }

                    bbox = line.getBBox();
                    line_style = line.getStyle();
                    outputListener.println("<Line box=\"" + bbox.getX1() + ", "
                            + bbox.getY1() + ", " + bbox.getX2() + ", "
                            + bbox.getY2() + "\"");
                    printStyle(line_style);
                    outputListener.println(">");

                    // For each word in the line...
                    for (word = line.getFirstWord(); word.isValid(); word = word.getNextWord()) {
                        // Output the bounding box for the word.
                        bbox = word.getBBox();
                        outputListener.print("<Word box=\"" + bbox.getX1()
                                + ", " + bbox.getY1() + ", " + bbox.getX2()
                                + ", " + bbox.getY2() + "\"");

                        int sz = word.getStringLen();
                        if (sz == 0)
                            continue;

                        // If the word style is different from the parent style,
                        // output the new style.
                        s = word.getStyle();
                        if (!s.equals(line_style)) {
                            printStyle(s);
                        }

                        outputListener.println(">\n" + word.getString());
                        outputListener.println("</Word>");
                    }
                    outputListener.println("</Line>");
                }

                if (cur_flow_id != -1) {
                    if (cur_para_id != -1) {
                        cur_para_id = -1;
                        outputListener.println("</Para>");
                    }
                    outputListener.println("</Flow>");
                }
            }
            txt.destroy();
            doc.close();
            outputListener.println("Done.");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }
                
        // Sample code showing how to use low-level text extraction APIs.
        if (example5_low_level) {
            try {
                PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "newsletter.pdf"));
                doc.initSecurityHandler();

                // Example 1. Extract all text content from the document

                ElementReader reader = new ElementReader();
                // Read every page
                for (PageIterator itr = doc.getPageIterator(); itr.hasNext();) {
                    reader.begin((Page) (itr.next()));
                    DumpAllText(reader);
                    reader.end();
                }

                // Example 2. Extract text content based on the
                // selection rectangle.
                outputListener.print("\n----------------------------------------------------");
                outputListener.print("\nExtract text based on the selection rectangle.");
                outputListener.println("\n----------------------------------------------------");

                Page first_page = (Page) (doc.getPageIterator().next());
                String s1 = ReadTextFromRect(first_page, new Rect(27, 392, 563, 534), reader);
                outputListener.print("\nField 1: " + s1);

                s1 = ReadTextFromRect(first_page, new Rect(28, 551, 106, 623), reader);
                outputListener.print("\nField 2: " + s1);

                s1 = ReadTextFromRect(first_page, new Rect(208, 550, 387, 621), reader);
                outputListener.print("\nField 3: " + s1);

                // ...
                doc.close();
                outputListener.println("Done.");
            } catch (Exception e) {
                outputListener.println(e.getStackTrace());
            }
        }
    }
    
    static void printStyle(TextExtractor.Style s) {
        String r = String.valueOf(s.getColor()[0]);
        String g = String.valueOf(s.getColor()[1]);
        String b = String.valueOf(s.getColor()[2]);
        mOutputListener.print(" style=\"font-family:" + s.getFontName() + "; "
                + "font-size:" + s.getFontSize() + ";"
                + (s.isSerif() ? " sans-serif; " : " ") + "color: [" + r + "," + g + "," + b + "]\"");
    }

    // A utility method used to dump all text content in the console window.
    static void DumpAllText(ElementReader reader) throws PDFNetException {
        Element element;
        while ((element = reader.next()) != null) {
            switch (element.getType()) {
            case Element.e_text_begin:
                mOutputListener.println("\n--> Text Block Begin");
                break;
            case Element.e_text_end:
                mOutputListener.println("\n--> Text Block End");
                break;
            case Element.e_text:
                Rect bbox = element.getBBox();
                if (bbox == null)
                    continue;
                mOutputListener.println("\n--> BBox: " + bbox.getX1() + ", "
                        + bbox.getY1() + ", " + bbox.getX2() + ", "
                        + bbox.getY2());

                String arr = element.getTextString();
                mOutputListener.println(arr);
                break;
            case Element.e_text_new_line:
                mOutputListener.println("\n--> New Line");
                break;
            case Element.e_form: // Process form XObjects
                reader.formBegin();
                DumpAllText(reader);
                reader.end();
                break;
            }
        }
    }

    // A helper method for ReadTextFromRect
    static String RectTextSearch(ElementReader reader, Rect pos)
            throws PDFNetException {
        Element element;
        String srch_str = new String();
        while ((element = reader.next()) != null) {
            switch (element.getType()) {
            case Element.e_text:
                Rect bbox = element.getBBox();
                if (bbox == null)
                    continue;
                if (bbox.intersectRect(bbox, pos)) {
                    String arr = element.getTextString();
                    srch_str += arr;
                    srch_str += "\n"; // add a new line?
                }
                break;
            case Element.e_text_new_line:
                break;
            case Element.e_form: // Process form XObjects
                reader.formBegin();
                srch_str += RectTextSearch(reader, pos);
                reader.end();
                break;
            }
        }
        return srch_str;
    }

    // A utility method used to extract all text content from
    // a given selection rectangle. The recnagle coordinates are
    // expressed in PDF user/page coordinate system.
    static String ReadTextFromRect(Page page, Rect pos, ElementReader reader)
            throws PDFNetException {
        reader.begin(page);
        String srch_str = RectTextSearch(reader, pos);
        reader.end();
        return srch_str;
    }

}
