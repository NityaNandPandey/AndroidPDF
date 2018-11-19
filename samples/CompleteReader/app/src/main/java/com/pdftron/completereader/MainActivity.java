package com.pdftron.completereader;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.pdftron.common.PDFNetException;
import com.pdftron.demo.app.AdvancedReaderActivity;
import com.pdftron.demo.app.SimpleReaderActivity;
import com.pdftron.pdf.PDFNet;
import com.pdftron.pdf.config.PDFViewCtrlConfig;
import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.utils.AppUtils;
import com.pdftron.pdf.utils.Utils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Utils.isLollipop()) {
            getWindow().setStatusBarColor(Color.BLACK);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("PDFNet Demo");
        }

        try {
            PDFNet.initialize(this, R.raw.pdfnet, AppUtils.getLicenseKey(getApplicationContext()));
        } catch (PDFNetException e) {
            showLicenseRequestDialog();
        }

        Button simpleReaderButton = findViewById(R.id.simpleReaderButton);
        simpleReaderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSimpleReaderActivity();
            }
        });

        ImageView simpleReaderImage = findViewById(R.id.simpleReaderImage);
        simpleReaderImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSimpleReaderActivity();
            }
        });

        Button completeReaderButton = findViewById(R.id.completeReaderButton);
        completeReaderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCompleteReaderActivity();
            }
        });

        ImageView completeReaderImage = findViewById(R.id.completeReaderImage);
        completeReaderImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCompleteReaderActivity();
            }
        });
    }

    private void showLicenseRequestDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(R.string.missing_license_key)
            .setMessage(Html.fromHtml(getString(R.string.missing_license_key_msg)))
            .setCancelable(false)
            .create();
        dialog.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void openCompleteReaderActivity() {
        AdvancedReaderActivity.setDebug(BuildConfig.DEBUG);
        AdvancedReaderActivity.open(this);
    }

    private void openSimpleReaderActivity() {
        ViewerConfig.Builder builder = new ViewerConfig.Builder();
        ViewerConfig config = builder
            .fullscreenModeEnabled(true)
            .multiTabEnabled(true)
            .documentEditingEnabled(true)
            .longPressQuickMenuEnabled(true)
            .showPageNumberIndicator(true)
            .showBottomNavBar(true)
            .showThumbnailView(true)
            .showBookmarksView(true)
            .showSearchView(true)
            .showShareOption(true)
            .showDocumentSettingsOption(true)
            .showAnnotationToolbarOption(true)
            .showOpenFileOption(true)
            .showOpenUrlOption(true)
            .showEditPagesOption(true)
            .showPrintOption(true)
            .showCloseTabOption(true)
            .showAnnotationsList(true)
            .showOutlineList(true)
            .showUserBookmarksList(true)
            .pdfViewCtrlConfig(PDFViewCtrlConfig.getDefaultConfig(this))
            .build();
        SimpleReaderActivity.open(this, config);
    }
}
