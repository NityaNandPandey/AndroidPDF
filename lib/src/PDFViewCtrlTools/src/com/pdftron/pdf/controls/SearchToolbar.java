package com.pdftron.pdf.controls;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;

public class SearchToolbar extends Toolbar {

    private static final int SHOW_SEARCH_PROGRESS_TIMER = 500; // 0.5 sec

    protected MenuItem mMenuListAll;
    protected MenuItem mMenuMatchCase;
    protected MenuItem mMenuWholeWord;
    protected MenuItem mMenuSearchWeb;
    protected MenuItem mMenuProgress;

    protected SearchView mSearchView;

    protected String mSearchQuery;
    private boolean mJustSubmittedQuery;

    private SearchToolbarListener mSearchToolbarListener;

    public void setSearchToolbarListener(SearchToolbarListener listener) {
        mSearchToolbarListener = listener;
    }

    public interface SearchToolbarListener {
        void onExitSearch();
        void onClearSearchQuery();
        void onSearchQuerySubmit(String s);
        void onSearchQueryChange(String s);
        void onSearchOptionsItemSelected(MenuItem item, String s);
    }

    // Show search progress bar
    private Handler mShowSearchProgressHandler = new Handler(Looper.getMainLooper());
    private Runnable mShowSearchProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMenuProgress == null) {
                return;
            }
            if (!mMenuProgress.isVisible()) {
                mMenuProgress.setVisible(true);
            }
        }
    };

    public SearchToolbar(Context context) {
        this(context, null);
    }

    public SearchToolbar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflateMenu(R.menu.controls_search_toolbar);
        Menu menu = getMenu();

        mMenuListAll = menu.findItem(R.id.action_list_all);
        if (mMenuListAll != null) {
            mMenuListAll.setEnabled(false);
        }
        mMenuMatchCase = menu.findItem(R.id.action_match_case);
        mMenuWholeWord = menu.findItem(R.id.action_whole_word);

        mMenuSearchWeb = menu.findItem(R.id.action_search_web);
        mMenuProgress = menu.findItem(R.id.search_progress);

        setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSearchToolbarListener != null) {
                    mSearchToolbarListener.onExitSearch();
                }
            }
        });

        setOnMenuItemClickListener(new OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final int id = item.getItemId();
                if (id == R.id.action_search_web) {
                    if (mSearchView != null && mSearchView.getQuery() != null) {
                        String query = mSearchView.getQuery().toString();
                        if (!Utils.isNullOrEmpty(query)) {
                            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                            intent.putExtra(SearchManager.QUERY, query);
                            if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                                try {
                                    getContext().startActivity(intent);
                                } catch (ActivityNotFoundException ignored) {
                                    // This exception gets thrown if Google Search has been deactivated
                                }
                            }
                        }
                    }
                } else if (id == R.id.action_list_all ||
                    id == R.id.action_match_case ||
                    id == R.id.action_whole_word) {
                    String searchQuery = null;
                    if (mSearchView != null && mSearchView.getQuery().length() > 0) {
                        searchQuery = mSearchView.getQuery().toString();
                    }
                    if (id == R.id.action_list_all) {
                        Utils.hideSoftKeyboard(getContext(), mSearchView);
                    }
                    if (mSearchToolbarListener != null) {
                        mSearchToolbarListener.onSearchOptionsItemSelected(item, searchQuery);
                    }
                }
                return false;
            }
        });


        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.controls_search_toolbar, this);
        mSearchView = view.findViewById(R.id.searchView);

        if (Utils.isTablet(getContext())) {
            // Show search magnifying glass icon for tablets
            mSearchView.setIconifiedByDefault(false);
        }
        if (Utils.isTablet(getContext()) && !Utils.isRtlLayout(getContext())) {
            // Align searchview to right in actionbar (tablets only) and set width
            Toolbar.LayoutParams params = new Toolbar.LayoutParams(Gravity.END);
            params.width = getResources().getDimensionPixelSize(R.dimen.viewer_search_bar_width);
            mSearchView.setLayoutParams(params);
        }
        mSearchView.setFocusable(true);
        mSearchView.setFocusableInTouchMode(true);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (ShortcutHelper.isEnabled() && mSearchView != null) {
                    // shouldn't be focused otherwise the shortcut for previous search (Shift+Enter)
                    // just adds a space
                    mSearchView.clearFocus();
                }

                // avoid responding to Enter key in handleKey function if Enter is coming from submitting query text
                mJustSubmittedQuery = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mJustSubmittedQuery = false;
                    }
                }, 250);

                if (mSearchView != null) {
                    Utils.hideSoftKeyboard(getContext(), mSearchView);
                }
                if (mSearchToolbarListener != null) {
                    mSearchToolbarListener.onSearchQuerySubmit(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (mMenuListAll != null) {
                    if (!Utils.isNullOrEmpty(newText)) {
                        mMenuListAll.setEnabled(true);
                    } else {
                        mMenuListAll.setEnabled(false);
                    }
                }
                if (mSearchToolbarListener != null) {
                    mSearchToolbarListener.onSearchQueryChange(newText);
                }
                return false;
            }
        });
        if (Utils.isTablet(getContext())) {
            mSearchView.setQueryHint(getContext().getString(R.string.search_hint));
        } else {
            mSearchView.setQueryHint(getContext().getString(R.string.action_search)); // Use "Search" for small devices
        }
        ImageView searchCloseButton = this.mSearchView.findViewById(R.id.search_close_btn);
        searchCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editText = mSearchView.findViewById(R.id.search_src_text);
                if (editText != null) {
                    // Clear text from EditText view and clear query
                    editText.setText("");
                }
                mSearchView.setQuery("", false);
                // hide progress bar
                if (mSearchToolbarListener != null) {
                    mSearchToolbarListener.onClearSearchQuery();
                }
                setSearchProgressBarVisible(false);

                mSearchView.requestFocus();
                Utils.showSoftKeyboard(getContext(), editText);
            }
        });
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        if (visibility == VISIBLE) {
            if (mSearchView != null) {
                mSearchView.setIconified(false);
            }
            startSearchMode();
        } else {
            // Save search query
            if (mSearchView != null) {
                mSearchQuery = mSearchView.getQuery().toString();
                mSearchView.setIconified(true);
            }
            exitSearchMode();
        }
    }

    protected void startSearchMode() {
        if (mSearchView != null) {
            if (mSearchQuery != null && mSearchQuery.length() > 0) {
                // Restore search query, if any
                mSearchView.setQuery(mSearchQuery, false);
            }
            mSearchView.requestFocus();
            EditText editText = mSearchView.findViewById(R.id.search_src_text);

            Utils.showSoftKeyboard(getContext(), editText);
        }
    }

    protected void exitSearchMode() {
        if (mSearchView != null) {
            mSearchView.clearFocus();
            Utils.hideSoftKeyboard(getContext(), mSearchView);
        }
    }

    public void setSearchProgressBarVisible(boolean visible) {
        if (visible) {
            startShowSearchProgressTimer();
        } else {
            stopShowSearchProgressTimer();
            if (mMenuProgress != null) {
                mMenuProgress.setVisible(false);
            }
        }
    }

    public void startShowSearchProgressTimer() {
        stopShowSearchProgressTimer();
        if (mShowSearchProgressHandler != null) {
            mShowSearchProgressHandler.postDelayed(mShowSearchProgressRunnable, SHOW_SEARCH_PROGRESS_TIMER);
        }
    }

    public void stopShowSearchProgressTimer() {
        if (mShowSearchProgressHandler != null) {
            mShowSearchProgressHandler.removeCallbacksAndMessages(null);
        }
    }

    public boolean isJustSubmittedQuery() {
        return mJustSubmittedQuery;
    }

    public void setJustSubmittedQuery(boolean justSubmittedQuery) {
        mJustSubmittedQuery = justSubmittedQuery;
    }

    public SearchView getSearchView() {
        return mSearchView;
    }

    public void pause() {
        stopShowSearchProgressTimer();
    }
}
