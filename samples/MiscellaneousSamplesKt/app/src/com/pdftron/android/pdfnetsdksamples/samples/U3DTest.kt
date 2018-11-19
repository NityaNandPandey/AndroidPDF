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
import com.pdftron.pdf.ElementBuilder
import com.pdftron.pdf.ElementWriter
import com.pdftron.pdf.Image
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.Rect
import com.pdftron.sdf.Obj
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

class U3DTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_u3d_title)
        setDescription(R.string.sample_u3d_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)
        try {

            val doc = PDFDoc()
            val page = doc.pageCreate()
            doc.pagePushBack(page)
            val annots = doc.createIndirectArray()
            page.sdfObj.put("Annots", annots)

            Create3DAnnotation(doc, annots)
            doc.save(Utils.createExternalFile("dice_u3d.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done")
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

        @Throws(PDFNetException::class)
        internal fun Create3DAnnotation(doc: PDFDoc, annots: Obj) {
            // ---------------------------------------------------------------------------------
            // Create a 3D annotation based on U3D content. PDF 1.6 introduces the capability
            // for collections of three-dimensional objects, such as those used by CAD software,
            // to be embedded in PDF files.
            val link_3D = doc.createIndirectDict()
            link_3D.putName("Subtype", "3D")

            // Annotation location on the page
            val link_3D_rect = Rect(25.0, 180.0, 585.0, 643.0)
            link_3D.putRect("Rect", link_3D_rect.x1, link_3D_rect.y1,
                    link_3D_rect.x2, link_3D_rect.y2)
            annots.pushBack(link_3D)

            // The 3DA entry is an activation dictionary (see Table 9.34 in the PDF Reference Manual)
            // that determines how the state of the annotation and its associated artwork can change.
            val activation_dict_3D = link_3D.putDict("3DA")

            // Set the annotation so that it is activated as soon as the page containing the
            // annotation is opened. Other options are: PV (page view) and XA (explicit) activation.
            activation_dict_3D.putName("A", "PO")

            // Embed U3D Streams (3D Model/Artwork).
            run {
                val u3d_file = MappedFile(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "dice.u3d")!!.absolutePath)
                val u3d_reader = FilterReader(u3d_file)

                // To embed 3D stream without compression, you can omit the second parameter in CreateIndirectStream.
                val u3d_data_dict = doc.createIndirectStream(u3d_reader, FlateEncode(null))
                u3d_data_dict.putName("Subtype", "U3D")
                link_3D.put("3DD", u3d_data_dict)
            }

            // Set the initial view of the 3D artwork that should be used when the annotation is activated.
            val view3D_dict = link_3D.putDict("3DV")
            run {
                view3D_dict.putString("IN", "Unnamed")
                view3D_dict.putString("XN", "Default")
                view3D_dict.putName("MS", "M")
                view3D_dict.putNumber("CO", 27.5)

                // A 12-element 3D transformation matrix that specifies a position and orientation
                // of the camera in world coordinates.
                val tr3d = view3D_dict.putArray("C2W")
                tr3d.pushBackNumber(1.0)
                tr3d.pushBackNumber(0.0)
                tr3d.pushBackNumber(0.0)
                tr3d.pushBackNumber(0.0)
                tr3d.pushBackNumber(0.0)
                tr3d.pushBackNumber(-1.0)
                tr3d.pushBackNumber(0.0)
                tr3d.pushBackNumber(1.0)
                tr3d.pushBackNumber(0.0)
                tr3d.pushBackNumber(0.0)
                tr3d.pushBackNumber(-27.5)
                tr3d.pushBackNumber(0.0)

            }

            // Create annotation appearance stream, a thumbnail which is used during printing or
            // in PDF processors that do not understand 3D data.
            val ap_dict = link_3D.putDict("AP")
            run {
                val builder = ElementBuilder()
                val writer = ElementWriter()
                writer.begin(doc)

                val thumb_pathname = Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "dice.jpg")!!.absolutePath
                val image = Image.create(doc, thumb_pathname)
                writer.writePlacedElement(builder.createImage(image, 0.0, 0.0, link_3D_rect.width, link_3D_rect.height))

                val normal_ap_stream = writer.end()
                normal_ap_stream.putName("Subtype", "Form")
                normal_ap_stream.putRect("BBox", 0.0, 0.0, link_3D_rect.width, link_3D_rect.height)
                ap_dict.put("N", normal_ap_stream)
            }
        }
    }

}