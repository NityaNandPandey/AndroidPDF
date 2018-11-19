//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.QuadPoint;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Highlight;
import com.pdftron.pdf.annots.Popup;
import com.pdftron.pdf.annots.Squiggly;
import com.pdftron.pdf.annots.StrikeOut;
import com.pdftron.pdf.annots.TextMarkup;
import com.pdftron.pdf.annots.Underline;
import com.pdftron.pdf.tools.DialogAnnotNote.DialogAnnotNoteListener;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * This class is responsible for editing text markup: highlight/strikeout/underline, e.g., moving and resizing.
 */
@Keep
public class AnnotEditTextMarkup extends TextSelect implements DialogAnnotNoteListener {

    private static final String TAG = AnnotEditTextMarkup.class.getName();
    private boolean mScaled;
    private boolean mModifiedAnnot;
    private boolean mCtrlPtsSet;
    private boolean mOnUpCalled = false;

    /**
     * Class constructor
     */
    public AnnotEditTextMarkup(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mScaled = false;
        mModifiedAnnot = false;
        mCtrlPtsSet = false;
    }

    /**
     * The overload implementation of {@link TextSelect#onCreate()}.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (mAnnot != null) {
            boolean shouldUnlockRead = false;
            try {
                // Locks the document first as accessing annotation/doc information isn't thread
                // safe. Since we are not going to modify the doc here, we can use the read lock.
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;

                mHasSelectionPermission = hasPermission(mAnnot, ANNOT_PERMISSION_SELECTION);
                mHasMenuPermission = hasPermission(mAnnot, ANNOT_PERMISSION_MENU);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
        }
    }

    @Override
    protected QuickMenu createQuickMenu() {
        QuickMenu quickMenu = new QuickMenu(mPdfViewCtrl, mHasMenuPermission);
        quickMenu.inflate(R.menu.annot_edit_text_markup);
        customizeQuickMenuItems(quickMenu);
        quickMenu.initMenuEntries();
        return quickMenu;
    }


    /**
     * The overload implementation of {@link Tool#isEditAnnotTool()}
     *
     * @return true
     */
    @Override
    public boolean isEditAnnotTool() {
        return true;
    }

    /**
     * The overload implementation of {@link TextSelect#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {

        int x = (int) (e.getX() + 0.5);
        int y = (int) (e.getY() + 0.5);

        if (mAnnot != null) {
            Annot tempAnnot = mPdfViewCtrl.getAnnotationAt(x, y);
            if (mAnnot.equals(tempAnnot)) {
                // Single clicked within the annotation, set the control points, draw the widget and
                // show the menu.
                mNextToolMode = ToolMode.ANNOT_EDIT_TEXT_MARKUP;
                if (!mCtrlPtsSet) {
                    setCtrlPts(false);
                }
                if (!mOnUpCalled) {
                    showMenu(getQMAnchorRect());
                }
            } else {
                // Otherwise goes back to the default tool mode.
                setNextToolMode(mCurrentDefaultToolMode);
            }
        } else {
            mNextToolMode = mCurrentDefaultToolMode;
        }
        return false;
    }

    /**
     * The overload implementation of {@link TextSelect#onPageTurning(int, int)}.
     */
    @Override
    public void onPageTurning(int old_page, int cur_page) {
        super.onPageTurning(old_page, cur_page);
    }

    /**
     * The overload implementation of {@link DialogAnnotNoteListener#onAnnotButtonPressed(int)}.
     */
    @Override
    public void onAnnotButtonPressed(int button) {
    }

