//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------
package com.pdftron.pdf.controls;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.TooltipCompat;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.asynctask.LoadFontAsyncTask;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsAnnotStylePicker;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.AnnotationPropertyPreviewView;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.ExpandableGridView;
import com.pdftron.pdf.utils.FontAdapter;
import com.pdftron.pdf.utils.IconPickerGridViewAdapter;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.UnitConverter;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A LinearLayout that can adjust annotation appearance
 */
public class AnnotStyleView extends LinearLayout implements
    TextView.OnEditorActionListener,
    SeekBar.OnSeekBarChangeListener,
    View.OnFocusChangeListener,
    View.OnClickListener,
    GridView.OnItemClickListener,
    AdapterView.OnItemSelectedListener {

    public static final String SOUND_ICON_OUTLINE = "annotation_icon_sound_outline";
    public static final String SOUND_ICON_FILL = "annotation_icon_sound_fill";

    private static final int MAX_PROGRESS = 100;
    private static final int PRESET_SIZE = 4;
    private int mAnnotType = Annot.e_Unknown;
    private Set<String> mWhiteListFonts;
    // View elements
    // more tool types
    private LinearLayout mMoreToolLayout;

    // stroke
    private LinearLayout mStrokeLayout;
    private TextView mStrokeColorTextView;
    private AnnotationPropertyPreviewView mStrokePreview;

    // fill
    private LinearLayout mFillLayout;
    private TextView mFillColorTextView;
    private AnnotationPropertyPreviewView mFillPreview;

    // thickness
    private LinearLayout mThicknessLayout;
    private SeekBar mThicknessSeekbar;
    private EditText mThicknessEditText;
    private LinearLayout mThicknessValueGroup;

    // opacity
    private LinearLayout mOpacityLayout;
    private TextView mOpacityTextView;
    private SeekBar mOpacitySeekbar;
    private EditText mOpacityEditText;
    private LinearLayout mOpacityValueGroup;

    // fonts
    private LinearLayout mFontLayout;
    private Spinner mFontSpinner;
    private FontAdapter mFontAdapter;

    // text color
    private LinearLayout mTextColorLayout;
    private AnnotationPropertyPreviewView mTextColorPreview;

    // text size
    private LinearLayout mTextSizeLayout;
    private SeekBar mTextSizeSeekbar;
    private EditText mTextSizeEditText;

    // icons
    private LinearLayout mIconLayout;
    private ImageView mIconExpandableBtn;
    private ExpandableGridView mIconExpandableGridView;
    private IconPickerGridViewAdapter mIconAdapter;
    private AnnotationPropertyPreviewView mIconPreview;

    // ruler unit
    private LinearLayout mRulerUnitLayout;
    private EditText mRulerBaseEditText;
    private Spinner mRulerBaseSpinner;
    private ArrayAdapter<CharSequence> mRulerBaseSpinnerAdapter;
    private EditText mRulerTranslateEditText;
    private Spinner mRulerTranslateSpinner;
    private ArrayAdapter<CharSequence> mRulerTranslateSpinnerAdapter;

    // presets
    private AnnotationPropertyPreviewView[] mPresetViews = new AnnotationPropertyPreviewView[PRESET_SIZE];
    private AnnotStyle[] mPresetStyles = new AnnotStyle[PRESET_SIZE];
    private OnPresetSelectedListener mPresetSelectedListner;

    // attributes

    private float mMaxThickness;
    private float mMinThickness;
    private float mMaxTextSize;
    private float mMinTextSize;
    private boolean mPrevThicknessFocus = false;
    private boolean mPrevOpacityFocus = false;
    private boolean mInitSpinner = true;

    private AnnotStyle.AnnotStyleHolder mAnnotStyleHolder;

    private ArrayList<Integer> mMoreAnnotTypes;
    private OnMoreAnnotTypeClickedListener mMoreAnnotTypeListener;

    // listener
    private OnColorLayoutClickedListener mColorLayoutClickedListener;
    private boolean mTextJustChanged;

    /**
     * Class constructor
     */
    public AnnotStyleView(Context context) {
        this(context, null);
    }

    /**
     * Class constructor
     */
    public AnnotStyleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Class constructor
     */
    public AnnotStyleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * set annotation type
     */
    public void setAnnotType(int annotType) {
        mAnnotType = annotType;
        mMaxThickness = ToolStyleConfig.getInstance().getDefaultMaxThickness(getContext(), annotType);
        mMinThickness = ToolStyleConfig.getInstance().getDefaultMinThickness(getContext(), annotType);
        mMinTextSize = ToolStyleConfig.getInstance().getDefaultMinTextSize(getContext());
        mMaxTextSize = ToolStyleConfig.getInstance().getDefaultMaxTextSize(getContext());

        mAnnotStyleHolder.getAnnotPreview().setAnnotType(mAnnotType);

        if (mAnnotType == Annot.e_FreeText ||
            mAnnotType == AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT) {
            mStrokePreview.setAnnotType(mAnnotType);
            // set up font spinner if it is TEXT_CREATE tool
            setupFontSpinner();
        }
        if (mAnnotType == Annot.e_Text) {
            // get list of note icons
            List<String> source = ToolStyleConfig.getInstance().getIconsList(getContext());
            mIconAdapter = new IconPickerGridViewAdapter(getContext(), source);
            mIconExpandableGridView.setAdapter(mIconAdapter);
            mIconExpandableGridView.setOnItemClickListener(this);
        }

        loadPresets();
    }

    /**
     * Load presets from settings
     */
    private void loadPresets() {
        for (int i = 0; i < PRESET_SIZE; i++) {
            AnnotationPropertyPreviewView presetView = mPresetViews[i];
            AnnotStyle preset = ToolStyleConfig.getInstance().getAnnotPresetStyle(getContext(), mAnnotType, i);
            presetView.setAnnotType(mAnnotType);
            preset.bindPreview(presetView);
            if (!preset.getFont().hasFontName() && mFontAdapter != null && mFontAdapter.getData() != null && mFontAdapter.getData().size() > 1) {
                preset.setFont(mFontAdapter.getData().get(1));
            }
            mPresetStyles[i] = preset;
        }
    }

    /**
     * Sets annotation style holder
     *
     * @param annotStyleHolder annotation style holder
     */
    public void setAnnotStyleHolder(AnnotStyle.AnnotStyleHolder annotStyleHolder) {
        mAnnotStyleHolder = annotStyleHolder;
    }

    private AnnotStyle getAnnotStyle() {
        return mAnnotStyleHolder.getAnnotStyle();
    }

    private void setIcon(String icon) {
        getAnnotStyle().setIcon(icon);
        int iconPosition = mIconAdapter.getItemIndex(icon);
        mIconAdapter.setSelected(iconPosition);
        mAnnotStyleHolder.getAnnotPreview().setImageDrawable(getAnnotStyle().getIconDrawable(getContext()));
        mIconPreview.setImageDrawable(AnnotStyle.getIconDrawable(getContext(), getAnnotStyle().getIcon(), getAnnotStyle().getColor(), 1));
    }

    /**
     * Sets white font list for Free text fonts spinner
     *
     * @param whiteFontList white font list
     */
    public void setWhiteFontList(Set<String> whiteFontList) {
        mWhiteListFonts = whiteFontList;
        if (!checkPresets()) {
            setFontSpinner();
        }
    }

    /**
     * Sets more tools to display
     *
     * @param annotTypes The tools to display
     */
    public void setMoreAnnotTypes(ArrayList<Integer> annotTypes) {
        mMoreAnnotTypes = annotTypes;
        // clear more tool layout except the first one
        View firstView = mMoreToolLayout.getChildAt(0);
        mMoreToolLayout.removeAllViews();
        mMoreToolLayout.addView(firstView);
        for (final Integer type : mMoreAnnotTypes) {
            AppCompatImageButton imageButton = getAnnotTypeButtonForTool(type);
            imageButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!v.isSelected() && mMoreAnnotTypeListener != null) {
                        mMoreAnnotTypeListener.onAnnotTypeClicked(type);
                    }
                }
            });
            mMoreToolLayout.addView(imageButton);
        }
        mMoreToolLayout.setVisibility(annotTypes.isEmpty() ? GONE : VISIBLE);
    }

    public void updateAnnotTypes() {
        if (mMoreAnnotTypes == null || mMoreAnnotTypes.isEmpty()) {
            return;
        }
        int annotTypeIndex = mMoreAnnotTypes.indexOf(mAnnotType);

        int childCount = mMoreToolLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mMoreToolLayout.getChildAt(i);
            if (child instanceof ImageButton) {
                child.setSelected(i == (annotTypeIndex + 1));
            }
        }
    }

    private void setFont(FontResource font) {
        getAnnotStyle().setFont(font);
        setFontSpinner();
    }


    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.controls_annotation_styles, this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setOrientation(VERTICAL);

        // stroke layout
        mStrokeLayout = findViewById(R.id.stroke_color_layout);
        mStrokeColorTextView = findViewById(R.id.stroke_color_textivew);
        mStrokePreview = findViewById(R.id.stroke_preview);

        // more tools
        mMoreToolLayout = findViewById(R.id.more_tools_layout);

        // fill layout
        mFillLayout = findViewById(R.id.fill_color_layout);
        mFillColorTextView = findViewById(R.id.fill_color_textview);
        mFillPreview = findViewById(R.id.fill_preview);

        // thickness layout
        mThicknessLayout = findViewById(R.id.thickness_layout);
        mThicknessSeekbar = findViewById(R.id.thickness_seekbar);
        mThicknessEditText = findViewById(R.id.thickness_edit_text);
        mThicknessValueGroup = findViewById(R.id.thickness_value_group);

        // opacity layout
        mOpacityLayout = findViewById(R.id.opacity_layout);
        mOpacityTextView = findViewById(R.id.opacity_textivew);
        mOpacitySeekbar = findViewById(R.id.opacity_seekbar);
        mOpacityEditText = findViewById(R.id.opacity_edit_text);
        mOpacityValueGroup = findViewById(R.id.opacity_value_group);

        // icons
        mIconLayout = findViewById(R.id.icon_layout);
        mIconExpandableBtn = findViewById(R.id.icon_expandable_btn);
        mIconExpandableGridView = findViewById(R.id.icon_grid);
        mIconPreview = findViewById(R.id.icon_preview);
        mIconExpandableGridView.setExpanded(true);
        mIconLayout.setOnClickListener(this);

        // font layout
        mFontLayout = findViewById(R.id.font_layout);
        mFontSpinner = findViewById(R.id.font_dropdown);

        // text color layout
        mTextColorLayout = findViewById(R.id.text_color_layout);
        mTextColorPreview = findViewById(R.id.text_color_preview);
        mTextColorPreview.setAnnotType(Annot.e_FreeText);
        mTextColorLayout.setOnClickListener(this);

        // text size layout
        mTextSizeLayout = findViewById(R.id.text_size_layout);
        mTextSizeSeekbar = findViewById(R.id.text_size_seekbar);
        mTextSizeEditText = findViewById(R.id.text_size_edit_text);
        mTextSizeSeekbar.setOnSeekBarChangeListener(this);
        mTextSizeEditText.setOnFocusChangeListener(this);
        mTextSizeEditText.setOnEditorActionListener(this);

        // ruler unit layout
        mRulerUnitLayout = findViewById(R.id.ruler_unit_layout);
        mRulerBaseEditText = findViewById(R.id.ruler_base_edit_text);
        mRulerBaseEditText.setText("1.0");
        mRulerBaseSpinner = findViewById(R.id.ruler_base_unit_spinner);
        mRulerBaseSpinnerAdapter = ArrayAdapter.createFromResource(getContext(),
            R.array.ruler_base_unit, android.R.layout.simple_spinner_item);
        mRulerBaseSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRulerBaseSpinner.setAdapter(mRulerBaseSpinnerAdapter);
        mRulerBaseSpinner.setOnItemSelectedListener(this);
        mRulerTranslateEditText = findViewById(R.id.ruler_translate_edit_text);
        mRulerTranslateEditText.setText("1.0");
        mRulerTranslateSpinner = findViewById(R.id.ruler_translate_unit_spinner);
        mRulerTranslateSpinnerAdapter = ArrayAdapter.createFromResource(getContext(),
            R.array.ruler_translate_unit, android.R.layout.simple_spinner_item);
        mRulerTranslateSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRulerTranslateSpinner.setAdapter(mRulerTranslateSpinnerAdapter);
        mRulerTranslateSpinner.setOnItemSelectedListener(this);

        // presets
        mPresetViews[0] = findViewById(R.id.preset1);
        mPresetViews[1] = findViewById(R.id.preset2);
        mPresetViews[2] = findViewById(R.id.preset3);
        mPresetViews[3] = findViewById(R.id.preset4);
        // background
        TypedArray typedArray = getContext().obtainStyledAttributes(new int[]{R.attr.colorBackgroundLight});
        int background = typedArray.getColor(0, getContext().getResources().getColor(R.color.controls_annot_style_preview_bg));
        typedArray.recycle();

        for (AnnotationPropertyPreviewView presetView : mPresetViews) {
            presetView.setOnClickListener(this);
            presetView.setParentBackgroundColor(background);
        }

        // listeners
        mStrokeLayout.setOnClickListener(this);
        mFillLayout.setOnClickListener(this);

        mThicknessSeekbar.setOnSeekBarChangeListener(this);
        mOpacitySeekbar.setOnSeekBarChangeListener(this);

        mThicknessEditText.setOnEditorActionListener(this);
        mOpacityEditText.setOnEditorActionListener(this);
        mRulerBaseEditText.setOnEditorActionListener(this);
        mRulerTranslateEditText.setOnEditorActionListener(this);

        mThicknessEditText.setOnFocusChangeListener(this);
        mOpacityEditText.setOnFocusChangeListener(this);
        mRulerBaseEditText.setOnFocusChangeListener(this);
        mRulerTranslateEditText.setOnFocusChangeListener(this);

        mThicknessValueGroup.setOnClickListener(this);
        mOpacityValueGroup.setOnClickListener(this);
    }

    /**
     * hide this annotation style view
     */
    public void dismiss() {
        setVisibility(GONE);
    }

    /**
     * Show this view
     */
    public void show() {
        setVisibility(VISIBLE);
        initLayoutStyle();
    }

    /**
     * Check if there is any annotation preset matches current annotation style.
     * If true, then call {@link OnPresetSelectedListener#onPresetSelected(AnnotStyle)}
     *
     * @return true then there is matched preset, false otherwise
     */
    public boolean checkPresets() {
        int i = 0;
        for (AnnotStyle preset : mPresetStyles) {
            if (preset == null) {
                break;
            }
            if (preset != getAnnotStyle() && preset.equals(getAnnotStyle())) {
                if (mPresetSelectedListner != null) {
                    // end initialize layout style if listener is not null
                    mPresetSelectedListner.onPresetSelected(preset);
                    AnalyticsAnnotStylePicker.getInstance().setPresetIndex(i);
                    return true;
                }
            }
            i++;
        }
        return false;
    }

    private void initLayoutStyle() {
        mAnnotStyleHolder.getAnnotPreview().updateFillPreview(getAnnotStyle());

        int backgroundColor = Utils.getBackgroundColor(getContext());

        // set stroke preview
        Drawable drawable;
        if (getAnnotStyle().getColor() == Color.TRANSPARENT) {
            drawable = getContext().getResources().getDrawable(R.drawable.oval_fill_transparent);
        } else if (getAnnotStyle().getColor() == backgroundColor) {
            if (getAnnotStyle().hasFillColor()) {
                drawable = getContext().getResources().getDrawable(R.drawable.ring_stroke_preview);
            } else {
                drawable = getContext().getResources().getDrawable(R.drawable.oval_stroke_preview);
            }

            drawable.mutate();
            ((GradientDrawable) drawable).setStroke((int) Utils.convDp2Pix(getContext(), 1), Color.GRAY);
        } else {
            if (getAnnotStyle().hasFillColor()) {
                drawable = getContext().getResources().getDrawable(R.drawable.oval_stroke_preview);
            } else {
                drawable = getContext().getResources().getDrawable(R.drawable.oval_fill_preview);
            }
            drawable.mutate();
            drawable.setColorFilter(getAnnotStyle().getColor(), PorterDuff.Mode.SRC_IN);
        }
        mStrokePreview.setImageDrawable(drawable);

        // set fill preview drawable
        if (getAnnotStyle().getFillColor() != backgroundColor) {
            int fillDrawableRes = getAnnotStyle().getFillColor() == Color.TRANSPARENT ? R.drawable.oval_fill_transparent : R.drawable.oval_fill_preview;
            Drawable fillDrawable = getContext().getResources().getDrawable(fillDrawableRes);
            if (fillDrawableRes != R.drawable.oval_fill_transparent) {
                fillDrawable.mutate();
                fillDrawable.setColorFilter(getAnnotStyle().getFillColor(), PorterDuff.Mode.SRC_IN);
            }
            mFillPreview.setImageDrawable(fillDrawable);
        } else {
            GradientDrawable fillDrawable = (GradientDrawable) getContext().getResources().getDrawable(R.drawable.oval_stroke_preview);
            fillDrawable.mutate();
            fillDrawable.setStroke((int) Utils.convDp2Pix(getContext(), 1), Color.GRAY);
            mFillPreview.setImageDrawable(fillDrawable);
        }

        // set thickness
        if (getAnnotStyle().hasThickness()) {
            String annotThickness = String.format(getContext().getString(R.string.tools_misc_thickness), getAnnotStyle().getThickness());
            if (!mThicknessEditText.getText().toString().equals(annotThickness)) {
                mThicknessEditText.setText(annotThickness);
            }
            mTextJustChanged = true;
            mThicknessSeekbar.setProgress(Math.round((getAnnotStyle().getThickness() - mMinThickness) / (mMaxThickness - mMinThickness) * MAX_PROGRESS));
        }

        // set text size, text color,
        if (getAnnotStyle().isFreeText()) {
            String textSizeStr = getContext().getString(R.string.tools_misc_textsize, (int) getAnnotStyle().getTextSize());
            if (!mTextSizeEditText.getText().toString().equals(textSizeStr)) {
                mTextSizeEditText.setText(textSizeStr);
            }
            mTextJustChanged = true;
            mTextSizeSeekbar.setProgress(Math.round((getAnnotStyle().getTextSize() - mMinTextSize) / (mMaxTextSize - mMinTextSize) * MAX_PROGRESS));
            mTextColorPreview.updateFillPreview(Color.TRANSPARENT, Color.TRANSPARENT, 0, 1);
            mTextColorPreview.updateFreeTextStyle(getAnnotStyle().getTextColor(), 1);
            setFont(getAnnotStyle().getFont());
        }

        // set opacity
        if (getAnnotStyle().hasOpacity()) {
            int progress = (int) (getAnnotStyle().getOpacity() * MAX_PROGRESS);
            mOpacityEditText.setText(String.valueOf(progress));
            mTextJustChanged = true;
            mOpacitySeekbar.setProgress(progress);
        }

        // set sticky note icon
        if (getAnnotStyle().hasIcon()) {
            if (!Utils.isNullOrEmpty(getAnnotStyle().getIcon())) {
                mAnnotStyleHolder.getAnnotPreview().setImageDrawable(getAnnotStyle().getIconDrawable(getContext()));
                if (mIconAdapter != null) {
                    mIconAdapter.setSelected(mIconAdapter.getItemIndex(getAnnotStyle().getIcon()));
                }
                mIconPreview.setImageDrawable(AnnotStyle.getIconDrawable(getContext(), getAnnotStyle().getIcon(), getAnnotStyle().getColor(), 1));
            }
            if (mIconAdapter != null) {
                mIconAdapter.updateIconColor(getAnnotStyle().getColor());
                mIconAdapter.updateIconOpacity(getAnnotStyle().getOpacity());
            }
        }

        // set ruler measures
        if (getAnnotStyle().isRuler()) {
            mRulerBaseEditText.setText(String.valueOf(getAnnotStyle().getRulerBaseValue()));
            if (getAnnotStyle().getRulerBaseUnit().equals(UnitConverter.CM)) {
                mRulerBaseSpinner.setSelection(0);
            } else if (getAnnotStyle().getRulerBaseUnit().equals(UnitConverter.INCH)) {
                mRulerBaseSpinner.setSelection(1);
            }
            mRulerTranslateEditText.setText(String.valueOf(getAnnotStyle().getRulerTranslateValue()));
            if (getAnnotStyle().getRulerTranslateUnit().equals(UnitConverter.CM)) {
                mRulerTranslateSpinner.setSelection(0);
            } else if (getAnnotStyle().getRulerTranslateUnit().equals(UnitConverter.INCH)) {
                mRulerTranslateSpinner.setSelection(1);
            } else if (getAnnotStyle().getRulerTranslateUnit().equals(UnitConverter.YARD)) {
                mRulerTranslateSpinner.setSelection(2);
            }
        }
    }

    /**
     * Deselect all annotation presets preview button
     */
    public void deselectAllPresetsPreview() {
        // Check presets
        for (AnnotStyle preset : mPresetStyles) {
            if (preset != null) {
                AnnotationPropertyPreviewView preview = preset.getBindedPreview();
                if (preview != null) {
                    preview.setSelected(false);
                }
            }
        }
    }

    private void setupFontSpinner() {
        // get supported languages
        ArrayList<FontResource> fonts = new ArrayList<>();
        FontResource loading = new FontResource(getContext().getString(R.string.free_text_fonts_loading), "", "", "");
        fonts.add(loading);
        // set font spinner
        mFontAdapter = new FontAdapter(getContext().getApplicationContext(), R.layout.fonts_row_item, fonts);
        mFontAdapter.setDropDownViewResource(R.layout.fonts_row_item);

        mFontSpinner.setAdapter(mFontAdapter);
        mFontSpinner.setOnItemSelectedListener(this);

        LoadFontAsyncTask fontAsyncTask = new LoadFontAsyncTask(getContext(), mWhiteListFonts);
        fontAsyncTask.setCallback(new LoadFontAsyncTask.Callback() {
            @Override
            public void onFinish(ArrayList<FontResource> fonts) {
                FontResource fontHint = new FontResource(getContext().getString(R.string.free_text_fonts_prompt), "", "", "");
                // add hint to fonts - "Pick Font" will display as the default
                // value in the spinner
                // the previous first font is "Loading fonts...", remove it
                fonts.add(0, fontHint);
                mFontAdapter.setData(fonts);
                if (getAnnotStyle() != null && getAnnotStyle().getFont() != null) {
                    setFontSpinner();
                }
                setPresetFonts(fonts);
            }
        });
        fontAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    private void setPresetFonts(ArrayList<FontResource> fonts) {
        for (AnnotStyle preset : mPresetStyles) {
            boolean fontFound = false;
            for (FontResource font : fonts) {
                if (preset.getFont().equals(font)) {
                    preset.setFont(font);
                    fontFound = true;
                    break;
                }
            }
            if (!fontFound) {
                // if no font found for presets, then set the first font to preset.(0th font is "Pick font")
                preset.setFont(fonts.get(1));
            }
        }
        checkPresets();
    }

    private void setFontSpinner() {
        if (mFontAdapter != null && mFontAdapter.getData() != null && mFontSpinner != null) {
            // check if font name matches a font
            Boolean matchFound = false;
            // if opening an existing free text annotation, the
            // only piece of info we have about the font is the font name
            if (getAnnotStyle().getFont().hasFontName()) {
                for (int i = 0; i < mFontAdapter.getData().size(); i++) {
                    if (mFontAdapter.getData().get(i).getFontName().equals(getAnnotStyle().getFont().getFontName())) {
                        mFontSpinner.setSelection(i);
                        matchFound = true;
                        break;
                    }
                }
                // if opening the free text property popup from tool bar, the
                // the only piece of info we have about the font is the pdftron font name
            } else if (getAnnotStyle().getFont().hasPDFTronName()) {
                for (int i = 0; i < mFontAdapter.getData().size(); i++) {
                    if (mFontAdapter.getData().get(i).getPDFTronName().equals(getAnnotStyle().getFont().getPDFTronName())) {
                        mFontSpinner.setSelection(i);
                        matchFound = true;
                        break;
                    }
                }
            }

            if (!matchFound) {
                mFontSpinner.setSelection(0);
            } else {
                int index = mFontSpinner.getSelectedItemPosition();
                FontResource fontResource = mFontAdapter.getItem(index);
                if (fontResource != null && !Utils.isNullOrEmpty(fontResource.getFilePath())) {
                    mAnnotStyleHolder.getAnnotPreview().setFontPath(fontResource.getFilePath());
                }
            }
        }
    }

    private void setPreviewThickness() {
        setPreviewThickness(getAnnotStyle().getThickness());
    }

    private void setPreviewThickness(float thickness) {
        mAnnotStyleHolder.getAnnotPreview().updateFillPreview(getAnnotStyle().getColor(), getAnnotStyle().getFillColor(), thickness, getAnnotStyle().getOpacity());
    }


    private void setPreviewOpacity() {
        setPreviewOpacity(getAnnotStyle().getOpacity());
    }

    private void setPreviewOpacity(float opacity) {
        mAnnotStyleHolder.getAnnotPreview().updateFillPreview(getAnnotStyle().getColor(), getAnnotStyle().getFillColor(),
            getAnnotStyle().getThickness(), opacity);

        if (getAnnotStyle().isStickyNote()) {
            mIconAdapter.updateIconOpacity(opacity);
        }
    }

    private void setPreviewTextSize() {
        setPreviewTextSize(getAnnotStyle().getTextSize());
    }

    private void setPreviewTextSize(float textSize) {
        mAnnotStyleHolder.getAnnotPreview().updateFreeTextStyle(getAnnotStyle().getTextColor(), textSize / mMaxTextSize);
    }

    private void updateUIVisibility() {
        mMoreToolLayout.setVisibility(mMoreAnnotTypes == null || mMoreAnnotTypes.isEmpty() ? GONE : VISIBLE);
        mStrokeLayout.setVisibility(getAnnotStyle().hasColor() ? VISIBLE : GONE);
        mFillLayout.setVisibility(getAnnotStyle().hasFillColor() ? VISIBLE : GONE);
        mThicknessLayout.setVisibility(getAnnotStyle().hasThickness() ? VISIBLE : GONE);
        mOpacityLayout.setVisibility(getAnnotStyle().hasOpacity() ? VISIBLE : GONE);
        mFontLayout.setVisibility(getAnnotStyle().isFreeText() ? VISIBLE : GONE);
        mIconLayout.setVisibility(getAnnotStyle().isStickyNote() ? VISIBLE : GONE);
        mTextSizeLayout.setVisibility(getAnnotStyle().isFreeText() ? VISIBLE : GONE);
        mTextColorLayout.setVisibility(getAnnotStyle().isFreeText() ? VISIBLE : GONE);
        mRulerUnitLayout.setVisibility(getAnnotStyle().isRuler() ? VISIBLE : GONE);
    }

    /**
     * Sets color layout clicked listener, color layout includes {stroke color layout, fill color layout, and text color layout}
     *
     * @param listener The color layout clicked listener
     */
    public void setOnColorLayoutClickedListener(OnColorLayoutClickedListener listener) {
        mColorLayoutClickedListener = listener;
    }

    /**
     * hide keyboard and remove edit text focus when {@link EditorInfo#IME_ACTION_DONE} is clicked
     *
     * @param v        text view
     * @param actionId action id
     * @param event    key event
     * @return Return true if you have consumed the action, else false.
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            Utils.hideSoftKeyboard(getContext(), v);
            v.clearFocus();
            mAnnotStyleHolder.getAnnotPreview().requestFocus();
            return true;
        }
        return false;
    }

    /**
     * When slider progress changes, set corresponding value to annotation style
     *
     * @param seekBar  seek bar
     * @param progress progress value
     * @param fromUser True if the progress change was initiated by the user.
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (mTextJustChanged) {
            mTextJustChanged = false;
            return;
        }
        if (seekBar.getId() == mThicknessSeekbar.getId()) {
            float thickness = (mMaxThickness - mMinThickness) * progress / MAX_PROGRESS + mMinThickness;
            getAnnotStyle().setThickness(thickness, false);
            mThicknessEditText.setText(String.format(getContext().getString(R.string.tools_misc_thickness), thickness));
            // change preview based on thickness
            setPreviewThickness(thickness);

        } else if (seekBar.getId() == mOpacitySeekbar.getId()) {
            float opacity = (float) progress / MAX_PROGRESS;
            getAnnotStyle().setOpacity(opacity, false);
            mOpacityEditText.setText(String.valueOf(progress));
            // change preview based on thickness
            setPreviewOpacity(opacity);

        } else if (seekBar.getId() == mTextSizeSeekbar.getId()) {
            int textSize = Math.round((mMaxTextSize - mMinTextSize) * progress / MAX_PROGRESS + mMinTextSize);
            getAnnotStyle().setTextSize(textSize, false);
            mTextSizeEditText.setText(getContext().getString(R.string.tools_misc_textsize, textSize));
            setPreviewTextSize(textSize);

        }
    }

    /**
     * Overload implementation of {@link android.widget.SeekBar.OnSeekBarChangeListener#onStartTrackingTouch(SeekBar)}
     *
     * @param seekBar seek bar
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
//        getAnnotStyle().disableUpdateListener(true);
    }

    /**
     * Overload implementation of {@link android.widget.SeekBar.OnSeekBarChangeListener#onStopTrackingTouch(SeekBar)}
     *
     * @param seekBar seek bar
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        if (seekBar.getId() == mThicknessSeekbar.getId()) {
            float thickness = (mMaxThickness - mMinThickness) * progress / MAX_PROGRESS + mMinThickness;
            getAnnotStyle().setThickness(thickness);
            mThicknessEditText.setText(String.format(getContext().getString(R.string.tools_misc_thickness), thickness));
            // change preview based on thickness
            setPreviewThickness();

            AnalyticsAnnotStylePicker.getInstance().selectThickness(thickness);

        } else if (seekBar.getId() == mOpacitySeekbar.getId()) {
            getAnnotStyle().setOpacity((float) progress / MAX_PROGRESS);
            mOpacityEditText.setText(String.valueOf(progress));
            // change preview based on thickness
            setPreviewOpacity();

            AnalyticsAnnotStylePicker.getInstance().selectOpacity(getAnnotStyle().getOpacity());

        } else if (seekBar.getId() == mTextSizeSeekbar.getId()) {
            int textSize = Math.round((mMaxTextSize - mMinTextSize) * progress / MAX_PROGRESS + mMinTextSize);
            getAnnotStyle().setTextSize(textSize);
            mTextSizeEditText.setText(getContext().getString(R.string.tools_misc_textsize, textSize));
            setPreviewTextSize();

            AnalyticsAnnotStylePicker.getInstance().selectTextSize(textSize);
        }
    }

    /**
     * Callback method when there is a focus change
     * in {@link #mThicknessEditText} and {@link #mOpacityEditText},
     * When focus removed, set corresponding thickness or opacity
     *
     * @param v        focus changed view
     * @param hasFocus whether the view has focus
     */
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        mTextJustChanged = true;
        if (v.getId() == mThicknessEditText.getId()) {
            if (!hasFocus && mPrevThicknessFocus) {
                Editable s = mThicknessEditText.getText();
                String number = s.toString();
                try {
                    // some soft keyboards let the usage of comma instead of decimal point
                    number = number.replace(",", ".");
                    float value = Float.valueOf(number);

                    if (value > getAnnotStyle().getMaxInternalThickness()) {
                        value = getAnnotStyle().getMaxInternalThickness();
                        mThicknessEditText.setText(getContext().getString(R.string.tools_misc_thickness, value));
                    }
                    getAnnotStyle().setThickness(value);
                    int progress = Math.round(getAnnotStyle().getThickness() / (mMaxThickness - mMinThickness) * MAX_PROGRESS);
                    mThicknessSeekbar.setProgress(progress);
                    setPreviewThickness();
                    AnalyticsAnnotStylePicker.getInstance().selectThickness(value);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "annot style invalid number");
                    CommonToast.showText(getContext(), R.string.invalid_number);
                }
            }
            mPrevThicknessFocus = hasFocus;
        } else if (v.getId() == mOpacityEditText.getId()) {
            if (!hasFocus && mPrevOpacityFocus) {
                try {
                    float value = Float.valueOf(mOpacityEditText.getText().toString());
                    if (value > MAX_PROGRESS) {
                        value = MAX_PROGRESS;
                        mOpacityEditText.setText(String.valueOf(value));
                    }
                    getAnnotStyle().setOpacity(value / MAX_PROGRESS);
                    mOpacitySeekbar.setProgress((int) value);
                    setPreviewOpacity();
                    AnalyticsAnnotStylePicker.getInstance().selectThickness(getAnnotStyle().getOpacity());
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "annot style invalid number");
                    CommonToast.showText(getContext(), R.string.invalid_number);
                }
            }
            mPrevOpacityFocus = hasFocus;

        } else if (v.getId() == mTextSizeEditText.getId() && !hasFocus) {
            Editable s = mTextSizeEditText.getText();
            String number = s.toString();
            try {
                float value = Float.valueOf(number);
                value = Math.round(value);
                getAnnotStyle().setTextSize(value);
                int progress = Math.round(getAnnotStyle().getTextSize() / (mMaxTextSize - mMinTextSize) * MAX_PROGRESS);
                mTextSizeSeekbar.setProgress(progress);
                setPreviewTextSize();
                AnalyticsAnnotStylePicker.getInstance().selectThickness(value);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "annot style invalid number");
                CommonToast.showText(getContext(), R.string.invalid_number);
            }
        } else if (v.getId() == mRulerBaseEditText.getId() && !hasFocus) {
            Editable s = mRulerBaseEditText.getText();
            String number = s.toString();
            try {
                float value = Float.valueOf(number);
                if (value < 0.1) {
                    value = 0.1f;
                    mRulerBaseEditText.setText("0.1");
                }
                getAnnotStyle().setRulerBaseValue(value);
                AnalyticsAnnotStylePicker.getInstance().selectRulerBaseValue(value);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "annot style invalid number");
                CommonToast.showText(getContext(), R.string.invalid_number);
            }
        } else if (v.getId() == mRulerTranslateEditText.getId() && !hasFocus) {
            Editable s = mRulerTranslateEditText.getText();
            String number = s.toString();
            try {
                float value = Float.valueOf(number);
                if (value < 0.1) {
                    value = 0.1f;
                    mRulerTranslateEditText.setText("0.1");
                }
                getAnnotStyle().setRulerTranslateValue(value);
                AnalyticsAnnotStylePicker.getInstance().selectRulerTranslateValue(value);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "annot style invalid number");
                CommonToast.showText(getContext(), R.string.invalid_number);
            }
        }
        if (!hasFocus) {
            Utils.hideSoftKeyboard(getContext(), v);
        }
    }

    /**
     * Callback method invoked when child view clicked
     *
     * @param v clicked view
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == mThicknessValueGroup.getId()) {
            Utils.showSoftKeyboard(getContext(), mThicknessEditText);
            mThicknessEditText.requestFocus();
        } else if (v.getId() == mOpacityValueGroup.getId()) {
            Utils.showSoftKeyboard(getContext(), mOpacityTextView);
            mOpacityEditText.requestFocus();
        } else if (v.getId() == mIconLayout.getId()) {
            boolean isGridViewVisible = mIconExpandableGridView.getVisibility() == VISIBLE;
            mIconExpandableGridView.setVisibility(isGridViewVisible ? GONE : VISIBLE);
            mIconExpandableBtn.setImageResource(isGridViewVisible ?
                R.drawable.ic_chevron_right_black_24dp : R.drawable.ic_arrow_down_white_24dp);

        } else if (v.getId() == mStrokeLayout.getId() && mColorLayoutClickedListener != null) {
            int colorMode = getAnnotStyle().hasFillColor() ? AnnotStyleDialogFragment.STROKE_COLOR : AnnotStyleDialogFragment.COLOR;
            mColorLayoutClickedListener.onColorLayoutClicked(colorMode);

        } else if (v.getId() == mTextColorLayout.getId() && mColorLayoutClickedListener != null) {
            mColorLayoutClickedListener.onColorLayoutClicked(AnnotStyleDialogFragment.TEXT_COLOR);

        } else if (v.getId() == mFillLayout.getId() && mColorLayoutClickedListener != null) {
            mColorLayoutClickedListener.onColorLayoutClicked(AnnotStyleDialogFragment.FILL_COLOR);

        } else {
            for (int i = 0; i < PRESET_SIZE; i++) {
                View presetView = mPresetViews[i];
                AnnotStyle presetStyle = mPresetStyles[i];
                if (v.getId() == presetView.getId() && mPresetSelectedListner != null) {
                    if (v.isSelected()) {
                        mPresetSelectedListner.onPresetDeselected(presetStyle);
                        AnalyticsAnnotStylePicker.getInstance().deselectPreset(i);
                    } else {
                        mPresetSelectedListner.onPresetSelected(presetStyle);
                        AnalyticsAnnotStylePicker.getInstance().selectPreset(i, isAnnotStyleInDefaults(presetStyle));
                        break;
                    }
                }
            }
        }
    }

    private boolean isAnnotStyleInDefaults(AnnotStyle annotStyle) {
        for (int i = 0; i < PRESET_SIZE; i++) {
            AnnotStyle defaultStyle = ToolStyleConfig.getInstance().getDefaultAnnotPresetStyle(
                getContext(), mAnnotType, i, ToolStyleConfig.getInstance().getPresetsAttr(mAnnotType),
                ToolStyleConfig.getInstance().getDefaultPresetsArrayRes(mAnnotType));
            if (defaultStyle.equals(annotStyle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update layout
     */
    public void updateLayout() {
        if (getAnnotStyle().isFreeText()) {
            mFillColorTextView.setText(R.string.pref_colormode_custom_bg_color);
        } else if (!getAnnotStyle().hasFillColor()) {
            mStrokeColorTextView.setText(R.string.tools_qm_color);
        } else {
            mStrokeColorTextView.setText(R.string.tools_qm_stroke_color);
        }
        updateUIVisibility();
        initLayoutStyle();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) {
            updateLayout();
        }
    }

    /**
     * Save annotation presets to settings
     */
    public void savePresets() {
        for (int i = 0; i < PRESET_SIZE; i++) {
            AnnotStyle preset = mPresetStyles[i];
            PdfViewCtrlSettingsManager.setAnnotStylePreset(getContext(), mAnnotType, i, preset.toJSONString());
        }
    }

    /**
     * Gets color based on color mode
     *
     * @param colorMode color mode, must be one of {@link AnnotStyleDialogFragment#STROKE_COLOR}
     *                  or {@link AnnotStyleDialogFragment#FILL_COLOR}
     * @return color
     */
    @SuppressLint("SwitchIntDef")
    public @ColorInt
    int getColor(@AnnotStyleDialogFragment.SelectColorMode int colorMode) {
        switch (colorMode) {
            case AnnotStyleDialogFragment.FILL_COLOR:
                return getAnnotStyle().getFillColor();
            default:
                return getAnnotStyle().getColor();
        }
    }

    /**
     * Called when icon grid item clicked
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this
     *                 will be a view provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String item = mIconAdapter.getItem(position);
        mIconAdapter.setSelected(position);
        setIcon(item);
    }

    /**
     * Called when selected one item from font spinner
     *
     * @param parent   Font spinner
     * @param view     selected view
     * @param position the position of the view in the adapter
     * @param id       the row id of the item that was selected
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == mFontSpinner.getId()) {
            if (position >= 0 && mFontAdapter != null) {
                FontResource font = mFontAdapter.getItem(position);
                if (font != null && !mInitSpinner) {
                    setFont(font);
                } else if (mInitSpinner) {
                    mInitSpinner = false;
                }
            }
        } else if (parent.getId() == mRulerBaseSpinner.getId()) {
            if (position >= 0 && mRulerBaseSpinnerAdapter != null) {
                CharSequence unit = mRulerBaseSpinnerAdapter.getItem(position);
                if (unit != null) {
                    getAnnotStyle().setRulerBaseUnit(unit.toString());
                }
            }
        } else if (parent.getId() == mRulerTranslateSpinner.getId()) {
            if (position >= 0 && mRulerTranslateSpinnerAdapter != null) {
                CharSequence unit = mRulerTranslateSpinnerAdapter.getItem(position);
                if (unit != null) {
                    getAnnotStyle().setRulerTranslateUnit(unit.toString());
                }
            }
        }
    }


    /**
     * Callback method to be invoked when the selection disappears from font spinner
     *
     * @param parent The AdapterView that now contains no selected item.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /**
     * Sets on annotation preset selected listener
     *
     * @param listener annotation preset selected listener
     */
    public void setOnPresetSelectedListener(OnPresetSelectedListener listener) {
        mPresetSelectedListner = listener;
    }

    /**
     * Sets a listener to listen more annot type click event
     *
     * @param listener The listener
     */
    public void setOnMoreAnnotTypesClickListener(OnMoreAnnotTypeClickedListener listener) {
        mMoreAnnotTypeListener = listener;
    }

    private AppCompatImageButton getAnnotTypeButtonForTool(int annotType) {
        AppCompatImageButton imageButton = new AppCompatImageButton(getContext());
        imageButton.setImageResource(AnnotUtils.getAnnotImageResId(annotType));
        imageButton.setBackgroundResource(R.drawable.annot_property_preview_bg);

        imageButton.setAlpha(0.54f);
        imageButton.setColorFilter(Utils.getThemeAttrColor(getContext(), android.R.attr.textColorPrimary));
        String text = AnnotUtils.getAnnotTypeAsString(getContext(), annotType);
        TooltipCompat.setTooltipText(imageButton, text);
        imageButton.setContentDescription(text);
        imageButton.setLayoutParams(
            new RelativeLayout.LayoutParams(
                getContext().getResources().getDimensionPixelSize(R.dimen.quick_menu_button_size),
                getContext().getResources().getDimensionPixelSize(R.dimen.quick_menu_button_size)));
        imageButton.setPadding(getContext().getResources().getDimensionPixelSize(R.dimen.quick_menu_button_padding),
            getContext().getResources().getDimensionPixelSize(R.dimen.quick_menu_button_padding),
            getContext().getResources().getDimensionPixelSize(R.dimen.quick_menu_button_padding),
            getContext().getResources().getDimensionPixelSize(R.dimen.quick_menu_button_padding));
        if (annotType == getAnnotStyle().getAnnotType()) {
            imageButton.setSelected(true);
        }
        return imageButton;
    }

    /**
     * This interface is for switching between color picker and style picker
     */
    public interface OnColorLayoutClickedListener {
        /**
         * This method is invoked when clicked on stroke color layout or fill color layout
         *
         * @param colorMode clicked color layout, has to be one of
         *                  {@link AnnotStyleDialogFragment#STROKE_COLOR}
         *                  or {@link AnnotStyleDialogFragment#FILL_COLOR}
         */
        void onColorLayoutClicked(@AnnotStyleDialogFragment.SelectColorMode int colorMode);
    }

    /**
     * This interface is for listening preset style buttons pressed event
     */
    public interface OnPresetSelectedListener {
        /**
         * This method is invoked when preset button is selected
         *
         * @param presetStyle presetStyle
         */
        void onPresetSelected(AnnotStyle presetStyle);


        /**
         * This method is invoked when preset button is de-selected
         *
         * @param presetStyle presetStyle
         */
        void onPresetDeselected(AnnotStyle presetStyle);
    }

    public interface OnMoreAnnotTypeClickedListener {
        void onAnnotTypeClicked(int annotType);
    }

}
