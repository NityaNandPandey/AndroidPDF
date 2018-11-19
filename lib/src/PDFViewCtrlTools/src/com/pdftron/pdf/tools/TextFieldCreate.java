
//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------


package com.pdftron.pdf.tools;

import android.graphics.Color;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.ColorSpace;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;

import java.util.UUID;

/**
 * This class is for creating multiline text field
 */
@Keep
public class TextFieldCreate extends RectCreate {

    private boolean mIsMultiline;
    private int mJustification;

    /**
     * Class constructor
     */
    public TextFieldCreate(PDFViewCtrl ctrl) {
        this(ctrl, true, Field.e_left_justified);
    }

    /**
     * Class constructor
     */
    public TextFieldCreate(PDFViewCtrl ctrl, boolean isMultiline, int justification) {
        super(ctrl);
        mIsMultiline = isMultiline;
        mJustification = justification;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.FORM_TEXT_FIELD_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Widget;
    }

    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        String fieldName = UUID.randomUUID().toString();
        Field field = doc.fieldCreate(fieldName, Field.e_text);
        Widget annot = Widget.create(doc, bbox,field);
        ColorPt colorPt = Utils.color2ColorPt(Color.WHITE);
        annot.setBackgroundColor(colorPt, ColorSpace.e_device_rgb);
        field.setFlag(Field.e_multiline, mIsMultiline);
        field.setJustification(mJustification);
        annot.getSDFObj().putString(PDFTRON_ID, "");
        // change font size
        // https://groups.google.com/forum/#!msg/pdfnet-sdk/FWmdwfup8c8/cVWfN9AwDQAJ
        Font font = Font.create(doc, Font.e_helvetica, false);
        Obj acroForm = doc.getAcroForm();
        if (null == acroForm) {
            acroForm = doc.getRoot().putDict("AcroForm");
        }
        Obj dr = acroForm.findObj("DR");
        if (null == dr) {
            dr = acroForm.putDict("DR");
        }
        Obj fonts = dr.findObj("Font");
        if (null == fonts) {
            fonts = dr.putDict("Font");
        }
        fonts.put("Helv", font.GetSDFObj());
        annot.getSDFObj().putString("DA", "/Helv 16 Tf 0 g");

        return annot;
    }
}
