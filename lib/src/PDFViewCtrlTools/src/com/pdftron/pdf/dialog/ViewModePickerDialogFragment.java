//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.dialog;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.content.res.AppCompatResources;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.ReflowPagerAdapter;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The ViewModePickerDialogFragment shows various view mode options,
 * including different page presentation modes, color modes, reflow mode etc.
 */
public class ViewModePickerDialogFragment extends DialogFragment {

    protected static final String BUNDLE_CURRENT_VIEW_MODE = "current_view_mode";
    protected static final String BUNDLE_CURRENT_RTL_MODE = "current_rtl_mode";
    protected static final String BUNDLE_CURRENT_REFLOW_MODE = "current_reflow_mode";
    protected static final String BUNDLE_CURRENT_REFLOW_TEXT_SIZE = "current_reflow_text_size";
    protected static final String BUNDLE_ACTION = "action";

    /**
     * List item id. Includes:  {@link #ITEM_ID_CONTINUOUS}, {@link #ITEM_ID_TEXT_SIZE}, {@link #ITEM_ID_ROTATION,
     * {@link #ITEM_ID_USERCROP}, {@link #ITEM_ID_RTLMODE}, {@link #ITEM_ID_BLANK}, {@link #ITEM_ID_COLORMODE})
     */
    @IntDef({ITEM_ID_CONTINUOUS, ITEM_ID_TEXT_SIZE, ITEM_ID_ROTATION,
        ITEM_ID_USERCROP, ITEM_ID_RTLMODE, ITEM_ID_BLANK, ITEM_ID_COLORMODE})
    @Retention(RetentionPolicy.SOURCE)
    @interface EntryItemId {
    }

    /**
     * Continuous mode control item
     */
    protected static final int ITEM_ID_CONTINUOUS = 100;
    /**
     * Text size control item
     */
    protected static final int ITEM_ID_TEXT_SIZE = 101;
    /**
     * Page rotation control item
     */
    protected static final int ITEM_ID_ROTATION = 103;
    /**
     * Crop page control item
     */
    protected static final int ITEM_ID_USERCROP = 105;
    /**
     * Right to left mode control item
     */
    protected static final int ITEM_ID_RTLMODE = 106;
    /**
     * Blank item
     */
    protected static final int ITEM_ID_BLANK = 107;
    /**
     * Color mode control item
     */
    protected static final int ITEM_ID_COLORMODE = 108;

    /**
     * @hide View mode entry key -- icon
     */
    protected static final String KEY_ITEM_ICON = "item_view_mode_picker_list_icon";
    /**
     * @hide View mode entry key -- text
     */
    protected static final String KEY_ITEM_TEXT = "item_view_mode_picker_list_text";
    /**
     * @hide View mode entry key -- id
     */
    protected static final String KEY_ITEM_ID = "item_view_mode_picker_list_id";
    /**
     * @hide View mode entry key -- control
     */
    protected static final String KEY_ITEM_CONTROL = "item_view_mode_picker_list_control";

    /**
     * View mode list item control type.
     * Includes: : {@link #CONTROL_TYPE_RADIO}, {@link #CONTROL_TYPE_SWITCH}, {@link #CONTROL_TYPE_SIZE},
     * {@link #CONTROL_TYPE_NONE}
     */
    @IntDef({CONTROL_TYPE_RADIO, CONTROL_TYPE_SWITCH, CONTROL_TYPE_SIZE,
        CONTROL_TYPE_NONE})
    @Retention(RetentionPolicy.SOURCE)
    @interface EntryControlType {
    }

    /**
     * The item is controlled by radio button
     */
    protected static final int CONTROL_TYPE_RADIO = 0;
    /**
     * The item is controlled by switch button
     */
    protected static final int CONTROL_TYPE_SWITCH = 1;
    /**
     * The item is controlled by size
     */
    protected static final int CONTROL_TYPE_SIZE = 2;
    /**
     * The item is controlled by nothing
     */
    protected static final int CONTROL_TYPE_NONE = 3;

    private boolean mHasEventAction;

    /**
     * @hide Color mode layout
     */
    protected RelativeLayout mColorModeLayout;
    /**
     * @hide View mode layout
     */
    protected LinearLayout mViewModeLayout;
    /**
     * @hide View mode options list view
     */
    protected ListView mOptionListView;
    /**
     * @hide Separated list adapter for {@link #mOptionListView}
     */
    protected SeparatedListAdapter mListAdapter;

    /**
     * @hide Current view mode
     */
    protected PDFViewCtrl.PagePresentationMode mCurrentViewMode;

    /**
     * @hide Whether the view mode is in right to left mode
     */
    protected boolean mIsRtlMode;
    /**
     * @hide Whether the view mode is in reflow mode
     */
    protected boolean mIsReflowMode;

    /**
     * @hide Reflow mode text size
     */
    protected int mReflowTextSize;

    /**
     * @hide View mode option entries list
     */
    protected List<Map<String, Object>> mViewModeOptionsList;

    /**
     * @hide ViewMode listener
     */
    protected ViewModePickerDialogFragmentListener mViewModePickerDialogListener;

