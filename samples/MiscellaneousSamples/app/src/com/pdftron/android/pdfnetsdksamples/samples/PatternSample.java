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
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PatternColor;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.SDFDoc;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

public class PatternSample extends PDFNetSample {

    public PatternSample() {
        setTitle(R.string.sample_pattern_title);
        setDescription(R.string.sample_pattern_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);

        printHeader(outputListener);

        try {
            PDFDoc doc = new PDFDoc();
            ElementWriter writer = new ElementWriter();
            ElementBuilder eb = new ElementBuilder();

            // The following sample illustrates how to create and use tiling patterns
            Page page = doc.pageCreate();
            writer.begin(page);

            Element element = eb.createTextBegin(Font.create(doc, Font.e_times_bold), 1);
            writer.writeElement(element); // Begin the text block

            String data = "G";
            element = eb.createTextRun(data);
            element.setTextMatrix(720, 0, 0, 720, 20, 240);
            GState gs = element.getGState();
            gs.setTextRenderMode(GState.e_fill_stroke_text);
            gs.setLineWidth(4);

            // Set the fill color space to the Pattern color space.
            gs.setFillColorSpace(ColorSpace.createPattern());
            gs.setFillColor(new PatternColor(CreateTilingPattern(doc)));

            writer.writeElement(element);
            writer.writeElement(eb.createTextEnd()); // Finish the text block

            writer.end(); // Save the page
            doc.pagePushBack(page);
            // -----------------------------------------------

            // The following sample illustrates how to create and use image
            // tiling pattern
            page = doc.pageCreate();
            writer.begin(page);

            eb.reset();
            element = eb.createRect(0, 0, 612, 794);

            // Set the fill color space to the Pattern color space.
            gs = element.getGState();
            gs.setFillColorSpace(ColorSpace.createPattern());
            gs.setFillColor(new PatternColor(CreateImageTilingPattern(doc)));
            element.setPathFill(true);

            writer.writeElement(element);

            writer.end(); // Save the page
            doc.pagePushBack(page);
            // -----------------------------------------------

            // / The following sample illustrates how to create and use PDF
            // shadings
            page = doc.pageCreate();
            writer.begin(page);

            eb.reset();
            element = eb.createRect(0, 0, 612, 794);

            // Set the fill color space to the Pattern color space.
            gs = element.getGState();
            gs.setFillColorSpace(ColorSpace.createPattern());

            gs.setFillColor(new PatternColor(CreateAxialShading(doc)));
            element.setPathFill(true);

            writer.writeElement(element);

            writer.end(); // save the page
            doc.pagePushBack(page);
            // -----------------------------------------------

            doc.save(Utils.createExternalFile("patterns.pdf").getAbsolutePath(), SDFDoc.e_remove_unused, null);
            doc.close();
            addToFileList("patterns.pdf");
            outputListener.println("Done. Result saved in patterns.pdf...");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }
        
        printFooter(outputListener);

    }
    
    static Obj CreateTilingPattern(PDFDoc doc) throws PDFNetException {
        ElementWriter writer = new ElementWriter();
        ElementBuilder eb = new ElementBuilder();

        // Create a new pattern content stream - a heart. ------------
        writer.begin(doc);
        eb.pathBegin();
        eb.moveTo(0, 0);
        eb.curveTo(500, 500, 125, 625, 0, 500);
        eb.curveTo(-125, 625, -500, 500, 0, 0);
        Element heart = eb.pathEnd();
        heart.setPathFill(true);

        // Set heart color to red.
        heart.getGState().setFillColorSpace(ColorSpace.createDeviceRGB());
        heart.getGState().setFillColor(new ColorPt(1, 0, 0));
        writer.writeElement(heart);

        Obj pattern_dict = writer.end();

        // Initialize pattern dictionary. For details on what each parameter represents please
        // refer to Table 4.22 (Section '4.6.2 Tiling Patterns') in PDF Reference Manual.
        pattern_dict.putName("Type", "Pattern");
        pattern_dict.putNumber("PatternType", 1);

        // TilingType - Constant spacing.
        pattern_dict.putNumber("TilingType", 1);

        // This is a Type1 pattern - A colored tiling pattern.
        pattern_dict.putNumber("PaintType", 1);

        // Set bounding box
        pattern_dict.putRect("BBox", -253, 0, 253, 545);

        // Create and set the matrix
        Matrix2D pattern_mtx = new Matrix2D(0.04, 0, 0, 0.04, 0, 0);
        pattern_dict.putMatrix("Matrix", pattern_mtx);

        // Set the desired horizontal and vertical spacing between pattern
        // cells,
        // measured in the pattern coordinate system.
        pattern_dict.putNumber("XStep", 1000);
        pattern_dict.putNumber("YStep", 1000);

        return pattern_dict; // finished creating the Pattern resource
    }

