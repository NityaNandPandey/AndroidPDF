//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Square;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.DrawingUtils;

/**
 * This class is for creating a rectangle annotation.
 */
@Keep
public class RectCreate extends SimpleShapeCreate {

    /**
     * Class constructor
     */
    public RectCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();
        mHasFill = true;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.RECT_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Square;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#createMarkup(PDFDoc, Rect)}.
     */
    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        return Square.create(doc, bbox);
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw (Canvas canvas, Matrix tfm) {
        // We are scrolling
        if (mAllowTwoFingerScroll) {
            return;
        }

        DrawingUtils.drawRectangle(canvas,
                mPt1, mPt2,
                mThicknessDraw,
                mFillColor, mStrokeColor,
                mFillPaint, mPaint);
    }
}
