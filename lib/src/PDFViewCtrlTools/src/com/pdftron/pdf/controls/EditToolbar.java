//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.transition.Slide;
import android.support.transition.Transition;
import android.support.transition.TransitionManager;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;

import java.util.ArrayList;

/**
 * The EditToolbar allows to edit/create an annotation such as freehand, polyline, polygon and cloud
 * using a list of buttons provided in the toolbar.
 * This class includes only the UI (known as View) and the logic (known as View-Model) that works
 * with a {@link com.pdftron.pdf.tools.ToolManager} is implemented in {@link EditToolbarImpl}.
 */
public class EditToolbar extends InsectHandlerToolbar {

    private static final int ANIMATION_DURATION = 250;

    private PDFViewCtrl mPdfViewCtrl;
    private ArrayList<View> mButtons;
    private int mSelectedButtonId;
    private ArrayList<AnnotStyle> mDrawStyles;
    private boolean mLayoutChanged;
    private boolean mForceUpdateView;
    private int mSelectedToolId = -1;
    boolean mHasClearButton;
    boolean mHasEraserButton;
    boolean mHasUndoRedoButtons;
    private int mBackgroundColor;
    private int mToolBackgroundColor;
    private int mToolIconColor;
    private int mCloseIconColor;
    private boolean mShouldExpand; // should the toolbar be expanded when phone is in portrait mode
    private boolean mIsExpanded; // is the toolbar in expanded mode
    private boolean mIsStyleFixed;

    private OnToolSelectedListener mOnToolSelectedListener;

    /**
     * Class constructor
     */
    public EditToolbar(@NonNull Context context) {
        super(context);
        init(context, null, R.attr.edit_toolbar, R.style.EditToolbarStyle);
    }

