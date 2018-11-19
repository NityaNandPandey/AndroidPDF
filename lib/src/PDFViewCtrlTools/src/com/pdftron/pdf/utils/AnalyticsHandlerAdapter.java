//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.BuildConfig;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import static com.pdftron.pdf.tools.ToolManager.ToolMode.TEXT_SELECT;

/**
 * @hide
 */
public class AnalyticsHandlerAdapter {

    private final static String TAG = AnalyticsHandlerAdapter.class.getName();

    // category/label identifier
    public final static int CATEGORY_VIEWER = 1;
    public final static int CATEGORY_FILEBROWSER = 2;
    public final static int CATEGORY_GENERAL = 3;
    public final static int CATEGORY_ANNOTATIONTOOLBAR = 4;
    public final static int CATEGORY_BOOKMARK = 5;
    public final static int CATEGORY_THUMBSLIDER = 6;
    public final static int CATEGORY_THUMBVIEW = 7;
    public final static int CATEGORY_QUICKTOOL = 8;
    public final static int CATEGORY_UNDOREDO = 9;
    public final static int CATEGORY_SHORTCUTS = 10;
    public final static int CATEGORY_CREATE_DOCUMENTS = 11;
    public final static int CATEGORY_COLOR_PICKER = 12;

    public final static int LABEL_STYLEEDITOR = 101;
    public final static int LABEL_QM_EMPTY = 102;
    public final static int LABEL_QM_TEXTSELECT = 103;
    public final static int LABEL_QM_ANNOTSELECT = 104;
    public final static int LABEL_QM_TEXTANNOTSELECT = 105;
    public final static int LABEL_QM_STAMPERSELECT = 106;
    public final static int LABEL_UNIVERSAL_VIEWING = 107;
    public final static int LABEL_VIEWER = 108;
    public final static int LABEL_FOLDERS = 109;
    public final static int LABEL_ALL_DOCUMENTS = 110;
    public final static int LABEL_SD_CARD = 111;
    public final static int LABEL_FILE_PICKER = 112;
    public final static int LABEL_EXTERNAL = 113;
    public final static int LABEL_RECENT = 114;
    public final static int LABEL_FAVORITES = 115;
    public final static int LABEL_MERGE = 116;
    public final static int LABEL_EDIT = 117;
    public final static int LABEL_STANDARD = 118;
    public final static int LABEL_WHEEL = 119;
    public final static int LABEL_PICKER_STANDARD = 120;
    public final static int LABEL_PICKER_RECENT = 121;
    public final static int LABEL_DIALOG_STANDARD = 122;
    public final static int LABEL_DIALOG_WHEEL = 123;
    public final static int LABEL_DIALOG_RECENT = 124;


    private static AnalyticsHandlerAdapter _INSTANCE;
    final protected static Object sLock = new Object();

    public static AnalyticsHandlerAdapter getInstance() {
        synchronized (sLock) {
            if (_INSTANCE == null) {
                _INSTANCE = new AnalyticsHandlerAdapter();
            }
        }
        return _INSTANCE;
    }

    public static void setInstance(AnalyticsHandlerAdapter value) {
        synchronized (sLock) {
            _INSTANCE = value;
        }
    }

    public void sendEvent(int categoryId, String action) {
    }

    public void sendEvent(String category, String action) {
    }

    public void sendEvent(int categoryId, String action, int labelId) {
    }

    public void sendEvent(int eventId) {
    }

    public void sendTimedEvent(int eventId) {
    }

