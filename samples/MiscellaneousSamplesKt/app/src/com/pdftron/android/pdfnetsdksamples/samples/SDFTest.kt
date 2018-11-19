//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.filters.FilterReader
import com.pdftron.filters.MappedFile
import com.pdftron.sdf.DictIterator
import com.pdftron.sdf.Obj
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

class SDFTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_sdf_title)
        setDescription(R.string.sample_sdf_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        try {
            mOutputListener!!.println("Opening the test file...")

            // Here we create a SDF/Cos document directly from PDF file. In case you have
            // PDFDoc you can always access SDF/Cos document using PDFDoc.GetSDFDoc() method.
            val doc = SDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "fish.pdf")!!.absolutePath)
            doc.initSecurityHandler()

            mOutputListener!!.println("Modifying info dictionary, adding custom properties, embedding a stream...")
            val trailer = doc.trailer            // Get the trailer

            // Now we will change PDF document information properties using SDF API

            // Get the Info dictionary.
            var itr = trailer.find("Info")
            val info: Obj
            if (itr.hasNext()) {
                info = itr.value()
                // Modify 'Producer' entry.
                info.putString("Producer", "PDFTron PDFNet")

                // Read title entry (if it is present)
                itr = info.find("Author")
                if (itr.hasNext()) {
                    val oldstr = itr.value().asPDFText

                    info.putText("Author", "$oldstr- Modified")
                } else {
                    info.putString("Author", "Me, myself, and I")
                }
            } else {
                // Info dict is missing.
                info = trailer.putDict("Info")
                info.putString("Producer", "PDFTron PDFNet")
                info.putString("Title", "My document")
            }

            // Create a custom inline dictionary within Info dictionary
            val custom_dict = info.putDict("My Direct Dict")
            custom_dict.putNumber("My Number", 100.0)     // Add some key/value pairs
            custom_dict.putArray("My Array")

            // Create a custom indirect array within Info dictionary
            val custom_array = doc.createIndirectArray()
            info.put("My Indirect Array", custom_array)    // Add some entries

            // Create indirect link to root
            custom_array.pushBack(trailer.get("Root").value())

            // Embed a custom stream (file mystream.txt).
            val embed_file = MappedFile(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "my_stream.txt")!!.absolutePath)
            val mystm = FilterReader(embed_file)
            custom_array.pushBack(doc.createIndirectStream(mystm))

            // Save the changes.
            mOutputListener!!.println("Saving modified test file...")
            doc.save(Utils.createExternalFile("sdftest_out.pdf").absolutePath, 0, null, "%PDF-1.4")
            mFileList.add(File(doc.fileName).name)
            doc.close()

            mOutputListener!!.println("Test completed.")
        } catch (e: Exception) {
            mOutputListener!!.println(e.stackTrace)
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