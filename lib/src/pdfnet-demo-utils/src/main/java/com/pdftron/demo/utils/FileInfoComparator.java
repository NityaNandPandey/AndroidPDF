//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.utils;

import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;

import java.util.Comparator;

public class FileInfoComparator {

    private static Comparator<ExternalFileInfo> EXTERNAL_PATH_ORDER;
    private static Comparator<ExternalFileInfo> EXTERNAL_DATE_ORDER;
    private static Comparator<FileInfo> FOLDER_PATH_ORDER;
    private static Comparator<FileInfo> FOLDER_DATE_ORDER;
    private static Comparator<FileInfo> ABSOLUTE_PATH_ORDER;
    private static Comparator<FileInfo> FILE_NAME_ORDER;
    private static Comparator<FileInfo> MODIFIED_DATE_ORDER;
    private static Comparator<FileInfo> MODIFIED_DATE_ORDER_ONLY;

    /**
     * Sort by alphabetical order, folders first.
     */
    public static Comparator<ExternalFileInfo> externalPathOrder(
    ) {

        if (EXTERNAL_PATH_ORDER == null) {
            EXTERNAL_PATH_ORDER = new Comparator<ExternalFileInfo>() {
                @Override
                public int compare(ExternalFileInfo lhs, ExternalFileInfo rhs) {
                    if (lhs.isDirectory() && !rhs.isDirectory()) {
                        return -1;
                    } else if (!lhs.isDirectory() && rhs.isDirectory()) {
                        return 1;
                    } else {
                        return lhs.getFileName().compareToIgnoreCase(rhs.getFileName());
                    }
                }
            };
        }
        return EXTERNAL_PATH_ORDER;

    }

    /**
     * Sort by alphabetical order, folders first.
     */
    public static Comparator<ExternalFileInfo> externalDateOrder(
    ) {

        if (EXTERNAL_DATE_ORDER == null) {
            EXTERNAL_DATE_ORDER = new Comparator<ExternalFileInfo>() {
                @Override
                public int compare(ExternalFileInfo lhs, ExternalFileInfo rhs) {
                    if (lhs.isDirectory() && !rhs.isDirectory()) {
                        return -1;
                    } else if (!lhs.isDirectory() && rhs.isDirectory()) {
                        return 1;
                    } else {
                        return rhs.getRawModifiedDate().compareTo(lhs.getRawModifiedDate());
                    }
                }
            };
        }
        return EXTERNAL_DATE_ORDER;

    }


    /**
     * Sort by absolute path asc
     */
    public static Comparator<FileInfo> folderPathOrder(
    ) {

        if (FOLDER_PATH_ORDER == null) {
            FOLDER_PATH_ORDER = new Comparator<FileInfo>() {
                @Override
                public int compare(FileInfo lhs, FileInfo rhs) {
                    if (lhs.getType() == rhs.getType()) {
                        return lhs.getAbsolutePath().compareToIgnoreCase(rhs.getAbsolutePath());
                    } else {
                        return lhs.getType() <= rhs.getType() ? -1 : 1;
                    }
                }
            };
        }
        return FOLDER_PATH_ORDER;

    }

    /**
     * Sort by absolute path asc
     */
    public static Comparator<FileInfo> folderDateOrder(
    ) {

        if (FOLDER_DATE_ORDER == null) {
            FOLDER_DATE_ORDER = new Comparator<FileInfo>() {
                @Override
                public int compare(FileInfo lhs, FileInfo rhs) {
                    if (lhs.getType() == rhs.getType()) {
                        return rhs.getRawModifiedDate().compareTo(lhs.getRawModifiedDate());
                    } else {
                        return lhs.getType() <= rhs.getType() ? -1 : 1;
                    }
                }
            };
        }
        return FOLDER_DATE_ORDER;

    }

    /**
     * Sorts by absolute file path asc (a to z, within each directory)
     */
    public static Comparator<FileInfo> absolutePathOrder(
    ) {

        if (ABSOLUTE_PATH_ORDER == null) {
            ABSOLUTE_PATH_ORDER = new Comparator<FileInfo>() {
                @Override
                public int compare(FileInfo lhs, FileInfo rhs) {
                    int parentPathCmp = lhs.getParentDirectoryPath().compareToIgnoreCase(rhs.getParentDirectoryPath());
                    if (parentPathCmp != 0) {
                        return parentPathCmp;
                    }

                    return lhs.getAbsolutePath().compareToIgnoreCase(rhs.getAbsolutePath());
                }
            };
        }
        return ABSOLUTE_PATH_ORDER;

    }

    /**
     * Sorts by filename asc (a to z)
     */
    public static Comparator<FileInfo> fileNameOrder(
    ) {

        if (FILE_NAME_ORDER == null) {
            FILE_NAME_ORDER = new Comparator<FileInfo>() {
                @Override
                public int compare(FileInfo lhs, FileInfo rhs) {
                    return lhs.getName().compareToIgnoreCase(rhs.getName());
                }
            };
        }
        return FILE_NAME_ORDER;

    }

    /**
     * Sorts by modified date desc (newest first)
     */
    public static Comparator<FileInfo> modifiedDateOrder(
    ) {

        if (MODIFIED_DATE_ORDER == null) {
            MODIFIED_DATE_ORDER = new Comparator<FileInfo>() {
                @Override
                public int compare(FileInfo lhs, FileInfo rhs) {
                    int parentPathCmp = lhs.getParentDirectoryPath().compareToIgnoreCase(rhs.getParentDirectoryPath());
                    if (parentPathCmp != 0) {
                        return parentPathCmp;
                    }
                    return rhs.getRawModifiedDate().compareTo(lhs.getRawModifiedDate());
                }
            };
        }
        return MODIFIED_DATE_ORDER;

    }

    /**
     * Sorts by modified date desc (newest first)
     */
    public static Comparator<FileInfo> modifiedDateOrderOnly(
    ) {

        if (MODIFIED_DATE_ORDER_ONLY == null) {
            MODIFIED_DATE_ORDER_ONLY = new Comparator<FileInfo>() {
                @Override
                public int compare(FileInfo lhs, FileInfo rhs) {
                    return rhs.getRawModifiedDate().compareTo(lhs.getRawModifiedDate());
                }
            };
        }
        return MODIFIED_DATE_ORDER_ONLY;

    }

}
