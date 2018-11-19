//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.model;

/**
 * Interface class for file
 */
public interface BaseFileInfo {

    /**
     * The file type is unknown.
     */
    int FILE_TYPE_UNKNOWN = -1;
    /**
     * The file is a folder.
     */
    int FILE_TYPE_FOLDER = 1;
    /**
     * The file is a regular file.
     */
    int FILE_TYPE_FILE = 2;
    /**
     * The file is opened from a URL.
     */
    int FILE_TYPE_OPEN_URL = 5;
    /**
     * The file is regular file but is stored in external storage (needs particular permission).
     */
    int FILE_TYPE_EXTERNAL = 6;
    /**
     * The file is the root folder and is stored in external storage.
     */
    int FILE_TYPE_EXTERNAL_ROOT = 7;
    /**
     * The file is a folder and is stored in external storage.
     */
    int FILE_TYPE_EXTERNAL_FOLDER = 9;
    /**
     * The file is opened from URI and have edit permission.
     */
    int FILE_TYPE_EDIT_URI = 13;
    /**
     * The file is an office URI.
     */
    int FILE_TYPE_OFFICE_URI = 15;

    /**
     * @return The file type
     */
    int getFileType();

    /**
     * @return The absolute path
     */
    String getAbsolutePath();

    /**
     * @return The file name
     */
    String getFileName();

    /**
     * @return The size info
     */
    String getSizeInfo();

    /**
     * @return The size
     */
    long getSize();

    /**
     * @return The UUID
     */
    String getIdentifier();

    /**
     * @return The modified date
     */
    String getModifiedDate();

    /**
     * @return True if the file exists
     */
    boolean exists();

    /**
     * @return True if the file is directory
     */
    boolean isDirectory();

    /**
     * @return True if the file is hidden
     */
    boolean isHidden();

    /**
     * Specifies if the file is hidden.
     *
     * @param hidden True if the file is hidden
     */
    void setHidden(boolean hidden);

    /**
     * Specifies if the file is secured.
     *
     * @param value True if the file is secured
     */
    void setIsSecured(boolean value);

    /**
     * @return True if the file is secured
     */
    boolean isSecured();

    /**
     * Specifies if the file is a package.
     *
     * @param value True if the file is a package
     */
    void setIsPackage(boolean value);

    /**
     * @return True if the file is a package
     */
    boolean isPackage();
}
