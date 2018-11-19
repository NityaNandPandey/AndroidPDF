//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pdftron.common.Matrix2D;
import com.pdftron.common.PDFNetException;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.Convert;
import com.pdftron.pdf.DocumentConversion;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.Image;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.BookmarkManager;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.ImageMemoryCache;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerViewAdapter;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;
import com.pdftron.sdf.Obj;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;

/**
 * A Recycler view adapter for loading thumbnail views
 */
@SuppressWarnings("WeakerAccess")
public class ThumbnailsViewAdapter extends SimpleRecyclerViewAdapter<Map<String, Object>, ThumbnailsViewAdapter.PageViewHolder>
    implements PDFViewCtrl.ThumbAsyncListener, ItemTouchHelperAdapter, PasswordDialogFragment.PasswordDialogFragmentListener {

    private static final String TAG = ThumbnailsViewAdapter.class.getName();

    // The page's page number (in the PDFDoc): expects an int
    static final String PAGE_NUMBER_SRC = "page_number_src";
    // The page's thumbnail: expects a Bitmap
    static final String THUMB_IMAGE = "thumb_image";

    private static boolean sDebug = false;

    /**
     * Callback interface to be invoked when pages of the document have been edited.
     */
    public interface EditPagesListener {
        /**
         * Called when a page was moved to a new position
         *
         * @param fromPageNum The page number from which the page moves
         * @param toPageNum   The page number to which the page moves
         */
        void onPageMoved(int fromPageNum, int toPageNum);

        /**
         * Called when new pages were added to the document.
         *
         * @param pageList The list of pages added to the document
         */
        void onPagesAdded(List<Integer> pageList);
    }

    private EditPagesListener mEditPageListener;

    private Context mContext;
    private FragmentManager mFragmentManager;
    private PDFViewCtrl mPdfViewCtrl;

    private LayoutInflater mLayoutInflater;

    // zero-indexed data source for the view
    private List<Map<String, Object>> mDataList;
    // page-indexed page number for thumbnails that are cached in memory
    private List<Integer> mCachedPageList;

    private SparseArray<LoadThumbnailTask> mTaskList;

    // For the page back and forward buttons:
    // When a page is moved, deleted or added the document has been modified.
    // If the document has been changed, the page back and forward stacks should be cleared.
    // However, if a page is added to the end of the document, the
    // page back and forward stacks are still considered valid.
    private boolean mDocPagesModified = false;

    private int mCurrentPage;
    private int mSpanCount;
    private int mRecyclerViewWidth;

    private final Object mPauseWorkLock = new Object();
    private boolean mPauseWork = false;

    private final Lock mDataLock = new ReentrantLock();

    private int mPwdRequestLastPage;
    private Uri mPwdRequestUri;

    /**
     * The format of document
     */
    public enum DocumentFormat {
        /**
         * Specified PDF page(s)
         */
        PDF_PAGE,
        /**
         * Blank PDF page
         */
        BLANK_PDF_PAGE,
        /**
         * A PDF document
         */
        PDF_DOC,
        /**
         * An image
         */
        IMAGE
    }

    /**
     * Class constructor
     */
    public ThumbnailsViewAdapter(Context context, EditPagesListener listener, FragmentManager fragmentManager, PDFViewCtrl pdfViewCtrl,
                                 List<Map<String, Object>> dataList, int spanCount,
                                 ViewHolderBindListener bindListener) {
        super(bindListener);
        mContext = context;
        mEditPageListener = listener;
        mFragmentManager = fragmentManager;
        mPdfViewCtrl = pdfViewCtrl;
        mDataList = dataList;
        mCachedPageList = new ArrayList<>();
        mTaskList = new SparseArray<>();
        mSpanCount = spanCount;
        mCurrentPage = mPdfViewCtrl.getCurrentPage();
        mPdfViewCtrl.addThumbAsyncListener(this);
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#onDetachedFromRecyclerView(RecyclerView)}.
     */
    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mPdfViewCtrl.removeThumbAsyncListener(this);
    }

    /**
     * Cleans up resources
     */
    public void finish() {
        mPdfViewCtrl.removeThumbAsyncListener(this);
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#getItemCount()}.
     */
    @Override
    public int getItemCount() {
        return (mDataList != null) ? mDataList.size() : 0;
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#getItem(int)}.
     */
    @Override
    public Map<String, Object> getItem(int position) {
        if (mDataList != null && position >= 0 && position < mDataList.size()) {
            return mDataList.get(position);
        }
        return null;
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#onCreateViewHolder(ViewGroup, int)}.
     */
    @Override
    public PageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = getLayoutInflater().inflate(R.layout.controls_thumbnails_view_grid_item, parent, false);
        return new PageViewHolder(view);
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#onBindViewHolder(RecyclerView.ViewHolder, int)}.
     */
    @Override
    public void onBindViewHolder(PageViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        if (mPdfViewCtrl == null) {
            return;
        }

        ViewGroup.LayoutParams params = holder.imageLayout.getLayoutParams();
        params.width = mRecyclerViewWidth / mSpanCount;
        params.height = (int) (params.width * 1.29); // US-letter size
        holder.imageLayout.requestLayout();

        int pageNum = position + 1;
        Map<String, Object> itemMap = getItem(position);
        if (itemMap == null) {
            return;
        }
        int pageNumSrc = (int) itemMap.get(PAGE_NUMBER_SRC);


        String pageLabel = ViewerUtils.getPageLabelTitle(mPdfViewCtrl, pageNum);
        if (!Utils.isNullOrEmpty(pageLabel)) {
            holder.pageNumber.setText(pageLabel);
        } else {
            holder.pageNumber.setText(Utils.getLocaleDigits(Integer.toString(pageNum)));
        }

        if (pageNumSrc == mCurrentPage) {
            holder.pageNumber.setBackgroundResource(R.drawable.controls_thumbnails_view_rounded_edges_current);
        } else {
            holder.pageNumber.setBackgroundResource(R.drawable.controls_thumbnails_view_rounded_edges);
        }

        mDataLock.lock();
        Bitmap bitmapFromCache = (Bitmap) itemMap.get(THUMB_IMAGE);
        mDataLock.unlock();
        if (bitmapFromCache != null && !bitmapFromCache.isRecycled()) {
            if (sDebug)
                Log.d(TAG, "using cached thumb bitmap for page: " + Integer.toString(pageNumSrc));
            holder.thumbImage.setImageBitmap(bitmapFromCache);
            holder.thumbImage.setBackgroundColor(mContext.getResources().getColor(R.color.controls_thumbnails_view_bg));
        } else {
            if (sDebug) Log.d(TAG, "use null; no cache for page: " + Integer.toString(pageNumSrc));
            holder.thumbImage.setImageBitmap(null);
            holder.thumbImage.setBackgroundColor(mContext.getResources().getColor(R.color.controls_thumbnails_view_bg));
            if (mTaskList.get(pageNumSrc) != null) {
                if (sDebug)
                    Log.d(TAG, "A task is already running for page: " + Integer.toString(pageNumSrc));
            } else {
                if (sDebug) Log.d(TAG, "getThumbAsync for page: " + Integer.toString(pageNumSrc));
                try {
                    mPdfViewCtrl.getThumbAsync(pageNumSrc);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }
    }

    @NonNull
    private LayoutInflater getLayoutInflater() {
        if (mLayoutInflater == null) {
            mLayoutInflater = LayoutInflater.from(mContext);
        }
        return mLayoutInflater;
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#add(Object)}.
     */
    @Override
    public void add(Map<String, Object> item) {
        if (mDataList != null && item != null) {
            mDataList.add(item);
        }
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#insert(Object, int)}.
     */
    @Override
    public void insert(Map<String, Object> item, int position) {
        if (mDataList != null && item != null) {
            mDataList.add(position, item);
        }
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#remove(Object)}.
     */
    @Override
    public boolean remove(Map<String, Object> item) {
        return (mDataList != null && item != null && mDataList.remove(item));
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#removeAt(int)}.
     */
    @Override
    public Map<String, Object> removeAt(int location) {
        if (location < 0 || mDataList == null || location >= mDataList.size()) {
            return null;
        }
        return mDataList.remove(location);
    }

    /**
     * The overload implementation of {@link SimpleRecyclerViewAdapter#updateSpanCount(int)}.
     */
    @Override
    public void updateSpanCount(int count) {
        mSpanCount = count;
    }

    /**
     * The overload implementation of {@link ItemTouchHelperAdapter#onItemMove(int, int)}.
     */
    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (toPosition < getItemCount()) {
            // Move item
            Map<String, Object> item = removeAt(fromPosition);
            insert(item, toPosition);
            // Update UI to reflect changes
            notifyItemMoved(fromPosition, toPosition);
            return true;
        }
        return false;
    }

    /**
     * The overload implementation of {@link ItemTouchHelperAdapter#onItemDrop(int, int)}.
     */
    @Override
    public void onItemDrop(int fromPosition, int toPosition) {
        if (fromPosition != RecyclerView.NO_POSITION && toPosition != RecyclerView.NO_POSITION &&
            fromPosition != toPosition) {
            // Update PDFDoc
            moveDocPage(fromPosition, toPosition);
        }
    }

    /**
     * The overload implementation of {@link ItemTouchHelperAdapter#onItemDismiss(int)}.
     */
    @Override
    public void onItemDismiss(int position) {
        // Do nothing
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ThumbAsyncListener#onThumbReceived(int, int[], int, int)}.
     */
    @Override
    public void onThumbReceived(int page, int[] buf, int width, int height) {
        if (sDebug) Log.d(TAG, "onThumbReceived received page: " + Integer.toString(page));
        int position = page - 1;

        // only update the ImageView if it is on screen
        PageViewHolder holder = null;
        RecyclerView recyclerView = getRecyclerView();
        if (recyclerView != null) {
            holder = (PageViewHolder) recyclerView.findViewHolderForLayoutPosition(position);
        }
        Map<String, Object> source = getItem(position);
        if (source != null) {
            if (mTaskList.get(page) == null) {
                if (sDebug) Log.d(TAG, "startLoadBitmapTask for page: " + Integer.toString(page));
                final LoadThumbnailTask task = new LoadThumbnailTask(holder, page, buf, width, height); // it is OK for holder to be null
                mTaskList.put(page, task);
                task.execute();
            } else {
                if (sDebug)
                    Log.d(TAG, "A task is already running for page: " + Integer.toString(page));
            }
        }
    }

    /**
     * The overload implementation of {@link PasswordDialogFragment.PasswordDialogFragmentListener#onPasswordDialogPositiveClick(int, File, String, String, String)}.
     */
    @Override
    public void onPasswordDialogPositiveClick(int fileType, File file, String path, String password, String id) {
        addDocPages(mPwdRequestLastPage, DocumentFormat.PDF_DOC, mPwdRequestUri, password);
    }

    /**
     * The overload implementation of {@link PasswordDialogFragment.PasswordDialogFragmentListener#onPasswordDialogPositiveClick(int, File, String, String, String)}.
     */
    @Override
    public void onPasswordDialogNegativeClick(int fileType, File file, String path) {

    }

    /**
     * The overload implementation of {@link PasswordDialogFragment.PasswordDialogFragmentListener#onPasswordDialogPositiveClick(int, File, String, String, String)}.
     */
    @Override
    public void onPasswordDialogDismiss(boolean forcedDismiss) {

    }

    /**
     * Sets the current page.
     *
     * @param pageNum The page number to be set as the current
     */
    public void setCurrentPage(int pageNum) {
        mCurrentPage = pageNum;
    }

    /**
     * @return The current page
     */
    public int getCurrentPage() {
        return mCurrentPage;
    }

    /**
     * @return True if the the pages of the document have been modified
     */
    public boolean getDocPagesModified() {
        return mDocPagesModified;
    }

    /**
     * Updates the main view width
     *
     * @param width The width
     */
    public void updateMainViewWidth(int width) {
        mRecyclerViewWidth = width;
    }

    private void setThumbnailBitmap(PageViewHolder holder, Bitmap bitmap) {
        if (sDebug)
            Log.d(TAG, "setThumbnailBitmap for page: " + (holder != null ? holder.pageNumber.getText().toString() : "-1"));
        if (holder != null && holder.thumbImage != null) {
            if (bitmap != null && !bitmap.isRecycled()) {
                Animation fadeAnimation = AnimationUtils.loadAnimation(mContext, R.anim.controls_thumbnails_view_fadein);
                if (sDebug)
                    Log.d(TAG, "holder not null; setThumbnailBitmap for page: " + holder.pageNumber.getText().toString());
                holder.thumbImage.setImageBitmap(bitmap);
                holder.thumbImage.setBackgroundColor(mContext.getResources().getColor(R.color.controls_thumbnails_view_bg));
                holder.thumbImage.setAnimation(fadeAnimation);
            } else {
                if (sDebug)
                    Log.d(TAG, "ERROR setThumbnailViewImageBitmapDrawable src not available");
            }
        }
    }

    /**
     * Clears off screen resources.
     */
    public void clearOffScreenResources() {
        int firstPosition = 0;
        int lastPosition = getItemCount();
        int offScreenKeepInCacheCount = mSpanCount;
        RecyclerView recyclerView = getRecyclerView();
        if (recyclerView != null && recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            firstPosition = layoutManager.findFirstVisibleItemPosition();
            lastPosition = layoutManager.findLastVisibleItemPosition();
        }
        if (firstPosition == RecyclerView.NO_POSITION || lastPosition == RecyclerView.NO_POSITION) {
            return;
        }

        if (sDebug)
            Log.d(TAG, "clearOffScreenResources:first:" + firstPosition + ";last:" + lastPosition);

        Iterator<Integer> it = mCachedPageList.iterator();
        while (it.hasNext()) {
            int page = it.next();
            int position = page - 1;

            if (position < (firstPosition - offScreenKeepInCacheCount) || position > (lastPosition + offScreenKeepInCacheCount)) {
                // not in range: clear BitmapDrawable cache, recycle Bitmap
                try {
                    Map<String, Object> source = getItem(position);
                    if (source != null) {
                        // clear the cache
                        mDataLock.lock();
                        source.put(THUMB_IMAGE, null);
                        mDataLock.unlock();
                        if (sDebug)
                            Log.d(TAG, "remove image cache for page: " + page + "; position: " + position);
                        // this page is no longer cached
                        it.remove();
                    }
                } catch (Exception ignored) {
                }
            } // else in range: do nothing
        }
        // cancel all off screen tasks as well
        for (int i = 0; i < mTaskList.size(); i++) {
            int page = mTaskList.keyAt(i);
            int position = page - 1;
            if (position < (firstPosition - offScreenKeepInCacheCount) || position > (lastPosition + offScreenKeepInCacheCount)) {
                LoadThumbnailTask task = mTaskList.valueAt(i);
                if (null != task) {
                    task.cancel(true);
                }
            } // else in range: do nothing
        }
    }

    /**
     * Clears resources.
     */
    public void clearResources() {
        for (int i = 0; i < getItemCount(); i++) {
            Map<String, Object> itemMap = getItem(i);
            if (itemMap != null) {
                if (sDebug)
                    Log.d(TAG, "clearResources recycle page: " + itemMap.get(PAGE_NUMBER_SRC));
                mDataLock.lock();
                ImageMemoryCache.getInstance().addBitmapToReusableSet((Bitmap) itemMap.get(THUMB_IMAGE));
                itemMap.put(THUMB_IMAGE, null);
                mDataLock.unlock();
            }
        }
    }

    void setPauseWork(boolean pauseWork) {
        synchronized (mPauseWorkLock) {
            mPauseWork = pauseWork;
            if (!mPauseWork) {
                mPauseWorkLock.notifyAll();
            }
        }
    }

    private void moveDocPage(int fromPosition, int toPosition) {
        mDocPagesModified = true;

        // avoid index out of bounds
        if (fromPosition > -1 && fromPosition < getItemCount() &&
            toPosition > -1 && toPosition < getItemCount()) {
            // Update PDFDoc
            int fromPageNum = fromPosition + 1;
            int toPageNum = toPosition + 1;
            boolean shouldUnlock = false;
            try {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;

                // get the page to move
                final PDFDoc doc = mPdfViewCtrl.getDoc();
                if (doc == null) {
                    return;
                }
                Page pageToMove = doc.getPage(fromPageNum);
                if (pageToMove == null) {
                    return;
                }

                if (fromPageNum < toPageNum) {
                    // get destination page iterator
                    PageIterator moveTo = doc.getPageIterator(toPageNum + 1);
                    // copy to destination
                    doc.pageInsert(moveTo, pageToMove);
                    // delete original page
                    PageIterator itr = doc.getPageIterator(fromPageNum);
                    doc.pageRemove(itr);
                } else if (fromPageNum > toPageNum) {
                    // get destination page iterator
                    PageIterator moveTo = doc.getPageIterator(toPageNum);
                    // copy to destination
                    doc.pageInsert(moveTo, pageToMove);
                    // delete original page
                    PageIterator itr = doc.getPageIterator(fromPageNum + 1);
                    doc.pageRemove(itr);
                }

                // update user bookmarks
                updateUserBookmarks(fromPageNum, toPageNum, pageToMove.getSDFObj().getObjNum(),
                    doc.getPage(toPageNum).getSDFObj().getObjNum());

                if (mEditPageListener != null) {
                    mEditPageListener.onPageMoved(fromPageNum, toPageNum);
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }

            // Adjust logical page numbers between original and new positions
            boolean currentPageUpdated = false;
            for (int i = Math.min(fromPosition, toPosition); i <= Math.max(fromPosition, toPosition); i++) {
                Map<String, Object> itemMap = getItem(i);
                if (itemMap != null) {
                    int oldPageNum = (Integer) itemMap.get(PAGE_NUMBER_SRC);
                    if (!currentPageUpdated && oldPageNum == mCurrentPage) {
                        // Update current page
                        mCurrentPage = i + 1;
                        currentPageUpdated = true;
                    }
                    itemMap.put(PAGE_NUMBER_SRC, i + 1);
                }
            }

            Utils.safeNotifyDataSetChanged(this);
        }
    }

    private class AddDocPagesTask extends CustomAsyncTask<Void, Void, Void> {

        private static final int MIN_DELAY = 250;
        private ProgressDialog mProgressDialog;
        private CountDownTimer mCountDownTimer;
        private int mPosition;
        private DocumentFormat mDocumentFormat;
        private Object mData;
        private String mPassword;
        private boolean mInsert;
        private int mNewPageNum = 1;
        private boolean mIsNotPdf = false;
        private PDFDoc mDocTemp;

        AddDocPagesTask(Context context, int position, DocumentFormat documentFormat, Object data, String password) {
            super(context);
            mPosition = position;
            mDocumentFormat = documentFormat;
            mData = data;
            mPassword = password;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setIndeterminate(true);
            if (documentFormat == DocumentFormat.IMAGE) {
                mProgressDialog.setMessage(context.getResources().getString(R.string.add_image_wait));
                mProgressDialog.setCancelable(false);
            } else {
                mProgressDialog.setMessage(context.getResources().getString(R.string.add_pdf_wait));
                mProgressDialog.setCancelable(true);
            }
            mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    AddDocPagesTask.this.cancel(true);
                }
            });

            mCountDownTimer = new CountDownTimer(MIN_DELAY, MIN_DELAY + 1) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    mProgressDialog.show();
                }
            };
        }

        @Override
        protected void onPreExecute() {
            final Context context = getContext();
            if (context == null) {
                return;
            }
            boolean toastNeeded = true;

            mCountDownTimer.start();
            // get the total page count and push back index

            int pageCount;
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                final PDFDoc doc = mPdfViewCtrl.getDoc();
                if (doc == null) {
                    return;
                }
                pageCount = doc.getPageCount();
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
                cancel(true);
                return;
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
            if (mPosition < 0) {
                // add page to end of file
                mNewPageNum = pageCount + 1;
            } else {
                mNewPageNum = mPosition + 1;
            }
            mInsert = (mNewPageNum <= pageCount);

            // if another document is going to be added
            mDocTemp = null;
            SecondaryFileFilter filter = null;
            if (mDocumentFormat == DocumentFormat.PDF_DOC) {
                boolean shouldUnlock = false;
                boolean canAdd;
                try {
                    if (Utils.isNotPdf(((Uri) mData).getEncodedPath())) {
                        mIsNotPdf = true;
                        return;
                    }
                    // shouldn't close filter since it is attached to mDocTemp
                    filter = new SecondaryFileFilter(context, (Uri) mData);
                    mDocTemp = new PDFDoc(filter);

                    canAdd = true;
                    mDocTemp.lock();
                    shouldUnlock = true;
                    do {
                        // Is this doc password protected?
                        if (!mDocTemp.initSecurityHandler()) {
                            if (mPassword == null || !mDocTemp.initStdSecurityHandler(mPassword)) {
                                canAdd = false;
                                toastNeeded = false;
                                mPwdRequestLastPage = mPosition;
                                mPwdRequestUri = (Uri) mData;

                                PasswordDialogFragment passwordDialog = PasswordDialogFragment.newInstance(0, null, ((Uri) mData).getEncodedPath(), "");
                                passwordDialog.setListener(ThumbnailsViewAdapter.this);
                                passwordDialog.setMessage(R.string.dialog_password_message);
                                passwordDialog.show(mFragmentManager, "password_dialog");
                                break;
                            }
                        }

                        // Does this doc need XFA rendering?
                        Obj needsRenderingObj = mDocTemp.getRoot().findObj("NeedsRendering");
                        if (needsRenderingObj != null && needsRenderingObj.isBool() && needsRenderingObj.getBool()) {
                            canAdd = false;
                            toastNeeded = false;
                            Utils.showAlertDialogWithLink(context, context.getString(R.string.error_has_xfa_forms_message), "");
                            break;
                        }

                        // Is this doc a package/portfolio?
                        Obj collectionObj = mDocTemp.getRoot().findObj("Collection");
                        if (collectionObj != null) {
                            canAdd = false;
                            toastNeeded = false;
                            Utils.showAlertDialogWithLink(context, context.getString(R.string.error_has_portfolio_message), "");
                            break;
                        }
                    } while (false);
                } catch (Exception e) {
                    canAdd = false;
                } finally {
                    if (shouldUnlock) {
                        Utils.unlockQuietly(mDocTemp);
                    }
                    // note: shouldn't close mDocTemp here, it will be closed later in doInBackground
                    if (mDocTemp == null) {
                        Utils.closeQuietly(filter);
                    }
                }

                if (!canAdd) {
                    mDocTemp = null;
                    if (toastNeeded) {
                        CommonToast.showText(context, context.getResources().getString(R.string.dialog_add_pdf_document_error_message), Toast.LENGTH_SHORT);
                    }
                }
            }
        }

        @SuppressWarnings("WrongThread")
        @Override
        protected Void doInBackground(Void... args) {
            if (isCancelled()) {
                return null;
            }

            boolean shouldUnlockDocTemp = false;
            boolean shouldUnlock = false;
            SecondaryFileFilter filter = null;
            try {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                final PDFDoc doc = mPdfViewCtrl.getDoc();
                if (doc == null) {
                    return null;
                }

                Page page;
                switch (mDocumentFormat) {
                    case PDF_PAGE:
                        if (mData != null && (mData instanceof Page || mData instanceof Page[])) {
                            Page[] pages;
                            if (mData instanceof Page[]) {
                                pages = (Page[]) mData;
                            } else { // instance of Page
                                pages = new Page[1];
                                pages[0] = (Page) mData;
                            }

                            for (Page p : pages) {
                                if (isCancelled()) {
                                    return null;
                                }
                                if (mInsert) {
                                    PageIterator pageIterator = doc.getPageIterator(mNewPageNum);
                                    doc.pageInsert(pageIterator, p);
                                } else {
                                    doc.pagePushBack(p);
                                }
                            }
                        }
                        break;
                    case BLANK_PDF_PAGE:
                        // create a new blank page and add to end of the document.
                        // the page before the destination is used to set the blank page's size.
                        page = doc.pageCreate();
                        Rect pageRect = getPDFPageRect(mNewPageNum - 1);
                        // uses pageSizeSource's page size
                        page.setMediaBox(pageRect);
                        page.setCropBox(pageRect);
                        if (mInsert) {
                            PageIterator pageIterator = doc.getPageIterator(mNewPageNum);
                            doc.pageInsert(pageIterator, page);
                        } else {
                            doc.pagePushBack(page);
                        }
                        break;
                    case PDF_DOC:
                        if (mIsNotPdf) {
                            ContentResolver cr = Utils.getContentResolver(getContext());
                            if (cr == null) {
                                return null;
                            }
                            filter = new SecondaryFileFilter(getContext(), (Uri) mData);
                            DocumentConversion conv = Convert.universalConversion(filter, null);
                            while (conv.getConversionStatus() == DocumentConversion.e_incomplete) {
                                conv.convertNextPage();
                                if (isCancelled()) {
                                    return null;
                                }
                            }

                            if (conv.getConversionStatus() == DocumentConversion.e_failure || conv.getConversionStatus() != DocumentConversion.e_success) {
                                break;
                            }

                            mDocTemp = conv.getDoc();
                        }

                        if (isCancelled()) {
                            return null;
                        }

                        if (mDocTemp != null) {
                            mDocTemp.lock();
                            shouldUnlockDocTemp = true;
                            doc.insertPages(mNewPageNum, mDocTemp, 1, mDocTemp.getPageCount(), PDFDoc.InsertBookmarkMode.INSERT, null);
                        }
                        break;
                    case IMAGE:
                        if (mData instanceof Bitmap) {
                            Image img = Image.create(doc.getSDFDoc(), (Bitmap) mData);
                            ElementBuilder f = new ElementBuilder();
                            Element element = f.createImage(img, new Matrix2D(img.getImageWidth(), 0, 0, img.getImageHeight(), 0, 0));

                            // Change page size
                            page = doc.pageCreate();
                            page.setMediaBox(new Rect(0, 0, img.getImageWidth(), img.getImageHeight()));
                            page.setCropBox(new Rect(0, 0, img.getImageWidth(), img.getImageHeight()));

                            if (isCancelled()) {
                                return null;
                            }

                            ElementWriter writer = new ElementWriter();
                            writer.begin(page);
                            writer.writePlacedElement(element);
                            writer.end();

                            if (isCancelled()) {
                                return null;
                            }

                            if (mInsert) {
                                PageIterator pageIterator = doc.getPageIterator(mNewPageNum);
                                doc.pageInsert(pageIterator, page);
                            } else {
                                doc.pagePushBack(page);
                            }
                        }
                        break;
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "AddDocPagesTask");
                return null;
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
                if (shouldUnlockDocTemp) {
                    Utils.unlockQuietly(mDocTemp);
                }
                Utils.closeQuietly(mDocTemp, filter);
            }

            return null;
        }

        @Override
        protected void onCancelled() {
            mCountDownTimer.cancel();
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }

        @Override
        protected void onPostExecute(Void arg) {
            Context context = getContext();
            if (context == null) {
                return;
            }
            mCountDownTimer.cancel();
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            if (!isCancelled()) {
                int pageCount;
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    final PDFDoc doc = mPdfViewCtrl.getDoc();
                    if (doc == null) {
                        return;
                    }
                    pageCount = doc.getPageCount();
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                    CommonToast.showText(context, context.getResources().getString(R.string.dialog_add_pdf_document_error_message), Toast.LENGTH_SHORT);
                    return;
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
                int pageAddedCnt = pageCount - mDataList.size();

                // TODO use the cached bitmaps for existing pages
                mDataList.clear();
                for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put(ThumbnailsViewAdapter.PAGE_NUMBER_SRC, pageNum);
                    itemMap.put(ThumbnailsViewAdapter.THUMB_IMAGE, null);
                    add(itemMap);
                }

                Utils.safeNotifyDataSetChanged(ThumbnailsViewAdapter.this);
                safeScrollToPosition(mNewPageNum - 1);

                List<Integer> pageList = new ArrayList<>(pageAddedCnt);
                for (int i = 0; i < pageAddedCnt; i++) {
                    pageList.add(mNewPageNum + i);
                }

                if (mEditPageListener != null) {
                    mEditPageListener.onPagesAdded(pageList);
                }
            }
        }
    }

    private class DuplicateDocPagesTask extends CustomAsyncTask<Void, Void, Void> {

        private static final int MIN_DELAY = 250;
        private ProgressDialog mProgressDialog;
        private CountDownTimer mCountDownTimer;
        private List<Integer> mPageList;
        private int mNewPageNum = 1;

        DuplicateDocPagesTask(Context context, List<Integer> pageList) {
            super(context);
            mPageList = pageList;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage(context.getResources().getString(R.string.add_pdf_wait));
            mProgressDialog.setCancelable(true);

            mProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    cancel(true);
                }
            });

            mCountDownTimer = new CountDownTimer(MIN_DELAY, MIN_DELAY + 1) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    mProgressDialog.show();
                }
            };
        }

        @Override
        protected void onPreExecute() {
            mCountDownTimer.start();
        }

        @SuppressWarnings("WrongThread")
        @Override
        protected Void doInBackground(Void... args) {
            if (isCancelled()) {
                return null;
            }

            boolean shouldUnlock = false;
            try {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                final PDFDoc doc = mPdfViewCtrl.getDoc();
                if (doc == null) {
                    return null;
                }

                // add duplicated pages after the last selected page
                Collections.sort(mPageList, Collections.<Integer>reverseOrder());
                int lastSelectedPage = mPageList.get(0);
                mNewPageNum = lastSelectedPage + 1;
                int count = mPageList.size();
                for (int i = 0; i < count; ++i) {
                    if (isCancelled()) {
                        break;
                    }
                    Page page = doc.getPage(mPageList.get(i));
                    PageIterator pageIterator = doc.getPageIterator(mNewPageNum);
                    doc.pageInsert(pageIterator, page);
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
                return null;
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }

            return null;
        }

        @Override
        protected void onCancelled() {
            mCountDownTimer.cancel();
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }

        @Override
        protected void onPostExecute(Void arg) {
            Context context = getContext();
            if (context == null) {
                return;
            }

            mCountDownTimer.cancel();
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            if (!isCancelled()) {
                int pageCount;
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    final PDFDoc doc = mPdfViewCtrl.getDoc();
                    if (doc == null) {
                        return;
                    }
                    pageCount = doc.getPageCount();
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                    CommonToast.showText(context, context.getResources().getString(R.string.dialog_add_pdf_document_error_message), Toast.LENGTH_SHORT);
                    return;
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
                int pageAddedCnt = pageCount - mDataList.size();

                mDataList.clear();
                for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put(ThumbnailsViewAdapter.PAGE_NUMBER_SRC, pageNum);
                    itemMap.put(ThumbnailsViewAdapter.THUMB_IMAGE, null);
                    add(itemMap);
                }

                Utils.safeNotifyDataSetChanged(ThumbnailsViewAdapter.this);
                safeScrollToPosition(mNewPageNum - 1);

                List<Integer> pageList = new ArrayList<>(pageAddedCnt);
                for (int i = 0; i < pageAddedCnt; i++) {
                    pageList.add(mNewPageNum + i);
                }

                if (mEditPageListener != null) {
                    mEditPageListener.onPagesAdded(pageList);
                }
            }
        }
    }

    /**
     * Updates the adapter after pages addition.
     *
     * @param pageList The list of pages added to the document
     */
    public void updateAfterAddition(List<Integer> pageList) {
        int pageNum = Collections.min(pageList);
        mDocPagesModified = true;
        int pageCount;
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            final PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null) {
                return;
            }
            pageCount = doc.getPageCount();
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
            return;
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        try {
            mDataList.clear();
            for (int p = 1; p <= pageCount; p++) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put(ThumbnailsViewAdapter.PAGE_NUMBER_SRC, p);
                itemMap.put(ThumbnailsViewAdapter.THUMB_IMAGE, null);
                add(itemMap);
            }
        } catch (Exception ignored) {
        }

        Utils.safeNotifyDataSetChanged(this);
        safeScrollToPosition(pageNum - 1);
    }

    /**
     * Updates the adapter after pages deletion.
     *
     * @param pageList The list of pages removed from the document
     */
    public void updateAfterDeletion(List<Integer> pageList) {
        if (pageList == null || pageList.size() == 0) {
            return;
        }

        mDocPagesModified = true;
        mCurrentPage -= pageList.size();

        Collections.sort(pageList); // since we will use binary search

        // Update page numbers
        ListIterator<Map<String, Object>> it = mDataList.listIterator();
        int deleteCnt = 0;
        Integer pageNum;
        while (it.hasNext()) {
            Map<String, Object> item = it.next();
            pageNum = (Integer) item.get(PAGE_NUMBER_SRC);
            try {
                if (Collections.binarySearch(pageList, pageNum) >= 0) {
                    mDataLock.lock();
                    item.put(THUMB_IMAGE, null);
                    mDataLock.unlock();

                    it.remove();

                    // Update cached page list
                    removeCachedPage(pageNum);

                    ++deleteCnt;
                } else {
                    item.put(PAGE_NUMBER_SRC, pageNum - deleteCnt);
                }
            } catch (Exception ignored) {
            }
        }

        Utils.safeNotifyDataSetChanged(this);

        // scroll to the item after the first deleted item
        pageNum = Collections.min(pageList);
        if (pageNum == mDataList.size()) {
            --pageNum;
        }
        safeScrollToPosition(pageNum - 1);
    }

    /**
     * Updates the adapter after pages rotation.
     *
     * @param pageList The list of rotated pages
     */
    public void updateAfterRotation(List<Integer> pageList) {
        if (pageList == null || pageList.size() == 0) {
            return;
        }

        mDocPagesModified = true;

        Collections.sort(pageList); // since we will use binary search

        // Update page numbers
        ListIterator<Map<String, Object>> it = mDataList.listIterator();
        Integer pageNum = 1;
        while (it.hasNext()) {
            Map<String, Object> item = it.next();
            pageNum = (Integer) item.get(PAGE_NUMBER_SRC);
            try {
                if (Collections.binarySearch(pageList, pageNum) >= 0) {
                    mDataLock.lock();
                    item.put(THUMB_IMAGE, null);
                    mDataLock.unlock();
                    // Update cached page list
                    removeCachedPage(pageNum);
                }
            } catch (Exception ignored) {
            }
        }

        Utils.safeNotifyDataSetChanged(this);
        safeScrollToPosition(pageNum - 1);
    }

    /**
     * Updates the adapter after page movement.
     *
     * @param fromPageNum The page number from which the page was moved
     * @param toPageNum   The page number to which the page was moved
     */
    public void updateAfterMove(int fromPageNum, int toPageNum) {
        try {
            int start = Math.min(fromPageNum - 1, toPageNum - 1);
            int end = Math.max(fromPageNum - 1, toPageNum - 1);
            if (start >= 0 && end < getItemCount() && start != end) {
                // Adjust logical page numbers and thumbnails between original and new positions
                if (fromPageNum > toPageNum) {
                    Map<String, Object> itemMap = getItem(end);
                    if (itemMap != null) {
                        mDataLock.lock();
                        Bitmap lastBitmap = (Bitmap) itemMap.get(THUMB_IMAGE);
                        boolean currentPageUpdated = false;
                        for (int i = start; i <= end; ++i) {
                            itemMap = getItem(i);
                            if (itemMap == null) {
                                break;
                            }
                            int oldPageNum = (Integer) itemMap.get(PAGE_NUMBER_SRC);
                            Bitmap tempBitmap = (Bitmap) itemMap.get(THUMB_IMAGE);
                            if (!currentPageUpdated && oldPageNum == mCurrentPage) {
                                // Update current page
                                mCurrentPage = i + 1;
                                currentPageUpdated = true;
                            }
                            itemMap.put(PAGE_NUMBER_SRC, i + 1);
                            itemMap.put(THUMB_IMAGE, lastBitmap);
                            lastBitmap = tempBitmap;
                        }
                        mDataLock.unlock();
                    }
                } else {
                    Map<String, Object> itemMap = getItem(start);
                    if (itemMap != null) {
                        mDataLock.lock();
                        Bitmap lastBitmap = (Bitmap) itemMap.get(THUMB_IMAGE);
                        boolean currentPageUpdated = false;
                        for (int i = end; i >= start; --i) {
                            itemMap = getItem(i);
                            if (itemMap == null) {
                                break;
                            }
                            int oldPageNum = (Integer) itemMap.get(PAGE_NUMBER_SRC);
                            Bitmap tempBitmap = (Bitmap) itemMap.get(THUMB_IMAGE);
                            if (!currentPageUpdated && oldPageNum == mCurrentPage) {
                                // Update current page
                                mCurrentPage = i + 1;
                                currentPageUpdated = true;
                            }
                            itemMap.put(PAGE_NUMBER_SRC, i + 1);
                            itemMap.put(THUMB_IMAGE, lastBitmap);
                            lastBitmap = tempBitmap;
                        }
                        mDataLock.unlock();
                    }
                }

                Utils.safeNotifyDataSetChanged(this);
                safeScrollToPosition(toPageNum - 1);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void safeScrollToPosition(int scrollIndex) {
        final RecyclerView recyclerView = getRecyclerView();
        if (recyclerView != null) {
            boolean scrollToPosition;
            if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int firstVisibleIndex = layoutManager.findFirstVisibleItemPosition();
                int lastVisibleIndex = layoutManager.findLastVisibleItemPosition();
                scrollToPosition = (scrollIndex < firstVisibleIndex || scrollIndex > lastVisibleIndex);
            } else {
                scrollToPosition = true;
            }
            if (scrollToPosition) {
                // View-holder is not ready or not in view - scroll to its position
                recyclerView.scrollToPosition(scrollIndex);
            }
        }
    }

    /**
     * Adds document pages at the specified (zero-indexed) position, or to end of file if position is -1.
     * Lastly scroll to newly created page, if necessary
     *
     * @param position       The position where the pages are added on
     * @param documentFormat The document format
     * @param data           The extra data including page(s)
     */
    public void addDocPages(int position, DocumentFormat documentFormat, Object data) {
        mDocPagesModified = true;
        new AddDocPagesTask(mContext, position, documentFormat, data, null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Adds document pages at the specified (zero-indexed) position, or to end of file if position is -1.
     * Lastly scroll to newly created page, if necessary
     *
     * @param position       The position where the pages are inserted to
     * @param documentFormat The document format
     * @param data           The extra data including page(s)
     * @param password       The document password
     */
    public void addDocPages(int position, DocumentFormat documentFormat, Object data, String password) {
        mDocPagesModified = true;
        new AddDocPagesTask(mContext, position, documentFormat, data, password).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Duplicates pages.
     *
     * @param pageList The list of pages to be duplicated
     */
    public void duplicateDocPages(List<Integer> pageList) {
        mDocPagesModified = true;
        new DuplicateDocPagesTask(mContext, pageList).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Removes a document page.
     *
     * @param pageNum The page number to be removed
     */
    public void removeDocPage(int pageNum) {
        mDocPagesModified = true;

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            final PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null) {
                return;
            }

            Page pageToDelete = doc.getPage(pageNum);
            PageIterator pageIterator = doc.getPageIterator(pageNum);
            doc.pageRemove(pageIterator);

            removeUserBookmarks(pageToDelete.getSDFObj().getObjNum(), pageNum, doc.getPageCount());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        int position = updatePageNumberOnDelete(pageNum);
        if (position >= 0) {
            notifyItemRemoved(position);
        }
    }

    /**
     * Rotates the document page.
     *
     * @param pageNum The page number to be rotated
     */
    public void rotateDocPage(int pageNum) {
        mDocPagesModified = true;
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            final PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null) {
                return;
            }
            Page pageToRotate = doc.getPage(pageNum);
            int pageRotation = (pageToRotate.getRotation() + 1) % 4;
            pageToRotate.setRotation(pageRotation);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        int position = getPositionForPage(pageNum);
        if (position < 0) {
            // fallback - assume that position is pageNum-1
            position = pageNum - 1;
        }

        Map<String, Object> itemMap = getItem(position);
        if (itemMap != null) {
            mDataLock.lock();
            itemMap.put(THUMB_IMAGE, null);
            mDataLock.unlock();
        }
        removeCachedPage(pageNum); // page-indexed

//        notifyItemChanged(position); // notifyItemChanged() does not update image correctly
        Utils.safeNotifyDataSetChanged(this);
    }

    private Rect getPDFPageRect(int pageNum) throws PDFNetException {
        final PDFDoc doc = mPdfViewCtrl.getDoc();
        if (doc == null) {
            return new Rect(0, 0, 0, 0);
        }
        Page page = doc.getPage(pageNum);
        double width = page.getPageWidth();
        double height = page.getPageHeight();
        return new Rect(0, 0, width, height);
    }

    private void updateUserBookmarks(int from, int to, Long pageToMoveObjNum, Long destPageObjNum) {
        try {
            final PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null) {
                return;
            }
            String filepath = doc.getFileName();
            ToolManager toolManager;
            try {
                toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            } catch (Exception e) {
                toolManager = null;
            }
            if (toolManager != null && !toolManager.isReadOnly()) {
                BookmarkManager.onPageMoved(mPdfViewCtrl, pageToMoveObjNum, destPageObjNum, to, false);
            } else {
                BookmarkManager.onPageMoved(mContext, filepath, pageToMoveObjNum, destPageObjNum, from, to);
            }
        } catch (PDFNetException ignored) {
        }
    }

    private void removeUserBookmarks(Long objNum, int pageNum, int pageCount) {
        try {
            final PDFDoc doc = mPdfViewCtrl.getDoc();
            if (doc == null) {
                return;
            }
            String filepath = doc.getFileName();
            ToolManager toolManager;
            try {
                toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            } catch (Exception e) {
                toolManager = null;
            }
            if (toolManager != null && !toolManager.isReadOnly()) {
                BookmarkManager.onPageDeleted(mPdfViewCtrl, objNum);
            } else {
                BookmarkManager.onPageDeleted(mContext, filepath, objNum, pageNum, pageCount);
            }
        } catch (PDFNetException ignored) {
        }
    }

    // This class loads processes bitmaps off the UI thread
    private class LoadThumbnailTask extends AsyncTask<Void, Void, Bitmap> {

        private final PageViewHolder mHolder;
        private final int mPosition;
        private final int mPage;

        private int mWidth;
        private int mHeight;
        private int[] mBuffer;

        LoadThumbnailTask(PageViewHolder holder, int page, int[] buffer, int width, int height) {
            this.mHolder = holder;
            this.mPage = page;
            this.mPosition = page - 1;
            this.mBuffer = buffer;
            this.mWidth = width;
            this.mHeight = height;
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap bitmap = null;

            // wait if work is paused and the task is not cancelled
            synchronized (mPauseWorkLock) {
                while (mPauseWork && !isCancelled()) {
                    try {
                        if (sDebug)
                            Log.d(TAG, "doInBackground - paused for page: " + Integer.toString(mPage));
                        mPauseWorkLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                if (mBuffer != null && mBuffer.length > 0) {
                    ImageMemoryCache imageMemoryCache = ImageMemoryCache.getInstance();
                    bitmap = imageMemoryCache.getBitmapFromReusableSet(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                    if (bitmap == null) {
                        bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                    }
                    bitmap.setPixels(mBuffer, 0, mWidth, 0, 0, mWidth, mHeight);
                    if (mHolder != null) {
                        bitmap = scaleBitmap(mHolder, bitmap);
                    }
                    if (sDebug)
                        Log.d(TAG, "doInBackground - finished work for page: " + Integer.toString(mPage));
                } else {
                    if (sDebug)
                        Log.d(TAG, "doInBackground - Buffer is empty for page: " + Integer.toString(mPage));
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (OutOfMemoryError oom) {
                Utils.manageOOM(mPdfViewCtrl);
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (sDebug) Log.d(TAG, "onPostExecute " + Integer.toString(mPage));
            if (isCancelled()) {
                if (sDebug) Log.d(TAG, "onPostExecute cancelled");
                mTaskList.remove(mPage);
                return;
            }

            if (result != null) {
                Map<String, Object> itemMap = getItem(mPosition);
                if (itemMap != null) {
                    mDataLock.lock();
                    itemMap.put(THUMB_IMAGE, result);
                    mDataLock.unlock();
                    mCachedPageList.add(mPage);

                    boolean handled = false;
                    if (mHolder != null) {
                        if (mPosition == mHolder.getAdapterPosition()) {
                            if (sDebug)
                                Log.d(TAG, "onPostExecute - mPosition == mHolder.position for page: " + mPage);
                            setThumbnailBitmap(mHolder, result);
                            handled = true;
                        }
                    }
                    if (!handled) {
                        if (sDebug)
                            Log.d(TAG, "onPostExecute - mPosition != mHolder.position for page: " + mPage);
                        Utils.safeNotifyItemChanged(ThumbnailsViewAdapter.this, mPosition);
                    }
                }
            }
            mTaskList.remove(mPage);
        }

        @Override
        protected void onCancelled(Bitmap value) {
            if (sDebug) Log.d(TAG, "onCancelled " + Integer.toString(mPage));
            synchronized (mPauseWorkLock) {
                mPauseWorkLock.notifyAll();
            }
            mTaskList.remove(mPage);
        }
    }

    /**
     * Scale bitmap to fit inside parent view.
     * See: https://argillander.wordpress.com/2011/11/24/scale-image-into-imageview-then-resize-imageview-to-match-the-image/
     */
    private Bitmap scaleBitmap(PageViewHolder holder, Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        // Get bitmap dimensions
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        ViewGroup.LayoutParams parentParams = holder.imageLayout.getLayoutParams();
        // Determine how much to scale: the dimension requiring less scaling is
        // closer to the its side. This way the image always stays inside the
        // bounding box AND either x/y axis touches it.
        float xScale = ((float) parentParams.width) / bitmapWidth;
        float yScale = ((float) parentParams.height) / bitmapHeight;
        float scale = (xScale <= yScale) ? xScale : yScale;

        // Create a matrix for the scaling (same scale amount in both directions)
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        // Create a new bitmap that is scaled to the bounding box
        Bitmap scaledBitmap = null;
        try {
            scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, matrix, true);
        } catch (OutOfMemoryError oom) {
            Utils.manageOOM(mPdfViewCtrl);
        }

        if (sDebug) Log.d(TAG, "scaleBitmap recycle");
        ImageMemoryCache.getInstance().addBitmapToReusableSet(bitmap);
        return scaledBitmap;
    }

    private int getPositionForPage(final int pageNum) {
        if (mDataList != null) {
            ListIterator<Map<String, Object>> it = mDataList.listIterator();
            while (it.hasNext()) {
                Map<String, Object> item = it.next();
                try {
                    int page = (Integer) item.get(PAGE_NUMBER_SRC);
                    if (page == pageNum) {
                        return it.previousIndex();
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return -1;
    }

    /**
     * Updates the page numbers after deleting a page
     *
     * @param deletedPage The page number of the page deleted
     * @return The deleted position
     */
    public int updatePageNumberOnDelete(final int deletedPage) {
        int deletedPosition = -1;

        // Update current page number
        if (deletedPage == mCurrentPage) {
            int pageCount;
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                final PDFDoc doc = mPdfViewCtrl.getDoc();
                if (doc == null) {
                    return deletedPosition;
                }
                pageCount = doc.getPageCount();
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
                return deletedPosition;
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
            if (deletedPage >= pageCount) {
                mCurrentPage--;
            }
        } else if (mCurrentPage > deletedPage) {
            mCurrentPage--;
        }

        // Update page numbers
        ListIterator<Map<String, Object>> it = mDataList.listIterator();
        while (it.hasNext()) {
            Map<String, Object> item = it.next();
            int page = (Integer) item.get(PAGE_NUMBER_SRC);
            try {
                if (page > deletedPage) {
                    item.put(PAGE_NUMBER_SRC, page - 1);
                } else if (page == deletedPage) {
                    mDataLock.lock();
                    item.put(THUMB_IMAGE, null);
                    mDataLock.unlock();

                    deletedPosition = it.previousIndex();
                    it.remove();
                }
            } catch (Exception ignored) {
            }
        }
        // Update cached page list
        removeCachedPage(deletedPage);

        return deletedPosition;
    }

    private void removeCachedPage(int pageNum) {
        if (mCachedPageList != null) {
            Iterator<Integer> cachedIt = mCachedPageList.iterator();
            while (cachedIt.hasNext()) {
                int cachedPage = cachedIt.next();
                if (cachedPage == pageNum) {
                    cachedIt.remove();
                    break;
                }
            }
        }
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {

        RelativeLayout imageLayout;
        ImageView thumbImage;
        TextView pageNumber;

        PageViewHolder(View itemView) {
            super(itemView);

            this.imageLayout = itemView.findViewById(R.id.item_image_layout);
            this.thumbImage = itemView.findViewById(R.id.item_image);
            this.pageNumber = itemView.findViewById(R.id.item_text);
        }
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }
}
