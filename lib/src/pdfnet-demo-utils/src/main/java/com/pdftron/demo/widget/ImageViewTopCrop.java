//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.widget;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.facebook.drawee.view.SimpleDraweeView;

public class ImageViewTopCrop extends SimpleDraweeView {

    public ImageViewTopCrop(Context context) {
        super(context);
    }

    public ImageViewTopCrop(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImageViewTopCrop(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        if (getScaleType() == ScaleType.MATRIX) { // Crop top - fits width
            Matrix matrix = getImageMatrix();
            Drawable drawable = getDrawable();
            if (drawable != null && drawable.getIntrinsicWidth() != -1) {
                float scaleFactor = (r-l) / (float) drawable.getIntrinsicWidth();
                matrix.setScale(scaleFactor, scaleFactor, 0, 0);
                setImageMatrix(matrix);
                float finalHeight = drawable.getIntrinsicHeight() * scaleFactor;
                if(finalHeight < (b-t)){
                    int centDist = (int) (b-t-finalHeight)/2;
                    t = t + centDist;
                }

            }
        }

        return super.setFrame(l, t, r, b);
    }
}
