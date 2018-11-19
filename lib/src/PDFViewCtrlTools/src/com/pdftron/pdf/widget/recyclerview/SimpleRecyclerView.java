package com.pdftron.pdf.widget.recyclerview;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.widget.recyclerview.decoration.DividerItemDecoration;
import com.pdftron.pdf.widget.recyclerview.decoration.SpacesItemDecoration;
import com.pdftron.pdf.utils.Utils;

public class SimpleRecyclerView extends RecyclerView {

    private static final String TAG = SimpleRecyclerView.class.getName();

    private int mSpanCount = 1;
    private int mGridSpacing;

    private SimpleRecyclerViewAdapter mAdapter;
    private LayoutManager mLayoutManager;
    private GridLayoutManager.SpanSizeLookup mSpanSizeLookup;

    private DividerItemDecoration mDividerDecorator;
    private SpacesItemDecoration mSpacesDecorator;

    public SimpleRecyclerView(Context context) {
        this(context, null);
    }

    public SimpleRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initDefaultView();
    }

    public void initDefaultView() {
        initView(1);
    }

    /**
     * Initializes the recycler view with span count and grid spacing.
     * Grid spacing is default to 4dp.
     *
     * @param spanCount The span count
     */
    public void initView(int spanCount) {
        initView(spanCount, getResources().getDimensionPixelSize(R.dimen.file_list_grid_spacing));
    }

    /**
     * Initializes the recycler view with span count and grid spacing.
     *
     * @param spanCount The span count
     * @param gridSpacing The grid spacing
     */
    public void initView(int spanCount, int gridSpacing) {
        mSpanCount = spanCount;
        mGridSpacing = gridSpacing;
        setHasFixedSize(true);

        if (mSpanCount > 0) {
            mLayoutManager = new GridLayoutManager(getContext(), mSpanCount);
            if (mSpanSizeLookup != null) {
                ((GridLayoutManager) mLayoutManager).setSpanSizeLookup(mSpanSizeLookup);
            }
        } else {
            mLayoutManager = getLinearLayoutManager();
        }
        setLayoutManager(mLayoutManager);

        if(mDividerDecorator != null){
            removeItemDecoration();
            mDividerDecorator = null;
        }
        // Remove old spaces decorator, if it exists
        Utils.safeRemoveItemDecoration(this, mSpacesDecorator);
        mSpacesDecorator = new SpacesItemDecoration(mSpanCount,
            mGridSpacing, false);
        addItemDecoration(mSpacesDecorator);
    }

    public void updateLayoutManager(int count) {
        if ((mSpanCount == 0 && count > 0) || (mSpanCount > 0 && count == 0)) {
            // need to change layout manager
            if (count > 0) {
                mLayoutManager = new GridLayoutManager(getContext(), count);
                if (mSpanSizeLookup != null) {
                    ((GridLayoutManager) mLayoutManager).setSpanSizeLookup(mSpanSizeLookup);
                }
            } else {
                mLayoutManager = getLinearLayoutManager();
            }
            setLayoutManager(mLayoutManager);
        } else {
            // update span count
            if (count > 0) {
                if (mLayoutManager instanceof GridLayoutManager) {
                    ((GridLayoutManager) mLayoutManager).setSpanCount(count);
                    mLayoutManager.requestLayout();
                } else {
                    mLayoutManager = new GridLayoutManager(getContext(), count);
                    setLayoutManager(mLayoutManager);
                }
            }
        }
        if(mDividerDecorator != null){
            Utils.safeRemoveItemDecoration(this, mDividerDecorator);
            mDividerDecorator = null;
        }

        Utils.safeRemoveItemDecoration(this, mSpacesDecorator);
        mSpacesDecorator = new SpacesItemDecoration(count,
            mGridSpacing, false);
        addItemDecoration(mSpacesDecorator);
    }

    public void updateSpanCount(int count) {
        if (mAdapter != null) {
            mAdapter.updateSpanCount(count);
        }
        updateLayoutManager(count);
        setRecycledViewPool(null); // Clear recycled view pool
        mSpanCount = count;
        Utils.safeNotifyDataSetChanged(getAdapter());
        invalidate();
    }

    public void update(int spanCount) {
        updateSpanCount(spanCount);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);

        if (adapter instanceof SimpleRecyclerViewAdapter) {
            mAdapter = (SimpleRecyclerViewAdapter) adapter;
        }
    }

    protected LinearLayoutManager getLinearLayoutManager() {
        return new LinearLayoutManager(getContext());
    }

    public void setSpanSizeLookup(GridLayoutManager.SpanSizeLookup spanSizeLookup) {
        mSpanSizeLookup = spanSizeLookup;
    }

    public void removeItemDecoration() {
        Utils.safeRemoveItemDecoration(this, mDividerDecorator);
    }


}
