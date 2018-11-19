//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.common.Matrix2D
import com.pdftron.common.PDFNetException
import com.pdftron.filters.FilterReader
import com.pdftron.pdf.CharData
import com.pdftron.pdf.CharIterator
import com.pdftron.pdf.ColorPt
import com.pdftron.pdf.ColorSpace
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementReader
import com.pdftron.pdf.Font
import com.pdftron.pdf.GSChangesIterator
import com.pdftron.pdf.GState
import com.pdftron.pdf.Image2RGB
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.PageIterator
import com.pdftron.pdf.PathData
import com.pdftron.pdf.PatternColor
import com.pdftron.pdf.Shading

import java.util.ArrayList

class ElementReaderAdvTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_elementreaderadv_title)
        setDescription(R.string.sample_elementreaderadv_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        // string output_path = "../../TestFiles/Output/";

        try
        // Extract text data from all pages in the document
        {
            mOutputListener!!.println("__________________________________________________")
            mOutputListener!!.println("Extract page element information from all ")
            mOutputListener!!.println("pages in the document.")

            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath)
            doc.initSecurityHandler()

            val pgnum = doc.pageCount
            val page_begin = doc.pageIterator

            val page_reader = ElementReader()

            val itr: PageIterator

            itr = page_begin
            while (itr.hasNext())
            //  Read every page
            {
                val nextPage = itr.next()
                mOutputListener!!.println("Page " + nextPage?.index +
                        "----------------------------------------")
                page_reader.begin(nextPage)
                ProcessElements(page_reader)
                page_reader.end()
            }

            //Close the open document to free up document
            //memory sooner than waiting for the
            //garbage collector
            doc.close()
            mOutputListener!!.println("Done")
        } catch (e: Exception) {
            mOutputListener!!.println(e.stackTrace)
        }

        for (file in mFileList) {
            addToFileList(file)
        }
        printFooter(outputListener)
    }

    companion object {

        private var mOutputListener: OutputListener? = null

        private val mFileList = ArrayList<String>()

        internal var m_buf: String? = null

        @Throws(PDFNetException::class)
        internal fun ProcessPath(reader: ElementReader, path: Element) {
            if (path.isClippingPath) {
                mOutputListener!!.println("This is a clipping path")
            }

            val pathData = path.pathData
            val data = pathData.points
            val opr = pathData.operators

            var x1: Double
            var y1: Double
            var x2: Double
            var y2: Double
            var x3: Double
            var y3: Double
            // Use path.getCTM() if you are interested in CTM (current transformation matrix).

            mOutputListener!!.print(" Path Data Points := \"")
            var data_index = 0
            for (opr_index in opr.indices) {
                when (opr[opr_index]) {
                    PathData.e_moveto.toByte() -> {
                        x1 = data[data_index]
                        ++data_index
                        y1 = data[data_index]
                        ++data_index
                        mOutputListener!!.print("M$x1 $y1")
                    }
                    PathData.e_lineto.toByte() -> {
                        x1 = data[data_index]
                        ++data_index
                        y1 = data[data_index]
                        ++data_index
                        mOutputListener!!.print(" L$x1 $y1")
                    }
                    PathData.e_cubicto.toByte() -> {
                        x1 = data[data_index]
                        ++data_index
                        y1 = data[data_index]
                        ++data_index
                        x2 = data[data_index]
                        ++data_index
                        y2 = data[data_index]
                        ++data_index
                        x3 = data[data_index]
                        ++data_index
                        y3 = data[data_index]
                        ++data_index
                        mOutputListener!!.print(" C$x1 $y1 $x2 $y2 $x3 $y3")
                    }
                    PathData.e_rect.toByte() -> {
                        x1 = data[data_index]
                        ++data_index
                        y1 = data[data_index]
                        ++data_index
                        val w = data[data_index]
                        ++data_index
                        val h = data[data_index]
                        ++data_index
                        x2 = x1 + w
                        y2 = y1
                        x3 = x2
                        y3 = y1 + h
                        mOutputListener!!.print("M$x1 $y1 L$x2 $y2 L$x3 $y3 L$x1 $y3 Z")
                    }
                    PathData.e_closepath.toByte() -> mOutputListener!!.println(" Close Path")
                    else -> throw PDFNetException("Invalid Element Type", 0, "", "", "")
                }
            }

            mOutputListener!!.print("\" ")

            val gs = path.gState

            // Set Path State 0 (stroke, fill, fill-rule) -----------------------------------
            if (path.isStroked) {
                mOutputListener!!.println("Stroke path")

                if (gs.strokeColorSpace.type == ColorSpace.e_pattern) {
                    mOutputListener!!.println("Path has associated pattern")
                } else {
                    // Get stroke color (you can use PDFNet color conversion facilities)
                    var rgb = ColorPt()
                    rgb = gs.strokeColor
                    var v = rgb.get(0)
                    rgb = gs.strokeColorSpace.convert2RGB(rgb)
                    v = rgb.get(0)
                }
            } else {
                // Do not stroke path
            }

            if (path.isFilled) {
                mOutputListener!!.println("Fill path")

                if (gs.fillColorSpace.type == ColorSpace.e_pattern) {
                    mOutputListener!!.println("Path has associated pattern")
                    val pat = gs.fillPattern
                    val type = pat.type
                    if (type == PatternColor.e_shading) {
                        mOutputListener!!.println("Shading")
                        val shading = pat.shading
                        if (shading.type == Shading.e_function_shading) {
                            mOutputListener!!.println("FUNCT")
                        } else if (shading.type == Shading.e_axial_shading) {
                            mOutputListener!!.println("AXIAL")
                        } else if (shading.type == Shading.e_radial_shading) {
                            mOutputListener!!.println("RADIAL")
                        }
                    } else if (type == PatternColor.e_colored_tiling_pattern) {
                        mOutputListener!!.println("e_colored_tiling_pattern")
                    } else if (type == PatternColor.e_uncolored_tiling_pattern) {
                        mOutputListener!!.println("e_uncolored_tiling_pattern")
                    } else {
                        mOutputListener!!.println("?")
                    }
                } else {
                    var rgb = ColorPt()
                    rgb = gs.fillColor
                    var v = rgb.get(0)
                    rgb = gs.fillColorSpace.convert2RGB(rgb)
                    v = rgb.get(0)
                }
            } else {
                // Do not fill path
            }

            // Process any changes in graphics state  ---------------------------------

            val gs_itr = reader.changesIterator
            while (gs_itr.hasNext()) {
                when (gs_itr.next()!!.toInt()) {
                    GState.e_transform -> {
                    }
                    GState.e_line_width -> {
                    }
                    GState.e_line_cap -> {
                    }
                    GState.e_line_join -> {
                    }
                    GState.e_flatness -> {
                    }
                    GState.e_miter_limit -> {
                    }
                    GState.e_dash_pattern -> {
                        //double[] dashes;
                        //dashes=gs.getDashes();
                        //gs.getPhase();
                    }
                    GState.e_fill_color -> {
                        if (gs.fillColorSpace.type == ColorSpace.e_pattern && gs.fillPattern.type != PatternColor.e_shading) {
                            //process the pattern data
                            reader.patternBegin(true)
                            ProcessElements(reader)
                            reader.end()
                        }
                    }
                }// Get transform matrix for this element. Unlike path.GetCTM()
                // that return full transformation matrix gs.GetTransform() return
                // only the transformation matrix that was installed for this element.
                //
                //gs.getTransform();
                //gs.getLineWidth();
                //gs.getLineCap();
                //gs.getLineJoin();
                //gs.getMiterLimit();
            }
            reader.clearChangeList()
        }

        @Throws(PDFNetException::class)
        internal fun ProcessText(page_reader: ElementReader) {
            // Begin text element
            mOutputListener!!.println("Begin Text Block:")

            var element: Element?
            while (true) {
                element = page_reader.next()
                if (element == null) {
                    break
                }
                when (element.type) {
                    Element.e_text_end -> {
                        // Finish the text block
                        mOutputListener!!.println("End Text Block.")
                        return
                    }

                    Element.e_text -> {
                        val gs = element.gState

                        val cs_fill = gs.fillColorSpace
                        val fill = gs.fillColor

                        val out: ColorPt
                        out = cs_fill.convert2RGB(fill)

                        val cs_stroke = gs.strokeColorSpace
                        val stroke = gs.strokeColor

                        val font = gs.font

                        mOutputListener!!.println("Font Name: " + font.name)
                        //font.isFixedWidth();
                        //font.isSerif();
                        //font.isSymbolic();
                        //font.isItalic();
                        // ...

                        //double font_size = gs.getFontSize();
                        //double word_spacing = gs.getWordSpacing();
                        //double char_spacing = gs.getCharSpacing();
                        //String txt = element.getTextString();

                        if (font.type == Font.e_Type3) {
                            //type 3 font, process its data
                            val itr = element.charIterator
                            while (itr.hasNext()) {
                                page_reader.type3FontBegin(itr.next(), null)
                                ProcessElements(page_reader)
                                page_reader.end()
                            }
                        } else {
                            val text_mtx = element.textMatrix
                            var x: Double
                            var y: Double
                            var char_code: Long

                            val itr = element.charIterator
                            while (itr.hasNext()) {
                                val data = itr.next()
                                char_code = data!!.charCode
                                //mOutputListener.print("Character code: ");

                                mOutputListener!!.print(char_code.toString())

                                x = data.getGlyphX()        // character positioning information
                                y = data.getGlyphY()

                                // Use element.getCTM() if you are interested in the CTM
                                // (current transformation matrix).
                                val ctm = element.ctm

                                // To get the exact character positioning information you need to
                                // concatenate current text matrix with CTM and then multiply
                                // relative positioning coordinates with the resulting matrix.
                                //
                                val mtx = ctm.multiply(text_mtx)
                                val t = mtx.multPoint(x, y)
                                x = t.x
                                y = t.y
                                //mOutputListener.println(" Position: x=" + x + " y=" + y );
                            }

                            mOutputListener!!.println()
                        }
                    }
                }
            }
        }

        @Throws(PDFNetException::class)
        internal fun ProcessImage(image: Element) {
            val image_mask = image.isImageMask
            val interpolate = image.isImageInterpolate
            val width = image.imageWidth
            val height = image.imageHeight
            val out_data_sz = width * height * 3

            mOutputListener!!.println("Image: " +
                    " width=\"" + width + "\""
                    + " height=\"" + height)

            // Matrix2D& mtx = image->GetCTM(); // image matrix (page positioning info)

            // You can use GetImageData to read the raw (decoded) image data
            //image->GetBitsPerComponent();
            //image->GetImageData();	// get raw image data
            // .... or use Image2RGB filter that converts every image to RGB format,
            // This should save you time since you don't need to deal with color conversions,
            // image up-sampling, decoding etc.

            val img_conv = Image2RGB(image)    // Extract and convert image to RGB 8-bpc format
            val reader = FilterReader(img_conv)

            // A buffer used to keep image data.
            val buf = ByteArray(out_data_sz)
            val image_data_out = reader.read(buf)
            // &image_data_out.front() contains RGB image data.

            // Note that you don't need to read a whole image at a time. Alternatively
            // you can read a chunk at a time by repeatedly calling reader.Read(buf)
            // until the function returns 0.
        }

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
                when (element.type) {
                    Element.e_path                        // Process path data...
                    -> {
                        ProcessPath(reader, element)
                    }
                    Element.e_text_begin                // Process text block...
                    -> {
                        ProcessText(reader)
                    }
                    Element.e_form                        // Process form XObjects
                    -> {
                        reader.formBegin()
                        ProcessElements(reader)
                        reader.end()
                    }
                    Element.e_image                        // Process Images
                    -> {
                        ProcessImage(element)
                    }
                }
            }
        }
    }

}