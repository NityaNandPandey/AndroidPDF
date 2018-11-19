package com.pdftron.demo.navigation.callbacks;

import android.view.KeyEvent;

// Fragments must implement this interface to receive events from the main activity.
public interface MainActivityListener {

    void onPreLaunchViewer();

    void onDataChanged();

    void onProcessNewIntent();

    void onDrawerOpened();

    void onDrawerSlide();

    boolean onBackPressed();

    boolean onKeyUp(int keyCode, KeyEvent event);
}
