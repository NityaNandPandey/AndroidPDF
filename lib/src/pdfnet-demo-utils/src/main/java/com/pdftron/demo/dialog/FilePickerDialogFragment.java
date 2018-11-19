//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.dialog;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.pdftron.demo.R;
import com.pdftron.demo.R2;
import com.pdftron.demo.asynctask.PopulateFolderTask;
import com.pdftron.demo.asynctask.PopulateSdFolderTask;
import com.pdftron.demo.navigation.adapter.ExternalStorageAdapter;
import com.pdftron.demo.navigation.adapter.LocalFileAdapter;
import com.pdftron.demo.navigation.adapter.RecentAdapter;
import com.pdftron.demo.navigation.callbacks.FileManagementListener;
import com.pdftron.demo.utils.ExternalFileManager;
import com.pdftron.demo.utils.ExternalFileManager.ExternalFileManagementListener;
import com.pdftron.demo.utils.FileInfoComparator;
import com.pdftron.demo.utils.FileManager;
import com.pdftron.demo.utils.Logger;
import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.FavoriteFilesManager;
import com.pdftron.pdf.utils.FileInfoManager;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.RecentFilesManager;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.ItemSelectionHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A multi-purpose file picker that allows picking folder and file from both internal storage and SD card (if permission is granted).
 */
