//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
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
import android.widget.Button;
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
import com.pdftron.demo.asynctask.PopulateFolderTask;
import com.pdftron.demo.dialog.FilePickerDialogFragment;
import com.pdftron.demo.dialog.ImportWebpageUrlSelectorDialogFragment;
import com.pdftron.demo.dialog.MergeDialogFragment;
import com.pdftron.demo.navigation.adapter.BaseFileAdapter;
import com.pdftron.demo.navigation.adapter.LocalFileAdapter;
import com.pdftron.demo.navigation.callbacks.FileManagementListener;
import com.pdftron.demo.navigation.callbacks.FileUtilCallbacks;
import com.pdftron.demo.navigation.callbacks.JumpNavigationCallbacks;
import com.pdftron.demo.navigation.callbacks.MainActivityListener;
import com.pdftron.demo.utils.AddDocPdfHelper;
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
import com.pdftron.pdf.utils.HTML2PDF;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class LocalFolderViewFragment extends FileBrowserViewFragment implements
    SearchView.OnQueryTextListener,
    FileManagementListener,
    FilePickerDialogFragment.LocalFolderListener,
    FilePickerDialogFragment.ExternalFolderListener,
    BaseFileAdapter.AdapterListener,
    MergeDialogFragment.MergeDialogFragmentListener,
    ActionMode.Callback,
    MainActivityListener,
    PopulateFolderTask.Callback {

    private static final String TAG = LocalFolderViewFragment.class.getName();
    private static final Boolean DEBUG = false;
    private static final int CACHED_SD_CARD_FOLDER_LIMIT = 25;

    @BindView(R2.id.recycler_view)
    protected SimpleRecyclerView mRecyclerView;
    @BindView(R2.id.empty_text_view)
    protected TextView mEmptyTextView;
    @BindView(R2.id.progress_bar_view)
    protected ProgressBar mProgressBarView;
    @BindView(R2.id.breadcrumb_bar_scroll_view)
    protected HorizontalScrollView mBreadcrumbBarScrollView;
    @BindView(R2.id.breadcrumb_bar_layout)
    protected LinearLayout mBreadcrumbBarLayout;
    @BindView(R2.id.fab_menu)
    protected FloatingActionMenu mFabMenu;
    @BindView(R2.id.go_to_sd_card_view)
    protected ViewGroup mGoToSdCardView;
    @BindView(R2.id.buttonGoToSdCard)
    protected Button mGoToSdCardButton;
    @BindView(R2.id.go_to_sd_card_view_text)
    protected TextView mGoToSdCardDescription;

    private Unbinder unbinder;

    protected final LruCache<String, Boolean> mSdCardFolderCache = new LruCache<>(CACHED_SD_CARD_FOLDER_LIMIT);

    protected ArrayList<FileInfo> mFileInfoList = new ArrayList<>();
    protected ArrayList<FileInfo> mFileInfoSelectedList = new ArrayList<>();
    protected FileInfo mSelectedFile;
    protected File mCurrentFolder;

    private FileUtilCallbacks mFileUtilCallbacks;
    private JumpNavigationCallbacks mJumpNavigationCallbacks;

    protected LocalFileAdapter mAdapter;
    protected ItemSelectionHelper mItemSelectionHelper;
    protected int mSpanCount;

    private PopulateFolderTask mPopulateFolderTask;
    private Comparator<FileInfo> mSortMode;
    private boolean mIsSearchMode;
    private boolean mIsFullSearchDone;
    private boolean mViewerLaunching;
    private Menu mOptionsMenu;
    private MenuItem mSearchMenuItem;
    private FileInfoDrawer mFileInfoDrawer;
    private Snackbar mSnackBar;
    private Uri mOutputFileUri;
    private int mCrumbColorActive;
    private int mCrumbColorInactive;
    private FileTypeFilterPopupWindow mFileTypeFilterListPopup;
    private boolean mInSDCardFolder;
    private RecursiveFileObserver mFileObserver;
    private String mFilterText = "";
    private boolean mDataChanged;
    private TextView mNotSupportedTextView;

    private LocalFolderViewFragmentListener mLocalFolderViewFragmentListener;

    public interface LocalFolderViewFragmentListener {

        void onLocalFolderShown();

        void onLocalFolderHidden();
    }

    public static LocalFolderViewFragment newInstance() {
        return new LocalFolderViewFragment();
    }

    private MenuItem itemDuplicate;
    private MenuItem itemEdit;
    private MenuItem itemDelete;
    private MenuItem itemMove;
    private MenuItem itemMerge;
    private MenuItem itemFavorite;
    private MenuItem itemShare;

    // Called each time the action mode is shown. Always called after
    // onCreateActionMode, but may be called multiple times if the mode is
    // invalidated.
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }
        if (mFileInfoSelectedList.isEmpty()) {
            return false;
        }

        if (mFileInfoSelectedList.size() > 1) {
            // Multiple selection of files and/or folders
            itemDelete.setVisible(true);
            itemDuplicate.setVisible(false);
            itemEdit.setVisible(false);
            itemMerge.setVisible(true);
            itemFavorite.setVisible(false);
            itemShare.setVisible(true);

            // If only files are selected, allow move action and merge
            itemMove.setVisible(true);
            for (FileInfo file : mFileInfoSelectedList) {
                if (file.getType() != BaseFileInfo.FILE_TYPE_FILE) {
                    itemMove.setVisible(false);
                    itemMerge.setVisible(false);
                    itemShare.setVisible(false);
                    break;
                }
            }
        } else {
            switch (mFileInfoSelectedList.get(0).getType()) {
                case BaseFileInfo.FILE_TYPE_FOLDER:
                    itemEdit.setVisible(true);
                    itemDuplicate.setVisible(false);
                    itemMove.setVisible(false);
                    itemDelete.setVisible(true);
                    itemMerge.setVisible(false);
                    itemFavorite.setVisible(true);
                    itemShare.setVisible(false);
                    break;
                case BaseFileInfo.FILE_TYPE_FILE:
                    itemEdit.setVisible(true);
                    itemDuplicate.setVisible(true);
                    itemMove.setVisible(true);
                    itemDelete.setVisible(true);
                    itemMerge.setVisible(true);
                    itemFavorite.setVisible(true);
                    itemShare.setVisible(true);
                    break;
                default:
                    itemEdit.setVisible(false);
                    itemDuplicate.setVisible(false);
                    itemMove.setVisible(false);
                    itemDelete.setVisible(false);
                    itemMerge.setVisible(false);
                    itemFavorite.setVisible(false);
                    itemShare.setVisible(false);
                    break;
            }
            if (canAddToFavorite(mFileInfoSelectedList.get(0))) {
                itemFavorite.setTitle(activity.getString(R.string.action_add_to_favorites));
            } else {
                itemFavorite.setTitle(activity.getString(R.string.action_remove_from_favorites));
            }
        }
        mode.setTitle(Utils.getLocaleDigits(Integer.toString(mFileInfoSelectedList.size())));
        // Ensure items are always shown
        itemEdit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemDuplicate.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemMove.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        itemDelete.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    // Called when the user exits the action mode.
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        mActionMode = null;
        clearFileInfoSelectedList();
    }

    // Called when the action mode is created or startActionMode() was called.
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (super.onCreateActionMode(mode, menu)) {
            return true;
        }

        mode.getMenuInflater().inflate(R.menu.cab_fragment_file_operations, menu);

        itemEdit = menu.findItem(R.id.cab_file_rename);
        itemDuplicate = menu.findItem(R.id.cab_file_copy);
        itemMove = menu.findItem(R.id.cab_file_move);
        itemDelete = menu.findItem(R.id.cab_file_delete);
        itemMerge = menu.findItem(R.id.cab_file_merge);
        itemFavorite = menu.findItem(R.id.cab_file_favorite);
        itemShare = menu.findItem(R.id.cab_file_share);
        return true;
    }

    // Called when the user selects a contextual menu item.
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        if (mFileInfoSelectedList.isEmpty()) {
            return false;
        }

        if (item.getItemId() == R.id.cab_file_rename) {
            if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(getContext(), mJumpNavigationCallbacks, getString(R.string.controls_misc_rename))) {
                finishActionMode();
                return true;
            }
            FileManager.rename(activity, mFileInfoSelectedList.get(0).getFile(), LocalFolderViewFragment.this);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_copy) {
            if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(getContext(), mJumpNavigationCallbacks, getString(R.string.controls_misc_duplicate))) {
                finishActionMode();
                return true;
            }
            FileManager.duplicate(activity, mFileInfoSelectedList.get(0).getFile(), LocalFolderViewFragment.this);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_move) {
            if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(getContext(), mJumpNavigationCallbacks, getString(R.string.action_file_move))) {
                finishActionMode();
                return true;
            }
            // Creates the dialog in full screen mode
            FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MOVE_FILE_LIST, mCurrentFolder);
            dialogFragment.setLocalFolderListener(LocalFolderViewFragment.this);
            dialogFragment.setExternalFolderListener(LocalFolderViewFragment.this);
            dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
            }
            return true;
        }
        if (item.getItemId() == R.id.cab_file_delete) {
            if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(getContext(), mJumpNavigationCallbacks, getString(R.string.delete))) {
                finishActionMode();
                return true;
            }
            FileManager.delete(activity, mFileInfoSelectedList, LocalFolderViewFragment.this);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_merge) {
            if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(getContext(), mJumpNavigationCallbacks, getString(R.string.merge))) {
                finishActionMode();
                return true;
            }
            handleMerge(mFileInfoSelectedList);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_favorite) {
            handleAddToFavorite(mFileInfoSelectedList.get(0));

            finishActionMode();
            // Update favorite file indicators
            Utils.safeNotifyDataSetChanged(mAdapter);
            return true;
        }
        if (item.getItemId() == R.id.cab_file_share) {
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
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_FILEBROWSER, "Item share clicked", AnalyticsHandlerAdapter.LABEL_FOLDERS);
            return true;
        }

        return false;
    }

    protected void handleMerge(ArrayList<FileInfo> files) {
        // Create and show file merge dialog-fragment
        MergeDialogFragment mergeDialog = getMergeDialogFragment(files, AnalyticsHandlerAdapter.SCREEN_FOLDERS);
        mergeDialog.initParams(LocalFolderViewFragment.this);
        mergeDialog.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            mergeDialog.show(fragmentManager, "merge_dialog");
        }
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

    protected void handleFilesRemoved(ArrayList<FileInfo> files) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        getRecentFilesManager().removeFiles(activity, files);
        getFavoriteFilesManager().removeFiles(activity, files);
    }

    private FileInfoDrawer.Callback mFileInfoDrawerCallback = new FileInfoDrawer.Callback() {

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

            if (mSelectedFile == null) {
                return;
            }

            if (mSelectedFile.getType() == BaseFileInfo.FILE_TYPE_FOLDER) {
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE); // Adjust scale type for folder
                imageView.setImageResource(R.drawable.ic_folder_black_24dp);
                int folderColorRes = Utils.getResourceColor(activity, getResources().getString(R.string.folder_color));
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
            if (activity == null || menu == null || mSelectedFile == null) {
                return true;
            }

            switch (mSelectedFile.getType()) {
                case BaseFileInfo.FILE_TYPE_FOLDER:
                    itemEdit.setVisible(true);
                    itemDuplicate.setVisible(false);
                    itemMove.setVisible(false);
                    itemDelete.setVisible(true);
                    itemMerge.setVisible(false);
                    itemFavorite.setVisible(true);
                    itemShare.setVisible(false);
                    break;
                case BaseFileInfo.FILE_TYPE_FILE:
                    itemEdit.setVisible(true);
                    itemDuplicate.setVisible(true);
                    itemMove.setVisible(true);
                    itemDelete.setVisible(true);
                    itemMerge.setVisible(true);
                    itemFavorite.setVisible(true);
                    itemShare.setVisible(true);
                    break;
                default:
                    itemEdit.setVisible(false);
                    itemDuplicate.setVisible(false);
                    itemMove.setVisible(false);
                    itemDelete.setVisible(false);
                    itemMerge.setVisible(false);
                    itemFavorite.setVisible(false);
                    itemShare.setVisible(false);
                    break;
            }

            if (canAddToFavorite(mSelectedFile)) {
                itemFavorite.setTitle(activity.getString(R.string.action_add_to_favorites));
                itemFavorite.setTitleCondensed(activity.getString(R.string.action_favorite));
                itemFavorite.setIcon(R.drawable.ic_star_outline_grey600_24dp);
            } else {
                itemFavorite.setTitle(activity.getString(R.string.action_remove_from_favorites));
                itemFavorite.setTitleCondensed(activity.getString(R.string.action_unfavorite));
                itemFavorite.setIcon(R.drawable.ic_star_white_24dp);
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
                if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.controls_misc_rename))) {
                    hideFileInfoDrawer();
                    return true;
                }
                FileManager.rename(activity, mSelectedFile.getFile(), LocalFolderViewFragment.this);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_copy) {
                if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.controls_misc_duplicate))) {
                    hideFileInfoDrawer();
                    return true;
                }
                FileManager.duplicate(activity, mSelectedFile.getFile(), LocalFolderViewFragment.this);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_move) {
                if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.action_file_move))) {
                    hideFileInfoDrawer();
                    return true;
                }
                FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MOVE_FILE,
                    Environment.getExternalStorageDirectory());
                dialogFragment.setLocalFolderListener(LocalFolderViewFragment.this);
                dialogFragment.setExternalFolderListener(LocalFolderViewFragment.this);
                dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
                }
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_delete) {
                if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.delete))) {
                    hideFileInfoDrawer();
                    return true;
                }
                FileManager.delete(activity, new ArrayList<>(Collections.singletonList(mSelectedFile)), LocalFolderViewFragment.this);
                return true;
            }
            if (menuItem.getItemId() == R.id.cab_file_merge) {
                if (mInSDCardFolder && MiscUtils.showSDCardActionErrorDialog(activity, mJumpNavigationCallbacks, activity.getString(R.string.merge))) {
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
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_FILEBROWSER, "Item share clicked", AnalyticsHandlerAdapter.LABEL_FOLDERS);
                return true;
            }
            return false;
        }

        @Override
        public void onThumbnailClicked(FileInfoDrawer drawer) {
            drawer.invalidate();
            if (mSelectedFile != null && mSelectedFile.getFile().exists()) {
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

            if (mSelectedFile.getType() != BaseFileInfo.FILE_TYPE_FOLDER) {
                try {
                    PDFDoc doc = new PDFDoc(mSelectedFile.getAbsolutePath());
                    doc.initSecurityHandler();
                    loadDocInfo(doc);
                } catch (PDFNetException e) {
                    mAuthor = null;
                    mTitle = null;
                    mProducer = null;
                    mCreator = null;
                    mPageCount = -1;
                }
            } else {
                mTitle = null;
                mAuthor = null;
                mProducer = null;
                mCreator = null;
                mPageCount = -1;
            }

            switch (mSelectedFile.getType()) {
                case BaseFileInfo.FILE_TYPE_FILE:
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
                    break;
                case BaseFileInfo.FILE_TYPE_FOLDER:
                    if (textBodyBuilder.length() > 0) {
                        textBodyBuilder.append("<br>");
                    }
                    // Directory
                    textBodyBuilder.append(res.getString(R.string.file_info_document_path, mSelectedFile.getAbsolutePath()));
                    textBodyBuilder.append("<br>");
                    // Size info: x files, y folders
                    int[] fileFolderCount = mSelectedFile.getFileCount();
                    String sizeInfo = res.getString(R.string.dialog_folder_info_size, fileFolderCount[0], fileFolderCount[1]);
                    textBodyBuilder.append(res.getString(R.string.file_info_document_folder_contains, sizeInfo));
                    textBodyBuilder.append("<br>");
                    // Date modified
                    textBodyBuilder.append(res.getString(R.string.file_info_document_date_modified, mSelectedFile.getModifiedDate()));
                    break;
                default:
                    break;
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
                    mProducer = docInfo.getProducer();
                    mCreator = docInfo.getCreator();
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCurrentFolder != null) {
            outState.putSerializable("current_folder", mCurrentFolder);
        }
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
        updateSpanCount(mSpanCount);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        FileManager.initCache(getContext());
        // When we use setRetainInstance, the Bundle (savedInstanceState) will always be null.
        setRetainInstance(true);

        // This Fragment wants to be able to have action bar items.
        setHasOptionsMenu(true);

        if (null != savedInstanceState) {
            mCurrentFolder = (File) savedInstanceState.getSerializable("current_folder");
            mOutputFileUri = savedInstanceState.getParcelable("output_file_uri");
        } else {
            mCurrentFolder = Environment.getExternalStorageDirectory();
        }
        updateFileObserver();

        if (PdfViewCtrlSettingsManager.getSortMode(activity).equals(PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_NAME)) {
            mSortMode = FileInfoComparator.folderPathOrder();
        } else {
            mSortMode = FileInfoComparator.folderDateOrder();
        }

        mCrumbColorActive = getResources().getColor(android.R.color.white);
        mCrumbColorInactive = getResources().getColor(R.color.breadcrumb_color_inactive);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Creates our custom view for the folder list.
        return inflater.inflate(R.layout.fragment_local_folder_view, container, false);
    }

    @SuppressLint("StringFormatInvalid")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        unbinder = ButterKnife.bind(this, view);

        mBreadcrumbBarScrollView.setVerticalScrollBarEnabled(false);
        mBreadcrumbBarScrollView.setHorizontalScrollBarEnabled(false);

        mBreadcrumbBarLayout.removeAllViews();

        mFabMenu.setClosedOnTouchOutside(true);

        FloatingActionButton createFolderButton = mFabMenu.findViewById(R.id.add_folder);
        createFolderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = getActivity();
                if (activity == null) {
                    return;
                }
                mFabMenu.close(true);
                FileManager.createFolder(getActivity(), mCurrentFolder, LocalFolderViewFragment.this);
            }
        });

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
                            AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_BLANK_PDF, AnalyticsHandlerAdapter.SCREEN_FOLDERS));
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
                mOutputFileUri = ViewerUtils.openImageIntent(LocalFolderViewFragment.this);
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
                        if (external || getActivity() == null || getActivity().isFinishing())
                            return;
                        if (fileAbsolutePath == null) {
                            Utils.showAlertDialog(getActivity(), R.string.dialog_add_photo_document_filename_error_message, R.string.error);
                            return;
                        }

