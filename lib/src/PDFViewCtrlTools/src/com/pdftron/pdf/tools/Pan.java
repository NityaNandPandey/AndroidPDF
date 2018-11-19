//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.net.Uri;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.config.ToolConfig;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.AnnotationClipboardHelper;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;

/**
 * Pan tool implements the following functions:
 * <ol>
 * <li>Select the hit annotation and switch to annotation edit tool on single tap event;</li>
 * <li>Bring up annotation creation menu upon long press event.</li>
 * </ol>
 */
@SuppressWarnings("WeakerAccess")
@Keep
public class Pan extends Tool {
    private Paint mPaint;
    boolean mSuppressSingleTapConfirmed;
    private QuickMenuItem mPasteMenuEntry;

    private PointF mTargetPoint;
    private RectF mAnchor;

    /**
     * Class constructor
     */
    public Pan(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mSuppressSingleTapConfirmed = false;

        // Enable page turning (in non-continuous page presentation mode).
        // It is only turned on in Pan tool.
        mPdfViewCtrl.setBuiltInPageSlidingEnabled(true);
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.PAN;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Unknown;
    }

    /**
     * The overload implementation of {@link Tool#onCreate()}.
     */
    @Override
    public void onCreate() {
        mPasteMenuEntry = new QuickMenuItem(mPdfViewCtrl.getContext(), R.id.qm_paste, QuickMenuItem.FIRST_ROW_MENU);
        mPasteMenuEntry.setTitle(R.string.tools_qm_paste);
    }

    @Override
    protected QuickMenu createQuickMenu() {
        QuickMenu quickMenu = super.createQuickMenu();
        quickMenu.inflate(R.menu.pan);

        if (AnnotationClipboardHelper.isItemCopied(mPdfViewCtrl.getContext())){
            QuickMenuItem menuItem = (QuickMenuItem) quickMenu.getMenu().add(R.id.qm_first_row_group, R.id.qm_paste, QuickMenuItem.ORDER_START, R.string.tools_qm_paste);
            menuItem.setIcon(R.drawable.ic_content_paste_black_24dp);
        }
        quickMenu.addMenuEntries(((QuickMenuBuilder) quickMenu.getMenu()).getMenuItems(), 4);
        quickMenu.setDividerVisibility(View.INVISIBLE);
        return quickMenu;
    }

    /**
     * The overload implementation of {@link Tool#onDown(MotionEvent)}.
     */
    @Override
    public boolean onDown(MotionEvent e) {
        super.onDown(e);
        boolean stylusAsPen = ((ToolManager) mPdfViewCtrl.getToolManager()).isStylusAsPen();
        if (stylusAsPen && e.getPointerCount() == 1 && e.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            mNextToolMode = ToolMode.INK_CREATE;
        }
        if (mNextToolMode == ToolMode.PAN && ShortcutHelper.isTextSelect(e)) {
            int x = (int) (e.getX() + 0.5);
            int y = (int) (e.getY() + 0.5);
            if (!Utils.isNougat() || mPdfViewCtrl.isThereTextInRect(x - 1, y - 1, x + 1, y + 1)) {
                mNextToolMode = ToolMode.TEXT_SELECT;
            }
        }

        return false;
    }

