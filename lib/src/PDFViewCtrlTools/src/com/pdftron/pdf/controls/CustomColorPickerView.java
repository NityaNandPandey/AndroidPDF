//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsAnnotStylePicker;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.ColorPickerGridViewAdapter;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;

import static com.pdftron.pdf.utils.ColorPickerGridViewAdapter.TYPE_ADD_COLOR;
import static com.pdftron.pdf.utils.ColorPickerGridViewAdapter.TYPE_REMOVE_COLOR;
import static com.pdftron.pdf.utils.ColorPickerGridViewAdapter.TYPE_RESTORE_COLOR;
import static com.pdftron.pdf.utils.ColorPickerGridViewAdapter.TYPE_TRANSPARENT;

/**
 * A Linear layout that shows recently selected colors, favorite colors
 */
public class CustomColorPickerView extends LinearLayout {
    public static final String KEY_RECENT_COLORS = "recent_colors";
    public static final String KEY_FAVORITE_COLORS = "favorite_colors";

    public static final int MAX_COLORS = 12;
    private GridView mFavoriteColorGrid;
    private GridView mRecentColorGrid;
    private ColorPickerGridViewAdapter mFavoriteColorAdapter;
    private ColorPickerGridViewAdapter mRecentColorAdapter;
    private TextView mRecentColorHint;
    private ColorPickerView.OnColorChangeListener mColorChangeListener;
    private FavoriteColorDialogFragment mFavoriteColorEditDialog;
    private OnEditFavoriteColorListener mEditFavoriteListener;
    private TextView mRecentTitle;
    private TextView mFavoriteTitle;
    private int mSelectedFavColorPosition = -1;
    private WeakReference<FragmentActivity> mActivityRef;
    private AdapterView.OnItemLongClickListener mRecentColorLongClickListener;

    /**
     * Class constructor
     */
    public CustomColorPickerView(Context context) {
        this(context, null);
    }

