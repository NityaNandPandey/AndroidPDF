//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Date;

//////////////////////////////////////////////////////////////////////
// IF YOU WANT TO CHANGE THIS FILE PLEASE READ THE NOTE BELOW FIRST //
//////////////////////////////////////////////////////////////////////

/**
 * Facility class for file.
 * <p>
 * <div class="warning">
 * Note: Please make sure you'll make necessary changes to {@link #FileInfo(JSONObject)} function
 * if you insert new params or change variable names. Otherwise existing users may lose entire data.
 * </div>
 */
@SuppressWarnings("WeakerAccess")
public class FileInfo implements BaseFileInfo {

    /**
     * @hide
     */
    protected static final String VAR_TYPE = "mType";
    /**
     * @hide
     */
    protected static final String VAR_FILE = "mFile";
    /**
     * @hide
     */
    protected static final String VAR_FILE_URI = "mFileUri";
    /**
     * @hide
     */
    protected static final String VAR_FILE_NAME = "mFileName";
    /**
     * @hide
     */
    protected static final String VAR_LAST_PAGE = "mLastPage";
    /**
     * @hide
     */
    protected static final String VAR_PAGE_ROTATION = "mPageRotation";
    /**
     * @hide
     */
    protected static final String VAR_PAGE_PRESENTATION_MODE = "mPagePresentationMode";
    /**
     * @hide
     */
    protected static final String VAR_H_SCROLL_POS = "mHScrollPos";
    /**
     * @hide
     */
    protected static final String VAR_V_SCROLL_POS = "mVScrollPos";
    /**
     * @hide
     */
    protected static final String VAR_ZOOM = "mZoom";
    /**
     * @hide
     */
    protected static final String VAR_IS_SECURED = "mIsSecured";
    /**
     * @hide
     */
    protected static final String VAR_IS_PACKAGE = "mIsPackage";
    /**
     * @hide
     */
    protected static final String VAR_IS_REFLOW_MODE = "mIsReflowMode";
    /**
     * @hide
     */
    protected static final String VAR_REFLOW_TEXT_SIZE = "mReflowTextSize";
    /**
     * @hide
     */
    protected static final String VAR_IS_RTL_MODE = "mIsRtlMode";
    /**
     * @hide
     */
    protected static final String VAR_IS_HIDDEN = "mIsHidden";
    /**
     * @hide
     */
    protected static final String VAR_BOOKMARK_DIALOG_CURRENT_TAB = "mBookmarkDialogCurrentTab";
    /**
     * @hide
     */
    protected static final String VAR_IS_HEADER = "mIsHeader";
    /**
     * @hide
     */
    protected static final String VAR_SECTION_FIRST_POS = "mSectionFirstPos";

    /**
     * @hide
     */
    protected int mType;
    /**
     * @hide
     */
    protected File mFile;
    /**
     * @hide
     */
    protected String mFileUri;
    /**
     * @hide
     */
    protected String mFileName;

    /**
     * @hide
     */
    protected int mLastPage;

    /**
     * @hide
     */
    protected int mPageRotation;
    /**
     * @hide
     */
    protected int mPagePresentationMode;

    /**
     * @hide Horizontal scrolling position
     */
    protected int mHScrollPos;

    /**
     * @hide Vertical scrolling position
     */
    protected int mVScrollPos;
    /**
     * @hide
     */
    protected double mZoom;
    /**
     * @hide
     */
    protected boolean mIsSecured;
    /**
     * @hide
     */
    protected boolean mIsPackage;
    /**
     * @hide
     */
    protected boolean mIsReflowMode;
    /**
     * @hide
     */
    protected int mReflowTextSize;
    /**
     * @hide
     */
    protected boolean mIsRtlMode;
    /**
     * @hide
     */
    protected boolean mIsHidden;
    /**
     * @hide
     */
    protected int mBookmarkDialogCurrentTab;
    /**
     * @hide
     */
    protected boolean mIsHeader;
    /**
     * @hide
     */
    protected int mSectionFirstPos = -1;

