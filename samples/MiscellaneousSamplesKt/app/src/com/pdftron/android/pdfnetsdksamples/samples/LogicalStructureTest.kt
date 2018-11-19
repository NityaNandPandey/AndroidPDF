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
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementReader
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.PageIterator
import com.pdftron.pdf.struct.ContentItem
import com.pdftron.pdf.struct.SElement
import com.pdftron.pdf.struct.STree
import com.pdftron.sdf.Obj

import java.util.ArrayList
import java.util.TreeMap

class LogicalStructureTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_logicalstructure_title)
        setDescription(R.string.sample_logicalstructure_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        // string output_path = "../../TestFiles/Output/";

        try
        // Extract logical structure from a PDF document
        {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "tagged.pdf")!!.absolutePath)
            doc.initSecurityHandler()

            mOutputListener!!.println("____________________________________________________________")
            mOutputListener!!.println("Sample 1 - Traverse logical structure tree...")
            run {
                val tree = doc.structTree
                if (tree.isValid) {
                    mOutputListener!!.println("Document has a StructTree root.")

                    for (i in 0 until tree.numKids) {
                        // Recursively get structure  info for all all child elements.
                        ProcessStructElement(tree.getKid(i), 0)
                    }
                } else {
                    mOutputListener!!.println("This document does not contain any logical structure.")
                }
            }
            mOutputListener!!.println("\nDone 1.")

            mOutputListener!!.println("____________________________________________________________")
            mOutputListener!!.println("Sample 2 - Get parent logical structure elements from")
            mOutputListener!!.println("layout elements.")
            run {
                val reader = ElementReader()
                val itr = doc.pageIterator
                while (itr.hasNext()) {
                    reader.begin(itr.next())
                    ProcessElements(reader)
                    reader.end()
                }
            }
            mOutputListener!!.println("\nDone 2.")

            mOutputListener!!.println("____________________________________________________________")
            mOutputListener!!.println("Sample 3 - 'XML style' extraction of PDF logical structure and page content.")
            run {
                //A map which maps page numbers(as Integers)
                //to page Maps(which map from struct mcid(as Integers) to
                //text Strings)
                val mcid_doc_map = TreeMap<Int, Map<Int, String>>()
                val reader = ElementReader()
                val itr = doc.pageIterator
                while (itr.hasNext()) {
                    val current = itr.next()
                    reader.begin(current)
                    val page_mcid_map = TreeMap<Int, String>()
                    mcid_doc_map[current!!.getIndex()] = page_mcid_map
                    ProcessElements2(reader, page_mcid_map)
                    reader.end()
                }

                val tree = doc.structTree
                if (tree.isValid) {
                    for (i in 0 until tree.numKids) {
                        ProcessStructElement2(tree.getKid(i), mcid_doc_map, 0)
                    }
                }
            }
            mOutputListener!!.println("\nDone 3.")

            doc.close()
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
        internal fun PrintIndent(indent: Int) {
            mOutputListener!!.println()
            for (i in 0 until indent) mOutputListener!!.print("  ")
        }

        // Used in code snippet 1.
        @Throws(PDFNetException::class)
        internal fun ProcessStructElement(element: SElement, indent: Int) {
            var indent = indent
            if (!element.isValid) {
                return
            }

            // Print out the type and title info, if any.
            PrintIndent(indent++)
            mOutputListener!!.print("Type: " + element.type)
            if (element.hasTitle()) {
                mOutputListener!!.print(". Title: " + element.title)
            }

            val num = element.numKids
            for (i in 0 until num) {
                // Check is the kid is a leaf node (i.e. it is a ContentItem).
                if (element.isContentItem(i)) {
                    val cont = element.getAsContentItem(i)
                    val type = cont.type

                    val page = cont.page

                    PrintIndent(indent)
                    mOutputListener!!.print("Content Item. Part of page #" + page.index)

                    PrintIndent(indent)
                    when (type) {
                        ContentItem.e_MCID, ContentItem.e_MCR -> mOutputListener!!.print("MCID: " + cont.mcid)
                        ContentItem.e_OBJR -> {
                            mOutputListener!!.print("OBJR ")
                            val ref_obj = cont.refObj
                            if (ref_obj != null)
                                mOutputListener!!.print("- Referenced Object#: " + ref_obj.objNum)
                        }
                        else -> {
                        }
                    }
                } else {  // the kid is another StructElement node.
                    ProcessStructElement(element.getAsStructElem(i), indent)
                }
            }
        }

        // Used in code snippet 2.
        @Throws(PDFNetException::class)
        internal fun ProcessElements(reader: ElementReader) {
            var element: Element?
            while (true)
            // Read page contents
            {
                element = reader.next()
                if (element == null) {
                    break
                }
                // In this sample we process only paths & text, but the code can be
                // extended to handle any element type.
                val type = element.type
                if (type == Element.e_path || type == Element.e_text || type == Element.e_path) {
                    when (type) {
                        Element.e_path                // Process path ...
                        -> mOutputListener!!.print("\nPATH: ")
                        Element.e_text                // Process text ...
                        -> mOutputListener!!.print("\nTEXT: " + element.textString + "\n  ")
                        Element.e_form                // Process form XObjects
                        -> mOutputListener!!.print("\nFORM XObject: ")
                    }//reader.FormBegin();
                    //ProcessElements(reader);
                    //reader.End();

                    // Check if the element is associated with any structural element.
                    // Content items are leaf nodes of the structure tree.
                    val struct_parent = element.parentStructElement
                    if (struct_parent.isValid) {
                        // Print out the parent structural element's type, title, and object number.
                        mOutputListener!!.print(" Type: " + struct_parent.type
                                + ", MCID: " + element.structMCID)
                        if (struct_parent.hasTitle()) {
                            mOutputListener!!.print(". Title: " + struct_parent.title)
                        }
                        mOutputListener!!.print(", Obj#: " + struct_parent.sdfObj.objNum)
                    }
                }
            }
        }

        // Used in code snippet 3.
        //typedef map<int, string> MCIDPageMap;
        //typedef map<int, MCIDPageMap> MCIDDocMap;

        // Used in code snippet 3.
        @Throws(PDFNetException::class)
        internal fun ProcessElements2(reader: ElementReader, mcid_page_map: MutableMap<Int, String>) {
            var element: Element?
            while (true)
            // Read page contents
            {
                element = reader.next()
                if (element == null) {
                    break
                }
                // In this sample we process only text, but the code can be extended
                // to handle paths, images, or any other Element type.
                val mcid = element.structMCID
                if (mcid >= 0 && element.type == Element.e_text) {
                    val `val` = element.textString
                    if (mcid_page_map.containsKey(mcid))
                        mcid_page_map[mcid] = mcid_page_map[mcid] as String + `val`
                    else
                        mcid_page_map[mcid] = `val`
                }
            }
        }

        // Used in code snippet 3.
        @Throws(PDFNetException::class)
        internal fun ProcessStructElement2(element: SElement, mcid_doc_map: Map<Int, Map<Int, String>>, indent: Int) {
            if (!element.isValid) {
                return
            }

            // Print out the type and title info, if any.
            PrintIndent(indent)
            mOutputListener!!.print("<" + element.type)
            if (element.hasTitle()) {
                mOutputListener!!.print(" title=\"" + element.title + "\"")
            }
            mOutputListener!!.print(">")

            val num = element.numKids
            for (i in 0 until num) {
                if (element.isContentItem(i)) {
                    val cont = element.getAsContentItem(i)
                    if (cont.type == ContentItem.e_MCID) {
                        val page_num = cont.page.index
                        if (mcid_doc_map.containsKey(page_num)) {
                            val mcid_page_map = mcid_doc_map[page_num]
                            val mcid_key = cont.mcid
                            if (mcid_page_map!!.containsKey(mcid_key)) {
                                mOutputListener!!.println(mcid_page_map.get(mcid_key))
                            }
                        }
                    }
                } else {  // the kid is another StructElement node.
                    ProcessStructElement2(element.getAsStructElem(i), mcid_doc_map, indent + 1)
                }
            }

            PrintIndent(indent)
            mOutputListener!!.print("</" + element.type + ">")
        }
    }

}