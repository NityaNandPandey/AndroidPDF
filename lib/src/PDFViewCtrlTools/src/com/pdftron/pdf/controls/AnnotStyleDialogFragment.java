//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------
package com.pdftron.pdf.controls;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.transition.ChangeBounds;
import android.support.transition.Fade;
import android.support.transition.Slide;
import android.support.transition.TransitionManager;
import android.support.transition.TransitionSet;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsAnnotStylePicker;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotationPropertyPreviewView;
import com.pdftron.pdf.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Annotation style dialog fragment is a {@link DialogFragment} that shows annotation style properties
 * in a bottom sheet. With the style dialog, users can edit annotation styles easily with style presets
 * as well as choosing their own colors through the advanced color picker. The style dialog also provides
 * a recent and a favorite list for quick access.
 * <p>
 * <p>
 * You can show annotation style dialog as follows:
 * <pre>
 * AnnotStyle annotStyle = new AnnotStyle();
 * // set annotation type to annot Style
 * annotStyle.setAnnotType(Annot.e_Square);
 * // set blue stroke, yellow fill color, thickness 5, opacity 0.8 to annotation style
 * annotStyle.setStyle(Color.BLUE, Color.YELLOW, 5, 0.8);
 * AnnotStyleDialogFragment annotStyleDialog = AnnotStyleDialogFragment.newInstance(annotStyle);
 * annotStyleDialog.show(getActivity().getSupportFragmentManager());
 * </pre>
 */
public class AnnotStyleDialogFragment extends DialogFragment implements AnnotStyleView.OnPresetSelectedListener, AnnotStyle.AnnotStyleHolder {

    /**
     * The selected color is the stroke color of annotation.
     */
    public static final int STROKE_COLOR = 0;
    /**
     * The selected color is the fill color of annotation.
     */
    public static final int FILL_COLOR = 1;
    /**
     * The selected color is the text color of {@link com.pdftron.pdf.annots.FreeText} annotation.
     */
    public static final int TEXT_COLOR = 2;

    /**
     * The selected color is the color of annotation obtained from {@link Annot#getColorAsRGB()}
     */
    public static final int COLOR = 3;

    private static String ARGS_KEY_ANNOT_STYLE = "annotStyle";
    private static String ARGS_KEY_WHITE_LIST_FONT = "whiteListFont";
    private static String ARGS_KEY_ANCHOR = "anchor";
    private static String ARGS_KEY_ANCHOR_SCREEN = "anchor_screen";
    private static String ARGS_KEY_MORE_ANNOT_TYPES = "more_tools";

    private AnnotStyleView mAnnotStyleView;
    private ColorPickerView mColorPickerView;
    private CoordinatorLayout.Behavior mDialogBehavior;
    private AnnotStyle mAnnotStyle;
    private BottomSheetCallback mBottomSheetCallback;
    private AnnotStyle.OnAnnotStyleChangeListener mAnnotAppearanceListener;
    private AnnotationPropertyPreviewView mPreview;

    private Set<String> mWhiteFontList;
    private DialogInterface.OnDismissListener mDismissListener;
    private NestedScrollView mBottomSheet;
    private Rect mAnchor;
    private boolean mIsAnchorInScreen;
    private ArrayList<Integer> mMoreAnnotTypes;
    private AnnotStyleView.OnMoreAnnotTypeClickedListener mMoreAnnotTypesClickListener;

    /**
     * Creates a new instance of AnnotStyleDialogFragment
     *
     * @return a new AnnotStyleDialogFragment
     */
    public static AnnotStyleDialogFragment newInstance() {
        return new AnnotStyleDialogFragment();
    }

