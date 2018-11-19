//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.PageIterator;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.tools.R;
import com.pdftron.sdf.Obj;

import java.lang.ref.WeakReference;

/**
 * A utility class for user crop
 */
public class UserCropUtilities {

    /**
     * An async task for auto cropping in background thread
     */
    static public class AutoCropInBackgroundTask extends CustomAsyncTask<Void, Integer, Boolean> {

        private static final String TAG = AutoCropInBackgroundTask.class.getName();

        private static final int MINIMUM_CROP_RECT_SIZE = 10;
        private static final int CROP_RECT_WHITE_SPACE_MARGIN = 2;

        private WeakReference<PDFViewCtrl> mPdfViewCtrlRef;
        private PDFDoc mDoc;
        private Rect[] mUserCropRects;
        private int mMaxPages;

        ProgressDialog mAutoCropProgressDialog;

        private AutoCropTaskListener mAutoCropTaskListener;

        /**
         * Callback interface to be invoked when auto crop task is finished.
         */
        public interface AutoCropTaskListener {
            /**
             * Called when auto crop task has been done.
             */
            void onAutoCropTaskDone();
        }

        public AutoCropInBackgroundTask(Context context, PDFViewCtrl pdfViewCtrl, AutoCropTaskListener listener) {
            super(context);

            mPdfViewCtrlRef = new WeakReference<>(pdfViewCtrl);
            mAutoCropTaskListener = listener;

            mDoc = pdfViewCtrl.getDoc();
            boolean shouldUnlockRead = false;
            try {
                mDoc.lockRead();
                shouldUnlockRead = true;
                mMaxPages = mDoc.getPageCount();
                mUserCropRects = new Rect[mMaxPages];
            } catch (PDFNetException ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex, "USER_CROP");
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(mDoc);
                }
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Context context = getContext();
            if (context == null) {
                return;
            }

            PDFViewCtrl pdfViewCtrl = mPdfViewCtrlRef.get();
            if (pdfViewCtrl == null) {
                return;
            }
            pdfViewCtrl.cancelRendering();

