//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.ColorSpace;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.FieldIterator;
import com.pdftron.pdf.FileSpec;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.Rect;
import com.pdftron.sdf.Obj;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;


//---------------------------------------------------------------------------------------
//This sample illustrates basic PDFNet capabilities related to interactive 
//forms (also known as AcroForms). 
//---------------------------------------------------------------------------------------

public class InteractiveFormsSample extends PDFNetSample {

    public InteractiveFormsSample() {
        setTitle(R.string.sample_interactiveforms_title);
        setDescription(R.string.sample_interactiveforms_description);
    }
    
    @SuppressWarnings("unused")
    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        printHeader(outputListener);

        // ----------------------------------------------------------------------------------
        // Example 1: Programatically create new Form Fields and Widget
        // Annotations.
        // ----------------------------------------------------------------------------------
        try {
            PDFDoc doc = new PDFDoc();
            Page blank_page = doc.pageCreate(); // Create a blank new page and add some form fields.

            // Create new fields.
            Field emp_first_name = doc.fieldCreate("employee.name.first", Field.e_text, "John");
            Field emp_last_name = doc.fieldCreate("employee.name.last", Field.e_text, "Doe");
            Field emp_last_check1 = doc.fieldCreate("employee.name.check1", Field.e_check, "Yes");

            Field submit = doc.fieldCreate("submit", Field.e_button);

            // Create page annotations for the above fields.

            // Create text annotations
            com.pdftron.pdf.annots.Widget annot1 = com.pdftron.pdf.annots.Widget
                    .create(doc, new Rect(50, 550, 350, 600), emp_first_name);
            com.pdftron.pdf.annots.Widget annot2 = com.pdftron.pdf.annots.Widget
                    .create(doc, new Rect(50, 450, 350, 500), emp_last_name);

            // Create a check-box annotation
            com.pdftron.pdf.annots.Widget annot3 = com.pdftron.pdf.annots.Widget
                    .create(doc, new Rect(64, 356, 120, 410), emp_last_check1);
            // Set the annotation appearance for the "Yes" state...
            annot3.setAppearance(createCheckmarkAppearance(doc), Annot.e_normal, "Yes");

            // Create button annotation
            com.pdftron.pdf.annots.Widget annot4 = com.pdftron.pdf.annots.Widget
                    .create(doc, new Rect(64, 284, 163, 320), submit);
            // Set the annotation appearances for the down and up state...
            annot4.setAppearance(createButtonAppearance(doc, false), Annot.e_normal);
            annot4.setAppearance(createButtonAppearance(doc, true), Annot.e_down);

            // Create 'SubmitForm' action. The action will be linked to the button.
            FileSpec url = FileSpec.createURL(doc, "http://www.pdftron.com");
            Action button_action = Action.createSubmitForm(url);

            // Associate the above action with 'Down' event in annotations
            // action dictionary.
            Obj annot_action = annot4.getSDFObj().putDict("AA");
            annot_action.put("D", button_action.getSDFObj());

            blank_page.annotPushBack(annot1); // Add annotations to the page
            blank_page.annotPushBack(annot2);
            blank_page.annotPushBack(annot3);
            blank_page.annotPushBack(annot4);

            doc.pagePushBack(blank_page); // Add the page as the last page in the document.

            // If you are not satisfied with the look of default auto-generated appearance
            // streams you can delete "AP" entry from the Widget annotation and set
            // "NeedAppearances" flag in AcroForm dictionary:
            // doc.GetAcroForm().PutBool("NeedAppearances", true);
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

            // doc.getAcroForm().putBool("NeedAppearances", true);
            doc.refreshFieldAppearances();

            doc.save(Utils.createExternalFile("forms_test1.pdf").getAbsolutePath(), 0, null);
            addToFileList("forms_test1.pdf");
            doc.close();
            outputListener.println("Done example 1.");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }

