//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.graphics.PointF;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * PointF Pool Pattern to avoid unnecessary memory allocation.
 */
public class PointFPool {

    public final static int length = 1 << 12;

    private final Object mLock = new Object();
    private PointF[] mPool = new PointF[length];
    private int mSize;

    private static class LazyHolder {
        private static final PointFPool INSTANCE = new PointFPool();
    }

    public static PointFPool getInstance() {
        return PointFPool.LazyHolder.INSTANCE;
    }

    /**
     * @return a PointF
     */
    public PointF obtain() {
        return obtain(0f, 0f);
    }

    /**
     * @return a PointF
     */
    public PointF obtain(float x, float y) {
        synchronized (mLock) {
            if (mSize > 0) {
                final int index = mSize - 1;
                PointF point = mPool[index];
                mPool[index] = null;
                --mSize;
                if (point == null) {
                    point = new PointF();
                }
                point.set(x, y);
                return point;
            }
        }
        return new PointF(x, y);
    }

    /**
     * Recycles a PointF to the pool
     *
     * @param point The PointF
     */
    public void recycle(PointF point) {
        // NOTE: we don't check if the point is already in the pool, so it is the caller's
        // responsibility to ensure this is not the case

        if (point == null) {
            return;
        }

        synchronized (mLock) {
            if (mPool == null) {
                mPool = new PointF[length];
            }
            if (mSize < length) {
                mPool[mSize] = point;
                ++mSize;
            }
        }
    }

    /**
     * Recycles points to the pool
     *
     * @param points A list of points to be recycled
     */
    public void recycle(@NonNull List<PointF> points) {
        int count = length - mSize;
        if (count > 0) {
            if (points.size() < count) {
                count = points.size();
            }
            for (int i = 0; i < count; ++i) {
                recycle(points.get(i));
            }
        }
    }

    /**
     * Recycles a specified number of points to the pool
     *
     * @param count The number of points to be recycled
     */
    public void recycle(int count) {
        if (count > length - mSize) {
            count = length - mSize;
        }
        for (int i = 0; i < count; ++i) {
            recycle(new PointF());
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
