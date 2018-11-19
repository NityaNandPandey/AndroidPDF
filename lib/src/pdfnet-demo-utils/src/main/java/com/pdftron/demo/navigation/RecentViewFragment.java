//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.pdftron.common.PDFNetException;
import com.pdftron.demo.R;
import com.pdftron.demo.R2;
import com.pdftron.demo.dialog.FilePickerDialogFragment;
import com.pdftron.demo.dialog.MergeDialogFragment;
import com.pdftron.demo.navigation.adapter.BaseFileAdapter;
import com.pdftron.demo.navigation.adapter.RecentAdapter;
import com.pdftron.demo.navigation.callbacks.FileManagementListener;
import com.pdftron.demo.navigation.callbacks.FileUtilCallbacks;
import com.pdftron.demo.navigation.callbacks.MainActivityListener;
import com.pdftron.demo.utils.AddDocPdfHelper;
import com.pdftron.demo.utils.ExternalFileManager;
import com.pdftron.demo.utils.FileManager;
import com.pdftron.demo.utils.Logger;
import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.demo.utils.ThumbnailPathCacheManager;
import com.pdftron.demo.utils.ThumbnailWorker;
import com.pdftron.demo.widget.FileTypeFilterPopupWindow;
import com.pdftron.demo.widget.ImageViewTopCrop;
import com.pdftron.demo.widget.MoveUpwardBehaviour;
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
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.PdfViewCtrlTabsManager;
import com.pdftron.pdf.utils.RequestCode;
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
import java.util.Date;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class RecentViewFragment extends FileBrowserViewFragment implements
    MainActivityListener,
    BaseFileAdapter.AdapterListener,
    FileManagementListener,
    ExternalFileManager.ExternalFileManagementListener,
    FilePickerDialogFragment.LocalFolderListener,
    FilePickerDialogFragment.ExternalFolderListener,
    MergeDialogFragment.MergeDialogFragmentListener,
    ActionMode.Callback,
    FileInfoDrawer.Callback {

    private static final String TAG = RecentViewFragment.class.getName();

    @BindView(R2.id.recycler_view)
    protected SimpleRecyclerView mRecyclerView;
    @BindView(R2.id.empty_text_view)
    protected TextView mEmptyTextView;
    @BindView(R2.id.progress_bar_view)
    protected ProgressBar mProgressBarView;
    @BindView(R2.id.fab_menu)
    protected FloatingActionMenu mFabMenu;

    private Unbinder unbinder;

    protected ArrayList<FileInfo> mFileInfoList = new ArrayList<>();
    protected ArrayList<FileInfo> mFileInfoSelectedList = new ArrayList<>();
    protected FileInfo mSelectedFile;

    private FileUtilCallbacks mFileUtilCallbacks;

    protected RecentAdapter mAdapter;
    private FileInfoDrawer mFileInfoDrawer;
    private PopulateFileInfoListTask mPopulateFileInfoListTask;
    private Menu mOptionsMenu;
    private boolean mFirstRun;
    private String mDocumentTitle;
    private PDFDoc mCreatedDoc;
    private String mCreatedDocumentTitle;
    private Uri mOutputFileUri;
    private String mImageFilePath;
    private Uri mImageUri;
    private String mImageFileDisplayName;
    private boolean mIsCamera;
    private String mEmptyText;

    protected ArrayList<FileInfo> mMergeFileList;
    protected ArrayList<FileInfo> mMergeTempFileList;

    protected int mSpanCount;
    protected ItemSelectionHelper mItemSelectionHelper;

    private RecentViewFragmentListener mRecentViewFragmentListener;

    protected MenuItem itemFavorite;
    protected MenuItem itemRemove;
    protected MenuItem itemMerge;
    protected MenuItem itemShare;
    protected MenuItem itemRename;

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (super.onCreateActionMode(mode, menu)) {
            return true;
        }

        mode.getMenuInflater().inflate(R.menu.cab_fragment_recent_view, menu);

        itemFavorite = menu.findItem(R.id.action_add_to_favorites);
        itemRemove = menu.findItem(R.id.action_recent_list_remove);
        itemMerge = menu.findItem(R.id.cab_file_merge);
        itemShare = menu.findItem(R.id.cab_file_share);
        itemRename = menu.findItem(R.id.cab_file_rename);
        itemRemove.setIcon(null); // Hide icon
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }
        if (itemShare != null) {
            itemShare.setVisible(true);
        }

        if (itemFavorite != null) {
            if (mFileInfoSelectedList.size() > 1) {
                itemFavorite.setVisible(false);
            } else {
                itemFavorite.setVisible(true);
                if (!getFavoriteFilesManager().containsFile(activity, mFileInfoSelectedList.get(0))) {
                    itemFavorite.setTitle(activity.getString(R.string.action_add_to_favorites));
                } else {
                    itemFavorite.setTitle(activity.getString(R.string.action_remove_from_favorites));
                }
            }
        }
        if (itemMerge != null) {
            itemMerge.setVisible(mFileInfoSelectedList.size() >= 1);
        }
        if (itemRename != null) {
            if (mFileInfoSelectedList.size() == 1) {
                if (mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_FOLDER ||
                    mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_FILE) {
                    boolean isSDCardFile = Utils.isSdCardFile(activity, mFileInfoSelectedList.get(0).getFile());
                    String DBX_CACHE_PATH = "/Android/data/com.dropbox.android/";
                    boolean isDbxFile = mFileInfoSelectedList.get(0).getAbsolutePath().contains(DBX_CACHE_PATH);
                    if (isSDCardFile || isDbxFile) {
                        itemRename.setVisible(false);
                    } else {
                        itemRename.setVisible(true);
                    }
                } else if (mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                    mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
                    itemRename.setVisible(true);
                } else {
                    itemRename.setVisible(false);
                }
            } else {
                itemRename.setVisible(false);
            }
        }
        mode.setTitle(Utils.getLocaleDigits(Integer.toString(mFileInfoSelectedList.size())));
        // Ensure items are always shown

        if (itemFavorite != null) {
            itemFavorite.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        if (itemRemove != null) {
            itemRemove.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        if (itemRename != null) {
            itemRename.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

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

        if (item.getItemId() == R.id.cab_file_rename) {
            if (mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_FILE ||
                mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_FOLDER) {
                FileManager.rename(activity, mFileInfoSelectedList.get(0).getFile(), RecentViewFragment.this);
            } else if (mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
                ExternalFileInfo externalFileInfo = Utils.buildExternalFile(activity,
                    Uri.parse(mFileInfoSelectedList.get(0).getAbsolutePath()));
                ExternalFileManager.rename(activity, externalFileInfo, RecentViewFragment.this);
            }
        }
        if (item.getItemId() == R.id.action_add_to_favorites) {
            if (!getFavoriteFilesManager().containsFile(activity, mFileInfoSelectedList.get(0))) {
                getFavoriteFilesManager().addFile(activity, mFileInfoSelectedList.get(0));
                CommonToast.showText(activity,
                    getString(R.string.file_added_to_favorites, mFileInfoSelectedList.get(0).getName()),
                    Toast.LENGTH_SHORT);
            } else {
                getFavoriteFilesManager().removeFile(activity, mFileInfoSelectedList.get(0));
                CommonToast.showText(activity,
                    getString(R.string.file_removed_from_favorites, mFileInfoSelectedList.get(0).getName()),
                    Toast.LENGTH_SHORT);
            }
            finishActionMode();
            // Update favorite file indicators
            Utils.safeNotifyDataSetChanged(mAdapter);
        }
        if (item.getItemId() == R.id.action_recent_list_remove) {
            getRecentFilesManager().removeFiles(activity, mFileInfoSelectedList);
            finishActionMode();
            reloadFileInfoList();
        }
        if (item.getItemId() == R.id.cab_file_merge) {
            // Create and show file merge dialog-fragment
            MergeDialogFragment mergeDialog = getMergeDialogFragment(mFileInfoSelectedList, AnalyticsHandlerAdapter.SCREEN_RECENT);
            mergeDialog.initParams(RecentViewFragment.this);
            mergeDialog.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.CustomAppTheme);
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                mergeDialog.show(fragmentManager, "merge_dialog");
            }
            return true;
        }
        if (item.getItemId() == R.id.cab_file_share) {
            if (mFileInfoSelectedList.size() > 1) {
                ArrayList<Uri> files = new ArrayList<>();
                for (FileInfo file : mFileInfoSelectedList) {
                    int fileType = file.getType();
                    if (fileType == BaseFileInfo.FILE_TYPE_FILE) {
                        Uri uri = Utils.getUriForFile(activity, file.getFile());
                        if (uri != null) {
                            files.add(uri);
                        }
                    } else if (fileType == BaseFileInfo.FILE_TYPE_EXTERNAL
                        || fileType == BaseFileInfo.FILE_TYPE_EDIT_URI
                        || fileType == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
                        files.add(Uri.parse(file.getAbsolutePath()));
                    }
                }
                if (mOnPdfFileSharedListener != null) {
                    Intent intent = Utils.createGenericShareIntents(activity, files);
                    mOnPdfFileSharedListener.onPdfFileShared(intent);
                    finishActionMode();
                } else {
                    Utils.shareGenericFiles(activity, files);
                }
            } else if (mFileInfoSelectedList.size() == 1) {
                FileInfo selectedFile = mFileInfoSelectedList.get(0);
                int selectedFileType = selectedFile.getType();
                if (selectedFileType == BaseFileInfo.FILE_TYPE_FILE) {
                    if (mOnPdfFileSharedListener != null) {
                        File sharedFile = new File(selectedFile.getAbsolutePath());
                        Intent intent = Utils.createShareIntent(activity, sharedFile);
                        mOnPdfFileSharedListener.onPdfFileShared(intent);
                        finishActionMode();
                    } else {
                        Utils.sharePdfFile(activity, new File(selectedFile.getAbsolutePath()));
                    }
                } else if (selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL
                    || selectedFileType == BaseFileInfo.FILE_TYPE_EDIT_URI
                    || selectedFileType == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
                    if (mOnPdfFileSharedListener != null) {
                        Intent intent = Utils.createGenericShareIntent(activity, Uri.parse(selectedFile.getAbsolutePath()));
                        mOnPdfFileSharedListener.onPdfFileShared(intent);
                        finishActionMode();
                    } else {
                        Utils.shareGenericFile(activity, Uri.parse(selectedFile.getAbsolutePath()));
                    }
                }
            }
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_FILEBROWSER, "Item share clicked", AnalyticsHandlerAdapter.LABEL_RECENT);
        }

        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        super.onDestroyActionMode(mode);
        mActionMode = null;
        clearFileInfoSelectedList();
    }

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

        if (mFileInfoDrawerHelper.mImageViewReference == null ||
            (mFileInfoDrawerHelper.mImageViewReference.get() != null &&
                !mFileInfoDrawerHelper.mImageViewReference.get().equals(imageView))) {
            mFileInfoDrawerHelper.mImageViewReference = new WeakReference<>(imageView);
        }

        if (mSelectedFile == null) {
            return;
        }

        // Setup thumbnail worker, if required
        if (mFileInfoDrawerHelper.mThumbnailWorker == null) {
            Point dimensions = drawer.getDimensions();
            mFileInfoDrawerHelper.mThumbnailWorker = new ThumbnailWorker(activity, dimensions.x, dimensions.y, null);
            mFileInfoDrawerHelper.mThumbnailWorker.setListener(mThumbnailWorkerListener);
        }

        drawer.setIsSecured(mSelectedFile.isSecured());

        switch (mSelectedFile.getType()) {
            case BaseFileInfo.FILE_TYPE_FILE:
                if (mSelectedFile.isSecured() || mSelectedFile.isPackage()) {
                    int errorRes = Utils.getResourceDrawable(activity, getResources().getString(R.string.thumb_error_res_name));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView.setImageResource(errorRes);
                } else {
                    imageView.setScaleType(ImageView.ScaleType.MATRIX);
                    mFileInfoDrawerHelper.mThumbnailWorker.tryLoadImageWithPath(0, mSelectedFile.getAbsolutePath(), null, imageView);
                }
                break;
            case BaseFileInfo.FILE_TYPE_FOLDER:
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE); // Adjust scale type for folder
                imageView.setImageResource(R.drawable.ic_folder_black_24dp);
                int folderColorRes = Utils.getResourceColor(activity, getResources().getString(R.string.folder_color));
                if (folderColorRes == 0) {
                    folderColorRes = android.R.color.black;
                }
                imageView.getDrawable().mutate().setColorFilter(getResources().getColor(folderColorRes), PorterDuff.Mode.SRC_IN);
                break;
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
                if (mFileInfoDrawerHelper.getExternalFile() != null && mFileInfoDrawerHelper.getExternalFile().isDirectory()) {
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE); // Adjust scale type for folder
                    imageView.setImageResource(R.drawable.ic_folder_black_24dp);
                    folderColorRes = Utils.getResourceColor(activity, getResources().getString(R.string.folder_color));
                    if (folderColorRes == 0) {
                        folderColorRes = android.R.color.black;
                    }
                    imageView.getDrawable().mutate().setColorFilter(getResources().getColor(folderColorRes), PorterDuff.Mode.SRC_IN);
                } else if (mSelectedFile.isSecured() || mSelectedFile.isPackage()) {
                    int errorRes = Utils.getResourceDrawable(activity, getResources().getString(R.string.thumb_error_res_name));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView.setImageResource(errorRes);
                } else {
                    imageView.setScaleType(ImageView.ScaleType.MATRIX);
                    mFileInfoDrawerHelper.mThumbnailWorker.tryLoadImageWithUuid(0, mSelectedFile.getIdentifier(), null, imageView);
                }
                break;
            default:
                int errorRes = Utils.getResourceDrawable(activity, getResources().getString(R.string.thumb_error_res_name));
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                imageView.setImageResource(errorRes);
                break;
        }
    }

    @Override
    public boolean onPrepareIsSecured(FileInfoDrawer drawer) {
        return mSelectedFile != null && mSelectedFile.isSecured();
    }

    @Override
    public CharSequence onPrepareMainContent(FileInfoDrawer drawer) {
        return mFileInfoDrawerHelper.getFileInfoTextBody();
    }

    @Override
    public boolean onCreateDrawerMenu(FileInfoDrawer drawer, Menu menu) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }
        activity.getMenuInflater().inflate(R.menu.cab_fragment_recent_view, menu);

        mFileInfoDrawerHelper.itemFavorite = menu.findItem(R.id.action_add_to_favorites);
        mFileInfoDrawerHelper.itemRemove = menu.findItem(R.id.action_recent_list_remove);
        mFileInfoDrawerHelper.itemShare = menu.findItem(R.id.cab_file_share);
        mFileInfoDrawerHelper.itemRename = menu.findItem(R.id.cab_file_rename);

        return true;
    }

    @Override
    public boolean onPrepareDrawerMenu(FileInfoDrawer drawer, Menu menu) {
        Activity activity = getActivity();
        if (activity == null || mSelectedFile == null) {
            return false;
        }
        if (mFileInfoDrawerHelper.itemFavorite != null) {
            if (!getFavoriteFilesManager().containsFile(activity, mSelectedFile)) {
                mFileInfoDrawerHelper.itemFavorite.setTitle(activity.getString(R.string.action_add_to_favorites));
                mFileInfoDrawerHelper.itemFavorite.setTitleCondensed(activity.getString(R.string.action_favorite));
                mFileInfoDrawerHelper.itemFavorite.setIcon(R.drawable.ic_star_outline_grey600_24dp);
            } else {
                mFileInfoDrawerHelper.itemFavorite.setTitle(activity.getString(R.string.action_remove_from_favorites));
                mFileInfoDrawerHelper.itemFavorite.setTitleCondensed(activity.getString(R.string.action_unfavorite));
                mFileInfoDrawerHelper.itemFavorite.setIcon(R.drawable.ic_star_white_24dp);
            }
        }
        int selectedFileType = mSelectedFile.getType();
        if (mFileInfoDrawerHelper.itemShare != null) {
            if (selectedFileType == BaseFileInfo.FILE_TYPE_FILE
                || selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL
                || selectedFileType == BaseFileInfo.FILE_TYPE_EDIT_URI
                || selectedFileType == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
                mFileInfoDrawerHelper.itemShare.setVisible(true);
            } else {
                mFileInfoDrawerHelper.itemShare.setVisible(false);
            }
        }
        if (mFileInfoDrawerHelper.itemRename != null) {
            if (selectedFileType == BaseFileInfo.FILE_TYPE_FOLDER ||
                selectedFileType == BaseFileInfo.FILE_TYPE_FILE) {
                boolean isSDCardFile = Utils.isSdCardFile(activity, mSelectedFile.getFile());
                String DBX_CACHE_PATH = "/Android/data/com.dropbox.android/";
                boolean isDbxFile = mSelectedFile.getAbsolutePath().contains(DBX_CACHE_PATH);
                if (isSDCardFile || isDbxFile) {
                    mFileInfoDrawerHelper.itemRename.setVisible(false);
                } else {
                    mFileInfoDrawerHelper.itemRename.setVisible(true);
                }
            } else if (selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
                mFileInfoDrawerHelper.itemRename.setVisible(true);
            } else {
                mFileInfoDrawerHelper.itemRename.setVisible(false);
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
        int selectedFileType = mSelectedFile.getType();

        if (menuItem.getItemId() == R.id.cab_file_rename) {
            if (selectedFileType == BaseFileInfo.FILE_TYPE_FILE ||
                selectedFileType == BaseFileInfo.FILE_TYPE_FOLDER) {
                FileManager.rename(activity, mSelectedFile.getFile(), RecentViewFragment.this);
            } else if (selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
                ExternalFileInfo externalFileInfo = Utils.buildExternalFile(getContext(),
                    Uri.parse(mSelectedFile.getAbsolutePath()));
                ExternalFileManager.rename(activity, externalFileInfo, RecentViewFragment.this);
            }
        }
        if (menuItem.getItemId() == R.id.action_add_to_favorites) {
            if (!getFavoriteFilesManager().containsFile(activity, mSelectedFile)) {
                getFavoriteFilesManager().addFile(activity, mSelectedFile);
                CommonToast.showText(activity,
                    getString(R.string.file_added_to_favorites, mSelectedFile.getName()),
                    Toast.LENGTH_SHORT);
            } else {
                getFavoriteFilesManager().removeFile(activity, mSelectedFile);
                CommonToast.showText(activity,
                    getString(R.string.file_removed_from_favorites, mSelectedFile.getName()),
                    Toast.LENGTH_SHORT);
            }
            drawer.invalidate();
            // Update favorite file indicators
            Utils.safeNotifyDataSetChanged(mAdapter);
        }
        if (menuItem.getItemId() == R.id.action_recent_list_remove) {
            getRecentFilesManager().removeFile(activity, mSelectedFile);
            drawer.hide();
            reloadFileInfoList();
        }
        if (menuItem.getItemId() == R.id.cab_file_merge) {
            // Create and show file merge dialog-fragment
            MergeDialogFragment mergeDialog = getMergeDialogFragment(new ArrayList<>(Collections.singletonList(mSelectedFile)), AnalyticsHandlerAdapter.SCREEN_RECENT);
            mergeDialog.initParams(RecentViewFragment.this);
            mergeDialog.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.CustomAppTheme);
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                mergeDialog.show(fragmentManager, "merge_dialog");
            }
        }
        if (menuItem.getItemId() == R.id.cab_file_share) {
            if (selectedFileType == BaseFileInfo.FILE_TYPE_FILE) {
                if (mOnPdfFileSharedListener != null) {
                    Intent intent = Utils.createShareIntent(activity, new File(mSelectedFile.getAbsolutePath()));
                    mOnPdfFileSharedListener.onPdfFileShared(intent);
                } else {
                    Utils.sharePdfFile(activity, new File(mSelectedFile.getAbsolutePath()));
                }
            } else if (selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL
                || selectedFileType == BaseFileInfo.FILE_TYPE_EDIT_URI
                || selectedFileType == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
                if (mOnPdfFileSharedListener != null) {
                    Intent intent = Utils.createGenericShareIntent(activity, Uri.parse(mSelectedFile.getAbsolutePath()));
                    mOnPdfFileSharedListener.onPdfFileShared(intent);
                } else {
                    Utils.shareGenericFile(activity, Uri.parse(mSelectedFile.getAbsolutePath()));
                }
            }
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_FILEBROWSER, "Item share clicked", AnalyticsHandlerAdapter.LABEL_RECENT);
        }

        return true;
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
        mFileInfoDrawerHelper.cancelAllThumbRequests();

        mSelectedFile = null;
        mFileInfoDrawer = null;
    }

    public interface RecentViewFragmentListener {

        void onRecentShown();

        void onRecentHidden();
    }

    public static RecentViewFragment newInstance() {
        return new RecentViewFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.INSTANCE.LogV("LifeCycle", TAG + ".onCreate");

        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        setHasOptionsMenu(true);
        FileManager.initCache(getContext());
        mFirstRun = true;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recent_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }

        unbinder = ButterKnife.bind(this, view);

        // if we are here after PopulateFileInfoListTask is complete
        if (mEmptyText != null) {
            mEmptyTextView.setText(mEmptyText);
            mEmptyTextView.setVisibility(View.VISIBLE);
        } else {
            mEmptyTextView.setVisibility(View.GONE);
        }
        // since there are not many files to process, no need to show "loading files ..."

        mFabMenu.setClosedOnTouchOutside(true);

        if (mFabMenu.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
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
                            AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_BLANK_PDF, AnalyticsHandlerAdapter.SCREEN_RECENT));
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
                mOutputFileUri = ViewerUtils.openImageIntent(RecentViewFragment.this);
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
                        if (fileAbsolutePath == null) {
                            Utils.showAlertDialog(getActivity(), R.string.dialog_add_photo_document_filename_error_message, R.string.error);
                            return;
                        }

                        File file = new File(fileAbsolutePath);
                        if (external) {
                            Logger.INSTANCE.LogD(TAG, "external folder selected");
                            mFirstRun = true;
                            mCallbacks.onExternalFileSelected(fileAbsolutePath, "");
                        } else {
                            FileInfo newFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file);
                            new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), newFileInfo, mCacheLock).execute();

                            mCallbacks.onFileSelected(new File(fileAbsolutePath), "");
                        }
                        if (!external) {
                            CommonToast.showText(getContext(), getString(R.string.dialog_create_new_document_filename_success) + fileAbsolutePath);
                        }
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                            AnalyticsParam.createNewParam(AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_DOCS, AnalyticsHandlerAdapter.SCREEN_RECENT));
                    }

                    @Override
                    public void onMultipleFilesSelected(int requestCode, ArrayList<FileInfo> fileInfoList) {
                        handleMultipleFilesSelected(fileInfoList, AnalyticsHandlerAdapter.SCREEN_RECENT);
                    }
                });
                mAddDocPdfHelper.pickFileAndCreate();
            }
        });

        mSpanCount = PdfViewCtrlSettingsManager.getGridSize(getActivity(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_RECENT_FILES);
        mRecyclerView.initView(mSpanCount);

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
                        mAdapter.getDerivedFilter().setFileTypeEnabledInFilterFromSettings(mRecyclerView.getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_RECENT_FILES);
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

                if (mActionMode == null) {
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
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                if (fileInfo == null || activity == null) {
                    return false;
                }

                if (mActionMode == null) {
                    mFileInfoSelectedList.add(fileInfo);
                    mItemSelectionHelper.setItemChecked(position, true);

                    mActionMode = activity.startSupportActionMode(RecentViewFragment.this);
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
    }

    @Override
    public void onStart() {
        super.onStart();
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_RECENT);
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
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_RECENT);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Logger.INSTANCE.LogD(TAG, "onDestroyView");
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // cleanup previous resource
        if (mAdapter != null) {
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

    protected void finishActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
            clearFileInfoSelectedList();
        }
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
    }

    private void reloadFileInfoList() {
        if (mPopulateFileInfoListTask != null) {
            mPopulateFileInfoListTask.cancel(true);
        }
        mPopulateFileInfoListTask = new PopulateFileInfoListTask(getActivity());
        mPopulateFileInfoListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MiscUtils.updateAdapterViewWidthAfterGlobalLayout(mRecyclerView, mAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            return;
        }

        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_recent_view, menu);
        inflater.inflate(R.menu.menu_addon_file_type_filter, menu);

        mOptionsMenu = menu;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        Context context = getContext();
        if (menu == null || context == null) {
            return;
        }

        MenuItem menuItem;
        // Set grid size radio buttons to correct value from settings
        int gridSize = PdfViewCtrlSettingsManager.getGridSize(context, PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_RECENT_FILES);
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
        if (item.getItemId() == R.id.action_clear_recent_list) {
            if (mAdapter != null && mAdapter.getItemCount() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.dialog_clear_recent_list_message)
                    .setTitle(R.string.dialog_clear_recent_list_title)
                    .setCancelable(true)
                    .setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Activity activity = getActivity();
                            if (activity == null) {
                                return;
                            }
                            getRecentFilesManager().clearFiles(activity);
                            reloadFileInfoList();
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
            FileTypeFilterPopupWindow fileTypeFilterListPopup = new FileTypeFilterPopupWindow(activity, this.getView(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_RECENT_FILES);

            fileTypeFilterListPopup.showAtLocation(this.getView(), Gravity.TOP | (Utils.isRtlLayout(getContext()) ? Gravity.LEFT : Gravity.RIGHT), 15, 75);
            fileTypeFilterListPopup.setFileTypeChangedListener(new FileTypeFilterPopupWindow.FileTypeChangedListener() {
                @Override
                public void filterTypeChanged(int index, boolean isOn) {
                    mAdapter.getDerivedFilter().setFileTypeEnabledInFilter(index, isOn);
                    updateFileListFilter();
                }
            });
        }

        return handled;
    }

    protected RecentAdapter getAdapter() {
        return new RecentAdapter(getActivity(), mFileInfoList, mFileListLock,
            mSpanCount, this, mItemSelectionHelper);
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

    private void updateFileListFilter() {
        if (mAdapter != null) {
            mAdapter.getFilter().filter("");
        }
    }

    public void updateSpanCount(int count) {
        if (mSpanCount != count) {
            PdfViewCtrlSettingsManager.updateGridSize(getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_RECENT_FILES, count);
        }
        mSpanCount = count;
        updateGridMenuState(mOptionsMenu);
        mRecyclerView.updateSpanCount(count);
    }

    @Override
    public void onExternalFileRenamed(ExternalFileInfo oldFile, ExternalFileInfo newFile) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        finishActionMode();
        hideFileInfoDrawer();

        FileInfo oldFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, oldFile.getUri().toString(),
            oldFile.getFileName(), false, 1);
        FileInfo newFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, newFile.getUri().toString(),
            newFile.getFileName(), false, 1);
        getRecentFilesManager().updateFile(activity, oldFileInfo, newFileInfo);
        getFavoriteFilesManager().updateFile(activity, oldFileInfo, newFileInfo);
        try {
            PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                oldFile.getAbsolutePath(), newFile.getAbsolutePath(), newFile.getFileName());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        reloadFileInfoList();
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

    }

    @Override
    public void onExternalFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {

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
        getRecentFilesManager().updateFile(activity, oldFile, newFile);
        getFavoriteFilesManager().updateFile(activity, oldFile, newFile);
        try {
            PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                oldFile.getAbsolutePath(), newFile.getAbsolutePath(), newFile.getFileName());
            // update user bookmarks
            BookmarkManager.updateUserBookmarksFilePath(activity, oldFile.getAbsolutePath(), newFile.getAbsolutePath());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        Utils.safeNotifyDataSetChanged(mAdapter);

        reloadFileInfoList();

        // delete old file in cache, add new file to cache
        new FileManager.ChangeCacheFileTask(oldFile, newFile, mCacheLock).execute();
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
    public void onFileMoved(Map<FileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {

    }

    @Override
    public void onFolderCreated(FileInfo rootFolder, FileInfo newFolder) {

    }

    @Override
    public void onFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {

    }

    @Override
    public void onFileChanged(String path, int event) {

    }

    @Override
    public void onFileClicked(FileInfo fileInfo) {
        switch (fileInfo.getType()) {
            case BaseFileInfo.FILE_TYPE_FILE:
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                    AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_RECENT));
                mCallbacks.onFileSelected(fileInfo.getFile(), "");
                break;
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
                if (!Utils.isNullOrEmpty(fileInfo.getAbsolutePath())) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                        AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_RECENT));
                    mCallbacks.onExternalFileSelected(fileInfo.getAbsolutePath(), "");
                }
                break;
            case BaseFileInfo.FILE_TYPE_EDIT_URI:
                if (!Utils.isNullOrEmpty(fileInfo.getAbsolutePath())) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                        AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_RECENT));
                    mCallbacks.onEditUriSelected(fileInfo.getAbsolutePath());
                }
                break;
            case BaseFileInfo.FILE_TYPE_OFFICE_URI:
                if (!Utils.isNullOrEmpty(fileInfo.getAbsolutePath())) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                        AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_RECENT));
                    mCallbacks.onOfficeUriSelected(Uri.parse(fileInfo.getAbsolutePath()));
                }
                break;
        }
    }

    // TODO move it to {@link com.pdftron.demo.asynctask}
    private class PopulateFileInfoListTask extends CustomAsyncTask<Void, Void, Void> {

        private List<FileInfo> fileInfoList = new ArrayList<>();
        private List<FileInfo> filesToRemove = new ArrayList<>();

        PopulateFileInfoListTask(Context context) {
            super(context);
        }

        @Override
        protected void onPreExecute() {
            if (mFirstRun) {
                // We already started with the progress bar visible, so no need
                // to show it again.
                mFirstRun = false;
            } else {
                if (mProgressBarView != null) {
                    mProgressBarView.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            fileInfoList.addAll(getRecentFilesManager().getFiles(getContext()));

            for (FileInfo fileInfo : fileInfoList) {
                if (fileInfo != null) {
                    boolean fileExists = true;
                    if (fileInfo.getFile() != null) {
                        // Check if the file exists
                        try {
                            fileExists = fileInfo.getFile().exists();
                        } catch (Exception ignored) {
                        }
                    } else if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                        // Check if the external file exists
                        Uri uri = Uri.parse(fileInfo.getAbsolutePath());
                        fileExists = Utils.uriHasReadPermission(getContext(), uri);
                    }
                    if (!fileExists) { // Flag file for removal if it does not exist
                        filesToRemove.add(fileInfo);
                    }
                }
            }
            // Commit any changes to the manager
            if (filesToRemove.size() > 0) {
                getRecentFilesManager().removeFiles(getContext(), filesToRemove);
                fileInfoList.removeAll(filesToRemove);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(Void result) {
            if (mAdapter == null || getContext() == null) {
                return;
            }

            synchronized (mFileListLock) {
                mFileInfoList.clear();
                mFileInfoList.addAll(fileInfoList);
            }

            if (mEmptyTextView != null) {
                if (fileInfoList.isEmpty()) {
                    mEmptyText = getString(R.string.textview_empty_recent_list);
                    mEmptyTextView.setText(mEmptyText);
                    mEmptyTextView.setVisibility(View.VISIBLE);
                } else {
                    mEmptyTextView.setVisibility(View.GONE);
                }
            }

            if (null != mProgressBarView) {
                mProgressBarView.setVisibility(View.GONE);
            }

            updateFileListFilter();
        }

        @Override
        protected void onCancelled() {
            if (mProgressBarView != null) {
                mProgressBarView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onLocalFolderSelected(int requestCode, Object object, File folder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (requestCode == RequestCode.SELECT_BLANK_DOC_FOLDER) {
            PDFDoc doc = null;
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
                String filePath = Utils.getFileNameNotInUse(documentFile.getAbsolutePath());
                if (Utils.isNullOrEmpty(filePath)) {
                    CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    return;
                }

                documentFile = new File(filePath);

                doc = mCreatedDoc;
                doc.save(documentFile.getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null);
                String toastMsg = getString(R.string.dialog_create_new_document_filename_success) + filePath;
                CommonToast.showText(getActivity(), toastMsg, Toast.LENGTH_LONG);

                FileInfo newFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, documentFile);
                new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), newFileInfo, mCacheLock).execute();

                if (mCallbacks != null) {
                    mCallbacks.onFileSelected(documentFile, "");
                }

                finishActionMode();
            } catch (Exception e) {
                CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                Utils.closeQuietly(doc);
            }
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

                    FileInfo newFileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, documentFile);
                    new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), newFileInfo, mCacheLock).execute();

                    if (mCallbacks != null) {
                        mCallbacks.onFileSelected(documentFile, "");
                    }
                }

                finishActionMode();
            } catch (FileNotFoundException e) {
                CommonToast.showText(activity, getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (Exception e) {
                CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (OutOfMemoryError oom) {
                com.pdftron.demo.utils.MiscUtils.manageOOM(activity);
                CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
            }

            // cleanup the image if it is from camera
            if (mIsCamera) {
                FileUtils.deleteQuietly(new File(mImageFilePath));
            }
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
            new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), newFileInfo, mCacheLock).execute();
            performMerge(newFileInfo);
        }
    }

    protected void performMerge(FileInfo newFileInfo) {
        FileManager.merge(getActivity(), mMergeFileList, mMergeTempFileList, newFileInfo, mFileManagementListener);
    }

    @Override
    public void onExternalFolderSelected(int requestCode, Object object, ExternalFileInfo folder) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

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
                CommonToast.showText(activity, getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (Exception e) {
                CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (OutOfMemoryError oom) {
                com.pdftron.demo.utils.MiscUtils.manageOOM(activity);
                CommonToast.showText(activity, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
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
        } else if (requestCode == RequestCode.MERGE_FILE_LIST) {
            boolean hasExtension = FilenameUtils.isExtension(mDocumentTitle, "pdf");
            if (!hasExtension) {
                mDocumentTitle = mDocumentTitle + ".pdf";
            }
            String fileName = Utils.getFileNameNotInUse(folder, mDocumentTitle);
            if (folder == null || Utils.isNullOrEmpty(fileName)) {
                CommonToast.showText(getActivity(), R.string.dialog_merge_error_message_general, Toast.LENGTH_SHORT);
                return;
            }
            final ExternalFileInfo file = folder.createFile("application/pdf", fileName);
            if (file == null) {
                return;
            }
            FileInfo targetFile = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, file.getAbsolutePath(), file.getFileName(), false, 1);
            new FileManager.ChangeCacheFileTask(new ArrayList<FileInfo>(), targetFile, mCacheLock).execute();
            performMerge(targetFile);
        }
    }

    @Override
    public void onMergeConfirmed(ArrayList<FileInfo> filesToMerge, ArrayList<FileInfo> filesToDelete, String title) {
        mDocumentTitle = title;
        mMergeFileList = filesToMerge;
        mMergeTempFileList = filesToDelete;
        // Launch folder picker
        FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MERGE_FILE_LIST,
            R.string.dialog_merge_save_location, Environment.getExternalStorageDirectory());
        dialogFragment.setLocalFolderListener(RecentViewFragment.this);
        dialogFragment.setExternalFolderListener(RecentViewFragment.this);
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
        }
    }

    @Override
    public void onPreLaunchViewer() {

    }

    @Override
    public void onDataChanged() {
        if (isAdded()) {
            reloadFileInfoList();
        } // otherwise it will be reloaded in resumeFragment
    }

    @Override
    public void onProcessNewIntent() {
        finishActionMode();
    }

    @Override
    public void onDrawerOpened() {
        finishActionMode();
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
        if (mFileInfoDrawer != null) {
            // Hide file info drawer
            hideFileInfoDrawer();
            handled = true;
        } else if (mActionMode != null) {
            finishActionMode();
            handled = true;
        }
        return handled;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public void onShowFileInfo(int position) {
        if (mFileUtilCallbacks != null) {
            mSelectedFile = mAdapter.getItem(position);
            mFileInfoDrawer = mFileUtilCallbacks.showFileInfoDrawer(this);
        }
    }

    private void resumeFragment() {
        reloadFileInfoList();

        if (mRecentViewFragmentListener != null) {
            mRecentViewFragmentListener.onRecentShown();
        }
        updateSpanCount(mSpanCount);
    }

    private void pauseFragment() {
        if (mPopulateFileInfoListTask != null) {
            mPopulateFileInfoListTask.cancel(true);
        }

        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.cleanupResources();
        }

        if (mRecentViewFragmentListener != null) {
            mRecentViewFragmentListener.onRecentHidden();
        }
    }

    protected FileManagementListener mFileManagementListener = new FileManagementListener() {
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
        public void onFileMoved(Map<FileInfo, Boolean> filesMoved, ExternalFileInfo targetFolder) {

        }

        @Override
        public void onFolderCreated(FileInfo rootFolder, FileInfo newFolder) {

        }

        @Override
        public void onFileMerged(ArrayList<FileInfo> mergedFiles, ArrayList<FileInfo> filesToDelete, FileInfo newFile) {
            finishActionMode();
            hideFileInfoDrawer();
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
        public void onFileChanged(String path, int event) {

        }

        @Override
        public void onFileClicked(FileInfo fileInfo) {

        }
    };

    protected FileInfoDrawerHelper mFileInfoDrawerHelper = new FileInfoDrawerHelper();

    protected class FileInfoDrawerHelper {
        MenuItem itemFavorite;
        MenuItem itemRemove;
        MenuItem itemShare;
        MenuItem itemRename;

        ExternalFileInfo externalFileInfo;

        int mPageCount;
        String mAuthor;
        String mTitle;
        String mProducer;
        String mCreator;

        public ThumbnailWorker mThumbnailWorker;
        WeakReference<ImageViewTopCrop> mImageViewReference;

        private void cancelAllThumbRequests() {
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

            PDFDoc doc = null;
            SecondaryFileFilter filter = null;
            try {
                switch (mSelectedFile.getType()) {
                    case BaseFileInfo.FILE_TYPE_FILE:
                        doc = new PDFDoc(mSelectedFile.getAbsolutePath());
                        doc.initSecurityHandler();
                        loadDocInfo(doc);
                        break;
                    case BaseFileInfo.FILE_TYPE_EXTERNAL:
                        if (getExternalFile() != null && getExternalFile().isDirectory()) {
                            try {
                                filter = new SecondaryFileFilter(getActivity(), getExternalFile().getUri());
                                doc = new PDFDoc(filter);
                                doc.initSecurityHandler();
                                loadDocInfo(doc);
                            } catch (Exception e) {
                                mPageCount = -1;
                                mAuthor = null;
                                mTitle = null;
                                mCreator = null;
                                mProducer = null;
                            }
                        } else {
                            mPageCount = -1;
                            mAuthor = null;
                            mTitle = null;
                            mProducer = null;
                            mCreator = null;
                        }
                        break;
                    default:
                        mPageCount = -1;
                        mAuthor = null;
                        mTitle = null;
                        mCreator = null;
                        mProducer = null;
                        break;
                }
            } catch (PDFNetException e) {
                mPageCount = -1;
                mAuthor = null;
                mTitle = null;
                mCreator = null;
                mProducer = null;
            } finally {
                Utils.closeQuietly(doc, filter);
            }

            if (mSelectedFile.getType() == BaseFileInfo.FILE_TYPE_FILE || (mSelectedFile.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL && getExternalFile() != null &&
                !getExternalFile().isDirectory())) {
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
            }

            switch (mSelectedFile.getType()) {
                case BaseFileInfo.FILE_TYPE_FILE:
                    if (textBodyBuilder.length() > 0) {
                        textBodyBuilder.append("<br>");
                    }
                    // Parent Directory
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
                case BaseFileInfo.FILE_TYPE_EXTERNAL:
                    if (getExternalFile() != null) {
                        if (!getExternalFile().isDirectory()) {
                            try {
                                // Directory
                                textBodyBuilder.append(res.getString(R.string.file_info_document_path, getExternalFile().getParentRelativePath() + "/" + getExternalFile().getFileName()));
                                textBodyBuilder.append("<br>");
                            } catch (NullPointerException ignored) {

                            }
                            // Size info
                            textBodyBuilder.append(res.getString(R.string.file_info_document_size, getExternalFile().getSizeInfo()));
                            textBodyBuilder.append("<br>");
                            // Date modified
                            textBodyBuilder.append(res.getString(R.string.file_info_document_date_modified,
                                DateFormat.getInstance().format(new Date(getExternalFile().getRawModifiedDate()))));
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
                                // Directory
                                textBodyBuilder.append(res.getString(R.string.file_info_document_path, getExternalFile().getParentRelativePath() + "/" + getExternalFile().getFileName()));
                                textBodyBuilder.append("<br>");
                            } catch (NullPointerException ignored) {

                            }
                            // Size info: x files, y folders
                            int[] externalFileFolderCount = getExternalFile().getFileCount();
                            String externalSizeInfo = res.getString(R.string.dialog_folder_info_size, externalFileFolderCount[0], externalFileFolderCount[1]);
                            textBodyBuilder.append(res.getString(R.string.file_info_document_folder_contains, externalSizeInfo));
                            textBodyBuilder.append("<br>");
                            // Date modified
                            textBodyBuilder.append(res.getString(R.string.file_info_document_date_modified,
                                DateFormat.getInstance().format(new Date(getExternalFile().getRawModifiedDate()))));
                        }
                    }
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
                mCreator = null;
                mProducer = null;
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(doc);
                }
            }
        }

        private ExternalFileInfo getExternalFile() {
            if (externalFileInfo == null && mSelectedFile != null) {
                externalFileInfo = new ExternalFileInfo(getActivity());
                externalFileInfo.setUri(Uri.parse(mSelectedFile.getAbsolutePath()));
                externalFileInfo.initFields();
            }
            return externalFileInfo;
        }
    }

    ThumbnailWorker.ThumbnailWorkerListener mThumbnailWorkerListener = new ThumbnailWorker.ThumbnailWorkerListener() {
        @Override
        public void onThumbnailReady(int result, int position, String iconPath, String identifier) {
            ImageViewTopCrop imageView = (mFileInfoDrawerHelper.mImageViewReference != null) ?
                mFileInfoDrawerHelper.mImageViewReference.get() : null;
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
            if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_NOT_FOUNT &&
                mSelectedFile.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                // create this file instead
                mFileInfoDrawerHelper.mThumbnailWorker.tryLoadImageFromFilter(position, mSelectedFile.getIdentifier(), mSelectedFile.getAbsolutePath());
                return;
            }

            if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR || result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PACKAGE_ERROR) {
                // Thumbnail has been generated before, and a placeholder icon should be used
                int errorRes = Utils.getResourceDrawable(getContext(), getResources().getString(R.string.thumb_error_res_name));
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setImageResource(errorRes);
            } else if (mFileInfoDrawerHelper.mThumbnailWorker != null) {
                // adds path to local cache for later access
                ThumbnailPathCacheManager.getInstance().putThumbnailPath(mSelectedFile.getAbsolutePath(),
                    iconPath, mFileInfoDrawerHelper.mThumbnailWorker.getMinXSize(),
                    mFileInfoDrawerHelper.mThumbnailWorker.getMinYSize());

                imageView.setScaleType(ImageView.ScaleType.MATRIX);
                mFileInfoDrawerHelper.mThumbnailWorker.tryLoadImageWithPath(position, mSelectedFile.getAbsolutePath(), iconPath, imageView);
            }
        }
    };

    @Override
    public void onFilterResultsPublished(int resultCode) {
        // Do nothing
    }

    public void setRecentViewFragmentListener(RecentViewFragmentListener listener) {
        mRecentViewFragmentListener = listener;
    }

    public void saveCreatedDocument(PDFDoc doc, String title) {
        mCreatedDocumentTitle = title;
        mCreatedDoc = doc;
        // launch folder picker
        FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.SELECT_BLANK_DOC_FOLDER, Environment.getExternalStorageDirectory());
        dialogFragment.setLocalFolderListener(RecentViewFragment.this);
        dialogFragment.setExternalFolderListener(RecentViewFragment.this);
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

                    mIsCamera = ViewerUtils.isImageFromCamera(imageIntent);
                    mImageFilePath = ViewerUtils.getImageFilePath(imageIntent);
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
                    dialogFragment.setLocalFolderListener(this);
                    dialogFragment.setExternalFolderListener(this);
                    dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
                    FragmentManager fragmentManager = getFragmentManager();
                    if (fragmentManager != null) {
                        dialogFragment.show(fragmentManager, "create_document_folder_picker_dialog");
                    }

                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CREATE_NEW,
                        AnalyticsParam.createNewParam(mIsCamera ? AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_CAMERA : AnalyticsHandlerAdapter.CREATE_NEW_ITEM_PDF_FROM_IMAGE,
                            AnalyticsHandlerAdapter.SCREEN_RECENT));
                } catch (FileNotFoundException e) {
                    CommonToast.showText(getContext(), getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } catch (Exception e) {
                    CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }
    }
}
