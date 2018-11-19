//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples;

import android.app.Activity;
import android.widget.ScrollView;
import android.widget.TextView;

public class OutputTextView implements OutputListener {
    
    private final TextView mOutputTextView;
    private final ScrollView mOutputScrollView;
    private final Activity mActivity;
    
    public OutputTextView(TextView textView, ScrollView scrollView, Activity activity) {
        this.mOutputTextView = textView;
        this.mOutputScrollView = scrollView;
        this.mActivity = activity;
    }
    
    @Override
    public void print(final String output) {
        
        mActivity.runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                mOutputTextView.append(output);
                
                mOutputScrollView.post(new Runnable() {
                    // This is necessary to scroll the ScrollView to the bottom.
                    @Override
                    public void run() {
                        mOutputScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    @Override
    public void println(final String output) {
        
        mActivity.runOnUiThread(new Runnable() {
            
            @Override
            public void run() {
                mOutputTextView.append(output + "\n");
                
                mOutputScrollView.post(new Runnable() {
                    // This is necessary to scroll the ScrollView to the bottom.
                    @Override
                    public void run() {
                        mOutputScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
                
            }
        });
    }

    @Override
    public void print() {
        print("");
    }

    @Override
    public void println() {
        println("");
    }

    @Override
    public void println(StackTraceElement[] stackTrace) {
        for (int i = 0; i < stackTrace.length; i++) {
            println(stackTrace[i].toString());
        }
    }
}
