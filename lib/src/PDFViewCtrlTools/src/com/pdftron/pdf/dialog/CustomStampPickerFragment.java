//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.pdftron.pdf.adapter.CustomStampAdapter;
import com.pdftron.pdf.interfaces.OnCustomStampChangedListener;
import com.pdftron.pdf.interfaces.OnCustomStampSelectedListener;
import com.pdftron.pdf.model.CustomStampOption;
import com.pdftron.pdf.model.CustomStampPreviewAppearance;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.ToolbarActionMode;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.ItemSelectionHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import com.pdftron.sdf.Obj;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import co.paulburke.android.itemtouchhelperdemo.helper.SimpleItemTouchHelperCallback;

public class CustomStampPickerFragment extends Fragment implements OnCustomStampChangedListener {

    public final static String TAG = CustomStampPickerFragment.class.getName();

    private CustomStampPreviewAppearance[] mCustomStampPreviewAppearances;
    private TextView mEmptyStampTextView;
    private SimpleRecyclerView mRecyclerView;
    private CustomStampAdapter mCustomStampAdapter;
    private ItemTouchHelper mItemTouchHelper;
    private ItemSelectionHelper mItemSelectionHelper;
    private ToolbarActionMode mActionMode;
    private Toolbar mToolbar;
    private Toolbar mCabToolbar;
    private MenuItem mMenuItemModify, mMenuItemDuplicate, mMenuItemDelete;

    private OnCustomStampSelectedListener mOnCustomStampSelectedListener;

    public static CustomStampPickerFragment newInstance(@NonNull CustomStampPreviewAppearance[] customStampPreviewAppearances) {
        CustomStampPickerFragment fragment = new CustomStampPickerFragment();
        Bundle bundle = new Bundle();
        CustomStampPreviewAppearance.putCustomStampAppearancesToBundle(bundle, customStampPreviewAppearances);
        fragment.setArguments(bundle);
        return fragment;
    }

    /**
     * Sets the listener to {@link OnCustomStampSelectedListener}.
     *
     * @param listener The listener
     */
    public void setOnCustomStampSelectedListener(@Nullable OnCustomStampSelectedListener listener) {
        mOnCustomStampSelectedListener = listener;
    }

