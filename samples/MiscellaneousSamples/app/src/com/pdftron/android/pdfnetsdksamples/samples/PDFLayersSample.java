//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.common.Matrix2D;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.ColorSpace;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.GState;
import com.pdftron.pdf.Image;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFDocViewPrefs;
import com.pdftron.pdf.PDFDraw;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.ocg.Config;
import com.pdftron.pdf.ocg.Context;
import com.pdftron.pdf.ocg.Group;
import com.pdftron.pdf.ocg.OCMD;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.SDFDoc;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;


/**
 * This sample demonstrates how to create layers in PDF.
 * The sample also shows how to extract and render PDF layers in documents
 * that contain optional content groups (OCGs)
 * 
 * With the introduction of PDF version 1.5 came the concept of Layers.
 * Layers, or as they are more formally known Optional Content Groups (OCGs),
 * refer to sections of content in a PDF document that can be selectively
 * viewed or hidden by document authors or consumers. This capability is useful
 * in CAD drawings, layered artwork, maps, multi-language documents etc.
 * 
 * Couple of notes regarding this sample:
 * ---------------------------------------
 * - This sample is using CreateLayer() utility method to create new OCGs.
 * CreateLayer() is relatively basic, however it can be extended to set
 * other optional entries in the 'OCG' and 'OCProperties' dictionary. For
 * a complete listing of possible entries in OC dictionary please refer to
 * section 4.10 'Optional Content' in the PDF Reference Manual.
 * - The sample is grouping all layer content into separate Form XObjects.
 * Although using PDFNet is is also possible to specify Optional Content in
 * Content Streams (Section 4.10.2 in PDF Reference), Optional Content in
 * XObjects results in PDFs that are cleaner, less-error prone, and faster
 * to process.
 */
public class PDFLayersSample extends PDFNetSample {

    public PDFLayersSample() {
        setTitle(R.string.sample_pdflayers_title);
        setDescription(R.string.sample_pdflayers_description);
    }

    @SuppressWarnings("unused")
    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        printHeader(outputListener);

