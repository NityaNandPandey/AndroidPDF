//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pdftron.demo.R;
import com.pdftron.demo.navigation.callbacks.FileManagementListener;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.Convert;
import com.pdftron.pdf.DocumentConversion;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.Constants;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.PdfViewCtrlTabsManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.SDFDoc;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

/**
 * @hide
 */
public class FileManager {
    private static final String TAG = FileManager.class.getName();
    public static final String CACHE_FILE_LIST_OBJECT_OLD = "cache_fileinfo_map";
    public static final String CACHE_FILE_LIST_OBJECT = "cache_fileinfo_map_v2";

    public static final int MAX_NUM_DUPLICATED_FILES = 100;
    public static final int SHOW_PROGRESS_DIALOG_DELAY = 500;

    public static boolean checkIfFileTypeIsInList(String filePath) {
        String ext = Utils.getExtension(filePath);
        if (!ext.isEmpty()) {
            ext = "*." + ext;
            for (String tp : Constants.ALL_NONPDF_FILETYPES_WILDCARD) {
                if (ext.equalsIgnoreCase(tp)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isValidFile(@Nullable String filePath) {
        if (filePath == null) {
            return false;
        }

        File file = new File(filePath);
        for (String fileType : Constants.FILE_NAME_EXTENSIONS_VALID) {
            if (FilenameUtils.wildcardMatch(file.getName(), "*." + fileType, IOCase.INSENSITIVE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This utility class can be used with File.listFiles() so it will
     * filter only PDF files and directories.
     */
    public static class FilterValidFilesAndDirs implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname != null && !pathname.isHidden() && (pathname.isDirectory() || isValidFile(pathname.getAbsolutePath()));
        }
    }

    public static class FilterValidFiles implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname != null && !pathname.isHidden() && isValidFile(pathname.getAbsolutePath());
        }
    }


    /**
     * This utility class can be used with File.listFiles() so it will
     * filter directories only.
     */
    public static class FilterDirsOnly implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname != null && !pathname.isHidden() && pathname.isDirectory();
        }
    }

    public static void merge(Context context, ArrayList<FileInfo> filesToMerge, ArrayList<FileInfo> filesToDelete,
                             final FileInfo targetFile, final FileManagementListener listener) {
        new FileManager.MergeFileTask(context, filesToMerge, filesToDelete, targetFile, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    static class MergeFileTask extends CustomAsyncTask<Void, Integer, Void> {

        private ArrayList<FileInfo> mFiles;
        private ArrayList<FileInfo> mTempFiles;

        private FileInfo mTargetFile;
        private FileManagementListener mListener;
        private ProgressDialog mProgressDialog;
        private Boolean mSuccess;
        private final Handler mHandler = new Handler();

        final private Object mSetupProgressLock;

        private static final int SETUP_PROGRESS_FOR_MERGE = 1;

        MergeFileTask(Context context, ArrayList<FileInfo> filesToMerge, ArrayList<FileInfo> filesToDelete,
                      FileInfo targetFile, FileManagementListener listener) {
            super(context);
            mFiles = filesToMerge;
            mTempFiles = filesToDelete;
            mTargetFile = targetFile;
            mListener = listener;
            mSuccess = true;
            mSetupProgressLock = new Object();
        }

        private void setMergeProgressDialog() {
            Context context = getContext();
            if (context == null) {
                return;
            }
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setTitle("");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setMessage(context.getResources().getString(R.string.merging_wait));
            if (!mProgressDialog.isShowing()) {
                // Delay showing progress dialog, so it does not flash for short tasks
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.show();
                    }
                }, SHOW_PROGRESS_DIALOG_DELAY);
            }
        }

        @Override
        protected void onPreExecute() {
            Context context = getContext();
            if (context == null) {
                return;
            }
            setMergeProgressDialog();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (mTargetFile == null) {
                mSuccess = false;
                return null;
            }
            PDFDoc mergedDoc = null;
            PDFDoc inDoc = null;
            boolean shouldUnlockRead = false;
            SecondaryFileFilter filter = null;
            InputStream inputStream = null;
            try {
                mergedDoc = new PDFDoc();
                mergedDoc.initSecurityHandler();

                for (int i = 0; i < mFiles.size(); i++) {
                    // Add pages to end of merged doc
                    FileInfo fileInfo = mFiles.get(i);
                    int infoType = fileInfo.getType();
                    ContentResolver cr = Utils.getContentResolver(getContext());
                    if (cr == null) {
                        return null;
                    }
                    switch (infoType) {
                        case BaseFileInfo.FILE_TYPE_FILE:
                            if (Utils.isNotPdf(fileInfo.getAbsolutePath())) {
                                DocumentConversion conv = Convert.universalConversion(fileInfo.getAbsolutePath(), null);
                                conv.convert();
                                if (conv.getDoc() == null) {
                                    mSuccess = false;
                                    return null;
                                }
                                inDoc = conv.getDoc();
                            } else {
                                inDoc = new PDFDoc(fileInfo.getAbsolutePath());
                            }
                            break;
                        case BaseFileInfo.FILE_TYPE_EXTERNAL:
                        case BaseFileInfo.FILE_TYPE_OFFICE_URI:
                            Uri uri = Uri.parse(fileInfo.getAbsolutePath());
                            filter = new SecondaryFileFilter(getContext(), uri);

                            if (Utils.isNotPdf(cr, uri)) {
                                DocumentConversion conv = Convert.universalConversion(filter, null);
                                conv.convert();
                                if (conv.getDoc() == null) {
                                    mSuccess = false;
                                    return null;
                                }
                                inDoc = conv.getDoc();
                            } else {
                                inDoc = new PDFDoc(filter);
                            }
                            break;
                        case BaseFileInfo.FILE_TYPE_EDIT_URI:
                            inputStream = cr.openInputStream(Uri.parse(fileInfo.getAbsolutePath()));
                            if (inputStream == null) {
                                mSuccess = false;
                                return null;
                            }
                            inDoc = new PDFDoc(inputStream);
                            break;
                        default:
                            continue;
                    }
                    inDoc.lockRead();
                    shouldUnlockRead = true;

                    Page[] copyPages = new Page[inDoc.getPageCount()];
                    PageIterator iterator = inDoc.getPageIterator();
                    int j = 0;
                    while (iterator.hasNext()) {
                        Page page = iterator.next();
                        copyPages[j++] = page;
                    }

                    Page[] importedPages = mergedDoc.importPages(copyPages, true);

                    // Add imported pages to the merged doc's page sequence
                    for (Page page : importedPages) {
                        mergedDoc.pagePushBack(page);
                    }

                    inDoc.unlockRead();
                    shouldUnlockRead = false;
                    Utils.closeQuietly(inDoc);
                    Utils.closeQuietly(filter);
                    inDoc = null;
                    filter = null;
                }
                if (mTargetFile.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                    mergedDoc.save(mTargetFile.getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null);
                } else if (mTargetFile.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                    ContentResolver cr = Utils.getContentResolver(getContext());
                    if (cr == null) {
                        return null;
                    }
                    filter = new SecondaryFileFilter(getContext(), Uri.parse(mTargetFile.getAbsolutePath()));
                    mergedDoc.save(filter, SDFDoc.SaveMode.REMOVE_UNUSED);
                } else {
                    mSuccess = false;
                    return null;
                }

                mSuccess = true;
            } catch (Exception e) {
                mSuccess = false;
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(inDoc);
                }
                Utils.closeQuietly(inDoc);
                Utils.closeQuietly(mergedDoc);
                Utils.closeQuietly(filter);
                Utils.closeQuietly(inputStream);
            }
            return null;
        }

        @Override
        public void onProgressUpdate(Integer... val) {
            super.onProgressUpdate(val);

            if (getContext() == null) {
                return;
            }

            if (val[0] == SETUP_PROGRESS_FOR_MERGE) {
                setMergeProgressDialog();
                synchronized (mSetupProgressLock) {
                    mSetupProgressLock.notify();
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            Context context = getContext();
            if (context == null) {
                return;
            }
            mHandler.removeCallbacksAndMessages(null);
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (mSuccess) {
                mListener.onFileMerged(mFiles, mTempFiles, mTargetFile);
            } else {
                Utils.safeShowAlertDialog(context,
                    context.getResources().getString(R.string.dialog_merge_error_message_general),
                    context.getResources().getString(R.string.error));
            }
        }
    }

    public static void rename(Context context, final File file, final FileManagementListener listener) {
        if (file == null) {
            return;
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return;
        }
        View renameDialog = inflater.inflate(R.layout.dialog_rename_file, null);
        String title;

        if (file.exists() && file.isDirectory()) {
            title = context.getResources().getString(R.string.dialog_rename_folder_dialog_title);
        } else {
            title = context.getResources().getString(R.string.dialog_rename_title);
        }

        final EditText renameEditText = renameDialog.findViewById(R.id.dialog_rename_file_edit);
        renameEditText.setText(file.getName());
        // Show the edit text with name of file selected.
        if (file.isDirectory()) {
            renameEditText.setSelection(0, file.getName().length());
            renameEditText.setHint(context.getResources().getString(R.string.dialog_rename_folder_hint));
        } else {
            int index = FilenameUtils.indexOfExtension(file.getName());
            if (index == -1) {
                index = file.getName().length();
            }
            renameEditText.setSelection(0, index);
            renameEditText.setHint(context.getResources().getString(R.string.dialog_rename_file_hint));
        }

        final WeakReference<Context> contextRef = new WeakReference<>(context);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(renameDialog)
            .setTitle(title)
            .setPositiveButton(R.string.ok, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Context context = contextRef.get();
                    if (context == null) {
                        return;
                    }

                    Boolean doAction = true;
                    String message = "";
                    String newFileName = file.getName();

                    //noinspection ConstantConditions
                    do {
                        // Check whether the file name is empty
                        if (renameEditText.getText().toString().trim().length() == 0) {
                            doAction = false;
                            message = file.isDirectory() ?
                                context.getResources().getString(R.string.dialog_rename_invalid_folder_name_message) :
                                context.getResources().getString(R.string.dialog_rename_invalid_file_name_message);
                            continue;
                        }

                        // Check if it is a file or directory, and add suffix if necessary.
                        // If they are the same file, just dismiss the dialog.
                        newFileName = renameEditText.getText().toString().trim();
                        // get extension of original file
                        String extension = Utils.getExtension(file.getAbsolutePath());
                        if (Utils.isNullOrEmpty(extension)) {
                            // no extension, let's try to open it as PDF
                            extension = "pdf";
                        }
                        if (file.isFile() && !newFileName.toLowerCase().endsWith("." + extension.toLowerCase())) {
                            newFileName = newFileName + "." + extension;
                        }
                        File newFile = new File(file.getParentFile(), newFileName);
                        if (newFile.equals(file)) {
                            doAction = false;
                            message = "";
                            continue;
                        }

                        // Check if the new file name already exists
                        if (newFile.exists()) {
                            doAction = false;
                            message = file.isDirectory() ?
                                context.getResources().getString(R.string.dialog_create_folder_invalid_folder_name_already_exists_message) :
                                context.getResources().getString(R.string.dialog_rename_invalid_file_name_already_exists_message);
                        }
                    } while (false);

                    if (doAction) {
                        Boolean success;
                        File newFile = null;
                        FileInfo fileInfoOldFile;
                        if (file.isDirectory()) {
                            fileInfoOldFile = new FileInfo(BaseFileInfo.FILE_TYPE_FOLDER, file);
                        } else {
                            fileInfoOldFile = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file);
                        }
                        try {
                            newFile = new File(file.getParentFile(), newFileName);
                            success = file.renameTo(newFile);
                        } catch (Exception e) {
                            success = false;
                        }
                        if (!success) {
                            Utils.safeShowAlertDialog(context,
                                context.getResources().getString(R.string.dialog_rename_invalid_file_name_error),
                                context.getResources().getString(R.string.alert));
                        } else {
                            FileInfo fileInfoNewFile = new FileInfo(fileInfoOldFile.getType(), newFile);
                            if (listener != null) {
                                listener.onFileRenamed(fileInfoOldFile, fileInfoNewFile);
                            }
                        }
                    } else {
                        if (message.length() > 0) {
                            Utils.safeShowAlertDialog(context, message,
                                context.getResources().getString(R.string.alert));
                        }
                    }
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                if (renameEditText.length() > 0) {
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                } else {
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });

        renameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    // Enable "Ok" button
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                } else {
                    // Disable "Ok" button
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Show keyboard automatically when the dialog is shown.
        renameEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus && dialog.getWindow() != null) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }

            }
        });

        dialog.show();
    }

    public static void createFolder(Context context, final File rootFolder, final FileManagementListener listener) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return;
        }
        View createFolderDialog = inflater.inflate(R.layout.dialog_create_folder, null);
        String title;

        title = context.getResources().getString(R.string.dialog_create_title);

        final EditText folderNameEditText = createFolderDialog.findViewById(R.id.dialog_create_folder_edit);

        final WeakReference<Context> contextRef = new WeakReference<>(context);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(createFolderDialog)
            .setTitle(title)
            .setPositiveButton(R.string.ok, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Context context = contextRef.get();
                    if (context == null) {
                        return;
                    }

                    Boolean doAction = true;
                    String message = "";
                    String newFolderName = "";

                    //noinspection ConstantConditions
                    do {
                        // Check whether the folder name is empty
                        if (folderNameEditText.getText().toString().trim().length() == 0) {
                            doAction = false;
                            message = context.getResources().getString(R.string.dialog_create_folder_invalid_folder_name_message);
                            continue;
                        }

                        newFolderName = folderNameEditText.getText().toString().trim();

                        // Check if filename is OK (only upper and lowercase letters, numbers, and underscores)
                        // and 200 is the max char limit.
                        // TODO:why do we need this check?
//                        if (!newFolderName.matches("\\w{1,200}?")) {
//                            doAction = false;
//                            message = context.getResources().getString(R.string.dialog_create_folder_invalid_folder_name_message);
//                            continue;
//                        }

                        // Check if the new folder name already exists
                        File newFolder = new File(rootFolder, newFolderName);
                        if (newFolder.isDirectory()) {
                            doAction = false;
                            message = context.getResources().getString(R.string.dialog_create_folder_invalid_folder_name_already_exists_message);
                        }
                    } while (false);

                    if (doAction) {
                        Boolean success;
                        File newFolder = null;
                        FileInfo fileInfoRootFolder = new FileInfo(BaseFileInfo.FILE_TYPE_FOLDER, rootFolder);
                        try {
                            newFolder = new File(rootFolder, newFolderName);
                            success = newFolder.mkdir();
                        } catch (Exception e) {
                            success = false;
                        }
                        if (!success) {
                            Utils.safeShowAlertDialog(context,
                                context.getResources().getString(R.string.dialog_create_folder_invalid_folder_name_error_message),
                                context.getResources().getString(R.string.alert));
                        } else {
                            FileInfo fileInfoNewFolder = new FileInfo(BaseFileInfo.FILE_TYPE_FOLDER, newFolder);
                            if (listener != null) {
                                listener.onFolderCreated(fileInfoRootFolder, fileInfoNewFolder);
                            }
                        }
                    } else {
                        if (message.length() > 0) {
                            Utils.safeShowAlertDialog(context, message,
                                context.getResources().getString(R.string.alert));
                        }
                    }
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Utils.hideSoftKeyboard(folderNameEditText.getContext(), folderNameEditText);
                    dialog.cancel();
                }
            });

        final AlertDialog dialog = builder.create();

        folderNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    // Enable "Ok" button
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                } else {
                    // Disable "Ok" button
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Show keyboard automatically when the dialog is shown.
        folderNameEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus && dialog.getWindow() != null) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }

            }
        });

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positiveButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                if (positiveButton != null) {
                    // Disable "Ok" button by default
                    positiveButton.setEnabled(false);
                }
            }
        });

        dialog.show();
    }

    public static void duplicate(Context context, final File file, final FileManagementListener listener) {
        new DuplicateFileTask(context, file, null, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void duplicate(Context context, final File file, final File destDir, final FileManagementListener listener) {
        new DuplicateFileTask(context, file, destDir, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void delete(Context context, final ArrayList<FileInfo> filesToDelete, final FileManagementListener listener) {
        // If we don't have anything to delete, just return.
        if (filesToDelete.size() == 0 || context == null) {
            return;
        }

        final ArrayList<FileInfo> files = new ArrayList<>(filesToDelete);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        CharSequence message;
        String title;
        if (files.size() > 1) {
            message = context.getResources().getString(R.string.dialog_delete_msg_files);
            title = context.getResources().getString(R.string.dialog_delete_title);
        } else {
            FileInfo fileInfo = files.get(0);
            String name = fileInfo.getName();
            if (fileInfo.isDirectory()) {
                int[] fileCount = files.get(0).getFileCount();
                message = Html.fromHtml(context.getResources().getString(R.string.dialog_delete_folder_message, fileCount[0], fileCount[1], name));
            } else {
                message = context.getResources().getString(R.string.dialog_delete_file_message, name);
            }
            title = context.getResources().getString(R.string.dialog_delete_title);
        }

        final WeakReference<Context> contextRef = new WeakReference<>(context);

        builder.setMessage(message)
            .setTitle(title)
            .setCancelable(true)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Context context = contextRef.get();
                    if (context != null) {
                        new DeleteFileTask(context, files, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            })
            .create().show();
    }

    private static void moveFile(Context context, final List<FileInfo> filesToMove, final File targetFolder, final Map<FileInfo, Boolean> filesMoved,
                                 boolean replaceAll, final FileManagementListener listener) {
        if (filesToMove.size() > 0) {
            final FileInfo file = filesToMove.remove(0);
            File targetFile = new File(targetFolder, file.getFile().getName());

            // If file being moved to the same location, then just return.
            if (FilenameUtils.equals(targetFile.getAbsolutePath(), file.getAbsolutePath())) {
                filesMoved.put(file, true);

                if (filesToMove.size() == 0) {
                    if (listener != null) {
                        listener.onFileMoved(filesMoved, targetFolder);
                    }
                } else {
                    moveFile(context, filesToMove, targetFolder, filesMoved, replaceAll, listener);
                }

                return;
            }

            // If file being moved to a different location, then check if it
            // already exists and ask for overwrite, or just move it.
            if (targetFile.exists()) {
                if (!replaceAll) {
                    String message = String.format(context.getResources().getString(R.string.dialog_move_file_already_exists_message), targetFile.getName());
                    View dialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_move_file_overwrite, null);
                    TextView textViewMessage = dialogLayout.findViewById(R.id.text_view_message);
                    textViewMessage.setText(message);
                    final CheckBox checkBoxRepeatAction = dialogLayout.findViewById(R.id.check_box_repeat_action);
                    if (filesToMove.size() > 0) {
                        // Show checkbox to pre-confirm replacing any other files
                        checkBoxRepeatAction.setVisibility(View.VISIBLE);
                        checkBoxRepeatAction.setChecked(false);
                    } else {
                        checkBoxRepeatAction.setVisibility(View.GONE);
                    }

                    final WeakReference<Context> contextRef = new WeakReference<>(context);

                    // Ask to replace file
                    AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setView(dialogLayout)
                        .setCancelable(false)
                        .setPositiveButton(R.string.replace, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Context context = contextRef.get();
                                if (context == null) {
                                    return;
                                }

                                // Get replace-all checkbox state
                                final boolean replaceAllChecked;
                                replaceAllChecked = checkBoxRepeatAction.getVisibility() == View.VISIBLE && checkBoxRepeatAction.isChecked();
                                dialog.dismiss();
                                try {
                                    FileUtils.copyFileToDirectory(file.getFile(), targetFolder);
                                    FileUtils.deleteQuietly(file.getFile());
                                    filesMoved.put(file, true);
                                } catch (Exception e) {
                                    filesMoved.put(file, false);
                                    CommonToast.showText(context, String.format(context.getResources().getString(R.string.dialog_move_file_error_message), file.getFile().getName()),
                                        Toast.LENGTH_SHORT);
                                }
                                if (filesToMove.size() == 0) {
                                    if (listener != null) {
                                        listener.onFileMoved(filesMoved, targetFolder);
                                    }
                                } else {
                                    moveFile(context, filesToMove, targetFolder, filesMoved, replaceAllChecked, listener);
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                // Don't move any more files
                                if (listener != null) {
                                    listener.onFileMoved(filesMoved, targetFolder);
                                }
                            }
                        });

                    builder.show();
                } else {
                    try {
                        FileUtils.copyFileToDirectory(file.getFile(), targetFolder);
                        FileUtils.deleteQuietly(file.getFile());
                        filesMoved.put(file, true);
                    } catch (Exception e) {
                        filesMoved.put(file, false);
                        CommonToast.showText(context, String.format(context.getResources().getString(R.string.dialog_move_file_error_message), file.getFile().getName()),
                            Toast.LENGTH_SHORT);
                    }
                    if (filesToMove.size() == 0) {
                        if (listener != null) {
                            listener.onFileMoved(filesMoved, targetFolder);
                        }
                    } else {
                        moveFile(context, filesToMove, targetFolder, filesMoved, true, listener);
                    }
                }
            } else {
                try {
                    FileUtils.moveFileToDirectory(file.getFile(), targetFolder, false);
                    filesMoved.put(file, true);
                } catch (IOException e) {
                    filesMoved.put(file, false);
                    CommonToast.showText(context, String.format(context.getResources().getString(R.string.dialog_move_file_error_message), file.getFile().getName()),
                        Toast.LENGTH_SHORT);
                }
                if (filesToMove.size() == 0) {
                    if (listener != null) {
                        listener.onFileMoved(filesMoved, targetFolder);
                    }
                } else {
                    moveFile(context, filesToMove, targetFolder, filesMoved, replaceAll, listener);
                }
            }
        }
    }

    private static void moveFile(Context context, final List<FileInfo> filesToMove, final Map<FileInfo, Boolean> filesMoved,
                                 final ExternalFileInfo targetFolder, final boolean replaceAll, final FileManagementListener listener) {
        if (filesToMove.size() > 0) {
            FileInfo file = filesToMove.get(0);
            // Create target file's uri from the target folder's
            Uri targetFileUri = ExternalFileInfo.appendPathComponent(targetFolder.getUri(), file.getName());

            // If file being moved to a different location, then check if it
            // already exists and ask for overwrite, or just move it.
            final ExternalFileInfo targetFile = new ExternalFileInfo(context, targetFolder, targetFileUri);
            if (targetFile.exists()) {
                if (!replaceAll) {
                    String message = String.format(context.getResources().getString(R.string.dialog_move_file_already_exists_message), file.getName());
                    View dialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_move_file_overwrite, null);
                    TextView textViewMessage = dialogLayout.findViewById(R.id.text_view_message);
                    textViewMessage.setText(message);
                    final CheckBox checkBoxRepeatAction = dialogLayout.findViewById(R.id.check_box_repeat_action);
                    if (filesToMove.size() > 1) {
                        // Show checkbox to pre-confirm replacing any other files
                        checkBoxRepeatAction.setVisibility(View.VISIBLE);
                        checkBoxRepeatAction.setChecked(false);
                    } else {
                        checkBoxRepeatAction.setVisibility(View.GONE);
                    }

                    final WeakReference<Context> contextRef = new WeakReference<>(context);

                    // Ask to replace this file
                    final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setView(dialogLayout)
                        .setCancelable(false)
                        .setPositiveButton(R.string.replace, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Context context = contextRef.get();
                                if (context == null) {
                                    return;
                                }

                                final boolean replaceAllChecked;
                                replaceAllChecked = checkBoxRepeatAction.getVisibility() == View.VISIBLE && checkBoxRepeatAction.isChecked();
                                dialog.dismiss();
                                // Try to delete the existing file asynchronously
                                (new CustomAsyncTask<Void, Void, Boolean>(context) {

                                    @Override
                                    protected Boolean doInBackground(Void... params) {
                                        return targetFile.delete();
                                    }

                                    @Override
                                    protected void onPostExecute(Boolean deleted) {
                                        Context context = getContext();
                                        if (context == null) {
                                            return;
                                        }
                                        if (deleted) {
                                            // Try to move file again (should succeed)
                                            new MoveFileTask(context, filesToMove, filesMoved,
                                                targetFile, targetFolder, replaceAllChecked, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                        } else {
                                            // Unable to delete file, don't move any more files
                                            MoveFileTask.dismissProgressDialog();
                                            Utils.safeShowAlertDialog(context,
                                                context.getResources().getString(R.string.dialog_delete_error_message, targetFile.getFileName()),
                                                context.getResources().getString(R.string.error));
                                            if (listener != null) {
                                                listener.onFileMoved(filesMoved, targetFolder);
                                            }
                                        }
                                    }
                                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                // Don't move any more files
                                if (listener != null) {
                                    listener.onFileMoved(filesMoved, targetFolder);
                                }
                            }
                        });

                    builder.show();
                } else {
                    // Try to delete the existing file asynchronously
                    (new CustomAsyncTask<Void, Void, Boolean>(context) {

                        @Override
                        protected Boolean doInBackground(Void... params) {
                            return targetFile.delete();
                        }

                        @Override
                        protected void onPostExecute(Boolean deleted) {
                            Context context = getContext();
                            if (context == null) {
                                return;
                            }
                            if (deleted) {
                                // Try to move file again (should succeed)
                                new MoveFileTask(context, filesToMove, filesMoved,
                                    targetFile, targetFolder, true, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            } else {
                                // Unable to delete file, don't move any more files
                                MoveFileTask.dismissProgressDialog();
                                Utils.safeShowAlertDialog(context,
                                    context.getResources().getString(R.string.dialog_delete_error_message, targetFile.getFileName()),
                                    context.getResources().getString(R.string.error));
                                if (listener != null) {
                                    listener.onFileMoved(filesMoved, targetFolder);
                                }
                            }
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            } else {
                new MoveFileTask(context, filesToMove, filesMoved,
                    targetFile, targetFolder, replaceAll, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } else {
            MoveFileTask.dismissProgressDialog();
            // No more files to move, notify listener
            if (listener != null) {
                listener.onFileMoved(filesMoved, targetFolder);
            }
        }
    }

    public static void move(Context context, ArrayList<FileInfo> filesToMove, final File targetFolder, final FileManagementListener listener) {
        Map<FileInfo, Boolean> filesMoved = new HashMap<>();
        moveFile(context, filesToMove, targetFolder, filesMoved, false, listener);
    }

    public static void move(Context context, ArrayList<FileInfo> filesToMove, final ExternalFileInfo targetFolder, final FileManagementListener listener) {
        Map<FileInfo, Boolean> filesMoved = new HashMap<>();
        moveFile(context, filesToMove, filesMoved, targetFolder, false, listener);
    }

    public static void initCache(Context context) {
        // if old cache exits, always delete it
        CacheUtils.configureCache(context);
        if (CacheUtils.hasCache(CACHE_FILE_LIST_OBJECT_OLD)) {
            CacheUtils.deleteFile(CACHE_FILE_LIST_OBJECT_OLD);
        }
    }

    public static void saveCache(ArrayList<FileInfo> saveList) throws IllegalStateException {
        MiscUtils.throwIfOnMainThread();
        LinkedHashMap<String, Integer> fileInfoMap = new LinkedHashMap<>();
        for (FileInfo fileInfo : saveList) {
            String fileName = fileInfo.getAbsolutePath();
            Integer fileType = fileInfo.getType();
            fileInfoMap.put(fileName, fileType);
        }
        try {
            CacheUtils.writeDataMapFile(CACHE_FILE_LIST_OBJECT, fileInfoMap);
        } catch (OutOfMemoryError oom) {
            MiscUtils.manageOOM(null);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    public static ArrayList<FileInfo> retrieveCache() throws IllegalStateException {
        MiscUtils.throwIfOnMainThread();
        try {
            // note the second entry should be considered as Double rather than Integer: https://goo.gl/XzXD5D
            LinkedHashMap<String, Double> fileInfoMap = CacheUtils.readDataMapFile(CACHE_FILE_LIST_OBJECT);
            ArrayList<FileInfo> result = new ArrayList<>();
            for (Map.Entry<String, Double> entry : fileInfoMap.entrySet()) {
                String filename = entry.getKey();
                double value = entry.getValue();
                int fileType = (int) value;
                FileInfo fileInfo = new FileInfo(fileType, new File(filename));
                result.add(fileInfo);
            }
            return result;
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return null;
        }
    }

    private static class DuplicateFileTask extends CustomAsyncTask<Void, Void, Void> {

        private FileManagementListener mFileManagementListener;
        private ProgressDialog mProgressDialog;
        private Boolean mSuccess;
        private File mFile;
        private String mMessage;
        private File mDestinationDir;
        private File mNewFile;

        DuplicateFileTask(Context context, final File file, final File destinationDir, FileManagementListener listener) {
            super(context);
            this.mFileManagementListener = listener;
            this.mFile = file;
            this.mMessage = "";
            this.mSuccess = false;
            this.mDestinationDir = destinationDir;
        }

        @Override
        protected void onPreExecute() {
            Context context = getContext();
            if (context == null) {
                return;
            }
            this.mProgressDialog = ProgressDialog.show(context, "", context.getResources().getString(R.string.duplicating_wait), true);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            this.mSuccess = false;
            for (int i = 1; i <= MAX_NUM_DUPLICATED_FILES; i++) {
                String newFileName = FilenameUtils.removeExtension(mFile.getAbsolutePath()) + " (" + String.valueOf(i) + ")."
                    + Utils.getExtension(mFile.getAbsolutePath());
                mNewFile = new File(newFileName);
                if (null != mDestinationDir) {
                    newFileName = FilenameUtils.getName(newFileName);
                    mNewFile = new File(mDestinationDir, newFileName);
                }

                if (!mNewFile.exists()) {
                    try {
                        FileUtils.copyFile(this.mFile, mNewFile);
                        this.mSuccess = true;
                    } catch (IOException | NullPointerException e) {
                        this.mSuccess = false;
                        Resources r = Utils.getResources(getContext());
                        if (r == null) {
                            return null;
                        }
                        this.mMessage = r.getString(R.string.duplicate_file_error_message);
                    }
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Context context = getContext();
            if (context == null) {
                return;
            }
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (!this.mSuccess) {
                String message = this.mMessage.length() > 0 ? this.mMessage : context.getResources().getString(R.string.duplicate_file_max_error_message);
                Utils.safeShowAlertDialog(context, message, context.getResources().getString(R.string.error));
            } else {
                if (this.mFileManagementListener != null) {
                    this.mFileManagementListener.onFileDuplicated(mNewFile);
                }
            }
        }
    }

    private static class DeleteFileTask extends CustomAsyncTask<Void, Void, Void> {

        private ArrayList<FileInfo> mFiles;
        private FileManagementListener mFileManagementListener;
        private Boolean mSuccess;
        private ProgressDialog mProgressDialog;

        DeleteFileTask(Context context, ArrayList<FileInfo> files, FileManagementListener listener) {
            super(context);
            this.mFiles = files;
            this.mFileManagementListener = listener;
            this.mSuccess = true;
        }

        @Override
        protected void onPreExecute() {
            Context context = getContext();
            if (context == null) {
                return;
            }
            this.mProgressDialog = ProgressDialog.show(context, "", context.getResources().getString(R.string.deleting_file_wait), true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                for (FileInfo file : this.mFiles) {
                    FileUtils.forceDelete(file.getFile());
                    PdfViewCtrlTabsManager.getInstance().removePdfViewCtrlTabInfo(getContext(), file.getAbsolutePath());
                }
                this.mSuccess = true;
            } catch (IOException e) {
                this.mSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Context context = getContext();
            if (context == null) {
                return;
            }
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (!this.mSuccess) {
                if (this.mFiles.size() > 1) {
                    Utils.safeShowAlertDialog(context,
                        context.getResources().getString(R.string.dialog_delete_error_message_general),
                        context.getResources().getString(R.string.error));
                } else {
                    Utils.safeShowAlertDialog(context,
                        context.getResources().getString(R.string.dialog_delete_error_message, this.mFiles.get(0).getName()),
                        context.getResources().getString(R.string.error));
                }
            } else {
                if (this.mFileManagementListener != null) {
                    this.mFileManagementListener.onFileDeleted(this.mFiles);
                }
            }
        }
    }

    private static class MoveFileTask extends CustomAsyncTask<Void, Integer, Void> implements
        ProgressDialog.OnCancelListener {

        private static ProgressDialog sProgressDialog;

        private String mMessage;
        private Boolean mSuccess;
        private FileInfo mSourceFile;
        private ExternalFileInfo mTargetFile;
        private ExternalFileInfo mTargetFolder;
        private List<FileInfo> mFilesToMove;
        private Map<FileInfo, Boolean> mFilesMoved;
        private boolean mReplaceAll;
        private FileManagementListener mListener;
        private final Handler mHandler = new Handler();

        MoveFileTask(Context context, List<FileInfo> filesToMove, Map<FileInfo, Boolean> filesMoved,
                     ExternalFileInfo targetFile, ExternalFileInfo targetFolder, boolean replaceAll,
                     final FileManagementListener listener) {
            super(context);
            this.mFilesToMove = filesToMove;
            this.mFilesMoved = filesMoved;
            this.mSourceFile = filesToMove.get(0);
            this.mTargetFolder = targetFolder;
            this.mTargetFile = targetFile;
            this.mReplaceAll = replaceAll;
            this.mListener = listener;
            this.mMessage = "";
            this.mSuccess = false;
        }

        @Override
        protected void onPreExecute() {
            Context context = getContext();
            if (context == null) {
                return;
            }
            if (sProgressDialog == null) {
                // Set up progress dialog
                sProgressDialog = new ProgressDialog(context);
                sProgressDialog.setTitle("");
                sProgressDialog.setIndeterminate(true);
                sProgressDialog.setCancelable(false);
            }
            sProgressDialog.setMessage(context.getResources().getString(R.string.moving_wait));
            sProgressDialog.setOnCancelListener(this);
            sProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                context.getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
            if (!sProgressDialog.isShowing()) {
                // Delay showing progress dialog, so it does not flash for short tasks
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sProgressDialog.show();
                    }
                }, SHOW_PROGRESS_DIALOG_DELAY);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            this.mSuccess = false;

            CheckedInputStream checkedInputStream = null;
            CheckedOutputStream checkedOutputStream = null;
            try {
                String extension = mSourceFile.getExtension();
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (mTargetFolder == null || Utils.isNullOrEmpty(mimeType)) {
                    this.mSuccess = false;
                    return null;
                }
                mTargetFile = mTargetFolder.createFile(mimeType, mSourceFile.getName());
                // Copy the file's contents to the target
                FileInputStream fileInputStream = new FileInputStream(mSourceFile.getFile());
                ContentResolver cr = Utils.getContentResolver(getContext());
                if (cr == null) {
                    return null;
                }
                OutputStream outputStream = cr.openOutputStream(mTargetFile.getUri(), "w");
                checkedInputStream = new CheckedInputStream(fileInputStream, new Adler32());
                checkedOutputStream = new CheckedOutputStream(outputStream, new Adler32());
                MiscUtils.copy(checkedInputStream, checkedOutputStream, this);
                if (isCancelled()) {
                    return null;
                }
                Adler32 inCheckSum = (Adler32) checkedInputStream.getChecksum();
                Adler32 outCheckSum = (Adler32) checkedOutputStream.getChecksum();
                // Before deleting old file, compare check-sums and ensure that sizes match
                if (inCheckSum.getValue() == outCheckSum.getValue()) {
                    // Update fields, including size
                    mTargetFile.initFields();
                    if (mSourceFile.getFile().length() == mTargetFile.getSize()) {
                        // Delete old file
                        this.mSuccess = FileUtils.deleteQuietly(mSourceFile.getFile());
                    }
                }
            } catch (IOException e) {
                this.mSuccess = false;
                this.mMessage = null;
                if (Utils.isLollipop() && e.getCause() instanceof ErrnoException) {
                    ErrnoException errnoException = (ErrnoException) e.getCause();
                    if (errnoException.errno == OsConstants.ENOSPC) {
                        Resources r = Utils.getResources(getContext());
                        if (r == null) {
                            return null;
                        }
                        this.mMessage = r.getString(R.string.duplicate_file_error_message_no_space);
                    }
                }
                if (this.mMessage == null) {
                    Resources r = Utils.getResources(getContext());
                    if (r == null) {
                        return null;
                    }
                    this.mMessage = r.getString(R.string.dialog_move_file_error_message, mSourceFile.getName());
                }
            } catch (Exception e) {
                this.mSuccess = false;
                Resources r = Utils.getResources(getContext());
                if (r == null) {
                    return null;
                }
                this.mMessage = r.getString(R.string.dialog_move_file_error_message, mSourceFile.getName());
            } finally {
                Utils.closeQuietly(checkedInputStream);
                Utils.closeQuietly(checkedOutputStream);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Context context = getContext();
            if (context == null) {
                return;
            }
            mHandler.removeCallbacksAndMessages(null);
            mFilesToMove.remove(mSourceFile);
            mFilesMoved.put(mSourceFile, this.mSuccess);
            if (mFilesToMove.size() < 1) {
                // No more files to move, dismiss progress dialog
                if (sProgressDialog != null && sProgressDialog.isShowing()) {
                    sProgressDialog.dismiss();
                }
            }
            if (!this.mSuccess) {
                if (mTargetFile != null) { // Clean up after failure
                    mTargetFile.delete();
                    mTargetFile = null;
                }
                String message = this.mMessage.length() > 0 ? this.mMessage : context.getResources().getString(R.string.dialog_move_file_error_message, mSourceFile.getName());

                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle(context.getResources().getString(R.string.error))
                    .setMessage(message)
                    .setCancelable(true);
                if (mFilesToMove.size() > 0) {
                    // Give user option to continue moving other files
                    builder.setPositiveButton(R.string.dialog_move_continue, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Context context = getContext();
                            if (context == null) {
                                return;
                            }
                            dialog.dismiss();
                            // Move next file in list
                            moveFile(context, mFilesToMove, mFilesMoved, mTargetFolder, mReplaceAll, mListener);
                        }
                    })
                        .setNegativeButton(R.string.cancel, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                // Stop moving files, notify listener
                                if (mListener != null) {
                                    mListener.onFileMoved(mFilesMoved, mTargetFolder);
                                }
                            }
                        });
                } else {
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    // No more files to move, notify listener
                    if (mListener != null) {
                        mListener.onFileMoved(mFilesMoved, mTargetFolder);
                    }
                }
                builder.show();
            } else {
                // Move next file in list
                moveFile(context, mFilesToMove, mFilesMoved, mTargetFolder, mReplaceAll, mListener);
            }
        }

        @Override
        protected void onCancelled() {
            mHandler.removeCallbacksAndMessages(null);
            if (!mSuccess && mTargetFile != null) { // Clean up after move cancelled
                mTargetFile.delete();
                mTargetFile = null;
            }
            mFilesToMove.remove(mSourceFile);
            mFilesMoved.put(mSourceFile, false);
            if (sProgressDialog != null) {
                // Dismiss progress dialog
                if (sProgressDialog.isShowing()) {
                    sProgressDialog.dismiss();
                }
            }
            // Stop moving files, notify listener
            if (mListener != null) {
                mListener.onFileMoved(mFilesMoved, mTargetFolder);
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            // Progress dialog was cancelled, so cancel move operation
            this.cancel(true);
        }

        static void dismissProgressDialog() {
            if (sProgressDialog != null) {
                if (sProgressDialog.isShowing()) {
                    sProgressDialog.dismiss();
                }
                sProgressDialog = null;
            }
        }
    }

    public static class ChangeCacheFileTask extends AsyncTask<Void, Void, Void> {
        ArrayList<FileInfo> deleteFiles;
        ArrayList<FileInfo> addFiles;
        ArrayList<FileInfo> cacheFiles;
        private boolean isChanged = false;
        final private Object cacheLock;

        public ChangeCacheFileTask(FileInfo deleteFile, FileInfo addFile, Object cacheLock) {
            deleteFiles = new ArrayList<>();
            addFiles = new ArrayList<>();
            if (deleteFile != null) {
                deleteFiles.add(deleteFile);
            }
            if (addFile != null) {
                addFiles.add(addFile);
            }
            this.cacheLock = cacheLock;
        }

        public ChangeCacheFileTask(ArrayList<FileInfo> deleteFiles, ArrayList<FileInfo> addFiles, Object cacheLock) {
            this.deleteFiles = new ArrayList<>();
            this.deleteFiles.addAll(deleteFiles);
            this.addFiles = new ArrayList<>();
            this.addFiles.addAll(addFiles);
            this.cacheLock = cacheLock;

        }

        public ChangeCacheFileTask(FileInfo deleteFile, ArrayList<FileInfo> addFiles, Object cacheLock) {
            deleteFiles = new ArrayList<>();
            if (deleteFile != null) {
                deleteFiles.add(deleteFile);
            }
            this.addFiles = new ArrayList<>();
            this.addFiles.addAll(addFiles);
            this.cacheLock = cacheLock;
        }

        public ChangeCacheFileTask(ArrayList<FileInfo> deleteFiles, FileInfo addFile, Object cacheLock) {
            this.deleteFiles = new ArrayList<>();
            this.deleteFiles.addAll(deleteFiles);
            addFiles = new ArrayList<>();
            if (addFile != null) {
                addFiles.add(addFile);
            }
            this.cacheLock = cacheLock;
        }

        @Override
        protected Void doInBackground(Void... params) {
            synchronized (cacheLock) {
                cacheFiles = retrieveCache();
            }
            if (cacheFiles == null) {
                return null;
            }
            if (null != deleteFiles && !deleteFiles.isEmpty()) {
                Logger.INSTANCE.LogD(TAG, "deleteFiles.size = " + deleteFiles.size());

                for (FileInfo file : deleteFiles) {
                    for (int i = 0; i < cacheFiles.size(); i++) {
                        if (file.getAbsolutePath().equals(cacheFiles.get(i).getAbsolutePath())) {
                            cacheFiles.remove(i);
                            isChanged = true;
                            break;
                        }
                    }
                }
            }

            if (null != addFiles && !addFiles.isEmpty()) {
                Logger.INSTANCE.LogD(TAG, "addFiles.size = " + addFiles.size());
                cacheFiles.addAll(addFiles);
                isChanged = true;
            }

            if (isChanged && cacheFiles != null) {
                Logger.INSTANCE.LogD(TAG, "save cache file to cache");

                saveCache(cacheFiles);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Logger.INSTANCE.LogD(TAG, "ChangeCacheFileTask: onPostExecute");
        }
    }
}
