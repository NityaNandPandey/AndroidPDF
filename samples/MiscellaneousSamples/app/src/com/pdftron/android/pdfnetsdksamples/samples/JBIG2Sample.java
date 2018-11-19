//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.filters.Filter;
import com.pdftron.filters.FilterReader;
import com.pdftron.pdf.ColorSpace;
import com.pdftron.pdf.Image;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.sdf.DictIterator;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.ObjSet;
import com.pdftron.sdf.SDFDoc;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

//This sample project illustrates how to recompress bi-tonal images in an 
//existing PDF document using JBIG2 compression. The sample is not intended 
//to be a generic PDF optimization tool.

public class JBIG2Sample extends PDFNetSample {

    public JBIG2Sample() {
        setTitle(R.string.sample_jbig_title);
        setDescription(R.string.sample_jbig_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);

        printHeader(outputListener);
        try {
            PDFDoc pdf_doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "US061222892-a.pdf"));
            pdf_doc.initSecurityHandler();

            SDFDoc cos_doc = pdf_doc.getSDFDoc();
            int num_objs = (int) cos_doc.xRefSize();
            for (int i = 1; i < num_objs; ++i) {
                Obj obj = cos_doc.getObj(i);
                if (obj != null && !obj.isFree() && obj.isStream()) {
                    // Process only images
                    DictIterator itr = obj.find("Subtype");
                    if (!itr.hasNext() || !itr.value().getName().equals("Image"))
                        continue;

                    Image input_image = new Image(obj);
                    // Process only gray-scale images
                    if (input_image.getComponentNum() != 1)
                        continue;
                    int bpc = input_image.getBitsPerComponent();
                    if (bpc != 1) // Recompress only 1 BPC images
                        continue;

                    // Skip images that are already compressed using JBIG2
                    itr = obj.find("Filter");
                    if (itr.hasNext() && itr.value().isName() && !itr.value().getName().equals("JBIG2Decode"))
                        continue;

                    Filter filter = obj.getDecodedStream();
                    FilterReader reader = new FilterReader(filter);

                    ObjSet hint_set = new ObjSet();
                    Obj hint = hint_set.createArray(); // A hint to image encoder to use JBIG2 compression
                    hint.pushBackName("JBIG2");
                    hint.pushBackName("Lossless");

                    Image new_image = Image.create(cos_doc, reader,
                            input_image.getImageWidth(),
                            input_image.getImageHeight(), 1,
                            ColorSpace.createDeviceGray(), hint);

                    Obj new_img_obj = new_image.getSDFObj();
                    itr = obj.find("Decode");
                    if (itr.hasNext())
                        new_img_obj.put("Decode", itr.value());
                    itr = obj.find("ImageMask");
                    if (itr.hasNext())
                        new_img_obj.put("ImageMask", itr.value());
                    itr = obj.find("Mask");
                    if (itr.hasNext())
                        new_img_obj.put("Mask", itr.value());

                    cos_doc.swap(i, new_img_obj.getObjNum());
                }
            }

            pdf_doc.save(Utils.createExternalFile("US061222892_JBIG2.pdf").getAbsolutePath(), SDFDoc.e_remove_unused, null);
            addToFileList("US061222892_JBIG2.pdf");
            pdf_doc.close();
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        printFooter(outputListener);
    }

}