    /**
     * Class constructor
     */
    public FileInfo(FileInfo another) {
        mType = another.mType;
        if (another.mFile != null) {
            try {
                mFile = new File(another.mFile.getAbsolutePath());
            } catch (Exception e) {
                sendException(another.mFile.getPath(), another.getType());
            }
        }
        mFileUri = another.mFileUri;
        mFileName = another.mFileName;
        mLastPage = another.mLastPage;
        mPageRotation = another.mPageRotation;
        mPagePresentationMode = another.mPagePresentationMode;
        mHScrollPos = another.mHScrollPos;
        mVScrollPos = another.mVScrollPos;
        mZoom = another.mZoom;
        mIsSecured = another.mIsSecured;
        mIsPackage = another.mIsPackage;
        mIsReflowMode = another.mIsReflowMode;
        mReflowTextSize = another.mReflowTextSize;
        mIsRtlMode = another.mIsRtlMode;
        mIsHidden = another.mIsHidden;
        mBookmarkDialogCurrentTab = another.mBookmarkDialogCurrentTab;
        mIsHeader = another.mIsHeader;
        mSectionFirstPos = another.mSectionFirstPos;
    }

    /**
     * Class constructor
     */
    public FileInfo(int type, File file) {
        this(type, file, false, 1);
    }

    /**
     * Class constructor
     */
    public FileInfo(int type, File file, int lastPage) {
        this(type, file, false, lastPage);
    }

    /**
     * Class constructor
     */
    public FileInfo(int type, File file, boolean isSecured, int lastPage) {
        mType = type;
        mFile = file;
        mIsSecured = isSecured;
        mLastPage = lastPage;
    }

    /**
     * Class constructor
     */
    public FileInfo(int type, String fileUri, String filename, boolean isSecured, int lastPage) {
        mType = type;
        mFileUri = fileUri;
        mFileName = filename;
        mIsSecured = isSecured;
        mLastPage = lastPage;
    }

    /**
     * Class constructor
     */
    public FileInfo(JSONObject jsonObject) {
        if (!jsonObject.has(VAR_TYPE)) {
            return;
        }

        try {
            mType = jsonObject.getInt(VAR_TYPE);

            if (jsonObject.has(VAR_FILE)) {
                JSONObject fileObject = jsonObject.getJSONObject(VAR_FILE);
                Type fileType = new TypeToken<File>() {
                }.getType();
                Gson gson = new Gson();
                mFile = gson.fromJson(fileObject.toString(), fileType);
            }

            if (jsonObject.has(VAR_FILE_URI)) {
                String fileUri = jsonObject.getString(VAR_FILE_URI);
                if (!Utils.isNullOrEmpty(fileUri)) {
                    mFileUri = fileUri;
                }
            }

            if (jsonObject.has(VAR_FILE_NAME)) {
                String fileName = jsonObject.getString(VAR_FILE_NAME);
                if (!Utils.isNullOrEmpty(fileName)) {
                    mFileName = fileName;
                }
            }

            if (jsonObject.has(VAR_LAST_PAGE)) {
                mLastPage = jsonObject.getInt(VAR_LAST_PAGE);
            }

            if (jsonObject.has(VAR_PAGE_ROTATION)) {
                mPageRotation = jsonObject.getInt(VAR_PAGE_ROTATION);
            }

            if (jsonObject.has(VAR_PAGE_PRESENTATION_MODE)) {
                mPagePresentationMode = jsonObject.getInt(VAR_PAGE_PRESENTATION_MODE);
            }

            if (jsonObject.has(VAR_H_SCROLL_POS)) {
                mHScrollPos = jsonObject.getInt(VAR_H_SCROLL_POS);
            }

            if (jsonObject.has(VAR_V_SCROLL_POS)) {
                mVScrollPos = jsonObject.getInt(VAR_V_SCROLL_POS);
            }

            if (jsonObject.has(VAR_ZOOM)) {
                mZoom = jsonObject.getDouble(VAR_ZOOM);
            }

            if (jsonObject.has(VAR_IS_SECURED)) {
                mIsSecured = jsonObject.getBoolean(VAR_IS_SECURED);
            }

            if (jsonObject.has(VAR_IS_PACKAGE)) {
                mIsPackage = jsonObject.getBoolean(VAR_IS_PACKAGE);
            }

            if (jsonObject.has(VAR_IS_REFLOW_MODE)) {
                mIsReflowMode = jsonObject.getBoolean(VAR_IS_REFLOW_MODE);
            }

            if (jsonObject.has(VAR_REFLOW_TEXT_SIZE)) {
                mReflowTextSize = jsonObject.getInt(VAR_REFLOW_TEXT_SIZE);
            }

            if (jsonObject.has(VAR_IS_RTL_MODE)) {
                mIsRtlMode = jsonObject.getBoolean(VAR_IS_RTL_MODE);
            }

            if (jsonObject.has(VAR_IS_HIDDEN)) {
                mIsHidden = jsonObject.getBoolean(VAR_IS_HIDDEN);
            }

            if (jsonObject.has(VAR_BOOKMARK_DIALOG_CURRENT_TAB)) {
                mBookmarkDialogCurrentTab = jsonObject.getInt(VAR_BOOKMARK_DIALOG_CURRENT_TAB);
            }

            if (jsonObject.has(VAR_IS_HEADER)) {
                mIsHeader = jsonObject.getBoolean(VAR_IS_HEADER);
            }

            if (jsonObject.has(VAR_SECTION_FIRST_POS)) {
                mSectionFirstPos = jsonObject.getInt(VAR_SECTION_FIRST_POS);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e, "\nJson from: " + jsonObject);
        }
    }