        try {
            PDFDoc doc = new PDFDoc();

            // Create three layers...
            Group image_layer = createLayer(doc, "Image Layer");
            Group text_layer = createLayer(doc, "Text Layer");
            Group vector_layer = createLayer(doc, "Vector Layer");

            // Start a new page ------------------------------------
            Page page = doc.pageCreate();

            ElementBuilder builder = new ElementBuilder(); // ElementBuilder is used to build new Element objects
            ElementWriter writer = new ElementWriter(); // ElementWriter is used to write Elements to the page
            writer.begin(page); // Begin writing to the page

            // Add new content to the page and associate it with one of the layers.
            Element element = builder.createForm(createGroup1(doc, image_layer.getSDFObj()));
            writer.writeElement(element);

            element = builder.createForm(createGroup2(doc, vector_layer.getSDFObj()));
            writer.writeElement(element);

            // Add the text layer to the page...
            if (false) // set to true to enable 'ocmd' example.
            {
                // A bit more advanced example of how to create an OCMD text layer that
                // is visible only if text, image and path layers are all 'ON'.
                // An example of how to set 'Visibility Policy' in OCMD.
                Obj ocgs = doc.createIndirectArray();
                ocgs.pushBack(image_layer.getSDFObj());
                ocgs.pushBack(vector_layer.getSDFObj());
                ocgs.pushBack(text_layer.getSDFObj());
                OCMD text_ocmd = OCMD.create(doc, ocgs, OCMD.e_AllOn);
                element = builder.createForm(createGroup3(doc, text_ocmd.getSDFObj()));
            } else {
                element = builder.createForm(createGroup3(doc, text_layer.getSDFObj()));
            }
            writer.writeElement(element);

            // Add some content to the page that does not belong to any layer...
            // In this case this is a rectangle representing the page border.
            element = builder.createRect(0, 0, page.getPageWidth(), page.getPageHeight());
            element.setPathFill(false);
            element.setPathStroke(true);
            element.getGState().setLineWidth(40);
            writer.writeElement(element);

            writer.end(); // save changes to the current page
            doc.pagePushBack(page);

            // Set the default viewing preference to display 'Layer' tab.
            PDFDocViewPrefs prefs = doc.getViewPrefs();
            prefs.setPageMode(PDFDocViewPrefs.e_UseOC);

            doc.save(Utils.createExternalFile("pdf_layers.pdf").getAbsolutePath(), SDFDoc.e_linearized, null);
            addToFileList("pdf_layers.pdf");
            doc.close();
            outputListener.println("Done.");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // The following is a code snippet shows how to selectively render
        // and export PDF layers.
        // Note: the following code uses PDFDraw to export pages to PNG. Since PNG/TIFF
        // is not supported in the standard library, this operation will fail. Please,
        // uncomment the following code if using the full libraries.
        /*
        try {
            PDFDoc doc = new PDFDoc(Utils.createExternalFile("pdf_layers.pdf").getAbsolutePath());
            doc.initSecurityHandler();

            if (doc.hasOC() == false) {
                outputListener.println("The document does not contain 'Optional Content'");
            } else {
                Config init_cfg = doc.getOCGConfig();
                Context ctx = new Context(init_cfg);

                PDFDraw pdfdraw = new PDFDraw();
                pdfdraw.setImageSize(1000, 1000);
                pdfdraw.setOCGContext(ctx); // Render the page using the given OCG context.

                Page page = doc.getPage(1); // Get the first page in the document.
                pdfdraw.export(page, Utils.createExternalFile("pdf_layers_default.png").getAbsolutePath());
                addToFileList("pdf_layers_default.png");

                // Disable drawing of content that is not optional (i.e. is not part of any layer).
                ctx.setNonOCDrawing(false);

                // Now render each layer in the input document to a separate image.
                Obj ocgs = doc.getOCGs(); // Get the array of all OCGs in the document.
                if (ocgs != null) {
                    int i, sz = (int) ocgs.size();
                    for (i = 0; i < sz; ++i) {
                        Group ocg = new Group(ocgs.getAt(i));
                        ctx.resetStates(false);
                        ctx.setState(ocg, true);
                        String fname = "pdf_layers_" + ocg.getName() + ".png";
                        outputListener.println(fname);
                        pdfdraw.export(page, Utils.createExternalFile(fname).getAbsolutePath());
                        addToFileList(fname);
                    }
                }

                // Now draw content that is not part of any layer...
                ctx.setNonOCDrawing(true);
                ctx.setOCDrawMode(Context.e_NoOC);
                pdfdraw.export(page, Utils.createExternalFile("pdf_layers_non_oc.png").getAbsolutePath());
                addToFileList("pdf_layers_non_oc.png");
            }

            doc.close();
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }
        */
        printFooter(outputListener);
    }
    
    // A utility function used to add new Content Groups (Layers) to the/ document.
    static Group createLayer(PDFDoc doc, String layer_name) throws PDFNetException {
        Group grp = Group.create(doc, layer_name);
        Config cfg = doc.getOCGConfig();
        if (cfg == null) {
            cfg = Config.create(doc, true);
            cfg.setName("Default");
        }

        // Add the new OCG to the list of layers that should appear in PDF viewer GUI.
        Obj layer_order_array = cfg.getOrder();
        if (layer_order_array == null) {
            layer_order_array = doc.createIndirectArray();
            cfg.setOrder(layer_order_array);
        }
        layer_order_array.pushBack(grp.getSDFObj());

        return grp;
    }

