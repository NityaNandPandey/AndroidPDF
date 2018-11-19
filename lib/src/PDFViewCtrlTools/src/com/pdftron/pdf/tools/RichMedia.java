//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.pdftron.filters.Filter;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.FileSpec;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.NameTree;
import com.pdftron.sdf.NameTreeIterator;
import com.pdftron.sdf.Obj;

import java.io.File;
import java.util.Arrays;

/**
 * A tool for handling single tap on rich media annotation
 */
@Keep
public class RichMedia extends Tool {

    // http://developer.android.com/guide/appendix/media-formats.html#core
    private static final String[] SUPPORTED_FORMATS = {".3gp", ".mp4", ".m4a", ".ts", ".webm", ".mkv", ".mp3", ".ogg", ".wav"};

    private CustomRelativeLayout mRootView;
    private VideoView mVideoView;

    // When adding or removing the VideoView widget, the content behind the widget is shown in its
    // place for an instant. To avoid this, we use another view on top of it to temporarily hide
    // any content.
    private FrameLayout mCoverView;
    protected int mCoverHideTime = 250;

    /**
     * Class constructor
     */
    public RichMedia(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mRootView = null;
        mVideoView = null;
        mCoverView = null;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.RICH_MEDIA;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Unknown;
    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        handleRichMediaAnnot(e);
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onLongPress(MotionEvent)}.
     */
    @Override
    public boolean onLongPress(MotionEvent e) {
        handleRichMediaAnnot(e);
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onDoubleTap(MotionEvent)}.
     */
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return true;
    }

    /**
     * The overload implementation of {@link Tool#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        // VideoView widget do not behave properly when scaling for older versions of Android
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mVideoView != null || mCoverView != null) {
            new CloseVideoViewTask().execute();
        }
    }

    private void handleRichMediaAnnot(MotionEvent e) {
        if (mAnnot == null) {
            return;
        }

        int x = (int) (e.getX() + 0.5);
        int y = (int) (e.getY() + 0.5);

        // Let's stay on this mode by default
        mNextToolMode = getToolMode();

        Annot tempAnnot = mPdfViewCtrl.getAnnotationAt(x, y);
        if (mAnnot.equals(tempAnnot)) {
            if (onInterceptAnnotationHandling(mAnnot)) {
                return;
            }
            handleRichMediaAnnot(mAnnot, mAnnotPageNum);
        } else {
            // Stop playback and quit current mode
            new CloseVideoViewTask().execute();
            mNextToolMode = ToolMode.PAN;
        }
    }

    public void handleRichMediaAnnot(Annot annot, int pageNum) {
        mAnnot = annot;
        mAnnotPageNum = pageNum;
        mNextToolMode = getToolMode();
        if (mVideoView == null) {
            // Extract embedded media from PDF and start playing
            new ExtractMediaTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, annot);
        }
    }

    /**
     * The overload implementation of {@link Tool#onClose()}.
     */
    @Override
    public void onClose() {
        if (mVideoView != null || mCoverView != null) {
            new CloseVideoViewTask().execute();
        }
    }

    private void setupAndPlayMedia(String path) {
        View view = LayoutInflater.from(mPdfViewCtrl.getContext()).inflate(R.layout.controls_rich_media_layout, mPdfViewCtrl, true);
        mRootView = view.findViewById(R.id.root_layout);
        mVideoView = mRootView.findViewById(R.id.video_view);

        mCoverView = mRootView.findViewById(R.id.cover_view);

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                new StartVideoViewTask().execute();
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                CommonToast.showText(mPdfViewCtrl.getContext(), getStringFromResId(R.string.tools_richmedia_playback_end), Toast.LENGTH_SHORT);
            }
        });

        mRootView.setAnnot(mPdfViewCtrl, mAnnot, mAnnotPageNum);
        mVideoView.setVideoPath(path);
        mVideoView.setMediaController(new MediaController(mPdfViewCtrl.getContext()));
    }

    private boolean isMediaFileValid(String result) {
        if (result.isEmpty()) {
            // Error while extracting the media
            CommonToast.showText(mPdfViewCtrl.getContext(), getStringFromResId(R.string.tools_richmedia_error_extracting_media), Toast.LENGTH_LONG);
            return false;

        } else if (!isMediaFileSupported(result)) {
            // Let's do a preliminary filter on the file before letting the VideoView complain
            CommonToast.showText(mPdfViewCtrl.getContext(), getStringFromResId(R.string.tools_richmedia_unsupported_format), Toast.LENGTH_LONG);
            return false;
        }

        return true;
    }

    /**
     * Checks whether the media file is supported.
     *
     * @param fileName The file name
     * @return True if the media file is supported
     */
    protected boolean isMediaFileSupported(String fileName) {
        int idx = fileName.lastIndexOf(".");
        return ((idx != -1) && Arrays.asList(SUPPORTED_FORMATS).contains(fileName.substring(idx)));
    }

    private class StartVideoViewTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            publishProgress();
            try {
                Thread.sleep(mCoverHideTime);
            } catch (InterruptedException e) {
                // Do nothing
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            if (mVideoView != null) {
                mVideoView.start();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mCoverView != null) {
                mCoverView.setVisibility(View.INVISIBLE);
            }
        }
    }

    private class CloseVideoViewTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            publishProgress();
            try {
                Thread.sleep(mCoverHideTime);
            } catch (InterruptedException e) {
                // Do nothing
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);

            if (mCoverView != null) {
                mCoverView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mRootView != null) {
                if (mVideoView != null) {
                    mVideoView.stopPlayback();
                }
                mPdfViewCtrl.removeView(mRootView);
                mRootView = null;
                mVideoView = null;
                mCoverView = null;
            }
        }
    }

    private class ExtractMediaTask extends CustomAsyncTask<Annot, Void, String> {
        private ProgressDialog dialog = null;

        ExtractMediaTask() {
            super(mPdfViewCtrl.getContext());
        }

        @Override
        protected String doInBackground(Annot... annots) {
            String fileName = "";

            boolean shouldUnlockRead = false;
            try {
                //noinspection WrongThread
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                // Extract media to device
                Obj ad = annots[0].getSDFObj();
                Obj mc = ad.findObj("RichMediaContent");
                if (mc != null) {
                    NameTree assets = new NameTree(mc.findObj("Assets"));
                    if (assets.isValid()) {
                        NameTreeIterator j = assets.getIterator();
                        for (; j.hasNext(); j.next()) {
                            String asset_name = j.key().getAsPDFText();

                            // Before going on with the extraction, let's check if the file
                            // already exists in our temp folder and if it is in the supported
                            // formats list.

                            // TODO Make the file name unique
                            // We could have in the same document two or more rich media annotations
                            // with the same asset name.
                            File cropName = new File(asset_name); // if the name is a full path, let's get the name
                            File externalFileDir = Utils.getExternalFilesDir(getContext(), null);
                            if (externalFileDir == null) {
                                return "";
                            }
                            File file = new File(externalFileDir, cropName.getName());
                            if (file.exists()) {
                                fileName = file.getAbsolutePath();
                                break;
                            } else {
                                if (isMediaFileSupported(asset_name)) {
                                    FileSpec file_spec = new FileSpec(j.value());
                                    Filter stm = file_spec.getFileData();
                                    if (stm != null) {
                                        stm.writeToFile(file.getAbsolutePath(), false);
                                        fileName = file.getAbsolutePath();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Error while extracting media
                fileName = "";
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    //noinspection WrongThread
                    mPdfViewCtrl.docUnlockRead();
                }
            }

            return fileName;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (dialog != null) {
                dialog.dismiss();
            }

            if (isMediaFileValid(result)) {
                setupAndPlayMedia(result);
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            dialog = new ProgressDialog(mPdfViewCtrl.getContext());
            dialog.setMessage(getStringFromResId(R.string.tools_richmedia_please_wait_loading));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            dialog.show();
        }
    }
}
