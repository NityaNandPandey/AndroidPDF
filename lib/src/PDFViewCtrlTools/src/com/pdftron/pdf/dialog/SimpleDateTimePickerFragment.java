package com.pdftron.pdf.dialog;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.pdftron.pdf.tools.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Calendar;

public class SimpleDateTimePickerFragment extends DialogFragment {

    public static final String BUNDLE_MODE = "mode";

    public static final int MODE_DATE = 0;
    public static final int MODE_TIME = 1;

    private boolean mManuallyEnterValue = false;

    @IntDef({MODE_DATE, MODE_TIME})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DialogMode {
    }

    public interface SimpleDatePickerListener {
        void onDateSet(DatePicker view, int year, int month, int day);
        void onTimeSet(TimePicker view, int hourOfDay, int minute);
        void onClear();
        void onDismiss(boolean manuallyEnterValue);
    }

    private SimpleDatePickerListener mListener;

    public void setSimpleDatePickerListener(SimpleDatePickerListener listener) {
        mListener = listener;
    }

    public static SimpleDateTimePickerFragment newInstance(@DialogMode final int mode) {
        SimpleDateTimePickerFragment fragment = new SimpleDateTimePickerFragment();
        Bundle args = new Bundle();
        args.putInt(BUNDLE_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int mode = MODE_DATE;
        if (getArguments() != null) {
            mode = getArguments().getInt(BUNDLE_MODE, MODE_DATE);
        }

        AlertDialog dialog;
        if (mode == MODE_DATE) {
            // Use the current date as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            dialog = new DatePickerDialog(getActivity(), dateSetListener, year, month, day);
        } else {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            dialog = new TimePickerDialog(getActivity(), timeSetListener, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
        }
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.clear), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mListener != null) {
                    mListener.onClear();
                }
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.enter_value), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mManuallyEnterValue = true;
            }
        });
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mListener != null) {
            mListener.onDismiss(mManuallyEnterValue);
        }
    }

    private DatePickerDialog.OnDateSetListener dateSetListener =
        new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int month, int day) {
                if (mListener != null) {
                    mListener.onDateSet(view, year, month, day);
                }
            }
        };

    private TimePickerDialog.OnTimeSetListener timeSetListener =
        new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                if (mListener != null) {
                    mListener.onTimeSet(view, hourOfDay, minute);
                }
            }
        };
}
