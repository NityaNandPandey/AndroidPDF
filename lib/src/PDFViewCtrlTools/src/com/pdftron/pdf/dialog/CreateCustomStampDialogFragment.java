//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;

import com.pdftron.pdf.adapter.CustomStampColorAdapter;
import com.pdftron.pdf.asynctask.CreateBitmapFromCustomStampTask;
import com.pdftron.pdf.controls.CustomSizeDialogFragment;
import com.pdftron.pdf.interfaces.OnCustomStampChangedListener;
import com.pdftron.pdf.model.CustomStampOption;
import com.pdftron.pdf.model.CustomStampPreviewAppearance;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import com.pdftron.sdf.Obj;

import java.lang.ref.WeakReference;

public class CreateCustomStampDialogFragment extends CustomSizeDialogFragment implements CreateBitmapFromCustomStampTask.OnCustomStampCreatedCallback {

    public final static String TAG = CreateCustomStampDialogFragment.class.getName();

    private final static String BUNDLE_EDIT_INDEX = "edit_index";
    private final static int COLOR_COLUMN_COUNT = 3;

    private Shape mShapeSelected = Shape.ROUNDED_RECTANGLE;
    private CustomStampPreviewAppearance[] mCustomStampPreviewAppearances;
    private int mEditIndex; // -1 if not in edit mode
    private AppCompatEditText mStampText;
    private AppCompatImageView mStampPreview;
    private SwitchCompat mDateSwitch, mTimeSwitch;
    private ImageButton mPointingLeftImage, mPointingRightImage, mRoundedRectangleImage;
    private CustomStampColorAdapter mCustomStampColorAdapter;
    private CreateBitmapFromCustomStampTask mCreateBitmapFromCustomStampTask;
    private OnCustomStampChangedListener mOnCustomStampChangedListener;

    /**
     * Creates an instance of this class.
     *
     * @param customStampPreviewAppearances An array of custom rubber stamp preview appearance
     * @return An instance of this class
     */
    public static CreateCustomStampDialogFragment newInstance(CustomStampPreviewAppearance[] customStampPreviewAppearances) {
        return newInstance(customStampPreviewAppearances, -1);
    }

    /**
     * Creates an instance of this class.
     *
     * @param customStampPreviewAppearances An array of custom rubber stamp preview appearance
     * @param editIndex                     the index of saved custom rubber stamp to be edited;
     *                                      -1 for creating a new custom rubber stamp
     * @return An instance of this class
     */
    public static CreateCustomStampDialogFragment newInstance(CustomStampPreviewAppearance[] customStampPreviewAppearances, int editIndex) {
        CreateCustomStampDialogFragment fragment = new CreateCustomStampDialogFragment();
        Bundle bundle = new Bundle();
        CustomStampPreviewAppearance.putCustomStampAppearancesToBundle(bundle, customStampPreviewAppearances);
        bundle.putInt(BUNDLE_EDIT_INDEX, editIndex);
        fragment.setArguments(bundle);
        return fragment;
    }

    /**
     * Sets the listener to {@link OnCustomStampChangedListener}.
     *
     * @param listener The listener
     */
    public void setOnCustomStampChangedListener(OnCustomStampChangedListener listener) {
        mOnCustomStampChangedListener = listener;
    }

