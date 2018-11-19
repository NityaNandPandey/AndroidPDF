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
import com.pdftron.pdf.ColorPt
import com.pdftron.pdf.ColorSpace
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementBuilder
import com.pdftron.pdf.ElementWriter
import com.pdftron.pdf.Font
import com.pdftron.pdf.GState
import com.pdftron.pdf.Image
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.PDFDocViewPrefs
import com.pdftron.pdf.PDFDraw
import com.pdftron.pdf.Page
import com.pdftron.pdf.ocg.Config
import com.pdftron.pdf.ocg.Context
import com.pdftron.pdf.ocg.Group
import com.pdftron.pdf.ocg.OCMD
import com.pdftron.sdf.Obj
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

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
class PDFLayersTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_pdflayers_title)
        setDescription(R.string.sample_pdflayers_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        try {
            val doc = PDFDoc()

            // Create three layers...
            val image_layer = createLayer(doc, "Image Layer")
            val text_layer = createLayer(doc, "Text Layer")
            val vector_layer = createLayer(doc, "Vector Layer")

            // Start a new page ------------------------------------
            val page = doc.pageCreate()

            val builder = ElementBuilder() // ElementBuilder is used to build new Element objects
            val writer = ElementWriter() // ElementWriter is used to write Elements to the page
            writer.begin(page)        // Begin writing to the page

            // Add new content to the page and associate it with one of the layers.
            var element = builder.createForm(createGroup1(doc, image_layer.sdfObj))
            writer.writeElement(element)

            element = builder.createForm(createGroup2(doc, vector_layer.sdfObj))
            writer.writeElement(element)

            // Add the text layer to the page...
            if (false)
            // set to true to enable 'ocmd' example.
            {
                // A bit more advanced example of how to create an OCMD text layer that
                // is visible only if text, image and path layers are all 'ON'.
                // An example of how to set 'Visibility Policy' in OCMD.
                val ocgs = doc.createIndirectArray()
                ocgs.pushBack(image_layer.sdfObj)
                ocgs.pushBack(vector_layer.sdfObj)
                ocgs.pushBack(text_layer.sdfObj)
                val text_ocmd = OCMD.create(doc, ocgs, OCMD.e_AllOn)
                element = builder.createForm(createGroup3(doc, text_ocmd.sdfObj))
            } else {
                element = builder.createForm(createGroup3(doc, text_layer.sdfObj))
            }
            writer.writeElement(element)

            // Add some content to the page that does not belong to any layer...
            // In this case this is a rectangle representing the page border.
            element = builder.createRect(0.0, 0.0, page.pageWidth, page.pageHeight)
            element.setPathFill(false)
            element.setPathStroke(true)
            element.gState.lineWidth = 40.0
            writer.writeElement(element)

            writer.end()  // save changes to the current page
            doc.pagePushBack(page)

            // Set the default viewing preference to display 'Layer' tab.
            val prefs = doc.viewPrefs
            prefs.pageMode = PDFDocViewPrefs.e_UseOC

            doc.save(Utils.createExternalFile("pdf_layers.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // The following is a code snippet shows how to selectively render
        // and export PDF layers.
        try {
            val doc = PDFDoc(Utils.createExternalFile("pdf_layers.pdf").absolutePath)
            doc.initSecurityHandler()

            if (doc.hasOC() == false) {
                mOutputListener!!.println("The document does not contain 'Optional Content'")
            } else {
                val init_cfg = doc.ocgConfig
                val ctx = Context(init_cfg)

                val pdfdraw = PDFDraw()
                pdfdraw.setImageSize(1000, 1000)
                pdfdraw.setOCGContext(ctx) // Render the page using the given OCG context.

                val page = doc.getPage(1) // Get the first page in the document.
                pdfdraw.export(page, Utils.createExternalFile("pdf_layers_default.png").absolutePath)
                mFileList.add("pdf_layers_default.png")

                // Disable drawing of content that is not optional (i.e. is not part of any layer).
                ctx.setNonOCDrawing(false)

                // Now render each layer in the input document to a separate image.
                val ocgs = doc.ocGs // Get the array of all OCGs in the document.
                if (ocgs != null) {
                    var i: Int
                    val sz = ocgs.size().toInt()
                    i = 0
                    while (i < sz) {
                        val ocg = Group(ocgs.getAt(i))
                        ctx.resetStates(false)
                        ctx.setState(ocg, true)
                        val fname = Utils.createExternalFile("pdf_layers_").absolutePath + ocg.name + ".png"
                        mOutputListener!!.println(fname)
                        pdfdraw.export(page, fname)
                        mFileList.add("pdf_layers_" + ocg.name + ".png")
                        ++i
                    }
                }

                // Now draw content that is not part of any layer...
                ctx.setNonOCDrawing(true)
                ctx.setOCDrawMode(Context.e_NoOC)
                pdfdraw.export(page, Utils.createExternalFile("pdf_layers_non_oc.png").absolutePath)
                mFileList.add("pdf_layers_non_oc.png")
            }

            doc.close()
            mOutputListener!!.println("Done.")
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

        // A utility function used to add new Content Groups (Layers) to the document.
        @Throws(PDFNetException::class)
        internal fun createLayer(doc: PDFDoc, layer_name: String): Group {
            val grp = Group.create(doc, layer_name)
            var cfg: Config? = doc.ocgConfig
            if (cfg == null) {
                cfg = Config.create(doc, true)
                cfg!!.name = "Default"
            }

            // Add the new OCG to the list of layers that should appear in PDF viewer GUI.
            var layer_order_array: Obj? = cfg.order
            if (layer_order_array == null) {
                layer_order_array = doc.createIndirectArray()
                cfg.order = layer_order_array!!
            }
            layer_order_array.pushBack(grp.sdfObj)

            return grp
        }

        // Creates some content (3 images) and associate them with the image layer
        @Throws(PDFNetException::class)
        internal fun createGroup1(doc: PDFDoc, layer: Obj): Obj {
            val writer = ElementWriter()
            writer.begin(doc)

            // Create an Image that can be reused in the document or on the same page.
            val img = Image.create(doc.sdfDoc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "peppers.jpg")!!.absolutePath)

            val builder = ElementBuilder()
            val element = builder.createImage(img, Matrix2D((img.imageWidth / 2).toDouble(), -145.0, 20.0, (img.imageHeight / 2).toDouble(), 200.0, 150.0))
            writer.writePlacedElement(element)

            val gstate = element.gState    // use the same image (just change its matrix)
            gstate.setTransform(200.0, 0.0, 0.0, 300.0, 50.0, 450.0)
            writer.writePlacedElement(element)

            // use the same image again (just change its matrix).
            writer.writePlacedElement(builder.createImage(img, 300.0, 600.0, 200.0, -150.0))

            val grp_obj = writer.end()

            // Indicate that this form (content group) belongs to the given layer (OCG).
            grp_obj.putName("Subtype", "Form")
            grp_obj.put("OC", layer)
            grp_obj.putRect("BBox", 0.0, 0.0, 1000.0, 1000.0)  // Set the clip box for the content.

            return grp_obj
        }

        // Creates some content (a path in the shape of a heart) and associate it with the vector layer
        @Throws(PDFNetException::class)
        internal fun createGroup2(doc: PDFDoc, layer: Obj): Obj {
            val writer = ElementWriter()
            writer.begin(doc)

            // Create a path object in the shape of a heart.
            val builder = ElementBuilder()
            builder.pathBegin()        // start constructing the path
            builder.moveTo(306.0, 396.0)
            builder.curveTo(681.0, 771.0, 399.75, 864.75, 306.0, 771.0)
            builder.curveTo(212.25, 864.75, -69.0, 771.0, 306.0, 396.0)
            builder.closePath()
            val element = builder.pathEnd() // the path geometry is now specified.

            // Set the path FILL color space and color.
            element.setPathFill(true)
            val gstate = element.gState
            gstate.fillColorSpace = ColorSpace.createDeviceCMYK()
            gstate.fillColor = ColorPt(1.0, 0.0, 0.0, 0.0)  // cyan

            // Set the path STROKE color space and color.
            element.setPathStroke(true)
            gstate.strokeColorSpace = ColorSpace.createDeviceRGB()
            gstate.strokeColor = ColorPt(1.0, 0.0, 0.0)  // red
            gstate.lineWidth = 20.0

            gstate.setTransform(0.5, 0.0, 0.0, 0.5, 280.0, 300.0)

            writer.writeElement(element)

            val grp_obj = writer.end()

            // Indicate that this form (content group) belongs to the given layer (OCG).
            grp_obj.putName("Subtype", "Form")
            grp_obj.put("OC", layer)
            grp_obj.putRect("BBox", 0.0, 0.0, 1000.0, 1000.0)    // Set the clip box for the content.

            return grp_obj
        }

        // Creates some text and associate it with the text layer
        @Throws(PDFNetException::class)
        internal fun createGroup3(doc: PDFDoc, layer: Obj): Obj {
            val writer = ElementWriter()
            writer.begin(doc)

            // Create a path object in the shape of a heart.
            val builder = ElementBuilder()

            // Begin writing a block of text
            var element = builder.createTextBegin(Font.create(doc, Font.e_times_roman), 120.0)
            writer.writeElement(element)

            element = builder.createTextRun("A text layer!")

            // Rotate text 45 degrees, than translate 180 pts horizontally and 100 pts vertically.
            val transform = Matrix2D.rotationMatrix(-45 * (3.1415 / 180.0))
            transform.concat(1.0, 0.0, 0.0, 1.0, 180.0, 100.0)
            element.textMatrix = transform

            writer.writeElement(element)
            writer.writeElement(builder.createTextEnd())

            val grp_obj = writer.end()

            // Indicate that this form (content group) belongs to the given layer (OCG).
            grp_obj.putName("Subtype", "Form")
            grp_obj.put("OC", layer)
            grp_obj.putRect("BBox", 0.0, 0.0, 1000.0, 1000.0)    // Set the clip box for the content.

            return grp_obj
        }
    }

}