package com.pdftron.pdf.widget.recyclerview.decoration;

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

// TODO: support vertical and horizontal orientations
public class SpacesItemDecoration extends RecyclerView.ItemDecoration {

    private int mSpanCount;
    private int mSpacing;
    private boolean mHeadersEnabled;

    public SpacesItemDecoration(int spanCount, int spacing, boolean headersEnabled) {
        this.mSpanCount = spanCount;
        this.mSpacing = spacing;
        this.mHeadersEnabled = headersEnabled;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
                               RecyclerView parent, RecyclerView.State state) {
        if (mSpanCount > 0) {
            RecyclerView.LayoutManager lm = parent.getLayoutManager();

            int position = parent.getChildAdapterPosition(view); // item position
            int spanIndex;
            int spanSize = 0;
            if (lm instanceof GridLayoutManager) {
                GridLayoutManager gridLm = (GridLayoutManager) lm;
                spanIndex = gridLm.getSpanSizeLookup().getSpanIndex(position, mSpanCount);
                spanSize = gridLm.getSpanSizeLookup().getSpanSize(position);
            } else {
                // Fallback - only works for items with uniform span sizes
                spanIndex = position % mSpanCount; // item column
            }

            if (mHeadersEnabled && spanSize == mSpanCount) {
                outRect.left = 0;
                outRect.right = 0;
            } else {
                outRect.left = mSpacing - spanIndex * mSpacing / mSpanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (spanIndex + 1) * mSpacing / mSpanCount; // (column + 1) * ((1f / spanCount) * spacing)
            }

            if (!mHeadersEnabled && position < mSpanCount) { // top edge
                outRect.top = mSpacing;
            } else {
                outRect.top = 0;
            }
            outRect.bottom = mSpacing; // item bottom
        } else {
            outRect.setEmpty();
        }
    }
}
