package com.pdftron.android.pdfnetsdksamples.samples

import com.pdftron.android.pdfnetsdksamples.GetFilesFromAssetsTask
import com.pdftron.android.pdfnetsdksamples.MiscellaneousSamplesApplication
import com.pdftron.android.pdfnetsdksamples.OutputListener
import com.pdftron.android.pdfnetsdksamples.PDFNetSample
import com.pdftron.android.pdfnetsdksamples.DocxConversionProgessTask
import com.pdftron.android.pdfnetsdksamples.R
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.common.PDFNetException
import com.pdftron.pdf.Convert
import com.pdftron.pdf.DocumentConversion
import com.pdftron.pdf.PDFDoc
import com.pdftron.pdf.WordToPDFOptions
import com.pdftron.sdf.SDFDoc

import android.os.Environment
import android.os.Handler

import org.json.JSONException
import org.json.JSONObject

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.util.ArrayList


/**
 * Created by Renchen on 6/11/2015.
 */
class DocxConvertTest : PDFNetSample() {
    val handler: Handler
    private var mFiles: MutableList<File>? = null
    var fileOutputStream: FileOutputStream? = null
        private set
    var nameMap: JSONObject? = null
        private set
    private var mWordToPDFOptions: WordToPDFOptions? = null

    val listFiles: List<File>?
        get() = mFiles

    fun getmWordToPDFOptions(): WordToPDFOptions? {
        return mWordToPDFOptions
    }

    init {
        setTitle(R.string.sample_docxconvert_title)
        setDescription(R.string.sample_docxconvert_description)
        handler = Handler()
    }

    override fun run(outputListener: OutputListener?) {
        super.run(outputListener)
        printHeader(outputListener!!)

        var docx_folder: File? = null
        var json_data: File? = null
        var json_output_stream: FileOutputStream? = null
        var name_map: File? = null
        try {
            val layoutPluginPath = Utils.copyResourceToTempFolder(MiscellaneousSamplesApplication.instance?.context, R.raw.pdftron_layout_resources, false, "pdftron_layout_resources.plugin")
            val layoutSmartPluginPath = Utils.copyResourceToTempFolder(MiscellaneousSamplesApplication.instance?.context, R.raw.pdftron_smart_substitution, false, "pdftron_smart_substitution.plugin")

            val internal_storage = Environment.getExternalStorageDirectory()
            docx_folder = File(internal_storage, "DocxTes")

            // The output result file
            json_data = File(docx_folder, "json_data.txt")
            json_output_stream = null

            // The NameMap file (name map for ScrapedFromWeb)
            name_map = File(docx_folder, "NameMap.txt")

            mWordToPDFOptions = WordToPDFOptions()
            mWordToPDFOptions!!.layoutResourcesPluginPath = layoutPluginPath
            mWordToPDFOptions!!.smartSubstitutionPluginPath = layoutSmartPluginPath
        } catch (e: PDFNetException) {
        }

        if (docx_folder != null && !docx_folder.exists()) {
            outputListener.println("No DocxFile folder found on SD card... Will be using built-in docx files for Demo")
            // first the one-line conversion interface
            simpleDocxConvert("simple-word_2007.docx", "simple-word_2007_a.pdf", outputListener)

            // then the more flexible line-by-line interface
            flexibleDocxConvert("the_rime_of_the_ancient_mariner.docx", "the_rime_of_the_ancient_mariner.pdf", outputListener)

            // then one more, with some advanced layout features
            flexibleDocxConvert("wrap_poly_demo.docx", "wrap_poly_demo.pdf", outputListener)

            printFooter(outputListener)
        } else {
            try {
                // Read from NameMap.txt and dump into a json object.
                if (name_map != null) {
                    json_output_stream = FileOutputStream(json_data!!)
                    val name_map_input_stream = FileInputStream(name_map)
                    val jString: String
                    try {
                        val fc = name_map_input_stream.channel
                        val bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size())
                        /* Instead of using default, pass in a decoder. */
                        jString = Charset.defaultCharset().decode(bb).toString()
                        nameMap = JSONObject(jString)
                    } catch (e: IOException) {
                        outputListener.println(e.message)
                    } catch (e: JSONException) {
                        outputListener.println(e.message)
                    }

                    fileOutputStream = json_output_stream
                }
            } catch (e: FileNotFoundException) {
                outputListener.println("Output stream cannot be created!")
                return
            }

            outputListener.println("Found DocxFile folder on SD card... Will be using provided docx files for Demo")
            try {
                // DocumentConversion convert;
                if (mFiles == null) {
                    mFiles = ArrayList()
                    GetFilesFromAssetsTask(this, mFiles!!, outputListener, docx_folder!!.absolutePath, ".docx").execute()
                } else {
                    DocxConversionProgessTask(this, outputListener).execute()
                }
            } catch (e: Exception) {
                outputListener.println(e.stackTrace)
            }

        }
    }

    fun simpleDocxConvert(inputFilename: String, outputFilename: String, outputListener: OutputListener) {
        try {
            // Start with a PDFDoc (the conversion destination)
            val pdfdoc = PDFDoc()

            // perform the conversion with no optional parameters
            Convert.wordToPdf(pdfdoc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + inputFilename)!!.absolutePath, mWordToPDFOptions)

            // save the result
            pdfdoc.save(Utils.createExternalFile(outputFilename).absolutePath, SDFDoc.e_linearized.toLong(), null)

            // And we're done!
            outputListener.println("Done conversion for $outputFilename")

            addToFileList(outputFilename)
        } catch (e: PDFNetException) {
            outputListener.println("Unable to convert MS Word document, error:")
            e.printStackTrace()
            outputListener.println(e.message)
        }

    }

    fun flexibleDocxConvert(inputFilename: String, outputFilename: String, outputListener: OutputListener) {
        try {
            // Start with a PDFDoc (the conversion destination)
            val pdfdoc = PDFDoc()

            // create a conversion object -- this sets things up but does not yet
            // perform any conversion logic.
            // in a multithreaded environment, this object can be used to monitor
            // the conversion progress and potentially cancel it as well
            val conversion = Convert.wordToPdfConversion(
                    pdfdoc, Utils.getAssetTempFile(PDFNetSample.Companion.INPUT_PATH + inputFilename)!!.absolutePath, mWordToPDFOptions)

            outputListener.println(inputFilename + ": " + Math.round(conversion.progress * 100.0)
                    + "% " + conversion.progressLabel)

            // actually perform the conversion
            while (conversion.conversionStatus == DocumentConversion.e_incomplete) {
                conversion.convertNextPage()
                outputListener.println(inputFilename + ": " + Math.round(conversion.progress * 100.0)
                        + "% " + conversion.progressLabel)
            }

            if (conversion.tryConvert() == DocumentConversion.e_success) {
                val num_warnings = conversion.numWarnings

                // print information about the conversion
                for (i in 0 until num_warnings) {
                    outputListener.println("Warning: " + conversion.getWarningString(i))
                }

                // save the result
                pdfdoc.save(Utils.createExternalFile(outputFilename).absolutePath, SDFDoc.e_linearized.toLong(), null)

                // done
                outputListener.println("Done conversion for $outputFilename")
                addToFileList(outputFilename)
            } else {
                outputListener.println("Encountered an error during conversion: " + conversion.errorString)
            }
        } catch (e: PDFNetException) {
            outputListener.println("Unable to convert MS Word document, error:")
            e.printStackTrace()
            outputListener.println(e.message)
        }

    }
}
