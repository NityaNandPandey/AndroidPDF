//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * A helper class for keyboard shortcuts
 */
@SuppressWarnings("UnusedParameters")
public class ShortcutHelper {

    private static boolean sEnabled = false;

    /**
     * Enables/Disables keyboard shortcuts
     *
     * @param enabled True if enabled
     */
    public static void enable(boolean enabled) {
        sEnabled = enabled;
    }

    /**
     * @return True if keyboard shortcuts is handled
     */
    public static boolean isEnabled() {
        return sEnabled;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers highlight annotation
     */
    public static boolean isHighlightAnnot(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_H;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_HIGHLIGHT);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers underline annotation
     */
    public static boolean isUnderlineAnnot(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_U;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_UNDERLINE);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers strikethrough/crossing-out annotation
     */
    public static boolean isStrikethroughAnnot(int keyCode, KeyEvent event) { // crossing out
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && (keyCode == KeyEvent.KEYCODE_K || keyCode == KeyEvent.KEYCODE_X);
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_STRIKETHROUGH);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers squiggly annotation
     */
    public static boolean isSquigglyAnnot(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_G;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_SQUIGGLY);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers text box annotation
     */
    public static boolean isTextboxAnnot(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_T;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_TEXTBOX);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers comment/note annotation
     */
    public static boolean isCommentAnnot(int keyCode, KeyEvent event) { // note
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && (keyCode == KeyEvent.KEYCODE_C || keyCode == KeyEvent.KEYCODE_N);
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_COMMENT);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers rectangle annotation
     */
    public static boolean isRectangleAnnot(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_R;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_RECTANGLE);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers oval annotation
     */
    public static boolean isOvalAnnot(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_O;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_OVAL);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers draw/pen/freehand annotation
     */
    public static boolean isDrawAnnot(int keyCode, KeyEvent event) { // pen/freehand
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && (keyCode == KeyEvent.KEYCODE_D || keyCode == KeyEvent.KEYCODE_P || keyCode == KeyEvent.KEYCODE_F);
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_DRAW);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers eraser annotation
     */
    public static boolean isEraserAnnot(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_E;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_ERASER);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers line annotation
     */
    public static boolean isLineAnnot(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_L;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_LINE);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers arrow annotation
     */
    public static boolean isArrowAnnot(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_A;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_ARROW);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers signature annotation
     */
    public static boolean isSignatureAnnot(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_S;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_SIGNATURE);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers image annotation
     */
    public static boolean isImageAnnot(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_I;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_IMAGE);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers hyper link annotation
     */
    public static boolean isHyperlinkAnnot(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_K;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ANNOTATION_HYPERLINK);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers deleting the selected annotation
     */
    public static boolean isDeleteAnnot(int keyCode, KeyEvent event) {
        boolean b = sEnabled && keyCode == KeyEvent.KEYCODE_FORWARD_DEL || keyCode == KeyEvent.KEYCODE_DEL;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_DELETE_ANNOTATION);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers going to the next document
     */
    public static boolean isGotoNextDoc(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_TAB;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_GO_TO_NEXT_DOC);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers going to the previous document
     */
    public static boolean isGotoPreviousDoc(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_TAB;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_GO_TO_PREV_DOC);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers finding text
     */
    public static boolean isFind(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_F;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_FIND);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers going to the next search result
     */
    public static boolean isGotoNextSearch(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_ENTER;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_GO_TO_NEXT_SEARCH);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers going to the previous search result
     */
    public static boolean isGotoPreviousSearch(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isCtrlPressed() && !event.isAltPressed() && event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_ENTER;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_GO_TO_PREV_SEARCH);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers undo
     */
    public static boolean isUndo(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_Z;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_UNDO);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers redo
     */
    public static boolean isRedo(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed()
                && ((!event.isShiftPressed() && keyCode == KeyEvent.KEYCODE_Y)
                || (event.isShiftPressed() && keyCode == KeyEvent.KEYCODE_Z));
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_REDO);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers copy
     */
    public static boolean isCopy(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_C;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_COPY);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers cut
     */
    public static boolean isCut(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_X;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_CUT);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers paste
     */
    public static boolean isPaste(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_V;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_PASTE);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers print
     */
    public static boolean isPrint(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_P;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_PRINT);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers adding a user bookmark
     */
    public static boolean isAddBookmark(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_D;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ADD_BOOKMARK);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers scrolling to the left
     */
    public static boolean isScrollToLeft(int keyCode, KeyEvent event) {
        return sEnabled && keyCode == KeyEvent.KEYCODE_DPAD_LEFT;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers scrolling to the right
     */
    public static boolean isScrollToRight(int keyCode, KeyEvent event) {
        return sEnabled && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers scrolling to the up
     */
    public static boolean isScrollToUp(int keyCode, KeyEvent event) {
        return sEnabled && keyCode == KeyEvent.KEYCODE_DPAD_UP;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers scrolling to the down
     */
    public static boolean isScrollToDown(int keyCode, KeyEvent event) {
        return sEnabled && keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers jumping towards the up in the page
     */
    public static boolean isPageUp(int keyCode, KeyEvent event) {
        boolean b = sEnabled && keyCode == KeyEvent.KEYCODE_PAGE_UP;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_PAGE_UP);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers jumping towards the down in the page
     */
    public static boolean isPageDown(int keyCode, KeyEvent event) {
        boolean b = sEnabled && (keyCode == KeyEvent.KEYCODE_PAGE_DOWN || keyCode == KeyEvent.KEYCODE_SPACE);
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_PAGE_DOWN);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers going to the first page
     */
    public static boolean isGotoFirstPage(int keyCode, KeyEvent event) {
        boolean b = sEnabled && keyCode == KeyEvent.KEYCODE_MOVE_HOME;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_GO_TO_FIRST_PAGE);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers going to the last page
     */
    public static boolean isGotoLastPage(int keyCode, KeyEvent event) {
        boolean b = sEnabled && keyCode == KeyEvent.KEYCODE_MOVE_END;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_GO_TO_LAST_PAGE);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers jumping back to the page in the stack
     */
    public static boolean isJumpPageBack(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isShiftPressed()
                && (event.isCtrlPressed() && !event.isAltPressed() && keyCode == KeyEvent.KEYCODE_LEFT_BRACKET)
                || (!event.isCtrlPressed() && event.isAltPressed() && keyCode == KeyEvent.KEYCODE_DPAD_LEFT);
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_JUMP_PAGE_BACK);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers jumping forward to the page in the stack
     */
    public static boolean isJumpPageForward(int keyCode, KeyEvent event) {
        boolean b = sEnabled && !event.isShiftPressed() &&
                (event.isCtrlPressed() && !event.isAltPressed() && keyCode == KeyEvent.KEYCODE_RIGHT_BRACKET)
                || (!event.isCtrlPressed() && event.isAltPressed() && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT);
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_JUMP_PAGE_FORWARD);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers starting edit the selected annotation
     */
    public static boolean isStartEdit(int keyCode, KeyEvent event) {
        boolean b = sEnabled && keyCode == KeyEvent.KEYCODE_ENTER;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_START_EDIT_SELECTED_ANNOT);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers switching to the next form
     */
    public static boolean isSwitchForm(int keyCode, KeyEvent event) {
        boolean b = sEnabled && keyCode == KeyEvent.KEYCODE_TAB;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_SWITCH_FORM);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers rotating clockwise
     */
    public static boolean isRotateClockwise(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && event.isShiftPressed()
                && (keyCode == KeyEvent.KEYCODE_PLUS || keyCode == KeyEvent.KEYCODE_EQUALS || keyCode == KeyEvent.KEYCODE_NUMPAD_ADD);
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ROTATE_CLOCKWISE);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers rotating counter-clockwise
     */
    public static boolean isRotateCounterClockwise(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && event.isShiftPressed()
                && (keyCode == KeyEvent.KEYCODE_MINUS || keyCode == KeyEvent.KEYCODE_NUMPAD_SUBTRACT);
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ROTATE_COUNTER_CLOCKWISE);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers zoom in
     */
    public static boolean isZoomIn(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && (keyCode == KeyEvent.KEYCODE_PLUS || keyCode == KeyEvent.KEYCODE_EQUALS || keyCode == KeyEvent.KEYCODE_NUMPAD_ADD);
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ZOOM_IN);
        }
        return b;
    }

    /**
     * @param event The motion event
     *
     * @return True if the motion event triggers zoom in
     */
    public static boolean isZoomIn(MotionEvent event) {
        return sEnabled
                && (event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0
                && (event.getMetaState() & KeyEvent.META_CTRL_ON) != 0
                && event.getAxisValue(MotionEvent.AXIS_VSCROLL) > 0;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the pressed key triggers zoom out
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isZoomOut(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && (keyCode == KeyEvent.KEYCODE_MINUS || keyCode == KeyEvent.KEYCODE_NUMPAD_SUBTRACT);
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ZOOM_OUT);
        }
        return b;
    }

    /**
     * @param event The motion event
     *
     * @return True if the motion event triggers zoom out
     */
    public static boolean isZoomOut(MotionEvent event) {
        return sEnabled
                && (event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0
                && (event.getMetaState() & KeyEvent.META_CTRL_ON) != 0
                && event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0;
    }

    /**
     * @param event The motion event
     *
     * @return True if the motion event triggers zoom in/out
     */
    public static boolean isZoomInOut(MotionEvent event) {
        // in Chromebook, mouse wheel events are processed and passed through onScroll event rather
        // than onGenericMotionEvent, so we should handle zoom in/out in onScroll event as well in
        // order to support Chromebook zoom in/out with mouse wheel
        return sEnabled
                && event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE
                && (event.getMetaState() & KeyEvent.META_CTRL_ON) != 0;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the motion event triggers resetting zoom
     */
    public static boolean isResetZoom(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_0;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_RESET_ZOOM);
        }
        return b;
    }

    /**
     * @param event The motion event
     *
     * @return True if the motion event triggers long press
     */
    public static boolean isLongPress(MotionEvent event) {
        return sEnabled
                && event.getButtonState() == MotionEvent.BUTTON_SECONDARY;
    }

    /**
     * @param event The motion event
     *
     * @return True if the motion event triggers scroll
     */
    public static boolean isScroll(MotionEvent event) {
        return sEnabled
                && (event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0
                && (event.getMetaState() & KeyEvent.META_CTRL_ON) == 0;
    }

    /**
     * @param event The motion event
     *
     * @return True if the motion event triggers selecting text
     */
    public static boolean isTextSelect(MotionEvent event) {
        return sEnabled
                && event.getPointerCount() == 1 && event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the key event triggers opening the drawer
     */
    public static boolean isOpenDrawer(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_O;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_OPEN_DRAWER);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the key event triggers committing the current text
     */
    public static boolean isCommitText(int keyCode, KeyEvent event) {
        boolean b = sEnabled && (keyCode == KeyEvent.KEYCODE_ESCAPE
                || (event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed() && keyCode == KeyEvent.KEYCODE_ENTER));
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_COMMIT_CLOSE_TEXT);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the key event triggers committing the current draw
     */
    public static boolean isCommitDraw(int keyCode, KeyEvent event) {
        boolean b = sEnabled && (keyCode == KeyEvent.KEYCODE_ESCAPE || keyCode == KeyEvent.KEYCODE_ENTER);
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_COMMIT_CLOSE_DRAW);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the key event triggers switching to the next ink
     */
    public static boolean isSwitchInk(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && (keyCode == KeyEvent.KEYCODE_1
                || keyCode == KeyEvent.KEYCODE_2
                || keyCode == KeyEvent.KEYCODE_3
                || keyCode == KeyEvent.KEYCODE_4
                || keyCode == KeyEvent.KEYCODE_5);
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_SWITCH_INK);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the key event triggers erase ink
     */
    public static boolean isEraseInk(int keyCode, KeyEvent event) {
        boolean b = sEnabled && (keyCode == KeyEvent.KEYCODE_0 || keyCode == KeyEvent.KEYCODE_E);
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_ERASE_INK);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the key event triggers canceling the current tool
     */
    public static boolean isCancelTool(int keyCode, KeyEvent event) {
        boolean b = sEnabled && keyCode == KeyEvent.KEYCODE_ESCAPE;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_CANCEL_TOOL);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the key event triggers closing the current menu
     */
    public static boolean isCloseMenu(int keyCode, KeyEvent event) {
        boolean b = sEnabled && keyCode == KeyEvent.KEYCODE_ESCAPE;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_CLOSE_MENU);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the key event triggers closing the current tab
     */
    public static boolean isCloseTab(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_W;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_CLOSE_TAB);
        }
        return b;
    }

    /**
     * @param keyCode The key code
     * @param event The key event
     *
     * @return True if the key event triggers closing the app
     */
    public static boolean isCloseApp(int keyCode, KeyEvent event) {
        boolean b = sEnabled && event.isCtrlPressed() && !event.isAltPressed() && event.isShiftPressed()
                && keyCode == KeyEvent.KEYCODE_W;
        if (b) {
            sendEvent(AnalyticsHandlerAdapter.SHORTCUT_CLOSE_APP);
        }
        return b;
    }

    private static void sendEvent(int shortcutId) {
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_SHORTCUT, AnalyticsParam.shortcutParam(shortcutId));
    }
}

