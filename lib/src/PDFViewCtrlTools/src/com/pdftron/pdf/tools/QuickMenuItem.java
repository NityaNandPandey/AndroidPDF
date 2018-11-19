package com.pdftron.pdf.tools;
//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.TooltipCompat;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.pdftron.pdf.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * the {@link MenuItem} inside {@link QuickMenu}
 */
@SuppressWarnings("WeakerAccess")
public class QuickMenuItem implements MenuItem, View.OnClickListener{

    /**
     * Menu display mode indicates where the menu item should be shown.
     * Mast be one of: {@link #FIRST_ROW_MENU}, {@link #SECOND_ROW_MENU}, {@link #OVERFLOW_ROW_MENU},
     */
    @IntDef({FIRST_ROW_MENU, SECOND_ROW_MENU, OVERFLOW_ROW_MENU})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MenuDisplayMode {}

    /**
     * Menu display mode: display in first row
     */
    public static final int FIRST_ROW_MENU = 0;

    /**
     * Menu display mode: display in second row
     */
    public static final int SECOND_ROW_MENU = 1;
    /**
     * Menu display mode: display in overflow list
     */
    public static final int OVERFLOW_ROW_MENU = 2;

    /**
     * Specifies menu item position in menu,
     * if it is -1, then it is placed in front of the menu list.
     */
    public static final int ORDER_START = -1;


    private String mTitle;   // menu display text
    private Drawable mIcon;
    private int mDisPlayMode; // where menu display
    private int mIconColor = -1; // menu color
    private float mOpacity; // menu opacity
    private boolean mHasCustomOpacity;
    private Context mContext;
    private int mId = -1;
    private int mOrder = 0;
    private ColorStateList mIconTintList;
    private PorterDuff.Mode mIconTintMode;
    private CharSequence mTitleCondensed;
    private boolean mIsVisible = true;
    private boolean mIsEnable = true;
    private QuickMenuBuilder mSubMenu = null;
    private View mButton = null;
    private OnMenuItemClickListener mMenuItemClickListener = null;
    /**
     * Class Constructor
     *  @param text : Menu display text
     * @param displayMode : whether menu displayed in first row, second row, or hide in overflow
     * @param color : menu icon color
     * @param opacity : menu icon opacity
     */
    public QuickMenuItem(Context context, String text, @MenuDisplayMode int displayMode, @ColorInt int color, float opacity) {
        this(context, text, displayMode);
        this.mIconColor = color;
        this.mOpacity = opacity;
        this.mHasCustomOpacity = true;
    }
    /**
     * Class Constructor
     *
     * @param itemId: Menu item id
     * @param displayMode: whether menu displayed in first row, second row, or hide in overflow
     */
    public QuickMenuItem(Context context, @IdRes int itemId, @MenuDisplayMode int displayMode) {
        this(context, "", displayMode);
        setItemId(itemId);
    }

    /**
     * Class Constructor
     *
     * @param text: Menu display text
     * @param displayMode: whether menu displayed in first row, second row, or hide in overflow
     */
    public QuickMenuItem(Context context, String text, @MenuDisplayMode int displayMode) {
        mContext = context;
        this.mTitle = text;
        this.mDisPlayMode = displayMode;
    }

    /**
     * Class Constructor
     *
     * @param id: Menu item id
     */
    public QuickMenuItem(Context context, @IdRes int id) {
        this(context, "");
        setItemId(id);
    }

    /**
     * Class Constructor
     *
     * @param text: Menu display text
     */
    public QuickMenuItem(Context context, String text) {
        this(context, text, FIRST_ROW_MENU);
    }

    /**
     * Creates image button that contains with corresponding icon, color, opacity
     * @return image button
     */
    public AppCompatImageButton createImageButton() {
        AppCompatImageButton imageButton = new AppCompatImageButton(mContext);
        loadAttributes(imageButton);
        if (hasIcon()) {
            imageButton.setImageDrawable(getIcon());
        }
        if (hasColor()) {
            imageButton.setColorFilter(mIconColor);
        }
        if (hasOpacity()) {
            imageButton.setAlpha(mOpacity);
        }
        if (!Utils.isNullOrEmpty(mTitle)) {
            TooltipCompat.setTooltipText(imageButton, mTitle);
            imageButton.setContentDescription(mTitle);
        }
        imageButton.setLayoutParams(
            new RelativeLayout.LayoutParams(
                mContext.getResources().getDimensionPixelSize(R.dimen.quick_menu_button_size),
                mContext.getResources().getDimensionPixelSize(R.dimen.quick_menu_button_size)));
        imageButton.setPadding(mContext.getResources().getDimensionPixelSize(R.dimen.quick_menu_button_padding),
            mContext.getResources().getDimensionPixelSize(R.dimen.quick_menu_button_padding),
            mContext.getResources().getDimensionPixelSize(R.dimen.quick_menu_button_padding),
            mContext.getResources().getDimensionPixelSize(R.dimen.quick_menu_button_padding));
        imageButton.setOnClickListener(this);
        mButton = imageButton;
        return imageButton;
    }

