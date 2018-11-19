package com.pdftron.pdf.tools;

import android.graphics.PointF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.filters.MappedFile;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Sound;
import com.pdftron.pdf.dialog.SoundDialogFragment;
import com.pdftron.pdf.interfaces.OnSoundRecordedListener;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import java.io.File;
import java.util.ArrayList;

@Keep
public class SoundCreate extends SimpleTapShapeCreate {

    public static final String SOUND_ICON = "sound";

    public static final int SAMPLE_RATE = 22050;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int NUM_CHANNELS = 1;

    private String mOutputFilePath;

    /**
     * Class constructor
     */
    public SoundCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.SOUND_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Sound;
    }


    @Override
    public void addAnnotation() {
        if (mPt2 == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(
                new Exception("target point is not specified."));
            return;
        }

        if (mPdfViewCtrl == null) {
            return;
        }

        setNextToolModeHelper();
        setCurrentDefaultToolModeHelper(getToolMode());

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        FragmentActivity activity = toolManager.getCurrentActivity();
        if (activity == null) {
            return;
        }

        SoundDialogFragment fragment = SoundDialogFragment.newInstance(mPt2, mDownPageNum);
        fragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        fragment.show(activity.getSupportFragmentManager(), SoundDialogFragment.TAG);
        fragment.setOnSoundRecordedListener(new OnSoundRecordedListener() {
            @Override
            public void onSoundRecorded(PointF targetPoint, int pageNum, String outputFile) {
                createSound(targetPoint, pageNum, outputFile);
            }
        });
    }

    private void createSound(PointF targetPoint, int pageNum, String outputPath) {
        mOutputFilePath = outputPath;

        createAnnotation(targetPoint, pageNum);

        if (outputPath != null) {
            File file = new File(outputPath);
            if (file.exists() && file.isFile()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        if (Utils.isNullOrEmpty(mOutputFilePath)) {
            return null;
        }
        MappedFile mappedFile = new MappedFile(mOutputFilePath);
        Sound sound = Sound.createWithData(mPdfViewCtrl.getDoc(),
            bbox, mappedFile, BITS_PER_SAMPLE, SAMPLE_RATE, NUM_CHANNELS);
        sound.setIcon(Sound.e_Speaker);
        sound.getSoundStream().putName("E", "Signed");

        sound.refreshAppearance();

        return sound;
    }
}
