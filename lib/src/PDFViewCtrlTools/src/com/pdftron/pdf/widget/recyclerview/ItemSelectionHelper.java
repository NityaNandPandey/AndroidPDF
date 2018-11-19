//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.widget.recyclerview;

import android.support.v4.util.LongSparseArray;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.widget.Checkable;

import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;

/**
 * RecyclerView plugin to support item check-states.
 */
public class ItemSelectionHelper implements ViewHolderBindListener {

    static final String TAG = ItemSelectionHelper.class.getName();

    public static final int INVALID_POSITION = -1;

    public static final int CHOICE_MODE_NONE = 0;
    public static final int CHOICE_MODE_SINGLE = 1;
    public static final int CHOICE_MODE_MULTIPLE = 2;

    private RecyclerView mRecyclerView;

    private int mChoiceMode = CHOICE_MODE_NONE;
    private SparseBooleanArray mCheckStates;
    private LongSparseArray<Integer> mCheckedIdStates;
    private int mCheckedItemCount;

    public ItemSelectionHelper() {}

    public void attachToRecyclerView(RecyclerView recyclerView) {
        if (mRecyclerView == recyclerView) {
            return;
        }
        if (mRecyclerView != null) {
            clearChoices();
        }
        mRecyclerView = recyclerView;
    }

    public int getCheckedItemCount() {
        return mCheckedItemCount;
    }

    public boolean isItemChecked(int position) {
        if (mChoiceMode != CHOICE_MODE_NONE && mCheckStates != null) {
            return mCheckStates.get(position);
        }
        return false;
    }

    public int getCheckedItemPosition() {
        if (mChoiceMode == CHOICE_MODE_SINGLE && mCheckStates != null && mCheckStates.size() == 1) {
            return mCheckStates.keyAt(0);
        }
        return INVALID_POSITION;
    }

    public SparseBooleanArray getCheckedItemPositions() {
        if (mChoiceMode != CHOICE_MODE_NONE) {
            return mCheckStates;
        }
        return null;
    }

    public long[] getCheckedItemIds() {
        if (mChoiceMode == CHOICE_MODE_NONE || mCheckedIdStates == null || mRecyclerView.getAdapter() == null) {
            return new long[0];
        }

        final LongSparseArray<Integer> idStates = mCheckedIdStates;
        final int count = idStates.size();
        final long[] ids = new long[count];

        for (int i = 0; i < count; i++) {
            ids[i] = idStates.keyAt(i);
        }

        return ids;
    }

    public void setItemChecked(int position, boolean checked) {
        if (mChoiceMode == CHOICE_MODE_NONE || mRecyclerView.getAdapter() == null) {
            return;
        }

        final RecyclerView.Adapter adapter = mRecyclerView.getAdapter();

        if (mChoiceMode == CHOICE_MODE_MULTIPLE) {
            boolean oldValue = mCheckStates.get(position);
            mCheckStates.put(position, checked);
            if (mCheckedIdStates != null && adapter.hasStableIds()) {
                if (checked) {
                    mCheckedIdStates.put(adapter.getItemId(position), position);
                } else {
                    mCheckedIdStates.delete(adapter.getItemId(position));
                }
            }
            if (oldValue != checked) {
                if (checked) {
                    mCheckedItemCount++;
                } else {
                    mCheckedItemCount--;
                }
                if (ViewCompat.getLayoutDirection(mRecyclerView) == ViewCompat.LAYOUT_DIRECTION_RTL) {
                    // work around issue where in RTL touch position
                    // randomly jumping
                    Utils.safeNotifyDataSetChanged(adapter);
                } else {
                    Utils.safeNotifyItemChanged(adapter, position);
                }
            }
        } else {
            int oldCheckedPosition = getCheckedItemPosition();
            boolean updateIds = mCheckedIdStates != null && adapter.hasStableIds();
            // Clear all values if we're checking something, or unchecking the currently
            // selected item
            if (checked || isItemChecked(position)) {
                mCheckStates.clear();
                if (updateIds) {
                    mCheckedIdStates.clear();
                }
            }
            // This may end up selecting the checked we just cleared but this way
            // we ensure length of mCheckStates is 1, a fact getCheckedItemPosition relies on
            if (checked) {
                mCheckStates.put(position, true);
                if (updateIds) {
                    mCheckedIdStates.put(adapter.getItemId(position), position);
                }
                mCheckedItemCount = 1;
            } else if (mCheckStates.size() == 0 || !mCheckStates.valueAt(0)) {
                mCheckedItemCount = 0;
            }

            if (oldCheckedPosition != INVALID_POSITION && oldCheckedPosition != position) {
                Utils.safeNotifyItemChanged(adapter, oldCheckedPosition);
            }
            Utils.safeNotifyItemChanged(adapter, position);
        }
    }

