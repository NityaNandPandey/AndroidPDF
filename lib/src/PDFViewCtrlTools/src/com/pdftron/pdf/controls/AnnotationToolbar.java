//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.transition.ChangeBounds;
import android.support.transition.Fade;
import android.support.transition.Slide;
import android.support.transition.Transition;
import android.support.transition.TransitionManager;
import android.support.transition.TransitionSet;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.TooltipCompat;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.config.ToolConfig;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.controls.UndoRedoPopupWindow.OnUndoRedoListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.AdvancedShapeCreate;
import com.pdftron.pdf.tools.Pan;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.StampStatePopup;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.pdftron.pdf.tools.ToolManager.ToolMode.INK_CREATE;
import static com.pdftron.pdf.tools.ToolManager.ToolMode.RUBBER_STAMPER;
import static com.pdftron.pdf.tools.ToolManager.ToolMode.SIGNATURE;
import static com.pdftron.pdf.tools.ToolManager.ToolMode.STAMPER;
import static com.pdftron.pdf.utils.StampStatePopup.STATE_IMAGE_STAMP;
import static com.pdftron.pdf.utils.StampStatePopup.STATE_RUBBER_STAMP;
import static com.pdftron.pdf.utils.StampStatePopup.STATE_SIGNATURE;

/**
 * The AnnotationToolbar works with a {@link com.pdftron.pdf.tools.ToolManager} to
 * allow quick selection of different tools. The toolbar shows a list of buttons
 * which prompts the associated ToolManager to switch to that tool.
 * If undo/redo is enabled in the PDFViewCtrl the undo/redo buttons are also provided.
 */
