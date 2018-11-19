//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.SparseArray;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.pdftron.demo.widget.ImageViewTopCrop;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.DocumentPreviewCache;
import com.pdftron.pdf.PreviewHandler;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import android.os.AsyncTask;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for easy bitmap handling when using {@link DocumentPreviewCache}.
 * <a target="_blank" href="https://github.com/facebook/fresco">fresco</a> is used for managing bitmap memory.
 */
public class ThumbnailWorker implements
        PreviewHandler.PreviewHandlerCallback {

    private static final String TAG = ThumbnailWorker.class.getName();

    private static final int MODE_FILE_PATH = 0;
    private static final int MODE_UUID = 1;

    private static final String CUSTOM_DATA_POSITION = "custom_data_position";
    private static final String CUSTOM_DATA_IDENTIFIER = "custom_data_identifier";
    private static final String CUSTOM_DATA_CUSTOM_FILTER = "custom_data_custom_filter";

    private Context mContext;
    private PreviewHandler mPreviewHandler;
    private Bitmap mLoadingBitmap;
    private SparseArray<String> mRequestedThumbs;
    private final Object mRequestedThumbsLock = new Object();
    private CancelRequestTask mCancelRequestTask;

    // Min x and y dimensions of thumbnail
    private int mMinXSize;
    private int mMinYSize;

    public ThumbnailWorkerListener mListener;

    public interface ThumbnailWorkerListener {
        void onThumbnailReady(int result, int position, String iconPath, String identifier);
    }

    public ThumbnailWorker(Context context, int min_x_size, int min_y_size, Bitmap loading_bitmap) {
        mContext = context.getApplicationContext();
        mMinXSize = min_x_size;
        mMinYSize = min_y_size;
        mLoadingBitmap = loading_bitmap;
        mRequestedThumbs = new SparseArray<>();
    }

    public void setListener(ThumbnailWorkerListener listener) {
        mListener = listener;
    }

    public void cleanupResources() {
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        imagePipeline.clearCaches();
    }

    public void evictFromMemoryCache(String uuid) {
        if (null == uuid) {
            return;
        }
        String iconPath = ThumbnailPathCacheManager.getInstance().getThumbnailPath(uuid, mMinXSize, mMinYSize);
        if (null == iconPath) {
            return;
        }
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        imagePipeline.evictFromCache(Uri.parse(iconPath));
        ThumbnailPathCacheManager.getInstance().removeThumbnailPath(uuid, mMinXSize, mMinYSize);
    }

    public void setMinXSize(int minXSize) {
        mMinXSize = minXSize;
    }

    public int getMinXSize() {
        return mMinXSize;
    }

    public void setMinYSize(int minYSize) {
        mMinYSize = minYSize;
    }

    public int getMinYSize() {
        return mMinYSize;
    }

    public void setLoadingBitmap(Bitmap loadingBitmap) {
        mLoadingBitmap = loadingBitmap;
    }

    @Override
    public void PreviewHandlerProc(int result, String iconPath, Object data) {
        // Alert listener that thumbnail is ready

        String identifier;
        int position;
        try {
            if (!(data instanceof Map)) {
                return;
            }
            iconPath =  ThumbnailPathCacheManager.getThumbnailWithSize(mContext, iconPath, mMinXSize, mMinYSize);
            Map customData = (Map) data;
            identifier = (String) customData.get(CUSTOM_DATA_IDENTIFIER);
            position = (int) customData.get(CUSTOM_DATA_POSITION);
            Object filter = customData.get(CUSTOM_DATA_CUSTOM_FILTER);
            if (filter != null && filter instanceof SecondaryFileFilter) {
                ((SecondaryFileFilter)filter).close();
            }
        } catch (Exception ignored) {
            return;
        }

        Logger.INSTANCE.LogD(TAG, "PreviewHandlerProc: " + result + " ==> " + identifier + " ==> " + iconPath);

        synchronized (mRequestedThumbsLock) {
            mRequestedThumbs.remove(position);
        }

        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_FAILURE ||
                result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_CANCEL ||
                result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PREVIOUS_CRASH ||
                result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_POSTPONED) {
            return;
        }

        if (mListener != null) {
            mListener.onThumbnailReady(result, position, iconPath, identifier);
        }
    }

    private boolean tryLoadImageFromCache(final String filePath, final String iconPath, final ImageViewTopCrop imageView) {
        if (Utils.isNullOrEmpty(iconPath)) {
            return true;
        }

        boolean needsRequest = true;
        File iconFile = new File(iconPath);
        if (iconFile.exists()) {
            needsRequest = false;
            try {
                imageView.setImageURI(Uri.fromFile(iconFile));
            } catch (Exception e) {
                imageView.setImageBitmap(mLoadingBitmap);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        } else {
            // file no longer exists, remove from cache map
            Logger.INSTANCE.LogD(TAG, "file no longer exists, remove from cache map");
            ThumbnailPathCacheManager.getInstance().removeThumbnailPath(filePath, mMinXSize, mMinYSize);
        }
        return needsRequest;
    }

    public void tryLoadImageWithPath(int position, String filePath, String iconPath, ImageViewTopCrop imageView) {
        // TODO: temp hack to not generate thumbnail for .doc file until the bug is fixed in core side
        if (Utils.isDoNotRequestThumbFile(filePath)) {
            return;
        }
        tryLoadImage(position, filePath, iconPath, imageView, MODE_FILE_PATH);
    }

    public void tryLoadImageWithUuid(int position, String identifier, String iconPath, ImageViewTopCrop imageView) {
        // TODO: temp hack to not generate thumbnail for .doc file until the bug is fixed in core side
        ContentResolver contentResolver = Utils.getContentResolver(mContext);
        if (contentResolver == null || Utils.isDoNotRequestThumbFile(contentResolver, identifier)) {
            return;
        }
        tryLoadImage(position, identifier, iconPath, imageView, MODE_UUID);
    }

    private void tryLoadImage(final int position, final String identifier, String iconPath, ImageViewTopCrop imageView, int mode) {
        if (Utils.isNullOrEmpty(iconPath)) {
            iconPath = ThumbnailPathCacheManager.getInstance().getThumbnailPath(identifier, mMinXSize, mMinYSize);
        }
        boolean needsRequest = tryLoadImageFromCache(identifier, iconPath, imageView);

        if (needsRequest) {
            imageView.setImageBitmap(mLoadingBitmap);
            // Bitmap not in memory or disk
            // check if a request is already sent
            synchronized (mRequestedThumbsLock) {
                if (mRequestedThumbs.get(position) != null) {
                    return; // Already requested
                }
            }
            if (position >= 0) {
                Map<String, Object> customData = new HashMap<>();
                customData.put(CUSTOM_DATA_IDENTIFIER, identifier);
                customData.put(CUSTOM_DATA_POSITION, position);
                if (mPreviewHandler == null) {
                    mPreviewHandler = new PreviewHandler(this);
                }
                if (mode == MODE_FILE_PATH) {
                    DocumentPreviewCache.getBitmapWithPath(identifier, mMinXSize, mMinYSize, mPreviewHandler, customData);
                    Logger.INSTANCE.LogD(TAG, "getBitmapWithPath: " + identifier);
                } else {
                    try {
                        SecondaryFileFilter filter = new SecondaryFileFilter(mContext, Uri.parse(identifier));
                        // important note: take the ownership of parcel/filter to the core and close it in callback
                        customData.put(CUSTOM_DATA_CUSTOM_FILTER, filter);
                        DocumentPreviewCache.getBitmapWithID(identifier, filter, mMinXSize, mMinYSize, mPreviewHandler, customData);
                        Logger.INSTANCE.LogD(TAG, "getBitmapWithID: " + identifier);
                    } catch (Exception e) {
                        DocumentPreviewCache.getBitmapWithID(identifier, mMinXSize, mMinYSize, mPreviewHandler, customData);
                        Logger.INSTANCE.LogD(TAG, "getBitmapWithID: " + identifier);
                    }
                }
                synchronized (mRequestedThumbsLock) {
                    mRequestedThumbs.put(position, identifier);
                }
            }
        }
    }

    public void tryLoadImageFromFilter(int position, String identifier, String uriStr) {
        Logger.INSTANCE.LogD(TAG, "position: " + position + " tryLoadImageFromFilter");

        if (mRequestedThumbs.get(position) != null) {
            return;
        }

        SecondaryFileFilter filter = null;
        try {
            Uri uri = Uri.parse(uriStr);
            filter = new SecondaryFileFilter(mContext, uri);

            Map<String, Object> customData = new HashMap<>();
            customData.put(CUSTOM_DATA_IDENTIFIER, identifier);
            customData.put(CUSTOM_DATA_POSITION, position);

            if (mPreviewHandler == null) {
                mPreviewHandler = new PreviewHandler(this);
            }
            DocumentPreviewCache.createBitmapWithID(identifier, filter, mMinXSize, mMinYSize, mPreviewHandler, customData);

            synchronized (mRequestedThumbsLock) {
                mRequestedThumbs.put(position, identifier);
            }
        } catch (FileNotFoundException ignored) {

        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(filter);
        }
    }

    private class CancelRequestTask extends CustomAsyncTask<Void, Void, SparseArray<String>> {

        final private SparseArray<String> mFiles;
        private SparseArray<String> mCancelledFiles;

        CancelRequestTask(Context context, SparseArray<String> filePathList) {
            super(context);
            mFiles = filePathList;
            mCancelledFiles = new SparseArray<>();
        }

        @Override
        protected SparseArray<String> doInBackground(Void... params) {

            for (int i = 0; i < mFiles.size(); i++) {
                if (isCancelled()) {
                    break;
                }

                int key = mFiles.keyAt(i);
                String value = mFiles.valueAt(i);
                DocumentPreviewCache.cancelRequest(value);
                mCancelledFiles.put(key, value);
            }

            return mCancelledFiles;
        }

        @Override
        protected void onPostExecute(SparseArray<String> strings) {
            if (strings == null || isCancelled()) {
                return;
            }

            for (int i = 0; i < strings.size(); i++) {
                // remove the successfully cancelled ones from request list
                int key = strings.keyAt(i);
                synchronized (mRequestedThumbsLock) {
                    mRequestedThumbs.remove(key);
                }
            }
        }

        @Override
        protected void onCancelled(SparseArray<String> strings) {
            if (strings == null) {
                return;
            }

            for (int i = 0; i < strings.size(); i++) {
                // remove the successfully cancelled ones from request list
                int key = strings.keyAt(i);
                synchronized (mRequestedThumbsLock) {
                    mRequestedThumbs.remove(key);
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public void cancelThumbRequests(int firstPosition, int lastPosition) {
        synchronized (mRequestedThumbsLock) {
            SparseArray<String> cancelList = new SparseArray<>();
            for (int i = 0; i < mRequestedThumbs.size(); i++) {
                int position = mRequestedThumbs.keyAt(i);
                String filePath = mRequestedThumbs.valueAt(i);

                if (position < firstPosition || position > lastPosition) {
                    // not in range: cancel loading of thumbnail
                    cancelList.put(position, filePath);
                }
            }
            if (cancelList.size() > 0) {
                abortCancelTask();
                mCancelRequestTask = new CancelRequestTask(mContext, cancelList);
                // start new task
                mCancelRequestTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR );
            }
        }
    }

    public void abortCancelTask() {
        if (mCancelRequestTask != null) {
            mCancelRequestTask.cancel(true);
            mCancelRequestTask = null;
        }
    }

    public void cancelAllThumbRequests() {
        synchronized (mRequestedThumbsLock) {
            mRequestedThumbs.clear();
        }
        DocumentPreviewCache.cancelAllRequests();
    }

    public void removePreviewHandler() {
        if (mPreviewHandler != null) {
            mPreviewHandler.removeListener();
            mPreviewHandler = null;
        }
    }
}
