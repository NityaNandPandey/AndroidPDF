//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.util.Log;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Convert;
import com.pdftron.pdf.DocumentConversion;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.R;

import java.io.File;

/**
 * Universal viewer batch test
 *
 * <div class="info">
 * <b> Note: </b>  This utility class is for universal conversion DEBUG testing purpose <b>ONLY</b>.
 * This is not meant to be used in release build.
 * </div>
 * <div class="warning">
 * <b>WARNING: </b>This batch test will cause unexpected visual effects on the PDFViewCtrl. This is expected.
 * </div>
 */
public class UniversalViewerBatchTest implements PDFViewCtrl.UniversalDocumentConversionListener {
    private int mCurrentFileConvertIndex = 1;
    private int mTotalFileCount;
    private File[] mFileList;
    private PDFViewCtrl mPdfViewCtrl;
    private boolean mPassing;

    private Context mContext;
    private PDFViewCtrl.UniversalDocumentConversionListener mOriginalConversionListener;

    public UniversalViewerBatchTest(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UniversalDocumentConversionListener originalConversionListener) {
        mPdfViewCtrl = pdfViewCtrl;
        mContext = context;
        mOriginalConversionListener = originalConversionListener;
    }

    public void run() {


        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "UniversalDocTest/");
        if (!directory.exists() || !directory.isDirectory()) {
            Log.d(this.getClass().getName(), "UniversalDocTest folder does not exist or is not a directory");
            showPostDialog(false);
            return;
        }
        mFileList = directory.listFiles();
        mTotalFileCount = mFileList.length;
        mCurrentFileConvertIndex = 0;
        mPassing = true;

        if (mTotalFileCount < 1) {
            Log.d(this.getClass().getName(), "UniversalDocTest folder does not have any files!");
            showPostDialog(false);
            return;
        }

        mPdfViewCtrl.addUniversalDocumentConversionListener(this);
        testUniversalFile(0);
    }

    private void showPostDialog(boolean pass) {
        mPdfViewCtrl.addUniversalDocumentConversionListener(mOriginalConversionListener);
        AlertDialog.Builder aBuilder = new AlertDialog.Builder(mContext);
        if (!pass) {
            aBuilder.setMessage("Error has occurred while performing batch test. Please check log for details.");
        } else {
            aBuilder.setMessage("Batch test success!");
        }
        aBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        aBuilder.create().show();
    }

    private void testUniversalFile(int index) {
        try {
            DocumentConversion conversion = Convert.universalConversion(mFileList[index].getAbsolutePath(), null);
            mPdfViewCtrl.closeDoc();
            mPdfViewCtrl.openUniversalDocument(conversion);
        } catch (PDFNetException e) {
            conversionFinish(false);
        }
    }

    private void conversionFinish(boolean pass) {
        if (!pass) {
            Log.d(this.getClass().getName(), "Document " + mCurrentFileConvertIndex + " (" + mFileList[mCurrentFileConvertIndex].getName() + ") FAIL!");
            mPassing = false;
        } else {
            Log.d(this.getClass().getName(), "Document " + mCurrentFileConvertIndex + " (" + mFileList[mCurrentFileConvertIndex].getName() + ") pass");
        }
        mCurrentFileConvertIndex++;
        if (mCurrentFileConvertIndex < mTotalFileCount) {
            testUniversalFile(mCurrentFileConvertIndex);
        } else {
            showPostDialog(mPassing);
        }
    }

    @Override
    public void onConversionEvent(PDFViewCtrl.ConversionState state, int totalPagesConverted) {
        switch(state) {
            case FAILED:
                conversionFinish(false);
                break;
            case FINISHED:
                conversionFinish(true);
                break;
            case PROGRESS:
                break;
        }
    }

}
