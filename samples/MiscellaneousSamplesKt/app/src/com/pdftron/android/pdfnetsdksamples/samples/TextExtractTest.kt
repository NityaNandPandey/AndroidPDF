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
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementReader
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.PageIterator
import com.pdftron.pdf.Rect
import com.pdftron.pdf.TextExtractor

import java.util.ArrayList

class TextExtractTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_textextract_title)
        setDescription(R.string.sample_textextract_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        // string output_path = "../../TestFiles/Output/";
        val example1_basic = true
        val example2_xml = true
        val example3_wordlist = true
        val example4_advanced = true
        val example5_low_level = false

        // Sample code showing how to use high-level text extraction APIs.
        try {
            val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath)
            doc.initSecurityHandler()

            val page = doc.getPage(1)
            if (page == null) {
                mOutputListener!!.println("Page not found.")
            }

            val txt = TextExtractor()
            txt.begin(page!!)  // Read the page.
            // Other options you may want to consider...
            // txt.Begin(page, 0, TextExtractor.ProcessingFlags.e_no_dup_remove);
            // txt.Begin(page, 0, TextExtractor.ProcessingFlags.e_remove_hidden_text);
            // ...

            // Example 1. Get all text on the page in a single string.
            // Words will be separated with space or new line characters.
            if (example1_basic) {
                // Get the word count.
                mOutputListener!!.println("Word Count: " + txt.wordCount)

                mOutputListener!!.println("\n\n- GetAsText --------------------------\n" + txt.asText)
                mOutputListener!!.println("-----------------------------------------------------------")
            }

            // Example 2. Get XML logical structure for the page.
            if (example2_xml) {
                val text = txt.getAsXML(TextExtractor.e_words_as_elements or TextExtractor.e_output_bbox or TextExtractor.e_output_style_info)
                mOutputListener!!.println("\n\n- GetAsXML  --------------------------\n$text")
                mOutputListener!!.println("-----------------------------------------------------------")
            }

            // Example 3. Extract words one by one.
            if (example3_wordlist) {
                var word: TextExtractor.Word
                var line: TextExtractor.Line = txt.firstLine
                while (line.isValid) {
                    word = line.firstWord
                    while (word.isValid) {
                        mOutputListener!!.println(word.string)
                        word = word.nextWord
                    }
                    line = line.nextLine
                }
                mOutputListener!!.println("-----------------------------------------------------------")
            }

            // Example 4. A more advanced text extraction example.
            // The output is XML structure containing paragraphs, lines, words,
            // as well as style and positioning information.
            if (example4_advanced) {
                var bbox: Rect
                var cur_flow_id = -1
                var cur_para_id = -1

                var line: TextExtractor.Line
                var word: TextExtractor.Word
                var s: TextExtractor.Style
                var line_style: TextExtractor.Style

                // For each line on the page...
                line = txt.firstLine
                while (line.isValid) {
                    if (line.numWords == 0) {
                        line = line.nextLine
                        continue
                    }
                    if (cur_flow_id != line.flowID) {
                        if (cur_flow_id != -1) {
                            if (cur_para_id != -1) {
                                cur_para_id = -1
                                mOutputListener!!.println("</Para>")
                            }
                            mOutputListener!!.println("</Flow>")
                        }
                        cur_flow_id = line.flowID
                        mOutputListener!!.println("<Flow id=\"$cur_flow_id\">")
                    }

                    if (cur_para_id != line.paragraphID) {
                        if (cur_para_id != -1)
                            mOutputListener!!.println("</Para>")
                        cur_para_id = line.paragraphID
                        mOutputListener!!.println("<Para id=\"$cur_para_id\">")
                    }

                    bbox = line.bBox
                    line_style = line.style
                    mOutputListener!!.println("<Line box=\"" + bbox.x1 + ", " + bbox.y1 + ", " + bbox.x2 + ", " + bbox.y2 + "\"")
                    printStyle(line_style)
                    mOutputListener!!.println(">")

                    // For each word in the line...
                    word = line.firstWord
                    while (word.isValid) {
                        // Output the bounding box for the word.
                        bbox = word.bBox
                        mOutputListener!!.print("<Word box=\"" + bbox.x1 + ", " + bbox.y1 + ", " + bbox.x2 + ", " + bbox.y2 + "\"")

                        val sz = word.stringLen
                        if (sz == 0) {
                            word = word.nextWord
                            continue
                        }

                        // If the word style is different from the parent style, output the new style.
                        s = word.style
                        if (s != line_style) {
                            printStyle(s)
                        }

                        mOutputListener!!.println(">\n" + word.string)
                        mOutputListener!!.println("</Word>")
                        word = word.nextWord
                    }
                    mOutputListener!!.println("</Line>")
                    line = line.nextLine
                }

                if (cur_flow_id != -1) {
                    if (cur_para_id != -1) {
                        cur_para_id = -1
                        mOutputListener!!.println("</Para>")
                    }
                    mOutputListener!!.println("</Flow>")
                }
            }
            txt.destroy()
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: PDFNetException) {
            mOutputListener!!.println(e.stackTrace)
        }

        // Sample code showing how to use low-level text extraction APIs.
        if (example5_low_level) {
            try {
                val doc = PDFDoc(Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + "newsletter.pdf")!!.absolutePath)
                doc.initSecurityHandler()

                // Example 1. Extract all text content from the document

                val reader = ElementReader()
                //  Read every page
                val itr = doc.pageIterator
                while (itr.hasNext()) {
                    reader.begin(itr.next())
                    DumpAllText(reader)
                    reader.end()
                }

                // Example 2. Extract text content based on the
                // selection rectangle.
                mOutputListener!!.print("\n----------------------------------------------------")
                mOutputListener!!.print("\nExtract text based on the selection rectangle.")
                mOutputListener!!.println("\n----------------------------------------------------")

                val first_page = doc.pageIterator.next()
                var s1 = ReadTextFromRect(first_page, Rect(27.0, 392.0, 563.0, 534.0), reader)
                mOutputListener!!.print("\nField 1: $s1")

                s1 = ReadTextFromRect(first_page, Rect(28.0, 551.0, 106.0, 623.0), reader)
                mOutputListener!!.print("\nField 2: $s1")

                s1 = ReadTextFromRect(first_page, Rect(208.0, 550.0, 387.0, 621.0), reader)
                mOutputListener!!.print("\nField 3: $s1")

                // ...
                doc.close()
                mOutputListener!!.println("Done.")
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        for (file in mFileList) {
            addToFileList(file)
        }
        printFooter(outputListener)
    }

    companion object {

        private var mOutputListener: OutputListener? = null

        private val mFileList = ArrayList<String>()


        internal fun printStyle(s: TextExtractor.Style) {
            val r = s.color[0].toString()
            val g = s.color[1].toString()
            val b = s.color[2].toString()
            mOutputListener!!.print(" style=\"font-family:" + s.fontName + "; "
                    + "font-size:" + s.fontSize + ";"
                    + (if (s.isSerif) " sans-serif; " else " ")
                    + "color: [" + r + "," + g + "," + b + "]" + "\"")
        }

        // A utility method used to dump all text content in the console window.
        @Throws(PDFNetException::class)
        internal fun DumpAllText(reader: ElementReader) {
            var element: Element?
            while (true) {
                element = reader.next()
                if (element == null) {
                    break
                }
                when (element.type) {
                    Element.e_text_begin -> mOutputListener!!.println("\n--> Text Block Begin")
                    Element.e_text_end -> mOutputListener!!.println("\n--> Text Block End")
                    Element.e_text -> {
                        val bbox = element.bBox
                        if (bbox != null) {
                            mOutputListener!!.println("\n--> BBox: " + bbox.x1 + ", "
                                    + bbox.y1 + ", "
                                    + bbox.x2 + ", "
                                    + bbox.y2)

                            val arr = element.textString
                            mOutputListener!!.println(arr)
                        }
                    }
                    Element.e_text_new_line -> mOutputListener!!.println("\n--> New Line")
                    Element.e_form                // Process form XObjects
                    -> {
                        reader.formBegin()
                        DumpAllText(reader)
                        reader.end()
                    }
                }
            }
        }

        // A helper method for ReadTextFromRect
        @Throws(PDFNetException::class)
        internal fun RectTextSearch(reader: ElementReader, pos: Rect): String {
            var element: Element?
            var srch_str = String()
            while (true) {
                element = reader.next()
                if (element == null) {
                    break
                }
                when (element.type) {
                    Element.e_text -> {
                        val bbox = element.bBox
                        if (bbox != null) {
                            if (bbox.intersectRect(bbox, pos)) {
                                val arr = element.textString
                                srch_str += arr
                                srch_str += "\n" // add a new line?
                            }
                        }
                    }
                    Element.e_text_new_line -> {
                    }
                    Element.e_form // Process form XObjects
                    -> {
                        reader.formBegin()
                        srch_str += RectTextSearch(reader, pos)
                        reader.end()
                    }
                }
            }
            return srch_str
        }

        // A utility method used to extract all text content from
        // a given selection rectangle. The recnagle coordinates are
        // expressed in PDF user/page coordinate system.
        @Throws(PDFNetException::class)
        internal fun ReadTextFromRect(page: Page?, pos: Rect, reader: ElementReader): String {
            reader.begin(page)
            val srch_str = RectTextSearch(reader, pos)
            reader.end()
            return srch_str
        }
    }

}