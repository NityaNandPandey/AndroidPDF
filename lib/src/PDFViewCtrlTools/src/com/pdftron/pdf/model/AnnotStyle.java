//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.model;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.RestrictTo;
import android.support.v4.content.ContextCompat;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.annots.Text;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.controls.AnnotStyleView;
import com.pdftron.pdf.tools.CloudCreate;
import com.pdftron.pdf.tools.SoundCreate;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotationPropertyPreviewView;
import com.pdftron.pdf.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class contains annotation style information
 */
public class AnnotStyle {

    private static String KEY_ANNOT_TYPE = "annotType";
    private static String KEY_THICKNESS = "thickness";
    private static String KEY_STROKE_COLOR = "strokeColor";
    private static String KEY_FILL_COLOR = "fillColor";
    private static String KEY_OPACITY = "opacity";
    private static String KEY_ICON = "icon";
    private static String KEY_TEXT_SIZE = "textSize";
    private static String KEY_TEXT_COLOR = "textColor";
    private static String KEY_FONT_PATH = "fontPath";
    private static String KEY_FONT_NAME = "fontName";
    private static String KEY_PDFTRON_NAME = "pdftronName";
    public static String KEY_PDFTRON_RULER = "pdftronRuler";
    public static String KEY_RULER_BASE = "rulerBase";
    public static String KEY_RULER_BASE_UNIT = "rulerBaseUnit";
    public static String KEY_RULER_TRANSLATE = "rulerTranslate";
    public static String KEY_RULER_TRANSLATE_UNIT = "rulerTranslateUnit";

    /**
     * The constant represents arrow annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_ARROW = 1001;

    /**
     * The constant represents signature annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_SIGNATURE = 1002;

    /**
     * The constant represents eraser annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_ERASER = 1003;

    /**
     * The constant represents free highlighter annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_FREE_HIGHLIGHTER = 1004;

    /**
     * The constant represents cloud annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_CLOUD = 1005;

    /**
     * The constant represents arrow annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_RULER = 1006;
    /**
     * The constant represents callout annotation type
     */
    public static final int CUSTOM_ANNOT_TYPE_CALLOUT = 1007;


    private static final String ANALYTICS_ANNOT_STYLE_CHANGED = "annotation property %s changed: %s";

    private float mThickness;
    private float mTextSize;
    private int mTextColor;
    private String mTextContent = "";
    private @ColorInt
    int mStrokeColor;
    private @ColorInt
    int mFillColor;
    private float mOpacity;
    private double mBorderEffectIntensity = CloudCreate.BORDER_INTENSITY;
    private String mIcon = "";
    private OnAnnotStyleChangeListener mAnnotChangeListener;
    private boolean mUpdateListener = true;
    private AnnotationPropertyPreviewView mPreview;

    private FontResource mFont = new FontResource("");

    private int mAnnotType = Annot.e_Unknown;

    private boolean mHasAppearance;

    private RulerItem mRuler = new RulerItem();
    private RulerItem mRulerCopy;


    /**
     * Class constructor
     */
    public AnnotStyle() {
    }


    /**
     * Class constructor
     *
     * @param other other annotation style to copy from
     */
    public AnnotStyle(AnnotStyle other) {
        mThickness = other.getThickness();
        mTextSize = other.mTextSize;
        mStrokeColor = other.getColor();
        mFillColor = other.getFillColor();
        mIcon = other.getIcon();
        mOpacity = other.getOpacity();
        mAnnotChangeListener = other.mAnnotChangeListener;
        mUpdateListener = other.mUpdateListener;
        mPreview = other.mPreview;
        mFont = other.getFont();
        mAnnotType = other.getAnnotType();
        mTextColor = other.mTextColor;
        mRuler = other.mRuler;
    }

