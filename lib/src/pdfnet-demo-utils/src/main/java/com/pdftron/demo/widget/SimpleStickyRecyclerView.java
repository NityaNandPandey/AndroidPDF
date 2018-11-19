package com.pdftron.demo.widget;

import android.content.Context;
import android.util.AttributeSet;

import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;

public class SimpleStickyRecyclerView extends SimpleRecyclerView {

    private StickyHeader mStickyHeader = null;

    public SimpleStickyRecyclerView(Context context) {
        super(context);
    }

    public SimpleStickyRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SimpleStickyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setStickyHeader(StickyHeader stickyHeader) {
        mStickyHeader = stickyHeader;
        mStickyHeader.setRecyclerView(this);
    }

    @Override
    protected android.support.v7.widget.LinearLayoutManager getLinearLayoutManager() {
        return new LinearLayoutManager(getContext());
    }

    public class LinearLayoutManager extends android.support.v7.widget.LinearLayoutManager {

        public LinearLayoutManager(Context context) {
            super(context);
        }

        @Override
        public void onLayoutCompleted(State state) {
            super.onLayoutCompleted(state);

            if (findFirstVisibleItemPosition() >= 0
                && findLastVisibleItemPosition() > findFirstVisibleItemPosition()
                && mStickyHeader != null && mStickyHeader.isDisabled()) {
                mStickyHeader.enable(findFirstVisibleItemPosition());
            }
        }
    }

}
