//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.asynctask;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.controls.ReflowControl;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A class that asynchronously generates an html file from the specified html where all its colors
 * are changed based on the given post processor
 */
public class HtmlPostProcessColorTask extends CustomAsyncTask<Void, Void, Void> {

    private String mHtmlFilename;
    private ReflowControl.OnPostProcessColorListener mOnPostProcessColorListener;
    private Callback mCallback;

    /**
     * Callback interface invoked when post-processing the color of the specified html is finished.
     */
    public interface Callback {
        /**
         * Called when post-processing the color of the specified html has been finished.
         *
         * @param outputFilename The generated html filename
         */
        void onPostProcessColorFinished(@NonNull HtmlPostProcessColorTask htmlPostProcessColorTask, String outputFilename);
    }

    /**
     * Class constructor
     *
     * @param context      The context
     * @param htmlFilename The input html filename
     */
    public HtmlPostProcessColorTask(Context context,
                                    @NonNull String htmlFilename) {
        super(context);
        mHtmlFilename = htmlFilename;
    }

    /**
     * Sets the callback listener.
     * <p>
     * Sets the callback to null when the task is cancelled.
     *
     * @param callback The callback when the task is finished
     */
    public void setCallback(@Nullable Callback callback) {
        mCallback = callback;
    }

    /**
     * Sets the OnPostProcessColorListener listener.
     * <p>
     * Sets the listener to null when the task is cancelled.
     *
     * @param listener The listener that post-processes the given color
     */
    public void setOnPostProcessColorListener(@Nullable ReflowControl.OnPostProcessColorListener listener) {
        mOnPostProcessColorListener = listener;
    }

