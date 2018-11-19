//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Print;
import com.pdftron.pdf.asynctask.PopulateAnnotationInfoListTask;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The AnnotationDialogFragment shows a list of all the annotations in a
 * document being viewed by a {@link com.pdftron.pdf.PDFViewCtrl}. The list will
 * contain any comments that have been added to the annotations and clicking on
 * an annotation will show it in the PDFViewCtrl.
 */
public class AnnotationDialogFragment extends NavigationListDialogFragment {

    /**
     * Bundle key to specify whether the document is read only or not
     */
    public static final String BUNDLE_IS_READ_ONLY = "is_read_only";
    public static final String BUNDLE_IS_RTL = "is_right-to-left";

    private static final int CONTEXT_MENU_DELETE_ITEM = 0;
    private static final int CONTEXT_MENU_DELETE_ITEM_ON_PAGE = 1;
    private static final int CONTEXT_MENU_DELETE_ALL = 2;

    protected boolean mIsReadOnly;
    protected boolean mIsRtl;
    private ArrayList<AnnotationInfo> mAnnotation;
    private AnnotationsAdapter mAnnotationsAdapter;
    private RecyclerView mRecyclerView;
    private PopulateAnnotationInfoListTask mPopulateAnnotationListTask;
    private TextView mEmptyTextView;
    private PDFViewCtrl mPdfViewCtrl;
    private FloatingActionButton mFab;
    private AnnotationDialogListener mAnnotationDialogListener;
    private ProgressBar mProgressBarView;

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface AnnotationDialogListener {
        /**
         * Called when an annotation has been clicked.
         *
         * @param annotation The annotation
         * @param pageNum The page number that holds the annotation
         */
        void onAnnotationClicked(Annot annotation, int pageNum);

        /**
         * Called when document annotations have been exported.
         *
         * @param outputDoc The PDFDoc containing the exported annotations
         */
        void onExportAnnotations(PDFDoc outputDoc);
    }

    /**
     * @return a new instance of this class
     */
    public static AnnotationDialogFragment newInstance() {
        return new AnnotationDialogFragment();
    }

