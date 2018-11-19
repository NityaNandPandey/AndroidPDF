// http://www.annalytics.co.uk/android/pdf/2017/04/06/Save-PDF-From-An-Android-WebView/
package android.print;

import android.annotation.TargetApi;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;

import java.io.File;

/**
 * @hide
 */
@TargetApi(21)
public class PdfPrint {

    public interface PdfPrintListener {
        void onWriteFinished(String output);
        void onError();
    }

    private static final String TAG = PdfPrint.class.getSimpleName();
    private final PrintAttributes printAttributes;

    private PdfPrintListener mListener;

    public void setPdfPrintListener(PdfPrintListener listener) {
        mListener = listener;
    }

    public PdfPrint(PrintAttributes printAttributes) {
        this.printAttributes = printAttributes;
    }

    public void print(final PrintDocumentAdapter printAdapter, final File path, final String fileName) {
        printAdapter.onLayout(null, printAttributes, null, new PrintDocumentAdapter.LayoutResultCallback() {
            @Override
            public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                ParcelFileDescriptor fileDescriptor = getOutputFile(path, fileName);
                if (null == fileDescriptor) {
                    if (mListener != null) {
                        mListener.onError();
                    }
                    return;
                }
                printAdapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, fileDescriptor, new CancellationSignal(), new PrintDocumentAdapter.WriteResultCallback() {
                    @Override
                    public void onWriteFinished(PageRange[] pages) {
                        super.onWriteFinished(pages);
                        if (mListener != null) {
                            mListener.onWriteFinished((new File(path, fileName)).getAbsolutePath());
                        }
                    }
                });
            }
        }, null);
    }

    private ParcelFileDescriptor getOutputFile(File path, String fileName) {
        boolean success = true;
        if (!path.exists()) {
            success = path.mkdirs();
        }
        if (success) {
            File file = new File(path, fileName);
            try {
                success = file.createNewFile();
                if (success) {
                    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
                }
            } catch (Exception e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
        }
        return null;
    }
}
