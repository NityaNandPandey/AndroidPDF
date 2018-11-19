//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.pdftron.pdf.utils.Utils;

/**
 * A dialog with custom size which depends running on tablet or phone devices
 */
public abstract class CustomSizeDialogFragment extends DialogFragment {

    protected int mWidth = 500;
    protected int mHeight = -1;

    @Override
    public void onStart() {
        super.onStart();
        setDialogSize();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setDialogSize();
    }

    private void setDialogSize() {
        View view = getView();
        Window window = getDialog().getWindow();
        if (view == null || window == null) {
            return;
        }

        Context context = view.getContext();

        int marginWidth = (int) Utils.convDp2Pix(context, 600);
        if (Utils.isTablet(context) && Utils.getScreenWidth(context) > marginWidth) {
            WindowManager.LayoutParams windowParams = window.getAttributes();
            windowParams.dimAmount = 0.60f;
            windowParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(windowParams);

            int width = (int) Utils.convDp2Pix(context, mWidth);
            int height = Utils.getScreenHeight(context);
            if (mHeight > 0) {
                window.setLayout(width, mHeight);
            } else {
                int dh = (int) Utils.convDp2Pix(context, 100);
                window.setLayout(width, height - dh);
            }
        } else {
            if (mHeight > 0) {
                int width = (int) (Utils.getScreenWidth(context) * 0.9);
                window.setLayout(width, mHeight);
            } else {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        }
    }

}
