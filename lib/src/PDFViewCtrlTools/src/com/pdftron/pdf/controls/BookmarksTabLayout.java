//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MenuItem;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;

/**
 * This class provides a tab layout having three tabs including document outline, user-defined bookmarks, and annotations in the document
 */
public class BookmarksTabLayout extends CustomFragmentTabLayout implements
    UserBookmarkDialogFragment.UserBookmarkDialogListener,
    OutlineDialogFragment.OutlineDialogListener,
    AnnotationDialogFragment.AnnotationDialogListener {

    private final static String TAG = BookmarksTabLayout.class.getName();
    private static boolean sDebug;

    public final static String TAG_TAB_OUTLINE = "tab-outline";
    public final static String TAG_TAB_ANNOTATION = "tab-annotation";
    public final static String TAG_TAB_BOOKMARK = "tab-bookmark";

    private PDFViewCtrl mPdfViewCtrl;
    private Bookmark mCurrentBookmark;

    private BookmarksTabsListener mBookmarksTabsListener;
    private NavigationListDialogFragment.AnalyticsEventListener mAnalyticsEventListener;
    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;
    private boolean mTabSelectInitialized;

    private TabLayoutOnPageChangeListener mPageChangeListener;

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface BookmarksTabsListener {
        /**
         * Called when a user bookmark has been clicked.
         *
         * @param pageNum The page number
         */
        void onUserBookmarkClick(int pageNum);

        /**
         * Callback interface invoked when an outline has been clicked
         *
         * @param parent The parent bookmark if any
         * @param bookmark The clicked bookmark
         */
        void onOutlineClicked(Bookmark parent, Bookmark bookmark);

        /**
         * Called when an annotation has been clicked.
         *
         * @param annotation The annotation
         * @param pageNum The page number that holds the annotation
         */
        void onAnnotationClicked(Annot annotation, int pageNum);

        /**
         * Called when document annotations have been exported.
         *
         * @param outputDoc The PDFDoc containing the exported annotations
         */
        void onExportAnnotations(PDFDoc outputDoc);
    }

    /**
     * Class constructor
     */
    public BookmarksTabLayout(Context context) {
        // Note that we call through to the version that takes an AttributeSet,
        // because the simple Context construct can result in a broken object!
        this(context, null);
    }

    /**
     * Class constructor
     */
    public BookmarksTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Class constructor
     */
    public BookmarksTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mShouldMemorizeTab = false;
    }

    /**
     * use {@link #setup(Context, FragmentManager, int, PDFViewCtrl, Bookmark)}
     */
    @Override
    public void setup(Context context, FragmentManager manager, int containerId) {
        throw new IllegalStateException(
            "Must call setup() that takes also a PDFViewCtrl, a Bookmark, and a String");
    }

    /**
     * Setups the class.
     *
     * @param context         The context
     * @param manager         The fragment manager
     * @param containerId     The container ID
     * @param pdfViewCtrl     The {@link PDFViewCtrl}
     * @param currentBookmark The {@link Bookmark}
     */
    public void setup(Context context, FragmentManager manager, int containerId,
                      PDFViewCtrl pdfViewCtrl, Bookmark currentBookmark) {
        super.setup(context, manager, containerId);

        mPdfViewCtrl = pdfViewCtrl;
        mCurrentBookmark = currentBookmark;
    }

    /**
     * Sets the listener
     *
     * @param listener The listener
     */
    public void setBookmarksTabsListener(BookmarksTabsListener listener) {
        mBookmarksTabsListener = listener;
    }

    /**
     * Sets the listener to {@link NavigationListDialogFragment.AnalyticsEventListener}
     *
     * @param listener The listener
     */
    public void setAnalyticsEventListener(NavigationListDialogFragment.AnalyticsEventListener listener) {
        mAnalyticsEventListener = listener;
    }

    /**
     * @hide
     * The overloaded implementation of {@link CustomFragmentTabLayout#startFragment(String)}.
     **/
    @Override
    public void startFragment(String tag) {

    }

    private Fragment getTabFragmentAt(int position) {
        if (mPdfViewCtrl == null) {
            return null;
        }

        TabInfo tabInfo = mTabs.get(position);

        switch (tabInfo.mTag) {
            case TAG_TAB_OUTLINE: {
                OutlineDialogFragment fragment;
                try {
                    fragment = (OutlineDialogFragment) tabInfo.mClass.newInstance();
                } catch (Exception e) {
                    // if the tab info fragment class instantiation failed
                    // use default newInstance()
                    // in this case, child class won't be instantiated
                    fragment = OutlineDialogFragment.newInstance();
                }
                fragment.setPdfViewCtrl(mPdfViewCtrl)
                    .setCurrentBookmark(mCurrentBookmark)
                    .setArguments(tabInfo.mArgs);
                fragment.setOutlineDialogListener(this);
                tabInfo.mFragment = fragment;
                break;
            }
            case TAG_TAB_ANNOTATION: {
                AnnotationDialogFragment fragment;
                try {
                    fragment = (AnnotationDialogFragment) tabInfo.mClass.newInstance();
                } catch (Exception e) {
                    // if the tab info fragment class instantiation failed
                    // use default newInstance()
                    // in this case, child class won't be instantiated
                    fragment = AnnotationDialogFragment.newInstance();
                }
                fragment.setPdfViewCtrl(mPdfViewCtrl)
                    .setArguments(tabInfo.mArgs);
                fragment.setAnnotationDialogListener(this);
                tabInfo.mFragment = fragment;
                break;
            }
            case TAG_TAB_BOOKMARK: {
                UserBookmarkDialogFragment fragment;
                try {
                    fragment = (UserBookmarkDialogFragment) tabInfo.mClass.newInstance();
                } catch (Exception e) {
                    // if the tab info fragment class instantiation failed
                    // use default newInstance()
                    // in this case, child class won't be instantiated
                    fragment = UserBookmarkDialogFragment.newInstance();
                }
                fragment.setPdfViewCtrl(mPdfViewCtrl)
                    .setArguments(tabInfo.mArgs);
                fragment.setUserBookmarkListener(this);
                tabInfo.mFragment = fragment;
                break;
            }
            default:
                tabInfo.mFragment = Fragment.instantiate(mContext,
                    tabInfo.mClass.getName(), tabInfo.mArgs);
                break;
        }
        if (tabInfo.mFragment instanceof NavigationListDialogFragment) {
            ((NavigationListDialogFragment) tabInfo.mFragment).setAnalyticsEventListener(
                new NavigationListDialogFragment.AnalyticsEventListener() {
                    @Override
                    public void onEventAction() {
                        if (mAnalyticsEventListener != null) {
                            mAnalyticsEventListener.onEventAction();
                        }
                    }
                });
        }
        return tabInfo.mFragment;
    }

    @Override
    public void onOutlineClicked(Bookmark parent, Bookmark bookmark) {
        if (mBookmarksTabsListener != null) {
            mBookmarksTabsListener.onOutlineClicked(parent, bookmark);
        }
    }

    @Override
    public void onAnnotationClicked(Annot annotation, int pageNum) {
        if (mBookmarksTabsListener != null) {
            mBookmarksTabsListener.onAnnotationClicked(annotation, pageNum);
        }
    }

    @Override
    public void onExportAnnotations(PDFDoc outputDoc) {
        if (mBookmarksTabsListener != null) {
            mBookmarksTabsListener.onExportAnnotations(outputDoc);
        }
    }

    @Override
    public void onUserBookmarkClicked(int pageNum) {
        if (mBookmarksTabsListener != null) {
            mBookmarksTabsListener.onUserBookmarkClick(pageNum);
        }
    }

    /**
     * Called when an annotation menu item has been clicked.
     *
     * @param item The menu item that was clicked
     */
    public void onAnnotationMenuItemClicked(MenuItem item) {
        if (mLastTabInfo.mFragment instanceof AnnotationDialogFragment) {
            AnnotationDialogFragment annotationFrag = (AnnotationDialogFragment) mLastTabInfo.mFragment;
            annotationFrag.onToolbarMenuItemClicked(item);
        }
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }

    @Override
    public void setupWithViewPager(@Nullable ViewPager viewPager) {
        if (viewPager == null) {
            return;
        }

        if (mPageChangeListener != null && mViewPager != null) {
            mViewPager.removeOnPageChangeListener(mPageChangeListener);
        }

        mViewPager = viewPager;
        if (mPagerAdapter == null) {
            mPagerAdapter = new BookmarkViewPagerAdapter(mFragmentManager);
        }
        mViewPager.setAdapter(mPagerAdapter);

        if (mPageChangeListener == null) {
            mPageChangeListener = new TabLayoutOnPageChangeListener(this);
        }
        mViewPager.addOnPageChangeListener(mPageChangeListener);

    }

    @Override
    public void onTabSelected(Tab tab) {
        super.onTabSelected(tab);
        if (mViewPager != null) {
            mViewPager.setCurrentItem(tab.getPosition());
        }
        if (mTabSelectInitialized) {
            // this is not considered as "noaction = false", because the use has not done something useful by selecting a tab
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATION_LISTS_CHANGE,
                AnalyticsParam.navigationListsTabParam(getNavigationId(tab)));
        } else {
            mTabSelectInitialized = true;
        }
    }

    @Override
    public void addTab(@NonNull Tab tab, Class<?> _class, Bundle args) {
        super.addTab(tab, _class, args);
        if (mPagerAdapter != null) {
            mPagerAdapter.notifyDataSetChanged();
        }
    }

    static public int getNavigationId(@Nullable Tab tab) {
        if (tab != null && tab.getTag() instanceof String) {
            String tag = (String) tab.getTag();
            switch (tag) {
                case TAG_TAB_OUTLINE:
                    return AnalyticsHandlerAdapter.NAVIGATION_TAB_OUTLINE;
                case TAG_TAB_ANNOTATION:
                    return AnalyticsHandlerAdapter.NAVIGATION_TAB_ANNOTATIONS;
                case TAG_TAB_BOOKMARK:
                    return AnalyticsHandlerAdapter.NAVIGATION_TAB_USER_BOOKMARKS;
            }
        }
        return 0;
    }

    private class BookmarkViewPagerAdapter extends FragmentStatePagerAdapter {

        BookmarkViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            return getTabFragmentAt(position);
        }
    }
}
