package typodev.keyboard.ai;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import typodev.keyboard.R;

/**
 * A sleek bar that sits directly above Keyboard2View when the user
 * is typing a custom prompt for Ask AI.
 *
 * Routes all keyboard input directly into this bar so the user can type
 * with TypoDev Keyboard (Bengali, English, symbols, etc.) and see
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
  private ImageView _btnCancel;
  private LinearLayout _btnConfirm;
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

    // Cancel Icon Button
    _btnCancel = new ImageView(context);
    _btnCancel.setImageResource(R.drawable.ic_ai_close);
    _btnCancel.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
    _btnCancel.setPadding(dp(9), dp(9), dp(9), dp(9));
    _btnCancel.setBackground(createPillBackground(Color.parseColor("#374151"), dp(17), Color.TRANSPARENT));
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
    _etPrompt.setHint("প্রম্পট লিখুন (Type prompt for AI)...");
    _etPrompt.setHintTextColor(Color.parseColor("#888888"));
    _etPrompt.setTextColor(Color.WHITE);
    _etPrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    _etPrompt.setBackground(createPillBackground(Color.parseColor("#2C2C2C"), dp(8), Color.parseColor("#444444")));
    _etPrompt.setPadding(dp(10), dp(6), dp(10), dp(6));
    _etPrompt.setSingleLine(true);
    _etPrompt.setHorizontallyScrolling(true);
    _etPrompt.setFocusable(false);
    _etPrompt.setFocusableInTouchMode(false);
    LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(0, dp(36), 1.0f);
    addView(_etPrompt, lpEt);

    // Confirm / Generate Button with Vector Icon
    _btnConfirm = new LinearLayout(context);
    _btnConfirm.setOrientation(HORIZONTAL);
    _btnConfirm.setGravity(Gravity.CENTER);
    _btnConfirm.setPadding(dp(10), 0, dp(10), 0);
    _btnConfirm.setBackground(createPillBackground(Color.parseColor("#059669"), dp(8), Color.TRANSPARENT));

    ImageView ivSend = new ImageView(context);
    ivSend.setImageResource(R.drawable.ic_ai_send);
    ivSend.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
    LinearLayout.LayoutParams lpSendIcon = new LinearLayout.LayoutParams(dp(14), dp(14));
    lpSendIcon.setMargins(0, 0, dp(5), 0);
    _btnConfirm.addView(ivSend, lpSendIcon);

    TextView tvSend = new TextView(context);
    tvSend.setText("Generate");
    tvSend.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    tvSend.setTypeface(null, Typeface.BOLD);
    tvSend.setTextColor(Color.WHITE);
    _btnConfirm.addView(tvSend);

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
      _btnCancel.setColorFilter(colorLabel, PorterDuff.Mode.SRC_IN);
      _btnCancel.setBackground(createPillBackground(colorKey, dp(17), adjustAlpha(colorLabel, 0.2f)));
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
    return new typodev.keyboard.InKeyboardEditTextConnection(_etPrompt, new Runnable()
    {
      @Override
      public void run()
      {
        if (_listener != null) _listener.onConfirm(getPromptText());
      }
    });
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
