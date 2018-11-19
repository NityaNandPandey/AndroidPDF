package com.pdftron.pdf.controls;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.tools.UndoRedoManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.ToolbarActionMode;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.ItemSelectionHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.paulburke.android.itemtouchhelperdemo.helper.SimpleItemTouchHelperCallback;

/**
 * The ThumbnailsViewFragment uses the {@link com.pdftron.pdf.PDFViewCtrl#getThumbAsync(int)}
 * to show thumbnails of the documents as a grid view. It supports add/remove/re-arrange pages,
 * as well as rotate/duplicate and export pages. Undo/Redo is also supported.
 */
public class ThumbnailsViewFragment extends DialogFragment implements ThumbnailsViewAdapter.EditPagesListener {

    private static final String BUNDLE_READ_ONLY_DOC = "read_only_doc";
    private static final String BUNDLE_EDIT_MODE = "edit_mode";
    private static final String BUNDLE_OUTPUT_FILE_URI = "output_file_uri";


    FloatingActionMenu mFabMenu;
    private Uri mOutputFileUri;
    private boolean mIsReadOnly;

    private Integer mInitSelectedItem;

    private PDFViewCtrl mPdfViewCtrl;

    private Toolbar mToolbar;
    private Toolbar mCabToolbar;
    private SimpleRecyclerView mRecyclerView;
    private ThumbnailsViewAdapter mAdapter;

    private ItemSelectionHelper mItemSelectionHelper;
    private ItemTouchHelper mItemTouchHelper;
    private ToolbarActionMode mActionMode;

    private MenuItem mMenuItemUndo;
    private MenuItem mMenuItemRedo;
    private MenuItem mMenuItemRotate;
    private MenuItem mMenuItemDelete;
    private MenuItem mMenuItemDuplicate;
    private MenuItem mMenuItemExport;

    private int mSpanCount;
    private String mTitle = "";
    private boolean mHasEventAction;

    boolean mAddDocPagesDelay;
    int mPositionDelay;
    ThumbnailsViewAdapter.DocumentFormat mDocumentFormatDelay;
    Object mDataDelay;

    private OnThumbnailsViewDialogDismissListener mOnThumbnailsViewDialogDismissListener;
    private OnThumbnailsEditAttemptWhileReadOnlyListener mOnThumbnailsEditAttemptWhileReadOnlyListener;
    private OnExportThumbnailsListener mOnExportThumbnailsListener;

    /**
     * Returns a new instance of the class
     */
    public static ThumbnailsViewFragment newInstance() {
        return newInstance(false);
    }

    /**
     * Returns a new instance of the class
     */
    public static ThumbnailsViewFragment newInstance(boolean readOnly) {
        return newInstance(readOnly, false);
    }

    /**
     * Returns a new instance of the class
     */
    public static ThumbnailsViewFragment newInstance(boolean readOnly, boolean editMode) {
        ThumbnailsViewFragment fragment = new ThumbnailsViewFragment();
        Bundle args = new Bundle();
        args.putBoolean(BUNDLE_READ_ONLY_DOC, readOnly);
        args.putBoolean(BUNDLE_EDIT_MODE, editMode);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * The overload implementation of {@link DialogFragment#onCreate(Bundle)}.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mOutputFileUri = savedInstanceState.getParcelable(BUNDLE_OUTPUT_FILE_URI);
        }
    }

