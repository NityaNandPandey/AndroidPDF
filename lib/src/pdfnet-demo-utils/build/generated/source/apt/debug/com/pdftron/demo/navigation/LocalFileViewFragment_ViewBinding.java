// Generated code from Butter Knife. Do not modify!
package com.pdftron.demo.navigation;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.github.clans.fab.FloatingActionMenu;
import com.pdftron.demo.R;
import com.pdftron.demo.widget.SimpleStickyRecyclerView;
import com.pdftron.demo.widget.StickyHeader;
import java.lang.IllegalStateException;
import java.lang.Override;

public class LocalFileViewFragment_ViewBinding implements Unbinder {
  private LocalFileViewFragment target;

  @UiThread
  public LocalFileViewFragment_ViewBinding(LocalFileViewFragment target, View source) {
    this.target = target;

    target.mRecyclerView = Utils.findRequiredViewAsType(source, R.id.recycler_view, "field 'mRecyclerView'", SimpleStickyRecyclerView.class);
    target.mEmptyTextView = Utils.findRequiredViewAsType(source, R.id.empty_text_view, "field 'mEmptyTextView'", TextView.class);
    target.mProgressBarView = Utils.findRequiredViewAsType(source, R.id.progress_bar_view, "field 'mProgressBarView'", ProgressBar.class);
    target.mFabMenu = Utils.findRequiredViewAsType(source, R.id.fab_menu, "field 'mFabMenu'", FloatingActionMenu.class);
    target.stickyHeader = Utils.findRequiredViewAsType(source, R.id.sticky_header, "field 'stickyHeader'", StickyHeader.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    LocalFileViewFragment target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.mRecyclerView = null;
    target.mEmptyTextView = null;
    target.mProgressBarView = null;
    target.mFabMenu = null;
    target.stickyHeader = null;
  }
}
