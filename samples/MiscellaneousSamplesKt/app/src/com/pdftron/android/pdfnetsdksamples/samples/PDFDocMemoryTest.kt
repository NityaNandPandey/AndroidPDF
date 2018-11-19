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
import com.pdftron.filters.MappedFile
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementReader
import com.pdftron.pdf.ElementWriter
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.PageIterator
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList

class PDFDocMemoryTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_pdfdocmemory_title)
        setDescription(R.string.sample_pdfdocmemory_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)


        // The following sample illustrates how to read/write a PDF document from/to
        // a memory buffer.  This is useful for applications that work with dynamic PDF
        // documents that don't need to be saved/read from a disk.
        try {
            // Read a PDF document in a memory buffer.
            val file = MappedFile(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "tiger.pdf")!!.absolutePath)
            val file_sz = file.fileSize()

            val file_reader = FilterReader(file)

            val mem = ByteArray(file_sz.toInt())

            val bytes_read = file_reader.read(mem)
            val doc = PDFDoc(mem)

            doc.initSecurityHandler()
            val num_pages = doc.pageCount

            val writer = ElementWriter()
            val reader = ElementReader()
            var element: Element?

            // Create a duplicate of every page but copy only path objects

            for (i in 1..num_pages) {
                val itr = doc.getPageIterator(2 * i - 1)
                val current = itr.next()
                reader.begin(current)
                val new_page = doc.pageCreate(current!!.getMediaBox())
                doc.pageInsert(itr, new_page)

                writer.begin(new_page)
                while (true)
                // Read page contents
                {
                    element = reader.next()
                    if (element == null) {
                        break
                    }
                    //if (element.getType() == Element.e_path)
                    writer.writeElement(element)
                }

                writer.end()
                reader.end()
            }

            doc.save(Utils.createExternalFile("doc_memory_edit.pdf").absolutePath, SDFDoc.SaveMode.REMOVE_UNUSED, null)
            mFileList.add(File(doc.fileName).name)

            // Save the document to a memory buffer.

            val buf = doc.save(SDFDoc.SaveMode.REMOVE_UNUSED, null)
            // doc.Save(buf, buf_sz, Doc::e_linearized, NULL);

            // Write the contents of the buffer to the disk
            run {
                val outfile = File(Utils.createExternalFile("doc_memory_edit.txt").absolutePath)
                mFileList.add("doc_memory_edit.txt")
                val fop = FileOutputStream(outfile)
                if (!outfile.exists()) {
                    outfile.createNewFile()
                }
                fop.write(buf)
                fop.flush()
                fop.close()
            }

            // Read some data from the file stored in memory
            reader.begin(doc.getPage(1))
            while (true) {
                element = reader.next()
                if (element == null) {
                    break
                }
                if (element.type == Element.e_path) mOutputListener!!.print("Path, ")
            }
            reader.end()

            doc.close()
            mOutputListener!!.println("\n\nDone. Result saved in doc_memory_edit.pdf and doc_memory_edit.txt ...")
        } catch (e: PDFNetException) {
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        //This sample illustrates how to open a PDF document
        //from an Java InputStream and how to save to an OutputStream.
        try {
            val doc = PDFDoc(FileInputStream(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath))
            doc.save(FileOutputStream(Utils.createExternalFile("StreamTest.pdf").absolutePath), SDFDoc.SaveMode.NO_FLAGS, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("\n\nDone. Result saved in StreamTest.pdf ...")
        } catch (e: PDFNetException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: FileNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
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