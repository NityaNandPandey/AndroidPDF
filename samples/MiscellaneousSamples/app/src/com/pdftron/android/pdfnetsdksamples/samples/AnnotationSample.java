//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Annot.BorderStyle;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.Destination;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.SDFDoc;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

public class AnnotationSample extends PDFNetSample {
    
    private static OutputListener mOutputListener;
    
    public AnnotationSample() {
        setTitle(R.string.sample_annotation_title);
        setDescription(R.string.sample_annotation_description);
    }
    
    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        mOutputListener = outputListener;
        printHeader(outputListener);

        try {
            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "numbered.pdf"));
            doc.initSecurityHandler();

            // An example of using SDF/Cos API to add any type of annotations.
            AnnotationLowLevelAPI(doc);
            doc.save(Utils.createExternalFile("annotation_test1.pdf").getAbsolutePath(), SDFDoc.e_linearized, null);
            addToFileList("annotation_test1.pdf");

            // An example of using the high-level PDFNet API to read existing
            // annotations, to edit existing annotations, and to create new
            // annotation from scratch.
            AnnotationHighLevelAPI(doc);
            doc.save(Utils.createExternalFile("annotation_test2.pdf").getAbsolutePath(), SDFDoc.e_linearized, null);
            addToFileList("annotation_test2.pdf");

            doc.close();
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        printFooter(outputListener);
    }
    
    static void AnnotationLowLevelAPI(PDFDoc doc) throws PDFNetException {
        Page page = (Page) (doc.getPageIterator().next());
        Obj annots = page.getAnnots();

        if (annots == null) {
            // If there are no annotations, create a new annotation
            // array for the page.
            annots = doc.createIndirectArray();
            page.getSDFObj().put("Annots", annots);
        }

        // Create a Text annotation
        Obj annot = doc.createIndirectDict();
        annot.putName("Subtype", "Text");
        annot.putBool("Open", true);
        annot.putString("Contents", "The quick brown fox ate the lazy mouse.");
        annot.putRect("Rect", 266, 116, 430, 204);

        // Insert the annotation in the page annotation array
        annots.pushBack(annot);

        // Create a Link annotation
        Obj link1 = doc.createIndirectDict();
        link1.putName("Subtype", "Link");
        Destination dest = Destination.createFit((Page) (doc.getPage(2)));
        link1.put("Dest", dest.getSDFObj());
        link1.putRect("Rect", 85, 705, 503, 661);
        annots.pushBack(link1);

        // Create another Link annotation
        Obj link2 = doc.createIndirectDict();
        link2.putName("Subtype", "Link");
        Destination dest2 = Destination.createFit((Page) (doc.getPage(3)));
        link2.put("Dest", dest2.getSDFObj());
        link2.putRect("Rect", 85, 638, 503, 594);
        annots.pushBack(link2);

        // Note that PDFNet APi can be used to modify existing annotations.
        // In the following example we will modify the second link annotation
        // (link2) so that it points to the 10th page. We also use a different
        // destination page fit type.

        // link2 = annots.getAt((int) (annots.size()-1));
        link2.put("Dest", Destination.createXYZ((Page) (doc.getPage(10)), 100, 792 - 70, 10).getSDFObj());

        // Create a third link annotation with a hyperlink action (all other
        // annotation types can be created in a similar way)
        Obj link3 = doc.createIndirectDict();
        link3.putName("Subtype", "Link");
        link3.putRect("Rect", 85, 570, 503, 524);

        // Create a URI action
        Obj action = link3.putDict("A");
        action.putName("S", "URI");
        action.putString("URI", "http://www.pdftron.com");

        annots.pushBack(link3);
    }

    static void AnnotationHighLevelAPI(PDFDoc doc) throws PDFNetException {
        // The following code snippet traverses all annotations in the document
        mOutputListener.println("Traversing all annotations in the document...");

        int page_num = 1;
        for (PageIterator itr = doc.getPageIterator(); itr.hasNext();) {
            mOutputListener.println("Page " + (page_num++) + ": ");

            Page page = (Page) (itr.next());
            int num_annots = page.getNumAnnots();
            for (int i = 0; i < num_annots; ++i) {
                Annot annot = page.getAnnot(i);
                if (annot.isValid() == false)
                    continue;
                mOutputListener.println("Annot Type: " + annot.getSDFObj().get("Subtype").value().getName());

                double[] bbox = annot.getRect().get();
                mOutputListener.println("  Position: " + ", " + bbox[0] + ", " + bbox[1] + ", " + bbox[2] + ", " + bbox[3]);

                switch (annot.getType()) {
                case Annot.e_Link: {
                    com.pdftron.pdf.annots.Link link = new com.pdftron.pdf.annots.Link(annot);
                    Action action = link.getAction();
                    if (action.isValid() == false)
                        continue;
                    if (action.getType() == Action.e_GoTo) {
                        Destination dest = action.getDest();
                        if (dest.isValid() == false) {
                            mOutputListener.println("  Destination is not valid.");
                        } else {
                            int page_link = dest.getPage().getIndex();
                            mOutputListener.println("  Links to: page number " + page_link + " in this document");
                        }
                    } else if (action.getType() == Action.e_URI) {
                        String uri = action.getSDFObj().get("URI").value().getAsPDFText();
                        mOutputListener.println("  Links to: " + uri);
                    }
                    // ...
                }
                    break;
                case Annot.e_Widget:
                    break;
                case Annot.e_FileAttachment:
                    break;
                // ...
                default:
                    break;
                }
            }
        }

        // Use the high-level API to create new annotations.
        Page first_page = doc.getPage(1);

        // Create a hyperlink...
        com.pdftron.pdf.annots.Link hyperlink = com.pdftron.pdf.annots.Link.create(doc,
                new Rect(85, 570, 503, 524),
                Action.createURI(doc, "http://www.pdftron.com"));
        first_page.annotPushBack(hyperlink);

        // Create an intra-document link...
        Action goto_page_3 = Action.createGoto(Destination.createFitH((Page) (doc.getPage(3)), 0));
        com.pdftron.pdf.annots.Link link = com.pdftron.pdf.annots.Link.create(doc.getSDFDoc(),
                new Rect(85, 458, 503, 502),
                goto_page_3);

        // Set the annotation border width to 3 points...
        BorderStyle border_style = new Annot.BorderStyle(Annot.BorderStyle.e_solid, 10, 0, 0);
        link.setBorderStyle(border_style);
        link.setColor(new ColorPt(1, 0, 0), 3);

        // Add the new annotation to the first page
        first_page.annotPushBack(link);

        // Create a stamp annotation ...
        com.pdftron.pdf.annots.RubberStamp stamp = com.pdftron.pdf.annots.RubberStamp.create(doc, new Rect(30, 30, 300, 200));
        stamp.setIcon("Draft");
        first_page.annotPushBack(stamp);

        // Create a file attachment annotation (embed the 'peppers.jpg').
        com.pdftron.pdf.annots.FileAttachment file_attach = com.pdftron.pdf.annots.FileAttachment.create(doc, new Rect(80, 280, 200, 320),
                Utils.getAssetTempFile(INPUT_PATH + "peppers.jpg").getAbsolutePath());
        first_page.annotPushBack(file_attach);

        com.pdftron.pdf.annots.Circle circle = com.pdftron.pdf.annots.Circle.create(doc, new Rect(10, 110, 100, 200));
        circle.setInteriorColor(new ColorPt(0, 0.5, 1), 3);
        circle.setTitle("This is a title for the circle");
        circle.setColor(new ColorPt(0, 1, 0), 3);
        circle.setInteriorColor(new ColorPt(0, 0, 1), 3);
        circle.setContentRect(new Rect(12, 112, 98, 198));
        circle.setOpacity(0.5);
        first_page.annotPushBack(circle);

        com.pdftron.pdf.annots.Ink ink = com.pdftron.pdf.annots.Ink.create(doc, new Rect(110, 10, 300, 200));
        Point pt3 = new Point(110, 10);
        // pt3.x = 110; pt3.y = 10;
        ink.setPoint(0, 0, pt3);
        pt3.x = 150;
        pt3.y = 50;
        ink.setPoint(0, 1, pt3);
        pt3.x = 190;
        pt3.y = 60;
        ink.setPoint(0, 2, pt3);
        pt3.x = 180;
        pt3.y = 90;
        ink.setPoint(1, 0, pt3);
        pt3.x = 190;
        pt3.y = 95;
        ink.setPoint(1, 1, pt3);
        pt3.x = 200;
        pt3.y = 100;
        ink.setPoint(1, 2, pt3);
        pt3.x = 166;
        pt3.y = 86;
        ink.setPoint(2, 0, pt3);
        pt3.x = 196;
        pt3.y = 96;
        ink.setPoint(2, 1, pt3);
        pt3.x = 221;
        pt3.y = 121;
        ink.setPoint(2, 2, pt3);
        pt3.x = 288;
        pt3.y = 188;
        ink.setPoint(2, 3, pt3);
        ink.setColor(new ColorPt(0, 1, 1), 3);
        first_page.annotPushBack(ink);

        // ...
    }
}
