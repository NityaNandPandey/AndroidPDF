//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.ColorInt;
import android.support.v7.widget.AppCompatImageView;
import android.text.TextPaint;
import android.util.AttributeSet;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.R;

import java.io.File;

/**
 * An image view for showing annotation preview based on annotation style
 */
public class AnnotationPropertyPreviewView extends AppCompatImageView {

    private String mPreviewText = "Aa";
    private float mPaintPadding;
    private int mAnnotType = Annot.e_Unknown;
    private Paint mPaint;
    private Paint mFillPaint;
    private Paint mTextPaint;

    private Path mPath;
    private Paint mTransparentPaint;
    private Paint mOuterStrokePaint;

    private double mWidth;
    private float mTextSizeRatio;
    private boolean mDrawTransparent = false;
    private int mParentBackground = Color.WHITE;
    private boolean mDrawStroke = true;
    private float mMinTextSize = 0;
    private boolean mUseStrokeRatio = false;
    private float mMaxThickness;

    public AnnotationPropertyPreviewView(Context context) {
        super(context);
        init(null);
    }

    public AnnotationPropertyPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public AnnotationPropertyPreviewView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    /**
     * Sets annotation type
     *
     * @param annotType The annotation type
     */
    public void setAnnotType(int annotType) {
        mAnnotType = annotType;
        mMaxThickness = ToolStyleConfig.getInstance().getDefaultMaxThickness(getContext(), annotType);
    }

    private void init(AttributeSet attrs) {
        mPaint = new Paint();
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);

        mFillPaint = new Paint();
        mFillPaint.setStrokeJoin(Paint.Join.ROUND);
        mFillPaint.setStrokeCap(Paint.Cap.ROUND);
        mFillPaint.setAntiAlias(true);
        mFillPaint.setStyle(Paint.Style.FILL);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setStrokeJoin(Paint.Join.ROUND);
        mTextPaint.setStrokeCap(Paint.Cap.ROUND);
        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setStrokeJoin(Paint.Join.ROUND);
        mTextPaint.setStrokeCap(Paint.Cap.ROUND);
        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mOuterStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOuterStrokePaint.setStyle(Paint.Style.STROKE);
        mOuterStrokePaint.setTextAlign(Paint.Align.CENTER);
        mOuterStrokePaint.setStrokeWidth(Utils.convDp2Pix(getContext(), 1));

        mTransparentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTransparentPaint.setStyle(Paint.Style.FILL);
        Bitmap transparent = BitmapFactory.decodeResource(getResources(), R.drawable.transparent_checker);
        mTransparentPaint.setShader(new BitmapShader(transparent, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        mTransparentPaint.setAlpha(137);
        setWillNotDraw(false);

        mPath = new Path();

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AnnotationPropertyPreviewView, 0, 0);

        boolean drawTransparent = a.getBoolean(R.styleable.AnnotationPropertyPreviewView_transparent_background, false);
        setDrawTransparentBackground(drawTransparent);

        boolean useStrokeRatio = a.getBoolean(R.styleable.AnnotationPropertyPreviewView_use_stroke_ratio, false);
        setUseStrokeRatio(useStrokeRatio);

        mParentBackground = a.getColor(R.styleable.AnnotationPropertyPreviewView_parent_background, Color.WHITE);
        int strokeColor = a.getColor(R.styleable.AnnotationPropertyPreviewView_stroke_color, getContext().getResources().getColor(R.color.tools_eraser_gray));
        setInnerOuterStrokeColor(strokeColor);
        String previewText = a.getString(R.styleable.AnnotationPropertyPreviewView_preview_text);
        if (!Utils.isNullOrEmpty(previewText)) {
            setPreviewText(previewText);
        }
        mMinTextSize = a.getDimensionPixelOffset(R.styleable.AnnotationPropertyPreviewView_min_text_size, 0);
        boolean drawStroke = a.getBoolean(R.styleable.AnnotationPropertyPreviewView_draw_stroke, true);
        setDrawInnerOuterStroke(drawStroke);
        a.recycle();
        mPaintPadding = Utils.convDp2Pix(getContext(), 2);
    }

