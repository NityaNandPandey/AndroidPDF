//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.widget.EdgeEffectCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.ActionParameter;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.ActionUtils;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.AnnotationClipboardHelper;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.AnnotView;
import com.pdftron.sdf.Obj;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The base class that implements the ToolManager.Tool interface and several
 * basic tool functionalities.
 */
@SuppressWarnings("ALL")
public abstract class Tool implements ToolManager.Tool {

    private static final String TAG = Tool.class.getName();

    protected static boolean sDebug;

    public static final String PDFTRON_ID = "pdftron";
    public static final String PDFTRON_THICKNESS = "pdftron_thickness";

    public static final String KEYS = "PDFTRON_KEYS";
    public static final String METHOD_FROM = "METHOD_FROM";
    public static final String PAGE_NUMBER = "PAGE_NUMBER";

    private static final String PREFS_FILE_NAME = "com_pdftron_pdfnet_pdfviewctrl_prefs_file";

    // user-default annotation properties
    public static final String PREF_ANNOTATION_CREATION_LINE = "annotation_creation"; // line
    public static final String PREF_ANNOTATION_CREATION_ARROW = "annotation_creation_arrow";
    public static final String PREF_ANNOTATION_CREATION_POLYLINE = "annotation_creation_polyline";
    public static final String PREF_ANNOTATION_CREATION_RECTANGLE = "annotation_creation_rectangle";
    public static final String PREF_ANNOTATION_CREATION_OVAL = "annotation_creation_oval";
    public static final String PREF_ANNOTATION_CREATION_POLYGON = "annotation_creation_polygon";
    public static final String PREF_ANNOTATION_CREATION_CLOUD = "annotation_creation_cloud";
    public static final String PREF_ANNOTATION_CREATION_HIGHLIGHT = "annotation_creation_highlight";
    public static final String PREF_ANNOTATION_CREATION_LINK = "annotation_creation_link";
    public static final String PREF_ANNOTATION_CREATION_UNDERLINE = "annotation_creation_text_markup"; // underline
    public static final String PREF_ANNOTATION_CREATION_STRIKEOUT = "annotation_creation_strikeout";
    public static final String PREF_ANNOTATION_CREATION_SQUIGGLY = "annotation_creation_squiggly";
    public static final String PREF_ANNOTATION_CREATION_FREETEXT = "annotation_creation_freetext";
    public static final String PREF_ANNOTATION_CREATION_FREEHAND = "annotation_creation_freehand";
    public static final String PREF_ANNOTATION_CREATION_NOTE = "annotation_creation_note";
    public static final String PREF_ANNOTATION_CREATION_ERASER = "annotation_creation_eraser";
    public static final String PREF_ANNOTATION_CREATION_SIGNATURE = "annotation_creation_signature";
    public static final String PREF_ANNOTATION_CREATION_FREE_HIGHLIGHTER = "annotation_creation_free_highlighter";
    public static final String PREF_ANNOTATION_CREATION_COLOR = "_color";
    public static final String PREF_ANNOTATION_CREATION_TEXT_COLOR = "_text_color";
    public static final String PREF_ANNOTATION_CREATION_FILL_COLOR = "_fill_color";
    public static final String PREF_ANNOTATION_CREATION_OPACITY = "_opacity";
    public static final String PREF_ANNOTATION_CREATION_THICKNESS = "_thickness";
    public static final String PREF_ANNOTATION_CREATION_TEXT_SIZE = "_text_size";
    public static final String PREF_ANNOTATION_CREATION_ICON = "_icon";
    public static final String PREF_ANNOTATION_CREATION_FONT = "_font";

    public static final String ANNOTATION_NOTE_ICON_FILE_PREFIX = "annotation_note_icon_";
    public static final String ANNOTATION_NOTE_ICON_FILE_POSTFIX_FILL = "_fill";
    public static final String ANNOTATION_NOTE_ICON_FILE_POSTFIX_OUTLINE = "_outline";
    public static final String ANNOTATION_TOOLBAR_SIGNATURE_STATE = "annotation_toolbar_signature_state";
    public static final String ANNOTATION_FREE_TEXT_FONTS = "annotation_property_free_text_fonts_list";
    public static final String ANNOTATION_FREE_TEXT_JSON_FONT_FILE_PATH = "filepath";
    public static final String ANNOTATION_FREE_TEXT_JSON_FONT_DISPLAY_NAME = "display name";
    public static final String ANNOTATION_FREE_TEXT_JSON_FONT_DISPLAY_IN_LIST = "display font";
    public static final String ANNOTATION_FREE_TEXT_JSON_FONT_PDFTRON_NAME = "pdftron name";
    public static final String ANNOTATION_FREE_TEXT_JSON_FONT_NAME = "font name";
    public static final String ANNOTATION_FREE_TEXT_JSON_FONT = "fonts";
    public static final String STAMP_SHOW_FLATTEN_WARNING = "stamp_show_flatten_warning";
    // list of white listed annotation free text fonts
    // the fonts on this list are from: https://www.microsoft.com/typography/fonts/popular.aspx,
    // the Base14 Fonts and other common fonts found on devices
    public static final String[] ANNOTATION_FREE_TEXT_WHITELIST_FONTS = {"Gill", "Calibri", "Arial",
        "SimSun", "Curlz", "Times", "Lucida", "Rockwell",
        "Old English", "Abadi", "Twentieth Century",
        "News Gothic", "Bodoni", "Candara", "PMingLiU",
        "Palace Script", "Helvetica", "Courier", "Roboto",
        "Comic", "Droid", "Georgia", "MotoyaLManu", "NanumGothic",
        "Kaiti", "Miaowu", "ShaoNV", "Rosemary"};
    public static final int ANNOTATION_FREE_TEXT_PREFERENCE_INLINE = 1;
    public static final int ANNOTATION_FREE_TEXT_PREFERENCE_DIALOG = 2;
    public static final String ANNOTATION_FREE_TEXT_PREFERENCE_EDITING = "annotation_free_text_preference_editing";
    public static final int ANNOTATION_FREE_TEXT_PREFERENCE_EDITING_DEFAULT = ANNOTATION_FREE_TEXT_PREFERENCE_INLINE;

    // form field appearance constants
    public static final String FORM_FIELD_SYMBOL_CHECKBOX = "4";
    public static final String FORM_FIELD_SYMBOL_CIRCLE = "l";
    public static final String FORM_FIELD_SYMBOL_CROSS = "8";
    public static final String FORM_FIELD_SYMBOL_DIAMOND = "u";
    public static final String FORM_FIELD_SYMBOL_SQUARE = "n";
    public static final String FORM_FIELD_SYMBOL_STAR = "H";


    // Translation languages properties
    public static final String PREF_TRANSLATION_SOURCE_LANGUAGE_CODE_KEY = "translation_source_language_code";
    public static final String PREF_TRANSLATION_TARGET_LANGUAGE_CODE_KEY = "translation_target_language_code";
    public static final String LAST_DEVICE_LOCALE_LANGUAGE = "last_device_locale_language";
    public static final String PREF_TRANSLATION_SOURCE_LANGUAGE_CODE_DEFAULT = "en";    // english
    public static final String PREF_TRANSLATION_TARGET_LANGUAGE_CODE_DEFAULT = "fr";    // french
    public static final int ANNOT_PERMISSION_SELECTION = 0;
    public static final int ANNOT_PERMISSION_MENU = 1;

    protected final float mTextSize;
    protected final float mTextVOffset;
    protected PDFViewCtrl mPdfViewCtrl;
    protected ToolManager.ToolModeBase mNextToolMode;
    protected ToolManager.ToolModeBase mCurrentDefaultToolMode; // the default tool in continuous annotating mode, used for allowing editing
    protected Annot mAnnot;
    protected int mAnnotPageNum;
    protected int mSelectPageNum;
    protected RectF mAnnotBBox; // In page space
    protected QuickMenu mQuickMenu;
    protected String mMruMenuItems[];
    protected String mOverflowMenuItems[];
    protected Paint mPaint4PageNum;
    protected boolean mJustSwitchedFromAnotherTool;
    protected boolean mAvoidLongPressAttempt;
    protected float mPageNumPosAdjust;
    protected RectF mTempPageDrawingRectF;
    protected boolean mForceSameNextToolMode;
    protected boolean mAnnotPushedBack;
    protected boolean mAllowTwoFingerScroll;
    protected boolean mAllowOneFingerScrollWithStylus;

    protected boolean mUpFromCalloutCreate;

    // Stylus support
    protected boolean mIsStylus;
    protected boolean mStylusUsed;

    protected boolean mAllowZoom;
    protected boolean mHasMenuPermission = true;
    protected boolean mHasSelectionPermission = true;
    private Matrix mTempMtx1;
    private Matrix mTempMtx2;
    // edge effect
    private EdgeEffectCompat mEdgeEffectLeft;
    private EdgeEffectCompat mEdgeEffectRight;
    private Markup mMarkupToAuthor;
    private boolean mPageNumberIndicatorVisible;

    protected AnnotView mAnnotView;

    /*
     * Used to remove the shown page number
     */
    private PageNumberRemovalHandler mPageNumberRemovalHandler = new PageNumberRemovalHandler(this);

