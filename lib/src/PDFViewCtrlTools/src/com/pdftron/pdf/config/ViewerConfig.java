package com.pdftron.pdf.config;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StyleRes;

/**
 * This class is responsible for configuration
 * {@link com.pdftron.pdf.controls.PdfViewCtrlTabHostFragment} and
 * {@link com.pdftron.pdf.controls.PdfViewCtrlTabFragment}.
 * See {@link Builder} for details.
 */
public class ViewerConfig implements Parcelable {

    /** @hide */
    public boolean isFullscreenModeEnabled() {
        return fullscreenModeEnabled;
    }

    /** @hide */
    public boolean isMultiTabEnabled() {
        return multiTabEnabled;
    }

    /** @hide */
    public boolean isDocumentEditingEnabled() {
        return documentEditingEnabled;
    }

    /** @hide */
    public boolean isLongPressQuickMenuEnabled() {
        return longPressQuickMenuEnabled;
    }

    /** @hide */
    public boolean isShowPageNumberIndicator() {
        return showPageNumberIndicator;
    }

    /** @hide */
    public boolean isShowBottomNavBar() {
        return showBottomNavBar;
    }

    /** @hide */
    public boolean isShowThumbnailView() {
        return showThumbnailView;
    }

    /** @hide */
    public boolean isShowBookmarksView() {
        return showBookmarksView;
    }

    /** @hide */
    public String getToolbarTitle() {
        return toolbarTitle;
    }

    /** @hide */
    public boolean isShowSearchView() {
        return showSearchView;
    }

    /** @hide */
    public boolean isShowShareOption() {
        return showShareOption;
    }

    /** @hide */
    public boolean isShowDocumentSettingsOption() {
        return showDocumentSettingsOption;
    }

    /** @hide */
    public boolean isShowAnnotationToolbarOption() {
        return showAnnotationToolbarOption;
    }

    /** @hide */
    public boolean isShowOpenFileOption() {
        return showOpenFileOption;
    }

    /** @hide */
    public boolean isShowOpenUrlOption() {
        return showOpenUrlOption;
    }

    /** @hide */
    public boolean isShowEditPagesOption() {
        return showEditPagesOption;
    }

    /** @hide */
    public boolean isShowPrintOption() {
        return showPrintOption;
    }

    /** @hide */
    public boolean isShowCloseTabOption() {
        return showCloseTabOption;
    }

    /** @hide */
    public boolean isShowAnnotationsList() {
        return showAnnotationsList;
    }

    /** @hide */
    public boolean isShowOutlineList() {
        return showOutlineList;
    }

    /** @hide */
    public boolean isShowUserBookmarksList() {
        return showUserBookmarksList;
    }

    /** @hide */
    public boolean isRightToLeftModeEnabled() {
        return rightToLeftModeEnabled;
    }

    /** @hide */
    public boolean isShowRightToLeftOption() {
        return showRightToLeftOption;
    }

    /** @hide */
    public PDFViewCtrlConfig getPdfViewCtrlConfig() {
        return pdfViewCtrlConfig;
    }

    /** @hide */
    public int getToolManagerBuilderStyleRes() {
        return toolManagerBuilderStyleRes;
    }

    /** @hide */
    public ToolManagerBuilder getToolManagerBuilder() {
        return toolManagerBuilder;
    }

    private boolean fullscreenModeEnabled = true;
    private boolean multiTabEnabled = true;
    private boolean documentEditingEnabled = true;
    private boolean longPressQuickMenuEnabled = true;
    private boolean showPageNumberIndicator = true;
    private boolean showBottomNavBar = true;
    private boolean showThumbnailView = true;
    private boolean showBookmarksView = true;
    private String toolbarTitle;
    private boolean showSearchView = true;
    private boolean showShareOption = true;
    private boolean showDocumentSettingsOption = true;
    private boolean showAnnotationToolbarOption = true;
    private boolean showOpenFileOption = true;
    private boolean showOpenUrlOption = true;
    private boolean showEditPagesOption = true;
    private boolean showPrintOption = true;
    private boolean showCloseTabOption = true;
    private boolean showAnnotationsList = true;
    private boolean showOutlineList = true;
    private boolean showUserBookmarksList = true;
    private boolean rightToLeftModeEnabled = false;
    private boolean showRightToLeftOption = false;
    private PDFViewCtrlConfig pdfViewCtrlConfig;
    private int toolManagerBuilderStyleRes = 0;
    private ToolManagerBuilder toolManagerBuilder;

    public ViewerConfig() {
    }

