//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.tools.R;

/**
 * A dialog that lets user select the print options including document, annotations and summary of annotations
 */
public class PrintAnnotationsSummaryDialogFragment extends DialogFragment {

    public static final String EXTRA_PRINT_DOCUMENT_CHECKED = "PrintAnnotationsSummaryDialogFragment.DOCUMENT_CHECKED";
    public static final String EXTRA_PRINT_ANNOTATIONS_CHECKED = "PrintAnnotationsSummaryDialogFragment.ANNOTATIONS_CHECKED";
    public static final String EXTRA_PRINT_SUMMARY_CHECKED = "PrintAnnotationsSummaryDialogFragment.SUMMARY_CHECKED";

    private static final String DOCUMENT_CHECKED = "document_checked";
    private static final String ANNOTATIONS_CHECKED = "annotations_checked";
    private static final String SUMMARY_CHECKED = "summary_checked";

    boolean mDocumentChecked, mAnnotationsChecked, mSummaryChecked;

    LinearLayout mLinearLayoutRoot = null;
    LinearLayout mLinearLayoutDocument = null;
    LinearLayout mLinearLayoutAnnotations = null;
    LinearLayout mLinearLayoutSummary = null;

    Button mDialogButton = null;
    AlertDialog mDialog = null;
    CheckBox mCheckBoxAnnots = null;

    /**
     * Returns a new instance of the class
     *
     * @param documentChecked True if the document should be printed
     * @param annotationsChecked True if the annotations on the document should be printed
     * @param summaryChecked True if the summary of annotations should be printed
     *
     * @return The new instance
     */
    public static PrintAnnotationsSummaryDialogFragment newInstance(boolean documentChecked,
                                                                    boolean annotationsChecked,
                                                                    boolean summaryChecked) {
        PrintAnnotationsSummaryDialogFragment fragment = new PrintAnnotationsSummaryDialogFragment();

        Bundle args = new Bundle();
        args.putBoolean(DOCUMENT_CHECKED, documentChecked);
        args.putBoolean(ANNOTATIONS_CHECKED, annotationsChecked);
        args.putBoolean(SUMMARY_CHECKED, summaryChecked);

        fragment.setArguments(args);

        return fragment;
    }

    /**
     * The overload implementation of {@link DialogFragment#onCreateDialog(Bundle)}.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mDocumentChecked = getArguments().getBoolean(DOCUMENT_CHECKED, true);
        mAnnotationsChecked = getArguments().getBoolean(ANNOTATIONS_CHECKED, true);
        mSummaryChecked = getArguments().getBoolean(SUMMARY_CHECKED, false);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_print_annotations_summary, null);
        builder.setView(view);

        mLinearLayoutRoot = view.findViewById(R.id.dialog_print_annotations_summary_root_view);
        mLinearLayoutDocument = view.findViewById(R.id.document_content_view);
        mLinearLayoutAnnotations = view.findViewById(R.id.annotations_content_view);
        mLinearLayoutSummary = view.findViewById(R.id.summary_content_view);

        ViewGroup.LayoutParams lp = mLinearLayoutDocument.getLayoutParams();
        if (mDocumentChecked) {
            lp.height = (int) getActivity().getResources().getDimension(R.dimen.print_annotations_summary_dist_document_annotations);
            mLinearLayoutAnnotations.setVisibility(View.VISIBLE);
        } else {
            lp.height = (int) getActivity().getResources().getDimension(R.dimen.print_annotations_summary_dist_annotations_summary);
            mLinearLayoutAnnotations.setVisibility(View.GONE);
        }

        lp = mLinearLayoutAnnotations.getLayoutParams();
        lp.height = (int) getActivity().getResources().getDimension(R.dimen.print_annotations_summary_dist_annotations_summary);

        lp = mLinearLayoutSummary.getLayoutParams();
        if (mDocumentChecked) {
            lp.height = (int) getActivity().getResources().getDimension(R.dimen.print_annotations_summary_dist_document_annotations);
        } else {
            lp.height = 2 * (int) getActivity().getResources().getDimension(R.dimen.print_annotations_summary_dist_document_annotations);
        }

        CheckBox checkBoxDocument = view.findViewById(R.id.checkBoxDocument);
        checkBoxDocument.setChecked(mDocumentChecked);
        checkBoxDocument.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDocumentChecked = !mDocumentChecked;
                updateWidgets();
            }
        });

        mCheckBoxAnnots = view.findViewById(R.id.checkBoxAnnots);
        mCheckBoxAnnots.setChecked(mAnnotationsChecked);
        mCheckBoxAnnots.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAnnotationsChecked = !mAnnotationsChecked;
                updateWidgets();
            }
        });

        CheckBox checkBoxSummary = view.findViewById(R.id.checkBoxSummary);
        checkBoxSummary.setChecked(mSummaryChecked);
        checkBoxSummary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSummaryChecked = !mSummaryChecked;
                updateWidgets();
            }
        });

        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendResult(Activity.RESULT_OK);
                    }
                });
        builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                });

        builder.setTitle(R.string.dialog_print_annotations_summary_title);

        mDialog = builder.create();
        return mDialog;
    }

    /**
     * The overload implementation of {@link DialogFragment#onStart()}.
     */
    @Override
    public void onStart() {
        super.onStart();
        mDialogButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);

