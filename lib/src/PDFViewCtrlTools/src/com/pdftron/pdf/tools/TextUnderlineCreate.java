//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Underline;
import com.pdftron.pdf.tools.ToolManager.ToolMode;

/**
 * This class is for creating text underline annotation.
 */
@Keep
public class TextUnderlineCreate extends TextMarkupCreate {

    /**
     * Class constructor
     */
    public TextUnderlineCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.TEXT_UNDERLINE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Underline;
    }

    /**
     * The overload implementation of {@link TextMarkupCreate#createMarkup(PDFDoc, Rect)}.
     */
    @Override
    protected Annot createMarkup(PDFDoc doc, Rect bbox) throws PDFNetException {
        return Underline.create(doc, bbox);
    }
}
