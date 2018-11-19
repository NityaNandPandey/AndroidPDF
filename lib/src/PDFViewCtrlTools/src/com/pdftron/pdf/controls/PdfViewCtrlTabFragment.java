//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewStub;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pdftron.common.PDFNetException;
import com.pdftron.common.RecentlyUsedCache;
import com.pdftron.filters.MappedFile;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.DocumentConversion;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFRasterizer;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Print;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.WordToPDFOptions;
import com.pdftron.pdf.annots.FileAttachment;
import com.pdftron.pdf.asynctask.GetTextInPageTask;
import com.pdftron.pdf.asynctask.PDFDocLoaderTask;
import com.pdftron.pdf.config.PDFViewCtrlConfig;
import com.pdftron.pdf.config.ToolManagerBuilder;
import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.model.FreeTextCacheStruct;
import com.pdftron.pdf.model.PdfViewCtrlTabInfo;
import com.pdftron.pdf.tools.AnnotEdit;
import com.pdftron.pdf.tools.AnnotEditLine;
import com.pdftron.pdf.tools.Pan;
import com.pdftron.pdf.tools.QuickMenuItem;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.TextHighlighter;
import com.pdftron.pdf.tools.TextSelect;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.tools.UndoRedoManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AppUtils;
import com.pdftron.pdf.utils.BasicHTTPDownloadTask;
import com.pdftron.pdf.utils.BookmarkManager;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.DialogGoToPage;
import com.pdftron.pdf.utils.FileInfoManager;
import com.pdftron.pdf.utils.ImageMemoryCache;
import com.pdftron.pdf.utils.PageBackButtonInfo;
import com.pdftron.pdf.utils.PathPool;
import com.pdftron.pdf.utils.PdfDocManager;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.PdfViewCtrlTabsManager;
import com.pdftron.pdf.utils.RecentFilesManager;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.ContentLoadingRelativeLayout;
import com.pdftron.sdf.SDFDoc;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static android.graphics.Color.HSVToColor;
import static android.graphics.Color.RGBToHSV;
import static com.pdftron.pdf.config.PDFViewCtrlConfig.MAX_RELATIVE_ZOOM_LIMIT;
import static com.pdftron.pdf.config.PDFViewCtrlConfig.MIN_RELATIVE_ZOOM_LIMIT;

/**
 * The PdfViewCtrlTabFragment shows {@link com.pdftron.pdf.PDFViewCtrl} out of the box with a various
 * of controls such as {@link com.pdftron.pdf.controls.AnnotationToolbar}, {@link com.pdftron.pdf.controls.ThumbnailSlider},
 * {@link com.pdftron.pdf.controls.ThumbnailsViewFragment} etc.
 */
