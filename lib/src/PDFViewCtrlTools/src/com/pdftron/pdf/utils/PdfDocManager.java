//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

/**
 * Constants for specifying state of documents
 */
public class PdfDocManager {

    /**
     * The document is error free.
     */
    public static final int DOCUMENT_SETDOC_ERROR_NONE = 0;
    /**
     * The document is not linked to any {@link com.pdftron.pdf.PDFDoc}
     */
    public static final int DOCUMENT_SETDOC_ERROR_NULL_PDFDOC = 1;
    /**
     * The document is corrupted.
     */
    public static final int DOCUMENT_SETDOC_ERROR_CORRUPTED = 2;
    /**
     * The document has not pages.
     */
    public static final int DOCUMENT_SETDOC_ERROR_ZERO_PAGE = 3;
    /**
     * The document has been cancelled during opening from URL.
     */
    public static final int DOCUMENT_SETDOC_ERROR_OPENURL_CANCELLED = 4;
    /**
     * The document has given wrong password.
     */
    public static final int DOCUMENT_SETDOC_ERROR_WRONG_PASSWORD = 6;
    /**
     * The document doesn't exist.
     */
    public static final int DOCUMENT_SETDOC_ERROR_NOT_EXIST = 7;
    /**
     * The document has been cancelled during download.
     */
    public static final int DOCUMENT_SETDOC_ERROR_DOWNLOAD_CANCEL = 9;

    /**
     * The document is clean.
     */
    public static final int DOCUMENT_STATE_CLEAN = 0;
    /**
     * The document is normal after saving changes.
     */
    public static final int DOCUMENT_STATE_NORMAL = 1;
    /**
     * The document has been modified but not saved.
     */
    public static final int DOCUMENT_STATE_MODIFIED = 2;
    /**
     * The document is corrupted.
     */
    public static final int DOCUMENT_STATE_CORRUPTED = 3;
    /**
     * The document is corrupted and modified.
     */
    public static final int DOCUMENT_STATE_CORRUPTED_AND_MODIFIED = 4;
    /**
     * The document is ready only.
     */
    public static final int DOCUMENT_STATE_READ_ONLY = 5;
    /**
     * The document is read only and but has been modified.
     */
    public static final int DOCUMENT_STATE_READ_ONLY_AND_MODIFIED = 6;
    /**
     * The document couldn't be saved.
     */
    public static final int DOCUMENT_STATE_COULD_NOT_SAVE = 7;
    /**
     * The document is in the middle of conversion.
     */
    public static final int DOCUMENT_STATE_DURING_CONVERSION = 8;
    /**
     * The document is obtained from conversion.
     */
    public static final int DOCUMENT_STATE_FROM_CONVERSION = 9;
    /**
     * The device is out of storage space.
     */
    public static final int DOCUMENT_STATE_OUT_OF_SPACE = 10;

}
