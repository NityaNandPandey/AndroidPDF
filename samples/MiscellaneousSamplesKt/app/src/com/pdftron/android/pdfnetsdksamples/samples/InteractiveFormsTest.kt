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
import com.pdftron.pdf.ColorPt
import com.pdftron.pdf.ColorSpace
import com.pdftron.pdf.Element
import com.pdftron.pdf.ElementBuilder
import com.pdftron.pdf.ElementWriter
import com.pdftron.pdf.Field
import com.pdftron.pdf.FieldIterator
import com.pdftron.pdf.FileSpec
import com.pdftron.pdf.Font
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.Page
import com.pdftron.pdf.PageIterator
import com.pdftron.pdf.Rect
import com.pdftron.sdf.Obj
import com.pdftron.sdf.SDFDoc

import java.io.File
import java.util.ArrayList

//---------------------------------------------------------------------------------------
//This sample illustrates basic PDFNet capabilities related to interactive
//forms (also known as AcroForms).
//---------------------------------------------------------------------------------------

class InteractiveFormsTest : PDFNetSample() {
    init {
        setTitle(R.string.sample_interactiveforms_title)
        setDescription(R.string.sample_interactiveforms_description)
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        mOutputListener = outputListener
        mFileList.clear()
        printHeader(outputListener!!)

        // string input_path =  "../../TestFiles/";

        //----------------------------------------------------------------------------------
        // Example 1: Programatically create new Form Fields and Widget Annotations.
        //----------------------------------------------------------------------------------
        try {
            val doc = PDFDoc()
            val blank_page = doc.pageCreate() // Create a blank new page and add some form fields.

            // Create new fields.
            val emp_first_name = doc.fieldCreate("employee.name.first",
                    Field.e_text, "John")
            val emp_last_name = doc.fieldCreate("employee.name.last",
                    Field.e_text, "Doe")
            val emp_last_check1 = doc.fieldCreate("employee.name.check1", Field.e_check, "Yes")

            val submit = doc.fieldCreate("submit", Field.e_button)

            // Create page annotations for the above fields.

            // Create text annotations
            val annot1 = com.pdftron.pdf.annots.Widget.create(doc, Rect(50.0, 550.0, 350.0, 600.0), emp_first_name)
            val annot2 = com.pdftron.pdf.annots.Widget.create(doc, Rect(50.0, 450.0, 350.0, 500.0), emp_last_name)

            // Create a check-box annotation
            val annot3 = com.pdftron.pdf.annots.Widget.create(doc, Rect(64.0, 356.0, 120.0, 410.0), emp_last_check1)
            // Set the annotation appearance for the "Yes" state...
            annot3.setAppearance(createCheckmarkAppearance(doc), Annot.e_normal, "Yes")

            // Create button annotation
            val annot4 = com.pdftron.pdf.annots.Widget.create(doc, Rect(64.0, 284.0, 163.0, 320.0), submit)
            // Set the annotation appearances for the down and up state...
            annot4.setAppearance(createButtonAppearance(doc, false), Annot.e_normal)
            annot4.setAppearance(createButtonAppearance(doc, true), Annot.e_down)

            // Create 'SubmitForm' action. The action will be linked to the button.
            val url = FileSpec.createURL(doc, "http://www.pdftron.com")
            val button_action = Action.createSubmitForm(url)

            // Associate the above action with 'Down' event in annotations action dictionary.
            val annot_action = annot4.sdfObj.putDict("AA")
            annot_action.put("D", button_action.sdfObj)

            blank_page.annotPushBack(annot1)  // Add annotations to the page
            blank_page.annotPushBack(annot2)
            blank_page.annotPushBack(annot3)
            blank_page.annotPushBack(annot4)

            doc.pagePushBack(blank_page)    // Add the page as the last page in the document.

            // If you are not satisfied with the look of default auto-generated appearance
            // streams you can delete "AP" entry from the Widget annotation and set
            // "NeedAppearances" flag in AcroForm dictionary:
            //    doc.GetAcroForm().PutBool("NeedAppearances", true);
            // This will force the viewer application to auto-generate new appearance streams
            // every time the document is opened.
            //
            // Alternatively you can generate custom annotation appearance using ElementWriter
            // and then set the "AP" entry in the widget dictionary to the new appearance
            // stream.
            //
            // Yet another option is to pre-populate field entries with dummy text. When
            // you edit the field values using PDFNet the new field appearances will match
            // the old ones.

            //doc.GetAcroForm().Put("NeedAppearances", new Bool(true));
            doc.refreshFieldAppearances()

            doc.save(Utils.createExternalFile("forms_test1.pdf").absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //----------------------------------------------------------------------------------
        // Example 2:
        // Fill-in forms / Modify values of existing fields.
        // Traverse all form fields in the document (and print out their names).
        // Search for specific fields in the document.
        //----------------------------------------------------------------------------------
        try {
            val doc = PDFDoc(Utils.createExternalFile("forms_test1.pdf").absolutePath)
            doc.initSecurityHandler()

            val itr = doc.fieldIterator
            while (itr.hasNext()) {
                val current = itr.next()
                mOutputListener!!.println("Field name: " + current!!.getName())
                mOutputListener!!.println("Field partial name: " + current.getPartialName())

                mOutputListener!!.print("Field type: ")
                val type = current.getType()
                when (type) {
                    Field.e_button -> mOutputListener!!.println("Button")
                    Field.e_radio -> mOutputListener!!.println("Radio button")
                    Field.e_check -> {
                        current.setValue(true)
                        mOutputListener!!.println("Check box")
                    }
                    Field.e_text -> {
                        mOutputListener!!.println("Text")
                        // Edit all variable text in the document
                        val old_value: String
                        if (current.getValue() != null) {
                            old_value = current.getValueAsString()
                            current.setValue("This is a new value. The old one was: $old_value")
                        }
                    }
                    Field.e_choice -> mOutputListener!!.println("Choice")
                    Field.e_signature -> mOutputListener!!.println("Signature")
                }

                mOutputListener!!.println("------------------------------")
            }

            // Search for a specific field
            val f = doc.getField("employee.name.first")
            if (f != null) {
                mOutputListener!!.println("Field search for " + f.name + " was successful")
            } else {
                mOutputListener!!.println("Field search failed")
            }

            // Regenerate field appearances.
            doc.refreshFieldAppearances()
            doc.save(Utils.createExternalFile("forms_test_edit.pdf").absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //----------------------------------------------------------------------------------
        // Sample: Form templating
        // Replicate pages and form data within a document. Then rename field names to make
        // them unique.
        //----------------------------------------------------------------------------------
        try {
            // Sample: Copying the page with forms within the same document
            val doc = PDFDoc(Utils.createExternalFile("forms_test1.pdf").absolutePath)
            doc.initSecurityHandler()

            val src_page = doc.getPage(1) as Page
            doc.pagePushBack(src_page)  // Append several copies of the first page
            doc.pagePushBack(src_page)     // Note that forms are successfully copied
            doc.pagePushBack(src_page)
            doc.pagePushBack(src_page)

            // Now we rename fields in order to make every field unique.
            // You can use this technique for dynamic template filling where you have a 'master'
            // form page that should be replicated, but with unique field names on every page.
            renameAllFields(doc, "employee.name.first")
            renameAllFields(doc, "employee.name.last")
            renameAllFields(doc, "employee.name.check1")
            renameAllFields(doc, "submit")

            doc.save(Utils.createExternalFile("forms_test1_cloned.pdf").absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            mFileList.add(File(doc.fileName).name)
            doc.close()
            mOutputListener!!.println("Done.")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        //----------------------------------------------------------------------------------
        // Sample:
        // Flatten all form fields in a document.
        // Note that this sample is intended to show that it is possible to flatten
        // individual fields. PDFNet provides a utility function PDFDoc.flattenAnnotations()
        // that will automatically flatten all fields.
        //----------------------------------------------------------------------------------
        try {
            val doc = PDFDoc(Utils.createExternalFile("forms_test1.pdf").absolutePath)
            doc.initSecurityHandler()

            // Traverse all pages
            if (true) {
                doc.flattenAnnotations()
            } else
            // Manual flattening
            {

                val pitr = doc.pageIterator
                while (pitr.hasNext()) {
                    val page = pitr.next()
                    val annots = page!!.getAnnots()
                    if (annots != null) {    // Look for all widget annotations (in reverse order)
                        for (i in annots.size().toInt() - 1 downTo 0) {
                            if (annots.getAt(i).get("Subtype").value().name == "Widget") {
                                val field = Field(annots.getAt(i))
                                field.flatten(page)

                                // Another way of making a read only field is by modifying
                                // field's e_read_only flag:
                                //    field.SetFlag(Field::e_read_only, true);
                            }
                        }
                    }
                }
            }

            doc.save(Utils.createExternalFile("forms_test1_flattened.pdf").absolutePath, SDFDoc.SaveMode.NO_FLAGS, null)
            mFileList.add(File(doc.fileName).name)
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

        @Throws(PDFNetException::class)
        internal fun renameAllFields(doc: PDFDoc, name: String) {
            var itr = doc.getFieldIterator(name)
            var counter = 0
            while (itr.hasNext()) {
                val f = itr.next()
                f!!.rename(name + counter)
                itr = doc.getFieldIterator(name)
                ++counter
            }
        }

        @Throws(PDFNetException::class)
        internal fun createCheckmarkAppearance(doc: PDFDoc): Obj {
            // Create a checkmark appearance stream ------------------------------------
            val build = ElementBuilder()
            val writer = ElementWriter()
            writer.begin(doc)
            writer.writeElement(build.createTextBegin())
            run {
                val symbol = "4"   // other options are circle ("l"), diamond ("H"), cross ("\x35")
                // See section D.4 "ZapfDingbats Set and Encoding" in PDF Reference Manual for
                // the complete graphical map for ZapfDingbats font.
                val checkmark = build.createTextRun(symbol, Font.create(doc, Font.e_zapf_dingbats), 1.0)
                writer.writeElement(checkmark)
            }
            writer.writeElement(build.createTextEnd())

            val stm = writer.end()

            // Set the bounding box
            stm.putRect("BBox", -0.2, -0.2, 1.0, 1.0)
            stm.putName("Subtype", "Form")
            return stm
        }

        @Throws(PDFNetException::class)
        internal fun createButtonAppearance(doc: PDFDoc, button_down: Boolean): Obj {
            // Create a button appearance stream ------------------------------------
            val build = ElementBuilder()
            val writer = ElementWriter()
            writer.begin(doc)

            // Draw background
            var element = build.createRect(0.0, 0.0, 101.0, 37.0)
            element.setPathFill(true)
            element.setPathStroke(false)
            element.gState.fillColorSpace = ColorSpace.createDeviceGray()
            element.gState.fillColor = ColorPt(0.75, 0.0, 0.0)
            writer.writeElement(element)

            // Draw 'Submit' text
            writer.writeElement(build.createTextBegin())
            run {
                val text = "Submit"
                element = build.createTextRun(text, Font.create(doc, Font.e_helvetica_bold), 12.0)
                element.gState.fillColor = ColorPt(0.0, 0.0, 0.0)

                if (button_down)
                    element.setTextMatrix(1.0, 0.0, 0.0, 1.0, 33.0, 10.0)
                else
                    element.setTextMatrix(1.0, 0.0, 0.0, 1.0, 30.0, 13.0)
                writer.writeElement(element)
            }
            writer.writeElement(build.createTextEnd())

            val stm = writer.end()

            // Set the bounding box
            stm.putRect("BBox", 0.0, 0.0, 101.0, 37.0)
            stm.putName("Subtype", "Form")
            return stm
        }
    }

}