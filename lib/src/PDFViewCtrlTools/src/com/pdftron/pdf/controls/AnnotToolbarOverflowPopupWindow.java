package com.pdftron.pdf.controls;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.UndoRedoManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.Utils;

/**
 * A popup window class that displays undo/redo options
 */

@SuppressWarnings("WeakerAccess")
public class AnnotToolbarOverflowPopupWindow extends UndoRedoPopupWindow {
    private TextView mShowMoreTitle;
    private AnnotationToolbar mAnnotationToolbar;

    public AnnotToolbarOverflowPopupWindow(Context context, UndoRedoManager undoRedoManager, OnUndoRedoListener listener, AnnotationToolbar annotationToolbar) {
        this(context, undoRedoManager, listener, R.layout.dialog_annot_toolbar_overflow, AnalyticsHandlerAdapter.LOCATION_ANNOTATION_TOOLBAR, annotationToolbar);
    }

    protected AnnotToolbarOverflowPopupWindow(Context context, UndoRedoManager undoRedoManager, OnUndoRedoListener listener, int layoutResource, int locationId, AnnotationToolbar annotationToolbar) {
        super(context, undoRedoManager, listener, layoutResource, locationId);
        setAnnotationToolbar(annotationToolbar);
        init();
    }

    private void init() {
        mShowMoreTitle = getContentView().findViewById(R.id.show_more_title);
        mShowMoreTitle.setText(mAnnotationToolbar.isExpanded() ? R.string.show_fewer_tools : R.string.show_all_tools);
        mShowMoreTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAnnotationToolbar.toggleExpanded();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_SHOW_ALL_TOOLS,
                    AnalyticsParam.showAllToolsParam(mAnnotationToolbar.isExpanded()));
                dismiss();
            }
        });

        if (!mAnnotationToolbar.isExpanded() &&
            (Utils.isLandscape(mShowMoreTitle.getContext()) || Utils.isTablet(mShowMoreTitle.getContext()))) {
            mShowMoreTitle.setVisibility(View.GONE);
        }
    }

    private void setAnnotationToolbar(AnnotationToolbar annotationToolbar) {
        mAnnotationToolbar = annotationToolbar;
    }
}
