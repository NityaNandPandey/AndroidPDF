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
import com.pdftron.pdf.annots.Squiggly;
import com.pdftron.pdf.tools.ToolManager.ToolMode;

/**
 * This class is for creating text squiggly annotation.
 */
@Keep
public class TextSquigglyCreate extends TextMarkupCreate {

    /**
     * Class constructor
     */
    public TextSquigglyCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.TEXT_SQUIGGLY;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Squiggly;
    }

    /**
     * The overload implementation of {@link TextMarkupCreate#createMarkup(PDFDoc, Rect)}.
     */
    @Override
    protected Annot createMarkup(PDFDoc doc, Rect bbox) throws PDFNetException {
        return Squiggly.create(doc, bbox);
    }
}
