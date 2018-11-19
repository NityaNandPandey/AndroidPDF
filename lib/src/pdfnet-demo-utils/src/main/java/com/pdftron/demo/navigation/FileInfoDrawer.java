package com.pdftron.demo.navigation;

import android.content.Context;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.pdftron.demo.widget.ImageViewTopCrop;

public class FileInfoDrawer implements
        DrawerLayout.DrawerListener,
        FileInfoDrawerFragment.Listener {

    private Context mContext;
    private DrawerLayout mDrawerLayout;
    private View mDrawerView;
    private FileInfoDrawerFragment mFragment;
    private FileInfoDrawer.Callback mCallback;

    private Point mDimensions;

    public FileInfoDrawer(Context context, DrawerLayout drawerLayout, View drawerView,
                          FileInfoDrawerFragment fragment) {
        if (context == null || fragment == null) {
            throw new IllegalArgumentException("Context and Fragment must be non-null");
        }
        mContext = context;
        mDrawerLayout = drawerLayout;
        mDrawerView = drawerView;
        mFragment = fragment;

        mDimensions = new Point();

        mFragment.setListener(this);
    }

    public void show(FileInfoDrawer.Callback callback) {
        mCallback = callback;

        if (mCallback != null) {
            // Initialize the title
            CharSequence title = mCallback.onPrepareTitle(FileInfoDrawer.this);
            mFragment.setTitle(title);

            // Initialize the header image
            mCallback.onPrepareHeaderImage(FileInfoDrawer.this, mFragment.getHeaderImageView());

            // Initialize the buttons menu
            Menu menu = mFragment.getButtonsMenu();
            if (menu != null) {
                menu.clear();
            }
            if (mCallback.onCreateDrawerMenu(FileInfoDrawer.this, menu)) {
                mCallback.onPrepareDrawerMenu(FileInfoDrawer.this, menu);
            }

            CharSequence mainContent = mCallback.onPrepareMainContent(FileInfoDrawer.this);
            mFragment.setMainContent(mainContent, mCallback.onPrepareIsSecured(FileInfoDrawer.this));

            mFragment.invalidateButtons();
        }

        if (mDrawerLayout != null && mDrawerView != null) {
            mDrawerLayout.openDrawer(mDrawerView);
        }
    }

    public void invalidate() {
        if (mCallback != null) {
            // Update title
            CharSequence title = mCallback.onPrepareTitle(FileInfoDrawer.this);
            mFragment.setTitle(title);

            // Update buttons menu
            Menu menu = mFragment.getButtonsMenu();
            mCallback.onPrepareDrawerMenu(FileInfoDrawer.this, menu);

            mFragment.invalidateButtons();
        }
    }

    public void hide() {
        if (mDrawerLayout != null && mDrawerView != null) {
            mDrawerLayout.closeDrawer(mDrawerView);
        }
    }

    public void setTitle(@StringRes int titleRes) {
        String title = mContext.getString(titleRes);
        mFragment.setTitle(title);
    }

    public void setTitle(CharSequence title) {
        mFragment.setTitle(title);
    }

    public void getTitle() {
        mFragment.getTitle();
    }

    public void setIsSecured(boolean isSecured) {
        mFragment.setLockVisibility(isSecured);
    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView) {
        if (mCallback != null) {
            mCallback.onShowDrawer(FileInfoDrawer.this);
        }
    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView) {
        mFragment.resetScroll();
        mFragment.setLockVisibility(false);

        if (mCallback != null) {
            mCallback.onHideDrawer(FileInfoDrawer.this);
        }
    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
        // Do nothing
    }

    @Override
    public void onDrawerStateChanged(int newState) {
        // Do nothing
    }

    public void setDimensions(int width, int height) {
        mDimensions.set(width, height);
        mFragment.setDimensions(width, height);
    }

    public Point getDimensions() {
        return mDimensions;
    }

    public FileInfoDrawer.Callback getCallback() {
        return mCallback;
    }

    // FileInfoDrawerFragment.Listener methods

    @Override
    public void onButtonClicked(MenuItem menuItem) {
        if (mCallback != null) {
            mCallback.onDrawerMenuItemClicked(FileInfoDrawer.this, menuItem);
        }
    }

    @Override
    public void onNavigationButtonClicked() {
        hide();
    }

    @Override
    public void onThumbnailClicked() {
        if(mCallback != null){
            mCallback.onThumbnailClicked(FileInfoDrawer.this);
        }
    }

    public interface Callback {

        CharSequence onPrepareTitle(FileInfoDrawer drawer);

        void onPrepareHeaderImage(FileInfoDrawer drawer, ImageViewTopCrop imageView);

        boolean onPrepareIsSecured(FileInfoDrawer drawer);

        CharSequence onPrepareMainContent(FileInfoDrawer drawer);

        boolean onCreateDrawerMenu(FileInfoDrawer drawer, Menu menu);

        boolean onPrepareDrawerMenu(FileInfoDrawer drawer, Menu menu);

        boolean onDrawerMenuItemClicked(FileInfoDrawer drawer, MenuItem menuItem);

        void onThumbnailClicked(FileInfoDrawer drawer);

        void onShowDrawer(FileInfoDrawer drawer);

        void onHideDrawer(FileInfoDrawer drawer);
    }
}
