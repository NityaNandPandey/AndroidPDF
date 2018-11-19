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
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.ViewChangeCollection;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;

import java.util.HashMap;

/**
 * A dialog about entering text in form text field
 */
@SuppressWarnings("WeakerAccess")
public class DialogFormFillText extends AlertDialog {

    private PDFViewCtrl mCtrl;
    private Annot mAnnot;
    private int mAnnotPageNum;
    private com.pdftron.pdf.Field mField;
    private EditText mTextBox;

    /**
     * Class constructor
     *
     * @param ctrl The PDFViewCtrl
     * @param annot The annotation
     * @param annotPageNum The page number where the annotation is on
     */
    public DialogFormFillText(@NonNull PDFViewCtrl ctrl, @NonNull Annot annot, int annotPageNum) {
        // Initialization
        super(ctrl.getContext());
        mCtrl = ctrl;
        mAnnot = annot;
        mAnnotPageNum = annotPageNum;
        mField = null;
        try {
            Widget w = new Widget(mAnnot);
            mField = w.getField();
            if (!mField.isValid()) {
                dismiss();
                return;
            }
        } catch (Exception e) {
            dismiss();
            return;
        }

        // Setup view
        LayoutInflater inflater = (LayoutInflater) mCtrl.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.tools_dialog_formfilltext, null);
        mTextBox = (EditText) view.findViewById(R.id.tools_dialog_formfilltext_edit_text);
        setTitle(mCtrl.getContext().getString(R.string.tools_dialog_formfilltext_title));
        setView(view);

        try {
            // Compute alignment
            boolean multiple_line = mField.getFlag(Field.e_multiline);
            mTextBox.setSingleLine(!multiple_line);
            int just = mField.getJustification();
            if (just == Field.e_left_justified) {
                mTextBox.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            } else if (just == Field.e_centered) {
                mTextBox.setGravity(Gravity.CENTER | Gravity.CENTER_VERTICAL);
            } else if (just == Field.e_right_justified) {
                mTextBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            }

            // Password format
            if (mField.getFlag(Field.e_password)) {
                mTextBox.setTransformationMethod(new PasswordTransformationMethod());
            }

            // Set initial text
            String str = mField.getValueAsString();
            mTextBox.setText(str);
            // Set the caret position to the end of the text
            mTextBox.setSelection(mTextBox.getText().length());

            // Max length
            int max_len = mField.getMaxLen();
            if (max_len >= 0) {
                LengthFilter filters[] = new LengthFilter[1];
                filters[0] = new InputFilter.LengthFilter(max_len);
                mTextBox.setFilters(filters);
            }

        } catch (Exception e) {
            dismiss();
            return;
        }

        // Add two button
        setButton(DialogInterface.BUTTON_POSITIVE, mCtrl.getContext().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String str = mTextBox.getText().toString();
                boolean shouldUnlock = false;
                try {
                    mCtrl.docLock(true);
                    shouldUnlock = true;
                    raiseAnnotationPreEditEvent(mAnnot);
                    ViewChangeCollection view_change =  mField.setValue(str);
                    mCtrl.refreshAndUpdate(view_change);
                    raiseAnnotationEditedEvent(mAnnot);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    if (shouldUnlock) {
                        mCtrl.docUnlock();
                    }
                }
            }
        });

        setButton(DialogInterface.BUTTON_NEGATIVE, mCtrl.getContext().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });
    }

    private void raiseAnnotationPreEditEvent(Annot annot) {
        ToolManager toolManager = (ToolManager) mCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, mAnnotPageNum);
        toolManager.raiseAnnotationsPreModifyEvent(annots);
    }

    private void raiseAnnotationEditedEvent(Annot annot) {
        ToolManager toolManager = (ToolManager) mCtrl.getToolManager();
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, mAnnotPageNum);
        Bundle bundle = Tool.getAnnotationModificationBundle(null);
        toolManager.raiseAnnotationsModifiedEvent(annots, bundle);
    }
}
