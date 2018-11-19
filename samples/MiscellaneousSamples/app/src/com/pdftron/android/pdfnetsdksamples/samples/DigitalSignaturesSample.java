//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.Image;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.sdf.Obj;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

//--------------------------------------------------------------------------------------------------
// This sample demonstrates the basic usage of high-level digital signature API in PDFNet.
//
// The following steps are typically used to add a digital signature to a PDF:
//
// 1. Extend and implement a new SignatureHandler. The SignatureHandler will be used to add or
//    validate/check a digital signature.
// 2. Create an instance of the implemented SignatureHandler and register it with PDFDoc with
//    pdfdoc.AddSignatureHandler(). The method returns an ID that can be used later to associate a
//    SignatureHandler with a field.
// 3. Find the required 'e_signature' field in the existing document or create a new field.
// 4. Call field.UseSignatureHandler() with the ID of your handler.
// 5. Call pdfdoc.Save()
//
// Additional processing can be done before document is signed. For example, UseSignatureHandler()
// returns an instance of SDF dictionary which represents the signature dictionary (or the /V entry
// of the form field). This can be used to add additional information to the signature dictionary
// (e.g. Name, Reason, Location, etc.).
//
// There are two options to digitally sign the document with PDFNet:
// 1. The full version library has a built-in signature handler, which can be used by calling
//    PDFDoc.addStdSignatureHandler(). This way you don't need to extend the SignatureHandler
//    interface and don't need to include any cryptographic libraries to your project (eg, Spongy
//    Castle).
// 2. In case you are using the standard version library, then you will have to create a class that
//    extends the SignatureHandler interface and code your own code that defines the digest and
//    cipher algorithms to sign the document. This interface is implemented here in the
//    MySignatureHandler.java.
//
// Look at the code in signPDF() and certifyPDF() methods to check how to use the methods above.
//
// Although the steps above describes extending the SignatureHandler class, this sample also
// demonstrates the use of StdSignatureHandler (a built-in SignatureHandler in PDFNet) to sign a PDF
// file.
//--------------------------------------------------------------------------------------------------

public class DigitalSignaturesSample extends PDFNetSample {

    private static OutputListener mOutputListener;
    private static ArrayList<String> mFileList;
    
    public DigitalSignaturesSample() {
        setTitle(R.string.sample_digitalsignatures_title);
        setDescription(R.string.sample_digitalsignatures_description);
        
        mFileList = new ArrayList<String>();
        
        // After proper setup (eg. Spongy Castle libs installed,
        // MySignatureHandler.createSignature() returns a valid buffer), please comment line below
        // to enable sample.
        // If you are using the full library, you do not need to use the custom signature handler.
        // PDFDoc has a standard signature handler that can be used instead. Check the code below
        // for more info.
        //DisableRun();
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        
        if (!isEnabled()) {
            outputListener.println("Sample is disabled. Please, check source code for more information.");
        } else {
        
            mFileList.clear();
            
            mOutputListener = outputListener;
    
            printHeader(outputListener);
    
            boolean result = true;
    
            if (!signPDF())
                result = false;
    
            if (!certifyPDF())
                result = false;
    
            if (!result) {
                outputListener.println("Tests failed.");
                return;
            }
            outputListener.println("All tests passed.");
            
            for (String file : mFileList) {
                addToFileList(file);
            }
        }

        printFooter(outputListener);
    }
    
    /**
     * This functions add an approval signature to the PDF document. The
     * original PDF document contains a blank form field that is prepared for a
     * user to sign. The following code demonstrate how to sign this document
     * using PDFNet.
     */
    public static boolean signPDF() {
        boolean result = true;
        String infile = "doc_to_sign.pdf";
        String outfile = "signed_doc.pdf";
        String certfile = "pdftron.pfx";
        String imagefile = "signature.jpg";

        mOutputListener.println("Signing PDF document \"" + infile + "\".");

        try {
            // Open an existing PDF
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + infile));