public class PdfViewCtrlTabFragment extends Fragment implements
    PDFViewCtrl.PageChangeListener,
    PDFViewCtrl.DocumentDownloadListener,
    PDFViewCtrl.UniversalDocumentConversionListener,
    PDFViewCtrl.DocumentLoadListener,
    PDFViewCtrl.RenderingListener,
    PDFViewCtrl.UniversalDocumentProgressIndicatorListener,
    ToolManager.PreToolManagerListener,
    ToolManager.QuickMenuListener,
    ToolManager.AnnotationModificationListener,
    ToolManager.PdfDocModificationListener,
    ToolManager.BasicAnnotationListener,
    ToolManager.OnGenericMotionEventListener,
    ToolManager.ToolChangedListener,
    ToolManager.AdvancedAnnotationListener,
    ReflowControl.OnReflowTapListener,
    ThumbnailSlider.OnThumbnailSliderTrackingListener,
    UndoRedoPopupWindow.OnUndoRedoListener {

    private static final String TAG = PdfViewCtrlTabFragment.class.getName();
    protected static boolean sDebug;

    public static final String BUNDLE_TAB_TAG = "bundle_tab_tag";
    public static final String BUNDLE_TAB_TITLE = "bundle_tab_title";
    public static final String BUNDLE_TAB_FILE_EXTENSION = "bundle_tab_file_extension";
    public static final String BUNDLE_TAB_PASSWORD = "bundle_tab_password";
    public static final String BUNDLE_TAB_ITEM_SOURCE = "bundle_tab_item_source";
    public static final String BUNDLE_TAB_CONTENT_LAYOUT = "bundle_tab_content_layout";
    public static final String BUNDLE_TAB_PDFVIEWCTRL_ID = "bundle_tab_pdfviewctrl_id";
    public static final String BUNDLE_TAB_CONFIG = "bundle_tab_config";

    private static final String BUNDLE_OUTPUT_FILE_URI = "output_file_uri";
    private static final String BUNDLE_IMAGE_STAMP_TARGET_POINT = "image_stamp_target_point";
    private static final String BUNDLE_ANNOTATION_TOOLBAR_SHOW = "bundle_annotation_toolbar_show";
    private static final String BUNDLE_ANNOTATION_TOOLBAR_TOOL_MODE = "bundle_annotation_toolbar_tool_mode";

    protected static final int MAX_SIZE_PAGE_BACK_BUTTON_STACK = 50; // maximum size of the stack for the back and forward page button
    protected static final int HIDE_BACK_BUTTON = 200;   // 0.2 second
    protected static final int INVISIBLE_BACK_BUTTON = 5000; // 5 seconds

    protected static final float TAP_REGION_THRESHOLD = (1f / 7f);
    protected static final int HIDE_PAGE_NUMBER_INDICATOR = 5000; // 5 sec
    protected static final int MAX_CONVERSION_TIME_WITHOUT_NOTIFICATION = 20000; // 20 sec

    // UI elements
    protected ThumbnailSlider mBottomNavBar;
    protected ContentLoadingRelativeLayout mProgressBarLayout;
    protected AnnotationToolbar mAnnotationToolbar;
    protected ViewGroup mViewerHost;
    protected View mPasswordLayout;
    protected EditText mPasswordInput;
    protected CheckBox mPasswordCheckbox;
    protected PageIndicatorLayout mPageNumberIndicator;
    protected ProgressBar mPageNumberIndicatorSpinner;
    protected TextView mPageNumberIndicatorAll;
    protected FindTextOverlay mSearchOverlay;
    protected ImageButton mPageBackButton;
    protected ImageButton mPageForwardButton;

    protected String mTabTag;
    protected String mTabTitle;
    protected String mFileExtension;
    protected String mPassword;
    protected int mTabSource;
    protected int mContentLayout;
    protected int mPdfViewCtrlId;
    protected ViewerConfig mViewerConfig;

    private GetTextInPageTask mGetTextInPageTask;

    private PDFDocLoaderTask mPDFDocLoaderTask;

    // Page Back and Forward Buttons
    // for toggling between previously viewed pages
    protected Deque<PageBackButtonInfo> mPageBackStack; // Page back button stack
    protected Deque<PageBackButtonInfo> mPageForwardStack; // Page forward button stack
    protected Boolean mInternalLinkClicked = false; // True if the next page change is due to an internal link
    protected PageBackButtonInfo mPreviousPageInfo; // Page info of previous page
    protected PageBackButtonInfo mCurrentPageInfo; // Page info of current page
    protected Boolean mPushNextPageOnStack = false; // True if the next page needs to be pushed onto the stack Used for pushing the landing page onto the stack

    // Document conversion
    protected DocumentConversion mDocumentConversion;
    protected boolean mHasWarnedAboutCanNotEditDuringConversion;
    protected boolean mShouldNotifyWhenConversionFinishes;
    protected String mTabConversionTempPath;
    protected boolean mUniversalConverted;

    protected View mRootView;
    protected View mStubPDFViewCtrl;
    protected PDFViewCtrl mPdfViewCtrl;
    protected ToolManager mToolManager;
    protected PDFDoc mPdfDoc;
    protected boolean mIsEncrypted;
    protected boolean mNonPdfDoc;
    protected boolean mIsOfficeDoc;
    protected boolean mIsOfficeDocReady;
    protected long mLastSuccessfulSave;

    protected boolean mDocumentLoading;
    protected boolean mDocumentLoaded;
    protected boolean mWaitingForSetPage;
    protected int mWaitingForSetPageNum;
    protected int mErrorCode = PdfDocManager.DOCUMENT_SETDOC_ERROR_NONE;
    protected int mDocumentState = PdfDocManager.DOCUMENT_STATE_CLEAN;
    protected boolean mHasChangesSinceOpened;
    protected boolean mHasChangesSinceResumed;
    protected boolean mWasSavedAndClosedShown;
    protected ProgressDialog mDownloadDocumentDialog;
    protected boolean mDownloading;
    protected File mCurrentFile; // System files
    protected Uri mCurrentUriFile; // Uri files

    protected long mOriginalFileLength = -1;

    protected boolean mNeedsCleanupFile; // do not delete backup file if something went wrong

    protected boolean mPrintDocumentChecked = true;
    protected boolean mPrintAnnotationsChecked = true;
    protected boolean mPrintSummaryChecked;

    protected int mPageCount;
    protected boolean mIsRtlMode;
    protected ReflowControl mReflowControl;
    protected boolean mIsReflowMode;
    protected boolean mUsedCacheCalled;
    protected int mSpinnerSize = 96; // TODO get this size from some dps
    protected ProgressBar mUniversalDocSpinner;
    protected boolean mAnnotNotAddedDialogShown;
    protected final Object saveDocumentLock = new Object();
    protected boolean mCanAddToTabInfo = true;
    protected boolean mErrorOnOpeningDocument;
    protected boolean mLocalReadOnlyChecked;
    protected boolean mColorModeChanged;
    protected boolean mAnnotationSelected;
    protected Annot mSelectedAnnot;
    protected boolean mIsPageNumberIndicatorConversionSpinningRunning;
    protected boolean mInSearchMode;
    protected int mBookmarkDialogCurrentTab;

    protected boolean mToolbarOpenedFromMouseMovement;

    protected TabListener mTabListener;
    protected ArrayList<AnnotationToolbar.AnnotationToolbarListener> mAnnotationToolbarListeners;
    protected ArrayList<ToolManager.QuickMenuListener> mQuickMenuListeners;

    /////////////////////////////////////////////////////////////////
    // Insert Image Stamper
    private android.net.Uri mOutputFileUri;
    private PointF mAnnotTargetPoint;
    private Intent mAnnotIntentData;
    private boolean mImageStampDelayCreation = false;

    // Insert File Attachment
    private boolean mFileAttachmentDelayCreation = false;

    private int mAnnotationToolbarMode = AnnotationToolbar.START_MODE_NORMAL_TOOLBAR;
    private boolean mAnnotationToolbarShow;
    private ToolMode mAnnotationToolbarToolMode = null;

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface TabListener {

        /**
         * Called when the document has been loaded.
         *
         * @param tag The tab tab
         */
        void onTabDocumentLoaded(String tag);

        /**
         * Called when an error has been happened to this tab.
         *
         * @param errorCode The code of error
         * @param info      The information
         */
        void onTabError(int errorCode, String info);

        /**
         * Called when a new tab has been opened.
         *
         * @param itemSource The item source of document
         * @param tag        The tab tag
         * @param name       The name of the document
         * @param password   The password
         */
        void onOpenAddNewTab(int itemSource, String tag, String name, String password);

        /**
         * Called when show tab info has been triggered.
         */
        void onShowTabInfo(String tag, String title, String fileExtension, int itemSource, int duration);

        /**
         * Called when an ink edit has been selected.
         *
         * @param annot The annotation
         */
        void onInkEditSelected(Annot annot);

        /**
         * Called when the annotation toolbar should open for a tool mode.
         *
         * @param mode The tool mode
         */
        void onOpenAnnotationToolbar(ToolMode mode);

        /**
         * Called when the edit toolbar should open for a tool mode.
         *
         * @param mode The tool mode
         */
        void onOpenEditToolbar(ToolMode mode);

        /**
         * Called when the reflow mode has been toggled.
         */
        void onToggleReflow();

        /**
         * Called when should find next/previous text.
         *
         * @param searchUp True if should go to previous search (up)
         * @return The status
         */
        SearchResultsView.SearchResultStatus onFullTextSearchFindText(boolean searchUp);

        /**
         * Called when thumbnail slider has been stopped tracking touch.
         */
        void onTabThumbSliderStopTrackingTouch();

        /**
         * Called when a single tap has been touched on the tab.
         */
        void onTabSingleTapConfirmed();

        /**
         * The implementation should show the search progress.
         */
        void onSearchProgressShow();

        /**
         * The implementation should hide the search progress.
         */
        void onSearchProgressHide();

        /**
         * The implementation should reset timer for hiding toolbars.
         */
        void resetHideToolbarsTimer();

        /**
         * The implementation should change the visibility of toolbars.
         */
        void setToolbarsVisible(boolean visible);

        /**
         * The implementation should change the visibility of top toolbar,
         * bottom navigation bar as well as system navigation bar.
         */
        void setViewerOverlayUIVisible(boolean visible);

        /**
         * The implementation should return the height of toolbar.
         *
         * @return The height of toolbar
         */
        int getToolbarHeight();

        /**
         * Called when download successfully has been finished.
         */
        void onDownloadedSuccessful();

        /**
         * The implementation should close undo/redo popup menu.
         */
        void onUndoRedoPopupClosed();

        /**
         * Called when the identity of the tab has been changed.
         *
         * @param oldTabTag        The old tab tag
         * @param newTabTag        The new tab tag
         * @param newTabTitle      The new title of the tab
         * @param newFileExtension The new extension of the document
         * @param newTabSource     The new item source of document
         */
        void onTabIdentityChanged(String oldTabTag, String newTabTag, String newTabTitle,
                                  String newFileExtension, int newTabSource);

        /**
         * Called when outline option selected
         */
        void onOutlineOptionSelected();

        /**
         * Called when page thumbnail viewer selected
         *
         * @param thumbnailEditMode <code>true</code> if thumbnail is in edit mode
         * @param checkedItem       The index of the item that is checked
         */
        void onPageThumbnailOptionSelected(boolean thumbnailEditMode, Integer checkedItem);

        /**
         * Called when onBackPressed is called when viewing a document.
         *
         * @return true if custom handling is required, false otherwise.
         */
        boolean onBackPressed();

        /**
         * Called when the fragment is paused.
         *
         * @param fileInfo                  The file shown when tab has been paused
         * @param isDocModifiedAfterOpening True if document has been modified
         *                                  after opening
         */
        void onTabPaused(FileInfo fileInfo, boolean isDocModifiedAfterOpening);
    }

    private enum RegionSingleTap {
        Left,
        Middle,
        Right
    }

    // Handlers
    // Hide page indicator, forward and back button setup
    private Handler mHidePageNumberAndPageBackButtonHandler = new Handler(Looper.getMainLooper());
    private Runnable mHidePageNumberAndPageBackButtonRunnable = new Runnable() {
        @Override
        public void run() {
            hidePageNumberIndicator();
        }
    };


    private Handler mPageNumberIndicatorConversionSpinningHandler = new Handler(Looper.getMainLooper());
    private Runnable mPageNumberIndicatorConversionSpinnerRunnable = new Runnable() {
        @Override
        public void run() {
            if (getActivity() != null && mPageNumberIndicatorSpinner != null)
                mPageNumberIndicatorSpinner.setVisibility(View.VISIBLE);
        }
    };

    // Hide page back button
    // Remove button background and image source, so that the area behind the button
    // is disabled from user input (ie. tapping in that area doesn't cause zooming or
    // the page to be changed)
    private Handler mHidePageBackButtonHandler = new Handler(Looper.getMainLooper());
    private Runnable mHidePageBackButtonRunnable = new Runnable() {
        @Override
        public void run() {
            // as we are working with getResources, needs to check if host still valid
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            mPageBackButton.setImageDrawable(null);
            mPageBackButton.setBackground(null);
            mPageBackButton.setColorFilter(ContextCompat.getColor(activity, android.R.color.white));
        }
    };

    private Handler mInvisiblePageBackButtonHandler = new Handler(Looper.getMainLooper());
    private Runnable mInvisiblePageBackButtonRunnable = new Runnable() {
        @Override
        public void run() {
            // as we are working with getResources, needs to check if host still valid
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            mPageBackButton.setVisibility(View.INVISIBLE);
            mPageBackButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_keyboard_arrow_left_white_24dp));
            mPageBackButton.setBackground(getResources().getDrawable(R.drawable.page_jump_button_bg));
            mPageBackButton.setColorFilter(ContextCompat.getColor(activity, android.R.color.white));
        }
    };

    // Hide page forward button
    // Remove button background and image source, so that the area behind the button
    // is disabled from user input (ie. tapping in that area doesn't cause zooming or
    // the page to be changed)
    private Handler mHidePageForwardButtonHandler = new Handler(Looper.getMainLooper());
    private Runnable mHidePageForwardButtonRunnable = new Runnable() {
        @Override
        public void run() {
            // as we are working with getResources, needs to check if host still valid
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            mPageForwardButton.setImageDrawable(null);
            mPageForwardButton.setBackground(null);
            mPageForwardButton.setColorFilter(ContextCompat.getColor(activity, android.R.color.white));
        }
    };

    private Handler mInvisiblePageForwardButtonHandler = new Handler(Looper.getMainLooper());
    private Runnable mInvisiblePageForwardButtonRunnable = new Runnable() {
        @Override
        public void run() {
            // as we are working with getResources, needs to check if host still valid
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }

            mPageForwardButton.setVisibility(View.INVISIBLE);
            mPageForwardButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_keyboard_arrow_right_white_24dp));
            mPageForwardButton.setBackground(getResources().getDrawable(R.drawable.page_jump_button_bg));
            mPageForwardButton.setColorFilter(ContextCompat.getColor(activity, android.R.color.white));
        }
    };

    private Handler mConversionFinishedMessageHandler = new Handler(Looper.getMainLooper());
    private Runnable mConversionFinishedMessageRunnable = new Runnable() {
        @Override
        public void run() {
            if (mDocumentConversion != null) {
                mShouldNotifyWhenConversionFinishes = true;
            }
        }
    };

    private Handler mResetTextSelectionHandler = new Handler(Looper.getMainLooper());
    private Runnable mResetTextSelectionRunnable = new Runnable() {
        @Override
        public void run() {
            Activity activity = getActivity();
            if (activity == null || activity.isFinishing() || mToolManager == null) {
                return;
            }
            ToolManager.Tool tool = mToolManager.getTool();
            if (tool instanceof TextSelect) {
                ((TextSelect) tool).resetSelection();
            }
        }
    };

    private final ReflowControl.OnPostProcessColorListener mOnPostProcessColorListener =
        new ReflowControl.OnPostProcessColorListener() {
            @Override
            public ColorPt getPostProcessedColor(ColorPt cp) {
                if (mPdfViewCtrl != null) {
                    return mPdfViewCtrl.getPostProcessedColor(cp);
                }
                return cp;
            }
        };

    /**
     * The overloaded implementation of {@link Fragment#onCreate(Bundle)}.
     **/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (sDebug)
            Log.v("LifeCycle", "TabFragment.onCreate");

        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (savedInstanceState != null) {
            mOutputFileUri = savedInstanceState.getParcelable(BUNDLE_OUTPUT_FILE_URI);
            mAnnotTargetPoint = savedInstanceState.getParcelable(BUNDLE_IMAGE_STAMP_TARGET_POINT);
            if (savedInstanceState.getBoolean(BUNDLE_ANNOTATION_TOOLBAR_SHOW, false)) {
                mAnnotationToolbarShow = true;
                String mode = savedInstanceState.getString(BUNDLE_ANNOTATION_TOOLBAR_TOOL_MODE, ToolMode.PAN.toString());
                mAnnotationToolbarToolMode = ToolMode.valueOf(mode);
            }
        }

        Bundle bundle = getArguments();
        if (bundle == null) {
            throw new NullPointerException("bundle cannot be null");
        }

        mViewerConfig = bundle.getParcelable(BUNDLE_TAB_CONFIG);

        mTabTag = bundle.getString(BUNDLE_TAB_TAG);
        if (Utils.isNullOrEmpty(mTabTag)) {
            throw new NullPointerException("Tab tag cannot be null or empty");
        }

        mTabTitle = bundle.getString(BUNDLE_TAB_TITLE);
        if (mTabTitle != null) {
            mTabTitle = mTabTitle.replaceAll("\\/", "-"); // replace illegal characters in filename
        }

        mFileExtension = bundle.getString(BUNDLE_TAB_FILE_EXTENSION);

        mPassword = bundle.getString(BUNDLE_TAB_PASSWORD);
        if (Utils.isNullOrEmpty(mPassword)) {
            mPassword = Utils.getPassword(activity, mTabTag);
        }

        mTabSource = bundle.getInt(BUNDLE_TAB_ITEM_SOURCE);
        if (mTabSource == BaseFileInfo.FILE_TYPE_FILE) {
            mCurrentFile = new File(mTabTag);
        }

        mContentLayout = bundle.getInt(BUNDLE_TAB_CONTENT_LAYOUT, R.layout.controls_fragment_tabbed_pdfviewctrl_tab_content);
        mPdfViewCtrlId = bundle.getInt(BUNDLE_TAB_PDFVIEWCTRL_ID, R.id.pdfviewctrl);

        mPreviousPageInfo = new PageBackButtonInfo();
        mCurrentPageInfo = new PageBackButtonInfo();
    }

    /**
     * The overloaded implementation of {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     **/
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (sDebug)
            Log.v("LifeCycle", "TabFragment.onCreateView");

        if (Utils.isNullOrEmpty(mTabTag)) {
            throw new NullPointerException("Tab tag (file path) cannot be null or empty");
        }

        int layoutResId = mContentLayout == 0 ? R.layout.controls_fragment_tabbed_pdfviewctrl_tab_content : mContentLayout;
        return inflater.inflate(layoutResId, container, false);
    }

    /**
     * The overloaded implementation of {@link Fragment#onViewCreated(View, Bundle)}.
     **/
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (sDebug)
            Log.v("LifeCycle", "TabFragment.onViewCreated");

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        mRootView = view;
        loadPDFViewCtrlView();

        initLayout();

        mViewerHost.setVisibility(View.INVISIBLE);
        mViewerHost.setBackgroundColor(mPdfViewCtrl.getClientBackgroundColor());

        mPageNumberIndicatorSpinner.getIndeterminateDrawable().mutate().setColorFilter(
            ContextCompat.getColor(activity, android.R.color.white),
            android.graphics.PorterDuff.Mode.SRC_IN);

        mToolManager.setAdvancedAnnotationListener(this);
    }

    /**
     * The overloaded implementation of {@link Fragment#onResume()}.
     **/
    @Override
    public void onResume() {
        if (sDebug)
            Log.v("LifeCycle", "TabFragment.onResume");

        super.onResume();

        if (isHidden()) {
            return;
        }

        if (mToolManager != null) {
            mToolManager.setCanResumePdfDocWithoutReloading(canResumeWithoutReloading());
        }

        resumeFragment(true);
    }

    /**
     * The overloaded implementation of {@link Fragment#onPause()}.
     **/
    @Override
    public void onPause() {
        if (sDebug)
            Log.v("LifeCycle", "TabFragment.onPause");

        pauseFragment();
        forceSave();

        super.onPause();
    }

    /**
     * The overloaded implementation of {@link Fragment#onStop()}.
     **/
    @Override
    public void onStop() {
        if (sDebug)
            Log.v("LifeCycle", "TabFragment.onStop");

        if (mTabSource == BaseFileInfo.FILE_TYPE_OPEN_URL) {
            if (mDownloading) {
                mDownloading = false;
                // if file opened from openUrl and download is not done yet
                // cancel openUrl
                if (mPdfViewCtrl != null) {
                    mPdfViewCtrl.closeDoc();
                }
                // delete the cache file
                if (mCurrentFile != null && mCurrentFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    mCurrentFile.delete();
                }
            }
        }

        super.onStop();
    }

    /**
     * The overloaded implementation of {@link Fragment#onDestroyView()}.
     **/
    @Override
    public void onDestroyView() {
        if (sDebug)
            Log.v("LifeCycle", "TabFragment.onDestroyView");

        super.onDestroyView();
    }

    /**
     * The overloaded implementation of {@link Fragment#onDestroy()}.
     **/
    @Override
    public void onDestroy() {
        if (sDebug)
            Log.v("LifeCycle", "TabFragment.onDestroy");

        if (mReflowControl != null && mReflowControl.isReady()) {
            mReflowControl.cleanUp();
            mReflowControl.clearReflowOnTapListeners();
            mReflowControl.clearOnPageChangeListeners();
        }

        // remove listeners
        if (mToolManager != null) {
            mToolManager.removeAnnotationModificationListener(this);
            mToolManager.removePdfDocModificationListener(this);
            mToolManager.removeToolChangedListener(this);
        }

        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.removeDocumentLoadListener(this);
            mPdfViewCtrl.removePageChangeListener(this);
            mPdfViewCtrl.removeDocumentDownloadListener(this);
            mPdfViewCtrl.removeUniversalDocumentConversionListener(this);
            mPdfViewCtrl.destroy();
            mPdfViewCtrl = null;
        }

        if (mPdfDoc != null) {
            try {
                mPdfDoc.close();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                mPdfDoc = null;
            }
        }

        // cleanup
        if (mTabConversionTempPath != null) {
            File file = new File(mTabConversionTempPath);
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            mTabConversionTempPath = null;
        }

        // cleanup
        if (mTabSource == BaseFileInfo.FILE_TYPE_EDIT_URI && mNeedsCleanupFile
            && mCurrentFile != null && mCurrentFile.exists()) {
            // delete temp file we created
            String path = mCurrentFile.getAbsolutePath();
            if (mCurrentFile.delete() && sDebug) {
                Log.d(TAG, "edit uri temp file deleted: " + path);
            }
        }
        if (mTabSource == BaseFileInfo.FILE_TYPE_OFFICE_URI && mNeedsCleanupFile
            && mCurrentFile != null && mCurrentFile.exists()) {
            // delete temp file we created
            String path = mCurrentFile.getAbsolutePath();
            if (mCurrentFile.delete() && sDebug) {
                Log.d(TAG, "office uri temp file deleted: " + path);
            }
        }

        super.onDestroy();
    }

    /**
     * The overloaded implementation of {@link Fragment#onHiddenChanged(boolean)}.
     **/
    @Override
    public void onHiddenChanged(boolean hidden) {
        if (sDebug)
            Log.v("LifeCycle", "TabFragment.onHiddenChanged called with " + (hidden ? "Hidden" : "Visible") + " <" + mTabTag + ">");

        if (hidden) {
            pauseFragment();
        } else {
            resumeFragment(false);
        }

        super.onHiddenChanged(hidden);
    }

    /**
     * The overloaded implementation of {@link Fragment#onLowMemory()}.
     **/
    @Override
    public void onLowMemory() {
        super.onLowMemory();

        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.purgeMemoryDueToOOM();
        }
        ImageMemoryCache.getInstance().clearAll();
        PathPool.getInstance().clear();
    }

    @Override
    public void onSaveInstanceState(
        @NonNull Bundle outState
    ) {

        super.onSaveInstanceState(outState);

        if (mOutputFileUri != null) {
            outState.putParcelable(BUNDLE_OUTPUT_FILE_URI, mOutputFileUri);
        }
        if (mAnnotTargetPoint != null) {
            outState.putParcelable(BUNDLE_IMAGE_STAMP_TARGET_POINT, mAnnotTargetPoint);
        }

        // show annotation toolbar after recreation if it is currently visible and don't need Annot
        boolean showAnnotationToolbar = mAnnotationToolbarMode == AnnotationToolbar.START_MODE_NORMAL_TOOLBAR
            && isAnnotationMode();
        outState.putBoolean(BUNDLE_ANNOTATION_TOOLBAR_SHOW, showAnnotationToolbar);
        if (showAnnotationToolbar) {
            ToolManager.ToolModeBase toolModeBase = mToolManager.getTool().getToolMode();
            outState.putString(BUNDLE_ANNOTATION_TOOLBAR_TOOL_MODE, toolModeBase.toString());
        }

    }

    /**
     * The overloaded implementation of {@link Fragment#onConfigurationChanged(Configuration)}.
     **/
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Let the PDFViewCtrl know that the orientation
        // changed. This way the tools will also receive this
        // notification.
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.onConfigurationChanged(newConfig);
            updateZoomLimits();
        }

        if (isAnnotationMode()) {
            mAnnotationToolbar.onConfigurationChanged(newConfig);
        }
    }

    /**
     * Event called when the tool changes.
     *
     * @param newTool the new tool
     * @param oldTool the old tool
     */
    @Override
    public void toolChanged(ToolManager.Tool newTool, ToolManager.Tool oldTool) {
        if (newTool != null && newTool.getToolMode().equals(ToolMode.FORM_FILL)) {
            if (mTabListener != null) {
                mTabListener.setToolbarsVisible(false);
            }
        }
    }


    /**
     * Called when a file attachment has been selected.
     *
     * @param attachment The file attachment
     */
    @Override
    public void fileAttachmentSelected(FileAttachment attachment) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (attachment == null) {
            return;
        }

        if (mPdfViewCtrl == null) {
            return;
        }

        String attachmentPath = ViewerUtils.exportFileAttachment(mPdfViewCtrl, attachment);
        if (null == attachmentPath) {
            return;
        }

        File attachmentFile = new File(attachmentPath);
        String extension = Utils.getExtension(attachmentPath);
        if (Utils.isExtensionHandled(extension)) {
            openFileInNewTab(attachmentFile);
        } else {
            Uri uri = Utils.getUriForFile(activity, attachmentFile);
            if (uri != null) {
                Utils.shareGenericFile(activity, uri);
            }
        }
    }

    @Override
    public void freehandStylusUsedFirstTime() {

    }

    /**
     * Called when a location has been selected for adding the image stamp.
     *
     * @param targetPoint The target location to add the image stamp
     */
    @Override
    public void imageStamperSelected(PointF targetPoint) {
        mAnnotTargetPoint = targetPoint;
        mOutputFileUri = ViewerUtils.openImageIntent(this);
    }

    @Override
    public void attachFileSelected(PointF targetPoint) {
        mAnnotTargetPoint = targetPoint;
        ViewerUtils.openFileIntent(this);
    }

    /**
     * Called when free text inline editing has started.
     */
    @Override
    public void freeTextInlineEditingStarted() {

    }

    /**
     * Handles changes the page number.
     *
     * @param old_page the old page number
     * @param cur_page the current (new) page number
     * @param state    in non-continuous page presentation modes and when the
     *                 built-in page sliding is in process, this flag is used to
     *                 indicate the state of page change.
     */
    @Override
    public void onPageChange(int old_page, int cur_page, PDFViewCtrl.PageChangeState state) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return;
        }

        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            mPageCount = mPdfViewCtrl.getDoc().getPageCount();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        updatePageIndicator();

        ///////////////////         Reflow        ///////////////////
        if (mIsReflowMode && mReflowControl != null) {
            try {
                mReflowControl.setCurrentPage(cur_page);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        ///////////    Page Back and Forward Buttons     ////////////
        // store current and previous page info
        mPreviousPageInfo.copyPageInfo(mCurrentPageInfo);
        mCurrentPageInfo = getCurrentPageInfo();

        // after a page change, push the current page onto the stack
        if (mPushNextPageOnStack && state == PDFViewCtrl.PageChangeState.END) {
            // if  top element of the back stack's page number does not equal the current page
            // (element we wish to push onto the back stack)
            if (mPageBackStack.isEmpty() || mPageBackStack.peek().pageNum != mPdfViewCtrl.getCurrentPage()) {
                // if the stack is at it's maximum size, then delete the bottom item of the stack
                if (mPageBackStack.size() >= MAX_SIZE_PAGE_BACK_BUTTON_STACK) {
                    mPageBackStack.removeLast();
                }

                mPageBackStack.push(getCurrentPageInfo());
                mPushNextPageOnStack = false;

                // if an element is pushed onto the back page stack we have
                // to clear the forward page stack (if it isn't empty already).
                if (!mPageForwardStack.isEmpty()) {
                    mPageForwardStack.clear();
                }
            }
        }

        // initial time set mPreviousPageInfo equal to mCurrentPageInfo
        if (mPreviousPageInfo.pageNum < 0) {
            mPreviousPageInfo.copyPageInfo(mCurrentPageInfo);
        }

        // if an internal link is clicked, call setCurrentPageHelper to
        // pass the event to the page back and forward buttons
        if (mInternalLinkClicked) {
            setCurrentPageHelper(cur_page, false, getCurrentPageInfo());
            mInternalLinkClicked = false;
        }

        if (PdfViewCtrlSettingsManager.getPageNumberOverlayOption(activity)) {
            resetHidePageNumberIndicatorTimer();
        }

        ///////////    FreeText restore cache     ////////////
        if (mWaitingForSetPage && mWaitingForSetPageNum == cur_page) {
            mWaitingForSetPage = false;
            mWaitingForSetPageNum = -1;
            restoreFreeText();
        }
    }

    /**
     * Handles terminating document load.
     */
    @Override
    public void onDocumentLoaded() {
        if (getActivity() == null || mPdfViewCtrl == null) {
            return;
        }

        // Since we subscribe to DocumentLoaded, this needs to be done if ThumbSlider does not get the event
        if (mBottomNavBar != null) {
            // We pass a reference of the PDFViewCtrl to the slider so it can
            // interact with it (know number of pages, change pages, get thumbnails...
            // At this point no doc is set and the slider has no enough data
            // to initialize itself. When a doc is set we need to reset its data.
            mBottomNavBar.setPdfViewCtrl(mPdfViewCtrl);

            mBottomNavBar.setThumbSliderListener(this);
            mBottomNavBar.handleDocumentLoaded();
        }

        doDocumentLoaded();

        resetHidePageNumberIndicatorTimer();
    }

    /**
     * Handles an update during download.
     *
     * @param state           the state of the update.
     * @param page_num        the number of the page that was just downloaded. Meaningful
     *                        if type is {@link PDFViewCtrl.DownloadState#PAGE}.
     * @param page_downloaded the total number of pages that have been downloaded
     * @param page_count      the page count of the associated document
     * @param message         error message in case the download has failed
     */
    @Override
    public void onDownloadEvent(PDFViewCtrl.DownloadState state, int page_num, int page_downloaded, int page_count, String message) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return;
        }

        if (mDocumentConversion != null && sDebug) {
            Log.e("UNIVERSAL SEQUENCE", "Got downloaded event of type " + state +
                " even though it should be a conversion.");
        }

        switch (state) {
            case PAGE:
            case FINISHED:
                if (mDownloadDocumentDialog != null && mDownloadDocumentDialog.isShowing()) {
                    mDownloadDocumentDialog.dismiss();
                }

                if (mPageCount != page_count) {
                    mPageCount = page_count;
                    mBottomNavBar.refreshPageCount();
                }

                if (state == PDFViewCtrl.DownloadState.FINISHED) {
                    mDownloading = false;
                    CommonToast.showText(activity, R.string.download_finished_message, Toast.LENGTH_SHORT);
                    if (mCurrentFile != null) {
                        if (Utils.isNotPdf(mCurrentFile.getAbsolutePath())) {
                            openOfficeDoc(mCurrentFile.getAbsolutePath(), false);
                            return;
                        }
                        try {
                            mPdfDoc = new PDFDoc(mCurrentFile.getAbsolutePath());
                        } catch (Exception e) {
                            // if the cache file was not saved properly, let's try to save it again here...
                            boolean shouldUnlock = false;
                            try {
                                mPdfViewCtrl.docLock(true);
                                shouldUnlock = true;
                                mPdfViewCtrl.getDoc().save(mCurrentFile.getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null);
                                mPdfDoc = new PDFDoc(mCurrentFile.getAbsolutePath());
                            } catch (Exception e2) {
                                mPdfDoc = null;
                                AnalyticsHandlerAdapter.getInstance().sendException(e);
                            } finally {
                                if (shouldUnlock) {
                                    mPdfViewCtrl.docUnlock();
                                }
                            }
                        }
                        boolean error = false;
                        if (mPdfDoc != null) {
                            try {
                                String oldTabTag = mTabTag;
                                int oldTabSource = mTabSource;
                                mTabTag = mCurrentFile.getAbsolutePath();
                                mTabSource = BaseFileInfo.FILE_TYPE_FILE;
                                mTabTitle = FilenameUtils.removeExtension(new File(mTabTag).getName());
                                if (!mTabTag.equals(oldTabTag) || mTabSource != oldTabSource) {
                                    if (mTabListener != null) {
                                        mTabListener.onTabIdentityChanged(oldTabTag, mTabTag, mTabTitle, mFileExtension, mTabSource);
                                    }
                                }
                                ToolManager.Tool currentTool = mToolManager.getTool();
                                int currentPage = mPdfViewCtrl.getCurrentPage();
                                mToolManager.setReadOnly(false);
                                checkPdfDoc();
                                mToolManager.setTool(currentTool);
                                mPdfViewCtrl.setCurrentPage(currentPage);
                            } catch (Exception e) {
                                error = true;
                                AnalyticsHandlerAdapter.getInstance().sendException(e, "checkPdfDoc");
                            }
                        } else {
                            error = true;
                        }

                        if (error) {
                            handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_NULL_PDFDOC);
                            return;
                        } else {
                            // download is done and successful
                            // add this item to tab list and recent list
                            PdfViewCtrlTabInfo info = new PdfViewCtrlTabInfo();
                            info.tabTitle = mTabTitle;
                            info.tabSource = BaseFileInfo.FILE_TYPE_FILE;
                            info.fileExtension = mFileExtension;
                            // set page presentation mode to the last chosen default
                            String mode = PdfViewCtrlSettingsManager.getViewMode(activity);
                            info.pagePresentationMode = getPagePresentationModeFromSettings(mode).getValue();

                            PdfViewCtrlTabsManager.getInstance().addPdfViewCtrlTabInfo(activity, mCurrentFile.getAbsolutePath(), info);
                            addToRecentList(info);

                            // if it is downloaded and added to tab manager successfully then we
                            // need to check if max tabs count is reached
                            if (mTabListener != null) {
                                mTabListener.onDownloadedSuccessful();
                            }
                        }
                    }
                }
                break;
            case FAILED:
                if (sDebug)
                    Log.d(TAG, "DOWNLOAD_FAILED: " + message);
                if (mDownloadDocumentDialog != null && mDownloadDocumentDialog.isShowing()) {
                    mDownloadDocumentDialog.dismiss();
                }
                if (message != null && !message.equals("cancelled")) {
                    CommonToast.showText(activity, R.string.download_failed_message, Toast.LENGTH_SHORT);
                    mErrorCode = PdfDocManager.DOCUMENT_SETDOC_ERROR_NULL_PDFDOC;
                    handleOpeningDocumentFailed(mErrorCode);
                }
                break;
        }
    }

    /**
     * Handles when {@link com.pdftron.pdf.PDFViewCtrl} starts rendering.
     */
    @Override
    public void onRenderingStarted() {

    }

    /**
     * Handles when {@link com.pdftron.pdf.PDFViewCtrl} finishes rendering.
     */
    @Override
    public void onRenderingFinished() {

    }

    /**
     * Handles an update during document conversion.
     *
     * @param state               - the state of update.
     * @param totalPagesConverted The total number of pages converted so far. Only relevant
     *                            for the @link #CONVERSION_PROGRESS state. Note that pages
     *                            can be processed in batches, and so the number might not
     */
    @Override
    public void onConversionEvent(PDFViewCtrl.ConversionState state, int totalPagesConverted) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return;
        }

        switch (state) {
            case PROGRESS:
                if (mPdfDoc == null) {
                    mPdfDoc = mPdfViewCtrl.getDoc();
                }
                mPageCount = totalPagesConverted;
                if (mPageCount > 0 && !mUsedCacheCalled) {
                    RecentlyUsedCache.accessDocument(mTabTag, mPdfViewCtrl.getDoc());
                    mUsedCacheCalled = true;
                }
                mBottomNavBar.refreshPageCount();
                updatePageIndicator();
                resetHidePageNumberIndicatorTimer();
                if (!mIsPageNumberIndicatorConversionSpinningRunning) {
                    mIsPageNumberIndicatorConversionSpinningRunning = mPageNumberIndicatorConversionSpinningHandler.
                        postDelayed(mPageNumberIndicatorConversionSpinnerRunnable, 1000);
                }
                break;
            case FINISHED:
                mDocumentLoading = false;
                if (mShouldNotifyWhenConversionFinishes) {
                    CommonToast.showText(activity, R.string.open_universal_succeeded, Toast.LENGTH_SHORT, Gravity.CENTER, 0, 0);
                }

                mIsOfficeDocReady = true;

                mDocumentConversion = null;
                mDocumentState = PdfDocManager.DOCUMENT_STATE_FROM_CONVERSION;

                stopConversionSpinningIndicator();

                // save a temp copy
                saveConversionTempCopy();
                break;
            case FAILED:
                if (sDebug) {
                    if (mDocumentConversion != null) {
                        try {
                            Log.e(TAG, mDocumentConversion.getErrorString());
                        } catch (PDFNetException e) {
                            e.printStackTrace();
                        }
                    }
                }

                stopConversionSpinningIndicator();
                break;
        }
    }

    /**
     * Handles when a blank page without content is added to the document
     * as a placeholder.
     */
    @Override
    public void onAddProgressIndicator() {
        if (mPdfViewCtrl == null) {
            return;
        }

        if (mUniversalDocSpinner != null && mPdfViewCtrl.indexOfChild(mUniversalDocSpinner) >= 0) {
            mPdfViewCtrl.removeView(mUniversalDocSpinner);
        }

        mUniversalDocSpinner = new ProgressBar(mPdfViewCtrl.getContext());
        mUniversalDocSpinner.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int width = mUniversalDocSpinner.getMeasuredWidth();
        if (width > 0) {
            mSpinnerSize = width;
        }
        mUniversalDocSpinner.setIndeterminate(true);
        mUniversalDocSpinner.setVisibility(View.INVISIBLE);
        mPdfViewCtrl.addView(mUniversalDocSpinner);
    }

    /**
     * Handles when the position of the blank page without content moves.
     *
     * @param position position
     */
    @Override
    public void onPositionProgressIndicatorPage(Rect position) {
        if (mUniversalDocSpinner != null) {
            try {
                int spinnerSize = mSpinnerSize;
                if (spinnerSize > position.getWidth()) {
                    spinnerSize = (int) position.getWidth();
                }
                if (spinnerSize > position.getHeight()) {
                    spinnerSize = (int) position.getHeight();
                }
                int halfX = (int) (position.getX1() + position.getX2()) / 2;
                int halfY = (int) (position.getY1() + position.getY2()) / 2;
                halfX -= spinnerSize / 2;
                halfY -= spinnerSize / 2;

                mUniversalDocSpinner.layout(halfX, halfY, halfX + spinnerSize, halfY + spinnerSize);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * Handles when the blank page without content enters or leaves the screen.
     *
     * @param isVisible whether visible or not
     */
    @Override
    public void onProgressIndicatorPageVisibilityChanged(boolean isVisible) {
        if (mUniversalDocSpinner != null) {
            if (isVisible) {
                mUniversalDocSpinner.setVisibility(View.VISIBLE);
            } else {
                mUniversalDocSpinner.setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     * Handles when a blank page without content is removed from the document.
     */
    @Override
    public void onRemoveProgressIndicator() {
        if (mUniversalDocSpinner != null && mPdfViewCtrl != null &&
            mPdfViewCtrl.indexOfChild(mUniversalDocSpinner) >= 0) {
            mPdfViewCtrl.removeView(mUniversalDocSpinner);
        }
    }

    /**
     * Handles when it user is trying to access the next page of the PDFViewCtrl but there is no
     * room for the visual progress indicator in the viewer. For example in single page mode.
     */
    @Override
    public void onShowContentPendingIndicator() {
        if (sDebug)
            Log.i("UNIVERSAL PROGRESS", "Told to show content pendering indicator");
//        mConversionInProgressNotification.show();
    }

    /**
     * Handles when the content on the page the user was previously trying to access is
     * accessible.
     */
    @Override
    public void onRemoveContentPendingIndicator() {
        if (sDebug)
            Log.i("UNIVERSAL PROGRESS", "Told to hide content pendering indicator");
//        mConversionInProgressNotification.hide(true);
    }

    /**
     * Handles generic motion events.
     *
     * @param event The generic motion event being processed.
     */
    @Override
    public void onGenericMotionEvent(MotionEvent event) {
        if (!Utils.isNougat()) {
            return;
        }

        final View view = getView();
        if (view == null || mPdfViewCtrl == null || mToolManager == null || mToolManager.getTool() == null) {
            return;
        }
        Tool tool = (Tool) mToolManager.getTool();
        ToolMode mode = ToolManager.getDefaultToolMode(tool.getToolMode());
        float x = event.getX();
        float y = event.getY();

        float threshold = 2.0f;
        if (y < threshold) {
            if (mTabListener != null) {
                mTabListener.setViewerOverlayUIVisible(true);
                mToolbarOpenedFromMouseMovement = true;
            }
        } else {
            if (mTabListener != null && mToolbarOpenedFromMouseMovement) {
                int height = mTabListener.getToolbarHeight();
                if (y > (height + threshold)) {
                    if (mTabListener != null) {
                        mTabListener.setViewerOverlayUIVisible(false);
                        mToolbarOpenedFromMouseMovement = false;
                    }
                }
            }
        }

        Context context = getContext();
        if (context == null) {
            return;
        }
        if (tool.isInsideQuickMenu(x, y)) {
            view.setPointerIcon(PointerIcon.getSystemIcon(context, PointerIcon.TYPE_ARROW));
            return;
        }

        if (mode == ToolMode.ANNOT_EDIT) {
            AnnotEdit annotEdit = (AnnotEdit) tool;
            if (annotEdit.isCtrlPtsHidden()) {
                view.setPointerIcon(PointerIcon.getSystemIcon(context, PointerIcon.TYPE_ARROW));
            } else {
                int effectCtrlPointId = annotEdit.getEffectCtrlPointId(x + mPdfViewCtrl.getScrollX(), y + mPdfViewCtrl.getScrollY());
                switch (effectCtrlPointId) {
                    case AnnotEdit.e_ll:
                    case AnnotEdit.e_ur:
                        view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW));
                        break;
                    case AnnotEdit.e_lr:
                    case AnnotEdit.e_ul:
                        view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW));
                        break;
                    case AnnotEdit.e_ml:
                    case AnnotEdit.e_mr:
                        view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW));
                        break;
                    case AnnotEdit.e_lm:
                    case AnnotEdit.e_um:
                        view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_VERTICAL_DOUBLE_ARROW));
                        break;
                    case AnnotEdit.e_moving:
                        view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_ALL_SCROLL));
                        break;
                    default:
                        view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_ARROW));
                        break;
                }
            }
            return;
        }

        if (mode == ToolMode.ANNOT_EDIT_LINE) {
            AnnotEditLine annotEditLine = (AnnotEditLine) tool;
            int effectCtrlPointId = annotEditLine.getEffectCtrlPointId(x + mPdfViewCtrl.getScrollX(), y + mPdfViewCtrl.getScrollY());
            if (effectCtrlPointId == 2) {
                view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_ALL_SCROLL));
            } else {
                view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_ARROW));
            }
            return;
        }

        if (mode == ToolMode.TEXT_SELECT) {
            TextSelect textSelect = (TextSelect) tool;
            if (textSelect.hitTest(x + mPdfViewCtrl.getScrollX(), y + mPdfViewCtrl.getScrollY()) >= 0) {
                view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW));
                return;
            }
        }

        if (mode == ToolMode.TEXT_UNDERLINE || mode == ToolMode.TEXT_HIGHLIGHT
            || mode == ToolMode.TEXT_SQUIGGLY || mode == ToolMode.TEXT_STRIKEOUT) {
            view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_TEXT));
            return;
        }

        boolean buttonPressed = event.isButtonPressed(MotionEvent.BUTTON_PRIMARY) || event.isButtonPressed(MotionEvent.BUTTON_TERTIARY);

        if (mode != ToolMode.PAN || tool.getNextToolMode() != ToolMode.PAN) {
            if (mode != ToolMode.TEXT_SELECT) {
                view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_ARROW));
                return;
            }
            if (buttonPressed) {
                return;
            }
        }

        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            //noinspection ConstantConditions
            do {
                int pageNum = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
                boolean isTextUnderMouse = false;
                boolean isAnnotUnderMouse = false;

                if (pageNum > 0) {
                    if (mPdfViewCtrl.wereWordsPrepared(pageNum)) {
                        if (mPdfViewCtrl.isThereTextInRect(x - 1, y - 1, x + 1, y + 1)) {
                            isTextUnderMouse = true;
                        }
                    } else {
                        mPdfViewCtrl.prepareWords(pageNum);
                    }

                    if (mPdfViewCtrl.wereAnnotsForMousePrepared(pageNum)) {
                        if (mPdfViewCtrl.getAnnotTypeUnder(x, y) != Annot.e_Unknown) {
                            isAnnotUnderMouse = true;
                        }
                    } else {
                        mPdfViewCtrl.prepareAnnotsForMouse(pageNum);
                    }
                }

                if (isAnnotUnderMouse) {
                    view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HAND));
                } else if (isTextUnderMouse || mode == ToolMode.TEXT_SELECT) {
                    view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_TEXT));
                } else if (buttonPressed) {
                    view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_GRABBING));
                } else {
                    view.setPointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_ARROW));
                }
            } while (false);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    /**
     * The overloaded implementation of {@link ToolManager.PreToolManagerListener#onChangePointerIcon(PointerIcon)}.
     **/
    @Override
    public void onChangePointerIcon(PointerIcon pointerIcon) {
        if (Utils.isNougat() && getView() != null) {
            getView().setPointerIcon(pointerIcon);
        }
    }

    /**
     * The overloaded implementation of {@link ToolManager.PreToolManagerListener#onSingleTapConfirmed(MotionEvent)}.
     **/
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return false;
        }

        int x = (int) (e.getX() + 0.5);
        int y = (int) (e.getY() + 0.5);

        if (mToolManager != null &&
            mToolManager.getTool() != null &&
            (mToolManager.getTool() instanceof Pan)) {
            boolean hasAnnotation = false;
            boolean hasLink = false;

            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                mSelectedAnnot = mPdfViewCtrl.getAnnotationAt(x, y);
                PDFViewCtrl.LinkInfo linkInfo = mPdfViewCtrl.getLinkAt(x, y);
                if (mSelectedAnnot != null && mSelectedAnnot.isValid()) {
                    hasAnnotation = true;
                }
                if (linkInfo != null) {
                    hasLink = true;
                }
                if (hasAnnotation && mSelectedAnnot.getType() == Annot.e_Link) {
                    hasLink = true;
                }
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }

            // handle read-only file
            // disable annotation edit except link
            if (hasLink) {
                // if anything is currently selected
                // deselect it before any further action
                return mToolManager.isQuickMenuJustClosed();
            }

            if (hasAnnotation) {
                handleSpecialFile();
            } else {
                // If we tapped an annotation, then return false and let the
                // tools process the event. Otherwise, we consume the event.
                if (!mToolManager.isQuickMenuJustClosed()) {
                    // if anything is currently selected
                    // deselect it before any further action
                    boolean handled = false;
                    // Check if tapped area should navigate to other pages.
                    RegionSingleTap region = getRegionTap(x, y);
                    if (isSinglePageMode() && region != RegionSingleTap.Middle) {
                        boolean allowPageChangeOnTapEnabled = PdfViewCtrlSettingsManager.getAllowPageChangeOnTap(activity);
                        if (allowPageChangeOnTapEnabled) {
                            boolean checkNext = false;
                            boolean checkPrevious = false;
                            if (region == RegionSingleTap.Left) {
                                if (isRtlMode()) {
                                    checkNext = true;
                                } else {
                                    checkPrevious = true;
                                }
                            } else if (region == RegionSingleTap.Right) {
                                if (isRtlMode()) {
                                    checkPrevious = true;
                                } else {
                                    checkNext = true;
                                }
                            }
                            if (checkPrevious) {
                                if (mPdfViewCtrl.canGotoPreviousPage()) {
                                    mPdfViewCtrl.gotoPreviousPage(PdfViewCtrlSettingsManager.getAllowPageChangeAnimation(activity));
                                    handled = true;
                                }
                            } else if (checkNext) {
                                if (mPdfViewCtrl.canGotoNextPage()) {
                                    mPdfViewCtrl.gotoNextPage(PdfViewCtrlSettingsManager.getAllowPageChangeAnimation(activity));
                                    handled = true;
                                }
                            }
                        }
                    }
                    if (!handled) {
                        if (mTabListener != null) {
                            mTabListener.onTabSingleTapConfirmed();
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * The overloaded implementation of {@link ToolManager.PreToolManagerListener#onMove(MotionEvent, MotionEvent, float, float)}.
     **/
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        return false;
    }

    /**
     * The overloaded implementation of {@link ToolManager.PreToolManagerListener#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     **/
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        return false;
    }

    /**
     * The overloaded implementation of {@link ToolManager.PreToolManagerListener#onScaleBegin(float, float)}.
     **/
    @Override
    public boolean onScaleBegin(float x, float y) {
        return false;
    }

    /**
     * The overloaded implementation of {@link ToolManager.PreToolManagerListener#onScale(float, float)}.
     **/
    @Override
    public boolean onScale(float x, float y) {
        return false;
    }

    /**
     * The overloaded implementation of {@link ToolManager.PreToolManagerListener#onScaleEnd(float, float)}.
     **/
    @Override
    public boolean onScaleEnd(float x, float y) {
        return false;
    }

    /**
     * The overloaded implementation of {@link ToolManager.PreToolManagerListener#onLongPress(MotionEvent)}.
     **/
    @Override
    public boolean onLongPress(MotionEvent e) {
        return false;
    }

    /**
     * The overloaded implementation of {@link ToolManager.PreToolManagerListener#onScrollChanged(int, int, int, int)}.
     **/
    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {

    }

    /**
     * The overloaded implementation of {@link ToolManager.PreToolManagerListener#onDoubleTap(MotionEvent)}.
     **/
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return handleKeyUp(keyCode, event);
    }

    /**
     * The overloaded implementation of {@link ToolManager.QuickMenuListener#onQuickMenuClicked(QuickMenuItem)}.
     **/
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        if (mQuickMenuListeners != null) {
            for (ToolManager.QuickMenuListener listener : mQuickMenuListeners) {
                listener.onQuickMenuClicked(menuItem);
            }
        }
        mToolManager.setQuickMenuJustClosed(false); // next tap brings the toolbar/widgets up
        return false;
    }

    /**
     * The overloaded implementation of {@link ToolManager.QuickMenuListener#onQuickMenuShown()}.
     **/
    @Override
    public void onQuickMenuShown() {
        if (mQuickMenuListeners != null) {
            for (ToolManager.QuickMenuListener listener : mQuickMenuListeners) {
                listener.onQuickMenuShown();
            }
        }
    }

    /**
     * The overloaded implementation of {@link ToolManager.QuickMenuListener#onQuickMenuDismissed()}.
     **/
    @Override
    public void onQuickMenuDismissed() {
        if (mQuickMenuListeners != null) {
            for (ToolManager.QuickMenuListener listener : mQuickMenuListeners) {
                listener.onQuickMenuDismissed();
            }
        }
        if (Utils.isNougat() && getContext() != null) {
            this.onChangePointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_ARROW));
        }
    }

    /**
     * The overloaded implementation of {@link ToolManager.AnnotationModificationListener#onAnnotationsAdded(Map)}.
     **/
    @Override
    public void onAnnotationsAdded(Map<Annot, Integer> annots) {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.AnnotationModificationListener#onAnnotationsPreModify(Map)}.
     **/
    @Override
    public void onAnnotationsPreModify(Map<Annot, Integer> annots) {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.AnnotationModificationListener#onAnnotationsModified(Map, Bundle)}
     **/
    @Override
    public void onAnnotationsModified(Map<Annot, Integer> annots, Bundle extra) {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.AnnotationModificationListener#onAnnotationsPreRemove(Map)}.
     **/
    @Override
    public void onAnnotationsPreRemove(Map<Annot, Integer> annots) {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.AnnotationModificationListener#onAnnotationsRemoved(Map)}.
     **/
    @Override
    public void onAnnotationsRemoved(Map<Annot, Integer> annots) {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.AnnotationModificationListener#onAnnotationsRemovedOnPage(int)}.
     **/
    @Override
    public void onAnnotationsRemovedOnPage(int pageNum) {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.PdfDocModificationListener#onAllAnnotationsRemoved()}.
     **/
    @Override
    public void onAllAnnotationsRemoved() {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.PdfDocModificationListener#onAnnotationAction()}.
     **/
    @Override
    public void onAnnotationAction() {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.PdfDocModificationListener#onBookmarkModified()}.
     **/
    @Override
    public void onBookmarkModified() {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.PdfDocModificationListener#onPagesCropped()}.
     **/
    @Override
    public void onPagesCropped() {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.PdfDocModificationListener#onPagesAdded(List)}.
     **/
    @Override
    public void onPagesAdded(List<Integer> pageList) {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.PdfDocModificationListener#onPagesDeleted(List)}.
     **/
    @Override
    public void onPagesDeleted(List<Integer> pageList) {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.PdfDocModificationListener#onPagesRotated(List)}.
     **/
    @Override
    public void onPagesRotated(List<Integer> pageList) {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.PdfDocModificationListener#onPageMoved(int, int)}.
     **/
    @Override
    public void onPageMoved(int from, int to) {
        handleSpecialFile();
    }

    /**
     * The overloaded implementation of {@link ToolManager.AnnotationModificationListener#annotationsCouldNotBeAdded(String)}.
     **/
    @Override
    public void annotationsCouldNotBeAdded(String errorMessage) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (!mAnnotNotAddedDialogShown) {
            if (null == errorMessage) {
                errorMessage = "Unknown Error";
            }
            Utils.showAlertDialog(activity, activity.getString(R.string.annotation_could_not_be_added_dialog_msg, errorMessage),
                activity.getString(R.string.error));
            mAnnotNotAddedDialogShown = true;
        }
    }

    @Override
    public void onAnnotationSelected(Annot annot, int pageNum) {

    }

    @Override
    public void onAnnotationUnselected() {

    }

    @Override
    public boolean onInterceptAnnotationHandling(@Nullable Annot annot, Bundle extra, ToolMode toolMode) {
        try {
            if (annot != null && annot.isValid() && annot.getType() == Annot.e_Link) {
                mInternalLinkClicked = true;
                updateCurrentPageInfo();
            }
        } catch (PDFNetException e) {
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean onInterceptDialog(android.app.AlertDialog dialog) {
        return false;
    }

    /**
     * The overloaded implementation of {@link ReflowControl.OnReflowTapListener#onReflowSingleTapUp(MotionEvent)}.
     **/
    @Override
    public void onReflowSingleTapUp(MotionEvent event) {
        if (mTabListener != null) {
            mTabListener.onTabSingleTapConfirmed();
        }
    }

    /**
     * The overloaded implementation of {@link ThumbnailSlider.OnThumbnailSliderTrackingListener#onThumbSliderStartTrackingTouch()}.
     **/
    @Override
    public void onThumbSliderStartTrackingTouch() {
        mPageNumberIndicator.setVisibility(View.GONE);
        mPageBackButton.setVisibility(View.INVISIBLE);
        mPageForwardButton.setVisibility(View.INVISIBLE);

        updateCurrentPageInfo();
    }

    /**
     * The overloaded implementation of {@link ThumbnailSlider.OnThumbnailSliderTrackingListener#onThumbSliderStopTrackingTouch(int)}.
     **/
    @Override
    public void onThumbSliderStopTrackingTouch(int pageNum) {
        if (mTabListener != null) {
            mTabListener.onTabThumbSliderStopTrackingTouch();
        }

        resetHidePageNumberIndicatorTimer();
        setCurrentPageHelper(pageNum, false);
    }

    /**
     * Dismisses the user crop dialog.
     */
    public void userCropDialogDismiss() {
        resetHidePageNumberIndicatorTimer();
    }

    /**
     * Sets the {@link TabListener} listener.
     *
     * @param listener The listener
     */
    public void setTabListener(TabListener listener) {
        mTabListener = listener;
    }

    /**
     * Add {@link AnnotationToolbar.AnnotationToolbarListener} listener.
     *
     * @param listener The listener
     */
    public void addAnnotationToolbarListener(AnnotationToolbar.AnnotationToolbarListener listener) {
        if (mAnnotationToolbarListeners == null) {
            mAnnotationToolbarListeners = new ArrayList<>();
        }
        if (!mAnnotationToolbarListeners.contains(listener)) {
            mAnnotationToolbarListeners.add(listener);
        }
    }

    public void removeAnnotationToolbarListener(AnnotationToolbar.AnnotationToolbarListener listener) {
        if (mAnnotationToolbarListeners != null) {
            mAnnotationToolbarListeners.remove(listener);
        }
    }

    /**
     * Add {@link ToolManager.QuickMenuListener} listener.
     *
     * @param listener The listener
     */
    public void addQuickMenuListener(ToolManager.QuickMenuListener listener) {
        if (mQuickMenuListeners == null) {
            mQuickMenuListeners = new ArrayList<>();
        }
        if (!mQuickMenuListeners.contains(listener)) {
            mQuickMenuListeners.add(listener);
        }
    }

    public void removeQuickMenuListener(ToolManager.QuickMenuListener listener) {
        if (mQuickMenuListeners != null) {
            mQuickMenuListeners.remove(listener);
        }
    }

    /**
     * Returns the bundle having information needed to create a tab fragment.
     *
     * @param tag           The tab tag
     * @param title         The title of tab
     * @param fileExtension The extension of the document
     * @param password      The password of the document
     * @param itemSource    The source of the document {@link FileInfo}
     * @return The bundle
     */
    public static Bundle createBasicPdfViewCtrlTabBundle(String tag, String title, String fileExtension,
                                                         String password, int itemSource) {
        return createBasicPdfViewCtrlTabBundle(tag, title, fileExtension, password, itemSource, null);
    }

    /**
     * Returns the bundle having information needed to create a tab fragment.
     *
     * @param tag           The tab tag
     * @param title         The title of tab
     * @param fileExtension The extension of the document
     * @param password      The password of the document
     * @param itemSource    The source of the document {@link FileInfo}
     * @param config        The configuration of the Fragment {@link ViewerConfig}
     * @return The bundle
     */
    public static Bundle createBasicPdfViewCtrlTabBundle(String tag, String title, String fileExtension,
                                                         String password, int itemSource, ViewerConfig config) {
        Bundle args = new Bundle();
        args.putString(BUNDLE_TAB_TAG, tag);
        args.putString(BUNDLE_TAB_TITLE, title);
        args.putString(BUNDLE_TAB_FILE_EXTENSION, fileExtension);
        args.putString(BUNDLE_TAB_PASSWORD, password);
        args.putInt(BUNDLE_TAB_ITEM_SOURCE, itemSource);
        args.putParcelable(BUNDLE_TAB_CONFIG, config);

        return args;
    }

    /**
     * Returns the bundle having information needed to create a tab fragment.
     *
     * @param context  The context
     * @param fileUri  The uri of the document
     * @param password The password of the document
     * @return The bundle
     */
    @SuppressWarnings("unused")
    public static Bundle createBasicPdfViewCtrlTabBundle(@NonNull Context context, @NonNull Uri fileUri, String password) {
        return createBasicPdfViewCtrlTabBundle(context, fileUri, password, null);
    }

    /**
     * Returns the bundle having information needed to create a tab fragment.
     *
     * @param context  The context
     * @param fileUri  The uri of the document
     * @param password The password of the document
     * @param config   The configuration of the Fragment {@link ViewerConfig}
     * @return The bundle
     */
    public static Bundle createBasicPdfViewCtrlTabBundle(@NonNull Context context, @NonNull Uri fileUri, String password, ViewerConfig config) {
        String tag = fileUri.toString();
        String title = Utils.getValidTitle(context, fileUri);
        String fileExtension = "";
        ContentResolver contentResolver = Utils.getContentResolver(context);
        if (contentResolver != null) {
            fileExtension = Utils.getUriExtension(contentResolver, fileUri);
        }
        int itemSource;

        if (ContentResolver.SCHEME_CONTENT.equals(fileUri.getScheme())) {
            // If scheme is a content
            itemSource = BaseFileInfo.FILE_TYPE_EXTERNAL;
        } else if (URLUtil.isHttpUrl(tag) || URLUtil.isHttpsUrl(tag)) {
            itemSource = BaseFileInfo.FILE_TYPE_OPEN_URL;
        } else {
            // If scheme is a File
            tag = fileUri.getPath();
            itemSource = BaseFileInfo.FILE_TYPE_FILE;
        }
        return createBasicPdfViewCtrlTabBundle(tag, title, fileExtension, password, itemSource, config);
    }

    protected boolean isNightModeForToolManager() {
        Activity activity = getActivity();
        return activity != null
            && (PdfViewCtrlSettingsManager.getColorMode(activity) == PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_NIGHT
            || (PdfViewCtrlSettingsManager.getColorMode(activity) == PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_CUSTOM
            && Utils.isColorDark(PdfViewCtrlSettingsManager.getCustomColorModeBGColor(activity))));
    }

    /**
     * Checks whether a warning about can not edit during conversion has been shown.
     *
     * @return True if the warning has been shown
     */
    public boolean getHasWarnedAboutCanNotEditDuringConversion() {
        return mHasWarnedAboutCanNotEditDuringConversion;
    }

    /**
     * Specifies that the warning about can not edit during conversion has been shown.
     */
    public void setHasWarnedAboutCanNotEditDuringConversion() {
        mHasWarnedAboutCanNotEditDuringConversion = true;
        mShouldNotifyWhenConversionFinishes = true;
    }

    /**
     * Checks if the document is ready.
     *
     * @return True if the document is ready
     */
    public boolean isDocumentReady() {
        return mDocumentLoaded;
    }

    /**
     * Updates the view mode.
     *
     * @param pagePresentationMode The page presentation mode
     */
    public void updateViewMode(PDFViewCtrl.PagePresentationMode pagePresentationMode) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return;
        }

        if (mCanAddToTabInfo) {
            PdfViewCtrlTabsManager.getInstance().updateViewModeForTab(activity, mTabTag, pagePresentationMode);
        }
        try {
            updateZoomLimits();
            mPdfViewCtrl.setPagePresentationMode(pagePresentationMode);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Sets the zoom limits for the PDFViewCtrl.
     */
    protected void updateZoomLimits() {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return;
        }

        try {
            boolean isMaintainZoomEnabled = PdfViewCtrlSettingsManager.getMaintainZoomOption(activity);
            if (mViewerConfig != null && mViewerConfig.getPdfViewCtrlConfig() != null) {
                isMaintainZoomEnabled = getPDFViewCtrlConfig(activity).isMaintainZoomEnabled();
            }
            mPdfViewCtrl.setMaintainZoomEnabled(isMaintainZoomEnabled);

            PDFViewCtrl.PageViewMode viewMode = PdfViewCtrlSettingsManager.getPageViewMode(activity);
            if (mViewerConfig != null && mViewerConfig.getPdfViewCtrlConfig() != null) {
                viewMode = getPDFViewCtrlConfig(activity).getPageViewMode();
            }
            mPdfViewCtrl.setZoomLimits(PDFViewCtrl.ZoomLimitMode.RELATIVE, MIN_RELATIVE_ZOOM_LIMIT, MAX_RELATIVE_ZOOM_LIMIT);

            if (!isMaintainZoomEnabled) {
                mPdfViewCtrl.setPageRefViewMode(viewMode);
            } else {
                mPdfViewCtrl.setPreferredViewMode(viewMode);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Updates the page indicator
     */
    protected void updatePageIndicator() {
        if (mPdfViewCtrl == null) {
            return;
        }

        int curPage = mPdfViewCtrl.getCurrentPage();

        if (mPageNumberIndicatorAll != null) {
            mPageNumberIndicatorAll.setText(ViewerUtils.getPageNumberIndicator(
                mPdfViewCtrl, curPage, mPageCount));
        }
        if (mBottomNavBar != null) {
            mBottomNavBar.setProgress(curPage);
        }
    }

    /**
     * Returns the {@link PDFViewCtrl} associated with this tab.
     *
     * @return The PDFViewCtrl
     */
    public PDFViewCtrl getPDFViewCtrl() {
        return mPdfViewCtrl;
    }

    /**
     * Returns {@link ToolManager} associated with this tab.
     *
     * @return The ToolManager
     */
    public ToolManager getToolManager() {
        return (mPdfViewCtrl == null) ? null : (ToolManager) mPdfViewCtrl.getToolManager();
    }

    /**
     * Returns the PDF document
     *
     * @return The PDF document associated with this tab
     */
    public PDFDoc getPdfDoc() {
        return (mPdfViewCtrl == null) ? null : mPdfViewCtrl.getDoc();
    }

    /**
     * Checks if opening file failed.
     *
     * @return True if opening file failed
     */
    public boolean isOpenFileFailed() {
        return mErrorOnOpeningDocument;
    }

    /**
     * Returns the error code, if any.
     * <p>
     * See {@link PdfDocManager}
     *
     * @return The error code
     */
    public int getTabErrorCode() {
        return mErrorCode;
    }

    /**
     * Saves the current PDFViewCtrl state
     */
    protected PdfViewCtrlTabInfo saveCurrentPdfViewCtrlState() {
        if (!mCanAddToTabInfo || !isDocumentReady()) {
            return null;
        }

        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return null;
        }

        PdfViewCtrlTabInfo info = PdfViewCtrlTabsManager.getInstance().getPdfFViewCtrlTabInfo(activity, mTabTag);
        if (info == null) {
            info = new PdfViewCtrlTabInfo();
        }

        // save tab info
        info.fileExtension = mFileExtension;
        info.tabTitle = mTabTitle;
        info.tabSource = mTabSource;
        info.hScrollPos = mPdfViewCtrl.getHScrollPos();
        info.vScrollPos = mPdfViewCtrl.getVScrollPos();
        info.zoom = mPdfViewCtrl.getZoom();
        info.lastPage = mPdfViewCtrl.getCurrentPage();
        info.pageRotation = mPdfViewCtrl.getPageRotation();
        info.setPagePresentationMode(mPdfViewCtrl.getPagePresentationMode());
        info.isRtlMode = mIsRtlMode;
        info.isReflowMode = mIsReflowMode;
        if (mReflowControl != null) {
            try {
                info.reflowTextSize = mReflowControl.getTextSizeInPercent();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
        info.bookmarkDialogCurrentTab = mBookmarkDialogCurrentTab;

        PdfViewCtrlTabsManager.getInstance().addPdfViewCtrlTabInfo(activity, mTabTag, info);

        return info;
    }

    /**
     * Checks if the tab is in annotation mode.
     *
     * @return True if the tab is in annotation mode.
     */
    public boolean isAnnotationMode() {
        return mAnnotationToolbar != null && mAnnotationToolbar.getVisibility() == View.VISIBLE;
    }

    // TODO: Pass the system window insets to the tool manager.
//    public void setNavBarVisible(boolean visible) {
//        mNavBarVisible = visible;
//    }

//    public void setStatusBarVisible(boolean visible) {
//        mStatusBarVisible = visible;
//        if (mToolManager != null) {
//            mToolManager.setStatusBarVisible(visible);
//        }
//    }

    /**
     * Shows the annotation toolbar.
     *
     * @param mode            The mode that annotation toolbar should start with. Possible values are
     *                        {@link AnnotationToolbar#START_MODE_NORMAL_TOOLBAR},
     *                        {@link AnnotationToolbar#START_MODE_EDIT_TOOLBAR},
     * @param inkAnnot        The ink annotation
     * @param toolMode        The tool mode annotation toolbar should start with
     * @param dismissAfterUse Whether should dismiss after use
     */
    public void showAnnotationToolbar(int mode, Annot inkAnnot, ToolMode toolMode, boolean dismissAfterUse) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        mToolManager.deselectAll();
        createAnnotationToolbar();
        mAnnotationToolbar.show(mode, inkAnnot, toolMode, dismissAfterUse);
        mAnnotationToolbarMode = mode;
    }

    private void createAnnotationToolbar() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (mAnnotationToolbar == null) {
            // annotation toolbar
            mAnnotationToolbar = mRootView.findViewById(R.id.annotationToolbar);
            mAnnotationToolbar.setup(mToolManager, this);
            mAnnotationToolbar.setButtonStayDown(PdfViewCtrlSettingsManager.getContinuousAnnotationEdit(activity));
            mAnnotationToolbar.setAnnotationToolbarListener(new AnnotationToolbar.AnnotationToolbarListener() {
                @Override
                public void onAnnotationToolbarClosed() {
                    if (mAnnotationToolbarListeners != null) {
                        for (AnnotationToolbar.AnnotationToolbarListener listener : mAnnotationToolbarListeners) {
                            listener.onAnnotationToolbarClosed();
                        }
                    }

                    setVisibilityOfImaginedToolbar(false);
                }

                @Override
                public void onAnnotationToolbarShown() {
                    if (mAnnotationToolbarListeners != null) {
                        for (AnnotationToolbar.AnnotationToolbarListener listener : mAnnotationToolbarListeners) {
                            listener.onAnnotationToolbarShown();
                        }
                    }

                    setVisibilityOfImaginedToolbar(true);
                }

                @Override
                public void onShowAnnotationToolbarByShortcut(final int mode) {
                    if (mAnnotationToolbarListeners != null) {
                        for (AnnotationToolbar.AnnotationToolbarListener listener : mAnnotationToolbarListeners) {
                            listener.onShowAnnotationToolbarByShortcut(mode);
                        }
                    }
                }
            });
        }
    }

    /**
     * Hides the annotation toolbar.
     */
    public void hideAnnotationToolbar() {
        if (mAnnotationToolbar != null) {
            mAnnotationToolbar.close();
        }
    }

    /**
     * Handles when undo/redo operation is done.
     */
    @Override
    public void onUndoRedoCalled() {
        refreshPageCount();
    }

    /**
     * Initializes the layout.
     */
    protected void initLayout() {
        Activity activity = getActivity();
        if (activity == null || mRootView == null) {
            return;
        }

        loadProgressView();
        loadOverlayView();

        mDownloadDocumentDialog = new ProgressDialog(activity);
        mDownloadDocumentDialog.setMessage(getString(R.string.download_in_progress_message));
        mDownloadDocumentDialog.setIndeterminate(true);
        mDownloadDocumentDialog.setCancelable(true);
        mDownloadDocumentDialog.setCanceledOnTouchOutside(false);
        mDownloadDocumentDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mDownloadDocumentDialog != null && mDownloadDocumentDialog.isShowing()) {
                    mDownloadDocumentDialog.dismiss();
                }
                handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_OPENURL_CANCELLED);
            }
        });
    }

    protected View loadStubProgress() {
        return ((ViewStub) mRootView.findViewById(R.id.stub_progress)).inflate();
    }

    protected void loadProgressView() {
        Activity activity = getActivity();
        if (activity == null || mRootView == null) {
            return;
        }
        if (mProgressBarLayout != null) {
            return;
        }
        View stub = loadStubProgress();

        mProgressBarLayout = stub.findViewById(R.id.progressBarLayout);
        mProgressBarLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDocumentConversion != null) {
                    try {
                        if (sDebug)
                            Log.i("UNIVERSAL", String.format("Conversion status is %d and label is %s, number of converted pages is %d, has been cancelled? %s",
                                mDocumentConversion.getConversionStatus(), mDocumentConversion.getProgressLabel(), mDocumentConversion.getNumConvertedPages(),
                                (mDocumentConversion.isCancelled() ? "YES" : "NO")));
                    } catch (Exception e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    }
                }
                if (mTabListener != null) {
                    mTabListener.onTabSingleTapConfirmed();
                }
            }
        });
    }

    protected View loadStubPDFViewCtrl() {
        return ((ViewStub) mRootView.findViewById(R.id.stub_pdfviewctrl)).inflate();
    }

    protected void loadPDFViewCtrlView() {
        Activity activity = getActivity();
        if (activity == null || mRootView == null) {
            return;
        }
        if (mStubPDFViewCtrl != null) {
            return;
        }
        mStubPDFViewCtrl = loadStubPDFViewCtrl();

        mViewerHost = mStubPDFViewCtrl.findViewById(R.id.pdfViewCtrlHost);
        int pdfViewCtrlResId = mPdfViewCtrlId == 0 ? R.id.pdfviewctrl : mPdfViewCtrlId;
        mPdfViewCtrl = mStubPDFViewCtrl.findViewById(pdfViewCtrlResId);
        if (null == mPdfViewCtrl) {
            // we are in trouble
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("loadPDFViewCtrlView PDFViewCtrl is null"));
            return;
        }
        // mPdfViewCtrl.setAccessibilityDelegate(new View.AccessibilityDelegate() {
        //     @Override
        //     public void onPopulateAccessibilityEvent(View host, final AccessibilityEvent event) {
        //         super.onPopulateAccessibilityEvent(host, event);

        //         if (mGetTextInPageTask != null && mGetTextInPageTask.getStatus() != AsyncTask.Status.FINISHED) {
        //             return;
        //         }
        //         mGetTextInPageTask.setCallback(new GetTextInPageTask.Callback() {
        //             @Override
        //             public void getText(String text) {
        //                 if (!Utils.isNullOrEmpty(text)) {
        //                     event.getText().add(text);
        //                 }
        //             }
        //         });
        //         mGetTextInPageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        //     }

        //     @Override
        //     public void onInitializeAccessibilityNodeInfo(View host, final AccessibilityNodeInfo info) {
        //         super.onInitializeAccessibilityNodeInfo(host, info);
        //         if (mGetTextInPageTask != null && mGetTextInPageTask.getStatus() != AsyncTask.Status.FINISHED) {
        //             return;
        //         }
        //         mGetTextInPageTask = new GetTextInPageTask(mPdfViewCtrl);
        //         mGetTextInPageTask.setCallback(new GetTextInPageTask.Callback() {
        //             @Override
        //             public void getText(String text) {
        //                 if (!Utils.isNullOrEmpty(text)) {
        //                     info.setText(text);
        //                 }
        //             }
        //         });
        //         mGetTextInPageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        //     }
        // });

        try {
            AppUtils.setupPDFViewCtrl(mPdfViewCtrl, getPDFViewCtrlConfig(activity));
            mPdfViewCtrl.setPageBox(Page.e_user_crop);

            updateZoomLimits();
            PDFViewCtrl.PageViewMode viewMode = PdfViewCtrlSettingsManager.getPageViewMode(activity);
            if (mViewerConfig != null && mViewerConfig.getPdfViewCtrlConfig() != null) {
                viewMode = getPDFViewCtrlConfig(activity).getPageViewMode();
            }
            mPdfViewCtrl.setPageViewMode(viewMode);
            if (mViewerConfig != null && mViewerConfig.getPdfViewCtrlConfig() != null) {
                mPdfViewCtrl.setImageSmoothing(getPDFViewCtrlConfig(activity).isImageSmoothing());
            } else {
                if (PdfViewCtrlSettingsManager.getImageSmoothing(activity)) {
                    mPdfViewCtrl.setImageSmoothing(true);
                } else {
                    mPdfViewCtrl.setImageSmoothing(false);
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        // PDFViewCtrl listeners
        mPdfViewCtrl.addPageChangeListener(this);
        mPdfViewCtrl.addDocumentLoadListener(this);
        mPdfViewCtrl.addDocumentDownloadListener(this);
        mPdfViewCtrl.setRenderingListener(this);
        mPdfViewCtrl.addUniversalDocumentConversionListener(this);
        mPdfViewCtrl.setUniversalDocumentProgressIndicatorListener(this);

        // Attach ToolManager to PDFViewCtrl
        int toolManagerResId = (mViewerConfig != null && mViewerConfig.getToolManagerBuilderStyleRes() != 0) ?
            mViewerConfig.getToolManagerBuilderStyleRes() : R.style.TabFragmentToolManager;
        ToolManagerBuilder toolManagerBuilder = mViewerConfig == null ? null : mViewerConfig.getToolManagerBuilder();
        if (toolManagerBuilder == null) {
            toolManagerBuilder = ToolManagerBuilder.from(getContext(), toolManagerResId);
        } else {
            toolManagerBuilder.setStyle(getContext(), toolManagerResId);
        }
        mToolManager = toolManagerBuilder.build(this);
        mToolManager.addToolChangedListener(this);
        mToolManager.setNightMode(isNightModeForToolManager());
        mToolManager.setCacheFileName(mTabTag);
        mToolManager.setAnnotationToolbarListener(new ToolManager.AnnotationToolbarListener() {
            @Override
            public void onInkEditSelected(Annot annot) {
                if (mTabListener != null) {
                    mTabListener.onInkEditSelected(annot);
                }
            }

            @Override
            public void onOpenAnnotationToolbar(
                ToolMode mode) {

                if (mTabListener != null) {
                    mTabListener.onOpenAnnotationToolbar(mode);
                }

            }

            @Override
            public int annotationToolbarHeight() {
                Activity activity = getActivity();
                if (activity == null) {
                    return 0;
                }
                if (mAnnotationToolbar != null && mAnnotationToolbar.getVisibility() == View.VISIBLE) {
//                    return mAnnotationToolbar.getHeight() + Utils.getStatusBarHeight(activity);
                    return mAnnotationToolbar.getHeight();
                }
                return -1;
            }

            @Override
            public int toolbarHeight() {
                if (mTabListener != null) {
                    return mTabListener.getToolbarHeight();
                }
                return -1;
            }

            @Override
            public void onOpenEditToolbar(ToolMode mode) {
                if (mTabListener != null) {
                    mTabListener.onOpenEditToolbar(mode);
                }
            }
        });
    }

    protected PDFViewCtrlConfig getPDFViewCtrlConfig(Context context) {
        PDFViewCtrlConfig pdfViewCtrlConfig = mViewerConfig != null ? mViewerConfig.getPdfViewCtrlConfig() : null;
        if (null == pdfViewCtrlConfig) {
            pdfViewCtrlConfig = PDFViewCtrlConfig.getDefaultConfig(context);
        }
        return pdfViewCtrlConfig;
    }

    /**
     * @return True if the document is password protected.
     */
    public boolean isPasswordProtected() {
        return !Utils.isNullOrEmpty(mPassword);
    }

    protected View loadStubPassword(View view) {
        return ((ViewStub) view.findViewById(R.id.stub_password)).inflate();
    }

    protected void loadPasswordView() {
        Activity activity = getActivity();
        if (activity == null || mRootView == null) {
            return;
        }
        if (mPasswordLayout != null) {
            return;
        }
        View stub = loadStubPassword(mRootView);

        // password layout
        mPasswordLayout = stub.findViewById(R.id.password_layout);
        mPasswordInput = stub.findViewById(R.id.password_input);
        if (mPasswordInput != null) {
            mPasswordInput.setImeOptions(EditorInfo.IME_ACTION_GO);
            mPasswordInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {

                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    Activity activity = getActivity();
                    if (activity == null) {
                        return false;
                    }

                    // If enter key was pressed, then submit password
                    if (actionId == EditorInfo.IME_ACTION_GO) {
                        try {
                            if (mPdfDoc != null && mPdfDoc.initStdSecurityHandler(mPasswordInput.getText().toString())) {
                                //password correct, open document.
                                mPassword = mPasswordInput.getText().toString();
                                checkPdfDoc();
                                mPasswordLayout.setVisibility(View.GONE);
                                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                                if (imm != null) {
                                    imm.hideSoftInputFromWindow(mPasswordInput.getWindowToken(), 0);
                                }
                            } else {
                                //password incorrect
                                mPasswordInput.setText("");
                                CommonToast.showText(activity, R.string.password_not_valid_message, Toast.LENGTH_SHORT);
                            }
                        } catch (Exception e) {
                            handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_NULL_PDFDOC);
                            AnalyticsHandlerAdapter.getInstance().sendException(e, "checkPdfDoc");
                        }

                        return true;
                    }
                    return false;
                }
            });
            mPasswordInput.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    Activity activity = getActivity();
                    if (activity == null) {
                        return false;
                    }

                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        try {
                            if (mPdfDoc != null && mPdfDoc.initStdSecurityHandler(mPasswordInput.getText().toString())) {
                                //password correct, open document.
                                mPassword = mPasswordInput.getText().toString();
                                checkPdfDoc();
                                mPasswordLayout.setVisibility(View.GONE);
                                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                                if (imm != null) {
                                    imm.hideSoftInputFromWindow(mPasswordInput.getWindowToken(), 0);
                                }
                            } else {
                                //password incorrect
                                mPasswordInput.setText("");
                                CommonToast.showText(activity, R.string.password_not_valid_message, Toast.LENGTH_SHORT);
                            }
                        } catch (Exception e) {
                            handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_NULL_PDFDOC);
                            AnalyticsHandlerAdapter.getInstance().sendException(e, "checkPdfDoc");
                        }

                        return true;
                    }
                    return false;
                }
            });
        }
        mPasswordCheckbox = stub.findViewById(R.id.password_checkbox);
        if (mPasswordCheckbox != null) {
            mPasswordCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (!isChecked) {
                        // show password
                        mPasswordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        mPasswordInput.setSelection(mPasswordInput.getText().length());
                    } else {
                        // hide password
                        mPasswordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        mPasswordInput.setSelection(mPasswordInput.getText().length());
                    }
                }
            });
        }
    }

    protected View loadStubReflow() {
        return ((ViewStub) mRootView.findViewById(R.id.stub_reflow)).inflate();
    }

    protected void loadReflowView() {
        Activity activity = getActivity();
        if (activity == null || mRootView == null) {
            return;
        }
        if (mReflowControl != null) {
            return;
        }
        View stub = loadStubReflow();
        mReflowControl = stub.findViewById(R.id.reflow_pager);
    }

    protected View loadStubOverlay() {
        return ((ViewStub) mRootView.findViewById(R.id.stub_overlay)).inflate();
    }

    protected void loadOverlayView() {
        Activity activity = getActivity();
        if (activity == null || mRootView == null) {
            return;
        }
        if (mSearchOverlay != null) {
            return;
        }
        View stub = loadStubOverlay();

        mSearchOverlay = stub.findViewById(R.id.find_text_view);
        mSearchOverlay.setPdfViewCtrl(mPdfViewCtrl);
        mSearchOverlay.setFindTextOverlayListener(new FindTextOverlay.FindTextOverlayListener() {

            @Override
            public void onGotoNextSearch(boolean useFullTextResults) {
                SearchResultsView.SearchResultStatus status = SearchResultsView.SearchResultStatus.NOT_HANDLED;
                if (useFullTextResults) {
                    if (mTabListener != null) {
                        status = mTabListener.onFullTextSearchFindText(false);
                    }
                }
                if (status != SearchResultsView.SearchResultStatus.HANDLED) {
                    if (mSearchOverlay != null) {
                        mSearchOverlay.findText();
                    }
                }
            }

            @Override
            public void onGotoPreviousSearch(boolean useFullTextResults) {
                SearchResultsView.SearchResultStatus status = SearchResultsView.SearchResultStatus.NOT_HANDLED;
                if (useFullTextResults) {
                    if (mTabListener != null) {
                        status = mTabListener.onFullTextSearchFindText(true);
                    }
                }
                if (status != SearchResultsView.SearchResultStatus.HANDLED) {
                    if (mSearchOverlay != null) {
                        if (status == SearchResultsView.SearchResultStatus.USE_FINDTEXT_FROM_END) {
                            mSearchOverlay.findText(mPdfViewCtrl.getPageCount());
                        } else {
                            mSearchOverlay.findText();
                        }
                    }
                }
            }

            @Override
            public void onSearchProgressShow() {
                if (mTabListener != null) {
                    mTabListener.onSearchProgressShow();
                }
            }

            @Override
            public void onSearchProgressHide() {
                if (mTabListener != null) {
                    mTabListener.onSearchProgressHide();
                }
            }
        });

        mBottomNavBar = stub.findViewById(R.id.thumbseekbar);
        mBottomNavBar.setOnMenuItemClickedListener(new ThumbnailSlider.OnMenuItemClickedListener() {
            @Override
            public void onMenuItemClicked(int menuItemPosition) {
                if (menuItemPosition == ThumbnailSlider.POSITION_LEFT) {
                    if (mTabListener != null) {
                        mTabListener.onPageThumbnailOptionSelected(false, null);
                    }
                } else {
                    if (mTabListener != null) {
                        mTabListener.onOutlineOptionSelected();
                    }
                }
            }
        });

        mPageNumberIndicator = stub.findViewById(R.id.page_number_indicator_view);
        mPageNumberIndicator.setPdfViewCtrl(mPdfViewCtrl);
        mPageNumberIndicator.setVisibility(View.GONE);
        mPageNumberIndicator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = getActivity();
                if (activity == null) {
                    return;
                }

                DialogGoToPage dlgGotoPage = new DialogGoToPage(PdfViewCtrlTabFragment.this, activity, mPdfViewCtrl, mReflowControl);
                dlgGotoPage.show();
            }
        });

        mPageNumberIndicatorAll = mPageNumberIndicator.getIndicator();
        if (Utils.isJellyBeanMR1()) {
            mPageNumberIndicatorAll.setTextDirection(View.TEXT_DIRECTION_LTR);
        }

        mPageNumberIndicatorSpinner = mPageNumberIndicator.getSpinner();

        // initialize page back button
        mPageBackStack = new ArrayDeque<>();
        mPageBackButton = stub.findViewById(R.id.page_back_button);
        mPageBackButton.setVisibility(View.INVISIBLE);
        mPageBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpPageBack();
            }
        });

        // initialize page forward button
        mPageForwardStack = new ArrayDeque<>();
        mPageForwardButton = stub.findViewById(R.id.page_forward_button);
        mPageForwardButton.setVisibility(View.INVISIBLE);
        mPageForwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpPageForward();
            }
        });

        if (Utils.isNougat()) {
            View[] views = new View[]{mBottomNavBar, mPageNumberIndicatorAll, mPageBackButton, mPageForwardButton};
            for (View v : views) {
                v.setOnGenericMotionListener(new View.OnGenericMotionListener() {
                    @Override
                    public boolean onGenericMotion(View v, MotionEvent event) {
                        Activity activity = getActivity();
                        if (activity == null || !Utils.isNougat()) {
                            return false;
                        }

                        getToolManager().onChangePointerIcon(PointerIcon.getSystemIcon(activity, PointerIcon.TYPE_HAND));
                        return true;
                    }
                });
            }
        }
    }

    private void jumpPageBack() {
        // reset the Toolbar/ Thumbnail slider so it doesn't disappear while using
        // the page back and forward buttons
        if (mTabListener != null) {
            mTabListener.resetHideToolbarsTimer();
        }

        if (!mPageBackStack.isEmpty()) {
            PageBackButtonInfo previousPageInfo = mPageBackStack.pop();
            PageBackButtonInfo currentPageInfo = getCurrentPageInfo();
            boolean successfulPageChange = false;

            // if the top page on stack is the same as the current page,
            // pop the next page info off the stack
            if (previousPageInfo.pageNum == currentPageInfo.pageNum) {
                if (!mPageBackStack.isEmpty()) {
                    previousPageInfo = mPageBackStack.pop();
                } else {
                    // if the current page equals the last page on the back stack, no
                    // need to chang the page, just add it to the forward stack
                    successfulPageChange = true;
                }
            }

            if (!successfulPageChange && previousPageInfo.pageNum > 0 && previousPageInfo.pageNum <= mPageCount) {
                successfulPageChange = setPageState(previousPageInfo);
            }

            // Add the current page to forward page stack.
            if (successfulPageChange && (mPageForwardStack.isEmpty() || mPageForwardStack.peek().pageNum != currentPageInfo.pageNum)) {
                mPageForwardStack.push(currentPageInfo);
            }
        }

        // if that was the last element on the stack, set the button to be
        // disabled.
        if (mPageBackStack.isEmpty()) {
            hidePageBackButton();
        }

        if (!mPageForwardStack.isEmpty()) {
            stopHidePageForwardButtonTimer();
            setupPageForwardButton();
            mPageForwardButton.setEnabled(true);
            mPageForwardButton.setVisibility(View.VISIBLE);
        }
    }

    private void jumpPageForward() {
        // reset the Toolbar/ Thumbnail slider so it doesn't disappear while using
        // the page back and forward buttons
        if (mTabListener != null) {
            mTabListener.resetHideToolbarsTimer();
        }

        if (!mPageForwardStack.isEmpty()) {
            PageBackButtonInfo nextPageInfo = mPageForwardStack.pop();
            PageBackButtonInfo currentPageInfo = getCurrentPageInfo();
            boolean successfulPageChange = false;

            if (currentPageInfo.pageNum == nextPageInfo.pageNum) {
                if (!mPageForwardStack.isEmpty()) {
                    nextPageInfo = mPageForwardStack.pop();
                } else {
                    successfulPageChange = true;
                }
            }

            if (!successfulPageChange && nextPageInfo.pageNum > 0 && nextPageInfo.pageNum <= mPageCount) {
                successfulPageChange = setPageState(nextPageInfo);
            }

            // Add the current page to back page stack.
            if (successfulPageChange && (mPageBackStack.isEmpty() || mPageBackStack.peek().pageNum != currentPageInfo.pageNum)) {
                mPageBackStack.push(currentPageInfo);
            }
        }

        // if that was the last element on the stack, set the button to be
        // disabled.
        if (mPageForwardStack.isEmpty()) {
            hidePageForwardButton();
        }

        if (!mPageBackStack.isEmpty()) {
            stopHidePageBackButtonTimer();
            setupPageBackButton();
            mPageBackButton.setEnabled(true);
            mPageBackButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Goes to the next text in search.
     */
    public void gotoNextSearch() {
        if (mSearchOverlay != null) {
            mSearchOverlay.gotoNextSearch();
        }
    }

    /**
     * Goes to the previous text in search.
     */
    public void gotoPreviousSearch() {
        if (mSearchOverlay != null) {
            mSearchOverlay.gotoPreviousSearch();
        }
    }

    /**
     * Cancels finding text.
     */
    public void cancelFindText() {
        if (mSearchOverlay != null) {
            mSearchOverlay.cancelFindText();
        }
    }

    /**
     * Specifies the search query.
     *
     * @param text The search query
     */
    public void setSearchQuery(String text) {
        if (mSearchOverlay != null) {
            mSearchOverlay.setSearchQuery(text);
        }
    }

    /**
     * Sets the search rule for match case.
     *
     * @param matchCase True if match case is enabled
     */
    public void setSearchMatchCase(boolean matchCase) {
        if (mSearchOverlay != null) {
            mSearchOverlay.setSearchMatchCase(matchCase);
        }
    }

    /**
     * Sets the search rule for whole word.
     *
     * @param wholeWord True if whole word is enabled
     */
    public void setSearchWholeWord(boolean wholeWord) {
        if (mSearchOverlay != null) {
            mSearchOverlay.setSearchWholeWord(wholeWord);
        }
    }

    /**
     * Sets the search rules for match case and whole word.
     *
     * @param matchCase True if match case is enabled
     * @param wholeWord True if whole word is enabled
     */
    @SuppressWarnings("unused")
    public void setSearchSettings(boolean matchCase, boolean wholeWord) {
        if (mSearchOverlay != null) {
            mSearchOverlay.setSearchSettings(matchCase, wholeWord);
        }
    }

    /**
     * Starts the TextHighlighter tool.
     */
    public void highlightSearchResults() {
        if (mSearchOverlay != null) {
            mSearchOverlay.highlightSearchResults();
        }
    }

    /**
     * Resets full text results.
     */
    public void resetFullTextResults() {
        if (mSearchOverlay != null) {
            mSearchOverlay.resetFullTextResults();
        }
    }

    /**
     * Submits the query text.
     *
     * @param text The query text
     */
    public void queryTextSubmit(String text) {
        if (mSearchOverlay != null) {
            mSearchOverlay.queryTextSubmit(text);
        }
    }

    /**
     * Exits the search mode.
     */
    public void exitSearchMode() {
        if (mSearchOverlay != null) {
            mSearchOverlay.exitSearchMode();
        }
    }

    /**
     * Resets timer for hiding page number indicator.
     */
    protected void resetHidePageNumberIndicatorTimer() {
        if (!mDocumentLoaded) {
            return;
        }
        stopHidePageNumberIndicatorTimer();

        if (mPageNumberIndicator != null) {
            boolean canShow = mViewerConfig == null || mViewerConfig.isShowPageNumberIndicator();
            mPageNumberIndicator.setVisibility(canShow ? View.VISIBLE : View.GONE);
        }
        if (mHidePageNumberAndPageBackButtonHandler != null) {
            mHidePageNumberAndPageBackButtonHandler.postDelayed(mHidePageNumberAndPageBackButtonRunnable, HIDE_PAGE_NUMBER_INDICATOR);
        }
    }

    /**
     * Stops the timer for hiding the page number indicator.
     */
    protected void stopHidePageNumberIndicatorTimer() {
        if (mHidePageNumberAndPageBackButtonHandler != null) {
            mHidePageNumberAndPageBackButtonHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Stops the timer for hiding the page back button indicator.
     */
    protected void stopHidePageBackButtonTimer() {
        if (mHidePageBackButtonHandler != null) {
            mHidePageBackButtonHandler.removeCallbacksAndMessages(null);
        }
        if (mInvisiblePageBackButtonHandler != null) {
            mInvisiblePageBackButtonHandler.removeCallbacksAndMessages(null);
        }
    }

    private void setupPageBackButton() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (mPageBackButton != null) {
            mPageBackButton.setEnabled(true);
            mPageBackButton.setColorFilter(ContextCompat.getColor(activity, android.R.color.white));
            mPageBackButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_keyboard_arrow_left_white_24dp));
            mPageBackButton.setBackground(getResources().getDrawable(R.drawable.page_jump_button_bg));
        }
    }

    private void setupPageForwardButton() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (mPageForwardButton != null) {
            mPageForwardButton.setEnabled(true);
            mPageForwardButton.setColorFilter(ContextCompat.getColor(activity, android.R.color.white));
            mPageForwardButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_keyboard_arrow_right_white_24dp));
            mPageForwardButton.setBackground(getResources().getDrawable(R.drawable.page_jump_button_bg));
        }
    }

    private void stopHidePageForwardButtonTimer() {
        if (mHidePageForwardButtonHandler != null) {
            mHidePageForwardButtonHandler.removeCallbacksAndMessages(null);
        }
        if (mInvisiblePageForwardButtonHandler != null) {
            mInvisiblePageForwardButtonHandler.removeCallbacksAndMessages(null);
        }
    }

    private void stopConversionFinishedTimer() {
        if (mConversionFinishedMessageHandler != null) {
            mConversionFinishedMessageHandler.removeCallbacksAndMessages(null);
        }
    }

    private void stopConversionSpinningIndicator() {
        if (mPageNumberIndicatorConversionSpinningHandler != null) {
            mPageNumberIndicatorConversionSpinningHandler.removeCallbacksAndMessages(null);
        }
        if (mPageNumberIndicatorSpinner != null) {
            mPageNumberIndicatorSpinner.setVisibility(View.GONE);
        }
        mIsPageNumberIndicatorConversionSpinningRunning = false;
    }

    protected void stopHandlers() {
        stopHidePageNumberIndicatorTimer();
        stopConversionSpinningIndicator();
        stopConversionFinishedTimer();
        stopHidePageBackButtonTimer();
        stopHidePageForwardButtonTimer();
    }

    // special case where landingPageInfo must be pushed onto the back stack AFTER the departurePageInfo
    // applies to internal links
    @SuppressWarnings("SameParameterValue")
    private void setCurrentPageHelper(int nextPageNum, boolean setPDFViewCtrl, PageBackButtonInfo landingPageInfo) {
        setCurrentPageHelper(nextPageNum, setPDFViewCtrl);
        mPageBackStack.push(landingPageInfo);
    }

    /**
     * Helper to set the current page.
     *
     * @param nextPageNum    The next page number
     * @param setPDFViewCtrl True if PDFViewCtrl should be set too
     */
    public void setCurrentPageHelper(int nextPageNum, boolean setPDFViewCtrl) {
        if (mPdfViewCtrl == null) {
            return;
        }

        ///////////////////         Page Back and Forward Buttons        ///////////////////
        PageBackButtonInfo departurePageInfo = new PageBackButtonInfo();
        boolean pageChangeOccurred = false;

        // if setCurrentPage needs to be called
        if (setPDFViewCtrl) {
            departurePageInfo = getCurrentPageInfo();
            // set current page state
            mPdfViewCtrl.setCurrentPage(nextPageNum);

            // if the page change will occur on its own
        } else {
            if (nextPageNum == mCurrentPageInfo.pageNum) {
                // if the page change has already occurred
                departurePageInfo.copyPageInfo(mPreviousPageInfo);
                pageChangeOccurred = true;
            } else {
                // if the page change has not occurred
                departurePageInfo = mCurrentPageInfo;
            }
        }

        // if the departure page number is within bounds and if the departure page's
        // page number does not equal the next page number.
        if (departurePageInfo.pageNum > 0 && departurePageInfo.pageNum <= mPageCount && departurePageInfo.pageNum != nextPageNum) {
            // if the top element's page number on the back stack does not equal the page number we are about
            // to push onto the stack (the departure page's page number)
            if ((mPageBackStack.isEmpty() || mPageBackStack.peek().pageNum != departurePageInfo.pageNum)) {
                // if the stack is at it's maximum size, then delete the bottom item of the stack
                if (mPageBackStack.size() >= MAX_SIZE_PAGE_BACK_BUTTON_STACK) {
                    mPageBackStack.removeLast();
                }

                // top element's page number of the back stack is the same as the page we are trying to
                // push onto the stack (departure page's number)
            } else {
                // remove top element and replace with a more recent version of that page's state
                mPageBackStack.pop();
            }

            mPageBackStack.push(departurePageInfo);
            if (!pageChangeOccurred) {
                mPushNextPageOnStack = true;
            }

            // if an element is pushed onto the back page stack we have
            // to clear the forward page stack (if it isn't empty already).
            if (!mPageForwardStack.isEmpty()) {
                mPageForwardStack.clear();
            }
        }

        // set the visibility of the page back and forward buttons
        if (!mPageBackStack.isEmpty()) {
            stopHidePageBackButtonTimer();
            setupPageBackButton();
            if (!mSearchOverlay.isShown()) {
                mPageBackButton.setVisibility(View.VISIBLE);
            }
            mPageBackButton.setEnabled(true);
        }

        if (mPageForwardStack.isEmpty()) {
            hidePageForwardButton();
        }

        ///////////////////              Reflow              ///////////////////
        if (mIsReflowMode && mReflowControl != null) {
            // Note no need to check setPdfViewCtrl since if it is in continuous mode, the current
            // page may not be updated in PDFViewCtrl (it actually updates in PDFViewCtrl.onDraw),
            // so we have to set the current page explicitly
            try {
                mReflowControl.setCurrentPage(mPdfViewCtrl.getCurrentPage());
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * Updates the current page info.
     */
    public void updateCurrentPageInfo() {
        mCurrentPageInfo = getCurrentPageInfo();
    }

    /**
     * Returns the current page info.
     *
     * @return The current page info
     */
    public PageBackButtonInfo getCurrentPageInfo() {
        PageBackButtonInfo pageState = new PageBackButtonInfo();

        if (mPdfViewCtrl != null) {
            pageState.zoom = mPdfViewCtrl.getZoom();
            pageState.pageRotation = mPdfViewCtrl.getPageRotation();
            pageState.pagePresentationMode = mPdfViewCtrl.getPagePresentationMode();
            pageState.hScrollPos = mPdfViewCtrl.getHScrollPos();
            pageState.vScrollPos = mPdfViewCtrl.getVScrollPos();
            pageState.pageNum = mPdfViewCtrl.getCurrentPage();
        }

        return pageState;
    }

    private void hidePageForwardButton() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        // disable page forward button and then hide it
        if (mHidePageForwardButtonHandler != null && mInvisiblePageForwardButtonHandler != null) {
            mPageForwardButton.setEnabled(false);
            mPageForwardButton.setColorFilter(ContextCompat.getColor(activity, R.color.back_fwd_buttons_disabled_outline));
            mHidePageForwardButtonHandler.postDelayed(mHidePageForwardButtonRunnable, HIDE_BACK_BUTTON);
            mInvisiblePageForwardButtonHandler.postDelayed(mInvisiblePageForwardButtonRunnable, INVISIBLE_BACK_BUTTON);
        } else {
            mPageForwardButton.setVisibility(View.INVISIBLE);
        }
    }

    private void hidePageBackButton() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        // disable page back button and then hide it
        if (mHidePageBackButtonHandler != null && mInvisiblePageBackButtonHandler != null) {
            mPageBackButton.setColorFilter(ContextCompat.getColor(activity, R.color.back_fwd_buttons_disabled_outline));
            mPageBackButton.setEnabled(false);
            mHidePageBackButtonHandler.postDelayed(mHidePageBackButtonRunnable, HIDE_BACK_BUTTON);
            mInvisiblePageBackButtonHandler.postDelayed(mInvisiblePageBackButtonRunnable, INVISIBLE_BACK_BUTTON);
        } else {
            mPageBackButton.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Clears the stacks of page backward/forward.
     */
    public void clearPageBackAndForwardStacks() {
        hidePageBackButton();
        hidePageForwardButton();

        // clear page button stacks
        mPageBackStack.clear();
        mPageForwardStack.clear();
    }

    /**
     * Checks if the tab is ready only.
     *
     * @return True if the tab is read only
     */
    public boolean isTabReadOnly() {
        return (mDocumentState == PdfDocManager.DOCUMENT_STATE_READ_ONLY ||
            mDocumentState == PdfDocManager.DOCUMENT_STATE_READ_ONLY_AND_MODIFIED ||
            mDocumentState == PdfDocManager.DOCUMENT_STATE_CORRUPTED ||
            mDocumentState == PdfDocManager.DOCUMENT_STATE_CORRUPTED_AND_MODIFIED ||
            mDocumentState == PdfDocManager.DOCUMENT_STATE_DURING_CONVERSION ||
            mDocumentState == PdfDocManager.DOCUMENT_STATE_FROM_CONVERSION ||
            mDocumentState == PdfDocManager.DOCUMENT_STATE_OUT_OF_SPACE ||
            (mToolManager != null && mToolManager.isReadOnly()));
    }

    @SuppressWarnings("unused")
    private RegionSingleTap getRegionTap(int x, int y) {
        RegionSingleTap regionSingleTap = RegionSingleTap.Middle;

        if (mPdfViewCtrl != null) {
            float width = mPdfViewCtrl.getWidth();
            float widthThresh = width * TAP_REGION_THRESHOLD;
            if (x <= widthThresh) {
                regionSingleTap = RegionSingleTap.Left;
            } else if (x >= width - widthThresh) {
                regionSingleTap = RegionSingleTap.Right;
            }
        }

        return regionSingleTap;
    }

    /**
     * Checks if the tab is in continuous page mode.
     *
     * @return True if the tab is in continuous page mode
     * @see #isSinglePageMode
     */
    public boolean isContinuousPageMode() {
        if (mPdfViewCtrl == null) {
            return false;
        }

        PDFViewCtrl.PagePresentationMode mode = mPdfViewCtrl.getPagePresentationMode();
        return mode == PDFViewCtrl.PagePresentationMode.SINGLE_CONT ||
            mode == PDFViewCtrl.PagePresentationMode.FACING_CONT ||
            mode == PDFViewCtrl.PagePresentationMode.FACING_COVER_CONT;
    }

    /**
     * Checks if the tab is in single page mode.
     *
     * @return True if the tab is in single page mode
     * @see #isContinuousPageMode
     */
    public boolean isSinglePageMode() {
        return !isContinuousPageMode();
    }

    private boolean setPageState(PageBackButtonInfo pageStateInfo) {
        if (mPdfViewCtrl == null) {
            return false;
        }

        boolean successfulPageChange = mPdfViewCtrl.setCurrentPage(pageStateInfo.pageNum);

        if (mIsReflowMode && mReflowControl != null) {
            try {
                mReflowControl.setCurrentPage(pageStateInfo.pageNum);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        // if the page change was successful AND the page rotation has not changed AND
        // the page presentation mode has not changed, set the page state based off of the
        // pageStateInfo
        if (successfulPageChange && pageStateInfo.pageRotation == mPdfViewCtrl.getPageRotation() &&
            pageStateInfo.pagePresentationMode == mPdfViewCtrl.getPagePresentationMode()) {

            // deal with situation where setZoom is larger than max zoom
            double desiredHPos = pageStateInfo.hScrollPos;
            double desiredVPos = pageStateInfo.vScrollPos;
            if (pageStateInfo.zoom > 0) {
                mPdfViewCtrl.setZoom(pageStateInfo.zoom);
                if (Math.abs(mPdfViewCtrl.getZoom() - pageStateInfo.zoom) > 0.01) {
                    double zoomDifference = mPdfViewCtrl.getZoom() / pageStateInfo.zoom;
                    desiredHPos *= zoomDifference;
                    desiredVPos *= zoomDifference;
                }
            }
            // end
            if (desiredHPos > 0 || desiredVPos > 0) {
                mPdfViewCtrl.scrollTo((int) desiredHPos, (int) desiredVPos);
            }
        }

        return successfulPageChange;
    }

    /**
     * Checks if the tab is in reflow mode.
     *
     * @return True if the tab is in reflow mode
     */
    public boolean isReflowMode() {
        return mIsReflowMode;
    }

    /**
     * Toggles the reflow mode.
     *
     * @return True if the reflow mode will be enabled after toggling
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean toggleReflow() {
        mIsReflowMode = !mIsReflowMode;
        setReflowMode(mIsReflowMode);
        return mIsReflowMode;
    }

    /**
     * Returns the reflow text size.
     *
     * @return The reflow text size.
     */
    public int getReflowTextSize() {
        try {
            if (mReflowControl != null && mReflowControl.isReady()) {
                return mReflowControl.getTextSizeInPercent();
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        return 100;
    }

    /**
     * Zooms in/out reflow mode.
     *
     * @param flagZoomIn True if zoom in; False if zoom out
     */
    public void zoomInOutReflow(boolean flagZoomIn) {
        if (mIsReflowMode && mReflowControl != null) {
            try {
                if (flagZoomIn) {
                    mReflowControl.zoomIn();
                } else {
                    mReflowControl.zoomOut();
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * Sets the reflow mode.
     *
     * @param isReflowMode True if reflow mode is enabled
     */
    public void setReflowMode(boolean isReflowMode) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null || mPdfDoc == null) {
            return;
        }
        loadReflowView();
        if (mReflowControl == null) {
            return;
        }

        mIsReflowMode = isReflowMode;
        if (mIsReflowMode) {
            int pageNum = mPdfViewCtrl.getCurrentPage();
            mReflowControl.setup(mPdfViewCtrl.getDoc(), mOnPostProcessColorListener);
            // Reflow control might have not been ready at the time RTL mode was set,
            // so to make sure RTL mode is set appropriately set it once again here
            setRtlMode(mIsRtlMode);
            mReflowControl.clearReflowOnTapListeners();
            mReflowControl.clearOnPageChangeListeners();
            mReflowControl.addReflowOnTapListener(this);
            mReflowControl.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    if (!mIsReflowMode) {
                        return;
                    }

                    if (mIsRtlMode) {
                        position = mPageCount - 1 - position;
                    }
                    int curPage = position + 1;
                    int oldPage = mPdfViewCtrl.getCurrentPage();

                    updatePageIndicator();

                    // if an internal link is clicked, call setCurrentPageHelper to
                    // pass the event to the page back and forward buttons
                    try {
                        if (mReflowControl.isInternalLinkClicked()) {
                            mReflowControl.resetInternalLinkClicked();
                            if (oldPage != curPage) {
                                setCurrentPageHelper(oldPage, false, getCurrentPageInfo());
                            }
                        }
                    } catch (Exception e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    }

                    mPdfViewCtrl.setCurrentPage(curPage);

                    Activity activity = getActivity();
                    if (activity != null && PdfViewCtrlSettingsManager.getPageNumberOverlayOption(activity)) {
                        resetHidePageNumberIndicatorTimer();
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });

            // It is possible that new markup annotations were added to the document
            save(false, true, false);
            try {
                mReflowControl.notifyPagesModified();
                mReflowControl.setCurrentPage(pageNum);
                mReflowControl.enableTurnPageOnTap(PdfViewCtrlSettingsManager.getAllowPageChangeOnTap(activity));
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
            mReflowControl.setVisibility(View.VISIBLE);
            updateReflowColorMode();
            mPdfViewCtrl.setCurrentPage(pageNum);

            updatePageIndicator();

            // hide and pause PDFViewCtrl
            mViewerHost.setVisibility(View.INVISIBLE);
            mPdfViewCtrl.pause();
        } else {
            // hide reflow
            mReflowControl.cleanUp();
            mReflowControl.setVisibility(View.GONE);
            mReflowControl.removeReflowOnTapListener(this);

            // switch back to PDFViewCtrl
            mPdfViewCtrl.resume();
            mViewerHost.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Toggles right-to-left mode.
     *
     * @return True if right-to-left mode is on after toggling
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean toggleRtlMode() {
        setRtlMode(!mIsRtlMode);
        return mIsRtlMode;
    }

    /**
     * Sets right-to-left mode
     *
     * @param isRtlMode True if right-to-left mode is enabled
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void setRtlMode(boolean isRtlMode) {
        if (mPdfViewCtrl == null) {
            return;
        }

        mIsRtlMode = isRtlMode;
        try {
            if (mReflowControl != null && mReflowControl.isReady()) {
                mReflowControl.setRightToLeftDirection(isRtlMode);
                if (mIsReflowMode && mPdfViewCtrl != null) {
                    int pageNum = mPdfViewCtrl.getCurrentPage();
                    mReflowControl.reset();
                    mReflowControl.setCurrentPage(pageNum);
                    mPdfViewCtrl.setCurrentPage(pageNum);
                }
            }
            if (mPdfViewCtrl != null) {
                mPdfViewCtrl.setRightToLeftLanguage(isRtlMode);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        if (Utils.isJellyBeanMR1() && mBottomNavBar != null) {
            Configuration config = getResources().getConfiguration();
            if ((config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL && !isRtlMode) ||
                (config.getLayoutDirection() != View.LAYOUT_DIRECTION_RTL && isRtlMode)) {
                mBottomNavBar.setReversed(true);
            } else {
                mBottomNavBar.setReversed(false);
            }
        }
    }

    /**
     * Checks whether right-to-left mode is enabled
     *
     * @return True if right-to-left mode is enabled.
     */
    public boolean isRtlMode() {
        return mIsRtlMode;
    }

    protected void doDocumentLoaded() {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return;
        }

        if (mDocumentLoaded) {
            return;
        }
        mDocumentLoaded = true;

        // setup reflow control
        if (mReflowControl != null) {
            mReflowControl.setup(mPdfViewCtrl.getDoc(), mOnPostProcessColorListener);
        }

        mViewerHost.setVisibility(View.VISIBLE);
        PdfViewCtrlTabInfo info = PdfViewCtrlTabsManager.getInstance().getPdfFViewCtrlTabInfo(activity, mTabTag);
        if (info == null) {
            if (PdfViewCtrlSettingsManager.getRememberLastPage(activity)) {
                info = getInfoFromRecentList(getCurrentFileInfo());
            }
        }

        boolean skipSetState = false;
        if (mTabConversionTempPath == null &&
            (mDocumentState == PdfDocManager.DOCUMENT_STATE_FROM_CONVERSION ||
                mDocumentState == PdfDocManager.DOCUMENT_STATE_DURING_CONVERSION)) {
            // do not load saved state for newly opened conversion file
            skipSetState = true;
        }

        if (info != null && !skipSetState) {
            // centralized place to restore previous viewing properties
            PDFViewCtrl.PagePresentationMode pagePresentationMode;
            if (info.hasPagePresentationMode()) {
                pagePresentationMode = info.getPagePresentationMode();
            } else {
                // set to last chosen default
                String mode = PdfViewCtrlSettingsManager.getViewMode(activity);
                pagePresentationMode = getPagePresentationModeFromSettings(mode);
            }
            updateViewMode(pagePresentationMode);

            // note: setRtlMode should be before setCurrentPage; otherwise, mCurCanvasScrollXOffsetSave
            // in PDFViewCtrl is not going to be set correctly for RTL
            if ((mViewerConfig != null && mViewerConfig.isShowRightToLeftOption()) ||
                PdfViewCtrlSettingsManager.hasRtlModeOption(activity)) {
                PdfViewCtrlSettingsManager.updateRtlModeOption(activity, true);
                // don't allow RTL mode when there is no setting to turn it off
                if (!info.isRtlMode) {
                    info.isRtlMode = mViewerConfig != null && mViewerConfig.isRightToLeftModeEnabled();
                }
                setRtlMode(info.isRtlMode);
            }

            if (info.lastPage > 0) {
                mPdfViewCtrl.setCurrentPage(info.lastPage);
            }

            try {
                switch (info.pageRotation) {
                    case Page.e_0:
                        // do nothing if no rotation
                        break;
                    case Page.e_90:
                        mPdfViewCtrl.rotateClockwise();
                        mPdfViewCtrl.updatePageLayout();
                        break;
                    case Page.e_180:
                        mPdfViewCtrl.rotateClockwise();
                        mPdfViewCtrl.rotateClockwise();
                        mPdfViewCtrl.updatePageLayout();
                        break;
                    case Page.e_270:
                        mPdfViewCtrl.rotateCounterClockwise();
                        mPdfViewCtrl.updatePageLayout();
                        break;
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
            if (info.zoom > 0.0) {
                mPdfViewCtrl.setZoom(info.zoom);
            }
            if (info.hScrollPos > 0 || info.vScrollPos > 0) {
                mPdfViewCtrl.scrollTo(info.hScrollPos, info.vScrollPos);
            }
            if (info.isReflowMode != isReflowMode()) {
                if (mTabListener != null) {
                    mTabListener.onToggleReflow();
                }
            }
            if (mReflowControl != null && mReflowControl.isReady()) {
                try {
                    mReflowControl.setTextSizeInPercent(info.reflowTextSize);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
            mBookmarkDialogCurrentTab = info.bookmarkDialogCurrentTab;
        } else {
            // set to last chosen default
            String mode = PdfViewCtrlSettingsManager.getViewMode(activity);
            PDFViewCtrl.PagePresentationMode pagePresentationMode = getPagePresentationModeFromSettings(mode);
            mPdfViewCtrl.setPagePresentationMode(pagePresentationMode);
        }

        if (mBookmarkDialogCurrentTab == -1) {
            mBookmarkDialogCurrentTab = Utils.getFirstBookmark(mPdfViewCtrl.getDoc()) != null ? 1 : 0;
        }

        updateColorMode();

        PdfViewCtrlTabInfo tempInfo = saveCurrentPdfViewCtrlState();

        if (info != null) {
            addToRecentList(info);
        } else {
            addToRecentList(tempInfo);
        }

        PdfViewCtrlTabsManager.getInstance().updateLastViewedTabTimestamp(getActivity(), mTabTag);

        if (mTabListener != null) {
            mTabListener.onTabDocumentLoaded(getTabTag());
        }

        toggleViewerVisibility(true);

        if (mToolManager != null) {
            String freeTextCacheFilename = mToolManager.getFreeTextCacheFileName();
            if (Utils.cacheFileExists(getContext(), freeTextCacheFilename)) {
                createRetrieveChangesDialog(freeTextCacheFilename);
            }
            if (mViewerConfig != null) {
                if (!mViewerConfig.isDocumentEditingEnabled()) {
                    mToolManager.setReadOnly(true);
                }
                if (!mViewerConfig.isLongPressQuickMenuEnabled()) {
                    mToolManager.setDisableQuickMenu(true);
                }
                if (mBottomNavBar != null) {
                    boolean canShowBookmark = mViewerConfig.isShowBookmarksView() &&
                        (mViewerConfig.isShowAnnotationsList() ||
                            mViewerConfig.isShowOutlineList() ||
                            mViewerConfig.isShowUserBookmarksList());
                    if (!canShowBookmark) {
                        mBottomNavBar.setMenuItemVisibility(ThumbnailSlider.POSITION_RIGHT, View.GONE);
                    }
                    if (!mViewerConfig.isShowThumbnailView()) {
                        mBottomNavBar.setMenuItemVisibility(ThumbnailSlider.POSITION_LEFT, View.GONE);
                    }
                }
            }
        }

        if (mImageStampDelayCreation) {
            mImageStampDelayCreation = false;
            ViewerUtils.createImageStamp(activity, mAnnotIntentData, mPdfViewCtrl, mOutputFileUri, mAnnotTargetPoint);
        }

        if (mFileAttachmentDelayCreation) {
            mFileAttachmentDelayCreation = false;
            ViewerUtils.createFileAttachment(getActivity(), mAnnotIntentData, mPdfViewCtrl, mAnnotTargetPoint);
        }

        if (Utils.isLargeScreenWidth(activity)) {
            mPdfViewCtrl.setFocusableInTouchMode(true);
            mPdfViewCtrl.requestFocus();
        }

        if (mAnnotationToolbarShow) {
            mAnnotationToolbarShow = false;
            if (mTabListener != null) {
                mTabListener.onOpenAnnotationToolbar(mAnnotationToolbarToolMode);
            }
        }
    }

    protected PDFViewCtrl.PagePresentationMode getPagePresentationModeFromSettings(String mode) {
        PDFViewCtrl.PagePresentationMode pagePresentationMode = PDFViewCtrl.PagePresentationMode.SINGLE;
        if (mode.equalsIgnoreCase(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_CONTINUOUS_VALUE)) {
            pagePresentationMode = PDFViewCtrl.PagePresentationMode.SINGLE_CONT;
        } else if (mode.equalsIgnoreCase(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_SINGLEPAGE_VALUE)) {
            pagePresentationMode = PDFViewCtrl.PagePresentationMode.SINGLE;
        } else if (mode.equalsIgnoreCase(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACING_VALUE)) {
            pagePresentationMode = PDFViewCtrl.PagePresentationMode.FACING;
        } else if (mode.equalsIgnoreCase(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACINGCOVER_VALUE)) {
            pagePresentationMode = PDFViewCtrl.PagePresentationMode.FACING_COVER;
        } else if (mode.equalsIgnoreCase(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACING_CONT_VALUE)) {
            pagePresentationMode = PDFViewCtrl.PagePresentationMode.FACING_CONT;
        } else if (mode.equalsIgnoreCase(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACINGCOVER_CONT_VALUE)) {
            pagePresentationMode = PDFViewCtrl.PagePresentationMode.FACING_COVER_CONT;
        }
        return pagePresentationMode;
    }

    protected PdfViewCtrlTabInfo getInfoFromRecentList(FileInfo fileInfo) {
        Activity activity = getActivity();
        if (activity == null) {
            return null;
        }

        // if it is a new tab let's use recent list's information
        FileInfo recentFileInfo = RecentFilesManager.getInstance().getFile(activity, fileInfo);
        return createTabInfoFromFileInfo(recentFileInfo);
    }

    protected PdfViewCtrlTabInfo createTabInfoFromFileInfo(FileInfo fileInfo) {
        PdfViewCtrlTabInfo info = new PdfViewCtrlTabInfo();
        if (fileInfo == null) {
            return null;
        }
        info.tabSource = fileInfo.getType();
        info.lastPage = fileInfo.getLastPage();
        info.pageRotation = fileInfo.getPageRotation();
        info.setPagePresentationMode(fileInfo.getPagePresentationMode());
        info.hScrollPos = fileInfo.getHScrollPos();
        info.vScrollPos = fileInfo.getVScrollPos();
        info.zoom = fileInfo.getZoom();
        info.isReflowMode = fileInfo.isReflowMode();
        info.reflowTextSize = fileInfo.getReflowTextSize();
        info.isRtlMode = fileInfo.isRtlMode();
        info.bookmarkDialogCurrentTab = fileInfo.getBookmarkDialogCurrentTab();
        return info;
    }

    /**
     * Saves the changes to the document forcefully
     */
    public void forceSave() {
        showDocumentSavedToast();
        save(true, true, false, true);
    }

    protected void showDocumentSavedToast() {
        Activity activity = getActivity();
        if (activity == null || isNotPdf()) {
            return;
        }

        if (mHasChangesSinceResumed) {
            mHasChangesSinceResumed = false;
            if (!mWasSavedAndClosedShown) {
                CommonToast.showText(activity, R.string.document_saved_toast_message, Toast.LENGTH_SHORT);
            }
        }
    }

    /**
     * Saves the changes to the document.
     *
     * @param close                True if the document should be closed
     * @param forceSave            True if save should be done forcefully
     * @param skipSpecialFileCheck True if special file check should be skipped
     */
    public void save(boolean close, boolean forceSave, boolean skipSpecialFileCheck) {
        save(close, forceSave, skipSpecialFileCheck, close);
    }

    /**
     * Saves the changes to the document.
     *
     * @param close                True if the document should be closed
     * @param forceSave            True if save should be done forcefully
     * @param skipSpecialFileCheck True if special file check should be skipped
     * @param upload               True if the document should be uploaded
     */
    public void save(boolean close, boolean forceSave, boolean skipSpecialFileCheck, boolean upload) {
        if (isNotPdf()) {
            return;
        }
        synchronized (saveDocumentLock) {
            if (mDocumentConversion == null && Utils.isDocModified(mPdfDoc)) {
                switch (mDocumentState) {
                    case PdfDocManager.DOCUMENT_STATE_CLEAN:
                    case PdfDocManager.DOCUMENT_STATE_NORMAL:
                    case PdfDocManager.DOCUMENT_STATE_MODIFIED:
                        mDocumentState = PdfDocManager.DOCUMENT_STATE_MODIFIED;
                        saveHelper(close, forceSave, true, upload);
                        break;
                    case PdfDocManager.DOCUMENT_STATE_READ_ONLY:
                        saveHelper(close, forceSave, false, upload);
                        break;
                    case PdfDocManager.DOCUMENT_STATE_READ_ONLY_AND_MODIFIED:
                        if (!skipSpecialFileCheck) {
                            handleSpecialFile(close);
                        }
                        break;
                    case PdfDocManager.DOCUMENT_STATE_CORRUPTED:
                        saveHelper(close, forceSave, false, upload);
                        break;
                    case PdfDocManager.DOCUMENT_STATE_CORRUPTED_AND_MODIFIED:
                        if (!skipSpecialFileCheck) {
                            handleSpecialFile(close);
                        }
                        break;
                    case PdfDocManager.DOCUMENT_STATE_FROM_CONVERSION:
                        // save changes to temp file
                        saveConversionTempHelper(close, forceSave, true);
                        break;
                    default:
                        if (close) {
                            // likely during conversion
                            // responsible for closing any cloud resources
                            saveHelper(true, forceSave, false, upload);
                        }
                        break;
                }
            } else {
                saveHelper(close, forceSave, false, upload);
            }
        }
    }

    protected void handleFailedSave(boolean close, Exception e) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        // we have trouble saving
        boolean handled = false;
        if (Utils.isLollipop() && mCurrentFile != null) {
            boolean isSDCardFile = Utils.isSdCardFile(activity, mCurrentFile);
            if (isSDCardFile) {
                // this is normal, no permission
                mDocumentState = PdfDocManager.DOCUMENT_STATE_READ_ONLY;
                handled = true;
            }
        }
        if (!handled) {
            mDocumentState = PdfDocManager.DOCUMENT_STATE_COULD_NOT_SAVE;
        }
        if (!mToolManager.isReadOnly()) {
            mToolManager.setReadOnly(true);
        }
        handleSpecialFile(close);
    }

    protected void saveHelper(boolean close, boolean forceSave, boolean hasChangesSinceLastSave, boolean upload) {
        if (forceSave) {
            if (mPdfViewCtrl != null) {
                mPdfViewCtrl.cancelRendering();
            }
        }
        switch (mTabSource) {
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
                if (hasChangesSinceLastSave) {
                    saveExternalFile(close, forceSave);
                }
                break;
            case BaseFileInfo.FILE_TYPE_FILE:
                if (hasChangesSinceLastSave) {
                    saveLocalFile(close, forceSave);
                }
                break;
            case BaseFileInfo.FILE_TYPE_EDIT_URI:
                if (hasChangesSinceLastSave) {
                    saveLocalFile(close, forceSave);
                }
                if (close) {
                    saveBackEditUri();
                }
                break;
        }
        if (hasChangesSinceLastSave && mDocumentState == PdfDocManager.DOCUMENT_STATE_MODIFIED) {
            mDocumentState = PdfDocManager.DOCUMENT_STATE_NORMAL;
        }

        if (forceSave && !close) {
            if (mPdfViewCtrl != null) {
                mPdfViewCtrl.requestRendering();
            }
        }
    }

    protected boolean docLock(boolean forceLock) {
        if (mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
            return false;
        }

        try {
            if (forceLock) {
                if (sDebug) {
                    Log.d(TAG, "PDFDoc FORCE LOCK");
                }
                mPdfViewCtrl.docLock(true);
                return true;
            } else {
                if (sDebug) {
                    Log.d(TAG, "PDFDoc TRY LOCK");
                }
                return mPdfViewCtrl.docTryLock(500);
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return false;
        }
    }

    protected void docUnlock() {
        if (mPdfViewCtrl == null) {
            return;
        }
        mPdfViewCtrl.docUnlock();
    }

    protected void checkDocIntegrity() {
        mHasChangesSinceOpened = true;
        mHasChangesSinceResumed = true;
    }

    /**
     * Saves local document file.
     *
     * @param close     True if the document should be closed
     * @param forceSave True if save should be forcefully
     */
    public void saveLocalFile(boolean close, boolean forceSave) {
        if (mCurrentFile != null) {
            if (Utils.isNotPdf(mCurrentFile.getAbsolutePath()))
                return;
            boolean shouldUnlock = false;
            try {
                shouldUnlock = docLock(close || forceSave);
                if (shouldUnlock) {
                    if (mPdfViewCtrl != null && mPdfViewCtrl.getDoc() == null) {
                        AnalyticsHandlerAdapter.getInstance().sendException(
                            new Exception("doc from PdfViewCtrl is null while we lock the document!"
                                + (mPdfDoc == null ? "" : " and the mPdfDoc is not null!")
                                + (" | source: " + mTabSource)));
                    }
                    if (sDebug) {
                        Log.d(TAG, "save local");
                        Log.d(TAG, "doc locked");
                    }
                    if (mToolManager.getUndoRedoManger() != null) {
                        mToolManager.getUndoRedoManger().takeUndoSnapshotForSafety();
                    }
                    mPdfDoc.save(mCurrentFile.getAbsolutePath(), SDFDoc.SaveMode.INCREMENTAL, null);
                    mLastSuccessfulSave = System.currentTimeMillis();
                    checkDocIntegrity();
                }
            } catch (Exception e) {
                // This file is most likely readonly (i.e. from external SD card for KitKat devices,
                // or can be a document with a repaired XRef table
                handleFailedSave(close, e);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    docUnlock();
                }
            }
        }
    }

    /**
     * Saves external document file.
     *
     * @param close     True if the document should be closed
     * @param forceSave True if save should be forcefully
     */
    public void saveExternalFile(boolean close, boolean forceSave) {
        if (mCurrentUriFile != null) {
            boolean shouldUnlock = false;
            try {
                shouldUnlock = docLock(close || forceSave);
                if (shouldUnlock) {
                    if (sDebug) {
                        Log.d(TAG, "save external file");
                        Log.d(TAG, "save external doc locked");
                    }
                    if (mToolManager.getUndoRedoManger() != null) {
                        mToolManager.getUndoRedoManger().takeUndoSnapshotForSafety();
                    }
                    mPdfDoc.save();
                    mLastSuccessfulSave = System.currentTimeMillis();
                    checkDocIntegrity();
                }
            } catch (Exception e) {
                handleFailedSave(close, e);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    docUnlock();
                }
            }
        }
    }

    private void saveBackEditUri() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mCurrentFile == null || !mCurrentFile.exists()) {
            return;
        }

        // about to close
        // let's try to write back the file
        if (mHasChangesSinceOpened) {
            // has changes
            mNeedsCleanupFile = false;
            InputStream is = null;
            OutputStream fos = null;
            FileInputStream fileInputStream = null;
            FileOutputStream fileOutputStream = null;
            RandomAccessFile raf = null;
            ParcelFileDescriptor pfd = null;
            boolean failed = true;
            boolean readWrite = false;
            ContentResolver contentResolver = Utils.getContentResolver(activity);
            if (contentResolver != null) {
                try {
                    pfd = contentResolver.openFileDescriptor(Uri.parse(mTabTag), "rw");
                    readWrite = true;
                } catch (Exception e) {
                    try {
                        pfd = contentResolver.openFileDescriptor(Uri.parse(mTabTag), "w");
                    } catch (Exception ignored) {
                    }
                }
            }
            if (pfd != null) {
                failed = false;
                try {
                    if (readWrite) {
                        fileInputStream = new FileInputStream(pfd.getFileDescriptor());

                        if (sDebug)
                            Log.d(TAG, "editUri | originalLength: " + mOriginalFileLength + " | stream: "
                                + fileInputStream.available() + " | localLength: " + mCurrentFile.length());

                        long originalFileLength = mOriginalFileLength;
                        long finalSize = mCurrentFile.length();
                        if (originalFileLength > finalSize) {
                            throw new Exception("Original file size is bigger than saved file size. Something went wrong");
                        }
                        // seek to the end of the file
                        long skipped = fileInputStream.skip(originalFileLength);
                        if (skipped == originalFileLength) {
                            // Append to the file
                            fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                            // Read backup copy's appended part
                            raf = new RandomAccessFile(mCurrentFile, "r");
                            raf.seek(originalFileLength);
                            long count = Utils.copyLarge(raf, fileOutputStream);
                            fileOutputStream.getChannel().truncate(finalSize);

                            if (sDebug)
                                Log.d(TAG, "seek to position: " + originalFileLength +
                                    " | sizeToWrite is: " + (finalSize - originalFileLength +
                                    " | actualWriteSize is: " + count) +
                                    " | truncate to: " + finalSize);
                        } else {
                            throw new Exception("Could not seek to size. Something went wrong");
                        }
                    } else {
                        is = new FileInputStream(mCurrentFile);
                        fos = new FileOutputStream(pfd.getFileDescriptor());
                        IOUtils.copy(is, fos);
                    }
                    mNeedsCleanupFile = true;
                } catch (OutOfMemoryError oom) {
                    failed = true;
                    Utils.manageOOM(getContext(), mPdfViewCtrl);
                } catch (Exception ex) {
                    failed = true;
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                } finally {
                    mHasChangesSinceOpened = false;
                    Utils.closeQuietly(is);
                    Utils.closeQuietly(fos);
                    Utils.closeQuietly(fileInputStream);
                    Utils.closeQuietly(fileOutputStream);
                    Utils.closeQuietly(raf);
                    Utils.closeQuietly(pfd);
                }
            }

            if (failed) {
                removeFromRecentList();
                File backupDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (mCurrentFile != null && mCurrentFile.exists() && mCurrentFile.getParent().equals(backupDir.getPath())) {
                    CommonToast.showText(activity, getString(R.string.document_notify_failed_commit_message, backupDir.getName()));
                } else {
                    CommonToast.showText(activity, R.string.document_save_error_toast_message);
                }
            }
        } else {
            mNeedsCleanupFile = true;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void saveConversionTempHelper(boolean close, boolean forceSave, boolean hasChangesSinceLastSave) {
        if (!hasChangesSinceLastSave) {
            return;
        }
        if (mTabConversionTempPath != null) {
            File file = new File(mTabConversionTempPath);
            boolean shouldUnlock = false;
            try {
                shouldUnlock = docLock(close || forceSave);
                if (shouldUnlock) {
                    if (sDebug) {
                        Log.d(TAG, "save Conversion Temp");
                        Log.d(TAG, "doc locked");
                    }
                    if (mToolManager.getUndoRedoManger() != null) {
                        mToolManager.getUndoRedoManger().takeUndoSnapshotForSafety();
                    }
                    mPdfDoc.save(file.getAbsolutePath(), SDFDoc.SaveMode.INCREMENTAL, null);
                }
            } catch (Exception e) {
                handleFailedSave(close, e);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    docUnlock();
                }
            }
        }
    }

    private void saveConversionTempCopy() {
        if (getActivity() == null) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mTabConversionTempPath = File.createTempFile("tmp", ".pdf", getActivity().getFilesDir()).getAbsolutePath();
            // make a copy of the buffer
            mPdfDoc.lock();
            shouldUnlock = true;
            mPdfDoc.save(mTabConversionTempPath, SDFDoc.SaveMode.REMOVE_UNUSED, null);
        } catch (Exception e) {
            handleFailedSave(false, e);
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(mPdfDoc);
            }
        }
    }

    /**
     * Handles if local file doesn't have write access.
     */
    public void localFileWriteAccessCheck() {
        if (!mLocalReadOnlyChecked) {
            // check once is enough for us to know if the file is read only
            mLocalReadOnlyChecked = true;
            if (mTabSource == BaseFileInfo.FILE_TYPE_FILE) {
                if (!isTabReadOnly()) {
                    boolean shouldUnlockRead = false;
                    try {
                        mPdfDoc.lockRead();
                        shouldUnlockRead = true;
                        boolean canSave = mPdfDoc.getSDFDoc().canSaveToPath(mCurrentFile.getAbsolutePath(), SDFDoc.SaveMode.INCREMENTAL);
                        mPdfDoc.unlockRead();
                        shouldUnlockRead = false;
                        if (!canSave) {
                            // This file is most likely readonly (i.e. from external SD card for KitKat devices)
                            mDocumentState = PdfDocManager.DOCUMENT_STATE_READ_ONLY;
                            mToolManager.setReadOnly(true);
                        }
                    } catch (Exception e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    } finally {
                        if (shouldUnlockRead) {
                            Utils.unlockReadQuietly(mPdfDoc);
                        }
                    }
                }
            } else if (mTabSource == BaseFileInfo.FILE_TYPE_EDIT_URI) {
                if (mCurrentFile == null || !mCurrentFile.exists()) {
                    mDocumentState = PdfDocManager.DOCUMENT_STATE_READ_ONLY;
                    mToolManager.setReadOnly(true);
                }
            }
        }
    }

    /**
     * Handles if the file is special case.
     *
     * @return True if the file is special case.
     */
    public boolean handleSpecialFile() {
        return handleSpecialFile(false);
    }

    protected boolean handleSpecialFile(boolean close) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        if (mTabSource == BaseFileInfo.FILE_TYPE_OPEN_URL) {
            // show warning message if the file is still being downloaded
            if (mToolManager.isReadOnly()) {
                CommonToast.showText(activity, R.string.download_not_finished_yet_with_changes_warning, Toast.LENGTH_SHORT);
                return true;
            }
        }

        switch (mDocumentState) {
            case PdfDocManager.DOCUMENT_STATE_READ_ONLY:
            case PdfDocManager.DOCUMENT_STATE_READ_ONLY_AND_MODIFIED:
                mDocumentState = PdfDocManager.DOCUMENT_STATE_READ_ONLY_AND_MODIFIED;
                CommonToast.showText(activity, R.string.document_read_only_error_message, Toast.LENGTH_SHORT);
                return true;
            case PdfDocManager.DOCUMENT_STATE_CORRUPTED:
            case PdfDocManager.DOCUMENT_STATE_CORRUPTED_AND_MODIFIED:
                mDocumentState = PdfDocManager.DOCUMENT_STATE_CORRUPTED_AND_MODIFIED;
                CommonToast.showText(activity, R.string.document_corrupted_error_message, Toast.LENGTH_SHORT);
                return true;
            case PdfDocManager.DOCUMENT_STATE_COULD_NOT_SAVE:
                CommonToast.showText(activity, R.string.document_save_error_toast_message, Toast.LENGTH_SHORT);
                return true;
            case PdfDocManager.DOCUMENT_STATE_DURING_CONVERSION:
                CommonToast.showText(activity, R.string.cant_edit_while_converting_message);
                return true;
            case PdfDocManager.DOCUMENT_STATE_FROM_CONVERSION:
                return true;
        }

        return false;
    }

    public void showReadOnlyAlert(DialogFragment dialogToDismiss) {
        CommonToast.showText(getContext(), R.string.document_read_only_warning_title);
    }

    /**
     * Handles sharing.
     */
    public void handleOnlineShare() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        switch (mTabSource) {
            case BaseFileInfo.FILE_TYPE_FILE:
            case BaseFileInfo.FILE_TYPE_EDIT_URI:
                Utils.sharePdfFile(activity, mCurrentFile);
                break;
            case BaseFileInfo.FILE_TYPE_OPEN_URL:
                // Show a dialog informing the user can't share a doc opened from a URL.
                if (mCurrentFile != null && mCurrentFile.isFile()) {
                    if (mToolManager.isReadOnly()) {
                        CommonToast.showText(activity, R.string.download_not_finished_yet_warning, Toast.LENGTH_SHORT);
                        return;
                    }
                    Utils.sharePdfFile(activity, mCurrentFile);
                }
                break;
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
                if (mCurrentUriFile != null) {
                    Utils.shareGenericFile(activity, mCurrentUriFile);
                }
                break;
            case BaseFileInfo.FILE_TYPE_OFFICE_URI:
                Utils.shareGenericFile(activity, Uri.parse(mTabTag));
                break;
        }
    }

    private void cancelUniversalConversion() {
        if (sDebug)
            Log.i("UNIVERSAL_TABCYCLE", FilenameUtils.getName(mTabTag) + " Cancels universal conversion");
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.closeDoc();
        }
        mViewerHost.setVisibility(View.INVISIBLE);
        mDocumentLoaded = false;
    }

    private String getUrlEncodedTabFilename() {
        String title = getTabTitleWithUniversalExtension();
        try {
            return URLEncoder.encode(title, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            Log.e(TAG, "We don't support utf-8 encoding for URLs?");
        }
        return title;
    }

    /**
     * Returns the item source of the tab.
     *
     * @return the item source of the tab
     */
    public int getTabSource() {
        return mTabSource;
    }

    /**
     * Returns the tab tag.
     *
     * @return the tab tag
     */
    public String getTabTag() {
        return mTabTag;
    }

    /**
     * Returns the filename of the tab
     *
     * @return filename without extension
     */
    public String getTabTitle() {
        return mTabTitle;
    }

    /**
     * Returns the filename of the tab
     *
     * @return filename with extension
     */
    public String getTabTitleWithExtension() {
        if (mTabTitle.toLowerCase().endsWith(".pdf")) {
            return mTabTitle;
        }
        return mTabTitle + ".pdf";
    }

    private String getTabTitleWithUniversalExtension() {
        String ext = Utils.getExtension(mTabTag);
        if (Utils.isNullOrEmpty(ext))
            ext = ".pdf";
        else
            ext = "." + ext;
        if (mTabTitle.toLowerCase().endsWith(ext)) {
            return mTabTitle;
        }
        return mTabTitle + ext;
    }

    /**
     * Handles when thumbnails view dialog is dismissed.
     *
     * @param pageNum          The selected page number
     * @param docPagesModified True if the document pages are modified.
     */
    public void onThumbnailsViewDialogDismiss(int pageNum, boolean docPagesModified) {
        resetHidePageNumberIndicatorTimer();

        if (mPdfViewCtrl == null) {
            return;
        }

        mPdfViewCtrl.resume();
        setCurrentPageHelper(pageNum, false);

        refreshPageCount();

        if (docPagesModified) {
            onDocPagesModified();
        }

        if (mIsReflowMode) {
            // hide and pause PDFViewCtrl when in reflow mode
            mViewerHost.setVisibility(View.INVISIBLE);
            mPdfViewCtrl.pause();
        }
    }

    /**
     * Handles when document pages are modified.
     */
    public void onDocPagesModified() {
        if (mPdfViewCtrl == null) {
            return;
        }

        clearPageBackAndForwardStacks();

        // reset the reflow adapter
        if (mIsReflowMode && mReflowControl != null) {
            try {
                mReflowControl.notifyPagesModified();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    public void onAddNewPages(Page[] pages) {
        if (pages == null || pages.length == 0 || mPdfViewCtrl == null) {
            return;
        }
        PDFDoc doc = mPdfViewCtrl.getDoc();
        if (doc == null) {
            return;
        }

        int currentPage = 0;
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            List<Integer> pageList = new ArrayList<>();
            for (int i = 1, cnt = pages.length; i <= cnt; i++) {
                int newPageNum = mPdfViewCtrl.getCurrentPage() + i;
                pageList.add(newPageNum);
                doc.pageInsert(doc.getPageIterator(newPageNum), pages[i - 1]);
            }
            mPageCount = doc.getPageCount();
            currentPage = mPdfViewCtrl.getCurrentPage() + 1;
            mPdfViewCtrl.setCurrentPage(currentPage);
            updatePageIndicator();

            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (toolManager != null) {
                toolManager.raisePagesAdded(pageList);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
            try {
                mPdfViewCtrl.updatePageLayout();
                onThumbnailsViewDialogDismiss(currentPage, true);
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        onDocPagesModified();
    }

    @SuppressWarnings("unused")
    public void onAddNewPage(Page page) {
        if (page == null) {
            return;
        }
        Page[] pages = new Page[1];
        pages[0] = page;
        onAddNewPages(pages);
    }

    public void onDeleteCurrentPage() {
        if (mPdfViewCtrl == null) {
            return;
        }

        PDFDoc doc = mPdfViewCtrl.getDoc();
        boolean shouldUnlock = false;
        int currentPageNum = 0;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            currentPageNum = mPdfViewCtrl.getCurrentPage();
            doc.pageRemove(doc.getPageIterator(currentPageNum));
            mPageCount = doc.getPageCount();
            updatePageIndicator();

            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (toolManager != null) {
                List<Integer> pageList = new ArrayList<>(1);
                pageList.add(currentPageNum);
                toolManager.raisePagesDeleted(pageList);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return;
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
            try {
                onThumbnailsViewDialogDismiss(currentPageNum, true);
                mPdfViewCtrl.updatePageLayout();
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        onDocPagesModified();
    }

    protected void openLocalFile(String tag) {
        try {
            if (mTabSource == BaseFileInfo.FILE_TYPE_FILE && !Utils.isNotPdf(tag)) {
                mPdfDoc = new PDFDoc(tag);
                checkPdfDoc();
            }
        } catch (Exception e) {
            if (mCurrentFile != null && !mCurrentFile.exists()) {
                // does not exist
                mErrorCode = PdfDocManager.DOCUMENT_SETDOC_ERROR_NOT_EXIST;
            } else {
                // document is damaged
                mErrorCode = PdfDocManager.DOCUMENT_SETDOC_ERROR_CORRUPTED;
            }
            handleOpeningDocumentFailed(mErrorCode);
        }
    }

    protected void openExternalFile(String tag) {
        if (!Utils.isNullOrEmpty(tag) && getContext() != null) {
            mCurrentUriFile = Uri.parse(tag);
            mPdfDoc = null;
            if (mPDFDocLoaderTask != null && mPDFDocLoaderTask.getStatus() != AsyncTask.Status.FINISHED) {
                mPDFDocLoaderTask.cancel(true);
            }
            mPDFDocLoaderTask = new PDFDocLoaderTask(getContext());
            mPDFDocLoaderTask.setFinishCallback(new PDFDocLoaderTask.onFinishListener() {
                @Override
                public void onFinish(PDFDoc pdfDoc) {
                    mPdfDoc = pdfDoc;
                    if (mPdfDoc == null) {
                        handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_NULL_PDFDOC);
                        return;
                    }
                    try {
                        checkPdfDoc();
                    } catch (Exception e) {
                        mPdfDoc = null;
                        handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_NULL_PDFDOC);
                        AnalyticsHandlerAdapter.getInstance().sendException(e, "checkPdfDoc");
                    }
                }

                @Override
                public void onCancelled() {
                    // do nothing
                }
            })
                .execute(mCurrentUriFile);
        }
    }

    protected void openEditUriFile(String uri) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        File file = Utils.duplicateInDownload(activity, Uri.parse(uri), getTabTitleWithExtension());
        if (file != null) {
            mCurrentFile = file;
            mOriginalFileLength = mCurrentFile.length();
            if (mOriginalFileLength <= 0) {
                mCurrentFile = null;
            } else {
                if (sDebug)
                    Log.d(TAG, "save edit uri file to: " + mCurrentFile.getAbsolutePath());
            }
        }

        if (mCurrentFile != null) {
            try {
                mPdfDoc = new PDFDoc(mCurrentFile.getAbsolutePath());
                checkPdfDoc();
            } catch (Exception e) {
                mPdfDoc = null;
                handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_NULL_PDFDOC);
                String path = mCurrentFile.getAbsolutePath();
                AnalyticsHandlerAdapter.getInstance().sendException(e, "checkPdfDoc " + path);
            }
        } else {
            handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_NULL_PDFDOC);
        }
    }

    protected void openUrlFile(String tag) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return;
        }

        try {
            mCanAddToTabInfo = false;
            mToolManager.setReadOnly(true);
            if (Utils.isNotPdf(tag)) {
                BasicHTTPDownloadTask.BasicHTTPDownloadTaskListener downListener = new BasicHTTPDownloadTask.BasicHTTPDownloadTaskListener() {
                    @Override
                    public void onDownloadTask(Boolean pass, File saveFile) {
                        if (mDownloadDocumentDialog != null && mDownloadDocumentDialog.isShowing()) {
                            mDownloadDocumentDialog.dismiss();
                        }
                        if (!pass) {
                            mErrorCode = PdfDocManager.DOCUMENT_SETDOC_ERROR_NULL_PDFDOC;
                            handleOpeningDocumentFailed(mErrorCode);
                        } else {
                            openOfficeDoc(saveFile.getAbsolutePath(), false);
                        }
                    }
                };

                File backupDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!backupDir.isDirectory()) {
                    //noinspection ResultOfMethodCallIgnored
                    backupDir.mkdirs();
                }
                File saveFile = new File(backupDir, FilenameUtils.getName(tag));
                saveFile = new File(Utils.getFileNameNotInUse(saveFile.getAbsolutePath()));
                mCurrentFile = saveFile;
                new BasicHTTPDownloadTask(activity, downListener, tag, saveFile).execute();
            } else {
                String cacheFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    + File.separator + getUrlEncodedTabFilename();
                if (!Utils.isNullOrEmpty(cacheFile)) {
                    cacheFile = Utils.getFileNameNotInUse(cacheFile);
                    mCurrentFile = new File(cacheFile);
                }
                mPdfViewCtrl.openUrlAsync(tag, cacheFile, "", null);
                mDownloading = true;
                mDownloadDocumentDialog.show();
                //mPdfViewCtrl.requestRenderingAsync();
            }
        } catch (Exception e) {
            if (mDownloadDocumentDialog != null && mDownloadDocumentDialog.isShowing()) {
                mDownloadDocumentDialog.dismiss();
            }
            mErrorCode = PdfDocManager.DOCUMENT_SETDOC_ERROR_NULL_PDFDOC;
            handleOpeningDocumentFailed(mErrorCode);
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    protected void openOfficeDoc(String tag, boolean isUri) {
        openOfficeDoc(tag, isUri, null);
    }

    protected void openOfficeDoc(String tag, boolean isUri, String pageOptionsJson) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null || Utils.isNullOrEmpty(tag)) {
            return;
        }

        mPdfDoc = null;
        mNonPdfDoc = true;

        if (isUri) {
            Uri uri = Uri.parse(tag);
            mIsOfficeDoc = Utils.isOfficeDocument(activity.getContentResolver(), uri);
        } else {
            mIsOfficeDoc = Utils.isOfficeDocument(tag);
        }

        try {
            // if URI not seekable, duplicate the file
            if (isUri) {
                Uri uri = Uri.parse(tag);
                if (!Utils.isUriSeekable(activity, uri)) {
                    String extension = Utils.getUriExtension(activity.getContentResolver(), uri);
                    String title = getTabTitle();
                    File file = Utils.duplicateInDownload(activity, uri, title + "." + extension);
                    if (file != null && file.exists()) {
                        isUri = false;
                        tag = file.getAbsolutePath();
                        mNeedsCleanupFile = true;
                    }
                }
            }

            if (!isUri) {
                mCurrentFile = new File(tag);
                if (!mCurrentFile.exists()) {
                    handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_NOT_EXIST);
                    return;
                }
                if (Utils.isNullOrEmpty(mTabConversionTempPath)) {
                    WordToPDFOptions options = null;
                    if (!Utils.isNullOrEmpty(pageOptionsJson)) {
                        if (sDebug)
                            Log.d(TAG, "PageSizes: " + pageOptionsJson);
                        options = new WordToPDFOptions(pageOptionsJson);
                    }
                    if (null == options) {
                        // chop off white space
                        if (sDebug)
                            Log.d(TAG, "DPI 96.0");
                        options = new WordToPDFOptions("{\"DPI\": 96.0}");
                    }
                    mDocumentConversion = mPdfViewCtrl.openNonPDFUri(Uri.fromFile(mCurrentFile), options);
                }
            } else {
                Uri uri = Uri.parse(tag);
                mCurrentUriFile = uri;
                if (Utils.isNullOrEmpty(mTabConversionTempPath)) {
                    mDocumentConversion = mPdfViewCtrl.openNonPDFUri(uri, null);
                }
            }

            mUniversalConverted = true;
            mDocumentLoaded = false;

            if (Utils.isNullOrEmpty(mTabConversionTempPath)) {
                mDocumentState = PdfDocManager.DOCUMENT_STATE_DURING_CONVERSION;
            } else {
                // if have temp file, load temp file
                mPdfDoc = new PDFDoc(mTabConversionTempPath);
                checkPdfDoc();
                mDocumentState = PdfDocManager.DOCUMENT_STATE_FROM_CONVERSION;
            }

            mShouldNotifyWhenConversionFinishes = false;
            mConversionFinishedMessageHandler.postDelayed(mConversionFinishedMessageRunnable, MAX_CONVERSION_TIME_WITHOUT_NOTIFICATION);
            mIsEncrypted = false;

            mToolManager.setTool(mToolManager.createTool(ToolMode.PAN, null));

            mProgressBarLayout.show();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_NULL_PDFDOC);
        }
    }

    protected void checkPdfDoc() throws PDFNetException {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null || mPdfDoc == null) {
            return;
        }
        mDocumentLoading = false;

        mDocumentLoaded = false;
        mDocumentState = PdfDocManager.DOCUMENT_STATE_CLEAN;

        boolean shouldUnlockRead = false;
        boolean hasRepairedXRef;
        boolean initStdSecurityHandler;
        int pageCount = 0;
        try {
            mPdfDoc.lockRead();
            shouldUnlockRead = true;
            hasRepairedXRef = mPdfDoc.hasRepairedXRef();
            initStdSecurityHandler = mPdfDoc.initStdSecurityHandler(mPassword);
            if (initStdSecurityHandler) {
                // cannot get page count when the given security is not valid
                pageCount = mPdfDoc.getPageCount();
            }
        } finally {
            if (shouldUnlockRead) {
                Utils.unlockReadQuietly(mPdfDoc);
            }
        }

        if (!initStdSecurityHandler) {
            loadPasswordView();
            // needs password
            mProgressBarLayout.hide(true);
            if (sDebug)
                Log.d(TAG, "hide progress bar");
            mPasswordLayout.setVisibility(View.VISIBLE);
            mPasswordInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
            return;
        }
        if (mPasswordLayout != null) {
            mPasswordLayout.setVisibility(View.GONE);
        }

        if (hasRepairedXRef) {
            mToolManager.setReadOnly(true);
            mDocumentState = PdfDocManager.DOCUMENT_STATE_CORRUPTED;
        }

        if (pageCount < 1) {
            handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_ZERO_PAGE);
        } else {
            mPdfViewCtrl.setDoc(mPdfDoc);
            if (mCurrentFile != null) {
                if (!mCurrentFile.canWrite()) {
                    mToolManager.setReadOnly(true);
                    if (mDocumentState != PdfDocManager.DOCUMENT_STATE_CORRUPTED) {
                        mDocumentState = PdfDocManager.DOCUMENT_STATE_READ_ONLY;
                    }
                }
            }
            long size = getCurrentFileSize();
            boolean canSave = Utils.hasEnoughStorageToSave(size);
            if (!canSave) {
                mToolManager.setReadOnly(true);
                mDocumentState = PdfDocManager.DOCUMENT_STATE_OUT_OF_SPACE;
            }
            mPageCount = pageCount;

            // We only want to generate thumbs for non-secured documents.
            if (mPassword != null && mPassword.isEmpty()) {
                if (mCurrentFile != null) {
                    RecentlyUsedCache.accessDocument(mCurrentFile.getAbsolutePath());
                } else {
                    if (!mUniversalConverted) {
                        RecentlyUsedCache.accessDocument(mTabTag, mPdfDoc);
                    }
                }
            }
            mIsEncrypted = (mPassword != null && !mPassword.isEmpty());

            if (mToolManager != null && mToolManager.getTool() == null) {
                mToolManager.setTool(mToolManager.createTool(ToolMode.PAN, null));
            }

            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                Fragment thumbFragment = fragmentManager.findFragmentByTag("thumbnails_fragment");
                if (thumbFragment != null && thumbFragment.getView() != null) {
                    if (thumbFragment instanceof ThumbnailsViewFragment) {
                        ((ThumbnailsViewFragment) thumbFragment).addDocPages();
                    }
                }
            }
        }
    }

    /**
     * Handles key when pressed up.
     *
     * @param keyCode The key code
     * @param event   The key event
     * @return True if the key is handled
     */
    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || !isDocumentReady() || mPdfViewCtrl == null) {
            return false;
        }

        if (mAnnotationToolbar == null) {
            createAnnotationToolbar();
        }

        if (mAnnotationToolbar.handleKeyUp(keyCode, event)) {
            return true;
        }

        if (ShortcutHelper.isUndo(keyCode, event)) {
            mAnnotationToolbar.closePopups();
            if (mTabListener != null) {
                mTabListener.onUndoRedoPopupClosed();
            }
            undo();
            return true;
        }

        if (ShortcutHelper.isRedo(keyCode, event)) {
            mAnnotationToolbar.closePopups();
            if (mTabListener != null) {
                mTabListener.onUndoRedoPopupClosed();
            }
            redo();
            return true;
        }

        if (ShortcutHelper.isPrint(keyCode, event)) {
            handlePrintAnnotationSummary();
            return true;
        }

        if (ShortcutHelper.isAddBookmark(keyCode, event)) {
            addPageToBookmark();
            return true;
        }

        if (!mPageBackStack.isEmpty() && ShortcutHelper.isJumpPageBack(keyCode, event)) {
            jumpPageBack();
            return true;
        }

        if (!mPageForwardStack.isEmpty() && ShortcutHelper.isJumpPageForward(keyCode, event)) {
            jumpPageForward();
            return true;
        }

        if (ShortcutHelper.isRotateClockwise(keyCode, event)) {
            try {
                mPdfViewCtrl.rotateClockwise();
                mPdfViewCtrl.updatePageLayout();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
            return true;
        }

        if (ShortcutHelper.isRotateCounterClockwise(keyCode, event)) {
            try {
                mPdfViewCtrl.rotateCounterClockwise();
                mPdfViewCtrl.updatePageLayout();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
            return true;
        }

        boolean isZoomIn = ShortcutHelper.isZoomIn(keyCode, event);
        boolean isZoomOut = ShortcutHelper.isZoomOut(keyCode, event);
        boolean isResetZoom = ShortcutHelper.isResetZoom(keyCode, event);
        if (isZoomIn || isZoomOut || isResetZoom) {
            final ToolManager.Tool tool = mToolManager.getTool();
            if (tool instanceof TextSelect) {
                ((TextSelect) tool).closeQuickMenu();
                ((TextSelect) tool).clearSelection();
            }

            if (isZoomIn) {
                mPdfViewCtrl.setZoom(0, 0, mPdfViewCtrl.getZoom() * PDFViewCtrl.SCROLL_ZOOM_FACTOR, true, true);
            } else if (isZoomOut) {
                mPdfViewCtrl.setZoom(0, 0, mPdfViewCtrl.getZoom() / PDFViewCtrl.SCROLL_ZOOM_FACTOR, true, true);
            } else { // if (isResetZoom) {
                PointF point = mPdfViewCtrl.getCurrentMousePosition();
                resetZoom(point);
            }

            if (tool instanceof TextSelect) {
                mResetTextSelectionHandler.removeCallbacksAndMessages(null);
                mResetTextSelectionHandler.postDelayed(mResetTextSelectionRunnable, 500);
            } else if (tool instanceof AnnotEdit) {
                mToolManager.setTool(mToolManager.createTool(((AnnotEdit) tool).getCurrentDefaultToolMode(), tool));
            }

            return true;
        }

        if (ShortcutHelper.isGotoFirstPage(keyCode, event)) {
            setCurrentPageHelper(1, true);
            return true;
        }

        if (ShortcutHelper.isGotoLastPage(keyCode, event)) {
            setCurrentPageHelper(mPdfViewCtrl.getPageCount(), true);
            return true;
        }

        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int widthStep = screenWidth / 8;
        int heightStep = screenHeight / 8;

        if (ShortcutHelper.isPageUp(keyCode, event)) {
            int dy = mPdfViewCtrl.getHeight() - heightStep;
            int y = mPdfViewCtrl.getScrollY();
            mPdfViewCtrl.scrollBy(0, -dy);
            int newY = mPdfViewCtrl.getScrollY();
            if (y == newY) {
                mPdfViewCtrl.gotoPreviousPage();
            }
        }

        if (ShortcutHelper.isPageDown(keyCode, event)) {
            int dy = mPdfViewCtrl.getHeight() - heightStep;
            int y = mPdfViewCtrl.getScrollY();
            mPdfViewCtrl.scrollBy(0, dy);
            int newY = mPdfViewCtrl.getScrollY();
            if (y == newY) {
                mPdfViewCtrl.gotoNextPage();
            }
            return true;
        }

        if (ViewerUtils.isViewerZoomed(mPdfViewCtrl)) {
            // zoomed: scroll accordingly

            if (ShortcutHelper.isScrollToLeft(keyCode, event)) {
                if (!mPdfViewCtrl.turnPageInNonContinuousMode(mPdfViewCtrl.getCurrentPage(), false)) {
                    mPdfViewCtrl.scrollBy(-widthStep, 0);
                }
                return true;
            }
            if (ShortcutHelper.isScrollToUp(keyCode, event)) {
                mPdfViewCtrl.scrollBy(0, -heightStep);
                return true;
            }
            if (ShortcutHelper.isScrollToRight(keyCode, event)) {
                if (!mPdfViewCtrl.turnPageInNonContinuousMode(mPdfViewCtrl.getCurrentPage(), true)) {
                    mPdfViewCtrl.scrollBy(widthStep, 0);
                }
                return true;
            }
            if (ShortcutHelper.isScrollToDown(keyCode, event)) {
                mPdfViewCtrl.scrollBy(0, heightStep);
                return true;
            }
        } else {
            // not zoomed: turn page accordingly

            if (ShortcutHelper.isScrollToLeft(keyCode, event)) {
                mPdfViewCtrl.gotoPreviousPage();
                return true;
            }
            if (ShortcutHelper.isScrollToUp(keyCode, event)) {
                if (isContinuousPageMode()) {
                    mPdfViewCtrl.scrollBy(0, -heightStep);
                } else {
                    mPdfViewCtrl.gotoPreviousPage();
                }
                return true;
            }
            if (ShortcutHelper.isScrollToRight(keyCode, event)) {
                mPdfViewCtrl.gotoNextPage();
                return true;
            }
            if (ShortcutHelper.isScrollToDown(keyCode, event)) {
                if (isContinuousPageMode()) {
                    mPdfViewCtrl.scrollBy(0, heightStep);
                } else {
                    mPdfViewCtrl.gotoNextPage();
                }
                return true;
            }
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (getToolManager() != null && getToolManager().getTool() != null && ((Tool) getToolManager().getTool()).isEditingAnnot()) {
                mPdfViewCtrl.closeTool();
                return true;
            }
            if (mTabListener != null) {
                return mTabListener.onBackPressed();
            }
        }

        // if in annotation mode, swallow all other shortcuts
        return isAnnotationMode();
    }

    protected void handleOpeningDocumentFailed(int errorCode) {
        handleOpeningDocumentFailed(errorCode, "");
    }

    protected void handleOpeningDocumentFailed(int errorCode, String info) {
        mDocumentLoading = false;
        mCanAddToTabInfo = false;
        mErrorOnOpeningDocument = true;
        mErrorCode = errorCode;
        if (mTabListener != null) {
            mTabListener.onTabError(mErrorCode, info);
        }
    }

    protected void handlePrintAnnotationSummary() {
        if (checkTabConversionAndAlert(R.string.cant_print_while_converting_message, true, false)) {
            return;
        }

        PrintAnnotationsSummaryDialogFragment dialog = PrintAnnotationsSummaryDialogFragment.newInstance(
            mPrintDocumentChecked, mPrintAnnotationsChecked, mPrintSummaryChecked);
        dialog.setTargetFragment(this, RequestCode.PRINT);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialog.show(fragmentManager, "print_annotations_summary_dialog");
        }
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_PRINT);
    }

    protected void addPageToBookmark() {
        Activity activity = getActivity();
        if (activity == null || isTabReadOnly()) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(false);
            shouldUnlock = true;

            int currentPage = mPdfViewCtrl.getCurrentPage();
            long pageObjNum = mPdfViewCtrl.getDoc().getPage(currentPage).getSDFObj().getObjNum();
            if (isTabReadOnly()) {
                BookmarkManager.addUserBookmark(activity, mPdfViewCtrl.getDoc().getFileName(), pageObjNum, currentPage);
            } else {
                BookmarkManager.addPdfBookmark(activity, mPdfViewCtrl, pageObjNum, currentPage);
            }
            CommonToast.showText(activity, R.string.controls_misc_bookmark_added);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    protected void resetZoom(PointF point) {
        PDFViewCtrl.PageViewMode refMode;
        if (mPdfViewCtrl.isMaintainZoomEnabled()) {
            refMode = mPdfViewCtrl.getPreferredViewMode();
        } else {
            refMode = mPdfViewCtrl.getPageRefViewMode();
        }
        mPdfViewCtrl.setPageViewMode(refMode, (int) point.x, (int) point.y, true);
    }

    protected void sentToPrinterDialog() {
        int printContent = 0;
        if (mPrintDocumentChecked) {
            printContent |= Print.PRINT_CONTENT_DOCUMENT_BIT;
        }
        if (mPrintAnnotationsChecked) {
            printContent |= Print.PRINT_CONTENT_ANNOTATION_BIT;
        }
        if (mPrintSummaryChecked) {
            printContent |= Print.PRINT_CONTENT_SUMMARY_BIT;
        }
        handlePrintJob(printContent);
    }

    private void handlePrintJob(int _printContent) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return;
        }

        if (_printContent < 1 || _printContent > (Print.PRINT_CONTENT_DOCUMENT_BIT |
            Print.PRINT_CONTENT_ANNOTATION_BIT | Print.PRINT_CONTENT_SUMMARY_BIT)) {
            return;
        }
        Integer printContent = _printContent;
        Boolean isRtl = isRtlMode();

        try {
            if (mTabSource == BaseFileInfo.FILE_TYPE_OPEN_URL) {
                Print.startPrintJob(activity, getString(R.string.app_name), mPdfViewCtrl.getDoc(), printContent, isRtl);
            } else {
                Print.startPrintJob(activity, getString(R.string.app_name), mPdfDoc, printContent, isRtl);
            }
        } catch (Exception e) {
            CommonToast.showText(activity, R.string.error_printing_file, Toast.LENGTH_SHORT);
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Called when document annotations have been exported.
     *
     * @param outputDoc The PDFDoc containing the exported annotations
     */
    public void onExportAnnotations(PDFDoc outputDoc) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (mTabSource == BaseFileInfo.FILE_TYPE_FILE) {
            handleExportAnnotations(mCurrentFile.getParentFile(), outputDoc);
        } else if (mTabSource == BaseFileInfo.FILE_TYPE_EXTERNAL) {
            ExternalFileInfo fileInfo = Utils.buildExternalFile(activity, mCurrentUriFile);
            if (fileInfo != null) {
                handleExportAnnotations(fileInfo.getParent(), outputDoc);
            }
        }
    }

    protected void handleExportAnnotations(File folder, PDFDoc outputDoc) {
        boolean shouldUnlock = false;
        File outputFile = null;
        boolean success = false;

        if (folder == null || outputDoc == null) {
            return;
        }

        try {
            String extension = getString(R.string.document_export_annotations_extension);
            File tempFile = new File(folder, mTabTitle + extension + ".pdf");
            String outputPath = Utils.getFileNameNotInUse(tempFile.getAbsolutePath());
            if (Utils.isNullOrEmpty(outputPath)) {
                return;
            }
            outputFile = new File(outputPath);
            outputDoc.lock();
            shouldUnlock = true;
            outputDoc.save(outputFile.getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null);
            success = true;
        } catch (Exception ePDFNet) {
            AnalyticsHandlerAdapter.getInstance().sendException(ePDFNet);
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(outputDoc);
            }
            Utils.closeQuietly(outputDoc);
        }

        if (success) {
            openFileInNewTab(outputFile);
        }
    }

    protected void handleExportAnnotations(ExternalFileInfo folder, PDFDoc outputDoc) {
        Activity activity = getActivity();
        if (activity == null || folder == null || outputDoc == null) {
            return;
        }

        boolean shouldUnlock = false;
        SecondaryFileFilter filter = null;
        try {
            String extension = getString(R.string.document_export_annotations_extension);
            String outputPath = Utils.getFileNameNotInUse(folder, mTabTitle + extension + ".pdf");
            ExternalFileInfo outputFile = folder.createFile("application/pdf", outputPath);

            if (outputFile != null) {
                outputDoc.lock();
                shouldUnlock = true;

                filter = new SecondaryFileFilter(activity, outputFile.getUri());
                outputDoc.save(filter, SDFDoc.SaveMode.REMOVE_UNUSED);
                openFileUriInNewTab(outputFile.getUri());
            }
        } catch (Exception ePDFNet) {
            AnalyticsHandlerAdapter.getInstance().sendException(ePDFNet);
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(outputDoc);
            }
            Utils.closeQuietly(outputDoc, filter);
        }
    }

    /**
     * Returns the URI file
     *
     * @return the URI file
     */
    public Uri getUriFile() {
        return mCurrentUriFile;
    }

    /**
     * Returns the file.
     *
     * @return the file
     */
    public File getFile() {
        return mCurrentFile;
    }

    /**
     * Refreshes the number of pages.
     */
    public void refreshPageCount() {
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            mPageCount = mPdfViewCtrl.getDoc().getPageCount();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        if (mBottomNavBar != null) {
            mBottomNavBar.refreshPageCount();
        }
        updatePageIndicator();
    }

    /**
     * Checks whether the document can be saved.
     *
     * @return True if the document can be saved
     */
    public boolean canDocBeSaved() {
        return mDocumentState != PdfDocManager.DOCUMENT_STATE_DURING_CONVERSION;
    }

    /**
     * Checks whether the document is not PDF.
     *
     * @return True if the document is not PDF
     */
    public boolean isNotPdf() {
        return Utils.isNotPdf(mTabTag);
    }

    /**
     * Updates the color mode.
     */
    public void updateColorMode() {
        FragmentActivity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return;
        }

        int colorMode = PdfViewCtrlSettingsManager.getColorMode(activity);
        int clientBackgroundColor = getPDFViewCtrlConfig(activity).getClientBackgroundColor();
        int mode = PDFRasterizer.e_postprocess_none;
        switch (colorMode) {
            case PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_CUSTOM:
                int customBGColor = PdfViewCtrlSettingsManager.getCustomColorModeBGColor(activity);
                int customTxtColor = PdfViewCtrlSettingsManager.getCustomColorModeTextColor(activity);
                clientBackgroundColor = getViewerBackgroundColor(customBGColor);
                mode = PDFRasterizer.e_postprocess_gradient_map;

                try {
                    mPdfViewCtrl.setColorPostProcessMode(mode);
                    mPdfViewCtrl.setClientBackgroundColor(
                        Color.red(clientBackgroundColor),
                        Color.green(clientBackgroundColor),
                        Color.blue(clientBackgroundColor), false);
                    mPdfViewCtrl.setColorPostProcessColors(customBGColor, customTxtColor);
                    mPdfViewCtrl.resume();
                    mPdfViewCtrl.update(true);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
                mViewerHost.setBackgroundColor(mPdfViewCtrl.getClientBackgroundColor());

                mToolManager.setNightMode(isNightModeForToolManager());
                if (mIsReflowMode) {
                    updateReflowColorMode();
                }
                break;
            case PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_SEPIA:
                mode = PDFRasterizer.e_postprocess_gradient_map;
                InputStream is = null;
                OutputStream os = null;
                try {
                    File filterFile = new File(activity.getCacheDir(), "sepia_mode_filter.png");
                    if (!filterFile.exists() || !filterFile.isFile()) {
                        is = getResources().openRawResource(R.raw.sepia_mode_filter);
                        os = new FileOutputStream(filterFile);
                        IOUtils.copy(is, os);
                    }
                    mPdfViewCtrl.setColorPostProcessMapFile(new MappedFile(filterFile.getAbsolutePath()));
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    Utils.closeQuietly(is);
                    Utils.closeQuietly(os);
                }
                break;
            case PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_NIGHT:
                clientBackgroundColor = getPDFViewCtrlConfig(activity).getClientBackgroundColorDark();
                mode = PDFRasterizer.e_postprocess_night_mode;
                break;
        }

        try {
            mPdfViewCtrl.setColorPostProcessMode(mode);
            mPdfViewCtrl.setClientBackgroundColor(
                Color.red(clientBackgroundColor),
                Color.green(clientBackgroundColor),
                Color.blue(clientBackgroundColor), false);
            mPdfViewCtrl.update(true);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        mViewerHost.setBackgroundColor(mPdfViewCtrl.getClientBackgroundColor());

        mToolManager.setNightMode(isNightModeForToolManager());
        if (mIsReflowMode) {
            updateReflowColorMode();
        }
    }

    private static int getViewerBackgroundColor(int color) {
        float[] hsv = new float[3];
        RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv);
        float hue = hsv[0] / 360f;
        float saturation = hsv[1];
        float value = hsv[2];

        float lowEarthHue = 0.05f;
        float highEarthHue = 0.11f;
        boolean earthTones = hue >= lowEarthHue && hue <= highEarthHue;
        if (value > 0.5) {
            if (earthTones) {
                value -= 0.2;
                saturation = Math.min(saturation * 2, Math.min(saturation + 0.05f, 1.0f));
            } else {
                value *= 0.6;
            }
        } else if (value >= 0.3) {
            value = (value / 2) + 0.05f;
        } else if (value >= 0.1) {
            value -= 0.1;
        } else {
            value += 0.1;
        }
        if (!earthTones) {
            float dist = Math.min(0.05f, lowEarthHue - hue);
            if (hue > highEarthHue) {
                dist = Math.min(0.05f, hue - highEarthHue);
            }
            saturation = saturation - (saturation * (20f * dist) * 0.6f);
        }

        hsv[0] = hue * 360f;
        hsv[1] = saturation;
        hsv[2] = value;
        return HSVToColor(hsv);

    }

    /**
     * Updates color mode in reflow
     */
    protected void updateReflowColorMode() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mReflowControl != null && mReflowControl.isReady()) {
            try {
                switch (PdfViewCtrlSettingsManager.getColorMode(activity)) {
                    case PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_NORMAL:
                        mReflowControl.setDayMode();
                        break;
                    case PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_SEPIA:
                        mReflowControl.setCustomColorMode(0xFFffead2);
                        break;
                    case PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_NIGHT:
                        mReflowControl.setNightMode();
                        break;
                    case PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_CUSTOM:
                        mReflowControl.setCustomColorMode(PdfViewCtrlSettingsManager.getCustomColorModeBGColor(activity));
                        break;
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * Specifies that color mode has been changed.
     */
    public void setColorModeChanged() {
        mColorModeChanged = true;
    }

    // TODO: Part of listener/callback/interface?

    /**
     * Called when the {@link android.support.v7.widget.Toolbar toolbar} of the containing
     * {@link PdfViewCtrlTabHostFragment host fragment} will be shown.
     *
     * @return {@code true} if the toolbar can be shown, {@code false} otherwise.
     */
    public boolean onShowToolbar() {
        return true;
    }

    /**
     * Called when the {@link android.support.v7.widget.Toolbar toolbar} of the containing
     * {@link PdfViewCtrlTabHostFragment host fragment} will be hidden.
     *
     * @return {@code true} if the toolbar can be hidden, {@code false} otherwise.
     */
    public boolean onHideToolbars() {
        return !mBottomNavBar.isProgressChanging();
    }

    /**
     * Called before the containing {@link PdfViewCtrlTabHostFragment host fragment} enters
     * fullscreen mode (system status bar and navigation bar will be hidden).
     *
     * @return {@code true} if fullscreen mode can be entered, {@code false} otherwise.
     */
    public boolean onEnterFullscreenMode() {
        return true;
    }

    /**
     * Called before the containing {@link PdfViewCtrlTabHostFragment host fragment} exits
     * fullscreen mode (system status bar and/or navigation bar will be shown).
     *
     * @return {@code true} if fullscreen mode can be exited, {@code false} otherwise.
     */
    public boolean onExitFullscreenMode() {
        return true;
    }

    // TODO: Tackle this beast...
    public void setVisibilityOfImaginedToolbar(boolean visible) {
        if (mPdfViewCtrl == null) {
            return;
        }

        View toolbar = mAnnotationToolbar;
        if (toolbar == null) {
            return;
        }

        // calculate new scroll position and how much we need to translate the PDFViewCtrl to make
        // the content appear at the same place
        int translateOffset;
        int canvasHeight = mPdfViewCtrl.getViewCanvasHeight();
        int viewHeight = mPdfViewCtrl.getHeight();
        int scrollY = mPdfViewCtrl.getScrollY();
        mPdfViewCtrl.setPageViewMode(PDFViewCtrl.PageViewMode.ZOOM); // so it doesn't re-fit
        int toolbarHeight = toolbar.getHeight();
        if (visible) {
            int newViewHeight = viewHeight - toolbarHeight;

            // need to know how tall the content after resizing
            int[] offsets = new int[2];
            if (canvasHeight > viewHeight) {
                offsets[1] = canvasHeight;
            } else {
                mPdfViewCtrl.getContentSize(offsets);
            }
            int newScrollableHeight = Math.max(offsets[1] - newViewHeight, 0);

            int newScrollY = Math.min(newScrollableHeight, scrollY + toolbarHeight);
            translateOffset = (toolbarHeight - newScrollY + scrollY) / 2;
            mPdfViewCtrl.setNextOnLayoutAdjustments(0, newScrollY - scrollY, true);
            if (translateOffset > 0) {
                mPdfViewCtrl.setTranslationY(-translateOffset);
                ViewPropertyAnimator ani = mPdfViewCtrl.animate();
                ani.translationY(0);
                ani.setDuration(300);
                ani.start();
            }
        } else {
            int newViewHeight = viewHeight + toolbarHeight;
            int newGraySpace = Math.max(newViewHeight - canvasHeight, 0);

            // if we are at the bottom of page and re-size, the PDFViewCtrl will do that
            // automatically since it's at the bottom
            int scrollYHandledByPdfViewCtrl = 0;
            if (canvasHeight > viewHeight) {
                int distanceFromBottom = canvasHeight - (viewHeight + scrollY);
                scrollYHandledByPdfViewCtrl = Math.max(0, toolbarHeight - distanceFromBottom);
            }

            int newScrollY = Math.max(scrollY - toolbarHeight, 0);

            int graySpaceOffset = newGraySpace / 2;
            translateOffset = (toolbarHeight - scrollY + newScrollY) - graySpaceOffset;
            mPdfViewCtrl.setNextOnLayoutAdjustments(0, newScrollY - scrollY + scrollYHandledByPdfViewCtrl, true);
            if (translateOffset > 0) {
                mPdfViewCtrl.setTranslationY(translateOffset);
                ViewPropertyAnimator ani = mPdfViewCtrl.animate();
                ani.translationY(0);
                ani.setDuration(300);
                ani.start();
            }
        }
    }

    public void setViewerTopMargin(int height) {
        if (mPdfViewCtrl == null || !(mPdfViewCtrl.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mPdfViewCtrl.getLayoutParams();
        params.topMargin = height;
        mPdfViewCtrl.setLayoutParams(params);
        mPdfViewCtrl.requestLayout();
    }

    /**
     * Sets the visibility of thumbnail slider.
     *
     * @param visible            True if the thumbnail slider should be visible
     * @param animateThumbSlider True if the visibility should be changed with animation
     */
    public void setThumbSliderVisible(boolean visible, boolean animateThumbSlider) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mBottomNavBar == null) {
            return;
        }

        boolean isVisible = mBottomNavBar.getVisibility() == View.VISIBLE;

        if (visible) {
            if (!isVisible) {
                if (mViewerConfig == null || mViewerConfig.isShowBottomNavBar()) {
                    mBottomNavBar.show(animateThumbSlider);
                }

                // show page back and forward buttons if their stacks are not empty
                if (mPageBackButton != null && !mPageBackStack.isEmpty()) {
                    mPageBackButton.setVisibility(View.VISIBLE);
                    mPageBackButton.setEnabled(true);
                }
                if (mPageForwardButton != null && !mPageForwardStack.isEmpty()) {
                    mPageForwardButton.setVisibility(View.VISIBLE);
                    mPageForwardButton.setEnabled(true);
                }

            }
        } else {
            if (isVisible) {
                mBottomNavBar.dismiss(animateThumbSlider);
            }
        }
    }

    public boolean isThumbSliderVisible() {
        return mBottomNavBar != null && mBottomNavBar.getVisibility() == View.VISIBLE;
    }

    /**
     * Handles the activity result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCode.PRINT && resultCode != Activity.RESULT_CANCELED) {
            boolean documentChecked = data.getBooleanExtra(
                PrintAnnotationsSummaryDialogFragment.EXTRA_PRINT_DOCUMENT_CHECKED, mPrintDocumentChecked);
            boolean annotationsChecked = data.getBooleanExtra(
                PrintAnnotationsSummaryDialogFragment.EXTRA_PRINT_ANNOTATIONS_CHECKED, mPrintAnnotationsChecked);
            boolean summaryChecked = data.getBooleanExtra(
                PrintAnnotationsSummaryDialogFragment.EXTRA_PRINT_SUMMARY_CHECKED, mPrintSummaryChecked);

            updatePrintDocumentMode(documentChecked);
            updatePrintAnnotationsMode(annotationsChecked);
            updatePrintSummaryMode(summaryChecked);

            sentToPrinterDialog();
        }
        if (Activity.RESULT_OK == resultCode) {
            if (requestCode == RequestCode.PICK_PHOTO_CAM) {
                // save the data and process the image stamp
                // after onResume is called.
                mImageStampDelayCreation = true;
                mAnnotIntentData = data;
                if (canResumeWithoutReloading()) {
                    mImageStampDelayCreation = false;
                    ViewerUtils.createImageStamp(getActivity(), mAnnotIntentData, mPdfViewCtrl, mOutputFileUri, mAnnotTargetPoint);
                }
            } else if (requestCode == RequestCode.SELECT_FILE) {
                // save the data and process the file attachment
                // after onResume is called.
                mFileAttachmentDelayCreation = true;
                mAnnotIntentData = data;
                if (canResumeWithoutReloading()) {
                    mFileAttachmentDelayCreation = false;
                    ViewerUtils.createFileAttachment(getActivity(), mAnnotIntentData, mPdfViewCtrl, mAnnotTargetPoint);
                }
            }
        } else {
            if (mToolManager != null && mToolManager.getTool() != null) {
                ((Tool) mToolManager.getTool()).clearTargetPoint();
            }
        }
    }

    /**
     * Updates documentation mode in print.
     *
     * @param enabled True if the documentation mode is checked
     */
    public void updatePrintDocumentMode(boolean enabled) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        mPrintDocumentChecked = enabled;
        PdfViewCtrlSettingsManager.setPrintDocumentMode(activity, mPrintDocumentChecked);
    }

    /**
     * Updates annotation mode in print.
     *
     * @param enabled True if the annotation mode is checked
     */
    public void updatePrintAnnotationsMode(boolean enabled) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        mPrintAnnotationsChecked = enabled;
        PdfViewCtrlSettingsManager.setPrintAnnotationsMode(activity, mPrintAnnotationsChecked);
    }

    /**
     * Updates summery mode in print.
     *
     * @param enabled True if the summery mode is checked
     */
    public void updatePrintSummaryMode(boolean enabled) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        mPrintSummaryChecked = enabled;
        PdfViewCtrlSettingsManager.setPrintSummaryMode(activity, mPrintSummaryChecked);
    }

    /**
     * Updates the recent list.
     */
    public void updateRecentList() {
        if (!isDocumentReady()) {
            return;
        }

        updateRecentFile(getCurrentFileInfo());
    }

    /**
     * Updates the recent file.
     *
     * @param fileInfo The {@link FileInfo}
     */
    protected void updateRecentFile(FileInfo fileInfo) {
        if (mPdfViewCtrl == null) {
            return;
        }

        if (fileInfo != null) {
            fileInfo.setHScrollPos(mPdfViewCtrl.getHScrollPos());
            fileInfo.setVScrollPos(mPdfViewCtrl.getVScrollPos());
            fileInfo.setZoom(mPdfViewCtrl.getZoom());
            fileInfo.setLastPage(mPdfViewCtrl.getCurrentPage());
            fileInfo.setPageRotation(mPdfViewCtrl.getPageRotation());
            fileInfo.setPagePresentationMode(mPdfViewCtrl.getPagePresentationMode());
            fileInfo.setReflowMode(mIsReflowMode);
            if (mReflowControl != null && mReflowControl.isReady()) {
                try {
                    int reflowTextSize = mReflowControl.getTextSizeInPercent();
                    fileInfo.setReflowTextSize(reflowTextSize);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
            fileInfo.setRtlMode(mIsRtlMode);
            fileInfo.setBookmarkDialogCurrentTab(mBookmarkDialogCurrentTab);
            updateRecentFilesManager(fileInfo);
        }
    }

    private void updateRecentFilesManager(FileInfo fileInfo) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        getRecentFilesManager().updateFile(activity, fileInfo);
    }

    protected FileInfoManager getRecentFilesManager() {
        return RecentFilesManager.getInstance();
    }

    /**
     * Removes the current file from the recent list.
     */
    public void removeFromRecentList() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        switch (mTabSource) {
            case BaseFileInfo.FILE_TYPE_FILE:
                if (mCurrentFile != null) {
                    RecentFilesManager.getInstance().removeFile(activity,
                        new FileInfo(BaseFileInfo.FILE_TYPE_FILE, mCurrentFile, mIsEncrypted, 1));
                }
                break;
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
            case BaseFileInfo.FILE_TYPE_EDIT_URI:
            case BaseFileInfo.FILE_TYPE_OFFICE_URI:
                RecentFilesManager.getInstance().removeFile(activity,
                    new FileInfo(mTabSource, mTabTag, mTabTitle, mIsEncrypted, 1));
                break;
        }
    }

    protected boolean containsInRecentList(FileInfo fileInfo) {
        Activity activity = getActivity();
        return activity != null && RecentFilesManager.getInstance().containsFile(activity, fileInfo);
    }

    /**
     * Adds a new tab info to the recent list.
     *
     * @param tabInfo The {@link PdfViewCtrlTabInfo}
     */
    protected void addToRecentList(PdfViewCtrlTabInfo tabInfo) {
        if (tabInfo == null) {
            return;
        }

        FileInfo fileInfo = null;
        try {
            switch (tabInfo.tabSource) {
                case BaseFileInfo.FILE_TYPE_FILE:
                case BaseFileInfo.FILE_TYPE_OPEN_URL:
                    if (mCurrentFile != null) {
                        fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, mCurrentFile, mIsEncrypted, 1);
                    }
                    break;
                case BaseFileInfo.FILE_TYPE_EXTERNAL:
                case BaseFileInfo.FILE_TYPE_OFFICE_URI:
                    fileInfo = new FileInfo(tabInfo.tabSource, mTabTag, mTabTitle, mIsEncrypted, 1);
                    break;
                case BaseFileInfo.FILE_TYPE_EDIT_URI:
                    if (mCurrentFile != null) {
                        fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EDIT_URI, mTabTag, mTabTitle, mIsEncrypted, 1);
                    }
                    break;
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        if (fileInfo != null) {
            addRecentFile(tabInfo, fileInfo);
        }
    }

    protected void addRecentFile(@NonNull PdfViewCtrlTabInfo tabInfo, @Nullable FileInfo fileInfo) {
        if (fileInfo != null) {
            fileInfo.setLastPage(tabInfo.lastPage);
            fileInfo.setPageRotation(tabInfo.pageRotation);
            fileInfo.setPagePresentationMode(tabInfo.getPagePresentationMode());
            fileInfo.setHScrollPos(tabInfo.hScrollPos);
            fileInfo.setVScrollPos(tabInfo.vScrollPos);
            fileInfo.setZoom(tabInfo.zoom);
            fileInfo.setReflowMode(tabInfo.isReflowMode);
            fileInfo.setReflowTextSize(tabInfo.reflowTextSize);
            fileInfo.setRtlMode(tabInfo.isRtlMode);
            fileInfo.setBookmarkDialogCurrentTab(tabInfo.bookmarkDialogCurrentTab);

            addToRecentFilesManager(fileInfo);
        }
    }

    protected void addToRecentFilesManager(FileInfo fileInfo) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        RecentFilesManager.getInstance().addFile(activity, fileInfo);
    }

    public long getCurrentFileSize() {
        try {
            if (mCurrentFile != null) {
                return mCurrentFile.length();
            } else if (mCurrentUriFile != null) {
                ExternalFileInfo externalFileInfo = Utils.buildExternalFile(getContext(), mCurrentUriFile);
                if (externalFileInfo != null) {
                    return externalFileInfo.getSize();
                }
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
        return -1;
    }

    /**
     * Returns the current file info.
     *
     * @return The {@link FileInfo}
     */
    public FileInfo getCurrentFileInfo() {
        FileInfo fileInfo = null;
        switch (mTabSource) {
            case BaseFileInfo.FILE_TYPE_FILE:
            case BaseFileInfo.FILE_TYPE_OPEN_URL:
                if (mCurrentFile != null) {
                    fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_FILE, mCurrentFile, mIsEncrypted, 1);
                }
                break;
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
            case BaseFileInfo.FILE_TYPE_OFFICE_URI:
                fileInfo = new FileInfo(mTabSource, mTabTag, mTabTitle, mIsEncrypted, 1);
                break;
            case BaseFileInfo.FILE_TYPE_EDIT_URI:
                if (mCurrentFile != null) {
                    fileInfo = new FileInfo(BaseFileInfo.FILE_TYPE_EDIT_URI, mTabTag, mTabTitle, mIsEncrypted, 1);
                }
                break;
        }
        return fileInfo;
    }

    protected PdfViewCtrlTabInfo getCurrentTabInfo(Activity activity) {
        PdfViewCtrlTabInfo info = new PdfViewCtrlTabInfo();
        info.tabTitle = mTabTitle;
        info.tabSource = mTabSource;
        info.fileExtension = mFileExtension;
        if (activity != null) {
            // restore last selected view mode
            String mode = PdfViewCtrlSettingsManager.getViewMode(activity);
            info.pagePresentationMode = getPagePresentationModeFromSettings(mode).getValue();
        }

        return info;
    }

    /**
     * Sets the visibility of search navigation buttons.
     *
     * @param visible True if visible
     */
    public void setSearchNavButtonsVisible(boolean visible) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        int visibility = visible ? View.VISIBLE : View.GONE;
        mSearchOverlay.setVisibility(visibility);
    }

    /**
     * Hides backward/forward buttons.
     */
    public void hideBackAndForwardButtons() {
        if (mPageBackButton != null) {
            mPageBackButton.setVisibility(View.INVISIBLE);
        }
        if (mPageForwardButton != null) {
            mPageForwardButton.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Highlights the results of full text search.
     *
     * @param result The {@link com.pdftron.pdf.TextSearchResult}
     */
    public void highlightFullTextSearchResult(com.pdftron.pdf.TextSearchResult result) {
        if (mSearchOverlay != null) {
            mSearchOverlay.highlightFullTextSearchResult(result);
        }
    }

    protected void openFileInNewTab(File file) {
        openFileInNewTab(file, mPassword);
    }

    protected void openFileUriInNewTab(Uri fileUri) {
        openFileUriInNewTab(fileUri, mPassword);
    }

    protected void openFileInNewTab(File file, String password) {
        if (mPdfViewCtrl == null) {
            return;
        }

        if (file == null) {
            handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_NULL_PDFDOC);
            return;
        }
        if (!file.exists()) {
            handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_NOT_EXIST);
            return;
        }
        if (mTabListener != null) {
            mPdfViewCtrl.closeTool();
            mTabListener.onOpenAddNewTab(BaseFileInfo.FILE_TYPE_FILE, file.getAbsolutePath(), file.getName(), password);
        }
    }

    private void openFileUriInNewTab(Uri fileUri, String password) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return;
        }

        if (fileUri == null) {
            handleOpeningDocumentFailed(PdfDocManager.DOCUMENT_SETDOC_ERROR_NULL_PDFDOC);
            return;
        }
        if (mTabListener != null) {
            ExternalFileInfo info = Utils.buildExternalFile(activity, fileUri);
            if (info != null) {
                mPdfViewCtrl.closeTool();
                mTabListener.onOpenAddNewTab(BaseFileInfo.FILE_TYPE_EXTERNAL, fileUri.toString(), info.getFileName(), password);
            }
        }
    }

    protected boolean canResumeWithoutReloading() {
        Activity activity = getActivity();
        if (activity == null || mPdfDoc == null) {
            return false;
        }

        // PDFDoc was previously initialized
        // let's ensure file source is still valid
        switch (mTabSource) {
            case BaseFileInfo.FILE_TYPE_FILE:
            case BaseFileInfo.FILE_TYPE_OPEN_URL:
            case BaseFileInfo.FILE_TYPE_EDIT_URI:
                // check if local file still valid
                if (null == mCurrentFile || !mCurrentFile.exists()) {
                    return false;
                }
                // if universal conversion cancelled due to fragment pause/hidden change
                // then need to open it again
                return !(mTabSource == BaseFileInfo.FILE_TYPE_FILE
                    && Utils.isNotPdf(mTabTag) && !mIsOfficeDocReady);
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
                // check if sd card file still valid
                if (mCurrentUriFile == null) {
                    return false;
                }
                ExternalFileInfo externalFileInfo = Utils.buildExternalFile(getContext(), mCurrentUriFile);
                ContentResolver contentResolver = Utils.getContentResolver(activity);
                return contentResolver != null && !(externalFileInfo == null || !externalFileInfo.exists())
                    && !Utils.isNotPdf(contentResolver, Uri.parse(mTabTag));
        }

        return false;
    }

    protected void resumeFragment(boolean fromOnResume) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return;
        }
        if (sDebug)
            Log.d("timing", "resumeFragment start");

        mPdfViewCtrl.resume();
        mHasChangesSinceResumed = false;

        if (mCanAddToTabInfo) {
            PdfViewCtrlTabInfo tabInfo = PdfViewCtrlTabsManager.getInstance().getPdfFViewCtrlTabInfo(activity, mTabTag);
            if (tabInfo != null) {
                // update last viewed timestamp
                PdfViewCtrlTabsManager.getInstance().updateLastViewedTabTimestamp(activity, mTabTag);

                // update recent list
                addToRecentList(tabInfo);
            } else if (PdfViewCtrlTabsManager.getInstance().getNewPath(mTabTag) == null
                && !containsInRecentList(getCurrentFileInfo())) {
                // add to recent list if not opened before
                addToRecentList(getCurrentTabInfo(activity));
            }
        }

        toggleViewerVisibility(false);

        if (null != mToolManager && mToolManager.getTool() instanceof TextHighlighter) {
            highlightSearchResults();
        }

        if (!mDocumentLoading) {
            boolean handled = true;
            mDocumentLoading = true;
            if (mPdfDoc != null) {
                // PDFDoc was previously initialized
                // let's ensure file source is still valid
                boolean fileNotExists = false;
                switch (mTabSource) {
                    case BaseFileInfo.FILE_TYPE_FILE:
                    case BaseFileInfo.FILE_TYPE_OPEN_URL:
                    case BaseFileInfo.FILE_TYPE_EDIT_URI:
                        // check if local file still valid
                        if (mCurrentFile == null) {
                            fileNotExists = true;
                        } else if (!mCurrentFile.exists()) {
                            String path = PdfViewCtrlTabsManager.getInstance().getNewPath(mTabTag);
                            if (!Utils.isNullOrEmpty(path) && new File(path).exists()) {
                                String oldTabTag = mTabTag;
                                mPdfDoc = null;
                                mTabTag = path;
                                mTabTitle = FilenameUtils.removeExtension(new File(path).getName());
                                mCurrentFile = new File(mTabTag);
                                if (mTabListener != null) {
                                    mTabListener.onTabIdentityChanged(oldTabTag, mTabTag, mTabTitle, mFileExtension, mTabSource);
                                }
                            } else {
                                fileNotExists = true;
                            }
                        } else {
                            if (!mIsOfficeDocReady
                                && (mTabSource == BaseFileInfo.FILE_TYPE_FILE && Utils.isNotPdf(mTabTag))) {
                                // if universal conversion cancelled due to fragment pause/hidden change
                                // then need to open it again
                                openOfficeDoc(mTabTag, false);
                            } else {
                                // doc is loaded properly
                                toggleViewerVisibility(true);
                                mDocumentLoading = false;
                            }
                        }
                        break;
                    case BaseFileInfo.FILE_TYPE_OFFICE_URI:
                        openOfficeDoc(mTabTag, true);
                        break;
                    case BaseFileInfo.FILE_TYPE_EXTERNAL:
                        // check if sd card file still valid
                        if (mCurrentUriFile == null) {
                            mErrorCode = PdfDocManager.DOCUMENT_SETDOC_ERROR_NOT_EXIST;
                            handleOpeningDocumentFailed(mErrorCode);
                        } else {
                            ExternalFileInfo externalFileInfo = Utils.buildExternalFile(getContext(), mCurrentUriFile);
                            if (externalFileInfo == null || !externalFileInfo.exists()) {
                                String path = PdfViewCtrlTabsManager.getInstance().getNewPath(mTabTag);
                                if (path == null) {
                                    fileNotExists = true;
                                } else {
                                    externalFileInfo = Utils.buildExternalFile(getContext(), Uri.parse(path));
                                    if (externalFileInfo == null || !externalFileInfo.exists()) {
                                        fileNotExists = true;
                                    } else {
                                        String oldTabTag = mTabTag;
                                        mPdfDoc = null;
                                        mTabTag = path;
                                        mTabTitle = FilenameUtils.removeExtension(externalFileInfo.getName());
                                        mCurrentUriFile = Uri.parse(mTabTag);
                                        if (mTabListener != null) {
                                            mTabListener.onTabIdentityChanged(oldTabTag, mTabTag, mTabTitle, mFileExtension, mTabSource);
                                        }
                                    }
                                }
                            } else {
                                ContentResolver contentResolver = Utils.getContentResolver(activity);
                                if (contentResolver == null) {
                                    handled = false;
                                    break;
                                }
                                if (Utils.isNotPdf(contentResolver, Uri.parse(mTabTag))) {
                                    openOfficeDoc(mTabTag, true);
                                } else {
                                    // doc is loaded properly
                                    toggleViewerVisibility(true);
                                    mDocumentLoading = false;
                                }
                            }
                        }
                        break;
                    default:
                        handled = false;
                }
                if (fileNotExists) {
                    // something went wrong, report error
                    mErrorCode = PdfDocManager.DOCUMENT_SETDOC_ERROR_NOT_EXIST;
                    handleOpeningDocumentFailed(mErrorCode);
                }
            }

            if (mPdfDoc == null) {
                // open when onResume
                // close when onPause
                switch (mTabSource) {
                    case BaseFileInfo.FILE_TYPE_FILE:
                        if (Utils.isNotPdf(mTabTag)) {
                            openOfficeDoc(mTabTag, false);
                        } else {
                            openLocalFile(mTabTag);
                        }
                        break;
                    case BaseFileInfo.FILE_TYPE_EXTERNAL:
                        ContentResolver contentResolver = Utils.getContentResolver(activity);
                        if (contentResolver == null) {
                            handled = false;
                            break;
                        }
                        if (Utils.isNotPdf(contentResolver, Uri.parse(mTabTag))) {
                            openOfficeDoc(mTabTag, true);
                        } else {
                            openExternalFile(mTabTag);
                        }
                        break;
                    case BaseFileInfo.FILE_TYPE_OPEN_URL:
                        openUrlFile(mTabTag);
                        break;
                    case BaseFileInfo.FILE_TYPE_EDIT_URI:
                        openEditUriFile(mTabTag);
                        break;
                    case BaseFileInfo.FILE_TYPE_OFFICE_URI:
                        openOfficeDoc(mTabTag, true);
                        break;
                    default:
                        handled = false;
                }
            }
            if (!handled) {
                mDocumentLoading = false;
            }
        }

        if (mColorModeChanged) {
            mColorModeChanged = false;
            updateColorMode();
        }
        if (sDebug)
            Log.d("timing", "resumeFragment end");
    }

    protected void pauseFragment() {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null) {
            return;
        }

        stopHandlers();

        if (mDocumentConversion != null) {
            cancelUniversalConversion();
        }

        updateRecentList();

        // save encrypted password
        if (mPassword != null && !mPassword.isEmpty()) {
            PdfViewCtrlTabInfo info = PdfViewCtrlTabsManager.getInstance().getPdfFViewCtrlTabInfo(activity, mTabTag);
            if (info != null) {
                info.password = Utils.encryptIt(activity, mPassword);
                PdfViewCtrlTabsManager.getInstance().addPdfViewCtrlTabInfo(activity, mTabTag, info);
            }
        }

        if (mDownloadDocumentDialog != null && mDownloadDocumentDialog.isShowing()) {
            mDownloadDocumentDialog.dismiss();
        }

        if (mGetTextInPageTask != null && mGetTextInPageTask.getStatus() != AsyncTask.Status.FINISHED) {
            mGetTextInPageTask.cancel(true);
            mGetTextInPageTask = null;
        }

        if (mPDFDocLoaderTask != null && mPDFDocLoaderTask.getStatus() != AsyncTask.Status.FINISHED) {
            mPDFDocLoaderTask.cancel(true);
            mPDFDocLoaderTask = null;
        }

        // always force to save when switching tabs
        // since we cancel rendering onPause, this should be quick enough to obtain a write lock
        showDocumentSavedToast();
        save(false, true, true, true); // skip showing the message as it is confusing as the current tab is going away

        saveCurrentPdfViewCtrlState();

        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.pause();
            mPdfViewCtrl.purgeMemory();
        }

        closeKeyboard();
        mDocumentLoading = false;

        if (mTabListener != null) {
            mTabListener.onTabPaused(getCurrentFileInfo(), isDocModifiedAfterOpening());
        }
    }

    protected void closeKeyboard() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (mPasswordLayout != null && mPasswordLayout.getVisibility() == View.VISIBLE) {
            Utils.hideSoftKeyboard(activity, mPasswordLayout);
        }
    }

    protected void toggleViewerVisibility(boolean visible) {
        if (mIsReflowMode) {
            return;
        }
        if (mViewerHost != null) {
            if (visible) {
                mViewerHost.setVisibility(View.VISIBLE);
                if (sDebug)
                    Log.d(TAG, "show viewer");
            } else {
                mViewerHost.setVisibility(View.INVISIBLE);
                if (sDebug)
                    Log.d(TAG, "hide viewer");
            }
        }
        if (mProgressBarLayout != null) {
            if (visible) {
                mProgressBarLayout.hide(false);
                if (sDebug)
                    Log.d(TAG, "hide progress bar");
            } else {
                mProgressBarLayout.show();
                if (sDebug)
                    Log.d(TAG, "show progress bar");
            }
        }
    }