    public void clearChoices() {
        if (mCheckStates != null) {
            mCheckStates.clear();
        }
        if (mCheckedIdStates != null) {
            mCheckedIdStates.clear();
        }
        mCheckedItemCount = 0;

        Utils.safeNotifyDataSetChanged(mRecyclerView.getAdapter());
    }

    public int getChoiceMode() {
        return mChoiceMode;
    }

    // NOTE: Must be called after the RecyclerView is attached
    public void setChoiceMode(int choiceMode) {
        if (mChoiceMode == choiceMode) {
            return;
        }

        mChoiceMode = choiceMode;

        if (mChoiceMode != CHOICE_MODE_NONE) {
            if (mCheckStates == null) {
                mCheckStates = new SparseBooleanArray();
            }

            final RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
            if (mCheckedIdStates == null && adapter != null && adapter.hasStableIds()) {
                mCheckedIdStates = new LongSparseArray<>();
            }
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder.itemView instanceof Checkable) {
            ((Checkable) holder.itemView).setChecked(isItemChecked(position));
        } else {
            holder.itemView.setActivated(isItemChecked(position));
        }
    }

    public RecyclerView.AdapterDataObserver getDataObserver() {
        return mDataObserver;
    }

    private RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            if (mCheckStates != null) {
                final RecyclerView.Adapter adapter = mRecyclerView.getAdapter();

                SparseBooleanArray states = new SparseBooleanArray();
                LongSparseArray<Integer> ids = null;
                if (mCheckedIdStates != null && adapter != null && adapter.hasStableIds()) {
                    ids = new LongSparseArray<Integer>();
                }
                // Copy the checked states to a new array, excluding the removed-range and
                // adjusting positions after the range
                for (int i = 0; i < mCheckStates.size(); i++) {
                    int position = mCheckStates.keyAt(i);
                    if (position >= positionStart && position < positionStart+itemCount) {
                        // In removed-range - skip
                        continue;
                    } else if (position >= positionStart+itemCount) {
                        // After removed-range - position changed
                        position = position - itemCount;
                    }
                    states.put(position, mCheckStates.valueAt(i));
                    if (ids != null) {
                        // Update ids
                        ids.put(adapter.getItemId(position), position);
                    }
                }
                mCheckStates = states;
                if (ids != null) {
                    mCheckedIdStates = ids;
                }
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (mCheckStates != null) {
                final RecyclerView.Adapter adapter = mRecyclerView.getAdapter();

                SparseBooleanArray states = new SparseBooleanArray();
                LongSparseArray<Integer> ids = null;
                if (mCheckedIdStates != null && adapter != null && adapter.hasStableIds()) {
                    ids = new LongSparseArray<Integer>();
                }
                // Copy the checked states to a new array, shifting positions after the start-position
                for (int i = 0; i < mCheckStates.size(); i++) {
                    int position = mCheckStates.keyAt(i);
                    if (position >= positionStart) {
                        // Shift position
                        position = position + itemCount;
                    }
                    states.put(position, mCheckStates.valueAt(i));
                    if (ids != null) {
                        // Update ids
                        ids.put(adapter.getItemId(position), position);
                    }
                }
                mCheckStates = states;
                if (ids != null) {
                    mCheckedIdStates = ids;
                }
            }
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            if (mCheckStates != null) {
                final RecyclerView.Adapter adapter = mRecyclerView.getAdapter();

                SparseBooleanArray states = new SparseBooleanArray();
                boolean updateIds = mCheckedIdStates != null && adapter != null && adapter.hasStableIds();

                // Save the checked-states and ids of the moved items
                for (int i = fromPosition; i < fromPosition+itemCount; i++) {
                    int position = toPosition + (i - fromPosition);
                    boolean value = mCheckStates.get(i);
                    states.put(position, value);
                }

                onItemRangeRemoved(fromPosition, itemCount);
                onItemRangeInserted(toPosition, itemCount);

                // Update checked-states and ids of the moved items, in their new positions
                for (int i = 0; i < states.size(); i++) {
                    int position = states.keyAt(i);
                    mCheckStates.put(position, states.valueAt(i));
                    if (updateIds) {
                        mCheckedIdStates.put(adapter.getItemId(position), position);
                    }
                }
            }
        }
    };
}
