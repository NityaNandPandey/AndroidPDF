package com.pdftron.pdf.dialog;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chibde.visualizer.LineBarVisualizer;
import com.pdftron.pdf.controls.CustomSizeDialogFragment;
import com.pdftron.pdf.interfaces.OnSoundRecordedListener;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.SoundCreate;
import com.pdftron.pdf.utils.Utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static com.pdftron.pdf.utils.RequestCode.RECORD_AUDIO;

public class SoundDialogFragment extends CustomSizeDialogFragment {

    public final static String TAG = SoundDialogFragment.class.getName();

    private final static int DIALOG_MODE_SOUND_CREATE = 0;
    private final static int DIALOG_MODE_SOUND_PLAY = 1;

    private final static String BUNDLE_DIALOG_MODE = "dialog_mode";
    private final static String BUNDLE_TARGET_POINT_X = "target_point_x";
    private final static String BUNDLE_TARGET_POINT_Y = "target_point_y";
    private final static String BUNDLE_TARGET_PAGE_NUM = "target_page_num";
    private final static String BUNDLE_AUDIO_FILE_PATH = "audio_file_path";
    private final static String BUNDLE_SAMPLE_RATE = "sample_rate";
    private final static String BUNDLE_ENCODING_BIT_RATE = "encoding_bit_rate";
    private final static String BUNDLE_NUM_CHANNEL_OUT = "num_channel_out";

    private int mSampleRate;
    private int mEncodingBitRate;
    private int mNumChannelIn;
    private int mNumChannelOut;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private PointF mTargetPoint;
    private int mPageNum = -1;
    private OnSoundRecordedListener mOnSoundRecordedListener;

    private AudioRecord mRecorder;
    private AudioTrack mPlayer;
    private int mBufferSize;

    private boolean mStartRecording = true;
    private boolean mStartPlaying = true;

    private boolean mCleaningRecorder;
    private boolean mCleaningPlayer;

    private Thread mRecordingThread;
    private Thread mPlayingThread;

    private boolean mRecorded;

    private boolean mShouldContinue; // Indicates if recording / playing should stop

    private boolean mRecordingDisabled;
    private boolean mPlayingDisabled;

    private int mDialogMode = DIALOG_MODE_SOUND_CREATE;

