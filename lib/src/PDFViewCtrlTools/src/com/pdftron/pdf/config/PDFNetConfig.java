//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.config;

import android.content.Context;
import android.util.Log;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a utility class for setting up PDFNet initially.
 * <p>
 * <div class="warning">
 * If any of the properties are changed via PDFNet,
 * this default config class will not get updated.
 * </div>
 */
public class PDFNetConfig {

    private static final String TAG = PDFNetConfig.class.getName();

    private static final String LAYOUT_PLUGIN_NAME = "pdftron_layout_resources.plugin";
    private static final String LAYOUT_SMART_PLUGIN_NAME = "pdftron_smart_substitution.plugin";

    public PDFNetConfig() {
    }

    public PDFNetConfig(Context context, int xmlRes) {
        try {
            HashMap<String, String> results = Utils.parseDefaults(context, xmlRes);
            for (Map.Entry<String, String> entry : results.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key.equals("javaScriptEnabled")) {
                    javaScriptEnabled = Boolean.parseBoolean(value);
                }
                if (key.equals("diskCachingEnabled")) {
                    diskCachingEnabled = Boolean.parseBoolean(value);
                }
                if (key.equals("persistentCachePath")) {
                    File file = new File(value);
                    if (file.exists()) {
                        persistentCachePath = value;
                    }
                }
                if (key.equals("extraResourcePaths")) {
                    File file = new File(value);
                    if (file.exists()) {
                        addExtraResourcePaths(file);
                    }
                }
                if (key.equals("tempPath")) {
                    File file = new File(value);
                    if (file.exists()) {
                        tempPath = value;
                    }
                }
                if (key.equals("viewerCacheMaxSize")) {
                    viewerCacheMaxSize = Integer.parseInt(value);
                }
                if (key.equals("viewerCacheOnDisk")) {
                    viewerCacheOnDisk = Boolean.parseBoolean(value);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
    }

    /**
     * Gets custom search location for PDFNet resources.
     * See {@link com.pdftron.pdf.PDFNet#addResourceSearchPath(String)}
     */
    public ArrayList<File> getExtraResourcePaths() {
        return extraResourcePaths;
    }

    /**
     * Adds custom search location for PDFNet resources.
     * See {@link com.pdftron.pdf.PDFNet#addResourceSearchPath(String)}
     */
    public void addExtraResourcePaths(File extraResourcePath) {
        if (this.extraResourcePaths == null) {
            this.extraResourcePaths = new ArrayList<>();
        }

        this.extraResourcePaths.add(extraResourcePath);
    }

    /**
     * See {@link com.pdftron.pdf.PDFNet#enableJavaScript(boolean)}
     */
    public boolean isJavaScriptEnabled() {
        return javaScriptEnabled;
    }

    /**
     * See {@link com.pdftron.pdf.PDFNet#enableJavaScript(boolean)}
     */
    public void setJavaScriptEnabled(boolean javaScriptEnabled) {
        this.javaScriptEnabled = javaScriptEnabled;
    }

    /**
     * See {@link com.pdftron.pdf.PDFNet#setPersistentCachePath(String)}
     */
    public String getPersistentCachePath() {
        return persistentCachePath;
    }

    /**
     * See {@link com.pdftron.pdf.PDFNet#setPersistentCachePath(String)}
     */
    public void setPersistentCachePath(String persistentCachePath) {
        this.persistentCachePath = persistentCachePath;
    }

    /**
     * See {@link com.pdftron.pdf.PDFNet#setTempPath(String)}
     */
    public String getTempPath() {
        return tempPath;
    }

    /**
     * See {@link com.pdftron.pdf.PDFNet#setTempPath(String)}
     */
    public void setTempPath(String tempPath) {
        this.tempPath = tempPath;
    }

    /**
     * See {@link com.pdftron.pdf.PDFNet#setDefaultDiskCachingEnabled(boolean)}
     */
    public boolean isDiskCachingEnabled() {
        return diskCachingEnabled;
    }

    /**
     * See {@link com.pdftron.pdf.PDFNet#setDefaultDiskCachingEnabled(boolean)}
     */
    public void setDiskCachingEnabled(boolean diskCachingEnabled) {
        this.diskCachingEnabled = diskCachingEnabled;
    }

    /**
     * See {@link com.pdftron.pdf.PDFNet#setViewerCache(int, boolean)}
     */
    public int getViewerCacheMaxSize() {
        return viewerCacheMaxSize;
    }

    /**
     * See {@link com.pdftron.pdf.PDFNet#setViewerCache(int, boolean)}
     */
    public void setViewerCacheMaxSize(int viewerCacheMaxSize) {
        this.viewerCacheMaxSize = viewerCacheMaxSize;
    }

    /**
     * See {@link com.pdftron.pdf.PDFNet#setViewerCache(int, boolean)}
     */
    public boolean isViewerCacheOnDisk() {
        return viewerCacheOnDisk;
    }

    /**
     * See {@link com.pdftron.pdf.PDFNet#setViewerCache(int, boolean)}
     */
    public void setViewerCacheOnDisk(boolean viewerCacheOnDisk) {
        this.viewerCacheOnDisk = viewerCacheOnDisk;
    }

    /**
     * Gets folder path for layout plugin used for universal conversion
     * @param applicationContext application context
     * @return folder path
     */
    public String getLayoutPluginPath(Context applicationContext) {
        try {
            return Utils.copyResourceToTempFolder(applicationContext,
                R.raw.pdftron_layout_resources, false, LAYOUT_PLUGIN_NAME);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    /**
     * Gets folder path for layout plugin used for universal conversion
     * @param applicationContext application context
     * @return folder path
     */
    public String getLayoutSmartPluginPath(Context applicationContext) {
        try {
            Utils.copyResourceToTempFolder(applicationContext,
                R.raw.pdftron_smart_substitution, false, LAYOUT_SMART_PLUGIN_NAME);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    // Resource related
    private ArrayList<File> extraResourcePaths;

    // PDFNet basics
    private boolean javaScriptEnabled = true;
    private boolean diskCachingEnabled = true;
    private String persistentCachePath;
    private String tempPath;
    private int viewerCacheMaxSize = 100 * 1024 * 1024;
    private boolean viewerCacheOnDisk = false;

    /**
     * Gets default PDFNet configuration
     * @return the PDFNet configuration
     */
    public static PDFNetConfig getDefaultConfig() {
        return new PDFNetConfig();
    }

    /**
     * Gets PDFNet configuration from XML resource
     * @param applicationContext application context
     * @param xmlRes the XML resource Id
     * @return the PDFNet configuration
     */
    public static PDFNetConfig loadFromXML(Context applicationContext, int xmlRes) {
        return new PDFNetConfig(applicationContext, xmlRes);
    }
}
