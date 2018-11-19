//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.widget.recyclerview;

import android.support.v7.widget.RecyclerView;

/**
 * Callback interface to be invoked when a view holder is bound to an adapter position.
 * @param <VH>
 */
public interface ViewHolderBindListener<VH extends RecyclerView.ViewHolder> {
    /**
     * Called when a view holder has been bound to an adapter position.
     *
     * @param holder The view holder
     * @param position The position of the item withing the adapter's data set
     */
    void onBindViewHolder(VH holder, int position);
}