    /**
     * Overload implementation of {@link android.widget.ImageView#onSizeChanged(int, int, int, int)}
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mPath.reset();
    }

    /**
     * Sets whether to draw transparent background
     *
     * @param draw, true then draw transparent background, false otherwise
     */
    public void setDrawTransparentBackground(boolean draw) {
        mDrawTransparent = draw;
    }

    /**
     * Sets inner and outer stroke color if border color
     * or fill color is the same as parent background color
     *
     * @param color stroke color
     */
    public void setInnerOuterStrokeColor(@ColorInt int color) {
        mOuterStrokePaint.setColor(color);
    }

    /**
     * Sets whether draw inner and outer stroke color if border color
     * or fill color is the same as parent background color
     *
     * @param drawStroke true then draw inner and outer stroke
     */
    public void setDrawInnerOuterStroke(boolean drawStroke) {
        mDrawStroke = drawStroke;
    }

    /**
     * Sets whether to use stroke ratio for drawing preview
     *
     * @param useStrokeRatio true then use stroke ratio, false otherwise
     */
    public void setUseStrokeRatio(boolean useStrokeRatio) {
        mUseStrokeRatio = useStrokeRatio;
    }

    /**
     * Sets the preview text for freetext annotation and text markup annotation
     *
     * @param text preview text
     */
    public void setPreviewText(String text) {
        mPreviewText = text;
    }

    /**
     * Updates preview based on annotation style
     *
     * @param annotStyle The annotation style
     */
    public void updateFillPreview(AnnotStyle annotStyle) {
        if (!Utils.isNullOrEmpty(annotStyle.getIcon())) {
            setImageDrawable(annotStyle.getIconDrawable(getContext()));
        }
        if (annotStyle.isFreeText()) {
            float maxTextSize = ToolStyleConfig.getInstance().getDefaultMaxTextSize(getContext());
            updateFreeTextStyle(annotStyle.getTextColor(), annotStyle.getTextSize() / maxTextSize);
            if (!Utils.isNullOrEmpty(annotStyle.getFontPath())) {
                setFontPath(annotStyle.getFontPath());
            }
        }
        updateFillPreview(annotStyle.getColor(), annotStyle.getFillColor(), annotStyle.getThickness(), annotStyle.getOpacity());
    }

    /**
     * Updates preview based on give style
     *
     * @param stroke    The stroke color
     * @param fill      The fill color of
     * @param thickness The thickness
     * @param opacity   The opacity
     */
    public void updateFillPreview(int stroke, int fill, double thickness, double opacity) {
        int alpha = (int) (255 * opacity);
        if (stroke == Color.TRANSPARENT) {
            mPaint.setColor(Color.argb(0, 0, 0, 0));
        } else {
            mPaint.setColor(Color.argb(alpha, Color.red(stroke), Color.green(stroke), Color.blue(stroke)));
        }

        if (fill == Color.TRANSPARENT) {
            mFillPaint.setColor(Color.argb(0, 0, 0, 0));
        } else {
            mFillPaint.setColor(Color.argb(alpha, Color.red(fill), Color.green(fill), Color.blue(fill)));
        }

        mTextPaint.setAlpha(alpha);

        // if it is sticky note
        if (mAnnotType == Annot.e_Text
            && getDrawable() != null && getDrawable() instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) getDrawable();
            layerDrawable.getDrawable(0).mutate();
            layerDrawable.getDrawable(0).setAlpha(alpha);
            layerDrawable.getDrawable(0).setColorFilter(stroke, PorterDuff.Mode.SRC_IN);
            layerDrawable.getDrawable(1).mutate();
            layerDrawable.getDrawable(1).setAlpha(alpha);
        }

        mWidth = thickness;

