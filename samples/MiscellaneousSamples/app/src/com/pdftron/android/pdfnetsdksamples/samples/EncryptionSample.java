//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.filters.FilterReader;
import com.pdftron.filters.FlateEncode;
import com.pdftron.filters.MappedFile;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.SecurityHandler;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

public class EncryptionSample extends PDFNetSample {

    public EncryptionSample() {
        setTitle(R.string.sample_encryption_title);
        setDescription(R.string.sample_encryption_description);
    }
    
    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        printHeader(outputListener);
        
        // Example 1: 
        // secure a document with password protection and 
        // adjust permissions 
        try {
            // Open the test file
            outputListener.println("Securing an existing document ...");
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "fish.pdf"));
            doc.initSecurityHandler();
            
            // Perform some operation on the document. In this case we use low level SDF API
            // to replace the content stream of the first page with contents of file 'my_stream.txt'
            if (true) {  // Optional
                outputListener.println("Replacing the content stream, use flate compression...");

                // Get the page dictionary using the following path: trailer/Root/Pages/Kids/0
                Obj page_dict = doc.getTrailer().get("Root").value()
                    .get("Pages").value()
                    .get("Kids").value()
                    .getAt(0);

                // Embed a custom stream (file mystream.txt) using Flate compression.
                MappedFile embed_file = new MappedFile(Utils.getAssetTempFile(INPUT_PATH + "my_stream.txt").getAbsolutePath());
                FilterReader mystm = new FilterReader(embed_file);
                page_dict.put("Contents", doc.createIndirectStream(mystm, new FlateEncode(null)));
            }

            //encrypt the document

            // Apply a new security handler with given security settings. 
            // In order to open saved PDF you will need a user password 'test'.
            SecurityHandler new_handler = new SecurityHandler();

            // Set a new password required to open a document
            String user_password = "test";
            new_handler.changeUserPassword(user_password);

            // Set Permissions
            new_handler.setPermission(SecurityHandler.e_print, true);
            new_handler.setPermission(SecurityHandler.e_extract_content, false);

            // Note: document takes the ownership of new_handler.
            doc.setSecurityHandler(new_handler);

            // Save the changes.
            outputListener.println("Saving modified file...");
            doc.save(Utils.createExternalFile("secured.pdf").getAbsolutePath(), 0, null);
            addToFileList("secured.pdf");
            doc.close();
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // Example 2:
        // Opens the encrypted document and removes all of 
        // its security.
        try {
            PDFDoc doc = new PDFDoc(Utils.createExternalFile("secured.pdf").getAbsolutePath());

            //If the document is encrypted prompt for the password
            if (!doc.initSecurityHandler()) {
                outputListener.println("The password is: test");
                // TODO: add code so the user can enter the password while running the sample
                if (doc.initStdSecurityHandler("test")) {
                    outputListener.println("The password is correct.");
                } else {
                    outputListener.println("The password is incorrect, please try again.");
                }
            }

            //remove all security on the document
            doc.removeSecurity();
            doc.save(Utils.createExternalFile("not_secured.pdf").getAbsolutePath(), 0, null);
            addToFileList("not_secured.pdf");
            doc.close();
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }
        
        printFooter(outputListener);
    }
}
