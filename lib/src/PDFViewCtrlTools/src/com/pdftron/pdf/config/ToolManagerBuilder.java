//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------
package com.pdftron.pdf.config;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.app.FragmentActivity;
import android.util.SparseArray;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.PdfViewCtrlTabFragment;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * ToolManagerBuilder is a helper for constructing {@link ToolManager} with xml configuration and
 * set {@link ToolManager} to {@link PDFViewCtrl}
 * <p>
 * For example, you can initialize ToolManager as following:
 * <pre>
 *  ToolManager toolManager = ToolManagerBuilder
 *      .from()
 *      .build(getActivity(), mPDFViewCtrl);
 * </pre>
 * where {@code mPDFViewCtrl} is an instance of {@link PDFViewCtrl}
 */
public class ToolManagerBuilder implements Parcelable {
    private boolean editInk;
    private boolean addImage;
    private boolean openToolbar;
    private boolean buildInPageIndicator = true;
    private boolean annotPermission;
    private boolean showAuthor;
    private boolean textMarkupAdobeHack = true;
    private boolean copyAnnot;
    private boolean stylusAsPen;
    private boolean inkSmoothing = true;
    private boolean autoSelect;
    private boolean disableQuickMenu;
    private boolean doubleTapToZoom = true;
    private boolean autoResizeFreeText;
    private boolean realtimeAnnotEdit = true;
    private boolean editFreeTextOnTap;
    private int disableToolModesId = -1;
    private int modeSize = 0;
    private String[] modes;
    private SparseArray<Object> customToolClassMap;
    private SparseArray<Object> customToolParamMap;
    private int disableEditingAnnotTypesId = -1;
    private int annotTypeSize = 0;
    private int[] annotTypes;

    private ToolManagerBuilder() {
    }

    protected ToolManagerBuilder(Parcel in) {
        editInk = in.readByte() != 0;
        addImage = in.readByte() != 0;
        openToolbar = in.readByte() != 0;
        copyAnnot = in.readByte() != 0;
        stylusAsPen = in.readByte() != 0;
        inkSmoothing = in.readByte() != 0;
        autoSelect = in.readByte() != 0;
        buildInPageIndicator = in.readByte() != 0;
        annotPermission = in.readByte() != 0;
        showAuthor = in.readByte() != 0;
        textMarkupAdobeHack = in.readByte() != 0;
        disableQuickMenu = in.readByte() != 0;
        doubleTapToZoom = in.readByte() != 0;
        autoResizeFreeText = in.readByte() != 0;
        realtimeAnnotEdit = in.readByte() != 0;
        editFreeTextOnTap = in.readByte() != 0;
        disableToolModesId = in.readInt();
        modeSize = in.readInt();
        modes = new String[modeSize];
        in.readStringArray(modes);
        customToolClassMap = in.readSparseArray(Tool.class.getClassLoader());
        customToolParamMap = in.readSparseArray(Object[].class.getClassLoader());
        disableEditingAnnotTypesId = in.readInt();
        annotTypeSize = in.readInt();
        annotTypes = new int[annotTypeSize];
        in.readIntArray(annotTypes);
    }

    public static final Creator<ToolManagerBuilder> CREATOR = new Creator<ToolManagerBuilder>() {
        @Override
        public ToolManagerBuilder createFromParcel(Parcel in) {
            return new ToolManagerBuilder(in);
        }

        @Override
        public ToolManagerBuilder[] newArray(int size) {
            return new ToolManagerBuilder[size];
        }
    };

    /**
     * Creates a new ToolManagerBuilder for constructing {@link ToolManager}.
     *
     * @return a new ToolManagerBuilder instance
     */
    public static ToolManagerBuilder from() {
        return new ToolManagerBuilder();
    }

    /**
     * Creates a new ToolManagerBuilder for constructing {@link ToolManager}.
     *
     * @param context  The context
     * @param styleRes style resource that contains tool manager configuration
     * @return a new ToolManagerBuilder instance
     */
    public static ToolManagerBuilder from(Context context, @StyleRes int styleRes) {
        return from().setStyle(context, styleRes);
    }

