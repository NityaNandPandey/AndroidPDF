//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.graphics.Path;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * Path Pool Pattern to avoid unnecessary memory allocation.
 */
public class PathPool {

    public final static int length = 1 << 12;

    private final Object mLock = new Object();
    private Path[] mPool = new Path[length];
    private int mSize;

    private static class LazyHolder {
        private static final PathPool INSTANCE = new PathPool();
    }

    public static PathPool getInstance() {
        return PathPool.LazyHolder.INSTANCE;
    }

    private PathPool() {

    }

    /**
     * @return a path
     */
    public Path obtain() {
        synchronized (mLock) {
            if (mSize > 0) {
                final int index = mSize - 1;
                Path path = mPool[index];
                mPool[index] = null;
                --mSize;
                if (path == null) {
                    path = new Path();
                }
                return path;
            }
        }
        return new Path();
    }

    /**
     * Recycles a path to the pool
     *
     * @param path The path
     */
    public void recycle(Path path) {
        // NOTE: we don't check if the path is already in the pool, so it is the caller's
        // responsibility to ensure this is not the case

        if (path == null) {
            return;
        }
        path.reset();

        synchronized (mLock) {
            if (mPool == null) {
                mPool = new Path[length];
            }
            if (mSize < length) {
                mPool[mSize] = path;
                ++mSize;
            }
        }
    }

    /**
     * Recycles the paths to the pool
     *
     * @param paths A list of paths to be recycled
     */
    public void recycle(@NonNull List<Path> paths) {
        int count = length - mSize;
        if (count > 0) {
            if (paths.size() < count) {
                count = paths.size();
            }
            for (int i = 0; i < count; ++i) {
                recycle(paths.get(i));
            }
        }
    }

    /**
     * Recycles a specified number of paths to the pool
     *
     * @param count The number of path to be recycled
     */
    public void recycle(int count) {
        if (count > length - mSize) {
            count = length - mSize;
        }
        for (int i = 0; i < count; ++i) {
            recycle(new Path());
        }
    }

    /**
     * Clears the pool
     */
    public void clear() {
        synchronized (mLock) {
            mPool = null;
            mSize = 0;
        }
    }
}
