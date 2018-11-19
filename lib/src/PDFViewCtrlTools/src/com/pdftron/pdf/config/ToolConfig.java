//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------
package com.pdftron.pdf.config;


import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnnotUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class for config tools and tool related customize stuffs
 */
public class ToolConfig {

    private static ToolConfig _INSTANCE;

    public static ToolConfig getInstance() {
        if (_INSTANCE == null) {
            _INSTANCE = new ToolConfig();
        }
        return _INSTANCE;
    }

    private SparseArray<ToolMode> mToolQMItemPair;
    private SparseArray<ToolMode> mToolAnnotationToolbarItemPair;
    private ArrayList<Integer> mNoAnnotPermissionQMList;
    private SparseArray<ToolManager.ToolModeBase> mAnnotHandlerToolPair;    // Annotation type and handler tool mode
    private PanLongPressSwitchToolCallback mPanLongPressSwitchToolCallback; //  customized callback when pan tool long press

    private Set<Integer> mDisabledAnnotEditingTypes;

    private ArrayList<FontResource> mFontList;

    private ToolConfig() {
        // quick menu item and tool pair
        mToolQMItemPair = new SparseArray<>();
        mToolQMItemPair.put(R.id.qm_line, ToolMode.LINE_CREATE);
        mToolQMItemPair.put(R.id.qm_arrow, ToolMode.ARROW_CREATE);
        mToolQMItemPair.put(R.id.qm_ruler, ToolMode.RULER_CREATE);
        mToolQMItemPair.put(R.id.qm_polyline, ToolMode.POLYLINE_CREATE);
        mToolQMItemPair.put(R.id.qm_free_text, ToolMode.TEXT_CREATE);
        mToolQMItemPair.put(R.id.qm_callout, ToolMode.CALLOUT_CREATE);
        mToolQMItemPair.put(R.id.qm_sticky_note, ToolMode.TEXT_ANNOT_CREATE);
        mToolQMItemPair.put(R.id.qm_free_hand, ToolMode.INK_CREATE);
        mToolQMItemPair.put(R.id.qm_free_highlighter, ToolMode.FREE_HIGHLIGHTER);
        mToolQMItemPair.put(R.id.qm_floating_sig, ToolMode.SIGNATURE);
        mToolQMItemPair.put(R.id.qm_image_stamper, ToolMode.STAMPER);
        mToolQMItemPair.put(R.id.qm_link, ToolMode.TEXT_LINK_CREATE);
        mToolQMItemPair.put(R.id.qm_rectangle, ToolMode.RECT_CREATE);
        mToolQMItemPair.put(R.id.qm_oval, ToolMode.OVAL_CREATE);
        mToolQMItemPair.put(R.id.qm_sound, ToolMode.SOUND_CREATE);
        mToolQMItemPair.put(R.id.qm_file_attachment, ToolMode.FILE_ATTACHMENT_CREATE);
        mToolQMItemPair.put(R.id.qm_polygon, ToolMode.POLYGON_CREATE);
        mToolQMItemPair.put(R.id.qm_cloud, ToolMode.CLOUD_CREATE);
        mToolQMItemPair.put(R.id.qm_ink_eraser, ToolMode.INK_ERASER);
        mToolQMItemPair.put(R.id.qm_form_text, ToolMode.FORM_TEXT_FIELD_CREATE);
        mToolQMItemPair.put(R.id.qm_form_check_box, ToolMode.FORM_CHECKBOX_CREATE);
        mToolQMItemPair.put(R.id.qm_form_signature, ToolMode.FORM_SIGNATURE_CREATE);
        mToolQMItemPair.put(R.id.qm_highlight, ToolMode.TEXT_HIGHLIGHT);
        mToolQMItemPair.put(R.id.qm_strikeout, ToolMode.TEXT_STRIKEOUT);
        mToolQMItemPair.put(R.id.qm_squiggly, ToolMode.TEXT_SQUIGGLY);
        mToolQMItemPair.put(R.id.qm_underline, ToolMode.TEXT_UNDERLINE);
        mToolQMItemPair.put(R.id.qm_redaction, ToolMode.TEXT_REDACTION);
        mToolQMItemPair.put(R.id.qm_rect_group_select, ToolMode.ANNOT_EDIT_RECT_GROUP);
        mToolQMItemPair.put(R.id.qm_rubber_stamper, ToolMode.RUBBER_STAMPER);

        // annotation toolbar item and tool pair
        mToolAnnotationToolbarItemPair = new SparseArray<>();

        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_line, ToolMode.LINE_CREATE);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_arrow, ToolMode.ARROW_CREATE);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_ruler, ToolMode.RULER_CREATE);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_polyline, ToolMode.POLYLINE_CREATE);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_freetext, ToolMode.TEXT_CREATE);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_callout, ToolMode.CALLOUT_CREATE);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_stickynote, ToolMode.TEXT_ANNOT_CREATE);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_freehand, ToolMode.INK_CREATE);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_free_highlighter, ToolMode.FREE_HIGHLIGHTER);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_rectangle, ToolMode.RECT_CREATE);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_oval, ToolMode.OVAL_CREATE);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_polygon, ToolMode.POLYGON_CREATE);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_cloud, ToolMode.CLOUD_CREATE);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_eraser, ToolMode.INK_ERASER);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_text_highlight, ToolMode.TEXT_HIGHLIGHT);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_text_strikeout, ToolMode.TEXT_STRIKEOUT);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_text_squiggly, ToolMode.TEXT_SQUIGGLY);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_text_underline, ToolMode.TEXT_UNDERLINE);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_multi_select, ToolMode.ANNOT_EDIT_RECT_GROUP);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_image_stamper, ToolMode.STAMPER);
        mToolAnnotationToolbarItemPair.put(R.id.controls_annotation_toolbar_tool_rubber_stamper, ToolMode.RUBBER_STAMPER);
        // when there is no annotation permission, hide the quick menu item in the following list
        mNoAnnotPermissionQMList = new ArrayList<>();
        mNoAnnotPermissionQMList.add(R.id.qm_appearance);
        mNoAnnotPermissionQMList.add(R.id.qm_note);
        mNoAnnotPermissionQMList.add(R.id.qm_flatten);
        mNoAnnotPermissionQMList.add(R.id.qm_edit);
        mNoAnnotPermissionQMList.add(R.id.qm_type);
        mNoAnnotPermissionQMList.add(R.id.qm_delete);
        mNoAnnotPermissionQMList.add(R.id.qm_rotate);
        mNoAnnotPermissionQMList.add(R.id.qm_text);

        // Annotation type and handler tool mode
        mAnnotHandlerToolPair = new SparseArray<>();
        mAnnotHandlerToolPair.put(Annot.e_Link, ToolMode.LINK_ACTION);
        mAnnotHandlerToolPair.put(Annot.e_Widget, ToolMode.FORM_FILL);
        mAnnotHandlerToolPair.put(Annot.e_RichMedia, ToolMode.RICH_MEDIA);
        mAnnotHandlerToolPair.put(Annot.e_Line, ToolMode.ANNOT_EDIT_LINE);
        mAnnotHandlerToolPair.put(AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW, ToolMode.ANNOT_EDIT_LINE);
        mAnnotHandlerToolPair.put(AnnotStyle.CUSTOM_ANNOT_TYPE_RULER, ToolMode.ANNOT_EDIT_LINE);
        mAnnotHandlerToolPair.put(Annot.e_Highlight, ToolMode.ANNOT_EDIT_TEXT_MARKUP);
        mAnnotHandlerToolPair.put(Annot.e_Underline, ToolMode.ANNOT_EDIT_TEXT_MARKUP);
        mAnnotHandlerToolPair.put(Annot.e_StrikeOut, ToolMode.ANNOT_EDIT_TEXT_MARKUP);
        mAnnotHandlerToolPair.put(Annot.e_Squiggly, ToolMode.ANNOT_EDIT_TEXT_MARKUP);
        mAnnotHandlerToolPair.put(Annot.e_Polyline, ToolMode.ANNOT_EDIT_ADVANCED_SHAPE);
        mAnnotHandlerToolPair.put(Annot.e_Polygon, ToolMode.ANNOT_EDIT_ADVANCED_SHAPE);
        mAnnotHandlerToolPair.put(AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD, ToolMode.ANNOT_EDIT_ADVANCED_SHAPE);
        mAnnotHandlerToolPair.put(AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT, ToolMode.ANNOT_EDIT_ADVANCED_SHAPE);
    }

    /**
     * Gets tool mode by quick menu item id
     *
     * @param qmItemId quick menu item id
     * @return tool mode
     */
    @Nullable
    public ToolMode getToolModeByQMItemId(@IdRes int qmItemId) {
        if (mToolQMItemPair != null && mToolQMItemPair.indexOfKey(qmItemId) >= 0) {
            return mToolQMItemPair.get(qmItemId);
        }
        return null;
    }

    /**
     * put custom tool mode and quick menu item pair
     *
     * @param mode     tool mode
     * @param qmItemId quick menu item id
     */
    public void putCustomToolQMItemPair(@IdRes int qmItemId, ToolMode mode) {
        mToolQMItemPair.put(qmItemId, mode);
    }

    /**
     * Gets tool mode by annotation toolbar item id
     *
     * @param itemId annotation toolbar item view id
     * @return tool mode
     */
    @Nullable
    public ToolMode getToolModeByAnnotationToolbarItemId(@IdRes int itemId) {
        if (mToolAnnotationToolbarItemPair != null && mToolAnnotationToolbarItemPair.indexOfKey(itemId) >= 0) {
            return mToolAnnotationToolbarItemPair.get(itemId);
        }
        return null;
    }

    /**
     * put custom tool mode and annotation toolbar item view id pair
     *
     * @param mode   tool mode
     * @param itemId annotation toolbar item view id
     */
    public void putCustomToolAnnotationToolbarItemPair(@IdRes int itemId, ToolMode mode) {
        mToolAnnotationToolbarItemPair.put(itemId, mode);
    }

    /**
     * Check if quick menu item should be hidden by a given item id
     *
     * @param itemId quick menu item id
     * @return true then hide quick menu item, else otherwise
     */
    public boolean isHideQMItem(@IdRes int itemId) {
        return mNoAnnotPermissionQMList.contains(itemId);
    }


    /**
     * Add quick menu item to no annotation permission list
     *
     * @param itemId quick menu item id
     */
    public void addQMHideItem(@IdRes int itemId) {
        mNoAnnotPermissionQMList.add(itemId);
    }

    /**
     * remove quick menu item from no annotation permission list
     *
     * @param itemId quick menu item id
     * @return true if removed successfully, else otherwise
     */
    public boolean removeQMHideItem(@IdRes int itemId) {
        return mNoAnnotPermissionQMList.remove(Integer.valueOf(itemId));
    }

    /**
     * get annotation handler tool mode
     *
     * @param annotType annotation type
     * @return tool mode
     */
    public ToolManager.ToolModeBase getAnnotationHandlerToolMode(int annotType) {
        if (isAnnotEditingDisabled(annotType)) {
            return ToolMode.PAN;
        }
        if (mAnnotHandlerToolPair.indexOfKey(annotType) > -1) {
            return mAnnotHandlerToolPair.get(annotType);
        }
        return ToolMode.ANNOT_EDIT;
    }

    /**
     * Put annotation type and tool mode into annotation tool mode pair
     *
     * @param annotType annotation type
     * @param toolMode  tool mode
     */
    public void putAnnotationToolModePair(int annotType, ToolManager.ToolModeBase toolMode) {
        mAnnotHandlerToolPair.put(annotType, toolMode);
    }

    /**
     * A functional interface for pan tool switch tool when long pressing on annotation
     */
    public interface PanLongPressSwitchToolCallback {
        /**
         * Called when {@link com.pdftron.pdf.tools.Pan#onLongPress(MotionEvent)} need to switch to next tool
         *
         * @param annot           potential annotation that long pressed on
         * @param isMadeByPDFTron whether this annotation is created by pdftron
         * @param isTextSelect    whether long pressing on text
         * @return next tool mode Pan to should switch to
         * @throws PDFNetException pdfnet exception
         */
        ToolMode onPanLongPressSwitchTool(@Nullable Annot annot, boolean isMadeByPDFTron, boolean isTextSelect) throws PDFNetException;
    }

    /**
     * Gets Pan tool long press switch tool callback, if no customized callback defined, return the default one.
     * Default callback:
     * <pre>
     *     {@code
     *      if (isTextSelect) {
     *          return ToolMode.TEXT_SELECT;
     *       }
     *       if (annot == null) {
     *           return ToolMode.PAN;
     *       }
     *       switch (annot.getType()) {
     *           case Annot.e_Link:
     *               if(isSelfMade){
     *                   return ToolMode.ANNOT_EDIT;
     *              }else{
     *                   return ToolMode.LINK_ACTION;
     *               }
     *           case Annot.e_Widget:
     *              if (isSelfMade) {
     *                  return ToolMode.ANNOT_EDIT;
     *              } else {
     *                   return ToolMode.FORM_FILL;
     *               }
     *           case Annot.e_Line:
     * *               return ToolMode.ANNOT_EDIT_LINE;
     *           default:
     *               return ToolMode.ANNOT_EDIT;
     *       }
     *     }
     * </pre>
     *
     * @return pan tool long press switch tool callback
     */
    public PanLongPressSwitchToolCallback getPanLongPressSwitchToolCallback() {
        if (mPanLongPressSwitchToolCallback != null) {
            return mPanLongPressSwitchToolCallback;
        }

        return new PanLongPressSwitchToolCallback() {
            @Override
            public ToolMode onPanLongPressSwitchTool(@Nullable Annot annot, boolean isMadeByPDFTron, boolean isTextSelect) throws PDFNetException {
                if (isTextSelect) {
                    return ToolMode.TEXT_SELECT;
                }
                if (annot == null) {
                    return ToolMode.PAN;
                }
                int annotType = AnnotUtils.getAnnotType(annot);
                if (isAnnotEditingDisabled(annotType)) {
                    return ToolMode.PAN;
                }
                switch (annotType) {
                    case Annot.e_Link:
                        if (isMadeByPDFTron) {
                            return ToolMode.ANNOT_EDIT;
                        }
                    case Annot.e_Widget:
                        if (isMadeByPDFTron) {
                            return ToolMode.ANNOT_EDIT;
                        }
                }
                ToolManager.ToolModeBase mode = getAnnotationHandlerToolMode(annotType);
                if (null != mode && mode instanceof ToolMode) {
                    return (ToolMode) mode;
                }
                return ToolMode.ANNOT_EDIT;
            }
        };
    }

    /**
     * Sets customized callback for pan tool switch tool when long press
     *
     * @param callback pan tool long press switch tool tool callback
     */
    public void setPanToolLongPressSwitchToolCallback(PanLongPressSwitchToolCallback callback) {
        mPanLongPressSwitchToolCallback = callback;
    }

    /**
     * Sets list of font to use for free text annotation
     * @param fonts the list of fonts
     */
    public void setFontList(ArrayList<FontResource> fonts) {
        mFontList = fonts;
    }

    /**
     * Gets list of font to use for free text annotation
     * @return the list of fonts
     */
    public ArrayList<FontResource> getFontList() {
        return mFontList;
    }

    /**
     * Disables annotation editing by type.
     *
     * @param annotTypes annot types to be disabled
     */
    public void disableAnnotEditing(Integer[] annotTypes) {
        if (mDisabledAnnotEditingTypes == null) {
            mDisabledAnnotEditingTypes = new HashSet<>();
        }
        Collections.addAll(mDisabledAnnotEditingTypes, annotTypes);
    }

    /**
     * Enables annotation editing by type.
     *
     * @param annotTypes annot types to be enabled
     */
    public void enableAnnotEditing(Integer[] annotTypes) {
        if (mDisabledAnnotEditingTypes == null) {
            return;
        }
        List<Integer> annotTypeList = Arrays.asList(annotTypes);
        mDisabledAnnotEditingTypes.removeAll(annotTypeList);
    }

    /**
     * Checks whether the editing of an annot type is disabled.
     *
     * @param annotType The annot type
     * @return True if editing of the annot type is disabled
     */
    public boolean isAnnotEditingDisabled(int annotType) {
        return mDisabledAnnotEditingTypes != null && mDisabledAnnotEditingTypes.contains(annotType);
    }

}
