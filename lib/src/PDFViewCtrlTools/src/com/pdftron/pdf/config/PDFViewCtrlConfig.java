//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.config;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.utils.Utils;

/**
 * This is a utility class for setting up PDFViewCtrl initially.
 * <p>
 * <div class="warning">
 * If any of the properties are changed via PDFViewCtrl,
 * this default config class will not get updated.
 * </div>
 */
public class PDFViewCtrlConfig implements Parcelable {

    private static final String TAG = PDFViewCtrlConfig.class.getName();

    public static final double MIN_RELATIVE_ZOOM_LIMIT = 1;
    public static final double MAX_RELATIVE_ZOOM_LIMIT = 20;
    public static final double MIN_RELATIVE_REF_ZOOM_DP = 0.5;

    public PDFViewCtrlConfig(Context context) {
        deviceDensity = context.getResources().getDisplayMetrics().density;
        android.graphics.Point displaySize = new android.graphics.Point(0, 0);
        Utils.getDisplaySize(context, displaySize);
        thumbnailMaxSideLength = Math.max(displaySize.x, displaySize.y) / 4;
        long allowedMax = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        renderedContentCacheSize = (long) (allowedMax * 0.25);
        minimumRefZoomForMaximumZoomLimit = MIN_RELATIVE_REF_ZOOM_DP * deviceDensity;
    }

    protected PDFViewCtrlConfig(Parcel in) {
        directionalScrollLockEnabled = in.readByte() != 0;
        minimumRefZoomForMaximumZoomLimit = in.readDouble();
        imageSmoothing = in.readByte() != 0;
        renderedContentCacheSize = in.readLong();
        highlightFields = in.readByte() != 0;
        pageViewMode = PDFViewCtrl.PageViewMode.valueOf(in.readInt());
        pageRefViewMode = PDFViewCtrl.PageViewMode.valueOf(in.readInt());
        pagePreferredViewMode = PDFViewCtrl.PageViewMode.valueOf(in.readInt());
        maintainZoomEnabled = in.readByte() != 0;
        deviceDensityScaleFactor = in.readInt();
        deviceDensity = in.readDouble();
        urlExtraction = in.readByte() != 0;
        thumbnailUseEmbedded = in.readByte() != 0;
        thumbnailGenerateAtRuntime = in.readByte() != 0;
        thumbnailUseDiskCache = in.readByte() != 0;
        thumbnailMaxSideLength = in.readInt();
        thumbnailMaxAbsoluteCacheSize = in.readLong();
        thumbnailMaxPercentageCacheSize = in.readDouble();
        pageHorizontalColumnSpacing = in.readInt();
        pageVerticalColumnSpacing = in.readInt();
        pageHorizontalPadding = in.readInt();
        pageVerticalPadding = in.readInt();
        clientBackgroundColor = in.readInt();
        clientBackgroundColorDark = in.readInt();
    }

    public static final Creator<PDFViewCtrlConfig> CREATOR = new Creator<PDFViewCtrlConfig>() {
        @Override
        public PDFViewCtrlConfig createFromParcel(Parcel in) {
            return new PDFViewCtrlConfig(in);
        }

        @Override
        public PDFViewCtrlConfig[] newArray(int size) {
            return new PDFViewCtrlConfig[size];
        }
    };

    /** @hide */
    public boolean isUrlExtraction() {
        return urlExtraction;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setUrlExtraction(boolean)}
     */
    public PDFViewCtrlConfig setUrlExtraction(boolean urlExtraction) {
        this.urlExtraction = urlExtraction;
        return this;
    }

    /** @hide */
    public boolean isThumbnailUseEmbedded() {
        return thumbnailUseEmbedded;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setupThumbnails(boolean, boolean, boolean, int, long, double)}
     */
    public PDFViewCtrlConfig setThumbnailUseEmbedded(boolean thumbnailUseEmbedded) {
        this.thumbnailUseEmbedded = thumbnailUseEmbedded;
        return this;
    }

