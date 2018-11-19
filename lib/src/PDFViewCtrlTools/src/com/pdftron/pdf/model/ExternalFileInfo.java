//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.model;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import org.apache.commons.io.FilenameUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Facility class for files stored in external storage.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ExternalFileInfo implements Cloneable, BaseFileInfo {

    private static final String TAG = ExternalFileInfo.class.getName();

    private Context mContext;
    private Uri mUri;
    private ExternalFileInfo mParent;
    private Uri mRootUri;
    private String mName;
    private long mDateLastModified = -1;
    private long mSize = -1;
    private String mType;
    private boolean mIsSecured;
    private boolean mIsPackage;
    private boolean mIsHidden = false;

    /**
     * Class constructor
     * @param context The context
     */
    public ExternalFileInfo(Context context) {
        mContext = context;
    }

    /**
     * Class consturctor
     * @param context The context
     * @param parent parent file
     * @param uri uri of this file info
     */
    public ExternalFileInfo(Context context, ExternalFileInfo parent, Uri uri) {
        mContext = context;
        mParent = parent;
        if (parent != null) {
            mUri = DocumentsContract.buildDocumentUriUsingTree(parent.mUri, DocumentsContract.getDocumentId(uri));
            mRootUri = parent.mRootUri;
        } else {
            mUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
            mRootUri = uri;
        }

        initFields();
    }

    /**
     * Initializes the fields by using the specified URI.
     */
    public void initFields() {
        Cursor cursor = null;
        ContentResolver contentResolver = Utils.getContentResolver(mContext);
        if (contentResolver == null) {
            return;
        }
        try {
            cursor = contentResolver.query(mUri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0 && cursor.getColumnCount() > 0) {
                int columnName = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                int columnDate = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
                int columnSize = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE);
                int columnType = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);

                mName = cursor.getString(columnName);
                mDateLastModified = cursor.getLong(columnDate);
                mSize = cursor.getLong(columnSize);
                mType = cursor.getString(columnType);
            }
        } catch (Exception ignored) {
            // for example FileNotFoundException
            // if happens, mType is null and "exists" method will accordingly return false
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Gets the context
     * @return The context
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Gets the URI
     * @return The URI
     */
    public Uri getUri() {
        return mUri;
    }

    /**
     * Sets the URI.
     *
     * @param uri The URI
     */
    public void setUri(Uri uri) {
        mUri = uri;
    }

    /**
     * Gets the parent file
     * @return The parent file.
     */
    public ExternalFileInfo getParent() {
        return mParent;
    }

    /**
     * Gets root URI
     * @return The root URI.
     */
    public Uri getRootUri() {
        return mRootUri;
    }

    /**
     * Sets the root URI.
     *
     * @param rootUri The root URI
     */
    public void setRootUri(Uri rootUri) {
        mRootUri = rootUri;
    }

    /**
     * Gets file name
     * @return The name of file
     */
    @NonNull
    public String getName() {
        return mName == null ? "" : mName;
    }

    /**
     * Gets file mime type
     * @return The mime type of file
     */
    public String getType() {
        return mType;
    }

    /**
     * Gets file size
     * @return The size of file
     */
    public long getSize() {
        return mSize;
    }

    @Override
    public String getIdentifier() {
        return getAbsolutePath() + getSize();
    }

    /**
     * The overloaded implementation of {@link BaseFileInfo#getSizeInfo()}.
     * Gets size in string format of this file
     */
    @Override
    public String getSizeInfo() {
        // Return binary size of file
        return Utils.humanReadableByteCount(mSize, false);
    }

    /**
     * @return The number of files (the first element) and folders (the second element) under this file.
     */
    public int[] getFileCount() {
        int[] count = new int[2];
        for (ExternalFileInfo file : listFiles()) {
            if (file.isDirectory()) {
                count[1]++;
            } else {
                count[0]++;
            }
        }
        return count;
    }

    /**
     * The overloaded implementation of {@link BaseFileInfo#getModifiedDate()}.
     */
    @Override
    public String getModifiedDate() {
        return DateFormat.getInstance().format(new Date(mDateLastModified));
    }

    /**
     * Gets raw modified date
     * @return The raw modified date
     */
    public Long getRawModifiedDate() {
        return mDateLastModified;
    }

    /**
     * Gets tree path
     * @return The tree path
     */
    public String getTreePath() {
        return Utils.getUriTreePath(mUri);
    }

    /**
     * Gets document path
     * @return The document path
     */
    public String getDocumentPath() {
        return Utils.getUriDocumentPath(mUri);
    }

    /**
     * Gets parent directory's relative path
     * @return The parent directory's relative path
     */
    public String getParentRelativePath() {
        String parentRelativePath = getDocumentPath();
        if (!Utils.isNullOrEmpty(parentRelativePath)) {
            // Remove the file/folder's name from the end of the path
            if (parentRelativePath.endsWith(mName)) {
                parentRelativePath = parentRelativePath.substring(0, parentRelativePath.length() - mName.length());
                // Remove trailing "/"
                if (parentRelativePath.endsWith("/")) {
                    parentRelativePath = parentRelativePath.substring(0, parentRelativePath.length() - 1);
                }
            }
        }
        return parentRelativePath;
    }

    /**
     * Gets file volumn
     * @return The volume
     */
    public String getVolume() {
        String volume = "";
        String docPath = getDocumentPath();
        if (!Utils.isNullOrEmpty(docPath)) {
            int volumeSeparatorIndex = docPath.indexOf(':');
            if (volumeSeparatorIndex >= 0) {
                volume = docPath.substring(0, volumeSeparatorIndex);
            }
        }
        return volume;
    }

    /**
     * Gets contained list of files and folders.
     * <br/>
     * <div class="warning"> Note that this should be executed in a background thread.</div>
     * @return a list of files and folders in this folder.
     */
    public ArrayList<ExternalFileInfo> listFiles() {
        ArrayList<ExternalFileInfo> files = new ArrayList<>();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mUri,
            DocumentsContract.getDocumentId(mUri));
        String[] projection = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        };
        Cursor cursor = null;
        ContentResolver contentResolver = Utils.getContentResolver(mContext);
        if (contentResolver == null) {
            return files;
        }
        try {
            cursor = contentResolver.query(childrenUri, projection, null, null, null);
            if (cursor == null || cursor.getCount() <= 0 || cursor.getColumnCount() <= 0) {
                return files;
            }
            int columnId = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int columnName = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int columnDate = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
            int columnSize = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE);
            int columnType = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);
            while (cursor.moveToNext()) {
                ExternalFileInfo file = new ExternalFileInfo(mContext);
                String documentId = cursor.getString(columnId);
                file.mUri = DocumentsContract.buildDocumentUriUsingTree(mUri, documentId);
                file.mParent = this;
                file.mRootUri = mRootUri;
                file.mName = cursor.getString(columnName);
                file.mDateLastModified = cursor.getLong(columnDate);
                file.mSize = cursor.getLong(columnSize);
                file.mType = cursor.getString(columnType);
                files.add(file);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return files;
    }

    /**
     * The overloaded implementation of {@link BaseFileInfo#exists()}.
     */
    @Override
    public boolean exists() {
        if (mType == null) {
            // if any exceptions occurred in initFields (e.g. readExceptionFromParcel during query)
            // then mType is null, so let's assume file doesn't exist
            return false;
        }

        boolean exists;
        Cursor cursor = null;
        try {
            if (DocumentsContract.isDocumentUri(mContext, mUri)) {
                ContentResolver contentResolver = Utils.getContentResolver(mContext);
                if (contentResolver == null) {
                    return false;
                }
                cursor = contentResolver.query(mUri,
                    new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID}, null, null, null);
                exists = (cursor != null && cursor.getCount() > 0 && cursor.getColumnCount() > 0);
            } else {
                exists = Utils.uriHasReadPermission(mContext, mUri);
            }
        } catch (Exception ignored) {
            exists = false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return exists;
    }

    /**
     * Searches this folder for the given display name.
     *
     * <div class="warning"> Note that this should be executed in a background thread.</div>
     *
     * @param displayName The display name
     *
     * @return The list of files match the given display name
     */
    public ExternalFileInfo findFile(String displayName) {
        for (ExternalFileInfo file : listFiles()) {
            if (displayName.equals(file.getFileName())) {
                return file;
            }
        }
        return null;
    }

    /**
     * Returns the appended path component to the specified URI.
     *
     * @param baseUri The base URI
     * @param component The component
     *
     * @return The appended path component
     */
    public static Uri appendPathComponent(final Uri baseUri, String component) {
        String separator;
        if (Uri.decode(baseUri.toString()).endsWith(":")) {
            // Parent uri ends with ":" - append name part directly to parent uri
            separator = "";
        } else {
            // Prefix the appended name part with an encoded "/" character
            separator = "%2F";
        }
        return Uri.parse(baseUri.toString() + separator + Uri.encode(component));
    }

    /**
     * A special version of {@link #listFiles()} that only creates one file.
     *
     * <div class="warning"> Note that this should be executed in a background thread.</div>

     * @param displayName The display name
     *
     * @return A file that match the specified display name
     */
    public ExternalFileInfo getFile(String displayName) {
        // Build the uri of the given file
        Uri uri = appendPathComponent(mUri, displayName);
        // Try to build the file
        ExternalFileInfo file;
        try {
            file = new ExternalFileInfo(mContext, this, uri);
            if (!file.exists()) {
                file = null;
            }
        } catch (Exception e) {
            file = null;
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        return file;
    }

    /**
     * Given the specified uri, builds its corresponding ExternalFile, complete with a reverse
     * mapping to this file.
     *
     * <div class="warning"> Note that this should be executed in a background thread.</div>
     *
     * @param uri The URI
     *
     * @return The tree file
     */
    public ExternalFileInfo buildTree(Uri uri) {
        String parentPath = Utils.getUriDocumentPath(mUri);
        String documentPath = Utils.getUriDocumentPath(uri);

        if (!documentPath.startsWith(parentPath)) {
            // Since the file should be a subtree of this folder, this folder must be the file's prefix
            return null;
        }
        // Gets the relative path under the root
        String relPath = documentPath.substring(parentPath.length());
        if (Utils.isNullOrEmpty(relPath)) {
            // The file is the same as this
            return this;
        }
        // Remove leading and trailing "/"s
        if (relPath.startsWith("/")) {
            relPath = relPath.substring(1);
        }
        if (relPath.endsWith("/")) {
            relPath = relPath.substring(0, relPath.length() - 1);
        }
        ExternalFileInfo parent = this;
        ExternalFileInfo file = null;
        String[] parts = relPath.split("/"); // parts should not be empty, since relPath is not
        for (String part : parts) {
            // Gets the file in the current parent ExternalFile that matches the name
            file = parent.getFile(part);
            if (file == null) {
                // File not found, might have been renamed, moved, or deleted
                break;
            }
            parent = file;
        }

        return file;
    }

    /**
     * Creates a new file as a direct child of this directory.
     *
     * <div class="warning"> Note that this should be executed in a background thread. </div>
     *
     * @param mimeType The mime type
     * @param displayName The display name
     *
     * @return The new file
     */
    @Nullable
    public ExternalFileInfo createFile(String mimeType, String displayName) {
        ContentResolver contentResolver = Utils.getContentResolver(mContext);
        if (contentResolver == null) {
            return null;
        }
        ExternalFileInfo newFile = null;
        try {
            Uri newUri = DocumentsContract.createDocument(contentResolver, mUri,
                mimeType, displayName);
            if (newUri != null) {
                newFile = new ExternalFileInfo(mContext);
                newFile.mUri = newUri;
                newFile.mParent = this;
                newFile.mRootUri = mRootUri;
                newFile.initFields();
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }

        return newFile;
    }

    /**
     * Creates a directory.
     *
     * <div class='warning'>
     *     Note that this should be executed in a background thread.
     * </div>
     *
     * @param displayName The display name
     *
     * @return The created directory
     */
    public ExternalFileInfo createDirectory(String displayName) {
        return createFile(DocumentsContract.Document.MIME_TYPE_DIR, displayName);
    }

    /**
     * Deletes the file.
     *
     * @return True if successful
     */
    public boolean delete() {
        ContentResolver contentResolver = Utils.getContentResolver(mContext);
        if (contentResolver == null) {
            return false;
        }
        try {
            return DocumentsContract.deleteDocument(contentResolver, mUri);
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
        return false;
    }

    /**
     * Rename file.
     *
     * @param displayName The new display name
     *
     * @return True if successful
     */
    public boolean renameTo(String displayName) {
        ContentResolver contentResolver = Utils.getContentResolver(mContext);
        if (contentResolver == null) {
            return false;
        }
        try {
            Uri newUri = DocumentsContract.renameDocument(contentResolver, mUri, displayName);
            if (newUri != null) {
                mUri = newUri;
                mName = displayName;
                return true;
            } else {
                return false;
            }
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
        return false;
    }

    /**
     * Clone file.
     *
     * @return A new file with exact properties
     */
    public ExternalFileInfo clone() {
        ExternalFileInfo clone;
        try {
            clone = (ExternalFileInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
        return clone;
    }

    /**
     * Returns the URI mime type of the specified URI
     *
     * @param context The context
     * @param encodedUri The encoded URI
     *
     * @return The URI mime type
     */
    @Nullable
    public static String getUriMimeType(@Nullable Context context, String encodedUri) {
        if (context == null) {
            return null;
        }

        String mimeType = null;
        Uri uri = Uri.parse(encodedUri);
        Cursor cursor = null;
        ContentResolver contentResolver = Utils.getContentResolver(context);
        if (contentResolver == null) {
            return null;
        }
        try {
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst() && cursor.getCount() > 0 && cursor.getColumnCount() > 0) {
                int columnType = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);

                mimeType = cursor.getString(columnType);
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return mimeType;
    }

    /**
     * The overloaded implementation of {@link BaseFileInfo#getFileType()}.
     */
    @Override
    public int getFileType() {
        if (!isDirectory()) {
            return BaseFileInfo.FILE_TYPE_EXTERNAL;
        } else {
            return BaseFileInfo.FILE_TYPE_EXTERNAL_FOLDER;
        }
    }

    /**
     * The overloaded implementation of {@link BaseFileInfo#isSecured()}.
     */
    @Override
    public boolean isSecured() {
        return mIsSecured;
    }

    /**
     * The overloaded implementation of {@link BaseFileInfo#isPackage()}.
     */
    @Override
    public boolean isPackage() {
        return mIsPackage;
    }

    /**
     * The overloaded implementation of {@link BaseFileInfo#setIsSecured(boolean)}.
     */
    @Override
    public void setIsSecured(boolean value) {
        mIsSecured = value;
    }

    /**
     * The overloaded implementation of {@link BaseFileInfo#setIsPackage(boolean)}.
     */
    @Override
    public void setIsPackage(boolean value) {
        mIsPackage = value;
    }

    /**
     * The overloaded implementation of {@link BaseFileInfo#getAbsolutePath()}.
     */
    @Override
    public String getAbsolutePath() {
        return mUri.toString();
    }

    /**
     * The overloaded implementation of {@link BaseFileInfo#getFileName()}.
     */
    @Override
    public String getFileName() {
        return getName();
    }

    /**
     * @return the file extension
     */
    public String getExtension() {
        return Utils.getExtension(getName());
    }

    /**
     * Whether it is a directory
     * @return True if the file is directory
     */
    @Override
    public boolean isDirectory() {
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(mType);
    }

    /**
     * The overloaded implementation of {@link Object#equals(Object)}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || !(o instanceof ExternalFileInfo)) {
            return false;
        }
        ExternalFileInfo that = (ExternalFileInfo) o;
        return (getType().equals(that.getType()) && getAbsolutePath().equals(that.getAbsolutePath()));
    }

    /**
     * Whether if this file is hidden
     * @return true if it is a hidden file
     */
    @Override
    public boolean isHidden() {
        return mIsHidden;
    }

    /**
     * Hide/ Reveal this file
     * @param isHidden whether to hide this file
     */
    @Override
    public void setHidden(boolean isHidden) {
        mIsHidden = isHidden;
    }
}
