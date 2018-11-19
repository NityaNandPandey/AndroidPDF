//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.LayerDrawable;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

/**
 * The DialogStickyNote is a subclass of {@link com.pdftron.pdf.tools.DialogAnnotNote} and is used
 * for all sticky note annotations. When a user creates or taps on a sticky note,
 * this dialog will appear and it allows the user to add or edit their note.
 */
@SuppressWarnings("WeakerAccess")
public class DialogStickyNote extends DialogAnnotNote {

    private ImageButton mStyleButton;

    private boolean mIconStyleChanged = false;
    private boolean mExistingNote;

    private int mIconColor;
    private float mIconOpacity;
    private String mIconType;

    private static final int TEXT_VIEW = 0;

    private AnnotStyleDialogFragment mAnnotStyleDialog;

    private AnnotStyle.OnAnnotStyleChangeListener mAnnotStyleChangeListener;

    /**
     * Class constructor
     */
    public DialogStickyNote(@NonNull PDFViewCtrl pdfViewCtrl, String note, boolean existingNote, String iconType, int iconColor, float iconOpacity) {
        super(pdfViewCtrl, note);
        Activity activity = ((ToolManager) mPdfViewCtrl.getToolManager()).getCurrentActivity();
        if (activity != null) {
            setOwnerActivity(activity);
        } else {
            Log.e(DialogStickyNote.class.getName(), "ToolManager is not attached to with an Activity");
        }
        mExistingNote = existingNote;
        mIconColor = iconColor;
        mIconType = iconType;
        mIconOpacity = iconOpacity;
        // Override gesture detector to account for sticky note icon changes.
        mGestureDetector = new GestureDetector(getContext(), new MyGestureDetector() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                boolean superReturnValue = super.onSingleTapUp(e);

                // Only if the icon hasn't been changed, the
                // save button is disabled until text or the icon is changed
                if (mIconStyleChanged) {
                    mPositiveButton.setEnabled(true);
                }

                return superReturnValue;
            }
        });

        // Add button which toggles between the style view and the text view
        mStyleButton = mMainView.findViewById(R.id.tools_dialog_annotation_popup_button_style);
        mStyleButton.setVisibility(View.VISIBLE);
        mStyleButton.setImageDrawable(createCurrentIcon());
        mStyleButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        mStyleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentActivity activity = null;
                if (getContext() instanceof FragmentActivity) {
                    activity = (FragmentActivity) getContext();
                } else if (mPdfViewCtrl != null){
                    ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                    activity = toolManager.getCurrentActivity();
                }
                if (activity == null) {
                    AnalyticsHandlerAdapter.getInstance().sendException(new Exception("DialogStickyNote is not attached with an Activity"));
                    return;
                }
                mAnnotStyleDialog.show(activity.getSupportFragmentManager(),
                    AnalyticsHandlerAdapter.STYLE_PICKER_LOC_STICKY_NOTE,
                    AnalyticsHandlerAdapter.getInstance().getAnnotationTool(AnalyticsHandlerAdapter.ANNOTATION_TOOL_STICKY_NOTE)
                );
            }
        });
        // add style options for sticky note
        addStyleView();
    }

    /**
     * The overload implementation of {@link DialogAnnotNote#initTextBox(String)}.
     */
    @Override
    public void initTextBox(String note) {
        if (mExistingNote && !note.equals("")) {
            switchToViewMode();
            mTextBox.requestFocus();
        } else {
            Utils.showSoftKeyboard(getContext(), mTextBox);
        }

        if (!note.equals("")) {
            mTextBox.setText(note);
            // Set the caret position to the end of the text
            mTextBox.setSelection(mTextBox.getText().length());
        }

        // modify textbox background color
        int noteBackgroundColor = getBackgroundColor();
        mTextBox.setBackgroundColor(noteBackgroundColor);
    }

    private int getBackgroundColor() {
        float[] hsv = new float[3];
        Color.colorToHSV(mIconColor, hsv);

        float hue = hsv[0];
        float saturation = hsv[1];
        float brightness = hsv[2];

        // if not white and not grey scale, lighten color
        int lightColor = ContextCompat.getColor(getContext(), R.color.tools_dialog_sticky_note_textbox_background);
        if ((saturation == 1.0f && brightness == 0) || saturation == 0 || Utils.isDeviceNightMode(getContext())) {
            return lightColor;
        }

        // lighten color
        if (brightness > 0.8f) {
            saturation = 0.10f;
        } else if (brightness > 0.6) {
            saturation = 0.12f;
        } else if (brightness > 0.4) {
            saturation = 0.14f;
        } else if (brightness > 0.2) {
            saturation = 0.16f;
        } else {
            saturation = 0.18f;
        }
        brightness = 0.97f;

        float[] lightColorHSV = {hue, saturation, brightness};
        lightColor = Color.HSVToColor(lightColorHSV);

        return lightColor;
    }

    private void addStyleView() {
        // Style View
        AnnotStyle annotStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(getContext(), Annot.e_Text, "");
        annotStyle.setIcon(mIconType);
        annotStyle.setStyle(mIconColor, 0, 0, mIconOpacity);
        mAnnotStyleDialog = new AnnotStyleDialogFragment.Builder(annotStyle).build();
        mAnnotStyleDialog.setOnAnnotStyleChangeListener(new AnnotStyle.OnAnnotStyleChangeListener() {
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
                mIconOpacity = opacity;
                mStyleButton.setAlpha(opacity);
                if (mAnnotStyleChangeListener != null) {
                    mAnnotStyleChangeListener.onChangeAnnotOpacity(opacity, done);
                }
            }

            @Override
            public void onChangeAnnotStrokeColor(int color) {
                mIconColor = color;
                mStyleButton.setImageDrawable(createCurrentIcon());
                mTextBox.setBackgroundColor(getBackgroundColor());
                if (mAnnotStyleChangeListener != null) {
                    mAnnotStyleChangeListener.onChangeAnnotStrokeColor(color);
                }
            }

            @Override
            public void onChangeAnnotFillColor(int color) {

            }

            @Override
            public void onChangeAnnotIcon(String icon) {
                mIconType = icon;
                mStyleButton.setImageDrawable(createCurrentIcon());
                if (mAnnotStyleChangeListener != null) {
                    mAnnotStyleChangeListener.onChangeAnnotIcon(icon);
                }
            }

            @Override
            public void onChangeAnnotFont(FontResource font) {

            }

            @Override
            public void onChangeRulerProperty(RulerItem rulerItem) {

            }
        });
        mAnnotStyleDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                // get current icon color and type
                mIconType = mAnnotStyleDialog.getAnnotStyle().getIcon();
                mIconColor = mAnnotStyleDialog.getAnnotStyle().getColor();
            }
        });

    }

    private LayerDrawable createCurrentIcon() {
        return (LayerDrawable) AnnotStyle.getIconDrawable(getContext(), mIconType.toLowerCase(), mIconColor, mIconOpacity);
    }

    public void setAnnotAppearanceChangeListener(AnnotStyle.OnAnnotStyleChangeListener listener) {
        mAnnotStyleChangeListener = listener;
    }

    /*
     *  Methods for accessing AnnotPropertyViewGroup methods
     */

    /**
     * Prepares dialog dismiss.
     */
    public void prepareDismiss() {
        ToolStyleConfig.getInstance().saveAnnotStyle(getContext(), mAnnotStyleDialog.getAnnotStyle(), "");
    }

    /**
     * @return True if it is an existing note
     */
    public boolean isExistingNote() {
        return mExistingNote;
    }

    /**
     * The overload implementation of {@link DialogAnnotNote#show()}.
     */
    @Override
    public void show() {
        boolean showable = ((ToolManager) mPdfViewCtrl.getToolManager()).getStickyNoteShowPopup();
        Log.d("DialogStickyNote", "show: " + showable);
        if (showable) {
            super.show();
        }
    }
}
