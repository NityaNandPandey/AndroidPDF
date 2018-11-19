//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.PageIterator
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

class PDFPageTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_pdfpage_title)
        setDescription(R.string.sample_pdfpage_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        // Sample 1 - Split a PDF document into multiple pages
        try {
            mOutputListener!!.println("_______________________________________________")
            mOutputListener!!.println("Sample 1 - Delete every second page...")
            mOutputListener!!.println("Opening the input pdf...")
            val in_doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath)
            in_doc.initSecurityHandler()

            val page_num = in_doc.pageCount
            for (i in 1..page_num) {
                val new_doc = PDFDoc()
                new_doc.insertPages(0, in_doc, i, i, PDFDoc.InsertBookmarkMode.NONE, null)
                new_doc.save(Utils.createExternalFile("newsletter_split_page_").absolutePath + i + ".pdf", SDFDoc.SaveMode.REMOVE_UNUSED, null)
                mFileList.add(File(new_doc.fileName).name)
                mOutputListener!!.println("Done. Result saved in newsletter_split_page_$i.pdf")
                new_doc.close()
            }
            in_doc.close()
        } catch (e2: Exception) {
            mOutputListener!!.println(e2.stackTrace)
        }

        // Sample 2 - Merge several PDF documents into one
        try {
            mOutputListener!!.println("_______________________________________________")
            mOutputListener!!.println("Sample 2 - Merge several PDF documents into one...")
            val new_doc = PDFDoc()
            new_doc.initSecurityHandler()

            val page_num = 15
            for (i in 1..page_num) {
                mOutputListener!!.println("Opening newsletter_split_page_$i.pdf")
                val in_doc = PDFDoc(Utils.createExternalFile("newsletter_split_page_").absolutePath + i + ".pdf")
                new_doc.insertPages(i, in_doc, 1, in_doc.pageCount, PDFDoc.InsertBookmarkMode.NONE, null)
                in_doc.close()
            }
            new_doc.save(Utils.createExternalFile("newsletter_merge_pages.pdf").absolutePath, SDFDoc.SaveMode.REMOVE_UNUSED, null)
            mFileList.add(File(new_doc.fileName).name)
            mOutputListener!!.println("Done. Result saved in newsletter_merge_pages.pdf...")
            new_doc.close()
        } catch (e2: Exception) {
            mOutputListener!!.println(e2.stackTrace)
        }

        // Sample 3 - Delete every second page
        try {
            mOutputListener!!.println("_______________________________________________")
            mOutputListener!!.println("Sample 3 - Delete every second page...")
            mOutputListener!!.println("Opening the input pdf...")
            val in_doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath)
            in_doc.initSecurityHandler()

            var page_num = in_doc.pageCount
            while (page_num >= 1) {
                val itr = in_doc.getPageIterator(page_num)
                in_doc.pageRemove(itr)
                page_num -= 2
            }

            in_doc.save(Utils.createExternalFile("newsletter_page_remove.pdf").absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            mFileList.add(File(in_doc.fileName).name)
            mOutputListener!!.println("Done. Result saved in newsletter_page_remove.pdf...")

            //Close the open document to free up document
            //memory sooner than waiting for the
            //garbage collector
            in_doc.close()
        } catch (e2: Exception) {
            mOutputListener!!.println(e2.stackTrace)
        }

        // Sample 4 - Inserts a page from one document at different
        // locations within another document
        try {
            mOutputListener!!.println("_______________________________________________")
            mOutputListener!!.println("Sample 4 - Insert a page at different locations...")
            mOutputListener!!.println("Opening the input pdf...")

            val in1_doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath)
            in1_doc.initSecurityHandler()

            val in2_doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "fish.pdf")!!.absolutePath)
            in2_doc.initSecurityHandler()

            val src_page_itr = in2_doc.pageIterator
            val src_page = src_page_itr.next()
            val dst_page_itr = in1_doc.pageIterator
            var page_num = 1
            while (dst_page_itr.hasNext()) {
                if (page_num++ % 3 == 0) {
                    in1_doc.pageInsert(dst_page_itr, src_page)
                }
                dst_page_itr.next()
            }

            in1_doc.save(Utils.createExternalFile("newsletter_page_insert.pdf").absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            mFileList.add(File(in1_doc.fileName).name)
            mOutputListener!!.println("Done. Result saved in newsletter_page_insert.pdf...")

            //Close the open documents to free up document
            //memory sooner than waiting for the
            //garbage collector
            in1_doc.close()
            in2_doc.close()
        } catch (e2: Exception) {
            mOutputListener!!.println(e2.stackTrace)
        }

        // Sample 5 - Replicate pages within a single document
        try {
            mOutputListener!!.println("_______________________________________________")
            mOutputListener!!.println("Sample 5 - Replicate pages within a single document...")
            mOutputListener!!.println("Opening the input pdf...")
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath)
            doc.initSecurityHandler()

            // Replicate the cover page three times (copy page #1 and place it before the
            // seventh page in the document page sequence)
            val cover = doc.getPage(1)
            val p7 = doc.getPageIterator(7)
            doc.pageInsert(p7, cover)
            doc.pageInsert(p7, cover)
            doc.pageInsert(p7, cover)

            // Replicate the cover page two more times by placing it before and after
            // existing pages.
            doc.pagePushFront(cover)
            doc.pagePushBack(cover)

            doc.save(Utils.createExternalFile("newsletter_page_clone.pdf").absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            mFileList.add(File(doc.fileName).name)
            mOutputListener!!.println("Done. Result saved in newsletter_page_clone.pdf...")

            //Close the open document to free up document
            //memory sooner than waiting for the
            //garbage collector
            doc.close()
        } catch (e2: Exception) {
            mOutputListener!!.println(e2.stackTrace)
        }

        // Sample 6 - Use ImportPages() in order to copy multiple pages at once
        // in order to preserve shared resources between pages (e.g. images, fonts,
        // colorspaces, etc.)
        try {
            mOutputListener!!.println("_______________________________________________")
            mOutputListener!!.println("Sample 6 - Preserving shared resources using ImportPages...")
            mOutputListener!!.println("Opening the input pdf...")
            val in_doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath)
            in_doc.initSecurityHandler()

            val new_doc = PDFDoc()

            val copy_pages = arrayOfNulls<Page>(in_doc.pageCount)
            var j = 0
            val itr = in_doc.pageIterator
            while (itr.hasNext()) {
                copy_pages[j] = itr.next()
                j++
            }

            val imported_pages = new_doc.importPages(copy_pages)
            for (i in imported_pages.indices) {
                new_doc.pagePushFront(imported_pages[i]) // Order pages in reverse order.
                // Use pushBackPage() if you would like to preserve the same order.
            }

            new_doc.save(Utils.createExternalFile("newsletter_import_pages.pdf").absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            mFileList.add(File(new_doc.fileName).name)

            //Close the open documents to free up document
            //memory sooner than waiting for the
            //garbage collector
            in_doc.close()
            new_doc.close()

            mOutputListener!!.println("Done. Result saved in newsletter_import_pages.pdf...")
            mOutputListener!!.println()
            mOutputListener!!.println("Note that the output file size is less than half the size")
            mOutputListener!!.println("of the file produced using individual page copy operations")
            mOutputListener!!.println("between two documents")
        } catch (e1: Exception) {
            mOutputListener!!.println(e1.stackTrace)
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