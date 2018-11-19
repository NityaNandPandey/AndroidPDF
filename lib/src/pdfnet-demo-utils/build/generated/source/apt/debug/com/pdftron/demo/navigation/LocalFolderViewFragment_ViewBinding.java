// Generated code from Butter Knife. Do not modify!
package com.pdftron.demo.navigation;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.github.clans.fab.FloatingActionMenu;
import com.pdftron.demo.R;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import java.lang.IllegalStateException;
import java.lang.Override;

public class LocalFolderViewFragment_ViewBinding implements Unbinder {
  private LocalFolderViewFragment target;

  @UiThread
  public LocalFolderViewFragment_ViewBinding(LocalFolderViewFragment target, View source) {
    this.target = target;

    target.mRecyclerView = Utils.findRequiredViewAsType(source, R.id.recycler_view, "field 'mRecyclerView'", SimpleRecyclerView.class);
    target.mEmptyTextView = Utils.findRequiredViewAsType(source, R.id.empty_text_view, "field 'mEmptyTextView'", TextView.class);
    target.mProgressBarView = Utils.findRequiredViewAsType(source, R.id.progress_bar_view, "field 'mProgressBarView'", ProgressBar.class);
    target.mBreadcrumbBarScrollView = Utils.findRequiredViewAsType(source, R.id.breadcrumb_bar_scroll_view, "field 'mBreadcrumbBarScrollView'", HorizontalScrollView.class);
    target.mBreadcrumbBarLayout = Utils.findRequiredViewAsType(source, R.id.breadcrumb_bar_layout, "field 'mBreadcrumbBarLayout'", LinearLayout.class);
    target.mFabMenu = Utils.findRequiredViewAsType(source, R.id.fab_menu, "field 'mFabMenu'", FloatingActionMenu.class);
    target.mGoToSdCardView = Utils.findRequiredViewAsType(source, R.id.go_to_sd_card_view, "field 'mGoToSdCardView'", ViewGroup.class);
    target.mGoToSdCardButton = Utils.findRequiredViewAsType(source, R.id.buttonGoToSdCard, "field 'mGoToSdCardButton'", Button.class);
    target.mGoToSdCardDescription = Utils.findRequiredViewAsType(source, R.id.go_to_sd_card_view_text, "field 'mGoToSdCardDescription'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    LocalFolderViewFragment target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.mRecyclerView = null;
    target.mEmptyTextView = null;
    target.mProgressBarView = null;
    target.mBreadcrumbBarScrollView = null;
    target.mBreadcrumbBarLayout = null;
    target.mFabMenu = null;
    target.mGoToSdCardView = null;
    target.mGoToSdCardButton = null;
    target.mGoToSdCardDescription = null;
  }
}
