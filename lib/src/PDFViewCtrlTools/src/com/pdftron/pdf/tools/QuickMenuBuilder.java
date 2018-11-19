//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.RestrictTo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import com.pdftron.pdf.config.ToolConfig;
import com.pdftron.pdf.tools.ToolManager.ToolMode;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * A helper {@link Menu} class for constructing {@link QuickMenu}
 */
public class QuickMenuBuilder implements Menu, SubMenu {
    private QuickMenuItem mParentItem;
    private ArrayList<QuickMenuItem> mQuickMenuItems;
    private Context mContext;
    private ToolManager mToolManager;
    private boolean mAnnotationPermission = true;

    public QuickMenuBuilder(Context context, ToolManager toolManager, boolean annotationPermission) {
        this.mContext = context;
        mQuickMenuItems = new ArrayList<>();
        mToolManager = toolManager;
        mAnnotationPermission = annotationPermission;
    }

    /**
     * Get menu item display mode by group id
     * @param groupId group id
     * @return menu item display mode. See: {@link QuickMenuItem.MenuDisplayMode}
     */
    private @QuickMenuItem.MenuDisplayMode int getDisplayModeByGroupId(int groupId) {
        if (groupId == R.id.qm_overflow_row_group) {
            return QuickMenuItem.OVERFLOW_ROW_MENU;
        } else if (groupId == R.id.qm_second_row_group) {
            return QuickMenuItem.SECOND_ROW_MENU;
        } else {
            return QuickMenuItem.FIRST_ROW_MENU;
        }
    }


    /**
     * Add a new item to the menu. This item displays the given title for its
     * label.
     *
     * @param title The text to display for the item.
     * @return The newly added menu item.
     */
    @Override
    public MenuItem add(CharSequence title) {
        QuickMenuItem result = new QuickMenuItem(mContext, title.toString());
        mQuickMenuItems.add(result);
        return result;
    }

    /**
     * Add a new item to the menu. This item displays the given title for its
     * label.
     *
     * @param titleRes Resource identifier of title string.
     * @return The newly added menu item.
     */
    @Override
    public MenuItem add(int titleRes) {
        return add(mContext.getString(titleRes));
    }

