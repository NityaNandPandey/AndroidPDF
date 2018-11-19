// Generated code from Butter Knife. Do not modify!
package com.pdftron.demo.navigation;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
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

public class ExternalStorageViewFragment_ViewBinding implements Unbinder {
  private ExternalStorageViewFragment target;

  @UiThread
  public ExternalStorageViewFragment_ViewBinding(ExternalStorageViewFragment target, View source) {
    this.target = target;

    target.mRecyclerView = Utils.findRequiredViewAsType(source, R.id.recycler_view, "field 'mRecyclerView'", SimpleRecyclerView.class);
    target.mEmptyView = Utils.findRequiredView(source, android.R.id.empty, "field 'mEmptyView'");
    target.mEmptyTextView = Utils.findRequiredViewAsType(source, R.id.empty_text_view, "field 'mEmptyTextView'", TextView.class);
    target.mEmptyImageView = Utils.findRequiredView(source, R.id.empty_image_view, "field 'mEmptyImageView'");
    target.mProgressBarView = Utils.findRequiredViewAsType(source, R.id.progress_bar_view, "field 'mProgressBarView'", ProgressBar.class);
    target.mBreadcrumbBarScrollView = Utils.findRequiredViewAsType(source, R.id.breadcrumb_bar_scroll_view, "field 'mBreadcrumbBarScrollView'", HorizontalScrollView.class);
    target.mBreadcrumbBarLayout = Utils.findRequiredViewAsType(source, R.id.breadcrumb_bar_layout, "field 'mBreadcrumbBarLayout'", LinearLayout.class);
    target.mFabMenu = Utils.findRequiredViewAsType(source, R.id.fab_menu, "field 'mFabMenu'", FloatingActionMenu.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    ExternalStorageViewFragment target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.mRecyclerView = null;
    target.mEmptyView = null;
    target.mEmptyTextView = null;
    target.mEmptyImageView = null;
    target.mProgressBarView = null;
    target.mBreadcrumbBarScrollView = null;
    target.mBreadcrumbBarLayout = null;
    target.mFabMenu = null;
  }
}
