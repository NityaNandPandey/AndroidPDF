package com.pdftron.demo.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.pdftron.pdf.Optimizer;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import com.pdftron.demo.model.OptimizeParams;

/**
 * A dialog with various options to optimize a PDF file.
 */
public class OptimizeDialogFragment extends DialogFragment {

    public interface OptimizeDialogFragmentListener {
        void onOptimizeClicked(OptimizeParams result);
    }

    final static int COMPRESSION_COLOR_MODE_RETAIN = 0;
    final static int COMPRESSION_COLOR_MODE_PNG = 1;
    final static int COMPRESSION_COLOR_MODE_JPEG = 2;
    final static int COMPRESSION_COLOR_MODE_JPEG2000 = 3;

    final static int COMPRESSION_MONO_MODE_PNG = 0;
    final static int COMPRESSION_MONO_MODE_JPIG2 = 1;

    final static int COMPRESSION_QUALITY_LOW = 0;
    final static int COMPRESSION_QUALITY_MEDIUM = 1;
    final static int COMPRESSION_QUALITY_HIGH = 2;
    final static int COMPRESSION_QUALITY_MAX = 3;

    final static int DOWNSAMPLING_MAX_50 = 0;
    final static int DOWNSAMPLING_MAX_72 = 1;
    final static int DOWNSAMPLING_MAX_96 = 2;
    final static int DOWNSAMPLING_MAX_120 = 3;
    final static int DOWNSAMPLING_MAX_150 = 4;
    final static int DOWNSAMPLING_MAX_225 = 5;
    final static int DOWNSAMPLING_MAX_300 = 6;
    final static int DOWNSAMPLING_MAX_600 = 7;

    final static int DOWNSAMPLING_RESAMPLE_50 = 0;
    final static int DOWNSAMPLING_RESAMPLE_72 = 1;
    final static int DOWNSAMPLING_RESAMPLE_96 = 2;
    final static int DOWNSAMPLING_RESAMPLE_150 = 3;
    final static int DOWNSAMPLING_RESAMPLE_225 = 4;

    private int mCurrentView = 0;

    private OptimizeDialogFragmentListener mListener;

    public void setListener(OptimizeDialogFragmentListener listener) {
        mListener = listener;
    }

    public OptimizeDialogFragment() {}

    public static OptimizeDialogFragment newInstance() {
        return new OptimizeDialogFragment();
    }

    Spinner mMaxDpiSpinner;
    Spinner mResampleDpiSpinner;
    Spinner mCompressionColorModeSpinner;
    Spinner mCompressionMonoModeSpinner;
    Spinner mCompressionQualitySpinner;

    ViewGroup mPresetsLayout;
    ViewGroup mCustomLayout;
    ViewGroup mQualityLayout;

    TextView mToggleButton;

