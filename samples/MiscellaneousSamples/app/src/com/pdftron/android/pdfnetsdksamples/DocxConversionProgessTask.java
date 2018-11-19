package com.pdftron.android.pdfnetsdksamples;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pdftron.android.pdfnetsdksamples.samples.DocxConvertSample;
import com.pdftron.android.pdfnetsdksamples.util.Utils;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Convert;
import com.pdftron.pdf.DocumentConversion;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.sdf.SDFDoc;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.DialogInterface;
import android.provider.DocumentsContract;


/**
 * Created by Renchen on 6/18/2015.
 */
public class DocxConversionProgessTask extends AsyncTask<DocxConvertSample, String, Map<String,String>>
{
    private final int CONVERTED_SUCESS = 0;
    private final int CONVERTED_FAIL = 1;
    private final int CONVERTED_INPROGRESS = 2;
    private final int OTHER_FAIL = 3;

    private ProgressDialog mProgessDialog;
    private List<File> mFiles;
    private Handler mHandler;
    private DocxConvertSample mSample;
    private OutputListener mLisener;

    public DocxConversionProgessTask(DocxConvertSample args, OutputListener lisener)
    {
        mProgessDialog = new ProgressDialog(SampleListActivity.getInstance());
        mFiles = args.getListFiles();
        mHandler = args.getHandler();
        mSample = args;
        mLisener = lisener;
    }

    @Override
    protected void onPreExecute()
    {
        mProgessDialog.setTitle("Converting documents...");
        mProgessDialog.setMessage("Converting in progress...");
        mProgessDialog.setProgressStyle(mProgessDialog.STYLE_HORIZONTAL);
        mProgessDialog.setProgress(0);
        mProgessDialog.setMax(mFiles.size());
        mProgessDialog.setCancelable(false);

        mProgessDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        mProgessDialog.show();
    }

    @Override
    protected void onPostExecute(Map<String,String> result)
    {
        try {
            mSample.getFileOutputStream().flush();
            mSample.getFileOutputStream().close();
        }
        catch(IOException e){}

        if (result.size() > 0)
        {
            mLisener.println("Error occurs during conversion. Success Rate: " + (double)(result.size()) / mFiles.size() * 100 + "%");
            for (Map.Entry<String, String> item : result.entrySet())
            {
                mLisener.println(item.getKey() + " : " + item.getValue());
            }
            mSample.printFooter(mLisener);
        }
        else
        {
            mLisener.println("Everything is ok!");
        }
    }

    @Override
    protected void onProgressUpdate(String... values)
    {
        int flag;
        try{
            flag = Integer.parseInt(values[0]);
        }
        catch(NumberFormatException e) {
            flag = -1;
        }

        switch (flag)
        {
            case CONVERTED_SUCESS:
                mProgessDialog.setMessage(values[1]);
                mProgessDialog.incrementProgressBy(1);
                mLisener.println(values[2]);
                break;
            case CONVERTED_FAIL:
                mProgessDialog.setMessage(values[1]);
                mProgessDialog.incrementProgressBy(1);
                mLisener.println(values[1]);
                break;
            case CONVERTED_INPROGRESS:
                mProgessDialog.setTitle(values[1]);
                mProgessDialog.setMessage("");
                break;
            case OTHER_FAIL:
                mProgessDialog.setMessage(values[1]);
                mLisener.println(values[1]);
                break;
            default:
                // mProgessDialog.setMessage(values[0]);
                mLisener.println(values[0]);
        }
    }

    @Override
    protected Map<String,String> doInBackground(DocxConvertSample... args){
        Map<String,String> crashed_docs = new HashMap<>();
        int total = mFiles.size();
        int converted = 0;
        int exception = 0;
        DocxQAReportWriter qaReporter = null;

        try{
            qaReporter = new DocxQAReportWriter(mSample.getFileOutputStream());
            qaReporter.BeginArray();
        }
        catch(IOException e){
        }

        int counter = mFiles.size();
        for (File file : mFiles)
        {
            try
            {
                if (counter-- == 0){break;}
                PDFDoc doc = new PDFDoc();
                publishProgress(Integer.toString(CONVERTED_INPROGRESS), "Converting " + file.getName());

                DocumentConversion docconvert = Convert.wordToPdfConversion(doc, file.getAbsolutePath(), mSample.getmWordToPDFOptions());
                doc.getPageCount();

                int result = docconvert.tryConvert();
                String bson = docconvert.getWarningString(-1);

                JSONObject jObj = new JSONObject(bson);
                double totalTime = jObj.getDouble("total_time");
                DocumentObject docObj = new DocumentObject(mSample.getNameMap().getString(file.getName()), doc.getPageCount(), totalTime);

                try{
                    String warnings = jObj.getString("Warnings");
                    docObj.setmWarnings(warnings);
                }
                catch(JSONException e){}

                try{
                    String errors = jObj.getString("Errors");
                    docObj.setmErrors(errors);
                }
                catch(JSONException e){}

                try{
                    String time_stats = jObj.getString("time_stats");
                    docObj.setmTimeStats(time_stats);
                }
                catch(JSONException e){}

                qaReporter.WriteDocumentObject(docObj);
                /*
                FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());
                MessageDigest md = MessageDigest.getInstance("MD5");
                */

                publishProgress(bson);
                converted = converted + 1;

                String updateString = file.getName() + " Converted " + " [" + (double) converted / total * 100 + "% ] Done";
                publishProgress(Integer.toString(CONVERTED_SUCESS), file.getName() + " Converted Sucessfully ", updateString);

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

                String fileWithoutExt = FilenameUtils.removeExtension(file.getName());
                doc.save(Utils.createExternalFile(fileWithoutExt + ".pdf").getAbsolutePath(), SDFDoc.e_linearized, null);
                doc.close();
                mSample.addToFileList(fileWithoutExt + ".pdf");
            }
            catch (PDFNetException e)
            {
                exception = exception + 1;
                crashed_docs.put(file.getName(), e.getMessage());
                String tmp = Integer.toString(CONVERTED_FAIL) + file.getName() + " Failed. Details: " + e.getMessage();
                publishProgress(tmp);
            }
            catch (Exception e)
            {
                publishProgress(Integer.toString(OTHER_FAIL), file.getName() + " Failed for non-PDFNet reasons... Details: " + e.getStackTrace());
            }
        }

        try {
            qaReporter.EndArray();
        }
        catch(IOException e){}
        return crashed_docs;
    }
}
