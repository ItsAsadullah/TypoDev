package typodev.keyboard.voice;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Gboard-style audio spectrum wave button.
 * Draws dynamic pulsing ripple spectrum rings around the microphone icon
 * responding in real time to voice audio levels (rmsdB).
 */
public class VoiceSpectrumButton extends FrameLayout
{
  private boolean _isListening = false;
  private float _currentRms = 0f;
  private float _targetRms = 0f;
  private float _pulseAngle = 0f;

  private Paint _haloPaint;
  private Paint _wavePaintInner;
  private Paint _wavePaintOuter;
  private ImageView _micIcon = null;
  private android.widget.TextView _micTextView = null;
  private int _normalTextColor = 0;

  // Vibrant Google-style Blue for voice typing active state
  private static final int COLOR_ACTIVE = 0xFF4285F4;

  public VoiceSpectrumButton(Context context)
  {
    super(context);
    init();
  }

  public VoiceSpectrumButton(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    init();
  }

  public VoiceSpectrumButton(Context context, AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init()
  {
    setWillNotDraw(false);

    _haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    _haloPaint.setStyle(Paint.Style.FILL);
    _haloPaint.setColor(COLOR_ACTIVE);

    _wavePaintInner = new Paint(Paint.ANTI_ALIAS_FLAG);
    _wavePaintInner.setStyle(Paint.Style.STROKE);
    _wavePaintInner.setColor(COLOR_ACTIVE);

    _wavePaintOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
    _wavePaintOuter.setStyle(Paint.Style.STROKE);
    _wavePaintOuter.setColor(COLOR_ACTIVE);
  }

  private int _normalIconColor = 0;

  private int getThemeLabelColor()
  {
    try
    {
      android.util.TypedValue tv = new android.util.TypedValue();
      if (getContext().getTheme().resolveAttribute(typodev.keyboard.R.attr.colorLabel, tv, true))
      {
        return tv.data;
      }
    }
    catch (Throwable ignored) {}
    return 0xFFFFFFFF;
  }

  @Override
  protected void onFinishInflate()
  {
    super.onFinishInflate();
    for (int i = 0; i < getChildCount(); i++)
    {
      android.view.View child = getChildAt(i);
      if (child instanceof android.widget.TextView)
      {
        setMicTextView((android.widget.TextView)child);
        break;
      }
      else if (child instanceof ImageView)
      {
        setMicIcon((ImageView)child);
        break;
      }
    }
  }

  public void setMicIcon(ImageView icon)
  {
    _micIcon = icon;
    if (_micIcon != null)
    {
      if (_normalIconColor == 0)
      {
        _normalIconColor = getThemeLabelColor();
      }
      _micIcon.setColorFilter(_isListening ? COLOR_ACTIVE : _normalIconColor, PorterDuff.Mode.SRC_IN);
    }
  }

  public void setMicTextView(android.widget.TextView tv)
  {
    _micTextView = tv;
    if (tv != null && _normalTextColor == 0)
    {
      _normalTextColor = tv.getCurrentTextColor();
    }
  }

  public void setListening(boolean listening)
  {
    _isListening = listening;
    if (listening)
    {
      _targetRms = 0.25f;
      _currentRms = 0.15f;
      if (_micIcon != null)
      {
        _micIcon.setColorFilter(COLOR_ACTIVE, PorterDuff.Mode.SRC_IN);
      }
      if (_micTextView != null)
      {
        if (_normalTextColor == 0)
        {
          _normalTextColor = _micTextView.getCurrentTextColor();
        }
        _micTextView.setTextColor(COLOR_ACTIVE);
      }
    }
    else
    {
      _targetRms = 0f;
      _currentRms = 0f;
      if (_micIcon != null)
      {
        if (_normalIconColor == 0)
        {
          _normalIconColor = getThemeLabelColor();
        }
        _micIcon.setColorFilter(_normalIconColor, PorterDuff.Mode.SRC_IN);
      }
      if (_micTextView != null && _normalTextColor != 0)
      {
        _micTextView.setTextColor(_normalTextColor);
      }
    }
    invalidate();
  }

  public boolean isListening()
  {
    return _isListening;
  }

  public void onRmsChanged(float rmsdB)
  {
    if (!_isListening) return;
    // rmsdB typically ranges from -2 (ambient silence) to 10+ (speech)
    float normalized = Math.max(0f, Math.min(1.0f, (rmsdB + 2.0f) / 12.0f));
    _targetRms = Math.max(_targetRms * 0.7f, normalized);
    postInvalidateOnAnimation();
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    if (_isListening)
    {
      float cx = getWidth() / 2f;
      float cy = getHeight() / 2f;
      float baseRadius = Math.min(cx, cy) * 0.52f;

      // Smooth interpolation toward target level
      _currentRms += (_targetRms - _currentRms) * 0.32f;
      // Decay target gradually so sound pulses organically
      _targetRms *= 0.92f;

      // Gentle organic oscillation
      _pulseAngle += 0.09f;
      float breath = (float)Math.sin(_pulseAngle) * 0.05f;
      float lvl = Math.max(0f, Math.min(1f, _currentRms + breath));

      // 1. Center soft circular halo
      _haloPaint.setAlpha((int)(25 + 45 * lvl));
      canvas.drawCircle(cx, cy, baseRadius * (1.0f + 0.15f * lvl), _haloPaint);

      // 2. Inner dynamic wave ring
      _wavePaintInner.setStrokeWidth(2.2f + 1.6f * lvl);
      _wavePaintInner.setAlpha((int)(40 + 80 * lvl));
      canvas.drawCircle(cx, cy, baseRadius * (1.18f + 0.35f * lvl), _wavePaintInner);

      // 3. Outer expanding spectrum ring
      _wavePaintOuter.setStrokeWidth(1.8f + 2.2f * lvl);
      _wavePaintOuter.setAlpha((int)(30 + 105 * lvl));
      canvas.drawCircle(cx, cy, baseRadius * (1.35f + 0.65f * lvl), _wavePaintOuter);

      postInvalidateOnAnimation();
    }
    super.onDraw(canvas);
  }
}
