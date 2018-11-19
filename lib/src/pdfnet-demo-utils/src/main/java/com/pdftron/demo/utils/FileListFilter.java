//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.utils;

import android.content.Context;
import android.widget.Filter;

import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.utils.Constants;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;

/** @hide */
public class FileListFilter<FileInfo extends BaseFileInfo> extends Filter {

    public interface FilterPublishListener<FileInfo> {
        /**
         * Raised when a filtering step is completed.
         *
         * @param result the list resulting from the filtering
         * @param resultCode FILTER_RESULT_SUCCESS if result contains at least 1 item.
         *                   Otherwise returns the reason why it's empty
         */
        void onFilterResultsPublished(ArrayList<FileInfo> result, int resultCode);
    }

    @SuppressWarnings("WeakerAccess")
    public static final int FILTER_RESULT_SUCCESS = 0;
    public static final int FILTER_RESULT_EMPTY_ORIGINAL_LIST = 1;
    public static final int FILTER_RESULT_NO_STRING_MATCH = 2;
    public static final int FILTER_RESULT_NO_ITEMS_OF_SELECTED_FILE_TYPES = 3;
    public static final int FILTER_RESULT_FAILURE = 4;

    private ArrayList<FileInfo> mOriginalFileList;
    private FilterPublishListener mFilterPublishListener;
    final private Object mOriginalFileListLock;

    // file extensions
    final private Object mExtensionLock;
    private String[] mPDFExtensions;
    private String[] mDocExtensions;
    private String[] mImageExtensions;

    public FileListFilter(ArrayList<FileInfo> fileList,
                          FilterPublishListener listener,
                          Object fileListLock) {
        mOriginalFileList = fileList;
        mFilterPublishListener = listener;
        if (fileListLock == null) {
            mOriginalFileListLock = new Object();
        } else {
            mOriginalFileListLock = fileListLock;
        }
        mExtensionLock = new Object();
    }

    /**
     * Makes the filter show or hide all files of fileType based on enabled.
     *
     * @param fileType The file type
     * @param enabled True if enabled
     */
    public void setFileTypeEnabledInFilter(int fileType, boolean enabled) {
        if (enabled) {
            enableFileTypeInFilter(fileType);
        } else {
            disableFileTypeInFilter(fileType);
        }
    }

    public void setFileTypeEnabledInFilterFromSettings(Context context, String settingsSuffix) {
        setFileTypeEnabledInFilter(Constants.FILE_TYPE_PDF, PdfViewCtrlSettingsManager.getFileFilter(context, Constants.FILE_TYPE_PDF, settingsSuffix));
        setFileTypeEnabledInFilter(Constants.FILE_TYPE_DOC, PdfViewCtrlSettingsManager.getFileFilter(context, Constants.FILE_TYPE_DOC, settingsSuffix));
        setFileTypeEnabledInFilter(Constants.FILE_TYPE_IMAGE, PdfViewCtrlSettingsManager.getFileFilter(context, Constants.FILE_TYPE_IMAGE, settingsSuffix));
    }

    /**
     * Makes the filter show all files of fileType
     * @param fileType The file type
     */
    private void enableFileTypeInFilter(int fileType) {
        synchronized (mExtensionLock) {
            switch (fileType) {
                case Constants.FILE_TYPE_PDF:
                    mPDFExtensions = Constants.FILE_NAME_EXTENSIONS_PDF;
                    break;
                case Constants.FILE_TYPE_DOC:
                    mDocExtensions = Constants.FILE_NAME_EXTENSIONS_DOC;
                    break;
                case Constants.FILE_TYPE_IMAGE:
                    mImageExtensions = Constants.FILE_NAME_EXTENSIONS_IMAGE;
                    break;
            }
        }
    }

    /**
     * Makes the filter hide all files of fileType
     * @param fileType The file type
     */
    private void disableFileTypeInFilter(int fileType) {
        synchronized (mExtensionLock) {
            switch (fileType) {
                case Constants.FILE_TYPE_PDF:
                    mPDFExtensions = null;
                    break;
                case Constants.FILE_TYPE_DOC:
                    mDocExtensions = null;
                    break;
                case Constants.FILE_TYPE_IMAGE:
                    mImageExtensions = null;
                    break;
            }
        }
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {

        FilterResults results = new FilterResults();

        if (mOriginalFileList == null) {
            results.count = 0;
        }
        else {
            ArrayList<FileInfo> newValues = new ArrayList<>();
            synchronized (mOriginalFileListLock) {
                final String prefixString;
                if (constraint == null || constraint.length() == 0) {
                    prefixString = null;
                } else {
                    prefixString = constraint.toString().toLowerCase();
                }
                for (FileInfo item : mOriginalFileList) {
                    if (prefixString == null) {
                        if (!item.isHidden()) {
                            newValues.add(item);
                        }
                    } else if (item.getFileName().toLowerCase().contains(prefixString)) {
                        // if searching for a constraint then doesn't matter if it is hidden or not
                        newValues.add(item);
                    }
                }
            }
            synchronized (mExtensionLock) {
                boolean filterByFileType = !(mPDFExtensions == null && mDocExtensions == null && mImageExtensions == null); // at least one type selected
                if (filterByFileType) {
                    ArrayList<FileInfo> filteredValues = new ArrayList<>();
                    for (FileInfo item : newValues) {
                        String fileName = item.getFileName();
                        if (item.isDirectory()) {
                            filteredValues.add(item);
                        } else if (mPDFExtensions != null && FilenameUtils.isExtension(fileName.toLowerCase(), mPDFExtensions)) {
                            filteredValues.add(item);
                        } else if (mDocExtensions != null && FilenameUtils.isExtension(fileName.toLowerCase(), mDocExtensions)) {
                            filteredValues.add(item);
                        } else if (mImageExtensions != null && FilenameUtils.isExtension(fileName.toLowerCase(), mImageExtensions)) {
                            filteredValues.add(item);
                        }
                    }
                    newValues = filteredValues;
                }
            }

            results.values = newValues;
            results.count = newValues.size();

        }

        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void publishResults(CharSequence charSequence, FilterResults filterResults) {

        ArrayList<FileInfo> filteredList = (ArrayList<FileInfo>) filterResults.values;

        if (filteredList == null) {
            filteredList = new ArrayList<>();
        }

        int resultCode = FILTER_RESULT_SUCCESS;
        if (filteredList.size() == 0) {
            synchronized (mOriginalFileListLock) {
                if (mOriginalFileList.size() == 0) {
                    resultCode = FILTER_RESULT_EMPTY_ORIGINAL_LIST;
                } else if (charSequence.length() > 0) {
                    resultCode = FILTER_RESULT_NO_STRING_MATCH;
                } else {
                    resultCode = FILTER_RESULT_NO_ITEMS_OF_SELECTED_FILE_TYPES;
                }
            }
        }
        if (mFilterPublishListener != null) {
            mFilterPublishListener.onFilterResultsPublished(filteredList, resultCode);
        }
    }
}
