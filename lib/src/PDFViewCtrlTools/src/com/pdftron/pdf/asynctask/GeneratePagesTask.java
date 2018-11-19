//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.asynctask;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFDocGenerator;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.controls.AddPageDialogFragment;
import com.pdftron.pdf.tools.R;

/**
 * A class that asynchronously either creates a document with empty pages or inserts empty pages into an existing document
 */
public class GeneratePagesTask extends AsyncTask<Void, Void, Boolean> {

    private static final double LINE_SHADE = 0.35;
    private static final double LINE_SHADE_BLUEPRINT = 0.85;
    private static final double MARGIN_RED = 1.0;
    private static final double MARGIN_GREEN = 0.5;
    private static final double MARGIN_BLUE = 0.5;

    private AddPageDialogFragment.PageSize mPageSize;
    private AddPageDialogFragment.PageOrientation mPageOrientation;
    private AddPageDialogFragment.PageColor mPageColor;
    private AddPageDialogFragment.PageType mPageType;
    private double mPageWidth;
    private double mPageHeight;
    private boolean mShouldCreateNewPdf;

    private int mNumOfPages;
    private String mTitle;
    private PDFDoc mFinalDoc;
    private Page[] mPages;
    private AddPageDialogFragment.OnAddNewPagesListener mOnAddNewPagesListener;
    private AddPageDialogFragment.OnCreateNewDocumentListener mOnCreateNewDocumentListener;

    private Handler mProgressDialogHandler;
    private ProgressDialog mProgressDialog;

    private Runnable mProgressDialogExecutor = new Runnable() {
        @Override
        public void run() {
            mProgressDialog.show();
        }
    };

