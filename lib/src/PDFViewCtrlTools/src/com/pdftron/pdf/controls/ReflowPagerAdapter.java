//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.ReflowProcessor;
import com.pdftron.pdf.RequestHandler;
import com.pdftron.pdf.asynctask.HtmlPostProcessColorTask;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.ReflowWebView;
import com.pdftron.pdf.utils.Utils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

// NOTE: if the html file is very large, it may take time for viewpager to show the html content,
//    and therefore, before showing the content it may first show the content from another page

/**
 * pager adapter for reflow
 */
@SuppressWarnings({"deprecation", "WeakerAccess"})
public class ReflowPagerAdapter extends PagerAdapter
    implements ReflowWebView.ReflowWebViewCallback, RequestHandler.RequestHandlerCallback {
    private static final String TAG = ReflowPagerAdapter.class.getName();
    private static boolean sDebug;

    private final static String LIGHT_MODE_LOADING_FILE = "file:///android_asset/loading_page_light.html";
    private final static String DARK_MODE_LOADING_FILE = "file:///android_asset/loading_page_dark.html";
    private final static String NIGHT_MODE_LOADING_FILE = "file:///android_asset/loading_page_night.html";

    private static final float TAP_REGION_THRESHOLD = (1f / 7f);

    private enum ColorMode {
        DayMode,
        NightMode,
        CustomMode
    }

    private ColorMode mColorMode = ColorMode.DayMode;
    private int mBackgroundColorMode = 0XFFFFFF;
    private RequestHandler mRequestHandler;
    private PDFDoc mDoc;
    private int mPageCount;
    private ViewPager mViewPager; // need it to get current page
    private SparseArray<String> mReflowFiles;
    private SparseArray<ReflowWebView> mViewHolders;
    private boolean mIsRtlMode;
    private boolean mDoTurnPageOnTap;
    private boolean mIsInternalLinkClicked = false;

    /*
     * scaling parameters
     * NOTE: if change SCALES, you may also need to change mDefaultScaleIndex
     */
    private static float[] SCALES = {0.05f, 0.1f, 0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 4.0f, 8.0f, 16.0f};
    private final int mDefaultScaleIndex = 5;
    private final static int mMaxIndex = SCALES.length - 1;
    public final static int TH_MIN_SCAlE = Math.round(SCALES[0] * 100);
    public final static int TH_MAX_SCAlE = Math.round(SCALES[mMaxIndex] * 100);
    private final static float TH_SCAlE_GESTURE = 1.25f;
    private int mScaleIndex = mDefaultScaleIndex;
    private Context mContext;
    private float mScaleFactor;
    private float mLastScaleFactor;
    private float mThConsecutiveScales;
    private boolean mZoomInFlag;

    private LongSparseArray<Integer> mObjNumMap = new LongSparseArray<>();
    private int mLastProcessedObjNum = 0;
    private ReflowControl.OnPostProcessColorListener mOnPostProcessColorListener;
    private final CopyOnWriteArrayList<HtmlPostProcessColorTask> mHtmlPostProcessColorTasks = new CopyOnWriteArrayList<>();

    private static class ClickHandler extends Handler {
        private static final int GO_TO_NEXT_PAGE = 1;
        private static final int GO_TO_PREVIOUS_PAGE = 2;
        private static final int CLICK_ON_URL = 3;

        private final WeakReference<ReflowPagerAdapter> mCtrl;

        ClickHandler(ReflowPagerAdapter ctrl) {
            mCtrl = new WeakReference<>(ctrl);
        }

        @Override
        public void handleMessage(Message msg) {
            ReflowPagerAdapter ctrl = mCtrl.get();
            if (ctrl != null && ctrl.mViewPager != null) {
                ViewPager viewPager = ctrl.mViewPager;
                int position = viewPager.getCurrentItem();
                switch (msg.what) {
                    case GO_TO_NEXT_PAGE:
                        viewPager.setCurrentItem(position + 1);
                        break;
                    case GO_TO_PREVIOUS_PAGE:
                        viewPager.setCurrentItem(position - 1);
                        break;
                }
            }
            if (msg.what == CLICK_ON_URL) {
                removeMessages(GO_TO_NEXT_PAGE);
                removeMessages(GO_TO_PREVIOUS_PAGE);
            }
        }
    }

    private final ClickHandler mClickHandler = new ClickHandler(this);

    private static class UpdateReflowHandler extends Handler {

        private final WeakReference<ReflowPagerAdapter> mCtrl;

        UpdateReflowHandler(ReflowPagerAdapter ctrl) {
            mCtrl = new WeakReference<>(ctrl);
        }

        @Override
        public void handleMessage(Message msg) {
            final ReflowPagerAdapter ctrl = mCtrl.get();
            if (ctrl != null) {
                Vector<?> params = (Vector<?>) msg.obj;
                final RequestHandler.JobRequestResult result = (RequestHandler.JobRequestResult) params.elementAt(0);
                String outFilename = (String) params.elementAt(1);
                final Object customData = params.elementAt(2);

                if (ctrl.mColorMode == ColorMode.CustomMode || ctrl.mColorMode == ColorMode.NightMode) {
                    HtmlPostProcessColorTask task = new HtmlPostProcessColorTask(ctrl.mContext, outFilename);
                    task.setOnPostProcessColorListener(ctrl.mOnPostProcessColorListener);
                    task.setCallback(new HtmlPostProcessColorTask.Callback() {
                        @Override
                        public void onPostProcessColorFinished(@NonNull HtmlPostProcessColorTask htmlPostProcessColorTask, String outputFilename) {
                            Log.d(TAG, "update reflow of page #" + (1 + (int) customData));
                            ctrl.updateReflow(result, outputFilename, customData);
                            ctrl.mHtmlPostProcessColorTasks.remove(htmlPostProcessColorTask);
                        }
                    });
                    ctrl.mHtmlPostProcessColorTasks.add(task);
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    ctrl.updateReflow(result, outFilename, customData);
                }
            }
        }
    }

    private final UpdateReflowHandler mUpdateReflowHandler = new UpdateReflowHandler(this);

    /**
     * Callback interfaces for tap
     */
    public interface ReflowPagerAdapterCallback {

        /**
         * Called with single tab up event
         *
         * @param event The motion event
         */
        void onReflowPagerSingleTapUp(MotionEvent event);
    }

    private ReflowPagerAdapterCallback mCallback;

    /**
     * Sets the listener to ReflowPagerAdapterCallback
     *
     * @param listener The listener
     */
    public void setListener(ReflowPagerAdapterCallback listener) {
        mCallback = listener;
    }

    /**
     * Class constructor
     *
     * @param viewPager The view pager
     * @param context   The context
     * @param doc       The PDF doc
     */
    public ReflowPagerAdapter(ViewPager viewPager, Context context, PDFDoc doc) {
        mViewPager = viewPager;
        mDoc = doc;
        mContext = context;
        mRequestHandler = new RequestHandler(this);
        mPageCount = 0;
        boolean shouldUnlockRead = false;

        try {
            mDoc.lockRead();
            shouldUnlockRead = true;
            mPageCount = mDoc.getPageCount();
            mReflowFiles = new SparseArray<>(mPageCount);
            mViewHolders = new SparseArray<>(mPageCount);
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                Utils.unlockReadQuietly(mDoc);
            }
        }

        // NOTE: make sure that the off-screen page limit is set to one
        viewPager.setOffscreenPageLimit(1);
    }

    /**
     * reset adapter to reload all data
     */
    private void resetAdapter() {
        int curPosition = mViewPager.getCurrentItem();
        mViewPager.setAdapter(this);
        mViewPager.setCurrentItem(curPosition, false);
    }

    /**
     * Should be called when pages of the document have been edited
     */
    public void onPagesModified() {
        if (sDebug) Log.d(TAG, "pages were modified.");
        ReflowProcessor.cancelAllRequests();
        mPageCount = 0;
        boolean shouldUnlockRead = false;
        try {
            mDoc.lockRead();
            shouldUnlockRead = true;
            mPageCount = mDoc.getPageCount();
            mReflowFiles = new SparseArray<>(mPageCount);
            mViewHolders = new SparseArray<>(mPageCount);
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                Utils.unlockReadQuietly(mDoc);
            }
        }
        mObjNumMap.clear();
        mLastProcessedObjNum = 0;
        resetAdapter();
    }

    /**
     * Cleans up resources.
     */
    public void cleanup() {
        if (sDebug) Log.d(TAG, "Cleanup");
        ReflowProcessor.cancelAllRequests();
        for (HtmlPostProcessColorTask task : mHtmlPostProcessColorTasks) {
            task.cancel(true);
            task.setOnPostProcessColorListener(null);
            task.setCallback(null);
        }
        mClickHandler.removeCallbacksAndMessages(null);
        mUpdateReflowHandler.removeCallbacksAndMessages(null);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private ReflowWebView getReflowWebView() {


        ReflowWebView webView = new ReflowWebView(mContext);
        webView.clearCache(true); // reset reading css file
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWillNotCacheDrawing(false);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setListener(this);

        webView.setWebChromeClient(new WebChromeClient()); // enable the use of methods like alert in javascript
        webView.setWebViewClient(new WebViewClient() {
            // now all links the user clicks load in your WebView
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, description + " url: " + failingUrl);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
                Log.e(TAG, error.toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (mClickHandler != null) {
                    mClickHandler.sendEmptyMessage(ClickHandler.CLICK_ON_URL);
                }
                if (url.startsWith("file:///") && url.endsWith(".html")) {
                    int slashPos = url.lastIndexOf('/');
                    boolean shouldUnlockRead = false;
                    try {
                        long curObjNum = Long.parseLong(url.substring(slashPos + 1, url.length() - 5));
                        int curPageNum = 0;
                        if (mObjNumMap.get(curObjNum) != null) {
                            curPageNum = mObjNumMap.get(curObjNum);
                        } else {
                            try {
                                mDoc.lockRead();
                                shouldUnlockRead = true;
                                for (int i = mLastProcessedObjNum + 1; i <= mPageCount; i++) {
                                    Page page = mDoc.getPage(i);
                                    long objNum = page.getSDFObj().getObjNum();
                                    ++mLastProcessedObjNum;
                                    mObjNumMap.put(objNum, i);
                                    if (objNum == curObjNum) {
                                        curPageNum = i;
                                        break;
                                    }
                                }
                            } catch (Exception ignored) {

                            } finally {
                                if (shouldUnlockRead) {
                                    Utils.unlockReadQuietly(mDoc);
                                }
                            }
                        }
                        if (curPageNum != 0) {
                            mIsInternalLinkClicked = true;
                            mViewPager.setCurrentItem(mIsRtlMode ? mPageCount - curPageNum : curPageNum - 1);
                        }
                    } catch (NumberFormatException e) {
                        return true;
                    }
                } else {
                    if (url.startsWith("mailto:") || android.util.Patterns.EMAIL_ADDRESS.matcher(url).matches()) {
                        if (url.startsWith("mailto:")) {
                            url = url.substring(7);
                        }
                        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", url, null));
                        mContext.startActivity(Intent.createChooser(intent, mContext.getResources().getString(R.string.tools_misc_sendemail)));
                    } else {
                        // ACTION_VIEW needs the address to have http or https
                        if (!url.startsWith("https://") && !url.startsWith("http://")) {
                            url = "http://" + url;
                        }
                        if (sDebug) Log.d(TAG, url);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        mContext.startActivity(Intent.createChooser(intent, mContext.getResources().getString(R.string.tools_misc_openwith)));
                    }
                }
                return true;
            }
        });

        switch (mColorMode) {
            case DayMode:
                webView.setBackgroundColor(Color.WHITE);
                break;
            case NightMode:
                webView.setBackgroundColor(Color.BLACK);
                break;
            case CustomMode:
                webView.setBackgroundColor(mBackgroundColorMode);
                break;
        }
        webView.loadUrl("about:blank");

        return webView;
    }

    /**
     * Sets the right-to-left direction of the document.
     * Used for supporting right-to-left languages.
     *
     * @param isRtlMode True if right-to-left mode is enabled
     */
    public void setRightToLeftDirection(boolean isRtlMode) {
        mIsRtlMode = isRtlMode;
        if (sDebug) Log.d("Reflow Right to Left", mIsRtlMode ? "True" : "False");
    }

    /**
     * @return True if the direction is right-to-left
     */
    public boolean isRightToLeftDirection() {
        return mIsRtlMode;
    }

    /**
     * Sets colors in the day mode (default).
     */
    public void setDayMode() {
        mColorMode = ColorMode.DayMode;
        updateColorMode();
    }

    /**
     * Sets colors in the night mode.
     */
    public void setNightMode() {
        mColorMode = ColorMode.NightMode;
        updateColorMode();
    }

    /**
     * Sets custom color.
     */
    public void setCustomColorMode(int backgroundColorMode) {
        mBackgroundColorMode = backgroundColorMode;
        mColorMode = ColorMode.CustomMode;
        updateColorMode();
    }

    /**
     * @return True if reflow is in day mode
     */
    boolean isDayMode() {
        return mColorMode == ColorMode.DayMode;
    }

    /**
     * @return True if reflow is in night mode
     */
    boolean isNightMode() {
        return mColorMode == ColorMode.NightMode;
    }

    /**
     * @return True if reflow is in custom color mode
     */
    boolean isCustomColorMode() {
        return mColorMode == ColorMode.CustomMode;
    }

    private void updateColorMode() {
        // note that reflowable files can be in custom color mode, night mode or in day mode.
        // An easy way to handle such situation is to clear everything in our cache (mReflowFiles)
        // and request again for reflowable files from the core.
        // It is not worst idea since core already cached the computed reflowable files so it
        // doesn't affect much on the performance, though it is possible to handle it here with some
        // effort. Right now we choose the easy solution.
        mReflowFiles.clear();

        resetAdapter();
    }

    /**
     * Enables turn page on tap.
     *
     * @param enabled True if enabled
     */
    public void enableTurnPageOnTap(boolean enabled) {
        mDoTurnPageOnTap = enabled;
    }

    /**
     * Sets the text size using percentage.
     *
     * @param textSize The text size using percentage
     */
    public void setTextSizeInPercent(int textSize) {
        mScaleIndex = mDefaultScaleIndex;
        for (int i = 0; i <= mMaxIndex; i++) {
            if (textSize == Math.round(SCALES[i] * 100)) {
                mScaleIndex = i;
                return;
            }
        }
    }

    /**
     * @return The text size in percentage
     */
    public int getTextSizeInPercent() {
        return Math.round(SCALES[mScaleIndex] * 100);
    }

    /**
     * Zooms in.
     */
    public void zoomIn() {
        if (mScaleIndex == mMaxIndex) {
            return;
        }
        mScaleIndex++;
        resetAdapter();
    }

    /**
     * Zooms out.
     */
    public void zoomOut() {
        if (mScaleIndex == 0) {
            return;
        }
        mScaleIndex--;
        resetAdapter();
    }

    private void setTextZoom(ReflowWebView webView) {
        if (webView != null) {
            webView.getSettings().setTextZoom(Math.round(SCALES[mScaleIndex] * 100));
        }
    }

    /**
     * @return True if an internal link has been clicked
     */
    public boolean isInternalLinkClicked() {
        return mIsInternalLinkClicked;
    }

    /**
     * When {@link #isInternalLinkClicked()} is called, this should be called to reset that
     * an internal link has been clicked.
     */
    public void resetInternalLinkClicked() {
        mIsInternalLinkClicked = false;
    }

    public int getCurrentPage() {
        return mIsRtlMode ? mPageCount - mViewPager.getCurrentItem() : mViewPager.getCurrentItem() + 1;
    }

    public void setCurrentPage(int pageNum) {
        mViewPager.setCurrentItem(mIsRtlMode ? mPageCount - pageNum : pageNum - 1, false);
    }

    @Override
    public void startUpdate(@NonNull ViewGroup container) {
    }

    @Override
    public int getCount() {
        return mPageCount;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (sDebug) Log.d(TAG, "Removing page #" + (position + 1));
        FrameLayout frameLayout = (FrameLayout) object;
        frameLayout.removeAllViews();
        mViewHolders.put(position, null);
        container.removeView(frameLayout);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull final ViewGroup container, final int position) {
        FrameLayout layout = new FrameLayout(mContext);
        ReflowWebView webView = getReflowWebView();

        int pagePosition = mIsRtlMode ? mPageCount - 1 - position : position;
        String filename = mReflowFiles.get(pagePosition);
        boolean need_load_flag = true;
        if (filename != null) {
            File file = new File(filename);
            if (file.exists()) {
                need_load_flag = false;
                if (sDebug) Log.d(TAG, "the file at page #" + (position + 1) + " already received");
                webView.loadUrl("file:///" + filename);
                setTextZoom(webView);
            }
        }

        if (need_load_flag) {
            String loadingFile = LIGHT_MODE_LOADING_FILE;
            if (mColorMode == ColorMode.NightMode) {
                loadingFile = NIGHT_MODE_LOADING_FILE;
            } else if (mColorMode == ColorMode.CustomMode) {
                int r = (mBackgroundColorMode) & 0xFF;
                int g = (mBackgroundColorMode >> 8) & 0xFF;
                int b = (mBackgroundColorMode >> 16) & 0xFF;
                double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
                if (luminance < 128) {
                    loadingFile = DARK_MODE_LOADING_FILE;
                }
            }
            webView.loadUrl(loadingFile);
            boolean shouldUnlockRead = false;
            try {
                // request for reflow output
                if (sDebug) Log.d(TAG, "request for page #" + (pagePosition + 1));
                mDoc.lockRead();
                shouldUnlockRead = true;
                Page page = mDoc.getPage(pagePosition + 1);
                ReflowProcessor.getReflow(page, mRequestHandler, position);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(mDoc);
                }
            }
        }

        FrameLayout parent = (FrameLayout) webView.getParent();
        if (parent != null) {
            // note that we share WebViews,
            // so before adding a WebView make sure it's not a child of any layout
            parent.removeAllViews();
        }
        mViewHolders.put(position, webView);
        layout.addView(webView);
        container.addView(layout);
        return layout;
    }

    @Override
    public void RequestHandlerProc(RequestHandler.JobRequestResult result, String outFilename, Object customData) {
        // this function is called in a background thread, but update reflow should be done in the UI thread
        Thread.yield();
        Message msg = new Message();
        msg.setTarget(mUpdateReflowHandler);
        Vector<Object> v = new Vector<>();
        v.add(result);
        v.add(outFilename);
        v.add(customData);
        msg.obj = v;
        msg.sendToTarget();
    }

    private void updateReflow(RequestHandler.JobRequestResult result, final String outFilename, Object customData) {
        final int position = (int) customData;
        int pagePosition = mIsRtlMode ? mPageCount - 1 - position : position;

        ReflowWebView webView = mViewHolders.get(position);
        if (webView == null) {
            if (result == RequestHandler.JobRequestResult.FINISHED) {
                // save data for later use
                if (sDebug) Log.d(TAG, "initially received page #" + (position + 1));
                mReflowFiles.put(pagePosition, outFilename);
            }
            // had destroyed before the result was received
            return;
        }

        if (result == RequestHandler.JobRequestResult.FAILED) {
            // if there is an error in parsing show a blank page
            if (sDebug)
                Log.d(TAG, "error in parsing page " + outFilename + " (" + (position + 1) + ")");
            webView.loadUrl("about:blank");
            return;
        }

        if (result == RequestHandler.JobRequestResult.CANCELLED) {
            if (sDebug)
                Log.d(TAG, "cancelled parsing page " + outFilename + " (" + (position + 1) + ")");
            webView.loadUrl("about:blank");
            return;
        }

        if (sDebug) Log.d(TAG, "received page #" + (position + 1));
        mReflowFiles.put(pagePosition, outFilename);

        webView.loadUrl("file:///" + outFilename);
        setTextZoom(webView);
    }

    @Override
    public boolean onReflowWebViewScaleBegin(ScaleGestureDetector detector) {
        mScaleFactor = mLastScaleFactor = SCALES[mScaleIndex];
        mThConsecutiveScales = TH_SCAlE_GESTURE;
        mZoomInFlag = true;
        return true;
    }

    @Override
    public boolean onReflowWebViewScale(ScaleGestureDetector detector) {
        mScaleFactor *= detector.getScaleFactor();

        // avoid considering a small gesture as scaling
        if (Math.max(mLastScaleFactor / mScaleFactor, mScaleFactor / mLastScaleFactor) < TH_SCAlE_GESTURE) {
            return true;
        }

        if (mZoomInFlag && mScaleFactor > mLastScaleFactor && mScaleFactor / mLastScaleFactor < mThConsecutiveScales) {
            return true;
        }

        if (!mZoomInFlag && mLastScaleFactor > mScaleFactor && mLastScaleFactor / mScaleFactor < mThConsecutiveScales) {
            return true;
        }

        if (mLastScaleFactor > mScaleFactor) {
            if (mScaleIndex > 0) {
                mScaleIndex--;
                mLastScaleFactor = mScaleFactor = SCALES[mScaleIndex];
                if (mZoomInFlag) {
                    mThConsecutiveScales = TH_SCAlE_GESTURE;
                }
                mZoomInFlag = false;
            }
        } else {
            if (mScaleIndex < mMaxIndex) {
                mScaleIndex++;
                mLastScaleFactor = mScaleFactor = SCALES[mScaleIndex];
                if (!mZoomInFlag) {
                    mThConsecutiveScales = TH_SCAlE_GESTURE;
                }
                mZoomInFlag = true;
            }
        }

        int position = mViewPager.getCurrentItem();
        ReflowWebView webView = mViewHolders.get(position);
        if (webView == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Reflow: An error happened trying to retrieve a WebView"));
            Log.e(TAG, "An error happened trying to get WebView at position " + position);
            return false;
        }
        setTextZoom(webView);

        mThConsecutiveScales *= TH_SCAlE_GESTURE;
        return true;
    }

    @Override
    public void onReflowWebViewScaleEnd(ScaleGestureDetector detector) {
        resetAdapter();
    }

    @Override
    public void onReflowWebViewSingleTapUp(MotionEvent event) {
        if (mDoTurnPageOnTap) {
            int x = (int) (event.getX() + 0.5);
            float width = mViewPager.getWidth();
            float widthThresh = width * TAP_REGION_THRESHOLD;
            int curPosition = mViewPager.getCurrentItem();
            if (x <= widthThresh) {
                if (curPosition > 0) {
                    if (mClickHandler != null) {
                        mClickHandler.sendEmptyMessageDelayed(ClickHandler.GO_TO_PREVIOUS_PAGE, 200);
                    }
                    return;
                }
            } else if (x >= width - widthThresh) {
                if (curPosition < mPageCount - 1) {
                    if (mClickHandler != null) {
                        mClickHandler.sendEmptyMessageDelayed(ClickHandler.GO_TO_NEXT_PAGE, 200);
                    }
                    return;
                }
            }
        }
        if (mCallback != null) {
            mCallback.onReflowPagerSingleTapUp(event);
        }
    }

    void setOnPostProcessColorListener(ReflowControl.OnPostProcessColorListener listener) {
        mOnPostProcessColorListener = listener;
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }
}
