//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.filters.Filter
import com.pdftron.filters.FilterReader
import com.pdftron.pdf.ColorSpace
import com.pdftron.pdf.Image
import com.pdftron.pdf.PDFDoc
import com.pdftron.sdf.DictIterator
import com.pdftron.sdf.Obj
import com.pdftron.sdf.ObjSet
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

//This sample project illustrates how to recompress bi-tonal images in an
//existing PDF document using JBIG2 compression. The sample is not intended
//to be a generic PDF optimization tool.

class JBIG2Test : PDFNetSample() {
    init {
        setTitle(R.string.sample_jbig_title)
        setDescription(R.string.sample_jbig_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        try {
            val pdf_doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "US061222892-a.pdf")!!.absolutePath)
            pdf_doc.initSecurityHandler()

            val cos_doc = pdf_doc.sdfDoc
            val num_objs = cos_doc.xRefSize().toInt()
            for (i in 1 until num_objs) {
                val obj = cos_doc.getObj(i.toLong())
                if (obj != null && !obj.isFree && obj.isStream) {
                    // Process only images
                    var itr = obj.find("Subtype")
                    if (!itr.hasNext() || itr.value().name != "Image")
                        continue

                    val input_image = Image(obj)
                    // Process only gray-scale images
                    if (input_image.componentNum != 1)
                        continue
                    val bpc = input_image.bitsPerComponent
                    if (bpc != 1)
                    // Recompress only 1 BPC images
                        continue

                    // Skip images that are already compressed using JBIG2
                    itr = obj.find("Filter")
                    if (itr.hasNext() && itr.value().isName &&
                            itr.value().name != "JBIG2Decode")
                        continue

                    val filter = obj.decodedStream
                    val reader = FilterReader(filter)

                    val hint_set = ObjSet()
                    val hint = hint_set.createArray() // A hint to image encoder to use JBIG2 compression
                    hint.pushBackName("JBIG2")
                    hint.pushBackName("Lossless")

                    val new_image = Image.create(cos_doc, reader,
                            input_image.imageWidth,
                            input_image.imageHeight, 1, ColorSpace.createDeviceGray(), hint)

                    val new_img_obj = new_image.sdfObj
                    itr = obj.find("Decode")
                    if (itr.hasNext())
                        new_img_obj.put("Decode", itr.value())
                    itr = obj.find("ImageMask")
                    if (itr.hasNext())
                        new_img_obj.put("ImageMask", itr.value())
                    itr = obj.find("Mask")
                    if (itr.hasNext())
                        new_img_obj.put("Mask", itr.value())

                    cos_doc.swap(i.toLong(), new_img_obj.objNum)
                }
            }

            pdf_doc.save(Utils.createExternalFile("US061222892_JBIG2.pdf").absolutePath, SDFDoc.SaveMode.REMOVE_UNUSED, null)
            mFileList.add(File(pdf_doc.fileName).name)
            pdf_doc.close()
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