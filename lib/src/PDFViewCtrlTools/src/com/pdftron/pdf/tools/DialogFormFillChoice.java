//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.ActionParameter;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.ViewChangeCollection;
import com.pdftron.pdf.adapter.FormFillAdapter;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.utils.ActionUtils;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.sdf.Obj;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * A dialog about choosing selection in Form choice field.
 */
public class DialogFormFillChoice extends AlertDialog implements
        android.view.View.OnClickListener,
        android.content.DialogInterface.OnDismissListener,
        View.OnFocusChangeListener,
        FormFillAdapter.OnItemSelectListener {

    private PDFViewCtrl mPdfViewCtrl;
    private Annot mAnnot;
    private int mAnnotPageNum;
    private com.pdftron.pdf.Field mField;
    private boolean mSingleChoice;
    private boolean mMultiChoice;
    private boolean mIsCombo;
    private boolean mIsEditable;
    private boolean mCancelled;
    private EditText mOtherOptionText;
    private RadioButton mOtherOptionRadioBtn;
    private FormFillAdapter mFormFillAdapter;

    /**
     * Class constructor
     *
     * @param pdfViewCtrl The PDFViewCtrl
     * @param annot The annotation
     * @param annotPageNum The page number where the annotation is on
     */
    @SuppressWarnings("WeakerAccess")
    public DialogFormFillChoice(@NonNull PDFViewCtrl pdfViewCtrl, @NonNull Annot annot, int annotPageNum) {
        // Initialization
        super(pdfViewCtrl.getContext());

        mPdfViewCtrl = pdfViewCtrl;
        mAnnot = annot;
        mAnnotPageNum = annotPageNum;
        mSingleChoice = true;
        mIsCombo = false;
        mIsEditable = false;

        try {
            Widget w = new Widget(mAnnot);
            mField = w.getField();
            if (!mField.isValid()) {
                dismiss();
                return;
            }
            mIsCombo = mField.getFlag(Field.e_combo);
            mSingleChoice = mIsCombo || !mField.getFlag(Field.e_multiselect);
            mMultiChoice = mField.getFlag(Field.e_multiselect);
            mIsEditable = mField.getFlag(Field.e_edit);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            dismiss();
            return;
        }

        // Setup view
        LayoutInflater inflater = (LayoutInflater) mPdfViewCtrl.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return;
        }
        View view = inflater.inflate(R.layout.tools_dialog_formfillchoice, null);
        setView(view);
        setOnDismissListener(this);

        // other options
        LinearLayout otherOptionLayout = view.findViewById(R.id.tools_dialog_formfillchoice_edit_text_layout);
        mOtherOptionText = view.findViewById(R.id.tools_dialog_formfillchoice_edit_text);
        mOtherOptionText.setOnFocusChangeListener(this);
        mOtherOptionText.setSelectAllOnFocus(true);
        mOtherOptionRadioBtn = view.findViewById(R.id.tools_dialog_formfillchoice_edit_text_ratio_button);
        mOtherOptionRadioBtn.setOnClickListener(this);
        final Button confirmButton = view.findViewById(R.id.button_ok);
        confirmButton.setOnClickListener(this);
        Button cancelButton = view.findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(this);

        if (!mIsEditable) {
            otherOptionLayout.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            confirmButton.setVisibility(View.GONE);
        }

        HashSet<Integer> selectedPositions;
        if (mSingleChoice) {
            selectedPositions = new HashSet<>(1);
            try {
                String selectedStr = mField.getValueAsString();
                int cnt = mField.getOptCount();
                for (int i = 0; i < cnt; ++i) {
                    String text = mField.getOpt(i);
                    if (selectedStr.equals(text)) {
                        selectedPositions.add(i);
                        break;
                    }
                }
                if (selectedPositions.isEmpty() && otherOptionLayout.getVisibility() == View.VISIBLE) {
                    mOtherOptionRadioBtn.setChecked(true);
                    mOtherOptionText.setText(selectedStr);
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        } else {
            otherOptionLayout.setVisibility(View.GONE);
            confirmButton.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            selectedPositions = getSelectedPositions();
        }

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_form_fill);
        recyclerView.setLayoutManager(new LinearLayoutManager(pdfViewCtrl.getContext()));
        mFormFillAdapter = new FormFillAdapter(mField, selectedPositions, this);
        recyclerView.setAdapter(mFormFillAdapter);

        int selectedPosition = -1;
        if (!mSingleChoice && selectedPositions != null) {
            Iterator<Integer> itr = selectedPositions.iterator();
            if (itr.hasNext()) {
                selectedPosition = itr.next();
            }
        }
        if (selectedPosition != -1) {
            recyclerView.scrollToPosition(selectedPosition);
        }
    }

    /**
     * The overload implementation of {@link View.OnClickListener#onClick(View)}.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_ok) {
            try {
                String defaultStr = mField.getDefaultValueAsString();
                if (mOtherOptionText.getText().toString().equals("")) {
                    mOtherOptionText.setText(defaultStr);
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
            dismiss();
        } else if (v.getId() == R.id.button_cancel) {
            mCancelled = true;
            dismiss();
        } else if (v.getId() == mOtherOptionRadioBtn.getId() && mFormFillAdapter.hasSingleSelectedPosition()) {
            mFormFillAdapter.clearSingleSelectedPosition();
            mOtherOptionRadioBtn.setChecked(true);
            mOtherOptionText.requestFocus();
        }
    }

    /**
     * The overload implementation of {@link FormFillAdapter.OnItemSelectListener#onItemSelected(int)}.
     */
    @Override
    public void onItemSelected(int position) {
        if (mSingleChoice || (!mIsEditable && !mMultiChoice)) {
            dismiss();
        } else if (mOtherOptionText.hasFocus()) {
            mOtherOptionText.clearFocus();
            // hide virtual keyboard
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mOtherOptionText.getWindowToken(), 0);
            }
        }
        mOtherOptionRadioBtn.setChecked(false);
    }

    /**
     * The overload implementation of {@link android.content.DialogInterface.OnDismissListener#onDismiss(DialogInterface)}.
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mCancelled) {
            return;
        }

        int fieldCount;
        try {
            fieldCount = mField.getOptCount();
        } catch (Exception e) {
            fieldCount = 0;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            if (mSingleChoice) {
                // When the text was set it was added two spaces at the beginning.
                // So we have to remove them when comparing.
                String str;
                if (!mFormFillAdapter.hasSingleSelectedPosition()) {
                    str = mOtherOptionText.getText().toString();
                } else {
                    str = mField.getOpt(mFormFillAdapter.getSingleSelectedPosition());
                }

                if (!mIsCombo || !mField.getValueAsString().equals(str)) {
                    raiseAnnotationPreEditEvent(mAnnot);
                    ViewChangeCollection view_change = mField.setValue(str);
                    mPdfViewCtrl.refreshAndUpdate(view_change);
                    executeAction(mField, Annot.e_action_trigger_annot_blur);
                    executeAction(mField, Annot.e_action_trigger_annot_exit);
                    raiseAnnotationEditedEvent(mAnnot);
                }
            } else if (fieldCount > 0) {
                raiseAnnotationPreEditEvent(mAnnot);
                PDFDoc doc = mPdfViewCtrl.getDoc();
                Obj arr = doc.createIndirectArray();
                HashSet<Integer> selectedPositions = mFormFillAdapter.getSelectedPositions();
                TreeSet<Integer> treeSet = new TreeSet<>(selectedPositions);
                // list needs to be sorted
                for (int selectedPosition : treeSet) {
                    String text = mField.getOpt(selectedPosition);
                    arr.pushBackText(text);
                }
                ViewChangeCollection view_change = mField.setValue(arr);
                mPdfViewCtrl.refreshAndUpdate(view_change);
                executeAction(mField, Annot.e_action_trigger_annot_blur);
                executeAction(mField, Annot.e_action_trigger_annot_exit);
                raiseAnnotationEditedEvent(mAnnot);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
            this.dismiss();
        }
    }

    /**
     * Execute action
     * <p>
     * <div class="warning">
     * The PDF doc should have been locked when call this method.
     * In addition, ToolManager's raise annotation should be handled in the caller function.
     * </div>
     */
    private void executeAction(Field fld, int type) {
        if (mAnnot != null) {
            try {
                Obj aa = mAnnot.getTriggerAction(type);
                if (aa != null) {
                    Action a;
                    a = new Action(aa);
                    ActionParameter action_param;
                    action_param = new ActionParameter(a, fld);
                    ActionUtils.getInstance().executeAction(action_param, mPdfViewCtrl);
                }
            } catch (PDFNetException e) {
                e.printStackTrace();
            }
        }
    }

    // Find the selected items from a multiple choice list
    private HashSet<Integer> getSelectedPositions() {
        try {
            HashSet<Integer> al = new HashSet<>();
            Obj val = mField.getValue();
            if (val != null) {
                if (val.isString()) {
                    Obj o = mAnnot.getSDFObj().findObj("Opt");
                    if (o != null) {
                        int id = getOptIdx(val, o);
                        if (id >= 0) {
                            al.add(id);
                        }
                    }
                } else if (val.isArray()) {
                    int sz = (int) val.size();
                    for (int i = 0; i < sz; ++i) {
                        Obj entry = val.getAt(i);
                        if (entry.isString()) {
                            Obj o = mAnnot.getSDFObj().findObj("Opt");
                            if (o != null) {
                                int id = getOptIdx(entry, o);
                                if (id >= 0) {
                                    al.add(id);
                                }
                            }
                        }
                    }
                }
            }

            return al;

        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return null;
        }
    }

    private Integer getOptIdx(Obj str_val, Obj opt) {
        try {
            int sz = (int) opt.size();
            String str_val_string = new String(str_val.getBuffer());
            for (int i = 0; i < sz; ++i) {
                Obj v = opt.getAt(i);
                if (v.isString() && str_val.size() == v.size()) {
                    String v_string = new String(v.getBuffer());
                    if (str_val_string.equals(v_string)) {
                        return i;
                    }
                } else if (v.isArray() && v.size() >= 2 && v.getAt(1).isString() && str_val.size() == v.getAt(1).size()) {
                    v = v.getAt(1);
                    String v_string = new String(v.getBuffer());
                    if (str_val_string.equals(v_string)) {
                        return i;
                    }
                }
            }

        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        return -1;
    }

    private void raiseAnnotationPreEditEvent(Annot annot) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, mAnnotPageNum);
        toolManager.raiseAnnotationsPreModifyEvent(annots);
    }

    private void raiseAnnotationEditedEvent(Annot annot) {
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, mAnnotPageNum);

        Bundle bundle = Tool.getAnnotationModificationBundle(null);
        toolManager.raiseAnnotationsModifiedEvent(annots, bundle);
    }

    /**
     * The overload implementation of {@link View.OnFocusChangeListener#onFocusChange(View, boolean)}.
     */
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v instanceof EditText) {
            if (hasFocus) {
                if (mFormFillAdapter.hasSingleSelectedPosition()) {
                    mFormFillAdapter.clearSingleSelectedPosition();
                    // show virtual keyboard
                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInputFromInputMethod(mOtherOptionText.getWindowToken(), 0);
                    }
                }
                mOtherOptionRadioBtn.setChecked(true);
            } else {
                mOtherOptionRadioBtn.setChecked(false);
            }
        }
    }
}
