//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation.adapter.viewholder;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.pdftron.demo.R;
import com.pdftron.demo.R2;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HeaderViewHolder extends RecyclerView.ViewHolder {

    String TAG = HeaderViewHolder.class.getName();
    @BindView(R2.id.title)
    public TextView textViewTitle;
    @BindView(R2.id.folding_btn)
    public AppCompatImageView foldingBtn;
    @BindView(R2.id.header_view)
    public View header_view;
    @BindView(R2.id.divider)
    public ImageView divider;

    public ArrayList<BaseFileInfo> childList;

    private boolean collapse = false;
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public HeaderViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(HeaderViewHolder.this, itemView);
        divider.setVisibility(View.GONE);
        childList = new ArrayList<>();
        if (Utils.isJellyBeanMR1() && textViewTitle != null) {
            // instead of creating a different layout for v17 we set alignment in the code:
            if (textViewTitle.getGravity() != Gravity.CENTER) {
                textViewTitle.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }
            textViewTitle.setTextDirection(View.TEXT_DIRECTION_LTR);
        }
    }


    public void toggleBtnDrawbale() {
        if(collapse){
            foldingBtn.setImageResource(R.drawable.ic_arrow_down_white_24dp);
        }
        else{
            foldingBtn.setImageResource(R.drawable.ic_arrow_up_white_24dp);
        }
        collapse = !collapse;
    }


}
