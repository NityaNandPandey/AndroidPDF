//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Field;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Widget;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for managing the undo/redo chain and importing/exporting meta-data
 * from/to each state of the chain
 * <p>
 * <div class="info">
 * if you wish to add a new action to be handled in UndoRedoManager,
 * make sure you will make necessary changes to
 * {@link #isValidAction(Context, String)} and
 * {@link #isEditPageAction(Context, String)}
 * </div>
 */
@SuppressWarnings("WeakerAccess")
public class UndoRedoManager implements
    ToolManager.AnnotationModificationListener,
    ToolManager.PdfDocModificationListener {

    private final static String TAG = UndoRedoManager.class.getName();
    private static boolean sDebug;

    private final static String JSON_DELIMITER = " ";

    private final static String JSON_INITIAL_LABEL = "label";
    private final static String JSON_INITIAL_CONTENT = "android_initial";

    private final static String JSON_ACTION = "Action";
    private final static String JSON_ACTION_EVENT = "Action event";
    private final static String JSON_STATE_NOT_FOUND = "state not found";
    private final static String JSON_SAFETY = "safety";
    private final static String JSON_ANNOT_INFO = "Annot Info";
    private final static String JSON_ANNOT_PRE_PAGE_NUM = "Page Number Before Modification";
    private final static String JSON_ANNOT_PRE_RECT = "Rect Before Modification";
    private final static String JSON_ANNOT_PAGE_NUMS = "Page Numbers";
    private final static String JSON_ANNOT_RECTS = "Rects";
    private final static String JSON_ANNOT_XFDF = "xfdf";

    private final static String JSON_PAGE_LIST = "Pages";
    private final static String JSON_PAGE_FROM = "From";
    private final static String JSON_PAGE_TO = "To";

    private final static String JSON_COLLAB_ANNOT_INFO = "collab_annot_info";
    private final static String JSON_COLLAB_UNDO_ACTION = "collab_undo_action";
    private final static String JSON_COLLAB_UNDO_COMMAND = "collab_undo_command";
    private final static String JSON_COLLAB_REDO_ACTION = "collab_redo_action";
    private final static String JSON_COLLAB_REDO_COMMAND = "collab_redo_command";
    private final static String JSON_COLLAB_ANNOT_PARAMS = "collab_annot_params";
    private final static String JSON_COLLAB_ANNOT_PARAMS_ID = "collab_annot_params_id";
    private final static String JSON_COLLAB_ANNOT_PARAMS_TYPE = "collab_annot_params_type";
    private final static String JSON_COLLAB_ANNOT_PARAMS_PAGE_NUM = "collab_annot_params_page_num";

    private final static String ACTION_EVENT_REMOVE_ANNOTS_FROM_PAGE = "remove_annots_from_page";
    private final static String ACTION_EVENT_REMOVE_ALL_ANNOTATIONS = "remove_all_annotations";
    private final static String ACTION_EVENT_ACTION = "action";
    private final static String ACTION_EVENT_MODIFY_BOOKMARKS = "modify_bookmarks";
    private final static String ACTION_EVENT_CROP_PAGES = "crop_pages";
    private final static String ACTION_EVENT_ADD_PAGES = "add_pages";
    private final static String ACTION_EVENT_DELETE_PAGES = "delete_pages";
    private final static String ACTION_EVENT_ROTATE_PAGES = "rotate_pages";
    private final static String ACTION_EVENT_MOVE_PAGE = "move_page";

    private enum AnnotAction {
        ADD,
        MODIFY,
        REMOVE
    }

    private Context mContext;
    private ToolManager mToolManager;
    private PDFViewCtrl mPdfViewCtrl;
    private Rect mPreModifyAnnotRect;
    private int mPreModifyPageNum;
    private int mUndoCount = 0;
    private int mRedoCount = 0;
    private int mLocationId;
    private String mLastAction;
    private boolean mLastActionIsUndo;

    /**
     * Class constructor
     *
     * @param toolManager The ToolManager
     */
    public UndoRedoManager(@NonNull ToolManager toolManager) {
        // ensure toolManager and PDFViewCtrl are valid objects

        mToolManager = toolManager;
        mPdfViewCtrl = toolManager.getPDFViewCtrl();
        if (mPdfViewCtrl == null) {
            throw new NullPointerException("PDFViewCtrl can't be null");
        }

        mToolManager.addAnnotationModificationListener(this);
        mToolManager.addPdfDocModificationListener(this);
        mContext = toolManager.getPDFViewCtrl().getContext();
    }

    /**
     * @return The PDFViewCtrl
     */
    public PDFViewCtrl getPdfViewCtrl() {
        return mPdfViewCtrl;
    }

    /**
     * Undo the last modification.
     *
     * @return The information attached to the last modification
     */
    public String undo() {
        return undo(0, false);
    }

    /**
     * Undo the last undo.
     *
     * @param locationId The location ID of where this is called from.
     *                   It will be used in Analytics Handler.
     * @param sendEvent  Whether it sends analytics event
     * @return The information attached to the last modification
     */
    public String undo(int locationId, boolean sendEvent) {
        mLocationId = locationId;
        mLastActionIsUndo = true;
        if (!sendEvent) {
            mUndoCount++;
        } else {
            mUndoCount = 0;
        }
        return performUndoRedo(true, locationId, sendEvent);
    }

    /**
     * Redo the last undo.
     *
     * @return The information attached to the last undo
     */
    public String redo() {
        return redo(0, false);
    }

    /**
     * Redo the last undo.
     *
     * @param locationId The location ID of where this is called from.
     *                   It will be used in Analytics Handler.
     * @param sendEvent  Whether it sends analytics event
     * @return The information attached to the last undo
     */
    public String redo(int locationId, boolean sendEvent) {
        mLocationId = locationId;
        mLastActionIsUndo = false;
        if (!sendEvent) {
            mRedoCount++;
        } else {
            mRedoCount = 0;
        }
        return performUndoRedo(false, locationId, sendEvent);
    }

    private String performUndoRedo(boolean isUndo, int location, boolean sendEvent) {
        String info = "";

        if (!mPdfViewCtrl.isUndoRedoEnabled()) {
            return info;
        }

        Tool tool = (Tool) mToolManager.getTool();
        if (tool instanceof FreehandCreate) {
            ((FreehandCreate) tool).commitAnnotation();
        }

        try {
            mPdfViewCtrl.cancelRendering();
            removeUnsafeUndoRedoInfo(isUndo);
            if (isUndo) {
                info = mPdfViewCtrl.undo();
            } else {
                info = mPdfViewCtrl.redo();
            }
            if (sDebug)
                Log.d(TAG, (isUndo ? "undo: " : "redo: ") + info);
            updatePageLayout(info);

            JSONObject jsonObject = new JSONObject(info);
            if (jsonObject.has(JSON_ANNOT_XFDF)) {
                if (mToolManager.getAnnotManager() != null) {
                    mToolManager.getAnnotManager().onLocalChange(isUndo ?
                        AnnotManager.AnnotationAction.UNDO :
                        AnnotManager.AnnotationAction.REDO);
                }
            }
            String action = "";
            if (jsonObject.has(JSON_ACTION_EVENT)) {
                action = jsonObject.getString(JSON_ACTION_EVENT);
            }
            if (sendEvent) {
                AnalyticsHandlerAdapter.getInstance().sendEvent(
                    isUndo ? AnalyticsHandlerAdapter.EVENT_VIEWER_UNDO : AnalyticsHandlerAdapter.EVENT_VIEWER_REDO,
                    AnalyticsParam.viewerUndoRedoParam(action, location));
            } else {
                mLastAction = action;
            }

        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e, "info: " + info);
        }

        return info;
    }

    public void sendConsecutiveUndoRedoEvent() {
        if (mUndoCount != 0 || mRedoCount != 0) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(mLastActionIsUndo ? AnalyticsHandlerAdapter.EVENT_VIEWER_UNDO : AnalyticsHandlerAdapter.EVENT_VIEWER_REDO,
                AnalyticsParam.viewerUndoRedoParam(mLastAction, mLocationId, mUndoCount, mRedoCount));
        } else {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_UNDO_REDO_DISMISSED_NO_ACTION);
        }
        mUndoCount = 0;
        mRedoCount = 0;
        mLastAction = "";
        mLocationId = 0;
    }

    /**
     * This method should be called whenever new annotations are added to the document.
     * Preferably call this only from {@link ToolManager#raiseAnnotationsAddedEvent(Map)}
     *
     * @param annots The annotation that are added
     */
    @Override
    public void onAnnotationsAdded(final Map<Annot, Integer> annots) {
        if (annots == null || annots.size() == 0 || !mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        // as long as continues annotations are added in the same page, consider them as one event;
        // otherwise, start to trigger a new one
        try {
            JSONObject obj = prepareAnnotSnapshot(annots, AnnotAction.ADD);
            takeAnnotSnapshot(obj);

            if (mToolManager.getAnnotManager() != null) {
                mToolManager.getAnnotManager().onLocalChange(AnnotManager.AnnotationAction.ADD);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * This method should be called before modifying annotations in the document.
     * This will let undo/redo operation know the region of annotation before and after
     * modification (for example, it can be used to animate between changes)
     * Preferably call this only from {@link ToolManager#raiseAnnotationsPreModifyEvent(Map)}
     *
     * @param annots The annotations that are going to be modified
     */
    @Override
    public void onAnnotationsPreModify(final Map<Annot, Integer> annots) {
        // store a rectangle jointing all annotations area
        // to make it simple if all annotations are belonging to one page then store the information;
        // otherwise, discard it
        if (annots == null || annots.size() == 0 || !mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        mPreModifyPageNum = 0;
        mPreModifyAnnotRect = null;

        for (Map.Entry<Annot, Integer> pair : annots.entrySet()) {
            Annot annot = pair.getKey();
            Integer pageNum = pair.getValue();
            if (mPreModifyPageNum != 0 && mPreModifyPageNum != pageNum) {
                mPreModifyPageNum = 0;
                mPreModifyAnnotRect = null;
                return;
            }
            mPreModifyPageNum = pageNum;
            try {
                if (annot != null && annot.isValid()) {
                    com.pdftron.pdf.Rect annotRect;
                    if (annot.getType() == Annot.e_Widget) {
                        // for widgets we store the rectangle of all its fields
                        Widget widget = new Widget(annot);
                        Field field = widget.getField();
                        annotRect = field.getUpdateRect();
                    } else {
                        annotRect = mPdfViewCtrl.getPageRectForAnnot(annot, pageNum);
                    }
                    annotRect.normalize();
                    if (mPreModifyAnnotRect != null) {
                        mPreModifyAnnotRect.setX1(Math.min(mPreModifyAnnotRect.getX1(), annotRect.getX1()));
                        mPreModifyAnnotRect.setY1(Math.min(mPreModifyAnnotRect.getY1(), annotRect.getY1()));
                        mPreModifyAnnotRect.setX2(Math.max(mPreModifyAnnotRect.getX2(), annotRect.getX2()));
                        mPreModifyAnnotRect.setY2(Math.max(mPreModifyAnnotRect.getY2(), annotRect.getY2()));
                    } else {
                        mPreModifyAnnotRect = annotRect;
                    }
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * This method should be called whenever annotations in the document are modified.
     * Preferably call this only from {@link ToolManager#raiseAnnotationsModifiedEvent(Map, Bundle)}
     *
     * @param annots The annotations that are modified
     */
    @Override
    public void onAnnotationsModified(final Map<Annot, Integer> annots, Bundle extra) {
        if (annots == null || annots.size() == 0 || !mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        // as long as continues annotations are modified in the same page, consider them as one event;
        // otherwise, start to trigger a new one
        JSONObject result = prepareAnnotSnapshot(annots, AnnotAction.MODIFY, mPreModifyAnnotRect, mPreModifyPageNum);
        mPreModifyAnnotRect = null; // unset pre-modify annotation
        takeAnnotSnapshot(result);
        if (mToolManager.getAnnotManager() != null) {
            mToolManager.getAnnotManager().onLocalChange(AnnotManager.AnnotationAction.MODIFY);
        }
    }

    /**
     * This method should be called before removing annotations.
     * Preferably call this only from {@link ToolManager#raiseAnnotationsPreRemoveEvent(Map)}
     *
     * @param annots The annotations that are going to be removed
     */
    @Override
    public void onAnnotationsPreRemove(final Map<Annot, Integer> annots) {
        if (annots == null || annots.size() == 0 || !mPdfViewCtrl.isUndoRedoEnabled()) {
            //noinspection UnnecessaryReturnStatement
            return;
        }
    }

    /**
     * This method should be called whenever annotations are removed from the document.
     * Preferably call this only from {@link ToolManager#raiseAnnotationsRemovedEvent(Map)}
     *
     * @param annots The annotation that are removed
     */
    @Override
    public void onAnnotationsRemoved(final Map<Annot, Integer> annots) {
        if (annots == null || annots.size() == 0 || !mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        // as long as continues annotations are added in the same page, consider them as one event;
        // otherwise, start to trigger a new one
        try {
            JSONObject result = prepareAnnotSnapshot(annots, AnnotAction.REMOVE);

            takeAnnotSnapshot(result);
            if (mToolManager.getAnnotManager() != null) {
                mToolManager.getAnnotManager().onLocalChange(AnnotManager.AnnotationAction.DELETE);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * This method should be called whenever all annotations in the page are removed.
     * Preferably call this only from {@link ToolManager#raiseAnnotationsRemovedEvent(int)}
     */
    @Override
    public void onAnnotationsRemovedOnPage(int pageNum) {
        if (!mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        try {
            JSONObject jsonObj = new JSONObject();
            if (mContext != null) {
                String strRemoveAnnotations = mContext.getResources().getString(R.string.undo_redo_annots_remove_from_page, pageNum);
                jsonObj.put(JSON_ACTION, strRemoveAnnotations);
            }
            jsonObj.put(JSON_ACTION_EVENT, ACTION_EVENT_REMOVE_ANNOTS_FROM_PAGE);

            if (Utils.isNullOrEmpty(jsonObj.toString())) {
                AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("takeUndoSnapshot with an empty string"));
            }
            takeUndoSnapshot(jsonObj.toString());
            if (mToolManager.getAnnotManager() != null) {
                mToolManager.getAnnotManager().onLocalChange(AnnotManager.AnnotationAction.DELETE);
            }
            if (sDebug)
                Log.d(TAG, "snapshot: " + jsonObj.toString());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * This method should be called whenever all annotations in the document are removed.
     * Preferably call this only from {@link ToolManager#raiseAllAnnotationsRemovedEvent()}
     */
    @Override
    public void onAllAnnotationsRemoved() {
        if (!mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        try {
            JSONObject jsonObj = new JSONObject();
            if (mContext != null) {
                String strRemoveAnnotations = mContext.getResources().getString(R.string.undo_redo_annots_remove);
                jsonObj.put(JSON_ACTION, strRemoveAnnotations);
            }
            jsonObj.put(JSON_ACTION_EVENT, ACTION_EVENT_REMOVE_ALL_ANNOTATIONS);

            if (Utils.isNullOrEmpty(jsonObj.toString())) {
                AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("takeUndoSnapshot with an empty string"));
            }
            takeUndoSnapshot(jsonObj.toString());
            if (mToolManager.getAnnotManager() != null) {
                mToolManager.getAnnotManager().onLocalChange(AnnotManager.AnnotationAction.DELETE);
            }
            if (sDebug)
                Log.d(TAG, "snapshot: " + jsonObj.toString());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * This method should be called whenever an action applies to an annotation.
     * Preferably call this only from {@link ToolManager#raiseAnnotationActionEvent()}
     */
    @Override
    public void onAnnotationAction() {
        if (!mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        try {
            JSONObject jsonObj = new JSONObject();
            if (mContext != null) {
                String strAnnotAction = mContext.getResources().getString(R.string.undo_redo_annot_action);
                jsonObj.put(JSON_ACTION, strAnnotAction);
            }
            jsonObj.put(JSON_ACTION_EVENT, ACTION_EVENT_ACTION);

            if (Utils.isNullOrEmpty(jsonObj.toString())) {
                AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("takeUndoSnapshot with an empty string"));
            }
            takeUndoSnapshot(jsonObj.toString());
            if (sDebug)
                Log.d(TAG, "snapshot: " + jsonObj.toString());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * This method should be called whenever the bookmark is modified.
     * Preferably call this only from {@link ToolManager#raiseBookmarkModified()}
     */
    @Override
    public void onBookmarkModified() {
        if (!mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        try {
            JSONObject jsonObj = new JSONObject();
            if (mContext != null) {
                String strBookmarkModified = mContext.getResources().getString(R.string.undo_redo_bookmark_modify);
                jsonObj.put(JSON_ACTION, strBookmarkModified);
            }
            jsonObj.put(JSON_ACTION_EVENT, ACTION_EVENT_MODIFY_BOOKMARKS);

            if (Utils.isNullOrEmpty(jsonObj.toString())) {
                AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("takeUndoSnapshot with an empty string"));
            }
            takeUndoSnapshot(jsonObj.toString());
            if (sDebug)
                Log.d(TAG, "snapshot: " + jsonObj.toString());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * This method should be called whenever the pages are cropped.
     * Preferably call this only from {@link ToolManager#raisePagesCropped()}
     */
    @Override
    public void onPagesCropped() {
        if (!mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        try {
            JSONObject jsonObj = new JSONObject();
            if (mContext != null) {
                String strCropPages = mContext.getResources().getString(R.string.pref_viewmode_user_crop);
                jsonObj.put(JSON_ACTION, strCropPages);
            }
            jsonObj.put(JSON_ACTION_EVENT, ACTION_EVENT_CROP_PAGES);

            if (Utils.isNullOrEmpty(jsonObj.toString())) {
                AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("takeUndoSnapshot with an empty string"));
            }
            takeUndoSnapshot(jsonObj.toString());
            if (sDebug)
                Log.d(TAG, "snapshot: " + jsonObj.toString());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * This method should be called whenever new pages are added.
     * Preferably call this only from {@link ToolManager#raisePagesAdded(List)}
     *
     * @param pageList The list of added pages
     */
    @Override
    public void onPagesAdded(final List<Integer> pageList) {
        if (pageList == null || pageList.size() == 0 || !mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        try {
            JSONObject jsonObj = new JSONObject();
            if (mContext != null) {
                String strPageAdd = mContext.getResources().getString(R.string.undo_redo_page_add);
                jsonObj.put(JSON_ACTION, strPageAdd);
            }
            jsonObj.put(JSON_ACTION_EVENT, ACTION_EVENT_ADD_PAGES);
            jsonObj.put(JSON_PAGE_LIST, convertPageListToString(pageList));

            if (Utils.isNullOrEmpty(jsonObj.toString())) {
                AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("takeUndoSnapshot with an empty string"));
            }
            takeUndoSnapshot(jsonObj.toString());
            if (sDebug)
                Log.d(TAG, "snapshot: " + jsonObj.toString());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * This method should be called whenever pages are deleted.
     * Preferably call this only from {@link ToolManager#raisePagesDeleted(List)}
     *
     * @param pageList The list of deleted pages
     */
    @Override
    public void onPagesDeleted(final List<Integer> pageList) {
        if (pageList == null || pageList.size() == 0 || !mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        try {
            JSONObject jsonObj = new JSONObject();
            if (mContext != null) {
                String strPageDelete = mContext.getResources().getString(R.string.undo_redo_page_delete);
                jsonObj.put(JSON_ACTION, strPageDelete);
            }
            jsonObj.put(JSON_ACTION_EVENT, ACTION_EVENT_DELETE_PAGES);
            jsonObj.put(JSON_PAGE_LIST, convertPageListToString(pageList));

            if (Utils.isNullOrEmpty(jsonObj.toString())) {
                AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("takeUndoSnapshot with an empty string"));
            }
            takeUndoSnapshot(jsonObj.toString());
            if (sDebug)
                Log.d(TAG, "snapshot: " + jsonObj.toString());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * This method should be called whenever pages are rotated.
     * Preferably call this only from {@link ToolManager#raisePagesRotated(List)}
     *
     * @param pageList The list of rotated pages
     */
    @Override
    public void onPagesRotated(final List<Integer> pageList) {
        if (pageList == null || pageList.size() == 0 || !mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        try {
            JSONObject jsonObj = new JSONObject();
            if (mContext != null) {
                String strPageRotate = mContext.getResources().getString(R.string.undo_redo_page_rotate);
                jsonObj.put(JSON_ACTION, strPageRotate);
            }
            jsonObj.put(JSON_ACTION_EVENT, ACTION_EVENT_ROTATE_PAGES);
            jsonObj.put(JSON_PAGE_LIST, convertPageListToString(pageList));

            if (Utils.isNullOrEmpty(jsonObj.toString())) {
                AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("takeUndoSnapshot with an empty string"));
            }
            takeUndoSnapshot(jsonObj.toString());
            if (sDebug)
                Log.d(TAG, "snapshot: " + jsonObj.toString());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * This method should be called whenever a page is repositioned.
     *
     * @param from The position on which the page moved from
     * @param to   The position on which the page moved to
     */
    @Override
    public void onPageMoved(int from, int to) {
        if (!mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        try {
            JSONObject jsonObj = new JSONObject();
            if (mContext != null) {
                String strPageMove = mContext.getResources().getString(R.string.undo_redo_page_move);
                jsonObj.put(JSON_ACTION, strPageMove);
            }
            jsonObj.put(JSON_ACTION_EVENT, ACTION_EVENT_MOVE_PAGE);
            jsonObj.put(JSON_PAGE_FROM, from);
            jsonObj.put(JSON_PAGE_TO, to);

            if (Utils.isNullOrEmpty(jsonObj.toString())) {
                AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("takeUndoSnapshot with an empty string"));
            }
            takeUndoSnapshot(jsonObj.toString());
            if (sDebug)
                Log.d(TAG, "snapshot: " + jsonObj.toString());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * The overloaded implementation of {@link ToolManager.AnnotationModificationListener#annotationsCouldNotBeAdded(String)}.
     */
    @Override
    public void annotationsCouldNotBeAdded(String errorMessage) {

    }

    /**
     * Returns a list of page numbers that are involved in the given attached information.
     * <p>
     * <div class="warning">Index starts from 1. </div>
     *
     * @param info The information attached to an undo
     * @return A list of pages
     */
    @NonNull
    public static List<Integer> getPageList(final String info) {
        List<Integer> pageList = new ArrayList<>();

        if (!Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                if (jsonObj.has(JSON_PAGE_LIST)) {
                    String allPages = jsonObj.getString(JSON_PAGE_LIST);
                    if (!Utils.isNullOrEmpty(allPages)) {
                        String[] pages = allPages.split(JSON_DELIMITER);
                        for (String page : pages) {
                            pageList.add(Integer.valueOf(page));
                        }
                    }
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        return pageList;
    }

    /**
     * Returns the page number on which the page moved from.
     * <p>
     * <div class="warning">Index starts from 1. </div>
     *
     * @param info The information attached to an undo
     * @return The page number
     */
    public static int getPageFrom(final String info) {
        int pageNum = 0;

        if (!Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                pageNum = jsonObj.getInt(JSON_PAGE_FROM);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        return pageNum;
    }

    /**
     * Returns the page number on which the page moved to.
     * <p>
     * <div class="warning">Index starts from 1. </div>
     *
     * @param info The information attached to an undo
     * @return The page number
     */
    public static int getPageTo(final String info) {
        int pageNum = 0;

        if (!Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                pageNum = jsonObj.getInt(JSON_PAGE_TO);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        return pageNum;
    }

    /**
     * @return True if there is any action to undo in the stack of undo/redo
     */
    public boolean canUndo() {
        return !Utils.isNullOrEmpty(getNextUndoAction());
    }

    /**
     * @return True if there is any action to redo in the stack of undo/redo
     */
    public boolean canRedo() {
        return !Utils.isNullOrEmpty(getNextRedoAction());
    }

    /**
     * Returns the information attached to the next undo action
     *
     * @return The information attached to the next undo action
     */
    public String getNextUndoAction() {
        String result = "";
        if (!mPdfViewCtrl.isUndoRedoEnabled() || mPdfViewCtrl.getDoc() == null) {
            return result;
        }

        removeUnsafeUndoRedoInfo(true);
        String info = null;
        JSONObject jsonObj = null;
        try {
            info = mPdfViewCtrl.getNextUndoInfo();
            if (sDebug)
                Log.d(TAG, "next undo: " + info);
            jsonObj = new JSONObject(info);
            if (jsonObj.has(JSON_ACTION)) {
                String action = jsonObj.getString(JSON_ACTION);
                if (mContext != null && !Utils.isNullOrEmpty(action) && isValidAction(mContext, action)) {
                    String strUndo = mContext.getResources().getString(R.string.undo);
                    result = strUndo + ": " + action;
                }
            }
        } catch (Exception e) {
            if (info == null || !info.equals(JSON_STATE_NOT_FOUND)) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "next undo info: "
                    + (info == null ? "null" : info));
            }
        }

        if (Utils.isNullOrEmpty(result) && jsonObj != null) {
            try {
                if (jsonObj.has(JSON_INITIAL_LABEL)) {
                    String label = jsonObj.getString(JSON_INITIAL_LABEL);
                    if (!label.equals("initial")) {
                        result = mContext.getResources().getString(R.string.undo) + "...";
                    }
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        return result;
    }

    /**
     * Returns the information attached to the next redo action
     *
     * @return The information attached to the next redo action
     */
    public String getNextRedoAction() {
        String result = "";
        if (!mPdfViewCtrl.isUndoRedoEnabled() || mPdfViewCtrl.getDoc() == null) {
            return result;
        }

        removeUnsafeUndoRedoInfo(false);
        String info = null;
        try {
            info = mPdfViewCtrl.getNextRedoInfo();
            if (sDebug)
                Log.d(TAG, "next redo: " + info);
            JSONObject jsonObj = new JSONObject(info);
            if (jsonObj.has(JSON_ACTION)) {
                String action = jsonObj.getString(JSON_ACTION);
                if (mContext != null && isValidAction(mContext, action)) {
                    String strRedo = mContext.getResources().getString(R.string.redo);
                    result = strRedo + ": " + action;
                }
            }
        } catch (Exception e) {
            if (info == null || !info.equals(JSON_STATE_NOT_FOUND)) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "next redo info: "
                    + (info == null ? "null" : info));
            }
        }

        return result;
    }


    /**
     * Checks if the next undo action is an edit to page(s).
     *
     * @return True if the next undo action is an edit to page(s); False otherwise
     */
    public boolean isNextUndoEditPageAction() {
        if (!mPdfViewCtrl.isUndoRedoEnabled()) {
            return false;
        }

        String info = null;
        try {
            info = mPdfViewCtrl.getNextUndoInfo();
            if (sDebug)
                Log.d(TAG, "next undo: " + info);
            return isEditPageAction(mContext, info);
        } catch (Exception e) {
            if (info == null || !info.equals(JSON_STATE_NOT_FOUND)) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "next undo info: "
                    + (info == null ? "null" : info));
            }
        }

        return false;
    }

    /**
     * Checks if the next redo action is an edit to page(s).
     *
     * @return True if the next redo action is an edit to page(s); False otherwise
     */
    public boolean isNextRedoEditPageAction() {
        if (!mPdfViewCtrl.isUndoRedoEnabled()) {
            return false;
        }

        String info = null;
        try {
            info = mPdfViewCtrl.getNextRedoInfo();
            if (sDebug)
                Log.d(TAG, "next redo: " + info);
            return isEditPageAction(mContext, info);
        } catch (Exception e) {
            if (info == null || !info.equals(JSON_STATE_NOT_FOUND)) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "next redo info: "
                    + (info == null ? "null" : info));
            }
        }

        return false;
    }

    /**
     * Checks if the information attached to an undo is related to adding pages.
     *
     * @return True if the information attached to an undo is related to adding pages; False otherwise
     */
    public static boolean isAddPagesAction(final Context context, final String info) {
        if (context != null && !Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                if (jsonObj.has(JSON_ACTION_EVENT)) {
                    String action = jsonObj.getString(JSON_ACTION_EVENT);
                    if (ACTION_EVENT_ADD_PAGES.equals(action)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                if (!info.equals(JSON_STATE_NOT_FOUND)) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "info: " + info);
                }
            }
        }

        return false;
    }

    /**
     * Checks if the information attached to an undo is related to deleting pages.
     *
     * @return True if the information attached to an undo is related to deleting pages; False otherwise
     */
    public static boolean isDeletePagesAction(final Context context, final String info) {
        if (context != null && !Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                if (jsonObj.has(JSON_ACTION_EVENT)) {
                    String action = jsonObj.getString(JSON_ACTION_EVENT);
                    if (ACTION_EVENT_DELETE_PAGES.equals(action)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                if (!info.equals(JSON_STATE_NOT_FOUND)) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "info: " + info);
                }
            }
        }

        return false;
    }

    /**
     * Checks if the information attached to an undo is related to rotating pages.
     *
     * @return True if the information attached to an undo is related to rotating pages; False otherwise
     */
    public static boolean isRotatePagesAction(final Context context, final String info) {
        if (context != null && !Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                if (jsonObj.has(JSON_ACTION_EVENT)) {
                    String action = jsonObj.getString(JSON_ACTION_EVENT);
                    if (ACTION_EVENT_ROTATE_PAGES.equals(action)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                if (!info.equals(JSON_STATE_NOT_FOUND)) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "info: " + info);
                }
            }
        }

        return false;
    }

    /**
     * Checks if the information attached to an undo is related to moving a page.
     *
     * @return True if the information attached to an undo is related to moving a page; False otherwise
     */
    public static boolean isMovePageAction(final Context context, final String info) {
        if (context != null && !Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                if (jsonObj.has(JSON_ACTION_EVENT)) {
                    String action = jsonObj.getString(JSON_ACTION_EVENT);
                    if (ACTION_EVENT_MOVE_PAGE.equals(action)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                if (!info.equals(JSON_STATE_NOT_FOUND)) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "info: " + info);
                }
            }
        }

        return false;
    }

    /**
     * Checks if the information attached to an undo is related to editing (adding, deleting, rotating, moving) pages.
     *
     * @return True if the information attached to an undo is related to editing pages; False otherwise
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isEditPageAction(final Context context, final String info) {
        if (context != null && !Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                if (jsonObj.has(JSON_ACTION_EVENT)) {
                    String action = jsonObj.getString(JSON_ACTION_EVENT);
                    if (ACTION_EVENT_ADD_PAGES.equals(action) ||
                        ACTION_EVENT_DELETE_PAGES.equals(action) ||
                        ACTION_EVENT_ROTATE_PAGES.equals(action) ||
                        ACTION_EVENT_MOVE_PAGE.equals(action) ||
                        ACTION_EVENT_CROP_PAGES.equals(action) || // // cropping should be considered as page edition
                        ACTION_EVENT_ACTION.equals(action)) { // executing action can be considered as page edition
                        return true;
                    }
                }
            } catch (Exception e) {
                if (!info.equals(JSON_STATE_NOT_FOUND)) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "info: " + info);
                }
            }
        }

        return false;
    }

    /**
     * Checks if the information attached to an undo is related to adding annotations.
     *
     * @return True if the information attached to an undo is related to adding annotations; False otherwise
     */
    @SuppressWarnings("unused")
    public static boolean isAddAnnotationAction(final Context context, final String info) {
        if (context != null && !Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                if (jsonObj.has(JSON_ACTION_EVENT)) {
                    String action = jsonObj.getString(JSON_ACTION_EVENT);
                    String strAnnotAdd = context.getResources().getString(R.string.add);
                    if (!Utils.isNullOrEmpty(action) && action.startsWith(strAnnotAdd)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                if (!info.equals(JSON_STATE_NOT_FOUND)) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "info: " + info);
                }
            }
        }

        return false;
    }

    /**
     * Check if the information attached to an undo is related to modifying annotations
     *
     * @return True if the information attached to an undo is related to modifying annotations; False otherwise
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isModifyAnnotationAction(final Context context, final String info) {
        if (context != null && !Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                if (jsonObj.has(JSON_ACTION_EVENT)) {
                    String action = jsonObj.getString(JSON_ACTION_EVENT);
                    String strAnnotModify = context.getResources().getString(R.string.undo_redo_annot_modify);
                    if (!Utils.isNullOrEmpty(action) && action.startsWith(strAnnotModify)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                if (!info.equals(JSON_STATE_NOT_FOUND)) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "info: " + info);
                }
            }
        }

        return false;
    }

    /**
     * Check if the information attached to an undo is related to removing annotations
     *
     * @return True if the information attached to an undo is related to removing annotations; False otherwise
     */
    @SuppressWarnings("unused")
    public static boolean isRemoveAnnotationAction(final Context context, final String info) {
        if (context != null && !Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                if (jsonObj.has(JSON_ACTION_EVENT)) {
                    String action = jsonObj.getString(JSON_ACTION_EVENT);
                    String strAnnotRemove = context.getResources().getString(R.string.undo_redo_annot_remove);
                    if (!Utils.isNullOrEmpty(action) && action.startsWith(strAnnotRemove)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                if (!info.equals(JSON_STATE_NOT_FOUND)) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e, "info: " + info);
                }
            }
        }

        return false;
    }


    /**
     * This should be called before saving to remove any potential unsafe undo snapshot
     */
    public void takeUndoSnapshotForSafety() {
        if (!mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(false);
            shouldUnlock = true;
            if (mPdfViewCtrl.getDoc().hasChangesSinceSnapshot()) {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put(JSON_ACTION, JSON_SAFETY);
                if (Utils.isNullOrEmpty(jsonObj.toString())) {
                    AnalyticsHandlerAdapter.getInstance().sendException(
                        new Exception("takeUndoSnapshot with an empty string"));
                }
                mPdfViewCtrl.takeUndoSnapshot(jsonObj.toString());
                if (sDebug)
                    Log.d(TAG, "snapshot for safety");
            }
        } catch (PDFNetException | JSONException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    /**
     * Call this when {@link PDFViewCtrl} should be updated after undo/redo and jumped to the last
     * modification. The transition is shown using an animation.
     *
     * @param pdfViewCtrl The {@link PDFViewCtrl}
     * @param info        The attached information to undo
     * @param isUndo      Whether the action is undo (true) or redo (false)
     */
    public static void jumpToUndoRedo(PDFViewCtrl pdfViewCtrl, final String info, boolean isUndo) {
        if (pdfViewCtrl == null || Utils.isNullOrEmpty(info)) {
            return;
        }

        if (sDebug)
            Log.d(TAG, "jump to " + info);

        Context context = pdfViewCtrl.getContext();
        if (isEditPageAction(context, info)) {
            if (isDeletePagesAction(context, info)) {
                List<Integer> pageList = getPageList(info);
                int minPageNum = Collections.min(pageList);
                if (pageList.size() != 0) {
                    if (isUndo) {
                        pdfViewCtrl.setCurrentPage(minPageNum);
                    } else {
                        pdfViewCtrl.setCurrentPage(minPageNum == 1 ? 1 : minPageNum - 1);
                    }
                }
            } else if (isAddPagesAction(context, info)) {
                List<Integer> pageList = getPageList(info);
                if (pageList.size() != 0) {
                    int minPageNum = Collections.min(pageList);
                    if (isUndo) {
                        pdfViewCtrl.setCurrentPage(minPageNum == 1 ? 1 : minPageNum - 1);
                    } else {
                        pdfViewCtrl.setCurrentPage(minPageNum);
                    }
                }
            } else if (isRotatePagesAction(context, info)) {
                List<Integer> pageList = getPageList(info);
                if (pageList.size() != 0) {
                    int curPage = pdfViewCtrl.getCurrentPage();
                    if (!pageList.contains(curPage)) {
                        int minPageNum = Collections.min(pageList);
                        pdfViewCtrl.setCurrentPage(minPageNum);
                    }
                }
            } else if (isMovePageAction(context, info)) {
                int pageFrom = getPageFrom(info);
                int pageTo = getPageTo(info);
                if (isUndo) {
                    pdfViewCtrl.setCurrentPage(pageFrom);
                } else {
                    pdfViewCtrl.setCurrentPage(pageTo);
                }
            }
        } else {
            boolean isAnnotModify = isModifyAnnotationAction(context, info);
            if (isAnnotModify && isUndo) {
                int prePageNum = getPreModifiedAnnotPageNumber(info);
                Rect preAnnotRect = getPreModifiedAnnotRect(info);
                if (prePageNum != 0 && preAnnotRect != null) {
                    ViewerUtils.animateUndoRedo(pdfViewCtrl, preAnnotRect, prePageNum);
                }
            } else {
                List<Integer> pageNumbers = getAnnotPageNumbers(info);
                List<Rect> annotRects = getAnnotRects(info);
                if (pageNumbers != null && annotRects.size() == pageNumbers.size()) {
                    for (int i = 0, n = annotRects.size(); i < n; ++i) {
                        Rect annotRect = annotRects.get(i);
                        int pageNum = pageNumbers.get(i);
                        ViewerUtils.animateUndoRedo(pdfViewCtrl, annotRect, pageNum);
                    }
                }
            }
        }
    }

    private void removeUnsafeUndoRedoInfo(boolean isUndo) {
        if (!mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        String info = null;
        try {
            do {
                info = null;
                if (isUndo) {
                    info = mPdfViewCtrl.getNextUndoInfo();
                } else {
                    info = mPdfViewCtrl.getNextRedoInfo();
                }
                if (sDebug)
                    Log.e(TAG, "remove unsafe " + (isUndo ? "undo" : "redo") + " info: " + info);
                JSONObject jsonObject = new JSONObject(info);
                if (!jsonObject.has(JSON_ACTION)) {
                    break;
                }
                String action = jsonObject.getString(JSON_ACTION);
                if (Utils.isNullOrEmpty(action) || !action.equals(JSON_SAFETY)) {
                    break;
                }
                if (isUndo) {
                    mPdfViewCtrl.undo();
                } else {
                    mPdfViewCtrl.redo();
                }
            } while (true);
        } catch (Exception e) {
            if (info == null || !info.equals(JSON_STATE_NOT_FOUND)) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "next " +
                    (isUndo ? "undo" : "redo") + " info: " + (info == null ? "null" : info));
            }
        }
    }

    private JSONObject prepareAnnotSnapshot(final Map<Annot, Integer> annots, AnnotAction annotAction) {
        return prepareAnnotSnapshot(annots, annotAction, null, 0);
    }

    private JSONObject prepareAnnotSnapshot(final Map<Annot, Integer> annots, AnnotAction annotAction,
                                            Rect preModifyAnnotRect, int preModifyPageNum) {
        if (annots == null || annots.size() == 0 || !mPdfViewCtrl.isUndoRedoEnabled()) {
            return null;
        }

        try {
            JSONObject jsonObj = new JSONObject();
            String actionAnnotType = "";
            String actionEventAnnotType = "";
            String type = "";
            if (mContext != null) {
                for (Map.Entry<Annot, Integer> entry : annots.entrySet()) {
                    Annot annot = entry.getKey();
                    if (annot == null) {
                        AnalyticsHandlerAdapter.getInstance().sendException(
                            new Exception("An entry of annots is null"), "annots: " + annots);
                        return null;
                    }
                    if (type.isEmpty()) {
                        type = AnnotUtils.getAnnotTypeAsString(annot);
                        if (annots.size() > 1) {
                            actionAnnotType = AnnotUtils.getAnnotTypeAsPluralString(mContext, annot);
                            actionEventAnnotType = AnnotUtils.getAnnotTypeAsPluralString(annot);
                        } else {
                            actionAnnotType = AnnotUtils.getAnnotTypeAsString(mContext, annot);
                            actionEventAnnotType = AnnotUtils.getAnnotTypeAsString(annot);
                        }
                    } else if (!type.equals(AnnotUtils.getAnnotTypeAsString(annot))) {
                        actionAnnotType = mContext.getResources().getString(R.string.annot_misc_plural);
                        actionEventAnnotType = "annotations";
                        break;
                    }
                }

                String action = "";
                String actionEvent = "";
                switch (annotAction) {
                    case ADD:
                        action = mContext.getResources().getString(R.string.add);
                        actionEvent = "add";
                        break;
                    case MODIFY:
                        action = mContext.getResources().getString(R.string.undo_redo_annot_modify);
                        actionEvent = "modify";
                        break;
                    case REMOVE:
                        action = mContext.getResources().getString(R.string.undo_redo_annot_remove);
                        actionEvent = "remove";
                        break;
                }
                jsonObj.put(JSON_ACTION, action + JSON_DELIMITER + actionAnnotType);
                jsonObj.put(JSON_ACTION_EVENT, actionEvent + "_" + actionEventAnnotType);
            }

            String annotInfo = embedAnnotInfo(mPdfViewCtrl, annots, preModifyAnnotRect, preModifyPageNum);
            jsonObj.put(JSON_ANNOT_INFO, annotInfo);
            return jsonObj;
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    private String takeAnnotSnapshot(final JSONObject jsonObj) {
        if (jsonObj == null || !mPdfViewCtrl.isUndoRedoEnabled()) {
            return null;
        }

        String result = null;
        try {
            if (Utils.isNullOrEmpty(jsonObj.toString())) {
                AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("takeUndoSnapshot with an empty string"));
            }
            result = takeUndoSnapshot(jsonObj.toString());
            if (sDebug)
                Log.d(TAG, "snapshot: " + jsonObj.toString());
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        return result;
    }

    @SuppressWarnings("unused")
    private static String embedCollabAnnotInfo(final String undoAction, final String undoCommand,
                                               final String redoAction, final String redoCommand,
                                               final String params) {
        JSONObject result = new JSONObject();
        try {
            // undo
            result.put(JSON_COLLAB_UNDO_ACTION, undoAction);
            result.put(JSON_COLLAB_UNDO_COMMAND, undoCommand);
            // redo
            result.put(JSON_COLLAB_REDO_ACTION, redoAction);
            result.put(JSON_COLLAB_REDO_COMMAND, redoCommand);
            // params
            if (!Utils.isNullOrEmpty(params)) {
                result.put(JSON_COLLAB_ANNOT_PARAMS, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    private static String embedAnnotInfo(@NonNull final PDFViewCtrl pdfViewCtrl, final Map<Annot, Integer> annots,
                                         @Nullable final Rect preAnnotRect, int prePageNum) {
        if (annots == null || annots.size() == 0) {
            return "";
        }

        JSONObject result = new JSONObject();

        StringBuilder sbRects = new StringBuilder();
        StringBuilder sbPages = new StringBuilder();
        int origAnnotType = -1;
        for (Map.Entry<Annot, Integer> pair : annots.entrySet()) {
            Annot annot = pair.getKey();
            Integer pageNum = pair.getValue();
            try {
                int annotType = annot.getType();
                if (origAnnotType != -1 && origAnnotType != annotType) {
                    Log.e(TAG, "embedAnnotInfo: all annotations should be from the same type!");
                    continue;
                }
                if (annot.isValid()) {
                    origAnnotType = annotType;
                    com.pdftron.pdf.Rect annotRect;
                    if (annot.getType() == Annot.e_Widget) {
                        // for widgets we store the rectangle of all its fields
                        Widget widget = new Widget(annot);
                        Field field = widget.getField();
                        annotRect = field.getUpdateRect();
                    } else {
                        annotRect = pdfViewCtrl.getPageRectForAnnot(annot, pageNum);
                    }
                    annotRect.normalize();
                    int x1 = (int) (annotRect.getX1() + .5);
                    int x2 = (int) (annotRect.getX2() + .5);
                    int y1 = (int) (annotRect.getY1() + .5);
                    int y2 = (int) (annotRect.getY2() + .5);
                    sbRects.append(x1).append(JSON_DELIMITER)
                        .append(y1)
                        .append(JSON_DELIMITER)
                        .append(x2)
                        .append(JSON_DELIMITER)
                        .append(y2)
                        .append(JSON_DELIMITER);
                    sbPages.append(pageNum.toString())
                        .append(JSON_DELIMITER);
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

        String rectList = sbRects.toString();
        String pageList = sbPages.toString();
        try {
            result.put(JSON_ANNOT_PAGE_NUMS, pageList);
            result.put(JSON_ANNOT_RECTS, rectList);
            if (preAnnotRect != null && prePageNum != 0) {
                try {
                    int x1 = (int) (preAnnotRect.getX1() + .5);
                    int x2 = (int) (preAnnotRect.getX2() + .5);
                    int y1 = (int) (preAnnotRect.getY1() + .5);
                    int y2 = (int) (preAnnotRect.getY2() + .5);
                    result.put(JSON_ANNOT_PRE_RECT, x1 + JSON_DELIMITER + y1 + JSON_DELIMITER + x2 + JSON_DELIMITER + y2);
                    result.put(JSON_ANNOT_PRE_PAGE_NUM, Integer.toString(prePageNum));
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                }
            }
        } catch (JSONException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        return result.toString();
    }

    private static List<Integer> getAnnotPageNumbers(final String info) {
        List<Integer> pageNums = new ArrayList<>();

        if (!Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                String annotInfo = jsonObj.optString(JSON_ANNOT_INFO);
                JSONObject annotObj = new JSONObject(annotInfo);
                String str = annotObj.optString(JSON_ANNOT_PAGE_NUMS);
                if (!Utils.isNullOrEmpty(str)) {
                    String[] pages = str.split(JSON_DELIMITER);
                    for (String page : pages) {
                        pageNums.add(Integer.valueOf(page));
                    }
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "info: " + info);
            }
        }

        return pageNums;
    }

    private static int getPreModifiedAnnotPageNumber(final String info) {
        int pageNum = 0;

        if (!Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                String annotInfo = jsonObj.optString(JSON_ANNOT_INFO);
                JSONObject annotObj = new JSONObject(annotInfo);
                String str = annotObj.optString(JSON_ANNOT_PRE_PAGE_NUM);
                if (!Utils.isNullOrEmpty(str)) {
                    pageNum = Integer.valueOf(str);
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "info: " + info);
            }
        }

        return pageNum;
    }

    @NonNull
    private static List<Rect> getAnnotRects(final String info) {
        List<Rect> annotRects = new ArrayList<>();

        if (!Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                String annotInfo = jsonObj.optString(JSON_ANNOT_INFO);
                JSONObject annotObj = new JSONObject(annotInfo);
                String str = annotObj.optString(JSON_ANNOT_RECTS);
                if (!Utils.isNullOrEmpty(str)) {
                    String[] coords = str.split(JSON_DELIMITER);
                    int count = coords.length / 4;
                    for (int i = 0; i < count; ++i) {
                        int x1 = Integer.valueOf(coords[i * 4]);
                        int y1 = Integer.valueOf(coords[i * 4 + 1]);
                        int x2 = Integer.valueOf(coords[i * 4 + 2]);
                        int y2 = Integer.valueOf(coords[i * 4 + 3]);
                        annotRects.add(new Rect(x1, y1, x2, y2));
                    }
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "info: " + info);
            }
        }

        return annotRects;
    }

    private static Rect getPreModifiedAnnotRect(final String info) {
        Rect annotRect = null;

        if (!Utils.isNullOrEmpty(info)) {
            try {
                JSONObject jsonObj = new JSONObject(info);
                String annotInfo = jsonObj.optString(JSON_ANNOT_INFO);
                JSONObject annotObj = new JSONObject(annotInfo);
                String str = annotObj.optString(JSON_ANNOT_PRE_RECT);
                if (!Utils.isNullOrEmpty(str)) {
                    String[] coords = str.split(JSON_DELIMITER);
                    if (coords.length == 4) {
                        int x1 = Integer.valueOf(coords[0]);
                        int y1 = Integer.valueOf(coords[1]);
                        int x2 = Integer.valueOf(coords[2]);
                        int y2 = Integer.valueOf(coords[3]);
                        annotRect = new Rect(x1, y1, x2, y2);
                    }
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e, "info: " + info);
            }
        }

        return annotRect;
    }

    private static String convertPageListToString(final List<Integer> pageList) {
        StringBuilder pages = new StringBuilder();

        if (pageList == null || pageList.size() == 0) {
            return pages.toString();
        }

        boolean start = true;
        for (int position : pageList) {
            pages.append(start ? "" : JSON_DELIMITER).append(position);
            start = false;
        }
        return pages.toString();
    }

    private static boolean isValidAction(final Context context, final String action) {
        // check if the action is either move action or annotation action
        if (context != null && !Utils.isNullOrEmpty(action)) {
            String strBookmarkModify = context.getResources().getString(R.string.undo_redo_bookmark_modify);
            String strCropPages = context.getResources().getString(R.string.pref_viewmode_user_crop);
            String strPageAdd = context.getResources().getString(R.string.undo_redo_page_add);
            String strPageDelete = context.getResources().getString(R.string.undo_redo_page_delete);
            String strPageRotate = context.getResources().getString(R.string.undo_redo_page_rotate);
            String strPageMove = context.getResources().getString(R.string.undo_redo_page_move);
            String strAnnotAdd = context.getResources().getString(R.string.add);
            String strAnnotModify = context.getResources().getString(R.string.undo_redo_annot_modify);
            String strAnnotRemove = context.getResources().getString(R.string.undo_redo_annot_remove);
            String strAnnotsRemove = context.getResources().getString(R.string.undo_redo_annots_remove);
            String strAnnotAction = context.getResources().getString(R.string.undo_redo_annot_action);

            return action.equals(strBookmarkModify) || action.equals(strCropPages) ||
                action.equals(strPageAdd) || action.equals(strPageDelete) ||
                action.equals(strPageRotate) || action.equals(strPageMove) ||
                action.contains(strAnnotAdd) || action.contains(strAnnotModify) ||
                action.contains(strAnnotRemove) || action.equals(strAnnotsRemove) ||
                action.contains(strAnnotAction);
        }

        return false;
    }

    private void updatePageLayout(final String info) {
        if (!mPdfViewCtrl.isUndoRedoEnabled()) {
            return;
        }

        if (sDebug)
            Log.d(TAG, "update page layout after undo/redo");
        if (isEditPageAction(mContext, info)) {
            try {
                mPdfViewCtrl.updatePageLayout();
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    private String takeUndoSnapshot(String info) throws PDFNetException {
        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(false);
            shouldUnlock = true;
            String result = mPdfViewCtrl.takeUndoSnapshot(info);
            mPdfViewCtrl.requestRendering();
            return result;
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }
}
