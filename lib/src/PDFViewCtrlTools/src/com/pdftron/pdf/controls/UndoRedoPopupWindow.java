package com.pdftron.pdf.controls;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.UndoRedoManager;
import com.pdftron.pdf.utils.Utils;

/**
 * A popup window class that displays undo/redo options
 */

@SuppressWarnings("WeakerAccess")
public class UndoRedoPopupWindow extends PopupWindow {
    private TextView mUndoTextView;
    private TextView mRedoTextView;
    private UndoRedoManager mUndoRedoManager;

    /**
     * Callback interface to be invoked when undo/redo is called.
     */
    public interface OnUndoRedoListener {
        /**
         * Called when undo/redo has been called.
         */
        void onUndoRedoCalled();
    }

    private OnUndoRedoListener mOnUndoRedoListener;


    /**
     * Class constructor
     *
     * @param context The context
     * @param undoRedoManager The UndoRedoManager
     * @param listener The listener to undo/redo event
     */
    @SuppressWarnings("unused")
    public UndoRedoPopupWindow(final Context context, UndoRedoManager undoRedoManager, OnUndoRedoListener listener) {
        this(context, undoRedoManager, listener, 0);
    }

    /**
     * Class constructor
     *
     * @param context The context
     * @param undoRedoManager The UndoRedoManager
     * @param listener The listener to undo/redo event
     * @param locationId The location ID of where this is called from.
     *                   It will be used in Analytics Handler.
     */
    public UndoRedoPopupWindow(final Context context, UndoRedoManager undoRedoManager, OnUndoRedoListener listener, final int locationId) {
       this(context, undoRedoManager, listener, R.layout.dialog_undo_redo, locationId);
    }


    /**
     * Class constructor
     *
     * @param context The context
     * @param undoRedoManager The UndoRedoManager
     * @param listener The listener to undo/redo event
     * @param layoutResource The layout resource id for inflating the popup window view
     * @param locationId The location ID of where this is called from.
     *                   It will be used in Analytics Handler.
     */
    protected UndoRedoPopupWindow(final Context context, UndoRedoManager undoRedoManager, OnUndoRedoListener listener, @LayoutRes int layoutResource, final int locationId) {
        super(context);

        setOutsideTouchable(true);
        setFocusable(false);

        setAnimationStyle(R.style.Controls_AnnotationPopupAnimation);
        mUndoRedoManager = undoRedoManager;
        mOnUndoRedoListener = listener;

        final View rootView = LayoutInflater.from(context).inflate(layoutResource, null);
        this.setContentView(rootView);

        mUndoTextView = rootView.findViewById(R.id.undo_title);
        mUndoTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUndoRedoManager != null && mUndoRedoManager.getPdfViewCtrl() != null) {
                    PDFViewCtrl pdfViewCtrl = mUndoRedoManager.getPdfViewCtrl();
                    String undoInfo = mUndoRedoManager.undo(locationId, false);
                    UndoRedoManager.jumpToUndoRedo(pdfViewCtrl, undoInfo, true);
                    updateUndoRedo();
                }
            }
        });

        mRedoTextView = rootView.findViewById(R.id.redo_title);
        mRedoTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUndoRedoManager != null && mUndoRedoManager.getPdfViewCtrl() != null) {
                    PDFViewCtrl pdfViewCtrl = mUndoRedoManager.getPdfViewCtrl();
                    String redoInfo = mUndoRedoManager.redo(locationId, false);
                    UndoRedoManager.jumpToUndoRedo(pdfViewCtrl, redoInfo, false);
                    updateUndoRedo();
                }
            }
        });

        updateUndoRedo();
    }

    private void updateUndoRedo() {
        if (mUndoRedoManager != null) {
            if (mUndoTextView != null) {
                String nextUndoAction = mUndoRedoManager.getNextUndoAction();
                if (!Utils.isNullOrEmpty(nextUndoAction)) {
                    mUndoTextView.setEnabled(true);
                    mUndoTextView.setText(nextUndoAction);
                } else {
                    mUndoTextView.setEnabled(false);
                    mUndoTextView.setText(R.string.undo);
                }
            }

            if (mRedoTextView != null) {
                String nextRedoAction = mUndoRedoManager.getNextRedoAction();
                if (!Utils.isNullOrEmpty(nextRedoAction)) {
                    mRedoTextView.setEnabled(true);
                    mRedoTextView.setText(nextRedoAction);
                } else {
                    mRedoTextView.setEnabled(false);
                    mRedoTextView.setText(R.string.redo);
                }
            }

            setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
            setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

            if (mOnUndoRedoListener != null) {
                mOnUndoRedoListener.onUndoRedoCalled();
            }
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mUndoRedoManager.sendConsecutiveUndoRedoEvent();
    }
}