    public void sendEvent(int eventId, @NonNull Map<String, String> params) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "event: " + getEvent(eventId) + " - " + params);
        }
    }

    public void sendTimedEvent(int eventId, @NonNull Map<String, String> params) {
    }

    public void endTimedEvent(int eventId) {
    }

    public void endTimedEvent(int eventId, @NonNull Map<String, String> params) {
    }

    public void sendException(Exception paramException) {
        if (null == paramException) {
            return;
        }
        if (paramException.getMessage() != null) {
            Log.e("PDFNet", paramException.getMessage());
        }
        paramException.printStackTrace();
    }

    public void sendException(Exception paramException, String moreInfo) {
        if (null == paramException) {
            return;
        }
        if (paramException.getMessage() != null) {
            Log.e("PDFNet", paramException.getMessage() + moreInfo);
        }
        paramException.printStackTrace();
    }

    public String getString(int id) {
        switch (id) {
            case CATEGORY_VIEWER:
                return "Viewer";
            case CATEGORY_FILEBROWSER:
                return "File Browser";
            case CATEGORY_GENERAL:
                return "General";
            case CATEGORY_ANNOTATIONTOOLBAR:
                return "Annotation Toolbar";
            case CATEGORY_BOOKMARK:
                return "Bookmark";
            case CATEGORY_QUICKTOOL:
                return "QuickMenu Tool";
            case CATEGORY_THUMBSLIDER:
                return "ThumbSlider";
            case CATEGORY_THUMBVIEW:
                return "ThumbnailsView";
            case CATEGORY_UNDOREDO:
                return "UndoRedo";
            case CATEGORY_SHORTCUTS:
                return "Shortcuts";
            case CATEGORY_CREATE_DOCUMENTS:
                return "Create Documents";
            case CATEGORY_COLOR_PICKER:
                return "Color Picker";
            case LABEL_STYLEEDITOR:
                return "StyleEditor";
            case LABEL_QM_EMPTY:
                return "EmptySpace";
            case LABEL_QM_TEXTSELECT:
                return "TextSelected";
            case LABEL_QM_ANNOTSELECT:
                return "AnnotationSelected";
            case LABEL_QM_TEXTANNOTSELECT:
                return "TextAnnotationSelected";
            case LABEL_QM_STAMPERSELECT:
                return "StampSelected";
            case LABEL_UNIVERSAL_VIEWING:
                return "Universal Viewing";
            case LABEL_VIEWER:
                return "Viewer";
            case LABEL_FOLDERS:
                return "Folders";
            case LABEL_ALL_DOCUMENTS:
                return "All Documents";
            case LABEL_SD_CARD:
                return "SD Card";
            case LABEL_FILE_PICKER:
                return "File Picker";
            case LABEL_EXTERNAL:
                return "External";
            case LABEL_RECENT:
                return "Recent";
            case LABEL_FAVORITES:
                return "Favorites";
            case LABEL_MERGE:
                return "Merge";
            case LABEL_EDIT:
                return "Edit";
            case LABEL_STANDARD:
                return "Standard";
            case LABEL_WHEEL:
                return "Wheel";
            case LABEL_PICKER_STANDARD:
                return "Picker standard color";
            case LABEL_PICKER_RECENT:
                return "Picker recent color";
            case LABEL_DIALOG_STANDARD:
                return "Dialog standard color";
            case LABEL_DIALOG_WHEEL:
                return "Dialog wheel color";
            case LABEL_DIALOG_RECENT:
                return "Dialog recent color";
            default:
                return "";
        }
    }

    // create new item identifier
    public final static int CREATE_NEW_ITEM_FOLDER = 1;
    public final static int CREATE_NEW_ITEM_BLANK_PDF = 2;
    public final static int CREATE_NEW_ITEM_PDF_FROM_DOCS = 3;
    public final static int CREATE_NEW_ITEM_PDF_FROM_IMAGE = 4;
    public final static int CREATE_NEW_ITEM_PDF_FROM_CAMERA = 5;
    public final static int CREATE_NEW_ITEM_PDF_FROM_WEBPAGE = 6;
    public final static int CREATE_NEW_ITEM_IMAGE_FROM_IMAGE = 7;
    public final static int CREATE_NEW_ITEM_IMAGE_FROM_CAMERA = 8;

    @NonNull
    public String getCreateNewItem(int itemId) {
        switch (itemId) {
            case CREATE_NEW_ITEM_FOLDER:
                return "ic_folder_black_24dp";
            case CREATE_NEW_ITEM_BLANK_PDF:
                return "blank_pdf";
            case CREATE_NEW_ITEM_PDF_FROM_DOCS:
                return "pdf_from_docs";
            case CREATE_NEW_ITEM_PDF_FROM_IMAGE:
                return "pdf_from_image";
            case CREATE_NEW_ITEM_PDF_FROM_CAMERA:
                return "pdf_from_camera";
            case CREATE_NEW_ITEM_PDF_FROM_WEBPAGE:
                return "pdf_from_webpage";
            case CREATE_NEW_ITEM_IMAGE_FROM_IMAGE:
                return "image_stamp_from_image";
            case CREATE_NEW_ITEM_IMAGE_FROM_CAMERA:
                return "image_stamp_from_camera";
            default:
                return "not_known";
        }
    }

    // open file origin identifier
    public final static int OPEN_FILE_IN_APP = 1;
    public final static int OPEN_FILE_3RD_PARTY = 2;

    public String getOpenFileOrigin(int originId) {
        switch (originId) {
            case OPEN_FILE_IN_APP:
                return "in_app";
            case OPEN_FILE_3RD_PARTY:
                return "3rd_party";
            default:
                return "not_known";
        }
    }

    // location identifier
    public final static int LOCATION_VIEWER = 1;
    public final static int LOCATION_ANNOTATION_TOOLBAR = 2;
    public final static int LOCATION_THUMBNAILS_VIEW = 3;
    public final static int LOCATION_THUMBNAILS_SLIDER = 4;

    public String getLocation(int locationId) {
        switch (locationId) {
            case LOCATION_VIEWER:
                return "viewer";
            case LOCATION_ANNOTATION_TOOLBAR:
                return "annotation_toolbar";
            case LOCATION_THUMBNAILS_VIEW:
                return "thumbnails_view";
            case LOCATION_THUMBNAILS_SLIDER:
                return "thumbnails_slider";
            default:
                sendException(new Exception("the location is not known"));
                return "not_known";
        }
    }

    // screen identifier
    public final static int SCREEN_VIEWER = 1;
    public final static int SCREEN_RECENT = 2;
    public final static int SCREEN_FAVORITES = 3;
    public final static int SCREEN_FOLDERS = 4;
    public final static int SCREEN_ALL_DOCUMENTS = 5;
    public final static int SCREEN_SD_CARD = 6;
    public final static int SCREEN_SETTINGS = 7;
    public final static int SCREEN_URL = 8;

    public String getScreen(int screenId) {
        switch (screenId) {
            case SCREEN_VIEWER:
                return "Viewer";
            case SCREEN_RECENT:
                return "Recent";
            case SCREEN_FAVORITES:
                return "Favorites";
            case SCREEN_FOLDERS:
                return "Folders";
            case SCREEN_ALL_DOCUMENTS:
                return "AllDocuments";
            case SCREEN_SD_CARD:
                return "SDCard";
            case SCREEN_SETTINGS:
                return "Settings";
            case SCREEN_URL:
                return "URL";
            default:
                return "not_known";
        }
    }

    // annotation tool identifier
    public final static int ANNOTATION_TOOL_LINE = 1;
    public final static int ANNOTATION_TOOL_RECTANGLE = 2;
    public final static int ANNOTATION_TOOL_OVAL = 3;
    public final static int ANNOTATION_TOOL_ERASER = 4;
    public final static int ANNOTATION_TOOL_FREEHAND = 5;
    public final static int ANNOTATION_TOOL_ARROW = 6;
    public final static int ANNOTATION_TOOL_STICKY_NOTE = 7;
    public final static int ANNOTATION_TOOL_FREE_TEXT = 8;
    public final static int ANNOTATION_TOOL_SIGNATURE = 9;
    public final static int ANNOTATION_TOOL_STAMP = 10;
    public final static int ANNOTATION_TOOL_RUBBER_STAMP = 11;
    public final static int ANNOTATION_TOOL_UNDERLINE = 12;
    public final static int ANNOTATION_TOOL_HIGHLIGHT = 13;
    public final static int ANNOTATION_TOOL_SQUIGGLY = 14;
    public final static int ANNOTATION_TOOL_STRIKEOUT = 15;
    public final static int ANNOTATION_TOOL_PAN = 16;
    public final static int ANNOTATION_TOOL_LINK = 17;
    public final static int ANNOTATION_TOOL_FREE_HIGHLIGHTER = 18;
    public final static int ANNOTATION_TOOL_SHOW_ALL_TOOLS = 19;
    public final static int ANNOTATION_TOOL_SHOW_FEW_TOOLS = 20;
    public final static int ANNOTATION_TOOL_POLYLINE = 21;
    public final static int ANNOTATION_TOOL_POLYGON = 22;
    public final static int ANNOTATION_TOOL_CLOUD = 23;
    public final static int ANNOTATION_TOOL_MULTI_SELECT = 24;
    public final static int ANNOTATION_TOOL_RULER = 25;
    public final static int ANNOTATION_TOOL_CALLOUT = 26;

    public String getAnnotationTool(int screenId) {
        switch (screenId) {
            case ANNOTATION_TOOL_LINE:
                return "line";
            case ANNOTATION_TOOL_RECTANGLE:
                return "rectangle";
            case ANNOTATION_TOOL_OVAL:
                return "oval";
            case ANNOTATION_TOOL_ERASER:
                return "eraser";
            case ANNOTATION_TOOL_FREEHAND:
                return "freehand";
            case ANNOTATION_TOOL_ARROW:
                return "arrow";
            case ANNOTATION_TOOL_STICKY_NOTE:
                return "sticky_note";
            case ANNOTATION_TOOL_FREE_TEXT:
                return "free_text";
            case ANNOTATION_TOOL_CALLOUT:
                return "callout";
            case ANNOTATION_TOOL_SIGNATURE:
                return "signature";
            case ANNOTATION_TOOL_STAMP:
                return "image_stamp";
            case ANNOTATION_TOOL_RUBBER_STAMP:
                return "rubber_stamp";
            case ANNOTATION_TOOL_UNDERLINE:
                return "underline";
            case ANNOTATION_TOOL_HIGHLIGHT:
                return "highlight";
            case ANNOTATION_TOOL_SQUIGGLY:
                return "squiggly";
            case ANNOTATION_TOOL_STRIKEOUT:
                return "strikeout";
            case ANNOTATION_TOOL_PAN:
                return "pan";
            case ANNOTATION_TOOL_LINK:
                return "link";
            case ANNOTATION_TOOL_FREE_HIGHLIGHTER:
                return "free_highlighter";
            case ANNOTATION_TOOL_SHOW_ALL_TOOLS:
                return "show_all_tools";
            case ANNOTATION_TOOL_SHOW_FEW_TOOLS:
                return "show_fewer_tools";
            case ANNOTATION_TOOL_POLYLINE:
                return "polyline";
            case ANNOTATION_TOOL_POLYGON:
                return "polygon";
            case ANNOTATION_TOOL_CLOUD:
                return "cloud";
            case ANNOTATION_TOOL_RULER:
                return "ruler";
            case ANNOTATION_TOOL_MULTI_SELECT:
                return "multi_select";
            default:
                return "not_known";
        }
    }

    public String getAnnotToolByAnnotType(int annotType) {
        int annotTool;
        switch (annotType) {
            case Annot.e_Highlight:
                annotTool = ANNOTATION_TOOL_HIGHLIGHT;
                break;
            case Annot.e_Underline:
                annotTool = ANNOTATION_TOOL_UNDERLINE;
                break;
            case Annot.e_Squiggly:
                annotTool = ANNOTATION_TOOL_SQUIGGLY;
                break;
            case Annot.e_StrikeOut:
                annotTool = ANNOTATION_TOOL_STRIKEOUT;
                break;
            case Annot.e_FreeText:
                annotTool = ANNOTATION_TOOL_FREE_TEXT;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_CALLOUT:
                annotTool = ANNOTATION_TOOL_CALLOUT;
                break;
            case Annot.e_Text:
                annotTool = ANNOTATION_TOOL_STICKY_NOTE;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE:
                annotTool = ANNOTATION_TOOL_SIGNATURE;
                break;
            case Annot.e_Link:
                annotTool = ANNOTATION_TOOL_LINK;
                break;
            case Annot.e_Ink:
                annotTool = ANNOTATION_TOOL_FREEHAND;
                break;
            case Annot.e_Square:
                annotTool = ANNOTATION_TOOL_RECTANGLE;
                break;
            case Annot.e_Circle:
                annotTool = ANNOTATION_TOOL_OVAL;
                break;
            case Annot.e_Line:
                annotTool = ANNOTATION_TOOL_LINE;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_ARROW:
                annotTool = ANNOTATION_TOOL_ARROW;
                break;
            case AnnotStyle.CUSTOM_ANNOT_TYPE_RULER:
                annotTool = ANNOTATION_TOOL_RULER;
                break;
            default:
                annotTool = -1;
                break;
        }
        return getAnnotationTool(annotTool);
    }

    public final static int EDIT_TOOLBAR_TOOL_PEN_1 = 1;
    public final static int EDIT_TOOLBAR_TOOL_PEN_2 = 2;
    public final static int EDIT_TOOLBAR_TOOL_PEN_3 = 3;
    public final static int EDIT_TOOLBAR_TOOL_PEN_4 = 4;
    public final static int EDIT_TOOLBAR_TOOL_PEN_5 = 5;
    public final static int EDIT_TOOLBAR_TOOL_ERASER = 6;
    public final static int EDIT_TOOLBAR_TOOL_CLEAR = 7;
    public final static int EDIT_TOOLBAR_TOOL_CLOSE = 8;
    public final static int EDIT_TOOLBAR_TOOL_UNDO = 9;
    public final static int EDIT_TOOLBAR_TOOL_REDO = 10;

    public String getEditToolbarTool(int screenId) {
        switch (screenId) {
            case EDIT_TOOLBAR_TOOL_PEN_1:
                return "pen1";
            case EDIT_TOOLBAR_TOOL_PEN_2:
                return "pen2";
            case EDIT_TOOLBAR_TOOL_PEN_3:
                return "pen3";
            case EDIT_TOOLBAR_TOOL_PEN_4:
                return "pen4";
            case EDIT_TOOLBAR_TOOL_PEN_5:
                return "pen5";
            case EDIT_TOOLBAR_TOOL_ERASER:
                return "eraser";
            case EDIT_TOOLBAR_TOOL_CLEAR:
                return "clear";
            case EDIT_TOOLBAR_TOOL_CLOSE:
                return "close";
            case EDIT_TOOLBAR_TOOL_UNDO:
                return "undo";
            case EDIT_TOOLBAR_TOOL_REDO:
                return "redo";
            default:
                return "not_known";
        }
    }

    // view mode identifier
    public final static int VIEW_MODE_SINGLE = 1;
    public final static int VIEW_MODE_DOUBLE = 2;
    public final static int VIEW_MODE_COVER = 3;
    public final static int VIEW_MODE_REFLOW = 4;
    public final static int VIEW_MODE_VERTICAL_ON = 5;
    public final static int VIEW_MODE_VERTICAL_OFF = 6;
    public final static int VIEW_MODE_DAY_MODE = 7;
    public final static int VIEW_MODE_NIGHT_MODE = 8;
    public final static int VIEW_MODE_SEPIA_MODE = 9;
    public final static int VIEW_MODE_CUSTOM_MODE = 10;
    public final static int VIEW_MODE_RTL_ON = 11;
    public final static int VIEW_MODE_RTL_OFF = 12;
    public final static int VIEW_MODE_ROTATE = 13;
    public final static int VIEW_MODE_CROP = 14;
    public final static int VIEW_MODE_REFLOW_ZOOM_IN = 15;
    public final static int VIEW_MODE_REFLOW_ZOOM_OUT = 16;

    public String getViewMode(int itemId) {
        switch (itemId) {
            case VIEW_MODE_SINGLE:
                return "single";
            case VIEW_MODE_DOUBLE:
                return "double";
            case VIEW_MODE_COVER:
                return "cover";
            case VIEW_MODE_REFLOW:
                return "reflow";
            case VIEW_MODE_VERTICAL_ON:
                return "vertical_on";
            case VIEW_MODE_VERTICAL_OFF:
                return "vertical_off";
            case VIEW_MODE_DAY_MODE:
                return "day_mode";
            case VIEW_MODE_NIGHT_MODE:
                return "night_mode";
            case VIEW_MODE_SEPIA_MODE:
                return "sepia_mode";
            case VIEW_MODE_CUSTOM_MODE:
                return "custom_mode";
            case VIEW_MODE_RTL_ON:
                return "rtl_on";
            case VIEW_MODE_RTL_OFF:
                return "rtl_off";
            case VIEW_MODE_ROTATE:
                return "rotate";
            case VIEW_MODE_CROP:
                return "crop";
            case VIEW_MODE_REFLOW_ZOOM_IN:
                return "reflow_zoom_in";
            case VIEW_MODE_REFLOW_ZOOM_OUT:
                return "reflow_zoom_out";
            default:
                return "not_known";
        }
    }

    // thumbnail view action identifier
    public final static int THUMBNAILS_VIEW_DUPLICATE = 1;
    public final static int THUMBNAILS_VIEW_ROTATE = 2;
    public final static int THUMBNAILS_VIEW_DELETE = 3;
    public final static int THUMBNAILS_VIEW_EXPORT = 4;
    public final static int THUMBNAILS_VIEW_ADD_BLANK_PAGES = 5;
    public final static int THUMBNAILS_VIEW_ADD_PAGES_FROM_DOCS = 6;
    public final static int THUMBNAILS_VIEW_ADD_PAGE_FROM_IMAGE = 7;
    public final static int THUMBNAILS_VIEW_ADD_PAGE_FROM_CAMERA = 8;
    public final static int THUMBNAILS_VIEW_MOVE = 9;

    public String getThumbnailsView(int itemId) {
        switch (itemId) {
            case THUMBNAILS_VIEW_DUPLICATE:
                return "duplicate_pages";
            case THUMBNAILS_VIEW_ROTATE:
                return "rotate_pages";
            case THUMBNAILS_VIEW_DELETE:
                return "delete_pages";
            case THUMBNAILS_VIEW_EXPORT:
                return "export_pages";
            case THUMBNAILS_VIEW_ADD_BLANK_PAGES:
                return "add_blank_pages";
            case THUMBNAILS_VIEW_ADD_PAGES_FROM_DOCS:
                return "add_pages_from_doc";
            case THUMBNAILS_VIEW_ADD_PAGE_FROM_IMAGE:
                return "add_page_from_image";
            case THUMBNAILS_VIEW_ADD_PAGE_FROM_CAMERA:
                return "add_page_from_camera";
            case THUMBNAILS_VIEW_MOVE:
                return "move_pages";
            default:
                return "not_known";
        }
    }

    // navigation tab identifier
    public final static int NAVIGATION_TAB_OUTLINE = 1;
    public final static int NAVIGATION_TAB_USER_BOOKMARKS = 2;
    public final static int NAVIGATION_TAB_ANNOTATIONS = 3;

    public String getNavigationListsTab(int itemId) {
        switch (itemId) {
            case NAVIGATION_TAB_OUTLINE:
                return "outline";
            case NAVIGATION_TAB_USER_BOOKMARKS:
                return "user_bookmarks";
            case NAVIGATION_TAB_ANNOTATIONS:
                return "annotations";
            default:
                return "not_known";
        }
    }

    // user bookmarks action identifier
    public final static int USER_BOOKMARKS_ADD = 1;
    public final static int USER_BOOKMARKS_DELETE = 2;
    public final static int USER_BOOKMARKS_DELETE_ALL = 3;
    public final static int USER_BOOKMARKS_RENAME = 4;

    public String getUerBookmarksAction(int itemId) {
        switch (itemId) {
            case USER_BOOKMARKS_ADD:
                return "add";
            case USER_BOOKMARKS_DELETE:
                return "delete";
            case USER_BOOKMARKS_DELETE_ALL:
                return "delete_all";
            case USER_BOOKMARKS_RENAME:
                return "rename";
            default:
                return "not_known";
        }
    }

    // annotations list action identifier
    public final static int ANNOTATIONS_LIST_EXPORT = 1;
    public final static int ANNOTATIONS_LIST_DELETE = 2;
    public final static int ANNOTATIONS_LIST_DELETE_ALL_ON_PAGE = 3;
    public final static int ANNOTATIONS_LIST_DELETE_ALL_IN_DOC = 4;

    public String getAnnotationsListAction(int itemId) {
        switch (itemId) {
            case ANNOTATIONS_LIST_EXPORT:
                return "export";
            case ANNOTATIONS_LIST_DELETE:
                return "delete";
            case ANNOTATIONS_LIST_DELETE_ALL_ON_PAGE:
                return "delete_all_on_page";
            case ANNOTATIONS_LIST_DELETE_ALL_IN_DOC:
                return "delete_all_in_document";
            default:
                return "not_known";
        }
    }

    // viewer navigation by identifier
    public final static int VIEWER_NAVIGATE_BY_OUTLINE = 1;
    public final static int VIEWER_NAVIGATE_BY_USER_BOOKMARK = 2;
    public final static int VIEWER_NAVIGATE_BY_ANNOTATIONS_LIST = 3;
    public final static int VIEWER_NAVIGATE_BY_THUMBNAILS_VIEW = 4;

    public String getViewerNavigateBy(int itemId) {
        switch (itemId) {
            case VIEWER_NAVIGATE_BY_OUTLINE:
                return "outline";
            case VIEWER_NAVIGATE_BY_USER_BOOKMARK:
                return "user_bookmarks";
            case VIEWER_NAVIGATE_BY_ANNOTATIONS_LIST:
                return "annotations_list";
            case VIEWER_NAVIGATE_BY_THUMBNAILS_VIEW:
                return "thumbnails_view ";
            default:
                return "not_known";
        }
    }

    // shortcut identifier
    public final static int SHORTCUT_ANNOTATION_HIGHLIGHT = 1;
    public final static int SHORTCUT_ANNOTATION_UNDERLINE = 2;
    public final static int SHORTCUT_ANNOTATION_STRIKETHROUGH = 3;
    public final static int SHORTCUT_ANNOTATION_SQUIGGLY = 4;
    public final static int SHORTCUT_ANNOTATION_TEXTBOX = 5;
    public final static int SHORTCUT_ANNOTATION_COMMENT = 6;
    public final static int SHORTCUT_ANNOTATION_RECTANGLE = 7;
    public final static int SHORTCUT_ANNOTATION_OVAL = 8;
    public final static int SHORTCUT_ANNOTATION_DRAW = 9;
    public final static int SHORTCUT_ANNOTATION_ERASER = 10;
    public final static int SHORTCUT_ANNOTATION_LINE = 11;
    public final static int SHORTCUT_ANNOTATION_ARROW = 12;
    public final static int SHORTCUT_ANNOTATION_SIGNATURE = 13;
    public final static int SHORTCUT_ANNOTATION_IMAGE = 14;
    public final static int SHORTCUT_ANNOTATION_HYPERLINK = 15;
    public final static int SHORTCUT_DELETE_ANNOTATION = 16;
    public final static int SHORTCUT_GO_TO_NEXT_DOC = 17;
    public final static int SHORTCUT_GO_TO_PREV_DOC = 18;
    public final static int SHORTCUT_FIND = 19;
    public final static int SHORTCUT_GO_TO_NEXT_SEARCH = 20;
    public final static int SHORTCUT_GO_TO_PREV_SEARCH = 21;
    public final static int SHORTCUT_UNDO = 22;
    public final static int SHORTCUT_REDO = 23;
    public final static int SHORTCUT_COPY = 24;
    public final static int SHORTCUT_CUT = 25;
    public final static int SHORTCUT_PASTE = 26;
    public final static int SHORTCUT_PRINT = 27;
    public final static int SHORTCUT_ADD_BOOKMARK = 28;
    public final static int SHORTCUT_PAGE_UP = 29;
    public final static int SHORTCUT_PAGE_DOWN = 30;
    public final static int SHORTCUT_GO_TO_FIRST_PAGE = 31;
    public final static int SHORTCUT_GO_TO_LAST_PAGE = 32;
    public final static int SHORTCUT_JUMP_PAGE_BACK = 33;
    public final static int SHORTCUT_JUMP_PAGE_FORWARD = 34;
    public final static int SHORTCUT_START_EDIT_SELECTED_ANNOT = 35;
    public final static int SHORTCUT_SWITCH_FORM = 36;
    public final static int SHORTCUT_ROTATE_CLOCKWISE = 37;
    public final static int SHORTCUT_ROTATE_COUNTER_CLOCKWISE = 38;
    public final static int SHORTCUT_ZOOM_IN = 39;
    public final static int SHORTCUT_ZOOM_OUT = 40;
    public final static int SHORTCUT_RESET_ZOOM = 41;
    public final static int SHORTCUT_OPEN_DRAWER = 42;
    public final static int SHORTCUT_COMMIT_CLOSE_TEXT = 43;
    public final static int SHORTCUT_COMMIT_CLOSE_DRAW = 44;
    public final static int SHORTCUT_SWITCH_INK = 45;
    public final static int SHORTCUT_ERASE_INK = 46;
    public final static int SHORTCUT_CANCEL_TOOL = 47;
    public final static int SHORTCUT_CLOSE_MENU = 48;
    public final static int SHORTCUT_CLOSE_TAB = 49;
    public final static int SHORTCUT_CLOSE_APP = 50;

    public String getShortcut(int itemId) {
        switch (itemId) {
            case SHORTCUT_ANNOTATION_HIGHLIGHT:
                return "annotation_highlight";
            case SHORTCUT_ANNOTATION_UNDERLINE:
                return "annotation_underline";
            case SHORTCUT_ANNOTATION_STRIKETHROUGH:
                return "annotation_strikethrough";
            case SHORTCUT_ANNOTATION_SQUIGGLY:
                return "annotation_squiggly";
            case SHORTCUT_ANNOTATION_TEXTBOX:
                return "annotation_textbox";
            case SHORTCUT_ANNOTATION_COMMENT:
                return "annotation_comment";
            case SHORTCUT_ANNOTATION_RECTANGLE:
                return "annotation_rectangle";
            case SHORTCUT_ANNOTATION_OVAL:
                return "annotation_oval";
            case SHORTCUT_ANNOTATION_DRAW:
                return "annotation_draw";
            case SHORTCUT_ANNOTATION_ERASER:
                return "annotation_eraser";
            case SHORTCUT_ANNOTATION_LINE:
                return "annotation_line";
            case SHORTCUT_ANNOTATION_ARROW:
                return "annotation_arrow";
            case SHORTCUT_ANNOTATION_SIGNATURE:
                return "annotation_signature";
            case SHORTCUT_ANNOTATION_IMAGE:
                return "annotation_image";
            case SHORTCUT_ANNOTATION_HYPERLINK:
                return "annotation_hyperlink";
            case SHORTCUT_DELETE_ANNOTATION:
                return "delete_annotation";
            case SHORTCUT_GO_TO_NEXT_DOC:
                return "go_to_next_doc";
            case SHORTCUT_GO_TO_PREV_DOC:
                return "go_to_prev_doc";
            case SHORTCUT_FIND:
                return "find";
            case SHORTCUT_GO_TO_NEXT_SEARCH:
                return "go_to_next_search";
            case SHORTCUT_GO_TO_PREV_SEARCH:
                return "go_to_prev_search";
            case SHORTCUT_UNDO:
                return "undo";
            case SHORTCUT_REDO:
                return "redo";
            case SHORTCUT_COPY:
                return "copy";
            case SHORTCUT_CUT:
                return "cut";
            case SHORTCUT_PASTE:
                return "paste";
            case SHORTCUT_PRINT:
                return "print";
            case SHORTCUT_ADD_BOOKMARK:
                return "add_bookmark";
            case SHORTCUT_PAGE_UP:
                return "page_up";
            case SHORTCUT_PAGE_DOWN:
                return "page_down";
            case SHORTCUT_GO_TO_FIRST_PAGE:
                return "go_to_first_page";
            case SHORTCUT_GO_TO_LAST_PAGE:
                return "go_to_last_page";
            case SHORTCUT_JUMP_PAGE_BACK:
                return "jump_page_back";
            case SHORTCUT_JUMP_PAGE_FORWARD:
                return "jump_page_forward";
            case SHORTCUT_START_EDIT_SELECTED_ANNOT:
                return "start_edit_selected_annot";
            case SHORTCUT_SWITCH_FORM:
                return "switch_form";
            case SHORTCUT_ROTATE_CLOCKWISE:
                return "rotate_clockwise";
            case SHORTCUT_ROTATE_COUNTER_CLOCKWISE:
                return "rotate_counter_clockwise";
            case SHORTCUT_ZOOM_IN:
                return "zoom_in";
            case SHORTCUT_ZOOM_OUT:
                return "zoom_out";
            case SHORTCUT_RESET_ZOOM:
                return "reset_zoom";
            case SHORTCUT_OPEN_DRAWER:
                return "open_drawer";
            case SHORTCUT_COMMIT_CLOSE_TEXT:
                return "commit_close_text";
            case SHORTCUT_COMMIT_CLOSE_DRAW:
                return "commit_close_draw";
            case SHORTCUT_SWITCH_INK:
                return "switch_ink";
            case SHORTCUT_ERASE_INK:
                return "erase_ink";
            case SHORTCUT_CANCEL_TOOL:
                return "cancel_tool";
            case SHORTCUT_CLOSE_MENU:
                return "close_menu";
            case SHORTCUT_CLOSE_TAB:
                return "close_tab";
            case SHORTCUT_CLOSE_APP:
                return "close_app";
            default:
                return "not_known";
        }
    }

    // event identifier
    public final static int EVENT_SCREEN_VIEWER = 1;
    public final static int EVENT_SCREEN_RECENT = 2;
    public final static int EVENT_SCREEN_FAVORITES = 3;
    public final static int EVENT_SCREEN_FOLDERS = 4;
    public final static int EVENT_SCREEN_ALL_DOCUMENTS = 5;
    public final static int EVENT_SCREEN_SD_CARD = 6;
    public final static int EVENT_SCREEN_SETTINGS = 7;
    public final static int EVENT_OPEN_FILE = 8;
    public final static int EVENT_CREATE_NEW = 9;
    public final static int EVENT_MERGE = 10;
    public final static int EVENT_VIEWER_SEARCH = 11;
    public final static int EVENT_VIEWER_SEARCH_LIST_ALL = 12;
    public final static int EVENT_VIEWER_SHARE = 13;
    public final static int EVENT_VIEWER_PRINT = 14;
    public final static int EVENT_VIEWER_EDIT_PAGES_ADD = 15;
    public final static int EVENT_VIEWER_EDIT_PAGES_DELETE = 16;
    public final static int EVENT_VIEWER_EDIT_PAGES_ROTATE = 17;
    public final static int EVENT_VIEWER_EDIT_PAGES_REARRANGE = 18;
    public final static int EVENT_VIEWER_UNDO = 19;
    public final static int EVENT_VIEWER_REDO = 20;
    public final static int EVENT_VIEWER_ANNOTATION_TOOLBAR_OPEN = 21;
    public final static int EVENT_VIEWER_ANNOTATION_TOOLBAR_CLOSE = 22;
    public final static int EVENT_ANNOTATION_TOOLBAR = 23;
    public final static int EVENT_VIEWER_VIEW_MODE_OPEN = 24;
    public final static int EVENT_VIEWER_VIEW_MODE_CLOSE = 25;
    public final static int EVENT_VIEW_MODE = 26;
    public final static int EVENT_VIEWER_THUMBNAILS_VIEW_OPEN = 27;
    public final static int EVENT_VIEWER_THUMBNAILS_VIEW_CLOSE = 28;
    public final static int EVENT_THUMBNAILS_VIEW = 29;
    public final static int EVENT_VIEWER_NAVIGATE_BY = 30;
    public final static int EVENT_VIEWER_NAVIGATION_LISTS_OPEN = 31;
    public final static int EVENT_VIEWER_NAVIGATION_LISTS_CLOSE = 32;
    public final static int EVENT_VIEWER_NAVIGATION_LISTS_CHANGE = 33;
    public final static int EVENT_USER_BOOKMARKS = 34;
    public final static int EVENT_ANNOTATIONS_LIST = 35;
    public final static int EVENT_SHORTCUT = 36;
    public final static int EVENT_QUICK_MENU_OPEN = 37;
    public final static int EVENT_QUICK_MENU_CLOSE = 38;
    public final static int EVENT_QUICK_MENU_ACTION = 39;
    public final static int EVENT_STYLE_PICKER_OPEN = 40;
    public final static int EVENT_STYLE_PICKER_SELECT_COLOR = 41;
    public final static int EVENT_STYLE_PICKER_ADD_FAVORITE = 42;
    public final static int EVENT_STYLE_PICKER_REMOVE_FAVORITE = 43;
    public final static int EVENT_STYLE_PICKER_CLOSE = 44;
    public final static int EVENT_SIG_STATE_POPUP_OPEN = 45;
    public final static int EVENT_HUGE_THUMBNAIL = 46;
    public final static int EVENT_FIRST_TIME = 47;
    public final static int EVENT_CREATE_IMAGE_STAMP = 48;
    public final static int EVENT_EDIT_TOOLBAR = 49;
    public final static int EVENT_LOW_MEMORY = 50;
    public final static int EVENT_STYLE_PICKER_SELECT_THICKNESS = 51;
    public final static int EVENT_STYLE_PICKER_SELECT_OPACITY = 52;
    public final static int EVENT_STYLE_PICKER_SELECT_TEXT_SIZE = 53;
    public final static int EVENT_STYLE_PICKER_SELECT_PRESET = 54;
    public final static int EVENT_STYLE_PICKER_DESELECT_PRESET = 55;
    public final static int EVENT_CROP_PAGES = 56;
    public final static int EVENT_CUSTOM_COLOR_MODE = 57;
    public final static int EVENT_VIEWER_SHOW_ALL_TOOLS = 58;
    public final static int EVENT_UNDO_REDO_DISMISSED_NO_ACTION = 59;
    public final static int EVENT_STYLE_PICKER_SELECT_RULER_BASE_VALUE = 60;
    public final static int EVENT_STYLE_PICKER_SELECT_RULER_TRANSLATE_VALUE = 61;
    public final static int EVENT_ADD_RUBBER_STAMP = 62;

    protected String getEvent(int eventId) {
        switch (eventId) {
            case EVENT_SCREEN_VIEWER:
                return "screen_Viewer";
            case EVENT_SCREEN_RECENT:
                return "screen_Recent";
            case EVENT_SCREEN_FAVORITES:
                return "screen_Favorites";
            case EVENT_SCREEN_FOLDERS:
                return "screen_Folders";
            case EVENT_SCREEN_ALL_DOCUMENTS:
                return "screen_AllDocuments";
            case EVENT_SCREEN_SD_CARD:
                return "screen_SDCard";
            case EVENT_SCREEN_SETTINGS:
                return "screen_Settings";
            case EVENT_OPEN_FILE:
                return "open_file";
            case EVENT_CREATE_NEW:
                return "create_new";
            case EVENT_MERGE:
                return "merge";
            case EVENT_VIEWER_SEARCH:
                return "viewer_search";
            case EVENT_VIEWER_SEARCH_LIST_ALL:
                return "viewer_search_list_all";
            case EVENT_VIEWER_SHARE:
                return "viewer_share";
            case EVENT_VIEWER_PRINT:
                return "viewer_print";
            case EVENT_VIEWER_EDIT_PAGES_ADD:
                return "viewer_edit_pages_add";
            case EVENT_VIEWER_EDIT_PAGES_DELETE:
                return "…viewer_edit_pages_delete";
            case EVENT_VIEWER_EDIT_PAGES_ROTATE:
                return "viewer_edit_pages_rotate";
            case EVENT_VIEWER_EDIT_PAGES_REARRANGE:
                return "viewer_edit_pages_rearrange";
            case EVENT_VIEWER_UNDO:
                return "viewer_undo";
            case EVENT_VIEWER_REDO:
                return "viewer_redo";
            case EVENT_VIEWER_ANNOTATION_TOOLBAR_OPEN:
                return "viewer_annotation_toolbar_open";
            case EVENT_VIEWER_ANNOTATION_TOOLBAR_CLOSE:
                return "viewer_annotation_toolbar_close";
            case EVENT_ANNOTATION_TOOLBAR:
                return "annotation_toolbar";
            case EVENT_VIEWER_VIEW_MODE_OPEN:
                return "viewer_view_mode_open";
            case EVENT_VIEWER_VIEW_MODE_CLOSE:
                return "viewer_view_mode_close";
            case EVENT_VIEW_MODE:
                return "view_mode";
            case EVENT_VIEWER_THUMBNAILS_VIEW_OPEN:
                return "viewer_thumbnails_view_open";
            case EVENT_VIEWER_THUMBNAILS_VIEW_CLOSE:
                return "viewer_thumbnails_view_close";
            case EVENT_THUMBNAILS_VIEW:
                return "thumbnails_view";
            case EVENT_VIEWER_NAVIGATE_BY:
                return "viewer_navigate_by";
            case EVENT_VIEWER_NAVIGATION_LISTS_OPEN:
                return "viewer_navigation_lists_open";
            case EVENT_VIEWER_NAVIGATION_LISTS_CLOSE:
                return "viewer_navigation_lists_close";
            case EVENT_VIEWER_NAVIGATION_LISTS_CHANGE:
                return "viewer_navigation_lists_change";
            case EVENT_USER_BOOKMARKS:
                return "user_bookmarks";
            case EVENT_ANNOTATIONS_LIST:
                return "annotations_list";
            case EVENT_SHORTCUT:
                return "shortcut";
            case EVENT_LOW_MEMORY:
                return "low_memory";
            case EVENT_QUICK_MENU_OPEN:
                return "quickmenu_open";
            case EVENT_QUICK_MENU_CLOSE:
                return "quickmenu_dismiss";
            case EVENT_QUICK_MENU_ACTION:
                return "quickmenu";
            case EVENT_STYLE_PICKER_OPEN:
                return "stylepicker_open";
            case EVENT_STYLE_PICKER_SELECT_COLOR:
                return "stylepicker_select_color";
            case EVENT_STYLE_PICKER_ADD_FAVORITE:
                return "stylepicker_add_favorite";
            case EVENT_STYLE_PICKER_REMOVE_FAVORITE:
                return "stylepicker_remove_favorite";
            case EVENT_STYLE_PICKER_CLOSE:
                return "stylepicker_close";
            case EVENT_SIG_STATE_POPUP_OPEN:
                return "signature_state_popup_opened";
            case EVENT_HUGE_THUMBNAIL:
                return "huge_thumbnail";
            case EVENT_FIRST_TIME:
                return "first_time_run";
            case EVENT_CREATE_IMAGE_STAMP:
                return "create_image_stamp";
            case EVENT_EDIT_TOOLBAR:
                return "edit_toolbar";
            case EVENT_STYLE_PICKER_SELECT_THICKNESS:
                return "stylepicker_select_thickness";
            case EVENT_STYLE_PICKER_SELECT_OPACITY:
                return "stylepicker_select_opacity";
            case EVENT_STYLE_PICKER_SELECT_TEXT_SIZE:
                return "stylepicker_select_text_size";
            case EVENT_STYLE_PICKER_SELECT_RULER_BASE_VALUE:
                return "stylepicker_select_ruler_base_value";
            case EVENT_STYLE_PICKER_SELECT_RULER_TRANSLATE_VALUE:
                return "stylepicker_select_ruler_translate_value";
            case EVENT_STYLE_PICKER_SELECT_PRESET:
                return "stylepicker_select_preset";
            case EVENT_STYLE_PICKER_DESELECT_PRESET:
                return "stylepicker_deselect_preset";
            case EVENT_CROP_PAGES:
                return "crop_pages";
            case EVENT_CUSTOM_COLOR_MODE:
                return "custom_color_mode";
            case EVENT_VIEWER_SHOW_ALL_TOOLS:
                return "viewer_show_all_tools";
            case EVENT_UNDO_REDO_DISMISSED_NO_ACTION:
                return "undoredo_dismissed_noaction";
            case EVENT_ADD_RUBBER_STAMP:
                return "add_rubber_stamp";
            default:
                return "not_known";
        }
    }

    // quick menu type
    @IntDef({
        QUICK_MENU_TYPE_EMPTY_SPACE,
        QUICK_MENU_TYPE_TEXT_SELECT,
        QUICK_MENU_TYPE_ANNOTATION_SELECT,
        QUICK_MENU_TYPE_TEXT_ANNOTATION_SELECT,
        QUICK_MENU_TYPE_TOOL_SELECT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface QuickMenuType {
    }

    public final static int QUICK_MENU_TYPE_EMPTY_SPACE = 1;
    public final static int QUICK_MENU_TYPE_TEXT_SELECT = 2;
    public final static int QUICK_MENU_TYPE_ANNOTATION_SELECT = 3;
    public final static int QUICK_MENU_TYPE_TEXT_ANNOTATION_SELECT = 4;
    public final static int QUICK_MENU_TYPE_TOOL_SELECT = 5;

    public String getQuickMenuType(int itemId) {
        switch (itemId) {
            case QUICK_MENU_TYPE_EMPTY_SPACE:
                return "empty_space";
            case QUICK_MENU_TYPE_TEXT_SELECT:
                return "text_selected";
            case QUICK_MENU_TYPE_ANNOTATION_SELECT:
                return "annotation_selected";
            case QUICK_MENU_TYPE_TEXT_ANNOTATION_SELECT:
                return "text_annotation_selected";
            case QUICK_MENU_TYPE_TOOL_SELECT:
                return "tool_selected";
            default:
                return "not_known";
        }
    }

    // quick menu type
    @IntDef({STYLE_PICKER_STANDARD, STYLE_PICKER_RECENT, STYLE_PICKER_FAVORITE, STYLE_PICKER_COLOR_WHEEL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StylePickerLocation {
    }

    public final static int STYLE_PICKER_STANDARD = 1;
    public final static int STYLE_PICKER_RECENT = 2;
    public final static int STYLE_PICKER_FAVORITE = 3;
    public final static int STYLE_PICKER_COLOR_WHEEL = 4;

    public String getStylePickerLocation(@StylePickerLocation int location) {
        switch (location) {
            case STYLE_PICKER_STANDARD:
                return "standard";
            case STYLE_PICKER_RECENT:
                return "recent";
            case STYLE_PICKER_FAVORITE:
                return "favorite";
            default:
                return "color_wheel";
        }
    }

    @IntDef({STYLE_PICKER_LOC_QM, STYLE_PICKER_LOC_ANNOT_TOOLBAR, STYLE_PICKER_LOC_SIGNATURE, STYLE_PICKER_LOC_STICKY_NOTE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StylePickerOpenedLocation {
    }

    public final static int STYLE_PICKER_LOC_QM = 1;
    public final static int STYLE_PICKER_LOC_ANNOT_TOOLBAR = 2;
    public final static int STYLE_PICKER_LOC_SIGNATURE = 3;
    public final static int STYLE_PICKER_LOC_STICKY_NOTE = 4;

    public String getStylePickerOpenedFromLocation(int location) {
        switch (location) {
            case STYLE_PICKER_LOC_QM:
                return "quickmenu";
            case STYLE_PICKER_LOC_ANNOT_TOOLBAR:
                return "annotation_toolbar";
            case STYLE_PICKER_LOC_SIGNATURE:
                return "signature_picker";
            case STYLE_PICKER_LOC_STICKY_NOTE:
                return "sticky_note";
            default:
                return "not_known";
        }
    }

    public String getQuickMenuAction(@IdRes int itemId, @Nullable ToolManager.ToolModeBase toolMode) {
        if (itemId == R.id.qm_image_stamper) return "stamper";
        if (itemId == R.id.qm_define) return "define";
        if (itemId == R.id.qm_translate) return "translate";
        if (itemId == R.id.qm_overflow) return "overflow";
        if (itemId == R.id.qm_appearance) return "appearance";
        if (itemId == R.id.qm_text) return "edit_free_text";
        if (itemId == R.id.qm_copy)
            return toolMode == TEXT_SELECT ? "copy_text" : "copy_annotation";
        if (itemId == R.id.qm_paste) return "paste_annotation";
        if (itemId == R.id.qm_delete) return "delete";
        if (itemId == R.id.qm_flatten) return "flatten";
        if (itemId == R.id.qm_note) return "note";
        if (itemId == R.id.qm_arrow) return "arrow";
        if (itemId == R.id.qm_free_text) return "free_text";
        if (itemId == R.id.qm_sticky_note) return "sticky_note";
        if (itemId == R.id.qm_floating_sig) return "signature";
        if (itemId == R.id.qm_form_text) return "form_text";
        if (itemId == R.id.qm_form_check_box) return "form_check_box";
        if (itemId == R.id.qm_form_signature) return "form_signature";
        if (itemId == R.id.qm_rectangle) return "rectangle";
        if (itemId == R.id.qm_link) return "link";
        if (itemId == R.id.qm_line) return "line";
        if (itemId == R.id.qm_oval) return "oval";
        if (itemId == R.id.qm_ink_eraser) return "ink_eraser";
        if (itemId == R.id.qm_free_hand) return "free_hand";
        if (itemId == R.id.qm_form) return "form) return";
        if (itemId == R.id.qm_highlight) return "highlight";
        if (itemId == R.id.qm_underline) return "underline";
        if (itemId == R.id.qm_squiggly) return "squiggly";
        if (itemId == R.id.qm_strikeout) return "strikeout";
        if (itemId == R.id.qm_search) return "search";
        if (itemId == R.id.qm_share) return "share";
        if (itemId == R.id.qm_tts) return "tts";
        if (itemId == R.id.qm_type) return "textmarkup_type:";
        if (itemId == R.id.qm_field_signed) return "field_signed";
        if (itemId == R.id.qm_form_radio_add_item) return "form_radio_add_item";
        if (itemId == R.id.qm_use_saved_sig) return "use_saved_signature";
        if (itemId == R.id.qm_new_signature) return "new_signature";
        if (itemId == R.id.qm_sign_and_save) return "sign_and_save";
        if (itemId == R.id.qm_rotate) return "rotate";
        if (itemId == R.id.qm_open_attachment) return "open_attachment";
        if (itemId == R.id.qm_edit) return "edit_freehand";
        if (itemId == R.id.qm_thickness) return "thickness";
        if (itemId == R.id.qm_color) return "color";
        if (itemId == R.id.qm_1) return "value_1";
        if (itemId == R.id.qm_2) return "value_2";
        if (itemId == R.id.qm_3) return "value_3";
        if (itemId == R.id.qm_4) return "value_4";
        if (itemId == R.id.qm_5) return "value_5";
        if (itemId == R.id.qm_redaction) return "redaction";
        if (itemId == R.id.qm_redact) return "redact";
        return "not_known";
    }

    public final static int NEXT_ACTION_SIGNATURE_FIRST_TIME = 1;
    public final static int NEXT_ACTION_SIGNATURE_NEW = 2;
    public final static int NEXT_ACTION_SIGNATURE_USE_SAVED = 3;
    public final static int NEXT_ACTION_SIGNATURE_DISMISS = 4;
    public final static int NEXT_ACTION_SHAPE_ARROW = 5;
    public final static int NEXT_ACTION_SHAPE_POLYLINE = 6;
    public final static int NEXT_ACTION_SHAPE_OVAL = 7;
    public final static int NEXT_ACTION_SHAPE_POLYGON = 8;
    public final static int NEXT_ACTION_SHAPE_CLOUD = 9;
    public final static int NEXT_ACTION_SHAPES_DISMISS = 10;
    public final static int NEXT_ACTION_FORM_TEXT_FIELD = 11;
    public final static int NEXT_ACTION_FORM_CHECKBOX = 12;
    public final static int NEXT_ACTION_FORM_SIGNATURE = 13;
    public final static int NEXT_ACTION_FORM_DISMISS = 14;

    @IntDef({
        NEXT_ACTION_SIGNATURE_FIRST_TIME,
        NEXT_ACTION_SIGNATURE_NEW,
        NEXT_ACTION_SIGNATURE_USE_SAVED,
        NEXT_ACTION_SIGNATURE_DISMISS,
        NEXT_ACTION_SHAPE_ARROW,
        NEXT_ACTION_SHAPE_POLYLINE,
        NEXT_ACTION_SHAPE_OVAL,
        NEXT_ACTION_SHAPE_POLYGON,
        NEXT_ACTION_SHAPE_CLOUD,
        NEXT_ACTION_SHAPES_DISMISS,
        NEXT_ACTION_FORM_TEXT_FIELD,
        NEXT_ACTION_FORM_CHECKBOX,
        NEXT_ACTION_FORM_SIGNATURE,
        NEXT_ACTION_FORM_DISMISS
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface NEXT_ACTION {
    }

    public String getQuickMenuNextAction(
        @NEXT_ACTION int type
    ) {

        switch (type) {
            case NEXT_ACTION_SIGNATURE_FIRST_TIME:
                return "signature_first_time";
            case NEXT_ACTION_SHAPE_ARROW:
                return "shape_arrow";
            case NEXT_ACTION_SHAPE_CLOUD:
                return "shape_cloud";
            case NEXT_ACTION_SHAPE_OVAL:
                return "shape_oval";
            case NEXT_ACTION_SHAPE_POLYGON:
                return "shape_polygon";
            case NEXT_ACTION_SHAPE_POLYLINE:
                return "shape_polyline";
            case NEXT_ACTION_SIGNATURE_NEW:
                return "signature_new";
            case NEXT_ACTION_SIGNATURE_DISMISS:
                return "signature_dismiss";
            case NEXT_ACTION_SIGNATURE_USE_SAVED:
                return "signature_use_saved";
            case NEXT_ACTION_FORM_CHECKBOX:
                return "form_checkbox";
            case NEXT_ACTION_FORM_DISMISS:
                return "forms_dismiss";
            case NEXT_ACTION_FORM_SIGNATURE:
                return "form_signature";
            case NEXT_ACTION_FORM_TEXT_FIELD:
                return "form_text_field";
            case NEXT_ACTION_SHAPES_DISMISS:
                return "shapes_dismiss";
            default:
                return "not_known";
        }

    }

    public String getColorPickerType(@AnnotStyleDialogFragment.SelectColorMode int type) {
        switch (type) {
            case AnnotStyleDialogFragment.STROKE_COLOR:
                return "stroke_color";
            case AnnotStyleDialogFragment.FILL_COLOR:
                return "fill_color";
            case AnnotStyleDialogFragment.TEXT_COLOR:
                return "text_color";
            case AnnotStyleDialogFragment.COLOR:
                return "color";
            default:
                return "not_known";
        }
    }


    // quick menu type
    @IntDef({CROP_PAGE_AUTO, CROP_PAGE_MANUAL, CROP_PAGE_REMOVE, CROP_PAGE_NO_ACTION, CROP_PAGE_CANCEL_MANUAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CropPageAction {
    }

    public final static int CROP_PAGE_AUTO = 1;
    public final static int CROP_PAGE_MANUAL = 2;
    public final static int CROP_PAGE_REMOVE = 3;
    public final static int CROP_PAGE_NO_ACTION = 4;
    public final static int CROP_PAGE_CANCEL_MANUAL = 5;

    public String getCropPageAction(@CropPageAction int action) {
        switch (action) {
            case CROP_PAGE_AUTO:
                return "automatic";
            case CROP_PAGE_MANUAL:
                return "manual";
            case CROP_PAGE_REMOVE:
                return "remove";
            case CROP_PAGE_NO_ACTION:
                return "noaction";
            case CROP_PAGE_CANCEL_MANUAL:
                return "manual_then_cancel";
            default:
                return "not_known";
        }
    }

    // quick menu type
    @IntDef({CROP_PAGE_ONE_PAGE, CROP_PAGE_ALL_PAGES, CROP_PAGE_EVEN_ODD_PAGES})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CropPageDetails {
    }

    public final static int CROP_PAGE_ONE_PAGE = 1;
    public final static int CROP_PAGE_ALL_PAGES = 2;
    public final static int CROP_PAGE_EVEN_ODD_PAGES = 3;

    public String getCropPageDetails(@CropPageDetails int details) {
        switch (details) {
            case CROP_PAGE_ONE_PAGE:
                return "one_page";
            case CROP_PAGE_ALL_PAGES:
                return "all_pages";
            case CROP_PAGE_EVEN_ODD_PAGES:
                return "even_or_odd_pages";
            default:
                return "not_known";
        }
    }

    public final static int CUSTOM_COLOR_MODE_SELECT = 1;
    public final static int CUSTOM_COLOR_MODE_EDIT = 2;
    public final static int CUSTOM_COLOR_MODE_CANCEL_EDIT = 3;
    public final static int CUSTOM_COLOR_MODE_RESTORE_DEFAULT = 4;
    public final static int CUSTOM_COLOR_MODE_NO_ACTION = 5;
    public final static int CUSTOM_COLOR_MODE_CANCEL_RESTORE_DEFAULT = 6;

    public String getCustomColorModeAction(int action) {
        switch (action) {
            case CUSTOM_COLOR_MODE_SELECT:
                return "select";
            case CUSTOM_COLOR_MODE_EDIT:
                return "edit";
            case CUSTOM_COLOR_MODE_NO_ACTION:
                return "noaction";
            case CUSTOM_COLOR_MODE_RESTORE_DEFAULT:
                return "restore_defaults";
            case CUSTOM_COLOR_MODE_CANCEL_EDIT:
                return "edit_then_cancel";
            case CUSTOM_COLOR_MODE_CANCEL_RESTORE_DEFAULT:
                return "cancel_restore_defaults";
            default:
                return "not_known";
        }
    }

    public final static int RUBBER_STAMP_STANDARD = 1;
    public final static int RUBBER_STAMP_CUSTOM = 2;
    public final static int RUBBER_STAMP_GOOGLE_IMAGE_SEARCH = 3;

    public String getRubberStampType(int type) {
        switch (type) {
            case RUBBER_STAMP_STANDARD:
                return "standard";
            case RUBBER_STAMP_CUSTOM:
                return "custom";
            case RUBBER_STAMP_GOOGLE_IMAGE_SEARCH:
                return "google_image_search";
            default:
                return "not_known";
        }
    }
}