public class FilePickerDialogFragment extends DialogFragment implements
    Toolbar.OnMenuItemClickListener, PopulateFolderTask.Callback, PopulateSdFolderTask.Callback {

    private static final String TAG = FilePickerDialogFragment.class.getName();

    public interface LocalFolderListener {
        void onLocalFolderSelected(int requestCode, Object customObject, File folder);
    }

    public interface ExternalFolderListener {
        void onExternalFolderSelected(int requestCode, Object customObject, ExternalFileInfo folder);
    }

    public interface SingleFileListener {
        void onSingleFileSelected(int requestCode, FileInfo fileInfo);
    }

    public interface MultipleFilesListener {
        void onMultipleFilesSelected(int requestCode, ArrayList<FileInfo> fileInfoList);
    }

    protected static final String REQUEST_CODE = "request_code";
    protected static final String REQUEST_CUSTOM_PARCELABLE_DATA = "request_custom_parcelable_data";
    protected static final String REQUEST_CUSTOM_STRING_DATA = "request_custom_string_data";
    protected static final String DIALOG_TITLE = "dialog_title";
    protected static final String STARTUP_FILE_TYPE = "startup_file_type";
    protected static final String STARTUP_FILE_PATH = "startup_file_path";
    protected static final String STARTUP_FOR_APPEND = "startup_for_append";

    protected static final int DIALOG_MODE_FILES = 0;
    protected static final int DIALOG_MODE_FOLDERS = 1;

    public static final int VIEW_MODE_RECENT = 0;
    public static final int VIEW_MODE_FAVORITE = 1;
    public static final int VIEW_MODE_LOCAL = 2;
    public static final int VIEW_MODE_EXTERNAL = 3;

    protected int mRequestCode;
    protected Parcelable mCustomParcelable;
    protected String mCustomString;
    protected int mDialogMode;
    protected int mViewMode;
    protected int mDialogTitle;
    protected boolean mIsForAppend = false;

    protected File mCurrentFolder;
    protected ArrayList<FileInfo> mFileInfoList;
    protected final Object mFileListLock = new Object();
    protected ExternalFileInfo mCurrentExternalFolder;
    protected ArrayList<ExternalFileInfo> mExternalFileList;
    protected ArrayList<ExternalFileInfo> mExternalRoots = new ArrayList<>();

    protected RecentAdapter mRecentAdapter;
    protected LocalFileAdapter mLocalAdapter;
    protected ExternalStorageAdapter mExternalAdapter;
    protected CustomAsyncTask mPopulateFileListTask;

    private LocalFolderListener mLocalFolderListener;
    private ExternalFolderListener mExternalFolderListener;
    private MultipleFilesListener mMultipleFilesListener;
    private SingleFileListener mSingleFileListener;

    protected ArrayList<FileInfo> mFileInfoSelectedList;
    protected ItemSelectionHelper mItemSelectionHelper;

    protected LinkedHashSet<String> mSelectedFiles;
    protected HashMap<String, Integer> mSelectedFoldersToFileCount;

    // UI
    @BindView(R2.id.fragment_file_picker_dialog_toolbar)
    protected Toolbar mToolbar;
    @BindView(R2.id.fragment_file_picker_dialog_folder_list)
    protected SimpleRecyclerView mRecyclerView;
    @BindView(android.R.id.empty)
    protected TextView mEmptyView;
    @BindView(R2.id.fragment_file_picker_dialog_progress_bar)
    protected com.pdftron.pdf.widget.ContentLoadingRelativeLayout mProgressBarLayout;
    @BindView(R2.id.fragment_file_picker_dialog_fab)
    protected FloatingActionButton mFab;

    private boolean mRootsAvailable;

    public FilePickerDialogFragment() {

    }

    public static Bundle buildBundle(int requestCode, int dialogTitleRes,
                                     File startupFolder, Uri startupFolderUri,
                                     Object customObject,
                                     boolean isForAppend) {
        Bundle args = new Bundle();
        args.putInt(REQUEST_CODE, requestCode);
        if (dialogTitleRes != 0) {
            args.putInt(DIALOG_TITLE, dialogTitleRes);
        }
        if (customObject != null) {
            if (customObject instanceof Parcelable) {
                args.putParcelable(REQUEST_CUSTOM_PARCELABLE_DATA, (Parcelable) customObject);
            } else if (customObject instanceof String) {
                args.putString(REQUEST_CUSTOM_STRING_DATA, (String) customObject);
            }
        }
        if (startupFolder != null) {
            args.putInt(STARTUP_FILE_TYPE, BaseFileInfo.FILE_TYPE_FOLDER);
            args.putString(STARTUP_FILE_PATH, startupFolder.getAbsolutePath());
        } else {
            args.putInt(STARTUP_FILE_TYPE, BaseFileInfo.FILE_TYPE_EXTERNAL);
            args.putString(STARTUP_FILE_PATH, (startupFolderUri != null) ? startupFolderUri.toString() : "");
        }
        args.putBoolean(STARTUP_FOR_APPEND, isForAppend);
        return args;
    }

    public static FilePickerDialogFragment newInstance(int requestCode, int dialogTitleRes,
                                                       File startupFolder, Uri startupFolderUri,
                                                       Object customObject,
                                                       boolean isForAppend) {
        FilePickerDialogFragment fragment = new FilePickerDialogFragment();

        Bundle args = buildBundle(requestCode, dialogTitleRes, startupFolder, startupFolderUri, customObject, isForAppend);
        fragment.setArguments(args);

        return fragment;
    }

    public static FilePickerDialogFragment newInstance(int requestCode, int dialogTitleRes, File startupFolder, Uri startupFolderUri, Object customObject) {
        return newInstance(requestCode, dialogTitleRes, startupFolder, startupFolderUri, customObject, false);
    }

    public static FilePickerDialogFragment newInstance(int requestCode, int dialogTitleRes, File startupFolder) {
        return newInstance(requestCode, dialogTitleRes, startupFolder, null, null);
    }

    public static FilePickerDialogFragment newInstance(int requestCode, File startupFolder) {
        return newInstance(requestCode, 0, startupFolder);
    }

    public static FilePickerDialogFragment newInstance(int requestCode, int dialogTitleRes, Uri startupFolderUri) {
        return newInstance(requestCode, dialogTitleRes, null, startupFolderUri, null);
    }

    public static FilePickerDialogFragment newInstance(int requestCode, Uri startupFolderUri) {
        return newInstance(requestCode, 0, startupFolderUri);
    }

    public void setLocalFolderListener(LocalFolderListener listener) {
        mLocalFolderListener = listener;
    }

    public void setExternalFolderListener(ExternalFolderListener listener) {
        if (Utils.isLollipop()) {
            mExternalFolderListener = listener;
        }
    }

    public void setMultipleFilesListener(MultipleFilesListener listener) {
        mMultipleFilesListener = listener;
    }

    public void setSingleFileListener(SingleFileListener listener) {
        mSingleFileListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        if (context == null) {
            return;
        }

        mFileInfoSelectedList = new ArrayList<>();
        mFileInfoList = new ArrayList<>();
        mExternalFileList = new ArrayList<>();
        mSelectedFiles = new LinkedHashSet<>();
        mSelectedFoldersToFileCount = new HashMap<>();

        mRootsAvailable = true;

        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        mRequestCode = args.getInt(REQUEST_CODE, 0);
        mCustomParcelable = args.getParcelable(REQUEST_CUSTOM_PARCELABLE_DATA);
        mCustomString = args.getString(REQUEST_CUSTOM_STRING_DATA);

        // Determine dialog mode from the listeners that are set
        if (mMultipleFilesListener != null || mSingleFileListener != null) {
            mDialogMode = DIALOG_MODE_FILES;
        } else {
            mDialogMode = DIALOG_MODE_FOLDERS;
        }

        if (mDialogMode == DIALOG_MODE_FILES) {
            mDialogTitle = args.getInt(DIALOG_TITLE, R.string.dialog_file_picker_title);
        } else {
            mDialogTitle = args.getInt(DIALOG_TITLE, R.string.dialog_folder_picker_title);
        }

        int startupFileType = args.getInt(STARTUP_FILE_TYPE);
        String startupFilePath = args.getString(STARTUP_FILE_PATH);

        String savedFilePath;
        int savedFileType;

        if (mDialogMode == DIALOG_MODE_FILES) {
            savedFileType = PdfViewCtrlSettingsManager.getSavedFilePickerFileType(context);
            savedFilePath = PdfViewCtrlSettingsManager.getSavedFilePickerLocation(context);
        } else {
            savedFileType = PdfViewCtrlSettingsManager.getSavedFolderPickerFileType(context);
            savedFilePath = PdfViewCtrlSettingsManager.getSavedFolderPickerLocation(context);
        }
        Logger.INSTANCE.LogI(TAG, "saveFilePath = " + savedFilePath);

        if (savedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL) {
            ExternalFileInfo savedExtFile = Utils.buildExternalFile(getContext(), Uri.parse(savedFilePath));
            if (savedExtFile == null || !savedExtFile.exists()) {
                savedFileType = BaseFileInfo.FILE_TYPE_UNKNOWN;
                savedFilePath = null;
            }
        } else if (savedFilePath != null) {
            File savedFile = new File(savedFilePath);
            if (!savedFile.exists()) {
                savedFileType = BaseFileInfo.FILE_TYPE_UNKNOWN;
                savedFilePath = null;
            }
        }

        // TODO revisit the logic here.
        // if we want to use the last used location,
        // why need to pass startup path/type through newInstance?
        if (savedFileType != BaseFileInfo.FILE_TYPE_UNKNOWN) {
            // use user last used location instead
            startupFileType = savedFileType;
            startupFilePath = savedFilePath;
        }

        if (args.containsKey(STARTUP_FOR_APPEND)) {
            mIsForAppend = args.getBoolean(STARTUP_FOR_APPEND);
        }

        if (startupFileType == BaseFileInfo.FILE_TYPE_FOLDER) {
            mViewMode = VIEW_MODE_LOCAL;
            if (Utils.isNullOrEmpty(startupFilePath)) {
                mCurrentFolder = Environment.getExternalStorageDirectory();
            } else {
                mCurrentFolder = new File(startupFilePath);
                if (Utils.isSdCardFile(getContext(), mCurrentFolder)) {
                    // if we are from SD card folder, re-direct to internal storage
                    mCurrentFolder = Environment.getExternalStorageDirectory();
                }
            }
            mCurrentExternalFolder = null;
        } else if (startupFileType == BaseFileInfo.FILE_TYPE_EXTERNAL) {
            mViewMode = VIEW_MODE_EXTERNAL;
            if (Utils.isNullOrEmpty(startupFilePath)) {
                // Show root external storage folder
                mCurrentExternalFolder = null;
            } else if (Utils.isLollipop()) {
                Uri uri = Uri.parse(startupFilePath);
                if (MiscUtils.isExternalFileUri(getContext(), uri)) {
                    mCurrentExternalFolder = Utils.buildExternalFile(getContext(), uri);
                }
            }
            mCurrentFolder = Environment.getExternalStorageDirectory();
        }
    }

    Button mClearButton, mContinueButton;

    void setContinueButtonProperties(int itemCnt) {
        if (mDialogMode == DIALOG_MODE_FOLDERS) {
            boolean enableSelect = false;
            if ((mViewMode == VIEW_MODE_LOCAL && mCurrentFolder != null) ||
                (mViewMode == VIEW_MODE_EXTERNAL && mCurrentExternalFolder != null)) {
                enableSelect = true;
            }
            if (mContinueButton != null) {
                if (enableSelect) {
                    mContinueButton.setEnabled(true);
                    mContinueButton.setClickable(true);
                } else {
                    mContinueButton.setEnabled(false);
                    mContinueButton.setClickable(false);
                }
                mContinueButton.setText(R.string.select);
            }
        } else {
            if (mIsForAppend) {
                if (mContinueButton != null) {
                    if (itemCnt == 0) {
                        mContinueButton.setEnabled(false);
                        mContinueButton.setClickable(false);
                        mContinueButton.setText(R.string.add);
                    } else {
                        mContinueButton.setEnabled(true);
                        mContinueButton.setClickable(true);
                        mContinueButton.setText(String.format(Locale.getDefault(), "%s (%d)",
                            getString(R.string.add), itemCnt));
                    }
                }
            } else {
                if (mContinueButton != null) {
                    if (itemCnt == 0) {
                        mContinueButton.setEnabled(false);
                        mContinueButton.setClickable(false);
                        mContinueButton.setText(R.string.select);
                    } else if (itemCnt == 1) {
                        mContinueButton.setEnabled(true);
                        mContinueButton.setClickable(true);
                        mContinueButton.setText(String.format(Locale.getDefault(), "%s (%d)",
                            getString(R.string.select), itemCnt));
                    } else {
                        mContinueButton.setEnabled(true);
                        mContinueButton.setClickable(true);
                        mContinueButton.setText(String.format(Locale.getDefault(), "%s (%d)",
                            getString(R.string.merge), itemCnt));
                    }
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            mContinueButton = d.getButton(Dialog.BUTTON_POSITIVE);
            if (mDialogMode == DIALOG_MODE_FILES) {
                if (mMultipleFilesListener != null) {
                    setContinueButtonProperties(0);
                    mClearButton = d.getButton(Dialog.BUTTON_NEUTRAL);
                    mClearButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mFileInfoSelectedList.clear();
                            mItemSelectionHelper.clearChoices();
                            mSelectedFiles.clear();
                            mSelectedFoldersToFileCount.clear();
                            setContinueButtonProperties(0);
                        }
                    });
                }
                mContinueButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mCurrentFolder != null) {
                            saveLastAccessLocation(false, BaseFileInfo.FILE_TYPE_FOLDER, mCurrentFolder.getAbsolutePath());
                        } else if (mCurrentExternalFolder != null) {
                            saveLastAccessLocation(false, BaseFileInfo.FILE_TYPE_EXTERNAL, mCurrentExternalFolder.getAbsolutePath());
                        }
                        if (mMultipleFilesListener != null) {
                            mMultipleFilesListener.
                                onMultipleFilesSelected(mRequestCode, mFileInfoSelectedList);
                        } else if (mSingleFileListener != null) {
                            if (mFileInfoSelectedList.size() > 0) {
                                mSingleFileListener.onSingleFileSelected(mRequestCode, mFileInfoSelectedList.get(0));
                            }
                        }
                        dismiss();
                    }
                });
            }
        }
    }

    protected FileInfoManager getRecentFilesManager() {
        return RecentFilesManager.getInstance();
    }

    protected FileInfoManager getFavoriteFilesManager() {
        return FavoriteFilesManager.getInstance();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        if (mDialogMode == DIALOG_MODE_FILES) {
            builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .setPositiveButton(R.string.select,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            if (mMultipleFilesListener != null) {
                builder.setNeutralButton(R.string.clear,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            }
        }
        if (mDialogMode == DIALOG_MODE_FOLDERS) {
            builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .setPositiveButton(R.string.select,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancelAllThumbRequests();
                            if (mViewMode == VIEW_MODE_LOCAL) {
                                if (mLocalFolderListener != null) {
                                    saveLastAccessLocation(true, BaseFileInfo.FILE_TYPE_FOLDER, mCurrentFolder.getAbsolutePath());
                                    mLocalFolderListener.onLocalFolderSelected(mRequestCode, mCustomParcelable != null ? mCustomParcelable : mCustomString, mCurrentFolder);
                                }
                            } else {
                                if (mCurrentExternalFolder == null) {
                                    CommonToast.showText(getContext(), R.string.dialog_file_picker_error_select_folder, Toast.LENGTH_SHORT);
                                } else {
                                    if (mExternalFolderListener != null) {
                                        saveLastAccessLocation(true, BaseFileInfo.FILE_TYPE_EXTERNAL, mCurrentExternalFolder.getAbsolutePath());
                                        mExternalFolderListener.onExternalFolderSelected(mRequestCode, mCustomParcelable != null ? mCustomParcelable : mCustomString, mCurrentExternalFolder);
                                    }
                                }
                            }
                            dismiss();
                        }
                    });
        }

        if (mDialogTitle != 0) {
            builder.setTitle(mDialogTitle);
        }

        if (getActivity() != null) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            @SuppressLint("InflateParams") View dialogLayout = inflater.inflate(R.layout.fragment_file_picker_dialog, null);

            ButterKnife.bind(this, dialogLayout);

            mToolbar.setNavigationIcon(null);
            mToolbar.setNavigationContentDescription(R.string.abc_action_bar_up_description);

            mToolbar.inflateMenu(R.menu.dialog_file_picker);
            mToolbar.setOnMenuItemClickListener(this);
            updateToolbarMenu(mToolbar.getMenu());
            updateTitle();

            mRecyclerView.initView(0);

            mItemSelectionHelper = new ItemSelectionHelper();
            mItemSelectionHelper.attachToRecyclerView(mRecyclerView);
            mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_MULTIPLE);

            mRecentAdapter = new RecentAdapter(getContext(), mFileInfoList, null, 0, null, /* null */ mItemSelectionHelper);
            mRecentAdapter.setShowInfoButton(false);
            mRecentAdapter.setShowFavoriteIndicator(false);
            mRecentAdapter.updateSpanCount(0); // Need to set ThumbnailWorker's sizes

            if (mViewMode == VIEW_MODE_RECENT || mViewMode == VIEW_MODE_FAVORITE) {
                mRecyclerView.setAdapter(mRecentAdapter);
            }

            if (mMultipleFilesListener != null || mSingleFileListener != null || mLocalFolderListener != null) {
                mLocalAdapter = new LocalFileAdapter(getContext(), mFileInfoList, null, 0, null, /* null */ mItemSelectionHelper);
                mLocalAdapter.setShowInfoButton(false);
                mLocalAdapter.setShowFavoriteIndicator(false);
                mLocalAdapter.updateSpanCount(0);
            }

            if (mMultipleFilesListener != null || mSingleFileListener != null || mExternalFolderListener != null) {
                mExternalAdapter = new ExternalStorageAdapter(getContext(), mExternalFileList, null, 0, null, /* null */ mItemSelectionHelper);
                mExternalAdapter.setShowInfoButton(false);
                mExternalAdapter.setShowFavoriteIndicator(false);
                mExternalAdapter.updateSpanCount(0);
            }

            if (mViewMode == VIEW_MODE_LOCAL) {
                mRecyclerView.setAdapter(mLocalAdapter);
            } else {
                mRecyclerView.setAdapter(mExternalAdapter);
            }


            ItemClickHelper itemClickHelper = new ItemClickHelper();
            itemClickHelper.attachToRecyclerView(mRecyclerView);

            itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
                @Override
                public void onItemClick(RecyclerView parent, View view, int position, long id) {
                    handleItemClick(parent, view, position, id);
                }
            });

            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                private int oldState = RecyclerView.SCROLL_STATE_IDLE;

                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE &&
                        (oldState == RecyclerView.SCROLL_STATE_SETTLING || oldState == RecyclerView.SCROLL_STATE_DRAGGING)) {
                        if (mViewMode == VIEW_MODE_LOCAL) {
                            if (mLocalAdapter != null) {
                                mLocalAdapter.cancelAllThumbRequests();
                                Utils.safeNotifyDataSetChanged(mLocalAdapter);
                            }
                        } else {
                            if (mExternalAdapter != null) {
                                mExternalAdapter.cancelAllThumbRequests();
                                Utils.safeNotifyDataSetChanged(mExternalAdapter);
                            }
                        }
                    }
                    oldState = newState;
                }
            });

            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mViewMode == VIEW_MODE_LOCAL) {
                        mCurrentFolder = mCurrentFolder.getParentFile();
                    } else {
                        mCurrentExternalFolder = mCurrentExternalFolder.getParent();
                    }
                    refreshFileInfoList();
                    updateTitle();
                }
            });

            mFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Utils.isLollipop()) {
                        // Launch standard picker to select a folder
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        // Force advanced devices (SD cards) to always be visible
                        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
                        // Only show local storage devices
                        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                        startActivityForResult(intent, RequestCode.DOCUMENT_TREE);
                    }
                }
            });

            builder.setView(dialogLayout);
        }

        return builder.create();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        MiscUtils.handleLowMemory(getContext(), mLocalAdapter);
        MiscUtils.handleLowMemory(getContext(), mExternalAdapter);
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_LOW_MEMORY, AnalyticsParam.lowMemoryParam(TAG));
        Logger.INSTANCE.LogE(TAG, "low memory");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload list if it was cancelled
        if (mPopulateFileListTask == null || mPopulateFileListTask.isCancelled()) {
            refreshFileInfoList();
        }
    }

    @Override
    public void onPause() {
        if (mPopulateFileListTask != null) {
            mPopulateFileListTask.cancel(true);
        }
        super.onPause();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Context context = getContext();
        if (context == null) {
            return;
        }

        if (requestCode == RequestCode.DOCUMENT_TREE) {
            if (Utils.isLollipop()) {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Uri treeUri = data.getData();

                    if (treeUri != null) {
                        int fabVisibility = View.GONE;
                        try {
                            ContentResolver contentResolver = Utils.getContentResolver(context);
                            if (contentResolver == null) {
                                return;
                            }
                            contentResolver.takePersistableUriPermission(treeUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        } catch (SecurityException e) {
                            // Failure taking permissions
                            CommonToast.showText(context, R.string.error_failed_to_open_document_tree, Toast.LENGTH_SHORT);
                            fabVisibility = View.VISIBLE;
                        }

                        mFab.setVisibility(fabVisibility);
                        refreshFileInfoList();
                    }
                } else {
                    CommonToast.showText(context, R.string.error_failed_to_open_document_tree, Toast.LENGTH_SHORT);
                }
            }
        }
    }

    protected void handleItemClick(RecyclerView parent, View view, int position, long id) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (mViewMode == VIEW_MODE_RECENT || mViewMode == VIEW_MODE_FAVORITE) {
            FileInfo fileInfo = mFileInfoList.get(position);

            if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_FOLDER) {
                if (mDialogMode == DIALOG_MODE_FOLDERS) {
                    cancelAllThumbRequests();
                    if (mLocalFolderListener != null) {
                        saveLastAccessLocation(true, BaseFileInfo.FILE_TYPE_FOLDER, fileInfo.getAbsolutePath());
                        mLocalFolderListener.onLocalFolderSelected(mRequestCode, mCustomParcelable != null ? mCustomParcelable : mCustomString, fileInfo.getFile());
                    }
                    dismiss();
                    return;
                } else {
                    CommonToast.showText(context, R.string.dialog_file_picker_error_select_file, Toast.LENGTH_SHORT);
                }
            } else if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                if (mDialogMode == DIALOG_MODE_FILES) {
                    cancelAllThumbRequests();
                    adjustItemChoices(position, fileInfo);
                    return;
                } else {
                    CommonToast.showText(context, R.string.dialog_file_picker_error_select_folder, Toast.LENGTH_SHORT);
                }
            } else if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                ExternalFileInfo externalFile = Utils.buildExternalFile(context,
                    Uri.parse(fileInfo.getAbsolutePath()));

                if (externalFile == null) {
                    CommonToast.showText(context, R.string.error_opening_file, Toast.LENGTH_SHORT);
                    return;
                }

                if (externalFile.isDirectory()) {
                    if (mDialogMode == DIALOG_MODE_FOLDERS) {
                        cancelAllThumbRequests();
                        if (mExternalFolderListener != null) {
                            saveLastAccessLocation(true, BaseFileInfo.FILE_TYPE_EXTERNAL, externalFile.getAbsolutePath());
                            mExternalFolderListener.onExternalFolderSelected(mRequestCode, mCustomParcelable != null ? mCustomParcelable : mCustomString, externalFile);
                        }
                        dismiss();
                        return;
                    } else {
                        CommonToast.showText(context, R.string.dialog_file_picker_error_select_file, Toast.LENGTH_SHORT);
                    }
                } else {
                    if (mDialogMode == DIALOG_MODE_FILES) {
                        cancelAllThumbRequests();
                        adjustExternalItemChoices(position, externalFile);
                        return;
                    } else {
                        CommonToast.showText(context, R.string.dialog_file_picker_error_select_folder, Toast.LENGTH_SHORT);
                    }
                }
            } // else Do nothing - file type not supported
        } else if (mViewMode == VIEW_MODE_LOCAL) {
            FileInfo fileInfo = mFileInfoList.get(position);

            if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_FOLDER) {
                mCurrentFolder = fileInfo.getFile();
                refreshFileInfoList();
            } else if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                if (mDialogMode == DIALOG_MODE_FILES) {
                    cancelAllThumbRequests();
                    adjustItemChoices(position, fileInfo);
                    return;
                } // Do nothing if in folder-mode (files should not be visible)
            }
        } else /* if (mViewMode == VIEW_MODE_EXTERNAL) */ {
            ExternalFileInfo externalFile = mExternalFileList.get(position);

            if (externalFile.isDirectory()) {
                mCurrentExternalFolder = externalFile;
                refreshFileInfoList();
            } else {
                if (mDialogMode == DIALOG_MODE_FILES) {
                    cancelAllThumbRequests();
                    adjustExternalItemChoices(position, externalFile);
                    return;
                } // Do nothing if in folder-mode (files should not be visible)
            }
        }
        updateTitle();
    }

    public void cancelAllThumbRequests() {
        if (mLocalAdapter != null) {
            mLocalAdapter.cancelAllThumbRequests(true);
        }
        if (mExternalAdapter != null) {
            mExternalAdapter.cancelAllThumbRequests(true);
        }
        if (mRecentAdapter != null) {
            mRecentAdapter.cancelAllThumbRequests(true);
        }
    }

    protected void saveLastAccessLocation(boolean folder, int fileType, String path) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (folder) {
            PdfViewCtrlSettingsManager.updateSavedFolderPickerFileType(context, fileType);
            PdfViewCtrlSettingsManager.updateSavedFolderPickerLocation(context, path);
        } else {
            PdfViewCtrlSettingsManager.updateSavedFilePickerFileType(context, fileType);
            PdfViewCtrlSettingsManager.updateSavedFilePickerLocation(context, path);
        }
    }

    protected void remakeItemChoices(ArrayList<FileInfo> fileInfos) {
        mItemSelectionHelper.clearChoices();
        for (int position = 0; position < fileInfos.size(); ++position) {
            FileInfo fileInfo = fileInfos.get(position);
            String absolutePath = fileInfo.getAbsolutePath();
            if (fileInfo.isDirectory()) {
                if (mSelectedFoldersToFileCount.containsKey(absolutePath)) {
                    mItemSelectionHelper.setItemChecked(position, true);
                }
            } else /*is file*/ {
                if (mSelectedFiles.contains(absolutePath)) {
                    mItemSelectionHelper.setItemChecked(position, true);
                }
            }
        }
    }

    protected void remakeExternalItemChoices(ArrayList<ExternalFileInfo> externalFileInfos) {
        mItemSelectionHelper.clearChoices();
        for (int position = 0; position < externalFileInfos.size(); ++position) {
            ExternalFileInfo externalFileInfo = externalFileInfos.get(position);
            String absolutePath = externalFileInfo.getAbsolutePath();
            if (externalFileInfo.isDirectory()) {
                if (mSelectedFoldersToFileCount.containsKey(absolutePath)) {
                    mItemSelectionHelper.setItemChecked(position, true);
                }
            } else /*is file*/ {
                if (mSelectedFiles.contains(absolutePath)) {
                    mItemSelectionHelper.setItemChecked(position, true);
                }
            }
        }
    }

    protected void adjustItemChoices(int position, FileInfo fileInfo) {
        if (mFileInfoSelectedList.contains(fileInfo)) {
            mFileInfoSelectedList.remove(fileInfo);
            setContinueButtonProperties(mFileInfoSelectedList.size());
            mSelectedFiles.remove(fileInfo.getAbsolutePath());
            String[] parentPathParts = fileInfo.getAbsolutePath().split("/");
            String ancestorPath = "";
            for (int i = 1; i < parentPathParts.length - 1; ++i) {
                ancestorPath += ("/" + parentPathParts[i]);
                if (mSelectedFoldersToFileCount.containsKey(ancestorPath)) {
                    int pathSelectedFileCount = mSelectedFoldersToFileCount.get(ancestorPath) - 1;
                    mSelectedFoldersToFileCount.remove(ancestorPath);
                    if (pathSelectedFileCount > 0) {
                        mSelectedFoldersToFileCount.put(ancestorPath, pathSelectedFileCount);
                    }
                } else {
                    Logger.INSTANCE.LogW(TAG, "at adjustItemChoices, must not occur");
                }
            }
            mItemSelectionHelper.setItemChecked(position, false);
        } else {
            if (mSingleFileListener != null) {
                mItemSelectionHelper.clearChoices();
                mFileInfoSelectedList.clear();
                mSelectedFiles.clear();
            }
            mFileInfoSelectedList.add(fileInfo);
            mSelectedFiles.add(fileInfo.getAbsolutePath());
            setContinueButtonProperties(mFileInfoSelectedList.size());
            String[] parentPathParts = fileInfo.getAbsolutePath().split("/");
            String ancestorPath = "";
            for (int i = 1; i < parentPathParts.length - 1; ++i) {
                ancestorPath += ("/" + parentPathParts[i]);
                if (mSelectedFoldersToFileCount.containsKey(ancestorPath)) {
                    int pathSelectedFileCount = mSelectedFoldersToFileCount.get(ancestorPath) + 1;
                    mSelectedFoldersToFileCount.remove(ancestorPath);
                    mSelectedFoldersToFileCount.put(ancestorPath, pathSelectedFileCount);
                } else {
                    mSelectedFoldersToFileCount.put(ancestorPath, 1);
                }
            }
            mItemSelectionHelper.setItemChecked(position, true);
        }
    }

    protected void adjustExternalItemChoices(int position, ExternalFileInfo externalFileInfo) {
        FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL,
            externalFileInfo.getUri().toString(), externalFileInfo.getFileName(), false, 1);
        adjustItemChoices(position, fileInfo);
    }

    protected void updateToolbarMenu(Menu menu) {
        if (menu != null) {
            MenuItem itemCreateFolder = menu.findItem(R.id.item_create_folder);
            if (itemCreateFolder != null) {
                itemCreateFolder.setVisible(mDialogMode == DIALOG_MODE_FOLDERS);
                if (mViewMode == VIEW_MODE_RECENT || mViewMode == VIEW_MODE_FAVORITE) {
                    itemCreateFolder.setEnabled(false);
                } else if (mViewMode == VIEW_MODE_LOCAL) {
                    itemCreateFolder.setEnabled(true);
                } else {
                    // Only enable create-folder button when roots are available
                    itemCreateFolder.setEnabled(mRootsAvailable);
                }
            }
            MenuItem itemRecentFiles = menu.findItem(R.id.item_recent_files);
            if (itemRecentFiles != null) {
                // Only show recent files item if in file-selection mode
                itemRecentFiles.setVisible(mDialogMode == DIALOG_MODE_FILES);
            }
            MenuItem itemFavoriteFiles = menu.findItem(R.id.item_favorite_files);
            if (itemFavoriteFiles != null) {
                // Favorite files item is always visible
                itemFavoriteFiles.setVisible(true);
            }
            MenuItem itemLocalStorage = menu.findItem(R.id.item_local_storage);
            MenuItem itemExternalStorage = menu.findItem(R.id.item_external_storage);
            // Show mode toggle if both listeners of a pair (file & file OR folder & folder) are set
            boolean showLocal = false;
            boolean showExternal = false;
            if (mMultipleFilesListener != null || mSingleFileListener != null) {
                showLocal = true;
                showExternal = true;
            }
            if (mLocalFolderListener != null) {
                showLocal = true;
            }
            if (mExternalFolderListener != null) {
                showExternal = true;
            }
            if (showLocal) {
                if (itemLocalStorage != null) {
                    itemLocalStorage.setVisible(true);
                    if (mViewMode == VIEW_MODE_LOCAL) {
                        itemLocalStorage.setChecked(true);
                    }
                }
            } else {
                if (itemLocalStorage != null) {
                    itemLocalStorage.setVisible(false);
                }
            }
            if (showExternal) {
                if (itemExternalStorage != null) {
                    if (Utils.isLollipop()) {
                        itemExternalStorage.setVisible(true);
                        if (mViewMode == VIEW_MODE_EXTERNAL) {
                            itemExternalStorage.setChecked(true);
                        }
                    } else {
                        // hide SD card option for lower APIs
                        itemExternalStorage.setVisible(false);
                    }
                }
            } else {
                if (itemExternalStorage != null) {
                    itemExternalStorage.setVisible(false);
                }
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        boolean modeChanged;
        if (item.getItemId() == R.id.item_recent_files) {
            item.setChecked(true);
            modeChanged = (mViewMode != VIEW_MODE_RECENT);
            mViewMode = VIEW_MODE_RECENT;
            if (modeChanged) {
                cancelAllThumbRequests();
                mRecyclerView.swapAdapter(mRecentAdapter, true);
                Utils.safeNotifyDataSetChanged(mRecentAdapter);
                mEmptyView.setVisibility(View.GONE);
                mFab.setVisibility(View.GONE);
                refreshFileInfoList();
                updateTitle();
            }
        } else if (item.getItemId() == R.id.item_favorite_files) {
            item.setChecked(true);
            modeChanged = (mViewMode != VIEW_MODE_FAVORITE);
            mViewMode = VIEW_MODE_FAVORITE;
            if (modeChanged) {
                cancelAllThumbRequests();
                mRecyclerView.swapAdapter(mRecentAdapter, true);
                Utils.safeNotifyDataSetChanged(mRecentAdapter);
                mEmptyView.setVisibility(View.GONE);
                mFab.setVisibility(View.GONE);
                refreshFileInfoList();
                updateTitle();
            }
        }
        if (item.getItemId() == R.id.item_create_folder) {
            if (mViewMode == VIEW_MODE_LOCAL) {
                FileManager.createFolder(activity, mCurrentFolder, mFMListener);
            } else {
                // Creating folders is only supported under a root folder
                if (mCurrentExternalFolder != null) {
                    ExternalFileManager.createFolder(activity, mCurrentExternalFolder, mExternalFMListener);
                }
            }
        } else if (item.getItemId() == R.id.item_local_storage) {
            item.setChecked(true);
            modeChanged = (mViewMode != VIEW_MODE_LOCAL);
            mViewMode = VIEW_MODE_LOCAL;
            if (modeChanged) {
                cancelAllThumbRequests();
                mRecyclerView.swapAdapter(mLocalAdapter, true);
                Utils.safeNotifyDataSetChanged(mLocalAdapter);
                mEmptyView.setVisibility(View.GONE);
                mFab.setVisibility(View.GONE);
                refreshFileInfoList();
                updateTitle();
            }
        } else if (item.getItemId() == R.id.item_external_storage) {
            if (Utils.isLollipop()) {
                item.setChecked(true);
                modeChanged = (mViewMode != VIEW_MODE_EXTERNAL);
                mViewMode = VIEW_MODE_EXTERNAL;
                if (modeChanged) {
                    cancelAllThumbRequests();
                    mRecyclerView.swapAdapter(mExternalAdapter, true);
                    Utils.safeNotifyDataSetChanged(mExternalAdapter);
                    mEmptyView.setVisibility(View.GONE);
                    refreshFileInfoList();
                    updateTitle();
                }
            }
        }
        return true;
    }

    public void updateTitle() {
        String title = "";
        String subTitle = "";
        switch (mViewMode) {
            case VIEW_MODE_RECENT:
                title = getString(R.string.title_item_recent);
                break;
            case VIEW_MODE_FAVORITE:
                title = getString(R.string.title_item_favorites);
                break;
            case VIEW_MODE_LOCAL:
                title = getString(R.string.local_storage);
                if (mCurrentFolder != null) {
                    subTitle = mCurrentFolder.getAbsolutePath();
                }
                break;
            case VIEW_MODE_EXTERNAL:
                title = getString(R.string.external_storage);
                subTitle = buildPath();
                break;
        }
        if (mToolbar != null) {
            // Update the toolbar's title
            mToolbar.setTitle(title);
            mToolbar.setSubtitle(subTitle);
        }
        if (mDialogMode == DIALOG_MODE_FOLDERS) {
            // update positive button
            setContinueButtonProperties(0);
        }
    }

    public String buildPath() {
        StringBuilder result = new StringBuilder();
        String separator = "";
        for (ExternalFileInfo parent = mCurrentExternalFolder; parent != null; parent = parent.getParent()) {
            result.insert(0, parent.getFileName() + separator);
            separator = "/";
        }
        String rootName = getString(R.string.external_storage);
        if (!Utils.isNullOrEmpty(result.toString())) {
            result.insert(0, rootName + "/");
        } else {
            result = new StringBuilder(rootName);
        }
        return result.toString();
    }

    protected void refreshFileInfoList() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (mLocalAdapter != null) {
            mLocalAdapter.cancelAllThumbRequests(true);
        }
        if (mExternalAdapter != null) {
            mExternalAdapter.cancelAllThumbRequests(true);
        }
        if (mRecentAdapter != null) {
            mRecentAdapter.cancelAllThumbRequests(true);
        }

        // cancel previous task
        if (mPopulateFileListTask != null) {
            mPopulateFileListTask.cancel(true);
        }
        switch (mViewMode) {
            case VIEW_MODE_LOCAL:
                mPopulateFileListTask = new PopulateFolderTask(context, mCurrentFolder,
                    mFileInfoList, mFileListLock, FileInfoComparator.folderPathOrder(),
                    mDialogMode == DIALOG_MODE_FILES, false, false, null, this);
                ((PopulateFolderTask) mPopulateFileListTask).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case VIEW_MODE_EXTERNAL:
                if (Utils.isLollipop()) {
                    mPopulateFileListTask = new PopulateSdFolderTask(
                        context, mExternalFileList, mFileListLock, mExternalRoots, false,
                        null, mCurrentExternalFolder, FileInfoComparator.externalPathOrder(),
                        null, null, mDialogMode == DIALOG_MODE_FILES, false, this);
                    ((PopulateSdFolderTask) mPopulateFileListTask).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                break;
            case VIEW_MODE_RECENT:
                mPopulateFileListTask = new PopulateRecentFileListTask(getContext());
                ((PopulateRecentFileListTask) mPopulateFileListTask).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case VIEW_MODE_FAVORITE:
                mPopulateFileListTask = new PopulateFavoriteFileListTask(getContext());
                ((PopulateFavoriteFileListTask) mPopulateFileListTask).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
        }
    }

    @Override
    public void onPopulateFolderTaskStarted() {
        if (mProgressBarLayout != null) {
            mProgressBarLayout.show();
        }
    }

    @Override
    public void onPopulateFolderTaskProgressUpdated(File currentFolder) {
        Context context = getContext();
        if (context == null || !(mPopulateFileListTask instanceof PopulateFolderTask)) {
            return;
        }

        remakeItemChoices(mFileInfoList);

        if (mToolbar != null) {
            // Navigate up through directories
            File rootDir = Environment.getExternalStorageDirectory();
            if (Utils.isLollipop()) {
                while (rootDir != null && rootDir.getParentFile() != null && !rootDir.getParentFile().getAbsolutePath().equalsIgnoreCase("/")) {
                    rootDir = rootDir.getParentFile();
                }
            } else {
                while (rootDir != null && rootDir.getParentFile() != null) {
                    rootDir = rootDir.getParentFile();
                }
            }
            boolean mShowNavButton = !currentFolder.equals(rootDir);
            if (mShowNavButton) {
                Drawable icon = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
                icon.mutate().setColorFilter(Utils.isDeviceNightMode(context) ? Color.WHITE : Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                mToolbar.setNavigationIcon(icon);
            } else {
                mToolbar.setNavigationIcon(null);
            }
            updateToolbarMenu(mToolbar.getMenu());
        }

        if (mEmptyView != null) {
            if (mFileInfoList.size() > 0) {
                mEmptyView.setVisibility(View.GONE);
            } else {
                mEmptyView.setText(R.string.textview_empty_file_list);
                mEmptyView.setVisibility(View.VISIBLE);
            }
        }
        if (mProgressBarLayout != null) {
            mProgressBarLayout.hide(false);
        }

        Utils.safeNotifyDataSetChanged(mLocalAdapter);
    }

    @Override
    public void onPopulateFolderTaskFinished() {
        // already done everything in onPopulateFolderTaskProgressUpdated
    }

    @Override
    public void onPopulateSdFilesTaskStarted() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        synchronized (mFileListLock) {
            mExternalFileList.clear();
        }

        if (mProgressBarLayout != null) {
            mProgressBarLayout.show();
        }

        if (mToolbar != null) {
            if (mCurrentExternalFolder != null) {
                Drawable icon = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
                icon.mutate().setColorFilter(Utils.isDeviceNightMode(getContext()) ? Color.WHITE : Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                mToolbar.setNavigationIcon(icon);
            } else {
                mToolbar.setNavigationIcon(null);
            }
        }
    }

    @Override
    public void onCurrentRootRemoved() {
        mCurrentExternalFolder = null;
    }

    @Override
    public void onPopulateSdFilesTaskProgressUpdated(ExternalFileInfo
                                                         savedRoot, ExternalFileInfo savedFolder, ExternalFileInfo savedLeaf) {

    }

    @Override
    public void onPopulateSdFilesTaskProgressUpdated(List<ExternalFileInfo> rootList) {

    }

    @Override
    public void onPopulateSdFilesTaskFinished() {
        remakeExternalItemChoices(mExternalFileList);

        if (mEmptyView != null) {
            if (mExternalFileList.size() > 0) {
                mEmptyView.setVisibility(View.GONE);
            } else {
                if (mRootsAvailable) {
                    mEmptyView.setText(R.string.textview_empty_file_list);
                } else {
                    mEmptyView.setText(R.string.textview_empty_no_external_roots);
                    if (mFab != null) {
                        mFab.setVisibility(View.VISIBLE);
                    }
                }
                mEmptyView.setVisibility(View.VISIBLE);
            }
        }
        if (mToolbar != null) {
            updateToolbarMenu(mToolbar.getMenu());
        }
        if (mProgressBarLayout != null) {
            mProgressBarLayout.hide(false);
        }

        // Notify the adapter that our data has changed: this will update
        // the list with the new data.
        Utils.safeNotifyDataSetChanged(mExternalAdapter);
    }

    private FileManagementListener mFMListener = new FileManagementListener() {
        @Override
        public void onFileRenamed(FileInfo oldFile, FileInfo newFile) {

        }

        @Override
        public void onFileDuplicated(File fileCopy) {

        }

        @Override
        public void onFileDeleted(ArrayList<FileInfo> deletedFiles) {

        }

        @Override
        public void onFileMoved(Map<FileInfo, Boolean> filesMoved, File targetFolder) {

        }

        @Override
        public void onFolderCreated(FileInfo rootFolder, FileInfo newFolder) {
            mCurrentFolder = newFolder.getFile();
            refreshFileInfoList();
            updateTitle();
        }

        @Override
        public void onFileMoved(Map<FileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {

        }

        @Override
        public void onFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {
            MiscUtils.removeFiles(filesToDelete);
        }

        @Override
        public void onFileChanged(String path, int event) {

        }

        @Override
        public void onFileClicked(FileInfo fileInfo) {

        }
    };

    private ExternalFileManagementListener mExternalFMListener = new ExternalFileManagementListener() {
        @Override
        public void onExternalFileRenamed(ExternalFileInfo oldFile, ExternalFileInfo newFile) {

        }

        @Override
        public void onExternalFileDuplicated(ExternalFileInfo fileCopy) {

        }

        @Override
        public void onExternalFileDeleted(ArrayList<ExternalFileInfo> deletedFiles) {

        }

        @Override
        public void onRootsRemoved(ArrayList<ExternalFileInfo> deletedFiles) {

        }

        @Override
        public void onExternalFileMoved(Map<ExternalFileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {

        }

        @Override
        public void onExternalFileMoved(Map<ExternalFileInfo, Boolean> filesMoved, File targetFolder) {

        }

        @Override
        public void onExternalFolderCreated(ExternalFileInfo rootFolder, ExternalFileInfo newFolder) {
            mCurrentExternalFolder = newFolder;
            refreshFileInfoList();
            updateTitle();
        }

        @Override
        public void onExternalFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {

        }
    };

    // TODO move it to {@link com.pdftron.demo.asynctask}
    protected class PopulateRecentFileListTask extends CustomAsyncTask<Void, Void, Void> {

        private ArrayList<FileInfo> fileInfoList;

        PopulateRecentFileListTask(Context context) {
            super(context);
            fileInfoList = new ArrayList<>();
        }

        @Override
        protected void onPreExecute() {
            if (mProgressBarLayout != null) {
                mProgressBarLayout.show();
            }
            if (mToolbar != null) {
                mToolbar.setNavigationIcon(null);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            ArrayList<FileInfo> filesToRemove = new ArrayList<>();
            fileInfoList.addAll(getRecentFilesManager().getFiles(getContext()));
            for (FileInfo fileInfo : fileInfoList) {
                if (fileInfo != null) {
                    if (mDialogMode == DIALOG_MODE_FILES) {
                        // Remove any non-file items
                        if (!canAdd(fileInfo)) {
                            filesToRemove.add(fileInfo);
                        }
                    } else {
                        // Remove any non-folder items
                        if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                            String mimeType = ExternalFileInfo.getUriMimeType(getContext(), fileInfo.getAbsolutePath());
                            if (!Utils.isNullOrEmpty(mimeType) && !DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                                // External file
                                filesToRemove.add(fileInfo);
                            }
                        } else if (fileInfo.getType() != BaseFileInfo.FILE_TYPE_FOLDER) {
                            filesToRemove.add(fileInfo);
                        }
                    }
                }
            }
            if (filesToRemove.size() > 0) {
                fileInfoList.removeAll(filesToRemove);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mFileInfoList.clear();
            mFileInfoList.addAll(fileInfoList);
            remakeItemChoices(mFileInfoList);

            if (mToolbar != null) {
                updateToolbarMenu(mToolbar.getMenu());
            }

            if (mEmptyView != null) {
                if (mFileInfoList.size() > 0) {
                    mEmptyView.setVisibility(View.GONE);
                } else {
                    mEmptyView.setText(R.string.textview_empty_recent_list);
                    mEmptyView.setVisibility(View.VISIBLE);
                }
            }
            if (mProgressBarLayout != null) {
                mProgressBarLayout.hide(false);
            }

            Utils.safeNotifyDataSetChanged(mRecentAdapter);
        }
    }

    // TODO move it to {@link com.pdftron.demo.asynctask}
    protected class PopulateFavoriteFileListTask extends CustomAsyncTask<Void, Void, Void> {

        private ArrayList<FileInfo> fileInfoList;

        PopulateFavoriteFileListTask(Context context) {
            super(context);
            fileInfoList = new ArrayList<>();
        }

        @Override
        protected void onPreExecute() {
            if (mProgressBarLayout != null) {
                mProgressBarLayout.show();
            }
            if (mToolbar != null) {
                mToolbar.setNavigationIcon(null);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            ArrayList<FileInfo> filesToRemove = new ArrayList<>();
            fileInfoList.addAll(getFavoriteFilesManager().getFiles(getContext()));
            for (FileInfo fileInfo : fileInfoList) {
                if (fileInfo != null) {
                    if (mDialogMode == DIALOG_MODE_FILES) {
                        // Remove any non-file items
                        if (!canAdd(fileInfo)) {
                            filesToRemove.add(fileInfo);
                        }
                    } else {
                        // Remove any non-folder items
                        if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                            String mimeType = ExternalFileInfo.getUriMimeType(getContext(), fileInfo.getAbsolutePath());
                            if (!Utils.isNullOrEmpty(mimeType) && !DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                                // External file
                                filesToRemove.add(fileInfo);
                            }
                        } else if (fileInfo.getType() != BaseFileInfo.FILE_TYPE_FOLDER) {
                            filesToRemove.add(fileInfo);
                        }
                    }
                }
            }
            if (filesToRemove.size() > 0) {
                fileInfoList.removeAll(filesToRemove);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mFileInfoList.clear();
            mFileInfoList.addAll(fileInfoList);
            remakeItemChoices(mFileInfoList);

            if (mToolbar != null) {
                updateToolbarMenu(mToolbar.getMenu());
            }

            if (mEmptyView != null) {
                if (mFileInfoList.size() > 0) {
                    mEmptyView.setVisibility(View.GONE);
                } else {
                    if (mDialogMode == DIALOG_MODE_FILES) {
                        mEmptyView.setText(R.string.textview_empty_favorite_list);
                    } else {
                        mEmptyView.setText(R.string.textview_empty_favorite_folder_list);
                    }
                    mEmptyView.setVisibility(View.VISIBLE);
                }
            }
            if (mProgressBarLayout != null) {
                mProgressBarLayout.hide(false);
            }

            Utils.safeNotifyDataSetChanged(mRecentAdapter);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected boolean canAdd(FileInfo fileInfo) {
        if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
            String mimeType = ExternalFileInfo.getUriMimeType(getContext(), fileInfo.getAbsolutePath());
            return !Utils.isNullOrEmpty(mimeType) && !DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType);
        }
        return true;
    }
}
