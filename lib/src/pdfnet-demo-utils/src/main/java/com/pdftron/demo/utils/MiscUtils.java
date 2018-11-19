package com.pdftron.demo.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Debug;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.MimeTypeMap;

import com.pdftron.demo.R;
import com.pdftron.demo.dialog.PortfolioDialogFragment;
import com.pdftron.demo.navigation.adapter.BaseFileAdapter;
import com.pdftron.demo.navigation.callbacks.JumpNavigationCallbacks;
import com.pdftron.filters.FilterReader;
import com.pdftron.filters.FilterWriter;
import com.pdftron.filters.SecondaryFileFilter;
import com.pdftron.pdf.FileSpec;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.CustomAsyncTask;
import com.pdftron.pdf.utils.ImageMemoryCache;
import com.pdftron.pdf.utils.PathPool;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.NameTree;
import com.pdftron.sdf.NameTreeIterator;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MiscUtils {
    // Cancellable copy operation
    public static int copy(InputStream input, OutputStream output, CustomAsyncTask task) throws IOException {
        long count = 0L;
        byte[] buffer = new byte[4096];

        int n1;
        for (; -1 != (n1 = input.read(buffer)) && !task.isCancelled(); count += (long) n1) {
            output.write(buffer, 0, n1);
        }

        return (count > 2147483647L) ? -1 : (int) count;
    }

    public static void manageOOM(@Nullable Context context) {
        manageOOM(context, null);
    }

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
        ThumbnailPathCacheManager.getInstance().cleanupResources(context);

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

    public static void handleLowMemory(@Nullable Context context) {
        handleLowMemory(context, null);
    }

    public static void handleLowMemory(@Nullable Context context, @Nullable BaseFileAdapter adapter) {
        ImageMemoryCache.getInstance().clearAll();
        PathPool.getInstance().clear();
        if (context != null) {
            ThumbnailPathCacheManager.getInstance().cleanupResources(context);
        }
        if (adapter != null) {
            adapter.cancelAllThumbRequests();
            adapter.cleanupResources();
        }
    }

    public static void throwIfOnMainThread() throws IllegalStateException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Must not be invoked from the main thread.");
        }
    }

    public static boolean showSDCardActionErrorDialog(Context context, final JumpNavigationCallbacks jumpNavigationCallbacks, String action) {
        if (Utils.isKitKatOnly()) {
            String message = String.format(context.getString(R.string.dialog_external_file_readonly_action_kitkat),
                context.getString(R.string.dialog_external_file_readonly_action_kitkat_more_info));
            Utils.showAlertDialogWithLink(context, message, "");
            return true;
        } else if (Utils.isLollipop()) {
            String message = String.format(context.getString(R.string.dialog_external_file_readonly_action),
                context.getString(R.string.local_folders), action, context.getString(R.string.app_name));

            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setMessage(Html.fromHtml(message))
                .setCancelable(true)
                .setPositiveButton(context.getString(R.string.dialog_external_file_readonly_action_positive_btn), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // go to external tab
                        if (null != jumpNavigationCallbacks) {
                            jumpNavigationCallbacks.gotoExternalTab();
                        }
                    }
                });

            final AlertDialog d = builder.create();
            d.show();
            return true;
        } else {
            return false;
        }
    }

    static public void removeFiles(ArrayList<FileInfo> filesToDelete) {
        if (filesToDelete != null) {
            for (int i = 0; i < filesToDelete.size(); i++) {
                final FileInfo fileInfo = filesToDelete.get(i);
                if (fileInfo.exists()) {
                    new AsyncTask<Void, Void, Boolean>() {
                        @Override
                        protected Boolean doInBackground(Void... params) {
                            try {
                                return fileInfo.getFile().delete();
                            } catch (Exception ignored) {

                            }
                            return false;
                        }
                    };
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isExternalFileUri(@Nullable Context context, Uri uri) {
        return context != null && Utils.isKitKat() && DocumentsContract.isDocumentUri(context, uri);
    }

    public static boolean isIntentActionMain(Intent intent) {
        return Intent.ACTION_MAIN.equals(intent.getAction());
    }

    /**
     * Given an Intent, try to parse it and get a File for the PDF document.
     * This will only work if the Intent action is ACTION_VIEW or ACTION_EDIT,
     * and the Intent data is a valid PDF file scheme.
     *
     * @param intent the Intent that holds the data
     * @return a File for the PDF document
     */
    public static File parseIntentGetPdfFile(Context context, Intent intent) {
        File pdfFile = null;

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Uri uri = intent.getData();

            if (uri != null) {
                if ((Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) &&
                    (uri.getLastPathSegment().toLowerCase().endsWith(".pdf")) && new File(uri.getPath()).exists()) {
                    pdfFile = new File(uri.getPath());
                } else if ((Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) &&
                    intent.getType() != null && intent.getType().equals("application/pdf")) {
                    // deal with files with extension .bin downloaded from browser
                    String[] projection = {"_data"};
                    Cursor cursor = null;
                    ContentResolver contentResolver = Utils.getContentResolver(context);
                    if (contentResolver == null) {
                        return null;
                    }
                    try {
                        cursor = contentResolver.query(uri, projection, null, null, null);
                        if (cursor != null && cursor.moveToFirst() && cursor.getColumnCount() > 0 && cursor.getCount() > 0) {
                            int index = cursor.getColumnIndex(projection[0]);
                            if (index != -1) {
                                String contentFilePath = cursor.getString(index);
                                if (contentFilePath != null && contentFilePath.toLowerCase().endsWith(".bin")) {
                                    File contentFile = new File(contentFilePath);
                                    if (contentFile.exists() && contentFile.isFile()) {
                                        pdfFile = contentFile;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (cursor != null) { // otherwise couldn't find projection
                            AnalyticsHandlerAdapter.getInstance().sendException(e);
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                } else if (intent.getExtras() != null) {
                    if ((Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) &&
                        (uri.toString().startsWith("file://") && uri.getLastPathSegment().toLowerCase().endsWith(".bin"))) {
                        // deal with files with extension .bin downloaded from browser
                        pdfFile = new File(uri.getPath());
                        if (!pdfFile.exists()) {
                            pdfFile = null;
                        }
                    }
                }
            }
        }

        return pdfFile;
    }

    public static String extractFileFromPortfolio(int type, Context context, PDFDoc portfolioDoc, String destFolderPath, String fileName) {
        // Note: this method assumes PDFDoc has already passed SecurityHandler check
        if (portfolioDoc == null) {
            return "";
        }
        String retValue = "";
        SecondaryFileFilter filter = null;
        try {
            NameTree files = NameTree.find(portfolioDoc.getSDFDoc(), "EmbeddedFiles");
            if (files.isValid()) {
                // Traverse the list of embedded files.
                NameTreeIterator iter = files.getIterator();
                while (iter.hasNext()) {
                    String entryName = iter.key().getAsPDFText();
                    com.pdftron.pdf.FileSpec file_spec = new com.pdftron.pdf.FileSpec(iter.value());
                    if (file_spec.isValid()) {
                        entryName = file_spec.getFilePath();
                    }
                    if (entryName.equalsIgnoreCase(fileName)) {
                        if (type == PortfolioDialogFragment.FILE_TYPE_FILE_URI) {
                            String newFileName;
                            ExternalFileInfo newFileInfo = null;
                            int i;
                            for (i = 0; i < FileManager.MAX_NUM_DUPLICATED_FILES; i++) {
                                Uri parentUri = Uri.parse(destFolderPath);
                                ExternalFileInfo parentUriInfo = Utils.buildExternalFile(context, parentUri);
                                if (parentUriInfo == null) {
                                    return "";
                                }

                                if (i == 0) {
                                    newFileName = fileName;
                                } else {
                                    String extension = FilenameUtils.getExtension(fileName);
                                    newFileName = FilenameUtils.removeExtension(fileName) + " (" + String.valueOf(i) + ")." + extension;
                                }

                                if (parentUriInfo.findFile(newFileName) == null) {
                                    Uri tempUri = ExternalFileInfo.appendPathComponent(parentUri, newFileName);
                                    String extension = MimeTypeMap.getFileExtensionFromUrl(tempUri.toString());
                                    MimeTypeMap mime = MimeTypeMap.getSingleton();
                                    String newFileType = mime.getMimeTypeFromExtension(extension);
                                    newFileInfo = parentUriInfo.createFile(newFileType, newFileName);
                                    break;
                                }
                            }
                            if (newFileInfo != null) {
                                retValue = newFileInfo.getAbsolutePath();

                                com.pdftron.filters.Filter stm = file_spec.getFileData();
                                if (stm != null) {
                                    filter = new SecondaryFileFilter(context, newFileInfo.getUri(), SecondaryFileFilter.WRITE_MODE);
                                    FilterWriter filterWriter = new FilterWriter(filter);
                                    FilterReader filterReader = new FilterReader(stm);
                                    filterWriter.writeFilter(filterReader);
                                    filterWriter.flushAll();
                                    break;
                                }
                            }
                        } else {
                            String fullFileName = (new File(destFolderPath, fileName)).getAbsolutePath();
                            String newFileName = fullFileName;
                            int i;
                            for (i = 1; i < FileManager.MAX_NUM_DUPLICATED_FILES; i++) {
                                File file = new File(newFileName);
                                if (file.exists()) {
                                    // get file extension
                                    // it is possible that the file attachment is not a PDF file
                                    String extension = FilenameUtils.getExtension(fullFileName);
                                    newFileName = FilenameUtils.removeExtension(fullFileName)
                                        + " (" + String.valueOf(i) + ")." + extension;
                                } else {
                                    break;
                                }
                            }
                            if (i < FileManager.MAX_NUM_DUPLICATED_FILES) {
                                retValue = newFileName;
                            } else {
                                break;
                            }

                            com.pdftron.filters.Filter stm = file_spec.getFileData();
                            if (stm != null) {
                                stm.writeToFile(newFileName, false);
                                break;
                            }
                        }
                    }
                    iter.next();
                }
            }
        } catch (Exception e) {
            retValue = "";
        } finally {
            Utils.closeQuietly(filter);
        }

        return retValue;
    }

    public static String extractFileFromPortfolio(File portfolioFile, String fileName) {
        String retValue;
        PDFDoc doc = null;
        boolean shouldUnlock = false;
        try {
            doc = new PDFDoc(portfolioFile.getAbsolutePath());
            doc.lock();
            shouldUnlock = true;
            doc.initSecurityHandler();
            return extractFileFromPortfolio(PortfolioDialogFragment.FILE_TYPE_FILE, null, doc, portfolioFile.getParent(), fileName);
        } catch (Exception e) {
            retValue = "";
        } finally {
            if (shouldUnlock) {
                Utils.unlockQuietly(doc);
            }
            Utils.closeQuietly(doc);
        }
        return retValue;
    }

    public static String extractFileFromPortfolio(Context context, Uri portfolioFileUri, String fileName) {
        String retValue;
        PDFDoc doc = null;
        SecondaryFileFilter filter = null;
        try {
            filter = new SecondaryFileFilter(context, portfolioFileUri);
            doc = new PDFDoc(filter);
            doc.initSecurityHandler();
            Uri parentUri = getUriParent(portfolioFileUri);
            return extractFileFromPortfolio(PortfolioDialogFragment.FILE_TYPE_FILE_URI, context, doc, parentUri.toString(), fileName);
        } catch (Exception e) {
            retValue = "";
        } finally {
            Utils.closeQuietly(doc, filter);
        }
        return retValue;
    }

    public static ExternalFileInfo extractFileFromFileSpec(Context context, ExternalFileInfo extractFolder, FileSpec fileSpec, String fileName) throws Exception {
        String newFileName;
        ExternalFileInfo newFileInfo = null;
        int i;
        SecondaryFileFilter filter = null;
        try {
            for (i = 0; i < FileManager.MAX_NUM_DUPLICATED_FILES; i++) {
                if (i == 0) {
                    newFileName = fileName;
                } else {
                    String extension = Utils.getExtension(fileName);
                    newFileName = FilenameUtils.removeExtension(fileName) + " (" + String.valueOf(i) + ")." + extension;
                }

                if (extractFolder != null && extractFolder.findFile(newFileName) == null) {
                    Uri tempUri = ExternalFileInfo.appendPathComponent(extractFolder.getUri(), newFileName);
                    String extension = MimeTypeMap.getFileExtensionFromUrl(tempUri.toString());
                    MimeTypeMap mime = MimeTypeMap.getSingleton();
                    String newFileType = mime.getMimeTypeFromExtension(extension);
                    newFileInfo = extractFolder.createFile(newFileType, newFileName);
                    break;
                }
            }
            if (newFileInfo != null) {
                com.pdftron.filters.Filter stm = fileSpec.getFileData();
                if (stm != null) {
                    filter = new SecondaryFileFilter(context, newFileInfo.getUri(), SecondaryFileFilter.WRITE_MODE);
                    FilterWriter filterWriter = new FilterWriter(filter);
                    FilterReader filterReader = new FilterReader(stm);
                    filterWriter.writeFilter(filterReader);
                    filterWriter.flushAll();
                }
            }
        } finally {
            Utils.closeQuietly(filter);
        }
        return newFileInfo;
    }

    public static boolean isPDFFile(String filename) {
        return FilenameUtils.wildcardMatch(filename, "*.pdf", IOCase.INSENSITIVE);
    }

    // Get the specified uri's path directory, built from its path component.
    // Suitable for use with the DocumentsContract API.
    public static Uri getUriParent(Uri uri) {
        if (uri != null) {
            String path = uri.getPath();
            if (!Utils.isNullOrEmpty(path)) {
                int lastSeparator = path.lastIndexOf(File.separatorChar);
                int lastColon = path.lastIndexOf(':');

                if (lastSeparator - 1 >= 0 && lastSeparator > lastColon && lastSeparator + 1 < path.length()) {
                    // There is content before and after this separator, and it appears after the last colon
                    // Truncate at last separator (exclusive)
                    path = path.substring(0, lastSeparator);
                } else if (lastColon - 1 >= 0 && lastColon + 1 < path.length()) {
                    // There is content before and after this colon-separator
                    // Truncate at last colon (inclusive)
                    path = path.substring(0, lastColon + 1);
                }
                // Build uri with truncated path
                return uri.buildUpon().path(path).build();
            }
        }
        return null;
    }

    public static void showPermissionResultSnackbar(Activity activity, View layout, boolean hasPermission, int requestCode) {
        if (activity == null || layout == null) {
            return;
        }

        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        if (hasPermission) {
            Logger.INSTANCE.LogI("permission", "permission granted");
            int resId;
            switch (requestCode) {
                case RequestCode.STORAGE_1:
                case RequestCode.STORAGE_2:
                    resId = R.string.permission_storage_available;
                    break;
                default:
                    resId = R.string.permission_generic_available;
                    break;
            }
            Snackbar.make(layout, resId,
                Snackbar.LENGTH_SHORT)
                .show();
        } else {
            Logger.INSTANCE.LogI("permission", "permissions were NOT granted.");
            Snackbar.make(layout, R.string.permissions_not_granted,
                Snackbar.LENGTH_LONG)
                .setAction(activity.getString(R.string.permission_screen_settings).toUpperCase(),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Activity activity = activityRef.get();
                            if (activity != null) {
                                activity.startActivity(getAppSettingsIntent(activity));
                            }
                        }
                    })
                .show();
        }
    }

    public static Intent getAppSettingsIntent(Context context) {
        Intent intent;
        try {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            String packageName = context.getApplicationContext().getPackageName();
            intent.setData(Uri.fromParts("package", packageName, null));
        } catch (Exception e) {
            // fall back to generic App page
            intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
        }
        return intent;
    }

    public static boolean verifyPermissions(int[] grantResults) {
        // At least one result must be checked.
        if (grantResults.length < 1) {
            return false;
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void updateAdapterViewWidthAfterGlobalLayout(RecyclerView recyclerView, BaseFileAdapter adapter) {
        final WeakReference<RecyclerView> recyclerViewRef = new WeakReference<>(recyclerView);
        final WeakReference<BaseFileAdapter> adapterRef = new WeakReference<>(adapter);
        try {
            recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        RecyclerView recyclerView = recyclerViewRef.get();
                        if (recyclerView == null) {
                            return;
                        }
                        try {
                            recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } catch (Exception ignored) {
                        }
                        BaseFileAdapter adapter = adapterRef.get();
                        if (adapter == null) {
                            return;
                        }
                        adapter.updateMainViewWidth(recyclerView.getMeasuredWidth());
                    }
                });
        } catch (Exception ignored) {
        }
    }

    /**
     * Sorts file info list according to sort setting
     *
     * @param list     A list of file info
     * @param sortMode The sort mode
     */
    public static void sortFileInfoList(
        @NonNull List<FileInfo> list,
        Comparator<FileInfo> sortMode
    ) {

        MiscUtils.throwIfOnMainThread();
        try {
            Collections.sort(list, sortMode);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e, "mode: " + sortMode.toString());
        }
    }

}
