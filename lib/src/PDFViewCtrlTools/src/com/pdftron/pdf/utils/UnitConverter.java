package com.pdftron.pdf.utils;


public class UnitConverter {

    public static final String CM = "cm";
    public static final String INCH = "inch";
    public static final String YARD = "yard";

    public static final String INCH_SHORT = "in";
    public static final String YARD_SHORT = "yd";

    private float mDistance;
    private String mUnitFrom;
    private String mUnitTo;

    public UnitConverter(float distance) {
        mDistance = distance;
    }

    public UnitConverter from(String unit) {
        mUnitFrom = unit;
        return this;
    }

    public float to(String unit) {
        mUnitTo = unit;

        if (mUnitFrom == null || mUnitTo == null) {
            return mDistance;
        }

        if (mUnitFrom.equals(INCH)) {
            if (mUnitTo.equals(CM)) {
                return inchesToCm(mDistance);
            } else if (mUnitTo.equals(YARD)) {
                return inchesToYard(mDistance);
            } else {
                return mDistance;
            }
        } else if (mUnitFrom.equals(CM)) {
            if (mUnitTo.equals(INCH)) {
                return cmToInches(mDistance);
            } else if (mUnitTo.equals(YARD)) {
                return cmToYard(mDistance);
            } else {
                return mDistance;
            }
        }
        return mDistance;
    }

    public static UnitConverter convert(float distance) {
        return new UnitConverter(distance);
    }

    public static float inchesToCm(float inches) {
        return inches * 2.54f;
    }

    public static float inchesToYard(float inches) {
        return inches / 36;
    }

    public static float cmToInches(float cm) {
        return cm / 2.54f;
    }

    public static float cmToYard(float cm) {
        return cm / 91.44f;
    }

    public static float pointsToInches(float points) {
        return points / 72f;
    }

    public static String getDisplayUnit(String unit) {
        if (unit.equals(INCH)) {
            return INCH_SHORT;
        } else if (unit.equals(YARD)) {
            return YARD_SHORT;
        } else {
            return CM;
        }
    }
}
