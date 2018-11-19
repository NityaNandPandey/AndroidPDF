//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.adapter;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

public class CustomStampColorAdapter extends RecyclerView.Adapter<CustomStampColorAdapter.ViewHolder> {

    private int[] mBgColors;
    private int[] mStrokeColors;
    private int mSelectedIndex;

    /**
     * Class constructor.
     *
     * @param bgColors     An array of background colors
     * @param strokeColors An array of stroke colors.
     *                     the length of stroke colors should be the same
     *                     as the length of background colors
     */
    public CustomStampColorAdapter(int[] bgColors, int[] strokeColors) {
        if (bgColors.length != strokeColors.length) {
            return;
        }
        mBgColors = bgColors;
        mStrokeColors = strokeColors;
    }

    /**
     * Select an icon.
     *
     * @param index The index of icon that should be selected
     */
    public void select(int index) {
        if (mSelectedIndex >= 0 && mSelectedIndex < mBgColors.length) {
            mSelectedIndex = index;
        }
    }

    /**
     * @return The selected index in adapter
     */
    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_color_icon, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mInnerIcon.setColorFilter(mBgColors[position], PorterDuff.Mode.SRC_IN);
        holder.mStrokeIcon.setColorFilter(mStrokeColors[position], PorterDuff.Mode.SRC_IN);
        holder.mOuterIcon.setVisibility(position == mSelectedIndex ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        return mStrokeColors == null ? 0 : mStrokeColors.length;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        AppCompatImageView mInnerIcon, mOuterIcon, mStrokeIcon;

        public ViewHolder(View itemView) {
            super(itemView);
            Context context = itemView.getContext();
            mOuterIcon = itemView.findViewById(R.id.select_bg_icon);
            setSize(mOuterIcon, Math.round(Utils.convDp2Pix(context, 40)));
            mInnerIcon = itemView.findViewById(R.id.bg_icon);
            setSize(mInnerIcon, Math.round(Utils.convDp2Pix(context, 36)));
            mStrokeIcon = itemView.findViewById(R.id.fg_icon);
            setSize(mStrokeIcon, Math.round(Utils.convDp2Pix(context, 20)));
        }

        private void setSize(@NonNull View view, int size) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = size;
            layoutParams.height = size;
            view.setLayoutParams(layoutParams);
        }

    }

}