    /**
     * Sets the main and cab toolbars.
     *
     * @param toolbar    The toolbar with one action called Edit
     * @param cabToolbar The cab toolbar with all possible actions
     */
    public void setToolbars(@NonNull Toolbar toolbar, @NonNull Toolbar cabToolbar) {
        mToolbar = toolbar;
        mCabToolbar = cabToolbar;
        updateUIVisibility();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        if (bundle != null) {
            mCustomStampPreviewAppearances = CustomStampPreviewAppearance.getCustomStampAppearancesFromBundle(bundle);
        }

        View view = inflater.inflate(R.layout.fragment_custom_rubber_stamp_picker, container, false);
        FloatingActionButton fab = view.findViewById(R.id.add_custom_stamp_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager == null) {
                    return;
                }
                CreateCustomStampDialogFragment fragment = CreateCustomStampDialogFragment.newInstance(mCustomStampPreviewAppearances);
                fragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
                fragment.show(fragmentManager, CreateCustomStampDialogFragment.TAG);
                fragment.setOnCustomStampChangedListener(CustomStampPickerFragment.this);
                finishActionMode();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mToolbar != null) {
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (mToolbar == null || mCabToolbar == null) {
                        return false;
                    }

                    if (item.getItemId() == R.id.controls_action_edit) {
                        // Start edit-mode
                        mActionMode = new ToolbarActionMode(getActivity(), mCabToolbar);
                        mActionMode.startActionMode(mActionModeCallback);
                        return true;
                    }
                    return false;
                }
            });

        }

        mRecyclerView = view.findViewById(R.id.stamp_list);
        mRecyclerView.initView(2);

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);
        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {

            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                if (mActionMode == null) {
                    if (mOnCustomStampSelectedListener != null) {
                        Obj stampObj = CustomStampOption.getCustomStampObj(view.getContext(), position);
                        mOnCustomStampSelectedListener.onCustomStampSelected(stampObj);
                    }
                } else {
                    mItemSelectionHelper.setItemChecked(position, !mItemSelectionHelper.isItemChecked(position));
                    mActionMode.invalidate();
                }
            }

        });
        itemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(RecyclerView recyclerView, View v, final int position, final long id) {
                if (mActionMode == null) {
                    mItemSelectionHelper.setItemChecked(position, true);
                    mActionMode = new ToolbarActionMode(getActivity(), mCabToolbar);
                    mActionMode.startActionMode(mActionModeCallback);
                } else {
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

        mItemSelectionHelper = new ItemSelectionHelper();
        mItemSelectionHelper.attachToRecyclerView(mRecyclerView);
        mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_MULTIPLE);

        mCustomStampAdapter = new CustomStampAdapter(view.getContext(), mItemSelectionHelper);
        mCustomStampAdapter.registerAdapterDataObserver(mItemSelectionHelper.getDataObserver());
        mRecyclerView.setAdapter(mCustomStampAdapter);

        mItemTouchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(mCustomStampAdapter, 2, false, false));
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        mEmptyStampTextView = view.findViewById(R.id.new_custom_stamp_guide_text_view);

        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK
                    && onBackPressed();
            }
        });
    }

    @Override
    public void onCustomStampCreated(@Nullable Bitmap bitmap) {
        if (mCustomStampAdapter == null) {
            return;
        }
        mCustomStampAdapter.add(bitmap);
        mCustomStampAdapter.notifyItemInserted(mCustomStampAdapter.getItemCount());
        updateUIVisibility();
    }

    @Override
    public void onCustomStampUpdated(@Nullable Bitmap bitmap, int index) {
        if (mCustomStampAdapter == null) {
            return;
        }
        mCustomStampAdapter.onCustomStampUpdated(bitmap, index);
    }

    private void updateUIVisibility() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        int count = CustomStampOption.getCustomStampsCount(context);
        if (mEmptyStampTextView != null) {
            mEmptyStampTextView.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        }
        if (mToolbar != null) {
            MenuItem menuEdit = mToolbar.getMenu().findItem(R.id.controls_action_edit);
            menuEdit.setVisible(count != 0);
            if (count == 0) {
                finishActionMode();
            }
        }
    }

    private ToolbarActionMode.Callback mActionModeCallback = new ToolbarActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ToolbarActionMode mode, Menu menu) {
            mode.inflateMenu(R.menu.cab_controls_fragment_rubber_stamp);
            mMenuItemModify = menu.findItem(R.id.controls_rubber_stamp_action_modify);
            mMenuItemDuplicate = menu.findItem(R.id.controls_rubber_stamp_action_duplicate);
            mMenuItemDelete = menu.findItem(R.id.controls_rubber_stamp_action_delete);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ToolbarActionMode mode, Menu menu) {
            boolean isEnabled;
            if (mMenuItemModify != null) {
                isEnabled = mItemSelectionHelper.getCheckedItemCount() == 1;
                mMenuItemModify.setEnabled(isEnabled);
                if (mMenuItemModify.getIcon() != null) {
                    mMenuItemModify.getIcon().mutate().setAlpha(isEnabled ? 255 : 150);
                }
            }
            if (mMenuItemDuplicate != null) {
                isEnabled = mItemSelectionHelper.getCheckedItemCount() == 1;
                mMenuItemDuplicate.setEnabled(isEnabled);
                if (mMenuItemDuplicate.getIcon() != null) {
                    mMenuItemDuplicate.getIcon().mutate().setAlpha(isEnabled ? 255 : 150);
                }
            }
            if (mMenuItemDelete != null) {
                isEnabled = mItemSelectionHelper.getCheckedItemCount() > 0;
                mMenuItemDelete.setEnabled(isEnabled);
                if (mMenuItemDelete.getIcon() != null) {
                    mMenuItemDelete.getIcon().mutate().setAlpha(isEnabled ? 255 : 150);
                }
            }

            if (Utils.isTablet(getContext()) || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mode.setTitle(getString(R.string.controls_thumbnails_view_selected,
                    Utils.getLocaleDigits(Integer.toString(mItemSelectionHelper.getCheckedItemCount()))));
            } else {
                mode.setTitle(Utils.getLocaleDigits(Integer.toString(mItemSelectionHelper.getCheckedItemCount())));
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ToolbarActionMode mode, MenuItem item) {
            final Context context = getContext();
            View view = getView();
            FragmentManager fragmentManager = getFragmentManager();
            if (context == null || view == null || fragmentManager == null) {
                return false;
            }

            SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();
            int count = selectedItems.size();
            final List<Integer> indexes = new ArrayList<>();
            int lastSelectedPosition = -1;
            for (int i = 0; i < count; ++i) {
                if (selectedItems.valueAt(i)) {
                    indexes.add(selectedItems.keyAt(i));
                    lastSelectedPosition = selectedItems.keyAt(i);
                }
            }

            if (lastSelectedPosition == -1 || indexes.size() == 0) {
                return false;
            }

            // remove repeated indexes and then sort them in ascending order
            Set<Integer> hs = new HashSet<>(indexes);
            indexes.clear();
            indexes.addAll(hs);
            Collections.sort(indexes);

            int id = item.getItemId();
            if (id == R.id.controls_rubber_stamp_action_modify) {
                try {
                    CreateCustomStampDialogFragment fragment = CreateCustomStampDialogFragment.newInstance(
                        mCustomStampPreviewAppearances, lastSelectedPosition);
                    fragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
                    fragment.show(fragmentManager, CreateCustomStampDialogFragment.TAG);
                    fragment.setOnCustomStampChangedListener(CustomStampPickerFragment.this);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            } else if (id == R.id.controls_rubber_stamp_action_duplicate) {
                CustomStampOption.duplicateCustomStamp(context, lastSelectedPosition);
                Bitmap bitmap = mCustomStampAdapter.getItem(lastSelectedPosition);
                mCustomStampAdapter.insert(bitmap, lastSelectedPosition + 1);
                mCustomStampAdapter.notifyItemInserted(lastSelectedPosition + 1);
            } else if (id == R.id.controls_rubber_stamp_action_delete) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.custom_stamp_dialog_delete_message)
                    .setTitle(R.string.custom_stamp_dialog_delete_title)
                    .setPositiveButton(R.string.tools_misc_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CustomStampOption.removeCustomStamps(context, indexes);
                            for (int i = indexes.size() - 1; i >= 0; --i) {
                                int index = indexes.get(i);
                                mCustomStampAdapter.removeAt(index);
                                mCustomStampAdapter.notifyItemRemoved(index);
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .create()
                    .show();
            }

            clearSelectedList();
            updateUIVisibility();

            return true;
        }

        @Override
        public void onDestroyActionMode(ToolbarActionMode mode) {
            mActionMode = null;
            clearSelectedList();
        }
    };

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

}
