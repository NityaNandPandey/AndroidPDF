package com.pdftron.demo.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.LruCache;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;
import android.os.AsyncTask;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

/**
 * A utility class used for {@link ThumbnailWorker}.
 */
public class ThumbnailPathCacheManager {

    private static final int DEFAULT_MEM_CACHE_SIZE = 1024;
    private final static String THUMBNAIL_TEMP_FOLDER = "ThumbCache";

    private ThumbLruCache mMemoryCache;

    private static class LazyHolder {
        private static final ThumbnailPathCacheManager INSTANCE = new ThumbnailPathCacheManager();
    }

    public static ThumbnailPathCacheManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private ThumbnailPathCacheManager() {
        mMemoryCache = new ThumbLruCache(DEFAULT_MEM_CACHE_SIZE);
    }

    public String getThumbnailPath(String filepath, int x, int y) {
        if (mMemoryCache == null) {
            return "";
        }
        return mMemoryCache.get(getFilepathWithSize(filepath, x, y));
    }

    public void putThumbnailPath(String filepath, String thumbnailPath, int x, int y) {
        if (mMemoryCache != null && !Utils.isNullOrEmpty(filepath) && !Utils.isNullOrEmpty(thumbnailPath)) {
            mMemoryCache.put(getFilepathWithSize(filepath, x, y), thumbnailPath);
        }
    }

    public void removeThumbnailPath(String filepath, int x, int y) {
        if (mMemoryCache != null && !Utils.isNullOrEmpty(filepath)) {
            mMemoryCache.remove(getFilepathWithSize(filepath, x, y));
        }
    }

    // remove all thumbnails related to this file path
    public void removeThumbnailPath(final String filepath) {
        new AsyncTask<Void, Void, ArrayList<String>>() {

            Map mSnapshotMap;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (mMemoryCache != null && !Utils.isNullOrEmpty(filepath)) {
                    mSnapshotMap = mMemoryCache.snapshot();
                }
            }

            @Override
            protected ArrayList<String> doInBackground(Void... params) {
                if (mSnapshotMap == null) {
                    return null;
                }
                ArrayList<String> filepathToRemove = new ArrayList<>();
                String path = FilenameUtils.getPath(filepath) + FilenameUtils.getBaseName(filepath) + "_";
                for (Object entry : mSnapshotMap.entrySet()) {
                    String key = (String) ((Map.Entry) entry).getKey();
                    if (key.startsWith(path)) {
                        filepathToRemove.add(key);
                    }
                }
                return filepathToRemove;
            }

            @Override
            protected void onPostExecute(ArrayList<String> filepathToRemove) {
                super.onPostExecute(filepathToRemove);
                if (mMemoryCache != null && filepathToRemove != null) {
                    for (String key : filepathToRemove) {
                        mMemoryCache.remove(key);
                    }
                }
            }
        }.executeOnExecutor(CustomAsyncTask.THREAD_POOL_EXECUTOR );
    }

    synchronized public void cleanupResources(Context context) {
        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
        }
        if (context != null) {
            clearThumbCache(context);
        }
        if (Fresco.hasBeenInitialized()) {
            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            imagePipeline.clearCaches();
        }
    }

    private static void clearThumbCache(@NonNull Context context) {
        File thumbCacheFolder = new File(context.getFilesDir() + File.separator + THUMBNAIL_TEMP_FOLDER);
        if (thumbCacheFolder.exists() && thumbCacheFolder.isDirectory()) {
            for (File child : thumbCacheFolder.listFiles()) {
                //noinspection ResultOfMethodCallIgnored
                child.delete();
            }
        }
    }

    private static String getFilepathWithSize(String filepath, int x, int y) {
        String path = FilenameUtils.getPath(filepath);
        String filename = FilenameUtils.getName(filepath);
        String extension = FilenameUtils.getExtension(filename);
        String title = FilenameUtils.removeExtension(filename);
        return path + title + "_" + x + "_" + y + "." + extension;
    }

    public static String getThumbnailWithSize(Context context, String thumbPath, int x, int y) {
        if (context == null) {
            return thumbPath;
        }
        File thumbFile = new File(thumbPath);
        if (thumbFile.exists()) {
            String filename = FilenameUtils.getName(thumbPath);
            String extension = FilenameUtils.getExtension(filename);
            String title = FilenameUtils.removeExtension(filename);
            filename = title + "_" + x + "_" + y + "." + extension;
            File thumbCacheFolder = new File(context.getFilesDir() + File.separator + THUMBNAIL_TEMP_FOLDER);
            File resultFile = new File(thumbCacheFolder, filename);
            try {
                FileUtils.copyFile(thumbFile, resultFile);
                return resultFile.getAbsolutePath();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return thumbPath;
    }

    private class ThumbLruCache extends LruCache<String, String> {

        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        ThumbLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, String oldValue, String newValue) {
            super.entryRemoved(evicted, key, oldValue, newValue);
            try {
                File file = new File(oldValue);
                if (file.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            } catch (Exception ignored) {

            }
        }
    }
}
