//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.IdRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.StampManager;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.List;


/**
 * This class implements the quick menu for tools. A quick menu is a popup window
 * that contains a set of buttons. A quick menu is able to adjust its position and
 * size to fit in the screen.
 */
public class QuickMenu extends RelativeLayout implements View.OnClickListener, MenuItem.OnMenuItemClickListener {

    private static final String TAG = QuickMenu.class.getName();
    private static final int PADDING_OFFSET_DP = 40;
    /**
     * @hide
     */
    protected final PDFViewCtrl mParentView;
    /**
     * @hide
     */
    protected View mAnchor;
    /**
     * @hide
     */
    protected Annot mAnnot;
    /**
     * @hide
     */
    protected ArrayList<QuickMenuItem> mFirstMenuItems;
    /**
     * @hide
     */
    protected ArrayList<QuickMenuItem> mSecondMenuItems;
    /**
     * @hide
     */
    protected ArrayList<QuickMenuItem> mOverflowMenuItems;
    /**
     * @hide
     */
    protected RelativeLayout mMenuView;
    /**
     * @hide
     */
    protected LinearLayout mMainLayout;
    /**
     * @hide
     */
    protected LinearLayout mOverflowLayout;
    /**
     * @hide
     */
    protected ToolManager mToolManager;
    /**
     * @hide
     */
    protected View mBgView;
    // Animations
    /**
     * @hide
     */
    protected Animation mFadeInAnim;
    /**
     * @hide
     */
    protected ScaleAnimation mMain2OverflowAnim;
    /**
     * @hide
     */
    protected ScaleAnimation mOverflow2MainAnim;
    // Variables to handle forwarding of events to the PDFViewCtrl.
    private MotionEvent mLastEventForwarded = null;
    private int mPopupWidth;
    private int mPopupHeight;
    private int mMainWidth;
    private int mMainHeight;
    private int mOverflowWidth;
    private int mOverflowHeight;
    private QuickMenuItem mSelectedMenuItem;
    private int mPaddingOffsetPx;
    private AppCompatImageButton mBackButton;
    private boolean mAlignTop = false;
    private OnDismissListener mListener;
    private int mDividerVisibility = VISIBLE;
    private QuickMenuItem mOverflowMenuItem;
    private QuickMenuBuilder mMenu;
    private int mAnalyticsQuickMenuType = AnalyticsHandlerAdapter.QUICK_MENU_TYPE_ANNOTATION_SELECT;
    private boolean mActionHasPerformed;
    private ToolManager.ToolModeBase mToolMode;

    /**
     * Class constructor
     *
     * @param parent               The parent view, {@link PDFViewCtrl}
     * @param annotationPermission Whether the annotation has the permission
     * @param toolMode             The tool mode. It is only used for Analytics Handler.
     */
    public QuickMenu(
        PDFViewCtrl parent,
        boolean annotationPermission,
        @Nullable ToolManager.ToolModeBase toolMode
    ) {

        super(parent.getContext());
        Context context = parent.getContext();
        mParentView = parent;
        mFirstMenuItems = new ArrayList<>();
        mSecondMenuItems = new ArrayList<>();
        mOverflowMenuItems = new ArrayList<>();
        mToolManager = (ToolManager) parent.getToolManager();
        mMenu = new QuickMenuBuilder(context, mToolManager, annotationPermission);
        mToolMode = toolMode;
        initView();

    }

    /**
     * Class constructor
     *
     * @param parent parent view, {@link PDFViewCtrl}
     */
    public QuickMenu(PDFViewCtrl parent, boolean annotationPermission) {
        this(parent, annotationPermission, null);
    }

    /**
     * Class constructor
     *
     * @param parent parent view, {@link PDFViewCtrl}
     */
    public QuickMenu(
        PDFViewCtrl parent,
        @Nullable ToolManager.ToolModeBase toolMode
    ) {

        this(parent, true, toolMode);

    }

    /**
     * Class constructor
     *
     * @param parent parent view, {@link PDFViewCtrl}
     */
    public QuickMenu(PDFViewCtrl parent) {
        this(parent, true);
    }

    /**
     * Class Constructor
     *
     * @param parent parent view, {@link PDFViewCtrl}
     * @param anchor anchor location
     */
    public QuickMenu(PDFViewCtrl parent, RectF anchor) {
        this(parent);
        setAnchorRect(anchor);
    }

