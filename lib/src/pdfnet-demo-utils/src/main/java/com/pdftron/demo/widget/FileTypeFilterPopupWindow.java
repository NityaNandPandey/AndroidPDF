package com.pdftron.demo.widget;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.pdftron.demo.R;
import com.pdftron.pdf.utils.Constants;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;

import java.util.ArrayList;

public class FileTypeFilterPopupWindow extends PopupWindow {

    private Context mContext;
    private ArrayAdapter<String> mAdapter;
    private ListView mListView;
    private String mSettingsSuffix;
    private View mRootView;

    public interface FileTypeChangedListener {
        void filterTypeChanged(int index, boolean isOn);
    }
    private FileTypeChangedListener mFileTypeChangedListener;

    public void setFileTypeChangedListener(FileTypeChangedListener listener) {
        mFileTypeChangedListener = listener;
    }

    public FileTypeFilterPopupWindow(Context context, View anchor, String settingsSuffix) {
        //super(anchor, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        super(context);
        setFocusable(true);
        mContext = context;
        mSettingsSuffix = settingsSuffix;

        ArrayList<String> titleList = new ArrayList<>();

        titleList.add(context.getResources().getString(R.string.file_type_show_all));
        titleList.add(context.getResources().getString(R.string.file_type_pdf));
        titleList.add(context.getResources().getString(R.string.file_type_docx));
        titleList.add(context.getResources().getString(R.string.file_type_image));

        mRootView = LayoutInflater.from(context).inflate(R.layout.dialog_file_type_filter, null);

        mAdapter =  new ArrayAdapter<String>(mContext, R.layout.dialog_file_type_filter_item, R.id.file_type_text_view, titleList);
        mListView = (ListView) mRootView.findViewById(R.id.fragment_file_type_filter_list_view);//new ListView(mContext);
        this.setContentView(mRootView);
        mListView.setAdapter(mAdapter);
        mListView.getItemAtPosition(0);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    setItemChecked(0, true);
                    notifyDataSetChanged();
                    return;
                }
                boolean wasChecked = PdfViewCtrlSettingsManager.getFileFilter(view.getContext(), position - 1, mSettingsSuffix);
                boolean isChecked = !wasChecked;
                setItemChecked(position, isChecked);

                PdfViewCtrlSettingsManager.updateFileFilter(mContext, position - 1, mSettingsSuffix, isChecked);
                if (mFileTypeChangedListener != null) {
                    mFileTypeChangedListener.filterTypeChanged(position - 1, isChecked);
                }
                calculateAllFilesCheckedStatus();
                notifyDataSetChanged();
            }
        });


        setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

        int measuredWidth = measureContentWidth(mAdapter);

        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        mAdapter.getView(0, null, mListView).measure(widthMeasureSpec, heightMeasureSpec);
        int measuredShowAllWidth = mAdapter.getView(0, null, mListView).getMeasuredWidth();
        if (measuredShowAllWidth > measuredWidth) {
            measuredWidth = measuredShowAllWidth;
        }
        setWidth(measuredWidth + 100);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        if (PdfViewCtrlSettingsManager.getFileFilter(mContext,
                Constants.FILE_TYPE_PDF, mSettingsSuffix)) {
            mListView.setItemChecked(1, true);
        }

        if (PdfViewCtrlSettingsManager.getFileFilter(mContext,
                Constants.FILE_TYPE_DOC, mSettingsSuffix)) {
            mListView.setItemChecked(2, true);
        }

        if (PdfViewCtrlSettingsManager.getFileFilter(mContext,
                Constants.FILE_TYPE_IMAGE, mSettingsSuffix)) {
            mListView.setItemChecked(3, true);
        }

        calculateAllFilesCheckedStatus();

        Log.i("FILTER", "List view item at 0: " + mListView.getItemAtPosition(0).toString());
    }
    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
    }

    public void setItemChecked(int index, boolean check) {

        if (index == 0 && check) {
            for (int i = 1; i < mListView.getCount(); ++i) {
                mListView.setItemChecked(i, false);
                PdfViewCtrlSettingsManager.updateFileFilter(mRootView.getContext(), i - 1, mSettingsSuffix, false);
                if (mFileTypeChangedListener != null) {
                    mFileTypeChangedListener.filterTypeChanged(i - 1, false);
                }
            }
        }
        mListView.setItemChecked(index, check);
    }

    private int measureContentWidth(ListAdapter listAdapter) {
        ViewGroup mMeasureParent = null;
        int maxWidth = 0;
        View itemView = null;
        int itemType = 0;

        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int count = listAdapter.getCount();
        for (int i = 0; i < count; i++) {
            final int positionType = listAdapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }

            if (mMeasureParent == null) {
                mMeasureParent = new FrameLayout(mContext);
            }

            itemView = listAdapter.getView(i, itemView, mMeasureParent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);

            final int itemWidth = itemView.getMeasuredWidth();

            if (itemWidth > maxWidth) {
                maxWidth = itemWidth;
            }
        }

        return maxWidth;
    }

    private void calculateAllFilesCheckedStatus() {
        boolean areAllTurnedOff = true;
        if (PdfViewCtrlSettingsManager.getFileFilter(mContext,
                Constants.FILE_TYPE_PDF, mSettingsSuffix)) {
            areAllTurnedOff = false;
        }
        if (PdfViewCtrlSettingsManager.getFileFilter(mContext,
                Constants.FILE_TYPE_DOC, mSettingsSuffix)) {
            areAllTurnedOff = false;
        }
        if (PdfViewCtrlSettingsManager.getFileFilter(mContext,
                Constants.FILE_TYPE_IMAGE, mSettingsSuffix)) {
            areAllTurnedOff = false;
        }

        mListView.setItemChecked(0, areAllTurnedOff);
    }

    public boolean indexIsOn(int index){
        return mListView.isItemChecked(index);
    }
}
