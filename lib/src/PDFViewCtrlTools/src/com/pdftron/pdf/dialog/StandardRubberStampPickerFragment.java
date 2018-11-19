//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.dialog;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFDraw;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.adapter.StandardRubberStampAdapter;
import com.pdftron.pdf.asynctask.CreateBitmapFromCustomStampTask;
import com.pdftron.pdf.interfaces.OnRubberStampSelectedListener;
import com.pdftron.pdf.model.StandardStampOption;
import com.pdftron.pdf.model.StandardStampPreviewAppearance;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import com.pdftron.sdf.Obj;

import java.io.InputStream;
import java.lang.ref.WeakReference;

public class StandardRubberStampPickerFragment extends Fragment {

    public final static String TAG = StandardRubberStampPickerFragment.class.getName();

    private StandardStampPreviewAppearance[] mStandardStampPreviewAppearances;
    private OnRubberStampSelectedListener mOnRubberStampSelectedListener;
    private AttachStampsToAdapterTask mAttachStampsToAdapterTask;

    public static StandardRubberStampPickerFragment newInstance(StandardStampPreviewAppearance[] standardStampPreviewAppearances) {
        StandardRubberStampPickerFragment fragment = new StandardRubberStampPickerFragment();
        Bundle bundle = new Bundle();
        StandardStampPreviewAppearance.putStandardStampAppearancesToBundle(bundle, standardStampPreviewAppearances);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_standard_rubber_stamp_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            mStandardStampPreviewAppearances = StandardStampPreviewAppearance.getStandardStampAppearancesFromBundle(bundle);
        }

