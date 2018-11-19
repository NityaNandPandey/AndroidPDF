//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.util.Log;

import java.io.FileDescriptor;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This class handles image memory caching to avoid allocating/de-allocating bitmaps by reusing them
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ImageMemoryCache {

    private static final String TAG = ImageMemoryCache.class.getName();
    private static boolean sDebug;

    private static final int DEFAULT_MEM_CACHE_SIZE = 4 * 1024; // 4 MB

    private final HashMap<Key, List<SoftReference<Bitmap>>> mReusableBitmaps = new HashMap<>(16);

    private boolean mActive = true;
    private LruCache<String, BitmapDrawable> mMemCache;

    private static class LazyHolder {
        private static final ImageMemoryCache INSTANCE = new ImageMemoryCache();
    }

    public static ImageMemoryCache getInstance() {
        return ImageMemoryCache.LazyHolder.INSTANCE;
    }

    private ImageMemoryCache() {
        init(DEFAULT_MEM_CACHE_SIZE);
    }

    /**
     * Sets the memory cache size based on a percentage of the max available VM memory.
     * Eg. setting percent to 0.2 would set the memory cache to one fifth of the available
     * memory. Throws {@link IllegalArgumentException} if percent is < 0.01 or > .8.
     * memory cache size is stored in kilobytes instead of bytes as this will eventually be passed
     * to construct a LruCache which takes an int in its constructor.
     *
     * @param percent Percent of available app memory to use to size memory cache
     */
    public void setMemCacheSizePercent(float percent) {
        if (percent < 0.01f || percent > 0.8f) {
            throw new IllegalArgumentException("setMemCacheSizePercent - percent must be "
                + "between 0.01 and 0.8 (inclusive)");
        }
        init(Math.round(percent * Runtime.getRuntime().maxMemory() / 1024));
    }

    /**
     * Sets the memory cache size.
     *
     * @param memCacheSize The memory cache size
     */
    public void setMemCacheSize(int memCacheSize) {
        init(memCacheSize);
    }

    private void init(int memCacheSize) {
        mMemCache = new LruCache<String, BitmapDrawable>(memCacheSize) {
            @Override
            protected void entryRemoved(boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue) {
                addBitmapToReusableSet(oldValue.getBitmap());
            }

            @Override
            protected int sizeOf(String key, BitmapDrawable value) {
                Bitmap bitmap = value.getBitmap();
                if (bitmap == null || bitmap.isRecycled()) {
                    return 1;
                }

                // Return bitmap byte count, in kilobytes
                int bitmapSize = getBitmapSize(value) / 1024;
                return bitmapSize == 0 ? 1 : bitmapSize;
            }
        };
    }

    /**
     * Returns bitmap having the specified key from cache
     *
     * @param key The bitmap key
     *
     * @return The bitmap
     */
    public BitmapDrawable getBitmapFromCache(String key) {
        return mMemCache.get(key);
    }

    /**
     * Returns a bitmap with specified configuration from the reusable set
     *
     * @param width The expected width
     * @param height The expected height
     * @param config The bitmap configuration
     *
     * @return The bitmap
     */
    public Bitmap getBitmapFromReusableSet(int width,
                                           int height,
                                           Bitmap.Config config) {
        if (!mActive || width <= 0 || height <= 0 || mReusableBitmaps.isEmpty()) {
            return null;
        }

        Key key = new Key(width, height);
        synchronized (mReusableBitmaps) {
            List<SoftReference<Bitmap>> bitmaps = mReusableBitmaps.get(key);
            if (bitmaps == null || bitmaps.isEmpty()) {
                return null;
            }

            final Iterator<SoftReference<Bitmap>> iterator = bitmaps.iterator();
            Bitmap item;

            while (iterator.hasNext()) {
                item = iterator.next().get();
                // check to see if the item can be reused
                if (item != null && item.isMutable()) {
                    if (config == item.getConfig()) {
                        // remove from reusable set so it can't be used again
                        iterator.remove();
                        if (sDebug)
                            Log.v(TAG, "a bitmap can be reused with width " + item.getWidth() + " and height " + item.getHeight());
                        return item;
                    }
                } else {
                    // remove from the set if the reference has been cleared.
                    iterator.remove();
                }
            }
        }

        return null;
    }

    private Bitmap getInBitmapFromReusableSet(BitmapFactory.Options targetOptions) {
        if (!mActive || mReusableBitmaps.isEmpty()) {
            return null;
        }

        Bitmap bitmap = null;
        Bitmap item;
        synchronized (mReusableBitmaps) {
            if (targetOptions.inSampleSize == 1) {
                bitmap = getBitmapFromReusableSet(targetOptions.outWidth, targetOptions.outHeight, targetOptions.inPreferredConfig);
                if (bitmap != null) {
                    return bitmap;
                }
            }

            for (Key key : mReusableBitmaps.keySet()) {
                List<SoftReference<Bitmap>> bitmaps = mReusableBitmaps.get(key);
                if (bitmaps == null || bitmaps.isEmpty()) {
                    continue;
                }

                final Iterator<SoftReference<Bitmap>> iterator = bitmaps.iterator();
                while (iterator.hasNext()) {
                    item = iterator.next().get();

                    if (item != null && item.isMutable()) {
                        // Check to see if the item can be used for inBitmap
                        if (canUseForInBitmap(item, targetOptions)) {
                            bitmap = item;

                            // Remove from reusable set so it can't be used again
                            iterator.remove();
                            break;
                        } else {
                            // assume the rest of bitmaps in this set has the same configuration
                            break;
                        }
                    } else {
                        // Remove from the set if the reference has been cleared.
                        iterator.remove();
                    }
                }
            }
        }

        return bitmap;
    }

    /**
     * Decode and sample down a bitmap from resources to the requested width and height.
     *
     * @param res       The resources object containing the image data
     * @param resId     The resource id of the image data
     * @param downSampleFactor  The down-sampling factor, 1.0 for no down-sampling
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     * that are equal to or greater than the requested width and height
     */
    @SuppressWarnings("SameParameterValue")
    public Bitmap decodeSampledBitmapFromResource(Resources res,
                                                  int resId,
                                                  float downSampleFactor) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        int height = options.outHeight;
        int width = options.outWidth;
        return decodeSampledBitmapFromResource(res, resId, (int) (height * downSampleFactor), (int) (width * downSampleFactor));
    }

    /**
     * Decode and sample down a bitmap from resources to the requested width and height.
     *
     * @param res       The resources object containing the image data
     * @param resId     The resource id of the image data
     * @param reqWidth  The requested width of the resulting bitmap, 0 for no down-sampling
     * @param reqHeight The requested height of the resulting bitmap, 0 for no down-sampling
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     * that are equal to or greater than the requested width and height
     */
    public Bitmap decodeSampledBitmapFromResource(Resources res,
                                                  int resId,
                                                  int reqWidth,
                                                  int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        if (reqWidth != 0 && reqHeight != 0) {
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        }

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        addInBitmapOptions(options);

        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * Decode and sample down a bitmap from a file to the requested width and height.
     *
     * @param filename  The full path of the file to decode
     * @param reqWidth  The requested width of the resulting bitmap, 0 for no down-sampling
     * @param reqHeight The requested height of the resulting bitmap, 0 for no down-sampling
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     * that are equal to or greater than the requested width and height
     */
    public Bitmap decodeSampledBitmapFromFile(String filename,
                                              int reqWidth,
                                              int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filename, options);

        // Calculate inSampleSize
        if (reqWidth != 0 && reqHeight != 0) {
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        }

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        addInBitmapOptions(options);

        return BitmapFactory.decodeFile(filename, options);
    }

    /**
     * Decode and sample down a bitmap from a file input stream to the requested width and height.
     *
     * @param fileDescriptor The file descriptor to read from
     * @param reqWidth       The requested width of the resulting bitmap
     * @param reqHeight      The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     * that are equal to or greater than the requested width and height
     */
    public Bitmap decodeSampledBitmapFromDescriptor(FileDescriptor fileDescriptor,
                                                    int reqWidth,
                                                    int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        addInBitmapOptions(options);

        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
    }

    private void addInBitmapOptions(BitmapFactory.Options options) {
        // inBitmap only works with mutable bitmaps so force the decoder to
        // return mutable bitmaps.
        options.inMutable = true;

        // Try and find a bitmap to use for inBitmap
        Bitmap inBitmap = getInBitmapFromReusableSet(options);

        if (inBitmap != null) {
            options.inBitmap = inBitmap;
        }
    }

    /**
     * Calculate an inSampleSize for use in a {@link android.graphics.BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link android.graphics.BitmapFactory}. This implementation calculates
     * the closest inSampleSize that is a power of 2 and will result in the final decoded bitmap
     * having a width and height equal to or larger than the requested width and height.
     *
     * @param options   An options object with out* params already populated (run through a decode*
     *                  method with inJustDecodeBounds==true
     * @param reqWidth  The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth,
                                             int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            long totalPixels = width * height / inSampleSize;

            // Anything more than 2x the requested pixels we'll sample down further
            final long totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels > totalReqPixelsCap) {
                inSampleSize *= 2;
                totalPixels /= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * @param candidate     - Bitmap to check
     * @param targetOptions - Options that have the out* value populated
     * @return true if <code>candidate</code> can be used for inBitmap re-use with
     * <code>targetOptions</code>
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static boolean canUseForInBitmap(
        Bitmap candidate, BitmapFactory.Options targetOptions) {
        if (!Utils.isKitKat()) {
            // On earlier versions, the dimensions must match exactly and the inSampleSize must be 1
            return candidate.getWidth() == targetOptions.outWidth
                && candidate.getHeight() == targetOptions.outHeight
                && targetOptions.inSampleSize == 1;
        }

        if (targetOptions.inSampleSize == 0) {
            return false;
        }

        // From Android 4.4 (KitKat) onward we can re-use if the byte size of the new bitmap
        // is smaller than the reusable bitmap candidate allocation byte count.
        int width = targetOptions.outWidth / targetOptions.inSampleSize;
        int height = targetOptions.outHeight / targetOptions.inSampleSize;
        int byteCount = width * height * getBytesPerPixel(candidate.getConfig());
        return byteCount <= candidate.getAllocationByteCount();
    }

    /**
     * Return the byte usage per pixel of a bitmap based on its configuration.
     *
     * @param config The bitmap configuration.
     * @return The byte usage per pixel.
     */
    private static int getBytesPerPixel(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        } else if (config == Bitmap.Config.RGB_565) {
            return 2;
        } else if (config == Bitmap.Config.ARGB_4444) {
            return 2;
        } else if (config == Bitmap.Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }

    /**
     * Adds a new image to the memory and disk caches
     *
     * @param key The key used to store the image
     * @param bitmap The bitmap to cache
     */
    public void addBitmapToCache(String key,
                                 BitmapDrawable bitmap) {
        if (key == null || bitmap == null) {
            return;
        }
        mMemCache.put(key, bitmap);
    }

    /**
     * Adds the specified bitmap to reusable set
     *
     * @param bitmap The bitmap
     */
    public void addBitmapToReusableSet(@Nullable Bitmap bitmap) {
        if (!mActive || bitmap == null) {
            return;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Key key = new Key(width, height);
        synchronized (mReusableBitmaps) {
            List<SoftReference<Bitmap>> bitmaps = mReusableBitmaps.get(key);
            if (bitmaps == null) {
                bitmaps = new ArrayList<>();
                mReusableBitmaps.put(key, bitmaps);
            }
            bitmaps.add(new SoftReference<>(bitmap));
        }
    }

    /**
     * Sets if the usage of the reusable set is allowed
     *
     * @param active True if the reusable set is active
     */
    void setReusableActive(boolean active) {
        mActive = active;
    }

    /**
     * @return True if the reusable set is active
     */
    boolean isReusableActive() {
        return mActive;
    }

    /**
     * Gets the size in bytes of a bitmap in a BitmapDrawable. Note that from Android 4.4 (KitKat)
     * onward this returns the allocated memory size of the bitmap which can be larger than the
     * actual bitmap data byte count (in the case it was re-used).
     *
     * @param value Bitmap Drawable
     * @return size in bytes
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static int getBitmapSize(BitmapDrawable value) {
        Bitmap bitmap = value.getBitmap();

        // From KitKat onward use getAllocationByteCount() as allocated bytes can potentially be
        // larger than bitmap byte count.
        if (Utils.isKitKat()) {
            return bitmap.getAllocationByteCount();
        }

        return bitmap.getByteCount();
    }

    /**
     * Clears all cached bitmaps including the reusable bitmap set.
     */
    public void clearAll() {
        clearReusableBitmaps();
        setReusableActive(false);
        clearCache();
        setReusableActive(true);
    }

    /**
     * Clears caches (but still keeps the weak reference to the reusable bitmaps).
     */
    public void clearCache() {
        mMemCache.evictAll();
    }

    void clearReusableBitmaps() {
        synchronized (mReusableBitmaps) {
            mReusableBitmaps.clear();
        }
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }

    private class Key {

        private final int x;
        private final int y;

        public Key(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof Key) {
                Key key = (Key) o;
                return x == key.x && y == key.y;
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = x;
            hash = hash * 31 + y;
            return hash;
        }
    }
}