    RadioGroup mBasicOptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.controls_fragment_optimize_dialog, null);
        builder.setView(view);

        builder.setPositiveButton(getActivity().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mListener != null) {
                    OptimizeParams result = processOptimizeRequest();
                    mListener.onOptimizeClicked(result);
                }
                dismiss();
            }
        });
        builder.setNegativeButton(getActivity().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        mBasicOptions = (RadioGroup) view.findViewById(R.id.radio_basic_group);
        mBasicOptions.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateAdvancedToMatchPresets();
            }
        });

        mMaxDpiSpinner = (Spinner) view.findViewById(R.id.max_dpi_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.optimize_downsampling_max_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mMaxDpiSpinner.setAdapter(adapter);
        mMaxDpiSpinner.setSelection(DOWNSAMPLING_MAX_225);

        mResampleDpiSpinner = (Spinner) view.findViewById(R.id.resample_dpi_spinner);
        adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.optimize_downsampling_resample_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mResampleDpiSpinner.setAdapter(adapter);
        mResampleDpiSpinner.setSelection(DOWNSAMPLING_RESAMPLE_150);

        mCompressionColorModeSpinner = (Spinner) view.findViewById(R.id.compression_color_mode_spinner);
        adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.optimize_compression_color_mode, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCompressionColorModeSpinner.setAdapter(adapter);
        mCompressionColorModeSpinner.setSelection(COMPRESSION_COLOR_MODE_JPEG);
        mCompressionColorModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == COMPRESSION_COLOR_MODE_JPEG ||
                        position == COMPRESSION_COLOR_MODE_JPEG2000) {
                    mQualityLayout.setVisibility(View.VISIBLE);
                } else {
                    mQualityLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mCompressionMonoModeSpinner = (Spinner) view.findViewById(R.id.compression_mono_mode_spinner);
        adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.optimize_compression_mono_mode, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCompressionMonoModeSpinner.setAdapter(adapter);
        mCompressionMonoModeSpinner.setSelection(COMPRESSION_MONO_MODE_JPIG2);

        mCompressionQualitySpinner = (Spinner) view.findViewById(R.id.compression_quality_spinner);
        adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.optimize_quality_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCompressionQualitySpinner.setAdapter(adapter);
        mCompressionQualitySpinner.setSelection(COMPRESSION_QUALITY_HIGH);

        mPresetsLayout = (ViewGroup) view.findViewById(R.id.basic_layout);
        mCustomLayout = (ViewGroup) view.findViewById(R.id.advanced_layout);

        mToggleButton = (TextView) view.findViewById(R.id.optimize_advanced);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentView == 0) {
                    mCurrentView = 1;
                    SpannableString content = new SpannableString(getString(R.string.optimize_basic));
                    content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                    mToggleButton.setText(content);
                    mPresetsLayout.setVisibility(View.GONE);
                    mCustomLayout.setVisibility(View.VISIBLE);
                } else {
                    mCurrentView = 0;
                    updateAdvancedToMatchPresets();
                    SpannableString content = new SpannableString(getString(R.string.optimize_advanced));
                    content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                    mToggleButton.setText(content);
                    mPresetsLayout.setVisibility(View.VISIBLE);
                    mCustomLayout.setVisibility(View.GONE);
                }
            }
        });

        mQualityLayout = (ViewGroup) view.findViewById(R.id.quality_layout);
        if (mCompressionColorModeSpinner.getSelectedItemPosition() == COMPRESSION_COLOR_MODE_JPEG ||
                mCompressionColorModeSpinner.getSelectedItemPosition() == COMPRESSION_COLOR_MODE_JPEG2000) {
            mQualityLayout.setVisibility(View.VISIBLE);
        } else {
            mQualityLayout.setVisibility(View.GONE);
        }

        return builder.create();
    }

    private void updateAdvancedToMatchPresets() {
        int id = mBasicOptions.getCheckedRadioButtonId();
        if (id == R.id.radio_first) {
            mMaxDpiSpinner.setSelection(DOWNSAMPLING_MAX_600);
            mResampleDpiSpinner.setSelection(DOWNSAMPLING_RESAMPLE_225);
            mCompressionQualitySpinner.setSelection(COMPRESSION_QUALITY_MAX);
        } else if (id == R.id.radio_second) {
            mMaxDpiSpinner.setSelection(DOWNSAMPLING_MAX_225);
            mResampleDpiSpinner.setSelection(DOWNSAMPLING_RESAMPLE_150);
            mCompressionQualitySpinner.setSelection(COMPRESSION_QUALITY_HIGH);
        } else {
            mMaxDpiSpinner.setSelection(DOWNSAMPLING_MAX_120);
            mResampleDpiSpinner.setSelection(DOWNSAMPLING_RESAMPLE_96);
            mCompressionQualitySpinner.setSelection(COMPRESSION_QUALITY_MEDIUM);
        }
    }

    private OptimizeParams processOptimizeRequest() {
        OptimizeParams result = new OptimizeParams();
        result.forceChanges = false;
        if (mCurrentView == 0) {
            int id = mBasicOptions.getCheckedRadioButtonId();
            if (id == R.id.radio_first) {
                result.colorDownsampleMode = Optimizer.ImageSettings.e_off;
                result.colorCompressionMode = Optimizer.ImageSettings.e_retain;
                result.colorQuality = 10;

                result.monoDownsampleMode = Optimizer.MonoImageSettings.e_off;
                result.monoCompressionMode = Optimizer.MonoImageSettings.e_jbig2;

                result.forceRecompression = false;
            } else if (id == R.id.radio_second) {
                result.colorDownsampleMode = Optimizer.ImageSettings.e_default;
                result.colorMaxDpi = 225.0;
                result.colorResampleDpi = 150.0;
                result.colorCompressionMode = Optimizer.ImageSettings.e_jpeg;
                result.colorQuality = 8;

                result.monoDownsampleMode = Optimizer.MonoImageSettings.e_default;
                result.monoMaxDpi = result.colorMaxDpi * 2.0;
                result.monoResampleDpi = result.colorResampleDpi * 2.0;
                result.monoCompressionMode = Optimizer.MonoImageSettings.e_jbig2;

                result.forceRecompression = true;
            } else {
                result.colorDownsampleMode = Optimizer.ImageSettings.e_default;
                result.colorMaxDpi = 120.0;
                result.colorResampleDpi = 96.0;
                result.colorCompressionMode = Optimizer.ImageSettings.e_jpeg;
                result.colorQuality = 6;

                result.monoDownsampleMode = Optimizer.MonoImageSettings.e_default;
                result.monoMaxDpi = result.colorMaxDpi * 2.0;
                result.monoResampleDpi = result.colorResampleDpi * 2.0;
                result.monoCompressionMode = Optimizer.MonoImageSettings.e_jbig2;

                result.forceRecompression = true;
            }
        } else {
            result.forceRecompression = true;
            result.colorDownsampleMode = Optimizer.ImageSettings.e_default;
            result.monoDownsampleMode = Optimizer.MonoImageSettings.e_default;

            // MaxDpi
            switch (mMaxDpiSpinner.getSelectedItemPosition()) {
                case DOWNSAMPLING_MAX_50:
                    result.colorMaxDpi = 50.0;
                    break;
                case DOWNSAMPLING_MAX_72:
                    result.colorMaxDpi = 72.0;
                    break;
                case DOWNSAMPLING_MAX_96:
                    result.colorMaxDpi = 96.0;
                    break;
                case DOWNSAMPLING_MAX_120:
                    result.colorMaxDpi = 120.0;
                    break;
                case DOWNSAMPLING_MAX_150:
                    result.colorMaxDpi = 150.0;
                    break;
                case DOWNSAMPLING_MAX_300:
                    result.colorMaxDpi = 300.0;
                    break;
                case DOWNSAMPLING_MAX_600:
                    result.colorMaxDpi = 600.0;
                    break;
                case DOWNSAMPLING_MAX_225:
                default:
                    result.colorMaxDpi = 225.0;
                    break;
            }

            // Resample DPI
            switch (mResampleDpiSpinner.getSelectedItemPosition()) {
                case DOWNSAMPLING_RESAMPLE_50:
                    result.colorResampleDpi = 50.0;
                    break;
                case DOWNSAMPLING_RESAMPLE_72:
                    result.colorResampleDpi = 72.0;
                    break;
                case DOWNSAMPLING_RESAMPLE_96:
                    result.colorResampleDpi = 96.0;
                    break;
                case DOWNSAMPLING_RESAMPLE_225:
                    result.colorResampleDpi = 225.0;
                    break;
                case DOWNSAMPLING_RESAMPLE_150:
                default:
                    result.colorResampleDpi = 150.0;
                    break;
            }

            // Color Mode
            switch (mCompressionColorModeSpinner.getSelectedItemPosition()) {
                case COMPRESSION_COLOR_MODE_RETAIN:
                    result.colorCompressionMode = Optimizer.ImageSettings.e_retain;
                    break;
                case COMPRESSION_COLOR_MODE_PNG:
                    result.colorCompressionMode = Optimizer.ImageSettings.e_flate;
                    break;
                case COMPRESSION_COLOR_MODE_JPEG2000:
                    result.colorCompressionMode = Optimizer.ImageSettings.e_jpeg2000;
                    break;
                case COMPRESSION_COLOR_MODE_JPEG:
                default:
                    result.colorCompressionMode = Optimizer.ImageSettings.e_jpeg;
                    break;
            }

            // Color Quality
            switch (mCompressionQualitySpinner.getSelectedItemPosition()) {
                case COMPRESSION_QUALITY_MAX:
                    result.colorQuality = 10;
                    break;
                case COMPRESSION_QUALITY_HIGH:
                    result.colorQuality = 8;
                    break;
                case COMPRESSION_QUALITY_LOW:
                    result.colorQuality = 4;
                    break;
                case COMPRESSION_QUALITY_MEDIUM:
                default:
                    result.colorQuality = 6;
                    break;
            }

            result.monoMaxDpi = result.colorMaxDpi * 2.0;
            result.monoResampleDpi = result.colorResampleDpi * 2.0;

            // Mono Mode
            switch (mCompressionMonoModeSpinner.getSelectedItemPosition()) {
                case COMPRESSION_MONO_MODE_PNG:
                    result.monoCompressionMode = Optimizer.MonoImageSettings.e_flate;
                    break;
                case COMPRESSION_MONO_MODE_JPIG2:
                default:
                    result.monoCompressionMode = Optimizer.MonoImageSettings.e_jbig2;
                    break;
            }
        }
        return result;
    }

    /**
     * Optimize a {@link PDFDoc}
     * @param doc the PDFDoc to be optimized
     * @param optimizeParams the {@link OptimizeParams}
     */
    public static void optimize(PDFDoc doc, OptimizeParams optimizeParams) {
        if (doc != null) {
            boolean shouldUnlock = false;
            try {
                Optimizer.ImageSettings ims = new Optimizer.ImageSettings();
                ims.setDownsampleMode(optimizeParams.colorDownsampleMode);
                ims.setImageDPI(optimizeParams.colorMaxDpi, optimizeParams.colorResampleDpi);
                ims.setCompressionMode(optimizeParams.colorCompressionMode);
                ims.setQuality(optimizeParams.colorQuality);
                ims.forceChanges(optimizeParams.forceChanges);
                ims.forceRecompression(optimizeParams.forceRecompression);

                Optimizer.MonoImageSettings mims = new Optimizer.MonoImageSettings();
                mims.setDownsampleMode(optimizeParams.monoDownsampleMode);
                mims.setImageDPI(optimizeParams.monoMaxDpi, optimizeParams.monoResampleDpi);
                mims.setCompressionMode(optimizeParams.monoCompressionMode);
                mims.forceChanges(optimizeParams.forceChanges);
                mims.forceRecompression(optimizeParams.forceRecompression);

                Optimizer.OptimizerSettings os = new Optimizer.OptimizerSettings();
                os.setColorImageSettings(ims);
                os.setGrayscaleImageSettings(ims);
                os.setMonoImageSettings(mims);

                doc.lock();
                shouldUnlock = true;
                Optimizer.optimize(doc, os);
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
