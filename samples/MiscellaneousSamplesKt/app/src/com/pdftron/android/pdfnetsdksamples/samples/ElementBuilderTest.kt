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
import com.pdftron.pdf.ColorPt
import com.pdftron.pdf.ColorSpace
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementBuilder
import com.pdftron.pdf.ElementReader
import com.pdftron.pdf.ElementWriter
import com.pdftron.pdf.Font
import com.pdftron.pdf.GState
import com.pdftron.pdf.Image
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.Rect
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.ArrayList

class ElementBuilderTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_elementbuilder_title)
        setDescription(R.string.sample_elementbuilder_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        try {
            val doc = PDFDoc()
            val eb = ElementBuilder()        // ElementBuilder is used to build new
            // Element objects
            val writer = ElementWriter()    // ElementWriter is used to write
            // Elements to the page

            var element: Element?
            var gstate: GState

            // Start a new page ------------------------------------
            var page = doc.pageCreate(Rect(0.0, 0.0, 612.0, 794.0))

            writer.begin(page)    // begin writing to the page

            // Create an Image that can be reused in the document or on the
            // same page.
            val img = Image.create(doc.sdfDoc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "peppers.jpg")!!.absolutePath)

            element = eb.createImage(img, Matrix2D((img.imageWidth / 2).toDouble(), -145.0, 20.0, (img.imageHeight / 2).toDouble(), 200.0, 150.0))
            writer.writePlacedElement(element)

            gstate = element.gState    // use the same image (just
            // change its matrix)
            gstate.setTransform(200.0, 0.0, 0.0, 300.0, 50.0, 450.0)
            writer.writePlacedElement(element)

            // use the same image again (just change its matrix).
            writer.writePlacedElement(eb.createImage(img, 300.0, 600.0, 200.0, -150.0))

            writer.end()  // save changes to the current page
            doc.pagePushBack(page)

            // Start a new page ------------------------------------
            // Construct and draw a path object using different styles
            page = doc.pageCreate(Rect(0.0, 0.0, 612.0, 794.0))

            writer.begin(page)    // begin writing to this page
            eb.reset()            // Reset the GState to default

            eb.pathBegin()        // start constructing the path
            eb.moveTo(306.0, 396.0)
            eb.curveTo(681.0, 771.0, 399.75, 864.75, 306.0, 771.0)
            eb.curveTo(212.25, 864.75, -69.0, 771.0, 306.0, 396.0)
            eb.closePath()
            element = eb.pathEnd()            // the path is now finished
            element.setPathFill(true)        // the path should be filled

            // Set the path color space and color
            gstate = element.gState
            gstate.fillColorSpace = ColorSpace.createDeviceCMYK()
            gstate.fillColor = ColorPt(1.0, 0.0, 0.0, 0.0)  // cyan
            gstate.setTransform(0.5, 0.0, 0.0, 0.5, -20.0, 300.0)
            writer.writePlacedElement(element)

            // Draw the same path using a different stroke color
            element.setPathStroke(true)        // this path is should be
            // filled and stroked
            gstate.fillColor = ColorPt(0.0, 0.0, 1.0, 0.0)  // yellow
            gstate.strokeColorSpace = ColorSpace.createDeviceRGB()
            gstate.strokeColor = ColorPt(1.0, 0.0, 0.0)  // red
            gstate.setTransform(0.5, 0.0, 0.0, 0.5, 280.0, 300.0)
            gstate.lineWidth = 20.0
            writer.writePlacedElement(element)

            // Draw the same path with with a given dash pattern
            element.setPathFill(false)    // this path is should be only
            // stroked
            gstate.strokeColor = ColorPt(0.0, 0.0, 1.0)  // blue
            gstate.setTransform(0.5, 0.0, 0.0, 0.5, 280.0, 0.0)
            val dash_pattern = doubleArrayOf(30.0)
            gstate.setDashPattern(dash_pattern, 0.0)
            writer.writePlacedElement(element)

            // Use the path as a clipping path
            writer.writeElement(eb.createGroupBegin())    // Save the graphics
            // state
            // Start constructing the new path (the old path was lost when
            // we created
            // a new Element using CreateGroupBegin()).
            eb.pathBegin()
            eb.moveTo(306.0, 396.0)
            eb.curveTo(681.0, 771.0, 399.75, 864.75, 306.0, 771.0)
            eb.curveTo(212.25, 864.75, -69.0, 771.0, 306.0, 396.0)
            eb.closePath()
            element = eb.pathEnd()    // path is now constructed
            element.setPathClip(true)    // this path is a clipping path
            element.setPathStroke(true)        // this path should be
            // filled and stroked
            gstate = element.gState
            gstate.setTransform(0.5, 0.0, 0.0, 0.5, -20.0, 0.0)

            writer.writeElement(element)

            writer.writeElement(eb.createImage(img, 100.0, 300.0, 400.0, 600.0))

            writer.writeElement(eb.createGroupEnd())    // Restore the
            // graphics state

            writer.end()  // save changes to the current page
            doc.pagePushBack(page)

            // Start a new page ------------------------------------
            page = doc.pageCreate(Rect(0.0, 0.0, 612.0, 794.0))

            writer.begin(page)    // begin writing to this page
            eb.reset()            // Reset the GState to default

            // Begin writing a block of text
            element = eb.createTextBegin(Font.create(doc, Font.e_times_roman), 12.0)
            writer.writeElement(element)

            element = eb.createTextRun("Hello World!")
            element.setTextMatrix(10.0, 0.0, 0.0, 10.0, 0.0, 600.0)
            element.gState.leading = 15.0         // Set the spacing
            // between lines
            writer.writeElement(element)

            writer.writeElement(eb.createTextNewLine())  // New line

            element = eb.createTextRun("Hello World!")
            gstate = element.gState
            gstate.textRenderMode = GState.e_stroke_text
            gstate.charSpacing = -1.25
            gstate.wordSpacing = -1.25
            writer.writeElement(element)

            writer.writeElement(eb.createTextNewLine())  // New line

            element = eb.createTextRun("Hello World!")
            gstate = element.gState
            gstate.charSpacing = 0.0
            gstate.wordSpacing = 0.0
            gstate.lineWidth = 3.0
            gstate.textRenderMode = GState.e_fill_stroke_text
            gstate.strokeColorSpace = ColorSpace.createDeviceRGB()
            gstate.strokeColor = ColorPt(1.0, 0.0, 0.0)    // red
            gstate.fillColorSpace = ColorSpace.createDeviceCMYK()
            gstate.fillColor = ColorPt(1.0, 0.0, 0.0, 0.0)    // cyan
            writer.writeElement(element)

            writer.writeElement(eb.createTextNewLine())  // New line

            // Set text as a clipping path to the image.
            element = eb.createTextRun("Hello World!")
            gstate = element.gState
            gstate.textRenderMode = GState.e_clip_text
            writer.writeElement(element)

            // Finish the block of text
            writer.writeElement(eb.createTextEnd())

            // Draw an image that will be clipped by the above text
            writer.writeElement(eb.createImage(img, 10.0, 100.0, 1300.0, 720.0))

            writer.end()  // save changes to the current page
            doc.pagePushBack(page)

            // Start a new page ------------------------------------
            //
            // The example illustrates how to embed the external font in a
            // PDF document.
            // The example also shows how ElementReader can be used to copy
            // and modify
            // Elements between pages.

            val reader = ElementReader()

            // Start reading Elements from the last page. We will copy all
            // Elements to
            // a new page but will modify the font associated with text.
            reader.begin(doc.getPage(doc.pageCount))

            page = doc.pageCreate(Rect(0.0, 0.0, 1300.0, 794.0))

            writer.begin(page)    // begin writing to this page
            eb.reset()        // Reset the GState to default

            // Embed an external font in the document.
            val font: Font
            val file = File(PDFNetSample.Companion.INPUT_PATH, "font.ttf")
            val stream = FileInputStream(file)
            font = Font.createTrueTypeFont(doc, stream)
            //Alternatively, the font can be created from the file path.
            //font = Font.createTrueTypeFont(doc, (Utils.getAssetTempFile(INPUT_PATH + "font.ttf").getAbsolutePath()));

            // Read page
            // contents
            while(true)
            {
                element = reader.next()
                if (element == null) {
                    break
                }
                if (element.type == Element.e_text) {
                    element.gState.setFont(font, 12.0)
                }

                writer.writeElement(element)
            }

            reader.end()
            writer.end()  // save changes to the current page

            doc.pagePushBack(page)

            // Start a new page ------------------------------------
            //
            // The example illustrates how to embed the external font in a
            // PDF document.
            // The example also shows how ElementReader can be used to copy
            // and modify
            // Elements between pages.

            // Start reading Elements from the last page. We will copy all
            // Elements to
            // a new page but will modify the font associated with text.
            reader.begin(doc.getPage(doc.pageCount))

            page = doc.pageCreate(Rect(0.0, 0.0, 1300.0, 794.0))

            writer.begin(page)    // begin writing to this page
            eb.reset()        // Reset the GState to default

            // Embed an external font in the document.
            val font2 = Font.createType1Font(doc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "Misc-Fixed.pfa")!!.absolutePath)

            while (true)
            // Read page contents
            {
                element = reader.next()
                if (element == null) {
                    break
                }
                if (element.type == Element.e_text) {
                    element.gState.setFont(font2, 12.0)
                }

                writer.writeElement(element)
            }

            reader.end()
            writer.end()  // save changes to the current page
            doc.pagePushBack(page)

            // Start a new page ------------------------------------
            page = doc.pageCreate()
            writer.begin(page)    // begin writing to this page
            eb.reset()            // Reset the GState to default

            // Begin writing a block of text
            element = eb.createTextBegin(Font.create(doc, Font.e_times_roman), 12.0)
            element.setTextMatrix(1.5, 0.0, 0.0, 1.5, 50.0, 600.0)
            element.gState.leading = 15.0    // Set the spacing between
            // lines
            writer.writeElement(element)

            val para = "A PDF text object consists of operators that can show " +
                    "text strings, move the text position, and set text state and certain " +
                    "other parameters. In addition, there are three parameters that are " +
                    "defined only within a text object and do not persist from one text " +
                    "object to the next: Tm, the text matrix, Tlm, the text line matrix, " +
                    "Trm, the text rendering matrix, actually just an intermediate result " +
                    "that combines the effects of text state parameters, the text matrix " +
                    "(Tm), and the current transformation matrix"

            val para_end = para.length
            var text_run = 0
            var text_run_end: Int

            val para_width = 300.0 // paragraph width is 300 units
            var cur_width = 0.0

            while (text_run < para_end) {
                text_run_end = para.indexOf(' ', text_run)
                if (text_run_end < 0)
                    text_run_end = para_end - 1

                var text = para.substring(text_run, text_run_end + 1)
                element = eb.createTextRun(text)
                if (cur_width + element.textLength < para_width) {
                    writer.writeElement(element)
                    cur_width += element.textLength
                } else {
                    writer.writeElement(eb.createTextNewLine())  // New
                    text = para.substring(text_run, text_run_end + 1)                                            // line
                    element = eb.createTextRun(text)
                    cur_width = element.textLength
                    writer.writeElement(element)
                }

                text_run = text_run_end + 1
            }

            // -----------------------------------------------------------------------
            // The following code snippet illustrates how to adjust
            // spacing between
            // characters (text runs).
            element = eb.createTextNewLine()
            writer.writeElement(element)  // Skip 2 lines
            writer.writeElement(element)

            writer.writeElement(eb.createTextRun("An example of space adjustments between inter-characters:"))
            writer.writeElement(eb.createTextNewLine())

            // Write string "AWAY" without space adjustments between
            // characters.
            element = eb.createTextRun("AWAY")
            writer.writeElement(element)

            writer.writeElement(eb.createTextNewLine())

            // Write string "AWAY" with space adjustments between
            // characters.
            element = eb.createTextRun("A")
            writer.writeElement(element)

            element = eb.createTextRun("W")
            element.posAdjustment = 140.0
            writer.writeElement(element)

            element = eb.createTextRun("A")
            element.posAdjustment = 140.0
            writer.writeElement(element)

            element = eb.createTextRun("Y again")
            element.posAdjustment = 115.0
            writer.writeElement(element)

            // Draw the same strings using direct content output...
            writer.flush()  // flush pending Element writing operations.

            // You can also write page content directly to the content
            // stream using
            // ElementWriter.WriteString(...) and
            // ElementWriter.WriteBuffer(...) methods.
            // Note that if you are planning to use these functions you need
            // to be familiar
            // with PDF page content operators (see Appendix A in PDF
            // Reference Manual).
            // Because it is easy to make mistakes during direct output we
            // recommend that
            // you use ElementBuilder and Element interface instead.

            writer.writeString("T* T* ") // Skip 2 lines
            writer.writeString("(Direct output to PDF page content stream:) Tj  T* ")
            writer.writeString("(AWAY) Tj T* ")
            writer.writeString("[(A)140(W)140(A)115(Y again)] TJ ")

            // Finish the block of text
            writer.writeElement(eb.createTextEnd())

            writer.end()  // save changes to the current page
            doc.pagePushBack(page)

            // Start a new page ------------------------------------

            // Image Masks
            //
            // In the opaque imaging model, images mark all areas they
            // occupy on the page as
            // if with opaque paint. All portions of the image, whether
            // black, white, gray,
            // or color, completely obscure any marks that may previously
            // have existed in the
            // same place on the page.
            // In the graphic arts industry and page layout applications,
            // however, it is common
            // to crop or 'mask out' the background of an image and then
            // place the masked image
            // on a different background, allowing the existing background
            // to show through the
            // masked areas. This sample illustrates how to use image masks.

            page = doc.pageCreate()
            writer.begin(page)    // begin writing to the page

            /*// Create the Image Mask
            MappedFile imgf=new MappedFile(Utils.getAssetTempFile(INPUT_PATH + "imagemask.dat").getAbsolutePath());
			FilterReader mask_read=new FilterReader(imgf);

			ColorSpace device_gray = ColorSpace.createDeviceGray();
			Image mask = Image.create(doc, mask_read, 64, 64, 1, device_gray, Image.e_ascii_hex);

			mask.getSDFObj().putBool("ImageMask", true);

			element = eb.createRect(0, 0, 612, 794);
			element.setPathStroke(false);
			element.setPathFill(true);
			element.getGState().setFillColorSpace(device_gray);
			element.getGState().setFillColor(new ColorPt(0.8));
			writer.writePlacedElement(element);

			element = eb.createImage(mask, new Matrix2D(200, 0, 0, -200, 40, 680));
			element.getGState().setFillColor(new ColorPt(0.1));
			writer.writePlacedElement(element);

			element.getGState().setFillColorSpace(ColorSpace.createDeviceRGB());
			element.getGState().setFillColor(new ColorPt(1, 0, 0));
			element = eb.createImage(mask, new Matrix2D(200, 0, 0, -200, 320, 680));
			writer.writePlacedElement(element);

			element.getGState().setFillColor(new ColorPt(0, 1, 0));
			element = eb.createImage(mask, new Matrix2D(200, 0, 0, -200, 40, 380));
			writer.writePlacedElement(element);

			{
				// This sample illustrates Explicit Masking.
				img = Image.create(doc, (Utils.getAssetTempFile(INPUT_PATH + "peppers.jpg").getAbsolutePath()));

				// mask is the explicit mask for the primary (base) image
				img.setMask(mask);

				element = eb.createImage(img, new Matrix2D(200, 0, 0, -200, 320, 380));
				writer.writePlacedElement(element);
			}

			writer.end();  // save changes to the current page
			doc.pagePushBack(page);*/

            // Transparency sample ----------------------------------

            // Start a new page -------------------------------------
            page = doc.pageCreate()
            writer.begin(page)    // begin writing to this page
            eb.reset()            // Reset the GState to default

            // Write some transparent text at the bottom of the page.
            element = eb.createTextBegin(Font.create(doc, Font.e_times_roman), 100.0)

            // Set the text knockout attribute. Text knockout must be set
            // outside of
            // the text group.
            gstate = element.gState
            gstate.isTextKnockout = false
            gstate.blendMode = GState.e_bl_difference
            writer.writeElement(element)

            element = eb.createTextRun("Transparency")
            element.setTextMatrix(1.0, 0.0, 0.0, 1.0, 30.0, 30.0)
            gstate = element.gState
            gstate.fillColorSpace = ColorSpace.createDeviceCMYK()
            gstate.fillColor = ColorPt(1.0, 0.0, 0.0, 0.0)

            gstate.fillOpacity = 0.5
            writer.writeElement(element)

            // Write the same text on top the old; shifted by 3 points
            element.setTextMatrix(1.0, 0.0, 0.0, 1.0, 33.0, 33.0)
            gstate.fillColor = ColorPt(0.0, 1.0, 0.0, 0.0)
            gstate.fillOpacity = 0.5

            writer.writeElement(element)
            writer.writeElement(eb.createTextEnd())

            // Draw three overlapping transparent circles.
            eb.pathBegin()        // start constructing the path
            eb.moveTo(459.223, 505.646)
            eb.curveTo(459.223, 415.841, 389.85, 343.04, 304.273, 343.04)
            eb.curveTo(218.697, 343.04, 149.324, 415.841, 149.324, 505.646)
            eb.curveTo(149.324, 595.45, 218.697, 668.25, 304.273, 668.25)
            eb.curveTo(389.85, 668.25, 459.223, 595.45, 459.223, 505.646)
            element = eb.pathEnd()
            element.setPathFill(true)

            gstate = element.gState
            gstate.fillColorSpace = ColorSpace.createDeviceRGB()
            gstate.fillColor = ColorPt(0.0, 0.0, 1.0)                     // Blue
            // Circle

            gstate.blendMode = GState.e_bl_normal
            gstate.fillOpacity = 0.5
            writer.writeElement(element)

            // Translate relative to the Blue Circle
            gstate.setTransform(1.0, 0.0, 0.0, 1.0, 113.0, -185.0)
            gstate.fillColor = ColorPt(0.0, 1.0, 0.0)                     // Green
            // Circle
            gstate.fillOpacity = 0.5
            writer.writeElement(element)

            // Translate relative to the Green Circle
            gstate.setTransform(1.0, 0.0, 0.0, 1.0, -220.0, 0.0)
            gstate.fillColor = ColorPt(1.0, 0.0, 0.0)                     // Red
            // Circle
            gstate.fillOpacity = 0.5
            writer.writeElement(element)

            writer.end()  // save changes to the current page
            doc.pagePushBack(page)

            // End page ------------------------------------

            doc.save(Utils.createExternalFile("element_builder.pdf").absolutePath, SDFDoc.SaveMode.REMOVE_UNUSED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done. Result saved in element_builder.pdf...")
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
    }


}