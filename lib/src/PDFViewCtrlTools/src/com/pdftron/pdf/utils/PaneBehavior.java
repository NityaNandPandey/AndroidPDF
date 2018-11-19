package com.pdftron.pdf.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.view.Gravity;
import android.view.View;

/**
 * A Coordinator layout behavior that adjusts child view size based on screen size and device configuration.
 */
public class PaneBehavior extends CoordinatorLayout.Behavior<View> {

    private Point mTempPoint = new Point();

    /**
     * A utility function to get the {@link PaneBehavior} associated with the {@code view}.
     *
     * @param view The {@link View} with {@link PaneBehavior}.
     * @return The {@link PaneBehavior} associated with the {@code view}.
     */
    @Nullable
    public static PaneBehavior from(View view) {
        if (view != null && view.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
            CoordinatorLayout.Behavior behavior = params.getBehavior();
            if (behavior instanceof PaneBehavior) {
                return (PaneBehavior) behavior;
            }
        }
        return null;
    }

    /**
     * Gets gravity for orientation
     * @param context The context
     * @param orientation The orientation
     * @return gravity
     */
    public static int getGravityForOrientation(Context context, int orientation) {
        int gravity = Gravity.NO_GRAVITY;
        if (Utils.isTablet(context)) {
            switch (orientation) {
                case Configuration.ORIENTATION_PORTRAIT:
                    gravity = Gravity.TOP;
                    break;
                case Configuration.ORIENTATION_LANDSCAPE:
                    gravity = GravityCompat.END;
                    break;
                case Configuration.ORIENTATION_UNDEFINED:
                default:
                    break;
            }
        }
        return gravity;
    }

    /**
     * The method is invoked when there is orientation changed
     * @param child child view
     * @param orientation orientation
     */
    public void onOrientationChanged(View child, int orientation) {
        int gravity = Gravity.NO_GRAVITY;
        if (Utils.isTablet(child.getContext())) {
            switch (orientation) {
                case Configuration.ORIENTATION_PORTRAIT:
                    gravity = Gravity.TOP;
                    break;
                case Configuration.ORIENTATION_LANDSCAPE:
                    gravity = GravityCompat.END;
                    break;
                case Configuration.ORIENTATION_UNDEFINED:
                default:
                    break;
            }
        }
        setRelativeLayoutGravity(child, gravity);
    }

    /**
     * Overload implementation of {@link CoordinatorLayout.Behavior#onMeasureChild(View, int, int, int, int)}.
     * Adjust child view size to fit in screen
     */
    @Override
    public boolean onMeasureChild(CoordinatorLayout parent, View child,
                                  int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        int childGravity = getAbsoluteLayoutGravity(child);
        if (isValidGravity(childGravity) && Utils.isTablet(child.getContext())) {
            int widthMode = View.MeasureSpec.getMode(parentWidthMeasureSpec);
            int widthSize = View.MeasureSpec.getSize(parentWidthMeasureSpec);
            int heightMode = View.MeasureSpec.getMode(parentHeightMeasureSpec);
            int heightSize = View.MeasureSpec.getSize(parentHeightMeasureSpec);

            int desiredWidth = widthSize;
            int desiredHeight = heightSize;

            Point displaySize = mTempPoint;
            Utils.getDisplaySize(child.getContext(), displaySize);

            // Take up third of display size in axis specified by gravity.
            if (Gravity.isHorizontal(childGravity)) {
                desiredWidth = displaySize.x / 3;
            } else { // isVertical
                desiredHeight = displaySize.y / 3;
            }

            // Respect original size requirements from parent.
            switch (widthMode) {
                case View.MeasureSpec.EXACTLY:
                case View.MeasureSpec.AT_MOST:
                    widthSize = Math.min(desiredWidth, widthSize);
                    break;
                case View.MeasureSpec.UNSPECIFIED:
                    widthSize = desiredWidth;
                    break;
            }
            switch (heightMode) {
                case View.MeasureSpec.EXACTLY:
                case View.MeasureSpec.AT_MOST:
                    heightSize = Math.min(desiredHeight, heightSize);
                    break;
                case View.MeasureSpec.UNSPECIFIED:
                    heightSize = desiredHeight;
                    break;
            }

            int childWidthMeasureSpec = View.MeasureSpec.makeMeasureSpec(widthSize, widthMode);
            int childHeightMeasureSpec = View.MeasureSpec.makeMeasureSpec(heightSize, heightMode);
            parent.onMeasureChild(child, childWidthMeasureSpec, widthUsed, childHeightMeasureSpec, heightUsed);
            return true; // Behavior performed measurement of child.
        }
        return false; // Parent should perform measurement of child.
    }


    private int getAbsoluteLayoutGravity(View child) {
        int gravity = Gravity.NO_GRAVITY;
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
        if (lp != null) {
            gravity = GravityCompat.getAbsoluteGravity(lp.gravity, ViewCompat.getLayoutDirection(child));
        }
        return gravity;
    }

    private void setRelativeLayoutGravity(View child, int gravity) {
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
        if (lp != null && lp.gravity != gravity) {
            lp.gravity = gravity;
            child.requestLayout();
        }
    }

    private boolean isValidGravity(int gravity) {
        if (!Gravity.isHorizontal(gravity) && !Gravity.isVertical(gravity)) {
            // Less than one gravity set.
            return false;
        } else if ((Gravity.isHorizontal(gravity) && Gravity.isVertical(gravity)) ||
            (gravity & Gravity.FILL_HORIZONTAL) == Gravity.FILL_HORIZONTAL ||
            (gravity & Gravity.FILL_VERTICAL) == Gravity.FILL_VERTICAL) {
            // More than one gravity set.
            return false;
        }
        return true;
    }
}
