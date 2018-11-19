//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pdftron.android.pdfnetsdksamples.util.Utils;

public class ListFilesDialogFragment extends DialogFragment {

    private ArrayList<String> mFiles;
    
    public ListFilesDialogFragment() {}
    
    public ListFilesDialogFragment(ArrayList<String> list) {
        mFiles = list;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View listView = inflater.inflate(R.layout.fragment_list_files, null);
        ListView fileList = (ListView) listView.findViewById(R.id.list_files_listview);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                mFiles);
        fileList.setAdapter(adapter);
        fileList.setOnItemClickListener(new OnItemClickListener() {
            
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent fileViewIntent = new Intent(Intent.ACTION_VIEW);
                File file = Utils.createExternalFile(mFiles.get(position));
                Uri fileUri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    fileUri = FileProvider.getUriForFile(getContext(), getContext().getApplicationContext().getPackageName() + ".provider", file);
                } else {
                    fileUri = Uri.fromFile(file);
                }
                fileViewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                String extension = FilenameUtils.getExtension(file.getName());
                
                if (extension.equalsIgnoreCase("pdf")) {
                    fileViewIntent.setDataAndType(fileUri, "application/pdf");
                } else if (extension.equalsIgnoreCase("png") || extension.equalsIgnoreCase("jpg")
                        || extension.equalsIgnoreCase("gif")) {
                    fileViewIntent.setDataAndType(fileUri, "image/*");
                } else {
                    fileViewIntent.setData(fileUri);
                }
                fileViewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                try {
                    Intent chooser = Intent.createChooser(fileViewIntent, getResources().getString(R.string.title_choose_application));
                    startActivity(chooser);
                    dismiss();
                } 
                catch (ActivityNotFoundException e)
                {
                    Toast.makeText(getActivity(), getResources().getString(R.string.str_no_application_available), Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        TextView emptyList = (TextView) listView.findViewById(R.id.list_files_empty_textview);
        if (adapter.getCount() > 0) {
            emptyList.setVisibility(View.GONE);
        } else {
            emptyList.setVisibility(View.VISIBLE);
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(listView)
            .setTitle(R.string.title_list_files)
            .setCancelable(true)
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
        return builder.create();
    }
}
