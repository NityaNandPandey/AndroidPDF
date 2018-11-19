package com.pdftron.pdf.controls;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.TextHighlighter;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.Utils;

public class FindTextOverlay extends ConstraintLayout implements
    PDFViewCtrl.TextSearchListener {

    public interface FindTextOverlayListener {

        /**
         * Called when next icon clicked.
         *
         * @param useFullTextResults
         */
        void onGotoNextSearch(boolean useFullTextResults);

        /**
         * Called when previous icon clicked.
         *
         * @param useFullTextResults
         */
        void onGotoPreviousSearch(boolean useFullTextResults);

        /**
         * The implementation should show the search progress.
         */
        void onSearchProgressShow();

        /**
         * The implementation should hide the search progress.
         */
        void onSearchProgressHide();
    }

    private FindTextOverlayListener mFindTextOverlayListener;

    public void setFindTextOverlayListener(FindTextOverlayListener listener) {
        mFindTextOverlayListener = listener;
    }

    protected ImageButton mButtonSearchNext;
    protected ImageButton mButtonSearchPrev;

    protected PDFViewCtrl mPdfViewCtrl;

    protected boolean mSearchMatchCase;
    protected boolean mSearchWholeWord;
    protected boolean mSearchUp;
    protected String mSearchQuery = "";
    protected boolean mSearchSettingsChanged;
    protected boolean mUseFullTextResults;

    protected int mNumSearchRunning;
    protected boolean mShowSearchCancelMessage = true;

    public FindTextOverlay(Context context) {
        this(context, null);
    }

    public FindTextOverlay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FindTextOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.controls_find_text_overlay, this);

        // Search prev/next buttons.
        mButtonSearchNext = view.findViewById(R.id.search_button_next);
        mButtonSearchNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoNextSearch();
            }
        });
        mButtonSearchPrev = view.findViewById(R.id.search_button_prev);
        mButtonSearchPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoPreviousSearch();
            }
        });
        if (Utils.isRtlLayout(getContext())) {
            mButtonSearchPrev.setImageResource(R.drawable.selector_search_next);
            mButtonSearchNext.setImageResource(R.drawable.selector_search_prev);
        }
    }

    /**
     * Sets the PDFViewCtrl
     *
     * @param pdfViewCtrl the PDFViewCtrl
     */
    public void setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;

        mPdfViewCtrl.setTextSearchListener(this);
    }

    /**
     * Goes to the next text in search.
     */
    public void gotoNextSearch() {
        if (mPdfViewCtrl == null) {
            return;
        }

        mSearchUp = false;
        if (mFindTextOverlayListener != null) {
            mFindTextOverlayListener.onGotoNextSearch(mUseFullTextResults);
        } else {
            findText();
        }
        highlightSearchResults();
    }

    /**
     * Goes to the previous text in search.
     */
    public void gotoPreviousSearch() {
        if (mPdfViewCtrl == null) {
            return;
        }

        mSearchUp = true;
        if (mFindTextOverlayListener != null) {
            mFindTextOverlayListener.onGotoPreviousSearch(mUseFullTextResults);
        } else {
            findText();
        }

        highlightSearchResults();
    }

    /**
     * Specifies the search query.
     *
     * @param text The search query
     */
    public void setSearchQuery(String text) {
        // If the search query has actually changed, stop using full text search results
        if (mSearchQuery != null && !mSearchQuery.equals(text)) {
            mUseFullTextResults = false;
        }
        mSearchQuery = text;
    }

    /**
     * Sets the search rule for match case.
     *
     * @param matchCase True if match case is enabled
     */
    public void setSearchMatchCase(boolean matchCase) {
        setSearchSettings(matchCase, mSearchWholeWord);
    }

    /**
     * Sets the search rule for whole word.
     *
     * @param wholeWord True if whole word is enabled
     */
    public void setSearchWholeWord(boolean wholeWord) {
        setSearchSettings(mSearchMatchCase, wholeWord);
    }

    /**
     * Sets the search rules for match case and whole word.
     *
     * @param matchCase True if match case is enabled
     * @param wholeWord True if whole word is enabled
     */
    public void setSearchSettings(boolean matchCase, boolean wholeWord) {
        mSearchMatchCase = matchCase;
        mSearchWholeWord = wholeWord;
        mSearchSettingsChanged = true;
    }

    /**
     * Resets full text results.
     */
    public void resetFullTextResults() {
        mUseFullTextResults = false;
        highlightSearchResults();
    }

    /**
     * Submits the query text.
     *
     * @param text The query text
     */
    public void queryTextSubmit(String text) {
        mSearchQuery = text;
        findText();
        highlightSearchResults();
    }

    public void findText() {
        findText(-1);
    }

    public void findText(int pageNum) {
        if (mPdfViewCtrl != null && mSearchQuery != null && mSearchQuery.trim().length() > 0) {
            mUseFullTextResults = false;
            mPdfViewCtrl.findText(mSearchQuery, mSearchMatchCase, mSearchWholeWord, mSearchUp, false, pageNum);
        }
    }

    /**
     * Starts the TextHighlighter tool.
     */
    public void highlightSearchResults() {
        if (mPdfViewCtrl == null) {
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        String prevPattern = null;
        if (toolManager.getTool() instanceof TextHighlighter) { // Get previous pattern, if highlighter is already running
            prevPattern = ((TextHighlighter) toolManager.getTool()).getSearchPattern();
        }
        // Restart text-highlighter only if query changed from last time, if applicable
        // NOTE: if the highlighter is not running, it will always be started here
        if (prevPattern == null || !mSearchQuery.equals(prevPattern) || mSearchSettingsChanged) {
            if (mSearchQuery.trim().length() > 0) {
                TextHighlighter highlighter = (TextHighlighter) toolManager.createTool(ToolManager.ToolMode.TEXT_HIGHLIGHTER, null);
                toolManager.setTool(highlighter);
                highlighter.start(mSearchQuery, mSearchMatchCase, mSearchWholeWord, false);
            }

            mSearchSettingsChanged = false;
        }
    }

    /**
     * Exits the search mode.
     */
    public void exitSearchMode() {
        if (mPdfViewCtrl == null) {
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

        if (toolManager.getTool() instanceof TextHighlighter) {
            TextHighlighter highlighter = (TextHighlighter) toolManager.getTool();
            mPdfViewCtrl.clearSelection();
            highlighter.clear();
            mPdfViewCtrl.invalidate();
        }
        toolManager.setTool(toolManager.createTool(ToolManager.ToolMode.PAN, null));
        //mPdfViewCtrl.requestFocus(); // Hide soft keyboard
        // Full doc. text search will be stopped by host fragment, so use findTextAsync for next search
        mUseFullTextResults = false;
    }

    /**
     * Highlights the results of full text search.
     *
     * @param result The {@link com.pdftron.pdf.TextSearchResult}
     */
    public void highlightFullTextSearchResult(com.pdftron.pdf.TextSearchResult result) {
        if (mPdfViewCtrl == null) {
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

        if (result.getCode() == com.pdftron.pdf.TextSearchResult.e_found) {
            mPdfViewCtrl.requestFocus(); // Removes focus from the SearchView, so the soft keyboard is not shown.
            mShowSearchCancelMessage = false;
            cancelFindText();
            mPdfViewCtrl.selectAndJumpWithHighlights(result.getHighlights());

            if (toolManager.getTool() instanceof TextHighlighter) {
                TextHighlighter highlighter = (TextHighlighter) toolManager.getTool();
                highlighter.update();
            }

            mUseFullTextResults = true;
        }
    }

    /**
     * Cancels finding text.
     */
    public void cancelFindText() {
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.cancelFindText();
        }
    }

    /**
     * Handles when {@link com.pdftron.pdf.PDFViewCtrl} starts to search text.
     */
    @Override
    public void onTextSearchStart() {
        mNumSearchRunning++;
        if (mFindTextOverlayListener != null) {
            mFindTextOverlayListener.onSearchProgressShow();
        }
    }

    /**
     * Handles when {@link com.pdftron.pdf.PDFViewCtrl} has progress on search text.
     *
     * @param progress progress indicator in the range [0, 100]
     */
    @Override
    public void onTextSearchProgress(int progress) {

    }

    /**
     * Handles when {@link com.pdftron.pdf.PDFViewCtrl} has progress on search text.
     *
     * @param result search result.
     */
    @Override
    public void onTextSearchEnd(PDFViewCtrl.TextSearchResult result) {
        if (mPdfViewCtrl == null) {
            return;
        }
        mNumSearchRunning--;
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.requestFocus();
        }

        if (mFindTextOverlayListener != null) {
            mFindTextOverlayListener.onSearchProgressHide();
            if (mNumSearchRunning > 0) { // Re-show progress, after delay
                mFindTextOverlayListener.onSearchProgressShow();
            }
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

        switch (result) {
            case NOT_FOUND:
                CommonToast.showText(getContext(), getContext().getString(R.string.search_results_none), Toast.LENGTH_SHORT, Gravity.CENTER, 0, 0);
                break;
            case FOUND:
                if (toolManager.getTool() instanceof TextHighlighter) {
                    TextHighlighter highlighter = (TextHighlighter) toolManager.getTool();
                    highlighter.update();
                }
                break;
            case CANCELED:
                CommonToast.showText(getContext(), getContext().getString(R.string.search_results_canceled), Toast.LENGTH_SHORT, Gravity.CENTER, 0, 0);
                break;
            case INVALID_INPUT:
                CommonToast.showText(getContext(), getContext().getString(R.string.search_results_invalid), Toast.LENGTH_SHORT, Gravity.CENTER, 0, 0);
                break;
        }
        mShowSearchCancelMessage = true;
    }
}
