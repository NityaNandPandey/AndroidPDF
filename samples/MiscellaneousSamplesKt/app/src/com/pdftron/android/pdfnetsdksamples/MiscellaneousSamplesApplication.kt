//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples

import java.util.ArrayList

import android.app.Application
import android.content.Context

import com.pdftron.android.pdfnetsdksamples.samples.AddImageTest
import com.pdftron.android.pdfnetsdksamples.samples.AnnotationTest
import com.pdftron.android.pdfnetsdksamples.samples.BookmarkTest
import com.pdftron.android.pdfnetsdksamples.samples.ContentReplacerTest
import com.pdftron.android.pdfnetsdksamples.samples.DigitalSignaturesTest
import com.pdftron.android.pdfnetsdksamples.samples.ElementBuilderTest
import com.pdftron.android.pdfnetsdksamples.samples.ElementEditTest
import com.pdftron.android.pdfnetsdksamples.samples.ElementReaderAdvTest
import com.pdftron.android.pdfnetsdksamples.samples.ElementReaderTest
import com.pdftron.android.pdfnetsdksamples.samples.EncTest
import com.pdftron.android.pdfnetsdksamples.samples.FDFTest
import com.pdftron.android.pdfnetsdksamples.samples.ImageExtractTest
import com.pdftron.android.pdfnetsdksamples.samples.ImpositionTest
import com.pdftron.android.pdfnetsdksamples.samples.InteractiveFormsTest
import com.pdftron.android.pdfnetsdksamples.samples.JBIG2Test
import com.pdftron.android.pdfnetsdksamples.samples.LogicalStructureTest
import com.pdftron.android.pdfnetsdksamples.samples.OptimizerTest
import com.pdftron.android.pdfnetsdksamples.samples.PDFATest
import com.pdftron.android.pdfnetsdksamples.samples.PDFDocMemoryTest
import com.pdftron.android.pdfnetsdksamples.samples.PDFDrawTest
import com.pdftron.android.pdfnetsdksamples.samples.PDFLayersTest
import com.pdftron.android.pdfnetsdksamples.samples.PDFPackageTest
import com.pdftron.android.pdfnetsdksamples.samples.PDFPageTest
import com.pdftron.android.pdfnetsdksamples.samples.PDFRedactTest
import com.pdftron.android.pdfnetsdksamples.samples.PageLabelsTest
import com.pdftron.android.pdfnetsdksamples.samples.DocxConvertTest
import com.pdftron.android.pdfnetsdksamples.samples.RectTest
import com.pdftron.android.pdfnetsdksamples.samples.SDFTest
import com.pdftron.android.pdfnetsdksamples.samples.StamperTest
import com.pdftron.android.pdfnetsdksamples.samples.TextExtractTest
import com.pdftron.android.pdfnetsdksamples.samples.TextSearchTest
import com.pdftron.android.pdfnetsdksamples.samples.U3DTest
import com.pdftron.android.pdfnetsdksamples.samples.UnicodeWriteTest
import com.pdftron.android.pdfnetsdksamples.samples.PatternTest
import com.pdftron.android.pdfnetsdksamples.samples.WordToPDFTest

class MiscellaneousSamplesApplication : Application() {

    private val mListSamples = ArrayList<PDFNetSample>()
    var context: Context? = null
        private set

    val content: List<PDFNetSample>
        get() = this.mListSamples

    override fun onCreate() {
        super.onCreate()
        instance = this
        mListSamples.add(DocxConvertTest())
        mListSamples.add(AddImageTest())
        mListSamples.add(AnnotationTest())
        mListSamples.add(BookmarkTest())
        mListSamples.add(ContentReplacerTest())
        mListSamples.add(DigitalSignaturesTest())
        mListSamples.add(ElementBuilderTest())
        mListSamples.add(ElementEditTest())
        mListSamples.add(ElementReaderTest())
        mListSamples.add(ElementReaderAdvTest())
        mListSamples.add(EncTest())
        mListSamples.add(FDFTest())
        mListSamples.add(ImageExtractTest())
        mListSamples.add(ImpositionTest())
        mListSamples.add(InteractiveFormsTest())
        mListSamples.add(JBIG2Test())
        mListSamples.add(LogicalStructureTest())
        mListSamples.add(OptimizerTest())
        mListSamples.add(PageLabelsTest())
        mListSamples.add(PatternTest())
        mListSamples.add(PDFATest())
        mListSamples.add(PDFDocMemoryTest())
        mListSamples.add(PDFDrawTest())
        mListSamples.add(PDFLayersTest())
        mListSamples.add(PDFPackageTest())
        mListSamples.add(PDFPageTest())
        mListSamples.add(PDFRedactTest())
        mListSamples.add(RectTest())
        mListSamples.add(SDFTest())
        mListSamples.add(StamperTest())
        mListSamples.add(TextExtractTest())
        mListSamples.add(TextSearchTest())
        mListSamples.add(U3DTest())
        mListSamples.add(UnicodeWriteTest())
        mListSamples.add(WordToPDFTest(applicationContext))

        context = applicationContext
    }

    companion object {
        var instance: MiscellaneousSamplesApplication? = null
            private set
    }
}
