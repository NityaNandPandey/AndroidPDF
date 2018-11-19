package com.pdftron.pdf.tools;

import android.graphics.Color;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.ColorSpace;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.Utils;

import java.util.UUID;

/**
 * This class is for creating checkbox field
 */
@Keep
public class CheckboxFieldCreate extends RectCreate {

    /**
     * Class constructor
     *
     * @param ctrl the PDFViewCtrl
     */
    public CheckboxFieldCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.FORM_CHECKBOX_CREATE;
    }

    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        String fieldName = UUID.randomUUID().toString();
        String defaultFieldValue = "Yes";
        Field textField = doc.fieldCreate(fieldName, Field.e_check, defaultFieldValue);
        textField.setValue(false);
        Widget annot = Widget.create(doc, bbox, textField);
        ColorPt colorPt = Utils.color2ColorPt(Color.WHITE);
        annot.setBackgroundColor(colorPt, ColorSpace.e_device_rgb);
        annot.setAppearance(createFormFieldAppearance(doc,
            FORM_FIELD_SYMBOL_CHECKBOX), Annot.e_normal, defaultFieldValue);
        annot.getSDFObj().putString(PDFTRON_ID, "");
        return annot;
    }
}