        // ----------------------------------------------------------------------------------
        // Example 2:
        // Fill-in forms / Modify values of existing fields.
        // Traverse all form fields in the document (and print out their names).
        // Search for specific fields in the document.
        // ----------------------------------------------------------------------------------
        try {
            PDFDoc doc = new PDFDoc(Utils.createExternalFile("forms_test1.pdf").getAbsolutePath());
            doc.initSecurityHandler();

            FieldIterator itr = doc.getFieldIterator();
            while (itr.hasNext()) {
                Field current = (Field) (itr.next());
                outputListener.println("Field name: " + current.getName());
                outputListener.println("Field partial name: " + current.getPartialName());

                outputListener.print("Field type: ");
                int type = current.getType();
                switch (type) {
                case Field.e_button:
                    outputListener.println("Button");
                    break;
                case Field.e_radio:
                    outputListener.println("Radio button");
                    break;
                case Field.e_check:
                    current.setValue(true);
                    outputListener.println("Check box");
                    break;
                case Field.e_text: {
                    outputListener.println("Text");
                    // Edit all variable text in the document
                    String old_value;
                    if (current.getValue() != null) {
                        old_value = current.getValueAsString();
                        current.setValue("This is a new value. The old one was: " + old_value);
                    }
                }
                    break;
                case Field.e_choice:
                    outputListener.println("Choice");
                    break;
                case Field.e_signature:
                    outputListener.println("Signature");
                    break;
                }

                outputListener.println("--------------------");
            }

            // Search for a specific field
            Field f = doc.getField("employee.name.first");
            if (f != null) {
                outputListener.println("Field search for " + f.getName() + " was successful");
            } else {
                outputListener.println("Field search failed");
            }

            // Regenerate field appearances.
            doc.refreshFieldAppearances();
            doc.save(Utils.createExternalFile("forms_test_edit.pdf").getAbsolutePath(), 0, null);
            addToFileList("forms_test_edit.pdf");
            doc.close();
            outputListener.println("Done example 2.");
        } catch (Exception e) {
            outputListener.println(e.getMessage());
        }

        // ----------------------------------------------------------------------------------
        // Sample: Form templating
        // Replicate pages and form data within a document. Then rename field
        // names to make
        // them unique.
        // ----------------------------------------------------------------------------------
        try {
            // Sample: Copying the page with forms within the same document
            PDFDoc doc = new PDFDoc(Utils.createExternalFile("forms_test1.pdf").getAbsolutePath());
            doc.initSecurityHandler();

            Page src_page = (Page) (doc.getPage(1));
            doc.pagePushBack(src_page); // Append several copies of the first page
            doc.pagePushBack(src_page); // Note that forms are successfully copied
            doc.pagePushBack(src_page);
            doc.pagePushBack(src_page);

            // Now we rename fields in order to make every field unique.
            // You can use this technique for dynamic template filling where you have a 'master'
            // form page that should be replicated, but with unique field names on every page.
            renameAllFields(doc, "employee.name.first");
            renameAllFields(doc, "employee.name.last");
            renameAllFields(doc, "employee.name.check1");
            renameAllFields(doc, "submit");

            doc.save(Utils.createExternalFile("forms_test1_cloned.pdf").getAbsolutePath(), 0, null);
            addToFileList("forms_test1_cloned.pdf");
            doc.close();
            outputListener.println("Done example 3.");
        } catch (Exception e) {
            outputListener.println(e.getMessage());
        }

