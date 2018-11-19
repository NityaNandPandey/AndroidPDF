package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.dialog.SoundDialogFragment;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AppUtils;
import com.pdftron.pdf.utils.PdfViewCtrlTabsManager;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;

import java.io.File;
import java.util.List;

/**
 * DocumentActivity is derived from <a target="_blank" href="https://developer.android.com/reference/android/support/v7/app/AppCompatActivity.html">android.support.v7.app.AppCompatActivity</a>
 * and is an all-in-one document reader and PDF editor. UI can be configured via {@link ViewerConfig class}.
 */
public class DocumentActivity extends AppCompatActivity implements
    PdfViewCtrlTabHostFragment.TabHostListener {


    public static final String EXTRA_FILE_URI = "extra_file_uri";
    public static final String EXTRA_FILE_RES_ID = "extra_file_res_id";
    public static final String EXTRA_FILE_PASSWORD = "extra_file_password";
    public static final String EXTRA_CONFIG = "extra_config";

    private static final String SAVE_INSTANCE_TABBED_HOST_FRAGMENT_TAG = "tabbed_host_fragment";

    protected PdfViewCtrlTabHostFragment mPdfViewCtrlTabHostFragment;
    protected ViewerConfig mViewerConfig;

    protected int mSampleRes = 0;
    protected int mToolbarMenuRes = R.menu.fragment_viewer;

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
        Intent intent = new Intent(packageContext, DocumentActivity.class);
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
        Intent intent = new Intent(packageContext, DocumentActivity.class);
        intent.putExtra(EXTRA_FILE_RES_ID, resId);
        if (null != password) {
            intent.putExtra(EXTRA_FILE_PASSWORD, password);
        }
        intent.putExtra(EXTRA_CONFIG, config);
        packageContext.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            AppUtils.initializePDFNetApplication(getApplicationContext());
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        if (Utils.applyDayNight(this)) {
            return;
        }

        if (savedInstanceState != null) {
            // fragments management
            mPdfViewCtrlTabHostFragment = (PdfViewCtrlTabHostFragment) getSupportFragmentManager().getFragment(savedInstanceState,
                SAVE_INSTANCE_TABBED_HOST_FRAGMENT_TAG);
            if (mPdfViewCtrlTabHostFragment != null) {
                mPdfViewCtrlTabHostFragment.addHostListener(this);
            }

            // removing existing tab fragments since they will be created from scratch in host fragment
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            for (Fragment fragment : fragments) {
                if (fragment instanceof PdfViewCtrlTabFragment ||
                    fragment instanceof DialogFragment) {
                    ft.remove(fragment);
                }
            }
            ft.commitNow();
        }

        setContentView(R.layout.activity_document);

        if (getIntent() != null && getIntent().getExtras() != null) {
            mViewerConfig = getIntent().getExtras().getParcelable(EXTRA_CONFIG);
        }

        ShortcutHelper.enable(true);

        onDocumentSelected();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!Utils.hasStoragePermission(this)) {
            Utils.requestStoragePermissions(this, null, RequestCode.STORAGE_1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != mPdfViewCtrlTabHostFragment) {
            mPdfViewCtrlTabHostFragment.removeHostListener(this);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v("LifeCycle", "Main.onSaveInstanceState");
        super.onSaveInstanceState(outState);

        FragmentManager fm = getSupportFragmentManager();
        List<Fragment> fragments = fm.getFragments();
        if (mPdfViewCtrlTabHostFragment != null && fragments.contains(mPdfViewCtrlTabHostFragment)) {
            fm.putFragment(outState, SAVE_INSTANCE_TABBED_HOST_FRAGMENT_TAG, mPdfViewCtrlTabHostFragment);
        }
    }

    @Override
    public void onBackPressed() {
        boolean handled = false;
        if (mPdfViewCtrlTabHostFragment != null) {
            handled = mPdfViewCtrlTabHostFragment.handleBackPressed();
        }
        if (!handled) {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RequestCode.RECORD_AUDIO) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(SoundDialogFragment.TAG);
            if (fragment != null && fragment instanceof SoundDialogFragment) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    protected Bundle buildTabBundle(Uri fileUri, String password) {
        Bundle args = null;
        if (fileUri != null) {
            args = PdfViewCtrlTabFragment.createBasicPdfViewCtrlTabBundle(this, fileUri, password, mViewerConfig);
        }
        return args;
    }

    protected void onDocumentSelected(Uri fileUri) {
        onDocumentSelected(fileUri, "");
    }

    protected void onDocumentSelected(Uri fileUri, String password) {
        Bundle args = buildTabBundle(fileUri, password);
        startTabHostFragment(args);
    }

    protected void onDocumentSelected() {
        if (isFinishing()) {
            return;
        }

        Uri fileUri = null;
        String password = "";
        try {
            if (getIntent() != null && getIntent().getExtras() != null) {
                fileUri = getIntent().getExtras().getParcelable(EXTRA_FILE_URI);
                int fileResId = getIntent().getExtras().getInt(EXTRA_FILE_RES_ID, 0);
                password = getIntent().getExtras().getString(EXTRA_FILE_PASSWORD);

                if (null == fileUri && fileResId != 0) {
                    File file = Utils.copyResourceToLocal(this, fileResId,
                        "untitled", ".pdf");
                    if (null != file && file.exists()) {
                        fileUri = Uri.fromFile(file);
                    }
                }
            }
            int tabCount = PdfViewCtrlTabsManager.getInstance().getDocuments(this).size();
            if (null == fileUri && tabCount == 0 && mSampleRes != 0) {
                File file = Utils.copyResourceToLocal(this, mSampleRes,
                    "getting_started", ".pdf");
                if (null != file && file.exists()) {
                    fileUri = Uri.fromFile(file);
                    password = "";
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        onDocumentSelected(fileUri, password);
    }

    protected void startTabHostFragment(Bundle args) {
        if (isFinishing()) {
            return;
        }

        if (args == null) {
            args = new Bundle();
        }
        args.putInt(PdfViewCtrlTabHostFragment.BUNDLE_TAB_HOST_NAV_ICON, R.drawable.ic_arrow_back_white_24dp);
        if (mToolbarMenuRes != 0) {
            args.putInt(PdfViewCtrlTabHostFragment.BUNDLE_TAB_HOST_TOOLBAR_MENU, mToolbarMenuRes);
        }
        args.putParcelable(PdfViewCtrlTabHostFragment.BUNDLE_TAB_HOST_CONFIG, mViewerConfig);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        mPdfViewCtrlTabHostFragment = PdfViewCtrlTabHostFragment.newInstance(args);
        mPdfViewCtrlTabHostFragment.addHostListener(this);

        ft.replace(R.id.container, mPdfViewCtrlTabHostFragment, null);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onTabHostShown() {

    }

    @Override
    public void onTabHostHidden() {

    }

    @Override
    public void onLastTabClosed() {
        finish();
    }

    @Override
    public void onTabChanged(String tag) {

    }

    @Override
    public void onOpenDocError() {

    }

    @Override
    public void onNavButtonPressed() {
        finish();
    }

    @Override
    public void onShowFileInFolder(String fileName, String filepath, int itemSource) {

    }

    @Override
    public boolean canShowFileInFolder() {
        return false;
    }

    @Override
    public boolean onToolbarCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        return false;
    }

    @Override
    public boolean onToolbarPrepareOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onToolbarOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onStartSearchMode() {

    }

    @Override
    public void onExitSearchMode() {

    }

    @Override
    public boolean canRecreateActivity() {
        return true;
    }

    @Override
    public void onTabPaused(FileInfo fileInfo, boolean isDocModifiedAfterOpening) {

    }
}
