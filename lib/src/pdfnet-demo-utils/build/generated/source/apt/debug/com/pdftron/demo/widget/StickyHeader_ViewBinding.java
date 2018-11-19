// Generated code from Butter Knife. Do not modify!
package com.pdftron.demo.widget;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.pdftron.demo.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class StickyHeader_ViewBinding implements Unbinder {
  private StickyHeader target;

  @UiThread
  public StickyHeader_ViewBinding(StickyHeader target) {
    this(target, target);
  }

  @UiThread
  public StickyHeader_ViewBinding(StickyHeader target, View source) {
    this.target = target;

    target.header = Utils.findRequiredView(source, R.id.header_view, "field 'header'");
    target.title = Utils.findRequiredViewAsType(source, R.id.title, "field 'title'", TextView.class);
    target.foldingBtn = Utils.findRequiredViewAsType(source, R.id.folding_btn, "field 'foldingBtn'", AppCompatImageView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    StickyHeader target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.header = null;
    target.title = null;
    target.foldingBtn = null;
  }
}
