//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.widget.recyclerview;

import android.support.v7.widget.RecyclerView;

public abstract class SimpleRecyclerViewAdapter<T, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    private RecyclerView mRecyclerView;
    private ViewHolderBindListener mBindListener;

    /**
     * Class constructor
     */
    public SimpleRecyclerViewAdapter() {
        this(null);
    }

    /**
     * Class constructor
     */
    public SimpleRecyclerViewAdapter(ViewHolderBindListener listener) {
        mBindListener = listener;
    }

    /**
     * The overloaded implementation of {@link RecyclerView.Adapter#onAttachedToRecyclerView(RecyclerView)}.
     */
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    /**
     * The overloaded implementation of {@link RecyclerView.Adapter#onDetachedFromRecyclerView(RecyclerView)}.
     */
    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = null;
    }

    /**
     * @return the recycler view
     */
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    /**
     * The overloaded implementation of {@link RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int)}.
     */
    @Override
    public void onBindViewHolder(VH holder, int position) {
        if (mBindListener != null) {
            mBindListener.onBindViewHolder(holder, position);
        }
    }

    /**
     * Returns the item in the specified position.
     *
     * @param position The position
     *
     * @return The item
     */
    public abstract T getItem(int position);

    /**
     * Adds the item.
     *
     * @param item The item
     */
    public abstract void add(T item);

    /**
     * Removes the item.
     *
     * @param item The item
     *
     * @return True if the item is removed
     */
    public abstract boolean remove(T item);

    /**
     * Removes the item at the specified position.
     *
     * @param position The position
     *
     * @return The removed item
     */
    public abstract T removeAt(int position);

    /**
     * Inserts the item at the specified position.
     *
     * @param item The item
     *
     * @param position The position
     */
    public abstract void insert(T item, int position);

    /**
     * Updates span count.
     *
     * @param count The span count
     */
    public abstract void updateSpanCount(int count);
}
