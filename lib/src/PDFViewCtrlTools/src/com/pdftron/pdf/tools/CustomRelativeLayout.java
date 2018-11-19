//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.sdf.Obj;

/**
 * a {@link RelativeLayout} inside {@link PDFViewCtrl} with
 * specified page position posX, posY, and page_num.
 * See: {@link #setPagePosition(double, double, int)}
 * <p>
 * <div class='info'>
 * The position of this layout is calculated in PDF page coordinates.
 * In page coordinate, the origin location (0, 0) is at the bottom left corner of the PDF page.
 * The x axis extends horizontally to the right and y axis extends vertically upward.
 * </div>
 */
public class CustomRelativeLayout extends RelativeLayout implements PDFViewCtrl.OnCanvasSizeChangeListener {

    private static final String TAG = CustomRelativeLayout.class.getName();
    private static final boolean DEFAULT_ZOOM_WITH_PARENT = true;
    protected PDFViewCtrl mParentView;
    protected double mPagePosLeft = 0;
    protected double mPagePosBottom = 0;
    protected double[] mScreenPt = new double[2];
    protected int mPageNum = 1;
    protected boolean mZoomWithParent = DEFAULT_ZOOM_WITH_PARENT;


    /**
     * Constructor
     *
     * @param context  context of view
     * @param parent   parent view
     * @param x        x coordinates in page pt.
     * @param y        y coordinates in page pt.
     * @param page_num pdf page number
     */
    public CustomRelativeLayout(Context context, PDFViewCtrl parent, double x, double y, int page_num) {
        this(context);
        mParentView = parent;
        setPagePosition(x, y, page_num);
    }

    public CustomRelativeLayout(Context context) {
        this(context, null);
    }

    public CustomRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CustomRelativeLayout, defStyleAttr, defStyleRes);
        try {
            double x = a.getFloat(R.styleable.CustomRelativeLayout_posX, 0);
            double y = a.getFloat(R.styleable.CustomRelativeLayout_posY, 0);
            int page = a.getInt(R.styleable.CustomRelativeLayout_pageNum, 1);
            setPagePosition(x, y, page);
            setZoomWithParent(a.getBoolean(R.styleable.CustomRelativeLayout_zoomWithParent, DEFAULT_ZOOM_WITH_PARENT));
        } finally {
            a.recycle();
        }
    }

    /**
     * decide if the view will zoom while parent view is zooming
     *
     * @param zoomWithParent if true, when parent view is zooming, this view will also zoom;
     *                       otherwise this view will remain same size
     */
    public void setZoomWithParent(boolean zoomWithParent) {
        mZoomWithParent = zoomWithParent;
    }

    /**
     * Sets page position of this view
     * <div class='info'>
     * The position of this layout is calculated in PDF page coordinates.
     * In page coordinate, the origin location (0, 0) is at the bottom left corner of the PDF page.
     * The x axis extends horizontally to the right and y axis extends vertically upward.
     * </div>
     *
     * @param x        x coordinates in page pt.
     * @param y        y  coordinates in page pt.
     * @param page_num page number
     */
    public void setPagePosition(double x, double y, int page_num) {
        mPagePosLeft = x;
        mPagePosBottom = y;
        mPageNum = page_num;
    }

    /**
     * set view position and size by given annotation bounding box
     *
     * @param pdfViewCtrl  the PDFViewCtrl
     * @param annot        the annotation
     * @param annotPageNum annotation page number
     */
    public void setAnnot(PDFViewCtrl pdfViewCtrl, Annot annot, int annotPageNum) {
        if (null == pdfViewCtrl || null == annot) {
            return;
        }
        try {
            if (!annot.isValid()) {
                return;
            }
            double borderWidth = 0;
            if (annot.getType() == Annot.e_Widget) {
                Annot.BorderStyle bs = annot.getBorderStyle();
                Obj aso = annot.getSDFObj();
                if (aso.findObj("BS") == null && aso.findObj("Border") == null) {
                    bs.setWidth(0);
                }
                if (bs.getStyle() == Annot.BorderStyle.e_beveled || bs.getStyle() == Annot.BorderStyle.e_inset) {
                    bs.setWidth(bs.getWidth() * 2);
                }
                borderWidth = bs.getWidth();
            }

            com.pdftron.pdf.Rect r = annot.getVisibleContentBox();
            if (annot.getType() == Annot.e_FreeText) {
                FreeText freeText = new FreeText(annot);
                r = freeText.getContentRect();
            }

            double x1 = r.getX1() + borderWidth;
            double y1 = r.getY1() + borderWidth;
            double x2 = r.getX2() - borderWidth;
            double y2 = r.getY2() - borderWidth;

            r = new Rect(x1, y1, x2, y2);
            setRect(pdfViewCtrl, r, annotPageNum);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * set view position and size by given rect
     *
     * @param pdfViewCtrl the PDFViewCtrl
     * @param rect        the rect
     * @param pageNum     the page number
     */
    public void setRect(PDFViewCtrl pdfViewCtrl, com.pdftron.pdf.Rect rect, int pageNum) {
        try {
            double x1 = rect.getX1();
            double y1 = rect.getY1();
            double x2 = rect.getX2();
            double y2 = rect.getY2();

            int width = (int) Math.abs(x2 - x1);
            int height = (int) Math.abs(y2 - y1);

            double[] point1 = pdfViewCtrl.convPagePtToHorizontalScrollingPt(x1, y1, pageNum);
            double[] point2 = pdfViewCtrl.convPagePtToHorizontalScrollingPt(x2, y2, pageNum);

            x1 = point1[0];
            y1 = point1[1];
            x2 = point2[0];
            y2 = point2[1];

            double minX = Math.min(x1, x2);
            double maxY = Math.max(y1, y2); // we want the bottom here

            double[] result = pdfViewCtrl.convHorizontalScrollingPtToPagePt(minX, maxY, pageNum);

            mParentView = pdfViewCtrl;
            setPagePosition(result[0], result[1], pageNum);
            measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            setLayoutParams(new ViewGroup.LayoutParams(width, height));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewParent parent = getParent();
        if (parent != null && parent instanceof PDFViewCtrl) {
            mParentView = (PDFViewCtrl) parent;
            mParentView.addOnCanvasSizeChangeListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mParentView != null) {
            mParentView.removeOnCanvasSizeChangeListener(this);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mParentView == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        mScreenPt = mParentView.convPagePtToHorizontalScrollingPt(mPagePosLeft, mPagePosBottom, mPageNum);
        if (mZoomWithParent) {
            double[] screenBounds = mParentView.convPagePtToHorizontalScrollingPt(mPagePosLeft + width, mPagePosBottom - height, mPageNum);

            width = Math.abs((int) (screenBounds[0] - mScreenPt[0]));
            height = Math.abs((int) (screenBounds[1] - mScreenPt[1]));
            int nextWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            int nextHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            super.onMeasure(nextWidthMeasureSpec, nextHeightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        int l = (int) mScreenPt[0];
        int t = (int) mScreenPt[1] - height;
        int r = (int) mScreenPt[0] + width;
        int b = (int) mScreenPt[1];
        layout(l, t, r, b);
    }

    @Override
    public void onCanvasSizeChanged() {
        measure(getMeasuredWidthAndState(), getMeasuredHeightAndState());
        requestLayout();
    }
}
