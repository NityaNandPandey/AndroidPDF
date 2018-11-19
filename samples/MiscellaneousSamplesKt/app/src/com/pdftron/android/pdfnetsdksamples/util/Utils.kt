//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.util

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList

import org.apache.commons.io.FilenameUtils

import com.pdftron.android.pdfnetsdksamples.GetFilesFromAssetsTask
import com.pdftron.common.PDFNetException
import com.pdftron.pdf.PDFNet
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.os.StatFs
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentActivity
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.widget.TextView

import com.pdftron.android.pdfnetsdksamples.MiscellaneousSamplesApplication
import com.pdftron.android.pdfnetsdksamples.R

object Utils {

    /**
     * Returns the absolute path to the directory on the external filesystem
     * (that is somewhere on Environment.getExternalStorageDirectory()) where
     * the application can place persistent files it owns.
     *
     * @return the absolute file path
     */
    val externalFilesDirPath: String?
        get() = MiscellaneousSamplesApplication.instance?.getExternalFilesDir(null)?.absolutePath

    /**
     * Get an InputStream object from an asset file. The file path is
     * relative to the root of the assets folder.
     *
     * @param filePath the file path to the file in assets folder
     * @return an InputStream of the file
     */
    fun getAssetInputStream(filePath: String): InputStream? {
        try {
            return MiscellaneousSamplesApplication.instance?.assets?.open(filePath)
        } catch (e: IOException) {
            return null
        }

    }

    /**
     * Creates a temporary file in the application specific cache directory
     * for the file stored in the assets folder. The file path is relative
     * to the root of the assets folder.
     *
     * @param filePath the file path to the file in assets folder
     * @return a File object for the supplied file path
     */
    fun getAssetTempFile(filePath: String): File? {
        var file: File? = null
        try {
            file = File(MiscellaneousSamplesApplication.instance?.cacheDir, FilenameUtils.getName(filePath))
            val inputStream = getAssetInputStream(filePath)
            val output = FileOutputStream(file!!)
            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)
            var len = 0
            while (true) {
                len = inputStream!!.read(buffer)
                if (len == -1) {
                    break
                }
                output.write(buffer, 0, len)
            }
            inputStream!!.close()
        } catch (e: Exception) {
            return null
        }

        return file
    }

    @Throws(PDFNetException::class)
    fun copyResourceToTempFolder(context: Context?, resId: Int, force: Boolean, resourceName: String): String {
        if (context == null) {
            throw PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "copyResourceToTempFolder()", "Context cannot be null to initialize resource file.")
        } else {
            val resFile = File(context.filesDir.toString() + File.separator + "resourceName")
            if (!resFile.exists() || force) {
                val filesDir = context.filesDir
                val stat = StatFs(filesDir.path)
                val size = stat.availableBlocks.toLong() * stat.blockSize.toLong()
                if (size < 2903023L) {
                    throw PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "copyResourceToTempFolder()", "Not enough space available to copy resources file.")
                }

                val rs = context.resources

                try {
                    val e = rs.openRawResource(resId)
                    val fos = context.openFileOutput(resourceName, 0)
                    val buffer = ByteArray(1024)

                    var read: Int
                    while (true) {
                        read = e.read(buffer)
                        if (read == -1) {
                            break
                        }
                        fos.write(buffer, 0, read)
                    }

                    e.close()
                    fos.flush()
                    fos.close()

                } catch (var13: Resources.NotFoundException) {
                    throw PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "initializeResource()", "Resource file ID does not exist.")
                } catch (var14: FileNotFoundException) {
                    throw PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "initializeResource()", "Resource file not found.")
                } catch (var15: IOException) {
                    throw PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "initializeResource()", "Error writing resource file to internal storage.")
                } catch (var16: Exception) {
                    throw PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "initializeResource()", "Unknown error.")
                }

            }

            return context.filesDir.absolutePath
        }
    }

    /**
     * Get all files under assets/folder_name with 'extension' ext.
     * @param folder_name the path name under assets folder
     * @param extension
     * @return
     */
    fun getListFiles(folder_name: String, extension: String): List<File> {
        val inFiles = ArrayList<File>()
        try {
            val files = MiscellaneousSamplesApplication.instance?.assets!!.list(folder_name)

            for (filename in files) {
                if (filename.endsWith(extension)) {
                    val file = getAssetTempFile("$folder_name/$filename")
                    if (file != null) {
                        inFiles.add(file)
                    }
                }
            }
        } catch (e: Exception) {
            println(e.message)
        }

        return inFiles
    }

    /**
     * Get a Bitmap object from a file stored in the assets folder.
     * The file path is relative to the root of the assets folder.
     *
     * @param filePath the file path to the file in assets folder
     * @return a Bitmap object for the supplied file
     */
    fun getAssetBitmap(filePath: String): Bitmap {
        return BitmapFactory.decodeStream(getAssetInputStream(filePath))
    }

    /**
     * Creates an external file on the external filesystem where the
     * application can place persistent files it owns.
     *
     * @param fileName the file name for the file to be created
     * @return a File object
     */
    fun createExternalFile(fileName: String): File {
        return File(MiscellaneousSamplesApplication.instance?.getExternalFilesDir(null), fileName)
    }

    fun showAbout(activity: FragmentActivity) {
        val newFragment = AboutDialog()
        newFragment.show(activity.supportFragmentManager, "dialog_about")
    }

    class AboutDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            // Get PDFNet version
            var pdfnetVersion = 0.0
            try {
                pdfnetVersion = PDFNet.getVersion()
            } catch (e: Exception) {
                // Do nothing
            }

            // Get application version
            val versionName = pdfnetVersion.toString()

            // Build the dialog body view
            val aboutBody = SpannableStringBuilder()
            aboutBody.append(Html.fromHtml(getString(R.string.about_body, versionName)))

            val layoutInflater = activity!!
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val aboutBodyView = layoutInflater.inflate(R.layout.dialog_about, null) as TextView
            aboutBodyView.text = aboutBody
            aboutBodyView.movementMethod = LinkMovementMethod()

            return AlertDialog.Builder(activity)
                    .setTitle(R.string.about)
                    .setView(aboutBodyView)
                    .setPositiveButton(android.R.string.ok
                    ) { dialog, whichButton -> dialog.dismiss() }
                    .create()
        }
    }
}
