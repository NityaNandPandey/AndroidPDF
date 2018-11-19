// Generated code from Butter Knife. Do not modify!
package com.pdftron.demo.navigation.adapter;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.pdftron.demo.R;
import java.lang.IllegalStateException;
import java.lang.Override;

public class ContentRecyclerAdapter$ItemViewHolder_ViewBinding implements Unbinder {
  private ContentRecyclerAdapter.ItemViewHolder target;

  @UiThread
  public ContentRecyclerAdapter$ItemViewHolder_ViewBinding(ContentRecyclerAdapter.ItemViewHolder target,
      View source) {
    this.target = target;

    target.textView = Utils.findOptionalViewAsType(source, R.id.item_text, "field 'textView'", TextView.class);
    target.imageView = Utils.findOptionalViewAsType(source, R.id.item_icon, "field 'imageView'", ImageView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    ContentRecyclerAdapter.ItemViewHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.textView = null;
    target.imageView = null;
  }
}
