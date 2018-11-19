package android.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

/**
 * Dispatch the incoming system window insets to all child views, where each child view receives the
 * same insets regardless of their order.
 *
 * By default, the insets are consumed after being dispatched to the child views. This is done to
 * preserve the depth-first traversal and consumption of insets, as seen by this layout's parent.
 * In the case when the insets are desired by one or more of this layout's siblings/ancestors, the
 * inset-consumption can be controlled with {@link #setConsumeInsets(boolean)}.
 *
 * The default insets policy handler (use insets as padding) is disabled by default but can be enabled
 * by calling {@link #setOnApplyWindowInsetsListener(OnApplyWindowInsetsListener)} with a value of
 * {@code null} or a {@link android.view.View.OnApplyWindowInsetsListener} which invokes the default
 * insets policy handler for this view ({@link #onApplyWindowInsets(WindowInsets)}).
 *
 * This class must be in the {@link android.view} package in order for the {@link View#fitSystemWindows(Rect)}
 * method to be accessible.
 */
public class DispatchFairInsetsFrameLayout extends FrameLayout {

    private static final boolean CONSUME_INSETS = true;

    private boolean mConsumeInsets = true;
    private Rect mTempInsets = null;

    private OnApplyWindowInsetsListener mListener = null;

    public DispatchFairInsetsFrameLayout(Context context) {
        this(context, null);
    }

    public DispatchFairInsetsFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DispatchFairInsetsFrameLayout(Context context, AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DispatchFairInsetsFrameLayout,
                defStyleAttr, R.style.DispatchFairInsetsFrameLayout);
        try {
            mConsumeInsets = a.getBoolean(R.styleable.DispatchFairInsetsFrameLayout_consumeInsets, CONSUME_INSETS);
        } catch (Exception ignored) {
            mConsumeInsets = CONSUME_INSETS;
        } finally {
            a.recycle();
        }

        // Set a default insets listener that does not invoke the default insets policy handler.
        ViewCompat.setOnApplyWindowInsetsListener(this, new android.support.v4.view.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
                return insets;
            }
        });
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
     *
     * It is not possible to use the parent {@link ViewGroup#dispatchApplyWindowInsets(WindowInsets)}
     * to handle calling {@link View#onApplyWindowInsets(WindowInsets)} or the insets listener set
     * with {@link View#setOnApplyWindowInsetsListener(OnApplyWindowInsetsListener)}, since the
     * custom dispatching needs to be done once here.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        insets = applyWindowInsets(insets);
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
     * Emulate the behavior of {@link View#dispatchApplyWindowInsets(WindowInsets)} and send the insets
     * to the listener, if set, or to the default insets policy handler.
     *
     * @param insets Insets to apply.
     * @return The provided insets minus the insets that were consumed.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private WindowInsets applyWindowInsets(WindowInsets insets) {
        if (mListener != null) {
            return mListener.onApplyWindowInsets(this, insets);
        } else {
            return onApplyWindowInsets(insets);
        }
    }

    /**
     * Intercept the {@link android.view.View.OnApplyWindowInsetsListener listener} and prevent the
     * parent class from receiving it. This is done so that the behavior of {@link View#dispatchApplyWindowInsets(WindowInsets)}
     * can be emulated by {@link #applyWindowInsets(WindowInsets)}.
     *
     * @param listener Listener to set.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void setOnApplyWindowInsetsListener(OnApplyWindowInsetsListener listener) {
        mListener = listener;
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
