//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.dialog;

import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.BookmarksTabLayout;
import com.pdftron.pdf.controls.NavigationListDialogFragment;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.DialogFragmentTab;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

/**
 * This class shows a dialog containing other dialogs in separate tabs. The possible dialogs that
 * can be shown inside this master dialog are
 *      user-defined bookmarks (See {@link com.pdftron.pdf.controls.UserBookmarkDialogFragment}),
 *      document outline (See {@link com.pdftron.pdf.controls.OutlineDialogFragment}),
 *      annotations (See {@link com.pdftron.pdf.controls.AnnotationDialogFragment})
 * or any classes that are inherited from them.
 */
public class BookmarksDialogFragment extends DialogFragment implements TabLayout.OnTabSelectedListener {

    @SuppressWarnings("unused")
    private static final String TAG = BookmarksDialogFragment.class.getName();

    protected BookmarksTabLayout mTabLayout;
    protected Toolbar mToolbar;
    private ArrayList<DialogFragmentTab> mDialogFragmentTabs;
    private PDFViewCtrl mPdfViewCtrl;
    private Bookmark mCurrentBookmark;
    protected int mInitialTabIndex;
    private boolean mHasEventAction;
    private BookmarksTabLayout.BookmarksTabsListener mBookmarksTabsListener;
    private BookmarksDialogListener mBookmarksDialogListener;

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface BookmarksDialogListener {
        /**
         * Called when the bookmarks dialog has been dismissed.
         *
         * @param tabIndex The index of selected tab when dismissed
         */
        void onBookmarksDialogDismissed(int tabIndex);
    }

    /**
     * Returns a new instance of the class.
     */
    public static BookmarksDialogFragment newInstance() {
        return new BookmarksDialogFragment();
    }

    public BookmarksDialogFragment() {

    }

