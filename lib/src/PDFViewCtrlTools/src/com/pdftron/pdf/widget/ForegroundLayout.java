package com.pdftron.pdf.widget;

import android.graphics.drawable.Drawable;

/**
 * Interface to facilitate drawing a foreground drawable, similar to {@link android.widget.FrameLayout},
 * for API versions before 23 (where {@link android.view.View} supports foregrounds natively).
 */
public interface ForegroundLayout {
    /**
     * The implementation should provide the foreground drawable.
     *
     * @return The foreground drawable
     */
    Drawable getForeground();

    /**
     * The implementation should set the foreground drawable.
     *
     * @param foreground The foreground drawable
     */
    void setForeground(Drawable foreground);

    /**
     * The implementation should specify the foreground gravity.
     *
     * @return The gravity (see {@link android.view.Gravity})
     */
    int getForegroundGravity();

    /**
     * The implementation should set the foreground gravity.
     *
     * @param gravity The gravity (see {@link android.view.Gravity})
     */
    void setForegroundGravity(int gravity);
}
