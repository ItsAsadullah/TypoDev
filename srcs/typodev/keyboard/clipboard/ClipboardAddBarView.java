package typodev.keyboard.clipboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

/**
 * Top bar view for adding a new custom clip to the pinned clipboard.
 * Sits directly above Keyboard2View so the user can type using TypoDev's soft keyboard.
 */
public class ClipboardAddBarView extends LinearLayout
{
  public interface OnAddClipActionListener
  {
    void onPin(String text);
    void onCancel();
  }

  private OnAddClipActionListener _listener = null;

  private Button _btnCancel;
  private EditText _etInput;
  private Button _btnPin;

  private int _colorKeyboard = Color.parseColor("#151A23");
  private int _colorKey = Color.parseColor("#212836");
  private int _colorLabel = Color.WHITE;
  private int _colorKeyActivated = Color.parseColor("#2AABEE");

  public ClipboardAddBarView(Context context)
  {
    super(context);
    init(context);
  }

  public ClipboardAddBarView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    init(context);
  }

  public ClipboardAddBarView(Context context, AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  public void setOnAddClipActionListener(OnAddClipActionListener listener)
  {
    _listener = listener;
  }

  private int dp(int val)
  {
    return (int) (val * getResources().getDisplayMetrics().density + 0.5f);
  }

  private int adjustAlpha(int color, float factor)
  {
    int alpha = Math.round(Color.alpha(color) * factor);
    int red = Color.red(color);
    int green = Color.green(color);
    int blue = Color.blue(color);
    return Color.argb(alpha, red, green, blue);
  }

  private GradientDrawable createPillBackground(int color, int radiusDp, int strokeColor)
  {
    GradientDrawable gd = new GradientDrawable();
    gd.setShape(GradientDrawable.RECTANGLE);
    gd.setCornerRadius(radiusDp * getResources().getDisplayMetrics().density);
    gd.setColor(color);
    if (strokeColor != Color.TRANSPARENT)
    {
      gd.setStroke((int) (1 * getResources().getDisplayMetrics().density), strokeColor);
    }
    return gd;
  }

  private void init(Context context)
  {
    setOrientation(HORIZONTAL);
    setGravity(Gravity.CENTER_VERTICAL);
    setPadding(dp(6), dp(4), dp(6), dp(4));

    // 1. Cancel button [✕]
    _btnCancel = new Button(context);
    _btnCancel.setText("✕");
    _btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    _btnCancel.setTextColor(_colorLabel);
    _btnCancel.setBackground(createPillBackground(_colorKey, dp(16), Color.TRANSPARENT));
    _btnCancel.setPadding(0, 0, 0, 0);
    _btnCancel.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_listener != null) _listener.onCancel();
      }
    });
    LinearLayout.LayoutParams lpCancel = new LinearLayout.LayoutParams(dp(34), dp(34));
    lpCancel.setMargins(0, 0, dp(6), 0);
    addView(_btnCancel, lpCancel);

    // 2. Input Field [ ➕ Type or paste text to pin... ]
    _etInput = new EditText(context);
    _etInput.setHint("Type text to pin...");
    _etInput.setHintTextColor(adjustAlpha(_colorLabel, 0.45f));
    _etInput.setTextColor(_colorLabel);
    _etInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    _etInput.setBackground(createPillBackground(_colorKey, dp(16), adjustAlpha(_colorLabel, 0.2f)));
    _etInput.setPadding(dp(12), dp(4), dp(12), dp(4));
    _etInput.setSingleLine(true);
    _etInput.setEllipsize(TextUtils.TruncateAt.END);
    _etInput.setFocusable(false);
    _etInput.setFocusableInTouchMode(false);
    LinearLayout.LayoutParams lpInput = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
    addView(_etInput, lpInput);

    // 3. Pin button [📌 Pin]
    _btnPin = new Button(context);
    _btnPin.setText("Pin");
    _btnPin.setTypeface(Typeface.DEFAULT_BOLD);
    _btnPin.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    _btnPin.setTextColor(Color.WHITE);
    _btnPin.setBackground(createPillBackground(_colorKeyActivated, dp(16), Color.TRANSPARENT));
    _btnPin.setPadding(dp(12), 0, dp(12), 0);
    _btnPin.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        String text = _etInput.getText() != null ? _etInput.getText().toString().trim() : "";
        if (!text.isEmpty() && _listener != null)
        {
          _listener.onPin(text);
        }
      }
    });
    LinearLayout.LayoutParams lpPin = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
    lpPin.setMargins(dp(6), 0, 0, 0);
    addView(_btnPin, lpPin);
  }

  public void reset()
  {
    if (_etInput != null)
    {
      _etInput.setText("");
    }
  }

  public void applyTheme(int colorKeyboard, int colorKey, int colorLabel, int colorKeyActivated)
  {
    _colorKeyboard = colorKeyboard;
    _colorKey = colorKey;
    _colorLabel = colorLabel;
    _colorKeyActivated = colorKeyActivated;

    setBackgroundColor(_colorKeyboard);
    if (_btnCancel != null)
    {
      _btnCancel.setTextColor(_colorLabel);
      _btnCancel.setBackground(createPillBackground(_colorKey, dp(16), Color.TRANSPARENT));
    }
    if (_etInput != null)
    {
      _etInput.setTextColor(_colorLabel);
      _etInput.setHintTextColor(adjustAlpha(_colorLabel, 0.45f));
      _etInput.setBackground(createPillBackground(_colorKey, dp(16), adjustAlpha(_colorLabel, 0.2f)));
    }
    if (_btnPin != null)
    {
      _btnPin.setBackground(createPillBackground(_colorKeyActivated, dp(16), Color.TRANSPARENT));
    }
  }

  public InputConnection createInputConnection()
  {
    return new BaseInputConnection(_etInput, true)
    {
      @Override
      public Editable getEditable()
      {
        return _etInput.getText();
      }

      @Override
      public boolean commitText(CharSequence text, int newCursorPosition)
      {
        if (text == null) return true;
        int start = _etInput.getSelectionStart();
        int end = _etInput.getSelectionEnd();
        if (start < 0) start = _etInput.length();
        if (end < 0) end = _etInput.length();
        if (start > end) { int t = start; start = end; end = t; }

        _etInput.getText().replace(start, end, text);
        int newCursor = start + text.length();
        _etInput.setSelection(Math.min(newCursor, _etInput.length()));
        return true;
      }

      @Override
      public boolean deleteSurroundingText(int beforeLength, int afterLength)
      {
        int start = _etInput.getSelectionStart();
        int end = _etInput.getSelectionEnd();
        if (start < 0) start = _etInput.length();
        if (end < 0) end = _etInput.length();
        if (start > end) { int t = start; start = end; end = t; }

        if (start != end)
        {
          _etInput.getText().delete(start, end);
          return true;
        }

        int delStart = Math.max(0, start - beforeLength);
        int delEnd = Math.min(_etInput.length(), end + afterLength);
        if (delStart < delEnd)
        {
          _etInput.getText().delete(delStart, delEnd);
          _etInput.setSelection(delStart);
        }
        return true;
      }

      @Override
      public boolean sendKeyEvent(KeyEvent event)
      {
        if (event.getAction() == KeyEvent.ACTION_DOWN)
        {
          if (event.getKeyCode() == KeyEvent.KEYCODE_DEL)
          {
            return deleteSurroundingText(1, 0);
          }
          else if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
          {
            if (_btnPin != null)
            {
              _btnPin.performClick();
              return true;
            }
          }
        }
        return super.sendKeyEvent(event);
      }

      @Override
      public CharSequence getTextBeforeCursor(int n, int flags)
      {
        int start = _etInput.getSelectionStart();
        if (start <= 0) return "";
        int from = Math.max(0, start - n);
        return _etInput.getText().subSequence(from, start);
      }

      @Override
      public CharSequence getTextAfterCursor(int n, int flags)
      {
        int end = _etInput.getSelectionEnd();
        if (end < 0 || end >= _etInput.length()) return "";
        int to = Math.min(_etInput.length(), end + n);
        return _etInput.getText().subSequence(end, to);
      }
    };
  }
}
