package juloo.keyboard2.ai;

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
import android.widget.TextView;

/**
 * A sleek bar that sits directly above Keyboard2View when the user
 * is typing a custom prompt for Ask AI.
 *
 * Routes all keyboard input directly into this bar so the user can type
 * with Unexpected Keyboard (Bengali, English, symbols, etc.) and see
 * the text live above the keyboard keys.
 */
public class AiPromptBarView extends LinearLayout
{
  public interface OnPromptActionListener
  {
    void onConfirm(String promptText);
    void onCancel();
  }

  private EditText _etPrompt;
  private Button _btnCancel;
  private Button _btnConfirm;
  private OnPromptActionListener _listener;
  private float _density;

  public AiPromptBarView(Context context)
  {
    super(context);
    initView(context);
  }

  public AiPromptBarView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    initView(context);
  }

  public AiPromptBarView(Context context, AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    initView(context);
  }

  private int dp(float dp)
  {
    return (int)(dp * _density + 0.5f);
  }

  private void initView(Context context)
  {
    _density = context.getResources().getDisplayMetrics().density;

    setOrientation(HORIZONTAL);
    setGravity(Gravity.CENTER_VERTICAL);
    setPadding(dp(6), dp(4), dp(6), dp(4));

    // Cancel Button (←)
    _btnCancel = new Button(context);
    _btnCancel.setText("✕");
    _btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    _btnCancel.setTextColor(Color.WHITE);
    _btnCancel.setPadding(0, 0, 0, 0);
    _btnCancel.setBackground(createPillBackground(Color.parseColor("#374151"), dp(16), Color.TRANSPARENT));
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

    // Prompt Live Input / Display
    _etPrompt = new EditText(context);
    _etPrompt.setHint("🎯 প্রম্পট লিখুন (এখানে দেখা যাবে)...");
    _etPrompt.setHintTextColor(Color.parseColor("#888888"));
    _etPrompt.setTextColor(Color.WHITE);
    _etPrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    _etPrompt.setBackground(createPillBackground(Color.parseColor("#2C2C2C"), dp(8), Color.parseColor("#444444")));
    _etPrompt.setPadding(dp(10), dp(6), dp(10), dp(6));
    _etPrompt.setSingleLine(true);
    _etPrompt.setEllipsize(TextUtils.TruncateAt.END);
    _etPrompt.setFocusable(false); // Prevents Android OS from trying to pop up external IME
    _etPrompt.setFocusableInTouchMode(false);
    LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(0, dp(36), 1.0f);
    addView(_etPrompt, lpEt);

    // Confirm / OK Button (✓ ওকে 🚀)
    _btnConfirm = new Button(context);
    _btnConfirm.setText("✓ ওকে 🚀");
    _btnConfirm.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _btnConfirm.setTypeface(null, Typeface.BOLD);
    _btnConfirm.setTextColor(Color.WHITE);
    _btnConfirm.setPadding(dp(10), 0, dp(10), 0);
    _btnConfirm.setBackground(createPillBackground(Color.parseColor("#059669"), dp(8), Color.TRANSPARENT));
    _btnConfirm.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_listener != null)
        {
          _listener.onConfirm(getPromptText());
        }
      }
    });
    LinearLayout.LayoutParams lpConfirm = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
    lpConfirm.setMargins(dp(6), 0, 0, 0);
    addView(_btnConfirm, lpConfirm);
  }

  public void setOnPromptActionListener(OnPromptActionListener listener)
  {
    _listener = listener;
  }

  public void setPromptText(String text)
  {
    if (_etPrompt != null)
    {
      _etPrompt.setText(text != null ? text : "");
      if (text != null && text.length() > 0)
      {
        _etPrompt.setSelection(text.length());
      }
    }
  }

  public String getPromptText()
  {
    return (_etPrompt != null && _etPrompt.getText() != null)
        ? _etPrompt.getText().toString().trim() : "";
  }

  public void applyTheme(int colorKeyboard, int colorKey, int colorLabel, int colorKeyActivated)
  {
    setBackgroundColor(colorKeyboard);
    if (_btnCancel != null)
    {
      _btnCancel.setTextColor(colorLabel);
      _btnCancel.setBackground(createPillBackground(colorKey, dp(16), adjustAlpha(colorLabel, 0.2f)));
    }
    if (_etPrompt != null)
    {
      _etPrompt.setTextColor(colorLabel);
      _etPrompt.setHintTextColor(adjustAlpha(colorLabel, 0.45f));
      _etPrompt.setBackground(createPillBackground(colorKey, dp(8), adjustAlpha(colorLabel, 0.25f)));
    }
    if (_btnConfirm != null)
    {
      int accent = (colorKeyActivated != 0) ? colorKeyActivated : Color.parseColor("#059669");
      _btnConfirm.setBackground(createPillBackground(accent, dp(8), Color.TRANSPARENT));
    }
  }

  /**
   * Creates an InputConnection that redirects all typed characters, backspaces,
   * and text operations directly into the prompt box.
   */
  public InputConnection createInputConnection()
  {
    return new BaseInputConnection(_etPrompt, true)
    {
      @Override
      public Editable getEditable()
      {
        return _etPrompt.getText();
      }

      @Override
      public boolean commitText(CharSequence text, int newCursorPosition)
      {
        if (text == null) return true;
        int start = _etPrompt.getSelectionStart();
        int end = _etPrompt.getSelectionEnd();
        if (start < 0) start = _etPrompt.length();
        if (end < 0) end = _etPrompt.length();
        if (start > end) { int t = start; start = end; end = t; }

        _etPrompt.getText().replace(start, end, text);
        int newCursor = start + text.length();
        _etPrompt.setSelection(Math.min(newCursor, _etPrompt.length()));
        return true;
      }

      @Override
      public boolean deleteSurroundingText(int beforeLength, int afterLength)
      {
        int start = _etPrompt.getSelectionStart();
        int end = _etPrompt.getSelectionEnd();
        if (start < 0) start = _etPrompt.length();
        if (end < 0) end = _etPrompt.length();
        if (start > end) { int t = start; start = end; end = t; }

        if (start != end)
        {
          _etPrompt.getText().delete(start, end);
          return true;
        }

        int delStart = Math.max(0, start - beforeLength);
        int delEnd = Math.min(_etPrompt.length(), end + afterLength);
        if (delStart < delEnd)
        {
          _etPrompt.getText().delete(delStart, delEnd);
          _etPrompt.setSelection(delStart);
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
            if (_listener != null) _listener.onConfirm(getPromptText());
            return true;
          }
        }
        return super.sendKeyEvent(event);
      }

      @Override
      public CharSequence getTextBeforeCursor(int n, int flags)
      {
        int start = _etPrompt.getSelectionStart();
        if (start <= 0) return "";
        int from = Math.max(0, start - n);
        return _etPrompt.getText().subSequence(from, start);
      }

      @Override
      public CharSequence getTextAfterCursor(int n, int flags)
      {
        int end = _etPrompt.getSelectionEnd();
        if (end < 0 || end >= _etPrompt.length()) return "";
        int to = Math.min(_etPrompt.length(), end + n);
        return _etPrompt.getText().subSequence(end, to);
      }

      @Override
      public CharSequence getSelectedText(int flags)
      {
        int start = _etPrompt.getSelectionStart();
        int end = _etPrompt.getSelectionEnd();
        if (start < 0 || end < 0 || start == end) return "";
        if (start > end) { int t = start; start = end; end = t; }
        return _etPrompt.getText().subSequence(start, end);
      }

      @Override
      public boolean setSelection(int start, int end)
      {
        int len = _etPrompt.length();
        int s = Math.max(0, Math.min(start, len));
        int e = Math.max(0, Math.min(end, len));
        _etPrompt.setSelection(s, e);
        return true;
      }
    };
  }

  private GradientDrawable createPillBackground(int bgColor, int cornerRadius, int strokeColor)
  {
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(bgColor);
    gd.setCornerRadius(cornerRadius);
    if (strokeColor != Color.TRANSPARENT)
    {
      gd.setStroke(dp(1), strokeColor);
    }
    return gd;
  }

  private int adjustAlpha(int color, float factor)
  {
    int alpha = Math.round(Color.alpha(color) * factor);
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
  }
}
