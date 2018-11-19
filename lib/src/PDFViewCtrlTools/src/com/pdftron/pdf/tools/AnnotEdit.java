//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.AudioFormat;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Keep;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.pdftron.common.Matrix2D;
import com.pdftron.common.PDFNetException;
import com.pdftron.filters.Filter;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.Redactor;
import com.pdftron.pdf.annots.FileAttachment;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.annots.Line;
import com.pdftron.pdf.annots.Link;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.annots.Popup;
import com.pdftron.pdf.annots.Sound;
import com.pdftron.pdf.annots.Text;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.dialog.SoundDialogFragment;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.FreeTextCacheStruct;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.tools.DialogAnnotNote.DialogAnnotNoteListener;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.AnnotationClipboardHelper;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.InlineEditText;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.AutoScrollEditText;
import com.pdftron.sdf.DictIterator;
import com.pdftron.sdf.Obj;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

/**
 * This class is responsible for editing a selected annotation, e.g., moving and resizing.
 */
// Note that mAnnot his can be null for example in AnnotEditRectGroup
@SuppressWarnings("WeakerAccess")
@Keep
public class AnnotEdit extends Tool implements DialogAnnotNoteListener,
    InlineEditText.InlineEditTextListener, TextWatcher,
    AnnotStyle.OnAnnotStyleChangeListener {

    public static final int RECTANGULAR_CTRL_PTS_CNT = 8;

    private static final String TAG = AnnotEdit.class.getName();
    private static boolean sDebug;

    /**
     * unknown control point
     */
    public static final int e_unknown = -1;
    /**
     * moving control
     */
    public static final int e_moving = -2;
    /**
     * lower left control point
     */
    public static final int e_ll = 0;
    /**
     * lower right control point
     */
    public static final int e_lr = 1;
    /**
     * upper right control point
     */
    public static final int e_ur = 2;
    /**
     * upper left control point
     */
    public static final int e_ul = 3;
    /**
     * middle right control point
     */
    public static final int e_mr = 4;
    /**
     * upper middle control point
     */
    public static final int e_um = 5;
    /**
     * lower middle control point
     */
    public static final int e_lm = 6;
    /**
     * middle left control point
     */
    public static final int e_ml = 7;

    protected final DashPathEffect mDashPathEffect = new DashPathEffect(new float[]{5, 3}, 0);

    protected RectF mBBox = new RectF();
    protected RectF mBBoxOnDown = new RectF();
    protected RectF mContentBox;
    protected RectF mContentBoxOnDown;
    protected RectF mPageCropOnClientF;
    protected int mEffCtrlPtId = e_unknown;
    protected boolean mModifiedAnnot;
    protected boolean mCtrlPtsSet;
    private boolean mAnnotIsSticky;
    private boolean mAnnotIsSound;
    private boolean mAnnotIsFileAttachment;
    protected boolean mAnnotIsTextMarkup;
    private boolean mAnnotIsFreeText;
    private boolean mAnnotIsStamper;
    private boolean mAnnotIsSignature;
    private boolean mAnnotIsLine;
    protected boolean mScaled;
    protected Paint mPaint = new Paint();
    private boolean mUpFromStickyCreate;
    private boolean mUpFromFreeTextCreate;
    private boolean mUpFromStickyCreateDlgShown;
    private boolean mMaintainAspectRatio; // if set to true, maintain aspect ratio of annotation's bounding box

    private InlineEditText mInlineEditText;
    private boolean mTapToSaveFreeTextAnnot;
    private boolean mSaveFreeTextAnnotInOnUp;
    private boolean mHasOnCloseCalled;
    private boolean mIsScaleBegun;
    private boolean mStamperToolSelected;

    protected int CTRL_PTS_CNT = RECTANGULAR_CTRL_PTS_CNT;
    protected PointF[] mCtrlPts = new PointF[CTRL_PTS_CNT];
    protected PointF[] mCtrlPtsOnDown = new PointF[CTRL_PTS_CNT];

    protected final float mCtrlRadius; // radius of the control point
    protected boolean mHideCtrlPts;

    private int mAnnotButtonPressed;

    private int mCurrentFreeTextEditMode;
    private boolean mUpdateFreeTextEditMode;
    private boolean mInEditMode;

    private float mAspectRatio; // aspect ratio of the bounding box surrounding the annotation (height / width)

    private String mCacheFileName;
    // freetext caching
    private long mStoredTimeStamp;
    private DialogStickyNote mDialogStickyNote;
    private DialogAnnotNote mDialogAnnotNote;
    private AnnotStyleDialogFragment mAnnotStyleDialog;
    private boolean mHandleEffCtrlPtsDisabled;

    /**
     * Class constructor
     */
    public AnnotEdit(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        // The radius size can be stored in the res/values folder.
        // This way we can pick up different sizes depending on the
        // device's size/resolution.
        mCtrlRadius = this.convDp2Pix(7.5f);

        for (int i = 0; i < CTRL_PTS_CNT; ++i) {
            mCtrlPts[i] = new PointF();
            mCtrlPtsOnDown[i] = new PointF();
        }

        mPaint.setAntiAlias(true);
    }

    /**
     * The overload implementation of {@link Tool#onCreate()}.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        if (mAnnot == null) {
            return;
        }

        boolean shouldUnlockRead = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread
            // safe. Since we are not going to modify the doc here, we can use the read lock.
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            mHasSelectionPermission = hasPermission(mAnnot, ANNOT_PERMISSION_SELECTION);
            mHasMenuPermission = hasPermission(mAnnot, ANNOT_PERMISSION_MENU);

            int type = mAnnot.getType();

            mAnnotIsLine = (type == Annot.e_Line);
            mAnnotIsSticky = (type == Annot.e_Text);
            mAnnotIsSound = (type == Annot.e_Sound);
            mAnnotIsFileAttachment = (type == Annot.e_FileAttachment);
            mAnnotIsFreeText = (type == Annot.e_FreeText);
            mAnnotIsTextMarkup = (type == Annot.e_Highlight ||
                type == Annot.e_Underline ||
                type == Annot.e_StrikeOut ||
                type == Annot.e_Squiggly);

            // Create menu items based on the type of the selected annotation
            if (mAnnot.isMarkup() && type == Annot.e_Stamp) {
                Obj sigObj = mAnnot.getSDFObj();
                sigObj = sigObj.findObj(Signature.SIGNATURE_ANNOTATION_ID);
                mMaintainAspectRatio = true;
                if (sigObj != null) {
                    mAnnotIsSignature = true;
                } else {
                    mAnnotIsStamper = true;
                }
                mStamperToolSelected = true;
            }

            // Remember the page bounding box in client space; this is used to ensure while
            // moving/resizing, the widget doesn't go beyond the page boundary.
            mPageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mAnnotPageNum);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.ANNOT_EDIT;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Unknown;
    }

    @SuppressWarnings("SameParameterValue")
    public void setUpFromStickyCreate(boolean flag) {
        mUpFromStickyCreate = flag;
    }

    @SuppressWarnings("SameParameterValue")
    public void setUpFromFreeTextCreate(boolean flag) {
        mUpFromFreeTextCreate = flag;
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
     * get quick menu resource by clicked annotation
     *
     * @param annot annotation
     * @return menu resource
     */
    protected @MenuRes
    int getMenuResByAnnot(@Nullable Annot annot) throws PDFNetException {
        if (annot == null) {
            return R.menu.annot_general;
        }

        int type = annot.getType();
        switch (type) {
            case Annot.e_Sound:
                return R.menu.annot_edit_sound;
            case Annot.e_Square:
            case Annot.e_Circle:
            case Annot.e_Polygon:
            case Annot.e_Polyline:
            case Annot.e_Line:
            case Annot.e_Text:
                return R.menu.annot_simple_shape;
            case Annot.e_Highlight:
            case Annot.e_Underline:
            case Annot.e_StrikeOut:
            case Annot.e_Squiggly:
                return R.menu.annot_edit_text_markup;
            case Annot.e_Redact:
                return R.menu.annot_edit_text_redaction;
            case Annot.e_FreeText:
                return R.menu.annot_free_text;
            case Annot.e_Link:
                return R.menu.annot_link;
            case Annot.e_Stamp:
                if (mAnnotIsSignature) {
                    return R.menu.annot_signature;
                }
                if (mAnnotIsStamper) {
                    return R.menu.annot_stamper;
                }
            case Annot.e_FileAttachment:
                return R.menu.annot_file_attachment;
            case Annot.e_Ink:
                if (AnnotUtils.isFreeHighlighter(annot)) {
                    return R.menu.annot_simple_shape;
                }
                return R.menu.annot_free_hand;
            case Annot.e_Widget:
                Widget annotWidget = new Widget(annot);
                Field annotWidgetField = annotWidget.getField();
                if (annotWidgetField.getType() == Field.e_radio) {
                    return R.menu.annot_radio_field;
                }
            default:
                return R.menu.annot_general;
        }
    }

    /**
     * The overload implementation of {@link Tool#createQuickMenu()}.
     */
    @Override
    protected QuickMenu createQuickMenu() {
        QuickMenu quickMenu = super.createQuickMenu();
        try {
            quickMenu.inflate(getMenuResByAnnot(mAnnot));
            customizeQuickMenuItems(quickMenu);
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        quickMenu.initMenuEntries();
        return quickMenu;
    }

    /**
     * The overload implementation of {@link Tool#selectAnnot(Annot, int)}.
     */
    @Override
    public void selectAnnot(Annot annot, int pageNum) {
        //need to execute selectAnnot from Tool.java first -- do this through an instance of Pan tool

        mNextToolMode = getToolMode();
        setCtrlPts();
        mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));
        showMenu(getAnnotRect());
    }

    protected Rect getAnnotScreenBBox() throws PDFNetException {
        if (mAnnot == null) {
            return null;
        }
        return mPdfViewCtrl.getScreenRectForAnnot(mAnnot, mAnnotPageNum);
    }

    protected Rect getAnnotScreenContentBox() throws PDFNetException {
        if (mAnnot == null) {
            return null;
        }
        if (AnnotUtils.isCallout(mAnnot)) {
            FreeText freeText = new FreeText(mAnnot);
            Rect contentRect = freeText.getContentRect();
            double[] pts1 = mPdfViewCtrl.convPagePtToScreenPt(contentRect.getX1(), contentRect.getY1(), mAnnotPageNum);
            double[] pts2 = mPdfViewCtrl.convPagePtToScreenPt(contentRect.getX2(), contentRect.getY2(), mAnnotPageNum);
            double x1 = pts1[0];
            double y1 = pts1[1];
            double x2 = pts2[0];
            double y2 = pts2[1];
            return new Rect(x1, y1, x2, y2);
        }
        return null;
    }

    /**
     * @return The screen rect of the annotation
     */
    protected RectF getScreenRect(Rect screen_rect) {
        if (screen_rect == null) {
            return null;
        }

        float x1 = 0, x2 = 0, y1 = 0, y2 = 0; // in screen pts
        try {
            x1 = (float) screen_rect.getX1();
            y1 = (float) screen_rect.getY1();
            x2 = (float) screen_rect.getX2();
            y2 = (float) screen_rect.getY2();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        float sx = mPdfViewCtrl.getScrollX();
        float sy = mPdfViewCtrl.getScrollY();

        // Compute the control points. In case that the page is rotated, have to
        // ensure the control points are properly positioned.
        float min_x, max_x;
        float min_y, max_y;
        float x, y;

        //double[] pts;
        //pts = mPdfViewCtrl.convPagePtToScreenPt(x1, y2, mAnnotPageNum);
        min_x = max_x = x1 + sx;
        min_y = max_y = y2 + sy;

        //pts = mPdfViewCtrl.convPagePtToScreenPt((x1 + x2) / 2, y2, mAnnotPageNum);
        x = (x1 + x2) / 2 + sx;
        y = y2 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);

        //pts = mPdfViewCtrl.convPagePtToScreenPt(x2, y2, mAnnotPageNum);
        x = x2 + sx;
        y = y2 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);

        //pts = mPdfViewCtrl.convPagePtToScreenPt(x2, (y1 + y2) / 2, mAnnotPageNum);
        x = x2 + sx;
        y = (y1 + y2) / 2 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);

        //pts = mPdfViewCtrl.convPagePtToScreenPt(x2, y1, mAnnotPageNum);
        x = x2 + sx;
        y = y1 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);

        //pts = mPdfViewCtrl.convPagePtToScreenPt((x1 + x2) / 2, y1, mAnnotPageNum);
        x = (x1 + x2) / 2 + sx;
        y = y1 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);

        //pts = mPdfViewCtrl.convPagePtToScreenPt(x1, y1, mAnnotPageNum);
        x = x1 + sx;
        y = y1 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);

        //pts = mPdfViewCtrl.convPagePtToScreenPt(x1, (y1 + y2) / 2, mAnnotPageNum);
        x = x1 + sx;
        y = (y1 + y2) / 2 + sy;
        min_x = Math.min(x, min_x);
        max_x = Math.max(x, max_x);
        min_y = Math.min(y, min_y);
        max_y = Math.max(y, max_y);
        return new RectF(min_x, min_y, max_x, max_y);
    }

    @Override
    protected boolean canAddAnnotView(Annot annot, AnnotStyle annotStyle) {
        if (!((ToolManager) mPdfViewCtrl.getToolManager()).isRealTimeAnnotEdit()) {
            return false;
        }
        //noinspection SimplifiableIfStatement
        if (annotStyle.hasAppearance()) {
            return false;
        }
        return annotStyle.getAnnotType() == Annot.e_Circle ||
            annotStyle.getAnnotType() == Annot.e_Square ||
            annotStyle.getAnnotType() == Annot.e_Text ||
            annotStyle.getAnnotType() == Annot.e_FreeText ||
            annotStyle.getAnnotType() == Annot.e_Ink;
    }

    /**
     * Initializes the positions of the eight control points based on
     * the bounding box of the annotation.
     */
    protected void setCtrlPts() {
        if (onInterceptAnnotationHandling(mAnnot)) {
            return;
        }

        RectF screenRect = null;
        RectF contentRect = null;
        try {
            screenRect = getScreenRect(getAnnotScreenBBox());
            contentRect = getScreenRect(getAnnotScreenContentBox());
        } catch (PDFNetException ignored) {
        }
        if (screenRect == null) {
            return;
        }

        mCtrlPtsSet = true;
        float min_x = screenRect.left;
        float min_y = screenRect.top;
        float max_x = screenRect.right;
        float max_y = screenRect.bottom;

        mBBox.left = min_x - mCtrlRadius;
        mBBox.top = min_y - mCtrlRadius;
        mBBox.right = max_x + mCtrlRadius;
        mBBox.bottom = max_y + mCtrlRadius;

        if (contentRect != null) {
            min_x = contentRect.left;
            min_y = contentRect.top;
            max_x = contentRect.right;
            max_y = contentRect.bottom;

            if (mContentBox == null) {
                mContentBox = new RectF();
            }
            mContentBox.left = min_x - mCtrlRadius;
            mContentBox.top = min_y - mCtrlRadius;
            mContentBox.right = max_x + mCtrlRadius;
            mContentBox.bottom = max_y + mCtrlRadius;
        }

        if (addAnnotView()) {
            mAnnotView.getDrawingView().initInkItem(mAnnot, new PointF(min_x, min_y));
            mAnnotView.layout((int) (min_x + 0.5),
                (int) (min_y + 0.5),
                (int) (max_x + 0.5),
                (int) (max_y + 0.5));
        }

        // if maintaining aspect ratio, calculate aspect ratio
        if (mMaintainAspectRatio) {
            float height = max_y - min_y;
            float width = max_x - min_x;
            mAspectRatio = height / width;
        }

        if (!mHandleEffCtrlPtsDisabled) {
            mCtrlPts[e_ll].x = min_x;
            mCtrlPts[e_ll].y = max_y;

            mCtrlPts[e_lm].x = (min_x + max_x) / 2.0f;
            mCtrlPts[e_lm].y = max_y;

            mCtrlPts[e_lr].x = max_x;
            mCtrlPts[e_lr].y = max_y;

            mCtrlPts[e_mr].x = max_x;
            mCtrlPts[e_mr].y = (min_y + max_y) / 2.0f;

            mCtrlPts[e_ur].x = max_x;
            mCtrlPts[e_ur].y = min_y;

            mCtrlPts[e_um].x = (min_x + max_x) / 2.0f;
            mCtrlPts[e_um].y = min_y;

            mCtrlPts[e_ul].x = min_x;
            mCtrlPts[e_ul].y = min_y;

            mCtrlPts[e_ml].x = min_x;
            mCtrlPts[e_ml].y = (min_y + max_y) / 2.0f;

            if (mAnnotView != null) {
                PointF[] ctrlPts = new PointF[CTRL_PTS_CNT];
                ctrlPts[e_lm] = new PointF(mCtrlPts[e_lm].x - min_x, mCtrlPts[e_lm].y - min_y);
                ctrlPts[e_ml] = new PointF(mCtrlPts[e_ml].x - min_x, mCtrlPts[e_ml].y - min_y);
                mAnnotView.setCtrlPts(ctrlPts);
            }
        }
    }

    private boolean isAnnotResizable() {
        return !mAnnotIsSticky && !mAnnotIsTextMarkup && !mAnnotIsSound && !mAnnotIsFileAttachment;
    }

    /**
     * The overload implementation of {@link Tool#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        super.onDraw(canvas, tfm);

        if (!hasAnnotSelected()) {
            return;
        }

        if (mHideCtrlPts) {
            return;
        }

        if (mAnnotIsLine) {
            // let child handle it
            return;
        }

        float left = mBBox.left + mCtrlRadius;
        float right = mBBox.right - mCtrlRadius;
        float top = mBBox.top + mCtrlRadius;
        float bottom = mBBox.bottom - mCtrlRadius;
        if (mContentBox != null) {
            left = mContentBox.left + mCtrlRadius;
            right = mContentBox.right - mCtrlRadius;
            top = mContentBox.top + mCtrlRadius;
            bottom = mContentBox.bottom - mCtrlRadius;
        }
        if (right - left <= 0 && bottom - top <= 0) {
            return;
        }

        if (mHasSelectionPermission) {
            mPaint.setColor(mPdfViewCtrl.getResources().getColor(R.color.tools_annot_edit_line_shadow));
        } else {
            mPaint.setColor(mPdfViewCtrl.getResources().getColor(R.color.tools_annot_edit_line_shadow_no_permission));
        }
        PathEffect pathEffect = mPaint.getPathEffect();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2);
//        mPaint.setPathEffect(mDashPathEffect);
        canvas.drawRect(left, top, right, bottom, mPaint);

        if (mHandleEffCtrlPtsDisabled) {
            return;
        }
        // Don't draw control points for sticky notes since they cannot be re-sized
        // (movable only).
        // Adobe does not allow resize sound annotation
        if (!isAnnotResizable()) {
            return;
        }

        mPaint.setPathEffect(pathEffect);
        mPaint.setStrokeWidth(1);

        DrawingUtils.drawCtrlPts(mPdfViewCtrl.getResources(), canvas, mPaint,
            mCtrlPts[e_ul], mCtrlPts[e_lr], mCtrlPts[e_lm], mCtrlPts[e_ml],
            mCtrlRadius, mHasSelectionPermission, mMaintainAspectRatio);
    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        super.onSingleTapConfirmed(e);

        if (mAnnot == null) {
            mNextToolMode = mCurrentDefaultToolMode;
            return false;
        }

        int x = (int) (e.getX() + 0.5);
        int y = (int) (e.getY() + 0.5);
        Annot tempAnnot = mPdfViewCtrl.getAnnotationAt(x, y);
        if (mAnnot.equals(tempAnnot) || mUpFromStickyCreate || mUpFromFreeTextCreate) {
            // Single clicked within the annotation, set the control points, draw the widget and
            // show the menu.
            mNextToolMode = getToolMode();
            setCtrlPts();
            mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));

            if (mAnnotIsSticky) {
                handleStickyNote(mForceSameNextToolMode, mUpFromStickyCreate);
            } else if (!mUpFromStickyCreate && !mUpFromFreeTextCreate) {
                if (mInlineEditText == null || !mInlineEditText.isEditing()) {
                    // don't show menu if we are editing
                    if (mAnnotIsFreeText) {
                        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                        if (toolManager.isEditFreeTextOnTap()) {
                            enterText();
                        } else {
                            showMenu(getAnnotRect());
                        }
                    } else {
                        showMenu(getAnnotRect());
                    }
                }
            }

        } else if (mTapToSaveFreeTextAnnot) {
            mTapToSaveFreeTextAnnot = false;
            return true;
        } else {
            // Otherwise goes back to the pan mode.
            if (sDebug) Log.d(TAG, "going to unsetAnnot: onSingleTapConfirmed");
            unsetAnnot();
            mNextToolMode = mCurrentDefaultToolMode;//ToolMode.PAN;

            // enable selecting another annotation with the same kind for signature, image/rubber stamper
            if (mCurrentDefaultToolMode == ToolMode.SIGNATURE
                || mCurrentDefaultToolMode == ToolMode.STAMPER
                || mCurrentDefaultToolMode == ToolMode.RUBBER_STAMPER) {
                mNextToolMode = ToolMode.PAN;
            }

            setCtrlPts();
            // Draw away the edit widget
            mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));
        }

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onPageTurning(int, int)}.
     */
    @Override
    public void onPageTurning(int old_page, int cur_page) {
        super.onPageTurning(old_page, cur_page);
        mNextToolMode = mCurrentDefaultToolMode;
    }

    /**
     * The overload implementation of {@link Tool#onClose()}.
     */
    @Override
    public void onClose() {
        super.onClose();

        if (mHasOnCloseCalled) {
            return;
        }
        mHasOnCloseCalled = true;

        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            InputMethodManager imm = (InputMethodManager) mPdfViewCtrl.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mPdfViewCtrl.getRootView().getWindowToken(), 0);
            }

            saveAndQuitInlineEditText(false);
        }

        if (mDialogStickyNote != null && mDialogStickyNote.isShowing()) {
            // force to save the content
            mAnnotButtonPressed = DialogInterface.BUTTON_POSITIVE;
            prepareDialogStickyNoteDismiss(false);
            mDialogStickyNote.dismiss();
        }

        unsetAnnot();
    }

    /**
     * The overload implementation of {@link Tool#onQuickMenuClicked(QuickMenuItem)}.
     */
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        if (super.onQuickMenuClicked(menuItem)) {
            return true;
        }

        if (!hasAnnotSelected()) {
            mNextToolMode = ToolMode.PAN;
            return true;
        }

        int type = Annot.e_Unknown;
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            type = mAnnot.getType();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        if (menuItem.getItemId() == R.id.qm_delete) {
            deleteAnnot();
        }
        // Add note to the annotation
        else if (menuItem.getItemId() == R.id.qm_note) {
            if (mAnnotIsSticky) {
                handleStickyNote(false, false);
            } else {
                handleAnnotNote(false);
            }
        }
        // Show Appearance Popup window
        else if (menuItem.getItemId() == R.id.qm_appearance) {
            changeAnnotAppearance();
        }
        // flatten the signature annotation
        else if (menuItem.getItemId() == R.id.qm_flatten) {
            handleFlattenAnnot();
            mNextToolMode = mCurrentDefaultToolMode;
        }
        // if mAnnot is a text markup, let AnnotEditTextMarkup handles the rest quick menu item
        else if (type == Annot.e_Underline
            || type== Annot.e_Highlight
            || type == Annot.e_StrikeOut
            || type == Annot.e_Squiggly) {
            mNextToolMode = ToolMode.ANNOT_EDIT_TEXT_MARKUP;
            return false;
        } else if (menuItem.getItemId() == R.id.qm_text) {
            enterText();
        } else if (menuItem.getItemId() == R.id.qm_copy) {
            AnnotationClipboardHelper.copyAnnot(mPdfViewCtrl.getContext(), mAnnot, mPdfViewCtrl,
                new AnnotationClipboardHelper.OnClipboardTaskListener() {
                    @Override
                    public void onnClipboardTaskDone(String error) {
                        if (error == null && mPdfViewCtrl.getContext() != null) {
                            CommonToast.showText(mPdfViewCtrl.getContext(), R.string.tools_copy_annot_confirmation, Toast.LENGTH_SHORT);
                        }
                    }
                });
        } else if (menuItem.getItemId() == R.id.qm_rotate) {
            rotateStamperAnnot();
        } else if (menuItem.getItemId() == R.id.qm_open_attachment) {
            FileAttachment fileAttachment = null;
            shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;

                fileAttachment = new FileAttachment(mAnnot);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
            // unlock here in case user needs to call setDoc on the attachment file
            ((ToolManager) (mPdfViewCtrl.getToolManager())).onFileAttachmentSelected(fileAttachment);
            mNextToolMode = ToolMode.PAN;
        } else if (menuItem.getItemId() == R.id.qm_edit) {
            if (type == Annot.e_Ink) {
                editAnnot();
            }
        } else if (menuItem.getItemId() == R.id.qm_link) {
            if (type == Annot.e_Link) {
                shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;

                    Link link = new Link(mAnnot);
                    DialogLinkEditor linkEditorDialog = new DialogLinkEditor(mPdfViewCtrl, this, link);
                    linkEditorDialog.show();
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
            }
        } else if (menuItem.getItemId() == R.id.qm_form_radio_add_item) {
            shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;

                Widget annotWidget = new Widget(mAnnot);
                Field annotWidgetField = annotWidget.getField();
                int fieldType = annotWidgetField.getType();
                if (fieldType == com.pdftron.pdf.Field.e_radio) {
                    mNextToolMode = ToolMode.FORM_RADIO_GROUP_CREATE;
                    RadioGroupFieldCreate radioGroupTool = (RadioGroupFieldCreate) ((ToolManager) mPdfViewCtrl.getToolManager()).createTool(ToolMode.FORM_RADIO_GROUP_CREATE, this);
                    ((ToolManager) mPdfViewCtrl.getToolManager()).setTool(radioGroupTool);
                    radioGroupTool.setTargetField(annotWidgetField);
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
        } else if (menuItem.getItemId() == R.id.qm_redact) {
            redactAnnot();
        } else if (menuItem.getItemId() == R.id.qm_play_sound) {
            playSoundAnnot();
        }
        return true;
    }

    /**
     * Handles annotation appearance change request
     */
    protected void changeAnnotAppearance() {
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlock = true;
            if (mAnnot == null || !mAnnot.isValid()) {
                return;
            }

            AnnotStyleDialogFragment.Builder styleDialogBuilder = new AnnotStyleDialogFragment.Builder();

            if (mAnnotIsFreeText) {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                Set<String> whiteListFonts = toolManager.getFreeTextFonts();
                styleDialogBuilder.setWhiteListFont(whiteListFonts);
            }

            AnnotStyle annotStyle = AnnotUtils.getAnnotStyle(mAnnot);

            int[] pdfViewCtrlOrigin = new int[2];
            mPdfViewCtrl.getLocationInWindow(pdfViewCtrlOrigin);
            RectF annotRect = getAnnotRect();
            annotRect.offset(pdfViewCtrlOrigin[0], pdfViewCtrlOrigin[1]);
            mAnnotStyleDialog = styleDialogBuilder.setAnnotStyle(annotStyle).setAnchor(annotRect).build();

            mAnnotStyleDialog.setOnAnnotStyleChangeListener(this);
            mAnnotStyleDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    mAnnotStyleDialog = null;
                    ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                    toolManager.selectAnnot(mAnnot, mAnnotPageNum);

                }
            });
            FragmentActivity activity = ((ToolManager) mPdfViewCtrl.getToolManager()).getCurrentActivity();
            if (activity == null) {
                AnalyticsHandlerAdapter.getInstance().sendException(new Exception("ToolManager is not attached with an Activity"));
                return;
            }
            mAnnotStyleDialog.show(activity.getSupportFragmentManager(),
                AnalyticsHandlerAdapter.STYLE_PICKER_LOC_QM,
                AnalyticsHandlerAdapter.getInstance().getAnnotToolByAnnotType(annotStyle.getAnnotType()));
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    /**
     * Handles annotation rotation if annotation is a stamper
     */
    protected void rotateStamperAnnot() {
        if (!mAnnotIsStamper || mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "rotateStamperAnnot");
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            // get stamps current rotation
            Obj stampObj = mAnnot.getSDFObj();
            Obj rotationObj = stampObj.findObj(Stamper.STAMPER_ROTATION_ID);
            int rotation = 0;
            if (rotationObj != null) {
                rotation = (int) rotationObj.getNumber();
            }

            // set Matrix in the appearance stream which sets the rotation
            // of the image
            Obj appearanceStream = mAnnot.getAppearance();
            if (appearanceStream != null) {
                switch (rotation) {
                    case 0:
                        appearanceStream.putMatrix("Matrix", Matrix2D.rotationMatrix(Math.PI / 2));
                        break;
                    case 90:
                        appearanceStream.putMatrix("Matrix", Matrix2D.rotationMatrix(Math.PI));
                        break;
                    case 180:
                        appearanceStream.putMatrix("Matrix", Matrix2D.rotationMatrix(Math.PI * 1.5));
                        break;
                    default:
                        appearanceStream.putMatrix("Matrix", Matrix2D.rotationMatrix(0));
                        break;
                }
                // update annot
                if (mAnnot.getType() != Annot.e_Stamp) {
                    AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), mAnnot);
                }

                // update rotation key
                rotation = (rotation + 90) % 360;
                stampObj.putNumber(Stamper.STAMPER_ROTATION_ID, rotation);

                // change annot size
                // invert Aspect Ratio
                mAspectRatio = 1 / mAspectRatio;

                // update all mCtrlPts to the new image orientation
                float width = mCtrlPts[e_ur].x - mCtrlPts[e_ul].x;
                float height = mCtrlPts[e_lr].y - mCtrlPts[e_ur].y;
                float delta = (height - width) / 2;

                if (mHandleEffCtrlPtsDisabled) {
                    return;
                }

                // corner mCtrlPts
                mCtrlPts[e_ul].x -= delta;
                mCtrlPts[e_ul].y += delta;
                mCtrlPts[e_ll].x -= delta;
                mCtrlPts[e_ll].y -= delta;
                mCtrlPts[e_lr].x += delta;
                mCtrlPts[e_lr].y -= delta;
                mCtrlPts[e_ur].x += delta;
                mCtrlPts[e_ur].y += delta;

                // decrease size of image to maintains its aspect
                // ratio if its width or height exceeds the page
                boundStamperCtrlPts();

                raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

                // update annotation
                updateAnnot();

                raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);

                // show quick menu
                showMenu(getAnnotRect());
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void boundStamperCtrlPts() {
        if (mPageCropOnClientF == null || mHandleEffCtrlPtsDisabled) {
            return;
        }

        if (mCtrlPts[e_lr].x > mPageCropOnClientF.right) {
            mCtrlPts[e_lr].x = mPageCropOnClientF.right;
            mCtrlPts[e_ur].x = mCtrlPts[e_lr].x;
            float width = mCtrlPts[e_lr].x - mCtrlPts[e_ll].x;
            float height = width * mAspectRatio;
            mCtrlPts[e_lr].y = mCtrlPts[e_ur].y + height;
            mCtrlPts[e_ll].y = mCtrlPts[e_ul].y + height;
        }
        if (mCtrlPts[e_ll].x < mPageCropOnClientF.left) {
            mCtrlPts[e_ll].x = mPageCropOnClientF.left;
            mCtrlPts[e_ul].x = mCtrlPts[e_ll].x;
            float width = mCtrlPts[e_lr].x - mCtrlPts[e_ll].x;
            float height = width * mAspectRatio;
            mCtrlPts[e_lr].y = mCtrlPts[e_ur].y + height;
            mCtrlPts[e_ll].y = mCtrlPts[e_ul].y + height;
        }
        if (mCtrlPts[e_ur].y < mPageCropOnClientF.top) {
            mCtrlPts[e_ur].y = mPageCropOnClientF.top;
            mCtrlPts[e_ul].y = mCtrlPts[e_ur].y;
            float height = mCtrlPts[e_lr].y - mCtrlPts[e_ur].y;
            float width = height * (1 / mAspectRatio);
            mCtrlPts[e_ul].x = mCtrlPts[e_ur].x - width;
            mCtrlPts[e_ll].x = mCtrlPts[e_lr].x - width;
        }
        if (mCtrlPts[e_ll].y > mPageCropOnClientF.bottom) {
            mCtrlPts[e_ll].y = mPageCropOnClientF.bottom;
            mCtrlPts[e_lr].y = mCtrlPts[e_ll].y;
            float height = mCtrlPts[e_lr].y - mCtrlPts[e_ur].y;
            float width = height * (1 / mAspectRatio);
            mCtrlPts[e_ul].x = mCtrlPts[e_ur].x - width;
            mCtrlPts[e_ll].x = mCtrlPts[e_lr].x - width;
        }

        // update BBox
        mBBox.left = mCtrlPts[e_ul].x - mCtrlRadius;
        mBBox.top = mCtrlPts[e_ul].y - mCtrlRadius;
        mBBox.right = mCtrlPts[e_lr].x + mCtrlRadius;
        mBBox.bottom = mCtrlPts[e_lr].y + mCtrlRadius;
    }

    public void enterText() {
        mInEditMode = true;
        mSaveFreeTextAnnotInOnUp = true;
        if (!mCtrlPtsSet) {
            setCtrlPts();
        }
        // get last used free text edit mode, either inline or dialog, and open
        // the free text annot in that mode
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            mCurrentFreeTextEditMode = settings.getInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, ANNOTATION_FREE_TEXT_PREFERENCE_EDITING_DEFAULT);
            mCacheFileName = ((ToolManager) mPdfViewCtrl.getToolManager()).getFreeTextCacheFileName();
            if (!Utils.isTablet(mPdfViewCtrl.getContext()) &&
                mPdfViewCtrl.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                fallbackFreeTextDialog(null, true);
            } else if (mCurrentFreeTextEditMode == ANNOTATION_FREE_TEXT_PREFERENCE_DIALOG) {
                fallbackFreeTextDialog(null, false);
            } else {
                initInlineFreeTextEditing(null);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void editAnnot() {
        try {
            if (((ToolManager) (mPdfViewCtrl.getToolManager())).editInkAnnots() && mAnnot != null) {
                ((ToolManager) (mPdfViewCtrl.getToolManager())).onInkEditSelected(mAnnot);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Updates the size, location and appearance of the annotation.
     *
     * @throws PDFNetException PDFNet exception
     */
    protected void updateAnnot() throws PDFNetException {
        if (mAnnot == null || onInterceptAnnotationHandling(mAnnot)) {
            return;
        }

        // obtain new and old annotation (for update) positions before refresh appearance
        Rect newAnnotRect = getNewAnnotPagePosition();
        Rect oldUpdateRect = getOldAnnotScreenPosition();
        if (newAnnotRect == null || oldUpdateRect == null) {
            return;
        }

        // It is possible during viewing that GetRect does not return the most accurate bounding box
        // of what is actually rendered, to obtain the correct behavior when resizing/moving, we
        // need to call refreshAppearance before resize
        if (mAnnot.getType() != Annot.e_Stamp && mAnnot.getType() != Annot.e_Text) {
            mAnnot.refreshAppearance();
        }

        if (mContentBox != null && mEffCtrlPtId != e_moving) {
            FreeText freeText = new FreeText(mAnnot);
            Rect oldContentRect = freeText.getContentRect();
            Rect newContentRect = getNewContentRectPagePosition();
            resizeCallout(freeText, oldContentRect, newContentRect);
        } else {
            mAnnot.resize(newAnnotRect);
        }
        // We do not want to call refreshAppearance for stamps
        // to not alter their original appearance.
        if (mAnnot.getType() != Annot.e_Stamp) {
            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), mAnnot);
        }
        buildAnnotBBox();

        mPdfViewCtrl.update(oldUpdateRect);   // Update the old position
        mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
    }

    protected void adjustExtraFreeTextProps(Rect oldContentRect, Rect newContentRect) {
        // override in child handling class
    }

    /**
     * @return The new annotation position in page space
     * @throws PDFNetException PDFNet exception
     */
    protected Rect getNewAnnotPagePosition() throws PDFNetException {
        if (mAnnot == null) {
            return null;
        }

        // Compute the new annotation position
        float x1 = mBBox.left + mCtrlRadius - mPdfViewCtrl.getScrollX();
        float y1 = mBBox.top + mCtrlRadius - mPdfViewCtrl.getScrollY();
        float x2 = mBBox.right - mCtrlRadius - mPdfViewCtrl.getScrollX();
        float y2 = mBBox.bottom - mCtrlRadius - mPdfViewCtrl.getScrollY();
        double[] pts1, pts2;
        pts1 = mPdfViewCtrl.convScreenPtToPagePt(x1, y1, mAnnotPageNum);
        pts2 = mPdfViewCtrl.convScreenPtToPagePt(x2, y2, mAnnotPageNum);

        Rect newAnnotRect;
        if (mAnnot.getFlag(Annot.e_no_zoom)) {
            newAnnotRect = new Rect(pts1[0], pts1[1] - mAnnot.getRect().getHeight(), pts1[0] + mAnnot.getRect().getWidth(), pts1[1]);
        } else {
            newAnnotRect = new Rect(pts1[0], pts1[1], pts2[0], pts2[1]);
        }
        newAnnotRect.normalize();
        return newAnnotRect;
    }

    /**
     * @return The new annotation content rect position in page space
     * @throws PDFNetException PDFNet exception
     */
    protected Rect getNewContentRectPagePosition() throws PDFNetException {
        if (mAnnot == null || mContentBox == null) {
            return null;
        }

        // Compute the new annotation position
        float x1 = mContentBox.left + mCtrlRadius - mPdfViewCtrl.getScrollX();
        float y1 = mContentBox.top + mCtrlRadius - mPdfViewCtrl.getScrollY();
        float x2 = mContentBox.right - mCtrlRadius - mPdfViewCtrl.getScrollX();
        float y2 = mContentBox.bottom - mCtrlRadius - mPdfViewCtrl.getScrollY();
        double[] pts1, pts2;
        pts1 = mPdfViewCtrl.convScreenPtToPagePt(x1, y1, mAnnotPageNum);
        pts2 = mPdfViewCtrl.convScreenPtToPagePt(x2, y2, mAnnotPageNum);

        Rect newAnnotRect;
        if (mAnnot.getFlag(Annot.e_no_zoom)) {
            newAnnotRect = new Rect(pts1[0], pts1[1] - mAnnot.getRect().getHeight(), pts1[0] + mAnnot.getRect().getWidth(), pts1[1]);
        } else {
            newAnnotRect = new Rect(pts1[0], pts1[1], pts2[0], pts2[1]);
        }
        newAnnotRect.normalize();
        return newAnnotRect;
    }

    /**
     * @return The old annotation position in screen space
     * @throws PDFNetException PDFNet exception
     */
    protected Rect getOldAnnotScreenPosition() throws PDFNetException {
        if (mAnnot == null) {
            return null;
        }

        Rect oldUpdateRect = mPdfViewCtrl.getScreenRectForAnnot(mAnnot, mAnnotPageNum);
        oldUpdateRect.normalize();
        return oldUpdateRect;
    }

    /**
     * The overload implementation of {@link Tool#onKeyUp(int, KeyEvent)}.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            return true;
        }

        if (mAnnot != null && isQuickMenuShown()) {
            if (hasMenuEntry(R.id.qm_copy) && ShortcutHelper.isCopy(keyCode, event)) {
                closeQuickMenu();
                AnnotationClipboardHelper.copyAnnot(mPdfViewCtrl.getContext(), mAnnot, mPdfViewCtrl,
                    new AnnotationClipboardHelper.OnClipboardTaskListener() {
                        @Override
                        public void onnClipboardTaskDone(String error) {
                            if (error == null && mPdfViewCtrl.getContext() != null) {
                                if (PdfViewCtrlSettingsManager.shouldShowHowToPaste(mPdfViewCtrl.getContext())) {
                                    // not show the copy/paste teach if mouse is not connected
                                    PointF point = mPdfViewCtrl.getCurrentMousePosition();
                                    if (point.x != 0f || point.y != 0f) {
                                        CommonToast.showText(mPdfViewCtrl.getContext(), R.string.tools_copy_annot_teach, Toast.LENGTH_SHORT);
                                    }
                                }
                            }
                        }
                    });

                return true;
            }

            if (hasMenuEntry(R.id.qm_copy) && hasMenuEntry(R.id.qm_delete) && ShortcutHelper.isCut(keyCode, event)) {
                closeQuickMenu();
                AnnotationClipboardHelper.copyAnnot(mPdfViewCtrl.getContext(), mAnnot, mPdfViewCtrl,
                    new AnnotationClipboardHelper.OnClipboardTaskListener() {
                        @Override
                        public void onnClipboardTaskDone(String error) {
                            if (error == null && mPdfViewCtrl.getContext() != null) {
                                if (PdfViewCtrlSettingsManager.shouldShowHowToPaste(mPdfViewCtrl.getContext())) {
                                    // not show the copy/paste teach if mouse is not connected
                                    PointF point = mPdfViewCtrl.getCurrentMousePosition();
                                    if (point.x != 0f || point.y != 0f) {
                                        CommonToast.showText(mPdfViewCtrl.getContext(), R.string.tools_copy_annot_teach, Toast.LENGTH_SHORT);
                                    }
                                }
                                deleteAnnot();
                            }
                        }
                    });
                return true;
            }

            if (hasMenuEntry((R.id.qm_delete)) && ShortcutHelper.isDeleteAnnot(keyCode, event)) {
                closeQuickMenu();
                deleteAnnot();
                return true;
            }

            if (ShortcutHelper.isStartEdit(keyCode, event)) {
                if (hasMenuEntry(R.id.qm_text)) {
                    closeQuickMenu();
                    enterText();
                    return true;
                } else if (hasMenuEntry(R.id.qm_edit)) { // ink
                    closeQuickMenu();
                    editAnnot();
                    return true;
                }
            }
        }

        if (mInEditMode) {
            if (ShortcutHelper.isCommitText(keyCode, event)) {
                // if DialogFreeTextNote is open then it swallows keys event, hence we only handle
                // inline edit
                saveAndQuitInlineEditText(false);
                // hide soft keyboard
                InputMethodManager imm = (InputMethodManager) mPdfViewCtrl.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(mPdfViewCtrl.getRootView().getWindowToken(), 0);
                }
            }
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Edits selected annotation size
     *
     * @param priorEventMode prior event mode
     * @return true is successfully modified annotation size, false otherwise
     */
    protected boolean editAnnotSize(PDFViewCtrl.PriorEventMode priorEventMode) {
        if (mAnnot == null) {
            return false;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            if (mModifiedAnnot) {
                mModifiedAnnot = false;
                raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                updateAnnot();
                raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);
            } else if (priorEventMode == PDFViewCtrl.PriorEventMode.PINCH || priorEventMode == PDFViewCtrl.PriorEventMode.DOUBLE_TAP) {
                setCtrlPts();
            }

            // Show sticky note dialog directly, if set so.
            if (mUpFromStickyCreate && !mUpFromStickyCreateDlgShown) {
                handleStickyNote(mForceSameNextToolMode, true);
                return false;
            }

        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
        return true;
    }

    /**
     * The overload implementation of {@link Tool#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        mIsScaleBegun = true;
        // hide edit text during scaling
        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            saveAndQuitInlineEditText(true);
        }
        return super.onScaleBegin(x, y);
    }

    @Override
    public boolean onScale(float x, float y) {
        setCtrlPts();

        return super.onScale(x, y);
    }

    /**
     * The overload implementation of {@link Tool#onScaleEnd(float, float)}.
     */
    @Override
    public boolean onScaleEnd(float x, float y) {
        super.onScaleEnd(x, y);

        mIsScaleBegun = false;

        if (mAnnot != null) {
            // Scaled and if while moving, disable moving and set the control points back to where
            // the annotation is; this is to avoid complications.
            mScaled = true;
            setCtrlPts();
            mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));
            if (isQuickMenuShown()) {
                closeQuickMenu();
                showMenu(getAnnotRect());
            }
        }
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        super.onFlingStop();

        mIsScaleBegun = false;

        if (mAnnot != null) {
            if (!mCtrlPtsSet) {
                setCtrlPts(); // May be preceded by annotation creation touch up.
            }
            mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));
            if (isQuickMenuShown()) {
                closeQuickMenu();
                showMenu(getAnnotRect());
            }
        }
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (sDebug) Log.d("AnnotEdit", "onLayout: " + changed);
        if (mAnnot != null) {
            if (!mPdfViewCtrl.isContinuousPagePresentationMode(mPdfViewCtrl.getPagePresentationMode())) {
                if (mAnnotPageNum != mPdfViewCtrl.getCurrentPage()) {
                    // Now in single page mode, and the annotation is not on this page, quit this
                    // tool mode.
                    if (sDebug) Log.d(TAG, "going to unsetAnnot: onLayout");
                    unsetAnnot();
                    mNextToolMode = ToolMode.PAN;
                    setCtrlPts();
                    mEffCtrlPtId = e_unknown;
                    closeQuickMenu();
                    return;
                }
            }

            setCtrlPts();
            if (isQuickMenuShown() && changed) {
                closeQuickMenu();
                showMenu(getAnnotRect());
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onLongPress(MotionEvent)}.
     */
    @Override
    public boolean onLongPress(MotionEvent e) {
        super.onLongPress(e);

        if (!hasAnnotSelected()) {
            return false;
        }

        // if we are editing a free text annot, consume the event
        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            return true;
        }

        // if the annot has been selected through Pan and not through AnnotEdit
        // we require to set bounding box and control point in onLongPress
        if (mEffCtrlPtId == e_unknown) {
            int x = (int) (e.getX() + 0.5);
            int y = (int) (e.getY() + 0.5);
            Annot tempAnnot = mPdfViewCtrl.getAnnotationAt(x, y);
            if (mAnnot != null && mAnnot.equals(tempAnnot)) {
                setCtrlPts();
                mEffCtrlPtId = e_moving;
            }
        }

        if (mEffCtrlPtId != e_unknown && !onInterceptAnnotationHandling(mAnnot)) {
            mNextToolMode = getToolMode();
            setCtrlPts();
            mEffCtrlPtId = e_moving;
            try {
                if (mAnnot != null && (mAnnot.getType() == Annot.e_Link || mAnnot.getType() == Annot.e_Widget)) {
                    showMenu(getAnnotRect());
                }
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        } else {
            if (sDebug) Log.d(TAG, "going to unsetAnnot");
            unsetAnnot();
            mNextToolMode = ToolMode.PAN;
            setCtrlPts();
            mEffCtrlPtId = e_unknown;
        }
        // onDown will not be called after onLongPress, so we need to set mBBoxOnDown:
        mBBoxOnDown.set(mBBox);
        if (mContentBox != null) {
            if (mContentBoxOnDown == null) {
                mContentBoxOnDown = new RectF();
            }
            mContentBoxOnDown.set(mContentBox);
        }
        mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onScrollChanged(int, int, int, int)}.
     */
    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        // don't show the quick menu during scale
        if (!mIsScaleBegun && mAnnot != null && (Math.abs(t - oldt) <= 1) && !isQuickMenuShown() && (mInlineEditText == null || !mInlineEditText.isEditing())) {
            showMenu(getAnnotRect());
        }
    }

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);

        float x = e.getX() + mPdfViewCtrl.getScrollX();
        float y = e.getY() + mPdfViewCtrl.getScrollY();

        // Check if editing a free text annot and tapped out side text box
        if (!mBBox.contains(x, y) && (mInlineEditText != null && mInlineEditText.isEditing()) && mAnnotIsFreeText) {
            // saveAndQuitInlineEditText(); // do it in onUp rather than here in onDown
            mTapToSaveFreeTextAnnot = true;
            mSaveFreeTextAnnotInOnUp = true;
            return true;
        }

        if (mAnnotIsLine) {
            // let child handle it
            return false;
        }

        // Re-compute the annotation's bounding box on screen, since the zoom
        // factor may have been changed.
        if (mAnnot != null) {
            mPageCropOnClientF = Utils.buildPageBoundBoxOnClient(mPdfViewCtrl, mAnnotPageNum);
        }

        // Check if any control point is hit
        mEffCtrlPtId = e_unknown;
        float thresh = mCtrlRadius * 2.25f;
        float shortest_dist = -1;
        int pointsCnt = CTRL_PTS_CNT;
        if (mAnnotIsSignature || mAnnotIsStamper) {
            pointsCnt = 4;
        }
        for (int i = 0; i < pointsCnt; ++i) {
            if (isAnnotResizable()) {
                // Sticky note and text markup cannot be re-sized
                float s = mCtrlPts[i].x;
                float t = mCtrlPts[i].y;

                float dist = (x - s) * (x - s) + (y - t) * (y - t);
                dist = (float) Math.sqrt(dist);
                if (dist <= thresh && (dist < shortest_dist || shortest_dist < 0)) {
                    mEffCtrlPtId = i;
                    shortest_dist = dist;
                }
            }

            mCtrlPtsOnDown[i].set(mCtrlPts[i]);
        }
        mBBoxOnDown.set(mBBox);
        if (mContentBox != null) {
            if (mContentBoxOnDown == null) {
                mContentBoxOnDown = new RectF();
            }
            mContentBoxOnDown.set(mContentBox);
        }

        // Check if hit within the bounding box without hitting any control point.
        // Note that text markup cannot be moved.
        if (!mAnnotIsTextMarkup && mEffCtrlPtId == e_unknown && mBBox.contains(x, y)) {
            mEffCtrlPtId = e_moving;
        }

        if (mAnnot != null) {
            if (!isInsideAnnot(e) && mEffCtrlPtId == e_unknown) {
                if (mInlineEditText == null || !mInlineEditText.isEditing()) {
                    if (sDebug) Log.d(TAG, "going to unsetAnnot: onDown");
                    unsetAnnot();
                    mNextToolMode = mCurrentDefaultToolMode;
                    setCtrlPts();
                    // Draw away the edit widget
                    mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));
                }
            }
        }

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onMove(MotionEvent, MotionEvent, float, float)}.
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

        if (mEffCtrlPtId != e_unknown) {
            float totalMoveX = e2.getX() - e1.getX();
            float totalMoveY = e2.getY() - e1.getY();
            float thresh = 2f * mCtrlRadius;
            RectF tempRect = new RectF(mBBox);

            float left = mBBoxOnDown.left + mCtrlRadius;
            float right = mBBoxOnDown.right - mCtrlRadius;
            float top = mBBoxOnDown.top + mCtrlRadius;
            float bottom = mBBoxOnDown.bottom - mCtrlRadius;

            if (mEffCtrlPtId == e_moving) {
                left += totalMoveX;
                right += totalMoveX;
                top += totalMoveY;
                bottom += totalMoveY;
                updateCtrlPts(true, left, right, top, bottom, mBBox);
                if (mContentBox != null) {
                    // adjust content box to match bbox
                    float diffLeft = mBBox.left - tempRect.left;
                    float diffRight = mBBox.right - tempRect.right;
                    float diffTop = mBBox.top - tempRect.top;
                    float diffBottom = mBBox.bottom - tempRect.bottom;
                    mContentBox.left += diffLeft;
                    mContentBox.right += diffRight;
                    mContentBox.top += diffTop;
                    mContentBox.bottom += diffBottom;
                }
                mModifiedAnnot = true;
            } else if (!mHandleEffCtrlPtsDisabled) {
                if (mContentBoxOnDown != null) {
                    left = mContentBoxOnDown.left + mCtrlRadius;
                    right = mContentBoxOnDown.right - mCtrlRadius;
                    top = mContentBoxOnDown.top + mCtrlRadius;
                    bottom = mContentBoxOnDown.bottom - mCtrlRadius;
                }
                boolean valid = false;
                switch (mEffCtrlPtId) {
                    case e_ll:
                        if (mCtrlPtsOnDown[e_ll].x + totalMoveX < mCtrlPtsOnDown[e_lr].x - thresh && mCtrlPtsOnDown[e_ll].y + totalMoveY > mCtrlPtsOnDown[e_ul].y + thresh) {
                            left = mCtrlPtsOnDown[e_ll].x + totalMoveX;
                            if (mMaintainAspectRatio) {
                                bottom = mCtrlPtsOnDown[e_ll].y + ((totalMoveX * -1) * mAspectRatio);
                            } else {
                                bottom = mCtrlPtsOnDown[e_ll].y + totalMoveY;
                            }
                            valid = true;
                        }
                        break;
                    case e_lm:
                        if (!mMaintainAspectRatio && mCtrlPtsOnDown[e_lm].y + totalMoveY > mCtrlPtsOnDown[e_ul].y + thresh) {
                            bottom = mCtrlPtsOnDown[e_lm].y + totalMoveY;
                            valid = true;
                        }
                        break;
                    case e_lr:
                        if (mCtrlPtsOnDown[e_ll].x < mCtrlPtsOnDown[e_lr].x + totalMoveX - thresh && mCtrlPtsOnDown[e_ll].y + totalMoveY > mCtrlPtsOnDown[e_ul].y + thresh) {
                            right = mCtrlPtsOnDown[e_lr].x + totalMoveX;
                            if (mMaintainAspectRatio) {
                                bottom = mCtrlPtsOnDown[e_lr].y + (totalMoveX * mAspectRatio);
                            } else {
                                bottom = mCtrlPtsOnDown[e_lr].y + totalMoveY;
                            }
                            valid = true;
                        }
                        break;
                    case e_mr:
                        if (!mMaintainAspectRatio && mCtrlPtsOnDown[e_ll].x < mCtrlPtsOnDown[e_lr].x + totalMoveX - thresh) {
                            right = mCtrlPtsOnDown[e_mr].x + totalMoveX;
                            valid = true;
                        }
                        break;
                    case e_ur:
                        if (mCtrlPtsOnDown[e_ll].x < mCtrlPtsOnDown[e_lr].x + totalMoveX - thresh && mCtrlPtsOnDown[e_ll].y > mCtrlPtsOnDown[e_ul].y + totalMoveY + thresh) {
                            right = mCtrlPtsOnDown[e_ur].x + totalMoveX;
                            if (mMaintainAspectRatio) {
                                top = mCtrlPtsOnDown[e_ur].y + ((totalMoveX * -1) * mAspectRatio);
                            } else {
                                top = mCtrlPtsOnDown[e_ur].y + totalMoveY;
                            }
                            valid = true;
                        }
                        break;
                    case e_um:
                        if (!mMaintainAspectRatio && mCtrlPtsOnDown[e_ll].y > mCtrlPtsOnDown[e_ul].y + totalMoveY + thresh) {
                            top = mCtrlPtsOnDown[e_um].y + totalMoveY;
                            valid = true;
                        }
                        break;
                    case e_ul:
                        if (mCtrlPtsOnDown[e_ll].x + totalMoveX < mCtrlPtsOnDown[e_lr].x - thresh && mCtrlPtsOnDown[e_ll].y > mCtrlPtsOnDown[e_ul].y + totalMoveY + thresh) {
                            left = mCtrlPtsOnDown[e_ul].x + totalMoveX;
                            if (mMaintainAspectRatio) {
                                top = mCtrlPtsOnDown[e_ul].y + (totalMoveX * mAspectRatio);
                            } else {
                                top = mCtrlPtsOnDown[e_ul].y + totalMoveY;
                            }
                            valid = true;
                        }
                        break;
                    case e_ml:
                        if (!mMaintainAspectRatio && mCtrlPtsOnDown[e_ll].x + totalMoveX < mCtrlPtsOnDown[e_lr].x - thresh) {
                            left = mCtrlPtsOnDown[e_ml].x + totalMoveX;
                            valid = true;
                        }
                        break;
                }

                if (valid) {
                    if (mContentBox != null) {
                        updateCtrlPts(false, left, right, top, bottom, mContentBox);
                    } else {
                        updateCtrlPts(false, left, right, top, bottom, mBBox);
                    }
                    mModifiedAnnot = true;
                }
            }

            float min_x = Math.min(tempRect.left, mBBox.left);
            float max_x = Math.max(tempRect.right, mBBox.right);
            float min_y = Math.min(tempRect.top, mBBox.top);
            float max_y = Math.max(tempRect.bottom, mBBox.bottom);
            mPdfViewCtrl.invalidate((int) min_x - 1, (int) min_y - 1, (int) Math.ceil(max_x) + 1, (int) Math.ceil(max_y) + 1);
            return true;
        } else {
            showTransientPageNumber();
            return false;
        }
    }

    /**
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        super.onUp(e, priorEventMode);
        if (sDebug) Log.d(TAG, "onUp");
        // Avoid double entry, if double tapped.
        if (mUpFromStickyCreateDlgShown) {
            return false;
        }

        if (mUpFromCalloutCreate) {
            mUpFromCalloutCreate = false;
            closeQuickMenu();
            enterText();

            mNextToolMode = getToolMode();
            return false;
        }

        if (mAnnotIsLine) {
            // let child handle it
            return false;
        }

        if (mScaled) {
            mScaled = false;
            if (mAnnot != null) {
                if (mModifiedAnnot) {
                    mModifiedAnnot = false;
                }
            }
            return false;
        }

        if (mSaveFreeTextAnnotInOnUp) {
            saveAndQuitInlineEditText(false);
            mSaveFreeTextAnnotInOnUp = false;
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (toolManager.isEditFreeTextOnTap()) {
                unsetAnnot();
            }
        }

        if (!mHasMenuPermission && mAnnot != null) {
            showMenu(getAnnotRect());
        }

        mNextToolMode = getToolMode();
        mScaled = false;

        if (hasAnnotSelected()
            && (mModifiedAnnot || !mCtrlPtsSet
            || priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING
            || priorEventMode == PDFViewCtrl.PriorEventMode.PINCH
            || priorEventMode == PDFViewCtrl.PriorEventMode.DOUBLE_TAP)) {
            if (!mCtrlPtsSet) {
                setCtrlPts();
            }

            if (!editAnnotSize(priorEventMode)) {
                return false;
            }

            showMenu(getAnnotRect());

            return priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING || priorEventMode == PDFViewCtrl.PriorEventMode.FLING;
        } else {
            return false;
        }
    }

    /**
     * Updates control points with new position.
     *
     * @param translate True if all control points were translated; False otherwise
     * @param left      The leftmost of control points
     * @param right     The rightmost of control points
     * @param top       The topmost of control points
     * @param bottom    The bottommost of control points
     */
    protected void updateCtrlPts(boolean translate,
                                 float left,
                                 float right,
                                 float top,
                                 float bottom,
                                 RectF which) {
        if (mPageCropOnClientF != null) {
            float w = right - left;
            float h = bottom - top;

            // Bounding along x-axis
            if (right > mPageCropOnClientF.right) {
                right = mPageCropOnClientF.right;
                if (translate) {
                    left = right - w;
                } else if (mMaintainAspectRatio) {
                    // If maintaining aspect ratio, must adjust height of box to
                    // correspond with new width. Use newly adjusted width and the aspect ratio
                    // to calculate the new height of the box.
                    float width = right - left;
                    float height = width * mAspectRatio;

                    if (mEffCtrlPtId == e_lr) {
                        // change box height from bottom
                        bottom = top + height;
                    } else if (mEffCtrlPtId == e_ur) {
                        // change box height from top
                        top = bottom - height;
                    }
                }
            }
            if (left < mPageCropOnClientF.left) {
                left = mPageCropOnClientF.left;
                if (translate) {
                    right = left + w;
                } else if (mMaintainAspectRatio) {
                    // If maintaining aspect ratio, must adjust height of box to
                    // correspond with new width. Use newly adjusted width and the aspect ratio
                    // to calculate the new height of the box.
                    float width = right - left;
                    float height = width * mAspectRatio;

                    if (mEffCtrlPtId == e_ll) {
                        // change box height from bottom
                        bottom = top + height;
                    } else if (mEffCtrlPtId == e_ul) {
                        // change box height from top
                        top = bottom - height;
                    }
                }
            }

            // Bounding along y-axis
            if (top < mPageCropOnClientF.top) {
                top = mPageCropOnClientF.top;
                if (translate) {
                    bottom = top + h;
                } else if (mMaintainAspectRatio) {
                    // If maintaining aspect ratio, must adjust width of box to
                    // correspond with new height. Use newly adjusted height and the aspect ratio
                    // to calculate the new width of the box.
                    float height = bottom - top;
                    float width = height * (1 / mAspectRatio);

                    if (mEffCtrlPtId == e_ul) {
                        // change box width from left
                        left = right - width;
                    } else if (mEffCtrlPtId == e_ur) {
                        // change box width from right
                        right = left + width;
                    }
                }
            }
            if (bottom > mPageCropOnClientF.bottom) {
                bottom = mPageCropOnClientF.bottom;
                if (translate) {
                    top = bottom - h;
                } else if (mMaintainAspectRatio) {
                    // If maintaining aspect ratio, must adjust width of box to
                    // correspond with new height. Use newly adjusted height and the aspect ratio
                    // to calculate the new width of the box.
                    float height = bottom - top;
                    float width = height * (1 / mAspectRatio);

                    if (mEffCtrlPtId == e_ll) {
                        // change box width from left
                        left = right - width;
                    } else if (mEffCtrlPtId == e_lr) {
                        // change box width from right
                        right = left + width;
                    }
                }
            }
        }

        if (!mHandleEffCtrlPtsDisabled && mEffCtrlPtId < RECTANGULAR_CTRL_PTS_CNT) {
            // update control points
            mCtrlPts[e_ll].x = mCtrlPts[e_ul].x = mCtrlPts[e_ml].x = left;
            mCtrlPts[e_lr].x = mCtrlPts[e_ur].x = mCtrlPts[e_mr].x = right;
            mCtrlPts[e_ll].y = mCtrlPts[e_lr].y = mCtrlPts[e_lm].y = bottom;
            mCtrlPts[e_ur].y = mCtrlPts[e_ul].y = mCtrlPts[e_um].y = top;
            mCtrlPts[e_ml].y = mCtrlPts[e_mr].y = (bottom + top) / 2;
            mCtrlPts[e_lm].x = mCtrlPts[e_um].x = (left + right) / 2;

            if (mAnnotView != null) {
                PointF[] ctrlPts = new PointF[CTRL_PTS_CNT];
                ctrlPts[e_lm] = new PointF(mCtrlPts[e_lm].x - left, mCtrlPts[e_lm].y - top);
                ctrlPts[e_ml] = new PointF(mCtrlPts[e_ml].x - left, mCtrlPts[e_ml].y - top);
                mAnnotView.setCtrlPts(ctrlPts);
            }
        }

        // update BBox
        if (which != null) {
            which.left = left - mCtrlRadius;
            which.top = top - mCtrlRadius;
            which.right = right + mCtrlRadius;
            which.bottom = bottom + mCtrlRadius;
        }

        if (mAnnotView != null) {
            mAnnotView.layout((int) (left + 0.5),
                (int) (top + 0.5),
                (int) (right + 0.5),
                (int) (bottom + 0.5));
        }
    }

    /**
     * The overload implementation of {@link Tool#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mInlineEditText != null && mInlineEditText.isEditing()) {
            saveAndQuitInlineEditText(false);
            closeQuickMenu();
        }
    }

    /**
     * The overload implementation of {@link Tool#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return mInlineEditText != null && mInlineEditText.isEditing();
    }

    /**
     * The overload implementation of {@link DialogAnnotNoteListener#onAnnotButtonPressed(int)}.
     */
    @Override
    public void onAnnotButtonPressed(int button) {
        mAnnotButtonPressed = button;
    }

    /**
     * The overload implementation of {@link Tool#showMenu(RectF, QuickMenu)}.
     */
    @Override
    public boolean showMenu(RectF anchor_rect, QuickMenu quickMenu) {
        return !onInterceptAnnotationHandling(mAnnot) && super.showMenu(anchor_rect, quickMenu);
    }

    private void deleteStickyAnnot() {
        if (mAnnot == null || onInterceptAnnotationHandling(mAnnot)) {
            return;
        }
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

    private void cancelNoteCreate(boolean forceSameNextToolMode, boolean backToPan) {
        mUpFromStickyCreate = false;
        mUpFromStickyCreateDlgShown = false;

        if (backToPan) {
            mNextToolMode = ToolMode.PAN;
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            ToolManager.Tool tool = toolManager.createTool(mNextToolMode, null);
            ((com.pdftron.pdf.tools.Tool) tool).mForceSameNextToolMode = forceSameNextToolMode;
            toolManager.setTool(tool);
        } else if (mAnnot != null) {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            toolManager.selectAnnot(mAnnot, mAnnotPageNum);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void handleAnnotNote(final boolean forceSameNextToolMode) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putStringArray(KEYS, new String[]{"forceSameNextTool"});
        bundle.putBoolean("forceSameNextTool", forceSameNextToolMode);
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlock = true;
            final Markup markup = new Markup(mAnnot);

            // adding/editing a note to a pen or shape annotation
            mDialogAnnotNote = new DialogAnnotNote(mPdfViewCtrl, markup.getContents());
            mDialogAnnotNote.setAnnotNoteListener(this);
            // set buttons
            mDialogAnnotNote.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    prepareDialogAnnotNoteDismiss(forceSameNextToolMode);
                }
            });
            mDialogAnnotNote.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancelNoteCreate(forceSameNextToolMode, false);
                }
            });

            mDialogAnnotNote.show();
            mUpFromStickyCreateDlgShown = true;
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    private void handleStickyNote(final boolean forceSameNextToolMode, final boolean upFromStickyCreate) {
        if (mAnnot == null || mUpFromStickyCreateDlgShown) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putStringArray(KEYS, new String[]{"forceSameNextTool", "upFromStickyCreate"});
        bundle.putBoolean("forceSameNextTool", forceSameNextToolMode);
        bundle.putBoolean("upFromStickyCreate", upFromStickyCreate);
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlock = true;

            final Markup markup = new Markup(mAnnot);

            // if user opens existing sticky note
            boolean existingStickyNote = !upFromStickyCreate;
            Text t = new Text(mAnnot);
            String iconType = t.getIconName();

            ColorPt colorPt = mAnnot.getColorAsRGB();
            int r = (int) (Math.round(colorPt.get(0) * 255));
            int g = (int) (Math.round(colorPt.get(1) * 255));
            int b = (int) (Math.round(colorPt.get(2) * 255));
            int iconColor = Color.rgb(r, g, b);
            double iconOpacity = markup.getOpacity();
            String contents = markup.getContents();
            if (!Utils.isNullOrEmpty(contents)) {
                existingStickyNote = true;
            }

            mDialogStickyNote = new DialogStickyNote(mPdfViewCtrl, markup.getContents(), existingStickyNote, iconType, iconColor, (float) iconOpacity);
            mDialogStickyNote.setAnnotNoteListener(this);
            // set buttons
            mDialogStickyNote.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {
                    prepareDialogStickyNoteDismiss(forceSameNextToolMode);
                }
            });
            mDialogStickyNote.setAnnotAppearanceChangeListener(this);
            mDialogStickyNote.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancelNoteCreate(forceSameNextToolMode, false);
                }
            });

            mDialogStickyNote.show();
            mUpFromStickyCreateDlgShown = true;
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    private void prepareDialogStickyNoteDismiss(boolean forceSameNextToolMode) {
        if (mPdfViewCtrl == null || mAnnot == null || mDialogStickyNote == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "prepareDialogStickyNoteDismiss");
        bundle.putStringArray(KEYS, new String[]{"contents", "pressedButton", "forceSameNextTool"});
        bundle.putBoolean("forceSameNextTool", forceSameNextToolMode);
        bundle.putInt("pressedButton", mAnnotButtonPressed);
        bundle.putString("contents", mDialogStickyNote.getNote());
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        try {
            Markup markup = new Markup(mAnnot);
            boolean existingStickyNote = mDialogStickyNote.isExistingNote();

            // positive button
            if (mAnnotButtonPressed == DialogInterface.BUTTON_POSITIVE) {
                boolean shouldUnlock = false;
                // SAVE button
                if (!existingStickyNote || mDialogStickyNote.isEditEnabled()) {
                    try {
                        String newContent = mDialogStickyNote.getNote();
                        Popup popup = markup.getPopup();
                        if (!existingStickyNote || (newContent != null && (popup == null || !popup.isValid()
                            || !newContent.equals(popup.getContents())))) {
                            // Locks the document first as accessing annotation/doc
                            // information isn't thread safe.
                            mPdfViewCtrl.docLock(true);
                            shouldUnlock = true;
                            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                            Utils.handleEmptyPopup(mPdfViewCtrl, markup);
                            popup = markup.getPopup();
                            popup.setContents(newContent);
                            if (!existingStickyNote) {
                                setAuthor(markup);
                            }
                            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
                        }
                    } catch (Exception e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    } finally {
                        if (shouldUnlock) {
                            mPdfViewCtrl.docUnlock();
                        }
                    }
                    mUpFromStickyCreate = false;
                    mUpFromStickyCreateDlgShown = false;

                    showMenu(getAnnotRect());
                } else {
                    // CLOSE button
                    showMenu(getAnnotRect());
                    cancelNoteCreate(forceSameNextToolMode, false);
                }
            } else if (mAnnotButtonPressed == DialogInterface.BUTTON_NEGATIVE && existingStickyNote) {
                // negative button
                // CANCEL button
                // Don't save note edits and show menu
                if (mDialogStickyNote.isEditEnabled()) {
                    showMenu(getAnnotRect());
                    cancelNoteCreate(forceSameNextToolMode, false);
                } else {
                    // DELETE button
                    deleteStickyAnnot();
                    cancelNoteCreate(forceSameNextToolMode, !mForceSameNextToolMode);
                }
            } else {
                // cancelled (through back button)
                if (!existingStickyNote) {
                    cancelNoteCreate(forceSameNextToolMode, true);
                }
            }
            mAnnotButtonPressed = 0;
            mDialogStickyNote.prepareDismiss();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void prepareDialogAnnotNoteDismiss(boolean forceSameNextToolMode) {
        if (mPdfViewCtrl == null || mAnnot == null || mDialogAnnotNote == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "prepareDialogAnnotNoteDismiss");
        bundle.putStringArray(KEYS, new String[]{"contents", "pressedButton", "forceSameNextTool"});
        bundle.putBoolean("forceSameNextTool", forceSameNextToolMode);
        bundle.putInt("pressedButton", mAnnotButtonPressed);
        bundle.putString("contents", mDialogAnnotNote.getNote());
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        try {
            Markup markup = new Markup(mAnnot);
            // positive button
            if (mAnnotButtonPressed == DialogInterface.BUTTON_POSITIVE) {
                boolean shouldUnlock = false;
                try {
                    // Locks the document first as accessing annotation/doc
                    // information isn't thread safe.
                    mPdfViewCtrl.docLock(true);
                    shouldUnlock = true;
                    raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                    Utils.handleEmptyPopup(mPdfViewCtrl, markup);
                    Popup popup = markup.getPopup();
                    String oldContent = popup.getContents();
                    String newContent = mDialogAnnotNote.getNote();
                    popup.setContents(mDialogAnnotNote.getNote());
                    if (Utils.isTextCopy(markup) && newContent != null && !newContent.equals(oldContent)) {
                        Utils.removeTextCopy(markup);
                    }
                    updateQuickMenuNoteText(mDialogAnnotNote.getNote());
                    raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    if (shouldUnlock) {
                        mPdfViewCtrl.docUnlock();
                    }
                }
                mUpFromStickyCreate = false;
                mUpFromStickyCreateDlgShown = false;

                if (forceSameNextToolMode) {
                    if (mCurrentDefaultToolMode != ToolMode.PAN) {
                        mNextToolMode = mCurrentDefaultToolMode;
                    } else {
                        mNextToolMode = ToolMode.TEXT_ANNOT_CREATE;
                    }
                    ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                    ToolManager.Tool tool = toolManager.createTool(mNextToolMode, null);
                    ((Tool) tool).mForceSameNextToolMode = true;
                    ((Tool) tool).mCurrentDefaultToolMode = mCurrentDefaultToolMode;
                    toolManager.setTool(tool);
                } else {
                    ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                    toolManager.selectAnnot(mAnnot, mAnnotPageNum);
                }
            } else {
                // negative button or dialog cancelled (via back button)
                if (!mDialogAnnotNote.isEditEnabled()) {
                    boolean shouldUnlock = false;
                    try {
                        // Locks the document first as accessing annotation/doc
                        // information isn't thread safe.
                        mPdfViewCtrl.docLock(true);
                        shouldUnlock = true;
                        raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                        Utils.handleEmptyPopup(mPdfViewCtrl, markup);
                        Popup popup = markup.getPopup();
                        popup.setContents("");
                        Utils.removeTextCopy(markup);
                        setAuthor(markup);
                        updateQuickMenuNoteText("");
                        raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
                    } catch (Exception e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    } finally {
                        if (shouldUnlock) {
                            mPdfViewCtrl.docUnlock();
                        }
                    }
                }
                cancelNoteCreate(forceSameNextToolMode, false);
            }
            mAnnotButtonPressed = 0;
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void editTextColor(@ColorInt int color) {
        if (mAnnot == null) {
            return;
        }

        Bundle interceptInfo = new Bundle();
        interceptInfo.putInt("textColor", color);
        interceptInfo.putStringArray(KEYS, new String[]{"textColor"});
        if (onInterceptAnnotationHandling(mAnnot, interceptInfo)) {
            return;
        }

        // Note no need for raising annotation modification, since it will be handled outside of
        // this function
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information
            // isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            ColorPt colorPt = Utils.color2ColorPt(color);
            FreeText freeText = new FreeText(mAnnot);
            freeText.setTextColor(colorPt, 3);
            mAnnot.refreshAppearance();
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);

            int type = Annot.e_FreeText;
            if (AnnotUtils.isCallout(freeText)) {
                type = AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT;
            }

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(getTextColorKey(type), color);
            editor.apply();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void editColor(int color) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editColor");
        bundle.putInt("color", color);
        bundle.putStringArray(KEYS, new String[]{"color"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        // Note no need for raising annotation modification, since it will be handled outside of
        // this function
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information
            // isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            ColorPt colorPt = Utils.color2ColorPt(color);

            if (mAnnotIsFreeText) {
                FreeText freeText = new FreeText(mAnnot);
                if (color != Color.TRANSPARENT) {
                    freeText.setLineColor(colorPt, 3);
                } else {
                    freeText.setLineColor(colorPt, 0);
                }
            } else {
                if (color != Color.TRANSPARENT) {
                    mAnnot.setColor(colorPt, 3);
                } else {
                    mAnnot.setColor(colorPt, 0);
                }
            }

            if (!mAnnotIsSticky) {
                // if color is transparent, then set thickness to 0,
                // and stored the original thickness to SDFObj
                if (color == Color.TRANSPARENT) {
                    com.pdftron.pdf.Annot.BorderStyle bs = mAnnot.getBorderStyle();
                    double thickness = bs.getWidth();
                    if (thickness > 0) {
                        mAnnot.getSDFObj().putNumber(PDFTRON_THICKNESS, thickness);
                    }
                    bs.setWidth(0);
                    mAnnot.setBorderStyle(bs);
                    mAnnot.getSDFObj().erase("AP");
                } else {
                    // if color is not transparent and it contains thickness object
                    // restore the thickness
                    Obj sdfObj = mAnnot.getSDFObj();
                    Obj thicknessObj = sdfObj.findObj(PDFTRON_THICKNESS);
                    if (thicknessObj != null) {
                        // restore thickness
                        double storedThickness = thicknessObj.getNumber();
                        com.pdftron.pdf.Annot.BorderStyle bs = mAnnot.getBorderStyle();
                        bs.setWidth(storedThickness);
                        mAnnot.setBorderStyle(bs);
                        mAnnot.getSDFObj().erase("AP");
                        // erase thickness obj
                        mAnnot.getSDFObj().erase(PDFTRON_THICKNESS);
                    }
                }
            }
            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), mAnnot);
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);

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

    private void editIcon(String icon) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editIcon");
        bundle.putString("icon", icon);
        bundle.putStringArray(KEYS, new String[]{"icon"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        // Note no need for raising annotation modification, since it will be handled outside of
        // this function
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information
            // isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            if (mAnnotIsSticky) {
                Text text = new Text(mAnnot);
                text.setIcon(icon);
            }
            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), mAnnot);
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);

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

    private void editFont(String pdftronFontName) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editFont");
        bundle.putString("fontName", pdftronFontName);
        bundle.putStringArray(KEYS, new String[]{"fontName"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        // Note no need for raising annotation modification, since it will be handled outside of
        // this function
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            FreeText textAnnot = new FreeText(mAnnot);
            String fontDRName = "F0";

            // Create a DR entry for embedding the font
            Obj annotObj = textAnnot.getSDFObj();
            Obj drDict = annotObj.putDict("DR");
            Obj fontDict = drDict.putDict("Font");

            // Embed the font
            Font font = Font.create(mPdfViewCtrl.getDoc(), pdftronFontName, textAnnot.getContents());
            fontDict.put(fontDRName, font.GetSDFObj());
            String fontName = font.getName();

            // Set DA string
            String DA = textAnnot.getDefaultAppearance();
            int slashPosition = DA.indexOf("/", 0);

            // if DR string contains '/' which it always should.
            if (slashPosition > 0) {
                String beforeSlash = DA.substring(0, slashPosition);
                String afterSlash = DA.substring(slashPosition);
                String afterFont = afterSlash.substring(afterSlash.indexOf(" "));
                String updatedDA = beforeSlash + "/" + fontDRName + afterFont;

                textAnnot.setDefaultAppearance(updatedDA);

                textAnnot.refreshAppearance();
                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

                raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
            }

            // save font name with font if not saved already
            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            String fontInfo = settings.getString(ANNOTATION_FREE_TEXT_FONTS, "");
            if (!fontInfo.equals("")) {
                JSONObject systemFontObject = new JSONObject(fontInfo);
                JSONArray systemFontArray = systemFontObject.getJSONArray(ANNOTATION_FREE_TEXT_JSON_FONT);

                for (int i = 0; i < systemFontArray.length(); i++) {
                    JSONObject fontObj = systemFontArray.getJSONObject(i);
                    // if has the same file name as the selected font, save the font name
                    if (fontObj.getString(ANNOTATION_FREE_TEXT_JSON_FONT_PDFTRON_NAME).equals(pdftronFontName)) {
                        fontObj.put(ANNOTATION_FREE_TEXT_JSON_FONT_NAME, fontName);
                        break;
                    }
                }
                fontInfo = systemFontObject.toString();
            }

            SharedPreferences.Editor editor = settings.edit();
            editor.putString(ANNOTATION_FREE_TEXT_FONTS, fontInfo);
            editor.putString(getFontKey(AnnotUtils.getAnnotType(mAnnot)), pdftronFontName);
            editor.apply();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void editFillColor(int color) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editFillColor");
        bundle.putInt("color", color);
        bundle.putStringArray(KEYS, new String[]{"color"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        // Note no need for raising annotation modification, since it will be handled outside of
        // this function
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information
            // isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            if (mAnnot.isMarkup() && !mAnnotIsFreeText) {
                Markup m = new Markup(mAnnot);

                if (color == Color.TRANSPARENT) {
                    ColorPt emptyColorPt = new ColorPt(0, 0, 0, 0);
                    m.setInteriorColor(emptyColorPt, 0);
                } else {
                    ColorPt colorPt = Utils.color2ColorPt(color);
                    m.setInteriorColor(colorPt, 3);
                }
                SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(getColorFillKey(AnnotUtils.getAnnotType(mAnnot)), color);
                editor.apply();
            } else if (mAnnotIsFreeText) {
                FreeText freeText = new FreeText(mAnnot);
                if (color == Color.TRANSPARENT) {
                    ColorPt emptyColorPt = new ColorPt(0, 0, 0, 0);
                    freeText.setColor(emptyColorPt, 0);
                } else {
                    ColorPt colorPt = Utils.color2ColorPt(color);
                    freeText.setColor(colorPt, 3);
                }
                SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(getColorFillKey(AnnotUtils.getAnnotType(mAnnot)), color);
                editor.apply();
            }

            mAnnot.refreshAppearance();
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void editTextSize(float textSize) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editTextSize");
        bundle.putFloat("textSize", textSize);
        bundle.putStringArray(KEYS, new String[]{"textSize"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        // Note no need for raising annotation modification, since it will be handled outside of
        // this function
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            FreeText freeText = new FreeText(mAnnot);
            freeText.setFontSize(textSize);
            freeText.refreshAppearance();

            int type = Annot.e_FreeText;
            if (AnnotUtils.isCallout(freeText)) {
                type = AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT;
            }

            boolean isRightToLeft = Utils.isRightToLeftString(freeText.getContents());
            resizeFreeText(freeText, mAnnot.getRect(), isRightToLeft);

            // Let's recalculate the selection bounding box
            buildAnnotBBox();
            setCtrlPts();

            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putFloat(getTextSizeKey(type), textSize);
            editor.apply();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void editThickness(float thickness) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editThickness");
        bundle.putFloat("thickness", thickness);
        bundle.putStringArray(KEYS, new String[]{"thickness"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        // Note no need for raising annotation modification, since it will be handled outside of
        // this function
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc
            // information isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            int colorCompNum;
            if (mAnnotIsFreeText) {
                FreeText freeText = new FreeText(mAnnot);
                colorCompNum = freeText.getLineColorCompNum();
            } else {
                colorCompNum = mAnnot.getColorCompNum();
            }
            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            com.pdftron.pdf.Annot.BorderStyle bs = mAnnot.getBorderStyle();
            double annotThickness = bs.getWidth();
            boolean canSetWidth = true;
            // if the annot color is transparent and already stored pdftron thickness value,
            // updates stored pdftron thickness value and don't set thickness
            if (colorCompNum == 0 && annotThickness == 0) {
                Obj storedThicknessObj = mAnnot.getSDFObj().findObj(PDFTRON_THICKNESS);
                if (storedThicknessObj != null) {
                    mAnnot.getSDFObj().putNumber(PDFTRON_THICKNESS, thickness);
                    canSetWidth = false;
                }
            }
            if (canSetWidth) {
                bs.setWidth(thickness);
                mAnnot.setBorderStyle(bs);
                if (thickness == 0) {
                    mAnnot.getSDFObj().erase("AP");
                }
            }
            mAnnot.refreshAppearance();
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);

            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putFloat(getThicknessKey(AnnotUtils.getAnnotType(mAnnot)), thickness);
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
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editOpacity");
        bundle.putFloat("opacity", opacity);
        bundle.putStringArray(KEYS, new String[]{"opacity"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        // Note no need for raising annotation modification, since it will be handled outside of
        // this function
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc
            // information isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            if (mAnnot.isMarkup()) {
                Markup m = new Markup(mAnnot);
                m.setOpacity(opacity);
            }
            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), mAnnot);
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);

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

    private void editRuler(RulerItem rulerItem) {
        if (mAnnot == null) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "editRuler");
        bundle.putParcelable("rulerItem", rulerItem);
        bundle.putStringArray(KEYS, new String[]{"rulerItem"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        // Note no need for raising annotation modification, since it will be handled outside of
        // this function
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc
            // information isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (AnnotUtils.isRuler(mAnnot)) {
                raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

                Line line = new Line(mAnnot);
                String label = RulerCreate.getRulerLabel(rulerItem, line.getStartPoint().x, line.getStartPoint().y,
                    line.getEndPoint().x, line.getEndPoint().y);
                line.setContents(label);
                RulerItem.saveToAnnot(line, rulerItem);

                AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), mAnnot);
                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

                raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);

                SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                SharedPreferences.Editor editor = settings.edit();
                editor.putFloat(getRulerBaseValueKey(AnnotUtils.getAnnotType(mAnnot)), rulerItem.mRulerBase);
                editor.putString(getRulerBaseUnitKey(AnnotUtils.getAnnotType(mAnnot)), rulerItem.mRulerBaseUnit);
                editor.putFloat(getRulerTranslateValueKey(AnnotUtils.getAnnotType(mAnnot)), rulerItem.mRulerTranslate);
                editor.putString(getRulerTranslateUnitKey(AnnotUtils.getAnnotType(mAnnot)), rulerItem.mRulerTranslateUnit);
                editor.apply();
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#onRenderingFinished()}.
     */
    @Override
    public void onRenderingFinished() {
        super.onRenderingFinished();

        if (mInlineEditText != null && mInlineEditText.delayViewRemoval()) {
            mInlineEditText.removeView();
            mInlineEditText = null;

            if (!mHasOnCloseCalled) {
                android.os.Handler handler = new android.os.Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        RectF annotRect = getAnnotRect();
                        if (annotRect != null) {
                            showMenu(annotRect);
                        }
                    }
                }, 100);
            }
        }

        if (mInlineEditText != null && mInlineEditText.delaySetContents()) {
            mInlineEditText.setContents();
        }
    }

    /**
     * @return True if the free text annotation is editable.
     */
    public boolean isFreeTextEditing() {
        if (mInlineEditText != null) {
            return mInlineEditText.isEditing();
        }
        return false;
    }

    /**
     * The overload implementation of {@link InlineEditText.InlineEditTextListener#getInlineEditTextPosition()}.
     */
    @Override
    public RectF getInlineEditTextPosition() {
        RectF box = mBBox;
        if (mContentBox != null) {
            box = mContentBox;
        }
        int left = (int) (box.left + mCtrlRadius);
        int right = (int) (box.right - mCtrlRadius);
        int top = (int) (box.top + mCtrlRadius);
        int bottom = (int) (box.bottom - mCtrlRadius);

        // the max width of the edit text is the screen size, shrink
        // the edit text if necessary
        int screenWidth = Utils.getScreenWidth(mPdfViewCtrl.getContext());
        if (box.width() > screenWidth) {
            right = left + screenWidth;
        }

        return new RectF(left, top, right, bottom);
    }

    protected void saveAndQuitInlineEditText(boolean immediatelyRemoveView) {
        mInEditMode = false;
        if (mInlineEditText != null) {
            String contents = mInlineEditText.getContents();
            updateFreeText(contents);

            mInlineEditText.close(immediatelyRemoveView);
            addOldTools();

            mHideCtrlPts = false;
            setCtrlPts();
            mPdfViewCtrl.invalidate((int) Math.floor(mBBox.left), (int) Math.floor(mBBox.top), (int) Math.ceil(mBBox.right), (int) Math.ceil(mBBox.bottom));

            if (immediatelyRemoveView) {
                mInlineEditText = null;
                if (isQuickMenuShown()) {
                    closeQuickMenu();
                }
            }
        }

        // save new edit mode in settings
        if (mUpdateFreeTextEditMode) {
            SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, mCurrentFreeTextEditMode);
            editor.apply();
        }
    }

    /**
     * The overload implementation of {@link InlineEditText.InlineEditTextListener#toggleToFreeTextDialog(String)}.
     */
    @Override
    public void toggleToFreeTextDialog(String interImText) {
        mCurrentFreeTextEditMode = ANNOTATION_FREE_TEXT_PREFERENCE_DIALOG;
        mUpdateFreeTextEditMode = true;

        try {
            fallbackFreeTextDialog(interImText, false);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void fallbackFreeTextDialog(String contents, boolean disableToggleButton) throws PDFNetException {
        Bundle bundle = new Bundle();
        bundle.putString("contents", contents);
        bundle.putBoolean("disableToggleButton", disableToggleButton);
        bundle.putStringArray(KEYS, new String[]{"contents", "disableToggleButton"});
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        removeAnnotView();

        boolean enableSave = true;
        if (contents == null && mAnnot != null) {
            Markup m = new Markup(mAnnot);
            contents = m.getContents();
            enableSave = false;
        }
        final DialogFreeTextNote d = new DialogFreeTextNote(mPdfViewCtrl, contents, enableSave);
        d.addTextWatcher(this);
        d.setAnnotNoteListener(this);
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mInEditMode = false;
                if (mAnnotButtonPressed == DialogInterface.BUTTON_POSITIVE) {
                    updateFreeText(d.getNote());

                    // update editing free text annots preference
                    if (mUpdateFreeTextEditMode) {
                        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, mCurrentFreeTextEditMode);
                        editor.apply();
                    }

                    // remove inline edit text and reshow control points
                    if (mInlineEditText != null && mInlineEditText.isEditing()) {
                        mInlineEditText.close(true);
                        mInlineEditText = null;
                    }
                    mHideCtrlPts = false;
                    setCtrlPts();
                    // show menu is delayed so that the view shift from the keyboard is gone
                    android.os.Handler handler = new android.os.Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showMenu(getAnnotRect());
                        }
                    }, 300);
                } else if (mAnnotButtonPressed == DialogInterface.BUTTON_NEUTRAL) {
                    // switch to inline editing
                    mCurrentFreeTextEditMode = ANNOTATION_FREE_TEXT_PREFERENCE_INLINE;
                    mUpdateFreeTextEditMode = true;

                    // if inline editing is already attached to the view, show the cursor and
                    // update the contents
                    if (mInlineEditText != null) {
                        mInlineEditText.setContents(d.getNote());
                    } else {
                        // otherwise initialized inline editing
                        try {
                            initInlineFreeTextEditing(d.getNote());
                        } catch (Exception e) {
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        }
                    }
                } else {
                    // show menu is delayed so that the view shift from the keyboard is gone
                    android.os.Handler handler = new android.os.Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showMenu(getAnnotRect());
                        }
                    }, 300);

                    // if inline editing was initialized, remove the view and
                    // reshow the annotation
                    if (mAnnot != null && mInlineEditText != null && mInlineEditText.isEditing()) {
                        mInlineEditText.close(true);
                        mInlineEditText = null;

                        // show annotation
                        try {
                            mPdfViewCtrl.showAnnotation(mAnnot);
                            Page page = mAnnot.getPage();
                            mPdfViewCtrl.update(mAnnot, page.getIndex());
                            mPdfViewCtrl.invalidate();
                        } catch (Exception e) {
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        }
                        Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);
                    }
                    mHideCtrlPts = false;
                    setCtrlPts();

                    if (mUpdateFreeTextEditMode) {
                        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putInt(ANNOTATION_FREE_TEXT_PREFERENCE_EDITING, mCurrentFreeTextEditMode);
                        editor.apply();
                    }
                }
                mAnnotButtonPressed = 0;
            }
        });
        d.show();
        mStoredTimeStamp = System.currentTimeMillis();
        if (disableToggleButton) {
            d.disableToggleButton();
        }
    }

    private void initInlineFreeTextEditing(String interimText) throws PDFNetException {
        if (mAnnot == null) {
            return;
        }

        removeAnnotView(false);

        if (interimText == null) {
            Markup m = new Markup(mAnnot);
            interimText = m.getContents();
        }
        mInlineEditText = new InlineEditText(mPdfViewCtrl, mAnnot, mAnnotPageNum, null, this);
        mInlineEditText.addTextWatcher(this);
        // keyboard shortcut
        mInlineEditText.getEditText().setAutoScrollEditTextListener(new AutoScrollEditText.AutoScrollEditTextListener() {
            @Override
            public boolean onKeyUp(int keyCode, KeyEvent event) {
                if (ShortcutHelper.isCommitText(keyCode, event)) {
                    // if DialogFreeTextNote is open then it swallows keys event, hence we only handle
                    // inline edit
                    saveAndQuitInlineEditText(false);
                    // hide soft keyboard
                    InputMethodManager imm = (InputMethodManager) mPdfViewCtrl.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(mPdfViewCtrl.getRootView().getWindowToken(), 0);
                    }
                }
                return true;
            }
        });
        try {
            mPdfViewCtrl.hideAnnotation(mAnnot);
            Page page = mAnnot.getPage();
            mPdfViewCtrl.update(mAnnot, page.getIndex());
            raiseAnnotationPreModifyEvent(mAnnot, page.getIndex());

            mHideCtrlPts = true;
            mPdfViewCtrl.invalidate((int) mBBox.left - 1, (int) mBBox.top - 1, (int) mBBox.right + 1, (int) mBBox.bottom + 1);

            // change style of text to match style of free text annot
            // font size
            FreeText freeText = new FreeText(mAnnot);
            int fontSize = (int) freeText.getFontSize();
            if (fontSize == 0) {
                fontSize = 12;
            }
            mInlineEditText.setTextSize(fontSize);

            // opacity
            Markup m = new Markup(mAnnot);
            int alpha = (int) (m.getOpacity() * 0xFF);

            // font color
            int r, g, b;
            if (freeText.getTextColorCompNum() == 3) {
                ColorPt fillColorPt = freeText.getTextColor();
                r = (int) (Math.round(fillColorPt.get(0) * 255));
                g = (int) (Math.round(fillColorPt.get(1) * 255));
                b = (int) (Math.round(fillColorPt.get(2) * 255));
                int fontColor = Color.argb(alpha, r, g, b);
                mInlineEditText.setTextColor(fontColor);
            }

            // default fill color is white, use free text background
            // color if it has one at full opacity.
            int fillColor = Color.TRANSPARENT;
            if (freeText.getColorCompNum() == 3) {
                ColorPt fillColorPt = freeText.getColorAsRGB();
                r = (int) (Math.round(fillColorPt.get(0) * 255));
                g = (int) (Math.round(fillColorPt.get(1) * 255));
                b = (int) (Math.round(fillColorPt.get(2) * 255));
                fillColor = Color.argb(alpha, r, g, b);
            }
            mInlineEditText.setBackgroundColor(fillColor);

            // set edit text contents
            mInlineEditText.setDelaySetContents(interimText);
            mStoredTimeStamp = System.currentTimeMillis();

            raiseAnnotationModifiedEvent(mAnnot, page.getIndex());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void resizeFreeText(FreeText freeText, Rect adjustedAnnotRect, boolean isRightToLeft) throws PDFNetException {
        double left = adjustedAnnotRect.getX1();
        double top = adjustedAnnotRect.getY1();
        double right = adjustedAnnotRect.getX2();
        double bottom = adjustedAnnotRect.getY2();

        boolean isCallout = AnnotUtils.isCallout(freeText);
        Rect temp = freeText.getContentRect();

        // set the new annot rect
        if (((ToolManager) (mPdfViewCtrl.getToolManager())).isAutoResizeFreeText()) {
            double[] pt1s = mPdfViewCtrl.convPagePtToScreenPt(left, top, mAnnotPageNum);
            double[] pt2s = mPdfViewCtrl.convPagePtToScreenPt(right, bottom, mAnnotPageNum);
            double scLeft = pt1s[0];
            double scTop = pt1s[1];
            double scRight = pt2s[0];
            double scBottom = pt2s[1];

            // find top left (LTR) or top right (RTL)
            double x = Math.min(scLeft, scRight);
            double y = Math.min(scTop, scBottom);
            if (isRightToLeft) {
                x = Math.max(scLeft, scRight);
            }

            double[] pt3s = mPdfViewCtrl.convScreenPtToPagePt(x, y, mAnnotPageNum);
            Point targetPoint = new Point(pt3s[0], pt3s[1]);

            Rect bbox = FreeTextCreate.getTextBBoxOnPage(mPdfViewCtrl, mAnnotPageNum, targetPoint);
            if (bbox != null) {
                if (isCallout) {
                    freeText.setRect(bbox);
                    freeText.setContentRect(bbox);
                } else {
                    freeText.resize(bbox);
                }
                freeText.refreshAppearance();

                Rect resizeRect = FreeTextCreate.calcFreeTextBBox(mPdfViewCtrl, freeText, mAnnotPageNum,
                    isRightToLeft, targetPoint);
                if (isCallout) {
                    resizeCallout(freeText, temp, resizeRect);
                } else {
                    freeText.resize(resizeRect);
                }
                freeText.refreshAppearance();
            }
        } else {
            if (isCallout) {
                resizeCallout(freeText, temp, adjustedAnnotRect);
            } else {
                freeText.setRect(adjustedAnnotRect);
            }
        }
    }

    protected void resizeCallout(FreeText callout,
                                 Rect originalAnnotRect,
                                 Rect adjustedAnnotRect) throws PDFNetException {
        adjustExtraFreeTextProps(originalAnnotRect, adjustedAnnotRect);
        callout.setRect(adjustedAnnotRect);
        callout.setContentRect(adjustedAnnotRect);
        callout.refreshAppearance();
        setCtrlPts(); // update ctrl points
    }

    private void updateFreeText(String contents) {
        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "updateFreeText");
        bundle.putStringArray(KEYS, new String[]{"contents"});
        bundle.putString("contents", contents);
        if (mAnnot == null || onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc
            // information isn't thread safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            final FreeText freeText = new FreeText(mAnnot);
            if (!contents.equals("")) {
                raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);
                freeText.setContents(contents);
                boolean isRightToLeft = Utils.isRightToLeftString(contents);
                if (isRightToLeft) {
                    freeText.setQuaddingFormat(2); // right justification
                } else if (Utils.isLeftToRightString(contents)) {
                    freeText.setQuaddingFormat(0);
                }

                // resize the edit text to accommodate the contents
                // but only if the inline edit text was used
                if (mInlineEditText != null) {
                    // determine the rotation of the page based on the users
                    // perspective
                    int pageRotation = mPdfViewCtrl.getDoc().getPage(mAnnotPageNum).getRotation();
                    int viewRotation = mPdfViewCtrl.getPageRotation();
                    int annotRotation = ((pageRotation + viewRotation) % 4) * 90;

                    // get the edit texts height and width
                    EditText editText = mInlineEditText.getEditText();
                    int editTextHeight = editText.getHeight();
                    int editTextWidth = editText.getWidth();

                    com.pdftron.pdf.Rect contentRect = freeText.getContentRect();
                    RectF contentBox = new RectF(
                        (float) contentRect.getX1(),
                        (float) contentRect.getY1(),
                        (float) contentRect.getX2(),
                        (float) contentRect.getY2());

                    // get the annotations original width (which can be
                    // its height depending on the page & view rotation
                    float annotBBoxWidth = contentBox.width();
                    if (annotRotation == 90 || annotRotation == 270) {
                        annotBBoxWidth = contentBox.height();
                    }

                    // calculate the pixels to page units conversion
                    // use this to calculate the height of the edit text in page
                    // space
                    float convRatio = annotBBoxWidth / editTextWidth;
                    int heightInPageUnits = (int) (editTextHeight * convRatio);

                    // adjust the original annots bounding box using the
                    // new height
                    double right, left, top, bottom;
                    right = contentBox.right;
                    left = contentBox.left;
                    top = contentBox.bottom;
                    bottom = contentBox.top;

                    if (annotRotation == 0) {
                        bottom = contentBox.bottom - heightInPageUnits;
                        if (bottom > contentBox.top) {
                            bottom = contentBox.top;
                        }
                    } else if (annotRotation == 90) {
                        right = contentBox.left + heightInPageUnits;
                        if (right < contentBox.right) {
                            right = contentBox.right;
                        }
                    } else if (annotRotation == 180) {
                        top = contentBox.top + heightInPageUnits;
                        if (top < contentBox.bottom) {
                            top = contentBox.bottom;
                        }
                    } else {
                        left = contentBox.right - heightInPageUnits;
                        if (left > contentBox.left) {
                            left = contentBox.left;
                        }
                    }

                    freeText.refreshAppearance();

                    Rect adjustedAnnotRect = new Rect(left, top, right, bottom);
                    adjustedAnnotRect.normalize();

                    resizeFreeText(freeText, adjustedAnnotRect, isRightToLeft);
                }

                // re-embed font if we originally embedded it
                // check if we embedded the font
                String fontName = "";
                Obj freeTextObj = freeText.getSDFObj();
                Obj drDict = freeTextObj.findObj("DR");
                if (drDict != null && drDict.isDict()) {
                    Obj fontDict = drDict.findObj("Font");
                    if (fontDict != null && fontDict.isDict()) {
                        DictIterator fItr = fontDict.getDictIterator();
                        if (fItr.hasNext()) {
                            Font f = new Font(fItr.value());
                            fontName = f.getName();
                        }
                    }
                }

                // if we have embedded the font, use the name to
                // get the PDFTron name
                if (!fontName.equals("")) {

                    // find a fontName and pdftronFontName match if possible
                    String pdftronFontName = "";
                    SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
                    String fontInfo = settings.getString(ANNOTATION_FREE_TEXT_FONTS, "");
                    if (!Utils.isNullOrEmpty(fontInfo)) {
                        JSONObject systemFontObject = new JSONObject(fontInfo);
                        JSONArray systemFontArray = systemFontObject.getJSONArray(ANNOTATION_FREE_TEXT_JSON_FONT);

                        for (int i = 0; i < systemFontArray.length(); i++) {
                            // check if font is selected in settings
                            JSONObject font = systemFontArray.getJSONObject(i);
                            if (font.has(ANNOTATION_FREE_TEXT_JSON_FONT_NAME)) {
                                String fontNameCompare = font.getString(ANNOTATION_FREE_TEXT_JSON_FONT_NAME);
                                if (fontName.equals(fontNameCompare)) {
                                    pdftronFontName = font.getString(ANNOTATION_FREE_TEXT_JSON_FONT_PDFTRON_NAME);
                                    break;
                                }
                            }
                        }
                    }

                    // if there was a match between the PDFTron Name and
                    // the font name
                    if (!pdftronFontName.equals("")) {
                        String fontDRName = "F0";

                        // Create a DR entry for embedding the font
                        Obj annotObj = freeText.getSDFObj();
                        drDict = annotObj.putDict("DR");
                        Obj fontDict = drDict.putDict("Font");

                        // Embed the font
                        Font font = Font.create(mPdfViewCtrl.getDoc(), pdftronFontName, contents);
                        fontDict.put(fontDRName, font.GetSDFObj());
                    } else {
                        // use default
                        // Set DA string
                        String DA = freeText.getDefaultAppearance();
                        int slashPosition = DA.indexOf("/", 0);

                        // if DR string contains '/' which it always should.
                        if (slashPosition > 0) {
                            String beforeSlash = DA.substring(0, slashPosition);
                            String afterSlash = DA.substring(slashPosition);
                            String afterFont = afterSlash.substring(afterSlash.indexOf(" "));
                            String updatedDA = beforeSlash + "/helv" + afterFont;

                            freeText.setDefaultAppearance(updatedDA);
                        }
                    }
                }
                buildAnnotBBox();
                mPdfViewCtrl.showAnnotation(mAnnot);
                mAnnot.refreshAppearance();
                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
                raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
                Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);

            } else {
                // else if the free text annotation is an empty string, delete it
                raiseAnnotationPreRemoveEvent(mAnnot, mAnnotPageNum);
                Page page = mPdfViewCtrl.getDoc().getPage(mAnnotPageNum);
                page.annotRemove(mAnnot);
                mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

                // make sure to raise remove event after mPdfViewCtrl.update and before unsetAnnot
                raiseAnnotationRemovedEvent(mAnnot, mAnnotPageNum);
                Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);
                if (sDebug) Log.d(TAG, "update free text");

                unsetAnnot();
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            dismissUpdatingFreeText();
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void dismissUpdatingFreeText() {
        if (mInlineEditText != null) {
            mInlineEditText.close(true);
        }
        if (mPdfViewCtrl == null || mAnnot == null) {
            return;
        }
        try {
            mPdfViewCtrl.showAnnotation(mAnnot);
            mAnnot.refreshAppearance();
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        // no need to save as we cannot handle it
        Utils.deleteCacheFile(mPdfViewCtrl.getContext(), mCacheFileName);
    }

    /**
     * The overload implementation of {@link TextWatcher#beforeTextChanged(CharSequence, int, int, int)}.
     */
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    /**
     * The overload implementation of {@link TextWatcher#onTextChanged(CharSequence, int, int, int)}.
     */
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mPdfViewCtrl == null || mAnnot == null) {
            return;
        }

        long currentTimeStamp = System.currentTimeMillis();
        if (currentTimeStamp - mStoredTimeStamp > 3000) {
            mStoredTimeStamp = currentTimeStamp;
            if (s != null && s.length() > 0) {
                Bundle bundle = new Bundle();
                bundle.putStringArray(KEYS, new String[]{"contents"});
                bundle.putCharSequence("contents", s);
                if (onInterceptAnnotationHandling(mAnnot, bundle)) {
                    return;
                }
                try {
                    FreeTextCacheStruct freeTextCacheStruct = new FreeTextCacheStruct();
                    freeTextCacheStruct.contents = s.toString();
                    freeTextCacheStruct.pageNum = mAnnotPageNum;
                    Rect rect = mPdfViewCtrl.getScreenRectForAnnot(mAnnot, mAnnotPageNum);
                    freeTextCacheStruct.x = (float) (Math.min(rect.getX1(), rect.getX2()));
                    freeTextCacheStruct.y = (float) (Math.min(rect.getX2(), rect.getY2()));
                    AnnotUtils.saveFreeTextCache(freeTextCacheStruct, mPdfViewCtrl);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }
    }

    /**
     * The overload implementation of {@link TextWatcher#afterTextChanged(Editable)}.
     */
    @Override
    public void afterTextChanged(Editable s) {

    }

    /**
     * The overload implementation of {@link Tool#getModeAHLabel()}.
     */
    @Override
    protected int getModeAHLabel() {
        if (mStamperToolSelected) {
            return AnalyticsHandlerAdapter.LABEL_QM_STAMPERSELECT;
        }
        return super.getModeAHLabel();
    }

    /**
     * Returns which effective control point is closest to the specified coordinate.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The effective control point which can be one of this
     * {@link #e_unknown},
     * {@link #e_moving},
     * {@link #e_ll},
     * {@link #e_lm},
     * {@link #e_lr},
     * {@link #e_mr},
     * {@link #e_ur},
     * {@link #e_um},
     * {@link #e_ul},
     * {@link #e_ml}
     */
    public int getEffectCtrlPointId(float x, float y) {
        if (mHandleEffCtrlPtsDisabled) {
            return e_unknown;
        }

        int effCtrlPtId = e_unknown;
        float thresh = mCtrlRadius * 2.25f;
        float shortest_dist = -1;
        for (int i = 0; i < RECTANGULAR_CTRL_PTS_CNT; ++i) {
            if (isAnnotResizable()) {
                // Sticky note and text markup cannot be re-sized
                float s = mCtrlPts[i].x;
                float t = mCtrlPts[i].y;

                float dist = (x - s) * (x - s) + (y - t) * (y - t);
                dist = (float) Math.sqrt(dist);
                if (dist <= thresh && (dist < shortest_dist || shortest_dist < 0)) {
                    effCtrlPtId = i;
                    shortest_dist = dist;
                }
            }
        }

        // Check if hit within the bounding box without hitting any control point.
        // Note that text markup cannot be moved.
        if (!mAnnotIsTextMarkup && effCtrlPtId == e_unknown && mBBox.contains(x, y)) {
            effCtrlPtId = e_moving;
        }

        return effCtrlPtId;
    }

    /**
     * @return True if control points are hidden
     */
    public boolean isCtrlPtsHidden() {
        return mHideCtrlPts;
    }

    @Override
    public void onChangeAnnotThickness(float thickness, boolean done) {
        if (mAnnotView != null) {
            mAnnotView.updateThickness(thickness);
        }
        if (done) {
            // change border thickness
            editThickness(thickness);
        }
    }

    @Override
    public void onChangeAnnotTextSize(float textSize, boolean done) {
        if (mAnnotView != null) {
            mAnnotView.updateTextSize(textSize);
        }
        if (done) {
            editTextSize(textSize);
        }
    }

    @Override
    public void onChangeAnnotOpacity(float opacity, boolean done) {
        if (mAnnotView != null) {
            mAnnotView.updateOpacity(opacity);
        }
        if (done) {
            editOpacity(opacity);
        }
    }

    @Override
    public void onChangeAnnotStrokeColor(int color) {
        if (mAnnot == null) {
            return;
        }

        if (mAnnotView != null) {
            mAnnotView.updateColor(color);
        }

        editColor(color);
        // check annot color and set quick menu style color
        try {
            if (mAnnot.getType() == Annot.e_Square || mAnnot.getType() == Annot.e_Circle) {
                Markup markup = new Markup(mAnnot);
                ColorPt fillColorPt = markup.getInteriorColor();
                int fillColor = Utils.colorPt2color(fillColorPt);
                if (fillColor == Color.TRANSPARENT) {
                    updateQuickMenuStyleColor(color);
                }
            } else {
                updateQuickMenuStyleColor(color);
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    @Override
    public void onChangeAnnotFillColor(int color) {
        if (mAnnotView != null) {
            mAnnotView.updateFillColor(color);
        }
        editFillColor(color);
        // if has fill need to check fill first
        if (color != Color.TRANSPARENT) {
            updateQuickMenuStyleColor(color);
        }
    }

    @Override
    public void onChangeAnnotIcon(String icon) {
        if (mAnnotView != null) {
            mAnnotView.updateIcon(icon);
        }
        editIcon(icon);
    }

    @Override
    public void onChangeAnnotFont(FontResource font) {
        if (mAnnotView != null) {
            mAnnotView.updateFont(font);
        }
        editFont(font.getPDFTronName());
    }

    @Override
    public void onChangeAnnotTextColor(int textColor) {
        if (mAnnotView != null) {
            mAnnotView.updateTextColor(textColor);
        }
        editTextColor(textColor);
    }

    @Override
    public void onChangeRulerProperty(RulerItem rulerItem) {
        if (mAnnotView != null) {
            mAnnotView.updateRulerItem(rulerItem);
        }

        editRuler(rulerItem);
    }

    /**
     * @return True if any annot is selected.
     * Note that in {@link AnnotEditRectGroup} multiple annots can be selected
     * while {@link Tool#mAnnot} is null
     */
    protected boolean hasAnnotSelected() {
        return mAnnot != null;
    }

    private void redactAnnot() {
        if (mAnnot == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "redactAnnot");
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            Redactor.Appearance app = new Redactor.Appearance();
            app.positiveOverlayColor = Utils.color2ColorPt(Color.BLACK);
            app.redactionOverlay = true;
            app.border = false;

            Rect rect = mAnnot.getRect();

            Redactor.Redaction[] vec = new Redactor.Redaction[1];
            vec[0] = new Redactor.Redaction(mAnnotPageNum, rect, false, "");

            Redactor.redact(mPdfViewCtrl.getDoc(), vec, app, false, true);
            mPdfViewCtrl.update(rect);

            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum, bundle);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        deleteAnnot();
    }

    private void playSoundAnnot() {
        if (mAnnot == null) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString(METHOD_FROM, "playSoundAnnot");
        if (onInterceptAnnotationHandling(mAnnot, bundle)) {
            return;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        FragmentActivity activity = toolManager.getCurrentActivity();
        if (activity == null) {
            return;
        }

        String audioPath = activity.getFilesDir().getAbsolutePath();
        audioPath += "/audiorecord.out";
        Integer sampleRate = null;
        int encodingBitRate = 8;
        int numChannel = 1;

        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;

            Sound sound = new Sound(mAnnot);
            Obj soundStream = sound.getSoundStream();
            if (soundStream != null) {
                Obj item = soundStream.findObj("R");
                if (item != null && item.isNumber()) {
                    sampleRate = (int) item.getNumber();
                }
                item = soundStream.findObj("B");
                if (item != null && item.isNumber()) {
                    encodingBitRate = (int) item.getNumber();
                }
                item = soundStream.findObj("C");
                if (item != null && item.isNumber()) {
                    numChannel = (int) item.getNumber();
                }

                Filter soundFilter = soundStream.getDecodedStream();
                soundFilter.writeToFile(audioPath, false);
            }
        } catch (Exception e) {
            audioPath = null;
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        mNextToolMode = ToolMode.PAN;

        if (!Utils.isNullOrEmpty(audioPath) && sampleRate != null) {
            encodingBitRate = encodingBitRate == 16 ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
            numChannel = numChannel == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
            SoundDialogFragment fragment = SoundDialogFragment.newInstance(audioPath, sampleRate, encodingBitRate, numChannel);
            fragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
            fragment.show(activity.getSupportFragmentManager(), SoundDialogFragment.TAG);
        }
    }

    /**
     * Sets whether disables handling original 8 control points.
     * This should be called if an inherited class
     * has different number of control points and needs special handling.
     * See {@link AnnotEditAdvancedShape}
     */
    protected void setOriginalCtrlPtsDisabled(boolean disabled) {
        mHandleEffCtrlPtsDisabled = disabled;
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }
}
