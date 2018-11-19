//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsAnnotStylePicker;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.AnnotationPropertyPreviewView;
import com.pdftron.pdf.utils.ColorPickerGridViewAdapter;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.CustomViewPager;

import java.util.ArrayList;

import static com.pdftron.pdf.controls.AnnotStyleDialogFragment.COLOR;
import static com.pdftron.pdf.controls.AnnotStyleDialogFragment.FILL_COLOR;
import static com.pdftron.pdf.controls.AnnotStyleDialogFragment.STROKE_COLOR;
import static com.pdftron.pdf.controls.AnnotStyleDialogFragment.TEXT_COLOR;

/**
 * A Linear layout for changing annotation color. It contains a annotation preview
 * and three pages: {@link CustomColorPickerView}, {@link PresetColorGridView}, and {@link AdvancedColorView}.
 */
public class ColorPickerView extends LinearLayout implements CustomColorPickerView.OnEditFavoriteColorListener {
    // UI
    private LinearLayout mToolbar;
    private ImageButton mBackButton;
    private TextView mToolbarTitle;
    private ImageButton mEditButton;
    private ImageButton mRemoveButton;
    private ImageButton mAddFavButton;
    private CustomViewPager mColorPager;
    private CharSequence mToolbarText;
    // View Pager views
    private PresetColorGridView mPresetColorView;
    private CustomColorPickerView mCustomColorView;
    private AdvancedColorView mAdvancedColorView;
    private TabLayout mPagerIndicator;
    private String mLatestAdvancedColor;
    private AnnotStyle.AnnotStyleHolder mAnnotStyleHolder;

    private OnBackButtonPressedListener mBackPressedListener;

    private @AnnotStyleDialogFragment.SelectColorMode
    int mColorMode = AnnotStyleDialogFragment.COLOR;

    private ArrayList<String> mSelectedAddFavoriteColors;
    private ColorPickerGridViewAdapter mAddFavoriteAdapter;

    /**
     * Class constructor
     */
    public ColorPickerView(Context context) {
        this(context, null);
    }

    /**
     * Class constructor
     */
    public ColorPickerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Class constructor
     */
    public ColorPickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.color_picker_layout, this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setOrientation(VERTICAL);
        mToolbar = findViewById(R.id.toolbar);
        mBackButton = findViewById(R.id.back_btn);
        mBackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackButtonPressed();
            }
        });
        mToolbarTitle = findViewById(R.id.toolbar_title);
        mColorPager = findViewById(R.id.color_pager);
        mPagerIndicator = findViewById(R.id.pager_indicator_tabs);
        mPresetColorView = new PresetColorGridView(getContext());
        mAdvancedColorView = new AdvancedColorView(getContext());
        mCustomColorView = new CustomColorPickerView(getContext());
        mRemoveButton = mToolbar.findViewById(R.id.remove_btn);
        mEditButton = mToolbar.findViewById(R.id.edit_btn);
        mAddFavButton = mToolbar.findViewById(R.id.fav_btn);
        mEditButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mCustomColorView.editSelectedColor();
            }
        });
        mRemoveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mCustomColorView.deleteAllSelectedFavColors();
            }
        });
        mAddFavButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addColorsToFavorites();
            }
        });
