package com.pdftron.pdf.interfaces;

import android.graphics.PointF;

/**
 * Callback interface to be invoked when sound recording is confirmed.
 */
public interface OnSoundRecordedListener {
    void onSoundRecorded(PointF targetPoint, int pageNum, String outputFile);
}
