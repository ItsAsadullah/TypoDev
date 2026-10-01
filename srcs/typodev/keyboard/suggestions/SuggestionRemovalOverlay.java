package typodev.keyboard.suggestions;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import typodev.keyboard.Keyboard2;
import typodev.keyboard.R;

/**
 * Fullscreen visual overlay replicating Google Keyboard (GBoard) suggestion removal.
 * Displays "Remove suggestion" title, a trash can target circle, and a floating dragged chip.
 */
public class SuggestionRemovalOverlay extends FrameLayout
{
  private final LinearLayout _targetContainer;
  private final TextView _tvTitle;
  private final FrameLayout _trashCircle;
  private final ImageView _ivTrash;
  private final GradientDrawable _trashBg;
  private final TextView _tvFloatingChip;
  private final GradientDrawable _chipBg;

  private PopupWindow _popup;
  private ViewGroup _decorView;
  private boolean _isHovered = false;
  private float _targetCenterX = 0f;
  private float _targetCenterY = 0f;

  public SuggestionRemovalOverlay(Context context)
  {
    super(context);
    setBackgroundColor(Color.TRANSPARENT);
    setClipChildren(false);
    setClipToPadding(false);

    final float density = getResources().getDisplayMetrics().density;

    // 1. Target container: "Remove suggestion" title + trash can circle
    _targetContainer = new LinearLayout(context);
    _targetContainer.setOrientation(LinearLayout.VERTICAL);
    _targetContainer.setGravity(Gravity.CENTER_HORIZONTAL);
    _targetContainer.setClipChildren(false);
    _targetContainer.setClipToPadding(false);

    // 1.1 "Remove suggestion" header text
    _tvTitle = new TextView(context);
    _tvTitle.setText("Remove suggestion");
    _tvTitle.setTextColor(0xFFFFFFFF);
    _tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
    _tvTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    _tvTitle.setGravity(Gravity.CENTER_HORIZONTAL);
    _tvTitle.setShadowLayer(4f * density, 0f, 2f * density, 0xCC000000);
    LinearLayout.LayoutParams lpTitle = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpTitle.bottomMargin = Math.round(14f * density);
    _targetContainer.addView(_tvTitle, lpTitle);

    // 1.2 Trash can circular container
    _trashCircle = new FrameLayout(context);
    int circleSize = Math.round(64f * density);
    LinearLayout.LayoutParams lpCircle = new LinearLayout.LayoutParams(circleSize, circleSize);
    lpCircle.gravity = Gravity.CENTER_HORIZONTAL;

    _trashBg = new GradientDrawable();
    _trashBg.setShape(GradientDrawable.OVAL);
    _trashBg.setColor(0xFF3C4043); // Material dark grey circle
    _trashBg.setStroke(Math.round(1f * density), 0x33FFFFFF);
    _trashCircle.setBackground(_trashBg);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
    {
      _trashCircle.setElevation(6f * density);
    }

    // 1.3 Trash can icon inside the circle
    _ivTrash = new ImageView(context);
    _ivTrash.setImageResource(R.drawable.ic_delete);
    _ivTrash.setColorFilter(0xFFFFFFFF);
    int iconSize = Math.round(28f * density);
    FrameLayout.LayoutParams lpIcon = new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER);
    _trashCircle.addView(_ivTrash, lpIcon);

    _targetContainer.addView(_trashCircle, lpCircle);

    FrameLayout.LayoutParams lpTarget = new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    addView(_targetContainer, lpTarget);

    // 2. Floating dragged chip preview
    _tvFloatingChip = new TextView(context);
    _tvFloatingChip.setTextColor(0xFFFFFFFF);
    _tvFloatingChip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
    _tvFloatingChip.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
    _tvFloatingChip.setGravity(Gravity.CENTER);
    int padH = Math.round(18f * density);
    int padV = Math.round(11f * density);
    _tvFloatingChip.setPadding(padH, padV, padH, padV);

    _chipBg = new GradientDrawable();
    _chipBg.setShape(GradientDrawable.RECTANGLE);
    _chipBg.setColor(0xFF383431); // Matching elevated brown/dark chip in screenshot
    _chipBg.setCornerRadius(8f * density);
    _chipBg.setStroke(Math.round(1f * density), 0x55888888);
    _tvFloatingChip.setBackground(_chipBg);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
    {
      _tvFloatingChip.setElevation(14f * density);
    }