    static Obj CreateImageTilingPattern(PDFDoc doc) throws PDFNetException {
        ElementWriter writer = new ElementWriter();
        ElementBuilder eb = new ElementBuilder();

        // Create a new pattern content stream - a single bitmap object
        // ----------
        writer.begin(doc);
        Image image = Image.create(doc, Utils.getAssetTempFile(INPUT_PATH + "dice.jpg").getAbsolutePath());
        Element img_element = eb.createImage(image, 0, 0, image.getImageWidth(), image.getImageHeight());
        writer.writePlacedElement(img_element);
        Obj pattern_dict = writer.end();

        // Initialize pattern dictionary. For details on what each parameter represents please
        // refer to Table 4.22 (Section '4.6.2 Tiling Patterns') in PDF Reference Manual.
        pattern_dict.putName("Type", "Pattern");
        pattern_dict.putNumber("PatternType", 1);

        // TilingType - Constant spacing.
        pattern_dict.putNumber("TilingType", 1);

        // This is a Type1 pattern - A colored tiling pattern.
        pattern_dict.putNumber("PaintType", 1);

        // Set bounding box
        pattern_dict.putRect("BBox", -253, 0, 253, 545);

        // Create and set the matrix
        Matrix2D pattern_mtx = new Matrix2D(0.3, 0, 0, 0.3, 0, 0);
        pattern_dict.putMatrix("Matrix", pattern_mtx);

        // Set the desired horizontal and vertical spacing between pattern
        // cells,
        // measured in the pattern coordinate system.
        pattern_dict.putNumber("XStep", 300);
        pattern_dict.putNumber("YStep", 300);

        return pattern_dict; // finished creating the Pattern resource
    }

    static Obj CreateAxialShading(PDFDoc doc) throws PDFNetException {
        // Create a new Shading object ------------
        Obj pattern_dict = doc.createIndirectDict();

        // Initialize pattern dictionary. For details on what each parameter represents
        // please refer to Tables 4.30 and 4.26 in PDF Reference Manual
        pattern_dict.putName("Type", "Pattern");
        pattern_dict.putNumber("PatternType", 2); // 2 stands for shading

        Obj shadingDict = pattern_dict.putDict("Shading");
        shadingDict.putNumber("ShadingType", 2);
        shadingDict.putName("ColorSpace", "DeviceCMYK");

        // pass the coordinates of the axial shading to the output
        Obj shadingCoords = shadingDict.putArray("Coords");
        shadingCoords.pushBackNumber(0);
        shadingCoords.pushBackNumber(0);
        shadingCoords.pushBackNumber(612);
        shadingCoords.pushBackNumber(794);

        // pass the function to the axial shading
        Obj function = shadingDict.putDict("Function");
        Obj C0 = function.putArray("C0");
        C0.pushBackNumber(1);
        C0.pushBackNumber(0);
        C0.pushBackNumber(0);
        C0.pushBackNumber(0);

        Obj C1 = function.putArray("C1");
        C1.pushBackNumber(0);
        C1.pushBackNumber(1);
        C1.pushBackNumber(0);
        C1.pushBackNumber(0);

        Obj domain = function.putArray("Domain");
        domain.pushBackNumber(0);
        domain.pushBackNumber(1);

        function.putNumber("FunctionType", 2);
        function.putNumber("N", 1);

        return pattern_dict;
    }

}
