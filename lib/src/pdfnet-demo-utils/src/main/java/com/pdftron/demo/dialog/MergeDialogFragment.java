//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.pdftron.demo.R;
import com.pdftron.demo.R2;
import com.pdftron.demo.navigation.adapter.FavoriteAdapter;
import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.ToolbarActionMode;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.ItemSelectionHelper;
import com.pdftron.pdf.widget.recyclerview.ItemTouchHelperCallback;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import com.pdftron.sdf.Obj;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * A dialog responsible for merging multiple files to one PDF file.
 */
public class MergeDialogFragment extends DialogFragment implements
    Toolbar.OnMenuItemClickListener,
    FavoriteAdapter.AdapterListener,
    FilePickerDialogFragment.MultipleFilesListener {

    public interface MergeDialogFragmentListener {
        void onMergeConfirmed(ArrayList<FileInfo> filesToMerge, ArrayList<FileInfo> filesToDelete, String title);
    }

    protected static final String BUNDLE_FILE_TYPES = "file_types";
    protected static final String BUNDLE_FILE_PATHS = "file_paths";
    protected static final String BUNDLE_FILE_NAMES = "file_names";
    protected static final String BUNDLE_SCREEN_ID = "screen_id";

    protected static final int SELECT_FILE_REQUEST = 0;

    protected ArrayList<FileInfo> mFileList = new ArrayList<>();
    protected ArrayList<FileInfo> mTempFileList = new ArrayList<>();
    protected ArrayList<FileInfo> mSelectedFileList = new ArrayList<>();

    private boolean mDoRemoveTemp = true;

    private Uri mOutputFileUri;

    @BindView(R2.id.fragment_merge_dialog_toolbar)
    Toolbar mToolbar;
    @BindView(R2.id.fragment_merge_dialog_cab)
    Toolbar mCabToolbar;
    @BindView(R2.id.fragment_merge_dialog_recycler_view)
    SimpleRecyclerView mRecyclerView;

    private Unbinder unbinder;

    private ItemSelectionHelper mItemSelectionHelper;
    private ItemTouchHelper mItemTouchHelper;

    private Menu mOptionsMenu;
    private ToolbarActionMode mActionMode;
    private int mSpanCount;

    private boolean mFirstDrag = true;

    private MergeDialogFragmentListener mListener;

    protected FavoriteAdapter mAdapter;
    protected int mInitialNumInvalid = 0;
    private int mScreenId;

    public MergeDialogFragment() {
    }

    public static MergeDialogFragment newInstance(ArrayList<FileInfo> files, int screenId) {
        MergeDialogFragment dialogFragment = new MergeDialogFragment();
        Bundle args = new Bundle();
        if (files != null && !files.isEmpty()) {
            ArrayList<Integer> fileTypes = new ArrayList<>();
            ArrayList<String> filePaths = new ArrayList<>();
            ArrayList<String> filenames = new ArrayList<>();
            for (FileInfo file : files) {
                if (file.getType() == BaseFileInfo.FILE_TYPE_FILE ||
                    file.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                    file.getType() == BaseFileInfo.FILE_TYPE_EDIT_URI ||
                    file.getType() == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
                    // Directories should not be included in list
                    fileTypes.add(file.getType());
                    filePaths.add(file.getAbsolutePath());
                    filenames.add(file.getName());
                }
            }
            args.putIntegerArrayList(BUNDLE_FILE_TYPES, fileTypes);
            args.putStringArrayList(BUNDLE_FILE_PATHS, filePaths);
            args.putStringArrayList(BUNDLE_FILE_NAMES, filenames);
        }
        args.putInt(BUNDLE_SCREEN_ID, screenId);
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    // This is not the ideal solution to avoid the error/warning when using a non-default
    // constructor, but since we are retaining configuration in the manifest this should be fine.
    public void initParams(MergeDialogFragmentListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDoRemoveTemp = true;

        if (savedInstanceState != null) {
            mOutputFileUri = savedInstanceState.getParcelable("output_file_uri");
            mInitialNumInvalid = savedInstanceState.getInt("initial_invalid_count", 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mInitialNumInvalid > 0) {
            Utils.showAlertDialog(getActivity(), getResources().getQuantityString(R.plurals.dialog_merge_invalid_file_message, mInitialNumInvalid), null);
            mInitialNumInvalid = 0;
        }
        updateSpanCount(mSpanCount);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mOutputFileUri != null) {
            outState.putParcelable("output_file_uri", mOutputFileUri);
            outState.putInt("initial_invalid_count", mInitialNumInvalid);
        }
    }

    @SuppressLint("InflateParams")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_merge_dialog, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        unbinder = ButterKnife.bind(this, view);
        Bundle arg = getArguments();
        if (arg != null) {
            ArrayList<Integer> fileTypes = arg.getIntegerArrayList(BUNDLE_FILE_TYPES);
            ArrayList<String> filePaths = arg.getStringArrayList(BUNDLE_FILE_PATHS);
            ArrayList<String> fileNames = arg.getStringArrayList(BUNDLE_FILE_NAMES);
            mScreenId = arg.getInt(BUNDLE_SCREEN_ID);

            if ((fileTypes != null && filePaths != null && fileNames != null) &&
                (fileTypes.size() == filePaths.size() && filePaths.size() == fileNames.size())) {
                addInitialFiles(fileTypes, filePaths, fileNames);
                if (mInitialNumInvalid > 0) {
                    if (mInitialNumInvalid < fileTypes.size()) {
                        // if only some file is invalid, show plural message
                        mInitialNumInvalid++;
                    }
                }
            }
        }

        final Drawable backArrow = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
        mToolbar.setNavigationIcon(backArrow);
        mToolbar.setNavigationContentDescription(R.string.cancel);
        mToolbar.setTitle(R.string.dialog_merge_title);

        mToolbar.inflateMenu(R.menu.fragment_merge_view);
        mToolbar.setOnMenuItemClickListener(this);
        mOptionsMenu = mToolbar.getMenu();
        updateToolbarMenu(mToolbar.getMenu());

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        View toolbarShadow = view.findViewById(R.id.toolbar_shadow);
        if (Utils.isLollipop()) {
            toolbarShadow.setVisibility(View.GONE);
        } else {
            toolbarShadow.setVisibility(View.VISIBLE);
        }

        mSpanCount = PdfViewCtrlSettingsManager.getGridSize(view.getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_MERGE_FILES);
        mRecyclerView.initView(mSpanCount);

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);

        mItemSelectionHelper = new ItemSelectionHelper();
        mItemSelectionHelper.attachToRecyclerView(mRecyclerView);
        mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_MULTIPLE);

        mAdapter = new FavoriteAdapter(view.getContext(), mFileList, null, mSpanCount, this, mItemSelectionHelper);
        mAdapter.setShowInfoButton(false);

        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(mAdapter, mSpanCount, false, false);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        mRecyclerView.setAdapter(mAdapter);
        MiscUtils.updateAdapterViewWidthAfterGlobalLayout(mRecyclerView, mAdapter);

        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                FileInfo fileInfo = mAdapter.getItem(position);
                if (fileInfo == null) {
                    return;
                }

                if (mActionMode == null) {
                    mItemSelectionHelper.setItemChecked(position, false);
                    CommonToast.showText(view.getContext(), fileInfo.getName(), Toast.LENGTH_SHORT);
                } else {
                    if (mSelectedFileList.contains(fileInfo)) {
                        mSelectedFileList.remove(fileInfo);
                        mItemSelectionHelper.setItemChecked(position, false);
                    } else {
                        mSelectedFileList.add(fileInfo);
                        mItemSelectionHelper.setItemChecked(position, true);
                    }

                    if (mSelectedFileList.size() == 0) {
                        finishActionMode();
                    } else {
                        mActionMode.invalidate();
                    }
                }
            }
        });
        itemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView parent, View view, final int position, long id) {
                FileInfo fileInfo = mAdapter.getItem(position);
                if (fileInfo == null) {
                    return false;
                }

                if (mActionMode == null) {
                    mSelectedFileList.add(fileInfo);
                    mItemSelectionHelper.setItemChecked(position, true);

                    mActionMode = new ToolbarActionMode(view.getContext(), mCabToolbar);
                    mActionMode.startActionMode(mActionModeCallback);

                }

                // Allow item's appearance to be updated before dragging
                mRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mFirstDrag)
                            mFirstDrag = false;
                        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
                        mItemTouchHelper.startDrag(holder);
                    }
                }, mFirstDrag ? 333 : 0);

                return true;
            }
        });

        FloatingActionButton fab = view.findViewById(R.id.fragment_merge_dialog_folder_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFileList.size() > 0) {
                    getDocumentTitle();
                } else {
                    String message = getString(R.string.dialog_merge_error_no_files);
                    CommonToast.showText(view.getContext(), message, Toast.LENGTH_SHORT);
                }
            }
        });

        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    // Back key has been pressed
                    if (onBackPressed()) {
                        // Back key handled
                        return true;
                    } else {
                        dialog.dismiss();
                    }
                }
                return false;
            }
        });
    }

    protected void addInitialFiles(ArrayList<Integer> fileTypes, ArrayList<String> filePaths, ArrayList<String> fileNames) {
        mFileList.clear();
        mTempFileList.clear();
        for (int i = 0; i < fileTypes.size(); i++) {
            int type = fileTypes.get(i);
            String path = filePaths.get(i);
            String name = fileNames.get(i);
            FileInfo fileInfo = null;
            if (type == BaseFileInfo.FILE_TYPE_FILE) {
                fileInfo = new FileInfo(type, new File(path));
            } else if (type == BaseFileInfo.FILE_TYPE_EXTERNAL
                || type == BaseFileInfo.FILE_TYPE_EDIT_URI
                || type == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
                fileInfo = new FileInfo(type, path, name, false, 1);
            }
            if (fileInfo != null) {
                if (canAddFile(fileInfo, false)) {
                    mFileList.add(fileInfo);
                } else {
                    mInitialNumInvalid++;
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void updateToolbarMenu(Menu menu) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (menu != null) {
            MenuItem menuItem;
            // Set grid size radio buttons to correct value from settings
            int gridSize = PdfViewCtrlSettingsManager.getGridSize(context, PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_MERGE_FILES);
            if (gridSize == 1) {
                menuItem = menu.findItem(R.id.menu_grid_count_1);
            } else if (gridSize == 2) {
                menuItem = menu.findItem(R.id.menu_grid_count_2);
            } else if (gridSize == 3) {
                menuItem = menu.findItem(R.id.menu_grid_count_3);
            } else if (gridSize == 4) {
                menuItem = menu.findItem(R.id.menu_grid_count_4);
            } else if (gridSize == 5) {
                menuItem = menu.findItem(R.id.menu_grid_count_5);
            } else if (gridSize == 6) {
                menuItem = menu.findItem(R.id.menu_grid_count_6);
            } else {
                menuItem = menu.findItem(R.id.menu_grid_count_0);
            }
            if (menuItem != null) {
                menuItem.setChecked(true);
            }

            updateGridMenuState(menu);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return false;
        }

        boolean handled = false;
        if (item.getItemId() == R.id.menu_add_document) {
//                if (!MiscUtils.hasInternetConnection(getActivity()) || MiscUtils.isInternetConnectionMobile(getActivity())) {
//                    // Ask user if they want to allow non-local file sources in the picker
//                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//                    // Show appropriate message
//                    builder.setMessage(MiscUtils.isInternetConnectionMobile(getActivity()) ?
//                            R.string.dialog_merge_show_local_files_only_mobile : R.string.dialog_merge_show_local_files_only)
//                            .setNegativeButton(R.string.tools_misc_no, new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    // Show local and non-local file sources
//                                    launchAndroidFilePicker(false);
//                                }
//                            })
//                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    // Don't show remote file sources
//                                    launchAndroidFilePicker(true);
//                                }
//                            }).show();
//                } else {
//                    // Show local and non-local file sources
//                    launchAndroidFilePicker(false);
//                }
            handleAddDocument();
            handled = true;
        }
        if (item.getItemId() == R.id.menu_add_image) {
            mOutputFileUri = ViewerUtils.openImageIntent(MergeDialogFragment.this);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_0) {
            item.setChecked(true);
            updateSpanCount(0);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_1) {
            item.setChecked(true);
            updateSpanCount(1);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_2) {
            item.setChecked(true);
            updateSpanCount(2);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_3) {
            item.setChecked(true);
            updateSpanCount(3);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_4) {
            item.setChecked(true);
            updateSpanCount(4);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_5) {
            item.setChecked(true);
            updateSpanCount(5);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_6) {
            item.setChecked(true);
            updateSpanCount(6);
            handled = true;
        }
        return handled;
    }

    protected void handleAddDocument() {
        FilePickerDialogFragment dialog = FilePickerDialogFragment.newInstance(SELECT_FILE_REQUEST, Environment.getExternalStorageDirectory());
        dialog.setMultipleFilesListener(this);
        dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialog.show(fragmentManager, "file_picker_dialog");
        }
    }

    private void updateGridMenuState(Menu menu) {
        if (menu == null) {
            return;
        }
        // Set grid/list icon & text based on current mode
        MenuItem menuItem = menu.findItem(R.id.menu_grid_toggle);
        if (menuItem == null) {
            return;
        }
        MenuItem menuItem1 = menu.findItem(R.id.menu_grid_count_1);
        menuItem1.setTitle(getResources().getString(R.string.columns_count, 1));
        MenuItem menuItem2 = menu.findItem(R.id.menu_grid_count_2);
        menuItem2.setTitle(getResources().getString(R.string.columns_count, 2));
        MenuItem menuItem3 = menu.findItem(R.id.menu_grid_count_3);
        menuItem3.setTitle(getResources().getString(R.string.columns_count, 3));
        MenuItem menuItem4 = menu.findItem(R.id.menu_grid_count_4);
        menuItem4.setTitle(getResources().getString(R.string.columns_count, 4));
        MenuItem menuItem5 = menu.findItem(R.id.menu_grid_count_5);
        menuItem5.setTitle(getResources().getString(R.string.columns_count, 5));
        MenuItem menuItem6 = menu.findItem(R.id.menu_grid_count_6);
        menuItem6.setTitle(getResources().getString(R.string.columns_count, 6));
        if (mSpanCount > 0) {
            // In grid mode
            menuItem.setTitle(R.string.dialog_add_page_grid);
            menuItem.setIcon(R.drawable.ic_view_module_white_24dp);
        } else {
            // In list mode
            menuItem.setTitle(R.string.action_list_view);
            menuItem.setIcon(R.drawable.ic_view_list_white_24dp);
        }
    }

    public void updateSpanCount(int count) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (mSpanCount != count) {
            PdfViewCtrlSettingsManager.updateGridSize(context, PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_MERGE_FILES, count);
        }
        mSpanCount = count;
        updateGridMenuState(mOptionsMenu);
        mRecyclerView.updateSpanCount(count);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MiscUtils.updateAdapterViewWidthAfterGlobalLayout(mRecyclerView, mAdapter);
    }

    private ToolbarActionMode.Callback mActionModeCallback = new ToolbarActionMode.Callback() {

        MenuItem itemRemove;

        @Override
        public boolean onPrepareActionMode(ToolbarActionMode mode, Menu menu) {
            if (itemRemove != null) {
                itemRemove.setVisible(true);
                itemRemove.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            mode.setTitle(Utils.getLocaleDigits(Integer.toString(mSelectedFileList.size())));

            return true;
        }

        @Override
        public void onDestroyActionMode(ToolbarActionMode mode) {
            mActionMode = null;
            clearSelectedFileList();
        }

        @Override
        public boolean onCreateActionMode(ToolbarActionMode mode, Menu menu) {
            mode.inflateMenu(R.menu.cab_fragment_merge_view);
            final Drawable backArrow = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
            backArrow.mutate().setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
            mToolbar.setNavigationIcon(backArrow);
            itemRemove = menu.findItem(R.id.cab_action_remove);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ToolbarActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.cab_action_remove) {
                mFileList.removeAll(mSelectedFileList);
                finishActionMode();
                Utils.safeNotifyDataSetChanged(mAdapter);
            }

            return true;
        }
    };

    @Override
    public void onMultipleFilesSelected(int requestCode, ArrayList<FileInfo> fileInfoList) {
        for (FileInfo fileInfo : fileInfoList) {
            if (!mFileList.contains(fileInfo)) {
                mFileList.add(fileInfo);
                Utils.safeNotifyDataSetChanged(mAdapter);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == RequestCode.PICK_PHOTO_CAM) {
            // TODO do it in a background thread
            boolean isCamera = false;
            String filePath = "";
            try {
                Map imageIntent = ViewerUtils.readImageIntent(data, activity, mOutputFileUri);
                if (!ViewerUtils.checkImageIntent(imageIntent)) {
                    Utils.handlePdfFromImageFailed(activity, imageIntent);
                    return;
                }

                Bitmap imageBitmap = ViewerUtils.getImageBitmap(getContext(), imageIntent);
                if (imageBitmap == null) {
                    Utils.handlePdfFromImageFailed(activity, imageIntent);
                    return;
                }
                filePath = ViewerUtils.getImageFilePath(imageIntent);
                Uri imageUri = ViewerUtils.getImageUri(imageIntent);
                isCamera = ViewerUtils.isImageFromCamera(imageIntent);

                // try to get display name
                String filename = Utils.getDisplayNameFromImageUri(getContext(), imageUri, filePath);
                // cannot get a valid filename
                if (Utils.isNullOrEmpty(filename)) {
                    Utils.handlePdfFromImageFailed(activity, imageIntent);
                    return;
                }

                String path = getContext().getCacheDir().getAbsolutePath() + File.separator + filename + ".pdf";
                String pdfFilePath = Utils.getFileNameNotInUse(path);
                File file = new File(pdfFilePath);
                if (file.exists()) {
                    Utils.handlePdfFromImageFailed(activity, imageIntent);
                    return;
                }
                ViewerUtils.imageIntentToPdf(getActivity(), imageUri, filePath, pdfFilePath);

                if (file.exists()) {
                    FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file, false, 1);
                    if (!mTempFileList.contains(fileInfo)) {
                        mTempFileList.add(fileInfo);
                    }
                    if (!mFileList.contains(fileInfo)) {
                        if (canAddFile(fileInfo)) {
                            mFileList.add(fileInfo);
                            Utils.safeNotifyDataSetChanged(mAdapter);
                        }
                    }
                }
                finishActionMode();
            } catch (FileNotFoundException e) {
                CommonToast.showText(getContext(), getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (Exception e) {
                CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (OutOfMemoryError oom) {
                com.pdftron.demo.utils.MiscUtils.manageOOM(getContext());
                CommonToast.showText(getContext(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
            }

            // cleanup the image if it is from camera
            if (isCamera) {
                FileUtils.deleteQuietly(new File(filePath));
            }
        }
    }

    protected boolean canAddFile(FileInfo fileInfo) {
        return canAddFile(fileInfo, true);
    }

    protected boolean canAddFile(FileInfo fileInfo, boolean showError) {
        Context context = getContext();
        if (fileInfo == null || context == null) {
            return false;
        }
        PDFDoc tempDoc = null;
        SecondaryFileFilter filter = null;
        boolean shouldUnlock = false;
        boolean nonPDF = false;
        boolean canAdd = false;
        try {
            switch (fileInfo.getType()) {
                case BaseFileInfo.FILE_TYPE_FILE:
                    if (fileInfo.getFile().exists()) {
                        if (Utils.isNotPdf(fileInfo.getAbsolutePath())) {
                            nonPDF = true;
                        } else {
                            tempDoc = new PDFDoc(fileInfo.getAbsolutePath());
                        }
                    }
                    break;
                case BaseFileInfo.FILE_TYPE_EXTERNAL:
                    if (Utils.isNotPdf(fileInfo.getAbsolutePath())) {
                        nonPDF = true;
                    } else {
                        Uri uri = Uri.parse(fileInfo.getAbsolutePath());
                        filter = new SecondaryFileFilter(context, uri);
                        tempDoc = new PDFDoc(filter);
                    }
                    break;
                default:
                    // Not supported
                    break;
            }
            if (tempDoc != null) {
                canAdd = true;

                tempDoc.lock();
                shouldUnlock = true;
                //noinspection ConstantConditions
                do {
                    // Is this doc password protected?
                    if (!tempDoc.initSecurityHandler()) {
                        canAdd = false;
                        break;
                    }

                    // Does this doc need XFA rendering?
                    Obj needsRenderingObj = tempDoc.getRoot().findObj("NeedsRendering");
                    if (needsRenderingObj != null && needsRenderingObj.isBool() && needsRenderingObj.getBool()) {
                        canAdd = false;
                        break;
                    }

                    // Is this doc a package/portfolio?
                    Obj collectionObj = tempDoc.getRoot().findObj("Collection");
                    if (collectionObj != null) {
                        canAdd = false;
                        break;
                    }
                } while (false);

                if (!canAdd && showError) {
                    // show error
                    String errorMessage = getResources().getQuantityString(R.plurals.dialog_merge_invalid_file_message, 1);
                    CommonToast.showText(context, errorMessage, Toast.LENGTH_LONG);
                }
            } else if (nonPDF) {
                canAdd = true;
            }
        } catch (Exception e) {
            canAdd = false;
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(tempDoc);
            }
            Utils.closeQuietly(tempDoc, filter);
        }
        return canAdd;
    }

    private boolean finishActionMode() {
        boolean success = false;
        if (mActionMode != null) {
            success = true;
            mActionMode.finish();
            mActionMode = null;
            clearSelectedFileList();
        }
        return success;
    }

    private void clearSelectedFileList() {
        if (mItemSelectionHelper != null) {
            mItemSelectionHelper.clearChoices();
        }
        mSelectedFileList.clear();
    }

    private void getDocumentTitle() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        @SuppressLint("InflateParams") final View nameDialog = LayoutInflater.from(context).inflate(R.layout.dialog_rename_file, null);
        final EditText nameEditText = nameDialog.findViewById(R.id.dialog_rename_file_edit);
        // Suggest the first file's name for the merged file
        if (mFileList.size() > 0) {
            String filename = null;
            FileInfo fileInfo = mFileList.get(0);
            if (fileInfo != null) {
                filename = fileInfo.getName();
            }
            if (!Utils.isNullOrEmpty(filename)) {
                filename = FilenameUtils.removeExtension(filename) + "-Merged.pdf";
                nameEditText.setText(filename);
            }
        }
        nameEditText.setHint(R.string.dialog_merge_set_file_name_hint);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(nameDialog)
            .setTitle(getString(R.string.dialog_merge_set_file_name_title))
            .setPositiveButton(R.string.merge, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mListener != null) {
                        mDoRemoveTemp = false;
                        mListener.onMergeConfirmed(mFileList, mTempFileList, nameEditText.getText().toString());
                    }
                    MergeDialogFragment.this.dismiss();
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
                if (nameEditText.getText() != null && nameEditText.getText().length() > 0) {
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                } else {
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });

        dialog.show();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        // clean up temporarily files
        if (mDoRemoveTemp) {
            com.pdftron.demo.utils.MiscUtils.removeFiles(mTempFileList);
        }
        int numNonPdf = 0;
        int numPdf = 0;
        for (FileInfo fileInfo : mFileList) {
            if ("pdf".equalsIgnoreCase(fileInfo.getExtension())) {
                ++numPdf;
            } else {
                ++numNonPdf;
            }
        }
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_MERGE,
            AnalyticsParam.mergeParam(mScreenId, numNonPdf, numPdf));
    }

    private boolean onBackPressed() {
        return isAdded() && finishActionMode();
    }

    @Override
    public void onFilterResultsPublished(int resultCode) {
        // Do nothing
    }

    @Override
    public void onShowFileInfo(int position) {
        // Do nothing
    }

    @Override
    public void onFilesReordered() {
        finishActionMode();
    }
}
