//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import java.io.File;
import java.util.LinkedList;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot.BorderStyle;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.sdf.SDFDoc;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;

/**
 * Singleton class to manage stamp signatures.
 */
public class StampManager {

    private StampManager() {
    }

    private static class LazyHolder {
        private static final StampManager INSTANCE = new StampManager();
    }

    public static StampManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static String SIGNATURE_FILE_NAME = "SignatureFile.CompleteReader";
    private static int PAGE_BUFFER = 20;

    private PDFDoc mStampDoc = null;
    private String mDefaultSignatureFilename;

    private PDFDoc getStampDoc(File file) {
        if (mStampDoc == null) {
            try {
                if (file.exists()) {
                    mStampDoc = new PDFDoc(file.getAbsolutePath());
                } else {
                    mStampDoc = new PDFDoc();
                }
            } catch (PDFNetException e) {
            }
        }
        return mStampDoc;
    }

    private File getStampFile(Context context) {
        if (mDefaultSignatureFilename == null) {
            return new File(context.getFilesDir().getAbsolutePath() + "/" + SIGNATURE_FILE_NAME);
        }
        return new File(mDefaultSignatureFilename);
    }

    /**
     * Sets the default signature.
     *
     * @param pdfFilename The file name of PDF containing the signature image
     */
    @SuppressWarnings("unused")
    public void setDefaultSignatureFile(String pdfFilename) {
        mDefaultSignatureFilename = pdfFilename;
    }

    /**
     * @return The default signature file name
     */
    @SuppressWarnings("unused")
    public String getDefaultSignatureFile() {
        return mDefaultSignatureFilename;
    }

    public boolean hasDefaultSignature(Context context) {
        boolean hasDefaultSignature = false;

        File stampFile = getStampFile(context);
        if (stampFile.exists()) {
            PDFDoc doc = getStampDoc(stampFile);
            boolean shouldUnlockRead = false;
            try {
                doc.lockRead();
                shouldUnlockRead = true;
                if (doc.getPageCount() > 0) {
                    hasDefaultSignature = true;
                }
            } catch (PDFNetException e) {
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(doc);
                }
            }
        }
        return hasDefaultSignature;
    }

    public Page getDefaultSignature(Context context) {
        Page page = null;

        File stampFile = getStampFile(context);
        if (stampFile.exists()) {
            PDFDoc doc = getStampDoc(stampFile);
            boolean shouldUnlockRead = false;
            try {
                doc.lockRead();
                shouldUnlockRead = true;
                if (doc.getPageCount() > 0) {
                    page = doc.getPage(1);
                }
            } catch (PDFNetException e) {
            } finally {
                if (shouldUnlockRead) {
                    Utils.unlockReadQuietly(doc);
                }
            }
        }
        return page;
    }

    public void deleteDefaultSignature(Context context) {
        File stampFile = getStampFile(context);
        if (stampFile.exists()) {
            PDFDoc doc = getStampDoc(stampFile);
            boolean shouldUnlock = false;
            try {
                doc.lock();
                shouldUnlock = true;
                doc.pageRemove(doc.getPageIterator(1));
                doc.save(stampFile.getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null);
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlock) {
                    Utils.unlockQuietly(doc);
                }
            }
        }
    }

    public Page createSignature(Context context, RectF signatureBBox, LinkedList<LinkedList<PointF>> paths, int strokeColor, float strokeThickness, boolean makeDefault) {
        Page page = null;

        PDFDoc doc = createDocument(context, signatureBBox, paths, strokeColor, strokeThickness, makeDefault);
        try {
            page = doc.getPage(1);
        } catch (PDFNetException e) {
        }
        return page;
    }

    private PDFDoc createDocument(Context context, RectF signatureBBox, LinkedList<LinkedList<PointF>> paths, int strokeColor, float strokeThickness, boolean makeDefault) {
        PDFDoc doc = null;
        boolean shouldUnlock = false;

        try {
            if (makeDefault) {
                doc = getStampDoc(getStampFile(context));
                doc.lock();
                shouldUnlock = true;
                if (doc.getPageCount() > 0) {
                    doc.pageRemove(doc.getPageIterator(1));
                }
            } else {
                doc = new PDFDoc();
                doc.lock();
                shouldUnlock = true;
            }

            // Create a new page with a "buffer" on each side.
            Page page = doc.pageCreate(new Rect(0, 0, signatureBBox.right - signatureBBox.left + (PAGE_BUFFER*2), signatureBBox.top - signatureBBox.bottom + (PAGE_BUFFER*2)));
            doc.pagePushBack(page);

            // Create the annotation in the middle of the page.
            com.pdftron.pdf.annots.Ink ink = com.pdftron.pdf.annots.Ink.create(doc, new Rect(PAGE_BUFFER, PAGE_BUFFER, signatureBBox.right - signatureBBox.left + PAGE_BUFFER, signatureBBox.top - signatureBBox.bottom + PAGE_BUFFER));
            BorderStyle bs = ink.getBorderStyle();
            bs.setWidth(strokeThickness);
            ink.setBorderStyle(bs);

            // Shove the points to the ink annotation
            int i = 0;
            Point pdfPoint = new Point();
            for (LinkedList<PointF> pointList : paths) {
                int j = 0;
                for (PointF p : pointList) {
                    pdfPoint.x = p.x - signatureBBox.left + PAGE_BUFFER;
                    pdfPoint.y = signatureBBox.top - p.y + PAGE_BUFFER;
                    ink.setPoint(i, j++,  pdfPoint);
                }
                i++;
            }

            double r = (double) Color.red(strokeColor) / 255;
            double g = (double) Color.green(strokeColor) / 255;
            double b = (double) Color.blue(strokeColor) / 255;
            ink.setColor(new ColorPt(r, g, b), 3);
            ink.refreshAppearance();

            // Make the page crop box the same as the annotation bounding box, so that there's no gaps.
            Rect newBoundRect = ink.getRect();
            page.setCropBox(newBoundRect);

            ink.refreshAppearance();

            page.annotPushBack(ink);

            if (makeDefault) {
                doc.save(getStampFile(context).getAbsolutePath(), SDFDoc.SaveMode.REMOVE_UNUSED, null);
            }

        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(doc);
            }
        }

        return doc;
    }
}
