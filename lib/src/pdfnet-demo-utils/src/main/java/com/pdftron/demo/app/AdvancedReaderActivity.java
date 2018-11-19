package com.pdftron.demo.app;
//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentCallbacks2;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.FixedDrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.pdftron.common.PDFNetException;
import com.pdftron.demo.R;
import com.pdftron.demo.dialog.PortfolioDialogFragment;
import com.pdftron.demo.navigation.CriticalPermissionDialogFragment;
import com.pdftron.demo.navigation.ExternalStorageViewFragment;
import com.pdftron.demo.navigation.FavoritesViewFragment;
import com.pdftron.demo.navigation.FileBrowserViewFragment;
import com.pdftron.demo.navigation.FileInfoDrawer;
import com.pdftron.demo.navigation.FileInfoDrawerFragment;
import com.pdftron.demo.navigation.LocalFileViewFragment;
import com.pdftron.demo.navigation.LocalFolderViewFragment;
import com.pdftron.demo.navigation.RecentViewFragment;
import com.pdftron.demo.navigation.ToolbarFragment;
import com.pdftron.demo.navigation.callbacks.FilePickerCallbacks;
import com.pdftron.demo.navigation.callbacks.FileUtilCallbacks;
import com.pdftron.demo.navigation.callbacks.JumpNavigationCallbacks;
import com.pdftron.demo.navigation.callbacks.MainActivityListener;
import com.pdftron.demo.utils.ActivityUtils;
import com.pdftron.demo.utils.AppUtils;
import com.pdftron.demo.utils.FileManager;
import com.pdftron.demo.utils.Logger;
import com.pdftron.demo.utils.MiscUtils;
import com.pdftron.demo.utils.SettingsManager;
import com.pdftron.demo.utils.ThumbnailPathCacheManager;
import com.pdftron.demo.viewmodel.FavoriteViewModel;
import com.pdftron.demo.viewmodel.RecentViewModel;
import com.pdftron.demo.widget.ScrimInsetsFrameLayout;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.DocumentPreviewCache;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFNetInternalTools;
import com.pdftron.pdf.config.PDFNetConfig;
import com.pdftron.pdf.controls.PasswordDialogFragment;
import com.pdftron.pdf.controls.PdfViewCtrlTabFragment;
import com.pdftron.pdf.controls.PdfViewCtrlTabHostFragment;
import com.pdftron.pdf.dialog.SoundDialogFragment;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.FavoriteFilesManager;
import com.pdftron.pdf.utils.ImageMemoryCache;
import com.pdftron.pdf.utils.PathPool;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.PdfViewCtrlTabsManager;
import com.pdftron.pdf.utils.RecentFilesManager;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.pdftron.pdf.controls.PdfViewCtrlTabFragment.BUNDLE_TAB_TITLE;

/**
 * AdvancedReaderActivity is derived from <a target="_blank" href="https://developer.android.com/reference/android/support/v7/app/AppCompatActivity.html">android.support.v7.app.AppCompatActivity</a>
 * and is an all-in-one document reader, PDF editor and file manager.
 */
