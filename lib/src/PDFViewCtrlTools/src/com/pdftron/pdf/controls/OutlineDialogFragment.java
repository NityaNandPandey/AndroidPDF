//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.ActionParameter;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.ActionUtils;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.BookmarkManager;
import com.pdftron.pdf.utils.CommonToast;

import java.util.ArrayList;

/**
 * The OutlineDialogFragment shows a document outline (bookmarks) that can be
 * used to navigate the document in the PDFViewCtrl.
 */
public class OutlineDialogFragment extends NavigationListDialogFragment {

    private PDFViewCtrl mPdfViewCtrl;
    private ArrayList<Bookmark> mBookmarks;
    private OutlineAdapter mOutlineAdapter;
    private RelativeLayout mNavigation;
    private TextView mNavigationText;

    /**
     * Our current bookmark. It reflects the current top bookmark being shown,
     * i.e., the list of bookmarks in the dialog are its children. A null
     * mCurrentBookmark or an indentation equals to zero means we are on the
     * root of the bookmark tree.
     */
    Bookmark mCurrentBookmark;

    private OutlineDialogListener mOutlineDialogListener;

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface OutlineDialogListener {
        /**
         * Called when an outline has been clicked.
         *
         * @param parent   The parent bookmark if any
         * @param bookmark The clicked bookmark
         */
        void onOutlineClicked(Bookmark parent, Bookmark bookmark);
    }

    /**
     * Returns a new instance of the class
     */
    public static OutlineDialogFragment newInstance() {
        return new OutlineDialogFragment();
    }

