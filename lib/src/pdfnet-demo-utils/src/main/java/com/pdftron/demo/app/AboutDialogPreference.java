//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.app;

import android.content.Context;
import android.preference.DialogPreference;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.pdftron.demo.R;
import com.pdftron.demo.utils.SettingsManager;

/**
 * About dialog used in the CompleteReader demo app.
 */
public class AboutDialogPreference extends DialogPreference {

    private Context mContext;

    public AboutDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AboutDialogPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        setDialogLayoutResource(R.layout.dialog_about);
        setDialogTitle(R.string.about);
        setSummary(getAboutSummary());
        setPositiveButtonText(R.string.ok);
        setNegativeButtonText(null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        TextView textView = (TextView) view.findViewById(R.id.dialog_about_textview);
        textView.setText(getAboutBody());
        textView.setMovementMethod(new LinkMovementMethod());
    }

    private String getAboutSummary() {
        StringBuilder summary = new StringBuilder();

        summary.append(getContext().getString(R.string.app_name));
        summary.append(getContext().getString(R.string.registered_trademark));
        String versionName = SettingsManager.getAppVersionName(mContext);
        if (!versionName.isEmpty()) {
            summary.append(" " + versionName);
        }

        return summary.toString();
    }

    private SpannableStringBuilder getAboutBody() {
        StringBuilder aboutBody = new StringBuilder();

        // App name
        aboutBody.append("<b>" + mContext.getResources().getString(R.string.app_name) + getContext().getString(R.string.registered_trademark) + "</b><br>");

        // App version
        aboutBody.append(mContext.getResources().getString(R.string.version) + " "
                + SettingsManager.getAppVersionName(mContext) + "<br>");

        aboutBody.append("<br><br>");

        // About text
        aboutBody.append(mContext.getResources().getString(R.string.dialog_about_body));

        SpannableStringBuilder aboutBodyFinal = new SpannableStringBuilder();
        aboutBodyFinal.append(Html.fromHtml(aboutBody.toString()));

        return aboutBodyFinal;
    }
}
