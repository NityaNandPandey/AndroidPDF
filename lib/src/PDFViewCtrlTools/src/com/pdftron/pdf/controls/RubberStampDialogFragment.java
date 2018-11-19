//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pdftron.pdf.adapter.StampFragmentAdapter;
import com.pdftron.pdf.interfaces.OnDialogDismissListener;
import com.pdftron.pdf.interfaces.OnRubberStampSelectedListener;
import com.pdftron.pdf.model.CustomStampPreviewAppearance;
import com.pdftron.pdf.model.StandardStampPreviewAppearance;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.sdf.Obj;

/**
 * A dialog that enables the user to select a stamp
 */
public class RubberStampDialogFragment extends CustomSizeDialogFragment {

    public final static String TAG = RubberStampDialogFragment.class.getName();

    private final static String PREF_LAST_SELECTED_TAB_IN_RUBBER_STAMP_DIALOG = "last_selected_tab_in_rubber_stamp_dialog";
    private final static String BUNDLE_TARGET_POINT_X = "target_point_x";
    private final static String BUNDLE_TARGET_POINT_Y = "target_point_y";

    private PointF mTargetPoint; // keep this in the fragment so that can retrieve it when the fragment is re-created
    private OnRubberStampSelectedListener mOnRubberStampSelectedListener;
    private OnDialogDismissListener mOnDialogDismissListener;

    public static RubberStampDialogFragment newInstance(@NonNull PointF targetPoint, StandardStampPreviewAppearance[] standardStampPreviewAppearance, CustomStampPreviewAppearance[] customStampPreviewAppearances) {
        RubberStampDialogFragment fragment = new RubberStampDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putFloat(BUNDLE_TARGET_POINT_X, targetPoint.x);
        bundle.putFloat(BUNDLE_TARGET_POINT_Y, targetPoint.y);
        StandardStampPreviewAppearance.putStandardStampAppearancesToBundle(bundle, standardStampPreviewAppearance);
        CustomStampPreviewAppearance.putCustomStampAppearancesToBundle(bundle, customStampPreviewAppearances);
        fragment.setArguments(bundle);
        return fragment;
    }

    public RubberStampDialogFragment() {
    }

    /**
     * @return The target point for adding rubber stamp
     */
    public PointF getTargetPoint() {
        return mTargetPoint;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arg = getArguments();
        if (arg != null) {
            float x = arg.getFloat(BUNDLE_TARGET_POINT_X);
            float y = arg.getFloat(BUNDLE_TARGET_POINT_Y);
            mTargetPoint = new PointF(x, y);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rubber_stamp_dialog, container);

        Toolbar toolbar = view.findViewById(R.id.stamp_dialog_toolbar);
        Toolbar cabToolbar = view.findViewById(R.id.stamp_dialog_toolbar_cab);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        toolbar.inflateMenu(R.menu.controls_fragment_edit_toolbar);

        Bundle bundle = getArguments();
        StandardStampPreviewAppearance[] standardStampPreviewAppearances = null;
        CustomStampPreviewAppearance[] customStampPreviewAppearances = null;
        if (bundle != null) {
            standardStampPreviewAppearances = StandardStampPreviewAppearance.getStandardStampAppearancesFromBundle(bundle);
            customStampPreviewAppearances = CustomStampPreviewAppearance.getCustomStampAppearancesFromBundle(bundle);
        }
        ViewPager pager = view.findViewById(R.id.stamp_dialog_view_pager);
        StampFragmentAdapter adapter = new StampFragmentAdapter(getChildFragmentManager(),
            getString(R.string.standard), getString(R.string.custom), standardStampPreviewAppearances, customStampPreviewAppearances, toolbar, cabToolbar);
        adapter.setOnRubberStampSelectedListener(new OnRubberStampSelectedListener() {
            @Override
            public void onRubberStampSelected(@NonNull String stampLabel) {
                if (mOnRubberStampSelectedListener != null) {
                    mOnRubberStampSelectedListener.onRubberStampSelected(stampLabel);
                }
                dismiss();
            }

            @Override
            public void onRubberStampSelected(@Nullable Obj stampObj) {
                if (mOnRubberStampSelectedListener != null) {
                    mOnRubberStampSelectedListener.onRubberStampSelected(stampObj);
                }
                dismiss();
            }
        });
        pager.setAdapter(adapter);

        TabLayout tabLayout = view.findViewById(R.id.stamp_dialog_tab_layout);
        tabLayout.setupWithViewPager(pager);

        SharedPreferences settings = Tool.getToolPreferences(view.getContext());
        int lastSelectedTab = settings.getInt(PREF_LAST_SELECTED_TAB_IN_RUBBER_STAMP_DIALOG, 0);
        pager.setCurrentItem(lastSelectedTab);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Context context = getContext();
                if (context == null) {
                    return;
                }
                SharedPreferences settings = Tool.getToolPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(PREF_LAST_SELECTED_TAB_IN_RUBBER_STAMP_DIALOG, tab.getPosition());
                editor.apply();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        return view;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mOnDialogDismissListener != null) {
            mOnDialogDismissListener.onDialogDismiss();
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

    /**
     * Sets the listener to {@link OnDialogDismissListener}.
     *
     * @param listener The listener
     */
    public void setOnDialogDismissListener(OnDialogDismissListener listener) {
        mOnDialogDismissListener = listener;
    }

}
