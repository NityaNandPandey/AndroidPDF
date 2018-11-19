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

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

public class ElementReaderSample extends PDFNetSample {

    private static OutputListener mOutputListener;
    
    public ElementReaderSample() {
        setTitle(R.string.sample_elementreader_title);
        setDescription(R.string.sample_elementreader_description);
    }
    
    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        mOutputListener = outputListener;
        
        printHeader(outputListener);
        
        try {   // Extract text data from all pages in the document
            
            outputListener.println("Sample 1 - Extract text data from all pages in the document.");
            outputListener.println("Opening the input pdf...");

            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "newsletter.pdf"));
            doc.initSecurityHandler();

            //int pgnum = doc.getPageCount();
            
            PageIterator itr;
            ElementReader page_reader = new ElementReader();

            for (itr = doc.getPageIterator(); itr.hasNext(); ) {    //  Read every page
                page_reader.begin((Page)(itr.next()));
                ProcessElements(page_reader);
                page_reader.end();
            }

            //Close the open document to free up document memory sooner.
            doc.close();
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }
        
        printFooter(outputListener);
    }
    
    static void ProcessElements(ElementReader reader) throws PDFNetException
    {
        for (Element element = reader.next(); element != null; element = reader.next()) {   // Read page contents
            if (element.getBBox() != null) {
                switch (element.getType()) {
                case Element.e_path:    // Process path data...
                    {
                        //PathData data = element.getPathData();
                        //byte[] operators = data.getOperators();
                        //double[] points = data.getPoints();
                    }
                    break; 
                case Element.e_text:    // Process text strings...
                    {
                        byte[] data = element.getTextData();
                        mOutputListener.println(new String(data) + ' ');
                    }
                    break;
                case Element.e_form:    // Process form XObjects
                    {
                        reader.formBegin(); 
                        ProcessElements(reader);
                        reader.end(); 
                    }
                    break; 
                }
            }
        }
    }

}
