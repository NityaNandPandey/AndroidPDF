package com.pdftron.pdf.utils;
// https://www.programcreek.com/java-api-examples/index.php?source_dir=DroidGraph-master/src/com/android/droidgraph/util/Vec2.java
// Modified by PDFTron

import android.graphics.PointF;

public class Vec2 {
    /**
     * x coordinate of the vector
     */
    private double x;

    /**
     * y coordinate of the vector
     */
    private double y;

    /**
     * Empty constructor, initializes to 0
     */
    public Vec2() {
        this.x = 0;
        this.y = 0;
    }

    /**
     * Creates an instance of the Vec2 class and initializes x and y
     *
     * @param x
     * @param y
     */
    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Vec2) {
            Vec2 v = (Vec2) obj;
            if (this.x == v.x && this.y == v.y) return true;
            else return false;
        }
        return false;
    }

    /**
     * Calculates the vector length
     *
     * @return the length of the vector
     */
    public double length() {
        return Math.sqrt((this.x * this.x + this.y * this.y));
    }

    /**
     * Normalizes the vector
     */
    public void normalize() {
        double len = length();
        this.x /= len;
        this.y /= len;
    }

    /**
     * Returns a vector from this to the point provided
     *
     * @param point to calculate the vector to
     * @return The vector.
     */
    public Vec2 getVectorTo(Vec2 point) {
        Vec2 aux = new Vec2();

        aux.setX(point.x - this.x);
        aux.setY(point.y - this.y);

        return aux;
    }

    public Vec2 getVectorTo(int x, int y) {
        Vec2 aux = new Vec2();

        aux.setX(x - this.x);
        aux.setY(y - this.y);

        return aux;
    }

    /**
     * Sets the x,y
     *
     * @param x
     * @param y
     */
    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Sets the x
     *
     * @param x
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Sets the y
     *
     * @param y
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Adds the offset to the current position
     *
     * @param x to add to the x component
     * @param y to add to the y component
     */
    public void offset(double x, double y) {
        this.x += x;
        this.y += y;
    }

    /**
     * Gets the x value
     *
     * @return x
     */
    public double x() {
        return this.x;
    }

    /**
     * Gets the y value
     *
     * @return y
     */
    public double y() {
        return this.y;
    }

    /**
     * Calculates the dot product of this Vec2 with another
     *
     * @param vec Vec2 to do the product with
     * @return the dot product
     */
    public float dot(Vec2 vec) {
        return (float) (this.x * vec.x() + this.y * vec.y());
    }

    /**
     * Adds to this Vec2 the values of another
     *
     * @param vec Vec2 to add
     */
    public void add(Vec2 vec) {
        this.x += vec.x();
        this.y += vec.y();
    }

    /**
     * Subtracts to this Vec2 the values of another
     *
     * @param vec Vec2 to subtract
     */
    public void subtract(Vec2 vec) {
        this.x -= vec.x();
        this.y -= vec.y();
    }

    public static Vec2 add(Vec2 vec1, Vec2 vec2) {
        return new Vec2(vec1.x() + vec2.x(), vec1.y() + vec2.y());
    }

    public static Vec2 subtract(Vec2 vec1, Vec2 vec2) {
        return new Vec2(vec1.x() - vec2.x(), vec1.y() - vec2.y());
    }

    public static Vec2 multiply(Vec2 vec1, Vec2 vec2) {
        return new Vec2(vec1.x() * vec2.x(), vec1.y() * vec2.y());
    }

    public static Vec2 multiply(Vec2 vec1, double val) {
        return new Vec2(vec1.x() * val, vec1.y() * val);
    }

    /**
     * Multiplies the x and y components by the value
     *
     * @param val Multiplier for the components
     */
    public void scale(float val) {
        this.x *= val;
        this.y *= val;
    }

    /**
     * Gets a Vec2 with the truncated values of the float coordinates
     *
     * @return A Vec2 with no decimals.
     */
    public Vec2 getIntValue() {
        Vec2 intVec = new Vec2();
        intVec.set((int) this.x, (int) this.y);
        return intVec;
    }

    /**
     * Checks if the rounded coordinates of both vectors are equal
     *
     * @param vec Vec2 to check against
     * @return True if they are equal, false if they are not.
     */
    public boolean roundEqual(Vec2 vec) {
        return ((Math.round(this.x) == Math.round(vec.x())) && (Math.round(this.y) == Math.round(vec.y())));
    }

    public Vec2 getPerp() {
        return new Vec2(-this.y, this.x);
    }

    public PointF toPointF() {
        return new PointF((float) this.x, (float) this.y);
    }
}
