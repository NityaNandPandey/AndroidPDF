//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.sdf.Obj;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Helper class for annotation copy/paste
 */
public class AnnotationClipboardHelper {
    private static Obj sCurrentAnnotation = null;
    private static Rect sBoundingBox = null;
    private static Lock sClipboardLock = new ReentrantLock();

    /**
     * Callback interface to be invoked when clipboard copy/paste task is finished.
     */
    public interface OnClipboardTaskListener {
        /**
         * Called when clipboard copy/paste task has been done.
         *
         * @param error The error message. Null if there is no error.
         */
        void onnClipboardTaskDone(String error);
    }

    /**
     * Copies an annotation to the clipboard.
     *
     * @param context The context
     * @param annot The annotation to be copied
     * @param pdfViewCopyFrom The PDFViewCtrl containing the annotation
     * @param listener The listener to be called when the task is finished
     */
    public static void copyAnnot(Context context, Annot annot, PDFViewCtrl pdfViewCopyFrom, OnClipboardTaskListener listener) {
        new CopyPasteTask(context, pdfViewCopyFrom, annot, null, 0, null, listener).execute();
        // clear system clipboard
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("text", "");
            clipboard.setPrimaryClip(clip);
        }
    }

    /**
     * Paste the annotation from the clipboard.
     *
     * @param context The context
     * @param pdfViewPasteTo The PDFViewCtrl to which the annotation should be pasted
     * @param pageNo The destination page number
     * @param target The destination location
     * @param listener The listener to be called when the task is finished
     */
    public static void pasteAnnot(Context context, PDFViewCtrl pdfViewPasteTo, int pageNo, PointF target, OnClipboardTaskListener listener) {
        new CopyPasteTask(context, null, null, pdfViewPasteTo, pageNo, target, listener).execute();
    }

    /**
     * Removes any annotation existing on the clipboard.
     */
    @SuppressWarnings("unused")
    public static void clearClipboard() {
        sClipboardLock.lock();
        sCurrentAnnotation = null;
        sBoundingBox = null;
        sClipboardLock.unlock();
    }

    /**
     * @return True if there is annotation in clipboard.
     */
    public static boolean isAnnotCopied() {
        sClipboardLock.lock();
        try {
            return sCurrentAnnotation != null;
        } finally {
            sClipboardLock.unlock();
        }
    }

    public static boolean isItemCopied(@Nullable Context context) {
        return isAnnotCopied() || Utils.isImageCopied(context);
    }

    private static class CopyPasteTask extends CustomAsyncTask<Void, Void, String> {
        private Annot mAnnotToCopy;
        private PDFViewCtrl mPdfViewCopyFrom;
        private PDFViewCtrl mPdfViewToPaste;
        private int mPageNoToPaste;
        private PDFDoc mDoc;
        private Handler mHandler;
        private ProgressDialog mProgress = null;
        private PointF mTarget;
        private double[] mPageTarget;
        private Annot mPastedAnnot;
        private OnClipboardTaskListener mOnClipboardTaskListener;

        CopyPasteTask(Context context, PDFViewCtrl pdfViewCopyFrom, Annot annotToCopy,
                      PDFViewCtrl pdfViewToPaste, int pageNoToPaste, PointF target,
                      OnClipboardTaskListener listener) {
            super(context);
            mAnnotToCopy = annotToCopy;
            mPdfViewToPaste = pdfViewToPaste;
            mPdfViewCopyFrom = pdfViewCopyFrom;
            mPageNoToPaste = pageNoToPaste;
            mHandler = new Handler();
            mTarget = target;
            mOnClipboardTaskListener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            final Context context = getContext();
            if (context == null) {
                return;
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mProgress = new ProgressDialog(context);
                    mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mProgress.setMessage(context.getString((mAnnotToCopy != null ? R.string.tools_copy_annot_waiting : R.string.tools_paste_annot_waiting)));
                    mProgress.show();
                }
            }, 750);
            if (mPdfViewToPaste != null) {
                mDoc = mPdfViewToPaste.getDoc();
                mPageTarget = mPdfViewToPaste.convScreenPtToPagePt(mTarget.x, mTarget.y, mPageNoToPaste);
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            String error = null;
            boolean shouldUnlockView = false;
            boolean shouldUnlockClipboard = false;
            if (mAnnotToCopy != null) {
                try {
                    //noinspection WrongThread
                    mPdfViewCopyFrom.docLock(true);
                    shouldUnlockView = true;

                    Obj srcAnnotation = mAnnotToCopy.getSDFObj();

                    PDFDoc tempDoc = new PDFDoc();

                    Obj p = srcAnnotation.findObj("P");
                    if (p == null) {
                        return "Cannot find the object";
                    }
                    Obj[] pageArray = {p};
                    Obj[] srcAnnotArray = {srcAnnotation};

                    sClipboardLock.lock();
                    shouldUnlockClipboard = true;
                    sBoundingBox = mAnnotToCopy.getRect();
                    sBoundingBox.normalize();
                    sCurrentAnnotation = tempDoc.getSDFDoc().importObjs(srcAnnotArray, pageArray)[0];
                } catch (Exception ex) {
                    // make sure we always have a non-null error string
                    error = (ex.getMessage() != null) ? ex.getMessage() : "Unknown Error";
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                } finally {
                    if (shouldUnlockView) {
                        //noinspection WrongThread
                        mPdfViewCopyFrom.docUnlock();
                    }
                    if (shouldUnlockClipboard) {
                        sClipboardLock.unlock();
                    }
                }
            } else {
                try {
                    if (!isAnnotCopied()) {
                        return null;
                    }
                    //noinspection WrongThread
                    mPdfViewToPaste.docLock(true);
                    shouldUnlockView = true;
                    sClipboardLock.lock();
                    shouldUnlockClipboard = true;

                    Obj destAnnot = mDoc.getSDFDoc().importObj(sCurrentAnnotation, true);
                    Rect boundingBox = new Rect(sBoundingBox.getX1(), sBoundingBox.getY1(),
                            sBoundingBox.getX2(), sBoundingBox.getY2());
                    boundingBox.normalize();

                    sClipboardLock.unlock();
                    shouldUnlockClipboard = false;

                    Annot newAnnot = new Annot(destAnnot);
                    if (mPdfViewToPaste.getToolManager() instanceof ToolManager) {
                        ToolManager toolManager = (ToolManager) mPdfViewToPaste.getToolManager();
                        String key = toolManager.generateKey();
                        if (key != null) {
                            newAnnot.setUniqueID(key);
                        }
                    }

                    Page page = mDoc.getPage(mPageNoToPaste);

                    Rect cropBox = page.getBox(Page.e_user_crop);

                    double pushX = -boundingBox.getX1() + mPageTarget[0];
                    double pagePushY = cropBox.getY2() - mPageTarget[1];
                    double pushY = cropBox.getY2() - boundingBox.getY2() - pagePushY;

                    page.annotPushBack(newAnnot);
                    Rect rect = newAnnot.getRect();

                    double x1 = rect.getX1() + pushX - (boundingBox.getWidth() / 2); // center the annot
                    double y1 = rect.getY1() + pushY + (boundingBox.getHeight() / 2); // center the annot

                    rect.setX1(x1);
                    rect.setY1(y1);
                    rect.setX2(x1 + boundingBox.getWidth());
                    rect.setY2(y1 + boundingBox.getHeight());

                    if (rect.getX1() < 0.0) {
                        rect.setX1(0);
                        rect.setX2(boundingBox.getWidth());
                    }

                    if (rect.getX2() >= cropBox.getX2()) {
                        rect.setX1(cropBox.getX2() - boundingBox.getWidth());
                        rect.setX2(cropBox.getX2());
                    }

                    if (rect.getY1() < 0.0) {
                        rect.setY1(0);
                        rect.setY2(boundingBox.getHeight());
                    }

                    if (rect.getY2() > cropBox.getY2()) {
                        rect.setY1(cropBox.getY2() - boundingBox.getHeight());
                        rect.setY2(cropBox.getY2());
                    }

                    newAnnot.resize(rect);

                    if (newAnnot.getType() == Annot.e_FreeText) {
                        int pageRotation = mPdfViewToPaste.getDoc().getPage(mPageNoToPaste).getRotation();
                        int viewRotation = mPdfViewToPaste.getPageRotation();
                        int annotRotation = ((pageRotation + viewRotation) % 4) * 90;
                        newAnnot.setRotation(annotRotation);
                    }

                    AnnotUtils.refreshAnnotAppearance(mPdfViewToPaste.getContext(), newAnnot);

                    mPastedAnnot = newAnnot;
                } catch (Exception ex) {
                    // make sure we always have a non-null error string
                    error = (ex.getMessage() != null) ? ex.getMessage() : "Unknown Error";
                    AnalyticsHandlerAdapter.getInstance().sendException(ex);
                } finally {
                    if (shouldUnlockView) {
                        //noinspection WrongThread
                        mPdfViewToPaste.docUnlock();
                    }
                    if (shouldUnlockClipboard) {
                        sClipboardLock.unlock();
                    }
                }
            }
            return error;
        }

        @Override
        public void onPostExecute(String error) {
            if (null != error) {
                // something went wrong, report error callback
                if (mPdfViewToPaste != null && mPdfViewToPaste.getToolManager() != null &&
                        mPdfViewToPaste.getToolManager() instanceof ToolManager) {
                    ToolManager toolManager = (ToolManager) mPdfViewToPaste.getToolManager();
                    toolManager.annotationCouldNotBeAdded(error);
                }
            } else {
                if (mPdfViewToPaste != null && isAnnotCopied()) {
                    try {
                        mPdfViewToPaste.update(mPastedAnnot, mPageNoToPaste);
                        if (mPdfViewToPaste.getToolManager() != null &&
                                mPdfViewToPaste.getToolManager() instanceof ToolManager) {
                            ToolManager toolManager = (ToolManager) mPdfViewToPaste.getToolManager();
                            HashMap<Annot, Integer> annots = new HashMap<>(1);
                            annots.put(mPastedAnnot, mPageNoToPaste);
                            toolManager.raiseAnnotationsAddedEvent(annots);
                        }
                    } catch (Exception ex) {
                        AnalyticsHandlerAdapter.getInstance().sendException(ex);
                    }
                }
            }

            mHandler.removeCallbacksAndMessages(null);
            if (mProgress != null) {
                if (mProgress.isShowing())
                    mProgress.dismiss();
                mProgress = null;
            }

            if (mOnClipboardTaskListener != null) {
                mOnClipboardTaskListener.onnClipboardTaskDone(error);
            }
        }
    }
}
