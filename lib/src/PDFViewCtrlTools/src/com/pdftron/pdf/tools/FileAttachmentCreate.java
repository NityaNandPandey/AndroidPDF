package com.pdftron.pdf.tools;

import android.graphics.PointF;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.FileAttachment;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

@Keep
public class FileAttachmentCreate extends SimpleTapShapeCreate {

    private String mSelectedFilePath;

    /**
     * Class constructor
     *
     */
    public FileAttachmentCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    public boolean createFileAttachment(PointF targetPoint, int pageNum, String outputPath) {
        mSelectedFilePath = outputPath;

        return createAnnotation(targetPoint, pageNum);
    }

    @Override
    public void addAnnotation() {
        if (mPt2 == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(
                new Exception("target point is not specified."));
            return;
        }

        if (mPdfViewCtrl == null) {
            return;
        }

        setNextToolModeHelper();
        setCurrentDefaultToolModeHelper(getToolMode());

        ((ToolManager) (mPdfViewCtrl.getToolManager())).onAttachFileSelected(mPt2);
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.FILE_ATTACHMENT_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_FileAttachment;
    }

    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        if (Utils.isNullOrEmpty(mSelectedFilePath)) {
            return null;
        }
        FileAttachment fileAttachment = FileAttachment.create(doc, bbox, mSelectedFilePath);
        fileAttachment.setIcon(FileAttachment.e_Paperclip);
        return fileAttachment;
    }

    @Override
    public void clearTargetPoint() {
        resetPts();
    }
}
