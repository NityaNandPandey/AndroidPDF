//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.common.PDFNetException
import com.pdftron.filters.FilterReader
import com.pdftron.filters.FlateEncode
import com.pdftron.filters.MappedFile
import com.pdftron.pdf.PDFDoc
import com.pdftron.sdf.Obj
import com.pdftron.sdf.SDFDoc
import com.pdftron.sdf.SecurityHandler

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.ArrayList

class EncTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_encryption_title)
        setDescription(R.string.sample_encryption_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        // Example 1:
        // secure a document with password protection and
        // adjust permissions

        try {
            // Open the test file
            mOutputListener!!.println("Securing an existing document ...")
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "fish.pdf")!!.absolutePath)
            doc.initSecurityHandler()

            // Perform some operation on the document. In this case we use low level SDF API
            // to replace the content stream of the first page with contents of file 'my_stream.txt'
            if (true)
            // Optional
            {
                mOutputListener!!.println("Replacing the content stream, use flate compression...")

                // Get the page dictionary using the following path: trailer/Root/Pages/Kids/0
                val page_dict = doc.trailer.get("Root").value()
                        .get("Pages").value()
                        .get("Kids").value()
                        .getAt(0)

                // Embed a custom stream (file mystream.txt) using Flate compression.
                val embed_file = MappedFile(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "my_stream.txt")!!.absolutePath)
                val mystm = FilterReader(embed_file)
                page_dict.put("Contents",
                        doc.createIndirectStream(mystm,
                                FlateEncode(null)))
            }

            //encrypt the document

            // Apply a new security handler with given security settings.
            // In order to open saved PDF you will need a user password 'test'.
            val new_handler = SecurityHandler()

            // Set a new password required to open a document
            val user_password = "test"
            new_handler.changeUserPassword(user_password)

            // Set Permissions
            new_handler.setPermission(SecurityHandler.e_print, true)
            new_handler.setPermission(SecurityHandler.e_extract_content, false)

            // Note: document takes the ownership of new_handler.
            doc.securityHandler = new_handler

            // Save the changes.
            mOutputListener!!.println("Saving modified file...")
            doc.save(Utils.createExternalFile("secured.pdf").absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
        } catch (e: PDFNetException) {
            e.printStackTrace()
        }

        // Example 2:
        // Opens the encrypted document and removes all of
        // its security.
        try {
            val doc = PDFDoc(Utils.createExternalFile("secured.pdf").absolutePath)

            //If the document is encrypted prompt for the password
            if (!doc.initSecurityHandler()) {
                var success = false
                mOutputListener!!.println("The password is: test")
                for (count in 0..2) {
                    val r = BufferedReader(InputStreamReader(System.`in`))
                    mOutputListener!!.println("A password required to open the document.")
                    mOutputListener!!.print("Please enter the password: ")
                    // String password = r.readLine();
                    if (doc.initStdSecurityHandler("test")) {
                        success = true
                        mOutputListener!!.println("The password is correct.")
                        break
                    } else if (count < 3) {
                        mOutputListener!!.println("The password is incorrect, please try again")
                    }
                }
                if (!success) {
                    mOutputListener!!.println("Document authentication error....")
                }
            }

            //remove all security on the document
            doc.removeSecurity()
            doc.save(Utils.createExternalFile("not_secured.pdf").absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mOutputListener!!.println("Test completed.")

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