    // Creates some content (3 images) and associate them with the image layer
    static Obj createGroup1(PDFDoc doc, Obj layer) throws PDFNetException {
        ElementWriter writer = new ElementWriter();
        writer.begin(doc);

        // Create an Image that can be reused in the document or on the same page.
        Image img = Image.create(doc.getSDFDoc(), Utils.getAssetTempFile(INPUT_PATH + "peppers.jpg").getAbsolutePath());

        ElementBuilder builder = new ElementBuilder();
        Element element = builder.createImage(img, new Matrix2D(img.getImageWidth() / 2, -145, 20, img.getImageHeight() / 2, 200, 150));
        writer.writePlacedElement(element);

        GState gstate = element.getGState(); // use the same image (just change its matrix)
        gstate.setTransform(200, 0, 0, 300, 50, 450);
        writer.writePlacedElement(element);

        // use the same image again (just change its matrix).
        writer.writePlacedElement(builder.createImage(img, 300, 600, 200, -150));

        Obj grp_obj = writer.end();

        // Indicate that this form (content group) belongs to the given layer (OCG).
        grp_obj.putName("Subtype", "Form");
        grp_obj.put("OC", layer);
        grp_obj.putRect("BBox", 0, 0, 1000, 1000); // Set the clip box for the content.

        return grp_obj;
    }

    // Creates some content (a path in the shape of a heart) and associate it with the vector layer
    static Obj createGroup2(PDFDoc doc, Obj layer) throws PDFNetException {
        ElementWriter writer = new ElementWriter();
        writer.begin(doc);

        // Create a path object in the shape of a heart.
        ElementBuilder builder = new ElementBuilder();
        builder.pathBegin(); // start constructing the path
        builder.moveTo(306, 396);
        builder.curveTo(681, 771, 399.75, 864.75, 306, 771);
        builder.curveTo(212.25, 864.75, -69, 771, 306, 396);
        builder.closePath();
        Element element = builder.pathEnd(); // the path geometry is now specified.

        // Set the path FILL color space and color.
        element.setPathFill(true);
        GState gstate = element.getGState();
        gstate.setFillColorSpace(ColorSpace.createDeviceCMYK());
        gstate.setFillColor(new ColorPt(1, 0, 0, 0)); // cyan

        // Set the path STROKE color space and color.
        element.setPathStroke(true);
        gstate.setStrokeColorSpace(ColorSpace.createDeviceRGB());
        gstate.setStrokeColor(new ColorPt(1, 0, 0)); // red
        gstate.setLineWidth(20);

        gstate.setTransform(0.5, 0, 0, 0.5, 280, 300);

        writer.writeElement(element);

        Obj grp_obj = writer.end();

        // Indicate that this form (content group) belongs to the given layer (OCG).
        grp_obj.putName("Subtype", "Form");
        grp_obj.put("OC", layer);
        grp_obj.putRect("BBox", 0, 0, 1000, 1000); // Set the clip box for the content.

        return grp_obj;
    }

    // Creates some text and associate it with the text layer
    static Obj createGroup3(PDFDoc doc, Obj layer) throws PDFNetException {
        ElementWriter writer = new ElementWriter();
        writer.begin(doc);

        // Create a path object in the shape of a heart.
        ElementBuilder builder = new ElementBuilder();

        // Begin writing a block of text
        Element element = builder.createTextBegin(Font.create(doc, Font.e_times_roman), 120);
        writer.writeElement(element);

        element = builder.createTextRun("A text layer!");

        // Rotate text 45 degrees, than translate 180 pts horizontally and 100 pts vertically.
        Matrix2D transform = Matrix2D.rotationMatrix(-45 * (3.1415 / 180.0));
        transform.concat(1, 0, 0, 1, 180, 100);
        element.setTextMatrix(transform);

        writer.writeElement(element);
        writer.writeElement(builder.createTextEnd());

        Obj grp_obj = writer.end();

        // Indicate that this form (content group) belongs to the given layer (OCG).
        grp_obj.putName("Subtype", "Form");
        grp_obj.put("OC", layer);
        grp_obj.putRect("BBox", 0, 0, 1000, 1000); // Set the clip box for the content.

        return grp_obj;
    }

}