            mAutoCropProgressDialog = new ProgressDialog(context);
            mAutoCropProgressDialog.setMessage(context.getResources().getString(R.string.user_crop_auto_crop_title));
            mAutoCropProgressDialog.setIndeterminate(false);
            mAutoCropProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mAutoCropProgressDialog.setCancelable(true);
            mAutoCropProgressDialog.setCanceledOnTouchOutside(false);
            mAutoCropProgressDialog.setProgress(0);
            mAutoCropProgressDialog.setMax(mMaxPages);
            mAutoCropProgressDialog.setProgressNumberFormat(context.getResources().getString(R.string.user_crop_auto_crop_progress));
            mAutoCropProgressDialog.setProgressPercentFormat(null);
            mAutoCropProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    AutoCropInBackgroundTask.this.cancel(false);
                }
            });

            mAutoCropProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean success = false;
            Rect previousRect = null;
            boolean shouldUnlockRead = false;

            try {
                mDoc.lockRead();
                shouldUnlockRead = true;

                PageIterator itr = mDoc.getPageIterator();
                int lastPageDone = 0;

                while (!isCancelled() && itr.hasNext()) {
                    Object obj = itr.next();
                    Page page = (Page) obj;
                    try {
                        Rect cropRect = page.getCropBox();
                        Rect visibleRect = page.getVisibleContentBox();
                        visibleRect.inflate(CROP_RECT_WHITE_SPACE_MARGIN);

                        boolean intersects = visibleRect.intersectRect(visibleRect, cropRect);
                        if (intersects
                                && (cropRect.getWidth() - visibleRect.getWidth() > 0.5 || cropRect.getHeight() - visibleRect.getHeight() > 0.5)
                                && (visibleRect.getHeight() > MINIMUM_CROP_RECT_SIZE && visibleRect.getWidth() > MINIMUM_CROP_RECT_SIZE)) {
                            mUserCropRects[lastPageDone] = visibleRect;
                        } else if (previousRect != null) { // just center a crop rect on the page with the same size as the last crop rect
                            visibleRect.set(cropRect.getX1(), cropRect.getY1(), cropRect.getX2(), cropRect.getY2());
                            double prevWidth = previousRect.getWidth();
                            double prevHeight = previousRect.getHeight();
                            if (visibleRect.getWidth() > prevWidth) {
                                double newMargin = (visibleRect.getWidth() - prevWidth) / 2.0;
                                visibleRect.setX1(visibleRect.getX1() + newMargin);
                                visibleRect.setX2(visibleRect.getX2() - newMargin);
                            }
                            if (visibleRect.getHeight() > prevHeight) {
                                double newMargin = (visibleRect.getHeight() - prevHeight) / 2.0;
                                visibleRect.setY1(visibleRect.getY1() + newMargin);
                                visibleRect.setY2(visibleRect.getY2() - newMargin);
                            }
                            mUserCropRects[lastPageDone] = visibleRect;
                        }
                    } catch (PDFNetException ignored) {

                    }
                    previousRect = mUserCropRects[lastPageDone];
                    lastPageDone++;
                    publishProgress(lastPageDone);
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "USER_CROP");
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(mDoc);
                }
            }

            boolean shouldUnlock = false;
            try {
                mDoc.lock();
                shouldUnlock = true;

                // too late to cancel now, as we are actually modifying the document
                // this part should be fairly quick though
                PageIterator itr = mDoc.getPageIterator();
                int lastPageDone = 0;

                while (!isCancelled() && itr.hasNext()) {
                    Object obj = itr.next();
                    Page page = (Page) obj;

                    try {
                        if (mUserCropRects[lastPageDone] != null) {
                            page.setBox(Page.e_user_crop, mUserCropRects[lastPageDone]);
                        } else {
                            removeUserCropFromPage(page);
                        }
                    } catch (PDFNetException ignored) {

                    }
                    lastPageDone++;
                }
                success = true;
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "USER_CROP");
            } finally {
                if (shouldUnlock) {
                    Utils.unlockQuietly(mDoc);
                }
            }

            return success;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            if (mAutoCropProgressDialog != null) {
                int pagesDone = values[0];
                mAutoCropProgressDialog.setProgress(pagesDone);
                if (pagesDone == mMaxPages) {
                    mAutoCropProgressDialog.setCancelable(false);
                }
            }
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);

            PDFViewCtrl pdfViewCtrl = mPdfViewCtrlRef.get();
            if (pdfViewCtrl == null) {
                return;
            }
            pdfViewCtrl.requestRendering();

            if (mAutoCropProgressDialog != null && mAutoCropProgressDialog.isShowing()) {
                mAutoCropProgressDialog.dismiss();
            }

            if (mAutoCropTaskListener != null) {
                mAutoCropTaskListener.onAutoCropTaskDone();
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            PDFViewCtrl pdfViewCtrl = mPdfViewCtrlRef.get();
            if (pdfViewCtrl == null) {
                return;
            }

            if (result) {
                try {
                    pdfViewCtrl.updatePageLayout();
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "USER_CROP");
                }
            } else {
                pdfViewCtrl.requestRendering();
            }
            if (mAutoCropProgressDialog != null && mAutoCropProgressDialog.isShowing()) {
                mAutoCropProgressDialog.dismiss();
            }

            if (mAutoCropTaskListener != null) {
                mAutoCropTaskListener.onAutoCropTaskDone();
            }
        }
    }

    // The name of the user crop box in a PDF Page
    private static final String CROPPING_OBJECT_STRING = "TRN_UserCrop";

    /**
     * Removes any user crop for a page if it is there
     *
     * @param page the page from which to remove the user crop
     * @throws PDFNetException PDFNet exception
     */
    public static void removeUserCropFromPage(Page page) throws PDFNetException {
        Obj pageRootObj = page.getSDFObj();
        if (pageRootObj.findObj(CROPPING_OBJECT_STRING) != null) {
            pageRootObj.erase(CROPPING_OBJECT_STRING);
        }
    }

    /**
     * Crops the PDF document permanently.
     *
     * @param doc The PDF document
     */
    public static void cropDoc(PDFDoc doc) {
        if (doc != null) {
            boolean shouldUnlock = false;
            try {
                doc.lock();
                shouldUnlock = true;
                PageIterator pageIterator = doc.getPageIterator();
                while (pageIterator.hasNext()) {
                    Page page = (Page) pageIterator.next();
                    page.setCropBox(page.getBox(Page.e_user_crop));
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    Utils.unlockQuietly(doc);
                }
            }
        }
    }
}
