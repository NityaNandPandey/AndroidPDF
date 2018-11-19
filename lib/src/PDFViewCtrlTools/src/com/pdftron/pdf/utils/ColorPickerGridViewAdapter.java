//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.StringDef;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.widget.TransparentDrawable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * An array adapter for showing colors.
 */
public class ColorPickerGridViewAdapter extends ArrayAdapter<String> {

    /**
     * @hide
     */
    @StringDef({TYPE_ADD_COLOR, TYPE_REMOVE_COLOR, TYPE_TRANSPARENT, TYPE_RESTORE_COLOR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface GRID_COLOR_TYPE {
    }

    /**
     * Color type add new color.
     */
    public static final String TYPE_ADD_COLOR = "add_custom_color";
    /**
     * Color type remove color
     */
    public static final String TYPE_REMOVE_COLOR = "remove_custom_color";
    /**
     * Color type transparent color
     */
    public static final String TYPE_TRANSPARENT = "no_fill_color";
    /**
     * Color type restore color
     */
    public static final String TYPE_RESTORE_COLOR = "restore_color";

    private static final float DARK_COLOR_THESHOLD = 0.15f;

    private final Context mContext;
    private List<String> mSource;
    private String mSelected;

    private ArrayList<String> mSelectedList;

    private int mCellHeight;
    private int mCellWidth;
    private int mBackgroundColorResId;

    private boolean mCustomizeBackground;
    private boolean mCustomizeSize;

    private boolean mEditing;
    private int mMaxSize = -1;

    private ArrayList<String> mDisabledList;
    private ArrayList<String> mFavoriteList;

    /**
     * @param context The context
     * @param list    the color list
     */
    public ColorPickerGridViewAdapter(Context context, List<String> list) {
        super(context, 0, list);
        mContext = context;
        setSource(list);
        mSelected = "";

        mCustomizeSize = false;
        mCustomizeBackground = false;
        mEditing = false;
    }

    /**
     * Sets cell size for color item
     *
     * @param width  Cell width
     * @param height Cell height
     */
    public void setCellSize(int width, int height) {
        mCustomizeSize = true;
        mCellWidth = width;
        mCellHeight = height;
    }

    /**
     * Sets color list source
     *
     * @param list color list source
     */
    public void setSource(List<String> list) {
        list = getListLowerCase(list);
        mSource = list;
        notifyDataSetChanged();
    }

    /**
     * Sets color item in specific index
     *
     * @param index The specific index
     * @param item  The color item in string format
     */
    public void setItem(int index, String item) {
        mSource.set(index, item.toLowerCase());
        notifyDataSetChanged();
    }

    /**
     * Sets color cell background
     *
     * @param resId The background resource id
     */
    public void setCellBackground(int resId) {
        mCustomizeBackground = true;
        mBackgroundColorResId = resId;
    }

    /**
     * Sets whether the specific position color is selected
     *
     * @param position The specific position
     */
    public void setSelected(int position) {
        if (position > -1) {
            mSelected = getItem(position);
        } else {
            mSelected = "";
        }
        notifyDataSetChanged();
    }

    /**
     * Sets the given color source to be selected
     *
     * @param selected The selected color source
     */
    public void setSelected(String selected) {
        mSelected = selected.toLowerCase();
        notifyDataSetChanged();
    }

    /**
     * Gets selected color source
     *
     * @return The selected color source
     */
    public String getSelected() {
        return mSelected;
    }

    /**
     * Sets whether the color source is editing.
     * If the color source is editing, show item {@link #TYPE_REMOVE_COLOR}, else hide it.
     *
     * @param editing whether the color source is editing
     */
    public void setEditing(boolean editing) {
        mEditing = editing;
        if (editing) {
            add(ColorPickerGridViewAdapter.TYPE_RESTORE_COLOR);
        } else {
            remove(ColorPickerGridViewAdapter.TYPE_RESTORE_COLOR);
        }
        notifyDataSetChanged();
    }

    /**
     * Gets whether the color source is editing
     *
     * @return true then it is editing, false otherwise
     */
    public boolean isEditing() {
        return mEditing;
    }

    /**
     * Removes item at specific position
     *
     * @param position The specific position
     */
    public void removeItem(int position) {
        mSource.remove(position);
        notifyDataSetChanged();
    }

    /**
     * Remove Item from color source
     *
     * @param value color value that is going to be removed
     * @return position of color value, return -1 if color value not found
     */
    public int removeItem(String value) {
        int result = -1;
        if (mSource.contains(value.toLowerCase())) {
            result = mSource.indexOf(value);
        }
        mSource.remove(value.toLowerCase());
        return result;
    }

    /**
     * Adds a color item. If color source exceeds maximum size, then it removes the first color item.
     *
     * @param object The color item
     */
    @Override
    public void add(String object) {
        if (object != null && !mSource.contains(object.toLowerCase())) {
            mSource.add(object.toLowerCase());
            if (mMaxSize >= 0 && mSource.size() > mMaxSize) {
                mSource.remove(0);
            }
            notifyDataSetChanged();
        }
    }

    /**
     * Adds a color item to the front of the color source list. If the color source exceeds maximum size,
     * then it removes the color item at the end of the list.
     *
     * @param object The color item
     */
    public void addFront(String object) {
        if (object != null && !mSource.contains(object.toLowerCase())) {
            mSource.add(0, object.toLowerCase());
            if (mMaxSize >= 0 && mSource.size() > mMaxSize) {
                mSource.remove(mSource.size() - 1);
            }
            notifyDataSetChanged();
        }
    }

    /**
     * Add source value to specified position
     *
     * @param position position
     * @param value    source value
     */
    public void addItem(int position, String value) {
        mSource.add(position, value.toLowerCase());
        notifyDataSetChanged();
    }

    /**
     * Gets color source count.
     *
     * @return The color source count
     */
    @Override
    public int getCount() {
        return mSource.size();
    }

    /**
     * Get color item in list at specific position.
     *
     * @param position The specific position.
     * @return color item in color source list.
     */
    @Override
    public String getItem(int position) {
        if (mSource != null && position >= 0 && position < mSource.size()) {
            return mSource.get(position);
        }
        return null;
    }

    /**
     * Gets the index of the color item.
     *
     * @param colorInt The color item
     * @return A pair of whether it founds the color, and the found color index. If the returned color
     * index is -1, it means the color is not found.
     */
    public Pair<Boolean, Integer> getItemIndex(@ColorInt int colorInt) {
        for (int i = 0; i < mSource.size(); i++) {
            try {
                if (Color.parseColor(mSource.get(i)) == colorInt) {
                    return Pair.create(true, i);
                }
            } catch (Exception e) {
                return Pair.create(false, -1);
            }
        }
        return Pair.create(false, -1);
    }


    /**
     * Gets the index of the color item.
     *
     * @param item The color item in string format
     * @return A pair of whether it founds the color, and the found color index. If the returned color
     * index is -1, it means the color is not found.
     */
    public Pair<Boolean, Integer> getItemIndex(String item) {
        for (int i = 0; i < mSource.size(); i++) {
            try {
                if (mSource.get(i).equals(item)) {
                    return Pair.create(true, i);
                }
            } catch (Exception e) {
                return Pair.create(false, -1);
            }
        }
        return Pair.create(false, -1);
    }

    /**
     * Gets all color source items
     *
     * @return all color source items
     */
    public List<String> getItems() {
        return mSource;
    }

    /**
     * Adds all items to color source list
     *
     * @param collection The collection of color items
     */
    @Override
    public void addAll(@NonNull Collection<? extends String> collection) {
        mSource.addAll(collection);
        super.addAll(collection);
    }

    /**
     * Whether the color source contains the given color item
     *
     * @param item The color item
     * @return true the color source list contains the given color item, false otherwise.
     */
    public boolean contains(String item) {
        return mSource.contains(item.toLowerCase());
    }

    /**
     * Sets the maximum size of holdable color sources.
     *
     * @param size Maximum size
     */
    public void setMaxSize(int size) {
        mMaxSize = size;
    }

    /**
     * Overload implementation of {@link ArrayAdapter#getView(int, View, ViewGroup)}
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.tools_gridview_color_picker, parent, false);

            RelativeLayout layout = convertView.findViewById(R.id.cell_layout);
            int size = getContext().getResources().getDimensionPixelSize(R.dimen.quick_menu_button_size);

            RelativeLayout.LayoutParams lp = mCustomizeSize ? new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mCellHeight)
                : new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size);

            if (mCustomizeBackground) {
                layout.setBackgroundColor(mContext.getResources().getColor(mBackgroundColorResId));
            }
            ImageView imageView = convertView.findViewById(R.id.color_image_view);
            imageView.setLayoutParams(lp);
            ImageView selectedImageView = convertView.findViewById(R.id.color_selected);
            ImageView removeImageView = convertView.findViewById(R.id.color_remove);
            ImageView buttonImageView = convertView.findViewById(R.id.color_buttons);
            buttonImageView.setLayoutParams(lp);
            layout.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.MATCH_PARENT));
            holder = new ViewHolder();
            holder.colorLayout = layout;
            holder.colorImage = imageView;
            holder.selectedIndicator = selectedImageView;
            holder.removeIndicator = removeImageView;
            holder.colorButtons = buttonImageView;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.colorImage.setBackgroundColor(mContext.getResources().getColor(R.color.tools_colors_white));
        holder.colorImage.setImageResource(android.R.color.transparent);
        if (mEditing) {
            holder.selectedIndicator.setVisibility(View.GONE);
            holder.removeIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.removeIndicator.setVisibility(View.GONE);
            if (mSelected != null) {
                // if the grid is selected, show a different view
                if (mSource.get(position).equalsIgnoreCase(mSelected) && (mSelectedList == null || mSelectedList.isEmpty())) {
                    int selectedIndicatorColor = Color.WHITE;
                    float selectedIndicatorAlpha = 0.87f;
                    boolean isTransparent = mSelected.equalsIgnoreCase(TYPE_TRANSPARENT);
                    if (!isTransparent) {
                        try {
                            int color = Color.parseColor(mSelected);
                            isTransparent = color == Color.TRANSPARENT;
                        } catch (IllegalArgumentException e) {

                        }
                    }

                    int drawableRes = isTransparent ? R.drawable.ic_check_circle_black_24dp : R.drawable.ic_check_black_24dp;
                    Drawable icon = getContext().getResources().getDrawable(drawableRes);
                    if (!isTransparent) {
                        icon.mutate();
                        try {
                            int color = Color.parseColor(mSelected);
                            if (!Utils.isColorDark(color, DARK_COLOR_THESHOLD)) {
                                selectedIndicatorColor = Color.BLACK;
                                selectedIndicatorAlpha = 0.54f;
                            }
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                        icon.setColorFilter(selectedIndicatorColor, PorterDuff.Mode.SRC_IN);
                        icon.setAlpha((int) (selectedIndicatorAlpha * 255));
                    }
                    holder.selectedIndicator.setImageDrawable(icon);

                    holder.selectedIndicator.setVisibility(View.VISIBLE);
                } else {
                    holder.selectedIndicator.setVisibility(View.GONE);
                }
            }
        }
        if (mSource.get(position).equalsIgnoreCase(TYPE_ADD_COLOR)) {
            holder.colorButtons.setImageResource(R.drawable.ic_add_white_24dp);
            holder.colorButtons.setColorFilter(Utils.getThemeAttrColor(getContext(), android.R.attr.textColorPrimary), PorterDuff.Mode.SRC_IN);
            holder.colorButtons.setAlpha(0.54f);
            holder.colorButtons.setVisibility(View.VISIBLE);
            // if it is at the first position, add a border background
            if (position == 0) {
                GradientDrawable background = (GradientDrawable) mContext.getResources().getDrawable(R.drawable.rounded_corners);
                background.mutate();
                background.setStroke(1, Color.GRAY);
                background.setColor(Color.TRANSPARENT);
                holder.colorButtons.setBackground(background);
            } else {
                TypedArray typedArray = getContext().obtainStyledAttributes(new int[]{R.attr.selectableItemBackground});
                Drawable background = typedArray.getDrawable(0);
                typedArray.recycle();
                if (background != null) {
                    holder.colorButtons.setBackground(background);
                }
            }
            holder.removeIndicator.setVisibility(View.GONE);
            holder.selectedIndicator.setVisibility(View.GONE);
            holder.colorImage.setVisibility(View.GONE);
        } else if (mSource.get(position).equalsIgnoreCase(TYPE_REMOVE_COLOR)) {
            if (mEditing) {
                holder.colorButtons.setImageResource(R.drawable.ic_check_black_24dp);
                holder.colorButtons.getDrawable().mutate();
                holder.colorButtons.getDrawable().setColorFilter(getContext().getResources().getColor(R.color.qm_item_color), PorterDuff.Mode.SRC_IN);
            } else {
                holder.colorButtons.setImageResource(R.drawable.ic_remove_white_24dp);
                holder.colorButtons.getDrawable().mutate();
                holder.colorButtons.getDrawable().setColorFilter(getContext().getResources().getColor(R.color.qm_item_color), PorterDuff.Mode.SRC_IN);
            }
            holder.colorButtons.setVisibility(View.VISIBLE);
            holder.selectedIndicator.setVisibility(View.GONE);
            holder.removeIndicator.setVisibility(View.GONE);
        } else if (mSource.get(position).equalsIgnoreCase(TYPE_RESTORE_COLOR)) {
            holder.colorImage.setImageResource(R.drawable.restore);
            holder.colorImage.getDrawable().mutate();
            holder.colorImage.getDrawable().setColorFilter(getContext().getResources().getColor(R.color.gray600), PorterDuff.Mode.SRC_IN);
            holder.colorImage.setScaleType(ImageView.ScaleType.CENTER);
//            holder.colorImage.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.border_dashed));
            holder.selectedIndicator.setVisibility(View.GONE);
            holder.removeIndicator.setVisibility(View.GONE);
            holder.colorButtons.setVisibility(View.GONE);
        } else {
            holder.colorButtons.setVisibility(View.GONE);
            holder.colorImage.setVisibility(View.VISIBLE);
            String colorStr = mSource.get(position);
            int color = Color.TRANSPARENT;
            if (!colorStr.equalsIgnoreCase(TYPE_TRANSPARENT)) {
                try {
                    color = Color.parseColor(colorStr);
                } catch (IllegalArgumentException e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "\ncolorStr: " + colorStr + "; index: " + position);
                }
            }
            if (color != Color.TRANSPARENT) {
                GradientDrawable colorSwatch = (GradientDrawable) mContext.getResources().getDrawable(R.drawable.rounded_corners);
                colorSwatch.mutate();
                colorSwatch.setColor(color);
                if (color != Color.WHITE) {
                    colorSwatch.setStroke(0, Color.TRANSPARENT);
                } else {
                    colorSwatch.setStroke(1, Color.GRAY);
                }
                holder.colorImage.setBackground(colorSwatch);
            } else {
                TransparentDrawable drawable = new TransparentDrawable(getContext());
                drawable.setRoundedConer(getContext().getResources().getDimensionPixelSize(R.dimen.tools_grid_color_picker_round_corners));
                holder.colorImage.setBackground(drawable);
            }

            if (mFavoriteList != null && mFavoriteList.contains(colorStr)) {
                int drawableRes = Utils.isColorDark(color, DARK_COLOR_THESHOLD) && color != Color.TRANSPARENT ? R.drawable.ic_star_white_24dp : R.drawable.ic_star_white_border_24dp;
                Drawable icon = getContext().getResources().getDrawable(drawableRes);
                holder.colorImage.setImageDrawable(icon);
                icon.mutate();
                if (mDisabledList != null && mDisabledList.contains(colorStr)) {
                    int colorFilter = color == Color.BLACK ? Color.GRAY : Color.BLACK;
                    icon.setColorFilter(colorFilter, PorterDuff.Mode.SRC_IN);
                    icon.setAlpha(137);
                } else {
                    icon.setAlpha(255);
                }
            }
            if (mSelectedList != null && mSelectedList.contains(colorStr)) {
                int drawableRes = color == Color.TRANSPARENT ? R.drawable.ic_check_circle_black_24dp : R.drawable.ic_check_black_24dp;
                Drawable icon = getContext().getResources().getDrawable(drawableRes);
                holder.colorImage.setImageDrawable(icon);
                icon.mutate();
                if (!Utils.isColorDark(color, DARK_COLOR_THESHOLD) && color != Color.TRANSPARENT) {
                    icon.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
                    icon.setAlpha(137);
                } else if (color != Color.TRANSPARENT) {
                    icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                    icon.setAlpha(255);
                }
            }

        }
        return convertView;
    }

    /**
     * Sets favorite list of color items. If a color source item is in favorite list, it will show
     * a star icon in middle
     *
     * @param favoriteList a list of favorite color items
     */
    public void setFavoriteList(ArrayList<String> favoriteList) {
        mFavoriteList = favoriteList;
        notifyDataSetChanged();
    }


    /**
     * Sets disabled list of color items. If a color source item is in disabled list, it will show
     * a half transparent star icon in middle
     *
     * @param disabledColorList a list of disabled color items
     */
    public void setDisabledColorList(ArrayList<String> disabledColorList) {
        mDisabledList = disabledColorList;
        notifyDataSetChanged();
    }

    /**
     * Sets selected list of color items. If a color source item is in selected list, it will show
     * a white check mark icon in middle
     *
     * @param selectedList a list of selected color items
     */
    public void setSelectedList(ArrayList<String> selectedList) {
        mSelectedList = selectedList;
        notifyDataSetChanged();
    }

    /**
     * @param list the list
     * @return list in lower case
     * @hide get list in lower case
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static List<String> getListLowerCase(List<String> list) {
        ListIterator<String> listIterator = list.listIterator();
        while (listIterator.hasNext()) {
            String next = listIterator.next();
            listIterator.set(next.toLowerCase());
        }
        return list;
    }

    /**
     * Add given position color source item to selected list
     *
     * @param position The position in color source
     */
    public void addSelected(int position) {
        String item = getItem(position);
        if (mSelectedList == null) {
            mSelectedList = new ArrayList<>();
        }
        mSelectedList.add(item);
        notifyDataSetChanged();
    }

    /**
     * Gets selected list
     *
     * @return selected list
     */
    public ArrayList<String> getSelectedList() {
        return mSelectedList;
    }

    /**
     * Deletes selected list items from color source
     */
    public void deleteAllSelected() {
        mSource.removeAll(mSelectedList);

        if (mSelectedList != null) {
            mSelectedList.clear();
        }

        notifyDataSetChanged();
    }

    /**
     * Clears selected list
     */
    public void removeAllSelected() {
        if (mSelectedList != null) {
            mSelectedList.clear();
        }

        notifyDataSetChanged();
    }

    /**
     * Remove the specific position color source item from selected list
     *
     * @param position The position of selected list
     */
    public void removeSelected(int position) {
        if (mSelectedList != null) {
            String item = getItem(position);
            mSelectedList.remove(item);
        }

        notifyDataSetChanged();
    }

    /**
     * Gets selected list count
     *
     * @return Selected list count
     */
    public int getSelectedListCount() {
        return mSelectedList == null ? 0 : mSelectedList.size();
    }

    /**
     * Whether the specified position in color source is in selected list
     *
     * @param position The position in color source
     * @return true then it is in selected list, false otherwise
     */
    public boolean isInSelectedList(int position) {
        String item = getItem(position);
        return mSelectedList != null && mSelectedList.contains(item);
    }

    /**
     * Gets first selected list item
     *
     * @return first selected list item
     */
    public String getFirstSelectedInList() {
        return mSelectedList == null ? null : mSelectedList.get(0);
    }

    /**
     * Whether the specified color item is disabled
     *
     * @param item The color item
     * @return true then this item is disabled, false other wise
     */
    public boolean isItemDisabled(String item) {
        return mDisabledList != null && mDisabledList.contains(item.toLowerCase());
    }

    private static class ViewHolder {
        RelativeLayout colorLayout;
        ImageView colorImage;
        ImageView selectedIndicator;
        ImageView removeIndicator;
        ImageView colorButtons;
    }
}