    protected ViewerConfig(Parcel in) {
        fullscreenModeEnabled = in.readByte() != 0;
        multiTabEnabled = in.readByte() != 0;
        documentEditingEnabled = in.readByte() != 0;
        longPressQuickMenuEnabled = in.readByte() != 0;
        showPageNumberIndicator = in.readByte() != 0;
        showBottomNavBar = in.readByte() != 0;
        showThumbnailView = in.readByte() != 0;
        showBookmarksView = in.readByte() != 0;
        toolbarTitle = in.readString();
        showSearchView = in.readByte() != 0;
        showShareOption = in.readByte() != 0;
        showDocumentSettingsOption = in.readByte() != 0;
        showAnnotationToolbarOption = in.readByte() != 0;
        showOpenFileOption = in.readByte() != 0;
        showOpenUrlOption = in.readByte() != 0;
        showEditPagesOption = in.readByte() != 0;
        showPrintOption = in.readByte() != 0;
        showCloseTabOption = in.readByte() != 0;
        showAnnotationsList = in.readByte() != 0;
        showOutlineList = in.readByte() != 0;
        showUserBookmarksList = in.readByte() != 0;
        rightToLeftModeEnabled = in.readByte() != 0;
        showRightToLeftOption = in.readByte() != 0;
        pdfViewCtrlConfig = in.readParcelable(PDFViewCtrlConfig.class.getClassLoader());
        toolManagerBuilderStyleRes = in.readInt();
        toolManagerBuilder = in.readParcelable(ToolManagerBuilder.class.getClassLoader());
    }

    public static final Creator<ViewerConfig> CREATOR = new Creator<ViewerConfig>() {
        @Override
        public ViewerConfig createFromParcel(Parcel in) {
            return new ViewerConfig(in);
        }

        @Override
        public ViewerConfig[] newArray(int size) {
            return new ViewerConfig[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte) (fullscreenModeEnabled ? 1 : 0));
        parcel.writeByte((byte) (multiTabEnabled ? 1 : 0));
        parcel.writeByte((byte) (documentEditingEnabled ? 1 : 0));
        parcel.writeByte((byte) (longPressQuickMenuEnabled ? 1 : 0));
        parcel.writeByte((byte) (showPageNumberIndicator ? 1 : 0));
        parcel.writeByte((byte) (showBottomNavBar ? 1 : 0));
        parcel.writeByte((byte) (showThumbnailView ? 1 : 0));
        parcel.writeByte((byte) (showBookmarksView ? 1 : 0));
        parcel.writeString(toolbarTitle);
        parcel.writeByte((byte) (showSearchView ? 1 : 0));
        parcel.writeByte((byte) (showShareOption ? 1 : 0));
        parcel.writeByte((byte) (showDocumentSettingsOption ? 1 : 0));
        parcel.writeByte((byte) (showAnnotationToolbarOption ? 1 : 0));
        parcel.writeByte((byte) (showOpenFileOption ? 1 : 0));
        parcel.writeByte((byte) (showOpenUrlOption ? 1 : 0));
        parcel.writeByte((byte) (showEditPagesOption ? 1 : 0));
        parcel.writeByte((byte) (showPrintOption ? 1 : 0));
        parcel.writeByte((byte) (showCloseTabOption ? 1 : 0));
        parcel.writeByte((byte) (showAnnotationsList ? 1 : 0));
        parcel.writeByte((byte) (showOutlineList ? 1 : 0));
        parcel.writeByte((byte) (showUserBookmarksList ? 1 : 0));
        parcel.writeByte((byte) (rightToLeftModeEnabled ? 1 : 0));
        parcel.writeByte((byte) (showRightToLeftOption ? 1 : 0));
        parcel.writeParcelable(pdfViewCtrlConfig, i);
        parcel.writeInt(toolManagerBuilderStyleRes);
        parcel.writeParcelable(toolManagerBuilder, i);
    }

    public static class Builder {
        private ViewerConfig mViewerConfig = new ViewerConfig();

        /**
         * Whether to enable full screen mode.
         */
        public Builder fullscreenModeEnabled(boolean fullscreenModeEnabled) {
            mViewerConfig.fullscreenModeEnabled = fullscreenModeEnabled;
            return this;
        }

        /**
         * Whether to enable multi-tab mode.
         */
        public Builder multiTabEnabled(boolean multiTab) {
            mViewerConfig.multiTabEnabled = multiTab;
            return this;
        }

        /**
         * Whether to enable document editing.
         */
        public Builder documentEditingEnabled(boolean documentEditingEnabled) {
            mViewerConfig.documentEditingEnabled = documentEditingEnabled;
            return this;
        }

        /**
         * Whether to enable long press quick menu.
         */
        public Builder longPressQuickMenuEnabled(boolean longPressQuickMenuEnabled) {
            mViewerConfig.longPressQuickMenuEnabled = longPressQuickMenuEnabled;
            return this;
        }

        /**
         * Whether to show page number indicator overlay.
         */
        public Builder showPageNumberIndicator(boolean showPageNumberIndicator) {
            mViewerConfig.showPageNumberIndicator = showPageNumberIndicator;
            return this;
        }

        /**
         * Whether to show bottom navigation bar.
         */
        public Builder showBottomNavBar(boolean showBottomNavBar) {
            mViewerConfig.showBottomNavBar = showBottomNavBar;
            return this;
        }

