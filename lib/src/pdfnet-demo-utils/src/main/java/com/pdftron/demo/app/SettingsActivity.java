//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.app;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.pdftron.demo.R;
import com.pdftron.demo.utils.SettingsManager;
import com.pdftron.pdf.utils.Utils;

/**
 * Settings dialog used in the CompleteReader demo app.
 */
public class SettingsActivity extends PreferenceActivity {

    private AppCompatDelegate mDelegate;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_complete_reader_settings);
        // Populate the activity with the settings from the xml.
        addPreferencesFromResource(R.xml.settings);

        Toolbar toolbar = findViewById(R.id.tool_bar);
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            setSupportActionBar(toolbar);
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getResources().getString(R.string.action_settings));
        }

        View toolbarShadow = findViewById(R.id.toolbar_shadow);
        if (toolbarShadow != null) {
            if (Utils.isLollipop()) {
                // Toolbar shadow will be handled by Toolbar's elevation attribute
                toolbarShadow.setVisibility(View.GONE);
            } else {
                // Use overlaid View's gradient to simulate elevation
                toolbarShadow.setVisibility(View.VISIBLE);
            }
        }

        Preference contAnnotEditPref = findPreference(SettingsManager.KEY_PREF_CONT_ANNOT_EDIT);
        final Preference annotSelectionPref = findPreference(SettingsManager.KEY_PREF_SHOW_QUICK_MENU);
        if(!SettingsManager.getContinuousAnnotationEdit(this)){
            annotSelectionPref.setEnabled(false);
        }
        contAnnotEditPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.toString().equals("false")) {
                    annotSelectionPref.setEnabled(false);
                } else {
                    annotSelectionPref.setEnabled(true);
                }
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        getDelegate().onStart();
        PreferenceCategory general = (PreferenceCategory) findPreference("pref_category_viewing");
        Preference prefFullScreen = findPreference("pref_full_screen_mode");
        if (general != null && prefFullScreen != null) {
            if (!Utils.isKitKat()) {
                general.removePreference(prefFullScreen);
            }
        }

        Preference prefDesktopUI = findPreference("pref_enable_desktop_ui");
        if (general != null && prefDesktopUI != null) {
            if (!Utils.isChromebook(this)) {
                general.removePreference(prefDesktopUI);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    @NonNull
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    @NonNull
    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }
}