    /**
     * Class constructor
     */
    public Tool(@NonNull PDFViewCtrl ctrl) {
        mPdfViewCtrl = ctrl;
        mNextToolMode = ToolMode.PAN;
        mCurrentDefaultToolMode = ToolMode.PAN;
        mAnnot = null;
        mAnnotBBox = new RectF();
        mJustSwitchedFromAnotherTool = false;
        mForceSameNextToolMode = false;
        mAvoidLongPressAttempt = false;
        mAnnotPushedBack = false;
        mPageNumPosAdjust = 0;
        mTempPageDrawingRectF = new RectF();

        mTextSize = convDp2Pix(15);
        mTextVOffset = convDp2Pix(50);
        mPaint4PageNum = new Paint();
        mPaint4PageNum.setAntiAlias(true);
        mPaint4PageNum.setTextSize(mTextSize);
        mPaint4PageNum.setStyle(Paint.Style.FILL);

        mTempMtx1 = new Matrix();
        mTempMtx2 = new Matrix();

        mPageNumberIndicatorVisible = true;

        mAllowTwoFingerScroll = false;
        mAllowOneFingerScrollWithStylus = false;
        mAllowZoom = true;

        // Disable page turning (in non-continuous page presentation mode);
        // it is only turned on in Pan tool.
        mPdfViewCtrl.setBuiltInPageSlidingEnabled(false);

        // Sets up edge effects
        mEdgeEffectLeft = new EdgeEffectCompat(ctrl.getContext());
        mEdgeEffectRight = new EdgeEffectCompat(ctrl.getContext());

        // find quick menu
        int childCount = mPdfViewCtrl.getChildCount();

        for (int i = 0; i < mPdfViewCtrl.getChildCount(); i++) {
            if (mPdfViewCtrl.getChildAt(i) instanceof QuickMenu) {
                mQuickMenu = (QuickMenu) mPdfViewCtrl.getChildAt(i);
                break;
            }
        }
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#getToolMode()}.
     */
    @Override
    public abstract ToolManager.ToolModeBase getToolMode();

    public abstract int getCreateAnnotType();

    /**
     * The overload implementation of {@link ToolManager.Tool#getNextToolMode()}.
     */
    @Override
    final public ToolManager.ToolModeBase getNextToolMode() {
        return mNextToolMode;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return false;
    }

    /**
     * Whether the purpose of this tool is editing annotation
     *
     * @return false
     */
    public boolean isEditAnnotTool() {
        return false;
    }

    /**
     * Whether the current quick menu includes a certain menu type
     *
     * @param menuId the menu item id
     */
    public boolean hasMenuEntry(@IdRes int menuId) {
        if (!isQuickMenuShown()) {
            return false;
        }
        if (mQuickMenu.getMenu().findItem(menuId) != null) {
            return true;
        }
        return false;
    }

    /**
     * paste annotation
     *
     * @param targetPoint that target position the annotation is going to paste to
     */
    void pasteAnnot(PointF targetPoint) {
        if (mPdfViewCtrl == null || !AnnotationClipboardHelper.isItemCopied(mPdfViewCtrl.getContext())) {
            return;
        }

        int pageNumber = mPdfViewCtrl.getPageNumberFromScreenPt(targetPoint.x, targetPoint.y);
        if (pageNumber == -1) {
            pageNumber = mPdfViewCtrl.getCurrentPage();
        }

        if (Utils.isImageCopied(mPdfViewCtrl.getContext())) {
            try {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                ToolManager.Tool tool = toolManager.createTool(ToolMode.STAMPER, this);
                toolManager.setTool(tool);
                ((Stamper) tool).addStampFromClipboard(targetPoint);
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        } else if (AnnotationClipboardHelper.isAnnotCopied()) {
            AnnotationClipboardHelper.pasteAnnot(mPdfViewCtrl.getContext(), mPdfViewCtrl, pageNumber, targetPoint, null);
        }
    }

    /**
     * get acutal next tool mode safely
     *
     * @param toolMode next tool mode
     * @return next tool mode
     */
    protected ToolManager.ToolModeBase safeSetNextToolMode(ToolManager.ToolModeBase toolMode) {
        if (null != toolMode && toolMode instanceof ToolMode) {
            boolean disabled = ((ToolManager) mPdfViewCtrl.getToolManager()).isToolModeDisabled((ToolMode) toolMode);
            if (disabled) {
                return ToolMode.PAN;
            }
        }
        return toolMode;
    }

    /**
     * creates a quick menu. It is used for {@link #showMenu(RectF)}
     *
     * @return quick menu
     */
    protected QuickMenu createQuickMenu() {
        QuickMenu quickMenu = new QuickMenu(mPdfViewCtrl, mHasMenuPermission, getToolMode());
        return quickMenu;
    }

    /**
     * Customize quick menu item inside quick menu
     *
     * @param menuBuilder quick menu builder, can be obtained by calling {@link QuickMenu#getMenu()}
     */
    protected void customizeQuickMenuItems(QuickMenu quickMenu) {
        if (null == mAnnot) {
            return;
        }
        try {
            // appearance
            QuickMenuItem appearanceMenuItem = quickMenu.findMenuItem(R.id.qm_appearance);
            if (appearanceMenuItem != null) {
                int color;
                float opacity = 1.0f;
                ColorPt colorPt = mAnnot.getColorAsRGB();
                color = Utils.colorPt2color(colorPt);
                if (mAnnot.getType() == Annot.e_FreeText) {
                    FreeText freeText = new FreeText(mAnnot);
                    if (freeText.getTextColorCompNum() == 3) {
                        ColorPt fillColorPt = freeText.getTextColor();
                        color = Utils.colorPt2color(fillColorPt);
                    }
                }
                if (mAnnot.isMarkup()) {
                    // if has fill color, use fill color
                    Markup m = new Markup(mAnnot);
                    if (m.getInteriorColorCompNum() == 3) {
                        ColorPt fillColorPt = m.getInteriorColor();
                        int fillColor = Utils.colorPt2color(fillColorPt);
                        if (fillColor != Color.TRANSPARENT) {
                            color = fillColor;
                        }
                    }
                    // opacity
                    opacity = (float) m.getOpacity();
                }
                int background = Utils.getBackgroundColor(mPdfViewCtrl.getContext());
                int foreground = ColorUtils.compositeColors(Color.argb((int) (opacity * 255), Color.red(color), Color.green(color), Color.blue(color)), background);

                boolean isColorSpaceClose = Utils.isTwoColorSimilar(background, foreground, 12);

                if (!isColorSpaceClose) {
                    appearanceMenuItem.setColor(color);
                    appearanceMenuItem.setOpacity(opacity);
                }
            }
            // note
            QuickMenuItem noteItem = quickMenu.findMenuItem(R.id.qm_note);
            if (noteItem != null
                && mAnnot.isMarkup()
                && mAnnot.getType() != Annot.e_FreeText
                && mAnnot.getType() != Annot.e_Stamp) {
                String contents = mAnnot.getContents();
                if (Utils.isNullOrEmpty(contents)) {
                    noteItem.setIcon(R.drawable.ic_annotation_sticky_note_black_24dp);
                    noteItem.setTitle(R.string.tools_qm_add_note);
                } else {
                    noteItem.setIcon(R.drawable.ic_chat_black_24dp);
                    noteItem.setTitle(R.string.tools_qm_view_note);
                }
            }
            // type item
            QuickMenuItem typeItem = quickMenu.findMenuItem(R.id.qm_type);
            if (typeItem != null && typeItem.hasSubMenu()) {
                QuickMenuBuilder subMenu = (QuickMenuBuilder) typeItem.getSubMenu();
                if (mAnnot != null) {
                    if (mAnnot.getType() == Annot.e_Highlight) {
                        subMenu.removeItem(R.id.qm_highlight);
                    } else if (mAnnot.getType() == Annot.e_StrikeOut) {
                        subMenu.removeItem(R.id.qm_strikeout);
                    } else if (mAnnot.getType() == Annot.e_Underline) {
                        subMenu.removeItem(R.id.qm_underline);
                    } else if (mAnnot.getType() == Annot.e_Squiggly) {
                        subMenu.removeItem(R.id.qm_squiggly);
                    }
                }
            }
            // ink
            QuickMenuItem inkEditItem = quickMenu.findMenuItem(R.id.qm_edit);
            if (inkEditItem != null) {
                if (((ToolManager) mPdfViewCtrl.getToolManager()).editInkAnnots() && mAnnot.getType() == Annot.e_Ink) {
                    inkEditItem.setVisible(true);
                } else {
                    inkEditItem.setVisible(false);
                }
            }
        } catch (PDFNetException e) {
            e.printStackTrace();
            AnalyticsHandlerAdapter.getInstance().sendException(e, "failed in AnnotEdit.createQuickMenu");
        }
    }

    /**
     * Checks whether the annotation has the specified permission
     *
     * @param annot The annotation
     * @param kind  The kind of permission. Possible values are
     *              {@link ANNOT_PERMISSION_SELECTION},
     *              {@link ANNOT_PERMISSION_MENU}
     * @return True if the annotation has permission
     */
    protected boolean hasPermission(Annot annot, int kind) {
        boolean hasPermission = true;
        if (mPdfViewCtrl.getToolManager() instanceof ToolManager) {
            boolean shouldUnlockRead = false;
            if (((ToolManager) mPdfViewCtrl.getToolManager()).getAnnotManager() != null) {
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    String currentAuthor = ((ToolManager) mPdfViewCtrl.getToolManager()).getAuthorId();
                    Markup mp = new Markup(annot);
                    if (mp.isValid()) {
                        String userId = mp.getTitle() != null ? mp.getTitle() : null;
                        if (null != userId && null != currentAuthor) {
                            hasPermission = currentAuthor.equals(userId);
                        }
                    }
                } catch (Exception e) {
                    hasPermission = true;
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
            } else if (((ToolManager) mPdfViewCtrl.getToolManager()).isAnnotPermissionCheckEnabled()) {
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    if (kind == ANNOT_PERMISSION_SELECTION) {
                        if (annot.getFlag(Annot.e_read_only) || annot.getFlag(Annot.e_locked)) {
                            hasPermission = false;
                        }
                    } else if (kind == ANNOT_PERMISSION_MENU) {
                        if (annot.getFlag(Annot.e_read_only)) {
                            hasPermission = false;
                        }
                    }
                } catch (Exception e) {
                    hasPermission = true;
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
            }
        }
        return hasPermission;
    }

    /**
     * @return analytics handler label mode
     */
    protected int getModeAHLabel() {
        return AnalyticsHandlerAdapter.LABEL_QM_ANNOTSELECT;
    }

    /**
     * Executes an action with the specified parameters.
     * <p>
     * <div class="warning">
     * Note that the PDF doc should have been locked when call this method.
     * In addition, ToolManager's raise annotation should be handled in the caller function.
     * </div>
     *
     * @param actionParam The action parameters
     */
    public void executeAction(ActionParameter actionParam) {
        ActionUtils.getInstance().executeAction(actionParam, mPdfViewCtrl);
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDocumentDownloadEvent(PDFViewCtrl.DownloadState, int, int, int, String)}.
     */
    @Override
    public void onDocumentDownloadEvent(PDFViewCtrl.DownloadState state, int page_num, int page_downloaded, int page_count, String message) {
    }

    /**
     * Handles generic motion event.
     *
     * @param event The motion event
     * @return True if handled
     */
    boolean onGenericMotionEvent(MotionEvent event) {
        if (mPdfViewCtrl == null) {
            return false;
        }

        if (ShortcutHelper.isZoomIn(event)) {
            mPdfViewCtrl.setZoom((int) event.getX(), (int) event.getY(),
                mPdfViewCtrl.getZoom() * PDFViewCtrl.SCROLL_ZOOM_FACTOR, true, true);
            return true;
        } else if (ShortcutHelper.isZoomOut(event)) {
            mPdfViewCtrl.setZoom((int) event.getX(), (int) event.getY(),
                mPdfViewCtrl.getZoom() / PDFViewCtrl.SCROLL_ZOOM_FACTOR, true, true);
            return true;
        } else if (ShortcutHelper.isScroll(event)) {
            int dx = (int) (event.getAxisValue(MotionEvent.AXIS_HSCROLL) * 100);
            int dy = (int) (event.getAxisValue(MotionEvent.AXIS_VSCROLL) * 100);
            if (dx != 0 || dy != 0) {
                mPdfViewCtrl.scrollBy(dx, -dy);
            }
            return true;
        }

        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onKeyUp(int, KeyEvent)}.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mPdfViewCtrl == null) {
            return false;
        }

        if (ShortcutHelper.isPaste(keyCode, event)) {
            pasteAnnot(mPdfViewCtrl.getCurrentMousePosition());
            return true;
        }

        if (isQuickMenuShown() && ShortcutHelper.isCloseMenu(keyCode, event)) {
            closeQuickMenu();
            unsetAnnot();
            mNextToolMode = mCurrentDefaultToolMode;
            mPdfViewCtrl.invalidate();
            return true;
        }

        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        mAllowZoom = !(!mPdfViewCtrl.isZoomingInAddingAnnotationEnabled() && isCreatingAnnotation());
        mPdfViewCtrl.setZoomEnabled(mAllowZoom);
        closeQuickMenu();

        if (isCreatingAnnotation()) {
            // stylus
            if (mIsStylus && e.getPointerCount() == 1 && e.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
                mIsStylus = false;
            } else if (!mIsStylus && e.getPointerCount() == 1 && e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                mIsStylus = true;
            }

            if (!mStylusUsed) {
                mStylusUsed = mIsStylus;
            }
        }
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onPointerDown(MotionEvent)}.
     */
    @Override
    public boolean onPointerDown(MotionEvent e) {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        mPageNumberRemovalHandler.sendEmptyMessageDelayed(1, 3000);
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        if (ShortcutHelper.isZoomInOut(e2)) {
            if (e1.getY() < e2.getY()) {
                mPdfViewCtrl.setZoom((int) e2.getX(), (int) e2.getY(),
                    mPdfViewCtrl.getZoom() * PDFViewCtrl.SCROLL_ZOOM_FACTOR, true, true);
                return true;
            } else if (e1.getY() > e2.getY()) {
                mPdfViewCtrl.setZoom((int) e2.getX(), (int) e2.getY(),
                    mPdfViewCtrl.getZoom() / PDFViewCtrl.SCROLL_ZOOM_FACTOR, true, true);
                return true;
            }
        }

        if (isCreatingAnnotation()) {
            if (e1.getPointerCount() == 2 || e2.getPointerCount() == 2) {
                mAllowTwoFingerScroll = true;
            }

            // check to see whether use finger to scroll or not
            mAllowOneFingerScrollWithStylus = mStylusUsed && e2.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS;

        } else {
            mAllowTwoFingerScroll = false;
        }

        // Enable page turning (in non-continuous page presentation mode);
        // it is always enabled for pan mode and text-highlighter
        // it is enabled only if scrolled with two fingers in other modes
        if (getToolMode() == ToolMode.PAN ||
            getToolMode() == ToolMode.TEXT_SELECT ||
            getToolMode() == ToolMode.TEXT_HIGHLIGHTER) {
            mPdfViewCtrl.setBuiltInPageSlidingEnabled(true);
        } else {
            if (mAllowTwoFingerScroll || mAllowOneFingerScrollWithStylus) {
                mPdfViewCtrl.setBuiltInPageSlidingEnabled(true);
            } else {
                mPdfViewCtrl.setBuiltInPageSlidingEnabled(false);
            }
        }

        showTransientPageNumber();
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onScrollChanged(int, int, int, int)}.
     */
    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onPageTurning(int, int)}.
     */
    @Override
    public void onPageTurning(int old_page, int cur_page) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onPostSingleTapConfirmed()}.
     */
    @Override
    public void onPostSingleTapConfirmed() {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onSingleTapUp(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDoubleTap(MotionEvent)}.
     */
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        showTransientPageNumber();

        // The following code shows how to override the double tap behavior of PDFViewCtrl.
        boolean customize = true;
        if (!customize) {
            // Let PDFViewCtrl handle how double tap zooms
            return false;

        } else {
            if (isCreatingAnnotation()) {
                // Disable double tap in annotation creation mode
                if (mStylusUsed && mIsStylus) {
                    return true;
                }
            }
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (!toolManager.isDoubleTapToZoom()) {
                // disable double tap to zoom
                return true;
            }
            boolean animate = true;
            // I want to customize how double tap zooms
            int x = (int) (e.getX() + 0.5);
            int y = (int) (e.getY() + 0.5);
            final PDFViewCtrl.PageViewMode refMode;
            if (mPdfViewCtrl.isMaintainZoomEnabled()) {
                refMode = mPdfViewCtrl.getPreferredViewMode();
            } else {
                refMode = mPdfViewCtrl.getPageRefViewMode();
            }
            if (!ViewerUtils.isViewerZoomed(mPdfViewCtrl)) {
                // Let's try smart zoom first
                boolean result = mPdfViewCtrl.smartZoom(x, y, animate);
                if (!result) {
                    // If not, just zoom in
                    boolean use_snapshot = true;
                    mPdfViewCtrl.setZoom(x, y, mPdfViewCtrl.getZoom() * 2.5, use_snapshot, animate);
                }
            } else {
                mPdfViewCtrl.setPageViewMode(refMode, x, y, animate);
            }
            return true;    // This tells PDFViewCtrl to skip its internal logic
        }
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDoubleTapEnd(MotionEvent)}.
     */
    @Override
    public void onDoubleTapEnd(MotionEvent e) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDoubleTapEvent(MotionEvent)}.
     */
    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onLongPress(MotionEvent)}.
     */
    @Override
    public boolean onLongPress(MotionEvent e) {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        //(x, y) is the scaling focal point in client space
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onScale(float, float)}.
     */
    @Override
    public boolean onScale(float x, float y) {
        //(x, y) is the scaling focal point in client space
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onScaleEnd(float, float)}.
     */
    @Override
    public boolean onScaleEnd(float x, float y) {
        //(x, y) is the scaling focal point in client space
        showTransientPageNumber();
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onShowPress(MotionEvent)}.
     */
    @Override
    public boolean onShowPress(MotionEvent e) {
        return false;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onClose()}.
     */
    @Override
    public void onClose() {
        mPageNumberRemovalHandler.removeCallbacksAndMessages(null);
        if (mPdfViewCtrl.hasSelection()) {
            mPdfViewCtrl.clearSelection();
        }
        closeQuickMenu();
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onCustomEvent(Object)}.
     */
    @Override
    public void onCustomEvent(Object o) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onSetDoc()}.
     */
    @Override
    public void onSetDoc() {
        boolean shouldUnlock = false;
        boolean hasExecutionChanges = false;
        try {
            Action open_action = mPdfViewCtrl.getDoc().getOpenAction();
            if (open_action.isValid() && (open_action.getType() == Action.e_JavaScript)) {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                ActionParameter action_param = new ActionParameter(open_action);
                executeAction(action_param);
                hasExecutionChanges = mPdfViewCtrl.getDoc().hasChangesSinceSnapshot();
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
                if (hasExecutionChanges) {
                    raiseAnnotationActionEvent();
                }
            }
        }
    }

    /**
     * Called after the tool is created by ToolManager.
     */
    public void onCreate() {
        if (mPageNumberIndicatorVisible) {
            showTransientPageNumber();
        }
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {

    }

    /**
     * Lets the tool know that it is just switched from annotation tool.
     */
    protected void setJustCreatedFromAnotherTool() {
        mJustSwitchedFromAnotherTool = true;
    }


    /**
     * Clears the target point.
     */
    public void clearTargetPoint() {
        if (getToolMode() == ToolMode.STAMPER) {
            try {
                ((Stamper) this).clearTargetPoint();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        } else if (getToolMode() == ToolMode.FILE_ATTACHMENT_CREATE) {
            try {
                ((FileAttachmentCreate) this).clearTargetPoint();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * Closes the quick menu
     */
    public void closeQuickMenu() {
        if (mQuickMenu != null && mQuickMenu.isShowing()) {
            mQuickMenu.dismiss();
        }
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDrawEdgeEffects(Canvas, int, int)}.
     */
    @Override
    public boolean onDrawEdgeEffects(Canvas canvas, int width, int verticalOffset) {
        boolean needsInvalidate = false;

        if (!mEdgeEffectLeft.isFinished()) {
            canvas.save();
            try {
                canvas.translate(0, canvas.getHeight() + verticalOffset);
                canvas.rotate(-90, 0, 0);
                mEdgeEffectLeft.setSize(canvas.getHeight(), canvas.getWidth());
                if (mEdgeEffectLeft.draw(canvas)) {
                    needsInvalidate = true;
                }
            } finally {
                canvas.restore();
            }
        }

        if (!mEdgeEffectRight.isFinished()) {
            canvas.save();
            try {
                canvas.translate(width, verticalOffset);
                canvas.rotate(90, 0, 0);
                mEdgeEffectRight.setSize(canvas.getHeight(), canvas.getWidth());
                if (mEdgeEffectRight.draw(canvas)) {
                    needsInvalidate = true;
                }
            } finally {
                canvas.restore();
            }
        }
        return needsInvalidate;
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onReleaseEdgeEffects()}.
     */
    @Override
    public void onReleaseEdgeEffects() {
        mEdgeEffectLeft.onRelease();
        mEdgeEffectRight.onRelease();
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onPullEdgeEffects(int, float)}.
     */
    @Override
    public void onPullEdgeEffects(int whichEdge, float deltaDistance) {
        if (whichEdge < 0) {
            // left
            mEdgeEffectLeft.onPull(deltaDistance);
        } else if (whichEdge > 0) {
            // right
            mEdgeEffectRight.onPull(deltaDistance);
        }
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDoubleTapZoomAnimationBegin()}.
     */
    @Override
    public void onDoubleTapZoomAnimationBegin() {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onDoubleTapZoomAnimationEnd()}.
     */
    @Override
    public void onDoubleTapZoomAnimationEnd() {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onNightModeUpdated(boolean)}.
     */
    @Override
    public void onNightModeUpdated(boolean isNightMode) {
    }

    /**
     * The overload implementation of {@link ToolManager.Tool#onRenderingFinished()}.
     */
    @Override
    public void onRenderingFinished() {
        if (mAnnotView != null && mAnnotView.isDelayViewRemoval()) {
            mPdfViewCtrl.removeView(mAnnotView);
            mAnnotView = null;
        }
    }

    /**
     * Called when a menu in quick menu has been clicked.
     *
     * @param menuItem The clicked menu item.
     * @return True if handled
     */
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        mNextToolMode = getToolMode();
        return false;
    }

    /**
     * Checks whether the specified point is inside the quick menu layout.
     *
     * @param x X coordinates
     * @param y Y coordinates
     * @return True if it is inside quick menu, false otherwise
     */
    public boolean isInsideQuickMenu(float x, float y) {
        return isQuickMenuShown() && x > mQuickMenu.getLeft() && x < mQuickMenu.getRight() && y < mQuickMenu.getBottom() && y > mQuickMenu.getTop();
    }

    /**
     * check if quick menu is showing
     *
     * @return true if quick menu is visible
     */
    public boolean isQuickMenuShown() {
        return mQuickMenu != null && mQuickMenu.isShowing();
    }

    /**
     * Update quick menu note text based on if the note has contents
     *
     * @param note the content of note
     */
    public void updateQuickMenuNoteText(String note) {
        if (!isQuickMenuShown()) {
            return;
        }
        QuickMenuItem menuItem = (QuickMenuItem) mQuickMenu.getMenu().findItem(R.id.qm_note);
        if (menuItem != null) {
            // update note text
            if (note != null && !note.equals("")) {
                menuItem.setTitle(R.string.tools_qm_view_note);
            } else {
                menuItem.setTitle(R.string.tools_qm_add_note);
            }
        }
    }

    /**
     * Update quick menu appearance item color
     *
     * @param color
     */
    public void updateQuickMenuStyleColor(int color) {
        if (color == Color.TRANSPARENT || mQuickMenu == null) {
            // ignore transparent color
            return;
        }
        QuickMenuItem menuItem = (QuickMenuItem) mQuickMenu.getMenu().findItem(R.id.qm_appearance);
        if (menuItem != null) {
            menuItem.setColor(color);
        }
    }

    /**
     * @param opacity
     */
    public void updateQuickMenuStyleOpacity(float opacity) {
        if (mQuickMenu == null) {
            // ignore transparent color
            return;
        }
        QuickMenuItem menuItem = (QuickMenuItem) mQuickMenu.getMenu().findItem(R.id.qm_appearance);
        if (menuItem != null) {
            menuItem.setOpacity(opacity);
        }
    }

    /**
     * Setup annotation properties.
     *
     * @param color           The color
     * @param opacity         The opacity
     * @param thickness       The thickness
     * @param fillColor       The color for filling
     * @param icon            The icon
     * @param pdfTronFontName The PDFTron font name
     */
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName) {

    }

    /**
     * Setup annotation properties.
     *
     * @param color           The color
     * @param opacity         The opacity
     * @param thickness       The thickness
     * @param fillColor       The color for filling
     * @param icon            The icon
     * @param pdfTronFontName The PDFTron font name
     * @param textColor       The text color
     * @param textSize        The text size
     */
    public void setupAnnotProperty(int color, float opacity, float thickness, int fillColor, String icon, String pdfTronFontName, @ColorInt int textColor, float textSize) {
        setupAnnotProperty(color, opacity, thickness, fillColor, icon, pdfTronFontName);
    }

    /**
     * Setup annotation properties.
     *
     * @param style The annot style
     */
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        int color = annotStyle.getColor();
        int fill = annotStyle.getFillColor();
        float thickness = annotStyle.getThickness();
        float opacity = annotStyle.getOpacity();
        String icon = annotStyle.getIcon();
        String pdftronFontName = annotStyle.getPDFTronFontName();
        float textSize = annotStyle.getTextSize();
        int textColor = annotStyle.getTextColor();

        setupAnnotProperty(color, opacity, thickness, fill, icon, pdftronFontName, textColor, textSize);
    }

    /**
     * Specifies the mode that should forcefully remains.
     *
     * @param mode
     */
    public void setForceSameNextToolMode(boolean mode) {
        mForceSameNextToolMode = mode;
    }

    /**
     * Gets whether the mode that should forcefully remains.
     *
     * @return
     */
    public boolean isForceSameNextToolMode() {
        return mForceSameNextToolMode;
    }

    /**
     * @return True if the annotation is editing
     */
    public boolean isEditingAnnot() {
        try {
            if (getToolMode() == ToolMode.TEXT_CREATE || getToolMode() == ToolMode.CALLOUT_CREATE) {
                return ((FreeTextCreate) this).isFreeTextEditing();
            } else if (getToolMode() == ToolMode.ANNOT_EDIT) {
                return ((AnnotEdit) this).isFreeTextEditing();
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        return false;
    }

    /**
     * Sets the visiblity of page number indicator.
     *
     * @param visible True if visibla
     */
    public void setPageNumberIndicatorVisible(boolean visible) {
        mPageNumberIndicatorVisible = visible;
    }

    /**
     * Uses a helper to set the next tool mode.
     *
     * @param nextToolMode The next tool mode
     */
    public void setNextToolModeHelper(ToolMode nextToolMode) {
        if (mForceSameNextToolMode) {
            mNextToolMode = getToolMode();
        } else {
            mNextToolMode = nextToolMode;
        }
    }

    /**
     * Uses a helper to set current default tool mode.
     *
     * @param defaultToolMode The current default tool mode
     */
    protected void setCurrentDefaultToolModeHelper(ToolManager.ToolModeBase defaultToolMode) {
        if (mForceSameNextToolMode) {
            mCurrentDefaultToolMode = defaultToolMode;
        } else {
            mCurrentDefaultToolMode = ToolMode.PAN;
        }
    }

    /**
     * @return The current default tool mode
     */
    public ToolManager.ToolModeBase getCurrentDefaultToolMode() {
        return mCurrentDefaultToolMode;
    }

    /**
     * Checks whether should skip on up based on the prior event
     *
     * @param priorEventMode The prior event
     * @return
     */
    protected boolean skipOnUpPriorEvent(PDFViewCtrl.PriorEventMode priorEventMode) {
        return priorEventMode == PDFViewCtrl.PriorEventMode.FLING
            || priorEventMode == PDFViewCtrl.PriorEventMode.SCROLLING;
    }

    /**
     * Shows transient page number.
     */
    protected void showTransientPageNumber() {
        ((ToolManager) mPdfViewCtrl.getToolManager()).showBuiltInPageNumber();
        mPageNumberRemovalHandler.removeMessages(1);
        mPageNumberRemovalHandler.sendEmptyMessageDelayed(1, 3000);
    }

    /**
     * Builds the bounding box of the annotation.
     */
    protected void buildAnnotBBox() {
        if (mAnnot != null) {
            mAnnotBBox.set(0, 0, 0, 0);
            try {
                com.pdftron.pdf.Rect r = mAnnot.getVisibleContentBox();
                mAnnotBBox.set((float) r.getX1(), (float) r.getY1(), (float) r.getX2(), (float) r.getY2());
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * Checks if given screen point is inside selected annotaiton
     *
     * @param e        The motion point
     * @param screen_y y coordinates in screen pt
     * @return true if inside, false otherwise
     */
    protected boolean isInsideAnnot(MotionEvent e) {
        double x = e.getX();
        double y = e.getY();
        if (mAnnot != null && mAnnotPageNum == mPdfViewCtrl.getPageNumberFromScreenPt(x, y)) {
            double[] pts = mPdfViewCtrl.convScreenPtToPagePt(x, y, mAnnotPageNum);
            if (mAnnotBBox.contains((float) pts[0], (float) pts[1])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets annotation rectangle in screen pt
     *
     * @return annnotation rectangle in screen pt.
     */
    protected RectF getAnnotRect() {
        if (mAnnot != null && mAnnotPageNum > 0) {
            double[] pts1 = mPdfViewCtrl.convPagePtToScreenPt(mAnnotBBox.left, mAnnotBBox.bottom, mAnnotPageNum);
            double[] pts2 = mPdfViewCtrl.convPagePtToScreenPt(mAnnotBBox.right, mAnnotBBox.top, mAnnotPageNum);
            float left = (float) (pts1[0] < pts2[0] ? pts1[0] : pts2[0]);
            float right = (float) (pts1[0] > pts2[0] ? pts1[0] : pts2[0]);
            float top = (float) (pts1[1] < pts2[1] ? pts1[1] : pts2[1]);
            float bottom = (float) (pts1[1] > pts2[1] ? pts1[1] : pts2[1]);
            return new RectF(left, top, right, bottom);
        } else {
            return null;
        }
    }

    /**
     * Gets annotation rectangle in canvas pt.
     *
     * @return annotation rectangle in canvas pt.
     */
    protected RectF getAnnotCanvasRect() {
        if (mAnnot != null) {
            double[] pts1 = mPdfViewCtrl.convPagePtToCanvasPt(mAnnotBBox.left, mAnnotBBox.bottom, mAnnotPageNum);
            double[] pts2 = mPdfViewCtrl.convPagePtToCanvasPt(mAnnotBBox.right, mAnnotBBox.top, mAnnotPageNum);
            float left = (float) (pts1[0] < pts2[0] ? pts1[0] : pts2[0]);
            float right = (float) (pts1[0] > pts2[0] ? pts1[0] : pts2[0]);
            float top = (float) (pts1[1] < pts2[1] ? pts1[1] : pts2[1]);
            float bottom = (float) (pts1[1] > pts2[1] ? pts1[1] : pts2[1]);
            return new RectF(left, top, right, bottom);
        } else {
            return null;
        }
    }


    /**
     * Called when doen with two finger scrolling.
     */
    protected void doneTwoFingerScrolling() {
        mAllowTwoFingerScroll = false;
    }

    /**
     * Called when done with one finger scrolling with stylus.
     */
    protected void doneOneFingerScrollingWithStylus() {
        mAllowOneFingerScrollWithStylus = false;
    }

    /**
     * Shows the quick menu.
     */
    public boolean showMenu(RectF anchor_rect) {
        return showMenu(anchor_rect, createQuickMenu());
    }

    /**
     * Gets quick menu type for analytics
     *
     * @return quick menu type
     */
    protected @AnalyticsHandlerAdapter.QuickMenuType
    int getQuickMenuAnalyticType() {
        return AnalyticsHandlerAdapter.QUICK_MENU_TYPE_ANNOTATION_SELECT;
    }

    /**
     * Shows the quick menu with given menu.
     */
    public boolean showMenu(RectF anchor_rect, QuickMenu quickMenu) {
        if (anchor_rect == null) {
            return false;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

        if (mQuickMenu != null) {
            if (mQuickMenu.isShowing() && mAnnot != null && mQuickMenu.getAnnot() != null) {
                if (mAnnot.equals(mQuickMenu.getAnnot())) {
                    return false;
                }
            }
            closeQuickMenu();
            mQuickMenu = null;
        }

        RectF client_r = new RectF(0, 0, mPdfViewCtrl.getWidth(), mPdfViewCtrl.getHeight());
        if (!client_r.intersect(anchor_rect)) {
            return false;
        }

        toolManager.setQuickMenuJustClosed(false);

        mQuickMenu = quickMenu;

        mQuickMenu.setAnchorRect(anchor_rect);
        mQuickMenu.setAnnot(mAnnot);

        mQuickMenu.setOnDismissListener(new QuickMenuDismissListener());

        mQuickMenu.show(getQuickMenuAnalyticType());

        return true;
    }


    /**
     * Calculates quick menu anchor
     *
     * @param anchorRect The anchor rectangle
     * @return The rectangle
     */
    protected RectF calculateQMAnchor(RectF anchorRect) {
        if (anchorRect != null) {
            int left = (int) anchorRect.left;
            int top = (int) anchorRect.top;
            int right = (int) anchorRect.right;
            int bottom = (int) anchorRect.bottom;

            try {
                // normalize the rect
                com.pdftron.pdf.Rect rect = new com.pdftron.pdf.Rect((double) anchorRect.left, (double) anchorRect.top, (double) anchorRect.right, (double) anchorRect.bottom);
                rect.normalize();
                left = (int) rect.getX1();
                top = (int) rect.getY1();
                right = (int) rect.getX2();
                bottom = (int) rect.getY2();
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }

            int[] location = new int[2];
            mPdfViewCtrl.getLocationInWindow(location);

            int atop = top + location[1];
            int aleft = left + location[0];
            int aright = right + location[0];
            int abottom = bottom + location[1];

            RectF qmAnchor = new RectF(aleft, atop, aright, abottom);
            return qmAnchor;
        }
        return null;
    }

    /**
     * Selects the specified annotation.
     *
     * @param annot   The annotaion
     * @param pageNum The page number where the annotaion is on
     */
    public void selectAnnot(Annot annot, int pageNum) {
        // Since find text locks the document, cancel it to release the document.
        mPdfViewCtrl.cancelFindText();
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            if (annot != null && annot.isValid()) {
                setAnnot(annot, pageNum);
                buildAnnotBBox();
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    /**
     * Converts density independent pixels to physical pixels.
     */
    protected float convDp2Pix(float dp) {
        return Utils.convDp2Pix(mPdfViewCtrl.getContext(), dp);
    }

    /**
     * Converts physical pixels to density independent pixels.
     */
    protected float convPix2Dp(float pix) {
        return Utils.convPix2Dp(mPdfViewCtrl.getContext(), pix);
    }

    /**
     * Gets a rectangle to use when selecting text.
     */
    public RectF getTextSelectRect(float x, float y) {
        float delta = 0.5f;
        float x2 = x + delta;
        float y2 = y + delta;
        delta *= 2;
        float x1 = x2 - delta >= 0 ? x2 - delta : 0;
        float y1 = y2 - delta >= 0 ? y2 - delta : 0;

        return new RectF(x1, y1, x2, y2);
    }

    /**
     * Returns string from resource ID
     *
     * @param id The resource ID
     * @return The string from resource ID
     */
    protected String getStringFromResId(@StringRes int id) {
        return mPdfViewCtrl.getResources().getString(id);
    }

    /**
     * Sets the author
     *
     * @param annot The markup annotation
     */
    protected void setAuthor(Markup annot) {
        final Context context = mPdfViewCtrl.getContext();
        if (context == null) {
            return;
        }
        try {
            boolean generateUID = annot.getUniqueID() == null;
            if (annot.getUniqueID() != null) {
                String uid = annot.getUniqueID().getAsPDFText();
                if (Utils.isNullOrEmpty(uid)) {
                    generateUID = true;
                }
            }
            if (generateUID) {
                setUniqueID(annot);
            }
        } catch (PDFNetException e) {
            e.printStackTrace();
        }
        if (mPdfViewCtrl.getToolManager() instanceof ToolManager) {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (toolManager.getAuthorId() != null) {
                setAuthor(annot, toolManager.getAuthorId());
                return;
            }
        }

        boolean authorNameHasBeenAsked = PdfViewCtrlSettingsManager.getAuthorNameHasBeenAsked(context);
        String authorName = PdfViewCtrlSettingsManager.getAuthorName(context);
        if (!authorNameHasBeenAsked && authorName.isEmpty()) {
            // Show dialog to get the author name.
            boolean askAuthor = false;
            if (mPdfViewCtrl.getToolManager() instanceof ToolManager) {
                if (((ToolManager) mPdfViewCtrl.getToolManager()).isShowAuthorDialog()) {
                    askAuthor = true;
                }
            }

            mMarkupToAuthor = annot;

            String possibleName = "";
            // If the author name in the preferences is empty, we try to get
            // the name of the current user in the device.
            int res = context.checkCallingOrSelfPermission("android.permission.GET_ACCOUNTS");
            if (res == PackageManager.PERMISSION_GRANTED) {
                Pattern emailPattern = Patterns.EMAIL_ADDRESS;
                Account[] accounts = AccountManager.get(context).getAccounts();
                for (Account account : accounts) {
                    if (emailPattern.matcher(account.name).matches()) {
                        possibleName = account.name;
                        break;
                    }
                }
            }

            PdfViewCtrlSettingsManager.setAuthorNameHasBeenAsked(context);

            if (askAuthor) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View authorNameDialog = inflater.inflate(R.layout.tools_dialog_author_name, null);
                final EditText authorNameEditText = (EditText) authorNameDialog.findViewById(R.id.tools_dialog_author_name_edittext);
                authorNameEditText.setText(possibleName);
                authorNameEditText.selectAll();

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                final AlertDialog authorDialog = builder.setView(authorNameDialog)
                    .setTitle(R.string.tools_dialog_author_name_title)
                    .setPositiveButton(R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String author = authorNameEditText.getText().toString().trim();
                            // Set author information on the markup
                            setAuthor(mMarkupToAuthor, author);
                            // Update preferences with the new name
                            PdfViewCtrlSettingsManager.updateAuthorName(context, author);
                        }
                    })
                    .setNegativeButton(R.string.tools_misc_skip, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).create();
                authorDialog.show();
                if (authorNameEditText.getText().length() == 0) {
                    // empty, don't allow OK
                    authorDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } else {
                    authorDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
                authorNameEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (authorDialog != null) {
                            if (s.length() == 0) {
                                // empty, don't allow OK
                                authorDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                            } else {
                                authorDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            }
                        }
                    }
                });
            } else {
                // Set author information on the markup
                setAuthor(mMarkupToAuthor, possibleName);
                // Update preferences with the new name
                PdfViewCtrlSettingsManager.updateAuthorName(context, possibleName);
            }
        } else {
            // Use author name in the preferences
            String author = PdfViewCtrlSettingsManager.getAuthorName(context);
            setAuthor(annot, author);
        }
    }

    private void setAuthor(Markup annot, String author) {
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            annot.setTitle(author);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * set unique id to annotation
     *
     * @param annot mark up annotation
     */
    protected void setUniqueID(Markup annot) {
        boolean shouldUnlock = false;
        try {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            String key = toolManager.generateKey();
            if (key != null) {
                mPdfViewCtrl.docLock(true);
                shouldUnlock = true;
                annot.setUniqueID(key);
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
     * set annotation date to now
     *
     * @param annot annotation
     */
    protected void setDateToNow(Annot annot) {
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            annot.setDateToNow();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    // Helper functions to obtain the storage key string for each of the annotation property

    /**
     * Returns the storage key string for color
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    protected String getColorKey(int annotType) {
        return ToolStyleConfig.getInstance().getColorKey(annotType, "");

//        String defaultResult = PREF_ANNOTATION_CREATION_LINE +
//            PREF_ANNOTATION_CREATION_COLOR;
//        if (!(mode instanceof ToolMode)) {
//            return defaultResult;
//        }
//
//        switch ((ToolMode) mode) {
//            case TEXT_HIGHLIGHT:
//                return PREF_ANNOTATION_CREATION_HIGHLIGHT +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case TEXT_UNDERLINE:
//                return PREF_ANNOTATION_CREATION_UNDERLINE +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case RECT_LINK:
//            case TEXT_LINK_CREATE:
//                return PREF_ANNOTATION_CREATION_LINK +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case TEXT_STRIKEOUT:
//                return PREF_ANNOTATION_CREATION_STRIKEOUT +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case TEXT_SQUIGGLY:
//                return PREF_ANNOTATION_CREATION_SQUIGGLY +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case TEXT_CREATE:
//                return PREF_ANNOTATION_CREATION_FREETEXT +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case INK_CREATE:
//                return PREF_ANNOTATION_CREATION_FREEHAND +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case RECT_CREATE:
//                return PREF_ANNOTATION_CREATION_RECTANGLE +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case OVAL_CREATE:
//                return PREF_ANNOTATION_CREATION_OVAL +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case POLYGON_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYGON +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case CLOUD_CREATE:
//                return PREF_ANNOTATION_CREATION_CLOUD +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case TEXT_ANNOT_CREATE:
//                return PREF_ANNOTATION_CREATION_NOTE +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case SIGNATURE:
//                return PREF_ANNOTATION_CREATION_SIGNATURE +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case ARROW_CREATE:
//                return PREF_ANNOTATION_CREATION_ARROW +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case POLYLINE_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYLINE +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            case FREE_HIGHLIGHTER:
//                return PREF_ANNOTATION_CREATION_FREE_HIGHLIGHTER +
//                    PREF_ANNOTATION_CREATION_COLOR;
//            default:
//                return defaultResult;
//        }
    }

    /**
     * Returns the storage key string for text color
     *
     * @return the storage key string for text color
     */
    protected String getTextColorKey(int annotType) {
        return ToolStyleConfig.getInstance().getTextColorKey(annotType, "");

//        return PREF_ANNOTATION_CREATION_FREETEXT +
//            PREF_ANNOTATION_CREATION_TEXT_COLOR;
    }

    protected String getTextSizeKey(int annotType) {
        return ToolStyleConfig.getInstance().getTextSizeKey(annotType, "");

//        return PREF_ANNOTATION_CREATION_FREETEXT +
//            PREF_ANNOTATION_CREATION_TEXT_SIZE;
    }

    /**
     * Returns the storage key string for thickness
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    protected String getThicknessKey(int annotType) {
        return ToolStyleConfig.getInstance().getThicknessKey(annotType, "");

//        String defaultResult = PREF_ANNOTATION_CREATION_LINE +
//            PREF_ANNOTATION_CREATION_THICKNESS;
//        if (!(mode instanceof ToolMode)) {
//            return defaultResult;
//        }
//        switch ((ToolMode) mode) {
//            case TEXT_UNDERLINE:
//                return PREF_ANNOTATION_CREATION_UNDERLINE +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case RECT_LINK:
//            case TEXT_LINK_CREATE:
//                return PREF_ANNOTATION_CREATION_LINK +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case TEXT_STRIKEOUT:
//                return PREF_ANNOTATION_CREATION_STRIKEOUT +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case TEXT_SQUIGGLY:
//                return PREF_ANNOTATION_CREATION_SQUIGGLY +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case TEXT_CREATE:
//                return PREF_ANNOTATION_CREATION_FREETEXT +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case INK_CREATE:
//                return PREF_ANNOTATION_CREATION_FREEHAND +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case RECT_CREATE:
//                return PREF_ANNOTATION_CREATION_RECTANGLE +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case OVAL_CREATE:
//                return PREF_ANNOTATION_CREATION_OVAL +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case POLYGON_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYGON +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case CLOUD_CREATE:
//                return PREF_ANNOTATION_CREATION_CLOUD +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case INK_ERASER:
//                return PREF_ANNOTATION_CREATION_ERASER +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case SIGNATURE:
//                return PREF_ANNOTATION_CREATION_SIGNATURE +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case ARROW_CREATE:
//                return PREF_ANNOTATION_CREATION_ARROW +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case POLYLINE_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYLINE +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            case FREE_HIGHLIGHTER:
//                return PREF_ANNOTATION_CREATION_FREE_HIGHLIGHTER +
//                    PREF_ANNOTATION_CREATION_THICKNESS;
//            default:
//                return defaultResult;
//        }
    }

    /**
     * Returns the storage key string for opacity
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    protected String getOpacityKey(int annotType) {
        return ToolStyleConfig.getInstance().getOpacityKey(annotType, "");

//        String defaultResult = PREF_ANNOTATION_CREATION_LINE +
//            PREF_ANNOTATION_CREATION_OPACITY;
//        if (!(mode instanceof ToolMode)) {
//            return defaultResult;
//        }
//        switch ((ToolMode) mode) {
//            case TEXT_HIGHLIGHT:
//                return PREF_ANNOTATION_CREATION_HIGHLIGHT +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case TEXT_UNDERLINE:
//                return PREF_ANNOTATION_CREATION_UNDERLINE +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case RECT_LINK:
//            case TEXT_LINK_CREATE:
//                return PREF_ANNOTATION_CREATION_LINK +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case TEXT_STRIKEOUT:
//                return PREF_ANNOTATION_CREATION_STRIKEOUT +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case TEXT_SQUIGGLY:
//                return PREF_ANNOTATION_CREATION_SQUIGGLY +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case TEXT_CREATE:
//                return PREF_ANNOTATION_CREATION_FREETEXT +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case INK_CREATE:
//                return PREF_ANNOTATION_CREATION_FREEHAND +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case RECT_CREATE:
//                return PREF_ANNOTATION_CREATION_RECTANGLE +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case OVAL_CREATE:
//                return PREF_ANNOTATION_CREATION_OVAL +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case POLYGON_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYGON +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case CLOUD_CREATE:
//                return PREF_ANNOTATION_CREATION_CLOUD +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case TEXT_ANNOT_CREATE:
//                return PREF_ANNOTATION_CREATION_NOTE +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case ARROW_CREATE:
//                return PREF_ANNOTATION_CREATION_ARROW +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case POLYLINE_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYLINE +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            case FREE_HIGHLIGHTER:
//                return PREF_ANNOTATION_CREATION_FREE_HIGHLIGHTER +
//                    PREF_ANNOTATION_CREATION_OPACITY;
//            default:
//                return defaultResult;
//        }
    }

    /**
     * Returns the storage key string for color fill
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    protected String getColorFillKey(int annotType) {
        return ToolStyleConfig.getInstance().getFillColorKey(annotType, "");

//        String defaultResult = PREF_ANNOTATION_CREATION_FREEHAND +
//            PREF_ANNOTATION_CREATION_FILL_COLOR;
//        if (!(mode instanceof ToolMode)) {
//            return defaultResult;
//        }
//        switch ((ToolMode) mode) {
//            case TEXT_CREATE:
//                return PREF_ANNOTATION_CREATION_FREETEXT +
//                    PREF_ANNOTATION_CREATION_FILL_COLOR;
//            case RECT_CREATE:
//                return PREF_ANNOTATION_CREATION_RECTANGLE +
//                    PREF_ANNOTATION_CREATION_FILL_COLOR;
//            case OVAL_CREATE:
//                return PREF_ANNOTATION_CREATION_OVAL +
//                    PREF_ANNOTATION_CREATION_FILL_COLOR;
//            case POLYGON_CREATE:
//                return PREF_ANNOTATION_CREATION_POLYGON +
//                    PREF_ANNOTATION_CREATION_FILL_COLOR;
//            case CLOUD_CREATE:
//                return PREF_ANNOTATION_CREATION_CLOUD +
//                    PREF_ANNOTATION_CREATION_FILL_COLOR;
//            default:
//                return defaultResult;
//        }
    }

    /**
     * Returns the storage key string for icon
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    protected String getIconKey(int annotType) {
        return ToolStyleConfig.getInstance().getIconKey(annotType, "");

//        String defaultResult = PREF_ANNOTATION_CREATION_NOTE +
//            PREF_ANNOTATION_CREATION_ICON;
//        if (!(mode instanceof ToolMode)) {
//            return defaultResult;
//        }
//        switch ((ToolMode) mode) {
//            case TEXT_ANNOT_CREATE:
//                return PREF_ANNOTATION_CREATION_NOTE +
//                    PREF_ANNOTATION_CREATION_ICON;
//            default:
//                return defaultResult;
//        }
    }

    /**
     * Returns the storage key string for font
     *
     * @param annotType The annotation type
     * @return The storage key string
     */
    public static String getFontKey(int annotType) {
        return ToolStyleConfig.getInstance().getFontKey(annotType, "");

//        String defaultResult = PREF_ANNOTATION_CREATION_FREETEXT +
//            PREF_ANNOTATION_CREATION_FONT;
//        if (!(mode instanceof ToolMode)) {
//            return defaultResult;
//        }
//        switch ((ToolMode) mode) {
//            case TEXT_CREATE:
//                return PREF_ANNOTATION_CREATION_FREETEXT +
//                    PREF_ANNOTATION_CREATION_FONT;
//            default:
//                return defaultResult;
//        }
    }

    /**
     * Returns the storage key string for ruler base unit
     *
     * @return The storage key string
     */
    protected String getRulerBaseUnitKey(int annotType) {
        return ToolStyleConfig.getInstance().getRulerBaseUnitKey(annotType, "");
    }

    /**
     * Returns the storage key string for ruler translate unit
     *
     * @return The storage key string
     */
    protected String getRulerTranslateUnitKey(int annotType) {
        return ToolStyleConfig.getInstance().getRulerTranslateUnitKey(annotType, "");
    }

    /**
     * Returns the storage key string for ruler base value
     *
     * @return The storage key string
     */
    protected String getRulerBaseValueKey(int annotType) {
        return ToolStyleConfig.getInstance().getRulerBaseValueKey(annotType, "");
    }

    /**
     * Returns the storage key string for ruler translate value
     *
     * @return The storage key string
     */
    protected String getRulerTranslateValueKey(int annotType) {
        return ToolStyleConfig.getInstance().getRulerTranslateValueKey(annotType, "");
    }

    /**
     * get tool mode from annotation type
     *
     * @param annot annotation
     * @return tool mode
     */
    protected static ToolMode getModeFromAnnotType(Annot annot) {
        ToolMode mode = ToolMode.LINE_CREATE;
        if (annot != null) {
            try {
                int annotType = annot.getType();
                switch (annotType) {
                    case Annot.e_Line:
                        if (AnnotUtils.isArrow(annot)) {
                            return ToolMode.ARROW_CREATE;
                        } else if (AnnotUtils.isRuler(annot)) {
                            return ToolMode.RULER_CREATE;
                        }
                        return ToolMode.LINE_CREATE;
                    case Annot.e_Polyline:
                        return ToolMode.POLYLINE_CREATE;
                    case Annot.e_Square:
                        return ToolMode.RECT_CREATE;
                    case Annot.e_Circle:
                        return ToolMode.OVAL_CREATE;
                    case Annot.e_Sound:
                        return ToolMode.SOUND_CREATE;
                    case Annot.e_FileAttachment:
                        return ToolMode.FILE_ATTACHMENT_CREATE;
                    case Annot.e_Polygon:
                        if (AnnotUtils.isCloud(annot)) {
                            return ToolMode.CLOUD_CREATE;
                        }
                        return ToolMode.POLYGON_CREATE;
                    case Annot.e_Highlight:
                        return ToolMode.TEXT_HIGHLIGHT;
                    case Annot.e_Underline:
                        return ToolMode.TEXT_UNDERLINE;
                    case Annot.e_StrikeOut:
                        return ToolMode.TEXT_STRIKEOUT;
                    case Annot.e_Squiggly:
                        return ToolMode.TEXT_SQUIGGLY;
                    case Annot.e_FreeText:
                        if (AnnotUtils.isCallout(annot)) {
                            return ToolMode.CALLOUT_CREATE;
                        }
                        return ToolMode.TEXT_CREATE;
                    case Annot.e_Ink:
                        if (AnnotUtils.isFreeHighlighter(annot)) {
                            return ToolMode.FREE_HIGHLIGHTER;
                        }
                        return ToolMode.INK_CREATE;
                    case Annot.e_Text:
                        return ToolMode.TEXT_ANNOT_CREATE;
                    case Annot.e_Link:
                        return ToolMode.RECT_LINK;
                    default:
                        return ToolMode.LINE_CREATE;
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
        return mode;
    }

    /**
     * Called when an annotation is added.
     *
     * @param annot The added annotation
     * @param page  The page where the annotation is on
     */
    protected void raiseAnnotationAddedEvent(Annot annot, int page) {
        if (annot == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Annot is null"));
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        toolManager.raiseAnnotationsAddedEvent(annots);
    }

    /**
     * Called when annotation are added.
     *
     * @param annots The map of added annotations (pairs of annotation and the page number
     *               where the annotation is on)
     */
    protected void raiseAnnotationAddedEvent(Map<Annot, Integer> annots) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.raiseAnnotationsAddedEvent(annots);
    }

    /**
     * Called right before an annotation is modified.
     *
     * @param annot The annotation to be modified
     * @param page  The page where the annotation is on
     */
    protected void raiseAnnotationPreModifyEvent(Annot annot, int page) {
        if (annot == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Annot is null"));
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        toolManager.raiseAnnotationsPreModifyEvent(annots);
    }

    /**
     * Called when annotation are going to be modified.
     *
     * @param annots The map of annotations to be modified (pairs of annotation and the page number
     *               where the annotation is on)
     */
    protected void raiseAnnotationPreModifyEvent(Map<Annot, Integer> annots) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.raiseAnnotationsPreModifyEvent(annots);
    }

    /**
     * Called when an annotation is modified.
     *
     * @param annot The modified annotation
     * @param page  The page where the annotation is on
     */
    protected void raiseAnnotationModifiedEvent(Annot annot, int page) {
        if (annot == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Annot is null"));
            return;
        }
        setDateToNow(annot);
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        toolManager.raiseAnnotationsModifiedEvent(annots, getAnnotationModificationBundle(null));
    }

    protected void raiseAnnotationModifiedEvent(Annot annot, int page, Bundle bundle) {
        if (annot == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Annot is null"));
            return;
        }
        setDateToNow(annot);
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        toolManager.raiseAnnotationsModifiedEvent(annots, getAnnotationModificationBundle(bundle));
    }

    /**
     * Called when annotation are modified.
     *
     * @param annots The map of modified annotations (pairs of annotation and the page number
     *               where the annotation is on)
     */
    protected void raiseAnnotationModifiedEvent(Map<Annot, Integer> annots) {
        for (Map.Entry<Annot, Integer> entry : annots.entrySet()) {
            setDateToNow(entry.getKey());
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.raiseAnnotationsModifiedEvent(annots, getAnnotationModificationBundle(null));
    }

    /**
     * Called right before an annotation is removed.
     *
     * @param annot The annotation to removed
     * @param page  The page where the annotation is on
     */
    protected void raiseAnnotationPreRemoveEvent(Annot annot, int page) {
        if (annot == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Annot is null"));
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        toolManager.raiseAnnotationsPreRemoveEvent(annots);
    }

    /**
     * Called when annotation are going to be removed.
     *
     * @param annots The map of annotations to be removed (pairs of annotation and the page number
     *               where the annotation is on)
     */
    protected void raiseAnnotationPreRemoveEvent(Map<Annot, Integer> annots) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.raiseAnnotationsPreRemoveEvent(annots);
    }

    /**
     * Called when an annotation is removed.
     *
     * @param annot The removed annotation
     * @param page  The page where the annotation is on
     */
    protected void raiseAnnotationRemovedEvent(Annot annot, int page) {
        if (annot == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("Annot is null"));
            return;
        }
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        toolManager.raiseAnnotationsRemovedEvent(annots);
    }

    /**
     * Called when annotation are removed.
     *
     * @param annots The map of removed annotations (pairs of annotation and the page number
     *               where the annotation is on)
     */
    protected void raiseAnnotationRemovedEvent(Map<Annot, Integer> annots) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.raiseAnnotationsRemovedEvent(annots);
    }

    /**
     * Called when an annotations action has been taken place.
     */
    protected void raiseAnnotationActionEvent() {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        toolManager.raiseAnnotationActionEvent();
    }

    /**
     * Handle annotation
     *
     * @param annot annotation
     * @return true then intercept the subclass function, false otherwise
     */
    protected final boolean onInterceptAnnotationHandling(@Nullable Annot annot) {
        Bundle bundle = getAnnotationModificationBundle(null);
        return onInterceptAnnotationHandling(annot, bundle);
    }

    /**
     * Handle annotation
     *
     * @param annot  annotation which is pending to be handled
     * @param bundle information going to pass to {@link ToolManager}
     * @return true then intercept the subclass function, false otherwise
     */
    protected final boolean onInterceptAnnotationHandling(@Nullable Annot annot, @NonNull Bundle bundle) {
        bundle = getAnnotationModificationBundle(bundle);
        bundle.putInt(PAGE_NUMBER, mAnnotPageNum);
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        return toolManager.raiseInterceptAnnotationHandlingEvent(annot, bundle, ToolManager.getDefaultToolMode(getToolMode()));
    }

    /**
     * Call this function when a dialog is about to show up.
     */
    protected boolean onInterceptDialogEvent(AlertDialog dialog) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        return toolManager.raiseInterceptDialogEvent(dialog);
    }

    /**
     * Adds old tools.
     */
    protected void addOldTools() {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (null != toolManager) {
            toolManager.getOldTools().add(this);
        }
    }

    /**
     * Converts from page rect to screen rect
     *
     * @param pageRect The page rect
     * @param page     The page number
     * @return The screen rect
     */
    protected com.pdftron.pdf.Rect convertFromPageRectToScreenRect(com.pdftron.pdf.Rect pageRect, int page) {
        com.pdftron.pdf.Rect screenRect = null;

        if (pageRect != null) {
            try {
                float sx = mPdfViewCtrl.getScrollX();
                float sy = mPdfViewCtrl.getScrollY();

                float x1, y1, x2, y2;

                double[] pts1 = mPdfViewCtrl.convPagePtToScreenPt(pageRect.getX1(), pageRect.getY1(), page);
                double[] pts2 = mPdfViewCtrl.convPagePtToScreenPt(pageRect.getX2(), pageRect.getY2(), page);

                x1 = (float) pts1[0] + sx;
                y1 = (float) pts1[1] + sy;
                x2 = (float) pts2[0] + sx;
                y2 = (float) pts2[1] + sy;

                screenRect = new com.pdftron.pdf.Rect(x1, y1, x2, y2);

            } catch (PDFNetException ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        }

        return screenRect;
    }

    /**
     * Sets the annotataion
     *
     * @param annot The annotation
     * @param page  The page number
     */
    protected void setAnnot(Annot annot, int pageNum) {
        mAnnot = annot;
        mAnnotPageNum = pageNum;
        try {
            if (mAnnot.getUniqueID() != null &&
                mPdfViewCtrl.getToolManager() instanceof ToolManager) {
                ((ToolManager) mPdfViewCtrl.getToolManager()).setSelectedAnnot(mAnnot, mAnnotPageNum);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Unsets the annotation.
     */
    protected void unsetAnnot() {
        removeAnnotView();
        mAnnot = null;
        try {
            if (mPdfViewCtrl.getToolManager() instanceof ToolManager) {
                ((ToolManager) mPdfViewCtrl.getToolManager()).setSelectedAnnot(null, -1);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Adds real time annotation view
     *
     * @return true if a view is added, false otherwise
     */
    protected boolean addAnnotView() {
        if (mAnnot == null) {
            return false;
        }
        if (mAnnotView != null) {
            removeAnnotView(false);
        }
        try {
            AnnotStyle annotStyle = AnnotUtils.getAnnotStyle(mAnnot);
            if (canAddAnnotView(mAnnot, annotStyle)) {
                mPdfViewCtrl.hideAnnotation(mAnnot);
                Page page = mAnnot.getPage();
                mPdfViewCtrl.update(mAnnot, page.getIndex());

                mAnnotView = new AnnotView(mPdfViewCtrl.getContext());
                mAnnotView.setAnnotStyle(mPdfViewCtrl, annotStyle);
                mAnnotView.setZoom(mPdfViewCtrl.getZoom());
                mAnnotView.setHasPermission(mHasSelectionPermission);
                mPdfViewCtrl.addView(mAnnotView);
                return true;
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
        return false;
    }

    protected void removeAnnotView() {
        removeAnnotView(true);
    }

    protected void removeAnnotView(boolean delayRemoval) {
        if (mAnnotView != null) {
            if (delayRemoval) {
                mAnnotView.setDelayViewRemoval(delayRemoval);
            } else {
                mPdfViewCtrl.removeView(mAnnotView);
                mAnnotView = null;
            }

            if (mAnnot != null) {
                boolean shouldUnlock = false;
                try {
                    mPdfViewCtrl.docLock(true);
                    shouldUnlock = true;
                    mPdfViewCtrl.showAnnotation(mAnnot);
                    Page page = mAnnot.getPage();
                    mPdfViewCtrl.update(mAnnot, page.getIndex());
                    mPdfViewCtrl.invalidate();
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                } finally {
                    if (shouldUnlock) {
                        mPdfViewCtrl.docUnlock();
                    }
                }
            }
        }
    }

    protected boolean canAddAnnotView(Annot annot, AnnotStyle annotStyle) {
        return false;
    }

    /**
     * Deletes the annotation.
     */
    protected void deleteAnnot() {
        if (mAnnot == null || mPdfViewCtrl == null) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            mNextToolMode = mCurrentDefaultToolMode;
            raiseAnnotationPreRemoveEvent(mAnnot, mAnnotPageNum);
            Page page = mPdfViewCtrl.getDoc().getPage(mAnnotPageNum);
            page.annotRemove(mAnnot);
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            // make sure to raise remove event after mPdfViewCtrl.update and before unsetAnnot
            raiseAnnotationRemovedEvent(mAnnot, mAnnotPageNum);
            if (sDebug) Log.d(TAG, "going to unsetAnnot: onQuickMenuclicked");

            unsetAnnot();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * Flattens the annotation.
     */
    protected void flattenAnnot() {
        if (mAnnot == null || mPdfViewCtrl == null) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread
            // safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            raiseAnnotationPreModifyEvent(mAnnot, mAnnotPageNum);

            // flatten annotation
            Page page = mPdfViewCtrl.getDoc().getPage(mAnnotPageNum);
            mAnnot.flatten(page);
            mPdfViewCtrl.update(mAnnot, mAnnotPageNum);

            raiseAnnotationModifiedEvent(mAnnot, mAnnotPageNum);

            unsetAnnot();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * Handles flattening the annotation.
     * Checks if the user does indeed want to flatten the annotation.
     */
    protected void handleFlattenAnnot() {
        SharedPreferences settings = getToolPreferences(mPdfViewCtrl.getContext());
        if (settings.getBoolean(STAMP_SHOW_FLATTEN_WARNING, true)) {
            // show flatten alert dialog
            LayoutInflater inflater = LayoutInflater.from(mPdfViewCtrl.getContext());
            View customLayout = inflater.inflate(R.layout.alert_dialog_with_checkbox, null);
            String text = mPdfViewCtrl.getContext().getResources().getString(R.string.tools_dialog_flatten_dialog_msg);
            final TextView dialogTextView = customLayout.findViewById(R.id.dialog_message);
            dialogTextView.setText(text);
            final CheckBox dialogCheckBox = customLayout.findViewById(R.id.dialog_checkbox);
            dialogCheckBox.setChecked(true);

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mPdfViewCtrl.getContext())
                .setView(customLayout)
                .setTitle(mPdfViewCtrl.getContext().getResources().getString(R.string.tools_dialog_flatten_dialog_title))
                .setPositiveButton(R.string.tools_qm_flatten, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean showAgain = !dialogCheckBox.isChecked();
                        SharedPreferences settings = getToolPreferences(mPdfViewCtrl.getContext());
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean(STAMP_SHOW_FLATTEN_WARNING, showAgain);
                        editor.apply();

                        // flatten
                        flattenAnnot();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean showAgain = !dialogCheckBox.isChecked();
                        SharedPreferences settings = getToolPreferences(mPdfViewCtrl.getContext());
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean(STAMP_SHOW_FLATTEN_WARNING, showAgain);
                        editor.apply();

                        // show quick menu
                        showMenu(getAnnotRect());
                    }
                });

            dialogBuilder.create().show();
        } else {
            // skip flatten alert dialog
            // flatten
            flattenAnnot();
        }
    }


    /**
     * @hide
     */
    protected final boolean isMadeByPDFTron(Annot annot) throws PDFNetException {
        String[] supportedTag = new String[]{Tool.PDFTRON_ID, "pdftronlink"};
        if (annot != null && annot.getSDFObj() != null) {
            Obj sdfObj = annot.getSDFObj();
            for (String tag : supportedTag) {
                Object selfMadeObj = sdfObj.findObj(tag);
                if (selfMadeObj != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Creates the formfield appearance. Default to checkmark.
     * Can be overridden for other appearances.
     *
     * @param doc    the PDFDoc
     * @param symbol the appearance symbol
     *               "4" = Checkbox
     *               "l" = Circle
     *               "8" = Cross
     *               "u" = Diamond
     *               "n" = Square
     *               "H" = Star
     * @return appearance stream
     * @throws PDFNetException PDFNet exception
     */
    protected Obj createFormFieldAppearance(PDFDoc doc, String symbol) throws PDFNetException {
        // Create a checkmark appearance stream
        // ------------------------------------
        ElementBuilder build = new ElementBuilder();
        ElementWriter writer = new ElementWriter();
        writer.begin(doc);
        writer.writeElement(build.createTextBegin());
        {
            // options are checkbox ("4"), circle ("l"), diamond ("H"), cross ("\x35")
            // See section D.4 "ZapfDingbats Set and Encoding" in PDF Reference
            // Manual for the complete graphical map for ZapfDingbats font.
            Element checkmark = build.createTextRun(symbol, Font.create(doc, Font.e_zapf_dingbats), 1);
            writer.writeElement(checkmark);
        }
        writer.writeElement(build.createTextEnd());

        Obj stm = writer.end();

        // Set the bounding box
        stm.putRect("BBox", -0.2, -0.2, 1, 1);
        stm.putName("Subtype", "Form");
        return stm;
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }

    public static Bundle getAnnotationModificationBundle(Bundle bundle) {
        if (bundle == null) {
            bundle = new Bundle();
        }
        return bundle;
    }

    public static SharedPreferences getToolPreferences(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_FILE_NAME, 0);
    }

    private static class PageNumberRemovalHandler extends Handler {
        private final WeakReference<Tool> mTool;

        public PageNumberRemovalHandler(Tool tool) {
            mTool = new WeakReference<Tool>(tool);
        }

        @Override
        public void handleMessage(Message msg) {
            Tool tool = mTool.get();
            if (tool != null) {
                ToolManager toolManager = (ToolManager) tool.mPdfViewCtrl.getToolManager();
                toolManager.hideBuiltInPageNumber();
            }
        }
    }

    private class QuickMenuDismissListener implements QuickMenu.OnDismissListener {

        @Override
        public void onDismiss() {
            if (mPdfViewCtrl.getToolManager() instanceof ToolManager) {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                toolManager.setQuickMenuJustClosed(true);
                toolManager.onQuickMenuDismissed();
            }
            // When dismissed, trigger the menu-clicked call-back function.
            if (mQuickMenu != null) {
                QuickMenuItem selectedMenuItem = mQuickMenu.getSelectedMenuItem();
                if (selectedMenuItem != null) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_QUICKTOOL, selectedMenuItem.getText(), getModeAHLabel());
                    ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                    toolManager.onQuickMenuClicked(selectedMenuItem);
                }
            }
        }
    }
}
