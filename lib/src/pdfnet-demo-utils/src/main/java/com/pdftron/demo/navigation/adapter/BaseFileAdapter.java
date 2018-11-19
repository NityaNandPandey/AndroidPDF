//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.pdftron.common.RecentlyUsedCache;
import com.pdftron.demo.R;
import com.pdftron.demo.model.FileHeader;
import com.pdftron.demo.navigation.adapter.viewholder.ContentViewHolder;
import com.pdftron.demo.navigation.adapter.viewholder.FooterViewHolder;
import com.pdftron.demo.navigation.adapter.viewholder.HeaderViewHolder;
import com.pdftron.demo.utils.FileListFilter;
import com.pdftron.demo.utils.ThumbnailPathCacheManager;
import com.pdftron.demo.utils.ThumbnailWorker;
import com.pdftron.pdf.PreviewHandler;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemTouchHelperCallback;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerViewAdapter;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;

import org.apache.commons.io.FilenameUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class BaseFileAdapter<FileInfo extends BaseFileInfo> extends SimpleRecyclerViewAdapter<FileInfo, RecyclerView.ViewHolder> implements
    Filterable,
    ThumbnailWorker.ThumbnailWorkerListener,
    FileListFilter.FilterPublishListener<FileInfo> {

    private static final String TAG = BaseFileAdapter.class.getName();

    public static final int VIEW_TYPE_HEADER = ItemTouchHelperCallback.VIEW_TYPE_HEADER;
    public static final int VIEW_TYPE_CONTENT = ItemTouchHelperCallback.VIEW_TYPE_CONTENT;
    public static final int VIEW_TYPE_FOOTER = ItemTouchHelperCallback.VIEW_TYPE_FOOTER;

    private WeakReference<Context> mContext;
    protected ArrayList<FileInfo> mFiles;
    private ArrayList<FileInfo> mOriginalFiles;
    private FileListFilter mFilter;
    private final Object mOriginalFilesLock;

    protected AdapterListener mAdapterListener;

    protected int mHeaderLayoutResourceId;
    protected int mFooterLayoutResourceId;
    protected int mListLayoutResourceId;
    protected int mGridLayoutResourceId;

    protected Bitmap mGridLoadingBitmap;
    protected Bitmap mListLoadingBitmap;
    private Bitmap mLoadingBitmap;
    protected int mSpanCount;
    private int mRecyclerViewWidth = 0;

    protected ThumbnailWorker mThumbnailWorker;
    protected int mMinXSize;
    protected int mMinYSize;
    private int mLockSize;

    private boolean mShowInfoButton;
    private boolean mShowFavoriteIndicator;

    private boolean mIsInSearchMode = false;

    //Headers
    protected HashMap<String, FileHeader> mOriginalHeaders;
    protected HashMap<String, ArrayList<FileInfo>> headerChildMap;


    public interface AdapterListener {
        void onShowFileInfo(int position);

        void onFilterResultsPublished(int resultCode);
    }

    public BaseFileAdapter(Context context, ArrayList<FileInfo> objects, Object objectsLock,
                           int spanCount,
                           AdapterListener adapterListener, ViewHolderBindListener bindListener) {
        super(bindListener);

        mContext = new WeakReference<>(context);
        mOriginalFiles = objects;
        // Don't assign mFiles to objects if a lock is provided
        // (This assumes that when a lock IS provided, mFiles will be populated after mOriginalFiles is filtered)
        mFiles = (objectsLock != null) ? null : objects;
        mOriginalFilesLock = (objectsLock != null) ? objectsLock : new Object();
        mAdapterListener = adapterListener;

        mSpanCount = spanCount;

        int listLoadingRes = Utils.getResourceDrawable(context, getResources().getString(R.string.list_loading_res_name));
        int gridLoadingRes = Utils.getResourceDrawable(context, getResources().getString(R.string.grid_loading_res_name));
        if (listLoadingRes == 0) {
            listLoadingRes = R.drawable.file_generic;
        }
        if (gridLoadingRes == 0) {
            gridLoadingRes = R.drawable.white_square;
        }

        mListLoadingBitmap = BitmapFactory.decodeResource(context.getResources(), listLoadingRes);
        mGridLoadingBitmap = BitmapFactory.decodeResource(context.getResources(), gridLoadingRes);

        if (spanCount > 0) {
            mLoadingBitmap = mGridLoadingBitmap;
        } else {
            mLoadingBitmap = mGridLoadingBitmap;
        }
        mHeaderLayoutResourceId = R.layout.recyclerview_header_item;
        mFooterLayoutResourceId = R.layout.recyclerview_footer_item;
        mListLayoutResourceId = R.layout.listview_item_file_list;
        mGridLayoutResourceId = R.layout.gridview_item_file_list;

        // ensure the size of thumbnail is not zero
        updateSpanCount(mSpanCount);
        mThumbnailWorker = new ThumbnailWorker(context, mMinXSize, mMinYSize, mLoadingBitmap);
        mThumbnailWorker.setListener(this);

        mShowInfoButton = true;
        mShowFavoriteIndicator = true;
        headerChildMap = new HashMap<>();
        mOriginalHeaders = new HashMap<>();
    }

    protected Context getContext() {
        return mContext.get();
    }

    protected Resources getResources() {
        Context context = getContext();
        if (context == null) {
            return null;
        }
        return context.getResources();
    }

    public int getSpanCount() {
        return mSpanCount;
    }

    protected ArrayList<FileInfo> getItems() {
        return mFiles;
    }

    @NonNull
    protected Object getListLock() {
        return mOriginalFilesLock;
    }

    @Override
    public FileInfo getItem(int position) {
        if (mFiles != null && position >= 0 && position < mFiles.size()) {
            return mFiles.get(position);
        }
        return null;
    }

    @Override
    public final void add(FileInfo item) {
        if (mFiles != null) {
            mFiles.add(item);
        }
    }

    @Override
    public final boolean remove(FileInfo item) {
        return (mFiles != null && mFiles.remove(item));
    }

    @Override
    public FileInfo removeAt(int location) {
        return (mFiles != null) ? mFiles.remove(location) : null;
    }

    @Override
    public void insert(FileInfo item, int position) {
        if (mFiles != null) {
            mFiles.add(position, item);
        }
    }

    @Override
    public int getItemCount() {
        return (mFiles != null) ? mFiles.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return isHeader(position) ? VIEW_TYPE_HEADER : VIEW_TYPE_CONTENT;
    }

    private void expandChildView(int headerPos, ArrayList<FileInfo> childInfoList) {

        for (int i = 0, cnt = childInfoList.size(); i < cnt; i++) {
            FileInfo child = childInfoList.get(i);
            insert(child, headerPos + 1 + i);
        }
        Utils.safeNotifyItemChanged(this, headerPos);
        notifyItemRangeInserted(headerPos + 1, childInfoList.size());
        childInfoList.clear();
        FileHeader header = (FileHeader) getItem(headerPos);
        if (header != null) {
            header.setCollapsed(false);
        }
    }

    private void collapseChildView(String header, int headerPos) {

        if (headerChildMap.containsKey(header)) {
            headerChildMap.get(header).clear();
        } else {
            headerChildMap.put(header, new ArrayList<FileInfo>());
        }
        // while next pos is a content, remove it
        while (headerPos + 1 < mFiles.size() && getItemViewType(headerPos + 1) == VIEW_TYPE_CONTENT) {
            FileInfo child = getItem(headerPos + 1);
            if (child != null) {
                headerChildMap.get(header).add(child);
                remove(child);
            }
        }
        Utils.safeNotifyItemChanged(this, headerPos);
        notifyItemRangeRemoved(headerPos + 1, headerChildMap.get(header).size());

    }

    public void clickHeader(int myPos) {
        if (getItemViewType(myPos) != VIEW_TYPE_HEADER || !(getItem(myPos) instanceof FileHeader)) {
            return;
        }
        FileHeader header = (FileHeader) getItem(myPos);
        header.setCollapsed(true);
        String myDir = header.getAbsolutePath();
        int childCount = getChildViewCount(myPos);

        if (childCount <= 0) { // no child
            ArrayList<FileInfo> childInfoList = headerChildMap.get(myDir);

            if (childInfoList == null || childInfoList.isEmpty()) {
                return;
            }
            expandChildView(myPos, childInfoList);
            return;
        }

        // has children
        collapseChildView(myDir, myPos);
    }

    protected class HeaderClickListener implements View.OnClickListener {
        HeaderViewHolder viewHolder;

        HeaderClickListener(HeaderViewHolder vh) {
            viewHolder = vh;
        }

        @Override
        public void onClick(View v) {
            int myPos = viewHolder.getAdapterPosition();
            if (myPos == RecyclerView.NO_POSITION) {
                return;
            }
            clickHeader(myPos);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final RecyclerView.ViewHolder holder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                View headerView = inflater.inflate(mHeaderLayoutResourceId, parent, false);
                holder = new HeaderViewHolder(headerView);
                final HeaderViewHolder headHolder = (HeaderViewHolder) holder;
                headHolder.header_view.setOnClickListener(new HeaderClickListener(headHolder));
                break;
            case VIEW_TYPE_CONTENT:
                View contentView;
                if (mSpanCount > 0) {
                    contentView = inflater.inflate(mGridLayoutResourceId, parent, false);
                } else {
                    contentView = inflater.inflate(mListLayoutResourceId, parent, false);
                }
                holder = new ContentViewHolder(contentView);
                ((ContentViewHolder) holder).infoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final int position = holder.getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && mAdapterListener != null) {
                            mAdapterListener.onShowFileInfo(position);
                        }
                    }
                });
                break;
            case VIEW_TYPE_FOOTER:
                View footerView = inflater.inflate(mFooterLayoutResourceId, parent, false);
                holder = new FooterViewHolder(footerView);
                break;
            default:
                throw new IllegalArgumentException("View type " + viewType + " not supported");
        }

        return holder;
    }

    public RecyclerView.Adapter getAdapter() {
        return this;
    }

    @Override
    public long getItemId(int position) {
        final FileInfo file = mFiles.get(position);
        return file.getAbsolutePath().hashCode();
    }

    private int getChildViewCount(int headerPosition) {
        int childCount = 0;
        int i = 1;
        while (headerPosition + i < mFiles.size() && getItemViewType(headerPosition + i) == VIEW_TYPE_CONTENT) {
            childCount++;
            i++;

        }
        return childCount;
    }


    public int findHeader(int contentPosition) {
        int i = contentPosition - 1;
        while (i >= 0) {
            if (getItemViewType(i) == VIEW_TYPE_HEADER) {
                return i;
            }
            i--;
        }
        return i;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        Context context = getContext();
        if (context == null) {
            return;
        }

        switch (getItemViewType(position)) {
            case VIEW_TYPE_FOOTER:
                break;
            case VIEW_TYPE_HEADER:
                onBindViewHolderHeader(holder, position);
                break;
            default:
            case VIEW_TYPE_CONTENT:
                onBindViewHolderContent(holder, position);
                break;
        }
    }

    public void onBindViewHolderHeader(final RecyclerView.ViewHolder holder, int position) {
        final FileInfo file = mFiles.get(position);
        final HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
        String headerTxt = file.getAbsolutePath();
        headerViewHolder.textViewTitle.setText(headerTxt);
        FileHeader header = (FileHeader) file;
        if (header.getCollapsed()) {
            headerViewHolder.foldingBtn.setImageResource(R.drawable.ic_keyboard_arrow_up_black_24dp);
            headerViewHolder.divider.setVisibility(View.VISIBLE);
        } else {
            headerViewHolder.foldingBtn.setImageResource(R.drawable.ic_keyboard_arrow_down_black_24dp);
            headerViewHolder.divider.setVisibility(View.GONE);
        }
    }

    public void onBindViewHolderContent(final RecyclerView.ViewHolder holder, int position) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        final FileInfo file = mFiles.get(position);
        ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
        contentViewHolder.imageViewFileIcon.setImageDrawable(null);
        contentViewHolder.imageViewFileLockIcon.getLayoutParams().width = mLockSize;
        contentViewHolder.imageViewFileLockIcon.getLayoutParams().height = mLockSize;
        contentViewHolder.imageViewFileLockIcon.requestLayout();
        contentViewHolder.itemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        if (position + 1 < getItemCount() && getItemViewType(position + 1) != VIEW_TYPE_HEADER) {
            contentViewHolder.divider.setVisibility(View.VISIBLE);
        } else {
            contentViewHolder.divider.setVisibility(View.GONE);
        }
        if (file.isSecured()) {
            contentViewHolder.imageViewFileLockIcon.setVisibility(View.VISIBLE);
        } else {
            contentViewHolder.imageViewFileLockIcon.setVisibility(View.GONE);
        }

        if (mShowInfoButton) {
            contentViewHolder.imageViewInfoIcon.setVisibility(View.VISIBLE);
            contentViewHolder.infoButton.setVisibility(View.VISIBLE);
        } else {
            contentViewHolder.imageViewInfoIcon.setVisibility(View.GONE);
            contentViewHolder.infoButton.setVisibility(View.GONE);
        }

        String fileTitle = file.getFileName();

        if (mShowFavoriteIndicator && isFavoriteFile(position, file)) {
            fileTitle = fileTitle + " ";
            SpannableString ss = new SpannableString(fileTitle);
            Drawable drawable = getResources().getDrawable(R.drawable.star);
            drawable = drawable.mutate();
            drawable.mutate().setColorFilter(getResources().getColor(R.color.orange), PorterDuff.Mode.SRC_IN);
            drawable.setBounds(0, 0, (int) Utils.convDp2Pix(context, 16), (int) Utils.convDp2Pix(context, 16));

            ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
            ss.setSpan(span, fileTitle.length() - 1, fileTitle.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

            contentViewHolder.textViewFileName.setText(ss);
        } else {
            contentViewHolder.textViewFileName.setText(fileTitle);
        }

        CharSequence description = getFileDescription(file);
        if (description != null && !Utils.isNullOrEmpty(description.toString())) {
            contentViewHolder.textViewFileInfo.setText(description);
            contentViewHolder.textViewFileInfo.setVisibility(View.VISIBLE);
        } else {
            contentViewHolder.textViewFileInfo.setVisibility(View.GONE);
        }
        contentViewHolder.docTextPlaceHolder.setVisibility(View.GONE);
        setFileIcon(holder, position);
    }

    public void setFileIcon(RecyclerView.ViewHolder holder, int position) {
        Context context = getContext();
        if (context == null) {
            return;
        }
        // Document Preview
        final FileInfo file = mFiles.get(position);
        ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
        int type = getFileType(file);
        switch (type) {
            default:
            case BaseFileInfo.FILE_TYPE_FILE:
            case BaseFileInfo.FILE_TYPE_EXTERNAL:
                if (mSpanCount <= 0) {
                    contentViewHolder.imageViewFileIcon.setBackgroundResource(0);
                }
                if (file.isSecured() || file.isPackage()) {
                    // Thumbnail has been generated before, and a placeholder icon should be used
                    if (mSpanCount > 0) {
                        contentViewHolder.imageViewFileIcon.setImageBitmap(mGridLoadingBitmap);
                    } else {
                        contentViewHolder.imageViewFileIcon.setImageBitmap(mListLoadingBitmap);
                    }
                } else {
                    String imagePath = ThumbnailPathCacheManager.getInstance().getThumbnailPath(file.getIdentifier(), mMinXSize, mMinYSize);

                    if (type == BaseFileInfo.FILE_TYPE_EXTERNAL) {
                        String path = file.getAbsolutePath();
                        if (!Utils.isNullOrEmpty(path)) {
                            ContentResolver contentResolver = Utils.getContentResolver(context);
                            if (contentResolver == null) {
                                return;
                            }
                            // it is empty when, for example, retrieve it from Cache in FileManager
                            if (Utils.isDoNotRequestThumbFile(contentResolver, path)) {
                                contentViewHolder.docTextPlaceHolder.setVisibility(View.VISIBLE);
                            } else {
                                contentViewHolder.docTextPlaceHolder.setVisibility(View.GONE);
                            }
                            mThumbnailWorker.tryLoadImageWithUuid(position, file.getIdentifier(),
                                imagePath, contentViewHolder.imageViewFileIcon);
                        }
                    } else if (type == BaseFileInfo.FILE_TYPE_FILE) {
                        if (Utils.isDoNotRequestThumbFile(file.getAbsolutePath())) {
                            contentViewHolder.docTextPlaceHolder.setVisibility(View.VISIBLE);
                        } else {
                            contentViewHolder.docTextPlaceHolder.setVisibility(View.GONE);
                        }
                        mThumbnailWorker.tryLoadImageWithPath(position, file.getAbsolutePath(),
                            imagePath, contentViewHolder.imageViewFileIcon);
                    }
                }
                break;
            case BaseFileInfo.FILE_TYPE_FOLDER:
            case BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER:
                int folderColorRes = Utils.getResourceColor(context, getResources().getString(R.string.folder_color));
                if (folderColorRes == 0) {
                    folderColorRes = android.R.color.black;
                }
                if (mSpanCount > 0) {
                    contentViewHolder.imageViewFileIcon.setImageResource(R.drawable.ic_folder_black_24dp);
                    contentViewHolder.imageViewFileIcon.getDrawable().mutate().setColorFilter(context.getResources().getColor(folderColorRes), PorterDuff.Mode.SRC_IN);
                } else {
                    contentViewHolder.imageViewFileIcon.setImageResource(R.drawable.ic_folder_black_24dp);
                    contentViewHolder.imageViewFileIcon.getDrawable().mutate().setColorFilter(context.getResources().getColor(folderColorRes), PorterDuff.Mode.SRC_IN);
                    contentViewHolder.imageViewFileIcon.setBackgroundResource(0);
                }
                break;
            case BaseFileInfo.FILE_TYPE_EXTERNAL_ROOT:
                if (mSpanCount > 0) {
                    contentViewHolder.imageViewFileIcon.setImageResource(R.drawable.ic_sd_storage_black_24dp);
                } else {
                    contentViewHolder.imageViewFileIcon.setImageResource(R.drawable.ic_sd_storage_black_24dp);
                    contentViewHolder.imageViewFileIcon.setBackgroundResource(0);
                }
                break;
        }
    }

    public void loadPreviewForCloudFiles(FileInfo file, int position, ContentViewHolder contentViewHolder, int loadingSmallRes, int loadingLargeRes) {
        if (mSpanCount > 0) {
            contentViewHolder.imageViewFileIcon.setImageResource(loadingLargeRes);
        } else {
            contentViewHolder.imageViewFileIcon.setImageResource(loadingSmallRes);
            contentViewHolder.imageViewFileIcon.setBackgroundResource(0);
        }
        if (!file.isSecured() && !file.isPackage()) {
            String imagePath;
            // Try to load thumbnail preview for file
            imagePath = RecentlyUsedCache.getBitmapPathIfExists(file.getAbsolutePath());
            if (Utils.isNullOrEmpty(imagePath)) { // Empty image paths are not considered by ThumbnailWorker
                imagePath = null;
            }
            if (null != imagePath) {
                if (mSpanCount <= 0) {
                    contentViewHolder.imageViewFileIcon.setBackgroundResource(0);
                }
                mThumbnailWorker.tryLoadImageWithUuid(position, file.getAbsolutePath(),
                    imagePath, contentViewHolder.imageViewFileIcon);
            }
        }
    }

    public int getFileType(FileInfo file) {
        return file.getFileType();
    }

    public CharSequence getFileDescription(FileInfo file) {
        Context context = getContext();
        if (context == null) {
            return "";
        }

        String description;

        if (isInSearchMode()) {
            if (file.getFileType() == BaseFileInfo.FILE_TYPE_EXTERNAL || file.getFileType() == BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER) {
                ExternalFileInfo fileInfo = Utils.buildExternalFile(context, Uri.parse(file.getAbsolutePath()));
                if (fileInfo == null) {
                    return "";
                }
                description = fileInfo.getParentRelativePath();
                if (description == null) {
                    description = "";
                }
                description += "/" + fileInfo.getFileName();
            } else {
                description = FilenameUtils.getPath(file.getAbsolutePath());
            }
        } else if (!(mSpanCount > 0) &&
            (file.getFileType() == BaseFileInfo.FILE_TYPE_FILE || file.getFileType() == BaseFileInfo.FILE_TYPE_EXTERNAL)) {
            description = file.getModifiedDate() + "   " + file.getSizeInfo();
        } else {
            description = file.getModifiedDate();
        }
        return description;
    }

    @Override
    public void updateSpanCount(int count) {
        Resources resources = getResources();
        if (resources == null) {
            return;
        }

        if (count > 0) {
            mMinXSize = mRecyclerViewWidth / count;
            mMinYSize = (int) (mMinXSize * 1.29);
            mLockSize = resources.getDimensionPixelSize(R.dimen.thumbnail_lock_size_medium);
            mLoadingBitmap = mGridLoadingBitmap;
            if (mMinXSize == 0 || mMinYSize == 0) {
                mMinXSize = resources.getDimensionPixelSize(R.dimen.thumbnail_height_large);
                mMinYSize = resources.getDimensionPixelSize(R.dimen.thumbnail_height_large);
            }
        } else {
            mMinXSize = resources.getDimensionPixelSize(R.dimen.list_thumbnail_width);
            mMinYSize = resources.getDimensionPixelSize(R.dimen.list_thumbnail_height);
            mLockSize = resources.getDimensionPixelSize(R.dimen.thumbnail_lock_size_list);
            mLoadingBitmap = mGridLoadingBitmap;
        }
        if (mThumbnailWorker != null) {
            mThumbnailWorker.setMinXSize(mMinXSize);
            mThumbnailWorker.setMinYSize(mMinYSize);
            mThumbnailWorker.setLoadingBitmap(mLoadingBitmap);
        }
        mSpanCount = count;
    }

    public void setShowInfoButton(boolean show) {
        mShowInfoButton = show;
    }

    public void setShowFavoriteIndicator(boolean show) {
        mShowFavoriteIndicator = show;
    }

    protected boolean isFavoriteFile(int position, FileInfo file) {
        return false;
    }

    public boolean isHeader(int position) {
        return false;
    }

    public void updateMainViewWidth(int width) {
        mRecyclerViewWidth = width;
    }

    @Override
    public void onThumbnailReady(int result, final int position, String iconPath, String identifier) {
        final FileInfo file = getItem(position); // getItem will perform bounds checks
        if (file == null || !identifier.contains(file.getAbsolutePath())) {
            return;
        }

        boolean canAdd = true;

        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR) {
            // avoid flashing caused by the callback
            file.setIsSecured(true);
            canAdd = false;
        }
        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PACKAGE_ERROR) {
            // avoid flashing caused by the callback
            file.setIsPackage(true);
            canAdd = false;
        }
        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_NOT_FOUNT) {
            // create this file instead
            mThumbnailWorker.tryLoadImageFromFilter(position, identifier, file.getAbsolutePath());
            return;
        } else if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_CANCEL ||
            result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PREVIOUS_CRASH ||
            result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_POSTPONED) {
            canAdd = false;
        }

        // adds path to local cache for later access
        if (canAdd) {
            ThumbnailPathCacheManager.getInstance().putThumbnailPath(identifier,
                iconPath, mMinXSize, mMinYSize);
        }

        // update this position only
        if (getRecyclerView() != null) {
            RecyclerView.ViewHolder holder = getRecyclerView().findViewHolderForLayoutPosition(position);
            if (holder != null) {
                updateViewHolder(holder, position, result, iconPath);
            } else {
                // if cannot update view holder at the moment, the next time the view holder
                // is available its thumbnail should be generated; otherwise it will be loaded
                // with an empty thumbnail
                getRecyclerView().post((new Runnable() {
                    @Override
                    public void run() {
                        if (position < getItemCount()) {
                            Utils.safeNotifyItemChanged(BaseFileAdapter.this, position);
                        }
                    }
                }));
            }
        }
    }

    private void updateViewHolder(RecyclerView.ViewHolder holder, int position, int result, String iconPath) {
        if (!(holder instanceof ContentViewHolder)) {
            return;
        }
        final ContentViewHolder contentViewHolder = (ContentViewHolder) holder;

        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR) {
            contentViewHolder.imageViewFileLockIcon.setVisibility(View.VISIBLE);
        } else {
            contentViewHolder.imageViewFileLockIcon.setVisibility(View.GONE);
        }
        if (result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_FAILURE ||
            result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_SECURITY_ERROR ||
            result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PACKAGE_ERROR ||
            result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_CANCEL ||
            result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_PREVIOUS_CRASH ||
            result == PreviewHandler.DOCUMENT_PREVIEW_RESULT_POSTPONED) {
            // Thumbnail has been generated before, and a placeholder icon should be used
            if (mSpanCount > 0) {
                contentViewHolder.imageViewFileIcon.setImageBitmap(mGridLoadingBitmap);
            } else {
                contentViewHolder.imageViewFileIcon.setImageBitmap(mListLoadingBitmap);
            }
        } else {
            String filePath = mFiles.get(position).getAbsolutePath();
            mThumbnailWorker.tryLoadImageWithPath(position, filePath, iconPath, contentViewHolder.imageViewFileIcon);
        }
    }

    public void cancelAllThumbRequests(boolean removePreviewHandler) {
        abortCancelThumbRequests();
        if (removePreviewHandler) {
            mThumbnailWorker.removePreviewHandler();
        }
        mThumbnailWorker.cancelAllThumbRequests();
    }

    public void cancelAllThumbRequests() {
        cancelAllThumbRequests(false);
    }

    public void abortCancelThumbRequests() {
        mThumbnailWorker.abortCancelTask();
    }

    public void cleanupResources() {
        mThumbnailWorker.cleanupResources();
    }

    public void evictFromMemoryCache(String uuid) {
        mThumbnailWorker.evictFromMemoryCache(uuid);
    }

    public boolean isInSearchMode() {
        return mIsInSearchMode;
    }

    public void setInSearchMode(boolean isInSearchMode) {
        mIsInSearchMode = isInSearchMode;
    }

    @Override
    public Filter getFilter() {
        return getDerivedFilter();
    }

    public FileListFilter getDerivedFilter() {
        if (mFilter == null) {
            mFilter = new FileListFilter<>(mOriginalFiles, this, mOriginalFilesLock);
        }
        return mFilter;
    }

    @Override
    public void onFilterResultsPublished(final ArrayList<FileInfo> filteredFiles, final int resultCode) {

        mFiles = filteredFiles;
        int code = resultCode;
        try {
            notifyDataSetChanged();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            code = FileListFilter.FILTER_RESULT_FAILURE;
        }

        if (mAdapterListener != null) {
            mAdapterListener.onFilterResultsPublished(code);
        }

    }

    public HashMap<String, FileHeader> getFileHeader() {
        return mOriginalHeaders;
    }

    public void setFileHeader(HashMap<String, FileHeader> fileHeaders) {
        if (mOriginalHeaders == null) {
            mOriginalHeaders = new HashMap<>();
        }
        mOriginalHeaders.clear();

        mOriginalHeaders.putAll(fileHeaders);
    }
}