    /**
     * Class constructor
     */
    public EditToolbar(@NonNull Context context,
                       @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, R.attr.edit_toolbar, R.style.EditToolbarStyle);
    }

    /**
     * Class constructor
     */
    public EditToolbar(@NonNull Context context,
                       @Nullable AttributeSet attrs,
                       int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.EditToolbarStyle);
    }

    /**
     * Class constructor
     */
    @SuppressWarnings("unused")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public EditToolbar(@NonNull Context context,
                       @Nullable AttributeSet attrs,
                       int defStyleAttr,
                       int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(@NonNull Context context,
                      AttributeSet attrs,
                      int defStyleAttr,
                      int defStyleRes) {
        // initialize colors
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.EditToolbar, defStyleAttr, defStyleRes);
        try {
            mBackgroundColor = typedArray.getColor(R.styleable.AnnotationToolbar_colorBackground, Color.BLACK);
            mToolBackgroundColor = typedArray.getColor(R.styleable.AnnotationToolbar_colorToolBackground, Color.BLACK);
            mToolIconColor = typedArray.getColor(R.styleable.AnnotationToolbar_colorToolIcon, Color.WHITE);
            mCloseIconColor = typedArray.getColor(R.styleable.AnnotationToolbar_colorCloseIcon, Color.WHITE);
        } finally {
            typedArray.recycle();
        }

        LayoutInflater.from(context).inflate(R.layout.controls_edit_toolbar_collapsed_layout, this);
    }

    /**
     * Setups the draw toolbar window.
     *
     * @param pdfViewCtrl            The PDFViewCtrl class used for obtaining post process color
     * @param onToolSelectedListener A listener to be called when a tool is selected
     * @param drawStyles             Styles for drawing
     * @param hasClearButton         Specify whether the toolbar can clear drawings
     * @param hasEraserButton        Specify whether the toolbar can erase a drawing
     * @param hasUndoRedoButtons     Specify whether the toolbar can undo/redo
     * @param shouldExpanded         Specify whether the toolbar should be expanded
     *                               when phone is in portrait mode
     */
    public void setup(@NonNull PDFViewCtrl pdfViewCtrl,
                      OnToolSelectedListener onToolSelectedListener,
                      @NonNull ArrayList<AnnotStyle> drawStyles,
                      boolean hasClearButton,
                      boolean hasEraserButton,
                      boolean hasUndoRedoButtons,
                      boolean shouldExpanded,
                      boolean isStyleFixed) {
        mPdfViewCtrl = pdfViewCtrl;
        mOnToolSelectedListener = onToolSelectedListener;
        mDrawStyles = drawStyles;
        mHasClearButton = hasClearButton;
        mHasEraserButton = hasEraserButton;
        mHasUndoRedoButtons = hasUndoRedoButtons;
        mShouldExpand = shouldExpanded;
        mIsStyleFixed = isStyleFixed;

        mSelectedButtonId = R.id.controls_edit_toolbar_tool_style1;
        updateExpanded(getResources().getConfiguration().orientation);
    }

    /**
     * Shows the edit toolbar.
     */
    public void show() {
        if (getWidth() != 0) {
            // otherwise the layout has not been yet set, wait for onLayout
            initViews();
            updateButtonsVisibility();
        }

        if (getVisibility() != VISIBLE) {
            Transition slide = new Slide(Gravity.TOP).setDuration(ANIMATION_DURATION);
            TransitionManager.beginDelayedTransition((ViewGroup) getParent(), slide);
            setVisibility(VISIBLE);
        }

        if (mSelectedToolId != -1) {
            selectTool(mSelectedToolId);
            mSelectedToolId = -1;
        }
    }

    private void updateButtonsVisibility() {
        if (mDrawStyles == null) {
            return;
        }
        boolean allVisible = canShowAllDrawButtons();
        int visibility;
        visibility = mDrawStyles.size() > 0 ? VISIBLE : GONE;
        findViewById(R.id.controls_edit_toolbar_tool_style1).setVisibility(visibility);
        visibility = allVisible && mDrawStyles.size() > 1 ? VISIBLE : GONE;
        findViewById(R.id.controls_edit_toolbar_tool_style2).setVisibility(visibility);
        visibility = allVisible && mDrawStyles.size() > 2 ? VISIBLE : GONE;
        findViewById(R.id.controls_edit_toolbar_tool_style3).setVisibility(visibility);
        visibility = allVisible && mDrawStyles.size() > 3 ? VISIBLE : GONE;
        findViewById(R.id.controls_edit_toolbar_tool_style4).setVisibility(visibility);
        visibility = allVisible && mDrawStyles.size() > 4 ? VISIBLE : GONE;
        findViewById(R.id.controls_edit_toolbar_tool_style5).setVisibility(visibility);

        findViewById(R.id.controls_edit_toolbar_tool_clear).setVisibility(mHasClearButton ? VISIBLE : GONE);
        findViewById(R.id.controls_edit_toolbar_tool_eraser).setVisibility(mHasEraserButton ? VISIBLE : GONE);
        findViewById(R.id.controls_edit_toolbar_tool_undo).setVisibility(mHasUndoRedoButtons ? VISIBLE : GONE);
        findViewById(R.id.controls_edit_toolbar_tool_redo).setVisibility(mHasUndoRedoButtons ? VISIBLE : GONE);
    }

    private boolean canShowAllDrawButtons() {
        Context context = getContext();
        return context != null && (mShouldExpand || Utils.isTablet(context) || (Utils.isLandscape(context) && getWidth() > Utils.getRealScreenHeight(context)));
    }

    /**
     * The overloaded implementation of {@link View#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateExpanded(newConfig.orientation);
        mForceUpdateView = true;
    }

    /**
     * The overloaded implementation of {@link FrameLayout#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    protected void onLayout(boolean changed,
                            int left,
                            int top,
                            int right,
                            int bottom) {
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
            // layout changed to true
            initViews();
            if (!mLayoutChanged) {
                updateButtonsVisibility();
            }
        }
        mLayoutChanged = changed;
    }

    private void initViews() {
        // initialize the color of each tool in the edit toolbar:
        initButtonsIcons();
    }

    private void initButtonsIcons() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        // initialize the background color of the edit toolbar
        setBackgroundColor(mBackgroundColor);

        int size = getResources().getDimensionPixelSize(R.dimen.quick_menu_button_size);

        // initialize the color of each button in the draw toolbar
        BitmapDrawable spinnerBitmapDrawable = ViewerUtils.getBitmapDrawable(context,
            R.drawable.controls_toolbar_spinner_selected, size, size, mToolBackgroundColor, mIsExpanded);
        findViewById(R.id.controls_edit_toolbar_tool_style1).setBackground(getStyleBackground(size, size));
        findViewById(R.id.controls_edit_toolbar_tool_style2).setBackground(getStyleBackground(size, size));
        findViewById(R.id.controls_edit_toolbar_tool_style3).setBackground(getStyleBackground(size, size));
        findViewById(R.id.controls_edit_toolbar_tool_style4).setBackground(getStyleBackground(size, size));
        findViewById(R.id.controls_edit_toolbar_tool_style5).setBackground(getStyleBackground(size, size));
        findViewById(R.id.controls_edit_toolbar_tool_eraser).setBackground(
            ViewerUtils.createBackgroundSelector(spinnerBitmapDrawable));

        Drawable normalBitmapDrawable;
        if (mIsExpanded) {
            normalBitmapDrawable = getResources().getDrawable(R.drawable.rounded_corners);
            normalBitmapDrawable.mutate();
            normalBitmapDrawable.setColorFilter(mToolBackgroundColor, PorterDuff.Mode.SRC_ATOP);
        } else {
            normalBitmapDrawable = ViewerUtils.getBitmapDrawable(context,
                R.drawable.controls_annotation_toolbar_bg_selected_blue,
                size, size, mToolBackgroundColor, false);
        }

        findViewById(R.id.controls_edit_toolbar_tool_clear).setBackground(
            ViewerUtils.createBackgroundSelector(normalBitmapDrawable));
        findViewById(R.id.controls_edit_toolbar_tool_undo).setBackground(
            ViewerUtils.createBackgroundSelector(normalBitmapDrawable));
        findViewById(R.id.controls_edit_toolbar_tool_redo).setBackground(
            ViewerUtils.createBackgroundSelector(normalBitmapDrawable));
        findViewById(R.id.controls_edit_toolbar_tool_close).setBackground(
            ViewerUtils.createBackgroundSelector(normalBitmapDrawable));

        // if RTL, swap the drawable of the undo and redo buttons
        if (Utils.isRtlLayout(context)) {
            ImageButton undo = findViewById(R.id.controls_edit_toolbar_tool_undo);
            ImageButton redo = findViewById(R.id.controls_edit_toolbar_tool_redo);
            Drawable newRedoDrawable = undo.getDrawable();
            Drawable newUndoDrawable = redo.getDrawable();
            redo.setImageDrawable(newRedoDrawable);
            undo.setImageDrawable(newUndoDrawable);
        }

        // initialize the color of the buttons in the draw toolbar
        Drawable drawable = Utils.createImageDrawableSelector(context, R.drawable.ic_delete_black_24dp, mToolIconColor);
        ((AppCompatImageButton) findViewById(R.id.controls_edit_toolbar_tool_clear)).setImageDrawable(drawable);
        drawable = Utils.createImageDrawableSelector(context, R.drawable.ic_annotation_eraser_black_24dp, mToolIconColor);
        ((AppCompatImageButton) findViewById(R.id.controls_edit_toolbar_tool_eraser)).setImageDrawable(drawable);
        drawable = Utils.createImageDrawableSelector(context, R.drawable.ic_undo_black_24dp, mToolIconColor);
        ((AppCompatImageButton) findViewById(R.id.controls_edit_toolbar_tool_undo)).setImageDrawable(drawable);
        drawable = Utils.createImageDrawableSelector(context, R.drawable.ic_redo_black_24dp, mToolIconColor);
        ((AppCompatImageButton) findViewById(R.id.controls_edit_toolbar_tool_redo)).setImageDrawable(drawable);

        // initialize the color of the close button in the ink toolbar
        drawable = Utils.createImageDrawableSelector(context, R.drawable.controls_edit_toolbar_close_24dp, mCloseIconColor);
        ((AppCompatImageButton) findViewById(R.id.controls_edit_toolbar_tool_close)).setImageDrawable(drawable);

        updateDrawButtons();
    }

    @Nullable
    private Drawable getStyleBackground(int width, int height) {
        Context context = getContext();
        if (context == null) {
            return null;
        }

        if (mIsStyleFixed) {
            return new ColorDrawable(getResources().getColor(R.color.tools_quickmenu_button_pressed));
        }
        BitmapDrawable spinnerBitmapDrawable = ViewerUtils.getBitmapDrawable(context,
            R.drawable.controls_toolbar_spinner_selected, width, height, mToolBackgroundColor, mIsExpanded);
        return ViewerUtils.createBackgroundSelector(spinnerBitmapDrawable);
    }

    /**
     * Updates the status of control buttons.
     *
     * @param canClear Specify whether can clear the drawings
     * @param canErase Specify whether can erase a draw
     * @param canUndo  Specify whether can undo the last action
     * @param canRedo  Specify whether can redo the last undo
     */
    public void updateControlButtons(boolean canClear,
                                     boolean canErase,
                                     boolean canUndo,
                                     boolean canRedo) {
        enableButton(R.id.controls_edit_toolbar_tool_clear, canClear);
        enableButton(R.id.controls_edit_toolbar_tool_eraser, canErase);
        enableButton(R.id.controls_edit_toolbar_tool_undo, canUndo);
        enableButton(R.id.controls_edit_toolbar_tool_redo, canRedo);
    }

    private void enableButton(int id,
                              boolean enabled) {
        if (mButtons == null) {
            return;
        }
        for (View view : mButtons) {
            if (view.getId() == id) {
                view.setEnabled(enabled);
                break;
            }
        }
    }

    /**
     * Updates the styles for drawing
     *
     * @param drawStyles The new styles
     */
    public void updateDrawStyles(ArrayList<AnnotStyle> drawStyles) {
        mDrawStyles = drawStyles;
        updateDrawButtons();
    }

    public void updateDrawColor(int styleIndex, int color) {
        switch (styleIndex) {
            case 0:
                updateDrawButtonColor(R.id.controls_edit_toolbar_tool_style1, color);
                break;
            case 1:
                updateDrawButtonColor(R.id.controls_edit_toolbar_tool_style2, color);
                break;
            case 2:
                updateDrawButtonColor(R.id.controls_edit_toolbar_tool_style3, color);
                break;
            case 3:
                updateDrawButtonColor(R.id.controls_edit_toolbar_tool_style4, color);
                break;
            case 4:
                updateDrawButtonColor(R.id.controls_edit_toolbar_tool_style5, color);
                break;
        }
    }

    private void updateDrawButtons() {
        updateDrawButton(R.id.controls_edit_toolbar_tool_style1, 0);
        updateDrawButton(R.id.controls_edit_toolbar_tool_style2, 1);
        updateDrawButton(R.id.controls_edit_toolbar_tool_style3, 2);
        updateDrawButton(R.id.controls_edit_toolbar_tool_style4, 3);
        updateDrawButton(R.id.controls_edit_toolbar_tool_style5, 4);
    }

    private void updateDrawButton(int id,
                                  int styleIndex) {
        if (mDrawStyles == null || mDrawStyles.size() <= styleIndex) {
            return;
        }
        int color = mDrawStyles.get(styleIndex).getColor();
        updateDrawButtonColor(id, color);
    }

    private void updateDrawButtonColor(int id, int color) {
        color = Utils.getPostProcessedColor(mPdfViewCtrl, color);
        ImageButton button = findViewById(id);
        button.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    private void setSelectedButton(int id) {
        mSelectedButtonId = id;
        if (mButtons == null) {
            return;
        }
        for (View view : mButtons) {
            if (view.getId() == id) {
                view.setSelected(true);
            } else {
                view.setSelected(false);
            }
        }
    }

    /**
     * Select a tool.
     *
     * @param id The id of the tool to be selected.
     */
    private void selectTool(int id) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        View button = findViewById(id);

        int drawIndex = getDrawIndex(id);
        int analyticsId = getAnalyticsId(id);
        if (drawIndex >= 0) {
            if (mOnToolSelectedListener != null) {
                mOnToolSelectedListener.onDrawSelected(drawIndex, mSelectedButtonId == id, button);
            }
            setSelectedButton(id);
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_EDIT_TOOLBAR,
                AnalyticsParam.editToolbarParam(analyticsId));
        } else if (id == R.id.controls_edit_toolbar_tool_eraser) {
            if (mOnToolSelectedListener != null) {
                mOnToolSelectedListener.onEraserSelected(mSelectedButtonId == id, button);
            }
            setSelectedButton(id);
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_EDIT_TOOLBAR,
                AnalyticsParam.editToolbarParam(AnalyticsHandlerAdapter.EDIT_TOOLBAR_TOOL_ERASER));
        } else if (id == R.id.controls_edit_toolbar_tool_clear) {
            if (mOnToolSelectedListener != null) {
                mOnToolSelectedListener.onClearSelected();
            }
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_EDIT_TOOLBAR,
                AnalyticsParam.editToolbarParam(AnalyticsHandlerAdapter.EDIT_TOOLBAR_TOOL_CLEAR));
        } else if (id == R.id.controls_edit_toolbar_tool_undo) {
            if (mOnToolSelectedListener != null) {
                mOnToolSelectedListener.onUndoSelected();
            }
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_EDIT_TOOLBAR,
                AnalyticsParam.editToolbarParam(AnalyticsHandlerAdapter.EDIT_TOOLBAR_TOOL_UNDO));
        } else if (id == R.id.controls_edit_toolbar_tool_redo) {
            if (mOnToolSelectedListener != null) {
                mOnToolSelectedListener.onRedoSelected();
            }
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_EDIT_TOOLBAR,
                AnalyticsParam.editToolbarParam(AnalyticsHandlerAdapter.EDIT_TOOLBAR_TOOL_REDO));
        } else if (id == R.id.controls_edit_toolbar_tool_close) {
            if (mSelectedButtonId != id) {
                if (mOnToolSelectedListener != null) {
                    mOnToolSelectedListener.onCloseSelected();
                }
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_EDIT_TOOLBAR,
                    AnalyticsParam.editToolbarParam(AnalyticsHandlerAdapter.EDIT_TOOLBAR_TOOL_CLOSE));
            }
        }
    }

    private int getDrawIndex(int id) {
        if (id == R.id.controls_edit_toolbar_tool_style1) {
            return 0;
        }
        if (id == R.id.controls_edit_toolbar_tool_style2) {
            return 1;
        }
        if (id == R.id.controls_edit_toolbar_tool_style3) {
            return 2;
        }
        if (id == R.id.controls_edit_toolbar_tool_style4) {
            return 3;
        }
        if (id == R.id.controls_edit_toolbar_tool_style5) {
            return 4;
        }

        return -1;
    }

    private int getAnalyticsId(int id) {
        if (id == R.id.controls_edit_toolbar_tool_style1) {
            return AnalyticsHandlerAdapter.EDIT_TOOLBAR_TOOL_PEN_1;
        }
        if (id == R.id.controls_edit_toolbar_tool_style2) {
            return AnalyticsHandlerAdapter.EDIT_TOOLBAR_TOOL_PEN_2;
        }
        if (id == R.id.controls_edit_toolbar_tool_style3) {
            return AnalyticsHandlerAdapter.EDIT_TOOLBAR_TOOL_PEN_3;
        }
        if (id == R.id.controls_edit_toolbar_tool_style4) {
            return AnalyticsHandlerAdapter.EDIT_TOOLBAR_TOOL_PEN_4;
        }
        if (id == R.id.controls_edit_toolbar_tool_style5) {
            return AnalyticsHandlerAdapter.EDIT_TOOLBAR_TOOL_PEN_5;
        }

        return -1;
    }

    /**
     * Handles the shortcuts key in the edit toolbar.
     *
     * @param keyCode the key code
     * @param event   the key event
     * @return true if it is handled; false otherwise
     */
    public boolean handleKeyUp(int keyCode,
                               KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_1
            && findViewById(R.id.controls_edit_toolbar_tool_style1).isShown()
            && ShortcutHelper.isSwitchInk(keyCode, event)) {
            selectTool(R.id.controls_edit_toolbar_tool_style1);
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_2
            && findViewById(R.id.controls_edit_toolbar_tool_style2).isShown()
            && ShortcutHelper.isSwitchInk(keyCode, event)) {
            selectTool(R.id.controls_edit_toolbar_tool_style2);
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_3
            && findViewById(R.id.controls_edit_toolbar_tool_style3).isShown()
            && ShortcutHelper.isSwitchInk(keyCode, event)) {
            selectTool(R.id.controls_edit_toolbar_tool_style3);
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_4
            && findViewById(R.id.controls_edit_toolbar_tool_style4).isShown()
            && ShortcutHelper.isSwitchInk(keyCode, event)) {
            selectTool(R.id.controls_edit_toolbar_tool_style4);
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_5
            && findViewById(R.id.controls_edit_toolbar_tool_style5).isShown()
            && ShortcutHelper.isSwitchInk(keyCode, event)) {
            selectTool(R.id.controls_edit_toolbar_tool_style5);
            return true;
        }

        if (findViewById(R.id.controls_edit_toolbar_tool_eraser).isShown()
            && findViewById(R.id.controls_edit_toolbar_tool_eraser).isEnabled()
            && ShortcutHelper.isEraseInk(keyCode, event)) {
            selectTool(R.id.controls_edit_toolbar_tool_eraser);
            return true;
        }

        if (findViewById(R.id.controls_edit_toolbar_tool_undo).isShown()
            && findViewById(R.id.controls_edit_toolbar_tool_undo).isEnabled()
            && ShortcutHelper.isUndo(keyCode, event)) {
            selectTool(R.id.controls_edit_toolbar_tool_undo);
            return true;
        }

        if (findViewById(R.id.controls_edit_toolbar_tool_redo).isShown()
            && findViewById(R.id.controls_edit_toolbar_tool_redo).isEnabled()
            && ShortcutHelper.isRedo(keyCode, event)) {
            selectTool(R.id.controls_edit_toolbar_tool_redo);
            return true;
        }

        if (findViewById(R.id.controls_edit_toolbar_tool_close).isShown()
            && ShortcutHelper.isCommitDraw(keyCode, event)) {
            selectTool(R.id.controls_edit_toolbar_tool_close);
            return true;
        }

        return false;
    }

    /**
     * Whether this toolbar is in expanded mode
     *
     * @return true if expanded, false otherwise
     */
    boolean isExpanded() {
        return mIsExpanded;
    }

    /**
     * Updates the expanded mode
     */
    private void updateExpanded(int orientation) {
        Context context = getContext();
        if (context == null || mDrawStyles == null) {
            return;
        }

        mIsExpanded = mShouldExpand && mDrawStyles.size() > 1
            && orientation == Configuration.ORIENTATION_PORTRAIT && !Utils.isTablet(context);

        removeAllViews();
        int nextLayoutRes = mIsExpanded ? R.layout.controls_edit_toolbar_expanded_layout
            : R.layout.controls_edit_toolbar_collapsed_layout;
        LayoutInflater.from(context).inflate(nextLayoutRes, this);

        collectButtons();
        updateButtonsVisibility();
        setSelectedButton(mSelectedButtonId);
    }

    private void collectButtons() {
        mButtons = new ArrayList<>();
        mButtons.add(findViewById(R.id.controls_edit_toolbar_tool_style1));
        mButtons.add(findViewById(R.id.controls_edit_toolbar_tool_style2));
        mButtons.add(findViewById(R.id.controls_edit_toolbar_tool_style3));
        mButtons.add(findViewById(R.id.controls_edit_toolbar_tool_style4));
        mButtons.add(findViewById(R.id.controls_edit_toolbar_tool_style5));
        mButtons.add(findViewById(R.id.controls_edit_toolbar_tool_clear));
        mButtons.add(findViewById(R.id.controls_edit_toolbar_tool_eraser));
        mButtons.add(findViewById(R.id.controls_edit_toolbar_tool_redo));
        mButtons.add(findViewById(R.id.controls_edit_toolbar_tool_undo));
        mButtons.add(findViewById(R.id.controls_edit_toolbar_tool_close));

        for (View view : mButtons) {
            if (view != null) {
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        selectTool(view.getId());
                    }
                });
            }
        }
    }
}
