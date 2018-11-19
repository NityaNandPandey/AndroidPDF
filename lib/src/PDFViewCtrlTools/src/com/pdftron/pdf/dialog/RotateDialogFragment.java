package com.pdftron.pdf.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import android.os.AsyncTask;
import com.pdftron.pdf.utils.ImageMemoryCache;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * A rotate dialog fragment allows users to rotate pages of the document by 90, 180 and 270 degree
 * with ease while they see the thumbnail of the current page as they rotate pages.
 */
public class RotateDialogFragment extends DialogFragment implements
    PDFViewCtrl.ThumbAsyncListener {

    private static final String TAG = RotateDialogFragment.class.getName();

    private PDFViewCtrl mPdfViewCtrl;
    private int mCurrentPage;
    private int mRotationDelta = 0;
    private LinearLayout mThumbImgParent;
    private ImageView mThumbImg;
    private LinearLayout mThumbImgVertParent;
    private ImageView mThumbImgVert;
    private TextView mRotationDeltaTextView;
    private ProgressBar mProgressBar;

    private boolean mDebug = false;

    private enum ApplyMode {
        CurrentPage,
        AllPages,
        EvenPages,
        OddPages,
    }

    private ApplyMode mCurrentApplyMode = ApplyMode.CurrentPage;

    public RotateDialogFragment() {

    }

    /**
     * Use this factory method to create a new instance of this fragment.
     *
     * @return A new instance of fragment RotateDialogFragment.
     */
    public static RotateDialogFragment newInstance() {
        return new RotateDialogFragment();
    }

    /**
     * Sets the {@link PDFViewCtrl}
     *
     * @param pdfViewCtrl The PDFViewCtrl
     *
     * @return This class
     */
    public RotateDialogFragment setPdfViewCtrl(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        mCurrentPage = pdfViewCtrl.getCurrentPage();
        return this;
    }

    /**
     * The overloaded implementation of {@link DialogFragment#onCreateDialog(Bundle)}.
     **/
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.tools_dialog_rotate, null);
        builder.setView(view);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                performRotation();
                dismiss();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        mThumbImgParent = (LinearLayout) view.findViewById(R.id.rotate_thumbnail_parent);
        mThumbImg = (ImageView) view.findViewById(R.id.rotate_thumbnail);
        mThumbImgVert = (ImageView) view.findViewById(R.id.rotate_thumbnail_vert);
        mThumbImgVertParent = (LinearLayout) view.findViewById(R.id.rotate_thumbnail_vert_parent);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        mRotationDeltaTextView = (TextView) view.findViewById(R.id.rotation_delta_text_view);

        ImageButton cwButton = (ImageButton) view.findViewById(R.id.tools_dialog_rotate_clockwise_btn);
        cwButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRotationDelta = (mRotationDelta + 1) % 4;
                if (mRotationDelta == 1 || mRotationDelta == 3) {
                    mThumbImgVert.setRotation(mRotationDelta == 1 ? 0.0f : 180.0f);
                    mThumbImgVertParent.setVisibility(View.VISIBLE);
                    mThumbImgParent.setVisibility(View.INVISIBLE);
                } else {
                    mThumbImg.setRotation(mRotationDelta == 0 ? 0.0f : 180.0f);
                    mThumbImgParent.setVisibility(View.VISIBLE);
                    mThumbImgVertParent.setVisibility(View.INVISIBLE);
                }

                updateRotationDeltaText();
            }
        });
        ImageButton ccwButton = (ImageButton) view.findViewById(R.id.tools_dialog_rotate_counter_clockwise_btn);
        ccwButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRotationDelta -= 1;
                if (mRotationDelta < 0) {
                    mRotationDelta = 4 + mRotationDelta;
                }
                if (mRotationDelta == 1 || mRotationDelta == 3) {
                    mThumbImgVert.setRotation(mRotationDelta == 1 ? 0.0f : 180.0f);
                    mThumbImgVertParent.setVisibility(View.VISIBLE);
                    mThumbImgParent.setVisibility(View.INVISIBLE);
                } else {
                    mThumbImg.setRotation(mRotationDelta == 0 ? 0.0f : 180.0f);
                    mThumbImgParent.setVisibility(View.VISIBLE);
                    mThumbImgVertParent.setVisibility(View.INVISIBLE);
                }

                updateRotationDeltaText();
            }
        });

        Spinner rotateModeSpinner = (Spinner) view.findViewById(R.id.rotate_mode_spinner);
        ArrayAdapter<String> rotateModeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item);
        rotateModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (ApplyMode i : ApplyMode.values()) {
            String str = null;
            if (i == ApplyMode.CurrentPage) {
                str = getString(R.string.dialog_rotate_current_page, mCurrentPage);
            } else if (i == ApplyMode.AllPages) {
                str = getString(R.string.dialog_rotate_all_pages);
            } else if (i == ApplyMode.EvenPages) {
                str = getString(R.string.dialog_rotate_even_pages);
            } else if (i == ApplyMode.OddPages) {
                str = getString(R.string.dialog_rotate_odd_pages);
            }
            rotateModeAdapter.add(str);
        }

        rotateModeSpinner.setAdapter(rotateModeAdapter);
        rotateModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentApplyMode = ApplyMode.values()[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        try {
            if (mPdfViewCtrl != null) {
                mPdfViewCtrl.addThumbAsyncListener(this);
                mPdfViewCtrl.getThumbAsync(mCurrentPage);
            }
        } catch (PDFNetException e) {

        }

        return builder.create();
    }

    /**
     * The overloaded implementation of {@link DialogFragment#onDestroy()}.
     **/
    @Override
    public void onDestroy() {
        if (mPdfViewCtrl != null) {
            mPdfViewCtrl.removeThumbAsyncListener(this);
        }

        super.onDestroy();
    }

    /**
     * The overloaded implementation of {@link PDFViewCtrl.ThumbAsyncListener#onThumbReceived(int, int[], int, int)}.
     **/
    @Override
    public void onThumbReceived(int page, int[] buf, int width, int height) {
        new LoadThumbnailTask(mThumbImgParent.getLayoutParams(), page, buf, width, height).execute();
    }

    private void updateRotationDeltaText() {
        String degree = "";
        switch (mRotationDelta) {
            case 0:
                degree = "0";
                break;
            case 1:
                degree = "90";
                break;
            case 2:
                degree = "180";
                break;
            case 3:
                degree = "270";
                break;
        }
        degree += "\u00b0";
        mRotationDeltaTextView.setText(degree);
    }

    private void performRotation() {
        if (mPdfViewCtrl == null) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            int pageCount = mPdfViewCtrl.getDoc().getPageCount();
            Page[] pages = null;
            List<Integer> pageList = new ArrayList<>();
            if (mCurrentApplyMode == ApplyMode.CurrentPage) {
                pages = new Page[1];
                pageList.add(mCurrentPage);
                pages[0] = mPdfViewCtrl.getDoc().getPage(mCurrentPage);
            } else if (mCurrentApplyMode == ApplyMode.AllPages) {
                pages = new Page[pageCount];
                for (int i = 1; i <= pageCount; i++) {
                    pageList.add(i);
                    pages[i - 1] = mPdfViewCtrl.getDoc().getPage(i);
                }
            } else if (mCurrentApplyMode == ApplyMode.OddPages) {
                int newPageCount = (int) Math.ceil(((double) pageCount) / 2.0);
                pages = new Page[newPageCount];
                int arrayIndex = 0;
                for (int i = 1; i <= pageCount; i += 2) {
                    pageList.add(i);
                    pages[arrayIndex++] = mPdfViewCtrl.getDoc().getPage(i);
                }
            } else if (mCurrentApplyMode == ApplyMode.EvenPages) {
                if (pageCount >= 2) {
                    int newPageCount = (int) Math.floor(((double) pageCount) / 2.0);
                    pages = new Page[newPageCount];
                    int arrayIndex = 0;
                    for (int i = 2; i <= pageCount; i += 2) {
                        pageList.add(i);
                        pages[arrayIndex++] = mPdfViewCtrl.getDoc().getPage(i);
                    }
                }
            }
            if (pages != null) {
                for (Page page : pages) {
                    page.setRotation((page.getRotation() + mRotationDelta) % 4);
                }
            }

            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            if (toolManager != null) {
                toolManager.raisePagesRotated(pageList);
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
            try {
                mPdfViewCtrl.updatePageLayout();
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    private class LoadThumbnailTask extends AsyncTask<Void, Void, Pair<BitmapDrawable, BitmapDrawable>> {

        private final ViewGroup.LayoutParams mLParams;
        private final int mPage;

        private int mWidth;
        private int mHeight;
        private int[] mBuffer;

        LoadThumbnailTask(ViewGroup.LayoutParams lParams, int page, int[] buffer, int width, int height) {
            this.mLParams = lParams;
            this.mPage = page;
            this.mBuffer = buffer;
            this.mWidth = width;
            this.mHeight = height;
        }

        @Override
        protected Pair<BitmapDrawable, BitmapDrawable> doInBackground(Void... voids) {
            Bitmap bitmap;
            BitmapDrawable drawable1;
            BitmapDrawable drawable2;

            Pair<BitmapDrawable, BitmapDrawable> retPair = null;
            try {
                if (mBuffer != null && mBuffer.length > 0) {
                    ImageMemoryCache imageMemoryCache = ImageMemoryCache.getInstance();
                    bitmap = imageMemoryCache.getBitmapFromReusableSet(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                    if (bitmap == null) {
                        bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                    }
                    bitmap.setPixels(mBuffer, 0, mWidth, 0, 0, mWidth, mHeight);
                    Pair<Bitmap, Bitmap> bitmapPair = scaleBitmap(mLParams, bitmap);
                    if (bitmapPair == null) {
                        return null;
                    }

                    drawable1 = new BitmapDrawable(getContext().getResources(), bitmapPair.first);
                    drawable2 = new BitmapDrawable(getContext().getResources(), bitmapPair.second);

                    retPair = new Pair<>(drawable1, drawable2);
                    if (mDebug)
                        Log.d(TAG, "doInBackground - finished work for page: " + Integer.toString(mPage));
                } else {
                    if (mDebug)
                        Log.d(TAG, "doInBackground - Buffer is empty for page: " + Integer.toString(mPage));
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } catch (OutOfMemoryError oom) {
                Utils.manageOOM(getContext(), mPdfViewCtrl);
            }

            return retPair;
        }

        @Override
        protected void onPostExecute(Pair<BitmapDrawable, BitmapDrawable> result) {
            if (isCancelled()) {
                return;
            }

            if (result != null) {
                if (mDebug)
                    Log.d(TAG, "onPostExecute - setting bitmap for page: " + Integer.toString(mPage));
                mThumbImgVert.setImageDrawable(result.second);

                mThumbImg.setImageDrawable(result.first);
                mThumbImgParent.setVisibility(View.VISIBLE);

                mProgressBar.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onCancelled(Pair<BitmapDrawable, BitmapDrawable> value) {

        }
    }

    private Pair<Bitmap, Bitmap> scaleBitmap(ViewGroup.LayoutParams parentParams, Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        // Get bitmap dimensions
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        // Determine how much to scale: the dimension requiring less scaling is
        // closer to the its side. This way the image always stays inside the
        // bounding box AND either x/y axis touches it.
        float xScale = ((float) parentParams.width) / bitmapWidth;
        float yScale = ((float) parentParams.height) / bitmapHeight;
        float scale = (xScale > yScale) ? xScale : yScale;

        // Create a matrix for the scaling (same scale amount in both directions)
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        try {
            // Create a new bitmap that is scaled to the bounding box
            Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, matrix, true);

            // Create a matrix for the scaling and rotated image (same scale amount in both directions)
            matrix = new Matrix();
            matrix.postRotate(90.0f);
            matrix.postScale(scale, scale);

            // Create a new bitmap that is scaled to the bounding box
            Bitmap scaledBitmapVert = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, matrix, true);

            return new Pair<>(scaledBitmap, scaledBitmapVert);
        } catch (OutOfMemoryError oom) {
            Utils.manageOOM(getContext(), mPdfViewCtrl);
            return null;
        }
    }

    /**
     * @hide
     */
    public void setDebug(boolean debug) {
        mDebug = debug;
    }
}
