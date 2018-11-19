//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples

import android.app.Activity
import android.widget.ScrollView
import android.widget.TextView

class OutputTextView(private val mOutputTextView: TextView, private val mOutputScrollView: ScrollView, private val mActivity: Activity) : OutputListener {

    override fun print(output: String?) {

        mActivity.runOnUiThread {
            mOutputTextView.append(output)

            mOutputScrollView.post( // This is necessary to scroll the ScrollView to the bottom.
            {
                mOutputScrollView.fullScroll(ScrollView.FOCUS_DOWN)
            })
        }
    }

    override fun println(output: String?) {

        mActivity.runOnUiThread {
            mOutputTextView.append(output + "\n")

            mOutputScrollView.post( // This is necessary to scroll the ScrollView to the bottom.
            {
                mOutputScrollView.fullScroll(ScrollView.FOCUS_DOWN)
            })
        }
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
