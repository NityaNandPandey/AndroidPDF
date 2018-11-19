//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.TooltipCompat;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.ImageMemoryCache;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The ThumbnailSlider uses the {@link com.pdftron.pdf.PDFViewCtrl#getThumbAsync(int)}
 * API to show thumbnails of the current page as the slider moves.
 */
public class ThumbnailSlider extends LinearLayout implements
    PDFViewCtrl.ThumbAsyncListener,
    PDFViewCtrl.DocumentLoadListener,
    PDFViewCtrl.PageChangeListener,
    View.OnClickListener {


    /**
     * Callback interface to be invoked when a tracking touch event occurs.
     */
    public interface OnThumbnailSliderTrackingListener {
        /**
         * Called when a tracking touch on thumbnail slider has started.
         */
        void onThumbSliderStartTrackingTouch();

        /**
         * Called when the tracking touch on thumbnail slider has been stopped.
         *
         * @param pageNum The current page number on thumbnail slider
         */
        void onThumbSliderStopTrackingTouch(int pageNum);
    }

    /**
     * Callback interface to be invoked when a menu item in thumbnail slider clicked
     */
    public interface OnMenuItemClickedListener {
        /**
         * called when the menu item is clicked
         *
         * @param menuItemPosition menu item position,
         *                         either {@link #POSITION_LEFT} or {@link #POSITION_RIGHT}
         */
        void onMenuItemClicked(@MenuItemPosition int menuItemPosition);
    }

    private enum ThumbnailState {
        None,       // No thumbnail
        Lingering,  // The current thumbnail does not match the current page on the slider, but it has yet to be removed
        Correct     // The current thumbnail matches that of the slider's page number
    }

    private static final int SCREEN_RATIO_THUMB_SIZE = 4;

    @IntDef({POSITION_LEFT, POSITION_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MenuItemPosition {
    }

    public static final int POSITION_LEFT = 0;
    public static final int POSITION_RIGHT = 1;
    private MirrorSeekBar mSeekBar;
    private LinearLayout mThumbView;
    private ImageView mThumbnailViewImage;
    private TextView mPageIndicator;
    private PopupWindow mThumbnailViewPopup;
    private PDFViewCtrl mPdfViewCtrl;

    private int mPageCount;
    private int mSeekBarMax;
    private int mCurrentPage;
    private int mPDFViewCtrlId;
    private boolean mIsProgressChanging;

    private int mThumbViewWidth;
    private int mThumbViewHeight;
    private int mScreenWidth;
    private int mScreenHeight;

    private boolean mViewerReady;
    private int mCurrentThumbnailPageNumber;
    private int mBlankDayResId;
    private int mBlankNightResId;

    private ThumbnailState mThumbnailState;

    private OnThumbnailSliderTrackingListener mListener;

    private static int SEEKBAR_GRANULARITY = 100;

    private ImageButton mLeftItemImageBtn;
    private ImageButton mRightItemImageBtn;

    private OnMenuItemClickedListener mMenuItemClickedListener;


    private Handler mRemoveOldThumbHandler = new Handler(Looper.getMainLooper());
    private Runnable mRemoveOldThumbRunnable = new Runnable() {
        @Override
        public void run() {
            removeOldThumbTick();
            mRemoveOldThumbHandler.postDelayed(this, 50);
        }
    };

    /**
     * Class constructor
     */
    public ThumbnailSlider(Context context) {
        this(context, null);
    }

    /**
     * Class constructor
     */
    public ThumbnailSlider(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.thumbnail_slider);
    }

    /**
     * Class constructor
     */
    public ThumbnailSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.ThumbnailSliderStyle);
    }

    /**
     * Class constructor
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ThumbnailSlider(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        initScreenSize(context);

        mViewerReady = false;
        mCurrentThumbnailPageNumber = -1;
        mPageCount = 1;
        mIsProgressChanging = false;
        mListener = null;

        setOrientation(VERTICAL);

        LayoutInflater.from(context).inflate(R.layout.controls_thumbnail_slider, this);
        mSeekBar = findViewById(R.id.controls_thumbnail_slider_scrubberview_seekbar);
        mLeftItemImageBtn = findViewById(R.id.controls_thumbnail_slider_left_menu_button);
        mRightItemImageBtn = findViewById(R.id.controls_thumbnail_slider_right_menu_button);
        mThumbView = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.controls_thumbnail_slider_preview, null);
        mThumbnailViewImage = mThumbView.findViewById(R.id.controls_thumbnail_slider_thumbview_thumb);
        mPageIndicator = mThumbView.findViewById(R.id.controls_thumbnail_slider_thumbview_pagenumber);

        mThumbnailViewPopup = new PopupWindow(mThumbView, mThumbViewWidth / SCREEN_RATIO_THUMB_SIZE, mThumbViewHeight / SCREEN_RATIO_THUMB_SIZE);

        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int currentPage = 1;

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mPdfViewCtrl != null) {
                    // get thumbnail popup position y
                    int[] sliderPosition = new int[2];
                    getLocationInWindow(sliderPosition);

                    int posX = sliderPosition[0] + (getWidth() / 2) - (mThumbnailViewPopup.getWidth() / 2);
                    int posY = sliderPosition[1] - (int) Utils.convDp2Pix(getContext(), 8) - mThumbnailViewPopup.getHeight();

                    mThumbnailViewPopup.showAtLocation(mPdfViewCtrl,
                        Gravity.LEFT | Gravity.TOP,
                        posX, posY);
                }
                mIsProgressChanging = true;
                if (mListener != null) {
                    mListener.onThumbSliderStartTrackingTouch();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mThumbnailViewPopup.dismiss();
                mIsProgressChanging = false;
                if (mPdfViewCtrl != null) {
                    mPdfViewCtrl.setCurrentPage(mCurrentPage);
                    try {
                        mPdfViewCtrl.cancelAllThumbRequests();
                    } catch (Exception e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    }
                }
                if (mListener != null) {
                    mListener.onThumbSliderStopTrackingTouch(mCurrentPage);
                }
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mSeekBarMax > SEEKBAR_GRANULARITY) {
                    currentPage = progress + 1;
                } else {
                    currentPage = (progress * mPageCount / SEEKBAR_GRANULARITY) + 1;
                }
                if (mPdfViewCtrl != null) {
                    String currentPageLabel = ViewerUtils.getPageLabelTitle(mPdfViewCtrl, currentPage);
                    if (!Utils.isNullOrEmpty(currentPageLabel)) {
                        mPageIndicator.setText(currentPageLabel);
                    } else {
                        mPageIndicator.setText(Utils.getLocaleDigits(Integer.toString(currentPage)));
                    }
                }
                mCurrentPage = currentPage;

                if (mViewerReady) {
                    requestThumb();
                }
            }
        });

        mThumbnailViewImage.setScaleType(ScaleType.CENTER_INSIDE);

        mBlankDayResId = R.drawable.white_square;
        mBlankNightResId = R.drawable.black_square;

        mLeftItemImageBtn.setOnClickListener(this);
        mRightItemImageBtn.setOnClickListener(this);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ThumbnailSlider, defStyleAttr, defStyleRes);
        try {
            mPDFViewCtrlId = typedArray.getResourceId(R.styleable.ThumbnailSlider_pdfviewctrlId, -1);
            int leftIconId = typedArray.getResourceId(R.styleable.ThumbnailSlider_leftMenuItemDrawable, -1);
            if (leftIconId != -1) {
                setMenuItem(leftIconId, POSITION_LEFT);
            }

            int rightIconId = typedArray.getResourceId(R.styleable.ThumbnailSlider_rightMenuItemDrawable, -1);
            if (rightIconId != -1) {
                setMenuItem(rightIconId, POSITION_RIGHT);
            }

            String contentDescriptionL = typedArray.getString(R.styleable.ThumbnailSlider_leftMenuItemContentDescription);
            if (!Utils.isNullOrEmpty(contentDescriptionL)) {
                setMenuItemContentDescription(POSITION_LEFT, contentDescriptionL);
            }

            String contentDescriptionR = typedArray.getString(R.styleable.ThumbnailSlider_rightMenuItemContentDescription);
            if (!Utils.isNullOrEmpty(contentDescriptionR)) {
                setMenuItemContentDescription(POSITION_RIGHT, contentDescriptionR);
            }

            // set seekbar color
            int primaryColor = Utils.getPrimaryColor(getContext());
            int seekbarColor = typedArray.getColor(R.styleable.ThumbnailSlider_seekbarColor, primaryColor);
            Drawable progressDrawable = mSeekBar.getProgressDrawable();
            progressDrawable.setColorFilter(seekbarColor, PorterDuff.Mode.SRC_IN);
            Drawable thumbDrawable = mSeekBar.getThumb();
            thumbDrawable.setColorFilter(seekbarColor, PorterDuff.Mode.SRC_IN);

            // set left/right items color
            int leftMenuItemColor = typedArray.getColor(R.styleable.ThumbnailSlider_leftMenuItemColor, primaryColor);
            int rightMenuItemColor = typedArray.getColor(R.styleable.ThumbnailSlider_rightMenuItemColor, primaryColor);
            mLeftItemImageBtn.setColorFilter(leftMenuItemColor, PorterDuff.Mode.SRC_IN);
            mRightItemImageBtn.setColorFilter(rightMenuItemColor, PorterDuff.Mode.SRC_IN);
        } finally {
            typedArray.recycle();
        }
    }

    private void initScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return;
        }
        Display display = wm.getDefaultDisplay();
        mScreenWidth = display.getWidth();
        mScreenHeight = display.getHeight();
        mThumbViewWidth = Math.min(mScreenWidth, mScreenHeight);
        mThumbViewHeight = Math.max(mScreenWidth, mScreenHeight);
    }

    /**
     * The overload implementation of {@link com.pdftron.pdf.PDFViewCtrl.ThumbAsyncListener#onThumbReceived(int, int[], int, int)}.
     */
    @Override
    public void onThumbReceived(int page, int[] buf, int width, int height) {
        boolean addThumb = true;

        if (mThumbnailState == ThumbnailState.Correct) {
            addThumb = false;
        }

        if (addThumb) {
            mCurrentThumbnailPageNumber = page;

            if (page != mCurrentPage) {
                mRemoveOldThumbHandler.postDelayed(mRemoveOldThumbRunnable, 50);
                mThumbnailState = ThumbnailState.Lingering;
            } else {

                if (width > mScreenWidth || height > mScreenHeight) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_HUGE_THUMBNAIL, AnalyticsParam.hugeThumbParam(width, height, buf.length, AnalyticsHandlerAdapter.LOCATION_THUMBNAILS_SLIDER));

                } else {
                    try {
                        ImageMemoryCache imageMemoryCache = ImageMemoryCache.getInstance();
                        Bitmap bitmap = imageMemoryCache.getBitmapFromReusableSet(width, height, Bitmap.Config.ARGB_8888);
                        if (bitmap == null) {
                            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        }
                        bitmap.setPixels(buf, 0, width, 0, 0, width, height);
                        RectF pageDim = getThumbnailSize(mCurrentPage);
                        setThumbnailViewImageBitmap(bitmap, 0, (int) pageDim.width(), (int) pageDim.height());
                        ImageMemoryCache.getInstance().addBitmapToReusableSet(bitmap);
                        mRemoveOldThumbHandler.removeCallbacks(mRemoveOldThumbRunnable);
                        mThumbnailState = ThumbnailState.Correct;
                    } catch (Exception e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    } catch (OutOfMemoryError oom) {
                        Utils.manageOOM(getContext(), mPdfViewCtrl);
                    }
                }
            }
        }
    }

    private void removeOldThumbTick() {
        mRemoveOldThumbHandler.removeCallbacks(mRemoveOldThumbRunnable);

        if (mThumbnailState == ThumbnailState.Lingering) {
            mCurrentThumbnailPageNumber = -1;
            mThumbnailState = ThumbnailState.None;
            RectF pageDim = getThumbnailSize(mCurrentPage);
            // check what should be the background for placeholder
            boolean nightMode;
            try {
                nightMode = ((ToolManager) mPdfViewCtrl.getToolManager()).isNightMode();
            } catch (Exception e) {
                nightMode = false;
            }
            int resId;
            if (nightMode) {
                resId = mBlankNightResId;
            } else {
                resId = mBlankDayResId;
            }
            setThumbnailViewImageBitmap(null, resId, (int) pageDim.width(), (int) pageDim.height());
        }
    }

    private void requestThumb() {
        boolean requestThumbnail = true;
        boolean blankPage = false;

        if (mThumbnailState == ThumbnailState.None) {
            requestThumbnail = true;
            blankPage = true;
        } else if (mCurrentPage == mCurrentThumbnailPageNumber) {
            requestThumbnail = false;
            mThumbnailState = ThumbnailState.Correct;
            mRemoveOldThumbHandler.removeCallbacks(mRemoveOldThumbRunnable);
        } else {
            mThumbnailState = ThumbnailState.Lingering;
            mRemoveOldThumbHandler.removeCallbacks(mRemoveOldThumbRunnable);
            mRemoveOldThumbHandler.postDelayed(mRemoveOldThumbRunnable, 50);
        }

        if (requestThumbnail) {
            try {
                mPdfViewCtrl.getThumbAsync(mCurrentPage);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        if (blankPage) {
            RectF pageDim = getThumbnailSize(mCurrentPage);
            // check what should be the background for placeholder
            boolean nightMode;
            try {
                nightMode = ((ToolManager) mPdfViewCtrl.getToolManager()).isNightMode();
            } catch (Exception e) {
                nightMode = false;
            }
            int resId;
            if (nightMode) {
                resId = mBlankNightResId;
            } else {
                resId = mBlankDayResId;
            }
            setThumbnailViewImageBitmap(null, resId, (int) pageDim.width(), (int) pageDim.height());
        }
    }

    /**
     * the input can be either a bitmap or a resource ID. If bitmap is null then
     * resource ID will be used.
     *
     * @param bitmap the bitmap to be scaled in view image
     * @param resId  the resource ID to be scaled in view image
     * @param width  the width of view image
     * @param height the height of view image
     */
    private void setThumbnailViewImageBitmap(Bitmap bitmap, int resId, int width, int height) {
        try {
            if (mThumbnailViewImage != null) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) mThumbnailViewImage.getDrawable();
                if (bitmapDrawable != null) {
                    Bitmap bmp = bitmapDrawable.getBitmap();
                    ImageMemoryCache.getInstance().addBitmapToReusableSet(bmp);
                }
                Bitmap scaledBitmap;
                if (bitmap == null) {
                    scaledBitmap = ImageMemoryCache.getInstance().decodeSampledBitmapFromResource(getResources(), resId, width, height);
                } else {
                    scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
                }
                mThumbnailViewImage.setImageBitmap(scaledBitmap);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            if (mThumbnailViewImage != null) {
                mThumbnailViewImage.setImageDrawable(null);
            }
        } catch (OutOfMemoryError oom) {
            if (mThumbnailViewImage != null) {
                mThumbnailViewImage.setImageDrawable(null);
            }
            Utils.manageOOM(getContext(), mPdfViewCtrl);
        }
    }

    /**
     * This method will free any bitmaps and other resources that are used internally by the
     * control. Only call this method when the control is not going to be used anymore (e.g., on the
     * onDestroy() of your Activity).
     */
    public void clearResources() {
        mRemoveOldThumbHandler.removeCallbacksAndMessages(null);
        if (mThumbnailViewImage != null) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) mThumbnailViewImage.getDrawable();
            if (bitmapDrawable != null) {
                Bitmap bmp = bitmapDrawable.getBitmap();
                ImageMemoryCache.getInstance().addBitmapToReusableSet(bmp);
            }
            mThumbnailViewImage.setImageDrawable(null);
        }

        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.removeDocumentLoadListener(this);
            mPdfViewCtrl.removeThumbAsyncListener(this);
            mPdfViewCtrl.removePageChangeListener(this);
        }
    }

    /**
     * Sets the progress based on the current page.
     *
     * @param currentPage The current page
     */
    public void setProgress(int currentPage) {
        int progress;
        if (currentPage <= 1) {
            progress = 0;
        } else {
            if (mSeekBarMax > SEEKBAR_GRANULARITY) {
                progress = currentPage == mPageCount ? mPageCount : currentPage - 1;
            } else {
                progress = currentPage == mPageCount ? SEEKBAR_GRANULARITY : (currentPage - 1) * SEEKBAR_GRANULARITY / mPageCount;
            }
        }
        mSeekBar.setProgress(progress);
    }

    /**
     * Sets the PDFViewCtrl.
     *
     * @param pdfViewCtrl The PDFViewCtrl
     */
    public void setPdfViewCtrl(PDFViewCtrl pdfViewCtrl) {
        if (pdfViewCtrl == null) {
            throw new NullPointerException("pdfViewCtrl can't be null");
        }
        mPdfViewCtrl = pdfViewCtrl;
        mPdfViewCtrl.addDocumentLoadListener(this);
        mPdfViewCtrl.addThumbAsyncListener(this);
        mPdfViewCtrl.addPageChangeListener(this);
    }

    /**
     * Sets the thumb slider listener.
     *
     * @param listener The listener
     */
    public void setThumbSliderListener(OnThumbnailSliderTrackingListener listener) {
        mListener = listener;
    }

    private RectF getThumbnailSize(int pageNum) {
        // These are the maximum dimensions we allow.
        double thumbWidth = mThumbViewWidth / SCREEN_RATIO_THUMB_SIZE;
        double thumbHeight = mThumbViewHeight / SCREEN_RATIO_THUMB_SIZE;
        if (mThumbViewWidth == 0 || mThumbViewHeight == 0) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("mThumbViewWidth/mThumbViewHeight are zero!"));
        }
        double pageWidth;
        double pageHeight;
        if (mPdfViewCtrl != null && mPdfViewCtrl.getDoc() != null) {
            try {
                Page page = mPdfViewCtrl.getDoc().getPage(pageNum);
                if (page != null) {
                    Rect pageCropBox = page.getCropBox();
                    pageWidth = pageCropBox.getWidth();
                    pageHeight = pageCropBox.getHeight();
                    int pageRotation = page.getRotation();
                    if (pageRotation == Page.e_90 || pageRotation == Page.e_270) {
                        double tmp = pageWidth;
                        //noinspection SuspiciousNameCombination
                        pageWidth = pageHeight;
                        pageHeight = tmp;
                    }
                    double scale = Math.min(thumbWidth / pageWidth, thumbHeight / pageHeight);
                    thumbWidth = scale * pageWidth;
                    thumbHeight = scale * pageHeight;
                    if ((int) thumbWidth == 0 || (int) thumbHeight == 0) {
                        AnalyticsHandlerAdapter.getInstance().sendException(
                            new Exception("thumb width/height are zero! " + "page width/height (" + pageWidth + "," + pageHeight + ")"));
                    }
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
        return new RectF(0, 0, (float) thumbWidth, (float) thumbHeight);
    }

    /**
     * @return True if progress is changing
     */
    public boolean isProgressChanging() {
        return mIsProgressChanging;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ThumbAsyncListener#onVisibilityChanged(View, int)}.
     */
    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        // Adjust position of the scrubber when this control is made visible
        if (mPdfViewCtrl != null) {
            if (mPageCount > 0) {
                if (visibility == View.VISIBLE) {
                    setProgress(mPdfViewCtrl.getCurrentPage());
                }
            }
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.DocumentLoadListener#onDocumentLoaded()}.
     */
    @Override
    public void onDocumentLoaded() {
        handleDocumentLoaded();
    }

    /**
     * Handles when the document is loaded.
     */
    public void handleDocumentLoaded() {
        mViewerReady = true;
        if (mPdfViewCtrl != null) {
            mThumbnailState = ThumbnailState.None;
            refreshPageCount();
            // We might be opening the document on a specific page
            setProgress(mPdfViewCtrl.getCurrentPage());
        }
    }

    /**
     * Refreshes the page count
     */
    public void refreshPageCount() {
        if (mPdfViewCtrl != null) {
            mPageCount = 0;
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                mPageCount = mPdfViewCtrl.getDoc().getPageCount();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }

            if (mPageCount <= 0) {
                mPageCount = 1;
            }
            mSeekBarMax = mPageCount > SEEKBAR_GRANULARITY ? mPageCount : SEEKBAR_GRANULARITY;
            mSeekBar.setMax(mSeekBarMax - 1);
        }
    }

    /**
     * @return True if the seek bar is reversed
     */
    public boolean isReversed() {
        return mSeekBar.isReversed();
    }

    /**
     * Reverses the seek bar.
     *
     * @param isReversed True if the seek bar should be reversed
     */
    public void setReversed(boolean isReversed) {
        mSeekBar.setReversed(isReversed);
        mSeekBar.invalidate();
    }

    public void show() {
        show(true);
    }

    public void show(boolean isAnimate) {
        if (isAnimate) {
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.thumbslider_slide_in_bottom);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            setVisibility(View.INVISIBLE);
            startAnimation(anim);
        } else {
            setVisibility(View.VISIBLE);
        }
    }

    public void dismiss() {
        dismiss(true);
    }

    public void dismiss(boolean isAnimate) {
        if (isAnimate) {
            Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.thumbslider_slide_out_bottom);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            startAnimation(anim);
        } else {
            setVisibility(View.GONE);
        }
    }

    /**
     * If it is has pdfViewCtrlId and current {@link #mPdfViewCtrl} is null,
     * set pdfviewCtrl when attached to window
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mPdfViewCtrl == null && mPDFViewCtrlId != -1) {
            View pdfView = getRootView().findViewById(mPDFViewCtrlId);
            if (pdfView != null && pdfView instanceof PDFViewCtrl) {
                setPdfViewCtrl((PDFViewCtrl) pdfView);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearResources();
    }

    @Override
    public void onPageChange(int old_page, int cur_page, PDFViewCtrl.PageChangeState state) {
        refreshPageCount();
    }


    @Override
    public void onClick(View v) {
        if (mMenuItemClickedListener == null) {
            return;
        }
        if (v.getId() == mLeftItemImageBtn.getId()) {
            mMenuItemClickedListener.onMenuItemClicked(POSITION_LEFT);
        } else if (v.getId() == mRightItemImageBtn.getId()) {
            mMenuItemClickedListener.onMenuItemClicked(POSITION_RIGHT);
        }
    }

    public void setOnMenuItemClickedListener(OnMenuItemClickedListener listener) {
        mMenuItemClickedListener = listener;
    }

    public void setMenuItem(@DrawableRes int drawableRes, @MenuItemPosition int position) {
        Drawable icon = Utils.getDrawable(getContext(), drawableRes);
        if (icon != null) {
            setMenuItem(icon, position);
        }
    }

    public void setMenuItem(@NonNull Drawable icon, @MenuItemPosition int position) {
        if (position == POSITION_LEFT) {
            mLeftItemImageBtn.setImageDrawable(icon);
            mLeftItemImageBtn.setVisibility(VISIBLE);
        } else {
            mRightItemImageBtn.setImageDrawable(icon);
            mRightItemImageBtn.setVisibility(VISIBLE);
        }
    }

    public void setMenuItemContentDescription(@MenuItemPosition int position, String content) {
        View view = null;
        switch (position) {
            case POSITION_LEFT:
                view = mLeftItemImageBtn;
                break;
            case POSITION_RIGHT:
                view = mRightItemImageBtn;
        }
        if (null != view) {
            TooltipCompat.setTooltipText(view, content);
            view.setContentDescription(content);
        }
    }

    public void setMenuItemVisibility(@MenuItemPosition int position, int visiblity) {
        ImageButton menuItem;
        switch (position) {
            case POSITION_LEFT:
                menuItem = mLeftItemImageBtn;
                break;
            case POSITION_RIGHT:
                menuItem = mRightItemImageBtn;
                break;
            default:
                menuItem = null;
                break;
        }
        if (menuItem != null) {
            menuItem.setVisibility(visiblity);
        }
    }

}
