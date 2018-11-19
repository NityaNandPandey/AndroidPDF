//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.LinearLayout;

/**
 * Dialog of given option when searching text
 */
@SuppressWarnings("WeakerAccess")
public class DialogTextSearchOption extends AlertDialog {

    private CheckBox mWholeWord;
    private CheckBox mCaseSensitive;
    private CheckBox mUseReg;

    /**
     * Class constructor
     */
    public DialogTextSearchOption(@NonNull Context context) {
        super(context);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.tools_dialog_textsearch, null);

        mCaseSensitive = (CheckBox) layout.findViewById(R.id.tools_dialog_textsearch_case_sensitive);
        mWholeWord = (CheckBox) layout.findViewById(R.id.tools_dialog_textsearch_wholeword);
        mUseReg = (CheckBox) layout.findViewById(R.id.tools_dialog_textsearch_regex);

        setTitle(context.getString(R.string.tools_dialog_textsearch_title));
        setView(layout);
    }

    /**
     * @return True if whole word is checked
     */
    public boolean getWholeWord() {
        return mWholeWord.isChecked();
    }

    /**
     * @return True if case sensitive is checked
     */
    public boolean getCaseSensitive() {
        return mCaseSensitive.isChecked();
    }

    /**
     * @return True if regular expressions is checked
     */
    public boolean getRegExps() {
        return mUseReg.isChecked();
    }

    /**
     * Sets if whole word should be used.
     *
     * @param flag True if whole word should be used
     */
    public void setWholeWord(boolean flag) {
        mWholeWord.setChecked(flag);
    }

    /**
     * Sets if case sensitive should be used.
     *
     * @param flag True if case sensitive should be used
     */
    public void setCaseSensitive(boolean flag) {
        mCaseSensitive.setChecked(flag);
    }

    /**
     * Sets if regular expressions should be used.
     *
     * @param flag True if regular expressions should be used
     */
    public void setRegExps(boolean flag) {
        mUseReg.setChecked(flag);
    }
}