    /**
     * The overloaded implementation of {@link android.os.AsyncTask#doInBackground(Object[])}.
     **/
    @Override
    protected Void doInBackground(Void... voids) {
        if (Utils.isNullOrEmpty(mHtmlFilename) || mOnPostProcessColorListener == null) {
            return null;
        }
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            File inputFile = new File(mHtmlFilename);
            byte[] input = new byte[(int) inputFile.length()];
            inputStream = new FileInputStream(inputFile);
            //noinspection ResultOfMethodCallIgnored
            inputStream.read(input);
            String data = new String(input, "UTF-8");

            if (isCancelled()) {
                return null;
            }

            File outputFile = new File(getCustomColorPath(mHtmlFilename), getCustomColorName(mHtmlFilename));
            outputStream = new FileOutputStream(outputFile);
            StringBuilder buffer = new StringBuilder();
            int index = 0;

            if (isCancelled()) {
                return null;
            }

            Pattern tagPattern = Pattern.compile("<.*?>");
            Pattern colorPattern = Pattern.compile("color:#[0-9|a-f|A-F]{6}");
            Pattern highlightPattern = Pattern.compile("background-color:#[0-9|a-f|A-F]{6}");
            Pattern paragraphPattern = Pattern.compile("<p.*?>");
            Pattern spanPattern = Pattern.compile("<span.*?>");

            Matcher tagMatcher = tagPattern.matcher(data);
            while (tagMatcher.find()) { // Find each match in turn
                String tag = tagMatcher.group();
                Matcher colorMatcher = colorPattern.matcher(tag);
                while (colorMatcher.find() && !isCancelled()) { // Find each match in turn
                    // output until the matched string
                    String subStr = data.substring(index, tagMatcher.start() + colorMatcher.start());
                    buffer.append(subStr);
                    index += subStr.length();

                    if (!highlightPattern.matcher(tag).find() &&
                        (paragraphPattern.matcher(tag).find() || spanPattern.matcher(tag).find())) {
                        // the matched color pattern doesn't belong to background color which
                        // indicates highlighted text, while belongs to either paragraph or span
                        String color = colorMatcher.group();
                        ColorPt inputCP = getColorPt(color.substring(7, 13));
                        if (inputCP == null) {
                            inputCP = new ColorPt();
                        }
                        ReflowControl.OnPostProcessColorListener listener = mOnPostProcessColorListener;
                        if (listener == null) {
                            return null;
                        }
                        ColorPt outputCP = listener.getPostProcessedColor(inputCP);
                        if (outputCP == null) {
                            outputCP = inputCP;
                        }
                        // As an alternative solution we can cache outputCP for later
                        // use to ignore frequent calls to the core.
                        String customColor = "color:#" + getHexadecimal(outputCP);
                        buffer.append(customColor);
                        index += customColor.length();
                    }
                }
            }

            if (isCancelled()) {
                return null;
            }

            buffer.append(data.substring(index));
            outputStream.write(buffer.toString().getBytes());
        } catch (FileNotFoundException e) {
            Context context = getContext();
            if (context instanceof FragmentActivity && !((FragmentActivity) context).isFinishing()) {
                AnalyticsHandlerAdapter.getInstance().sendException(e,
                    "avail memory: " + getAvailableInternalMemorySize());
            }
        } catch (IOException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e, "Can not read/write file");
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(inputStream);
            Utils.closeQuietly(outputStream);
        }
        return null;
    }

    private static String getAvailableInternalMemorySize() {
        if (!Utils.isJellyBeanMR2()) {
            return "";
        }
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return formatSize(availableBlocks * blockSize);
    }

    private static String formatSize(long size) {
        String suffix = null;

        if (size >= 1024) {
            suffix = "KB";
            size /= 1024;
            if (size >= 1024) {
                suffix = "MB";
                size /= 1024;
            }
        }

        StringBuilder resultBuffer = new StringBuilder(Long.toString(size));

        int commaOffset = resultBuffer.length() - 3;
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',');
            commaOffset -= 3;
        }

        if (suffix != null) resultBuffer.append(suffix);
        return resultBuffer.toString();
    }


    /**
     * The overloaded implementation of {@link android.os.AsyncTask#onPostExecute(Object)}.
     **/
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        Callback callback = mCallback;
        if (callback != null) {
            callback.onPostProcessColorFinished(this, getCustomColorFullName(mHtmlFilename));
        }
    }

    private String getCustomColorFullName(String inputFilename) {
        String outputFilepath = "", outputFilename;
        int index = inputFilename.lastIndexOf('/');
        if (index != -1) {
            outputFilepath = inputFilename.substring(0, index);
        }
        outputFilename = inputFilename.substring(index + 1);
        index = outputFilename.lastIndexOf('.');
        if (index != -1) {
            outputFilename = outputFilename.substring(0, index);
        }
        outputFilename += "-cus.html";

        return outputFilepath + "/" + outputFilename;
    }

    private static String getCustomColorName(String inputFilename) {
        String outputFilename;
        int index = inputFilename.lastIndexOf('/');
        outputFilename = inputFilename.substring(index + 1);
        index = outputFilename.lastIndexOf('.');
        if (index != -1) {
            outputFilename = outputFilename.substring(0, index);
        }
        outputFilename += "-cus.html";

        return outputFilename;
    }

    private static String getCustomColorPath(String inputFilename) {
        String outputFilepath = "";
        int index = inputFilename.lastIndexOf('/');
        if (index != -1) {
            outputFilepath = inputFilename.substring(0, index);
        }
        return outputFilepath;
    }

    private static ColorPt getColorPt(String str) {
        int len = str.length();
        if (len != 6 && len != 8) {
            return null;
        }

        double x, y, z, w = 0;
        if (len == 8) {
            w = Integer.parseInt(str.substring(len - 2, len), 16);
            len -= 2;
        }
        z = Integer.parseInt(str.substring(len - 2, len), 16);
        len -= 2;
        y = Integer.parseInt(str.substring(len - 2, len), 16);
        len -= 2;
        x = Integer.parseInt(str.substring(len - 2, len), 16);

        try {
            len = str.length();
            if (len == 6) {
                return new ColorPt(x / 255., y / 255., z / 255.);
            } else { // if (len == 8)
                return new ColorPt(x / 255., y / 255., z / 255., w / 255.);
            }
        } catch (PDFNetException e) {
            return null;
        }
    }

    private static String getHexadecimal(ColorPt cp) {
        try {
            int x = (int) (cp.get(0) * 255.0);
            int y = (int) (cp.get(1) * 255.0);
            int z = (int) (cp.get(2) * 255.0);
            return String.format("%1$02X%2$02X%3$02X", x, y, z);
        } catch (PDFNetException e) {
            return "";
        }
    }
}
