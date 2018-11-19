//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.Destination;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.ViewChangeCollection;
import com.pdftron.pdf.annots.Link;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * A dialog editing link url/ page number
 */
public class DialogLinkEditor extends AlertDialog implements
    View.OnClickListener,
    View.OnFocusChangeListener,
    DialogInterface.OnClickListener {

    private PDFViewCtrl mCtrl;
    private View dialogView;
    private RadioButton externalRadioButton;
    private RadioButton internalRadioButton;
    private RadioButton mSelectedRadioButton;
    private EditText pageEditText;
    private EditText urlEditText;
    private Tool mTool;
    private HashMap<Rect, Integer> mSelRect;
    private int mStrokeColor;
    private float mThickness;
    private Link mLink;
    private int mLinkPage;
    private int mPageCount;
    private Button mPosButton;
    private HashMap<Annot, Integer> mAnnots;

    @IntDef({LINK_OPTION_URL, LINK_OPTION_PAGE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LinkOption {}

    public static final int LINK_OPTION_URL = 0;
    public static final int LINK_OPTION_PAGE = 1;

    /**
     * Class constructor
     */
    public DialogLinkEditor(@NonNull PDFViewCtrl ctrl, @NonNull Tool tool, HashMap<Rect, Integer> selRect) {
        super(ctrl.getContext());
        mCtrl = ctrl;
        mTool = tool;
        mSelRect = new HashMap<>();
        mSelRect.putAll(selRect);
        if (mSelRect == null) {
            ((ToolManager) mCtrl.getToolManager()).annotationCouldNotBeAdded("no link selected");
            dismiss();
        }

        init();

    }

    /**
     * Class constructor
     */
    public DialogLinkEditor(@NonNull PDFViewCtrl ctrl, @NonNull Tool tool, Link link) {
        super(ctrl.getContext());
        mCtrl = ctrl;
        mLink = link;
        mTool = tool;
        if (mLink == null) {
            ((ToolManager) mCtrl.getToolManager()).annotationCouldNotBeAdded("no link selected");
            dismiss();
        }
        init();
        try {
            Action linkAction = mLink.getAction();
            if(!linkAction.isValid()){
                return;
            }
            if(linkAction.getType() == Action.e_GoTo){
                Destination dest = linkAction.getDest();
                if(dest.isValid()){
                    Page linkPage = dest.getPage();

                    if(linkPage!= null){

                        int linkPageNum = dest.getPage().getIndex();
                        pageEditText.setText(""+linkPageNum);
                        pageEditText.requestFocus();
                        externalRadioButton.setChecked(false);
                        mSelectedRadioButton = internalRadioButton;
                    }
                }
            }
            else if(linkAction.getType() == Action.e_URI){
                String target = linkAction.getSDFObj().get("URI").value().getAsPDFText();
                urlEditText.setText(target);
                urlEditText.requestFocus();
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            e.printStackTrace();
        }

    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater) mCtrl.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogView = inflater.inflate(R.layout.tools_dialog_link_editor, null);
        mPosButton = null;

        externalRadioButton = (RadioButton) dialogView.findViewById(R.id.tools_dialog_link_external_radio_button);
        externalRadioButton.setOnClickListener(this);
        internalRadioButton = (RadioButton) dialogView.findViewById(R.id.tools_dialog_link_internal_radio_button);
        internalRadioButton.setOnClickListener(this);

        urlEditText = (EditText) dialogView.findViewById(R.id.tools_dialog_link_external_edit_text);
        urlEditText.setOnFocusChangeListener(this);
        urlEditText.setSelectAllOnFocus(true);
        urlEditText.addTextChangedListener(new UrlChangeListener());

        pageEditText = (EditText) dialogView.findViewById(R.id.tools_dialog_link_internal_edit_text);
        pageEditText.setOnFocusChangeListener(this);
        pageEditText.setSelectAllOnFocus(true);
        pageEditText.addTextChangedListener(new PageChangeListener());
        mPageCount = mCtrl.getPageCount();
        if (mPageCount > 0) {
            String mHint = String.format(mCtrl.getResources().getString(R.string.dialog_gotopage_number),
                    1, mPageCount);
            pageEditText.setHint(mHint);
        }
        mSelectedRadioButton = externalRadioButton;
        mSelectedRadioButton.setChecked(true);

        int lastOption = PdfViewCtrlSettingsManager.getLinkEditLastOption(getContext());
        boolean isLinkingPage = lastOption == LINK_OPTION_PAGE;
        if(isLinkingPage){
            urlEditText.clearFocus();
            pageEditText.requestFocus();
        }

        setView(dialogView);

        // Add two button
        setButton(DialogInterface.BUTTON_POSITIVE, mCtrl.getContext().getString(R.string.ok), this);

        setButton(DialogInterface.BUTTON_NEGATIVE, mCtrl.getContext().getString(R.string.cancel), this);

        mAnnots = new HashMap<>();
    }

    /**
     * The overload implementation of {@link AlertDialog#show()}.
     */
    @Override
    public void show() {
        super.show();
        mPosButton = getButton(DialogInterface.BUTTON_POSITIVE);
        mPosButton.setEnabled(false);
    }

    /**
     * The overload implementation of {@link View.OnClickListener#onClick(View)}.
     */
    @Override
    public void onClick(View v) {
        if (v instanceof RadioButton && mSelectedRadioButton != v) {
            if (mSelectedRadioButton == internalRadioButton) {
                urlEditText.requestFocus();
            } else {
                pageEditText.requestFocus();
            }
        }
    }

    /**
     * The overload implementation of {@link DialogInterface.OnClickListener#onClick(DialogInterface, int)}.
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (mSelectedRadioButton == null || dialogView == null ) {
                    dismiss();
                    return;
                }
                if (mSelectedRadioButton == externalRadioButton && !urlEditText.getText().toString().equals("")) {
                    PdfViewCtrlSettingsManager.setLinkEditLastOption(getContext(), LINK_OPTION_URL);
                    if (mLink == null) {
                        createLink(urlEditText.getText().toString(), -1);
                    } else {
                        editLink(urlEditText.getText().toString(), -1);
                    }
                }
                if (mSelectedRadioButton == internalRadioButton && !pageEditText.getText().toString().equals("")) {
                    int pageNum = Integer.parseInt(pageEditText.getText().toString());
                    if (pageNum > 0 && pageNum <= mCtrl.getPageCount()) {
                        PdfViewCtrlSettingsManager.setLinkEditLastOption(getContext(), LINK_OPTION_PAGE);
                        if (mLink == null) {
                            createLink("", pageNum);
                        } else {
                            editLink("", pageNum);
                        }
                    }
                }
                dismiss();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                cancel();
                break;
        }
    }

    /**
     * The overload implementation of {@link AlertDialog#dismiss()}.
     */
    @Override
    public void dismiss() {
        super.dismiss();
        mTool.unsetAnnot();
        mCtrl.invalidate();
    }

    /**
     * The overload implementation of {@link View.OnFocusChangeListener#onFocusChange(View, boolean)}.
     */
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
            return;
        }
        if (v == urlEditText && mSelectedRadioButton != externalRadioButton) {
            mSelectedRadioButton.setChecked(false);
            mSelectedRadioButton = externalRadioButton;
        } else if (v == pageEditText) {
            mSelectedRadioButton.setChecked(false);
            mSelectedRadioButton = internalRadioButton;
        }

        if (v instanceof EditText && mPosButton != null){
            String txt = ((EditText)v).getText().toString();
            if (Utils.isNullOrEmpty(txt)){
                mPosButton.setEnabled(false);
            } else if (v == pageEditText ){
                try {
                    int page = Integer.parseInt(txt);
                    if (page <= mPageCount && page >0){
                        mPosButton.setEnabled(true);
                    } else {
                        mPosButton.setEnabled(false);
                    }
                }catch (NumberFormatException e){
                    mPosButton.setEnabled(false);
                }
            }else {
                mPosButton.setEnabled(true);
            }
        }
        mSelectedRadioButton.setChecked(true);
    }

    /**
     * Sets the color.
     *
     * @param color The color
     */
    public void setColor(int color) {
        mStrokeColor = color;
    }

    /**
     * Sets the thickness.
     *
     * @param thickness The thickness
     */
    public void setThickness(float thickness) {
        mThickness = thickness;
    }

    private void setStyle(Link linkMarkup){
        try {
            ColorPt colorPt = Utils.color2ColorPt(mStrokeColor);
            linkMarkup.setColor(colorPt, 3);
            // Set the annotation border width to 3 points...
            Annot.BorderStyle bs = linkMarkup.getBorderStyle();
            // bs.setAnnotStyle(Annot.BorderStyle.e_underline);
            bs.setWidth(mThickness);
            linkMarkup.setBorderStyle(bs);
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            e.printStackTrace();
        }
    }

    private void createLink(String url, int page) {
        if (mSelRect == null || (url.equals("") && page == -1)) {
            return;
        }
        LinkedList<AnnotUpdateInfo> updateInfoList = new LinkedList<>();
        boolean shouldUnlock = false;

        try {
            mCtrl.docLock(true);
            shouldUnlock = true;
            for (Map.Entry<Rect, Integer> entry : mSelRect.entrySet()) {
                Rect bbox = entry.getKey();
                int pg = entry.getValue();
                Link linkMarkup;
                if (page > -1) {
                    Action gotoPage = Action.createGoto(Destination.createFitBH(mCtrl.getDoc().getPage(page), mCtrl.getDoc().getPage(page).getPageHeight()));
                    linkMarkup = Link.create(mCtrl.getDoc().getSDFDoc(), bbox, gotoPage);
                } else {
                    linkMarkup = Link.create(mCtrl.getDoc(), bbox, Action.createURI(mCtrl.getDoc(), url));
                }
                setStyle(linkMarkup);

                linkMarkup.refreshAppearance();

                Page page1 = mCtrl.getDoc().getPage(pg);
                page1.annotPushBack(linkMarkup);
                mTool.mAnnotPushedBack = true;

                Obj obj = linkMarkup.getSDFObj();
                obj.putString(Tool.PDFTRON_ID, "");

                mTool.setAnnot(linkMarkup, pg);
                mTool.buildAnnotBBox();

                RectF rectF = mTool.getAnnotRect();

                Rect ur = new Rect(rectF.left, rectF.top, rectF.right, rectF.bottom);
                updateInfoList.add(new AnnotUpdateInfo(linkMarkup, pg, ur));
            }
            // make sure to raise add event after mPdfViewCtrl.update
            for (AnnotUpdateInfo updateInfo : updateInfoList) {
                Rect rect = updateInfo.mRect;
                mCtrl.update(rect);
                if (updateInfo.mAnnot != null) {
                    mAnnots.put(updateInfo.mAnnot, updateInfo.mPageNum);
                    mCtrl.update(updateInfo.mAnnot, updateInfo.mPageNum);
                }
            }
            mCtrl.docUnlock();
            shouldUnlock = false;
            mTool.raiseAnnotationAddedEvent(mAnnots);
        } catch (Exception e) {
            ((ToolManager) mCtrl.getToolManager()).annotationCouldNotBeAdded(e.getMessage());
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mCtrl.docUnlock();
            }
        }
    }

    private void editLink(String url, int page) {
        if (mLink == null || (url.equals("") && page == -1)) {
            return;
        }
        boolean shouldUnlock = false;
        try {
            mCtrl.docLock(true);
            shouldUnlock = true;
            PDFDoc mDoc = mCtrl.getDoc();

            mCtrl.docUnlock();
            shouldUnlock = false;

            if (page == -1 && !url.equals("")) {
                mLink.setAction(Action.createURI(mCtrl.getDoc(), url));
            }
            mCtrl.docLock(true);
            shouldUnlock = true;
            Page mDocPage = mDoc.getPage(page);
            mCtrl.docUnlock();
            shouldUnlock = false;

            int pageCount = mCtrl.getPageCount();
            if (page > 0 && page <= pageCount) {
                Action gotoPage = Action.createGoto(Destination.createFitBH(mCtrl.getDoc().getPage(page), mCtrl.getDoc().getPage(page).getPageHeight()));
                mLink.setAction(gotoPage);
            }
            mCtrl.docLock(true);
            shouldUnlock = true;
            if (mTool != null) {
                mLinkPage = mLink.getPage().getIndex();
                mTool.raiseAnnotationPreModifyEvent(mLink, mLinkPage);
                mCtrl.update(mLink, mLinkPage);
                mLink.refreshAppearance();

                // make sure to raise modify event after mPdfViewCtrl.update and before unsetAnnot
                mTool.raiseAnnotationModifiedEvent(mLink, mLinkPage);
                mAnnots.put(mLink, mLinkPage);
            }
        } catch (Exception e) {
            ((ToolManager) mCtrl.getToolManager()).annotationCouldNotBeAdded(e.getMessage());
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mCtrl.docUnlock();
            }
        }
    }

    private class PageChangeListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mPosButton == null) {
                return;
            }
            if (Utils.isNullOrEmpty(s.toString())) {
                mPosButton.setEnabled(false);
                return;
            }
            try {
                int page = Integer.parseInt(s.toString().trim());
                if (page > 0 && page <= mPageCount) {
                    mPosButton.setEnabled(true);
                } else {
                    Log.d("DialogLinkEditor", "page greater");
                    mPosButton.setEnabled(false);
                }
            } catch (NumberFormatException e) {
                mPosButton.setEnabled(false);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private class UrlChangeListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mPosButton == null) {
                return;
            }
            if (Utils.isNullOrEmpty(s.toString())) {
                mPosButton.setEnabled(false);
                return;
            }
            mPosButton.setEnabled(true);
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }

    private class AnnotUpdateInfo {
        public Link mAnnot;
        public int mPageNum;
        com.pdftron.pdf.Rect mRect;

        AnnotUpdateInfo(Link linkMarkup, int pageNum, com.pdftron.pdf.Rect rect) {
            mAnnot = linkMarkup;
            mPageNum = pageNum;
            mRect = rect;
        }
    }
}