    /**
     * The overload implementation of {@link Tool#onMove(MotionEvent, MotionEvent, float, float)}.
     */
    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);
        mJustSwitchedFromAnotherTool = false;
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onUp(MotionEvent, PDFViewCtrl.PriorEventMode)}.
     */
    @Override
    public boolean onUp(MotionEvent e, PDFViewCtrl.PriorEventMode priorEventMode) {
        super.onUp(e, priorEventMode);
        mJustSwitchedFromAnotherTool = false;
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onSingleTapConfirmed(MotionEvent)}.
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        super.onSingleTapConfirmed(e);
        showTransientPageNumber();

        if (mSuppressSingleTapConfirmed) {
            mSuppressSingleTapConfirmed = false;
            mJustSwitchedFromAnotherTool = false;
            return false;
        }

        int x = (int) (e.getX() + 0.5);
        int y = (int) (e.getY() + 0.5);
        selectAnnot(x, y);

        if (mAnnot != null) {
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                mNextToolMode = safeSetNextToolMode(ToolConfig.getInstance().getAnnotationHandlerToolMode(AnnotUtils.getAnnotType(mAnnot)));
                mAnnotPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
        } else {
            mNextToolMode = ToolMode.PAN;

            // If PDFViewCtrl.setUrlExtraction is enabled, do the test for a possible link here.
            try {
                PDFViewCtrl.LinkInfo linkInfo = mPdfViewCtrl.getLinkAt(x, y);
                if (linkInfo != null) {
                    String url = linkInfo.getURL();
                    if (url.startsWith("mailto:") || android.util.Patterns.EMAIL_ADDRESS.matcher(url).matches()) {
                        if (url.startsWith("mailto:")) {
                            url = url.substring(7);
                        }
                        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", url, null));
                        mPdfViewCtrl.getContext().startActivity(Intent.createChooser(i, getStringFromResId(R.string.tools_misc_sendemail)));
                    } else {
                        // ACTION_VIEW needs the address to have http or https
                        if (!url.startsWith("https://") && !url.startsWith("http://")) {
                            url = "http://" + url;
                        }

                        final String finalUrl = url;
                        // Ask the user if he/she really want to open the link in an external browser
                        AlertDialog.Builder builder = new AlertDialog.Builder(mPdfViewCtrl.getContext());
                        builder.setTitle(R.string.tools_dialog_open_web_page_title)
                            .setMessage(String.format(getStringFromResId(R.string.tools_dialog_open_web_page_message), finalUrl))
                            .setIcon(null)
                            .setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
                                    mPdfViewCtrl.getContext().startActivity(Intent.createChooser(i, getStringFromResId(R.string.tools_misc_openwith)));
                                }
                            })
                            .setNegativeButton(R.string.cancel, null);
                        builder.create().show();
                    }
                }
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        }
        mPdfViewCtrl.invalidate();

        mJustSwitchedFromAnotherTool = false;
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onLayout(boolean, int, int, int, int)}.
     */
    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && isQuickMenuShown() && mAnnot == null){
            closeQuickMenu();
        }
    }

    /**
     * The overload implementation of {@link Tool#onLongPress(MotionEvent)}.
     */
    @Override
    public boolean onLongPress(MotionEvent e) {
        if (mAvoidLongPressAttempt) {
            mAvoidLongPressAttempt = false;
            return false;
        }


        int x = (int) (e.getX() + 0.5);
        int y = (int) (e.getY() + 0.5);
        selectAnnot(x, y);

        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            boolean is_form = mAnnot != null && mAnnot.getType() == Annot.e_Widget;

            RectF textSelectRect = getTextSelectRect(e.getX(), e.getY());
            boolean isTextSelect = !is_form && mPdfViewCtrl.selectByRect(textSelectRect.left, textSelectRect.top, textSelectRect.right, textSelectRect.bottom);

            boolean isMadeByPDFTron = isMadeByPDFTron(mAnnot);

            // get next tool mode by long press callback
            ToolMode toolMode = ToolConfig.getInstance()
                .getPanLongPressSwitchToolCallback()
                .onPanLongPressSwitchTool(mAnnot, isMadeByPDFTron, isTextSelect);

            mNextToolMode = toolMode;

            if (mAnnot != null) {
                mAnnotPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
            }

            // if remain in Pan mode, show menu
            if (toolMode == ToolMode.PAN) {
                mSelectPageNum = mPdfViewCtrl.getPageNumberFromScreenPt(x, y);
                if (mSelectPageNum > 0) {
                    mAnchor = new RectF(x - 5, y, x + 5, y + 1);
                    mTargetPoint = new PointF(e.getX(), e.getY());
                    showMenu(mAnchor);
                }
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }

        mJustSwitchedFromAnotherTool = false;
        return false;
    }

    /**
     * @return The target point
     */
    @SuppressWarnings("unused")
    protected PointF getTargetPoint() {
        return mTargetPoint;
    }

    /**
     * The overload implementation of {@link Tool#onScaleBegin(float, float)}.
     */
    @Override
    public boolean onScaleBegin(float x, float y) {
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onScaleEnd(float, float)}.
     */
    @Override
    public boolean onScaleEnd(float x, float y) {
        super.onScaleEnd(x, y);
        mJustSwitchedFromAnotherTool = false;
        return false;
    }

    /**
     * The overload implementation of {@link Tool#onDraw(Canvas, Matrix)}.
     */
    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        mPageNumPosAdjust = 0;
        super.onDraw(canvas, tfm);
    }

    /**
     * The overload implementation of {@link Tool#onQuickMenuClicked(QuickMenuItem)}.
     */
    @Override
    public boolean onQuickMenuClicked(QuickMenuItem menuItem) {
        if (super.onQuickMenuClicked(menuItem)) {
            return true;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        if (toolManager.isReadOnly()) {
            mNextToolMode = ToolMode.PAN;
            return true;
        }

        if (menuItem.getItemId() == R.id.qm_line) {
            mNextToolMode = ToolMode.LINE_CREATE;
            ToolManager.Tool tool = toolManager.createTool(ToolMode.LINE_CREATE, this);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_arrow) {
            mNextToolMode = ToolMode.ARROW_CREATE;
            ToolManager.Tool tool = toolManager.createTool(ToolMode.ARROW_CREATE, this);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_ruler) {
            mNextToolMode = ToolMode.RULER_CREATE;
            ToolManager.Tool tool = toolManager.createTool(ToolMode.RULER_CREATE, this);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_polyline) {
            if (toolManager.isOpenEditToolbarFromPan()) {
                toolManager.onOpenEditToolbar(ToolMode.POLYLINE_CREATE);
            }
        } else if (menuItem.getItemId() == R.id.qm_rectangle) {
            mNextToolMode = ToolMode.RECT_CREATE;
            ToolManager.Tool tool = toolManager.createTool(ToolMode.RECT_CREATE, this);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_oval) {
            mNextToolMode = ToolMode.OVAL_CREATE;
            ToolManager.Tool tool = toolManager.createTool(ToolMode.OVAL_CREATE, this);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_sound) {
            mNextToolMode = ToolMode.SOUND_CREATE;
            SoundCreate tool = (SoundCreate) toolManager.createTool(ToolMode.SOUND_CREATE, this);
            toolManager.setTool(tool);
            tool.setTargetPoint(mTargetPoint, true);
        } else if (menuItem.getItemId() == R.id.qm_file_attachment) {
            mNextToolMode = ToolMode.FILE_ATTACHMENT_CREATE;
            FileAttachmentCreate tool = (FileAttachmentCreate) toolManager.createTool(ToolMode.FILE_ATTACHMENT_CREATE, this);
            toolManager.setTool(tool);
            tool.setTargetPoint(mTargetPoint, true);
        } else if (menuItem.getItemId() == R.id.qm_polygon) {
            if (toolManager.isOpenEditToolbarFromPan()) {
                toolManager.onOpenEditToolbar(ToolMode.POLYGON_CREATE);
            }
        } else if (menuItem.getItemId() == R.id.qm_cloud) {
            if (toolManager.isOpenEditToolbarFromPan()) {
                toolManager.onOpenEditToolbar(ToolMode.CLOUD_CREATE);
            }
        } else if (menuItem.getItemId() == R.id.qm_free_hand) {
            mNextToolMode = ToolMode.INK_CREATE;
            if (toolManager.isOpenEditToolbarFromPan()) {
                toolManager.onInkEditSelected(null);
            }
        } else if (menuItem.getItemId() == R.id.qm_free_highlighter) {
            mNextToolMode = ToolMode.FREE_HIGHLIGHTER;
            ToolManager.Tool tool = toolManager.createTool(ToolMode.FREE_HIGHLIGHTER, this);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_free_text) {
            mNextToolMode = ToolMode.TEXT_CREATE;
            FreeTextCreate freeTextTool = (FreeTextCreate) toolManager.createTool(ToolMode.TEXT_CREATE, this);
            toolManager.setTool(freeTextTool);
            freeTextTool.initFreeText(mTargetPoint);
        } else if (menuItem.getItemId() == R.id.qm_callout) {
            mNextToolMode = ToolMode.CALLOUT_CREATE;
            CalloutCreate calloutCreate = (CalloutCreate) toolManager.createTool(ToolMode.CALLOUT_CREATE, this);
            toolManager.setTool(calloutCreate);
            calloutCreate.initFreeText(mTargetPoint);
            AnnotEditAdvancedShape annotEdit = (AnnotEditAdvancedShape) toolManager.createTool(ToolMode.ANNOT_EDIT_ADVANCED_SHAPE, calloutCreate);
            toolManager.setTool(annotEdit);
            annotEdit.enterText();
            annotEdit.mNextToolMode = ToolMode.ANNOT_EDIT_ADVANCED_SHAPE;
        } else if (menuItem.getItemId() == R.id.qm_sticky_note) {
            mNextToolMode = ToolMode.TEXT_ANNOT_CREATE;
            StickyNoteCreate stickyNoteTool = (StickyNoteCreate) toolManager.createTool(ToolMode.TEXT_ANNOT_CREATE, this);
            toolManager.setTool(stickyNoteTool);
            stickyNoteTool.setTargetPoint(mTargetPoint);
        } else if (menuItem.getItemId() == R.id.qm_floating_sig) {
            mNextToolMode = ToolMode.SIGNATURE;
            Signature signatureTool = (Signature) toolManager.createTool(ToolMode.SIGNATURE, this);
            toolManager.setTool(signatureTool);
            signatureTool.setTargetPoint(mTargetPoint);
        } else if (menuItem.getItemId() == R.id.qm_image_stamper) {
            mNextToolMode = ToolMode.STAMPER;
            Stamper stamperTool = (Stamper) toolManager.createTool(ToolMode.STAMPER, this);
            toolManager.setTool(stamperTool);
            stamperTool.setTargetPoint(mTargetPoint, true);
        } else if (menuItem.getItemId() == R.id.qm_rubber_stamper) {
            mNextToolMode = ToolMode.RUBBER_STAMPER;
            RubberStampCreate tool = (RubberStampCreate) toolManager.createTool(ToolMode.RUBBER_STAMPER, this);
            toolManager.setTool(tool);
            tool.setTargetPoint(mTargetPoint, true);
        } else if (menuItem.getItemId() == R.id.qm_paste) {
            pasteAnnot(mTargetPoint);
        } else if (menuItem.getItemId() == R.id.qm_link) {
            mNextToolMode = ToolMode.RECT_LINK;
            ToolManager.Tool tool = toolManager.createTool(ToolMode.RECT_LINK, this);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_ink_eraser) {
            mNextToolMode = ToolMode.INK_ERASER;
            if (toolManager.isOpenEditToolbarFromPan()) {
                toolManager.onOpenAnnotationToolbar(ToolMode.INK_ERASER);
            }
        } else if (menuItem.getItemId() == R.id.qm_form_text) {
            mNextToolMode = ToolMode.FORM_TEXT_FIELD_CREATE;
            ToolManager.Tool tool = toolManager.createTool(ToolMode.FORM_TEXT_FIELD_CREATE, this);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_form_check_box) {
            mNextToolMode = ToolMode.FORM_CHECKBOX_CREATE;
            ToolManager.Tool tool = toolManager.createTool(ToolMode.FORM_CHECKBOX_CREATE, this);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_form_signature) {
            mNextToolMode = ToolMode.FORM_SIGNATURE_CREATE;
            ToolManager.Tool tool = toolManager.createTool(ToolMode.FORM_SIGNATURE_CREATE, this);
            toolManager.setTool(tool);
        } else if (menuItem.getItemId() == R.id.qm_rect_group_select) {
            mNextToolMode = ToolMode.ANNOT_EDIT_RECT_GROUP;
            ToolManager.Tool tool = toolManager.createTool(ToolMode.ANNOT_EDIT_RECT_GROUP, this);
            toolManager.setTool(tool);
        } else {
            // if mNetToolMode is not set, return false and let the child class handles
            return false;
        }

        return true;
    }

    @Override
    public boolean showMenu(RectF anchor_rect) {
        if (onInterceptAnnotationHandling(mAnnot)) {
            return true;
        }

        if (mPdfViewCtrl == null) {
            return false;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        return toolManager != null && !toolManager.isQuickMenuDisabled() && super.showMenu(anchor_rect);
    }

    private void selectAnnot(int x, int y) {
        unsetAnnot();

        mAnnotPageNum = 0;
        // Since find text locks the document, cancel it to release the document.
        mPdfViewCtrl.cancelFindText();
        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            Annot a = mPdfViewCtrl.getAnnotationAt(x, y);

            if (a != null && a.isValid()) {
                setAnnot(a, mPdfViewCtrl.getPageNumberFromScreenPt(x, y));
                buildAnnotBBox();
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
    }

    /**
     * The overload implementation of {@link Tool#getModeAHLabel()}.
     */
    @Override
    protected int getModeAHLabel() {
        return AnalyticsHandlerAdapter.LABEL_QM_EMPTY;
    }

    /**
     * @return {@link AnalyticsHandlerAdapter#QUICK_MENU_TYPE_EMPTY_SPACE}
     */
    @Override
    protected int getQuickMenuAnalyticType() {
        return AnalyticsHandlerAdapter.QUICK_MENU_TYPE_EMPTY_SPACE;
    }
}
