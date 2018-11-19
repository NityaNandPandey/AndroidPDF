//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pdftron.demo.R;
import com.pdftron.demo.navigation.adapter.ContentRecyclerAdapter;
import com.pdftron.demo.widget.ImageViewTopCrop;
import com.pdftron.demo.widget.menu.ActionMenu;
import com.pdftron.demo.widget.menu.ActionMenuItem;
import com.pdftron.demo.widget.menu.ActionSubMenu;
import com.pdftron.pdf.utils.ImageMemoryCache;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;

import java.util.List;

public class FileInfoDrawerFragment extends Fragment {

    public interface Listener {
        void onButtonClicked(MenuItem menuItem);

        void onNavigationButtonClicked();

        void onThumbnailClicked();
    }

    private static final int MAX_BUTTONS_PER_ROW = 4;
    private static final int MAX_NUM_BUTTON_ROWS = 2;

    private FileInfoDrawerFragment.Listener mListener;

    // UI
    private AppBarLayout mAppBarLayout;
    private TextView mTitleView;
    private ImageViewTopCrop mHeaderImageView;
    private ImageView mLockImageView;

    /**
     * Items in this menu should have their icons set via MenuItem#setIcon(int resId), so that the
     * Drawable resource Id can be given to the adapter in the JSON array.
     */
    private ActionMenu mButtonsMenu;

    private JsonArray mJsonArray;
    private JsonArray mJsonButtons;
    private JsonObject mJsonContent;
    private boolean mIsSecured = false;

    private GridLayoutManager mRecyclerLayoutManager;
    private ContentRecyclerAdapter mRecyclerAdapter;

    public FileInfoDrawerFragment() {
    }

    public static FileInfoDrawerFragment newInstance() {
        return new FileInfoDrawerFragment();
    }

