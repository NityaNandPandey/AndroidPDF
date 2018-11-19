//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to allow the use of checkable widgets inside ListViews
 *
 * WARNING: Make sure to use this with InertCheckBox/InertRadioButton/InertSwitch
 * or onItemClickListener will not work.
 *
 * WARNING2: This class is not reliable for storing binary data (i.e. don't query it for
 * whether it's checked or not) but instead only use it for visible feedback for the user.
 */
public class CheckableRelativeLayout extends RelativeLayout implements Checkable {

    /**
     * Callback interface to be invoked when the checked state of the layout is changed.
     */
	public interface OnCheckedChangeListener {
        /**
         * Called when the checked state of the layout has been changed.
         *
         * @param layout The checkable relative layout
         * @param checked The new checked state of buttonView
         */
		void onCheckedChanged(CheckableRelativeLayout layout, boolean checked);
	}

	private boolean mChecked;
	private List<Checkable> mCheckableViews;
	private OnCheckedChangeListener mListener;

	public CheckableRelativeLayout(Context context) {
		super(context);
		init();
	}

	public CheckableRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CheckableRelativeLayout(Context context, AttributeSet attrs,	int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		mChecked = false;
		mCheckableViews = new ArrayList<Checkable>();
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void setChecked(boolean checked) {
		mChecked = checked;

		for (Checkable c : mCheckableViews) {
            c.setChecked(checked);
		}

		if (mListener != null) {
            mListener.onCheckedChanged(this, checked);
		}
	}

	@Override
	public void toggle() {
		mChecked = !mChecked;

		for (Checkable c : mCheckableViews) {
			c.toggle();
		}
	}

	public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
		mListener = listener;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		int childCount = this.getChildCount();
        for (int i = 0; i < childCount; ++i) {
        	findCheckableChildren(this.getChildAt(i));
        }
	}

    private void findCheckableChildren(View view) {
        if (view instanceof Checkable) {
        	mCheckableViews.add((Checkable) view);
        }

        if (view instanceof ViewGroup) {
        	final ViewGroup vg = (ViewGroup) view;
            final int childCount = vg.getChildCount();
            for (int i = 0; i < childCount; ++i) {
            	findCheckableChildren(vg.getChildAt(i));
            }
        }
    }

}
