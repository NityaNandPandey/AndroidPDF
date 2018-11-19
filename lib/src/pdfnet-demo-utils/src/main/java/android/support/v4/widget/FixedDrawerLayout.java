package android.support.v4.widget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;

import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

/**
 * Fix for NullPointerException at {@link DrawerLayout#isContentView(View)}.
 * See http://stackoverflow.com/a/18107942
 *
 * Fix for broken scrim drawing when one or more content views do not fill the entire layout's size.
 * (In order for those content views to achieve that, they need to ignore the height specified in
 * {@link View#onMeasure(int, int)} since the {@link DrawerLayout} always
 * tries to measure content views at *exactly* its own size)
 * A scrim is drawn for each drawer view and is calculated to fill the space to the right/left of
 * the drawer.
 *
 * Fix for system window insets being dispatched from {@link DrawerLayout#onMeasure(int, int)}. This
 * is not an issue if the only system-window-fitting views are the drawer views (and they have
 * shallow view hierarchies). However, when the insets are dispatched further down the hierarchy (as
 * for a flexible fullscreen app), they could be dispatched every time {@link View#requestLayout()}
 * is called somewhere in the hierarchy.
 * This issue is resolved by disabling the default insets-dispatching and instead performing the
 * standard fair-dispatch to ensure that each direct child of the {@link DrawerLayout} receives the
 * same insets.
 *
 * Fix for drawers receiving the system window inset opposite to their layout gravity. For drawers
 * with scrim protection for system window insets, they may incorrectly draw scrims for the insets
 * in some cases (landscape orientation) if this is not handled.
 *
 * Fix for setting fitsSystemWindows on pre-Lollipop devices and having this view apply the insets
 * as padding. {@link #getFitsSystemWindows()} is overridden to be enabled only for Lollipop
 * devices to ensure the correct/expected behavior.
 */
public class FixedDrawerLayout extends DrawerLayout {

    public static final int DEFAULT_SCRIM_COLOR = 0x99000000;

    private int mScrimColor = DEFAULT_SCRIM_COLOR;
    private float mScrimOpacity = 0;
    private Paint mScrimPaint = new Paint();

    private boolean mDisallowIntercept;

    private final ArrayList<View> mNonDrawerViews;

    public FixedDrawerLayout(Context context) {
        this(context, null);
    }

    public FixedDrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FixedDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Disable super's (broken) scrim drawing.
        super.setScrimColor(Color.TRANSPARENT);

        if (Utils.isLollipop()) {
            // Disable super's handling of insets.
            setOnApplyWindowInsetsListener(null);
            // Remove the LAYOUT_STABLE flag - we want unstable insets to be available.
            setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        mNonDrawerViews = new ArrayList<View>();
    }

    public void setDisallowIntercept(boolean disallowIntercept) {
        if (!Utils.isLargeScreenWidth(getContext())) {
            mDisallowIntercept = false;
            return;
        }
        mDisallowIntercept = disallowIntercept;
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        // as the drawer intercepts all touches when it is opened
        // we need this to let the content beneath the drawer to be touchable
        return !mDisallowIntercept && super.onInterceptTouchEvent(ev);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS) {
            return;
        }

