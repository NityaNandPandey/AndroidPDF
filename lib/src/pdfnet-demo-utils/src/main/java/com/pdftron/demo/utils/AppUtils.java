package com.pdftron.demo.utils;

import android.content.Context;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.pdftron.common.PDFNetException;
import com.pdftron.common.RecentlyUsedCache;
import com.pdftron.pdf.DocumentPreviewCache;
import com.pdftron.pdf.ReflowProcessor;
import com.pdftron.pdf.config.PDFNetConfig;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.RecentFilesManager;

/**
 * A collection of utility functions used for easy setup of PDFNet application.
 */
public class AppUtils {

    /**
     * Utility function for setting up default PDFNet application.
     * A valid PDFNet license key is expected to in AndroidManifest meta data.
     * If using this function, {@link DocumentPreviewCache}, {@link ReflowProcessor} and
     * {@link RecentlyUsedCache} will also be initialized.
     *
     * @param applicationContext the context
     */
    public static void initializePDFNetApplication(Context applicationContext) throws PDFNetException {
        initializePDFNetApplication(applicationContext, PDFNetConfig.getDefaultConfig());
    }

    /**
     * Utility function for setting up custom PDFNet application.
     * A valid PDFNet license key is expected to in AndroidManifest meta data.
     * If using this function, {@link DocumentPreviewCache}, {@link ReflowProcessor} and
     * {@link RecentlyUsedCache} will also be initialized.
     *
     * @param applicationContext the context
     * @param config             the custom {@link PDFNetConfig}
     */
    public static void initializePDFNetApplication(Context applicationContext, PDFNetConfig config) throws PDFNetException {
        com.pdftron.pdf.utils.AppUtils.initializePDFNetApplication(applicationContext, config);
        initHelper(applicationContext);
    }

    /**
     * Utility function for setting up default PDFNet application.
     *
     * @param applicationContext the context
     * @param licenseKey         the license key
     */
    public static void initializePDFNetApplication(Context applicationContext, String licenseKey) throws PDFNetException {
        com.pdftron.pdf.utils.AppUtils.initializePDFNetApplication(applicationContext, licenseKey);
        initHelper(applicationContext);
    }

    /**
     * Utility function for setting up custom PDFNet application.
     *
     * @param applicationContext the context
     * @param licenseKey         the license key
     * @param config             the custom {@link PDFNetConfig}
     */
    public static void initializePDFNetApplication(Context applicationContext, String licenseKey, PDFNetConfig config) throws PDFNetException {
        com.pdftron.pdf.utils.AppUtils.initializePDFNetApplication(applicationContext, licenseKey, config);
        initHelper(applicationContext);
    }

    public static void initHelper(Context applicationContext) throws PDFNetException {
        // Initialize global thumbnail cache for file browsers
        DocumentPreviewCache.initialize(50 * 1024 * 1024, 0.1);
        // Initialize thumbnail cache for the recent list.
        RecentlyUsedCache.initializeRecentlyUsedCache(RecentFilesManager.MAX_NUM_RECENT_FILES, 10 * 1024 * 1024, 0.1);

        if (!Fresco.hasBeenInitialized()) {
            Fresco.initialize(applicationContext);
        }
    }
}
