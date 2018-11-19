//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import java.io.File;

import com.pdftron.fdf.FDFDoc;
import com.pdftron.fdf.FDFField;
import com.pdftron.fdf.FDFFieldIterator;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.FieldIterator;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.sdf.SDFDoc;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

public class FDFSample extends PDFNetSample {

    public FDFSample() {
        setTitle(R.string.sample_fdf_title);
        setDescription(R.string.sample_fdf_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        printHeader(outputListener);
        
        // Example 1)
        // Iterate over all form fields in the document. Display all field names.
        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "form1.pdf"));
            doc.initSecurityHandler();

            for (FieldIterator itr = doc.getFieldIterator(); itr.hasNext();) {
                Field current = (Field)(itr.next());
                outputListener.println("Field name: " + current.getName());
                outputListener.println("Field partial name: " + current.getPartialName());

                outputListener.print("Field type: ");
                int type = current.getType();
                switch (type) {
                case Field.e_button: outputListener.println("Button"); break;
                case Field.e_text: outputListener.println("Text"); break;
                case Field.e_choice: outputListener.println("Choice"); break;
                case Field.e_signature: outputListener.println("Signature"); break;
                }

                outputListener.println("------------------------------");
            }

            doc.close();
            outputListener.println("Done example 1.");

        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // Example 2) Import XFDF into FDF, then merge data from FDF into PDF
        try {
            // XFDF to FDF
            // form fields
            outputListener.println("Import form field data from XFDF to FDF.");
            
            FDFDoc fdf_doc1 = FDFDoc.createFromXFDF(Utils.getAssetTempFile(INPUT_PATH + "form1_data.xfdf").getAbsolutePath());
            fdf_doc1.save(Utils.createExternalFile("form1_data.fdf").getAbsolutePath());
            addToFileList("form1_data.fdf");
            
            // annotations
            outputListener.println("Import annotations from XFDF to FDF.");
            
            FDFDoc fdf_doc2 = FDFDoc.createFromXFDF(Utils.getAssetTempFile(INPUT_PATH + "form1_annots.xfdf").getAbsolutePath());
            fdf_doc2.save(Utils.createExternalFile("form1_annots.fdf").getAbsolutePath());
            addToFileList("form1_annots.fdf");
            
            // FDF to PDF
            // form fields
            outputListener.println("Merge form field data from FDF.");
            
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "form1.pdf"));
            doc.initSecurityHandler();
            doc.fdfMerge(fdf_doc1);
            
            // To use PDFNet form field appearance generation instead of relying on 
            // Acrobat, uncomment the following two lines:
            //doc.refreshFieldAppearances();
            //doc.getAcroForm().putBool("NeedAppearances", false);

            doc.save(Utils.createExternalFile("form1_filled.pdf").getAbsolutePath(), SDFDoc.e_linearized, null);
            addToFileList("form1_filled.pdf");
            
            // annotations
            outputListener.println("Merge annotations from FDF.");
            
            doc.fdfMerge(fdf_doc2);
            doc.save(Utils.createExternalFile("form1_filled_with_annots.pdf").getAbsolutePath(), SDFDoc.e_linearized, null);
            addToFileList("form1_filled_with_annots.pdf");
            doc.close();
            outputListener.println("Done example 2.");
            
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // Example 3) Extract data from PDF to FDF, then export FDF as XFDF
        try {
            // PDF to FDF
            File form1_filled = Utils.createExternalFile("form1_filled_with_annots.pdf");
            PDFDoc in_doc = new PDFDoc(form1_filled.getAbsolutePath());
            in_doc.initSecurityHandler();
            
            // form fields only
            outputListener.println("Extract form fields data to FDF.");
            
            FDFDoc doc_fields = in_doc.fdfExtract(PDFDoc.e_forms_only);
            doc_fields.setPDFFileName(form1_filled.getAbsolutePath());
            doc_fields.save(Utils.createExternalFile("form1_filled_data.fdf").getAbsolutePath());
            addToFileList("form1_filled_data.fdf");
            
            // annotations only
            outputListener.println("Extract annotations from FDF.");
            
            FDFDoc doc_annots = in_doc.fdfExtract(PDFDoc.e_annots_only);
            doc_annots.setPDFFileName(form1_filled.getAbsolutePath());
            doc_annots.save(Utils.createExternalFile("form1_filled_annot.fdf").getAbsolutePath());
            addToFileList("form1_filled_annot.fdf");
            
            // both form fields and annotations
            outputListener.println("Extract both form fields and annotations to FDF.");
            
            FDFDoc doc_both = in_doc.fdfExtract(PDFDoc.e_both);
            doc_both.setPDFFileName(form1_filled.getAbsolutePath());
            doc_both.save(Utils.createExternalFile("form1_filled_both.fdf").getAbsolutePath());
            addToFileList("form1_filled_both.fdf");
            
            // FDF to XFDF
            // form fields
            outputListener.println("Export form field data from FDF to XFDF.");
            
            doc_fields.saveAsXFDF(Utils.createExternalFile("form1_filled_data.xfdf").getAbsolutePath());
            addToFileList("form1_filled_data.xfdf");
            
            // annotations
            outputListener.println("Export annotations from FDF to XFDF.");
            
            doc_annots.saveAsXFDF(Utils.createExternalFile("form1_filled_annot.xfdf").getAbsolutePath());
            addToFileList("form1_filled_annot.xfdf");
            
            // both form fields and annotations
            outputListener.println("Export both form fields and annotations from FDF to XFDF.");
            
            doc_both.saveAsXFDF(Utils.createExternalFile("form1_filled_both.xfdf").getAbsolutePath());
            addToFileList("form1_filled_both.xfdf");
            
            in_doc.close();
            outputListener.println("Done example 3.");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // Example 4) Merge/Extract XFDF into/from PDF
        try {
            // Merge XFDF from string
            PDFDoc in_doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "numbered.pdf"));
            in_doc.initSecurityHandler();

            outputListener.println("Merge XFDF string into PDF.");

            String str = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><xfdf xmlns=\"http://ns.adobe.com/xfdf\" xml:space=\"preserve\"><square subject=\"Rectangle\" page=\"0\" name=\"cf4d2e58-e9c5-2a58-5b4d-9b4b1a330e45\" title=\"user\" creationdate=\"D:20120827112326-07'00'\" date=\"D:20120827112326-07'00'\" rect=\"227.7814207650273,597.6174863387978,437.07103825136608,705.0491803278688\" color=\"#000000\" interior-color=\"#FFFF00\" flags=\"print\" width=\"1\"><popup flags=\"print,nozoom,norotate\" open=\"no\" page=\"0\" rect=\"0,792,0,792\" /></square></xfdf>";

            FDFDoc fdoc = FDFDoc.createFromXFDF(str);
            in_doc.fdfMerge(fdoc);
            in_doc.save(Utils.createExternalFile("numbered_modified.pdf").getAbsolutePath(), SDFDoc.e_linearized, null);
            addToFileList("numbered_modified.pdf");
            outputListener.println("Merge complete.");

            // Extract XFDF as string
            outputListener.println("Extract XFDF as a string.");

            FDFDoc fdoc_new = in_doc.fdfExtract(PDFDoc.e_both);
            String XFDF_str = fdoc_new.saveAsXFDF();
            outputListener.println("Extracted XFDF: ");
            outputListener.println(XFDF_str);
            in_doc.close();
            outputListener.println("Extract complete.");
            outputListener.println("Done example 4.");

        } catch(Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // Example 5) Read FDF files directly
        try {
            FDFDoc in_doc = new FDFDoc(Utils.createExternalFile("form1_filled_data.fdf").getAbsolutePath());

            for (FDFFieldIterator itr = in_doc.getFieldIterator(); itr.hasNext();)
            {
                FDFField current = (FDFField)(itr.next());
                outputListener.println("Field name: " + current.getName());
                outputListener.println("Field partial name: " + current.getPartialName());
                outputListener.println("------------------------------");
            }
            in_doc.close();
            outputListener.println("Done example 5.");
        }
        catch(Exception e)
        {
            outputListener.println(e.getStackTrace());
        }

        // Example 6) Direct generation of FDF.
        try  
        {
            FDFDoc doc = new FDFDoc();
            // Create new fields (i.e. key/value pairs).
            doc.fieldCreate("Company", Field.e_text, "PDFTron Systems");
            doc.fieldCreate("First Name", Field.e_text, "John");
            doc.fieldCreate("Last Name", Field.e_text,  "Doe");
            // ...

            // doc.setPdfFileName("mydoc.pdf");

            doc.save(Utils.createExternalFile("sample_output.fdf").getAbsolutePath());
            doc.close();
            addToFileList("sample_output.fdf");
            outputListener.println("Done example 6.");
            
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }
        
        printFooter(outputListener);
    }
}