            // For full version --------------------------------------------------------------------
            // If you are using the full version, you can use the code below:
            InputStream is = Utils.getAssetInputStream(INPUT_PATH + certfile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int reads = is.read();
            while (reads != -1) {
                baos.write(reads);
                reads = is.read();
            }
            // Add an StdSignatureHandler instance to PDFDoc, making sure to keep track of it using the ID returned.
            long sigHandlerId = doc.addStdSignatureHandler(baos.toByteArray(), "password");
            // End full version --------------------------------------------------------------------


            // For standard version ----------------------------------------------------------------
            // In case your are using the standard version, then you can use the following
            // implementation (you also need to uncomment code in MySignatureHandler.java):
            // Create a new instance of the SignatureHandler.
//            MySignatureHandler sigCreator = new MySignatureHandler(Utils.getAssetTempFile(INPUT_PATH + certfile).getAbsolutePath(), "password");
//            sigCreator.setOutputListener(mOutputListener);
//            // Add the SignatureHandler instance to PDFDoc, making sure to keep
//            // track of it using the ID returned.
//            long sigHandlerId = doc.addSignatureHandler(sigCreator);
            // End standard version ----------------------------------------------------------------


            // Obtain the signature form field from the PDFDoc via Annotation;
            Field sigField = doc.getField("Signature1");
            Widget widgetAnnot = new Widget(sigField.getSDFObj());

            // Tell PDFNet to use the SignatureHandler created to sign the new
            // signature form field.
            Obj sigDict = sigField.useSignatureHandler(sigHandlerId);

            // Add more information to the signature dictionary
            sigDict.putName("SubFilter", "adbe.pkcs7.detached");
            sigDict.putString("Name", "PDFTron");
            sigDict.putString("Location", "Vancouver, BC");
            sigDict.putString("Reason", "Document verification.");

            // Add the signature appearance
            ElementWriter apWriter = new ElementWriter();
            ElementBuilder apBuilder = new ElementBuilder();
            apWriter.begin(doc);
            Image sigImg = Image.create(doc, Utils.getAssetTempFile(INPUT_PATH + imagefile).getAbsolutePath());
            double w = sigImg.getImageWidth(), h = sigImg.getImageHeight();
            Element apElement = apBuilder.createImage(sigImg, 0, 0, w, h);
            apWriter.writePlacedElement(apElement);
            Obj apObj = apWriter.end();
            apObj.putRect("BBox", 0, 0, w, h);
            apObj.putName("Subtype", "Form");
            apObj.putName("Type", "XObject");
            apWriter.begin(doc);
            apElement = apBuilder.createForm(apObj);
            apWriter.writePlacedElement(apElement);
            apObj = apWriter.end();
            apObj.putRect("BBox", 0, 0, w, h);
            apObj.putName("Subtype", "Form");
            apObj.putName("Type", "XObject");

            widgetAnnot.setAppearance(apObj);
            widgetAnnot.refreshAppearance();

            // Save the PDFDoc. Once the method below is called, PDFNet will
            // also sign the document using the information provided.
            doc.save(Utils.createExternalFile(outfile).getAbsolutePath(), 0, null);
            // Calling close is important to clear the signature handler. Not calling
            // close might lead to incorrect signing of the document.
            doc.close();
            mFileList.add(outfile);

            mOutputListener.println("Finished signing PDF document.");
        } catch (Exception e) {
            mOutputListener.println(e.getStackTrace());
            result = false;
        }

