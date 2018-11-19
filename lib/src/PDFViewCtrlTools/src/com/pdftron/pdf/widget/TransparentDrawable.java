//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

/**
 * This class is used for drawing a grey rectangle border and a red line inside,
 * It is used to represents the transparent color
 */
public class TransparentDrawable extends Drawable {
    private float mRoundedCorner = 0;
    private Paint mBorderPaint;
    private Paint mRedLinePaint;
    private Context mContext;
    public TransparentDrawable(Context context) {
        mContext = context;
        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(Utils.convDp2Pix(context, 0.5f));
        mBorderPaint.setColor(Color.GRAY);

        mRedLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRedLinePaint.setStyle(Paint.Style.STROKE);
        mRedLinePaint.setStrokeWidth(Utils.convDp2Pix(context, 1));
        mRedLinePaint.setColor(Color.RED);
    }

    /**
     * Overload implementation of {@link Drawable#draw(Canvas)}
     * Draw a light gray transparent rectangle border and a red line
     */

    @Override
    public void draw(@NonNull Canvas canvas) {
        float strokePadding = mBorderPaint.getStrokeWidth() / 2;
        double corner = mRoundedCorner + strokePadding;
        double d = Math.sqrt(corner * corner * 2) - corner;
        float x = (float) Math.sqrt(d * d / 2);

        canvas.drawLine(x, canvas.getHeight() - x, canvas.getWidth() - x, x, mRedLinePaint);
        RectF rectF = new RectF(strokePadding,
            strokePadding,
            canvas.getWidth() - strokePadding,
            canvas.getHeight() -strokePadding);

        canvas.drawRoundRect(rectF, mRoundedCorner, mRoundedCorner, mBorderPaint);

    }


    /**
     * Overload implementation of {@link Drawable#setAlpha(int)}
     * Specify an alpha value for the drawable. 0 means fully transparent, and
     * 255 means fully opaque.
     */
    @Override
    public void setAlpha(int alpha) {
        mBorderPaint.setAlpha(alpha);
        mRedLinePaint.setAlpha(alpha);
    }

    /**
     * Overload implementation of {@link Drawable#setColorFilter(int, PorterDuff.Mode)}
     * @param colorFilter The color filter to apply, or {@code null} to remove the
     *            existing color filter
     */
    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mBorderPaint.setColorFilter(colorFilter);
        mRedLinePaint.setColorFilter(colorFilter);
    }

    /**
     * Overload implementation of {@link Drawable#getOpacity()}
     * @returni int The opacity class of the Drawable.
     */
    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    /**
     * Sets rounded corner
     * @param roundedConer
     */
    public void setRoundedConer(float roundedConer) {
        mRoundedCorner = roundedConer;
    }

    /**
     * Sets rectangle border color
     * @param borderColor border color
     */
    public void setBorderColor(@ColorInt int borderColor) {
        mBorderPaint.setColor(borderColor);
    }


}
