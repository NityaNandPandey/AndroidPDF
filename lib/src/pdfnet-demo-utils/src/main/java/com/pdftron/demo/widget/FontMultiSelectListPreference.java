package com.pdftron.demo.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.MultiSelectListPreference;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;

import com.pdftron.demo.R;
import com.pdftron.pdf.PDFNet;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

public class FontMultiSelectListPreference extends MultiSelectListPreference {

    private final static String TAG = FontMultiSelectListPreference.class.getName();
    private static boolean sDebug;
    private PopulateFontInfoTask mPopulateFontInfoTask;
    private boolean isDialogShowing;

    @SuppressWarnings("unused")
    public FontMultiSelectListPreference(Context context) {
        super(context);
        init();
    }

    @SuppressWarnings("unused")
    public FontMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FontMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FontMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        // initialize entries until the data will be ready in background process
        setEntries(new CharSequence[0]);
        setEntryValues(new CharSequence[0]);
        final CharSequence dialogTitle = getDialogTitle();
        setDialogTitle(R.string.pref_free_text_font_init_dialog_title);
        setDialogMessage(R.string.pref_free_text_font_init_dialog_message);
        final CharSequence positiveButtonText = getPositiveButtonText();
        setPositiveButtonText(null);

        // start to collect entries in background process
        mPopulateFontInfoTask = new PopulateFontInfoTask();
        mPopulateFontInfoTask.setCallback(new PopulateFontInfoTask.Callback() {
            @Override
            public void getFontInfo(HashSet<String> defaultValues, CharSequence[] entries, CharSequence[] entryValues) {
                Context context = getContext();
                if (context == null) {
                    return;
                }

                // set Entries
                setEntries(entries);
                setEntryValues(entryValues);
                setPositiveButtonText(positiveButtonText);
                setDialogTitle(dialogTitle);
                setDialogMessage(null);
                if (isDialogShowing) {
                    onActivityDestroy();
                    showDialog(null);
                }

                // set default values
                SharedPreferences settings = Tool.getToolPreferences(context);
                Boolean isInitialized = settings.getBoolean(PdfViewCtrlSettingsManager.KEY_PREF_FREE_TEXT_FONTS_INIT, false);
                if (!isInitialized) {
                    onSetInitialValue(false, defaultValues);

                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(PdfViewCtrlSettingsManager.KEY_PREF_FREE_TEXT_FONTS_INIT, true);
                    editor.apply();
                }
            }
        });
        mPopulateFontInfoTask.execute();
    }

    @Override
    protected void onClick() {
        super.onClick();
        isDialogShowing = true;
        if (sDebug) Log.d(TAG, "onClick");
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        isDialogShowing = false;
        mPopulateFontInfoTask.cancel(true);
        mPopulateFontInfoTask.setCallback(null);
        if (sDebug) Log.d(TAG, "onDismiss");
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();

        final SavedState myState = new SavedState(superState);
        myState.isDialogShowing = isDialogShowing;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        isDialogShowing = myState.isDialogShowing;
    }

    public void setDebug(boolean enabled) {
        sDebug = enabled;
    }

    private static class SavedState extends BaseSavedState {
        boolean isDialogShowing;

        SavedState(Parcel source) {
            super(source);
            isDialogShowing = source.readInt() == 1;
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isDialogShowing ? 1 : 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    private static class FontPreferenceResource {
        private CharSequence mEntry;
        private CharSequence mEntryValue;

        FontPreferenceResource(CharSequence entry, CharSequence entryValue) {
            mEntry = entry;
            mEntryValue = entryValue;
        }

        CharSequence getEntry() {
            return mEntry;
        }

        CharSequence getEntryValue() {
            return mEntryValue;
        }
    }

    private static class PopulateFontInfoTask extends AsyncTask<Void, Void, Exception> {

        Callback mCallback;
        CharSequence[] mEntries;
        CharSequence[] mEntryValues;
        HashSet<String> mDefaultValues = new HashSet<>();

        interface Callback {
            /**
             * Called when font info has been populated.
             */
            void getFontInfo(HashSet<String> defaultValues, CharSequence[] entries, CharSequence[] entryValues);
        }

        /**
         * Sets the callback listener.
         *
         * Sets the callback to null when the task is cancelled.
         *
         * @param callback The callback when the task is finished
         */
        public void setCallback(@Nullable Callback callback) {
            mCallback = callback;
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                String fontInfo = PDFNet.getSystemFontList();

                if (isCancelled()) {
                    return null;
                }

                fontInfo = FontResource.whiteListFonts(fontInfo);

                JSONObject systemFontObject = new JSONObject(fontInfo);
                JSONArray systemFontArray = systemFontObject.getJSONArray(Tool.ANNOTATION_FREE_TEXT_JSON_FONT);

                ArrayList<FontPreferenceResource> tempEntries = new ArrayList<>();

                for (int i = 0, cnt = systemFontArray.length(); i < cnt; i++) {
                    JSONObject font = systemFontArray.getJSONObject(i);
                    String entry = font.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_DISPLAY_NAME);
                    String entryValue = font.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_FILE_PATH);
                    tempEntries.add(new FontPreferenceResource(entry, entryValue));

                    // set default values
                    String whiteList = font.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_DISPLAY_IN_LIST);
                    if (whiteList.equals("true")) {
                        mDefaultValues.add(entryValue);
                    }
                }

                if (isCancelled()) {
                    return null;
                }

                // sort entries and entry values
                Collections.sort(tempEntries, new Comparator<FontPreferenceResource>() {
                    @Override
                    public int compare(FontPreferenceResource lhs, FontPreferenceResource rhs) {
                        String lhsEntry = lhs.getEntry().toString();
                        String rhsEntry = rhs.getEntry().toString();
                        return lhsEntry.compareTo(rhsEntry);
                    }
                });

                if (isCancelled()) {
                    return null;
                }

                // set arrays
                mEntries = new CharSequence[systemFontArray.length()];
                mEntryValues = new CharSequence[systemFontArray.length()];
                for (int i = 0; i < systemFontArray.length(); i++) {
                    mEntries[i] = tempEntries.get(i).getEntry();
                    mEntryValues[i] = tempEntries.get(i).getEntryValue();
                }
            } catch (Exception e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception exception) {
            if (exception != null) {
                AnalyticsHandlerAdapter.getInstance().sendException(exception);
            } else if (mCallback != null) {
                mCallback.getFontInfo(mDefaultValues, mEntries, mEntryValues);
            }
        }
    }
}