    /**
     * Returns the file type.
     *
     * @return the file type
     */
    public int getType() {
        return (mType == FILE_TYPE_EXTERNAL_FOLDER ? FILE_TYPE_EXTERNAL : mType);
    }

    /**
     * @return the file
     */
    public File getFile() {
        return mFile;
    }

    /**
     * @return the absolute path
     */
    @NonNull
    public String getAbsolutePath() {
        if (mType == FILE_TYPE_EXTERNAL
            || mType == FILE_TYPE_EXTERNAL_FOLDER
            || mType == FILE_TYPE_EDIT_URI
            || mType == FILE_TYPE_OFFICE_URI) {
            return mFileUri == null ? "" : mFileUri;
        } else {
            try {
                return mFile == null ? "" : mFile.getAbsolutePath();
            } catch (Exception e) {
                sendException(mFile.getPath(), mType);
                return "";
            }
        }
    }

    /**
     * @return the parent directory path
     */
    public String getParentDirectoryPath() {
        if (mType == FILE_TYPE_EXTERNAL || mType == FILE_TYPE_EXTERNAL_FOLDER) {
            return "";
        } else {
            try {
                return (mFile != null && mFile.getParentFile() != null) ?
                    mFile.getParentFile().getAbsolutePath() : "";
            } catch (Exception e) {
                sendException(mFile.getPath(), mType);
                return "";
            }
        }
    }

    /**
     * @return the name of file
     */
    @NonNull
    public String getName() {
        if (mFileName == null) {
            mFileName = "";
        }
        if (mType == FILE_TYPE_EXTERNAL) {
            // Append file extension if necessary
            if (Utils.isNotPdf(mFileUri)) {
                String ext = Utils.getExtension(mFileUri);
                return mFileName.toLowerCase().endsWith("." + ext) ? mFileName : mFileName + "." + ext;
            } else {
                return (mFileName.toLowerCase().endsWith(".pdf") || Utils.isNotPdf(mFileName)) ? mFileName : mFileName + ".pdf";
            }
        } else if (mType == FILE_TYPE_EXTERNAL_FOLDER
            || mType == FILE_TYPE_EDIT_URI
            || mType == FILE_TYPE_OFFICE_URI) {
            return mFileName;
        } else {
            return mFile != null ? mFile.getName() : "";
        }
    }

