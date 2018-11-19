//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.pdftron.common.PDFNetException;
import com.pdftron.demo.R;
import com.pdftron.demo.R2;
import com.pdftron.demo.asynctask.PopulateAllFilesTask;
import com.pdftron.demo.asynctask.RetrieveAllFilesCacheTask;
import com.pdftron.demo.asynctask.RetrieveAllFilesHeaderCacheTask;
import com.pdftron.demo.asynctask.SaveFileListCacheTask;
import com.pdftron.demo.asynctask.SortAllFilesTask;
import com.pdftron.demo.asynctask.UpdateAllFilesTask;
import com.pdftron.demo.dialog.FilePickerDialogFragment;
import com.pdftron.demo.dialog.MergeDialogFragment;
import com.pdftron.demo.model.FileHeader;
import com.pdftron.demo.navigation.adapter.BaseFileAdapter;
import com.pdftron.demo.navigation.adapter.LocalFileAdapter;
import com.pdftron.demo.navigation.callbacks.FileManagementListener;
import com.pdftron.demo.navigation.callbacks.FileUtilCallbacks;
import com.pdftron.demo.navigation.callbacks.JumpNavigationCallbacks;
import com.pdftron.demo.navigation.callbacks.MainActivityListener;
import com.pdftron.demo.utils.AddDocPdfHelper;
import com.pdftron.demo.utils.CacheUtils;
import com.pdftron.demo.utils.FileInfoComparator;
import com.pdftron.demo.utils.FileListFilter;
import com.pdftron.demo.utils.FileManager;
import com.pdftron.demo.utils.Logger;
import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.demo.utils.RecursiveFileObserver;
import com.pdftron.demo.utils.ThumbnailPathCacheManager;
import com.pdftron.demo.utils.ThumbnailWorker;
import com.pdftron.demo.widget.FileTypeFilterPopupWindow;
import com.pdftron.demo.widget.ImageViewTopCrop;
import com.pdftron.demo.widget.MoveUpwardBehaviour;
import com.pdftron.demo.widget.SimpleStickyRecyclerView;
import com.pdftron.demo.widget.StickyHeader;
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
import com.pdftron.pdf.utils.BookmarkManager;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.PdfViewCtrlTabsManager;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.ItemSelectionHelper;
import com.pdftron.sdf.SDFDoc;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class LocalFileViewFragment extends FileBrowserViewFragment implements
    SearchView.OnQueryTextListener,
    FileManagementListener,
    FilePickerDialogFragment.LocalFolderListener,
    FilePickerDialogFragment.ExternalFolderListener,
    BaseFileAdapter.AdapterListener,
    MainActivityListener,
    MergeDialogFragment.MergeDialogFragmentListener,
    ActionMode.Callback,
    LocalFileAdapter.OnReloadListener,
    PopulateAllFilesTask.Callback,
    UpdateAllFilesTask.Callback,
    RetrieveAllFilesCacheTask.Callback,
    RetrieveAllFilesHeaderCacheTask.Callback,
    SortAllFilesTask.Callback {

    private static final String TAG = LocalFileViewFragment.class.getName();
    private static final boolean DEBUG = false;

    @BindView(R2.id.recycler_view)
    protected SimpleStickyRecyclerView mRecyclerView;
    @BindView(R2.id.empty_text_view)
    protected TextView mEmptyTextView;
    @BindView(R2.id.progress_bar_view)
    protected ProgressBar mProgressBarView;
    @BindView(R2.id.fab_menu)
    protected FloatingActionMenu mFabMenu;
    @BindView(R2.id.sticky_header)
    protected StickyHeader stickyHeader;

    private Unbinder unbinder;

    protected final Object mFileChangeLock = new Object();
    protected final Object mUpdateTaskLock = new Object();

    // mFileInfoList should not be queried directly, since it does not reflect what is actually
    // being displayed.
    protected ArrayList<FileInfo> mFileInfoList = new ArrayList<>();
    protected ArrayList<FileInfo> mFileInfoSelectedList = new ArrayList<>();
    protected ArrayList<FileInfo> mMergeFileList;
    protected ArrayList<FileInfo> mMergeTempFileList;
    protected FileInfo mSelectedFile;

    protected String mDocumentTitle;

    private FileUtilCallbacks mFileUtilCallbacks;
    private JumpNavigationCallbacks mJumpNavigationCallbacks;

    protected LocalFileAdapter mAdapter;
    protected int mSpanCount;
    protected ItemSelectionHelper mItemSelectionHelper;

    private Menu mOptionsMenu;
    private MenuItem mSearchMenuItem;
    private FileInfoDrawer mFileInfoDrawer;
    private RecursiveFileObserver mFileObserver;
    private ArrayList<UpdateAllFilesTask> mUpdateAllFilesTasks = new ArrayList<>();
    private PopulateAllFilesTask mPopulateAllFilesTask;
    private RetrieveAllFilesCacheTask mRetrieveAllFilesCacheTask;
    private RetrieveAllFilesHeaderCacheTask mRetrieveAllFilesHeaderCacheTask;
    private SortAllFilesTask mSortAllFilesTask;
    private PDFDoc mCreatedDoc;
    private String mCreatedDocumentTitle;
    private String mImageFilePath;
    private Uri mImageUri;
    private String mImageFileDisplayName;
    private Uri mOutputFileUri;
    private boolean mIsCamera;
    private boolean mIsSearchMode;
    private boolean mViewerLaunching;
    private boolean mIsFinishedFiltering;
    private Comparator<FileInfo> mSortMode;
    private boolean mActionLock = true;
    private boolean mFileEventLock;
    private FileTypeFilterPopupWindow mFileTypeFilterListPopup;
    private String mFilterText = "";

    private MenuItem itemDuplicate;
    private MenuItem itemEdit;
    private MenuItem itemDelete;
    private MenuItem itemMove;
    private MenuItem itemMerge;
    private MenuItem itemFavorite;
    private MenuItem itemShare;

    private LocalFileViewFragmentListener mLocalFileViewFragmentListener;

    public interface LocalFileViewFragmentListener {

        void onLocalFileShown();

        void onLocalFileHidden();
    }

    public static LocalFileViewFragment newInstance() {
        return new LocalFileViewFragment();
    }

    // Called each time the action mode is shown. Always called after
    // onCreateActionMode, but may be called multiple times if the mode is
    // invalidated.
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        itemDelete.setVisible(true);
        itemMove.setVisible(true);
        itemMerge.setVisible(true);
        itemShare.setVisible(true);

        if (mFileInfoSelectedList.size() > 1) {
            itemDuplicate.setVisible(false);
            itemEdit.setVisible(false);
            itemFavorite.setVisible(false);
        } else {
            itemDuplicate.setVisible(true);
            itemEdit.setVisible(true);
            itemFavorite.setVisible(true);
            if (!mFileInfoSelectedList.isEmpty()) {
                if (canAddToFavorite(mFileInfoSelectedList.get(0))) {
                    itemFavorite.setTitle(activity.getString(R.string.action_add_to_favorites));
                } else {
                    itemFavorite.setTitle(activity.getString(R.string.action_remove_from_favorites));
                }
            }
        }
        mode.setTitle(Utils.getLocaleDigits(Integer.toString(mFileInfoSelectedList.size())));
        // Ensure items are always shown
        itemDuplicate.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemEdit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemDelete.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemMove.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        mActionMode = null;
        clearFileInfoSelectedList();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (super.onCreateActionMode(mode, menu)) {
            return true;
        }

        mode.getMenuInflater().inflate(R.menu.cab_fragment_file_operations, menu);

        itemDuplicate = menu.findItem(R.id.cab_file_copy);
        itemEdit = menu.findItem(R.id.cab_file_rename);
        itemDelete = menu.findItem(R.id.cab_file_delete);
        itemMove = menu.findItem(R.id.cab_file_move);
        itemMerge = menu.findItem(R.id.cab_file_merge);
        itemFavorite = menu.findItem(R.id.cab_file_favorite);
        itemShare = menu.findItem(R.id.cab_file_share);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        if (mFileInfoSelectedList.isEmpty()) {
            return false;
        }

        boolean isSDCardFile = Utils.isSdCardFile(activity, mFileInfoSelectedList.get(0).getFile());
        mFileEventLock = true;
        if (item.getItemId() == R.id.cab_file_rename) {
            if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.controls_misc_rename))) {
                finishActionMode();
                return true;
            }
            FileManager.rename(activity, mFileInfoSelectedList.get(0).getFile(), LocalFileViewFragment.this);
            return true;
        } else if (item.getItemId() == R.id.cab_file_copy) {
            if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.controls_misc_duplicate))) {
                finishActionMode();
                return true;
            }
            FileManager.duplicate(activity, mFileInfoSelectedList.get(0).getFile(), LocalFileViewFragment.this);
            return true;
        } else if (item.getItemId() == R.id.cab_file_move) {
            if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.action_file_move))) {
                finishActionMode();
                return true;
            }

            // Creates the dialog in full screen mode
            FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MOVE_FILE_LIST,
                Environment.getExternalStorageDirectory());
            dialogFragment.setLocalFolderListener(LocalFileViewFragment.this);
            dialogFragment.setExternalFolderListener(LocalFileViewFragment.this);
            dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
            }
            return true;
        } else if (item.getItemId() == R.id.cab_file_delete) {
            if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.delete))) {
                finishActionMode();
                return true;
            }

            FileManager.delete(activity, mFileInfoSelectedList, LocalFileViewFragment.this);
            return true;
        } else if (item.getItemId() == R.id.cab_file_merge) {
            if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.merge))) {
                finishActionMode();
                return true;
            }
            handleMerge(mFileInfoSelectedList);
            return true;
        } else if (item.getItemId() == R.id.cab_file_favorite) {
            handleAddToFavorite(mFileInfoSelectedList.get(0));

            finishActionMode();
            // Update favorite file indicators
            Utils.safeNotifyDataSetChanged(mAdapter);
            return true;
        } else if (item.getItemId() == R.id.cab_file_share) {
            if (mFileInfoSelectedList.size() > 1) {
                if (mOnPdfFileSharedListener != null) {
                    Intent intent = Utils.createShareIntents(activity, mFileInfoSelectedList);
                    mOnPdfFileSharedListener.onPdfFileShared(intent);
                    finishActionMode();
                } else {
                    Utils.sharePdfFiles(activity, mFileInfoSelectedList);
                }
            } else {
                if (mOnPdfFileSharedListener != null) {
                    Intent intent = Utils.createShareIntent(activity, mFileInfoSelectedList.get(0).getFile());
                    mOnPdfFileSharedListener.onPdfFileShared(intent);
                    finishActionMode();
                } else {
                    Utils.sharePdfFile(activity, mFileInfoSelectedList.get(0).getFile());
                }
            }
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_FILEBROWSER, "Item share clicked", AnalyticsHandlerAdapter.LABEL_ALL_DOCUMENTS);
            return true;
        }
        return false;
    }

    protected void handleMerge(ArrayList<FileInfo> files) {
        // Create and show file merge dialog-fragment
        MergeDialogFragment mergeDialog = getMergeDialogFragment(files, AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS);
        mergeDialog.initParams(LocalFileViewFragment.this);
        mergeDialog.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            mergeDialog.show(fragmentManager, "merge_dialog");
        }
    }

    protected void performMerge(FileInfo targetFile) {
        FileManager.merge(getActivity(), mMergeFileList, mMergeTempFileList, targetFile, LocalFileViewFragment.this);
    }

    protected LocalFileAdapter createAdapter() {
        return new LocalFileAdapter(getActivity(), mFileInfoList, mFileListLock,
            mSpanCount, this, mItemSelectionHelper);
    }

    protected boolean canAddToFavorite(FileInfo file) {
        FragmentActivity activity = getActivity();
        return !(activity == null || activity.isFinishing()) && (!getFavoriteFilesManager().containsFile(activity, file));
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

    protected void handleFileRemoved(FileInfo file) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        getRecentFilesManager().removeFile(activity, file);
        getFavoriteFilesManager().removeFile(activity, file);
    }

    protected void handleFilesRemoved(ArrayList<FileInfo> files) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        getRecentFilesManager().removeFiles(activity, files);
        getFavoriteFilesManager().removeFiles(activity, files);
    }

    private FileInfoDrawer.Callback mFileInfoDrawerCallback = new FileInfoDrawer.Callback() {

        int mPageCount;
        String mAuthor;
        String mTitle;

        String mProducer;
        String mCreator;

        ThumbnailWorker mThumbnailWorker;
        WeakReference<ImageViewTopCrop> mImageViewReference;
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
                    if (mFileInfoDrawer != null) {
                        mFileInfoDrawer.setIsSecured(true);
                    }
                } else {
                    if (mFileInfoDrawer != null) {
                        mFileInfoDrawer.setIsSecured(false);
                    }
                }
                if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PACKAGE_ERROR) {
                    // avoid flashing caused by the callback
                    mSelectedFile.setIsPackage(true);
                }

                if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR || result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PACKAGE_ERROR) {
                    // Thumbnail has been generated before, and a placeholder icon should be used
                    int errorRes = Utils.getResourceDrawable(getContext(), getResources().getString(R.string.thumb_error_res_name));
                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    imageView.setImageResource(errorRes);
                } else if (mThumbnailWorker != null) {
                    // adds path to local cache for later access
                    ThumbnailPathCacheManager.getInstance().putThumbnailPath(mSelectedFile.getAbsolutePath(),
                        iconPath, mThumbnailWorker.getMinXSize(), mThumbnailWorker.getMinYSize());

                    imageView.setScaleType(ImageView.ScaleType.MATRIX);
                    mThumbnailWorker.tryLoadImageWithPath(position, mSelectedFile.getAbsolutePath(), iconPath, imageView);
                }
            }
        };

        @Override
        public CharSequence onPrepareTitle(FileInfoDrawer drawer) {
            return (mSelectedFile != null) ? mSelectedFile.getName() : null;
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
            // Setup thumbnail worker, if required
            if (mThumbnailWorker == null) {
                Point dimensions = drawer.getDimensions();
                mThumbnailWorker = new ThumbnailWorker(activity, dimensions.x, dimensions.y, null);
                mThumbnailWorker.setListener(mThumbnailWorkerListener);
            }

            if (mSelectedFile != null) {
                drawer.setIsSecured(mSelectedFile.isSecured());

                if (mSelectedFile.isSecured() || mSelectedFile.isPackage()) {
                    int errorRes = Utils.getResourceDrawable(activity, getResources().getString(R.string.thumb_error_res_name));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView.setImageResource(errorRes);
                } else {
                    imageView.setScaleType(ImageView.ScaleType.MATRIX);
                    mThumbnailWorker.tryLoadImageWithPath(0, mSelectedFile.getAbsolutePath(), null, imageView);
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
            return true;
        }

        @Override
        public boolean onPrepareDrawerMenu(FileInfoDrawer drawer, Menu menu) {
            Activity activity = getActivity();
            if (activity == null || mSelectedFile == null || menu == null) {
                return false;
            }
            boolean changed = false;
            MenuItem menuItem = menu.findItem(R.id.cab_file_favorite);
            if (menuItem != null) {
                if (canAddToFavorite(mSelectedFile)) {
                    menuItem.setTitle(activity.getString(R.string.action_add_to_favorites));
                    menuItem.setTitleCondensed(activity.getString(R.string.action_favorite));
                    menuItem.setIcon(R.drawable.ic_star_outline_grey600_24dp);
                } else {
                    menuItem.setTitle(activity.getString(R.string.action_remove_from_favorites));
                    menuItem.setTitleCondensed(activity.getString(R.string.action_unfavorite));
                    menuItem.setIcon(R.drawable.ic_star_white_24dp);
                }
                changed = true;
            }
            return changed;
        }

        @Override
        public boolean onDrawerMenuItemClicked(FileInfoDrawer drawer, MenuItem menuItem) {
            FragmentActivity activity = getActivity();
            if (activity == null || mSelectedFile == null || mActionLock) {
                return false;
            }

            boolean isSDCardFile = Utils.isSdCardFile(activity, mSelectedFile.getFile());
            mFileEventLock = true;
            if (menuItem.getItemId() == R.id.cab_file_rename) {
                if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.controls_misc_rename))) {
                    hideFileInfoDrawer();
                    return true;
                }
                FileManager.rename(activity, mSelectedFile.getFile(), LocalFileViewFragment.this);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_copy) {
                if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.controls_misc_duplicate))) {
                    hideFileInfoDrawer();
                    return true;
                }
                FileManager.duplicate(activity, mSelectedFile.getFile(), LocalFileViewFragment.this);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_move) {
                if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.action_file_move))) {
                    hideFileInfoDrawer();
                    return true;
                }
                FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MOVE_FILE,
                    Environment.getExternalStorageDirectory());
                dialogFragment.setLocalFolderListener(LocalFileViewFragment.this);
                dialogFragment.setExternalFolderListener(LocalFileViewFragment.this);
                dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
                }
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_delete) {
                if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.delete))) {
                    hideFileInfoDrawer();
                    return true;
                }
                FileManager.delete(activity, new ArrayList<>(Collections.singletonList(mSelectedFile)), LocalFileViewFragment.this);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_merge) {
                if (isSDCardFile && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.merge))) {
                    hideFileInfoDrawer();
                    return true;
                }
                // Create and show file merge dialog-fragment
                handleMerge(new ArrayList<>(Collections.singletonList(mSelectedFile)));
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_favorite) {
                handleAddToFavorite(mSelectedFile);
                drawer.invalidate();
                // Update favorite file indicators
                Utils.safeNotifyDataSetChanged(mAdapter);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_share) {
                if (mOnPdfFileSharedListener != null) {
                    Intent intent = Utils.createShareIntent(activity, mSelectedFile.getFile());
                    mOnPdfFileSharedListener.onPdfFileShared(intent);
                } else {
                    Utils.sharePdfFile(activity, mSelectedFile.getFile());
                }
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_FILEBROWSER, "Item share clicked", AnalyticsHandlerAdapter.LABEL_ALL_DOCUMENTS);
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
        }

        @Override
        public void onShowDrawer(FileInfoDrawer drawer) {

        }

        @Override
        public void onHideDrawer(FileInfoDrawer drawer) {
            cancelAllThumbRequests();

            mSelectedFile = null;
            mFileInfoDrawer = null;
        }

        void cancelAllThumbRequests() {
            if (mThumbnailWorker != null) {
                mThumbnailWorker.abortCancelTask();
                mThumbnailWorker.cancelAllThumbRequests();
            }
        }

        private CharSequence getFileInfoTextBody() {
            Activity activity = getActivity();
            if (activity == null || mSelectedFile == null) {
                return null;
            }
            StringBuilder textBodyBuilder = new StringBuilder();
            Resources res = activity.getResources();

            try {
                PDFDoc doc = new PDFDoc(mSelectedFile.getAbsolutePath());
                doc.initSecurityHandler();
                loadDocInfo(doc);
            } catch (PDFNetException e) {
                mTitle = null;
                mAuthor = null;
                mProducer = null;
                mCreator = null;
                mPageCount = -1;
            }

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
            textBodyBuilder.append(res.getString(R.string.file_info_document_path, mSelectedFile.getAbsolutePath()));
            textBodyBuilder.append("<br>");
            // Size info
            textBodyBuilder.append(res.getString(R.string.file_info_document_size, mSelectedFile.getSizeInfo()));
            textBodyBuilder.append("<br>");
            // Date modified
            textBodyBuilder.append(res.getString(R.string.file_info_document_date_modified, mSelectedFile.getModifiedDate()));
            textBodyBuilder.append("<br>");

            //Producer
            textBodyBuilder.append(res.getString(R.string.file_info_document_producer,
                Utils.isNullOrEmpty(mProducer) ? res.getString(R.string.file_info_document_attr_not_available) : mProducer));
            textBodyBuilder.append("<br>");

            //Creator
            textBodyBuilder.append(res.getString(R.string.file_info_document_creator,
                Utils.isNullOrEmpty(mCreator) ? res.getString(R.string.file_info_document_attr_not_available) : mCreator));
            textBodyBuilder.append("<br>");

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
                    mProducer = docInfo.getProducer();
                    mCreator = docInfo.getCreator();
                    docInfo.getProducer();
                }
            } catch (PDFNetException e) {
                mPageCount = -1;
                mAuthor = null;
                mTitle = null;
                mProducer = null;
                mCreator = null;
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(doc);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        FileManager.initCache(getContext());
        if (CacheUtils.hasCache("cache_header_list_object")) {
            CacheUtils.deleteFile("cache_header_list_object");
        }
        Logger.INSTANCE.LogD(TAG, "onCreate");
        // Control whether a fragment instance is retained across Activity re-creation (such as from a configuration change).
        // This can only be used with fragments not in the back stack. If set, the fragment lifecycle will be slightly different when an activity is recreated
        setRetainInstance(true);

        // This Fragment wants to be able to have action bar items.
        setHasOptionsMenu(true);

        if (null != savedInstanceState) {
            mOutputFileUri = savedInstanceState.getParcelable("output_file_uri");
            mIsCamera = savedInstanceState.getBoolean("is_photo_from_camera");
        }
        mSpanCount = PdfViewCtrlSettingsManager.getGridSize(getActivity(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_LOCAL_FILES);
        if (PdfViewCtrlSettingsManager.getSortMode(getActivity()).equals(PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_NAME)) {
            if (mSpanCount > 0) {
                mSortMode = FileInfoComparator.fileNameOrder();
            } else {
                mSortMode = FileInfoComparator.absolutePathOrder();
            }
        } else {
            if (mSpanCount > 0) {
                mSortMode = FileInfoComparator.modifiedDateOrderOnly();
            } else {
                mSortMode = FileInfoComparator.modifiedDateOrder();
            }
        }

        String downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        mFileObserver = new RecursiveFileObserver(downloadFolder, RecursiveFileObserver.CHANGES_ONLY, this);
    }

    @Override
    public void onResume() {
        Logger.INSTANCE.LogD(TAG, "onResume");
        super.onResume();
        resumeFragment();
    }

    @Override
    public void onPause() {
        Logger.INSTANCE.LogD(TAG, "onPause");
        super.onPause();
        pauseFragment();
    }

    @Override
    public void onStop() {
        super.onStop();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_ALL_DOCUMENTS);
    }

    @Override
    public void onDestroyView() {
        Logger.INSTANCE.LogD(TAG, "onDestroyView");
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        Logger.INSTANCE.LogD(TAG, "onDestroy");

        // cleanup previous resource
        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.cleanupResources();
        }

        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mOutputFileUri != null) {
            outState.putParcelable("output_file_uri", mOutputFileUri);
        }
        outState.putBoolean("is_photo_from_camera", mIsCamera);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        MiscUtils.handleLowMemory(getContext(), mAdapter);
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_LOW_MEMORY, AnalyticsParam.lowMemoryParam(TAG));
        Logger.INSTANCE.LogE(TAG, "low memory");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_local_file_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Logger.INSTANCE.LogD(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);

        unbinder = ButterKnife.bind(this, view);

        mFabMenu.setClosedOnTouchOutside(true);

        if (!Utils.isTablet(getActivity()) & mFabMenu.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.LayoutParams clp = (CoordinatorLayout.LayoutParams) mFabMenu.getLayoutParams();
            clp.setBehavior(new MoveUpwardBehaviour());
        }

        FloatingActionButton createPDFButton = mFabMenu.findViewById(R.id.blank_PDF);
        createPDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                AddPageDialogFragment addPageDialogFragment = AddPageDialogFragment.newInstance();
                addPageDialogFragment.setOnCreateNewDocumentListener(new AddPageDialogFragment.OnCreateNewDocumentListener() {
                    @Override
                    public void onCreateNewDocument(PDFDoc doc, String title) {
                        saveCreatedDocument(doc, title);
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                            AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_BLANK_PDF, AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS));
                    }
                });
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    addPageDialogFragment.show(fragmentManager, "create_document_local_file");
                }
            }
        });

        FloatingActionButton imagePDFButton = mFabMenu.findViewById(R.id.image_PDF);
        imagePDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                mOutputFileUri = ViewerUtils.openImageIntent(LocalFileViewFragment.this);
            }
        });

        FloatingActionButton officePDFButton = mFabMenu.findViewById(R.id.office_PDF);
        officePDFButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentActivity activity = getActivity();
                FragmentManager fragmentManager = getFragmentManager();
                if (activity == null || fragmentManager == null) {
                    return;
                }

                mFabMenu.close(true);
                mAddDocPdfHelper = new AddDocPdfHelper(activity, fragmentManager, new AddDocPdfHelper.AddDocPDFHelperListener() {
                    @Override
                    public void onPDFReturned(String fileAbsolutePath, boolean external) {
                        Activity activity = getActivity();
                        if (activity == null) {
                            return;
                        }

                        if (fileAbsolutePath == null) {
                            Utils.showAlertDialog(activity, R.string.dialog_add_photo_document_filename_error_message, R.string.error);
                            return;
                        }

                        File file = new File(fileAbsolutePath);
                        if (external) {
                            Logger.INSTANCE.LogD(TAG, "external folder selected");
                            mCallbacks.onExternalFileSelected(fileAbsolutePath, "");
                        } else {
                            FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file);
                            addFileToList(fileInfo);
                            Logger.INSTANCE.LogD(TAG, "inside");
                            mCallbacks.onFileSelected(new File(fileAbsolutePath), "");
                        }
                        if (!external) {
                            CommonToast.showText(getContext(), getString(R.string.dialog_create_new_document_filename_success) + fileAbsolutePath);
                        }
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                            AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_DOCS, AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS));
                    }

                    @Override
                    public void onMultipleFilesSelected(int requestCode, ArrayList<FileInfo> fileInfoList) {
                        handleMultipleFilesSelected(fileInfoList, AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS);
                    }
                });
                mAddDocPdfHelper.pickFileAndCreate();
            }
        });

        mRecyclerView.initView(mSpanCount);

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);

        mItemSelectionHelper = new ItemSelectionHelper();
        mItemSelectionHelper.attachToRecyclerView(mRecyclerView);
        mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_MULTIPLE);

        mAdapter = createAdapter();
        mAdapter.setOnReloadListener(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemViewCacheSize(20);
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
                        mAdapter.getDerivedFilter().setFileTypeEnabledInFilterFromSettings(mRecyclerView.getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_LOCAL_FILES);
                        updateFileListFilter();
                    }
                });
        } catch (Exception ignored) {
        }

        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                final FileInfo fileInfo = mAdapter.getItem(position);
                if (fileInfo == null) {
                    return;
                }
                if (mAdapter.getItemViewType(position) == LocalFileAdapter.VIEW_TYPE_HEADER) {
                    // Header item - ignore click
                    return;
                }

                if (mActionMode == null) {
                    // We are not in CAB mode, we don't want to let the item checked
                    // in this case... We are just opening the document, not selecting it.
                    mItemSelectionHelper.setItemChecked(position, false);
                    onFileClicked(fileInfo);
                } else {
                    if (mFileInfoSelectedList.contains(fileInfo)) {
                        mFileInfoSelectedList.remove(fileInfo);
                        mItemSelectionHelper.setItemChecked(position, false);
                    } else {
                        mFileInfoSelectedList.add(fileInfo);
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
            public boolean onItemLongClick(RecyclerView parent, View view, int position, long id) {

                FileInfo fileInfo = mAdapter.getItem(position);
                if (fileInfo == null) {
                    return false;
                }

                if (mActionLock || mAdapter.getItemViewType(position) == LocalFileAdapter.VIEW_TYPE_HEADER) {
                    // locked or Header item - ignore click
                    return false;
                }

                closeSearch();
                if (mActionMode == null) {
                    mFileInfoSelectedList.add(fileInfo);
                    mItemSelectionHelper.setItemChecked(position, true);

                    mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(LocalFileViewFragment.this);
                    if (mActionMode != null) {
                        mActionMode.invalidate();
                    }
                } else {
                    if (mFileInfoSelectedList.contains(fileInfo)) {
                        mFileInfoSelectedList.remove(fileInfo);
                        mItemSelectionHelper.setItemChecked(position, false);
                    } else {
                        mFileInfoSelectedList.add(fileInfo);
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


        if (Utils.isLollipop()) {
            stickyHeader.setElevation(getResources().getDimensionPixelSize(R.dimen.card_elevation));
        }
        stickyHeader.setVisibility(View.VISIBLE);
        stickyHeader.disable();
        mRecyclerView.setStickyHeader(stickyHeader);
    }

    @Override
    public void onStart() {
        super.onStart();
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_ALL_DOCUMENTS);
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
    public void onAttach(Context context) {
        Logger.INSTANCE.LogV("LifeCycle", TAG + ".onAttach");
        super.onAttach(context);

        try {
            mFileUtilCallbacks = (FileUtilCallbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + e.getClass().toString());
        }

        try {
            mJumpNavigationCallbacks = (JumpNavigationCallbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + e.getClass().toString());
        }
    }

    @Override
    public void onDetach() {
        Logger.INSTANCE.LogV("LifeCycle", TAG + ".onDetach");
        super.onDetach();
        mFileUtilCallbacks = null;
        mJumpNavigationCallbacks = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            return;
        }

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_local_file_view, menu);
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

            EditText editText = searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
            if (editText != null) {
                // Disable long-click context menu
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
        if (context == null || menu == null) {
            return;
        }

        MenuItem menuItem;
        if (PdfViewCtrlSettingsManager.getSortMode(context).equals(PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_NAME)) {
            if (mSpanCount > 0) {
                mSortMode = FileInfoComparator.fileNameOrder();
            } else {
                mSortMode = FileInfoComparator.absolutePathOrder();
            }
            menuItem = menu.findItem(R.id.menu_file_sort_by_name);
        } else {
            if (mSpanCount > 0) {
                mSortMode = FileInfoComparator.modifiedDateOrderOnly();
            } else {
                mSortMode = FileInfoComparator.modifiedDateOrder();
            }
            menuItem = menu.findItem(R.id.menu_file_sort_by_date);
        }
        if (menuItem != null) {
            menuItem.setChecked(true);
        }

        // Set grid size radio buttons to correct value from settings
        int gridSize = PdfViewCtrlSettingsManager.getGridSize(getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_LOCAL_FILES);
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
        boolean reload;
        if (item.getItemId() == R.id.menu_action_search) {
            finishSearchView();
            handled = true;
        }
        if (item.getItemId() == R.id.menu_action_reload) {
            ThumbnailPathCacheManager.getInstance().cleanupResources(getContext());
            setReloadActionButtonState(true);
            if (DEBUG) Log.d(TAG, "onOptionsItemSelected");
            reloadFileInfoList(true);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_file_sort_by_name) {
            // Update sort mode setting
            if (mSpanCount > 0) {
                mSortMode = FileInfoComparator.fileNameOrder();
            } else {
                mSortMode = FileInfoComparator.absolutePathOrder();
            }
            PdfViewCtrlSettingsManager.updateSortMode(activity, PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_NAME);
            item.setChecked(true);
            sortFileInfoList();
            handled = true;
        }
        if (item.getItemId() == R.id.menu_file_sort_by_date) {
            // Update sort mode setting
            if (mSpanCount > 0) {
                mSortMode = FileInfoComparator.modifiedDateOrderOnly();
            } else {
                mSortMode = FileInfoComparator.modifiedDateOrder();
            }
            PdfViewCtrlSettingsManager.updateSortMode(activity, PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_DATE);
            item.setChecked(true);
            sortFileInfoList();
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_0) {
            item.setChecked(true);
            reload = mSpanCount != 0;
            handleColumnChanged(0, reload);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_1) {
            item.setChecked(true);
            reload = mSpanCount == 0;
            handleColumnChanged(1, reload);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_2) {
            item.setChecked(true);
            reload = mSpanCount == 0;
            handleColumnChanged(2, reload);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_3) {
            item.setChecked(true);
            reload = mSpanCount == 0;
            handleColumnChanged(3, reload);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_4) {
            item.setChecked(true);
            reload = mSpanCount == 0;
            handleColumnChanged(4, reload);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_5) {
            item.setChecked(true);
            reload = mSpanCount == 0;
            handleColumnChanged(5, reload);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_grid_count_6) {
            item.setChecked(true);
            reload = mSpanCount == 0;
            handleColumnChanged(6, reload);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_file_filter) {
            mFileTypeFilterListPopup = new FileTypeFilterPopupWindow(activity, getView(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_LOCAL_FILES);

            mFileTypeFilterListPopup.showAtLocation(getView(), Gravity.TOP | (Utils.isRtlLayout(getContext()) ? Gravity.LEFT : Gravity.RIGHT), 15, 75);
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
        mDocumentTitle = title;
        mMergeFileList = filesToMerge;
        mMergeTempFileList = filesToDelete;
        // Launch folder picker
        FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MERGE_FILE_LIST,
            Environment.getExternalStorageDirectory());
        dialogFragment.setLocalFolderListener(LocalFileViewFragment.this);
        dialogFragment.setExternalFolderListener(LocalFileViewFragment.this);
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
        }
    }

    private void handleColumnChanged(final int span, boolean reload) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (PdfViewCtrlSettingsManager.getSortMode(context).equals(PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_NAME)) {
            if (span > 0) {
                mSortMode = FileInfoComparator.fileNameOrder();
            } else {
                mSortMode = FileInfoComparator.absolutePathOrder();
            }
        } else {
            if (span > 0) {
                mSortMode = FileInfoComparator.modifiedDateOrderOnly();
            } else {
                mSortMode = FileInfoComparator.modifiedDateOrder();
            }
        }
        updateSpanCount(span);
        if (reload) {
            sortFileInfoList();
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

    private void setReloadActionButtonState(boolean reloading) {
        if (mOptionsMenu != null) {
            MenuItem reloadItem = mOptionsMenu.findItem(R.id.menu_action_reload);
            if (reloadItem != null) {
                if (reloading) {
                    reloadItem.setActionView(R.layout.actionbar_indeterminate_progress);
                } else {
                    reloadItem.setActionView(null);
                }
            }
        }
    }

    public void updateSpanCount(int count) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        Logger.INSTANCE.LogD(TAG, "count == " + count);
        if (mSpanCount != count) {
            PdfViewCtrlSettingsManager.updateGridSize(context, PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_LOCAL_FILES, count);
            if (mAdapter != null) {
                if (count == 0) {
                    stickyHeader.disable();
                    mAdapter.updateListHeaders();
                } else {
                    stickyHeader.disable();
                    mAdapter.removeListHeaders();
                }
            }
        }
        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests();
        }
        mSpanCount = count;
        updateGridMenuState(mOptionsMenu);
        mRecyclerView.update(count); // Enable sticky headers if count = 1 (list)
    }

    public void resetFileListFilter() {
        String filterText = getFilterText();
        if (!Utils.isNullOrEmpty(filterText)) {
            if (mAdapter != null) {
                if (mSpanCount == 0) {
                    mAdapter.removeListHeaders();
                }
                mAdapter.getFilter().filter("");
            }
        }
    }

    public String getFilterText() {
        if (!Utils.isNullOrEmpty(mFilterText)) {
            return mFilterText;
        }

        String filterText = "";
        if (mSearchMenuItem != null) {
            SearchView searchView = (SearchView) mSearchMenuItem.getActionView();
            filterText = searchView.getQuery().toString();
        }
        return filterText;
    }

    protected void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
            clearFileInfoSelectedList();
        }
        closeSearch();
    }

    protected void clearFileInfoSelectedList() {
        if (mItemSelectionHelper != null) {
            mItemSelectionHelper.clearChoices();
        }
        mFileInfoSelectedList.clear();
    }

    protected void hideFileInfoDrawer() {
        if (mFileInfoDrawer != null) {
            mFileInfoDrawer.hide();
            mFileInfoDrawer = null;
        }
        mSelectedFile = null;
    }

    protected void finishSearchView() {
        if (mSearchMenuItem != null && mSearchMenuItem.isActionViewExpanded()) {
            mSearchMenuItem.collapseActionView();
        }
        resetFileListFilter();
    }

    protected void updateFileListFilter() {
        if (mAdapter != null) {
            if (mSpanCount == 0) {
                mAdapter.removeListHeaders();
            }
            String constraint = getFilterText();
            if (constraint == null) {
                constraint = "";
            }
            mAdapter.getFilter().filter(constraint);
        }
    }

    private Comparator<FileInfo> getSortMode(
    ) {

        if (mSortMode != null) {
            return mSortMode;
        }

        if (mSpanCount > 0) {
            return FileInfoComparator.fileNameOrder();
        }

        return FileInfoComparator.absolutePathOrder();

    }

    protected int compare(FileInfo file1, FileInfo file2) {
        if (mSortMode != null) {
            return mSortMode.compare(file1, file2);
        }
        if (mSpanCount > 0) {
            return FileInfoComparator.fileNameOrder().compare(file1, file2);
        }
        return FileInfoComparator.absolutePathOrder().compare(file1, file2);
    }

    protected void reloadFileInfoList() {
        boolean emptyList;
        synchronized (mFileListLock) {
            emptyList = mFileInfoList.isEmpty();
        }

        if (!emptyList) {
            reloadFileInfoList(true);
        } else {
            // retrieve file list
            if (mRetrieveAllFilesCacheTask == null
                || (mRetrieveAllFilesCacheTask.getStatus() != AsyncTask.Status.RUNNING
                && mRetrieveAllFilesCacheTask.getStatus() != AsyncTask.Status.PENDING)) {
                mRetrieveAllFilesCacheTask = new RetrieveAllFilesCacheTask(
                    mFileInfoList, mFileListLock, mCacheLock, getSortMode(), this);
                mRetrieveAllFilesCacheTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }

        // retrieve file header list
        if (mAdapter != null && (mAdapter.getFileHeader() == null || mAdapter.getFileHeader().isEmpty())) {
            if (mRetrieveAllFilesHeaderCacheTask == null
                || (mRetrieveAllFilesHeaderCacheTask.getStatus() != AsyncTask.Status.RUNNING
                && mRetrieveAllFilesHeaderCacheTask.getStatus() != AsyncTask.Status.PENDING)) {
                mRetrieveAllFilesHeaderCacheTask = new RetrieveAllFilesHeaderCacheTask(mCacheLock, this);
                mRetrieveAllFilesHeaderCacheTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
    }

    private void sortFileInfoList(
    ) {

        if (mSortAllFilesTask != null) {
            mSortAllFilesTask.cancel(true);
        }
        mSortAllFilesTask = new SortAllFilesTask(mFileInfoList, mFileListLock, getSortMode(), this);
        mSortAllFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    @SuppressWarnings("SameParameterValue")
    protected void reloadFileInfoList(boolean removePreviewHandler) {

        Context context = getContext();
        if (context == null) {
            return;
        }

        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(removePreviewHandler);
        }

        boolean emptyList;
        synchronized (mFileListLock) {
            emptyList = mFileInfoList.isEmpty();
        }
        if (mPopulateAllFilesTask != null) {
            mPopulateAllFilesTask.cancel(true);
        }
        mPopulateAllFilesTask = new PopulateAllFilesTask(
            context, null, mFileInfoList, mFileListLock,
            getSortMode(), emptyList, this);
        mPopulateAllFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private void saveCache() {
        // save local document list
        new SaveFileListCacheTask(mFileInfoList, mFileListLock, mCacheLock,
            mAdapter == null ? null : mAdapter.getFileHeader()).execute();
    }

    private void saveCreatedDocument(PDFDoc doc, String title) {
        mCreatedDocumentTitle = title;
        mCreatedDoc = doc;
        // launch folder picker
        FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.SELECT_BLANK_DOC_FOLDER, Environment.getExternalStorageDirectory());
        dialogFragment.setLocalFolderListener(LocalFileViewFragment.this);
        dialogFragment.setExternalFolderListener(LocalFileViewFragment.this);
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialogFragment.show(fragmentManager, "create_document_folder_picker_dialog");
        }
        Logger.INSTANCE.LogD(TAG, "new blank folder");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (Activity.RESULT_OK == resultCode) {
            if (RequestCode.PICK_PHOTO_CAM == requestCode) {
                try {
                    Map imageIntent = ViewerUtils.readImageIntent(data, activity, mOutputFileUri);
                    if (!ViewerUtils.checkImageIntent(imageIntent)) {
                        Utils.handlePdfFromImageFailed(activity, imageIntent);
                        return;
                    }

                    mImageFilePath = ViewerUtils.getImageFilePath(imageIntent);
                    mIsCamera = ViewerUtils.isImageFromCamera(imageIntent);
                    mImageUri = ViewerUtils.getImageUri(imageIntent);

                    // try to get display name
                    mImageFileDisplayName = Utils.getDisplayNameFromImageUri(activity, mImageUri, mImageFilePath);
                    // cannot get a valid filename
                    if (Utils.isNullOrEmpty(mImageFileDisplayName)) {
                        Utils.handlePdfFromImageFailed(activity, imageIntent);
                        return;
                    }

                    // launch folder picker
                    FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(
                        RequestCode.SELECT_PHOTO_DOC_FOLDER, Environment.getExternalStorageDirectory());
                    dialogFragment.setLocalFolderListener(LocalFileViewFragment.this);
                    dialogFragment.setExternalFolderListener(LocalFileViewFragment.this);
                    dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
                    FragmentManager fragmentManager = getFragmentManager();
                    if (fragmentManager != null) {
                        dialogFragment.show(fragmentManager, "create_document_folder_picker_dialog");
                    }

                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                        AnalyticsParam.createNewParam(mIsCamera ? AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_CAMERA : AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_IMAGE,
                            AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS));
                } catch (FileNotFoundException e) {
                    CommonToast.showText(activity, getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } catch (Exception e) {
                    CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // prevent clearing filter text when the fragment is hidden
        if (mAdapter != null && Utils.isNullOrEmpty(mFilterText)) {
            if (mSpanCount == 0) {
                mAdapter.removeListHeaders();
            }
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.getFilter().filter(newText);
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

    private void deleteFileFromList(String path) {
        synchronized (mFileListLock) {
            for (int i = 0, count = mFileInfoList.size(); i < count; ++i) {
                FileInfo f = mFileInfoList.get(i);
                if (f.getAbsolutePath().equals(path)) {
                    mFileInfoList.remove(i);
                    break;
                }
            }
        }

        saveCache();
    }

    private void addFileToList(FileInfo fileInfo) {
        synchronized (mFileListLock) {
            // add the new file to the right position based on sort method
            for (int i = 0, count = mFileInfoList.size(); i < count; ++i) {
                FileInfo f = mFileInfoList.get(i);
                if (compare(f, fileInfo) > 0) {
                    mFileInfoList.add(i, fileInfo);
                    break;
                }
            }
        }

        saveCache();
    }

    @Override
    public void onFileRenamed(FileInfo oldFile, FileInfo newFile) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mSelectedFile == null || oldFile.getName().equals(mSelectedFile.getName())) {
            mSelectedFile = newFile; // update mSelectedFile
        }
        finishActionMode();
        hideFileInfoDrawer();

        ArrayList<FileInfo> deleteFile = new ArrayList<>();
        deleteFile.add(oldFile);
        ArrayList<FileInfo> addFile = new ArrayList<>();
        addFile.add(newFile);
        updateListFile(deleteFile, addFile);

        handleFileUpdated(oldFile, newFile);

        Utils.safeNotifyDataSetChanged(mAdapter);
        try {
            PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                oldFile.getAbsolutePath(), newFile.getAbsolutePath(), newFile.getFileName());
            // update user bookmarks
            BookmarkManager.updateUserBookmarksFilePath(activity, oldFile.getAbsolutePath(), newFile.getAbsolutePath());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        mFileEventLock = false;

    }

    @Override
    public void onFileDuplicated(File fileCopy) {
        finishActionMode();
        hideFileInfoDrawer();
        FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, fileCopy);
        ArrayList<FileInfo> arrayList = new ArrayList<>();
        arrayList.add(fileInfo);
        updateListFile(new ArrayList<FileInfo>(), arrayList);
        mFileEventLock = false;
    }

    @Override
    public void onFileDeleted(ArrayList<FileInfo> deletedFiles) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        finishActionMode();
        hideFileInfoDrawer();
        if (deletedFiles != null && deletedFiles.size() > 0) {
            // update user bookmarks
            for (FileInfo info : deletedFiles) {
                handleFileRemoved(info);
                BookmarkManager.removeUserBookmarks(activity, info.getAbsolutePath());
                if (mAdapter != null) {
                    mAdapter.evictFromMemoryCache(info.getAbsolutePath());
                }
            }
            handleFilesRemoved(deletedFiles);

            updateListFile(deletedFiles, new ArrayList<FileInfo>());
            mFileEventLock = false;
        }
    }

    @Override
    public void onFileMoved(Map<FileInfo, Boolean> filesMoved, File targetFolder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        finishActionMode();
        hideFileInfoDrawer();
        ArrayList<FileInfo> deletedFileInfo = new ArrayList<>();
        ArrayList<FileInfo> addedFileInfo = new ArrayList<>();
        for (Map.Entry<FileInfo, Boolean> entry : filesMoved.entrySet()) {
            // only update if the move operation was successful
            if (entry.getValue()) {
                FileInfo fileInfo = entry.getKey();
                File targetFile = new File(targetFolder, fileInfo.getName());
                FileInfo targetFileInfo = new FileInfo(fileInfo.getType(), targetFile);

                // update recent and favorite lists
                handleFileUpdated(fileInfo, targetFileInfo);
                Utils.safeNotifyDataSetChanged(mAdapter);

                try {
                    // Update tab info
                    PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                        fileInfo.getAbsolutePath(), targetFile.getAbsolutePath(), targetFileInfo.getFileName());
                    // update user bookmarks
                    BookmarkManager.updateUserBookmarksFilePath(activity, fileInfo.getAbsolutePath(), targetFile.getAbsolutePath());
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
                deletedFileInfo.add(fileInfo);
                addedFileInfo.add(targetFileInfo);
            }
        }
        updateListFile(deletedFileInfo, addedFileInfo);
        mFileEventLock = false;
    }

    @Override
    public void onFileMoved(Map<FileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        Logger.INSTANCE.LogD(TAG, "onExternalFileMoved: " + targetFolder.getAbsolutePath());
        finishActionMode();
        hideFileInfoDrawer();
        ArrayList<FileInfo> deletedFileInfo = new ArrayList<>();
        ArrayList<FileInfo> addedFileInfo = new ArrayList<>();
        for (Map.Entry<FileInfo, Boolean> entry : filesMoved.entrySet()) {
            // only update if the move operation was successful
            if (entry.getValue()) {
                FileInfo fileInfo = entry.getKey();
                String targetFilePath = ExternalFileInfo.appendPathComponent(targetFolder.getUri(), fileInfo.getName()).toString();
                FileInfo targetFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, targetFilePath,
                    fileInfo.getName(), false, 1);
                // Update recent and favorite lists
                handleFileUpdated(fileInfo, targetFileInfo);
                Utils.safeNotifyDataSetChanged(mAdapter);

                try {
                    // Update tab info
                    PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                        fileInfo.getAbsolutePath(), targetFileInfo.getAbsolutePath(), targetFileInfo.getFileName());
                    // update user bookmarks
                    BookmarkManager.updateUserBookmarksFilePath(activity, fileInfo.getAbsolutePath(), targetFileInfo.getAbsolutePath());
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
                deletedFileInfo.add(fileInfo);
                addedFileInfo.add(targetFileInfo);
            }
        }
        updateListFile(deletedFileInfo, addedFileInfo);
        mFileEventLock = false;
    }

    @Override
    public void onFileChanged(final String path, int event) {
        // this method is called from background thread

        synchronized (mFileChangeLock) {
            Logger.INSTANCE.LogD(TAG, "onFileChanged: " + path + "; isValid: " + FileManager.isValidFile(path) + ", mFileEventLock:" + mFileEventLock);
            if (!FileManager.isValidFile(path) || mFileEventLock) {
                return;
            }

            Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable;

            switch (event) {
                case FileObserver.MOVED_TO:
                case FileObserver.CREATE:
                    // run it in UI thread
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            File file = new File(path);
                            addFileToList(new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file));
                            updateFileListFilter();
                        }
                    };
                    handler.post(runnable);
                    break;
                case FileObserver.MOVED_FROM:
                case FileObserver.DELETE:
                    // run it in UI thread
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            deleteFileFromList(path);
                            updateFileListFilter();
                        }
                    };
                    handler.post(runnable);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onFolderCreated(FileInfo rootFolder, FileInfo newFolder) {
        mFileEventLock = false;
        reloadFileInfoList();
    }

    @Override
    public void onFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {
        Logger.INSTANCE.LogD(TAG, "onFileMerged");
        finishActionMode();
        hideFileInfoDrawer();
        if (newFile == null) {
            return;
        }

        mFileEventLock = false;
        if (mCallbacks != null) {
            // Open merged file in viewer
            if (newFile.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                addFileToList(newFile);
                mCallbacks.onFileSelected(newFile.getFile(), "");
            } else if (newFile.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                mCallbacks.onExternalFileSelected(newFile.getAbsolutePath(), "");
            }
        }
        MiscUtils.removeFiles(filesToDelete);
    }

    @Override
    public void onFileClicked(final FileInfo fileInfo) {
        Activity activity = getActivity();
        if (activity == null || fileInfo == null) {
            return;
        }
        final File file = fileInfo.getFile();
        if (file == null) {
            return;
        }

        if (mIsSearchMode) {
            hideSoftKeyboard();
        }
        if (Utils.isLollipop()
            && Utils.isSdCardFile(activity, file)
            && PdfViewCtrlSettingsManager.getShowOpenReadOnlySdCardFileWarning(activity)) {
            LayoutInflater inflater = LayoutInflater.from(activity);
            @SuppressLint("InflateParams") View customLayout = inflater.inflate(R.layout.alert_dialog_with_checkbox, null);
            String message = String.format(getString(R.string.dialog_files_go_to_sd_card_description),
                getString(R.string.app_name),
                getString(R.string.dialog_go_to_sd_card_description_more_info));
            final TextView dialogTextView = customLayout.findViewById(R.id.dialog_message);
            dialogTextView.setText(Html.fromHtml(message));
            dialogTextView.setMovementMethod(LinkMovementMethod.getInstance());
            final CheckBox dialogCheckBox = customLayout.findViewById(R.id.dialog_checkbox);
            dialogCheckBox.setChecked(true);

            AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setView(customLayout)
                .setPositiveButton(R.string.dialog_folder_go_to_sd_card_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Activity activity = getActivity();
                        if (activity == null) {
                            return;
                        }
                        boolean showAgain = !dialogCheckBox.isChecked();
                        PdfViewCtrlSettingsManager.updateShowOpenReadOnlySdCardFileWarning(activity, showAgain);
                        if (null != mJumpNavigationCallbacks) {
                            mJumpNavigationCallbacks.gotoExternalTab();
                        }
                    }
                }).setNegativeButton(R.string.document_read_only_warning_negative, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Activity activity = getActivity();
                        if (activity == null) {
                            return;
                        }
                        boolean showAgain = !dialogCheckBox.isChecked();
                        PdfViewCtrlSettingsManager.updateShowOpenReadOnlySdCardFileWarning(activity, showAgain);
                        if (file.exists()) {
                            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                                AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS));
                            mCallbacks.onFileSelected(file, "");
                        }
                    }
                })
                .setCancelable(true);

            final AlertDialog alertDialog = builder.create();
            alertDialog.show();

            // Make the textview clickable. Must be called after show()
            TextView textView = alertDialog.findViewById(android.R.id.message);
            if (textView != null) {
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }
            return;
        }

        if (file.exists()) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_ALL_DOCUMENTS));
            mCallbacks.onFileSelected(file, "");
        }
    }

    @Override
    public void onLocalFolderSelected(int requestCode, Object object, final File folder) {
        mFileEventLock = true;
        Logger.INSTANCE.LogD(TAG, "onLocalFolderSelected");
        if (requestCode == RequestCode.MOVE_FILE) {
            if (mSelectedFile != null) {
                FileManager.move(getActivity(), new ArrayList<>(Collections.singletonList(mSelectedFile)), folder, LocalFileViewFragment.this);
            }
        } else if (requestCode == RequestCode.MOVE_FILE_LIST) {
            FileManager.move(getActivity(), mFileInfoSelectedList, folder, LocalFileViewFragment.this);
        } else if (requestCode == RequestCode.SELECT_BLANK_DOC_FOLDER) {
            PDFDoc doc = null;
            String filePath = "";
            try {
                if (mCreatedDocumentTitle == null) {
                    CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    return;
                }
                boolean hasExtension = FilenameUtils.isExtension(mCreatedDocumentTitle, "pdf");
                if (!hasExtension) {
                    mCreatedDocumentTitle = mCreatedDocumentTitle + ".pdf";
                }
                File documentFile = new File(folder, mCreatedDocumentTitle);
                filePath = Utils.getFileNameNotInUse(documentFile.getAbsolutePath());
                if (Utils.isNullOrEmpty(filePath)) {
                    CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    return;
                }

                documentFile = new File(filePath);

                doc = mCreatedDoc;
                doc.save(filePath, SDFDoc.SaveMode.REMOVE_UNUSED, null);
                String toastMsg = getString(R.string.dialog_create_new_document_filename_success) + filePath;
                CommonToast.showText(getActivity(), toastMsg, Toast.LENGTH_LONG);
                addFileToList(new FileInfo(BaseFileInfo.FILE_TYPE_FILE, documentFile));

                if (mCallbacks != null) {
                    mCallbacks.onFileSelected(documentFile, "");
                }

                finishActionMode();
                Logger.INSTANCE.LogD(TAG, "finisheActionMode");
            } catch (Exception e) {
                CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e, filePath);
            } finally {
                Utils.closeQuietly(doc);
            }
            mFileEventLock = false;
        } else if (requestCode == RequestCode.SELECT_PHOTO_DOC_FOLDER) {
            if (Utils.isNullOrEmpty(mImageFileDisplayName)) {
                CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                return;
            }
            try {
                File documentFile = new File(folder, mImageFileDisplayName + ".pdf");
                documentFile = new File(Utils.getFileNameNotInUse(documentFile.getAbsolutePath()));
                String outputPath = ViewerUtils.imageIntentToPdf(getActivity(), mImageUri, mImageFilePath, documentFile.getAbsolutePath());
                if (outputPath != null) {
                    String toastMsg = getString(R.string.dialog_create_new_document_filename_success) + folder.getPath();
                    CommonToast.showText(getActivity(), toastMsg, Toast.LENGTH_LONG);

                    addFileToList(new FileInfo(BaseFileInfo.FILE_TYPE_FILE, documentFile));

                    if (mCallbacks != null) {
                        mCallbacks.onFileSelected(documentFile, "");
                    }
                }

                finishActionMode();

                mFileEventLock = false;
            } catch (FileNotFoundException e) {
                CommonToast.showText(getContext(), getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (Exception e) {
                CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (OutOfMemoryError oom) {
                MiscUtils.manageOOM(getContext());
                CommonToast.showText(getContext(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
            }

            // cleanup the image if it is from camera
            if (mIsCamera) {
                FileUtils.deleteQuietly(new File(mImageFilePath));
            }
            mFileEventLock = false;
        } else if (requestCode == RequestCode.MERGE_FILE_LIST) {
            boolean hasExtension = FilenameUtils.isExtension(mDocumentTitle, "pdf");
            if (!hasExtension) {
                mDocumentTitle = mDocumentTitle + ".pdf";
            }
            File documentFile = new File(folder, mDocumentTitle);
            String filePath = Utils.getFileNameNotInUse(documentFile.getAbsolutePath());
            if (Utils.isNullOrEmpty(filePath)) {
                CommonToast.showText(getActivity(), R.string.dialog_merge_error_message_general, Toast.LENGTH_SHORT);
                return;
            }
            documentFile = new File(filePath);
            FileInfo newFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, documentFile);
            performMerge(newFileInfo);
        }
    }

    @Override
    public void onExternalFolderSelected(int requestCode, Object object, final ExternalFileInfo folder) {
        Logger.INSTANCE.LogD(TAG, "onExternalFolderSelected");

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        mFileEventLock = true;
        if (requestCode == RequestCode.MOVE_FILE) {
            Logger.INSTANCE.LogD(TAG, "MOVE_FILE REQUEST");
            if (mSelectedFile != null) {
                FileManager.move(activity, new ArrayList<>(Collections.singletonList(mSelectedFile)), folder, LocalFileViewFragment.this);
            }
        } else if (requestCode == RequestCode.MOVE_FILE_LIST) {
            Logger.INSTANCE.LogD(TAG, "MOVE_FILE_LIST REQUEST");
            FileManager.move(activity, mFileInfoSelectedList, folder, LocalFileViewFragment.this);
        } else {
            if (requestCode == RequestCode.SELECT_BLANK_DOC_FOLDER) {
                PDFDoc doc = null;
                SecondaryFileFilter filter = null;
                try {
                    if (mCreatedDocumentTitle == null) {
                        CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                        return;
                    }
                    boolean hasExtension = FilenameUtils.isExtension(mCreatedDocumentTitle, "pdf");
                    if (!hasExtension) {
                        mCreatedDocumentTitle = mCreatedDocumentTitle + ".pdf";
                    }
                    String fileName = Utils.getFileNameNotInUse(folder, mCreatedDocumentTitle);
                    if (folder == null || Utils.isNullOrEmpty(fileName)) {
                        CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                        return;
                    }
                    ExternalFileInfo documentFile = folder.createFile("application/pdf", fileName);
                    if (documentFile == null) {
                        return;
                    }

                    doc = mCreatedDoc;

                    Uri uri = documentFile.getUri();
                    if (uri == null) {
                        return;
                    }
                    filter = new SecondaryFileFilter(activity, uri);
                    doc.save(filter, SDFDoc.SaveMode.REMOVE_UNUSED);

                    String toastMsg = getString(R.string.dialog_create_new_document_filename_success)
                        + documentFile.getDocumentPath();
                    CommonToast.showText(activity, toastMsg, Toast.LENGTH_LONG);

                    finishActionMode();

                    if (mCallbacks != null) {
                        mCallbacks.onExternalFileSelected(documentFile.getAbsolutePath(), "");
                    }
                } catch (Exception e) {
                    CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    Utils.closeQuietly(doc, filter);
                }
                mFileEventLock = false;
            } else if (requestCode == RequestCode.SELECT_PHOTO_DOC_FOLDER) {
                String pdfFilePath = Utils.getFileNameNotInUse(folder, mImageFileDisplayName + ".pdf");
                if (folder == null || Utils.isNullOrEmpty(pdfFilePath)) {
                    CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    return;
                }

                try {
                    ExternalFileInfo documentFile = folder.createFile("application/pdf", pdfFilePath);
                    if (documentFile == null) {
                        return;
                    }

                    String outputPath = ViewerUtils.imageIntentToPdf(activity, mImageUri, mImageFilePath, documentFile);
                    if (outputPath != null) {
                        String toastMsg = getString(R.string.dialog_create_new_document_filename_success)
                            + folder.getAbsolutePath();
                        CommonToast.showText(activity, toastMsg, Toast.LENGTH_LONG);
                        if (mCallbacks != null) {
                            mCallbacks.onExternalFileSelected(documentFile.getAbsolutePath(), "");
                        }
                    }

                    finishActionMode();
                } catch (FileNotFoundException e) {
                    CommonToast.showText(getContext(), getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } catch (Exception e) {
                    CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } catch (OutOfMemoryError oom) {
                    MiscUtils.manageOOM(getContext());
                    CommonToast.showText(getContext(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                }

                String pdfFilename = Utils.getFileNameNotInUse(mImageFileDisplayName + ".pdf");
                if (Utils.isNullOrEmpty(pdfFilename)) {
                    CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    return;
                }

                // cleanup the image if it is from camera
                if (mIsCamera) {
                    FileUtils.deleteQuietly(new File(mImageFilePath));
                }
                mFileEventLock = false;
            } else if (requestCode == RequestCode.MERGE_FILE_LIST) {
                boolean hasExtension = FilenameUtils.isExtension(mDocumentTitle, "pdf");
                if (!hasExtension) {
                    mDocumentTitle = mDocumentTitle + ".pdf";
                }
                String fileName = Utils.getFileNameNotInUse(folder, mDocumentTitle);
                if (folder == null || Utils.isNullOrEmpty(fileName)) {
                    CommonToast.showText(activity, R.string.dialog_merge_error_message_general, Toast.LENGTH_SHORT);
                    return;
                }
                final ExternalFileInfo file = folder.createFile("application/pdf", fileName);
                if (file == null) {
                    return;
                }
                FileInfo targetFile = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, file.getAbsolutePath(), file.getFileName(), false, 1);
                performMerge(targetFile);
            }
        }
    }

    @Override
    public void onPreLaunchViewer() {
        mViewerLaunching = true;
    }

    @Override
    public void onDataChanged() {
        if (isAdded()) {
            if (DEBUG) Log.d(TAG, "onDataChanged");
            reloadFileInfoList(true);
        } // otherwise it will be reloaded in resumeFragment
    }

    @Override
    public void onProcessNewIntent() {
        finishActionMode();
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
        if (mFabMenu != null && mFabMenu.isOpened()) {
            // Close fab menu
            mFabMenu.close(true);
            handled = true;
        } else if (mFileInfoDrawer != null) {
            // Hide file info drawer
            hideFileInfoDrawer();
            handled = true;
        } else if (mActionMode != null) {
            finishActionMode();
            handled = true;
        } else if (mIsSearchMode) {
            finishSearchView();
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
    public void onShowFileInfo(int position) {
        if (mFileUtilCallbacks != null) {
            mSelectedFile = mAdapter.getItem(position);
            mFileInfoDrawer = mFileUtilCallbacks.showFileInfoDrawer(mFileInfoDrawerCallback);
        }
    }

    @Override
    public void onFilterResultsPublished(int resultCode) {
        if (mAdapter != null) {
            if (mSpanCount == 0 && mAdapter.getItemCount() > 0) {
                if (stickyHeader != null) {
                    stickyHeader.disable();
                }
                mAdapter.updateListHeaders();
            }
            Logger.INSTANCE.LogD(TAG, "onFilterResultsPublished: " + mAdapter.getItemCount());
            if (mEmptyTextView != null) {
                if (mAdapter.getItemCount() > 0) {
                    mAdapter.cancelAllThumbRequests();
                    mEmptyTextView.setVisibility(View.GONE);
                } else if (mIsFinishedFiltering && !mAdapter.isAddingHeaders()) {
                    switch (resultCode) {
                        case FileListFilter.FILTER_RESULT_EMPTY_ORIGINAL_LIST:
                            mEmptyTextView.setText(R.string.textview_empty_file_list);
                            break;
                        case FileListFilter.FILTER_RESULT_NO_STRING_MATCH:
                            mEmptyTextView.setText(R.string.textview_empty_because_no_string_match);
                            break;
                        case FileListFilter.FILTER_RESULT_NO_ITEMS_OF_SELECTED_FILE_TYPES:
                            mEmptyTextView.setText(R.string.textview_empty_because_no_files_of_selected_type);
                            break;
                        case FileListFilter.FILTER_RESULT_FAILURE:
                            saveCache();
                            mEmptyTextView.setText(R.string.textview_empty_failed);
                            break;
                        default:
                            mEmptyTextView.setText(R.string.textview_empty_file_list);
                            break;
                    }
                    mEmptyTextView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onReload() {
        Logger.INSTANCE.LogD(TAG, "reload everything");
        if (mPopulateAllFilesTask != null &&
            (mPopulateAllFilesTask.getStatus() == AsyncTask.Status.RUNNING ||
                mPopulateAllFilesTask.getStatus() == AsyncTask.Status.PENDING)) {
            return;
        }
        if (DEBUG) Log.d(TAG, "onReload");
        reloadFileInfoList(true);
    }

    private void resumeFragment() {

        reloadFileInfoList();
        mViewerLaunching = false;

        if (mLocalFileViewFragmentListener != null) {
            mLocalFileViewFragmentListener.onLocalFileShown();
        }

        if (mFileObserver != null) {
            mFileObserver.startWatching();
        }

        updateSpanCount(mSpanCount);
    }

    private void pauseFragment() {
        mFilterText = getFilterText();

        if (mIsSearchMode && !mViewerLaunching) {
            finishSearchView();
        }

        // Cancel task if not already finished
        for (UpdateAllFilesTask updateAllFilesTask : mUpdateAllFilesTasks) {
            updateAllFilesTask.cancel(true);
        }
        if (mSortAllFilesTask != null) {
            mSortAllFilesTask.cancel(true);
        }
        if (mPopulateAllFilesTask != null) {
            mPopulateAllFilesTask.cancel(true);
        }
        if (mRetrieveAllFilesCacheTask != null) {
            mRetrieveAllFilesCacheTask.cancel(true);
        }
        if (mRetrieveAllFilesHeaderCacheTask != null) {
            mRetrieveAllFilesHeaderCacheTask.cancel(true);
        }

        if (mProgressBarView != null && mProgressBarView.getVisibility() == View.VISIBLE) {
            mProgressBarView.setVisibility(View.GONE);
        }

        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.cleanupResources();
        }
        saveCache();
        finishActionMode();

        if (mLocalFileViewFragmentListener != null) {
            mLocalFileViewFragmentListener.onLocalFileHidden();
        }
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }
    }

    public void setLocalFileViewFragmentListener(LocalFileViewFragmentListener listener) {
        mLocalFileViewFragmentListener = listener;
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

    private void updateListFile
        (ArrayList<FileInfo> deleteFiles,
         ArrayList<FileInfo> addFiles
        ) {

        // we may have several tasks for updating file info list
        UpdateAllFilesTask updateAllFilesTask = new UpdateAllFilesTask(
            mFileInfoList, mFileListLock, mCacheLock,
            mUpdateTaskLock, getSortMode(),
            deleteFiles, addFiles, this);
        updateAllFilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        mUpdateAllFilesTasks.add(updateAllFilesTask);

    }

    @Override
    public void onRetrieveFileInfoListTaskFinished(
    ) {

        Logger.INSTANCE.LogD(TAG, "retrieve file info list finished");

        boolean emptyList;
        synchronized (mFileListLock) {
            emptyList = mFileInfoList.isEmpty();
        }
        if (!emptyList) { // cache has file info list
            updateFileListFilter();
            if (mRecyclerView != null) {
                mRecyclerView.setVerticalScrollBarEnabled(true);
            }
        }

        // now populate file info list from device:
        reloadFileInfoList(true);

    }

    @Override
    public void onRetrieveAllFilesHeaderCacheTaskFinished(
        HashMap<String, FileHeader> result
    ) {

        Logger.INSTANCE.LogD(TAG, "retrieve file header list finished");

        if (result != null && !result.isEmpty() && mAdapter != null) {
            mAdapter.setFileHeader(result);
        }

    }

    @Override
    public void onPopulateAllFilesTaskStarted(
    ) {

        Logger.INSTANCE.LogD(TAG, "start populate file info list");

        if (mEmptyTextView != null) {
            synchronized (mFileListLock) {
                if (mFileInfoList.isEmpty()) {
                    mEmptyTextView.setText(R.string.loading_files_wait);
                    mEmptyTextView.setVisibility(View.VISIBLE);
                } else {
                    mEmptyTextView.setVisibility(View.GONE);
                }
            }
        }

        if (mProgressBarView != null) {
            mProgressBarView.setVisibility(View.VISIBLE);
        }

        mIsFinishedFiltering = false;
        mActionLock = true;

        setReloadActionButtonState(true);

    }

    @Override
    public void onPopulateAllFilesTaskProgressUpdated(
    ) {

        Logger.INSTANCE.LogD(TAG, "populate file info list updated");

        updateFileListFilter();

    }

    @Override
    public void onPopulateAllFilesTaskFinished(
    ) {

        Logger.INSTANCE.LogD(TAG, "populate file info list finished");

        // update list
        if (mRecyclerView != null) {
            Snackbar.make(mRecyclerView, "File List Updated", Snackbar.LENGTH_LONG).show();
            mRecyclerView.setVerticalScrollBarEnabled(true);
        }

        if (mProgressBarView != null) {
            mProgressBarView.setVisibility(View.GONE);
        }

        mIsFinishedFiltering = true;
        updateFileListFilter();

        setReloadActionButtonState(false);
        mActionLock = false;

    }

    @Override
    public void onUpdateAllFilesTaskStarted(
    ) {

        if (mProgressBarView != null) {
            mProgressBarView.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onUpdateAllFilesTaskFinished(
        UpdateAllFilesTask task,
        boolean isChanged
    ) {

        mUpdateAllFilesTasks.remove(task);
        if (isChanged) {
            updateFileListFilter();
        }
        if (mProgressBarView != null) {
            mProgressBarView.setVisibility(View.GONE);
        }

    }

    @Override
    public void onSortAllFilesTaskStarted() {

        mIsFinishedFiltering = false;
        if (mProgressBarView != null) {
            mProgressBarView.setVisibility(View.GONE);
        }

    }

    @Override
    public void onSortAllFilesTaskFinished(
    ) {

        mIsFinishedFiltering = true;
        updateFileListFilter();
        if (mProgressBarView != null) {
            mProgressBarView.setVisibility(View.GONE);
        }

    }

}
