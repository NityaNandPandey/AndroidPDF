//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PointF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.annots.Popup;
import com.pdftron.pdf.annots.Text;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.tools.DialogAnnotNote.DialogAnnotNoteListener;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;


/**
 * This class is for creating a sticky note annotation.
 */
@SuppressWarnings("WeakerAccess")
@Keep
public class StickyNoteCreate extends SimpleShapeCreate
    implements DialogAnnotNoteListener,
    AnnotStyle.OnAnnotStyleChangeListener {

    private String mIconType;
    private int mIconColor;
    private float mIconOpacity;
    private int mAnnotButtonPressed;
    private boolean mClosed;

    private DialogStickyNote mDialogStickyNote;

    /**
     * Class constructor
     */
    public StickyNoteCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mNextToolMode = ToolMode.TEXT_ANNOT_CREATE;

        // Set icon color and type based on previous style
        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());

        mIconColor = settings.getInt(getColorKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultColor(mPdfViewCtrl.getContext(), getCreateAnnotType()));
        mIconType = settings.getString(getIconKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultIcon(mPdfViewCtrl.getContext(), getCreateAnnotType()));
        mIconOpacity = settings.getFloat(getOpacityKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultOpacity(mPdfViewCtrl.getContext(), getCreateAnnotType()));
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.TEXT_ANNOT_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Text;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#setupAnnotProperty(int, float, float, int, String, String)}.
     */
    @Override
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName) {
        mIconColor = color;
        mIconType = icon;
        mIconOpacity = opacity;

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(getIconKey(getCreateAnnotType()), icon);
        editor.putInt(getColorKey(getCreateAnnotType()), color);
        editor.putFloat(getOpacityKey(getCreateAnnotType()), opacity);

        editor.apply();
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        if (mAllowTwoFingerScroll) {
            return false;
        }

        mPt1.x = e2.getX() + mPdfViewCtrl.getScrollX();
        mPt1.y = e2.getY() + mPdfViewCtrl.getScrollY();

        return true;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        if (mAllowTwoFingerScroll) {
            mAllowTwoFingerScroll = false;
        }
        return false;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#createMarkup(PDFDoc, Rect)}.
     */
    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        return null;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        // We are scrolling
        if (mAllowTwoFingerScroll || mPt1.x < 0 || mPt1.y < 0) {
            mAllowTwoFingerScroll = false;

            return false;
        }

        if (priorEventMode == PDFViewCtrl.PriorEventMode.PAGE_SLIDING) {
            return false;
        }

        // If we are just up from fling or pinch, do not add new note
        if (priorEventMode == PDFViewCtrl.PriorEventMode.FLING ||
            priorEventMode == PDFViewCtrl.PriorEventMode.PINCH) {
            // don't let scroll the page
            return true;
        }
        // If annotation was already pushed back, avoid re-entry due to fling motion
        // but allow when creating multiple strokes.
        if (mAnnotPushedBack && mForceSameNextToolMode) {
            return true;
        }

        // If all points are outside of the page, we don't push back the annotation
        if (mIsAllPointsOutsidePage) {
            return true;
        }

        boolean shouldCreate = true;
        int x = (int) e.getX();
        int y = (int) e.getY();
        ArrayList<Annot> annots = mPdfViewCtrl.getAnnotationListAt(x, y, x, y);
        int pageNum = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
        try {
            for (Annot annot : annots) {
                if (annot.getType() == Annot.e_Text) {
                    shouldCreate = false;
                    setAnnot(annot, pageNum);
                    buildAnnotBBox();
                    mNextToolMode = ToolMode.ANNOT_EDIT;
                    setCurrentDefaultToolModeHelper(getToolMode());
                    break;
                }
            }
        } catch (PDFNetException ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }

        if (shouldCreate) {
            setTargetPoint(new PointF(x, y));
        }

        return skipOnUpPriorEvent(priorEventMode);
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {

        // if quick menu just dismissed, then super.onDown will return true,
        // setting mAnnotPushedBack will stop stick note calling onUp
        mAnnotPushedBack = super.onDown(e);

        return false;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onClose()}.
     */
    @Override
    public void onClose() {
        if (!mClosed) {
            mClosed = true;
            unsetAnnot();
        }

        if (mDialogStickyNote != null && mDialogStickyNote.isShowing()) {
            // force to save the content
            mAnnotButtonPressed = DialogInterface.BUTTON_POSITIVE;
            prepareDialogStickyNoteDismiss();
            mDialogStickyNote.dismiss();
        }
    }

    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        super.onQuickMenuClicked(menuItem);
        mNextToolMode = ToolMode.ANNOT_EDIT;
        return false;
    }

    /**
     * The overload implementation of {@link SimpleShapeCreate#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setNextToolMode();
    }

    /**
     * The overload implementation of {@link DialogAnnotNoteListener#onAnnotButtonPressed(int)}.
     */
    @Override
    public void onAnnotButtonPressed(int button) {
        mAnnotButtonPressed = button;
    }

    /**
     * Sets the target point.
     *
     * @param point The target point
     */
    public void setTargetPoint(PointF point) {
        mPt1.x = point.x + mPdfViewCtrl.getScrollX();
        mPt1.y = point.y + mPdfViewCtrl.getScrollY();
        mDownPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(point.x, point.y);

        createStickyNote();

        showPopup();
    }

    private void createStickyNote() {
        boolean shouldUnlock = false;
        try {
            // add UI to drawing list
            addOldTools();

            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            double[] pts;
            pts = mPdfViewCtrl.convScreenPtToPagePt(mPt1.x - mPdfViewCtrl.getScrollX(), mPt1.y - mPdfViewCtrl.getScrollY(), mDownPageNum);
            pts[1] -= 20;
            Point p = new Point(pts[0] - 0, pts[1] - 0);
            com.pdftron.pdf.annots.Text text = com.pdftron.pdf.annots.Text.create(mPdfViewCtrl.getDoc(), p);
            text.setIcon(mIconType);
            ColorPt color = getColorPoint(mIconColor);
            if (color != null) {
                text.setColor(color, 3);
            } else {
                text.setColor(new ColorPt(1, 1, 0), 3);
            }
            text.setOpacity(mIconOpacity);

            com.pdftron.pdf.Rect rect = new com.pdftron.pdf.Rect();
            rect.set(pts[0] + 20, pts[1] + 20, pts[0] + 90, pts[1] + 90);
            com.pdftron.pdf.annots.Popup pop = com.pdftron.pdf.annots.Popup.create(mPdfViewCtrl.getDoc(), rect);
            pop.setParent(text);
            text.setPopup(pop);

            Page page = mPdfViewCtrl.getDoc().getPage(mDownPageNum);
            page.annotPushBack(text);
            page.annotPushBack(pop);
            setAnnot(text, mDownPageNum);
            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), mAnnot);

            mAnnotPushedBack = true;
            buildAnnotBBox();
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
            raiseAnnotationAddedEvent(mAnnot, mAnnotPageNum);
        } catch (Exception ex) {
            mNextToolMode = ToolMode.PAN;
            ((ToolManager) mPdfViewCtrl.getToolManager()).annotationCouldNotBeAdded(ex.getMessage());
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private ColorPt getColorPoint(int color) {
        double r = (double) Color.red(color) / 255;
        double g = (double) Color.green(color) / 255;
        double b = (double) Color.blue(color) / 255;
        ColorPt c = null;
        try {
            c = new ColorPt(r, g, b);
        } catch (Exception ignored) {

        }
        return c;
    }

    private void showPopup() {
        mNextToolMode = ToolMode.TEXT_ANNOT_CREATE;

        if (mAnnot == null) {
            return;
        }

        try {
            String iconType = "";
            float opacity = 1;
            try {
                Text t = new Text(mAnnot);
                iconType = t.getIconName();
                opacity = (float) t.getOpacity();
            } catch (Exception ignored) {

            }

            ColorPt colorPt = mAnnot.getColorAsRGB();
            int color = Utils.colorPt2color(colorPt);
            mDialogStickyNote = new DialogStickyNote(mPdfViewCtrl, "", false, iconType, color, opacity);
            mDialogStickyNote.setAnnotAppearanceChangeListener(this);
            mDialogStickyNote.setAnnotNoteListener(this);
            mDialogStickyNote.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    prepareDialogStickyNoteDismiss();
                }
            });
            mDialogStickyNote.show();

        } catch (Exception e1) {
            AnalyticsHandlerAdapter.getInstance().sendException(e1);
        }
    }

    private void prepareDialogStickyNoteDismiss() {
        if (mPdfViewCtrl == null || mAnnot == null || mDialogStickyNote == null) {
            return;
        }
        boolean createdAnnot = false;
        if (mAnnotButtonPressed == DialogInterface.BUTTON_POSITIVE) {
            boolean shouldUnlock = false;
            try {
                final Markup markup = new Markup(mAnnot);
                // Locks the document first as accessing annotation/doc information
                // isn't thread safe.
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                Utils.handleEmptyPopup(mPdfViewCtrl, markup);
                Popup popup = markup.getPopup();
                popup.setContents(mDialogStickyNote.getNote());
                setAuthor(markup);
                raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);
                createdAnnot = true;
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    mPdfViewCtrl.docUnlock();
                }
            }

        } else {
            deleteStickyAnnot();
        }
        mAnnotButtonPressed = 0;
        mDialogStickyNote.prepareDismiss();
        setNextToolMode();
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (createdAnnot && (toolManager.isAutoSelectAnnotation() || !mForceSameNextToolMode)) {
            toolManager.selectAnnot(mAnnot, mAnnotPageNum);
        }
    }

    private void deleteStickyAnnot() {
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread
            // safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreRemoveEvent(mAnnot, mAnnotPageNum);
            Page page = mPdfViewCtrl.getDoc().getPage(mAnnotPageNum);
            page.annotRemove(mAnnot);
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            // make sure to raise remove event after mPdfViewCtrl.update and before unsetAnnot
            raiseAnnotationRemovedEvent(mAnnot, mAnnotPageNum);

            unsetAnnot();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void editColor(int color) {

        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information
            // isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
            ColorPt colorPt = Utils.color2ColorPt(color);
            mAnnot.setColor(colorPt, 3);

            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), mAnnot);
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(getColorKey(AnnotUtils.getAnnotType(mAnnot)), color);
            editor.apply();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void editOpacity(float opacity) {

        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information
            // isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
            ((Markup) mAnnot).setOpacity(opacity);
            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), mAnnot);
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putFloat(getOpacityKey(AnnotUtils.getAnnotType(mAnnot)), opacity);
            editor.apply();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }


    private void editIcon(String icon) {
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information
            // isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
            Text text = new Text(mAnnot);
            text.setIcon(icon);
            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), mAnnot);
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(getIconKey(AnnotUtils.getAnnotType(mAnnot)), icon);
            editor.apply();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void setNextToolMode() {
        if (mAnnot != null && (((ToolManager) mPdfViewCtrl.getToolManager()).isAutoSelectAnnotation() || !mForceSameNextToolMode)) {
            mNextToolMode = ToolMode.ANNOT_EDIT;
            setCurrentDefaultToolModeHelper(getToolMode());
        } else if (mForceSameNextToolMode) {
            mNextToolMode = ToolMode.TEXT_ANNOT_CREATE;
        } else {
            mNextToolMode = ToolMode.PAN;
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, null);
            toolManager.setTool(tool);
        }
    }

    @Override
    public void onChangeAnnotThickness(float thickness, boolean done) {

    }

    @Override
    public void onChangeAnnotTextSize(float textSize, boolean done) {

    }

    @Override
    public void onChangeAnnotTextColor(int textColor) {

    }

    @Override
    public void onChangeAnnotOpacity(float opacity, boolean done) {
        if (done) {
            editOpacity(opacity);
        }
    }

    @Override
    public void onChangeAnnotStrokeColor(int color) {
        editColor(color);
    }

    @Override
    public void onChangeAnnotFillColor(int color) {

    }

    @Override
    public void onChangeAnnotIcon(String icon) {
        editIcon(icon);
    }

    @Override
    public void onChangeAnnotFont(FontResource font) {

    }

    @Override
    public void onChangeRulerProperty(RulerItem rulerItem) {

    }
}
