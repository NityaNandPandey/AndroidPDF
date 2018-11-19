package com.pdftron.pdf.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.SegmentedGroup;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemTouchHelperCallback;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerViewAdapter;
import com.rarepebble.colorpicker.ColorPickerView;

import java.util.ArrayList;

/**
 * A dialog fragment shows a list of preset color modes. It allows user to set PDFViewCtrl to custom color view mode.
 */
public class CustomColorModeDialogFragment extends DialogFragment {

    private static String ARG_BGCOLOR = "arg_bgcolor";
    private static String ARG_TXTCOLOR = "arg_txtcolor";

    private static int[][] DEFAULT_PRESET_COLORS = {
            {0xFFf1f3f4, 0xFF363b3d},
            {0xFFb5c588, 0xFF31363a},
            {0xFFbfd8c2, 0xFF404b40},
            {0xFFd0d9e3, 0xFF293c58},
            {0xFFdeceac, 0xFF5b432e},
            {0xFFe9cdd5, 0xFFaa455c},
            {0xFF99FFCC, 0xFF000000},
            {0xFF99CCFF, 0xFF000000},

            {0xFF34302e, 0xFF898784},
            {0xFF6C2D2C, 0xFFFCDCDC},
            {0xFF5A5F37, 0xFFFCDCDC},
            {0xFF402F21, 0xFFdeceac},
            {0xFF083045, 0xFFdbe1e3},
            {0xFF353b3d, 0xFF879699},
            {0xFF003300, 0xFFFFFFFF},
    };

    private CustomColorModeSelectedListener mCustomColorModeSelectedListener = null;

    private boolean mDarkSelected = false;
    private boolean mEditing = false;
    private ColorPickerView mColorPickerView;
    private LinearLayout mPreviewLayout;
    private SegmentedGroup mColorCompSelector;

    private int mCurrentDarkColor;
    private int mCurrentLightColor;

    private View mColorPickingView;
    private Button mPickerCancelBtn;
    private Button mPickerOKBtn;

    private View mPresetView;
    private PresetRecyclerAdapter mPresetRecyclerAdapter;
    private Button mPresetCancelBtn;
    private Button mPresetOKBtn;

    private CustomViewPager mViewPager;

    private JsonArray mPresetArray;
    private boolean mHasAction = false;
    private int mPreviousClickedPosition = -1;

