//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.interfaces;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pdftron.sdf.Obj;

/**
 * Callback interface to be invoked when either a standard rubber stamp or custom rubber stamp has been selected.
 */
public interface OnRubberStampSelectedListener {
    /**
     * Called when a standard rubber stamp is selected.
     *
     * @param stampLabel The label of stamp
     */
    void onRubberStampSelected(@NonNull String stampLabel);

    /**
     * Called when a custom rubber stamp is selected.
     *
     * @param stampObj The option for creating custom rubber stamp
     */
    void onRubberStampSelected(@Nullable Obj stampObj);

}
