//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

/**
 * A dialog about picking number
 */
@TargetApi(11)
public class DialogNumberPicker extends AlertDialog {

    private NumberPicker mInteger;
    private NumberPicker mDecimal;

    public DialogNumberPicker(Context context, float val) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.tools_dialog_numberpicker, null);
        mInteger = (NumberPicker) view.findViewById(R.id.tools_dialog_numberpicker_integer);
        mDecimal = (NumberPicker) view.findViewById(R.id.tools_dialog_numberpicker_decimal);

        mInteger.setMinValue(0);
        mInteger.setMaxValue(50);
        mInteger.setOnLongPressUpdateInterval(50);
        mInteger.setValue((int)val);

        mDecimal.setMinValue(0);
        mDecimal.setMaxValue(9);
        val -= (int) val;
        val = (float) Math.floor(val*10 + 0.5f);
        mDecimal.setValue((int)val);

        // To avoid soft keyboard
        //mInteger.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        //mDecimal.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        setView(view);
    }

    public float getNumber() {
        float v = mInteger.getValue() + ((float)mDecimal.getValue()) / 10;
        return v;
    }
}
