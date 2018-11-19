//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * An async task for basic HTTP downloading
 */
public class BasicHTTPDownloadTask extends CustomAsyncTask<String, Void, Boolean> {

    private String mURL;
    private File mSaveFile;
    private BasicHTTPDownloadTaskListener mListener;

    /**
     * @param context The context
     * @param listener HTTP download task listener
     * @param url Source URL to download
     * @param saveFile The file where it save to
     */
    public BasicHTTPDownloadTask(Context context, BasicHTTPDownloadTaskListener listener, String url, File saveFile) {
        super(context);
        mURL = url;
        mSaveFile = saveFile;
        mListener = listener;
    }

    /**
     * Overload implementation of {@link CustomAsyncTask#doInBackground(Object[])}.
     * It downloads the URL to the file
     * @param params parameters
     * @return true if download success, false otherwise
     */
    @Override
    protected Boolean doInBackground(String... params) {
        URL url;
        OutputStream filestream = null;
        try {
            url = new URL(mURL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            filestream = new BufferedOutputStream(new FileOutputStream(mSaveFile));
            IOUtils.copy(urlConnection.getInputStream(), filestream);
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            IOUtils.closeQuietly(filestream);
        }

        return true;
    }

    /**
     * Overload implemntation of {@link CustomAsyncTask#onPostExecute(Object)}
     * It invokes the download listener.
     * @param pass Whether it successfully downloads the file.
     */
    @Override
    protected void onPostExecute(Boolean pass) {
        mListener.onDownloadTask(pass, mSaveFile);
    }

    /**
     * Callback interface to be invoked when basic HTTP download is finished.
     */
    public interface BasicHTTPDownloadTaskListener {
        /**
         * Called when download task has been done.
         *
         * @param pass True if successful
         * @param saveFile The saved file
         */
        void onDownloadTask(Boolean pass, File saveFile);
    }
}
