package typodev.keyboard.clipboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import typodev.keyboard.ClipboardHistoryService;

/**
 * Top bar view for Clipboard Search Mode.
 * Displays active search query, clear/back actions, and a live horizontal carousel of matching clips.
 * Key events from the soft keyboard are routed to this view via its custom InputConnection.
 */
public class ClipboardSearchBarView extends LinearLayout
{
  public interface OnClipboardSearchActionListener
  {
    void onClipSelected(String clipContent);
    void onCloseSearch();
    void onOpenFullClipboard();
  }

  private OnClipboardSearchActionListener _listener = null;

  private Button _btnBack;
  private EditText _etSearch;
  private Button _btnClear;
  private ImageView _btnFullClipboard;
  private HorizontalScrollView _scrollResults;
  private LinearLayout _rowResults;
  private TextView _tvEmptyNotice;

  private int _colorKeyboard = Color.parseColor("#151A23");
  private int _colorKey = Color.parseColor("#212836");
  private int _colorLabel = Color.WHITE;
  private int _colorKeyActivated = Color.parseColor("#2AABEE");

  public ClipboardSearchBarView(Context context)
  {
    super(context);
    init(context);
  }

  public ClipboardSearchBarView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    init(context);
  }

  public ClipboardSearchBarView(Context context, AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  public void setOnClipboardSearchActionListener(OnClipboardSearchActionListener listener)
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

  private int blendSurface(float factor)
  {
    float inv = 1f - factor;
    int r = Math.round(Color.red(_colorKeyboard) * inv + Color.red(_colorLabel) * factor);
    int g = Math.round(Color.green(_colorKeyboard) * inv + Color.green(_colorLabel) * factor);
    int b = Math.round(Color.blue(_colorKeyboard) * inv + Color.blue(_colorLabel) * factor);
    return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
  }

  private int getSafeAccent()
  {
    double kbLum = (0.299 * Color.red(_colorKeyboard) + 0.587 * Color.green(_colorKeyboard) + 0.114 * Color.blue(_colorKeyboard)) / 255.0;
    double actLum = (0.299 * Color.red(_colorKeyActivated) + 0.587 * Color.green(_colorKeyActivated) + 0.114 * Color.blue(_colorKeyActivated)) / 255.0;
    if (Math.abs(actLum - kbLum) >= 0.22) return _colorKeyActivated;
    return (kbLum >= 0.5) ? Color.parseColor("#1A73E8") : Color.parseColor("#60A5FA");
  }

  private void init(Context context)
  {
    setOrientation(VERTICAL);
    setPadding(dp(4), dp(4), dp(4), dp(4));

    // =========================================================================
    // 1. Top Header Row: [←] [ 🔍 Search clipboard... ] [✕] [📋]
    // =========================================================================
    LinearLayout headerRow = new LinearLayout(context);
    headerRow.setOrientation(HORIZONTAL);
    headerRow.setGravity(Gravity.CENTER_VERTICAL);
    headerRow.setPadding(0, 0, 0, dp(4));

    // Back button
    _btnBack = new Button(context);
    _btnBack.setText("←");
    _btnBack.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    _btnBack.setTextColor(_colorLabel);
    _btnBack.setBackground(createPillBackground(Color.TRANSPARENT, dp(14), Color.TRANSPARENT));
    _btnBack.setPadding(0, 0, 0, 0);
    _btnBack.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_listener != null) _listener.onCloseSearch();
      }
    });
    headerRow.addView(_btnBack, new LinearLayout.LayoutParams(dp(32), dp(32)));

    // Search query box
    _etSearch = new EditText(context);
    _etSearch.setHint("🔍 Search clipboard...");
    _etSearch.setHintTextColor(adjustAlpha(_colorLabel, 0.45f));
    _etSearch.setTextColor(_colorLabel);
    _etSearch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    _etSearch.setBackground(createPillBackground(_colorKey, dp(16), adjustAlpha(_colorLabel, 0.2f)));
    _etSearch.setPadding(dp(12), dp(4), dp(12), dp(4));
    _etSearch.setSingleLine(true);
    _etSearch.setEllipsize(TextUtils.TruncateAt.END);
    _etSearch.setFocusable(false);
    _etSearch.setFocusableInTouchMode(false);
    _etSearch.addTextChangedListener(new TextWatcher()
    {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count)
      {
        String q = s != null ? s.toString() : "";
        if (_btnClear != null)
        {
          _btnClear.setVisibility(q.isEmpty() ? GONE : VISIBLE);
        }
        refreshClips(q);
      }
      @Override public void afterTextChanged(Editable s) {}
    });
    LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
    lpEt.setMargins(dp(4), 0, dp(4), 0);
    headerRow.addView(_etSearch, lpEt);

    // Clear query button
    _btnClear = new Button(context);
    _btnClear.setText("✕");
    _btnClear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    _btnClear.setTextColor(_colorLabel);
    _btnClear.setBackground(createPillBackground(Color.TRANSPARENT, dp(14), Color.TRANSPARENT));
    _btnClear.setPadding(0, 0, 0, 0);
    _btnClear.setVisibility(GONE);
    _btnClear.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_etSearch != null && _etSearch.length() > 0)
        {
          _etSearch.setText("");
        }
      }
    });
    headerRow.addView(_btnClear, new LinearLayout.LayoutParams(dp(30), dp(32)));

    // Full clipboard launcher button (vector icon)
    _btnFullClipboard = new ImageView(context);
    _btnFullClipboard.setImageResource(typodev.keyboard.R.drawable.ic_toolbar_clipboard);
    _btnFullClipboard.setColorFilter(_colorLabel);
    _btnFullClipboard.setPadding(dp(3), dp(3), dp(3), dp(3));
    _btnFullClipboard.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_listener != null) _listener.onOpenFullClipboard();
      }
    });
    headerRow.addView(_btnFullClipboard, new LinearLayout.LayoutParams(dp(32), dp(32)));

    addView(headerRow);

    // =========================================================================
    // 2. Matching Clips Carousel Row
    // =========================================================================
    _scrollResults = new HorizontalScrollView(context);
    _scrollResults.setHorizontalScrollBarEnabled(false);
    _scrollResults.setOverScrollMode(OVER_SCROLL_NEVER);
    _scrollResults.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

    _rowResults = new LinearLayout(context);
    _rowResults.setOrientation(HORIZONTAL);
    _rowResults.setGravity(Gravity.CENTER_VERTICAL);
    _scrollResults.addView(_rowResults, new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
    addView(_scrollResults);

    _tvEmptyNotice = new TextView(context);
    _tvEmptyNotice.setText("No matching clips found");
    _tvEmptyNotice.setTextColor(adjustAlpha(_colorLabel, 0.5f));
    _tvEmptyNotice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    _tvEmptyNotice.setGravity(Gravity.CENTER);
    _tvEmptyNotice.setPadding(dp(8), dp(4), dp(8), dp(4));
    _tvEmptyNotice.setVisibility(GONE);
    addView(_tvEmptyNotice);

    refreshClips("");
  }

  public void setQuery(String query)
  {
    if (_etSearch != null)
    {
      _etSearch.setText(query != null ? query : "");
      if (query != null && !query.isEmpty())
      {
        _etSearch.setSelection(query.length());
      }
    }
  }

  public void refreshClips(String query)
  {
    if (_rowResults == null) return;
    _rowResults.removeAllViews();

    final Context context = getContext();
    final String cleanQuery = (query != null) ? query.trim().toLowerCase(Locale.ROOT) : "";

    PinnedClipboardStore pinnedStore = PinnedClipboardStore.instance(context);
    List<String> rawPinned = pinnedStore.getPinnedClips();
    ClipboardHistoryService historyService = ClipboardHistoryService.get_service(context);
    List<String> rawRecent = (historyService != null)
        ? historyService.clear_expired_and_get_history() : new ArrayList<String>();

    List<ClipboardItem> matches = new ArrayList<>();

    // 1. Matching Pinned items first
    for (String s : rawPinned)
    {
      if (cleanQuery.isEmpty() || (s != null && s.toLowerCase(Locale.ROOT).contains(cleanQuery)))
      {
        matches.add(new ClipboardItem(s, true));
      }
    }

    // 2. Matching Recent items next
    for (String s : rawRecent)
    {
      if (!pinnedStore.isPinned(s))
      {
        if (cleanQuery.isEmpty() || (s != null && s.toLowerCase(Locale.ROOT).contains(cleanQuery)))
        {
          matches.add(new ClipboardItem(s, false));
        }
      }
    }

    if (matches.isEmpty())
    {
      if (_tvEmptyNotice != null) _tvEmptyNotice.setVisibility(VISIBLE);
      if (_scrollResults != null) _scrollResults.setVisibility(GONE);
      return;
    }

    if (_tvEmptyNotice != null) _tvEmptyNotice.setVisibility(GONE);
    if (_scrollResults != null) _scrollResults.setVisibility(VISIBLE);

    for (final ClipboardItem item : matches)
    {
      _rowResults.addView(createClipChip(item));
    }
  }

  private View createClipChip(final ClipboardItem item)
  {
    final Context context = getContext();
    LinearLayout chip = new LinearLayout(context);
    chip.setOrientation(HORIZONTAL);
    chip.setGravity(Gravity.CENTER_VERTICAL);
    chip.setBackground(createPillBackground(blendSurface(0.10f), 14, adjustAlpha(_colorLabel, 0.25f)));
    chip.setPadding(dp(10), dp(4), dp(10), dp(4));

    // Category / Pin Icon
    TextView tvIcon = new TextView(context);
    tvIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    tvIcon.setPadding(0, 0, dp(4), 0);
    if (item.isPinned)
    {
      tvIcon.setText("\u25c6"); // solid diamond for pinned
      tvIcon.setTextColor(getSafeAccent());
    }
    else
    {
      switch (item.category)
      {
        case URL: tvIcon.setText("\u2197"); break; // ↗ for URL
        case CODE_OTP: tvIcon.setText("#"); break; // # for OTP/code
        case EMAIL: tvIcon.setText("@"); break; // @ for email
        case PHONE: tvIcon.setText("\u2706"); break; // ✆ for phone
        default: tvIcon.setText("\u00b6"); break; // ¶ for text
      }
      tvIcon.setTextColor(adjustAlpha(_colorLabel, 0.65f));
    }
    chip.addView(tvIcon);

    // Clip snippet preview
    TextView tvText = new TextView(context);
    String preview = item.content.replaceAll("\\s+", " ").trim();
    if (preview.length() > 28)
    {
      preview = preview.substring(0, 28) + "…";
    }
    tvText.setText(preview);
    tvText.setTextColor(_colorLabel);
    tvText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    tvText.setSingleLine(true);
    chip.addView(tvText);

    chip.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); }
        catch (Throwable ignored) {}
        if (_listener != null)
        {
          _listener.onClipSelected(item.content);
        }
      }
    });

    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
    lp.setMargins(dp(3), 0, dp(3), 0);
    chip.setLayoutParams(lp);

    return chip;
  }

  public void applyTheme(int colorKeyboard, int colorKey, int colorLabel, int colorKeyActivated)
  {
    _colorKeyboard = colorKeyboard;
    _colorKey = colorKey;
    _colorLabel = colorLabel;
    _colorKeyActivated = colorKeyActivated;

    setBackgroundColor(_colorKeyboard);
    if (_btnBack != null) _btnBack.setTextColor(_colorLabel);
    if (_btnClear != null) _btnClear.setTextColor(_colorLabel);
    if (_btnFullClipboard != null) _btnFullClipboard.setColorFilter(_colorLabel);
    if (_etSearch != null)
    {
      _etSearch.setTextColor(_colorLabel);
      _etSearch.setHintTextColor(adjustAlpha(_colorLabel, 0.50f));
      _etSearch.setBackground(createPillBackground(blendSurface(0.08f), 16, adjustAlpha(_colorLabel, 0.25f)));
    }
    if (_tvEmptyNotice != null) _tvEmptyNotice.setTextColor(adjustAlpha(_colorLabel, 0.5f));
    refreshClips(_etSearch != null ? _etSearch.getText().toString() : "");
  }

  public InputConnection createInputConnection()
  {
    return new BaseInputConnection(_etSearch, true)
    {
      @Override
      public Editable getEditable()
      {
        return _etSearch.getText();
      }

      @Override
      public boolean commitText(CharSequence text, int newCursorPosition)
      {
        if (text == null) return true;
        int start = _etSearch.getSelectionStart();
        int end = _etSearch.getSelectionEnd();
        if (start < 0) start = _etSearch.length();
        if (end < 0) end = _etSearch.length();
        if (start > end) { int t = start; start = end; end = t; }

        _etSearch.getText().replace(start, end, text);
        int newCursor = start + text.length();
        _etSearch.setSelection(Math.min(newCursor, _etSearch.length()));
        return true;
      }

      @Override
      public boolean deleteSurroundingText(int beforeLength, int afterLength)
      {
        int start = _etSearch.getSelectionStart();
        int end = _etSearch.getSelectionEnd();
        if (start < 0) start = _etSearch.length();
        if (end < 0) end = _etSearch.length();
        if (start > end) { int t = start; start = end; end = t; }

        if (start != end)
        {
          _etSearch.getText().delete(start, end);
          return true;
        }

        int delStart = Math.max(0, start - beforeLength);
        int delEnd = Math.min(_etSearch.length(), end + afterLength);
        if (delStart < delEnd)
        {
          _etSearch.getText().delete(delStart, delEnd);
          _etSearch.setSelection(delStart);
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
            return true;
          }
        }
        return super.sendKeyEvent(event);
      }

      @Override
      public CharSequence getTextBeforeCursor(int n, int flags)
      {
        int start = _etSearch.getSelectionStart();
        if (start <= 0) return "";
        int from = Math.max(0, start - n);
        return _etSearch.getText().subSequence(from, start);
      }

      @Override
      public CharSequence getTextAfterCursor(int n, int flags)
      {
        int end = _etSearch.getSelectionEnd();
        if (end < 0 || end >= _etSearch.length()) return "";
        int to = Math.min(_etSearch.length(), end + n);
        return _etSearch.getText().subSequence(end, to);
      }
    };
  }
}
