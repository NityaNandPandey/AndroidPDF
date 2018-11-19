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
import com.pdftron.pdf.controls.SearchResultsView;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

/**
 * A class that asynchronously finds a pattern through the document
 */
public class FindTextTask extends AsyncTask<Void, Boolean, Integer> {

    private PDFDoc mPdfDoc;
    private String mPattern;
    private int mTextSearchMode;
    private ArrayList<TextSearchResult> mResults = new ArrayList<>();
    private final ArrayList<SearchResultsView.Section> mSectionList;
    private final ArrayList<String> mSectionTitleList;
    private int mPagesSearched;
    private String mFacingCoverText;
    private Callback mCallback;

    /**
     * Callback interface invoked when a pattern is found in the document.
     */
    public interface Callback {

        /**
         * Called when the task of finding text has started.
         */
        void onFindTextTaskStarted();

        /**
         * Called when the task of finding text has been updated.
         *
         * @param foundResultOnPage True if result on the page has been found
         * @param results The text search results
         */
        void onFindTextTaskProgressUpdated(boolean foundResultOnPage, int pagesSearched, ArrayList<TextSearchResult> results);

        /**
         * Called when the task of finding text has been terminated.
         *
         * @param numResults The number of result
         * @param results The results
         */
        void onFindTextTaskFinished(int numResults, ArrayList<TextSearchResult> results);

        /**
         * Called when the task of finding text has been cancelled.
         */
        void onFindTextTaskCancelled();
    }

    public FindTextTask(@NonNull PDFViewCtrl pdfViewCtrl,
                        String pattern,
                        int textSearchMode,
                        ArrayList<SearchResultsView.Section> sectionList,
                        ArrayList<String> sectionTitleList) {
        mPdfDoc = pdfViewCtrl.getDoc();
        mPattern = pattern;
        mTextSearchMode = textSearchMode;
        mSectionList = sectionList;
        mSectionTitleList = sectionTitleList;
        mFacingCoverText = pdfViewCtrl.getContext().getResources().getString(R.string.pref_viewmode_facingcover_short);
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
    protected void onPreExecute() {
        if (mCallback != null) {
            mCallback.onFindTextTaskStarted();
        }
    }

    @Override
    protected Integer doInBackground(Void... params) {
        if (mPdfDoc == null) {
            return 0;
        }

        TextSearch textSearch = new TextSearch();
        int numResults = 0;
        boolean foundResultOnPage = false;

        synchronized (mSectionList) {
            int currentSectionIndex = (!mSectionList.isEmpty()) ? 0 : -1;

            boolean shouldUnlockRead = false;
            try {
                mPdfDoc.lockRead();
                shouldUnlockRead = true;
                mPagesSearched = 0;

                if (!textSearch.begin(mPdfDoc, mPattern, mTextSearchMode, -1, -1)) {
                    // if text search cannot be initialized
                    AnalyticsHandlerAdapter.getInstance().sendException(new Exception("pattern: " + mPattern + " | mode: " + mTextSearchMode));
                    return 0;
                }

                while (!isCancelled()) {
                    TextSearchResult result = textSearch.run();

                    if (result.getCode() == TextSearchResult.e_found) {
                        // Add result to end of list
                        mResults.add(numResults, result);
                        foundResultOnPage = true;
                        // Get result's section if doc has sections
                        if (currentSectionIndex >= 0) {
                            // Get position of result's first quad (bottom-left)
                            double resultX = -1, resultY = -1;
                            Highlights highlights = result.getHighlights();
                            highlights.begin(mPdfDoc);
                            if (highlights.hasNext()) {
                                double[] quads = highlights.getCurrentQuads();
                                if (quads.length / 8 > 0) {
                                    resultX = quads[0];
                                    resultY = quads[1];
                                }
                            }
                            if (result.getPageNum() < mSectionList.get(0).mPageNum) { // Result appears before the first section
                                mSectionTitleList.add(numResults, mFacingCoverText);
                            } else {
                                // Results are found in-order, so the lookup time can be reduced by starting the lookup
                                // at the last result's section.
                                for (int index = currentSectionIndex; index < mSectionList.size(); index++) {
                                    if (index < mSectionList.size() - 1) {
                                        // Check if result occurs before the next section
                                        SearchResultsView.Section nextSection = mSectionList.get(index + 1);
                                        boolean inNextSection = false;
                                        if (result.getPageNum() < nextSection.mPageNum) { // Result is in current section
                                            inNextSection = false;
                                        } else if (result.getPageNum() == nextSection.mPageNum && resultX >= 0 && resultY >= 0) {
                                            // Result is on same page as next section: check positions
                                            if (nextSection.top >= 0) { // Section's top offset is defined
                                                inNextSection = (resultY < nextSection.top);
                                                if (inNextSection && nextSection.left >= 0) { // Result might be in section, so check left offset
                                                    inNextSection = (resultX >= nextSection.left);
                                                }
                                            } else if (nextSection.left >= 0) {
                                                inNextSection = (resultX >= nextSection.left);
                                            }
                                        } else {
                                            inNextSection = true;
                                        }
                                        if (!inNextSection) {
                                            mSectionTitleList.add(numResults, mSectionList.get(index).mBookmark.getTitle());
                                            currentSectionIndex = index; // All subsequent results will be in this or a later section
                                            break;
                                        }
                                    } else { // on last section, so result must be in this section
                                        mSectionTitleList.add(numResults, mSectionList.get(index).mBookmark.getTitle());
                                        currentSectionIndex = index;
                                        break;
                                    }
                                }
                            }
                        }
                        numResults++;
                    } else if (result.getCode() == TextSearchResult.e_page) {
                        mPagesSearched++;
                        // Hit end of page, update UI
                        publishProgress(foundResultOnPage);
                        foundResultOnPage = false;
                    } else {
                        break;
                    }
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(mPdfDoc);
                }
            }
        }

        return numResults;
    }

    @Override
    protected void onProgressUpdate(Boolean... values) {
        if (mCallback != null) {
            mCallback.onFindTextTaskProgressUpdated(values[0], mPagesSearched, mResults);
        }
    }

    @Override
    protected void onPostExecute(Integer numResults) {
        if (mCallback != null) {
            mCallback.onFindTextTaskFinished(numResults, mResults);
        }
    }

    @Override
    protected void onCancelled(Integer numResults) {
        if (mCallback != null) {
            mCallback.onFindTextTaskCancelled();
        }
    }

    public boolean isRunning() {
        return getStatus() == Status.RUNNING;
    }

    public boolean isFinished() {
        return getStatus() == Status.FINISHED;
    }

    public String getPattern() {
        return mPattern;
    }
}
