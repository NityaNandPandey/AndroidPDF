package com.pdftron.pdf.widget.recyclerview;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

//import com.tonicartos.superslim.LayoutManager;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;
import co.paulburke.android.itemtouchhelperdemo.helper.SimpleItemTouchHelperCallback;

/**
 * An implementation of {@link SimpleItemTouchHelperCallback} that enables basic drag & drop and
 * swipe-to-dismiss or grid layout manager only.
 */
public class ItemTouchHelperCallback extends SimpleItemTouchHelperCallback {
    public ItemTouchHelperCallback(ItemTouchHelperAdapter adapter, int span, boolean enableLongPress, boolean enableSwipe) {
        super(adapter, span, enableLongPress, enableSwipe);
    }

    /**
     * Overload implementation of {@link SimpleItemTouchHelperCallback#getMovementFlags(RecyclerView, RecyclerView.ViewHolder)}
     */
    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        // Set movement flags based on the layout manager
        if (viewHolder.getItemViewType() == VIEW_TYPE_HEADER) {
            return makeMovementFlags(0, 0);
        }
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager
                || (recyclerView.getLayoutManager() instanceof RecyclerView.LayoutManager && mSpan > 1)) {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
            final int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        } else {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            final int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        }
    }
}
