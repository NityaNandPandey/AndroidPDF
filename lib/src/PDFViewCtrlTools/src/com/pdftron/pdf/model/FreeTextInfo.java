//------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.model;

import android.content.Context;
import android.content.SharedPreferences;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.sdf.Obj;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.pdftron.pdf.tools.Tool;

public class FreeTextInfo {
    public int mAnnotIndex;
    public int mPageIndex;
    public String mFontName;

    public FreeTextInfo(int mAnnotIndex, int mPageIndex, String mFontName) {
        this.mAnnotIndex = mAnnotIndex;
        this.mPageIndex = mPageIndex;
        this.mFontName = mFontName;
    }


    public void setFont(Context context, PDFViewCtrl pdfViewCtrl) throws PDFNetException, JSONException {
        Annot annot = pdfViewCtrl.getDoc().getPage(mPageIndex).getAnnot(mAnnotIndex);
        if (annot != null) {
            FreeText freeText = new FreeText(annot);
            setFont(pdfViewCtrl, freeText, mFontName);
            freeText.refreshAppearance();
            pdfViewCtrl.update(freeText, mPageIndex);
        }
    }

    public static void setFont(PDFViewCtrl pdfViewCtrl, FreeText freeText, String pdfFontName) throws JSONException, PDFNetException {
        // Set Font
        // system will automatically set it to the default if font
        // is not embedded.
        if (pdfFontName != null && !pdfFontName.equals("")) {
            String fontDRName = "F0";

            // Create a DR entry for embedding the font
            Obj annotObj = freeText.getSDFObj();
            Obj drDict = annotObj.putDict("DR");

            // Embed the font
            Obj fontDict = drDict.putDict("Font");
            Font font = Font.create(pdfViewCtrl.getDoc(), pdfFontName, freeText.getContents());
            fontDict.put(fontDRName, font.GetSDFObj());
            String fontName = font.getName();

            // Set DA string
            String DA = freeText.getDefaultAppearance();
            int slashPosition = DA.indexOf("/", 0);

            // if DR string contains '/' which it always should.
            if (slashPosition > 0) {
                String beforeSlash = DA.substring(0, slashPosition);
                String afterSlash = DA.substring(slashPosition);
                String afterFont = afterSlash.substring(afterSlash.indexOf(" "));
                String updatedDA = beforeSlash + "/" + fontDRName + afterFont;

                freeText.setDefaultAppearance(updatedDA);
                freeText.refreshAppearance();
            }

            // save font name with font if not saved already
            SharedPreferences settings = Tool.getToolPreferences(pdfViewCtrl.getContext());
            String fontInfo = settings.getString(Tool.ANNOTATION_FREE_TEXT_FONTS, "");
            if (!fontInfo.equals("")) {
                JSONObject systemFontObject = new JSONObject(fontInfo);
                JSONArray systemFontArray = systemFontObject.getJSONArray(Tool.ANNOTATION_FREE_TEXT_JSON_FONT);

                for (int i = 0; i < systemFontArray.length(); i++) {
                    JSONObject fontObj = systemFontArray.getJSONObject(i);
                    // if has the same file name as the selected font, save the font name
                    if (fontObj.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_PDFTRON_NAME).equals(pdfFontName)) {
                        fontObj.put(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_NAME, fontName);
                        break;
                    }
                }
                fontInfo = systemFontObject.toString();
            }

            SharedPreferences.Editor editor = settings.edit();
            editor.putString(Tool.ANNOTATION_FREE_TEXT_FONTS, fontInfo);
            editor.putString(Tool.getFontKey(Annot.e_FreeText), pdfFontName);
            editor.apply();
        }
    }

}
