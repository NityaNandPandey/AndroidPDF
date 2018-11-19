package android.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

/**
 * This class must be in the {@link android.view} package in order for the {@link View#fitSystemWindows(Rect)}
 * method to be accessible.
 */
public class DispatchFairInsetsLinearLayout extends LinearLayout {

    private boolean mConsumeInsets = true;
    private Rect mTempInsets = null;

    public DispatchFairInsetsLinearLayout(Context context) {
        this(context, null);
    }

    public DispatchFairInsetsLinearLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DispatchFairInsetsLinearLayout(Context context, AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DispatchFairInsetsLinearLayout,
            defStyleAttr, R.style.DispatchFairInsetsLinearLayout);
        try {
            mConsumeInsets = a.getBoolean(R.styleable.DispatchFairInsetsLinearLayout_consumeInsets, true);
        } finally {
            a.recycle();
        }
    }

    /**
     * Override {@link ViewGroup#fitSystemWindows(Rect)} for API versions before Lollipop
     * to simulate a fair dispatch of system window insets.
     */
    @SuppressWarnings("deprecation")
    @Override
    protected boolean fitSystemWindows(Rect insets) {
        if (Utils.isLollipop()) {
            return super.fitSystemWindows(insets);
        } else {
            if (mTempInsets == null) {
                mTempInsets = new Rect();
            }
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                // Reset the temp insets Rect to the original insets for each child because
                // we want all the children to receive the same insets.
                mTempInsets.set(insets);
                child.fitSystemWindows(mTempInsets);
            }

            // If this ViewGroup's siblings need insets, then their parent might need to
            // handle that (it could be a DispatchFairInsetsRelativeLayout too).
            return mConsumeInsets;
        }
    }

    /**
     * Dispatch the incoming system window insets to each of this ViewGroup's children. Each child
     * receives the same insets regardless of index, so for best results the children that are
     * fitting the system windows should be laid out in a stack.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child != null) {
                child.dispatchApplyWindowInsets(insets);
            }
        }
        return mConsumeInsets ? insets.consumeSystemWindowInsets() : insets;
    }

    /**
     * Sets whether the system window insets received by this View will be consume after they
     * have been dispatched to all this View's children. Consuming the insets is required to
     * preserve/simulate the standard depth-first consumption of insets.
     *
     * @param consumeInsets Whether to consume the system window insets after dispatching them
     *                      to all this View's children.
     */
    public void setConsumeInsets(boolean consumeInsets) {
        if (mConsumeInsets != consumeInsets) {
            mConsumeInsets = consumeInsets;

            // Request that the system window insets be dispatched again.
            ViewCompat.requestApplyInsets(this);
        }
    }

    /**
     * @return Whether this View will consume the system window insets after dispatching
     * them to all of its children. The default value is {@code true}.
     */
    public boolean getConsumeInsets() {
        return mConsumeInsets;
    }
}
