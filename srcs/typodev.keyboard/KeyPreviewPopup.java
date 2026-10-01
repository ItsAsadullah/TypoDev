package typodev.keyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

public class KeyPreviewPopup
{
  private final PopupWindow _popup;
  private final FrameLayout _container;
  private final TextView _textView;
  private final float _density;

  public KeyPreviewPopup(Context context)
  {
    _density = context.getResources().getDisplayMetrics().density;
    _container = new FrameLayout(context);
    _textView = new TextView(context);
    _textView.setGravity(Gravity.CENTER);
    _textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
    _textView.setTypeface(null, Typeface.BOLD);
    if (android.os.Build.VERSION.SDK_INT >= 21)
    {
      _container.setElevation(8 * _density);
    }

    _container.addView(_textView, new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

    _popup = new PopupWindow(_container, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    _popup.setClippingEnabled(false);
    _popup.setTouchable(false);
    _popup.setFocusable(false);
    _popup.setBackgroundDrawable(null);
  }

  public void show(View anchor, String text, float keyX, float keyY, float keyW, float keyH, int bgColor, int textColor)
  {
    try
    {
      if (text == null || text.trim().isEmpty() || anchor == null || !anchor.isAttachedToWindow())
        return;

      _textView.setText(text);
      _textView.setTextColor(textColor);

      if (text.length() > 2)
      {
        _textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
      }
      else if (text.length() > 1)
      {
        _textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
      }
      else
      {
        _textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
      }

      int width = Math.max(Math.round(keyW * 1.30f), Math.round(50 * _density));
      if (text.length() > 2)
      {
        width = Math.max(width, Math.round(65 * _density));
      }
      int height = Math.round(keyH * 1.35f);

      // Contrast border based on background luminance
      int r = Color.red(bgColor);
      int g = Color.green(bgColor);
      int b = Color.blue(bgColor);
      double brightness = (r * 299 + g * 587 + b * 114) / 1000.0;
      int strokeColor = (brightness > 128) ? Color.parseColor("#33000000") : Color.parseColor("#44FFFFFF");

      GradientDrawable bg = new GradientDrawable();
      bg.setShape(GradientDrawable.RECTANGLE);
      bg.setColor(bgColor);
      bg.setCornerRadius(8 * _density);
      bg.setStroke(Math.round(1.5f * _density), strokeColor);
      _container.setBackground(bg);
      _container.invalidate();

      int[] location = new int[2];
      anchor.getLocationInWindow(location);

      int popupX = location[0] + Math.round(keyX + (keyW - width) / 2f);
      int popupY = location[1] + Math.round(keyY - height - (6 * _density));

      if (_popup.isShowing())
      {
        _popup.update(popupX, popupY, width, height);
      }
      else
      {
        _popup.setWidth(width);
        _popup.setHeight(height);
        _popup.showAtLocation(anchor, Gravity.NO_GRAVITY, popupX, popupY);
      }
    }
    catch (Throwable ignored) {}
  }

  public void dismiss()
  {
    try
    {
      if (_popup != null && _popup.isShowing())
      {
        _popup.dismiss();
      }
    }
    catch (Throwable ignored) {}
  }
}
