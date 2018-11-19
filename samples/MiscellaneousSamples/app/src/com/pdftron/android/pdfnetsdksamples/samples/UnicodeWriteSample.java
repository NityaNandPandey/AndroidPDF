//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples.samples;

import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementBuilder;
import com.pdftron.pdf.ElementWriter;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.sdf.SDFDoc;

import com.pdftron.android.pdfnetsdksamples.OutputListener;
import com.pdftron.android.pdfnetsdksamples.PDFNetSample;
import com.pdftron.android.pdfnetsdksamples.R;
import com.pdftron.android.pdfnetsdksamples.util.Utils;

/**
 * This example illustrates how to create Unicode text and how to embed
 * composite fonts.
 * 
 * Note: This sample assumes that your device contains some of the regular
 * fonts distributed with the Android SDK. Since not all fonts are shipped
 * depending on the manufacturer, you may need to change the sample code
 * or add a font that covers the text you want to use.
 * 
 * In case some of the text used in this sample does not work properly
 * (squared or dot characters appear instead of the real characters) you can
 * search for the correct fonts in the Android SDK or download and use the
 * Cyberbit font, available here:
 * http://ftp.netscape.com/pub/communicator/extras/fonts/windows/
 * 
 * Add the font file to the assets\TestFiles folder and change the code
 * accordingly.
 */
public class UnicodeWriteSample extends PDFNetSample {

    public UnicodeWriteSample() {
        setTitle(R.string.sample_unicodewrite_title);
        setDescription(R.string.sample_unicodewrite_description);
    }

