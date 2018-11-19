package com.pdftron.android.pdfnetsdksamples;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.pdftron.android.pdfnetsdksamples.samples.DocxConvertSample;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

import org.apache.commons.io.FilenameUtils;

/**
 * Created by Renchen on 6/19/2015.
 */
public class GetFilesFromAssetsTask extends AsyncTask<Void, String, List<File>>{
    private File mFolder;
    private String mExt;
    private ProgressDialog mProgessDialog;
    private List<File> mFiles;
    private OutputListener mOutputlis;
    private DocxConvertSample mDocxConvertSample;

    public GetFilesFromAssetsTask(DocxConvertSample docx, List<File> result, OutputListener outputLis, String FolderPath, String Ext)
    {
        mProgessDialog = new ProgressDialog(SampleListActivity.getInstance());
        mFolder = new File(FolderPath);
        mExt = Ext;
        mFiles = result;
        mOutputlis = outputLis;
        mDocxConvertSample = docx;
    }

    @Override
    protected void onPostExecute(List<File> files)
    {
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
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
        mProgessDialog.dismiss();
        AsyncTask task = new DocxConversionProgessTask(mDocxConvertSample, mOutputlis).execute();
    }

    @Override
    protected void onPreExecute()
    {
        mProgessDialog.setTitle("Please wait while I fetching files for converting...");
        // mProgessDialog.setMessage("...");
        mProgessDialog.setProgressStyle(mProgessDialog.STYLE_SPINNER);
        mProgessDialog.setCancelable(false);

        mProgessDialog.show();
    }

    @Override
    protected void onProgressUpdate(String... fileName)
    {
        mProgessDialog.setMessage(fileName[0] + " is added to queue...");
        // Toast.makeText(SampleListActivity.getInstance(), fileName[0] + " is added to queue...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected List<File> doInBackground(Void... parms)
    {
        try {
            String[] files = mFolder.list();
            for (String filename : files){
                if (filename.endsWith(mExt)) {
                    publishProgress(filename);
                    if (filename == "1879.docx")
                    {
                        boolean tmp = false;
                    }
                    mFiles.add(new File(mFolder, filename));
                }
            }
            return mFiles;
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
        return mFiles;
    }
}
