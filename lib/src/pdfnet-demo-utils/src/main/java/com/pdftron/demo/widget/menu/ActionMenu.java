/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pdftron.demo.widget.menu;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.v4.internal.view.SupportMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.SubMenu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ActionMenu implements SupportMenu {
    private static final int[] sCategoryToOrder = new int[]{
        1, /* No category */
        4, /* CONTAINER */
        5, /* SYSTEM */
        3, /* SECONDARY */
        2, /* ALTERNATIVE */
        0, /* SELECTED_ALTERNATIVE */
    };
    private Context mContext;
    private boolean mIsQwerty;
    private ArrayList<ActionMenuItem> mItems;

    public ActionMenu(Context context) {
        mContext = context;
        mItems = new ArrayList<>();
    }

    private static int findInsertIndex(ArrayList<ActionMenuItem> items, int ordering) {
        for (int i = items.size() - 1; i >= 0; i--) {
            ActionMenuItem item = items.get(i);
            if (item.getOrder() <= ordering) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * Returns the ordering across all items. This will grab the category from
     * the upper bits, find out how to order the category with respect to other
     * categories, and combine it with the lower bits.
     *
     * @param categoryOrder The category order for a particular item (if it has
     *                      not been or/add with a category, the default category is
     *                      assumed).
     * @return An ordering integer that can be used to order this item across
     * all the items (even from other categories).
     */
    private static int getOrdering(int categoryOrder) {
        final int index = (categoryOrder & CATEGORY_MASK) >> CATEGORY_SHIFT;

        if (index < 0 || index >= sCategoryToOrder.length) {
            throw new IllegalArgumentException("order does not contain a valid category.");
        }

        return (sCategoryToOrder[index] << CATEGORY_SHIFT) | (categoryOrder & USER_MASK);
    }

    public Context getContext() {
        return mContext;
    }

    public MenuItem add(CharSequence title) {
        return add(0, 0, 0, title);
    }

    public MenuItem add(int titleRes) {
        return add(0, 0, 0, titleRes);
    }

    public MenuItem add(int groupId, int itemId, int order, int titleRes) {
        return add(groupId, itemId, order, mContext.getResources().getString(titleRes));
    }

    public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
        ActionMenuItem item = new ActionMenuItem(this, groupId, itemId, order, title);
        mItems.add(findInsertIndex(mItems, getOrdering(order)), item);
        return item;
    }

    public MenuItem add(ActionMenuItem item) {
        mItems.add(findInsertIndex(mItems, getOrdering(item.getOrder())), item);
        return item;
    }

    public int addIntentOptions(int groupId, int itemId, int order,
                                ComponentName caller, Intent[] specifics, Intent intent, int flags,
                                MenuItem[] outSpecificItems) {
        PackageManager pm = mContext.getPackageManager();
        final List<ResolveInfo> lri =
            pm.queryIntentActivityOptions(caller, specifics, intent, 0);
        final int N = lri != null ? lri.size() : 0;

        if ((flags & FLAG_APPEND_TO_GROUP) == 0) {
            removeGroup(groupId);
        }

        for (int i = 0; i < N; i++) {
            final ResolveInfo ri = lri.get(i);
            Intent rintent = new Intent(
                                           ri.specificIndex < 0 ? intent : specifics[ri.specificIndex]);
            rintent.setComponent(new ComponentName(
                                                      ri.activityInfo.applicationInfo.packageName,
                                                      ri.activityInfo.name));
            final MenuItem item = add(groupId, itemId, order, ri.loadLabel(pm))
                                      .setIcon(ri.loadIcon(pm))
                                      .setIntent(rintent);
            if (outSpecificItems != null && ri.specificIndex >= 0) {
                outSpecificItems[ri.specificIndex] = item;
            }
        }

        return N;
    }

    public SubMenu addSubMenu(CharSequence title) {
        return addSubMenu(0, 0, 0, title);
    }

    public SubMenu addSubMenu(int titleRes) {
        return addSubMenu(0, 0, 0, mContext.getResources().getString(titleRes));
    }

    public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) {
        ActionMenuItem menuItem = (ActionMenuItem) add(groupId, itemId, order, title);
        ActionSubMenu subMenu = new ActionSubMenu(mContext, this, menuItem);
        menuItem.setSubMenu(subMenu);

        return subMenu;
    }

    public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes) {
        return addSubMenu(groupId, itemId, order, mContext.getResources().getString(titleRes));
    }

    public void clear() {
        mItems.clear();
    }

    public void close() {
    }

    private int findItemIndex(int id) {
        final ArrayList<ActionMenuItem> items = mItems;
        final int itemCount = items.size();
        for (int i = 0; i < itemCount; i++) {
            if (items.get(i).getItemId() == id) {
                return i;
            }
        }
        return -1;
    }

    public MenuItem findItem(int id) {
        final int size = size();
        for (int i = 0; i < size; i++) {
            ActionMenuItem item = mItems.get(i);
            if (item.getItemId() == id) {
                return item;
            } else if (item.hasSubMenu()) {
                // Search submenu for item
                MenuItem possibleItem = item.getSubMenu().findItem(id);
                if (possibleItem != null) {
                    return possibleItem;
                }
            }
        }
        return null;
    }

    public MenuItem getItem(int index) {
        return mItems.get(index);
    }

    public MenuItem getItem(int index, boolean visible) {
        if (!visible) {
            return getItem(index);
        } else {
            final ArrayList<ActionMenuItem> items = mItems;
            final int itemCount = items.size();
            int menuIndex = 0;

            for (int i = 0; i < itemCount; i++) {
                if (items.get(i).isVisible()) {
                    if (menuIndex == index) {
                        return items.get(i);
                    }
                    // Only advance menuPosition for visible items
                    menuIndex++;
                }
            }
            return null;
        }
    }

    public boolean hasVisibleItems() {
        final ArrayList<ActionMenuItem> items = mItems;
        final int itemCount = items.size();

        for (int i = 0; i < itemCount; i++) {
            if (items.get(i).isVisible()) {
                return true;
            }
        }

        return false;
    }

    private ActionMenuItem findItemWithShortcut(int keyCode, KeyEvent event) {
        // TODO Make this smarter.
        final boolean qwerty = mIsQwerty;
        final ArrayList<ActionMenuItem> items = mItems;
        final int itemCount = items.size();

        for (int i = 0; i < itemCount; i++) {
            ActionMenuItem item = items.get(i);
            final char shortcut = qwerty ? item.getAlphabeticShortcut() :
                                      item.getNumericShortcut();
            if (keyCode == shortcut) {
                return item;
            }
        }
        return null;
    }

    public boolean isShortcutKey(int keyCode, KeyEvent event) {
        return findItemWithShortcut(keyCode, event) != null;
    }

    public boolean performIdentifierAction(int id, int flags) {
        final ActionMenuItem item = (ActionMenuItem) findItem(id);
        return item != null && item.invoke();
    }

    public boolean performShortcut(int keyCode, KeyEvent event, int flags) {
        ActionMenuItem item = findItemWithShortcut(keyCode, event);
        return item != null && item.invoke();

    }

    public void removeGroup(int groupId) {
        final ArrayList<ActionMenuItem> items = mItems;
        int itemCount = items.size();
        int i = 0;
        while (i < itemCount) {
            if (items.get(i).getGroupId() == groupId) {
                items.remove(i);
                itemCount--;
            } else {
                i++;
            }
        }
    }

    public void removeItem(int id) {
        final int size = size();
        for (int i = 0; i < size; i++) {
            ActionMenuItem item = mItems.get(i);
            if (item.getItemId() == id) {
                mItems.remove(item);
                break;
            } else if (item.hasSubMenu()) {
                // Search submenu for item and remove if found
                MenuItem possibleItem = item.getSubMenu().findItem(id);
                if (possibleItem != null) {
                    item.getSubMenu().removeItem(id);
                    break;
                }
            }
        }
    }

    public void setGroupCheckable(int group, boolean checkable,
                                  boolean exclusive) {
        final ArrayList<ActionMenuItem> items = mItems;
        final int itemCount = items.size();

        for (int i = 0; i < itemCount; i++) {
            ActionMenuItem item = items.get(i);
            if (item.getGroupId() == group) {
                item.setCheckable(checkable);
                item.setExclusiveCheckable(exclusive);
            }
        }
    }

    public void setGroupEnabled(int group, boolean enabled) {
        final ArrayList<ActionMenuItem> items = mItems;
        final int itemCount = items.size();

        for (int i = 0; i < itemCount; i++) {
            ActionMenuItem item = items.get(i);
            if (item.getGroupId() == group) {
                item.setEnabled(enabled);
            }
        }
    }

    public void setGroupVisible(int group, boolean visible) {
        final ArrayList<ActionMenuItem> items = mItems;
        final int itemCount = items.size();

        for (int i = 0; i < itemCount; i++) {
            ActionMenuItem item = items.get(i);
            if (item.getGroupId() == group) {
                item.setVisible(visible);
            }
        }
    }

    public void setQwertyMode(boolean isQwerty) {
        mIsQwerty = isQwerty;
    }

    public int size() {
        return mItems.size();
    }

    public int getVisibleItemCount() {
        final ArrayList<ActionMenuItem> items = mItems;
        int visibleItemCount = 0;

        for (int i = 0; i < items.size(); i++) {
            ActionMenuItem item = items.get(i);
            if (item.isVisible()) {
                visibleItemCount++;
            }
        }
        return visibleItemCount;
    }

    public ActionMenu clone(int size) {
        ActionMenu out = new ActionMenu(getContext());
        out.mItems = new ArrayList<>(this.mItems.subList(0, size));
        return out;
    }

    public ActionMenu clone(int start, int end) {
        ActionMenu out = new ActionMenu(getContext());
        out.mItems = new ArrayList<>(this.mItems.subList(start, end));
        return out;
    }

    public List<ActionMenuItem> removeAfter(int index) {
        final Iterator<ActionMenuItem> iter = mItems.iterator();
        final ArrayList<ActionMenuItem> removedItems = new ArrayList<>();
        // Advance to the removal index
        for (int i = 0; i <= index && iter.hasNext(); i++) {
            iter.next();
        }
        // Remove items after removal index and add to list
        while (iter.hasNext()) {
            ActionMenuItem item = iter.next();
            iter.remove();
            removedItems.add(item);
        }
        return removedItems;
    }

    void removeInvisible() {
        Iterator<ActionMenuItem> iter = mItems.iterator();
        while (iter.hasNext()) {
            ActionMenuItem item = iter.next();
            if (!item.isVisible()) iter.remove();
        }
    }
}
