//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.widget.AutoScrollEditText;
import com.pdftron.pdf.widget.AutoScrollEditor;

/**
 * An EditText that can editing inline at PDFViewCtrl
 */
public class InlineEditText {

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface InlineEditTextListener {
        /**
         * The implementation should specify the position of the inline edit text.
         *
         * @return The position of the inline edit text
         */
        RectF getInlineEditTextPosition();

        /**
         * The implementation should toggle to the free text dialog.
         *
         * @param interimText The interim text
         */
        void toggleToFreeTextDialog(String interimText);
    }

    private AutoScrollEditor mEditor;
    private ImageButton mToggleButton;
    private int mToggleButtonWidth;
    private boolean mIsEditing;
    private boolean mCreatingAnnot;

    private InlineEditTextListener mListener;
    private PDFViewCtrl mPdfViewCtrl;

    private boolean mTapToCloseConfirmed = false;

    private boolean mDelayViewRemoval = false;
    private boolean mDelaySetContents = false;
    private String mDelayedContents;

    /**
     * Class constructor
     *
     * @param pdfView         The pdf view
     * @param annot           The annotation
     * @param pageNum         The annotation page number
     * @param targetPagePoint The target point page
     * @param listener        The inline edit text listener
     */
    @SuppressLint("ClickableViewAccessibility")
    public InlineEditText(PDFViewCtrl pdfView,
                          Annot annot, int pageNum,
                          com.pdftron.pdf.Point targetPagePoint,
                          @NonNull InlineEditTextListener listener) {
        mCreatingAnnot = targetPagePoint != null;
        mPdfViewCtrl = pdfView;
        mListener = listener;

        // set edit text
        mEditor = new AutoScrollEditor(mPdfViewCtrl.getContext());
        try {
            if (annot != null) {
                // existing freetext
                mEditor.setAnnot(mPdfViewCtrl, annot, pageNum);
                try {
                    mEditor.setAnnotStyle(pdfView, AnnotUtils.getAnnotStyle(annot));
                } catch (Exception ignored) {}
            } else if (targetPagePoint != null) {
                // new freetext
                Rect screenRect = Utils.getScreenRectInPageSpace(pdfView, pageNum);
                Rect pageRect = Utils.getPageRect(pdfView, pageNum);
                Rect intersectRect = new Rect();
                if (screenRect != null && pageRect != null) {
                    screenRect.normalize();
                    pageRect.normalize();
                    intersectRect.intersectRect(screenRect, pageRect);
                    intersectRect.normalize();
                    Rect freeTextRect = new Rect();

                    int pageRotation = mPdfViewCtrl.getDoc().getPage(pageNum).getRotation();
                    int viewRotation = mPdfViewCtrl.getPageRotation();
                    int annotRotation = ((pageRotation + viewRotation) % 4) * 90;

                    if (mPdfViewCtrl.getRightToLeftLanguage()) {
                        if (annotRotation == 0) {
                            freeTextRect.set(targetPagePoint.x, targetPagePoint.y, 0, 0);
                        } else if (annotRotation == 90) {
                            freeTextRect.set(targetPagePoint.x, targetPagePoint.y, pageRect.getX2(), 0);
                        } else if (annotRotation == 180) {
                            freeTextRect.set(targetPagePoint.x, targetPagePoint.y, pageRect.getX2(), pageRect.getY2());
                        } else {
                            freeTextRect.set(targetPagePoint.x, targetPagePoint.y, 0, pageRect.getY2());
                        }
                    } else {
                        if (annotRotation == 0) {
                            freeTextRect.set(targetPagePoint.x, targetPagePoint.y, pageRect.getX2(), 0);
                        } else if (annotRotation == 90) {
                            freeTextRect.set(targetPagePoint.x, targetPagePoint.y, pageRect.getX2(), pageRect.getY2());
                        } else if (annotRotation == 180) {
                            freeTextRect.set(targetPagePoint.x, targetPagePoint.y, 0, pageRect.getY2());
                        } else {
                            freeTextRect.set(targetPagePoint.x, targetPagePoint.y, 0, 0);
                        }
                    }
                    freeTextRect.normalize();
                    intersectRect.intersectRect(intersectRect, freeTextRect);
                    intersectRect.normalize();
                    mEditor.setRect(mPdfViewCtrl, intersectRect, pageNum);
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        LayoutInflater inflater = (LayoutInflater) mPdfViewCtrl.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return;
        }

        int padding = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.padding_small);
        mEditor.getEditText().setPadding(padding, 0, 0, 0);

        // set up toggle button
        View toggleButtonView = inflater.inflate(R.layout.tools_free_text_inline_toggle_button, null);
        mToggleButton = toggleButtonView.findViewById(R.id.inline_toggle_button);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.toggleToFreeTextDialog(mEditor.getEditText().getText().toString());
            }
        });
        mToggleButtonWidth = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.free_text_inline_toggle_button_width);

        // hide PDFView scroll bars
        mPdfViewCtrl.setVerticalScrollBarEnabled(false);
        mPdfViewCtrl.setHorizontalScrollBarEnabled(false);

        // set position and size of edit text in post
        // since we need the height of a line
        mEditor.getEditText().post(new Runnable() {
            @Override
            public void run() {
                RectF editBoxRect = mListener.getInlineEditTextPosition();
                setEditTextLocation(editBoxRect);
            }
        });

        mPdfViewCtrl.addView(mEditor);
        mPdfViewCtrl.addView(mToggleButton);

        // bring up keyboard
        if (mEditor.getEditText().requestFocus()) {
            // Bring up soft keyboard in case it is not shown automatically
            InputMethodManager imm = (InputMethodManager) mPdfViewCtrl.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        }
        if (mCreatingAnnot) {
            // set touch listener for edittext
            // add touch listener to edit view to determine if
            // the user has tapped text or blank space within the text box.
            // If blank space, save text to free text annotation and remove edit view
            mEditor.getEditText().setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            int y = (int) event.getY();

                            if (mEditor.getEditText() != null) {
                                int width = mEditor.getEditText().getMeasuredWidth();

                                TextPaint textPaint = mEditor.getEditText().getPaint();
                                String text = mEditor.getEditText().getText().toString();

                                StaticLayout layout = new StaticLayout(text, textPaint, width,
                                    Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                                int height = layout.getHeight();

                                // if tap is not on text
                                int buffer = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, mPdfViewCtrl.getContext().getResources().getDisplayMetrics());
                                if (y > (height + buffer)) {
                                    mTapToCloseConfirmed = true;
                                }
                            }
                            break;

                        case MotionEvent.ACTION_SCROLL:
                            mTapToCloseConfirmed = false;
                            break;

                        case MotionEvent.ACTION_UP:
                            if (mTapToCloseConfirmed) {
                                mTapToCloseConfirmed = false;

                                // give the tool manager a chance to process onUp event.
                                // This is particularly useful for AnnotEdit to set
                                // next tool mode and pop up quick menu
                                mPdfViewCtrl.getToolManager().onUp(event, PDFViewCtrl.PriorEventMode.OTHER);

                                return true;
                            }
                        default:
                            break;
                    }

                    return false;
                }
            });
        }

        // Hide any additional components
        ((ToolManager) mPdfViewCtrl.getToolManager()).onInlineFreeTextEditingStarted();

        mIsEditing = true;
    }

    /**
     * Close inline eidt text
     *
     * @param manuallyRemoveView whether remove view manually
     */
    public void close(boolean manuallyRemoveView) {
        close(manuallyRemoveView, true);
    }

    /**
     * Close inline edit text
     *
     * @param manuallyRemoveView whether remove view manually
     * @param hideKeyboard       whether hides keyboard
     */
    public void close(boolean manuallyRemoveView, boolean hideKeyboard) {
        // remove toggle button
        mPdfViewCtrl.removeView(mToggleButton);

        // Hide soft keyboard
        if (hideKeyboard) {
            InputMethodManager imm = (InputMethodManager) mPdfViewCtrl.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mPdfViewCtrl.getRootView().getWindowToken(), 0);
            }
        }

        // show toolbars
        mPdfViewCtrl.setVerticalScrollBarEnabled(false);
        mPdfViewCtrl.setHorizontalScrollBarEnabled(false);

        mIsEditing = false;

        if (manuallyRemoveView) {
            mPdfViewCtrl.removeView(mEditor);
        } else {
            mDelayViewRemoval = true;
        }
    }

    /**
     * Whether it is editing
     *
     * @return true then editing, false otherwise
     */
    public Boolean isEditing() {
        return mIsEditing;
    }

    /**
     * Whether it delays when removing this view
     *
     * @return True if it delay when removing this view; False otherwise
     */
    public boolean delayViewRemoval() {
        return mDelayViewRemoval;
    }

    /**
     * Gets contents of this edit text
     *
     * @return The contents
     */
    public String getContents() {
        return mEditor.getEditText().getText().toString();
    }

    /**
     * Sets delayed contents to the contents
     */
    public void setContents() {
        if (null != mDelayedContents) {
            mEditor.getEditText().getText().clear();
            mEditor.getEditText().append(mDelayedContents);
        }
        mDelaySetContents = false;
        mDelayedContents = null;
    }

    /**
     * Sets contents
     *
     * @param contents The contents
     */
    public void setContents(String contents) {
        mEditor.getEditText().getText().clear();
        mEditor.getEditText().append(contents);
    }

    /**
     * Whether it delays when setting contents
     *
     * @return true then delay, false otherwise
     */
    public boolean delaySetContents() {
        return mDelaySetContents;
    }

    /**
     * Sets delayed contents
     *
     * @param contents The contents
     */
    public void setDelaySetContents(String contents) {
        mDelaySetContents = true;
        mDelayedContents = contents;
    }

    /**
     * Sets text size
     *
     * @param textSize The text size
     */
    public void setTextSize(int textSize) {
        textSize *= (float) mPdfViewCtrl.getZoom();
        mEditor.getEditText().setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }

    /**
     * Sets text color
     *
     * @param textColor The text color
     */
    public void setTextColor(@ColorInt int textColor) {
        mEditor.getEditText().setTextColor(textColor);
    }

    /**
     * Gets the edit text
     *
     * @return The edit text
     */
    public AutoScrollEditText getEditText() {
        return mEditor.getEditText();
    }

    /**
     * Sets background color
     *
     * @param backgroundColor background color
     */
    public void setBackgroundColor(@ColorInt int backgroundColor) {
        mEditor.getEditText().setBackgroundColor(backgroundColor);
    }

    /**
     * Sets edit text location
     *
     * @param position The location
     */
    private void setEditTextLocation(RectF position) {
        int left = (int) position.left;
        int top = (int) position.top;
        int right = (int) position.right;
        int bottom = (int) position.bottom;

        if (mCreatingAnnot) {
            // if the text box is smaller than 1 line,
            // set the height to be equal to one line
            int lineHeight = mEditor.getEditText().getLineHeight();
            if (Math.abs(bottom - top) < (lineHeight * 1.5)) {
                top = bottom - (int) (lineHeight * 1.5);
            }

            int minWidth = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.free_text_inline_min_textbox_width);
            if (Math.abs(left - right) < minWidth) {
                left = right - minWidth;
            }
        }

        // set the location of the popup
        int screenWidth = Utils.getScreenWidth(mPdfViewCtrl.getContext());
        int editTextWidth = right - left;

        // buttons position relative to the screen
        int screenButtonPosRight = right - mPdfViewCtrl.getScrollX() + mPdfViewCtrl.getHScrollPos() + mToggleButtonWidth;
        int screenButtonPosLeft = left - mPdfViewCtrl.getScrollX() + mPdfViewCtrl.getHScrollPos() - mToggleButtonWidth;

        int screenPageLeft = mPdfViewCtrl.getHScrollPos(); // left edge of PDFViewCtrl relative to screen
        int screenPageRight = screenPageLeft + mPdfViewCtrl.getWidth(); // right edge of PDFViewCtrl relative to screen
        int buttonViewLeftPos = left - mToggleButtonWidth; // left position of button if put in PDFViewCtrl
        int buttonViewRightPos = right + mToggleButtonWidth; // right position of button if put in PDFViewCtrl

        // move button up if the line height is smaller than the toggle button height
        int buttonPosBottom = top + mToggleButtonWidth;
        if (mEditor.getEditText().getLineHeight() < mToggleButtonWidth) {
            buttonPosBottom = top + mEditor.getEditText().getLineHeight();

            int screenButtonPostTop = buttonPosBottom - mPdfViewCtrl.getScrollY() + mPdfViewCtrl.getVScrollPos() - mToggleButtonWidth;
            if (screenButtonPostTop < mPdfViewCtrl.getScrollY()) {
                buttonPosBottom = top + mToggleButtonWidth;
            }
        }

        if (mPdfViewCtrl.getRightToLeftLanguage()) {
            if (editTextWidth >= screenWidth) {
                // if the edit text occupies the entire screen, set the toggle button within the EditText
                mToggleButton.layout(left, buttonPosBottom - mToggleButtonWidth, left + mToggleButtonWidth, buttonPosBottom);
                mPdfViewCtrl.scrollBy(right - mPdfViewCtrl.getScrollX(), 0);
                // rotate the button so that it is facing away
                mToggleButton.setRotation(270);
                mToggleButton.setBackgroundResource(R.drawable.annotation_free_text_toggle_button_transparent_bgd);
            } else if (screenPageRight > screenButtonPosRight) {
                // if the button will fit to the right of the edit text on the screen (without having to
                // scroll the PDFViewCtrl)
                mToggleButton.setRotation(0);
                mToggleButton.layout(right, buttonPosBottom - mToggleButtonWidth, buttonViewRightPos, buttonPosBottom);
            } else if (screenButtonPosRight < screenPageRight) {
                // if there is room to scroll the PDFViewCtrl so that it will fit on the right, then
                // scroll the PDFViewCtrl to show the button
                mToggleButton.setRotation(0);
                mPdfViewCtrl.scrollBy(buttonViewRightPos - mPdfViewCtrl.getScrollX() - mPdfViewCtrl.getWidth(), 0);
                mToggleButton.layout(right, buttonPosBottom - mToggleButtonWidth, buttonViewRightPos, buttonPosBottom);
            } else if (screenButtonPosLeft > screenPageLeft) {
                // if there is room to the left of the edit text, place the
                // toggle button there
                mToggleButton.layout(left - mToggleButtonWidth, buttonPosBottom - mToggleButtonWidth, left, buttonPosBottom);
                // rotate the button so that it is facing away
                mToggleButton.setRotation(270);
            } else {
                // rotate the button so that it is facing away
                mToggleButton.setRotation(270);
                mToggleButton.setBackgroundResource(R.drawable.annotation_free_text_toggle_button_transparent_bgd);
                // if none of the above cases work, set it within the edit text
                mToggleButton.layout(left, buttonPosBottom - mToggleButtonWidth, left + mToggleButtonWidth, buttonPosBottom);
            }
        } else {
            if (editTextWidth >= screenWidth) {
                // if the edit text occupies the entire screen, set the toggle button within the EditText
                mToggleButton.layout(right - mToggleButtonWidth, buttonPosBottom - mToggleButtonWidth, right, buttonPosBottom);
                mPdfViewCtrl.scrollBy(left - mPdfViewCtrl.getScrollX(), 0);
                // rotate the button so that it is facing away
                mToggleButton.setRotation(0);
                mToggleButton.setBackgroundResource(R.drawable.annotation_free_text_toggle_button_transparent_bgd);
            } else if (screenPageLeft < screenButtonPosLeft) {
                // if the button will fit to the left of the edit text on the screen (without having to
                // scroll the PDFViewCtrl)
                mToggleButton.setRotation(270);
                mToggleButton.layout(buttonViewLeftPos, buttonPosBottom - mToggleButtonWidth, left, buttonPosBottom);
            } else if (screenButtonPosLeft > 0) {
                // if there is room to scroll the PDFViewCtrl so that it will fit on the left, then
                // scroll the PDFViewCtrl to show the button
                mToggleButton.setRotation(270);
                mPdfViewCtrl.scrollBy(buttonViewLeftPos - mPdfViewCtrl.getScrollX(), 0);
                mToggleButton.layout(buttonViewLeftPos, buttonPosBottom - mToggleButtonWidth, left, buttonPosBottom);
            } else if (screenButtonPosRight < screenPageRight) {
                // if there is room to the right of the edit text, place the
                // toggle button there
                mToggleButton.layout(right, buttonPosBottom - mToggleButtonWidth, right + mToggleButtonWidth, buttonPosBottom);
                // rotate the button so that it is facing away
                mToggleButton.setRotation(0);
            } else {
                // rotate the button so that it is facing away
                mToggleButton.setRotation(0);
                mToggleButton.setBackgroundResource(R.drawable.annotation_free_text_toggle_button_transparent_bgd);
                // if none of the above cases work, set it within the edit text
                mToggleButton.layout(right - mToggleButtonWidth, buttonPosBottom - mToggleButtonWidth, right, buttonPosBottom);
            }
        }
    }

    /**
     * Removes view
     */
    public void removeView() {
        if (mEditor != null) {
            mPdfViewCtrl.removeView(mEditor);
        }
    }

    /**
     * Adds text watcher listener
     *
     * @param textWatcherListener The text watcher listener
     */
    public void addTextWatcher(TextWatcher textWatcherListener) {
        mEditor.getEditText().addTextChangedListener(textWatcherListener);
    }
}
