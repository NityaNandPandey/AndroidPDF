//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import java.io.File
import java.util.ArrayList

import com.pdftron.fdf.FDFDoc
import com.pdftron.fdf.FDFField
import com.pdftron.fdf.FDFFieldIterator
import com.pdftron.pdf.Field
import com.pdftron.pdf.FieldIterator
import com.pdftron.pdf.PDFDoc
import com.pdftron.sdf.SDFDoc

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils

class FDFTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_fdf_title)
        setDescription(R.string.sample_fdf_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)


        // Example 1)
        // Iterate over all form fields in the document. Display all field names.
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "form1.pdf")!!.absolutePath)
            doc.initSecurityHandler()

            val itr = doc.fieldIterator
            while (itr.hasNext()) {
                val current = itr.next()
                mOutputListener!!.println("Field name: " + current!!.getName())
                mOutputListener!!.println("Field partial name: " + current.getPartialName())

                mOutputListener!!.print("Field type: ")
                val type = current.getType()
                when (type) {
                    Field.e_button -> mOutputListener!!.println("Button")
                    Field.e_text -> mOutputListener!!.println("Text")
                    Field.e_choice -> mOutputListener!!.println("Choice")
                    Field.e_signature -> mOutputListener!!.println("Signature")
                }

                mOutputListener!!.println("------------------------------")
            }

            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Example 2) Import XFDF into FDF, then merge data from FDF into PDF
        try {
            // XFDF to FDF
            // form fields
            mOutputListener!!.println("Import form field data from XFDF to FDF.")

            val fdf_doc1 = FDFDoc.createFromXFDF(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "form1_data.xfdf")!!.absolutePath)
            fdf_doc1.save(Utils.createExternalFile("form1_data.fdf").absolutePath)
            mFileList.add(File(fdf_doc1.sdfDoc.fileName).name)

            // annotations
            mOutputListener!!.println("Import annotations from XFDF to FDF.")

            val fdf_doc2 = FDFDoc.createFromXFDF(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "form1_annots.xfdf")!!.absolutePath)
            fdf_doc2.save(Utils.createExternalFile("form1_annots.fdf").absolutePath)
            mFileList.add(File(fdf_doc2.sdfDoc.fileName).name)

            // FDF to PDF
            // form fields
            mOutputListener!!.println("Merge form field data from FDF.")

            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "form1.pdf")!!.absolutePath)
            doc.initSecurityHandler()
            doc.fdfMerge(fdf_doc1)

            // To use PDFNet form field appearance generation instead of relying on
            // Acrobat, uncomment the following two lines:
            //doc.refreshFieldAppearances();
            //doc.getAcroForm().putBool("NeedAppearances", false);

            doc.save(Utils.createExternalFile("form1_filled.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)

            // annotations
            mOutputListener!!.println("Merge annotations from FDF.")

            doc.fdfMerge(fdf_doc2)
            doc.save(Utils.createExternalFile("form1_filled_with_annots.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Example 3) Extract data from PDF to FDF, then export FDF as XFDF
        try {
            // PDF to FDF
            val in_doc = PDFDoc(Utils.createExternalFile("form1_filled_with_annots.pdf").absolutePath)
            in_doc.initSecurityHandler()

            // form fields only
            mOutputListener!!.println("Extract form fields data to FDF.")

            val doc_fields = in_doc.fdfExtract(PDFDoc.e_forms_only)
            doc_fields.pdfFileName = Utils.createExternalFile("form1_filled_with_annots.pdf").absolutePath
            doc_fields.save(Utils.createExternalFile("form1_filled_data.fdf").absolutePath)
            mFileList.add(File(doc_fields.sdfDoc.fileName).name)

            // annotations only
            mOutputListener!!.println("Extract annotations from FDF.")

            val doc_annots = in_doc.fdfExtract(PDFDoc.e_annots_only)
            doc_annots.pdfFileName = Utils.createExternalFile("form1_filled_with_annots.pdf").absolutePath
            doc_annots.save(Utils.createExternalFile("form1_filled_annot.fdf").absolutePath)
            mFileList.add(File(doc_annots.sdfDoc.fileName).name)

            // both form fields and annotations
            mOutputListener!!.println("Extract both form fields and annotations to FDF.")

            val doc_both = in_doc.fdfExtract(PDFDoc.e_both)
            doc_both.pdfFileName = Utils.createExternalFile("form1_filled_with_annots.pdf").absolutePath
            doc_both.save(Utils.createExternalFile("form1_filled_both.fdf").absolutePath)
            mFileList.add(File(doc_both.sdfDoc.fileName).name)

            // FDF to XFDF
            // form fields
            mOutputListener!!.println("Export form field data from FDF to XFDF.")

            doc_fields.saveAsXFDF(Utils.createExternalFile("form1_filled_data.xfdf").absolutePath)
            mFileList.add(File(doc_fields.sdfDoc.fileName).name)

            // annotations
            mOutputListener!!.println("Export annotations from FDF to XFDF.")

            doc_annots.saveAsXFDF(Utils.createExternalFile("form1_filled_annot.xfdf").absolutePath)
            mFileList.add(File(doc_annots.sdfDoc.fileName).name)

            // both form fields and annotations
            mOutputListener!!.println("Export both form fields and annotations from FDF to XFDF.")

            doc_both.saveAsXFDF(Utils.createExternalFile("form1_filled_both.xfdf").absolutePath)
            mFileList.add(File(doc_both.sdfDoc.fileName).name)

            in_doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Example 4) Merge/Extract XFDF into/from PDF
        try {
            // Merge XFDF from string
            val in_doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "numbered.pdf")!!.absolutePath)
            in_doc.initSecurityHandler()

            mOutputListener!!.println("Merge XFDF string into PDF.")

            val str = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><xfdf xmlns=\"http://ns.adobe.com/xfdf\" xml:space=\"preserve\"><square subject=\"Rectangle\" page=\"0\" name=\"cf4d2e58-e9c5-2a58-5b4d-9b4b1a330e45\" title=\"user\" creationdate=\"D:20120827112326-07'00'\" date=\"D:20120827112326-07'00'\" rect=\"227.7814207650273,597.6174863387978,437.07103825136608,705.0491803278688\" color=\"#000000\" interior-color=\"#FFFF00\" flags=\"print\" width=\"1\"><popup flags=\"print,nozoom,norotate\" open=\"no\" page=\"0\" rect=\"0,792,0,792\" /></square></xfdf>"

            val fdoc = FDFDoc.createFromXFDF(str)
            in_doc.fdfMerge(fdoc)
            in_doc.save(Utils.createExternalFile("numbered_modified.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(in_doc.fileName).name)
            mOutputListener!!.println("Merge complete.")

            // Extract XFDF as string
            mOutputListener!!.println("Extract XFDF as a string.")

            val fdoc_new = in_doc.fdfExtract(PDFDoc.e_both)
            val XFDF_str = fdoc_new.saveAsXFDF()
            mOutputListener!!.println("Extracted XFDF: ")
            mOutputListener!!.println(XFDF_str)
            in_doc.close()
            mOutputListener!!.println("Extract complete.")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Example 5) Read FDF files directly
        try {
            val doc = FDFDoc(Utils.createExternalFile("form1_filled_data.fdf").absolutePath)

            val itr = doc.fieldIterator
            while (itr.hasNext()) {
                val current = itr.next()
                mOutputListener!!.println("Field name: " + current?.getName())
                mOutputListener!!.println("Field partial name: " + current?.getPartialName())

                mOutputListener!!.println("------------------------------")
            }
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Example 6) Direct generation of FDF.
        try {
            val doc = FDFDoc()
            // Create new fields (i.e. key/value pairs).
            doc.fieldCreate("Company", Field.e_text, "PDFTron Systems")
            doc.fieldCreate("First Name", Field.e_text, "John")
            doc.fieldCreate("Last Name", Field.e_text, "Doe")
            // ...

            // doc.setPdfFileName("mydoc.pdf");

            doc.save(Utils.createExternalFile("sample_output.fdf").absolutePath)
            mFileList.add(File(doc.sdfDoc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        for (file in mFileList) {
            addToFileList(file)
        }
        printFooter(outputListener)
    }

    companion object {

        private var mOutputListener: OutputListener? = null

        private val mFileList = ArrayList<String>()
    }

}