    /**
     * Convert annotation style to string in json format
     *
     * @return json format string
     */
    public String toJSONString() {
        try {
            JSONObject object = new JSONObject();
            object.put(KEY_ANNOT_TYPE, String.valueOf(mAnnotType));
            object.put(KEY_THICKNESS, String.valueOf(mThickness));
            object.put(KEY_STROKE_COLOR, mStrokeColor);
            object.put(KEY_FILL_COLOR, mFillColor);
            object.put(KEY_OPACITY, String.valueOf(mOpacity));
            if (hasIcon()) {
                object.put(KEY_ICON, mIcon);
            }
            if (isFreeText()) {
                object.put(KEY_TEXT_SIZE, String.valueOf(mTextSize));
                object.put(KEY_TEXT_COLOR, mTextColor);
                object.put(KEY_FONT_PATH, mFont.getFilePath());
                object.put(KEY_FONT_NAME, mFont.getFontName());
                object.put(KEY_PDFTRON_NAME, mFont.getPDFTronName());
            }
            if (isRuler()) {
                object.put(KEY_RULER_BASE, String.valueOf(mRuler.mRulerBase));
                object.put(KEY_RULER_BASE_UNIT, mRuler.mRulerBaseUnit);
                object.put(KEY_RULER_TRANSLATE, String.valueOf(mRuler.mRulerTranslate));
                object.put(KEY_RULER_TRANSLATE_UNIT, mRuler.mRulerTranslateUnit);
            }
            return object.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Convert JSON String to annotation style
     *
     * @param jsonStr   json string
     * @return annotation style
     */
    public static AnnotStyle loadJSONString(String jsonStr) {
        return loadJSONString(null, jsonStr, -1);
    }

    /**
     * Convert JSON String to annotation style
     *
     * @param context   the context
     * @param jsonStr   json string
     * @param annotType annotation type
     * @return annotation style
     */
    public static AnnotStyle loadJSONString(Context context, String jsonStr, int annotType) {
        AnnotStyle annotStyle = new AnnotStyle();
        if (context != null && annotType > -1) {
            annotStyle = ToolStyleConfig.getInstance().getDefaultAnnotStyle(context, annotType);
        }
        if (!Utils.isNullOrEmpty(jsonStr)) {
            try {
                JSONObject object = new JSONObject(jsonStr);
                if (object.has(KEY_ANNOT_TYPE)) {
                    annotStyle.setAnnotType(Integer.valueOf(object.getString(KEY_ANNOT_TYPE)));
                }
                if (object.has(KEY_THICKNESS)) {
                    annotStyle.setThickness(Float.valueOf(object.getString(KEY_THICKNESS)));
                }
                if (object.has(KEY_STROKE_COLOR)) {
                    annotStyle.setStrokeColor(object.getInt(KEY_STROKE_COLOR));
                }
                if (object.has(KEY_FILL_COLOR)) {
                    annotStyle.setFillColor(object.getInt(KEY_FILL_COLOR));
                }
                if (object.has(KEY_OPACITY)) {
                    annotStyle.setOpacity(Float.valueOf(object.getString(KEY_OPACITY)));
                }
                if (object.has(KEY_TEXT_SIZE)) {
                    annotStyle.setTextSize(Float.valueOf(object.getString(KEY_TEXT_SIZE)));
                }
                if (object.has(KEY_TEXT_COLOR)) {
                    annotStyle.setTextColor(object.getInt(KEY_TEXT_COLOR));
                }
                if (object.has(KEY_ICON)) {
                    String icon = object.getString(KEY_ICON);
                    if (!Utils.isNullOrEmpty(icon)) {
                        annotStyle.setIcon(icon);
                    }
                }
                if (object.has(KEY_FONT_PATH)) {
                    String fontPath = object.getString(KEY_FONT_PATH);
                    if (!Utils.isNullOrEmpty(fontPath)) {
                        FontResource f = new FontResource("");
                        f.setFilePath(fontPath);
                        if (object.has(KEY_FONT_NAME)) {
                            String fontName = object.getString(KEY_FONT_NAME);
                            if (!Utils.isNullOrEmpty(fontName)) {
                                f.setFontName(fontName);
                            }
                        }

                        if (object.has(KEY_PDFTRON_NAME)) {
                            String pdftronName = object.getString(KEY_PDFTRON_NAME);
                            if (!Utils.isNullOrEmpty(pdftronName)) {
                                f.setPDFTronName(pdftronName);
                                if (!f.hasFontName()) {
                                    f.setFontName(pdftronName);
                                }
                            }
                        }
                        annotStyle.setFont(f);
                    }
                }
                if (object.has(KEY_RULER_BASE) &&
                    object.has(KEY_RULER_BASE_UNIT) &&
                    object.has(KEY_RULER_TRANSLATE) &&
                    object.has(KEY_RULER_TRANSLATE_UNIT)) {
                    annotStyle.setRulerBaseValue(Float.valueOf(object.getString(KEY_RULER_BASE)));
                    annotStyle.setRulerBaseUnit(object.getString(KEY_RULER_BASE_UNIT));
                    annotStyle.setRulerTranslateValue(Float.valueOf(object.getString(KEY_RULER_TRANSLATE)));
                    annotStyle.setRulerTranslateUnit(object.getString(KEY_RULER_TRANSLATE_UNIT));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "Failed converting annotStype from json to object");
            }
        }

        return annotStyle;
    }

    /**
     * Sets annotation type, can be obtained from {@link Annot#getType()}
     *
     * @param annotType annotation type
     */
    public void setAnnotType(int annotType) {
        mAnnotType = annotType;
        if (mPreview != null) {
            mPreview.setAnnotType(mAnnotType);
        }
    }

    /**
     * Sets annotation style
     *
     * @param strokeColor stroke color
     * @param fillColor   fill color
     * @param thickness   stroke thickness
     * @param opacity     opacity
     */
    public void setStyle(@ColorInt int strokeColor, @ColorInt int fillColor, float thickness, float opacity) {
        mStrokeColor = strokeColor;
        mFillColor = fillColor;
        mThickness = thickness;
        mOpacity = opacity;
        updatePreviewStyle();
    }

    /**
     * Copy other annotation style
     *
     * @param other other annotation style
     */
    public void setStyle(AnnotStyle other) {
        setStrokeColor(other.getColor());
        setFillColor(other.getFillColor());
        setThickness(other.getThickness());
        setOpacity(other.getOpacity());
        setIcon(other.getIcon());
        setFont(other.getFont());
        setTextSize(other.getTextSize());
        setTextColor(other.getTextColor());
    }

    /**
     * Sets stroke color.
     * For normal annotation, stroke color can be obtained from {@link  Annot#getColorAsRGB()};
     * For {@link com.pdftron.pdf.annots.FreeText} annotation, stroke color can be obtained from {@link  FreeText#getLineColor()}
     *
     * @param strokeColor stroke color
     */
    public void setStrokeColor(@ColorInt int strokeColor) {
        updateStrokeColorListener(strokeColor);
        mStrokeColor = strokeColor;
        updatePreviewStyle();
    }

    /**
     * Sets fill color.
     * For {@link com.pdftron.pdf.annots.Markup} annotation, fill color can be obtained from {@link  Markup#getInteriorColor()};
     * For {@link FreeText} annotation, fill color can be obtained from {@link  FreeText#getColorAsRGB()}
     *
     * @param fillColor fill color
     */
    public void setFillColor(@ColorInt int fillColor) {
        updateFillColorListener(fillColor);
        mFillColor = fillColor;
        updatePreviewStyle();
    }

    /**
     * Sets stroke thickness.
     * Annotation stroke thickness can be obtained from {@link  Annot.BorderStyle#getWidth()}
     *
     * @param thickness border style thickness
     */
    public void setThickness(float thickness) {
        setThickness(thickness, true);
    }

    /**
     * Sets stroke thickness.
     * Annotation stroke thickness can be obtained from {@link  Annot.BorderStyle#getWidth()}
     *
     * @param thickness border style thickness
     * @param done done sliding
     */
    public void setThickness(float thickness, boolean done) {
        updateThicknessListener(thickness, done);
        mThickness = thickness;
        updatePreviewStyle();
    }

    /**
     * Sets text size for {@link FreeText} annotation.
     * text size can be obtained from {@link  FreeText#getFontSize()}
     *
     * @param textSize text size
     */
    public void setTextSize(float textSize) {
        setTextSize(textSize, true);
    }

    /**
     * Sets text size for {@link FreeText} annotation.
     * text size can be obtained from {@link  FreeText#getFontSize()}
     * @param textSize text size
     * @param done done sliding
     */
    public void setTextSize(float textSize, boolean done) {
        updateTextSizeListener(textSize, done);
        mTextSize = textSize;
        updatePreviewStyle();
    }

    /**
     * Sets text color for {@link FreeText} annotation.
     * text color can be obtained from {@link  FreeText#getTextColor()}
     *
     * @param textColor text color
     */
    public void setTextColor(@ColorInt int textColor) {
        updateTextColorListener(textColor);
        mTextColor = textColor;
        updatePreviewStyle();
    }

    /**
     * Sets content of {@link FreeText} annotation.
     * @param content content
     */
    public void setTextContent(String content) {
        if (null != content) {
            mTextContent = content;
        }
    }

    /**
     * Sets opacity for {@link Markup} annotation.
     * Opacity can be obtained from {@link Markup#getOpacity()}
     *
     * @param opacity opacity
     */
    public void setOpacity(float opacity) {
        setOpacity(opacity, true);
    }

    /**
     * Sets opacity for {@link Markup} annotation.
     * Opacity can be obtained from {@link Markup#getOpacity()}
     *
     * @param opacity opacity
     * @param done done sliding
     */
    public void setOpacity(float opacity, boolean done) {
        updateOpacityListener(opacity, done);
        mOpacity = opacity;
        updatePreviewStyle();
    }

    /**
     * Sets the border effect intensity
     * @param intensity border effect intensity
     */
    public void setBorderEffectIntensity(double intensity) {
        mBorderEffectIntensity = intensity;
    }

    /**
     * Gets the border effect intensity
     * @return border effect intensity
     */
    public double getBorderEffectIntensity() {
        return mBorderEffectIntensity;
    }

    /**
     * Sets ruler properties
     * @param ruler the ruler
     */
    public void setRulerItem(RulerItem ruler) {
        mRuler = ruler;
    }

    /**
     * Sets ruler base value
     * @param value the value
     */
    public void setRulerBaseValue(float value) {
        updateRulerBaseValueListener(value);
        mRuler.mRulerBase = value;
    }

    /**
     * Sets ruler translate value
     * @param value the value
     */
    public void setRulerTranslateValue(float value) {
        updateRulerTranslateValueListener(value);
        mRuler.mRulerTranslate = value;
    }

    /**
     * Sets ruler base unit
     * @param unit the unit
     */
    public void setRulerBaseUnit(String unit) {
        updateRulerBaseUnitListener(unit);
        mRuler.mRulerBaseUnit = unit;
    }

    /**
     * Sets ruler translate unit
     * @param unit the unit
     */
    public void setRulerTranslateUnit(String unit) {
        updateRulerTranslateUnitListener(unit);
        mRuler.mRulerTranslateUnit = unit;
    }

    /**
     * Gets the ruler item
     * @return the ruler item
     */
    public RulerItem getRulerItem() {
        return mRuler;
    }

    /**
     * Gets the ruler base value
     * @return the value
     */
    public float getRulerBaseValue() {
        return mRuler.mRulerBase;
    }

    /**
     * Gets the ruler translate value
     * @return the value
     */
    public float getRulerTranslateValue() {
        return mRuler.mRulerTranslate;
    }

    /**
     * Gets the ruler base unit
     * @return the unit
     */
    public String getRulerBaseUnit() {
        return mRuler.mRulerBaseUnit;
    }

    /**
     * Gets the ruler translate unit
     * @return the unit
     */
    public String getRulerTranslateUnit() {
        return mRuler.mRulerTranslateUnit;
    }

    /**
     * @hide
     */
    public void setHasAppearance(boolean hasAppearance) {
        mHasAppearance = hasAppearance;
    }

    /**
     * @hide
     */
    public boolean hasAppearance() {
        return mHasAppearance;
    }

    /**
     * Sets icon for {@link com.pdftron.pdf.annots.Text} annotation.
     * Icon can be obtained from {@link Text#getIconName()}
     *
     * @param icon icon
     */
    public void setIcon(String icon) {
        if (!Utils.isNullOrEmpty(icon)) {
            updateIconListener(icon);
            mIcon = icon;
            updatePreviewStyle();
        }
    }

    /**
     * Sets font for {@link com.pdftron.pdf.annots.FreeText} annotation.
     * Font can be obtained from calling
     * <pre>
     * {@code
     *     Obj freeTextObj = freeText.getSDFObj();
     *     Obj drDict = freeTextObj.findObj("DR");
     *
     *     if (drDict != null && drDict.isDict()) {
     *       Obj fontDict = drDict.findObj("Font");
     *
     *       if (fontDict != null && fontDict.isDict()) {
     *         DictIterator fItr = fontDict.getDictIterator();
     *
     *         if (fItr.hasNext()) {
     *           Font f = new Font(fItr.value());
     *           String fontName = f.getName();
     *           FontResource font = new FontResource(fontName);
     *         }
     *       }
     *     }
     * }
     * </pre>
     *
     * @param font font
     */
    public void setFont(FontResource font) {
        updateFontListener(font);
        mFont = font;
        updatePreviewStyle();
    }

    /**
     * Disable update annotation change listener
     *
     * @param disable true then disable, false otherwise
     */
    public void disableUpdateListener(boolean disable) {
        mUpdateListener = !disable;
    }

    /**
     * Gets color of annotation style, for annotation style has border, it means stroke color
     *
     * @return color
     */
    public int getColor() {
        return mStrokeColor;
    }

    /**
     * Gets stroke thickness of annotation style
     *
     * @return thickness
     */
    public float getThickness() {
        return mThickness;
    }

    /**
     * Gets text size of free text annotation style
     *
     * @return text size
     */
    public float getTextSize() {
        return mTextSize;
    }

    /**
     * Gets text content of free text annotation
     * @return content
     */
    public String getTextContent() {
        return mTextContent;
    }

    /**
     * Gets text color of free text annotation style
     *
     * @return text color
     */
    public int getTextColor() {
        return mTextColor;
    }

    /**
     * Gets fill color of annotation style
     *
     * @return fill color
     */
    public int getFillColor() {
        return mFillColor;
    }

    /**
     * Gets opacity of annotation style
     *
     * @return opacity
     */
    public float getOpacity() {
        return mOpacity;
    }

    /**
     * Gets Icon for StickyNote {@link Text} annotation
     *
     * @return icon name
     */
    public String getIcon() {
        return mIcon;
    }

    /**
     * Gets font for free text annotation
     *
     * @return font resource
     */
    public FontResource getFont() {
        return mFont;
    }

    /**
     * Gets PDFTron font name for free text annotation.
     * This is equivalent to calling {@code annotStyle.getFont().getPDFTronName(); }
     *
     * @return font pdftron font name
     */
    public String getPDFTronFontName() {
        if (mFont != null) {
            return mFont.getPDFTronName();
        }
        return null;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    public int getAnnotType() {
        return mAnnotType;
    }

    /**
     * Whether annotation style has border thickness
     *
     * @return true then annotation has border thickness, false otherwise
     */
    public boolean hasThickness() {
        switch (mAnnotType) {
            case Annot.e_Highlight:
            case Annot.e_Text:
            case Annot.e_Sound:
                return false;
            default:
                return true;
        }
    }

    /**
     * Whether annotation style has fill color
     *
     * @return true then annotation has fill color, false otherwise
     */
    public boolean hasFillColor() {
        switch (mAnnotType) {
            case Annot.e_Circle:
            case Annot.e_Square:
            case Annot.e_Polygon:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD:
            case Annot.e_FreeText:
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style has color
     *
     * @return true then annotation has color, false otherwise
     */
    public boolean hasColor() {
        switch (mAnnotType) {
            case CUSTOM_ANNOT_TYPE_ERASER:
                return false;
            default:
                return true;
        }
    }

    /**
     * Whether annotation style has opacity
     *
     * @return true then annotation has opacity, false otherwise
     */
    public boolean hasOpacity() {
        switch (mAnnotType) {
            case CUSTOM_ANNOT_TYPE_ERASER:
            case CUSTOM_ANNOT_TYPE_SIGNATURE:
            case Annot.e_Link:
            case Annot.e_Sound:
                return false;
            default:
                return true;
        }
    }

    /**
     * Whether annotation style has icon
     * @return true then annotation has icon, false otherwise
     */
    public boolean hasIcon() {
        switch (mAnnotType) {
            case Annot.e_Text:
            case Annot.e_Sound:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is sticky note
     *
     * @return true then annotation is sticky note, false otherwise
     */
    public boolean isStickyNote() {
        switch (mAnnotType) {
            case Annot.e_Text:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is sound
     *
     * @return true then annotation is sound, false otherwise
     */
    public boolean isSound() {
        switch (mAnnotType) {
            case Annot.e_Sound:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is freetext annotation
     *
     * @return true then annotation is freetext annotation, false otherwise
     */
    public boolean isFreeText() {
        switch (mAnnotType) {
            case Annot.e_FreeText:
            case CUSTOM_ANNOT_TYPE_CALLOUT:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is text markup annotation style
     *
     * @return true then annotation is text markup annotation style, false otherwise
     */
    public boolean isTextMarkup() {
        switch (mAnnotType) {
            case Annot.e_Highlight:
            case Annot.e_Underline:
            case Annot.e_Squiggly:
            case Annot.e_StrikeOut:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is line annotation
     *
     * @return true then annotation is line annotation, false otherwise
     */
    public boolean isLine() {
        switch (mAnnotType) {
            case Annot.e_Line:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether annotation style is ruler
     *
     * @return true then annotation is line annotation, false otherwise
     */
    public boolean isRuler() {
        switch (mAnnotType) {
            case CUSTOM_ANNOT_TYPE_RULER:
                return true;
            default:
                return false;
        }
    }

    /**
     * Gets font path of font.
     * This is equivalent to calling {@code annotStyle.getFont().getFontPath();}
     *
     * @return font path
     */
    public String getFontPath() {
        if (mFont != null) {
            return mFont.getFilePath();
        }

        return null;
    }

    /**
     * Gets icon drawable
     *
     * @param context The context to get resource
     * @return drawable
     */
    public Drawable getIconDrawable(Context context) {
        return getIconDrawable(context, mIcon, mStrokeColor, mOpacity);
    }

    /**
     * Gets icon drawable based on icon name, opacity
     *
     * @param context           The context to get resources
     * @param iconOutline       name of icon outline
     * @param iconFill         name of icon fill
     * @param opacity           opacity of icon. from [0, 1]
     * @return drawable
     */
    public static Drawable getIconDrawable(Context context, String iconOutline, String iconFill, int color, float opacity) {
        int alpha = (int) (255 * opacity);
        int iconOutlineID = context.getResources().getIdentifier(iconOutline, "drawable", context.getPackageName());
        int iconFillID = context.getResources().getIdentifier(iconFill, "drawable", context.getPackageName());
        if (iconOutlineID != 0 && iconFillID != 0) {
            try {
                Drawable[] layers = new Drawable[2];
                layers[0] = ContextCompat.getDrawable(context, iconFillID);
                layers[0].mutate();
                layers[0].setAlpha(alpha);
                layers[0].setColorFilter(color, PorterDuff.Mode.SRC_IN);
                layers[1] = ContextCompat.getDrawable(context, iconOutlineID);
                layers[1].mutate();
                layers[1].setAlpha(alpha);
                LayerDrawable layerDrawable = new LayerDrawable(layers);
                return layerDrawable;
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, iconFillID + ", " + iconOutlineID);
            }

        }
        return null;
    }

    /**
     * Gets icon drawable based on icon name, opacity
     *
     * @param context The context to get resources
     * @param icon    icon name
     * @param opacity opacity of icon. from [0, 1]
     * @return drawable
     */
    public static Drawable getIconDrawable(Context context, String icon, int color, float opacity) {
        String iconOutline, iconFill;
        if (icon.equals(SoundCreate.SOUND_ICON)) {
            iconOutline = AnnotStyleView.SOUND_ICON_OUTLINE;
            iconFill = AnnotStyleView.SOUND_ICON_FILL;
        } else {
            iconOutline = com.pdftron.pdf.tools.Tool.ANNOTATION_NOTE_ICON_FILE_PREFIX + icon.toLowerCase() + Tool.ANNOTATION_NOTE_ICON_FILE_POSTFIX_OUTLINE;
            iconFill = Tool.ANNOTATION_NOTE_ICON_FILE_PREFIX + icon.toLowerCase() + Tool.ANNOTATION_NOTE_ICON_FILE_POSTFIX_FILL;
        }
        return getIconDrawable(context, iconOutline, iconFill, color, opacity);
    }

    @Override
    public String toString() {
        return "AnnotStyle{" +
            "mThickness=" + mThickness +
            ", mStrokeColor=" + mStrokeColor +
            ", mFillColor=" + mFillColor +
            ", mOpacity=" + mOpacity +
            ", mIcon='" + mIcon + '\'' +
            ", mFont=" + mFont.toString() +
            ", mRulerBase=" + mRuler.mRulerBase + mRuler.mRulerBaseUnit +
            ", mRulerTranslate=" + mRuler.mRulerTranslate + mRuler.mRulerTranslateUnit +
            '}';
    }

    /**
     * Sets annotation style change listener
     *
     * @param listener annotation style change listener
     */
    public void setAnnotAppearanceChangeListener(OnAnnotStyleChangeListener listener) {
        mAnnotChangeListener = listener;
    }

    private void updateThicknessListener(float thickness, boolean done) {
        updateThicknessListener(thickness, mThickness != thickness, done);
    }

    /**
     * Update thickness to listener
     *
     * @param thickness thickness
     * @param update    whether to invoke listener method
     */
    public void updateThicknessListener(float thickness, boolean update, boolean done) {
        if (mUpdateListener && mAnnotChangeListener != null && (update || done)) {
            mAnnotChangeListener.onChangeAnnotThickness(thickness, done);
        }
    }

    private void updateTextSizeListener(float textSize, boolean done) {
        updateTextSizeListener(textSize, mTextSize != textSize, done);
    }

    /**
     * Update text size to listener
     *
     * @param textSize textSize
     * @param update   whether to invoke listener method
     */
    public void updateTextSizeListener(float textSize, boolean update, boolean done) {
        if (mUpdateListener && mAnnotChangeListener != null && (update || done)) {
            mAnnotChangeListener.onChangeAnnotTextSize(textSize, done);
        }
    }

    private void updateTextColorListener(@ColorInt int textColor) {
        updateTextColorListener(textColor, mTextColor != textColor);
    }

    private void updateTextColorListener(@ColorInt int textColor, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotTextColor(textColor);
        }
    }

    /**
     * Bind a preview view to this annotation style, whenever the annotation style updates, the preview updates
     *
     * @param previewView preview view
     */
    public void bindPreview(AnnotationPropertyPreviewView previewView) {
        mPreview = previewView;
        updatePreviewStyle();
    }

    /**
     * Gets binded preview
     *
     * @return binded preview
     */
    public AnnotationPropertyPreviewView getBindedPreview() {
        return mPreview;
    }

    private void updatePreviewStyle() {
        if (mPreview != null) {
            mPreview.updateFillPreview(this);
        }
    }

    public void updateAllListeners() {
        updateStrokeColorListener(mStrokeColor, true);
        updateFillColorListener(mFillColor, true);
        updateThicknessListener(mThickness, true, true);
        updateOpacityListener(mOpacity, true, true);
        if (isStickyNote() && !Utils.isNullOrEmpty(mIcon)) {
            updateIconListener(mIcon, true);
        }
        if (isFreeText()) {
            updateTextColorListener(mTextColor, true);
            updateTextSizeListener(mTextSize, true);
        }
        if (isFreeText() && !Utils.isNullOrEmpty(mFont.getPDFTronName())) {
            updateFontListener(mFont, true);
        }
        if (isRuler()) {
            updateRulerBaseValueListener(mRuler.mRulerBase, true);
            updateRulerBaseUnitListener(mRuler.mRulerBaseUnit, true);
            updateRulerTranslateValueListener(mRuler.mRulerTranslate, true);
            updateRulerTranslateUnitListener(mRuler.mRulerTranslateUnit, true);
        }
    }

    private void updateStrokeColorListener(@ColorInt int strokeColor) {
        updateStrokeColorListener(strokeColor, strokeColor != mStrokeColor);
    }

    private void updateStrokeColorListener(@ColorInt int strokeColor, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotStrokeColor(strokeColor);
        }
    }

    private void updateFillColorListener(@ColorInt int color) {
        updateFillColorListener(color, color != mFillColor);
    }

    private void updateFillColorListener(@ColorInt int color, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotFillColor(color);
        }
    }

    private void updateOpacityListener(float opacity, boolean done) {
        updateOpacityListener(opacity, opacity != mOpacity, done);
    }

    private void updateOpacityListener(float opacity, boolean update, boolean done) {
        if (mUpdateListener && mAnnotChangeListener != null && (update || done)) {
            mAnnotChangeListener.onChangeAnnotOpacity(opacity, done);
            if (isStickyNote()) {
                updateIconListener(mIcon, update);
            }
        }
    }

    private void updateIconListener(String icon) {
        updateIconListener(icon, !icon.equals(mIcon));
    }

    private void updateIconListener(String icon, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotIcon(icon);
        }
    }

    private void updateFontListener(FontResource font) {
        updateFontListener(font, !font.equals(mFont));
    }

    private void updateFontListener(FontResource font, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            mAnnotChangeListener.onChangeAnnotFont(font);
        }
    }

    private void updateRulerBaseValueListener(float val) {
        updateRulerBaseValueListener(val, val != mRuler.mRulerBase);
    }

    private void updateRulerBaseValueListener(float val, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            if (mRulerCopy == null) {
                mRulerCopy = new RulerItem(mRuler);
            }
            mRulerCopy.mRulerBase = val;
            mAnnotChangeListener.onChangeRulerProperty(mRulerCopy);
        }
    }

    private void updateRulerBaseUnitListener(String val) {
        updateRulerBaseUnitListener(val, !val.equals(mRuler.mRulerBaseUnit));
    }

    private void updateRulerBaseUnitListener(String val, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            if (mRulerCopy == null) {
                mRulerCopy = new RulerItem(mRuler);
            }
            mRulerCopy.mRulerBaseUnit = val;
            mAnnotChangeListener.onChangeRulerProperty(mRulerCopy);
        }
    }

    private void updateRulerTranslateValueListener(float val) {
        updateRulerTranslateValueListener(val, val != mRuler.mRulerTranslate);
    }

    private void updateRulerTranslateValueListener(float val, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            if (mRulerCopy == null) {
                mRulerCopy = new RulerItem(mRuler);
            }
            mRulerCopy.mRulerTranslate = val;
            mAnnotChangeListener.onChangeRulerProperty(mRulerCopy);
        }
    }

    private void updateRulerTranslateUnitListener(String val) {
        updateRulerTranslateUnitListener(val, !val.equals(mRuler.mRulerTranslateUnit));
    }

    private void updateRulerTranslateUnitListener(String val, boolean update) {
        if (mUpdateListener && mAnnotChangeListener != null && update) {
            if (mRulerCopy == null) {
                mRulerCopy = new RulerItem(mRuler);
            }
            mRulerCopy.mRulerTranslateUnit = val;
            mAnnotChangeListener.onChangeRulerProperty(mRulerCopy);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof AnnotStyle) {
            AnnotStyle other = (AnnotStyle) obj;
            boolean styleEquals = other.getThickness() == getThickness()
                && other.getAnnotType() == getAnnotType()
                && other.getOpacity() == getOpacity()
                && other.getColor() == getColor()
                && other.getFillColor() == getFillColor();

            boolean fontEquals = other.getFont().equals(getFont());
            boolean iconEquals = other.getIcon().equals(getIcon());
            boolean annotTypeEquals = getAnnotType() == other.getAnnotType();
            boolean textStyleEquals = other.getTextSize() == getTextSize()
                && other.getTextColor() == getTextColor();

            if (!annotTypeEquals) {
                return false;
            }

            if (isStickyNote()) {
                return iconEquals && other.getOpacity() == getOpacity()
                    && other.getColor() == getColor();
            }

            if (isFreeText()) {
                return fontEquals && textStyleEquals && styleEquals;
            }

            if (isRuler()) {
                return styleEquals
                    && other.getRulerBaseValue() == getRulerBaseValue()
                    && other.getRulerBaseUnit().equals(getRulerBaseUnit())
                    && other.getRulerTranslateValue() == getRulerTranslateValue()
                    && other.getRulerTranslateUnit().equals(getRulerTranslateUnit());
            }

            return styleEquals;
        }
        return super.equals(obj);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public float getMaxInternalThickness() {
        switch (mAnnotType) {
            case Annot.e_Underline:
            case Annot.e_StrikeOut:
            case Annot.e_Highlight:
            case Annot.e_Squiggly:
                return 40;
            case CUSTOM_ANNOT_TYPE_RULER:
                return 10;
            default:
                return 70;
        }
    }

    /**
     * This interface is used for changing annotation appearance
     */
    public interface OnAnnotStyleChangeListener {
        /**
         * The method is invoked when thickness is changed in style picker
         *
         * @param thickness thickness of annotation style.
         */
        void onChangeAnnotThickness(float thickness, boolean done);

        /**
         * The method is invoked when text size is changed in style picker
         *
         * @param textSize text size of annotation style.
         */
        void onChangeAnnotTextSize(float textSize, boolean done);

        /**
         * The method is invoked when text color is changed in style picker
         *
         * @param textColor text color of annotation style.
         */
        void onChangeAnnotTextColor(@ColorInt int textColor);

        /**
         * The method is invoked when opacity is changed in style picker
         *
         * @param opacity opacity of annotation
         */
        void onChangeAnnotOpacity(float opacity, boolean done);

        /**
         * The method is invoked when color is selected
         * If it is {@link com.pdftron.pdf.annots.Text} annotation, it will change sticky note icon color
         * If it is {@link com.pdftron.pdf.annots.FreeText} annoation, it will change text color
         *
         * @param color stroke color/ icon color/ text color of annotation
         */
        void onChangeAnnotStrokeColor(@ColorInt int color);

        /**
         * The method is invoked when fill color is selected
         *
         * @param color fill color of annotation
         */
        void onChangeAnnotFillColor(@ColorInt int color);

        /**
         * The method is invoked when icon is selected in icon picker
         *
         * @param icon icon name of sticky note
         */
        void onChangeAnnotIcon(String icon);

        /**
         * The method is invoked when font resource is selected in font spinner
         *
         * @param font font resource
         */
        void onChangeAnnotFont(FontResource font);

        /**
         * The method is invoked when any of the ruler properties change
         * @param rulerItem the ruler item
         */
        void onChangeRulerProperty(RulerItem rulerItem);
    }

    /**
     * This interface is for holding annotation style
     */
    public interface AnnotStyleHolder {
        /**
         * Abstract method for getting annotation style
         *
         * @return annotation style
         */
        AnnotStyle getAnnotStyle();

        /**
         * Abstract method for getting annotation style preview view
         * @return preview of annotation style
         */
        AnnotationPropertyPreviewView getAnnotPreview();

        /**
         * Abstract method for setting annotation style preview visibility
         */
        void setAnnotPreviewVisibility(int visibility);
    }


}
