//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.PointF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.pdftron.common.Matrix2D;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.annots.Polygon;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.DrawingUtils;
import com.pdftron.pdf.utils.PointFPool;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

/**
 * This class is for creating a rectangle annotation.
 */
@Keep
public class CloudCreate extends PolygonCreate {

    public static final int BORDER_INTENSITY = 2;

    /**
     * Class constructor
     */
    public CloudCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.CLOUD_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return AnnotStyle.CUSTOM_ANNOT_TYPE_CLOUD;
    }

    /**
     * The overload implementation of {@link AdvancedShapeCreate#createMarkup(PDFDoc, ArrayList)}.
     */
    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc,
                                 ArrayList<Point> pagePoints) throws PDFNetException {
        Rect annotRect = Utils.getBBox(pagePoints);
        if (annotRect == null) {
            return null;
        }
        annotRect.inflate(mThickness);

        Polygon poly = (Polygon) super.createMarkup(doc, pagePoints);
        poly.setBorderEffect(Markup.e_Cloudy);

        int pointIdx = 0;
        for (Point point : pagePoints) {
            poly.setVertex(pointIdx++, point);
        }
        poly.setRect(annotRect);

        return poly;
    }

    @Override
    protected void drawMarkup(@NonNull Canvas canvas,
                              Matrix tfm,
                              @NonNull ArrayList<PointF> canvasPoints) {
        if (mPdfViewCtrl == null) {
            return;
        }

        DrawingUtils.drawCloud(mPdfViewCtrl, getPageNum(), canvas,
            canvasPoints, mPath, mPaint, mStrokeColor,
            mFillPaint, mFillColor, BORDER_INTENSITY);
    }

    /**
     * Gets the closed path of poly
     *
     * @param input The input poly witch might be open
     * @return The closed version of the input poly
     */
    public static ArrayList<PointF> getClosedPoly(ArrayList<PointF> input) {
        ArrayList<PointF> poly = new ArrayList<>(input);
        int size = input.size();
        if (size >= 2) {
            PointF firstPoint = input.get(0);
            PointF lastPoint = input.get(size - 1);
            if (!firstPoint.equals(lastPoint)) {
                poly.add(firstPoint);
            }
        }
        return poly;
    }

    /**
     * @param canvasPoints The canvas points
     * @return True if this polygon is clockwise oriented;
     * False if this polygon is counterclockwise oriented
     */
    public static boolean IsPolyWrapClockwise(ArrayList<PointF> canvasPoints) {
        PointF oldPoint = null;
        double accum = 0.0;
        for (PointF point : canvasPoints) {
            if (oldPoint != null) {
                accum += (point.x - oldPoint.x) * (point.y + oldPoint.y);
            }
            oldPoint = point;
        }

        return accum < 0;
    }

    public static PointF add(@NonNull PointF p1,
                              @NonNull PointF p2) {
        return PointFPool.getInstance().obtain(p1.x + p2.x, p1.y + p2.y);
    }

    public static PointF subtract(@NonNull PointF p1,
                                   @NonNull PointF p2) {
        return PointFPool.getInstance().obtain(p1.x - p2.x, p1.y - p2.y);
    }

    public static PointF multiply(@NonNull PointF p,
                                   double n) {
        return PointFPool.getInstance().obtain((float) (p.x * n), (float) (p.y * n));
    }

    public static PointF divide(@NonNull PointF p,
                                 double n) {
        return multiply(p, 1. / n);
    }

    public static double cross(@NonNull PointF p1,
                                @NonNull PointF p2) {
        return p1.x * p2.y - p1.y * p2.x;
    }

    /**
     * map radians to a value between 0 and 360 degrees
     */
    public static double toDegreesMod360(double radians) {
        double PI_2 = Math.PI * 2;
        double invPI = 1.0 / Math.PI;
        double fracPart = ((radians + PI_2) % PI_2) * invPI;
        return fracPart * 180;
    }

    public static void arcToCurves(double startX,
                                    double startY,
                                    ArrayList<Double> outPoints,
                                    ArrayList<Character> outOperations,
                                    double xr,
                                    double yr,
                                    double rx,
                                    boolean isLargeArc,
                                    boolean sweep,
                                    double endX, double endY) {
        if (xr < 0.0) {
            xr = -xr;
        }
        if (yr < 0.0) {
            yr = -yr;
        }

        // Calculate the middle point between
        // the current and the final points
        //------------------------
        double dx2 = (startX - endX) / 2.0;
        double dy2 = (startY - endY) / 2.0;

        double cosA = Math.cos(rx);
        double sinA = Math.sin(rx);

        // Calculate (x1, y1)
        //------------------------
        double x1 = cosA * dx2 + sinA * dy2;
        double y1 = -sinA * dx2 + cosA * dy2;

        // Ensure radii are large enough
        //------------------------
        double prx = xr * xr;
        double pry = yr * yr;
        double px1 = x1 * x1;
        double py1 = y1 * y1;

        // Check that radii are large enough
        //------------------------
        double radiiCheck = px1 / prx + py1 / pry;
        if (radiiCheck > 1.0) {
            xr = Math.sqrt(radiiCheck) * xr;
            yr = Math.sqrt(radiiCheck) * yr;
            prx = xr * xr;
            pry = yr * yr;
        }

        double denom = (prx * py1) + (pry * px1);
        if (denom == 0) {
            // we shouldn't divide by zero
            // (this is a strange case if it occurs in an actual document
            // since it seems to only occur when start=end)
            return;
        }

        // Calculate (cx1, cy1)
        //------------------------
        double sign = (isLargeArc == sweep) ? -1.0 : 1.0;
        double sq = ((prx * pry) - (prx * py1) - (pry * px1)) / (denom);
        double coef = sign * Math.sqrt((sq < 0) ? 0 : sq);
        double cx1 = coef * ((xr * y1) / yr);
        double cy1 = coef * -((yr * x1) / xr);

        //
        // Calculate (cx, cy) from (cx1, cy1)
        //------------------------
        double sx2 = (startX + endX) / 2.0;
        double sy2 = (startY + endY) / 2.0;
        double cx = sx2 + (cosA * cx1 - sinA * cy1);
        double cy = sy2 + (sinA * cx1 + cosA * cy1);

        // Calculate the startAngle (angle1) and the sweepAngle (dangle)
        //------------------------
        double ux = (x1 - cx1) / xr;
        double uy = (y1 - cy1) / yr;
        double vx = (-x1 - cx1) / xr;
        double vy = (-y1 - cy1) / yr;
        double p, n;

        // Calculate the angle start
        //------------------------
        n = Math.sqrt((ux * ux) + (uy * uy));
        p = ux; // (1 * ux) + (0 * uy)
        sign = (uy < 0) ? -1.0 : 1.0;
        double startAngle = sign * Math.acos(p / n);

        // Calculate the sweep angle
        //------------------------
        n = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
        p = ux * vx + uy * vy;
        sign = ((ux * vy) - (uy * vx) < 0) ? -1.0 : 1.0;

        // we want to avoid taking the inverse cosine of
        // a value > 1 or < -1 since it is undefined
        // (this can occur due to rounding error)
        double acosValue = Math.max(Math.min(p / n, 1.0), -1.0);

        double sweepAngle = sign * Math.acos(acosValue);
        if (!sweep && sweepAngle > 0) {
            sweepAngle -= Math.PI * 2.0;
        } else if (sweep && sweepAngle < 0) {
            sweepAngle += Math.PI * 2.0;
        }

        int numVertices;
        double[] vertices = new double[26];
        boolean bLineTo = false;

        if (Math.abs(sweepAngle) < 1e-10) {
            numVertices = 4;

            // LineTo command;
            bLineTo = true;

            vertices[0] = xr * Math.cos(startAngle);
            vertices[1] = yr * Math.sin(startAngle);
            vertices[2] = xr * Math.cos(startAngle + sweepAngle);
            vertices[3] = yr * Math.sin(startAngle + sweepAngle);
        } else {
            double totalSweep = 0.0;
            double localSweep;
            double prev_sweep;
            numVertices = 2;
            // CurveTo command;
            boolean done = false;
            do {
                if (sweepAngle < 0.0) {
                    prev_sweep = totalSweep;
                    localSweep = -Math.PI * 0.5;
                    totalSweep -= Math.PI * 0.5;
                    if (totalSweep <= sweepAngle + 0.01) // bezier_arc_angle_epsilon = 0.01
                    {
                        localSweep = sweepAngle - prev_sweep;
                        done = true;
                    }
                } else {
                    prev_sweep = totalSweep;
                    localSweep = Math.PI * 0.5;
                    totalSweep += Math.PI * 0.5;
                    if (totalSweep >= sweepAngle - 0.01) // bezier_arc_angle_epsilon = 0.01
                    {
                        localSweep = sweepAngle - prev_sweep;
                        done = true;
                    }
                }

                // BEGIN: Arc to cubic bezier curve

                final int OFFSET = numVertices - 2;

                double x0 = Math.cos(localSweep / 2.0);
                double y0 = Math.sin(localSweep / 2.0);
                double tx = (1.0 - x0) * 4.0 / 3.0;
                double ty = y0 - tx * x0 / y0;
                double[] px = new double[4];
                double[] py = new double[4];
                px[0] = x0;
                py[0] = -y0;
                px[1] = x0 + tx;
                py[1] = -ty;
                px[2] = x0 + tx;
                py[2] = ty;
                px[3] = x0;
                py[3] = y0;

                double sn = Math.sin(startAngle + localSweep / 2.0);
                double cs = Math.cos(startAngle + localSweep / 2.0);

                for (int i = 0; i < 4; i++) {
                    vertices[OFFSET + i * 2] = xr * (px[i] * cs - py[i] * sn);
                    vertices[OFFSET + i * 2 + 1] = yr * (px[i] * sn + py[i] * cs);
                }

                // END: Arc to cubic bezier curve

                numVertices += 6;
                startAngle += localSweep;
            }
            while (!done && numVertices < 26);
        }

        try {
            // We can now transform the resulting arc
            Matrix2D mtx = Matrix2D.rotationMatrix(-rx);
            mtx = mtx.translate(cx, cy);

            for (int i = 2; i < numVertices - 2; i += 2) {
                Point point = mtx.multPoint(vertices[i], vertices[i + 1]);
                vertices[i] = point.x;
                vertices[i + 1] = point.y;
            }
        } catch (PDFNetException ignored) {
            return;
        }

        // We must make sure that the starting and ending points
        // exactly coincide with the initial (x0,y0) and (x2,y2)
        vertices[0] = startX;
        vertices[1] = startY;
        if (numVertices > 2) {
            vertices[numVertices - 2] = endX;
            vertices[numVertices - 1] = endY;
        }

        if (bLineTo) {
            outOperations.add('l');
            outOperations.add('l');
            outPoints.add(vertices[0]);
            outPoints.add(vertices[1]);
            outPoints.add(vertices[2]);
            outPoints.add(vertices[3]);
        } else {
            outOperations.add('l');
            outPoints.add(vertices[0]);
            outPoints.add(vertices[1]);

            for (int i = 2; i < numVertices; i += 6) {
                outOperations.add('c');
                outPoints.add(vertices[i]);
                outPoints.add(vertices[i + 1]);
                outPoints.add(vertices[i + 2]);
                outPoints.add(vertices[i + 3]);
                outPoints.add(vertices[i + 4]);
                outPoints.add(vertices[i + 5]);
            }
        }
    }

    public static PointF arcTo(@NonNull Path path,
                         float startX,
                         float startY,
                         double xr,
                         double yr,
                         double rx,
                         boolean isLargeArc,
                         boolean sweep,
                         double endX,
                         double endY) {
        ArrayList<Double> outPoints = new ArrayList<>(30);
        ArrayList<Character> outOperations = new ArrayList<>(); // 'l':line or 'c':cube
        arcToCurves(startX, startY, outPoints, outOperations, xr, yr, rx, isLargeArc, sweep, endX, endY);

        // write the point data gathered to the ElementBuilderImpl
        int pointIndex = 0;
        for (Character operation : outOperations) {
            if (operation == 'c') {
                path.cubicTo(outPoints.get(pointIndex).floatValue(),
                    outPoints.get(pointIndex + 1).floatValue(),
                    outPoints.get(pointIndex + 2).floatValue(),
                    outPoints.get(pointIndex + 3).floatValue(),
                    outPoints.get(pointIndex + 4).floatValue(),
                    outPoints.get(pointIndex + 5).floatValue());
                startX = outPoints.get(pointIndex + 4).floatValue();
                startY = outPoints.get(pointIndex + 5).floatValue();
                pointIndex += 6;
            } else if (operation == 'l') {
                path.lineTo(outPoints.get(pointIndex).floatValue(),
                    outPoints.get(pointIndex + 1).floatValue());
                startX = outPoints.get(pointIndex).floatValue();
                startY = outPoints.get(pointIndex + 1).floatValue();
                pointIndex += 2;
            }
        }

        return PointFPool.getInstance().obtain(startX, startY);
    }
}