        invalidate();
    }

    /**
     * Sets font path to preview
     *
     * @param fontPath The font path
     */
    public void setFontPath(String fontPath) {
        if (Utils.isNullOrEmpty(fontPath)) {
            return;
        }
        File file = new File(fontPath);
        if (file.exists()) {
            Typeface typeFace = Typeface.createFromFile(file);
            if (typeFace != null) {
                mTextPaint.setTypeface(typeFace);
                mOuterStrokePaint.setTypeface(typeFace);
                invalidate();
            }
        }
    }

    /**
     * update free text color and size
     *
     * @param textColor     text color
     * @param textSizeRatio text size ratio, [0..1]
     */
    public void updateFreeTextStyle(@ColorInt int textColor, float textSizeRatio) {
        mTextSizeRatio = textSizeRatio;
        mTextPaint.setColor(textColor);
        invalidate();
    }

    /**
     * Sets parent view background color.
     * If parent view background color matches annotation color, it will draw a border around annotation
     * in case the preview looks invisible
     *
     * @param color The parent view background color.
     */
    public void setParentBackgroundColor(int color) {
        mParentBackground = color;
    }

    /**
     * Overload implementation of {@link android.widget.ImageView#onDraw(Canvas)}
     * If there is a image drawable, draw image; Else draw annotation preview based on annotation type
     * and annotation style.
     *
     * @param canvas the canvas on which everything will be drawn
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() != null) {
            super.onDraw(canvas);
            return;
        }

        int saveCount = canvas.getSaveCount();
        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        float realWidth = canvas.getWidth() - (getPaddingLeft() + getPaddingRight());
        float realHeight = canvas.getHeight() - (getPaddingTop() + getPaddingBottom());
        float widthScale = realWidth / canvas.getWidth();
        float heightScale = realHeight / canvas.getHeight();
        canvas.scale(widthScale, heightScale);

        float maxStroke = Math.min(canvas.getWidth(), canvas.getHeight()) * 3 / 8;

        float strokeWidth;
        if (mUseStrokeRatio) {
            float strokeRatio = mWidth < mMaxThickness ? (float) mWidth / mMaxThickness : 1f;
            strokeWidth = maxStroke * strokeRatio;
            if (strokeWidth > 0 && strokeWidth < Utils.convDp2Pix(getContext(), 2)) {
                strokeWidth = Utils.convDp2Pix(getContext(), 2);
            }
        } else {
            strokeWidth = Utils.convDp2Pix(getContext(), (float) mWidth);
            if (strokeWidth > maxStroke) {
                strokeWidth = maxStroke;
            } else if (strokeWidth > 0 && strokeWidth < Utils.convDp2Pix(getContext(), 2)) {
                strokeWidth = Utils.convDp2Pix(getContext(), 2);
            }
        }
        mPaint.setStrokeWidth(strokeWidth);

        drawTransparentBackground(canvas);
        switch (mAnnotType) {
            case Annot.e_FreeText:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                drawText(canvas);
                break;
            case Annot.e_Highlight:
                drawHighlight(canvas);
                break;
            case Annot.e_Underline:
                drawUnderline(canvas);
                break;
            case Annot.e_StrikeOut:
                drawStrikeOut(canvas);
                break;
            case Annot.e_Squiggly:
                drawSquiggly(canvas);
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
            case Annot.e_Ink:
                drawFreehand(canvas);
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                drawSigStroke(canvas);
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
                drawArrow(canvas);
                break;
            case Annot.e_Polyline:
                drawPolyline(canvas);
                break;
            case Annot.e_Square:
                drawRect(canvas);
                break;
            case Annot.e_Polygon:
                drawPolygon(canvas, false);
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
                drawPolygon(canvas, true);
                break;
            case Annot.e_Circle:
                drawOval(canvas);
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER:
                drawEraser(canvas);
                break;
            default:
                drawLine(canvas);
                break;
        }
        canvas.restoreToCount(saveCount);
    }

    private void drawTransparentBackground(Canvas canvas) {
        boolean drawTransparent = mDrawTransparent && mPaint.getAlpha() < 255 && mFillPaint.getAlpha() < 255;

        if (mAnnotType == Annot.e_FreeText || mAnnotType == AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT) {
            drawTransparent = drawTransparent && mTextPaint.getAlpha() < 255;
        }
        if (drawTransparent) {
            canvas.drawRect(mPaintPadding, mPaintPadding, getWidth() - mPaintPadding, getHeight() - mPaintPadding, mTransparentPaint);
        }
    }

    private void drawLine(Canvas canvas) {
        mPaint.setStrokeJoin(Paint.Join.MITER);
        mPaint.setStrokeCap(Paint.Cap.SQUARE);

        float strokeWidth = mPaint.getStrokeWidth();
        float pad = mPaintPadding + strokeWidth / 2;

        //noinspection UnnecessaryLocalVariable
        float x1 = pad;
        float y1 = getMeasuredHeight() - pad;
        float x2 = getMeasuredWidth() - pad;
        //noinspection UnnecessaryLocalVariable
        float y2 = pad;

        canvas.drawLine(x1, y1, x2, y2, mPaint);

        if (mDrawStroke && hasSameRgb(mParentBackground,mPaint.getColor())) {
            mOuterStrokePaint.setStrokeJoin(Paint.Join.MITER);
            mOuterStrokePaint.setStrokeCap(Paint.Cap.SQUARE);

            float dx = x2 - x1;
            float dy = y1 - y2;
            if (dy == 0) {
                return;
            }
            float len = strokeWidth / 2;
            double a = Math.atan(dx / dy);
            double sina = Math.sin(a);
            double cosa = Math.cos(a);
            float w = (float) (len * cosa);
            float h = (float) (len * sina);

            len = (strokeWidth - mOuterStrokePaint.getStrokeWidth()) / 2f;
            float w2 = (float) (len * sina);
            float h2 = (float) (len * cosa);

            Path path = new Path();
            path.moveTo(x1 + w - w2, y1 + h + h2);
            path.lineTo(x2 + w + w2, y2 + h - h2);
            path.lineTo(x2 - w + w2, y2 - h - h2);
            path.lineTo(x1 - w - w2, y1 - h + h2);
            path.lineTo(x1 + w - w2, y1 + h + h2);
            canvas.drawPath(path, mOuterStrokePaint);
        }
    }

    private void drawArrow(Canvas canvas) {
        mPaint.setStrokeJoin(Paint.Join.MITER);
        mPaint.setStrokeCap(Paint.Cap.SQUARE);

        float cos30 = (float) Math.cos(3.14159265 / 6);
        float sin30 = (float) Math.sin(3.14159265 / 6);
        float arrowLength = Utils.convDp2Pix(getContext(), 20);

        float strokeWidth = mPaint.getStrokeWidth();
        float pad = mPaintPadding + strokeWidth;
        //noinspection UnnecessaryLocalVariable
        float x1 = pad;
        float y1 = getMeasuredHeight() - pad;
        float x2 = getMeasuredWidth() - pad;
        float arrowOffset = pad / 6f; // y3 can go beyond the boundary, so consider more space
        float y2 = pad + arrowOffset;
        float dx = x1 - x2;
        float dy = y1 - y2;
        float len = dx * dx + dy * dy;
        if (len == 0f) {
            return;
        }
        len = (float) Math.sqrt(len);
        dx /= len;
        dy /= len;
        float dx1 = dx * cos30 - dy * sin30;
        float dy1 = dy * cos30 + dx * sin30;
        float x3 = x2 + arrowLength * dx1;
        float y3 = y2 + arrowLength * dy1;
        float dx2 = dx * cos30 + dy * sin30;
        float dy2 = dy * cos30 - dx * sin30;
        float x4 = x2 + arrowLength * dx2;
        float y4 = y2 + arrowLength * dy2;
        float x5 = x2 + dx * strokeWidth;
        float y5 = y2 + dy * strokeWidth;

        Path path = new Path();
        path.moveTo(x3, y3);
        path.lineTo(x2, y2);
        path.lineTo(x4, y4);
        canvas.drawPath(path, mPaint);
        canvas.drawLine(x5, y5, x1, y1, mPaint);

        if (mDrawStroke && hasSameRgb(mParentBackground, mPaint.getColor())) {
            mOuterStrokePaint.setStrokeJoin(Paint.Join.MITER);
            mOuterStrokePaint.setStrokeCap(Paint.Cap.SQUARE);
            canvas.drawPath(path, mOuterStrokePaint);
            canvas.drawLine(x5, y5, x1, y1, mOuterStrokePaint);
        }
    }

    private void drawPolyline(Canvas canvas) {
        mPaint.setStrokeJoin(Paint.Join.MITER);
        mPaint.setStrokeCap(Paint.Cap.SQUARE);

        float strokeWidth = mPaint.getStrokeWidth();
        float pad = mPaintPadding + strokeWidth / 2;
        Path path = getPolylinePath(pad);
        canvas.drawPath(path, mPaint);

        if (mDrawStroke && hasSameRgb(mParentBackground, mPaint.getColor())) {
            mOuterStrokePaint.setStrokeJoin(Paint.Join.MITER);
            mOuterStrokePaint.setStrokeCap(Paint.Cap.SQUARE);
            canvas.drawPath(path, mOuterStrokePaint);
        }
    }

    private Path getPolylinePath(float padding) {
        Path path = new Path();
        float w = getMeasuredWidth() - 2 * padding;
        float w_6 = w / 6;
        float h = getMeasuredHeight() - 2 * padding;
        float h_2 = h / 2;

        path.moveTo(padding, padding + h);
        path.lineTo(padding + w_6, padding + h_2);
        path.lineTo(padding + w - w_6, padding + h_2);
        path.lineTo(padding + w, padding);
        return path;
    }

    private void drawRect(Canvas canvas) {
        mPaint.setStrokeJoin(Paint.Join.MITER);
        mFillPaint.setStrokeJoin(Paint.Join.MITER);
        mPaint.setStrokeCap(Paint.Cap.SQUARE);
        mFillPaint.setStrokeCap(Paint.Cap.SQUARE);
        float strokeWidth = mPaint.getStrokeWidth();
        float pos = mPaintPadding + strokeWidth / 2;
        float w = getMeasuredWidth();
        float h = getMeasuredHeight();
        canvas.drawRect(pos, pos, w - pos, h - pos, mFillPaint);
        canvas.drawRect(pos, pos, w - pos, h - pos, mPaint);

        // if no fill color, and stroke color is the same as parent background, draw inner and outer border
        if (mDrawStroke && mFillPaint.getColor() == Color.TRANSPARENT && hasSameRgb(mParentBackground, mPaint.getColor())) {
            canvas.drawRect(mPaintPadding, mPaintPadding, w - mPaintPadding, h - mPaintPadding, mOuterStrokePaint);
            float innerPos = pos + strokeWidth / 2;
            canvas.drawRect(innerPos, innerPos, w - innerPos, h - innerPos, mOuterStrokePaint);
        }
        // if no stroke and fill color is as same as parent background, draw a outer border
        if (mDrawStroke && hasSameRgb(mParentBackground, mFillPaint.getColor()) && (mPaint.getColor() == Color.TRANSPARENT || mPaint.getStrokeWidth() == 0)) {
            canvas.drawRect(pos, pos, w - pos, h - pos, mOuterStrokePaint);
        }
    }

    private void drawPolygon(Canvas canvas, boolean isCloud) {
        mPaint.setStrokeJoin(Paint.Join.MITER);
        mFillPaint.setStrokeJoin(Paint.Join.MITER);
        mPaint.setStrokeCap(Paint.Cap.SQUARE);
        mFillPaint.setStrokeCap(Paint.Cap.SQUARE);
        float strokeWidth = mPaint.getStrokeWidth();
        float pos = mPaintPadding + strokeWidth / 2;
        Path path = getPolygonPath(pos, isCloud);
        canvas.drawPath(path, mFillPaint);
        canvas.drawPath(path, mPaint);

        // if no fill color, and stroke color is the same as parent background, draw inner and outer border
        if (mDrawStroke && mFillPaint.getColor() == Color.TRANSPARENT && hasSameRgb(mParentBackground, mPaint.getColor())) {
            path = getPolygonPath(mPaintPadding, isCloud);
            canvas.drawPath(path, mOuterStrokePaint);
            float innerPos = pos + strokeWidth / 2;
            path = getPolygonPath(innerPos, isCloud);
            canvas.drawPath(path, mOuterStrokePaint);
        }
        // if no stroke and fill color is as same as parent background, draw a outer border
        if (mDrawStroke && hasSameRgb(mParentBackground, mFillPaint.getColor()) && (mPaint.getColor() == Color.TRANSPARENT || mPaint.getStrokeWidth() == 0)) {
            path = getPolygonPath(pos, isCloud);
            canvas.drawPath(path, mOuterStrokePaint);
        }
    }

    private Path getPolygonPath(float padding, boolean isCloud) {
        Path path = new Path();
        float w = getMeasuredWidth() - 2 * padding;
        float w_2 = w / 2;
        float w_4 = w / 4;
        float w_6 = w / 6;
        float h = getMeasuredHeight() - 2 * padding;
        float h_2 = h / 2;
        float h_6 = h / 6;
        if (isCloud) {
            path.moveTo(padding + w_2, padding + h);
            path.lineTo(padding + w_6, padding + h);
            path.cubicTo(padding, padding + h, padding, padding + h_2, padding + w_6, padding + h_2);
            path.cubicTo(padding + w_6, padding - h_6, padding + w - w_6, padding - h_6, padding + w - w_6, padding + h_2);
            path.cubicTo(padding + w, padding + h_2, padding + w, padding + h, padding + w - w_6, padding + h);
            path.lineTo(padding + w_2, padding + h);
        } else {
            path.moveTo(padding, padding + h_2);
            path.lineTo(padding + w_4, padding);
            path.lineTo(padding + w - w_4, padding);
            path.lineTo(padding + w, padding + h_2);
            path.lineTo(padding + w - w_4, padding + h);
            path.lineTo(padding + w_4, padding + h);
            path.lineTo(padding, padding + h_2);
        }
        return path;
    }

    private void drawOval(Canvas canvas) {
        if (Utils.isLollipop()) {
            float strokeWidth = mPaint.getStrokeWidth();
            float pos = mPaintPadding + strokeWidth / 2;
            canvas.drawOval(pos, pos, getMeasuredWidth() - pos, getMeasuredHeight() - pos, mFillPaint);
            canvas.drawOval(pos, pos, getMeasuredWidth() - pos, getMeasuredHeight() - pos, mPaint);
            if (mDrawStroke && mFillPaint.getColor() == Color.TRANSPARENT && hasSameRgb(mParentBackground, mPaint.getColor())) {
                canvas.drawOval(mPaintPadding, mPaintPadding, getMeasuredWidth() - mPaintPadding, getMeasuredHeight() - mPaintPadding, mOuterStrokePaint);
                float innerPos = pos + strokeWidth / 2;
                canvas.drawOval(innerPos, innerPos, getMeasuredWidth() - innerPos, getMeasuredHeight() - innerPos, mOuterStrokePaint);
            }
            if (mDrawStroke && hasSameRgb(mParentBackground, mFillPaint.getColor()) && (mPaint.getColor() == Color.TRANSPARENT || mPaint.getStrokeWidth() == 0)) {
                canvas.drawOval(pos, pos, getMeasuredWidth() - pos, getMeasuredHeight() - pos, mOuterStrokePaint);
            }
        } else {
            canvas.drawCircle(getMeasuredWidth() * .5f, getMeasuredHeight() * .5f, getMeasuredWidth() * .3f, mFillPaint);
            canvas.drawCircle(getMeasuredWidth() * .5f, getMeasuredHeight() * .5f, getMeasuredWidth() * .3f, mPaint);

            if (mDrawStroke && mFillPaint.getColor() == Color.TRANSPARENT && hasSameRgb(mParentBackground, mPaint.getColor())) {
                float outerRadius = getMeasuredWidth() * .3f + mPaint.getStrokeWidth() / 2;
                float innerRadius = getMeasuredWidth() * .3f - mPaint.getStrokeWidth() / 2;
                canvas.drawCircle(getMeasuredWidth() * .5f, getMeasuredHeight() * .5f, outerRadius, mOuterStrokePaint);
                canvas.drawCircle(getMeasuredWidth() * .5f, getMeasuredHeight() * .5f, innerRadius, mOuterStrokePaint);
            }

            if (mDrawStroke && hasSameRgb(mParentBackground, mFillPaint.getColor()) && (mPaint.getColor() == Color.TRANSPARENT || mPaint.getStrokeWidth() == 0)) {
                canvas.drawCircle(getMeasuredWidth() * .5f, getMeasuredHeight() * .5f, getMeasuredWidth() * .3f, mOuterStrokePaint);
            }
        }
    }

    private void drawEraser(Canvas canvas) {
        // calculate the radius of the preview circle
        int minRadius = (int) (getMeasuredHeight() * 0.1f);
        int maxRadius = (int) (getMeasuredHeight() * 0.5f);
        float eraserRange = ToolStyleConfig.getInstance().getDefaultThicknessRange(getContext(), AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER);
        float radiusRange = maxRadius - minRadius;
        float scale = radiusRange / eraserRange;
        float eraserMinThickness = ToolStyleConfig.getInstance().getDefaultMinThickness(getContext(), AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER);
        int radius = (int) (((mWidth - eraserMinThickness) * scale) + minRadius);

        // draw the preview circle
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setAntiAlias(true);
        paint.setColor(getResources().getColor(R.color.tools_eraser_gray));
        canvas.drawCircle(getMeasuredWidth() * .5f, getMeasuredHeight() * .5f, radius, paint);
    }

    private void drawFreehand(Canvas canvas) {
        mPath.moveTo(getMeasuredWidth() * .3f, getMeasuredHeight() * .8f);
        mPath.quadTo(getMeasuredWidth() * .3f, getMeasuredHeight() * .3f, getMeasuredWidth() * .8f, getMeasuredHeight() * .2f);

        canvas.drawPath(mPath, mPaint);
    }

    private void drawSigStroke(Canvas canvas) {
        mPaint.setStrokeWidth((float) mWidth);
        drawFreehand(canvas);
    }

    private void drawText(Canvas canvas) {
        drawRect(canvas);

        float currTextSize = mTextPaint.getTextSize();
        Rect bounds = new Rect();
        mTextPaint.getTextBounds(mPreviewText, 0, mPreviewText.length(), bounds);
        float maxTextSize = currTextSize * canvas.getWidth() / bounds.width();
        float textSize = mTextSizeRatio * maxTextSize;
        if (textSize < mMinTextSize) {
            textSize = mMinTextSize;
        }

        mTextPaint.setTextSize(textSize);

        mOuterStrokePaint.setTextSize(textSize);

        float xPos = (canvas.getWidth() * .5f);
        float yPos = (canvas.getHeight() * .5f) - ((mTextPaint.descent() + mTextPaint.ascent()) * .5f);

        canvas.drawText(mPreviewText, xPos, yPos, mTextPaint);

        if (mDrawStroke && mFillPaint.getColor() == Color.TRANSPARENT && hasSameRgb(mParentBackground, mTextPaint.getColor())) {
            canvas.drawText(mPreviewText, xPos, yPos, mOuterStrokePaint);
        }
    }

    private void drawSquiggly(Canvas canvas) {
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(getContext().getResources().getColor(R.color.controls_annot_style_markup_text));
        float currTextSize = textPaint.getTextSize();
        Rect bounds = new Rect();
        textPaint.getTextBounds(mPreviewText, 0, mPreviewText.length(), bounds);
        float maxTextSize = currTextSize * (canvas.getWidth() - 2 * mPaintPadding) / bounds.width();

        textPaint.setTextSize(maxTextSize);

        Paint.FontMetrics fm = new Paint.FontMetrics();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.getFontMetrics(fm);

        float xPos = (canvas.getWidth() * .5f);
        float yPos = canvas.getHeight() * .5f - ((textPaint.descent() + textPaint.ascent()) / 2);
        canvas.drawText(mPreviewText, xPos, yPos, textPaint);
        // draw squiggly
        float size = Utils.convDp2Pix(getContext(), 4);
        Path path = new Path();
        float startX = xPos - textPaint.measureText(mPreviewText) * .5f;
        float startY = canvas.getHeight() - textPaint.descent();
        float endX = xPos + textPaint.measureText(mPreviewText) * .5f;
        boolean upward = true;
        path.moveTo(startX, startY);

        while (startX < endX) {
            float nextX = startX + size * 2;
            float midY = upward ? (startY - size) : (startY + size);
            float midX = startX + size;
            path.quadTo(midX, midY, nextX, startY);
            path.moveTo(nextX, startY);
            startX = nextX;
            upward = !upward;
        }
        canvas.drawPath(path, mPaint);
    }

    private void drawUnderline(Canvas canvas) {
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(getContext().getResources().getColor(R.color.controls_annot_style_markup_text));
        float currTextSize = textPaint.getTextSize();
        Rect bounds = new Rect();
        textPaint.getTextBounds(mPreviewText, 0, mPreviewText.length(), bounds);
        float maxTextSize = currTextSize * (canvas.getWidth() - 2 * mPaintPadding) / bounds.width();

        textPaint.setTextSize(maxTextSize);

        Paint.FontMetrics fm = new Paint.FontMetrics();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.getFontMetrics(fm);

        float xPos = (canvas.getWidth() * .5f);
        float yPos = canvas.getHeight() * .5f - ((textPaint.descent() + textPaint.ascent()) / 2);

        canvas.drawLine(xPos - textPaint.measureText(mPreviewText) * .5f, canvas.getHeight() - textPaint.descent(), xPos + textPaint.measureText(mPreviewText) * .5f, canvas.getHeight() - textPaint.descent(), mPaint);
        canvas.drawText(mPreviewText, xPos, yPos, textPaint);
    }

    private void drawStrikeOut(Canvas canvas) {
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(getContext().getResources().getColor(R.color.controls_annot_style_markup_text));
        float currTextSize = textPaint.getTextSize();
        Rect bounds = new Rect();
        textPaint.getTextBounds(mPreviewText, 0, mPreviewText.length(), bounds);
        float maxTextSize = currTextSize * (canvas.getWidth() - 2 * mPaintPadding) / bounds.width();

        textPaint.setTextSize(maxTextSize);

        Paint.FontMetrics fm = new Paint.FontMetrics();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.getFontMetrics(fm);

        float xPos = (canvas.getWidth() * .5f);
        float yPos = canvas.getHeight() * .5f - ((textPaint.descent() + textPaint.ascent()) / 2);
        canvas.drawText(mPreviewText, xPos, yPos, textPaint);
        canvas.drawLine(xPos - textPaint.measureText(mPreviewText) * .5f, canvas.getHeight() * .5f, xPos + textPaint.measureText(mPreviewText) * .5f, canvas.getHeight() * .5f, mPaint);
    }

    private void drawHighlight(Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(getContext().getResources().getColor(R.color.controls_annot_style_markup_text));
        float currTextSize = textPaint.getTextSize();
        Rect bounds = new Rect();
        textPaint.getTextBounds(mPreviewText, 0, mPreviewText.length(), bounds);
        float maxTextSize = currTextSize * (canvas.getWidth() - 2 * mPaintPadding) / bounds.width();

        textPaint.setTextSize(maxTextSize);

        Paint.FontMetrics fm = new Paint.FontMetrics();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.getFontMetrics(fm);

        float xPos = (canvas.getWidth() * .5f);
        float yPos = canvas.getHeight() * .5f - ((textPaint.descent() + textPaint.ascent()) / 2);
        canvas.drawRect(xPos - textPaint.measureText(mPreviewText) * .5f, yPos - textPaint.getTextSize() + 5f, xPos + textPaint.measureText(mPreviewText) * .5f, yPos + 10f, mPaint);
        canvas.drawText(mPreviewText, xPos, yPos, textPaint);
    }

    private static boolean hasSameRgb(int color1, int color2) {
        return (color1 & 0X00FFFFFF) == (color2 & 0X00FFFFFF);
    }
}