    /**
     * Sets the {@link PDFViewCtrl}
     *
     * @param pdfViewCtrl The {@link PDFViewCtrl}
     *
     * @return This class
     */
    public AnnotationDialogFragment setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        return this;
    }

    /**
     * Sets if the document is read only
     *
     * @param isReadOnly True if the document is read only
     *
     * @return This class
     */
    @SuppressWarnings("unused")
    public AnnotationDialogFragment setReadOnly(boolean isReadOnly) {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putBoolean(BUNDLE_IS_READ_ONLY, isReadOnly);
        setArguments(args);

        return this;
    }

    /**
     * Sets if the document is right-to-left
     *
     * @param isRtl True if the document is right-to-left
     *
     * @return This class
     */
    @SuppressWarnings("unused")
    public AnnotationDialogFragment setRtlMode(boolean isRtl) {
        Bundle args = getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putBoolean(BUNDLE_IS_RTL, isRtl);
        setArguments(args);

        return this;
    }

    /**
     * Sets the listener to {@link AnnotationDialogListener}
     *
     * @param listener The listener
     */
    public void setAnnotationDialogListener(AnnotationDialogListener listener) {
        mAnnotationDialogListener = listener;
    }

    /**
     * The overloaded implementation of {@link DialogFragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            mIsReadOnly = args.getBoolean(BUNDLE_IS_READ_ONLY);
            mIsRtl = args.getBoolean(BUNDLE_IS_RTL);
        }

        View view = inflater.inflate(R.layout.controls_fragment_annotation_dialog, null);

        // Get reference to controls
        mRecyclerView = view.findViewById(R.id.recyclerview_control_annotation);
        mEmptyTextView = view.findViewById(R.id.control_annotation_textview_empty);
        mProgressBarView = view.findViewById(R.id.progress_bar_view);

        mFab = view.findViewById(R.id.export_annotations_button);
        if (mIsReadOnly) {
            mFab.setVisibility(View.GONE);
        }
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    PDFDoc outputDoc = Print.exportAnnotations(mPdfViewCtrl.getDoc(), mIsRtl);
                    if (mAnnotationDialogListener != null) {
                        mAnnotationDialogListener.onExportAnnotations(outputDoc);
                    }
                } catch (Exception ePDFNet) {
                    AnalyticsHandlerAdapter.getInstance().sendException(ePDFNet);
                }
                onEventAction();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATIONS_LIST,
                    AnalyticsParam.annotationsListActionParam(AnalyticsHandlerAdapter.ANNOTATIONS_LIST_EXPORT));
            }
        });

        // Add click listener to the list
        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);
        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView recyclerView, View view, int position, long id) {
                onEventAction();
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_NAVIGATE_BY,
                    AnalyticsParam.viewerNavigateByParam(AnalyticsHandlerAdapter.VIEWER_NAVIGATE_BY_ANNOTATIONS_LIST));

                AnnotationInfo annotInfo = mAnnotation.get(position);
                if (mPdfViewCtrl != null) {
                    ViewerUtils.jumpToAnnotation(mPdfViewCtrl, annotInfo.getAnnotation(), annotInfo.getPageNum());
                }

                // Notify listeners
                if (mAnnotationDialogListener != null) {
                    mAnnotationDialogListener.onAnnotationClicked(annotInfo.getAnnotation(), annotInfo.getPageNum());
                }
            }
        });


        return view;
    }

    /**
     * The overloaded implementation of {@link DialogFragment#onViewCreated(View, Bundle)}
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAnnotation = new ArrayList<>();
        mAnnotationsAdapter = new AnnotationsAdapter(mAnnotation);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mRecyclerView.setAdapter(mAnnotationsAdapter);

        mEmptyTextView.setText(R.string.controls_annotation_dialog_loading);
        // This task will populate mAnnotation
        mPopulateAnnotationListTask = new PopulateAnnotationInfoListTask(mPdfViewCtrl);
        mPopulateAnnotationListTask.setCallback(new PopulateAnnotationInfoListTask.Callback() {
                @Override
                public void getAnnotationsInfo(ArrayList<AnnotationInfo> result, boolean done) {
                    if (result == null) {
                        result = new ArrayList<>();
                    }

                    mAnnotation.clear();
                    mAnnotation.addAll(result);
                    mAnnotationsAdapter.notifyDataSetChanged();
                    if (mFab != null) {
                        mFab.setVisibility(result.size() > 0 ? View.VISIBLE : View.GONE);
                        if (mIsReadOnly) {
                            mFab.setVisibility(View.GONE);
                        }
                    }

                    mEmptyTextView.setText(R.string.controls_annotation_dialog_empty);
                    if (result.isEmpty()) {
                        mEmptyTextView.setVisibility(View.VISIBLE);
                        mRecyclerView.setVisibility(View.GONE);
                    } else {
                        mEmptyTextView.setVisibility(View.GONE);
                        mRecyclerView.setVisibility(View.VISIBLE);
                    }

                    if (done) {
                        mProgressBarView.setVisibility(View.GONE);
                    }
                }
            });
        mPopulateAnnotationListTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * The overloaded implementation of {@link DialogFragment#onStop()}
     */
    @Override
    public void onStop() {
        if (mPopulateAnnotationListTask != null) {
            mPopulateAnnotationListTask.cancel(true);
        }
        super.onStop();
    }

    private void deleteOnPage(AnnotationInfo annotationInfo) {
        if (mPdfViewCtrl == null) {
            return;
        }

        int pageNum = annotationInfo.getPageNum();
        boolean hasChange = false;
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread
            // safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            ArrayList<AnnotationInfo> items = mAnnotationsAdapter.getItemsOnPage(pageNum);
            Page page = mPdfViewCtrl.getDoc().getPage(pageNum);
            for (AnnotationInfo info : items) {
                if (info.getAnnotation() != null) {
                    page.annotRemove(info.getAnnotation());
                    mAnnotationsAdapter.remove(info);
                }
            }
            mPdfViewCtrl.update(true);
            hasChange = mPdfViewCtrl.getDoc().hasChangesSinceSnapshot();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        if (hasChange) {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (toolManager != null) {
                toolManager.raiseAnnotationsRemovedEvent(pageNum);
            }
        }

        mAnnotationsAdapter.notifyDataSetChanged();
    }

    private void deleteAll() {
        if (mPdfViewCtrl == null) {
            return;
        }

        boolean hasChange = false;
        boolean shouldUnlock = false;
        try {
            // Locks the document first as accessing annotation/doc information isn't thread
            // safe.
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            AnnotUtils.safeDeleteAllAnnots(mPdfViewCtrl.getDoc());
            mPdfViewCtrl.update(true);
            hasChange = mPdfViewCtrl.getDoc().hasChangesSinceSnapshot();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }

        if (hasChange) {
            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (toolManager != null) {
                toolManager.raiseAllAnnotationsRemovedEvent();
            }
        }

        mAnnotationsAdapter.clear();
        mAnnotationsAdapter.notifyDataSetChanged();
    }

    /**
     * Called when a toolbar menu item has been clicked.
     *
     * @param item The menu item that was clicked
     */
    public void onToolbarMenuItemClicked(MenuItem item) {

    }

    /**
     * Annotation Info class. Internal use in {@link com.pdftron.pdf.asynctask.PopulateAnnotationInfoListTask}
     */
    public static class AnnotationInfo {
        /**
         * The annotation type is one of the types found in com.pdftron.pdf.Annot.
         */
        private int mType;

        /**
         * Holds the page where this annotation is found.
         */
        private int mPageNum;

        /**
         * The contents of the annotation are used in the list view of the
         * BookmarkDialogFragment.
         */
        private String mContent;

        /**
         * The author for this annotation.
         */
        private String mAuthor;

        private Annot mAnnotation;

        /**
         * This date and time info for this annotaiton
         */
        private String mDate;

        /**
         * Default constructor. Creates an empty annotation entry.
         */
        @SuppressWarnings("unused")
        public AnnotationInfo() {
            // TODO Maybe -1, -1, "", ""?
            this(0, 0, "", "", "", null);
        }

        /**
         * Class constructor specifying the type, page and content of the
         * annotation.
         *
         * @param type    the type of the annotation
         * @param pageNum the page where this annotation lies in
         * @param content the content of the annotation
         * @param date The date
         * @see <a href="http://www.pdftron.com/pdfnet/mobile/Javadoc/pdftron/PDF/Annot.html">Class Annot</a>
         */
        public AnnotationInfo(int type, int pageNum, String content, String author, String date, Annot annotation) {
            this.mType = type;
            this.mPageNum = pageNum;
            this.mContent = content;
            this.mAuthor = author;
            this.mDate = date;
            this.mAnnotation = annotation;
        }

        /**
         * Returns the type of this annotation.
         *
         * @return The type of the annotation
         * @see com.pdftron.pdf.Annot
         */
        public int getType() {
            return mType;
        }

        /**
         * Sets the type of the annotation.
         *
         * @param mType The type of the annotation
         * @see com.pdftron.pdf.Annot
         */
        public void setType(int mType) {
            this.mType = mType;
        }

        /**
         * @return The page number where the annotation is on
         */
        public int getPageNum() {
            return mPageNum;
        }

        /**
         * Sets he page number where the annotation is on.
         *
         * @param mPageNum The page number
         */
        public void setPageNum(int mPageNum) {
            this.mPageNum = mPageNum;
        }

        /**
         * @return The content
         */
        public String getContent() {
            return mContent;
        }

        /**
         * Sets the content.
         *
         * @param mContent The content
         */
        public void setContent(String mContent) {
            this.mContent = mContent;
        }

        /**
         * @return The author
         */
        public String getAuthor() {
            return mAuthor;
        }

        /**
         * Sets the author.
         *
         * @param author The author
         * */
        public void setAuthor(String author) {
            this.mAuthor = author;
        }

        /**
         * @return The annotation
         */
        public Annot getAnnotation() {
            return mAnnotation;
        }

        /**
         * Get date
         * @return Date of the annotation
         */
        public String getDate() {
            return mDate;
        }
    }

    private class AnnotationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int STATE_UNKNOWN = 0;
        private static final int STATE_SECTIONED_CELL = 1;
        private static final int STATE_REGULAR_CELL = 2;
        private ArrayList<AnnotationInfo> mAnnotation;
        private int[] mCellStates;

        private RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
            public void onChanged() {
                mCellStates = mAnnotation == null ? null : new int[mAnnotation.size()];
            }
        };

        AnnotationsAdapter(ArrayList<AnnotationInfo> objects) {
            mAnnotation = objects;

            mCellStates = new int[objects.size()];
            registerAdapterDataObserver(observer);
        }


        public AnnotationInfo getItem(int position) {
            if (mAnnotation != null && position >= 0 && position < mAnnotation.size()) {
                return mAnnotation.get(position);
            }
            return null;
        }

        ArrayList<AnnotationInfo> getItemsOnPage(int pageNum) {
            ArrayList<AnnotationInfo> list = new ArrayList<>();
            if (mAnnotation != null) {
                for (AnnotationInfo info : mAnnotation) {
                    if (info.getPageNum() == pageNum) {
                        list.add(info);
                    }
                }
                return list;
            }
            return null;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(
                R.layout.controls_fragment_annotation_listview_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            Context context = getContext();
            if (context == null) {
                return;
            }

            boolean needSeparator = false;
            AnnotationInfo annotationInfo = mAnnotation.get(position);
            if (position < mCellStates.length) {
                switch (mCellStates[position]) {
                    case STATE_SECTIONED_CELL:
                        needSeparator = true;
                        break;
                    case STATE_REGULAR_CELL:
                        needSeparator = false;
                        break;
                    case STATE_UNKNOWN:
                    default:
                        if (position == 0) {
                            needSeparator = true;
                        } else {
                            AnnotationInfo previousAnnotation = mAnnotation.get(position - 1);
                            if (annotationInfo.getPageNum() != previousAnnotation.getPageNum()) {
                                needSeparator = true;
                            }
                        }

                        // Cache the result
                        mCellStates[position] = needSeparator ? STATE_SECTIONED_CELL : STATE_REGULAR_CELL;
                        break;
                }
            }

            ViewHolder viewHolder = (ViewHolder) holder;

            if (needSeparator) {
                viewHolder.separator.setText(String.format(getString(R.string.controls_annotation_dialog_page), annotationInfo.getPageNum()));
                viewHolder.separator.setVisibility(View.VISIBLE);
            } else {
                viewHolder.separator.setVisibility(View.GONE);
            }
            String content = annotationInfo.getContent();
            if (Utils.isNullOrEmpty(content)) {
                viewHolder.line1.setVisibility(View.GONE);
            } else {
                viewHolder.line1.setText(annotationInfo.getContent());
                viewHolder.line1.setVisibility(View.VISIBLE);
            }

            // Set icon based on the annotation type
            viewHolder.icon.setImageResource(AnnotUtils.getAnnotImageResId(annotationInfo.getType()));

            StringBuilder descBuilder = new StringBuilder();
            if (PdfViewCtrlSettingsManager.getAnnotListShowAuthor(context)) {
                String author = annotationInfo.getAuthor();
                if (!author.isEmpty()) {
                    descBuilder.append(author).append(", ");
                }
            }
            descBuilder.append(annotationInfo.getDate());
            viewHolder.line2.setText(descBuilder.toString());

            // set author and date
            Annot annot = annotationInfo.getAnnotation();

            int color = AnnotUtils.getAnnotColor(annot);
            if (color == -1) {
                color = Color.BLACK;
            }
            viewHolder.icon.setColorFilter(color);
            viewHolder.icon.setAlpha(AnnotUtils.getAnnotOpacity(annot));
        }

        @Override
        public int getItemCount() {
            if (mAnnotation != null) {
                return mAnnotation.size();
            } else {
                return 0;
            }
        }

        public void clear() {
            mAnnotation.clear();
        }

        public boolean remove(AnnotationInfo annotInfo) {
            return mAnnotation.remove(annotInfo);
        }

        private class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
            TextView separator;
            TextView line1;
            TextView line2;
            ImageView icon;

            public ViewHolder(View itemView) {
                super(itemView);
                separator = itemView.findViewById(R.id.textview_annotation_recyclerview_item_separator);
                icon = itemView.findViewById(R.id.imageview_annotation_recyclerview_item);
                line1 = itemView.findViewById(R.id.textview_annotation_recyclerview_item);
                line2 = itemView.findViewById(R.id.textview_desc_recyclerview_item);
                if (!mIsReadOnly) {
                    itemView.setOnCreateContextMenuListener(this);
                }
            }

            @Override
            public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
                final int position = mRecyclerView.getChildAdapterPosition(view);
                AnnotationInfo item = mAnnotationsAdapter.getItem(position);
                if (item != null) {
                    String title = String.format(getString(R.string.controls_annotation_dialog_page), item.getPageNum());
                    String author = item.getAuthor();
                    if (!Utils.isNullOrEmpty(author)) {
                        title = title + " " + getString(R.string.controls_annotation_dialog_author) + " " + author;
                    }
                    menu.setHeaderTitle(title);
                }
                String[] menuItems = getResources().getStringArray(R.array.annotation_dialog_context_menu);
                menu.add(Menu.NONE, CONTEXT_MENU_DELETE_ITEM, CONTEXT_MENU_DELETE_ITEM, menuItems[CONTEXT_MENU_DELETE_ITEM]);
                String deleteOnPage = menuItems[CONTEXT_MENU_DELETE_ITEM_ON_PAGE];
                if (item != null) {
                    deleteOnPage = deleteOnPage + " " + item.getPageNum();
                }
                menu.add(Menu.NONE, CONTEXT_MENU_DELETE_ITEM_ON_PAGE, CONTEXT_MENU_DELETE_ITEM_ON_PAGE, deleteOnPage);
                menu.add(Menu.NONE, CONTEXT_MENU_DELETE_ALL, CONTEXT_MENU_DELETE_ALL, menuItems[CONTEXT_MENU_DELETE_ALL]);
                MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        onContextMenuItemClicked(item, position);
                        return true;
                    }
                };
                menu.getItem(CONTEXT_MENU_DELETE_ITEM).setOnMenuItemClickListener(listener);
                menu.getItem(CONTEXT_MENU_DELETE_ITEM_ON_PAGE).setOnMenuItemClickListener(listener);
                menu.getItem(CONTEXT_MENU_DELETE_ALL).setOnMenuItemClickListener(listener);
            }

            void onContextMenuItemClicked(MenuItem item, int position) {
                int menuItemIndex = item.getItemId();
                switch (menuItemIndex) {
                    case CONTEXT_MENU_DELETE_ITEM:
                        AnnotationInfo annotationInfo = mAnnotationsAdapter.getItem(position);
                        if (annotationInfo == null || mPdfViewCtrl == null) {
                            return;
                        }
                        Annot annot = annotationInfo.getAnnotation();
                        if (annot != null) {
                            int annotPageNum = annotationInfo.getPageNum();
                            HashMap<Annot, Integer> annots = new HashMap<>(1);
                            annots.put(annot, annotPageNum);
                            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
                            boolean shouldUnlock = false;
                            try {
                                // Locks the document first as accessing annotation/doc information isn't thread
                                // safe.
                                mPdfViewCtrl.docLock(true);
                                shouldUnlock = true;
                                if (toolManager != null) {
                                    toolManager.raiseAnnotationsPreRemoveEvent(annots);
                                }

                                Page page = mPdfViewCtrl.getDoc().getPage(annotPageNum);
                                page.annotRemove(annot);
                                mPdfViewCtrl.update(annot, annotPageNum);

                                // make sure to raise remove event after mPdfViewCtrl.update
                                if (toolManager != null) {
                                    toolManager.raiseAnnotationsRemovedEvent(annots);
                                }

                                mAnnotationsAdapter.remove(mAnnotationsAdapter.getItem(position));
                                mAnnotationsAdapter.notifyDataSetChanged();
                            } catch (Exception e) {
                                AnalyticsHandlerAdapter.getInstance().sendException(e);
                            } finally {
                                if (shouldUnlock) {
                                    mPdfViewCtrl.docUnlock();
                                }
                            }
                        }
                        onEventAction();
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATIONS_LIST,
                            AnalyticsParam.annotationsListActionParam(AnalyticsHandlerAdapter.ANNOTATIONS_LIST_DELETE));
                        break;
                    case CONTEXT_MENU_DELETE_ITEM_ON_PAGE:
                        annotationInfo = mAnnotationsAdapter.getItem(position);
                        if (annotationInfo != null && annotationInfo.getAnnotation() != null) {
                            deleteOnPage(annotationInfo);
                        }
                        onEventAction();
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATIONS_LIST,
                            AnalyticsParam.annotationsListActionParam(AnalyticsHandlerAdapter.ANNOTATIONS_LIST_DELETE_ALL_ON_PAGE));
                        break;
                    case CONTEXT_MENU_DELETE_ALL:
                        deleteAll();
                        onEventAction();
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ANNOTATIONS_LIST,
                            AnalyticsParam.annotationsListActionParam(AnalyticsHandlerAdapter.ANNOTATIONS_LIST_DELETE_ALL_IN_DOC));
                        break;
                }
            }
        }
    }
}
