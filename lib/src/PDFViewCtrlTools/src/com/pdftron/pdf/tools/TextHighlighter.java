//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Highlights;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.asynctask.GenerateHighlightsTask;
import com.pdftron.pdf.tools.ToolManager.ToolMode;

import java.util.ArrayList;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * This class can be used to highlight all search results.
 */
@Keep
public class TextHighlighter extends Tool implements GenerateHighlightsTask.Callback {

    private static final String TAG = TextHighlighter.class.getName();

    /**
     * Range of pages whose highlights should be loaded as part of one batch.
     * This range counts only for one direction, forward or backwards,
     * so a batch will include twice the range of pages.
     */
    private static final int LOAD_PAGE_RANGE = 5;

    /**
     * Page is in a normal state that is "good" for drawing highlights.
     */
    private static final int STATE_IS_NORMAL = 0;
    /**
     * Page is being zoomed.
     */
    private static final int STATE_IS_ZOOMING = 1;
    /**
     * Page has been flung. Only used in continuous modes.
     */
    private static final int STATE_IS_FLUNG = 2;

    private Paint mSelPaintInDayMode = new Paint();
    private Paint mSelPaintInNightMode = new Paint();
    private Paint mHighlightPaintInDayMode = new Paint();
    private Paint mHighlightPaintInNightMode = new Paint();
    private Paint mSelPaint;
    private Paint mHighlightPen;

    private String mSearchPattern;
    private TextHighlighterSettings mSettings = new TextHighlighterSettings();

    private GenerateHighlightsTask mMultiPageTask;
    private SparseArray<GenerateHighlightsTask> mSinglePageTasks = new SparseArray<>();
    private SparseArray<PageHighlights> mPageHighlights = new SparseArray<>();
    private SparseArray<ArrayList<QuadPath>> mSelQuadPaths = new SparseArray<>();

    private boolean mIsRunning = false;
    private boolean mPathsClipped = false;
    private Rect mVisRect;
    private Rect mQuadRect;
    private RectF mTempRectF;
    private int mState = STATE_IS_NORMAL;
    private int mMaxTextureSize;

    /**
     * Class constructor
     */
    @SuppressWarnings("unused")
    public TextHighlighter(@NonNull PDFViewCtrl pdfViewCtrl) {
        super(pdfViewCtrl);

        mNextToolMode = getToolMode();

        init();
    }

    private void init() {
        // Set colors and blend modes
        mSelPaintInDayMode.setAntiAlias(true);
        mSelPaintInNightMode.setAntiAlias(true);
        mHighlightPaintInDayMode.setAntiAlias(true);
        mHighlightPaintInNightMode.setAntiAlias(true);
        mSelPaintInDayMode.setStyle(Paint.Style.FILL);
        mSelPaintInNightMode.setStyle(Paint.Style.FILL);
        mHighlightPaintInDayMode.setStyle(Paint.Style.FILL);
        mHighlightPaintInNightMode.setStyle(Paint.Style.FILL);
        setHighlightColors(mPdfViewCtrl.getContext().getResources().getColor(R.color.tools_text_highlighter_highlight_color),
            mPdfViewCtrl.getContext().getResources().getColor(R.color.tools_text_highlighter_highlight_color_inverse),
            mPdfViewCtrl.getContext().getResources().getColor(R.color.tools_text_highlighter_selection_color),
            mPdfViewCtrl.getContext().getResources().getColor(R.color.tools_text_highlighter_selection_color_inverse));

        try {
            mVisRect = new Rect();
            mQuadRect = new Rect();
        } catch (PDFNetException e) {
            Log.e(TAG, e.getMessage());
        }
        mTempRectF = new RectF();

        // Enable page turning (in non-continuous page presentation mode).
        mPdfViewCtrl.setBuiltInPageSlidingEnabled(true);

        // Get maximum texture size that can be drawn by OpenGL
        mMaxTextureSize = getMaxTextureSize();
    }