        // Only the views in the open drawers are focusables. Add normal child views when
        // no drawers are opened.
        final int childCount = getChildCount();
        boolean isDrawerOpen = false;
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (isDrawerView(child)) {
                if (isDrawerOpen(child)) {
                    isDrawerOpen = true;
                    child.addFocusables(views, direction, focusableMode);
                }
            } else {
                mNonDrawerViews.add(child);
            }
        }

        if (!isDrawerOpen || Utils.isLargeScreenWidth(getContext())) {
            final int nonDrawerViewsCount = mNonDrawerViews.size();
            for (int i = 0; i < nonDrawerViewsCount; ++i) {
                final View child = mNonDrawerViews.get(i);
                if (child.getVisibility() == View.VISIBLE) {
                    child.addFocusables(views, direction, focusableMode);
                }
            }
        }

        mNonDrawerViews.clear();
    }

    @Override
    public boolean getFitsSystemWindows() {
        return Utils.isLollipop();
    }

    @Override
    boolean isContentView(@Nullable View child) {
        return child != null && super.isContentView(child);
    }

    @Override
    boolean isDrawerView(@Nullable View child) {
        return child != null && super.isDrawerView(child);
    }

    /**
     * Draw a scrim over the visible area of the content views. The topmost visible drawer is
     * responsible for drawing the scrim since this ensures that the scrim will only be drawn once
     * and will not obscure other drawers.
     */
    @SuppressLint("RtlHardcoded")
    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final boolean result = super.drawChild(canvas, child, drawingTime);

        if (isDrawerView(child) && isDrawerVisible(child)) {
            // Determine the area the scrim should be drawn, if applicable.
            int scrimLeft = 0, scrimRight = getWidth();

            final int index = indexOfChild(child);
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View view = getChildAt(i);
                if (view == child || view.getVisibility() != View.VISIBLE
                        || !hasOpaqueBackground(view) || !isDrawerView(view)) {
                    // Not a different visible drawer view - skip.
                    continue;
                }

                if (isDrawerVisible(view) && i > index) {
                    // This is not the topmost visible drawer - abort.
                    return result;
                }

                if (checkDrawerViewAbsoluteGravity(view, Gravity.LEFT)) {
                    final int right = view.getRight();
                    if (right > scrimLeft) scrimLeft = right;
                } else { // Gravity.RIGHT
                    final int left = view.getLeft();
                    if (left < scrimRight) scrimRight = left;
                }
            }

            if (checkDrawerViewAbsoluteGravity(child, Gravity.LEFT)) {
                final int childRight = child.getRight();
                if (childRight > scrimLeft) scrimLeft = childRight;
            } else {  // Gravity.RIGHT
                final int childLeft = child.getLeft();
                if (childLeft < scrimRight) scrimRight = childLeft;
            }

            if (scrimLeft < scrimRight) {
                final int baseAlpha = (mScrimColor & 0xff000000) >>> 24;
                final int alpha = (int) (baseAlpha * mScrimOpacity);
                final int color = (alpha << 24) | (mScrimColor & 0x00ffffff);
                mScrimPaint.setColor(color);

                canvas.drawRect(scrimLeft, 0, scrimRight, getHeight(), mScrimPaint);
            }
        }

        return result;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        // Calculate our own scrim opacity.
        final int childCount = getChildCount();
        float scrimOpacity = 0;
        for (int i = 0; i < childCount; i++) {
            final float onscreen = ((LayoutParams) getChildAt(i).getLayoutParams()).onScreen;
            scrimOpacity = Math.max(scrimOpacity, onscreen);
        }
        mScrimOpacity = scrimOpacity;
    }

    @SuppressLint("RtlHardcoded")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child != null) {
                WindowInsets adjustedInsets = insets;
                // Remove system window insets opposite from each drawer.
                // This is to prevent drawers with scrim protection for system window insets from
                // drawing scrims for insets that they shouldn't worry about.
                if (checkDrawerViewAbsoluteGravity(child, Gravity.LEFT)) {
                    // Remove the right system window inset.
                    adjustedInsets = insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(),
                            insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
                } else if (checkDrawerViewAbsoluteGravity(child, Gravity.RIGHT)) {
                    // Remove the left system window inset.
                    adjustedInsets = insets.replaceSystemWindowInsets(0,
                            insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(),
                            insets.getSystemWindowInsetBottom());
                }
                child.dispatchApplyWindowInsets(adjustedInsets);
            }
        }
        return insets;
    }

    @Override
    public void setScrimColor(int color) {
        mScrimColor = color;

        // Disable super's (broken) scrim drawing.
        super.setScrimColor(Color.TRANSPARENT);
    }

    private static boolean hasOpaqueBackground(View v) {
        final Drawable bg = v.getBackground();
        return bg != null && bg.getOpacity() == PixelFormat.OPAQUE;
    }
}