    /**
     * Add a new item to the menu. This item displays the given title for its
     * label.
     *
     * @param groupId The group identifier that this item should be part of.
     *        This can be used to define groups of items for batch state
     *        changes. Normally use {@link #NONE} if an item should not be in a
     *        group.
     * @param itemId Unique item ID. Use {@link #NONE} if you do not need a
     *        unique ID.
     * @param order The order for the item. Use {@link #NONE} if you do not care
     *        about the order. See {@link MenuItem#getOrder()}.
     * @param title The text to display for the item.
     * @return The newly added menu item.
     */
    @Override
    public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
        int displayMode = getDisplayModeByGroupId(groupId);
        QuickMenuItem result = new QuickMenuItem(mContext, title.toString(), displayMode);
        if (isQuickMenuItemValid(itemId)) {
            result.setItemId(itemId);
            result.setOrder(order);
            mQuickMenuItems.add(result);
        }
        return result;
    }

    /**
     * Variation on {@link #add(int, int, int, CharSequence)} that takes a
     * string resource identifier instead of the string itself.
     *
     * @param groupId The group identifier that this item should be part of.
     *        This can also be used to define groups of items for batch state
     *        changes. Normally use {@link #NONE} if an item should not be in a
     *        group.
     * @param itemId Unique item ID. Use {@link #NONE} if you do not need a
     *        unique ID.
     * @param order The order for the item. Use {@link #NONE} if you do not care
     *        about the order. See {@link MenuItem#getOrder()}.
     * @param titleRes Resource identifier of title string.
     * @return The newly added menu item.
     */
    @Override
    public MenuItem add(int groupId, int itemId, int order, int titleRes) {
        return add(groupId, itemId, order, mContext.getString(titleRes));
    }

    /**
     * Check if the given quick menu item id is valid or not
     * @param itemId quick menu item id
     * @return true if valid, false otherwise
     */
    private boolean isQuickMenuItemValid(int itemId) {
        // if the menu item is disabled, don't add it to the list
        ToolMode toolMode;
        if (itemId != -1 && (toolMode = ToolConfig.getInstance().getToolModeByQMItemId(itemId)) != null) {
            if (mToolManager.isToolModeDisabled(toolMode)) {
                return false;
            }
        }

        // remove items that should hide when there is no permission
        boolean shouldHide = !mAnnotationPermission || mToolManager.isReadOnly();
        if (shouldHide && itemId != -1 && ToolConfig.getInstance().isHideQMItem(itemId)) {
            return false;
        }

        return true;
    }

    /**
     * Remove the item with the given identifier.
     *
     * @param id The item to be removed.  If there is no item with this
     *           identifier, nothing happens.
     */
    @Override
    public void removeItem(int id) {
        for (QuickMenuItem item : mQuickMenuItems) {
            if (item.getItemId() == id) {
                mQuickMenuItems.remove(item);
                break;
            }
        }
    }

    /**
     * Remove all items in the given group.
     *
     * @param groupId The group to be removed.  If there are no items in this
     *           group, nothing happens.
     */
    @Override
    public void removeGroup(int groupId) {
        int displayMode = getDisplayModeByGroupId(groupId);
        ListIterator<QuickMenuItem> iterator = mQuickMenuItems.listIterator();
        while (iterator.hasNext()) {
            QuickMenuItem item = iterator.next();
            if (item.getDisplayMode() == displayMode) {
                iterator.remove();
            }
        }
    }

    /**
     * Remove all existing items from the menu, leaving it empty as if it had
     * just been created.
     */
    @Override
    public void clear() {
        mQuickMenuItems.clear();
    }

    /**
     * Return the menu item with a particular identifier.
     *
     * @param id The identifier to find.
     *
     * @return The menu item object, or null if there is no item with
     *         this identifier.
     */
    @Override
    public MenuItem findItem(int id) {
        for (QuickMenuItem item : mQuickMenuItems) {
            if (item.getItemId() == id) {
                return item;
            }
        }
        return null;
    }

    /**
     * Gets the number of items in the menu.  Note that this will change any
     * times items are added or removed from the menu.
     *
     * @return The item count.
     */
    @Override
    public int size() {
        return mQuickMenuItems.size();
    }

    /**
     * Gets the menu item at the given index.
     *
     * @param index The index of the menu item to return.
     * @return The menu item.
     * @exception IndexOutOfBoundsException
     *                when {@code index < 0 || >= size()}
     */
    @Override
    public MenuItem getItem(int index) {
        return mQuickMenuItems.get(index);
    }

    /**
     * Same as {@link #clear()}
     */
    @Override
    public void close() {
        clear();
    }

    /**
     * Gets menu items, it will remove items if menu item's sub menu list is empty
     * @return list of valid quick menu items
     */
    public ArrayList<QuickMenuItem> getMenuItems() {
        // remove all items where it's sub menu list is empty
        ListIterator listIterator = mQuickMenuItems.listIterator();
        while (listIterator.hasNext()) {
            QuickMenuItem item = (QuickMenuItem) listIterator.next();
            if (!isQuickMenuItemValid(item.getItemId())) {
                listIterator.remove();
                continue;
            }
            // remove items that it's subclass list is empty
            if (item.hasSubMenu()) {
                QuickMenuBuilder subMenu = (QuickMenuBuilder) item.getSubMenu();
                ArrayList<QuickMenuItem> subItems = subMenu.getMenuItems();
                if (subItems.isEmpty()) {
                    listIterator.remove();
                }
            }
        }
        return mQuickMenuItems;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {

    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public void setGroupVisible(int group, boolean visible) {

    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public void setGroupEnabled(int group, boolean enabled) {

    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public boolean hasVisibleItems() {
        return false;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public boolean performShortcut(int keyCode, KeyEvent event, int flags) {
        return false;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public boolean isShortcutKey(int keyCode, KeyEvent event) {
        return false;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public boolean performIdentifierAction(int id, int flags) {
        return false;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public void setQwertyMode(boolean isQwerty) {

    }


    /**
     * Add a new sub-menu to the menu. This item displays the given title for
     * its label. To modify other attributes on the submenu's menu item, use
     * {@link #getItem()}.
     *
     * @param title The text to display for the item.
     * @return The newly added sub-menu
     */
    public SubMenu addSubMenu(CharSequence title) {
        QuickMenuItem item = (QuickMenuItem) add(title);
        item.initSubMenu(mToolManager, mAnnotationPermission);
        return item.getSubMenu();
    }
    /**
     * Add a new sub-menu to the menu. This item displays the given title for
     * its label. To modify other attributes on the submenu's menu item, use
     * {@link #getItem()}.
     *
     * @param titleRes Resource identifier of title string.
     * @return The newly added sub-menu
     */
    public SubMenu addSubMenu(int titleRes) {
        QuickMenuItem item = (QuickMenuItem) add(titleRes);
        item.initSubMenu(mToolManager, mAnnotationPermission);
        return item.getSubMenu();
    }

    /**
     * Add a new sub-menu to the menu. This item displays the given
     * <var>title</var> for its label. To modify other attributes on the
     * submenu's menu item, use {@link #getItem()}.
     *<p>
     * <div class="warning">
     * You can only have one level of sub-menus, i.e. you cannnot add
     * a subMenu to a subMenu: An {@link UnsupportedOperationException} will be
     * thrown if you try.
     * </div>
     *
     * @param groupId The group identifier that this item should be part of.
     *        This can also be used to define groups of items for batch state
     *        changes. Normally use {@link #NONE} if an item should not be in a
     *        group.
     * @param itemId Unique item ID. Use {@link #NONE} if you do not need a
     *        unique ID.
     * @param order The order for the item. Use {@link #NONE} if you do not care
     *        about the order. See {@link MenuItem#getOrder()}.
     * @param title The text to display for the item.
     * @return The newly added sub-menu
     */
    public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) {
        QuickMenuItem item = (QuickMenuItem) add(groupId, itemId, order, title);
        item.initSubMenu(mToolManager, mAnnotationPermission);
        return item.getSubMenu();
    }

    /**
     * Variation on {@link #addSubMenu(int, int, int, CharSequence)} that takes
     * a string resource identifier for the title instead of the string itself.
     *
     * @param groupId The group identifier that this item should be part of.
     *        This can also be used to define groups of items for batch state
     *        changes. Normally use {@link #NONE} if an item should not be in a group.
     * @param itemId Unique item ID. Use {@link #NONE} if you do not need a unique ID.
     * @param order The order for the item. Use {@link #NONE} if you do not care about the
     *        order. See {@link MenuItem#getOrder()}.
     * @param titleRes Resource identifier of title string.
     * @return The newly added sub-menu
     */
    public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes) {
        QuickMenuItem item = (QuickMenuItem) add(groupId, itemId, order, titleRes);
        item.initSubMenu(mToolManager, mAnnotationPermission);
        return item.getSubMenu();
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public int addIntentOptions(int groupId, int itemId, int order, ComponentName caller, Intent[] specifics, Intent intent, int flags, MenuItem[] outSpecificItems) {
        return 0;
    }
    /** @hide */
    @Override
    public SubMenu setHeaderTitle(int titleRes) {
        return this;
    }
    /** @hide */
    @Override
    public SubMenu setHeaderTitle(CharSequence title) {
        return this;
    }
    /** @hide */
    @Override
    public SubMenu setHeaderIcon(int iconRes) {
        return this;
    }
    /** @hide */
    @Override
    public SubMenu setHeaderIcon(Drawable icon) {
        return this;
    }
    /** @hide */
    @Override
    public SubMenu setHeaderView(View view) {
        return this;
    }
    /** @hide */
    @Override
    public void clearHeader() {

    }
    /** @hide */
    @Override
    public SubMenu setIcon(int iconRes) {
        return this;
    }
    /** @hide */
    @Override
    public SubMenu setIcon(Drawable icon) {
        return this;
    }

    /**
     * Gets the {@link MenuItem} that represents this submenu in the parent
     * menu.  Use this for setting additional item attributes.
     *
     * @return The {@link MenuItem} that launches the submenu when invoked.
     */
    @Override
    public MenuItem getItem() {
        return mParentItem;
    }

    /**
     * Sets parent item that represents this submenu in the parent. Use this for {@link #getItem()}
     * @param item parent menu item
     */
    public void setParentMenuItem(QuickMenuItem item) {
        mParentItem = item;
    }
}
