package com.pdftron.android.pdfnetsdksamples

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.app.NavUtils
import android.view.MenuItem

/**
 * An activity representing a single Sample detail screen. This activity is
 * only used on handset devices. On tablet-size devices, sample details are
 * presented side-by-side with a list of items in a [SampleListActivity].
 *
 *
 * This activity is mostly just a 'shell' activity containing nothing more than a
 * [SampleDetailFragment].
 */
class SampleDetailActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sample_detail)

        // Show the Up button in the action bar.
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            val arguments = Bundle()
            arguments.putInt(SampleDetailFragment.ARG_SAMPLE_ID, intent
                    .getIntExtra(SampleDetailFragment.ARG_SAMPLE_ID, 0))
            val fragment = SampleDetailFragment()
            fragment.arguments = arguments
            supportFragmentManager.beginTransaction()
                    .add(R.id.sample_detail_container, fragment).commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpTo(this,
                        Intent(this, SampleListActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
