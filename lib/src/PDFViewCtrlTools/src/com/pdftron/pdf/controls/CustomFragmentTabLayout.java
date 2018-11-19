//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.util.Log;

import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

/**
 * This class implements {@link TabLayout} where each tab is attached with a fragment
 */
public class CustomFragmentTabLayout extends TabLayout
    implements TabLayout.OnTabSelectedListener {

    private final static String TAG = CustomFragmentTabLayout.class.getName();
    private static boolean sDebug;

    protected Context mContext;
    protected FragmentManager mFragmentManager;
    protected final ArrayList<TabInfo> mTabs = new ArrayList<>();
    protected int mContainerId;

    protected TabInfo mLastTabInfo;
    protected boolean mShouldMemorizeTab = true;

    private final ArrayList<OnTabSelectedListener> mSelectedListeners = new ArrayList<>();

    static final class TabInfo {
        final Bundle mArgs;
        final Class<?> mClass;
        String mTag;
        Fragment mFragment;

        TabInfo(String tag, Class<?> _class, Bundle args) {
            mTag = tag;
            mClass = _class;
            mArgs = args;
        }
    }

    private static class SavedState extends BaseSavedState {
        String curTab;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            curTab = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(curTab);
        }

        @Override
        public String toString() {
            return "FragmentTabLayout.SavedState{"
                + Integer.toHexString(System.identityHashCode(this))
                + " curTab=" + curTab + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
            = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * Class constructor
     */
    public CustomFragmentTabLayout(Context context) {
        // Note that we call through to the version that takes an AttributeSet,
        // because the simple Context construct can result in a broken object!
        this(context, null);
    }

    /**
     * Class constructor
     */
    public CustomFragmentTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Class constructor
     */
    public CustomFragmentTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.addOnTabSelectedListener(this);
    }

    /**
     * The overloaded implementation of {@link TabLayout#onSaveInstanceState()}.
     **/
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        if (mShouldMemorizeTab) {
            ss.curTab = getCurrentTabTag();
        }
        return ss;
    }

    /**
     * The overloaded implementation of {@link TabLayout#onRestoreInstanceState(Parcelable)}.
     **/
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        if (mShouldMemorizeTab) {
            Tab tab = getTabByTag(ss.curTab);
            if (tab != null) {
                tab.select();
            }
        }
    }

    /**
     * The overloaded implementation of {@link TabLayout#addOnTabSelectedListener(OnTabSelectedListener)}.
     **/
    public void addOnTabSelectedListener(@NonNull OnTabSelectedListener listener) {
        if (!mSelectedListeners.contains(listener)) {
            mSelectedListeners.add(listener);
        }
    }

    /**
     * The overloaded implementation of {@link TabLayout#removeOnTabSelectedListener(OnTabSelectedListener)}.
     **/
    public void removeOnTabSelectedListener(@NonNull OnTabSelectedListener listener) {
        mSelectedListeners.remove(listener);
    }

    /**
     * The overloaded implementation of {@link TabLayout#clearOnTabSelectedListeners()}.
     **/
    public void clearOnTabSelectedListeners() {
        mSelectedListeners.clear();
    }

    /**
     * The overloaded implementation of {@link TabLayout.OnTabSelectedListener#onTabSelected(Tab)}.
     **/
    @Override
    public void onTabSelected(Tab tab) {
        if (tab == null || tab.getTag() == null) {
            return;
        }

        startFragment((String) tab.getTag());

        for (int i = mSelectedListeners.size() - 1; i >= 0; --i) {
            mSelectedListeners.get(i).onTabSelected(tab);
        }
    }

    /**
     * The overloaded implementation of {@link TabLayout.OnTabSelectedListener#onTabUnselected(Tab)}.
     **/
    @Override
    public void onTabUnselected(Tab tab) {
        if (tab == null) {
            return;
        }

        for (int i = mSelectedListeners.size() - 1; i >= 0; --i) {
            mSelectedListeners.get(i).onTabUnselected(tab);
        }
    }

    /**
     * The overloaded implementation of {@link TabLayout.OnTabSelectedListener#onTabReselected(Tab)}.
     **/
    @Override
    public void onTabReselected(Tab tab) {
        if (tab == null) {
            return;
        }

        for (int i = mSelectedListeners.size() - 1; i >= 0; --i) {
            mSelectedListeners.get(i).onTabReselected(tab);
        }
    }

    /**
     * Setups the class.
     *
     * @param context     The context
     * @param manager     The fragment manager
     * @param containerId The container ID
     */
    public void setup(Context context, FragmentManager manager, int containerId) {
        mContext = context;
        mFragmentManager = manager;
        mContainerId = containerId;
    }

    /**
     * Adds a new tab.
     *
     * @param tab    The tab
     * @param _class The class of fragment that should be attached to this tab
     * @param args   The arguments that should be passed to the fragment
     */
    public void addTab(@NonNull Tab tab, Class<?> _class, Bundle args) {
        String tag = (String) tab.getTag();
        if (Utils.isNullOrEmpty(tag)) {
            return;
        }

        TabInfo tabInfo = new TabInfo(tag, _class, args);
        mTabs.add(tabInfo);
        addTab(tab, false);
    }

    /**
     * Start the fragment that has the specified tag name.
     *
     * @param tag The tag name of the fragment
     */
    public void startFragment(String tag) {
        if (tag == null) {
            return;
        }

        TabInfo newTabInfo = null;
        for (int i = 0, sz = mTabs.size(); i < sz; i++) {
            TabInfo tabInfo = mTabs.get(i);
            if (tabInfo.mTag.equals(tag)) {
                newTabInfo = tabInfo;
                break;
            }
        }

        if (newTabInfo == null) {
            throw new IllegalStateException("No tab known for tag " + tag);
        }

        if (mLastTabInfo != newTabInfo) {
            if (sDebug)
                Log.d(TAG, "start fragment " + newTabInfo.mTag);

            FragmentTransaction ft = mFragmentManager.beginTransaction();

            if (mLastTabInfo != null && mLastTabInfo.mFragment != null) {
                ft.hide(mLastTabInfo.mFragment);
            }

            boolean isAdded = false;
            if (newTabInfo.mFragment == null) {
                Fragment fragmentInManager = null;
                for (Fragment fragment : mFragmentManager.getFragments()) {
                    if (fragment instanceof PdfViewCtrlTabFragment) {
                        String tabTag = ((PdfViewCtrlTabFragment) fragment).getTabTag();
                        if (newTabInfo.mTag.equals(tabTag)) {
                            fragmentInManager = fragment;
                            break;
                        }
                    }
                }
                if (fragmentInManager != null) {
                    newTabInfo.mFragment = fragmentInManager;
                } else {
                    newTabInfo.mFragment = Fragment.instantiate(mContext,
                        newTabInfo.mClass.getName(), newTabInfo.mArgs);
                    ft.add(mContainerId, newTabInfo.mFragment);
                    isAdded = true;
                }
            }

            if (!isAdded) {
                if (newTabInfo.mFragment.isHidden()) {
                    ft.show(newTabInfo.mFragment);
                } else if (!newTabInfo.mFragment.isDetached()) {
                    // when closing background tabs, the current tab is not hidden
                    // but it is not detached either
                    // we just need to keep it showing
                    ft.show(newTabInfo.mFragment);
                } else {
                    ft.attach(newTabInfo.mFragment);
                }
            }

            ft.commitAllowingStateLoss();

            mLastTabInfo = newTabInfo;
        }
    }

    /**
     * The overloaded implementation of {@link TabLayout#removeTab(Tab)}.
     * <p>
     * <div class="warning">
     * Attached fragment to the specified tab is also removed from the fragment manager.
     * </div>
     **/
    @Override
    public void removeTab(Tab tab) {
        String tag = (String) tab.getTag();

        if (Utils.isNullOrEmpty(tag)) {
            return;
        }

        TabInfo curTabInfo = null;
        for (int i = 0, sz = mTabs.size(); i < sz; i++) {
            TabInfo tabInfo = mTabs.get(i);
            if (tabInfo.mTag.equals(tag)) {
                curTabInfo = tabInfo;
                break;
            }
        }

        if (curTabInfo != null) {
            if (curTabInfo.mFragment != null) {
                FragmentTransaction ft = mFragmentManager.beginTransaction();
                ft.remove(curTabInfo.mFragment);
                ft.commitAllowingStateLoss();
            }

            mTabs.remove(curTabInfo);
        }

        super.removeTab(tab);
    }

    /**
     * Remove all fragments.
     */
    public void removeAllFragments() {
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        for (TabInfo tabInfo : mTabs) {
            if (tabInfo.mFragment != null) {
                ft.remove(tabInfo.mFragment);
            }
        }
        ft.commitAllowingStateLoss();
    }

    /**
     * Returns the tab that has the specified tag name.
     *
     * @param tag The tag name of the tab
     * @return The tab that has the specified tag name
     */
    public Tab getTabByTag(String tag) {
        Tab tab = null;
        if (tag != null) {
            for (int i = 0, sz = getTabCount(); i < sz; ++i) {
                Tab tab_i = getTabAt(i);
                if (tab_i != null) {
                    String tag_i = (String) tab_i.getTag();
                    if (tag_i != null && tag.equals(tag_i)) {
                        tab = tab_i;
                        break;
                    }
                }
            }
        }
        return tab;
    }

    /**
     * Replaces the tag name of an existing tab.
     *
     * @param tab    The tab
     * @param newTag the new tag name
     */
    public void replaceTag(@NonNull Tab tab, @NonNull String newTag) {
        TabInfo curTabInfo = null;
        String oldTag = (String) tab.getTag();
        for (int i = 0, sz = mTabs.size(); i < sz; i++) {
            TabInfo tabInfo = mTabs.get(i);
            if (tabInfo.mTag.equals(oldTag)) {
                curTabInfo = tabInfo;
                break;
            }
        }

        if (curTabInfo == null) {
            return;
        }

        curTabInfo.mTag = newTag;
        tab.setTag(newTag);
    }

    /**
     * Returns the tag name of the current fragment.
     *
     * @return The tag name of the current fragment
     */
    protected String getCurrentTabTag() {
        int selectedTabPosition = getSelectedTabPosition();
        if (selectedTabPosition == -1) {
            return null;
        }
        Tab tab = getTabAt(selectedTabPosition);
        return tab == null ? null : (String) tab.getTag();
    }

    /**
     * Returns a fragment that has the specified tag name.
     *
     * @param tag The tag name of the fragment
     * @return A fragment that has the specified tag name
     */
    public Fragment getFragmentByTag(String tag) {
        if (tag != null) {
            for (int i = 0, sz = mTabs.size(); i < sz; i++) {
                TabInfo tabInfo = mTabs.get(i);
                if (tabInfo.mTag.equals(tag)) {
                    return tabInfo.mFragment;
                }
            }
        }

        return null;
    }

    /**
     * Returns the current fragment.
     *
     * @return the current fragment
     */
    public Fragment getCurrentFragment() {
        return getFragmentByTag(getCurrentTabTag());
    }

    /**
     * Returns fragments that are alive.
     *
     * @return a list of live fragments
     */
    public ArrayList<Fragment> getLiveFragments() {
        ArrayList<Fragment> fragments = new ArrayList<>();
        for (int i = 0, sz = mTabs.size(); i < sz; i++) {
            TabInfo tabInfo = mTabs.get(i);
            if (tabInfo.mFragment != null) {
                fragments.add(tabInfo.mFragment);
            }
        }

        return fragments;
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }
}