        return result;
    }

    /**
     * Adds a certification signature to the PDF document. Certifying a document
     * is like notarizing a document. Unlike approval signatures, there can be
     * only one certification per PDF document. Only the first signature in the
     * PDF document can be used as the certification signature. The process of
     * certifying a document is almost exactly the same as adding approval
     * signatures with the exception of certification signatures requires an
     * entry in the "Perms".
     */
    public static boolean certifyPDF() {
        boolean result = true;
        String infile = "newsletter.pdf";
        String outfile = "newsletter_certified.pdf";
        String certfile = "pdftron.pfx";

        mOutputListener.println("Certifying PDF document \"" + infile + "\".");
        try {
            // Open an existing PDF
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + infile));


            // For full version --------------------------------------------------------------------
            // If you are using the full version, you can use the code below:
            InputStream is = Utils.getAssetInputStream(INPUT_PATH + certfile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int reads = is.read();
            while (reads != -1) {
                baos.write(reads);
                reads = is.read();
            }
            // Add an StdSignatureHandler instance to PDFDoc, making sure to keep track of it using the ID returned.
            long sigHandlerId = doc.addStdSignatureHandler(baos.toByteArray(), "password");
            // End full version --------------------------------------------------------------------


            // For standard version ----------------------------------------------------------------
            // In case your are using the standard version, then you can use the following
            // implementation (you also need to uncomment code in MySignatureHandler.java):
//            // Create a new instance of the SignatureHandler.
//            MySignatureHandler sigCreator = new MySignatureHandler(Utils.getAssetTempFile(INPUT_PATH + certfile).getAbsolutePath(), "password");
//            sigCreator.setOutputListener(mOutputListener);
//
//            // Add the SignatureHandler instance to PDFDoc, making sure to keep
//            // track of it using the ID returned.
//            long sigHandlerId = doc.addSignatureHandler(sigCreator);
            // End standard version ----------------------------------------------------------------


            // Create new signature form field in the PDFDoc.
            Field sigField = doc.fieldCreate("Signature1", Field.e_signature);

            Page page1 = doc.getPage(1);
            Widget widgetAnnot = Widget.create(doc.getSDFDoc(), new Rect(0, 0, 0, 0), sigField);
            page1.annotPushBack(widgetAnnot);
            widgetAnnot.setPage(page1);
            Obj widgetObj = widgetAnnot.getSDFObj();
            widgetObj.putNumber("F", 132);
            widgetObj.putName("Type", "Annot");

            // Tell PDFNet to use the SignatureHandler created to sign the new
            // signature form field
            Obj sigDict = sigField.useSignatureHandler(sigHandlerId);

            // Add more information to the signature dictionary
            sigDict.putName("SubFilter", "adbe.pkcs7.detached");
            sigDict.putString("Name", "PDFTron");
            sigDict.putString("Location", "Vancouver, BC");
            sigDict.putString("Reason", "Document verification.");

            // Appearance can be added to the widget annotation. Please see the
            // "SignPDF()" function for details.

            // Add this sigDict as DocMDP in Perms dictionary from root
            Obj root = doc.getRoot();
            Obj perms = root.putDict("Perms");
            // add the sigDict as DocMDP (indirect) in Perms
            perms.put("DocMDP", sigDict);

            // add the additional DocMDP transform params
            Obj refObj = sigDict.putArray("Reference");
            Obj transform = refObj.pushBackDict();
            transform.putName("TransformMethod", "DocMDP");
            transform.putName("Type", "SigRef");
            Obj transformParams = transform.putDict("TransformParams");
            transformParams.putNumber("P", 1); // Set permissions as necessary.
            transformParams.putName("Type", "TransformParams");
            transformParams.putName("V", "1.2");

            // Save the PDFDoc. Once the method below is called, PDFNet will
            // also sign the document using the information provided.
            doc.save(Utils.createExternalFile(outfile).getAbsolutePath(), 0, null);
            // Calling close is important to clear the signature handler. Not calling
            // close might lead to incorrect signing of the document.
            doc.close();
            mFileList.add(outfile);

            mOutputListener.println("Finished certifying PDF document.");
        } catch (Exception e) {
            mOutputListener.println(e.getStackTrace());
            result = false;
        }

        return result;
    }
}
