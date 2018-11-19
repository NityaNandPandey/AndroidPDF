//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.model;

import android.support.annotation.Nullable;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;

import org.json.JSONObject;

// IF YOU WANT TO CHANGE THIS FILE PLEASE READ THE NOTE BELOW FIRST

/**
 * Structure corresponds to the information of a PDFViewCtrl tab
 */
public class PdfViewCtrlTabInfo {

    private static final String VAR_TAB_TITLE = "tabTitle";
    private static final String VAR_TAB_SOURCE = "tabSource";
    private static final String VAR_H_SCROLL_POS = "hScrollPos";
    private static final String VAR_V_SCROLL_POS = "vScrollPos";
    private static final String VAR_ZOOM = "zoom";
    private static final String VAR_LAST_PAGE = "lastPage";
    private static final String VAR_PAGE_ROTATION = "pageRotation";
    private static final String VAR_PAGE_PRESENTATION_MODE = "pagePresentationMode";
    private static final String VAR_TAB_LAST_VIEWED_TIMESTAMP = "tabLastViewedTimestamp";
    private static final String VAR_PASSWORD = "password";
    private static final String VAR_FILE_EXTENSION = "fileExtension";
    private static final String VAR_IS_REFLOW_MODE = "isReflowMode";
    private static final String VAR_REFLOW_TEXT_SIZE = "reflowTextSize";
    private static final String VAR_IS_RTL_MODE = "isRtlMode";
    private static final String VAR_BOOKMARK_DIALOG_CURRENT_TAB = "bookmarkDialogCurrentTab";

    // NOTE: PLEASE MAKE SURE YOU'LL MAKE NECESSARY CHANGES TO PdfViewCtrlTabInfo(JSONObject) FUNCTION
    //       IF YOU INSERT NEW PARAMS OR CHANGE VARIABLE NAMES;
    //       OTHERWISE EXISTING USERS MAY LOSE ENTIRE DATA

    /**
     * Tab title
     */
    public String tabTitle;
    /**
     * The source of tab
     */
    public int tabSource;
    /**
     * Horizontal scroll position
     */
    public int hScrollPos;
    /**
     * Vertical scroll position
     */
    public int vScrollPos;
    /**
     * Zoom factor
     */
    public double zoom;
    /**
     * Last viewed page number
     * <b> Note: </b> Page number starts from 1.
     */
    public int lastPage;
    /**
     * Page rotation
     */
    public int pageRotation;
    /**
     * Page presentation mode.
     */
    public int pagePresentationMode = PDFViewCtrl.PagePresentationMode.SINGLE.getValue();
    /**
     * Last viewed time
     */
    public String tabLastViewedTimestamp;
    /**
     * Password
     */
    public String password;
    /**
     * File extension
     */
    public String fileExtension;
    /**
     * Reflow mode
     */
    public boolean isReflowMode;
    /**
     * Reflow text size
     */
    public int reflowTextSize;
    /**
     * Right-to-left mode
     */
    public boolean isRtlMode;
    /**
     * The tab index of the bookmark dialog.
     * Note: -1 specifies the tab index has not yet defined.
     */
    public int bookmarkDialogCurrentTab = -1;

    public PdfViewCtrlTabInfo() {

    }

    public PdfViewCtrlTabInfo(JSONObject jsonObject) {
        if (!jsonObject.has(VAR_TAB_TITLE)) {
            return;
        }

        try {
            tabTitle = jsonObject.getString(VAR_TAB_TITLE);
            tabSource = jsonObject.getInt(VAR_TAB_SOURCE);
            hScrollPos = jsonObject.getInt(VAR_H_SCROLL_POS);
            vScrollPos = jsonObject.getInt(VAR_V_SCROLL_POS);
            zoom = jsonObject.getDouble(VAR_ZOOM);
            lastPage = jsonObject.getInt(VAR_LAST_PAGE);
            pageRotation = jsonObject.getInt(VAR_PAGE_ROTATION);
            pagePresentationMode = jsonObject.getInt(VAR_PAGE_PRESENTATION_MODE);

            if (jsonObject.has(VAR_TAB_LAST_VIEWED_TIMESTAMP)) {
                tabLastViewedTimestamp = jsonObject.getString(VAR_TAB_LAST_VIEWED_TIMESTAMP);
            }

            if (jsonObject.has(VAR_PASSWORD)) {
                password = jsonObject.getString(VAR_PASSWORD);
            }

            if (jsonObject.has(VAR_FILE_EXTENSION)) {
                fileExtension = jsonObject.getString(VAR_FILE_EXTENSION);
            }
            if (jsonObject.has(VAR_IS_REFLOW_MODE)) {
                isReflowMode = jsonObject.getBoolean(VAR_IS_REFLOW_MODE);
            }

            if (jsonObject.has(VAR_REFLOW_TEXT_SIZE)) {
                reflowTextSize = jsonObject.getInt(VAR_REFLOW_TEXT_SIZE);
            }

            if (jsonObject.has(VAR_IS_RTL_MODE)) {
                isRtlMode = jsonObject.getBoolean(VAR_IS_RTL_MODE);
            }

            if (jsonObject.has(VAR_BOOKMARK_DIALOG_CURRENT_TAB)) {
                bookmarkDialogCurrentTab = jsonObject.getInt(VAR_BOOKMARK_DIALOG_CURRENT_TAB);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e, "\nJson from: " + jsonObject);
        }
    }

    /**
     * @return The page presentation mode
     * See {@link com.pdftron.pdf.PDFViewCtrl.PagePresentationMode}
     */
    @Nullable
    public PDFViewCtrl.PagePresentationMode getPagePresentationMode() {
        return PDFViewCtrl.PagePresentationMode.valueOf(pagePresentationMode);
    }

    /**
     * Sets the page presentation mode.
     * See {@link com.pdftron.pdf.PDFViewCtrl.PagePresentationMode}
     *
     * @param pagePresentationMode The page presentation mode
     */
    public void setPagePresentationMode(@Nullable PDFViewCtrl.PagePresentationMode pagePresentationMode) {
        if (pagePresentationMode == null) {
            this.pagePresentationMode = 0;
        } else {
            this.pagePresentationMode = pagePresentationMode.getValue();
        }
    }

    /**
     * @return True if the page presentation mode is specified
     */
    public boolean hasPagePresentationMode() {
        return pagePresentationMode > 0;
    }
}
