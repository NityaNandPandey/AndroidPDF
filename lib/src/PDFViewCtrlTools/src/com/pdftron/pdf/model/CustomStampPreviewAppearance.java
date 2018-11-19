//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * The appearance of a custom rubber stamp which is used in the custom rubber dialog's preview.
 */
public class CustomStampPreviewAppearance implements Parcelable {

    private final static String BUNDLE_CUSTOM_STAMP_APPEARANCES = "custom_stamp_appearances";

    public String colorName;
    public int bgColorStart;
    public int bgColorMiddle;
    public int bgColorEnd;
    public int textColor;
    public int borderColor;
    public double fillOpacity;

    /**
     * Class constructor
     *
     * @param bgColorStart  The start background color in gradient background
     * @param bgColorMiddle The background color of buttons in custom rubber stamp dialog
     * @param bgColorEnd    The end background color in gradient background
     * @param textColor     The text color
     * @param borderColor   The border color
     * @param fillOpacity   The fill opacity
     */
    public CustomStampPreviewAppearance(@NonNull String colorName, @ColorInt int bgColorStart, @ColorInt int bgColorMiddle, @ColorInt int bgColorEnd, @ColorInt int textColor, @ColorInt int borderColor, @FloatRange(from = 0, to = 1.0) double fillOpacity) {
        this.colorName = colorName;
        this.bgColorStart = bgColorStart;
        this.bgColorMiddle = bgColorMiddle;
        this.bgColorEnd = bgColorEnd;
        this.textColor = textColor;
        this.borderColor = borderColor;
        this.fillOpacity = fillOpacity;
    }

    /**
     * Puts an array of custom rubber stamp appearances into a bundle.
     *
     * @param bundle                        The bundle
     * @param customStampPreviewAppearances An array of custom rubber stamp appearances
     */
    public static void putCustomStampAppearancesToBundle(Bundle bundle, CustomStampPreviewAppearance[] customStampPreviewAppearances) {
        bundle.putParcelableArray(BUNDLE_CUSTOM_STAMP_APPEARANCES, customStampPreviewAppearances);
    }

    /**
     * Gets an array of custom rubber stamp appearances from bundle
     *
     * @param bundle The bundle
     * @return An array of custom rubber stamp appearances
     */
    public static CustomStampPreviewAppearance[] getCustomStampAppearancesFromBundle(@Nullable Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        return (CustomStampPreviewAppearance[]) bundle.getParcelableArray(BUNDLE_CUSTOM_STAMP_APPEARANCES);
    }

    private CustomStampPreviewAppearance(Parcel in) {
        colorName = in.readString();
        bgColorStart = in.readInt();
        bgColorMiddle = in.readInt();
        bgColorEnd = in.readInt();
        textColor = in.readInt();
        borderColor = in.readInt();
        fillOpacity = in.readDouble();
    }

    public static final Creator<CustomStampPreviewAppearance> CREATOR = new Creator<CustomStampPreviewAppearance>() {
        @Override
        public CustomStampPreviewAppearance createFromParcel(Parcel in) {
            return new CustomStampPreviewAppearance(in);
        }

        @Override
        public CustomStampPreviewAppearance[] newArray(int size) {
            return new CustomStampPreviewAppearance[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(colorName);
        dest.writeInt(bgColorStart);
        dest.writeInt(bgColorMiddle);
        dest.writeInt(bgColorEnd);
        dest.writeInt(textColor);
        dest.writeInt(borderColor);
        dest.writeDouble(fillOpacity);
    }

}
