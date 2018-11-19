//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.utils;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.pdftron.demo.R;
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
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.SDFDoc;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

/**
 * @hide
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ExternalFileManager {

    public interface ExternalFileManagementListener {

        void onExternalFileRenamed(ExternalFileInfo oldFile, ExternalFileInfo newFile);

        void onExternalFileDuplicated(ExternalFileInfo fileCopy);

        void onExternalFileDeleted(ArrayList<ExternalFileInfo> deletedFiles);

        void onRootsRemoved(ArrayList<ExternalFileInfo> deletedFiles);

        void onExternalFileMoved(Map<ExternalFileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder);

        void onExternalFileMoved(Map<ExternalFileInfo, Boolean> filesMoved, File targetFolder);

        void onExternalFolderCreated(ExternalFileInfo rootFolder, ExternalFileInfo newFolder);

        void onExternalFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile);
    }

    private static final int MAX_NUM_DUPLICATED_FILES = 100;

    private static final int SHOW_PROGRESS_DIALOG_DELAY = 500;

    public static void rename(Context context, final ExternalFileInfo file, final ExternalFileManagementListener listener) {
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

        EditText renameEditText = renameDialog.findViewById(R.id.dialog_rename_file_edit);
        final WeakReference<EditText> renameEditTextRef = new WeakReference<>(renameEditText);
        renameEditText.setText(file.getFileName());
        // Show the edit text with name of file selected.
        if (file.isDirectory()) {
            renameEditText.setSelection(0, file.getFileName().length());
            renameEditText.setHint(context.getResources().getString(R.string.dialog_rename_folder_hint));
        } else {
            int index = FilenameUtils.indexOfExtension(file.getFileName());
            if (index == -1) {
                index = file.getFileName().length();
            }
            renameEditText.setSelection(0, index);
            renameEditText.setHint(context.getResources().getString(R.string.dialog_rename_file_hint));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(renameDialog)
            .setTitle(title)
            .setPositiveButton(R.string.ok, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditText renameEditText = renameEditTextRef.get();
                    if (renameEditText == null) {
                        return;
                    }
                    Context context = renameEditText.getContext();

                    Boolean doAction = true;
                    String message = "";
                    String newFileName = file.getFileName();

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
                        String extension = file.getExtension();
                        if (Utils.isNullOrEmpty(extension)) {
                            // no extension, let's try to open it as PDF
                            extension = "pdf";
                        }
                        if (!file.isDirectory() && !newFileName.toLowerCase().endsWith("." + extension.toLowerCase())) {
                            newFileName = newFileName + "." + extension;
                        }
                        if (file.getFileName().equals(newFileName)) {
                            doAction = false;
                            message = "";
                            continue;
                        }

                        // Check if the new file name already exists in this directory
                        if (file.getParent() != null && file.getParent().findFile(newFileName) != null) {
                            doAction = false;
                            message = file.isDirectory() ?
                                context.getResources().getString(R.string.dialog_create_folder_invalid_folder_name_already_exists_message) :
                                context.getResources().getString(R.string.dialog_rename_invalid_file_name_already_exists_message);
                        }
                    } while (false);

                    if (doAction) { // Attempt to rename file
                        Boolean success;
                        ExternalFileInfo oldFile = file.clone();
                        success = file.renameTo(newFileName);
                        if (!success) {
                            Utils.safeShowAlertDialog(context,
                                context.getResources().getString(R.string.dialog_rename_invalid_file_name_error),
                                context.getResources().getString(R.string.alert));
                        } else {
                            if (listener != null) {
                                listener.onExternalFileRenamed(oldFile, file);
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
            .setNegativeButton(R.string.cancel, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

        AlertDialog dialog = builder.create();
        final WeakReference<AlertDialog> dialogRef = new WeakReference<>(dialog);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                EditText renameEditText = renameEditTextRef.get();
                if (renameEditText == null) {
                    return;
                }

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
                AlertDialog dialog = dialogRef.get();
                if (dialog == null) {
                    return;
                }

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
                AlertDialog dialog = dialogRef.get();
                if (dialog == null) {
                    return;
                }

                if (hasFocus && dialog.getWindow() != null) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }

            }
        });

        dialog.show();
    }

    public static void createFolder(Context context, final ExternalFileInfo rootFolder, final ExternalFileManagementListener listener) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return;
        }
        View createFolderDialog = inflater.inflate(R.layout.dialog_create_folder, null);
        String title;

        title = context.getResources().getString(R.string.dialog_create_title);

        EditText folderNameEditText = createFolderDialog.findViewById(R.id.dialog_create_folder_edit);
        final WeakReference<EditText> folderNameEditTextRef = new WeakReference<>(folderNameEditText);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(createFolderDialog)
            .setTitle(title)
            .setPositiveButton(R.string.ok, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditText folderNameEditText = folderNameEditTextRef.get();
                    if (folderNameEditText == null) {
                        return;
                    }
                    Context context = folderNameEditText.getContext();

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
                        if (rootFolder.findFile(newFolderName) != null) {
                            doAction = false;
                            message = context.getResources().getString(R.string.dialog_create_folder_invalid_folder_name_already_exists_message);
                        }
                    } while (false);

                    if (doAction) {
                        ExternalFileInfo newFolder = rootFolder.createDirectory(newFolderName);
                        if (newFolder == null) {
                            Utils.safeShowAlertDialog(context,
                                context.getResources().getString(R.string.dialog_create_folder_invalid_folder_name_error_message),
                                context.getResources().getString(R.string.alert));
                        } else {
                            if (listener != null) {
                                listener.onExternalFolderCreated(rootFolder, newFolder);
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
            .setNegativeButton(R.string.cancel, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EditText folderNameEditText = folderNameEditTextRef.get();
                    if (folderNameEditText == null) {
                        return;
                    }

                    Utils.hideSoftKeyboard(folderNameEditText.getContext(), folderNameEditText);
                    dialog.cancel();
                }
            });
        AlertDialog dialog = builder.create();
        final WeakReference<AlertDialog> dialogRef = new WeakReference<>(dialog);

        folderNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                AlertDialog dialog = dialogRef.get();
                if (dialog == null) {
                    return;
                }

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
                AlertDialog dialog = dialogRef.get();
                if (dialog == null) {
                    return;
                }

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

    public static void duplicate(Context context, final ExternalFileInfo file, final ExternalFileManagementListener listener) {
        new DuplicateFileTask(context, file, null, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void duplicate(Context context, final ExternalFileInfo file, final ExternalFileInfo destDir, final ExternalFileManagementListener listener) {
        new DuplicateFileTask(context, file, destDir, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void delete(Context context, final ArrayList<ExternalFileInfo> filesToDelete, final ExternalFileManagementListener listener) {
        // If we don't have anything to delete, just return.
        if (filesToDelete.size() == 0) {
            return;
        }

        final ArrayList<ExternalFileInfo> files = new ArrayList<>(filesToDelete);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        CharSequence message;
        String title;
        if (files.size() > 1) {
            message = context.getResources().getString(R.string.dialog_delete_msg_files);
            title = context.getResources().getString(R.string.dialog_delete_title);
        } else {
            ExternalFileInfo fileInfo = files.get(0);
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
            .setPositiveButton(R.string.delete, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Context context = contextRef.get();
                    if (context != null) {
                        new DeleteFileTask(context, files, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            })
            .setNegativeButton(R.string.cancel, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            })
            .create().show();
    }

    public static void removeAccess(Context context, final ArrayList<ExternalFileInfo> rootsToRemove, final ExternalFileManagementListener listener) {
        // If we don't have anything to remove, just return.
        if (context == null || rootsToRemove.size() == 0) {
            return;
        }

        final ArrayList<ExternalFileInfo> files = new ArrayList<>(rootsToRemove);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String message;
        String title;
        if (files.size() > 1) {
            message = context.getResources().getString(R.string.dialog_remove_roots_msg);
            title = context.getResources().getString(R.string.dialog_remove_roots_title);
        } else {
            message = context.getResources().getString(R.string.dialog_remove_root_msg, files.get(0).getFileName());
            title = context.getResources().getString(R.string.dialog_remove_roots_title);
        }

        final WeakReference<Context> contextRef = new WeakReference<>(context);

        builder.setMessage(message)
            .setTitle(title)
            .setCancelable(true)
            .setPositiveButton(R.string.undo_redo_annot_remove, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Context context = contextRef.get();
                    if (context != null) {
                        new RemoveAccessTask(context, files, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            })
            .setNegativeButton(R.string.cancel, new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            })
            .create().show();
    }

    private static void moveFile(Context context, final List<ExternalFileInfo> filesToMove, final Map<ExternalFileInfo, Boolean> filesMoved,
                                 final ExternalFileInfo targetFolder, final boolean replaceAll, final ExternalFileManagementListener listener) {
        if (filesToMove.size() > 0) {
            ExternalFileInfo file = filesToMove.get(0);
            // Create target file's uri from the target folder's
            Uri targetFileUri = ExternalFileInfo.appendPathComponent(targetFolder.getUri(), file.getFileName());

            // If file being moved to the same location, then just return.
            if (targetFileUri.equals(file.getUri())) {
                filesToMove.remove(file);
                filesMoved.put(file, true);

                if (filesToMove.size() == 0) {
                    MoveFileTask.dismissProgressDialog();
                    // No more files to move, notify listener
                    if (listener != null) {
                        listener.onExternalFileMoved(filesMoved, targetFolder);
                    }
                } else {
                    // Move next file
                    moveFile(context, filesToMove, filesMoved, targetFolder, replaceAll, listener);
                }
                return;
            }

            // If file being moved to a different location, then check if it
            // already exists and ask for overwrite, or just move it.
            final ExternalFileInfo targetFile = new ExternalFileInfo(context, targetFolder, targetFileUri);
            if (targetFile.exists()) {
                if (!replaceAll) {
                    String message = String.format(context.getResources().getString(R.string.dialog_move_file_already_exists_message), file.getFileName());
                    View dialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_move_file_overwrite, null);
                    TextView textViewMessage = dialogLayout.findViewById(R.id.text_view_message);
                    textViewMessage.setText(message);
                    CheckBox checkBoxRepeatAction = dialogLayout.findViewById(R.id.check_box_repeat_action);
                    final WeakReference<CheckBox> checkBoxRepeatActionRef = new WeakReference<>(checkBoxRepeatAction);
                    if (filesToMove.size() > 1) {
                        // Show checkbox to pre-confirm replacing any other files
                        checkBoxRepeatAction.setVisibility(View.VISIBLE);
                        checkBoxRepeatAction.setChecked(false);
                    } else {
                        checkBoxRepeatAction.setVisibility(View.GONE);
                    }
                    // Ask to replace this file
                    AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setView(dialogLayout)
                        .setCancelable(false)
                        .setPositiveButton(R.string.replace, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                CheckBox checkBoxRepeatAction = checkBoxRepeatActionRef.get();
                                if (checkBoxRepeatAction == null) {
                                    return;
                                }
                                Context context = checkBoxRepeatAction.getContext();

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
                                                listener.onExternalFileMoved(filesMoved, targetFolder);
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
                                    listener.onExternalFileMoved(filesMoved, targetFolder);
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
                                    listener.onExternalFileMoved(filesMoved, targetFolder);
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
                listener.onExternalFileMoved(filesMoved, targetFolder);
            }
        }
    }

    private static void moveFile(Context context, final List<ExternalFileInfo> filesToMove, final Map<ExternalFileInfo, Boolean> filesMoved,
                                 final File targetFolder, final boolean replaceAll, final ExternalFileManagementListener listener) {
        if (filesToMove.size() > 0) {
            ExternalFileInfo file = filesToMove.get(0);
            final File targetFile = new File(targetFolder, file.getFileName());

            // If file being moved to a different location, then check if it
            // already exists and ask for overwrite, or just move it.
            if (targetFile.exists()) {
                if (!replaceAll) {
                    String message = String.format(context.getResources().getString(R.string.dialog_move_file_already_exists_message), file.getFileName());
                    View dialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_move_file_overwrite, null);
                    TextView textViewMessage = dialogLayout.findViewById(R.id.text_view_message);
                    textViewMessage.setText(message);
                    CheckBox checkBoxRepeatAction = dialogLayout.findViewById(R.id.check_box_repeat_action);
                    final WeakReference<CheckBox> checkBoxRepeatActionRef = new WeakReference<>(checkBoxRepeatAction);
                    if (filesToMove.size() > 1) {
                        // Show checkbox to pre-confirm replacing any other files
                        checkBoxRepeatAction.setVisibility(View.VISIBLE);
                        checkBoxRepeatAction.setChecked(false);
                    } else {
                        checkBoxRepeatAction.setVisibility(View.GONE);
                    }
                    // Ask to replace this file
                    AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setView(dialogLayout)
                        .setCancelable(false)
                        .setPositiveButton(R.string.replace, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                CheckBox checkBoxRepeatAction = checkBoxRepeatActionRef.get();
                                if (checkBoxRepeatAction == null) {
                                    return;
                                }
                                Context context = checkBoxRepeatAction.getContext();

                                final boolean replaceAllChecked;
                                replaceAllChecked = checkBoxRepeatAction.getVisibility() == View.VISIBLE && checkBoxRepeatAction.isChecked();
                                dialog.dismiss();

                                new MoveFileTask(context, filesToMove, filesMoved,
                                    targetFile, targetFolder, replaceAllChecked, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                // Don't move any more files
                                if (listener != null) {
                                    listener.onExternalFileMoved(filesMoved, targetFolder);
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
                                    context.getResources().getString(R.string.dialog_delete_error_message, targetFile.getName()),
                                    context.getResources().getString(R.string.error));
                                if (listener != null) {
                                    listener.onExternalFileMoved(filesMoved, targetFolder);
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
                listener.onExternalFileMoved(filesMoved, targetFolder);
            }
        }
    }

    public static void move(Context context, ArrayList<ExternalFileInfo> filesToMove, final ExternalFileInfo targetFolder, final ExternalFileManagementListener listener) {
        Map<ExternalFileInfo, Boolean> filesMoved = new HashMap<>();
        moveFile(context, filesToMove, filesMoved, targetFolder, false, listener);
    }

    public static void move(Context context, ArrayList<ExternalFileInfo> filesToMove, final File targetFolder, final ExternalFileManagementListener listener) {
        Map<ExternalFileInfo, Boolean> filesMoved = new HashMap<>();
        moveFile(context, filesToMove, filesMoved, targetFolder, false, listener);
    }

    public static void merge(Context context, ArrayList<FileInfo> filesToMerge, ArrayList<FileInfo> filesToDelete,
                             final FileInfo targetFile, final ExternalFileManagementListener listener) {
        new MergeFileTask(context, filesToMerge, filesToDelete, targetFile, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class DuplicateFileTask extends CustomAsyncTask<Void, Integer, Void> implements
        ProgressDialog.OnCancelListener {

        private ExternalFileManagementListener mFileManagementListener;
        private ProgressDialog mProgressDialog;
        private Boolean mSuccess;
        private ExternalFileInfo mFile;
        private String mMessage;
        private ExternalFileInfo mDestinationDir;
        private ExternalFileInfo mNewFile;
        private final Handler mHandler = new Handler();

        DuplicateFileTask(Context context, final ExternalFileInfo file, final ExternalFileInfo destinationDir, ExternalFileManagementListener listener) {
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
            this.mProgressDialog = new ProgressDialog(context);
            this.mProgressDialog.setTitle("");
            this.mProgressDialog.setMessage(context.getResources().getString(R.string.duplicating_wait));
            this.mProgressDialog.setIndeterminate(true);
            this.mProgressDialog.setCancelable(false);
            this.mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                context.getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
            this.mProgressDialog.setOnCancelListener(this);
            // Delay showing progress dialog, so it does not flash for short tasks
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.show();
                }
            }, SHOW_PROGRESS_DIALOG_DELAY);
        }

        @Override
        protected Void doInBackground(Void... params) {
            this.mSuccess = true;
            if (mDestinationDir == null) { // No destination set, use same dir as file
                mDestinationDir = mFile.getParent();
            }
            if (mDestinationDir == null) {
                return null;
            }
            for (int i = 1; i <= MAX_NUM_DUPLICATED_FILES; i++) {
                if (isCancelled()) {
                    this.mSuccess = false;
                    break;
                }

                String newFileName = FilenameUtils.removeExtension(mFile.getFileName()) + " (" + String.valueOf(i) + ")";
                String fileExtension = mFile.getExtension();
                String newFileNameWithExtension = newFileName + fileExtension;

                if (mDestinationDir.findFile(newFileNameWithExtension) == null) {
                    InputStream inputStream = null;
                    OutputStream outputStream = null;
                    try {
                        if (mDestinationDir != null) {
                            mNewFile = mDestinationDir.createFile(mFile.getType(), newFileName);
                        }
                        if (mNewFile != null) {
                            // Copy the file's contents to the duplicate
                            ContentResolver cr = Utils.getContentResolver(getContext());
                            if (cr == null) {
                                return null;
                            }
                            inputStream = cr.openInputStream(mFile.getUri());
                            outputStream = cr.openOutputStream(mNewFile.getUri(), "w");
                            if (inputStream != null && outputStream != null) {
                                copy(inputStream, outputStream, this);
                            } else {
                                throw new Exception("cannot create input/output stream");
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
                            this.mMessage = r.getString(R.string.duplicate_file_error_message);
                        }
                        break;
                    } catch (Exception e) {
                        this.mSuccess = false;
                        Resources r = Utils.getResources(getContext());
                        if (r == null) {
                            return null;
                        }
                        this.mMessage = r.getString(R.string.duplicate_file_error_message);
                        break;
                    } finally {
                        Utils.closeQuietly(inputStream);
                        Utils.closeQuietly(outputStream);
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
            mHandler.removeCallbacksAndMessages(null);
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (!this.mSuccess) {
                if (mNewFile != null) { // Clean up after failure
                    mNewFile.delete();
                    mNewFile = null;
                }
                String message = this.mMessage.length() > 0 ? this.mMessage : context.getResources().getString(R.string.duplicate_file_max_error_message);
                Utils.safeShowAlertDialog(context, message, context.getResources().getString(R.string.error));
            }
            if (this.mFileManagementListener != null) {
                this.mFileManagementListener.onExternalFileDuplicated(this.mSuccess ? mNewFile : null);
            }
        }

        @Override
        protected void onCancelled() {
            mHandler.removeCallbacksAndMessages(null);
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            if (mNewFile != null) { // Clean up after duplication cancelled
                mNewFile.delete();
                mNewFile = null;
            }
            if (this.mFileManagementListener != null) {
                this.mFileManagementListener.onExternalFileDuplicated(null);
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            // Progress dialog was cancelled, so cancel duplication
            this.cancel(true);
        }
    }

    private static class MoveFileTask extends CustomAsyncTask<Void, Integer, Void> implements
        ProgressDialog.OnCancelListener {

        private static ProgressDialog mProgressDialog;

        private String mMessage;
        private Boolean mSuccess;
        private ExternalFileInfo mSourceFile;
        private ExternalFileInfo mTargetFile;
        private ExternalFileInfo mTargetFolder;
        private File mTargetLocalFile;
        private File mTargetLocalFolder;
        private List<ExternalFileInfo> mFilesToMove;
        private Map<ExternalFileInfo, Boolean> mFilesMoved;
        private boolean mReplaceAll;
        private ExternalFileManagementListener mListener;
        private final Handler mHandler = new Handler();

        MoveFileTask(Context context, List<ExternalFileInfo> filesToMove, Map<ExternalFileInfo, Boolean> filesMoved,
                     ExternalFileInfo targetFile, ExternalFileInfo targetFolder, boolean replaceAll,
                     final ExternalFileManagementListener listener) {
            super(context);
            this.mFilesToMove = filesToMove;
            this.mFilesMoved = filesMoved;
            this.mSourceFile = filesToMove.get(0);
            this.mTargetFolder = targetFolder;
            this.mTargetFile = targetFile;
            this.mTargetLocalFile = null;
            this.mTargetLocalFolder = null;
            this.mReplaceAll = replaceAll;
            this.mListener = listener;
            this.mMessage = "";
            this.mSuccess = false;
        }

        MoveFileTask(Context context, List<ExternalFileInfo> filesToMove, Map<ExternalFileInfo, Boolean> filesMoved,
                     File targetFile, File targetFolder, boolean replaceAll,
                     final ExternalFileManagementListener listener) {
            super(context);
            this.mFilesToMove = filesToMove;
            this.mFilesMoved = filesMoved;
            this.mSourceFile = filesToMove.get(0);
            this.mTargetFolder = null;
            this.mTargetFile = null;
            this.mTargetLocalFile = targetFile;
            this.mTargetLocalFolder = targetFolder;
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
            if (mProgressDialog == null) {
                // Set up progress dialog
                mProgressDialog = new ProgressDialog(context);
                mProgressDialog.setTitle("");
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
            }
            mProgressDialog.setMessage(context.getResources().getString(R.string.moving_wait));
            mProgressDialog.setOnCancelListener(this);
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                context.getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
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
        protected Void doInBackground(Void... params) {
            this.mSuccess = false;

            InputStream inputStream = null;
            OutputStream outputStream = null;
            CheckedInputStream checkedInputStream = null;
            CheckedOutputStream checkedOutputStream = null;
            try {
                ContentResolver cr = Utils.getContentResolver(getContext());
                if (cr == null) {
                    return null;
                }
                inputStream = cr.openInputStream(mSourceFile.getUri());
                if (mTargetFolder != null) {
                    mTargetFile = mTargetFolder.createFile(mSourceFile.getType(), mSourceFile.getFileName());
                    // Copy the file's contents to the target
                    if (mTargetFile != null) {
                        outputStream = cr.openOutputStream(mTargetFile.getUri(), "w");
                    }
                } else {
                    if (!mTargetLocalFile.exists()) {
                        // Create file
                        try {
                            //noinspection ResultOfMethodCallIgnored
                            mTargetLocalFile.createNewFile();
                        } catch (IOException e) {
                            return null;
                        }
                    }
                    outputStream = new FileOutputStream(mTargetLocalFile);
                }
                if (inputStream == null || outputStream == null) {
                    return null;
                }
                checkedInputStream = new CheckedInputStream(inputStream, new Adler32());
                checkedOutputStream = new CheckedOutputStream(outputStream, new Adler32());
                copy(checkedInputStream, checkedOutputStream, this);
                if (isCancelled()) {
                    return null;
                }
                Adler32 inCheckSum = (Adler32) checkedInputStream.getChecksum();
                Adler32 outCheckSum = (Adler32) checkedOutputStream.getChecksum();
                // Before deleting old file, compare check-sums and ensure that sizes match
                if (inCheckSum.getValue() == outCheckSum.getValue()) {
                    if (mTargetFile != null) {
                        // Update fields, including size
                        mTargetFile.initFields();
                        if (mSourceFile.getSize() == mTargetFile.getSize()) {
                            // Delete old file
                            this.mSuccess = mSourceFile.delete();
                        }
                    } else {
                        if (mSourceFile.getSize() == mTargetLocalFile.length()) {
                            // Delete old file
                            this.mSuccess = mSourceFile.delete();
                        }
                    }
                }
            } catch (IOException e) {
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
                    this.mMessage = r.getString(R.string.dialog_move_file_error_message, mSourceFile.getFileName());
                }
            } catch (Exception e) {
                Resources r = Utils.getResources(getContext());
                if (r == null) {
                    return null;
                }
                this.mMessage = r.getString(R.string.dialog_move_file_error_message, mSourceFile.getFileName());
            } finally {
                Utils.closeQuietly(inputStream);
                Utils.closeQuietly(outputStream);
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
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
            }
            if (!this.mSuccess) {
                // Clean up after failure
                if (mTargetFile != null) {
                    mTargetFile.delete();
                    mTargetFile = null;
                } else if (mTargetLocalFile != null) {
                    FileUtils.deleteQuietly(mTargetLocalFile);
                }
                String message = this.mMessage.length() > 0 ? this.mMessage : context.getResources().getString(R.string.dialog_move_file_error_message, mSourceFile.getFileName());

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
                            if (mTargetFolder != null) {
                                moveFile(context, mFilesToMove, mFilesMoved, mTargetFolder, mReplaceAll, mListener);
                            } else {
                                moveFile(context, mFilesToMove, mFilesMoved, mTargetLocalFolder, mReplaceAll, mListener);
                            }
                        }
                    });
                    builder.setNegativeButton(R.string.cancel, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            // Stop moving files, notify listener
                            if (mListener != null) {
                                if (mTargetFolder != null) {
                                    mListener.onExternalFileMoved(mFilesMoved, mTargetFolder);
                                } else {
                                    mListener.onExternalFileMoved(mFilesMoved, mTargetLocalFolder);
                                }
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
                        if (mTargetFolder != null) {
                            mListener.onExternalFileMoved(mFilesMoved, mTargetFolder);
                        } else {
                            mListener.onExternalFileMoved(mFilesMoved, mTargetLocalFolder);
                        }
                    }
                }
                builder.show();
            } else {
                // Move next file in list
                if (mTargetFolder != null) {
                    moveFile(context, mFilesToMove, mFilesMoved, mTargetFolder, mReplaceAll, mListener);
                } else {
                    moveFile(context, mFilesToMove, mFilesMoved, mTargetLocalFolder, mReplaceAll, mListener);
                }
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
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                // Dismiss progress dialog
                mProgressDialog.dismiss();
            }
            // Stop moving files, notify listener
            if (mListener != null) {
                if (mTargetFolder != null) {
                    mListener.onExternalFileMoved(mFilesMoved, mTargetFolder);
                } else {
                    mListener.onExternalFileMoved(mFilesMoved, mTargetLocalFolder);
                }
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            // Progress dialog was cancelled, so cancel move operation
            this.cancel(true);
        }

        static void dismissProgressDialog() {
            if (mProgressDialog != null) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                mProgressDialog = null;
            }
        }
    }

    private static class DeleteFileTask extends CustomAsyncTask<Void, Void, Void> {

        private ArrayList<ExternalFileInfo> mFiles;
        private ExternalFileManagementListener mFileManagementListener;
        private Boolean mSuccess;
        private ProgressDialog mProgressDialog;

        private final Handler mHandler = new Handler();

        DeleteFileTask(Context context, ArrayList<ExternalFileInfo> files, ExternalFileManagementListener listener) {
            super(context);
            this.mFiles = files;
            this.mFileManagementListener = listener;
            this.mSuccess = false;
        }

        @Override
        protected void onPreExecute() {
            Context context = getContext();
            if (context == null) {
                return;
            }
            this.mProgressDialog = new ProgressDialog(context);
            this.mProgressDialog.setTitle("");
            this.mProgressDialog.setMessage(context.getResources().getString(R.string.deleting_file_wait));
            this.mProgressDialog.setIndeterminate(true);
            this.mProgressDialog.setCancelable(false);
            // Delay showing progress dialog, so it does not flash for short tasks
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.show();
                }
            }, SHOW_PROGRESS_DIALOG_DELAY);
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (ExternalFileInfo file : this.mFiles) {
                if (!file.delete()) {
                    this.mSuccess = false;
                    return null;
                }
            }
            this.mSuccess = true;
            return null;
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
            if (!this.mSuccess) {
                if (this.mFiles.size() > 1) {
                    Utils.safeShowAlertDialog(context,
                        context.getResources().getString(R.string.dialog_delete_error_message_general),
                        context.getResources().getString(R.string.error));
                } else {
                    Utils.safeShowAlertDialog(context,
                        context.getResources().getString(R.string.dialog_delete_error_message, this.mFiles.get(0).getFileName()),
                        context.getResources().getString(R.string.error));
                }
            } else {
                if (this.mFileManagementListener != null) {
                    this.mFileManagementListener.onExternalFileDeleted(this.mFiles);
                }
            }
        }
    }

    private static class RemoveAccessTask extends CustomAsyncTask<Void, Void, Void> {

        private ArrayList<ExternalFileInfo> mFiles;
        private ExternalFileManagementListener mFileManagementListener;
        private Boolean mSuccess;
        private ProgressDialog mProgressDialog;

        private final Handler mHandler = new Handler();

        RemoveAccessTask(Context context, ArrayList<ExternalFileInfo> files, ExternalFileManagementListener listener) {
            super(context);
            this.mFiles = files;
            this.mFileManagementListener = listener;
            this.mSuccess = false;
        }

        @Override
        protected void onPreExecute() {
            Context context = getContext();
            if (context == null) {
                return;
            }
            this.mProgressDialog = new ProgressDialog(context);
            this.mProgressDialog.setTitle("");
            this.mProgressDialog.setMessage(context.getResources().getString(R.string.removing_access_wait));
            this.mProgressDialog.setIndeterminate(true);
            this.mProgressDialog.setCancelable(false);
            // Delay showing progress dialog, so it does not flash for short tasks
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.show();
                }
            }, SHOW_PROGRESS_DIALOG_DELAY);
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (ExternalFileInfo file : this.mFiles) {
                if (file.getRootUri() != null) {
                    try {
                        ContentResolver cr = Utils.getContentResolver(getContext());
                        if (cr == null) {
                            return null;
                        }
                        cr.releasePersistableUriPermission(file.getRootUri(),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    } catch (Exception e) {
                        this.mSuccess = false;
                        return null;
                    }
                }
            }
            this.mSuccess = true;
            return null;
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
            if (!this.mSuccess) {
                if (this.mFiles.size() > 1) {
                    Utils.safeShowAlertDialog(context,
                        context.getResources().getString(R.string.dialog_remove_roots_error_message_general),
                        context.getResources().getString(R.string.error));
                } else {
                    Utils.safeShowAlertDialog(context,
                        context.getResources().getString(R.string.dialog_remove_roots_error_message, this.mFiles.get(0).getFileName()),
                        context.getResources().getString(R.string.error));
                }
            } else {
                if (this.mFileManagementListener != null) {
                    this.mFileManagementListener.onRootsRemoved(this.mFiles);
                }
            }
        }
    }

    private static class MergeFileTask extends CustomAsyncTask<Void, Void, Void> {

        private ArrayList<FileInfo> mFiles;
        private ArrayList<FileInfo> mTempFiles;
        private FileInfo mTargetFile;
        private ExternalFileManagementListener mListener;
        private ProgressDialog mProgressDialog;
        private Boolean mSuccess;

        MergeFileTask(Context context, ArrayList<FileInfo> filesToMerge, ArrayList<FileInfo> filesToDelete, FileInfo targetFile, ExternalFileManagementListener listener) {
            super(context);
            mFiles = filesToMerge;
            mTempFiles = filesToDelete;
            mTargetFile = targetFile;
            mListener = listener;
            mSuccess = false;
        }

        @Override
        protected void onPreExecute() {
            Context context = getContext();
            if (context == null) {
                return;
            }
            mProgressDialog = ProgressDialog.show(context, "", context.getResources().getString(R.string.merging_wait), true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            mSuccess = false;
            if (mTargetFile == null) {
                return null;
            }
            PDFDoc mergedDoc = null;
            PDFDoc inDoc = null;
            boolean shouldUnlockRead = false;
            SecondaryFileFilter filter = null;
            try {
                mergedDoc = new PDFDoc();
                mergedDoc.initSecurityHandler();

                for (int i = 0; i < mFiles.size(); i++) {
                    // Add pages to end of merged doc
                    FileInfo fileInfo = mFiles.get(i);

                    ContentResolver cr = Utils.getContentResolver(getContext());
                    if (cr == null) {
                        return null;
                    }
                    if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_FILE) {
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
                    } else if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
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
                    } else {
                        continue;
                    }
                    inDoc.lockRead();
                    shouldUnlockRead = true;

                    Logger.INSTANCE.LogD("MERGE", "Merging " + fileInfo.getAbsolutePath());

                    Page[] copyPages = new Page[inDoc.getPageCount()];
                    PageIterator iterator = inDoc.getPageIterator();
                    int j = 0;
                    while (iterator.hasNext()) {
                        Page page = iterator.next();
                        copyPages[j++] = page;
                    }

                    Logger.INSTANCE.LogD("MERGE", "Importing pages from " + fileInfo.getAbsolutePath() + " to " + mTargetFile.getAbsolutePath());
                    Page[] importedPages = mergedDoc.importPages(copyPages, true);

                    Logger.INSTANCE.LogD("MERGE", "Pushing pages back into " + mTargetFile.getAbsolutePath());
                    // Add imported pages to the merged doc's page sequence
                    for (Page page : importedPages) {
                        mergedDoc.pagePushBack(page);
                    }

                    inDoc.unlockRead();
                    shouldUnlockRead = false;
                    Utils.closeQuietly(inDoc, filter);
                    inDoc = null;
                    filter = null;
                }

                Logger.INSTANCE.LogD("MERGE", "Saving merged doc to " + mTargetFile.getAbsolutePath());
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
                    return null;
                }

                mSuccess = true;
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(inDoc);
                }
                Utils.closeQuietly(inDoc);
                Utils.closeQuietly(mergedDoc, filter);
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
            if (mSuccess) {
                mListener.onExternalFileMerged(mFiles, mTempFiles, mTargetFile);
            } else {
                Utils.safeShowAlertDialog(context,
                    context.getResources().getString(R.string.dialog_merge_error_message_general),
                    context.getResources().getString(R.string.error));
            }
        }
    }

    // Cancellable copy operation
    @SuppressWarnings("UnusedReturnValue")
    private static int copy(InputStream input, OutputStream output, CustomAsyncTask task) throws IOException {
        long count = 0L;
        byte[] buffer = new byte[4096];

        int n1;
        for (; -1 != (n1 = input.read(buffer)) && !task.isCancelled(); count += (long) n1) {
            output.write(buffer, 0, n1);
        }

        return (count > 2147483647L) ? -1 : (int) count;
    }
}
