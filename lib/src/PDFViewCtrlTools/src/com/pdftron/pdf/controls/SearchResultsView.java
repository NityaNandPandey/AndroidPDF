package com.pdftron.pdf.controls;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.Destination;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.TextSearch;
import com.pdftron.pdf.TextSearchResult;
import com.pdftron.pdf.asynctask.FindTextTask;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.DictIterator;
import com.pdftron.sdf.Obj;

import java.util.ArrayList;

/**
 * A Relative layout that shows search results
 */
public class SearchResultsView extends RelativeLayout implements FindTextTask.Callback {

    enum SearchResultStatus {
        NOT_HANDLED(0),
        HANDLED(1),
        USE_FINDTEXT(2),
        USE_FINDTEXT_FROM_END(3);

        private final int value;

        SearchResultStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    private TextView mEmptyView;
    private RelativeLayout mProgressLayout;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private SearchResultsAdapter mAdapter;

    private final ArrayList<Section> mSectionList = new ArrayList<>();
    private final ArrayList<String> mSectionTitleList = new ArrayList<>();
    private ArrayList<TextSearchResult> mSearchResultList = new ArrayList<>();
    private int mCurrentResult = -1;

    private String mLastSearchPattern;
    private int mTextSearchMode = TextSearch.e_page_stop | TextSearch.e_ambient_string | TextSearch.e_highlight;
    private boolean mListenerWaitingForResult = false;

    private PDFViewCtrl mPdfViewCtrl;
    private FindTextTask mTask;
    private PopulateSectionList mPopulateSectionListTask;
    private Animation mAnimationFadeOut;
    private Animation mAnimationFadeIn;
    private boolean mClickEnabled = true;
    private boolean mIsSectionListPopulated;

    private SearchResultsListener mListener;

    /**
     * Class constructor
     */
    public SearchResultsView(Context context) {
        this(context, null);
    }

    /**
     * Class constructor
     */
    public SearchResultsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Class constructor
     */
    public SearchResultsView(Context context, AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.controls_search_results, this, true);

        setBackgroundColor(Utils.getBackgroundColor(getContext()));

        mProgressLayout = findViewById(R.id.progress_layout);
        mProgressBar = findViewById(R.id.dialog_search_results_progress_bar);
        mProgressText = findViewById(R.id.progress_text);
        mEmptyView = findViewById(android.R.id.empty);
        ListView listView = findViewById(android.R.id.list);

        mAdapter = new SearchResultsAdapter(getContext(), R.layout.controls_search_results_popup_list_item,
            mSearchResultList, mSectionTitleList);
        listView.setAdapter(mAdapter);

