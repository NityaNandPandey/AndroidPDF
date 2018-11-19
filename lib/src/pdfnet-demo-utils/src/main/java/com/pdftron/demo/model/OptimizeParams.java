package com.pdftron.demo.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Options for optimizing a PDF file.
 */
public class OptimizeParams implements Parcelable {
    public int colorDownsampleMode;
    public double colorMaxDpi;
    public double colorResampleDpi;
    public int colorCompressionMode;
    public long colorQuality;

    public int monoDownsampleMode;
    public double monoMaxDpi;
    public double monoResampleDpi;
    public int monoCompressionMode;

    public boolean forceRecompression;
    public boolean forceChanges;

    // No-arg Ctor
    public OptimizeParams() {
    }

    // all getters and setters go here //...

    /**
     * Used to give additional hints on how to process the received parcel.
     */
    @Override
    public int describeContents() {
        // ignore for now
        return 0;
    }

    @Override
    public void writeToParcel(Parcel pc, int flags) {
        pc.writeInt(colorDownsampleMode);
        pc.writeDouble(colorMaxDpi);
        pc.writeDouble(colorResampleDpi);
        pc.writeInt(colorCompressionMode);
        pc.writeLong(colorQuality);

        pc.writeInt(monoDownsampleMode);
        pc.writeDouble(monoMaxDpi);
        pc.writeDouble(monoResampleDpi);
        pc.writeInt(monoCompressionMode);

        pc.writeInt(forceRecompression ? 1 : 0);
        pc.writeInt(forceChanges ? 1 : 0);
    }

    /**
     * Static field used to regenerate object, individually or as arrays
     */
    public static final Parcelable.Creator<OptimizeParams> CREATOR = new Parcelable.Creator<OptimizeParams>() {
        public OptimizeParams createFromParcel(Parcel pc) {
            return new OptimizeParams(pc);
        }

        public OptimizeParams[] newArray(int size) {
            return new OptimizeParams[size];
        }
    };

    /**
     * Ctor from Parcel, reads back fields IN THE ORDER they were written
     */
    public OptimizeParams(Parcel pc) {
        colorDownsampleMode = pc.readInt();
        colorMaxDpi = pc.readDouble();
        colorResampleDpi = pc.readDouble();
        colorCompressionMode = pc.readInt();
        colorQuality = pc.readLong();

        monoDownsampleMode = pc.readInt();
        monoMaxDpi = pc.readDouble();
        monoResampleDpi = pc.readDouble();
        monoCompressionMode = pc.readInt();

        forceRecompression = (pc.readInt() == 1);
        forceChanges = (pc.readInt() == 1);
    }
}
