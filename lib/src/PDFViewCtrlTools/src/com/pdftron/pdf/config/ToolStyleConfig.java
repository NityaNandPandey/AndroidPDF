//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.ArrayRes;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.util.SparseIntArray;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.SoundCreate;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.UnitConverter;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

/**
 * A helper class for configuring style of annotation creator tools.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ToolStyleConfig {
    private static ToolStyleConfig _INSTANCE;


    /**
     * Custom annotation properties for SharedPreference
     */
    private static final String PREF_ANNOTATION_PROPERTY_LINE = "annotation_property_shape"; // line
    private static final String PREF_ANNOTATION_PROPERTY_ARROW = "annotation_property_arrow";
    private static final String PREF_ANNOTATION_PROPERTY_RULER = "annotation_property_ruler";
    private static final String PREF_ANNOTATION_PROPERTY_POLYLINE = "annotation_property_polyline";
    private static final String PREF_ANNOTATION_PROPERTY_RECTANGLE = "annotation_property_rectangle";
    private static final String PREF_ANNOTATION_PROPERTY_OVAL = "annotation_property_oval";
    private static final String PREF_ANNOTATION_PROPERTY_POLYGON = "annotation_property_polygon";
    private static final String PREF_ANNOTATION_PROPERTY_CLOUD = "annotation_property_cloud";
    private static final String PREF_ANNOTATION_PROPERTY_HIGHLIGHT = "annotation_property_highlight";
    private static final String PREF_ANNOTATION_PROPERTY_UNDERLINE = "annotation_property_text_markup"; // underline
    private static final String PREF_ANNOTATION_PROPERTY_LINK = "annotation_property_link"; // link
    private static final String PREF_ANNOTATION_PROPERTY_STRIKEOUT = "annotation_property_strikeout";
    private static final String PREF_ANNOTATION_PROPERTY_SQUIGGLY = "annotation_property_squiggly";
    private static final String PREF_ANNOTATION_PROPERTY_FREETEXT = "annotation_property_freetext";
    private static final String PREF_ANNOTATION_PROPERTY_CALLOUT = "annotation_property_callout";
    private static final String PREF_ANNOTATION_PROPERTY_FREEHAND = "annotation_property_freehand";
    private static final String PREF_ANNOTATION_PROPERTY_FREE_HIGHLIGHTER = "annotation_property_free_highlighter";
    private static final String PREF_ANNOTATION_PROPERTY_NOTE = "annotation_property_note";
    private static final String PREF_ANNOTATION_PROPERTY_ERASER = "annotation_property_eraser";
    private static final String PREF_ANNOTATION_PROPERTY_SIGNATURE = "annotation_property_signature";
    private static final String PREF_ANNOTATION_PROPERTY_FILL_COLORS = "_fill_colors";
    private static final String PREF_ANNOTATION_PROPERTY_COLOR = "_color";
    private static final String PREF_ANNOTATION_PROPERTY_TEXT_COLOR = "_text_color";
    private static final String PREF_ANNOTATION_PROPERTY_TEXT_SIZE = "_text_size";
    private static final String PREF_ANNOTATION_PROPERTY_FILL_COLOR = "_fill_color";
    private static final String PREF_ANNOTATION_PROPERTY_OPACITY = "_opacity";
    private static final String PREF_ANNOTATION_PROPERTY_THICKNESS = "_thickness";
    private static final String PREF_ANNOTATION_PROPERTY_ICON = "_icon";
    private static final String PREF_ANNOTATION_PROPERTY_FONT = "_font";
    private static final String PREF_ANNOTATION_PROPERTY_CUSTOM = "_custom";

    private static final String PREF_ANNOTATION_PROPERTY_RULER_BASE_UNIT = "_ruler_base_unit";
    private static final String PREF_ANNOTATION_PROPERTY_RULER_BASE_VALUE = "_ruler_base_value";
    private static final String PREF_ANNOTATION_PROPERTY_RULER_TRANSLATE_UNIT = "_ruler_translate_unit";
    private static final String PREF_ANNOTATION_PROPERTY_RULER_TRANSLATE_VALUE = "_ruler_translate_value";

    /**
     * @return instance of ToolStyleConfig
     */
    public static ToolStyleConfig getInstance() {
        if (null == _INSTANCE) {
            _INSTANCE = new ToolStyleConfig();
        }
        return _INSTANCE;
    }

    /**
     * Annotation type and annotation style map
     */
    private SparseIntArray mAnnotStyleMap;

    /**
     * Annotation type and annotation presets map
     */
    private SparseIntArray mAnnotPresetMap;

    /**
     * Class constructor
     */
    public ToolStyleConfig() {
        mAnnotStyleMap = new SparseIntArray();
    }

    /**
     * Add Customized default style for annotation
     *
     * @param annotType annotation mode
     * @param styleRes  annotation style resource
     */
    public void addDefaultStyleMap(int annotType, @StyleRes int styleRes) {
        mAnnotStyleMap.put(annotType, styleRes);
    }

    /**
     * Add Customized default style for annotation
     *
     * @param annotType annotation mode
     * @param attrRes   preset attribute resource
     */
    public void addAnnotPresetMap(int annotType, @AttrRes int attrRes) {
        if (mAnnotPresetMap == null) {
            mAnnotPresetMap = new SparseIntArray();
        }
        mAnnotPresetMap.put(annotType, attrRes);
    }

    /**
     * Gets default tool style
     *
     * @param annotType annotation tool mode
     * @return style resource
     */
    @StyleRes
    public int getDefaultStyle(int annotType) {
        if (mAnnotStyleMap.indexOfKey(annotType) >= 0) {
            return mAnnotStyleMap.get(annotType);
        }
        switch (annotType) {
            case Annot.e_Highlight:
                return R.style.HighlightPresetStyle1;
            case Annot.e_Underline:
            case Annot.e_Squiggly:
            case Annot.e_StrikeOut:
                return R.style.TextMarkupStyle1;
            case Annot.e_FreeText:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                return R.style.FreeTextPresetStyle1;
            case Annot.e_Text:
                return R.style.AnnotPresetStyle1;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                return R.style.SignaturePresetStyle1;
            case Annot.e_Ink:
                return R.style.AnnotPresetStyle4;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER:
                return R.style.EraserStyle1;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
                return R.style.FreeHighlighterStyle4;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                return R.style.RulerStyle1;
            default:
                return R.style.AnnotPresetStyle4;
        }
    }

    /**
     * Gets default tool style
     *
     * @param annotType annotation tool mode
     * @return style resource
     */
    @AttrRes
    public int getDefaultAttr(int annotType) {

        switch (annotType) {
            case Annot.e_Highlight:
                return R.attr.highlight_default_style;
            case Annot.e_Underline:
                return R.attr.underline_default_style;
            case Annot.e_Squiggly:
                return R.attr.squiggly_default_style;
            case Annot.e_StrikeOut:
                return R.attr.strikeout_default_style;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
                return R.attr.free_highlighter_default_style;
            case Annot.e_FreeText:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                return R.attr.free_text_default_style;
            case Annot.e_Text:
                return R.attr.sticky_note_default_style;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                return R.attr.signature_default_style;
            case Annot.e_Link:
                return R.attr.link_default_style;
            case Annot.e_Ink:
                return R.attr.freehand_default_style;
            case Annot.e_Line:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
                return R.attr.line_default_style;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                return R.attr.ruler_default_style;
            case Annot.e_Polyline:
                return R.attr.polyline_default_style;
            case Annot.e_Square:
                return R.attr.rect_default_style;
            case Annot.e_Circle:
                return R.attr.oval_default_style;
            case Annot.e_Polygon:
                return R.attr.polygon_default_style;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
                return R.attr.cloud_default_style;
            default:
                return R.attr.other_default_style;
        }
    }

    /**
     * Gets presets attr
     *
     * @param annotType annotation tool mode
     * @return attribute resource
     */
    public @AttrRes
    int getPresetsAttr(int annotType) {
        if (mAnnotPresetMap != null && mAnnotPresetMap.indexOfKey(annotType) >= 0) {
            return mAnnotPresetMap.get(annotType);
        }
        switch (annotType) {
            case Annot.e_Highlight:
                return R.attr.highlight_presets;
            case Annot.e_Underline:
                return R.attr.underline_presets;
            case Annot.e_Squiggly:
                return R.attr.squiggly_presets;
            case Annot.e_StrikeOut:
                return R.attr.strikeout_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
                return R.attr.free_highlighter_presets;
            case Annot.e_FreeText:
                return R.attr.free_text_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                return R.attr.callout_presets;
            case Annot.e_Text:
                return R.attr.sticky_note_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                return R.attr.signature_presets;
            case Annot.e_Link:
                return R.attr.link_presets;
            case Annot.e_Ink:
                return R.attr.freehand_presets;
            case Annot.e_Line:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
                return R.attr.line_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                return R.attr.ruler_presets;
            case Annot.e_Polyline:
                return R.attr.polyline_presets;
            case Annot.e_Square:
                return R.attr.rect_presets;
            case Annot.e_Circle:
                return R.attr.oval_presets;
            case Annot.e_Polygon:
                return R.attr.polygon_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
                return R.attr.cloud_presets;
            default:
                return R.attr.other_presets;
        }
    }

    /**
     * Gets default tool style
     *
     * @param annotType annotation tool mode
     * @return style resource
     */
    public @ArrayRes
    int getDefaultPresetsArrayRes(int annotType) {

        switch (annotType) {
            case Annot.e_Highlight:
                return R.array.highlight_presets;
            case Annot.e_Underline:
            case Annot.e_Squiggly:
            case Annot.e_StrikeOut:
                return R.array.text_markup_presets;
            case Annot.e_Text:
                return R.array.color_only_presets;
            case Annot.e_FreeText:
                return R.array.free_text_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                return R.array.callout_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                return R.array.signature_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER:
                return R.array.eraser_presets;
            case Annot.e_Square:
            case Annot.e_Circle:
            case Annot.e_Polygon:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
                return R.array.fill_only_presets;
            case Annot.e_Ink:
                return R.array.freehand_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
                return R.array.freehand_highlighter_presets;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                return R.array.ruler_presets;
            default:
                return R.array.stroke_only_presets;
        }
    }

    /**
     * Gets default tool color
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @param extraTag  extra tag
     * @return color
     */
    public @ColorInt
    int getDefaultColor(Context context, int annotType, String extraTag) {
        int index = 0;
        if (extraTag.endsWith("2")) {
            index = 1;
        } else if (extraTag.endsWith("3")) {
            index = 2;
        } else if (extraTag.endsWith("4")) {
            index = 3;
        } else if (extraTag.endsWith("5")) {
            index = 4;
        }
        return getPresetColor(context, index, getPresetsAttr(annotType), getDefaultPresetsArrayRes(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default tool color
     *
     * @param context The context
     * @return color
     */
    public @ColorInt
    int getDefaultColor(@NonNull Context context, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defStyleAttr, defStyleRes);
        int color = a.getColor(R.styleable.ToolStyle_annot_color, Color.BLACK);
        a.recycle();
        return color;
    }

    /**
     * Gets default icon list
     *
     * @param context The context
     * @return list of icon names
     */
    public ArrayList<String> getIconsList(Context context) {
        return getIconsList(context, R.attr.sticky_note_icons, R.array.stickynote_icons);
    }

    /**
     * Gets icon list based on given attribute resource and default array resource
     *
     * @param context     The context
     * @param defAttrRes  attribute resource
     * @param defArrayRes array resource
     * @return list of icon names
     */
    public ArrayList<String> getIconsList(@NonNull Context context, @AttrRes int defAttrRes, @ArrayRes int defArrayRes) {

        context = context.getApplicationContext();
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{defAttrRes});
        int iconsRes = typedArray.getResourceId(0, defArrayRes);
        typedArray.recycle();

        TypedArray iconsArray = context.getResources().obtainTypedArray(iconsRes);
        ArrayList<String> icons = new ArrayList<>();
        for (int i = 0; i < iconsArray.length(); i++) {
            String icon = iconsArray.getString(i);
            if (!Utils.isNullOrEmpty(icon)) {
                icons.add(icon);
            }
        }
        iconsArray.recycle();
        return icons;
    }

    /**
     * Gets default color
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return color
     */
    public @ColorInt
    int getDefaultColor(Context context, int annotType) {
        return getDefaultColor(context, annotType, "");
    }

    /**
     * Gets default text color
     *
     * @param context the context
     * @return color
     */
    public @ColorInt
    int getDefaultTextColor(Context context) {
        return getDefaultTextColor(context, getDefaultAttr(Annot.e_FreeText), getDefaultStyle(Annot.e_FreeText));
    }

    /**
     * Gets default text color
     *
     * @param context     the context
     * @param attrRes     attribute resource
     * @param defStyleRes default style resource
     * @return color
     */
    @ColorInt
    public int getDefaultTextColor(@NonNull Context context, @AttrRes int attrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, attrRes, defStyleRes);
        int color = a.getColor(R.styleable.ToolStyle_annot_text_color, Color.BLACK);
        a.recycle();
        return color;
    }

    /**
     * Gets default thickness
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return default thickness
     */
    public float getDefaultThickness(Context context, int annotType) {
        return getDefaultThickness(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default thickness
     *
     * @param context The context
     * @return default thickness
     */
    public float getDefaultThickness(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float thickness = a.getFloat(R.styleable.ToolStyle_annot_thickness, 1.0f);
        a.recycle();
        return thickness;
    }

    /**
     * Gets default ruler scale base value
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return default value
     */
    public float getDefaultRulerBaseValue(Context context, int annotType) {
        return getDefaultRulerBaseValue(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default ruler scale base value
     *
     * @param context The context
     * @return default value
     */
    public float getDefaultRulerBaseValue(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float value = a.getFloat(R.styleable.ToolStyle_ruler_base_value, 1.0f);
        a.recycle();
        return value;
    }

    /**
     * Gets default ruler scale base unit
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return default unit
     */
    public String getDefaultRulerBaseUnit(Context context, int annotType) {
        return getDefaultRulerBaseUnit(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default ruler scale base unit
     *
     * @param context The context
     * @return default unit
     */
    public String getDefaultRulerBaseUnit(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        String unit = a.getString(R.styleable.ToolStyle_ruler_base_unit);
        if (unit == null) {
            unit = UnitConverter.INCH;
        }
        a.recycle();
        return unit;
    }

    /**
     * Gets default ruler scale translate value
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return default value
     */
    public float getDefaultRulerTranslateValue(Context context, int annotType) {
        return getDefaultRulerTranslateValue(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default ruler scale translate value
     *
     * @param context The context
     * @return default value
     */
    public float getDefaultRulerTranslateValue(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float value = a.getFloat(R.styleable.ToolStyle_ruler_translate_value, 1.0f);
        a.recycle();
        return value;
    }

    /**
     * Gets default ruler scale base unit
     *
     * @param context   The context
     * @param annotType annotation tool mode
     * @return default unit
     */
    public String getDefaultRulerTranslateUnit(Context context, int annotType) {
        return getDefaultRulerTranslateUnit(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default ruler scale base unit
     *
     * @param context The context
     * @return default unit
     */
    public String getDefaultRulerTranslateUnit(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        String unit = a.getString(R.styleable.ToolStyle_ruler_translate_unit);
        if (unit == null) {
            unit = UnitConverter.INCH;
        }
        a.recycle();
        return unit;
    }

    /**
     * @deprecated
     */
    public float getDefaultFontSize(Context context) {
        return getDefaultTextSize(context);
    }

    /**
     * @deprecated
     */
    public float getDefaultFontSize(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        return getDefaultTextSize(context, defAttrRes, defStyleRes);
    }

    /**
     * Gets default font size
     *
     * @param context The context
     * @return default font size
     */
    public float getDefaultTextSize(Context context) {
        return getDefaultTextSize(context, getDefaultAttr(Annot.e_FreeText), getDefaultStyle(Annot.e_FreeText));
    }

    /**
     * Gets default font size
     *
     * @param context The context
     * @return default font size
     */
    public float getDefaultTextSize(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {

        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float fontSize;
        try {
            fontSize = a.getFloat(R.styleable.ToolStyle_annot_font_size, 16);
        } finally {
            a.recycle();
        }
        return fontSize;
    }


    /**
     * Gets default fill color
     *
     * @param context   The context
     * @param annotType The annotation type
     * @return default fill color
     */
    public @ColorInt
    int getDefaultFillColor(Context context, int annotType) {
        return getDefaultFillColor(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default fill color
     *
     * @param context context
     * @return default fill color
     */
    @ColorInt
    public int getDefaultFillColor(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        int color;
        try {
            color = a.getColor(R.styleable.ToolStyle_annot_fill_color, Color.TRANSPARENT);
        } finally {
            a.recycle();
        }
        return color;
    }

    /**
     * Gets default maximum thickness
     *
     * @param context   context
     * @param annotType The annotation type
     * @return maximum thickness
     */
    public float getDefaultMaxThickness(Context context, int annotType) {
        return getDefaultMaxThickness(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default maximum thickness
     *
     * @param context context
     * @return maximum thickness
     */
    public float getDefaultMaxThickness(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float thickness;
        try {
            thickness = a.getFloat(R.styleable.ToolStyle_annot_thickness_max, 1.0f);
        } finally {
            a.recycle();
        }
        return thickness;
    }

    /**
     * Gets default minimum thickness
     *
     * @param context   context
     * @param annotType annotation tool mode
     * @return minimum thickness
     */
    public float getDefaultMinThickness(Context context, int annotType) {
        return getDefaultMinThickness(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default minimum thickness
     *
     * @param context context
     * @return minimum thickness
     */
    public float getDefaultMinThickness(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float thickness;
        try {
            thickness = a.getFloat(R.styleable.ToolStyle_annot_thickness_min, 0);
        } finally {
            a.recycle();
        }
        return thickness;
    }

    /**
     * Gets default minimum text size
     *
     * @param context context
     * @return minimum text size
     */
    public float getDefaultMinTextSize(Context context) {
        return getDefaultMinTextSize(context, getDefaultAttr(Annot.e_FreeText), getDefaultStyle(Annot.e_FreeText));
    }

    /**
     * Gets default minimum text size
     *
     * @param context context
     * @return minimum text size
     */
    public float getDefaultMinTextSize(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float thickness;
        try {
            thickness = a.getFloat(R.styleable.ToolStyle_annot_text_size_min, 1.0f);
        } finally {
            a.recycle();
        }
        return thickness;
    }

    /**
     * Gets default maximum text size
     *
     * @param context context
     * @return maximum text size
     */
    public float getDefaultMaxTextSize(Context context) {
        return getDefaultMaxTextSize(context, getDefaultAttr(Annot.e_FreeText), getDefaultStyle(Annot.e_FreeText));
    }

    /**
     * Gets default maximum text size
     *
     * @param context context
     * @return maximum text size
     */
    public float getDefaultMaxTextSize(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float thickness;
        try {
            thickness = a.getFloat(R.styleable.ToolStyle_annot_text_size_max, 72.0f);
        } finally {
            a.recycle();
        }
        return thickness;
    }

    /**
     * Gets default font
     *
     * @param context   context
     * @param annotType annotation tool mode
     * @return default font
     */
    public String getDefaultFont(Context context, int annotType) {
        return getDefaultFont(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default font
     *
     * @param context context
     * @return default font
     */
    public String getDefaultFont(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        String font;
        try {
            font = a.getString(R.styleable.ToolStyle_annot_font);
        } finally {
            a.recycle();
        }
        if (null == font) {
            return "";
        }
        return font;
    }

    /**
     * Gets default icon
     *
     * @param context   context
     * @param annotType annotation tool mode
     * @return default icon
     */
    public String getDefaultIcon(Context context, int annotType) {
        return getDefaultIcon(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default icon
     *
     * @param context context
     * @return default icon
     */
    public String getDefaultIcon(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        String result;
        try {
            result = a.getString(R.styleable.ToolStyle_annot_icon);
        } finally {
            a.recycle();
        }
        if (null == result) {
            return "";
        }
        return result;
    }

    /**
     * Gets default opacity
     *
     * @param context   context
     * @param annotType annotation tool mode
     * @return default opacity
     */
    public float getDefaultOpacity(Context context, int annotType) {
        return getDefaultOpacity(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }

    /**
     * Gets default opacity
     *
     * @param context context
     * @return default opacity
     */
    public float getDefaultOpacity(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float result;
        try {
            result = a.getFloat(R.styleable.ToolStyle_annot_opacity, 1.0f);
        } finally {
            a.recycle();
        }
        return result;
    }

    /**
     * Gets thickness range
     *
     * @param context   context
     * @param annotType annotation tool mode
     * @return thickness range
     */
    public float getDefaultThicknessRange(Context context, int annotType) {
        return getDefaultThicknessRange(context, getDefaultAttr(annotType), getDefaultStyle(annotType));
    }


    /**
     * Gets thickness range
     *
     * @param context context
     * @return thickness range
     */
    public float getDefaultThicknessRange(@NonNull Context context, @AttrRes int defAttrRes, @StyleRes int defStyleRes) {
        context = context.getApplicationContext();
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ToolStyle, defAttrRes, defStyleRes);
        float thicknessRange;
        try {
            float thicknessMin = a.getFloat(R.styleable.ToolStyle_annot_thickness_min, 0);
            float thicknessMax = a.getFloat(R.styleable.ToolStyle_annot_thickness_max, 1);
            thicknessRange = thicknessMax - thicknessMin;
        } finally {
            a.recycle();
        }
        return thicknessRange;
    }

    /**
     * Gets default annotation style defined in attrs
     *
     * @param context   the context
     * @param annotType tool mode
     * @return annotation style
     */
    public AnnotStyle getDefaultAnnotStyle(Context context, int annotType) {
        AnnotStyle annotStyle = new AnnotStyle();
        annotStyle.setAnnotType(annotType);
        annotStyle.setStrokeColor(getDefaultColor(context, annotType));
        annotStyle.setFillColor(getDefaultFillColor(context, annotType));
        annotStyle.setOpacity(getDefaultOpacity(context, annotType));
        if (annotStyle.isFreeText()) {
            annotStyle.setTextSize(getDefaultTextSize(context));
            annotStyle.setTextColor(getDefaultTextColor(context));
            annotStyle.setFont(new FontResource(getDefaultFont(context, annotType)));
        }

        annotStyle.setThickness(getDefaultThickness(context, annotType));

        if (annotStyle.isStickyNote()) {
            annotStyle.setIcon(getDefaultIcon(context, annotType));
        } else if (annotStyle.isSound()) {
            annotStyle.setIcon(SoundCreate.SOUND_ICON);
        } else if (annotStyle.isRuler()) {
            annotStyle.setRulerBaseValue(getDefaultRulerBaseValue(context, annotType));
            annotStyle.setRulerBaseUnit(getDefaultRulerBaseUnit(context, annotType));
            annotStyle.setRulerTranslateValue(getDefaultRulerTranslateValue(context, annotType));
            annotStyle.setRulerTranslateUnit(getDefaultRulerTranslateUnit(context, annotType));
        }
        return annotStyle;
    }

    /**
     * Gets custom annotation style from settings
     *
     * @param context   the context
     * @param annotType tool mode
     * @return annotation style
     */
    public AnnotStyle getCustomAnnotStyle(@NonNull Context context, int annotType, String extraTag) {
        AnnotStyle annotStyle = new AnnotStyle();
        annotStyle.setAnnotType(annotType);

        annotStyle.setThickness(getCustomThickness(context, annotType, extraTag));
        annotStyle.setOpacity(getCustomOpacity(context, annotType, extraTag));
        annotStyle.setStrokeColor(getCustomColor(context, annotType, extraTag));
        annotStyle.setFillColor(getCustomFillColor(context, annotType, extraTag));
        annotStyle.setTextColor(getCustomTextColor(context, annotType, extraTag));
        annotStyle.setTextSize(getCustomTextSize(context, annotType, extraTag));
        annotStyle.setIcon(getCustomIconName(context, annotType, extraTag));
        annotStyle.setRulerBaseValue(getCustomRulerBaseValue(context, annotType, extraTag));
        annotStyle.setRulerBaseUnit(getCustomRulerBaseUnit(context, annotType, extraTag));
        annotStyle.setRulerTranslateValue(getCustomRulerTranslateValue(context, annotType, extraTag));
        annotStyle.setRulerTranslateUnit(getCustomRulerTranslateUnit(context, annotType, extraTag));

        String fontPDFTronName = getCustomFontName(context, annotType, extraTag);
        FontResource mFont = new FontResource(fontPDFTronName);
        annotStyle.setFont(mFont);

        return annotStyle;
    }

    // TODO make it private
    public String getThicknessKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
            PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_THICKNESS);
    }

    // TODO make it private
    public String getOpacityKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
            PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_OPACITY);
    }

    // TODO make it private
    public String getColorKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
            PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_COLOR);
    }

    // TODO make it private
    public String getTextColorKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
            PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_TEXT_COLOR);
    }

    // TODO make it private
    public String getTextSizeKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
            PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_TEXT_SIZE);
    }

    // TODO make it private
    public String getFillColorKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
            PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_FILL_COLOR);
    }

    // TODO make it private
    public String getIconKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
            PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_ICON);
    }

    // TODO make it private
    public String getFontKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
            PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_FONT);
    }

    public String getRulerBaseUnitKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_RULER_BASE_UNIT);
    }

    public String getRulerTranslateUnitKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_RULER_TRANSLATE_UNIT);
    }

    public String getRulerBaseValueKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_RULER_BASE_VALUE);
    }

    public String getRulerTranslateValueKey(int annotType, String extraTag) {
        return getAnnotationPropertySettingsKey(annotType, extraTag,
                PREF_ANNOTATION_PROPERTY_CUSTOM + PREF_ANNOTATION_PROPERTY_RULER_TRANSLATE_VALUE);
    }

    /**
     * Save annotation style to settings. The saved annotation style can be retrieved by {@link #getCustomAnnotStyle(Context, int, String)}
     *
     * @param context    The context
     * @param annotStyle annotation style
     * @param extraTag   extra tag for settings
     */
    public void saveAnnotStyle(@NonNull Context context, AnnotStyle annotStyle, String extraTag) {
        SharedPreferences settings = Tool.getToolPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        int annotType = annotStyle.getAnnotType();
        editor.putFloat(getThicknessKey(annotType, extraTag), annotStyle.getThickness());
        editor.putFloat(getOpacityKey(annotType, extraTag), annotStyle.getOpacity());
        editor.putInt(getColorKey(annotType, extraTag), annotStyle.getColor());
        editor.putInt(getTextColorKey(annotType, extraTag), annotStyle.getTextColor());
        editor.putFloat(getTextSizeKey(annotType, extraTag), annotStyle.getTextSize());
        editor.putInt(getFillColorKey(annotType, extraTag), annotStyle.getFillColor());
        editor.putString(getIconKey(annotType, extraTag), annotStyle.getIcon());
        String font = annotStyle.getFont() != null ? annotStyle.getFont().getPDFTronName() : "";
        editor.putString(getFontKey(annotType, extraTag), font);
        editor.apply();
    }

    /**
     * Gets color saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return color in settings
     */
    public int getCustomColor(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getInt(
            getColorKey(annotType, extraTag),
            getDefaultColor(context, annotType, extraTag));
    }

    /**
     * Gets text color saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return text color in settings
     */
    public int getCustomTextColor(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getInt(
            getTextColorKey(annotType, extraTag),
            getDefaultTextColor(context));
    }

    /**
     * Gets text size saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return text size in settings
     */
    public float getCustomTextSize(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getFloat(
            getTextSizeKey(annotType, extraTag),
            getDefaultTextSize(context));
    }

    /**
     * Gets fill color saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return fill color in settings
     */
    public int getCustomFillColor(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getInt(
            getFillColorKey(annotType, extraTag),
            getDefaultFillColor(context, annotType));
    }

    /**
     * Gets thickness saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return thickness in settings
     */
    public float getCustomThickness(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getFloat(
            getThicknessKey(annotType, extraTag),
            getDefaultThickness(context, annotType));
    }

    /**
     * Gets opacity saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return opacity in settings
     */
    public float getCustomOpacity(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getFloat(
            getOpacityKey(annotType, extraTag),
            getDefaultOpacity(context, annotType));
    }

    /**
     * Gets font name saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return font name in settings
     */
    public String getCustomFontName(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getString(
            getFontKey(annotType, extraTag),
            getDefaultFont(context, annotType));
    }

    /**
     * Gets icon name saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return icon name in settings
     */
    public String getCustomIconName(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getString(
            getIconKey(annotType, extraTag),
            getDefaultIcon(context, annotType));
    }

    /**
     * Gets ruler base value saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return ruler base value in settings
     */
    public float getCustomRulerBaseValue(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getFloat(
            getRulerBaseValueKey(annotType, extraTag),
            getDefaultRulerBaseValue(context, annotType));
    }

    /**
     * Gets ruler base unit saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return ruler base unit in settings
     */
    public String getCustomRulerBaseUnit(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getString(
            getRulerBaseUnitKey(annotType, extraTag),
            getDefaultRulerBaseUnit(context, annotType));
    }

    /**
     * Gets ruler translate value saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return ruler translate value in settings
     */
    public float getCustomRulerTranslateValue(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getFloat(
            getRulerTranslateValueKey(annotType, extraTag),
            getDefaultRulerTranslateValue(context, annotType));
    }

    /**
     * Gets ruler translate unit saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param extraTag  extra tag for settings
     * @return ruler translate unit in settings
     */
    public String getCustomRulerTranslateUnit(@NonNull Context context, int annotType, String extraTag) {
        return Tool.getToolPreferences(context).getString(
            getRulerTranslateUnitKey(annotType, extraTag),
            getDefaultRulerTranslateUnit(context, annotType));
    }

    /**
     * Gets annotation preset style saved in settings
     *
     * @param context   The context
     * @param annotType annotation type
     * @param index     index of the preset
     * @return annotation style
     */
    public AnnotStyle getAnnotPresetStyle(Context context, int annotType, int index) {
        // load preset from settings first
        String presetJSON = PdfViewCtrlSettingsManager.getAnnotStylePreset(context, annotType, index);
        if (!Utils.isNullOrEmpty(presetJSON)) {
            return AnnotStyle.loadJSONString(context, presetJSON, annotType);
        }

        return getDefaultAnnotPresetStyle(context, annotType, index, getPresetsAttr(annotType), getDefaultPresetsArrayRes(annotType));
    }


    /**
     * Gets default annotation preset style defined in attribute, style, and array resource
     *
     * @param context   The context
     * @param annotType annotation type
     * @param index     index of annotation type
     * @param attrRes   attribute resource for annotation preset
     * @param arrayRes  array resource for defining presets of annotation style
     * @return annotation style of preset
     */
    public AnnotStyle getDefaultAnnotPresetStyle(@NonNull Context context, int annotType, int index, @AttrRes int attrRes, @ArrayRes int arrayRes) {
        // load the preset from style attributes

        context = context.getApplicationContext();
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attrRes});
        int presetArrayRes = typedArray.getResourceId(0, arrayRes);
        typedArray.recycle();

        TypedArray presetsArray = context.getResources().obtainTypedArray(presetArrayRes);
        int styleResId = presetsArray.getResourceId(index, getDefaultStyle(annotType));

        presetsArray.recycle();

        AnnotStyle annotStyle = new AnnotStyle();
        annotStyle.setAnnotType(annotType);
        annotStyle.setStrokeColor(getDefaultColor(context, 0, styleResId));
        annotStyle.setFillColor(getDefaultFillColor(context, 0, styleResId));
        annotStyle.setOpacity(getDefaultOpacity(context, 0, styleResId));
        if (annotStyle.isFreeText()) {
            annotStyle.setTextSize(getDefaultTextSize(context, 0, styleResId));
            annotStyle.setTextColor(getDefaultTextColor(context, 0, styleResId));
            annotStyle.setFont(new FontResource(getDefaultFont(context, 0, styleResId)));
        }
        annotStyle.setThickness(getDefaultThickness(context, 0, styleResId));
        if (annotStyle.isStickyNote()) {
            annotStyle.setIcon(getDefaultIcon(context, 0, styleResId));
        } else if (annotStyle.isSound()) {
            annotStyle.setIcon(SoundCreate.SOUND_ICON);
        } else if (annotStyle.isRuler()) {
            annotStyle.setRulerBaseValue(getDefaultRulerBaseValue(context, 0, styleResId));
            annotStyle.setRulerBaseUnit(getDefaultRulerBaseUnit(context, 0, styleResId));
            annotStyle.setRulerTranslateValue(getDefaultRulerTranslateValue(context, 0, styleResId));
            annotStyle.setRulerTranslateUnit(getDefaultRulerTranslateUnit(context, 0, styleResId));
        }
        return annotStyle;

    }

    /**
     * Gets preset color
     *
     * @param context         the context
     * @param index           index of presets
     * @param attrRes         preset attribute resource, get default attr:{@link #getPresetsAttr(int)}
     * @param arrayRes        array resource, get default array resource: {@link #getDefaultPresetsArrayRes(int)}
     * @param defaultStyleRes default style resource, get default style resource: {@link #getDefaultStyle(int)}
     * @return preset color
     */
    public int getPresetColor(@NonNull Context context, int index, @AttrRes int attrRes, @ArrayRes int arrayRes, @StyleRes int defaultStyleRes) {
        context = context.getApplicationContext();
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attrRes});
        int presetArrayRes = typedArray.getResourceId(0, arrayRes);
        typedArray.recycle();

        TypedArray presetsArray = context.getResources().obtainTypedArray(presetArrayRes);
        int styleResId = presetsArray.getResourceId(index, defaultStyleRes);

        presetsArray.recycle();

        return getDefaultColor(context, 0, styleResId);
    }

    /**
     * Gets color key to put in settings
     *
     * @param annotType The annotation mode
     * @param extraTag  extra tag
     * @param mode      mode
     * @return key
     */
    public String getAnnotationPropertySettingsKey(int annotType, String extraTag, String mode) {
        String annotProperty;
        switch (annotType) {
            case Annot.e_Highlight:
                annotProperty = PREF_ANNOTATION_PROPERTY_HIGHLIGHT;
                break;
            case Annot.e_Underline:
                annotProperty = PREF_ANNOTATION_PROPERTY_UNDERLINE;
                break;
            case Annot.e_Link:
                annotProperty = PREF_ANNOTATION_PROPERTY_LINK;
                break;
            case Annot.e_StrikeOut:
                annotProperty = PREF_ANNOTATION_PROPERTY_STRIKEOUT;
                break;
            case Annot.e_Squiggly:
                annotProperty = PREF_ANNOTATION_PROPERTY_SQUIGGLY;
                break;
            case Annot.e_FreeText:
                annotProperty = PREF_ANNOTATION_PROPERTY_FREETEXT;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                annotProperty = PREF_ANNOTATION_PROPERTY_CALLOUT;
                break;
            case Annot.e_Ink:
                annotProperty = PREF_ANNOTATION_PROPERTY_FREEHAND;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
                annotProperty = PREF_ANNOTATION_PROPERTY_ARROW;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                annotProperty = PREF_ANNOTATION_PROPERTY_RULER;
                break;
            case Annot.e_Polyline:
                annotProperty = PREF_ANNOTATION_PROPERTY_POLYLINE;
                break;
            case Annot.e_Square:
                annotProperty = PREF_ANNOTATION_PROPERTY_RECTANGLE;
                break;
            case Annot.e_Circle:
                annotProperty = PREF_ANNOTATION_PROPERTY_OVAL;
                break;
            case Annot.e_Polygon:
                annotProperty = PREF_ANNOTATION_PROPERTY_POLYGON;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
                annotProperty = PREF_ANNOTATION_PROPERTY_CLOUD;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                annotProperty = PREF_ANNOTATION_PROPERTY_SIGNATURE;
                break;
            case Annot.e_Text:
                annotProperty = PREF_ANNOTATION_PROPERTY_NOTE;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER:
                annotProperty = PREF_ANNOTATION_PROPERTY_ERASER;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER:
                annotProperty = PREF_ANNOTATION_PROPERTY_FREE_HIGHLIGHTER;
                break;
            default:
                annotProperty = PREF_ANNOTATION_PROPERTY_LINE;
        }

        return annotProperty + extraTag + mode;
    }
}
