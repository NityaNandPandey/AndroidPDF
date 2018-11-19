//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples

import android.annotation.SuppressLint
import java.io.File
import java.util.ArrayList

import org.apache.commons.io.FilenameUtils

import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.FileProvider
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import com.pdftron.android.pdfnetsdksamples.util.Utils

class ListFilesDialogFragment : DialogFragment {

    private var mFiles: ArrayList<String> = ArrayList<String>()

    constructor() {}

    @SuppressLint("ValidFragment")
    constructor(list: ArrayList<String>) {
        this.mFiles = list
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = activity!!.layoutInflater
        val listView = inflater.inflate(R.layout.fragment_list_files, null)
        val fileList = listView.findViewById<View>(R.id.list_files_listview) as ListView
        val adapter = ArrayAdapter(activity!!,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                mFiles)
        fileList.adapter = adapter
        fileList.onItemClickListener = OnItemClickListener { parent, view, position, id ->
            val fileViewIntent = Intent(Intent.ACTION_VIEW)
            val file = Utils.createExternalFile(mFiles[position])
            val fileUri: Uri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                fileUri = FileProvider.getUriForFile(context!!, context!!.applicationContext.packageName + ".provider", file)
            } else {
                fileUri = Uri.fromFile(file)
            }
            fileViewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val extension = FilenameUtils.getExtension(file.name)

            if (extension.equals("pdf", ignoreCase = true)) {
                fileViewIntent.setDataAndType(fileUri, "application/pdf")
            } else if (extension.equals("png", ignoreCase = true) || extension.equals("jpg", ignoreCase = true)
                    || extension.equals("gif", ignoreCase = true)) {
                fileViewIntent.setDataAndType(fileUri, "image/*")
            } else {
                fileViewIntent.data = fileUri
            }
            fileViewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                val chooser = Intent.createChooser(fileViewIntent, resources.getString(R.string.title_choose_application))
                startActivity(chooser)
                dismiss()
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(activity, resources.getString(R.string.str_no_application_available), Toast.LENGTH_SHORT).show()
            }
        }

        val emptyList = listView.findViewById<View>(R.id.list_files_empty_textview) as TextView
        if (adapter.count > 0) {
            emptyList.visibility = View.GONE
        } else {
            emptyList.visibility = View.VISIBLE
        }

        val builder = AlertDialog.Builder(activity)
        builder.setView(listView)
                .setTitle(R.string.title_list_files)
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel) { dialog, id -> dialog.cancel() }
        return builder.create()
    }
}