    /**
     * The overload implementation of {@link TextSelect#onQuickMenuClicked(QuickMenuItem)}.
     */
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        if (!mPdfViewCtrl.hasSelection() && mAnnot != null) {
            selectAnnot(mAnnot, mAnnotPageNum);
        }
        if (menuItem.getItemId() != R.id.qm_highlight &&
            menuItem.getItemId() != R.id.qm_strikeout &&
            menuItem.getItemId() != R.id.qm_underline &&
            menuItem.getItemId() != R.id.qm_squiggly) {
            // these are handled differently than TextSelect
            if (super.onQuickMenuClicked(menuItem)) {
                return true;
            }
        }
        if (mAnnot != null) {
            // Delete the annotation
            if (menuItem.getItemId() == R.id.qm_delete) {
                deleteAnnot();
            } else if (menuItem.getItemId() == R.id.qm_highlight
                || menuItem.getItemId() == R.id.qm_underline
                || menuItem.getItemId() == R.id.qm_strikeout
                || menuItem.getItemId() == R.id.qm_squiggly) {
                changeAnnotType(menuItem);
                setNextToolMode(mCurrentDefaultToolMode);
            }
            // Add note to the annotation
            else if (menuItem.getItemId() == R.id.qm_note || menuItem.getItemId() == R.id.qm_appearance) {
                mNextToolMode = ToolMode.ANNOT_EDIT;
                return false;
            } else if (menuItem.getItemId() == R.id.qm_flatten) {
                handleFlattenAnnot();

                mNextToolMode = mCurrentDefaultToolMode;
            }

            // Update MRU and overflow menu
        } else {
            mNextToolMode = ToolMode.PAN;
        }
        return true;
    }

    /**
     * Change Annotation type based on quick menu item
     *
     * @param menuItem quick menu item
     */
    protected void changeAnnotType(QuickMenuItem menuItem) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "changeAnnotType");
        bundle.putStringArray(KEYS, new String[]{"menuItemId"});
        bundle.putInt("menuItemId", menuItem.getItemId());
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread
            // safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
            if (menuItem.getItemId() == R.id.qm_highlight) {
                mAnnot.getSDFObj().putName("Subtype", "Highlight");
            } else if (menuItem.getItemId() == R.id.qm_underline) {
                mAnnot.getSDFObj().putName("Subtype", "Underline");

            } else if (menuItem.getItemId() == R.id.qm_strikeout) {
                mAnnot.getSDFObj().putName("Subtype", "StrikeOut");
            } else {
                mAnnot.getSDFObj().putName("Subtype", "Squiggly");
            }
            mAnnot.refreshAppearance();
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            // make sure to raise modification event after mPdfViewCtrl.update and before unsetAnnot
            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * The overload implementation of {@link TextSelect#deleteAnnot()}.
     */
    @Override
    protected void deleteAnnot() {
        super.deleteAnnot();
        setNextToolMode(mCurrentDefaultToolMode);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.ANNOT_EDIT_TEXT_MARKUP;
    }

    /**
     * The overload implementation of {@link TextSelect#selectAnnot(Annot, int)}.
     */
    @Override
    public void selectAnnot(Annot annot, int pageNum) {
        //need to execute selectAnnot from Tool.java first -- do this through an instance of Pan tool

        mNextToolMode = ToolMode.ANNOT_EDIT_TEXT_MARKUP;
        setCtrlPts();

        showMenu(getQMAnchorRect());
    }

    /**
     * The overload implementation of {@link TextSelect#onScrollChanged(int, int, int, int)}.
     */
    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        if (mAnnot != null && (Math.abs(t - oldt) <= 1) && !isQuickMenuShown()) {
            showMenu(getQMAnchorRect());
        }
    }

    /**
     * The overload implementation of {@link TextSelect#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);
        mOnUpCalled = false;

        if (mAnnot != null && !isInsideAnnot(e) && mEffSelWidgetId < 0) {
            unsetAnnot();
            setNextToolMode(mCurrentDefaultToolMode);
        }

        return false;
    }

    /**
     * The overload implementation of {@link TextSelect#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        if (mScaled) {
            // Scaled and if while moving, disable moving to avoid complications.
            return false;
        }
        if (!mHasSelectionPermission) {
            // does not have permission to modify annotation
            return false;
        }
        if (mEffSelWidgetId < 0) {
            if (mBeingLongPressed) {
                mModifiedAnnot = true;
            }
        } else {
            mModifiedAnnot = true;
        }
        return super.onMove(e1, e2, x_dist, y_dist);
    }

    /**
     * The overload implementation of {@link TextSelect#onKeyUp(int, KeyEvent)}.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isQuickMenuShown() && hasMenuEntry((R.id.qm_delete)) && ShortcutHelper.isDeleteAnnot(keyCode, event)) {
            closeQuickMenu();
            deleteAnnot();
            return true;
        }

        if (mPdfViewCtrl.hasSelection() && isQuickMenuShown()) {
            if ((hasMenuEntry(R.id.qm_type) || hasMenuEntry(R.id.qm_highlight)) && ShortcutHelper.isHighlightAnnot(keyCode, event)) {
                onQuickMenuClicked(new QuickMenuItem(mPdfViewCtrl.getContext(), R.id.qm_highlight));
                return true;
            }

            if ((hasMenuEntry(R.id.qm_type) || hasMenuEntry(R.id.qm_underline)) && ShortcutHelper.isUnderlineAnnot(keyCode, event)) {
                onQuickMenuClicked(new QuickMenuItem(mPdfViewCtrl.getContext(), R.id.qm_underline));
                return true;
            }

            if ((hasMenuEntry(R.id.qm_type) || hasMenuEntry(R.id.qm_strikeout)) && ShortcutHelper.isStrikethroughAnnot(keyCode, event)) {
                onQuickMenuClicked(new QuickMenuItem(mPdfViewCtrl.getContext(), R.id.qm_strikeout));

                return true;
            }

            if ((hasMenuEntry(R.id.qm_type) || hasMenuEntry(R.id.qm_squiggly)) && ShortcutHelper.isSquigglyAnnot(keyCode, event)) {
                onQuickMenuClicked(new QuickMenuItem(mPdfViewCtrl.getContext(), R.id.qm_squiggly));
                return true;
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * The overload implementation of {@link TextSelect#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        super.onUp(e, priorEventMode);
        mNextToolMode = ToolMode.ANNOT_EDIT_TEXT_MARKUP;
        mScaled = false;
        mOnUpCalled = true;

        if (mAnnot != null) {
            if (mModifiedAnnot) {
                mModifiedAnnot = false;
                expandTextMarkup();
            }
            if (!mCtrlPtsSet) {
                setCtrlPts(false);
            }

            showMenu(getQMAnchorRect());

            // Don't let the main view scroll
            return (priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING || priorEventMode == PDFViewCtrl.PriorEventMode.FLING);
        } else {
            return false;
        }
    }

    /**
     * The overload implementation of {@link TextSelect#onLongPress(MotionEvent)}.
     */
    @Override
    public boolean onLongPress(MotionEvent e) {
        super.onLongPress(e);
        if (mAnnot != null) {
            mNextToolMode = ToolMode.ANNOT_EDIT_TEXT_MARKUP;
            setCtrlPts();
        }
        return false;
    }

    /**
     * The overload implementation of {@link TextSelect#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        super.onDraw(canvas, tfm);
    }

    /**
     * The overload implementation of {@link TextSelect#onScaleEnd(float, float)}.
     */
    @Override
    public boolean onScaleEnd(float x, float y) {
        super.onScaleEnd(x, y);

        if (mAnnot != null) {
            // Scaled and if while moving, disable moving and set the control points back to where
            // the annotation is; this is to avoid complications.
            mScaled = true;
            setCtrlPts();
        }
        return false;
    }

    /**
     * The overload implementation of {@link TextSelect#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        super.onFlingStop();

        if (mAnnot != null) {
            //mPdfViewCtrl.invalidate(mInvalidateBBox);
        }
        return false;
    }

    /**
     * The overload implementation of {@link TextSelect#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mAnnot != null) {
            if (!mPdfViewCtrl.isContinuousPagePresentationMode(mPdfViewCtrl.getPagePresentationMode())) {
                if (mAnnotPageNum != mPdfViewCtrl.getCurrentPage()) {
                    // Now in single page mode, and the annotation is not on this page, quit this
                    // tool mode.
                    unsetAnnot();
                    mNextToolMode = mCurrentDefaultToolMode;
                    setCtrlPts();
                    closeQuickMenu();
                    return;
                }
            }

            setCtrlPts();
            if (isQuickMenuShown() && changed) {
                closeQuickMenu();
                showMenu(getQMAnchorRect());
            }
        }
    }

    /**
     * The overload implementation of {@link TextSelect#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mAnnot != null) {
            mNextToolMode = getToolMode();
        } else {
            setNextToolMode(mCurrentDefaultToolMode);
        }
    }

    private void setCtrlPts() {
        setCtrlPts(false);
    }

    private void setCtrlPts(boolean bySnap) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putStringArray(KEYS, new String[]{"bySnap"});
        bundle.putBoolean("bySnap", bySnap);
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        mCtrlPtsSet = true;
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            double[] pts1 = new double[2];
            double[] pts3 = new double[2];

            double[] screen1 = new double[2];
            double[] screen2 = new double[2];
            double[] screen3 = new double[2];
            double[] screen4 = new double[2];

            TextMarkup markup = new TextMarkup(mAnnot);
            if (markup.isValid()) {
                int count;
                QuadPoint pt1 = null;
                QuadPoint pt2 = null;
                try {
                    count = markup.getQuadPointCount();
                    pt1 = markup.getQuadPoint(0);
                    pt2 = markup.getQuadPoint(count - 1);
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                }

                if (pt1 != null && pt2 != null) {
                    screen1 = mPdfViewCtrl.convPagePtToScreenPt(pt1.p1.x, pt1.p1.y, mAnnotPageNum);
                    screen2 = mPdfViewCtrl.convPagePtToScreenPt(pt1.p2.x, pt1.p2.y, mAnnotPageNum);
                    screen3 = mPdfViewCtrl.convPagePtToScreenPt(pt1.p3.x, pt1.p3.y, mAnnotPageNum);
                    screen4 = mPdfViewCtrl.convPagePtToScreenPt(pt1.p4.x, pt1.p4.y, mAnnotPageNum);

                    double minX = Math.min(Math.min(screen1[0], screen2[0]), Math.min(screen3[0], screen4[0]));
                    double maxX = Math.max(Math.max(screen1[0], screen2[0]), Math.max(screen3[0], screen4[0]));
                    double minY = Math.min(Math.min(screen1[1], screen2[1]), Math.min(screen3[1], screen4[1]));
                    double maxY = Math.max(Math.max(screen1[1], screen2[1]), Math.max(screen3[1], screen4[1]));

                    boolean rightToLeft = mPdfViewCtrl.getRightToLeftLanguage();

                    double newX1 = minX;
                    if (rightToLeft) {
                        newX1 = maxX;
                    }
                    double newY1 = 0.6 * maxY + 0.4 * minY;

                    screen1 = mPdfViewCtrl.convPagePtToScreenPt(pt2.p1.x, pt2.p1.y, mAnnotPageNum);
                    screen2 = mPdfViewCtrl.convPagePtToScreenPt(pt2.p2.x, pt2.p2.y, mAnnotPageNum);
                    screen3 = mPdfViewCtrl.convPagePtToScreenPt(pt2.p3.x, pt2.p3.y, mAnnotPageNum);
                    screen4 = mPdfViewCtrl.convPagePtToScreenPt(pt2.p4.x, pt2.p4.y, mAnnotPageNum);

                    minX = Math.min(Math.min(screen1[0], screen2[0]), Math.min(screen3[0], screen4[0]));
                    maxX = Math.max(Math.max(screen1[0], screen2[0]), Math.max(screen3[0], screen4[0]));
                    minY = Math.min(Math.min(screen1[1], screen2[1]), Math.min(screen3[1], screen4[1]));
                    maxY = Math.max(Math.max(screen1[1], screen2[1]), Math.max(screen3[1], screen4[1]));

                    double newX2 = maxX;
                    if (rightToLeft) {
                        newX2 = minX;
                    }
                    double newY2 = 0.4 * maxY + 0.6 * minY;

                    pts1 = new double[]{newX1, newY1};
                    pts3 = new double[]{newX2, newY2};
                }
                selectText((float) pts1[0], (float) pts1[1], (float) pts3[0], (float) pts3[1], false, bySnap);
                mSelWidgetEnabled = true;
                mPdfViewCtrl.invalidate(mInvalidateBBox);
            }

        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    private void expandTextMarkup() {
        if (mAnnot == null || onInterceptAnnotationHandling(mAnnot)) {
            return;
        }

        if (mPdfViewCtrl.hasSelection()) {
            int sel_pg_begin = mPdfViewCtrl.getSelectionBeginPage();
            int sel_pg_end = mPdfViewCtrl.getSelectionEndPage();
            int sel_up_page = mAnnotPageNum;

            class AnnotUpdateInfo {
                private AnnotUpdateInfo(TextMarkup textMarkup, int pageNum, com.pdftron.pdf.Rect rect,
                                        boolean isAdded, boolean isModified) {
                    mTextMarkup = textMarkup;
                    mPageNum = pageNum;
                    mRect = rect;
                    mIsAdded = isAdded;
                    mIsModified = isModified;
                }

                private TextMarkup mTextMarkup;
                private int mPageNum;
                private com.pdftron.pdf.Rect mRect;
                private boolean mIsAdded;
                private boolean mIsModified;
            }

            LinkedList<AnnotUpdateInfo> updateInfoList = new LinkedList<>();

            boolean multiPageSel = false;
            boolean shouldUnlock = false;
            try {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                PDFDoc doc = mPdfViewCtrl.getDoc();

                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                boolean useAdobeHack = toolManager.isTextMarkupAdobeHack();

                for (int pg = sel_pg_begin; pg <= sel_pg_end; ++pg) {
                    PDFViewCtrl.Selection sel = mPdfViewCtrl.getSelection(pg);
                    double[] quads = sel.getQuads();
                    int sz = quads.length / 8;
                    if (sz == 0) {
                        continue;
                    }

                    Point p1 = new Point();
                    Point p2 = new Point();
                    Point p3 = new Point();
                    Point p4 = new Point();
                    QuadPoint qp = new QuadPoint(p1, p2, p3, p4);

                    TextMarkup tm = new TextMarkup();
                    com.pdftron.pdf.Rect bbox = new com.pdftron.pdf.Rect(quads[0], quads[1], quads[4], quads[5]);//just use the first quad to temporarily populate the bbox

                    if (pg == mAnnotPageNum) {
                        raiseAnnotationPreModifyEvent(mAnnot, pg);
                        tm = new TextMarkup(mAnnot);

                        // Compute the old annotation position in screen space for update
                        double[] pts1, pts2;
                        Rect r = tm.getRect();
                        pts1 = mPdfViewCtrl.convPagePtToScreenPt(r.getX1(), r.getY1(), mAnnotPageNum);
                        pts2 = mPdfViewCtrl.convPagePtToScreenPt(r.getX2(), r.getY2(), mAnnotPageNum);
                        com.pdftron.pdf.Rect old_update_rect = new com.pdftron.pdf.Rect(pts1[0], pts1[1], pts2[0], pts2[1]);
                        old_update_rect.normalize();
                        updateInfoList.add(new AnnotUpdateInfo(null, 0, old_update_rect, false, false));

                        // If new selection is smaller than the old selection
                        // we need to update both the QuadPoints and Rect
                        // the simplest way is to erase the key from the SDF.Obj, then repopulate them
                        tm.getSDFObj().erase("QuadPoints");
                        tm.getSDFObj().erase("Rect");

                        tm.setRect(bbox);

                        if (((ToolManager) mPdfViewCtrl.getToolManager()).isCopyAnnotatedTextToNoteEnabled() &&
                            Utils.isTextCopy(mAnnot)) {
                            // create note from selected text
                            try {
                                Popup p = tm.getPopup();
                                if (p == null || !p.isValid()) {
                                    p = Popup.create(mPdfViewCtrl.getDoc(), tm.getRect());
                                    p.setParent(tm);
                                    tm.setPopup(p);
                                }
                                String content = getHighlightedText(pg);
                                p.setContents(content);
                            } catch (Exception e) {
                                AnalyticsHandlerAdapter.getInstance().sendException(e);
                            }
                        }
                    } else {
                        if (mAnnot.getType() == Annot.e_Highlight) {
                            tm = Highlight.create(doc, bbox);
                        } else if (mAnnot.getType() == Annot.e_Underline) {
                            tm = Underline.create(doc, bbox);
                        } else if (mAnnot.getType() == Annot.e_StrikeOut) {
                            tm = StrikeOut.create(doc, bbox);
                        } else if (mAnnot.getType() == Annot.e_Squiggly) {
                            tm = Squiggly.create(doc, bbox);
                        }
                        multiPageSel = true;
                    }

                    boolean isModified = false;
                    boolean isAdded = false;
                    int k = 0;
                    for (int i = 0; i < sz; ++i, k += 8) {
                        p1.x = quads[k];
                        p1.y = quads[k + 1];

                        p2.x = quads[k + 2];
                        p2.y = quads[k + 3];

                        p3.x = quads[k + 4];
                        p3.y = quads[k + 5];

                        p4.x = quads[k + 6];
                        p4.y = quads[k + 7];

                        if (useAdobeHack) {
                            qp.p1 = p4;
                            qp.p2 = p3;
                            qp.p3 = p1;
                            qp.p4 = p2;
                        } else {
                            qp.p1 = p1;
                            qp.p2 = p2;
                            qp.p3 = p3;
                            qp.p4 = p4;
                        }

                        tm.setQuadPoint(i, qp);
                        isModified = true;
                    }

                    if (pg != mAnnotPageNum) {
                        TextMarkup org_tm = new TextMarkup(mAnnot);
                        tm.setColor(org_tm.getColorAsRGB(), 3);
                        tm.setBorderStyle(org_tm.getBorderStyle());
                        tm.setOpacity(org_tm.getOpacity());
                        tm.setContents(org_tm.getContents());
                        setAuthor(tm);
                        Page page = mPdfViewCtrl.getDoc().getPage(pg);
                        int index = TextMarkupCreate.getAnnotIndexForAddingMarkup(page);
                        page.annotInsert(index, tm);

                        setAnnot(tm, pg);
                        sel_up_page = pg;

                        if (((ToolManager) mPdfViewCtrl.getToolManager()).isCopyAnnotatedTextToNoteEnabled() &&
                            Utils.isTextCopy(mAnnot)) {
                            // create note from selected text
                            try {
                                Popup p = tm.getPopup();
                                if (p == null || !p.isValid()) {
                                    p = Popup.create(mPdfViewCtrl.getDoc(), tm.getRect());
                                    p.setParent(tm);
                                    tm.setPopup(p);
                                }
                                p.setContents(getHighlightedText(pg));
                            } catch (Exception e) {
                                AnalyticsHandlerAdapter.getInstance().sendException(e);
                            }
                        }

                        isAdded = true;
                    }

                    tm.refreshAppearance();

                    // Compute the bbox of the annotation in screen space
                    Rect r = tm.getRect();
                    com.pdftron.pdf.Rect ur = new com.pdftron.pdf.Rect();
                    r.normalize();
                    double[] pts;
                    pts = mPdfViewCtrl.convPagePtToScreenPt(r.getX1(), r.getY2(), pg);
                    ur.setX1(pts[0]);
                    ur.setY1(pts[1]);
                    pts = mPdfViewCtrl.convPagePtToScreenPt(r.getX2(), r.getY1(), pg);
                    ur.setX2(pts[0]);
                    ur.setY2(pts[1]);
                    if (isModified) {
                        updateInfoList.add(new AnnotUpdateInfo(tm, mAnnotPageNum, ur, false, true));
                    }
                    if (isAdded) {
                        updateInfoList.add(new AnnotUpdateInfo(tm, pg, ur, true, false));
                    }
                }

                buildAnnotBBox();

                if (multiPageSel) {
                    mAnnotPageNum = sel_up_page;
                    buildAnnotBBox();
                    setCtrlPts(true);
                    showMenu(getQMAnchorRect());
                }

                // make sure to raise add event after mPdfViewCtrl.update
                Map<Annot, Integer> annotsModifiedList = new HashMap<>();
                Map<Annot, Integer> annotsAddedList = new HashMap<>();
                for (AnnotUpdateInfo updateInfo : updateInfoList) {
                    Rect rect = updateInfo.mRect;
                    mPdfViewCtrl.update(rect);
                    TextMarkup textMarkup = updateInfo.mTextMarkup;
                    int pageNum = updateInfo.mPageNum;

                    if (textMarkup != null) {
                        if (updateInfo.mIsModified) {
                            annotsModifiedList.put(textMarkup, pageNum);
                        }
                        if (updateInfo.mIsAdded) {
                            annotsAddedList.put(textMarkup, pageNum);
                        }
                    }
                }
                if (annotsAddedList.size() > 0) {
                    raiseAnnotationAddedEvent(annotsAddedList);
                } else if (annotsModifiedList.size() > 0) {
                    // if an annotation is added then its modification is not important for undo/redo
                    raiseAnnotationModifiedEvent(annotsModifiedList);
                }

            } catch (Exception e) {
                ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(e.getMessage());
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }
        }
    }

    private void setNextToolMode(ToolManager.ToolModeBase mode) {
        unsetAnnot();
        mNextToolMode = mode;
        // Draw away the edit widget
        mPdfViewCtrl.clearSelection();
        mEffSelWidgetId = -1;
        mSelWidgetEnabled = false;
        mPdfViewCtrl.invalidate(mInvalidateBBox);
        if (!mSelPath.isEmpty()) {
            // Clear the path data for highlighting
            mSelPath.reset();
        }
    }

    /**
     * Returns the highlighted text.
     *
     * @param pageNum The page number where the highlighted text is on
     * @return The highlighted text
     */
    @SuppressWarnings("WeakerAccess")
    public String getHighlightedText(int pageNum) {
        String text = "";
        if (mPdfViewCtrl.hasSelection()) {
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                PDFViewCtrl.Selection sel = mPdfViewCtrl.getSelection(pageNum);
                text = sel.getAsUnicode();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
        }
        return text;
    }

    /**
     * The overload implementation of {@link TextSelect#getModeAHLabel()}.
     */
    @Override
    protected int getModeAHLabel() {
        return AnalyticsHandlerAdapter.LABEL_QM_TEXTANNOTSELECT;
    }

    private RectF getQMAnchorRect() {
        RectF annotRect = getAnnotRect();
        if (annotRect == null) {
            return null;
        }
        return new RectF(annotRect.left, annotRect.top, annotRect.right, annotRect.bottom + Utils.convDp2Pix(mPdfViewCtrl.getContext(), 24));
    }

    /**
     * @return {@link AnalyticsHandlerAdapter#QUICK_MENU_TYPE_TEXT_ANNOTATION_SELECT}
     */
    @Override
    protected int getQuickMenuAnalyticType() {
        return AnalyticsHandlerAdapter.QUICK_MENU_TYPE_TEXT_ANNOTATION_SELECT;
    }
}
