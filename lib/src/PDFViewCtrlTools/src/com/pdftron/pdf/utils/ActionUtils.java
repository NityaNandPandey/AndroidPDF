//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.ActionParameter;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.R;
import com.pdftron.sdf.Obj;

/**
 * A utility class for annotation actions.
 */
public class ActionUtils {

    /**
     * A intercept callback for {@link #onInterceptExecuteAction(ActionParameter, PDFViewCtrl)}
     */
    public interface ActionInterceptCallback {
        /**
         * Called when {@link #onInterceptExecuteAction(ActionParameter, PDFViewCtrl)} is called
         * @param actionParam The action parameter
         * @param pdfViewCtrl The PDFViewCtrl
         * @return true then intercept {@link #onInterceptExecuteAction(ActionParameter, PDFViewCtrl)}, false otherwise
         */
        boolean onInterceptExecuteAction(ActionParameter actionParam, PDFViewCtrl pdfViewCtrl);
    }

    private static class LazzyHolder {
        static final ActionUtils INSTANCE = new ActionUtils();
    }

    public static ActionUtils getInstance() {
        return LazzyHolder.INSTANCE;
    }

    private  ActionInterceptCallback mActionCallback;

    /**
     * Sets {@link Action} intercept callback
     * @param callback ActionInterceptCallback
     */
    public void setActionInterceptCallback(ActionInterceptCallback callback) {
        mActionCallback = callback;
    }


    /**
     * Gets {@link} intercept callback
     * @return action intercept callback
     */
    public ActionInterceptCallback getActionInterceptCallback() {
        return mActionCallback;
    }

    /**
     * Executes an action on the PDF.
     * <p>
     * <div class="warning">
     * The PDF doc should have been locked when call this method.
     * In addition, ToolManager's raise annotation should be handled in the caller function.
     * </div>
     *
     * @param actionParam The action parameter
     * @param pdfViewCtrl The PDFViewCtrl
     */
    public void executeAction(ActionParameter actionParam, final PDFViewCtrl pdfViewCtrl) {
        try {
            if (getActionInterceptCallback() != null && getActionInterceptCallback().onInterceptExecuteAction(actionParam, pdfViewCtrl)) {
                return;
            }
            Action action = actionParam.getAction();
            int action_type = action.getType();
            if (action_type == Action.e_URI) {
                Obj o = action.getSDFObj();
                o = o.findObj("URI");
                if (o != null) {
                    String uri = o.getAsPDFText();
                    if (uri.startsWith("mailto:") || android.util.Patterns.EMAIL_ADDRESS.matcher(uri).matches()) {
                        if (uri.startsWith("mailto:")) {
                            uri = uri.substring(7);
                        }
                        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", uri, null));
                        pdfViewCtrl.getContext().startActivity(Intent.createChooser(i, pdfViewCtrl.getResources().getString(R.string.tools_misc_sendemail)));
                    } else {
                        // ACTION_VIEW needs the address to have http or https
                        if (!uri.startsWith("https://") && !uri.startsWith("http://")) {
                            uri = "http://" + uri;
                        }

                        final String finalUrl = uri;
                        // Ask the user if he/she really want to open the link in an external browser
                        AlertDialog.Builder builder = new AlertDialog.Builder(pdfViewCtrl.getContext());
                        builder.setTitle(R.string.tools_dialog_open_web_page_title)
                            .setMessage(String.format(pdfViewCtrl.getResources().getString(R.string.tools_dialog_open_web_page_message), finalUrl))
                            .setIcon(null)
                            .setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
                                    pdfViewCtrl.getContext().startActivity(Intent.createChooser(i, pdfViewCtrl.getResources().getString(R.string.tools_misc_openwith)));
                                }
                            })
                            .setNegativeButton(R.string.cancel, null);
                        builder.create().show();
                    }
                }
            } else {
                pdfViewCtrl.executeAction(actionParam);
            }
        } catch (PDFNetException e) {
            e.printStackTrace();
        }
    }
}
