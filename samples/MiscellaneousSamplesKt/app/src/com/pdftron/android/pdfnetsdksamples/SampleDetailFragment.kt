//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView

/**
 * A fragment representing a single Sample detail screen.
 */
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
class SampleDetailFragment : Fragment() {

    /**
     * The sample content this fragment is presenting.
     */
    private var mSample: PDFNetSample? = null

    private var mOutputListener: OutputListener? = null
    private var mOutputScrollView: ScrollView? = null
    private var mOutputTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments!!.containsKey(ARG_SAMPLE_ID)) {
            // Load the sample content specified by the fragment arguments.
            mSample = MiscellaneousSamplesApplication.instance?.content?.get(arguments!!.getInt(ARG_SAMPLE_ID)) as PDFNetSample
        }

        mOutputListener = object : OutputListener {

            override fun print(output: String?) {
                mOutputTextView!!.append(output)

                mOutputScrollView!!.post( // This is necessary to scroll the ScrollView to the bottom.
                {
                    mOutputScrollView!!.fullScroll(ScrollView.FOCUS_DOWN)
                })

            }

            override fun println(output: String?) {
                System.out.println(output)
                mOutputTextView!!.append(output + "\n")

                mOutputScrollView!!.post( // This is necessary to scroll the ScrollView to the bottom.
                {
                    mOutputScrollView!!.fullScroll(ScrollView.FOCUS_DOWN)
                })

            }

            override fun print() {
                print("")
            }

            override fun println() {
                println("")
            }

            override fun println(stackTrace: Array<StackTraceElement>) {
                for (i in stackTrace.indices) {
                    println(stackTrace[i].toString())
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_sample_detail, container, false)

        // Show the sample description as text in a TextView.
        if (mSample != null) {
            (rootView.findViewById<View>(R.id.sample_detail_textview) as TextView).text = mSample!!.description
        }

        mOutputScrollView = rootView.findViewById<View>(R.id.sample_output_scrollview) as ScrollView
        mOutputTextView = rootView.findViewById<View>(R.id.sample_output_textview) as TextView

        val buttonRun = rootView.findViewById<View>(R.id.sample_run_button) as Button
        buttonRun.setOnClickListener { mSample!!.run(mOutputListener) }

        val buttonOpenFiles = rootView.findViewById<View>(R.id.sample_open_files_button) as Button
        buttonOpenFiles.setOnClickListener {
            val listFilesDialog = ListFilesDialogFragment(mSample!!.files)
            listFilesDialog.show(activity!!.supportFragmentManager, "list_files_dialog_fragment")
        }

        return rootView
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        val ARG_SAMPLE_ID = "sample_id"
    }
}
