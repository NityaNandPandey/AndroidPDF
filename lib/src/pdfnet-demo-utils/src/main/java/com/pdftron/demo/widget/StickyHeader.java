package com.pdftron.demo.widget;
//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.pdftron.demo.R;
import com.pdftron.demo.R2;
import com.pdftron.demo.model.FileHeader;
import com.pdftron.demo.navigation.adapter.BaseFileAdapter;
import com.pdftron.demo.navigation.adapter.viewholder.HeaderViewHolder;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class StickyHeader extends FrameLayout implements View.OnClickListener {
    private static final String TAG = StickyHeader.class.getSimpleName();
    private final ScrollListener scrollListener = new ScrollListener();
    @BindView(R2.id.header_view)
    View header;
    @BindView(R2.id.title)
    TextView title;
    @BindView(R2.id.folding_btn)
    AppCompatImageView foldingBtn;
    private RecyclerView mRecyclerView;
    private BaseFileAdapter mAdapter;
    private View view;
    private int headerPos = 0;
    private boolean isDisabled = false;


    public StickyHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public StickyHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);

    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.sticky_header, this);
        ButterKnife.bind(this, view);
        mRecyclerView = null;
        mAdapter = null;
        header.setOnClickListener(this);
        header.setBackgroundColor(getContext().getResources().getColor(R.color.recyclerview_header_bg));
        if (Utils.isLollipop()) {
            header.setElevation(3);
        }
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mAdapter = (BaseFileAdapter) mRecyclerView.getAdapter();

        mRecyclerView.addOnScrollListener(scrollListener);
    }

    public void disable() {
        isDisabled = true;
        header.setVisibility(View.GONE);
    }

    public void enable(int position) {
        isDisabled = false;
        if (locateHeaderPos(position)) {
            updateStickyHeader();
        }
    }

    public boolean isDisabled(){
        return isDisabled;
    }

    private boolean locateHeaderPos(int firstChildPos) {
        int viewType = mAdapter.getItemViewType(firstChildPos);
        switch (viewType) {
            case BaseFileAdapter.VIEW_TYPE_HEADER:
                headerPos = firstChildPos;
                break;
            case BaseFileAdapter.VIEW_TYPE_CONTENT:
                headerPos = mAdapter.findHeader(firstChildPos);
                break;
        }
        return true;
    }

    private void updateStickyHeader() {
        if (mAdapter == null || headerPos < 0) {
            return;
        }
        if (!(mAdapter.getItem(headerPos) instanceof FileHeader)) {
            return;
        }
        FileHeader fileHeader = (FileHeader) mAdapter.getItem(headerPos);
        if (fileHeader == null) {
            return;
        }
        title.setText(fileHeader.getAbsolutePath());
        View firstChild = mRecyclerView.getChildAt(0);
        int firstChildAdapterPos = mRecyclerView.getChildAdapterPosition(firstChild);
        if (fileHeader.getCollapsed() || (firstChildAdapterPos == headerPos && firstChild.getBottom() == header.getLayoutParams().height)) {
            header.setVisibility(View.GONE);
        } else {
            View nextChild = mRecyclerView.getChildAt(1);
            header.setVisibility(View.VISIBLE);
            if (nextChild != null){
                RecyclerView.ViewHolder vh = mRecyclerView.getChildViewHolder(nextChild);
                if (vh instanceof HeaderViewHolder && firstChild.getBottom() < header.getLayoutParams().height){
                    header.setTranslationY(firstChild.getBottom() - header.getLayoutParams().height);
                }
                else {
                    header.setTranslationY(0);
                }
            }else {
                header.setTranslationY(0);
            }
            foldingBtn.setImageResource(R.drawable.ic_keyboard_arrow_down_black_24dp);
        }
    }

    @Override
    public void onClick(View v) {
        if (isDisabled) {
            return;
        }
        if (mAdapter != null) {
            mRecyclerView.scrollToPosition(headerPos);
            mAdapter.clickHeader(headerPos);
            updateStickyHeader();
        }
    }

    private class ScrollListener extends RecyclerView.OnScrollListener {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (mAdapter == null || mRecyclerView == null || isDisabled) {
                header.setVisibility(View.GONE);
                return;
            }
            View firstChild = mRecyclerView.getChildAt(0);
            if (firstChild == null) {
                header.setVisibility(View.GONE);
                return;
            }
            int firstChildPos = mRecyclerView.getChildAdapterPosition(firstChild);
            if (firstChildPos == RecyclerView.NO_POSITION) {
                header.setVisibility(View.GONE);
                return;
            }
            int viewType = mAdapter.getItemViewType(firstChildPos);
            header.setVisibility(View.VISIBLE);
            switch (viewType) {
                case BaseFileAdapter.VIEW_TYPE_HEADER:
                    FileHeader fileHeader = (FileHeader) mAdapter.getItem(firstChildPos);
                    if (fileHeader == null) {
                        return;
                    }

                    if (fileHeader.getCollapsed()) {
                        header.setVisibility(View.GONE);
                        return;
                    }
                    HeaderViewHolder headerViewHolder = (HeaderViewHolder) mRecyclerView.getChildViewHolder(firstChild);
                    title.setText(headerViewHolder.textViewTitle.getText());
                    foldingBtn.setImageDrawable(headerViewHolder.foldingBtn.getDrawable());
                    header.setTranslationY(0);
                    if (firstChild.getBottom() == header.getLayoutParams().height){
                        header.setVisibility(View.GONE);
                    }
                    headerPos = firstChildPos;
                    break;
                case BaseFileAdapter.VIEW_TYPE_CONTENT:
                    if (firstChildPos == mAdapter.getItemCount() - 1) {
                        return;
                    }
                    FileInfo contentInfo = (FileInfo) mAdapter.getItem(firstChildPos);
                    if (contentInfo == null) {
                        return;
                    }

                    int nextChildType = mAdapter.getItemViewType(firstChildPos + 1);

                    if (nextChildType == BaseFileAdapter.VIEW_TYPE_HEADER) {    // last content
                        if (dy >= 0) { // scrolling up
                            if (dy == 0 && !title.getText().equals(contentInfo.getParentDirectoryPath())) {
                                title.setText(contentInfo.getParentDirectoryPath());
                                foldingBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_down_white_24dp));
                                foldingBtn.getDrawable().mutate().setColorFilter(getResources().getColor(android.R.color.black), PorterDuff.Mode.SRC_IN);
                                headerPos = mAdapter.findHeader(firstChildPos);
                            }
                            if (firstChild.getBottom() <= header.getLayoutParams().height) {
                                header.setTranslationY(firstChild.getBottom() - header.getLayoutParams().height);
                            }
                            else {
                                header.setTranslationY(0);
                            }
                        } else {   // scrolling down
                            int startingPoint = -header.getLayoutParams().height;
                            if (!title.getText().equals(contentInfo.getParentDirectoryPath())) {
                                header.setTranslationY(startingPoint + header.getTranslationY());
                                title.setText(contentInfo.getParentDirectoryPath());
                                foldingBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_down_white_24dp));
                                foldingBtn.getDrawable().mutate().setColorFilter(getResources().getColor(android.R.color.black), PorterDuff.Mode.SRC_IN);
                                headerPos = mAdapter.findHeader(firstChildPos);
                            }

                            if (header.getTranslationY() < startingPoint) {
                                header.setTranslationY(startingPoint);
                            } else if (header.getTranslationY() < 0) {
                                header.setTranslationY(header.getTranslationY() - dy);
                            }
                        }
                    } else {
                        if (!title.getText().equals(contentInfo.getParentDirectoryPath())) {
                            title.setText(contentInfo.getParentDirectoryPath());
                            foldingBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_arrow_down_white_24dp));
                            foldingBtn.setColorFilter(getResources().getColor(android.R.color.black), PorterDuff.Mode.SRC_IN);
                            headerPos = mAdapter.findHeader(firstChildPos);
                        }
                        if(header.getTranslationY() != 0){
                            header.setTranslationY(0);
                        }
                    }

                    if (header.getTranslationY() > 0) {
                        header.setTranslationY(0);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
