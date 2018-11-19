package com.pdftron.pdf.tools;

import android.support.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.tools.ToolManager.ToolMode;

import java.util.UUID;

/**
 * @hide
 * This class is for creating radio group field
 */
public class RadioGroupFieldCreate extends RectCreate {

    private Field mTargetField = null;
    /**
     * Class constructor
     *
     * @param ctrl the PDFViewCtrl
     */
    public RadioGroupFieldCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    public void setTargetField(Field field) {
        mTargetField = field;
    }

    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        String fieldName = UUID.randomUUID().toString();
        String defaultFieldValue = "No";
        Field field = mTargetField;
        if (null == field) {
            field = doc.fieldCreate(fieldName, Field.e_radio, defaultFieldValue, defaultFieldValue);
        }
        boolean checked = field.getValueAsBool();
        Annot annot = Widget.create(doc, bbox, field);
        annot.setAppearance(createFormFieldAppearance(doc,
            FORM_FIELD_SYMBOL_CIRCLE), Annot.e_normal, defaultFieldValue);
        field.setValue(checked);
        annot.getSDFObj().putString(PDFTRON_ID, "");
        return annot;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.FORM_RADIO_GROUP_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Widget;
    }
}