    /**
     * Class constructor
     *
     * @param context The context
     * @param numOfPages The number of pages which should be generated
     * @param title The title of the document
     * @param pageSize The page size
     * @param pageOrientation The page orientation
     * @param pageColor The page color
     * @param pageType The type of page
     * @param pageWidth The width size of page
     * @param pageHeight The height size of page
     * @param shouldCreateNewPdf Whether should create a new PDF or not
     * @param onCreateNewDocumentListener The listener to {@link AddPageDialogFragment.OnCreateNewDocumentListener}
     * @param onAddNewPagesListener The listener to {@link AddPageDialogFragment.OnAddNewPagesListener}
     */
    public GeneratePagesTask(@NonNull Context context,
                             int numOfPages,
                             String title,
                             AddPageDialogFragment.PageSize pageSize,
                             AddPageDialogFragment.PageOrientation pageOrientation,
                             AddPageDialogFragment.PageColor pageColor,
                             AddPageDialogFragment.PageType pageType,
                             double pageWidth,
                             double pageHeight,
                             boolean shouldCreateNewPdf,
                             AddPageDialogFragment.OnCreateNewDocumentListener onCreateNewDocumentListener,
                             AddPageDialogFragment.OnAddNewPagesListener onAddNewPagesListener) {
        mNumOfPages = numOfPages;
        mTitle = title;
        mOnCreateNewDocumentListener = onCreateNewDocumentListener;
        mOnAddNewPagesListener = onAddNewPagesListener;
        mPageSize = pageSize;
        mPageColor = pageColor;
        mPageType = pageType;
        mPageWidth = pageWidth;
        mPageHeight = pageHeight;
        mShouldCreateNewPdf = shouldCreateNewPdf;
        mPageOrientation = pageOrientation;
        mPages = new Page[numOfPages];

        mProgressDialogHandler = new Handler();
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage(context.getString(R.string.tools_misc_please_wait));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                GeneratePagesTask.this.cancel(true);
                dialog.dismiss();
            }
        });
    }

    /**
     * The overloaded implementation of {@link AsyncTask#onPreExecute()}.
     **/
    @Override
    protected void onPreExecute() {
        mProgressDialogHandler.postDelayed(mProgressDialogExecutor, 790);
    }

    /**
     * The overloaded implementation of {@link android.os.AsyncTask#doInBackground(Object[])}.
     **/
    @Override
    protected Boolean doInBackground(Void... params) {
        PDFDoc doc = null;
        Page page = null;

        double width = 8.5;
        double height = 11;

        if (mPageSize == AddPageDialogFragment.PageSize.Legal) {
            height = 14;
        } else if (mPageSize == AddPageDialogFragment.PageSize.Ledger) {
            width = 11;
            height = 17;
        } else if (mPageSize == AddPageDialogFragment.PageSize.A3) {
            width = 11.69;
            height = 16.53;
        } else if (mPageSize == AddPageDialogFragment.PageSize.A4) {
            width = 8.27;
            height = 11.69;
        } else if (mPageSize == AddPageDialogFragment.PageSize.Custom) {
            width = mPageWidth;
            height = mPageHeight;
        }

        if ((mPageOrientation == AddPageDialogFragment.PageOrientation.Portrait && width > height) ||
            (mPageOrientation == AddPageDialogFragment.PageOrientation.Landscape && height > width)) {
            double temp = width;
            //noinspection SuspiciousNameCombination
            width = height;
            height = temp;
        }

        try {
            if (isCancelled()) {
                return false;
            }
            if (mShouldCreateNewPdf) {
                mFinalDoc = new PDFDoc();
            }
            double lineShade = mPageColor == AddPageDialogFragment.PageColor.Blueprint ? LINE_SHADE_BLUEPRINT : LINE_SHADE;
            double bgR = ((double) ((AddPageDialogFragment.PageColorValues[mPageColor.ordinal()] & 0x00FF0000) >> 16)) / 255.0;
            double bgG = ((double) ((AddPageDialogFragment.PageColorValues[mPageColor.ordinal()] & 0x0000FF00) >> 8)) / 255.0;
            double bgB = ((double) ((AddPageDialogFragment.PageColorValues[mPageColor.ordinal()] & 0x000000FF))) / 255.0;
            if (mPageType == AddPageDialogFragment.PageType.Grid) {
                doc = PDFDocGenerator.generateGridPaperDoc(width, height, 0.25, 0.45, lineShade, lineShade, lineShade, bgR, bgG, bgB);
            } else if (mPageType == AddPageDialogFragment.PageType.Graph) {
                doc = PDFDocGenerator.generateGraphPaperDoc(width, height, 0.25, 0.45, 1.7, 5, lineShade, lineShade, lineShade, bgR, bgG, bgB);
            } else if (mPageType == AddPageDialogFragment.PageType.Music) {
                doc = PDFDocGenerator.generateMusicPaperDoc(width, height, 0.5, 10, 6.5, 0.25, lineShade, lineShade, lineShade, bgR, bgG, bgB);
            } else if (mPageType == AddPageDialogFragment.PageType.Lined) {
                double leftMarginR = mPageColor == AddPageDialogFragment.PageColor.White ? MARGIN_RED :
                    (mPageColor == AddPageDialogFragment.PageColor.Blueprint ? LINE_SHADE_BLUEPRINT : LINE_SHADE) * 0.7;
                double leftMarginG = mPageColor == AddPageDialogFragment.PageColor.White ? MARGIN_GREEN :
                    (mPageColor == AddPageDialogFragment.PageColor.Blueprint ? LINE_SHADE_BLUEPRINT : LINE_SHADE) * 0.7;
                double leftMarginB = mPageColor == AddPageDialogFragment.PageColor.White ? MARGIN_BLUE :
                    (mPageColor == AddPageDialogFragment.PageColor.Blueprint ? LINE_SHADE_BLUEPRINT : LINE_SHADE) * 0.7;

                double rightMarginR = mPageColor == AddPageDialogFragment.PageColor.White ? MARGIN_RED :
                    (mPageColor == AddPageDialogFragment.PageColor.Blueprint ? LINE_SHADE_BLUEPRINT : LINE_SHADE) * 0.45;
                double rightMarginG = mPageColor == AddPageDialogFragment.PageColor.White ? MARGIN_GREEN * 1.6 :
                    (mPageColor == AddPageDialogFragment.PageColor.Blueprint ? LINE_SHADE_BLUEPRINT : LINE_SHADE) * 0.45;
                double rightMarginB = mPageColor == AddPageDialogFragment.PageColor.White ? MARGIN_BLUE * 1.6 :
                    (mPageColor == AddPageDialogFragment.PageColor.Blueprint ? LINE_SHADE_BLUEPRINT : LINE_SHADE) * 0.45;

                doc = PDFDocGenerator.generateLinedPaperDoc(width, height, 0.25, 0.45, lineShade, lineShade, lineShade, 1.2, leftMarginR, leftMarginG, leftMarginB,
                    rightMarginR, rightMarginG, rightMarginB, bgR, bgG, bgB, 0.85, 0.35);
            } else if (mPageType == AddPageDialogFragment.PageType.Blank) {
                doc = PDFDocGenerator.generateBlankPaperDoc(width, height, bgR, bgG, bgB);
            }

            for (int i = 0; i < mNumOfPages; i++) {
                if (page == null && doc != null) {
                    page = doc.getPage(1);
                }
                if (!mShouldCreateNewPdf) {
                    mPages[i] = page;
                } else {
                    mFinalDoc.pagePushBack(page);
                }

            }
        } catch (PDFNetException e) {
            return false;
        }

        return true;
    }

    /**
     * The overloaded implementation of {@link android.os.AsyncTask#onPostExecute(Object)}.
     **/
    @Override
    protected void onPostExecute(Boolean pass) {
        super.onPostExecute(pass);

        if (mProgressDialogHandler != null) {
            mProgressDialogHandler.removeCallbacks(mProgressDialogExecutor);
        }
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        if (!pass) {
            return;
        }

        if (mShouldCreateNewPdf && mOnCreateNewDocumentListener != null) {
            mOnCreateNewDocumentListener.onCreateNewDocument(mFinalDoc, mTitle);
        }
        if (!mShouldCreateNewPdf && mOnAddNewPagesListener != null) {
            mOnAddNewPagesListener.onAddNewPages(mPages);
        }
    }
}