    public void setListener(FileInfoDrawerFragment.Listener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mButtonsMenu = new ActionMenu(getActivity());

        mJsonArray = new JsonArray();
        mJsonButtons = new JsonArray();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final View bottomScrim = view.findViewById(R.id.header_bottom_scrim);

        mAppBarLayout = view.findViewById(R.id.app_bar_layout);
        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
                    // Collapsed
                    bottomScrim.setVisibility(View.GONE);
                } else {
                    // Expanded
                    bottomScrim.setVisibility(View.VISIBLE);
                }
            }
        });

        float shadowDx = getResources().getDimensionPixelSize(R.dimen.file_info_drawer_title_shadow_dx);
        float shadowDy = getResources().getDimensionPixelSize(R.dimen.file_info_drawer_title_shadow_dy);
        float shadowRadius = getResources().getDimensionPixelSize(R.dimen.file_info_drawer_title_shadow_radius);
        int shadowColor = getResources().getColor(R.color.gray600);

        Toolbar toolbar = view.findViewById(R.id.tool_bar);
        toolbar.setNavigationIcon(drawableShadow(R.drawable.ic_arrow_back_white_24dp, shadowRadius, shadowDx, shadowDy, shadowColor));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onNavigationButtonClicked();
                }
            }
        });

        mTitleView = toolbar.findViewById(R.id.header_title_text_view);
        mTitleView.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);

        mHeaderImageView = view.findViewById(R.id.header_image_view);
        mHeaderImageView.setClickable(true);
        mHeaderImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onThumbnailClicked();
                }
            }
        });

        // Set header image's minimum height to be status-bar_height + action-bar_height
        {
            int minHeight = 0;
            if (Utils.isLollipop()) {
                int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resourceId > 0) {
                    minHeight += getResources().getDimensionPixelSize(resourceId);
                }
            }
            TypedValue value = new TypedValue();
            if (view.getContext().getTheme().resolveAttribute(R.attr.actionBarSize, value, true)) {
                minHeight += getResources().getDimensionPixelSize(value.resourceId);
            }
            mHeaderImageView.setMinimumHeight(minHeight);
        }

        mLockImageView = view.findViewById(R.id.lock_image_view);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);

        mRecyclerLayoutManager = new GridLayoutManager(getActivity(), MAX_BUTTONS_PER_ROW);
        mRecyclerLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mRecyclerAdapter.getItemViewType(position)) {
                    case ContentRecyclerAdapter.VIEW_TYPE_BUTTON:
                        // Button items occupy only one span
                        return 1;
                    default:
                        // All other items occupy the entire width
                        return mRecyclerLayoutManager.getSpanCount();
                }

            }
        });
        recyclerView.setLayoutManager(mRecyclerLayoutManager);
        recyclerView.setItemAnimator(null);

        int accentColor = Color.BLACK;
        try {
            TypedValue value = new TypedValue();
            if (view.getContext().getTheme().resolveAttribute(R.attr.colorAccent, value, true)) {
                accentColor = value.data;
            }
        } catch (Exception e) {
            accentColor = Color.BLACK;
        }

        mRecyclerAdapter = new ContentRecyclerAdapter(getActivity(), mJsonArray,
            getResources().getColor(R.color.gray600), accentColor);
        recyclerView.setAdapter(mRecyclerAdapter);

        mRecyclerAdapter.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                switch (mRecyclerAdapter.getItemViewType(position)) {
                    case ContentRecyclerAdapter.VIEW_TYPE_BUTTON:
                        if (mListener != null) {
                            MenuItem menuItem = mButtonsMenu.findItem((int) id);
                            if (menuItem != null) {
                                if (menuItem.hasSubMenu()) {
                                    SubMenu subMenu = menuItem.getSubMenu();
                                    showPopupMenu(subMenu, view);
                                } else {
                                    mListener.onButtonClicked(menuItem);
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void showPopupMenu(Menu menu, View anchor) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), anchor);
        // Copy all visible menu items to the popup's menu
        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).isVisible()) {
                MenuItem menuItem = menu.getItem(i);
                popupMenu.getMenu().add(menuItem.getGroupId(), menuItem.getItemId(), menuItem.getOrder(), menuItem.getTitle());
            }
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (mListener != null) {
                    mListener.onButtonClicked(item);
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    public void resetScroll() {
        if (mAppBarLayout != null) {
            mAppBarLayout.setExpanded(true, false);
        }
        if (mRecyclerLayoutManager != null) {
            mRecyclerLayoutManager.scrollToPosition(0);
        }
    }

    @SuppressWarnings("unused")
    public void setDimensions(int width, int height) {
        ViewGroup.LayoutParams params = mHeaderImageView.getLayoutParams();
        params.height = height;
        mHeaderImageView.setMaxHeight(height);
    }

    public void setTitle(CharSequence title) {
        if (mTitleView != null) {
            mTitleView.setText(title);
        }
    }

    public CharSequence getTitle() {
        return (mTitleView != null) ? mTitleView.getText() : null;
    }

    @SuppressWarnings("unused")
    public void setLockVisibility(boolean visible) {
        if (mLockImageView != null) {
//            mLockImageView.setVisibility(visible ? View.VISIBLE : View.GONE);
            mLockImageView.setVisibility(View.GONE);
        }
    }

    public ImageViewTopCrop getHeaderImageView() {
        return mHeaderImageView;
    }

    public Menu getButtonsMenu() {
        return mButtonsMenu;
    }

    public void invalidateButtons() {
        // Check if an item already has a submenu (overflow)
        int overFlowIndex = -1;
        for (int i = 0; i < mButtonsMenu.size(); i++) {
            MenuItem item = mButtonsMenu.getItem(i);
            if (item.getItemId() == R.id.menu_overflow_button) {
                overFlowIndex = i;
                break;
            }
        }
        if (overFlowIndex >= 0) {
            // Move all items from overflow into the main menu (overflow will be recreated if necessary)
            ActionMenuItem overFlowItem = (ActionMenuItem) mButtonsMenu.getItem(overFlowIndex);
            ActionSubMenu overflowMenu = (ActionSubMenu) overFlowItem.getSubMenu();
            // Remove overflow item
            mButtonsMenu.removeItem(overFlowItem.getItemId());

            List<ActionMenuItem> overflowMenuItems = overflowMenu.removeAfter(-1); // Remove all items
            for (ActionMenuItem item : overflowMenuItems) {
                // Add item to main menu
                mButtonsMenu.add(item);
            }
        }

        // Check if the buttons menu needs an overflow menu
        int visibleItems = mButtonsMenu.getVisibleItemCount();
        int limit = MAX_BUTTONS_PER_ROW * MAX_NUM_BUTTON_ROWS;
        if (visibleItems > limit) {
            // Find the split index (index of item, visible or not, after the visible item at limit-1)
            int splitIndex;
            int visibleIndex = 0;
            for (splitIndex = 0; splitIndex < mButtonsMenu.size(); splitIndex++) {
                if (visibleIndex + 1 >= limit - 1) {
                    break;
                }
                if (mButtonsMenu.getItem(splitIndex).isVisible()) {
                    visibleIndex++;
                }
            }
            // Split off the extra menu items into an overflow menu
            List<ActionMenuItem> overflowMenuItems = mButtonsMenu.removeAfter(splitIndex);
            ActionSubMenu overflowMenu = (ActionSubMenu) mButtonsMenu.addSubMenu(0, R.id.menu_overflow_button, 100, null);
            Drawable drawable = getResources().getDrawable(R.drawable.ic_overflow_black_24dp);
            drawable.mutate().setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
            overflowMenu.setIcon(drawable);

            for (ActionMenuItem menuItem : overflowMenuItems) {
                overflowMenu.add(menuItem);
            }
        }

        if (mJsonButtons != null) {
            while (mJsonButtons.size() > 0) {
                mJsonButtons.remove(0);
            }
        } else {
            mJsonButtons = new JsonArray();
        }
        for (int i = 0; i < mButtonsMenu.size(); i++) {
            ActionMenuItem menuItem = (ActionMenuItem) mButtonsMenu.getItem(i);
            if (menuItem.isVisible()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.Id, menuItem.getItemId());
                jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.Icon, menuItem.getIconId());
                jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.Text, menuItem.getTitleCondensed().toString());
                jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.LongText, menuItem.getTitle().toString());
                jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsHtml, false);
                jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsHeader, false);
                jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsDivider, false);
                mJsonButtons.add(jsonObject);
            }
        }

        updateContent();
        Utils.safeNotifyDataSetChanged(mRecyclerAdapter);
    }

    public void setMainContent(CharSequence content, boolean isSecured) {
        mIsSecured = isSecured;

        if (content != null) {
            if (mJsonContent == null) {
                mJsonContent = new JsonObject();
            }
            if (content instanceof Spanned) {
                mJsonContent.addProperty(ContentRecyclerAdapter.ContentInfoKey.Text, Html.toHtml((Spanned) content));
                mJsonContent.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsHtml, true);
            } else {
                mJsonContent.addProperty(ContentRecyclerAdapter.ContentInfoKey.Text, content.toString());
                mJsonContent.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsHtml, false);
            }
            mJsonContent.addProperty(ContentRecyclerAdapter.ContentInfoKey.Id, 0); // No Id
            mJsonContent.addProperty(ContentRecyclerAdapter.ContentInfoKey.Icon, (Number) null); // No icon
            mJsonContent.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsHeader, false);
            mJsonContent.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsDivider, false);
        } else {
            mJsonContent = null;
        }

        updateContent();
    }

    /**
     * Based on: http://www.shaikhhamadali.blogspot.ro/2013/06/highlightfocusshadow-image-in-imageview.html
     */
    public Drawable drawableShadow(@DrawableRes int resId, float radius, float dx, float dy, int color) {
        // resId can be a vector drawable so we have to convert it to bitmap first if needed
        Drawable drawable = getResources().getDrawable(resId);
        Bitmap src = Utils.convDrawableToBitmap(drawable);

        // create new bitmap, which will be painted and becomes result image
        Bitmap bmOut = Bitmap.createBitmap(src.getWidth() + (int) (dx * radius),
            src.getHeight() + (int) (dy * radius), Bitmap.Config.ARGB_8888);

        // setup canvas for painting
        Canvas canvas = new Canvas(bmOut);
        // setup default color
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        // create a blur paint for capturing alpha
        Paint ptBlur = new Paint();
        ptBlur.setMaskFilter(new BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL));
        int[] offsetXY = new int[2];
        // capture alpha into a bitmap
        Bitmap bmAlpha = src.extractAlpha(ptBlur, offsetXY);

        // create a color paint
        Paint ptAlphaColor = new Paint();
        ptAlphaColor.setColor(color);

        int sc = canvas.save();
        canvas.translate(dx, dy);
        // paint color for captured alpha region (bitmap)
        canvas.drawBitmap(bmAlpha, offsetXY[0], offsetXY[1], ptAlphaColor);
        canvas.restoreToCount(sc);

        // free memory
        ImageMemoryCache.getInstance().addBitmapToReusableSet(bmAlpha);

        // paint the image source
        canvas.drawBitmap(src, 0, 0, null);

        // return out final image
        return new BitmapDrawable(getResources(), bmOut);
    }

    private void updateContent() {
        // Clear JSON array
        while (mJsonArray.size() > 0) {
            mJsonArray.remove(0);
        }
        JsonObject jsonObject;

        // Add buttons section
        for (int i = 0; i < mJsonButtons.size(); i++) {
            jsonObject = mJsonButtons.get(i).getAsJsonObject();
            mJsonArray.add(jsonObject);
        }

        if (mIsSecured) {
            jsonObject = new JsonObject();
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.Id, 0); // No Id
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.Icon, (Number) null); // No icon
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsHtml, false);
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsHeader, false);
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsDivider, true);
            mJsonArray.add(jsonObject);

            jsonObject = new JsonObject();
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.Id, 0); // No Id
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.Icon, R.drawable.thumbnail_lock);
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsHtml, false);
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsHeader, false);
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsDivider, false);
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.Text, getResources().getString(R.string.file_info_document_protected));
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.LongText, getResources().getString(R.string.file_info_document_protected));
            mJsonArray.add(jsonObject);
        }

        if (mJsonContent != null) {
            // Add divider
            jsonObject = new JsonObject();
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.Id, 0); // No Id
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.Icon, (Number) null); // No icon
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsHtml, false);
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsHeader, false);
            jsonObject.addProperty(ContentRecyclerAdapter.ContentInfoKey.IsDivider, true);
            mJsonArray.add(jsonObject);

            // Add content
            mJsonArray.add(mJsonContent);
        }
    }
}
