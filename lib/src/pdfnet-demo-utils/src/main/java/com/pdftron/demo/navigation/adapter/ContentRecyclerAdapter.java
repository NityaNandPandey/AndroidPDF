//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation.adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pdftron.demo.R;
import com.pdftron.demo.R2;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ContentRecyclerAdapter extends RecyclerView.Adapter<ContentRecyclerAdapter.ItemViewHolder> {

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_BUTTON = 1;
    public static final int VIEW_TYPE_CONTENT = 2;
    public static final int VIEW_TYPE_DIVIDER = 3;

    private Context mContext;
    private JsonArray mJsonItems;

    private RecyclerView mRecyclerView;

    private ItemClickHelper.OnItemClickListener mOnItemClickListener;

    private ColorFilter mButtonColorFilter;
    private int mAccentColor;

    public ContentRecyclerAdapter(Context context, JsonArray jsonItems, @ColorInt int buttonColor, @ColorInt int accentColor) {
        mContext = context;
        mJsonItems = jsonItems;

        mButtonColorFilter = new PorterDuffColorFilter(buttonColor, PorterDuff.Mode.SRC_IN);
        mAccentColor = accentColor;

        setHasStableIds(true);
    }

    public void setOnItemClickListener(ItemClickHelper.OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                view = LayoutInflater.from(mContext).inflate(R.layout.file_info_drawer_header_item, parent, false);
                break;
            case VIEW_TYPE_BUTTON:
                view = LayoutInflater.from(mContext).inflate(R.layout.file_info_drawer_button_item, parent, false);
                break;
            case VIEW_TYPE_CONTENT:
                view = LayoutInflater.from(mContext).inflate(R.layout.file_info_drawer_content_item, parent, false);
                break;
            case VIEW_TYPE_DIVIDER:
                view = LayoutInflater.from(mContext).inflate(R.layout.file_info_drawer_divider_item, parent, false);
                break;
            default:
                throw new IllegalArgumentException("View type " + viewType + " is not supported");
        }
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        final JsonObject jsonObject = mJsonItems.get(position).getAsJsonObject();
        int iconResId;
        switch (getItemViewType(position)) {
            case VIEW_TYPE_HEADER:
                String header = jsonObject.get(ContentInfoKey.Text).getAsString();
                if (holder.textView != null) {
                    holder.textView.setText(header);
                }
                break;
            case VIEW_TYPE_BUTTON:
                String condensedTitle = jsonObject.get(ContentInfoKey.Text).getAsString();
                String longTitle = jsonObject.get(ContentInfoKey.LongText).getAsString();
                iconResId = jsonObject.get(ContentInfoKey.Icon).getAsInt();
                // Display the condensed title
                if (holder.textView != null && holder.imageView != null) {
                    holder.textView.setText(condensedTitle);

                    if (iconResId > 0) {
                        // Show icon and adjust color appropriately
                        holder.imageView.setVisibility(View.VISIBLE);
                        holder.imageView.setImageResource(iconResId);
                        holder.imageView.getDrawable().mutate().setColorFilter(mButtonColorFilter);
                    } else {
                        // No icon for menu item - hide
                        holder.imageView.setVisibility(View.GONE);
                    }
                    holder.imageView.setContentDescription(longTitle);
                }
                break;
            case VIEW_TYPE_CONTENT:
                String content = jsonObject.get(ContentInfoKey.Text).getAsString();
                boolean isHtml = jsonObject.get(ContentInfoKey.IsHtml).getAsBoolean();
                iconResId = (!jsonObject.get(ContentInfoKey.Icon).isJsonNull() ? jsonObject.get(ContentInfoKey.Icon).getAsInt() : 0);
                if (holder.textView != null) {
                    if (isHtml) {
                        // Handle HTML content
                        SpannableStringBuilder builder = new SpannableStringBuilder(Html.fromHtml(content));
                        boldToColorSpans(builder);
                        holder.textView.setText(builder);
                    } else {
                        holder.textView.setText(content);
                    }
                }
                if (holder.imageView != null) {
                    if (iconResId > 0) {
                        // Show icon and adjust color appropriately
                        holder.imageView.setVisibility(View.VISIBLE);
                        //noinspection ResourceType
                        holder.imageView.setImageResource(iconResId);
                        holder.imageView.getDrawable().mutate().setColorFilter(mButtonColorFilter);
                    } else {
                        // No icon for menu item - hide
                        holder.imageView.setVisibility(View.GONE);
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        final JsonObject jsonObject = mJsonItems.get(position).getAsJsonObject();

        boolean isHeader = jsonObject.get(ContentInfoKey.IsHeader).getAsBoolean();
        if (isHeader) {
            return VIEW_TYPE_HEADER;
        }

        boolean isDivider = jsonObject.get(ContentInfoKey.IsDivider).getAsBoolean();
        if (isDivider) {
            return VIEW_TYPE_DIVIDER;
        }

        int id = jsonObject.get(ContentInfoKey.Id).getAsInt();
        if (id > 0) {
            return VIEW_TYPE_BUTTON;
        }

        return VIEW_TYPE_CONTENT;
    }

    @Override
    public int getItemCount() {
        return mJsonItems.size();
    }

    @Override
    public long getItemId(int position) {
        JsonObject jsonObject = mJsonItems.get(position).getAsJsonObject();
        int itemId = jsonObject.get(ContentInfoKey.Id).getAsInt();
        if (itemId > 0) {
            return itemId;
        } else {
            return position;
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mRecyclerView = null;
    }

    private void boldToColorSpans(SpannableStringBuilder builder) {
        // Get all StyleSpans in the builder
        StyleSpan[] spans = builder.getSpans(0, builder.length(), StyleSpan.class);
        for (StyleSpan span : spans) {
            if (span.getStyle() == Typeface.BOLD) {
                // Create new color span to replace the bold span
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(mAccentColor);
                int spanStart = builder.getSpanStart(span);
                int spanEnd = builder.getSpanEnd(span);
                int spanFlags = builder.getSpanFlags(span);
                builder.removeSpan(span);
                builder.setSpan(colorSpan, spanStart, spanEnd, spanFlags);
            }
        }
    }

    class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @Nullable
        @BindView(R2.id.item_text)
        TextView textView;
        @Nullable
        @BindView(R2.id.item_icon)
        ImageView imageView;

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        ItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(ItemViewHolder.this, itemView);

            if (imageView != null) {
                imageView.setOnClickListener(this);
            }

            if (Utils.isJellyBeanMR1() && textView != null) {
                textView.setTextDirection(View.TEXT_DIRECTION_LOCALE);
            }
        }

        @Override
        public void onClick(View v) {
            int pos = getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(mRecyclerView, itemView, pos, getItemId());
            }
        }
    }

    public class ContentInfoKey {
        public static final String Id = "id";
        public static final String Icon = "icon";
        public static final String Text = "text";
        public static final String LongText = "long_text";
        public static final String IsHeader = "is_header";
        public static final String IsDivider = "is_divider";
        public static final String IsHtml = "is_html";
    }
}