    /**
     * The overload implementation of {@link DialogFragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.controls_fragment_thumbnails_view, container, false);
    }

    /**
     * The overload implementation of {@link DialogFragment#onViewCreated(View, Bundle)}.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (mPdfViewCtrl == null) {
            return;
        }

        List<Map<String, Object>> dataList = new ArrayList<>();
        if (Utils.isNullOrEmpty(mTitle)) {
            mTitle = getString(R.string.controls_thumbnails_view_description);
        }

        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            int pageCount = mPdfViewCtrl.getDoc().getPageCount();
            for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put(ThumbnailsViewAdapter.PAGE_NUMBER_SRC, pageNum);
                itemMap.put(ThumbnailsViewAdapter.THUMB_IMAGE, null);
                dataList.add(itemMap);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        int viewWidth = getDisplayWidth();
        int thumbSize = getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_image_width);
        int thumbSpacing = getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_grid_spacing);
        // Calculate number of columns
        mSpanCount = (int) Math.floor(viewWidth / (thumbSize + thumbSpacing));

        mToolbar = view.findViewById(R.id.controls_thumbnails_view_toolbar);
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);

        mCabToolbar = view.findViewById(R.id.controls_thumbnails_view_cab);
        mCabToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!onBackPressed()) {
                    dismiss();
                }
            }
        });

        if (getArguments() != null) {
            Bundle args = getArguments();
            mIsReadOnly = args.getBoolean(BUNDLE_READ_ONLY_DOC, false);
        }

        mToolbar.inflateMenu(R.menu.controls_fragment_edit_toolbar);
        MenuItem menuEdit = mToolbar.getMenu().findItem(R.id.controls_action_edit);
        if (menuEdit != null) {
            menuEdit.setVisible(!mIsReadOnly);
        }
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.controls_action_edit) {
                    // Start edit-mode
                    if (mCabToolbar != null && mCabToolbar.getNavigationIcon() != null) {
                        mCabToolbar.getNavigationIcon().mutate().setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
                    }
                    mActionMode = new ToolbarActionMode(getActivity(), mCabToolbar);
                    mActionMode.startActionMode(mActionModeCallback);
                    return true;
                }
                return false;
            }
        });

        mToolbar.setTitle(mTitle);

        mRecyclerView = view.findViewById(R.id.controls_thumbnails_view_recycler_view);
        mRecyclerView.initView(mSpanCount, getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_grid_spacing));
        mRecyclerView.setItemViewCacheSize(mSpanCount * 2);
        if (mPdfViewCtrl != null && mPdfViewCtrl.getToolManager() != null) {
            if (((ToolManager)mPdfViewCtrl.getToolManager()).isNightMode()) {
                mRecyclerView.setBackgroundColor(getResources().getColor(R.color.controls_thumbnails_view_bg_dark));
            }
        }

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
                        mAdapter.updateMainViewWidth(getMainViewWidth());
                        updateSpanCount(mSpanCount);
                    }
                });
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);

        mItemSelectionHelper = new ItemSelectionHelper();
        mItemSelectionHelper.attachToRecyclerView(mRecyclerView);
        mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_MULTIPLE);

        mAdapter = new ThumbnailsViewAdapter(getActivity(), this, getFragmentManager(), mPdfViewCtrl, dataList, mSpanCount,
            mItemSelectionHelper);
        mAdapter.registerAdapterDataObserver(mItemSelectionHelper.getDataObserver());
        mAdapter.updateMainViewWidth(getMainViewWidth());
        mRecyclerView.setAdapter(mAdapter);

        mItemTouchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(mAdapter, mSpanCount, false, false));
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView recyclerView, View v, final int position, final long id) {
                if (mActionMode == null) {
                    mAdapter.setCurrentPage(position + 1);
                    mHasEventAction = true;
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATE_BY,
                        AnalyticsParam.viewerNavigateByParam(AnalyticsHandlerAdapter.VIEWER_NAVIGATE_BY_THUMBNAILS_VIEW));
                    dismiss();
                } else {
                    mItemSelectionHelper.setItemChecked(position, !mItemSelectionHelper.isItemChecked(position));
                    mActionMode.invalidate();
                }
            }
        });

        itemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView recyclerView, View v, final int position, final long id) {
                if (mIsReadOnly) {
                    return true;
                }
                if (mActionMode == null) {
                    mItemSelectionHelper.setItemChecked(position, true);

                    mActionMode = new ToolbarActionMode(getActivity(), mCabToolbar);
                    mActionMode.startActionMode(mActionModeCallback);
                } else {
                    if (mIsReadOnly) {
                        if (mOnThumbnailsEditAttemptWhileReadOnlyListener != null)
                            mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                        return true;
                    }
                    mRecyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
                            mItemTouchHelper.startDrag(holder);
                        }
                    });
                }

                return true;
            }
        });

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            int oldState = RecyclerView.SCROLL_STATE_IDLE;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mAdapter.clearOffScreenResources();
                }
                oldState = newState;
            }
        });

        getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    // Back key has been pressed
                    if (onBackPressed()) {
                        // Back key handled
                        return true;
                    } else {
                        dialog.dismiss();
                    }
                }
                return false;
            }
        });

        mFabMenu = view.findViewById(R.id.fab_menu);
        mFabMenu.setClosedOnTouchOutside(true);
        if (mIsReadOnly) {
            mFabMenu.setVisibility(View.GONE);
        }

        FloatingActionButton pagePdfButton = mFabMenu.findViewById(R.id.page_PDF);
        pagePdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                if (mIsReadOnly) {
                    if (mOnThumbnailsEditAttemptWhileReadOnlyListener != null)
                        mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                    return;
                }
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    Page lastPage = mPdfViewCtrl.getDoc().getPage(mPdfViewCtrl.getDoc().getPageCount());
                    AddPageDialogFragment addPageDialogFragment = AddPageDialogFragment.newInstance(
                        lastPage.getPageWidth(), lastPage.getPageHeight())
                        .setInitialPageSize(AddPageDialogFragment.PageSize.Custom);
                    addPageDialogFragment.setOnAddNewPagesListener(new AddPageDialogFragment.OnAddNewPagesListener() {
                            @Override
                            public void onAddNewPages(Page[] pages) {
                                if (pages == null || pages.length == 0) {
                                    return;
                                }

                                mAdapter.addDocPages(getLastSelectedPage(), ThumbnailsViewAdapter.DocumentFormat.PDF_PAGE, pages);
                                mHasEventAction = true;
                                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                                    AnalyticsParam.thumbnailsViewCountParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_ADD_BLANK_PAGES, pages.length));
                            }
                        });
                    addPageDialogFragment.show(getActivity().getSupportFragmentManager(), "add_page_dialog");
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
            }
        });

        FloatingActionButton pdfDocButton = mFabMenu.findViewById(R.id.PDF_doc);
        pdfDocButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                if (mIsReadOnly) {
                    if (mOnThumbnailsEditAttemptWhileReadOnlyListener != null)
                        mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                    return;
                }
                launchAndroidFilePicker();
            }
        });

        FloatingActionButton imagePdfButton = mFabMenu.findViewById(R.id.image_PDF);
        imagePdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFabMenu.close(true);
                if (mIsReadOnly) {
                    if (mOnThumbnailsEditAttemptWhileReadOnlyListener != null)
                        mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                    return;
                }

                mOutputFileUri = ViewerUtils.openImageIntent(ThumbnailsViewFragment.this);
            }
        });

        // adjust scroll position
        if (mRecyclerView != null && mAdapter != null && mPdfViewCtrl != null) {
            int pos = mPdfViewCtrl.getCurrentPage() - 1;
            if (pos >= 0 && pos < mAdapter.getItemCount()) {
                mRecyclerView.scrollToPosition(pos);
            }
        }
        if (getArguments() != null) {
            if (getArguments().getBoolean(BUNDLE_EDIT_MODE, false)) {
                mActionMode = new ToolbarActionMode(getActivity(), mCabToolbar);
                mActionMode.startActionMode(mActionModeCallback);
                if (mInitSelectedItem != null) {
                    mItemSelectionHelper.setItemChecked(mInitSelectedItem, true);
                    mActionMode.invalidate();
                    mInitSelectedItem = null;
                }
            }
        }
    }

    /**
     * The overload implementation of {@link DialogFragment#onResume()}.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (mPdfViewCtrl != null && mPdfViewCtrl.getToolManager() != null) {
            if (((ToolManager) mPdfViewCtrl.getToolManager()).canResumePdfDocWithoutReloading()) {
                addDocPages();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_THUMBNAILS_VIEW_OPEN);
    }

    @Override
    public void onStop() {
        super.onStop();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_THUMBNAILS_VIEW_OPEN);
    }

    /**
     * Adds document pages.
     */
    public void addDocPages() {
        if (mAddDocPagesDelay && mDataDelay != null) {
            mAddDocPagesDelay = false;
            mAdapter.addDocPages(mPositionDelay, mDocumentFormatDelay, mDataDelay);
        }
    }