        listView.setLongClickable(false);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                if (!mClickEnabled) {
                    return;
                }
                // Post click event to give item's ripple time to end, otherwise if the view is
                // hidden, the ripple will be shown the next time the view is shown.
                post(new Runnable() {
                    @Override
                    public void run() {

                        mCurrentResult = position;

                        if (mListener != null) {
                            // Get result at position
                            TextSearchResult result = mSearchResultList.get(position);
                            mListener.onSearchResultClicked(result);
                        }

                        if (Utils.isTablet(getContext())) {
                            startAnimation(mAnimationFadeOut);
                        }
                    }
                });
            }
        });

        // Fade-in/out animations: each have different startOffsets, so they cannot be combined
        mAnimationFadeOut = AnimationUtils.loadAnimation(getContext(), R.anim.controls_search_results_popup_fadeout);
        mAnimationFadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.controls_search_results_popup_fadein);

        // Start fade-in animation when fade-out animation ends
        mAnimationFadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // Disable clicking on items during fade out/in animation.
                mClickEnabled = false;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                startAnimation(mAnimationFadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mAnimationFadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Re-enable clicking on items after fade out/in animation ends.
                mClickEnabled = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    /**
     * Sets listener to {@link SearchResultsListener}.
     *
     * @param listener The Listener
     */
    public void setListener(SearchResultsListener listener) {
        mListener = listener;
    }

    /**
     * Sets the PDFViewCtrl.
     *
     * @param pdfViewCtrl The PDFViewCtrl
     */
    public void setPdfViewCtrl(PDFViewCtrl pdfViewCtrl) {
        if (pdfViewCtrl == null) {
            return;
        }
        mPdfViewCtrl = pdfViewCtrl;
        reset();
        startPopulateSectionListTask();
        resetProgressText();
    }

    /**
     * Gets PDFDoc contained in PDFViewCtrl
     *
     * @return The PDFDoc
     */
    public PDFDoc getDoc() {
        return (mPdfViewCtrl == null) ? null : mPdfViewCtrl.getDoc();
    }

    /**
     * @return The search pattern
     */
    @NonNull
    public String getSearchPattern() {
        return (mLastSearchPattern != null) ? mLastSearchPattern : "";
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == GONE) {
            cancelSearch();
            cancelPopulateSectionListTask();
        } else {
            startPopulateSectionListTask();
        }
    }

    /**
     * Resets internal state of window
     */
    public void reset() {
        cancelSearch();
        clearSearchResults();
        mLastSearchPattern = null;
    }

    /**
     * @return True if searching task is active
     */
    public boolean isActive() {
        return (mTask != null && !mTask.isCancelled() && (mTask.isRunning() || mTask.isFinished()));
    }

    private void clearSearchResults() {
        mSearchResultList.clear();
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Adds a text search result item to search result list
     *
     * @param item text search result item
     */
    public void add(TextSearchResult item) {
        mSearchResultList.add(item);
        mAdapter.setRtlMode((mPdfViewCtrl != null) && mPdfViewCtrl.getRightToLeftLanguage());
        mAdapter.notifyDataSetChanged();
    }

    private void resetProgressText() {
        mProgressText.setText(getContext().getResources().getString(R.string.tools_misc_please_wait));
    }

    private void updateProgressText(int pagesSearched) {
        try {
            int pageCount = mPdfViewCtrl.getDoc().getPageCount();
            mProgressText.setText(getContext().getResources().getString(R.string.search_progress_text, pagesSearched, pageCount));
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Goes to the next result in the window.
     *
     * @param searchUp True if should go to previous search (up);
     *                 False if should go to the next search (down)
     * @return The status of search.
     */
    public SearchResultsView.SearchResultStatus getResult(boolean searchUp) {
        int pageNum = mPdfViewCtrl.getCurrentPage();
        SearchResultsView.SearchResultStatus status = SearchResultsView.SearchResultStatus.NOT_HANDLED;
        TextSearchResult result = null;
        if (mSearchResultList.size() > 0 && mTask != null) {
            if (mCurrentResult != -1 && mSearchResultList.get(mCurrentResult).getPageNum() == pageNum) {
                // A result from the list on the same page has been selected before
                if (searchUp) {
                    if (mCurrentResult - 1 >= 0) {
                        // Get the previous result
                        mCurrentResult--;
                        result = mSearchResultList.get(mCurrentResult);
                    } else {
                        if (mTask.isFinished() && !mTask.isCancelled()) {
                            // Task finished successfully, wrap around to end
                            mCurrentResult = mSearchResultList.size() - 1;
                            result = mSearchResultList.get(mCurrentResult);
                        } else if (mTask.isRunning()) {
                            // We want the last result, so we would have to wait for entire search to finish
                            // Don't handle this case
                            status = SearchResultsView.SearchResultStatus.USE_FINDTEXT_FROM_END;
                        }
                    }
                } else {
                    if (mCurrentResult + 1 < mSearchResultList.size()) {
                        // Get the next result
                        mCurrentResult++;
                        result = mSearchResultList.get(mCurrentResult);
                    } else {
                        if (mTask.isFinished() && !mTask.isCancelled()) {
                            // Task finished successfully, so no more results could be after the current
                            // Wrap around to beginning
                            mCurrentResult = 0;
                            result = mSearchResultList.get(mCurrentResult);
                        } else if (mTask.isRunning()) {
                            // More results might be after the current, so we should wait
                            mListenerWaitingForResult = true;
                        }
                    }
                }
            } else {
                if (mTask.isRunning()) {
                    if (mCurrentResult != -1) {
                        if (mSearchResultList.get(mCurrentResult).getPageNum() < pageNum) {
                            // Cannot loop through results list to get results because the list is changing
                            // Don't handle this case
                            status = SearchResultsView.SearchResultStatus.USE_FINDTEXT;
                        }
                    } else {
                        status = SearchResultsView.SearchResultStatus.USE_FINDTEXT;
                    }
                }
                if (status != SearchResultsView.SearchResultStatus.USE_FINDTEXT) {
                    // Try to get the first or last result on the page
                    if (searchUp) {
                        if (mTask.isRunning()) {
                            // Current result's page number MUST be greater than current page's.
                            // Find last result on page, starting with current result's page
                            for (int i = mCurrentResult; i >= 0; i--) {
                                if (mSearchResultList.get(i).getPageNum() <= pageNum) {
                                    // Last result on specified page, or next page backward with a result
                                    mCurrentResult = i;
                                    result = mSearchResultList.get(mCurrentResult);
                                    break;
                                }
                            }
                        } else {
                            for (int i = mSearchResultList.size() - 1; i >= 0; i--) {
                                if (mSearchResultList.get(i).getPageNum() <= pageNum) {
                                    // Last result on specified page, or next page backward with a result
                                    mCurrentResult = i;
                                    result = mSearchResultList.get(mCurrentResult);
                                    break;
                                }
                            }
                        }
                        if (result == null) {
                            // No results found on or before pageNum
                            if (mTask.isFinished() && !mTask.isCancelled()) {
                                // Task finished successfully, wrap around to end
                                mCurrentResult = mSearchResultList.size() - 1;
                                result = mSearchResultList.get(mCurrentResult);
                            } else {
                                status = SearchResultsView.SearchResultStatus.USE_FINDTEXT;
                            }
                        }
                    } else {
                        if (mTask.isRunning()) {
                            // Current result's page number MUST be greater than current page's.
                            // Find first result on page, stopping at current result's page
                            for (int i = 0; i <= mCurrentResult; i++) {
                                if (mSearchResultList.get(i).getPageNum() >= pageNum) {
                                    // First result on specified page, or next page forward with a result
                                    mCurrentResult = i;
                                    result = mSearchResultList.get(mCurrentResult);
                                    break;
                                }
                            }
                        } else {
                            for (int i = 0; i < mSearchResultList.size(); i++) {
                                if (mSearchResultList.get(i).getPageNum() >= pageNum) {
                                    // First result on specified page, or next page forward with a result
                                    mCurrentResult = i;
                                    result = mSearchResultList.get(mCurrentResult);
                                    break;
                                }
                            }
                        }
                        if (result == null) {
                            // No result found on or after pageNum
                            if (mTask.isFinished() && !mTask.isCancelled()) {
                                // Task finished successfully, wrap around to beginning
                                mCurrentResult = 0;
                                result = mSearchResultList.get(mCurrentResult);
                            } else {
                                status = SearchResultsView.SearchResultStatus.USE_FINDTEXT;
                            }
                        }
                    }
                }
            }
        }
        if (result != null) {
            if (mListener != null) {
                mListener.onSearchResultFound(result);
            }
            status = SearchResultsView.SearchResultStatus.HANDLED;
        } else if (mListenerWaitingForResult) {
            if (mListener != null) {
                mListener.onFullTextSearchStart();
            }
            status = SearchResultsView.SearchResultStatus.HANDLED;
        }
        return status;
    }

    /**
     * This should be called when cancelling search
     */
    public void cancelGetResult() {
        if (mListenerWaitingForResult) {
            if (mListener != null) {
                mListener.onSearchResultFound(null);
            }
            mListenerWaitingForResult = false;
            CommonToast.showText(getContext(), getContext().getResources().getString(R.string.search_results_canceled), Toast.LENGTH_SHORT, Gravity.CENTER, 0, 0);
        }
    }

    /**
     * Finds a specified pattern in the document.
     *
     * @param pattern The pattern to be found
     */
    public void findText(
        @NonNull String pattern) {

        // a temporarily hack on right-to-left scripts until the core will support RTL in search
        pattern = Utils.getBidiString(pattern);

        if (mLastSearchPattern == null || !pattern.equals(mLastSearchPattern)) {
            // This is a new search: save pattern
            mLastSearchPattern = pattern;
        } else if (mLastSearchPattern != null && pattern.equals(mLastSearchPattern)) {
            // Same search pattern as last time

            // Checkboxes have not changed
            if (mTask != null && pattern.equals(mTask.getPattern())) {
                if (mTask.isRunning()) {
                    // Task is currently searching for the same pattern
                    return;
                } else if (mTask.isFinished()) {
                    // Task has completed for the same pattern
                    return;
                }
            }
        }

        if (isSectionListPopulated()) {
            restartSearch();
        } // otherwise wait until section list is populated

    }

    // Cancel the currently running task, if any
    // Should be called when window is dismissed
    private void cancelSearch() {

        if (mTask != null) {
            mTask.cancel(true);
        }

    }

    private void restartSearch() {

        // cancel the current search task and reset the search results
        cancelSearch();
        clearSearchResults();

        mTask = new FindTextTask(mPdfViewCtrl, mLastSearchPattern, mTextSearchMode, mSectionList, mSectionTitleList);
        mTask.setCallback(this);
        mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    /**
     * Sets whether the the pattern is case sensitive.
     *
     * @param matchCase True if the pattern is case sensitive
     */
    public void setMatchCase(boolean matchCase) {
        if (matchCase) {
            // Set flag in bitmask
            mTextSearchMode |= TextSearch.e_case_sensitive;
        } else {
            // Clear flag in bitmask
            mTextSearchMode &= ~TextSearch.e_case_sensitive;
        }
        restartSearch();
    }

    /**
     * Sets whether the whole word should match the pattern.
     *
     * @param wholeWord True if the whole word should match the pattern
     */
    public void setWholeWord(boolean wholeWord) {
        if (wholeWord) {
            // Set flag in bitmask
            mTextSearchMode |= TextSearch.e_whole_word;
        } else {
            // Clear flag in bitmask
            mTextSearchMode &= ~TextSearch.e_whole_word;
        }
        restartSearch();
    }

    private void startPopulateSectionListTask() {

        if (mPdfViewCtrl == null || isSectionListPopulated()) {
            return;
        }

        if (mPopulateSectionListTask != null && mPopulateSectionListTask.isRunning()) {
            return;
        }

        Bookmark firstBookmark = Utils.getFirstBookmark(mPdfViewCtrl.getDoc());
        if (firstBookmark != null) {
            mPopulateSectionListTask = new PopulateSectionList(firstBookmark);
            mPopulateSectionListTask.setCallback(new PopulateSectionList.Callback() {
                @Override
                public void onPopulateSectionListFinished(ArrayList<Section> sectionList) {

                    synchronized (mSectionList) {
                        mSectionList.clear();
                        mSectionList.addAll(sectionList);
                    }
                    restartSearch();

                }
            });
            mPopulateSectionListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

    }

    private void cancelPopulateSectionListTask() {

        if (mPopulateSectionListTask != null && mPopulateSectionListTask.isRunning()) {
            mPopulateSectionListTask.cancel(true);
        }

    }

    private boolean isSectionListPopulated() {

        if (mIsSectionListPopulated) {
            return true;
        }

        mIsSectionListPopulated = !mSectionList.isEmpty()
            || (mPopulateSectionListTask != null && mPopulateSectionListTask.isFinished())
            || Utils.getFirstBookmark(mPdfViewCtrl.getDoc()) == null;

        return mIsSectionListPopulated;

    }

    /**
     * Called when the task of finding text has started.
     */
    @Override
    public void onFindTextTaskStarted() {
        // Show progress bar and progress text
        mProgressLayout.setVisibility(View.VISIBLE);
        mEmptyView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        mAdapter.setRtlMode((mPdfViewCtrl != null) && mPdfViewCtrl.getRightToLeftLanguage());
    }

    /**
     * Called when the task of finding text has been updated.
     *
     * @param foundResultOnPage True if result on the page has been found
     * @param results           The text search results
     */
    @Override
    public void onFindTextTaskProgressUpdated(boolean foundResultOnPage, int pagesSearched, ArrayList<TextSearchResult> results) {
        // Update adapter's list of results
        mSearchResultList.clear();
        mSearchResultList.addAll(results);
        mAdapter.notifyDataSetChanged();

        // Update progress text
        updateProgressText(pagesSearched);

        if (foundResultOnPage && mSearchResultList.size() > 0 && mListenerWaitingForResult) {
            if (mListener != null) {
                TextSearchResult result = null;
                if (mCurrentResult != -1 && mCurrentResult + 1 < mSearchResultList.size()) {
                    // Give listener the next result after the current result
                    result = mSearchResultList.get(++mCurrentResult);
                }
                mListener.onSearchResultFound(result);
            }
            mListenerWaitingForResult = false;
        }
    }

    /**
     * Called when the task of finding text has been terminated.
     *
     * @param numResults The number of result
     * @param results    The results
     */
    @Override
    public void onFindTextTaskFinished(int numResults, ArrayList<TextSearchResult> results) {
        // Update adapter's list of results
        mSearchResultList.clear();
        mSearchResultList.addAll(results);
        mAdapter.notifyDataSetChanged();

        // Hide progress bar and set progress text
        mProgressBar.setVisibility(View.GONE);

        // Only show empty text if no results were found
        if (numResults > 0) {
            mEmptyView.setVisibility(View.GONE);
            mProgressText.setText(getContext().getResources().getString(R.string.search_results_text, numResults));
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
            mProgressLayout.setVisibility(View.GONE);
        }

        if (mListenerWaitingForResult) {
            // Listener is still waiting for a result
            // Must not have found any more results since the listener started waiting.
            if (mListener != null) {
                if (mSearchResultList.size() > 0) {
                    // Give listener the first result
                    TextSearchResult result = mSearchResultList.get(0);
                    mListener.onSearchResultFound(result);
                } else {
                    // No results (should never happen)
                    mListener.onSearchResultFound(null);
                    CommonToast.showText(getContext(), getContext().getResources().getString(R.string.search_results_none), Toast.LENGTH_SHORT, Gravity.CENTER, 0, 0);
                }
            }
            mListenerWaitingForResult = false;
        }
    }

    /**
     * Called when the task of finding text has been cancelled.
     */
    @Override
    public void onFindTextTaskCancelled() {
        cancelGetResult();
    }

    /**
     * Listener interface for search result events
     */
    public interface SearchResultsListener {
        /**
         * Called when an item from the full document text search has been clicked.
         *
         * @param result The text search result
         */
        void onSearchResultClicked(TextSearchResult result);

        /**
         * Called when full text search has started.
         */
        void onFullTextSearchStart();

        /**
         * Called when a text search result has been found.
         *
         * @param result The text search result
         */
        void onSearchResultFound(TextSearchResult result);
    }

    /**
     * A class contains information about search result section
     */
    public static class Section {
        /**
         * Position in page space
         */
        public double left, bottom, right, top; // Page Space
        /**
         * Bookmark
         */
        public Bookmark mBookmark;

        /**
         * Page number
         */
        public int mPageNum;

        /**
         * Class constructor
         *
         * @param bookmark the bookmark
         * @param pageNum  page number
         */
        public Section(Bookmark bookmark, int pageNum) {
            mBookmark = bookmark;
            mPageNum = pageNum;
            left = -1;
            bottom = -1;
            right = -1;
            top = -1;
        }

        /**
         * Sets position to section
         *
         * @param dest destination position
         */
        public void setPosition(Destination dest) {
            try {
                Obj obj = dest.getSDFObj();
                if (obj.isArray() || obj.isDict()) { // Explicit destination
                    switch (dest.getFitType()) {
                        case Destination.e_XYZ: // Fit to horizontal and vertical coordinates, with zoom
                            // Array = [ page /XYZ left top zoom ]
                            if (obj.isArray() && obj.size() == 5) {
                                if (obj.getAt(2).isNumber()) {
                                    this.left = obj.getAt(2).getNumber();
                                }
                                if (obj.getAt(3).isNumber()) {
                                    this.top = obj.getAt(3).getNumber();
                                }
                            } else if (obj.isDict()) {
                                DictIterator dictIterator = obj.getDictIterator();
                                while (dictIterator.hasNext()) { // Should only have one key/value
                                    Obj value = dictIterator.value();
                                    if (value.isArray() && value.size() == 5) {
                                        if (value.getAt(2).isNumber()) {
                                            this.left = value.getAt(2).getNumber();
                                        }
                                        if (value.getAt(3).isNumber()) {
                                            this.top = value.getAt(3).getNumber();
                                        }
                                    }
                                    dictIterator.next();
                                }
                            }
                            break;
                        case Destination.e_Fit: // Fit entire page
                            // Array = [ page /Fit ]
                            break;
                        case Destination.e_FitH: // Fit width with vertical coordinate at top
                            // Array = [ page /FitH top ]
                            if (obj.isArray() && obj.size() == 3) {
                                if (obj.getAt(2).isNumber()) {
                                    this.top = obj.getAt(2).getNumber();
                                }
                            } else if (obj.isDict()) {
                                DictIterator dictIterator = obj.getDictIterator();
                                while (dictIterator.hasNext()) { // Should only have one key/value
                                    Obj value = dictIterator.value();
                                    if (value.isArray() && value.size() == 3) {
                                        if (value.getAt(2).isNumber()) {
                                            this.top = value.getAt(2).getNumber();
                                        }
                                    }
                                    dictIterator.next();
                                }
                            }
                            break;
                        case Destination.e_FitV: // Fit height with horizontal coordinate at left
                            // Array = [ page /FitV left ]
                            if (obj.isArray() && obj.size() == 3) {
                                if (obj.getAt(2).isNumber()) {
                                    this.left = obj.getAt(2).getNumber();
                                }
                            } else if (obj.isDict()) {
                                DictIterator dictIterator = obj.getDictIterator();
                                while (dictIterator.hasNext()) { // Should only have one key/value
                                    Obj value = dictIterator.value();
                                    if (value.isArray() && value.size() == 3) {
                                        if (value.getAt(2).isNumber()) {
                                            this.left = value.getAt(2).getNumber();
                                        }
                                    }
                                    dictIterator.next();
                                }
                            }
                            break;
                        case Destination.e_FitR: // Fit rectangle
                            // Array = [ page /FitR left bottom right top ]
                            if (obj.isArray() && obj.size() == 6) {
                                if (obj.getAt(2).isNumber()) {
                                    this.left = obj.getAt(2).getNumber();
                                }
                                if (obj.getAt(3).isNumber()) {
                                    this.bottom = obj.getAt(3).getNumber();
                                }
                                if (obj.getAt(4).isNumber()) {
                                    this.right = obj.getAt(4).getNumber();
                                }
                                if (obj.getAt(5).isNumber()) {
                                    this.top = obj.getAt(5).getNumber();
                                }
                            } else if (obj.isDict()) {
                                DictIterator dictIterator = obj.getDictIterator();
                                while (dictIterator.hasNext()) { // Should only have one key/value
                                    Obj value = dictIterator.value();
                                    if (value.isArray() && value.size() == 6) {
                                        if (value.getAt(2).isNumber()) {
                                            this.left = value.getAt(2).getNumber();
                                        }
                                        if (value.getAt(3).isNumber()) {
                                            this.bottom = value.getAt(3).getNumber();
                                        }
                                        if (value.getAt(4).isNumber()) {
                                            this.right = value.getAt(4).getNumber();
                                        }
                                        if (value.getAt(5).isNumber()) {
                                            this.top = value.getAt(5).getNumber();
                                        }
                                    }
                                    dictIterator.next();
                                }
                            }
                            break;
                        case Destination.e_FitB: // Fit bounding box
                            // Array = [ page /FitB ]
                            break;
                        case Destination.e_FitBH: // Fit bounding box with vertical coordinate at top
                            // Array = [ page /FitBH top ]
                            if (obj.isArray() && obj.size() == 3) {
                                if (obj.getAt(2).isNumber()) {
                                    this.top = obj.getAt(2).getNumber();
                                }
                            } else if (obj.isDict()) {
                                DictIterator dictIterator = obj.getDictIterator();
                                while (dictIterator.hasNext()) { // Should only have one key/value
                                    Obj value = dictIterator.value();
                                    if (value.isArray() && value.size() == 3) {
                                        if (value.getAt(2).isNumber()) {
                                            this.top = value.getAt(2).getNumber();
                                        }
                                    }
                                    dictIterator.next();
                                }
                            }
                            break;
                        case Destination.e_FitBV: // Fit bounding box with horizontal coordinate at left
                            // Array = [ page /FitBV left ]
                            if (obj.isArray() && obj.size() == 3) {
                                if (obj.getAt(2).isNumber()) {
                                    this.left = obj.getAt(2).getNumber();
                                }
                            } else if (obj.isDict()) {
                                DictIterator dictIterator = obj.getDictIterator();
                                while (dictIterator.hasNext()) { // Should only have one key/value
                                    Obj value = dictIterator.value();
                                    if (value.isArray() && value.size() == 3) {
                                        if (value.getAt(2).isNumber()) {
                                            this.left = value.getAt(2).getNumber();
                                        }
                                    }
                                    dictIterator.next();
                                }
                            }
                            break;
                    }
                }
            } catch (PDFNetException e) {
                left = -1;
                bottom = -1;
                right = -1;
                top = -1;
            }
        }
    }

    private static class PopulateSectionList extends AsyncTask<Void, Void, Void> {

        Bookmark mBookmark;
        Callback mCallback;
        private ArrayList<Section> mPopulatedSectionList = new ArrayList<>();

        PopulateSectionList(
            Bookmark bookmark) {

            mBookmark = bookmark;

        }

        /**
         * Sets the callback listener.
         *
         * @param callback The callback when the task is finished
         */
        public void setCallback(@Nullable Callback callback) {
            mCallback = callback;
        }


        @Override
        protected Void doInBackground(
            Void... params) {

            try {
                populateSectionList(mBookmark);
            } catch (PDFNetException e) {
                mPopulatedSectionList.clear();
            }
            return null;

        }

        @Override
        protected void onPostExecute(
            Void aVoid) {

            super.onPostExecute(aVoid);

            if (mCallback != null) {
                mCallback.onPopulateSectionListFinished(mPopulatedSectionList);
            }

        }

        private void populateSectionList(
            Bookmark root)
            throws PDFNetException {

            for (Bookmark item = root; item.isValid() && !isCancelled(); item = item.getNext()) {
                Action action = item.getAction();
                if (action.isValid() && action.getType() == Action.e_GoTo) { // check for GoTo dest. type
                    Destination dest = action.getDest();
                    if (dest.isValid()) {
                        Section section = new Section(item, dest.getPage().getIndex());
                        section.setPosition(dest);
                        mPopulatedSectionList.add(section); // add section to list
                    }
                }

                if (item.hasChildren()) { // recursively print bookmark's subtree
                    populateSectionList(item.getFirstChild());
                }
            }

        }

        boolean isRunning() {
            return getStatus() == Status.RUNNING;
        }

        boolean isFinished() {
            return getStatus() == Status.FINISHED;
        }

        /**
         * Callback interface invoked when section list is populated.
         */
        public interface Callback {

            /**
             * Called when the task of populating section list has been terminated.
             *
             * @param sectionList The section list result
             */
            void onPopulateSectionListFinished(ArrayList<Section> sectionList);

        }

    }

}