//        setBackgroundColor(Utils.getThemeAttrColor(getContext(), android.R.attr.colorBackground));

        // layout params
        MarginLayoutParams mlp = new MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mPresetColorView.setLayoutParams(mlp);
        mPresetColorView.setClipToPadding(false);

        mColorPager.setAdapter(new ColorPagerAdapter());
        mColorPager.setCurrentItem(PdfViewCtrlSettingsManager.getColorPickerPage(getContext()));
        mPagerIndicator.setupWithViewPager(mColorPager, true);

        // add color change listener
        mAdvancedColorView.setOnColorChangeListener(new OnColorChangeListener() {
            @Override
            public void OnColorChanged(View view, int color) {
                onColorChanged(view, color);
            }
        });
        mCustomColorView.setOnColorChangeListener(new OnColorChangeListener() {
            @Override
            public void OnColorChanged(View view, int color) {
                onColorChanged(view, color);
            }
        });
        mCustomColorView.setOnEditFavoriteColorlistener(this);
        mCustomColorView.setRecentColorLongPressListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return onColorItemLongClickListener(parent, position);
            }
        });
    }

    /**
     * Sets annotation style holder
     *
     * @param annotStyleHolder annotation style holder
     */
    public void setAnnotStyleHolder(AnnotStyle.AnnotStyleHolder annotStyleHolder) {
        mAnnotStyleHolder = annotStyleHolder;
    }

    private AnnotStyle getAnnotStyle() {
        return mAnnotStyleHolder.getAnnotStyle();
    }

    private AnnotationPropertyPreviewView getAnnotStylePreview() {
        return mAnnotStyleHolder.getAnnotPreview();
    }

    /**
     * Show color picker view with given colorMode, must be one of:
     * {@link com.pdftron.pdf.controls.AnnotStyleDialogFragment.SelectColorMode#STROKE_COLOR},
     * {@link com.pdftron.pdf.controls.AnnotStyleDialogFragment.SelectColorMode#FILL_COLOR},
     * {@link com.pdftron.pdf.controls.AnnotStyleDialogFragment.SelectColorMode#TEXT_COLOR},
     * {@link com.pdftron.pdf.controls.AnnotStyleDialogFragment.SelectColorMode#COLOR}
     *
     * @param colorMode color mode
     */
    public void show(@AnnotStyleDialogFragment.SelectColorMode int colorMode) {
        AnalyticsAnnotStylePicker.getInstance().setSelectedColorMode(colorMode);
        mColorMode = colorMode;
        getAnnotStylePreview().setAnnotType(getAnnotStyle().getAnnotType());

        getAnnotStylePreview().updateFillPreview(getAnnotStyle());

        // don't show transparent color if the annotation has icon
        boolean presetShowTransparent;
        switch (colorMode) {
            case STROKE_COLOR:
                presetShowTransparent = true;
                setSelectedColor(getAnnotStyle().getColor());
                mToolbarTitle.setText(R.string.tools_qm_stroke_color);
                break;
            case FILL_COLOR:
                presetShowTransparent = true;
                setSelectedColor(getAnnotStyle().getFillColor());
                if (getAnnotStyle().isFreeText()) {
                    mToolbarTitle.setText(R.string.pref_colormode_custom_bg_color);
                } else {
                    mToolbarTitle.setText(R.string.tools_qm_fill_color);
                }
                break;
            case TEXT_COLOR:
                presetShowTransparent = false;
                setSelectedColor(getAnnotStyle().getTextColor());
                mToolbarTitle.setText(R.string.pref_colormode_custom_text_color);
                break;
            case COLOR:
            default:
                presetShowTransparent = getAnnotStyle().hasFillColor();
                setSelectedColor(getAnnotStyle().getColor());
                mToolbarTitle.setText(R.string.tools_qm_color);
                break;
        }
        mPresetColorView.showTransparentColor(presetShowTransparent);
        mPresetColorView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onPresetColorGridItemClicked(parent, position);
            }
        });
        mPresetColorView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return onColorItemLongClickListener(parent, position);
            }
        });
        mToolbarText = mToolbarTitle.getText();

        setVisibility(VISIBLE);
    }

    /**
     * Hide Color picker view
     */
    public void dismiss() {
        setVisibility(GONE);
    }

    private void onBackButtonPressed() {
        if (mCustomColorView.onBackButonPressed()) {
            return;
        }
        if (mSelectedAddFavoriteColors != null && !mSelectedAddFavoriteColors.isEmpty()) {
            mSelectedAddFavoriteColors.clear();
            if (mAddFavoriteAdapter != null) {
                mAddFavoriteAdapter.notifyDataSetChanged();
            }
            toggleFavoriteToolbar();
            return;
        }
        if (!Utils.isNullOrEmpty(mLatestAdvancedColor)) {
            mCustomColorView.addRecentColorSource(mLatestAdvancedColor);
            AnalyticsAnnotStylePicker.getInstance().selectColor(mLatestAdvancedColor.toUpperCase(), AnalyticsHandlerAdapter.STYLE_PICKER_COLOR_WHEEL);
        }
        if (mBackPressedListener != null) {
            mBackPressedListener.onBackPressed();
        }
    }

    /**
     * Sets selected color.
     * If selected color matches any of the color grids, it will show white check mark.
     *
     * @param color color
     */
    public void setSelectedColor(@ColorInt int color) {
        mAdvancedColorView.setSelectedColor(color);
        mPresetColorView.setSelectedColor(color);
        mCustomColorView.setSelectedColor(Utils.getColorHexString(color));
    }

    /**
     * Sets on back button pressed listener
     *
     * @param listener back button pressed listener
     */
    public void setOnBackButtonPressedListener(OnBackButtonPressedListener listener) {
        mBackPressedListener = listener;
    }

    private void onColorChanged(View view, @ColorInt int color) {
        switch (mColorMode) {
            case FILL_COLOR:
                getAnnotStyle().setFillColor(color);
                break;
            case TEXT_COLOR:
                getAnnotStyle().setTextColor(color);
                break;
            case STROKE_COLOR:
            case COLOR:
            default:
                getAnnotStyle().setStrokeColor(color);
                break;
        }
        getAnnotStylePreview().updateFillPreview(getAnnotStyle());
        String colorStr = Utils.getColorHexString(color);
        if (view != mPresetColorView) {
            mPresetColorView.setSelectedColor(colorStr);
        } else {
            AnalyticsAnnotStylePicker.getInstance().selectColor(colorStr, AnalyticsHandlerAdapter.STYLE_PICKER_STANDARD);
        }
        if (view != mCustomColorView) {
            mCustomColorView.setSelectedColor(colorStr);
        }

        String source = color == Color.TRANSPARENT ? ColorPickerGridViewAdapter.TYPE_TRANSPARENT
            : Utils.getColorHexString(color);
        if (view != mAdvancedColorView) {
            mAdvancedColorView.setSelectedColor(color);
            mCustomColorView.addRecentColorSource(source);
            mLatestAdvancedColor = "";
        } else {
            mLatestAdvancedColor = source;
        }
    }

    private void onPresetColorGridItemClicked(AdapterView<?> parent, int position) {
        if (mSelectedAddFavoriteColors != null && !mSelectedAddFavoriteColors.isEmpty()
            && onColorItemLongClickListener(parent, position)) {
            return;
        }

        String colorStr = (String) parent.getAdapter().getItem(position);
        if (colorStr == null) {
            return;
        }
        mPresetColorView.setSelectedColor(colorStr);

        int color;
        if (colorStr.equals(ColorPickerGridViewAdapter.TYPE_TRANSPARENT)) {
            color = Color.TRANSPARENT;
            onColorChanged(parent, color);
        } else {
            try {
                color = Color.parseColor(colorStr);
                onColorChanged(parent, color);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean onColorItemLongClickListener(AdapterView<?> parent, int position) {
        ColorPickerGridViewAdapter adapter = (ColorPickerGridViewAdapter) parent.getAdapter();
        String color = adapter.getItem(position);
        if (color == null) {
            return false;
        }
        if (mSelectedAddFavoriteColors == null) {
            mSelectedAddFavoriteColors = new ArrayList<>();
            adapter.setSelectedList(mSelectedAddFavoriteColors);
        }
        if (mSelectedAddFavoriteColors.contains(color)) {
            mSelectedAddFavoriteColors.remove(color);
        } else {
            mSelectedAddFavoriteColors.add(color);
        }
        adapter.notifyDataSetChanged();
        toggleFavoriteToolbar();
        mAddFavoriteAdapter = adapter;
        return true;
    }

    private void addColorsToFavorites() {
        ArrayList<String> allFavorites = new ArrayList<>(mCustomColorView.getFavoriteColors());
        allFavorites.addAll(mSelectedAddFavoriteColors);
        mCustomColorView.setColorsToFavorites(allFavorites, FavoriteColorDialogFragment.ADD_COLOR);
        for (String color : mSelectedAddFavoriteColors) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_ADD_FAVORITE,
                AnalyticsParam.colorParam(color));
        }
        onBackButtonPressed();
    }

    private void toggleFavoriteToolbar() {
        if (mSelectedAddFavoriteColors == null || mSelectedAddFavoriteColors.isEmpty()) {
            mToolbar.setBackgroundColor(Utils.getThemeAttrColor(getContext(), android.R.attr.colorBackground));
            int textColor = Utils.getThemeAttrColor(getContext(), android.R.attr.textColorPrimary);

            mToolbarTitle.setTextColor(textColor);
            mToolbarTitle.setAlpha(0.54f);
            mToolbarTitle.setText(mToolbarText);

            mAnnotStyleHolder.setAnnotPreviewVisibility(VISIBLE);
            mAddFavButton.setVisibility(GONE);
            mPagerIndicator.setVisibility(VISIBLE);
            mColorPager.setSwippingEnabled(true);
            mBackButton.setImageResource(R.drawable.ic_arrow_back_black_24dp);
            mBackButton.setColorFilter(textColor);
            mBackButton.setAlpha(0.54f);
            mSelectedAddFavoriteColors = null;
            mAddFavoriteAdapter = null;
        } else {
            mToolbar.setBackgroundColor(Utils.getAccentColor(getContext()));
            mToolbarTitle.setText(getContext().getString(R.string.controls_thumbnails_view_selected,
                Utils.getLocaleDigits(Integer.toString(mSelectedAddFavoriteColors.size()))));
            int textColor = Utils.getThemeAttrColor(getContext(), android.R.attr.textColorPrimaryInverse);
            mToolbarTitle.setTextColor(textColor);
            mToolbarTitle.setAlpha(1f);
            mAnnotStyleHolder.setAnnotPreviewVisibility(GONE);
            mBackButton.setImageResource(R.drawable.ic_close_black_24dp);
            mBackButton.setColorFilter(textColor);
            mBackButton.setAlpha(1f);
            mColorPager.setSwippingEnabled(false);
            mAddFavButton.setVisibility(VISIBLE);
            mPagerIndicator.setVisibility(INVISIBLE);
        }
    }

    /**
     * Save colors in custom color view to settings
     */
    public void saveColors() {
        mCustomColorView.saveColors();
        PdfViewCtrlSettingsManager.setColorPickerPage(getContext(), mColorPager.getCurrentItem());
    }

    /**
     * Sets activity to custom color view to show {@link FavoriteColorDialogFragment} fragment
     * when clicked in "Add favorite" button.
     *
     * @param activity the activity
     */
    public void setActivity(FragmentActivity activity) {
        mCustomColorView.setActivity(activity);
    }

    /**
     * Overload implementation of {@link CustomColorPickerView.OnEditFavoriteColorListener#onEditFavoriteItemSelected(int)}
     *
     * @param selectedCount selected favorite color count
     */
    @Override
    public void onEditFavoriteItemSelected(int selectedCount) {
        mToolbar.setBackgroundColor(Utils.getAccentColor(getContext()));
        mToolbarTitle.setText(getContext().getString(R.string.controls_thumbnails_view_selected,
            Utils.getLocaleDigits(Integer.toString(selectedCount))));

        int textColor = Utils.getThemeAttrColor(getContext(), android.R.attr.textColorPrimaryInverse);
        mToolbarTitle.setTextColor(textColor);
        mToolbarTitle.setAlpha(1f);
        mAnnotStyleHolder.setAnnotPreviewVisibility(GONE);
        mBackButton.setImageResource(R.drawable.ic_close_black_24dp);
        mBackButton.setColorFilter(textColor);
        mBackButton.setAlpha(1f);
        mColorPager.setSwippingEnabled(false);
        mRemoveButton.setVisibility(VISIBLE);
        mPagerIndicator.setVisibility(INVISIBLE);
        if (selectedCount == 1) {
            mEditButton.setVisibility(VISIBLE);
        } else {
            mEditButton.setVisibility(GONE);
        }
    }

    /**
     * Overload implementation of {@link CustomColorPickerView.OnEditFavoriteColorListener#onEditFavoriteColorDismissed()} (int)}
     */
    @Override
    public void onEditFavoriteColorDismissed() {
        mToolbar.setBackgroundColor(Utils.getThemeAttrColor(getContext(), android.R.attr.colorBackground));
        int textColor = Utils.getThemeAttrColor(getContext(), android.R.attr.textColorPrimary);

        mToolbarTitle.setTextColor(textColor);
        mToolbarTitle.setAlpha(0.54f);
        mToolbarTitle.setText(mToolbarText);

        mAnnotStyleHolder.setAnnotPreviewVisibility(VISIBLE);
        mRemoveButton.setVisibility(GONE);
        mEditButton.setVisibility(GONE);
        mPagerIndicator.setVisibility(VISIBLE);
        mColorPager.setSwippingEnabled(true);
        mBackButton.setImageResource(R.drawable.ic_arrow_back_black_24dp);
        mBackButton.setColorFilter(textColor);
        mBackButton.setAlpha(0.54f);
    }

    /**
     * A pager adapter to show three pages: {@link CustomColorPickerView}, {@link PresetColorGridView}, and {@link AdvancedColorView}
     */
    protected class ColorPagerAdapter extends PagerAdapter {

        /**
         * Overload implementation of {@link PagerAdapter#getCount()}
         *
         * @return value 3
         */
        @Override
        public int getCount() {
            return 3;
        }

        /**
         * Overload implementation of {@link PagerAdapter#isViewFromObject(View, Object)}
         *
         * @return true if view equals object
         */
        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
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
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view;
            switch (position) {
                case 0:
                    view = mCustomColorView;
                    break;
                case 1:
                    view = mPresetColorView;
                    break;
                default:
                    view = mAdvancedColorView;
                    break;
            }
            view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            container.addView(view);
            return view;
        }

        /**
         * Overload implementation of {@link PagerAdapter#destroyItem(ViewGroup, int, Object)}
         */
        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }
    }

    /**
     * A interface that is invoked when there is color changes in color picker pages
     */
    public interface OnColorChangeListener {
        /**
         * This method invoked when there is color changed
         *
         * @param view  invoked color picker view that made the color change
         * @param color new color
         */
        void OnColorChanged(View view, @ColorInt int color);
    }

    /**
     * This method is used for back button in toolbar pressed event
     */
    public interface OnBackButtonPressedListener {
        /**
         * This method is invoked when back button in toolbar is pressed
         */
        void onBackPressed();
    }

}
