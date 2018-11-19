//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.common.Matrix2D;
import com.pdftron.common.PDFNetException;
import com.pdftron.filters.FilterReader;
import com.pdftron.pdf.CharData;
import com.pdftron.pdf.CharIterator;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.ColorSpace;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementReader;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.GSChangesIterator;
import com.pdftron.pdf.GState;
import com.pdftron.pdf.Image2RGB;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.PathData;
import com.pdftron.pdf.PatternColor;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Shading;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

public class ElementReaderAdvSample extends PDFNetSample {

    private static OutputListener mOutputListener;
    
    public ElementReaderAdvSample() {
        setTitle(R.string.sample_elementreaderadv_title);
        setDescription(R.string.sample_elementreaderadv_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        mOutputListener = outputListener;
        
        printHeader(outputListener);
        
        try {   // Extract text data from all pages in the document

            //mOutputListener.println("Extract page element information from all pages in the document.");

            PDFDoc doc = new PDFDoc(Utils.getAssetInputStream(INPUT_PATH + "newsletter.pdf"));
            doc.initSecurityHandler();

            PageIterator page_begin = doc.getPageIterator();

            ElementReader page_reader = new ElementReader();

            PageIterator itr;
            int j = 0;
            for (itr = page_begin; itr.hasNext() && j < 1; j++) {   //  Let's read only first pages
                Page nextPage = (Page)(itr.next());
                mOutputListener.println("Page " + nextPage.getIndex() + "--------------------");
                page_reader.begin(nextPage);
                ProcessElements(page_reader);
                page_reader.end();
            }

            //Close the open document to free up document
            //memory sooner than waiting for the
            //garbage collector
            doc.close();
        } catch (Exception e) {
            mOutputListener.println(e.getStackTrace());
        }
        
        printFooter(outputListener);
    }
    
    @SuppressWarnings("unused")
    static void ProcessPath(ElementReader reader, Element path) throws PDFNetException {
        if (path.isClippingPath()) {
            mOutputListener.println("This is a clipping path");
        }

        PathData pathData = path.getPathData();
        double[] data = pathData.getPoints();
        byte[] opr = pathData.getOperators();

        double x1, y1, x2, y2, x3, y3;
        // Use path.getCTM() if you are interested in CTM (current transformation matrix).

        mOutputListener.print(" Path Data Points := \"");
        int data_index = 0;
        for (int opr_index = 0; opr_index < opr.length; ++opr_index) {
            switch (opr[opr_index]) {
            case PathData.e_moveto:
                x1 = data[data_index]; ++data_index;
                y1 = data[data_index]; ++data_index;
                mOutputListener.print("M" + x1 + " " + y1);
                break;
            case PathData.e_lineto:
                x1 = data[data_index]; ++data_index;
                y1 = data[data_index]; ++data_index;
                mOutputListener.print(" L" + x1 + " " + y1);
                break;
            case PathData.e_cubicto:
                x1 = data[data_index]; ++data_index;
                y1 = data[data_index]; ++data_index;
                x2 = data[data_index]; ++data_index;
                y2 = data[data_index]; ++data_index;
                x3 = data[data_index]; ++data_index;
                y3 = data[data_index]; ++data_index;
                mOutputListener.print(" C" + x1 + " " + y1 + " " + x2 + " " + y2 + " " + x3 + " " + y3);
                break;
            case PathData.e_rect:
                {
                    x1 = data[data_index]; ++data_index;
                    y1 = data[data_index]; ++data_index;
                    double w = data[data_index]; ++data_index;
                    double h = data[data_index]; ++data_index;
                    x2 = x1 + w;
                    y2 = y1;
                    x3 = x2;
                    y3 = y1 + h;
                    double x4 = x1;
                    double y4 = y3;
                    mOutputListener.print("M" + x1 + " " + y1 + " L" + x2 + " " + y2 + " L" + x3 + " " + y3 + " L" + x4 + " " + y4 + " Z");
                }
                break;
            case PathData.e_closepath:
                mOutputListener.println(" Close Path");
                break;
            default:
                throw new PDFNetException("Invalid Element Type", 0, "", "", "");
            }
        }

        mOutputListener.print("\" ");

        GState gs = path.getGState();

        // Set Path State 0 (stroke, fill, fill-rule) -----------------------------------
        if (path.isStroked()) {
            mOutputListener.println("Stroke path"); 

            if (gs.getStrokeColorSpace().getType() == ColorSpace.e_pattern) {
                mOutputListener.println("Path has associated pattern"); 
            } else {
                // Get stroke color (you can use PDFNet color conversion facilities)
                ColorPt rgb = new ColorPt();
                rgb = gs.getStrokeColor();
                double v = rgb.get(0);
                rgb = gs.getStrokeColorSpace().convert2RGB(rgb);
                v = rgb.get(0);
            }
        } else {
            // Do not stroke path
        }

        if (path.isFilled()) {
            mOutputListener.println("Fill path"); 

            if (gs.getFillColorSpace().getType() == ColorSpace.e_pattern) {
                mOutputListener.println("Path has associated pattern");
                PatternColor pat = gs.getFillPattern();
                int type = pat.getType();
                if (type == PatternColor.e_shading) {
                    mOutputListener.println("Shading"); 
                    Shading shading = pat.getShading();
                    if (shading.getType() == Shading.e_function_shading) {
                        mOutputListener.println("FUNCT"); 
                    } else if (shading.getType() == Shading.e_axial_shading) {
                        mOutputListener.println("AXIAL"); 
                    } else if (shading.getType() == Shading.e_radial_shading) {
                        mOutputListener.println("RADIAL"); 
                    }
                } else if (type == PatternColor.e_colored_tiling_pattern) {
                    mOutputListener.println("e_colored_tiling_pattern"); 
                } else if (type == PatternColor.e_uncolored_tiling_pattern) {
                    mOutputListener.println("e_uncolored_tiling_pattern"); 
                } else {
                    mOutputListener.println("?");
                }
            } else {
                ColorPt rgb = new ColorPt();
                rgb = gs.getFillColor();
                double v = rgb.get(0);
                rgb = gs.getFillColorSpace().convert2RGB(rgb);
                v = rgb.get(0);
            }
        } else {
            // Do not fill path
        }

        // Process any changes in graphics state  ---------------------------------

        GSChangesIterator gs_itr = reader.getChangesIterator();
        while (gs_itr.hasNext()) {
            switch(((Integer)(gs_itr.next())).intValue()) {
            case GState.e_transform:
                // Get transform matrix for this element. Unlike path.GetCTM() 
                // that return full transformation matrix gs.GetTransform() return 
                // only the transformation matrix that was installed for this element.
                //
                //gs.getTransform();
                break;
            case GState.e_line_width:
                //gs.getLineWidth();
                break;
            case GState.e_line_cap:
                //gs.getLineCap();
                break;
            case GState.e_line_join:
                //gs.getLineJoin();
                break;
            case GState.e_flatness:
                break;
            case GState.e_miter_limit:
                //gs.getMiterLimit();
                break;
            case GState.e_dash_pattern:
                {
                    //double[] dashes;
                    //dashes=gs.getDashes();
                    //gs.getPhase();
                }
                break;
            case GState.e_fill_color:
                {
                    if ( gs.getFillColorSpace().getType() == ColorSpace.e_pattern &&
                        gs.getFillPattern().getType() != PatternColor.e_shading ) {
                        //process the pattern data
                        reader.patternBegin(true);
                        ProcessElements(reader);
                        reader.end();
                    }
                }
                break;
            }
        }
        reader.clearChangeList();
    }

    @SuppressWarnings("unused")
    static void ProcessText(ElementReader page_reader) throws PDFNetException {
        // Begin text element
        mOutputListener.println("Begin Text Block:");

        Element element; 
        while ((element = page_reader.next())!=null) 
        {
            switch (element.getType())
            {
            case Element.e_text_end: 
                // Finish the text block
                mOutputListener.println("End Text Block.");
                return;

            case Element.e_text:
                {
                    GState gs =  element.getGState();

                    ColorSpace cs_fill = gs.getFillColorSpace();
                    ColorPt fill = gs.getFillColor();

                    ColorPt out;
                    out=cs_fill.convert2RGB(fill);

                    ColorSpace cs_stroke = gs.getStrokeColorSpace();
                    ColorPt stroke = gs.getStrokeColor();

                    Font font = gs.getFont();

                    mOutputListener.println("Font Name: " + font.getName());
                    //font.isFixedWidth();
                    //font.isSerif();
                    //font.isSymbolic();
                    //font.isItalic();
                    // ... 

                    //double font_size = gs.getFontSize();
                    //double word_spacing = gs.getWordSpacing();
                    //double char_spacing = gs.getCharSpacing();
                    //String txt = element.getTextString();

                    if ( font.getType() == Font.e_Type3 ) {
                        //type 3 font, process its data
                        for (CharIterator itr = element.getCharIterator(); itr.hasNext();) {
                            page_reader.type3FontBegin((CharData)(itr.next()), null);
                            ProcessElements(page_reader);
                            page_reader.end();
                        }
                    } else {
                        Matrix2D text_mtx = element.getTextMatrix();
                        double x, y;
                        long char_code;

                        for (CharIterator itr = element.getCharIterator(); itr.hasNext();) {
                            CharData data=(CharData)(itr.next());
                            char_code = data.getCharCode();
                            mOutputListener.print("Character code: ");
                            mOutputListener.print(String.valueOf((char)char_code));

                            x = data.getGlyphX();       // character positioning information
                            y = data.getGlyphY();

                            // Use element.getCTM() if you are interested in the CTM 
                            // (current transformation matrix).
                            Matrix2D ctm = element.getCTM();

                            // To get the exact character positioning information you need to 
                            // concatenate current text matrix with CTM and then multiply 
                            // relative positioning coordinates with the resulting matrix.
                            //
                            Matrix2D mtx = ctm.multiply(text_mtx);
                            Point t = mtx.multPoint(x, y);
                            x = t.x;
                            y = t.y;
                            //mOutputListener.println(" Position: x=" + x + " y=" + y );
                        }

                        mOutputListener.println("");
                    }
                }
                break;
            }
        }
    }

    @SuppressWarnings("unused")
    static void ProcessImage(Element image) throws PDFNetException
    {
        boolean image_mask = image.isImageMask();
        boolean interpolate = image.isImageInterpolate();
        int width = image.getImageWidth();
        int height = image.getImageHeight();
        int out_data_sz = width * height * 3;

        mOutputListener.println("Image: width=" + width + " height=" + height);

        //Matrix2D mtx = image.getCTM();  // image matrix (page positioning info)

        // You can use GetImageData to read the raw (decoded) image data
        //image.getBitsPerComponent();
        //image.getImageData();   // get raw image data
        // .... or use Image2RGB filter that converts every image to RGB format,
        // This should save you time since you don't need to deal with color conversions, 
        // image up-sampling, decoding etc.

        Image2RGB img_conv = new Image2RGB(image);    // Extract and convert image to RGB 8-bpc format
        FilterReader reader = new FilterReader(img_conv);

        // A buffer used to keep image data.
        byte[] buf = new byte[out_data_sz];
        long image_data_out = reader.read(buf);
        // image_data_out.front() contains RGB image data.

        // Note that you don't need to read a whole image at a time. Alternatively
        // you can read a chunk at a time by repeatedly calling reader.Read(buf) 
        // until the function returns 0. 
    }

    static void ProcessElements(ElementReader reader) throws PDFNetException {
        Element element;
        while ((element = reader.next()) != null) {     // Read page contents
            switch (element.getType()) {
            case Element.e_path:                        // Process path data...
                {
                    ProcessPath(reader, element);
                }
                break; 
            case Element.e_text_begin:                  // Process text block...
                {
                    ProcessText(reader);
                }
                break;
            case Element.e_form:                        // Process form XObjects
                {
                    reader.formBegin(); 
                    ProcessElements(reader);
                    reader.end();
                }
                break; 
            case Element.e_image:                       // Process Images
                {
                    ProcessImage(element);
                }   
                break; 
            }
        }
    }
}
