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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
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

import com.pdftron.common.PDFNetException;
import com.pdftron.demo.R;
import com.pdftron.demo.R2;
import com.pdftron.demo.dialog.FilePickerDialogFragment;
import com.pdftron.demo.dialog.MergeDialogFragment;
import com.pdftron.demo.navigation.adapter.FavoriteAdapter;
import com.pdftron.demo.navigation.callbacks.FileManagementListener;
import com.pdftron.demo.navigation.callbacks.FileUtilCallbacks;
import com.pdftron.demo.navigation.callbacks.MainActivityListener;
import com.pdftron.demo.utils.ExternalFileManager;
import com.pdftron.demo.utils.FileManager;
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
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.ItemSelectionHelper;
import com.pdftron.pdf.widget.recyclerview.ItemTouchHelperCallback;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
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

public class FavoritesViewFragment extends FileBrowserViewFragment implements
    MainActivityListener,
    FavoriteAdapter.AdapterListener,
    FileManagementListener,
    ExternalFileManager.ExternalFileManagementListener,
    FilePickerDialogFragment.LocalFolderListener,
    FilePickerDialogFragment.ExternalFolderListener,
    MergeDialogFragment.MergeDialogFragmentListener,
    ActionMode.Callback,
    FileInfoDrawer.Callback {

    private static final String TAG = FavoritesViewFragment.class.getName();

    @BindView(R2.id.recycler_view)
    SimpleRecyclerView mRecyclerView;
    @BindView(android.R.id.empty)
    View mEmptyView;
    @BindView(R2.id.empty_text_view)
    TextView mEmptyTextView;
    @BindView(R2.id.progress_bar_view)
    ProgressBar mProgressBarView;

    private Unbinder unbinder;

    protected ArrayList<FileInfo> mFileInfoList = new ArrayList<>();
    protected ArrayList<FileInfo> mFileInfoSelectedList = new ArrayList<>();
    protected FileInfo mSelectedFile;

    private FileUtilCallbacks mFileUtilCallbacks;

    protected FavoriteAdapter mAdapter;
    private FileInfoDrawer mFileInfoDrawer;
    private ItemTouchHelper mItemTouchHelper;
    private PopulateFileInfoListTask mPopulateFileInfoListTask;
    private Menu mOptionsMenu;
    private boolean mFirstRun;
    private String mDocumentTitle;
    private SpannableString mEmptyFavoriteText;
    private SpannableString mEmptyText;

    private FavoritesViewFragmentListener mFavoritesViewFragmentListener;

    protected ArrayList<FileInfo> mMergeFileList;
    protected ArrayList<FileInfo> mMergeTempFileList;

    protected int mSpanCount;
    protected ItemSelectionHelper mItemSelectionHelper;

    protected MenuItem itemRemove;
    protected MenuItem itemMerge;
    protected MenuItem itemShare;
    protected MenuItem itemRename;

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (super.onCreateActionMode(mode, menu)) {
            return true;
        }

        mode.getMenuInflater().inflate(R.menu.cab_fragment_favorite_view, menu);

        itemRemove = menu.findItem(R.id.action_favorite_list_remove);
        itemMerge = menu.findItem(R.id.cab_file_merge);
        itemShare = menu.findItem(R.id.cab_file_share);
        itemRename = menu.findItem(R.id.cab_file_rename);
        itemRemove.setIcon(null); // Remove icon
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }
        if (itemRename != null) {
            if (mFileInfoSelectedList.size() > 1) {
                // Multiple selection of files and/or folders
                itemRename.setVisible(false);
            } else {
                if (!mFileInfoSelectedList.isEmpty() && mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_FOLDER ||
                    mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_FILE) {
                    boolean isSDCardFile = Utils.isSdCardFile(activity, mFileInfoSelectedList.get(0).getFile());
                    String DBX_CACHE_PATH = "/Android/data/com.dropbox.android/";
                    boolean isDbxFile = mFileInfoSelectedList.get(0).getAbsolutePath().contains(DBX_CACHE_PATH);
                    if (isSDCardFile || isDbxFile) {
                        itemRename.setVisible(false);
                    } else {
                        itemRename.setVisible(true);
                    }
                } else if (!mFileInfoSelectedList.isEmpty() && mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                    mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
                    itemRename.setVisible(true);
                } else {
                    itemRename.setVisible(false);
                }
            }
        }

        if (itemShare != null) {
            itemShare.setVisible(true);
        }
        if (itemRemove != null) {
            itemRemove.setVisible(true);
        }
        if (itemMerge != null) {
            itemMerge.setVisible(true);
        }

        for (FileInfo file : mFileInfoSelectedList) {
            if (file.getType() == BaseFileInfo.FILE_TYPE_FOLDER) {
                if (itemRemove != null) {
                    itemMerge.setVisible(false);
                }
                if (itemShare != null) {
                    itemShare.setVisible(false);
                }
                break;
            }
        }

        mode.setTitle(Utils.getLocaleDigits(Integer.toString(mFileInfoSelectedList.size())));
        // Ensure items are always shown
        if (itemRemove != null) {
            itemRemove.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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
                FileManager.rename(activity, mFileInfoSelectedList.get(0).getFile(), FavoritesViewFragment.this);
            } else if (mFileInfoSelectedList.get(0).getType() == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                mSelectedFile.getType() == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
                ExternalFileInfo externalFileInfo = Utils.buildExternalFile(activity,
                    Uri.parse(mFileInfoSelectedList.get(0).getAbsolutePath()));
                if (externalFileInfo != null) {
                    ExternalFileManager.rename(activity, externalFileInfo, FavoritesViewFragment.this);
                }
            }
        }
        if (item.getItemId() == R.id.action_favorite_list_remove) {
            getFavoriteFilesManager().removeFiles(activity, mFileInfoSelectedList);
            finishActionMode();
            reloadFileInfoList();
        }
        if (item.getItemId() == R.id.cab_file_merge) {
            // Create and show file merge dialog-fragment
            MergeDialogFragment mergeDialog = getMergeDialogFragment(mFileInfoSelectedList, AnalyticsHandlerAdapter.SCREEN_FAVORITES);
            mergeDialog.initParams(FavoritesViewFragment.this);
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
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_FILEBROWSER, "Item share clicked", AnalyticsHandlerAdapter.LABEL_FAVORITES);
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
            mFileInfoDrawerHelper.mThumbnailWorker = new ThumbnailWorker(getActivity(), dimensions.x, dimensions.y, null);
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
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE); // Adjust scale type for ic_folder_black_24dp
                imageView.setImageResource(R.drawable.ic_folder_black_24dp);
                int folderColorRes = Utils.getResourceColor(activity, getResources().getString(R.string.folder_color));
                if (folderColorRes == 0) {
                    folderColorRes = android.R.color.black;
                }
                imageView.getDrawable().mutate().setColorFilter(getResources().getColor(folderColorRes), PorterDuff.Mode.SRC_IN);
                break;
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
                if (mFileInfoDrawerHelper.getExternalFile() != null && mFileInfoDrawerHelper.getExternalFile().isDirectory()) {
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE); // Adjust scale type for ic_folder_black_24dp
                    imageView.setImageResource(R.drawable.ic_folder_black_24dp);
                    folderColorRes = Utils.getResourceColor(activity, getResources().getString(R.string.folder_color));
                    if (folderColorRes == 0) {
                        folderColorRes = android.R.color.black;
                    }
                    imageView.getDrawable().mutate().setColorFilter(getResources().getColor(folderColorRes), PorterDuff.Mode.SRC_IN);
                } else if (mSelectedFile.isSecured() || mSelectedFile.isPackage()) {
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    int errorRes = Utils.getResourceDrawable(activity, getResources().getString(R.string.thumb_error_res_name));
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
        activity.getMenuInflater().inflate(R.menu.cab_fragment_favorite_view, menu);

        return true;
    }

    @Override
    public boolean onPrepareDrawerMenu(FileInfoDrawer drawer, Menu menu) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }
        boolean changed = false;
        if (menu != null) {
            int selectedFileType = mSelectedFile.getType();
            MenuItem menuItem = menu.findItem(R.id.action_favorite_list_remove);
            if (menuItem != null) {
                if (mFileInfoDrawerHelper.removeOnHide) {
                    menuItem.setTitle(activity.getString(R.string.action_add_to_favorites));
                    menuItem.setTitleCondensed(activity.getString(R.string.action_favorite));
                    menuItem.setIcon(R.drawable.ic_star_outline_grey600_24dp);
                } else {
                    menuItem.setTitle(activity.getString(R.string.undo_redo_annot_remove));
                    menuItem.setTitleCondensed(activity.getString(R.string.undo_redo_annot_remove));
                    menuItem.setIcon(R.drawable.ic_star_white_24dp);
                }
                changed = true;
            }
            menuItem = menu.findItem(R.id.cab_file_share);
            if (menuItem != null) {
                if (selectedFileType == BaseFileInfo.FILE_TYPE_FILE
                    || selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL
                    || selectedFileType == BaseFileInfo.FILE_TYPE_EDIT_URI
                    || selectedFileType == BaseFileInfo.FILE_TYPE_OFFICE_URI) {
                    menuItem.setVisible(true);
                } else {
                    menuItem.setVisible(false);
                }
                changed = true;
            }

            menuItem = menu.findItem(R.id.cab_file_rename);
            if (menuItem != null) {
                if (selectedFileType == BaseFileInfo.FILE_TYPE_FOLDER ||
                    selectedFileType == BaseFileInfo.FILE_TYPE_FILE) {
                    boolean isSDCardFile = Utils.isSdCardFile(activity, mSelectedFile.getFile());
                    String DBX_CACHE_PATH = "/Android/data/com.dropbox.android/";
                    boolean isDbxFile = mSelectedFile.getAbsolutePath().contains(DBX_CACHE_PATH);
                    if (isSDCardFile || isDbxFile) {
                        menuItem.setVisible(false);
                    } else {
                        menuItem.setVisible(true);
                    }
                } else if (selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                    selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
                    menuItem.setVisible(true);
                } else {
                    menuItem.setVisible(false);
                }
            }
        }
        return changed;
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
                FileManager.rename(activity, mSelectedFile.getFile(), FavoritesViewFragment.this);
            } else if (selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL ||
                selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
                ExternalFileInfo externalFileInfo = Utils.buildExternalFile(activity,
                    Uri.parse(mSelectedFile.getAbsolutePath()));
                ExternalFileManager.rename(activity, externalFileInfo, FavoritesViewFragment.this);
            }
        }
        if (menuItem.getItemId() == R.id.action_favorite_list_remove) {
            if (!mFileInfoDrawerHelper.removeOnHide) {
                // This file will be removed from the drawer when it is hidden
                mFileInfoDrawerHelper.removeOnHide = true;
                CommonToast.showText(activity,
                    getString(R.string.file_removed_from_favorites, mSelectedFile.getName()),
                    Toast.LENGTH_SHORT);
            } else {
                // This file will no longer be removed from the drawer when it is hidden
                mFileInfoDrawerHelper.removeOnHide = false;
                CommonToast.showText(activity,
                    getString(R.string.file_added_to_favorites, mSelectedFile.getName()),
                    Toast.LENGTH_SHORT);
            }
            drawer.invalidate();
        }
        if (menuItem.getItemId() == R.id.cab_file_merge) {
            // Create and show file merge dialog-fragment
            MergeDialogFragment mergeDialog = getMergeDialogFragment(new ArrayList<>(Collections.singletonList(mSelectedFile)), AnalyticsHandlerAdapter.SCREEN_FAVORITES);
            mergeDialog.initParams(FavoritesViewFragment.this);
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
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_FILEBROWSER, "Item share clicked", AnalyticsHandlerAdapter.LABEL_FAVORITES);
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
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mFileInfoDrawerHelper.cancelAllThumbRequests();

        if (mFileInfoDrawerHelper.removeOnHide && mSelectedFile != null) {
            getFavoriteFilesManager().removeFile(activity, mSelectedFile);
            reloadFileInfoList();
        }

        mFileInfoDrawerHelper.removeOnHide = false;
        mSelectedFile = null;
        mFileInfoDrawer = null;
    }

    public interface FavoritesViewFragmentListener {

        void onFavoritesShown();

        void onFavoritesHidden();
    }

    public static FavoritesViewFragment newInstance() {
        return new FavoritesViewFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.INSTANCE.LogV("LifeCycle", TAG + ".onCreate");

        super.onCreate(savedInstanceState);

        Context context = getContext();
        if (context == null) {
            return;
        }

        setRetainInstance(true);
        setHasOptionsMenu(true);
        FileManager.initCache(context);
        mFirstRun = true;

        // set up empty spannable string
        String secondLine = String.format(getString(R.string.textview_empty_favorite_list_second_part1), getString(R.string.app_name));
        String firstPart = getString(R.string.textview_empty_favorite_list)
            + "\n\n" + secondLine + " ";
        String secondPart = ". " + getString(R.string.textview_empty_favorite_list_second_part2) + " ";
        String total = firstPart + secondPart;
        int pixelSize = (int) Utils.convDp2Pix(context, 24);
        mEmptyFavoriteText = new SpannableString(total);
        Drawable drawable = getResources().getDrawable(R.drawable.info);
        drawable.mutate().setColorFilter(getResources().getColor(R.color.gray600), PorterDuff.Mode.SRC_IN);
        drawable.setBounds(0, 0, pixelSize, pixelSize);
        ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
        mEmptyFavoriteText.setSpan(span, firstPart.length() - 1, firstPart.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        drawable = getResources().getDrawable(R.drawable.star);
        drawable.mutate().setColorFilter(getResources().getColor(R.color.gray600), PorterDuff.Mode.SRC_IN);
        drawable.setBounds(0, 0, pixelSize, pixelSize);
        ImageSpan star_span = new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
        mEmptyFavoriteText.setSpan(star_span, total.length() - 1, total.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unbinder = ButterKnife.bind(this, view);

        // if we are here after PopulateFileInfoListTask is complete
        if (mEmptyText != null) {
            mEmptyTextView.setText(mEmptyText);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mEmptyView.setVisibility(View.GONE);
        }
        // since there are not many files to process, no need to show "loading files ..."

        mSpanCount = PdfViewCtrlSettingsManager.getGridSize(view.getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FAVOURITE_FILES);
        mRecyclerView.initView(mSpanCount);

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);

        mItemSelectionHelper = new ItemSelectionHelper();
        mItemSelectionHelper.attachToRecyclerView(mRecyclerView);
        mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_MULTIPLE);

        mAdapter = getAdapter();

        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(mAdapter, mSpanCount, false, false);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

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
                        mAdapter.getDerivedFilter().setFileTypeEnabledInFilterFromSettings(mRecyclerView.getContext(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FAVOURITE_FILES);
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
            public boolean onItemLongClick(RecyclerView parent, View view, final int position, long id) {
                FileInfo fileInfo = mAdapter.getItem(position);
                if (fileInfo == null) {
                    return false;
                }

                if (mActionMode == null) {
                    mFileInfoSelectedList.add(fileInfo);
                    mItemSelectionHelper.setItemChecked(position, true);

                    if (getActivity() instanceof AppCompatActivity) {
                        AppCompatActivity activity = (AppCompatActivity) getActivity();
                        mActionMode = activity.startSupportActionMode(FavoritesViewFragment.this);
                        if (mActionMode != null) {
                            mActionMode.invalidate();
                        }
                    }
                }

                // Start edit mode
                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
                        mItemTouchHelper.startDrag(holder);
                    }
                });
                return true;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_FAVORITES);
    }

    @Override
    public void onStop() {
        super.onStop();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_SCREEN_FAVORITES);
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
    public void onDestroy() {
        super.onDestroy();

        // cleanup previous resource
        if (null != mAdapter) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.cleanupResources();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
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

    private void updateFileListFilter() {
        if (mAdapter != null) {
            mAdapter.getFilter().filter("");
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
        inflater.inflate(R.menu.fragment_favorite_view, menu);
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
        int gridSize = PdfViewCtrlSettingsManager.getGridSize(context, PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FAVOURITE_FILES);
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
        if (item.getItemId() == R.id.action_clear_favorite_list) {
            if (mAdapter != null && mAdapter.getItemCount() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.dialog_clear_favorite_list_message)
                    .setTitle(R.string.dialog_clear_favorite_list_title)
                    .setCancelable(true)
                    .setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Activity activity = getActivity();
                            if (activity == null) {
                                return;
                            }
                            getFavoriteFilesManager().clearFiles(activity);
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
            FileTypeFilterPopupWindow fileTypeFilterListPopup = new FileTypeFilterPopupWindow(activity, getView(), PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FAVOURITE_FILES);

            fileTypeFilterListPopup.showAtLocation(getView(), Gravity.TOP | (Utils.isRtlLayout(activity) ? Gravity.LEFT : Gravity.RIGHT), 15, 75);
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

    protected FavoriteAdapter getAdapter() {
        return new FavoriteAdapter(getActivity(), mFileInfoList, mFileListLock,
            mSpanCount, this, mItemSelectionHelper);
    }

    protected void handleFileUpdated(FileInfo oldFile, FileInfo newFile) {
        FragmentActivity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        getRecentFilesManager().updateFile(activity, oldFile, newFile);
        getFavoriteFilesManager().updateFile(activity, oldFile, newFile);
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
            PdfViewCtrlSettingsManager.updateGridSize(context, PdfViewCtrlSettingsManager.KEY_PREF_SUFFIX_FAVOURITE_FILES, count);
        }
        mSpanCount = count;
        updateGridMenuState(mOptionsMenu);
        mRecyclerView.updateSpanCount(count);
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
        handleFileUpdated(oldFile, newFile);
        try {
            PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                oldFile.getAbsolutePath(), newFile.getAbsolutePath(), newFile.getFileName());
            // update user bookmarks
            BookmarkManager.updateUserBookmarksFilePath(activity, oldFile.getAbsolutePath(), newFile.getAbsolutePath());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        reloadFileInfoList();
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
    public void onFileClicked(@NonNull FileInfo fileInfo) {
        switch (fileInfo.getType()) {
            case BaseFileInfo.FILE_TYPE_FILE:
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                    AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_FAVORITES));
                mCallbacks.onFileSelected(fileInfo.getFile(), "");
                break;
            case BaseFileInfo.FILE_TYPE_FOLDER:
                mCallbacks.onFolderSelected(fileInfo.getAbsolutePath());
                break;
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
                if (!Utils.isNullOrEmpty(fileInfo.getAbsolutePath())) {
                    // Check if file is directory
                    String type = ExternalFileInfo.getUriMimeType(getActivity(), fileInfo.getAbsolutePath());
                    if (!Utils.isNullOrEmpty(type) && DocumentsContract.Document.MIME_TYPE_DIR.equals(type)) {
                        mCallbacks.onExternalFolderSelected(fileInfo.getAbsolutePath());
                    } else {
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                            AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_FAVORITES));
                        mCallbacks.onExternalFileSelected(fileInfo.getAbsolutePath(), "");
                    }
                }
                break;
            case BaseFileInfo.FILE_TYPE_EDIT_URI:
                if (!Utils.isNullOrEmpty(fileInfo.getAbsolutePath())) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                        AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_FAVORITES));
                    mCallbacks.onEditUriSelected(fileInfo.getAbsolutePath());
                }
                break;
            case BaseFileInfo.FILE_TYPE_OFFICE_URI:
                if (!Utils.isNullOrEmpty(fileInfo.getAbsolutePath())) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_OPEN_FILE,
                        AnalyticsParam.openFileParam(fileInfo, AnalyticsHandlerAdapter.SCREEN_FAVORITES));
                    mCallbacks.onOfficeUriSelected(Uri.parse(fileInfo.getAbsolutePath()));
                }
                break;
        }
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
        handleFileUpdated(oldFileInfo, newFileInfo);
        try {
            PdfViewCtrlTabsManager.getInstance().updatePdfViewCtrlTabInfo(activity,
                oldFile.getAbsolutePath(), newFile.getAbsolutePath(), newFile.getFileName());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        reloadFileInfoList();
        new FileManager.ChangeCacheFileTask(oldFileInfo, newFileInfo, mCacheLock).execute();
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

    // TODO move it to {@link com.pdftron.demo.asynctask}
    private class PopulateFileInfoListTask extends CustomAsyncTask<Void, Void, Void> {

        private List<FileInfo> fileInfoList;
        private List<FileInfo> filesToRemove;

        PopulateFileInfoListTask(Context context) {
            super(context);
            fileInfoList = new ArrayList<>();
            filesToRemove = new ArrayList<>();
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
            fileInfoList.addAll(getFavoriteFilesManager().getFiles(getContext()));
            for (FileInfo fileInfo : fileInfoList) {
                if (fileInfo != null) {
                    Logger.INSTANCE.LogD("LocalFolderViewFragment", "get fav file info: " + fileInfo.getAbsolutePath());
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
                    if (fileExists) {
                        // it may be hidden by LocalFolderViewFragment
                        fileInfo.setHidden(false);
                    } else { // Flag file for removal
                        filesToRemove.add(fileInfo);
                    }
                }
            }
            // Commit any changes to the manager
            if (filesToRemove.size() > 0) {
                getFavoriteFilesManager().removeFiles(getContext(), filesToRemove);
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

            if (mEmptyView != null && mEmptyTextView != null) {
                if (fileInfoList.isEmpty()) {
                    mEmptyText = mEmptyFavoriteText;
                    mEmptyTextView.setText(mEmptyFavoriteText);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    mEmptyView.setVisibility(View.GONE);
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
        if (requestCode == RequestCode.MERGE_FILE_LIST) {
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
        if (requestCode == RequestCode.MERGE_FILE_LIST) {
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
        dialogFragment.setLocalFolderListener(FavoritesViewFragment.this);
        dialogFragment.setExternalFolderListener(FavoritesViewFragment.this);
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

    @Override
    public void onFilterResultsPublished(int resultCode) {
        // Not used
    }

    @Override
    public void onFilesReordered() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        synchronized (this) {
            int count = mAdapter.getItemCount();
            ArrayList<FileInfo> reorderedFiles = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                FileInfo info = mAdapter.getItem(i);
                if (info != null) {
                    reorderedFiles.add(info);
                }
            }
            getFavoriteFilesManager().saveFiles(context, reorderedFiles);
        }
        finishActionMode();
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
            Logger.INSTANCE.LogD("Thumbnail", "Listener: onFileClicked");
        }
    };

    protected FileInfoDrawerHelper mFileInfoDrawerHelper = new FileInfoDrawerHelper();

    protected class FileInfoDrawerHelper {

        public int mPageCount;
        public String mAuthor;
        public String mTitle;
        public String mProducer;
        public String mCreator;

        public ExternalFileInfo externalFileInfo;
        public boolean removeOnHide = false;

        public ThumbnailWorker mThumbnailWorker;
        public WeakReference<ImageViewTopCrop> mImageViewReference;

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
            int selectedFileType = mSelectedFile.getType();

            PDFDoc doc = null;
            SecondaryFileFilter filter = null;
            try {
                switch (selectedFileType) {
                    case BaseFileInfo.FILE_TYPE_FILE:
                        doc = new PDFDoc(mSelectedFile.getAbsolutePath());
                        doc.initSecurityHandler();
                        loadDocInfo(doc);
                        break;
                    case BaseFileInfo.FILE_TYPE_EXTERNAL:
                        if (getExternalFile() != null && getExternalFile().isDirectory()) {
                            try {
                                filter = new SecondaryFileFilter(activity, getExternalFile().getUri());
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
                            mCreator = null;
                            mProducer = null;
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

            if (selectedFileType == BaseFileInfo.FILE_TYPE_FILE
                || (selectedFileType == BaseFileInfo.FILE_TYPE_EXTERNAL && getExternalFile() != null &&
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

            switch (selectedFileType) {
                case BaseFileInfo.FILE_TYPE_FILE:
                    if (textBodyBuilder.length() > 0) {
                        textBodyBuilder.append("<br>");
                    }
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
                }
            } catch (PDFNetException e) {
                mPageCount = -1;
                mAuthor = null;
                mTitle = null;
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
                    iconPath, mFileInfoDrawerHelper.mThumbnailWorker.getMinXSize(), mFileInfoDrawerHelper.mThumbnailWorker.getMinYSize());

                imageView.setScaleType(ImageView.ScaleType.MATRIX);
                mFileInfoDrawerHelper.mThumbnailWorker.tryLoadImageWithPath(position, mSelectedFile.getAbsolutePath(), iconPath, imageView);
            }
        }
    };

    private void resumeFragment() {
        reloadFileInfoList();

        if (mFavoritesViewFragmentListener != null) {
            mFavoritesViewFragmentListener.onFavoritesShown();
        }
    }

    private void pauseFragment() {
        if (mPopulateFileInfoListTask != null) {
            mPopulateFileInfoListTask.cancel(true);
        }

        if (mAdapter != null) {
            mAdapter.cancelAllThumbRequests(true);
            mAdapter.cleanupResources();
        }

        if (mFavoritesViewFragmentListener != null) {
            mFavoritesViewFragmentListener.onFavoritesHidden();
        }
    }

    public void setFavoritesViewFragmentListener(FavoritesViewFragmentListener listener) {
        mFavoritesViewFragmentListener = listener;
    }
}