        /**
         * If {@link ViewerConfig#showBottomNavBar} returns false,
         * then this value is ignored.
         * Whether to show thumbnail view icon.
         */
        public Builder showThumbnailView(boolean showThumbnailView) {
            mViewerConfig.showThumbnailView = showThumbnailView;
            return this;
        }

        /**
         * If {@link ViewerConfig#showBottomNavBar} returns false,
         * then this value is ignored.
         * If all of {@link ViewerConfig#showAnnotationsList},
         * {@link ViewerConfig#showOutlineList}, and
         * {@link ViewerConfig#showUserBookmarksList} return false,
         * then this value is ignored.
         * Whether to show bookmarks view icon.
         */
        public Builder showBookmarksView(boolean showBookmarksView) {
            mViewerConfig.showBookmarksView = showBookmarksView;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Toolbar title.
         */
        public Builder toolbarTitle(String toolbarTitle) {
            mViewerConfig.toolbarTitle = toolbarTitle;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show search view icon.
         */
        public Builder showSearchView(boolean showSearchView) {
            mViewerConfig.showSearchView = showSearchView;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show share icon.
         */
        public Builder showShareOption(boolean showShareOption) {
            mViewerConfig.showShareOption = showShareOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show bookmarks view icon.
         */
        public Builder showDocumentSettingsOption(boolean showDocumentSettingsOption) {
            mViewerConfig.showDocumentSettingsOption = showDocumentSettingsOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show annotation toolbar view icon.
         */
        public Builder showAnnotationToolbarOption(boolean showAnnotationToolbarOption) {
            mViewerConfig.showAnnotationToolbarOption = showAnnotationToolbarOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show open file option.
         */
        public Builder showOpenFileOption(boolean showOpenFileOption) {
            mViewerConfig.showOpenFileOption = showOpenFileOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show open url option.
         */
        public Builder showOpenUrlOption(boolean showOpenUrlOption) {
            mViewerConfig.showOpenUrlOption = showOpenUrlOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show edit pages option.
         */
        public Builder showEditPagesOption(boolean showEditPagesOption) {
            mViewerConfig.showEditPagesOption = showEditPagesOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show print option.
         */
        public Builder showPrintOption(boolean showPrintOption) {
            mViewerConfig.showPrintOption = showPrintOption;
            return this;
        }

        /**
         * If Activity or Fragment supply its own Toolbar,
         * then this value is ignored.
         * Whether to show close document option in the overflow menu.
         */
        public Builder showCloseTabOption(boolean showCloseTabOption) {
            mViewerConfig.showCloseTabOption = showCloseTabOption;
            return this;
        }

        /**
         * If {@link ViewerConfig#showBookmarksView} returns false,
         * then this value is ignored.
         * Whether to show annotation list.
         */
        public Builder showAnnotationsList(boolean showAnnotationsList) {
            mViewerConfig.showAnnotationsList = showAnnotationsList;
            return this;
        }

        /**
         * If {@link ViewerConfig#showBookmarksView} returns false,
         * then this value is ignored.
         * Whether to show outline list.
         */
        public Builder showOutlineList(boolean showOutlineList) {
            mViewerConfig.showOutlineList = showOutlineList;
            return this;
        }

        /**
         * If {@link ViewerConfig#showBookmarksView} returns false,
         * then this value is ignored.
         * Whether to show user bookmarks list.
         */
        public Builder showUserBookmarksList(boolean showUserBookmarksList) {
            mViewerConfig.showUserBookmarksList = showUserBookmarksList;
            return this;
        }

        /**
         * Whether to view documents from right to left.
         * If {@link ViewerConfig#showRightToLeftOption} return false,
         * then this value is ignored.
         */
        public Builder rightToLeftModeEnabled(boolean rightToLeftModeEnabled) {
            mViewerConfig.rightToLeftModeEnabled = rightToLeftModeEnabled;
            return this;
        }

        /**
         * Whether to enable RTL option in the view mode dialog.
         */
        public Builder showRightToLeftOption(boolean showRightToLeftOption) {
            mViewerConfig.showRightToLeftOption = showRightToLeftOption;
            return this;
        }

        /**
         * Sets the {@link PDFViewCtrlConfig} for {@link com.pdftron.pdf.PDFViewCtrl}
         */
        public Builder pdfViewCtrlConfig(PDFViewCtrlConfig config) {
            mViewerConfig.pdfViewCtrlConfig = config;
            return this;
        }

        /**
         * Sets the style resource ID used for {@link ToolManagerBuilder}
         */
        public Builder toolManagerBuilderStyleRes(@StyleRes int styleRes) {
            mViewerConfig.toolManagerBuilderStyleRes = styleRes;
            return this;
        }

        /**
         * Sets tool manager builder for building tool manager
         * @param toolManagerBuilder The tool manager buidler
         * @return This PDFViewCtrlConfig
         */
        public Builder setToolManagerBuilder(ToolManagerBuilder toolManagerBuilder) {
            mViewerConfig.toolManagerBuilder = toolManagerBuilder;
            return this;
        }

        public ViewerConfig build() {
            return mViewerConfig;
        }
    }
}