//    private void setToolManagerStatusBarHeight() {
//        Activity activity = getActivity();
//        if (activity == null) {
//            return;
//        }
//
//        try {
//            activity.getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//                @SuppressWarnings("deprecation")
//                @Override
//                public void onGlobalLayout() {
//                    Activity activity = getActivity();
//                    if (activity == null) {
//                        return;
//                    }
//                    //noinspection deprecation
//                    try {
//                        activity.getWindow().getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//
//                    if (mToolManager != null) {
//                        mToolManager.setStatusBarHeight(Utils.getStatusBarHeight(activity));
//                    }
//                }
//            });
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void createRetrieveChangesDialog(final String cacheFileName) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.freetext_restore_cache_message)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Context context = getContext();
                    if (context == null) {
                        return;
                    }
                    JSONObject obj = Utils.retrieveToolCache(context, cacheFileName);
                    if (null == obj || mPdfViewCtrl == null) {
                        return;
                    }

                    mToolManager.setTool(mToolManager.createTool(ToolMode.TEXT_CREATE, null));
                    try {
                        int page = obj.getInt(FreeTextCacheStruct.PAGE_NUM);
                        if (mPdfViewCtrl.getCurrentPage() != page) {
                            if (sDebug)
                                Log.d(TAG, "restoreFreeText mWaitingForSetPage: " + page);
                            mPdfViewCtrl.setCurrentPage(page);
                            mWaitingForSetPage = true;
                            mWaitingForSetPageNum = page;
                        } else {
                            restoreFreeText();
                        }
                    } catch (JSONException e) {
                        AnalyticsHandlerAdapter.getInstance().sendException(e);
                    }
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Context context = getContext();
                    if (context == null) {
                        return;
                    }
                    if (sDebug)
                        Log.d(TAG, "cancel");
                    Utils.deleteCacheFile(context, cacheFileName);
                }
            });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void restoreFreeText() {
        if (mPdfViewCtrl == null || mToolManager == null) {
            return;
        }

        mToolManager.setTool(mToolManager.createTool(ToolMode.TEXT_CREATE, null));
        String freeTextCacheFilename = mToolManager.getFreeTextCacheFileName();
        JSONObject obj = Utils.retrieveToolCache(getContext(), freeTextCacheFilename);
        if (obj != null) {
            try {
                JSONObject pointObj = obj.getJSONObject(FreeTextCacheStruct.TARGET_POINT);
                int x = pointObj.getInt(FreeTextCacheStruct.X);
                int y = pointObj.getInt(FreeTextCacheStruct.Y);
                MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, x, y, 0);
                mPdfViewCtrl.dispatchTouchEvent(event);
                event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0);
                mPdfViewCtrl.dispatchTouchEvent(event);
            } catch (JSONException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    protected void hidePageNumberIndicator() {
        mPageNumberIndicator.setVisibility(View.GONE);
        mPageBackButton.setVisibility(View.INVISIBLE);
        mPageForwardButton.setVisibility(View.INVISIBLE);
    }

    /**
     * Sets if the file an be added to tab info.
     *
     * @param enabled True if enabled
     */
    @SuppressWarnings("SameParameterValue")
    public void setCanAddToTabInfo(boolean enabled) {
        mCanAddToTabInfo = enabled;
    }

    /**
     * Returns the path of the document
     *
     * @return the path of the document
     */
    public String getFilePath() {
        if (mCurrentFile != null) {
            return mCurrentFile.getAbsolutePath();
        }
        if (mCurrentUriFile != null) {
            return mCurrentUriFile.getPath();
        }
        return null;
    }

    /**
     * Checks whether the document modified after opening.
     *
     * @return true if the document is modified after opening; false otherwise
     */
    public boolean isDocModifiedAfterOpening() {
        return Utils.isDocModified(mPdfDoc) || mDocumentState == PdfDocManager.DOCUMENT_STATE_NORMAL;
    }

    /**
     * Sets search mode
     *
     * @param enabled true if search mode is enabled; false otherwise.
     */
    public void setSearchMode(boolean enabled) {
        mInSearchMode = enabled;
    }

    /**
     * Checks whether it is in search mode.
     *
     * @return true if in search mode; false otherwise
     */
    public boolean isSearchMode() {
        return mInSearchMode;
    }

    /**
     * Undo the last modification.
     */
    protected void undo() {
        undo(true);
    }

    /**
     * Undo the last modification.
     *
     * @param sendAnalytics Whether it sends data to analytics
     */
    protected void undo(boolean sendAnalytics) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null || mToolManager == null) {
            return;
        }

        UndoRedoManager undoRedoManager = mToolManager.getUndoRedoManger();
        if (undoRedoManager != null && undoRedoManager.canUndo()) {
            String undoInfo = undoRedoManager.undo(AnalyticsHandlerAdapter.LOCATION_VIEWER, sendAnalytics);
            UndoRedoManager.jumpToUndoRedo(mPdfViewCtrl, undoInfo, true);
            refreshPageCount();
        }
    }

    /**
     * Redo the last undo operation.
     */
    protected void redo() {
        redo(true);
    }

    /**
     * Redo the last undo operation.
     *
     * @param sendAnalytics Whether it sends data to analytics
     */
    protected void redo(boolean sendAnalytics) {
        Activity activity = getActivity();
        if (activity == null || mPdfViewCtrl == null || mToolManager == null) {
            return;
        }

        UndoRedoManager undoRedoManager = mToolManager.getUndoRedoManger();
        if (undoRedoManager != null && undoRedoManager.canRedo()) {
            String redoInfo = undoRedoManager.redo(AnalyticsHandlerAdapter.LOCATION_VIEWER, sendAnalytics);
            UndoRedoManager.jumpToUndoRedo(mPdfViewCtrl, redoInfo, false);
            refreshPageCount();
        }
    }

    /**
     * Confirms that document saved toast has been shown.
     */
    public void setSavedAndClosedShown() {
        mWasSavedAndClosedShown = true;
    }

    /**
     * Sets the which tab in bookmarks dialog should be selected.
     *
     * @param index The index of tab
     */
    public void setBookmarkDialogCurrentTab(int index) {
        mBookmarkDialogCurrentTab = index;
    }

    /**
     * Returns which tab in bookmark dialog is selected.
     *
     * @return The index of tab
     */
    public int getBookmarkDialogCurrentTab() {
        return mBookmarkDialogCurrentTab;
    }

    /**
     * Checks tab conversion and shows the alert.
     *
     * @param messageID            The message ID
     * @param allowConverted       True if conversion is allowed
     * @param skipSpecialFileCheck True if spcecial files should be skipped
     * @return True if handled
     */
    protected boolean checkTabConversionAndAlert(int messageID, boolean allowConverted, boolean skipSpecialFileCheck) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        localFileWriteAccessCheck();
        if (isTabReadOnly()) {
            if (canDocBeSaved() && allowConverted)
                return false;

            if (canDocBeSaved()) {
                if (!isNotPdf() && skipSpecialFileCheck) {
                    return false;
                } else {
                    handleSpecialFile();
                }
            } else {
                if (getHasWarnedAboutCanNotEditDuringConversion()) {
                    CommonToast.showText(activity, messageID);
                } else {
                    setHasWarnedAboutCanNotEditDuringConversion();
                    Utils.getAlertDialogNoTitleBuilder(activity, messageID)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setCancelable(false)
                        .create().show();
                }
            }

            return true;
        }

        return false;
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }
}