    /**
     * Sets configuration to tool manager
     *
     * @param styleRes style resource that contains tool manager configuration
     * @return ToolManagerBuilder instance
     */
    public ToolManagerBuilder setStyle(Context context, @StyleRes int styleRes) {
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolManager, 0, styleRes);
        try {
            setEditInk(a.getBoolean(R.styleable.ToolManager_edit_ink_annots, editInk))
                .setAddImage(a.getBoolean(R.styleable.ToolManager_add_image_stamper_tool, addImage))
                .setOpenToolbar(a.getBoolean(R.styleable.ToolManager_open_toolbar_on_pan_ink_selected, openToolbar))
                .setBuildInPageIndicator(a.getBoolean(R.styleable.ToolManager_build_in_page_number_indicator, buildInPageIndicator))
                .setAnnotPermission(a.getBoolean(R.styleable.ToolManager_annot_permission_check, annotPermission))
                .setShowAuthor(a.getBoolean(R.styleable.ToolManager_show_author_dialog, showAuthor))
                .setTextMarkupAdobeHack(a.getBoolean(R.styleable.ToolManager_text_markup_adobe_hack, textMarkupAdobeHack))
                .setCopyAnnot(a.getBoolean(R.styleable.ToolManager_copy_annotated_text_to_note, copyAnnot))
                .setStylusAsPen(a.getBoolean(R.styleable.ToolManager_stylus_as_Pen, stylusAsPen))
                .setInkSmoothing(a.getBoolean(R.styleable.ToolManager_ink_smoothing_enabled, inkSmoothing))
                .setDisableQuickMenu(a.getBoolean(R.styleable.ToolManager_quick_menu_disable, disableQuickMenu))
                .setDoubleTapToZoom(a.getBoolean(R.styleable.ToolManager_double_tap_to_zoom, doubleTapToZoom))
                .setAutoResizeFreeText(a.getBoolean(R.styleable.ToolManager_auto_resize_freetext, autoResizeFreeText))
                .setRealTimeAnnotEdit(a.getBoolean(R.styleable.ToolManager_realtime_annot_edit, realtimeAnnotEdit))
                .setEditFreeTextOnTap(a.getBoolean(R.styleable.ToolManager_edit_freetext_on_tap, editFreeTextOnTap))
                .setAutoSelect(a.getBoolean(R.styleable.ToolManager_auto_select_annotation, autoSelect))
                .setDisableToolModesId(a.getResourceId(R.styleable.ToolManager_disable_tool_modes, disableToolModesId))
                .setDisableEditingAnnotTypesId(a.getResourceId(R.styleable.ToolManager_disable_annot_editing_by_types, disableEditingAnnotTypesId));
            if (disableToolModesId != -1) {
                modes = context.getResources().getStringArray(disableToolModesId);
            }
            if (disableEditingAnnotTypesId != -1) {
                annotTypes = context.getResources().getIntArray(disableEditingAnnotTypesId);
            }
        } finally {
            a.recycle();
        }
        return this;
    }

    /**
     * Sets whether user can edit ink annotation
     *
     * @param editInk true then able to edit ink, false other wise
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder setEditInk(boolean editInk) {
        this.editInk = editInk;
        return this;
    }

    /**
     * Sets whether user can add stamper image
     *
     * @param addImage true then able to add image, false other wise
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder setAddImage(boolean addImage) {
        this.addImage = addImage;
        return this;
    }

    /**
     * Sets whether ink tool will open annotation toolbar
     *
     * @param openToolbar if true, click ink from quick menu will
     *                    open the annotation toolbar in ink mode,
     *                    false otherwise
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder setOpenToolbar(boolean openToolbar) {
        this.openToolbar = openToolbar;
        return this;
    }

    /**
     * Indicates whether to use/show the built-in page number indicator.
     *
     * @param buildInPageIndicator true to show the built-in page number indicator, false
     *                             otherwise.
     */
    public ToolManagerBuilder setBuildInPageIndicator(boolean buildInPageIndicator) {
        this.buildInPageIndicator = buildInPageIndicator;
        return this;
    }

    /**
     * Sets whether to check annotation author permission
     *
     * @param annotPermission if true, annotation created by user A cannot be modified by user B,
     *                        else anyone can modify any annotation
     */
    public ToolManagerBuilder setAnnotPermission(boolean annotPermission) {
        this.annotPermission = annotPermission;
        return this;
    }

    /**
     * ets whether to show author dialog the first time when user annotates.
     *
     * @param showAuthor if true, show author dialog the first time when user annotates
     */
    public ToolManagerBuilder setShowAuthor(boolean showAuthor) {
        this.showAuthor = showAuthor;
        return this;
    }

    /**
     * Sets whether the TextMarkup annotations are compatible with Adobe
     * (Adobe's quads don't follow the specification, but they don't handle quads that do).
     */
    public ToolManagerBuilder setTextMarkupAdobeHack(boolean textMarkupAdobeHack) {
        this.textMarkupAdobeHack = textMarkupAdobeHack;
        return this;
    }

    /**
     * Sets whether to copy annotated text to note
     *
     * @param copyAnnot enable copy annotated text to note
     */
    public ToolManagerBuilder setCopyAnnot(boolean copyAnnot) {
        this.copyAnnot = copyAnnot;
        return this;
    }

    /**
     * Sets whether to use stylus to draw without entering ink tool
     *
     * @param stylusAsPen enable inking with stylus in pan mode
     */
    public ToolManagerBuilder setStylusAsPen(boolean stylusAsPen) {
        this.stylusAsPen = stylusAsPen;
        return this;
    }

    /**
     * Sets whether to smooth ink annotation
     *
     * @param inkSmoothing enable ink smoothing
     */
    public ToolManagerBuilder setInkSmoothing(boolean inkSmoothing) {
        this.inkSmoothing = inkSmoothing;
        return this;
    }

    /**
     * Sets whether auto select annotation after annotation is created
     *
     * @param autoSelect if true, after creating annotation, it will auto select it and show quick menu
     */
    public ToolManagerBuilder setAutoSelect(boolean autoSelect) {
        this.autoSelect = autoSelect;
        return this;
    }

    /**
     * Sets whether disable showing the long press quick menu
     *
     * @param disableQuickMenu if true, disable showing the long press quick menu
     */
    public ToolManagerBuilder setDisableQuickMenu(boolean disableQuickMenu) {
        this.disableQuickMenu = disableQuickMenu;
        return this;
    }

    /**
     * Sets whether can double tap to zoom, true by default
     *
     * @param doubleTapToZoom if true, can double tap to zoom, false otherwise
     */
    public ToolManagerBuilder setDoubleTapToZoom(boolean doubleTapToZoom) {
        this.doubleTapToZoom = doubleTapToZoom;
        return this;
    }

    /**
     * Sets whether can auto resize free text when editing
     *
     * @param autoResizeFreeText if true can auto resize, false otherwise
     */
    public ToolManagerBuilder setAutoResizeFreeText(boolean autoResizeFreeText) {
        this.autoResizeFreeText = autoResizeFreeText;
        return this;
    }

    /**
     * Sets whether annotation editing is real time
     *
     * @param realTimeAnnotEdit true if real time, false otherwise
     */
    public ToolManagerBuilder setRealTimeAnnotEdit(boolean realTimeAnnotEdit) {
        this.realtimeAnnotEdit = realTimeAnnotEdit;
        return this;
    }

    /**
     * Sets whether can edit freetext annotation on tap
     *
     * @param editFreeTextOnTap true if tap will start edit freetext, false otherwise
     */
    public ToolManagerBuilder setEditFreeTextOnTap(boolean editFreeTextOnTap) {
        this.editFreeTextOnTap = editFreeTextOnTap;
        return this;
    }

    /**
     * Sets disabled tool modes reference array id
     *
     * @param disableToolModesId disabled tool modes string array id
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder setDisableToolModesId(@ArrayRes int disableToolModesId) {
        this.disableToolModesId = disableToolModesId;
        return this;
    }

    /**
     * Disable tool modes in tool manager
     *
     * @param toolModes disabled tool modes
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder disableToolModes(ToolManager.ToolMode[] toolModes) {
        this.modes = new String[toolModes.length];
        for (int i = 0; i < toolModes.length; i++) {
            this.modes[i] = toolModes[i].name();
        }
        return this;
    }

    /**
     * Sets disabled editing annot type reference int id
     *
     * @param disableEditingAnnotId disabled editing of annotation type int array id
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder setDisableEditingAnnotTypesId(@ArrayRes int disableEditingAnnotId) {
        this.disableEditingAnnotTypesId = disableEditingAnnotId;
        return this;
    }

    /**
     * Disable editing by annot type in tool manager
     *
     * @param annotTypes disabled editing of annot types
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder disableAnnotEditing(int[] annotTypes) {
        this.annotTypes = new int[annotTypes.length];
        System.arraycopy(annotTypes, 0, this.annotTypes, 0, annotTypes.length);
        return this;
    }

    /**
     * Add customized tool
     *
     * @param tool The customized tool
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder addCustomizedTool(Tool tool) {
        if (null == customToolClassMap) {
            customToolClassMap = new SparseArray<>();
        }
        customToolClassMap.put(tool.getToolMode().getValue(), tool.getClass());
        return this;
    }

    /**
     * Add customized tool
     *
     * @param toolMode  The customized tool mode
     * @param toolClass The customized tool mode class
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder addCustomizedTool(ToolManager.ToolModeBase toolMode, Class<? extends Tool> toolClass) {
        if (null == customToolClassMap) {
            customToolClassMap = new SparseArray<>();
        }
        customToolClassMap.put(toolMode.getValue(), toolClass);
        return this;
    }


    /**
     * Add customized tool
     *
     * @param tool   customized tool.
     * @param params parameter for instantiate tool
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder addCustomizedTool(Tool tool, Object... params) {
        addCustomizedTool(tool);
        if (null == customToolParamMap) {
            customToolParamMap = new SparseArray<>();
        }
        customToolParamMap.put(tool.getToolMode().getValue(), params);
        return this;
    }

    /**
     * Add customized tool
     *
     * @param toolMode  customized tool mode.
     * @param toolClass The customized tool mode class
     * @param params    parameter for instantiate tool
     * @return ToolManagerBuilder
     */
    public ToolManagerBuilder addCustomizedTool(ToolManager.ToolModeBase toolMode, Class<? extends Tool> toolClass, Object... params) {
        addCustomizedTool(toolMode, toolClass);
        if (null == customToolParamMap) {
            customToolParamMap = new SparseArray<>();
        }
        customToolParamMap.put(toolMode.getValue(), params);
        return this;
    }


    /**
     * Building tool manager by given {@link PDFViewCtrl}
     *
     * @param pdfViewCtrl The pdfviewCtrl
     * @return tool manager
     */
    public ToolManager build(@Nullable FragmentActivity activity, @NonNull PDFViewCtrl pdfViewCtrl) {
        ToolManager toolManager = new ToolManager(pdfViewCtrl);
        Context context = pdfViewCtrl.getContext();
        toolManager.setEditInkAnnots(editInk);
        toolManager.setAddImageStamperTool(addImage);
        toolManager.setCanOpenEditToolbarFromPan(openToolbar);
        toolManager.setCopyAnnotatedTextToNoteEnabled(PdfViewCtrlSettingsManager.getCopyAnnotatedTextToNote(context, copyAnnot));
        toolManager.setStylusAsPen(PdfViewCtrlSettingsManager.getStylusAsPen(context, stylusAsPen));
        toolManager.setInkSmoothingEnabled(PdfViewCtrlSettingsManager.getInkSmoothing(context, inkSmoothing));
        toolManager.setFreeTextFonts(PdfViewCtrlSettingsManager.getFreeTextFonts(context));
        toolManager.setAutoSelectAnnotation(PdfViewCtrlSettingsManager.isAutoSelectAnnotation(context, autoSelect));
        toolManager.setBuiltInPageNumberIndicatorVisible(buildInPageIndicator);
        toolManager.setAnnotPermissionCheckEnabled(annotPermission);
        toolManager.setShowAuthorDialog(showAuthor);
        toolManager.setTextMarkupAdobeHack(textMarkupAdobeHack);
        toolManager.setDisableQuickMenu(disableQuickMenu);
        toolManager.setDoubleTapToZoom(doubleTapToZoom);
        toolManager.setAutoResizeFreeText(autoResizeFreeText);
        toolManager.setRealTimeAnnotEdit(realtimeAnnotEdit);
        toolManager.setEditFreeTextOnTap(editFreeTextOnTap);
        toolManager.setCurrentActivity(activity);

        if (modes == null && disableToolModesId != -1) {
            modes = context.getResources().getStringArray(disableToolModesId);
        }

        if (annotTypes == null && disableEditingAnnotTypesId != -1) {
            annotTypes = context.getResources().getIntArray(disableEditingAnnotTypesId);
        }

        if (modes != null) {
            ArrayList<ToolManager.ToolMode> disabledModes = new ArrayList<>(modes.length);
            for (String mode : modes) {
                disabledModes.add(ToolManager.ToolMode.valueOf(mode));
            }
            toolManager.disableToolMode(disabledModes.toArray(new ToolManager.ToolMode[disabledModes.size()]));
        }
        if (annotTypes != null) {
            toolManager.disableAnnotEditing(ArrayUtils.toObject(annotTypes));
        }
        if (customToolClassMap != null) {
            HashMap<ToolManager.ToolModeBase, Class<? extends Tool>> map = new HashMap<>();
            for (int i = 0; i < customToolClassMap.size(); i++) {
                int toolModeVal = customToolClassMap.keyAt(i);
                ToolManager.ToolModeBase toolMode = ToolManager.ToolMode.toolModeFor(toolModeVal);
                Class<? extends Tool> value = (Class<? extends Tool>) customToolClassMap.valueAt(i);
                map.put(toolMode, value);
            }
            toolManager.addCustomizedTool(map);
        }
        if (customToolParamMap != null) {
            HashMap<ToolManager.ToolModeBase, Object[]> map = new HashMap<>();
            for (int i = 0; i < customToolParamMap.size(); i++) {
                int toolModeVal = customToolParamMap.keyAt(i);
                ToolManager.ToolModeBase toolMode = ToolManager.ToolMode.toolModeFor(toolModeVal);
                Object[] value = (Object[]) customToolParamMap.valueAt(i);
                map.put(toolMode, value);
            }
            toolManager.addCustomizedToolParams(map);
        }
        pdfViewCtrl.setToolManager(toolManager);
        return toolManager;
    }

    /**
     * Building tool manager by given {@link PdfViewCtrlTabFragment} and sets fragment listener
     *
     * @param fragment The PdfViewCtrlTabFragment
     * @return tool manager
     */
    public ToolManager build(@NonNull PdfViewCtrlTabFragment fragment) {
        ToolManager toolManager = build(fragment.getActivity(), fragment.getPDFViewCtrl());
        toolManager.setPreToolManagerListener(fragment);
        toolManager.setQuickMenuListener(fragment);
        toolManager.addAnnotationModificationListener(fragment);
        toolManager.addPdfDocModificationListener(fragment);
        toolManager.setBasicAnnotationListener(fragment);
        toolManager.setOnGenericMotionEventListener(fragment);
        return toolManager;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (editInk ? 1 : 0));
        dest.writeByte((byte) (addImage ? 1 : 0));
        dest.writeByte((byte) (openToolbar ? 1 : 0));
        dest.writeByte((byte) (copyAnnot ? 1 : 0));
        dest.writeByte((byte) (stylusAsPen ? 1 : 0));
        dest.writeByte((byte) (inkSmoothing ? 1 : 0));
        dest.writeByte((byte) (autoSelect ? 1 : 0));
        dest.writeByte((byte) (buildInPageIndicator ? 1 : 0));
        dest.writeByte((byte) (annotPermission ? 1 : 0));
        dest.writeByte((byte) (showAuthor ? 1 : 0));
        dest.writeByte((byte) (textMarkupAdobeHack ? 1 : 0));
        dest.writeByte((byte) (disableQuickMenu ? 1 : 0));
        dest.writeByte((byte) (doubleTapToZoom ? 1 : 0));
        dest.writeByte((byte) (autoResizeFreeText ? 1 : 0));
        dest.writeByte((byte) (realtimeAnnotEdit ? 1 : 0));
        dest.writeByte((byte) (editFreeTextOnTap ? 1 : 0));
        dest.writeInt(disableToolModesId);
        if (modes == null) {
            modes = new String[0];
        }
        modeSize = modes.length;
        dest.writeInt(modeSize);
        dest.writeStringArray(modes);
        dest.writeSparseArray(customToolClassMap);
        dest.writeSparseArray(customToolParamMap);
        dest.writeInt(disableEditingAnnotTypesId);
        if (annotTypes == null) {
            annotTypes = new int[0];
        }
        annotTypeSize = annotTypes.length;
        dest.writeInt(annotTypeSize);
        dest.writeIntArray(annotTypes);
    }
}


