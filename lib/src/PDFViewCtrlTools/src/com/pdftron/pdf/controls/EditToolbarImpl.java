//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.View;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.tools.AdvancedShapeCreate;
import com.pdftron.pdf.tools.FreehandCreate;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static com.pdftron.pdf.tools.ToolManager.ToolMode.CLOUD_CREATE;
import static com.pdftron.pdf.tools.ToolManager.ToolMode.INK_CREATE;
import static com.pdftron.pdf.tools.ToolManager.ToolMode.POLYGON_CREATE;
import static com.pdftron.pdf.tools.ToolManager.ToolMode.POLYLINE_CREATE;

/**
 * This class is implementing the logic for {@link EditToolbar} to create/edit annotations.
 */
public class EditToolbarImpl implements
    OnToolbarStateUpdateListener,
    OnToolSelectedListener {

    private static final String INK_TAG_1 = "ink_tag_1";
    private static final String INK_TAG_2 = "ink_tag_2";
    private static final String INK_TAG_3 = "ink_tag_3";
    private static final String INK_TAG_4 = "ink_tag_4";
    private static final String INK_TAG_5 = "ink_tag_5";

    private WeakReference<FragmentActivity> mActivityRef;
    private EditToolbar mEditToolbar;
    private ToolManager mToolManager;
    private PDFViewCtrl mPdfViewCtrl;
    private ToolManager.ToolMode mStartToolMode;
    private ArrayList<AnnotStyle> mDrawStyles = new ArrayList<>();
    private AnnotStyle mEraserStyle;
    private boolean mIsStyleFixed;

    private OnEditToolbarListener mOnEditToolbarListener;

    /**
     * Callback interface invoked when the edit toolbar is dismissed.
     */
    public interface OnEditToolbarListener {

        /**
         * Called when the edit toolbar has been dismissed.
         */
        void onEditToolbarDismissed();

    }

    /**
     * Class constructor
     *
     * @param activity     The activity which is used for showing a popup window dialog
     * @param editToolbar  The edit toolbar view
     * @param toolManager  The tool manager
     * @param toolMode     The tool mode which the toolbar should start with
     * @param editAnnot    The annotation to be edited
     * @param shouldExpand Specify whether the toolbar should be expanded
     *                     when phone is in portrait mode
     */
    @SuppressWarnings("WeakerAccess")
    public EditToolbarImpl(@NonNull FragmentActivity activity,
                           @NonNull EditToolbar editToolbar,
                           @NonNull ToolManager toolManager,
                           @NonNull ToolManager.ToolMode toolMode,
                           @Nullable Annot editAnnot,
                           boolean shouldExpand) {
        mActivityRef = new WeakReference<>(activity);
        mEditToolbar = editToolbar;
        mToolManager = toolManager;
        mPdfViewCtrl = mToolManager.getPDFViewCtrl();
        mStartToolMode = toolMode;

        mEditToolbar.setVisibility(View.GONE);

        boolean hasEraseBtn = false;
        initTool(toolMode);
        if (toolMode == INK_CREATE) {
            if (editAnnot != null) {
                mIsStyleFixed = true;
                mDrawStyles.add(getAnnotStyleFromAnnot(editAnnot));
                ((FreehandCreate) mToolManager.getTool()).setInitInkItem(editAnnot);
            } else {
                for (int i = 0; i < 5; ++i) {
                    AnnotStyle annotStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(activity, Annot.e_Ink, getInkTag(i));
                    mDrawStyles.add(annotStyle);
                }
            }
            mEraserStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(activity, AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER, "");
            hasEraseBtn = true;

            ((FreehandCreate) mToolManager.getTool()).setOnToolbarStateUpdateListener(this);
        } else if (toolMode == POLYLINE_CREATE || toolMode == POLYGON_CREATE || toolMode == CLOUD_CREATE) {
            AnnotStyle annotStyle;
            switch (toolMode) {
                case POLYLINE_CREATE:
                    annotStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(activity, Annot.e_Polyline, "");
                    break;
                case POLYGON_CREATE:
                    annotStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(activity, Annot.e_Polygon, "");
                    break;
                case CLOUD_CREATE:
                default:
                    annotStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(activity, AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD, "");
                    break;
            }
            mDrawStyles.add(annotStyle);
            ((AdvancedShapeCreate) mToolManager.getTool()).setOnToolbarStateUpdateListener(this);
        }

        mEditToolbar.setup(mPdfViewCtrl, this, mDrawStyles,
            true, hasEraseBtn, true, shouldExpand, mIsStyleFixed);
        updateToolbarControlButtons();
        if (!mDrawStyles.isEmpty()) {
            updateAnnotProperties(mDrawStyles.get(0));
        }
    }

    /**
     * make the edit toolbar visible
     */
    @SuppressWarnings("WeakerAccess")
    public void showToolbar() {
        mEditToolbar.show();
    }

    /**
     * @return True if the edit toolbar is shown
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isToolbarShown() {
        return mEditToolbar.isShown();
    }

    private boolean startWith(ToolManager.ToolMode toolMode) {
        if (mToolManager == null) {
            return false;
        }
        if (mStartToolMode == toolMode) {
            if (mStartToolMode != mToolManager.getTool().getToolMode()) {
                initTool(mStartToolMode);
            }
            return true;
        }

        return false;
    }

    private boolean startWithClickBasedAnnot() {
        if (mToolManager == null) {
            return false;
        }
        if (mStartToolMode == POLYLINE_CREATE || mStartToolMode == POLYGON_CREATE || mStartToolMode == CLOUD_CREATE) {
            if (mStartToolMode != mToolManager.getTool().getToolMode()) {
                initTool(mStartToolMode);
            }
            return true;
        }

        return false;
    }

    private AnnotStyle getAnnotStyleFromAnnot(Annot annot) {
        if (mToolManager == null || mPdfViewCtrl == null) {
            return null;
        }

        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            // color
            ColorPt colorPt = annot.getColorAsRGB();
            int color = Utils.colorPt2color(colorPt);

            // opacity
            Markup m = new Markup(annot);
            float opacity = (float) m.getOpacity();

            // thickness
            float thickness = (float) annot.getBorderStyle().getWidth();

            final AnnotStyle annotStyle = new AnnotStyle();
            annotStyle.setAnnotType(annot.getType());
            // AnnotStyle should store the real color of the annotation and not the post processed color
            annotStyle.setStyle(color, Color.TRANSPARENT, thickness, opacity);
            return annotStyle;
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        return null;
    }

    /**
     * The overloaded implementation of {@link OnToolSelectedListener#onDrawSelected(int, boolean, View)} }
     */
    @Override
    public void onDrawSelected(int drawIndex,
                               boolean wasSelectedBefore,
                               View anchor) {
        if (mToolManager == null) {
            return;
        }

        AnnotStyle annotStyle = mDrawStyles.get(drawIndex);
        if (annotStyle != null) {
            if (!mIsStyleFixed && wasSelectedBefore) {
                AnnotStyleDialogFragment popupWindow = new AnnotStyleDialogFragment.Builder(annotStyle).setAnchorView(anchor).build();
                if (startWith(INK_CREATE)) {
                    showAnnotPropertyPopup(popupWindow, drawIndex, getInkTag(drawIndex), AnalyticsHandlerAdapter.ANNOTATION_TOOL_FREEHAND);
                } else if (startWith(POLYLINE_CREATE)) {
                    showAnnotPropertyPopup(popupWindow, drawIndex, "", AnalyticsHandlerAdapter.ANNOTATION_TOOL_POLYLINE);
                } else if (startWith(POLYGON_CREATE)) {
                    showAnnotPropertyPopup(popupWindow, drawIndex, "", AnalyticsHandlerAdapter.ANNOTATION_TOOL_POLYGON);
                } else if (startWith(CLOUD_CREATE)) {
                    showAnnotPropertyPopup(popupWindow, drawIndex, "", AnalyticsHandlerAdapter.ANNOTATION_TOOL_CLOUD);
                }
            }
            updateAnnotProperties(annotStyle);
        }

        if (mToolManager.isSkipNextTapEvent()) {
            // if we previously closed a popup without clicking viewer
            // let's clear the flag
            mToolManager.resetSkipNextTapEvent();
        }
    }

    /**
     * The overloaded implementation of {@link OnToolSelectedListener#onClearSelected()} }
     */
    @Override
    public void onClearSelected() {
        if (mToolManager == null) {
            return;
        }

        if (startWith(INK_CREATE)) {
            ((FreehandCreate) mToolManager.getTool()).clearStrokes();
        } else if (startWithClickBasedAnnot()) {
            ((AdvancedShapeCreate) mToolManager.getTool()).clear();
        }

        updateToolbarControlButtons();
    }

    /**
     * The overloaded implementation of {@link OnToolSelectedListener#onEraserSelected(boolean, View)} }
     */
    @Override
    public void onEraserSelected(boolean wasSelectedBefore,
                                 View anchor) {
        if (mToolManager == null) {
            return;
        }
        if (startWith(INK_CREATE)) {
            if (wasSelectedBefore && mEraserStyle != null) {
                AnnotStyleDialogFragment popupWindow = new AnnotStyleDialogFragment.Builder(mEraserStyle).setAnchorView(anchor).build();
                showInkEraserAnnotPropertyPopup(popupWindow);
            }
        }

        if (mToolManager.isSkipNextTapEvent()) {
            // if we previously closed a popup without clicking viewer
            // let's clear the flag
            mToolManager.resetSkipNextTapEvent();
        }

        updateInkEraserAnnotProperties();
    }

    /**
     * The overloaded implementation of {@link OnToolSelectedListener#onUndoSelected()} }
     */
    @Override
    public void onUndoSelected() {
        if (mToolManager == null) {
            return;
        }

        if (startWith(INK_CREATE)) {
            ((FreehandCreate) mToolManager.getTool()).undoStroke();
        } else if (startWithClickBasedAnnot()) {
            ((AdvancedShapeCreate) mToolManager.getTool()).undo();
        }

        updateToolbarControlButtons();
    }

    /**
     * The overloaded implementation of {@link OnToolSelectedListener#onRedoSelected()} }
     */
    @Override
    public void onRedoSelected() {
        if (mToolManager == null) {
            return;
        }

        if (startWith(INK_CREATE)) {
            ((FreehandCreate) mToolManager.getTool()).redoStroke();
        } else if (startWithClickBasedAnnot()) {
            ((AdvancedShapeCreate) mToolManager.getTool()).redo();
        }

        updateToolbarControlButtons();
    }

    /**
     * The overloaded implementation of {@link OnToolSelectedListener#onCloseSelected()} }
     */
    @Override
    public void onCloseSelected() {
        if (mToolManager == null || mEditToolbar == null) {
            return;
        }

        if (startWith(INK_CREATE)) {
            ((FreehandCreate) mToolManager.getTool()).commitAnnotation();
        } else if (startWithClickBasedAnnot()) {
            ((AdvancedShapeCreate) mToolManager.getTool()).commit();
        }

        mEditToolbar.setVisibility(View.GONE);
        if (mOnEditToolbarListener != null) {
            mOnEditToolbarListener.onEditToolbarDismissed();
        }
    }

    /**
     * Called when the state of edit toolbar should be updated.
     */
    @Override
    public void onToolbarStateUpdated() {
        updateToolbarControlButtons();
    }

    /**
     * Commits the changes and closes the Edit toolbar
     */
    public void close() {
        onCloseSelected();
    }

    private void updateToolbarControlButtons() {
        if (mToolManager == null) {
            return;
        }
        boolean canClear = false, canErase = false, canUndo = false, canRedo = false;

        if (startWith(INK_CREATE)) {
            canClear = ((FreehandCreate) mToolManager.getTool()).canEraseStroke();
            canUndo = ((FreehandCreate) mToolManager.getTool()).canUndoStroke();
            canRedo = ((FreehandCreate) mToolManager.getTool()).canRedoStroke();
            canErase = ((FreehandCreate) mToolManager.getTool()).canEraseStroke();
        } else if (startWithClickBasedAnnot()) {
            canClear = ((AdvancedShapeCreate) mToolManager.getTool()).canClear();
            canUndo = ((AdvancedShapeCreate) mToolManager.getTool()).canUndo();
            canRedo = ((AdvancedShapeCreate) mToolManager.getTool()).canRedo();
        }

        mEditToolbar.updateControlButtons(canClear, canErase, canUndo, canRedo);
    }

    private void initTool(ToolManager.ToolMode toolMode) {
        if (mToolManager.getTool().getToolMode() != toolMode) {
            mToolManager.setTool(mToolManager.createTool(toolMode, mToolManager.getTool()));
            if (toolMode == INK_CREATE) {
                ((FreehandCreate) mToolManager.getTool()).setMultiStrokeMode(true);
                ((FreehandCreate) mToolManager.getTool()).setTimedModeEnabled(false);
            }
        }
    }

    private void showAnnotPropertyPopup(@NonNull final AnnotStyleDialogFragment popupWindow,
                                        final int drawIndex,
                                        final String extraTag,
                                        int analyticsScreenId) {
        FragmentActivity activity = mActivityRef.get();
        if (activity == null || mToolManager == null) {
            return;
        }

        if (mToolManager.isSkipNextTapEvent()) {
            mToolManager.resetSkipNextTapEvent();
            return;
        }

        popupWindow.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (mToolManager == null || mPdfViewCtrl == null) {
                    return;
                }
                Context context = mPdfViewCtrl.getContext();
                if (context == null) {
                    return;
                }

                AnnotStyle annotStyle = popupWindow.getAnnotStyle();
                updateAnnotProperties(annotStyle);
                ToolStyleConfig.getInstance().saveAnnotStyle(context, annotStyle, extraTag);

                mDrawStyles.set(drawIndex, annotStyle);
                mEditToolbar.updateDrawStyles(mDrawStyles);
            }
        });
        popupWindow.setOnAnnotStyleChangeListener(new AnnotStyle.OnAnnotStyleChangeListener() {
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

            }

            @Override
            public void onChangeAnnotStrokeColor(int color) {
                mEditToolbar.updateDrawColor(drawIndex, color);
            }

            @Override
            public void onChangeAnnotFillColor(int color) {

            }

            @Override
            public void onChangeAnnotIcon(String icon) {

            }

            @Override
            public void onChangeAnnotFont(FontResource font) {

            }

            @Override
            public void onChangeRulerProperty(RulerItem rulerItem) {

            }
        });
        popupWindow.show(activity.getSupportFragmentManager(),
            AnalyticsHandlerAdapter.STYLE_PICKER_LOC_ANNOT_TOOLBAR,
            AnalyticsHandlerAdapter.getInstance().getAnnotationTool(analyticsScreenId));
    }

    private void showInkEraserAnnotPropertyPopup(@NonNull final AnnotStyleDialogFragment popupWindow) {
        FragmentActivity activity = mActivityRef.get();
        if (activity == null || mToolManager == null) {
            return;
        }

        if (mToolManager.isSkipNextTapEvent()) {
            mToolManager.resetSkipNextTapEvent();
            return;
        }

        popupWindow.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (mToolManager == null || mPdfViewCtrl == null) {
                    return;
                }
                Context context = mPdfViewCtrl.getContext();
                if (context == null) {
                    return;
                }

                AnnotStyle annotStyle = popupWindow.getAnnotStyle();
                ToolStyleConfig.getInstance().saveAnnotStyle(context, annotStyle, "");
                float thickness = annotStyle.getThickness();
                Tool tool = (Tool) mToolManager.getTool();
                if (tool instanceof FreehandCreate) {
                    ((FreehandCreate) tool).setupEraserProperty(thickness);
                }

                mEraserStyle = annotStyle;
            }
        });
        popupWindow.show(activity.getSupportFragmentManager(),
            AnalyticsHandlerAdapter.STYLE_PICKER_LOC_ANNOT_TOOLBAR,
            AnalyticsHandlerAdapter.getInstance().getAnnotationTool(AnalyticsHandlerAdapter.ANNOTATION_TOOL_ERASER));
    }

    private void updateAnnotProperties(AnnotStyle annotStyle) {
        if (mToolManager == null || annotStyle == null) {
            return;
        }

        int color = annotStyle.getColor();
        int fill = annotStyle.getFillColor();
        float thickness = annotStyle.getThickness();
        float opacity = annotStyle.getOpacity();
        String icon = annotStyle.getIcon();
        String pdftronFontName = annotStyle.getPDFTronFontName();
        ((Tool) mToolManager.getTool()).setupAnnotProperty(color, opacity, thickness, fill, icon, pdftronFontName);
    }

    private void updateInkEraserAnnotProperties() {
        if (mToolManager == null || mEraserStyle == null) {
            return;
        }

        if (startWith(INK_CREATE)) {
            float thickness = mEraserStyle.getThickness();
            ((FreehandCreate) mToolManager.getTool()).setupEraserProperty(thickness);
        }
    }

    private String getInkTag(int inkIndex) {
        switch (inkIndex) {
            case 0:
                return INK_TAG_1;
            case 1:
                return INK_TAG_2;
            case 2:
                return INK_TAG_3;
            case 3:
                return INK_TAG_4;
            case 4:
                return INK_TAG_5;
        }
        return "";
    }

    /**
     * Handles the shortcuts key in the edit toolbar.
     *
     * @param keyCode the key code
     * @param event   the key event
     * @return true if it is handled; false otherwise
     */
    @SuppressWarnings("WeakerAccess")
    public boolean handleKeyUp(int keyCode,
                               KeyEvent event) {
        return mEditToolbar.handleKeyUp(keyCode, event);
    }

    /**
     * Sets listener to {@link OnEditToolbarListener}.
     *
     * @param listener The {@link OnEditToolbarListener} listener
     */
    @SuppressWarnings("WeakerAccess")
    public void setOnEditToolbarListener(OnEditToolbarListener listener) {
        mOnEditToolbarListener = listener;
    }
}