    /**
     * @return the size of file which be readable by human.
     * If the file is not valid returns empty string.
     */
    public String getSizeInfo() {
        if (mFile != null) {
            // Return binary size of file
            return Utils.humanReadableByteCount(mFile.length(), false);
        } else {
            return "";
        }
    }

    @Override
    public long getSize() {
        return mFile.length();
    }

    @Override
    public String getIdentifier() {
        return getAbsolutePath();
    }

    /**
     * @return the number of files this file/folder contains
     */
    public int[] getFileCount() {
        int[] count = new int[2];
        if (null != mFile) {
            for (File file : mFile.listFiles()) {
                if (file.isDirectory()) {
                    count[1]++;
                } else {
                    count[0]++;
                }
            }
        }
        return count;
    }

    /**
     * @return Gets the number of bytes this file has
     */
    public String getBytes() {
        return mFile != null ?
            Utils.getByteCount(mFile.length()) : Utils.getLocaleDigits("0");
    }

    /**
     * @return the last modified date
     */
    public String getModifiedDate() {
        return mFile != null ?
            DateFormat.getInstance().format(new Date(mFile.lastModified())) : "";
    }

    /**
     * @return the last modified date
     */
    public Long getRawModifiedDate() {
        return mFile != null ?
            mFile.lastModified() : 0;
    }

    /**
     * Sets if the file is secured.
     *
     * @param value True if the file is secured
     */
    public void setIsSecured(boolean value) {
        mIsSecured = value;
    }

    /**
     * @return True if the file is secured
     */
    public boolean isSecured() {
        return mIsSecured;
    }

    /**
     * Sets if the file is a package.
     *
     * @param value True if the file is a package
     */
    public void setIsPackage(boolean value) {
        mIsPackage = value;
    }

    /**
     * @return True if the file is a package
     */
    public boolean isPackage() {
        return mIsPackage;
    }

    /**
     * @return the last page
     */
    public int getLastPage() {
        return mLastPage;
    }

    /**
     * Sets the last page.
     *
     * @param lastPage The last page
     */
    public void setLastPage(int lastPage) {
        mLastPage = lastPage;
    }

    /**
     * Sets the page rotation.
     *
     * @param rotation The page rotation
     */
    public void setPageRotation(int rotation) {
        mPageRotation = rotation;
    }

    /**
     * @return The page rotation
     */
    public int getPageRotation() {
        return mPageRotation;
    }

    /**
     * Sets the page presentation mode.
     *
     * @param pagePresentationMode The page presentation mode.
     */
    public void setPagePresentationMode(@Nullable PDFViewCtrl.PagePresentationMode pagePresentationMode) {
        if (pagePresentationMode != null) {
            mPagePresentationMode = pagePresentationMode.getValue();
        }
    }

    /**
     * @return The page presentation mode.
     */
    public PDFViewCtrl.PagePresentationMode getPagePresentationMode() {
        return PDFViewCtrl.PagePresentationMode.valueOf(mPagePresentationMode);
    }

    /**
     * Sets reflow mode.
     *
     * @param inReflowMode True if reflow mode is enabled, False otherwise
     */
    public void setReflowMode(boolean inReflowMode) {
        mIsReflowMode = inReflowMode;
    }

    /**
     * @return True if reflow mode is enabled
     */
    public boolean isReflowMode() {
        return mIsReflowMode;
    }

    /**
     * Sets the text size in reflow mode.
     *
     * @param textSize The text size ranging from 0 to 100
     */
    public void setReflowTextSize(int textSize) {
        mReflowTextSize = textSize;
    }

    /**
     * @return The text size ranging from 0 to 100
     */
    public int getReflowTextSize() {
        return mReflowTextSize;
    }

    /**
     * @return True if right-to-left mode is enabled
     */
    public boolean isRtlMode() {
        return mIsRtlMode;
    }