    /**
     * Creates a new instance of view mode picker dialog fragment
     *
     * @param currentViewMode current view mode
     * @param isRTLMode       is right to left mode
     * @param isReflowMode    is reflow mode
     * @param reflowTextSize  reflow mode text size
     * @return view mode picker dialog fragment
     */
    public static ViewModePickerDialogFragment newInstance(PDFViewCtrl.PagePresentationMode currentViewMode, boolean isRTLMode,
                                                           boolean isReflowMode, int reflowTextSize) {
        ViewModePickerDialogFragment f = new ViewModePickerDialogFragment();
        Bundle args = new Bundle();
        args.putInt(BUNDLE_CURRENT_VIEW_MODE, currentViewMode.getValue());
        args.putBoolean(BUNDLE_CURRENT_RTL_MODE, isRTLMode);
        args.putBoolean(BUNDLE_CURRENT_REFLOW_MODE, isReflowMode);
        args.putInt(BUNDLE_CURRENT_REFLOW_TEXT_SIZE, reflowTextSize);
        f.setArguments(args);

        return f;
    }

    /**
     * Populate view mode option list
     */
    protected void populateOptionsList() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        mViewModeOptionsList = new ArrayList<>();
        Resources res = getResources();

        mViewModeOptionsList.add(createItem(ITEM_ID_CONTINUOUS, res.getDrawable(R.drawable.ic_view_mode_continuous_black_24dp),
            getString(R.string.pref_viewmode_scrolling_direction), CONTROL_TYPE_SWITCH));

        mViewModeOptionsList.add(createItem(ITEM_ID_TEXT_SIZE, res.getDrawable(R.drawable.ic_font_size_black_24dp),
            getString(R.string.pref_viewmode_reflow_text_size), CONTROL_TYPE_SIZE));

        mViewModeOptionsList.add(createItem(ITEM_ID_COLORMODE, null, null, CONTROL_TYPE_RADIO));

        if (PdfViewCtrlSettingsManager.hasRtlModeOption(context)) {
            mViewModeOptionsList.add(createItem(ITEM_ID_RTLMODE, res.getDrawable(R.drawable.rtl),
                getString(R.string.pref_viewmode_rtl_mode), CONTROL_TYPE_SWITCH));
        }

        mViewModeOptionsList.add(createItem(ITEM_ID_BLANK, null, null, CONTROL_TYPE_NONE));

        mListAdapter.addSection(null, new ViewModeEntryAdapter(getActivity(), mViewModeOptionsList));

        List<Map<String, Object>> list = new ArrayList<>();

        list.add(createItem(ITEM_ID_ROTATION, res.getDrawable(R.drawable.ic_rotate_right_black_24dp),
            getString(R.string.pref_viewmode_rotation), CONTROL_TYPE_NONE));

        list.add(createItem(ITEM_ID_USERCROP, res.getDrawable(R.drawable.user_crop),
            getString(R.string.pref_viewmode_user_crop), CONTROL_TYPE_NONE));

