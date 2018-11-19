//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * The option for creating a standard rubber stamp.
 */
public class StandardStampOption extends CustomStampOption {

    private final static String FILE_STANDARD_STAMP_INFO = "com_pdftron_pdf_model_file_standard_stamp";

    public StandardStampOption(@NonNull String text, @Nullable String secondText, int bgColorStart, int bgColorEnd, int textColor, int borderColor, double fillOpacity, boolean isPointingLeft, boolean isPointingRight) {
        super(text, secondText, bgColorStart, bgColorEnd, textColor, borderColor, fillOpacity, isPointingLeft, isPointingRight);
    }

    /**
     * Checks whether a certain standard stamp exists on disk and it is valid
     *
     * @param context The context
     * @param name    The name of standard rubber stamp
     * @return True if the standard stamp exists on disk and it is valid; False otherwise
     */
    synchronized public static boolean checkStandardStamp(@Nullable Context context, @NonNull String name) {
        if (context == null) {
            return false;
        }
        String path = getStandardStampBitmapPath(context, name);
        return !Utils.isNullOrEmpty(path) && getStandardStampObj(context, name) != null;
    }


    /**
     * Gets the bitmap of a standard rubber stamp.
     *
     * @param context The context
     * @param name    The name of standard rubber stamp
     * @return The bitmap of custom rubber stamp, or null if it could not be obtained
     */
    @Nullable
    synchronized public static Bitmap getStandardStampBitmap(@Nullable Context context, @NonNull String name) {
        if (context == null) {
            return null;
        }
        String path = getStandardStampBitmapPath(context, name);
        return BitmapFactory.decodeFile(path);
    }

    /**
     * Gets the saved custom rubber stamp at a certain position.
     *
     * @param context The context
     * @param name    The name of standard rubber stamp
     * @return The saved custom rubber stamp as SDF Obj
     */
    @Nullable
    synchronized public static Obj getStandardStampObj(@NonNull Context context, @NonNull String name) {
        if (Utils.isNullOrEmpty(name)) {
            return null;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(getStandardStampInfoPath(context, name));
            String info = IOUtils.toString(fis);
            JSONObject jsonObject = new JSONObject(info);
            CustomStampOption customStampOption = new CustomStampOption(jsonObject);
            return convertToObj(customStampOption);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(fis);
        }

        return null;
    }

    /**
     * Saves a new custom rubber stamp.
     *
     * @param context     The context
     * @param stampOption The new custom rubber stamp
     * @param bitmap      The generated bitmap which can be reloaded later to avoid recreating the bitmap
     */
    synchronized public static void saveStandardStamp(@Nullable Context context, @NonNull String name, @NonNull CustomStampOption stampOption, @NonNull Bitmap bitmap) {
        if (context == null || Utils.isNullOrEmpty(name)) {
            return;
        }

        // save bitmap for fast read access
        FileOutputStream fos = null;
        try {
            String path = getStandardStampBitmapPath(context, name);
            fos = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(fos);
        }

        saveStandardStamp(context, name, stampOption);
    }

    private static void saveStandardStamp(@NonNull Context context, @NonNull String name, @NonNull CustomStampOption stampOption) {
        Gson gson = new Gson();
        Type collectionType = new TypeToken<CustomStampOption>() {
        }.getType();
        String serializedData = gson.toJson(stampOption, collectionType);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(getStandardStampInfoPath(context, name));
            IOUtils.write(serializedData, fos);
        } catch (IOException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(fos);
        }
    }

    private static String getStandardStampBitmapPath(@NonNull Context context, @NonNull String name) {
        return context.getFilesDir().getAbsolutePath() + File.separator + "standard_stamp_bitmap_" + name + ".png";
    }

    private static String getStandardStampInfoPath(@NonNull Context context, @NonNull String name) {
        return context.getFilesDir().getAbsolutePath() + File.separator + FILE_STANDARD_STAMP_INFO + name;
    }

}
