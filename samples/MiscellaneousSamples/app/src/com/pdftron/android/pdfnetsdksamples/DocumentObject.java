package com.pdftron.android.pdfnetsdksamples;

/**
 * Created by Renchen on 6/23/2015.
 */
public class DocumentObject {
    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    private String mName;

    public int getmNumPages() {
        return mNumPages;
    }

    public void setmNumPages(int mNumPages) {
        this.mNumPages = mNumPages;
    }

    private int mNumPages;

    public double getmTotalConvertTime() {
        return mTotalConvertTime;
    }

    public void setmTotalConvertTime(double mTotalConvertTime) {
        this.mTotalConvertTime = mTotalConvertTime;
    }

    private double mTotalConvertTime;

    public String getmHash() {
        return mHash;
    }

    public void setmHash(String mHash) {
        this.mHash = mHash;
    }

    private String mHash;

    public String getmWarnings() {
        return mWarnings;
    }

    public void setmWarnings(String mWarnings) {
        this.mWarnings = mWarnings;
    }

    private String mWarnings;

    public String getmErrors() {
        return mErrors;
    }

    public void setmErrors(String mErrors) {
        this.mErrors = mErrors;
    }

    private String mErrors;

    public String getmTimeStats() {
        return mTimeStats;
    }

    public void setmTimeStats(String mTimeStats) {
        this.mTimeStats = mTimeStats;
    }

    private String mTimeStats;

    public DocumentObject(String name, int numPages, double convert_time)
    {
        mName = name;
        mNumPages = numPages;
        mTotalConvertTime = convert_time;
        mWarnings = null;
        mErrors = null;
        mHash = null;
    }


}