    /**
     * Class Constructor
     *
     * @param parent      parent view, {@link PDFViewCtrl}
     * @param anchor      anchor location
     * @param menu_titles a list of menu titles to show in quick menu
     */
    // Explicitly pass in anchor locations because it seems that ICS has a bug when mAnchor.getLocationOnScreen() is called.
    public QuickMenu(PDFViewCtrl parent, RectF anchor,
                     List<QuickMenuItem> menu_titles) {
        this(parent, anchor);

        addMenuEntries(menu_titles);
    }

    /**
     * Class Constructor
     *
     * @param parent      parent view, {@link PDFViewCtrl}
     * @param anchor      anchor location
     * @param menu_titles a list of menu titles to show in quick menu
     * @param maxRowSize  maximum row size for first row and second row.
     */
    public QuickMenu(PDFViewCtrl parent, RectF anchor,
                     List<QuickMenuItem> menu_titles, int maxRowSize) {
        this(parent, anchor);
        addMenuEntries(menu_titles, maxRowSize);
    }

    /**
     * Initialize menu entries by inflated menu
     */
    public void initMenuEntries() {
        addMenuEntries(mMenu.getMenuItems());
    }

    /**
     * Inflate and initialize a menu resource into this QuickMenu.  This is equivalent to
     * calling {@code quickMenu.inflate(menuRes); quickMenu.initMenuEntries()}.
     *
     * @param menuRes Menu resource to inflate
     */
    public void initMenuEntries(@MenuRes int menuRes) {
        inflate(menuRes);
        initMenuEntries();
    }

    /**
     * Inflate and initialize a menu resource into this QuickMenu.  This is equivalent to
     * calling {@code quickMenu.inflate(menuRes); quickMenu.initMenuEntries()}.
     *
     * @param menuRes    Menu resource to inflate
     * @param maxRowSize maximum number of items in each row
     */
    public void initMenuEntries(@MenuRes int menuRes, int maxRowSize) {
        inflate(menuRes);
        addMenuEntries(mMenu.getMenuItems(), maxRowSize);
    }

    /**
     * add menu entries to quick menu. It will initialize menu views at the end
     *
     * @param menuEntries menu entries to be added to quick menu based on display mode.
     */
    public void addMenuEntries(List<QuickMenuItem> menuEntries) {
        // put menu titles into different list
        for (QuickMenuItem entry : menuEntries) {
            switch (entry.getDisplayMode()) {
                case QuickMenuItem.FIRST_ROW_MENU:
                    if (entry.getOrder() > -1) {
                        mFirstMenuItems.add(entry.getOrder(), entry);
                    } else {
                        mFirstMenuItems.add(entry);
                    }
                    break;
                case QuickMenuItem.SECOND_ROW_MENU:
                    if (entry.getOrder() > -1) {
                        mSecondMenuItems.add(entry.getOrder(), entry);
                    } else {
                        mSecondMenuItems.add(entry);
                    }
                    break;
                default:
                    if (entry.getOrder() > -1) {
                        mOverflowMenuItems.add(entry.getOrder(), entry);
                    } else {
                        mOverflowMenuItems.add(entry);
                    }
            }
            entry.setOnMenuItemClickListener(this);
        }
        initMenuView();
    }

    /**
     * add menu entries to quick menu, if exceed max row size, menu entries
     * will auto added to next row/ overflow list. It will initialize menu views at the end
     *
     * @param menuEntries menu entries
     * @param maxRowSize  maximum row size
     */
    public void addMenuEntries(List<QuickMenuItem> menuEntries, int maxRowSize) {
        addMenuEntries(menuEntries);
        mFirstMenuItems.remove(mOverflowMenuItem);
        if (maxRowSize > 0 && mSecondMenuItems.size() > maxRowSize) {
            int diffSize = mSecondMenuItems.size() - maxRowSize;
            List<QuickMenuItem> list = mSecondMenuItems.subList(mSecondMenuItems.size() - diffSize, mSecondMenuItems.size());
            mFirstMenuItems.addAll(new ArrayList<>(list));
            mSecondMenuItems.removeAll(new ArrayList<>(list));

        }

        int firstRowSize = maxRowSize - 1;  // leave one place for overflow menu item
        if (firstRowSize > 0 && mFirstMenuItems.size() > firstRowSize) {
            int diffSize = mFirstMenuItems.size() - firstRowSize;
            List<QuickMenuItem> list = mFirstMenuItems.subList(mFirstMenuItems.size() - diffSize, mFirstMenuItems.size());
            mOverflowMenuItems.addAll(new ArrayList<>(list));
            mFirstMenuItems.removeAll(new ArrayList<>(list));
        }

        initMenuView();
    }