    /**
     * Class constructor
     */
    public CustomColorPickerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Class constructor
     */
    public CustomColorPickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.color_picker_layout_custom, this);
        mFavoriteColorGrid = findViewById(R.id.favorite_color_grid);
        mRecentColorGrid = findViewById(R.id.recent_color_grid);
        mRecentColorHint = findViewById(R.id.recent_color_hint);
        mRecentTitle = findViewById(R.id.recent_title);
        mFavoriteTitle = findViewById(R.id.favorite_title);
        loadColors();
    }

    private void loadColors() {
        // favorite
        String savedFavColors = PdfViewCtrlSettingsManager.getFavoriteColors(getContext());
        mFavoriteColorAdapter = new ColorPickerGridViewAdapter(getContext(), new ArrayList<String>());
        ArrayList<String> favColors = setStoredColors(mFavoriteColorAdapter, savedFavColors);
        if (favColors.size() < MAX_COLORS && !favColors.contains(TYPE_ADD_COLOR)) {
            mFavoriteColorAdapter.addItem(mFavoriteColorAdapter.getCount(), TYPE_ADD_COLOR);
        }
        mFavoriteColorGrid.setAdapter(mFavoriteColorAdapter);
        mFavoriteColorGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onItemClicked(adapterView, view, i, l);
            }
        });
        mFavoriteColorGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                return onItemLongClicked(adapterView, view, i, l);
            }
        });
        // recent
        String savedRecentColors = PdfViewCtrlSettingsManager.getRecentColors(getContext());
        mRecentColorAdapter = new ColorPickerGridViewAdapter(getContext(), new ArrayList<String>());
        ArrayList<String> recentColorSources = setStoredColors(mRecentColorAdapter, savedRecentColors);
        mRecentColorGrid.setAdapter(mRecentColorAdapter);
        mRecentColorGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                onItemClicked(adapterView, view, i, l);
            }
        });
        mRecentColorGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                return onItemLongClicked(adapterView, view, i, l);
            }
        });
        if (recentColorSources.isEmpty()) {
            mRecentColorHint.setVisibility(VISIBLE);
            mRecentColorGrid.setVisibility(GONE);
        } else {
            mRecentColorGrid.setVisibility(VISIBLE);
            mRecentColorHint.setVisibility(GONE);
        }
    }

    private static ArrayList<String> setStoredColors(ColorPickerGridViewAdapter adapter, String colorListStr) {
        ArrayList<String> colorSources = new ArrayList<>();
        if (!Utils.isNullOrEmpty(colorListStr)) {
            String[] colorList = colorListStr.split(", ");
            colorSources.addAll(Arrays.asList(colorList));

        }
        adapter.setMaxSize(MAX_COLORS);
        adapter.setSource(colorSources);
        return colorSources;
    }

    /**
     * Add color to recent color list
     *
     * @param source the color in string format to be added to recent list
     */
    public void addRecentColorSource(String source) {
        mRecentColorAdapter.addFront(source);
        if (mRecentColorHint.getVisibility() == VISIBLE
            && mRecentColorAdapter.getCount() > 0) {
            mRecentColorHint.setVisibility(GONE);
            mRecentColorGrid.setVisibility(VISIBLE);
        }
    }

    private static String getColorsToString(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String str : items) {
            sb = sb.append(str);
            if (items.indexOf(str) < (items.size() - 1)) {
                sb = sb.append(',').append(' ');
            }
        }
        return sb.toString();
    }

    /**
     * Save favorite colors and recent colors to settings
     */
    public void saveColors() {
        PdfViewCtrlSettingsManager.setRecentColors(getContext(), getColorsToString(mRecentColorAdapter.getItems()));
        List<String> favorites = mFavoriteColorAdapter.getItems();
        favorites.remove(TYPE_ADD_COLOR);
        PdfViewCtrlSettingsManager.setFavoriteColors(getContext(), getColorsToString(favorites));
    }

    /**
     * Sets on color change listener
     *
     * @param listener color change listener
     */
    public void setOnColorChangeListener(ColorPickerView.OnColorChangeListener listener) {
        mColorChangeListener = listener;
    }

    /**
     * Sets selected color. If selected color matches any of the color grids, it will show a white check mark
     *
     * @param colorStr color in string format
     */
    public void setSelectedColor(String colorStr) {
        mRecentColorAdapter.setSelected(colorStr);
        mFavoriteColorAdapter.setSelected(colorStr);
    }

    private void onItemClicked(AdapterView<?> parent, View view, int position, long id) {
        ColorPickerGridViewAdapter adapter = (ColorPickerGridViewAdapter) parent.getAdapter();
        String item = adapter.getItem(position);

        // if clicked item is recent color grid and recent color has selected list item
        if (parent.getId() == mFavoriteColorGrid.getId() && mRecentColorAdapter.getSelectedListCount() > 0) {
            return;
        } else if (parent.getId() == mRecentColorGrid.getId() && mFavoriteColorAdapter.getSelectedListCount() > 0) {
            return;
        } else if (adapter.getSelectedListCount() > 0 && onItemLongClicked(parent, view, position, id)) {
            return;
        }

        if (item != null && !item.equalsIgnoreCase(TYPE_ADD_COLOR)
            && !item.equalsIgnoreCase(TYPE_REMOVE_COLOR)
            && !item.equalsIgnoreCase(TYPE_RESTORE_COLOR)) {
            if (!item.equalsIgnoreCase(adapter.getSelected()) && mColorChangeListener != null) {
                int labelId = parent.getId() == mFavoriteColorGrid.getId() ? AnalyticsHandlerAdapter.LABEL_FAVORITES : AnalyticsHandlerAdapter.LABEL_RECENT;
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_COLOR_PICKER,
                    String.format("color selected %s", item), labelId);
                int color;
                if (item.equals(TYPE_TRANSPARENT)) {
                    color = Color.TRANSPARENT;
                } else {
                    try {
                        color = Color.parseColor(item);
                    } catch (IllegalArgumentException e) {
                        color = Color.TRANSPARENT;
                    }
                }
                mColorChangeListener.OnColorChanged(this, color);
            }
            adapter.setSelected(position);
            if (adapter != mFavoriteColorAdapter) { // recent color is selected
                mFavoriteColorAdapter.setSelected(item);
                AnalyticsAnnotStylePicker.getInstance().selectColor(item.toUpperCase(), AnalyticsHandlerAdapter.STYLE_PICKER_RECENT);
            } else {    // favorite color is selected
                mRecentColorAdapter.setSelected(item);
                AnalyticsAnnotStylePicker.getInstance().selectColor(item.toUpperCase(), AnalyticsHandlerAdapter.STYLE_PICKER_FAVORITE);
            }
        } else if (item != null && item.equalsIgnoreCase(TYPE_ADD_COLOR)) {
            openFavoriteEditDialog(FavoriteColorDialogFragment.ADD_COLOR);
        }
    }

    private boolean onItemLongClicked(AdapterView<?> parent, View view, int position, long id) {

        ColorPickerGridViewAdapter adapter = (ColorPickerGridViewAdapter) parent.getAdapter();
        if (adapter.getItem(position) != null && TYPE_ADD_COLOR.equalsIgnoreCase(adapter.getItem(position))) {
            return false;
        }
        // if it is recent color grid item
        if (parent.getId() == mRecentColorGrid.getId() && mRecentColorLongClickListener != null) {
            boolean result = mRecentColorLongClickListener.onItemLongClick(parent, view, position, id);
            boolean favoriteClickable = adapter.getSelectedListCount() <= 0;
            float alpha = adapter.getSelectedListCount() > 0 ? 0.38f : 1.0f;
            mFavoriteColorGrid.setClickable(favoriteClickable);
            mFavoriteColorGrid.setLongClickable(favoriteClickable);
            mFavoriteColorGrid.setAlpha(alpha);
            mFavoriteTitle.setAlpha(alpha);
            return result;
        }

        if (adapter.isInSelectedList(position)) {
            adapter.removeSelected(position);
        } else {
            adapter.addSelected(position);
        }

        if (adapter.getSelectedListCount() > 0) {
            mSelectedFavColorPosition = position;
            onFavoriteItemSelected();
        } else {
            onFavoriteItemCleared();
        }
        return true;
    }

    private void onFavoriteItemSelected() {
        mFavoriteColorAdapter.remove(TYPE_ADD_COLOR);
        mRecentColorGrid.setClickable(false);
        mRecentColorGrid.setAlpha(0.38f);
        mRecentColorGrid.setLongClickable(false);
        mRecentTitle.setAlpha(0.38f);

        if (mEditFavoriteListener != null) {
            mEditFavoriteListener.onEditFavoriteItemSelected(mFavoriteColorAdapter.getSelectedListCount());
        }
    }

    private void onFavoriteItemCleared() {
        if (mFavoriteColorAdapter.getCount() < MAX_COLORS && !mFavoriteColorAdapter.contains(TYPE_ADD_COLOR)) {
            mFavoriteColorAdapter.add(TYPE_ADD_COLOR);
        }
        mRecentColorGrid.setClickable(true);
        mRecentColorGrid.setLongClickable(true);
        mRecentColorGrid.setAlpha(1);
        mRecentTitle.setAlpha(1);
        mSelectedFavColorPosition = -1;
        if (mEditFavoriteListener != null) {
            mEditFavoriteListener.onEditFavoriteColorDismissed();
        }
    }

    /**
     * Sets activity. The activity is used to open add favorite dialog
     *
     * @param activity the activity
     */
    public void setActivity(FragmentActivity activity) {
        mActivityRef = new WeakReference<>(activity);
    }

    private void openFavoriteEditDialog(@FavoriteColorDialogFragment.FavoriteDialogMode int favDialogMode) {
        // Open Add Favorite Dialog Fragment
        FragmentActivity activity = null;
        if (mActivityRef != null) {
            activity = mActivityRef.get();
        }
        if (activity == null && getContext() instanceof FragmentActivity) {
            activity = ((FragmentActivity) getContext());
        }
        if (activity == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putStringArrayList(KEY_RECENT_COLORS, (ArrayList<String>) mRecentColorAdapter.getItems());
        ArrayList<String> favoriteItem = new ArrayList<>(mFavoriteColorAdapter.getItems());
        favoriteItem.remove(TYPE_ADD_COLOR);
        if (mSelectedFavColorPosition >= 0) {
            favoriteItem.remove(mSelectedFavColorPosition);
        }
        bundle.putStringArrayList(KEY_FAVORITE_COLORS, favoriteItem);
        bundle.putInt(FavoriteColorDialogFragment.FAVORITE_DIALOG_MODE, favDialogMode);

        mFavoriteColorEditDialog = FavoriteColorDialogFragment.newInstance(bundle);

        mFavoriteColorEditDialog.setOnEditFinishedListener(new FavoriteColorDialogFragment.OnEditFinishedListener() {
            @Override
            public void onEditFinished(ArrayList<String> colors, int dialogMode) {
                setColorsToFavorites(colors, dialogMode);
            }
        });

        mFavoriteColorEditDialog.show(activity.getSupportFragmentManager(), "dialog");
    }

    /**
     * If it is in edit favorite color mode, dismiss the edit favorite color mode
     */
    public boolean onBackButonPressed() {
        if (mFavoriteColorAdapter.getSelectedListCount() > 0) {
            mFavoriteColorAdapter.removeAllSelected();
            onFavoriteItemCleared();
            return true;
        } else if (mRecentColorAdapter.getSelectedListCount() > 0) {
            mFavoriteColorGrid.setClickable(true);
            mFavoriteColorGrid.setLongClickable(true);
            mFavoriteColorGrid.setAlpha(1);
            mFavoriteTitle.setAlpha(1);
        }
        return false;
    }

    /**
     * Sets given colors to favorite colors
     *
     * @param colors     The specified colors
     * @param dialogMode favorite mode. It must be one of {@link FavoriteColorDialogFragment#ADD_COLOR} or {@link FavoriteColorDialogFragment#EDIT_COLOR}
     */
    public void setColorsToFavorites(ArrayList<String> colors, @FavoriteColorDialogFragment.FavoriteDialogMode int dialogMode) {
        // remove duplicate colors
        TreeSet<String> toRetain = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        toRetain.addAll(colors);
        LinkedHashSet<String> set = new LinkedHashSet<>(colors);
        set.retainAll(new LinkedHashSet<>(toRetain));
        colors = new ArrayList<>(set);
        if (colors.size() > MAX_COLORS) {
            colors = new ArrayList<>(colors.subList(colors.size() - MAX_COLORS, colors.size()));
        }
        if (colors.size() < MAX_COLORS && dialogMode == FavoriteColorDialogFragment.ADD_COLOR) {
            colors.add(TYPE_ADD_COLOR);
        }
        if (dialogMode == FavoriteColorDialogFragment.ADD_COLOR) {
            mFavoriteColorAdapter.setSource(colors);
        } else {
            String originalColor = mFavoriteColorAdapter.getItem(mSelectedFavColorPosition);
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_REMOVE_FAVORITE,
                AnalyticsParam.colorParam(originalColor));
            mFavoriteColorAdapter.setItem(mSelectedFavColorPosition, colors.get(0));
            mFavoriteColorAdapter.setSelectedList(null);
            mSelectedFavColorPosition = -1;
            onFavoriteItemCleared();
        }
    }

    /**
     * Gets favorite colors
     *
     * @return Favorite colors
     */
    public ArrayList<String> getFavoriteColors() {
        ArrayList<String> favoriteColors = new ArrayList<>(mFavoriteColorAdapter.getItems());
        favoriteColors.remove(TYPE_ADD_COLOR);
        return favoriteColors;
    }

    /**
     * Open the edit favorite dialog.
     */
    public void editSelectedColor() {
        openFavoriteEditDialog(FavoriteColorDialogFragment.EDIT_COLOR);
        try {
            int color = Color.parseColor(mFavoriteColorAdapter.getFirstSelectedInList());
            mFavoriteColorEditDialog.setSelectedColor(color);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete all selected favorite colors
     */
    public void deleteAllSelectedFavColors() {
        ArrayList<String> selectedItems = mFavoriteColorAdapter.getSelectedList();
        for (String color : selectedItems) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_REMOVE_FAVORITE,
                AnalyticsParam.colorParam(color));
        }
        mFavoriteColorAdapter.deleteAllSelected();
        onFavoriteItemCleared();
    }

    /**
     * Sets on favorite color dialog listener
     *
     * @param listener The listener
     */
    public void setOnEditFavoriteColorlistener(OnEditFavoriteColorListener listener) {
        mEditFavoriteListener = listener;
    }

    /**
     * Sets recent color item long press listener
     *
     * @param listener The listener
     */
    public void setRecentColorLongPressListener(AdapterView.OnItemLongClickListener listener) {
        mRecentColorLongClickListener = listener;
    }

    /**
     * This interface is for listening events about editing favorite colors
     */
    public interface OnEditFavoriteColorListener {
        /**
         * This method is invoked when selected a favorite color in edit mode
         *
         * @param selectedCount selected favorite items count
         */
        void onEditFavoriteItemSelected(int selectedCount);

        /**
         * This method is invoked when favorite color edit mode is dismissed
         */
        void onEditFavoriteColorDismissed();
    }

}
