//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.asynctask;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Highlights;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.TextSearch;
import com.pdftron.pdf.TextSearchResult;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

/**
 * A class that asynchronously generates highlights for all instances of a specified text in the document
 */
public class GenerateHighlightsTask extends AsyncTask<Void, Void, Void> {

    private int mPageStart;
    private int mPageEnd;
    private String mSearchPattern;
    private Callback mCallback;
    private int mTextSearchMode;
    private Highlights[] mHighlights;
    private final boolean mIsRtl;
    private final PDFDoc mDoc;

    /**
     * Callback interface invoked when search highlights are obtained.
     */
    public interface Callback {
        /**
         * Called when the task of highlights has been cancelled.
         *
         * @param pageStart The start page
         * @param pageEnd The end page
         */
        void onHighlightsTaskCancelled(int pageStart, int pageEnd);

        /**
         * Called when the task of highlights has been terminated.
         *
         * @param highlights The output highlights
         * @param pageStart The start page
         * @param pageEnd The end page
         */
        void onHighlightsTaskFinished(Highlights[] highlights, int pageStart, int pageEnd);
    }

    /**
     * Class constructor
     *
     * @param pdfViewCtrl The PDFViewCtrl
     * @param pageStart The start page to search
     * @param pageEnd The end page to search
     * @param searchPattern The search pattern
     * @param matchCase True if it should match case
     * @param matchWholeWords True if it should match the whole words
     * @param useRegularExpressions True if it should use regular expressions
     */
    public GenerateHighlightsTask(@NonNull PDFViewCtrl pdfViewCtrl,
                                  int pageStart,
                                  int pageEnd,
                                  String searchPattern,
                                  boolean matchCase,
                                  boolean matchWholeWords,
                                  boolean useRegularExpressions) {
        mPageStart = pageStart;
        mPageEnd = pageEnd;
        mSearchPattern = searchPattern;

        mIsRtl = pdfViewCtrl.getRightToLeftLanguage();
        mDoc = pdfViewCtrl.getDoc();

        mTextSearchMode = TextSearch.e_page_stop | TextSearch.e_highlight; // Set mode bitmask
        if (matchCase) {
            mTextSearchMode |= TextSearch.e_case_sensitive;
        }
        if (matchWholeWords) {
            mTextSearchMode |= TextSearch.e_whole_word;
        }
        if (useRegularExpressions) {
            mTextSearchMode |= TextSearch.e_reg_expression;
        }

        if (pageEnd > pageStart) {
            mHighlights = new Highlights[pageEnd - pageStart + 1];
        } else { // single page
            mHighlights = new Highlights[1];
        }
        for (int i = 0, cnt = mHighlights.length; i < cnt; i++) {
            mHighlights[i] = new Highlights();
        }
    }

    /**
     * Sets the callback listener.
     *
     * Sets the callback to null when the task is cancelled.
     *
     * @param callback The callback when the task is finished
     */
    public void setCallback(@Nullable Callback callback) {
        mCallback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {
        TextSearch textSearch = new TextSearch();
        int i = 0;

        if (mIsRtl) {
            textSearch.setRightToLeftLanguage(true);
        }

        boolean shouldUnlockRead = false;
        try {
            mDoc.lockRead();
            shouldUnlockRead = true;

            if (!textSearch.begin(mDoc, mSearchPattern, mTextSearchMode, mPageStart, mPageEnd)) {
                // if text search cannot be initialized
                return null;
            }

            while (!isCancelled()) {
                TextSearchResult result = textSearch.run();

                if (result.getCode() == TextSearchResult.e_found) {
                    // Add result and Highlights to lists
                    mHighlights[i].add(result.getHighlights());
                } else if (result.getCode() == TextSearchResult.e_page) {
                    i++;
                } else {
                    break;
                }
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                Utils.unlockReadQuietly(mDoc);
            }
        }

        return null;
    }

    @Override
    protected void onCancelled() {
        if (mCallback != null) {
            mCallback.onHighlightsTaskCancelled(mPageStart, mPageEnd);
        }
    }

    @Override
    protected void onPostExecute(Void param) {
        if (mCallback != null) {
            mCallback.onHighlightsTaskFinished(mHighlights, mPageStart, mPageEnd);
        }
    }
}
