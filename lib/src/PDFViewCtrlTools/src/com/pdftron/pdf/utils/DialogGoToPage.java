//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.PdfViewCtrlTabFragment;
import com.pdftron.pdf.controls.ReflowControl;
import com.pdftron.pdf.tools.R;

/**
 * A dialog for going to a specific page.
 */
public class DialogGoToPage {
    private PdfViewCtrlTabFragment pdfViewCtrlTabFragment;
    private EditText mTextBox;
    private PDFViewCtrl mPdfViewCtrl;
    private ReflowControl mReflowControl;
    private Context mContext;
    private String mHint;
    private int mPageCount;

    private boolean mUsingLabel;

    public DialogGoToPage(@NonNull PdfViewCtrlTabFragment pdfViewCtrlTabFragment, @NonNull Context context, @NonNull PDFViewCtrl ctrl, ReflowControl reflowControl) {
        this.pdfViewCtrlTabFragment = pdfViewCtrlTabFragment;
        mPdfViewCtrl = ctrl;
        mContext = context;
        mReflowControl = reflowControl;
        mPageCount = 0;
        mHint = "";
        PDFDoc doc = mPdfViewCtrl.getDoc();
        if (doc != null) {
            boolean shouldUnlockRead = false;
            try {
                doc.lockRead();
                shouldUnlockRead = true;
                mPageCount = doc.getPageCount();
                if (mPageCount > 0) {
                    mHint = String.format(pdfViewCtrlTabFragment.getResources().getString(R.string.dialog_gotopage_number), 1, mPageCount);
                    String firstPage = ViewerUtils.getPageLabelTitle(mPdfViewCtrl, 1);
                    String lastPage = ViewerUtils.getPageLabelTitle(mPdfViewCtrl, mPageCount);
                    if (!Utils.isNullOrEmpty(firstPage) && !Utils.isNullOrEmpty(lastPage)) {
                        mHint = String.format(pdfViewCtrlTabFragment.getResources().getString(R.string.dialog_gotopage_label), firstPage, lastPage);
                        mUsingLabel = true;
                    }
                }
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(doc);
                }
            }
        }
    }

    /**
     * Shows the dialog.
     */
    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(pdfViewCtrlTabFragment.getResources().getString(R.string.dialog_gotopage_title));
        mTextBox = new EditText(mContext);
        if (mPageCount > 0) {
            mTextBox.setHint(mHint);
        }
        if (!mUsingLabel) {
            mTextBox.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
        mTextBox.setImeOptions(EditorInfo.IME_ACTION_GO);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-1, -1);
        mTextBox.setLayoutParams(layoutParams);
        FrameLayout layout = new FrameLayout(mPdfViewCtrl.getContext());
        layout.addView(mTextBox);
        int topPadding = mContext.getResources().getDimensionPixelSize(R.dimen.alert_dialog_top_padding);
        int sidePadding = mContext.getResources().getDimensionPixelSize(R.dimen.alert_dialog_side_padding);
        layout.setPadding(sidePadding, topPadding, sidePadding, topPadding);
        builder.setView(layout);

        builder.setPositiveButton(pdfViewCtrlTabFragment.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing here because we override this button later
                // to change the close behaviour. However, we still need
                // this because on older versions of Android unless we pass
                // a handler the button doesn't get instantiated.
            }
        });
        builder.setNegativeButton(pdfViewCtrlTabFragment.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goToPage(dialog);
                }
            });

        mTextBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // If enter key was pressed, then submit password
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    goToPage(dialog);
                    return true;
                }
                return false;
            }
        });
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private void goToPage(AlertDialog dialog) {
        if (mPdfViewCtrl == null) {
            return;
        }

        String text = mTextBox.getText().toString();
        int pageNum;
        try {
            pageNum = Integer.parseInt(text);
        } catch (NumberFormatException nfe) {
            pageNum = 0;
        }
        int pageLabelNum = ViewerUtils.getPageNumberFromLabel(mPdfViewCtrl, text);
        if (pageLabelNum > 0) {
            pageNum = pageLabelNum;
        } else if (pageNum > 0) {
            // try if it is still a valid page number greater than the page count
            try {
                String lastPage = ViewerUtils.getPageLabelTitle(mPdfViewCtrl, mPageCount);
                if (!Utils.isNullOrEmpty(lastPage)) {
                    int lastPageNum = Integer.parseInt(lastPage);
                    if (pageNum > lastPageNum) {
                        pageNum = mPageCount;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (pageNum > 0 && pageNum <= mPageCount) {
            pdfViewCtrlTabFragment.setCurrentPageHelper(pageNum, true);
            if (mReflowControl != null) {
                try {
                    mReflowControl.setCurrentPage(pageNum);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }

            if (null != dialog) {
                dialog.dismiss();
            }
        } else {
            mTextBox.setText("");
        }
    }
}
