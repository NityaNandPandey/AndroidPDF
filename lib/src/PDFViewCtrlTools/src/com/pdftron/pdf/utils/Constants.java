//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

/**
 * A utility classes that contains constant values
 */
@SuppressWarnings("SpellCheckingInspection")
public class Constants {

    /**
     * The file is a PDF
     */
    public static final int FILE_TYPE_PDF = 0;
    /**
     * The file is a document
     */
    public static final int FILE_TYPE_DOC = 1;
    /**
     * The file is an image
     */
    public static final int FILE_TYPE_IMAGE = 2;

    /**
     * The file extension is pdf
     */
    public static final String[] FILE_NAME_EXTENSIONS_PDF = {"pdf"};
    /**
     * The file extension is document
     */
    public static final String[] FILE_NAME_EXTENSIONS_DOC = {"docx", "doc", "pptx", "xlsx", "md"};
    /**
     * The file extension is image
     */
    public static final String[] FILE_NAME_EXTENSIONS_IMAGE = {"jpeg", "jpg", "gif", "png", "bmp", "cbz"};
    /**
     * The file extension is valid
     */
    public static final String[] FILE_NAME_EXTENSIONS_VALID = {"pdf", "docx", "doc", "pptx", "xlsx",
        "jpeg", "jpg", "gif", "png", "bmp", "cbz", "md"};

    /**
     * The file extension is not PDF
     */
    public static final String[] ALL_NONPDF_FILETYPES_WILDCARD = {"*.docx", "*.doc", "*.pptx", "*.xlsx",
        "*.jpeg", "*.jpg", "*.gif", "*.png", "*.bmp", "*.cbz", "*.md"};

    /**
     * All supported file meme types
     */
    public static final String[] ALL_FILE_MIME_TYPES = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/msword", "image/jpeg", "image/gif", "image/png", "image/bmp", "application/x-cbr", "text/markdown"};
    /**
     * Office file meme types
     */
    public static final String[] OFFICE_FILE_MIME_TYPES = {"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation", "application/msword", "text/markdown",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"};
    /**
     * Image file meme types
     */
    public static final String[] IMAGE_FILE_MIME_TYPES = {"image/jpeg", "image/gif", "image/png", "image/bmp", "application/x-cbr"};

    /**
     * Google docs file meme types
     */
    public static final String[] ALL_GOOGLE_DOCS_TYPES = {"application/vnd.google-apps.document",
        "application/vnd.google-apps.drawing", "application/vnd.google-apps.presentation",
        "application/vnd.google-apps.spreadsheet"};

}
