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
import com.pdftron.pdf.Page
import com.pdftron.pdf.PatternColor
import com.pdftron.sdf.Obj
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

class PatternTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_pattern_title)
        setDescription(R.string.sample_pattern_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        try {
            val doc = PDFDoc()
            val writer = ElementWriter()
            val eb = ElementBuilder()

            // The following sample illustrates how to create and use tiling patterns
            var page = doc.pageCreate()
            writer.begin(page)

            var element = eb.createTextBegin(Font.create(doc, Font.e_times_bold), 1.0)
            writer.writeElement(element)  // Begin the text block

            val data = "G"
            element = eb.createTextRun(data)
            element.setTextMatrix(720.0, 0.0, 0.0, 720.0, 20.0, 240.0)
            var gs = element.gState
            gs.textRenderMode = GState.e_fill_stroke_text
            gs.lineWidth = 4.0

            // Set the fill color space to the Pattern color space.
            gs.fillColorSpace = ColorSpace.createPattern()
            gs.setFillColor(PatternColor(CreateTilingPattern(doc)))

            writer.writeElement(element)
            writer.writeElement(eb.createTextEnd()) // Finish the text block

            writer.end()    // Save the page
            doc.pagePushBack(page)
            //-----------------------------------------------

            /// The following sample illustrates how to create and use image tiling pattern
            page = doc.pageCreate()
            writer.begin(page)

            eb.reset()
            element = eb.createRect(0.0, 0.0, 612.0, 794.0)

            // Set the fill color space to the Pattern color space.
            gs = element.gState
            gs.fillColorSpace = ColorSpace.createPattern()
            gs.setFillColor(PatternColor(CreateImageTilingPattern(doc)))
            element.setPathFill(true)

            writer.writeElement(element)

            writer.end()    // Save the page
            doc.pagePushBack(page)
            //-----------------------------------------------

            /// The following sample illustrates how to create and use PDF shadings
            page = doc.pageCreate()
            writer.begin(page)

            eb.reset()
            element = eb.createRect(0.0, 0.0, 612.0, 794.0)

            // Set the fill color space to the Pattern color space.
            gs = element.gState
            gs.fillColorSpace = ColorSpace.createPattern()

            gs.setFillColor(PatternColor(CreateAxialShading(doc)))
            element.setPathFill(true)

            writer.writeElement(element)

            writer.end()    // save the page
            doc.pagePushBack(page)
            //-----------------------------------------------

            doc.save(Utils.createExternalFile("patterns.pdf").absolutePath, SDFDoc.SaveMode.REMOVE_UNUSED, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done. Result saved in patterns.pdf...")
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


        @Throws(PDFNetException::class)
        internal fun CreateTilingPattern(doc: PDFDoc): Obj {
            val writer = ElementWriter()
            val eb = ElementBuilder()

            // Create a new pattern content stream - a heart. ------------
            writer.begin(doc)
            eb.pathBegin()
            eb.moveTo(0.0, 0.0)
            eb.curveTo(500.0, 500.0, 125.0, 625.0, 0.0, 500.0)
            eb.curveTo(-125.0, 625.0, -500.0, 500.0, 0.0, 0.0)
            val heart = eb.pathEnd()
            heart.setPathFill(true)

            // Set heart color to red.
            heart.gState.fillColorSpace = ColorSpace.createDeviceRGB()
            heart.gState.fillColor = ColorPt(1.0, 0.0, 0.0)
            writer.writeElement(heart)

            val pattern_dict = writer.end()

            // Initialize pattern dictionary. For details on what each parameter represents please
            // refer to Table 4.22 (Section '4.6.2 Tiling Patterns') in PDF Reference Manual.
            pattern_dict.putName("Type", "Pattern")
            pattern_dict.putNumber("PatternType", 1.0)

            // TilingType - Constant spacing.
            pattern_dict.putNumber("TilingType", 1.0)

            // This is a Type1 pattern - A colored tiling pattern.
            pattern_dict.putNumber("PaintType", 1.0)

            // Set bounding box
            pattern_dict.putRect("BBox", -253.0, 0.0, 253.0, 545.0)

            // Create and set the matrix
            val pattern_mtx = Matrix2D(0.04, 0.0, 0.0, 0.04, 0.0, 0.0)
            pattern_dict.putMatrix("Matrix", pattern_mtx)

            // Set the desired horizontal and vertical spacing between pattern cells,
            // measured in the pattern coordinate system.
            pattern_dict.putNumber("XStep", 1000.0)
            pattern_dict.putNumber("YStep", 1000.0)

            return pattern_dict // finished creating the Pattern resource
        }

        @Throws(PDFNetException::class)
        internal fun CreateImageTilingPattern(doc: PDFDoc): Obj {
            val writer = ElementWriter()
            val eb = ElementBuilder()

            // Create a new pattern content stream - a single bitmap object ----------
            writer.begin(doc)
            val image = Image.create(doc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "dice.jpg")!!.absolutePath)
            val img_element = eb.createImage(image, 0.0, 0.0, image.imageWidth.toDouble(), image.imageHeight.toDouble())
            writer.writePlacedElement(img_element)
            val pattern_dict = writer.end()

            // Initialize pattern dictionary. For details on what each parameter represents please
            // refer to Table 4.22 (Section '4.6.2 Tiling Patterns') in PDF Reference Manual.
            pattern_dict.putName("Type", "Pattern")
            pattern_dict.putNumber("PatternType", 1.0)

            // TilingType - Constant spacing.
            pattern_dict.putNumber("TilingType", 1.0)

            // This is a Type1 pattern - A colored tiling pattern.
            pattern_dict.putNumber("PaintType", 1.0)

            // Set bounding box
            pattern_dict.putRect("BBox", -253.0, 0.0, 253.0, 545.0)

            // Create and set the matrix
            val pattern_mtx = Matrix2D(0.3, 0.0, 0.0, 0.3, 0.0, 0.0)
            pattern_dict.putMatrix("Matrix", pattern_mtx)

            // Set the desired horizontal and vertical spacing between pattern cells,
            // measured in the pattern coordinate system.
            pattern_dict.putNumber("XStep", 300.0)
            pattern_dict.putNumber("YStep", 300.0)

            return pattern_dict // finished creating the Pattern resource
        }

        @Throws(PDFNetException::class)
        internal fun CreateAxialShading(doc: PDFDoc): Obj {
            // Create a new Shading object ------------
            val pattern_dict = doc.createIndirectDict()

            // Initialize pattern dictionary. For details on what each parameter represents
            // please refer to Tables 4.30 and 4.26 in PDF Reference Manual
            pattern_dict.putName("Type", "Pattern")
            pattern_dict.putNumber("PatternType", 2.0) // 2 stands for shading

            val shadingDict = pattern_dict.putDict("Shading")
            shadingDict.putNumber("ShadingType", 2.0)
            shadingDict.putName("ColorSpace", "DeviceCMYK")

            // pass the coordinates of the axial shading to the output
            val shadingCoords = shadingDict.putArray("Coords")
            shadingCoords.pushBackNumber(0.0)
            shadingCoords.pushBackNumber(0.0)
            shadingCoords.pushBackNumber(612.0)
            shadingCoords.pushBackNumber(794.0)

            // pass the function to the axial shading
            val function = shadingDict.putDict("Function")
            val C0 = function.putArray("C0")
            C0.pushBackNumber(1.0)
            C0.pushBackNumber(0.0)
            C0.pushBackNumber(0.0)
            C0.pushBackNumber(0.0)

            val C1 = function.putArray("C1")
            C1.pushBackNumber(0.0)
            C1.pushBackNumber(1.0)
            C1.pushBackNumber(0.0)
            C1.pushBackNumber(0.0)

            val domain = function.putArray("Domain")
            domain.pushBackNumber(0.0)
            domain.pushBackNumber(1.0)

            function.putNumber("FunctionType", 2.0)
            function.putNumber("N", 1.0)

            return pattern_dict
        }
    }


}