        // ----------------------------------------------------------------------------------
        // Sample:
        // Flatten all form fields in a document.
        // Note that this sample is intended to show that it is possible to flatten
        // individual fields. PDFNet provides a utility function PDFDoc.flattenAnnotations()
        // that will automatically flatten all fields.
        // ----------------------------------------------------------------------------------
        try {
            PDFDoc doc = new PDFDoc(Utils.createExternalFile("forms_test1.pdf").getAbsolutePath());
            doc.initSecurityHandler();

            // Traverse all pages
            if (true) {
                doc.flattenAnnotations();
            } else {    // Manual flattening

                for (PageIterator pitr = doc.getPageIterator(); pitr.hasNext();) {
                    Page page = (Page) (pitr.next());
                    Obj annots = page.getAnnots();
                    if (annots != null) {   // Look for all widget annotations (in reverse order)
                        for (int i = ((int) annots.size()) - 1; i >= 0; --i) {
                            if (annots.getAt(i).get("Subtype").value().getName().equals("Widget")) {
                                Field field = new Field(annots.getAt(i));
                                field.flatten(page);

                                // Another way of making a read only field is by/ modifying
                                // field's e_read_only flag:
                                // field.SetFlag(Field::e_read_only, true);
                            }
                        }
                    }
                }
            }

            doc.save(Utils.createExternalFile("forms_test1_flattened.pdf").getAbsolutePath(), 0, null);
            addToFileList("forms_test1_flattened.pdf");
            doc.close();
            outputListener.println("Done example 4.");
        } catch (Exception e) {
            outputListener.println(e.getMessage());
        }
        
        printFooter(outputListener);

    }
    
    static void renameAllFields(PDFDoc doc, String name) throws PDFNetException {
        FieldIterator itr = doc.getFieldIterator(name);
        for (int counter = 0; itr.hasNext(); itr = doc.getFieldIterator(name), ++counter) {
            Field f = (Field) (itr.next());
            f.rename(name + counter);
        }
    }
    
    static Obj createCheckmarkAppearance(PDFDoc doc) throws PDFNetException {
        // Create a checkmark appearance stream
        // ------------------------------------
        ElementBuilder build = new ElementBuilder();
        ElementWriter writer = new ElementWriter();
        writer.begin(doc);
        writer.writeElement(build.createTextBegin());
        {
            String symbol = "4"; // other options are circle ("l"), diamond // ("H"), cross ("\x35")
            // See section D.4 "ZapfDingbats Set and Encoding" in PDF Reference
            // Manual for the complete graphical map for ZapfDingbats font.
            Element checkmark = build.createTextRun(symbol, Font.create(doc, Font.e_zapf_dingbats), 1);
            writer.writeElement(checkmark);
        }
        writer.writeElement(build.createTextEnd());

        Obj stm = writer.end();

        // Set the bounding box
        stm.putRect("BBox", -0.2, -0.2, 1, 1);
        stm.putName("Subtype", "Form");
        return stm;
    }
    
    static Obj createButtonAppearance(PDFDoc doc, boolean button_down) throws PDFNetException {
        // Create a button appearance stream
        // ------------------------------------
        ElementBuilder build = new ElementBuilder();
        ElementWriter writer = new ElementWriter();
        writer.begin(doc);

        // Draw background
        Element element = build.createRect(0, 0, 101, 37);
        element.setPathFill(true);
        element.setPathStroke(false);
        element.getGState().setFillColorSpace(ColorSpace.createDeviceGray());
        element.getGState().setFillColor(new ColorPt(0.75, 0, 0));
        writer.writeElement(element);

        // Draw 'Submit' text
        writer.writeElement(build.createTextBegin());
        {
            String text = "Submit";
            element = build.createTextRun(text, Font.create(doc, Font.e_helvetica_bold), 12);
            element.getGState().setFillColor(new ColorPt(0, 0, 0));

            if (button_down)
                element.setTextMatrix(1, 0, 0, 1, 33, 10);
            else
                element.setTextMatrix(1, 0, 0, 1, 30, 13);
            writer.writeElement(element);
        }
        writer.writeElement(build.createTextEnd());

        Obj stm = writer.end();

        // Set the bounding box
        stm.putRect("BBox", 0, 0, 101, 37);
        stm.putName("Subtype", "Form");
        return stm;
    }

}
