//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.RectF;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;

import java.util.HashMap;

/**
 * This tool is for creating link annotation by selected text
 */
@Keep
public class TextLinkCreate extends TextMarkupCreate {
    private SparseArray<RectF> mPageSelectedRects;

    /**
     * Class constructor
     */
    public TextLinkCreate(PDFViewCtrl ctrl) {
        super(ctrl);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.TEXT_LINK_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Link;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (mPageSelectedRects != null) {
            mPageSelectedRects.clear();
        }
        return super.onDown(e);
    }

    @Override
    protected Annot createMarkup(PDFDoc doc, Rect bbox) throws PDFNetException {
        return null;
    }

    @Override
    protected void setAnnotRect(@Nullable Annot annot, Rect rect, int pageNum) throws PDFNetException {
        RectF pageRect = new RectF();
        if (mPageSelectedRects != null && mPageSelectedRects.indexOfKey(pageNum) > -1) {
            pageRect = mPageSelectedRects.get(pageNum);
        } else if(mPageSelectedRects == null) {
            mPageSelectedRects = new SparseArray<>();
        }
        pageRect.union((float) rect.getX1(), (float) rect.getY1(), (float) rect.getX2(), (float) rect.getY2());
        mPageSelectedRects.put(pageNum, pageRect);
    }

    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        super.onQuickMenuClicked(menuItem);
            if (mPageSelectedRects == null || mPageSelectedRects.size() == 0) {
                return true;
            }
            HashMap<Rect, Integer> selRect = new HashMap<>();
            try {
                for (int i = 0; i < mPageSelectedRects.size(); i++) {
                    int pageNum = mPageSelectedRects.keyAt(i);
                    RectF pageRect = mPageSelectedRects.get(pageNum);
                    selRect.put(new Rect((double) pageRect.left, (double)pageRect.top, (double)pageRect.right, (double)pageRect.bottom), pageNum);
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
            DialogLinkEditor linkEditor = new DialogLinkEditor(mPdfViewCtrl, this, selRect);
            linkEditor.setColor(mColor);
            linkEditor.setThickness(mThickness);
            linkEditor.show();

        mNextToolMode = ToolMode.PAN;
        return true;
    }
}