    /**
     * Creates text button that contains with corresponding icon, color, opacity
     * @return text button
     */
    public Button createTextButton() {
        Button button =  new AppCompatButton(mContext);
        loadAttributes(button);
        if(hasOpacity()) {
            button.setAlpha(mOpacity);
        }
        if (!Utils.isNullOrEmpty(mTitle)) {
            button.setText(mTitle.toUpperCase());
        }
        button.setTextAppearance(mContext, android.R.style.TextAppearance_DeviceDefault);
        button.setLayoutParams(
            new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, (int) Utils.convDp2Pix(mContext, 48)));
        button.setPadding(mContext.getResources().getDimensionPixelSize(R.dimen.quick_menu_button_padding),
            0,
            mContext.getResources().getDimensionPixelSize(R.dimen.quick_menu_button_padding),
            0);
//            setAlpha(0.87f);
        int gravity = Utils.isRtlLayout(mContext) ? Gravity.END : Gravity.START;
        button.setGravity(Gravity.CENTER_VERTICAL | gravity);
        button.setSingleLine(true);
        button.setOnClickListener(this);
        mButton = button;
        return button;
    }

    private void loadAttributes(View v) {
        TypedArray a = mContext.obtainStyledAttributes(null,R.styleable.QuickMenuItem, R.attr.quick_menu_item, R.style.QuickMenuButton);
        try {
            // tint
            int tintColor = a.getColor(R.styleable.QuickMenuItem_android_tint, Utils.getThemeAttrColor(mContext, android.R.attr.textColorPrimary));
            float opacity = 1;
            if (v instanceof ImageButton) {
                ((ImageButton) v).setColorFilter(tintColor);
                opacity = a.getFloat(R.styleable.QuickMenuItem_icon_alpha, 0.54f);
            } else if(v instanceof Button) {
                ((Button) v).setTextColor(tintColor);
                opacity = a.getFloat(R.styleable.QuickMenuItem_text_alpha, 0.87f);
            }
            v.setAlpha(opacity);

            // background
            try {
                Drawable background = a.getDrawable(R.styleable.QuickMenuItem_android_background);
                if (background != null) {
                    v.setBackground(background);
                }
            } catch (UnsupportedOperationException e) {
                v.setBackground(mContext.getResources().getDrawable(R.drawable.btn_borderless));
            }
        }finally {
            a.recycle();
        }
    }


    /**
     * Called when button is clicked
     * @param v view
     */
    @Override
    public void onClick(View v) {
        if (mMenuItemClickListener != null) {
            mMenuItemClickListener.onMenuItemClick(this);
        }
    }

    /**
     * Check if this menu item has icon resource id
     * @return true if menu item has icon resource id
     */
    public boolean hasIcon() {
        return mIcon != null;
    }

    /**
     * get displayed text of menu item.
     * long pressing menu item will show text.
     * @return menu item text
     */
    public String getText() {
        return this.mTitle;
    }

    /**
     * get where the menu item is displayed in quick menu
     * @return display mode
     */
    public @MenuDisplayMode int getDisplayMode() {
        return mDisPlayMode;
    }

    /**
     * get menu icon color
     * @return menu icon color
     */
    public @ColorInt int getIconColor() {
        return mIconColor;
    }

    /**
     * set menu icon color
     * @param iconColor menu icon color
     */
    public void setColor(@ColorInt int iconColor) {
        this.mIconColor = iconColor;
        if (mButton != null) {
            if (mButton instanceof ImageButton) {
                ((ImageButton) mButton).setColorFilter(iconColor);
            } else if(mButton instanceof Button) {
                ((Button) mButton).setTextColor(iconColor);
            }
        }
    }

    /**
     * check if menu has icon color
     * @return true if menu item has icon color, false otherwise
     */
    public boolean hasColor() {
        return mIconColor != -1;
    }

    /**
     * get menu item icon opacity
     * @return menu item icon opacity
     */
    public float getOpacity() {
        return mOpacity;
    }

    /**
     * set menu item icon opacity
     * @param opacity menu item icon opacity
     */
    public void setOpacity(float opacity) {
        this.mOpacity = opacity;
        this.mHasCustomOpacity = true;
        if (mButton != null) {
            mButton.setAlpha(opacity);
        }
    }

    /**
     * Check if menu item has set opacity
     * @return true if menu item has opacity, false otherwise
     */
    public boolean hasOpacity() {
        return mHasCustomOpacity;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof QuickMenuItem) {
            if(((QuickMenuItem) obj).getItemId() == mId) {
                return true;
            }
        }
        return false;
    }

    /**
     * get item id
     * @return menu item id
     */
    @Override
    public int getItemId() {
        return mId;
    }

    /**
     * Sets menu item id
     * @param id menu item id
     * @return menu item
     */
    public MenuItem setItemId(@IdRes int id) {
        mId = id;
        return this;
    }

    /**
     * get order in quick menu list.
     * @return quick menu order
     */
    @Override
    public int getOrder() {
        if (mOrder == 0) {
            return -1;
        } else if (mOrder == ORDER_START) {
            return 0;
        } else {
            return mOrder;
        }
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public int getGroupId() {
        return 0;
    }


    /**
     * Sets order to show in quick menu
     * @param order order
     */
    public MenuItem setOrder(int order) {
        mOrder = order;
        return this;
    }

    /**
     * Sets text of current quick menu item
     * @param title quick menu item display text
     * @return quick menu item
     */
    @Override
    public MenuItem setTitle(CharSequence title) {
        mTitle = title.toString();
        if (mButton != null && mButton instanceof Button) {
            ((Button) mButton).setText(mTitle);
        }
        return this;
    }

    /**
     * Sets text of current quick menu item
     * @param title quick menu item display text resource
     * @return quick menu item
     */
    @Override
    public MenuItem setTitle(int title) {
        mTitle = mContext.getString(title);
        if (mButton != null && mButton instanceof Button) {
            ((Button) mButton).setText(mTitle);
        }
        return this;
    }

    /**
     * Gets text of current quick menu item
     * @return quick menu item display text
     */
    @Override
    public CharSequence getTitle() {
        return getText();
    }

    /**
     * Change the condensed title associated with this item. The condensed
     * title is used in situations where the normal title may be too long to
     * be displayed.
     *
     * @param title The new text to be displayed as the condensed title.
     * @return This Item so additional setters can be called.
     */
    @Override
    public MenuItem setTitleCondensed(CharSequence title) {
        mTitleCondensed = title;
        return this;
    }
    /**
     * Retrieve the current condensed title of the item. If a condensed
     * title was never set, it will return the normal title.
     *
     * @return The condensed title, if it exists.
     *         Otherwise the normal title.
     */
    @Override
    public CharSequence getTitleCondensed() {
        return mTitleCondensed;
    }

    /**
     * Change the icon associated with this item. This icon will not always be
     * shown, so the title should be sufficient in describing this item. See
     * {@link Menu} for the menu types that support icons.
     *
     * @param icon The new icon (as a Drawable) to be displayed.
     * @return This Item so additional setters can be called.
     */
    @Override
    public MenuItem setIcon(Drawable icon) {
        mIcon = icon;
        if (mIconTintList != null) {
            setIconTintList(mIconTintList);
        }
        if (mIconTintList != null) {
            setIconTintMode(mIconTintMode);
        }
        if (mButton != null && mButton instanceof ImageButton) {
            ((ImageButton) mButton).setImageDrawable(mIcon);
        }
        return this;
    }

    /**
     * Change the icon associated with this item. This icon will not always be
     * shown, so the title should be sufficient in describing this item. See
     * {@link Menu} for the menu types that support icons.
     * <p>
     * This method will set the resource ID of the icon which will be used to
     * lazily get the Drawable when this item is being shown.
     *
     * @param iconRes The new icon (as a resource ID) to be displayed.
     * @return This Item so additional setters can be called.
     */
    @Override
    public MenuItem setIcon(int iconRes) {
        if (iconRes != 0) {
            setIcon(Utils.getDrawable(mContext, iconRes));
        }
        return this;
    }

    /**
     * Applies a tint to this item's icon. Does not modify the
     * current tint mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link #setIcon(Drawable)} or {@link #setIcon(int)} will
     * automatically mutate the icon and apply the specified tint and
     * tint mode using
     * {@link Drawable#setTintList(ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @see #getIconTintList()
     * @see Drawable#setTintList(ColorStateList)
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public MenuItem setIconTintList(@Nullable ColorStateList tint) {
        if (mIcon != null) {
            mIcon.mutate();
            mIcon.setTintList(tint);
        }
        mIconTintList = tint;
        return this;
    }

    /**
     * @return the tint applied to this item's icon
     * @see #setIconTintList(ColorStateList)
     */
    @Nullable
    @Override
    public ColorStateList getIconTintList() {
        return mIconTintList;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setIconTintList(ColorStateList)} to this item's icon. The default mode is
     * {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @see #setIconTintList(ColorStateList)
     * @see Drawable#setTintMode(PorterDuff.Mode)
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public MenuItem setIconTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mIcon != null) {
            mIcon.mutate();
            mIcon.setTintMode(tintMode);
        }
        mIconTintMode = tintMode;
        return this;
    }

    /**
     * Returns the blending mode used to apply the tint to this item's icon, if specified.
     *
     * @return the blending mode used to apply the tint to this item's icon
     * @see #setIconTintMode(PorterDuff.Mode)
     */
    @Nullable
    @Override
    public PorterDuff.Mode getIconTintMode() {
        return mIconTintMode;
    }

    /**
     * Returns title of quick menu item
     * @return title
     */
    @Override
    public String toString() {
        return mTitle;
    }

    /**
     * Returns the icon for this item as a Drawable (getting it from resources if it hasn't been
     * loaded before). Note that if you call {@link #setIconTintList(ColorStateList)} or
     * {@link #setIconTintMode(PorterDuff.Mode)} on this item, and you use a custom menu presenter
     * in your application, you have to apply the tinting explicitly on the {@link Drawable}
     * returned by this method.
     *
     * @return The icon as a Drawable.
     */
    @Override
    public Drawable getIcon() {
        return mIcon;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public MenuItem setIntent(Intent intent) {
        return this;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public Intent getIntent() {
        return null;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public MenuItem setShortcut(char numericChar, char alphaChar) {
        return this;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public MenuItem setNumericShortcut(char numericChar) {
        return this;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public char getNumericShortcut() {
        return 0;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public MenuItem setAlphabeticShortcut(char alphaChar) {
        return this;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public char getAlphabeticShortcut() {
        return 0;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public MenuItem setCheckable(boolean checkable) {
        return this;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public boolean isCheckable() {
        return false;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public MenuItem setChecked(boolean checked) {
        return this;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public boolean isChecked() {
        return false;
    }

    /**
     * Sets the visibility of the menu item. Even if a menu item is not visible,
     * it may still be invoked via its shortcut (to completely disable an item,
     * set it to invisible and {@link #setEnabled(boolean) disabled}).
     *
     * @param visible If true then the item will be visible; if false it is
     *        hidden.
     * @return This Item so additional setters can be called.
     */

    @Override
    public MenuItem setVisible(boolean visible) {
        mIsVisible = visible;
        return this;
    }

    /**
     * Return the visibility of the menu item.
     *
     * @return If true the item is visible; else it is hidden.
     */
    @Override
    public boolean isVisible() {
        return mIsVisible;
    }

    /**
     * Return the enabled state of the menu item.
     *
     * @return If true the item is enabled and hence invokable; else it is not.
     */
    @Override
    public MenuItem setEnabled(boolean enabled) {
        mIsEnable = enabled;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return mIsEnable;
    }

    /**
     * Initialize sub menu
     * @param toolManager tool manager
     * @param annotationPermission whether annotation has permission
     * @return this quick menu item
     */
    public QuickMenuItem initSubMenu(ToolManager toolManager, boolean annotationPermission) {
        mSubMenu = new QuickMenuBuilder(mContext, toolManager, annotationPermission);
        mSubMenu.setParentMenuItem(this);
        return this;
    }

    /**
     * Check whether this item has an associated sub-menu.  I.e. it is a
     * sub-menu of another menu.
     *
     * @return If true this item has a menu; else it is a
     *         normal item.
     */
    @Override
    public boolean hasSubMenu() {
        return mSubMenu != null;
    }

    /**
     * Gets the sub-menu to be invoked when this item is selected, if it has
     * one. See {@link #hasSubMenu()}.
     *
     * @return The associated menu if there is one, else null
     */
    @Override
    public SubMenu getSubMenu() {
        return mSubMenu;
    }

    /**
     * Sets a custom listener for invocation of this menu item. In most
     * situations, it is more efficient and easier to use
     *
     * @param menuItemClickListener The object to receive invokations.
     * @return This Item so additional setters can be called.
     */
    @Override
    public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
        mMenuItemClickListener = menuItemClickListener;
        return this;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public ContextMenu.ContextMenuInfo getMenuInfo() {
        return null;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public void setShowAsAction(int actionEnum) {

    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public MenuItem setShowAsActionFlags(int actionEnum) {
        return this;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public MenuItem setActionView(View view) {
        return this;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public MenuItem setActionView(int resId) {
        return this;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public View getActionView() {
        return null;
    }

    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public MenuItem setActionProvider(ActionProvider actionProvider) {
        return null;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public ActionProvider getActionProvider() {
        return null;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public boolean expandActionView() {
        return false;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public boolean collapseActionView() {
        return false;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public boolean isActionViewExpanded() {
        return false;
    }

    /** @hide */
    @Override
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
        return null;
    }
}
