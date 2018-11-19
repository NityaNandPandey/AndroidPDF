//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Line;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.DrawingUtils;

/**
 * This class is for creating a line annotation.
 */
@Keep
public class LineCreate extends SimpleShapeCreate {

    /**
     * Class constructor
     */
    public LineCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.LINE_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Line;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#createMarkup(PDFDoc, Rect)}.
     */
    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        return Line.create(doc, bbox);
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        if (mAllowTwoFingerScroll) {
            return;
        }
        if (mIsAllPointsOutsidePage) {
            return;
        }
        DrawingUtils.drawLine(canvas, mPt1, mPt2, mPaint);
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#getDefaultNextTool()}.
     */
    @Override
    protected ToolMode getDefaultNextTool() {
        return ToolMode.ANNOT_EDIT_LINE;
    }
}
