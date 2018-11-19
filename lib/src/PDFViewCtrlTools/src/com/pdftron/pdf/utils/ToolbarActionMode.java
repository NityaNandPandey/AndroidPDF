//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.AnimRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.StringRes;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.pdftron.pdf.tools.R;

/**
 * Toolbar action mode
 */
public class ToolbarActionMode {

    private Context mContext;
    private Toolbar mToolbar;
    private ToolbarActionMode.Callback mCallback;
    private Animation mEnterAnimation;
    private Animation mExitAnimation;
    private Animation.AnimationListener mEnterAnimationListener;
    private Animation.AnimationListener mExitAnimationListener;
    private View mCustomView;

    /**
     * Class constructor
     * @param context The context
     * @param toolbar The toolbar
     */
    public ToolbarActionMode(Context context, Toolbar toolbar) {
        if (context == null || toolbar == null) {
            throw new IllegalArgumentException("Context and Toolbar must be non-null");
        }
        mContext = context;
        mToolbar = toolbar;

        setEnterAnimation(R.anim.action_mode_enter);
        setExitAnimation(R.anim.action_mode_exit);
    }

    /**
     * Starts action mode
     * @param callback The tool bar action mode callback
     */
    public void startActionMode(ToolbarActionMode.Callback callback) {
        mCallback = callback;
        if (mToolbar == null) {
            // Toolbar has not been set
            return;
        }

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToolbarActionMode.this.finish();
            }
        });
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return ToolbarActionMode.this.mCallback.onActionItemClicked(ToolbarActionMode.this, item);
            }
        });

        mCallback.onCreateActionMode(ToolbarActionMode.this, mToolbar.getMenu());
        invalidate();
        show();
    }

    /**
     * Invalidates action mode
     */
    public void invalidate() {
        Menu menu = (mToolbar != null) ? mToolbar.getMenu() : null;
        mCallback.onPrepareActionMode(ToolbarActionMode.this, menu);
    }

    /**
     * Finish action mode
     */
    public void finish() {
        mCallback.onDestroyActionMode(ToolbarActionMode.this);
        hide();
    }

    /**
     * Sets title to toolbar
     * @param resId title resource id
     */
    public void setTitle(@StringRes int resId) {
        if (mToolbar != null) {
            mToolbar.setTitle(resId);
        }
    }

    /**
     * Sets title to toolbar
     * @param title The title
     */
    public void setTitle(CharSequence title) {
        if (mToolbar != null) {
            mToolbar.setTitle(title);
        }
    }

    /**
     * Sets subtitle to toolbar
     * @param resId Subtitle resource id
     */
    public void setSubtitle(@StringRes int resId) {
        if (mToolbar != null) {
            mToolbar.setSubtitle(resId);
        }
    }

    /**
     * Sets subtitle to toolbar
     * @param subtitle The subtitle
     */
    public void setSubtitle(CharSequence subtitle) {
        if (mToolbar != null) {
            mToolbar.setSubtitle(subtitle);
        }
    }

    /**
     * Inflates toolbar menu
     * @param menuRes Toolbar menu resource
     */
    public void inflateMenu(@MenuRes int menuRes) {
        if (mToolbar != null) {
            mToolbar.getMenu().clear();
            mToolbar.inflateMenu(menuRes);
        }
    }

    /**
     * Sets toolbar navigation icon drawable
     * @param resId The drawable resource id
     */
    public void setCloseDrawable(@DrawableRes int resId) {
        if (mToolbar != null) {
            mToolbar.setNavigationIcon(resId);
        }
    }

    /**
     * Sets toolbar navigation icon drawable
     * @param drawable The drawable
     */
    public void setCloseDrawable(Drawable drawable) {
        if (mToolbar != null) {
            mToolbar.setNavigationIcon(drawable);
        }
    }

    /**
     * Sets toolbar custom view
     * @param view The custom view
     */
    public void setCustomView(View view) {
        if (mToolbar != null) {
            if (mCustomView != null) {
                mToolbar.removeView(mCustomView);
            }
            mToolbar.addView(view);
            mCustomView = view;
        }
    }

    /**
     * Sets toolbar enter animation
     * @param animId The animation resource id
     */
    public void setEnterAnimation(@AnimRes int animId) {
        mEnterAnimation = AnimationUtils.loadAnimation(mContext, animId);
        initEnterAnimationListener();
    }

    /**
     * Sets toolbar enter animation
     * @param animation The animation
     */
    public void setEnterAnimation(Animation animation) {
        mEnterAnimation = animation;
        initEnterAnimationListener();
    }

    /**
     * Sets toolbar exit animation
     * @param animId The animation resource id
     */
    public void setExitAnimation(@AnimRes int animId) {
        mExitAnimation = AnimationUtils.loadAnimation(mContext, animId);
        initExitAnimationListener();
    }

    /**
     * Sets toolbar exit animation
     * @param animation The animation
     */
    public void setExitAnimation(Animation animation) {
        mExitAnimation = animation;
        initExitAnimationListener();
    }

    /**
     * Gets toolbar menu
     * @return Toolbar menu
     */
    public Menu getMenu() {
        return (mToolbar != null) ? mToolbar.getMenu() : null;
    }

    /**
     * Gets toolbar title
     * @return Toolbar title
     */
    public CharSequence getTitle() {
        return (mToolbar != null) ? mToolbar.getTitle() : "";
    }

    /**
     * Gets toolbar subtitle
     * @return Toolbar subtitle
     */
    public CharSequence getSubtitle() {
        return (mToolbar != null) ? mToolbar.getSubtitle() : "";
    }

    /**
     * Gets toolbar custom view
     * @return Toolbar custom view
     */
    public View getCustomView() {
        return mCustomView;
    }

    /**
     * Shows toolbar
     */
    private void show() {
        if (mToolbar != null) {
            if (mEnterAnimation != null) {
                mToolbar.startAnimation(mEnterAnimation);
            } else {
                mToolbar.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Hides toolbar
     */
    private void hide() {
        if (mToolbar != null) {
            if (mExitAnimation != null) {
                mToolbar.startAnimation(mExitAnimation);
            } else {
                mToolbar.setVisibility(View.GONE);
            }
        }
    }

    private void initEnterAnimationListener() {
        // Create animation listener, if not already created
        if (mEnterAnimationListener == null) {
            mEnterAnimationListener = new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if (mToolbar != null) {
                        mToolbar.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onAnimationEnd(Animation animation) {}

                @Override
                public void onAnimationRepeat(Animation animation) {}
            };
        }
        mEnterAnimation.setAnimationListener(mEnterAnimationListener);
    }

    private void initExitAnimationListener() {
        // Create animation listener, if not already created
        if (mExitAnimationListener == null) {
            mExitAnimationListener = new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mToolbar != null) {
                        mToolbar.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            };
        }
        mExitAnimation.setAnimationListener(mExitAnimationListener);
    }

    /**
     * Callback interface to be invoked when an interaction is needed (see {@link android.view.ActionMode.Callback}).
     */
    public interface Callback {
        /**
         * Called when a toolbar action mode is first created. The menu supplied will be used to
         * generate action buttons for the action mode.
         *
         * @param mode The toolbar action mode being created
         * @param menu The menu used to populate action buttons
         *
         * @return True if the toolbar action mode should be created,
         * false if entering this mode should be aborted.
         */
        boolean onCreateActionMode(ToolbarActionMode mode, Menu menu);

        /**
         * Called to report a user click on an action button.
         *
         * @param mode The current toolbar action mode
         * @param item The menu item that was clicked
         *
         * @return True if this callback handled the event
         */
        boolean onActionItemClicked(ToolbarActionMode mode, MenuItem item);

        /**
         * Called when a toolbar action mode is about to be exited and destroyed.
         *
         * @param mode The current toolbar action mode being destroyed
         */
        void onDestroyActionMode(ToolbarActionMode mode);

        /**
         * Called to refresh a toolbar action mode's action menu whenever it is invalidated.
         *
         * @param mode The toolbar action mode being prepared
         * @param menu The menu used to populate action buttons
         *
         * @return True if the menu or toolbar action mode was updated, false otherwise.
         */
        boolean onPrepareActionMode(ToolbarActionMode mode, Menu menu);
    }
}
