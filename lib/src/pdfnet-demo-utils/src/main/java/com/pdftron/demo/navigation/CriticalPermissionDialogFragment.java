package com.pdftron.demo.navigation;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.pdftron.demo.R;
import com.pdftron.pdf.utils.Utils;

public class CriticalPermissionDialogFragment extends DialogFragment {

    static final String ASK_PERMISSION_ARG = "ask_permission";

    private OnPermissionDialogFragmentListener mListener;
    private boolean mAskPermission = false;
    private boolean mExit = true;

    private ImageView mImageView;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CriticalPermissionDialogFragment.
     */
    public static CriticalPermissionDialogFragment newInstance(boolean askPermission) {
        CriticalPermissionDialogFragment f = new CriticalPermissionDialogFragment();

        Bundle args = new Bundle();
        args.putBoolean(ASK_PERMISSION_ARG, askPermission);
        f.setArguments(args);

        return f;
    }

    public CriticalPermissionDialogFragment() {
        // Required empty public constructor
    }

    public void setListener(OnPermissionDialogFragmentListener listener) {
        mListener = listener;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (OnPermissionDialogFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + e.getClass().toString());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        if (mListener != null) {
            mListener.onPermissionScreenDismiss(mExit, mAskPermission);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAskPermission = getArguments().getBoolean(ASK_PERMISSION_ARG, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_critical_permission_dialog, container, false);

        TextView textView = (TextView) view.findViewById(R.id.permission_body);

        TextView textViewTitle = (TextView) view.findViewById(R.id.permission_title);
        // format the title
        StringBuilder textTitle = new StringBuilder();
        textTitle.append(getResources().getString(R.string.permission_screen_title));
        SpannableStringBuilder textTitleFinal = new SpannableStringBuilder();
        textTitleFinal.append(Html.fromHtml(textTitle.toString()));
        textViewTitle.setText(textTitleFinal);

        // format the body
        StringBuilder textBody = new StringBuilder();
        String message = String.format(getString(R.string.permission_screen_body),
            getString(R.string.app_name));
        textBody.append(message);
        SpannableStringBuilder textBodyFinal = new SpannableStringBuilder();
        textBodyFinal.append(Html.fromHtml(textBody.toString()));
        textView.setText(textBodyFinal);

        mImageView = (ImageView)view.findViewById(R.id.permission_image);
        Button exitButton = (Button)view.findViewById(R.id.permission_exit);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExit = true;
                dismiss();
            }
        });
        Button settingsButton = (Button)view.findViewById(R.id.permission_settings);
        if (mAskPermission) {
            settingsButton.setText(getString(R.string.permission_screen_grant_permission));
        } else {
            settingsButton.setText(getString(R.string.permission_screen_settings));
        }
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExit = false;
                dismiss();
            }
        });
        return view;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mImageView.setImageResource(R.drawable.permissions_land);
        } else {
            mImageView.setImageResource(R.drawable.permissions);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (Utils.isLollipop()) {
            if (getDialog() != null && getDialog().getWindow() != null) {
                getDialog().getWindow().setStatusBarColor(getResources().getColor(R.color.permission_screen_background));
            }
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mImageView.setImageResource(R.drawable.permissions_land);
        } else {
            mImageView.setImageResource(R.drawable.permissions);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnPermissionDialogFragmentListener {
        void onPermissionScreenDismiss(boolean exit, boolean askPermission);
    }
}
