//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.pdf.ColorPt
import com.pdftron.pdf.ColorSpace
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementReader
import com.pdftron.pdf.ElementWriter
import com.pdftron.pdf.GState
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.PageIterator
import com.pdftron.sdf.Obj
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList
import java.util.TreeSet

class ElementEditTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_elementedit_title)
        setDescription(R.string.sample_elementedit_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        val input_filename = "newsletter.pdf"
        val output_filename = "newsletter_edited.pdf"

        try {
            mOutputListener!!.println("Opening the input file...")
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + input_filename)!!.absolutePath)
            doc.initSecurityHandler()

            val writer = ElementWriter()
            val reader = ElementReader()
            val visited = TreeSet<Int>()

            val itr = doc.pageIterator
            while (itr.hasNext()) {
                val page = itr.next()
                visited.add(page!!.sdfObj!!.objNum.toInt())

                reader.begin(page)
                writer.begin(page, ElementWriter.e_replacement, false)
                processElements(writer, reader, visited)
                writer.end()
                reader.end()
            }

            // Save modified document
            doc.save(Utils.createExternalFile(output_filename).absolutePath, SDFDoc.SaveMode.REMOVE_UNUSED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done. Result saved in $output_filename...")
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
        fun processElements(writer: ElementWriter, reader: ElementReader, visited: MutableSet<Int>) {
            var element: Element?
            try {
                while (true) {
                    element = reader.next()
                    if (element == null) {
                        break
                    }
                    when (element.type) {
                        Element.e_image, Element.e_inline_image -> {
                        }
                        Element.e_path -> {
                            // Set all paths to red color.
                            val gs = element.gState
                            gs.fillColorSpace = ColorSpace.createDeviceRGB()
                            gs.fillColor = ColorPt(1.0, 0.0, 0.0)
                            writer.writeElement(element)
                        }
                        Element.e_text -> {
                            // Set all text to blue color.
                            val gs = element.gState
                            gs.fillColorSpace = ColorSpace.createDeviceRGB()
                            gs.fillColor = ColorPt(0.0, 0.0, 1.0)
                            writer.writeElement(element)
                        }
                        Element.e_form -> {
                            writer.writeElement(element) // write Form XObject reference to current stream
                            val form_obj = element.xObject
                            if (!visited.contains(form_obj.objNum.toInt()))
                            // if this XObject has not been processed
                            {
                                // recursively process the Form XObject
                                visited.add(form_obj.objNum.toInt())
                                val new_writer = ElementWriter()
                                reader.formBegin()
                                new_writer.begin(form_obj)
                                processElements(new_writer, reader, visited)
                                new_writer.end()
                                reader.end()
                            }
                        }
                        else -> writer.writeElement(element)
                    }// remove all images by skipping them
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

}