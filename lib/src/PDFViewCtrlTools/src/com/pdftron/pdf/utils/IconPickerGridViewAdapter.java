//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.Tool;

import java.util.List;

/**
 * An array adapter for showing icons
 */
public class IconPickerGridViewAdapter extends ArrayAdapter<String> {
    private Context mContext;
    private String mSelected;
    private List<String> mSource;
    private int mIconColor;
    private float mIconOpacity;

    /**
     * Class constructor
     *
     * @param context The context
     * @param list    The list of icons
     */
    public IconPickerGridViewAdapter(Context context, List<String> list) {
        super(context, 0, list);

        mContext = context;
        mSelected = "";
        mSource = list;
        mIconColor = 0;
        mIconOpacity = 0.0f;
    }

    /**
     * Gets icons count
     *
     * @return icons count
     */
    public int getCount() {
        return mSource.size();
    }

    /**
     * Gets icon at specified position
     *
     * @param position The specified position
     * @return icon
     */
    public String getItem(int position) {
        if (mSource != null && position >= 0 && position < mSource.size()) {
            return mSource.get(position);
        }
        return null;
    }

    /**
     * Gets icon index
     *
     * @param icon The icon
     * @return The index
     */
    public int getItemIndex(String icon) {
        for (int i = 0; i < mSource.size(); i++) {
            if (icon.equals(mSource.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets selected icon
     *
     * @return The selected icon
     */
    public String getSelected() {
        return mSelected;
    }

    /**
     * Selects icon at specified position
     *
     * @param position The specified position
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
     * Create a new ImageView for each item referenced by the Adapter
     */
    @NonNull
    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.tools_gridview_icon_picker, parent, false);

            RelativeLayout layout = convertView.findViewById(R.id.cell_layout);
            ImageView imageView = convertView.findViewById(R.id.icon_image_view);

            holder = new ViewHolder();
            holder.mIconLayout = layout;
            holder.mIconImage = imageView;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final String iconOutline = Tool.ANNOTATION_NOTE_ICON_FILE_PREFIX + mSource.get(position).toLowerCase() + Tool.ANNOTATION_NOTE_ICON_FILE_POSTFIX_OUTLINE;

        int iconOutlineID = mContext.getResources().getIdentifier(iconOutline, "drawable", mContext.getPackageName());

        // create the preview color using the icon color and opacity
        int r = Color.red(mIconColor);
        int g = Color.green(mIconColor);
        int b = Color.blue(mIconColor);
        int a = (int) (mIconOpacity * 0xFF);
        int color = Color.argb(a, r, g, b);

        // set selected icon
        if (!mSelected.equals("")) {
            try {
                if (mSelected.equals(getItem(position))) {
                    holder.mIconImage.setImageDrawable(AnnotStyle.getIconDrawable(getContext(), mSource.get(position).toLowerCase(), color, 1));
                } else {
                    holder.mIconImage.setImageDrawable(mContext.getResources().getDrawable(iconOutlineID));
                    if (Utils.isDeviceNightMode(getContext())) {
                        holder.mIconImage.getDrawable().mutate();
                        holder.mIconImage.getDrawable().setColorFilter(getContext().getResources().getColor(R.color.gray400), PorterDuff.Mode.SRC_IN);
                    }
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
        return convertView;
    }

    /**
     * Updates icon color
     *
     * @param color The icon color
     */
    public void updateIconColor(@ColorInt int color) {
        mIconColor = color;
        notifyDataSetChanged();
    }

    /**
     * Updates icon opacity
     *
     * @param opacity The icon opacity
     */
    public void updateIconOpacity(@FloatRange(from = 0, to = 1) float opacity) {
        mIconOpacity = opacity;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        RelativeLayout mIconLayout;
        ImageView mIconImage;
    }

    /**
     * Overriding unregisterDataSetObserver resolves a bug on ICS where
     * this function is called twice the number of times it should be. This causes the
     * observer to be unregistered twice and throws a "Observer is null" exception when
     * unregistered the second time.
     *
     * @param observer data set observer
     */
    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        if (observer != null) {
            super.unregisterDataSetObserver(observer);
        }
    }
}
