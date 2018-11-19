// Generated code from Butter Knife. Do not modify!
package com.pdftron.demo.navigation.adapter.viewholder;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import com.pdftron.demo.R;
import com.pdftron.demo.widget.ImageViewTopCrop;
import java.lang.IllegalStateException;
import java.lang.Override;

public class ContentViewHolder_ViewBinding implements Unbinder {
  private ContentViewHolder target;

  @UiThread
  public ContentViewHolder_ViewBinding(ContentViewHolder target, View source) {
    this.target = target;

    target.imageViewFileIcon = Utils.findRequiredViewAsType(source, R.id.file_icon, "field 'imageViewFileIcon'", ImageViewTopCrop.class);
    target.imageViewFileLockIcon = Utils.findRequiredViewAsType(source, R.id.file_lock_icon, "field 'imageViewFileLockIcon'", ImageView.class);
    target.docTextPlaceHolder = Utils.findRequiredViewAsType(source, R.id.docTextPlaceHolder, "field 'docTextPlaceHolder'", TextView.class);
    target.textViewFileName = Utils.findRequiredViewAsType(source, R.id.file_name, "field 'textViewFileName'", TextView.class);
    target.textViewFileInfo = Utils.findRequiredViewAsType(source, R.id.file_info, "field 'textViewFileInfo'", TextView.class);
    target.imageViewInfoIcon = Utils.findRequiredViewAsType(source, R.id.info_icon, "field 'imageViewInfoIcon'", ImageView.class);
    target.infoButton = Utils.findRequiredView(source, R.id.info_button, "field 'infoButton'");
    target.divider = Utils.findRequiredViewAsType(source, R.id.divider, "field 'divider'", ImageView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    ContentViewHolder target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.imageViewFileIcon = null;
    target.imageViewFileLockIcon = null;
    target.docTextPlaceHolder = null;
    target.textViewFileName = null;
    target.textViewFileInfo = null;
    target.imageViewInfoIcon = null;
    target.infoButton = null;
    target.divider = null;
  }
}
