//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupWindow;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;

public class StampStatePopup extends PopupWindow {

    public static final String STATE_SIGNATURE = "signature";
    public static final String STATE_IMAGE_STAMP = "stamp";
    public static final String STATE_RUBBER_STAMP = "rubber_stamp";

    private ToolManager mToolManager;
    private String mStampState;
    private ImageButton mSignatureButton, mImageStampButton, mRubberStampButton;

    public StampStatePopup(
        @NonNull Context context,
        @NonNull ToolManager toolManager,
        @NonNull String stampState,
        int backgroundColor,
        int iconColor) {

        super(context);
        mToolManager = toolManager;
        mStampState = stampState;

        View mainView = LayoutInflater.from(context).inflate(R.layout.tools_annotation_toolbar_state_stamp_popup, null);
        mainView.setBackgroundColor(backgroundColor);
        mSignatureButton = mainView.findViewById(R.id.tools_annotation_toolbar_state_signature_button);
        mImageStampButton = mainView.findViewById(R.id.tools_annotation_toolbar_state_image_stamp_button);
        mRubberStampButton = mainView.findViewById(R.id.tools_annotation_toolbar_state_rubber_stamp_button);

        Drawable drawable = Utils.createImageDrawableSelector(context, R.drawable.ic_annotation_signature_black_24dp, iconColor);
        mSignatureButton.setImageDrawable(drawable);
        drawable = Utils.createImageDrawableSelector(context, R.drawable.ic_annotation_image_black_24dp, iconColor);
        mImageStampButton.setImageDrawable(drawable);
        drawable = Utils.createImageDrawableSelector(context, R.drawable.ic_annotation_stamp_black_24dp, iconColor);
        mRubberStampButton.setImageDrawable(drawable);

        mSignatureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStampState = STATE_SIGNATURE;
                dismiss();
            }
        });
        mImageStampButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStampState = STATE_IMAGE_STAMP;
                dismiss();
            }
        });
        mRubberStampButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStampState = STATE_RUBBER_STAMP;
                dismiss();
            }
        });

        updateButtonsVisibility();

        setContentView(mainView);
        setOutsideTouchable(true);
        setFocusable(false);

        setAnimationStyle(R.style.Controls_AnnotationPopupAnimation);
    }

    public void show(View parent, int x, int y) {
        try {
            showAtLocation(parent, Gravity.NO_GRAVITY, x, y);
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
    }

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
        super.showAtLocation(parent, gravity, x, y);
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_SIG_STATE_POPUP_OPEN);
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        super.showAsDropDown(anchor, xoff, yoff, gravity);
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_SIG_STATE_POPUP_OPEN);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_STYLE_PICKER_OPEN);
    }

    public String getStampState() {
        return mStampState;
    }

    public void updateView(String newSignatureState) {
        mStampState = newSignatureState;
        updateButtonsVisibility();
    }

    private void updateButtonsVisibility() {
        boolean visible = !mToolManager.isToolModeDisabled(ToolManager.ToolMode.SIGNATURE) && !STATE_SIGNATURE.equals(mStampState);
        mSignatureButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        visible = !mToolManager.isToolModeDisabled(ToolManager.ToolMode.STAMPER) && !STATE_IMAGE_STAMP.equals(mStampState);
        mImageStampButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        visible = !mToolManager.isToolModeDisabled(ToolManager.ToolMode.RUBBER_STAMPER) && !STATE_RUBBER_STAMP.equals(mStampState);
        mRubberStampButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