public class AnnotationToolbar extends InsectHandlerToolbar implements
    ToolManager.ToolChangedListener,
    EditToolbarImpl.OnEditToolbarListener,
    AdvancedShapeCreate.OnEditToolbarListener {

    /**
     * Starts with regular annotation toolbar
     */
    public static final int START_MODE_NORMAL_TOOLBAR = 0;
    /**
     * Starts with edit toolbar
     */
    public static final int START_MODE_EDIT_TOOLBAR = 1;

    private static final String PREF_KEY_LINE = "pref_line";
    private static final String PREF_KEY_RECT = "pref_rect";
    private static final String PREF_KEY_TEXT = "pref_text";
    private static final int NUM_TABLET_NORMAL_STATE_ICONS = 16;
    private static final int NUM_PHONE_NORMAL_STATE_ICONS = 9;
    private static final int START_MODE_UNKNOWN = -1;
    private static final int ANIMATION_DURATION = 250;

    private EditToolbarImpl mEditToolbarImpl;
    private ToolManager mToolManager;
    private PDFViewCtrl mPdfViewCtrl;
    private AnnotToolbarOverflowPopupWindow mOverflowPopupWindow;
    private ArrayList<View> mButtons;
    private boolean mButtonStayDown;
    private boolean mDismissAfterExitEdit;
    private int mSelectedButtonId;
    private String mStampState;
    private boolean mEventAction;

    private AnnotStyleDialogFragment mAnnotStyleDialog;

    private StampStatePopup mStampStatePopup;
    private boolean mLayoutChanged;
    private boolean mForceUpdateView;
    private int mSelectedToolId = -1;

    private int mToolbarBackgroundColor;
    private int mToolbarToolBackgroundColor;
    private int mToolbarToolIconColor;
    private int mToolbarCloseIconColor;

    private AnnotationToolbarListener mAnnotationToolbarListener;
    private OnUndoRedoListener mOnUndoRedoListener;

    private SparseIntArray mButtonAnnotTypeMap;
    private HashMap<String, Integer> mVisibleAnnotTypeMap;
    private boolean mShouldExpand; // should the toolbar be expanded when phone is in portrait mode
    private boolean mIsExpanded; // is the toolbar in expanded mode
    private ArrayList<GroupedItem> mGroupItems;

    /**
     * Class constructor
     */
    public AnnotationToolbar(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Class constructor
     */
    public AnnotationToolbar(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.annotation_toolbar);
    }

    /**
     * Class constructor
     */
    public AnnotationToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.AnnotationToolbarStyle);
    }

    /**
     * Class constructor
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AnnotationToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(@NonNull Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        // initialize colors
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AnnotationToolbar, defStyleAttr, defStyleRes);
        try {
            mToolbarBackgroundColor = typedArray.getColor(R.styleable.AnnotationToolbar_colorBackground, Color.BLACK);
            mToolbarToolBackgroundColor = typedArray.getColor(R.styleable.AnnotationToolbar_colorToolBackground, Color.BLACK);
            mToolbarToolIconColor = typedArray.getColor(R.styleable.AnnotationToolbar_colorToolIcon, Color.WHITE);
            mToolbarCloseIconColor = typedArray.getColor(R.styleable.AnnotationToolbar_colorCloseIcon, Color.WHITE);
        } finally {
            typedArray.recycle();
        }

        // collect annot types
        LayoutInflater.from(context).inflate(R.layout.controls_annotation_toolbar_layout, this, true);
        mButtonAnnotTypeMap = new SparseIntArray();
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_stickynote, Annot.e_Text);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_text_highlight, Annot.e_Highlight);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_text_strikeout, Annot.e_StrikeOut);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_text_underline, Annot.e_Underline);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_text_squiggly, Annot.e_Squiggly);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_free_highlighter, AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_stamp, AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_freehand, Annot.e_Ink);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_eraser, AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_freetext, Annot.e_FreeText);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_callout, AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_arrow, AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_ruler, AnnotStyle.CUSTOM_ANNOT_TYPE_RULER);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_line, Annot.e_Line);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_polyline, Annot.e_Polyline);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_rectangle, Annot.e_Square);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_oval, Annot.e_Circle);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_polygon, Annot.e_Polygon);
        mButtonAnnotTypeMap.put(R.id.controls_annotation_toolbar_tool_cloud, AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD);

        // collect visible annot types
        String visibleAnnotTypesJsonStr = PdfViewCtrlSettingsManager.getAnnotToolbarVisibleAnnotTypes(context);
        mVisibleAnnotTypeMap = new HashMap<>();
        if (!Utils.isNullOrEmpty(visibleAnnotTypesJsonStr)) {
            try {
                JSONObject object = new JSONObject(visibleAnnotTypesJsonStr);
                if (object.has(PREF_KEY_LINE)) {
                    mVisibleAnnotTypeMap.put(PREF_KEY_LINE, object.getInt(PREF_KEY_LINE));
                }
                if (object.has(PREF_KEY_RECT)) {
                    mVisibleAnnotTypeMap.put(PREF_KEY_RECT, object.getInt(PREF_KEY_RECT));
                }
                if (object.has(PREF_KEY_TEXT)) {
                    mVisibleAnnotTypeMap.put(PREF_KEY_TEXT, object.getInt(PREF_KEY_TEXT));
                }
            } catch (JSONException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        // grouped items
        mGroupItems = new ArrayList<>();
        mGroupItems.add(new GroupedItem(PREF_KEY_LINE, new int[]{Annot.e_Line, AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW, Annot.e_Polyline, AnnotStyle.CUSTOM_ANNOT_TYPE_RULER}));
        mGroupItems.add(new GroupedItem(PREF_KEY_RECT, new int[]{Annot.e_Square, Annot.e_Circle, Annot.e_Polygon, AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD}));
        mGroupItems.add(new GroupedItem(PREF_KEY_TEXT, new int[] {Annot.e_FreeText, AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT}));
    }

    /**
     * Setups the annotation toolbar window.
     *
     * @param toolManager The toolManager class
     */
    public void setup(@NonNull ToolManager toolManager) {
        setup(toolManager, null);
    }

    /**
     * Setups the annotation toolbar window.
     *
     * @param toolManager The toolManager class
     * @param listener    The listener for undo/redo events
     */
    public void setup(@NonNull ToolManager toolManager,
                      @Nullable OnUndoRedoListener listener) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        mToolManager = toolManager;
        mPdfViewCtrl = mToolManager.getPDFViewCtrl();
        mOnUndoRedoListener = listener;

        // init signature state:
        SharedPreferences settings = Tool.getToolPreferences(context);
        mStampState = settings.getString(Tool.ANNOTATION_TOOLBAR_SIGNATURE_STATE, STATE_SIGNATURE);
        if ("stamper".equals(mStampState)) {
            // workaround for mistakenly change the value of STATE_IMAGE_STAMP
            // from "stamper" to "stamp"
            mStampState = STATE_IMAGE_STAMP;
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(Tool.ANNOTATION_TOOLBAR_SIGNATURE_STATE, mStampState);
            editor.apply();
        }
        checkStampState();

        // Configure buttons
        initButtons();

        mToolManager.addToolChangedListener(this);
        // Force tool to be Pan when using the toolbar.
        mToolManager.setTool(mToolManager.createTool(ToolMode.PAN, null));

        initSelectedButton();

        setVisibility(View.GONE);
    }

    private void initViews() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        // initialize the background color of the annotation toolbar:
        setBackgroundColor(mToolbarBackgroundColor);

        // initialize the color of each tool in the annotation toolbar:

        class ToolItem {
            int color;
            private @IdRes
            int id;
            private @DrawableRes
            int drawable;
            private boolean spinner;

            private ToolItem(int type, @IdRes int id, boolean spinner) {
                this(type, id, AnnotUtils.getAnnotImageResId(type), spinner);
            }

            private ToolItem(@SuppressWarnings("unused") int type, @IdRes int id, @DrawableRes int drawable, boolean spinner) {
                this(type, id, drawable, spinner, mToolbarToolIconColor);
            }

            private ToolItem(@SuppressWarnings("unused") int type, @IdRes int id, @DrawableRes int drawable, boolean spinner, int color) {
                this.id = id;
                this.drawable = drawable;
                this.spinner = spinner;
                this.color = color;
            }
        }

        ArrayList<ToolItem> tools = new ArrayList<>();
        tools.add(new ToolItem(Annot.e_Text, R.id.controls_annotation_toolbar_tool_stickynote, true));
        tools.add(new ToolItem(Annot.e_Highlight, R.id.controls_annotation_toolbar_tool_text_highlight, true));
        tools.add(new ToolItem(Annot.e_StrikeOut, R.id.controls_annotation_toolbar_tool_text_strikeout, true));
        tools.add(new ToolItem(Annot.e_Underline, R.id.controls_annotation_toolbar_tool_text_underline, true));
        tools.add(new ToolItem(Annot.e_Squiggly, R.id.controls_annotation_toolbar_tool_text_squiggly, true));
        tools.add(new ToolItem(AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER, R.id.controls_annotation_toolbar_tool_free_highlighter, true));
        tools.add(new ToolItem(AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE, R.id.controls_annotation_toolbar_tool_stamp, !mIsExpanded && getStampsEnabledCount() >= 2));
        tools.add(new ToolItem(Annot.e_Ink, R.id.controls_annotation_toolbar_tool_freehand, false));
        tools.add(new ToolItem(AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER, R.id.controls_annotation_toolbar_tool_eraser, true));
        tools.add(new ToolItem(Annot.e_FreeText, R.id.controls_annotation_toolbar_tool_freetext, true));
        tools.add(new ToolItem(AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT, R.id.controls_annotation_toolbar_tool_callout, true));
        tools.add(new ToolItem(-1, R.id.controls_annotation_toolbar_tool_image_stamper, R.drawable.ic_annotation_image_black_24dp, false));
        tools.add(new ToolItem(-1, R.id.controls_annotation_toolbar_tool_rubber_stamper, R.drawable.ic_annotation_stamp_black_24dp, false));
        tools.add(new ToolItem(Annot.e_Line, R.id.controls_annotation_toolbar_tool_line, true));
        tools.add(new ToolItem(AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW, R.id.controls_annotation_toolbar_tool_arrow, true));
        tools.add(new ToolItem(AnnotStyle.CUSTOM_ANNOT_TYPE_RULER, R.id.controls_annotation_toolbar_tool_ruler, true));
        tools.add(new ToolItem(Annot.e_Polyline, R.id.controls_annotation_toolbar_tool_polyline, true));
        tools.add(new ToolItem(Annot.e_Square, R.id.controls_annotation_toolbar_tool_rectangle, true));
        tools.add(new ToolItem(Annot.e_Circle, R.id.controls_annotation_toolbar_tool_oval, true));
        tools.add(new ToolItem(Annot.e_Polygon, R.id.controls_annotation_toolbar_tool_polygon, true));
        tools.add(new ToolItem(AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD, R.id.controls_annotation_toolbar_tool_cloud, true));
        tools.add(new ToolItem(-1, R.id.controls_annotation_toolbar_tool_multi_select, R.drawable.ic_select_rectangular_black_24dp, false));
        tools.add(new ToolItem(-1, R.id.controls_annotation_toolbar_tool_pan, R.drawable.ic_pan_black_24dp, false));
        tools.add(new ToolItem(-1, R.id.controls_annotation_toolbar_btn_close, R.drawable.ic_close_black_24dp, false, mToolbarCloseIconColor));
        tools.add(new ToolItem(-1, R.id.controls_annotation_toolbar_btn_more, R.drawable.ic_overflow_white_24dp, false));

        int width = getToolWidth();
        int height = getToolHeight();

        // initialize the color of each tool in the annotation toolbar:
        int spinnerDrawableId = R.drawable.controls_toolbar_spinner_selected_blue;
        Drawable spinnerBitmapDrawable = ViewerUtils.getBitmapDrawable(context, spinnerDrawableId,
            width, height, mToolbarToolBackgroundColor, mIsExpanded);
        Drawable normalBitmapDrawable;
        if (mIsExpanded) {
            normalBitmapDrawable = Utils.getDrawable(context, R.drawable.rounded_corners);
            if (normalBitmapDrawable != null) {
                normalBitmapDrawable = normalBitmapDrawable.mutate();
                normalBitmapDrawable.setColorFilter(mToolbarToolBackgroundColor, PorterDuff.Mode.SRC_ATOP);
            }
        } else {
            normalBitmapDrawable = ViewerUtils.getBitmapDrawable(context, R.drawable.controls_annotation_toolbar_bg_selected_blue,
                width, height, mToolbarToolBackgroundColor, false);
        }

        for (ToolItem tool : tools) {
            findViewById(tool.id).setBackground(ViewerUtils.createBackgroundSelector(tool.spinner ? spinnerBitmapDrawable : normalBitmapDrawable));
            Drawable drawable = Utils.createImageDrawableSelector(context, tool.drawable, mToolbarToolIconColor);
            ((AppCompatImageButton) findViewById(tool.id)).setImageDrawable(drawable);
        }

        updateStampBtnState();
        updateStampPopupSize();
    }

    private int getStampsEnabledCount() {

        int stampsEnabledCounts = 0;
        if (!mToolManager.isToolModeDisabled(SIGNATURE)) {
            ++stampsEnabledCounts;
        }
        if (!mToolManager.isToolModeDisabled(STAMPER)) {
            ++stampsEnabledCounts;
        }
        if (!mToolManager.isToolModeDisabled(RUBBER_STAMPER)) {
            ++stampsEnabledCounts;
        }
        return stampsEnabledCounts;

    }

    /**
     * Shows the annotation toolbar.
     */
    public void show() {
        show(START_MODE_NORMAL_TOOLBAR, null, null, false);
    }

    /**
     * Shows the annotation toolbar.
     *
     * @param mode The mode that annotation toolbar should start with. Possible values are
     *             {@link AnnotationToolbar#START_MODE_NORMAL_TOOLBAR},
     *             {@link AnnotationToolbar#START_MODE_EDIT_TOOLBAR}
     */
    public void show(int mode) {
        show(mode, null, null, false);
    }

    /**
     * Shows the annotation toolbar.
     *
     * @param mode                 The mode that annotation toolbar should start with. Possible values are
     *                             {@link AnnotationToolbar#START_MODE_NORMAL_TOOLBAR},
     *                             {@link AnnotationToolbar#START_MODE_EDIT_TOOLBAR}
     * @param inkAnnot             The ink annotation if the mode is {@link AnnotationToolbar#START_MODE_EDIT_TOOLBAR}
     *                             and the tool is Ink
     * @param toolMode             The tool mode that annotation toolbar should start with
     * @param dismissAfterExitEdit If it is true and the mode is {@link AnnotationToolbar#START_MODE_EDIT_TOOLBAR},
     *                             the regular annotation toolbar shouldn't be shown when the edit
     *                             toolbar is dismissed
     */
    public void show(int mode, Annot inkAnnot, ToolMode toolMode, boolean dismissAfterExitEdit) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        mForceUpdateView = true;

        if (getWidth() > 0 && getHeight() > 0) {
            initViews();
        }

        if (mode == START_MODE_EDIT_TOOLBAR) {
            if (toolMode != null) {
                mDismissAfterExitEdit = dismissAfterExitEdit;
                showEditToolbar(toolMode, inkAnnot);
                // update which item in the group items (e.g. line, arrow, polyline)
                // should be shown in annotation toolbar
                ToolManager.Tool tool = mToolManager.createTool(toolMode, null);
                updateVisibleAnnotType(tool.getCreateAnnotType());
            }
        } else {
            updateButtonsVisibility();
            showAnnotationToolbar();
        }

        if (getVisibility() != VISIBLE) {
            Transition slide = new Slide(Gravity.TOP).setDuration(ANIMATION_DURATION);
            TransitionManager.beginDelayedTransition((ViewGroup) getParent(), slide);
            setVisibility(View.VISIBLE);
        }

        if (mAnnotationToolbarListener != null) {
            mAnnotationToolbarListener.onAnnotationToolbarShown();
        }

        mShouldExpand = PdfViewCtrlSettingsManager.getDoubleRowToolbarInUse(context);
        if ((mShouldExpand && !mIsExpanded) || (!mShouldExpand && mIsExpanded)) {
            updateExpanded(getResources().getConfiguration().orientation);
        }

        if (toolMode != null && mode != START_MODE_EDIT_TOOLBAR) {
            mSelectedToolId = getResourceIdOfTool(toolMode);
        }

        if (mSelectedToolId != -1) {
            selectTool(null, mSelectedToolId);
            mSelectedToolId = -1;
        }

        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_ANNOTATION_TOOLBAR_OPEN);
    }

    /**
     * Specifies whether the button should stay down or return to pan tool after an annotation is created.
     *
     * @param value True if the button should stay down after an annotation is created
     */
    public void setButtonStayDown(boolean value) {
        mButtonStayDown = value;
    }

    /**
     * The overloaded implementation of {@link View#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mOverflowPopupWindow != null && mOverflowPopupWindow.isShowing()) {
            mOverflowPopupWindow.dismiss();
        }
        if (mStampStatePopup != null && mStampStatePopup.isShowing()) {
            mStampStatePopup.dismiss();
        }
        updateExpanded(newConfig.orientation);
        mForceUpdateView = true;
    }

    /**
     * The overloaded implementation of {@link FrameLayout#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (getWidth() == 0 || getHeight() == 0) {
            mLayoutChanged = false;
            return;
        }

        if (mForceUpdateView && !changed) {
            mForceUpdateView = false;
            // in case layout still being modified i.e. dragging in multi-window mode
            // we still need to update the button size and background
            initViews();
        }

        if (changed) {
            mForceUpdateView = false;
            initViews();
            // layout changed to true
            if (!mLayoutChanged) {
                updateButtonsVisibility();
                initSelectedButton();
            }
        }
        mLayoutChanged = changed;
    }

    /**
     * Closes the annotation toolbar.
     */
    public void close() {
        closePopups();

        if (isInEditMode()) {
            mEditToolbarImpl.close();
            setBackgroundColor(mToolbarBackgroundColor); // revert back the toolbar background color
            return;
        }

        if (mToolManager == null) {
            return;
        }

        mToolManager.onClose();
        reset();
        ((Tool) mToolManager.getTool()).setForceSameNextToolMode(false);

        Transition slide = new Slide(Gravity.TOP).setDuration(ANIMATION_DURATION);
        slide.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                if (mAnnotationToolbarListener != null) {
                    mAnnotationToolbarListener.onAnnotationToolbarClosed();
                }
            }

            @Override
            public void onTransitionCancel(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionPause(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionResume(@NonNull Transition transition) {

            }
        });

        TransitionManager.beginDelayedTransition((ViewGroup) getParent(), slide);

        setVisibility(View.GONE);

        saveVisibleAnnotTypes();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_ANNOTATION_TOOLBAR_OPEN);
    }

    private void reset() {
        if (mToolManager == null || mPdfViewCtrl == null) {
            return;
        }
        mToolManager.setTool(mToolManager.createTool(ToolMode.PAN, null));
        selectButton(getResourceIdOfTool(ToolMode.PAN));

        ((Tool) mToolManager.getTool()).setForceSameNextToolMode(mButtonStayDown);
        mPdfViewCtrl.clearSelection();

        mDismissAfterExitEdit = false;
    }

    private void showAnnotationToolbar() {
        Transition slide = new Slide(Gravity.TOP).setDuration(ANIMATION_DURATION);
        TransitionManager.beginDelayedTransition((ViewGroup) getParent(), slide);
        findViewById(R.id.controls_annotation_toolbar_state_normal).setVisibility(VISIBLE);
    }

    private void hideAnnotationToolbar() {
        findViewById(R.id.controls_annotation_toolbar_state_normal).setVisibility(GONE);
    }

    private void selectButton(int id) {
        mSelectedButtonId = id;
        for (View view : mButtons) {
            if (view.getId() == id) {
                view.setSelected(true);
            } else {
                view.setSelected(false);
            }
        }
        if (mToolManager.isSkipNextTapEvent()) {
            // if we previously closed a popup without clicking viewer
            // let's clear the flag
            mToolManager.resetSkipNextTapEvent();
        }
    }

    private void updateButtonsVisibility() {
        Context context = getContext();
        if (context == null || mToolManager == null || mPdfViewCtrl == null) {
            return;
        }

        boolean hasAllTools = hasAllTool();
        int visibility = hasAllTools ? View.VISIBLE : View.GONE;
        findViewById(R.id.controls_annotation_toolbar_tool_text_squiggly).setVisibility(visibility);
        findViewById(R.id.controls_annotation_toolbar_tool_text_strikeout).setVisibility(visibility);
        findViewById(R.id.controls_annotation_toolbar_tool_eraser).setVisibility(visibility);
        findViewById(R.id.controls_annotation_toolbar_tool_free_highlighter).setVisibility(visibility);
        findViewById(R.id.controls_annotation_toolbar_tool_multi_select).setVisibility(visibility);
        // combine tools into a single tool
        for (GroupedItem item : mGroupItems) {
            for (int buttonId : item.getButtonIds()) {
                findViewById(buttonId).setVisibility(GONE);
            }
            if (hasAllTools || item.mPrefKey.equals(PREF_KEY_TEXT)) {
                // show one of the tools from each group when showing all tools
                // show freetext group when not showing all tools
                int visibleButtonId = item.getVisibleButtonId();
                if (visibleButtonId != -1) {
                    findViewById(visibleButtonId).setVisibility(VISIBLE);
                }
            }
        }

        for (View button : mButtons) {
            int viewId = button.getId();
            ToolMode toolMode = ToolConfig.getInstance().getToolModeByAnnotationToolbarItemId(viewId);
            if (toolMode != null && mToolManager.isToolModeDisabled(toolMode)) {
                button.setVisibility(GONE);
            }
        }

        if (getStampsEnabledCount() == 0) {
            findViewById(R.id.controls_annotation_toolbar_tool_stamp).setVisibility(GONE);
        }

        if (mPdfViewCtrl != null && mPdfViewCtrl.isUndoRedoEnabled()) {
            findViewById(R.id.controls_annotation_toolbar_btn_more).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.controls_annotation_toolbar_btn_more).setVisibility(View.GONE);
        }
    }

    private boolean hasAllTool() {
        Context context = getContext();
        return context != null && (Utils.isTablet(context) || mIsExpanded || (Utils.isLandscape(context) && getWidth() > Utils.getRealScreenHeight(context)));
    }

    /**
     * Closes the popup windows.
     */
    public void closePopups() {
        if (mAnnotStyleDialog != null) {
            mAnnotStyleDialog.dismiss();
            mAnnotStyleDialog = null;
        }
        if (mStampStatePopup != null && mStampStatePopup.isShowing()) {
            mStampStatePopup.dismiss();
        }
        if (mOverflowPopupWindow != null && mOverflowPopupWindow.isShowing()) {
            mOverflowPopupWindow.dismiss();
        }
    }

    private void initButtons() {
        Context context = getContext();
        if (context == null || mToolManager == null || mPdfViewCtrl == null) {
            return;
        }

        updateStampBtnState();

        mButtons = new ArrayList<>();
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_text_highlight));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_text_underline));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_stickynote));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_text_squiggly));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_text_strikeout));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_free_highlighter));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_stamp)); // by default signature stamp
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_image_stamper));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_rubber_stamper));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_line));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_arrow));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_ruler));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_polyline));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_freehand));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_eraser));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_freetext));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_callout));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_rectangle));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_oval));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_polygon));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_cloud));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_multi_select));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_tool_pan));
        mButtons.add(findViewById(R.id.controls_annotation_toolbar_btn_close));
        if (mPdfViewCtrl != null && mPdfViewCtrl.isUndoRedoEnabled()) {
            mButtons.add(findViewById(R.id.controls_annotation_toolbar_btn_more));
        }

        View.OnClickListener clickListener = getButtonsClickListener();
        for (View view : mButtons) {
            if (view != null) {
                view.setOnClickListener(clickListener);
                TooltipCompat.setTooltipText(view, view.getContentDescription());
                if (Utils.isNougat()) {
                    view.setOnGenericMotionListener(new OnGenericMotionListener() {
                        @Override
                        public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                            Context context = getContext();
                            if (context == null) {
                                return false;
                            }

                            if (view.isShown() && Utils.isNougat()) {
                                mToolManager.onChangePointerIcon(PointerIcon.getSystemIcon(context, PointerIcon.TYPE_HAND));
                            }
                            return true;
                        }
                    });
                }
            }
        }
    }

    private void updateStampBtnState() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        try {
            int imageResId;
            if (mIsExpanded) { // signature should have its own icon
                imageResId = R.drawable.ic_annotation_signature_black_24dp;
            } else {
                switch (mStampState) {
                    case STATE_SIGNATURE:
                        imageResId = R.drawable.ic_annotation_signature_black_24dp;
                        break;
                    case STATE_RUBBER_STAMP:
                        imageResId = R.drawable.ic_annotation_stamp_black_24dp;
                        break;
                    case STATE_IMAGE_STAMP:
                        imageResId = R.drawable.ic_annotation_image_black_24dp;
                        break;
                    default:
                        return;
                }
            }
            int iconColor = mToolbarToolIconColor;
            Drawable drawable = Utils.createImageDrawableSelector(context, imageResId, iconColor);
            ((AppCompatImageButton) findViewById(R.id.controls_annotation_toolbar_tool_stamp)).setImageDrawable(drawable);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private void initSelectedButton() {
        if (mToolManager == null) {
            return;
        }

        // Let's make the button selected according to the current tool
        ToolMode toolMode = ToolManager.getDefaultToolMode(mToolManager.getTool().getToolMode());
        selectButton(getResourceIdOfTool(toolMode));
    }

    private int getResourceIdOfTool(ToolMode toolMode) {
        switch (toolMode) {
            case LINE_CREATE:
                return R.id.controls_annotation_toolbar_tool_line;
            case ARROW_CREATE:
                return R.id.controls_annotation_toolbar_tool_arrow;
            case RULER_CREATE:
                return R.id.controls_annotation_toolbar_tool_ruler;
            case POLYLINE_CREATE:
                return R.id.controls_annotation_toolbar_tool_polyline;
            case RECT_CREATE:
                return R.id.controls_annotation_toolbar_tool_rectangle;
            case OVAL_CREATE:
                return R.id.controls_annotation_toolbar_tool_oval;
            case POLYGON_CREATE:
                return R.id.controls_annotation_toolbar_tool_polygon;
            case CLOUD_CREATE:
                return R.id.controls_annotation_toolbar_tool_cloud;
            case INK_ERASER:
                return R.id.controls_annotation_toolbar_tool_eraser;
            case TEXT_ANNOT_CREATE:
                return R.id.controls_annotation_toolbar_tool_stickynote;
            case TEXT_CREATE:
                return R.id.controls_annotation_toolbar_tool_freetext;
            case CALLOUT_CREATE:
                return R.id.controls_annotation_toolbar_tool_callout;
            case TEXT_UNDERLINE:
                return R.id.controls_annotation_toolbar_tool_text_underline;
            case TEXT_HIGHLIGHT:
                return R.id.controls_annotation_toolbar_tool_text_highlight;
            case TEXT_SQUIGGLY:
                return R.id.controls_annotation_toolbar_tool_text_squiggly;
            case TEXT_STRIKEOUT:
                return R.id.controls_annotation_toolbar_tool_text_strikeout;
            case FREE_HIGHLIGHTER:
                return R.id.controls_annotation_toolbar_tool_free_highlighter;
            case ANNOT_EDIT_RECT_GROUP:
                return R.id.controls_annotation_toolbar_tool_multi_select;
            case SIGNATURE:
                return R.id.controls_annotation_toolbar_tool_stamp;
            case STAMPER:
                // when only one row of tools are displayed, we use
                // controls_annotation_toolbar_tool_stamp as the only
                // resource for all signature/stamper/rubber stamper annotations.
                // when two rows of tools are displayed, each annotation has
                // its own resource.
                return mIsExpanded ? R.id.controls_annotation_toolbar_tool_image_stamper
                    : R.id.controls_annotation_toolbar_tool_stamp;
            case RUBBER_STAMPER:
                return mIsExpanded ? R.id.controls_annotation_toolbar_tool_rubber_stamper
                    : R.id.controls_annotation_toolbar_tool_stamp;
            case PAN:
            default:
                return R.id.controls_annotation_toolbar_tool_pan;
        }
    }

    private View.OnClickListener getButtonsClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View view) {
                selectTool(view, view.getId());
            }
        };
    }

    /**
     * Select a tool.
     *
     * @param view The view
     * @param id   The id of the tool to be selected.
     */
    public void selectTool(View view, int id) {
        Context context = getContext();
        if (context == null || mToolManager == null) {
            return;
        }

        ToolMode annotMode = ToolManager.getDefaultToolMode(mToolManager.getTool().getToolMode());
        if (Utils.isAnnotationHandlerToolMode(annotMode) ||
            annotMode == ToolMode.TEXT_CREATE ||
            annotMode == ToolMode.CALLOUT_CREATE ||
            annotMode == ToolMode.PAN) {
            mToolManager.onClose();
        }

        // Because the Controls is a library project, we can't use
        // R.id... as a constant value in a switch-case, so we need
        // to chain if-else instead.

        View button = findViewById(id);
        int annotType = getAnnotTypeFromButtonId(id);

        ToolMode toolMode = null;
        int analyticId = -1;
        Set<String> whiteListFonts = null;
        if (id == R.id.controls_annotation_toolbar_tool_line) {
            toolMode = ToolMode.LINE_CREATE;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_LINE;
        } else if (id == R.id.controls_annotation_toolbar_tool_arrow) {
            toolMode = ToolMode.ARROW_CREATE;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_ARROW;
        } else if (id == R.id.controls_annotation_toolbar_tool_ruler) {
            toolMode = ToolMode.RULER_CREATE;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_RULER;
        } else if (id == R.id.controls_annotation_toolbar_tool_polyline) {
            toolMode = ToolMode.POLYLINE_CREATE;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_POLYLINE;
        } else if (id == R.id.controls_annotation_toolbar_tool_rectangle) {
            toolMode = ToolMode.RECT_CREATE;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_RECTANGLE;
        } else if (id == R.id.controls_annotation_toolbar_tool_oval) {
            toolMode = ToolMode.OVAL_CREATE;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_OVAL;
        } else if (id == R.id.controls_annotation_toolbar_tool_polygon) {
            toolMode = ToolMode.POLYGON_CREATE;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_POLYGON;
        } else if (id == R.id.controls_annotation_toolbar_tool_cloud) {
            toolMode = ToolMode.CLOUD_CREATE;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_CLOUD;
        } else if (id == R.id.controls_annotation_toolbar_tool_eraser) {
            toolMode = ToolMode.INK_ERASER;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_ERASER;
        } else if (id == R.id.controls_annotation_toolbar_tool_free_highlighter) {
            toolMode = ToolMode.FREE_HIGHLIGHTER;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_FREE_HIGHLIGHTER;
        } else if (id == R.id.controls_annotation_toolbar_tool_stickynote) {
            toolMode = ToolMode.TEXT_ANNOT_CREATE;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_STICKY_NOTE;
        } else if (id == R.id.controls_annotation_toolbar_tool_freetext) {
            toolMode = ToolMode.TEXT_CREATE;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_FREE_TEXT;
            whiteListFonts = mToolManager.getFreeTextFonts();
        } else if (id == R.id.controls_annotation_toolbar_tool_callout) {
            toolMode = ToolMode.CALLOUT_CREATE;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_CALLOUT;
            whiteListFonts = mToolManager.getFreeTextFonts();
        } else if (id == R.id.controls_annotation_toolbar_tool_text_highlight) {
            toolMode = ToolMode.TEXT_HIGHLIGHT;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_HIGHLIGHT;
        } else if (id == R.id.controls_annotation_toolbar_tool_text_underline) {
            toolMode = ToolMode.TEXT_UNDERLINE;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_UNDERLINE;
        } else if (id == R.id.controls_annotation_toolbar_tool_text_squiggly) {
            toolMode = ToolMode.TEXT_SQUIGGLY;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_SQUIGGLY;
        } else if (id == R.id.controls_annotation_toolbar_tool_text_strikeout) {
            toolMode = ToolMode.TEXT_STRIKEOUT;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_STRIKEOUT;
        }
        boolean sendAnalytics = mSelectedButtonId != id;
        if (toolMode != null) {
            if (mSelectedButtonId == id) {
                AnnotStyle annotStyle = getCustomAnnotStyle(annotType);
                if (annotStyle == null) {
                    return;
                }
                AnnotStyleDialogFragment popupWindow = new AnnotStyleDialogFragment.Builder(annotStyle)
                    .setAnchorView(button)
                    .setMoreAnnotTypes(getMoreAnnotTypes(annotType))
                    .setWhiteListFont(whiteListFonts)
                    .build();
                showAnnotPropertyPopup(popupWindow, view, analyticId);
            }
            mToolManager.setTool(mToolManager.createTool(toolMode, mToolManager.getTool()));
            ToolManager.Tool tool = mToolManager.getTool();
            ((Tool) tool).setForceSameNextToolMode(mButtonStayDown);
            selectButton(id);
            mEventAction = true;
            if (tool instanceof AdvancedShapeCreate) {
                ((AdvancedShapeCreate) tool).setOnEditToolbarListener(this);
            }
        } else if (id == R.id.controls_annotation_toolbar_tool_freehand) {
            showEditToolbar(ToolMode.INK_CREATE, null);
            mEventAction = true;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_FREEHAND;
        } else if (id == R.id.controls_annotation_toolbar_tool_stamp
            || id == R.id.controls_annotation_toolbar_tool_image_stamper
            || id == R.id.controls_annotation_toolbar_tool_rubber_stamper) {
            // only show popup containing the other tools if the image stamper is included.
            // Otherwise, there is no popup.
            if (getStampsEnabledCount() >= 2) {
                boolean imageStamperEnabled = !mToolManager.isToolModeDisabled(STAMPER);
                boolean rubberStamperEnabled = !mToolManager.isToolModeDisabled(RUBBER_STAMPER);
                if (!mIsExpanded && (imageStamperEnabled || rubberStamperEnabled)) {
                    if (mSelectedButtonId == id) {
                        showStampStatePopup(id, view);
                    }
                }
            }
            if (id == R.id.controls_annotation_toolbar_tool_stamp && (mIsExpanded || STATE_SIGNATURE.equals(mStampState))) {
                analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_SIGNATURE;
                mToolManager.setTool(mToolManager.createTool(SIGNATURE, mToolManager.getTool()));
                ((Tool) mToolManager.getTool()).setForceSameNextToolMode(mButtonStayDown);
                selectButton(id);
                mEventAction = true;
            } else if (id == R.id.controls_annotation_toolbar_tool_image_stamper
                || (!mIsExpanded && STATE_IMAGE_STAMP.equals(mStampState))) {
                analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_STAMP;
                mToolManager.setTool(mToolManager.createTool(STAMPER, mToolManager.getTool()));
                ((Tool) mToolManager.getTool()).setForceSameNextToolMode(mButtonStayDown);
                selectButton(id);
                mEventAction = true;
            } else if (id == R.id.controls_annotation_toolbar_tool_rubber_stamper
                || (!mIsExpanded && STATE_RUBBER_STAMP.equals(mStampState))) {
                analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_RUBBER_STAMP;
                mToolManager.setTool(mToolManager.createTool(RUBBER_STAMPER, mToolManager.getTool()));
                ((Tool) mToolManager.getTool()).setForceSameNextToolMode(mButtonStayDown);
                selectButton(id);
                mEventAction = true;
            }
        } else if (id == R.id.controls_annotation_toolbar_tool_multi_select) {
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_MULTI_SELECT;
            mToolManager.setTool(mToolManager.createTool(ToolMode.ANNOT_EDIT_RECT_GROUP, null)); // reset
            ((Tool) mToolManager.getTool()).setForceSameNextToolMode(mButtonStayDown);
            selectButton(id);
            mEventAction = true;
        } else if (id == R.id.controls_annotation_toolbar_tool_pan) {
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_PAN;
            mToolManager.setTool(mToolManager.createTool(ToolMode.PAN, null)); // reset
            selectButton(id);
        } else if (id == R.id.controls_annotation_toolbar_btn_close) {
            sendAnalytics = false;
            close();
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_ANNOTATION_TOOLBAR_CLOSE,
                AnalyticsParam.noActionParam(mEventAction));
            mEventAction = false;
        } else if (id == R.id.controls_annotation_toolbar_btn_more) {
            if (mOnUndoRedoListener != null) {
                if (mOverflowPopupWindow != null && mOverflowPopupWindow.isShowing()) {
                    mOverflowPopupWindow.dismiss();
                }
                mOverflowPopupWindow = new AnnotToolbarOverflowPopupWindow(context, mToolManager.getUndoRedoManger(), mOnUndoRedoListener, this);
                try {
                    mOverflowPopupWindow.showAsDropDown(view);
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                }
            }
            sendAnalytics = false;
        }

        if (sendAnalytics) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATION_TOOLBAR, AnalyticsParam.annotationToolbarParam(analyticId));
        }
    }

    private void saveVisibleAnnotTypes() {
        Context context = getContext();
        if (context == null || mVisibleAnnotTypeMap == null) {
            return;
        }

        JSONObject object = new JSONObject();
        for (Map.Entry<String, Integer> entry : mVisibleAnnotTypeMap.entrySet()) {
            try {
                object.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        PdfViewCtrlSettingsManager.setAnnotToolbarVisibleAnnotTypes(context, object.toString());
    }

    /**
     * The overloaded implementation of {@link ToolManager.ToolChangedListener#toolChanged(ToolManager.Tool, ToolManager.Tool)}.
     */
    @Override
    public void toolChanged(ToolManager.Tool newTool, ToolManager.Tool oldTool) {

        if (newTool == null || !isShowing()) {
            return;
        }

        boolean canSelectButton = false;

        if (oldTool != null && oldTool instanceof Tool && newTool instanceof Tool) {
            Tool oldT = (Tool) oldTool;
            Tool newT = (Tool) newTool;
            canSelectButton = !oldT.isForceSameNextToolMode() || !newT.isEditAnnotTool();
        }

        if (canSelectButton) {
            ToolMode newToolMode = ToolManager.getDefaultToolMode(newTool.getToolMode());
            // The new tool might be grouped and not visible, so should update
            // group items and update buttons visibility
            ToolManager.Tool tool = mToolManager.createTool(newToolMode, null);
            updateVisibleAnnotType(tool.getCreateAnnotType());
            updateButtonsVisibility();
            selectButton(getResourceIdOfTool(newToolMode));
            ToolManager.ToolModeBase toolModeBase = newTool.getToolMode();

            if (ToolMode.SIGNATURE.equals(toolModeBase)) {
                mStampState = STATE_SIGNATURE;
            } else if (RUBBER_STAMPER.equals(toolModeBase)) {
                mStampState = STATE_RUBBER_STAMP;
            } else if (STAMPER.equals(toolModeBase)) {
                mStampState = STATE_IMAGE_STAMP;
            }
            // if it is not in expand mode then for all signature, image stamp
            // and rubber stamp there is only one button id
            if (mSelectedButtonId == R.id.controls_annotation_toolbar_tool_stamp) {
                updateStampBtnState();
            }
        }

        if (newTool instanceof AdvancedShapeCreate) {
            ((AdvancedShapeCreate) newTool).setOnEditToolbarListener(this);
        }
    }

    private void showAnnotPropertyPopup(final AnnotStyleDialogFragment popupWindow, View view, final int annotToolId) {
        if (view == null || popupWindow == null) {
            return;
        }
        if (mToolManager.isSkipNextTapEvent()) {
            mToolManager.resetSkipNextTapEvent();
            return;
        }

        if (mAnnotStyleDialog != null) {
            // prev style picker is not closed yet
            return;
        }
        mAnnotStyleDialog = popupWindow;

        popupWindow.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mAnnotStyleDialog = null;

                Context context = getContext();
                if (context == null || mToolManager == null) {
                    return;
                }

                AnnotStyle annotStyle = popupWindow.getAnnotStyle();
                ToolStyleConfig.getInstance().saveAnnotStyle(context, annotStyle, "");


                Tool tool = (Tool) mToolManager.getTool();
                if (tool != null) {
                    tool.setupAnnotProperty(annotStyle);
                }
            }
        });
        popupWindow.setOnMoreAnnotTypesClickListener(new AnnotStyleView.OnMoreAnnotTypeClickedListener() {
            @Override
            public void onAnnotTypeClicked(int annotType) {
                Context context = getContext();
                if (context == null) {
                    return;
                }

                popupWindow.saveAnnotStyles();
                ToolStyleConfig.getInstance().saveAnnotStyle(context, popupWindow.getAnnotStyle(), "");
                updateAnnotStyleDialog(popupWindow, annotType);
            }
        });

        FragmentActivity activity = null;
        if (getContext() instanceof FragmentActivity) {
            activity = (FragmentActivity) getContext();
        } else if (mToolManager.getCurrentActivity() != null) {
            activity = mToolManager.getCurrentActivity();
        }
        if (activity == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("AnnotationToolbar is not attached to with an Activity"));
            return;
        }
        popupWindow.show(activity.getSupportFragmentManager(), AnalyticsHandlerAdapter.STYLE_PICKER_LOC_ANNOT_TOOLBAR,
            AnalyticsHandlerAdapter.getInstance().getAnnotationTool(annotToolId));
    }

    private void updateAnnotStyleDialog(AnnotStyleDialogFragment dialog, int annotType) {
        AnnotStyle previousAnnotStyle = dialog.getAnnotStyle();
        int previousAnnotType = previousAnnotStyle.getAnnotType();
        AnnotStyle annotStyle = getCustomAnnotStyle(annotType);
        if (annotStyle == null) {
            return;
        }
        dialog.setAnnotStyle(annotStyle);
        int nextToolId = getButtonIdFromAnnotType(annotType);
        View nextToolButton = findViewById(nextToolId);
        View previousToolButton = findViewById(getButtonIdFromAnnotType(previousAnnotType));
        if (nextToolButton != null && previousToolButton != null && previousToolButton.getVisibility() == VISIBLE) {
            previousToolButton.setVisibility(GONE);
            nextToolButton.setVisibility(VISIBLE);
        }
        updateVisibleAnnotType(annotType);
        selectTool(null, nextToolId);
    }

    private void updateVisibleAnnotType(
        int annotType) {

        if (mVisibleAnnotTypeMap == null) {
            return;
        }
        for (GroupedItem item : mGroupItems) {
            if (item.contains(annotType)) {
                mVisibleAnnotTypeMap.put(item.mPrefKey, annotType);
            }
        }

    }

    private int getButtonIdFromAnnotType(int annotType) {
        int indexAtValue = mButtonAnnotTypeMap.indexOfValue(annotType);
        if (indexAtValue > -1) {
            return mButtonAnnotTypeMap.keyAt(indexAtValue);
        }
        return -1;
    }

    private int getAnnotTypeFromButtonId(int buttonId) {
        return mButtonAnnotTypeMap.get(buttonId);
    }

    @Nullable
    private ArrayList<Integer> getMoreAnnotTypes(int annotType) {
        for (GroupedItem item : mGroupItems) {
            if (item.contains(annotType)) {
                return item.getAvailableAnnotTypes();
            }
        }
        return null;
    }

    private void showStampStatePopup(final int id, final View view) {
        Context context = getContext();
        if (context == null || view == null || mToolManager == null || getStampsEnabledCount() < 2) {
            return;
        }

        if (mStampStatePopup == null) {
            mStampStatePopup = new StampStatePopup(context, mToolManager, mStampState, mToolbarBackgroundColor, mToolbarToolIconColor);
            updateStampPopupSize();
        } else {
            mStampStatePopup.updateView(mStampState);
        }

        mStampStatePopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                Context context = getContext();
                if (context == null || mToolManager == null) {
                    return;
                }

                String currentStampState = mStampState;
                mStampState = mStampStatePopup.getStampState();
                if (mStampState == null) {
                    return;
                }
                checkStampState();
                updateStampBtnState();

                SharedPreferences settings = Tool.getToolPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(Tool.ANNOTATION_TOOLBAR_SIGNATURE_STATE, mStampState);
                editor.apply();
                int analyticsId = 0;
                boolean differentState = !mStampState.equals(currentStampState);
                switch (mStampState) {
                    case STATE_SIGNATURE:
                        mToolManager.setTool(mToolManager.createTool(SIGNATURE, mToolManager.getTool()));
                        ((Tool) mToolManager.getTool()).setForceSameNextToolMode(mButtonStayDown);
                        selectButton(id);
                        analyticsId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_SIGNATURE;
                        mEventAction = true;
                        break;
                    case STATE_IMAGE_STAMP:
                        mToolManager.setTool(mToolManager.createTool(STAMPER, mToolManager.getTool()));
                        ((Tool) mToolManager.getTool()).setForceSameNextToolMode(mButtonStayDown);
                        selectButton(id);
                        analyticsId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_STAMP;
                        mEventAction = true;
                        break;
                    case STATE_RUBBER_STAMP:
                        mToolManager.setTool(mToolManager.createTool(RUBBER_STAMPER, mToolManager.getTool()));
                        ((Tool) mToolManager.getTool()).setForceSameNextToolMode(mButtonStayDown);
                        selectButton(id);
                        analyticsId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_RUBBER_STAMP;
                        mEventAction = true;
                        break;
                }
                if (differentState) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATION_TOOLBAR,
                        AnalyticsParam.annotationToolbarParam(analyticsId));
                } else {
                    mToolManager.skipNextTapEvent();
                }
            }
        });
        mStampStatePopup.showAsDropDown(view);
    }

    private void updateStampPopupSize() {

        if (mStampStatePopup != null) {
            mStampStatePopup.setWidth(getToolWidth());
            mStampStatePopup.setHeight(getToolHeight() * (getStampsEnabledCount() - 1));
        }

    }

    /**
     * Sets the {@link AnnotationToolbarListener} listener.
     *
     * @param listener The listener
     */
    public void setAnnotationToolbarListener(AnnotationToolbarListener listener) {
        mAnnotationToolbarListener = listener;
    }

    /**
     * Sets the {@link OnUndoRedoListener} listener.
     *
     * @param listener The listener
     */
    @SuppressWarnings("unused")
    public void setOnUndoRedoListener(OnUndoRedoListener listener) {
        mOnUndoRedoListener = listener;
    }

    /**
     * @return True if the annotation toolbar is visible
     */
    public boolean isShowing() {
        return getVisibility() == View.VISIBLE;
    }

    /**
     * @return True if the ink toolbar is visible
     */
    @SuppressWarnings("unused")
    public boolean isInEditMode() {
        return mEditToolbarImpl != null && mEditToolbarImpl.isToolbarShown();
    }

    /**
     * Handles the shortcuts key in the annotation toolbar.
     *
     * @param keyCode the key code
     * @param event   the key event
     * @return true if it is handled; false otherwise
     */
    // Note: we wouldn't override onKeyUp event here because it only gets called if the view is
    // focused, but we don't want this view gets focused since then the PDFViewCtrl will not
    // receive key events.
    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        Context context = getContext();
        if (context == null || mToolManager == null) {
            return false;
        }

        if (isInEditMode()) {
            return mEditToolbarImpl.handleKeyUp(keyCode, event);
        }

        Tool tool = (Tool) mToolManager.getTool();
        if (tool == null) {
            return false;
        }

        if (findViewById(R.id.controls_annotation_toolbar_tool_pan).isShown()
            && !(tool instanceof Pan)
            && ShortcutHelper.isCancelTool(keyCode, event)) {
            closePopups();
            selectTool(null, R.id.controls_annotation_toolbar_tool_pan);
            return true;
        }

        if (findViewById(R.id.controls_annotation_toolbar_btn_close).isShown()
            && ShortcutHelper.isCloseMenu(keyCode, event)) {
            closePopups();
            selectTool(null, R.id.controls_annotation_toolbar_btn_close);
            return true;
        }

        int mode = START_MODE_UNKNOWN;
        mSelectedToolId = -1;

        if (ShortcutHelper.isHighlightAnnot(keyCode, event)) {
            mode = START_MODE_NORMAL_TOOLBAR;
            mSelectedToolId = R.id.controls_annotation_toolbar_tool_text_highlight;
        }

        if (ShortcutHelper.isUnderlineAnnot(keyCode, event)) {
            mode = START_MODE_NORMAL_TOOLBAR;
            mSelectedToolId = R.id.controls_annotation_toolbar_tool_text_underline;
        }

        if (ShortcutHelper.isStrikethroughAnnot(keyCode, event)) {
            mode = START_MODE_NORMAL_TOOLBAR;
            mSelectedToolId = R.id.controls_annotation_toolbar_tool_text_strikeout;
        }

        if (ShortcutHelper.isSquigglyAnnot(keyCode, event)) {
            mode = START_MODE_NORMAL_TOOLBAR;
            mSelectedToolId = R.id.controls_annotation_toolbar_tool_text_squiggly;
        }

        if (ShortcutHelper.isTextboxAnnot(keyCode, event)) {
            mode = START_MODE_NORMAL_TOOLBAR;
            mSelectedToolId = R.id.controls_annotation_toolbar_tool_freetext;
        }

        if (ShortcutHelper.isCommentAnnot(keyCode, event)) {
            mode = START_MODE_NORMAL_TOOLBAR;
            mSelectedToolId = R.id.controls_annotation_toolbar_tool_stickynote;
        }

        if (ShortcutHelper.isRectangleAnnot(keyCode, event)) {
            mode = START_MODE_NORMAL_TOOLBAR;
            mSelectedToolId = R.id.controls_annotation_toolbar_tool_rectangle;
        }

        if (ShortcutHelper.isOvalAnnot(keyCode, event)) {
            mode = START_MODE_NORMAL_TOOLBAR;
            mSelectedToolId = R.id.controls_annotation_toolbar_tool_oval;
        }

        if (ShortcutHelper.isDrawAnnot(keyCode, event)) {
            mode = START_MODE_NORMAL_TOOLBAR;
            mSelectedToolId = R.id.controls_annotation_toolbar_tool_freehand;
        }

        if (findViewById(R.id.controls_annotation_toolbar_tool_eraser).isShown()
            && ShortcutHelper.isEraserAnnot(keyCode, event)) {
            // don't let start with eraser when eraser will not be shown in the toolbar.
            mode = START_MODE_NORMAL_TOOLBAR;
            mSelectedToolId = R.id.controls_annotation_toolbar_tool_eraser;
        }

        if (ShortcutHelper.isLineAnnot(keyCode, event)) {
            mode = START_MODE_NORMAL_TOOLBAR;
            mSelectedToolId = R.id.controls_annotation_toolbar_tool_line;
        }

        if (ShortcutHelper.isArrowAnnot(keyCode, event)) {
            mode = START_MODE_NORMAL_TOOLBAR;
            mSelectedToolId = R.id.controls_annotation_toolbar_tool_arrow;
        }

        if (ShortcutHelper.isSignatureAnnot(keyCode, event) && !mToolManager.isToolModeDisabled(SIGNATURE)) {
            mode = START_MODE_NORMAL_TOOLBAR;
            mStampState = STATE_SIGNATURE;
            checkStampState();
            updateStampBtnState();
            mSelectedToolId = R.id.controls_annotation_toolbar_tool_stamp;
        }

        if (ShortcutHelper.isImageAnnot(keyCode, event) && !mToolManager.isToolModeDisabled(STAMPER)) {
            mode = START_MODE_NORMAL_TOOLBAR;
            mStampState = STATE_IMAGE_STAMP;
            checkStampState();
            updateStampBtnState();
            if (mIsExpanded) {
                mSelectedToolId = R.id.controls_annotation_toolbar_tool_image_stamper;
            } else {
                mSelectedToolId = R.id.controls_annotation_toolbar_tool_stamp;
            }
        }

        if (mode != START_MODE_UNKNOWN) {
            closePopups();
            if (!isShowing()) {
                if (mAnnotationToolbarListener != null) {
                    mAnnotationToolbarListener.onShowAnnotationToolbarByShortcut(mode);
                }
            } else {
                selectTool(null, mSelectedToolId);
            }
            return true;
        }

        return false;
    }

    @Nullable
    private AnnotStyle getCustomAnnotStyle(int annotType) {
        Context context = getContext();
        if (context == null) {
            return null;
        }

        return ToolStyleConfig.getInstance().getCustomAnnotStyle(context, annotType, "");
    }

    /**
     * Whether this annotation toolbar is in expanded mode
     *
     * @return true if expanded, false otherwise
     */
    public boolean isExpanded() {
        return mIsExpanded;
    }

    /**
     * Toggles the expanded mode in annotation toolbar
     */
    public void toggleExpanded() {
        mShouldExpand = !mShouldExpand;
        PdfViewCtrlSettingsManager.updateDoubleRowToolbarInUse(getContext(), mShouldExpand);
        updateExpanded(getResources().getConfiguration().orientation);
    }

    /**
     * Updates the expanded mode
     */
    private void updateExpanded(int orientation) {
        Context context = getContext();
        if (context == null || mToolManager == null) {
            return;
        }

        mIsExpanded = mShouldExpand && orientation == Configuration.ORIENTATION_PORTRAIT && !Utils.isTablet(context);

        if (!mIsExpanded) {
            if (mSelectedButtonId == R.id.controls_annotation_toolbar_tool_image_stamper) {
                mStampState = STATE_IMAGE_STAMP;
            } else if (mSelectedButtonId == R.id.controls_annotation_toolbar_tool_rubber_stamper) {
                mStampState = STATE_RUBBER_STAMP;
            }
        } else {
            mStampState = STATE_SIGNATURE;
        }
        checkStampState();

        ViewGroup root = findViewById(R.id.controls_annotation_toolbar_state_normal);
        int nextLayoutRes = mIsExpanded ? R.layout.controls_annotation_toolbar_expanded_layout
            : R.layout.controls_annotation_toolbar_collapsed_layout;

        // calculate next layout height
        int height;
        if (!mIsExpanded) {
            int[] attrs = new int[]{R.attr.actionBarSize};
            TypedArray ta = context.obtainStyledAttributes(attrs);
            try {
                height = ta.getDimensionPixelSize(0, (int) Utils.convDp2Pix(context, 56));
            } finally {
                ta.recycle();
            }
        } else {
            height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        View nextLayout = LayoutInflater.from(context).inflate(nextLayoutRes, null);
        nextLayout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

        // transition animation
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(new ChangeBounds());
        transitionSet.addTransition(new Fade());
        TransitionManager.beginDelayedTransition((ViewGroup) getParent(), transitionSet);
        root.removeViewAt(0);
        root.addView(nextLayout);

        initButtons();
        updateButtonsVisibility();
        initSelectedButton();
    }

    /**
     * The overloaded implementation of {@link AdvancedShapeCreate.OnEditToolbarListener#showEditToolbar(ToolMode, Annot)}.
     */
    @Override
    public void showEditToolbar(
        @NonNull ToolMode toolMode,
        @Nullable Annot inkAnnot) {

        FragmentActivity activity = mToolManager.getCurrentActivity();
        if (activity == null || isInEditMode()) {
            return;
        }
        setBackgroundColor(0); // not show toolbar background when starts with edit mode
        hideAnnotationToolbar();
        EditToolbar editToolbar = findViewById(R.id.controls_annotation_toolbar_state_edit);
        mEditToolbarImpl = new EditToolbarImpl(activity, editToolbar, mToolManager, toolMode, inkAnnot, mShouldExpand);
        mEditToolbarImpl.setOnEditToolbarListener(this);
        mEditToolbarImpl.showToolbar();

    }

    /**
     * The overloaded implementation of {@link AdvancedShapeCreate.OnEditToolbarListener#closeEditToolbar()}.
     */
    @Override
    public void closeEditToolbar() {

        mEditToolbarImpl.close();

    }

    /**
     * The overloaded implementation of {@link EditToolbarImpl.OnEditToolbarListener#onEditToolbarDismissed()}.
     */
    @Override
    public void onEditToolbarDismissed() {

        if (mToolManager == null) {
            return;
        }

        setBackgroundColor(mToolbarBackgroundColor); // revert back the toolbar background color
        if (mDismissAfterExitEdit) {
            close();
        } else {
            showAnnotationToolbar();
        }
        ToolManager.Tool tool = mToolManager.getTool();
        if (tool == null) {
            return;
        }
        ToolMode toolMode = ToolManager.getDefaultToolMode(tool.getToolMode());
        if (toolMode == INK_CREATE) {
            mToolManager.setTool(mToolManager.createTool(ToolMode.PAN, null));
            selectButton(R.id.controls_annotation_toolbar_tool_pan);
        } else {
            // otherwise stay in the same tool
            mToolManager.setTool(mToolManager.createTool(toolMode, tool));
            selectTool(null, getResourceIdOfTool(toolMode));
        }

    }

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface AnnotationToolbarListener {
        /**
         * Called when the annotation toolbar has been shown.
         */
        void onAnnotationToolbarShown();

        /**
         * Called when the annotation toolbar has been closed.
         */
        void onAnnotationToolbarClosed();

        /**
         * The implementation should show the annotation toolbar starting with the certain mode.
         * The listener may do additional checks such as checking whether the document is read-only
         * or has write access before showing the annotation toolbar.
         *
         * @param mode The mode that annotation toolbar should start with. Possible values are
         *             {@link AnnotationToolbar#START_MODE_NORMAL_TOOLBAR},
         *             {@link AnnotationToolbar#START_MODE_EDIT_TOOLBAR}
         */
        void onShowAnnotationToolbarByShortcut(final int mode);
    }

    private class GroupedItem {
        String mPrefKey;
        int[] mAnnotTypes;

        GroupedItem(String prefKey, int[] annotTypes) {
            this.mPrefKey = prefKey;
            this.mAnnotTypes = annotTypes;
        }

        ArrayList<Integer> getButtonIds() {
            ArrayList<Integer> buttonIds = new ArrayList<>();
            for (int annotType : mAnnotTypes) {
                buttonIds.add(getButtonIdFromAnnotType(annotType));
            }
            return buttonIds;
        }

        ArrayList<Integer> getAvailableAnnotTypes() {
            ArrayList<Integer> result = new ArrayList<>();
            for (int annotType : mAnnotTypes) {
                int buttonId = getButtonIdFromAnnotType(annotType);
                ToolMode toolMode = ToolConfig.getInstance().getToolModeByAnnotationToolbarItemId(buttonId);
                if (mToolManager != null && !mToolManager.isToolModeDisabled(toolMode)) {
                    result.add(annotType);
                }
            }
            return result;
        }

        int getVisibleButtonId() {
            if (mVisibleAnnotTypeMap.containsKey(mPrefKey)) {
                int annotType = mVisibleAnnotTypeMap.get(mPrefKey);
                int buttonId = getButtonIdFromAnnotType(annotType);
                ToolMode toolMode = ToolConfig.getInstance().getToolModeByAnnotationToolbarItemId(buttonId);
                if (!mToolManager.isToolModeDisabled(toolMode)) {
                    return buttonId;
                }
            }
            ArrayList<Integer> annotTypes = getAvailableAnnotTypes();
            if (annotTypes == null || annotTypes.isEmpty()) {
                return -1;
            }
            int firstAnnotType = annotTypes.get(0);
            return getButtonIdFromAnnotType(firstAnnotType);
        }

        boolean contains(int annotType) {
            for (int type : mAnnotTypes) {
                if (type == annotType) {
                    return true;
                }
            }
            return false;
        }

    }

    private void checkStampState() {

        if (STATE_RUBBER_STAMP.equals(mStampState)
            && mToolManager.isToolModeDisabled(RUBBER_STAMPER)) {
            AnalyticsHandlerAdapter.getInstance().sendException(
                new Exception("rubber stamper is selected while it is disabled"));
            mStampState = STATE_SIGNATURE;
        }
        if (STATE_IMAGE_STAMP.equals(mStampState)
            && mToolManager.isToolModeDisabled(STAMPER)) {
            AnalyticsHandlerAdapter.getInstance().sendException(
                new Exception("image stamper is selected while it is disabled"));
            mStampState = STATE_SIGNATURE;
        }
        if (STATE_SIGNATURE.equals(mStampState)
            && mToolManager.isToolModeDisabled(SIGNATURE)) {
            AnalyticsHandlerAdapter.getInstance().sendException(
                new Exception("signature is selected while it is disabled"));
            mStampState = STATE_IMAGE_STAMP;
        }

    }

    private int getToolWidth() {

        Context context = getContext();
        if (context == null) {
            return 0;
        }

        int numIcons = NUM_PHONE_NORMAL_STATE_ICONS;
        if (Utils.isLandscape(context) || Utils.isTablet(context)) {
            numIcons = NUM_TABLET_NORMAL_STATE_ICONS;
        }
        int width = getWidth() / numIcons;
        View panBtn = findViewById(R.id.controls_annotation_toolbar_tool_pan);
        if (mIsExpanded && panBtn != null) {
            panBtn.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            width = panBtn.getMeasuredWidth();
        }

        return width;

    }

    private int getToolHeight() {

        int height = getHeight();
        View panBtn = findViewById(R.id.controls_annotation_toolbar_tool_pan);
        if (mIsExpanded && panBtn != null) {
            panBtn.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            height = panBtn.getMeasuredHeight();
        }

        return height;

    }

}
