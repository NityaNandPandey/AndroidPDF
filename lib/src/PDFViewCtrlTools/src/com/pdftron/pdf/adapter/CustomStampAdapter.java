//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pdftron.pdf.model.CustomStampOption;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerViewAdapter;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;

public class CustomStampAdapter extends SimpleRecyclerViewAdapter<Bitmap, CustomStampAdapter.ViewHolder>
    implements ItemTouchHelperAdapter {

    private WeakReference<Context> mContextRef;
    private List<WeakReference<Bitmap>> mBitmapsRef = new ArrayList<>();

    /**
     * Class constructor
     *
     * @param context The context
     */
    public CustomStampAdapter(@NonNull Context context, ViewHolderBindListener bindListener) {
        super(bindListener);
        mContextRef = new WeakReference<>(context);
        int count = CustomStampOption.getCustomStampsCount(context);
        for (int i = 0; i < count; ++i) {
            mBitmapsRef.add(new WeakReference<Bitmap>(null));
        }
    }

    /**
     * should be called when a custom rubber stamp has been updated.
     *
     * @param item     The bitmap of the updated custom rubber stamp
     * @param position The zero-indexed position of custom rubber stamp in the adapter
     */
    public void onCustomStampUpdated(Bitmap item, int position) {
        mBitmapsRef.set(position, new WeakReference<>(item));
        notifyItemChanged(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_rubber_stamp, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        holder.mStampView.setImageBitmap(getItem(position));
    }

    @Override
    public int getItemCount() {
        return mBitmapsRef.size();
    }

    @Override
    public Bitmap getItem(int position) {
        if (position < 0 || position >= mBitmapsRef.size()) {
            return null;
        }

        Bitmap bitmap = mBitmapsRef.get(position).get();
        if (bitmap == null) {
            Context context = mContextRef.get();
            if (context == null) {
                return null;
            }
            bitmap = CustomStampOption.getCustomStampBitmap(context, position);
            if (bitmap == null) {
                // the bitmap should have been saved when it was created for the
                // first time, so we expect it would be in the disk.
                // even if a user clear cache then all custom bitmaps should be
                // removed and not only bitmaps since all are saved in cache directory
                AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("The bitmap of stamp is not stored in the disk! position: " + position));
            }
            mBitmapsRef.set(position, new WeakReference<>(bitmap));
        }
        return bitmap;
    }

    @Override
    public void add(Bitmap item) {
        mBitmapsRef.add(new WeakReference<>(item));
    }

    @Override
    public boolean remove(Bitmap item) {
        for (WeakReference<Bitmap> bitmapRef : mBitmapsRef) {
            Bitmap bitmap = bitmapRef.get();
            if (bitmap == item) {
                mBitmapsRef.remove(bitmapRef);
                return true;
            }
        }

        return false;
    }

    @Override
    public Bitmap removeAt(int position) {
        return mBitmapsRef.remove(position).get();
    }

    @Override
    public void insert(Bitmap item, int position) {
        mBitmapsRef.add(position, new WeakReference<>(item));
    }

    @Override
    public void updateSpanCount(int count) {

    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (toPosition < getItemCount()) {
            Bitmap item = removeAt(fromPosition);
            insert(item, toPosition);
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        return false;
    }

    @Override
    public void onItemDrop(int fromPosition, int toPosition) {
        Context context = mContextRef.get();
        if (context != null && fromPosition != toPosition
            && fromPosition != RecyclerView.NO_POSITION && toPosition != RecyclerView.NO_POSITION) {
            CustomStampOption.moveCustomStamp(context, fromPosition, toPosition);
        }
    }

    @Override
    public void onItemDismiss(int position) {

    }

    class ViewHolder extends RecyclerView.ViewHolder {

        AppCompatImageView mStampView;

        public ViewHolder(View itemView) {
            super(itemView);
            mStampView = itemView.findViewById(R.id.stamp_image_view);
        }

    }

}