    @Override
    public void run(OutputListener outputListener) {
        super.run(outputListener);
        printHeader(outputListener);
        
        try {
            PDFDoc doc=new PDFDoc();

            ElementBuilder eb=new ElementBuilder();
            ElementWriter writer=new ElementWriter();

            // Start a new page ------------------------------------
            Page page = doc.pageCreate(new Rect(0, 0, 612, 794));

            writer.begin(page); // begin writing to this page

            Font fnt;
            try {
                // Embed and subset the font. Uncomment the line bellow to use the Cyberbit font.
                //fnt = Font.createCIDTrueTypeFont(doc, Utils.getAssetInputStream(INPUT_PATH + "Cyberbit.ttf"), true, true);
                
                // Let's try to find the DroidSans font available in the system
                fnt = Font.create(doc, "DroidSans", "");
            } catch (Exception e) {
                outputListener.println("Note: 'Cyberbit.ttf' font file was not found.");
                outputListener.println(e.getStackTrace());
                return;
            }
            
            Element element = eb.createTextBegin(fnt, 1);
            element.setTextMatrix(10, 0, 0, 10, 50, 600);
            element.getGState().setLeading(2);       // Set the spacing between lines
            writer.writeElement(element);

            // Hello World!
            char hello[] = { 'H','e','l','l','o',' ','W','o','r','l','d','!'};
            writer.writeElement(eb.createUnicodeTextRun(new String(hello)));
            writer.writeElement(eb.createTextNewLine());

            // Latin
            char latin[] = {
                'a', 'A', 'b', 'B', 'c', 'C', 'd', 'D', 0x45, 0x0046, 0x00C0, 
                0x00C1, 0x00C2, 0x0143, 0x0144, 0x0145, 0x0152, '1', '2' // etc.
            };
            writer.writeElement(eb.createUnicodeTextRun(new String(latin)));
            writer.writeElement(eb.createTextNewLine());

            // Greek
            char greek[] = {
                0x039E, 0x039F, 0x03A0, 0x03A1,0x03A3, 0x03A6, 0x03A8, 0x03A9  // etc.
            };
            writer.writeElement(eb.createUnicodeTextRun(new String(greek)));
            writer.writeElement(eb.createTextNewLine());
            
            // Cyrillic
            char cyrilic[] = {
                0x0409, 0x040A, 0x040B, 0x040C, 0x040E, 0x040F, 0x0410, 0x0411,
                0x0412, 0x0413, 0x0414, 0x0415, 0x0416, 0x0417, 0x0418, 0x0419 // etc.
            };
            writer.writeElement(eb.createUnicodeTextRun(new String(cyrilic)));
            writer.writeElement(eb.createTextNewLine());

            writer.writeElement(eb.createTextEnd());
            
            fnt = Font.create(doc, "DroidSansHebrew-Regular", "");
            element = eb.createTextBegin(fnt, 1);
            element.setTextMatrix(10, 0, 0, 10, 50, 500);
            writer.writeElement(element); 
            
            // Hebrew
            char hebrew[] = {
                0x05D0, 0x05D1, 0x05D3, 0x05D3, 0x05D4, 0x05D5, 0x05D6, 0x05D7, 0x05D8, 
                0x05D9, 0x05DA, 0x05DB, 0x05DC, 0x05DD, 0x05DE, 0x05DF, 0x05E0, 0x05E1 // etc. 
            };
            writer.writeElement(eb.createUnicodeTextRun(new String(hebrew)));
            writer.writeElement(eb.createTextNewLine());

            writer.writeElement(eb.createTextEnd());
            
            /*
            fnt = Font.create(doc, "DroidSansArabic", "");
            element = eb.createTextBegin(fnt, 1);
            element.setTextMatrix(10, 0, 0, 10, 50, 450);
            writer.writeElement(element); 
            
            // Arabic
            char arabic[] = {
                0x0624, 0x0625, 0x0626, 0x0627, 0x0628, 0x0629, 0x062A, 0x062B, 0x062C, 
                0x062D, 0x062E, 0x062F, 0x0630, 0x0631, 0x0632, 0x0633, 0x0634, 0x0635 // etc. 
            };
            writer.writeElement(eb.createUnicodeTextRun(new String(arabic)));
            writer.writeElement(eb.createTextNewLine());

            writer.writeElement(eb.createTextEnd());
            */
            
            fnt = Font.create(doc, "DroidSansThai", "");
            element = eb.createTextBegin(fnt, 1);
            element.setTextMatrix(10, 0, 0, 10, 50, 400);
            writer.writeElement(element);
            
            // Thai 
            char thai[] = {
                0x0E01, 0x0E02, 0x0E03, 0x0E04, 0x0E05, 0x0E06, 0x0E07, 0x0E08, 0x0E09, 
                0x0E0A, 0x0E0B, 0x0E0C, 0x0E0D, 0x0E0E, 0x0E0F, 0x0E10, 0x0E11, 0x0E12 // etc. 
            };
            writer.writeElement(eb.createUnicodeTextRun(new String(thai)));
            writer.writeElement(eb.createTextNewLine());

            writer.writeElement(eb.createTextEnd());
            
            fnt = Font.create(doc, "DroidSansFallback", "");
            element = eb.createTextBegin(fnt, 1);
            element.setTextMatrix(10, 0, 0, 10, 50, 350);
            writer.writeElement(element);
            
            // Hiragana - Japanese 
            char hiragana[] = {
                0x3041, 0x3042, 0x3043, 0x3044, 0x3045, 0x3046, 0x3047, 0x3048, 0x3049, 
                0x304A, 0x304B, 0x304C, 0x304D, 0x304E, 0x304F, 0x3051, 0x3051, 0x3052 // etc. 
            };
            writer.writeElement(eb.createUnicodeTextRun(new String(hiragana)));
            writer.writeElement(eb.createTextNewLine());
            
            // CJK Unified Ideographs
            char cjk_uni[] = {
                0x5841, 0x5842, 0x5843, 0x5844, 0x5845, 0x5846, 0x5847, 0x5848, 0x5849, 
                0x584A, 0x584B, 0x584C, 0x584D, 0x584E, 0x584F, 0x5850, 0x5851, 0x5852 // etc. 
            };
            writer.writeElement(eb.createUnicodeTextRun(new String(cjk_uni)));
            writer.writeElement(eb.createTextNewLine());

            // Finish the block of text
            writer.writeElement(eb.createTextEnd());

            writer.end();  // save changes to the current page
            doc.pagePushBack(page);

            doc.save(Utils.createExternalFile("unicodewrite.pdf").getAbsolutePath(), SDFDoc.e_remove_unused | SDFDoc.e_hex_strings , null);
            addToFileList("unicodewrite.pdf");
            doc.close();
            outputListener.println("Done. Result saved in unicodewrite.pdf...");
        } catch (Exception e) {
            outputListener.println(e.getStackTrace());
        }
        
        printFooter(outputListener);
    }

}