    /**
     * Sets right-to-left mode.
     *
     * @param isRtlMode True if right-to-left mode is enabled
     */
    public void setRtlMode(boolean isRtlMode) {
        mIsRtlMode = isRtlMode;
    }

    /**
     * @return The horizontal scroll position
     */
    public int getHScrollPos() {
        return mHScrollPos;
    }

    /**
     * Sets the horizontal scroll position.
     *
     * @param hScrollPos The horizontal scroll position
     */
    public void setHScrollPos(int hScrollPos) {
        mHScrollPos = hScrollPos;
    }

    /**
     * @return The vertical scroll position
     */
    public int getVScrollPos() {
        return mVScrollPos;
    }

    /**
     * Sets the horizontal scroll position.
     *
     * @param vScrollPos The vertical scroll position
     */
    public void setVScrollPos(int vScrollPos) {
        mVScrollPos = vScrollPos;
    }

    /**
     * @return The zoom level
     */
    public double getZoom() {
        return mZoom;
    }

    /**
     * Sets the zoom level.
     *
     * @param zoom The zoom level
     */
    public void setZoom(double zoom) {
        mZoom = zoom;
    }

    /**
     * @return The index of selected tab in bookmark dialog
     */
    public int getBookmarkDialogCurrentTab() {
        return mBookmarkDialogCurrentTab;
    }

    /**
     * Sets the index of selected tab in bookmark dialog.
     *
     * @param index The index of selected tab in bookmark dialog
     */
    public void setBookmarkDialogCurrentTab(int index) {
        mBookmarkDialogCurrentTab = index;
    }

    /**
     * @return True if this is a header
     */
    public boolean isHeader() {
        return mIsHeader;
    }

    /**
     * Sets if this is header.
     *
     * @param isHeader True if it is header
     */
    public void setHeader(boolean isHeader) {
        mIsHeader = isHeader;
    }

    /**
     * @return The first position in section
     */
    @SuppressWarnings("unused")
    public int getSectionFirstPos() {
        return mSectionFirstPos;
    }

    /**
     * Sets first position in section.
     *
     * @param sectionFirstPos The first position in section
     */
    public void setSectionFirstPos(int sectionFirstPos) {
        mSectionFirstPos = sectionFirstPos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || !(o instanceof FileInfo)) {
            return false;
        }
        FileInfo that = (FileInfo) o;
        try {
            return (getType() == that.getType() && getAbsolutePath().equalsIgnoreCase(that.getAbsolutePath()));
        } catch (Exception e) {
            sendException(that.mFile.getPath(), that.getType());
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + getAbsolutePath().hashCode();
        return hash;
    }

    /**
     * @return The file name
     * Same as {@link #getName()}.
     */
    public String getFileName() {
        return getName();
    }

    /**
     * @return the file extension
     */
    public String getExtension() {
        return Utils.getExtension(getName());
    }

    /**
     * Whether this file exists
     *
     * @return True if the file exists
     */
    @Override
    public boolean exists() {
        return mType != FILE_TYPE_FILE || (mFile != null && mFile.exists());
    }

    /**
     * Gets file type
     *
     * @return The file type
     */
    @Override
    public int getFileType() {
        return mType;
    }

    /**
     * @return True if this is a directory
     */
    @Override
    public boolean isDirectory() {
        return mType == BaseFileInfo.FILE_TYPE_FOLDER;
    }

    /**
     * @return True if this is hidden
     */
    @Override
    public boolean isHidden() {
        return mIsHidden;
    }

    /**
     * Sets if this is hidden
     *
     * @param isHidden True if hidden
     */
    @Override
    public void setHidden(boolean isHidden) {
        mIsHidden = isHidden;
    }

    private void sendException(
        String path,
        int type) {

        if (path == null) {
            path = "null";
        }
        AnalyticsHandlerAdapter.getInstance().sendException(
            new Exception("filepath:" + path + ", type:" + type));

    }
}