    /**
     * set pointer icon clickable
     */
    public void setPointerIconClickable() {
        if (isShowing() && Utils.isNougat()) {
            mParentView.getToolManager().onChangePointerIcon(PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HAND));
        }
    }

    /**
     * called right after constructor is called
     */
    @SuppressLint("InflateParams")
    protected void initView() {
        setVisibility(GONE);
        if (Utils.isLollipop()) {
            setElevation(2);
        }
        LayoutInflater inflater = (LayoutInflater) mParentView.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return;
        }
        mMenuView = (RelativeLayout) inflater.inflate(R.layout.quick_menu_layout, null);
        addView(mMenuView);
        mMainLayout = mMenuView.findViewById(R.id.main_group);
        mOverflowLayout = mMenuView.findViewById(R.id.overflow_group);
        mBgView = mMenuView.findViewById(R.id.bg_view);
        mBackButton = mMenuView.findViewById(R.id.back_btn);
        mBackButton.setColorFilter(Utils.getForegroundColor(getContext()));
        if (Utils.isNougat()) {
            mBackButton.setOnGenericMotionListener(new OnGenericMotionListener() {
                @Override
                public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                    setPointerIconClickable();
                    return true;
                }
            });
        }
        // get the offset at the top from the status bar
        mPaddingOffsetPx = (int) Utils.convDp2Pix(getContext(), PADDING_OFFSET_DP);

        initBackground();
    }

    /**
     * called after menu items has been set
     */
    protected void initMenuView() {
        // add overflow button if overflow buttons exists
        if (mOverflowMenuItem == null) {
            mOverflowMenuItem = new QuickMenuItem(getContext(), R.id.qm_overflow, QuickMenuItem.FIRST_ROW_MENU);
            mOverflowMenuItem.setIcon(R.drawable.ic_overflow_black_24dp);
            mOverflowMenuItem.setTitle("Overflow");
            mOverflowMenuItem.setOnMenuItemClickListener(this);
        }
        if (!mOverflowMenuItems.isEmpty() && !mFirstMenuItems.contains(mOverflowMenuItem)) {
            mFirstMenuItems.add(mOverflowMenuItem);
        }

        initMainMenuView();

        initOverflowMenuView();

        measureLayoutSize();
    }

    /**
     * called in {@link #initMenuView()}
     */
    protected void initMainMenuView() {
        // show divider if second row exists
        if (mSecondMenuItems == null || mSecondMenuItems.isEmpty()) {
            setDividerVisibility(GONE);
        } else {
            setDividerVisibility(mDividerVisibility);
        }

        LinearLayout mFirstRowView = mMenuView.findViewById(R.id.group1);
        LinearLayout mSecondRowView = mMenuView.findViewById(R.id.group2);

        // add first row menus
        addMenuButtons(mFirstMenuItems, mFirstRowView);

        // add second row menus
        addMenuButtons(mSecondMenuItems, mSecondRowView);
    }

    /**
     * set the divider in main menu layout visibility
     *
     * @param visibility divider visibility,
     *                   One of {@link #VISIBLE}, {@link #INVISIBLE}, or {@link #GONE}.
     */
    public void setDividerVisibility(int visibility) {
        View divider = mMenuView.findViewById(R.id.divider);
        divider.setVisibility(visibility);
        mDividerVisibility = visibility;
    }

    /**
     * called in {@link #initMenuView()}
     */
    protected void initOverflowMenuView() {
        mOverflowLayout.removeAllViews();
        for (QuickMenuItem entry : mOverflowMenuItems) {
            if (!entry.isVisible()) {
                continue;
            }
            View view = entry.createTextButton();
            view.setEnabled(entry.isEnabled());
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.width = (int) Utils.convDp2Pix(getContext(), 120);
            view.setLayoutParams(lp);
            if (Utils.isNougat()) {
                view.setOnGenericMotionListener(new OnGenericMotionListener() {
                    @Override
                    public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                        setPointerIconClickable();
                        return true;
                    }
                });
            }
            mOverflowLayout.addView(view);
//            view.setOnClickListener(this);
        }
        mOverflowLayout.addView(mBackButton);
        mOverflowLayout.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        mOverflowWidth = mOverflowLayout.getMeasuredWidth();
        mOverflowHeight = mOverflowLayout.getMeasuredHeight();
        mBackButton.setOnClickListener(this);
        mBackButton.setBackground(getContext().getResources().getDrawable(R.drawable.btn_borderless));
    }

    /**
     * initialize background if api is under 21
     */
    protected void initBackground() {
        if (!Utils.isLollipop()) {
            mBgView.setBackground(getContext().getResources().getDrawable(R.drawable.quickmenu_bg_rect_old_api));
        }

        Drawable drawable = mBgView.getBackground();
        if (drawable instanceof GradientDrawable) {
            GradientDrawable bgShape = (GradientDrawable) mBgView.getBackground();
            bgShape.setColor(Utils.getBackgroundColor(getContext()));
        }
    }

    /**
     * Initialize animation
     */
    protected void initAnimation() {
        mFadeInAnim = AnimationUtils.loadAnimation(getContext(), R.anim.controls_quickmenu_show);
        float main2OverflowWidthScale = (float) mOverflowWidth / mMainWidth;
        float main2OverflowHeightScale = (float) mOverflowHeight / mMainHeight;
        float pivotX = 1;
        float pivotY = 1;
        if (Utils.isRtlLayout(getContext())) {
            pivotX = 0;
        }
        if (mAlignTop) {
            pivotY = 0;
        }
        mMain2OverflowAnim = new ScaleAnimation(1, main2OverflowWidthScale, 1, main2OverflowHeightScale,
            Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);
        mOverflow2MainAnim = new ScaleAnimation(main2OverflowWidthScale, 1, main2OverflowHeightScale, 1,
            Animation.RELATIVE_TO_SELF, pivotX, Animation.RELATIVE_TO_SELF, pivotY);

        mMain2OverflowAnim.setDuration(100);
        mOverflow2MainAnim.setDuration(100);
        mMain2OverflowAnim.setInterpolator(new AccelerateInterpolator());
        mMain2OverflowAnim.setInterpolator(new AccelerateInterpolator());
        mMain2OverflowAnim.setFillEnabled(true);
        mMain2OverflowAnim.setFillAfter(true);
        mOverflow2MainAnim.setFillEnabled(true);
        mOverflow2MainAnim.setFillAfter(true);
        mMain2OverflowAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                mOverflowLayout.setVisibility(VISIBLE);
                mOverflowLayout.startAnimation(mFadeInAnim);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mOverflow2MainAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mMainLayout.setVisibility(VISIBLE);
                mMainLayout.startAnimation(mFadeInAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    /**
     * measure main menu and overflow menu layout size,
     * called in {@link #initMenuView()} after {@link #initOverflowMenuView()}
     */
    protected void measureLayoutSize() {
        mMainLayout.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        mMainWidth = mMainLayout.getMeasuredWidth();
        mMainHeight = mMainLayout.getMeasuredHeight();

        mPopupWidth = mMainWidth + mPaddingOffsetPx;
        mPopupHeight = mMainHeight;
        if (mOverflowHeight > mPopupHeight) {
            mPopupHeight = mOverflowHeight;
        }
        mPopupHeight = mPopupHeight + mPaddingOffsetPx;

        mMenuView.setLayoutParams(new LayoutParams(mPopupWidth, mPopupHeight));
        mMenuView.setClickable(false);

        RelativeLayout.LayoutParams layoutParams = new LayoutParams(mPopupWidth, mPopupHeight);
        setLayoutParams(layoutParams);
    }

    /**
     * calculate location of quick menu
     */
    public void requestLocation() {
        if (mAnchor == null) {
            return;
        }
        // the margin of quick menu
        int margin = (int) Utils.convDp2Pix(getContext(), 20);
        int subHeight = mPopupHeight - mMainHeight;

        // calculate offsetX
        int offsetX = mAnchor.getRight() / 2 + mAnchor.getLeft() / 2 - mPopupWidth / 2;
        if (offsetX < 0) {
            offsetX = 0;
        } else if (offsetX + mPopupWidth > mParentView.getWidth()) {
            offsetX = mParentView.getWidth() - mPopupWidth;
        }

        // calculate offsetY
        // state 1, quick menu shows at top of the annotation and main view align bottom of the quick menu
        int offsetY = mAnchor.getTop() - mPopupHeight;
        mAlignTop = false;

        // state 2: if offsetY is higher than the top of parent view, make main view align top
        if (offsetY + margin < 0) {
            offsetY = mAnchor.getTop() - mMainHeight - 2 * margin;
            mAlignTop = true;
        }

        // state 3: if offsetY still higher than top of parent view, make the quick menu show at the bottom of the annotation
        if (offsetY + margin < 0) {
            offsetY = mAnchor.getBottom();
            mAlignTop = true;
        }

        // state 4: if quick menu bottom lower than the parent view bottom, then make main view align bottom
        // note: margin / 2 to leave some space for shadows
        if (offsetY + mPopupHeight - margin / 2 > mParentView.getHeight()) {
            offsetY = mAnchor.getBottom() - subHeight + margin;
            mAlignTop = false;
        }

        // state 5: if quick menu top higher than parent view, then move it down a bit
        if (offsetY + margin < 0) {
            offsetY = -margin;
        }

        // state 6: if quick menu bottom still lower than the parent view bottom,
        // then it doesn't fit all of above cases, the main view will definitely exceed the boundary of parent view.
        // make main view align top and show at the top of the parent view
        if (offsetY + mPopupHeight - margin / 2 > mParentView.getHeight()) {
            mAlignTop = true;
            offsetY = mAnchor.getTop() - mMainHeight - 2 * margin;
            // move it up a bit if quick menu bottom exceed boundary
            if (offsetY + mPopupHeight - margin / 2 > mParentView.getHeight()) {
                int diff = offsetY + mPopupHeight - margin / 2 - mParentView.getHeight();
                offsetY -= diff;
            }

            // move it down a bit if quick menu top exceed boundary
            if (offsetY + margin < 0) {
                offsetY = -margin;
            }
        }

        // change the position to the page position
        measure(MeasureSpec.makeMeasureSpec(mPopupWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(mPopupHeight, MeasureSpec.EXACTLY));

        offsetX = offsetX + mParentView.getScrollX();
        offsetY = offsetY + mParentView.getScrollY();
        layout(offsetX, offsetY, offsetX + mPopupWidth, offsetY + mPopupHeight);

        if (Utils.isRtlLayout(getContext())) {
            mOverflowLayout.layout(margin, margin, mOverflowWidth + margin, mOverflowHeight + margin);
        }

        if (mAlignTop) {
            int marginBottom = (mMainHeight + margin < mPopupHeight - margin) ? mMainHeight + margin : mPopupHeight - margin;
            mBgView.layout(margin, margin, mMainWidth + margin, marginBottom);
            // reorder overflow buttons if it is align top
            if (!mOverflowMenuItems.isEmpty()) {
                ArrayList<View> childs = new ArrayList<>();
                for (int i = 0; i < mOverflowLayout.getChildCount(); i++) {
                    childs.add(mOverflowLayout.getChildAt(i));
                }
                mOverflowLayout.removeAllViews();
                for (View view : childs) {
                    mOverflowLayout.addView(view);
                    if (Utils.isNougat()) {
                        view.setOnGenericMotionListener(new OnGenericMotionListener() {
                            @Override
                            public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                                setPointerIconClickable();
                                return true;
                            }
                        });
                    }

                    if (view == mBackButton) {
                        view.layout(view.getLeft(), 0, view.getRight(), mBackButton.getHeight());
                    } else {
                        view.layout(view.getLeft(), view.getTop() + view.getHeight(), view.getRight(), view.getBottom() + view.getHeight());
                    }
                }
            }
        } else {
            int marginTop = (mPopupHeight - mMainHeight - margin > margin) ? mPopupHeight - mMainHeight - margin : margin;

            mMainLayout.layout(margin, marginTop, mMainWidth + margin, mPopupHeight - margin);
            mBgView.layout(margin, marginTop, mMainWidth + margin, mPopupHeight - margin);
        }
        initAnimation();
    }

    /**
     * Show quick menu with quick menu type for analytics
     *
     * @param quickMenuType quick menu type for analytics
     */
    public void show(@AnalyticsHandlerAdapter.QuickMenuType int quickMenuType) {
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(AnalyticsHandlerAdapter.EVENT_QUICK_MENU_OPEN, AnalyticsParam.quickMenuOpenParam(quickMenuType));
        mAnalyticsQuickMenuType = quickMenuType;
        show();
    }

    /**
     * Show quick menu
     */
    public void show() {
        // don't show quick menu if the menu list is empty
        if (mFirstMenuItems.isEmpty() && mSecondMenuItems.isEmpty() && mOverflowMenuItems.isEmpty()) {
            return;
        }
        post(new Runnable() {
            @Override
            public void run() {
                requestLocation();
            }
        });
        mParentView.addView(this);
        mMenuView.requestFocus();
        setVisibility(VISIBLE);
        bringToFront();
        if (mToolManager != null) {
            mToolManager.onQuickMenuShown();
        }
    }

    /**
     * dismiss quick menu
     */
    public void dismiss() {
        if (mLastEventForwarded != null) {
            MotionEvent upEvent = MotionEvent.obtain(mLastEventForwarded);
            upEvent.setAction(MotionEvent.ACTION_UP);
            mLastEventForwarded = null;
            mToolManager.getPDFViewCtrl().onTouchEvent(upEvent);
        }
        setVisibility(GONE);
        mMenuView.setVisibility(GONE);
        if (mListener != null) {
            mListener.onDismiss();
        }
        mParentView.removeView(this);
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(AnalyticsHandlerAdapter.EVENT_QUICK_MENU_OPEN);
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_QUICK_MENU_CLOSE,
            AnalyticsParam.quickMenuDismissParam(mAnalyticsQuickMenuType, mActionHasPerformed));

        if (!mActionHasPerformed) {
            if (mSelectedMenuItem != null && mSelectedMenuItem.hasSubMenu()) {
                int id = mSelectedMenuItem.getItemId();
                if (id == R.id.qm_floating_sig) { // signature_dismiss
                    AnalyticsHandlerAdapter.getInstance()
                        .sendEvent(AnalyticsHandlerAdapter.EVENT_QUICK_MENU_ACTION,
                            AnalyticsParam.quickMenuParam(mAnalyticsQuickMenuType,
                                "signature",
                                AnalyticsHandlerAdapter.getInstance().getQuickMenuNextAction(AnalyticsHandlerAdapter.NEXT_ACTION_SIGNATURE_DISMISS)));
                } else if (id == R.id.qm_shape) { // shapes_dismiss
                    AnalyticsHandlerAdapter.getInstance()
                        .sendEvent(AnalyticsHandlerAdapter.EVENT_QUICK_MENU_ACTION,
                            AnalyticsParam.quickMenuParam(mAnalyticsQuickMenuType,
                                "shapes",
                                AnalyticsHandlerAdapter.getInstance().getQuickMenuNextAction(AnalyticsHandlerAdapter.NEXT_ACTION_SHAPES_DISMISS)));
                } else if (id == R.id.qm_form) { // forms_dismiss
                    AnalyticsHandlerAdapter.getInstance()
                        .sendEvent(AnalyticsHandlerAdapter.EVENT_QUICK_MENU_ACTION,
                            AnalyticsParam.quickMenuParam(mAnalyticsQuickMenuType,
                                "forms",
                                AnalyticsHandlerAdapter.getInstance().getQuickMenuNextAction(AnalyticsHandlerAdapter.NEXT_ACTION_FORM_DISMISS)));
                }
            }
        }
    }

    /**
     * set on dismiss listener.
     *
     * @param listener listener get called when quick menu is dismissed
     */
    public void setOnDismissListener(OnDismissListener listener) {
        mListener = listener;
    }

    /**
     * The overload implementation of {@link View.OnClickListener#onClick(View)}.
     */
    @Override
    public void onClick(View v) {
        if (v == mBackButton) {
            hideOverflowAnim();
        }
    }

    /**
     * get menu selected type
     *
     * @return selected menu item type
     */
    public QuickMenuItem getSelectedMenuItem() {
        return mSelectedMenuItem;
    }

    /**
     * add menu items to layouts
     *
     * @param menuItems the menu items that will be added
     * @param rowView   the view that the menu items will be added to
     */
    protected final void addMenuButtons(ArrayList<QuickMenuItem> menuItems, ViewGroup rowView) {
        rowView.removeAllViews();
        boolean containsFlatten = false;
        for (QuickMenuItem entry : menuItems) {
            if (!entry.isVisible()) {
                continue;
            }
            View view;
            if (entry.getItemId() == R.id.qm_flatten) {
                containsFlatten = true;
            }
            if (entry.hasIcon() && !containsFlatten) {
                view = entry.createImageButton();
            } else {
                view = entry.createTextButton();
            }
            view.setEnabled(entry.isEnabled());
            if (entry.getOrder() >= 0) {
                rowView.addView(view, entry.getOrder());
            } else {
                rowView.addView(view);
            }
            if (Utils.isNougat()) {
                view.setOnGenericMotionListener(new OnGenericMotionListener() {
                    @Override
                    public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                        setPointerIconClickable();
                        return true;
                    }
                });
            }
        }
    }

    /**
     * check if the quick menu is visible
     *
     * @return true if visible
     */
    public boolean isShowing() {
        return getVisibility() == VISIBLE;
    }

    /**
     * show overflow menu and hide main menu
     */
    public void showOverflowAnim() {
        Animation fadeOutAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.controls_quickmenu_hide);
        fadeOutAnimation.setAnimationListener(new FadeOutAnimListener(mMainLayout, mBgView, mMain2OverflowAnim));
        mMainLayout.startAnimation(fadeOutAnimation);
    }

    /**
     * hide overflow menu and show main menu
     */
    public void hideOverflowAnim() {
        Animation fadeOutAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.controls_quickmenu_hide);
        fadeOutAnimation.setAnimationListener(new FadeOutAnimListener(mOverflowLayout, mBgView, mOverflow2MainAnim));
        mOverflowLayout.startAnimation(fadeOutAnimation);
    }

    /**
     * set location by given anchor view
     *
     * @param anchor target anchor view
     */
    public void setAnchor(@NonNull View anchor) {
        mAnchor = anchor;
    }

    /**
     * set location by given anchor rectangle
     *
     * @param anchor_rect target anchor rectangle
     */
    public void setAnchorRect(@NonNull RectF anchor_rect) {
        if (mAnchor == null) {
            mAnchor = new View(getContext());
        }
        mAnchor.setVisibility(INVISIBLE);
        mAnchor.layout((int) anchor_rect.left, (int) anchor_rect.top, (int) anchor_rect.right, (int) anchor_rect.bottom);
    }

    /**
     * If the quick menu is associated with an annotation, we store it for later use
     * @param annot the annotation
     */
    public void setAnnot(@Nullable Annot annot) {
        mAnnot = annot;
    }

    /**
     * Return the associated annotation
     * @return the annotation
     */
    public Annot getAnnot() {
        return mAnnot;
    }

    /**
     * get menu entries in first row
     *
     * @return first row menu items
     */
    @SuppressWarnings("unused")
    public List<QuickMenuItem> getFirstRowMenuItems() {
        return mFirstMenuItems;
    }

    /**
     * get menu items in second row
     *
     * @return second row menu items
     */
    @SuppressWarnings("unused")
    public List<QuickMenuItem> getSecondRowMenuItems() {
        return mSecondMenuItems;
    }

    /**
     * get menu items in overflow list
     *
     * @return menu items in overflow list
     */
    @SuppressWarnings("unused")
    public List<QuickMenuItem> getOverflowMenuItems() {
        return mOverflowMenuItems;
    }

    /**
     * get menu background view
     *
     * @return menu background view
     */
    @SuppressWarnings("unused")
    public View getMenuBackground() {
        return mBgView;
    }

    /**
     * get back button in menu
     *
     * @return back button
     */
    @SuppressWarnings("unused")
    public AppCompatImageButton getBackButton() {
        return mBackButton;
    }

    /**
     * Returns the {@link QuickMenuBuilder} associated with this quick menu. Populate the
     * returned Menu with items before calling {@link #show()}.
     *
     * @return the {@link QuickMenuBuilder} associated with this quick menu
     * @see #show()
     * @see #getMenuInflater()
     */
    @NonNull
    public Menu getMenu() {
        return mMenu;
    }

    /**
     * @return a {@link MenuInflater} that can be used to inflate menu items
     * from XML into the menu returned by {@link #getMenu()}
     * @see #getMenu()
     */
    @NonNull
    public MenuInflater getMenuInflater() {
        return new MenuInflater(getContext());
    }

    /**
     * Inflate a menu resource into this QuickMenu. This is equivalent to
     * calling {@code quickMenu.getMenuInflater().inflate(menuRes, quickMenu.getMenu())}.
     *
     * @param menuRes Menu resource to inflate
     */
    public void inflate(@MenuRes int menuRes) {
        getMenuInflater().inflate(menuRes, mMenu);
    }

    /**
     * Find quick menu item in this QuickMenu. This is equaivalent to
     * calling {@code quickMenu.getMenu().findItem(int)}.
     *
     * @param itemId menu item id
     */
    public QuickMenuItem findMenuItem(@IdRes int itemId) {
        return (QuickMenuItem) getMenu().findItem(itemId);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        mSelectedMenuItem = (QuickMenuItem) item;

        if (item.hasSubMenu()) {
            mFirstMenuItems.clear();
            mSecondMenuItems.clear();
            mOverflowMenuItems.clear();
            QuickMenuBuilder subMenu = (QuickMenuBuilder) item.getSubMenu();
            addMenuEntries(subMenu.getMenuItems());
            mMainLayout.setVisibility(VISIBLE);
            mOverflowLayout.setVisibility(INVISIBLE);
            mParentView.removeView(this);
            show();
            return true;
        }

        if (!item.equals(mOverflowMenuItem)) {
            String action = null;
            int nextAction = 0;
            int id = item.getItemId();
            if (id == R.id.qm_floating_sig && !StampManager.getInstance().hasDefaultSignature(getContext())) {
                action = "signature";
                nextAction = AnalyticsHandlerAdapter.NEXT_ACTION_SIGNATURE_FIRST_TIME;
            } else if (id == R.id.qm_new_signature) {
                action = "signature";
                nextAction = AnalyticsHandlerAdapter.NEXT_ACTION_SIGNATURE_NEW;
            } else if (id == R.id.qm_use_saved_sig) {
                action = "signature";
                nextAction = AnalyticsHandlerAdapter.NEXT_ACTION_SIGNATURE_USE_SAVED;
            } else if (id == R.id.qm_arrow) {
                action = "shapes";
                nextAction = AnalyticsHandlerAdapter.NEXT_ACTION_SHAPE_ARROW;
            } else if (id == R.id.qm_polyline) {
                action = "shapes";
                nextAction = AnalyticsHandlerAdapter.NEXT_ACTION_SHAPE_POLYLINE;
            } else if (id == R.id.qm_oval) {
                action = "shapes";
                nextAction = AnalyticsHandlerAdapter.NEXT_ACTION_SHAPE_OVAL;
            } else if (id == R.id.qm_polygon) {
                action = "shapes";
                nextAction = AnalyticsHandlerAdapter.NEXT_ACTION_SHAPE_POLYLINE;
            } else if (id == R.id.qm_cloud) {
                action = "shapes";
                nextAction = AnalyticsHandlerAdapter.NEXT_ACTION_SHAPE_CLOUD;
            } else if (id == R.id.qm_field_signed) {
                action = "forms";
                nextAction = AnalyticsHandlerAdapter.NEXT_ACTION_SHAPE_CLOUD;
            }

            if (action == null) {
                AnalyticsHandlerAdapter.getInstance()
                    .sendEvent(AnalyticsHandlerAdapter.EVENT_QUICK_MENU_ACTION,
                        AnalyticsParam.quickMenuParam(mAnalyticsQuickMenuType,
                            AnalyticsHandlerAdapter.getInstance().getQuickMenuAction(id, mToolMode)));
            } else {
                AnalyticsHandlerAdapter.getInstance()
                    .sendEvent(AnalyticsHandlerAdapter.EVENT_QUICK_MENU_ACTION,
                        AnalyticsParam.quickMenuParam(mAnalyticsQuickMenuType, action,
                            AnalyticsHandlerAdapter.getInstance().getQuickMenuNextAction(nextAction)));
            }

            mActionHasPerformed = true;
            dismiss();
            return true;
        }
        showOverflowAnim();
        return false;
    }

    /**
     * Callback interface to be invoked when the quick menu is dismissed.
     * It is used for receiving updates when dismiss quick menu.
     */
    public interface OnDismissListener {
        /**
         * Called when the quick menu has been dismissed.
         */
        void onDismiss();
    }

    /**
     * fade out animation listener
     * when animation ends, set the fadeout view to be invisible,
     * and start the next coming view animation
     */
    public class FadeOutAnimListener implements Animation.AnimationListener {
        View mNextView;
        Animation mNextAnimation;
        View mView;

        /**
         * Class Constructor
         *
         * @param view          the current view that has this animation listener
         * @param nextView      the next view going to start animation when this animation ends
         * @param nextAnimation the next animation that the nextView going to start
         */
        FadeOutAnimListener(View view, View nextView, Animation nextAnimation) {
            mView = view;
            mNextView = nextView;
            mNextAnimation = nextAnimation;
        }

        /**
         * Overload implementation of {@link Animation.AnimationListener#onAnimationStart(Animation)}
         *
         * @param animation animation
         */
        @Override
        public void onAnimationStart(Animation animation) {
        }

        /**
         * Overload implementation of {@link Animation.AnimationListener#onAnimationEnd(Animation)}
         * Sets the fade out animation view to {@link View#INVISIBLE}, and set next view to start next animation
         *
         * @param animation animation
         */
        @Override
        public void onAnimationEnd(Animation animation) {
            mView.setVisibility(INVISIBLE);
            mNextView.startAnimation(mNextAnimation);
        }

        /**
         * Overload implementation of {@link Animation.AnimationListener#onAnimationRepeat(Animation)}
         *
         * @param animation animation
         */
        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }
}