    FrameLayout.LayoutParams lpChip = new FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    addView(_tvFloatingChip, lpChip);
  }

  public void show(Keyboard2 keyboard, View anchor, String word, float startX, float startY)
  {
    if (keyboard == null || anchor == null || _tvFloatingChip == null || _targetContainer == null)
      return;
    _tvFloatingChip.setText(word != null ? word : "");
    _isHovered = false;
    _trashCircle.setScaleX(1.0f);
    _trashCircle.setScaleY(1.0f);
    _trashBg.setColor(0xFF3C4043);
    _tvTitle.setTextColor(0xFFFFFFFF);

    final float density = getResources().getDisplayMetrics().density;

    // Calculate vertical position of trash target based on keyboard position
    int[] anchorLoc = new int[2];
    anchor.getLocationOnScreen(anchorLoc);
    int anchorTop = anchorLoc[1];

    float targetY = anchorTop - (170f * density);
    if (targetY < 80f * density)
    {
      targetY = Math.max(50f * density, anchorTop * 0.45f);
    }
    _targetContainer.setY(targetY);

    // Subtle entrance animation for target container
    _targetContainer.setAlpha(0f);
    _targetContainer.setScaleX(0.85f);
    _targetContainer.setScaleY(0.85f);
    _targetContainer.animate()
        .alpha(1.0f)
        .scaleX(1.0f)
        .scaleY(1.0f)
        .setDuration(180)
        .setInterpolator(new DecelerateInterpolator())
        .start();

    // Attach to Window DecorView or PopupWindow
    _decorView = null;
    if (keyboard != null && keyboard.getWindow() != null && keyboard.getWindow().getWindow() != null)
    {
      try
      {
        _decorView = (ViewGroup) keyboard.getWindow().getWindow().getDecorView();
      }
      catch (Throwable ignored) {}
    }

    if (_decorView != null)
    {
      try
      {
        if (getParent() != null)
        {
          ((ViewGroup) getParent()).removeView(this);
        }
        _decorView.addView(this, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      }
      catch (Throwable t)
      {
        _decorView = null;
      }
    }

    if (_decorView == null)
    {
      try
      {
        _popup = new PopupWindow(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        _popup.setClippingEnabled(false);
        _popup.setFocusable(false);
        _popup.setTouchable(false);
        _popup.setBackgroundDrawable(null);
        _popup.showAtLocation(anchor, Gravity.FILL, 0, 0);
      }
      catch (Throwable ignored) {}
    }

    // Initialize floating chip position
    _tvFloatingChip.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    updateDrag(startX, startY);
  }

  public void updateDrag(float rawX, float rawY)
  {
    final float density = getResources().getDisplayMetrics().density;
    int chipW = _tvFloatingChip.getMeasuredWidth();
    int chipH = _tvFloatingChip.getMeasuredHeight();
    if (chipW <= 0) chipW = Math.round(90f * density);
    if (chipH <= 0) chipH = Math.round(36f * density);

    // Place chip slightly above user's thumb so it is clearly visible
    float chipX = rawX - (chipW / 2f);
    float chipY = rawY - (chipH / 2f) - (50f * density);

    _tvFloatingChip.setX(chipX);
    _tvFloatingChip.setY(chipY);

    // Compute trash center
    if (_targetCenterX <= 0 || _targetCenterY <= 0)
    {
      int[] circleLoc = new int[2];
      _trashCircle.getLocationOnScreen(circleLoc);
      _targetCenterX = circleLoc[0] + (_trashCircle.getWidth() / 2f);
      _targetCenterY = circleLoc[1] + (_trashCircle.getHeight() / 2f);
    }

    if (_targetCenterX > 0 && _targetCenterY > 0)
    {
      float dist = (float) Math.hypot(rawX - _targetCenterX, rawY - _targetCenterY);
      float hoverThreshold = 75f * density;

      if (dist < hoverThreshold)
      {
        if (!_isHovered)
        {
          _isHovered = true;
          _trashCircle.animate().scaleX(1.22f).scaleY(1.22f).setDuration(130).start();
          _trashBg.setColor(0xFFEA4335); // Google Red highlight!
          _tvTitle.setTextColor(0xFFFF8A80);
          try
          {
            performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
          }
          catch (Throwable ignored) {}
        }
      }
      else
      {
        if (_isHovered)
        {
          _isHovered = false;
          _trashCircle.animate().scaleX(1.0f).scaleY(1.0f).setDuration(130).start();
          _trashBg.setColor(0xFF3C4043); // Back to dark grey
          _tvTitle.setTextColor(0xFFFFFFFF);
        }
      }
    }
  }

  public boolean endDrag(float rawX, float rawY)
  {
    boolean wasHovered = _isHovered;
    final float density = getResources().getDisplayMetrics().density;

    if (!wasHovered && _targetCenterX > 0 && _targetCenterY > 0)
    {
      float dist = (float) Math.hypot(rawX - _targetCenterX, rawY - _targetCenterY);
      if (dist < 75f * density)
      {
        wasHovered = true;
      }
    }

    dismiss();
    return wasHovered;
  }

  public void dismiss()
  {
    if (_decorView != null)
    {
      try
      {
        _decorView.removeView(this);
      }
      catch (Throwable ignored) {}
      _decorView = null;
    }
    if (_popup != null)
    {
      try
      {
        _popup.dismiss();
      }
      catch (Throwable ignored) {}
      _popup = null;
    }
  }
}