    /** @hide */
    public boolean isThumbnailGenerateAtRuntime() {
        return thumbnailGenerateAtRuntime;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setupThumbnails(boolean, boolean, boolean, int, long, double)}
     */
    public PDFViewCtrlConfig setThumbnailGenerateAtRuntime(boolean thumbnailGenerateAtRuntime) {
        this.thumbnailGenerateAtRuntime = thumbnailGenerateAtRuntime;
        return this;
    }

    /** @hide */
    public boolean isThumbnailUseDiskCache() {
        return thumbnailUseDiskCache;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setupThumbnails(boolean, boolean, boolean, int, long, double)}
     */
    public PDFViewCtrlConfig setThumbnailUseDiskCache(boolean thumbnailUseDiskCache) {
        this.thumbnailUseDiskCache = thumbnailUseDiskCache;
        return this;
    }

    /** @hide */
    public int getThumbnailMaxSideLength() {
        return thumbnailMaxSideLength;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setupThumbnails(boolean, boolean, boolean, int, long, double)}
     */
    public PDFViewCtrlConfig setThumbnailMaxSideLength(int thumbnailMaxSideLength) {
        this.thumbnailMaxSideLength = thumbnailMaxSideLength;
        return this;
    }

    /** @hide */
    public long getThumbnailMaxAbsoluteCacheSize() {
        return thumbnailMaxAbsoluteCacheSize;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setupThumbnails(boolean, boolean, boolean, int, long, double)}
     */
    public PDFViewCtrlConfig setThumbnailMaxAbsoluteCacheSize(long thumbnailMaxAbsoluteCacheSize) {
        this.thumbnailMaxAbsoluteCacheSize = thumbnailMaxAbsoluteCacheSize;
        return this;
    }

    /** @hide */
    public double getThumbnailMaxPercentageCacheSize() {
        return thumbnailMaxPercentageCacheSize;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setupThumbnails(boolean, boolean, boolean, int, long, double)}
     */
    public PDFViewCtrlConfig setThumbnailMaxPercentageCacheSize(double thumbnailMaxPercentageCacheSize) {
        this.thumbnailMaxPercentageCacheSize = thumbnailMaxPercentageCacheSize;
        return this;
    }

    /** @hide */
    public int getPageHorizontalColumnSpacing() {
        return pageHorizontalColumnSpacing;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setPageSpacingDP(int, int, int, int)}
     */
    public PDFViewCtrlConfig setPageHorizontalColumnSpacing(int pageHorizontalColumnSpacing) {
        this.pageHorizontalColumnSpacing = pageHorizontalColumnSpacing;
        return this;
    }

    /** @hide */
    public int getPageVerticalColumnSpacing() {
        return pageVerticalColumnSpacing;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setPageSpacingDP(int, int, int, int)}
     */
    public PDFViewCtrlConfig setPageVerticalColumnSpacing(int pageVerticalColumnSpacing) {
        this.pageVerticalColumnSpacing = pageVerticalColumnSpacing;
        return this;
    }

    /** @hide */
    public int getPageHorizontalPadding() {
        return pageHorizontalPadding;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setPageSpacingDP(int, int, int, int)}
     */
    public PDFViewCtrlConfig setPageHorizontalPadding(int pageHorizontalPadding) {
        this.pageHorizontalPadding = pageHorizontalPadding;
        return this;
    }

    /** @hide */
    public int getPageVerticalPadding() {
        return pageVerticalPadding;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setPageSpacingDP(int, int, int, int)}
     */
    public PDFViewCtrlConfig setPageVerticalPadding(int pageVerticalPadding) {
        this.pageVerticalPadding = pageVerticalPadding;
        return this;
    }

    /** @hide */
    public double getDeviceDensity() {
        return deviceDensity;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setDevicePixelDensity(double, double)}
     */
    public PDFViewCtrlConfig setDeviceDensity(double deviceDensity) {
        this.deviceDensity = deviceDensity;
        return this;
    }

