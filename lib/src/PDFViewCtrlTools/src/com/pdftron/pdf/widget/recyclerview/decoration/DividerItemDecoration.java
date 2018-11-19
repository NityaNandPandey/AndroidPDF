package com.pdftron.pdf.widget.recyclerview.decoration;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    private static final int[] ATTRS = { android.R.attr.listDivider };

    private Drawable mDivider;

    private DividerLookup mDividerLookup;

    /**
     * Default list divider will be used
     */
    public DividerItemDecoration(Context context) {
        final TypedArray styledAttributes = context.obtainStyledAttributes(ATTRS);
        mDivider = styledAttributes.getDrawable(0);
        styledAttributes.recycle();
    }

    public DividerItemDecoration(Context context, @DrawableRes int resId) {
        mDivider = ContextCompat.getDrawable(context, resId);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
                               RecyclerView parent, RecyclerView.State state) {
        boolean drawDivider = true;

        final int position = getChildPosition(parent, view);
        if (position == RecyclerView.NO_POSITION || view == null) {
            drawDivider = false;
        } else if (mDividerLookup != null) {
            drawDivider = mDividerLookup.drawDivider(position);
        }

        if (drawDivider) {
            outRect.set(0, 0, 0, (mDivider != null) ? mDivider.getIntrinsicHeight(): 0);
        } else {
            outRect.setEmpty();
        }
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final RecyclerView.LayoutManager lm = parent.getLayoutManager();
        final int childCount = parent.getChildCount();

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            boolean drawDivider = true;

            if (child == null) {
                continue;
            }
            if (mDividerLookup != null) {
                int position = getChildPosition(parent, child);
                if (position == RecyclerView.NO_POSITION) {
                    continue;
                }
                // Look up whether a divider should be drawn for this position
                drawDivider = mDividerLookup.drawDivider(position);
            }
            if (!drawDivider) {
                continue;
            }
//            final int childLeft = lm.getDecoratedLeft(child);
//            final int childRight = lm.getDecoratedRight(child);
//            final int childBottom = lm.getDecoratedBottom(child);
//
//            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
//
//            final int bottomOffset = childBottom - child.getBottom() - params.bottomMargin;
//            if (bottomOffset > 0) { // Only draw if a bottom offset was set in getItemOffsets()
//                final int top = childBottom - bottomOffset;
//                final int bottom = top + mDivider.getIntrinsicHeight();
//                mDivider.setBounds(childLeft, top, childRight, bottom);
//                mDivider.draw(c);
//            }
        }
    }

    private int getChildPosition(RecyclerView parent, View child) {
        // Try to use adapter position first
        int position = parent.getChildAdapterPosition(child);
        if (position == RecyclerView.NO_POSITION) {
            // Try layout position next
            position = parent.getChildLayoutPosition(child);
        }
        return position;
    }

    public interface DividerLookup {
        boolean drawDivider(int position);
    }

    public void setDividerLookup(DividerLookup dividerLookup) {
        mDividerLookup = dividerLookup;
    }
}
