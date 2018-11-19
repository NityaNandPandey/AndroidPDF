//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.asynctask;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.model.UserBookmarkItem;
import com.pdftron.pdf.utils.BookmarkManager;
import com.pdftron.pdf.utils.CustomAsyncTask;

import java.util.ArrayList;
import java.util.List;

public class PopulateUserBookmarkListTask extends CustomAsyncTask<Void, Void, Void> {

    private List<UserBookmarkItem> mBookmarkList;
    private boolean mModified;
    private String mFilePath;
    private Bookmark mBookmark;
    private boolean mReadOnly;

    private Callback mCallback;

    /**
     * Callback interface invoked when user bookmarks are populated.
     */
    public interface Callback {
        /**
         * Called when user bookmarks have been populated.
         *
         * @param result The populated user bookmarks
         * @param modified True if the PDF was modified
         */
        void getUserBookmarks(List<UserBookmarkItem> result, boolean modified);
    }

    /**
     * Class constructor
     *
     * @param context The context
     * @param filePath The file path
     * @param bookmark The {@link Bookmark}
     * @param readOnly Is the document read only
     */
    public PopulateUserBookmarkListTask(@NonNull Context context, String filePath, Bookmark bookmark, boolean readOnly) {
        super(context);
        mBookmark = bookmark;
        mFilePath = filePath;
        mReadOnly = readOnly;

        mBookmarkList = new ArrayList<>();
    }

    /**
     * Sets the callback listener.
     *
     * Sets the callback to null when the task is cancelled.
     *
     * @param callback The callback when the task is finished
     */
    public void setCallback(@Nullable Callback callback) {
        mCallback = callback;
    }

    /**
     * The overloaded implementation of {@link android.os.AsyncTask#doInBackground(Object[])}.
     **/
    @Override
    protected Void doInBackground(Void... params) {
        if (mReadOnly) {
            mBookmarkList.addAll(BookmarkManager.getUserBookmarks(getContext(), mFilePath));
            if (mBookmarkList.isEmpty()) {
                mBookmarkList.addAll(BookmarkManager.getPdfBookmarks(mBookmark));
            }
        } else {
            mBookmarkList.addAll(BookmarkManager.getPdfBookmarks(mBookmark));
            if (mBookmarkList.isEmpty()) {
                // Backwards compatibility
                // try to see if there were bookmarks from previous releases
                mBookmarkList.addAll(BookmarkManager.getUserBookmarks(getContext(), mFilePath));
                if (!mBookmarkList.isEmpty()) {
                    mModified = true;
                    // after porting, remove the old bookmarks
                    BookmarkManager.removeUserBookmarks(getContext(), mFilePath);
                }
            }
        }
        return null;
    }

    /**
     * The overloaded implementation of {@link android.os.AsyncTask#onPostExecute(Object)}.
     **/
    @Override
    protected void onPostExecute(Void aVoid) {
        if (mCallback != null) {
            mCallback.getUserBookmarks(mBookmarkList, mModified);
        }
    }
}
