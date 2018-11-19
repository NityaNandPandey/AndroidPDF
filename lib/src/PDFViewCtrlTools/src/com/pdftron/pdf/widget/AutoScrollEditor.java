package com.pdftron.pdf.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.CustomRelativeLayout;
import com.pdftron.pdf.tools.R;

/**
 * A {@link CustomRelativeLayout} that contains an {@link AutoScrollEditText}.
 */
public class AutoScrollEditor extends CustomRelativeLayout {

    private AutoScrollEditText mEditText;

    /**
     * Gets edit text
     * @return edit text
     */
    public AutoScrollEditText getEditText() {
        return mEditText;
    }

    public AutoScrollEditor(Context context) {
        super(context);
        init();
    }

    public AutoScrollEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AutoScrollEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.widget_auto_scroll_editor, this);
        mEditText = view.findViewById(R.id.editText);
        mZoomWithParent = true;
    }

    public void setAnnotStyle(PDFViewCtrl pdfViewCtrl, AnnotStyle annotStyle) {
        mEditText.setAnnotStyle(pdfViewCtrl, annotStyle);
    }
}
