package com.pdftron.android.pdfnetsdksamples

import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Handler

import java.io.File
import java.io.IOException
import java.util.HashMap

import com.pdftron.android.pdfnetsdksamples.samples.DocxConvertTest
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.common.PDFNetException
import com.pdftron.pdf.Convert
import com.pdftron.pdf.DocumentConversion
import com.pdftron.pdf.PDFDoc
import com.pdftron.sdf.SDFDoc

import org.apache.commons.io.FilenameUtils
import org.json.JSONException
import org.json.JSONObject

import android.content.DialogInterface


/**
 * Created by Renchen on 6/18/2015.
 */
class DocxConversionProgessTask(private val mSample: DocxConvertTest, private val mLisener: OutputListener) : AsyncTask<DocxConvertTest, String, Map<String, String>>() {
    private val CONVERTED_SUCESS = 0
    private val CONVERTED_FAIL = 1
    private val CONVERTED_INPROGRESS = 2
    private val OTHER_FAIL = 3

    private val mProgessDialog: ProgressDialog
    private val mFiles: List<File>?
    private val mHandler: Handler

    init {
        mProgessDialog = ProgressDialog(SampleListActivity.instance)
        mFiles = mSample.listFiles
        mHandler = mSample.handler
    }

    override fun onPreExecute() {
        mProgessDialog.setTitle("Converting documents...")
        mProgessDialog.setMessage("Converting in progress...")
        mProgessDialog.setProgressStyle(1)
        mProgessDialog.progress = 0
        mProgessDialog.max = mFiles!!.size
        mProgessDialog.setCancelable(false)

        mProgessDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel") { dialog, which -> dialog.dismiss() }

        mProgessDialog.show()
    }

    override fun onPostExecute(result: Map<String, String>) {
        try {
            mSample.fileOutputStream?.flush()
            mSample.fileOutputStream?.close()
        } catch (e: IOException) {
        }

        if (result.size > 0) {
            mLisener.println("Error occurs during conversion. Success Rate: " + result.size.toDouble() / mFiles!!.size * 100 + "%")
            for ((key, value) in result) {
                mLisener.println("$key : $value")
            }
            mSample.printFooter(mLisener)
        } else {
            mLisener.println("Everything is ok!")
        }
    }

    override fun onProgressUpdate(vararg values: String) {
        var flag: Int
        try {
            flag = Integer.parseInt(values[0])
        } catch (e: NumberFormatException) {
            flag = -1
        }

        when (flag) {
            CONVERTED_SUCESS -> {
                mProgessDialog.setMessage(values[1])
                mProgessDialog.incrementProgressBy(1)
                mLisener.println(values[2])
            }
            CONVERTED_FAIL -> {
                mProgessDialog.setMessage(values[1])
                mProgessDialog.incrementProgressBy(1)
                mLisener.println(values[1])
            }
            CONVERTED_INPROGRESS -> {
                mProgessDialog.setTitle(values[1])
                mProgessDialog.setMessage("")
            }
            OTHER_FAIL -> {
                mProgessDialog.setMessage(values[1])
                mLisener.println(values[1])
            }
            else ->
                // mProgessDialog.setMessage(values[0]);
                mLisener.println(values[0])
        }
    }

    override fun doInBackground(vararg args: DocxConvertTest): Map<String, String> {
        val crashed_docs = HashMap<String, String>()
        val total = mFiles!!.size
        var converted = 0
        var exception = 0
        var qaReporter: DocxQAReportWriter? = null

        try {
            qaReporter = DocxQAReportWriter(mSample.fileOutputStream)
            qaReporter.BeginArray()
        } catch (e: IOException) {
        }

        var counter = mFiles.size
        for (file in mFiles) {
            try {
                if (counter-- == 0) {
                    break
                }
                val doc = PDFDoc()
                publishProgress(Integer.toString(CONVERTED_INPROGRESS), "Converting " + file.name)

                val docconvert = Convert.wordToPdfConversion(doc, file.absolutePath, mSample.getmWordToPDFOptions())
                doc.pageCount

                val result = docconvert.tryConvert()
                val bson = docconvert.getWarningString(-1)

                val jObj = JSONObject(bson)
                val totalTime = jObj.getDouble("total_time")
                val docObj = DocumentObject(mSample.nameMap!!.getString(file.name), doc.pageCount, totalTime)

                try {
                    val warnings = jObj.getString("Warnings")
                    docObj.setmWarnings(warnings)
                } catch (e: JSONException) {
                }

                try {
                    val errors = jObj.getString("Errors")
                    docObj.setmErrors(errors)
                } catch (e: JSONException) {
                }

                try {
                    val time_stats = jObj.getString("time_stats")
                    docObj.setmTimeStats(time_stats)
                } catch (e: JSONException) {
                }

                qaReporter!!.WriteDocumentObject(docObj)
                /*
                FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());
                MessageDigest md = MessageDigest.getInstance("MD5");
                */

                publishProgress(bson)
                converted = converted + 1

                val updateString = file.name + " Converted " + " [" + converted.toDouble() / total * 100 + "% ] Done"
                publishProgress(Integer.toString(CONVERTED_SUCESS), file.name + " Converted Sucessfully ", updateString)

                // We have to do this on UI thread
                // mProgessDialog.setMessage(file.getName() + " Converted Sucessfully ");
                /*
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mProgessDialog.incrementProgressBy(1);
                    }
                });
                */

                val fileWithoutExt = FilenameUtils.removeExtension(file.name)
                doc.save(Utils.createExternalFile("$fileWithoutExt.pdf").absolutePath, SDFDoc.e_linearized.toLong(), null)
                doc.close()
                mSample.addToFileList("$fileWithoutExt.pdf")
            } catch (e: PDFNetException) {
                exception = exception + 1
                crashed_docs[file.name] = e.message.toString()
                val tmp = Integer.toString(CONVERTED_FAIL) + file.name + " Failed. Details: " + e.message
                publishProgress(tmp)
            } catch (e: Exception) {
                publishProgress(Integer.toString(OTHER_FAIL), file.name + " Failed for non-PDFNet reasons... Details: " + e.stackTrace)
            }

        }

        try {
            qaReporter!!.EndArray()
        } catch (e: IOException) {
        }

        return crashed_docs
    }
}
