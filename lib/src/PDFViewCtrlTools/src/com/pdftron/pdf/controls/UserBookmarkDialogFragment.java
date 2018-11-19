//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.asynctask.PopulateUserBookmarkListTask;
import com.pdftron.pdf.model.UserBookmarkItem;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.BookmarkManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerViewAdapter;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;
import com.pdftron.pdf.widget.recyclerview.decoration.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;
import co.paulburke.android.itemtouchhelperdemo.helper.SimpleItemTouchHelperCallback;

/**
 * The UserBookmarkDialogFragment shows a list of user-defined bookmarks that can be used to navigate
 * the document in the {@link com.pdftron.pdf.PDFViewCtrl}. This is different from {@link com.pdftron.pdf.controls.OutlineDialogFragment}
 * as user can add new custom bookmarks with any name at any time.
 * Modification to existing bookmarks is also supported.
 */
public class UserBookmarkDialogFragment extends NavigationListDialogFragment {

    @SuppressWarnings("unused")
    private static final String TAG = UserBookmarkDialogFragment.class.getName();

    /**
     * Bundle key to set the file path
     */
    public static final String BUNDLE_FILE_PATH = "file_path";

    /**
     * Bundle key to specify whether the document is read only or not
     */
    public static final String BUNDLE_IS_READ_ONLY = "is_read_only";

    private static final int CONTEXT_MENU_EDIT_ITEM = 0;
    private static final int CONTEXT_MENU_DELETE_ITEM = 1;
    private static final int CONTEXT_MENU_DELETE_ALL = 2;

    private PopulateUserBookmarkListTask mTask;

    private ArrayList<UserBookmarkItem> mSource = new ArrayList<>();
    private UserBookmarksAdapter mUserBookmarksAdapter;

    private SimpleRecyclerView mRecyclerView;
    private ItemTouchHelper mItemTouchHelper;

    private PDFViewCtrl mPdfViewCtrl;
    private boolean mReadOnly;
    private String mFilePath;

    private boolean mModified;
    private boolean mRebuild;
    private boolean mIsDragging;

    private UserBookmarkDialogListener mUserBookmarkDialogListener;

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface UserBookmarkDialogListener {
        /**
         * Called when a user bookmark has been clicked.
         *
         * @param pageNum The page number
         */
        void onUserBookmarkClicked(int pageNum);
    }

    /**
     * Returns a new instance of the class
     */
    public static UserBookmarkDialogFragment newInstance() {
        return new UserBookmarkDialogFragment();
    }

    /**
     * Sets the {@link PDFViewCtrl}
     *
     * @param pdfViewCtrl The PDFViewCtrl
     * @return This class
     */
    public UserBookmarkDialogFragment setPdfViewCtrl(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        return this;
    }

    /**
     * Sets the file path. If not specified it is extracted from PDFViewCtrl.
     *
     * @param filePath The file path
     * @return This class
     */
    public UserBookmarkDialogFragment setFilePath(String filePath) {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putString(BUNDLE_FILE_PATH, filePath);
        setArguments(args);

        return this;
    }

    /**
     * Sets if the document is read only
     *
     * @param isReadOnly True if the document is read only
     * @return This class
     */
    @SuppressWarnings("unused")
    public UserBookmarkDialogFragment setReadOnly(boolean isReadOnly) {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putBoolean(BUNDLE_IS_READ_ONLY, isReadOnly);
        setArguments(args);

        return this;
    }