    private Handler mPlayerHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            handlePlay();
        }
    };

    private Handler mRecordVisualizerHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg != null && msg.obj != null) {
                byte[] data = (byte[]) msg.obj;
                if (mVisualizer != null) {
                    mVisualizer.setRecorder(data);
                }
            }
        }
    };

    private long mStartTime;
    private Handler mHandler = new Handler();
    private final int SEC_UPDATE = 100;
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            if (mLengthLabel == null) {
                return;
            }
            final long start = mStartTime;
            long millis = System.currentTimeMillis() - start;
            updateLengthLabel(millis);

            mHandler.postDelayed(this, SEC_UPDATE);
        }
    };

    private String mFilePath = null;

    private TextView mLeftLabel;
    private ImageView mLeftIcon;
    private TextView mCenterLabel;
    private ImageView mCenterIcon;
    private TextView mRightLabel;
    private ImageView mRightIcon;

    private LineBarVisualizer mVisualizer;
    private TextView mLengthLabel;

    public static SoundDialogFragment newInstance(@NonNull PointF targetPoint, int pageNum) {
        SoundDialogFragment fragment = new SoundDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putFloat(BUNDLE_TARGET_POINT_X, targetPoint.x);
        bundle.putFloat(BUNDLE_TARGET_POINT_Y, targetPoint.y);
        bundle.putInt(BUNDLE_TARGET_PAGE_NUM, pageNum);
        bundle.putInt(BUNDLE_DIALOG_MODE, DIALOG_MODE_SOUND_CREATE);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static SoundDialogFragment newInstance(String filePath, int sampleRate, int encodingBitRate, int channel) {
        SoundDialogFragment fragment = new SoundDialogFragment();
        Bundle bundle = new Bundle();

        bundle.putInt(BUNDLE_DIALOG_MODE, DIALOG_MODE_SOUND_PLAY);
        bundle.putString(BUNDLE_AUDIO_FILE_PATH, filePath);
        bundle.putInt(BUNDLE_SAMPLE_RATE, sampleRate);
        bundle.putInt(BUNDLE_ENCODING_BIT_RATE, encodingBitRate);
        bundle.putInt(BUNDLE_NUM_CHANNEL_OUT, channel);
        fragment.setArguments(bundle);
        return fragment;
    }

    public SoundDialogFragment() {

    }

    /**
     * Sets the listener
     *
     * @param listener the listener
     */
    public void setOnSoundRecordedListener(OnSoundRecordedListener listener) {
        this.mOnSoundRecordedListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHeight = 800;

        Bundle args = getArguments();
        if (args != null) {
            mDialogMode = args.getInt(BUNDLE_DIALOG_MODE, DIALOG_MODE_SOUND_CREATE);
            if (mDialogMode == DIALOG_MODE_SOUND_CREATE) {
                float x = args.getFloat(BUNDLE_TARGET_POINT_X);
                float y = args.getFloat(BUNDLE_TARGET_POINT_Y);
                mTargetPoint = new PointF(x, y);
                mPageNum = args.getInt(BUNDLE_TARGET_PAGE_NUM, -1);
            } else {
                mFilePath = args.getString(BUNDLE_AUDIO_FILE_PATH, null);
                mSampleRate = args.getInt(BUNDLE_SAMPLE_RATE);
                mEncodingBitRate = args.getInt(BUNDLE_ENCODING_BIT_RATE, AudioFormat.ENCODING_PCM_8BIT);
                mNumChannelOut = args.getInt(BUNDLE_NUM_CHANNEL_OUT, AudioFormat.CHANNEL_OUT_MONO);
            }
        }

        Activity activity = getActivity();
        if (activity != null) {
            if (mDialogMode == DIALOG_MODE_SOUND_CREATE) {
                mFilePath = activity.getFilesDir().getAbsolutePath();
                mFilePath += "/audiorecord.out";
            }

            ActivityCompat.requestPermissions(activity, permissions, RECORD_AUDIO);
        }

        if (mDialogMode == DIALOG_MODE_SOUND_CREATE) {
            mRecordingDisabled = false;
            mPlayingDisabled = true;

            mSampleRate = SoundCreate.SAMPLE_RATE;
            mEncodingBitRate = AudioFormat.ENCODING_PCM_16BIT;
            mNumChannelIn = AudioFormat.CHANNEL_IN_MONO;
            mNumChannelOut = AudioFormat.CHANNEL_OUT_MONO;
        } else {
            mRecordingDisabled = true;
            mPlayingDisabled = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case RECORD_AUDIO:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) {
            dismiss();
        } else {
            if (mDialogMode == DIALOG_MODE_SOUND_PLAY) {
                // start playing right away
                handlePlay();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sound_create_dialog, container);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        if (mDialogMode == DIALOG_MODE_SOUND_CREATE) {
            toolbar.setTitle(R.string.tools_qm_sound);
        } else {
            toolbar.setTitle(R.string.tools_qm_play_sound);
        }

        mLengthLabel = view.findViewById(R.id.record_length);
        updateLengthLabel(0);

        mVisualizer = view.findViewById(R.id.visualizer);
        mVisualizer.setColor(Utils.getPrimaryColor(view.getContext()));
        mVisualizer.setDensity(90f);

        mLeftIcon = view.findViewById(R.id.record_preview);
        mLeftLabel = view.findViewById(R.id.record_preview_label);
        mCenterIcon = view.findViewById(R.id.record_icon);
        mCenterLabel = view.findViewById(R.id.record_icon_label);
        mRightIcon = view.findViewById(R.id.record_done);
        mRightLabel = view.findViewById(R.id.record_done_label);

        if (mDialogMode == DIALOG_MODE_SOUND_PLAY) {
            mCenterIcon.setImageDrawable(AppCompatResources.getDrawable(view.getContext(), R.drawable.ic_play_arrow_black_24dp));
            mCenterLabel.setText(R.string.sound_label_preview);
        }

        mLeftIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePlay();
            }
        });

        mLeftLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePlay();
            }
        });

        mCenterIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialogMode == DIALOG_MODE_SOUND_CREATE) {
                    handleRecord();
                } else {
                    handlePlay();
                }
            }
        });

        mCenterLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDialogMode == DIALOG_MODE_SOUND_CREATE) {
                    handleRecord();
                } else {
                    handlePlay();
                }
            }
        });

        mRightIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDone();
            }
        });

        mRightLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDone();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        updateLayoutVisibility();
    }

    @Override
    public void onStop() {
        super.onStop();
        mShouldContinue = false;
        if (mRecordingThread != null) {
            mRecordingThread.interrupt();
            mRecordingThread = null;
        }
        if (mPlayingThread != null) {
            mPlayingThread.interrupt();
            mPlayingThread = null;
        }
        if (mRecorder != null && !mCleaningRecorder) {
            mCleaningRecorder = true;
            mRecorder.release();
            mRecorder = null;
            mCleaningRecorder = false;
        }
        if (mPlayer != null && !mCleaningPlayer) {
            mCleaningPlayer = true;
            mPlayer.release();
            mPlayer = null;
            mCleaningPlayer = false;
        }
        mHandler.removeCallbacksAndMessages(null);
        mPlayerHandler.removeCallbacksAndMessages(null);
    }

    private void updateLengthLabel(long millis) {
        String text = DurationFormatUtils.formatDuration(millis, "mm:ss.SSS");
        mLengthLabel.setText(text);
    }

    private void updateLayoutVisibility() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (mLeftIcon == null || mLeftLabel == null ||
            mCenterIcon == null || mCenterLabel == null ||
            mRightIcon == null || mRightLabel == null) {
            return;
        }
        if (mDialogMode == DIALOG_MODE_SOUND_CREATE) {
            if (!mRecorded) {
                // never recorded before, hide the other buttons
                mLeftIcon.setVisibility(View.INVISIBLE);
                mLeftLabel.setVisibility(View.INVISIBLE);
                mRightIcon.setVisibility(View.INVISIBLE);
                mRightLabel.setVisibility(View.INVISIBLE);
            } else {
                mLeftIcon.setVisibility(View.VISIBLE);
                mLeftLabel.setVisibility(View.VISIBLE);
                mRightIcon.setVisibility(View.VISIBLE);
                mRightLabel.setVisibility(View.VISIBLE);

                setViewEnabled(context, mLeftIcon, mLeftLabel, true);
                setViewEnabled(context, mRightIcon, mRightLabel, true);
                setViewEnabled(context, mCenterIcon, mCenterLabel, true);

                if (mPlayingDisabled) {
                    setViewEnabled(context, mLeftIcon, mLeftLabel, false);
                    setViewEnabled(context, mRightIcon, mRightLabel, false);
                }
                if (mRecordingDisabled) {
                    setViewEnabled(context, mCenterIcon, mCenterLabel, false);
                }
            }
        } else {
            mLeftIcon.setVisibility(View.INVISIBLE);
            mLeftLabel.setVisibility(View.INVISIBLE);
            mRightIcon.setVisibility(View.INVISIBLE);
            mRightLabel.setVisibility(View.INVISIBLE);
        }
    }

    private void setViewEnabled(@NonNull Context context, @NonNull ImageView view, @NonNull TextView label, boolean enabled) {
        int color = Utils.getPrimaryColor(context);
        if (!enabled) {
            color = context.getResources().getColor(R.color.gray400);
        }
        view.getDrawable().mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        label.setTextColor(color);
    }

    private void handleDone() {
        if (mFilePath != null && mOnSoundRecordedListener != null) {
            mOnSoundRecordedListener.onSoundRecorded(mTargetPoint, mPageNum, mFilePath);
        }
        dismiss();
    }

    private void handlePlay() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (mLeftIcon == null || mLeftLabel == null) {
            return;
        }
        if (mPlayingDisabled) {
            // ignore if currently recording
            return;
        }
        onPlay(mStartPlaying);
        if (mStartPlaying) {
            if (mDialogMode == DIALOG_MODE_SOUND_CREATE) {
                mLeftIcon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_stop_black_24dp));
                mLeftLabel.setText(R.string.sound_label_stop);
            } else {
                mCenterIcon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_stop_black_24dp));
                mCenterLabel.setText(R.string.sound_label_stop);
            }
        } else {
            if (mDialogMode == DIALOG_MODE_SOUND_CREATE) {
                mLeftIcon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_play_arrow_black_24dp));
                mLeftLabel.setText(R.string.sound_label_preview);
            } else {
                mCenterIcon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_play_arrow_black_24dp));
                mCenterLabel.setText(R.string.sound_label_play);
            }
        }
        updateLayoutVisibility();
        mStartPlaying = !mStartPlaying;
    }

    private void handleRecord() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (mCenterIcon == null || mCenterLabel == null) {
            return;
        }
        if (mRecordingDisabled) {
            // ignore if currently playing
            return;
        }
        onRecord(mStartRecording);
        if (mStartRecording) {
            mCenterIcon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_mic_off_black_24dp));
            mCenterLabel.setText(R.string.sound_label_stop);
        } else {
            mCenterIcon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_mic_black_24dp));
            mCenterLabel.setText(R.string.sound_label_record);
        }
        updateLayoutVisibility();
        mStartRecording = !mStartRecording;
    }

    private void startRecording() {
        mPlayingDisabled = true;
        mStartTime = System.currentTimeMillis();

        mShouldContinue = true;

        // buffer size in bytes
        mBufferSize = AudioRecord.getMinBufferSize(mSampleRate,
            mNumChannelIn,
            mEncodingBitRate);

        mRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
            mSampleRate,
            mNumChannelIn,
            mEncodingBitRate,
            mBufferSize);

        recordAudio();
    }

    private void stopRecording() {
        mShouldContinue = false;

        mPlayingDisabled = false;
        mRecorded = true;

        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startPlaying() {
        mRecordingDisabled = true;
        mStartTime = System.currentTimeMillis();

        mShouldContinue = true;

        mBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mNumChannelOut, mEncodingBitRate);
        if (mBufferSize == AudioTrack.ERROR_BAD_VALUE || mBufferSize == AudioTrack.ERROR) {
            // something went run
            return;
        }

        mPlayer = new AudioTrack(
            AudioManager.STREAM_MUSIC,
            mSampleRate,
            mNumChannelOut,
            mEncodingBitRate,
            mBufferSize,
            AudioTrack.MODE_STREAM);

        if (mVisualizer != null) {
            mVisualizer.setPlayer(mPlayer.getAudioSessionId());
        }

        playAudio();
    }

    private void stopPlaying() {
        mShouldContinue = false;

        mRecordingDisabled = false;

        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void recordAudio() {
        mRecordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(mFilePath);
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                }

                byte[] audioBuffer = new byte[mBufferSize];

                if (mRecorder == null || mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "Audio Record can't initialize!");
                    return;
                }
                mRecorder.startRecording();

                mHandler.removeCallbacks(mUpdateTimeTask);
                mHandler.postDelayed(mUpdateTimeTask, SEC_UPDATE);

                Log.v(TAG, "Start recording");

                long bytesRead = 0;
                if (fos != null) {
                    while (mShouldContinue && !Thread.interrupted()) {
                        int numberOfByte = mRecorder.read(audioBuffer, 0, audioBuffer.length);
                        bytesRead += numberOfByte;

                        // Do something with the audioBuffer
                        if (AudioRecord.ERROR_INVALID_OPERATION != numberOfByte) {
                            try {
                                byte[] audioBufferReversed = swapByteArray(audioBuffer);

                                byte[] eightBit = toEightBitArray(audioBufferReversed);
                                if (eightBit != null) {
                                    Message m = new Message();
                                    m.obj = Arrays.copyOf(eightBit, eightBit.length);
                                    mRecordVisualizerHandler.sendMessage(m);
                                }

                                if (audioBufferReversed != null) {
                                    fos.write(audioBufferReversed);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (mRecorder != null && !mCleaningRecorder) {
                    mCleaningRecorder = true;
                    mRecorder.stop();
                    mRecorder.release();
                    mRecorder = null;
                    mCleaningRecorder = false;
                }

                Log.v(TAG, String.format("Recording stopped. Samples read: %d", bytesRead));
            }
        });
        mRecordingThread.start();
    }

    private void playAudio() {
        mPlayingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                FileInputStream fis = null;
                byte[] byteData = null;
                try {
                    fis = new FileInputStream(mFilePath);
                    byteData = IOUtils.toByteArray(fis);
                    byteData = swapByteArray(byteData);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    Utils.closeQuietly(fis);
                }
                if (byteData == null) {
                    return;
                }
                if (mPlayer == null || mPlayer.getState() != AudioTrack.STATE_INITIALIZED) {
                    Log.e(TAG, "Audio Track can't initialize!");
                    return;
                }

                mPlayer.play();

                mHandler.removeCallbacks(mUpdateTimeTask);
                mHandler.postDelayed(mUpdateTimeTask, SEC_UPDATE);

                Log.v(TAG, "Audio streaming started");

                int bytesRead = 0;
                int size = byteData.length;
                int bytesPerRead;

                while (bytesRead < size && mShouldContinue && !Thread.interrupted()) {
                    bytesPerRead = Math.min(mBufferSize, (size - bytesRead));
                    bytesRead += mPlayer.write(byteData, bytesRead, bytesPerRead);
                }

                if (mPlayer != null && !mCleaningPlayer) {
                    mCleaningPlayer = true;
                    mPlayer.stop();
                    mPlayer.release();
                    mPlayer = null;
                    mCleaningPlayer = false;
                }

                if (mShouldContinue) {
                    // user did not click stop
                    // let's finish player UI
                    mPlayerHandler.sendEmptyMessage(0);
                }

                Log.v(TAG, "Audio streaming finished. Samples written: " + byteData.length);
            }
        });
        mPlayingThread.start();
    }

    private static byte[] swapByteArray(byte[] a) {
        if (a == null) {
            return null;
        }
        byte[] ret = new byte[a.length];
        // if array is odd we set limit to a.length - 1.
        int limit = a.length - (a.length % 2);
        if (limit < 1) return null;
        for (int i = 0; i < limit - 1; i = i + 2) {
            ret[i] = a[i + 1];
            ret[i + 1] = a[i];
        }
        return ret;
    }

    private static byte[] toEightBitArray(byte[] a) {
        if (a == null) {
            return null;
        }
        byte[] ret = new byte[a.length / 2];
        int j = 0;
        for (int i = 0; i < a.length; i++) {
            if ((i & 1) == 0) {
                // even
                ret[j++] = a[i];
            }
        }
        return ret;
    }

}
