package com.pdftron.demo.utils;

import android.util.Log;

/**
 * This is a utility singleton class used to log messages.
 */
@SuppressWarnings("UnusedDeclaration") // Public API
public enum Logger {
    INSTANCE;

    private boolean mAllowLoggingInReleaseMode;
    private boolean mDebug;

    Logger() {
        mAllowLoggingInReleaseMode = false;
    }

    public void setDebug(boolean debug) {
        mDebug = debug;
    }

    public void LogV(String tag, String message) {
        if (mDebug || mAllowLoggingInReleaseMode) {
            if (tag != null && message != null) {
                Log.v(tag, message);
            }
        }
    }

    public void LogD(String tag, String message) {
        if (mDebug || mAllowLoggingInReleaseMode) {
            if (tag != null && message != null) {
                Log.d(tag, message);
            }
        }
    }

    public void LogI(String tag, String message) {
        if (mDebug || mAllowLoggingInReleaseMode) {
            if (tag != null && message != null) {
                Log.i(tag, message);
            }
        }
    }

    public void LogW(String tag, String message) {
        if (mDebug || mAllowLoggingInReleaseMode) {
            if (tag != null && message != null) {
                Log.w(tag, message);
            }
        }
    }

    public void LogW(String tag, String message, java.lang.Throwable tr) {
        if (mDebug || mAllowLoggingInReleaseMode) {
            if (tag != null && message != null) {
                Log.w(tag, message, tr);
            }
        }
    }

    public void LogE(String tag, String message) {
        if (mDebug|| mAllowLoggingInReleaseMode) {
            if (tag != null && message != null) {
                Log.e(tag, message);
            }
        }
    }

    public void LogE(String tag, Exception exception) {
        if (mDebug || mAllowLoggingInReleaseMode) {
            exception.printStackTrace();
            if (tag != null && exception.getMessage() != null) {
                Log.e(tag, exception.getMessage());
            }
        }
    }

    public void LogE(String tag, String message, java.lang.Throwable tr) {
        if (mDebug || mAllowLoggingInReleaseMode) {
            if (tag != null && message != null) {
                Log.e(tag, message, tr);
            }
        }
    }

}
