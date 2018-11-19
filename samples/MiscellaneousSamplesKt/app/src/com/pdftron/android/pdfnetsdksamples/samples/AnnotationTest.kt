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
import com.pdftron.pdf.Action
import com.pdftron.pdf.Annot
import com.pdftron.pdf.Annot.BorderStyle
import com.pdftron.pdf.ColorPt
import com.pdftron.pdf.Destination
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementBuilder
import com.pdftron.pdf.ElementWriter
import com.pdftron.pdf.FileSpec
import com.pdftron.pdf.Font
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.PageIterator
import com.pdftron.pdf.Point
import com.pdftron.pdf.QuadPoint
import com.pdftron.pdf.Rect
import com.pdftron.pdf.annots.Caret
import com.pdftron.pdf.annots.Circle
import com.pdftron.pdf.annots.FreeText
import com.pdftron.pdf.annots.Highlight
import com.pdftron.pdf.annots.Ink
import com.pdftron.pdf.annots.Line
import com.pdftron.pdf.annots.Link
import com.pdftron.pdf.annots.PolyLine
import com.pdftron.pdf.annots.Polygon
import com.pdftron.pdf.annots.RubberStamp
import com.pdftron.pdf.annots.Sound
import com.pdftron.pdf.annots.Square
import com.pdftron.pdf.annots.Squiggly
import com.pdftron.pdf.annots.Text
import com.pdftron.sdf.Obj
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.text.DecimalFormat
import java.util.ArrayList

class AnnotationTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_annotation_title)
        setDescription(R.string.sample_annotation_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        try {
            mOutputListener!!.println("-------------------------------------------------")
            mOutputListener!!.println("Opening the input file...")

            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "numbered.pdf")!!.absolutePath)
            doc.initSecurityHandler()

            // An example of using SDF/Cos API to add any type of annotations.
            AnnotationLowLevelAPI(doc)
            doc.save(Utils.createExternalFile("annotation_test1.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            mOutputListener!!.println("Done. Results saved in annotation_test1.pdf")

            // An example of using the high-level PDFNet API to read existing annotations,
            // to edit existing annotations, and to create new annotation from scratch.
            AnnotationHighLevelAPI(doc)
            doc.save(Utils.createExternalFile("annotation_test2.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc.fileName).name)
            mOutputListener!!.println("Done. Results saved in annotation_test2.pdf")

            // an example of creating various annotations in a brand new document
            val doc1 = PDFDoc()
            CreateTestAnnots(doc1)
            doc1.save(Utils.createExternalFile("new_annot_test_api.pdf").absolutePath, SDFDoc.SaveMode.LINEARIZED, null)
            mFileList.add(File(doc1.fileName).name)
            mOutputListener!!.println("Saved new_annot_test_api.pdf")
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

        val format = DecimalFormat("0.#")

        @Throws(PDFNetException::class)
        internal fun AnnotationHighLevelAPI(doc: PDFDoc) {
            // The following code snippet traverses all annotations in the document
            mOutputListener!!.println("Traversing all annotations in the document...")

            var page_num = 1
            val itr = doc.pageIterator
            while (itr.hasNext()) {
                mOutputListener!!.println("Page " + page_num++ + ": ")

                val page = itr.next()
                val num_annots = page!!.numAnnots
                for (i in 0 until num_annots) {
                    val annot = page.getAnnot(i)
                    if (annot?.isValid == false) continue
                    mOutputListener!!.println("Annot Type: " + annot!!.sdfObj.get("Subtype").value().name)

                    val bbox = annot.rect!!.get()
                    mOutputListener!!.println("  Position: " + ", " + format.format(bbox[0])
                            + ", " + format.format(bbox[1])
                            + ", " + format.format(bbox[2])
                            + ", " + format.format(bbox[3]))

                    when (annot.type) {
                        Annot.e_Link -> {
                            val link = com.pdftron.pdf.annots.Link(annot)
                            val action = link.action
                            if (action.isValid) {
                                if (action.type == Action.e_GoTo) {
                                    val dest = action.dest
                                    if (dest.isValid == false) {
                                        mOutputListener!!.println("  Destination is not valid.")
                                    } else {
                                        val page_link = dest.page.index
                                        mOutputListener!!.println("  Links to: page number $page_link in this document")
                                    }
                                } else if (action.type == Action.e_URI) {
                                    val uri = action.sdfObj.get("URI").value().asPDFText
                                    mOutputListener!!.println("  Links to: $uri")
                                }
                                // ...
                            }
                        }
                        Annot.e_Widget -> {
                        }
                        Annot.e_FileAttachment -> {
                        }
                    // ...
                        else -> {
                        }
                    }
                }
            }

            // Use the high-level API to create new annotations.
            val first_page = doc.getPage(1)

            // Create a hyperlink...
            val hyperlink = com.pdftron.pdf.annots.Link.create(doc, Rect(85.0, 570.0, 503.0, 524.0), Action.createURI(doc, "http://www.pdftron.com"))
            first_page.annotPushBack(hyperlink)

            // Create an intra-document link...
            val goto_page_3 = Action.createGoto(Destination.createFitH(doc.getPage(3), 0.0))
            val link = com.pdftron.pdf.annots.Link.create(doc.sdfDoc,
                    Rect(85.0, 458.0, 503.0, 502.0),
                    goto_page_3)

            // Set the annotation border width to 3 points...
            val border_style = Annot.BorderStyle(Annot.BorderStyle.e_solid, 10, 0, 0)
            link.borderStyle = border_style
            link.setColor(ColorPt(1.0, 0.0, 0.0), 3)

            // Add the new annotation to the first page
            first_page.annotPushBack(link)

            // Create a stamp annotation ...
            val stamp = com.pdftron.pdf.annots.RubberStamp.create(doc, Rect(30.0, 30.0, 300.0, 200.0))
            stamp.setIcon("Draft")
            first_page.annotPushBack(stamp)

            // Create a file attachment annotation (embed the 'peppers.jpg').
            val file_attach = com.pdftron.pdf.annots.FileAttachment.create(doc, Rect(80.0, 280.0, 200.0, 320.0), Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "peppers.jpg")!!.absolutePath)
            first_page.annotPushBack(file_attach)

            val circle = com.pdftron.pdf.annots.Circle.create(doc, Rect(10.0, 110.0, 100.0, 200.0))
            circle.setInteriorColor(ColorPt(0.0, 0.5, 1.0), 3)
            circle.title = "This is a title for the circle"
            circle.setColor(ColorPt(0.0, 1.0, 0.0), 3)
            circle.setInteriorColor(ColorPt(0.0, 0.0, 1.0), 3)
            circle.contentRect = Rect(12.0, 112.0, 98.0, 198.0)
            circle.opacity = 0.5
            first_page.annotPushBack(circle)

            val ink = com.pdftron.pdf.annots.Ink.create(doc, Rect(110.0, 10.0, 300.0, 200.0))
            val pt3 = Point(110.0, 10.0)
            //pt3.x = 110; pt3.y = 10;
            ink.setPoint(0, 0, pt3)
            pt3.x = 150.0
            pt3.y = 50.0
            ink.setPoint(0, 1, pt3)
            pt3.x = 190.0
            pt3.y = 60.0
            ink.setPoint(0, 2, pt3)
            pt3.x = 180.0
            pt3.y = 90.0
            ink.setPoint(1, 0, pt3)
            pt3.x = 190.0
            pt3.y = 95.0
            ink.setPoint(1, 1, pt3)
            pt3.x = 200.0
            pt3.y = 100.0
            ink.setPoint(1, 2, pt3)
            pt3.x = 166.0
            pt3.y = 86.0
            ink.setPoint(2, 0, pt3)
            pt3.x = 196.0
            pt3.y = 96.0
            ink.setPoint(2, 1, pt3)
            pt3.x = 221.0
            pt3.y = 121.0
            ink.setPoint(2, 2, pt3)
            pt3.x = 288.0
            pt3.y = 188.0
            ink.setPoint(2, 3, pt3)
            ink.setColor(ColorPt(0.0, 1.0, 1.0), 3)
            first_page.annotPushBack(ink)

            // ...
        }

        @Throws(PDFNetException::class)
        internal fun AnnotationLowLevelAPI(doc: PDFDoc) {
            val page = doc.pageIterator.next()

            var annots: Obj? = page?.annots

            if (annots == null) {
                // If there are no annotations, create a new annotation
                // array for the page.
                annots = doc.createIndirectArray()
                page?.sdfObj?.put("Annots", annots!!)
            }

            // Create a Text annotation
            val annot = doc.createIndirectDict()
            annot.putName("Subtype", "Text")
            annot.putBool("Open", true)
            annot.putString("Contents", "The quick brown fox ate the lazy mouse.")
            annot.putRect("Rect", 266.0, 116.0, 430.0, 204.0)

            // Insert the annotation in the page annotation array
            annots?.pushBack(annot)

            // Create a Link annotation
            val link1 = doc.createIndirectDict()
            link1.putName("Subtype", "Link")
            val dest = Destination.createFit(doc.getPage(2))
            link1.put("Dest", dest.sdfObj)
            link1.putRect("Rect", 85.0, 705.0, 503.0, 661.0)
            annots?.pushBack(link1)

            // Create another Link annotation
            val link2 = doc.createIndirectDict()
            link2.putName("Subtype", "Link")
            val dest2 = Destination.createFit(doc.getPage(3))
            link2.put("Dest", dest2.sdfObj)
            link2.putRect("Rect", 85.0, 638.0, 503.0, 594.0)
            annots?.pushBack(link2)

            // Note that PDFNet APi can be used to modify existing annotations.
            // In the following example we will modify the second link annotation
            // (link2) so that it points to the 10th page. We also use a different
            // destination page fit type.

            // link2 = annots.GetAt(annots.Size()-1);
            link2.put("Dest",
                    Destination.createXYZ(doc.getPage(10), 100.0, (792 - 70).toDouble(), 10.0).sdfObj)

            // Create a third link annotation with a hyperlink action (all other
            // annotation types can be created in a similar way)
            val link3 = doc.createIndirectDict()
            link3.putName("Subtype", "Link")
            link3.putRect("Rect", 85.0, 570.0, 503.0, 524.0)

            // Create a URI action
            val action = link3.putDict("A")
            action.putName("S", "URI")
            action.putString("URI", "http://www.pdftron.com")

            annots?.pushBack(link3)
        }

        @Throws(PDFNetException::class)
        internal fun CreateTestAnnots(doc: PDFDoc) {
            val ew = ElementWriter()
            val eb = ElementBuilder()
            var element: Element

            val first_page = doc.pageCreate(Rect(0.0, 0.0, 600.0, 600.0))
            doc.pagePushBack(first_page)
            ew.begin(first_page, ElementWriter.e_overlay, false)    // begin writing to this page
            ew.end()  // save changes to the current page

            //
            // Test of a free text annotation.
            //
            run {
                val txtannot = FreeText.create(doc, Rect(10.0, 400.0, 160.0, 570.0))
                txtannot.contents = "\n\nSome swift brown fox snatched a gray hare out of the air by freezing it with an angry glare." + "\n\nAha!\n\nAnd there was much rejoicing!"
                txtannot.borderStyle = Annot.BorderStyle(Annot.BorderStyle.e_solid, 1, 10, 20)
                txtannot.quaddingFormat = 0
                first_page.annotPushBack(txtannot)
                txtannot.refreshAppearance()
            }
            run {
                val txtannot = FreeText.create(doc, Rect(100.0, 100.0, 350.0, 500.0))
                txtannot.contentRect = Rect(200.0, 200.0, 350.0, 500.0)
                txtannot.contents = "\n\nSome swift brown fox snatched a gray hare out of the air by freezing it with an angry glare." + "\n\nAha!\n\nAnd there was much rejoicing!"
                txtannot.setCalloutLinePoints(Point(200.0, 300.0), Point(150.0, 290.0), Point(110.0, 110.0))
                txtannot.borderStyle = Annot.BorderStyle(Annot.BorderStyle.e_solid, 1, 10, 20)
                txtannot.endingStyle = Line.e_ClosedArrow
                txtannot.setColor(ColorPt(0.0, 1.0, 0.0))
                txtannot.quaddingFormat = 1
                first_page.annotPushBack(txtannot)
                txtannot.refreshAppearance()
            }
            run {
                val txtannot = FreeText.create(doc, Rect(400.0, 10.0, 550.0, 400.0))
                txtannot.contents = "\n\nSome swift brown fox snatched a gray hare out of the air by freezing it with an angry glare." + "\n\nAha!\n\nAnd there was much rejoicing!"
                txtannot.borderStyle = Annot.BorderStyle(Annot.BorderStyle.e_solid, 1, 10, 20)
                txtannot.setColor(ColorPt(0.0, 0.0, 1.0))
                txtannot.opacity = 0.2
                txtannot.quaddingFormat = 2
                first_page.annotPushBack(txtannot)
                txtannot.refreshAppearance()
            }

            val page = doc.pageCreate(Rect(0.0, 0.0, 600.0, 600.0))
            doc.pagePushBack(page)
            ew.begin(page, ElementWriter.e_overlay, false)    // begin writing to this page
            eb.reset()            // Reset the GState to default
            ew.end()  // save changes to the current page

            run {
                //Create a Line annotation...
                val line = Line.create(doc, Rect(250.0, 250.0, 400.0, 400.0))
                line.startPoint = Point(350.0, 270.0)
                line.endPoint = Point(260.0, 370.0)
                line.startStyle = Line.e_Square
                line.endStyle = Line.e_Circle
                line.setColor(ColorPt(.3, .5, 0.0), 3)
                line.contents = "Dashed Captioned"
                line.showCaption = true
                line.captionPosition = Line.e_Top
                val dash = doubleArrayOf(2.0, 2.0)
                line.borderStyle = Annot.BorderStyle(Annot.BorderStyle.e_dashed, 2, 0, 0, dash)
                line.refreshAppearance()
                page.annotPushBack(line)
            }
            run {
                val line = Line.create(doc, Rect(347.0, 377.0, 600.0, 600.0))
                line.startPoint = Point(385.0, 410.0)
                line.endPoint = Point(540.0, 555.0)
                line.startStyle = Line.e_Circle
                line.endStyle = Line.e_OpenArrow
                line.setColor(ColorPt(1.0, 0.0, 0.0), 3)
                line.setInteriorColor(ColorPt(0.0, 1.0, 0.0), 3)
                line.contents = "Inline Caption"
                line.showCaption = true
                line.captionPosition = Line.e_Inline
                line.leaderLineExtensionLength = -4.0
                line.leaderLineLength = -12.0
                line.leaderLineOffset = 2.0
                line.refreshAppearance()
                page.annotPushBack(line)
            }
            run {
                val line = Line.create(doc, Rect(10.0, 400.0, 200.0, 600.0))
                line.startPoint = Point(25.0, 426.0)
                line.endPoint = Point(180.0, 555.0)
                line.startStyle = Line.e_Circle
                line.endStyle = Line.e_Square
                line.setColor(ColorPt(0.0, 0.0, 1.0), 3)
                line.setInteriorColor(ColorPt(1.0, 0.0, 0.0), 3)
                line.contents = "Offset Caption"
                line.showCaption = true
                line.captionPosition = Line.e_Top
                line.textHOffset = -60.0
                line.textVOffset = 10.0
                line.refreshAppearance()
                page.annotPushBack(line)
            }
            run {
                val line = Line.create(doc, Rect(200.0, 10.0, 400.0, 70.0))
                line.startPoint = Point(220.0, 25.0)
                line.endPoint = Point(370.0, 60.0)
                line.startStyle = Line.e_Butt
                line.endStyle = Line.e_OpenArrow
                line.setColor(ColorPt(0.0, 0.0, 1.0), 3)
                line.contents = "Regular Caption"
                line.showCaption = true
                line.captionPosition = Line.e_Top
                line.refreshAppearance()
                page.annotPushBack(line)
            }
            run {
                val line = Line.create(doc, Rect(200.0, 70.0, 400.0, 130.0))
                line.startPoint = Point(220.0, 111.0)
                line.endPoint = Point(370.0, 78.0)
                line.startStyle = Line.e_Circle
                line.endStyle = Line.e_Diamond
                line.contents = "Circle to Diamond"
                line.setColor(ColorPt(0.0, 0.0, 1.0), 3)
                line.setInteriorColor(ColorPt(0.0, 1.0, 0.0), 3)
                line.showCaption = true
                line.captionPosition = Line.e_Top
                line.refreshAppearance()
                page.annotPushBack(line)
            }
            run {
                val line = Line.create(doc, Rect(10.0, 100.0, 160.0, 200.0))
                line.startPoint = Point(15.0, 110.0)
                line.endPoint = Point(150.0, 190.0)
                line.startStyle = Line.e_Slash
                line.endStyle = Line.e_ClosedArrow
                line.contents = "Slash to CArrow"
                line.setColor(ColorPt(1.0, 0.0, 0.0), 3)
                line.setInteriorColor(ColorPt(0.0, 1.0, 1.0), 3)
                line.showCaption = true
                line.captionPosition = Line.e_Top
                line.refreshAppearance()
                page.annotPushBack(line)
            }
            run {
                val line = Line.create(doc, Rect(270.0, 270.0, 570.0, 433.0))
                line.startPoint = Point(300.0, 400.0)
                line.endPoint = Point(550.0, 300.0)
                line.startStyle = Line.e_RClosedArrow
                line.endStyle = Line.e_ROpenArrow
                line.contents = "ROpen & RClosed arrows"
                line.setColor(ColorPt(0.0, 0.0, 1.0), 3)
                line.setInteriorColor(ColorPt(0.0, 1.0, 0.0), 3)
                line.showCaption = true
                line.captionPosition = Line.e_Top
                line.refreshAppearance()
                page.annotPushBack(line)
            }
            run {
                val line = Line.create(doc, Rect(195.0, 395.0, 205.0, 505.0))
                line.startPoint = Point(200.0, 400.0)
                line.endPoint = Point(200.0, 500.0)
                line.refreshAppearance()
                page.annotPushBack(line)
            }
            run {
                val line = Line.create(doc, Rect(55.0, 299.0, 150.0, 301.0))
                line.startPoint = Point(55.0, 300.0)
                line.endPoint = Point(155.0, 300.0)
                line.startStyle = Line.e_Circle
                line.endStyle = Line.e_Circle
                line.contents = "Caption that's longer than its line."
                line.setColor(ColorPt(1.0, 0.0, 1.0), 3)
                line.setInteriorColor(ColorPt(0.0, 1.0, 0.0), 3)
                line.showCaption = true
                line.captionPosition = Line.e_Top
                line.refreshAppearance()
                page.annotPushBack(line)
            }
            run {
                val line = Line.create(doc, Rect(300.0, 200.0, 390.0, 234.0))
                line.startPoint = Point(310.0, 210.0)
                line.endPoint = Point(380.0, 220.0)
                line.setColor(ColorPt(0.0, 0.0, 0.0), 3)
                line.refreshAppearance()
                page.annotPushBack(line)
            }

            val page3 = doc.pageCreate(Rect(0.0, 0.0, 600.0, 600.0))
            ew.begin(page3)    // begin writing to the page
            ew.end()  // save changes to the current page
            doc.pagePushBack(page3)
            run {
                val circle = Circle.create(doc, Rect(300.0, 300.0, 390.0, 350.0))
                circle.setColor(ColorPt(0.0, 0.0, 0.0), 3)
                circle.refreshAppearance()
                page3.annotPushBack(circle)
            }
            run {
                val circle = Circle.create(doc, Rect(100.0, 100.0, 200.0, 200.0))
                circle.setColor(ColorPt(0.0, 1.0, 0.0), 3)
                circle.setInteriorColor(ColorPt(0.0, 0.0, 1.0), 3)
                val dash = doubleArrayOf(2.0, 4.0)
                circle.borderStyle = Annot.BorderStyle(Annot.BorderStyle.e_dashed, 3, 0, 0, dash)
                circle.setPadding(2.0)
                circle.refreshAppearance()
                page3.annotPushBack(circle)
            }
            run {
                val sq = Square.create(doc, Rect(10.0, 200.0, 80.0, 300.0))
                sq.setColor(ColorPt(0.0, 0.0, 0.0), 3)
                sq.refreshAppearance()
                page3.annotPushBack(sq)
            }
            run {
                val sq = Square.create(doc, Rect(500.0, 200.0, 580.0, 300.0))
                sq.setColor(ColorPt(1.0, 0.0, 0.0), 3)
                sq.setInteriorColor(ColorPt(0.0, 1.0, 1.0), 3)
                val dash = doubleArrayOf(4.0, 2.0)
                sq.borderStyle = Annot.BorderStyle(Annot.BorderStyle.e_dashed, 6, 0, 0, dash)
                sq.setPadding(4.0)
                sq.refreshAppearance()
                page3.annotPushBack(sq)
            }
            run {
                val poly = Polygon.create(doc, Rect(5.0, 500.0, 125.0, 590.0))
                poly.setColor(ColorPt(1.0, 0.0, 0.0), 3)
                poly.setInteriorColor(ColorPt(1.0, 1.0, 0.0), 3)
                poly.setVertex(0, Point(12.0, 510.0))
                poly.setVertex(1, Point(100.0, 510.0))
                poly.setVertex(2, Point(100.0, 555.0))
                poly.setVertex(3, Point(35.0, 544.0))
                poly.borderStyle = Annot.BorderStyle(Annot.BorderStyle.e_solid, 4, 0, 0)
                poly.setPadding(4.0)
                poly.refreshAppearance()
                page3.annotPushBack(poly)
            }
            run {
                val poly = PolyLine.create(doc, Rect(400.0, 10.0, 500.0, 90.0))
                poly.setColor(ColorPt(1.0, 0.0, 0.0), 3)
                poly.setInteriorColor(ColorPt(0.0, 1.0, 0.0), 3)
                poly.setVertex(0, Point(405.0, 20.0))
                poly.setVertex(1, Point(440.0, 40.0))
                poly.setVertex(2, Point(410.0, 60.0))
                poly.setVertex(3, Point(470.0, 80.0))
                poly.borderStyle = Annot.BorderStyle(Annot.BorderStyle.e_solid, 2, 0, 0)
                poly.setPadding(4.0)
                poly.startStyle = Line.e_RClosedArrow
                poly.endStyle = Line.e_ClosedArrow
                poly.refreshAppearance()
                page3.annotPushBack(poly)
            }
            run {
                val lk = Link.create(doc, Rect(5.0, 5.0, 55.0, 24.0))
                lk.refreshAppearance()
                page3.annotPushBack(lk)
            }

            val page4 = doc.pageCreate(Rect(0.0, 0.0, 600.0, 600.0))
            ew.begin(page4)    // begin writing to the page
            ew.end()  // save changes to the current page
            doc.pagePushBack(page4)

            run {
                ew.begin(page4)
                val font = Font.create(doc, Font.e_helvetica)
                element = eb.createTextBegin(font, 16.0)
                element.setPathFill(true)
                ew.writeElement(element)
                element = eb.createTextRun("Some random text on the page", font, 16.0)
                element.setTextMatrix(1.0, 0.0, 0.0, 1.0, 100.0, 500.0)
                ew.writeElement(element)
                ew.writeElement(eb.createTextEnd())
                ew.end()
            }
            run {
                val hl = Highlight.create(doc, Rect(100.0, 490.0, 150.0, 515.0))
                hl.setColor(ColorPt(0.0, 1.0, 0.0), 3)
                hl.refreshAppearance()
                page4.annotPushBack(hl)
            }
            run {
                val sq = Squiggly.create(doc, Rect(100.0, 450.0, 250.0, 600.0))
                sq.setQuadPoint(0, QuadPoint(Point(122.0, 455.0), Point(240.0, 545.0), Point(230.0, 595.0), Point(101.0, 500.0)))
                sq.refreshAppearance()
                page4.annotPushBack(sq)
            }
            run {
                val cr = Caret.create(doc, Rect(100.0, 40.0, 129.0, 69.0))
                cr.setColor(ColorPt(0.0, 0.0, 1.0), 3)
                cr.symbol = "P"
                cr.refreshAppearance()
                page4.annotPushBack(cr)
            }

            val page5 = doc.pageCreate(Rect(0.0, 0.0, 600.0, 600.0))
            ew.begin(page5)    // begin writing to the page
            ew.end()  // save changes to the current page
            doc.pagePushBack(page5)
            val fs = FileSpec.create(doc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "butterfly.png")!!.absolutePath, false)
            val page6 = doc.pageCreate(Rect(0.0, 0.0, 600.0, 600.0))
            ew.begin(page6)    // begin writing to the page
            ew.end()  // save changes to the current page
            doc.pagePushBack(page6)

            run {
                val txt = Text.create(doc, Point(10.0, 20.0))
                txt.setIcon("UserIcon")
                txt.contents = "User defined icon, unrecognized by appearance generator"
                txt.setColor(ColorPt(0.0, 1.0, 0.0))
                txt.refreshAppearance()
                page6.annotPushBack(txt)
            }
            run {
                val ink = Ink.create(doc, Rect(100.0, 400.0, 200.0, 550.0))
                ink.setColor(ColorPt(0.0, 0.0, 1.0))
                ink.setPoint(1, 3, Point(220.0, 505.0))
                ink.setPoint(1, 0, Point(100.0, 490.0))
                ink.setPoint(0, 1, Point(120.0, 410.0))
                ink.setPoint(0, 0, Point(100.0, 400.0))
                ink.setPoint(1, 2, Point(180.0, 490.0))
                ink.setPoint(1, 1, Point(140.0, 440.0))
                ink.borderStyle = Annot.BorderStyle(Annot.BorderStyle.e_solid, 3, 0, 0)
                ink.refreshAppearance()
                page6.annotPushBack(ink)
            }

            val page7 = doc.pageCreate(Rect(0.0, 0.0, 600.0, 600.0))
            ew.begin(page7)    // begin writing to the page
            ew.end()  // save changes to the current page
            doc.pagePushBack(page7)

            run {
                val snd = Sound.create(doc, Rect(100.0, 500.0, 120.0, 520.0))
                snd.setColor(ColorPt(1.0, 1.0, 0.0))
                snd.icon = Sound.e_Speaker
                snd.refreshAppearance()
                page7.annotPushBack(snd)
            }
            run {
                val snd = Sound.create(doc, Rect(200.0, 500.0, 220.0, 520.0))
                snd.setColor(ColorPt(1.0, 1.0, 0.0))
                snd.icon = Sound.e_Mic
                snd.refreshAppearance()
                page7.annotPushBack(snd)
            }

            val page8 = doc.pageCreate(Rect(0.0, 0.0, 600.0, 600.0))
            ew.begin(page8)    // begin writing to the page
            ew.end()  // save changes to the current page
            doc.pagePushBack(page8)

            for (ipage in 0..1) {
                var px = 5.0
                var py = 520.0
                for (istamp in 0..RubberStamp.e_Draft) {
                    val st = RubberStamp.create(doc, Rect(1.0, 1.0, 100.0, 100.0))
                    st.SetIcon(istamp)
                    st.contents = st.iconName
                    st.rect = Rect(px, py, px + 100, py + 25)
                    py -= 100.0
                    if (py < 0) {
                        py = 520.0
                        px += 200.0
                    }
                    if (ipage == 0)
                    else {
                        page8.annotPushBack(st)
                        st.refreshAppearance()
                    }//page7.AnnotPushBack( st );
                }
            }
            val st = RubberStamp.create(doc, Rect(400.0, 5.0, 550.0, 45.0))
            st.setIcon("UserStamp")
            st.contents = "User defined stamp"
            page8.annotPushBack(st)
            st.refreshAppearance()
        }
    }


}