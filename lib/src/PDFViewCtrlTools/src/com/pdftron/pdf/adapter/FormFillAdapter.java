//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;

import com.pdftron.pdf.Field;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import java.util.HashSet;


/**
 * Adapter class used in {@link com.pdftron.pdf.tools.DialogFormFillChoice}.
 */
public class FormFillAdapter extends RecyclerView.Adapter<FormFillAdapter.ViewHolder> {

    private Field mField;
    private OnItemSelectListener mOnItemSelectListener;
    private HashSet<Integer> mSelectedPositions;
    private boolean mSingleChoice = true;

    /**
     * Callback interface invoked when an item is selected.
     */
    public interface OnItemSelectListener {
        /**
         * Called when an item has been clicked.
         *
         * @param position The position of the item that was clicked
         */
        void onItemSelected(int position);
    }

    /**
     * Class constructor
     *
     * @param field             The field
     * @param selectedPositions The selected positions
     * @param listener          The listener
     */
    public FormFillAdapter(Field field, HashSet<Integer> selectedPositions, OnItemSelectListener listener) {
        mField = field;
        mSelectedPositions = selectedPositions;
        mOnItemSelectListener = listener;

        try {
            mSingleChoice = mField.getFlag(Field.e_combo) || !mField.getFlag(Field.e_multiselect);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * The overloaded implementation of {@link RecyclerView.Adapter#onCreateViewHolder(ViewGroup, int)}.
     */
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_item_form, parent, false);
        return new ViewHolder(view);
    }

    /**
     * The overloaded implementation of {@link RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int)}.
     */
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        try {
            String text = mField.getOpt(position);
            if (mSingleChoice) {
                holder.checkBox.setVisibility(View.GONE);
                holder.radioButton.setVisibility(View.VISIBLE);
                holder.radioButton.setChecked(mSelectedPositions.contains(position));
                holder.radioButton.setText(text);
            } else {
                holder.radioButton.setVisibility(View.GONE);
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setChecked(mSelectedPositions.contains(position));
                holder.checkBox.setText(text);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Gets selected position for multiple choice cases
     * @return selected position for multiple choice cases
     */
    public HashSet<Integer> getSelectedPositions() {
        return mSelectedPositions;
    }

    /**
     * Gets selected position for single choice cases
     * @return selected position for single choice cases (-1 if no item has been selected)
     */
    public int getSingleSelectedPosition() {
        if (hasSingleSelectedPosition()) {
            return mSelectedPositions.iterator().next();
        }
        return -1;
    }

    /**
     * Whether this form fill is single choice case and is selected
     * @return True if it is single choice case and one item is selected
     */
    public boolean hasSingleSelectedPosition() {
        return mSingleChoice && !mSelectedPositions.isEmpty();
    }

    /**
     * Removes the selected item for single item choice cases
     */
    public void clearSingleSelectedPosition() {
        if (hasSingleSelectedPosition()) {
            mSelectedPositions.clear();
        }
        Utils.safeNotifyDataSetChanged(this);
    }

    /**
     * The overloaded implementation of {@link RecyclerView.Adapter#getItemCount()}.
     */
    @Override
    public int getItemCount() {
        try {
            return mField.getOptCount();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return 0;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        RadioButton radioButton;
        CheckBox checkBox;

        public ViewHolder(View view) {
            super(view);
            radioButton = view.findViewById(R.id.radio_button_form_fill);
            radioButton.setOnClickListener(this);
            checkBox = view.findViewById(R.id.check_box_form_fill);
            checkBox.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int pos = getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }
            if (mSingleChoice) {
                mSelectedPositions.clear();
                mSelectedPositions.add(pos);
                Utils.safeNotifyDataSetChanged(FormFillAdapter.this);
            } else {
                if (mSelectedPositions.contains(pos)) {
                    mSelectedPositions.remove(pos);
                } else {
                    mSelectedPositions.add(pos);
                }
            }
            if (mOnItemSelectListener != null) {
                mOnItemSelectListener.onItemSelected(pos);
            }
            Utils.safeNotifyDataSetChanged(FormFillAdapter.this);
        }
    }
}