    /**
     * Sets on dismiss listener
     *
     * @param listener dismiss listener
     */
    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        mDismissListener = listener;
    }

    /**
     * Overload implementation of {@link android.app.DialogFragment#onSaveInstanceState(Bundle)}
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARGS_KEY_ANNOT_STYLE, mAnnotStyle.toJSONString());
        if (mWhiteFontList != null) {
            outState.putStringArrayList(ARGS_KEY_WHITE_LIST_FONT, new ArrayList<>(mWhiteFontList));
        }
        if (mAnchor != null) {
            Bundle rect = new Bundle();
            rect.putInt("left", mAnchor.left);
            rect.putInt("top", mAnchor.top);
            rect.putInt("right", mAnchor.right);
            rect.putInt("bottom", mAnchor.bottom);
            outState.putBundle(ARGS_KEY_ANCHOR, rect);
        }
        outState.putBoolean(ARGS_KEY_ANCHOR_SCREEN, mIsAnchorInScreen);
    }

    /**
     * Overload implementation of {@link android.app.DialogFragment#onViewStateRestored(Bundle)}
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            String annotStyleJSON = savedInstanceState.getString(ARGS_KEY_ANNOT_STYLE);
            if (!Utils.isNullOrEmpty(annotStyleJSON)) {
                mAnnotStyle = AnnotStyle.loadJSONString(annotStyleJSON);
            }
            if (savedInstanceState.containsKey(ARGS_KEY_WHITE_LIST_FONT)) {
                ArrayList<String> whiteFontList = savedInstanceState.getStringArrayList(ARGS_KEY_WHITE_LIST_FONT);
                if (whiteFontList != null) {
                    mWhiteFontList = new LinkedHashSet<>(whiteFontList);
                }
            }
            if (savedInstanceState.containsKey(ARGS_KEY_ANCHOR)) {
                Bundle rect = savedInstanceState.getBundle(ARGS_KEY_ANCHOR);
                if (rect != null) {
                    mAnchor = new Rect(rect.getInt("left"), rect.getInt("top"), rect.getInt("right"), rect.getInt("bottom"));
                }
            }

            if (savedInstanceState.containsKey(ARGS_KEY_ANCHOR_SCREEN)) {
                mIsAnchorInScreen = savedInstanceState.getBoolean(ARGS_KEY_ANCHOR_SCREEN);
            }
        }
    }

    /**
     * Overload implementation of {@link android.app.DialogFragment#onCreate(Bundle)}
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }
        if (arguments.containsKey(ARGS_KEY_ANNOT_STYLE)) {
            String annotStyleJSON = arguments.getString(ARGS_KEY_ANNOT_STYLE);
            if (!Utils.isNullOrEmpty(annotStyleJSON)) {
                mAnnotStyle = AnnotStyle.loadJSONString(annotStyleJSON);
            }
        }

        if (arguments.containsKey(ARGS_KEY_WHITE_LIST_FONT)) {
            ArrayList<String> whiteListFont = arguments.getStringArrayList(ARGS_KEY_WHITE_LIST_FONT);
            if (whiteListFont != null) {
                mWhiteFontList = new LinkedHashSet<>(whiteListFont);
            }
        }

        if (arguments.containsKey(ARGS_KEY_ANCHOR)) {
            Bundle bundle = arguments.getBundle(ARGS_KEY_ANCHOR);
            if (bundle != null) {
                mAnchor = new Rect(bundle.getInt("left"), bundle.getInt("top"), bundle.getInt("right"), bundle.getInt("bottom"));
            }
        }

        if (arguments.containsKey(ARGS_KEY_ANCHOR_SCREEN)) {
            mIsAnchorInScreen = arguments.getBoolean(ARGS_KEY_ANCHOR_SCREEN);
        }

        if (arguments.containsKey(ARGS_KEY_MORE_ANNOT_TYPES)) {
            ArrayList<Integer> annotTypes = arguments.getIntegerArrayList(ARGS_KEY_MORE_ANNOT_TYPES);
            if (annotTypes != null) {
                mMoreAnnotTypes = new ArrayList<>(annotTypes);
            }
        }
    }

    /**
     * Overload implementation of {@link android.app.DialogFragment#onCreateDialog(Bundle)}
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog d = new Dialog(getActivity(), R.style.FullScreenDialogStyle) {
            @Override
            public void onBackPressed() {
                if (mAnnotStyleView.getVisibility() == View.VISIBLE) {
                    AnnotStyleDialogFragment.this.dismiss();
                } else {
                    dismissColorPickerView();
                }
            }
        };
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        if (d.getWindow() != null) {
            lp.copyFrom(d.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            d.getWindow().setAttributes(lp);
        }
        return d;
    }

    /**
     * Overload implementation of {@link DialogFragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}
     */
    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.controls_annot_style_container, container, false);
        mAnnotStyleView = view.findViewById(R.id.annot_style);
        mColorPickerView = view.findViewById(R.id.color_picker);

        // background
        TypedArray typedArray = getActivity().obtainStyledAttributes(new int[]{R.attr.colorBackgroundLight});
        int background = typedArray.getColor(0, getActivity().getResources().getColor(R.color.controls_annot_style_preview_bg));
        typedArray.recycle();

        mPreview = view.findViewById(R.id.preview);
        mPreview.setParentBackgroundColor(background);

        mBottomSheet = view.findViewById(R.id.bottom_sheet);
        mBottomSheet.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mBottomSheetCallback.isLocked();
            }
        });

        adjustBottomSheetWidth();

        mBottomSheetCallback = new BottomSheetCallback();

        if (isBottomSheet()) {
            BottomSheetBehavior behavior = new BottomSheetBehavior();
            behavior.setHideable(true);
            behavior.setPeekHeight((int) Utils.convDp2Pix(view.getContext(), 1));
            behavior.setBottomSheetCallback(mBottomSheetCallback);
            mDialogBehavior = behavior;
        } else {
            mDialogBehavior = new StyleDialogBehavior();
        }

        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) mBottomSheet.getLayoutParams();
        lp.setBehavior(mDialogBehavior);

        mColorPickerView.setActivity(getActivity());
        init();
        view.findViewById(R.id.background).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAnnotStyleView.findFocus() != null && mAnnotStyleView.findFocus() instanceof EditText) {
                    mAnnotStyleView.findFocus().clearFocus();
                } else {
                    dismiss();
                }
            }
        });

        return view;
    }

    /**
     * Overload implementation of {@link android.app.DialogFragment#onViewCreated(View, Bundle)}
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mDialogBehavior instanceof BottomSheetBehavior) {
                    ((BottomSheetBehavior) mDialogBehavior).setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        }, 10);
    }

    private boolean isBottomSheet() {
        return !Utils.isTablet(mBottomSheet.getContext()) || mAnchor == null;
    }

    private void adjustBottomSheetWidth() {
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) mBottomSheet.getLayoutParams();
        View child = mBottomSheet.getChildAt(0);
        child.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        lp.width = Utils.isLandscape(mBottomSheet.getContext()) || Utils.isTablet(mBottomSheet.getContext()) ?
            mBottomSheet.getContext().getResources().getDimensionPixelSize(R.dimen.annot_style_picker_width) : ViewGroup.LayoutParams.MATCH_PARENT;
        lp.gravity = isBottomSheet() ? Gravity.CENTER_HORIZONTAL : Gravity.LEFT;
        mBottomSheet.setLayoutParams(lp);
    }

    /**
     * Overload implementation of {@link android.app.DialogFragment#onConfigurationChanged(Configuration)}
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustBottomSheetWidth();
    }

    private void init() {
        mAnnotStyleView.setAnnotStyleHolder(this);
        mColorPickerView.setAnnotStyleHolder(this);
        mAnnotStyleView.setOnPresetSelectedListener(this);
        mAnnotStyleView.setOnColorLayoutClickedListener(new AnnotStyleView.OnColorLayoutClickedListener() {
            @Override
            public void onColorLayoutClicked(int colorMode) {
                openColorPickerView(colorMode);
            }
        });
        mColorPickerView.setOnBackButtonPressedListener(new ColorPickerView.OnBackButtonPressedListener() {
            @Override
            public void onBackPressed() {
                dismissColorPickerView();
            }
        });
        if (mMoreAnnotTypesClickListener != null) {
            mAnnotStyleView.setOnMoreAnnotTypesClickListener(mMoreAnnotTypesClickListener);
        }
        if (mWhiteFontList != null && !mWhiteFontList.isEmpty()) {
            mAnnotStyleView.setWhiteFontList(mWhiteFontList);
        }
        if (mMoreAnnotTypes != null) {
            mAnnotStyleView.setMoreAnnotTypes(mMoreAnnotTypes);
        }
        mAnnotStyleView.setAnnotType(mAnnotStyle.getAnnotType());
        mAnnotStyleView.updateLayout();

        mAnnotStyleView.checkPresets();

        if (mAnnotAppearanceListener != null) {
            mAnnotStyle.setAnnotAppearanceChangeListener(mAnnotAppearanceListener);
        }
    }

    public void setAnnotStyle(AnnotStyle annotStyle) {
        mAnnotStyle = annotStyle;
        mAnnotStyleView.setAnnotType(annotStyle.getAnnotType());
        mAnnotStyleView.updateLayout();
        mAnnotStyleView.deselectAllPresetsPreview();
        mAnnotStyleView.checkPresets();
        mAnnotStyleView.updateAnnotTypes();
        if (mAnnotAppearanceListener != null) {
            mAnnotStyle.setAnnotAppearanceChangeListener(mAnnotAppearanceListener);
        }
    }

    /**
     * Sets Annotation style change listener.
     *
     * @param listener annotation style change listener
     */
    public void setOnAnnotStyleChangeListener(AnnotStyle.OnAnnotStyleChangeListener listener) {
        mAnnotAppearanceListener = listener;
    }

    private void openColorPickerView(@SelectColorMode final int colorMode) {
        TransitionManager.beginDelayedTransition(mBottomSheet, getLayoutChangeTransition());
        mAnnotStyleView.dismiss();
        mColorPickerView.show(colorMode);
    }

    private void dismissColorPickerView() {
        TransitionManager.beginDelayedTransition(mBottomSheet, getLayoutChangeTransition());
        mColorPickerView.dismiss();
        mAnnotStyleView.show();
    }

    private TransitionSet getLayoutChangeTransition() {
        TransitionSet transition = new TransitionSet();
        transition.addTransition(new ChangeBounds());
        Slide slideFromEnd = new Slide(Gravity.END);
        slideFromEnd.addTarget(mColorPickerView);
        transition.addTransition(slideFromEnd);
        Slide slideFromStart = new Slide(Gravity.START);
        slideFromStart.addTarget(mAnnotStyleView);
        transition.addTransition(slideFromStart);
        Fade fade = new Fade();
        fade.setDuration(100);
        fade.setStartDelay(50);
        transition.addTransition(fade);
        return transition;
    }

    /**
     * Overload implementation of {@link AnnotStyleView.OnPresetSelectedListener#onPresetSelected(AnnotStyle)}
     *
     * @param presetStyle presetStyle
     */
    @Override
    public void onPresetSelected(AnnotStyle presetStyle) {
        // call setAnnotStyle will update the real annotation
        if (mAnnotAppearanceListener != null) {
            presetStyle.setAnnotAppearanceChangeListener(mAnnotAppearanceListener);
        }

        if (!presetStyle.equals(mAnnotStyle)) {
            presetStyle.updateAllListeners();
        }
        mAnnotStyle = presetStyle;
        mAnnotStyleView.updateLayout();
        mAnnotStyleView.deselectAllPresetsPreview();
        if (presetStyle.getBindedPreview() != null) {
            presetStyle.getBindedPreview().setSelected(true);
        }
    }

    /**
     * Overload implementation of {@link AnnotStyleView.OnPresetSelectedListener#onPresetDeselected(AnnotStyle)}
     *
     * @param presetStyle presetStyle
     */
    @Override
    public void onPresetDeselected(AnnotStyle presetStyle) {
        mAnnotStyle = new AnnotStyle(presetStyle);
        mAnnotStyle.bindPreview(null);
        if (mAnnotAppearanceListener != null) {
            mAnnotStyle.setAnnotAppearanceChangeListener(mAnnotAppearanceListener);
        }
        mAnnotStyleView.deselectAllPresetsPreview();
    }

    /**
     * Dismiss the dialog
     *
     * @param waitBottomSheet whether to wait for bottom sheet to collapse.
     */
    public void dismiss(boolean waitBottomSheet) {
        if (waitBottomSheet && (mDialogBehavior instanceof BottomSheetBehavior)) {
            ((BottomSheetBehavior) mDialogBehavior).setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            super.dismiss();
            saveAnnotStyles();
            if (mDismissListener != null) {
                mDismissListener.onDismiss(getDialog());
            }
            AnalyticsAnnotStylePicker.getInstance().dismissAnnotStyleDialog();
        }
    }

    /**
     * Saves annotation styles to settings
     */
    public void saveAnnotStyles() {
        mAnnotStyleView.savePresets();
        mColorPickerView.saveColors();
    }

    /**
     * Dismiss the dialog
     */
    @Override
    public void dismiss() {
        dismiss(true);
    }

    /**
     * @hide
     */
    public void show(@NonNull FragmentManager fragmentManager,
                     @AnalyticsHandlerAdapter.StylePickerOpenedLocation int from,
                     String annotation) {

        AnalyticsAnnotStylePicker.getInstance().showAnnotStyleDialog(from, annotation);
        show(fragmentManager);

        if (mDialogBehavior != null && mDialogBehavior instanceof StyleDialogBehavior) {
            ((StyleDialogBehavior) mDialogBehavior).setShowBottom(from == AnalyticsHandlerAdapter.LOCATION_ANNOTATION_TOOLBAR);
        }

    }

    /**
     * Show the dialog
     */
    public void show(
        @NonNull FragmentManager fragmentManager) {

        if (isAdded()) {
            return;
        }
        show(fragmentManager, "dialog");

    }

    /**
     * Overload implementation of {@link AnnotStyle.AnnotStyleHolder#getAnnotStyle()}
     * Gets annotation style
     *
     * @return annotation style
     */
    @Override
    public AnnotStyle getAnnotStyle() {
        if (mAnnotStyle != null) {
            return mAnnotStyle;
        } else if (getArguments() != null && getArguments().containsKey(ARGS_KEY_ANNOT_STYLE)) {
            String annotStyleJSON = getArguments().getString(ARGS_KEY_ANNOT_STYLE);
            if (!Utils.isNullOrEmpty(annotStyleJSON)) {
                mAnnotStyle = AnnotStyle.loadJSONString(annotStyleJSON);
            }
            return mAnnotStyle;
        }
        return null;
    }

    @Override
    public AnnotationPropertyPreviewView getAnnotPreview() {
        return mPreview;
    }

    @Override
    public void setAnnotPreviewVisibility(int visibility) {
        mPreview.setVisibility(visibility);
        if (getView() != null) {
            getView().findViewById(R.id.divider).setVisibility(visibility);
        }
    }

    /**
     * Sets more annot types row item click event listener
     *
     * @param listener The listener
     */
    public void setOnMoreAnnotTypesClickListener(AnnotStyleView.OnMoreAnnotTypeClickedListener listener) {
        mMoreAnnotTypesClickListener = listener;
        if (mAnnotStyleView != null) {
            mAnnotStyleView.setOnMoreAnnotTypesClickListener(mMoreAnnotTypesClickListener);
        }
    }

    /**
     * Selected color mode for color picker view
     */
    @IntDef({STROKE_COLOR, FILL_COLOR, TEXT_COLOR, COLOR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SelectColorMode {
    }

    private class BottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
        boolean mLocked = false;

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {

            if (mLocked && (newState == BottomSheetBehavior.STATE_DRAGGING
                || newState == BottomSheetBehavior.STATE_COLLAPSED)) {
                ((BottomSheetBehavior) mDialogBehavior).setState(BottomSheetBehavior.STATE_EXPANDED);
            } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss(false);
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }

        public boolean isLocked() {
            return mLocked;
        }

        public void setLocked(boolean locked) {
            this.mLocked = locked;
        }
    }

    private class StyleDialogBehavior extends CoordinatorLayout.Behavior<View> {
        private boolean mIsShowBottom = false;
        private boolean mInitialized = false;

        @Override
        public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
            if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
                child.setFitsSystemWindows(true);
            }
            // First let the parent lay it out
            parent.onLayoutChild(child, layoutDirection);

            int margin = mBottomSheet.getContext().getResources().getDimensionPixelSize(R.dimen.padding_large);
            Rect anchor = new Rect(mAnchor);

            if (mIsAnchorInScreen) {
                int[] parentScreenPos = new int[2];
                parent.getLocationOnScreen(parentScreenPos);
                anchor.offset(-parentScreenPos[0], -parentScreenPos[1]);
            }
            int midAnchorX = anchor.left + (anchor.width() / 2);
            int midAnchorY = anchor.top + (anchor.height() / 2);

            int midPosY = midAnchorY - (child.getHeight() / 2);
            int midPosX = midAnchorX - (child.getWidth() / 2);

            int posX = 0;
            int posY = 0;
            boolean showTop = !mIsShowBottom;
            boolean showBottom = mIsShowBottom;
            boolean showLeft = false;
            boolean showRight = false;
            if (showTop) {
                posX = midPosX;
                posY = anchor.top - margin - child.getHeight();
                if (!mInitialized) {
                    mIsShowBottom = showBottom = posY < margin;
                    showTop = !showBottom;
                }
            }

            if (showBottom) {
                posX = midPosX;
                posY = anchor.bottom + margin;
                showLeft = posY + child.getHeight() > parent.getHeight();
                showBottom = !showLeft;
            }

            if (showLeft) {
                posX = anchor.left - margin - child.getWidth();
                posY = anchor.top < margin ? midPosY : anchor.top;
                showRight = posX < 0;
                showLeft = !showRight;
            }

            if (showRight) {
                posX = anchor.right + margin;
                posY = anchor.top < margin ? midPosY : anchor.top;
                showRight = !(posX + child.getWidth() > parent.getWidth());
            }

            if (!showTop && !showBottom && !showLeft && !showRight) {
                posX = midPosX;
                posY = midPosY;
            }

            if (posX < margin) {
                posX = margin;
            } else if (posX + child.getWidth() > parent.getWidth() - margin) {
                posX = parent.getWidth() - child.getWidth() - margin;
            }

            // two times margin on top because of the status bar
            if (posY < 2 * margin) {
                posY = 2 * margin;
            } else if (posY + child.getHeight() > parent.getHeight()) {
                posY = parent.getHeight() - child.getHeight();
            }

            mInitialized = true;

            ViewCompat.offsetTopAndBottom(child, posY);
            ViewCompat.offsetLeftAndRight(child, posX);

            return true;
        }

        void setShowBottom(boolean showBottom) {
            mIsShowBottom = showBottom;
            mInitialized = true;
        }
    }


    /**
     * Builder for building annotation style dialog
     */
    public static class Builder {
        Bundle bundle;

        /**
         * Creates a builder for an annotation style dialog
         */
        public Builder() {
            bundle = new Bundle();
        }

        /**
         * Creates a builder for an annotation style dialog with given annotation style
         *
         * @param annotStyle The annotation style for building the dialog
         */
        public Builder(AnnotStyle annotStyle) {
            bundle = new Bundle();
            setAnnotStyle(annotStyle);
        }

        /**
         * Sets annotation style to the builder, it is used for setting annotation style for dialog
         *
         * @param annotStyle The annotation style for building dialog. This is equivalent to call: {@code new Builder(annotStyle)}
         * @return The builder
         */
        public Builder setAnnotStyle(AnnotStyle annotStyle) {
            bundle.putString(ARGS_KEY_ANNOT_STYLE, annotStyle.toJSONString());
            return this;
        }

        /**
         * Sets white list fonts. You can get white list fonts from {@link ToolManager#getFreeTextFonts()}
         *
         * @param whiteListFont The white list fonts.
         * @return The builder
         */
        public Builder setWhiteListFont(@Nullable Set<String> whiteListFont) {
            if (whiteListFont != null) {
                ArrayList<String> whiteListFontArr = new ArrayList<>(whiteListFont);
                bundle.putStringArrayList(ARGS_KEY_WHITE_LIST_FONT, whiteListFontArr);
            }
            return this;
        }

        /**
         * Sets anchor rectangle in window location for tablet mode.
         * The annotation style dialog will be displayed around the anchor rectangle.
         * <br/>
         * You can get window location of a view as follows:
         * <pre>
         * int[] pos = new int[2];
         * view.getLocationInWindow(pos);
         * RectF rect = new RectF(pos[0], pos[1], pos[0] + view.getWidth(), pos[1] + view.getHeight());
         * builder.setAnchor(rect);
         * </pre>
         * <p>
         * where {@code view} is an instance of {@link View}, and {@code builder} is an instance of {@link Builder}
         *
         * @param anchor The anchor rectangle
         * @return The builder
         */
        public Builder setAnchor(RectF anchor) {
            Bundle rect = new Bundle();
            rect.putInt("left", (int) anchor.left);
            rect.putInt("top", (int) anchor.top);
            rect.putInt("right", (int) anchor.right);
            rect.putInt("bottom", (int) anchor.bottom);
            bundle.putBundle(ARGS_KEY_ANCHOR, rect);
            return this;
        }

        /**
         * Sets anchor rectangle in window location for tablet mode. The annotation style dialog will be displayed around the anchor rectangle.
         * This is equivalent to call {@link #setAnchor(RectF)}
         *
         * @param anchor The anchor rectangle
         * @return The builder
         */
        public Builder setAnchor(Rect anchor) {
            Bundle rect = new Bundle();
            rect.putInt("left", anchor.left);
            rect.putInt("top", anchor.top);
            rect.putInt("right", anchor.right);
            rect.putInt("bottom", anchor.bottom);
            bundle.putBundle(ARGS_KEY_ANCHOR, rect);
            return this;
        }

        /**
         * Sets anchor view for tablet mode. The annotation style dialog will be displayed around the anchor rectangle.
         *
         * @param view The anchor view
         * @return The builder
         */
        public Builder setAnchorView(View view) {
            int[] pos = new int[2];
            view.getLocationInWindow(pos);
            return setAnchor(new Rect(pos[0], pos[1], pos[0] + view.getWidth(), pos[1] + view.getHeight()));
        }

        /**
         * Sets anchor rectangle in screen position for tablet mode. The annotation style dialog will be displayed around the anchor rectangle.
         * <br/>
         * You can get screen location of a view as follows:
         * <pre>
         * int[] pos = new int[2];
         * view.getLocationInScreen(pos);
         * RectF rect = new RectF(pos[0], pos[1], pos[0] + view.getWidth(), pos[1] + view.getHeight());
         * builder.setAnchorInScreen(rect);
         * </pre>
         * <p>
         * where {@code view} is an instance of {@link View}, and {@code builder} is an instance of {@link Builder}
         *
         * @param anchor The anchor rectangle
         * @return The builder
         */
        public Builder setAnchorInScreen(Rect anchor) {
            setAnchor(anchor);
            bundle.putBoolean(ARGS_KEY_ANCHOR_SCREEN, true);
            return this;
        }

        /**
         * Sets more annot types to show in annotation style dialog
         *
         * @param annotTypes annot types to add
         */
        public Builder setMoreAnnotTypes(@Nullable ArrayList<Integer> annotTypes) {
            if (annotTypes != null) {
                bundle.putIntegerArrayList(ARGS_KEY_MORE_ANNOT_TYPES, annotTypes);
            }
            return this;
        }

        /**
         * Creates an {@link AnnotStyleDialogFragment} with the arguments supplied to this builder.
         *
         * @return An new instance of {@link AnnotStyleDialogFragment}
         */
        public AnnotStyleDialogFragment build() {
            AnnotStyleDialogFragment annotStyleDialogFragment = newInstance();
            annotStyleDialogFragment.setArguments(bundle);
            return annotStyleDialogFragment;
        }
    }

}