        SimpleRecyclerView recyclerView = view.findViewById(R.id.stamp_list);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(recyclerView);
        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                if (mOnRubberStampSelectedListener != null
                    && mStandardStampPreviewAppearances != null && mStandardStampPreviewAppearances.length > position) {
                    String name = mStandardStampPreviewAppearances[position].text;
                    if (mStandardStampPreviewAppearances[position].previewAppearance == null) {
                        mOnRubberStampSelectedListener.onRubberStampSelected(name);
                    } else {
                        Obj stampObj = StandardStampOption.getStandardStampObj(view.getContext(), name);
                        mOnRubberStampSelectedListener.onRubberStampSelected(stampObj);
                    }
                }
            }
        });

        Context context = view.getContext();
        // set the background color of PDFDraw to window background
        int bgColor = Utils.isDeviceNightMode(context) ? Color.BLACK : Color.WHITE;
        // only handle when the background is color
        if (getView() != null && getView().getBackground() instanceof ColorDrawable) {
            Drawable background = getView().getBackground();
            bgColor = ((ColorDrawable) background).getColor();
        } else {
            TypedValue a = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.windowBackground, a, true);
            if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                bgColor = a.data;
            }
        }

        mAttachStampsToAdapterTask = new AttachStampsToAdapterTask(context,
            recyclerView, (ProgressBar) view.findViewById(R.id.progress_bar),
            mStandardStampPreviewAppearances, bgColor);
        mAttachStampsToAdapterTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAttachStampsToAdapterTask != null) {
            mAttachStampsToAdapterTask.cancel(true);
        }
    }

    /**
     * Sets the listener to {@link OnRubberStampSelectedListener}.
     *
     * @param listener The listener
     */
    public void setOnRubberStampSelectedListener(OnRubberStampSelectedListener listener) {
        mOnRubberStampSelectedListener = listener;
    }

    private static class AttachStampsToAdapterTask extends CustomAsyncTask<Void, Void, Bitmap[]> {

        WeakReference<RecyclerView> mRecyclerViewRef;
        WeakReference<ProgressBar> mProgressBarRef;
        StandardStampPreviewAppearance[] mStandardStampPreviewAppearances;
        int mBgColor;
        int mHeight;

        AttachStampsToAdapterTask(Context context, RecyclerView recyclerView, ProgressBar progressBar, StandardStampPreviewAppearance[] standardStampPreviewAppearances, int bgColor) {
            super(context);
            mRecyclerViewRef = new WeakReference<>(recyclerView);
            mProgressBarRef = new WeakReference<>(progressBar);
            mStandardStampPreviewAppearances = standardStampPreviewAppearances;
            mBgColor = bgColor;
            mHeight = context.getResources().getDimensionPixelSize(R.dimen.stamp_image_height);
        }

        @Override
        protected Bitmap[] doInBackground(Void... voids) {
            int count = mStandardStampPreviewAppearances.length;
            Bitmap[] bitmaps = new Bitmap[count];
            boolean progressShown = false;
            for (int i = 0; i < count && !isCancelled(); ++i) {
                String name = mStandardStampPreviewAppearances[i].text;
                if (mStandardStampPreviewAppearances[i].previewAppearance == null) {
                    // seems this is fast procedure, so no point to save/restore it on/from disk
                    bitmaps[i] = getStandardStampBitmapFromPdf(getContext(), mStandardStampPreviewAppearances[i].text, mBgColor);
                    continue;
                }

                if (StandardStampOption.checkStandardStamp(getContext(), name)) {
                    bitmaps[i] = StandardStampOption.getStandardStampBitmap(getContext(), name);
                    continue;
                }

                if (!progressShown) {
                    publishProgress();
                    progressShown = true;
                }

                StandardStampOption stampOption = new StandardStampOption(
                    mStandardStampPreviewAppearances[i].text, null,
                    mStandardStampPreviewAppearances[i].previewAppearance.bgColorStart,
                    mStandardStampPreviewAppearances[i].previewAppearance.bgColorEnd,
                    mStandardStampPreviewAppearances[i].previewAppearance.textColor,
                    mStandardStampPreviewAppearances[i].previewAppearance.borderColor,
                    mStandardStampPreviewAppearances[i].previewAppearance.fillOpacity,
                    mStandardStampPreviewAppearances[i].pointLeft, false);

                try {
                    Bitmap bitmap = CreateBitmapFromCustomStampTask.createBitmapFromCustomStamp(stampOption, mHeight, mHeight);
                    if (bitmap != null) {
                        StandardStampOption.saveStandardStamp(getContext(), name, stampOption, bitmap);
                        bitmaps[i] = bitmap;
                    }
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
            return bitmaps;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            ProgressBar progressBar = mProgressBarRef.get();
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(Bitmap[] bitmaps) {
            super.onPostExecute(bitmaps);
            RecyclerView recyclerView = mRecyclerViewRef.get();
            if (recyclerView != null && bitmaps != null) {
                recyclerView.setAdapter(new StandardRubberStampAdapter(bitmaps));
            }
            ProgressBar progressBar = mProgressBarRef.get();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }

        @Nullable
        private Bitmap getStandardStampBitmapFromPdf(@Nullable Context context, @NonNull String stampLabel, int bgColor) {
            if (context == null || Utils.isNullOrEmpty(stampLabel)) {
                return null;
            }

            InputStream fis = null;
            PDFDoc template = null;
            PDFDraw pdfDraw = null;
            try {
                fis = context.getResources().openRawResource(R.raw.stamps_icons);
                template = new PDFDoc(fis);

                pdfDraw = new PDFDraw();

                int r = Color.red(bgColor);
                int g = Color.green(bgColor);
                int b = Color.blue(bgColor);
                pdfDraw.setDefaultPageColor((byte) r, (byte) g, (byte) b);

                int pageCount = template.getPageCount();
                int maxWidth = (int) Utils.convDp2Pix(context, 200);
                int marginWidth = (int) Utils.convDp2Pix(context, 175);
                for (int pageNum = 1; pageNum <= pageCount; ++pageNum) {
                    if (stampLabel.equals(template.getPageLabel(pageNum).getPrefix())) {
                        Page page = template.getPage(pageNum);
                        int width = (int) Math.min(maxWidth, (mHeight * page.getPageWidth() / page.getPageHeight() + .5));
                        if (width > marginWidth && width < maxWidth) {
                            width = maxWidth;
                        }
                        pdfDraw.setImageSize(width, mHeight, false);
                        return pdfDraw.getBitmap(page);
                    }
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                Utils.closeQuietly(template);
                Utils.closeQuietly(fis);
                if (pdfDraw != null) {
                    try {
                        pdfDraw.destroy();
                    } catch (PDFNetException ignored) {
                    }
                }
            }
            return null;
        }

    }

}
