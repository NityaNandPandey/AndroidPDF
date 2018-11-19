//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import java.util.Map;
import java.util.TreeMap;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementReader;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.struct.ContentItem;
import com.pdftron.pdf.struct.SElement;
import com.pdftron.pdf.struct.STree;
import com.pdftron.sdf.Obj;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

public class LogicalStructureSample extends PDFNetSample {

    private static OutputListener mOutputListener;
    
    public LogicalStructureSample() {
        setTitle(R.string.sample_logicalstructure_title);
        setDescription(R.string.sample_logicalstructure_description);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        mOutputListener = outputListener;

        printHeader(outputListener);

        try {   // Extract logical structure from a PDF document
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "tagged.pdf"));
            doc.initSecurityHandler();

            outputListener.println("--------------------");
            outputListener.println("Sample 1 - Traverse logical structure tree...");
            {
                STree tree = doc.getStructTree();
                if (tree.isValid()) {
                    outputListener.println("Document has a StructTree root.");

                    for (int i = 0; i < tree.getNumKids(); ++i) {
                        // Recursively get structure info for all all child
                        // elements.
                        ProcessStructElement(tree.getKid(i), 0);
                    }
                } else {
                    outputListener.println("This document does not contain any logical structure.");
                }
            }
            outputListener.println("\nDone 1.");

            outputListener.println("--------------------");
            outputListener.println("Sample 2 - Get parent logical structure elements from");
            outputListener.println("layout elements.");
            {
                ElementReader reader = new ElementReader();
                for (PageIterator itr = doc.getPageIterator(); itr.hasNext();) {
                    reader.begin((Page) (itr.next()));
                    ProcessElements(reader);
                    reader.end();
                }
            }
            outputListener.println("\nDone 2.");

            outputListener.println("--------------------");
            outputListener.println("Sample 3 - 'XML style' extraction of PDF logical structure and page content.");
            {
                // A map which maps page numbers(as Integers)
                // to page Maps(which map from struct mcid (as Integers) to
                // text Strings)
                Map mcid_doc_map = new TreeMap();
                ElementReader reader = new ElementReader();
                for (PageIterator itr = doc.getPageIterator(); itr.hasNext();) {
                    Page current = (Page) (itr.next());
                    reader.begin(current);
                    Map page_mcid_map = new TreeMap();
                    mcid_doc_map.put(Integer.valueOf(current.getIndex()),page_mcid_map);
                    ProcessElements2(reader, page_mcid_map);
                    reader.end();
                }

                STree tree = doc.getStructTree();
                if (tree.isValid()) {
                    for (int i = 0; i < tree.getNumKids(); ++i) {
                        ProcessStructElement2(tree.getKid(i), mcid_doc_map, 0);
                    }
                }
            }
            outputListener.println("\nDone 3.");

