//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.pdftron.demo.R;
import com.pdftron.demo.dialog.MergeDialogFragment;
import com.pdftron.demo.navigation.adapter.BaseFileAdapter;
import com.pdftron.demo.navigation.callbacks.FilePickerCallbacks;
import com.pdftron.demo.navigation.callbacks.OnPdfFileSharedListener;
import com.pdftron.demo.utils.AddDocPdfHelper;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.FavoriteFilesManager;
import com.pdftron.pdf.utils.FileInfoManager;
import com.pdftron.pdf.utils.RecentFilesManager;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

public class FileBrowserViewFragment extends ToolbarFragment implements ActionMode.Callback {
    public static final String KEY_CURRENT_FILE = "key_current_file";

    protected FilePickerCallbacks mCallbacks;
    protected OnPdfFileSharedListener mOnPdfFileSharedListener;
    protected AddDocPdfHelper mAddDocPdfHelper;
    protected final Object mFileListLock = new Object();
    protected final Object mCacheLock = new Object();
    protected ActionMode mActionMode;

    protected String mCurrentFile;

    public void setFilePickerCallbacks(FilePickerCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCurrentFile = savedInstanceState.getString(KEY_CURRENT_FILE);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCurrentFile != null) {
            outState.putString(KEY_CURRENT_FILE, mCurrentFile);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallbacks = (FilePickerCallbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + e.getClass().toString());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    public void setOnPdfFileSharedListener(OnPdfFileSharedListener listener) {
        mOnPdfFileSharedListener = listener;
    }

    protected void handleMultipleFilesSelected(ArrayList<FileInfo> fileInfoList, int screenId) {
        MergeDialogFragment mergeDialog = getMergeDialogFragment(fileInfoList, screenId);
        mergeDialog.initParams(new MergeDialogFragment.MergeDialogFragmentListener() {
            @Override
            public void onMergeConfirmed(ArrayList<FileInfo> filesToMerge, ArrayList<FileInfo> filesToDelete, String title) {
                mAddDocPdfHelper.handleMergeConfirmed(filesToMerge, filesToDelete, title);
            }
        });
        mergeDialog.setStyle(DialogFragment.STYLE_NO_FRAME, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            mergeDialog.show(fragmentManager, "merge_dialog");
        }
    }

    protected MergeDialogFragment getMergeDialogFragment(ArrayList<FileInfo> files, int screenId) {
        return MergeDialogFragment.newInstance(files, screenId);
    }

    protected FileInfoManager getRecentFilesManager() {
        return RecentFilesManager.getInstance();
    }

    protected FileInfoManager getFavoriteFilesManager() {
        return FavoriteFilesManager.getInstance();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Fix transparent status-bar color from going black
        Activity activity = getActivity();
        if (activity == null || !Utils.isLollipop()) {
            return false;
        }

        Window w = activity.getWindow();
        if (w != null) {
            w.setStatusBarColor(Utils.getPrimaryDarkColor(activity));
        }

        return false;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        Activity activity = getActivity();
        if (activity == null || !Utils.isLollipop()) {
            return;
        }
        Window w = activity.getWindow();
        if (w != null) {
            w.setStatusBarColor(getResources().getColor(android.R.color.transparent));
        }
    }

    public void setCurrentFile(String filename) {
        mCurrentFile = filename;
    }

    protected void scrollToCurrentFile(@NonNull RecyclerView recyclerView) {
        if (recyclerView.getAdapter() == null
            || null == mCurrentFile) {
            return;
        }
        BaseFileAdapter adapter = (BaseFileAdapter) recyclerView.getAdapter();
        if (!Utils.isNullOrEmpty(mCurrentFile) && adapter != null && adapter.getItemCount() > 0) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                BaseFileInfo fileInfo = adapter.getItem(i);
                if (fileInfo == null) {
                    continue;
                }
                if (mCurrentFile.equals(fileInfo.getFileName())) {
                    recyclerView.scrollToPosition(i);
                    break;
                }
            }
            mCurrentFile = null;
        }
    }

    /**
     * Hides the soft keyboard. This should usually be called if in search mode
     * and wants to hide the soft keyboard due to for example open a file in
     * the document viewer
     */
    protected void hideSoftKeyboard(
    ) {

        Activity activity = getActivity();
        View view = getView();
        if (activity == null || view == null) {
            return;
        }
        Utils.hideSoftKeyboard(activity, view);

    }

}