    /**
     * The overload implementation of {@link DialogFragment#onSaveInstanceState(Bundle)}.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mOutputFileUri != null) {
            outState.putParcelable(BUNDLE_OUTPUT_FILE_URI, mOutputFileUri);
        }
    }

    /**
     * The overload implementation of {@link DialogFragment#onActivityResult(int, int, Intent)}.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == RequestCode.PICK_PDF_FILE) {
            if (data == null || data.getData() == null) {
                return;
            }

            // save the data and add pages to the document after onResume is called.
            mPositionDelay = getLastSelectedPage();
            mDocumentFormatDelay = ThumbnailsViewAdapter.DocumentFormat.PDF_DOC;
            mDataDelay = data.getData();
            mAddDocPagesDelay = true;
            mHasEventAction = true;
        }

        if (requestCode == RequestCode.PICK_PHOTO_CAM) {
            try {
                Map imageIntent = ViewerUtils.readImageIntent(data, activity, mOutputFileUri);
                if (!ViewerUtils.checkImageIntent(imageIntent)) {
                    Utils.handlePdfFromImageFailed(activity, imageIntent);
                    return;
                }

                // save the data and add pages to the document after onResume is called.
                mPositionDelay = getLastSelectedPage();
                mDocumentFormatDelay = ThumbnailsViewAdapter.DocumentFormat.IMAGE;
                mDataDelay = ViewerUtils.getImageBitmap(getContext(), imageIntent);
                if (mDataDelay == null) {
                    Utils.handlePdfFromImageFailed(activity, imageIntent);
                    return;
                }
                mAddDocPagesDelay = true;

                mHasEventAction = true;
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                    AnalyticsParam.thumbnailsViewParam(ViewerUtils.isImageFromCamera(imageIntent) ?
                        AnalyticsHandlerAdapter.THUMBNAILS_VIEW_ADD_PAGE_FROM_CAMERA : AnalyticsHandlerAdapter.THUMBNAILS_VIEW_ADD_PAGE_FROM_IMAGE));
            } catch (FileNotFoundException e) {
                CommonToast.showText(getContext(), getString(R.string.dialog_add_photo_document_filename_file_error), Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (Exception e) {
                CommonToast.showText(getActivity(), R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * Sets the PDFViewCtrl.
     *
     * @param pdfViewCtrl Sets the PDFViewCtrl
     *
     * @return The instance of the class
     */
    public ThumbnailsViewFragment setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        return this;
    }

    /**
     * Sets the OnThumbnailsViewDialogDismissListener listener
     *
     * @param listener The listener
     */
    public void setOnThumbnailsViewDialogDismissListener(OnThumbnailsViewDialogDismissListener listener) {
        mOnThumbnailsViewDialogDismissListener = listener;
    }

    /**
     * Sets the OnThumbnailsViewDialogDismissListener listener
     *
     * @param listener The listener
     */
    public void setOnThumbnailsEditAttemptWhileReadOnlyListener(OnThumbnailsEditAttemptWhileReadOnlyListener listener) {
        mOnThumbnailsEditAttemptWhileReadOnlyListener = listener;
    }

    /**
     * Sets the OnThumbnailsViewDialogDismissListener listener
     *
     * @param listener The listener
     */
    public void setOnExportThumbnailsListener(OnExportThumbnailsListener listener) {
        mOnExportThumbnailsListener = listener;
    }

    /**
     * Sets the specified item as checked.
     *
     * @param position The position
     */
    public void setItemChecked(int position) {
        mInitSelectedItem = position;
    }

    private void launchAndroidFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // Show pdf files only
        intent.setType("*/*");
        // Restrict to URIs that can be opened with ContentResolver#openFileDescriptor(Uri, String)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Force advanced devices (SD cards) to always be visible
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        startActivityForResult(intent, RequestCode.PICK_PDF_FILE);
    }

    private int getLastSelectedPage() {
        int lastSelectedPage = -1;
        if (mItemSelectionHelper.getCheckedItemCount() > 0) {
            lastSelectedPage = Integer.MIN_VALUE; // page-indexed
            SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();
            for (int i = 0; i < selectedItems.size(); i++) {
                if (selectedItems.valueAt(i)) {
                    int position = selectedItems.keyAt(i);
                    Map<String, Object> itemMap = mAdapter.getItem(position);
                    if (itemMap != null) {
                        int pageNum = (int) itemMap.get(ThumbnailsViewAdapter.PAGE_NUMBER_SRC);
                        if (pageNum > lastSelectedPage) {
                            lastSelectedPage = pageNum;
                        }
                    }
                }
            }
        }

        // (page-indexed, so conversion to zero-index is (lastSelectedPage-1)+1)
        // NOTE: pageCount+1 is allowed
        return lastSelectedPage;
    }

    /**
     * Sets the title.
     *
     * @param title The title
     */
    public void setTitle(String title) {
        mTitle = title;
        if (mToolbar != null)
            mToolbar.setTitle(title);
    }

    /**
     * The overload implementation of {@link DialogFragment#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mAdapter != null) {
            int viewWidth = getDisplayWidth();
            int thumbSize = getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_image_width);
            int thumbSpacing = getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_grid_spacing);
            // Calculate number of columns
            mSpanCount = (int) Math.floor(viewWidth / (thumbSize + thumbSpacing));

            mAdapter.updateMainViewWidth(viewWidth);
            updateSpanCount(mSpanCount); // notifyDataSetChanged will be called
        }

        if (mActionMode != null) {
            mActionMode.invalidate();
        }
    }

    private int getMainViewWidth() {
        if (mRecyclerView != null && ViewCompat.isLaidOut(mRecyclerView)) {
            return mRecyclerView.getMeasuredWidth();
        } else {
            return getDisplayWidth();
        }
    }

    private int getDisplayWidth() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return metrics.widthPixels;
    }

    /**
     * Updates span count.
     *
     * @param count The span count
     */
    public void updateSpanCount(int count) {
        mSpanCount = count;
        mRecyclerView.updateSpanCount(count);
    }

    /**
     * @return The attached adapter
     */
    public ThumbnailsViewAdapter getAdapter() {
        return mAdapter;
    }

    private boolean finishActionMode() {
        boolean success = false;
        if (mActionMode != null) {
            success = true;
            mActionMode.finish();
            mActionMode = null;
        }
        clearSelectedList();
        return success;
    }

    private void clearSelectedList() {
        if (mItemSelectionHelper != null) {
            mItemSelectionHelper.clearChoices();
        }
        if (mActionMode != null) {
            mActionMode.invalidate();
        }
    }

    private boolean onBackPressed() {
        if (!isAdded()) {
            return false;
        }

        boolean handled = false;
        if (mActionMode != null) {
            handled = finishActionMode();
        }
        return handled;
    }

    /**
     * The overload implementation of {@link DialogFragment#onDismiss(DialogInterface)}.
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (mPdfViewCtrl == null) {
            return;
        }

        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_THUMBNAILS_VIEW_CLOSE,
            AnalyticsParam.noActionParam(mHasEventAction));

        try {
            if (mAdapter.getDocPagesModified()) {
                // update page layout if document is modified
                mPdfViewCtrl.updatePageLayout();
            }
            // set current page to updated current page
            mPdfViewCtrl.setCurrentPage(mAdapter.getCurrentPage());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        // free resource
        mAdapter.clearResources();
        // cancel remaining request
        try {
            mPdfViewCtrl.cancelAllThumbRequests();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        // callback
        if (mOnThumbnailsViewDialogDismissListener != null) {
            mOnThumbnailsViewDialogDismissListener.onThumbnailsViewDialogDismiss(mAdapter.getCurrentPage(), mAdapter.getDocPagesModified());
        }
    }

    private ToolbarActionMode.Callback mActionModeCallback = new ToolbarActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ToolbarActionMode mode, Menu menu) {
            mode.inflateMenu(R.menu.cab_controls_fragment_thumbnails_view);

            mMenuItemUndo = menu.findItem(R.id.controls_thumbnails_view_action_undo);
            mMenuItemRedo = menu.findItem(R.id.controls_thumbnails_view_action_redo);
            mMenuItemRotate = menu.findItem(R.id.controls_thumbnails_view_action_rotate);
            mMenuItemDelete = menu.findItem(R.id.controls_thumbnails_view_action_delete);
            mMenuItemDuplicate = menu.findItem(R.id.controls_thumbnails_view_action_duplicate);
            mMenuItemExport = menu.findItem(R.id.controls_thumbnails_view_action_export);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ToolbarActionMode mode, Menu menu) {
            boolean isEnabled = mItemSelectionHelper.getCheckedItemCount() > 0;

            if (mMenuItemRotate != null) {
                mMenuItemRotate.setEnabled(isEnabled);
                if (mMenuItemRotate.getIcon() != null) {
                    mMenuItemRotate.getIcon().setAlpha(isEnabled ? 255 : 150);
                }
            }
            if (mMenuItemDelete != null) {
                mMenuItemDelete.setEnabled(isEnabled);
                if (mMenuItemDelete.getIcon() != null) {
                    mMenuItemDelete.getIcon().setAlpha(isEnabled ? 255 : 150);
                }
            }
            if (mMenuItemDuplicate != null) {
                mMenuItemDuplicate.setEnabled(isEnabled);
                if (mMenuItemDuplicate.getIcon() != null) {
                    mMenuItemDuplicate.getIcon().setAlpha(isEnabled ? 255 : 150);
                }
            }
            if (mMenuItemExport != null) {
                mMenuItemExport.setEnabled(isEnabled);
                if (mMenuItemExport.getIcon() != null) {
                    mMenuItemExport.getIcon().setAlpha(isEnabled ? 255 : 150);
                }
                mMenuItemExport.setVisible(mOnExportThumbnailsListener != null);
            }

            if (Utils.isTablet(getContext()) || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mode.setTitle(getString(R.string.controls_thumbnails_view_selected,
                    Utils.getLocaleDigits(Integer.toString(mItemSelectionHelper.getCheckedItemCount()))));
            } else {
                mode.setTitle(Utils.getLocaleDigits(Integer.toString(mItemSelectionHelper.getCheckedItemCount())));
            }
            updateUndoRedoIcons();
            return true;
        }

        @Override
        public boolean onActionItemClicked(ToolbarActionMode mode, MenuItem item) {
            if (mPdfViewCtrl == null) {
                throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
            }

            if (item.getItemId() == R.id.controls_thumbnails_view_action_rotate) {
                if (mIsReadOnly) {
                    if (mOnThumbnailsEditAttemptWhileReadOnlyListener != null)
                        mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                    return true;
                }
                // rotate all selected pages
                SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();
                List<Integer> pageList = new ArrayList<>();
                for (int i = 0; i < selectedItems.size(); i++) {
                    if (selectedItems.valueAt(i)) {
                        int position = selectedItems.keyAt(i);
                        mAdapter.rotateDocPage(position + 1);
                        pageList.add(position + 1);
                    }
                }
                manageRotatePages(pageList);
                mHasEventAction = true;
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                    AnalyticsParam.thumbnailsViewCountParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_ROTATE, selectedItems.size()));
            } else if (item.getItemId() == R.id.controls_thumbnails_view_action_delete) {
                if (mIsReadOnly) {
                    if (mOnThumbnailsEditAttemptWhileReadOnlyListener != null)
                        mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                    return true;
                }
                // Need to convert checked-positions to a sortable list
                List<Integer> pageList = new ArrayList<>();
                SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();

                int pageCount;
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    pageCount = mPdfViewCtrl.getDoc().getPageCount();
                } catch (Exception ex) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                    return true;
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }

                if (selectedItems.size() >= pageCount) {
                    CommonToast.showText(getContext(), R.string.controls_thumbnails_view_delete_msg_all_pages);
                    clearSelectedList();
                    return true;
                }

                for (int i = 0; i < selectedItems.size(); i++) {
                    if (selectedItems.valueAt(i)) {
                        pageList.add(selectedItems.keyAt(i) + 1);
                    }
                }
                // delete should start from back
                Collections.sort(pageList, Collections.reverseOrder());
                int count = pageList.size();
                for (int i = 0; i < count; ++i) {
                    mAdapter.removeDocPage(pageList.get(i));
                }
                clearSelectedList();
                manageDeletePages(pageList);
                mHasEventAction = true;
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                    AnalyticsParam.thumbnailsViewCountParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_DELETE, selectedItems.size()));
            } else if (item.getItemId() == R.id.controls_thumbnails_view_action_duplicate) {
                if (mAdapter != null) {
                    List<Integer> pageList = new ArrayList<>();
                    SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();
                    for (int i = 0; i < selectedItems.size(); i++) {
                        if (selectedItems.valueAt(i)) {
                            pageList.add(selectedItems.keyAt(i) + 1);
                        }
                    }
                    mAdapter.duplicateDocPages(pageList);
                    mHasEventAction = true;
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                        AnalyticsParam.thumbnailsViewCountParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_DUPLICATE, selectedItems.size()));
                }
            } else if (item.getItemId() == R.id.controls_thumbnails_view_action_export) {
                if (mOnExportThumbnailsListener != null) {
                    SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();
                    mOnExportThumbnailsListener.onExportThumbnails(selectedItems);
                    mHasEventAction = true;
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                        AnalyticsParam.thumbnailsViewCountParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_EXPORT, selectedItems.size()));
                }
            } else if (item.getItemId() == R.id.controls_thumbnails_view_action_undo) {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                if (toolManager != null && toolManager.getUndoRedoManger() != null) {
                    String undoInfo = toolManager.getUndoRedoManger().undo(AnalyticsHandlerAdapter.LOCATION_THUMBNAILS_VIEW, true);
                    updateUndoRedoIcons();
                    if (!Utils.isNullOrEmpty(undoInfo)) {
                        try {
                            if (UndoRedoManager.isDeletePagesAction(getContext(), undoInfo)) {
                                List<Integer> pageList = UndoRedoManager.getPageList(undoInfo);
                                if (pageList.size() != 0) {
                                    mAdapter.updateAfterAddition(pageList);
                                }
                            } else if (UndoRedoManager.isAddPagesAction(getContext(), undoInfo)) {
                                List<Integer> pageList = UndoRedoManager.getPageList(undoInfo);
                                if (pageList.size() != 0) {
                                    mAdapter.updateAfterDeletion(pageList);
                                }
                            } else if (UndoRedoManager.isRotatePagesAction(getContext(), undoInfo)) {
                                List<Integer> pageList = UndoRedoManager.getPageList(undoInfo);
                                if (pageList.size() != 0) {
                                    mAdapter.updateAfterRotation(pageList);
                                }
                            } else if (UndoRedoManager.isMovePageAction(getContext(), undoInfo)) {
                                mAdapter.updateAfterMove(UndoRedoManager.getPageTo(undoInfo),
                                    UndoRedoManager.getPageFrom(undoInfo));
                            }
                        } catch (Exception e) {
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        }
                    }
                }
            } else if (item.getItemId() == R.id.controls_thumbnails_view_action_redo) {
                ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                if (toolManager != null && toolManager.getUndoRedoManger() != null) {
                    String redoInfo = toolManager.getUndoRedoManger().redo(AnalyticsHandlerAdapter.LOCATION_THUMBNAILS_VIEW, true);
                    updateUndoRedoIcons();
                    if (!Utils.isNullOrEmpty(redoInfo)) {
                        try {
                            if (UndoRedoManager.isDeletePagesAction(getContext(), redoInfo)) {
                                List<Integer> pageList = UndoRedoManager.getPageList(redoInfo);
                                if (pageList.size() != 0) {
                                    mAdapter.updateAfterDeletion(pageList);
                                }
                            } else if (UndoRedoManager.isAddPagesAction(getContext(), redoInfo)) {
                                List<Integer> pageList = UndoRedoManager.getPageList(redoInfo);
                                if (pageList.size() != 0) {
                                    mAdapter.updateAfterAddition(pageList);
                                }
                            } else if (UndoRedoManager.isRotatePagesAction(getContext(), redoInfo)) {
                                List<Integer> pageList = UndoRedoManager.getPageList(redoInfo);
                                if (pageList.size() != 0) {
                                    mAdapter.updateAfterRotation(pageList);
                                }
                            } else if (UndoRedoManager.isMovePageAction(getContext(), redoInfo)) {
                                mAdapter.updateAfterMove(UndoRedoManager.getPageFrom(redoInfo),
                                    UndoRedoManager.getPageTo(redoInfo));
                            }
                        } catch (Exception e) {
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        }
                    }
                }
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ToolbarActionMode mode) {
            mActionMode = null;
            clearSelectedList();
        }
    };

    private void manageAddPages(List<Integer> pageList) {
        if (mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager != null) {
            toolManager.raisePagesAdded(pageList);
        }

        updateUndoRedoIcons();
    }

    private void manageDeletePages(List<Integer> pageList) {
        if (mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager != null) {
            toolManager.raisePagesDeleted(pageList);
        }

        updateUndoRedoIcons();
    }

    private void manageRotatePages(List<Integer> pageList) {
        if (mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager != null) {
            toolManager.raisePagesRotated(pageList);
        }

        updateUndoRedoIcons();
    }

    private void manageMovePage(int fromPageNum, int toPageNum) {
        if (mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager != null) {
            toolManager.raisePageMoved(fromPageNum, toPageNum);
        }

        updateUndoRedoIcons();
    }

    /**
     * The overload implementation of {@link ThumbnailsViewAdapter.EditPagesListener#onPagesAdded(List)}.
     */
    @Override
    public void onPagesAdded(List<Integer> pageList) {
        manageAddPages(pageList);
        if (mDocumentFormatDelay != null) {
            if (mDocumentFormatDelay == ThumbnailsViewAdapter.DocumentFormat.PDF_DOC) {
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW,
                    AnalyticsParam.thumbnailsViewCountParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_ADD_PAGES_FROM_DOCS, pageList.size()));
            }
            mDocumentFormatDelay = null;
        }
    }

    /**
     * The overload implementation of {@link ThumbnailsViewAdapter.EditPagesListener#onPageMoved(int, int)}.
     */
    @Override
    public void onPageMoved(int fromPageNum, int toPageNum) {
        manageMovePage(fromPageNum, toPageNum);
        // move pages event
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_THUMBNAILS_VIEW, AnalyticsParam.thumbnailsViewParam(AnalyticsHandlerAdapter.THUMBNAILS_VIEW_MOVE));
    }

    /**
     * Updates undo/redo icons
     */
    public void updateUndoRedoIcons() {
        if (mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        }

        if (mMenuItemUndo != null && mMenuItemRedo != null) {
            boolean undoEnabled = false;
            boolean redoEnabled = false;

            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (toolManager != null) {
                UndoRedoManager undoRedoManager = toolManager.getUndoRedoManger();
                if (undoRedoManager != null) {
                    undoEnabled = undoRedoManager.isNextUndoEditPageAction();
                    redoEnabled = undoRedoManager.isNextRedoEditPageAction();
                }
            }

            mMenuItemUndo.setEnabled(undoEnabled);
            if (mMenuItemUndo.getIcon() != null) {
                mMenuItemUndo.getIcon().setAlpha(undoEnabled ? 255 : 150);
            }
            mMenuItemRedo.setEnabled(redoEnabled);
            if (mMenuItemRedo.getIcon() != null) {
                mMenuItemRedo.getIcon().setAlpha(redoEnabled ? 255 : 150);
            }
        }
    }

    /**
     * Callback interface to be invoked when the dialog fragment is dismissed.
     */
    public interface OnThumbnailsViewDialogDismissListener {
        /**
         * Called when the thumbnails view dialog has been dismissed.
         *
         * @param pageNum The selected page number
         * @param docPagesModified True if the pages of the document has been modified
         */
        void onThumbnailsViewDialogDismiss(int pageNum, boolean docPagesModified);
    }

    /**
     * Callback interface to be invoked when the user attempts to edit pages while the document is read only.
     */
    public interface OnThumbnailsEditAttemptWhileReadOnlyListener {
        /**
         * Called when the user attempts to edit pages while the document is read only.
         */
        void onThumbnailsEditAttemptWhileReadOnly();
    }

    /**
     * Callback interface to be invoked when pages should be exported.
     */
    public interface OnExportThumbnailsListener {
        /**
         * The implementation should export given pages.
         *
         * @param pageNums The page numbers to be exported
         */
        void onExportThumbnails(SparseBooleanArray pageNums);
    }
}
