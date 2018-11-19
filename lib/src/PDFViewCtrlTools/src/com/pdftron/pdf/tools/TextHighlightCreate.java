//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.SharedPreferences;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Highlight;
import com.pdftron.pdf.tools.ToolManager.ToolMode;

/**
 * This class is for creating text highlight annotation.
 */
@Keep
public class TextHighlightCreate extends TextMarkupCreate {

    /**
     * Class constructor
     */
    public TextHighlightCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.TEXT_HIGHLIGHT;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Highlight;
    }

    /**
     * The overload implementation of {@link TextMarkupCreate#createMarkup(PDFDoc, Rect)}.
     */
    @Override
    protected Annot createMarkup(PDFDoc doc, Rect bbox) throws PDFNetException {
        return Highlight.create(doc, bbox);
    }

    /**
     * The overload implementation of {@link TextMarkupCreate#setupAnnotProperty(int, float, float, int, String, String)}.
     */
    @Override
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName) {
        mColor = color;
        mOpacity = opacity;
        mThickness = thickness;

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt(getColorKey(getCreateAnnotType()), mColor);
        editor.putFloat(getOpacityKey(getCreateAnnotType()), mOpacity);

        editor.apply();
    }
}
