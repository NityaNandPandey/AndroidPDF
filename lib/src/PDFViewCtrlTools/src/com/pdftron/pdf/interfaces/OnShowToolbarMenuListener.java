//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.interfaces;

/**
 * Callback interface to be invoked when the toolbar menu should be shown/hidden.
 */
public interface OnShowToolbarMenuListener {
    /**
     * Called when the toolbar menu should be shown/hidden
     *
     * @param visible True if toolbar menu should be shown; False otherwise
     */
    void onShowToolbarMenu(boolean visible);
}
