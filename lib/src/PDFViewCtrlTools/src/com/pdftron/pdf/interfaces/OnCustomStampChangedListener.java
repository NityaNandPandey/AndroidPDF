//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.interfaces;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;

/**
 * Callback interface to be invoked when a new custom rubber stamp has been created,
 * or an existing custom rubber stamp has been updated.
 */
public interface OnCustomStampChangedListener {
    /**
     * Called when a new stamp is created.
     *
     * @param bitmap The generated bitmap
     */
    void onCustomStampCreated(@Nullable Bitmap bitmap);
    /**
     * Called when a custom rubber stamp is updated.
     *
     * @param bitmap The generated bitmap
     * @param index The index where the custom rubber stamp is updated
     */
    void onCustomStampUpdated(@Nullable Bitmap bitmap, int index);
}