        if (Utils.isTablet(getActivity())) {
            // a hack for shrinking the dialog's width
            final int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            Window window = mDialog.getWindow();
            if (window == null) {
                return;
            }
            View view = window.peekDecorView();
            if (view != null) {
                view.measure(spec, spec);
                int width = 3 * view.getMeasuredWidth() / 2;

                Display display = getActivity().getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int widthScreen = size.x;

                if (width < 2 * widthScreen / 3) {
                    mDialog.getWindow().setLayout(width, LinearLayout.LayoutParams.WRAP_CONTENT);
                }
            }
        }

        updateWidgets();
    }

    private void sendResult(int resultCode) {
        if (getTargetFragment() == null) {
            return;
        }

        Intent intent = new Intent();
        intent.putExtra(EXTRA_PRINT_DOCUMENT_CHECKED, mDocumentChecked);
        intent.putExtra(EXTRA_PRINT_ANNOTATIONS_CHECKED, mAnnotationsChecked);
        intent.putExtra(EXTRA_PRINT_SUMMARY_CHECKED, mSummaryChecked);

        getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, intent);
    }

    private void updateWidgets() {
        if (mDialogButton != null) {
            if (!mSummaryChecked && !mDocumentChecked) {
                mDialogButton.setEnabled(false);
            } else {
                mDialogButton.setEnabled(true);
            }
        }

        ViewGroup.LayoutParams lpDocument = mLinearLayoutDocument.getLayoutParams();
        ViewGroup.LayoutParams lpSummary = mLinearLayoutSummary.getLayoutParams();
        if (mDocumentChecked) {
            lpDocument.height = (int) getActivity().getResources().getDimension(R.dimen.print_annotations_summary_dist_document_annotations);
            lpSummary.height = (int) getActivity().getResources().getDimension(R.dimen.print_annotations_summary_dist_document_annotations);
            mLinearLayoutAnnotations.setVisibility(View.VISIBLE);
        } else {
            lpDocument.height = (int) getActivity().getResources().getDimension(R.dimen.print_annotations_summary_dist_annotations_summary);
            lpSummary.height = 2 * (int) getActivity().getResources().getDimension(R.dimen.print_annotations_summary_dist_document_annotations);
            mLinearLayoutAnnotations.setVisibility(View.GONE);
        }

        if (mDocumentChecked && mSummaryChecked) {
            mCheckBoxAnnots.setChecked(true);
            mCheckBoxAnnots.setEnabled(false);
        } else {
            mCheckBoxAnnots.setChecked(mAnnotationsChecked);
            mCheckBoxAnnots.setEnabled(true);
        }
    }
}
