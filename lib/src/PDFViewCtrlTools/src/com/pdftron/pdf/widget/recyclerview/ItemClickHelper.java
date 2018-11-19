//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.widget.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.pdftron.pdf.utils.Utils;

/**
 * Helper class to receive the click and long click events on the items of a recycler view
 */
public class ItemClickHelper implements RecyclerView.OnChildAttachStateChangeListener {

    private RecyclerView mRecyclerView;

    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;

    public ItemClickHelper() {}

    /**
     * Calls this method to enable receiving click and long click events on its items.
     *
     * @param recyclerView The recycler view
     */
    public void attachToRecyclerView(RecyclerView recyclerView) {
        if (mRecyclerView == recyclerView) {
            return;
        }
        if (mRecyclerView != null) {
            mRecyclerView.removeOnChildAttachStateChangeListener(this);
        }
        mRecyclerView = recyclerView;
        if (mRecyclerView != null) {
            mRecyclerView.addOnChildAttachStateChangeListener(this);
        }
    }

    /**
     * Callback interface invoked when an item is clicked.
     */
    public interface OnItemClickListener {
        /**
         * Called when an item in the attached recycler view has been clicked.
         *
         * @param recyclerView The attached recycler view
         * @param view The item view that was clicked
         * @param position The position of the item in the recycler view
         * @param id The item's id if adapter has stable ids, {@link RecyclerView#NO_ID}
         * otherwise
         */
        void onItemClick(RecyclerView recyclerView, View view, int position, long id);
    }

    /**
     * Callback interface invoked when an item is long clicked.
     */
    public interface OnItemLongClickListener {
        /**
         * Called when an item in the attached recycler view has been long clicked.
         *
         * @param recyclerView The attached recycler view
         * @param view The item view that was long clicked
         * @param position The position of the item in the recycler view
         * @param id The ID
         */
        boolean onItemLongClick(RecyclerView recyclerView, View view, int position, long id);
    }

    /**
     * Sets the {@link OnItemClickListener} listener.
     *
     * @param listener The listener
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    /**
     * Sets the {@link OnItemLongClickListener} listener.
     *
     * @param listener The listener
     */
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        mOnItemLongClickListener = listener;
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
        if (mOnItemClickListener != null) {
            view.setOnClickListener(mOnClickListener);
        }
        if (mOnItemLongClickListener != null) {
            view.setOnLongClickListener(mOnLongClickListener);
        }
        if (Utils.isMarshmallow()) {
            view.setOnContextClickListener(new View.OnContextClickListener() {
                @Override
                public boolean onContextClick(View v) {
                    return v.performLongClick();
                }
            });
        }
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {}

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mOnItemClickListener != null) {
                final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
                if (holder != null) {
                    int pos = holder.getAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) {
                        return;
                    }
                    mOnItemClickListener.onItemClick(mRecyclerView, view, pos, holder.getItemId());
                }
            }
        }
    };

    private View.OnLongClickListener mOnLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            if (mOnItemLongClickListener != null) {
                final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
                if (holder != null) {
                    int pos = holder.getAdapterPosition();
                    return pos != RecyclerView.NO_POSITION
                        && mOnItemLongClickListener.onItemLongClick(mRecyclerView, view, pos, holder.getItemId());
                }
            }
            return false;
        }
    };
}
