//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;

// ref: https://stackoverflow.com/a/16103514/8033906

/**
 * {@link Toast} decorator allowing for easy cancellation of notifications. Use this class if you
 * want subsequent Toast notifications to overwrite current ones. </p>
 * <p/>
 * By default, a current {@link CommonToast} notification will be cancelled by a subsequent notification.
 * This default behaviour can be changed by calling certain methods like {@link #show(boolean)}.
 */
public class CommonToast {
    /**
     * Keeps track of certain CommonToast notifications that may need to be cancelled. This functionality
     * is only offered by some of the methods in this class.
     * <p>
     * Uses a {@link WeakReference} to avoid leaking the activity context used to show the original {@link Toast}.
     */
    @Nullable
    private volatile static WeakReference<CommonToast> weakCommonToast = null;

    @Nullable
    private static CommonToast getGlobalCommonToast() {
        if (weakCommonToast == null) {
            return null;
        }

        return weakCommonToast.get();
    }

    private static void setGlobalCommonToast(@Nullable CommonToast globalCommonToast) {
        CommonToast.weakCommonToast = new WeakReference<>(globalCommonToast);
    }


    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Internal reference to the {@link Toast} object that will be displayed.
     */
    private Toast internalToast;

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Private constructor creates a new {@link CommonToast} from a given {@link Toast}.
     *
     * @throws NullPointerException if the parameter is <code>null</code>.
     */
    private CommonToast(Toast toast) {
        // null check
        if (toast == null) {
            throw new NullPointerException("CommonToast.CommonToast(Toast) requires a non-null parameter.");
        }

        internalToast = toast;
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Make a standard {@link CommonToast} that just contains a text view.
     *
     * @param context  The context to use. Usually your {@link android.app.Application} or
     *                 {@link android.app.Activity} object.
     * @param text     The text to show. Can be formatted text.
     * @param duration How long to display the message. Either {@link Toast#LENGTH_SHORT} or
     *                 {@link Toast#LENGTH_LONG}
     */
    @SuppressLint("ShowToast")
    private static CommonToast makeText(Context context, CharSequence text, int duration) {
        return new CommonToast(Toast.makeText(context, text, duration));
    }

    /**
     * Make a standard {@link CommonToast} that just contains a text view with the text from a resource.
     *
     * @param context  The context to use. Usually your {@link android.app.Application} or
     *                 {@link android.app.Activity} object.
     * @param resId    The resource id of the string resource to use. Can be formatted text.
     * @param duration How long to display the message. Either {@link Toast#LENGTH_SHORT} or
     *                 {@link Toast#LENGTH_LONG}
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    @SuppressLint("ShowToast")
    private static CommonToast makeText(Context context, int resId, int duration)
        throws Resources.NotFoundException {
        return new CommonToast(Toast.makeText(context, resId, duration));
    }

    /**
     * Make a standard {@link CommonToast} that just contains a text view. Duration defaults to
     * {@link Toast#LENGTH_SHORT}.
     *
     * @param context The context to use. Usually your {@link android.app.Application} or
     *                {@link android.app.Activity} object.
     * @param text    The text to show. Can be formatted text.
     */
    @SuppressWarnings("unused")
    @SuppressLint("ShowToast")
    private static CommonToast makeText(Context context, CharSequence text) {
        return new CommonToast(Toast.makeText(context, text, Toast.LENGTH_SHORT));
    }

    /**
     * Make a standard {@link CommonToast} that just contains a text view with the text from a resource.
     * Duration defaults to {@link Toast#LENGTH_SHORT}.
     *
     * @param context The context to use. Usually your {@link android.app.Application} or
     *                {@link android.app.Activity} object.
     * @param resId   The resource id of the string resource to use. Can be formatted text.
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    @SuppressWarnings("unused")
    @SuppressLint("ShowToast")
    private static CommonToast makeText(Context context, int resId) throws Resources.NotFoundException {
        return new CommonToast(Toast.makeText(context, resId, Toast.LENGTH_SHORT));
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Show a standard {@link CommonToast} that just contains a text view.
     *
     * @param context  The context to use. Usually your {@link android.app.Application} or
     *                 {@link android.app.Activity} object.
     * @param text     The text to show. Can be formatted text.
     * @param duration How long to display the message. Either {@link Toast#LENGTH_SHORT} or
     *                 {@link Toast#LENGTH_LONG}
     */
    public static void showText(
        @Nullable final Context context,
        final CharSequence text,
        final int duration) {

        if (!validContext(context)) {
            return;
        }

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (validContext(context)) {
                        CommonToast.makeText(context, text, duration).show();
                    }
                }
            });
        } else {
            CommonToast.makeText(context, text, duration).show();
        }

    }

    /**
     * Show a standard {@link CommonToast} that just contains a text view with the text from a resource.
     *
     * @param context  The context to use. Usually your {@link android.app.Application} or
     *                 {@link android.app.Activity} object.
     * @param resId    The resource id of the string resource to use. Can be formatted text.
     * @param duration How long to display the message. Either {@link Toast#LENGTH_SHORT} or
     *                 {@link Toast#LENGTH_LONG}
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    public static void showText(
        @Nullable final Context context,
        final int resId,
        final int duration)
        throws Resources.NotFoundException {

        if (!validContext(context)) {
            return;
        }

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (validContext(context)) {
                        CommonToast.makeText(context, resId, duration).show();
                    }
                }
            });
        } else {
            CommonToast.makeText(context, resId, duration).show();
        }

    }

    /**
     * Show a standard {@link CommonToast} that just contains a text view. Duration defaults to
     * {@link Toast#LENGTH_SHORT}.
     *
     * @param context The context to use. Usually your {@link android.app.Application} or
     *                {@link android.app.Activity} object.
     * @param text    The text to show. Can be formatted text.
     */
    public static void showText(
        @Nullable final Context context,
        final CharSequence text) {

        if (!validContext(context)) {
            return;
        }

        if (validContext(context)) {
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (validContext(context)) {
                            CommonToast.makeText(context, text, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                CommonToast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        }

    }

    /**
     * Show a standard {@link CommonToast} that just contains a text view with the text from a resource.
     * Duration defaults to {@link Toast#LENGTH_SHORT}.
     *
     * @param context The context to use. Usually your {@link android.app.Application} or
     *                {@link android.app.Activity} object.
     * @param resId   The resource id of the string resource to use. Can be formatted text.
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    public static void showText(
        @Nullable final Context context,
        final int resId) throws Resources.NotFoundException {

        if (!validContext(context)) {
            return;
        }

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (validContext(context)) {
                        CommonToast.makeText(context, resId, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            CommonToast.makeText(context, resId, Toast.LENGTH_SHORT).show();
        }

    }

    public static void showText(
        @Nullable final Context context,
        final CharSequence text,
        final int duration,
        final int gravity,
        final int xOffset,
        final int yOffset) {

        if (!validContext(context)) {
            return;
        }

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (validContext(context)) {
                        CommonToast toast = CommonToast.makeText(context, text, duration);
                        toast.internalToast.setGravity(gravity, xOffset, yOffset);
                        toast.show();
                    }
                }
            });
        } else {
            CommonToast toast = CommonToast.makeText(context, text, duration);
            toast.internalToast.setGravity(gravity, xOffset, yOffset);
            toast.show();
        }

    }

    public static void showText(
        @Nullable final Context context,
        final int resId,
        final int duration,
        final int gravity,
        final int xOffset,
        final int yOffset)
        throws Resources.NotFoundException {

        if (!validContext(context)) {
            return;
        }

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (validContext(context)) {
                        CommonToast toast = CommonToast.makeText(context, resId, duration);
                        toast.internalToast.setGravity(gravity, xOffset, yOffset);
                        toast.show();
                    }
                }
            });
        } else {
            CommonToast toast = CommonToast.makeText(context, resId, duration);
            toast.internalToast.setGravity(gravity, xOffset, yOffset);
            toast.show();
        }

    }

    private static boolean validContext(@Nullable Context context) {
        return context != null
            && (!(context instanceof Activity) || !((Activity) context).isFinishing());
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Close the view if it's showing, or don't show it if it isn't showing yet. You do not normally
     * have to call this. Normally view will disappear on its own after the appropriate duration.
     */
    public void cancel() {
        internalToast.cancel();
    }

    /**
     * Show the view for the specified duration. By default, this method cancels any current
     * notification to immediately display the new one. For conventional {@link Toast#show()}
     * queueing behaviour, use method {@link #show(boolean)}.
     *
     * @see #show(boolean)
     */
    public void show() {
        show(true);
    }

    /**
     * Show the view for the specified duration. This method can be used to cancel the current
     * notification, or to queue up notifications.
     *
     * @param cancelCurrent <code>true</code> to cancel any current notification and replace it with this new
     *                      one
     * @see #show()
     */
    public void show(boolean cancelCurrent) {
        // cancel current
        if (cancelCurrent) {
            final CommonToast cachedGlobalCommonToast = getGlobalCommonToast();
            if ((cachedGlobalCommonToast != null)) {
                cachedGlobalCommonToast.cancel();
            }
        }

        // save an instance of this current notification
        setGlobalCommonToast(this);

        if (Utils.isJellyBeanMR1()) {
            if (Utils.isRtlLayout(internalToast.getView().getContext())) {
                internalToast.getView().setTextDirection(View.TEXT_DIRECTION_RTL);
            } else {
                internalToast.getView().setTextDirection(View.TEXT_DIRECTION_LTR);
            }
        }

        internalToast.show();
    }
}
