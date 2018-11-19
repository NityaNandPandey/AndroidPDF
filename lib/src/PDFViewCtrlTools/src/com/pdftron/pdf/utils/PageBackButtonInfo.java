//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import com.pdftron.pdf.PDFViewCtrl;

/**
 * A structure which has all information needed to go back/forward to the previous/next page
 */
public class PageBackButtonInfo {
    /**
     * Horizontal scrolling position
     */
    public int hScrollPos;
    /**
     * Vertical scrolling position
     */
    public int vScrollPos;
    /**
     * Zoom level
     */
    public double zoom;
    /**
     * Page number
     */
    public int pageNum;
    /**
     * Page rotation
     */
    public int pageRotation;
    /**
     * Page presentation mode
     */
    public PDFViewCtrl.PagePresentationMode pagePresentationMode;

    public PageBackButtonInfo() {
        pageNum = -1;
    }

    /**
     * Copy page info from the other page info
     * @param pageStateToCopy other page info to copy
     */
    public void copyPageInfo(PageBackButtonInfo pageStateToCopy) {
        this.pageNum = pageStateToCopy.pageNum;
        this.hScrollPos = pageStateToCopy.hScrollPos;
        this.vScrollPos = pageStateToCopy.vScrollPos;
        this.zoom = pageStateToCopy.zoom;
        this.pageNum = pageStateToCopy.pageNum;
        this.pageRotation = pageStateToCopy.pageRotation;
        this.pagePresentationMode = pageStateToCopy.pagePresentationMode;
    }
}
