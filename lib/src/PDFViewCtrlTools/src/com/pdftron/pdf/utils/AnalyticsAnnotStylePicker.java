package com.pdftron.pdf.utils;

import com.pdftron.pdf.controls.AnnotStyleDialogFragment;

/**
 * @hide
 */
public class AnalyticsAnnotStylePicker {

    private static AnalyticsAnnotStylePicker _INSTANCE;
    final private static Object sLock = new Object();

    public static AnalyticsAnnotStylePicker getInstance() {
        synchronized (sLock) {
            if (_INSTANCE == null) {
                _INSTANCE = new AnalyticsAnnotStylePicker();
            }
        }
        return _INSTANCE;
    }


    private @AnalyticsHandlerAdapter.StylePickerOpenedLocation int  mOpenedFromLocation;
    private String mOpenedAnnotation = "unknown";
    private @AnnotStyleDialogFragment.SelectColorMode int mSelectedColorMode;
    private int mPresetIndex = -1;
    private boolean mHasAction = false;

    private AnalyticsAnnotStylePicker() {
        reset();
    }

    public void showAnnotStyleDialog(@AnalyticsHandlerAdapter.StylePickerOpenedLocation int from, String annotation) {
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_OPEN,
            AnalyticsParam.stylePickerBasicParam(from, annotation));
        setOpenedAnnotation(annotation);
        setOpenedLocation(from);
    }

    public void dismissAnnotStyleDialog() {
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_OPEN);
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_CLOSE,
            AnalyticsParam.stylePickerCloseParam(mHasAction, mOpenedFromLocation, mOpenedAnnotation));
        reset();
    }

    private void reset() {
        mOpenedFromLocation = -1;
        mOpenedAnnotation = "unknown";
        mSelectedColorMode = -1;
        mPresetIndex = -1;
        mHasAction = false;
    }

    public int getOpenedFromLocation() {
        return mOpenedFromLocation;
    }

    private void setOpenedLocation(@AnalyticsHandlerAdapter.StylePickerOpenedLocation int from) {
        mOpenedFromLocation = from;
    }

    private void setOpenedAnnotation(String annotation) {
        mOpenedAnnotation = annotation;
    }

    public void setSelectedColorMode(@AnnotStyleDialogFragment.SelectColorMode int colorMode) {
        mSelectedColorMode = colorMode;
    }

    public void setPresetIndex(int presetIndex) {
        mPresetIndex = presetIndex;
    }

    public void selectColor(String color, @AnalyticsHandlerAdapter.StylePickerLocation int picker) {
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_SELECT_COLOR,
            AnalyticsParam.stylePickerSelectColorParam(color, picker, mSelectedColorMode, mOpenedFromLocation, mOpenedAnnotation,  mPresetIndex));
        mHasAction = true;
    }


    public void selectThickness(float thickness) {
        AnalyticsHandlerAdapter.getInstance().sendEvent(
            AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_SELECT_THICKNESS,
            AnalyticsParam.stylePickerSelectThicknessParam(thickness, mOpenedFromLocation, mOpenedAnnotation,  mPresetIndex));
        mHasAction = true;
    }


    public void selectOpacity(float opacity) {
        AnalyticsHandlerAdapter.getInstance().sendEvent(
            AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_SELECT_OPACITY,
            AnalyticsParam.stylePickerSelectOpacityParam(opacity, mOpenedFromLocation, mOpenedAnnotation, mPresetIndex));
        mHasAction = true;
    }


    public void selectTextSize(float textSize) {
        AnalyticsHandlerAdapter.getInstance().sendEvent(
            AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_SELECT_TEXT_SIZE,
            AnalyticsParam.stylePickerSelectTextSizeParam(textSize, mOpenedFromLocation, mOpenedAnnotation, mPresetIndex));
        mHasAction = true;
    }

    public void selectRulerBaseValue(float base) {
        AnalyticsHandlerAdapter.getInstance().sendEvent(
                AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_SELECT_RULER_BASE_VALUE,
                AnalyticsParam.stylePickerSelectRulerBaseParam(base, mOpenedFromLocation, mOpenedAnnotation, mPresetIndex));
        mHasAction = true;
    }

    public void selectRulerTranslateValue(float translate) {
        AnalyticsHandlerAdapter.getInstance().sendEvent(
                AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_SELECT_RULER_BASE_VALUE,
                AnalyticsParam.stylePickerSelectRulerTranslateParam(translate, mOpenedFromLocation, mOpenedAnnotation, mPresetIndex));
        mHasAction = true;
    }

    public void selectPreset(int presetIndex, boolean isInDefaultStyles) {
        AnalyticsHandlerAdapter.getInstance().sendEvent(
            AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_SELECT_PRESET,
            AnalyticsParam.stylePickerSelectPresetParam(mOpenedFromLocation, mOpenedAnnotation, presetIndex, isInDefaultStyles));
        setPresetIndex(presetIndex);
        mHasAction = true;
    }

    public void deselectPreset(int presetIndex) {
        AnalyticsHandlerAdapter.getInstance().sendEvent(
            AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_DESELECT_PRESET,
            AnalyticsParam.stylePickerDeselectPresetParam(mOpenedFromLocation, mOpenedAnnotation, presetIndex));
        setPresetIndex(-1);
        mHasAction = true;
    }

}
