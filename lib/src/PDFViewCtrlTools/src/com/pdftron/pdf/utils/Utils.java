//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.TextView;
import android.widget.Toast;

import com.pdftron.common.PDFNetException;
import com.pdftron.fdf.FDFDoc;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Bookmark;
import com.pdftron.pdf.ColorPt;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.annots.Popup;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.model.PdfViewCtrlTabInfo;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.sdf.NameTree;
import com.pdftron.sdf.NameTreeIterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.text.Bidi;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/**
 * A general utility class
 */
@SuppressWarnings("WeakerAccess")
public class Utils {

    private static String[] PERMISSIONS_STORAGE = {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE};

    /**
     * The URI is unknown.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int URI_TYPE_UNKNOWN = 0;
    /**
     * The URI is a file.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int URI_TYPE_FILE = 1;
    /**
     * The URI is a document.
     */
    @SuppressWarnings("WeakerAccess")
    public static final int URI_TYPE_DOCUMENT = 2;
    /**
     * The URI is a file tree.
     */
    public static final int URI_TYPE_TREE = 3;

    private static final String URI_PATH_PREFIX_DOCUMENT = "/document/";
    private static final String URI_PATH_PREFIX_TREE = "/tree/";

    /**
     * Checks if this device is running Jelly Bean MR1 or higher.
     *
     * @return true if Jelly Bean MR1 or higher, false otherwise.
     */
    public static boolean isJellyBeanMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    /**
     * Checks if this device is running Jelly Bean MR2 or higher.
     *
     * @return true if Jelly Bean MR2 or higher, false otherwise.
     */
    public static boolean isJellyBeanMR2() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
    }

    /**
     * Checks if this device is running KitKat or higher.
     *
     * @return true if KitKat or higher, false otherwise.
     */
    public static boolean isKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    /**
     * Checks if this device is running KitKat only.
     *
     * @return true if KitKat, false otherwise.
     */
    public static boolean isKitKatOnly() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT;
    }

    /**
     * Checks if this device is running Lollipop or higher.
     *
     * @return true if Lollipop or higher, false otherwise.
     */
    public static boolean isLollipop() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * Checks if this device is running Lollipop MR1 or higher.
     *
     * @return true if Lollipop MR1 or higher, false otherwise.
     */
    @SuppressWarnings("unused")
    public static boolean isLollipopMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    /**
     * Checks if this device is running Marshmallow or higher.
     *
     * @return true if Marshmallow or higher, false otherwise.
     */
    public static boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * Checks if this device is running Nougat or higher.
     *
     * @return true if Nougat or higher, false otherwise.
     */
    public static boolean isNougat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    /**
     * Checks if this device is running Oreo or higher.
     *
     * @return true if Oreo or higher, false otherwise.
     */
    public static boolean isOreo() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /**
     * Copies the specified resource file into the temp folder.
     *
     * @param context      The context
     * @param resId        The resource ID to be copied
     * @param force        True to overwrite the existing resource file
     * @param resourceName The name of resource
     * @return The file path of copied resource file
     * @throws PDFNetException PDFNet exception
     */
    public static String copyResourceToTempFolder(Context context, int resId, boolean force, String resourceName) throws PDFNetException {
        if (context == null) {
            throw new PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "copyResourceToTempFolder()", "Context cannot be null to initialize resource file.");
        } else {
            File resFile = new File(context.getFilesDir() + File.separator + "resourceName");
            if (!resFile.exists() || force) {
                File filesDir = context.getFilesDir();
                StatFs stat = new StatFs(filesDir.getPath());
                long size = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
                if (size < 2903023L) {
                    throw new PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "copyResourceToTempFolder()", "Not enough space available to copy resources file.");
                }

                Resources rs = context.getResources();

                InputStream inputStream = null;
                FileOutputStream outputStream = null;
                try {
                    inputStream = rs.openRawResource(resId);
                    outputStream = context.openFileOutput(resourceName, 0);
                    byte[] buffer = new byte[1024];

                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                    outputStream.flush();
                } catch (Resources.NotFoundException var13) {
                    throw new PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "initializeResource()", "Resource file ID does not exist.");
                } catch (FileNotFoundException var14) {
                    throw new PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "initializeResource()", "Resource file not found.");
                } catch (IOException var15) {
                    throw new PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "initializeResource()", "Error writing resource file to internal storage.");
                } catch (Exception var16) {
                    throw new PDFNetException("", 0L, "com.pdftron.pdf.PDFNet", "initializeResource()", "Unknown error.");
                } finally {
                    Utils.closeQuietly(inputStream);
                    Utils.closeQuietly(outputStream);
                }
            }

            return context.getFilesDir().getAbsolutePath();
        }
    }

    /**
     * Copies the specified resource file into the temp folder with provided name.
     * If file already exists, it will be overridden.
     *
     * @param context The context
     * @param resId   The resource ID to be copied
     * @param name    The name of resource and will be used as name of the output file
     * @param ext     The extension of resource and will be used as extension of the output file
     * @return The file path of copied resource file
     */
    public static File copyResourceToLocal(Context context, int resId, String name, String ext) {
        InputStream is = null;
        OutputStream fos = null;
        File tempFile;
        try {
            tempFile = new File(context.getFilesDir(), name + ext);
            is = context.getResources().openRawResource(resId);
            fos = new FileOutputStream(tempFile);
            IOUtils.copy(is, fos);
        } catch (Exception e) {
            tempFile = null;
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            Utils.closeQuietly(fos);
            Utils.closeQuietly(is);
        }
        return tempFile;
    }

    /**
     * Checks if the device is a tablet.
     *
     * @param context The context
     * @return True if the device is a tablet
     */
    public static boolean isTablet(Context context) {
        if (context == null) {
            return false;
        }
        boolean result = false;
        try {
            result = context.getResources().getBoolean(R.bool.isTablet);
        } catch (Exception e) {
            // Do nothing
        }
        return result;
    }

    /**
     * Checks if the screen size is large.
     *
     * @param context The context
     * @return True if the screen size is large
     */
    public static boolean isLargeScreen(Context context) {
        if (context == null) {
            return false;
        }

        if (!isChromebook(context) || !PdfViewCtrlSettingsManager.isDesktopUI(context)) {
            return false;
        }

        float minWidth = 1024.0f;
        float minHeight = 768.0f;

        Point pt = new Point();
        Utils.getDisplaySize(context, pt);
        float dpx = Utils.convPix2Dp(context, pt.x);
        float dpy = Utils.convPix2Dp(context, pt.y);

        return (dpx > minWidth && dpy > minHeight);
    }

    /**
     * Checks if the screen width is large.
     *
     * @param context The context
     * @return True if the screen size is large
     */
    public static boolean isLargeScreenWidth(Context context) {
        if (context == null) {
            return false;
        }

        if (!isChromebook(context) || !PdfViewCtrlSettingsManager.isDesktopUI(context)) {
            return false;
        }

        float minWidth = 1024.0f;

        Point pt = new Point();
        Utils.getDisplaySize(context, pt);
        float dpx = Utils.convPix2Dp(context, pt.x);

        return (dpx > minWidth);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isChromebook(Context context) {
        PackageManager pm = context.getPackageManager();
        //noinspection SimplifiableIfStatement
        if (pm.hasSystemFeature(PackageManager.FEATURE_PC)) {
            return true;
        }
        return Build.BRAND != null && Build.BRAND.equals("chromium")
            && Build.MANUFACTURER != null && Build.MANUFACTURER.equals("chromium");
    }

    /**
     * Checks if the device is in portrait mode.
     *
     * @param context The context
     * @return True if the device is in portrait mode
     */
    public static boolean isPortrait(Context context) {
        return context != null && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * Checks if the device is in landscape mode.
     *
     * @param context The context
     * @return True if the device is in landscape mode
     */
    @SuppressWarnings("unused")
    public static boolean isLandscape(Context context) {
        return context != null && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * Converts a rect in page space to screen space
     *
     * @param pdfViewCtrl The {@link PDFViewCtrl}
     * @param rect        The rectangle in page space
     * @param pageNum     The page number
     * @return The rectangle in screen space
     * @throws PDFNetException PDFNet exception
     */
    public static com.pdftron.pdf.Rect convertFromPageRectToScreenRect(PDFViewCtrl pdfViewCtrl, com.pdftron.pdf.Rect rect, int pageNum) throws PDFNetException {
        rect.normalize();

        double x1 = rect.getX1();
        double y1 = rect.getY1();
        double x2 = rect.getX2();
        double y2 = rect.getY2();

        double[] pts1 = pdfViewCtrl.convPagePtToScreenPt(x1, y1, pageNum);
        double[] pts2 = pdfViewCtrl.convPagePtToScreenPt(x2, y2, pageNum);

        com.pdftron.pdf.Rect retRect = new com.pdftron.pdf.Rect(pts1[0], pts1[1], pts2[0], pts2[1]);
        retRect.normalize();

        return retRect;
    }

    /**
     * Converts a rect in page space to scroll view space
     *
     * @param pdfViewCtrl The {@link PDFViewCtrl}
     * @param rect        The rectangle in page space
     * @param pageNum     The page number
     * @return The rectangle in scroll view space
     * @throws PDFNetException PDFNet exception
     */
    public static com.pdftron.pdf.Rect convertFromPageRectToScrollViewerRect(PDFViewCtrl pdfViewCtrl, com.pdftron.pdf.Rect rect, int pageNum) throws PDFNetException {
        rect.normalize();

        double x1 = rect.getX1();
        double y1 = rect.getY1();
        double x2 = rect.getX2();
        double y2 = rect.getY2();

        double[] pts1 = pdfViewCtrl.convPagePtToHorizontalScrollingPt(x1, y1, pageNum);
        double[] pts2 = pdfViewCtrl.convPagePtToHorizontalScrollingPt(x2, y2, pageNum);

        return new com.pdftron.pdf.Rect(pts1[0], pts1[1], pts2[0], pts2[1]);
    }

    public static com.pdftron.pdf.Rect getPageRect(PDFViewCtrl pdfViewCtrl, int pageNum) {
        if (pageNum >= 1) {
            boolean shouldUnlockRead = false;
            try {
                pdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                Page page = pdfViewCtrl.getDoc().getPage(pageNum);
                if (page != null) {
                    com.pdftron.pdf.Rect r = page.getBox(pdfViewCtrl.getPageBox());
                    r.normalize();
                    return r;
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    pdfViewCtrl.docUnlockRead();
                }
            }
        }
        return null;
    }

    public static com.pdftron.pdf.Rect getScreenRectInPageSpace(PDFViewCtrl pdfViewCtrl, int pageNum) {
        if (pageNum >= 1) {
            boolean shouldUnlockRead = false;
            try {
                pdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                int x1 = 0;
                int y1 = 0;
                int x2 = pdfViewCtrl.getWidth();
                int y2 = pdfViewCtrl.getHeight();

                double[] pts1, pts2, pts3, pts4;
                pts1 = pdfViewCtrl.convScreenPtToPagePt(x1, y1, pageNum);
                pts2 = pdfViewCtrl.convScreenPtToPagePt(x2, y1, pageNum);
                pts3 = pdfViewCtrl.convScreenPtToPagePt(x2, y2, pageNum);
                pts4 = pdfViewCtrl.convScreenPtToPagePt(x1, y2, pageNum);
                double min_x = Math.min(Math.min(Math.min(pts1[0], pts2[0]), pts3[0]), pts4[0]);
                double max_x = Math.max(Math.max(Math.max(pts1[0], pts2[0]), pts3[0]), pts4[0]);
                double min_y = Math.min(Math.min(Math.min(pts1[1], pts2[1]), pts3[1]), pts4[1]);
                double max_y = Math.max(Math.max(Math.max(pts1[1], pts2[1]), pts3[1]), pts4[1]);
                return new com.pdftron.pdf.Rect(min_x, min_y, max_x, max_y);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    pdfViewCtrl.docUnlockRead();
                }
            }
        }
        return null;
    }

    /**
     * Computes the page bounding box of PDFView in the client space.
     *
     * @param pdfViewCtrl The {@link PDFViewCtrl}
     * @param pageNum     The page number
     * @return The rectangle of PDFView in the client space
     */
    public static RectF buildPageBoundBoxOnClient(PDFViewCtrl pdfViewCtrl, int pageNum) {
        RectF rect = null;
        if (pageNum >= 1) {
            boolean shouldUnlockRead = false;
            try {
                pdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                Page page = pdfViewCtrl.getDoc().getPage(pageNum);
                if (page != null) {
                    rect = new RectF();
                    com.pdftron.pdf.Rect r = page.getBox(pdfViewCtrl.getPageBox());

                    double x1 = r.getX1();
                    double y1 = r.getY1();
                    double x2 = r.getX2();
                    double y2 = r.getY2();
                    double[] pts1, pts2, pts3, pts4;

                    // Need to compute the transformed coordinates for the four
                    // corners of the page bounding box, since a page can be rotated.
                    pts1 = pdfViewCtrl.convPagePtToScreenPt(x1, y1, pageNum);
                    pts2 = pdfViewCtrl.convPagePtToScreenPt(x2, y1, pageNum);
                    pts3 = pdfViewCtrl.convPagePtToScreenPt(x2, y2, pageNum);
                    pts4 = pdfViewCtrl.convPagePtToScreenPt(x1, y2, pageNum);

                    double min_x = Math.min(Math.min(Math.min(pts1[0], pts2[0]), pts3[0]), pts4[0]);
                    double max_x = Math.max(Math.max(Math.max(pts1[0], pts2[0]), pts3[0]), pts4[0]);
                    double min_y = Math.min(Math.min(Math.min(pts1[1], pts2[1]), pts3[1]), pts4[1]);
                    double max_y = Math.max(Math.max(Math.max(pts1[1], pts2[1]), pts3[1]), pts4[1]);

                    float sx = pdfViewCtrl.getScrollX();
                    float sy = pdfViewCtrl.getScrollY();
                    rect = new RectF();
                    rect.set((float) min_x + sx, (float) min_y + sy, (float) max_x + sx, (float) max_y + sy);
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    pdfViewCtrl.docUnlockRead();
                }
            }
        }
        return rect;
    }

    /**
     * Snaps a point to the specified rectangle
     *
     * @param point The resulted point snapped to the rectangle
     * @param rect  The rectangle
     */
    public static void snapPointToRect(PointF point, RectF rect) {
        if (rect != null) {
            if (point.x < rect.left) {
                point.x = rect.left;
            } else if (point.x > rect.right) {
                point.x = rect.right;
            }
            if (point.y < rect.top) {
                point.y = rect.top;
            } else if (point.y > rect.bottom) {
                point.y = rect.bottom;
            }
        }
    }

    public static Rect getBBox(ArrayList<com.pdftron.pdf.Point> pagePoints) {
        double min_x = Double.MAX_VALUE;
        double min_y = Double.MAX_VALUE;
        double max_x = Double.MIN_VALUE;
        double max_y = Double.MIN_VALUE;

        for (com.pdftron.pdf.Point point : pagePoints) {
            min_x = Math.min(min_x, point.x);
            max_x = Math.max(max_x, point.x);
            min_y = Math.min(min_y, point.y);
            max_y = Math.max(max_y, point.y);
        }

        try {
            if (min_x == Double.MAX_VALUE && min_y == Double.MAX_VALUE
                && max_x == Double.MIN_VALUE && max_y == Double.MIN_VALUE) {
                // no stroke
                return null;
            } else {
                Rect rect = new Rect(min_x, min_y, max_x, max_y);
                rect.normalize();
                return rect;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return True if the system language is Persian/Arabic
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isSystemLanguagePersianArabic() {
        if (!isJellyBeanMR1()) {
            return false;
        }
        final int directionality = Character.getDirectionality(Locale.getDefault().getDisplayName().charAt(0));
        return directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
            directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }

    /**
     * Checks whether the layout direction is right-to-left.
     *
     * @param context The context
     * @return True if the layout direction is right-to-left
     */
    public static boolean isRtlLayout(Context context) {
        if (isJellyBeanMR1() && context != null) {
            Configuration config = context.getResources().getConfiguration();
            return config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        }
        return false;
    }

    /**
     * Checks whether the specified string is left-to-right.
     *
     * @param str The string
     * @return True if the specified string is left-to-right
     */
    public static boolean isLeftToRightString(String str) {
        // decision is made based on the first character belonging to RTL/LTR unicode values
        // as defined in the Core (UnicodeUtils)
        if (isNullOrEmpty(str)) {
            return false;
        }

        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            char u = str.charAt(i);
            if ((0x0590 <= u && u <= 0x05FF) || (0x0600 <= u && u <= 0x06FF) || (0x0750 <= u && u <= 0x077F)
                || (0xFB50 <= u && u <= 0xFDFF) || (0xFE70 <= u && u <= 0xFEFF)) {
                return false;
            }
            if ((0x0041 <= u && u <= 0x005A) || (0x0061 <= u && u <= 0x007A) || (0xFB00 <= u && u <= 0xFB06)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the specified string is right-to-left.
     *
     * @param str The string
     * @return True if the specified string is right-to-left
     */
    public static boolean isRightToLeftString(String str) {
        // decision is made based on the first character belonging to RTL/LTR unicode values
        // as defined in the Core (UnicodeUtils)
        if (isNullOrEmpty(str)) {
            return false;
        }

        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            char u = str.charAt(i);
            if ((0x0590 <= u && u <= 0x05FF) || (0x0600 <= u && u <= 0x06FF) || (0x0750 <= u && u <= 0x077F)
                || (0xFB50 <= u && u <= 0xFDFF) || (0xFE70 <= u && u <= 0xFEFF)) {
                return true;
            }
            if ((0x0041 <= u && u <= 0x005A) || (0x0061 <= u && u <= 0x007A) || (0xFB00 <= u && u <= 0xFB06)) {
                return false;
            }
        }
        return false;
    }

    /**
     * Checks whether the specified string has any right-to-left characters.
     *
     * @param str The string
     * @return True if the specified string has any right-to-left characters
     */
    @SuppressWarnings("unused")
    public static boolean hasRightToLeftChar(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }

        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            char u = str.charAt(i);
            if ((0x0590 <= u && u <= 0x05FF) || (0x0600 <= u && u <= 0x06FF) || (0x0750 <= u && u <= 0x077F)
                || (0xFB50 <= u && u <= 0xFDFF) || (0xFE70 <= u && u <= 0xFEFF)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts the specified string to Bidi.
     *
     * @param str The string
     * @return The Bidi string
     */
    public static String getBidiString(@NonNull String str) {
        // a temporarily hack on right-to-left scripts until the core will support RTL in search
        StringBuilder out = new StringBuilder(str);
        if (Bidi.requiresBidi(str.toCharArray(), 0, str.length())) {
            Bidi bidi = new Bidi(str, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
            out = new StringBuilder();
            for (int i = 0; i < bidi.getRunCount(); i++) {
                String s1 = str.substring(bidi.getRunStart(i), bidi.getRunLimit(i));
                if (bidi.getRunLevel(i) % 2 == 1) {
                    s1 = new StringBuffer(s1).reverse().toString();
                }
                out.append(s1);
            }
        }
        return out.toString();
    }

    /**
     * Returns digits based on the Locale.
     *
     * @param input The digits in string format
     * @return The digits based on the Locale
     */
    public static String getLocaleDigits(String input) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch >= '0' && ch <= '9') {
                output.append(String.format(Locale.getDefault(), "%d", (int) ch - (int) '0'));
            } else {
                output.append(ch);
            }
        }
        return output.toString();
    }

    /**
     * Checks whether the string is null or empty.
     *
     * @param string the string to be checked against
     * @return true if the string is null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(String string) {
        return string == null || string.trim().isEmpty();
    }

    /**
     * Returns URI column info.
     *
     * @param context    The context
     * @param contentUri The content URI
     * @param column     The column
     * @return The URI column info
     */
    private static String getUriColumnInfo(@NonNull Context context, @Nullable Uri contentUri, String column) {
        if (contentUri == null) {
            return null;
        }

        Cursor cursor = null;
        ContentResolver contentResolver = getContentResolver(context);
        if (contentResolver == null) {
            return null;
        }
        try {
            String[] proj = {column};
            cursor = contentResolver.query(contentUri,
                proj, // Which columns to return
                null,       // WHERE clause; which rows to return (all rows)
                null,       // WHERE clause selection arguments (none)
                null); // Order-by clause (ascending by name)
            if (null != cursor && cursor.moveToFirst() && cursor.getColumnCount() > 0 && cursor.getCount() > 0) {
                int column_index = cursor.getColumnIndexOrThrow(proj[0]);
                return cursor.getString(column_index);
            }
            return "";
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }

    /**
     * Returns real path from the specified image URI.
     *
     * @param context  The context
     * @param imageUri The image URI
     * @return The real path from the specified image URI
     */
    public static String getRealPathFromImageURI(Context context, Uri imageUri) {
        if (null == context || null == imageUri) {
            return "";
        }
        Cursor cursor = null;
        ContentResolver contentResolver = getContentResolver(context);
        if (contentResolver == null) {
            return "";
        }
        try {
            // can post image
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = contentResolver.query(imageUri,
                proj, // Which columns to return
                null,       // WHERE clause; which rows to return (all rows)
                null,       // WHERE clause selection arguments (none)
                null); // Order-by clause (ascending by name)
            if (cursor != null && cursor.moveToFirst() && cursor.getColumnCount() > 0 && cursor.getCount() > 0) {
                int column_index = cursor.getColumnIndexOrThrow(proj[0]);
                return cursor.getString(column_index);
            }
            return "";
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }

    /**
     * Returns real path from the specified image URI.
     *
     * @param context        The context
     * @param imageUri       The image URI
     * @param backupFilepath The backup file path
     * @return The bitmap from the specified image URI
     */
    public static Bitmap getBitmapFromImageUri(Context context, Uri imageUri, String backupFilepath) {
        if (null == context || null == imageUri) {
            return null;
        }
        ContentResolver contentResolver = Utils.getContentResolver(context);
        if (contentResolver == null) {
            return null;
        }
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri);
            if (bitmap == null) {
                // Add image to the output file
                if (!isNullOrEmpty(backupFilepath)) {
                    FileInputStream fileInputStream = new FileInputStream(backupFilepath);
                    bitmap = BitmapFactory.decodeStream(fileInputStream);
                }
            }
            return bitmap;
        } catch (FileNotFoundException ignored) {
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } catch (OutOfMemoryError oom) {
            Utils.manageOOM(context, null);
        }
        return null;
    }

    /**
     * Returns the display name of the specified URI
     *
     * @param context    The context
     * @param contentUri The content URI
     * @return The display name of the specified URI
     */
    public static String getUriDisplayName(@NonNull Context context, @Nullable Uri contentUri) {
        if (contentUri == null) {
            return null;
        }

        String displayName = null;
        String[] projection = {OpenableColumns.DISPLAY_NAME};

        if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(contentUri.getScheme())) {
            return contentUri.getLastPathSegment();
        }
        Cursor cursor = null;
        ContentResolver contentResolver = getContentResolver(context);
        if (contentResolver == null) {
            return null;
        }
        try {
            cursor = contentResolver.query(contentUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst() && cursor.getColumnCount() > 0 && cursor.getCount() > 0) {
                int nameIndex = cursor.getColumnIndexOrThrow(projection[0]);
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            displayName = null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return displayName;
    }

    /**
     * Returns the file size of the specified URI
     *
     * @param contentResolver The content resolver
     * @param uri             The URI
     * @return The file size of the specified URI
     */
    public static long getUriFileSize(@NonNull ContentResolver contentResolver, @NonNull Uri uri) {
        long size = -1;

        // Get file name, size, and path if available.
        if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            File file = new File(uri.getPath());
            if (file.exists()) {
                size = file.length();
            }
        } else {
            // Try to get the display name and size from the content resolver.
            final String[] projection = {OpenableColumns.SIZE};
            Cursor cursor = null;
            try {
                cursor = contentResolver.query(uri, projection, null, null, null);
                if (cursor != null && cursor.getColumnCount() > 0 && cursor.moveToFirst()) {
                    // Get size if available.
                    int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (index >= 0) {
                        size = cursor.getInt(index);
                    }
                }
            } catch (Exception ignored) {
                size = -1;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return size;
    }

    /**
     * Returns the file extension of the specified URI
     *
     * @param contentResolver The content resolver
     * @param uri             The URI
     * @return The file extension of the specified URI
     */
    public static String getUriExtension(@NonNull ContentResolver contentResolver, @NonNull Uri uri) {
        String extension;

        // Check uri format
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            // If scheme is a content
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(contentResolver.getType(uri));
        } else {
            // If scheme is a File
            //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
        }

        return extension;
    }

    /**
     * Converts density independent pixels to physical pixels.
     */
    public static float convDp2Pix(@NonNull Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return dp * density;
    }

    /**
     * Converts physical pixels to density independent pixels.
     */
    public static float convPix2Dp(@NonNull Context context, float pix) {
        float density = context.getResources().getDisplayMetrics().density;
        return pix / density;
    }

    /**
     * Gets the size of the display, in pixels.
     *
     * @param context the Context
     * @param outSize A Point object to receive the size information
     */
    public static void getDisplaySize(@NonNull Context context, Point outSize) {
        if (outSize == null) {
            return;
        }
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            Display display = wm.getDefaultDisplay();
            display.getSize(outSize);
        }
    }

    /**
     * Checks whether should request thumbnail file
     *
     * @param cr            The content resolver
     * @param fileUriString The URI
     * @return True if not request thumbnail file
     */
    public static boolean isDoNotRequestThumbFile(@Nullable ContentResolver cr, @Nullable String fileUriString) {
        if (cr == null || isNullOrEmpty(fileUriString)) {
            return false;
        }
        try {
            Uri fileUri = Uri.parse(fileUriString);
            String mime = cr.getType(fileUri);
            if (mime != null) {
                String[] mimeTypes = {"application/msword"};
                for (String type : mimeTypes) {
                    if (mime.equalsIgnoreCase(type)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    // TODO: hack
    public static boolean isDoNotRequestThumbFile(String filePath) {
        if (isNullOrEmpty(filePath)) {
            return false;
        }
        String ext = Utils.getExtension(filePath);
        String[] fileTypes = {"doc"};
        for (String type : fileTypes) {
            if (ext.equalsIgnoreCase(type)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether the specified URI is an office document.
     *
     * @param cr      The content resolver
     * @param fileUri The URI
     * @return True if the specified URI is an office document
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isOfficeDocument(@NonNull ContentResolver cr, Uri fileUri) {
        try {
            String mime = cr.getType(fileUri);
            if (mime != null) {
                String[] mimeTypes = Constants.OFFICE_FILE_MIME_TYPES;
                for (String type : mimeTypes) {
                    if (mime.equalsIgnoreCase(type)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    /**
     * Checks whether the specified file is an office document.
     *
     * @param filePath The file path
     * @return True if the specified file is an office document
     */
    public static boolean isOfficeDocument(String filePath) {
        if (isNullOrEmpty(filePath)) {
            return false;
        }

        String ext = Utils.getExtension(filePath);
        String[] fileTypes = Constants.FILE_NAME_EXTENSIONS_DOC;
        for (String type : fileTypes) {
            if (ext.equalsIgnoreCase(type)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether the specified URI is an image file.
     *
     * @param cr      The content resolver
     * @param fileUri The URI
     * @return True if the specified URI is an image file
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isImageFile(@NonNull ContentResolver cr, Uri fileUri) {
        try {
            String mime = cr.getType(fileUri);
            if (mime != null) {
                String[] mimeTypes = Constants.IMAGE_FILE_MIME_TYPES;
                for (String type : mimeTypes) {
                    if (mime.equalsIgnoreCase(type)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    /**
     * Checks whether the specified file is an image file.
     *
     * @param filePath The file path
     * @return True if the specified file is an image file
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isImageFile(String filePath) {
        if (isNullOrEmpty(filePath)) {
            return false;
        }

        String ext = Utils.getExtension(filePath);
        String[] fileTypes = Constants.FILE_NAME_EXTENSIONS_IMAGE;
        for (String type : fileTypes) {
            if (ext.equalsIgnoreCase(type)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether the specified file is non-PDF file.
     *
     * @param filePath The file path
     * @return True if the specified file is non-PDF file
     */
    public static boolean isNotPdf(String filePath) {
        return isOfficeDocument(filePath) || isImageFile(filePath);
    }

    /**
     * Checks whether the specified URI is non-PDF file.
     *
     * @param cr      The content resolver
     * @param fileUri The URI
     * @return True if the specified URI is non-PDF file
     */
    public static boolean isNotPdf(@NonNull ContentResolver cr, Uri fileUri) {
        return isOfficeDocument(cr, fileUri) || isImageFile(cr, fileUri) || isNotPdf(fileUri.toString());
    }

    /**
     * Decrypts the specified value
     *
     * @param context The context
     * @param value   The value
     * @return The decrypted value
     */
    public static String decryptIt(Context context, String value) {
        if (isNullOrEmpty(value)) {
            return value;
        }

        String cryptoPass = context.getString(context.getApplicationInfo().labelRes);
        try {
            DESKeySpec keySpec = new DESKeySpec(cryptoPass.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] encrypedPwdBytes = Base64.decode(value, Base64.DEFAULT);
            // cipher is not thread safe
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypedValueBytes = (cipher.doFinal(encrypedPwdBytes));

            String decrypedValue = new String(decrypedValueBytes);
            Log.d("MiscUtils", "Decrypted: " + value + " -> " + decrypedValue);
            return decrypedValue;

        } catch (Exception e) {
            Log.e(e.getClass().getName(), e.getMessage());
        }
        return value;
    }

    /**
     * Encrypts the specified value
     *
     * @param context The context
     * @param value   The value
     * @return The encrypted value
     */
    public static String encryptIt(Context context, String value) {
        String cryptoPass = context.getString(context.getApplicationInfo().labelRes);
        try {
            DESKeySpec keySpec = new DESKeySpec(cryptoPass.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] clearText = value.getBytes("UTF8");
            // Cipher is not thread safe
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            String encrypedValue = Base64.encodeToString(cipher.doFinal(clearText), Base64.DEFAULT);
            Log.d("MiscUtils", "Encrypted: " + value + " -> " + encrypedValue);
            return encrypedValue;

        } catch (Exception e) {
            Log.d(e.getClass().getName(), e.getMessage());
        }
        return value;
    }

    /**
     * Returns password if it is saved in {@link PdfViewCtrlTabsManager}.
     *
     * @param context  The Context
     * @param filepath The file path
     * @return The password saved for the specified file
     */
    @NonNull
    public static String getPassword(@NonNull Context context, @Nullable String filepath) {
        if (isNullOrEmpty(filepath)) {
            return "";
        }
        PdfViewCtrlTabInfo info = PdfViewCtrlTabsManager.getInstance().getPdfFViewCtrlTabInfo(context, filepath);
        if (info != null && !isNullOrEmpty(info.password)) {
            return decryptIt(context, info.password);
        }
        return "";
    }

    /**
     * Creates an alert dialog builder.
     *
     * @param context   The context
     * @param messageId The id of the message
     * @return The dialog builder
     */
    public static AlertDialog.Builder getAlertDialogNoTitleBuilder(Context context, int messageId) {
        return getAlertDialogBuilder(context, context.getResources().getString(messageId), "");
    }

    /**
     * Creates an alert dialog builder.
     *
     * @param context   The context
     * @param messageId The id of the message
     * @return The dialog builder
     */
    public static AlertDialog.Builder getAlertDialogBuilder(Context context, int messageId, int titleId) {
        return getAlertDialogBuilder(context, context.getResources().getString(messageId), context.getResources().getString(titleId));
    }

    /**
     * Creates an alert dialog builder.
     *
     * @param context The context
     * @param message The message of the dialog
     * @param title   The title of the dialog
     * @return The dialog builder
     */
    public static AlertDialog.Builder getAlertDialogBuilder(Context context, String message, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage(message)
            .setCancelable(true);

        if (!isNullOrEmpty(title)) {
            builder.setTitle(title);
        }
        return builder;
    }

    /**
     * Gets a human readable format of a byte size.
     * <p/>
     * See http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
     *
     * @param bytes the number of bytes
     * @param si    true for SI units or false for binary units
     * @return the human readable format of the byte size.
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            if (isSystemLanguagePersianArabic()) {
                return String.format(Locale.getDefault(), "%dB", bytes); // force put B in front of the number
            }
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        //String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + "";
        if (isSystemLanguagePersianArabic()) {
            return String.format(Locale.getDefault(), "%.1f%sB", bytes / Math.pow(unit, exp), pre); // force put B in front of the number
        }
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static Long getReadableByteValue(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes;
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        return (long) (bytes / Math.pow(unit, exp));
    }

    public static String getReadableByteUnit(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return "B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        //String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + "";
    }

    /**
     * Returns the number of bytes in string format
     *
     * @param bytes The number of bytes
     * @return The number of bytes in string format
     */
    public static String getByteCount(long bytes) {
        return NumberFormat.getNumberInstance().format(bytes);
    }

    /**
     * Returns the display name from the image URI
     *
     * @param context        The context
     * @param imageUri       The URI
     * @param backupFilepath The backup file path
     * @return The display name from the image URI
     */
    public static String getDisplayNameFromImageUri(Context context, Uri imageUri, String backupFilepath) {
        if (null == context || null == imageUri) {
            return null;
        }
        try {
            String filename = getUriColumnInfo(context, imageUri, MediaStore.Images.Media.DISPLAY_NAME);
            if (isNullOrEmpty(filename)) {
                if (!isNullOrEmpty(backupFilepath)) {
                    filename = FilenameUtils.getBaseName(backupFilepath);
                }
            }
            return FilenameUtils.removeExtension(filename);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        return null;
    }

    /**
     * Checks whether the specified file is stored in a SD card.
     *
     * @param context The context
     * @param file    The file
     * @return True if the specified file is stored in a SD card
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean isSdCardFile(@Nullable Context context, File file) {
        if (context == null || !isKitKat()) {
            // not applicable for below kitkat
            return false;
        }
        if (file != null) {
            if (file.getParentFile() == null || file.getAbsolutePath().equals("/storage")) {
                // File cannot be on a SD-card
                return false;
            }
            final File storageDirectory = Environment.getExternalStorageDirectory();
            final File[] rootDirs = context.getExternalFilesDirs(null);
            if (rootDirs != null && rootDirs.length > 0) {
                for (File dir : rootDirs) {
                    if (dir != null) {
                        try {
                            if (!FilenameUtils.equals(storageDirectory.getAbsolutePath(), dir.getAbsolutePath()) &&
                                !FileUtils.directoryContains(storageDirectory, dir)) {
                                while (dir.getParentFile() != null && !dir.getAbsolutePath().equalsIgnoreCase("/storage")) {
                                    if (FilenameUtils.equals(file.getAbsolutePath(), dir.getAbsolutePath()) ||
                                        FileUtils.directoryContains(dir, file)) {
                                        // The current folder is on the SD-card
                                        return true;
                                    }
                                    dir = dir.getParentFile();
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Converts external content URI to a file
     *
     * @param activity The activity
     * @param beamUri  The URI
     * @return The file
     */
    public static File convExternalContentUriToFile(@NonNull Context activity, @Nullable Uri beamUri) {
        if (beamUri == null) {
            return null;
        }

        // Position of the filename in the query Cursor
        int filenameIndex;
        // The filename stored in MediaStore
        String fileName;
        // Test the authority of the URI
        if (!beamUri.getAuthority().contains(MediaStore.AUTHORITY)) {
            /*
             * Handle content URIs for other content providers
             */
            // For a MediaStore content URI
        } else {
            // Get the column that contains the file name
            Cursor pathCursor = null;
            try {
                String[] projection = {MediaStore.MediaColumns.DATA};
                ContentResolver contentResolver = Utils.getContentResolver(activity);
                if (contentResolver == null) {
                    return null;
                }
                pathCursor = contentResolver.query(beamUri, projection, null, null, null);
                // Check for a valid cursor
                if (pathCursor != null && pathCursor.moveToFirst()
                    && pathCursor.getCount() > 0 && pathCursor.getColumnCount() > 0) {
                    // Get the column index in the Cursor
                    filenameIndex = pathCursor.getColumnIndex(
                        MediaStore.MediaColumns.DATA);
                    if (filenameIndex == -1) {
                        return null;
                    }
                    // Get the full file name including path
                    fileName = pathCursor.getString(filenameIndex);
                    // Create a File object for the filename
                    return new File(fileName);
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (pathCursor != null) {
                    pathCursor.close();
                }
            }
        }

        // The query didn't work; return null
        return null;
    }

    /**
     * Shows an alert dialog.
     *
     * @param context The context
     * @param message The message
     * @param title   The title
     */
    public static void showAlertDialogWithLink(@NonNull Context context, String message, @Nullable String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage(Html.fromHtml(message))
            .setCancelable(true)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
        if (title != null && title.length() > 0) {
            builder.setTitle(title);
        }
        final AlertDialog d = builder.create();
        d.show();

        // Make the textview clickable. Must be called after show()
        ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    /**
     * Returns the screen width.
     * Value returned by this method does not necessarily represent the actual screen width
     * (native resolution) of the display.
     *
     * @param context The context
     * @return The screen width
     */
    public static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return 0;
        }
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    /**
     * Returns the screen height.
     * Value returned by this method does not necessarily represent the actual screen height
     * (native resolution) of the display.
     *
     * @param context The context
     * @return The screen height
     */
    @SuppressWarnings("WeakerAccess")
    public static int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return 0;
        }
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;

    }

    /**
     * Returns the real screen width without subtracting any window decor or
     * applying any compatibility scale factors.
     *
     * @param context The context
     * @return The real screen width
     */
    @SuppressWarnings("WeakerAccess")
    public static int getRealScreenWidth(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                return 0;
            }
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getRealSize(size);
            return size.x;
        } else {
            return getScreenWidth(context);
        }
    }

    /**
     * Returns the real screen height without subtracting any window decor or
     * applying any compatibility scale factors.
     *
     * @param context The context
     * @return The real screen height
     */
    public static int getRealScreenHeight(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                return 0;
            }
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getRealSize(size);
            return size.y;
        } else {
            return getScreenHeight(context);
        }
    }

    /**
     * Check whether the screen is too narrow.
     *
     * @param context The context
     * @return True if the screen is too narrow
     */
    public static boolean isScreenTooNarrow(@NonNull Context context) {
        int screenWidth = getScreenWidth(context);
        int minIconSize = (int) convDp2Pix(context, 48);
        // if screen width small than 10 icon space, we think it's too narrow
        return screenWidth < (minIconSize * 9);
    }

    public static int toolbarIconMaxCount(@NonNull Context context) {
        int screenWidth = getScreenWidth(context);
        int minIconSize = (int) convDp2Pix(context, 48);
        return (int) ((double) screenWidth / (double) minIconSize);
    }

    /**
     * Returns the URI tree path.
     *
     * @param uri The URI
     * @return The tree path
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static String getUriTreePath(Uri uri) {
        if (!isLollipop()) {
            return uri.getPath();
        }

        String treePath;
        try {
            treePath = DocumentsContract.getTreeDocumentId(uri);
        } catch (IllegalArgumentException e) {
            treePath = "";
        }
        return treePath;
    }

    /**
     * Returns the URI document path.
     *
     * @param uri The URI
     * @return The document path
     */
    // Document path (content://<authority>/tree/<tree-path>/document/<document-path>)
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static String getUriDocumentPath(Uri uri) {
        if (!isLollipop()) {
            return uri.getPath();
        }
        String docPath;
        try {
            docPath = DocumentsContract.getDocumentId(uri);
        } catch (IllegalArgumentException e) {
            docPath = "";
        }
        return docPath;
    }

    /**
     * Shows an alert dialog.
     * /\p>
     * Note: always call {@link #showAlertDialog(Activity, CharSequence, String)} instead unless you
     * are sure it is running on the UI thread, for example inside onPostExecute, onClick, etc
     *
     * @param context The context
     * @param message The message
     * @param title   The title
     */
    public static void safeShowAlertDialog(final Context context, final CharSequence message, final String title) {
        if (context instanceof Activity && ((Activity) context).isFinishing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
            .setCancelable(true)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
        if (!isNullOrEmpty(title)) {
            builder.setTitle(title);
        }
        builder.create().show();
    }

    /**
     * Shows an alert dialog.
     * /\p>
     * Note: always call {@link #showAlertDialog(Activity, int, int)} instead unless you are sure
     * it is running on the UI thread, for example inside onPostExecute, onClick, etc
     *
     * @param context   The context
     * @param messageId The ID of the message
     * @param titleId   The ID of the title
     */
    public static void safeShowAlertDialog(@NonNull Context context, int messageId, int titleId) {
        safeShowAlertDialog(context, context.getResources().getString(messageId), context.getResources().getString(titleId));
    }

    /**
     * Shows an alert dialog.
     *
     * @param activity The activity
     * @param message  The message
     * @param title    The title
     */
    public static void showAlertDialog(Activity activity, final CharSequence message, final String title) {
        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activity = activityRef.get();
                if (activity != null && !activity.isFinishing()) {
                    safeShowAlertDialog(activity, message, title);
                }
            }
        });
    }

    /**
     * Shows an alert dialog.
     *
     * @param activity  The activity
     * @param messageId The ID of the message
     * @param titleId   The ID of the title
     */
    public static void showAlertDialog(Activity activity, int messageId, int titleId) {
        showAlertDialog(activity, activity.getResources().getString(messageId), activity.getResources().getString(titleId));
    }

    /**
     * Checks whether the specified app is installed on the device.
     *
     * @param context The context
     * @param uri     The app URI
     * @return True if the specified app is installed on the device
     */
    static public boolean isAppInstalledOnDevice(Context context, String uri) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    /**
     * Returns a file path in the specified path that is not used.
     *
     * @param original The original file path
     * @return The file path that is not used
     */
    public static String getFileNameNotInUse(String original) {
        if (isNullOrEmpty(original)) {
            return "";
        }

        String extension = FilenameUtils.getExtension(original);

        String newFileName = original;
        int i = 1;
        while (true) {
            File newFile = new File(newFileName);
            if (!newFile.exists()) {
                break;
            }
            newFileName = FilenameUtils.removeExtension(original) + " (" + String.valueOf(i) + ")." + extension;

            if ((++i % 10) == 0) {
                Random random = new Random();
                i = (Math.abs(random.nextInt()) / 10) * 10 + 1;
            }
        }

        return newFileName;
    }

    /**
     * Returns a file path in the specified path that is not used.
     *
     * @param folder   The folder in external storage
     * @param original The original file name
     * @return The file path that is not used
     */
    public static String getFileNameNotInUse(ExternalFileInfo folder, String original) {
        if (folder == null || isNullOrEmpty(original)) {
            return "";
        }
        String extension = FilenameUtils.getExtension(original);

        String newFileName = original;
        int i = 1;
        while (true) {
            if (folder.getFile(newFileName) == null) {
                // File does not exist
                break;
            }
            newFileName = FilenameUtils.removeExtension(original) + " (" + String.valueOf(i) + ")." + extension;

            if ((++i % 10) == 0) {
                Random random = new Random();
                i = (Math.abs(random.nextInt()) / 10) * 10 + 1;
            }
        }

        return newFileName;
    }

    /**
     * Returns a generic share intent for the specified URI
     *
     * @param activity The activity
     * @param uri      The URI
     * @return The intent
     */
    //single file
    @Nullable
    public static Intent createGenericShareIntent(@NonNull Activity activity, @Nullable Uri uri) {
        if (uri == null) {
            return null;
        }

        Intent sharingIntent;
        String uriStr = uri.toString();
        String mimeType = "application/*";
        // get mime type
        String extension = MimeTypeMap.getFileExtensionFromUrl(uriStr);
        if (isNullOrEmpty(extension)) {
            extension = Utils.getExtension(getUriDisplayName(activity, uri));
        }
        if (!isNullOrEmpty(extension)) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        // get action type, use SEND for every file that we can handle and VIEW for anything else
        if (!isExtensionHandled(extension)) {
            sharingIntent = new Intent(Intent.ACTION_VIEW);
            sharingIntent.setDataAndType(uri, mimeType);
        } else {
            String signature = String.format(activity.getString(R.string.share_email_body), activity.getString(R.string.app_name));
            sharingIntent = ShareCompat.IntentBuilder.from(activity)
                .setStream(uri)
                .setSubject(uri.getLastPathSegment())
                .setHtmlText(signature)
                .getIntent();
            sharingIntent.setType(mimeType); // not always file provider can select the correct type even for new APIs
        }
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return sharingIntent;
    }

    /**
     * Returns a generic share intent for the specified URIs
     *
     * @param activity The activity
     * @param uris     A list of URIs
     * @return The intent
     */

    //multiple files
    @Nullable
    public static Intent createGenericShareIntents(@NonNull Activity activity, ArrayList<Uri> uris) {
        if (uris == null || uris.size() == 0) {
            return null;
        }

        Intent sharingIntent;
        String extension = MimeTypeMap.getFileExtensionFromUrl(uris.get(0).toString());
        String mimeType = "application/*";
        if (extension != null) {
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        // get action type, use SEND for every file that we can handle and VIEW for anything else
        boolean isView = false;
        for (Uri uri : uris) {
            if (!isExtensionHandled(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))) {
                isView = true;
                break;
            }
        }

        if (isView) {
            sharingIntent = new Intent(Intent.ACTION_VIEW);
            sharingIntent.setDataAndType(uris.get(0), mimeType);
        } else {
            ShareCompat.IntentBuilder intentBuilder = ShareCompat.IntentBuilder.from(activity);
            for (Uri uri : uris) {
                if (uri != null) {
                    intentBuilder.addStream(uri);
                }
            }

            StringBuilder subject = new StringBuilder();
            subject.append(activity.getResources().getString(R.string.app_name))
                .append(" ")
                .append(activity.getResources().getString(R.string.document))
                .append("s - ");
            int count = uris.size();
            for (int i = 0; i < count - 1; i++) {
                subject.append(uris.get(i).getLastPathSegment())
                    .append(", ");
            }
            subject.append(uris.get(count - 1).getLastPathSegment());
            intentBuilder.setSubject(subject.toString());
            sharingIntent = intentBuilder.getIntent();
            sharingIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            sharingIntent.setType(mimeType);
        }

        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return sharingIntent;
    }

    /**
     * Returns a generic share intent for the specified file
     *
     * @param activity The activity
     * @param file     The file
     * @return The intent
     */
    //single file
    @Nullable
    public static Intent createShareIntent(@NonNull Activity activity, @NonNull File file) {
        return createGenericShareIntent(activity, getUriForFile(activity, file));
    }

    /**
     * Returns a generic share intent for the specified files
     *
     * @param activity The activity
     * @param files    A list of files
     * @return The intent
     */
    //multiple files
    @Nullable
    public static Intent createShareIntents(@NonNull Activity activity, ArrayList<FileInfo> files) {
        // may not work properly for API before 16
        if (files == null || files.size() == 0) {
            return null;
        }
        ArrayList<Uri> uris = new ArrayList<>(files.size());
        for (int i = 0; i < files.size(); i++) {
            Uri uri = getUriForFile(activity, files.get(i).getFile());
            if (uri != null) {
                uris.add(uri);
            }
        }
        return createGenericShareIntents(activity, uris);
    }

    /**
     * Shares a PDF file.
     *
     * @param activity The activity
     * @param file     The file
     */
    //single file
    public static void sharePdfFile(@NonNull Activity activity, @NonNull File file) {
        try {
            Intent shareIntent = createShareIntent(activity, file);
            if (shareIntent != null && shareIntent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(Intent.createChooser(shareIntent, activity.getResources().getString(R.string.action_file_share)));
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Shares PDF files.
     *
     * @param activity The activity
     * @param files    A list of files
     */
    //multiple files
    public static void sharePdfFiles(@NonNull Activity activity, ArrayList<FileInfo> files) {
        try {
            Intent shareIntent = createShareIntents(activity, files);
            if (shareIntent != null && shareIntent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(Intent.createChooser(shareIntent, activity.getResources().getString(R.string.action_file_share)));
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Shares a URI.
     *
     * @param activity The activity
     * @param file     The file
     */
    //single file
    public static void shareGenericFile(@NonNull Activity activity, @NonNull Uri file) {
        try {
            Intent shareIntent = createGenericShareIntent(activity, file);
            if (shareIntent != null && shareIntent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(Intent.createChooser(shareIntent, activity.getResources().getString(R.string.action_file_share)));
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Shares URIs.
     *
     * @param activity The activity
     * @param files    A list of URIs
     */
    //multiple files
    public static void shareGenericFiles(@NonNull Activity activity, ArrayList<Uri> files) {
        try {
            Intent shareIntent = createGenericShareIntents(activity, files);
            if (shareIntent != null && shareIntent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(Intent.createChooser(shareIntent, activity.getResources().getString(R.string.action_file_share)));
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Converts an integer color to a color point
     *
     * @param color The color as integer
     * @return The color as {@link ColorPt}
     */
    public static ColorPt color2ColorPt(int color) {
        double red = (double) Color.red(color) / 255;
        double green = (double) Color.green(color) / 255;
        double blue = (double) Color.blue(color) / 255;
        ColorPt colorPt = null;
        try {
            colorPt = new ColorPt(red, green, blue);
        } catch (PDFNetException e) {
            e.printStackTrace();
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        return colorPt;
    }

    /**
     * Converts a color point to an integer color
     *
     * @param colorPt The color as {@link ColorPt}
     * @return The color as integer
     */
    public static int colorPt2color(ColorPt colorPt) {
        try {
            int r = (int) (Math.round(colorPt.get(0) * 255));
            int g = (int) (Math.round(colorPt.get(1) * 255));
            int b = (int) (Math.round(colorPt.get(2) * 255));
            return Color.rgb(r, g, b);
        } catch (PDFNetException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Shows soft keyboard.
     *
     * @param context The context
     * @param view    The view
     */
    public static void showSoftKeyboard(Context context, View view) {
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Hides soft keyboard.
     *
     * @param context The context
     * @param view    The view
     */
    public static void hideSoftKeyboard(Context context, View view) {
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && imm.isActive()) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Returns an external file info from the specified URI
     *
     * @param context The context
     * @param uri     The URI
     * @return The external file info
     */
    @Nullable
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static ExternalFileInfo buildExternalFile(@Nullable Context context, Uri uri) {
        if (context == null) {
            return null;
        }

        ExternalFileInfo file = null;
        ExternalFileInfo root = null;
        String treePath = getUriTreePath(uri);
        ContentResolver contentResolver = Utils.getContentResolver(context);
        if (contentResolver == null) {
            return null;
        }
        try {
            // Check if the uri resides under one of the persisted root folders
            List<UriPermission> permissionList = contentResolver.getPersistedUriPermissions();
            for (UriPermission uriPermission : permissionList) {
                ExternalFileInfo tempRoot;
                if (getUriType(uriPermission.getUri()) == URI_TYPE_TREE) {
                    tempRoot = new ExternalFileInfo(context, null, uriPermission.getUri());
                } else {
                    continue;
                }
                if (tempRoot.exists() && tempRoot.isDirectory()) {
                    // Check if specified file is under this root
                    String rootTreePath = tempRoot.getTreePath();
                    if (rootTreePath.equals(treePath)) {
                        root = tempRoot;
                        break;
                    }
                }
            }
            if (root != null) {
                file = root.buildTree(uri);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            file = null;
        }

        return file;
    }

    /**
     * Returns the URI type
     *
     * @param uri The URI
     * @return The URI type. Possible values are {@link #URI_TYPE_UNKNOWN},
     * {@link #URI_TYPE_FILE}, {@link #URI_TYPE_DOCUMENT} and {@link #URI_TYPE_TREE}
     */
    public static int getUriType(Uri uri) {
        int type = URI_TYPE_UNKNOWN;
        if (uri != null) {
            if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
                String path = uri.getPath();
                if (path.startsWith(URI_PATH_PREFIX_DOCUMENT)) {
                    type = URI_TYPE_DOCUMENT;
                } else if (path.startsWith(URI_PATH_PREFIX_TREE)) {
                    type = URI_TYPE_TREE;
                }
            } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
                type = URI_TYPE_FILE;
            }
        }
        return type;
    }

    /**
     * Handles an empty popup
     *
     * @param pdfViewCtrl The PDFViewCtrl
     * @param markup      The markup
     */
    public static void handleEmptyPopup(PDFViewCtrl pdfViewCtrl, Markup markup) {
        try {
            Popup popup = markup.getPopup();
            if (popup == null || !popup.isValid()) {
                popup = Popup.create(pdfViewCtrl.getDoc(), markup.getRect());
                popup.setParent(markup);
                markup.setPopup(popup);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    /**
     * Checks whether the file is modified.
     *
     * @param pdfDoc The PDF doc
     * @return True if the file is modified
     */
    public static boolean isDocModified(PDFDoc pdfDoc) {
        boolean isModified = false;
        if (pdfDoc != null) {
            boolean shouldUnlockRead = false;
            try {
                pdfDoc.lockRead();
                shouldUnlockRead = true;
                isModified = pdfDoc.isModified();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            } finally {
                if (shouldUnlockRead) {
                    unlockReadQuietly(pdfDoc);
                }
            }
        }

        return isModified;
    }

    /**
     * Retrieves the tool cache.
     *
     * @param context       The context
     * @param cacheFileName The cache file name
     * @return The JSON object
     */
    public static JSONObject retrieveToolCache(Context context, String cacheFileName) {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(new File(new File(context.getCacheDir(), "") + cacheFileName)));
            String str = (String) in.readObject();
            return new JSONObject(str);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            e.printStackTrace();
        } finally {
            closeQuietly(in);
        }
        return null;
    }

    public static String getCachedContents(JSONObject obj) {
        try {
            return obj.getString("contents");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static Rect getCachedBBox(JSONObject obj) {
        try {
            JSONObject rectObj = obj.getJSONObject("bbox");
            int x1 = rectObj.getInt("x1");
            int x2 = rectObj.getInt("x2");
            int y1 = rectObj.getInt("y1");
            int y2 = rectObj.getInt("y2");
            return new Rect(x1, y1, x2, y2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks whether the cache file exists.
     *
     * @param context       The context
     * @param cacheFileName The cache file name
     * @return true if the cache file exists
     */
    public static boolean cacheFileExists(Context context, String cacheFileName) {
        if (context == null) {
            return false;
        }
        File file = new File(new File(context.getCacheDir(), "") + cacheFileName);
        Log.d("PdfViewCtrlTabFragment", "cacheFile: " + file.getAbsolutePath() + "exists: " + file.exists() + "; length:" + file.length());
        return file.exists() && file.length() > 0;
    }

    /**
     * Deletes the cache file.
     *
     * @param context       The context
     * @param cacheFileName The cache file name
     */
    public static void deleteCacheFile(Context context, String cacheFileName) {
        if (context == null) {
            return;
        }
        File file = new File(new File(context.getCacheDir(), "") + cacheFileName);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static Bitmap getBitmapByVecDrawable(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
            vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    /**
     * Converts a drawable to a bitmap
     *
     * @param drawable The drawable
     * @return The bitmap
     */
    public static Bitmap convDrawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Checks whether the app has the storage permission.
     *
     * @param context The context
     * @return True if the app has the storage permission
     */
    public static boolean hasStoragePermission(Context context) {
        if (isMarshmallow()) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                // Storage permissions have not been granted.
                Log.i("permission", "Storage permissions has NOT been granted. Needs to request permissions.");
                return false;
            }
        }
        return true;
    }

    /**
     * Requests the user for the storage permission.
     *
     * @param activity    The activity
     * @param requestCode The request code
     */
    public static void requestStoragePermissions(Activity activity, final View layout, final int requestCode) {
        if (layout != null &&
            (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))) {

            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // For example, if the request has been denied previously.
            Log.i("permission", "Displaying storage permission rationale to provide additional context.");

            // Display a SnackBar with an explanation and a button to trigger the request.
            final WeakReference<Activity> activityRef = new WeakReference<>(activity);
            Snackbar.make(layout, R.string.permission_storage_rationale,
                Snackbar.LENGTH_LONG)
                .setAction(activity.getString(R.string.ok).toUpperCase(),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Activity activity = activityRef.get();
                            if (activity != null && !activity.isFinishing()) {
                                ActivityCompat
                                    .requestPermissions(activity, PERMISSIONS_STORAGE,
                                        requestCode);
                            }
                        }
                    })
                .show();
        } else {
            // Storage permissions have not been granted yet. Request them directly.
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, requestCode);
        }
    }

    /**
     * Checks whether the specified extension is handled.
     *
     * @param extension The extension
     * @return True if the specified extension is handled
     */
    public static boolean isExtensionHandled(String extension) {
        return extension != null && !extension.isEmpty()
            && Arrays.asList(Constants.FILE_NAME_EXTENSIONS_VALID).contains(extension.toLowerCase());
    }

    /**
     * Checks whether the specified mime type is handled.
     *
     * @param mimeType The mime type
     * @return True if the specified mime type is handled
     */
    public static boolean isMimeTypeHandled(String mimeType) {
        return (mimeType != null && mimeType.startsWith("image/*"))
            || isExtensionHandled(MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType));
    }

    /**
     * This method ignores case
     */
    public static boolean isItemInList(String item, String[] array) {
        return item != null && Arrays.asList(array).contains(item.toLowerCase());
    }

    public static String[] getAllGoogleDocsSupportedTypes() {
        return ArrayUtils.addAll(Constants.ALL_FILE_MIME_TYPES, Constants.ALL_GOOGLE_DOCS_TYPES);
    }

    /**
     * Returns file extension in lower case, or empty string if input is null
     */
    public static String getExtension(String filename) {
        String ext = FilenameUtils.getExtension(filename);
        if (!isNullOrEmpty(ext)) {
            return ext.toLowerCase();
        }
        return "";
    }

    /**
     * Copies a large file.
     *
     * @param inputFile The input file
     * @param output    The output stream
     * @return The number of copied bytes
     * @throws IOException IO exception
     */
    public static long copyLarge(RandomAccessFile inputFile, OutputStream output) throws IOException {
        return copyLarge(inputFile, output, new byte[4096]);
    }

    /**
     * Copies a large file.
     *
     * @param inputFile The input file
     * @param output    The output stream
     * @param buffer    A helper buffer
     * @return The number of copied bytes
     * @throws IOException IO exception
     */
    @SuppressWarnings("WeakerAccess")
    public static long copyLarge(RandomAccessFile inputFile, OutputStream output, byte[] buffer) throws IOException {
        long count = 0L;

        int n;
        for (; -1 != (n = inputFile.read(buffer)); count += (long) n) {
            output.write(buffer, 0, n);
        }

        return count;
    }

    /**
     * Gets a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     *                <p>
     *                reference http://stackoverflow.com/a/20559175
     *                <p>
     *                Note: there is also another version of this function in QuadOverImageView package (FileUtils.java)
     */
    @Nullable
    public static String getRealPathFromURI(@Nullable Context context, @Nullable Uri uri) {
        if (context == null || uri == null) {
            return null;
        }

        try {
            // DocumentProvider
            if (isKitKat() && DocumentsContract.isDocumentUri(context, uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                // ExternalStorageProvider
                if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                    // TODO handle non-primary volumes
                }
                // DownloadsProvider
                else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                    if (docId.startsWith("raw:")) {
                        return docId.replaceFirst("raw:", "");
                    }
                    final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                        split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, null, null);
            }
            // File
            else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }

            // last try
            File actualFile = new File(uri.getPath());
            if (actualFile.exists()) {
                return uri.getPath();
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return null;
        }

        return null;
    }

    /**
     * Gets the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    @SuppressWarnings("WeakerAccess")
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
            column
        };

        ContentResolver contentResolver = getContentResolver(context);
        if (contentResolver == null) {
            return null;
        }
        try {
            cursor = contentResolver.query(uri, projection, selection, selectionArgs,
                null);
            if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0 && cursor.getColumnCount() > 0) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                String filepath = cursor.getString(column_index);
                if (new File(filepath).exists()) {
                    return filepath;
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * Duplicates a file in "Download" folder
     * </p>
     * <div class="warning">
     * If "Download" has already a file with the same name, it will pick up a different name
     * </div>
     *
     * @param context The context
     * @param uri     The URI of a file
     * @param title   The title of the file
     * @return The duplicated file
     */
    @Nullable
    public static File duplicateInDownload(@Nullable Context context, @Nullable Uri uri, @Nullable String title) {
        if (context == null || uri == null || isNullOrEmpty(title)) {
            return null;
        }

        InputStream is = null;
        try {
            title = getValidFilename(title);
            File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadFolder.isDirectory()) {
                FileUtils.forceMkdir(downloadFolder);
            }
            File file = new File(downloadFolder, title);
            String tempDownloadPath = getFileNameNotInUse(file.getAbsolutePath());
            if (isNullOrEmpty(tempDownloadPath)) {
                return null;
            }
            file = new File(tempDownloadPath);

            ContentResolver contentResolver = Utils.getContentResolver(context);
            if (contentResolver == null) {
                return null;
            }
            is = contentResolver.openInputStream(uri);
            if (is != null) {
                FileUtils.copyInputStreamToFile(is, file);
            }
            return file;
        } catch (FileNotFoundException ignored) {
            CommonToast.showText(context, R.string.permission_storage_rationale);
        } catch (SecurityException ignored) {
            // file provider no longer exists
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e, "title: " + title);
        } finally {
            closeQuietly(is);
        }
        return null;
    }

    /**
     * Return a valid file name from the specified file name.
     *
     * @param filename The file name
     * @return A valid file name
     */
    public static String getValidFilename(@Nullable String filename) {
        // customize output filename
        final int MAX_FILENAME_LENGTH = 128;
        final int SHORT_SEGMENT_LENGTH = 10;

        if (isNullOrEmpty(filename)) {
            Random random = new Random();
            return "file " + Math.abs(random.nextInt()) + ".pdf";
        }

        if (filename.length() > MAX_FILENAME_LENGTH) {
            filename = filename.substring(0, MAX_FILENAME_LENGTH);
            String[] strs = filename.split(" ");
            String lastStr = strs[strs.length - 1];
            if (lastStr.length() <= SHORT_SEGMENT_LENGTH) { // if the last word is short drop it from subject
                filename = filename.substring(0, filename.length() - lastStr.length() - 1);
            }
        }

        if (!isExtensionHandled(Utils.getExtension(filename))) {
            filename += ".pdf";
        }
        return filename;
    }

    /**
     * Return a valid title from the specified URI
     *
     * @param context The context
     * @param fileUri The uri of the document
     * @return A valid title
     */
    @NonNull
    public static String getValidTitle(@NonNull Context context, @NonNull Uri fileUri) {
        final int MAX_FILENAME_LENGTH = 32;
        final int SHORT_SEGMENT_LENGTH = 8;

        String title = getUriDisplayName(context, fileUri);
        String tag = fileUri.toString();
        if (isNullOrEmpty(title) || URLUtil.isHttpUrl(tag) || URLUtil.isHttpsUrl(tag)) {
            title = fileUri.getLastPathSegment();
            if (isNullOrEmpty(title)) {
                title = "untitled";
            }
        }

        if (title.length() > MAX_FILENAME_LENGTH) {
            title = title.substring(0, MAX_FILENAME_LENGTH);
            String[] strs = title.split(" ");
            String lastStr = strs[strs.length - 1];
            if (lastStr.length() <= SHORT_SEGMENT_LENGTH) { // if the last word is short drop it from title
                title = title.substring(0, title.length() - lastStr.length() - 1);
            }
        }

        return title;
    }

    /**
     * Checks whether the specified URI has read/write permission.
     *
     * @param context The context
     * @param uri     The URI
     * @return True if the specified URI has read/write permission
     */
    public static boolean uriHasReadWritePermission(Context context, Uri uri) {
        return checkUriPermission(context, uri, "rw");
    }

    /**
     * Checks whether the specified URI has read permission.
     *
     * @param context The context
     * @param uri     The URI
     * @return True if the specified URI has read permission
     */
    public static boolean uriHasReadPermission(@Nullable Context context, Uri uri) {
        return context != null && checkUriPermission(context, uri, "r");
    }

    /**
     * Checks the permission of the specified URI
     *
     * @param context    The context
     * @param uri        The URI
     * @param permission The permission e.g. "r", "rw"
     * @return True if the URI has the specified permission
     */
    private static boolean checkUriPermission(Context context, Uri uri, String permission) {
        if (context == null || uri == null || isNullOrEmpty(permission)) {
            return false;
        }
        if (permission.equals("r") || permission.equals("rw")) {
            ParcelFileDescriptor pfd = null;
            ContentResolver contentResolver = Utils.getContentResolver(context);
            if (contentResolver == null) {
                return false;
            }
            try {
                pfd = contentResolver.openFileDescriptor(uri, permission);
                if (pfd != null) {
                    return true;
                }
            } catch (Exception ignored) {
            } finally {
                closeQuietly(pfd);
            }
        }
        return false;
    }

    /**
     * Unlocks the PDF doc write lock quietly
     *
     * @param doc The PDF doc
     */
    public static void unlockQuietly(PDFDoc doc) {
        if (doc != null) {
            try {
                doc.unlock();
            } catch (Exception ignored) {

            }
        }
    }

    /**
     * Unlocks the PDF doc read lock quietly
     *
     * @param doc The PDF doc
     */
    public static void unlockReadQuietly(PDFDoc doc) {
        if (doc != null) {
            try {
                doc.unlockRead();
            } catch (Exception ignored) {

            }
        }
    }

    /**
     * Closes the file filter proper quietly
     *
     * @param filter the SecondaryFileFilter
     */
    public static void closeQuietly(SecondaryFileFilter filter) {
        if (filter != null) {
            filter.close();
        }
    }

    /**
     * Closes the PDFDoc and file filter proper quietly
     *
     * @param doc    the PDFDoc
     * @param filter the SecondaryFileFilter
     */
    public static void closeQuietly(PDFDoc doc, SecondaryFileFilter filter) {
        try {
            if (doc != null) {
                doc.close();
            } else if (filter != null) {
                filter.close();
            }
        } catch (Exception ignored) {

        }
    }

    /**
     * Closes the PDF doc quietly
     *
     * @param doc The PDF doc
     */
    // Although only the first call has any effect, it is safe to call close multiple times on the same object.
    public static void closeQuietly(PDFDoc doc) {
        if (doc != null) {
            try {
                doc.close();
            } catch (Exception ignored) {

            }
        }
    }

    /**
     * Closes the parcel descriptor quietly
     *
     * @param pfd The parcel descriptor
     */
    // Although only the first call has any effect, it is safe to call close multiple times on the same object.
    public static void closeQuietly(ParcelFileDescriptor pfd) {
        try {
            if (pfd != null) {
                pfd.close();
            }
        } catch (Exception ignored) {

        }
    }

    /**
     * Closes the closeable quietly
     *
     * @param c The closeable
     */
    // Although only the first call has any effect, it is safe to call close multiple times on the same object.
    public static void closeQuietly(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (IOException ignore) {
        }
    }

    /**
     * Closes the FDF doc quietly
     *
     * @param doc The FDF doc
     */

    public static void closeQuietly(FDFDoc doc) {
        try {
            if (doc != null) {
                doc.close();
            }
        } catch (Exception ignored) {

        }
    }

    /**
     * Return the URI for the specified file
     *
     * @param context The context
     * @param file    The file
     * @return The URI
     */
    @Nullable
    public static Uri getUriForFile(@NonNull Context context, @NonNull File file) {
        String filepath = file.getAbsolutePath();
        String extension = FilenameUtils.getExtension(filepath);
        filepath = FilenameUtils.removeExtension(filepath);
        filepath += "." + extension.toLowerCase();

        if (!isNougat()) {
            // for Android 7+, the app crashes with a FileUriExposedException
            return Uri.fromFile(new File(filepath));
        }

        try {
            // should use file provider for Android 7+ when the file is associated with the app
            return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", new File(filepath));
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e, filepath);
        }
        return null;
    }

    /**
     * A helper to safely call {@link RecyclerView.Adapter#notifyDataSetChanged()}
     */
    public static void safeNotifyDataSetChanged(RecyclerView.Adapter adapter) {
        if (adapter != null) {
            try {
                adapter.notifyDataSetChanged();
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * A helper to safely call {@link RecyclerView.Adapter#notifyItemChanged(int)}
     */
    public static void safeNotifyItemChanged(RecyclerView.Adapter adapter, int position) {
        if (adapter != null) {
            try {
                adapter.notifyItemChanged(position);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * A helper to safely call {@link RecyclerView.Adapter#removeItemDecoration(RecyclerView.ItemDecoration)}
     */
    public static void safeRemoveItemDecoration(RecyclerView recyclerView, RecyclerView.ItemDecoration itemDecoration) {
        if (recyclerView != null && itemDecoration != null) {
            try {
                recyclerView.removeItemDecoration(itemDecoration);
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
    }

    /**
     * Frees up memory when out-of-memory happens.
     *
     * @param context The context
     */
    public static void manageOOM(@Nullable Context context) {
        manageOOM(context, null);
    }

    /**
     * Frees up memory when out-of-memory happens.
     *
     * @param pdfViewCtrl The PDFViewCtrl
     */
    public static void manageOOM(@Nullable PDFViewCtrl pdfViewCtrl) {
        Context context = pdfViewCtrl == null ? null : pdfViewCtrl.getContext();
        manageOOM(context, pdfViewCtrl);
    }

    /**
     * Frees up memory when out-of-memory happens.
     *
     * @param context     The context
     * @param pdfViewCtrl The PDFViewCtrl
     */
    public static void manageOOM(@Nullable Context context, @Nullable PDFViewCtrl pdfViewCtrl) {
        String oldMemStr = "", newMemStr = "";
        String memoryClass = "";
        ActivityManager.MemoryInfo memInfo = null;
        ActivityManager activityManager = null;

        // get memory info before cleanup
        if (context != null) {
            activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                memInfo = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(memInfo);
                oldMemStr = "available memory size before cleanup: " + memInfo.availMem / (1024f * 1024f) + "MB, ";
            }
        }

        // cleanup
        ImageMemoryCache.getInstance().clearAll();
        PathPool.getInstance().clear();
        if (pdfViewCtrl != null) {
            // in future replace it with purgeMemoryDueToOOM. for now we want to get some statistics
            pdfViewCtrl.purgeMemory();
        }

        // get memory info after cleanup
        if (activityManager != null) {
            activityManager.getMemoryInfo(memInfo);
            newMemStr = "available memory size after cleanup: " + memInfo.availMem / (1024f * 1024f) + "MB, ";
            memoryClass = ", memory class: " + activityManager.getMemoryClass();
        }

        if (context != null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("OOM - "
                + oldMemStr + newMemStr
                + "native heap allocated size: " + Debug.getNativeHeapAllocatedSize() / (1024f * 1024f) + "MB"
                + memoryClass));
        }
    }

    /**
     * A helper to get {@link Context#getContentResolver()}.
     */
    @Nullable
    public static ContentResolver getContentResolver(@Nullable Context context) {
        return context == null ? null : context.getContentResolver();
    }

    /**
     * A helper to get {@link Context#getExternalFilesDir(String)}.
     */
    @Nullable
    public static File getExternalFilesDir(@Nullable Context context, String type) {
        return context == null ? null : context.getExternalFilesDir(type);
    }

    /**
     * A helper to get {@link Context#getExternalFilesDirs(String)}.
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Nullable
    public static File[] getExternalFilesDirs(@Nullable Context context, String type) {
        return context == null ? null : context.getExternalFilesDirs(type);
    }

    /**
     * A helper to get {@link Context#getResources()}.
     */
    @Nullable
    public static Resources getResources(@Nullable Context context) {
        return context == null ? null : context.getResources();
    }

    /**
     * A helper to get drawable identifier.
     */
    public static int getResourceDrawable(Context context, String name) {
        int res = 0;
        if (context != null) {
            res = context.getResources().getIdentifier(name, "drawable", context.getApplicationInfo().packageName);
        }
        return res;
    }

    /**
     * A helper to get color identifier.
     */
    public static int getResourceColor(Context context, String name) {
        int res = 0;
        if (context != null) {
            res = context.getResources().getIdentifier(name, "color", context.getApplicationInfo().packageName);
        }
        return res;
    }

    /**
     * A helper to get raw identifier.
     */
    public static int getResourceRaw(Context context, String name) {
        int res = 0;
        if (context != null) {
            res = context.getResources().getIdentifier(name, "raw", context.getApplicationInfo().packageName);
        }
        return res;
    }

    /**
     * Adjusts alpha color.
     *
     * @param color  The color
     * @param factor The factor for adjustment
     * @return The adjusted color
     */
    public static int adjustAlphaColor(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    /**
     * Given a portfolio PDF document (ie, with embedded files), returns a list
     * of the embedded files.
     *
     * @param file     the PDF document
     * @param password the password to open this document if secured
     * @return a list of file names of the embedded files
     */
    public static ArrayList<String> getFileNamesFromPortfolio(File file, String password) {
        ArrayList<String> entryNames = new ArrayList<>();

        PDFDoc doc = null;
        try {
            doc = new PDFDoc(file.getAbsolutePath());
            if (doc.initStdSecurityHandler(password)) {
                entryNames = getFileNamesFromPortfolio(doc);
            }
        } catch (Exception e) {
            entryNames = new ArrayList<>();
        } finally {
            if (doc != null) {
                try {
                    doc.close();
                } catch (Exception ignored) {

                }
            }
        }

        return entryNames;
    }

    public static ArrayList<String> getFileNamesFromPortfolio(Context context, Uri fileUri, String password) {
        ArrayList<String> entryNames = new ArrayList<>();

        PDFDoc doc = null;
        SecondaryFileFilter filter = null;
        try {
            filter = new SecondaryFileFilter(context, fileUri);
            doc = new PDFDoc(filter);
            if (doc.initStdSecurityHandler(password)) {
                entryNames = getFileNamesFromPortfolio(doc);
            }
        } catch (Exception e) {
            entryNames = new ArrayList<>();
        } finally {
            closeQuietly(doc, filter);
        }

        return entryNames;
    }

    public static ArrayList<String> getFileNamesFromPortfolio(PDFDoc doc) {
        // Note: this method assumes PDFDoc has already passed SecurityHandler check
        if (doc == null) {
            return new ArrayList<>();
        }
        ArrayList<String> entryNames = new ArrayList<>();
        boolean shouldUnlockRead = false;
        try {
            doc.lockRead();
            shouldUnlockRead = true;
            com.pdftron.sdf.NameTree files = com.pdftron.sdf.NameTree.find(doc.getSDFDoc(), "EmbeddedFiles");
            if (files.isValid()) {
                NameTreeIterator i = files.getIterator();
                while (i.hasNext()) {
                    String entryName = i.key().getAsPDFText();
                    com.pdftron.pdf.FileSpec file_spec = new com.pdftron.pdf.FileSpec(i.value());
                    if (file_spec.isValid()) {
                        entryNames.add(file_spec.getFilePath());
                    } else {
                        entryNames.add(entryName);
                    }
                    i.next();
                }
            }
        } catch (Exception e) {
            entryNames = new ArrayList<>();
        } finally {
            if (shouldUnlockRead) {
                unlockReadQuietly(doc);
            }
        }
        return entryNames;
    }

    public static boolean hasFileAttachments(PDFDoc doc) {
        boolean result = false;
        boolean shouldUnlockRead = false;
        try {
            doc.lockRead();
            shouldUnlockRead = true;
            NameTree file_map = NameTree.find(doc, "EmbeddedFiles");
            if (!file_map.isValid()) return false;
            result = true;
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                unlockReadQuietly(doc);
            }
        }
        return result;
    }

    public static HashMap<String, String> parseDefaults(Context context, int xmlRes) {
        HashMap<String, String> result = new HashMap<>();
        XmlResourceParser parser = context.getResources().getXml(xmlRes);
        if (parser != null) {
            try {
                int eventType = parser.getEventType();
                String lastKey = null;
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        String key = parser.getName();
                        if ((key.equals("key") || key.equals("value")) && parser.next() == XmlPullParser.TEXT) {
                            String value = parser.getText();
                            if (key.equals("key")) {
                                lastKey = value;
                            } else {
                                result.put(lastKey, value);
                            }
                            parser.next();
                        }
                    }
                    eventType = parser.next();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                parser.close();
            }
        }
        return result;
    }

    /**
     * Returns the accent color defined in android attribute.
     *
     * @param context The context
     * @return The accent color defined in android attribute
     */
    @ColorInt
    public static int getAccentColor(@NonNull Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, value, true);
        return value.data;
    }

    /**
     * Returns the primary color defined in android attribute.
     *
     * @param context The context
     * @return The primary color defined in android attribute
     */
    @ColorInt
    public static int getPrimaryColor(@NonNull Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimary, value, true);
        return value.data;
    }

    /**
     * Returns the primary dark color defined in android attribute.
     *
     * @param context The context
     * @return The primary dark color defined in android attribute
     */
    @ColorInt
    public static int getPrimaryDarkColor(@NonNull Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimaryDark, value, true);
        return value.data;
    }

    /**
     * Returns the foreground color defined in android attribute.
     *
     * @param context The context
     * @return The foreground color defined in android attribute
     */
    @ColorInt
    public static int getForegroundColor(@NonNull Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorForeground, value, true);
        return value.data;
    }

    /**
     * Returns the background color defined in android attribute.
     *
     * @param context The context
     * @return The background color defined in android attribute
     */
    @ColorInt
    public static int getBackgroundColor(@NonNull Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorBackground, value, true);
        return value.data;
    }

    /**
     * Returns whether the current device UI mode is night.
     *
     * @param context The context
     * @return True if the current device UI mode is night
     */
    public static boolean isDeviceNightMode(@NonNull Context context) {
        int currentNightMode = context.getResources().getConfiguration().uiMode
            & Configuration.UI_MODE_NIGHT_MASK;
//        switch (currentNightMode) {
//            case Configuration.UI_MODE_NIGHT_NO:
//                // Night mode is not active, we're in day time
//            case Configuration.UI_MODE_NIGHT_YES:
//                // Night mode is active, we're at night!
//            case Configuration.UI_MODE_NIGHT_UNDEFINED:
//                // We don't know what mode we're in, assume notnight
//        }
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Apply day night mode.
     *
     * @return true if the night mode was applied, false otherwise
     */
    public static boolean applyDayNight(@NonNull AppCompatActivity activity) {
        boolean isDarkMode = PdfViewCtrlSettingsManager.isDarkMode(activity);
        if (isDeviceNightMode(activity) != isDarkMode) {
            activity.getDelegate().setLocalNightMode(isDarkMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);
            activity.getDelegate().applyDayNight();
            return true;
        }
        return false;
    }

    /**
     * Checks whether the given color is considered as dark.
     *
     * @param color The color
     * @return True if the given color is considered as dark
     */
    public static boolean isColorDark(int color) {
        return isColorDark(color, 0.5f);
    }

    /**
     * Checks whether the given color is considered as dark.
     *
     * @param color     The color
     * @param threshold threshold value to determine if the color is dark
     * @return True if the given color is considered as dark
     */
    public static boolean isColorDark(int color, float threshold) {
        double bgDarkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return bgDarkness > threshold;
    }

    /**
     * Checks whether two colors are close to each other by CIE76 computation
     *
     * @param color1    The first color to compare
     * @param color2    The second color to compare
     * @param threshold If the distance of two colors is lower than the threshold, then they are considered as closed color
     * @return true if two colors are close to each other, false otherwise
     */
    public static boolean isTwoColorSimilar(int color1, int color2, float threshold) {
        double[] lab1 = new double[3];
        double[] lab2 = new double[3];
        ColorUtils.colorToLAB(color1, lab1);
        ColorUtils.colorToLAB(color2, lab2);
        return ColorUtils.distanceEuclidean(lab1, lab2) < threshold;
    }

    /**
     * Gets font file name
     *
     * @param fontFilePath font file path
     * @return font file name
     */
    public static String getFontFileName(String fontFilePath) {
        // get font code from the file path
        String fileName = fontFilePath.substring(fontFilePath.lastIndexOf("/") + 1);

        final int lastPeriodPos = fileName.lastIndexOf(".");
        if (lastPeriodPos > 0) {
            return fileName.substring(0, lastPeriodPos);
        } else {
            return fileName;
        }
    }

    /**
     * Gets color value in hex format (#0xFFFFFF)
     *
     * @param color color
     * @return hex format string
     */
    public static String getColorHexString(@ColorInt int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    /**
     * convert color to post processed color in pdf
     *
     * @param pdfViewCtrl The PDFViewCtrl
     * @param color       The color to be post processed
     * @return The post processed color
     */
    public static int getPostProcessedColor(@NonNull PDFViewCtrl pdfViewCtrl, int color) {
        ColorPt colorPt = Utils.color2ColorPt(color);
        ColorPt postColorPt = pdfViewCtrl.getPostProcessedColor(colorPt);
        return Utils.colorPt2color(postColorPt);
    }

    /**
     * Replaces a color with another when the input bitmap is a 9-path.
     *
     * @param inputBmp  The input 9-patch bitmap
     * @param fromColor The color to be replaced from
     * @param toColor   The color to be replaced to
     * @return The output bitmap after replacing the color
     */
    public static Bitmap replace9PatchColor(Bitmap inputBmp, int fromColor, int toColor) {
        if (inputBmp == null) {
            return null;
        }

        // exclude extra 1-pixel border
        int width = inputBmp.getWidth() - 2;
        int height = inputBmp.getHeight() - 2;
        Bitmap insideBmp = Bitmap.createBitmap(inputBmp, 1, 1, width, height);
        int[] insidePixels = new int[width * height];
        insideBmp.getPixels(insidePixels, 0, width, 0, 0, width, height);

        for (int i = 0, cnt = insidePixels.length; i < cnt; ++i) {
            insidePixels[i] = (insidePixels[i] == fromColor) ? toColor : insidePixels[i];
        }

        Bitmap outputBitmap = Bitmap.createBitmap(width, height, inputBmp.getConfig());
        outputBitmap.setPixels(insidePixels, 0, width, 0, 0, width, height);
        return outputBitmap;
    }

    /**
     * Creates a state image drawable (enable/disable) from a resource id with specified color and
     * the transparency factor when disabled.
     *
     * @param context    The context
     * @param resourceId The input resource id
     * @param color      The tint color
     * @return The state image drawable
     */
    public static StateListDrawable createImageDrawableSelector(@NonNull Context context, int resourceId, int color) {
        return createImageDrawableSelector(context, resourceId, color, 98);
    }

    /**
     * Creates a state image drawable (enable/disable) from a resource id with specified color and
     * the transparency factor when disabled.
     *
     * @param context        The context
     * @param resourceId     The input resource id
     * @param color          The tint color
     * @param disableOpacity The transparency when the drawable is disabled
     * @return The state image drawable
     */
    public static StateListDrawable createImageDrawableSelector(@NonNull Context context, int resourceId, int color, int disableOpacity) {
        // It seems that when you add a drawable to a StateList, it loses all its filters on
        // pre-lollipop devices: https://stackoverflow.com/q/7979440/8033906
        // Therefore, rather than apply filters such as setColorFilter and setAlpha,
        // we have to change bitmap colors directly.

        Resources resources = context.getResources();
        Drawable drawable = Utils.getDrawable(context, resourceId);
        if (drawable == null) {
            return new StateListDrawableBuilder()
                .build();
        }
        int width = drawable.getMinimumWidth();
        int height = drawable.getMinimumHeight();
        Bitmap icon = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(icon);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        // when enabled
        Bitmap enableIcon = tint(icon, color);
        BitmapDrawable enableDrawable = new BitmapDrawable(resources, enableIcon);

        // when disabled
        Bitmap disableIcon = adjustAlpha(icon, disableOpacity);
        BitmapDrawable disableDrawable = new BitmapDrawable(resources, disableIcon);

        return new StateListDrawableBuilder()
            .setDisabledDrawable(disableDrawable)
            .setNormalDrawable(enableDrawable)
            .build();
    }

    /**
     * Tints a bitmap to a new color.
     *
     * @param bitmap The input bitmap
     * @param color  The tinting color
     * @return The output bitmap after tinting the bitmap
     */
    public static Bitmap tint(Bitmap bitmap, int color) {
        if (bitmap == null) {
            return null;
        }

        Paint paint = new Paint();
        ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
        paint.setColorFilter(filter);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap outputBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());
        Canvas canvas = new Canvas(outputBitmap);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return outputBitmap;
    }

    /**
     * Adjusts alpha channel of a bitmap
     *
     * @param bitmap The input bitmap
     * @param alpha  The alpha
     * @return The output bitmap after adjusting the alpha channel of the bitmap
     */
    public static Bitmap adjustAlpha(Bitmap bitmap, int alpha) {
        if (bitmap == null) {
            return null;
        }

        Paint paint = new Paint();
        paint.setAlpha(alpha);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap outputBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());
        Canvas canvas = new Canvas(outputBitmap);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return outputBitmap;
    }

    public static int getThemeAttrColor(@NonNull Context context, int attr) {
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attr});
        int color = typedArray.getColor(0, 0);
        typedArray.recycle();
        return color;
    }

    public static Drawable getDrawableWithTint(@NonNull Context context, @DrawableRes int drawableId, int tintColor) {
        Drawable drawable = context.getResources().getDrawable(drawableId);
        drawable.mutate();
        drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
        return drawable;
    }
    
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public static boolean isUriSeekable(Context context, Uri uri) {
        ParcelFileDescriptor pfd = null;
        ContentResolver contentResolver = Utils.getContentResolver(context);
        boolean seekable = false;
        if (contentResolver != null) {
            try {
                pfd = contentResolver.openFileDescriptor(uri, "rw");
                seekable = true;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Utils.closeQuietly(pfd);
            }
        }
        return seekable;
    }

    public static String getDateTimeFormatFromField(@Nullable String fieldValue, boolean isDate) {
        if (fieldValue == null) {
            return "";
        }
        String s = fieldValue;

        boolean handled = false;
        if (s.contains("AFTime_Keystroke")) {
            // this is a special format
            // let's parse it
            if (s.contains("AFTime_Keystroke(0)")) {
                s = "HH:mm";
                handled = true;
            } else if (s.contains("AFTime_Keystroke(1)")) {
                s = "h:mm a";
                handled = true;
            } else if (s.contains("AFTime_Keystroke(2)")) {
                s = "HH:mm:ss";
                handled = true;
            } else if (s.contains("AFTime_Keystroke(3)")) {
                s = "h:mm:ss a";
                handled = true;
            }
        }
        if (!handled) {
            s = s.substring(s.indexOf("\"") + 1);
            s = s.substring(0, s.indexOf("\""));
        }
        if (isDate) {
            if (s.contains("mm") && !s.contains("mmm")) {
                s = s.replace("mm", "MM");
            }
            s = s.replace("mmm", "MMM");
            s = s.replace("YYYY", "yyyy");
            s = s.replace("DD", "dd");
            s = s.replace(":MM", ":mm");
            s = s.replace("tt", "a");
        } else {
            s = s.replace("MM", "mm");
        }
        return s;
    }

    /**
     * Handles when trying to create PDF from an image fails. It remove any
     * cached memory if any and show a toast
     *
     * @param context     The context
     * @param imageIntent The internal image intent map obtained from {@link ViewerUtils#readImageIntent(Intent, Context, Uri)}
     */
    public static void handlePdfFromImageFailed(
        @NonNull Context context,
        @NonNull Map imageIntent) {

        if (ViewerUtils.isImageFromCamera(imageIntent)) {
            String imageFilePath = ViewerUtils.getImageFilePath(imageIntent);
            if (imageFilePath != null) {
                FileUtils.deleteQuietly(new File(imageFilePath));
            }
        }

        CommonToast.showText(context, R.string.dialog_add_photo_document_filename_error_message, Toast.LENGTH_SHORT);
    }

    /**
     * Adds the text copy tag to the annotation's SDF Obj which is used to determine
     * if the note is copied from annotated text
     *
     * @param annot The annotation
     */
    public static void setTextCopy(
        @NonNull Annot annot) {

        try {
            annot.getSDFObj().putString("textcopy", "");
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

    }

    /**
     * Removes the text copy tag from the annotation's SDF Obj. See {{@link #setTextCopy(Annot)}}.
     *
     * @param annot The annotation
     */
    public static void removeTextCopy(
        @NonNull Annot annot) {

        if (isTextCopy(annot)) {
            try {
                annot.getSDFObj().erase("textcopy");
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }

    }

    /**
     * Determines if the note is copied from annotated text.
     *
     * @param annot The annotation
     * @return True if the note is copied from annotated text; False otherwise
     */
    public static boolean isTextCopy(
        @NonNull Annot annot) {

        try {
            return annot.getSDFObj().findObj("textcopy") != null;
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        return false;
    }

    /**
     * Gets the free disk storage size
     */
    public static long getFreeDiskStorage() {
        try {
            StatFs external = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
            if (Utils.isJellyBeanMR2()) {
                return external.getFreeBytes();
            } else {
                return external.getAvailableBlocks() * external.getBlockSize();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean hasEnoughStorageToSave(long fileSize) {
        if (!Utils.isKitKat()) {
            // for very old devices, this system API does not seem to be
            // getting correct info, skip this check if on old devices.
            return true;
        }
        long MEGABYTE = 1024L * 1024L;
        long freeStorage = getFreeDiskStorage();
        return freeStorage > (10 * MEGABYTE);
    }

    /**
     * Returns the first bookmark in a certain document.
     *
     * @param pdfDoc The PDF Doc
     * @return The first bookmark in the given document; null if the document
     * doesn't have a valid bookmark
     */
    public static Bookmark getFirstBookmark(PDFDoc pdfDoc) {
        boolean shouldUnlockRead = false;
        try {
            pdfDoc.lockRead();
            shouldUnlockRead = true;
            Bookmark firstBookmark = pdfDoc.getFirstBookmark();
            if (firstBookmark != null && firstBookmark.isValid()) {
                return firstBookmark;
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                Utils.unlockReadQuietly(pdfDoc);
            }
        }
        return null;
    }

    /**
     * Helper to get a drawable from drawable resource.
     *
     * @param context     The context
     * @param drawableRes The drawable resource
     * @return Drawable
     */
    @Nullable
    public static Drawable getDrawable(
        @Nullable Context context,
        @DrawableRes int drawableRes
    ) {

        if (context == null) {
            return null;
        }
        // workaround for getting bad drawable for API 21:
        return AppCompatResources.getDrawable(context, drawableRes);

    }

    /**
     * Gets whether a tool mode is one of the handler tool modes.
     *
     * @param toolMode the tool mode
     * @return whether the tool mode is one of the handler tool modes
     */
    public static boolean isAnnotationHandlerToolMode(@NonNull ToolManager.ToolMode toolMode) {
        return toolMode == ToolManager.ToolMode.ANNOT_EDIT || toolMode == ToolManager.ToolMode.ANNOT_EDIT_LINE
            || toolMode == ToolManager.ToolMode.ANNOT_EDIT_ADVANCED_SHAPE || toolMode == ToolManager.ToolMode.ANNOT_EDIT_TEXT_MARKUP
            || toolMode == ToolManager.ToolMode.ANNOT_EDIT_RECT_GROUP
            || toolMode == ToolManager.ToolMode.LINK_ACTION
            || toolMode == ToolManager.ToolMode.FORM_FILL
            || toolMode == ToolManager.ToolMode.RICH_MEDIA;
    }

    public static final double INTERSECTION_EPSILON = 1.0e-30;

    public static com.pdftron.pdf.Point calcIntersection(double ax, double ay, double bx, double by,
                                                         double cx, double cy, double dx, double dy) {
        double num = (ay - cy) * (dx - cx) - (ax - cx) * (dy - cy);
        double den = (bx - ax) * (dy - cy) - (by - ay) * (dx - cx);
        if (Math.abs(den) < INTERSECTION_EPSILON) return null;
        double r = num / den;
        double x = ax + r * (bx - ax);
        double y = ay + r * (by - ay);
        return new com.pdftron.pdf.Point(x, y);
    }

    public static boolean isSamePoint(double ax, double ay, double bx, double by) {
        double x = Math.abs(ax - bx);
        double y = Math.abs(ay - by);
        if (x < 0.1 && y < 0.1) {
            return true;
        }
        return false;
    }

    public static final double vertex_dist_epsilon = 1e-8;

    public static double calcLinePointDistance(double x1, double y1,
                                               double x2, double y2,
                                               double x, double y) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d < vertex_dist_epsilon) {
            return calcDistance(x1, y1, x, y);
        }
        return ((x - x2) * dy - (y - y2) * dx) / d;
    }

    public static double calcDistance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static int findMinIndex(double[] numbers) {
        if (numbers == null || numbers.length == 0) return -1;
        double minVal = numbers[0];
        int minIdx = 0;
        for (int i = 1; i < numbers.length; i++) {
            if (numbers[i] < minVal) {
                minVal = numbers[i];
                minIdx = i;
            }
        }
        return minIdx;
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    public static boolean isImageCopied(@Nullable Context context) {
        if (context == null) {
            return false;
        }
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ContentResolver cr = context.getContentResolver();
        if (clipboard == null || cr == null) {
            return false;
        }
        ClipData clip = clipboard.getPrimaryClip();

        if (clip != null) {
            ClipData.Item item = clip.getItemAt(0);
            Uri pasteUri = item.getUri();
            if (Utils.isImageFile(cr, pasteUri)) {
                return true;
            }
        }
        return false;
    }
}
