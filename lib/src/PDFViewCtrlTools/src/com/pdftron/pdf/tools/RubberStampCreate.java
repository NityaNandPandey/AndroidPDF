//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.RubberStamp;
import com.pdftron.pdf.controls.RubberStampDialogFragment;
import com.pdftron.pdf.interfaces.OnDialogDismissListener;
import com.pdftron.pdf.interfaces.OnRubberStampSelectedListener;
import com.pdftron.pdf.model.CustomStampPreviewAppearance;
import com.pdftron.pdf.model.StandardStampPreviewAppearance;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;

/**
 * This class is for creating rubber stamp annotation.
 */
@Keep
public class RubberStampCreate extends Stamper {

    // default values
    private CustomStampPreviewAppearance[] mCustomStampPreviewAppearances = new CustomStampPreviewAppearance[]{
        new CustomStampPreviewAppearance("green", 0xffF4F8EE, 0xFFdee7d8, 0xffD4E0CC, 0xFF267F00, 0xFF2E4B11, .85),
        new CustomStampPreviewAppearance("red", 0xffFAEBE8, 0xfffed6d6, 0xffFFC9C9, 0xFF9C0E04, 0xFF9C0E04, .85),
        new CustomStampPreviewAppearance("blue", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85),
        new CustomStampPreviewAppearance("dark yellow", 0xfffbf7aa, 0xffF8F055, 0xffe5da09, 0xFF3f3c02, 0xFFd0ad2e, 1),
        new CustomStampPreviewAppearance("dark_purple", 0xffc6bee6, 0xff8E7FCD, 0xff8878ca, 0xFF18122f, 0xFF413282, 1),
        new CustomStampPreviewAppearance("dark_red", 0xffda7a67, 0xffCF4E35, 0xffd5624b, 0xFF2a0f09, 0xFF6e0005, 1),
    };
    private StandardStampPreviewAppearance[] mStandardStampPreviewAppearance = new StandardStampPreviewAppearance[]{
        new StandardStampPreviewAppearance("APPROVED", new CustomStampPreviewAppearance("", 0xffF4F8EE, 0xFFdee7d8, 0xffD4E0CC, 0xFF267F00, 0xFF2E4B11, .85)),
        new StandardStampPreviewAppearance("NOT APPROVED", new CustomStampPreviewAppearance("", 0xffFAEBE8, 0xfffed6d6, 0xffFFC9C9, 0xFF9C0E04, 0xFF9C0E04, .85)),
        new StandardStampPreviewAppearance("FINAL", new CustomStampPreviewAppearance("", 0xffF4F8EE, 0xFFdee7d8, 0xffD4E0CC, 0xFF267F00, 0xFF2E4B11, .85)),
        new StandardStampPreviewAppearance("DRAFT", new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
        new StandardStampPreviewAppearance("COMPLETED", new CustomStampPreviewAppearance("", 0xffF4F8EE, 0xFFdee7d8, 0xffD4E0CC, 0xFF267F00, 0xFF2E4B11, .85)),
        new StandardStampPreviewAppearance("VOID", new CustomStampPreviewAppearance("", 0xffFAEBE8, 0xfffed6d6, 0xffFFC9C9, 0xFF9C0E04, 0xFF9C0E04, .85)),
        new StandardStampPreviewAppearance("FOR PUBLIC RELEASE", new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
        new StandardStampPreviewAppearance("NOT FOR PUBLIC RELEASE", new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
        new StandardStampPreviewAppearance("FOR COMMENT", new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
        new StandardStampPreviewAppearance("CONFIDENTIAL", new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
        new StandardStampPreviewAppearance("PRELIMINARY RESULTS", new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
        new StandardStampPreviewAppearance("INFORMATION ONLY", new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
        new StandardStampPreviewAppearance("WITNESS", new CustomStampPreviewAppearance("", 0xfffbf7aa, 0xffF8F055, 0xffe5da09, 0xFF3f3c02, 0xFFd0ad2e, 1), true, false),
        new StandardStampPreviewAppearance("CHECK_MARK"),
        new StandardStampPreviewAppearance("INITIAL HERE", new CustomStampPreviewAppearance("", 0xffc6bee6, 0xff8E7FCD, 0xff8878ca, 0xFF18122f, 0xFF413282, 1), true, false),
        new StandardStampPreviewAppearance("CROSS_MARK"),
        new StandardStampPreviewAppearance("SIGN HERE", new CustomStampPreviewAppearance("", 0xffda7a67, 0xffCF4E35, 0xffd5624b, 0xFF2a0f09, 0xFF6e0005, 1), true, false),
    };

    /**
     * Class constructor
     */
    public RubberStampCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();

        FragmentActivity activity = ((ToolManager) ctrl.getToolManager()).getCurrentActivity();
        if (activity != null) {
            Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(RubberStampDialogFragment.TAG);
            if (fragment instanceof RubberStampDialogFragment) {
                setRubberStampDialogFragmentListeners((RubberStampDialogFragment) fragment);
            }
        }
    }

    /**
     * Sets how to show stamp appearances in custom rubber stamp dialog.
     *
     * @param standardStampPreviewAppearance An array of standard rubber stamp appearances; null for default
     * @param customStampPreviewAppearance   An array of custom rubber stamp appearances; null for default
     */
    @SuppressWarnings("unused")
    public void setCustomStampAppearance(@Nullable StandardStampPreviewAppearance[] standardStampPreviewAppearance, @Nullable CustomStampPreviewAppearance[] customStampPreviewAppearance) {
        if (standardStampPreviewAppearance != null) {
            mStandardStampPreviewAppearance = standardStampPreviewAppearance;
        }
        if (customStampPreviewAppearance != null) {
            mCustomStampPreviewAppearances = customStampPreviewAppearance;
        }
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.RUBBER_STAMPER;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Stamp;
    }

    /**
     * The overload implementation of {@link Stamper#addStamp()}.
     */
    @Override
    protected void addStamp() {
        if (mTargetPoint == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(
                new Exception("target point is not specified."));
            return;
        }

        if (mPdfViewCtrl == null) {
            return;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        FragmentActivity activity = toolManager.getCurrentActivity();
        if (activity == null) {
            return;
        }

        RubberStampDialogFragment fragment = RubberStampDialogFragment.newInstance(mTargetPoint, mStandardStampPreviewAppearance, mCustomStampPreviewAppearances);
        fragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        fragment.show(activity.getSupportFragmentManager(), RubberStampDialogFragment.TAG);
        setRubberStampDialogFragmentListeners(fragment);
    }

    private void setRubberStampDialogFragmentListeners(@NonNull final RubberStampDialogFragment fragment) {
        fragment.setOnRubberStampSelectedListener(new OnRubberStampSelectedListener() {
            @Override
            public void onRubberStampSelected(@NonNull String stampLabel) {
                mTargetPoint = fragment.getTargetPoint();
                if (!Utils.isNullOrEmpty(stampLabel) && mTargetPoint != null) {
                    createStandardRubberStamp(stampLabel);
                }
            }

            @Override
            public void onRubberStampSelected(@Nullable Obj stampObj) {
                mTargetPoint = fragment.getTargetPoint();
                if (stampObj != null && mTargetPoint != null) {
                    createCustomStamp(stampObj);
                }
            }
        });

        fragment.setOnDialogDismissListener(new OnDialogDismissListener() {
            @Override
            public void onDialogDismiss(
            ) {
                // reset target point
                clearTargetPoint();
                safeSetNextToolMode();
            }
        });
    }

    private void createStandardRubberStamp(@NonNull String stampName) {
        if (mTargetPoint == null) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            int pageNum = mPdfViewCtrl.getPageNumberFromScreenPt(mTargetPoint.x, mTargetPoint.y);
            if (pageNum < 1) {
                return;
            }

            double[] size = AnnotUtils.getStampSize(mPdfViewCtrl.getContext(), stampName);
            if (size == null) {
                return;
            }
            int width = (int) (size[0] + .5);
            int height = (int) (size[1] + .5);
            double[] pageTarget = mPdfViewCtrl.convScreenPtToPagePt(mTargetPoint.x, mTargetPoint.y, pageNum);
            Rect rect = new Rect(
                pageTarget[0] - width / 2,
                pageTarget[1] - height / 2,
                pageTarget[0] + width / 2,
                pageTarget[1] + height / 2);
            Page page = mPdfViewCtrl.getDoc().getPage(pageNum);
            boundToCropBox(page, rect);
            RubberStamp stamp = RubberStamp.create(mPdfViewCtrl.getDoc(), rect);
            stamp.setIcon(stampName);
            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), stamp);

            page.annotPushBack(stamp);

            mPdfViewCtrl.update(stamp, pageNum);
            raiseAnnotationAddedEvent(stamp, pageNum);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void createCustomStamp(@NonNull Obj stampObj) {
        if (mTargetPoint == null) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            int pageNum = mPdfViewCtrl.getPageNumberFromScreenPt(mTargetPoint.x, mTargetPoint.y);
            if (pageNum < 1) {
                return;
            }

            Rect rubberRect = getCustomRubberRect(stampObj);
            int width = (int) (rubberRect.getWidth() + .5);
            int height = (int) (rubberRect.getHeight() + .5);
            double[] pageTarget = mPdfViewCtrl.convScreenPtToPagePt(mTargetPoint.x, mTargetPoint.y, pageNum);
            Rect rect = new Rect(
                pageTarget[0] - width / 2,
                pageTarget[1] - height / 2,
                pageTarget[0] + width / 2,
                pageTarget[1] + height / 2);
            Page page = mPdfViewCtrl.getDoc().getPage(pageNum);
            boundToCropBox(page, rect);
            RubberStamp stamp = RubberStamp.createCustom(mPdfViewCtrl.getDoc(), rect, stampObj);

            page.annotPushBack(stamp);

            mPdfViewCtrl.update(stamp, pageNum);
            raiseAnnotationAddedEvent(stamp, pageNum);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private Rect getCustomRubberRect(@NonNull Obj stampObj) throws PDFNetException {
        PDFDoc tempDoc = null;
        try {
            tempDoc = new PDFDoc();
            tempDoc.initSecurityHandler();
            RubberStamp rubberStamp = RubberStamp.createCustom(tempDoc, new Rect(), stampObj);
            return rubberStamp.getRect();
        } finally {
            Utils.closeQuietly(tempDoc);
        }
    }

    private void boundToCropBox(@NonNull Page page, @NonNull Rect rect) throws PDFNetException {
        com.pdftron.pdf.Rect cropBox = page.getBox(mPdfViewCtrl.getPageBox());
        cropBox.normalize();

        double width = rect.getWidth();
        double height = rect.getHeight();
        if (rect.getX1() < cropBox.getX1()) {
            rect.setX1(cropBox.getX1());
            rect.setX2(cropBox.getX1() + width);
        }
        if (rect.getX2() > cropBox.getX2()) {
            rect.setX2(cropBox.getX2());
            rect.setX1(cropBox.getX2() - width);
        }
        if (rect.getY1() < cropBox.getY1()) {
            rect.setY1(cropBox.getY1());
            rect.setY2(cropBox.getY1() + height);
        }
        if (rect.getY2() > cropBox.getY2()) {
            rect.setY2(cropBox.getY2());
            rect.setY1(cropBox.getY2() - height);
        }
    }

}
