//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

import java.io.File;

/**
 * A Dialog fragment for entering password
 */
public class PasswordDialogFragment extends DialogFragment {

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface PasswordDialogFragmentListener {
        /**
         * Called when OK button has been clicked.
         *
         * @param fileType The file type
         * @param file The file
         * @param path The file path
         * @param password The entered password
         * @param id The ID
         */
        void onPasswordDialogPositiveClick(int fileType, File file, String path, String password, String id);

        /**
         * Called when Cancel button has been clicked.
         *
         * @param fileType The file type
         * @param file The file
         * @param path The file path
         */
        void onPasswordDialogNegativeClick(int fileType, File file, String path);

        /**
         * Called when dialog is dismissed
         *
         * @param forcedDismiss True if the dialog is forced to dismiss
         */
        void onPasswordDialogDismiss(boolean forcedDismiss);
    }

    private PasswordDialogFragmentListener mListener;
    private File mFile;
    private boolean mForcedDismiss;
    private String mPath;
    private int mFileType;
    private int mMessageId = -1;
    private String mId;

    private EditText mPasswordEditText;

    private static final String KEY_FILE = "key_file";
    private static final String KEY_FILETYPE = "key_filetype";
    private static final String KEY_PATH = "key_path";
    private static final String KEY_ID = "key_id";
    private static final String KEY_HINT = "key_hint";

    /**
     * Class constructor
     */
    public PasswordDialogFragment() {

    }

    /**
     * Returns a new instance of the class.
     */
    public static PasswordDialogFragment newInstance(int fileType, File file, String path, String id) {
        PasswordDialogFragment fragment = new PasswordDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_FILE, file);
        bundle.putInt(KEY_FILETYPE, fileType);
        bundle.putString(KEY_PATH, path);
        bundle.putString(KEY_ID, id);
        fragment.setArguments(bundle);

        return fragment;
    }

    /**
     * Returns a new instance of the class.
     */
    public static PasswordDialogFragment newInstance(int fileType, File file, String path, String id, String hint) {
        PasswordDialogFragment fragment = new PasswordDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_FILE, file);
        bundle.putInt(KEY_FILETYPE, fileType);
        bundle.putString(KEY_PATH, path);
        bundle.putString(KEY_ID, id);
        bundle.putString(KEY_HINT, hint);
        fragment.setArguments(bundle);

        return fragment;
    }

    /**
     * Sets {@link PasswordDialogFragmentListener} listener
     * @param listener The listener
     */
    public void setListener(PasswordDialogFragmentListener listener) {
        mListener = listener;
    }

    /**
     * The overload implementation of {@link DialogFragment#onCreateDialog(Bundle)}.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Initialize member variables from the arguments
        mFile = (File) getArguments().getSerializable(KEY_FILE);
        mFileType = getArguments().getInt(KEY_FILETYPE);
        mPath = getArguments().getString(KEY_PATH);
        mId = getArguments().getString(KEY_ID);
        String hint = getArguments().getString(KEY_HINT);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialog = inflater.inflate(R.layout.fragment_password_dialog, null);

        mPasswordEditText = dialog.findViewById(R.id.fragment_password_dialog_password);
        if (!Utils.isNullOrEmpty(hint)) {
            mPasswordEditText.setHint(hint);
        }
        mPasswordEditText.setImeOptions(EditorInfo.IME_ACTION_GO);
        mPasswordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // If enter key was pressed, then submit password
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    onPositiveClicked();
                    return true;
                }
                return false;
            }
        });
        mPasswordEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    onPositiveClicked();
                    return true;
                }
                return false;
            }
        });

        CheckBox showPassword = dialog.findViewById(R.id.show_password);
        showPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    // show password
                    mPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    mPasswordEditText.setSelection(mPasswordEditText.getText().length());
                } else {
                    // hide password
                    mPasswordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    mPasswordEditText.setSelection(mPasswordEditText.getText().length());
                }
            }
        });

        builder.setView(dialog)
            .setTitle(R.string.dialog_password_title)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onPositiveClicked();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    PasswordDialogFragment.this.getDialog().cancel();
                    if (null != mListener) {
                        mListener.onPasswordDialogNegativeClick(mFileType, mFile, mPath);
                    }
                }
            });
        if (mMessageId != -1) {
            builder.setMessage(mMessageId);
        }

        final AlertDialog alertDialog = builder.create();

        // Show keyboard automatically when the dialog is shown.
        mPasswordEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus && alertDialog.getWindow() != null) {
                    alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }

            }
        });

        return alertDialog;
    }

    private void onPositiveClicked() {
        String password = mPasswordEditText.getText().toString().trim();
        mForcedDismiss = true;
        if (PasswordDialogFragment.this.getDialog().isShowing()) {
            PasswordDialogFragment.this.getDialog().dismiss();
        }
        if (null != mListener) {
            mListener.onPasswordDialogPositiveClick(mFileType, mFile, mPath, password, mId);
        }
    }

    /**
     * The overload implementation of {@link DialogFragment#onDismiss(DialogInterface)}.
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (null != mListener) {
            mListener.onPasswordDialogDismiss(mForcedDismiss);
            mListener = null;
        }
    }

    /**
     * The overload implementation of {@link DialogFragment#onCancel(DialogInterface)}.
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }

    /**
     * Sets the message ID
     *
     * @param messageId The message ID
     */
    public void setMessage(int messageId) {
        this.mMessageId = messageId;
    }
}