    /**
     * Sets the highlight colors.
     *
     * @param highlightColorInDayMode   highlight color in day mode
     * @param highlightColorInNightMode highlight color in night mode
     * @param selColorInDayMode         Selection color in day mode
     * @param selColorInNightMode       Selection color in night mode
     */
    @SuppressWarnings("WeakerAccess")
    public void setHighlightColors(int highlightColorInDayMode, int highlightColorInNightMode, int selColorInDayMode, int selColorInNightMode) {
        mHighlightPaintInNightMode.setColor(highlightColorInNightMode);
        mHighlightPaintInNightMode.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        mHighlightPaintInDayMode.setColor(highlightColorInDayMode);
        mHighlightPaintInDayMode.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
        mSelPaintInNightMode.setColor(selColorInNightMode);
        mSelPaintInNightMode.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
        mSelPaintInDayMode.setColor(selColorInDayMode);
        mSelPaintInDayMode.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));

        boolean isNightMode = ((ToolManager) mPdfViewCtrl.getToolManager()).isNightMode();
        mHighlightPen = isNightMode ? mHighlightPaintInNightMode : mHighlightPaintInDayMode;
        mSelPaint = isNightMode ? mSelPaintInNightMode : mSelPaintInDayMode;
    }

    /**
     * Starts highlighting text.
     * <p>
     * The search pattern doesn't need to match case or match the whole word.
     * See {@link #start(String, boolean, boolean, boolean)}.
     *
     * @param searchPattern The search pattern
     */
    // Reset settings and stop/clear tasks and highlights
    // NOTE: TextHighlighter must be start()'ed after calling reset()
    public void start(String searchPattern) {
        start(searchPattern, false, false, false);
    }

    /**
     * Starts highlighting text.
     *
     * @param searchPattern         The search pattern
     * @param matchCase             True if match case is enabled
     * @param matchWholeWords       True if whole word is enabled
     * @param useRegularExpressions True if regular expressions is enabled
     */
    // Reset settings and stop/clear tasks and highlights
    // NOTE: TextHighlighter must be start()'ed after calling reset()
    public void start(String searchPattern, boolean matchCase,
                      boolean matchWholeWords, boolean useRegularExpressions) {
        clear();

        mSearchPattern = searchPattern;
        mSettings.mMatchCase = matchCase;
        mSettings.mMatchWholeWords = matchWholeWords;
        mSettings.mUseRegularExpressions = useRegularExpressions;

        if (mPdfViewCtrl.getDoc() != null) {
            mIsRunning = true;
            // Highlight current page first to avoid waiting for longer search
            highlightPage(mPdfViewCtrl.getCurrentPage());
            highlightPageRange();
        }
    }

    /**
     * Clears highlighted text.
     */
    public void clear() {
        stop();
        mPageHighlights.clear();
        resetSearchPaths(true);
        resetHighlightPaths(true);
        mSearchPattern = null;
    }

    /**
     * Resets the search paths.
     *
     * @param all if true, all search-paths on all pages are reset; otherwise,
     *            only the visible pages' paths are reset.
     */
    @SuppressWarnings("WeakerAccess")
    protected void resetSearchPaths(boolean all) {
        if (all) {
            for (int i = 0; i < mSelQuadPaths.size(); i++) {
                ArrayList<QuadPath> quadPaths = mSelQuadPaths.valueAt(i);
                for (QuadPath quadPath : quadPaths) {
                    quadPath.mPath.reset();
                }
            }
        } else {
            for (int pageNum : mPdfViewCtrl.getVisiblePagesInTransition()) {
                ArrayList<QuadPath> quadPaths = mSelQuadPaths.get(pageNum);
                if (quadPaths != null) {
                    for (QuadPath quadPath : quadPaths) {
                        quadPath.mPath.reset();
                    }
                }
            }
        }
    }

    /**
     * @param all if true, all highlight-paths on all pages are reset; otherwise,
     *            only the visible pages' paths are reset.
     */
    @SuppressWarnings("WeakerAccess")
    protected void resetHighlightPaths(boolean all) {
        if (all) {
            for (int i = 0; i < mPageHighlights.size(); i++) {
                PageHighlights pgHlts = mPageHighlights.valueAt(i);
                for (QuadPath quadPath : pgHlts.mQuadPaths) {
                    quadPath.mPath.reset();
                }
            }
        } else {
            for (int pageNum : mPdfViewCtrl.getVisiblePagesInTransition()) {
                PageHighlights pgHlts = mPageHighlights.get(pageNum);
                if (pgHlts != null) {
                    for (QuadPath quadPath : pgHlts.mQuadPaths) {
                        quadPath.mPath.reset();
                    }
                }
            }
        }
    }

    /**
     * Resets the search paths.
     *
     * @param all if true, all clip-paths on all pages are reset; otherwise,
     *            only the visible pages' paths are reset.
     */
    @SuppressWarnings("WeakerAccess")
    protected void resetClipPaths(@SuppressWarnings("SameParameterValue") boolean all) {
        if (all) {
            // Reset highlight quads' clip flag
            for (int i = 0; i < mPageHighlights.size(); i++) {
                PageHighlights pgHlts = mPageHighlights.valueAt(i);
                for (QuadPath quadPath : pgHlts.mQuadPaths) {
                    quadPath.mClip = false;
                }
            }
            // Reset search quads' clip flag
            for (int i = 0; i < mSelQuadPaths.size(); i++) {
                ArrayList<QuadPath> quadPaths = mSelQuadPaths.valueAt(i);
                for (QuadPath quadPath : quadPaths) {
                    quadPath.mClip = false;
                }
            }
        } else {
            // Reset highlight quads' clip flag
            for (int pageNum : mPdfViewCtrl.getVisiblePagesInTransition()) {
                PageHighlights pgHlts = mPageHighlights.get(pageNum);
                if (pgHlts != null) {
                    for (QuadPath quadPath : pgHlts.mQuadPaths) {
                        quadPath.mClip = false;
                    }
                }
            }
            // Reset search quads' clip flag
            for (int pageNum : mPdfViewCtrl.getVisiblePagesInTransition()) {
                ArrayList<QuadPath> quadPaths = mSelQuadPaths.get(pageNum);
                if (quadPaths != null) {
                    for (QuadPath quadPath : quadPaths) {
                        quadPath.mClip = false;
                    }
                }
            }
        }
    }

    /**
     * Resets all clip-paths on all pages.
     */
    @SuppressWarnings("WeakerAccess")
    protected void resetClipPaths() {
        resetClipPaths(true);
    }

    /**
     * Cancel all tasks and indicate that the TextHighlighter has stopped.
     * This method should be used instead of just cancel() if the search will
     * not be continued.
     */
    public void stop() {
        cancel();
        mIsRunning = false;
    }

    /**
     * Cancel the running search task(s). The state of the TextHighlighter
     * will still be "running", so calling classes that depend on the
     * TextHighlighter running will still function the same way.
     */
    public void cancel() {
        if (mMultiPageTask != null) {
            mMultiPageTask.cancel(true);
        }
        for (int i = 0; i < mSinglePageTasks.size(); i++) {
            mSinglePageTasks.valueAt(i).cancel(true);
        }
    }

    /**
     * @return True if in highlighting process
     */
    public boolean isRunning() {
        return mIsRunning;
    }

    /**
     * @return The search pattern
     */
    public String getSearchPattern() {
        return mSearchPattern;
    }

    /**
     * Highlight text matches for on-screen pages and the previous and next pages.
     */
    private void highlightPageRange() {
        int pageStart = getReversePageRange();
        int pageEnd = getForwardPageRange();
        boolean shouldSearch = false;

        for (int pageNum = pageStart; pageNum <= pageEnd; pageNum++) {
            PageHighlights pageHlts = mPageHighlights.get(pageNum);
            if (pageHlts == null) {
                pageHlts = new PageHighlights();
                mPageHighlights.put(pageNum, pageHlts);
                shouldSearch = true;
            }
        }

        if (!shouldSearch) { // There were no pages without highlights in the range
            return;
        }
        // At least one page in range does not have highlights
        for (int pageNum = pageStart; pageNum <= pageEnd; pageNum++) {
            PageHighlights pageHlts = mPageHighlights.get(pageNum);
            // Indicate that one more task is generating highlights for this page
            pageHlts.mNumTasks++;
        }

        // Cancel previous task if not yet finished
        if (mMultiPageTask != null && mMultiPageTask.getStatus() != AsyncTask.Status.FINISHED) {
            mMultiPageTask.cancel(true);
        }
        mMultiPageTask = new GenerateHighlightsTask(mPdfViewCtrl, pageStart, pageEnd, mSearchPattern,
            mSettings.mMatchCase, mSettings.mMatchWholeWords, mSettings.mUseRegularExpressions);
        mMultiPageTask.setCallback(this);
        mMultiPageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Highlights the specified page.
     *
     * @param pageNum The page number to be highlighted
     */
    @SuppressWarnings("WeakerAccess")
    protected void highlightPage(int pageNum) {
        PageHighlights pageHlts = mPageHighlights.get(pageNum);
        if (pageHlts != null) {
            // Highlights for this page already exist or are being generated
            return;
        }
        pageHlts = new PageHighlights();
        pageHlts.mNumTasks++;
        mPageHighlights.put(pageNum, pageHlts);

        GenerateHighlightsTask task = mSinglePageTasks.get(pageNum);
        // Cancel previous single-page task for this page, if there is one still running
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            task.cancel(true);
        }
        task = new GenerateHighlightsTask(mPdfViewCtrl, pageNum, pageNum, mSearchPattern,
            mSettings.mMatchCase, mSettings.mMatchWholeWords, mSettings.mUseRegularExpressions);
        task.setCallback(this);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        mSinglePageTasks.put(pageNum, task);
    }

    /**
     * Updates search selection.
     */
    @SuppressWarnings("WeakerAccess")
    protected void updateSearchSelection() {
        resetSearchPaths(true);

        mSelQuadPaths.clear();

        int selPageBegin = mPdfViewCtrl.getSelectionBeginPage();
        int selPageEnd = mPdfViewCtrl.getSelectionEndPage();

        // Add one QuadPath to selQuadPath list for each selection quad
        for (int page = selPageBegin; page <= selPageEnd; page++) {
            if (!mPdfViewCtrl.hasSelectionOnPage(page)) {
                continue;
            }

            PDFViewCtrl.Selection sel = mPdfViewCtrl.getSelection(page);
            double[] quads = sel.getQuads();
            int quad_count = quads.length / 8;

            if (quad_count == 0) {
                continue;
            }
            // ArrayList to hold all search QuadPaths on this page
            ArrayList<QuadPath> quadPaths = new ArrayList<>();

            int k = 0;
            for (int j = 0; j < quad_count; ++j, k += 8) {
                QuadPath quadPath = new QuadPath(Arrays.copyOfRange(quads, k, k + 8));
                quadPaths.add(quadPath);
            }

            // Add quadPaths ArrayList to SparseArray
            mSelQuadPaths.put(page, quadPaths);
        }
    }

    /**
     * Forces an update of all visible highlights.
     */
    @SuppressWarnings("WeakerAccess")
    protected void updateVisibleHighlights() {
        resetHighlightPaths(false);
        // For non-cont. modes, ensure that highlights are drawn by resetting
        // the TextHighlighter state to STATE_IS_NORMAL
        if (!mPdfViewCtrl.isContinuousPagePresentationMode(mPdfViewCtrl.getPagePresentationMode())) {
            mState = STATE_IS_NORMAL;
        }
    }

    /**
     * Updates highlighted text
     */
    public void update() {
        updateSearchSelection();
        updateVisibleHighlights();
        mPdfViewCtrl.invalidate();
    }

    /**
     * Makes quad path.
     *
     * @param page     The page
     * @param quadPath The quad path
     */
    @SuppressWarnings("WeakerAccess")
    protected void makeQuadPath(int page, QuadPath quadPath) {
        double[] pts;
        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();
        float x, y;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

        PointF pt1, pt3, pt4;
        Rect tempRect;

        if (quadPath.mClip) {
            try {
                getVisibleRect(page);

                // Get quad rect in page space
                mQuadRect.set(quadPath.mQuads[2], quadPath.mQuads[3], quadPath.mQuads[6], quadPath.mQuads[7]);
                // Get intersection of quad rect with vis. rect
                if (!intersect(mQuadRect, mVisRect)) {
                    // Quad is off-screen, don't make path
                    return;
                }
                // Part of the quad rect is visible, and it has been clipped to vis. rect if required
                // Convert rect. from page space to screen space
                pts = mPdfViewCtrl.convPagePtToScreenPt(mQuadRect.getX1(), mQuadRect.getY1(), page);
                mTempRectF.left = (float) pts[0] + sx;
                mTempRectF.bottom = (float) pts[1] + sy;
                pts = mPdfViewCtrl.convPagePtToScreenPt(mQuadRect.getX2(), mQuadRect.getY2(), page);
                mTempRectF.right = (float) pts[0] + sx;
                mTempRectF.top = (float) pts[1] + sy;
                tempRect = new Rect(mTempRectF.left, mTempRectF.top, mTempRectF.right, mTempRectF.bottom);
                tempRect.normalize();
                mTempRectF = new RectF((float) tempRect.getX1(), (float) tempRect.getY1(), (float) tempRect.getX2(), (float) tempRect.getY2());
                quadPath.mPath.addRect(mTempRectF, Path.Direction.CW);
            } catch (PDFNetException e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            // Use RectF to help create Path, because using path.moveTo(), path.lineTo(), etc.
            // results in a black border around path when hardware acceleration is enabled.
            if (mPdfViewCtrl.isContinuousPagePresentationMode(mPdfViewCtrl.getPagePresentationMode())) {
                pts = mPdfViewCtrl.convPagePtToScreenPt(quadPath.mQuads[0], quadPath.mQuads[1], page);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
            } else {
                pts = mPdfViewCtrl.convPagePtToHorizontalScrollingPt(quadPath.mQuads[0], quadPath.mQuads[1], page);
                x = (float) pts[0];
                y = (float) pts[1];
            }
            mTempRectF.left = x;
            mTempRectF.bottom = y;
            pt1 = new PointF(x, y);
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;

            if (mPdfViewCtrl.isContinuousPagePresentationMode(mPdfViewCtrl.getPagePresentationMode())) {
                pts = mPdfViewCtrl.convPagePtToScreenPt(quadPath.mQuads[2], quadPath.mQuads[3], page);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
            } else {
                pts = mPdfViewCtrl.convPagePtToHorizontalScrollingPt(quadPath.mQuads[2], quadPath.mQuads[3], page);
                x = (float) pts[0];
                y = (float) pts[1];
            }
            mTempRectF.right = x;
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;

            if (mPdfViewCtrl.isContinuousPagePresentationMode(mPdfViewCtrl.getPagePresentationMode())) {
                pts = mPdfViewCtrl.convPagePtToScreenPt(quadPath.mQuads[4], quadPath.mQuads[5], page);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
            } else {
                pts = mPdfViewCtrl.convPagePtToHorizontalScrollingPt(quadPath.mQuads[4], quadPath.mQuads[5], page);
                x = (float) pts[0];
                y = (float) pts[1];
            }
            mTempRectF.top = y;
            pt3 = new PointF(x, y);
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;

            if (mPdfViewCtrl.isContinuousPagePresentationMode(mPdfViewCtrl.getPagePresentationMode())) {
                pts = mPdfViewCtrl.convPagePtToScreenPt(quadPath.mQuads[6], quadPath.mQuads[7], page);
                x = (float) pts[0] + sx;
                y = (float) pts[1] + sy;
            } else {
                pts = mPdfViewCtrl.convPagePtToHorizontalScrollingPt(quadPath.mQuads[6], quadPath.mQuads[7], page);
                x = (float) pts[0];
                y = (float) pts[1];
            }
            pt4 = new PointF(x, y);
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;

            try {
                Page page1 = mPdfViewCtrl.getDoc().getPage(page);
                if (page1.getRotation() == Page.e_90 ||
                    page1.getRotation() == Page.e_270 ||
                    mPdfViewCtrl.getPageRotation() == Page.e_90 ||
                    mPdfViewCtrl.getPageRotation() == Page.e_270) {
                    // flip the X and Y for rotated Page
                    tempRect = new Rect(pt1.x, pt1.y, pt3.x, pt3.y);
                } else {
                    tempRect = new Rect(pt1.x, pt1.y, pt3.x, pt4.y);
                }
                tempRect.normalize();
                mTempRectF = new RectF((float) tempRect.getX1(), (float) tempRect.getY1(), (float) tempRect.getX2(), (float) tempRect.getY2());
            } catch (PDFNetException e) {
                Log.e(TAG, e.getMessage());
            }
            quadPath.mPath.addRect(mTempRectF, Path.Direction.CW);

            // Check if path is too large to be rendered
            if (minX < Float.MAX_VALUE && minY < Float.MAX_VALUE &&
                maxX > Float.MIN_VALUE && maxY > Float.MIN_VALUE) {
                float pathWidth = maxX - minX;
                float pathHeight = maxY - minY;
                if (mMaxTextureSize > 0 && (pathWidth > mMaxTextureSize || pathHeight > mMaxTextureSize)) {
                    // Turn on clipping for this quad and re-make its path
                    quadPath.mClip = true;
                    quadPath.mPath.reset();
                    mPathsClipped = true; // indicate that a path has been clipped
                    makeQuadPath(page, quadPath);
                }
            }
        }
    }

    /**
     * @return the end of the page range.
     */
    private int getReversePageRange() {
        int prev = mPdfViewCtrl.getCurrentPage();

        // Loop until range or first page is reached
        for (int i = 0; i < LOAD_PAGE_RANGE && (prev - 1) > 0; i++) {
            prev--;
            if (mPageHighlights.get(prev) != null) {
                // page already has highlights, so stop range before it
                prev++;
                break;
            }
        }
        return prev;
    }

    /**
     * @return the end of the page range.
     */
    private int getForwardPageRange() {
        int pageCount = mPdfViewCtrl.getPageCount();
        int next = mPdfViewCtrl.getCurrentPage();

        // Loop until range or end page is reached
        for (int i = 0; i < LOAD_PAGE_RANGE && (next + 1) <= pageCount; i++) {
            next++;
            if (mPageHighlights.get(next) != null) {
                // page already has highlights, so stop range before it
                next--;
                break;
            }
        }
        return next;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.TEXT_HIGHLIGHTER;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Unknown;
    }

    /**
     * The overload implementation of {@link Tool#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        super.onDraw(canvas, tfm);

        for (int pageNum : mPdfViewCtrl.getVisiblePagesInTransition()) { // Make and/or draw search paths for visible pages
            ArrayList<QuadPath> quadPaths = mSelQuadPaths.get(pageNum);
            if (quadPaths != null) { // draw paths if not zooming
                for (QuadPath quadPath : quadPaths) {
                    if (quadPath.mPath.isEmpty() &&
                        mState != STATE_IS_FLUNG &&
                        mState != STATE_IS_ZOOMING) {
                        // Make paths if not already made and not being flung or page-turning
                        makeQuadPath(pageNum, quadPath);
                    }
                    if (mPdfViewCtrl.isMaintainZoomEnabled()) {
                        canvas.save();
                        try {
                            int dx = mPdfViewCtrl.getScrollXOffsetInTools(pageNum);
                            int dy = (mPdfViewCtrl.isCurrentSlidingCanvas(pageNum) ? 0 : mPdfViewCtrl.getSlidingScrollY()) - mPdfViewCtrl.getScrollYOffsetInTools(pageNum);
                            canvas.translate(dx, dy);
                            canvas.drawPath(quadPath.mPath, mSelPaint);
                        } finally {
                            canvas.restore();
                        }
                    } else {
                        canvas.drawPath(quadPath.mPath, mSelPaint);
                    }
                }
            }
        }

        for (int pageNum : mPdfViewCtrl.getVisiblePagesInTransition()) { // Make and/or draw highlight paths for visible pages
            PageHighlights pgHlts = mPageHighlights.get(pageNum);
            if (pgHlts != null && pgHlts.mLoaded) { // draw paths if not zooming
                for (QuadPath quadPath : pgHlts.mQuadPaths) {
                    if (quadPath.mPath.isEmpty() &&
                        mState != STATE_IS_FLUNG &&
                        mState != STATE_IS_ZOOMING) {
                        // Make paths if not already made and not being flung or page-turning
                        boolean skipCurrentPage = false;

                        if (mPdfViewCtrl.isSlidingWhileZoomed()) {
                            for (int pageNum2 : mPdfViewCtrl.getVisiblePages()) {
                                if (pageNum == pageNum2) {
                                    skipCurrentPage = true;
                                    break;
                                }
                            }
                        }
                        if (!skipCurrentPage) {
                            makeQuadPath(pageNum, quadPath);
                        }
                    }
                    ArrayList<QuadPath> selQuadPaths = mSelQuadPaths.get(pageNum);
                    boolean drawQuad = true;
                    if (selQuadPaths != null) {
                        for (QuadPath selQuadPath : selQuadPaths) {
                            if (selQuadPath.compareQuads(quadPath)) {
                                // Don't draw path: selection is already drawn
                                drawQuad = false;
                                break;
                            }
                        }
                    }
                    if (drawQuad) {
                        if (mPdfViewCtrl.isMaintainZoomEnabled()) {
                            canvas.save();
                            try {
                                int dx = mPdfViewCtrl.getScrollXOffsetInTools(pageNum);
                                int dy = (mPdfViewCtrl.isCurrentSlidingCanvas(pageNum) ? 0 : mPdfViewCtrl.getSlidingScrollY()) - mPdfViewCtrl.getScrollYOffsetInTools(pageNum);
                                canvas.translate(dx, dy);
                                canvas.drawPath(quadPath.mPath, mHighlightPen);
                            } finally {
                                canvas.restore();
                            }
                        } else {
                            canvas.drawPath(quadPath.mPath, mHighlightPen);
                        }
                    }
                }
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        // Force redraw of search and highlight paths by resetting paths
        resetSearchPaths(true);
        resetHighlightPaths(true);

        mPdfViewCtrl.invalidate();
    }

    /**
     * The overload implementation of {@link Tool#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        mState = STATE_IS_ZOOMING;
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onScaleEnd(float, float)}.
     */
    @Override
    public boolean onScaleEnd(float x, float y) {
        mState = STATE_IS_NORMAL;
        // Reset selection and highlight paths after scaling
        resetSearchPaths(true);
        resetHighlightPaths(true);
        mPathsClipped = false; // Reset clip-mode - will be determined in onDraw
        resetClipPaths(true);

        mPdfViewCtrl.invalidate();

        return super.onScaleEnd(x, y); // super calls invalidate: highlight paths will be re-made
    }

    /**
     * The overload implementation of {@link Tool#onScrollChanged(int, int, int, int)}.
     */
    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        if (mState != STATE_IS_FLUNG) {
            // Generate highlights for any visible pages without highlights
            for (int pageNum : mPdfViewCtrl.getVisiblePagesInTransition()) {
                PageHighlights pgHlts = mPageHighlights.get(pageNum);
                if (pgHlts == null) { // Load a new batch of page highlights
                    highlightPageRange();
                    break;
                }
            }
        }

        if (mPathsClipped) { // when clipping, redraw visible pages' highlights
            resetSearchPaths(false);
            resetHighlightPaths(false);
            if (mState != STATE_IS_FLUNG) {
                mPdfViewCtrl.invalidate();
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        boolean handled = super.onUp(e, priorEventMode);

        if (priorEventMode == PDFViewCtrl.PriorEventMode.FLING) {
            mState = STATE_IS_FLUNG;
        }

        return handled;
    }

    /**
     * The overload implementation of {@link Tool#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        mState = STATE_IS_NORMAL;

        for (int pageNum : mPdfViewCtrl.getVisiblePagesInTransition()) {
            PageHighlights pgHlts = mPageHighlights.get(pageNum);
            if (pgHlts == null) { // Load a new batch of page highlights
                highlightPageRange();
                break;
            }
        }

        if (mPathsClipped) {
            mPdfViewCtrl.invalidate();
        }

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onPageTurning(int, int)}.
     */
    @Override
    public void onPageTurning(int old_page, int cur_page) {
        mState = STATE_IS_NORMAL;

        // Cleanup any paths in incorrect positions
        resetSearchPaths(false);
        resetHighlightPaths(false);
        mPdfViewCtrl.invalidate();
    }

    /**
     * The overload implementation of {@link Tool#onClose()}.
     */
    @Override
    public void onClose() {
        super.onClose();
        clear();
    }

    /**
     * The overload implementation of {@link Tool#onDoubleTapZoomAnimationBegin()}.
     */
    @Override
    public void onDoubleTapZoomAnimationBegin() {
        mState = STATE_IS_ZOOMING;
        resetSearchPaths(true);
        resetHighlightPaths(true);
        mPdfViewCtrl.invalidate();
    }

    /**
     * The overload implementation of {@link Tool#onDoubleTapZoomAnimationEnd()}.
     */
    @Override
    public void onDoubleTapZoomAnimationEnd() {
        mState = STATE_IS_NORMAL;
        mPathsClipped = false; // Reset clip-mode - will be determined in onDraw
        resetClipPaths();

        mPdfViewCtrl.invalidate();
    }

    /**
     * If the two rectangles intersect, set the first rectangle to their intersection.
     *
     * @return True if the rectangles intersect (and rect1 is set to their intersection),
     * false otherwise and do not change rect1.
     */
    private boolean intersect(Rect rect1, Rect rect2) {
        try {
            rect1.normalize();
            rect2.normalize();
            if (rect1.intersectRect(rect1, rect2)) {
                if (rect1.getX1() < rect2.getX1()) {
                    rect1.setX1(rect2.getX1());
                }
                if (rect1.getY1() < rect2.getY1()) {
                    rect1.setY1(rect2.getY1());
                }
                if (rect1.getX2() > rect2.getX2()) {
                    rect1.setX2(rect2.getX2());
                }
                if (rect1.getY2() > rect2.getY2()) {
                    rect1.setY2(rect2.getY2());
                }
                return true;
            }
        } catch (PDFNetException e) {
            Log.e(TAG, e.getMessage());
        }
        return false;
    }

    /**
     * Gets the visible rectangle, in page space.
     */
    private void getVisibleRect(int pageNum) {
        double[] visPts1 = mPdfViewCtrl.convScreenPtToPagePt(0, mPdfViewCtrl.getHeight(), pageNum); // lower-left corner
        double[] visPts2 = mPdfViewCtrl.convScreenPtToPagePt(mPdfViewCtrl.getWidth(), 0, pageNum); // upper-right corner
        try {
            mVisRect.set(visPts1[0], visPts1[1], visPts2[0], visPts2[1]);
        } catch (PDFNetException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Gets the maximum texture size that can be drawn by the OpenGL renderer.
     * See: http://stackoverflow.com/questions/7428996/hw-accelerated-activity-how-to-get-opengl-texture-size-limit*
     */
    @SuppressWarnings("WeakerAccess")
    protected static int getMaxTextureSize() {
        // Safe minimum default size
        final int IMAGE_MAX_BITMAP_DIMENSION = 2048;

        // Get EGL Display
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        // Initialise
        int[] version = new int[2];
        egl.eglInitialize(display, version);

        // Query total number of configurations
        int[] totalConfigurations = new int[1];
        egl.eglGetConfigs(display, null, 0, totalConfigurations);

        // Query actual list configurations
        EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
        egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

        int[] textureSize = new int[1];
        int maximumTextureSize = 0;

        // Iterate through all the configurations to located the maximum texture size
        for (int i = 0; i < totalConfigurations[0]; i++) {
            // Only need to check for width since opengl textures are always squared
            egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);

            // Keep track of the maximum texture size
            if (maximumTextureSize < textureSize[0])
                maximumTextureSize = textureSize[0];
        }

        // Release
        egl.eglTerminate(display);

        // Return largest texture size found, or default
        return Math.max(maximumTextureSize, IMAGE_MAX_BITMAP_DIMENSION);
    }

    /**
     * The overload implementation of {@link Tool#onNightModeUpdated(boolean)}.
     */
    @Override
    public void onNightModeUpdated(boolean isNightMode) {
        mHighlightPen = isNightMode ? mHighlightPaintInNightMode : mHighlightPaintInDayMode;
        mSelPaint = isNightMode ? mSelPaintInNightMode : mSelPaintInDayMode;
    }

    @Override
    public void onHighlightsTaskCancelled(int pageStart, int pageEnd) {
        for (int pageNum = pageStart; pageNum <= pageEnd; pageNum++) {
            // Reset page highlights
            PageHighlights pgHlts = mPageHighlights.get(pageNum);
            if (pgHlts != null) {
                if (!pgHlts.mLoaded && pgHlts.mNumTasks > 1) {
                    // Highlights for this page are being generated by tasks other than this,
                    // so do not remove from list
                    pgHlts.mNumTasks--;
                } else {
                    // Remove PageHighlights for this page, since they are no longer being generated
                    mPageHighlights.remove(pageNum);
                }
            }
        }
    }

    @Override
    public void onHighlightsTaskFinished(Highlights[] highlights, int pageStart, int pageEnd) {
        PageHighlights pageHlts;
        PDFDoc doc = mPdfViewCtrl.getDoc();
        for (int i = 0, cnt = highlights.length; i < cnt; i++) {
            pageHlts = mPageHighlights.get(pageStart + i);

            if (pageHlts == null) { // Should never happen
                Log.e(TAG, "Page result for page " + (pageStart + i) + " is null");
                return;
            }

            pageHlts.mQuadPaths.clear(); // Remove any old entries from QuadPath list

            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;

                highlights[i].begin(doc);
                // Add one QuadPath to PageHighlight's list for each highlight quad
                while (highlights[i].hasNext()) {
                    double[] quads = highlights[i].getCurrentQuads();
                    int quad_count = quads.length / 8;

                    if (quad_count == 0) {
                        highlights[i].next();
                        continue;
                    }

                    int k = 0;
                    for (int j = 0; j < quad_count; ++j, k += 8) {
                        QuadPath quadPath = new QuadPath(Arrays.copyOfRange(quads, k, k + 8));
                        pageHlts.mQuadPaths.add(quadPath);
                    }

                    highlights[i].next();
                }
            } catch (PDFNetException e) {
                continue;
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }

            pageHlts.mLoaded = true;
            pageHlts.mNumTasks--;
        }
        // Trigger draw update if viewer is in "good" state
        if (mState == STATE_IS_NORMAL) {
            mPdfViewCtrl.invalidate();
        }
    }

    private class PageHighlights {
        ArrayList<QuadPath> mQuadPaths = new ArrayList<>();
        boolean mLoaded;
        /**
         * Number of GenerateHighlightsTasks that are working on this page.
         */
        int mNumTasks;
    }

    private class QuadPath {
        double[] mQuads; // Array of 4 (x,y) coordinate pairs
        Path mPath;
        boolean mClip;

        QuadPath(double[] quads) {
            mQuads = quads;
            mPath = new Path();
            mClip = false;
        }

        boolean compareQuads(QuadPath quadPath) {
            if (quadPath != null) {
                if (this.mQuads.length == 8 && quadPath.mQuads.length == 8) {
                    for (int i = 0; i < 8; i++) {
                        if (this.mQuads[i] != quadPath.mQuads[i]) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }
    }

    private class TextHighlighterSettings {
        boolean mMatchCase;
        boolean mMatchWholeWords;
        boolean mUseRegularExpressions;
    }
}