    /**
     * The overload implementation of {@link DialogFragment#onCreate(Bundle)}.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mReadOnly = args.getBoolean(BUNDLE_IS_READ_ONLY, false);
            mFilePath = args.getString(BUNDLE_FILE_PATH, null);
        }
    }

    /**
     * The overload implementation of {@link DialogFragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.controls_fragment_bookmark_dialog, null);

        mUserBookmarksAdapter = new UserBookmarksAdapter(getActivity(), mSource, null);
        mRecyclerView = view.findViewById(R.id.controls_bookmark_recycler_view);
        //mRecyclerView.setEmptyView(view.findViewById(R.id.control_bookmark_textview_empty));

        mRecyclerView.setAdapter(mUserBookmarksAdapter);

        FloatingActionButton fab = view.findViewById(R.id.control_bookmark_add);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getContext();
                if (context == null || mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
                    return;
                }
                try {
                    int curPageNum = mPdfViewCtrl.getCurrentPage();
                    long curObjNum = mPdfViewCtrl.getDoc().getPage(curPageNum).getSDFObj().getObjNum();
                    UserBookmarkItem item = new UserBookmarkItem(context, curObjNum, curPageNum);
                    mUserBookmarksAdapter.add(item);
                    mModified = true;
                    Utils.safeNotifyDataSetChanged(mUserBookmarksAdapter);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
                onEventAction();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_USER_BOOKMARKS,
                    AnalyticsParam.userBookmarksActionParam(AnalyticsHandlerAdapter.USER_BOOKMARKS_ADD));
            }
        });

        DividerItemDecoration mDivDecorator = new DividerItemDecoration(getContext());
        mRecyclerView.addItemDecoration(mDivDecorator);

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);

        ItemTouchHelper.Callback touchCallback = new BookmarkItemTouchHelperCallback(mUserBookmarksAdapter, 1, !mReadOnly, false);
        mItemTouchHelper = new ItemTouchHelper(touchCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView recyclerView, View view, int position, long id) {
                if (mUserBookmarkDialogListener != null) {
                    UserBookmarkItem item = mUserBookmarksAdapter.getItem(position);
                    if (item == null) {
                        return;
                    }
                    mUserBookmarkDialogListener.onUserBookmarkClicked(item.pageNumber);
                    onEventAction();
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATE_BY,
                        AnalyticsParam.viewerNavigateByParam(AnalyticsHandlerAdapter.VIEWER_NAVIGATE_BY_USER_BOOKMARK));
                }
            }
        });

        itemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView recyclerView, View v, final int position, final long id) {
                if (mReadOnly) {
                    return true;
                }
                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        mIsDragging = true;
                        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
                        mItemTouchHelper.startDrag(holder);
                    }
                });

                return true;
            }
        });

        return view;
    }

    /**
     * The overload implementation of {@link DialogFragment#onResume()}.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadBookmarks();
    }

    /**
     * The overload implementation of {@link DialogFragment#onPause()}.
     */
    @Override
    public void onPause() {
        commitData(getActivity());

        super.onPause();
    }

    /**
     * Sets the listener to {@link UserBookmarkDialogFragment}
     *
     * @param listener The listener
     */
    public void setUserBookmarkListener(UserBookmarkDialogListener listener) {
        mUserBookmarkDialogListener = listener;
    }

