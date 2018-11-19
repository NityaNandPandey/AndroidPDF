package com.pdftron.demo.utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import com.pdftron.common.PDFNetException;
import com.pdftron.demo.R;
import com.pdftron.demo.dialog.FilePickerDialogFragment;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.Convert;
import com.pdftron.pdf.DocumentConversion;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.SDFDoc;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;

public class AddDocPdfHelper implements
        FilePickerDialogFragment.MultipleFilesListener,
        FilePickerDialogFragment.LocalFolderListener,
        FilePickerDialogFragment.ExternalFolderListener {

    private static final String TAG = AddDocPdfHelper.class.getName();

    private static final int SELECT_FILE_REQUEST = 1;
    private static final int SELECT_FOLDER_REQUEST = 3;

    private FragmentManager mFragmentManager;
    private AddDocPDFHelperListener mListener;
    private Context mContext;
    private ProgressDialog mProgressDialog;

    private File mCurrentFolder;
    private ExternalFileInfo mCurrentExtFolder;

    private ConvertTask mCurrentConvertTask;
    private MultiConvertTask mCurrentMultiConvertTask;
    private SingleConvertTask mCurrentSingleConvertTask;

    public AddDocPdfHelper(Context context, FragmentManager fragmentManager, @NonNull AddDocPDFHelperListener listener) {
        mFragmentManager = fragmentManager;
        mListener = listener;
        mContext = context;
    }

    public void pickFileAndCreate(File currentFolder) {
        mCurrentFolder = currentFolder;
        FilePickerDialogFragment dialog = FilePickerDialogFragment.newInstance(SELECT_FILE_REQUEST, Environment.getExternalStorageDirectory());
        dialog.setMultipleFilesListener(this);
        dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        dialog.show(mFragmentManager, "file_picker_dialog");
    }

    public void pickFileAndCreate(ExternalFileInfo currentExtFolder) {
        mCurrentExtFolder = currentExtFolder;
        FilePickerDialogFragment dialog = FilePickerDialogFragment.newInstance(SELECT_FILE_REQUEST, Environment.getExternalStorageDirectory());
        dialog.setMultipleFilesListener(this);
        dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        dialog.show(mFragmentManager, "file_picker_dialog");
    }

    public void pickFileAndCreate() {
        FilePickerDialogFragment dialog = FilePickerDialogFragment.newInstance(SELECT_FILE_REQUEST, Environment.getExternalStorageDirectory());
        dialog.setMultipleFilesListener(this);
        dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        dialog.show(mFragmentManager, "file_picker_dialog");
    }

    @Override
    public void onMultipleFilesSelected(int requestCode, ArrayList<FileInfo> fileInfoList) {
        if (fileInfoList.size() == 1) {
            FileInfo fileInfo = fileInfoList.get(0);
            getDocumentTitleAndConvert(fileInfo);
        } else if (fileInfoList.size() > 1) {
            if (mListener != null) {
                mListener.onMultipleFilesSelected(requestCode, fileInfoList);
            }
        } else {
            Logger.INSTANCE.LogE(TAG, "trying to convert zero files");
        }
    }

    public void handleMergeConfirmed(final ArrayList<FileInfo> filesToMerge, final ArrayList<FileInfo> filesToDelete, final String title) {
        if (mCurrentFolder == null && mCurrentExtFolder == null) {
            FilePickerDialogFragment filePicker = FilePickerDialogFragment.
                    newInstance(SELECT_FOLDER_REQUEST, Environment.getExternalStorageDirectory());
            filePicker.setLocalFolderListener(new FilePickerDialogFragment.LocalFolderListener() {
                @Override
                public void onLocalFolderSelected(int requestCode, Object customObject, File folder) {
                    AddDocPdfHelper.this.mCurrentFolder = folder;
                    if (mCurrentMultiConvertTask != null) {
                        mCurrentConvertTask.cancel(true);
                    }
                    mCurrentMultiConvertTask = new MultiConvertTask(mContext, filesToMerge, filesToDelete, title);
                    mCurrentMultiConvertTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            });
            filePicker.setExternalFolderListener(new FilePickerDialogFragment.ExternalFolderListener() {
                @Override
                public void onExternalFolderSelected(int requestCode, Object customObject, ExternalFileInfo folder) {
                    AddDocPdfHelper.this.mCurrentExtFolder = folder;
                    if (mCurrentMultiConvertTask != null) {
                        mCurrentConvertTask.cancel(true);
                    }
                    mCurrentMultiConvertTask = new MultiConvertTask(mContext, filesToMerge, filesToDelete, title);
                    mCurrentMultiConvertTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            });
            filePicker.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
            filePicker.show(mFragmentManager, "file_picker_dialog");
        } else {
            if (mCurrentMultiConvertTask != null) {
                mCurrentConvertTask.cancel(true);
            }
            mCurrentMultiConvertTask = new MultiConvertTask(mContext, filesToMerge, filesToDelete, title);
            mCurrentMultiConvertTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onExternalFolderSelected(int requestCode, Object object, ExternalFileInfo folder) {

    }

    @Override
    public void onLocalFolderSelected(int requestCode, Object object, File folder) {

    }

    private void createAndShowProgressDialog() {
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(mContext.getString(R.string.tools_misc_please_wait));
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                if (mCurrentConvertTask != null)
                    mCurrentConvertTask.cancel(true);
            }
        });
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.show();
    }

    private String saveDocAndGetPath(DocumentConversion documentConversion, String name) {
        if (documentConversion == null) {
            return null;
        }
        PDFDoc doc = null;
        SecondaryFileFilter filter = null;
        try {
            doc = documentConversion.getDoc();
            if (mCurrentFolder != null) {
                String newFile = Utils.getFileNameNotInUse(new File(mCurrentFolder, FilenameUtils.removeExtension(name) + ".pdf").getAbsolutePath());
                doc.save(newFile, SDFDoc.SaveMode.REMOVE_UNUSED, null);
                return newFile;
            }
            if (mCurrentExtFolder != null) {
                String filename = Utils.getFileNameNotInUse(mCurrentExtFolder, FilenameUtils.removeExtension(name) + ".pdf");
                ExternalFileInfo targetFile = mCurrentExtFolder.createFile("application/pdf", filename);
                if (targetFile == null) {
                    return null;
                }

                // parcel is attached to doc, so shouldn't be close here
                filter = new SecondaryFileFilter(mContext, Uri.parse(targetFile.getAbsolutePath()));
                doc.save(filter, SDFDoc.SaveMode.REMOVE_UNUSED);
                return targetFile.getAbsolutePath();
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(doc, filter);
        }

        return null;
    }

    @SuppressWarnings("unused")
    public void createPdfFromImage(ExternalFileInfo currentFolder, Uri inputUri, String targetFilename) {
        mCurrentExtFolder = currentFolder;
        if (mCurrentConvertTask != null) {
            mCurrentConvertTask.cancel(true);
        }
        mCurrentConvertTask = new ConvertTask(mContext, inputUri, targetFilename);
        mCurrentConvertTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @SuppressWarnings("unused")
    public void createPdfFromImage(File currentFolder, String inputPath, String targetFilename) {
        mCurrentFolder = currentFolder;
        try {
            File file = new File(inputPath);
            if (mCurrentConvertTask != null) {
                mCurrentConvertTask.cancel(true);
            }
            mCurrentConvertTask = new ConvertTask(mContext, file, targetFilename);
            mCurrentConvertTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            mListener.onPDFReturned(null, false);
        }
    }

    private String saveDocumentConversion(DocumentConversion documentConversion,
                                          CustomAsyncTask<Void, Void, Boolean> parentAsyncTask,
                                          String fileName) {
        try {
            while (documentConversion.getConversionStatus() == DocumentConversion.e_incomplete) {
                documentConversion.convertNextPage();

                if ((parentAsyncTask != null) && parentAsyncTask.isCancelled()) {
                    return null;
                }
            }

            if (documentConversion.getConversionStatus() != DocumentConversion.e_success) {
                return null;
            }

            return saveDocAndGetPath(documentConversion, fileName);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return null;
        }
    }

    private class SingleConvertTask extends CustomAsyncTask<Void, Void, Boolean> {

        FileInfo mFileInfo;
        String mTargetFileName;
        String mOutFile;

        SingleConvertTask(Context context, FileInfo fileInfo, String targetFileName) {
            super(context);
            mFileInfo = fileInfo;
            mTargetFileName = targetFileName;
            mOutFile = null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            createAndShowProgressDialog();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            DocumentConversion documentConversion = null;
            SecondaryFileFilter filter = null;
            try {
                if (mFileInfo.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                    documentConversion = Convert.universalConversion(mFileInfo.getAbsolutePath(), null);
                }
                if (mFileInfo.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                    Uri uri = Uri.parse(mFileInfo.getAbsolutePath());
                    filter = new SecondaryFileFilter(mContext, uri);
                    documentConversion = Convert.universalConversion(filter, null);
                }
                Logger.INSTANCE.LogE(TAG, "Merge only supports internal and external files");
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                Utils.closeQuietly(filter);
            }

            if (documentConversion != null) {
                mOutFile = saveDocumentConversion(documentConversion, SingleConvertTask.this,
                        mTargetFileName);
                return !Utils.isNullOrEmpty(mOutFile);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean pass) {
            super.onPostExecute(pass);
            if (mProgressDialog != null) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                mProgressDialog = null;
            }
            if (pass) {
                if (mCurrentFolder != null)
                    mListener.onPDFReturned(mOutFile, false);
                if (mCurrentExtFolder != null)
                    mListener.onPDFReturned(mOutFile, true);
            } else {
                mListener.onPDFReturned(null, false);
            }
        }
    }

    private class MultiConvertTask extends CustomAsyncTask<Void, Void, Boolean> {

        ArrayList<FileInfo> mFilesToMerge;
        ArrayList<FileInfo> mFilesToDelete;
        String mTargetFileName;
        String mOutFile;

        MultiConvertTask(Context context, ArrayList<FileInfo> filesToMerge,
                         ArrayList<FileInfo> filesToDelete, String targetFileName) {
            super(context);
            mFilesToMerge = filesToMerge;
            mFilesToDelete = filesToDelete;
            mTargetFileName = targetFileName;
            mOutFile = null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            createAndShowProgressDialog();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            DocumentConversion documentConversion = null;
            SecondaryFileFilter filter = null;
            try {
                for (FileInfo fileToMerge : mFilesToMerge) {
                    if (documentConversion == null) {
                        if (fileToMerge.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                            documentConversion = Convert.universalConversion(fileToMerge.getAbsolutePath(), null);
                        }
                        if (fileToMerge.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                            Uri uri = Uri.parse(fileToMerge.getAbsolutePath());
                            filter = new SecondaryFileFilter(mContext, uri);
                            documentConversion = Convert.universalConversion(filter, null);
                        }
                        Logger.INSTANCE.LogE(TAG, "Merge only supports internal and external files");
                    } else {
                        if (fileToMerge.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                            documentConversion = Convert.appendUniversalConversion(documentConversion,
                                    fileToMerge.getAbsolutePath(), null);
                        }
                        if (fileToMerge.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                            Uri uri = Uri.parse(fileToMerge.getAbsolutePath());
                            filter = new SecondaryFileFilter(mContext, uri);
                            documentConversion = Convert.appendUniversalConversion(documentConversion,
                                filter, null);
                        }
                        Logger.INSTANCE.LogE(TAG, "Merge only supports internal and external files");
                    }
                    if (documentConversion == null) {
                        return false;
                    }
                }
                mOutFile = saveDocumentConversion(documentConversion, MultiConvertTask.this,
                        mTargetFileName);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                Utils.closeQuietly(filter);
            }
            return !Utils.isNullOrEmpty(mOutFile);
        }

        @Override
        protected void onPostExecute(Boolean pass) {
            super.onPostExecute(pass);
            if (mProgressDialog != null) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                mProgressDialog = null;
            }
            if (pass) {
                if (mCurrentFolder != null)
                    mListener.onPDFReturned(mOutFile, false);
                if (mCurrentExtFolder != null)
                    mListener.onPDFReturned(mOutFile, true);
            } else {
                mListener.onPDFReturned(null, false);
            }
        }
    }

    private class ConvertTask extends CustomAsyncTask<Void, Void, Boolean> {

        private File mInFile;
        private Uri mExternalUri;
        private String mTargetFileName;
        private String mOutFile;

        ConvertTask(Context context, File inFile, String targetFileName) {
            super(context);
            mInFile = inFile;
            mExternalUri = null;
            mTargetFileName = targetFileName;
        }

        ConvertTask(Context context, Uri externalUri, String targetFileName) {
            super(context);
            mInFile = null;
            mExternalUri = externalUri;
            mTargetFileName = targetFileName;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            createAndShowProgressDialog();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (mInFile != null) {
                try {
                    DocumentConversion conv = Convert.universalConversion(mInFile.getAbsolutePath(), null);
                    while (conv.getConversionStatus() == DocumentConversion.e_incomplete) {
                        conv.convertNextPage();
                        if (isCancelled()) {
                            return false;
                        }
                    }

                    if (conv.getConversionStatus() == DocumentConversion.e_failure || conv.getConversionStatus() != DocumentConversion.e_success) {
                        return false;
                    }
                    mOutFile = saveDocAndGetPath(conv, mTargetFileName);
                    return mOutFile != null;
                } catch (PDFNetException e) {
                    return false;
                }
            } else if (mExternalUri != null) {
                SecondaryFileFilter filter = null;
                try {
                    ContentResolver cr = Utils.getContentResolver(getContext());
                    if (cr == null) {
                        return null;
                    }
                    filter = new SecondaryFileFilter(getContext(), mExternalUri);
                    DocumentConversion conv = Convert.universalConversion(filter, null);
                    while (conv.getConversionStatus() == DocumentConversion.e_incomplete) {
                        conv.convertNextPage();
                        if (isCancelled()) {
                            return false;
                        }
                    }

                    if (conv.getConversionStatus() == DocumentConversion.e_failure || conv.getConversionStatus() != DocumentConversion.e_success) {
                        return false;
                    }
                    mOutFile = saveDocAndGetPath(conv, mTargetFileName);
                    return mOutFile != null;
                } catch (Exception e) {
                    return false;
                } finally {
                    Utils.closeQuietly(filter);
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean pass) {
            super.onPostExecute(pass);
            if (mProgressDialog != null) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                mProgressDialog = null;
            }
            if (pass) {
                if (mCurrentFolder != null)
                    mListener.onPDFReturned(mOutFile, false);
                if (mCurrentExtFolder != null)
                    mListener.onPDFReturned(mOutFile, true);
            } else {
                mListener.onPDFReturned(null, false);
            }
        }
    }

    public interface AddDocPDFHelperListener {
        void onPDFReturned(String fileAbsolutePath, boolean external);
        void onMultipleFilesSelected(int requestCode, ArrayList<FileInfo> fileInfoList);
    }

    private void getDocumentTitleAndConvert(final FileInfo fileInfo) {
        final View nameDialog = LayoutInflater.from(mContext).inflate(R.layout.dialog_rename_file, null);
        final EditText nameEditText = (EditText) nameDialog.findViewById(R.id.dialog_rename_file_edit);
        // Suggest the first file's name for the merged file
        String defaultFileName;
        if (fileInfo != null) {
            defaultFileName = fileInfo.getName();
            if (!Utils.isNullOrEmpty(defaultFileName)) {
                defaultFileName = FilenameUtils.removeExtension(defaultFileName) + "-Converted.pdf";
                nameEditText.setHint(defaultFileName);
            }
        } else {
            nameEditText.setHint(R.string.dialog_merge_set_file_name_hint);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setView(nameDialog)
                .setTitle(mContext.getString(R.string.dialog_merge_set_file_name_title))
                .setPositiveButton(R.string.convert, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String fileName = nameEditText.getText().toString();
                        if (Utils.isNullOrEmpty(fileName)) {
                            fileName = nameEditText.getHint().toString();
                        }
                        if (!fileName.toLowerCase().endsWith(".pdf")) {
                            fileName += ".pdf";
                        }
                        final String finalFileName = fileName;
                        if (mCurrentFolder == null && mCurrentExtFolder == null) {
                            FilePickerDialogFragment filePicker = FilePickerDialogFragment.
                                    newInstance(SELECT_FOLDER_REQUEST, Environment.getExternalStorageDirectory());
                            filePicker.setLocalFolderListener(new FilePickerDialogFragment.LocalFolderListener() {
                                @Override
                                public void onLocalFolderSelected(int requestCode, Object customObject, File folder) {
                                    AddDocPdfHelper.this.mCurrentFolder = folder;
                                    if (mCurrentSingleConvertTask != null) {
                                        mCurrentSingleConvertTask.cancel(true);
                                    }
                                    mCurrentSingleConvertTask = new SingleConvertTask(mContext, fileInfo, finalFileName);
                                    mCurrentSingleConvertTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                }
                            });
                            filePicker.setExternalFolderListener(new FilePickerDialogFragment.ExternalFolderListener() {
                                @Override
                                public void onExternalFolderSelected(int requestCode, Object customObject, ExternalFileInfo folder) {
                                    AddDocPdfHelper.this.mCurrentExtFolder = folder;
                                    if (mCurrentSingleConvertTask != null) {
                                        mCurrentSingleConvertTask.cancel(true);
                                    }
                                    mCurrentSingleConvertTask = new SingleConvertTask(mContext, fileInfo, finalFileName);
                                    mCurrentSingleConvertTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                }
                            });
                            filePicker.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
                            filePicker.show(mFragmentManager, "file_picker_dialog");
                        } else {
                            if (mCurrentSingleConvertTask != null) {
                                mCurrentSingleConvertTask.cancel(true);
                            }
                            mCurrentSingleConvertTask = new SingleConvertTask(mContext, fileInfo, finalFileName);
                            mCurrentSingleConvertTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        final AlertDialog dialog = builder.create();

        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((s.length() > 0) || !Utils.isNullOrEmpty(nameEditText.getHint().toString())) {
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
        nameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
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
                if ((nameEditText.getText() != null && nameEditText.getText().length() > 0) ||
                        !Utils.isNullOrEmpty(nameEditText.getHint().toString())) {
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                } else {
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });

        dialog.show();
    }
}
