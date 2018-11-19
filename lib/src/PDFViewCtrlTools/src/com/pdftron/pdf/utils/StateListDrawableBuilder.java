//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

/**
 * A builder class to programmatically create state list drawable
 */

@SuppressWarnings("unused")
public class StateListDrawableBuilder {

    private static final int[] STATE_SELECTED = new int[]{android.R.attr.state_selected};
    private static final int[] STATE_PRESSED = new int[]{android.R.attr.state_pressed};
    private static final int[] STATE_FOCUSED = new int[]{android.R.attr.state_focused};
    private static final int[] STATE_HOVERED = new int[]{android.R.attr.state_hovered};
    private static final int[] STATE_ENABLED = new int[]{android.R.attr.state_enabled};
    private static final int[] STATE_DISABLED = new int[]{-android.R.attr.state_enabled};

    private Drawable mNormalDrawable;
    private Drawable mSelectedDrawable;
    private Drawable mFocusedDrawable;
    private Drawable mHoveredDrawable;
    private Drawable mPressedDrawable;
    private Drawable mDisabledDrawable;

    /**
     * Sets the drawable in normal state.
     *
     * @param normalDrawable The drawable in normal state
     *
     * @return The class
     */
    public StateListDrawableBuilder setNormalDrawable(Drawable normalDrawable) {
        mNormalDrawable = normalDrawable;
        return this;
    }

    /**
     * Sets the drawable in pressed state.
     *
     * @param pressedDrawable The drawable in pressed state
     *
     * @return The class
     */
    public StateListDrawableBuilder setPressedDrawable(Drawable pressedDrawable) {
        mPressedDrawable = pressedDrawable;
        return this;
    }

    /**
     * Sets the drawable in selected state.
     *
     * @param selectedDrawable The drawable in selected state
     *
     * @return The class
     */
    public StateListDrawableBuilder setSelectedDrawable(Drawable selectedDrawable) {
        mSelectedDrawable = selectedDrawable;
        return this;
    }

    /**
     * Sets the drawable in focused state.
     *
     * @param focusedDrawable The drawable in focused state
     *
     * @return The class
     */
    public StateListDrawableBuilder setFocusedDrawable(Drawable focusedDrawable) {
        mFocusedDrawable = focusedDrawable;
        return this;
    }

    /**
     * Sets the drawable in hovered state.
     *
     * @param hoveredDrawable The drawable in hovered state
     *
     * @return The class
     */
    public StateListDrawableBuilder setHoveredDrawable(Drawable hoveredDrawable) {
        mHoveredDrawable = hoveredDrawable;
        return this;
    }

    /**
     * Sets the drawable in disabled state.
     *
     * @param disabledDrawable The drawable in disabled state
     *
     * @return The class
     */
    public StateListDrawableBuilder setDisabledDrawable(Drawable disabledDrawable) {
        mDisabledDrawable = disabledDrawable;
        return this;
    }

    /**
     * Builds a StateListDrawable using defined drawables in different states.
     *
     * @return The {@link StateListDrawable}
     */
    public StateListDrawable build() {
        StateListDrawable stateListDrawable = new StateListDrawable();
        if (mSelectedDrawable != null) {
            stateListDrawable.addState(STATE_SELECTED, mSelectedDrawable);
        }

        if (mPressedDrawable != null) {
            stateListDrawable.addState(STATE_PRESSED, mPressedDrawable);
        }

        if (mFocusedDrawable != null) {
            stateListDrawable.addState(STATE_FOCUSED, mFocusedDrawable);
        }

        if (mHoveredDrawable != null) {
            stateListDrawable.addState(STATE_HOVERED, mHoveredDrawable);
        }

        if (mNormalDrawable != null) {
            stateListDrawable.addState(STATE_ENABLED, mNormalDrawable);
        }

        if (mDisabledDrawable != null) {
            stateListDrawable.addState(STATE_DISABLED, mDisabledDrawable);
        }

        return stateListDrawable;
    }
}
