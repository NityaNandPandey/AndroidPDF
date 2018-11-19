//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.pdftron.demo.R;
import com.pdftron.pdf.utils.Utils;

public class ImportWebpageUrlSelectorDialogFragment extends DialogFragment {

    @SuppressWarnings("unused")
    private static final String TAG = ImportWebpageUrlSelectorDialogFragment.class.getName();

    private AlertDialog mDialog;
    private EditText mUrlEditText;

    private OnLinkSelectedListener mOnLinkSelectedListener;

    public interface OnLinkSelectedListener {
        void linkSelected(String link);
    }

    public static ImportWebpageUrlSelectorDialogFragment newInstance() {
        return new ImportWebpageUrlSelectorDialogFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_import_webpage_url_selector, null);
        builder.setView(view);

        builder.setPositiveButton(R.string.dialog_webpage_pdf_convert,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mOnLinkSelectedListener != null) {
                        mOnLinkSelectedListener.linkSelected(mUrlEditText.getText().toString());
                    }
                }
            });
        builder.setNegativeButton(R.string.cancel,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });

        builder.setTitle(R.string.dialog_webpage_pdf_title);

        mUrlEditText = (EditText) view.findViewById(R.id.webpage_url);
        mUrlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                String link = s.toString();
                if (link.length() > 0
                    && !Utils.isNullOrEmpty(link)
                    && !link.contains(" ")
                    && Patterns.WEB_URL.matcher(link).matches()) {
                    mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                } else {
                    mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mDialog = builder.create();
        return mDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mDialog != null) {
            mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    public void setOnLinkSelectedListener(OnLinkSelectedListener listener) {
        mOnLinkSelectedListener = listener;
    }
}
