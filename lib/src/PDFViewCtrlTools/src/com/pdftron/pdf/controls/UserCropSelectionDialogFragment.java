//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//-------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The UserCropSelectionDialogFragment shows three crop mode options.
 * Activities that contain this fragment must implement the
 * {@link UserCropSelectionDialogFragment.UserCropSelectionDialogFragmentListener} interface
 * to handle interaction events.
 * Use the {@link UserCropSelectionDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserCropSelectionDialogFragment extends DialogFragment {

    /**
     * Auto crop mode
     */
    public static final int MODE_AUTO_CROP    = 0;
    /**
     * Manual crop mode
     */
    public static final int MODE_MANUAL_CROP  = 1;
    /**
     * Reset crop mode
     */
    public static final int MODE_RESET_CROP   = 2;

    private static final String KEY_ITEM_TEXT  = "item_crop_mode_picker_list_text";

    private UserCropSelectionDialogFragmentListener mUserCropSelectionDialogFragmentListener = null;
    private boolean mHasAction = false;

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface UserCropSelectionDialogFragmentListener {
        /**
         * Called when a user crop method has been selected.
         *
         * @param cropMode The crop mode. The possible values are
         *                 {@link #MODE_AUTO_CROP},
         *                 {@link #MODE_MANUAL_CROP},
         *                 {@link #MODE_RESET_CROP}
         */
        void onUserCropMethodSelected(int cropMode);

        /**
         * Called when the dialog for user crop selection has been dismissed.
         */
        void onUserCropSelectionDialogFragmentDismiss();
    }

    /**
     * @return new instance of this class
     */
    public static UserCropSelectionDialogFragment newInstance() {
        return new UserCropSelectionDialogFragment();
    }

    public UserCropSelectionDialogFragment() {
        // Required empty public constructor
    }

    /**
     * The overload implementation of {@link DialogFragment#onDismiss(DialogInterface)}.
     */
    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mUserCropSelectionDialogFragmentListener != null) {
            mUserCropSelectionDialogFragmentListener.onUserCropSelectionDialogFragmentDismiss();
        }
        super.onDismiss(dialog);
        if (!mHasAction) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CROP_PAGES,
                AnalyticsParam.cropPageParam(AnalyticsHandlerAdapter.CROP_PAGE_NO_ACTION));
        }
    }

    /**
     * The overload implementation of {@link DialogFragment#onCreateDialog(Bundle)}.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_user_crop_selection_dialog, null);
        builder.setView(view);

        ListView cropModeListView = (ListView) view.findViewById(R.id.fragment_user_crop_slection_dialog_listview);
        cropModeListView.setItemsCanFocus(false);

        CropModeEntryAdapter cropModeEntryAdapter = new CropModeEntryAdapter(getActivity(), getViewModeList());
        cropModeListView.setAdapter(cropModeEntryAdapter);

        cropModeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mHasAction = true;
                dismiss();
                if (mUserCropSelectionDialogFragmentListener != null) {
                    mUserCropSelectionDialogFragmentListener.onUserCropMethodSelected(position);
                }
                if (position == MODE_AUTO_CROP) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CROP_PAGES,
                        AnalyticsParam.cropPageParam(AnalyticsHandlerAdapter.CROP_PAGE_AUTO));
                } else if (position == MODE_RESET_CROP) {
                    AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_CROP_PAGES,
                        AnalyticsParam.cropPageParam(AnalyticsHandlerAdapter.CROP_PAGE_REMOVE));
                }
            }
        });

        return builder.create();
    }

    /**
     * Sets the UserCropSelectionDialogFragmentListener listener
     * @param listener The listener
     */
    public void setUserCropSelectionDialogFragmentListener(UserCropSelectionDialogFragmentListener listener) {
        mUserCropSelectionDialogFragmentListener = listener;
    }

    private class CropModeEntryAdapter extends ArrayAdapter<HashMap<String, Object>> {
        private Context mContext;
        private List<HashMap<String, Object>> mEntries;

        CropModeEntryAdapter(Context context, List<HashMap<String, Object>> entries) {
            super(context, 0, entries);
            this.mContext = context;
            this.mEntries = entries;
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
            final ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_item_view_mode_picker_list, parent, false);

                holder = new ViewHolder();
                holder.icon = convertView.findViewById(R.id.item_view_mode_picker_list_icon);
                holder.text = convertView.findViewById(R.id.item_view_mode_picker_list_text);
                holder.radioButton = convertView.findViewById(R.id.item_view_mode_picker_list_radiobutton);
                holder.switchButton = convertView.findViewById(R.id.item_view_mode_picker_list_switch);

                convertView.setTag(holder);

            } else  {
                holder = (ViewHolder) convertView.getTag();
            }

            HashMap<String, Object> map = this.mEntries.get(position);
            holder.icon.setVisibility(View.GONE);
            holder.text.setText((String) map.get(KEY_ITEM_TEXT));
            holder.radioButton.setVisibility(View.GONE);
            holder.switchButton.setVisibility(View.GONE);

            return convertView;
        }

        private class ViewHolder {
            protected ImageView icon;
            protected TextView text;
            protected RadioButton radioButton;
            protected com.pdftron.pdf.widget.InertSwitch switchButton;
        }
    }

    private List<HashMap<String, Object>> getViewModeList() {
        List<HashMap<String, Object>> viewModesList = new ArrayList<>();

        HashMap<String, Object> mapSinglePage = new HashMap<>();
        mapSinglePage.put(KEY_ITEM_TEXT, getActivity().getResources().getString(R.string.user_crop_selection_auto_crop));
        viewModesList.add(mapSinglePage);

        HashMap<String, Object> mapFacing = new HashMap<>();
        mapFacing.put(KEY_ITEM_TEXT, getActivity().getResources().getString(R.string.user_crop_selection_manual_crop));
        viewModesList.add(mapFacing);

        HashMap<String, Object> mapFacingCover = new HashMap<>();
        mapFacingCover.put(KEY_ITEM_TEXT, getActivity().getResources().getString(R.string.user_crop_selection_remove_crop));
        viewModesList.add(mapFacingCover);

        return viewModesList;
    }
}
