//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;
import com.pdftron.sdf.ObjSet;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The option for creating a custom rubber stamp.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class CustomStampOption {

    private static final String TAG = CustomStampOption.class.getName();

    private final static String FILE_CUSTOM_STAMPS_INFO = "com_pdftron_pdf_model_file_custom_stamps";

    private static final String STAMP_OPTION_TEXT = "TEXT";
    private static final String STAMP_OPTION_TEXT_BELOW = "TEXT_BELOW";
    private static final String STAMP_OPTION_FILL_COLOR_START = "FILL_COLOR_START";
    private static final String STAMP_OPTION_FILL_COLOR_END = "FILL_COLOR_END";
    private static final String STAMP_OPTION_TEXT_COLOR = "TEXT_COLOR";
    private static final String STAMP_OPTION_BORDER_COLOR = "BORDER_COLOR";
    private static final String STAMP_OPTION_FILL_OPACITY = "FILL_OPACITY";
    private static final String STAMP_OPTION_POINTING_LEFT = "POINTING_LEFT";
    private static final String STAMP_OPTION_POINTING_RIGHT = "POINTING_RIGHT";

    private static final String VAR_TEXT = "text";
    private static final String VAR_SECOND_TEXT = "secondText";
    private static final String VAR_BG_COLOR_START = "bgColorStart";
    private static final String VAR_BG_COLOR_END = "bgColorEnd";
    private static final String VAR_TEXT_COLOR = "textColor";
    private static final String VAR_BORDER_COLOR = "borderColor";
    private static final String VAR_FILL_OPACITY = "fillOpacity";
    private static final String VAR_POINTING_LEFT = "isPointingLeft";
    private static final String VAR_POINTING_RIGHT = "isPointingRight";

    public String text = "", secondText;
    @ColorInt
    public int bgColorStart, bgColorEnd, textColor, borderColor;
    @FloatRange(from = 0, to = 1.0)
    public double fillOpacity;
    public boolean isPointingLeft, isPointingRight;

    /**
     * Class constructor
     *
     * @param text            The text to be displayed in the rubber stamp
     * @param secondText      The text to be displayed below first text
     * @param bgColorStart    The start background color in gradient background
     * @param bgColorEnd      The end background color in gradient background
     * @param textColor       The text color
     * @param borderColor     The border color
     * @param fillOpacity     The fill opacity
     * @param isPointingLeft  True if pointing left
     * @param isPointingRight True if pointing right
     */
    public CustomStampOption(@NonNull String text, @Nullable String secondText, @ColorInt int bgColorStart, @ColorInt int bgColorEnd, @ColorInt int textColor, @ColorInt int borderColor, @FloatRange(from = 0, to = 1.0) double fillOpacity, boolean isPointingLeft, boolean isPointingRight) {
        this.text = text;
        this.secondText = secondText;
        this.bgColorStart = bgColorStart;
        this.bgColorEnd = bgColorEnd;
        this.textColor = textColor;
        this.borderColor = borderColor;
        this.fillOpacity = fillOpacity;
        this.isPointingLeft = isPointingLeft;
        this.isPointingRight = isPointingRight;
    }

    /**
     * Class constructor
     *
     * @param another Another custom rubber stamp option object
     */
    public CustomStampOption(@NonNull CustomStampOption another) {
        this.text = another.text;
        this.secondText = another.secondText;
        this.bgColorStart = another.bgColorStart;
        this.bgColorEnd = another.bgColorEnd;
        this.textColor = another.textColor;
        this.borderColor = another.borderColor;
        this.fillOpacity = another.fillOpacity;
        this.isPointingLeft = another.isPointingLeft;
        this.isPointingRight = another.isPointingRight;
    }

    /**
     * Class constructor
     *
     * @param stampObj The stamp SDF Obj
     * @throws PDFNetException PDFNet exception
     */
    public CustomStampOption(@NonNull Obj stampObj) throws PDFNetException {
        Obj found = stampObj.findObj(STAMP_OPTION_TEXT);
        if (found == null || !found.isString()) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            throw new PDFNetException("", 0, TAG, name, STAMP_OPTION_TEXT + " is mandatory in custom rubber stamp's SDF Obj");
        }
        text = found.getAsPDFText();
        found = stampObj.findObj(STAMP_OPTION_TEXT_BELOW);
        if (found != null && found.isString()) {
            secondText = found.getAsPDFText();
        }
        found = stampObj.findObj(STAMP_OPTION_FILL_COLOR_START);
        if (found != null && found.isArray() && (found.size() == 3 || found.size() == 4)) {
            bgColorStart = getColorFromOption(found);
        }
        found = stampObj.findObj(STAMP_OPTION_FILL_COLOR_END);
        if (found != null && found.isArray() && (found.size() == 3 || found.size() == 4)) {
            bgColorEnd = getColorFromOption(found);
        }
        found = stampObj.findObj(STAMP_OPTION_TEXT_COLOR);
        if (found != null && found.isArray() && (found.size() == 3 || found.size() == 4)) {
            textColor = getColorFromOption(found);
        }
        found = stampObj.findObj(STAMP_OPTION_BORDER_COLOR);
        if (found != null && found.isArray() && (found.size() == 3 || found.size() == 4)) {
            borderColor = getColorFromOption(found);
        }
        found = stampObj.findObj(STAMP_OPTION_FILL_OPACITY);
        if (found != null && found.isNumber()) {
            fillOpacity = found.getNumber();
        }
        found = stampObj.findObj(STAMP_OPTION_POINTING_LEFT);
        if (found != null && found.isBool()) {
            isPointingLeft = found.getBool();
        }
        found = stampObj.findObj(STAMP_OPTION_POINTING_RIGHT);
        if (found != null && found.isBool()) {
            isPointingRight = found.getBool();
        }
    }

    protected CustomStampOption(JSONObject jsonObject) throws PDFNetException {
        if (!jsonObject.has(VAR_TEXT)) {
            String name = new Object() {
            }.getClass().getEnclosingMethod().getName();
            throw new PDFNetException("", 0, TAG, name, STAMP_OPTION_TEXT + " is mandatory in custom rubber stamp's SDF Obj");
        }

        try {
            text = jsonObject.getString(VAR_TEXT);
            if (jsonObject.has(VAR_SECOND_TEXT)) {
                secondText = jsonObject.getString(VAR_SECOND_TEXT);
            }
            if (jsonObject.has(VAR_BG_COLOR_START)) {
                bgColorStart = jsonObject.getInt(VAR_BG_COLOR_START);
            }
            if (jsonObject.has(VAR_BG_COLOR_END)) {
                bgColorEnd = jsonObject.getInt(VAR_BG_COLOR_END);
            }
            if (jsonObject.has(VAR_TEXT_COLOR)) {
                textColor = jsonObject.getInt(VAR_TEXT_COLOR);
            }
            if (jsonObject.has(VAR_BORDER_COLOR)) {
                borderColor = jsonObject.getInt(VAR_BORDER_COLOR);
            }
            if (jsonObject.has(VAR_FILL_OPACITY)) {
                fillOpacity = jsonObject.getDouble(VAR_FILL_OPACITY);
            }
            if (jsonObject.has(VAR_POINTING_LEFT)) {
                isPointingLeft = jsonObject.getBoolean(VAR_POINTING_LEFT);
            }
            if (jsonObject.has(VAR_POINTING_RIGHT)) {
                isPointingRight = jsonObject.getBoolean(VAR_POINTING_RIGHT);
            }
        } catch (JSONException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * @return True if the custom rubber stamp has time
     */
    public boolean hasTimeStamp() {
        // example: 6:00 PM
        return !Utils.isNullOrEmpty(secondText) && secondText.contains(":");
    }

    /**
     * @return True if the custom rubber stamp has date
     */
    public boolean hasDateStamp() {
        // example: Jul 31, 2018
        return !Utils.isNullOrEmpty(secondText) && secondText.contains(",");
    }

    /**
     * Creates the second text based on whether the custom rubber stamp has date/time
     *
     * @param hasDate True if the custom rubber stamp has date; False otherwise
     * @param hasTime True if the custom rubber stamp has date; False otherwise
     * @return The second text
     */
    @Nullable
    public static String createSecondText(boolean hasDate, boolean hasTime) {
        String secondText = null;
        Date currentTime = Calendar.getInstance().getTime();
        if (hasDate && hasTime) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.US);
            secondText = dateFormat.format(currentTime);
        } else if (hasDate) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            secondText = dateFormat.format(currentTime);
        } else if (hasTime) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a", Locale.US);
            secondText = dateFormat.format(currentTime);
        }
        return secondText;
    }

    /**
     * Updates the second line text since date/time could have be changed.
     */
    @SuppressWarnings("WeakerAccess")
    public void updateDateTime() {
        secondText = createSecondText(hasDateStamp(), hasTimeStamp());
    }

    /**
     * Loads all saved custom rubber stamps.
     *
     * @param context The context
     * @return The saved custom rubber stamps as SDF Obj
     */
    @SuppressWarnings("unused")
    @NonNull
    synchronized public static List<Obj> loadCustomStamps(@NonNull Context context) {
        List<Obj> customStamps = new ArrayList<>();
        List<CustomStampOption> customStampOptions = loadCustomStampOptions(context);
        for (int i = 0, count = customStampOptions.size(); i < count; ++i) {
            try {
                Obj obj = convertToObj(customStampOptions.get(i));
                customStamps.add(obj);
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        return customStamps;
    }

    /**
     * Gets the number of saved custom rubber stamps.
     *
     * @param context The context
     * @return The number of saved custom rubber stamps
     */
    synchronized public static int getCustomStampsCount(@NonNull Context context) {
        String customStampsInfo = getCustomStampsInfo(context);
        if (Utils.isNullOrEmpty(customStampsInfo)) {
            return 0;
        }

        try {
            JSONArray jsonArray = new JSONArray(customStampsInfo);
            return jsonArray.length();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        return 0;
    }

    /**
     * Gets the saved custom rubber stamp at a certain position.
     *
     * @param context The context
     * @param index   The zero-indexed position of the saved custom rubber stamp
     * @return The saved custom rubber stamp as SDF Obj
     */
    @Nullable
    synchronized public static Obj getCustomStampObj(@NonNull Context context, int index) {
        String customStampsInfo = getCustomStampsInfo(context);
        if (Utils.isNullOrEmpty(customStampsInfo)) {
            return null;
        }

        try {
            JSONArray jsonArray = new JSONArray(customStampsInfo);
            JSONObject jsonObject = jsonArray.getJSONObject(index);
            CustomStampOption customStampOption = new CustomStampOption(jsonObject);
            return convertToObj(customStampOption);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        return null;
    }

    /**
     * Gets the custom rubber stamp bitmap for a specific index.
     *
     * @param context The context
     * @param index   The zero-indexed position of the saved custom rubber stamp
     * @return The bitmap of custom rubber stamp, or null if it could not be obtained
     */
    synchronized public static Bitmap getCustomStampBitmap(@NonNull Context context, int index) {
        String path = getCustomStampBitmapPath(context, index);
        return BitmapFactory.decodeFile(path);
    }

    /**
     * Saves a new custom rubber stamp.
     *
     * @param context           The context
     * @param customStampOption The new custom rubber stamp
     * @param bitmap            The generated bitmap which can be reloaded later to avoid recreating the bitmap
     */
    synchronized public static void addCustomStamp(@Nullable Context context, @NonNull CustomStampOption customStampOption, @NonNull Bitmap bitmap) {
        if (context == null) {
            return;
        }

        List<CustomStampOption> customStampOptions = loadCustomStampOptions(context);

        // save bitmap for fast read access
        FileOutputStream fos = null;
        try {
            String path = getCustomStampBitmapPath(context, customStampOptions.size());
            fos = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(fos);
        }

        customStampOptions.add(customStampOption);
        saveCustomStamps(context, customStampOptions);
    }

    /**
     * Duplicates a certain custom rubber stamp right after the current position.
     *
     * @param context The context
     * @param index   The zero-indexed position
     */
    synchronized public static void duplicateCustomStamp(@Nullable Context context, int index) {
        if (context == null) {
            return;
        }

        List<CustomStampOption> customStampOptions = loadCustomStampOptions(context);
        int size = customStampOptions.size();
        if (index < 0 || index >= size) {
            return;
        }

        for (int i = size - 1; i >= index; --i) {
            String oldPath = getCustomStampBitmapPath(context, i);
            String newPath = getCustomStampBitmapPath(context, i + 1);
            File oldFile = new File(oldPath);
            File newFile = new File(newPath);
            if (oldFile.exists()) {
                if (i == index) {
                    try {
                        Utils.copy(oldFile, newFile);
                    } catch (IOException e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    }
                } else {
                    oldFile.renameTo(newFile);
                }
            }
        }

        CustomStampOption customStampOption = customStampOptions.get(index);
        customStampOptions.add(index, customStampOption);
        saveCustomStamps(context, customStampOptions);
    }

    /**
     * Updates an existing saved custom rubber stamp.
     *
     * @param context           The context
     * @param index             The zero-indexed position
     * @param customStampOption The updated custom rubber stamp
     * @param bitmap            The generated bitmap which can be reloaded later to avoid recreating the bitmap
     */
    synchronized public static void updateCustomStamp(@Nullable Context context, int index, @NonNull CustomStampOption customStampOption, @NonNull Bitmap bitmap) {
        if (context == null) {
            return;
        }

        List<CustomStampOption> customStampOptions = loadCustomStampOptions(context);

        // save bitmap for fast read access
        FileOutputStream fos = null;
        try {
            String path = getCustomStampBitmapPath(context, index);
            fos = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(fos);
        }

        customStampOptions.set(index, customStampOption);
        saveCustomStamps(context, customStampOptions);
    }

    /**
     * Removes a specific custom rubber stamp from saved custom rubber stamps.
     *
     * @param context The context
     * @param index   The index of custom rubber stamp to be removed
     */
    @SuppressWarnings("unused")
    synchronized public static void removeCustomStamp(@Nullable Context context, int index) {
        if (context == null) {
            return;
        }

        List<CustomStampOption> customStampOptions = loadCustomStampOptions(context);
        int size = customStampOptions.size();
        if (index < 0 || index >= size) {
            return;
        }

        for (int i = index + 1; i < size; ++i) {
            String oldPath = getCustomStampBitmapPath(context, i);
            String newPath = getCustomStampBitmapPath(context, i - 1);
            File oldFile = new File(oldPath);
            File newFile = new File(newPath);
            if (oldFile.exists()) {
                oldFile.renameTo(newFile);
            }
        }

        customStampOptions.remove(index);
        saveCustomStamps(context, customStampOptions);
    }

    /**
     * Removes certain custom rubber stamps from saved custom rubber stamps.
     *
     * @param context The context
     * @param indexes The indexes of custom rubber stamps to be removed.
     *                The indexes shouldn't contain repeated items and are
     *                supposed to be in ascending order.
     */
    synchronized public static void removeCustomStamps(@Nullable Context context, @NonNull List<Integer> indexes) {
        if (context == null || indexes.size() == 0) {
            return;
        }

        List<CustomStampOption> customStampOptions = loadCustomStampOptions(context);
        int size = customStampOptions.size();

        for (int j = indexes.size() - 1; j >= 0; --j) {
            int index = indexes.get(j);
            if (index < 0 || index >= size) {
                continue;
            }

            for (int i = index + 1; i < size; ++i) {
                String oldPath = getCustomStampBitmapPath(context, i);
                String newPath = getCustomStampBitmapPath(context, i - 1);
                File oldFile = new File(oldPath);
                File newFile = new File(newPath);
                if (oldFile.exists()) {
                    oldFile.renameTo(newFile);
                }
            }

            customStampOptions.remove(index);
        }

        saveCustomStamps(context, customStampOptions);
    }

    /**
     * Removes all saved custom rubber stamps.
     *
     * @param context The context
     */
    @SuppressWarnings("unused")
    synchronized public static void removeAllCustomStamps(@Nullable Context context) {
        if (context == null) {
            return;
        }

        List<CustomStampOption> customStampOptions = loadCustomStampOptions(context);
        int size = customStampOptions.size();

        for (int i = 0; i < size; ++i) {
            String path = getCustomStampBitmapPath(context, i);
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }

        saveCustomStampsInfo(context, "");
    }

    /**
     * Moves a specific custom rubber stamp to a new position.
     *
     * @param context      The context
     * @param fromPosition The index of custom rubber stamp to be moved
     * @param toPosition   The new index of custom rubber stamp
     */
    synchronized public static void moveCustomStamp(@Nullable Context context, int fromPosition, int toPosition) {
        if (context == null) {
            return;
        }

        List<CustomStampOption> customStampOptions = loadCustomStampOptions(context);
        int size = customStampOptions.size();
        if (fromPosition < 0 || fromPosition == toPosition
            || fromPosition >= size || toPosition < 0 || toPosition >= size) {
            return;
        }

        String path = getCustomStampBitmapPath(context, fromPosition);
        File file = new File(path);
        File tempFile = null;
        if (file.exists()) {
            tempFile = new File(getCustomStampBitmapPath(context, "temp"));
            file.renameTo(tempFile);
        }

        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; ++i) {
                String oldPath = getCustomStampBitmapPath(context, i + 1);
                String newPath = getCustomStampBitmapPath(context, i);
                File oldFile = new File(oldPath);
                File newFile = new File(newPath);
                if (oldFile.exists()) {
                    oldFile.renameTo(newFile);
                } else if (newFile.exists()) {
                    newFile.delete();
                }
            }
        } else {
            for (int i = fromPosition; i > toPosition; --i) {
                String oldPath = getCustomStampBitmapPath(context, i - 1);
                String newPath = getCustomStampBitmapPath(context, i);
                File oldFile = new File(oldPath);
                File newFile = new File(newPath);
                if (oldFile.exists()) {
                    oldFile.renameTo(newFile);
                } else if (newFile.exists()) {
                    newFile.delete();
                }
            }
        }
        path = getCustomStampBitmapPath(context, toPosition);
        file = new File(path);
        if (tempFile != null && tempFile.exists()) {
            tempFile.renameTo(file);
        } else if (file.exists()) {
            file.delete();
        }

        CustomStampOption item = customStampOptions.get(fromPosition);
        customStampOptions.remove(fromPosition);
        customStampOptions.add(toPosition, item);

        saveCustomStamps(context, customStampOptions);
    }

    /**
     * Creates an SDF obj option from a certain custom rubber stamp
     *
     * @param customStampOption The custom rubber stamp option
     * @return The stamp's SDF Obj
     * @throws PDFNetException PDFNet exception
     */
    public static Obj convertToObj(@NonNull CustomStampOption customStampOption) throws PDFNetException {
        customStampOption.updateDateTime();
        ObjSet objSet = new ObjSet();
        Obj stampObj = objSet.createDict();
        stampObj.putText(STAMP_OPTION_TEXT, customStampOption.text);
        if (!Utils.isNullOrEmpty(customStampOption.secondText)) {
            stampObj.putText(STAMP_OPTION_TEXT_BELOW, customStampOption.secondText);
        }
        addColorToOption(stampObj, STAMP_OPTION_FILL_COLOR_START, customStampOption.bgColorStart);
        addColorToOption(stampObj, STAMP_OPTION_FILL_COLOR_END, customStampOption.bgColorEnd);
        addColorToOption(stampObj, STAMP_OPTION_TEXT_COLOR, customStampOption.textColor);
        addColorToOption(stampObj, STAMP_OPTION_BORDER_COLOR, customStampOption.borderColor);
        stampObj.putNumber(STAMP_OPTION_FILL_OPACITY, customStampOption.fillOpacity);
        stampObj.putBool(STAMP_OPTION_POINTING_LEFT, customStampOption.isPointingLeft);
        stampObj.putBool(STAMP_OPTION_POINTING_RIGHT, customStampOption.isPointingRight);
        return stampObj;
    }

    @NonNull
    private static List<CustomStampOption> loadCustomStampOptions(@NonNull Context context) {
        List<CustomStampOption> customStampOptions = new ArrayList<>();
        String customStampsInfo = getCustomStampsInfo(context);
        if (Utils.isNullOrEmpty(customStampsInfo)) {
            return customStampOptions;
        }

        try {
            JSONArray jsonArray = new JSONArray(customStampsInfo);
            for (int i = 0, count = jsonArray.length(); i < count; ++i) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                CustomStampOption customStampOption = new CustomStampOption(jsonObject);
                customStampOptions.add(customStampOption);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        return customStampOptions;
    }

    private static void saveCustomStamps(@NonNull Context context, @NonNull List<CustomStampOption> customStampOptions) {
        Gson gson = new Gson();
        Type collectionType = new TypeToken<ArrayList<CustomStampOption>>() {
        }.getType();
        String serializedData = gson.toJson(customStampOptions, collectionType);
        saveCustomStampsInfo(context, serializedData);
    }

    private static void addColorToOption(@NonNull Obj stampObj, @NonNull String arrName, int color) throws PDFNetException {
        Obj objColor = stampObj.putArray(arrName);
        double red = (double) Color.red(color) / 255.;
        double green = (double) Color.green(color) / 255.;
        double blue = (double) Color.blue(color) / 255.;
        objColor.pushBackNumber(red);
        objColor.pushBackNumber(green);
        objColor.pushBackNumber(blue);
    }

    private static int getColorFromOption(@NonNull Obj stampObj) throws PDFNetException {
        int red = (int) (255. * stampObj.getAt(0).getNumber() + .5);
        int green = (int) (255. * stampObj.getAt(1).getNumber() + .5);
        int blue = (int) (255. * stampObj.getAt(2).getNumber() + .5);
        return Color.rgb(red, green, blue);
    }

    private static String getCustomStampBitmapPath(@NonNull Context context, int index) {
        return getCustomStampBitmapPath(context, String.valueOf(index));
    }

    private static String getCustomStampBitmapPath(@NonNull Context context, String index) {
        return context.getFilesDir().getAbsolutePath() + File.separator + "custom_stamp_bitmap_" + index + ".png";
    }

    private static String getCustomStampsInfoPath(@NonNull Context context) {
        return context.getFilesDir().getAbsolutePath() + File.separator + FILE_CUSTOM_STAMPS_INFO;
    }

    @Nullable
    private static String getCustomStampsInfo(@NonNull Context context) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(getCustomStampsInfoPath(context));
            return IOUtils.toString(fis);
        } catch (IOException ignored) {
            return null;
        } finally {
            Utils.closeQuietly(fis);
        }
    }

    private static void saveCustomStampsInfo(@NonNull Context context, String fileContent) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(getCustomStampsInfoPath(context));
            IOUtils.write(fileContent, fos);
        } catch (IOException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(fos);
        }
    }

}
