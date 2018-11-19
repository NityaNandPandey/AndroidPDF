//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.common.PDFNetException;
import com.pdftron.filters.Filter;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.ColorSpace;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.FileSpec;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.sdf.NameTree;
import com.pdftron.sdf.NameTreeIterator;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.SDFDoc;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

//-----------------------------------------------------------------------------------
//This sample illustrates how to create, extract, and manipulate PDF Portfolios
//(a.k.a. PDF Packages) using PDFNet SDK.
//-----------------------------------------------------------------------------------

public class PDFPackageSample extends PDFNetSample {

    public PDFPackageSample() {
        setTitle(R.string.sample_pdfpackage_title);
        setDescription(R.string.sample_pdfpackage_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        printHeader(outputListener);
        
        // Create a PDF Package.
        try
        {
            PDFDoc doc = new PDFDoc();
            addPackage(doc, Utils.getAssetTempFile(INPUT_PATH + "numbered.pdf").getAbsolutePath(), "My File 1");
            addPackage(doc, Utils.getAssetTempFile(INPUT_PATH + "newsletter.pdf").getAbsolutePath(), "My Newsletter...");
            addPackage(doc, Utils.getAssetTempFile(INPUT_PATH + "peppers.jpg").getAbsolutePath(), "An image");
            addCoverPage(doc);
            doc.save(Utils.createExternalFile("package.pdf").getAbsolutePath(), SDFDoc.e_linearized, null);
            addToFileList("package.pdf");
            doc.close();
        }
        catch(Exception e)
        {
            outputListener.println(e.getStackTrace());
        }
        
        // Extract parts from a PDF Package.
        try
        {
            PDFDoc doc = new PDFDoc(Utils.createExternalFile("package.pdf").getAbsolutePath());
            doc.initSecurityHandler();

            com.pdftron.sdf.NameTree files = NameTree.find(doc.getSDFDoc(), "EmbeddedFiles");
            if(files.isValid()) 
            { 
                // Traverse the list of embedded files.
                NameTreeIterator i = files.getIterator();
                for (int counter = 0; i.hasNext(); i.next(), ++counter) 
                {
                    String entry_name = i.key().getAsPDFText();
                    outputListener.println("Part: " + entry_name);
                    
                    FileSpec file_spec = new FileSpec(i.value());
                    Filter stm = file_spec.getFileData();
                    if (stm != null) 
                    {
                        String fname = Utils.getExternalFilesDirPath() + "/" + "extract_" + counter;
                        stm.writeToFile(fname, false);
                    }
                }
            }
            doc.close();
        } catch(Exception e) {
            outputListener.println(e.getStackTrace());
        }
        
        printFooter(outputListener);
    }

    static void addPackage(PDFDoc doc, String file, String desc) throws PDFNetException
    {
        NameTree files = NameTree.create(doc.getSDFDoc(), "EmbeddedFiles");
        FileSpec fs = FileSpec.create(doc, file, true);
        files.put(file.getBytes(), fs.getSDFObj());
        fs.getSDFObj().putText("Desc", desc);

        Obj collection = doc.getRoot().findObj("Collection");
        if (collection == null) collection = doc.getRoot().putDict("Collection");

        // You could here manipulate any entry in the Collection dictionary. 
        // For example, the following line sets the tile mode for initial view mode
        // Please refer to section '2.3.5 Collections' in PDF Reference for details.
        collection.putName("View", "T");
    }

    static void addCoverPage(PDFDoc doc) throws PDFNetException
    {
        // Here we dynamically generate cover page (please see ElementBuilder 
        // sample for more extensive coverage of PDF creation API).
        Page page = doc.pageCreate(new Rect(0, 0, 200, 200));

        ElementBuilder b = new ElementBuilder();
        ElementWriter w = new ElementWriter();
        w.begin(page);
        Font font = Font.create(doc.getSDFDoc(), Font.e_helvetica);
        w.writeElement(b.createTextBegin(font, 12));
        Element e = b.createTextRun("My PDF Collection");
        e.setTextMatrix(1, 0, 0, 1, 50, 96);
        e.getGState().setFillColorSpace(ColorSpace.createDeviceRGB());
        e.getGState().setFillColor(new ColorPt(1, 0, 0));
        w.writeElement(e);
        w.writeElement(b.createTextEnd());
        w.end();
        doc.pagePushBack(page);

        // Alternatively we could import a PDF page from a template PDF document
        // (for an example please see PDFPage sample project).
        // ...
    }
}
