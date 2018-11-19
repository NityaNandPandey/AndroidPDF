// Generated code from Butter Knife. Do not modify!
package com.pdftron.demo.dialog;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.github.clans.fab.FloatingActionButton;
import com.pdftron.demo.R;
import com.pdftron.pdf.widget.ContentLoadingRelativeLayout;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import java.lang.IllegalStateException;
import java.lang.Override;

public class FilePickerDialogFragment_ViewBinding implements Unbinder {
  private FilePickerDialogFragment target;

  @UiThread
  public FilePickerDialogFragment_ViewBinding(FilePickerDialogFragment target, View source) {
    this.target = target;

    target.mToolbar = Utils.findRequiredViewAsType(source, R.id.fragment_file_picker_dialog_toolbar, "field 'mToolbar'", Toolbar.class);
    target.mRecyclerView = Utils.findRequiredViewAsType(source, R.id.fragment_file_picker_dialog_folder_list, "field 'mRecyclerView'", SimpleRecyclerView.class);
    target.mEmptyView = Utils.findRequiredViewAsType(source, android.R.id.empty, "field 'mEmptyView'", TextView.class);
    target.mProgressBarLayout = Utils.findRequiredViewAsType(source, R.id.fragment_file_picker_dialog_progress_bar, "field 'mProgressBarLayout'", ContentLoadingRelativeLayout.class);
    target.mFab = Utils.findRequiredViewAsType(source, R.id.fragment_file_picker_dialog_fab, "field 'mFab'", FloatingActionButton.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    FilePickerDialogFragment target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.mToolbar = null;
    target.mRecyclerView = null;
    target.mEmptyView = null;
    target.mProgressBarLayout = null;
    target.mFab = null;
  }
}
