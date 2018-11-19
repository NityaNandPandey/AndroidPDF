//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples;

import com.pdftron.pdf.PDFNet;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.pdftron.android.pdfnetsdksamples.util.Utils;

/**
 * An activity representing a list of Samples.
 */
public class SampleListActivity extends FragmentActivity
        implements SampleListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private boolean mTwoPane;
    private static SampleListActivity m_singleton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_singleton = this;
        setContentView(R.layout.activity_sample_list);

        if (findViewById(R.id.sample_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
            
            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((SampleListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.sample_list))
                    .setActivateOnItemClick(true);
        }

        try {
            PDFNet.initialize(this, R.raw.pdfnet, getLicenseKey(this));
        }
        catch (Exception e) {
            Toast.makeText(this, "Error during PDFNet initialization", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private static String getLicenseKey(Context applicationContext) {
        try {
            ApplicationInfo ai = applicationContext.getPackageManager().getApplicationInfo(
                    applicationContext.getPackageName(),
                    PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            return bundle.getString("pdftron_license_key");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static SampleListActivity getInstance(){ return m_singleton; }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_about:
                Utils.showAbout(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Callback method from {@link SampleListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(int id) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putInt(SampleDetailFragment.ARG_SAMPLE_ID, id);
            SampleDetailFragment fragment = new SampleDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.sample_detail_container, fragment)
                    .commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, SampleDetailActivity.class);
            detailIntent.putExtra(SampleDetailFragment.ARG_SAMPLE_ID, id);
            startActivity(detailIntent);
        }
    }
}
