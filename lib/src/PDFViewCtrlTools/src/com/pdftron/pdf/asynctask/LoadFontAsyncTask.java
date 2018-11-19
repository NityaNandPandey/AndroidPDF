//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.asynctask;


import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import com.pdftron.pdf.PDFNet;
import com.pdftron.pdf.config.ToolConfig;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Async Task for loading fonts in system path.
 */
public class LoadFontAsyncTask extends CustomAsyncTask<Void, Void, ArrayList<FontResource>> {
    private ArrayList<FontResource> mFonts;
    private Set<String> mWhiteListFonts;
    private Callback mCallback;

    /**
     * A interface for listening finish event
     */
    public interface Callback {
        /**
         * This method is invoked when async task is finished
         * @param fonts The loaded fonts
         */
        void onFinish(ArrayList<FontResource> fonts);
    }

    /**
     * Class constructor
     * @param context The context
     * @param whiteListFonts white list fonts
     */
    public  LoadFontAsyncTask(Context context, Set<String> whiteListFonts) {
        super(context);
        mFonts = new ArrayList<>();
        mWhiteListFonts = whiteListFonts;
    }

    /**
     * Sets the callback listener.
     *
     * Sets the callback to null when the task is cancelled.
     *
     * @param callback The callback when the task is finished
     */
    public void setCallback(@Nullable Callback callback) {
        mCallback = callback;
    }

    private void loadFontResources() {
        try {
            SharedPreferences settings;
            {
                Context context = getContext();
                if (context == null) {
                    return;
                }
                settings = Tool.getToolPreferences(context);
            }
            String fontInfo = settings.getString(Tool.ANNOTATION_FREE_TEXT_FONTS, "");

            if (fontInfo.equals("")) {
                fontInfo = PDFNet.getSystemFontList();

                // save font list
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(Tool.ANNOTATION_FREE_TEXT_FONTS, fontInfo);
                editor.apply();
            }

            JSONObject fontInfoObject = new JSONObject(fontInfo);
            JSONArray fontInfoArray = fontInfoObject.getJSONArray(Tool.ANNOTATION_FREE_TEXT_JSON_FONT);

            int numWhiteListFonts = 0;
            int numFoundWhiteListFonts = 0;
            if (mWhiteListFonts != null && !mWhiteListFonts.isEmpty()) {
                numWhiteListFonts = mWhiteListFonts.size();
            } else {
                mWhiteListFonts = new HashSet<>();
                fontInfo = FontResource.whiteListFonts(fontInfo);

                fontInfoObject = new JSONObject(fontInfo);
                fontInfoArray = fontInfoObject.getJSONArray(Tool.ANNOTATION_FREE_TEXT_JSON_FONT);

                // white list the fonts
                for (int i = 0; i < fontInfoArray.length(); i++) {
                    JSONObject font = fontInfoArray.getJSONObject(i);
                    String entryValue = font.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_FILE_PATH);

                    // set default values
                    String whiteList = font.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_DISPLAY_IN_LIST);
                    if (whiteList.equals("true")) {
                        mWhiteListFonts.add(entryValue);
                    }
                }
            }

            for (int i = 0; i < fontInfoArray.length(); i++) {
                // check if font is selected in settings
                JSONObject font = fontInfoArray.getJSONObject(i);
                String filePath = font.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_FILE_PATH);

                // if white list fonts equals null, show all fonts to users. If not, only show
                // the white listed fonts.
                if (mWhiteListFonts.contains(filePath)) {
                    // if display name is null or empty, replace it will the file name
                    String displayName = font.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_DISPLAY_NAME);
                    if (displayName == null || displayName.equals("")) {
                        displayName = Utils.getFontFileName(font.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_FILE_PATH));
                        font.put(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_DISPLAY_NAME, displayName);
                    }

                    // check if the font has it's font name (used for setting the location
                    // of the font spinner)
                    String fontName = "";
                    if (font.has(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_NAME)) {
                        fontName = font.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_NAME);
                    }

                    // create fontResource and add to fonts
                    String pdfTronName = font.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_PDFTRON_NAME);
                    FontResource fontResource = new FontResource(displayName, filePath, fontName, pdfTronName);
                    mFonts.add(fontResource);

                    numFoundWhiteListFonts++;
                }
            }

            // if the number of found white listed fonts does not equal the number
            // of white listed fonts, then the user has added another font
            if (numFoundWhiteListFonts != numWhiteListFonts) {
                // get system list - used to compare against the saved font list
                // to determine which fonts are missing
                String fontSystemInfo = PDFNet.getSystemFontList();
                JSONObject fontSystemObject = new JSONObject(fontSystemInfo);
                JSONArray fontSystemArray = fontSystemObject.getJSONArray(Tool.ANNOTATION_FREE_TEXT_JSON_FONT);

                for (int i = 0; i < fontSystemArray.length(); i++) {
                    String fontSystemFilePath = fontSystemArray.getJSONObject(i).getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_FILE_PATH);
                    Boolean found = false;

                    for (int j = 0; j < fontInfoArray.length(); j++) {
                        String fontInfoFilePath = fontInfoArray.getJSONObject(j).getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_FILE_PATH);

                        // if font is already in font info string, break loop and don't
                        // add font to array
                        if (fontSystemFilePath.equals(fontInfoFilePath)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        // add font to JSON array
                        fontInfoArray.put(fontSystemArray.getJSONObject(i));

                        // add font to list of fonts if on white list
                        String filePath = fontSystemArray.getJSONObject(i).getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_FILE_PATH);
                        if (mWhiteListFonts.contains(filePath)) {
                            String displayName = fontSystemArray.getJSONObject(i).getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_DISPLAY_NAME);
                            String pdftronName = fontSystemArray.getJSONObject(i).getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_PDFTRON_NAME);
                            FontResource fontResource = new FontResource(displayName, filePath, "", pdftronName);
                            mFonts.add(fontResource);
                        }
                    }
                }

                fontInfo = fontInfoObject.toString();
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(Tool.ANNOTATION_FREE_TEXT_FONTS, fontInfo);
                editor.apply();
            }

        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        // sort fonts alphabetically
        Collections.sort(mFonts, new Comparator<FontResource>() {
            @Override
            public int compare(FontResource lhs, FontResource rhs) {
                return lhs.getDisplayName().compareTo(rhs.getDisplayName());
            }
        });
    }

    /**
     * Overload implementation on {@link CustomAsyncTask#doInBackground(Object[])}.
     * Adds system fonts to fonts
     * @param voids The void parameters of the task.
     * @return a array list of font resource
     */
    @Override
    protected ArrayList<FontResource> doInBackground(Void... voids) {
        loadFontResources();
        return mFonts;
    }


    /**
     * Overload implementation on {@link CustomAsyncTask#onPostExecute(Object)}
     * @param fontResources font resources that finished loading
     */
    @Override
    protected void onPostExecute(ArrayList<FontResource> fontResources) {
        super.onPostExecute(fontResources);
        // save fonts for future usage
        ToolConfig.getInstance().setFontList(fontResources);
        if (mCallback != null) {
            mCallback.onFinish(mFonts);
        }
    }
}
