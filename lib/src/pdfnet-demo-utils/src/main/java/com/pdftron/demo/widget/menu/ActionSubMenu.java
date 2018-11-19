package com.pdftron.demo.widget.menu;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.internal.view.SupportSubMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

public class ActionSubMenu extends ActionMenu implements SupportSubMenu {

    private ActionMenu mParentMenu;
    private ActionMenuItem mItem;

    private Drawable mHeaderIconDrawable;
    private int mHeaderIconResId = NO_ICON;
    private CharSequence mHeaderTitle;
    private View mHeaderView;

    private Context mContext;

    private static final int NO_ICON = 0;

    public ActionSubMenu(Context context, ActionMenu parentMenu, ActionMenuItem item) {
        super(context);

        mContext = context;
        mParentMenu = parentMenu;
        mItem = item;
    }

    @Override
    public void clearHeader() {
        mHeaderTitle = null;
        mHeaderView = null;
    }

    @Override
    public MenuItem getItem() {
        return mItem;
    }

    @Override
    public SubMenu setHeaderIcon(Drawable icon) {
        mHeaderIconDrawable = icon;
        mHeaderIconResId = NO_ICON;
        return this;
    }

    @Override
    public SubMenu setHeaderIcon(@DrawableRes int iconRes) {
        mHeaderIconResId = iconRes;
        if (iconRes > 0)
            mHeaderIconDrawable = ContextCompat.getDrawable(mContext, iconRes);
        return this;
    }

    public Drawable getHeaderIcon() {
        return mHeaderIconDrawable;
    }

    @Override
    public SubMenu setHeaderTitle(CharSequence title) {
        mHeaderTitle = title;
        return this;
    }

    @Override
    public SubMenu setHeaderTitle(@StringRes int titleRes) {
        mHeaderTitle = mContext.getResources().getString(titleRes);
        return this;
    }

    public CharSequence getHeaderTitle() {
        return mHeaderTitle;
    }

    @Override
    public SubMenu setHeaderView(View view) {
        mHeaderView = view;
        return this;
    }

    public View getHeaderView() {
        return mHeaderView;
    }

    @Override
    public SubMenu setIcon(Drawable icon) {
        mItem.setIcon(icon);
        return this;
    }

    @Override
    public SubMenu setIcon(@DrawableRes int iconRes) {
        mItem.setIcon(iconRes);
        return this;
    }

    public Menu getParentMenu() {
        return mParentMenu;
    }
}