public class AdvancedReaderActivity extends AppCompatActivity implements
    NavigationView.OnNavigationItemSelectedListener,
    PasswordDialogFragment.PasswordDialogFragmentListener,
    PortfolioDialogFragment.PortfolioDialogFragmentListener,
    FilePickerCallbacks,
    FileUtilCallbacks,
    JumpNavigationCallbacks,
    ActivityCompat.OnRequestPermissionsResultCallback,
    CriticalPermissionDialogFragment.OnPermissionDialogFragmentListener,
    PdfViewCtrlTabHostFragment.TabHostListener {

    private static final String TAG = AdvancedReaderActivity.class.getSimpleName();

    private static boolean sDebug = false;

    public static void setDebug(boolean debug) {
        AdvancedReaderActivity.sDebug = debug;
    }

    private static final String FRAG_TAG_FILE_INFO_DRAWER = "file_info_drawer";

    private static final String FRAG_TAG_PERMISSION_SCREEN = "permission_screen";

    public static final int MAX_PASSWORD_ATTEMPTS = 3;

    public static final int PDFDOC_TYPE_UNKNOWN = -1;
    public static final int PDFDOC_TYPE_FILE = 0;
    public static final int PDFDOC_TYPE_ENCRYPTED = 1;
    public static final int PDFDOC_TYPE_XFA = 2;
    public static final int PDFDOC_TYPE_PORTFOLIO = 3;

    private static final String SAVE_INSTANCE_QUIT_APP = "processed_should_quit_app";
    private static final String SAVE_INSTANCE_CURRENT_FRAGMENT_TAG = "current_fragment";
    private static final String SAVE_INSTANCE_LAST_ADDED_BROWSER_FRAGMENT_TAG = "last_added_browser_fragment";
    private static final String SAVE_INSTANCE_TABBED_HOST_FRAGMENT_TAG = "tabbed_host_fragment";
    private static final String SAVE_INSTANCE_PROCESSED_FRAGMENT_VIEW_ID = "processed_fragment_view_id";
    private static final String SAVE_INSTANCE_BROWSER_PROCESSED_FRAGMENT_VIEW_ID = "browser_processed_fragment_view_id";

    public static final int MENU_ITEM_NONE = -1;

    private static final int SELECT_NAVIGATION_ITEM_DELAY = 250; // ms
    private static final int TEACH_NAVIGATION_DRAWER_DELAY = 500; // ms

    private Fragment mCurrentFragment;
    private Fragment mLastAddedBrowserFragment;
    private PdfViewCtrlTabHostFragment mPdfViewCtrlTabHostFragment;
    private Bundle mTabbedHostBundle;
    private boolean mReturnFromSettings;
    private boolean mOnResumeFragmentsCalled;
    private boolean mHandleOpenDocError;
    private boolean mHandleLastTabClosed;

    private FixedDrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private final Handler mDrawerHandler = new Handler(Looper.getMainLooper());

    // Navigation drawer
    private NavigationView mNavigationDrawerView;

    // File info drawer
    private ScrimInsetsFrameLayout mFileInfoDrawerView;

    @Nullable
    private FileInfoDrawer mFileInfoDrawer;

    private int mPasswordAttemptCounter = 0;
    private File mGettingStartedFile = null;

    private int mProcessedFragmentViewId = R.id.item_file_list;
    private int mBrowserProcessedFragmentViewId = R.id.item_file_list;
    private MenuItem mNextFragmentViewMenuItem = null;

    private boolean mTeachNavDrawer = false;
    private boolean mQuitAppWhenDoneViewing = false;

    private boolean mIsFirstTimeRunConsumed;

    /**
     * Opens the CompleteReader demo app.
     *
     * @param packageContext the context
     */
    public static void open(Context packageContext) {
        Intent intent = new Intent(packageContext, AdvancedReaderActivity.class);
        packageContext.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.INSTANCE.LogV("LifeCycle", "Main.onCreate");
        super.onCreate(savedInstanceState);

        if (Utils.applyDayNight(this)) {
            return;
        }

        File user_app_folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            getApplicationContext().getResources().getString(R.string.app_name));
        File download_folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        PDFNetConfig config = PDFNetConfig.getDefaultConfig();
        config.addExtraResourcePaths(user_app_folder);
        config.addExtraResourcePaths(download_folder);
        try {
            AppUtils.initializePDFNetApplication(getApplicationContext(), config);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (sDebug) {
            try {
                PDFNetInternalTools.setDefaultLogThreshold(PDFNetInternalTools.e_debug);
                PDFNetInternalTools.setThresholdForLogStream("NOZOOM", PDFNetInternalTools.e_info);
                PDFNetInternalTools.setThresholdForLogStream("tracer", PDFNetInternalTools.e_trace);
                PDFNetInternalTools.setThresholdForLogStream("tiling", PDFNetInternalTools.e_trace);
                PDFNetInternalTools.setThresholdForLogStream("undo", PDFNetInternalTools.e_trace);
                PDFNetInternalTools.setThresholdForLogStream("save", PDFNetInternalTools.e_trace);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        ShortcutHelper.enable(true);

        if (savedInstanceState != null) {
            mQuitAppWhenDoneViewing = savedInstanceState.getBoolean(SAVE_INSTANCE_QUIT_APP);

            // fragments management
            mCurrentFragment = getSupportFragmentManager().getFragment(savedInstanceState,
                SAVE_INSTANCE_CURRENT_FRAGMENT_TAG);
            if (mCurrentFragment != null) {
                setCurrentFragment(mCurrentFragment);
            }
            mLastAddedBrowserFragment = getSupportFragmentManager().getFragment(savedInstanceState,
                SAVE_INSTANCE_LAST_ADDED_BROWSER_FRAGMENT_TAG);
            mPdfViewCtrlTabHostFragment = (PdfViewCtrlTabHostFragment) getSupportFragmentManager().getFragment(savedInstanceState,
                SAVE_INSTANCE_TABBED_HOST_FRAGMENT_TAG);
            if (mPdfViewCtrlTabHostFragment != null) {
                mPdfViewCtrlTabHostFragment.addHostListener(this);
            }
            mProcessedFragmentViewId = savedInstanceState
                .getInt(SAVE_INSTANCE_PROCESSED_FRAGMENT_VIEW_ID, R.id.item_file_list);
            mBrowserProcessedFragmentViewId = savedInstanceState
                .getInt(SAVE_INSTANCE_BROWSER_PROCESSED_FRAGMENT_VIEW_ID, R.id.item_file_list);

            // removing existing tab fragments since they will be created from scratch in host fragment
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            for (Fragment fragment : fragments) {
                if (fragment instanceof PdfViewCtrlTabFragment ||
                    fragment instanceof DialogFragment) {
                    ft.remove(fragment);
                }
            }
            ft.commit();
        }

        // Is this the first run of the app?
        boolean isAppUpdated = SettingsManager.getAppUpdated(this);

        if (Utils.hasStoragePermission(this)) {
            isAppUpdated = isAppUpdated();
        }

        try {
            if (isAppUpdated) {
                Logger.INSTANCE.LogD(TAG, "Resetting thumb cache and recent cache");
                DocumentPreviewCache.clearCache();
                // ReflowProcessor.clearCache();
                // RecentlyUsedCache.resetCache();
            }
        } catch (Exception e) {
            Logger.INSTANCE.LogE(TAG, "Error");
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        setContentView(R.layout.activity_complete_reader);

        if (Utils.isLollipop()) {
            // Use a transparent status bar, because the NavigationView and other fullscreen Views
            // need to draw inside the system window insets.
            // This is not set in the v21/styles.xml file because other windows that get their
            // theme/style from this Activity may not work well with a transparent status bar.
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mNavigationDrawerView = findViewById(R.id.navigation_drawer);
        ConstraintLayout navigationDrawerHeader = (ConstraintLayout) getLayoutInflater().inflate(R.layout.custom_nav_drawer_header, mNavigationDrawerView, false);
        ActivityUtils.setupDrawer(this, mDrawerLayout, mNavigationDrawerView, navigationDrawerHeader);
        mNavigationDrawerView.setNavigationItemSelectedListener(this);

        mFileInfoDrawerView = findViewById(R.id.file_info_drawer);

        if (!Utils.isTablet(this)) {
            // Set the drawer's width to be the smallest screen dimension
            Point displaySize = new Point();
            Utils.getDisplaySize(this, displaySize);
            mFileInfoDrawerView.getLayoutParams().width = Math.min(displaySize.x, displaySize.y);
        } else {
            mFileInfoDrawerView.getLayoutParams().width = getResources().getDimensionPixelSize(R.dimen.navigation_drawer_width);
        }

        toggleInfoDrawerLockMode(false); // Lock by default
        toggleNavigationDrawerLockMode(true); // Unlock by default

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
            this, /* host Activity */
            mDrawerLayout, /* DrawerLayout object */
            R.string.navigation_drawer_open, /* "open drawer" description for accessibility */
            R.string.navigation_drawer_close /* "close drawer" description for accessibility */
        );

        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                if (drawerView.equals(mNavigationDrawerView)) {
                    mDrawerToggle.onDrawerSlide(drawerView, 0); // 0 offset disables animation
                } else if (drawerView.equals(mFileInfoDrawerView)) {
                    if (mFileInfoDrawer != null) {
                        mFileInfoDrawer.onDrawerSlide(drawerView, slideOffset);
                    }
                }

                if (mCurrentFragment != null && mCurrentFragment.getView() != null
                    && hasMainActivityListener(mCurrentFragment)) {
                    ((MainActivityListener) mCurrentFragment).onDrawerSlide();
                }
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                handleDrawerOpen(drawerView);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                handleDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_DRAGGING && mCurrentFragment instanceof PdfViewCtrlTabHostFragment) {
                    ((PdfViewCtrlTabHostFragment) mCurrentFragment).setLongPressEnabled(false);
                }

                if (newState == DrawerLayout.STATE_SETTLING) {
                    updateViewerVisibility();
                }

                mDrawerToggle.onDrawerStateChanged(newState);

                if (mFileInfoDrawer != null) {
                    mFileInfoDrawer.onDrawerStateChanged(newState);
                }
            }
        });

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        loadFragmentViewIds();

        // Get intent and launch viewer if we get a valid file
        int currentProgressedFragmentViewId = mProcessedFragmentViewId;
        if (savedInstanceState == null) {
            processIntent(getIntent());
        }
        if (currentProgressedFragmentViewId != mProcessedFragmentViewId) {
            updateNavTab();
        }

        /* Until further notice, it seems that in order to receive any system-ui visibility events
         * further down the View hierarchy (where visibility listeners are set on non-decor Views)
         * for Views that are GONE or INVISIBLE, a visibility listener needs to be set elsewhere
         * on a VISIBLE View. */
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                // No-op
            }
        });

        Logger.INSTANCE.LogI("TIMING", "Main.onCreate END");

        // for facilitating computations:
        // load all favorite files
        ViewModelProviders.of(this).get(FavoriteViewModel.class);
        // load all recent files
        ViewModelProviders.of(this).get(RecentViewModel.class);
    }

    private void handleDrawerOpen(View drawerView) {
        if (drawerView.equals(mNavigationDrawerView)) {
            //mDrawerToggle.onDrawerOpened(drawerView);

            Logger.INSTANCE.LogD(TAG, "onNavigationDrawerOpened");
            if (mDrawerLayout != null && mCurrentFragment instanceof PdfViewCtrlTabHostFragment) {
                // Unlock the navigation drawer, so that it can be swiped closed by the user.
                // (It should be locked closed again the next time it closes)
                toggleNavigationDrawerLockMode(true);
                // Lock drawer closed, so that it can only be opened programmatically
                toggleInfoDrawerLockMode(false);
            }
        } else if (drawerView.equals(mFileInfoDrawerView)) {
            if (mFileInfoDrawer != null) {
                mFileInfoDrawer.onDrawerOpened(drawerView);
            }

            // Lock navigation drawer closed
            toggleNavigationDrawerLockMode(false);
            // Unlock right drawer when opened, so that it can be swiped closed
            toggleInfoDrawerLockMode(true);
        }

        if (mCurrentFragment != null && mCurrentFragment.getView() != null
            && hasMainActivityListener(mCurrentFragment)) {
            ((MainActivityListener) mCurrentFragment).onDrawerOpened();
        }
    }

    private void handleDrawerClosed(View drawerView) {
        if (mCurrentFragment instanceof PdfViewCtrlTabHostFragment) {
            ((PdfViewCtrlTabHostFragment) mCurrentFragment).resetHideToolbarsTimer();
            ((PdfViewCtrlTabHostFragment) mCurrentFragment).setLongPressEnabled(true);
        }

        if (drawerView.equals(mNavigationDrawerView)) {
            //mDrawerToggle.onDrawerClosed(drawerView);
            if (mNextFragmentViewMenuItem != null) {
                selectNavigationItem(mNextFragmentViewMenuItem);
                mNextFragmentViewMenuItem = null;
            }
            if (mDrawerLayout != null && mCurrentFragment instanceof PdfViewCtrlTabHostFragment) {
                // disable swipe open if in viewer mode
                toggleNavigationDrawerLockMode(false);
                toggleInfoDrawerLockMode(false);
            }
            //Logger.INSTANCE.LogD(TAG, "onNavigationDrawerClosed");
        } else if (drawerView.equals(mFileInfoDrawerView)) {
            if (mFileInfoDrawer != null) {
                mFileInfoDrawer.onDrawerClosed(drawerView);
            }

            // Unlock navigation drawer
            toggleNavigationDrawerLockMode(true);
            // Lock drawer closed, so that it can only be opened programmatically
            toggleInfoDrawerLockMode(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Logger.INSTANCE.LogV("LifeCycle", "Main.onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putBoolean(SAVE_INSTANCE_QUIT_APP, mQuitAppWhenDoneViewing);

        FragmentManager fm = getSupportFragmentManager();
        List<Fragment> fragments = fm.getFragments();
        if (mCurrentFragment != null && fragments.contains(mCurrentFragment)) {
            fm.putFragment(outState, SAVE_INSTANCE_CURRENT_FRAGMENT_TAG, mCurrentFragment);
        }
        if (mLastAddedBrowserFragment != null && fragments.contains(mLastAddedBrowserFragment)) {
            fm.putFragment(outState, SAVE_INSTANCE_LAST_ADDED_BROWSER_FRAGMENT_TAG, mLastAddedBrowserFragment);
        }
        if (mPdfViewCtrlTabHostFragment != null && fragments.contains(mPdfViewCtrlTabHostFragment)) {
            fm.putFragment(outState, SAVE_INSTANCE_TABBED_HOST_FRAGMENT_TAG, mPdfViewCtrlTabHostFragment);
        }
        outState.putInt(SAVE_INSTANCE_PROCESSED_FRAGMENT_VIEW_ID, mProcessedFragmentViewId);
        outState.putInt(SAVE_INSTANCE_BROWSER_PROCESSED_FRAGMENT_VIEW_ID, mBrowserProcessedFragmentViewId);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean handled = false;

        if (mCurrentFragment != null && mCurrentFragment.getView() != null) {
            if (hasMainActivityListener(mCurrentFragment)) {
                handled = ((MainActivityListener) mCurrentFragment).onKeyUp(keyCode, event);
            }
            if (!handled && mCurrentFragment instanceof PdfViewCtrlTabHostFragment) {
                // pass handle key up to host fragment only if drawer is not open
                if (mDrawerLayout == null || !mDrawerLayout.isDrawerOpen(mNavigationDrawerView)) {
                    handled = ((PdfViewCtrlTabHostFragment) mCurrentFragment).handleKeyUp(keyCode, event);
                }
            }
        }

        if (!handled) {
            handled = super.onKeyUp(keyCode, event);
        }

        return handled;
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    private void handleBackPress() {
        if (mDrawerLayout != null) {
            if (mDrawerLayout.isDrawerOpen(mFileInfoDrawerView)) {
                toggleInfoDrawer(false);
                return;
            }
            if (!Utils.isLargeScreenWidth(this)) {
                if (mDrawerLayout.isDrawerOpen(mNavigationDrawerView)) {
                    toggleNavigationDrawer(false);
                    return;
                }
            }
        }
        boolean handled = false;
        if (mCurrentFragment != null && mCurrentFragment.getView() != null) {
            if (hasMainActivityListener(mCurrentFragment)) {
                try {
                    handled = ((MainActivityListener) mCurrentFragment).onBackPressed();
                } catch (Exception e) {
                    // Do nothing
                }
            } else if (mCurrentFragment instanceof PdfViewCtrlTabHostFragment) {
                handled = ((PdfViewCtrlTabHostFragment) mCurrentFragment).handleBackPressed();
                if (!handled && !mQuitAppWhenDoneViewing && mLastAddedBrowserFragment != null) {
                    startFragment(mLastAddedBrowserFragment);
                    handled = true;
                }
            }
        }
        if (!handled) {
            super.onBackPressed();
        }
    }

    private boolean isFirstTimeRun() {
        if (mIsFirstTimeRunConsumed) {
            return true;
        }
        boolean isFirstTimeRun = SettingsManager.getFirstTimeRun(this);
        if (isFirstTimeRun) {
            SettingsManager.updateFirstTimeRun(this, false);
            mIsFirstTimeRunConsumed = true;
        }
        return isFirstTimeRun;
    }

    @Override
    public void onLastTabClosed() {
        MenuItem viewerMenuItem = mNavigationDrawerView.getMenu().findItem(R.id.item_viewer);
        if (viewerMenuItem != null) {
            viewerMenuItem.setVisible(false);
        }

        SettingsManager.updateNavTab(this, getNavTabFromId(mBrowserProcessedFragmentViewId));

        if (!mQuitAppWhenDoneViewing) {
            handleLastTabClosed();
        } else {
            finish();
        }
    }

    private void handleLastTabClosed() {
        if (mOnResumeFragmentsCalled) {
            if (mBrowserProcessedFragmentViewId != MENU_ITEM_NONE) {
                MenuItem menuItem = mNavigationDrawerView.getMenu().findItem(mBrowserProcessedFragmentViewId);
                selectNavigationItem(menuItem);
            } else {
                MenuItem menuItem = mNavigationDrawerView.getMenu().findItem(R.id.item_file_list);
                selectNavigationItem(menuItem);
            }
        } else {
            mHandleLastTabClosed = true;
        }
    }

    @Override
    public void onTabChanged(String tag) {
        mQuitAppWhenDoneViewing = false;
    }

    @Override
    public void onOpenDocError() {
        if (!mQuitAppWhenDoneViewing) {
            handleOpenDocError();
        } else {
            finish();
        }
    }

    private void handleOpenDocError() {
        if (mOnResumeFragmentsCalled) {
            if (mBrowserProcessedFragmentViewId != MENU_ITEM_NONE
                && mBrowserProcessedFragmentViewId != mProcessedFragmentViewId) {
                MenuItem menuItem = mNavigationDrawerView.getMenu().findItem(mBrowserProcessedFragmentViewId);
                selectNavigationItem(menuItem);
            }
        } else {
            mHandleOpenDocError = true;
        }
    }

    @Override
    public void onNavButtonPressed() {
        Logger.INSTANCE.LogD(TAG, "mCurrentFragment: " + mCurrentFragment.getClass().getName());
        if (mDrawerLayout != null) {
            if (mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            } else {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        }
    }

    private void toggleNavigationDrawer(boolean open) {
        toggleNavigationDrawer(open, true);
    }

    private void toggleNavigationDrawer(boolean open, boolean animated) {
        if (null == mDrawerLayout) {
            return;
        }
        if (open) {
            mDrawerLayout.openDrawer(mNavigationDrawerView, animated);
        } else {
            if (Utils.isLargeScreenWidth(this)) {
                return;
            }
            mDrawerLayout.closeDrawer(mNavigationDrawerView, animated);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void toggleInfoDrawer(boolean open) {
        if (null == mDrawerLayout) {
            return;
        }
        if (open) {
            mDrawerLayout.openDrawer(mFileInfoDrawerView);
        } else {
            mDrawerLayout.closeDrawer(mFileInfoDrawerView);
        }
    }

    @Override
    public boolean canShowFileInFolder() {
        return true;
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
    public void onTabPaused(
        FileInfo fileInfo,
        boolean isDocModifiedAfterOpening) {

        // force to load thumbnail from Core rather than from bitmap cache
        // because if the document is modified, the thumbnail for the first page might be changed too
        if (isDocModifiedAfterOpening && fileInfo != null) {
            ThumbnailPathCacheManager.getInstance().removeThumbnailPath(fileInfo.getAbsolutePath());
        }

    }

    @Override
    public void onShowFileInFolder(String filename, String filepath, int itemSource) {
        if (filepath == null || filename == null) {
            return;
        }

        switch (itemSource) {
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
                PdfViewCtrlSettingsManager.updateSavedExternalFolderUri(this, filepath);
                PdfViewCtrlSettingsManager.updateSavedExternalFolderTreeUri(this, filepath);
                mProcessedFragmentViewId = R.id.item_external_storage;
                mBrowserProcessedFragmentViewId = mProcessedFragmentViewId;
                break;
            case BaseFileInfo.FILE_TYPE_FILE:
            case BaseFileInfo.FILE_TYPE_OPEN_URL:
                // if this file is from external go to all files instead
                if (Utils.isSdCardFile(this, new File(filepath))) {
                    PdfViewCtrlSettingsManager.updateSavedExternalFolderUri(this, filepath);
                    PdfViewCtrlSettingsManager.updateSavedExternalFolderTreeUri(this, filepath);
                    mProcessedFragmentViewId = R.id.item_file_list;
                    mBrowserProcessedFragmentViewId = mProcessedFragmentViewId;
                } else {
                    // Update last-used local folder path to the specified path
                    PdfViewCtrlSettingsManager.updateLocalFolderPath(this, filepath);
                    PdfViewCtrlSettingsManager.updateLocalFolderTree(this, filepath);
                    mProcessedFragmentViewId = R.id.item_folder_list;
                    mBrowserProcessedFragmentViewId = mProcessedFragmentViewId;
                }
                break;
        }
        if (mProcessedFragmentViewId != MENU_ITEM_NONE) {
            MenuItem menuItem = mNavigationDrawerView.getMenu().findItem(mProcessedFragmentViewId);
            selectNavigationItem(menuItem);
        }
    }

    @Override
    public void onTabHostHidden() {
        Logger.INSTANCE.LogV(TAG, "Tab Host is hidden");
        unlockDrawer();
    }

    @Override
    public void onTabHostShown() {
        Logger.INSTANCE.LogV(TAG, "Tab Host is shown");
        // even if the current fragment is not an instance of PdfViewCtrlTabHostFragment and exit the app
        // and open it again, the PdfViewCtrlTabHostFragment instance, if exists, will be resumed.
        // In such cases, shouldn't lock drawer
        lockDrawer();

        updateViewerVisibility();
    }

    private void toggleInfoDrawerLockMode(boolean unlock) {
        if (unlock) {
            // Lock drawer closed, so that it can only be opened programmatically
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mFileInfoDrawerView);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mFileInfoDrawerView);
        }
    }

    private void toggleNavigationDrawerLockMode(boolean unlock) {
        if (mDrawerLayout == null) {
            return;
        }
        if (Utils.isLargeScreenWidth(this)) {
            // always lock open
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, mNavigationDrawerView);
            mDrawerLayout.setScrimColor(Color.TRANSPARENT);
            mDrawerLayout.setDisallowIntercept(true);
            return;
        }
        mDrawerLayout.setScrimColor(FixedDrawerLayout.DEFAULT_SCRIM_COLOR);
        if (unlock) {
            // Unlock navigation drawer
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, mNavigationDrawerView);
        } else {
            // Lock navigation drawer closed
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mNavigationDrawerView);
        }
    }

    private void unlockDrawer() {
        toggleNavigationDrawerLockMode(true);
        toggleInfoDrawerLockMode(false);
    }

    private void lockDrawer() {
        toggleNavigationDrawerLockMode(false);
        toggleInfoDrawerLockMode(false);
    }

    private void updateViewerVisibility() {
        MenuItem viewerMenuItem = mNavigationDrawerView.getMenu().findItem(R.id.item_viewer);
        if (viewerMenuItem != null) {
            if (PdfViewCtrlTabsManager.getInstance().getDocuments(this).size() > 0) {
                MenuItem recentMenuItem = mNavigationDrawerView.getMenu().findItem(R.id.item_recent);
                if (recentMenuItem != null && recentMenuItem.isVisible()) {
                    viewerMenuItem.setVisible(true);
                } else {
                    // if currently showing chat view, don't toggle viewer visibility
                    viewerMenuItem.setVisible(false);
                }
            } else {
                viewerMenuItem.setVisible(false);
            }
        }
    }

    private boolean doesNeedReloadBrowser() {
        return mCurrentFragment instanceof PdfViewCtrlTabHostFragment && mCurrentFragment.getView() != null
            && ((PdfViewCtrlTabHostFragment) mCurrentFragment).readAndUnsetFileSystemChanged();
    }

    private void reloadBrowser() {
        if (mCurrentFragment != null && mCurrentFragment.getView() != null
            && hasMainActivityListener(mCurrentFragment)) {
            ((MainActivityListener) mCurrentFragment).onDataChanged();
        }
    }

    private void copyTutorialFile() {
        File fileAppFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            getResources().getString(R.string.app_name));

        File tutFile = new File(fileAppFolder, "Getting Started.pdf");
        if (tutFile.exists() && tutFile.isFile()) {
            mGettingStartedFile = tutFile;
            FavoriteFilesManager.getInstance().addFile(this, new FileInfo(BaseFileInfo.FILE_TYPE_FILE, tutFile));
            return;
        }

        InputStream is = null;
        OutputStream fos = null;
        try {
            FileUtils.forceMkdir(fileAppFolder);
            mGettingStartedFile = tutFile;
            is = getResources().openRawResource(R.raw.getting_started);
            fos = new FileOutputStream(mGettingStartedFile);
            IOUtils.copy(is, fos);
        } catch (Exception e) {
            mGettingStartedFile = null;
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(fos);
            Utils.closeQuietly(is);
        }
        if (mGettingStartedFile != null)
            FavoriteFilesManager.getInstance().addFile(this, new FileInfo(BaseFileInfo.FILE_TYPE_FILE, mGettingStartedFile));
    }

    private boolean isAppUpdated() {
        boolean isAppUpdated = SettingsManager.getAppUpdated(this);
        if (isAppUpdated) {
            SettingsManager.updateLocalAppVersion(this);

            File fileAppFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                getResources().getString(R.string.app_name));

            InputStream is = null;
            OutputStream fos = null;
            try {
                FileUtils.forceMkdir(fileAppFolder);
                mGettingStartedFile = new File(fileAppFolder, "Getting Started.pdf");
                is = getResources().openRawResource(R.raw.getting_started);
                fos = new FileOutputStream(mGettingStartedFile);
                IOUtils.copy(is, fos);
            } catch (IOException e) {
                isAppUpdated = false;
                mGettingStartedFile = null;
            } catch (Exception e) {
                isAppUpdated = false;
                mGettingStartedFile = null;
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                Utils.closeQuietly(fos);
                Utils.closeQuietly(is);
            }
        }
        return isAppUpdated;
    }

    private void processIntent(Intent intent) {
        if (mCurrentFragment != null && mCurrentFragment.getView() != null
            && hasMainActivityListener(mCurrentFragment)) {
            try {
                ((MainActivityListener) mCurrentFragment).onProcessNewIntent();
            } catch (Exception e) {
                // Do nothing
            }
        }

        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        Uri data = intent.getData();
        ContentResolver contentResolver = getContentResolver();
        if (contentResolver == null) {
            return;
        }

        if (!MiscUtils.isIntentActionMain(intent)) {
            if (intent.getScheme() != null) {
                if (data != null && (intent.getScheme().equalsIgnoreCase("http") || intent.getScheme().equalsIgnoreCase("https"))) {
                    mQuitAppWhenDoneViewing = true;

                    // Opening URL
                    String strUri = data.toString();
                    String fileExtension = Utils.getExtension(strUri);
                    String title = data.getLastPathSegment();
                    // start fragment safely in onResumeFragments
                    mTabbedHostBundle = PdfViewCtrlTabFragment.createBasicPdfViewCtrlTabBundle(
                        strUri, title, fileExtension, "",
                        BaseFileInfo.FILE_TYPE_OPEN_URL);
                } else {
                    // check if it's an edit action with write access uri and is PDF file
                    if (action != null && (Intent.ACTION_EDIT.equals(action) || Intent.ACTION_VIEW.equals(action))
                        && data != null) {
                        String content = contentResolver.getType(data);
                        if (!Utils.isNullOrEmpty(content) && content.equals("application/pdf")) {
                            mQuitAppWhenDoneViewing = true;
                            onEditPdfIntentReceived(data, Intent.ACTION_EDIT.equals(action));
                            return;
                        }
                    }

                    // check if it's an edit action with write access PDF temp file
                    if (action != null && (Intent.ACTION_EDIT.equals(action) || Intent.ACTION_VIEW.equals(action))
                        && data != null && (data.toString().startsWith("file://")
                        && data.getLastPathSegment().toLowerCase().endsWith(".pdf"))) {
                        mQuitAppWhenDoneViewing = true;
                        onEditPdfIntentReceived(data, Intent.ACTION_EDIT.equals(action));
                        return;
                    }

                    // It can be a file in the storage media
                    File intentFile = MiscUtils.parseIntentGetPdfFile(this, intent);
                    if (intentFile != null) {
                        // Local file
                        mQuitAppWhenDoneViewing = true;
                        onFileSelected(intentFile, "");
                    } else {
                        // could be office files
                        String type = intent.getType();
                        if (data != null && type != null && Utils.isMimeTypeHandled(type)) {
                            intentFile = new File(data.getPath());
                            if (intentFile.exists()) {
                                mQuitAppWhenDoneViewing = true;
                                onFileSelected(intentFile, "");
                                return;
                            }
                        }

                        // It still can be a content stream...

                        // try to get the download path
                        String contentFilePath;
                        String[] projection = {MediaStore.MediaColumns.DATA};
                        Cursor cursor = null;
                        if (data != null) {
                            try {
                                cursor = contentResolver.query(data, projection, null, null, null);
                                if (cursor != null && cursor.getColumnCount() > 0 && cursor.getCount() > 0) {
                                    cursor.moveToFirst();
                                    int index = cursor.getColumnIndex(projection[0]);
                                    if (index != -1) {
                                        contentFilePath = cursor.getString(index);
                                        if (contentFilePath != null) {
                                            File contentFile = new File(contentFilePath);
                                            if (contentFile.exists() && contentFile.isFile()) {
                                                // So we have the file and we can launch the viewer normally
                                                mQuitAppWhenDoneViewing = true;
                                                onFileSelected(contentFile, "");
                                                return;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                if (cursor != null) { // otherwise couldn't find projection
                                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                                }
                            } finally {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }

                            // try with content resolver
                            if (intent.getDataString() != null && action != null && Intent.ACTION_EDIT.equals(action)) {
                                if (Utils.uriHasReadWritePermission(this, data)) {
                                    // if we have read/write access to a Uri, then copy it to Download folder
                                    String content = contentResolver.getType(data);
                                    if (!Utils.isNullOrEmpty(content) && content.equals("application/pdf")) {
                                        mQuitAppWhenDoneViewing = true;
                                        onEditPdfIntentReceived(data, Intent.ACTION_EDIT.equals(action));
                                        return;
                                    }
                                }
                            }
                        }

                        if (data != null && Utils.isOfficeDocument(contentResolver, data)) {
                            onOfficeUriSelected(data);
                            return;
                        }

                        // Let's try to copy the stream to a local file and open it
                        if (openLocalCopyFromUri(data)) {
                            return;
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(R.string.alert)
                            .setMessage(R.string.error_opening_doc_message)
                            .setPositiveButton(R.string.ok, null)
                            .create().show();
                    }
                }
            } else if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
                Bundle extras = intent.getExtras();

                // could be a content stream
                Uri uri = null;

                // First we try with content resolver
                // if we have read/write access to a Uri, open it directly
                if (extras != null) {
                    uri = (Uri) extras.get(Intent.EXTRA_STREAM);
                }
                if (Utils.uriHasReadWritePermission(this, uri)) {
                    // we have write permission to the file
                    String fileExtension = Utils.getUriExtension(contentResolver, uri);
                    if (fileExtension.equalsIgnoreCase("pdf")) {
                        // open pdf file directly
                        mQuitAppWhenDoneViewing = true;
                        onEditUriSelected(uri.toString());
                        return;
                    }
                }

                // could be office or image files
                String type = intent.getType();
                if (data != null && type != null && Utils.isMimeTypeHandled(type)) {
                    onOfficeUriSelected(data);
                    return;
                }
                if (uri != null && Utils.uriHasReadPermission(this, uri) && Utils.isMimeTypeHandled(type)) {
                    onOfficeUriSelected(uri);
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.alert)
                    .setMessage(R.string.error_opening_doc_message)
                    .setPositiveButton(R.string.ok, null)
                    .create().show();
            }
        }
    }

    private boolean openLocalCopyFromUri(Uri dataUri) {
        // gets the display name, fallback to "download_file"
        String title = getContentUriName(dataUri);
        File contentFile = Utils.duplicateInDownload(this, dataUri, title);
        if (contentFile != null) {
            mQuitAppWhenDoneViewing = true;
            onFileSelected(contentFile, "");
            return true;
        }

        return false;
    }

    public String getContentUriName(@Nullable Uri dataUri) {
        String tempContentFilename = Utils.getUriDisplayName(this, dataUri);
        if (tempContentFilename == null && dataUri != null) {
            tempContentFilename = dataUri.getLastPathSegment();
        }
        if (Utils.isNullOrEmpty(tempContentFilename)) {
            tempContentFilename = "download_file.pdf";
        }

        if (!Utils.isExtensionHandled(Utils.getExtension(tempContentFilename))) {
            tempContentFilename += ".pdf";
        }

        return tempContentFilename;
    }

    @Override
    public void onNewIntent(Intent intent) {
        Logger.INSTANCE.LogV("LifeCycle", "Main.onNewIntent, " + getTaskId());

        super.onNewIntent(intent);

        setIntent(intent);

        // On a new intent call we don't want to have the recent list as a default view, since this
        // can be the case we are coming back from the viewer. Set to none so Android will pick up
        // the right fragment if it is the case.
        mProcessedFragmentViewId = MENU_ITEM_NONE;
        int currentProgressedFragmentViewId = mProcessedFragmentViewId;
        processIntent(intent);
        if (currentProgressedFragmentViewId != mProcessedFragmentViewId) {
            updateNavTab();
        }
    }

    @Override
    protected void onStart() {
        Logger.INSTANCE.LogV("LifeCycle", "Main.onStart, " + getTaskId());
        super.onStart();

        updateDrawerLayout();
    }

    @Override
    protected void onStop() {
        Logger.INSTANCE.LogV("LifeCycle", "Main.onStop");

        super.onStop();
    }

    @Override
    protected void onResume() {
        Logger.INSTANCE.LogV("LifeCycle", "Main.onResume");

        super.onResume();

        // if all tabs are closed from another instance of the app
        if (mPdfViewCtrlTabHostFragment != null && mPdfViewCtrlTabHostFragment.getCurrentPdfViewCtrlFragment() == null) {
            mProcessedFragmentViewId = mBrowserProcessedFragmentViewId;
        }

        if (mTeachNavDrawer) {
            // Teach the user where the nav drawer is (only done once, after returning from GettingStarted file)
            if (mDrawerLayout != null && mNavigationDrawerView != null && !mDrawerLayout.isDrawerOpen(mNavigationDrawerView)) {
                // Open drawer, after a small delay
                mDrawerHandler.removeCallbacksAndMessages(null);
                mDrawerHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        toggleNavigationDrawer(true);
                    }
                }, TEACH_NAVIGATION_DRAWER_DELAY);
            }
            mTeachNavDrawer = false;
        }

        if (!Utils.hasStoragePermission(this)) {
            // START REQUEST PERMISSIONS
            // Check if we asked before first
            // If so, display full screen permission
            if (SettingsManager.getStoragePermissionHasBeenAsked(this)) {
                // As we NEED these permissions to proceed, display full screen explain permissions
                CriticalPermissionDialogFragment fragment = CriticalPermissionDialogFragment.newInstance(!SettingsManager.getStoragePermissionDenied(this));
                fragment.setListener(this);
                fragment.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomAppTheme);
                fragment.show(getSupportFragmentManager(), FRAG_TAG_PERMISSION_SCREEN);
            } else {
                Utils.requestStoragePermissions(this, null, RequestCode.STORAGE_1);
            }
            // END REQUEST PERMISSIONS
        } else {
            // first check if we need to dismiss the permission screen
            dismissDialogFragment(FRAG_TAG_PERMISSION_SCREEN);

            // So, if this is the first time and the getting started file was
            // copied properly, we launch the viewer with the document.
            if (isFirstTimeRun()) {
                copyTutorialFile();
            }

            //TODO: Remove the following if no Getting Started, and wish to use onboarding
            /*if (isFirstTimeRun && mGettingStartedFile != null) {
                // Open navigation drawer the next time onResume is called
                mTeachNavDrawer = true;
                onFileSelected(mGettingStartedFile, "");
            } else {
                // Try to update fragment since underlying data has changed
            reloadBrowser();
            }*/
        }
    }

    @Override
    protected void onResumeFragments() {
        // Note: put all transactions after onCreate life cycle (i.e. onActivityResult, onStart,
        // and onResume) here (or in onPostResume) otherwise activity state may be restored thereafter.
        // In other words, this method is guaranteed to be called after the Activity has been
        // restored to its original state, and therefore avoid the possibility of state loss all together.

        Logger.INSTANCE.LogV("LifeCycle", "Main.onResumeFragments");

        super.onResumeFragments();
        mOnResumeFragmentsCalled = true;

        if (mHandleOpenDocError) {
            handleOpenDocError();
            mHandleOpenDocError = false;
        } else if (mHandleLastTabClosed) {
            handleLastTabClosed();
            mHandleLastTabClosed = false;
        } else if (mTabbedHostBundle != null) {
            startTabHostFragment(mTabbedHostBundle);
            mTabbedHostBundle = null;
        } else if (mNavigationDrawerView != null && mNavigationDrawerView.getMenu() != null) {
            MenuItem menuItem = null;
            if (isFirstTimeRun()) {
                mIsFirstTimeRunConsumed = false; // consumed
                menuItem = mNavigationDrawerView.getMenu().findItem(R.id.item_file_list);
            } else if (mProcessedFragmentViewId != MENU_ITEM_NONE) {
                menuItem = mNavigationDrawerView.getMenu().findItem(mProcessedFragmentViewId);
            }
            if (menuItem != null) {
                selectNavigationItem(menuItem);
            }
        }

        if (mReturnFromSettings) {
            mReturnFromSettings = false;
            if (mProcessedFragmentViewId == R.id.item_viewer) {
                if (PdfViewCtrlTabsManager.getInstance().getLatestViewedTabTag(this) != null) {
                    startTabHostFragment(null);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        Logger.INSTANCE.LogV("LifeCycle", "Main.onPause");

        updateNavTab();
        updateBrowserNavTab();

        super.onPause();
    }

    @Override
    public void onTrimMemory(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            ImageMemoryCache.getInstance().clearCache();
            ThumbnailPathCacheManager.getInstance().cleanupResources(this);
            if (level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
                ImageMemoryCache.getInstance().clearAll();
                PathPool.getInstance().clear();
            }
            Logger.INSTANCE.LogE(TAG, "Trim memory, level: " + level);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        MiscUtils.handleLowMemory(this);
        Logger.INSTANCE.LogE(TAG, "low memory");
    }

    @Override
    protected void onDestroy() {
        Logger.INSTANCE.LogV("LifeCycle", "Main.onDestroy");
        super.onDestroy();
        ThumbnailPathCacheManager.getInstance().cleanupResources(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCode.SETTINGS) {
            mReturnFromSettings = true; // do necessary tasks in onResumeFragments after restoring activity state
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RequestCode.STORAGE_1 ||
            requestCode == RequestCode.STORAGE_2) {
            if (requestCode == RequestCode.STORAGE_1) {
                SettingsManager.updateStoragePermissionHasBeenAsked(this, true);
            }
            if (MiscUtils.verifyPermissions(grantResults)) {
                if (!SettingsManager.getFirstTimeRun(this)) {
                    // avoid showing this as we will display getting started
                    MiscUtils.showPermissionResultSnackbar(this, mDrawerLayout, true, requestCode);
                    reloadBrowser();
                    copyTutorialFile();
                }
            } else {
                if (requestCode == RequestCode.STORAGE_2) {
                    SettingsManager.updateStoragePermissionDenied(this, true);
                }
            }
        } else if (requestCode == RequestCode.RECORD_AUDIO) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(SoundDialogFragment.TAG);
            if (fragment != null && fragment instanceof SoundDialogFragment) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem menuItem) {
        if (mDrawerLayout != null && mNavigationDrawerView != null) {
            // Perform the fragment change when the drawer has finished closing
            mNextFragmentViewMenuItem = menuItem;
            toggleNavigationDrawer(false);
            if (Utils.isLargeScreenWidth(this)) {
                handleDrawerClosed(mNavigationDrawerView);
            }
        } else {
            // Calling the callback will change the fragment right away,
            // making the drawer closing effect to stutter. We try to
            // avoid that by postponing the callback a bit.
            mDrawerHandler.removeCallbacksAndMessages(null);
            mDrawerHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    selectNavigationItem(menuItem);
                }
            }, SELECT_NAVIGATION_ITEM_DELAY);
        }

        return true;
    }

    public void selectNavigationItem(MenuItem menuItem) {
        if (menuItem == null) {
            return;
        }

        int navItemId = menuItem.getItemId();
        // Update the the currently checked item
        if (mNavigationDrawerView != null && mNavigationDrawerView.getMenu() != null) {
            if (navItemId != R.id.item_settings) {
                selectNavItem(navItemId);
            } else {
                // Selected item should not be checked, keep old item
                selectNavItem(mCurrentFragment);
            }
        }

        Fragment fragment = null;
        // Let's first check if the selected fragment is already on the view
        boolean replaceFragment = false;

        if (navItemId == R.id.item_viewer) {
            if (!(mCurrentFragment instanceof PdfViewCtrlTabHostFragment)) {
                if (mPdfViewCtrlTabHostFragment == null) {
                    if (PdfViewCtrlTabsManager.getInstance().getLatestViewedTabTag(this) != null) {
                        startTabHostFragment(null);
                    } else {
                        onLastTabClosed();
                    }
                    replaceFragment = false;
                } else {
                    startTabHostFragment(null);
                    replaceFragment = false;
                }
            }
        } else if (navItemId == R.id.item_recent) {
            if (!(mCurrentFragment instanceof RecentViewFragment)) {
                if (mLastAddedBrowserFragment instanceof RecentViewFragment) {
                    fragment = mLastAddedBrowserFragment;
                } else {
                    fragment = RecentViewFragment.newInstance();
                    ((RecentViewFragment) fragment).setRecentViewFragmentListener(
                        new RecentViewFragment.RecentViewFragmentListener() {
                            @Override
                            public void onRecentShown() {
                            }

                            @Override
                            public void onRecentHidden() {
                            }
                        });
                }
                replaceFragment = true;
                setTitle(R.string.title_item_recent);
            }
        } else if (navItemId == R.id.item_favorites) {
            if (!(mCurrentFragment instanceof FavoritesViewFragment)) {
                if (mLastAddedBrowserFragment instanceof FavoritesViewFragment) {
                    fragment = mLastAddedBrowserFragment;
                } else {
                    fragment = FavoritesViewFragment.newInstance();
                    ((FavoritesViewFragment) fragment).setFavoritesViewFragmentListener(
                        new FavoritesViewFragment.FavoritesViewFragmentListener() {
                            @Override
                            public void onFavoritesShown() {
                            }

                            @Override
                            public void onFavoritesHidden() {
                            }
                        });
                }
                replaceFragment = true;
                setTitle(R.string.title_item_favorites);
            }
        } else if (navItemId == R.id.item_folder_list) {
            if (!(mCurrentFragment instanceof LocalFolderViewFragment)) {
                if (mLastAddedBrowserFragment instanceof LocalFolderViewFragment) {
                    fragment = mLastAddedBrowserFragment;
                } else {
                    // Load last used folder, if set
                    fragment = LocalFolderViewFragment.newInstance();
                    ((LocalFolderViewFragment) fragment).setLocalFolderViewFragmentListener(
                        new LocalFolderViewFragment.LocalFolderViewFragmentListener() {
                            @Override
                            public void onLocalFolderShown() {
                            }

                            @Override
                            public void onLocalFolderHidden() {
                            }
                        });
                }
                replaceFragment = true;
                setTitle(R.string.title_item_local_folder_list);
            }
        } else if (navItemId == R.id.item_external_storage) {
            if (Utils.isLollipop()) {
                if (!(mCurrentFragment instanceof ExternalStorageViewFragment)) {
                    if (mLastAddedBrowserFragment instanceof ExternalStorageViewFragment) {
                        fragment = mLastAddedBrowserFragment;
                    } else {
                        // Load last used folder, if set
                        fragment = ExternalStorageViewFragment.newInstance();
                        ((ExternalStorageViewFragment) fragment).setExternalStorageViewFragmentListener(
                            new ExternalStorageViewFragment.ExternalStorageViewFragmentListener() {
                                @Override
                                public void onExternalStorageShown() {
                                }

                                @Override
                                public void onExternalStorageHidden() {
                                }
                            });
                    }

                    replaceFragment = true;
                    setTitle(R.string.external_storage);
                }
            }
        } else if (navItemId == R.id.item_settings) {
            // some settings like multi-tap, full-screen mode might have been updated, so we have
            // to create Host Fragment from scratch
            if (mPdfViewCtrlTabHostFragment != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                Logger.INSTANCE.LogD(TAG, "remove " + mPdfViewCtrlTabHostFragment);
                ft.remove(mPdfViewCtrlTabHostFragment);
                ft.commit();
                mPdfViewCtrlTabHostFragment = null;
            }
            startActivityForResult(new Intent().setClass(this, SettingsActivity.class), RequestCode.SETTINGS);
        } else if (navItemId == R.id.item_exit) {
            finish();
        } else {
            if (!(mCurrentFragment instanceof LocalFileViewFragment)) {
                if (mLastAddedBrowserFragment instanceof LocalFileViewFragment) {
                    fragment = mLastAddedBrowserFragment;
                } else {
                    fragment = LocalFileViewFragment.newInstance();
                    ((LocalFileViewFragment) fragment).setLocalFileViewFragmentListener(
                        new LocalFileViewFragment.LocalFileViewFragmentListener() {
                            @Override
                            public void onLocalFileShown() {
                            }

                            @Override
                            public void onLocalFileHidden() {
                            }
                        });
                }
                replaceFragment = true;
                setTitle(R.string.title_item_local_file_list);
            }
        }

        if (replaceFragment) {
            mProcessedFragmentViewId = navItemId;
            if (mProcessedFragmentViewId != R.id.item_viewer) {
                mBrowserProcessedFragmentViewId = mProcessedFragmentViewId;
            }
            boolean needReloadBrowser = doesNeedReloadBrowser();

            startFragment(fragment);

            if (needReloadBrowser) {
                reloadBrowser();
            }
        }
    }

    private void startFragment(Fragment fragment) {
        startFragment(fragment, null);
    }

    @SuppressWarnings("SameParameterValue")
    private void startFragment(Fragment fragment, String tag) {
        if (isFinishing()) {
            return;
        }

        selectNavItem(fragment);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Logger.INSTANCE.LogD(TAG, "replace " + fragment);
        ft.replace(R.id.container, fragment, tag);
        ft.commit();

        List<Fragment> newFragments = getSupportFragmentManager().getFragments();
        ArrayList<Fragment> debugFragments = getFragmentsOnContainer(newFragments);
        Logger.INSTANCE.LogD(TAG, "Fragments on the Container:" + debugFragments.size() + "\n" + debugFragments);

        if (!(fragment instanceof PdfViewCtrlTabHostFragment)) {
            mLastAddedBrowserFragment = fragment;
        }
        setCurrentFragment(fragment);

        toggleInfoDrawer(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_complete_reader, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null) {
            if (item.getItemId() == android.R.id.home) {
                onNavButtonPressed();
            } else if (item.getItemId() == R.id.action_settings) {
                // some settings like multi-tap, full-screen mode might have been updated, so we have
                // to create Host Fragment from scratch
                if (mPdfViewCtrlTabHostFragment != null) {
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    Logger.INSTANCE.LogD(TAG, "remove " + mPdfViewCtrlTabHostFragment);
                    ft.remove(mPdfViewCtrlTabHostFragment);
                    ft.commit();
                    mPdfViewCtrlTabHostFragment = null;
                }
                startActivity(new Intent().setClass(this, SettingsActivity.class));
            }

        }
        // Call through to the super implementation so that fragments can receive the event.
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
        updateDrawerLayout();
    }

    private void updateDrawerLayout() {
        View container = findViewById(R.id.container);
        if (null != container) {
            int drawerWidth = getResources().getDimensionPixelSize(R.dimen.navigation_drawer_width);
            int padding = 0;
            if (Utils.isLargeScreenWidth(this)) {
                toggleNavigationDrawer(true, false);
                padding = drawerWidth;
            } else {
                toggleNavigationDrawer(false, false);
            }
            if (Utils.isJellyBeanMR1()) {
                container.setPaddingRelative(padding, container.getPaddingTop(),
                    container.getPaddingEnd(), container.getPaddingBottom());
            } else {
                boolean isRTL = Utils.isRtlLayout(this);
                container.setPadding(isRTL ? container.getPaddingLeft() : padding, container.getPaddingTop(),
                    isRTL ? padding : container.getPaddingRight(), container.getPaddingBottom());
            }
            toggleNavigationDrawerLockMode(true); // Unlock by default
        }

        if (mCurrentFragment instanceof ToolbarFragment) {
            ((ToolbarFragment) mCurrentFragment).updateToolbarDrawable();
        } else if (mCurrentFragment instanceof PdfViewCtrlTabHostFragment) {
            ((PdfViewCtrlTabHostFragment) mCurrentFragment).updateToolbarDrawable();
        }

        if (mDrawerLayout != null) {
            mDrawerLayout.setDisallowIntercept(Utils.isLargeScreenWidth(this));
        }
    }

    // Permission
    @Override
    public void onPermissionScreenDismiss(boolean exit, boolean askPermission) {
        if (exit) {
            finish();
        } else {
            if (askPermission) {
                Utils.requestStoragePermissions(this, null, RequestCode.STORAGE_2);
            } else {
                startActivity(MiscUtils.getAppSettingsIntent(this));
            }
        }
    }

    public void onEditPdfIntentReceived(Uri dataUri, boolean skipPermission) {
        if (dataUri == null) {
            return;
        }
        String realPath = Utils.getRealPathFromURI(this, dataUri);
        if (!Utils.isNullOrEmpty(realPath)) {
            File currentFile = new File(realPath);
            if (currentFile.exists()) {
                onFileSelected(currentFile, "", true);
                return;
            }
        }

        if (!skipPermission) {
            File externalFile = Utils.convExternalContentUriToFile(this, dataUri);
            if (externalFile != null) {
                onFileSelected(externalFile, "", true);
                return;
            }
        }

        String title = getContentUriName(dataUri);
        String tag = dataUri.toString();
        Bundle args = PdfViewCtrlTabFragment.createBasicPdfViewCtrlTabBundle(
            tag, title, "pdf", "", BaseFileInfo.FILE_TYPE_EDIT_URI);
        startTabHostFragment(args);
    }

    public void onFileSelected(final File file, String password, boolean skipPasswordCheck) {
        boolean openedSucessfully = false;
        if (file != null && file.exists()) {
            if (Utils.isNullOrEmpty(password)) {
                password = Utils.getPassword(this, file.getAbsolutePath());
            }

            if (Utils.isOfficeDocument(file.getAbsolutePath())) {
                String tag = file.getAbsolutePath();
                String title = file.getName();
                String fileExtension = Utils.getExtension(tag);
                Bundle args = PdfViewCtrlTabFragment.createBasicPdfViewCtrlTabBundle(
                    tag, title, fileExtension, password, BaseFileInfo.FILE_TYPE_FILE);
                startTabHostFragment(args);

                return;
            }

            CheckDifferentFileTypeResult result = checkDifferentFileType(file, password, BaseFileInfo.FILE_TYPE_FILE, "", skipPasswordCheck);

            if (result.getOpenDocument()) {
                // Perform any operation needed on the current fragment before launching the viewer.
                if (mCurrentFragment != null && hasMainActivityListener(mCurrentFragment)) {
                    ((MainActivityListener) mCurrentFragment).onPreLaunchViewer();
                }
            }

            if (result.getOpenDocument() // PDF document
                || FileManager.checkIfFileTypeIsInList(file.getAbsolutePath())) { // non-PDF document
                String tag = file.getAbsolutePath();
                String title = file.getName();
                String fileExtension = Utils.getExtension(tag);
                Bundle args = PdfViewCtrlTabFragment.createBasicPdfViewCtrlTabBundle(
                    tag, title, fileExtension, password, BaseFileInfo.FILE_TYPE_FILE);
                startTabHostFragment(args);
                openedSucessfully = true;
            }
        } else {
            Utils.showAlertDialog(this, R.string.file_does_not_exist_message, R.string.error_opening_file);
        }

        if (!openedSucessfully) {
            // Update recent and favorite files lists
            FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, file);
            RecentFilesManager.getInstance().removeFile(this, fileInfo);
            FavoriteFilesManager.getInstance().removeFile(this, fileInfo);
            if (file != null) {
                PdfViewCtrlTabsManager.getInstance().removePdfViewCtrlTabInfo(this, file.getAbsolutePath());
            }

            // Try to update fragment since underlying data has changed
            reloadBrowser();

            onOpenDocError();
        }
    }

    @Override
    public void onFileSelected(final File file, String password) {
        onFileSelected(file, password, false);
    }

    @Override
    public void onFolderSelected(String absolutePath) {
        // Update last-used local folder path to the specified path
        PdfViewCtrlSettingsManager.updateLocalFolderPath(this, absolutePath);
        // Update last-used local folder tree to the same path
        PdfViewCtrlSettingsManager.updateLocalFolderTree(this, absolutePath);

        if (mNavigationDrawerView != null && mNavigationDrawerView.getMenu() != null) {
            MenuItem menuItem = mNavigationDrawerView.getMenu().findItem(R.id.item_folder_list);
            selectNavigationItem(menuItem);
        }
    }

    @Override
    public void onExternalFileSelected(String encodedUri, String password) {
        if (!Utils.isKitKat()) {
            return;
        }
        Uri uri = Uri.parse(encodedUri);
        boolean fileExists = Utils.uriHasReadPermission(this, uri);
        if (encodedUri != null && fileExists) {
            if (Utils.isNullOrEmpty(password)) {
                password = Utils.getPassword(this, encodedUri);
            }

            ContentResolver contentResolver = getContentResolver();
            if (contentResolver == null) {
                return;
            }
            if (Utils.isNotPdf(contentResolver, uri)) {
                String fileExtension = Utils.getUriExtension(contentResolver, uri);
                String title = Utils.getUriDisplayName(this, uri);
                Bundle args = PdfViewCtrlTabFragment.createBasicPdfViewCtrlTabBundle(
                    encodedUri, title, fileExtension,
                    password, BaseFileInfo.FILE_TYPE_EXTERNAL);
                startTabHostFragment(args);

                return;
            }

            CheckDifferentFileTypeResult result = checkDifferentExternalFileType(encodedUri, password);

            if (result != null && result.getOpenDocument()) {
                // Perform any operation needed on the current fragment before launching the viewer.
                if (mCurrentFragment != null && hasMainActivityListener(mCurrentFragment)) {
                    ((MainActivityListener) mCurrentFragment).onPreLaunchViewer();
                }

                String fileExtension = Utils.getUriExtension(contentResolver, uri);
                String title = Utils.getUriDisplayName(this, uri);
                Bundle args = PdfViewCtrlTabFragment.createBasicPdfViewCtrlTabBundle(
                    encodedUri, title, fileExtension,
                    password, BaseFileInfo.FILE_TYPE_EXTERNAL);
                startTabHostFragment(args);
            }
        } else {
            Utils.showAlertDialog(this, R.string.file_does_not_exist_message, R.string.error_opening_file);

            if (encodedUri != null) {
                String name = Utils.getUriDisplayName(this, uri);
                if (Utils.isNullOrEmpty(name)) {
                    // cannot get display name from content resolver but we want to remove it from
                    // Recent files anyway, so lets try to retrieve it directly from URI, maybe it works
                    name = FilenameUtils.getBaseName(uri.getPath());
                }
                if (!Utils.isNullOrEmpty(name)) {
                    // Update recent and favorite files lists
                    FileInfo fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EXTERNAL, encodedUri, name, false, 1);
                    RecentFilesManager.getInstance().removeFile(this, fileInfo);
                    FavoriteFilesManager.getInstance().removeFile(this, fileInfo);
                    PdfViewCtrlTabsManager.getInstance().removePdfViewCtrlTabInfo(this, encodedUri);
                }
            }

            // Try to update fragment since underlying data has changed
            reloadBrowser();
            if (mCurrentFragment != null) {
                selectNavItem(mCurrentFragment);
            }

            onOpenDocError();
        }
    }

    @Override
    public void onExternalFolderSelected(String folderUri) {
        if (null != folderUri) {
            // Update last-used external folder uri to the specified uri
            PdfViewCtrlSettingsManager.updateSavedExternalFolderUri(this, folderUri);
            // Reset external folder tree uri
            PdfViewCtrlSettingsManager.updateSavedExternalFolderTreeUri(this, PdfViewCtrlSettingsManager.KEY_PREF_SAVED_EXTERNAL_FOLDER_TREE_URI_DEFAULT_VALUE);
        }

        if (mNavigationDrawerView != null && mNavigationDrawerView.getMenu() != null) {
            MenuItem menuItem = mNavigationDrawerView.getMenu().findItem(R.id.item_external_storage);
            selectNavigationItem(menuItem);
        }
    }

    @Override
    public void onEditUriSelected(String fileUri) {
        Uri uri = Uri.parse(fileUri);
        onEditPdfIntentReceived(uri, true);
    }

    @Override
    public void onOfficeUriSelected(Uri fileUri) {
        String title = getContentUriName(fileUri);
        String tag = fileUri.toString();
        String fileExtension = Utils.getExtension(title);
        Bundle args = PdfViewCtrlTabFragment.createBasicPdfViewCtrlTabBundle(
            tag, title, fileExtension, "", BaseFileInfo.FILE_TYPE_OFFICE_URI);
        startTabHostFragment(args);
    }

    @Override
    public void gotoExternalTab() {
        onExternalFolderSelected(null);
    }

    private class CheckDifferentFileTypeResult {

        private int fileType;
        private boolean openDocument;
        private String password;

        CheckDifferentFileTypeResult(int fileType, boolean openDocument, String password) {
            this.fileType = fileType;
            this.openDocument = openDocument;
            this.password = password;
        }

        int getFileType() {
            return this.fileType;
        }

        boolean getOpenDocument() {
            return this.openDocument;
        }

        String getPassword() {
            return this.password;
        }
    }

    public int checkPdfDocTypeAndClose(PDFDoc tempDoc, final String password) {
        int fileType = PDFDOC_TYPE_FILE;
        boolean shouldUnlock = false;
        try {
            tempDoc.lock();
            shouldUnlock = true;

            // We use a do...while block to control all the steps and/or
            // verifications needed before opening the document, so we
            // can easily return if one of the steps is not met.
            //noinspection ConstantConditions
            do {
                // Is this doc password protected?
                if (!tempDoc.initSecurityHandler()) {
                    if (!tempDoc.initStdSecurityHandler(password)) {
                        fileType = PDFDOC_TYPE_ENCRYPTED;
                        break;
                    } else {
                        mPasswordAttemptCounter = 0;
                    }
                }

                // Does this doc need XFA rendering?
                Obj needsRenderingObj = tempDoc.getRoot().findObj("NeedsRendering");
                if (needsRenderingObj != null && needsRenderingObj.isBool() && needsRenderingObj.getBool()) {
                    fileType = PDFDOC_TYPE_XFA;
                    break;
                }

                // Is this doc a package/portfolio?
                Obj collectionObj = tempDoc.getRoot().findObj("Collection");
                if (collectionObj != null) {
                    fileType = PDFDOC_TYPE_PORTFOLIO;
                    break;
                }
            } while (false);

        } catch (Exception e) {
            fileType = PDFDOC_TYPE_UNKNOWN;
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            try {
                if (shouldUnlock) {
                    tempDoc.unlock();
                }
            } catch (Exception e) {
                fileType = PDFDOC_TYPE_UNKNOWN;
            }
            try {
                if (tempDoc != null) {
                    tempDoc.close();
                }
            } catch (Exception e) {
                fileType = PDFDOC_TYPE_UNKNOWN;
            }
        }
        return fileType;
    }

    public CheckDifferentFileTypeResult checkDifferentExternalFileType(String encodedUri, final String password) {
        if (encodedUri == null) {
            return null;
        }

        boolean openDocument;
        final Uri fileUri = Uri.parse(encodedUri);
        SecondaryFileFilter fileFilter = null;
        try {
            openDocument = Utils.uriHasReadPermission(this, fileUri);
            if (openDocument) {
                fileFilter = new SecondaryFileFilter(this, fileUri);
                PDFDoc tempDoc = new PDFDoc(fileFilter);
                int pdfDocType = checkPdfDocTypeAndClose(tempDoc, password);
                fileFilter = null;
                switch (pdfDocType) {
                    case PDFDOC_TYPE_UNKNOWN:
                        openDocument = false;
                        String msg = String.format(getString(R.string.error_corrupt_file_message), getString(R.string.app_name));
                        Utils.showAlertDialog(this, msg, getString(R.string.error_opening_file));
                        break;
                    case PDFDOC_TYPE_ENCRYPTED:
                        openDocument = false;
                        if (mPasswordAttemptCounter >= MAX_PASSWORD_ATTEMPTS) {
                            mPasswordAttemptCounter = 0;
                            Utils.showAlertDialog(this, R.string.password_not_valid_message, R.string.error);
                        } else {
                            mPasswordAttemptCounter++;

                            // Show password Dialog
                            FragmentManager fm = getSupportFragmentManager();
                            PasswordDialogFragment passwordDialog = PasswordDialogFragment.newInstance(BaseFileInfo.FILE_TYPE_EXTERNAL, null, encodedUri, "");
                            passwordDialog.setListener(this);
                            if (mPasswordAttemptCounter == 1) {
                                passwordDialog.setMessage(R.string.dialog_password_message);
                            } else {
                                passwordDialog.setMessage(R.string.password_not_valid_message);
                            }
                            passwordDialog.show(fm, "password_dialog");
                        }
                        break;
                    case PDFDOC_TYPE_PORTFOLIO:
                        openDocument = false;
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(R.string.pdf_portfolio)
                            .setMessage(R.string.pdf_portfolio_message)
                            .setCancelable(true)
                            .setPositiveButton(R.string.tools_misc_yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    FragmentManager fm = getSupportFragmentManager();
                                    PortfolioDialogFragment portfolioDialog = PortfolioDialogFragment.newInstance(fileUri, password);
                                    portfolioDialog.show(fm, "portfolio_dialog");
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                        builder.create().show();
                        break;
                    case PDFDOC_TYPE_XFA:
                        openDocument = false;
                        Utils.showAlertDialogWithLink(this, getString(R.string.error_has_xfa_forms_message), "");
                        break;
                }
            }
        } catch (Exception e) {
            openDocument = false;
            String msg = String.format(getString(R.string.error_corrupt_file_message), getString(R.string.app_name));
            Utils.showAlertDialog(this, msg, getString(R.string.error_opening_file));
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(fileFilter);
        }
        return new CheckDifferentFileTypeResult(BaseFileInfo.FILE_TYPE_EXTERNAL, openDocument, password);
    }

    public CheckDifferentFileTypeResult checkDifferentFileType(final File file, final String password, final int type, final String tag, final boolean skipPasswordCheck) {
        boolean openDocument = true;
        PDFDoc tempDoc = null;
        boolean shouldUnlock = false;

        try {
            tempDoc = new PDFDoc(file.getAbsolutePath());
            tempDoc.lock();
            shouldUnlock = true;

            // We use a do...while block to control all the steps and/or
            // verifications needed before opening the document, so we
            // can easily return if one of the steps is not met.
            //noinspection ConstantConditions
            do {
                // Is this doc password protected?
                if (!tempDoc.initSecurityHandler()) {
                    if (!tempDoc.initStdSecurityHandler(password)) {
                        openDocument = false;
                        if (skipPasswordCheck) {
                            openDocument = true;
                        } else {
                            if (mPasswordAttemptCounter >= MAX_PASSWORD_ATTEMPTS) {
                                mPasswordAttemptCounter = 0;

                                Utils.showAlertDialog(this, R.string.password_not_valid_message, R.string.error);

                            } else {
                                mPasswordAttemptCounter++;

                                // Show password Dialog
                                FragmentManager fm = getSupportFragmentManager();
                                PasswordDialogFragment passwordDialog = PasswordDialogFragment.newInstance(type, file, FilenameUtils.removeExtension(file.getName()), tag);
                                passwordDialog.setListener(this);
                                if (mPasswordAttemptCounter == 1) {
                                    passwordDialog.setMessage(R.string.dialog_password_message);
                                } else {
                                    passwordDialog.setMessage(R.string.password_not_valid_message);
                                }
                                passwordDialog.show(fm, "password_dialog");
                            }
                        }
                        break;
                    } else {
                        mPasswordAttemptCounter = 0;
                    }
                }

                // Does this doc need XFA rendering?
                Obj needsRenderingObj = tempDoc.getRoot().findObj("NeedsRendering");
                if (needsRenderingObj != null && needsRenderingObj.isBool() && needsRenderingObj.getBool()) {
                    openDocument = false;
                    Utils.showAlertDialogWithLink(this, getString(R.string.error_has_xfa_forms_message), "");
                    break;
                }

                // Is this doc a package/portfolio?
                Obj collectionObj = tempDoc.getRoot().findObj("Collection");
                if (collectionObj != null) {
                    openDocument = false;
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.pdf_portfolio)
                        .setMessage(R.string.pdf_portfolio_message)
                        .setCancelable(true)
                        .setPositiveButton(R.string.tools_misc_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                FragmentManager fm = getSupportFragmentManager();
                                PortfolioDialogFragment portfolioDialog = PortfolioDialogFragment.newInstance(file, password);
                                portfolioDialog.show(fm, "portfolio_dialog");
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                    builder.create().show();
                    break;
                }
            } while (false);

        } catch (PDFNetException e) {
            openDocument = false;
            if (!FileManager.checkIfFileTypeIsInList(file.getAbsolutePath())) {
                String msg = String.format(getString(R.string.error_corrupt_file_message), getString(R.string.app_name));
                Utils.showAlertDialog(this, msg, getString(R.string.error_opening_file));
            }
            if (e.getMessage() != null && !e.getMessage().equals("Header not found")) {
                // "Header not found" means the file has been modified and no longer being a PDF document
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        } catch (Exception e) {
            openDocument = false;
            String msg = String.format(getString(R.string.error_corrupt_file_message), getString(R.string.app_name));
            Utils.showAlertDialog(this, msg, getString(R.string.error_opening_file));
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            try {
                if (shouldUnlock) {
                    tempDoc.unlock();
                }
            } catch (Exception e) {
                openDocument = false;
            }
            try {
                if (tempDoc != null) {
                    tempDoc.close();
                }
            } catch (Exception e) {
                openDocument = false;
            }
        }
        return new CheckDifferentFileTypeResult(type, openDocument, password);
    }

    public void dismissDialogFragment(String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment != null) {
            ((DialogFragment) fragment).dismissAllowingStateLoss();
        }
    }

    private boolean hasMainActivityListener(Fragment fragment) {
        return (fragment instanceof MainActivityListener);
    }


    @Override
    public void onPasswordDialogPositiveClick(int fileType, File file, String path, String password, String id) {
        if (fileType == BaseFileInfo.FILE_TYPE_EXTERNAL) {
            onExternalFileSelected(path, password);
        } else {
            onFileSelected(file, password);
        }
    }

    @Override
    public void onPasswordDialogNegativeClick(int fileType, File file, String path) {
        mPasswordAttemptCounter = 0;
        if (mCurrentFragment == null
            || (mCurrentFragment instanceof PdfViewCtrlTabHostFragment
            && mCurrentFragment.getView() != null
            && ((PdfViewCtrlTabHostFragment) mCurrentFragment).getTabCount() <= 1)) {
            onLastTabClosed();
        }
    }

    @Override
    public void onPasswordDialogDismiss(boolean forcedDismiss) {
        if (!forcedDismiss) {
            mPasswordAttemptCounter = 0;
            if (mCurrentFragment == null
                || (mCurrentFragment instanceof PdfViewCtrlTabHostFragment
                && mCurrentFragment.getView() != null
                && ((PdfViewCtrlTabHostFragment) mCurrentFragment).getTabCount() <= 1)) {
                onLastTabClosed();
            }
        }
    }

    @Override
    public void onPortfolioDialogFragmentFileClicked(int fileType, PortfolioDialogFragment dialog, String fileName) {
        // First do a quick check if it is a PDF file or other file type we can handle
        String extension = Utils.getExtension(fileName);
        if (Utils.isExtensionHandled(extension)) {
            // Then extract the file and open it
            if (fileType == PortfolioDialogFragment.FILE_TYPE_FILE) {
                String fullFileName = MiscUtils.extractFileFromPortfolio(dialog.getPortfolioFile(), fileName);
                if (!Utils.isNullOrEmpty(fullFileName)) {
                    File file = new File(fullFileName);
                    onFileSelected(file, "");
                }
            } else {
                String fullFileName = MiscUtils.extractFileFromPortfolio(this, dialog.getPortfolioFileUri(), fileName);
                if (!Utils.isNullOrEmpty(fullFileName)) {
                    onExternalFileSelected(fullFileName, "");
                }
            }
        } else {
            if (fileType == PortfolioDialogFragment.FILE_TYPE_FILE) {
                String fullFileName = MiscUtils.extractFileFromPortfolio(dialog.getPortfolioFile(), fileName);
                if (!Utils.isNullOrEmpty(fullFileName)) {
                    File file = new File(fullFileName);
                    Uri uri = Utils.getUriForFile(this, file);
                    if (uri != null) {
                        Utils.shareGenericFile(this, uri);
                    }
                }
            } else {
                String fullFileName = MiscUtils.extractFileFromPortfolio(this, dialog.getPortfolioFileUri(), fileName);
                if (!Utils.isNullOrEmpty(fullFileName)) {
                    Uri fileUri = Uri.parse(fullFileName);
                    Utils.shareGenericFile(this, fileUri);
                }
            }
        }
        // reload file browser
        reloadBrowser();
    }

    @Override
    public FileInfoDrawer showFileInfoDrawer(final FileInfoDrawer.Callback callback) {
        FileInfoDrawerFragment fragment = (FileInfoDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.file_info_drawer);
        if (fragment == null) {
            // The FileInfoDrawerFragment has not been created yet.
            fragment = FileInfoDrawerFragment.newInstance();

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.file_info_drawer, fragment, FRAG_TAG_FILE_INFO_DRAWER);
            ft.commit();
        }
        if (mFileInfoDrawer == null) {
            mFileInfoDrawer = new FileInfoDrawer(this, mDrawerLayout, mFileInfoDrawerView, fragment);

            final int width;
            final int height;
            if (!Utils.isTablet(this)) {
                // Set the drawer's width to be the smallest screen dimension
                Point displaySize = new Point();
                Utils.getDisplaySize(this, displaySize);
                int size = Math.min(displaySize.x, displaySize.y);
                int standardIncrement = (int) (getResources().getDisplayMetrics().density * 64); // 64 dp
                width = size;
                height = size - standardIncrement;
            } else {
                width = height = getResources().getDimensionPixelSize(R.dimen.navigation_drawer_width);
            }
            // Apply dimensions after the fragment's view has been laid out.
            mFileInfoDrawerView.post(new Runnable() {
                @Override
                public void run() {
                    mFileInfoDrawer.setDimensions(width, height);
                }
            });
        }

        // Wait until after the drawer content is laid out before showing.
        // (Also waiting until the dimensions are applied, above).
        mFileInfoDrawerView.post(new Runnable() {
            @Override
            public void run() {
                mFileInfoDrawer.show(callback);
            }
        });
        return mFileInfoDrawer;
    }

    private void startTabHostFragment(Bundle args) {
        if (isFinishing()) {
            return;
        }

        if (args == null) {
            args = new Bundle();
        }
        args.putBoolean(PdfViewCtrlTabHostFragment.BUNDLE_TAB_HOST_QUIT_APP_WHEN_DONE_VIEWING, mQuitAppWhenDoneViewing);
        if (args.containsKey(BUNDLE_TAB_TITLE)) {
            String title = args.getString(BUNDLE_TAB_TITLE);
            if (mLastAddedBrowserFragment != null && mLastAddedBrowserFragment instanceof FileBrowserViewFragment) {
                ((FileBrowserViewFragment) mLastAddedBrowserFragment).setCurrentFile(title);
            }
        }
        mProcessedFragmentViewId = R.id.item_viewer;
        selectNavItem(mProcessedFragmentViewId);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        mPdfViewCtrlTabHostFragment = PdfViewCtrlTabHostFragment.newInstance(args);
        mPdfViewCtrlTabHostFragment.addHostListener(this);

        Logger.INSTANCE.LogD(TAG, "replace with " + mPdfViewCtrlTabHostFragment);
        ft.replace(R.id.container, mPdfViewCtrlTabHostFragment, null);
        ft.commit();

        setCurrentFragment(mPdfViewCtrlTabHostFragment);

        updateNavTab(); // update navigation tab in case the activity will be resumed

        toggleInfoDrawer(false);
    }

    private void loadFragmentViewIds() {
        String navTab = SettingsManager.getNavTab(this);

        switch (navTab) {
            case SettingsManager.KEY_PREF_NAV_TAB_VIEWER:
                mProcessedFragmentViewId = R.id.item_viewer;
                break;
            case SettingsManager.KEY_PREF_NAV_TAB_RECENT:
                mProcessedFragmentViewId = R.id.item_recent;
                break;
            case SettingsManager.KEY_PREF_NAV_TAB_FAVORITES:
                mProcessedFragmentViewId = R.id.item_favorites;
                break;
            case SettingsManager.KEY_PREF_NAV_TAB_FOLDERS:
                mProcessedFragmentViewId = R.id.item_folder_list;
                break;
            case SettingsManager.KEY_PREF_NAV_TAB_FILES:
                mProcessedFragmentViewId = R.id.item_file_list;
                break;
            case SettingsManager.KEY_PREF_NAV_TAB_EXTERNAL:
                mProcessedFragmentViewId = R.id.item_external_storage;
                break;
            default:
                mProcessedFragmentViewId = R.id.item_file_list;
                break;
        }

        String browserNavTab = SettingsManager.getBrowserNavTab(this);
        switch (browserNavTab) {
            case SettingsManager.KEY_PREF_NAV_TAB_RECENT:
                mBrowserProcessedFragmentViewId = R.id.item_recent;
                break;
            case SettingsManager.KEY_PREF_NAV_TAB_FAVORITES:
                mBrowserProcessedFragmentViewId = R.id.item_favorites;
                break;
            case SettingsManager.KEY_PREF_NAV_TAB_FOLDERS:
                mBrowserProcessedFragmentViewId = R.id.item_folder_list;
                break;
            case SettingsManager.KEY_PREF_NAV_TAB_FILES:
                mBrowserProcessedFragmentViewId = R.id.item_file_list;
                break;
            case SettingsManager.KEY_PREF_NAV_TAB_EXTERNAL:
                mBrowserProcessedFragmentViewId = R.id.item_external_storage;
                break;
            default:
                mBrowserProcessedFragmentViewId = R.id.item_file_list;
                break;
        }
    }

    private void updateNavTab() {
        if (mCurrentFragment != null) {
            String navTab = SettingsManager.KEY_PREF_NAV_TAB_DEFAULT_VALUE;
            try {
                if (mCurrentFragment instanceof PdfViewCtrlTabHostFragment) {
                    navTab = SettingsManager.KEY_PREF_NAV_TAB_VIEWER;
                } else if (mCurrentFragment instanceof RecentViewFragment) {
                    navTab = SettingsManager.KEY_PREF_NAV_TAB_RECENT;
                } else if (mCurrentFragment instanceof FavoritesViewFragment) {
                    navTab = SettingsManager.KEY_PREF_NAV_TAB_FAVORITES;
                } else if (mCurrentFragment instanceof LocalFolderViewFragment) {
                    navTab = SettingsManager.KEY_PREF_NAV_TAB_FOLDERS;
                } else if (mCurrentFragment instanceof LocalFileViewFragment) {
                    navTab = SettingsManager.KEY_PREF_NAV_TAB_FILES;
                } else if (mCurrentFragment instanceof ExternalStorageViewFragment) {
                    navTab = SettingsManager.KEY_PREF_NAV_TAB_EXTERNAL;
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
                navTab = SettingsManager.KEY_PREF_NAV_TAB_DEFAULT_VALUE;
            }

            SettingsManager.updateNavTab(this, navTab);
        }
    }

    private void updateBrowserNavTab() {
        if (mCurrentFragment != null) {
            String browserNavTab = getNavTabFromId(mBrowserProcessedFragmentViewId);
            SettingsManager.updateBrowserNavTab(this, browserNavTab);
        }
    }

    private String getNavTabFromId(int id) {
        String browserNavTab = SettingsManager.KEY_PREF_NAV_TAB_DEFAULT_VALUE;
        if (id == R.id.item_recent) {
            browserNavTab = SettingsManager.KEY_PREF_NAV_TAB_RECENT;
        }
        if (id == R.id.item_favorites) {
            browserNavTab = SettingsManager.KEY_PREF_NAV_TAB_FAVORITES;
        }
        if (id == R.id.item_folder_list) {
            browserNavTab = SettingsManager.KEY_PREF_NAV_TAB_FOLDERS;
        }
        if (id == R.id.item_file_list) {
            browserNavTab = SettingsManager.KEY_PREF_NAV_TAB_FILES;
        }
        if (id == R.id.item_external_storage) {
            browserNavTab = SettingsManager.KEY_PREF_NAV_TAB_EXTERNAL;
        }

        return browserNavTab;
    }

    private void setCurrentFragment(Fragment fragment) {
        mCurrentFragment = fragment;
    }

    private ArrayList<Fragment> getFragmentsOnContainer(List<Fragment> fragments) {
        ArrayList<Fragment> output = new ArrayList<>();
        if (fragments == null) {
            return output;
        }
        for (Fragment f : fragments) {
            if (f instanceof RecentViewFragment
                || f instanceof FavoritesViewFragment
                || f instanceof LocalFolderViewFragment
                || f instanceof LocalFileViewFragment
                || f instanceof ExternalStorageViewFragment
                || f instanceof PdfViewCtrlTabHostFragment) {
                output.add(f);
            }
        }

        return output;
    }

    private void selectNavItem(int id) {
        // Only one item should be checked

        if (mNavigationDrawerView == null) {
            return;
        }
        Menu navMenu = mNavigationDrawerView.getMenu();
        if (navMenu == null) {
            return;
        }
        for (int i = 0; i < navMenu.size(); i++) {
            MenuItem navItem = navMenu.getItem(i);
            if (navItem.getItemId() == id) {
                navItem.setChecked(true);
            } else {
                navItem.setChecked(false);
            }
        }
    }

    private void selectNavItem(Fragment fragment) {
        int id = R.id.item_recent;
        if (fragment instanceof FavoritesViewFragment) {
            id = R.id.item_favorites;
        } else if (fragment instanceof LocalFolderViewFragment) {
            id = R.id.item_folder_list;
        } else if (fragment instanceof LocalFileViewFragment) {
            id = R.id.item_file_list;
        } else if (fragment instanceof ExternalStorageViewFragment) {
            id = R.id.item_external_storage;
        }
        selectNavItem(id);
    }
}
