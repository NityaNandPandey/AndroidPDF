//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.pdftron.android.pdfnetsdksamples.GetFilesFromAssetsTask;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFNet;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.pdftron.android.pdfnetsdksamples.MiscellaneousSamplesApplication;
import com.pdftron.android.pdfnetsdksamples.R;

public class Utils {
    
    /**
     * Get an InputStream object from an asset file. The file path is
     * relative to the root of the assets folder.
     * 
     * @param filePath the file path to the file in assets folder
     * @return an InputStream of the file
     */
    public static InputStream getAssetInputStream(String filePath) {
        try {
            return MiscellaneousSamplesApplication.getInstance().getAssets().open(filePath);
        } catch (IOException e) {
            return null;
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
    public static File getAssetTempFile(String filePath) {
        File file = null;
        try {
            file = new File(MiscellaneousSamplesApplication.getInstance().getCacheDir(), FilenameUtils.getName(filePath));
            InputStream inputStream = getAssetInputStream(filePath);
            FileOutputStream output = new FileOutputStream(file); 
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
            inputStream.close();
        } catch (Exception e) {
            return null;
        }
        return file;
    }

    public static String copyResourceToTempFolder(Context context, int resId, boolean force, String resourceName) throws PDFNetException {
        if(context == null) {
            throw new PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "copyResourceToTempFolder()", "Context cannot be null to initialize resource file.");
        } else {
            File resFile = new File(context.getFilesDir() + File.separator + "resourceName");
            if(!resFile.exists() || force) {
                File filesDir = context.getFilesDir();
                StatFs stat = new StatFs(filesDir.getPath());
                long size = (long)stat.getAvailableBlocks() * (long)stat.getBlockSize();
                if(size < 2903023L) {
                    throw new PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "copyResourceToTempFolder()", "Not enough space available to copy resources file.");
                }

                Resources rs = context.getResources();

                try {
                    InputStream e = rs.openRawResource(resId);
                    FileOutputStream fos = context.openFileOutput(resourceName, 0);
                    byte[] buffer = new byte[1024];

                    int read;
                    while((read = e.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }

                    e.close();
                    fos.flush();
                    fos.close();

                } catch (Resources.NotFoundException var13) {
                    throw new PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "initializeResource()", "Resource file ID does not exist.");
                } catch (FileNotFoundException var14) {
                    throw new PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "initializeResource()", "Resource file not found.");
                } catch (IOException var15) {
                    throw new PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "initializeResource()", "Error writing resource file to internal storage.");
                } catch (Exception var16) {
                    throw new PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "initializeResource()", "Unknown error.");
                }
            }

            return context.getFilesDir().getAbsolutePath();
        }
    }

    /**
     * Get all files under assets/folder_name with 'extension' ext.
     * @param folder_name the path name under assets folder
     * @param extension
     * @return
     */
    public static List<File> getListFiles(String folder_name, String extension){
        ArrayList<File> inFiles = new ArrayList<File>();
        try{
            String[] files = MiscellaneousSamplesApplication.getInstance().getAssets().list(folder_name);

            for (String filename : files){
                if (filename.endsWith(extension)) {
                    inFiles.add(getAssetTempFile(folder_name + "/" + filename));
                }
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
        return inFiles;
    }
    
    /**
     * Get a Bitmap object from a file stored in the assets folder.
     * The file path is relative to the root of the assets folder.
     * 
     * @param filePath the file path to the file in assets folder
     * @return a Bitmap object for the supplied file
     */
    public static Bitmap getAssetBitmap(String filePath) {
        return BitmapFactory.decodeStream(getAssetInputStream(filePath));
    }
    
    /**
     * Creates an external file on the external filesystem where the
     * application can place persistent files it owns.
     *  
     * @param fileName the file name for the file to be created
     * @return a File object
     */
    public static File createExternalFile(String fileName) {
        return new File(MiscellaneousSamplesApplication.getInstance().getExternalFilesDir(null), fileName);
    }
    
    /**
     * Returns the absolute path to the directory on the external filesystem
     * (that is somewhere on Environment.getExternalStorageDirectory()) where
     * the application can place persistent files it owns.
     * 
     * @return the absolute file path
     */
    public static String getExternalFilesDirPath() {
        return MiscellaneousSamplesApplication.getInstance().getExternalFilesDir(null).getAbsolutePath();
    }
    
    public static void showAbout(FragmentActivity activity) {
        DialogFragment newFragment = new AboutDialog();
        newFragment.show(activity.getSupportFragmentManager(), "dialog_about");
    }

    public static class AboutDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get PDFNet version
            double pdfnetVersion = 0;
            try {
                pdfnetVersion = PDFNet.getVersion();
            } catch (Exception e) {
                // Do nothing
            }
            // Get application version
            String versionName = String.valueOf(pdfnetVersion);

            // Build the dialog body view
            SpannableStringBuilder aboutBody = new SpannableStringBuilder();
            aboutBody.append(Html.fromHtml(getString(R.string.about_body, versionName)));

            LayoutInflater layoutInflater = (LayoutInflater) getActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            TextView aboutBodyView = (TextView) layoutInflater.inflate(R.layout.dialog_about, null);
            aboutBodyView.setText(aboutBody);
            aboutBodyView.setMovementMethod(new LinkMovementMethod());

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.about)
                    .setView(aboutBodyView)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }
    }
}