//                            reloadFileInfoList();
                        mCallbacks.onFileSelected(new File(fileAbsolutePath), "");
                        CommonToast.showText(getContext(), getString(R.string.dialog_create_new_document_filename_success) + fileAbsolutePath);
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                            AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_DOCS, AnalyticsHandlerAdapter.SCREEN_FOLDERS));
                    }

                    @Override
                    public void onMultipleFilesSelected(int requestCode, ArrayList<FileInfo> fileInfoList) {
                        handleMultipleFilesSelected(fileInfoList, AnalyticsHandlerAdapter.SCREEN_FOLDERS);
                    }
                });
                mAddDocPdfHelper.pickFileAndCreate(mCurrentFolder);
            }
        });

        if (Utils.isLollipop()) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View btnView = inflater.inflate(R.layout.fab_btn_web_pdf, null);
            FloatingActionButton webpagePDFButton = btnView.findViewById(R.id.webpage_PDF);
            webpagePDFButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFabMenu.close(true);
                    handleWebpageToPDF();
                }
            });
            mFabMenu.addMenuButton(webpagePDFButton);
        }

        mSpanCount = PdfViewCtrlSettingsManager.getGridSize(getActivity(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FOLDER_FILES);
        mRecyclerView.initView(mSpanCount);

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);

        mItemSelectionHelper = new ItemSelectionHelper();
        mItemSelectionHelper.attachToRecyclerView(mRecyclerView);
        mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_MULTIPLE);

        mAdapter = createAdapter();
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
                        mAdapter.getDerivedFilter().setFileTypeEnabledInFilterFromSettings(mRecyclerView.getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FOLDER_FILES);
                        updateFileListFilter();
                    }
                });
        } catch (Exception ignored) {
        }

        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                FileInfo fileInfo = mAdapter.getItem(position);
                if (fileInfo == null) {
                    return;
                }

                if (mActionMode == null) {
                    // We are not in CAB mode, we don't want to let the item checked
                    // in this case... We are just opening the document, not selecting it.
                    mItemSelectionHelper.setItemChecked(position, false);

                    if (fileInfo.getFile().exists()) {
                        onFileClicked(fileInfo);
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
            }
        });

        itemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView parent, View view, int position, long id) {
                Activity activity = getActivity();
                if (activity == null) {
                    return false;
                }

                FileInfo fileInfo = mAdapter.getItem(position);
                if (fileInfo == null) {
                    return false;
                }

                closeSearch();
                if (mActionMode == null) {
                    mFileInfoSelectedList.add(fileInfo);
                    mItemSelectionHelper.setItemChecked(position, true);

                    if (activity instanceof AppCompatActivity) {
                        mActionMode = ((AppCompatActivity) activity).startSupportActionMode(LocalFolderViewFragment.this);
                    }
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


//        mScroller.setRecyclerView(mRecyclerView);

        mGoToSdCardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // go to external tab
                if (null != mJumpNavigationCallbacks) {
                    finishActionMode();
                    mJumpNavigationCallbacks.gotoExternalTab();
                }
            }
        });
        String message = String.format(getString(R.string.dialog_folder_go_to_sd_card_description),
            getString(R.string.dialog_go_to_sd_card_description_more_info));
        mGoToSdCardDescription.setText(Html.fromHtml(message));
        mGoToSdCardDescription.setMovementMethod(LinkMovementMethod.getInstance());

        mNotSupportedTextView = view.findViewById(R.id.num_no_supported_files);
    }

    public void handleWebpageToPDF() {
        ImportWebpageUrlSelectorDialogFragment importWebpageUrlSelectorDialogFragment = ImportWebpageUrlSelectorDialogFragment.newInstance();
        importWebpageUrlSelectorDialogFragment.setOnLinkSelectedListener(
            new ImportWebpageUrlSelectorDialogFragment.OnLinkSelectedListener() {
                @Override
                public void linkSelected(String link) {
                    Activity activity = getActivity();
                    if (activity == null || activity.isFinishing()) {
                        return;
                    }

                    HTML2PDF.fromUrl(activity, link, mCurrentFolder, new HTML2PDF.HTML2PDFListener() {
                        @Override
                        public void onConversionFinished(String pdfOutput) {
                            if (mCallbacks != null) {
                                mCallbacks.onFileSelected(new File(pdfOutput), "");
                            }
                        }

                        @Override
                        public void onConversionFailed() {

                        }
                    });
                }
            });
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            importWebpageUrlSelectorDialogFragment.show(fragmentManager, "ImportWebpageUrlSelectorDialogFragment");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_FOLDERS);
    }

    @Override
    public void onStop() {
        super.onStop();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_FOLDERS);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        // cleanup previous resource
        if (null != mAdapter) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.cleanupResources();
        }

        super.onDestroy();
    }

    public void onLowMemory() {
        super.onLowMemory();
        MiscUtils.handleLowMemory(getContext(), mAdapter);
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_LOW_MEMORY, AnalyticsParam.lowMemoryParam(TAG));
        Logger.INSTANCE.LogE(TAG, "low memory");
    }

    // Let's make sure that the parent activity implements all necessary interfaces.
    @Override
    public void onAttach(Context context) {
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
            mSortMode = FileInfoComparator.folderPathOrder();
            menuItem = menu.findItem(R.id.menu_file_sort_by_name);
        } else {
            mSortMode = FileInfoComparator.folderDateOrder();
            menuItem = menu.findItem(R.id.menu_file_sort_by_date);
        }
        if (menuItem != null) {
            menuItem.setChecked(true);
        }

        // Set grid size radio buttons to correct value from settings
        int gridSize = PdfViewCtrlSettingsManager.getGridSize(getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FOLDER_FILES);
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
        Context context = getContext();
        if (context == null) {
            return false;
        }

        boolean handled = false;
        if (item.getItemId() == R.id.menu_action_search) {
            finishSearchView();
            handled = true;
        }
        if (item.getItemId() == R.id.menu_action_reload) {
            ThumbnailPathCacheManager.getInstance().cleanupResources(getContext());
            reloadFileInfoList(true);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_file_sort_by_name) {
            // Update sort mode setting
            mSortMode = FileInfoComparator.folderPathOrder();
            PdfViewCtrlSettingsManager.updateSortMode(context, PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_NAME);
            item.setChecked(true);
            reloadFileInfoList(false);
            handled = true;
        }
        if (item.getItemId() == R.id.menu_file_sort_by_date) {
            // Update sort mode setting
            mSortMode = FileInfoComparator.folderDateOrder();
            PdfViewCtrlSettingsManager.updateSortMode(context, PdfViewCtrlSettingsManager.KEY_PREF_SORT_BY_DATE);
            item.setChecked(true);
            reloadFileInfoList(false);
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
            mFileTypeFilterListPopup = new FileTypeFilterPopupWindow(this.getContext(), this.getView(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FOLDER_FILES);

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
        boolean hasExtension = FilenameUtils.isExtension(title, "pdf");
        if (!hasExtension) {
            title = title + ".pdf";
        }
        File documentFile = new File(mCurrentFolder, title);
        String filePath = Utils.getFileNameNotInUse(documentFile.getAbsolutePath());
        if (Utils.isNullOrEmpty(filePath)) {
            CommonToast.showText(getActivity(), R.string.dialog_merge_error_message_general, Toast.LENGTH_SHORT);
            return;
        }
        documentFile = new File(filePath);

        performMerge(filesToMerge, filesToDelete, documentFile);
    }

    protected void performMerge(ArrayList<FileInfo> filesToMerge, ArrayList<FileInfo> filesToDelete, File documentFile) {
        FileManager.merge(getActivity(), filesToMerge, filesToDelete, new FileInfo(BaseFileInfo.FILE_TYPE_FILE, documentFile), LocalFolderViewFragment.this);
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
        if (mSpanCount != count) {
            PdfViewCtrlSettingsManager.updateGridSize(context, PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FOLDER_FILES, count);
        }
        mSpanCount = count;
        updateGridMenuState(mOptionsMenu);
        mRecyclerView.updateSpanCount(count);
    }

    public void resetFileListFilter() {
        String filterText = getFilterText();
        if (!Utils.isNullOrEmpty(filterText) && mAdapter != null) {
            mAdapter.getFilter().filter("");
            mAdapter.setInSearchMode(false);
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

    private void clearFileInfoSelectedList() {
        if (mItemSelectionHelper != null) {
            mItemSelectionHelper.clearChoices();
        }
        mFileInfoSelectedList.clear();
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

    private void updateFileListFilter() {
        if (mAdapter == null) {
            return;
        }

        String constraint = getFilterText();
        if (constraint == null) {
            constraint = "";
        }
        mAdapter.getFilter().filter(constraint);
        boolean isEmpty = Utils.isNullOrEmpty(constraint);
        mAdapter.setInSearchMode(!isEmpty);
    }

    private void rebuildBreadcrumbBar(Context context, File leafFile) {
        mBreadcrumbBarLayout.removeAllViews();

        File parent;
        if (leafFile != null) { // Start generating crumbs at leafFile, if provided
            parent = leafFile;
        } else {
            parent = mCurrentFolder;
        }
        if (Utils.isLollipop()) {
            while (parent != null) {
                createBreadcrumb(context, parent, 0); // Add to start
                if (parent.getParentFile() != null && !parent.getParent().equalsIgnoreCase("/")) {
                    parent = parent.getParentFile();
                } else {
                    break;
                }
            }
        } else {
            while (parent != null) {
                createBreadcrumb(context, parent, 0); // Add to start
                if (parent.getParentFile() != null && !parent.getParent().equalsIgnoreCase("/")) {
                    parent = parent.getParentFile();
                } else {
                    break;
                }
            }
        }
    }

    private int findBreadcrumb(File folder) {
        int position = -1;
        if (folder != null) {
            for (int i = 0; i < mBreadcrumbBarLayout.getChildCount(); i++) {
                LinearLayout crumb = (LinearLayout) mBreadcrumbBarLayout.getChildAt(i);
                Object tag = crumb.getTag();
                if (tag != null && tag instanceof File) {
                    File file = (File) tag;
                    if (file.getAbsolutePath().equals(folder.getAbsolutePath())) {
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
    private void appendBreadcrumb(Context context, File newFolder) {
        int currentCrumb = -1;
        if (mBreadcrumbBarLayout.getChildCount() > 0) {
            // Find the current folder's crumb in the bar
            if (mCurrentFolder != null) {
                currentCrumb = findBreadcrumb(mCurrentFolder);
            }
            // Check if the next crumb (right) corresponds to the new folder
            if (currentCrumb >= 0) {
                if (currentCrumb + 1 < mBreadcrumbBarLayout.getChildCount()) {
                    boolean clearToRight = true;
                    LinearLayout crumb = (LinearLayout) mBreadcrumbBarLayout.getChildAt(currentCrumb + 1);
                    Object tag = crumb.getTag();
                    if (tag != null && tag instanceof File) {
                        File file = (File) tag;
                        if (file.getAbsolutePath().equals(newFolder.getAbsolutePath())) {
                            // New folder is already in breadcrumb bar
                            clearToRight = false;
                        }
                    }
                    if (clearToRight) {
                        // let's rebuild bread crumb bar from scratch, since it can be a
                        // non-immediate child
                        rebuildBreadcrumbBar(getContext(), newFolder);
                        // New subtree - update local folder tree setting
                        PdfViewCtrlSettingsManager.updateLocalFolderTree(context, newFolder.getAbsolutePath());
                    }
                } else {
                    // let's rebuild bread crumb bar from scratch, since it can be a
                    // non-immediate child
                    rebuildBreadcrumbBar(getContext(), newFolder);
                    // Traversing down tree - update local folder tree setting
                    PdfViewCtrlSettingsManager.updateLocalFolderTree(context, newFolder.getAbsolutePath());
                }
                setCurrentBreadcrumb(currentCrumb + 1, true);
            }
        }
        if (currentCrumb < 0) {
            // Current crumb could not be found or bar is not built, try (re)building the bar
            rebuildBreadcrumbBar(context, null);
            // Create a new crumb and add to end of bar
            createBreadcrumb(context, newFolder, -1);
            // Update local folder tree setting
            PdfViewCtrlSettingsManager.updateLocalFolderTree(context, newFolder.getAbsolutePath());

            setCurrentBreadcrumb(-1, true);
        }
    }

    private void createBreadcrumb(Context context, File folder, int position) {
        @SuppressLint("InflateParams") final LinearLayout crumb =
            (LinearLayout) LayoutInflater.from(context).inflate(R.layout.breadcrumb, null);
        TextView dirTextView = crumb.findViewById(R.id.crumbText);
        if (!Utils.isLollipop() && folder.getParentFile() == null) { // Show "ROOT" instead of "/"
            dirTextView.setText(getString(R.string.root_folder).toUpperCase());
        } else {
            dirTextView.setText(folder.getName().toUpperCase());
        }
        crumb.setTag(folder);
        crumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object tag = crumb.getTag();
                if (tag != null && tag instanceof File) {
                    File file = (File) tag;
                    if (mCurrentFolder != null &&
                        !FilenameUtils.equals(file.getAbsolutePath(), mCurrentFolder.getAbsolutePath())) {
                        mCurrentFolder = file;
                        updateFileObserver();

                        finishSearchView();
                        finishActionMode();
                        reloadFileInfoList(false);
                    }
                }
            }
        });

        mBreadcrumbBarLayout.addView(crumb, position);
    }

    // Adjust crumb text and chevron colors to indicate current crumb
    private void setCurrentBreadcrumb(int position, boolean focusCurrent) {
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
        if (focusCurrent) {
            View lastChild = mBreadcrumbBarLayout.getChildAt(position);
            mBreadcrumbBarScrollView.requestChildFocus(mBreadcrumbBarLayout, lastChild);
        }
    }

    private void reloadFileInfoList(boolean focusCurrentCrumb) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(true);
        }

        if (mPopulateFolderTask != null) {
            mPopulateFolderTask.cancel(true);
        }
        mPopulateFolderTask = new PopulateFolderTask(context, mCurrentFolder,
            mFileInfoList, mFileListLock, getSortMode(), true, true, true, mSdCardFolderCache, this);
        mPopulateFolderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        File leafFile = null;
        String leaf = PdfViewCtrlSettingsManager.getLocalFolderTree(context);
        if (!Utils.isNullOrEmpty(leaf)) {
            leafFile = new File(leaf);
        }
        while (leafFile != null && !leafFile.exists()) {
            leafFile = leafFile.getParentFile();
        }
        rebuildBreadcrumbBar(context, leafFile);

        focusCurrentCrumb = (focusCurrentCrumb || leafFile != null);
        if (mCurrentFolder != null) {
            setCurrentBreadcrumb(findBreadcrumb(mCurrentFolder), focusCurrentCrumb);
        } else {
            setCurrentBreadcrumb(0, focusCurrentCrumb);
        }

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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (Activity.RESULT_OK == resultCode) {
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

                    File documentFile = new File(mCurrentFolder, filename + ".pdf");
                    documentFile = new File(Utils.getFileNameNotInUse(documentFile.getAbsolutePath()));
                    String outputPath = ViewerUtils.imageIntentToPdf(activity, selectedImage, imageFilePath, documentFile.getAbsolutePath());
                    if (outputPath != null) {
                        String toastMsg = getString(R.string.dialog_create_new_document_filename_success) + mCurrentFolder.getPath();
                        CommonToast.showText(getActivity(), toastMsg, Toast.LENGTH_LONG);
                        if (mCallbacks != null) {
                            mCallbacks.onFileSelected(documentFile, "");
                        }
                    }

                    finishActionMode();

                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                        AnalyticsParam.createNewParam(isCamera ? AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_CAMERA : AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_IMAGE,
                            AnalyticsHandlerAdapter.SCREEN_FOLDERS));
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
                if (isCamera) {
                    FileUtils.deleteQuietly(new File(imageFilePath));
                }
            }
        }
    }

    public void saveCreatedDocument(PDFDoc doc, String title) {
        try {
            boolean hasExtension = FilenameUtils.isExtension(title, "pdf");
            if (!hasExtension) {
                title = title + ".pdf";
            }
            File documentFile = new File(mCurrentFolder, title);
            String filePath = Utils.getFileNameNotInUse(documentFile.getAbsolutePath());
            if (Utils.isNullOrEmpty(filePath)) {
                CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                return;
            }
            documentFile = new File(filePath);

            doc.save(documentFile.getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null);
            doc.close();

            String toastMsg = getString(R.string.dialog_create_new_document_filename_success) + filePath;
            CommonToast.showText(getActivity(), toastMsg, Toast.LENGTH_LONG);

            mCallbacks.onFileSelected(documentFile, "");

            finishActionMode();
//            reloadFileInfoList();
        } catch (Exception e) {
            CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // prevent clearing filter text when the fragment is hidden
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
        //      if changes not in current folder, reload everything
        if (!mCurrentFolder.getAbsolutePath().equals(oldFile.getAbsolutePath())
            || !mCurrentFolder.getAbsolutePath().equals(newFile.getAbsolutePath())) {
            reloadFileInfoList(false);
        }
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
    }

    @Override
    public void onFolderCreated(FileInfo rootFolder, FileInfo newFolder) {
        if (newFolder != null) {
            appendBreadcrumb(getActivity(), newFolder.getFile());
            // Refresh the view with new content.
            mCurrentFolder = newFolder.getFile();
            updateFileObserver();
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_FOLDER, AnalyticsHandlerAdapter.SCREEN_FOLDERS));
        }
        reloadFileInfoList(true);

    }

    @Override
    public void onFileDuplicated(File fileCopy) {
        finishActionMode();
        hideFileInfoDrawer();
//        reloadFileInfoList();
        if (!mCurrentFolder.getAbsolutePath().equals(fileCopy.getAbsolutePath())) {
            reloadFileInfoList(false);
        }
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
            boolean rebuildBreadcrumbs = false;
            boolean reloadList = false;
            File parent = null;
            // update user bookmarks
            for (FileInfo info : deletedFiles) {
                if (!rebuildBreadcrumbs && info.getType() == BaseFileInfo.FILE_TYPE_FOLDER && findBreadcrumb(info.getFile()) != -1) {
                    // Folder is part of breadcrumb bar - rebuild the breadcrumbs
                    rebuildBreadcrumbs = true;
                    parent = info.getFile().getParentFile();
                }
                BookmarkManager.removeUserBookmarks(activity, info.getAbsolutePath());
                if (!info.getParentDirectoryPath().equals(mCurrentFolder.getAbsolutePath())) {
                    reloadList = true;
                }
                if (mAdapter != null) {
                    mAdapter.evictFromMemoryCache(info.getAbsolutePath());
                }
            }
            if (rebuildBreadcrumbs) {
                rebuildBreadcrumbBar(activity, parent);

                if (mCurrentFolder != null) {
                    setCurrentBreadcrumb(findBreadcrumb(mCurrentFolder), true);
                } else {
                    setCurrentBreadcrumb(0, true);
                }
                String folderTree = getCrumbFolderTree();
                PdfViewCtrlSettingsManager.updateLocalFolderTree(activity, folderTree);
            }

            if (reloadList) {
                reloadFileInfoList(true);
            }
            handleFilesRemoved(deletedFiles);
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
//        reloadFileInfoList();
        ArrayList<FileInfo> targetFileList = new ArrayList<>();
        boolean reloadList = false;
        for (Map.Entry<FileInfo, Boolean> entry : filesMoved.entrySet()) {
            // only update if the move operation was successful
            if (entry.getValue()) {
                FileInfo info = entry.getKey();
                File targetFile = new File(targetFolder, info.getName());
                FileInfo targetFileInfo = new FileInfo(info.getType(), targetFile);
                // update recent and favorite lists
                handleFileUpdated(info, targetFileInfo);
                // update user bookmarks
                BookmarkManager.updateUserBookmarksFilePath(activity, info.getAbsolutePath(), targetFile.getAbsolutePath());
                targetFileList.add(targetFileInfo);
                if (!info.getAbsolutePath().equals(mCurrentFolder.getAbsolutePath())
                    || !targetFile.getAbsolutePath().equals(mCurrentFolder.getAbsolutePath())) {
                    reloadList = true;
                }
            }
        }

        if (reloadList) {
            reloadFileInfoList(false);
        }
        new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), targetFileList, mCacheLock).execute();
    }

    @Override
    public void onFileMoved(Map<FileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        finishActionMode();
        hideFileInfoDrawer();
//        reloadFileInfoList();
        ArrayList<FileInfo> targetFileList = new ArrayList<>();
        boolean reloadList = false;
        for (Map.Entry<FileInfo, Boolean> entry : filesMoved.entrySet()) {
            // only update if the move operation was successful
            if (entry.getValue()) {
                try {
                    FileInfo fileInfo = entry.getKey();
                    String targetFilePath = ExternalFileInfo.appendPathComponent(targetFolder.getUri(), fileInfo.getName()).toString();
                    FileInfo targetFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, targetFilePath,
                        fileInfo.getName(), false, 1);
                    // Update recent and favorite lists
                    handleFileUpdated(fileInfo, targetFileInfo);
                    // Update tab info
                    PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                        fileInfo.getAbsolutePath(), targetFileInfo.getAbsolutePath(), targetFileInfo.getFileName());
                    // update user bookmarks
                    BookmarkManager.updateUserBookmarksFilePath(activity, fileInfo.getAbsolutePath(), targetFileInfo.getAbsolutePath());
                    // add target file to cache
                    targetFileList.add(targetFileInfo);
                    if (!fileInfo.getAbsolutePath().equals(mCurrentFolder.getAbsolutePath())
                        || !targetFilePath.equals(mCurrentFolder.getAbsolutePath())) {
                        reloadList = true;
                    }
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }
        if (reloadList) {
            reloadFileInfoList(false);
        }
        new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), targetFileList, mCacheLock).execute();
    }

    @Override
    public void onFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {
        finishActionMode();
        hideFileInfoDrawer();

        if (newFile == null) {
            return;
        }

        if (mCallbacks != null) {
            if (newFile.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                mCallbacks.onFileSelected(newFile.getFile(), "");
            } else if (newFile.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                mCallbacks.onExternalFileSelected(newFile.getAbsolutePath(), "");
            }
        }

        if (!newFile.getAbsolutePath().equals(mCurrentFolder.getAbsolutePath())) {
            reloadFileInfoList(false);
        }
        MiscUtils.removeFiles(filesToDelete);
    }

    @Override
    public void onFileChanged(String path, int event) {
        // this method is called from background thread

        if (path == null) {
            return;
        }

        File file = new File(path);

        Logger.INSTANCE.LogI(TAG, "file changes: "
            + file.getAbsolutePath() + " is file: "
            + file.isFile() + " is dir: "
            + file.isDirectory());

        boolean fileChanged = true;
        ArrayList<FileInfo> tempList;
        synchronized (mFileListLock) {
            tempList = new ArrayList<>(mFileInfoList);
        }
        switch (event &= RecursiveFileObserver.ALL_EVENTS) {
            case RecursiveFileObserver.MOVED_FROM:
            case RecursiveFileObserver.DELETE:
                if (event == RecursiveFileObserver.MOVED_FROM)
                    Logger.INSTANCE.LogI(TAG, "MOVE_FROM: ");
                else
                    Logger.INSTANCE.LogI(TAG, "DELETE: " + tempList.size());
                for (int i = 0; i < tempList.size(); i++) {
                    FileInfo fileInfo = tempList.get(i);
                    if (fileInfo.getAbsolutePath().equals(path)) {
                        // FileManager.deleteFileInCache(fileInfo);
                        new FileManager.ChangeCacheFileTask(fileInfo, new ArrayList<FileInfo>(), mCacheLock).execute();
                        tempList.remove(i);
                        break;
                    }
                }
                break;
            case RecursiveFileObserver.MOVED_TO:
            case RecursiveFileObserver.CREATE:
                Logger.INSTANCE.LogI(TAG, "CREATE: ");
                // add the new file to the right position based on sort method

                FileInfo fileInfo;
                if (file.isDirectory()) {
                    fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FOLDER, file);
                } else if (file.isFile()) {
                    fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file);
                } else {
                    fileChanged = false;
                    break;
                }

                // add the new file to the right position based on sort method
                for (int i = 0, count = tempList.size(); i < count; ++i) {
                    FileInfo f = tempList.get(i);
                    if (compare(f, fileInfo) > 0) {
                        tempList.add(i, fileInfo);
                        if (file.isFile()) {
                            //FileManager.addFileToCache(fileInfo);
                            new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), fileInfo, mCacheLock).execute();
                        }
                        break;
                    }
                }
                break;
            default:
                Logger.INSTANCE.LogI(TAG, "OTHER: " + event);
                fileChanged = false;
                break;
        }

        if (fileChanged) {
            synchronized (mFileListLock) {
                mFileInfoList.clear();
                mFileInfoList.addAll(tempList);
            }

            // run it in UI thread
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    updateFileListFilter();
                }
            };
            handler.post(runnable);
        }
    }

    private Comparator<FileInfo> getSortMode(
    ) {

        if (mSortMode != null) {
            return mSortMode;
        }

        return FileInfoComparator.folderPathOrder();

    }

    private int compare(FileInfo file1, FileInfo file2) {
        if (mSortMode != null) {
            return mSortMode.compare(file1, file2);
        }
        return FileInfoComparator.folderPathOrder().compare(file1, file2);
    }

    @Override
    public void onFileClicked(FileInfo fileInfo) {
        if (mIsSearchMode) {
            hideSoftKeyboard();
        }

        if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_FILE) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_FOLDERS));
            mCallbacks.onFileSelected(fileInfo.getFile(), "");
        } else if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_FOLDER) {
            appendBreadcrumb(getActivity(), fileInfo.getFile());
            // Refresh the view with new content.
            mCurrentFolder = fileInfo.getFile();
            updateFileObserver();
            reloadFileInfoList(true);
        }
    }

    @Override
    public void onLocalFolderSelected(int requestCode, Object object, File folder) {
        if (requestCode == RequestCode.MOVE_FILE) {
            if (mSelectedFile != null) {
                FileManager.move(getActivity(), new ArrayList<>(Collections.singletonList(mSelectedFile)), folder, LocalFolderViewFragment.this);
            }
        } else if (requestCode == RequestCode.MOVE_FILE_LIST) {
            FileManager.move(getActivity(), mFileInfoSelectedList, folder, LocalFolderViewFragment.this);
        }
    }

    @Override
    public void onExternalFolderSelected(int requestCode, Object object, ExternalFileInfo folder) {
        if (requestCode == RequestCode.MOVE_FILE) {
            if (mSelectedFile != null) {
                FileManager.move(getActivity(), new ArrayList<>(Collections.singletonList(mSelectedFile)), folder, LocalFolderViewFragment.this);
            }
        } else if (requestCode == RequestCode.MOVE_FILE_LIST) {
            FileManager.move(getActivity(), mFileInfoSelectedList, folder, LocalFolderViewFragment.this);
        }
    }

    @Override
    public void onPreLaunchViewer() {
        mViewerLaunching = true;
    }

    @Override
    public void onDataChanged() {
        if (isAdded()) {
            reloadFileInfoList(true);
        } else {
            // otherwise it will be reloaded in resumeFragment
            mDataChanged = true;
        }
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
            // Exit action mode
            finishActionMode();
            handled = true;
        } else if (mIsSearchMode) {
            // Exit search mode
            finishSearchView();
            handled = true;
        } else {
            // Navigate up through directories
            File rootDir = Environment.getExternalStorageDirectory();
            if (Utils.isLollipop()) {
                while (rootDir != null && rootDir.getParentFile() != null && (!rootDir.getParentFile().getAbsolutePath().equalsIgnoreCase("/") && !DEBUG)) {
                    rootDir = rootDir.getParentFile();
                }
            } else {
                while (rootDir != null && rootDir.getParentFile() != null) {
                    rootDir = rootDir.getParentFile();
                }
            }
            if (!mCurrentFolder.equals(rootDir) && mCurrentFolder.getParentFile() != null && !mCurrentFolder.getParent().equals("/")) {
                mCurrentFolder = mCurrentFolder.getParentFile();
                updateFileObserver();
                reloadFileInfoList(true);
                handled = true;
            }
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
            if (mEmptyTextView != null) {
                if (mAdapter.getItemCount() > 0) {
                    mEmptyTextView.setVisibility(View.GONE);
                    scrollToCurrentFile(mRecyclerView);
                } else if (mIsFullSearchDone) {
                    switch (resultCode) {
                        case FileListFilter.FILTER_RESULT_NO_STRING_MATCH:
                            mEmptyTextView.setText(R.string.textview_empty_because_no_string_match);
                            break;
                        case FileListFilter.FILTER_RESULT_NO_ITEMS_OF_SELECTED_FILE_TYPES:
                            mEmptyTextView.setText(R.string.textview_empty_because_no_files_of_selected_type);
                            break;
                        default:
                            mEmptyTextView.setText(R.string.textview_empty_file_list);
                            break;
                    }
                    mEmptyTextView.setVisibility(View.VISIBLE);
                }
            }

            mNotSupportedTextView.setVisibility(View.GONE);
            if (mCurrentFolder != null && mCurrentFolder.list() != null && getContext() != null) {
                int fileCount = mCurrentFolder.list().length;
                if (fileCount > mAdapter.getItemCount()) {
                    int extraCount = fileCount - mAdapter.getItemCount();
                    String fileStr = extraCount > 1 ? getString(R.string.files) : getString(R.string.file);
                    mNotSupportedTextView.setText(getString(R.string.num_files_not_supported, extraCount, fileStr));
                    mNotSupportedTextView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    public void updateFileObserver() {
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }
        if (mCurrentFolder == null) {
            return;
        }
        mFileObserver = new RecursiveFileObserver(mCurrentFolder.getAbsolutePath(), RecursiveFileObserver.CHANGES_ONLY, this);
        mFileObserver.startWatching();
    }

    private void resumeFragment() {
        Logger.INSTANCE.LogD(TAG, "resumeFragment");

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        mViewerLaunching = false;

        String directory = PdfViewCtrlSettingsManager.getLocalFolderPath(activity);
        if (!Utils.isNullOrEmpty(directory)) {
            mCurrentFolder = new File(directory);
            updateFileObserver();
        }

        reloadFileInfoList(mDataChanged);
        mDataChanged = false;

        if (mLocalFolderViewFragmentListener != null) {
            mLocalFolderViewFragmentListener.onLocalFolderShown();
        }
    }

    private void pauseFragment() {
        Logger.INSTANCE.LogD(TAG, "pauseFragment");

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        mFilterText = getFilterText();

        if (mIsSearchMode && !mViewerLaunching) {
            finishSearchView();
        }

        if (mPopulateFolderTask != null) {
            mPopulateFolderTask.cancel(true);
        }

        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.cleanupResources();
        }

        // Save the current folder path
        PdfViewCtrlSettingsManager.updateLocalFolderPath(activity, mCurrentFolder.getAbsolutePath());
        // Save the folder tree, ie. the right-most breadcrumb
        String folderTree = getCrumbFolderTree();
        PdfViewCtrlSettingsManager.updateLocalFolderTree(activity, folderTree);

        if (mLocalFolderViewFragmentListener != null) {
            mLocalFolderViewFragmentListener.onLocalFolderHidden();
        }
    }

    public void setLocalFolderViewFragmentListener(LocalFolderViewFragmentListener listener) {
        mLocalFolderViewFragmentListener = listener;
    }

    private String getCrumbFolderTree() {
        // Save the folder tree, ie. the right-most breadcrumb
        String folderTree = "";
        if (mBreadcrumbBarLayout != null && mBreadcrumbBarLayout.getChildCount() > 0) {
            View crumb = mBreadcrumbBarLayout.getChildAt(mBreadcrumbBarLayout.getChildCount() - 1);
            Object tag = crumb.getTag();
            if (tag != null && tag instanceof File) {
                File file = (File) tag;
                folderTree = file.getAbsolutePath();
            }
        }
        return folderTree;
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

    @Override
    public void onPopulateFolderTaskStarted(
    ) {

        Context context = getContext();
        if (context == null) {
            return;
        }

        synchronized (mFileListLock) {
            mFileInfoList.clear();
        }
        updateFileListFilter();

        if (mEmptyTextView != null) {
            mLoadingFileHandler.sendEmptyMessageDelayed(0, 100);
        }
        if (mProgressBarView != null) {
            mProgressBarView.setVisibility(View.VISIBLE);
        }

        setReloadActionButtonState(true);
        mIsFullSearchDone = false;

    }

    @Override
    public void onPopulateFolderTaskProgressUpdated(
        File currentFolder
    ) {

        mLoadingFileHandler.removeMessages(0);
        if (mProgressBarView != null) {
            mProgressBarView.setVisibility(View.GONE);
        }

        showPopulatedFolder(currentFolder);
        updateFileListFilter();
        setReloadActionButtonState(false);

    }

    @Override
    public void onPopulateFolderTaskFinished(
    ) {

        mIsFullSearchDone = true;
        updateFileListFilter();

    }

    void showPopulatedFolder(
        File currentFolder
    ) {

        if (currentFolder == null) {
            return;
        }

        mInSDCardFolder = false;
        Boolean isSdCardFolder = mSdCardFolderCache.get(currentFolder.getAbsolutePath());
        if (isSdCardFolder != null && isSdCardFolder) {
            mInSDCardFolder = true;
        }

        if (mGoToSdCardView == null || mRecyclerView == null || mFabMenu == null) {
            return;
        }

        if (Utils.isLollipop()) {
            if (mInSDCardFolder) {
                if (mSnackBar == null) {
                    mSnackBar = Snackbar.make(mRecyclerView, R.string.snack_bar_local_folder_read_only, Snackbar.LENGTH_INDEFINITE);
                    mSnackBar.setAction(getString(R.string.snack_bar_local_folder_read_only_redirect).toUpperCase(),
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // go to external tab
                                if (null != mJumpNavigationCallbacks) {
                                    finishActionMode();
                                    mJumpNavigationCallbacks.gotoExternalTab();
                                }
                            }
                        });

                    mSnackBar.addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            mSnackBar = null;
                        }
                    });
                    //mSnackBar.show();
                }
                // Show dialog re-direct user to SD card tab
                mRecyclerView.setVisibility(View.GONE);
                mFabMenu.setVisibility(View.GONE);
                mGoToSdCardView.setVisibility(View.VISIBLE);
            } else {
                if (mSnackBar != null) {
                    if (mSnackBar.isShown()) {
                        mSnackBar.dismiss();
                    }
                    mSnackBar = null;
                }
                mGoToSdCardView.setVisibility(View.GONE);
                mRecyclerView.setVisibility(View.VISIBLE);
                mFabMenu.setVisibility(View.VISIBLE);
            }
        }

    }

    private static class LoadingFileHandler extends Handler {
        private final WeakReference<LocalFolderViewFragment> mFragment;

        LoadingFileHandler(LocalFolderViewFragment fragment) {
            mFragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            LocalFolderViewFragment fragment = mFragment.get();
            if (fragment != null && fragment.mEmptyTextView != null) {
                fragment.mEmptyTextView.setText(R.string.loading_files_wait);
                fragment.mEmptyTextView.setVisibility(View.VISIBLE);
            }
            removeMessages(0);
        }
    }

    private final LoadingFileHandler mLoadingFileHandler = new LoadingFileHandler(this);

}
