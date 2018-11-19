package com.pdftron.pdf.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDelegate;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFNet;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.ReflowProcessor;
import com.pdftron.pdf.config.PDFNetConfig;
import com.pdftron.pdf.config.PDFViewCtrlConfig;
import com.pdftron.pdf.tools.R;

import java.io.File;

/**
 * A collection of utility functions used for easy setup of PDFNet application.
 */
public class AppUtils {
    private static final String TAG = AppUtils.class.getName();

    /**
     * Utility function for setting up default PDFNet application.
     * A valid PDFNet license key is expected to in AndroidManifest meta data.
     *
     * @param applicationContext the context
     */
    public static void initializePDFNetApplication(Context applicationContext) throws PDFNetException {
        initializePDFNetApplication(applicationContext, getLicenseKey(applicationContext), PDFNetConfig.getDefaultConfig());
    }

    /**
     * Utility function for setting up default PDFNet application.
     *
     * @param applicationContext the context
     * @param licenseKey         the license key
     */
    public static void initializePDFNetApplication(Context applicationContext, String licenseKey) throws PDFNetException {
        initializePDFNetApplication(applicationContext, licenseKey, PDFNetConfig.getDefaultConfig());
    }

    /**
     * Utility function for setting up custom PDFNet application.
     * A valid PDFNet license key is expected to in AndroidManifest meta data.
     *
     * @param applicationContext the context
     * @param config             the custom {@link PDFNetConfig}
     */
    public static void initializePDFNetApplication(Context applicationContext, PDFNetConfig config) throws PDFNetException {
        initializePDFNetApplication(applicationContext, getLicenseKey(applicationContext), config);
    }

    /**
     * Utility function for setting up custom PDFNet application.
     *
     * @param applicationContext the context
     * @param licenseKey         the license key
     * @param config             the custom {@link PDFNetConfig}
     */
    public static void initializePDFNetApplication(Context applicationContext, String licenseKey, PDFNetConfig config) throws PDFNetException {
        if (config.getExtraResourcePaths() != null) {
            for (File resPath : config.getExtraResourcePaths()) {
                if (resPath != null && resPath.exists()) {
                    PDFNet.addResourceSearchPath(resPath.getAbsolutePath());
                }
            }
        }
        if (!PDFNet.hasBeenInitialized()) {
            PDFNet.initialize(applicationContext, R.raw.pdfnet, licenseKey);
        }
        PDFNet.enableJavaScript(config.isJavaScriptEnabled());
        PDFNet.setDefaultDiskCachingEnabled(config.isDiskCachingEnabled());
        if (config.getPersistentCachePath() != null) {
            PDFNet.setPersistentCachePath(config.getPersistentCachePath());
        }
        if (config.getTempPath() != null) {
            PDFNet.setTempPath(config.getTempPath());
        }
        PDFNet.setViewerCache(config.getViewerCacheMaxSize(), config.isViewerCacheOnDisk());

        String layoutPluginPath = config.getLayoutPluginPath(applicationContext);
        if (null != layoutPluginPath) {
            PDFNet.addResourceSearchPath(layoutPluginPath);
        }
        String layoutSmartPluginPath = config.getLayoutSmartPluginPath(applicationContext);
        if (null != layoutSmartPluginPath) {
            PDFNet.addResourceSearchPath(layoutSmartPluginPath);
        }
        ReflowProcessor.initialize();

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public static String getLicenseKey(Context applicationContext) {
        try {
            ApplicationInfo ai = applicationContext.getPackageManager().getApplicationInfo(
                applicationContext.getPackageName(),
                PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            return bundle.getString("pdftron_license_key");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Utility function for setting up {@link PDFViewCtrl} with default configuration.
     *
     * @param pdfViewCtrl the {@link PDFViewCtrl}
     */
    public static void setupPDFViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) throws PDFNetException {
        setupPDFViewCtrl(pdfViewCtrl, PDFViewCtrlConfig.getDefaultConfig(pdfViewCtrl.getContext()));
    }

    /**
     * Utility function for setting up {@link PDFViewCtrl}.
     *
     * @param pdfViewCtrl the {@link PDFViewCtrl}
     * @param config      the custom {@link PDFViewCtrlConfig}
     */
    public static void setupPDFViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl, PDFViewCtrlConfig config) throws PDFNetException {
        pdfViewCtrl.setDevicePixelDensity(config.getDeviceDensity(),
            config.getDeviceDensityScaleFactor());

        pdfViewCtrl.setUrlExtraction(config.isUrlExtraction());

        pdfViewCtrl.setupThumbnails(config.isThumbnailUseEmbedded(),
            config.isThumbnailGenerateAtRuntime(),
            config.isThumbnailUseDiskCache(),
            config.getThumbnailMaxSideLength(),
            config.getThumbnailMaxAbsoluteCacheSize(),
            config.getThumbnailMaxPercentageCacheSize());

        pdfViewCtrl.setPageSpacingDP(config.getPageHorizontalColumnSpacing(),
            config.getPageVerticalColumnSpacing(),
            config.getPageHorizontalPadding(),
            config.getPageVerticalPadding());

        pdfViewCtrl.setHighlightFields(config.isHighlightFields());

        pdfViewCtrl.setMaintainZoomEnabled(config.isMaintainZoomEnabled());

        if (config.isMaintainZoomEnabled()) {
            pdfViewCtrl.setPreferredViewMode(config.getPagePreferredViewMode());
        } else {
            pdfViewCtrl.setPageRefViewMode(config.getPageRefViewMode());
        }
        pdfViewCtrl.setPageViewMode(config.getPageViewMode());

        pdfViewCtrl.setZoomLimits(PDFViewCtrl.ZoomLimitMode.RELATIVE, PDFViewCtrlConfig.MIN_RELATIVE_ZOOM_LIMIT, PDFViewCtrlConfig.MAX_RELATIVE_ZOOM_LIMIT);

        pdfViewCtrl.setRenderedContentCacheSize(config.getRenderedContentCacheSize());

        pdfViewCtrl.setImageSmoothing(config.isImageSmoothing());

        pdfViewCtrl.setMinimumRefZoomForMaximumZoomLimit(config.getMinimumRefZoomForMaximumZoomLimit());

        pdfViewCtrl.setDirectionalLockEnabled(config.isDirectionalScrollLockEnabled());
    }
}
