package com.pdftron.android.pdfnetsdksamples

/**
 * Created by Renchen on 6/23/2015.
 */
class DocumentObject(private var mName: String?, private var mNumPages: Int, private var mTotalConvertTime: Double) {

    private var mHash: String? = null

    private var mWarnings: String? = null

    private var mErrors: String? = null

    private var mTimeStats: String? = null
    fun getmName(): String? {
        return mName
    }

    fun setmName(mName: String) {
        this.mName = mName
    }

    fun getmNumPages(): Int {
        return mNumPages
    }

    fun setmNumPages(mNumPages: Int) {
        this.mNumPages = mNumPages
    }

    fun getmTotalConvertTime(): Double {
        return mTotalConvertTime
    }

    fun setmTotalConvertTime(mTotalConvertTime: Double) {
        this.mTotalConvertTime = mTotalConvertTime
    }

    fun getmHash(): String? {
        return mHash
    }

    fun setmHash(mHash: String) {
        this.mHash = mHash
    }

    fun getmWarnings(): String? {
        return mWarnings
    }

    fun setmWarnings(mWarnings: String) {
        this.mWarnings = mWarnings
    }

    fun getmErrors(): String? {
        return mErrors
    }

    fun setmErrors(mErrors: String) {
        this.mErrors = mErrors
    }

    fun getmTimeStats(): String? {
        return mTimeStats
    }

    fun setmTimeStats(mTimeStats: String) {
        this.mTimeStats = mTimeStats
    }

    init {
        mWarnings = null
        mErrors = null
        mHash = null
    }


}
