//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * A fragment representing a single Sample detail screen.
 */
public class SampleDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_SAMPLE_ID = "sample_id";

    /**
     * The sample content this fragment is presenting.
     */
    private PDFNetSample mSample;

    private OutputListener mOutputListener;
    private ScrollView mOutputScrollView;
    private TextView mOutputTextView;
    
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SampleDetailFragment() {
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_SAMPLE_ID)) {
            // Load the sample content specified by the fragment arguments.
            mSample = (PDFNetSample) MiscellaneousSamplesApplication.getInstance()
                    .getContent().get(getArguments().getInt(ARG_SAMPLE_ID));
        }

        mOutputListener = new OutputListener() {

            @Override
            public void print(String output) {
                mOutputTextView.append(output);
                
                mOutputScrollView.post(new Runnable() {
                    // This is necessary to scroll the ScrollView to the bottom.
                    @Override
                    public void run() {
                        mOutputScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
                
            }

            @Override
            public void println(String output) {
                System.out.println(output);
                mOutputTextView.append(output + "\n");
                
                mOutputScrollView.post(new Runnable() {
                    // This is necessary to scroll the ScrollView to the bottom.
                    @Override
                    public void run() {
                        mOutputScrollView.fullScroll(ScrollView.FOCUS_DOWN);
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
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_sample_detail, container, false);

        // Show the sample description as text in a TextView.
        if (mSample != null) {
            ((TextView) rootView.findViewById(R.id.sample_detail_textview)).setText(mSample.getDescription());
        }
        
        mOutputScrollView = (ScrollView) rootView.findViewById(R.id.sample_output_scrollview);
        mOutputTextView = (TextView) rootView.findViewById(R.id.sample_output_textview);
        
        Button buttonRun = (Button) rootView.findViewById(R.id.sample_run_button);
        buttonRun.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                mSample.run(mOutputListener);
            }
        });
        
        Button buttonOpenFiles = (Button) rootView.findViewById(R.id.sample_open_files_button);
        buttonOpenFiles.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                ListFilesDialogFragment listFilesDialog = new ListFilesDialogFragment(mSample.getFiles());
                listFilesDialog.show(getActivity().getSupportFragmentManager(), "list_files_dialog_fragment");
            }
        });
        
        return rootView;
    }
}
