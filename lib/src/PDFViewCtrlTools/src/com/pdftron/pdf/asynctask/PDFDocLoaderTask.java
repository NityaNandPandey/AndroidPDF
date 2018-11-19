package com.pdftron.pdf.asynctask;

import android.content.Context;
import android.net.Uri;

import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;

public class PDFDocLoaderTask extends CustomAsyncTask<Uri, Void, PDFDoc> {

    private onFinishListener mListener;

    public PDFDocLoaderTask(Context context) {
        super(context);
    }

    public PDFDocLoaderTask setFinishCallback(onFinishListener listener) {
        mListener = listener;
        return this;
    }

    @Override
    protected PDFDoc doInBackground(Uri... uris) {
        if (uris.length < 1) {
            return null;
        }
        Uri uri = uris[0];
        Context context = getContext();
        if (uri == null || context == null) {
            return null;
        }

        SecondaryFileFilter fileFilter = null;
        PDFDoc pdfDoc = null;
        try {
            fileFilter = new SecondaryFileFilter(context, uri);
            pdfDoc = new PDFDoc(fileFilter);
            return pdfDoc;
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
            return null;
        } finally {
            if (pdfDoc == null) {
                Utils.closeQuietly(fileFilter);
            }
        }
    }

    @Override
    protected void onPostExecute(PDFDoc pdfDoc) {
        super.onPostExecute(pdfDoc);
        if (mListener != null) {
            mListener.onFinish(pdfDoc);
        }
    }

    @Override
    protected void onCancelled(PDFDoc pdfDoc) {
        super.onCancelled(pdfDoc);

        if (mListener != null) {
            mListener.onCancelled();
        }
    }

    /**
     * A interface for listening finish event
     */
    public interface onFinishListener {
        /**
         * This method is invoked when async task is finished
         */
        void onFinish(PDFDoc pdfDoc);
        void onCancelled();
    }
}