    private View.OnClickListener mPickerCancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mPresetRecyclerAdapter.getSelectedPos() < 0) {
                mCurrentLightColor = 0xFFFFFFFF;
                mCurrentDarkColor = 0xFF000000;
            } else {
                JsonObject presetObj = mPresetArray.get(mPresetRecyclerAdapter.getSelectedPos()).getAsJsonObject();
                mCurrentDarkColor = presetObj.get("fg").getAsInt();
                mCurrentLightColor = presetObj.get("bg").getAsInt();
            }
            mEditing = false;
            mViewPager.setCurrentItem(0, true);
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CUSTOM_COLOR_MODE,
                AnalyticsParam.customColorParam(AnalyticsHandlerAdapter.CUSTOM_COLOR_MODE_CANCEL_EDIT));
        }
    };

    /**
     * Creates a new instance of custom color mode dialog fragment
     * @param bgColor background color
     * @param txtColor text color
     * @return new instance of custom color mode dialog fragment
     */
    public static CustomColorModeDialogFragment newInstance(int bgColor, int txtColor) {
        CustomColorModeDialogFragment fragment = new CustomColorModeDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_BGCOLOR, bgColor);
        args.putInt(ARG_TXTCOLOR, txtColor);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Overload implementation of {@link DialogFragment#onCreate(Bundle)}
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCurrentDarkColor = getArguments().getInt(ARG_TXTCOLOR);
            mCurrentLightColor = getArguments().getInt(ARG_BGCOLOR);
        }
    }

    /**
     * Overload implementation of {@link DialogFragment#onCreateDialog(Bundle)}
     * @param savedInstanceState The last saved instance state of the Fragment,
     * or null if this is a freshly created Fragment.
     * @return a new Dialog instance to be displayed by the Fragment.
     */
    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.fragment_custom_color_mode_dialog, null);
        builder.setView(view);

        builder.setPositiveButton(null, null);

        builder.setNegativeButton(null, null);

        mViewPager = view.findViewById(R.id.colormode_viewpager);
        mViewPager.setDimens(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT,
                getResources().getDimensionPixelSize(R.dimen.colormode_height_portrait),
                getResources().getDimensionPixelSize(R.dimen.colormode_height_landscape));

        mViewPager.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return true;
            }
        });
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    if (mEditing) {
                        mPickerCancelListener.onClick(null);
                    }
                    return false;
                }
                return true;
            }
        });

        mColorPickingView = inflater.inflate(R.layout.fragment_custom_color_mode_colorpicker_page, null);
        mPresetView = inflater.inflate(R.layout.fragment_custom_color_mode_preset_page, null);

        mPresetCancelBtn = mPresetView.findViewById(R.id.colormode_preset_cancelbtn);
        mPresetOKBtn = mPresetView.findViewById(R.id.colormode_preset_okbtn);

        mPickerCancelBtn = mColorPickingView.findViewById(R.id.colormode_picker_cancelbtn);
        mPickerOKBtn = mColorPickingView.findViewById(R.id.colormode_picker_okbtn);

        String serializedPresetArray = PdfViewCtrlSettingsManager.getColorModePresets(getContext());
        if (Utils.isNullOrEmpty(serializedPresetArray)) {
            serializedPresetArray = loadDefaultPresets();
        }
        JsonParser jParser = new JsonParser();
        mPresetArray = jParser.parse(serializedPresetArray).getAsJsonArray();

        SimpleRecyclerView presetRecyclerView = mPresetView.findViewById(R.id.colormode_preset_recycler);
        presetRecyclerView.initView(4);

        mPresetRecyclerAdapter = new PresetRecyclerAdapter(mPresetArray, 4, new PresetRecyclerAdapterListener() {
            @Override
            public void onPositionSelected(int position) {
                JsonObject presetObj = mPresetArray.get(position).getAsJsonObject();
                mCurrentDarkColor = presetObj.get("fg").getAsInt();
                mCurrentLightColor = presetObj.get("bg").getAsInt();
            }
        });
        presetRecyclerView.setAdapter(mPresetRecyclerAdapter);
        Utils.safeNotifyDataSetChanged(mPresetRecyclerAdapter);
        mPresetRecyclerAdapter.setSelectedPos(PdfViewCtrlSettingsManager.getSelectedColorModePreset(getContext()));

        mColorCompSelector = mColorPickingView.findViewById(R.id.colormode_comp_selector);
        mColorCompSelector.check(R.id.colormode_bgcolor_selector);
        mColorCompSelector.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.colormode_textcolor_selector) {
                    mDarkSelected = true;
                    mColorPickerView.setColor(mCurrentDarkColor);
                } else if (checkedId == R.id.colormode_bgcolor_selector) {
                    mDarkSelected = false;
                    mColorPickerView.setColor(mCurrentLightColor);
                }
            }
        });
        mColorCompSelector.setTintColor(getResources().getColor(R.color.gray600));

        mColorPickerView = mColorPickingView.findViewById(R.id.color_picker_picker);

        mColorPickerView.setColor(mCurrentLightColor);

        mColorPickerView.setListener(new ColorPickerView.ColorPickerViewListener() {
            @Override
            public void onColorChanged(int color) {
                if (mDarkSelected) {
                    mCurrentDarkColor = color;
                } else {
                    mCurrentLightColor = color;
                }
                updatePreview();
            }
        });

        mPreviewLayout = mColorPickingView.findViewById(R.id.colormode_testchars);

        mPreviewLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int temp = mCurrentDarkColor;
                mCurrentDarkColor = mCurrentLightColor;
                mCurrentLightColor = temp;

                mDarkSelected = true;
                mColorPickerView.setColor(mCurrentDarkColor);
                mColorCompSelector.check(R.id.colormode_textcolor_selector);
            }
        });

        mViewPager.setAdapter(new CustomPagerAdapter());

        updatePreview();

        return builder.create();
    }

    /**
     * Overload implementation of {@link DialogFragment#onStart()}
     *
     */
    @Override
    public void onStart() {
        super.onStart();

        mPresetOKBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPresetRecyclerAdapter.getSelectedPos() < 0) {
                    dismiss();
                    return;
                }
                savePresets();
                PdfViewCtrlSettingsManager.setSelectedColorModePreset(getContext(), mPresetRecyclerAdapter.getSelectedPos());
                if (mCustomColorModeSelectedListener != null)
                    mCustomColorModeSelectedListener.onCustomColorModeSelected(mCurrentLightColor, mCurrentDarkColor);
                if (mPreviousClickedPosition > -1) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CUSTOM_COLOR_MODE,
                        AnalyticsParam.customColorParam(AnalyticsHandlerAdapter.CUSTOM_COLOR_MODE_SELECT,
                            mPreviousClickedPosition,  isColorDefault(mCurrentLightColor, mCurrentDarkColor),
                            mCurrentLightColor, mCurrentDarkColor, true));
                }
                dismiss();
            }
        });
        mPresetCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        mPickerOKBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPresetRecyclerAdapter.getSelectedPos() < 0) {
                    dismiss();
                    return;
                }
                savePresets();
                Utils.safeNotifyDataSetChanged(mPresetRecyclerAdapter);
                mViewPager.setCurrentItem(0, true);
                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CUSTOM_COLOR_MODE,
                    AnalyticsParam.customColorParam(AnalyticsHandlerAdapter.CUSTOM_COLOR_MODE_EDIT,
                        mPresetRecyclerAdapter.getSelectedPos(),
                        isColorDefault(mCurrentLightColor, mCurrentDarkColor),
                        mCurrentLightColor, mCurrentDarkColor));
            }
        });
        mPickerCancelBtn.setOnClickListener(mPickerCancelListener);
    }

    private String loadDefaultPresets() {
        int defaultColorArraySize = DEFAULT_PRESET_COLORS.length;
        JsonArray array = new JsonArray();
        for (int i = 0; i < 15; i++) {
            JsonObject obj = new JsonObject();
            if (i < defaultColorArraySize) {
                obj.addProperty("bg", DEFAULT_PRESET_COLORS[i][0]);
                obj.addProperty("fg", DEFAULT_PRESET_COLORS[i][1]);
            } else {
                obj.addProperty("bg", 0xFFFFFFFF);
                obj.addProperty("fg", 0xFF000000);
            }
            array.add(obj);
        }

        Gson gson = new Gson();
        String serialize = gson.toJson(array);
        PdfViewCtrlSettingsManager.setColorModePresets(getContext(), serialize);
        return serialize;
    }

    private void loadDefaultPresets(JsonArray presets) {
        int defaultColorArraySize = DEFAULT_PRESET_COLORS.length;
        for (int i = 0; i < 15; i++) {
            if (i < presets.size()) {
                presets.get(i).getAsJsonObject().remove("bg");
                presets.get(i).getAsJsonObject().remove("fg");
            }
            int bg = i < defaultColorArraySize ? DEFAULT_PRESET_COLORS[i][0] : 0xFFFFFFFF;
            int fg = i < defaultColorArraySize ? DEFAULT_PRESET_COLORS[i][1] : 0xFF000000;
            if (i < presets.size()) {
                presets.get(i).getAsJsonObject().addProperty("bg", bg);
                presets.get(i).getAsJsonObject().addProperty("fg", fg);
            } else {
                JsonObject obj = new JsonObject();
                obj.addProperty("bg", bg);
                obj.addProperty("fg", fg);
                presets.add(obj);
            }
        }
        mCurrentDarkColor = 0xFF000000;
        mCurrentLightColor = 0xFFFFFFFF;
        mPresetRecyclerAdapter.setSelectedPos(0);
    }

    private void savePresets() {
        int selectedPreset = mPresetRecyclerAdapter.getSelectedPos();
        if (selectedPreset < 0)
            return;
        mPresetArray.get(selectedPreset).getAsJsonObject().remove("bg");
        mPresetArray.get(selectedPreset).getAsJsonObject().remove("fg");
        mPresetArray.get(selectedPreset).getAsJsonObject().addProperty("bg", mCurrentLightColor);
        mPresetArray.get(selectedPreset).getAsJsonObject().addProperty("fg", mCurrentDarkColor);
        Gson gson = new Gson();
        String serialized = gson.toJson(mPresetArray);
        PdfViewCtrlSettingsManager.setColorModePresets(getContext(), serialized);
    }

    private void updatePreview() {
        mPreviewLayout.setBackgroundColor(mCurrentLightColor);

        int redBase = ((mCurrentDarkColor & 0x00FF0000) >> 16);
        int greenBase = ((mCurrentDarkColor & 0x0000FF00) >> 8);
        int blueBase = (mCurrentDarkColor & 0x000000FF);

        int redDiff = ((mCurrentLightColor & 0x00FF0000) >> 16) - redBase;
        int greenDiff = ((mCurrentLightColor & 0x0000FF00) >> 8) - greenBase;
        int blueDiff = (mCurrentLightColor & 0x000000FF) - blueBase;

        for (int i = 0; i < mPreviewLayout.getChildCount(); i++) {
            TextView tView = (TextView) mPreviewLayout.getChildAt(i);
            float t = i * 0.2f;

            int redComp = redBase + (int) (redDiff * t);
            int greenComp = greenBase + (int) (greenDiff * t);
            int blueComp = blueBase + (int) (blueDiff * t);

            int finalColor = 0xFF000000 + ((redComp << 16) & 0x00FF0000) + ((greenComp << 8) & 0x0000FF00) + (blueComp & 0x000000FF);

            tView.setTextColor(finalColor);
        }
    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!mHasAction) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CUSTOM_COLOR_MODE,
                AnalyticsParam.customColorParam(AnalyticsHandlerAdapter.CUSTOM_COLOR_MODE_NO_ACTION));
        }
    }

    /**
     * Sets custom color mode selected listener
     * @param listener The selected listener
     */
    public void setCustomColorModeSelectedListener(CustomColorModeSelectedListener listener) {
        mCustomColorModeSelectedListener = listener;
    }

    /**
     * Listener interface for custom color mode selected event
     */
    public interface CustomColorModeSelectedListener {
        /**
         * This method is invoked when there is a custom color mode selected
         * @param bgColor background color
         * @param txtColor text color
         */
        void onCustomColorModeSelected(int bgColor, int txtColor);
    }

    private class CustomPagerAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(ViewGroup collection, int pos) {
            if (pos == 0) {
                collection.addView(mPresetView);
                return mPresetView;
            } else if (pos == 1) {
                collection.addView(mColorPickingView);
                return mColorPickingView;
            }
            return null;
        }

        @Override
        public void destroyItem(ViewGroup collection, int pos, Object obj) {
            collection.removeView((View) obj);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    /**
     * A Recycler view adapter of preset custom color modes
     */
    public class PresetRecyclerAdapter extends SimpleRecyclerViewAdapter<JsonObject, RecyclerView.ViewHolder> {

        private JsonArray mItems;
        private int mSelectedPosition = -1;
        private ArrayList<ItemViewHolder> mHolders = new ArrayList<>();
        private PresetRecyclerAdapterListener mListener;


        /**
         * Class constructor
         * @param items preset color items
         * @param spanCount span count for grid recycler view
         * @param listener preset recycler adapter listener
         */
        PresetRecyclerAdapter(JsonArray items, @SuppressWarnings("unused") int spanCount, PresetRecyclerAdapterListener listener) {
            mItems = items;
            mListener = listener;
        }

        /**
         * @hide
         * Gets item of preset color mode in JsonObject format
         * @param position The position
         *
         * @return null
         */
        @Override
        public JsonObject getItem(int position) {
            return null;
        }

        /**
         * @hide
         * @param item The item
         */
        @Override
        public void add(JsonObject item) {

        }

        /**
         * @hide
         * @param item The item
         */
        @Override
        public boolean remove(JsonObject item) {
            return false;
        }

        /**
         * @hide
         */
        @Override
        public JsonObject removeAt(int location) {
            return null;
        }

        /**
         * @hide
         */
        @Override
        public void insert(JsonObject item, int position) {

        }

        /**
         * Gets view type of specified position
         * @param position The specified position
         * @return {@link ItemTouchHelperCallback#VIEW_TYPE_CONTENT}
         */
        @Override
        public int getItemViewType(int position) {
            return ItemTouchHelperCallback.VIEW_TYPE_CONTENT;
        }

        /**
         * Overload implementation of {@link SimpleRecyclerViewAdapter#onCreateViewHolder(ViewGroup, int)}
         * @return new instance of view holder
         */
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;

            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recyclerview_item_colorpreset_list, parent, false);

            return new ItemViewHolder(view, -1, false);
        }

        /**
         * Overload implementation of {@link SimpleRecyclerViewAdapter#onBindViewHolder(RecyclerView.ViewHolder, int)}
         */
        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
            ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
            itemViewHolder.position = position;
            itemViewHolder.isResetButton = position == getItemCount() - 1;
            if (position < mHolders.size()) {
                mHolders.remove(position);
                mHolders.add(position, itemViewHolder);
            } else {
                mHolders.add(itemViewHolder);
            }

            if (position == getItemCount() - 1) {
                itemViewHolder.selectedBgIcon.setVisibility(View.INVISIBLE);
                itemViewHolder.editButton.setVisibility(View.GONE);
                itemViewHolder.fgIcon.setVisibility(View.GONE);
                itemViewHolder.bgIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_settings_backup_restore_black_24dp));
                itemViewHolder.bgIcon.getDrawable().mutate().setColorFilter(getResources().getColor(R.color.gray600), PorterDuff.Mode.SRC_IN);
                return;
            }

            final JsonObject jObj = mItems.get(position).getAsJsonObject();
            if (mSelectedPosition == position) {
                itemViewHolder.selectedBgIcon.setVisibility(View.VISIBLE);
                itemViewHolder.editButton.setVisibility(View.VISIBLE);
                itemViewHolder.editButton.setEnabled(true);
            } else {
                itemViewHolder.selectedBgIcon.setVisibility(View.INVISIBLE);
                itemViewHolder.editButton.setVisibility(View.INVISIBLE);
                itemViewHolder.fgIcon.setVisibility(View.VISIBLE);
                itemViewHolder.bgIcon.setColorFilter(null);
                itemViewHolder.bgIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_custommode_icon));
            }

            int bgColor = jObj.get("bg").getAsInt();
            int fgColor = jObj.get("fg").getAsInt();

            Drawable drawable = itemViewHolder.bgIcon.getDrawable();
            drawable.mutate().setColorFilter(bgColor, PorterDuff.Mode.SRC_ATOP);

            itemViewHolder.fgIcon.setColorFilter(fgColor);
        }

        /**
         * Gets preset custom color mode count
         * @return preset custom color mode size + 1
         */
        @Override
        public int getItemCount() {
            return mItems.size() + 1;
        }

        void setSelectedPos(int position) {
            mSelectedPosition = position;
            for (int i = 0; i < (mHolders.size() - 1); i++) {
                if (i == position) {
                    mHolders.get(i).selectedBgIcon.setVisibility(View.VISIBLE);
                    mHolders.get(i).editButton.setVisibility(View.VISIBLE);
                    mHolders.get(i).editButton.setEnabled(true);
                } else {
                    if (mHolders.get(i).editButton.getVisibility() == View.GONE)
                        continue;
                    mHolders.get(i).selectedBgIcon.setVisibility(View.INVISIBLE);
                    mHolders.get(i).editButton.setVisibility(View.INVISIBLE);
                    mHolders.get(i).editButton.setEnabled(false);
                }
            }
            if (mSelectedPosition >= 0) {
                mListener.onPositionSelected(position);
            }
        }

        /**
         * Gets selected position
         * @return selected position
         */
        int getSelectedPos() {
            return mSelectedPosition;
        }

        int[] getColorsAtPos(int position) {
            JsonObject jObj = mItems.get(position).getAsJsonObject();
            int[] result = new int[2];
            result[0] = jObj.get("bg").getAsInt();
            result[1] = jObj.get("fg").getAsInt();
            return result;
        }

        /**
         * @hide
         */
        @Override
        public void updateSpanCount(int count) {

        }

        class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            ImageView fgIcon;
            ImageView bgIcon;
            ImageView selectedBgIcon;
            Button editButton;
            public int position;
            boolean isResetButton;

            ItemViewHolder(View itemView, int position, boolean isResetButton) {
                super(itemView);

                fgIcon = itemView.findViewById(R.id.fg_icon);
                bgIcon = itemView.findViewById(R.id.bg_icon);
                selectedBgIcon = itemView.findViewById(R.id.select_bg_icon);
                editButton = itemView.findViewById(R.id.color_editbutton);

                int accentColor = Utils.getAccentColor(getContext());
                selectedBgIcon.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);

                this.position = position;
                this.isResetButton = isResetButton;
                bgIcon.setOnClickListener(this);
                if (!isResetButton) {
                    editButton.setOnClickListener(this);
                }
            }

            @Override
            public void onClick(View v) {
                if (v == bgIcon) {
                    mHasAction = true;
                    if (isResetButton) {
                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getContext());
                        alertBuilder.setTitle(R.string.pref_colormode_custom_defaults);
                        alertBuilder.setMessage(R.string.pref_colormode_custom_defaults_msg);
                        alertBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                loadDefaultPresets(mPresetArray);
                                Utils.safeNotifyDataSetChanged(mPresetRecyclerAdapter);
                                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CUSTOM_COLOR_MODE,
                                    AnalyticsParam.customColorParam(AnalyticsHandlerAdapter.CUSTOM_COLOR_MODE_RESTORE_DEFAULT));
                                dialog.dismiss();
                            }
                        });
                        alertBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CUSTOM_COLOR_MODE,
                                    AnalyticsParam.customColorParam(AnalyticsHandlerAdapter.CUSTOM_COLOR_MODE_CANCEL_RESTORE_DEFAULT));
                                dialog.dismiss();
                            }
                        });
                        alertBuilder.create().show();
                        return;
                    }

                    if (mPreviousClickedPosition > -1 && mPreviousClickedPosition != position) {
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CUSTOM_COLOR_MODE,
                            AnalyticsParam.customColorParam(AnalyticsHandlerAdapter.CUSTOM_COLOR_MODE_SELECT,
                                mPreviousClickedPosition,
                                isColorDefault(mCurrentLightColor, mCurrentDarkColor),
                                mCurrentLightColor, mCurrentDarkColor, false));
                    }

                    setSelectedPos(position);
                    mPreviousClickedPosition = position;
                } else if (v == editButton) {
                    if (!v.isEnabled())
                        return;
                    int selectedPos = mPresetRecyclerAdapter.getSelectedPos();
                    if (selectedPos < 0)
                        return;
                    mHasAction = true;
                    mDarkSelected = false;
                    mColorPickerView.setColor(mCurrentLightColor);
                    mColorCompSelector.check(R.id.colormode_bgcolor_selector);
                    mEditing = true;
                    mViewPager.setCurrentItem(1, true);
                }
            }
        }
    }

    private boolean isColorDefault(int bgColor, int fgColor) {
        for (int[] defaultColor : DEFAULT_PRESET_COLORS) {
            if (bgColor == defaultColor[0] && fgColor == defaultColor[1]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Listener interface for preset custom color mode selected event
     */
    public interface PresetRecyclerAdapterListener {
        /**
         * This method is invoked when a preset custom color mode is selected
         * @param position selected position
         */
        void onPositionSelected(int position);
    }

    /**
     * A view pager for displaying custom color mode list
     */
    public static class CustomViewPager extends ViewPager {

        private boolean mIsPortrait = true;
        private int mHeightPortrait = 0;
        private int mHeightLandscape = 0;

        /**
         * Class constructor
         */
        public CustomViewPager(Context context) {
            super(context);
        }

        /**
         * Class constructor
         */
        public CustomViewPager(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        /**
         * Overload implementation of {@link ViewPager#onInterceptTouchEvent(MotionEvent)}
         * @param event The motion event
         * @return false
         */
        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            return false;
        }

        /**
         * Sets view pager dimensions
         * @param isPortrait whether it is in portrait mode
         * @param heightPortrait portrait mode height
         * @param heightLandscape landscape mode height
         */
        public void setDimens(boolean isPortrait, int heightPortrait, int heightLandscape) {
            mIsPortrait = isPortrait;
            mHeightPortrait = heightPortrait;
            mHeightLandscape = heightLandscape;
        }

        /**
         * Override implementation of {@link ViewPager#onTouchEvent(MotionEvent)}
         * @param event The motion event
         * @return false
         */
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return false;
        }

        /**
         * Overload implementation of {@link ViewPager#onConfigurationChanged(Configuration)}
         * @param newConfig new configuration
         */
        @Override
        protected void onConfigurationChanged(Configuration newConfig) {
            mIsPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        }

        /**
         * Overload implementation of {@link ViewPager#onMeasure(int, int)}
         * @param widthMeasureSpec width measure spec
         * @param heightMeasureSpec height measure spec
         */
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int height = 0;
            int h = mIsPortrait ? mHeightPortrait : mHeightLandscape;
            if (h > height) height = h;

            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
