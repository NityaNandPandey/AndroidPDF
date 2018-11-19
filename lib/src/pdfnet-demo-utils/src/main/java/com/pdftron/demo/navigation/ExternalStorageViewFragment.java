//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.pdftron.common.PDFNetException;
import com.pdftron.demo.R;
import com.pdftron.demo.R2;
import com.pdftron.demo.asynctask.PopulateSdFolderTask;
import com.pdftron.demo.dialog.FilePickerDialogFragment;
import com.pdftron.demo.dialog.MergeDialogFragment;
import com.pdftron.demo.navigation.adapter.BaseFileAdapter;
import com.pdftron.demo.navigation.adapter.ExternalStorageAdapter;
import com.pdftron.demo.navigation.callbacks.FileUtilCallbacks;
import com.pdftron.demo.navigation.callbacks.MainActivityListener;
import com.pdftron.demo.utils.AddDocPdfHelper;
import com.pdftron.demo.utils.ExternalFileManager;
import com.pdftron.demo.utils.FileInfoComparator;
import com.pdftron.demo.utils.FileListFilter;
import com.pdftron.demo.utils.Logger;
import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.demo.utils.ThumbnailPathCacheManager;
import com.pdftron.demo.utils.ThumbnailWorker;
import com.pdftron.demo.widget.FileTypeFilterPopupWindow;
import com.pdftron.demo.widget.ImageViewTopCrop;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFDocInfo;
import com.pdftron.pdf.PreviewHandler;
import com.pdftron.pdf.controls.AddPageDialogFragment;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.PdfViewCtrlTabsManager;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.ItemSelectionHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import com.pdftron.sdf.SDFDoc;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ExternalStorageViewFragment extends FileBrowserViewFragment implements
    SearchView.OnQueryTextListener,
    MainActivityListener,
    BaseFileAdapter.AdapterListener,
    ExternalFileManager.ExternalFileManagementListener,
    MergeDialogFragment.MergeDialogFragmentListener,
    FilePickerDialogFragment.LocalFolderListener,
    ActionMode.Callback,
    FilePickerDialogFragment.ExternalFolderListener,
    PopulateSdFolderTask.Callback {

    private static final String TAG = ExternalStorageViewFragment.class.getName();

    @BindView(R2.id.recycler_view)
    protected SimpleRecyclerView mRecyclerView;
    @BindView(android.R.id.empty)
    protected View mEmptyView;
    @BindView(R2.id.empty_text_view)
    protected TextView mEmptyTextView;
    @BindView(R2.id.empty_image_view)
    protected View mEmptyImageView;
    @BindView(R2.id.progress_bar_view)
    protected ProgressBar mProgressBarView;
    @BindView(R2.id.breadcrumb_bar_scroll_view)
    protected HorizontalScrollView mBreadcrumbBarScrollView;
    @BindView(R2.id.breadcrumb_bar_layout)
    protected LinearLayout mBreadcrumbBarLayout;
    @BindView(R2.id.fab_menu)
    protected FloatingActionMenu mFabMenu;

    private Unbinder unbinder;

    protected ArrayList<ExternalFileInfo> mFileInfoList = new ArrayList<>();
    protected ArrayList<ExternalFileInfo> mFileInfoSelectedList = new ArrayList<>();
    protected ArrayList<ExternalFileInfo> mRoots = new ArrayList<>();
    protected ExternalFileInfo mCurrentRoot;
    protected ExternalFileInfo mCurrentFolder;
    protected ExternalFileInfo mSelectedFile;

    private FileUtilCallbacks mFileUtilCallbacks;

    protected ExternalStorageAdapter mAdapter;
    protected int mSpanCount;
    protected ItemSelectionHelper mItemSelectionHelper;

    private PopulateSdFolderTask mPopulateSdFolderTask;
    private Menu mOptionsMenu;
    private MenuItem mSearchMenuItem;
    private boolean mIsSearchMode;
    private boolean mIsFullSearchDone;
    private boolean mViewerLaunching;
    private FileInfoDrawer mFileInfoDrawer;
    private Comparator<ExternalFileInfo> mSortMode;
    private int mCrumbColorActive;
    private int mCrumbColorInactive;
    private Uri mOutputFileUri;
    private String mSavedFolderUri;
    private String mSavedLeafUri;
    private FileTypeFilterPopupWindow mFileTypeFilterListPopup;
    private String mFilterText = "";

    private MenuItem itemRename;
    private MenuItem itemDuplicate;
    private MenuItem itemMove;
    private MenuItem itemDelete;
    private MenuItem itemMerge;
    private MenuItem itemFavorite;
    private MenuItem itemShare;

    private ExternalStorageViewFragmentListener mExternalStorageViewFragmentListener;

    public interface ExternalStorageViewFragmentListener {

        void onExternalStorageShown();

        void onExternalStorageHidden();
    }

    public static ExternalStorageViewFragment newInstance() {
        return new ExternalStorageViewFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        if (null != savedInstanceState) {
            mOutputFileUri = savedInstanceState.getParcelable("output_file_uri");
        }

        mCrumbColorActive = getResources().getColor(android.R.color.white);
        mCrumbColorInactive = getResources().getColor(R.color.breadcrumb_color_inactive);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_external_storage_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        unbinder = ButterKnife.bind(this, view);

        mSpanCount = PdfViewCtrlSettingsManager.getGridSize(activity, PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_EXTERNAL_FILES);

        mBreadcrumbBarScrollView.setVerticalScrollBarEnabled(false);
        mBreadcrumbBarScrollView.setHorizontalScrollBarEnabled(false);

        mBreadcrumbBarLayout.removeAllViews();

        mFabMenu.setClosedOnTouchOutside(true);
        FloatingActionButton menuButton = mFabMenu.getMenuButton();
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentFolder != null && mCurrentRoot != null) {
                    // Non-root folder, show menu
                    mFabMenu.toggle(mFabMenu.isAnimated());
                } else {
                    finishActionMode();

                    try {
                        // Root folder, launch standard picker
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        // Force advanced devices (SD cards) to always be visible
                        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
                        // Only show local storage devices
                        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                        startActivityForResult(intent, RequestCode.DOCUMENT_TREE);
                    } catch (Exception e) {
                        String message = String.format(getString(R.string.dialog_external_intent_not_supported),
                            getString(R.string.dialog_external_intent_not_supported_more_info));
                        CommonToast.showText(getContext(), message, Toast.LENGTH_LONG);
                    }
                }
            }
        });

        FloatingActionButton createFolderButton = mFabMenu.findViewById(R.id.add_folder);
        createFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                ExternalFileManager.createFolder(v.getContext(), mCurrentFolder, ExternalStorageViewFragment.this);
            }
        });

        FloatingActionButton createBlankPDFButton = mFabMenu.findViewById(R.id.blank_PDF);
        createBlankPDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                AddPageDialogFragment addPageDialogFragment = AddPageDialogFragment.newInstance();
                addPageDialogFragment.setOnCreateNewDocumentListener(new AddPageDialogFragment.OnCreateNewDocumentListener() {
                    @Override
                    public void onCreateNewDocument(PDFDoc doc, String title) {
                        saveCreatedDocument(doc, title);
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                            AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_BLANK_PDF, AnalyticsHandlerAdapter.SCREEN_SD_CARD));
                    }
                });
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    addPageDialogFragment.show(fragmentManager, "create_document_external_folder");
                }
            }
        });

        FloatingActionButton imagePDFButton = mFabMenu.findViewById(R.id.image_PDF);
        imagePDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                mOutputFileUri = ViewerUtils.openImageIntent(ExternalStorageViewFragment.this);
            }
        });

        FloatingActionButton officePDFButton = mFabMenu.findViewById(R.id.office_PDF);
        officePDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);

                FragmentActivity activity = getActivity();
                FragmentManager fragmentManager = getFragmentManager();
                if (activity == null || fragmentManager == null) {
                    return;
                }

                mAddDocPdfHelper = new AddDocPdfHelper(activity, fragmentManager, new AddDocPdfHelper.AddDocPDFHelperListener() {
                    @Override
                    public void onPDFReturned(String fileAbsolutePath, boolean external) {
                        Activity activity = getActivity();
                        if (activity == null) {
                            return;
                        }
                        if (!external) {
                            return;
                        }
                        if (fileAbsolutePath == null) {
                            Utils.showAlertDialog(activity, R.string.dialog_add_photo_document_filename_error_message, R.string.error);
                            return;
                        }

                        reloadDocumentList(false);
                        mCallbacks.onExternalFileSelected(fileAbsolutePath, "");
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                            AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_DOCS, AnalyticsHandlerAdapter.SCREEN_SD_CARD));
                    }

                    @Override
                    public void onMultipleFilesSelected(int requestCode, ArrayList<FileInfo> fileInfoList) {
                        handleMultipleFilesSelected(fileInfoList, AnalyticsHandlerAdapter.SCREEN_SD_CARD);
                    }
                });
                mAddDocPdfHelper.pickFileAndCreate(mCurrentFolder);
            }
        });

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);

        mItemSelectionHelper = new ItemSelectionHelper();
        mItemSelectionHelper.attachToRecyclerView(mRecyclerView);
        mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_MULTIPLE);

        mAdapter = getAdapter();
        mRecyclerView.setAdapter(mAdapter);

        try {
            mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mRecyclerView == null) {
                            return;
                        }
                        try {
                            mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } catch (Exception ignored) {
                        }
                        if (mAdapter == null) {
                            return;
                        }
                        int viewWidth = mRecyclerView.getMeasuredWidth();
                        mAdapter.updateMainViewWidth(viewWidth);
                        mAdapter.getDerivedFilter().setFileTypeEnabledInFilterFromSettings(mRecyclerView.getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_EXTERNAL_FILES);
                        updateFileListFilter();
                    }
                });
        } catch (Exception ignored) {
        }

        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                ExternalFileInfo file = mAdapter.getItem(position);
                if (file == null) {
                    return;
                }

                if (mActionMode == null) {
                    mItemSelectionHelper.setItemChecked(position, false);

                    onFileClicked(file);
                } else {
                    // Multiple selection is only available starting in Honeycomb.
                    if (mFileInfoSelectedList.contains(file)) {
                        mFileInfoSelectedList.remove(file);
                        mItemSelectionHelper.setItemChecked(position, false);
                    } else {
                        mFileInfoSelectedList.add(file);
                        mItemSelectionHelper.setItemChecked(position, true);
                    }
                    if (mFileInfoSelectedList.isEmpty()) {
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
                final ExternalFileInfo file = mAdapter.getItem(position);
                if (file == null) {
                    return false;
                }

                closeSearch();
                if (mActionMode == null) {
                    mFileInfoSelectedList.add(file);
                    mItemSelectionHelper.setItemChecked(position, true);

                    // Start the CAB using the ActionMode.Callback defined above.
                    mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(ExternalStorageViewFragment.this);
                    if (mActionMode != null) {
                        mActionMode.invalidate();
                    }
                } else {
                    if (mFileInfoSelectedList.contains(file)) {
                        mFileInfoSelectedList.remove(file);
                        mItemSelectionHelper.setItemChecked(position, false);
                    } else {
                        mFileInfoSelectedList.add(file);
                        mItemSelectionHelper.setItemChecked(position, true);
                    }

                    if (mFileInfoSelectedList.isEmpty()) {
                        finishActionMode();
                    } else {
                        mActionMode.invalidate();
                    }
                }

                return true;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_SD_CARD);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mOutputFileUri != null) {
            outState.putParcelable("output_file_uri", mOutputFileUri);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        resumeFragment();
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseFragment();
    }

    @Override
    public void onStop() {
        super.onStop();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_SD_CARD);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // cleanup previous resource
        if (null != mAdapter) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.cleanupResources();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        MiscUtils.handleLowMemory(getContext(), mAdapter);
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_LOW_MEMORY, AnalyticsParam.lowMemoryParam(TAG));
        Logger.INSTANCE.LogE(TAG, "low memory");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mFileUtilCallbacks = (FileUtilCallbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + e.getClass().toString());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFileUtilCallbacks = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MiscUtils.updateAdapterViewWidthAfterGlobalLayout(mRecyclerView, mAdapter);
        if (mFileTypeFilterListPopup != null && mFileTypeFilterListPopup.isShowing()) {
            mFileTypeFilterListPopup.dismiss();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            return;
        }

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_folder_view, menu);
        inflater.inflate(R.menu.menu_addon_file_type_filter, menu);

        mOptionsMenu = menu;

        mSearchMenuItem = menu.findItem(R.id.menu_action_search);
        if (mSearchMenuItem != null) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            searchView.setQueryHint(getString(R.string.action_file_filter));
            searchView.setOnQueryTextListener(this);
            searchView.setSubmitButtonEnabled(false);

            if (!Utils.isNullOrEmpty(mFilterText)) {
                mSearchMenuItem.expandActionView();
                searchView.setQuery(mFilterText, true);
                mFilterText = "";
            }

            // Disable long-click context menu
            EditText editText = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
            if (editText != null) {
                editText.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
                        return false;
                    }

                    @Override
                    public void onDestroyActionMode(android.view.ActionMode mode) {

                    }
                });
            }

            final MenuItem reloadMenuItem = menu.findItem(R.id.menu_action_reload);
            final MenuItem listToggleMenuItem = menu.findItem(R.id.menu_grid_toggle);

            // We need to override this method to get the collapse event, so we can
            // clear the filter.
            mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

                @Override
                public boolean onMenuItemActionExpand(MenuItem item) {
                    // Let's return true to expand the view.
                    reloadMenuItem.setVisible(false);
                    listToggleMenuItem.setVisible(false);
                    mIsSearchMode = true;
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem item) {
                    reloadMenuItem.setVisible(true);
                    listToggleMenuItem.setVisible(true);
                    resetFileListFilter();
                    mIsSearchMode = false;
                    return true;
                }
            });
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        Context context = getContext();
        if (menu == null || context == null) {
            return;
        }

        MenuItem menuItem;
        if (PdfViewCtrlSettingsManager.getSortMode(context).equals(PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_NAME)) {
            mSortMode = FileInfoComparator.externalPathOrder();
            menuItem = menu.findItem(R.id.menu_file_sort_by_name);
        } else {
            mSortMode = FileInfoComparator.externalDateOrder();
            menuItem = menu.findItem(R.id.menu_file_sort_by_date);
        }
        if (menuItem != null) {
            menuItem.setChecked(true);
        }

        // Set grid size radio buttons to correct value from settings
        int gridSize = PdfViewCtrlSettingsManager.getGridSize(getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_EXTERNAL_FILES);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }
        boolean handled = false;
        if (item.getItemId() == R.id.menu_action_search) {
            finishSearchView();
            handled = true;
        }
        if (item.getItemId() == R.id.menu_action_reload) {
            ThumbnailPathCacheManager.getInstance().cleanupResources(getContext());
            reloadDocumentList(mCurrentRoot == null);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_file_sort_by_name) {
            // Update sort mode setting
            mSortMode = FileInfoComparator.externalPathOrder();
            PdfViewCtrlSettingsManager.updateSortMode(activity, PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_NAME);
            item.setChecked(true);
            reloadDocumentList(false);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_file_sort_by_date) {
            // Update sort mode setting
            mSortMode = FileInfoComparator.externalDateOrder();
            PdfViewCtrlSettingsManager.updateSortMode(activity, PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_DATE);
            item.setChecked(true);
            reloadDocumentList(false);
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
        if (item.getItemId() == R.id.menu_file_filter) {
            mFileTypeFilterListPopup = new FileTypeFilterPopupWindow(this.getContext(), this.getView(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_EXTERNAL_FILES);
            mFileTypeFilterListPopup.showAtLocation(this.getView(), Gravity.TOP | (Utils.isRtlLayout(getContext()) ? Gravity.LEFT : Gravity.RIGHT), 15, 75);
            mFileTypeFilterListPopup.setFileTypeChangedListener(new FileTypeFilterPopupWindow.FileTypeChangedListener() {
                @Override
                public void filterTypeChanged(int index, boolean isOn) {
                    mAdapter.getDerivedFilter().setFileTypeEnabledInFilter(index, isOn);
                    updateFileListFilter();
                }
            });
        }

        return handled;
    }

    @Override
    public void onMergeConfirmed(ArrayList<FileInfo> filesToMerge, ArrayList<FileInfo> filesToDelete, String title) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        boolean hasExtension = FilenameUtils.isExtension(title, "pdf");
        if (!hasExtension) {
            title = title + ".pdf";
        }
        String fileName = Utils.getFileNameNotInUse(mCurrentFolder, title);
        if (mCurrentFolder == null || Utils.isNullOrEmpty(fileName)) {
            CommonToast.showText(activity, R.string.dialog_merge_error_message_general, Toast.LENGTH_SHORT);
            return;
        }
        final ExternalFileInfo file = mCurrentFolder.createFile("application/pdf", fileName);
        if (file == null) {
            return;
        }
        FileInfo targetFile = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, file.getAbsolutePath(), file.getFileName(), false, 1);

        ExternalFileManager.merge(activity, filesToMerge, filesToDelete, targetFile, ExternalStorageViewFragment.this);
    }

    protected void handleMerge(ArrayList<FileInfo> files) {
        MergeDialogFragment mergeDialog = getMergeDialogFragment(files, AnalyticsHandlerAdapter.SCREEN_SD_CARD);
        mergeDialog.initParams(ExternalStorageViewFragment.this);
        mergeDialog.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            mergeDialog.show(fragmentManager, "merge_dialog");
        }
    }

    protected ExternalStorageAdapter getAdapter() {
        return new ExternalStorageAdapter(getActivity(), mFileInfoList, mFileListLock,
            mSpanCount, this, mItemSelectionHelper);
    }

    protected boolean canAddToFavorite(FileInfo file) {
        FragmentActivity activity = getActivity();
        return activity != null && !activity.isFinishing()
            && (!getFavoriteFilesManager().containsFile(activity, file));
    }

    protected void addToFavorite(FileInfo file) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        getFavoriteFilesManager().addFile(activity, file);
    }

    protected void removeFromFavorite(FileInfo file) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        getFavoriteFilesManager().removeFile(activity, file);
    }

    protected void handleAddToFavorite(FileInfo file) {
        FragmentActivity activity = getActivity();
        if (canAddToFavorite(file)) {
            addToFavorite(file);
            CommonToast.showText(activity,
                getString(R.string.file_added_to_favorites, file.getName()),
                Toast.LENGTH_SHORT);
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_FILEBROWSER,
                (file.isDirectory()) ? "Folder added to Favorites" : "File added to Favorites",
                AnalyticsHandlerAdapter.LABEL_EXTERNAL);
        } else {
            removeFromFavorite(file);
            CommonToast.showText(activity,
                getString(R.string.file_removed_from_favorites, file.getName()),
                Toast.LENGTH_SHORT);
        }
    }

    protected void handleFileUpdated(FileInfo oldFile, FileInfo newFile) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        getRecentFilesManager().updateFile(activity, oldFile, newFile);
        getFavoriteFilesManager().updateFile(activity, oldFile, newFile);
    }

    protected void handleFilesRemoved(ArrayList<FileInfo> files) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        getRecentFilesManager().removeFiles(activity, files);
        getFavoriteFilesManager().removeFiles(activity, files);
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
        menuItem1.setTitle(getString(R.string.columns_count, 1));
        MenuItem menuItem2 = menu.findItem(R.id.menu_grid_count_2);
        menuItem2.setTitle(getString(R.string.columns_count, 2));
        MenuItem menuItem3 = menu.findItem(R.id.menu_grid_count_3);
        menuItem3.setTitle(getString(R.string.columns_count, 3));
        MenuItem menuItem4 = menu.findItem(R.id.menu_grid_count_4);
        menuItem4.setTitle(getString(R.string.columns_count, 4));
        MenuItem menuItem5 = menu.findItem(R.id.menu_grid_count_5);
        menuItem5.setTitle(getString(R.string.columns_count, 5));
        MenuItem menuItem6 = menu.findItem(R.id.menu_grid_count_6);
        menuItem6.setTitle(getString(R.string.columns_count, 6));
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            if (RequestCode.PICK_PHOTO_CAM == requestCode) {
                boolean isCamera = false;
                String imageFilePath = "";
                try {
                    Map imageIntent = ViewerUtils.readImageIntent(data, activity, mOutputFileUri);
                    if (!ViewerUtils.checkImageIntent(imageIntent)) {
                        Utils.handlePdfFromImageFailed(activity, imageIntent);
                        return;
                    }

                    imageFilePath = ViewerUtils.getImageFilePath(imageIntent);
                    Uri selectedImage = ViewerUtils.getImageUri(imageIntent);
                    isCamera = ViewerUtils.isImageFromCamera(imageIntent);

                    // try to get display name
                    String filename = Utils.getDisplayNameFromImageUri(getContext(), selectedImage, imageFilePath);
                    if (Utils.isNullOrEmpty(filename)) {
                        // cannot get a valid filename
                        Utils.handlePdfFromImageFailed(activity, imageIntent);
                        return;
                    }

                    String pdfFilePath = Utils.getFileNameNotInUse(mCurrentFolder, filename + ".pdf");
                    if (mCurrentFolder == null || Utils.isNullOrEmpty(pdfFilePath)) {
                        Utils.handlePdfFromImageFailed(activity, imageIntent);
                        return;
                    }
                    ExternalFileInfo documentFile = mCurrentFolder.createFile("application/pdf", pdfFilePath);
                    if (documentFile == null) {
                        Utils.handlePdfFromImageFailed(activity, imageIntent);
                        return;
                    }

                    String outputPath = ViewerUtils.imageIntentToPdf(getActivity(), selectedImage, imageFilePath, documentFile);
                    if (outputPath != null) {
                        String toastMsg = getString(R.string.dialog_create_new_document_filename_success)
                            + mCurrentFolder.getAbsolutePath();
                        CommonToast.showText(activity, toastMsg, Toast.LENGTH_LONG);
                        if (mCallbacks != null) {
                            mCallbacks.onExternalFileSelected(documentFile.getAbsolutePath(), "");
                        }
                    }

                    finishActionMode();

                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                        AnalyticsParam.createNewParam(isCamera ? AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_CAMERA : AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_IMAGE,
                            AnalyticsHandlerAdapter.SCREEN_SD_CARD));
                } catch (FileNotFoundException e) {
                    CommonToast.showText(getContext(), R.string.dialog_add_photo_document_filename_file_error, Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } catch (Exception e) {
                    CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } catch (OutOfMemoryError oom) {
                    MiscUtils.manageOOM(getContext());
                    CommonToast.showText(getContext(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                }

                // cleanup the image if it is from camera
                if (isCamera) {
                    FileUtils.deleteQuietly(new File(imageFilePath));
                }
            } else if (requestCode == RequestCode.DOCUMENT_TREE) {
                ContentResolver contentResolver = Utils.getContentResolver(activity);
                if (data != null && contentResolver != null) {
                    Uri treeUri = data.getData();
                    if (treeUri == null) {
                        return;
                    }
                    try {
                        contentResolver.takePersistableUriPermission(treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    } catch (SecurityException e) {
                        // Failure taking permissions
                        CommonToast.showText(activity, R.string.error_failed_to_open_document_tree, Toast.LENGTH_SHORT);
                    }

                    reloadDocumentList(true);
                }
            }
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (super.onCreateActionMode(mode, menu)) {
            return true;
        }

        mode.getMenuInflater().inflate(R.menu.cab_fragment_file_operations, menu);

        itemRename = menu.findItem(R.id.cab_file_rename);
        itemDuplicate = menu.findItem(R.id.cab_file_copy);
        itemMove = menu.findItem(R.id.cab_file_move);
        itemDelete = menu.findItem(R.id.cab_file_delete);
        itemMerge = menu.findItem(R.id.cab_file_merge);
        itemFavorite = menu.findItem(R.id.cab_file_favorite);
        itemShare = menu.findItem(R.id.cab_file_share);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }
        if (mFileInfoSelectedList.size() > 1) {
            // Multiple selection of files
            itemRename.setVisible(false);
            itemDuplicate.setVisible(false);
            itemMove.setVisible(true);
            itemDelete.setVisible(true);
            itemMerge.setVisible(true);
            itemFavorite.setVisible(false);
            itemShare.setVisible(true);

            // If only files are selected, allow move action
            itemMove.setVisible(true);
            for (ExternalFileInfo file : mFileInfoSelectedList) {
                if (file.isDirectory()) {
                    itemMove.setVisible(false);
                    itemMerge.setVisible(false);
                    itemShare.setVisible(false);
                    break;
                }
            }
        } else {
            if (mFileInfoSelectedList.size() == 1) {
                if (mFileInfoSelectedList.get(0).isDirectory()) {
                    itemDuplicate.setVisible(false);
                    itemMove.setVisible(false);
                    itemMerge.setVisible(false);
                    itemFavorite.setVisible(true);
                    itemShare.setVisible(false);
                } else {
                    itemDuplicate.setVisible(true);
                    itemMove.setVisible(true);
                    itemMerge.setVisible(true);
                    itemFavorite.setVisible(true);
                    itemShare.setVisible(true);
                }
                itemDelete.setVisible(true);
                // Renaming & moving is only supported for non-root level items
                if (mCurrentRoot != null && mCurrentFolder != null) {
                    itemRename.setVisible(true);
                } else {
                    itemRename.setVisible(false);
                    itemMove.setVisible(false);
                }
                // Adjust favorite item
                FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, mFileInfoSelectedList.get(0).getAbsolutePath(),
                    mFileInfoSelectedList.get(0).getFileName(), false, 1);
                if (canAddToFavorite(fileInfo)) {
                    itemFavorite.setTitle(activity.getString(R.string.action_add_to_favorites));
                } else {
                    itemFavorite.setTitle(activity.getString(R.string.action_remove_from_favorites));
                }
            }
        }
        // Adjust delete action's title for root vs non-root folder
        if (mCurrentRoot != null && mCurrentFolder != null) {
            itemDelete.setTitle(getString(R.string.delete));
        } else {
            itemDelete.setTitle(getString(R.string.undo_redo_annot_remove));
            itemDelete.setIcon(null); // Do not show icon
        }
        mode.setTitle(Utils.getLocaleDigits(Integer.toString(mFileInfoSelectedList.size())));
        // Ensure items are always shown
        itemRename.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemDuplicate.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemMove.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemDelete.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return false;
        }
        if (mFileInfoSelectedList.isEmpty()) {
            return false;
        }

        if (item.getItemId() == R.id.cab_file_rename) {
            if (mCurrentRoot != null && mCurrentFolder != null) {
                // Renaming non-root level item
                ExternalFileManager.rename(activity, mFileInfoSelectedList.get(0), ExternalStorageViewFragment.this);
            }
            return true;
        }
        if (item.getItemId() == R.id.cab_file_copy) {
            if (mCurrentRoot != null && mCurrentFolder != null) {
                // Copying a non-root level item (root level copying is not supported)
                ExternalFileManager.duplicate(activity, mFileInfoSelectedList.get(0), ExternalStorageViewFragment.this);
            }
            return true;
        }
        if (item.getItemId() == R.id.cab_file_move) {
            if (mCurrentRoot != null && mCurrentFolder != null) {
                // Moving a non-root level item (root level moving is not supported)
                FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MOVE_FILE_LIST, mCurrentFolder.getUri());
                dialogFragment.setLocalFolderListener(ExternalStorageViewFragment.this);
                dialogFragment.setExternalFolderListener(ExternalStorageViewFragment.this);
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
                }
            }
            return true;
        }
        if (item.getItemId() == R.id.cab_file_delete) {
            if (mCurrentRoot != null && mCurrentFolder != null) {
                // Deleting non-root level item(s)
                ExternalFileManager.delete(activity, mFileInfoSelectedList, ExternalStorageViewFragment.this);
            } else {
                // Removing a root level item
                ExternalFileManager.removeAccess(activity, mFileInfoSelectedList, ExternalStorageViewFragment.this);
            }
            return true;
        }
        if (item.getItemId() == R.id.cab_file_merge) {
            // Create and show file merge dialog-fragment
            ArrayList<FileInfo> fileInfoList = new ArrayList<>();
            for (ExternalFileInfo externalFileInfo : mFileInfoSelectedList) {
                fileInfoList.add(new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL,
                    externalFileInfo.getAbsolutePath(), externalFileInfo.getFileName(), false, 1));
            }
            handleMerge(fileInfoList);

            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_FILEBROWSER,
                "Merge item clicked", AnalyticsHandlerAdapter.LABEL_EXTERNAL);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_favorite) {
            int fileType = mFileInfoSelectedList.get(0).isDirectory() ? BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER : BaseFileInfo.FILE_TYPE_EXTERNAL;
            FileInfo fileInfo = new FileInfo(fileType, mFileInfoSelectedList.get(0).getAbsolutePath(),
                mFileInfoSelectedList.get(0).getFileName(), false, 1);
            handleAddToFavorite(fileInfo);

            finishActionMode();
            // Update favorite file indicators
            Utils.safeNotifyDataSetChanged(mAdapter);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_share) {
            if (mFileInfoSelectedList.size() > 1) {
                ArrayList<Uri> files = new ArrayList<>();
                for (ExternalFileInfo file : mFileInfoSelectedList) {
                    files.add(Uri.parse(file.getAbsolutePath()));
                }
                if (mOnPdfFileSharedListener != null) {
                    Intent intent = Utils.createGenericShareIntents(activity, files);
                    mOnPdfFileSharedListener.onPdfFileShared(intent);
                    finishActionMode();
                } else {
                    Utils.shareGenericFiles(activity, files);
                }
            } else {
                if (mOnPdfFileSharedListener != null) {
                    Intent intent = Utils.createGenericShareIntent(activity, mFileInfoSelectedList.get(0).getUri());
                    mOnPdfFileSharedListener.onPdfFileShared(intent);
                    finishActionMode();
                } else {
                    Utils.shareGenericFile(activity, mFileInfoSelectedList.get(0).getUri());
                }
            }
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_FILEBROWSER, "Item share clicked", AnalyticsHandlerAdapter.LABEL_SD_CARD);
            return true;
        }

        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        mActionMode = null;
        clearFileInfoSelectedList();
    }

    protected void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
            clearFileInfoSelectedList();
        }
        closeSearch();
    }

    private void hideFileInfoDrawer() {
        if (mFileInfoDrawer != null) {
            mFileInfoDrawer.hide();
            mFileInfoDrawer = null;
        }
        mSelectedFile = null;
    }

    private void finishSearchView() {
        if (mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded()) {
            mSearchMenuItem.collapseActionView();
        }
        resetFileListFilter();
    }

    public void updateSpanCount(int count) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (mSpanCount != count) {
            PdfViewCtrlSettingsManager.updateGridSize(context, PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_EXTERNAL_FILES, count);
        }
        mSpanCount = count;
        updateGridMenuState(mOptionsMenu);
        mRecyclerView.updateSpanCount(count);
    }

    private void resetFileListFilter() {
        String filterText = getFilterText();
        if (!Utils.isNullOrEmpty(filterText) && mAdapter != null) {
            mAdapter.setInSearchMode(false);
            mAdapter.getFilter().filter("");
        }
    }

    public String getFilterText() {
        if (!Utils.isNullOrEmpty(mFilterText)) {
            return mFilterText;
        }

        String filterText = "";
        if (mSearchMenuItem != null) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            if (searchView != null) {
                filterText = searchView.getQuery().toString();
            }
        }
        return filterText;
    }

    private void clearFileInfoSelectedList() {
        if (mItemSelectionHelper != null) {
            mItemSelectionHelper.clearChoices();
        }
        mFileInfoSelectedList.clear();
    }

    private void updateFileListFilter() {
        if (mAdapter != null) {
            String constraint = getFilterText();
            if (constraint == null) {
                constraint = "";
            }
            mAdapter.getFilter().filter(constraint);
            boolean isEmpty = Utils.isNullOrEmpty(constraint);
            mAdapter.setInSearchMode(!isEmpty);
        }
    }

    private void rebuildBreadcrumbBar(Context context, ExternalFileInfo leaf) {
        mBreadcrumbBarLayout.removeAllViews();

        ExternalFileInfo parent;
        if (leaf != null) {
            parent = leaf;
        } else {
            parent = mCurrentFolder;
        }
        while (parent != null) {
            createBreadcrumb(context, parent, 0); // Add to start
            parent = parent.getParent();
        }
        // Add crumb for "root" folder
        createBreadcrumb(context, null, 0);
    }

    private int findBreadcrumb(ExternalFileInfo folder) {
        int position = -1;
        if (folder != null) {
            for (int i = 0; i < mBreadcrumbBarLayout.getChildCount(); i++) {
                LinearLayout crumb = (LinearLayout) mBreadcrumbBarLayout.getChildAt(i);
                Object tag = crumb.getTag();
                if (tag != null && tag instanceof ExternalFileInfo) {
                    ExternalFileInfo file = (ExternalFileInfo) tag;
                    if (file.getUri().equals(folder.getUri())) {
                        position = i;
                        break;
                    }
                }
            }
        }
        return position;
    }

    // Create and append a new breadcrumb to the bar
    // NOTE: this should be called BEFORE the current folder is changed
    private void appendBreadcrumb(Context context, ExternalFileInfo newFolder) {
        int currentCrumb = -1;
        if (mBreadcrumbBarLayout.getChildCount() > 0) {
            // Find the current folder's crumb in the bar
            if (mCurrentFolder != null && mCurrentRoot != null) {
                currentCrumb = findBreadcrumb(mCurrentFolder);
            } else {
                // In the root folder, so current crumb is the first child
                currentCrumb = 0;
            }
            // Check if the next crumb (right) corresponds to the new folder
            if (currentCrumb >= 0) {
                if (currentCrumb + 1 < mBreadcrumbBarLayout.getChildCount()) {
                    boolean clearToRight = true;
                    LinearLayout crumb = (LinearLayout) mBreadcrumbBarLayout.getChildAt(currentCrumb + 1);
                    Object tag = crumb.getTag();
                    if (tag != null && tag instanceof ExternalFileInfo) {
                        ExternalFileInfo file = (ExternalFileInfo) tag;
                        if (file.getUri().equals(newFolder.getUri())) {
                            // New folder is already in breadcrumb bar
                            clearToRight = false;
                        }
                    }
                    if (clearToRight) {
                        // let's rebuild bread crumb bar from scratch, since it can be a
                        // non-immediate child
                        rebuildBreadcrumbBar(getContext(), newFolder);
                    }
                } else {
                    // let's rebuild bread crumb bar from scratch, since it can be a
                    // non-immediate child
                    rebuildBreadcrumbBar(getContext(), newFolder);
                }
                setCurrentBreadcrumb(currentCrumb + 1);
            }
        }
        if (currentCrumb < 0) {
            // Current crumb could not be found or bar is not built, try (re)building the bar
            rebuildBreadcrumbBar(context, null);
            // Create a new crumb and add to end of bar
            createBreadcrumb(context, newFolder, -1);

            setCurrentBreadcrumb(-1);
        }
    }

    private void createBreadcrumb(Context context, ExternalFileInfo folder, int position) {
        @SuppressLint("InflateParams") final LinearLayout crumb =
            (LinearLayout) LayoutInflater.from(context).inflate(R.layout.breadcrumb, null);
        TextView dirTextView = crumb.findViewById(R.id.crumbText);
        if (folder != null) { // Non-root breadcrumb
            dirTextView.setText(folder.getFileName().toUpperCase());
        } else { // Root breadcrumb
            String rootText = context.getString(R.string.external_storage);
            dirTextView.setText(rootText.toUpperCase());
        }
        crumb.setTag(folder);
        crumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object tag = crumb.getTag();
                if (tag != null) { // Non-root breadcrumb
                    if (tag instanceof ExternalFileInfo) {
                        ExternalFileInfo file = (ExternalFileInfo) tag;
                        if (mCurrentFolder == null || !file.equals(mCurrentFolder)) {
                            mCurrentFolder = file;
                            // Get the folder's root
                            ExternalFileInfo root = file;
                            while (root.getParent() != null) {
                                root = root.getParent();
                            }
                            mCurrentRoot = root;

                            finishSearchView();
                            finishActionMode();
                            reloadDocumentList(false);
                        }
                    }
                } else { // Root breadcrumb
                    if (mCurrentFolder != null && mCurrentRoot != null) {
                        mCurrentFolder = null;
                        mCurrentRoot = null;

                        finishSearchView();
                        finishActionMode();
                        reloadDocumentList(true);
                    }
                }
            }
        });

        mBreadcrumbBarLayout.addView(crumb, position);
    }

    // Adjust crumb text and chevron colors to indicate current crumb
    private void setCurrentBreadcrumb(int position) {
        if (position < 0 || position >= mBreadcrumbBarLayout.getChildCount()) {
            position = mBreadcrumbBarLayout.getChildCount() - 1;
        }
        boolean showChevron = false;
        for (int i = mBreadcrumbBarLayout.getChildCount() - 1; i >= 0; i--) {
            LinearLayout crumb = (LinearLayout) mBreadcrumbBarLayout.getChildAt(i);
            TextView dirTextView = crumb.findViewById(R.id.crumbText);
            ImageView chevronImageView = crumb.findViewById(R.id.crumbChevron);
            if (Utils.isRtlLayout(getContext())) {
                chevronImageView.setScaleX(-1f);
            }
            int crumbColor;
            if (i == position) {
                crumbColor = mCrumbColorActive;
            } else {
                crumbColor = mCrumbColorInactive;
            }
            dirTextView.setTextColor(crumbColor);
            if (showChevron) {
                chevronImageView.setColorFilter(crumbColor, PorterDuff.Mode.SRC_IN);
                chevronImageView.setVisibility(View.VISIBLE);
            } else {
                chevronImageView.setVisibility(View.GONE);
            }
            showChevron = true;
        }
        final View child = mBreadcrumbBarLayout.getChildAt(position);
        if (child != null) {
            mBreadcrumbBarScrollView.requestChildFocus(mBreadcrumbBarLayout, child);
        }
    }

    private void reloadDocumentList(boolean reloadRoots) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(true);
        }

        if (mPopulateSdFolderTask != null) {
            mPopulateSdFolderTask.cancel(true);
        }

        mPopulateSdFolderTask = new PopulateSdFolderTask(
            context, mFileInfoList, mFileListLock, mRoots, reloadRoots,
            mCurrentRoot, mCurrentFolder, getSortMode(),
            mSavedFolderUri, mSavedLeafUri, true, true, this);
        mPopulateSdFolderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private Comparator<ExternalFileInfo> getSortMode(
    ) {

        if (mSortMode != null) {
            return mSortMode;
        }

        return FileInfoComparator.externalPathOrder();

    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (mAdapter != null && Utils.isNullOrEmpty(mFilterText)) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.getFilter().filter(newText);
            boolean isEmpty = Utils.isNullOrEmpty(newText);
            mAdapter.setInSearchMode(!isEmpty);
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mRecyclerView != null) {
            mRecyclerView.requestFocus();
        }
        return false;
    }

    @Override
    public void onPreLaunchViewer() {
        mViewerLaunching = true;
    }

    @Override
    public void onDataChanged() {
        if (isAdded()) {
            reloadDocumentList(true);
        } // otherwise it will be reloaded in resumeFragment
    }

    @Override
    public void onProcessNewIntent() {
    }

    @Override
    public void onDrawerOpened() {
        finishActionMode();
        if (mIsSearchMode) {
            hideSoftKeyboard();
        }
    }

    @Override
    public void onDrawerSlide() {
        finishActionMode();
    }

    @Override
    public boolean onBackPressed() {
        if (!isAdded()) {
            return false;
        }

        boolean handled = false;
        if (mFabMenu.isOpened()) {
            // Close fab menu
            mFabMenu.close(true);
            handled = true;
        } else if (mFileInfoDrawer != null) {
            // Hide file info drawer
            hideFileInfoDrawer();
            handled = true;
        } else if (mActionMode != null) {
            // Exit action mode
            finishActionMode();
            handled = true;
        } else if (mIsSearchMode) {
            // Exit search mod
            finishSearchView();
            handled = true;
        } else if (mCurrentRoot != null && mCurrentFolder != null) {
            // Navigate up through directories
            ExternalFileInfo parent = mCurrentFolder.getParent();
            boolean reloadRoots = false;
            if (mCurrentRoot.equals(mCurrentFolder) || parent == null) {
                // Navigating up takes us to the list of root folders
                mCurrentFolder = null;
                mCurrentRoot = null;
                reloadRoots = true;
            } else {
                mCurrentFolder = parent;
            }
            reloadDocumentList(reloadRoots);
            handled = true;
        }

        return handled;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (ShortcutHelper.isFind(keyCode, event)) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            if (searchView.isShown()) {
                searchView.setFocusable(true);
                searchView.requestFocus();
            } else {
                mSearchMenuItem.expandActionView();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onFilterResultsPublished(int resultCode) {
        if (mAdapter != null) {
            if (mEmptyView != null) {
                if (mAdapter.getItemCount() > 0) {
                    mEmptyView.setVisibility(View.GONE);
                    scrollToCurrentFile(mRecyclerView);
                } else if (mIsFullSearchDone) {
                    mEmptyImageView.setVisibility(View.GONE);
                    switch (resultCode) {
                        case FileListFilter.FILTER_RESULT_NO_STRING_MATCH:
                            mEmptyTextView.setText(R.string.textview_empty_because_no_string_match);
                            break;
                        case FileListFilter.FILTER_RESULT_NO_ITEMS_OF_SELECTED_FILE_TYPES:
                            mEmptyTextView.setText(R.string.textview_empty_because_no_files_of_selected_type);
                            break;
                        default:
                            if (mCurrentRoot == null && mCurrentFolder == null) {
                                mEmptyImageView.setVisibility(View.VISIBLE);
                                mEmptyTextView.setText(R.string.textview_empty_external_storage_root);
                            } else {
                                mEmptyTextView.setText(R.string.textview_empty_file_list);
                            }
                            break;
                    }
                    mEmptyView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void saveCreatedDocument(PDFDoc doc, String title) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (doc == null) {
            return;
        }
        SecondaryFileFilter filter = null;
        try {
            boolean hasExtension = FilenameUtils.isExtension(title, "pdf");
            if (!hasExtension) {
                title = title + ".pdf";
            }
            String fileName = Utils.getFileNameNotInUse(mCurrentFolder, title);
            if (mCurrentFolder == null || Utils.isNullOrEmpty(fileName)) {
                CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                return;
            }
            ExternalFileInfo documentFile = mCurrentFolder.createFile("application/pdf", fileName);
            if (documentFile == null) {
                return;
            }
            filter = new SecondaryFileFilter(activity, documentFile.getUri());
            doc.save(filter, SDFDoc.SaveMode.REMOVE_UNUSED);

            String toastMsg = getString(R.string.dialog_create_new_document_filename_success)
                + documentFile.getDocumentPath();
            CommonToast.showText(activity, toastMsg, Toast.LENGTH_LONG);

            if (mCallbacks != null) {
                mCallbacks.onExternalFileSelected(documentFile.getAbsolutePath(), "");
            }

            finishActionMode();
            reloadDocumentList(false);
        } catch (Exception e) {
            CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(doc, filter);
        }
    }

    @Override
    public void onLocalFolderSelected(int requestCode, Object object, File folder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (requestCode == RequestCode.MOVE_FILE) {
            if (mSelectedFile != null) {
                ExternalFileManager.move(activity, new ArrayList<>(Collections.singletonList(mSelectedFile)), folder, this);
                mSelectedFile = null; // reset selected file
            }
        } else if (requestCode == RequestCode.MOVE_FILE_LIST) {
            ExternalFileManager.move(activity, mFileInfoSelectedList, folder, this);
        }
    }

    @Override
    public void onExternalFolderSelected(int requestCode, Object object, ExternalFileInfo folder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (requestCode == RequestCode.MOVE_FILE) {
            if (mSelectedFile != null) {
                ExternalFileManager.move(activity, new ArrayList<>(Collections.singletonList(mSelectedFile)), folder, this);
            }
            mSelectedFile = null; // reset selected file
        } else if (requestCode == RequestCode.MOVE_FILE_LIST) {
            ExternalFileManager.move(activity, mFileInfoSelectedList, folder, this);
        }
    }

    @Override
    public void onExternalFileRenamed(ExternalFileInfo oldFile, ExternalFileInfo newFile) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mSelectedFile == null || oldFile.getName().equals(mSelectedFile.getName())) {
            mSelectedFile = newFile; // update mSelectedFile
        }

        finishActionMode();
        hideFileInfoDrawer();
        if (oldFile.isDirectory() && findBreadcrumb(oldFile) != -1) {
            rebuildBreadcrumbBar(activity, oldFile.getParent());
        }

        reloadDocumentList(false); // Don't reload root, since they cannot be renamed

        FileInfo oldFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, oldFile.getUri().toString(),
            oldFile.getFileName(), false, 1);
        FileInfo newFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, newFile.getUri().toString(),
            newFile.getFileName(), false, 1);
        handleFileUpdated(oldFileInfo, newFileInfo);
        try {
            PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                oldFile.getAbsolutePath(), newFile.getAbsolutePath(), newFile.getFileName());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    @Override
    public void onExternalFileDuplicated(ExternalFileInfo fileCopy) {
        if (fileCopy != null) { // Duplication was successful
            finishActionMode();
            hideFileInfoDrawer();
            reloadDocumentList(false); // Don't reload roots, since they cannot be duplicated
        }
    }

    @Override
    public void onExternalFileMoved(Map<ExternalFileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        finishActionMode();
        hideFileInfoDrawer();

        for (Map.Entry<ExternalFileInfo, Boolean> entry : filesMoved.entrySet()) {
            if (entry.getValue()) {
                ExternalFileInfo file = entry.getKey();
                // File was moved successfully - remove file from list
                if (mAdapter != null) {
                    mAdapter.remove(file);
                }

                FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, file.getAbsolutePath(),
                    file.getFileName(), false, 1);
                try {
                    String targetFilePath = ExternalFileInfo.appendPathComponent(targetFolder.getUri(), file.getFileName()).toString();
                    FileInfo targetFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, targetFilePath,
                        file.getFileName(), false, 1);
                    // Update recent and favorite lists
                    handleFileUpdated(fileInfo, targetFileInfo);
                    // Update tab info
                    PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                        fileInfo.getAbsolutePath(), targetFileInfo.getAbsolutePath(), targetFileInfo.getFileName());
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }

        reloadDocumentList(false);
    }

    @Override
    public void onExternalFileMoved(Map<ExternalFileInfo, Boolean> filesMoved, File targetFolder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        finishActionMode();
        hideFileInfoDrawer();

        for (Map.Entry<ExternalFileInfo, Boolean> entry : filesMoved.entrySet()) {
            if (entry.getValue()) {
                ExternalFileInfo file = entry.getKey();
                // File was moved successfully - remove file from list
                if (mAdapter != null) {
                    mAdapter.remove(file);
                }

                try {
                    FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, file.getAbsolutePath(),
                        file.getFileName(), false, 1);
                    File targetFile = new File(targetFolder, file.getFileName());
                    FileInfo targetFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, targetFile);
                    // Update recent and favorite lists
                    handleFileUpdated(fileInfo, targetFileInfo);
                    // Update tab info
                    PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                        fileInfo.getAbsolutePath(), targetFileInfo.getAbsolutePath(), targetFileInfo.getFileName());
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }

        reloadDocumentList(false);
    }

    @Override
    public void onExternalFileDeleted(ArrayList<ExternalFileInfo> deletedFiles) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        finishActionMode();
        hideFileInfoDrawer();
        if (deletedFiles != null && !deletedFiles.isEmpty()) {
            // Remove files from recent and favorite lists
            boolean rebuildBreadcrumbs = false;
            ArrayList<FileInfo> filesToRemove = new ArrayList<>();
            for (ExternalFileInfo extFileInfo : deletedFiles) {
                FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, extFileInfo.getAbsolutePath(),
                    extFileInfo.getFileName(), false, 1);
                filesToRemove.add(fileInfo);

                PdfViewCtrlTabsManager.getInstance().removePdfViewCtrlTabInfo(activity, extFileInfo.getAbsolutePath());
                if (!rebuildBreadcrumbs && extFileInfo.isDirectory() && findBreadcrumb(extFileInfo) != -1) {
                    // File was part of the breadcrumb bar - rebuild breadcrumbs
                    rebuildBreadcrumbs = true;
                }
                if (mAdapter != null) {
                    mAdapter.evictFromMemoryCache(extFileInfo.getAbsolutePath());
                }
            }
            if (rebuildBreadcrumbs) {
                rebuildBreadcrumbBar(activity, null);
            }
            handleFilesRemoved(filesToRemove);
        }
        reloadDocumentList(true); // Reload roots, in case one or more roots were removed
    }

    @Override
    public void onRootsRemoved(ArrayList<ExternalFileInfo> deletedFiles) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        finishActionMode();
        hideFileInfoDrawer();
        // Clean up the breadcrumb bar
        if (deletedFiles != null && deletedFiles.size() > 0) {
            for (ExternalFileInfo folder : deletedFiles) {
                if (findBreadcrumb(folder) == 1) {
                    // Folder was part of the breadcrumb bar - rebuild breadcrumbs
                    rebuildBreadcrumbBar(activity, null);
                    break;
                }
            }
        }
        reloadDocumentList(true);
    }

    @Override
    public void onExternalFolderCreated(ExternalFileInfo rootFolder, ExternalFileInfo newFolder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (newFolder != null) {
            appendBreadcrumb(activity, newFolder);
            // Navigate to directory
            mCurrentFolder = newFolder;
            if (mCurrentRoot == null) {
                // A root folder has been selected
                mCurrentRoot = newFolder;
            }
        }
        reloadDocumentList(false); // Don't reload roots, since root folders cannot be created this way
    }

    @Override
    public void onExternalFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {
        finishActionMode();
        hideFileInfoDrawer();
        reloadDocumentList(false); // Don't reload roots, since roots cannot be merged
        if (mCallbacks != null && newFile != null) {
            // Open merged file in viewer
            if (newFile.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                mCallbacks.onFileSelected(newFile.getFile(), "");
            } else if (newFile.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                mCallbacks.onExternalFileSelected(newFile.getAbsolutePath(), "");
            }
        }
        MiscUtils.removeFiles(filesToDelete);
    }

    @Override
    public void onShowFileInfo(int position) {
        if (mFileUtilCallbacks != null) {
            mSelectedFile = mAdapter.getItem(position);
            mFileInfoDrawer = mFileUtilCallbacks.showFileInfoDrawer(mFileInfoDrawerCallback);
        }
    }

    private void resumeFragment() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mViewerLaunching = false;

        mSavedFolderUri = PdfViewCtrlSettingsManager.getSavedExternalFolderUri(activity);
        mSavedLeafUri = PdfViewCtrlSettingsManager.getSavedExternalFolderTreeUri(activity);

        reloadDocumentList(true);

        if (mExternalStorageViewFragmentListener != null) {
            mExternalStorageViewFragmentListener.onExternalStorageShown();
        }

        updateSpanCount(mSpanCount);
    }

    private void pauseFragment() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mFilterText = getFilterText();

        if (mIsSearchMode && !mViewerLaunching) {
            finishSearchView();
        }

        if (mPopulateSdFolderTask != null) {
            mPopulateSdFolderTask.cancel(true);
        }

        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.cleanupResources();
        }

        // Save the current folder and tree uris
        String folderUri = "";
        String treeUri = "";
        if (mCurrentFolder != null && mCurrentRoot != null) {
            folderUri = mCurrentFolder.getUri().toString();
        }
        if (mBreadcrumbBarLayout != null && mBreadcrumbBarLayout.getChildCount() > 0) {
            View crumb = mBreadcrumbBarLayout.getChildAt(mBreadcrumbBarLayout.getChildCount() - 1);
            Object tag = crumb.getTag();
            if (tag != null && tag instanceof ExternalFileInfo) {
                ExternalFileInfo file = (ExternalFileInfo) tag;
                treeUri = file.getUri().toString();
            }
        }
        PdfViewCtrlSettingsManager.updateSavedExternalFolderUri(activity, folderUri);
        PdfViewCtrlSettingsManager.updateSavedExternalFolderTreeUri(activity, treeUri);

        if (mExternalStorageViewFragmentListener != null) {
            mExternalStorageViewFragmentListener.onExternalStorageHidden();
        }
    }

    private FileInfoDrawer.Callback mFileInfoDrawerCallback = new FileInfoDrawer.Callback() {

        FileInfo mFileInfo; // FileInfo version of selected file - created when needed

        MenuItem itemDuplicate;
        MenuItem itemEdit;
        MenuItem itemDelete;
        MenuItem itemMove;
        MenuItem itemMerge;
        MenuItem itemFavorite;
        MenuItem itemShare;

        int mPageCount;
        String mAuthor;
        String mTitle;
        String mProducer;
        String mCreator;

        ThumbnailWorker mThumbnailWorker;
        WeakReference<ImageViewTopCrop> mImageViewReference;

        @Override
        public CharSequence onPrepareTitle(FileInfoDrawer drawer) {
            return (mSelectedFile != null) ? mSelectedFile.getFileName() : null;
        }

        @Override
        public void onPrepareHeaderImage(FileInfoDrawer drawer, ImageViewTopCrop imageView) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            if (mImageViewReference == null ||
                (mImageViewReference.get() != null && !mImageViewReference.get().equals(imageView))) {
                mImageViewReference = new WeakReference<>(imageView);
            }

            if (mSelectedFile == null) {
                return;
            }

            if (mSelectedFile.isDirectory()) {
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE); // Adjust scale type for folder
                imageView.setImageResource(R.drawable.ic_folder_black_24dp);
                int folderColorRes = Utils.getResourceColor(getContext(), getResources().getString(R.string.folder_color));
                if (folderColorRes == 0) {
                    folderColorRes = android.R.color.black;
                }
                imageView.getDrawable().mutate().setColorFilter(getResources().getColor(folderColorRes), PorterDuff.Mode.SRC_IN);
            } else {
                // Setup thumbnail worker, if required
                if (mThumbnailWorker == null) {
                    Point dimensions = drawer.getDimensions();
                    mThumbnailWorker = new ThumbnailWorker(activity, dimensions.x, dimensions.y, null);
                    mThumbnailWorker.setListener(mThumbnailWorkerListener);
                }

                if (mSelectedFile.isSecured() || mSelectedFile.isPackage()) {
                    int errorRes = Utils.getResourceDrawable(getContext(), getResources().getString(R.string.thumb_error_res_name));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView.setImageResource(errorRes);
                } else {
                    imageView.setScaleType(ImageView.ScaleType.MATRIX);
                    mThumbnailWorker.tryLoadImageWithUuid(0, mSelectedFile.getIdentifier(), null, imageView);
                }
            }
        }

        @Override
        public boolean onPrepareIsSecured(FileInfoDrawer drawer) {
            return mSelectedFile != null && mSelectedFile.isSecured();
        }

        @Override
        public CharSequence onPrepareMainContent(FileInfoDrawer drawer) {
            return getFileInfoTextBody();
        }

        @Override
        public boolean onCreateDrawerMenu(FileInfoDrawer drawer, Menu menu) {
            Activity activity = getActivity();
            if (activity == null) {
                return false;
            }
            activity.getMenuInflater().inflate(R.menu.cab_fragment_file_operations, menu);

            itemEdit = menu.findItem(R.id.cab_file_rename);
            itemDuplicate = menu.findItem(R.id.cab_file_copy);
            itemMove = menu.findItem(R.id.cab_file_move);
            itemDelete = menu.findItem(R.id.cab_file_delete);
            itemMerge = menu.findItem(R.id.cab_file_merge);
            itemFavorite = menu.findItem(R.id.cab_file_favorite);
            itemShare = menu.findItem(R.id.cab_file_share);

            return true;
        }

        @Override
        public boolean onPrepareDrawerMenu(FileInfoDrawer drawer, Menu menu) {
            Activity activity = getActivity();
            if (activity == null) {
                return false;
            }
            if (menu != null && mSelectedFile != null) {
                if (mSelectedFile.isDirectory()) {
                    itemEdit.setVisible(true);
                    itemDuplicate.setVisible(false);
                    itemMove.setVisible(false);
                    itemDelete.setVisible(true);
                    itemMerge.setVisible(false);
                    itemFavorite.setVisible(true);
                    itemShare.setVisible(false);
                } else {
                    itemEdit.setVisible(true);
                    itemDuplicate.setVisible(true);
                    itemMove.setVisible(true);
                    itemDelete.setVisible(true);
                    itemMerge.setVisible(true);
                    itemFavorite.setVisible(true);
                    itemShare.setVisible(true);
                }

                if (canAddToFavorite(getFileInfo())) {
                    itemFavorite.setTitle(activity.getString(R.string.action_add_to_favorites));
                    itemFavorite.setTitleCondensed(activity.getString(R.string.action_favorite));
                    itemFavorite.setIcon(R.drawable.ic_star_outline_grey600_24dp);
                } else {
                    itemFavorite.setTitle(activity.getString(R.string.action_remove_from_favorites));
                    itemFavorite.setTitleCondensed(activity.getString(R.string.action_unfavorite));
                    itemFavorite.setIcon(R.drawable.ic_star_white_24dp);
                }

                // Adjust delete action's title for root vs non-root folder
                if (mCurrentRoot != null && mCurrentFolder != null) {
                    itemDelete.setTitle(getString(R.string.delete));
                } else {
                    itemDelete.setTitle(getString(R.string.undo_redo_annot_remove));
                }
            }

            return true;
        }

        @Override
        public boolean onDrawerMenuItemClicked(FileInfoDrawer drawer, MenuItem menuItem) {
            FragmentActivity activity = getActivity();
            if (activity == null || mSelectedFile == null) {
                return false;
            }
            if (menuItem.getItemId() == R.id.cab_file_rename) {
                ExternalFileManager.rename(activity, mSelectedFile, ExternalStorageViewFragment.this);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_copy) {
                ExternalFileManager.duplicate(activity, mSelectedFile, ExternalStorageViewFragment.this);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_move) {
                FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MOVE_FILE, mCurrentFolder.getUri());
                dialogFragment.setLocalFolderListener(ExternalStorageViewFragment.this);
                dialogFragment.setExternalFolderListener(ExternalStorageViewFragment.this);
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
                }
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_delete) {
                if (mCurrentRoot != null && mCurrentFolder != null) {
                    // Deleting non-root level item(s)
                    ExternalFileManager.delete(activity,
                        new ArrayList<>(Collections.singletonList(mSelectedFile)),
                        ExternalStorageViewFragment.this);
                } else {
                    // Removing a root level item
                    ExternalFileManager.removeAccess(activity,
                        new ArrayList<>(Collections.singletonList(mSelectedFile)),
                        ExternalStorageViewFragment.this);
                }
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_merge) {
                // Create and show file merge dialog-fragment
                handleMerge(new ArrayList<>(Collections.singletonList(getFileInfo())));
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_favorite) {
                int fileType = mSelectedFile.isDirectory() ? BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER : BaseFileInfo.FILE_TYPE_EXTERNAL;
                FileInfo fileInfo = new FileInfo(fileType, mSelectedFile.getAbsolutePath(),
                    mSelectedFile.getFileName(), false, 1);
                handleAddToFavorite(fileInfo);
                drawer.invalidate();
                // Update favorite file indicators
                Utils.safeNotifyDataSetChanged(mAdapter);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_share) {
                if (mOnPdfFileSharedListener != null) {
                    Intent intent = Utils.createGenericShareIntent(activity, mSelectedFile.getUri());
                    mOnPdfFileSharedListener.onPdfFileShared(intent);
                } else {
                    Utils.shareGenericFile(activity, mSelectedFile.getUri());
                }
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_FILEBROWSER, "Item share clicked", AnalyticsHandlerAdapter.LABEL_SD_CARD);
                return true;
            }
            return false;
        }

        @Override
        public void onThumbnailClicked(FileInfoDrawer drawer) {
            drawer.invalidate();
            if (mSelectedFile != null) {
                onFileClicked(mSelectedFile);
            }
            onHideDrawer(drawer);
        }

        @Override
        public void onShowDrawer(FileInfoDrawer drawer) {

        }

        @Override
        public void onHideDrawer(FileInfoDrawer drawer) {
            cancelAllThumbRequests();

            mFileInfo = null;
            mSelectedFile = null;
            mFileInfoDrawer = null;
        }

        void cancelAllThumbRequests() {
            if (mThumbnailWorker != null) {
                mThumbnailWorker.abortCancelTask();
                mThumbnailWorker.cancelAllThumbRequests();
            }
        }

        private FileInfo getFileInfo() {
            if (mFileInfo == null && mSelectedFile != null) {
                mFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, mSelectedFile.getAbsolutePath(), mSelectedFile.getFileName(), false, 1);
            }
            return mFileInfo;
        }

        private CharSequence getFileInfoTextBody() {
            Activity activity = getActivity();
            if (activity == null || mSelectedFile == null) {
                return null;
            }
            StringBuilder textBodyBuilder = new StringBuilder();
            Resources res = activity.getResources();

            if (!mSelectedFile.isDirectory()) {
                PDFDoc doc = null;
                SecondaryFileFilter filter = null;
                try {
                    filter = new SecondaryFileFilter(activity, mSelectedFile.getUri());
                    doc = new PDFDoc(filter);
                    doc.initSecurityHandler();
                    loadDocInfo(doc);
                } catch (Exception e) {
                    mPageCount = -1;
                    mAuthor = null;
                    mTitle = null;
                    mCreator = null;
                    mProducer = null;
                } finally {
                    Utils.closeQuietly(doc, filter);
                }
            } else {
                mPageCount = -1;
                mAuthor = null;
                mTitle = null;
                mCreator = null;
                mProducer = null;
            }

            if (!mSelectedFile.isDirectory()) {
                textBodyBuilder.append(res.getString(R.string.file_info_document_title,
                    Utils.isNullOrEmpty(mTitle) ? res.getString(R.string.file_info_document_attr_not_available) : mTitle));
                textBodyBuilder.append("<br>");

                textBodyBuilder.append(res.getString(R.string.file_info_document_author,
                    Utils.isNullOrEmpty(mAuthor) ? res.getString(R.string.file_info_document_attr_not_available) : mAuthor));
                textBodyBuilder.append("<br>");

                String pageCountStr = "" + mPageCount;
                textBodyBuilder.append(res.getString(R.string.file_info_document_pages,
                    mPageCount < 0 ? res.getString(R.string.file_info_document_attr_not_available) : Utils.getLocaleDigits(pageCountStr)));
                textBodyBuilder.append("<br>");

                // Directory
                try {
                    // Parent Directory
                    textBodyBuilder.append(res.getString(R.string.file_info_document_path, mSelectedFile.getParentRelativePath() + "/" + mSelectedFile.getFileName()));
                    textBodyBuilder.append("<br>");
                } catch (NullPointerException ignored) {

                }
                // Size info
                textBodyBuilder.append(res.getString(R.string.file_info_document_size, mSelectedFile.getSizeInfo()));
                textBodyBuilder.append("<br>");
                // Date modified
                textBodyBuilder.append(res.getString(R.string.file_info_document_date_modified, DateFormat.getInstance().format(new Date(mSelectedFile.getRawModifiedDate()))));
                textBodyBuilder.append("<br>");

                //Producer
                textBodyBuilder.append(res.getString(R.string.file_info_document_producer,
                    Utils.isNullOrEmpty(mProducer) ? res.getString(R.string.file_info_document_attr_not_available) : mProducer));
                textBodyBuilder.append("<br>");

                //Creator
                textBodyBuilder.append(res.getString(R.string.file_info_document_creator,
                    Utils.isNullOrEmpty(mCreator) ? res.getString(R.string.file_info_document_attr_not_available) : mCreator));
                textBodyBuilder.append("<br>");

            } else {
                if (textBodyBuilder.length() > 0) {
                    textBodyBuilder.append("<br>");
                }
                try {
                    // Parent Directory
                    textBodyBuilder.append(res.getString(R.string.file_info_document_path, mSelectedFile.getParentRelativePath() + "/" + mSelectedFile.getFileName()));
                    textBodyBuilder.append("<br>");
                } catch (NullPointerException ignored) {

                }
                // Size info: x files, y folders
                int[] fileFolderCount = mSelectedFile.getFileCount();
                String sizeInfo = res.getString(R.string.dialog_folder_info_size, fileFolderCount[0], fileFolderCount[1]);
                textBodyBuilder.append(res.getString(R.string.file_info_document_folder_contains, sizeInfo));
                textBodyBuilder.append("<br>");
                // Date modified
                textBodyBuilder.append(res.getString(R.string.file_info_document_date_modified,
                    DateFormat.getInstance().format(new Date(mSelectedFile.getRawModifiedDate()))));
            }

            return Html.fromHtml(textBodyBuilder.toString());
        }

        private void loadDocInfo(PDFDoc doc) {
            if (doc == null) {
                return;
            }
            boolean shouldUnlockRead = false;
            try {
                doc.lockRead();
                shouldUnlockRead = true;

                mPageCount = doc.getPageCount();

                PDFDocInfo docInfo = doc.getDocInfo();
                if (docInfo != null) {
                    mAuthor = docInfo.getAuthor();
                    mTitle = docInfo.getTitle();
                    mCreator = docInfo.getCreator();
                    mProducer = docInfo.getProducer();
                }
            } catch (PDFNetException e) {
                mPageCount = -1;
                mAuthor = null;
                mTitle = null;
                mCreator = null;
                mProducer = null;
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(doc);
                }
            }
        }

        ThumbnailWorker.ThumbnailWorkerListener mThumbnailWorkerListener = new ThumbnailWorker.ThumbnailWorkerListener() {
            @Override
            public void onThumbnailReady(int result, int position, String iconPath, String identifier) {
                ImageViewTopCrop imageView = (mImageViewReference != null) ? mImageViewReference.get() : null;
                if (mSelectedFile == null || imageView == null) {
                    return;
                }

                if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR) {
                    // avoid flashing caused by the callback
                    mSelectedFile.setIsSecured(true);
                }
                if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PACKAGE_ERROR) {
                    // avoid flashing caused by the callback
                    mSelectedFile.setIsPackage(true);
                }
                if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_NOT_FOUNT) {
                    // create this file instead
                    mThumbnailWorker.tryLoadImageFromFilter(position, mSelectedFile.getIdentifier(), mSelectedFile.getAbsolutePath());
                    return;
                }

                if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR || result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PACKAGE_ERROR) {
                    // Thumbnail has been generated before, and a placeholder icon should be used
                    int errorRes = Utils.getResourceDrawable(getContext(), getResources().getString(R.string.thumb_error_res_name));
                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    imageView.setImageResource(errorRes);
                } else if (mThumbnailWorker != null) {
                    // adds path to local cache for later access
                    ThumbnailPathCacheManager.getInstance().putThumbnailPath(mSelectedFile.getIdentifier(),
                        iconPath, mThumbnailWorker.getMinXSize(), mThumbnailWorker.getMinYSize());

                    imageView.setScaleType(ImageView.ScaleType.MATRIX);
                    mThumbnailWorker.tryLoadImageWithPath(position, mSelectedFile.getAbsolutePath(), iconPath, imageView);
                }
            }
        };
    };

    @SuppressWarnings("unused")
    public void setCurrentFolder(String folderUri, String treeUri) {
        if (folderUri != null) {
            mSavedFolderUri = folderUri;
        }
        if (treeUri != null) {
            mSavedLeafUri = treeUri;
        }
        mCurrentFolder = null;
        mCurrentRoot = null;
        reloadDocumentList(true);
    }

    public void setExternalStorageViewFragmentListener(ExternalStorageViewFragmentListener listener) {
        mExternalStorageViewFragmentListener = listener;
    }

    private void onFileClicked(@NonNull ExternalFileInfo file) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mIsSearchMode) {
            hideSoftKeyboard();
        }
        if (file.isDirectory()) {
            appendBreadcrumb(activity, file);
            // Navigate to directory
            mCurrentFolder = file;
            if (mCurrentRoot == null) {
                // A root folder has been selected
                mCurrentRoot = file;
            }
            finishSearchView();
        } else {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                AnalyticsParam.openFileParam(file, activity.getContentResolver(), AnalyticsHandlerAdapter.SCREEN_SD_CARD));
            // Open file
            mCallbacks.onExternalFileSelected(file.getUri().toString(), "");
        }

        if (mCurrentFolder != null) {
            reloadDocumentList(false);
        }
    }

    // close soft keyboard if in searching mode
    private void closeSearch() {
        if (mIsSearchMode && mSearchMenuItem != null) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            EditText editText = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
            if (editText.isFocused()) {
                editText.onEditorAction(EditorInfo.IME_ACTION_SEARCH);
            }
        }
    }

    private static class LoadingFileHandler extends Handler {
        private final WeakReference<ExternalStorageViewFragment> mFragment;

        LoadingFileHandler(ExternalStorageViewFragment fragment) {
            mFragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            ExternalStorageViewFragment fragment = mFragment.get();
            if (fragment != null && fragment.mEmptyTextView != null) {
                fragment.mEmptyTextView.setText(R.string.loading_files_wait);
                fragment.mEmptyView.setVisibility(View.VISIBLE);
                fragment.mEmptyImageView.setVisibility(View.GONE);
            }
            removeMessages(0);
        }
    }

    private final LoadingFileHandler mLoadingFileHandler = new LoadingFileHandler(this);

    @Override
    public void onPopulateSdFilesTaskStarted(
    ) {

        Context context = getContext();
        if (context == null) {
            return;
        }

        mLoadingFileHandler.sendEmptyMessageDelayed(0, 100);
        if (mProgressBarView != null) {
            mProgressBarView.setVisibility(View.VISIBLE);
        }

        mIsFullSearchDone = false;

        synchronized (mFileListLock) {
            mFileInfoList.clear();
        }

        updateFileListFilter();

        if (Utils.isNullOrEmpty(mSavedFolderUri) && Utils.isNullOrEmpty(mSavedLeafUri)) {
            if (mBreadcrumbBarLayout != null && mBreadcrumbBarLayout.getChildCount() == 0) {
                rebuildBreadcrumbBar(context, null);
            }
            if (mCurrentFolder != null && mCurrentRoot != null) {
                setCurrentBreadcrumb(findBreadcrumb(mCurrentFolder));
            } else {
                setCurrentBreadcrumb(0);
            }
        }

    }

    @Override
    public void onCurrentRootRemoved(
    ) {

        mCurrentRoot = mCurrentFolder = null;

    }

    // the first update when saved folder is ready
    @Override
    public void onPopulateSdFilesTaskProgressUpdated(
        ExternalFileInfo savedRoot,
        ExternalFileInfo savedFolder,
        ExternalFileInfo savedLeaf) {

        // at this point breadcrum bar should be updated
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (savedFolder != null && savedRoot != null) {
            // load the new built folder
            mCurrentFolder = savedFolder;
            mCurrentRoot = savedRoot;
        } else {
            // folder could not be built, so load root folder
            mCurrentFolder = null;
            mCurrentRoot = null;
        }
        if (savedLeaf != null) {
            rebuildBreadcrumbBar(context, savedLeaf);
        }
        if (mCurrentFolder != null) {
            setCurrentBreadcrumb(findBreadcrumb(mCurrentFolder));
        }

        // no need to build saved folder anymore. the relevant settings will be
        // updated in pauseFragment
        mSavedFolderUri = null;
        mSavedLeafUri = null;

    }

    // the second update when the list of files immediately beneath the current
    // folder was created
    @Override
    public void onPopulateSdFilesTaskProgressUpdated(
        List<ExternalFileInfo> rootList
    ) {

        // at this point no loading progress should be shown any longer
        mLoadingFileHandler.removeMessages(0);
        if (mProgressBarView != null) {
            mProgressBarView.setVisibility(View.GONE);
        }

        if (rootList != null && mRoots != null) {
            mRoots.clear();
            mRoots.addAll(rootList);
        }

        updateFileListFilter();

    }

    @Override
    public void onPopulateSdFilesTaskFinished(
    ) {

        mIsFullSearchDone = true;
        updateFileListFilter();

    }

}
