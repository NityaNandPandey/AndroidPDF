//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.ColorPickerGridViewAdapter;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link DialogFragment} for adding/ editing colors to Favorites.
 * It contains two pages: a standard color pages contains recent colors and standard preset colors;
 * And a {@link AdvancedColorView} page.
 * The favorite colors will be displayed in {@link CustomColorPickerView}
 */
public class FavoriteColorDialogFragment extends DialogFragment
    implements ViewPager.OnPageChangeListener,
    TabLayout.OnTabSelectedListener {

    private static final String TAG = FavoriteColorDialogFragment.class.getName();

    @IntDef({ADD_COLOR, EDIT_COLOR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FavoriteDialogMode {
    }

    /**
     * The favorite color mode will be adding colors to favorites
     */
    public static final int ADD_COLOR = 0;

    /**
     * The favorite color mode will be editing selected color in favorites
     */
    public static final int EDIT_COLOR = 1;

    /**
     * The key in {@link #getArguments()} for getting dialog mode value
     */
    public static final String FAVORITE_DIALOG_MODE = "favDialogMode";

    /**
     * Creates a new instance of favorite dialog mode
     *
     * @param bundle
     * @return
     */
    public static FavoriteColorDialogFragment newInstance(Bundle bundle) {
        FavoriteColorDialogFragment fragment = new FavoriteColorDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    private ViewPager mViewPager;
    private View mStandardColorTab;
    private View mAdvancedColorTab;
    private TabLayout mTablayout;
    private AdvancedColorView mAdvancedColorPicker;
    private Button mAddColorButton;
    private GridView mAddedColorsGridView;
    private ColorPickerGridViewAdapter mAddedColorsAdapter;
    private Button mFinishButton;
    private GridView mRecentColors;
    private PresetColorGridView mPresetColors;
    private OnEditFinishedListener mFinishedListener;
    private int mSelectedColor = Color.BLACK;
    private ArrayList<String> mFavoriteColors;
    private int mDialogMode = ADD_COLOR;
    private HashMap<String, Integer> mSelectedColorLabels;
    private ArrayList<String> mSelectedColors;

    /**
     * Class constructor
     */
    public FavoriteColorDialogFragment() {
        mSelectedColors = new ArrayList<>();
        mSelectedColorLabels = new HashMap<>();
    }

    /**
     * The overload implementation of {@link DialogFragment#onCreateDialog(Bundle)}
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog d = new Dialog(getActivity(), R.style.FullScreenDialogStyle);
        if (d.getWindow() == null) {
            return d;
        }
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(d.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        d.getWindow().setAttributes(lp);
        return d;
    }

    /**
     * The overload implementation of {@link DialogFragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_favorite_color, container, false);

        mStandardColorTab = inflater.inflate(R.layout.controls_add_favorite_standard, null);
        mAdvancedColorTab = inflater.inflate(R.layout.controls_add_favorite_advanced, null);

        mAdvancedColorPicker = mAdvancedColorTab.findViewById(R.id.advanced_color_picker);
        mAdvancedColorPicker.setSelectedColor(mSelectedColor);
        mAdvancedColorPicker.setOnColorChangeListener(new ColorPickerView.OnColorChangeListener() {
            @Override
            public void OnColorChanged(View view, int color) {
                onAdvancedColorChanged(view, color);
            }
        });

        mAddColorButton = mAdvancedColorTab.findViewById(R.id.add_color_btn);
        mAddColorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAddColorButtonClicked(view);
            }
        });

        mAddedColorsGridView = mAdvancedColorTab.findViewById(R.id.added_colors);
        mAddedColorsAdapter = new ColorPickerGridViewAdapter(getActivity(), new ArrayList<String>());
        mAddedColorsGridView.setAdapter(mAddedColorsAdapter);
        mAddedColorsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onGridItemClicked(adapterView, view, i, l);
            }
        });

        ImageButton closeButton = view.findViewById(R.id.close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCloseButtonClicked(view);
            }
        });

        mFinishButton = view.findViewById(R.id.finish_btn);
        mFinishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCloseButtonClicked(view);
            }
        });
        TextView toolbarTitleTextView = view.findViewById(R.id.toolbar_title);

        mRecentColors = mStandardColorTab.findViewById(R.id.recent_colors);

        // preset colors
        mPresetColors = mStandardColorTab.findViewById(R.id.preset_colors);
        mPresetColors.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onGridItemClicked(adapterView, view, i, l);
            }
        });
        ArrayList<String> recentColors = new ArrayList<>();
        // get arguments
        if (getArguments() != null) {

            // arguments contains key recent colors
            if (getArguments().containsKey(CustomColorPickerView.KEY_RECENT_COLORS)) {
                recentColors = getArguments().getStringArrayList(CustomColorPickerView.KEY_RECENT_COLORS);
            }

            // arguments contains key favorite colors
            if (getArguments().containsKey(CustomColorPickerView.KEY_FAVORITE_COLORS)) {
                mFavoriteColors = getArguments().getStringArrayList(CustomColorPickerView.KEY_FAVORITE_COLORS);
                mFavoriteColors = (ArrayList<String>) ColorPickerGridViewAdapter.getListLowerCase(mFavoriteColors);
                mSelectedColors.addAll(mFavoriteColors);
                mPresetColors.getAdapter().setDisabledColorList(mFavoriteColors);
            }

            // arguments contains key favorite dialog mode
            if (getArguments().containsKey(FAVORITE_DIALOG_MODE)) {
                mDialogMode = getArguments().getInt(FAVORITE_DIALOG_MODE);
                if (mDialogMode == EDIT_COLOR) {
                    toolbarTitleTextView.setText(R.string.controls_fav_color_editor_edit_color);
                    mAddedColorsAdapter.setMaxSize(1);
                    mSelectedColors.clear();
                    mAddColorButton.setText(R.string.controls_fav_color_editor_select_color);
                }
            }
        }
        if (mDialogMode == ADD_COLOR) {
            mPresetColors.getAdapter().setFavoriteList(mSelectedColors);
            mAddedColorsAdapter.setFavoriteList(mSelectedColors);
        } else {
            mPresetColors.getAdapter().setFavoriteList(mFavoriteColors);
            mAddedColorsAdapter.setFavoriteList(mFavoriteColors);
            mPresetColors.getAdapter().setSelectedList(mSelectedColors);
            mAddedColorsAdapter.setSelectedList(mSelectedColors);
        }

        if (recentColors == null || recentColors.isEmpty()) {
            mStandardColorTab.findViewById(R.id.recent_colors_title).setVisibility(View.GONE);
            mRecentColors.setVisibility(View.GONE);
        } else {
            mStandardColorTab.findViewById(R.id.recent_colors_title).setVisibility(View.VISIBLE);
            mRecentColors.setVisibility(View.VISIBLE);
            mRecentColors.setAdapter(new ColorPickerGridViewAdapter(getActivity(), recentColors));
            ((ColorPickerGridViewAdapter) mRecentColors.getAdapter()).setDisabledColorList(mFavoriteColors);
            if (mDialogMode == EDIT_COLOR) {
                ((ColorPickerGridViewAdapter) mRecentColors.getAdapter()).setSelectedList(mSelectedColors);
            }
            ((ColorPickerGridViewAdapter) mRecentColors.getAdapter()).setFavoriteList(mSelectedColors);
            mRecentColors.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    onGridItemClicked(adapterView, view, i, l);
                }
            });
        }

        mViewPager = view.findViewById(R.id.view_pager);
        mTablayout = view.findViewById(R.id.tab_layout);
        mViewPager.setAdapter(new FavoriteViewPagerAdapter());
        mViewPager.addOnPageChangeListener(this);
        mTablayout.addOnTabSelectedListener(this);

        // select tab
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("selectedTab")) {
                int selectedIndex = savedInstanceState.getInt("selectedTab");
                TabLayout.Tab tab = mTablayout.getTabAt(selectedIndex);
                if (tab != null) {
                    tab.select();
                }
                mViewPager.setCurrentItem(selectedIndex);
            }
        }
        // set each tab color, set icon color
        int tabCount = mTablayout.getTabCount();
        for (int i = 0; i < tabCount; i++) {
            TabLayout.Tab tab = mTablayout.getTabAt(i);
            if (tab == null) {
                continue;
            }
            Drawable icon = tab.getIcon();
            if (icon == null) {
                continue;
            }
            icon.mutate();
            icon.setColorFilter(getActivity().getResources().getColor(android.R.color.primary_text_dark), PorterDuff.Mode.SRC_IN);
            if (i != mTablayout.getSelectedTabPosition()) {
                icon.setAlpha(137);
            } else {
                icon.setAlpha(255);
            }
        }
        return view;
    }

    /**
     * The overload implementation of {@link DialogFragment#onSaveInstanceState(Bundle)}
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selectedTab", mTablayout.getSelectedTabPosition());
    }


    private void addFavoriteColor(String color, int labelFrom) {
        if (mDialogMode == ADD_COLOR || mSelectedColors.isEmpty()) {
            mSelectedColors.add(color.toLowerCase());
            mSelectedColorLabels.put(color.toLowerCase(), labelFrom);
        } else {
            mSelectedColors.set(0, color.toLowerCase());
            mSelectedColorLabels.clear();
            mSelectedColorLabels.put(color.toLowerCase(), labelFrom);
        }
        if (mAddedColorsGridView.getVisibility() == View.INVISIBLE) {
            mAddedColorsGridView.setVisibility(View.VISIBLE);
        }
        mFinishButton.setVisibility(View.VISIBLE);
        if (mRecentColors.getAdapter() != null) {
            ((ColorPickerGridViewAdapter) mRecentColors.getAdapter()).notifyDataSetChanged();
        }
        mPresetColors.getAdapter().notifyDataSetChanged();
    }

    private void removeFavoriteColor(String color) {
        mSelectedColors.remove(color.toLowerCase());
        mSelectedColorLabels.remove(color.toLowerCase());
        mAddedColorsAdapter.removeItem(color);
        if (mAddedColorsAdapter.getCount() == 0) {
            mAddedColorsGridView.setVisibility(View.INVISIBLE);
        }

        if (mSelectedColors.equals(mFavoriteColors)) {
            mFinishButton.setVisibility(View.GONE);
        } else {
            mFinishButton.setVisibility(View.VISIBLE);
        }

        mPresetColors.getAdapter().notifyDataSetChanged();
        if (mRecentColors.getAdapter() != null) {
            ((ColorPickerGridViewAdapter) mRecentColors.getAdapter()).notifyDataSetChanged();
        }
        mAddedColorsAdapter.notifyDataSetChanged();
    }

    private void onAddColorButtonClicked(View v) {
        String color = Utils.getColorHexString(mAdvancedColorPicker.getColor());
        mAdvancedColorPicker.setSelectedColor(mAdvancedColorPicker.getColor());
        mAddedColorsAdapter.add(color);
        addFavoriteColor(color, AnalyticsHandlerAdapter.LABEL_DIALOG_WHEEL);
        mAddedColorsAdapter.notifyDataSetChanged();
        mAddColorButton.setEnabled(false);
    }

    private void onCloseButtonClicked(View v) {
        if (v.getId() == mFinishButton.getId()) {
            StringBuilder sb = new StringBuilder();
            for (String str : mSelectedColors) {
                sb = sb.append(str);
                if (mSelectedColors.indexOf(str) < (mSelectedColors.size() - 1)) {
                    sb = sb.append(',').append(' ');
                }
            }

            PdfViewCtrlSettingsManager.setFavoriteColors(getActivity(), sb.toString());
            for (Map.Entry<String, Integer> entry : mSelectedColorLabels.entrySet()) {

                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_ADD_FAVORITE,
                    AnalyticsParam.colorParam(entry.getKey()));
            }
            if (mFinishedListener != null) {
                mFinishedListener.onEditFinished(mSelectedColors, mDialogMode);
            }
        }
        dismiss();
    }

    private void onGridItemClicked(AdapterView<?> parent, View view, int position, long id) {
        ColorPickerGridViewAdapter adapter = (ColorPickerGridViewAdapter) parent.getAdapter();
        String item = adapter.getItem(position);
        if (item == null) {
            return;
        }
        if (parent.getId() == mAddedColorsGridView.getId()) {
            removeFavoriteColor(item);
        } else if (parent.getId() == mPresetColors.getId() || parent.getId() == mRecentColors.getId()) {
            if (adapter.isItemDisabled(item)) {
                return;
            }
            if (mSelectedColors.contains(item.toLowerCase())) {
                removeFavoriteColor(item);
            } else {
                int labelId = parent.getId() == mPresetColors.getId() ? AnalyticsHandlerAdapter.LABEL_DIALOG_STANDARD : AnalyticsHandlerAdapter.LABEL_DIALOG_RECENT;
                addFavoriteColor(item, labelId);
            }
        }
    }

    private void onAdvancedColorChanged(View view, @ColorInt int color) {
        if (mSelectedColors.contains(Utils.getColorHexString(color).toLowerCase())) {
            mAddColorButton.setEnabled(false);
        } else {
            mAddColorButton.setEnabled(true);
        }
    }

    /**
     * Dismiss this favorite dialog
     */
    @Override
    public void dismiss() {
        super.dismiss();
        mViewPager.removeOnPageChangeListener(this);
        mTablayout.removeOnTabSelectedListener(this);
    }

    /**
     * Gets selected favorite colors
     *
     * @return
     */
    public ArrayList<String> getSelectedColors() {
        return mSelectedColors;
    }

    /**
     * The overload implementation of {@link android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrolled(int, float, int)}.
     * <p>
     * This method is invoked when pager is scrolled.
     *
     * @param position             Position index of the first page currently being displayed.
     *                             Page position+1 will be visible if positionOffset is nonzero.
     * @param positionOffset       Value from [0, 1) indicating the offset from the page at position.
     * @param positionOffsetPixels Value in pixels indicating the offset from position.
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mTablayout.setScrollPosition(position, positionOffset, true);
    }

    /**
     * The overload implementation of {@link android.support.v4.view.ViewPager.OnPageChangeListener#onPageSelected(int)}.
     * <p>
     * This method will be invoked when a new page becomes selected. Animation is not
     * necessarily complete.
     *
     * @param position Position index of the new selected page.
     */
    @Override
    public void onPageSelected(int position) {
        TabLayout.Tab tab = mTablayout.getTabAt(position);
        if (tab != null) {
            tab.select();
        }
    }

    /**
     * The overload implementation of {@link android.support.v4.view.ViewPager.OnPageChangeListener#onPageSelected(int)}.
     * <p>
     * Called when the scroll state changes. Useful for discovering when the user
     * begins dragging, when the pager is automatically settling to the current page,
     * or when it is fully stopped/idle.
     *
     * @param state The new scroll state.
     */
    @Override
    public void onPageScrollStateChanged(int state) {

    }

    /**
     * Called when a tab enters the selected state.
     *
     * @param tab The tab that was selected
     */
    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        int index = tab.getPosition();
        mViewPager.setCurrentItem(index, true);
        Drawable icon = tab.getIcon();
        if (icon != null) {
            icon.mutate();
            icon.setAlpha(255);
        }
    }

    /**
     * Called when a tab exits the selected state.
     *
     * @param tab The tab that was unselected
     */
    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        Drawable icon = tab.getIcon();
        if (icon != null) {
            icon.mutate();
            icon.setAlpha(137);
        }
    }

    /**
     * Called when a tab that is already selected is chosen again by the user. Some applications
     * may use this action to return to the top level of a category.
     *
     * @param tab The tab that was reselected.
     */
    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        int index = tab.getPosition();
        mViewPager.setCurrentItem(index, true);
        Drawable icon = tab.getIcon();
        if (icon != null) {
            icon.mutate();
            icon.setAlpha(255);
        }
    }

    /**
     * Sets selected color for advanced color picker
     *
     * @param color the color
     */
    public void setSelectedColor(@ColorInt int color) {
        mSelectedColor = color;
        if (mAdvancedColorPicker != null) {
            mAdvancedColorPicker.setSelectedColor(mSelectedColor);
        }
    }

    /**
     * Sets on finish editing favorite colors listener
     *
     * @param listener finished editing favorite color listener
     */
    public void setOnEditFinishedListener(OnEditFinishedListener listener) {
        mFinishedListener = listener;
    }


    /**
     * Favorite dialog view pager adapter for displaying standard color view and advanced color view.
     */
    protected class FavoriteViewPagerAdapter extends PagerAdapter {

        /**
         * The overload implementation of {@link PagerAdapter#getCount()}
         *
         * @return value 2
         */
        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        /**
         * Creates the page for the given position.  The adapter is responsible
         * for adding the view to the container given here, although it only
         * must ensure this is done by the time it returns from
         * {@link #finishUpdate(ViewGroup)}.
         *
         * @param container The containing View in which the page will be shown.
         * @param position  The page position to be instantiated.
         * @return Returns an Object representing the new page.  This does not
         * need to be a View, but can be some other container of the page.
         */
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view;
            switch (position) {
                case 0:
                    view = mStandardColorTab;
                    break;
                default:
                    view = mAdvancedColorTab;
                    break;
            }
            container.addView(view);
            return view;
        }

        /**
         * Remove a page for the given position.  The adapter is responsible
         * for removing the view from its container, although it only must ensure
         * this is done by the time it returns from {@link #finishUpdate(ViewGroup)}.
         *
         * @param container The containing View from which the page will be removed.
         * @param position  The page position to be removed.
         * @param object    The same object that was returned by
         *                  {@link #instantiateItem(View, int)}.
         */
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        /**
         * This method may be called by the ViewPager to obtain a title string
         * to describe the specified page. This method may return null
         * indicating no title for this page. The default implementation returns
         * null.
         *
         * @param position The position of the title requested
         * @return A title for the requested page
         */
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "standard";
                default:
                    return "advanced";
            }
        }
    }


    /**
     * This interface is for listening whether editing favorites is finished event
     */
    public interface OnEditFinishedListener {
        /**
         * This method is invoked when editing favorite color is finished
         *
         * @param colors     selected colors
         * @param dialogMode dialog mode, one of {@link #ADD_COLOR} or {@link #EDIT_COLOR}
         */
        void onEditFinished(ArrayList<String> colors, @FavoriteDialogMode int dialogMode);
    }

}