    private void onShowPopupMenu(final int position, View anchor) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), anchor);
        Menu menu = popupMenu.getMenu();

        String[] menuItems = getResources().getStringArray(R.array.user_bookmark_dialog_context_menu);
        menu.add(Menu.NONE, CONTEXT_MENU_EDIT_ITEM, CONTEXT_MENU_EDIT_ITEM, menuItems[CONTEXT_MENU_EDIT_ITEM]);
        menu.add(Menu.NONE, CONTEXT_MENU_DELETE_ITEM, CONTEXT_MENU_DELETE_ITEM, menuItems[CONTEXT_MENU_DELETE_ITEM]);
        menu.add(Menu.NONE, CONTEXT_MENU_DELETE_ALL, CONTEXT_MENU_DELETE_ALL, menuItems[CONTEXT_MENU_DELETE_ALL]);
        MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onPopupItemSelected(item, position);
                return true;
            }
        };
        menu.getItem(CONTEXT_MENU_EDIT_ITEM).setOnMenuItemClickListener(listener);
        menu.getItem(CONTEXT_MENU_DELETE_ITEM).setOnMenuItemClickListener(listener);
        menu.getItem(CONTEXT_MENU_DELETE_ALL).setOnMenuItemClickListener(listener);
        popupMenu.show();
    }

    private void onPopupItemSelected(MenuItem item, int position) {
        if (mPdfViewCtrl == null) {
            return;
        }

        int menuItemIndex = item.getItemId();
        switch (menuItemIndex) {
            case CONTEXT_MENU_EDIT_ITEM:
                mModified = true;
                mUserBookmarksAdapter.setEditMode(true);
                mUserBookmarksAdapter.setSelectedIndex(position);
                Utils.safeNotifyDataSetChanged(mUserBookmarksAdapter);
                onEventAction();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_USER_BOOKMARKS,
                    AnalyticsParam.userBookmarksActionParam(AnalyticsHandlerAdapter.USER_BOOKMARKS_RENAME));
                break;
            case CONTEXT_MENU_DELETE_ITEM:
                UserBookmarkItem userBookmarkItem = mUserBookmarksAdapter.getItem(position);
                if (userBookmarkItem == null) {
                    return;
                }
                mModified = true;
                if (!mReadOnly) {
                    PDFDoc pdfDoc = mPdfViewCtrl.getDoc();
                    if (pdfDoc != null) {
                        boolean hasChange = false;
                        boolean shouldUnlock = false;
                        try {
                            pdfDoc.lock();
                            shouldUnlock = true;
                            if (userBookmarkItem.pdfBookmark != null) {
                                userBookmarkItem.pdfBookmark.delete();
                            }
                            hasChange = pdfDoc.hasChangesSinceSnapshot();
                        } catch (Exception e) {
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        } finally {
                            if (shouldUnlock) {
                                Utils.unlockQuietly(pdfDoc);
                            }
                        }
                        if (hasChange) {
                            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                            if (toolManager != null) {
                                toolManager.raiseBookmarkModified();
                            }
                        }
                    }
                }
                mUserBookmarksAdapter.remove(userBookmarkItem);
                Utils.safeNotifyDataSetChanged(mUserBookmarksAdapter);

                onEventAction();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_USER_BOOKMARKS,
                    AnalyticsParam.userBookmarksActionParam(AnalyticsHandlerAdapter.USER_BOOKMARKS_DELETE));
                break;
            case CONTEXT_MENU_DELETE_ALL:
                mModified = true;

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.controls_bookmark_dialog_delete_all_message)
                    .setTitle(R.string.controls_misc_delete_all)
                    .setPositiveButton(R.string.tools_misc_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!mReadOnly) {
                                BookmarkManager.removeRootPdfBookmark(mPdfViewCtrl, true);
                            }
                            mUserBookmarksAdapter.clear();
                            Utils.safeNotifyDataSetChanged(mUserBookmarksAdapter);

                            onEventAction();
                            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_USER_BOOKMARKS,
                                AnalyticsParam.userBookmarksActionParam(AnalyticsHandlerAdapter.USER_BOOKMARKS_DELETE_ALL));
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .create()
                    .show();
                break;
        }
    }

    private void loadBookmarks() {
        if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
            return;
        }

        if (mTask != null && mTask.getStatus() == AsyncTask.Status.RUNNING) {
            return;
        }

        try {
            Bookmark rootBookmark = BookmarkManager.getRootPdfBookmark(mPdfViewCtrl.getDoc(), false);
            if (Utils.isNullOrEmpty(mFilePath)) {
                mFilePath = mPdfViewCtrl.getDoc().getFileName();
            }
            mTask = new PopulateUserBookmarkListTask(getContext(), mFilePath, rootBookmark, mReadOnly);
            mTask.setCallback(new PopulateUserBookmarkListTask.Callback() {
                @Override
                public void getUserBookmarks(List<UserBookmarkItem> bookmarks, boolean modified) {
                    mModified = modified;
                    mUserBookmarksAdapter.clear();
                    mUserBookmarksAdapter.addAll(bookmarks);
                    Utils.safeNotifyDataSetChanged(mUserBookmarksAdapter);
                }
            });
            mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void commitData(Context context) {
        if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
            return;
        }

        if (!mModified) {
            return;
        }
        if (mReadOnly) {
            try {
                if (Utils.isNullOrEmpty(mFilePath)) {
                    mFilePath = mPdfViewCtrl.getDoc().getFileName();
                }
                BookmarkManager.saveUserBookmarks(context, mFilePath, mSource);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        } else {
            BookmarkManager.savePdfBookmarks(mPdfViewCtrl, mSource, true, mRebuild);
            mRebuild = false;
        }
    }

    private class UserBookmarksAdapter extends SimpleRecyclerViewAdapter<UserBookmarkItem, UserBookmarksAdapter.PageViewHolder>
        implements ItemTouchHelperAdapter {

        private ArrayList<UserBookmarkItem> mBookmarks;
        private Context mContext;
        private boolean mEditMode;
        private int mSelectedIndex = -1;

        UserBookmarksAdapter(Context context, ArrayList<UserBookmarkItem> bookmarks, ViewHolderBindListener bindListener) {
            super(bindListener);

            mContext = context;
            mBookmarks = bookmarks;
            mEditMode = false;
        }

        public void clear() {
            mBookmarks.clear();
        }

        public void addAll(List<UserBookmarkItem> listBookmarks) {
            mBookmarks.addAll(listBookmarks);
        }

        void setEditMode(boolean editMode) {
            mEditMode = editMode;
        }

        void setSelectedIndex(int index) {
            mSelectedIndex = index;
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            UserBookmarkItem oldItem = mBookmarks.get(fromPosition);
            UserBookmarkItem item = new UserBookmarkItem();
            item.pageObjNum = oldItem.pageObjNum;
            item.pageNumber = oldItem.pageNumber;
            item.title = oldItem.title;

            for (UserBookmarkItem uitem : mBookmarks) {
                uitem.pdfBookmark = null;
            }
            mRebuild = true;

            mBookmarks.remove(fromPosition);
            mBookmarks.add(toPosition, item);

            notifyItemMoved(fromPosition, toPosition);
            mModified = true;

            return true;
        }

        @Override
        public void onItemDrop(int fromPosition, int toPosition) {

        }

        @Override
        public void onItemDismiss(int position) {

        }

        @Override
        public UserBookmarkItem getItem(int position) {
            if (mBookmarks != null && position >= 0 && position < mBookmarks.size()) {
                return mBookmarks.get(position);
            }
            return null;
        }

        @Override
        public void add(UserBookmarkItem item) {
            mBookmarks.add(item);
        }

        @Override
        public boolean remove(UserBookmarkItem item) {
            if (mBookmarks.contains(item)) {
                mBookmarks.remove(item);
                return true;
            }
            return false;
        }

        @Override
        public UserBookmarkItem removeAt(int location) {
            if (location < mBookmarks.size()) {
                return mBookmarks.remove(location);
            }
            return null;
        }

        @Override
        public void insert(UserBookmarkItem item, int position) {
            mBookmarks.add(position, item);
        }

        @Override
        public void updateSpanCount(int count) {

        }

        @Override
        public UserBookmarksAdapter.PageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.controls_fragment_bookmark_listview_item, parent, false);
            return new PageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final UserBookmarksAdapter.PageViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            UserBookmarkItem item = mBookmarks.get(position);

            holder.itemView.getBackground().setColorFilter(null);
            holder.itemView.getBackground().invalidateSelf();
            holder.bookmarkTextView.setText(item.title);

            if (mEditMode) {
                holder.itemView.setFocusableInTouchMode(true);
                if (position == mSelectedIndex) {
                    holder.bookmarkTextView.setVisibility(View.GONE);
                    holder.bookmarkEditText.setVisibility(View.VISIBLE);
                    holder.bookmarkEditText.setText(item.title);
                    holder.bookmarkEditText.requestFocus();
                    holder.bookmarkEditText.selectAll();
                    showSoftInput();

                    // commit changes
                    holder.bookmarkEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            int pos = holder.getAdapterPosition();
                            if (pos == RecyclerView.NO_POSITION) {
                                return false;
                            }
                            if (actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                                v.clearFocus();
                                return true;
                            }
                            return false;
                        }
                    });

                    holder.bookmarkEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if (hasFocus || holder == null) {
                                return;
                            }
                            int pos = holder.getAdapterPosition();
                            if (pos == RecyclerView.NO_POSITION) {
                                return;
                            }
                            hideSoftInput(v);

                            saveEditTextChanges((TextView) v, pos);
                        }
                    });
                }
            } else {
                holder.bookmarkEditText.clearFocus();
                holder.itemView.setFocusableInTouchMode(false);
                holder.bookmarkTextView.setVisibility(View.VISIBLE);
                holder.bookmarkEditText.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return mBookmarks.size();
        }

        private void saveEditTextChanges(TextView v, int position) {
            v.clearFocus();

            setEditMode(false);
            String title = v.getText().toString();
            if (title.isEmpty()) {
                title = mContext.getString(R.string.empty_title);
            }
            UserBookmarkItem userBookmarkItem = mUserBookmarksAdapter.getItem(position);
            if (userBookmarkItem == null) {
                return;
            }
            userBookmarkItem.title = title;
            userBookmarkItem.isBookmarkEdited = true;
            Utils.safeNotifyDataSetChanged(this);
        }

        private void hideSoftInput(View view) {
            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }

        private void showSoftInput() {
            InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        }

        class PageViewHolder extends RecyclerView.ViewHolder {

            TextView bookmarkTextView;
            EditText bookmarkEditText;
            ImageButton contextButton;

            PageViewHolder(final View itemView) {
                super(itemView);
                this.bookmarkTextView = itemView.findViewById(R.id.control_bookmark_listview_item_textview);
                this.bookmarkEditText = itemView.findViewById(R.id.control_bookmark_listview_item_edittext);
                this.contextButton = itemView.findViewById(R.id.control_bookmark_listview_item_context_button);
                this.contextButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = getAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) {
                            return;
                        }
                        if (!mEditMode) {
                            UserBookmarkDialogFragment.this.onShowPopupMenu(pos, v);
                        } else {
                            itemView.requestFocus();
                        }
                    }
                });
            }
        }
    }

    private class BookmarkItemTouchHelperCallback extends SimpleItemTouchHelperCallback {

        BookmarkItemTouchHelperCallback(ItemTouchHelperAdapter adapter, int span, boolean enableLongPress, boolean enableSwipe) {
            super(adapter, span, enableLongPress, enableSwipe);
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            // Set movement flags based on the layout manager
            if (viewHolder.getItemViewType() == VIEW_TYPE_HEADER) {
                return makeMovementFlags(0, 0);
            }
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            final int swipeFlags = 0;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            if (mIsDragging) {
                viewHolder.itemView.getBackground().mutate().setColorFilter(getResources().getColor(R.color.gray), PorterDuff.Mode.MULTIPLY);
                viewHolder.itemView.getBackground().invalidateSelf();
            }
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            if (mIsDragging) {
                viewHolder.itemView.getBackground().setColorFilter(null);
                viewHolder.itemView.getBackground().invalidateSelf();
                mIsDragging = false;
            }
        }
    }
}