    /** @hide */
    public int getDeviceDensityScaleFactor() {
        return deviceDensityScaleFactor;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setDevicePixelDensity(double, double)}
     */
    public PDFViewCtrlConfig setDeviceDensityScaleFactor(int deviceDensityScaleFactor) {
        this.deviceDensityScaleFactor = deviceDensityScaleFactor;
        return this;
    }

    /** @hide */
    public boolean isHighlightFields() {
        return highlightFields;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setHighlightFields(boolean)}
     */
    public PDFViewCtrlConfig setHighlightFields(boolean highlightFields) {
        this.highlightFields = highlightFields;
        return this;
    }

    /** @hide */
    public PDFViewCtrl.PageViewMode getPageViewMode() {
        return pageViewMode;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setPageViewMode(PDFViewCtrl.PageViewMode)}
     */
    public PDFViewCtrlConfig setPageViewMode(PDFViewCtrl.PageViewMode pageViewMode) {
        this.pageViewMode = pageViewMode;
        return this;
    }

    /** @hide */
    public PDFViewCtrl.PageViewMode getPageRefViewMode() {
        return pageRefViewMode;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setPreferredViewMode(PDFViewCtrl.PageViewMode)}
     */
    public PDFViewCtrlConfig setPageRefViewMode(PDFViewCtrl.PageViewMode pageRefViewMode) {
        this.pageRefViewMode = pageRefViewMode;
        return this;
    }

    /** @hide */
    public PDFViewCtrl.PageViewMode getPagePreferredViewMode() {
        return pagePreferredViewMode;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setPreferredViewMode(PDFViewCtrl.PageViewMode)}
     */
    public PDFViewCtrlConfig setPagePreferredViewMode(PDFViewCtrl.PageViewMode pagePreferredViewMode) {
        this.pagePreferredViewMode = pagePreferredViewMode;
        return this;
    }

    /** @hide */
    public boolean isMaintainZoomEnabled() {
        return maintainZoomEnabled;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setMaintainZoomEnabled(boolean)}
     */
    public PDFViewCtrlConfig setMaintainZoomEnabled(boolean maintainZoomEnabled) {
        this.maintainZoomEnabled = maintainZoomEnabled;
        return this;
    }

    /** @hide */
    public double getMinimumRefZoomForMaximumZoomLimit() {
        return minimumRefZoomForMaximumZoomLimit;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setMinimumRefZoomForMaximumZoomLimit(double)}
     */
    public PDFViewCtrlConfig setMinimumRefZoomForMaximumZoomLimit(double minimumRefZoomForMaximumZoomLimit) {
        this.minimumRefZoomForMaximumZoomLimit = minimumRefZoomForMaximumZoomLimit;
        return this;
    }

    /** @hide */
    public boolean isImageSmoothing() {
        return imageSmoothing;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setImageSmoothing(boolean)}
     */
    public PDFViewCtrlConfig setImageSmoothing(boolean imageSmoothing) {
        this.imageSmoothing = imageSmoothing;
        return this;
    }

    /** @hide */
    public long getRenderedContentCacheSize() {
        return renderedContentCacheSize;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setRenderedContentCacheSize(long)}
     */
    public PDFViewCtrlConfig setRenderedContentCacheSize(long renderedContentCacheSize) {
        this.renderedContentCacheSize = renderedContentCacheSize;
        return this;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setDirectionalLockEnabled(boolean)}
     */
    public boolean isDirectionalScrollLockEnabled() {
        return directionalScrollLockEnabled;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setDirectionalLockEnabled(boolean)}
     */
    public PDFViewCtrlConfig setDirectionalScrollLockEnabled(boolean directionalScrollLockEnabled) {
        this.directionalScrollLockEnabled = directionalScrollLockEnabled;
        return this;
    }

    /** @hide */
    public int getClientBackgroundColor() {
        return clientBackgroundColor;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setClientBackgroundColor(int, int, int, boolean)}
     */
    public PDFViewCtrlConfig setClientBackgroundColor(int clientBackgroundColor) {
        this.clientBackgroundColor = clientBackgroundColor;
        return this;
    }

