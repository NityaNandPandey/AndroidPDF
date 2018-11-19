//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.PdfViewCtrlTabInfo;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Manager class for PDFView control tabs
 */
public class PdfViewCtrlTabsManager {

    /**
     * The maximum number of tabs allowed to be opened at the same time in phone devices
     */
    public static final int MAX_NUM_TABS_PHONE = 3;
    /**
     * The maximum number of tabs allowed to be opened at the same time in tablet devices
     */
    public static final int MAX_NUM_TABS_TABLET = 5;

    private static final String KEY_PREFS_PDFVIEWCTRL_TAB_MANAGER = "prefs_pdfviewctrl_tab_manager";

    private final Type mCollectionType = new TypeToken<LinkedHashMap<String, PdfViewCtrlTabInfo>>() {
    }.getType();

    private ArrayList<String> mDocumentList;
    private LinkedHashMap<String, PdfViewCtrlTabInfo> mInternalTabsInfo;
    private HashMap<String, String> mUpdatedPaths = new HashMap<>();

    private static class LazyHolder {
        private static final PdfViewCtrlTabsManager INSTANCE = new PdfViewCtrlTabsManager();
    }

    public static PdfViewCtrlTabsManager getInstance() {
        return PdfViewCtrlTabsManager.LazyHolder.INSTANCE;
    }

    private PdfViewCtrlTabsManager() {

    }

