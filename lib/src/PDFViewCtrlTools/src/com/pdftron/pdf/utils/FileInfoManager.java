//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.pdftron.pdf.model.FileInfo;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class to manage files defined as {@link FileInfo}
 */
public class FileInfoManager {

    final static private String DELIMITER = " ";

    final private Object locker = new Object();

    protected final String mKeyPreferenceFiles;
    private final int mMaxNumFiles;

    private List<Integer> mUsedRefSet;
    private List<Integer> mAvailableRefSet = new ArrayList<>();
    private List<FileInfo> mInternalFiles; // NOTE: always keep its size fixed
    private boolean mAllLoaded;
    private Gson mGson = new Gson();

    protected FileInfoManager(String keyPreferenceFiles, int maxNumFiles) {
        mKeyPreferenceFiles = keyPreferenceFiles;
        mMaxNumFiles = maxNumFiles;
    }

    private static SharedPreferences getDefaultSharedPreferences(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    /**
     * Returns the number of files in the repository.
     *
     * @param context The context
     * @return The number of files
     */
    public int size(@NonNull Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs == null) {
            return 0;
        }

        loadPreferenceRefs(prefs);

        return mUsedRefSet.size();
    }

    /**
     * Checks whether there is any file in the repository.
     *
     * @param context The context
     * @return True if there is no file in the repository
     */
    public boolean isEmpty(@NonNull Context context) {
        return size(context) == 0;
    }

