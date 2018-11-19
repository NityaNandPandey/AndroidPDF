package com.pdftron.demo.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;

public class SettingsManager extends PdfViewCtrlSettingsManager {

    /*
     * Navigation Drawer tab
     */
    public static final String KEY_PREF_NAV_TAB_NONE = "none";
    public static final String KEY_PREF_NAV_TAB_VIEWER = "viewer";
    public static final String KEY_PREF_NAV_TAB_RECENT = "recent";
    public static final String KEY_PREF_NAV_TAB_FAVORITES = "favorites";
    public static final String KEY_PREF_NAV_TAB_FOLDERS = "folders";
    public static final String KEY_PREF_NAV_TAB_FILES = "files";
    public static final String KEY_PREF_NAV_TAB_EXTERNAL = "external";

    public static final String KEY_PREF_NAV_TAB = "pref_nav_tab";
    public static final String KEY_PREF_NAV_TAB_DEFAULT_VALUE = KEY_PREF_NAV_TAB_NONE;
    public static final String KEY_PREF_BROWSER_NAV_TAB = "browser_nav_tab";
    public static final String KEY_PREF_BROWSER_NAV_TAB_DEFAULT_VALUE = KEY_PREF_NAV_TAB_NONE;

    /*
     * Getting Started bundle
     */
    public final static String KEY_PREF_FIRST_TIME_RUN = "pref_first_time_run";
    public final static boolean KEY_PREF_FIRST_TIME_RUN_DEFAULT_VALUE = true;

    public static String getNavTab(Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_NAV_TAB,
            KEY_PREF_NAV_TAB_DEFAULT_VALUE);
    }

    public static void updateNavTab(Context context, String navTab) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_NAV_TAB, navTab);
        editor.apply();
    }

    public static String getBrowserNavTab(Context context) {
        return getDefaultSharedPreferences(context).getString(KEY_PREF_BROWSER_NAV_TAB,
            KEY_PREF_BROWSER_NAV_TAB_DEFAULT_VALUE);
    }

    public static void updateBrowserNavTab(Context context, String navTab) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putString(KEY_PREF_BROWSER_NAV_TAB, navTab);
        editor.apply();
    }

    public static boolean getFirstTimeRun(Context context) {
        return getDefaultSharedPreferences(context).getBoolean(KEY_PREF_FIRST_TIME_RUN,
            KEY_PREF_FIRST_TIME_RUN_DEFAULT_VALUE);
    }

    public static void updateFirstTimeRun(Context context, boolean value) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putBoolean(KEY_PREF_FIRST_TIME_RUN, value);
        editor.apply();
    }

}
