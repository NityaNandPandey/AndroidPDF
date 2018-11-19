package com.pdftron.demo.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.os.AsyncTask;

import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.FavoriteFilesManager;
import com.pdftron.pdf.utils.FileInfoManager;

public class FavoriteViewModel extends AndroidViewModel {

    private boolean mDataLoaded;
    private AsyncTask<Void, Void, Void> mTask;

    public FavoriteViewModel(Application application) {
        super(application);
        if (!mDataLoaded) {
            loadData();
        }
    }

    protected FileInfoManager getFavoriteFilesManager() {
        return FavoriteFilesManager.getInstance();
    }

    private void loadData() {
        mTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                getFavoriteFilesManager().getFiles(getApplication());
                mDataLoaded = true;
                return null;
            }
        };
        mTask.executeOnExecutor(CustomAsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (mTask != null) {
            mTask.cancel(true);
        }
    }
}
