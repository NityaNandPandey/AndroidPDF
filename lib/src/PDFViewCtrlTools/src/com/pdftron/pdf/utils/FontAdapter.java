//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.tools.R;

import java.util.ArrayList;
import java.util.List;


/**
 * Adapter for fonts
 */
public class FontAdapter extends ArrayAdapter<FontResource> {

    private Context mContext;
    private List<FontResource> mSource;
    private int mLayoutResourceId;

    public FontAdapter(Context context, int textViewResource, List<FontResource> list) {
        super(context, textViewResource, list);

        mContext = context;
        mSource = list;
        mLayoutResourceId = textViewResource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(mLayoutResourceId, parent, false);
        }

        // get the language and set the text as the language name
        TextView textView = convertView.findViewById(R.id.fonts_row_item_text);
        FontResource font = mSource.get(position);
        textView.setText(font.getDisplayName());
        textView.setPadding(0, 0, 0, 0);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        // cannot reuse convertView since for the first position we want the height
        // of the text view to be zero - this hides the font selection hint
        convertView = LayoutInflater.from(mContext).inflate(mLayoutResourceId, null);

        // get the language and set the text as the language name
        TextView textView = convertView.findViewById(R.id.fonts_row_item_text);
        FontResource font = mSource.get(position);
        textView.setText(font.getDisplayName());
        int padding = (int) mContext.getResources().getDimension(R.dimen.padding_medium);
        textView.setPadding(padding, padding, padding, padding);

        if (position == 0) {
            textView.setHeight(0);
        }

        return convertView;
    }

    @Override
    public int getPosition(FontResource font) {
        if (font == null) {
            return -1;
        }

        String filePath = font.getFilePath();
        for (int i = 0; i < mSource.size(); i++) {
            if (mSource.get(i).getFilePath().equals(filePath)) {
                return i;
            }
        }
        return -1;
    }

    public void setData(List<FontResource> data) {
        if (mSource == null) {
            mSource = new ArrayList<>();
        }
        mSource.clear();
        mSource.addAll(data);
        notifyDataSetChanged();
    }

    public List<FontResource> getData() {
        if (mSource == null) {
            mSource = new ArrayList<>();
        }
        return mSource;
    }
}
