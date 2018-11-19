package com.pdftron.android.pdfnetsdksamples.samples;
import com.pdftron.android.pdfnetsdksamples.GetFilesFromAssetsTask;
import com.pdftron.android.pdfnetsdksamples.MiscellaneousSamplesApplication;
import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.DocxConversionProgessTask;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Convert;
import com.pdftron.pdf.DocumentConversion;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.WordToPDFOptions;
import com.pdftron.sdf.SDFDoc;

import android.os.Environment;
import android.os.Handler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;




/**
 * Created by Renchen on 6/11/2015.
 */
public class DocxConvertSample extends PDFNetSample{
    private Handler mUpdateHandler;
    private List<File> mFiles;
    private FileOutputStream mFileOutput;
    private JSONObject mNameMap;
    private WordToPDFOptions mWordToPDFOptions;

    public WordToPDFOptions getmWordToPDFOptions(){return mWordToPDFOptions;}

    public JSONObject getNameMap() {return mNameMap;}

    public List<File> getListFiles() { return mFiles; }

    public FileOutputStream getFileOutputStream() { return mFileOutput; }

    public Handler getHandler() { return mUpdateHandler; }

    public DocxConvertSample() {
        setTitle(R.string.sample_docxconvert_title);
        setDescription(R.string.sample_docxconvert_description);
        mUpdateHandler = new Handler();
    }

    @Override
    public void run(OutputListener outputListener){
        super.run(outputListener);
        printHeader(outputListener);

        File docx_folder = null;
        File json_data = null;
        FileOutputStream json_output_stream = null;
        File name_map = null;
        try {
            String layoutPluginPath = Utils.copyResourceToTempFolder(MiscellaneousSamplesApplication.getInstance().getContext(), R.raw.pdftron_layout_resources, false, "pdftron_layout_resources.plugin");
            String layoutSmartPluginPath = Utils.copyResourceToTempFolder(MiscellaneousSamplesApplication.getInstance().getContext(), R.raw.pdftron_smart_substitution, false, "pdftron_smart_substitution.plugin");

            File internal_storage = Environment.getExternalStorageDirectory();
            docx_folder = new File(internal_storage, "DocxTes");

            // The output result file
            json_data = new File(docx_folder, "json_data.txt");
            json_output_stream = null;

            // The NameMap file (name map for ScrapedFromWeb)
            name_map = new File(docx_folder, "NameMap.txt");

            mWordToPDFOptions = new WordToPDFOptions();
            mWordToPDFOptions.setLayoutResourcesPluginPath(layoutPluginPath);
            mWordToPDFOptions.setSmartSubstitutionPluginPath(layoutSmartPluginPath);
        }
        catch (PDFNetException e){}

        if (docx_folder != null &&
                !docx_folder.exists())
        {
            outputListener.println("No DocxFile folder found on SD card... Will be using built-in docx files for Demo");
            // first the one-line conversion interface
            simpleDocxConvert("simple-word_2007.docx", "simple-word_2007_a.pdf", outputListener);

            // then the more flexible line-by-line interface
            flexibleDocxConvert("the_rime_of_the_ancient_mariner.docx", "the_rime_of_the_ancient_mariner.pdf", outputListener);

            // then one more, with some advanced layout features
            flexibleDocxConvert("wrap_poly_demo.docx", "wrap_poly_demo.pdf", outputListener);

            printFooter(outputListener);
        }
		else
        {
            try {
                // Read from NameMap.txt and dump into a json object.
                if (name_map != null) {
                    json_output_stream = new FileOutputStream(json_data);
                    FileInputStream name_map_input_stream = new FileInputStream(name_map);
                    String jString;
                    try {
                        FileChannel fc = name_map_input_stream.getChannel();
                        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                /* Instead of using default, pass in a decoder. */
                        jString = Charset.defaultCharset().decode(bb).toString();
                        mNameMap = new JSONObject(jString);
                    } catch (IOException e) {
                        outputListener.println(e.getMessage());
                    } catch (JSONException e) {
                        outputListener.println(e.getMessage());
                    }
                    mFileOutput = json_output_stream;
                }
            }
            catch(FileNotFoundException e)
            {
                outputListener.println("Output stream cannot be created!");
                return;
            }

            outputListener.println("Found DocxFile folder on SD card... Will be using provided docx files for Demo");
            try{
                // DocumentConversion convert;
                if (mFiles == null) {
                    mFiles = new ArrayList<>();
                    new GetFilesFromAssetsTask(this,  mFiles, outputListener, docx_folder.getAbsolutePath(), ".docx").execute();
                }
                else
                {
                    new DocxConversionProgessTask(this, outputListener).execute();
                }
            }
            catch (Exception e){
                outputListener.println(e.getStackTrace());
            }
        }        
    }

    public void simpleDocxConvert(String inputFilename, String outputFilename, OutputListener outputListener)
    {
        try
        {
            // Start with a PDFDoc (the conversion destination)
            PDFDoc pdfdoc = new PDFDoc();

            // perform the conversion with no optional parameters
            Convert.wordToPdf(pdfdoc, Utils.getAssetTempFile(INPUT_PATH + inputFilename).getAbsolutePath(), mWordToPDFOptions);

            // save the result
            pdfdoc.save(Utils.createExternalFile(outputFilename).getAbsolutePath(), SDFDoc.e_linearized, null);

            // And we're done!
            outputListener.println("Done conversion for " + outputFilename);

            addToFileList(outputFilename);
        }
        catch(PDFNetException e)
        {
            outputListener.println("Unable to convert MS Word document, error:");
            e.printStackTrace();
            outputListener.println(e.getMessage());
        }
    }

    public void flexibleDocxConvert(String inputFilename, String outputFilename, OutputListener outputListener)
    {
        try
        {
            // Start with a PDFDoc (the conversion destination)
            PDFDoc pdfdoc = new PDFDoc();

            // create a conversion object -- this sets things up but does not yet
            // perform any conversion logic.
            // in a multithreaded environment, this object can be used to monitor
            // the conversion progress and potentially cancel it as well
            DocumentConversion conversion = Convert.wordToPdfConversion(
                    pdfdoc, Utils.getAssetTempFile(INPUT_PATH + inputFilename).getAbsolutePath(), mWordToPDFOptions);

            outputListener.println(inputFilename + ": " + Math.round(conversion.getProgress() * 100.0)
                    + "% " + conversion.getProgressLabel());

            // actually perform the conversion
            while(conversion.getConversionStatus() == DocumentConversion.e_incomplete){
                conversion.convertNextPage();
                outputListener.println(inputFilename + ": " + Math.round(conversion.getProgress() * 100.0)
                        + "% " + conversion.getProgressLabel());
            }

            if(conversion.tryConvert() == DocumentConversion.e_success)
            {
                int num_warnings = conversion.getNumWarnings();

                // print information about the conversion
                for (int i = 0; i < num_warnings; ++i)
                {
                    outputListener.println("Warning: " + conversion.getWarningString(i));
                }

                // save the result
                pdfdoc.save(Utils.createExternalFile(outputFilename).getAbsolutePath(), SDFDoc.e_linearized, null);

                // done
                outputListener.println("Done conversion for " + outputFilename);
                addToFileList(outputFilename);
            }
            else
            {
                outputListener.println("Encountered an error during conversion: " + conversion.getErrorString());
            }
        }
        catch(PDFNetException e)
        {
            outputListener.println("Unable to convert MS Word document, error:");
            e.printStackTrace();
            outputListener.println(e.getMessage());
        }
    }
}
