//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------


package com.pdftron.demo.navigation.adapter.viewholder;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.pdftron.demo.R2;
import com.pdftron.pdf.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.pdftron.demo.widget.ImageViewTopCrop;

public class ContentViewHolder extends RecyclerView.ViewHolder {

    @BindView(R2.id.file_icon)
    public ImageViewTopCrop imageViewFileIcon;
    @BindView(R2.id.file_lock_icon)
    public ImageView imageViewFileLockIcon;
    @BindView(R2.id.docTextPlaceHolder)
    public TextView docTextPlaceHolder;
    @BindView(R2.id.file_name)
    public TextView textViewFileName;
    @BindView(R2.id.file_info)
    public TextView textViewFileInfo;
    @BindView(R2.id.info_icon)
    public ImageView imageViewInfoIcon;
    @BindView(R2.id.info_button)
    public View infoButton;
    @BindView(R2.id.divider)
    public ImageView divider;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public ContentViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(ContentViewHolder.this, itemView);
        if (Utils.isJellyBeanMR1() && textViewFileName != null && textViewFileInfo != null) {
            // instead of creating a different layout for v17 we set alignment in the code:
            if (textViewFileName.getGravity() != Gravity.CENTER) {
                textViewFileName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }
            textViewFileName.setTextDirection(View.TEXT_DIRECTION_LTR);
            textViewFileInfo.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        }
    }
}
