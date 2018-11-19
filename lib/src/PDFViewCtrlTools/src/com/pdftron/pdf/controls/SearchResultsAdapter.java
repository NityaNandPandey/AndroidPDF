//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------


package com.pdftron.pdf.controls;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.pdftron.pdf.TextSearchResult;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

/**
 * A {@link TextSearchResult} array adapter for showing search results
 */
public class SearchResultsAdapter extends ArrayAdapter<TextSearchResult> {

    private Context mContext;
    private int mLayoutResourceId;
    private ArrayList<TextSearchResult> mResults;
    private ArrayList<String> mSectionTitles;
    private boolean mIsRtlMode;

    /**
     * Class constructor
     * @param context The context
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * @param objects The objects to represent in the ListView.
     * @param titles The section titles
     */
    SearchResultsAdapter(Context context, int resource, ArrayList<TextSearchResult> objects,
                                ArrayList<String> titles) {
        super(context, resource, objects);

        mContext = context;
        mLayoutResourceId = resource;
        mResults = objects;
        mSectionTitles = titles;
        mIsRtlMode = false;
    }


    /**
     * Overload implementation of {@link ArrayAdapter#getCount()}
     * @return search result size.
     */
    @Override
    public int getCount() {
        if (mResults != null) {
            return mResults.size();
        } else {
            return 0;
        }
    }

    /**
     * Overload implementation of {@link ArrayAdapter#getItem(int)}
     * Gets the {@link TextSearchResult} item associated with the specified position in the search result set.
     *
     * @param position Position of the item whose data we want within the adapter's
     * search result set.
     * @return The search result at the specified position.
     */
    @Override
    public TextSearchResult getItem(int position) {
        if (mResults != null && position >= 0 && position < mResults.size()) {
            return mResults.get(position);
        }
        return null;
    }

    /**
     * Overload implementation of {@link ArrayAdapter#getView(int, View, ViewGroup)}
     *
     * @param position The position of the search result item
     *        we want.
     * @param convertView The old view to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to
     * @return A Linear layout contains section title, search text, and page number
     * corresponding to the search result data at the specified position.
     */
    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(mLayoutResourceId, parent, false);

            holder = new ViewHolder();
            holder.mSectionTitle = convertView.findViewById(R.id.section_title);
            holder.mPageNumber = convertView.findViewById(R.id.page_number);
            holder.mSearchText = convertView.findViewById(R.id.search_text);
            if (Utils.isJellyBeanMR1()) {
                holder.mSectionTitle.setTextDirection(View.TEXT_DIRECTION_LOCALE);
                holder.mPageNumber.setTextDirection(View.TEXT_DIRECTION_LOCALE);
                if (Utils.isRtlLayout(getContext())) {
                    holder.mSectionTitle.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                    holder.mPageNumber.setTextDirection(View.TEXT_DIRECTION_LTR);
                } else {
                    holder.mSectionTitle.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                    holder.mPageNumber.setTextDirection(View.TEXT_DIRECTION_RTL);
                }
                if (mIsRtlMode) {
                    holder.mSearchText.setTextDirection(View.TEXT_DIRECTION_RTL);
                    holder.mSearchText.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
                } else {
                    holder.mSearchText.setTextDirection(View.TEXT_DIRECTION_LTR);
                    holder.mSearchText.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                }
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        TextSearchResult result = getItem(position);
        if (result != null) {
            holder.mSearchText.setText(formatResultText(result));
            holder.mPageNumber.setText(mContext.getResources().getString(R.string.controls_annotation_dialog_page, result.getPageNum()));
            holder.mSectionTitle.setText(formatSectionTitle(position));
        }

        return convertView;
    }

    private SpannableStringBuilder formatResultText(TextSearchResult result) {
        String match = result.getResultStr();
        String ambient = result.getAmbientStr();

        // a temporarily hack on right-to-left scripts until the core will support RTL in search
        ambient = Utils.getBidiString(ambient);
        match = Utils.getBidiString(match);

        // Break ambient string into two parts: before and after the match
        int start = ambient.indexOf(match); // Get first occurrence of match in ambient
        int end = start + match.length(); // Get end of first occurrence

        if (start < 0 || end > ambient.length()) {
            // Should never happen
            AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("start/end of result text is invalid -> " + "match: " + match + ", ambient: " + ambient + ", start: " + start + "end:" + end));
            start = end = 0;
        }

        SpannableStringBuilder builder = new SpannableStringBuilder(ambient);
        builder.setSpan(new BackgroundColorSpan(mContext.getResources().getColor(R.color.controls_search_results_popup_highlight)),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    private String formatSectionTitle(int position) {
        if (!mSectionTitles.isEmpty()) {
            String text = mSectionTitles.get(position);
            if (text != null) {
                return text;
            }
        }
        // Bookmark list is not set or section can't be determined
        return "";
    }

    void setRtlMode(boolean isRtlMode) {
        mIsRtlMode = isRtlMode;
    }

    private static class ViewHolder {
        TextView mSectionTitle;
        TextView mPageNumber;
        TextView mSearchText;
    }
}
