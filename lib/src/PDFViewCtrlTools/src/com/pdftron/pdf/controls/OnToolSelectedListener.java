package com.pdftron.pdf.controls;

import android.view.View;

/**
 * Callback interface to be invoked when user select a button shown in the edit toolbar.
 */
public interface OnToolSelectedListener {
    /**
     * Called when a draw button has been selected.
     *
     * @param drawIndex        The index of draw button that was selected
     * @param isSelectedBefore Specify if the button has been already selected
     * @param anchor           The view of the selected button which will be used as an anchor
     */
    void onDrawSelected(int drawIndex, boolean isSelectedBefore, View anchor);

    /**
     * Called when the clear button has been selected.
     */
    void onClearSelected();

    /**
     * Called when the eraser button has been selected.
     *
     * @param isSelectedBefore Specify if the button has been already selected
     * @param anchor           The view of the button which will be used as an anchor
     */
    void onEraserSelected(boolean isSelectedBefore, View anchor);

    /**
     * Called when the undo button has been selected.
     */
    void onUndoSelected();

    /**
     * Called when the redo button has been selected.
     */
    void onRedoSelected();

    /**
     * Called when the close button has been selected.
     */
    void onCloseSelected();
}
