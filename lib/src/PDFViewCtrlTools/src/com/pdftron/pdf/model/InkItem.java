package com.pdftron.pdf.model;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

import java.util.ArrayList;

public class InkItem {
    public Paint mPaint;
    public int mColor;
    public float mOpacity;
    public float mThickness;

    // page pt
    public ArrayList<PointF> mPageStrokePoints;
    public ArrayList<ArrayList<PointF>> mPageStrokes;

    public ArrayList<ArrayList<PointF>> mDrawingStrokes;

    public ArrayList<Path> mPaths;

    public int mPageForFreehandAnnot;

    public boolean mIsFirstPath;
    public boolean mDirtyPaths;
    public boolean mDirtyDrawingPts;
    public boolean mStylusUsed;
    public boolean mCommittedAnnot;

    public InkItem(Paint paint, int color, float opacity, float thickness, boolean isStylus) {
        mPaint = new Paint(paint);
        mColor = color;
        mOpacity = opacity;
        mThickness = thickness;

        mPageStrokePoints = new ArrayList<>();
        mPageStrokes = new ArrayList<>();
        mDrawingStrokes = new ArrayList<>();
        mPaths = new ArrayList<>();

        mIsFirstPath = true;
        mPageForFreehandAnnot = -1;

        mDirtyPaths = true;
        mDirtyDrawingPts = true;
        mStylusUsed = isStylus;

        mCommittedAnnot = false;
    }

    public InkItem(Paint paint, float thickness, boolean isStylus) {
        this(paint, 0, 0, thickness, isStylus);
    }

    public InkItem(InkItem inkItem) {
        mPaint = new Paint(inkItem.mPaint);
        mColor = inkItem.mColor;
        mOpacity = inkItem.mOpacity;
        mThickness = inkItem.mThickness;

        // cloning
        mPageStrokePoints = new ArrayList<>(inkItem.mPageStrokePoints.size());
        for (PointF point : inkItem.mPageStrokePoints) {
            mPageStrokePoints.add(new PointF(point.x, point.y));
        }
        mPageStrokes = new ArrayList<>(inkItem.mPageStrokes.size());
        for (ArrayList<PointF> arrayList : inkItem.mPageStrokes) {
            ArrayList<PointF> temp = new ArrayList<>();
            for (PointF point : arrayList) {
                PointF copyPoint = new PointF(point.x, point.y);
                temp.add(copyPoint);
            }
            mPageStrokes.add(temp);
        }
        mDrawingStrokes = new ArrayList<>(inkItem.mDrawingStrokes.size());
        for (ArrayList<PointF> arrayList : inkItem.mDrawingStrokes) {
            ArrayList<PointF> temp = new ArrayList<>();
            for (PointF point : arrayList) {
                PointF copyPoint = new PointF(point.x, point.y);
                temp.add(copyPoint);
            }
            mDrawingStrokes.add(temp);
        }

        mPageForFreehandAnnot = inkItem.mPageForFreehandAnnot;

        mPaths = new ArrayList<>();
        for (Path path : inkItem.mPaths) {
            mPaths.add(new Path(path));
        }
        mDirtyPaths = true;
        mDirtyDrawingPts = true;
        mStylusUsed = inkItem.mStylusUsed;

        mCommittedAnnot = false;
    }
}
