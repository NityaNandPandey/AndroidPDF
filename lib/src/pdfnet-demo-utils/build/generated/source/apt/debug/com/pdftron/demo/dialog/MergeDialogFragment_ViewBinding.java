// Generated code from Butter Knife. Do not modify!
package com.pdftron.demo.dialog;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.v7.widget.Toolbar;
import android.view.View;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.pdftron.demo.R;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import java.lang.IllegalStateException;
import java.lang.Override;

public class MergeDialogFragment_ViewBinding implements Unbinder {
  private MergeDialogFragment target;

  @UiThread
  public MergeDialogFragment_ViewBinding(MergeDialogFragment target, View source) {
    this.target = target;

    target.mToolbar = Utils.findRequiredViewAsType(source, R.id.fragment_merge_dialog_toolbar, "field 'mToolbar'", Toolbar.class);
    target.mCabToolbar = Utils.findRequiredViewAsType(source, R.id.fragment_merge_dialog_cab, "field 'mCabToolbar'", Toolbar.class);
    target.mRecyclerView = Utils.findRequiredViewAsType(source, R.id.fragment_merge_dialog_recycler_view, "field 'mRecyclerView'", SimpleRecyclerView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    MergeDialogFragment target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.mToolbar = null;
    target.mCabToolbar = null;
    target.mRecyclerView = null;
  }
}
