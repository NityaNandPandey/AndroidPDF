//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Line;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.UnitConverter;

import java.util.Locale;

@Keep
public class RulerCreate extends ArrowCreate {

    protected PointF mPt5, mPt6;

    private String mText = "";

    private RulerItem mRuler;

    /**
     * Class constructor
     *
     */
    public RulerCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);

        mPt5 = new PointF(0, 0);
        mPt6 = new PointF(0, 0);
    }

    @Override
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        super.setupAnnotProperty(annotStyle);

        mRuler = new RulerItem(annotStyle.getRulerBaseValue(),
            annotStyle.getRulerBaseUnit(),
            annotStyle.getRulerTranslateValue(),
            annotStyle.getRulerTranslateUnit());

        SharedPreferences settings = Tool.getToolPreferences(mPdfViewCtrl.getContext());
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(getRulerBaseUnitKey(getCreateAnnotType()), mRuler.mRulerBaseUnit);
        editor.putString(getRulerTranslateUnitKey(getCreateAnnotType()), mRuler.mRulerTranslateUnit);
        editor.putFloat(getRulerBaseValueKey(getCreateAnnotType()), mRuler.mRulerBase);
        editor.putFloat(getRulerTranslateValueKey(getCreateAnnotType()), mRuler.mRulerTranslate);
        editor.apply();
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.RULER_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_RULER;
    }

    @Override
    public boolean onDown(MotionEvent e) {

        Context context = mPdfViewCtrl.getContext();
        SharedPreferences settings = Tool.getToolPreferences(context);

        mRuler = new RulerItem();
        mRuler.mRulerBaseUnit = settings.getString(getRulerBaseUnitKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultRulerBaseUnit(context, getCreateAnnotType()));
        mRuler.mRulerBase = settings.getFloat(getRulerBaseValueKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultRulerBaseValue(context, getCreateAnnotType()));
        mRuler.mRulerTranslateUnit = settings.getString(getRulerTranslateUnitKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultRulerTranslateUnit(context, getCreateAnnotType()));
        mRuler.mRulerTranslate = settings.getFloat(getRulerTranslateValueKey(getCreateAnnotType()), ToolStyleConfig.getInstance().getDefaultRulerTranslateValue(context, getCreateAnnotType()));

        return super.onDown(e);
    }

    @Override
    public boolean onMove(MotionEvent e1, MotionEvent e2, float x_dist, float y_dist) {
        super.onMove(e1, e2, x_dist, y_dist);

        // We are scrolling
        if (mAllowTwoFingerScroll) {
            return false;
        }
        if (mAllowOneFingerScrollWithStylus) {
            return false;
        }

        double[] pts1, pts2;
        pts1 = mPdfViewCtrl.convScreenPtToPagePt(mPt1.x, mPt1.y, mDownPageNum);
        pts2 = mPdfViewCtrl.convScreenPtToPagePt(mPt2.x, mPt2.y, mDownPageNum);

        mText = getRulerLabel(mRuler, pts1[0], pts1[1], pts2[0], pts2[1]);

        return true;
    }

    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        Line line = Line.create(doc, bbox);
        line.setShowCaption(true);
        line.setContents(mText);
        line.setEndStyle(Line.e_Butt);
        line.setStartStyle(Line.e_Butt);
        line.setCaptionPosition(Line.e_Top);

        RulerItem.saveToAnnot(line, mRuler);

        return line;
    }

    @Override
    protected void calculateEndingStyle() {
        DrawingUtils.calcRuler(mPt1, mPt2, mPt3, mPt4, mPt5, mPt6,
            mThickness, mZoom);
    }

    @Override
    public void onDraw(Canvas canvas, Matrix tfm) {
        // We are scrolling
        if (mAllowTwoFingerScroll) {
            return;
        }
        if (mIsAllPointsOutsidePage) {
            return;
        }

        DrawingUtils.drawRuler(canvas, mPt1, mPt2,
            mPt3, mPt4, mPt5, mPt6, mOnDrawPath, mPaint, mText, mZoom);
    }

    public static String getRulerLabel(RulerItem rulerItem, double pt1x, double pt1y, double pt2x, double pt2y) {
        float distance = Math.round(Math.sqrt(Math.pow((pt2x - pt1x), 2) + Math.pow((pt2y - pt1y), 2)));

        float inches = UnitConverter.pointsToInches(distance);

        float baseMeasure = UnitConverter.convert(inches).from(UnitConverter.INCH).to(rulerItem.mRulerBaseUnit);
        float convertedMeasure = UnitConverter.convert(baseMeasure).from(rulerItem.mRulerBaseUnit).to(rulerItem.mRulerTranslateUnit);
        float ratio = rulerItem.mRulerTranslate / rulerItem.mRulerBase;

        float distanceFinal = convertedMeasure * ratio;
        String distanceFinals = String.format(Locale.getDefault(), "%.2f", distanceFinal);

        return distanceFinals + UnitConverter.getDisplayUnit(rulerItem.mRulerTranslateUnit);
    }
}
