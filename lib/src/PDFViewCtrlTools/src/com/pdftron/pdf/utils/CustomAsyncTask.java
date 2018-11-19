//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * A version of {@link AsyncTask} class that keeps a weak reference to the context, so
 * the context can be accessed (if still alive).
 */
public abstract class CustomAsyncTask<Params, Progress, Result>
        extends AsyncTask<Params, Progress, Result> {

    private WeakReference<Context> mContext;

    public CustomAsyncTask(Context context) {
        mContext = new WeakReference<>(context);
    }

    /**
     * @return The context if still alive
     */
    @Nullable
    protected Context getContext() {
        Context context = mContext.get();
        if (context instanceof Activity && ((Activity) context).isFinishing()) {
            context = null;
        }
        return context;
    }
}
