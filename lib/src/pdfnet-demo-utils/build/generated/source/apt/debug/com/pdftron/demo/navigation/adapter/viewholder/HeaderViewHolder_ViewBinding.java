// Generated code from Butter Knife. Do not modify!
package com.pdftron.demo.navigation.adapter.viewholder;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.pdftron.demo.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class HeaderViewHolder_ViewBinding implements Unbinder {
  private HeaderViewHolder target;

  @UiThread
  public HeaderViewHolder_ViewBinding(HeaderViewHolder target, View source) {
    this.target = target;

    target.textViewTitle = Utils.findRequiredViewAsType(source, R.id.title, "field 'textViewTitle'", TextView.class);
    target.foldingBtn = Utils.findRequiredViewAsType(source, R.id.folding_btn, "field 'foldingBtn'", AppCompatImageView.class);
    target.header_view = Utils.findRequiredView(source, R.id.header_view, "field 'header_view'");
    target.divider = Utils.findRequiredViewAsType(source, R.id.divider, "field 'divider'", ImageView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    HeaderViewHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.textViewTitle = null;
    target.foldingBtn = null;
    target.header_view = null;
    target.divider = null;
  }
}
