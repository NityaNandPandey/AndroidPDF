//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.tools.ToolManager.ToolMode;

import java.util.UUID;

/**
 * This class is for creating a signature field annotation
 */
@Keep
public class SignatureFieldCreate extends RectCreate {

    /**
     * Class constructor
     */
    public SignatureFieldCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.FORM_SIGNATURE_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Widget;
    }

    /**
     * The overload implementation of {@link RectCreate#createMarkup(PDFDoc, Rect)}.
     */
    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        String fieldName = UUID.randomUUID().toString();
        Annot annot =  Widget.create(doc, bbox, doc.fieldCreate(fieldName, Field.e_signature));
        annot.getSDFObj().putString(PDFTRON_ID, "");
        return annot;
    }
}
