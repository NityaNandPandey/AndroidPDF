//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import com.pdftron.pdf.controls.BookmarksTabLayout;
import com.pdftron.pdf.dialog.BookmarksDialogFragment;

/**
 * Structure of dialog fragment that used in {@link BookmarksDialogFragment} as a single tab.
 */
public class DialogFragmentTab {
    /**
     * The type of class should be an instance of {@link DialogFragment}.
     * Note: must not be null.
     */
    public Class<?> _class;

    /**
     * The tab tag. Only possible values are
     *      {@link BookmarksTabLayout#TAG_TAB_BOOKMARK}
     *      {@link BookmarksTabLayout#TAG_TAB_OUTLINE}
     *      {@link BookmarksTabLayout#TAG_TAB_ANNOTATION}
     */
    public String tabTag;

    /**
     * The tab icon
     */
    @Nullable
    public Drawable tabIcon;

    /**
     * The tab text
     * Note: it can be set to null.
     */
    @Nullable
    public String tabText;

    /**
     * The title shown in the toolbar when the tab is selected.
     */
    @Nullable
    public String toolbarTitle;

    /**
     * any arguments that should be passed to the {@link DialogFragment}
     */
    @Nullable
    public Bundle bundle;

    /**
     * Class constructor
     */
    @SuppressWarnings("unused")
    public DialogFragmentTab(@NonNull Class<?> _class,
                             @NonNull String tabTag) {
        this._class = _class;
        this.tabTag = tabTag;
    }

    public DialogFragmentTab(@NonNull Class<?> _class,
                             @NonNull String tabTag,
                             @Nullable Drawable tabIcon,
                             @Nullable String tabText,
                             @Nullable String toolbarTitle,
                             @Nullable Bundle bundle) {
        this._class = _class;
        this.tabTag = tabTag;
        this.tabIcon = tabIcon;
        this.tabText = tabText;
        this.toolbarTitle = toolbarTitle;
        this.bundle = bundle;
    }
}
