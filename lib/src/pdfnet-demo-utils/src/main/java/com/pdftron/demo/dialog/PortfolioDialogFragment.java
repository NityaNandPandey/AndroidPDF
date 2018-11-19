//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.pdftron.demo.R;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A dialog that lists all files included in a PDF portfolio.
 */
public class PortfolioDialogFragment extends DialogFragment {

    public interface PortfolioDialogFragmentListener {
        void onPortfolioDialogFragmentFileClicked(int fileType, PortfolioDialogFragment dialog, String fileName);
    }

    private ArrayList<String> mEmbeddedFiles;
    private int mFileType;
    private File mFile;
    private Uri mFileUri;
    private PDFDoc mPdfDoc;

    PortfolioDialogFragmentListener mListener;

    public static final int FILE_TYPE_FILE = 0;
    public static final int FILE_TYPE_FILE_URI = 1;
    public static final int FILE_TYPE_PDFDOC = 2;

    private static final String KEY_FILE = "key_file";
    private static final String KEY_FILE_URI = "key_file_uri";
    private static final String KEY_PASS = "key_pass";
    private static final String KEY_FILE_TYPE = "key_file_type";
    private static final String KEY_DIALOG_TITLE = "key_dialog_title";

    public PortfolioDialogFragment() {
    }

    public static PortfolioDialogFragment newInstance(File file, String password, int dialogTitleRes) {
        PortfolioDialogFragment fragment = new PortfolioDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_FILE, file);
        bundle.putString(KEY_PASS, password);
        bundle.putInt(KEY_FILE_TYPE, FILE_TYPE_FILE);
        if (dialogTitleRes != 0) {
            bundle.putInt(KEY_DIALOG_TITLE, dialogTitleRes);
        }
        fragment.setArguments(bundle);

        return fragment;
    }

    public static PortfolioDialogFragment newInstance(File file, String password) {
        return newInstance(file, password, 0);
    }

    public static PortfolioDialogFragment newInstance(Uri fileUri, String password, int dialogTitleRes) {
        PortfolioDialogFragment fragment = new PortfolioDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_FILE_URI, fileUri.toString());
        bundle.putString(KEY_PASS, password);
        bundle.putInt(KEY_FILE_TYPE, FILE_TYPE_FILE_URI);
        if (dialogTitleRes != 0) {
            bundle.putInt(KEY_DIALOG_TITLE, dialogTitleRes);
        }
        fragment.setArguments(bundle);

        return fragment;
    }

    public static PortfolioDialogFragment newInstance(Uri fileUri, String password) {
        return newInstance(fileUri, password, 0);
    }

    public static PortfolioDialogFragment newInstance(int fileType, int dialogTitleRes) {
        PortfolioDialogFragment fragment = new PortfolioDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_FILE_TYPE, fileType);
        if (dialogTitleRes != 0) {
            bundle.putInt(KEY_DIALOG_TITLE, dialogTitleRes);
        }
        fragment.setArguments(bundle);

        return fragment;
    }

    public static PortfolioDialogFragment newInstance(int fileType) {
        return newInstance(fileType, 0);
    }

    public File getPortfolioFile() {
        return mFile;
    }

    public Uri getPortfolioFileUri() {
        return mFileUri;
    }

    public void setListener(PortfolioDialogFragmentListener listener) {
        mListener = listener;
    }

    public void initParams(PDFDoc pdfDoc) {
        mPdfDoc = pdfDoc;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Initialize member variables from the arguments
        Bundle bundle = getArguments();
        int dialogTitle = R.string.pdf_portfolio;
        if (bundle != null) {
            mFileType = bundle.getInt(KEY_FILE_TYPE);
            boolean success = true;
            if (mFileType == FILE_TYPE_FILE) {
                mFile = (File) getArguments().getSerializable(KEY_FILE);
            } else if (mFileType == FILE_TYPE_FILE_URI) {
                String fileUriStr = getArguments().getString(KEY_FILE_URI);
                if (!Utils.isNullOrEmpty(fileUriStr)) {
                    try {
                        mFileUri = Uri.parse(fileUriStr);
                    } catch (Exception e) {
                        success = false;
                    }
                } else {
                    success = false;
                }
            } else if (mFileType == FILE_TYPE_PDFDOC) {
                if (mPdfDoc == null) {
                    success = false;
                }
            }
            if (!success) {
                dismiss();
            }

            String password = getArguments().getString(KEY_PASS);
            if (mFileType == FILE_TYPE_FILE) {
                mEmbeddedFiles = Utils.getFileNamesFromPortfolio(mFile, password);
            } else if (mFileType == FILE_TYPE_FILE_URI) {
                mEmbeddedFiles = Utils.getFileNamesFromPortfolio(getContext(), mFileUri, password);
            } else if (mFileType == FILE_TYPE_PDFDOC) {
                mEmbeddedFiles = Utils.getFileNamesFromPortfolio(mPdfDoc);
            }

            dialogTitle = getArguments().getInt(KEY_DIALOG_TITLE, R.string.pdf_portfolio);
        }

        // Inflate our dialog view
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_portfolio_dialog, null);
        ListView fileList = dialogView.findViewById(R.id.list);

        PortfolioEntryAdapter fileListAdapter = new PortfolioEntryAdapter(getActivity(), mEmbeddedFiles);
        fileList.setAdapter(fileListAdapter);
        fileList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mListener.onPortfolioDialogFragmentFileClicked(mFileType, PortfolioDialogFragment.this, mEmbeddedFiles.get(position));
                dismiss();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(dialogView)
            .setTitle(dialogTitle)
            .setCancelable(true)
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            if (mListener == null) {
                mListener = (PortfolioDialogFragmentListener) context;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + e.getClass().toString());
        }
    }

    private class PortfolioEntryAdapter extends ArrayAdapter<String> {

        private List<String> mEntries;

        PortfolioEntryAdapter(Context context, List<String> entries) {
            super(context, 0, entries);
            this.mEntries = entries;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_item_portfolio_list, parent, false);

                holder = new ViewHolder();
                holder.fileName = convertView.findViewById(R.id.file_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.fileName.setText(this.mEntries.get(position));

            return convertView;
        }

        private class ViewHolder {
            protected TextView fileName;
        }
    }
}
