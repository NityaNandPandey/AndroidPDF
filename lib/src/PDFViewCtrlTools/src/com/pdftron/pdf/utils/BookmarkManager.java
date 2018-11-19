//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.Destination;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.UserBookmarkItem;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.sdf.Obj;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for handling bookmarks in PDF
 */
public class BookmarkManager {

    private static final String TAG = BookmarkManager.class.getName();

    private static final String PREFS_CONTROLS_FILE_NAME = "com_pdftron_pdfnet_pdfviewctrl_controls_prefs_file";
    private static final String KEY_PREF_USER_BOOKMARK = "user_bookmarks_key";
    private static final String KEY_PREF_USER_BOOKMARK_OBJ_TITLE = "pdftronUserBookmarks";

    // START Bookmark using Bookmark object

    /**
     * Returns the root PDF bookmark
     *
     * @param pdfDoc The PDFDoc
     * @param createNew True if should create new bookmark object if doc doesn't have any
     *
     * @return The root PDF bookmark
     */
    public static Bookmark getRootPdfBookmark(PDFDoc pdfDoc, boolean createNew) {
        Bookmark bookmark = null;
        if (null != pdfDoc) {
            boolean shouldUnlockRead = false;
            try {
                pdfDoc.lockRead();
                shouldUnlockRead = true;
                Obj catalog = pdfDoc.getRoot();
                Obj bookmark_obj = catalog.findObj(KEY_PREF_USER_BOOKMARK_OBJ_TITLE);
                if (null != bookmark_obj) {
                    // found existing bookmark obj
                    bookmark = new Bookmark(bookmark_obj);
                } else {
                    if (createNew) {
                        // create new bookmark obj
                        bookmark = Bookmark.create(pdfDoc, KEY_PREF_USER_BOOKMARK_OBJ_TITLE);
                        pdfDoc.getRoot().put(KEY_PREF_USER_BOOKMARK_OBJ_TITLE, bookmark.getSDFObj());
                    }
                }
            } catch (PDFNetException e) {
                bookmark = null;
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(pdfDoc);
                }
            }
        }
        return bookmark;
    }

    /**
     * Returns pdf bookmarks.
     *
     * @param rootBookmark The root PDF bookmark
     *
     * @return A list of user bookmarks
     */
    public static List<UserBookmarkItem> getPdfBookmarks(Bookmark rootBookmark) {
        ArrayList<UserBookmarkItem> data = new ArrayList<>();
        if (null != rootBookmark) {
            try {
                if (rootBookmark.hasChildren()) {
                    Bookmark item = rootBookmark.getFirstChild();
                    for (; item.isValid(); item = item.getNext()) {
                        UserBookmarkItem bookmarkItem = new UserBookmarkItem();
                        bookmarkItem.isBookmarkEdited = false;
                        bookmarkItem.pdfBookmark = item;
                        bookmarkItem.title = item.getTitle();
                        Action action = item.getAction();
                        if (null != action && action.isValid()) {
                            if (action.getType() == Action.e_GoTo) {
                                Destination dest = action.getDest();
                                if (null != dest && dest.isValid()) {
                                    bookmarkItem.pageNumber = dest.getPage().getIndex();
                                    bookmarkItem.pageObjNum = dest.getPage().getSDFObj().getObjNum();
                                    data.add(bookmarkItem);
                                }
                            }
                        }
                    }
                }
            } catch (PDFNetException ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
                Log.e("PDFNet", ex.getMessage());
            }
        }
        return data;
    }

    /**
     * Saves PDF bookmarks.
     *
     * @param pdfViewCtrl the PDFViewCtrl
     * @param data A list of user bookmarks
     * @param shouldTakeUndoSnapshot True if should take undo snapshot
     * @param rebuild True if should rebuild the root bookmark
     */
    public static void savePdfBookmarks(PDFViewCtrl pdfViewCtrl, List<UserBookmarkItem> data, boolean shouldTakeUndoSnapshot, boolean rebuild) {
        if (pdfViewCtrl == null) {
            return;
        }

        final PDFDoc pdfDoc = pdfViewCtrl.getDoc();
        if (pdfDoc == null) {
            return;
        }

        if (data.size() > 0) {
            Bookmark rootBookmark = getRootPdfBookmark(pdfDoc, true);
            Bookmark firstBookmark = null;
            Bookmark currentBookmark = null;

            if (null != rootBookmark) {
                boolean hasChange = false;
                boolean shouldUnlock = false;
                try {
                    if (rootBookmark.hasChildren()) {
                        firstBookmark = rootBookmark.getFirstChild();
                    }
                    pdfDoc.lock();
                    shouldUnlock = true;

                    if (rebuild) {
                        Obj catalog = pdfDoc.getRoot();
                        if (catalog != null) {
                            catalog.erase(KEY_PREF_USER_BOOKMARK_OBJ_TITLE);
                        }
                        rootBookmark = getRootPdfBookmark(pdfDoc, true);
                        firstBookmark = null;
                    }
                    for (UserBookmarkItem item : data) {
                        if (null == item.pdfBookmark) {
                            if (null == currentBookmark) {
                                // No items in the list above this on are currently in the document
                                if (null == firstBookmark) {
                                    // this means there are no bookmarks at all, so create one.
                                    currentBookmark = rootBookmark.addChild(item.title);
                                    currentBookmark.setAction(Action.createGoto(Destination.createFit(pdfDoc.getPage(item.pageNumber))));
                                    firstBookmark = currentBookmark;
                                } else {
                                    // there already are bookmarks, so the new bookmark needs to be inserted in front of the first one
                                    currentBookmark = firstBookmark.addPrev(item.title);
                                    currentBookmark.setAction(Action.createGoto(Destination.createFit(pdfDoc.getPage(item.pageNumber))));
                                    firstBookmark = currentBookmark;
                                }
                            } else {
                                // at least one item in the list above the current item was in the list
                                currentBookmark = currentBookmark.addNext(item.title);
                                currentBookmark.setAction(Action.createGoto(Destination.createFit(pdfDoc.getPage(item.pageNumber))));
                            }
                            item.pdfBookmark = currentBookmark;
                        } else {
                            currentBookmark = item.pdfBookmark;
                            if (item.isBookmarkEdited) {
                                Action action = item.pdfBookmark.getAction();
                                Destination dest = action.getDest();
                                dest.setPage(pdfDoc.getPage(item.pageNumber));
                                item.pdfBookmark.setTitle(item.title);
                            }
                        }
                    }
                    hasChange = pdfDoc.hasChangesSinceSnapshot();
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                    Log.e("PDFNet", ex.getMessage());
                } finally {
                    if (shouldUnlock) {
                        Utils.unlockQuietly(pdfDoc);
                    }
                }

                if (shouldTakeUndoSnapshot && hasChange) {
                    ToolManager toolManager = (ToolManager) pdfViewCtrl.getToolManager();
                    if (toolManager != null) {
                        toolManager.raiseBookmarkModified();
                    }
                }
            }
        } else {
            removeRootPdfBookmark(pdfViewCtrl, shouldTakeUndoSnapshot);
        }
    }

    /**
     * Removes the root PDF bookmark.
     *
     * @param pdfViewCtrl The PDFViewCtrl
     * @param shouldTakeUndoSnapshot True if should take undo snapshot
     *
     * @return True if the root PDF bookmark is removed.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean removeRootPdfBookmark(PDFViewCtrl pdfViewCtrl, boolean shouldTakeUndoSnapshot) {
        if (pdfViewCtrl == null) {
            return false;
        }

        final PDFDoc pdfDoc = pdfViewCtrl.getDoc();
        if (pdfDoc == null) {
            return false;
        }

        boolean hasChange;
        boolean shouldUnlock = false;
        try {
            pdfDoc.lock();
            shouldUnlock = true;
            Obj catalog = pdfDoc.getRoot();
            if (catalog != null) {
                catalog.erase(KEY_PREF_USER_BOOKMARK_OBJ_TITLE);
            }
            hasChange = pdfDoc.hasChangesSinceSnapshot();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return false;
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(pdfDoc);
            }
        }

        if (shouldTakeUndoSnapshot && hasChange) {
            ToolManager toolManager = (ToolManager) pdfViewCtrl.getToolManager();
            if (toolManager != null) {
                toolManager.raiseBookmarkModified();
            }
        }

        return true;
    }

    /**
     * Adds a PDF bookmark.
     *
     * @param context The context
     * @param pdfViewCtrl The PDFViewCtrl
     * @param pageObjNum The page object number
     * @param pageNumber The page number
     */
    public static void addPdfBookmark(Context context, PDFViewCtrl pdfViewCtrl, long pageObjNum, int pageNumber) {
        if (context == null || pdfViewCtrl == null) {
            return;
        }

        final PDFDoc pdfDoc = pdfViewCtrl.getDoc();
        if (pdfDoc == null) {
            return;
        }

        List<UserBookmarkItem> bookmarks = getPdfBookmarks(getRootPdfBookmark(pdfDoc, true));
        bookmarks.add(new UserBookmarkItem(context, pageObjNum, pageNumber));
        savePdfBookmarks(pdfViewCtrl, bookmarks, true, false);
    }

    /**
     * Handles bookmarks when a pdf bookmark is deleted.
     *
     * @param pdfViewCtrl The PDFViewCtrl
     * @param objNumber The object number
     */
    public static void onPageDeleted(PDFViewCtrl pdfViewCtrl, Long objNumber) {
        // Note no need to take snapshot here, because snapshot should take place when page is
        //      deleted rather than when its bookmarks are deleted
        if (pdfViewCtrl == null) {
            return;
        }

        final PDFDoc pdfDoc = pdfViewCtrl.getDoc();
        if (pdfDoc == null) {
            return;
        }

        List<UserBookmarkItem> items = getPdfBookmarks(getRootPdfBookmark(pdfDoc, false));
        boolean shouldUnlock = false;
        try {
            pdfDoc.lock();
            shouldUnlock = true;
            for (UserBookmarkItem item : items) {
                if (item.pageObjNum == objNumber) {
                    item.pdfBookmark.delete();
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(pdfDoc);
            }
        }
    }

    /**
     * Handles bookmarks when a pdf bookmarks is moved.
     *
     * @param pdfViewCtrl The PDFViewCtrl
     * @param objNumber The object number before moving
     * @param newObjNumber The object number after moving
     * @param newPageNumber The new page number
     * @param rebuild True if should rebuild the root bookmark
     */
    public static void onPageMoved(PDFViewCtrl pdfViewCtrl, long objNumber, long newObjNumber, int newPageNumber, @SuppressWarnings("SameParameterValue") boolean rebuild) {
        // Note no need to take snapshot here, because snapshot should take place when page is
        //      moved rather than when bookmarks are changed
        if (pdfViewCtrl == null) {
            return;
        }

        final PDFDoc pdfDoc = pdfViewCtrl.getDoc();
        if (pdfDoc == null) {
            return;
        }

        List<UserBookmarkItem> items = getPdfBookmarks(getRootPdfBookmark(pdfDoc, false));
        boolean shouldUnlock = false;
        try {
            pdfDoc.lock();
            shouldUnlock = true;
            for (UserBookmarkItem item : items) {
                if (item.pageObjNum == objNumber) {
                    item.pageObjNum = newObjNumber;
                    item.pageNumber = newPageNumber;
                    item.pdfBookmark.delete();
                    item.pdfBookmark = null;
                    break;
                }
            }
            savePdfBookmarks(pdfViewCtrl, items, false, rebuild);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(pdfDoc);
            }
        }

    }

    /**
     * Returns the list of sibling bookmarks after the specified bookmark.
     * Note: if the specified bookmark is null the sibling bookmarks after the first bookmark in
     * the document will be returned. If the specified bookmark is null and there is no sibling
     * then all bookmarks under the first bookmark will be returned.
     *
     * @param pdfDoc The PDF doc
     * @param firstSibling The first sibling
     *
     * @return The list of sibling bookmarks after the specified bookmark
     */
    @NonNull
    public static ArrayList<Bookmark> getBookmarkList(@NonNull PDFDoc pdfDoc, @Nullable Bookmark firstSibling) {
        ArrayList<Bookmark> bookmarkList = new ArrayList<>();
        Bookmark current;
        boolean shouldUnlockRead = false;

        try {
            pdfDoc.lockRead();
            shouldUnlockRead = true;
            if (firstSibling == null || !firstSibling.isValid()) {
                current = pdfDoc.getFirstBookmark();
            } else {
                current = firstSibling;
            }

            int numBookmarks = 0;
            while (current.isValid()) {
                bookmarkList.add(current);
                current = current.getNext();
                numBookmarks++;
            }
            if (firstSibling == null && numBookmarks == 1) {
                ArrayList<Bookmark> bookmarkListNextLevel = getBookmarkList(pdfDoc, pdfDoc.getFirstBookmark().getFirstChild());
                if (bookmarkListNextLevel.size() > 0) {
                    return bookmarkListNextLevel;
                }
            }
        } catch (PDFNetException e) {
            bookmarkList.clear();
        } finally {
            if (shouldUnlockRead) {
                Utils.unlockReadQuietly(pdfDoc);
            }
        }

        return bookmarkList;
    }
    // END Bookmark using Bookmark object

    // START Bookmark using SharedPreferences

    /**
     * Returns user bookmarks.
     *
     * @param context The context
     * @param filePath The file path
     *
     * @return a list of user bookmarks
     */
    public static List<UserBookmarkItem> getUserBookmarks(@Nullable Context context, String filePath) {
        ArrayList<UserBookmarkItem> userBookmarks = new ArrayList<>();
        if (context == null) {
            return userBookmarks;
        }
        SharedPreferences settings = context.getSharedPreferences(PREFS_CONTROLS_FILE_NAME, 0);
        if (settings != null) {
            String serializedDocs = settings.getString(KEY_PREF_USER_BOOKMARK + filePath, "");
            if (!Utils.isNullOrEmpty(serializedDocs)) {
                try {
                    JSONArray jsonArray = new JSONArray(serializedDocs);
                    int count = jsonArray.length();
                    for (int i = 0; i < count; ++i) {
                        UserBookmarkItem bookmarkItem = null;
                        try {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            bookmarkItem = new UserBookmarkItem(jsonObject);
                        } catch (Exception e) {
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        }
                        if (bookmarkItem != null) {
                            userBookmarks.add(bookmarkItem);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        }

        return userBookmarks;
    }

    /**
     * Saves user bookmarks.
     *
     * @param context The context
     * @param filePath The file path
     * @param data The user bookmarks
     */
    public static void saveUserBookmarks(Context context, String filePath, List<UserBookmarkItem> data) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_CONTROLS_FILE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        Gson gson = new Gson();
        Type collectionType = new TypeToken<ArrayList<UserBookmarkItem>>() {
        }.getType();
        String serializedDocs = gson.toJson(data, collectionType);

        editor.putString(KEY_PREF_USER_BOOKMARK + filePath, serializedDocs);
        editor.apply();
    }

    /**
     * Removes user bookmarks.
     *
     * @param context The context
     * @param filePath The file path
     */
    public static void removeUserBookmarks(@Nullable Context context, String filePath) {
        if (context == null) {
            return;
        }
        SharedPreferences settings = context.getSharedPreferences(PREFS_CONTROLS_FILE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(KEY_PREF_USER_BOOKMARK + filePath);
        editor.apply();
    }

    /**
     * Adds a user bookmark
     *
     * @param context The context
     * @param filePath The file path
     * @param pageObjNum The page object number
     * @param pageNumber The page number
     */
    public static void addUserBookmark(Context context, String filePath, long pageObjNum, int pageNumber) {
        if (context == null || Utils.isNullOrEmpty(filePath)) {
            return;
        }
        List<UserBookmarkItem> bookmarks = getUserBookmarks(context, filePath);
        bookmarks.add(new UserBookmarkItem(context, pageObjNum, pageNumber));
        saveUserBookmarks(context, filePath, bookmarks);
    }

    /**
     * Updates user bookmark page object number.
     *
     * @param context The context
     * @param filePath The file path
     * @param pageObjNum The old page object number
     * @param newPageObjNum The new page object number
     * @param newPageNum The new page number
     */
    @SuppressWarnings("WeakerAccess")
    public static void updateUserBookmarkPageObj(Context context, String filePath, long pageObjNum, long newPageObjNum, int newPageNum) {
        List<UserBookmarkItem> items = getUserBookmarks(context, filePath);
        for (UserBookmarkItem item : items) {
            if (item.pageObjNum == pageObjNum) {
                item.pageObjNum = newPageObjNum;
                item.pageNumber = newPageNum;
            }
        }
        saveUserBookmarks(context, filePath, items);
    }

    /**
     * Handles bookmarks when a user bookmarks is deleted.
     *
     * @param context The context
     * @param filePath The file path
     * @param objNumber The page object number
     * @param pageNumber The page number
     * @param pageCount The number of pages
     */
    public static void onPageDeleted(Context context, String filePath, Long objNumber, int pageNumber, int pageCount) {
        List<UserBookmarkItem> items = getUserBookmarks(context, filePath);
        List<UserBookmarkItem> newItems = new ArrayList<>();
        for (UserBookmarkItem item : items) {
            if (item.pageObjNum != objNumber) {
                newItems.add(item);
            }
        }
        saveUserBookmarks(context, filePath, newItems);
        updateUserBookmarksAfterRearranging(context, filePath, pageNumber, pageCount, false, -1);
    }

    /**
     * Handles bookmarks when a user bookmarks is moved.
     *
     * @param context The context
     * @param filePath The file path
     * @param objNumber The old page object number
     * @param newObjNumber The new page object number
     * @param oldPageNumber The page number before moving
     * @param newPageNumber the page number after moving
     */
    public static void onPageMoved(Context context, String filePath, long objNumber, long newObjNumber, int oldPageNumber, int newPageNumber) {
        updateUserBookmarkPageObj(context, filePath, objNumber, newObjNumber, newPageNumber);
        if (oldPageNumber < newPageNumber) {
            updateUserBookmarksAfterRearranging(context, filePath, oldPageNumber + 1, newPageNumber, false, newObjNumber);
        } else {
            updateUserBookmarksAfterRearranging(context, filePath, newPageNumber, oldPageNumber - 1, true, newObjNumber);
        }
    }

    private static void updateUserBookmarksAfterRearranging(Context context, String filePath, int fromPage, int toPage, boolean increment, long ignoreObjNumber) {
        if (fromPage > toPage) {
            int temp = fromPage;
            fromPage = toPage;
            toPage = temp;
        }
        int change = -1;
        if (increment) {
            change = 1;
        }
        List<UserBookmarkItem> items = getUserBookmarks(context, filePath);
        for (UserBookmarkItem item : items) {
            if (item.pageNumber >= fromPage && item.pageNumber <= toPage && item.pageObjNum != ignoreObjNumber) {
                item.pageNumber += change;
            }
        }
        saveUserBookmarks(context, filePath, items);
    }

    /**
     * Updates user bookmark file path.
     *
     * @param context The context
     * @param oldPath The old file path
     * @param newPath The new file path
     */
    public static void updateUserBookmarksFilePath(Context context, String oldPath, String newPath) {
        List<UserBookmarkItem> items = getUserBookmarks(context, oldPath);
        if (items.size() > 0) {
            saveUserBookmarks(context, newPath, items);
            removeUserBookmarks(context, oldPath);
        }
    }

    // END Bookmark using SharedPreferences
}