            doc.close();
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }
        
        printFooter(outputListener);

    }
    
    static void PrintIndent(int indent) {
        mOutputListener.println();
        for (int i = 0; i < indent; ++i)
            mOutputListener.print("  ");
    }

    // Used in code snippet 1.
    static void ProcessStructElement(SElement element, int indent) throws PDFNetException {
        if (!element.isValid()) {
            return;
        }

        // Print out the type and title info, if any.
        PrintIndent(indent++);
        mOutputListener.print("Type: " + element.getType());
        if (element.hasTitle()) {
            mOutputListener.print(". Title: " + element.getTitle());
        }

        int num = element.getNumKids();
        for (int i = 0; i < num; ++i) {
            // Check is the kid is a leaf node (i.e. it is a ContentItem).
            if (element.isContentItem(i)) {
                ContentItem cont = element.getAsContentItem(i);
                int type = cont.getType();

                Page page = cont.getPage();

                PrintIndent(indent);
                mOutputListener.print("Content Item. Part of page #" + page.getIndex());

                PrintIndent(indent);
                switch (type) {
                case ContentItem.e_MCID:
                case ContentItem.e_MCR:
                    mOutputListener.print("MCID: " + cont.getMCID());
                    break;
                case ContentItem.e_OBJR: {
                    mOutputListener.print("OBJR ");
                    Obj ref_obj = cont.getRefObj();
                    if (ref_obj != null)
                        mOutputListener.print("- Referenced Object#: " + ref_obj.getObjNum());
                }
                    break;
                default:
                    break;
                }
            } else { // the kid is another StructElement node.
                ProcessStructElement(element.getAsStructElem(i), indent);
            }
        }
    }
    
    // Used in code snippet 2.
    static void ProcessElements(ElementReader reader) throws PDFNetException {
        Element element;
        while ((element = reader.next()) != null) { // Read page contents
            // In this sample we process only paths & text, but the code can be
            // extended to handle any element type.
            int type = element.getType();
            if (type == Element.e_path || type == Element.e_text || type == Element.e_path) {
                switch (type) {
                case Element.e_path: // Process path ...
                    mOutputListener.print("\nPATH: ");
                    break;
                case Element.e_text: // Process text ...
                    mOutputListener.print("\nTEXT: " + element.getTextString() + "\n  ");
                    break;
                case Element.e_form: // Process form XObjects
                    mOutputListener.print("\nFORM XObject: ");
                    // reader.FormBegin();
                    // ProcessElements(reader);
                    // reader.End();
                    break;
                }

                // Check if the element is associated with any structural
                // element.
                // Content items are leaf nodes of the structure tree.
                SElement struct_parent = element.getParentStructElement();
                if (struct_parent.isValid()) {
                    // Print out the parent structural element's type, title,
                    // and object number.
                    mOutputListener.print(" Type: " + struct_parent.getType() + ", MCID: " + element.getStructMCID());
                    if (struct_parent.hasTitle()) {
                        mOutputListener.print(". Title: " + struct_parent.getTitle());
                    }
                    mOutputListener.print(", Obj#: " + struct_parent.getSDFObj().getObjNum());
                }
            }
        }
    }
    
    // Used in code snippet 3.
    //typedef map<int, string> MCIDPageMap;
    //typedef map<int, MCIDPageMap> MCIDDocMap;

    // Used in code snippet 3.
    @SuppressWarnings({ "rawtypes", "unchecked" })
    static void ProcessElements2(ElementReader reader, Map mcid_page_map) throws PDFNetException {
        Element element;
        while ((element = reader.next()) != null) { // Read page contents
            // In this sample we process only text, but the code can be extended
            // to handle paths, images, or any other Element type.
            int mcid = element.getStructMCID();
            Integer key_mcid = Integer.valueOf(mcid);
            if (mcid >= 0 && element.getType() == Element.e_text) {
                String val = element.getTextString();
                if (mcid_page_map.containsKey(key_mcid))
                    mcid_page_map.put(key_mcid, ((String) (mcid_page_map.get(key_mcid)) + val));
                else
                    mcid_page_map.put(key_mcid, val);
            }
        }
    }
    
    // Used in code snippet 3.
    @SuppressWarnings("rawtypes")
    static void ProcessStructElement2(SElement element, Map mcid_doc_map, int indent) throws PDFNetException {
        if (!element.isValid()) {
            return;
        }

        // Print out the type and title info, if any.
        PrintIndent(indent);
        mOutputListener.print("<" + element.getType());
        if (element.hasTitle()) {
            mOutputListener.print(" title=\"" + element.getTitle() + "\"");
        }
        mOutputListener.print(">");

        int num = element.getNumKids();
        for (int i = 0; i < num; ++i) {
            if (element.isContentItem(i)) {
                ContentItem cont = element.getAsContentItem(i);
                if (cont.getType() == ContentItem.e_MCID) {
                    int page_num = cont.getPage().getIndex();
                    Integer page_num_key = Integer.valueOf(page_num);
                    if (mcid_doc_map.containsKey(page_num_key)) {
                        Map mcid_page_map = (Map) (mcid_doc_map.get(page_num_key));
                        Integer mcid_key = Integer.valueOf(cont.getMCID());
                        if (mcid_page_map.containsKey(mcid_key)) {
                            mOutputListener.println(mcid_page_map.get(mcid_key).toString());
                        }
                    }
                }
            } else { // the kid is another StructElement node.
                ProcessStructElement2(element.getAsStructElem(i), mcid_doc_map, indent + 1);
            }
        }

        PrintIndent(indent);
        mOutputListener.print("</" + element.getType() + ">");
    }
    
}
