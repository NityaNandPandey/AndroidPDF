package com.pdftron.pdf.model;

/**
 * Helper class used for caching/retrieving free text
 */
public class FreeTextCacheStruct {
    /**
     * Contents key of free text
     */
    public static final String CONTENTS = "contents";
    /**
     * Page number key of free text
     */
    public static final String PAGE_NUM = "pageNum";
    /**
     * Target point key of free text
     */
    public static final String TARGET_POINT = "targetPoint";
    /**
     * Page position X key of free text
     */
    public static final String X = "x";
    /**
     * Page position y key of free text
     */
    public static final String Y = "y";

    /**
     * FreeText contents
     */
    public String contents;
    /**
     * Page number
     */
    public int pageNum;
    /**
     * Page position x
     */
    public float x;
    /**
     * Page position y
     */
    public float y;
}
