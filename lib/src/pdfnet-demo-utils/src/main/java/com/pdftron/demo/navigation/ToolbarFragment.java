package com.pdftron.demo.navigation;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.pdftron.demo.R;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.ForegroundLayout;

/**
 * A base fragment class used to provide basic support for an embedded {@link Toolbar} widget.
 * <p>
 * It handles setting a Toolbar with id {@link R.id#fragment_toolbar} as the containing
 * {@link FragmentActivity}'s {@link android.support.v7.app.ActionBar action bar}, applying a fallback shadow
 * to the main fragment content view with id {@link R.id#fragment_content} for pre-Lollipop devices,
 * and positioning the Toolbar correctly when the {@link View#SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN} flag
 * is used with {@link View#setSystemUiVisibility(int)}.
 * </p>
 */
public abstract class ToolbarFragment extends Fragment {

    private Delegate mDelegate;

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDelegate = Delegate.create(getActivity());
    }

    @CallSuper
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mDelegate != null) {
            mDelegate.onViewCreated(view, savedInstanceState);
        }
    }

    @CallSuper
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mDelegate != null) {
            mDelegate.onActivityCreated(savedInstanceState);
        }
    }

    public void updateToolbarDrawable() {
        if (mDelegate != null) {
            mDelegate.updateToolbarDrawable();
        }
    }

    /**
     * This class defines a delegate which can be used to add inset-aware toolbar functionality to
     * any {@link Fragment fragment} inside a {@link FragmentActivity}. It can be used when it is
     * not possible to derive directly from {@link ToolbarFragment}, for instance when creating
     * a {@link android.support.v4.app.DialogFragment} subclass.
     */
    public static abstract class Delegate {

        public static Delegate create(FragmentActivity activity) {
            if (Utils.isLollipop()) {
                return new ToolbarFragment.LollipopDelegate(activity);
            } else {
                return new ToolbarFragment.KitKatDelegate(activity);
            }
        }

        public abstract void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState);

        public abstract void onActivityCreated(@Nullable Bundle savedInstanceState);

        public abstract FragmentActivity getActivity();

        public abstract void updateToolbarDrawable();
    }

    private static abstract class BaseDelegate extends Delegate {
        FragmentActivity mActivity;

        LinearLayout mAppBarLayout;
        Toolbar mToolbar;
        View mContent;
        Drawable mToolbarNavDrawable;

        BaseDelegate(FragmentActivity activity) {
            mActivity = activity;
        }

        @Override
        public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
            if (view != null) {
                mAppBarLayout = (LinearLayout) view.findViewById(R.id.fragment_app_bar);
                mToolbar = (Toolbar) view.findViewById(R.id.fragment_toolbar);
                mToolbarNavDrawable = mToolbar.getNavigationIcon();
                mContent = view.findViewById(R.id.fragment_content);

                if (Utils.isLargeScreenWidth(mActivity)) {
                    mToolbar.setNavigationIcon(null);
                }
            }
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            Activity activity = getActivity();
            if (activity instanceof AppCompatActivity) {
                ((AppCompatActivity) activity).setSupportActionBar(mToolbar);
            }
        }

        @Override
        public FragmentActivity getActivity() {
            return mActivity;
        }

        @Override
        public void updateToolbarDrawable() {
            if (mToolbar != null && mActivity != null) {
                if (Utils.isLargeScreenWidth(mActivity)) {
                    mToolbar.setNavigationIcon(null);
                } else {
                    mToolbar.setNavigationIcon(mToolbarNavDrawable);
                }
            }
        }
    }

    private static class KitKatDelegate extends BaseDelegate {
        KitKatDelegate(FragmentActivity activity) {
            super(activity);
        }

        @Override
        public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            if ((mAppBarLayout != null || mToolbar != null) && mContent != null) {
                // Try to apply a toolbar shadow as a foreground drawable.
                if (mContent instanceof FrameLayout) {
                    ((FrameLayout) mContent).setForeground(ContextCompat.getDrawable(getActivity(), R.drawable.controls_toolbar_dropshadow));
                    ((FrameLayout) mContent).setForegroundGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL);
                } else if (mContent instanceof ForegroundLayout) {
                    ((ForegroundLayout) mContent).setForeground(ContextCompat.getDrawable(getActivity(), R.drawable.controls_toolbar_dropshadow));
                    ((ForegroundLayout) mContent).setForegroundGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL);
                }
            }
        }
    }

    private static class LollipopDelegate extends BaseDelegate implements
            View.OnAttachStateChangeListener {
        LollipopDelegate(FragmentActivity activity) {
            super(activity);
        }

        @Override
        public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            if (view != null) {
                // Request a new dispatch of system window insets when the inset view is
                // attached to the view hierarchy.
                if (ViewCompat.isAttachedToWindow(view)) {
                    ViewCompat.requestApplyInsets(view);
                } else {
                    view.addOnAttachStateChangeListener(this);
                }
            }
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            // Request a new dispatch of system window insets.
            ViewCompat.requestApplyInsets(view);
            view.removeOnAttachStateChangeListener(this);
        }

        @Override
        public void onViewDetachedFromWindow(View view) {}
    }
}