    /**
     * Checks whether the specified file exists in the repository.
     *
     * @param context  The context
     * @param fileInfo The {@link FileInfo}
     * @return True if the file exists in the repository
     */
    public boolean containsFile(@NonNull Context context, @Nullable FileInfo fileInfo) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs == null || fileInfo == null) {
            return false;
        }

        createInternalFiles();

        synchronized (locker) {
            if (mInternalFiles.contains(fileInfo)) {
                return true;
            }
            if (!mAllLoaded) {
                load(prefs, fileInfo);
            }
            return mInternalFiles.contains(fileInfo);
        }
    }

    /**
     * Saves the specified files into the repository.
     *
     * @param context The context
     * @param files   A list of files to be added into the repository
     */
    public void saveFiles(@NonNull Context context, @NonNull List<FileInfo> files) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs == null) {
            return;
        }

        createInternalFiles();

        synchronized (locker) {
            save(prefs, files);
        }
        mAllLoaded = true;
    }

    /**
     * Returns all files in the repository
     *
     * @param context The context
     * @return A list of all files in the repository
     */
    @NonNull
    public List<FileInfo> getFiles(@Nullable Context context) {
        List<FileInfo> files = new ArrayList<>();
        if (context == null) {
            return files;
        }
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs == null) {
            return files;
        }

        createInternalFiles();

        // size can be changed within loop by adding/removing item(s)
        for (int i = 0; i < size(context) && i < mMaxNumFiles; ++i) {
            synchronized (locker) {
                if (mInternalFiles.get(i) == null) {
                    load(prefs, i);
                }
            }

            synchronized (locker) {
                FileInfo fileInfo = mInternalFiles.get(i);
                if (fileInfo != null) {
                    files.add(cloneFileInfo(fileInfo));
                }
            }
        }

        mAllLoaded = true;
        return files;
    }

    /**
     * Returns the n'th file added into the repository.
     *
     * @param context The context
     * @param index   The index of the file
     * @return The n'th {@link FileInfo} int the repository
     */
    @Nullable
    public FileInfo getFile(@NonNull Context context, int index) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs == null || index < 0 || index >= mMaxNumFiles) {
            return null;
        }

        createInternalFiles();

        synchronized (locker) {
            if (mInternalFiles.get(index) == null) {
                load(prefs, index);
            }

            return cloneFileInfo(mInternalFiles.get(index));
        }
    }

    /**
     * Returns the file in the repository that equals to the specified file.
     * See {@link FileInfo#equals(Object)}.
     *
     * @param context  The context
     * @param fileInfo The file info
     * @return The {@link FileInfo}
     */
    @Nullable
    public FileInfo getFile(@NonNull Context context, @Nullable FileInfo fileInfo) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs == null || fileInfo == null) {
            return null;
        }

        createInternalFiles();

        synchronized (locker) {
            if (!mInternalFiles.contains(fileInfo)) {
                if (!mAllLoaded) {
                    load(prefs, fileInfo);
                }
                if (!mInternalFiles.contains(fileInfo)) {
                    return null;
                }
            }

            int index = mInternalFiles.indexOf(fileInfo);
            return cloneFileInfo(mInternalFiles.get(index));
        }
    }

    /**
     * Adds the specified file into the repository.
     *
     * @param context  The context
     * @param fileInfo The file info
     */
    public void addFile(@NonNull Context context, @Nullable FileInfo fileInfo) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs == null || fileInfo == null) {
            return;
        }

        createInternalFiles();

        synchronized (locker) {
            if (!mAllLoaded && !mInternalFiles.contains(fileInfo)) {
                load(prefs, fileInfo);
            }
        }

        synchronized (locker) {
            // if already exists in the list, remove it and add it in front
            if (mInternalFiles.contains(fileInfo)) {
                int index = mInternalFiles.indexOf(fileInfo);
                remove(prefs, index);
            }
            add(prefs, 0, fileInfo);
        }
    }

    /**
     * Removes all files from the repository.
     *
     * @param context The context
     */
    public void clearFiles(@NonNull Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs == null) {
            return;
        }

        createInternalFiles();

        synchronized (locker) {
            for (int i = 0; i < mMaxNumFiles; ++i) {
                mInternalFiles.set(i, null);
            }
            clearPreferenceRefs(prefs);
        }
        mAllLoaded = true;
    }

    /**
     * Removes the specified file from the repository.
     *
     * @param context      The context
     * @param fileToRemove The file to be removed
     * @return True if the specified file was in the repository and removed successfully
     */
    public boolean removeFile(@Nullable Context context, @Nullable FileInfo fileToRemove) {
        if (context == null) {
            return false;
        }

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs == null || fileToRemove == null) {
            return false;
        }

        createInternalFiles();

        synchronized (locker) {
            if (!mInternalFiles.contains(fileToRemove) && !mAllLoaded) {
                load(prefs, fileToRemove);
            }
        }

        synchronized (locker) {
            if (mInternalFiles.contains(fileToRemove)) {
                int index = mInternalFiles.indexOf(fileToRemove);
                remove(prefs, index);
                return true;
            }
        }

        return false;
    }

    /**
     * Removes the specified files from the repository.
     *
     * @param context       The context
     * @param filesToRemove The list of files to be removed
     * @return The list of files that were in the repository and removed successfully
     */
    @NonNull
    public List<FileInfo> removeFiles(@Nullable Context context, @Nullable List<FileInfo> filesToRemove) {
        List<FileInfo> filesRemoved = new ArrayList<>();
        if (context == null) {
            return filesRemoved;
        }
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs == null || filesToRemove == null) {
            return filesRemoved;
        }

        createInternalFiles();

        for (FileInfo fileToRemove : filesToRemove) {
            synchronized (locker) {
                if (!mInternalFiles.contains(fileToRemove) && !mAllLoaded) {
                    load(prefs, fileToRemove);
                }
            }

            synchronized (locker) {
                if (mInternalFiles.contains(fileToRemove)) {
                    int index = mInternalFiles.indexOf(fileToRemove);
                    remove(prefs, index);
                    filesRemoved.add(fileToRemove);
                }
            }
        }
        return filesRemoved;
    }

    /**
     * Updates the file in the repository.
     *
     * @param context The context
     * @param oldFile The old file info
     * @param newFile The new file info
     * @return True if the file info was updated successfully
     */
    public boolean updateFile(@NonNull Context context, @Nullable FileInfo oldFile, @Nullable FileInfo newFile) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs == null || oldFile == null || newFile == null) {
            return false;
        }

        createInternalFiles();

        synchronized (locker) {
            if (!mInternalFiles.contains(oldFile) && !mAllLoaded) {
                load(prefs, oldFile);
            }
        }

        synchronized (locker) {
            if (mInternalFiles.contains(oldFile)) {
                int index = mInternalFiles.indexOf(oldFile);
                set(prefs, index, newFile);
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the information of the specified file in the repository.
     *
     * @param context  The context
     * @param fileInfo The file info
     */
    public void updateFile(@NonNull Context context, @Nullable FileInfo fileInfo) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        if (prefs == null || fileInfo == null) {
            return;
        }

        createInternalFiles();

        synchronized (locker) {
            if (!mInternalFiles.contains(fileInfo) && !mAllLoaded) {
                load(prefs, fileInfo);
            }
        }

        synchronized (locker) {
            if (mInternalFiles.contains(fileInfo)) {
                int index = mInternalFiles.indexOf(fileInfo);
                // update information
                set(prefs, index, fileInfo);
            }
        }
    }

    private void save(@NonNull SharedPreferences prefs, @NonNull List<FileInfo> files) {
        mInternalFiles.clear();
        mInternalFiles.addAll(files);
        for (int i = mInternalFiles.size(); i < mMaxNumFiles; ++i) {
            mInternalFiles.add(i, null);
        }
        mAllLoaded = true;

        clearPreferenceRefs(prefs);
        int index = 0;
        for (FileInfo fileInfo : files) {
            String serializedDoc = mGson.toJson(fileInfo);
            addPreferenceItem(prefs, serializedDoc, index++);
        }
    }

    private void load(@NonNull SharedPreferences prefs, int index) {
        String data = getPreferenceItem(prefs, index);
        FileInfo fileInfo = null;
        if (!Utils.isNullOrEmpty(data)) {
            try {
                JSONObject jsonObject = new JSONObject(data);
                fileInfo = getFileInfo(jsonObject);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
        mInternalFiles.set(index, fileInfo);
    }

    // load all file info that has the same filename
    private void load(@NonNull SharedPreferences prefs, FileInfo fileInfo) {
        loadPreferenceRefs(prefs);

        int index, cnt;
        for (index = 0, cnt = mUsedRefSet.size(); index < cnt; ++index) {
            if (mInternalFiles.get(index) != null) {
                continue;
            }
            String data = getPreferenceItem(prefs, index);
            if (!Utils.isNullOrEmpty(data)) {
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    FileInfo file = getFileInfo(jsonObject);
                    mInternalFiles.set(index, file);
                    if (fileInfo.equals(file)) {
                        break;
                    }
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        }
        if (index == cnt) {
            mAllLoaded = true;
        }
    }

    private void set(@NonNull SharedPreferences prefs, int index, @NonNull FileInfo fileInfo) {
        mInternalFiles.set(index, fileInfo);
        String serializedDoc = mGson.toJson(fileInfo);
        setPreferenceItem(prefs, serializedDoc, index);
    }

    private void add(@NonNull SharedPreferences prefs, @SuppressWarnings("SameParameterValue") int index, @NonNull FileInfo fileInfo) {
        mInternalFiles.add(index, fileInfo);
        mInternalFiles.remove(mMaxNumFiles);
        String serializedDoc = mGson.toJson(fileInfo);
        addPreferenceItem(prefs, serializedDoc, index);
    }

    private void remove(@NonNull SharedPreferences prefs, int index) {
        mInternalFiles.remove(index);
        mInternalFiles.add(mMaxNumFiles - 1, null);
        removePreferenceItem(prefs, index);
    }

    private void createInternalFiles() {
        synchronized (locker) {
            if (mInternalFiles == null) {
                mInternalFiles = new ArrayList<>(mMaxNumFiles);
                for (int i = 0; i < mMaxNumFiles; ++i) {
                    mInternalFiles.add(null);
                }
            }
        }
    }

    protected void loadPreferenceRefs(@NonNull SharedPreferences prefs) {
        if (mUsedRefSet != null) {
            return;
        }

        mUsedRefSet = new ArrayList<>();
        for (Integer ref = 0; ref < mMaxNumFiles; ++ref) {
            mAvailableRefSet.add(ref);
        }

        String data = prefs.getString(mKeyPreferenceFiles + "_refs", "");
        if (!Utils.isNullOrEmpty(data)) {
            String[] refs = data.split(DELIMITER);
            for (String r : refs) {
                Integer ref = Integer.parseInt(r);
                mAvailableRefSet.remove(ref);
                mUsedRefSet.add(ref);
            }
        }
    }

    protected void addPreferenceItem(@NonNull SharedPreferences prefs, String data, int index) {
        loadPreferenceRefs(prefs);

        Integer ref;
        if (mAvailableRefSet.isEmpty()) {
            ref = mUsedRefSet.remove(mUsedRefSet.size() - 1);
        } else {
            ref = mAvailableRefSet.remove(0);
        }

        mUsedRefSet.add(index, ref);
        StringBuilder refs = new StringBuilder();
        String delimiter = "";
        for (Integer r : mUsedRefSet) {
            refs.append(delimiter).append(r);
            delimiter = DELIMITER;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(mKeyPreferenceFiles + "_" + ref, data);
        editor.putString(mKeyPreferenceFiles + "_refs", refs.toString());
        editor.apply();
    }

    private void setPreferenceItem(@NonNull SharedPreferences prefs, String data, int index) {
        loadPreferenceRefs(prefs);

        if (index < 0 || index > mUsedRefSet.size()) {
            AnalyticsHandlerAdapter.getInstance().sendException(
                new Exception("out of bound index! (index: " + index + ") size: " + mUsedRefSet.size() + ")"));
            return;
        }

        if (index == mUsedRefSet.size()) {
            addPreferenceItem(prefs, data, index);
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        Integer ref = mUsedRefSet.get(index);
        editor.putString(mKeyPreferenceFiles + "_" + ref, data);
        editor.apply();
    }

    private void removePreferenceItem(@NonNull SharedPreferences prefs, int index) {
        loadPreferenceRefs(prefs);

        if (index < 0 || index >= mUsedRefSet.size()) {
            AnalyticsHandlerAdapter.getInstance().sendException(
                new Exception("out of bound index! (index: " + index + ") size: " + mUsedRefSet.size() + ")"));
            return;
        }

        Integer ref = mUsedRefSet.remove(index);
        mAvailableRefSet.add(ref);
        StringBuilder refs = new StringBuilder();
        String delimiter = "";
        for (Integer r : mUsedRefSet) {
            refs.append(delimiter).append(r);
            delimiter = DELIMITER;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(mKeyPreferenceFiles + "_refs", refs.toString());
        editor.apply();
    }

    private String getPreferenceItem(@NonNull SharedPreferences prefs, int index) {
        loadPreferenceRefs(prefs);

        if (index >= mUsedRefSet.size()) {
            return "";
        }
        Integer ref = mUsedRefSet.get(index);
        if (ref == null) {
            return "";
        }
        return prefs.getString(mKeyPreferenceFiles + "_" + ref, "");
    }

    private void clearPreferenceRefs(@NonNull SharedPreferences prefs) {
        mUsedRefSet = new ArrayList<>();
        for (Integer ref = 0; ref < mMaxNumFiles; ++ref) {
            mAvailableRefSet.add(ref);
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(mKeyPreferenceFiles + "_refs");
        editor.apply();
    }

    @Nullable
    protected FileInfo cloneFileInfo(@Nullable FileInfo fileInfo) {
        if (fileInfo == null) {
            return null;
        }
        return new FileInfo(fileInfo);
    }

    protected FileInfo getFileInfo(JSONObject jsonObject) {
        return new FileInfo(jsonObject);
    }
}
