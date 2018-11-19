package com.pdftron.demo.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;

import com.pdftron.demo.R;
import com.pdftron.pdf.utils.Utils;

/**
 * A dialog shown when open PDF from link is selected.
 */
public class DialogOpenUrl extends AlertDialog implements
    DialogInterface.OnClickListener {

    public interface DialogOpenUrlListener {
        void onSubmit(String url);
    }

    private Button mPosButton;
    private EditText mUrlEditText;
    private DialogOpenUrlListener mDialogOpenUrlListener;

    public DialogOpenUrl(@NonNull Context context, DialogOpenUrlListener listener) {
        super(context);

        mDialogOpenUrlListener = listener;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_open_url, null);
        mUrlEditText = (EditText) view.findViewById(R.id.edit_text);
        mUrlEditText.addTextChangedListener(new UrlChangeListener());

        setView(view);

        // Add two button
        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(com.pdftron.pdf.tools.R.string.ok), this);

        setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(com.pdftron.pdf.tools.R.string.cancel), this);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == DialogInterface.BUTTON_POSITIVE) {
            if (mDialogOpenUrlListener != null) {
                String link = mUrlEditText.getText().toString();
                if (!Utils.isNullOrEmpty(link)) {
                    link = URLUtil.guessUrl(link);
                    mDialogOpenUrlListener.onSubmit(link);
                }
            }
            dismiss();
        } else if (i == DialogInterface.BUTTON_NEGATIVE) {
            dismiss();
        }
    }

    @Override
    public void show() {
        super.show();
        mPosButton = getButton(DialogInterface.BUTTON_POSITIVE);
        mPosButton.setEnabled(false);
    }

    private class UrlChangeListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mPosButton == null) {
                return;
            }
            if (Utils.isNullOrEmpty(s.toString())) {
                mPosButton.setEnabled(false);
                return;
            }
            mPosButton.setEnabled(true);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }
}
