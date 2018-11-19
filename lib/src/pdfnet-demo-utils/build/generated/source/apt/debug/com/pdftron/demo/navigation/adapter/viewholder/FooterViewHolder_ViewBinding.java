// Generated code from Butter Knife. Do not modify!
package com.pdftron.demo.navigation.adapter.viewholder;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.ProgressBar;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.pdftron.demo.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class FooterViewHolder_ViewBinding implements Unbinder {
  private FooterViewHolder target;

  @UiThread
  public FooterViewHolder_ViewBinding(FooterViewHolder target, View source) {
    this.target = target;

    target.progBar = Utils.findRequiredViewAsType(source, R.id.footer_progress_bar, "field 'progBar'", ProgressBar.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    FooterViewHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.progBar = null;
  }
}
