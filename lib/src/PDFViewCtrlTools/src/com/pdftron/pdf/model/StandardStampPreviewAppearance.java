package com.pdftron.pdf.model;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class StandardStampPreviewAppearance implements Parcelable {

    private final static String BUNDLE_STANDARD_STAMP_APPEARANCES = "standard_stamp_appearances";

    public String text;
    public CustomStampPreviewAppearance previewAppearance;
    public boolean pointLeft;
    @SuppressWarnings("WeakerAccess")
    public boolean pointRight;

    /**
     * Class constructor when should look for a stamp in "stamps_icon.pdf"
     *
     * @param stampLabel The label of stamp
     */
    public StandardStampPreviewAppearance(@NonNull String stampLabel) {
        this.text = stampLabel;
    }

    /**
     * Class constructor when the stamp should be created using given info
     *
     * @param text              The text to show in stamp
     * @param previewAppearance The appearance option for creating stamp
     */
    public StandardStampPreviewAppearance(@NonNull String text, @NonNull CustomStampPreviewAppearance previewAppearance) {
        this(text, previewAppearance, false, false);
    }

    /**
     * Class constructor when the stamp should be created using given info
     *
     * @param text              The text to show in stamp
     * @param previewAppearance The appearance option for creating stamp
     * @param pointLeft         True if point to left
     * @param pointRight        True if point to right
     */
    public StandardStampPreviewAppearance(@NonNull String text, @NonNull CustomStampPreviewAppearance previewAppearance, boolean pointLeft, boolean pointRight) {
        this.text = text;
        this.previewAppearance = previewAppearance;
        this.pointLeft = pointLeft;
        this.pointRight = pointRight;
    }

    /**
     * Puts an array of standard rubber stamp appearances into a bundle.
     *
     * @param bundle                          The bundle
     * @param standardStampPreviewAppearances An array of standard rubber stamp appearances
     */
    public static void putStandardStampAppearancesToBundle(Bundle bundle, StandardStampPreviewAppearance[] standardStampPreviewAppearances) {
        bundle.putParcelableArray(BUNDLE_STANDARD_STAMP_APPEARANCES, standardStampPreviewAppearances);
    }

    /**
     * Gets an array of standard rubber stamp appearances from bundle
     *
     * @param bundle The bundle
     * @return An array of standard rubber stamp appearances
     */
    public static StandardStampPreviewAppearance[] getStandardStampAppearancesFromBundle(@Nullable Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        return (StandardStampPreviewAppearance[]) bundle.getParcelableArray(BUNDLE_STANDARD_STAMP_APPEARANCES);
    }

    private StandardStampPreviewAppearance(Parcel in) {
        text = in.readString();
        previewAppearance = in.readParcelable(CustomStampPreviewAppearance.class.getClassLoader());
        pointLeft = in.readByte() != 0;
        pointRight = in.readByte() != 0;
    }

    public static final Creator<StandardStampPreviewAppearance> CREATOR = new Creator<StandardStampPreviewAppearance>() {
        @Override
        public StandardStampPreviewAppearance createFromParcel(Parcel in) {
            return new StandardStampPreviewAppearance(in);
        }

        @Override
        public StandardStampPreviewAppearance[] newArray(int size) {
            return new StandardStampPreviewAppearance[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(text);
        dest.writeParcelable(previewAppearance, flags);
        dest.writeByte((byte) (pointLeft ? 1 : 0));
        dest.writeByte((byte) (pointRight ? 1 : 0));
    }
}
