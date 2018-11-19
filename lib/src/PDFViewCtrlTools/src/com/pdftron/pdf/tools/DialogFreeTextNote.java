//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.utils.Utils;

/**
 *  The DialogFreeTextNote is used for adding and editing free text.
 */
@SuppressWarnings("WeakerAccess")
public class DialogFreeTextNote extends DialogAnnotNote {

    private ImageButton mToggleButton;

    /**
     * Class constructor
     */

    public DialogFreeTextNote(@NonNull PDFViewCtrl pdfViewCtrl, String note, boolean enablePositiveButton) {
        super(pdfViewCtrl, note);

        mPositiveButton.setEnabled(enablePositiveButton);
    }

    /**
     * The overload implementation of {@link DialogAnnotNote#initTextBox(String)}.
     */
    @Override
    public void initTextBox(String note) {
        if (!note.equals("")) {
            mTextBox.setText(note);
            // Set the caret position to the end of the text
            mTextBox.setSelection(mTextBox.getText().length());
        }

        mTextBox.addTextChangedListener(textWatcher);
        mTextBox.setHint(getContext().getString(R.string.tools_dialog_annotation_popup_text_hint));

        // Add button which toggles between the style view and the text view
        mToggleButton = mMainView.findViewById(R.id.tools_dialog_annotation_popup_button_style);
        mToggleButton.setVisibility(View.VISIBLE);
        mToggleButton.setImageResource(R.drawable.ic_call_received_black_24dp);
        mToggleButton.getDrawable().mutate().setColorFilter(Utils.getAccentColor(getContext()), PorterDuff.Mode.SRC_ATOP);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonPressed(BUTTON_NEUTRAL);
            }
        });
    }

    /**
     * Disables the toggle button.
     */
    public void disableToggleButton() {
        mToggleButton.setEnabled(false);
        mToggleButton.getDrawable().mutate().setColorFilter(ContextCompat.getColor(getContext(), R.color.tools_gray), PorterDuff.Mode.SRC_ATOP);
    }

    /**
     * The overload implementation of {@link DialogAnnotNote#onStop()}.
     */
    @Override
    protected void onStop() {
        super.onStop();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mTextBox.getWindowToken(), 0);
        }
    }

    /**
     * Adds a text watcher.
     *
     * @param textWatcherListener The text watcher
     */
    @SuppressWarnings("WeakerAccess")
    public void addTextWatcher(TextWatcher textWatcherListener){
        mTextBox.addTextChangedListener(textWatcherListener);
    }
}