    @Override
    public void onCustomStampCreated(@Nullable Bitmap bitmap) {
        if (mStampPreview != null) {
            mStampPreview.setImageBitmap(bitmap);
            if (bitmap != null) {
                ViewGroup.LayoutParams lp = mStampPreview.getLayoutParams();
                lp.height = bitmap.getHeight();
                lp.width = bitmap.getWidth();
                mStampPreview.setLayoutParams(lp);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_custom_rubber_stamp_dialog, container, false);

        Toolbar toolbar = view.findViewById(R.id.create_stamp_dialog_toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        toolbar.inflateMenu(R.menu.save);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_save) {
                    saveCustomStamp();
                    dismiss();
                    return true;
                }
                return false;
            }

        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = view.getContext();

        Bundle bundle = getArguments();
        mCustomStampPreviewAppearances = CustomStampPreviewAppearance.getCustomStampAppearancesFromBundle(bundle);
        if (mCustomStampPreviewAppearances == null || mCustomStampPreviewAppearances.length == 0) {
            return;
        }
        mEditIndex = bundle.getInt(BUNDLE_EDIT_INDEX, -1);

        mStampText = view.findViewById(R.id.stamp_text);
        mStampText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                showPreview();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showPreview();
            }
        };
        View.OnClickListener onShapeClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == mPointingLeftImage) {
                    mShapeSelected = Shape.POINTING_LEFT;
                } else if (v == mPointingRightImage) {
                    mShapeSelected = Shape.POINTING_RIGHT;
                } else if (v == mRoundedRectangleImage) {
                    mShapeSelected = Shape.ROUNDED_RECTANGLE;
                }
                showPreview();
            }
        };
        mDateSwitch = view.findViewById(R.id.date_switch);
        mDateSwitch.setOnCheckedChangeListener(checkedChangeListener);
        mTimeSwitch = view.findViewById(R.id.time_switch);
        mTimeSwitch.setOnCheckedChangeListener(checkedChangeListener);
        mPointingLeftImage = view.findViewById(R.id.pointing_left_shape);
        mPointingLeftImage.setOnClickListener(onShapeClickListener);
        mPointingRightImage = view.findViewById(R.id.pointing_right_shape);
        mPointingRightImage.setOnClickListener(onShapeClickListener);
        mRoundedRectangleImage = view.findViewById(R.id.rounded_rectangle_shape);
        mRoundedRectangleImage.setOnClickListener(onShapeClickListener);
        mStampPreview = view.findViewById(R.id.stamp_preview);

        SimpleRecyclerView recyclerView = view.findViewById(R.id.stamp_color_recycler);
        recyclerView.initView(COLOR_COLUMN_COUNT, 0);
        int len = mCustomStampPreviewAppearances.length;
        int[] bgColors = new int[len];
        int[] textColors = new int[len];
        for (int i = 0; i < len; ++i) {
            bgColors[i] = mCustomStampPreviewAppearances[i].bgColorMiddle;
            textColors[i] = mCustomStampPreviewAppearances[i].textColor;
        }
        mCustomStampColorAdapter = new CustomStampColorAdapter(bgColors, textColors);
        recyclerView.setAdapter(mCustomStampColorAdapter);
        recyclerView.addItemDecoration(new CustomSpaceItemDecoration(context));

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(recyclerView);
        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                CustomStampColorAdapter adapter = (CustomStampColorAdapter) parent.getAdapter();
                adapter.select(position);
                Utils.safeNotifyDataSetChanged(adapter);
                showPreview();
            }
        });

        if (mEditIndex >= 0) {
            Obj stampObj = CustomStampOption.getCustomStampObj(context, mEditIndex);
            if (stampObj == null) {
                mEditIndex = -1;
            } else {
                // reload custom rubber stamp option
                try {
                    CustomStampOption customStamp = new CustomStampOption(stampObj);
                    // tries to reload custom rubber stamp option
                    mStampText.setText(customStamp.text);
                    if (customStamp.isPointingLeft) {
                        mShapeSelected = Shape.POINTING_LEFT;
                    } else if (customStamp.isPointingRight) {
                        mShapeSelected = Shape.POINTING_RIGHT;
                    } else {
                        mShapeSelected = Shape.ROUNDED_RECTANGLE;
                    }
                    int index = bgColors.length - 1;
                    for (; index > 0; --index) {
                        if (customStamp.textColor == textColors[index]
                            && customStamp.bgColorStart == mCustomStampPreviewAppearances[index].bgColorStart
                            && customStamp.bgColorEnd == mCustomStampPreviewAppearances[index].bgColorEnd) {
                            break;
                        }
                    }
                    mTimeSwitch.setChecked(customStamp.hasTimeStamp());
                    mDateSwitch.setChecked(customStamp.hasDateStamp());
                    mCustomStampColorAdapter.select(index);
                } catch (Exception e) {
                    mEditIndex = -1;
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }

        showPreview();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mCreateBitmapFromCustomStampTask != null) {
            mCreateBitmapFromCustomStampTask.cancel(true);
            mCreateBitmapFromCustomStampTask.setOnCustomStampCreatedCallback(null);
        }
    }

    /**
     * shows a preview of custom rubber stamp using selected options
     */
    private void showPreview() {
        Context context = getContext();
        if (context == null || mCustomStampPreviewAppearances == null || mCustomStampPreviewAppearances.length == 0) {
            return;
        }

        String text = getFirstText();
        String secondText = CustomStampOption.createSecondText(mDateSwitch.isChecked(), mTimeSwitch.isChecked());
        int index = mCustomStampColorAdapter.getSelectedIndex();

        int borderColor = mCustomStampPreviewAppearances[index].borderColor;
        mPointingRightImage.setSelected(mShapeSelected == Shape.POINTING_RIGHT);
        mPointingLeftImage.setSelected(mShapeSelected == Shape.POINTING_LEFT);
        mRoundedRectangleImage.setSelected(mShapeSelected == Shape.ROUNDED_RECTANGLE);

        int bgColorStart = mCustomStampPreviewAppearances[index].bgColorStart;
        int bgColorEnd = mCustomStampPreviewAppearances[index].bgColorEnd;
        int textColor = mCustomStampPreviewAppearances[index].textColor;
        double fillOpacity = mCustomStampPreviewAppearances[index].fillOpacity;

        CustomStampOption customStampOption = new CustomStampOption(text, secondText,
            bgColorStart, bgColorEnd, textColor, borderColor, fillOpacity,
            mShapeSelected == Shape.POINTING_LEFT,
            mShapeSelected == Shape.POINTING_RIGHT);

        if (mCreateBitmapFromCustomStampTask != null) {
            mCreateBitmapFromCustomStampTask.cancel(true);
        }
        mCreateBitmapFromCustomStampTask = new CreateBitmapFromCustomStampTask(context, customStampOption);
        mCreateBitmapFromCustomStampTask.setOnCustomStampCreatedCallback(this);
        mCreateBitmapFromCustomStampTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @NonNull
    private String getFirstText() {
        String text = mStampText.getText().toString();
        if (Utils.isNullOrEmpty(text)) {
            text = getString(R.string.custom_stamp_text_hint);
        }
        return text;
    }

    private void saveCustomStamp() {
        Context context = getContext();
        if (context == null || mOnCustomStampChangedListener == null
            || mCustomStampPreviewAppearances == null || mCustomStampPreviewAppearances.length == 0) {
            return;
        }

        String text = getFirstText();
        String secondText = CustomStampOption.createSecondText(mDateSwitch.isChecked(), mTimeSwitch.isChecked());
        int index = mCustomStampColorAdapter.getSelectedIndex();
        int bgColorStart = mCustomStampPreviewAppearances[index].bgColorStart;
        int bgColorEnd = mCustomStampPreviewAppearances[index].bgColorEnd;
        int textColor = mCustomStampPreviewAppearances[index].textColor;
        int borderColor = mCustomStampPreviewAppearances[index].borderColor;
        double fillOpacity = mCustomStampPreviewAppearances[index].fillOpacity;

        CustomStampOption customStampOption = new CustomStampOption(text, secondText, bgColorStart,
            bgColorEnd, textColor, borderColor, fillOpacity,
            mShapeSelected == Shape.POINTING_LEFT,
            mShapeSelected == Shape.POINTING_RIGHT);
        SaveCustomStampOptionTask task = new SaveCustomStampOptionTask(context, mEditIndex, customStampOption, mOnCustomStampChangedListener);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class SaveCustomStampOptionTask extends CustomAsyncTask<Void, Void, Bitmap> {
        CustomStampOption mCustomStampOption;
        int mEditIndex;
        private int mSingleLineHeight, mTwoLinesHeight;
        WeakReference<OnCustomStampChangedListener> mListenerRef;

        SaveCustomStampOptionTask(Context context, int editIndex, CustomStampOption customStampOption, OnCustomStampChangedListener listener) {
            super(context);
            mEditIndex = editIndex;
            mCustomStampOption = customStampOption;
            mListenerRef = new WeakReference<>(listener);
            mSingleLineHeight = context.getResources().getDimensionPixelSize(R.dimen.stamp_image_height);
            mTwoLinesHeight = context.getResources().getDimensionPixelSize(R.dimen.stamp_image_height_two_lines);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Context context = getContext();
            if (context == null || isCancelled()) {
                return null;
            }

            try {
                final int height = context.getResources().getDimensionPixelSize(R.dimen.stamp_image_height);
                Bitmap bitmap = CreateBitmapFromCustomStampTask.createBitmapFromCustomStamp(mCustomStampOption, mSingleLineHeight, mTwoLinesHeight);

                if (bitmap == null || isCancelled()) {
                    return null;
                }

                int maxWidth = (int) Utils.convDp2Pix(context, 200);
                int marginWidth = (int) Utils.convDp2Pix(context, 175);
                int width = (int) Math.min(maxWidth, (height * bitmap.getWidth() / bitmap.getHeight() + .5));
                if (width > marginWidth && width < maxWidth) {
                    width = maxWidth;
                    bitmap = CreateBitmapFromCustomStampTask.createBitmapFromCustomStamp(mCustomStampOption, mSingleLineHeight, mTwoLinesHeight, width);
                }

                if (isCancelled() || bitmap == null) {
                    return null;
                }

                if (mEditIndex >= 0) {
                    CustomStampOption.updateCustomStamp(getContext(), mEditIndex, mCustomStampOption, bitmap);
                } else {
                    CustomStampOption.addCustomStamp(getContext(), mCustomStampOption, bitmap);
                }
                return bitmap;
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            OnCustomStampChangedListener listener = mListenerRef.get();
            if (listener != null) {
                if (mEditIndex == -1) {
                    listener.onCustomStampCreated(bitmap);
                } else {
                    listener.onCustomStampUpdated(bitmap, mEditIndex);
                }
            }
        }
    }

    private class CustomSpaceItemDecoration extends RecyclerView.ItemDecoration {

        int space;

        CustomSpaceItemDecoration(@NonNull Context context) {
            space = Math.round(Utils.convDp2Pix(context, 8));
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            if (mCustomStampPreviewAppearances == null) {
                return;
            }
            if (parent.getChildAdapterPosition(view) < mCustomStampPreviewAppearances.length / COLOR_COLUMN_COUNT) {
                outRect.bottom = space;
            }

            if (Utils.isRtlLayout(view.getContext())) {
                outRect.right = space;
            } else {
                outRect.left = space;
            }
        }

    }

    private enum Shape {
        POINTING_LEFT,
        POINTING_RIGHT,
        ROUNDED_RECTANGLE
    }

}
