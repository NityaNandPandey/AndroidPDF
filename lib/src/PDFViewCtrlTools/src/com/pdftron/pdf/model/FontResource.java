package com.pdftron.pdf.model;

import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class that contains info about font resource
 */
public class FontResource {
    private String mDisplayName = "";
    private String mFontName = "";
    private String mFilePath = "";
    private String mPDFTronName = "";


    /**
     * Class constructor
     * @param displayName font display name
     * @param filePath font system path
     * @param fontName font name
     * @param pdftronName font pdftron name
     */
    public FontResource(String displayName, String filePath, String fontName, String pdftronName) {
        if (displayName != null) {
            mDisplayName = displayName;
        }
        if (filePath != null) {
            mFilePath = filePath;
        }
        if (fontName != null) {
            mFontName = fontName;
        }
        if (pdftronName != null) {
            mPDFTronName = pdftronName;
        }
    }

    /**
     * Class constructor
     * @param fontName font name
     */
    public FontResource(String fontName) {
        if (fontName == null) {
            return;
        }
        mFontName = fontName;
        mDisplayName = fontName;
        mPDFTronName = fontName;
    }

    /**
     * Sets font display name
     * @param displayName display name
     */
    public void setDisplayName(String displayName) {
        this.mDisplayName = displayName;
    }

    /**
     * Sets font name
     * @param fontName font name
     */
    public void setFontName(String fontName) {
        this.mFontName = fontName;
    }

    /**
     * Sets font system path
     * @param filePath font file path
     */
    public void setFilePath(String filePath) {
        this.mFilePath = filePath;
    }

    /**
     * Sets font pdftron name
     * @param PDFTronName font pdftron name
     */
    public void setPDFTronName(String PDFTronName) {
        this.mPDFTronName = PDFTronName;
    }

    /**
     * Gets display name
     * @return display name
     */
    public String getDisplayName() {
        return mDisplayName;
    }

    /**
     * Gets font file path
     * @return font file path
     */
    public String getFilePath() {
        return mFilePath;
    }

    /**
     * Gets font name
     * @return font name
     */
    public String getFontName() {
        return mFontName;
    }

    /**
     * Gets font pdftron name
     * @return font pdftron name
     */
    public String getPDFTronName() {
        return mPDFTronName;
    }

    /**
     * Whether the font has font name
     * @return true then it has font name, false otherwise
     */
    public Boolean hasFontName() {
        return !Utils.isNullOrEmpty(mFontName);
    }

    /**
     * Whether the font has font file path
     * @return true then it has font file path, false otherwise
     */
    public Boolean hasFilePath() {
        return !Utils.isNullOrEmpty(mFilePath);
    }

    /**
     * Whether the font has pdftron name
     * @return true then it has font pdftron name, false otherwise
     */
    public Boolean hasPDFTronName() {
        return !Utils.isNullOrEmpty(mPDFTronName);
    }
    /**
     * Whether the font is empty
     * @return true then it is empty, false otherwise
     */
    public boolean isEmpty() {
        return !hasFontName() && !hasFilePath() && !hasPDFTronName();
    }

    /**
     * Convert font to string
     * @return String of font Resource
     */
    @Override
    public String toString() {
        return "FontResource{" +
            "mDisplayName='" + mDisplayName + '\'' +
            ", mFontName='" + mFontName + '\'' +
            ", mFilePath='" + mFilePath + '\'' +
            ", mPDFTronName='" + mPDFTronName + '\'' +
            '}';
    }

    /**
     * Whether this font equals other font resource
     * @param obj other font resource
     * @return true then equals, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FontResource) {
            FontResource other = (FontResource) obj;
            if (other.hasPDFTronName() && hasPDFTronName()) {
                return other.getPDFTronName().equals(getPDFTronName());
            }
            if (other.hasFilePath() && hasFilePath()) {
                return other.getFilePath().equals(getFilePath());
            }
            if (other.hasFontName() && hasFontName()) {
                return other.getFontName().equals(getFontName());
            }
            if (other.isEmpty() && isEmpty()) {
                return true;
            }
        }
        return super.equals(obj);
    }

    public static String getFileName(String filePath) {
        // get font code from the file path
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);

        final int lastPeriodPos = fileName.lastIndexOf(".");
        if (lastPeriodPos > 0) {
            return fileName.substring(0, lastPeriodPos);
        } else {
            return fileName;
        }
    }

    /**
     * Gets white list fonts from given font info
     * @param fontInfo font info
     * @return white list fonts in string format
     */
    public static String whiteListFonts(String fontInfo) {
        try {
            JSONObject systemFontObject = new JSONObject(fontInfo);
            JSONArray systemFontArray = systemFontObject.getJSONArray(Tool.ANNOTATION_FREE_TEXT_JSON_FONT);

            for (int i = 0; i < systemFontArray.length(); i++) {
                JSONObject font = systemFontArray.getJSONObject(i);
                String displayName = font.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_DISPLAY_NAME);
                String fontFilePath = font.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_FILE_PATH);

                // if display name is null or empty, replace it will the file name
                if (displayName == null || displayName.equals("")) {
                    displayName = getFileName(font.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_FILE_PATH));
                    font.put(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_DISPLAY_NAME, displayName);
                }

                // put boolean into font info string: boolean represents whether
                // or not this font is white listed
                Boolean whiteListFont = whiteListFont(displayName, fontFilePath);
                font.put(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_DISPLAY_IN_LIST, whiteListFont);
            }

            fontInfo = systemFontObject.toString();
        } catch (Exception e) {

        }

        return fontInfo;
    }

    /**
     * Whehter given font name is a white list name
     * @param fontName font name
     * @param fontFilePath font file path
     * @return true then it is a white list name, false otherwise
     */
    public static boolean whiteListFont(String fontName, String fontFilePath) {
        // if the font name is a white list name, return true
        String[] whiteList = Tool.ANNOTATION_FREE_TEXT_WHITELIST_FONTS;
        int size = whiteList.length;
        for (String aWhiteList : whiteList) {
            if (fontName.contains(aWhiteList)) {
                return true;
            }
        }

        // if the font file path is not in the system directory, return true
        String dir1 = "/system/fonts";
        String dir2 = "/system/font";
        String dir3 = "/data/fonts";

        return !fontFilePath.contains(dir1) && !fontFilePath.contains(dir2) && !fontFilePath.contains(dir3);

    }

}