        mListAdapter.addSection(getString(R.string.pref_viewmode_actions), new ViewModeEntryAdapter(getActivity(), list));
    }

    /**
     * Creates a view mode entry
     *
     * @param id          item id
     * @param drawable    item drawable
     * @param title       item title
     * @param controlType item control type
     * @return View mode entry
     */
    protected Map<String, Object> createItem(@EntryItemId int id, Drawable drawable, String title, @EntryControlType int controlType) {
        Map<String, Object> item = new HashMap<>();
        item.put(KEY_ITEM_ID, id);
        item.put(KEY_ITEM_ICON, drawable);
        item.put(KEY_ITEM_TEXT, title);
        item.put(KEY_ITEM_CONTROL, controlType);
        return item;
    }

    /**
     * Sets view mode picker dialog fragment listener
     *
     * @param listener The listener
     */
    public void setViewModePickerDialogFragmentListener(ViewModePickerDialogFragmentListener listener) {
        mViewModePickerDialogListener = listener;
    }

    /**
     * Overload implementation of {@link DialogFragment#onCreate(Bundle)}
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null) {
            return;
        }
        int mode = getArguments().getInt(BUNDLE_CURRENT_VIEW_MODE, PDFViewCtrl.PagePresentationMode.SINGLE.getValue());
        mCurrentViewMode = PDFViewCtrl.PagePresentationMode.valueOf(mode);
        mIsRtlMode = getArguments().getBoolean(BUNDLE_CURRENT_RTL_MODE, false);
        mIsReflowMode = getArguments().getBoolean(BUNDLE_CURRENT_REFLOW_MODE, false);
        mReflowTextSize = getArguments().getInt(BUNDLE_CURRENT_REFLOW_TEXT_SIZE, 100);
        mHasEventAction = getArguments().getBoolean(BUNDLE_ACTION, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_VIEW_MODE_OPEN);
    }

    @Override
    public void onStop() {
        super.onStop();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_VIEW_MODE_OPEN);
    }

    /**
     * Overload implementation of {@link DialogFragment#onDismiss(DialogInterface)}
     *
     * @param dialog dismissed dialog
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mViewModePickerDialogListener != null) {
            mViewModePickerDialogListener.onViewModePickerDialogFragmentDismiss();
        }
        super.onDismiss(dialog);
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEWER_VIEW_MODE_CLOSE,
            AnalyticsParam.noActionParam(mHasEventAction));
    }

    /**
     * Sets list view height based on children layout
     *
     * @param listView list view
     * @param layout   children layout
     */
    public void setListViewHeightBasedOnChildren(ListView listView, LinearLayout layout) {
        if (mListAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < mListAdapter.getCount(); i++) {
            View listItem = mListAdapter.getView(i, null, listView);
            listItem.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (mListAdapter.getCount() - 1)) + 10;
        listView.setLayoutParams(params);
        listView.requestLayout();

        ViewGroup.LayoutParams params2 = layout.getLayoutParams();
        params2.height = totalHeight + (listView.getDividerHeight() * (mListAdapter.getCount() - 1)) + 10;
        layout.setLayoutParams(params2);
        layout.requestLayout();
    }

    /**
     * Overload implementation of {@link DialogFragment#onCreateDialog(Bundle)}
     *
     * @param savedInstanceState The last saved instance state of the Fragment,
     *                           or null if this is a freshly created Fragment.
     * @return View mode dialog
     */
    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_view_mode_picker_dialog, null);
        builder.setView(view);

        mViewModeLayout = (TableLayout) view.findViewById(R.id.fragment_view_mode_button_table_layout);
        for (int i = 0; i < mViewModeLayout.getChildCount() * 2; i++) {
            View child = ((TableRow) mViewModeLayout.getChildAt(i / 2)).getChildAt(i % 2);
            child.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int id = v.getId();
                    if (id == R.id.fragment_view_mode_button_reflow) {
                        if (mViewModePickerDialogListener != null
                            && mViewModePickerDialogListener.checkTabConversionAndAlert(R.string.cant_reflow_while_converting_message, true)) {
                            return;
                        }
                    }

                    mHasEventAction = true;
                    int viewMode = -1;
                    if (id == R.id.fragment_view_mode_button_single) {
                        viewMode = AnalyticsHandlerAdapter.VIEW_MODE_SINGLE;
                    } else if (id == R.id.fragment_view_mode_button_facing) {
                        viewMode = AnalyticsHandlerAdapter.VIEW_MODE_DOUBLE;
                    } else if (id == R.id.fragment_view_mode_button_cover) {
                        viewMode = AnalyticsHandlerAdapter.VIEW_MODE_COVER;
                    } else if (id == R.id.fragment_view_mode_button_reflow) {
                        viewMode = AnalyticsHandlerAdapter.VIEW_MODE_REFLOW;
                    }

                    if (viewMode != -1) {
                        boolean isCurrent = id == getActiveMode();
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEW_MODE,
                            AnalyticsParam.viewModeParam(viewMode, isCurrent));
                    }

                    if (id != getActiveMode()) {
                        // Set this view-mode as active
                        setActiveMode(v.getId());
                        // Set the view mode first, then update dialog using the new view-mode settings
                        setViewMode(isContinuousMode());
                        updateDialogLayout();
                    }
                }
            });
            child.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    String description = v.getContentDescription().toString();
                    // Show toast below view, with the end aligned with the view's center
                    int location[] = new int[2];
                    v.getLocationOnScreen(location);
                    CommonToast.showText(getActivity(), description, Toast.LENGTH_SHORT, Gravity.START | Gravity.TOP, location[0], location[1] + v.getMeasuredHeight() / 2);
                    return true;
                }
            });

            View img = ((LinearLayout) child).getChildAt(0);
            if (img instanceof ImageView) {
                ImageView imageView = (ImageView) img;
                Drawable icon = imageView.getDrawable();
                if (icon != null && icon.getConstantState() != null) {
                    icon = DrawableCompat.wrap(icon.getConstantState().newDrawable()).mutate();
                    ColorStateList iconTintList = AppCompatResources.getColorStateList(getActivity(), R.color.selector_action_item_icon_color);
                    DrawableCompat.setTintList(icon, iconTintList);
                }
                imageView.setImageDrawable(icon);
            }
        }

        // Configure the page view mode list
        mOptionListView = view.findViewById(R.id.fragment_view_mode_picker_dialog_listview);
        mOptionListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        mOptionListView.setItemsCanFocus(false);
        // Show top and bottom dividers in ListView
        View v = new View(getActivity());
        v.setBackground(mOptionListView.getDivider());
        v.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, mOptionListView.getDividerHeight()));
        mOptionListView.addHeaderView(v);

        mListAdapter = new SeparatedListAdapter(getActivity());
        populateOptionsList(); // Add sections and items to adapter
        mOptionListView.setAdapter(mListAdapter);

        setListViewHeightBasedOnChildren(mOptionListView, (LinearLayout) view.findViewById(R.id.scroll_layout));

        mOptionListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view1, int position, long id) {
                switch ((int) id) {
                    case ITEM_ID_CONTINUOUS:
                        boolean checked = mOptionListView.isItemChecked(position);
                        setViewMode(checked);
                        mHasEventAction = true;
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEW_MODE,
                            AnalyticsParam.viewModeParam(checked ? AnalyticsHandlerAdapter.VIEW_MODE_VERTICAL_ON : AnalyticsHandlerAdapter.VIEW_MODE_VERTICAL_OFF));
                        break;
                    case ITEM_ID_RTLMODE:
                        mIsRtlMode = !mIsRtlMode;
                        if (mViewModePickerDialogListener != null) {
                            mViewModePickerDialogListener.onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_RTLMODE);
                        }
                        mHasEventAction = true;
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEW_MODE,
                            AnalyticsParam.viewModeParam(mIsRtlMode ? AnalyticsHandlerAdapter.VIEW_MODE_RTL_ON : AnalyticsHandlerAdapter.VIEW_MODE_RTL_OFF));
                        break;
                    case ITEM_ID_ROTATION:
                        if (!mIsReflowMode) {
                            if (mViewModePickerDialogListener != null) {
                                mViewModePickerDialogListener.onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_ROTATION_VALUE);
                            }
                            mHasEventAction = true;
                            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEW_MODE,
                                AnalyticsParam.viewModeParam(AnalyticsHandlerAdapter.VIEW_MODE_ROTATE));
                        }
                        break;
                    case ITEM_ID_USERCROP:
                        if (!mIsReflowMode) {
                            if (mViewModePickerDialogListener != null) {
                                mViewModePickerDialogListener.onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_USERCROP_VALUE);
                            }
                            mHasEventAction = true;
                            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEW_MODE,
                                AnalyticsParam.viewModeParam(AnalyticsHandlerAdapter.VIEW_MODE_CROP));
                            dismiss();
                        }
                        break;
                    default:
                        break;
                }
            }
        });

        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                dismiss();
            }
        });

        // Set the radio-button to the current view mode
        int checkedItem = -1;
        if (mIsReflowMode) {
            checkedItem = R.id.fragment_view_mode_button_reflow;
        } else {
            switch (mCurrentViewMode) {
                case SINGLE:
                    checkedItem = R.id.fragment_view_mode_button_single;
                    break;
                case SINGLE_CONT:
                    checkedItem = R.id.fragment_view_mode_button_single;
                    break;
                case FACING:
                    checkedItem = R.id.fragment_view_mode_button_facing;
                    break;
                case FACING_COVER:
                    checkedItem = R.id.fragment_view_mode_button_cover;
                    break;
                case FACING_CONT:
                    checkedItem = R.id.fragment_view_mode_button_facing;
                    break;
                case FACING_COVER_CONT:
                    checkedItem = R.id.fragment_view_mode_button_cover;
                    break;
            }
        }
        if (mIsReflowMode)
            listSwapFirstSecondViewModeOptions();
        setActiveMode(checkedItem);

        updateDialogLayout();

        final ScrollView sView = view.findViewById(R.id.viewMode_scrollView);
        try {
            sView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    //noinspection deprecation
                    try {
                        sView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } catch (Exception ignored) {
                    }

                    // Ready, move up
                    sView.fullScroll(View.FOCUS_UP);
                }
            });
        } catch (Exception ignored) {
        }

        return builder.create();
    }

    private boolean isContinuousMode() {
        // Check if the view-mode is continuous (vertical scrolling)
        boolean continuous = false;

        switch (mCurrentViewMode) {
            case SINGLE:
                continuous = false;
                break;
            case SINGLE_CONT:
                continuous = true;
                break;
            case FACING:
                continuous = false;
                break;
            case FACING_COVER:
                continuous = false;
                break;
            case FACING_CONT:
                continuous = true;
                break;
            case FACING_COVER_CONT:
                continuous = true;
                break;
        }

        return continuous;
    }

    private int getActiveMode() {
        for (int i = 0; i < mViewModeLayout.getChildCount() * 2; i++) {
            View view = ((TableRow) mViewModeLayout.getChildAt(i / 2)).getChildAt(i % 2);
            if (view.isActivated()) return view.getId();
        }
        return View.NO_ID;
    }

    private void setActiveMode(int id) {
        for (int i = 0; i < mViewModeLayout.getChildCount() * 2; i++) {
            View view = ((TableRow) mViewModeLayout.getChildAt(i / 2)).getChildAt(i % 2);
            view.setActivated(id == view.getId());
        }
    }

    private void setButtonChecked(@IdRes int buttonId, @DrawableRes int iconId, boolean checked) {
        Activity activity = getActivity();
        if (mColorModeLayout == null || activity == null) {
            return;
        }

        try {
            LayerDrawable layerDrawable = (LayerDrawable) AppCompatResources.getDrawable(activity, iconId);
            if (layerDrawable != null) {
                if (checked) {
                    GradientDrawable shape = (GradientDrawable) (layerDrawable.findDrawableByLayerId(R.id.selectable_shape));
                    if (shape != null) {
                        shape.mutate();
                        int w = (int) Utils.convDp2Pix(activity, 4.0f);
                        shape.setStroke(w, Utils.getAccentColor(activity));
                    }
                }
                ((ImageButton) mColorModeLayout.findViewById(buttonId)).setImageDrawable(layerDrawable);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Sets active color mode
     *
     * @param id resource id of selected color mode
     */
    protected void setActiveColorMode(@IdRes int id) {
        Context context = getContext();
        if(context == null) {
            return;
        }

        if (id == R.id.item_view_mode_picker_customcolor_button) {
            dismiss();
            CustomColorModeDialogFragment frag = CustomColorModeDialogFragment.newInstance(PdfViewCtrlSettingsManager.getCustomColorModeBGColor(context),
                PdfViewCtrlSettingsManager.getCustomColorModeTextColor(context));
            frag.setCustomColorModeSelectedListener(new CustomColorModeDialogFragment.CustomColorModeSelectedListener() {
                @Override
                public void onCustomColorModeSelected(int bgColor, int txtColor) {
                    if (mViewModePickerDialogListener != null) {
                        mViewModePickerDialogListener.onCustomColorModeSelected(bgColor, txtColor);
                    }
                }
            });
            frag.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                frag.show(fragmentManager, "custom_color_mode");
            }
        } else {
            setButtonChecked(R.id.item_view_mode_picker_daymode_button, R.drawable.ic_daymode_icon,
                id == R.id.item_view_mode_picker_daymode_button);
            setButtonChecked(R.id.item_view_mode_picker_nightmode_button, R.drawable.ic_nightmode_icon,
                id == R.id.item_view_mode_picker_nightmode_button);
            setButtonChecked(R.id.item_view_mode_picker_sepiamode_button, R.drawable.ic_sepiamode_icon,
                id == R.id.item_view_mode_picker_sepiamode_button);
        }
    }

    private void updateDialogLayout() {
        int activeId = getActiveMode();
        mIsReflowMode = activeId == R.id.fragment_view_mode_button_reflow;

        boolean continuous = isContinuousMode();

        for (int i = 0; i < mOptionListView.getCount(); i++) {
            int id = (int) mOptionListView.getItemIdAtPosition(i);
            switch (id) {
                case ITEM_ID_CONTINUOUS:
                    // Set vertical scrolling
                    mOptionListView.setItemChecked(i, continuous);
                    break;
                case ITEM_ID_RTLMODE:
                    // Set rtl mode
                    mOptionListView.setItemChecked(i, mIsRtlMode);
                    break;
            }
        }
        mListAdapter.notifyDataSetChanged();
    }

    private void listSwapFirstSecondViewModeOptions() {
        Map<String, Object> tmp = mViewModeOptionsList.get(0);
        mViewModeOptionsList.set(0, (mViewModeOptionsList.get(1)));
        mViewModeOptionsList.set(1, tmp);
    }

    private void setViewMode(boolean verticalScrolling) {
        int activeId = getActiveMode();
        if (activeId != R.id.fragment_view_mode_button_reflow
            && (int) mViewModeOptionsList.get(0).get(KEY_ITEM_ID) == ITEM_ID_TEXT_SIZE)
            listSwapFirstSecondViewModeOptions();
        if (verticalScrolling) {
            // Get the selected view mode (single, facing, cover)
            if (activeId == R.id.fragment_view_mode_button_single) {
                if (mViewModePickerDialogListener != null) {
                    mViewModePickerDialogListener.onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_CONTINUOUS_VALUE);
                }
                mCurrentViewMode = PDFViewCtrl.PagePresentationMode.SINGLE_CONT;
            } else if (activeId == R.id.fragment_view_mode_button_facing) {
                if (mViewModePickerDialogListener != null) {
                    mViewModePickerDialogListener.onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACING_CONT_VALUE);
                }
                mCurrentViewMode = PDFViewCtrl.PagePresentationMode.FACING_CONT;
            } else if (activeId == R.id.fragment_view_mode_button_cover) {
                if (mViewModePickerDialogListener != null) {
                    mViewModePickerDialogListener.onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACINGCOVER_CONT_VALUE);
                }
                mCurrentViewMode = PDFViewCtrl.PagePresentationMode.FACING_COVER_CONT;
            } else if (activeId == R.id.fragment_view_mode_button_reflow) {
                if ((int) mViewModeOptionsList.get(0).get(KEY_ITEM_ID) == ITEM_ID_CONTINUOUS)
                    listSwapFirstSecondViewModeOptions();
                if (mViewModePickerDialogListener != null) {
                    mViewModePickerDialogListener.onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_REFLOWMODE);
                }
            }
        } else {
            // Get the selected view mode (single, facing, cover)
            if (activeId == R.id.fragment_view_mode_button_single) {
                if (mViewModePickerDialogListener != null) {
                    mViewModePickerDialogListener.onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_SINGLEPAGE_VALUE);
                }
                mCurrentViewMode = PDFViewCtrl.PagePresentationMode.SINGLE;
            } else if (activeId == R.id.fragment_view_mode_button_facing) {
                if (mViewModePickerDialogListener != null) {
                    mViewModePickerDialogListener.onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACING_VALUE);
                }
                mCurrentViewMode = PDFViewCtrl.PagePresentationMode.FACING;
            } else if (activeId == R.id.fragment_view_mode_button_cover) {
                if (mViewModePickerDialogListener != null) {
                    mViewModePickerDialogListener.onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_VIEWMODE_FACINGCOVER_VALUE);
                }
                mCurrentViewMode = PDFViewCtrl.PagePresentationMode.FACING_COVER;
            } else if (activeId == R.id.fragment_view_mode_button_reflow) {
                if ((int) mViewModeOptionsList.get(0).get(KEY_ITEM_ID) == ITEM_ID_CONTINUOUS)
                    listSwapFirstSecondViewModeOptions();
                if (mViewModePickerDialogListener != null) {
                    mViewModePickerDialogListener.onViewModeSelected(PdfViewCtrlSettingsManager.KEY_PREF_REFLOWMODE);
                }
            }
        }
    }

    /**
     * Listener interface for view mode dialog events.
     */
    public interface ViewModePickerDialogFragmentListener {
        /**
         * Called when a view mode has been selected.
         *
         * @param viewMode The selected view mode
         */
        void onViewModeSelected(String viewMode);

        /**
         * Called when a color mode has been selected.
         *
         * @param colorMode The color mode
         * @return True if the dialog should be dismissed
         */
        boolean onViewModeColorSelected(int colorMode);

        /**
         * Called when a custom color mode is selected
         *
         * @param bgColor  The selected background color
         * @param txtColor The selected text color
         * @return True if the dialog should be dismissed
         */
        boolean onCustomColorModeSelected(int bgColor, int txtColor);

        /**
         * Called when the dialog has been dismissed.
         */
        void onViewModePickerDialogFragmentDismiss();

        /**
         * The implementation should zoom in/out reflow
         *
         * @param flagZoomIn True if zoom in; False if zoom out
         * @return The text size raging from 0 to 100
         */
        int onReflowZoomInOut(boolean flagZoomIn);

        /**
         * The implementation should check tab conversion and shows the alert if needed.
         *
         * @param messageID      The message ID
         * @param allowConverted True if conversion is allowed
         * @return True if handled
         */
        boolean checkTabConversionAndAlert(int messageID, boolean allowConverted);
    }

    /**
     * An array adapter for view mode entries
     */
    protected class ViewModeEntryAdapter extends ArrayAdapter<Map<String, Object>> {
        private List<Map<String, Object>> mEntries;
        private ColorStateList mIconTintList;
        private ColorStateList mTextTintList;

        /**
         * Class constructor
         *
         * @param context The context
         * @param entries list of view mode entries
         */
        ViewModeEntryAdapter(Context context, List<Map<String, Object>> entries) {
            super(context, 0, entries);
            this.mEntries = entries;
            this.mIconTintList = AppCompatResources.getColorStateList(getContext(), R.color.selector_color);
            this.mTextTintList = AppCompatResources.getColorStateList(getContext(), R.color.selector_color);
        }

        /**
         * Sets view mode entries
         *
         * @param list view mode entries
         */
        public void setEntries(List<Map<String, Object>> list) {
            mEntries = list;
        }

        /**
         * Overload implementation of {@link ArrayAdapter#getView(int, View, ViewGroup)}
         *
         * @return View mode entry view
         */
        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            final ViewHolder holder;

            Map<String, Object> map = mEntries.get(position);
            int id = (Integer) map.get(KEY_ITEM_ID);

            if (convertView == null || convertView.getTag() == null) {
                holder = new ViewHolder();
                if (id == ITEM_ID_COLORMODE) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_view_mode_color_mode_row, parent, false);
                    RelativeLayout layoutView = (RelativeLayout) convertView;
                    LinearLayout colorBtnLayout = layoutView.findViewById(R.id.item_view_mode_picker_modebtn_layout);
                    for (int i = 0; i < colorBtnLayout.getChildCount(); i++) {
                        View child = colorBtnLayout.getChildAt(i);
                        if (child instanceof ImageButton) {
                            child.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    int id1 = v.getId();
                                    setActiveColorMode(id1);

                                    if (mViewModePickerDialogListener != null) {
                                        if (id1 == R.id.item_view_mode_picker_daymode_button) {
                                            mHasEventAction = true;
                                            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEW_MODE,
                                                AnalyticsParam.viewModeParam(AnalyticsHandlerAdapter.VIEW_MODE_DAY_MODE,
                                                    PdfViewCtrlSettingsManager.getColorMode(getContext()) == PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_NORMAL));
                                            if (mViewModePickerDialogListener.onViewModeColorSelected(
                                                PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_NORMAL)) {
                                                dismiss();
                                            }
                                        } else if (id1 == R.id.item_view_mode_picker_nightmode_button) {
                                            mHasEventAction = true;
                                            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEW_MODE,
                                                AnalyticsParam.viewModeParam(AnalyticsHandlerAdapter.VIEW_MODE_NIGHT_MODE,
                                                    PdfViewCtrlSettingsManager.getColorMode(getContext()) == PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_NIGHT));
                                            if (mViewModePickerDialogListener.onViewModeColorSelected(
                                                PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_NIGHT)) {
                                                dismiss();
                                            }
                                        } else if (id1 == R.id.item_view_mode_picker_sepiamode_button) {
                                            mHasEventAction = true;
                                            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEW_MODE,
                                                AnalyticsParam.viewModeParam(AnalyticsHandlerAdapter.VIEW_MODE_SEPIA_MODE,
                                                    PdfViewCtrlSettingsManager.getColorMode(getContext()) == PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_SEPIA));
                                            if (mViewModePickerDialogListener.onViewModeColorSelected(
                                                PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_SEPIA)) {
                                                dismiss();
                                            }
                                        } else if (id1 == R.id.item_view_mode_picker_customcolor_button) {
                                            mHasEventAction = true;
                                            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEW_MODE,
                                                AnalyticsParam.viewModeParam(AnalyticsHandlerAdapter.VIEW_MODE_CUSTOM_MODE,
                                                    PdfViewCtrlSettingsManager.getColorMode(getContext()) == PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_CUSTOM));
                                        }
                                    }
                                }
                            });
                        }
                    }
                    applyTintList((android.support.v7.widget.AppCompatImageView) layoutView.findViewById(R.id.item_view_mode_picker_color_list_icon), mIconTintList);
                    mColorModeLayout = layoutView;
                    convertView.setTag(holder);
                } else {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.listview_item_view_mode_picker_list, parent, false);

                    holder.icon = convertView.findViewById(R.id.item_view_mode_picker_list_icon);
                    holder.text = convertView.findViewById(R.id.item_view_mode_picker_list_text);
                    holder.radioButton = convertView.findViewById(R.id.item_view_mode_picker_list_radiobutton);
                    holder.switchButton = convertView.findViewById(R.id.item_view_mode_picker_list_switch);
                    holder.sizeLayout = convertView.findViewById(R.id.item_view_mode_picker_list_size_layout);
                    holder.decButton = convertView.findViewById(R.id.item_view_mode_picker_list_dec);
                    holder.sizeText = convertView.findViewById(R.id.item_view_mode_picker_list_size_text);
                    holder.incButton = convertView.findViewById(R.id.item_view_mode_picker_list_inc);

                    convertView.setTag(holder);
                }
                try {
                    int res = -1;
                    int currentColorMode = PdfViewCtrlSettingsManager.getColorMode(getContext());
                    if (currentColorMode == PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_NIGHT) {
                        res = R.id.item_view_mode_picker_nightmode_button;
                    } else if (currentColorMode == PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_SEPIA) {
                        res = R.id.item_view_mode_picker_sepiamode_button;
                    } else if (currentColorMode == PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_NORMAL) {
                        res = R.id.item_view_mode_picker_daymode_button;
                    }
                    if (res != -1) {
                        setActiveColorMode(res);
                    }
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            } else {
                holder = (ViewHolder) convertView.getTag();
            }


            switch (id) {
                case ITEM_ID_BLANK:
                    return new View(getContext());
                case ITEM_ID_CONTINUOUS:
                    if (mIsReflowMode) return new View(getContext());
                    break;
                case ITEM_ID_TEXT_SIZE:
                    if (!mIsReflowMode) return new View(getContext());
                    break;
                case ITEM_ID_ROTATION:
                    holder.icon.setEnabled(!mIsReflowMode);
                    holder.text.setEnabled(!mIsReflowMode);
                    break;
                case ITEM_ID_USERCROP:
                    holder.icon.setEnabled(!mIsReflowMode);
                    holder.text.setEnabled(!mIsReflowMode);
                    break;
                default:
                    break;
            }

            if (id == ITEM_ID_COLORMODE) {
                return convertView;
            }

            Drawable icon = (Drawable) map.get(KEY_ITEM_ICON);
            holder.icon.setImageDrawable(icon);
            applyTintList(holder.icon, mIconTintList);

            holder.text.setText((String) map.get(KEY_ITEM_TEXT));
            holder.text.setTextColor(mTextTintList);

            int controlType = (Integer) map.get(KEY_ITEM_CONTROL);
            holder.radioButton.setVisibility(controlType == CONTROL_TYPE_RADIO ? View.VISIBLE : View.GONE);
            holder.switchButton.setVisibility(controlType == CONTROL_TYPE_SWITCH ? View.VISIBLE : View.GONE);
            holder.sizeLayout.setVisibility(controlType == CONTROL_TYPE_SIZE ? View.VISIBLE : View.GONE);

            if (controlType == CONTROL_TYPE_SIZE) {
                // Apply icon tints
                applyTintList(holder.decButton, mIconTintList);
                applyTintList(holder.incButton, mIconTintList);

                if (mReflowTextSize == ReflowPagerAdapter.TH_MIN_SCAlE) {
                    holder.decButton.setEnabled(false);
                }
                if (mReflowTextSize == ReflowPagerAdapter.TH_MAX_SCAlE) {
                    holder.incButton.setEnabled(false);
                }

                holder.decButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mViewModePickerDialogListener != null) {
                            mReflowTextSize = mViewModePickerDialogListener.onReflowZoomInOut(false);
                        }
                        mHasEventAction = true;
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEW_MODE,
                            AnalyticsParam.viewModeParam(AnalyticsHandlerAdapter.VIEW_MODE_REFLOW_ZOOM_OUT));
                        if (mReflowTextSize == ReflowPagerAdapter.TH_MIN_SCAlE) {
                            // Disable this button
                            holder.decButton.setEnabled(false);
                        } else {
                            // Enable another button
                            holder.incButton.setEnabled(true);
                        }

                        holder.sizeText.setText(String.format(Locale.getDefault(), "%d%%", mReflowTextSize));
                    }
                });
                holder.incButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mViewModePickerDialogListener != null) {
                            mReflowTextSize = mViewModePickerDialogListener.onReflowZoomInOut(true);
                        }
                        mHasEventAction = true;
                        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_VIEW_MODE,
                            AnalyticsParam.viewModeParam(AnalyticsHandlerAdapter.VIEW_MODE_REFLOW_ZOOM_IN));
                        if (mReflowTextSize == ReflowPagerAdapter.TH_MAX_SCAlE) {
                            // Disable this button
                            holder.incButton.setEnabled(false);
                        } else {
                            // Enable another button
                            holder.decButton.setEnabled(true);
                        }

                        holder.sizeText.setText(String.format(Locale.getDefault(), "%d%%", mReflowTextSize));
                    }
                });
                holder.sizeText.setText(String.format(Locale.getDefault(), "%d%%", mReflowTextSize));
            }

            return convertView;
        }

        /**
         * Gets view mode entry id
         *
         * @param position specifies position
         * @return view mode entry id
         */
        @Override
        public long getItemId(int position) {
            Map<String, Object> map = this.mEntries.get(position);
            return (Integer) map.get(KEY_ITEM_ID);
        }

        private void applyTintList(ImageView imageView, ColorStateList tintList) {
            if (imageView == null) {
                return;
            }
            Drawable icon = imageView.getDrawable();
            if (icon != null && icon.getConstantState() != null) {
                try {
                    icon = DrawableCompat.wrap(icon.getConstantState().newDrawable()).mutate();
                    DrawableCompat.setTintList(icon, tintList);
                } catch (NullPointerException ignored) {

                }
            }
            imageView.setImageDrawable(icon);
        }

        private class ViewHolder {
            ImageView icon;
            TextView text;
            RadioButton radioButton;
            com.pdftron.pdf.widget.InertSwitch switchButton;
            LinearLayout sizeLayout;
            ImageButton decButton;
            TextView sizeText;
            ImageButton incButton;
        }
    }

    /**
     * A list adapter that can seperate list with header.
     */
    // see: http://jsharkey.org/blog/2008/08/18/separating-lists-with-headers-in-android-09/
    protected class SeparatedListAdapter extends BaseAdapter {

        static final int TYPE_SECTION_HEADER = -1;

        final Map<String, Adapter> mSections;
        final ArrayAdapter<String> mHeaders;

        /**
         * Class constructor
         *
         * @param context The context
         */
        SeparatedListAdapter(Context context) {
            mSections = new LinkedHashMap<>();
            mHeaders = new ArrayAdapter<>(context, R.layout.listview_header_view_mode_picker_list);
        }

        void addSection(String section, Adapter adapter) {
            mHeaders.add(section);
            mSections.put(section, adapter);
        }

        /**
         * Gets item based on position
         *
         * @param position The position
         * @return item
         */
        @Override
        public Object getItem(int position) {
            for (String section : mSections.keySet()) {
                Adapter adapter = mSections.get(section);
                int size = adapter.getCount() + 1;

                // Check if position is inside this section
                if (position == 0) return section;
                if (position < size) return adapter.getItem(position - 1);

                // otherwise, jump into next section
                position -= size;
            }
            return null;
        }

        /**
         * Gets data set count
         *
         * @return data set count
         */
        @Override
        public int getCount() {
            // total together all sections, plus one for each section header
            int total = 0;
            for (Adapter adapter : mSections.values()) {
                total += adapter.getCount() + 1;
            }
            return total;
        }

        /**
         * <p>
         * Returns the number of types of Views that will be created by
         * {@link #getView}. Each type represents a set of views that can be
         * converted in {@link #getView}. If the adapter always returns the same
         * type of View for all items, this method should return 1.
         * </p>
         * <p>
         * This method will only be called when the adapter is set on the {@link AdapterView}.
         * </p>
         *
         * @return The number of types of Views that will be created by this adapter
         */
        @Override
        public int getViewTypeCount() {
            // assume that headers count as one, then total all sections
            int total = 1;
            for (Adapter adapter : mSections.values()) {
                total += adapter.getViewTypeCount();
            }
            return total;
        }

        /**
         * Gets item view type based on position
         *
         * @param position The specified position
         * @return {@link #TYPE_SECTION_HEADER} if position is 0.
         * Else return section item view type
         */
        @Override
        public int getItemViewType(int position) {
            int type = 1;
            for (String section : mSections.keySet()) {
                Adapter adapter = mSections.get(section);
                int size = adapter.getCount() + 1;

                // check if position is inside this section
                if (position == 0) return TYPE_SECTION_HEADER;
                if (position < size) return type + adapter.getItemViewType(position - 1);

                // otherwise jump into next section
                position -= size;
                type += adapter.getViewTypeCount();
            }
            return -1;
        }

        /**
         * Whether specified position item is enabled
         *
         * @param position The specified position
         * @return true if item view type is not {@link #TYPE_SECTION_HEADER}
         */
        @Override
        public boolean isEnabled(int position) {
            return (getItemViewType(position) != TYPE_SECTION_HEADER);
        }

        /**
         * Overload implementation of {@link BaseAdapter#getView(int, View, ViewGroup)}
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (parent == null) {
                return null;
            }
            int sectionNum = 0;
            for (String section : mSections.keySet()) {
                Adapter adapter = mSections.get(section);
                int size = adapter.getCount() + 1;

                // check if position is inside this section
                if (position == 0) {
                    if (!Utils.isNullOrEmpty(section)) {
                        return mHeaders.getView(sectionNum, convertView, parent);
                    } else {
                        return new View(parent.getContext());
                    }
                }
                if (position < size) return adapter.getView(position - 1, convertView, parent);

                // otherwise jump into next section
                position -= size;
                sectionNum++;
            }
            return null;
        }

        /**
         * Gets specified position item id
         *
         * @param position The specified position
         * @return item id
         */
        @Override
        public long getItemId(int position) {
            int id = position;
            for (String section : mSections.keySet()) {
                Adapter adapter = mSections.get(section);
                int size = adapter.getCount() + 1;

                // Check if position is inside this section
                if (id == 0) return position;
                if (id < size) return adapter.getItemId(id - 1);

                // otherwise, jump into next section
                id -= size;
            }
            return position;
        }
    }
}
