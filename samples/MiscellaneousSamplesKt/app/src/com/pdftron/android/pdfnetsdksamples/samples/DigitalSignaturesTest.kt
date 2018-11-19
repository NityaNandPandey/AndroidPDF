//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementBuilder
import com.pdftron.pdf.ElementWriter
import com.pdftron.pdf.Field
import com.pdftron.pdf.Image
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.Rect
import com.pdftron.pdf.annots.Widget
import com.pdftron.sdf.Obj
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

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

class DigitalSignaturesTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_digitalsignatures_title)
        setDescription(R.string.sample_digitalsignatures_description)

        // After proper setup (eg. Spongy Castle libs installed,
        // MySignatureHandler.createSignature() returns a valid buffer), please comment line below
        // to enable sample.
        // If you are using the full library, you do not need to use the custom signature handler.
        // PDFDoc has a standard signature handler that can be used instead. Check the code below
        // for more info.
        //DisableRun();
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)
        // Initialize PDFNet

        var result = true

        if (!signPDF())
            result = false

        if (!certifyPDF())
            result = false

        if (!result) {
            mOutputListener!!.println("Tests failed.")
            return
        }
        mOutputListener!!.println("All tests passed.")

        for (file in mFileList) {
            addToFileList(file)
        }
        printFooter(outputListener)
    }

    companion object {

        private var mOutputListener: OutputListener? = null

        private val mFileList = ArrayList<String>()
        /**
         * This functions add an approval signature to the PDF document. The original PDF document contains a blank form field
         * that is prepared for a user to sign. The following code demonstrate how to sign this document using PDFNet.
         */
        fun signPDF(): Boolean {
            var result = true

            val infile = Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "doc_to_sign.pdf")!!.absolutePath
            val outfile = Utils.createExternalFile("signed_doc.pdf").absolutePath
            val certfile = Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "pdftron.pfx")!!.absolutePath
            val imagefile = Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "signature.jpg")!!.absolutePath

            mOutputListener!!.println("Signing PDF document \"$infile\".")

            try {
                // Open an existing PDF
                val doc = PDFDoc(infile)

                // Add an StdSignatureHandler instance to PDFDoc, making sure to keep track of it using the ID returned.
                val sigHandlerId = doc.addStdSignatureHandler(certfile, "password")
                // When using BCSignatureHandler class, uncomment the following lines and comment the line above.
                // Create a new instance of the SignatureHandler.
                // BCSignatureHandler sigHandler = new BCSignatureHandler(certfile, "password");
                // Add the SignatureHandler instance to PDFDoc, making sure to keep track of it using the ID returned.
                // long sigHandlerId = doc.addSignatureHandler(sigHandler);

                // Obtain the signature form field from the PDFDoc via Annotation;
                val sigField = doc.getField("Signature1")
                val widgetAnnot = Widget(sigField.sdfObj)

                // Tell PDFNet to use the SignatureHandler created to sign the new signature form field.
                val sigDict = sigField.useSignatureHandler(sigHandlerId)

                // Add more information to the signature dictionary
                sigDict.putName("SubFilter", "adbe.pkcs7.detached")
                sigDict.putString("Name", "PDFTron")
                sigDict.putString("Location", "Vancouver, BC")
                sigDict.putString("Reason", "Document verification.")

                // Add the signature appearance
                val apWriter = ElementWriter()
                val apBuilder = ElementBuilder()
                apWriter.begin(doc)
                val sigImg = Image.create(doc, imagefile)
                val w = sigImg.imageWidth.toDouble()
                val h = sigImg.imageHeight.toDouble()
                var apElement = apBuilder.createImage(sigImg, 0.0, 0.0, w, h)
                apWriter.writePlacedElement(apElement)
                var apObj = apWriter.end()
                apObj.putRect("BBox", 0.0, 0.0, w, h)
                apObj.putName("Subtype", "Form")
                apObj.putName("Type", "XObject")
                apWriter.begin(doc)
                apElement = apBuilder.createForm(apObj)
                apWriter.writePlacedElement(apElement)
                apObj = apWriter.end()
                apObj.putRect("BBox", 0.0, 0.0, w, h)
                apObj.putName("Subtype", "Form")
                apObj.putName("Type", "XObject")

                widgetAnnot.appearance = apObj
                widgetAnnot.refreshAppearance()

                // Save the PDFDoc. Once the method below is called, PDFNet will also sign the document using the information
                // provided.
                doc.save(outfile, SDFDoc.SaveMode.NO_FLAGS, null)
                mFileList.add(File(doc.fileName).name)
                doc.close()

                mOutputListener!!.println("Finished signing PDF document.")
            } catch (e: Exception) {
                System.err.println(e.message)
                e.printStackTrace(System.err)
                result = false
            }

            return result
        }

        /**
         * Adds a certification signature to the PDF document. Certifying a document is like notarizing a document. Unlike
         * approval signatures, there can be only one certification per PDF document. Only the first signature in the PDF
         * document can be used as the certification signature. The process of certifying a document is almost exactly the same
         * as adding approval signatures with the exception of certification signatures requires an entry in the "Perms"
         */
        fun certifyPDF(): Boolean {
            var result = true

            val infile = Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath
            val outfile = Utils.createExternalFile("newsletter_certified.pdf").absolutePath
            val certfile = Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "pdftron.pfx")!!.absolutePath

            mOutputListener!!.println("Certifying PDF document \"$infile\".")
            try {
                // Open an existing PDF
                val doc = PDFDoc(infile)

                // Add an StdSignatureHandler instance to PDFDoc, making sure to keep track of it using the ID returned.
                val sigHandlerId = doc.addStdSignatureHandler(certfile, "password")
                // When using BCSignatureHandler class, uncomment the following lines and comment the line above.
                // Create a new instance of the SignatureHandler.
                // MySignatureHandler sigHandler = new MySignatureHandler(certfile, "password");
                // Add the SignatureHandler instance to PDFDoc, making sure to keep track of it using the ID returned.
                // long sigHandlerId = doc.addSignatureHandler(sigHandler);

                // Create new signature form field in the PDFDoc.
                val sigField = doc.fieldCreate("Signature1", Field.e_signature)

                val page1 = doc.getPage(1)
                val widgetAnnot = Widget.create(doc.sdfDoc, Rect(0.0, 0.0, 0.0, 0.0), sigField)
                page1.annotPushBack(widgetAnnot)
                widgetAnnot.page = page1
                val widgetObj = widgetAnnot.sdfObj
                widgetObj.putNumber("F", 132.0)
                widgetObj.putName("Type", "Annot")

                // Tell PDFNet to use the SignatureHandler created to sign the new signature form field
                val sigDict = sigField.useSignatureHandler(sigHandlerId)

                // Add more information to the signature dictionary
                sigDict.putName("SubFilter", "adbe.pkcs7.detached")
                sigDict.putString("Name", "PDFTron")
                sigDict.putString("Location", "Vancouver, BC")
                sigDict.putString("Reason", "Document verification.")

                // Appearance can be added to the widget annotation. Please see the "SignPDF()" function for details.

                // Add this sigDict as DocMDP in Perms dictionary from root
                val root = doc.root
                val perms = root.putDict("Perms")
                // add the sigDict as DocMDP (indirect) in Perms
                perms.put("DocMDP", sigDict)

                // add the additional DocMDP transform params
                val refObj = sigDict.putArray("Reference")
                val transform = refObj.pushBackDict()
                transform.putName("TransformMethod", "DocMDP")
                transform.putName("Type", "SigRef")
                val transformParams = transform.putDict("TransformParams")
                transformParams.putNumber("P", 1.0) // Set permissions as necessary.
                transformParams.putName("Type", "TransformParams")
                transformParams.putName("V", "1.2")

                // Save the PDFDoc. Once the method below is called, PDFNet will also sign the document using the information
                // provided.
                doc.save(outfile, SDFDoc.SaveMode.NO_FLAGS, null)
                mFileList.add(File(doc.fileName).name)
                doc.close()

                mOutputListener!!.println("Finished certifying PDF document.")
            } catch (e: Exception) {
                System.err.println(e.message)
                e.printStackTrace(System.err)
                result = false
            }

            return result
        }
    }

}