    /** @hide */
    public int getClientBackgroundColorDark() {
        return clientBackgroundColorDark;
    }

    /**
     * See {@link com.pdftron.pdf.PDFViewCtrl#setClientBackgroundColor(int, int, int, boolean)}
     */
    public PDFViewCtrlConfig setClientBackgroundColorDark(int clientBackgroundColorDark) {
        this.clientBackgroundColorDark = clientBackgroundColorDark;
        return this;
    }

    private boolean directionalScrollLockEnabled = true;
    private double minimumRefZoomForMaximumZoomLimit;
    private boolean imageSmoothing = true;
    private long renderedContentCacheSize;
    private boolean highlightFields = true;
    private PDFViewCtrl.PageViewMode pageViewMode = PDFViewCtrl.PageViewMode.FIT_PAGE;
    private PDFViewCtrl.PageViewMode pageRefViewMode = PDFViewCtrl.PageViewMode.FIT_PAGE;
    private PDFViewCtrl.PageViewMode pagePreferredViewMode = PDFViewCtrl.PageViewMode.FIT_PAGE;
    private boolean maintainZoomEnabled = true;
    private int deviceDensityScaleFactor = 1;
    private double deviceDensity;
    private boolean urlExtraction = true;
    private boolean thumbnailUseEmbedded = false;
    private boolean thumbnailGenerateAtRuntime = true;
    private boolean thumbnailUseDiskCache = true;
    private int thumbnailMaxSideLength;
    private long thumbnailMaxAbsoluteCacheSize = 50 * 1024 * 1024;
    private double thumbnailMaxPercentageCacheSize = 0.1;
    private int pageHorizontalColumnSpacing = 3;
    private int pageVerticalColumnSpacing = 3;
    private int pageHorizontalPadding = 0;
    private int pageVerticalPadding = 0;
    private int clientBackgroundColor = PDFViewCtrl.DEFAULT_BG_COLOR;
    private int clientBackgroundColorDark = PDFViewCtrl.DEFAULT_DARK_BG_COLOR;


    /**
     * Gets default PDFNet configuration
     * @param context application context
     * @return the PDFNet configuration
     */
    public static PDFViewCtrlConfig getDefaultConfig(Context context) {
        return new PDFViewCtrlConfig(context);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte) (directionalScrollLockEnabled ? 1 : 0));
        parcel.writeDouble(minimumRefZoomForMaximumZoomLimit);
        parcel.writeByte((byte) (imageSmoothing ? 1 : 0));
        parcel.writeLong(renderedContentCacheSize);
        parcel.writeByte((byte) (highlightFields ? 1 : 0));
        parcel.writeInt(pageViewMode.getValue());
        parcel.writeInt(pageRefViewMode.getValue());
        parcel.writeInt(pagePreferredViewMode.getValue());
        parcel.writeByte((byte) (maintainZoomEnabled ? 1 : 0));
        parcel.writeInt(deviceDensityScaleFactor);
        parcel.writeDouble(deviceDensity);
        parcel.writeByte((byte) (urlExtraction ? 1 : 0));
        parcel.writeByte((byte) (thumbnailUseEmbedded ? 1 : 0));
        parcel.writeByte((byte) (thumbnailGenerateAtRuntime ? 1 : 0));
        parcel.writeByte((byte) (thumbnailUseDiskCache ? 1 : 0));
        parcel.writeInt(thumbnailMaxSideLength);
        parcel.writeLong(thumbnailMaxAbsoluteCacheSize);
        parcel.writeDouble(thumbnailMaxPercentageCacheSize);
        parcel.writeInt(pageHorizontalColumnSpacing);
        parcel.writeInt(pageVerticalColumnSpacing);
        parcel.writeInt(pageHorizontalPadding);
        parcel.writeInt(pageVerticalPadding);
        parcel.writeInt(clientBackgroundColor);
        parcel.writeInt(clientBackgroundColorDark);
    }
}
