//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.asynctask;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFDraw;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.RubberStamp;
import com.pdftron.pdf.model.CustomStampOption;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;

/**
 * A class that asynchronously generates a bitmap from a certain custom rubber stamp.
 */
public class CreateBitmapFromCustomStampTask extends CustomAsyncTask<Void, Void, Bitmap> {

    private CustomStampOption mCustomStampOption;
    private int mSingleLineHeight, mTwoLinesHeight;
    private OnCustomStampCreatedCallback mOnCustomStampCreatedCallback;

    /**
     * Class constructor for creating a bitmap from a custom rubber stamp.
     *
     * @param context           The context
     * @param customStampOption A custom rubber stamp option
     */
    public CreateBitmapFromCustomStampTask(@NonNull Context context, @NonNull CustomStampOption customStampOption) {
        super(context);
        mCustomStampOption = customStampOption;
        mSingleLineHeight = context.getResources().getDimensionPixelSize(R.dimen.stamp_image_height);
        mTwoLinesHeight = context.getResources().getDimensionPixelSize(R.dimen.stamp_image_height_two_lines);
    }

    /**
     * Sets the callback for when the bitmap of a custom rubber stamp is created
     * <p>
     * Sets the callback to null when the task is cancelled.
     *
     * @param callback The callback when the task is finished
     */
    public void setOnCustomStampCreatedCallback(@Nullable OnCustomStampCreatedCallback callback) {
        mOnCustomStampCreatedCallback = callback;
    }

    @Override
    protected Bitmap doInBackground(Void... voids) {
        try {
            return createBitmapFromCustomStamp(mCustomStampOption, mSingleLineHeight, mTwoLinesHeight);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        if (mOnCustomStampCreatedCallback != null) {
            mOnCustomStampCreatedCallback.onCustomStampCreated(bitmap);
        }
    }

    /**
     * Creates a bitmap from a specific custom rubber stamp.
     * <p>
     * This function maintains aspect ratio. See {@link #createBitmapFromCustomStamp(CustomStampOption, int, int, int)}.
     *
     * @param customStampOption A custom rubber stamp option
     * @param singleLineHeight  The height of resulting bitmap if there is no second text in stamp
     * @param twoLinesHeight    The height of resulting bitmap if stamp has a second text line
     * @return The bitmap of a stamp
     */
    public static Bitmap createBitmapFromCustomStamp(@NonNull CustomStampOption customStampOption, int singleLineHeight, int twoLinesHeight) {
        return createBitmapFromCustomStamp(customStampOption, singleLineHeight, twoLinesHeight, -1);
    }

    /**
     * Creates a bitmap from a specific custom rubber stamp.
     *
     * @param customStampOption A custom rubber stamp option
     * @param singleLineHeight  The height of resulting bitmap if there is no second text in stamp
     * @param twoLinesHeight    The height of resulting bitmap if stamp has a second text line
     * @param width             The width of resulting bitmap;
     *                          -1 if it should be obtained automatically to maintain the aspect ratio
     * @return The bitmap of a stamp
     */
    public static Bitmap createBitmapFromCustomStamp(@NonNull CustomStampOption customStampOption, int singleLineHeight, int twoLinesHeight, int width) {
        CustomStampOption stampOption = new CustomStampOption(customStampOption);
        stampOption.fillOpacity = 1; // we want no transparency for generated bitmaps

        PDFDoc tempDoc = null;
        PDFDraw pdfDraw = null;
        boolean shouldUnlock = false;
        try {
            tempDoc = new PDFDoc();
            tempDoc.lock();
            shouldUnlock = true;
            tempDoc.initSecurityHandler();
            Page page = tempDoc.pageCreate();

            Rect rect = new Rect();
            Obj obj = CustomStampOption.convertToObj(stampOption);
            RubberStamp rubberStamp = RubberStamp.createCustom(tempDoc, rect, obj);

            page.annotPushBack(rubberStamp);
            page.setCropBox(rubberStamp.getRect());

            pdfDraw = new PDFDraw();
            pdfDraw.setPageTransparent(true);

            int height = Utils.isNullOrEmpty(stampOption.secondText) ? singleLineHeight : twoLinesHeight;
            if (width == -1) {
                double ratio = height / page.getPageHeight();
                width = (int) (page.getPageWidth() * ratio + .5);
            }
            pdfDraw.setImageSize(width, height, false);

            return pdfDraw.getBitmap(page);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(tempDoc);
            }
            Utils.closeQuietly(tempDoc);
            if (pdfDraw != null) {
                try {
                    pdfDraw.destroy();
                } catch (PDFNetException ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Callback interface for when the bitmap of a custom rubber stamp is created.
     */
    public interface OnCustomStampCreatedCallback {
        /**
         * Called when the bitmap of a custom rubber stamp is generated.
         *
         * @param bitmap The bitmap of the custom rubber stamp
         */
        void onCustomStampCreated(@Nullable Bitmap bitmap);
    }

}
