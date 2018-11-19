//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.annots.FileAttachment;
import com.pdftron.pdf.asynctask.LoadSystemFontAsyncTask;
import com.pdftron.pdf.config.ToolConfig;
import com.pdftron.pdf.controls.AnnotIndicatorManger;
import com.pdftron.pdf.controls.PageIndicatorLayout;
import com.pdftron.pdf.model.FreeTextInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;

import org.json.JSONArray;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class implements the {@link com.pdftron.pdf.PDFViewCtrl.ToolManager}
 * interface. The ToolManager interface is basically a listener for the several
 * different events triggered by PDFViewCtrl, including gesture, layout and
 * custom events.
 * <p>
 * The Tool interface defined in this class is used to propagate these events
 * to the different tools, so making it possible to control which actions to
 * execute upon such events. Each concrete Tool implementation decides which
 * is the next tool, and the ToolManager uses the {@link Tool#getNextToolMode()}
 * to check if it must stop the event loop or create a new tool
 * and continue to propagate the event.
 * <p>
 * For example, the code for {@link #onDown(MotionEvent)} is as below:
 * <p>
 * <pre>
 * if (mTool != null) {
 *     ToolMode prev_tm = mTool.getAnnotType(), next_tm;
 *     do {
 *         mTool.onDown(e);
 *         next_tm = mTool.getNextToolMode();
 *         if (prev_tm != next_tm) {
 *             mTool = createTool(next_tm, mTool);
 *             prev_tm = next_tm;
 *         } else {
 *             break;
 *         }
 *     } while (true);
 * }
 * </pre>
 * <p>
 * With this being said, a Tool implementation should prevent forming tools in
 * a cyclic way.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ToolManager implements
    PDFViewCtrl.ToolManager,
    PDFViewCtrl.ActionCompletedListener {

    /**
     * This interface is used to forward events from {@link com.pdftron.pdf.PDFViewCtrl.ToolManager}
     * to the actual implementation of the Tool.
     */
    public interface Tool {
        /**
         * Gets the tool mode.
         *
         * @return the mode/identifier of this tool.
         */
        ToolModeBase getToolMode();

        /**
         * Gets what annotation type this tool can create
         * @return annot type for annotation creation tool, or unknown for non-creation tool.
         */
        int getCreateAnnotType();

        /**
         * Gets the next tool mode.
         *
         * @return the mode of the next tool. Via this method, a tool can
         * indicate the next tool to switch to.
         */
        ToolModeBase getNextToolMode();

        /**
         * Called when night mode has been updated.
         */
        void onNightModeUpdated(boolean isNightMode);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onSetDoc()} to the tools.
         */
        void onSetDoc();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onKeyUp(int, KeyEvent)} to the tools.
         */
        boolean onKeyUp(int keyCode, KeyEvent event);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDoubleTap(MotionEvent)} to the tools.
         */
        boolean onDoubleTap(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDoubleTapEnd(MotionEvent)} to the tools.
         */
        void onDoubleTapEnd(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDoubleTapEvent(MotionEvent)} to the tools.
         */
        boolean onDoubleTapEvent(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDown(MotionEvent)} to the tools.
         */
        boolean onDown(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onPointerDown(MotionEvent)} to the tools.
         */
        boolean onPointerDown(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)} to the tools.
         */
        boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onFlingStop()} to the tools.
         */
        boolean onFlingStop();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onLayout(boolean, int, int, int, int)} to the tools.
         */
        void onLayout(boolean changed, int l, int t, int r, int b);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onLongPress(MotionEvent)} to the tools.
         */
        boolean onLongPress(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScaleBegin(float, float)} to the tools.
         */
        boolean onScaleBegin(float x, float y);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScale(float, float)} to the tools.
         */
        boolean onScale(float x, float y);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScaleEnd(float, float)} to the tools.
         */
        boolean onScaleEnd(float x, float y);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onSingleTapConfirmed(MotionEvent)} to the tools.
         */
        boolean onSingleTapConfirmed(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onMove(MotionEvent, MotionEvent, float, float)} to the tools.
         */
        boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScrollChanged(int, int, int, int)} to the tools.
         */
        void onScrollChanged(int l, int t, int oldl, int oldt);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onShowPress(MotionEvent)} to the tools.
         */
        boolean onShowPress(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onSingleTapUp(MotionEvent)} to the tools.
         */
        boolean onSingleTapUp(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onClose()} to the tools.
         */
        void onClose();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onConfigurationChanged(Configuration)} to the tools.
         */
        void onConfigurationChanged(Configuration newConfig);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onPageTurning(int, int)} to the tools.
         */
        void onPageTurning(int old_page, int cur_page);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onPostSingleTapConfirmed()} to the tools.
         */
        void onPostSingleTapConfirmed();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onCustomEvent(Object)} to the tools.
         */
        void onCustomEvent(Object obj);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDocumentDownloadEvent(PDFViewCtrl.DownloadState, int, int, int, String)} to the tools.
         */
        void onDocumentDownloadEvent(PDFViewCtrl.DownloadState state, int page_num, int page_downloaded, int page_count, String message);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDraw(Canvas, Matrix)} to the tools.
         */
        void onDraw(Canvas canvas, Matrix tfm);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDrawEdgeEffects(Canvas, int, int)} to the tools.
         */
        boolean onDrawEdgeEffects(Canvas canvas, int width, int verticalOffset);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onReleaseEdgeEffects()} to the tools.
         */
        void onReleaseEdgeEffects();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onPullEdgeEffects(int, float)} to the tools.
         */
        void onPullEdgeEffects(int which_edge, float delta_distance);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDoubleTapZoomAnimationBegin()} to the tools.
         */
        void onDoubleTapZoomAnimationBegin();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDoubleTapZoomAnimationEnd()} to the tools.
         */
        void onDoubleTapZoomAnimationEnd();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onRenderingFinished()} to the tools.
         */
        void onRenderingFinished();

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#isCreatingAnnotation()} to the tools.
         */
        boolean isCreatingAnnotation();
    }

    /**
     * This interface can be used to listen for when the current tool changes.
     */
    public interface ToolChangedListener {

        /**
         * Event called when the tool changes.
         *
         * @param newTool the new tool
         * @param oldTool the old tool
         */
        void toolChanged(Tool newTool, Tool oldTool);
    }

    /**
     * This interface can be used to listen for when the PDFViewCtrl's onLayout() is triggered.
     */
    public interface OnLayoutListener {

        /**
         * Event called when the PDFViewCtrl's onLayout() is triggered.
         *
         * @param changed This is a new size or position for this view.
         * @param l       Left position, relative to parent.
         * @param t       Top position, relative to parent.
         * @param r       Right position, relative to parent.
         * @param b       Bottom position, relative to parent.
         */
        void onLayout(boolean changed, int l, int t, int r, int b);
    }

    /**
     * This interface can be used to avoid executing Tool's code in the {@link com.pdftron.pdf.tools.ToolManager}
     * implementation (the events will be called before Tool's ones).
     */
    public interface PreToolManagerListener {

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onSingleTapConfirmed(MotionEvent)} to the tools.
         */
        boolean onSingleTapConfirmed(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onMove(MotionEvent, MotionEvent, float, float)} to the tools.
         */
        boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)} to the tools.
         */
        boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScaleBegin(float, float)} to the tools.
         */
        boolean onScaleBegin(float x, float y);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScale(float, float)} to the tools.
         */
        boolean onScale(float x, float y);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScaleEnd(float, float)} to the tools.
         */
        boolean onScaleEnd(float x, float y);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onLongPress(MotionEvent)} to the tools.
         */
        boolean onLongPress(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onScrollChanged(int, int, int, int)} to the tools.
         */
        void onScrollChanged(int l, int t, int oldl, int oldt);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onDoubleTap(MotionEvent)} to the tools.
         */
        boolean onDoubleTap(MotionEvent e);

        /**
         * Propagates {@link PDFViewCtrl.ToolManager#onKeyUp(int, KeyEvent)} to the tools.
         */
        boolean onKeyUp(int keyCode, KeyEvent event);
    }

    /**
     * This interface can be used to detect if the built-in Tools quick menu item has been clicked.
     */
    public interface QuickMenuListener {

        /**
         * Called when a menu in quick menu has been clicked.
         *
         * @param menuItem The quick menu item
         * @return True if handled
         */
        boolean onQuickMenuClicked(QuickMenuItem menuItem);

        /**
         * Called when quick menu has been shown.
         */
        void onQuickMenuShown();

        /**
         * Called when quick menu has been dismissed.
         */
        void onQuickMenuDismissed();
    }

    /**
     * This interface can be used to monitor any modification events that change the structure of
     * the PDF document such as page manipulation, bookmark modification, etc.
     * <p>
     * For listening to annotation modification events see {@link AnnotationModificationListener}.
     */
    public interface PdfDocModificationListener {
        /**
         * Called when PDF bookmark has been modified.
         */
        void onBookmarkModified();

        /**
         * Called when document pages haven cropped.
         */
        void onPagesCropped();

        /**
         * Called when pages have been added to the document.
         *
         * @param pageList The list of pages added to the document
         */
        void onPagesAdded(final List<Integer> pageList);

        /**
         * Called when pages have been deleted from the document.
         *
         * @param pageList The list of pages deleted from the document
         */
        void onPagesDeleted(final List<Integer> pageList);

        /**
         * Called when pages have been rotated.
         *
         * @param pageList The list of rotated pages
         */
        void onPagesRotated(final List<Integer> pageList);

        /**
         * Called when a page has been moved.
         *
         * @param from The page number on which the page moved from
         * @param to   The page number on which the page moved to
         */
        void onPageMoved(int from, int to);

        /**
         * Called when all annotations in the document have been removed.
         */
        void onAllAnnotationsRemoved();

        /**
         * Called when an annotations action has been taken place.
         */
        void onAnnotationAction();
    }

    /**
     * This interface can be used to monitor annotation modification events such as added/edited/removed.
     * <p>
     * For listening to events that change the structure of PDF document such as page manipulation see
     * {@link PdfDocModificationListener}.
     */
    public interface AnnotationModificationListener {

        /**
         * Called when annotations have been added to the document.
         *
         * @param annots The list of annotations (a pair of annotation and the page number
         *               where the annotation is on)
         */
        void onAnnotationsAdded(Map<Annot, Integer> annots);

        /**
         * Called right before annotations have been modified.
         *
         * @param annots The list of annotations (a pair of annotation and the page number
         *               where the annotation is on)
         */
        void onAnnotationsPreModify(Map<Annot, Integer> annots);

        /**
         * Called when annotations have been modified.
         *
         * @param annots The list of annotations (a pair of annotation and the page number
         *               where the annotation is on)
         */
        void onAnnotationsModified(Map<Annot, Integer> annots, Bundle extra);

        /**
         * Called right before annotations have been removed from the document.
         *
         * @param annots The list of annotations (a pair of annotation and the page number
         *               where the annotation is on)
         */
        void onAnnotationsPreRemove(Map<Annot, Integer> annots);

        /**
         * Called when annotations have been removed from the document.
         *
         * @param annots The list of annotations (a pair of annotation and the page number
         *               where the annotation is on)
         */
        void onAnnotationsRemoved(Map<Annot, Integer> annots);

        /**
         * Called when all annotations in the specified page have been removed.
         *
         * @param pageNum The page number where the annotations are on
         */
        void onAnnotationsRemovedOnPage(int pageNum);

        /**
         * Called when annotations couldn't have been added
         *
         * @param errorMessage The error message
         */
        void annotationsCouldNotBeAdded(String errorMessage);
    }

    /**
     * This interface can be used to monitor basic annotation events such as selected/unselected.
     */
    public interface BasicAnnotationListener {
        /**
         * Called when an annotation has been selected.
         *
         * @param annot   The selected annotation
         * @param pageNum The page number where the annotation is on
         */
        void onAnnotationSelected(Annot annot, int pageNum);

        /**
         * Called when an annotation has been unselected.
         */
        void onAnnotationUnselected();

        /**
         * Intercept tool's response to user actions (such as clicking on links,
         * clicking on form widget, or about to change annotation properties etc.)
         * If handled, tool will stop default logic.
         *
         * @param annot    annotation
         * @param extra    extra information
         * @param toolMode tool mode that handles annotation
         * @return true then intercept the subclass function, false otherwise
         */
        boolean onInterceptAnnotationHandling(@Nullable Annot annot, Bundle extra, ToolMode toolMode);

        /**
         * Intercept handling of dialog
         *
         * @param dialog the dialog about to show up
         * @return true if intercept the subclass function, false otherwise
         */
        boolean onInterceptDialog(AlertDialog dialog);
    }

    /**
     * This interface can be used to monitor advanced annotation events from various tools.
     */
    public interface AdvancedAnnotationListener {
        /**
         * Called when a file attachment has been selected.
         *
         * @param attachment The file attachment
         */
        void fileAttachmentSelected(FileAttachment attachment);

        /**
         * Called when free hand stylus has been used for the firs time.
         */
        void freehandStylusUsedFirstTime();

        /**
         * Called when a location has been selected for adding the image stamp.
         *
         * @param targetPoint The target location to add the image stamp
         */
        void imageStamperSelected(PointF targetPoint);

        /**
         * Called when a location has been selected for adding the file attachment.
         *
         * @param targetPoint The target location to add the file attachment
         */
        void attachFileSelected(PointF targetPoint);

        /**
         * Called when free text inline editing has started.
         */
        void freeTextInlineEditingStarted();
    }

    public interface SpecialAnnotationListener {
        /**
         * @param text     The text
         * @param anchor   The anchor
         * @param isDefine True if it is define, False if it is translate
         * @hide Called when define or translate has been selected.
         */
        void defineTranslateSelected(String text, RectF anchor, Boolean isDefine);
    }

    /**
     * This interface can be used to monitor tools interaction with annotation toolbar
     */
    public interface AnnotationToolbarListener {
        /**
         * Called when ink edit has been selected.
         *
         * @param annot The ink annotation
         */
        void onInkEditSelected(Annot annot);

        /**
         * The implementation should specify the annotation toolbar height.
         *
         * @return The annotation toolbar height
         */
        int annotationToolbarHeight();

        /**
         * The implementation should specify the toolbar height.
         *
         * @return The annotation toolbar height
         */
        int toolbarHeight();

        /**
         * The implementation should open the edit toolbar for the mode
         *
         * @param mode The tool mode
         */
        void onOpenEditToolbar(ToolMode mode);

        /**
         * The implementation should open the annotation toolbar for the mode
         *
         * @param mode The tool mode
         */
        void onOpenAnnotationToolbar(ToolMode mode);
    }

    /**
     * This interface can be used to monitor generic motion event
     */
    public interface OnGenericMotionEventListener {
        /**
         * Called when a generic motion occurred.
         *
         * @param event The motion event
         */
        void onGenericMotionEvent(MotionEvent event);

        /**
         * The implementation should change the pointer icon.
         *
         * @param pointerIcon The pointer icon
         */
        void onChangePointerIcon(PointerIcon pointerIcon);
    }

    /**
     * This interface can be used to provide custom key for annotation creation
     */
    public interface ExternalAnnotationManagerListener {
        /**
         * The implementation should generate a string key.
         *
         * @return The generated string key
         */
        String generateKey();
    }

    /**
     * Base tool mode
     */
    public interface ToolModeBase {
        /**
         * Gets value of this tool mode
         *
         * @return value
         */
        int getValue();
    }

    /**
     * Tool modes
     */
    public enum ToolMode implements ToolModeBase {
        /**
         * Identifier of the Pan tool.
         */
        PAN(1),
        /**
         * Identifier of the Annotation Edit tool.
         */
        ANNOT_EDIT(2),
        /**
         * Identifier of the Line tool.
         */
        LINE_CREATE(3),
        /**
         * Identifier of the Arrow tool.
         */
        ARROW_CREATE(4),
        /**
         * Identifier of the Rectangle tool.
         */
        RECT_CREATE(5),
        /**
         * Identifier of the Oval/Ellipse tool
         */
        OVAL_CREATE(6),
        /**
         * Identifier of the Ink tool.
         */
        INK_CREATE(7),
        /**
         * Identifier of the Note tool.
         */
        TEXT_ANNOT_CREATE(8),
        /**
         * Identifier of the Link tool.
         */
        LINK_ACTION(9),
        /**
         * Identifier of the Text Selection tool.
         */
        TEXT_SELECT(10),
        /**
         * Identifier of the Form Filling too.
         */
        FORM_FILL(11),
        /**
         * Identifier of the Text tool.
         */
        TEXT_CREATE(12),
        /**
         * Identifier of the Annotation Edit for Line/Arrow tool.
         */
        ANNOT_EDIT_LINE(13),
        /**
         * Identifier of the Rich Media tool.
         */
        RICH_MEDIA(14),
        /**
         * Identifier of the Digital Signature tool.
         */
        DIGITAL_SIGNATURE(15),
        /**
         * Identifier of the Text Underline tool.
         */
        TEXT_UNDERLINE(16),
        /**
         * Identifier of the Text Highlight tool.
         */
        TEXT_HIGHLIGHT(17),
        /**
         * Identifier of the Text Squiggly tool.
         */
        TEXT_SQUIGGLY(18),
        /**
         * Identifier of the Text Strikeout tool.
         */
        TEXT_STRIKEOUT(19),
        /**
         * Identifier of the Eraser tool.
         */
        INK_ERASER(20),
        /**
         * Identifier of the Annotation Edit for text markup tool.
         */
        ANNOT_EDIT_TEXT_MARKUP(21),
        /**
         * Identifier of the TextHighlighter tool.
         */
        TEXT_HIGHLIGHTER(22),
        /**
         * Identifier of the (floating) Signature tool.
         */
        SIGNATURE(23),
        /**
         * Identifier of the Image Stamper tool.
         */
        STAMPER(24),
        /**
         * Identifier of the Rubber Stamper tool.
         */
        RUBBER_STAMPER(25),
        /**
         * Identifier of the Stamper tool.
         */
        RECT_LINK(26),
        /**
         * Identifier of the Signature form field tool.
         */
        FORM_SIGNATURE_CREATE(27),
        /**
         * Identifier of the form field text tool.
         */
        FORM_TEXT_FIELD_CREATE(28),
        /**
         * Identifier of the Text Link tool.
         */
        TEXT_LINK_CREATE(29),
        /**
         * Identifier of the form checkbox tool.
         */
        FORM_CHECKBOX_CREATE(30),
        /**
         * @hide Identifier of the form radio group tool.
         */
        FORM_RADIO_GROUP_CREATE(31),
        /**
         * Identifier of the text redaction tool.
         */
        TEXT_REDACTION(32),
        /**
         * Identifier of the free highlighter tool (Ink in blend mode).
         */
        FREE_HIGHLIGHTER(33),
        /**
         * Identifier of the polygon tool.
         */
        POLYLINE_CREATE(34),
        /**
         * Identifier of the polygon tool.
         */
        POLYGON_CREATE(35),
        /**
         * Identifier of the polygon cloud tool.
         */
        CLOUD_CREATE(36),
        /**
         * Identifier of the multi select annotation edit tool (Select annots by draw a rectangle).
         */
        ANNOT_EDIT_RECT_GROUP(37),
        /**
         * Identifier of the editing tool for polyline/polygon/cloud tool.
         */
        ANNOT_EDIT_ADVANCED_SHAPE(38),
        /**
         * Identifier of the ruler tool.
         */
        RULER_CREATE(39),
        /**
         * Identifier of the Callout tool
         */
        CALLOUT_CREATE(40),
        /**
         * Identifier of the Sound tool
         */
        SOUND_CREATE(41),
        /**
         * Identifier of the FileAttachment tool
         */
        FILE_ATTACHMENT_CREATE(42);
        // Note: when adding a new entry, update NUM_TOOL_MODE

        private final static int NUM_TOOL_MODE = 42;

        private final int mode;

        ToolMode(int mode) {
            this.mode = mode;
        }

        public int getValue() {
            return this.mode;
        }

        private static SparseArray<ToolModeBase> map = new SparseArray<>();

        static {
            for (ToolMode toolMode : ToolMode.values()) {
                map.put(toolMode.getValue(), toolMode);
            }
        }


        private static AtomicInteger modeGenerator = new AtomicInteger(NUM_TOOL_MODE * 2);
        // multiply to 2 for safety just in case forgot to update NUM_TOOL_MODE

        /**
         * Gets tool mode based on tool mode value
         *
         * @param toolMode tool mode value
         * @return tool mode
         */
        public static ToolModeBase toolModeFor(int toolMode) {
            return map.get(toolMode);
        }

        /**
         * Add a new tool mode
         *
         * @return new tool mode
         */
        public static ToolModeBase addNewMode() {
            return addNewMode(Annot.e_Unknown);
        }

        /**
         * Add a new creator tool mode
         *
         * @param annotType annotation type that this tool creates
         * @return new tool mode
         */
        public static ToolModeBase addNewMode(final int annotType) {
            final int newMode = modeGenerator.incrementAndGet();
            ToolModeBase mode = new ToolModeBase() {
                @Override
                public int getValue() {
                    return newMode;
                }
            };
            map.put(newMode, mode);
            return mode;
        }
    }

    public static ToolMode getDefaultToolMode(ToolModeBase toolModeBase) {
        if (toolModeBase != null && toolModeBase instanceof ToolMode) {
            return (ToolMode) toolModeBase;
        }
        return ToolMode.PAN;
    }

    private ArrayList<ToolChangedListener> mToolChangedListeners;
    private CopyOnWriteArray<OnLayoutListener> mOnLayoutListeners;
    private PreToolManagerListener mPreToolManagerListener;
    private QuickMenuListener mQuickMenuListener;
    private ArrayList<AnnotationModificationListener> mAnnotationModificationListeners;
    private ArrayList<PdfDocModificationListener> mPdfDocModificationListeners;
    private AdvancedAnnotationListener mAdvancedAnnotationListener;
    private SpecialAnnotationListener mSpecialAnnotationListener;
    private BasicAnnotationListener mBasicAnnotationListener;
    private AnnotationToolbarListener mAnnotationToolbarListener;
    private OnGenericMotionEventListener mOnGenericMotionEventListener;
    private ExternalAnnotationManagerListener mExternalAnnotationManagerListener;

    private boolean mPageNumberIndicatorVisible = true;

    private boolean mSkipNextTapEvent = false;
    private boolean mSkipNextTouchEvent = false;

    private Tool mTool;
    private PDFViewCtrl mPdfViewCtrl;
    private UndoRedoManager mUndoRedoManger;
    private AnnotManager mAnnotManager = null;

    private boolean mReadOnly = false;
    private boolean mTextMarkupAdobeHack = true;

    private boolean mCanCheckAnnotPermission = false;

    private boolean mShowAuthorDialog = false;

    // permission
    private String mAuthorId;

    private boolean mCopyAnnotatedTextToNote = false;
    private boolean mStylusAsPen = false;
    private boolean mIsNightMode = false;
    private boolean mInkSmoothing = true;
    private boolean mStickyNoteShowPopup = true;
    private boolean mEditInkAnnots = false;
    private boolean mCanOpenEditToolbarFromPan = false;
    private boolean mAddImageStamperTool = false;

    private Set<String> mFreeTextFonts;
    private Set<ToolMode> mDisabledToolModes;
    private Set<ToolMode> mDisabledToolModesSave;

    private TextToSpeech mTTS;

    // draw UI until rendering finishes to prevent flash
    private ArrayList<Tool> mOldTools;

    private String mSelectedAnnotId;
    private int mSelectedAnnotPageNum = -1;

    private boolean mQuickMenuJustClosed = false;

    private boolean mIsAutoSelectAnnotation = true;

    private boolean mDisableQuickMenu = false;

    private boolean mDoubleTapToZoom = true;

    private boolean mAutoResizeFreeText = false;

    private boolean mRealTimeAnnotEdit = true;

    private boolean mEditFreeTextOnTap = false;

    // for caching related
    private String mCacheFileName;

    // for lifecycle
    private boolean mCanResumePdfDocWithoutReloading;

    @SuppressWarnings("FieldCanBeLocal")
    private boolean mAnnotIndicatorsEnabled = true;
    private AnnotIndicatorManger mAnnotIndicatorManger;
    private HashMap<ToolModeBase, Class<? extends com.pdftron.pdf.tools.Tool>> mCustomizedToolClassMap;
    private HashMap<ToolModeBase, Object[]> mCustomizedToolParamMap;
    private Class<? extends Pan> mDefaultToolClass = Pan.class;
    private ArrayList<FreeTextInfo> mPendingFreeText;
    private LoadSystemFontAsyncTask mLoadSystemFontTask;
    private boolean mSystemFontsLoaded = false;
    // built in page number indicator
    private PopupWindow mPageIndicatorPopup;

    private @Nullable
    WeakReference<FragmentActivity> mCurrentActivity;

    /**
     * Class constructor.
     *
     * @param pdfViewCtrl the {@link com.pdftron.pdf.PDFViewCtrl}. It must not be null.
     */
    public ToolManager(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
        mPdfViewCtrl.setActionCompletedListener(this);
        try {
            mPdfViewCtrl.enableUndoRedo();
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        mUndoRedoManger = new UndoRedoManager(this);
        mOldTools = new ArrayList<>();

        if (mAnnotIndicatorsEnabled) {
            mAnnotIndicatorManger = new AnnotIndicatorManger(this);
        }
    }

    /**
     * Resets annotation indicators
     */
    public void resetIndicator() {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }
    }

    /**
     * Creates the default tool (Pan tool).
     * If instantiate default tool failed, it will create {@link Pan} tool
     *
     * @return the default tool
     */
    public Pan createDefaultTool() {
        Pan tool;
        try {
            tool = mDefaultToolClass.getDeclaredConstructor(mPdfViewCtrl.getClass()).newInstance(mPdfViewCtrl);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e, "failed to instantiate default tool");
            tool = new Pan(mPdfViewCtrl);
        }
        tool.setPageNumberIndicatorVisible(mPageNumberIndicatorVisible);
        tool.onCreate();

        return tool;
    }

    /**
     * Creates the specified tool and copies the necessary info from the
     * previous tool if provided.
     *
     * @param newTool     the identifier for the tool to be created
     * @param currentTool the current tool before this call
     */
    public Tool createTool(ToolModeBase newTool, Tool currentTool) {
        com.pdftron.pdf.tools.Tool tool = safeCreateTool(newTool);

        tool.setPageNumberIndicatorVisible(mPageNumberIndicatorVisible);

        if (currentTool == null || tool.getToolMode() != currentTool.getToolMode()) {
            if (mToolChangedListeners != null) {
                for (ToolChangedListener listener : mToolChangedListeners) {
                    listener.toolChanged(tool, currentTool);
                }
            }
        }

        if (currentTool != null) {
            com.pdftron.pdf.tools.Tool oldTool = (com.pdftron.pdf.tools.Tool) currentTool;
            tool.mAnnot = oldTool.mAnnot;
            tool.mAnnotBBox = oldTool.mAnnotBBox;
            tool.mAnnotPageNum = oldTool.mAnnotPageNum;
            tool.mAvoidLongPressAttempt = oldTool.mAvoidLongPressAttempt;
            if (tool.getToolMode() != ToolMode.PAN) {
                tool.mCurrentDefaultToolMode = oldTool.mCurrentDefaultToolMode;
            } else {
                tool.mCurrentDefaultToolMode = ToolMode.PAN;
            }
            tool.mForceSameNextToolMode = oldTool.mForceSameNextToolMode;
            if (oldTool.mForceSameNextToolMode) {
                tool.mStylusUsed = oldTool.mStylusUsed;
            }
            tool.mAnnotView = oldTool.mAnnotView;
            oldTool.onClose();   // Close the old tool; old tool can use this to clean up things.

            if (oldTool.getToolMode() != tool.getToolMode()) {
                tool.setJustCreatedFromAnotherTool();
            }

            if (oldTool.mCurrentDefaultToolMode != tool.getToolMode()) {
                setQuickMenuJustClosed(false);
            }

            // When creating sticky note, let annotation edit tool pop up the note dialog
            // directly, instead of showing the menu as the intermediate step.
            if (oldTool.getToolMode() == ToolMode.TEXT_ANNOT_CREATE && tool.getToolMode() == ToolMode.ANNOT_EDIT) {
                //noinspection ConstantConditions
                AnnotEdit at = (AnnotEdit) tool;
                at.setUpFromStickyCreate(true);
                at.mForceSameNextToolMode = oldTool.mForceSameNextToolMode;
            }

            // When creating free text, let annotation edit tool pop up the note dialog
            // directly, instead of showing the menu as the intermediate step.
            if ((oldTool.getToolMode() == ToolMode.TEXT_CREATE ||
                oldTool.getToolMode() == ToolMode.CALLOUT_CREATE) &&
                (tool.getToolMode() == ToolMode.ANNOT_EDIT ||
                tool.getToolMode() == ToolMode.ANNOT_EDIT_ADVANCED_SHAPE)) {
                //noinspection ConstantConditions
                AnnotEdit at = (AnnotEdit) tool;
                if (oldTool.getToolMode() == ToolMode.TEXT_CREATE) {
                    at.setUpFromFreeTextCreate(true);
                } else if (oldTool.getToolMode() == ToolMode.CALLOUT_CREATE) {
                    at.mUpFromCalloutCreate = oldTool.mUpFromCalloutCreate;
                }
                at.mForceSameNextToolMode = oldTool.mForceSameNextToolMode;
            }

            // When erase using single tap, do not show annot edit bounding box
            if (oldTool.getToolMode() == ToolMode.INK_ERASER && tool.getToolMode() == ToolMode.PAN) {
                //noinspection ConstantConditions
                Pan pan = (Pan) tool;
                pan.mSuppressSingleTapConfirmed = true;
            }
        } else if (getTool() != null && getTool() instanceof com.pdftron.pdf.tools.Tool) {
            // cleanup
            com.pdftron.pdf.tools.Tool oldTool = (com.pdftron.pdf.tools.Tool) getTool();
            if (oldTool.mAnnotView != null) {
                oldTool.removeAnnotView(false);
            }
        }

        // Class a tool's onCreate() function in which the tool can initialize things.
        tool.onCreate();

        // if system font is not loaded, load system font
        if (!mSystemFontsLoaded) {
            if (mLoadSystemFontTask == null) {
                mLoadSystemFontTask = new LoadSystemFontAsyncTask(mPdfViewCtrl.getContext());
                mLoadSystemFontTask.addFinishCallback(new LoadSystemFontAsyncTask.onFinishListener() {
                    @Override
                    public void onFinish() {
                        ToolManager.this.onSystemFontsFinishedLoaded();
                    }
                });
            }
            if (mLoadSystemFontTask.getStatus() == AsyncTask.Status.PENDING) {
                try {
                    mLoadSystemFontTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "Tool Manager load font too many times");
                    mSystemFontsLoaded = true;  // stop checking whether system font is loaded
                }
            } else if (mLoadSystemFontTask.getStatus() == AsyncTask.Status.FINISHED) {
                mSystemFontsLoaded = true;
            }

        }

        return tool;
    }

    /**
     * Creates tool safely, if there is any exception, creates default tool
     *
     * @param mode tool mode
     * @return tool
     */
    private com.pdftron.pdf.tools.Tool safeCreateTool(ToolModeBase mode) {
        com.pdftron.pdf.tools.Tool tool;
        ToolModeBase actualMode = mode;
        if (mDisabledToolModes != null && mDisabledToolModes.contains(mode)) {
            actualMode = ToolMode.PAN;
        }
        try {
            Object[] toolArgs = getToolArguments(actualMode);
            Class toolClass = getToolClassByMode(actualMode);
            tool = instantiateTool(toolClass, toolArgs);
        } catch (Exception e) {
            tool = createDefaultTool();
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } catch (OutOfMemoryError oom) {
            Utils.manageOOM(mPdfViewCtrl);
            tool = createDefaultTool();
        }
        return tool;
    }

    /**
     * get tool class by tool mode
     *
     * @param modeBase tool mode
     * @return tool class
     */
    private Class<? extends com.pdftron.pdf.tools.Tool> getToolClassByMode(ToolModeBase modeBase) {
        if (null != mCustomizedToolClassMap && mCustomizedToolClassMap.containsKey(modeBase)) {
            return mCustomizedToolClassMap.get(modeBase);
        }
        ToolMode mode = getDefaultToolMode(modeBase);
        switch (mode) {
            case PAN:
                return Pan.class;
            case ANNOT_EDIT:
                return AnnotEdit.class;
            case LINE_CREATE:
                return LineCreate.class;
            case ARROW_CREATE:
                return ArrowCreate.class;
            case RULER_CREATE:
                return RulerCreate.class;
            case POLYLINE_CREATE:
                return PolylineCreate.class;
            case RECT_CREATE:
                return RectCreate.class;
            case OVAL_CREATE:
                return OvalCreate.class;
            case SOUND_CREATE:
                return SoundCreate.class;
            case FILE_ATTACHMENT_CREATE:
                return FileAttachmentCreate.class;
            case POLYGON_CREATE:
                return PolygonCreate.class;
            case CLOUD_CREATE:
                return CloudCreate.class;
            case INK_CREATE:
                return FreehandCreate.class;
            case FREE_HIGHLIGHTER:
                return FreeHighlighterCreate.class;
            case TEXT_ANNOT_CREATE:
                return StickyNoteCreate.class;
            case LINK_ACTION:
                return LinkAction.class;
            case TEXT_SELECT:
                return TextSelect.class;
            case FORM_FILL:
                return FormFill.class;
            case TEXT_CREATE:
                return FreeTextCreate.class;
            case CALLOUT_CREATE:
                return CalloutCreate.class;
            case ANNOT_EDIT_LINE:
                return AnnotEditLine.class;
            case ANNOT_EDIT_ADVANCED_SHAPE:
                return AnnotEditAdvancedShape.class;
            case RICH_MEDIA:
                return RichMedia.class;
            case TEXT_UNDERLINE:
                return TextUnderlineCreate.class;
            case TEXT_HIGHLIGHT:
                return TextHighlightCreate.class;
            case TEXT_SQUIGGLY:
                return TextSquigglyCreate.class;
            case TEXT_STRIKEOUT:
                return TextStrikeoutCreate.class;
            case TEXT_REDACTION:
                return TextRedactionCreate.class;
            case INK_ERASER:
                return Eraser.class;
            case ANNOT_EDIT_TEXT_MARKUP:
                return AnnotEditTextMarkup.class;
            case TEXT_HIGHLIGHTER:
                return TextHighlighter.class;
            case DIGITAL_SIGNATURE:
                return DigitalSignature.class;
            case SIGNATURE:
                return Signature.class;
            case STAMPER:
                return Stamper.class;
            case RUBBER_STAMPER:
                return RubberStampCreate.class;
            case RECT_LINK:
                return RectLinkCreate.class;
            case FORM_SIGNATURE_CREATE:
                return SignatureFieldCreate.class;
            case FORM_TEXT_FIELD_CREATE:
                return TextFieldCreate.class;
            case TEXT_LINK_CREATE:
                return TextLinkCreate.class;
            case FORM_CHECKBOX_CREATE:
                return CheckboxFieldCreate.class;
            case FORM_RADIO_GROUP_CREATE:
                return RadioGroupFieldCreate.class;
            case ANNOT_EDIT_RECT_GROUP:
                return AnnotEditRectGroup.class;
            default:
                return Pan.class;
        }
    }

    /**
     * get tool arguments by tool mode for instantiating tool
     * By default, instantiating new tool needs {@link #mPdfViewCtrl}
     *
     * @param mode tool mode
     * @return arguments for instantiating new tool, default is [ @link #mPdfViewCtrl} ]
     */
    private Object[] getToolArguments(ToolModeBase mode) {
        if (null != mCustomizedToolParamMap && mCustomizedToolParamMap.containsKey(mode)) {
            return mCustomizedToolParamMap.get(mode);
        }
        if (mode == ToolMode.INK_ERASER) {
            return new Object[]{mPdfViewCtrl, Eraser.EraserType.INK_ERASER};
        }
        return new Object[]{mPdfViewCtrl};
    }

    /**
     * Creates next tool by tool class and arguments
     *
     * @param toolClass tool class
     * @param args      arguments for instantiate the tool
     * @return next tool@throws
     * @throws NoSuchMethodException       if a matching method is not found.
     *                                     Please add a public static newInstance(Object... args) to your custom Tool class
     * @throws IllegalAccessException      if this {@code Constructor} object
     *                                     is enforcing Java language access control and the underlying
     *                                     constructor is inaccessible.
     * @throws IllegalArgumentException    if the number of actual
     *                                     and formal parameters differ; if an unwrapping
     *                                     conversion for primitive arguments fails; or if,
     *                                     after possible unwrapping, a parameter value
     *                                     cannot be converted to the corresponding formal
     *                                     parameter type by a method invocation conversion; if
     *                                     this constructor pertains to an enum type.
     * @throws InstantiationException      if the class that declares the
     *                                     underlying constructor represents an abstract class.
     * @throws InvocationTargetException   if the underlying constructor
     *                                     throws an exception.
     * @throws ExceptionInInitializerError if the initialization provoked
     *                                     by this method fails.
     */
    private com.pdftron.pdf.tools.Tool instantiateTool(Class<? extends Tool> toolClass, Object... args) throws NoSuchMethodException,
        IllegalAccessException,
        InvocationTargetException,
        InstantiationException {
        Class[] cArg = processArguments(args);
        return (com.pdftron.pdf.tools.Tool) toolClass.getDeclaredConstructor(cArg).newInstance(args);
    }

    private Class[] processArguments(Object... args) {
        Class[] cArg = new Class[args.length];
        int i = 0;
        for (Object arg : args) {
            if (arg instanceof PDFViewCtrl) {
                cArg[i] = PDFViewCtrl.class;
            } else {
                cArg[i] = arg.getClass();
            }
            i++;
        }
        return cArg;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onControlReady()}.
     */
    @Override
    public void onControlReady() {
        if (mTool == null) {
            mTool = createDefaultTool();
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onClose()}.
     */
    @Override
    public void onClose() {
        if (mTool != null) {
            mTool.onClose();
        }
        if (mLoadSystemFontTask != null) {
            mLoadSystemFontTask.cancel(true);
            mLoadSystemFontTask = null;
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onCustomEvent(Object)}.
     */
    @Override
    public void onCustomEvent(Object obj) {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onCustomEvent(obj);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onKeyUp(int, KeyEvent)}.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onKeyUp(keyCode, event)) {
                return true;
            }
        }
        boolean handled = false;

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                handled = mTool.onKeyUp(keyCode, event);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return handled;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDoubleTap(MotionEvent)}.
     */
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onDoubleTap(e)) {
                return true;
            }
        }
        boolean handled = false;

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                handled = mTool.onDoubleTap(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return handled;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDoubleTapEvent(MotionEvent)}.
     */
    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        boolean handled = false;

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                handled = mTool.onDoubleTapEvent(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return handled;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDoubleTapEnd(MotionEvent)}.
     */
    @Override
    public void onDoubleTapEnd(MotionEvent e) {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onDoubleTapEnd(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        if (mSkipNextTouchEvent) {
            return true;
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onDown(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);

                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onPointerDown(MotionEvent)}.
     */
    @Override
    public boolean onPointerDown(MotionEvent e) {
        if (mSkipNextTouchEvent) {
            return true;
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onPointerDown(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);

                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDocumentDownloadEvent(PDFViewCtrl.DownloadState, int, int, int, String)}.
     */
    @Override
    public void onDocumentDownloadEvent(PDFViewCtrl.DownloadState mode, int page_num, int page_downloaded, int page_count, String message) {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onDocumentDownloadEvent(mode, page_num, page_downloaded, page_count, message);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        // Draw old Tools UI to prevent flash when waiting for rendering
        if (mOldTools != null) {
            for (Tool t : mOldTools) {
                t.onDraw(canvas, tfm);
            }
        }

        if (mTool != null) {
            mTool.onDraw(canvas, tfm);
        }

        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.drawAnnotIndicators(canvas);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onFlingStop()}.
     */
    @Override
    public boolean onFlingStop() {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_NORMAL);
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onFlingStop();
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }

        if (mTool != null) {
            mTool.onLayout(changed, l, t, r, b);
        }

        if (mOldTools != null) {
            for (Tool tool : mOldTools) {
                tool.onLayout(changed, l, t, r, b);
            }
        }

        final CopyOnWriteArray<OnLayoutListener> listeners = mOnLayoutListeners;
        if (listeners != null && listeners.size() > 0) {
            CopyOnWriteArray.Access<OnLayoutListener> access = listeners.start();
            try {
                int count = access.size();
                for (int i = 0; i < count; i++) {
                    access.get(i).onLayout(changed, l, t, r, b);
                }
            } finally {
                listeners.end();
            }
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onLongPress(MotionEvent)}.
     */
    @Override
    public boolean onLongPress(MotionEvent e) {
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onLongPress(e)) {
                return true;
            }
        }
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                boolean handled = mTool.onLongPress(e);
                if (!handled) {
                    next_tm = mTool.getNextToolMode();
                    if (prev_tm != next_tm) {
                        mTool = createTool(next_tm, mTool);
                        prev_tm = next_tm;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        if (mSkipNextTouchEvent) {
            return true;
        }
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onMove(e1, e2, x_dist, y_dist)) {
                return true;
            }
        }

        boolean handled = false;
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                // to improve freehand writing, onMove is added to PDFViewCtrl.onTouchEvent
                // however, we do not want this to affect anything else
                if (mTool.getToolMode() == ToolMode.INK_CREATE) {
                    // handle all move events for ink create
                    handled |= mTool.onMove(e1, e2, x_dist, y_dist);
                } else {
                    // for any other tools, only handle if x_dist and y_dist not equal to -1
                    //noinspection SimplifiableIfStatement
                    if ((Float.compare(x_dist, -1) == 0) && (Float.compare(y_dist, -1) == 0)) {
                        handled |= true;
                    } else {
                        handled |= mTool.onMove(e1, e2, x_dist, y_dist);
                    }
                }

                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return handled;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onScrollChanged(int, int, int, int)}.
     */
    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        mQuickMenuJustClosed = false; // clear this flag if it is not consumed by onSingleTapConfirmed
        if (mPreToolManagerListener != null) {
            mPreToolManagerListener.onScrollChanged(l, t, oldl, oldt);
        }
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onScrollChanged(l, t, oldl, oldt);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onPageTurning(int, int)}.
     */
    @Override
    public void onPageTurning(int old_page, int cur_page) {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_NORMAL);
            mAnnotIndicatorManger.reset(false);
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onPageTurning(old_page, cur_page);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onScale(float, float)}.
     */
    @Override
    public boolean onScale(float x, float y) {
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onScale(x, y)) {
                return true;
            }
        }

        if (mTool != null) {
            boolean handled;
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;

            do {
                handled = mTool.onScale(x, y);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);

            if (handled) {
                return true;
            }
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_ZOOMING);
        }

        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onScaleBegin(x, y)) {
                return true;
            }
        }

        if (mTool != null) {
            boolean handled;
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;

            do {
                handled = mTool.onScaleBegin(x, y);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);

            if (handled) {
                return true;
            }
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onScaleEnd(float, float)}.
     */
    @Override
    public boolean onScaleEnd(float x, float y) {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_NORMAL);
            mAnnotIndicatorManger.reset(true);
        }

        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onScaleEnd(x, y)) {
                return true;
            }
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onScaleEnd(x, y);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            }
            while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onSetDoc()}.
     */
    @Override
    public void onSetDoc() {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onSetDoc();
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onShowPress(MotionEvent)}.
     */
    @Override
    public boolean onShowPress(MotionEvent e) {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onShowPress(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (mSkipNextTapEvent) {
            mSkipNextTapEvent = false;
            return true;
        }
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onSingleTapConfirmed(e)) {
                return true;
            }
        }
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                boolean handled = mTool.onSingleTapConfirmed(e);
                if (!handled) {
                    next_tm = mTool.getNextToolMode();
                    if (prev_tm != next_tm) {
                        mTool = createTool(next_tm, mTool);
                        prev_tm = next_tm;
                    } else {
                        break;
                    }
                } else {
                    break;
                }

            } while (true);
        }

        // for a single tap, onSingleTapConfirmed event is called after all generic motion events,
        // so we may need to take care of generic motion event handler if new tool is selected
        onGenericMotionEvent(e);

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onPostSingleTapConfirmed()}.
     */
    @Override
    public void onPostSingleTapConfirmed() {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onPostSingleTapConfirmed();
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onSingleTapUp(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onSingleTapUp(e);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return false;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        if (mAnnotIndicatorManger != null && priorEventMode == PDFViewCtrl.PriorEventMode.FLING) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_FLUNG);
        }

        if (mSkipNextTouchEvent) {
            mSkipNextTouchEvent = false;
            return true;
        }
        if (mPreToolManagerListener != null) {
            if (mPreToolManagerListener.onUp(e, priorEventMode)) {
                return true;
            }
        }

        boolean handled = false;
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                handled |= mTool.onUp(e, priorEventMode);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }

        return handled;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onGenericMotionEvent(MotionEvent)}.
     */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mOnGenericMotionEventListener != null) {
            mOnGenericMotionEventListener.onGenericMotionEvent(event);
        }

        if (ShortcutHelper.isLongPress(event)) {
            onLongPress(event);
            return true;
        }

        return mTool != null && ((com.pdftron.pdf.tools.Tool) mTool).onGenericMotionEvent(event);
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onChangePointerIcon(PointerIcon)}.
     */
    @Override
    public void onChangePointerIcon(PointerIcon pointerIcon) {
        if (mOnGenericMotionEventListener != null) {
            mOnGenericMotionEventListener.onChangePointerIcon(pointerIcon);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onConfigurationChanged(Configuration)}.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onConfigurationChanged(newConfig);
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDrawEdgeEffects(Canvas, int, int)}.
     */
    @Override
    public boolean onDrawEdgeEffects(Canvas canvas, int width, int verticalOffset) {
        boolean handled = false;
        if (mTool != null) {
            handled = mTool.onDrawEdgeEffects(canvas, width, verticalOffset);
        }

        return handled;
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onReleaseEdgeEffects()}.
     */
    @Override
    public void onReleaseEdgeEffects() {
        if (mTool != null) {
            mTool.onReleaseEdgeEffects();
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onPullEdgeEffects(int, float)}.
     */
    @Override
    public void onPullEdgeEffects(int which_edge, float delta_distance) {
        if (mTool != null) {
            mTool.onPullEdgeEffects(which_edge, delta_distance);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDoubleTapZoomAnimationBegin()}.
     */
    @Override
    public void onDoubleTapZoomAnimationBegin() {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_ZOOMING);
            mAnnotIndicatorManger.reset(true);
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onDoubleTapZoomAnimationBegin();
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onDoubleTapZoomAnimationEnd()}.
     */
    @Override
    public void onDoubleTapZoomAnimationEnd() {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.updateState(AnnotIndicatorManger.STATE_IS_NORMAL);
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onDoubleTapZoomAnimationEnd();
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#onRenderingFinished()}.
     */
    @Override
    public void onRenderingFinished() {
        if (mOldTools != null) {
            for (Tool tool : mOldTools) {
                tool.onRenderingFinished();
            }
            mOldTools.clear();
        }

        if (mTool != null) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                mTool.onRenderingFinished();
                next_tm = mTool.getNextToolMode();
                if (prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }
            } while (true);
        }
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ToolManager#isCreatingAnnotation()}.
     */
    @Override
    public boolean isCreatingAnnotation() {
        return mTool != null && mTool.isCreatingAnnotation();
    }

    @Override
    public void onDestroy() {
        destroy();
    }

    /**
     * The overload implementation of {@link PDFViewCtrl.ActionCompletedListener#onActionCompleted(Action)}.
     */
    @Override
    public void onActionCompleted(Action action) {
        boolean hasChange = false;
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            hasChange = mPdfViewCtrl.getDoc().hasChangesSinceSnapshot();
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
                if (hasChange) {
                    raiseAnnotationActionEvent();
                }
            }
        }
    }

    /**
     * Gets the current {@link com.pdftron.pdf.tools.ToolManager.Tool} instance.
     */
    public Tool getTool() {
        return mTool;
    }

    /**
     * Sets the current {@link com.pdftron.pdf.tools.ToolManager.Tool} instance.
     * <p>
     * <p>
     * There are two ways to set the current tool. One is via {@link #createTool(ToolModeBase, com.pdftron.pdf.tools.ToolManager.Tool)},
     * which is used during events. The other way is through this method, which
     * allows for setting the tool without any events.
     */
    public void setTool(Tool t) {
        mTool = t;
    }

    /**
     * @return The PDFViewCtrl
     */
    public PDFViewCtrl getPDFViewCtrl() {
        return mPdfViewCtrl;
    }

    /**
     * Gets the undo redo manager
     *
     * @return undo redo manager
     */
    public UndoRedoManager getUndoRedoManger() {
        return mUndoRedoManger;
    }

    /**
     * Enables annotation manager for annotation syncing
     *
     * @param userId the unique identifier of the current user
     */
    public void enableAnnotManager(String userId) {
        enableAnnotManager(userId, null);
    }

    /**
     * Enables annotation manager for annotation syncing
     *
     * @param userId   the unique identifier of the current user
     * @param listener the {@link AnnotManager.AnnotationSyncingListener}
     */
    public void enableAnnotManager(String userId, AnnotManager.AnnotationSyncingListener listener) {
        enableAnnotManager(userId, null, listener);
    }

    /**
     * Enables annotation manager for annotation syncing
     *
     * @param userId       the unique identifier of the current user
     * @param initialAnnot if set, viewer will jump to the set annotation automatically
     * @param listener     the {@link AnnotManager.AnnotationSyncingListener}
     */
    public void enableAnnotManager(String userId, Bundle initialAnnot,
                                   AnnotManager.AnnotationSyncingListener listener) {
        if (null == userId) {
            mAnnotManager = null;
            return;
        }
        try {
            mAnnotManager = new AnnotManager(this, userId, initialAnnot, listener);
        } catch (Exception e) {
            e.printStackTrace();
            mAnnotManager = null;
        }
    }

    /**
     * @return The annotation manager
     */
    public AnnotManager getAnnotManager() {
        return mAnnotManager;
    }

    public void handleGotNewAnnotationData(String uuid, String annotId, String userId, String xfdfCommand, String annotParams) {
    }

    public void handleAnnotationChanged(String lastChange, boolean init, JSONArray annotDataList) {
    }

    public void handleUnreadMessageCountChanged(JSONArray annotDataList) {
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.ToolChangedListener}.
     *
     * @param listener the listener
     */
    public void addToolChangedListener(ToolChangedListener listener) {
        if (mToolChangedListeners == null) {
            mToolChangedListeners = new ArrayList<>();
        }
        if (!mToolChangedListeners.contains(listener)) {
            mToolChangedListeners.add(listener);
        }
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.ToolChangedListener}.
     *
     * @param listener the listener
     */
    public void removeToolChangedListener(ToolChangedListener listener) {
        if (mToolChangedListeners != null) {
            mToolChangedListeners.remove(listener);
        }
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.OnLayoutListener}.
     *
     * @param listener the listener
     */
    public void addOnLayoutListener(OnLayoutListener listener) {
        if (mOnLayoutListeners == null) {
            mOnLayoutListeners = new CopyOnWriteArray<>();
        }

        mOnLayoutListeners.add(listener);
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.OnLayoutListener}.
     *
     * @param listener the listener
     */
    public void removeOnLayoutListener(OnLayoutListener listener) {
        if (mOnLayoutListeners != null) {
            mOnLayoutListeners.remove(listener);
        }
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.PreToolManagerListener}.
     *
     * @param listener the listener
     */
    public void setPreToolManagerListener(PreToolManagerListener listener) {
        mPreToolManagerListener = listener;
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.QuickMenuListener}.
     *
     * @param listener the listener
     */
    public void setQuickMenuListener(QuickMenuListener listener) {
        mQuickMenuListener = listener;
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.AnnotationModificationListener}.
     *
     * @param listener the listener
     */
    public void addAnnotationModificationListener(AnnotationModificationListener listener) {
        if (mAnnotationModificationListeners == null) {
            mAnnotationModificationListeners = new ArrayList<>();
        }
        if (!mAnnotationModificationListeners.contains(listener)) {
            mAnnotationModificationListeners.add(listener);
        }
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.AnnotationModificationListener}.
     *
     * @param listener the listener
     */
    public void removeAnnotationModificationListener(AnnotationModificationListener listener) {
        if (mAnnotationModificationListeners != null) {
            mAnnotationModificationListeners.remove(listener);
        }
    }

    /**
     * Adds the {@link com.pdftron.pdf.tools.ToolManager.PdfDocModificationListener}.
     *
     * @param listener the listener
     */
    public void addPdfDocModificationListener(PdfDocModificationListener listener) {
        if (mPdfDocModificationListeners == null) {
            mPdfDocModificationListeners = new ArrayList<>();
        }
        if (!mPdfDocModificationListeners.contains(listener)) {
            mPdfDocModificationListeners.add(listener);
        }
    }

    /**
     * Removes the {@link com.pdftron.pdf.tools.ToolManager.PdfDocModificationListener}.
     *
     * @param listener the listener
     */
    public void removePdfDocModificationListener(PdfDocModificationListener listener) {
        if (mPdfDocModificationListeners != null) {
            mPdfDocModificationListeners.remove(listener);
        }
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.BasicAnnotationListener}.
     *
     * @param listener the listener
     */
    public void setBasicAnnotationListener(BasicAnnotationListener listener) {
        mBasicAnnotationListener = listener;
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.AdvancedAnnotationListener}.
     *
     * @param listener the listener
     */
    public void setAdvancedAnnotationListener(AdvancedAnnotationListener listener) {
        mAdvancedAnnotationListener = listener;
    }

    /**
     * @param listener
     * @hide
     */
    public void setSpecialAnnotationListener(SpecialAnnotationListener listener) {
        mSpecialAnnotationListener = listener;
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.QuickMenuListener}.
     *
     * @param menuItem the menu item. See: {@link QuickMenuItem}
     */
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {

        if (mQuickMenuListener != null && mQuickMenuListener.onQuickMenuClicked(menuItem)) {
            return true;
        }

        boolean handled = false;
        if (mTool != null && mTool instanceof com.pdftron.pdf.tools.Tool) {
            ToolModeBase prev_tm = mTool.getToolMode(), next_tm;
            do {
                handled = ((com.pdftron.pdf.tools.Tool) mTool).onQuickMenuClicked(menuItem);
                next_tm = mTool.getNextToolMode();

                if (!handled && prev_tm != next_tm) {
                    mTool = createTool(next_tm, mTool);
                    prev_tm = next_tm;
                } else {
                    break;
                }

            } while (true);
        }

        return handled;
    }

    /**
     * a callback to be invoked when quickmenu is shown
     */
    public void onQuickMenuShown() {
        if (mQuickMenuListener != null) {
            mQuickMenuListener.onQuickMenuShown();
        }
    }

    /**
     * a callback to be invoked when quickmenu is dismissed
     */
    public void onQuickMenuDismissed() {
        if (mQuickMenuListener != null) {
            mQuickMenuListener.onQuickMenuDismissed();
        }
    }

    /**
     * Handle annotation
     *
     * @param annot    annotation
     * @param extra    @return true then intercept the subclass function, false otherwise
     * @param toolMode tool mode
     */
    public boolean raiseInterceptAnnotationHandlingEvent(@Nullable Annot annot, Bundle extra, ToolMode toolMode) {
        return mBasicAnnotationListener != null && mBasicAnnotationListener.onInterceptAnnotationHandling(annot, extra, toolMode);
    }

    /**
     * Call this function when a dialog is about to show up.
     */
    public boolean raiseInterceptDialogEvent(AlertDialog dialog) {
        return mBasicAnnotationListener != null && mBasicAnnotationListener.onInterceptDialog(dialog);
    }

    /**
     * Call this function when annotations have been added to the document.
     *
     * @param annots map of annotations added
     */
    public void raiseAnnotationsAddedEvent(Map<Annot, Integer> annots) {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }

        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.onAnnotationsAdded(annots);
            }
        }
    }

    /**
     * Call this function before annotations in the document are modified.
     *
     * @param annots map of annotations about to be modified
     */
    public void raiseAnnotationsPreModifyEvent(Map<Annot, Integer> annots) {
        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.onAnnotationsPreModify(annots);
            }
        }
    }

    /**
     * Call this function when annotations in the document have been modified.
     *
     * @param annots map of annotations modified
     */
    public void raiseAnnotationsModifiedEvent(Map<Annot, Integer> annots, Bundle bundle) {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }

        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.onAnnotationsModified(annots, bundle);
            }
        }
    }

    /**
     * Call this function before annotations are removed from the document.
     *
     * @param annots map of annotations about to be removed
     */
    public void raiseAnnotationsPreRemoveEvent(Map<Annot, Integer> annots) {
        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.onAnnotationsPreRemove(annots);
            }
        }
    }

    /**
     * Call this function when annotations have been removed from the document.
     *
     * @param annots map of annotations removed
     */
    public void raiseAnnotationsRemovedEvent(Map<Annot, Integer> annots) {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }

        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.onAnnotationsRemoved(annots);
            }
        }
    }

    /**
     * Call this function when all annotations in the specified page have been removed from the document.
     *
     * @param pageNum The page number where the annotations are on
     */
    public void raiseAnnotationsRemovedEvent(int pageNum) {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }

        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.onAnnotationsRemovedOnPage(pageNum);
            }
        }
    }

    /**
     * Lets various tools raise the annotation could not be
     * add event from a unified location.
     */
    public void annotationCouldNotBeAdded(String errorMessage) {
        if (mAnnotationModificationListeners != null) {
            for (AnnotationModificationListener listener : mAnnotationModificationListeners) {
                listener.annotationsCouldNotBeAdded(errorMessage);
            }
        }
    }

    /**
     * Call this function when document bookmark has been modified.
     */
    public void raiseBookmarkModified() {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onBookmarkModified();
            }
        }
    }

    /**
     * Call this function when pages of the document have been cropped.
     */
    public void raisePagesCropped() {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onPagesCropped();
            }
        }
    }

    /**
     * Call this function when new pages have been added to the document.
     */
    public void raisePagesAdded(List<Integer> pageList) {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onPagesAdded(pageList);
            }
        }
    }

    /**
     * Call this function when pages have been deleted from the document.
     */
    public void raisePagesDeleted(List<Integer> pageList) {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onPagesDeleted(pageList);
            }
        }
    }

    /**
     * Call this function when pages in the document have been rotated.
     */
    public void raisePagesRotated(List<Integer> pageList) {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onPagesRotated(pageList);
            }
        }
    }

    /**
     * Call this function when a page in the document have been moved to a new position.
     */
    public void raisePageMoved(int from, int to) {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onPageMoved(from, to);
            }
        }
    }

    /**
     * Call this function when all annotations in the document have been removed.
     */
    public void raiseAllAnnotationsRemovedEvent() {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.reset(true);
        }

        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onAllAnnotationsRemoved();
            }
        }
    }

    /**
     * Call this function when an action has taken place that changes the document.
     */
    public void raiseAnnotationActionEvent() {
        if (mPdfDocModificationListeners != null) {
            for (PdfDocModificationListener listener : mPdfDocModificationListeners) {
                listener.onAnnotationAction();
            }
        }
    }

    /**
     * Lets various tools raise the file attachment selected event.
     *
     * @param fileAttachment the selected file attachment
     */
    public void onFileAttachmentSelected(FileAttachment fileAttachment) {
        if (mAdvancedAnnotationListener != null) {
            mAdvancedAnnotationListener.fileAttachmentSelected(fileAttachment);
        }
    }

    /**
     * Lets various tools raise the freehand stylus used for the first time event.
     */
    public void onFreehandStylusUsedFirstTime() {
        if (mAdvancedAnnotationListener != null) {
            mAdvancedAnnotationListener.freehandStylusUsedFirstTime();
        }
    }

    /**
     * Pass the image stamper selected event.
     *
     * @param targetPoint target location to add the image stamp
     */
    public void onImageStamperSelected(PointF targetPoint) {
        if (mAdvancedAnnotationListener != null) {
            mAdvancedAnnotationListener.imageStamperSelected(targetPoint);
        }
    }

    /**
     * Pass the file attachment selected event.
     *
     * @param targetPoint target location to add the image stamp
     */
    public void onAttachFileSelected(PointF targetPoint) {
        if (mAdvancedAnnotationListener != null) {
            mAdvancedAnnotationListener.attachFileSelected(targetPoint);
        }
    }

    /**
     * Pass inline free text editing started event.
     */
    public void onInlineFreeTextEditingStarted() {
        if (mAdvancedAnnotationListener != null) {
            mAdvancedAnnotationListener.freeTextInlineEditingStarted();
        }
    }

    /**
     * @param text     the text
     * @param anchor   the anchor
     * @param isDefine true if in define mode, false if in translation mode
     * @hide Pass the define and translate event.
     */
    public void defineTranslateSelected(String text, RectF anchor, Boolean isDefine) {
        if (mSpecialAnnotationListener != null) {
            mSpecialAnnotationListener.defineTranslateSelected(text, anchor, isDefine);
        }
    }

    /**
     * Pass the ink edit selected event.
     *
     * @param inkAnnot the ink annotation to be modified
     */
    public void onInkEditSelected(Annot inkAnnot) {
        if (mAnnotationToolbarListener != null) {
            mAnnotationToolbarListener.onInkEditSelected(inkAnnot);
        }
    }

    /**
     * Called when the annotation toolbar should open for a tool
     *
     * @param mode the tool mode
     */
    public void onOpenAnnotationToolbar(ToolMode mode) {

        if (mAnnotationToolbarListener != null) {
            mAnnotationToolbarListener.onOpenAnnotationToolbar(mode);
        }

    }

    /**
     * Called when the edit toolbar should open for a tool
     *
     * @param mode the tool mode
     */
    public void onOpenEditToolbar(ToolMode mode) {
        if (mAnnotationToolbarListener != null) {
            mAnnotationToolbarListener.onOpenEditToolbar(mode);
        }
    }

    /**
     * Gets the annotation toolbar height
     *
     * @return annotation toolbar height in pixel, -1 if annotation toolbar not visible
     */
    public int getAnnotationToolbarHeight() {
        if (mAnnotationToolbarListener != null) {
            return mAnnotationToolbarListener.annotationToolbarHeight();
        }
        return -1;
    }

    /**
     * Gets the toolbar height
     *
     * @return toolbar height in pixel, -1 if annotation toolbar not visible
     */
    public int getToolbarHeight() {
        if (mAnnotationToolbarListener != null) {
            return mAnnotationToolbarListener.toolbarHeight();
        }
        return -1;
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.AnnotationToolbarListener}.
     *
     * @param annotationToolbarListener the listener
     */
    public void setAnnotationToolbarListener(AnnotationToolbarListener annotationToolbarListener) {
        mAnnotationToolbarListener = annotationToolbarListener;
    }

    /**
     * Sets the {@link com.pdftron.pdf.tools.ToolManager.OnGenericMotionEventListener}.
     *
     * @param onGenericMotionEventListener the listener
     */
    public void setOnGenericMotionEventListener(OnGenericMotionEventListener onGenericMotionEventListener) {
        mOnGenericMotionEventListener = onGenericMotionEventListener;
    }

    public void setExternalAnnotationManagerListener(ExternalAnnotationManagerListener externalAnnotationManagerListener) {
        mExternalAnnotationManagerListener = externalAnnotationManagerListener;
    }

    /**
     * Indicates whether to use/show the built-in page number indicator.
     *
     * @param visible true to show the built-in page number indicator, false
     *                otherwise.
     */
    public void setBuiltInPageNumberIndicatorVisible(boolean visible) {
        mPageNumberIndicatorVisible = visible;
    }

    /**
     * Indicates whether to use/show the built-in page number indicator.
     *
     * @return true to show the built-in page number indicator, false
     * otherwise.
     */
    public boolean isBuiltInPageNumberIndicatorVisible() {
        return mPageNumberIndicatorVisible;
    }

    /**
     * Indicates whether the file associated with PDFViewCtrl is read-only.
     */
    public void setReadOnly(boolean readOnly) {
        mReadOnly = readOnly;
        ToolMode[] editableToolModes = new ToolManager.ToolMode[]{
            ToolMode.ANNOT_EDIT,
            ToolMode.ANNOT_EDIT_LINE,
            ToolMode.ANNOT_EDIT_TEXT_MARKUP,
            ToolMode.ANNOT_EDIT_ADVANCED_SHAPE,
            ToolMode.ANNOT_EDIT_RECT_GROUP,
            ToolMode.FORM_FILL,
            ToolMode.RECT_LINK,
            ToolMode.INK_CREATE,
            ToolMode.FREE_HIGHLIGHTER,
            ToolMode.LINE_CREATE,
            ToolMode.ARROW_CREATE,
            ToolMode.RULER_CREATE,
            ToolMode.POLYLINE_CREATE,
            ToolMode.RECT_CREATE,
            ToolMode.OVAL_CREATE,
            ToolMode.SOUND_CREATE,
            ToolMode.FILE_ATTACHMENT_CREATE,
            ToolMode.POLYGON_CREATE,
            ToolMode.CLOUD_CREATE,
            ToolMode.TEXT_CREATE,
            ToolMode.CALLOUT_CREATE,
            ToolMode.TEXT_ANNOT_CREATE,
            ToolMode.TEXT_LINK_CREATE,
            ToolMode.FORM_CHECKBOX_CREATE,
            ToolMode.FORM_SIGNATURE_CREATE,
            ToolMode.FORM_TEXT_FIELD_CREATE,
            ToolMode.FORM_RADIO_GROUP_CREATE,
            ToolMode.SIGNATURE,
            ToolMode.STAMPER,
            ToolMode.RUBBER_STAMPER,
            ToolMode.INK_ERASER,
            ToolMode.TEXT_HIGHLIGHT,
            ToolMode.TEXT_SQUIGGLY,
            ToolMode.TEXT_STRIKEOUT,
            ToolMode.TEXT_UNDERLINE,
            ToolMode.TEXT_REDACTION
        };
        if (readOnly) {
            // first let's store the previously disabled modes so we can recover later
            if (mDisabledToolModes != null && mDisabledToolModes.size() > 0) {
                if (mDisabledToolModesSave == null) {
                    mDisabledToolModesSave = new HashSet<>();
                }
                mDisabledToolModesSave.clear();
                mDisabledToolModesSave.addAll(mDisabledToolModes);
            }

            disableToolMode(editableToolModes);
        } else {
            enableToolMode(editableToolModes);

            // now recover from the previous disabled modes
            if (mDisabledToolModesSave != null && mDisabledToolModesSave.size() > 0) {
                disableToolMode(mDisabledToolModesSave.toArray(new ToolManager.ToolMode[mDisabledToolModesSave.size()]));
                mDisabledToolModesSave.clear();
            }
        }
    }

    /**
     * Gets whether the file associated with PDFViewCtrl is read-only.
     */
    public boolean isReadOnly() {
        return mReadOnly;
    }

    /**
     * Sets whether to check annotation author permission
     *
     * @param enable if true, annotation created by user A cannot be modified by user B,
     *               else anyone can modify any annotation
     */
    public void setAnnotPermissionCheckEnabled(boolean enable) {
        mCanCheckAnnotPermission = enable;
    }

    /**
     * Gets whether annotation author permission is enabled
     *
     * @return true if enabled, false otherwise
     */
    public boolean isAnnotPermissionCheckEnabled() {
        return mCanCheckAnnotPermission;
    }

    /**
     * Sets the user ID used for checking whether an annotation is created by current user.
     *
     * @param authorId author identification
     */
    public void setAuthorId(String authorId) {
        mAuthorId = authorId;
    }

    /**
     * Gets the user ID used for checking whether an annotation is created by current user.
     *
     * @return auther identification
     */
    public String getAuthorId() {
        return mAuthorId;
    }

    /**
     * Sets the selected annotation identification
     *
     * @param annot   the annotation
     * @param pageNum the page number where the annotation is on
     */
    public void setSelectedAnnot(Annot annot, int pageNum) {
        try {
            mSelectedAnnotId = annot == null ? null : annot.getUniqueID().getAsPDFText();
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        mSelectedAnnotPageNum = pageNum;
        if (mBasicAnnotationListener != null) {
            if (null == annot) {
                mBasicAnnotationListener.onAnnotationUnselected();
            } else {
                mBasicAnnotationListener.onAnnotationSelected(annot, pageNum);
            }
        }
    }

    /**
     * Gets the identification of the selected annotation
     *
     * @return identification
     */
    public String getSelectedAnnotId() {
        return mSelectedAnnotId;
    }

    /**
     * Deselects all annotations
     */
    public void deselectAll() {
        mTool.onClose();
        // keep current annotation mode
        setTool(createTool(getTool().getToolMode(), null));
        mPdfViewCtrl.invalidate();
    }

    /**
     * Selects the annotation.
     *
     * @param annotId The annotation ID
     * @param pageNum The page number where the annotation is on
     */
    public void selectAnnot(String annotId, int pageNum) {
        Annot annot = ViewerUtils.getAnnotById(mPdfViewCtrl, annotId, pageNum);
        if (null != annot) {
            selectAnnot(annot, pageNum);
        }
    }

    /**
     * Selects an annotation
     *
     * @param annot   the annotation
     * @param pageNum the page number where the annotation is on
     */
    public void selectAnnot(
        @Nullable Annot annot,
        int pageNum) {

        try {
            if (annot == null || !annot.isValid()) {
                return;
            }

            ((com.pdftron.pdf.tools.Tool) mTool).selectAnnot(annot, pageNum);
            ToolMode mode = ToolMode.ANNOT_EDIT;
            int annotType = annot.getType();
            if (annotType == Annot.e_Line) {
                mode = ToolMode.ANNOT_EDIT_LINE;
            } else if (annotType == Annot.e_Polyline
                || annotType == Annot.e_Polygon
                || AnnotUtils.isCallout(annot)) {
                mode = ToolMode.ANNOT_EDIT_ADVANCED_SHAPE;
            } else if (annotType == Annot.e_Highlight
                || annotType == Annot.e_StrikeOut
                || annotType == Annot.e_Underline
                || annotType == Annot.e_Squiggly) {
                mode = ToolMode.ANNOT_EDIT_TEXT_MARKUP;
            }

            if (mTool.getToolMode() != mode) {
                mTool = createTool(mode, mTool);
                if (mTool instanceof AnnotEdit ||
                    mTool instanceof AnnotEditTextMarkup) {
                    ((com.pdftron.pdf.tools.Tool) mTool).selectAnnot(annot, pageNum);
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Re-selects the last selected annotation
     */
    public void reselectAnnot() {
        if (null != mSelectedAnnotId && mSelectedAnnotPageNum > 0 && mPdfViewCtrl.getDoc() != null) {
            Annot annot = ViewerUtils.getAnnotById(mPdfViewCtrl, mSelectedAnnotId, mSelectedAnnotPageNum);
            if (annot != null) {
                selectAnnot(annot, mSelectedAnnotPageNum);
            }
        }
    }

    /**
     * Sets whether the quick menu is just closed.
     *
     * @param closed True if the quick menu is just closed
     */
    public void setQuickMenuJustClosed(boolean closed) {
        mQuickMenuJustClosed = closed;
    }

    /**
     * @return True if the quick menu is just closed
     */
    public boolean isQuickMenuJustClosed() {
        boolean result = mQuickMenuJustClosed;
        mQuickMenuJustClosed = false;
        return result;
    }

    /**
     * Sets whether to show author dialog the first time when user annotates.
     */
    public void setShowAuthorDialog(boolean show) {
        mShowAuthorDialog = show;
    }

    /**
     * Gets whether to show author dialog the first time when user annotates.
     */
    public boolean isShowAuthorDialog() {
        return mShowAuthorDialog;
    }

    /**
     * Sets whether the TextMarkup annotations are compatible with Adobe
     * (Adobe's quads don't follow the specification, but they don't handle quads that do).
     */
    public void setTextMarkupAdobeHack(boolean enable) {
        mTextMarkupAdobeHack = enable;
    }

    /**
     * Gets whether the TextMarkup annotations are compatible with Adobe
     * (Adobe's quads don't follow the specification, but they don't handle quads that do).
     */
    public boolean isTextMarkupAdobeHack() {
        return mTextMarkupAdobeHack;
    }

    /**
     * Sets whether to copy annotated text to note
     *
     * @param enable enable copy annotated text to note
     */
    public void setCopyAnnotatedTextToNoteEnabled(boolean enable) {
        mCopyAnnotatedTextToNote = enable;
    }

    /**
     * Gets whether to copy annotated text to note
     *
     * @return true if enabled, false otherwise
     */
    public boolean isCopyAnnotatedTextToNoteEnabled() {
        return mCopyAnnotatedTextToNote;
    }

    /**
     * Sets whether to use stylus to draw without entering ink tool
     *
     * @param stylusAsPen enable inking with stylus in pan mode
     */
    public void setStylusAsPen(boolean stylusAsPen) {
        mStylusAsPen = stylusAsPen;
    }

    /**
     * Gets whether to use stylus to draw without entering ink tool
     *
     * @return true if enabled, false otherwise
     */
    public boolean isStylusAsPen() {
        return mStylusAsPen;
    }

    /**
     * Sets whether to smooth ink annotation
     *
     * @param enable enable ink smoothing
     */
    public void setInkSmoothingEnabled(boolean enable) {
        mInkSmoothing = enable;
    }

    /**
     * Gets whether to smooth ink annotation
     *
     * @return true if enabled, false otherwise
     */
    public boolean isInkSmoothingEnabled() {
        return mInkSmoothing;
    }

    /**
     * Sets list of free text fonts to have as
     * options in the properties popup.
     */
    public void setFreeTextFonts(Set<String> freeTextFonts) {
        mFreeTextFonts = freeTextFonts;
    }

    /**
     * Gets the list of free text fonts to have as
     * options in the properties popup
     */
    public Set<String> getFreeTextFonts() {
        return mFreeTextFonts;
    }

    /**
     * Sets whether night mode is enabled
     *
     * @param isNightMode enable night mode for tools
     */
    public void setNightMode(boolean isNightMode) {
        mIsNightMode = isNightMode;
        if (mTool != null) {
            mTool.onNightModeUpdated(isNightMode);
        }
    }

    /**
     * Gets whether night mode is enabled for tools
     *
     * @return true if enabled, false otherwise
     */
    public boolean isNightMode() {
        return mIsNightMode;
    }

    /**
     * @return the list of old tools
     */
    public ArrayList<Tool> getOldTools() {
        if (mOldTools == null) {
            mOldTools = new ArrayList<>();
        }
        return mOldTools;
    }

    /**
     * Initialize system Text To Speech
     */
    public void initTTS() {
        try {
            // initialize text to speech
            mTTS = new TextToSpeech(mPdfViewCtrl.getContext().getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {

                }
            });
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    /**
     * Gets the Text To Speech object
     *
     * @return TextToSpeech object
     */
    public TextToSpeech getTTS() {
        return mTTS;
    }

    /**
     * Sets whether editing ink annotation should open the annotation toolbar
     *
     * @param editInkAnnots if true, can edit ink annotation via annotation toolbar, false otherwise
     */
    public void setEditInkAnnots(boolean editInkAnnots) {
        mEditInkAnnots = editInkAnnots;
    }

    /**
     * Gets whether editing ink annotation should open the annotation toolbar
     *
     * @return true if can edit ink annotation via annotation toolbar, false otherwise
     */
    public boolean editInkAnnots() {
        return mEditInkAnnots;
    }

    /**
     * Sets whether to enable image stamper tool
     *
     * @param addImageStamperTool if true, image stamper tool will be available
     * @Deprecated Use {@link Tool#disableToolMode(ToolMode[])}
     */
    public void setAddImageStamperTool(boolean addImageStamperTool) {
        mAddImageStamperTool = addImageStamperTool;
    }

    /**
     * Gets whether image stamper tool is enabled
     *
     * @return true if enabled, false otherwise
     * @Deprecated Use {@link Tool#isToolModeDisabled(ToolMode)}
     */
    public boolean getAddImageStamperTool() {
        return mAddImageStamperTool;
    }

    /**
     * Sets whether edit tool will open when tools selected in pan quick menu
     *
     * @param canOpenEditToolbarFromPan if true, click tools from quick menu will
     *                                  open the edit toolbar in pan mode,
     *                                  false otherwise
     */
    public void setCanOpenEditToolbarFromPan(boolean canOpenEditToolbarFromPan) {
        mCanOpenEditToolbarFromPan = canOpenEditToolbarFromPan;
    }

    /**
     * Gets whether edit tool will open when tools selected in pan quick menu
     *
     * @return true if click tools from quick menu will open the edit toolbar in pan mode,
     * false otherwise
     */
    public boolean isOpenEditToolbarFromPan() {
        return mCanOpenEditToolbarFromPan;
    }

    /**
     * Sets whether auto select annotation after annotation is created
     *
     * @param autoSelect if true, after creating annotation, it will auto select it and show quick menu
     */
    public void setAutoSelectAnnotation(boolean autoSelect) {
        mIsAutoSelectAnnotation = autoSelect;
    }

    /**
     * Gets auto select annotation after annotation is created
     *
     * @return true if auto select, false otherwise
     */
    public boolean isAutoSelectAnnotation() {
        return mIsAutoSelectAnnotation;
    }


    /**
     * Sets whether disable showing the long press quick menu
     *
     * @param disabled if true, disable showing the long press quick menu
     */
    public void setDisableQuickMenu(boolean disabled) {
        mDisableQuickMenu = disabled;
    }

    /**
     * Sets whether can double tap to zoom the viewer
     *
     * @param doubleTapToZoom if true, can double tap to zoom, false otherwise
     */
    public void setDoubleTapToZoom(boolean doubleTapToZoom) {
        mDoubleTapToZoom = doubleTapToZoom;
    }

    /**
     * Gets whether can double tap to zoom
     *
     * @return true if can double tap to zoom, false otherwise
     */
    public boolean isDoubleTapToZoom() {
        return mDoubleTapToZoom;
    }

    /**
     * Gets whether can auto resize free text when editing
     * @return true if can auto resize, false otherwise
     */
    public boolean isAutoResizeFreeText() {
        return mAutoResizeFreeText;
    }

    /**
     * Sets whether can auto resize free text when editing
     * @param autoResizeFreeText if true can auto resize, false otherwise
     */
    public void setAutoResizeFreeText(boolean autoResizeFreeText) {
        this.mAutoResizeFreeText = autoResizeFreeText;
    }


    /**
     * Gets whether annotation editing is real time
     * @return true if real time, false otherwise
     */
    public boolean isRealTimeAnnotEdit() {
        return mRealTimeAnnotEdit;
    }

    /**
     * Sets whether annotation editing is real time
     * @param realTimeAnnotEdit true if real time, false otherwise
     */
    public void setRealTimeAnnotEdit(boolean realTimeAnnotEdit) {
        this.mRealTimeAnnotEdit = realTimeAnnotEdit;
    }

    /**
     * Gets whether can edit FreeText on tap
     * @return true if start edit on tap, false otherwise
     */
    public boolean isEditFreeTextOnTap() {
        return mEditFreeTextOnTap;
    }

    /**
     * Sets whether can edit FreeText on tap
     * @param editFreeTextOnTap true if start edit on tap, false otherwise
     */
    public void setEditFreeTextOnTap(boolean editFreeTextOnTap) {
        this.mEditFreeTextOnTap = editFreeTextOnTap;
    }

    /**
     * Gets whether quick menu is disabled
     *
     * @return true if disabled, false otherwise
     */
    public boolean isQuickMenuDisabled() {
        return mDisableQuickMenu;
    }


    /**
     * Skips the next tap event.
     */
    public void skipNextTapEvent() {
        this.mSkipNextTouchEvent = true;
        this.mSkipNextTapEvent = true;
    }

    /**
     * @return True if next tap event is skipped.
     */
    public boolean isSkipNextTapEvent() {
        return this.mSkipNextTouchEvent || this.mSkipNextTapEvent;
    }

    /**
     * Resets skipping the next tap event.
     */
    public void resetSkipNextTapEvent() {
        this.mSkipNextTouchEvent = false;
        this.mSkipNextTapEvent = false;
    }

    /**
     * @return The cache file name
     */
    public String getCacheFileName() {
        return mCacheFileName;
    }

    /**
     * Sets the cache file name
     *
     * @param tag The tag which will be used to generate cache file name
     */
    public void setCacheFileName(String tag) {
        mCacheFileName = String.valueOf(tag.hashCode());
    }

    /**
     * @return The free text cache file name
     */
    public String getFreeTextCacheFileName() {
        return "freetext_" + mCacheFileName + ".srl";
    }

    /**
     * @return True if can resume PDF Doc without reloading
     */
    public boolean canResumePdfDocWithoutReloading() {
        return mCanResumePdfDocWithoutReloading;
    }

    /**
     * Sets if can resume PDF Doc without reloading.
     *
     * @param canResumePdfDocWithoutReloading True if can resume PDF Doc without reloading
     */
    public void setCanResumePdfDocWithoutReloading(boolean canResumePdfDocWithoutReloading) {
        this.mCanResumePdfDocWithoutReloading = canResumePdfDocWithoutReloading;
    }

    /**
     * Disables annotation editing by type.
     *
     * @param annotTypes annot types to be disabled
     */
    public void disableAnnotEditing(Integer[] annotTypes) {
        ToolConfig.getInstance().disableAnnotEditing(annotTypes);
    }

    /**
     * Enables annotation editing by type.
     *
     * @param annotTypes annot types to be enabled
     */
    public void enableAnnotEditing(Integer[] annotTypes) {
        ToolConfig.getInstance().disableAnnotEditing(annotTypes);
    }

    /**
     * Checks whether the editing of an annot type is disabled.
     *
     * @param annotType The annot type
     * @return True if editing of the annot type is disabled
     */
    public boolean isAnnotEditingDisabled(int annotType) {
        return ToolConfig.getInstance().isAnnotEditingDisabled(annotType);
    }

    /**
     * Disables tool modes. Pan tool cannot be disabled.
     *
     * @param toolModes tool modes to be disabled
     */
    public void disableToolMode(ToolMode[] toolModes) {
        if (mDisabledToolModes == null) {
            mDisabledToolModes = new HashSet<>();
        }
        Collections.addAll(mDisabledToolModes, toolModes);
    }

    /**
     * Enables tool modes.
     *
     * @param toolModes tool modes to be enabled
     */
    public void enableToolMode(ToolMode[] toolModes) {
        if (mDisabledToolModes == null) {
            return;
        }
        List<ToolMode> toolModeList = Arrays.asList(toolModes);
        mDisabledToolModes.removeAll(toolModeList);
    }

    /**
     * Checks whether the specified tool mode is disabled.
     *
     * @param toolMode The tool mode
     * @return True if the tool mode is disabled
     */
    public boolean isToolModeDisabled(ToolMode toolMode) {
        if (toolMode == ToolMode.STAMPER) {
            if (!getAddImageStamperTool()) {
                return false;
            }
        }

        return mDisabledToolModes != null && mDisabledToolModes.contains(toolMode);
    }

    /**
     * @return The generated key
     */
    public String generateKey() {
        if (mExternalAnnotationManagerListener != null) {
            return mExternalAnnotationManagerListener.generateKey();
        }
        return null;
    }

    /**
     * Cleans up resources.
     */
    public void destroy() {
        if (mAnnotIndicatorManger != null) {
            mAnnotIndicatorManger.cleanup();
        }
        mCurrentActivity = null;
    }

    /**
     * Copy on write array. This array is not thread safe, and only one loop can
     * iterate over this array at any given time. This class avoids allocations
     * until a concurrent modification happens.
     * <p>
     * Usage:
     * <p>
     * CopyOnWriteArray.Access<MyData> access = array.start();
     * try {
     * for (int i = 0; i < access.size(); i++) {
     * MyData d = access.get(i);
     * }
     * } finally {
     * access.end();
     * }
     */
    // This class is taken from ViewTreeObserver class
    private static class CopyOnWriteArray<T> {
        private ArrayList<T> mData = new ArrayList<T>();
        private ArrayList<T> mDataCopy;

        private final Access<T> mAccess = new Access<T>();

        private boolean mStart;

        static class Access<T> {
            private ArrayList<T> mData;
            private int mSize;

            T get(int index) {
                return mData.get(index);
            }

            int size() {
                return mSize;
            }
        }

        CopyOnWriteArray() {
        }

        private ArrayList<T> getArray() {
            if (mStart) {
                if (mDataCopy == null) mDataCopy = new ArrayList<T>(mData);
                return mDataCopy;
            }
            return mData;
        }

        Access<T> start() {
            if (mStart) throw new IllegalStateException("Iteration already started");
            mStart = true;
            mDataCopy = null;
            mAccess.mData = mData;
            mAccess.mSize = mData.size();
            return mAccess;
        }

        void end() {
            if (!mStart) throw new IllegalStateException("Iteration not started");
            mStart = false;
            if (mDataCopy != null) {
                mData = mDataCopy;
                mAccess.mData.clear();
                mAccess.mSize = 0;
            }
            mDataCopy = null;
        }

        int size() {
            return getArray().size();
        }

        void add(T item) {
            getArray().add(item);
        }

        void addAll(CopyOnWriteArray<T> array) {
            getArray().addAll(array.mData);
        }

        void remove(T item) {
            getArray().remove(item);
        }

        void clear() {
            getArray().clear();
        }
    }

    /**
     * Sets whether show pop up dialog when sticky note is added/ selected/ etc.
     *
     * @param show if true, show sticky note pop up when sticky note is created/ prepare to modify
     */
    public void setStickyNoteShowPopup(boolean show) {
        mStickyNoteShowPopup = show;
    }

    /**
     * Gets whether shows stick note pop up dialog
     *
     * @return true if shows, false otherwise
     */
    public boolean getStickyNoteShowPopup() {
        return mStickyNoteShowPopup;
    }

    /**
     * Add a custom tool to tool class map
     *
     * @param tool customized tool
     */
    public void addCustomizedTool(com.pdftron.pdf.tools.Tool tool) {
        if (null == mCustomizedToolClassMap) {
            mCustomizedToolClassMap = new HashMap<>();
        }
        mCustomizedToolClassMap.put(tool.getToolMode(), tool.getClass());
    }

    /**
     * Add a custom tool to tool class map
     *
     * @param toolClassMap customized tool mode and class map
     */
    public void addCustomizedTool(HashMap<ToolModeBase, Class<? extends com.pdftron.pdf.tools.Tool>> toolClassMap) {
        if (null == mCustomizedToolClassMap) {
            mCustomizedToolClassMap = new HashMap<>();
        }
        mCustomizedToolClassMap.putAll(toolClassMap);
    }

    /**
     * Add a custom tool to tool class map
     *
     * @param tool   customized tool.
     * @param params parameter for instantiate tool
     */
    public void addCustomizedTool(com.pdftron.pdf.tools.Tool tool, Object... params) {
        addCustomizedTool(tool);
        if (null == mCustomizedToolParamMap) {
            mCustomizedToolParamMap = new HashMap<>();
        }
        mCustomizedToolParamMap.put(tool.getToolMode(), params);
    }

    /**
     * Add a custom tool to tool class map
     *
     * @param toolParamMap tool mode and tool initialize parameter map
     */
    public void addCustomizedToolParams(HashMap<ToolModeBase, Object[]> toolParamMap) {
        if (null == mCustomizedToolParamMap) {
            mCustomizedToolParamMap = new HashMap<>();
        }
        mCustomizedToolParamMap.putAll(toolParamMap);
    }

    /**
     * set default tool class
     *
     * @param cLass default tool class
     */
    public void setDefaultToolCLass(Class<? extends Pan> cLass) {
        mDefaultToolClass = cLass;
    }

    /**
     * Show built in page number indicator
     */
    public void showBuiltInPageNumber() {
        if (!mPageNumberIndicatorVisible || (mPageIndicatorPopup != null && mPageIndicatorPopup.isShowing())) {
            return;
        }

        PageIndicatorLayout pageIndicator;
        if (mPageIndicatorPopup != null) {
            pageIndicator = (PageIndicatorLayout) mPageIndicatorPopup.getContentView();
        } else {
            pageIndicator = new PageIndicatorLayout(mPdfViewCtrl.getContext());
            pageIndicator.setPdfViewCtrl(mPdfViewCtrl);
            pageIndicator.setAutoAdjustPosition(false);
            pageIndicator.setVisibility(View.VISIBLE);
            ViewGroup.MarginLayoutParams mlp = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pageIndicator.setLayoutParams(mlp);
            pageIndicator.setOnPdfViewCtrlVisibilityChangeListener(new PageIndicatorLayout.OnPDFViewVisibilityChanged() {
                @Override
                public void onPDFViewVisibilityChanged(int prevVisibility, int currVisibility) {
                    if (currVisibility != View.VISIBLE) {
                        hideBuiltInPageNumber();
                    }
                }
            });

            // initialize page indicator popup
            mPageIndicatorPopup = new PopupWindow(pageIndicator, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int[] position = pageIndicator.calculateAutoAdjustPosition();
        mPageIndicatorPopup.showAtLocation(mPdfViewCtrl, Gravity.TOP | Gravity.START, position[0], position[1]);
        pageIndicator.onPageChange(0, mPdfViewCtrl.getCurrentPage(), PDFViewCtrl.PageChangeState.END);
    }

    /**
     * Hide built in page number indicator
     */
    public void hideBuiltInPageNumber() {
        if (mPageNumberIndicatorVisible && mPageIndicatorPopup != null) {
            mPageIndicatorPopup.dismiss();
        }
    }

    /**
     * Whether ToolManager is currently attached with an activity
     */
    public boolean hasCurrentActivity() {
        return getCurrentActivity() != null;
    }

    /**
     * Set the activity to which the ToolManager is currently attached, or {@code null} if not attached.
     */
    public void setCurrentActivity(@Nullable FragmentActivity activity) {
        mCurrentActivity = new WeakReference<>(activity);
    }

    /**
     * Get the activity to which the ToolManager is currently attached, or {@code null} if not attached.
     * DO NOT HOLD LONG-LIVED REFERENCES TO THE OBJECT RETURNED BY THIS METHOD, AS THIS WILL CAUSE
     * MEMORY LEAKS.
     */
    public @Nullable
    FragmentActivity getCurrentActivity() {
        FragmentActivity activity = null;
        if (mPdfViewCtrl != null && (mPdfViewCtrl.getContext() instanceof FragmentActivity)) {
            activity = (FragmentActivity) mPdfViewCtrl.getContext();
        } else if (mCurrentActivity != null && mCurrentActivity.get() != null) {
            activity = mCurrentActivity.get();
        }
        return activity;
    }

    /**
     * Overload implementation of {@link LoadSystemFontAsyncTask.onFinishListener#onFinish()}
     */
    public void onSystemFontsFinishedLoaded() {
        mSystemFontsLoaded = true;
        if (mPendingFreeText == null || mPdfViewCtrl == null) {
            return;
        }
        CommonToast.showText(mPdfViewCtrl.getContext(), R.string.font_loaded);
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;
            for (FreeTextInfo freeTextInfo : mPendingFreeText) {
                freeTextInfo.setFont(mPdfViewCtrl.getContext(), mPdfViewCtrl);
            }
            mPendingFreeText = null;
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * Adds free text information to pending list for loading system fonts
     *
     * @param freeTextInfo the free text information
     * @param showToast    true then show toast message about font not loaded
     */
    public void addPendingFreeText(FreeTextInfo freeTextInfo, boolean showToast) {
        if (!mSystemFontsLoaded) {
            if (showToast) {
                CommonToast.showText(mPdfViewCtrl.getContext(), R.string.font_not_loaded);
            }
            if (mPendingFreeText == null) {
                mPendingFreeText = new ArrayList<>();
            }
            mPendingFreeText.add(freeTextInfo);
        }
    }

}
