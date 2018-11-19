package com.pdftron.demo.app;
//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.pdftron.demo.R;
import com.pdftron.demo.dialog.DialogOpenUrl;
import com.pdftron.demo.dialog.FilePickerDialogFragment;
import com.pdftron.demo.utils.AppUtils;
import com.pdftron.pdf.config.PDFNetConfig;
import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.controls.DocumentActivity;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.RequestCode;

import java.io.File;

/**
 * SimpleReaderActivity is derived from {@link DocumentActivity}.
 * and is an all-in-one document reader and PDF editor. UI can be configured via {@link ViewerConfig class}.
 */
public class SimpleReaderActivity extends DocumentActivity {

    private static final String TAG = SimpleReaderActivity.class.getName();

    MenuItem mMenuOpenFile;
    MenuItem mMenuOpenUrl;

    /**
     * Opens a built-in sample document with default configuration.
     *
     * @param packageContext the context
     */
    public static void open(Context packageContext) {
        open(packageContext, null);
    }


    /**
     * Opens a built-in sample document with custom configuration.
     * @param packageContext the context
     * @param config the {@link ViewerConfig}
     */
    public static void open(Context packageContext, @Nullable ViewerConfig config) {
        Intent intent = new Intent(packageContext, SimpleReaderActivity.class);
        intent.putExtra(EXTRA_CONFIG, config);
        packageContext.startActivity(intent);
    }

    /**
     * Opens a file from Uri with empty password and default configuration.
     *
     * @param packageContext the context
     * @param fileUri        the file Uri
     */
    public static void openDocument(Context packageContext, Uri fileUri) {
        openDocument(packageContext, fileUri, "");
    }

    /**
     * Opens a file from Uri with empty password and custom configuration.
     *
     * @param packageContext the context
     * @param fileUri        the file Uri
     * @param config         the configuration
     */
    public static void openDocument(Context packageContext, Uri fileUri, @Nullable ViewerConfig config) {
        openDocument(packageContext, fileUri, "", config);
    }

    /**
     * Opens a file from resource id with empty password and default configuration.
     *
     * @param packageContext the context
     * @param resId          the resource id
     */
    public static void openDocument(Context packageContext, int resId) {
        openDocument(packageContext, resId, "");
    }

    /**
     * Opens a file from resource id with empty password and custom configuration.
     *
     * @param packageContext the context
     * @param resId          the resource id
     * @param config         the configuration
     */
    public static void openDocument(Context packageContext, int resId, @Nullable ViewerConfig config) {
        openDocument(packageContext, resId, "", config);
    }

    /**
     * Opens a file from Uri with password and default configuration.
     *
     * @param packageContext the context
     * @param fileUri        the file Uri
     * @param password       the password
     */
    public static void openDocument(Context packageContext, Uri fileUri, String password) {
        openDocument(packageContext, fileUri, password, null);
    }

    /**
     * Opens a file from resource id with password and default configuration.
     *
     * @param packageContext the context
     * @param resId          the resource id
     * @param password       the password
     */
    public static void openDocument(Context packageContext, int resId, String password) {
        openDocument(packageContext, resId, password, null);
    }

    /**
     * Opens a file from Uri with password and custom configuration.
     *
     * @param packageContext the context
     * @param fileUri        the file Uri
     * @param password       the password
     * @param config         the configuration
     */
    public static void openDocument(Context packageContext, Uri fileUri, String password, @Nullable ViewerConfig config) {
        Intent intent = new Intent(packageContext, SimpleReaderActivity.class);
        if (null != fileUri) {
            intent.putExtra(EXTRA_FILE_URI, fileUri);
        }
        if (null != password) {
            intent.putExtra(EXTRA_FILE_PASSWORD, password);
        }
        intent.putExtra(EXTRA_CONFIG, config);
        packageContext.startActivity(intent);
    }

    /**
     * Opens a file from resource id with password and custom configuration.
     *
     * @param packageContext the context
     * @param resId          the resource id
     * @param password       the password
     * @param config         the configuration
     */
    public static void openDocument(Context packageContext, int resId, String password, @Nullable ViewerConfig config) {
        Intent intent = new Intent(packageContext, SimpleReaderActivity.class);
        intent.putExtra(EXTRA_FILE_RES_ID, resId);
        if (null != password) {
            intent.putExtra(EXTRA_FILE_PASSWORD, password);
        }
        intent.putExtra(EXTRA_CONFIG, config);
        packageContext.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        try {
            AppUtils.initializePDFNetApplication(getApplicationContext(),
                PDFNetConfig.loadFromXML(getApplicationContext(), R.xml.pdfnet_config));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        mToolbarMenuRes = R.menu.fragment_viewer_simple;
        mSampleRes = R.raw.getting_started;

        super.onCreate(savedInstanceState);
    }

    /** @hide */
    @Override
    public boolean onToolbarCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenuOpenFile = menu.findItem(R.id.action_open_file);
        mMenuOpenUrl = menu.findItem(R.id.action_open_url);
        return false;
    }

    /** @hide */
    @Override
    public boolean onToolbarOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (id == R.id.action_open_file) {
            FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.SELECT_FILE,
                Environment.getExternalStorageDirectory());
            dialogFragment.setSingleFileListener(new FilePickerDialogFragment.SingleFileListener() {
                @Override
                public void onSingleFileSelected(int requestCode, FileInfo fileInfo) {
                    Uri fileUri;
                    if (fileInfo.getType() == BaseFileInfo.FILE_TYPE_FILE) {
                        fileUri = Uri.fromFile(new File(fileInfo.getAbsolutePath()));
                    } else {
                        fileUri = Uri.parse(fileInfo.getAbsolutePath());
                    }

                    Bundle args = buildTabBundle(fileUri, "");
                    mPdfViewCtrlTabHostFragment.onOpenAddNewTab(args);
                }
            });
            dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
            dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
        } else if (id == R.id.action_open_url) {
            DialogOpenUrl dialogOpenUrl = new DialogOpenUrl(this, new DialogOpenUrl.DialogOpenUrlListener() {
                @Override
                public void onSubmit(String url) {
                    Bundle args = buildTabBundle(Uri.parse(url), "");
                    mPdfViewCtrlTabHostFragment.onOpenAddNewTab(args);
                }
            });
            dialogOpenUrl.show();
        }
        return false;
    }
}
