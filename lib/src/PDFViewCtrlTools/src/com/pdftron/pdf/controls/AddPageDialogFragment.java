//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.asynctask.GeneratePagesTask;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;

/**
 * The AddPageDialogFragment is responsible for add new pages with various page options.
 * Use the {@link AddPageDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AddPageDialogFragment extends DialogFragment {

    /**
     * The type of page
     */
    public enum PageType {
        Blank,
        Lined,
        Grid,
        Graph,
        Music,
    }

    /**
     * The page size
     */
    public enum PageSize {
        Custom,
        Letter,
        Legal,
        A4,
        A3,
        Ledger,
    }

    /**
     * The page orientation
     */
    public enum PageOrientation {
        Portrait,
        Landscape,
    }

    /**
     * The page color
     */
    public enum PageColor {
        White,
        Yellow,
        Blueprint,
    }

    /**
     * The value of page color corresponds to {@link PageColor}
     */
    public static int[] PageColorValues = {
        0xFFFFFFFF,
        0xFFFFFF99,
        0xFF001484,
    };

    /**
     * The page color name corresponds to {@link PageColor}
     */
    public static int[] PageColorStrings = {
        R.string.dialog_add_page_color_white,
        R.string.dialog_add_page_color_yellow,
        R.string.dialog_add_page_color_blueprint,
    };

    private static final String ARG_CREATE_NEW_PDF = "create_new_pdf";
    private static final String ARG_CUSTOM_PAGE_WIDTH = "custom_page_width";
    private static final String ARG_CUSTOM_PAGE_HEIGHT = "custom_page_height";

    private float mDeviceScale;

    private PageSize mCurrentPageSize = PageSize.Letter;
    private PageType mCurrentPageType = PageType.Blank;
    private PageOrientation mCurrentPageOrientation;
    private PageColor mCurrentPageColor;

    private double mCustomPageWidth;
    private double mCustomPageHeight;

    private ViewPager mViewPager;
    private LinearLayout mViewPagerDotLayout;
    private ArrayList<LinearLayout> mViewPagerChildren;

    private OnAddNewPagesListener mOnAddNewPagesListener;
    private OnCreateNewDocumentListener mOnCreateNewDocumentListener;

    private boolean mShouldCreateNewPdf = false;

    private EditText mNumOfPagesEdit;
    private EditText mTitleEdit;

    public AddPageDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Returns a new instance of this class.
     * The creator must implement {@link OnCreateNewDocumentListener} interface by calling
     * {@link #setOnCreateNewDocumentListener(OnCreateNewDocumentListener)} to handle interaction events.
     * <p>
     * <Note>
     * Use this when the goal is to create a new document with new pages.If you want to add new
     * pages to the existing document call {@link #newInstance(double, double)}.
     * </Note>
     *
     * @return An instance of this class
     */
    public static AddPageDialogFragment newInstance() {
        AddPageDialogFragment fragment = new AddPageDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_CREATE_NEW_PDF, true);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Returns a new instance of this class
     * The creator must implement {@link OnAddNewPagesListener} interface by calling
     * {@link #setOnAddNewPagesListener(OnAddNewPagesListener)} to handle interaction events.
     * <p>
     * <Note>
     * Use this when the goal is to add pages to the existing document. If you want to create a
     * new document with new pages call {@link #newInstance()}.
     * </Note>
     *
     * @param lastPageWidth  The page width if {@link PageSize#Custom} is selected by the user.
     * @param lastPageHeight The page height if {@link PageSize#Custom} is selected by the user.
     * @return An instance of this class
     */
    public static AddPageDialogFragment newInstance(double lastPageWidth, double lastPageHeight) {
        AddPageDialogFragment fragment = new AddPageDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_CREATE_NEW_PDF, false);
        args.putDouble(ARG_CUSTOM_PAGE_WIDTH, lastPageWidth / 72);
        args.putDouble(ARG_CUSTOM_PAGE_HEIGHT, lastPageHeight / 72);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Sets the listener for creating a new document with new pages.
     *
     * @param listener The listener
     */
    public void setOnCreateNewDocumentListener(OnCreateNewDocumentListener listener) {
        mOnCreateNewDocumentListener = listener;
    }

    /**
     * Sets the listener for adding new pages to a new document.
     *
     * @param listener The listener
     */
    public void setOnAddNewPagesListener(OnAddNewPagesListener listener) {
        mOnAddNewPagesListener = listener;
    }

    /**
     * Sets the initial option for page type (see {@link PageType})
     */
    @SuppressWarnings("unused")
    public AddPageDialogFragment setInitialPageType(PageType initialPageType) {
        mCurrentPageType = initialPageType;
        return this;
    }

    /**
     * Sets the initial option for page size (see {@link PageSize})
     */
    @SuppressWarnings("unused")
    public AddPageDialogFragment setInitialPageSize(PageSize initialPageSize) {
        mCurrentPageSize = initialPageSize;
        return this;
    }

    /**
     * Sets the initial option for page color (see {@link PageSize})
     */
    @SuppressWarnings("unused")
    public AddPageDialogFragment setInitialPageColor(PageColor initialPageColor) {
        mCurrentPageColor = initialPageColor;
        return this;
    }

    /**
     * The overloaded implementation of {@link DialogFragment#onCreate(Bundle)}.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mShouldCreateNewPdf = args.getBoolean(ARG_CREATE_NEW_PDF);
            mCustomPageWidth = args.getDouble(ARG_CUSTOM_PAGE_WIDTH);
            mCustomPageHeight = args.getDouble(ARG_CUSTOM_PAGE_HEIGHT);
            mCurrentPageOrientation = mCustomPageWidth > mCustomPageHeight ? PageOrientation.Landscape : PageOrientation.Portrait;
            if (getContext() != null) {
                mDeviceScale = getContext().getResources().getDisplayMetrics().density;
            }
            mViewPagerChildren = new ArrayList<>();
        }
    }

    /**
     * The overloaded implementation of {@link DialogFragment#onCreateDialog(Bundle)}.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_add_page_dialog, null); //, container, false);
        builder.setView(view);

        builder.setPositiveButton(getActivity().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                generateDoc();
                dismiss();
            }
        });
        builder.setNegativeButton(getActivity().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        if (mShouldCreateNewPdf) {
            TextView title = view.findViewById(R.id.addpagedialog_title);
            title.setText(R.string.dialog_add_page_title_newdoc);
        }

        mViewPagerDotLayout = view.findViewById(R.id.dot_layout);
        initViewPagerDotLayout();

        mViewPager = view.findViewById(R.id.page_type_view_pager);
        mViewPager.setAdapter(new PageTypePagerAdapter());
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                changeDotPosition(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        if (Utils.isRtlLayout(getContext()))
            mViewPager.setCurrentItem(mViewPager.getAdapter().getCount() - 1);

        mTitleEdit = view.findViewById(R.id.add_page_document_title_input);
        mNumOfPagesEdit = view.findViewById(R.id.addpagedialog_numpages_edit);
        TextView titleEditLabel = view.findViewById(R.id.addpagedialog_doctitle_label);

        if (!mShouldCreateNewPdf) {
            mTitleEdit.setVisibility(View.GONE);
            titleEditLabel.setVisibility(View.GONE);
        }

        Spinner pageSizeSpinner = view.findViewById(R.id.pageSize_spinner);
        pageSizeSpinner.setAdapter(getPageSizeSpinnerAdapter());
        pageSizeSpinner.setSelection(mCurrentPageSize.ordinal() - (mShouldCreateNewPdf ? 1 : 0));
        pageSizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentPageSize = PageSize.values()[position + (mShouldCreateNewPdf ? 1 : 0)];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        Spinner pageOrientationSpinner = view.findViewById(R.id.pageOrientation_spinner);
        pageOrientationSpinner.setAdapter(getOrientationSpinnerAdapter());
        pageOrientationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentPageOrientation = PageOrientation.values()[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mCurrentPageColor = PageColor.White;
        Spinner pageColorSpinner = view.findViewById(R.id.pageColor_spinner);
        pageColorSpinner.setAdapter(getPageColorSpinnerAdapter());
        pageColorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentPageColor = PageColor.values()[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        pageColorSpinner.setSelection(0);

        return builder.create();
    }

    private void initViewPagerDotLayout() {
        if ((int) Math.ceil(PageType.values().length / 3.0) < 2)
            return;

        int initEnabled = 0;
        if (Utils.isRtlLayout(getContext()))
            initEnabled = (int) Math.ceil(PageType.values().length / 3.0) - 1;

        for (int i = 0; i < (int) Math.ceil(PageType.values().length / 3.0); i++) {
            ImageView dot = new ImageView(getContext());
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams((int) (8 * mDeviceScale + 0.5f), (int) (8 * mDeviceScale + 0.5f));
            int margin = (int) (5 * mDeviceScale + 0.5f);
            dotParams.setMargins(margin, margin, margin, margin);
            dot.setLayoutParams(dotParams);
            dot.setImageDrawable(getResources().getDrawable(R.drawable.viewpager_point));
            dot.setEnabled(i == initEnabled);
            mViewPagerDotLayout.addView(dot);
        }
    }

    private void changeDotPosition(int position) {
        if ((int) Math.ceil(PageType.values().length / 3.0) < 2)
            return;

        if (Utils.isRtlLayout(getContext()))
            position = mViewPager.getAdapter().getCount() - position - 1;

        for (int i = 0; i < (int) Math.ceil(PageType.values().length / 3.0); i++) {
            ImageView dot = (ImageView) mViewPagerDotLayout.getChildAt(i);
            dot.setEnabled(i == position);
        }
    }

    private void setActiveMode(PageType type) {
        mCurrentPageType = type;
        for (PageType pType : PageType.values()) {
            LinearLayout btnLayout = mViewPagerChildren.get(pType.ordinal());
            ImageView imgView = (ImageView) btnLayout.getChildAt(0);
            switch (pType) {
                case Blank:
                    imgView.setImageDrawable(getResources().getDrawable((type == pType) ? R.drawable.blankpage_selected : R.drawable.blankpage_regular));
                    break;
                case Lined:
                    imgView.setImageDrawable(getResources().getDrawable((type == pType) ? R.drawable.linedpage_selected : R.drawable.linedpage_regular));
                    break;
                case Graph:
                    imgView.setImageDrawable(getResources().getDrawable((type == pType) ? R.drawable.graphpage2_selected : R.drawable.graphpage2_regular));
                    break;
                case Grid:
                    imgView.setImageDrawable(getResources().getDrawable((type == pType) ? R.drawable.graphpage_selected : R.drawable.graphpage_regular));
                    break;
                case Music:
                    imgView.setImageDrawable(getResources().getDrawable((type == pType) ? R.drawable.musicpage_selected : R.drawable.musicpage_regular));
                    break;
            }
        }
    }

    private ArrayAdapter<CharSequence> getPageColorSpinnerAdapter() {
        Context context = getContext();
        if (context == null) {
            return null;
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context, R.layout.simple_image_spinner_item, android.R.id.text1) {

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view;
                if (convertView != null) {
                    view = convertView;
                } else {
                    view = LayoutInflater.from(getContext()).inflate(R.layout.simple_image_spinner_item, parent, false);
                }

                ImageView preview = view.findViewById(R.id.spinner_image);
                ((GradientDrawable) preview.getBackground()).setColor(0xFF000000);
                GradientDrawable bgShape = (GradientDrawable) preview.getDrawable();
                bgShape.setColor(PageColorValues[position]);

                TextView textView = view.findViewById(android.R.id.text1);
                textView.setText(getString(PageColorStrings[position]));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view;
                if (convertView != null) {
                    view = convertView;
                } else {
                    LayoutInflater lInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    if (lInflater == null) {
                        lInflater = LayoutInflater.from(getContext());
                    }
                    view = lInflater.inflate(R.layout.simple_image_spinner_dropdown_item, parent, false);
                }

                ImageView preview = view.findViewById(R.id.spinner_image);
                ((GradientDrawable) preview.getBackground()).setColor(0xFF000000);
                GradientDrawable bgShape = (GradientDrawable) preview.getDrawable();
                bgShape.setColor(PageColorValues[position]);

                TextView textView = view.findViewById(android.R.id.text1);
                textView.setText(getString(PageColorStrings[position]));
                return view;
            }
        };
        for (int i = 0, cnt = PageColor.values().length; i < cnt; i++) {
            adapter.add(getString(PageColorStrings[i]));
        }
        adapter.setDropDownViewResource(R.layout.simple_image_spinner_dropdown_item);

        return adapter;
    }

    private ArrayAdapter<CharSequence> getPageSizeSpinnerAdapter() {
        Context context = getContext();
        if (context == null) {
            return null;
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
        for (PageSize size : PageSize.values()) {
            switch (size) {
                case Letter:
                    adapter.add(getString(R.string.dialog_add_page_page_size_letter));
                    break;
                case Legal:
                    adapter.add(getString(R.string.dialog_add_page_page_size_legal));
                    break;
                case Ledger:
                    adapter.add(getString(R.string.dialog_add_page_page_size_ledger));
                    break;
                case Custom:
                    if (!mShouldCreateNewPdf)
                        adapter.add(getString(R.string.dialog_add_page_page_size_custom,
                            (int) (mCustomPageWidth * 72), (int) (mCustomPageHeight * 72)));
                    break;
                case A4:
                    adapter.add(getString(R.string.dialog_add_page_page_size_a4));
                    break;
                case A3:
                    adapter.add(getString(R.string.dialog_add_page_page_size_a3));
                    break;
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private ArrayAdapter<CharSequence> getOrientationSpinnerAdapter() {
        Context context = getContext();
        if (context == null) {
            return null;
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
        for (PageOrientation size : PageOrientation.values()) {
            switch (size) {
                case Portrait:
                    adapter.add(getString(R.string.dialog_add_page_orientation_portrait));
                    break;
                case Landscape:
                    adapter.add(getString(R.string.dialog_add_page_orientation_landscape));
                    break;
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void generateDoc() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        int numOfPages = 1;
        try {
            if (!Utils.isNullOrEmpty(mNumOfPagesEdit.getText().toString())) {
                numOfPages = Integer.parseInt(mNumOfPagesEdit.getText().toString());
                if (numOfPages < 1 || numOfPages > 1000)
                    numOfPages = 1;
            }
        } catch (NumberFormatException e) {
            numOfPages = 1;
        }

        String title = getString(R.string.empty_title);
        if (mShouldCreateNewPdf) {
            if (!Utils.isNullOrEmpty(mTitleEdit.getText().toString()))
                title = mTitleEdit.getText().toString();
        }

        new GeneratePagesTask(context, numOfPages, title, mCurrentPageSize,
            mCurrentPageOrientation, mCurrentPageColor, mCurrentPageType, mCustomPageWidth,
            mCustomPageHeight, mShouldCreateNewPdf, mOnCreateNewDocumentListener, mOnAddNewPagesListener)
            .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * The overloaded implementation of {@link DialogFragment#onDetach()}.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mOnCreateNewDocumentListener = null;
        mOnAddNewPagesListener = null;
    }

    private class PageTypePagerAdapter extends PagerAdapter {

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup collection, int position) {
            LinearLayout layout = new LinearLayout(getContext());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
            layout.setLayoutParams(layoutParams);
            layout.setGravity(Gravity.CENTER);
            layout.setOrientation(LinearLayout.HORIZONTAL);

            if (Utils.isRtlLayout(getContext())) {
                position = getCount() - position - 1;
            }

            for (int i = position * 3, cnt = PageType.values().length; i < (position * 3 + 3) && i < cnt; i++) {
                PageType type = PageType.values()[i];
                LinearLayout btnLayout = new LinearLayout(getContext());
                LinearLayout.LayoutParams btnLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                btnLayoutParams.setMargins((int) (10 * mDeviceScale + 0.5f), 0, (int) (10 * mDeviceScale + 0.5f), (int) (10 * mDeviceScale + 0.5f));
                btnLayout.setLayoutParams(btnLayoutParams);
                btnLayout.setGravity(Gravity.CENTER);
                btnLayout.setOrientation(LinearLayout.VERTICAL);
                btnLayout.setTag(i);
                btnLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setActiveMode(PageType.values()[(Integer) v.getTag()]);
                    }
                });
                layout.addView(btnLayout);

                mViewPagerChildren.add(btnLayout);

                ImageView btnImage = new ImageView(getContext());
                LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, (int) (100 * mDeviceScale + 0.5f));
                imgParams.setMargins(0, (int) (5 * mDeviceScale + 0.5f), 0, 0);
                btnImage.setLayoutParams(imgParams);
                btnImage.setAdjustViewBounds(true);
                btnLayout.addView(btnImage);

                TextView btnTextView = new TextView(getContext());
                btnTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                btnLayout.addView(btnTextView);
                switch (type) {
                    case Blank:
                        btnImage.setImageDrawable(getResources().getDrawable(type == mCurrentPageType ? R.drawable.blankpage_selected : R.drawable.blankpage_regular));
                        btnTextView.setText(R.string.dialog_add_page_blank);
                        break;
                    case Lined:
                        btnImage.setImageDrawable(getResources().getDrawable(type == mCurrentPageType ? R.drawable.linedpage_selected : R.drawable.linedpage_regular));
                        btnTextView.setText(R.string.dialog_add_page_lined);
                        break;
                    case Graph:
                        btnImage.setImageDrawable(getResources().getDrawable(type == mCurrentPageType ? R.drawable.graphpage2_selected : R.drawable.graphpage2_regular));
                        btnTextView.setText(R.string.dialog_add_page_graph);
                        break;
                    case Grid:
                        btnImage.setImageDrawable(getResources().getDrawable(type == mCurrentPageType ? R.drawable.graphpage_selected : R.drawable.graphpage_regular));
                        btnTextView.setText(R.string.dialog_add_page_grid);
                        break;
                    case Music:
                        btnImage.setImageDrawable(getResources().getDrawable(type == mCurrentPageType ? R.drawable.musicpage_selected : R.drawable.musicpage_regular));
                        btnTextView.setText(R.string.dialog_add_page_music);
                        break;
                }
            }
            collection.addView(layout);

            return layout;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup collection, int position, @NonNull Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return (int) Math.ceil(PageType.values().length / 3.0);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }

    /**
     * Callback interface invoked when new pages should be added to the current document.
     */
    public interface OnAddNewPagesListener {
        /**
         * The implementation should add the specified pages to the current PDF document.
         *
         * @param pages The pages
         */
        void onAddNewPages(Page[] pages);
    }

    /**
     * Callback interface invoked when new document should be created.
     */
    public interface OnCreateNewDocumentListener {
        /**
         * The implementation should create a new PDF document.
         *
         * @param doc   The PDF doc
         * @param title The title of the PDF doc
         */
        void onCreateNewDocument(PDFDoc doc, String title);
    }

}