    /**
     * Sets the {@link PDFViewCtrl}
     *
     * @param pdfViewCtrl The {@link PDFViewCtrl}
     *
     * @return This class
     */
    public BookmarksDialogFragment setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        return this;
    }

    /**
     * Sets the dialog fragment tabs.
     *
     * @param dialogFragmentTabs A list of dialog fragments that should be shown in separate tabs
     *
     * @return This class
     */
    @SuppressWarnings("unused")
    public BookmarksDialogFragment setDialogFragmentTabs(@NonNull ArrayList<DialogFragmentTab> dialogFragmentTabs) {
        return setDialogFragmentTabs(dialogFragmentTabs, 0);
    }

    /**
     * Sets the dialog fragment tabs.
     *
     * @param dialogFragmentTabs A list of dialog fragments that should be shown in separate tabs
     * @param initialTabIndex The initial tab index
     *
     * @return This class
     */
    public BookmarksDialogFragment setDialogFragmentTabs(@NonNull ArrayList<DialogFragmentTab> dialogFragmentTabs, int initialTabIndex) {
        mDialogFragmentTabs = dialogFragmentTabs;
        if (dialogFragmentTabs.size() > initialTabIndex) {
            mInitialTabIndex = initialTabIndex;
        }
        return this;
    }

    /**
     * Sets the current bookmark.
     *
     * @param currentBookmark The current bookmark
     *
     * @return This class
     */
    @SuppressWarnings("unused")
    public BookmarksDialogFragment setCurrentBookmark(Bookmark currentBookmark) {
        mCurrentBookmark = currentBookmark;
        return this;
    }

    /**
     * Sets the BookmarksDialogListener listener.
     *
     * @param listener The listener
     */
    @SuppressWarnings("unused")
    public void setBookmarksDialogListener(BookmarksDialogListener listener) {
        mBookmarksDialogListener = listener;
    }

    /**
     * Sets the BookmarksTabsListener listener.
     *
     * @param listener The listener
     */
    @SuppressWarnings("unused")
    public void setBookmarksTabsListener(BookmarksTabLayout.BookmarksTabsListener listener) {
        mBookmarksTabsListener = listener;
    }

    /**
     * The overloaded implementation of {@link DialogFragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookmarks_dialog, null);

        if (mPdfViewCtrl == null) {
            // it can be happened because the dialog is recreated from activity
            // and since this fragment is relying on PDFViewCtrl and PDFViewCtrl cannot be
            // retrieved after re-creation there is no way to reuse the dialog
            return view;
        }

        mToolbar = view.findViewById(R.id.toolbar);

        // Tab layout
        mTabLayout = view.findViewById(R.id.tabhost);
        ViewPager viewPager = view.findViewById(R.id.view_pager);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        setHasOptionsMenu(true);

        if (mDialogFragmentTabs == null) {
            throw new NullPointerException("DialogFragmentTabs cannot be null. Call setDialogFragmentTabs(ArrayList<DialogFragmentTab>)");
        }

        mTabLayout.setup(getActivity(), getChildFragmentManager(), R.id.view_pager,
            mPdfViewCtrl, mCurrentBookmark);
        for (DialogFragmentTab dialogFragmentTab : mDialogFragmentTabs) {
            if (dialogFragmentTab._class == null || dialogFragmentTab.tabTag == null) {
                continue;
            }

            TabLayout.Tab tab = mTabLayout.newTab().setTag(dialogFragmentTab.tabTag);
            if (dialogFragmentTab.tabIcon != null) {
                dialogFragmentTab.tabIcon.mutate();
                tab.setIcon(dialogFragmentTab.tabIcon);
            }
            if (dialogFragmentTab.tabText != null) {
                tab.setText(dialogFragmentTab.tabText);
            }
            mTabLayout.addTab(tab, dialogFragmentTab._class, dialogFragmentTab.bundle);
        }

        mTabLayout.setupWithViewPager(viewPager);

        TabLayout.Tab selectedTab = mTabLayout.getTabAt(mInitialTabIndex);
        if (selectedTab != null) {
            selectedTab.select();
            setToolbarTitleBySelectedTab((String) selectedTab.getTag());
            mTabLayout.onTabSelected(selectedTab);
        }

        int selectedColor = view.getContext().getResources().getColor(android.R.color.white);
        int normalColor = Utils.adjustAlphaColor(selectedColor, .5f);
        mTabLayout.setTabTextColors(normalColor, selectedColor);

        for (int i = 0, cnt = mTabLayout.getTabCount(); i < cnt; ++i) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            if (tab == null) {
                continue;
            }
            Drawable icon = tab.getIcon();
            if (icon != null) {
                icon.setAlpha(tab.isSelected() ? 255 : 127);
            }
        }

        // If only one tab item is supplied, hide the TabLayout
        if (mDialogFragmentTabs.size() == 1) {
            mTabLayout.setVisibility(View.GONE);
        }

        if (mBookmarksTabsListener != null) {
            mTabLayout.setBookmarksTabsListener(mBookmarksTabsListener);
        }
        mTabLayout.setAnalyticsEventListener(
            new NavigationListDialogFragment.AnalyticsEventListener() {
                @Override
                public void onEventAction() {
                    mHasEventAction = true;
                }
            });

        mHasEventAction = false;

        // click events
        mTabLayout.addOnTabSelectedListener(this);
        return view;
    }

    private void setToolbarTitleBySelectedTab(String tag) {
        String toolbarTitle = getString(R.string.bookmark_dialog_fragment_bookmark_tab_title);
        for (DialogFragmentTab dialogFragmentTab: mDialogFragmentTabs) {
            if (dialogFragmentTab._class == null || dialogFragmentTab.tabTag == null) {
                continue;
            }
            if (dialogFragmentTab.tabTag.equals(tag)) {
                toolbarTitle = dialogFragmentTab.toolbarTitle;
                break;
            }
        }

        mToolbar.setTitle(toolbarTitle);
    }

    @Override
    public void onStart() {
        super.onStart();
        TabLayout.Tab selectedTab = mTabLayout.getTabAt(mInitialTabIndex);
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATION_LISTS_OPEN,
            AnalyticsParam.navigationListsTabParam(BookmarksTabLayout.getNavigationId(selectedTab)));
    }

    @Override
    public void onStop() {
        super.onStop();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATION_LISTS_OPEN);
    }

    /**
     * The overloaded implementation of {@link DialogFragment#onDismiss(DialogInterface)}.
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (mTabLayout != null) {
            TabLayout.Tab selectedTab = mTabLayout.getTabAt(mTabLayout.getSelectedTabPosition());
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATION_LISTS_CLOSE,
                AnalyticsParam.navigateListCloseParam(selectedTab, mHasEventAction));

            mTabLayout.removeAllFragments();
            mTabLayout.removeAllViews();
            mTabLayout.removeOnTabSelectedListener(this);
            if (mBookmarksDialogListener != null) {
                mBookmarksDialogListener.onBookmarksDialogDismissed(mTabLayout.getSelectedTabPosition());
            }
        }
    }

    /**
     * The overloaded implementation of {@link TabLayout.OnTabSelectedListener#onTabSelected(TabLayout.Tab)}.
     */
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        setToolbarTitleBySelectedTab((String) tab.getTag());
        Drawable icon = tab.getIcon();
        if (icon != null) {
            icon.setAlpha(255);
        }
    }

    /**
     * The overloaded implementation of {@link TabLayout.OnTabSelectedListener#onTabUnselected(TabLayout.Tab)}.
     */
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        Drawable icon = tab.getIcon();
        if (icon != null) {
            icon.setAlpha(127);
        }
    }

    /**
     * The overloaded implementation of {@link TabLayout.OnTabSelectedListener#onTabReselected(TabLayout.Tab)}.
     */
    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }
}
