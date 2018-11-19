package com.pdftron.android.pdfnetsdksamples

import android.app.ProgressDialog
import android.os.AsyncTask

import java.io.File

import com.pdftron.android.pdfnetsdksamples.samples.DocxConvertTest

/**
 * Created by Renchen on 6/19/2015.
 */
class GetFilesFromAssetsTask(private val mDocxConvertSample: DocxConvertTest, private val mFiles: MutableList<File>, private val mOutputlis: OutputListener, FolderPath: String, private val mExt: String) : AsyncTask<Void, String, List<File>>() {
    private val mFolder: File
    private val mProgessDialog: ProgressDialog

    init {
        mProgessDialog = ProgressDialog(SampleListActivity.instance)
        mFolder = File(FolderPath)
    }

    override fun onPostExecute(files: List<File>) {
        try {
            /*
            Collections.sort(files, new Comparator<File>() {
                public int compare(File obj1, File obj2) {
                    if (obj1 == null ^ obj2 == null) {
                        return (obj1 == obj2) ? -1 : 1;
                    }

                    if (obj1 == null && obj2 == null) {
                        return 0;
                    }
                    String name1 = obj1.getName();
                    String name2 = obj2.getName();
                    String fileWithoutExt1 = FilenameUtils.removeExtension(name1);
                    String fileWithoutExt2 = FilenameUtils.removeExtension(name2);

                    return Integer.parseInt(fileWithoutExt1) - Integer.parseInt(fileWithoutExt2);
                }
            });
            */
        } catch (e: Exception) {
            println(e.message)
        }

        mProgessDialog.dismiss()
        val task = DocxConversionProgessTask(mDocxConvertSample, mOutputlis).execute()
    }

    override fun onPreExecute() {
        mProgessDialog.setTitle("Please wait while I fetching files for converting...")
        // mProgessDialog.setMessage("...");
        mProgessDialog.setProgressStyle(0)
        mProgessDialog.setCancelable(false)

        mProgessDialog.show()
    }

    override fun onProgressUpdate(vararg fileName: String) {
        mProgessDialog.setMessage(fileName[0] + " is added to queue...")
        // Toast.makeText(SampleListActivity.instance, fileName[0] + " is added to queue...", Toast.LENGTH_SHORT).show();
    }

    override fun doInBackground(vararg parms: Void): List<File> {
        try {
            val files = mFolder.list()
            for (filename in files) {
                if (filename.endsWith(mExt)) {
                    publishProgress(filename)
                    if (filename === "1879.docx") {
                        val tmp = false
                    }
                    mFiles.add(File(mFolder, filename))
                }
            }
            return mFiles
        } catch (e: Exception) {
            println(e.message)
        }

        return mFiles
    }
}