    private static SharedPreferences getDefaultSharedPreferences(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    /**
     * Returns the path of all opened documents.
     *
     * @param context The context
     * @return A list of opened documents
     */
    public synchronized ArrayList<String> getDocuments(@NonNull Context context) {
        if (mDocumentList != null) {
            return mDocumentList;
        }

        loadAllPdfViewCtrlTabInfo(context);
        mDocumentList = new ArrayList<>(mInternalTabsInfo.keySet());
        return mDocumentList;
    }

    /**
     * Adds a file path to the list of opened documents.
     *
     * @param context  The context
     * @param filepath The file path
     */
    public synchronized void addDocument(@NonNull Context context, @Nullable String filepath) {
        if (filepath == null) {
            return;
        }

        if (mDocumentList == null) {
            loadAllPdfViewCtrlTabInfo(context);
            mDocumentList = new ArrayList<>(mInternalTabsInfo.keySet());
        }

        // put at the end of list
        if (mDocumentList.contains(filepath)) {
            mDocumentList.remove(filepath);
        }
        mDocumentList.add(filepath);
    }

    /**
     * Removes a file path from the list of opened documents.
     *
     * @param context  The context
     * @param filepath The file path
     */
    public synchronized void removeDocument(@NonNull Context context, @Nullable String filepath) {
        if (mDocumentList == null) {
            loadAllPdfViewCtrlTabInfo(context);
            mDocumentList = new ArrayList<>(mInternalTabsInfo.keySet());
        }
        mDocumentList.remove(filepath);

        // remove this entry from TabInfo dictionary
        removePdfViewCtrlTabInfo(context, filepath);
    }

    /**
     * Cleans up resources.
     */
    public void cleanup() {
        mDocumentList = null;
    }

    private synchronized void updateAllPdfViewCtrlTabInfo(@NonNull final Context context) {
        // update shared preferences with the information from mInternalTabsInfo

        if (mInternalTabsInfo == null || mInternalTabsInfo.isEmpty()) {
            clearAllPdfViewCtrlTabInfo(context);
            return;
        }

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs != null) {
            Gson gson = new Gson();
            String serializedInfo = gson.toJson(mInternalTabsInfo, mCollectionType);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_PREFS_PDFVIEWCTRL_TAB_MANAGER, serializedInfo);
            editor.apply();
        }
    }

    private synchronized void loadAllPdfViewCtrlTabInfo(@NonNull Context context) {
        if (mInternalTabsInfo != null) {
            return;
        }
        mInternalTabsInfo = new LinkedHashMap<>();

        try {
            SharedPreferences prefs = getDefaultSharedPreferences(context);
            if (prefs != null) {
                String serializedInfo = prefs.getString(KEY_PREFS_PDFVIEWCTRL_TAB_MANAGER, "");
                if (!Utils.isNullOrEmpty(serializedInfo)) {
                    JSONObject jsonObject = new JSONObject(serializedInfo);
                    Iterator<?> keys = jsonObject.keys();
                    while (keys.hasNext()) {
                        try {
                            String key = (String) keys.next();
                            if (jsonObject.get(key) instanceof JSONObject) {
                                JSONObject infoJsonObject = (JSONObject) jsonObject.get(key);
                                PdfViewCtrlTabInfo info = new PdfViewCtrlTabInfo(infoJsonObject);
                                mInternalTabsInfo.put(key, info);
                            }
                        } catch (Exception e) {
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Returns the PDFView control tab for the specified file path
     *
     * @param context  The context
     * @param filepath The file path
     * @return The {@link PdfViewCtrlTabInfo} for the specified file path
     */
    public synchronized PdfViewCtrlTabInfo getPdfFViewCtrlTabInfo(@NonNull Context context, @NonNull String filepath) {
        loadAllPdfViewCtrlTabInfo(context);
        return mInternalTabsInfo.get(filepath);
    }

    /**
     * Adds the PDFView control tab linked to the specified file path
     *
     * @param context  The context
     * @param filepath The file path
     * @param info     The {@link PdfViewCtrlTabInfo}
     */
    public synchronized void addPdfViewCtrlTabInfo(@NonNull Context context, String filepath, PdfViewCtrlTabInfo info) {
        if (filepath == null || info == null) {
            return;
        }

        if (info.tabTitle == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("tab title is null:" + info));
            return;
        }

        loadAllPdfViewCtrlTabInfo(context);
        mInternalTabsInfo.put(filepath, info);
        updateAllPdfViewCtrlTabInfo(context);
    }

    /**
     * Updates a PDFView control tab.
     *
     * @param context     The context
     * @param oldFilepath The file path linked to the desired {@link PdfViewCtrlTabInfo}
     * @param newFilepath The new file path that should be linked to the {@link PdfViewCtrlTabInfo}
     * @param newFilename The new file name for the {@link PdfViewCtrlTabInfo}
     */
    public synchronized void updatePdfViewCtrlTabInfo(@NonNull Context context, String oldFilepath, String newFilepath, String newFilename) {
        if (Utils.isNullOrEmpty(oldFilepath) || Utils.isNullOrEmpty(newFilepath)) {
            return;
        }

        loadAllPdfViewCtrlTabInfo(context);
        PdfViewCtrlTabInfo info = mInternalTabsInfo.get(oldFilepath);
        if (info != null) {
            // remove old entry
            mInternalTabsInfo.remove(oldFilepath);

            // add new entry
            info.tabTitle = FilenameUtils.removeExtension(newFilename);
            mInternalTabsInfo.put(newFilepath, info);

            updateAllPdfViewCtrlTabInfo(context);

            // update previous map
            // Note: if a->b is already in the map and now we have b->c then the new map
            // should have both a->c and b->c
            for (Map.Entry<String, String> entry : mUpdatedPaths.entrySet()) {
                if (entry.getValue().equals(oldFilepath)) {
                    mUpdatedPaths.put(entry.getKey(), newFilepath);
                }
            }
            mUpdatedPaths.put(oldFilepath, newFilepath);
        }
    }

    /**
     * Removes all PDFView control tabs from the repository.
     *
     * @param context The context
     */
    public synchronized void clearAllPdfViewCtrlTabInfo(@NonNull Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs != null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(KEY_PREFS_PDFVIEWCTRL_TAB_MANAGER);
            editor.apply();
        }
        if (mInternalTabsInfo == null) {
            mInternalTabsInfo = new LinkedHashMap<>();
        } else {
            mInternalTabsInfo.clear();
        }
    }

    /**
     * Removes the PDFView control tab linked to the specified file path.
     *
     * @param context  The context
     * @param filepath The file path
     */
    public synchronized void removePdfViewCtrlTabInfo(@Nullable Context context, String filepath) {
        if (context == null || filepath == null) {
            return;
        }

        loadAllPdfViewCtrlTabInfo(context);
        mInternalTabsInfo.remove(filepath);
        updateAllPdfViewCtrlTabInfo(context);
    }

    /**
     * Updates the view mode of the PDFView control tab linked to the specified file path.
     *
     * @param context  The context
     * @param filepath The file path
     * @param mode     The page presentation mode.
     */
    public void updateViewModeForTab(@NonNull Context context, String filepath, PDFViewCtrl.PagePresentationMode mode) {
        if (filepath == null) {
            return;
        }

        loadAllPdfViewCtrlTabInfo(context);
        PdfViewCtrlTabInfo info = mInternalTabsInfo.get(filepath);
        if (info != null) {
            info.setPagePresentationMode(mode);
            addPdfViewCtrlTabInfo(context, filepath, info);
        }
    }

    /**
     * Updates when the PDFView control tab (linked to the specified file path) was viewed last time.
     *
     * @param context  The context
     * @param filepath The file path
     */
    public void updateLastViewedTabTimestamp(@NonNull Context context, String filepath) {
        if (filepath == null) {
            return;
        }

        loadAllPdfViewCtrlTabInfo(context);
        PdfViewCtrlTabInfo info = mInternalTabsInfo.get(filepath);
        if (info != null) {
            java.util.Date date = new Date();
            info.tabLastViewedTimestamp = (new Timestamp(date.getTime())).toString();
            addPdfViewCtrlTabInfo(context, filepath, info);
        }
    }

    /**
     * Returns the file path of the PDFView control tab that was viewed most recently.
     *
     * @param context The context
     */
    public String getLatestViewedTabTag(@NonNull Context context) {
        String latestViewedTag = null;
        loadAllPdfViewCtrlTabInfo(context);
        Timestamp timestamp = null;
        for (Map.Entry<String, PdfViewCtrlTabInfo> entry : mInternalTabsInfo.entrySet()) {
            String key = entry.getKey();
            PdfViewCtrlTabInfo value = entry.getValue();
            if (timestamp == null) {
                latestViewedTag = key;
                timestamp = safeGetTimestamp(value.tabLastViewedTimestamp);
            }
            if (timestamp != null && value.tabLastViewedTimestamp != null) {
                Timestamp tabLastViewedTimestamp = safeGetTimestamp(value.tabLastViewedTimestamp);
                if (tabLastViewedTimestamp != null && tabLastViewedTimestamp.after(timestamp)) {
                    latestViewedTag = key;
                    timestamp = tabLastViewedTimestamp;
                }
            }
        }

        return latestViewedTag;
    }

    /**
     * Removes the PDFView control tab that was viewed least recently.
     *
     * @param context The context
     */
    public String removeOldestViewedTab(@NonNull Context context) {
        String fileToRemove = null;
        loadAllPdfViewCtrlTabInfo(context);
        java.util.Date date = new Date();
        Timestamp timestamp = new Timestamp(date.getTime());
        Set<Map.Entry<String, PdfViewCtrlTabInfo>> entries = mInternalTabsInfo.entrySet();
        for (Map.Entry<String, PdfViewCtrlTabInfo> entry : entries) {
            String key = entry.getKey();
            PdfViewCtrlTabInfo value = entry.getValue();
            if (null != value.tabLastViewedTimestamp) {
                Timestamp tabLastViewedTimestamp = safeGetTimestamp(value.tabLastViewedTimestamp);
                if (tabLastViewedTimestamp != null && tabLastViewedTimestamp.before(timestamp)) {
                    fileToRemove = key;
                    timestamp = tabLastViewedTimestamp;
                }
            }
        }

        if (fileToRemove != null) {
            PdfViewCtrlTabsManager.getInstance().removeDocument(context, fileToRemove);
        }

        return fileToRemove;
    }

    private static Timestamp safeGetTimestamp(String timeStr) {
        if (timeStr == null) {
            return null;
        }
        Timestamp timestamp;
        try {
            timestamp = Timestamp.valueOf(timeStr);
        } catch (Exception e) {
            // try fallback
            timestamp = getFallbackTimestamp(timeStr);
        }
        return timestamp;
    }

    private static Timestamp getFallbackTimestamp(String timeStr) {
        Timestamp result;
        SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.US);
        ParsePosition pp = new ParsePosition(0);
        Date tmpDate;
        try {
            tmpDate = df.parse(timeStr, pp);
            result = new Timestamp(tmpDate.getTime());
        } catch (Exception e2) {
            result = null;
        }
        return result;
    }

    /**
     * Returns the most updated file path. The file path of a PDFView control tab can be changed
     * through {@link #updatePdfViewCtrlTabInfo}.
     *
     * @param filepath The file path
     * @return The new file path
     */
    @Nullable
    public String getNewPath(String filepath) {
        if (Utils.isNullOrEmpty(filepath)) {
            return null;
        }

        return mUpdatedPaths.get(filepath);
    }
}