    /**
     * Sets the {@link PDFViewCtrl}
     *
     * @param pdfViewCtrl The {@link PDFViewCtrl}
     * @return This class
     */
    public OutlineDialogFragment setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        return this;
    }

    /**
     * Sets the current bookmark.
     *
     * @param currentBookmark The current bookmark
     * @return This class
     */
    public OutlineDialogFragment setCurrentBookmark(@Nullable Bookmark currentBookmark) {
        mCurrentBookmark = currentBookmark;
        return this;
    }

    /**
     * Sets the OutlineDialogListener listener.
     *
     * @param listener The listener
     */
    public void setOutlineDialogListener(OutlineDialogListener listener) {
        mOutlineDialogListener = listener;
    }

    /**
     * The overload implementation of {@link DialogFragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.controls_fragment_outline_dialog, null);
        if (mPdfViewCtrl == null) {
            return view;
        }

        // Navigation
        mNavigation = view.findViewById(R.id.control_outline_layout_navigation);
        mNavigationText = mNavigation.findViewById(R.id.control_outline_layout_navigation_title);
        mNavigation.setVisibility(View.GONE);
        try {
            if (mCurrentBookmark != null) {
                if (mCurrentBookmark.getIndent() > 0) {
                    mNavigationText.setText(mCurrentBookmark.getTitle());
                    mNavigation.setVisibility(View.VISIBLE);
                } else { // the file has incorrect outline indent
                    mCurrentBookmark = null;
                }
            }
        } catch (PDFNetException e) {
            mCurrentBookmark = null;
        }
        mNavigation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToParentBookmark();
            }
        });

        mBookmarks = new ArrayList<>();
        if (mCurrentBookmark != null) {
            try {
                mBookmarks.addAll(BookmarkManager.getBookmarkList(mPdfViewCtrl.getDoc(), mCurrentBookmark.getFirstChild()));
            } catch (PDFNetException e) {
                mBookmarks.clear();
                mBookmarks.addAll(BookmarkManager.getBookmarkList(mPdfViewCtrl.getDoc(), null));
                mCurrentBookmark = null;
            }
        } else {
            mBookmarks.addAll(BookmarkManager.getBookmarkList(mPdfViewCtrl.getDoc(), null));
        }

        mOutlineAdapter = new OutlineAdapter(getActivity(), R.layout.controls_fragment_outline_listview_item, mBookmarks);
        ListView listViewBookmarks = view.findViewById(R.id.control_outline_listview);
        listViewBookmarks.setEmptyView(view.findViewById(R.id.control_outline_textview_empty));
        listViewBookmarks.setAdapter(mOutlineAdapter);
        listViewBookmarks.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    onEventAction();
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATE_BY,
                        AnalyticsParam.viewerNavigateByParam(AnalyticsHandlerAdapter.VIEWER_NAVIGATE_BY_OUTLINE));
                    Action action = mBookmarks.get(position).getAction();
                    if (action != null && action.isValid()) {
                        if (mPdfViewCtrl != null) {
                            boolean shouldUnlock = false;
                            boolean shouldUnlockRead = false;
                            boolean hasChanges = false;
                            try {
                                if (action.needsWriteLock()) {
                                    mPdfViewCtrl.docLock(true);
                                    shouldUnlock = true;
                                } else {
                                    mPdfViewCtrl.docLockRead();
                                    shouldUnlockRead = true;
                                }
                                ActionParameter action_param = new ActionParameter(action);
                                ActionUtils.getInstance().executeAction(action_param, mPdfViewCtrl);
                                hasChanges = mPdfViewCtrl.getDoc().hasChangesSinceSnapshot();
                            } catch (Exception e) {
                                AnalyticsHandlerAdapter.getInstance().sendException(e);
                            } finally {
                                if (shouldUnlock || shouldUnlockRead) {
                                    if (shouldUnlock) {
                                        mPdfViewCtrl.docUnlock();
                                    }
                                    if (shouldUnlockRead) {
                                        mPdfViewCtrl.docUnlockRead();
                                    }
                                    if (hasChanges) {
                                        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                                        toolManager.raiseAnnotationActionEvent();
                                    }
                                }
                            }
                        }
                        if (mOutlineDialogListener != null) {
                            mOutlineDialogListener.onOutlineClicked(mCurrentBookmark, mBookmarks.get(position));
                        }
                    }
                } catch (PDFNetException e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                    CommonToast.showText(getActivity(), "This bookmark has an invalid action", Toast.LENGTH_SHORT);
                }
            }
        });

        return view;
    }

    private void navigateToParentBookmark() {
        if (mPdfViewCtrl == null) {
            return;
        }

        ArrayList<Bookmark> temp;
        try {
            if (mCurrentBookmark != null && mCurrentBookmark.getIndent() > 0) {
                mCurrentBookmark = mCurrentBookmark.getParent();
                temp = BookmarkManager.getBookmarkList(mPdfViewCtrl.getDoc(), mCurrentBookmark.getFirstChild());
                mNavigationText.setText(mCurrentBookmark.getTitle());
                if (mCurrentBookmark.getIndent() <= 0) {
                    mNavigation.setVisibility(View.GONE);
                }
            } else {
                temp = BookmarkManager.getBookmarkList(mPdfViewCtrl.getDoc(), null);
                mCurrentBookmark = null;
                mNavigation.setVisibility(View.GONE);
            }
        } catch (PDFNetException e) {
            mCurrentBookmark = null;
            temp = null;
        }

        if (temp != null) {
            mBookmarks.clear();
            mBookmarks.addAll(temp);
            mOutlineAdapter.notifyDataSetChanged();
        }
    }

    private class OutlineAdapter extends ArrayAdapter<Bookmark> {

        private int mLayoutResourceId;
        private ArrayList<Bookmark> mBookmarks;

        private ViewHolder mViewHolder;

        OutlineAdapter(Context context, int resource, ArrayList<Bookmark> objects) {
            super(context, resource, objects);

            mLayoutResourceId = resource;
            mBookmarks = objects;
        }

        @Override
        public int getCount() {
            if (mBookmarks != null) {
                return mBookmarks.size();
            } else {
                return 0;
            }
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(mLayoutResourceId, parent, false);

                mViewHolder = new ViewHolder();
                mViewHolder.bookmarkText = convertView.findViewById(R.id.control_outline_listview_item_textview);
                mViewHolder.bookmarkArrow = convertView.findViewById(R.id.control_outline_listview_item_imageview);

                convertView.setTag(mViewHolder);

            } else {
                mViewHolder = (ViewHolder) convertView.getTag();
            }

            mViewHolder.bookmarkArrow.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCurrentBookmark = mBookmarks.get(position);
                    if (mCurrentBookmark == null) {
                        return;
                    }

                    ArrayList<Bookmark> temp = new ArrayList<>();
                    if (mPdfViewCtrl != null) {
                        try {
                            temp = BookmarkManager.getBookmarkList(mPdfViewCtrl.getDoc(), mCurrentBookmark.getFirstChild());
                        } catch (PDFNetException ignored) {

                        }
                    }
                    mBookmarks.clear();
                    mBookmarks.addAll(temp);
                    notifyDataSetChanged();
                    mNavigation.setVisibility(View.VISIBLE);
                    try {
                        mNavigationText.setText(mCurrentBookmark.getTitle());
                    } catch (PDFNetException e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    }
                }
            });

            Bookmark bookmark = mBookmarks.get(position);
            try {
                mViewHolder.bookmarkText.setText(bookmark.getTitle());
                if (bookmark.hasChildren()) {
                    mViewHolder.bookmarkArrow.setVisibility(View.VISIBLE);
                } else {
                    mViewHolder.bookmarkArrow.setVisibility(View.GONE);
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }

            return convertView;
        }

        private class ViewHolder {
            private TextView bookmarkText;
            ImageView bookmarkArrow;
        }
    }
}
