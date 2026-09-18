package juloo.keyboard2;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/**
 * Top bar view for Emoji Search Mode.
 * Displays active search query, clear/close actions, and a live scrollable row of matching emojis.
 * Routes soft keyboard key events through its custom InputConnection.
 */
public class EmojiSearchBarView extends LinearLayout
{
  public interface OnEmojiSearchActionListener
  {
    void onEmojiSelected(Emoji emoji);
    void onCloseSearch();
  }

  private OnEmojiSearchActionListener _listener = null;

  private Button _btnBack;
  private EditText _etSearch;
  private Button _btnClear;
  private HorizontalScrollView _scrollResults;
  private LinearLayout _rowResults;
  private TextView _tvEmptyNotice;

  private int _colorKeyboard = Color.parseColor("#151A23");
  private int _colorKey = Color.parseColor("#212836");
  private int _colorLabel = Color.WHITE;
  private int _colorKeyActivated = Color.parseColor("#2AABEE");

  public EmojiSearchBarView(Context context)
  {
    super(context);
    init(context);
  }

  public EmojiSearchBarView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    init(context);
  }

  public EmojiSearchBarView(Context context, AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private int dp(int val)
  {
    return (int) (val * getResources().getDisplayMetrics().density);
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
    setOrientation(VERTICAL);
    setPadding(dp(4), dp(4), dp(4), dp(4));

    // 1. Top Search Header Row: [←] [ 🔍 query... ] [✕]
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

    // Search input field
    _etSearch = new EditText(context);
    _etSearch.setHint("🔍 Search emoji (e.g. smile, love, cat, হাসি)...");
    _etSearch.setHintTextColor(Color.parseColor("#8E99A8"));
    _etSearch.setTextColor(_colorLabel);
    _etSearch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    _etSearch.setBackground(createPillBackground(_colorKey, dp(16), Color.TRANSPARENT));
    _etSearch.setPadding(dp(12), dp(4), dp(12), dp(4));
    _etSearch.setSingleLine(true);
    _etSearch.setEllipsize(TextUtils.TruncateAt.END);
    _etSearch.setFocusable(false); // Prevents OS from showing an external soft keyboard
    _etSearch.setFocusableInTouchMode(false);
    _etSearch.addTextChangedListener(new TextWatcher()
    {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int count, int after)
      {
        refreshResults(s != null ? s.toString() : "");
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });
    LinearLayout.LayoutParams lpEt = new LinearLayout.LayoutParams(0, dp(34), 1.0f);
    lpEt.setMargins(dp(4), 0, dp(4), 0);
    headerRow.addView(_etSearch, lpEt);

    // Clear / Close button
    _btnClear = new Button(context);
    _btnClear.setText("✕");
    _btnClear.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    _btnClear.setTextColor(_colorLabel);
    _btnClear.setBackground(createPillBackground(Color.TRANSPARENT, dp(14), Color.TRANSPARENT));
    _btnClear.setPadding(0, 0, 0, 0);
    _btnClear.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_etSearch != null && _etSearch.length() > 0)
        {
          _etSearch.setText("");
        }
        else
        {
          if (_listener != null) _listener.onCloseSearch();
        }
      }
    });
    headerRow.addView(_btnClear, new LinearLayout.LayoutParams(dp(32), dp(32)));

    addView(headerRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    // 2. Bottom Live Results Row (Horizontal Scroll)
    _scrollResults = new HorizontalScrollView(context);
    _scrollResults.setHorizontalScrollBarEnabled(false);
    _scrollResults.setOverScrollMode(OVER_SCROLL_NEVER);

    _rowResults = new LinearLayout(context);
    _rowResults.setOrientation(HORIZONTAL);
    _rowResults.setGravity(Gravity.CENTER_VERTICAL);
    _rowResults.setPadding(dp(4), 0, dp(4), 0);
    _scrollResults.addView(_rowResults);

    addView(_scrollResults, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

    // Notice when no matches
    _tvEmptyNotice = new TextView(context);
    _tvEmptyNotice.setText("No emojis matched");
    _tvEmptyNotice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    _tvEmptyNotice.setTextColor(Color.parseColor("#8E99A8"));
    _tvEmptyNotice.setGravity(Gravity.CENTER);
    _tvEmptyNotice.setVisibility(GONE);
    addView(_tvEmptyNotice, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));

    // Populate initial popular results
    refreshResults("");
  }

  public void setOnEmojiSearchActionListener(OnEmojiSearchActionListener listener)
  {
    _listener = listener;
  }

  public void setQuery(String query)
  {
    if (_etSearch != null)
    {
      _etSearch.setText(query != null ? query : "");
      if (query != null)
      {
        _etSearch.setSelection(query.length());
      }
    }
  }

  public void refreshResults(String query)
  {
    if (_rowResults == null) return;
    _rowResults.removeAllViews();

    List<Emoji> matches = EmojiSearchIndex.search(query, 60);
    if (matches.isEmpty())
    {
      if (_tvEmptyNotice != null) _tvEmptyNotice.setVisibility(VISIBLE);
      return;
    }

    if (_tvEmptyNotice != null) _tvEmptyNotice.setVisibility(GONE);

    int dpBtn = dp(40);
    for (final Emoji emoji : matches)
    {
      TextView tv = new TextView(getContext());
      tv.setText(emoji.kv().getString());
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
      tv.setGravity(Gravity.CENTER);
      tv.setBackground(createPillBackground(Color.TRANSPARENT, dp(8), Color.TRANSPARENT));

      tv.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (_listener != null)
          {
            _listener.onEmojiSelected(emoji);
          }
        }
      });

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpBtn, dpBtn);
      lp.setMargins(dp(1), 0, dp(1), 0);
      _rowResults.addView(tv, lp);
    }
  }

  public void applyTheme(int colorKeyboard, int colorKey, int colorLabel, int colorKeyActivated)
  {
    _colorKeyboard = colorKeyboard;
    _colorKey = colorKey;
    _colorLabel = colorLabel;
    _colorKeyActivated = colorKeyActivated;

    setBackgroundColor(colorKeyboard);
    if (_etSearch != null)
    {
      _etSearch.setBackground(createPillBackground(colorKey, dp(16), Color.TRANSPARENT));
      _etSearch.setTextColor(colorLabel);
    }
    if (_btnBack != null) _btnBack.setTextColor(colorLabel);
    if (_btnClear != null) _btnClear.setTextColor(colorLabel);
  }

  /**
   * Creates an InputConnection to allow standard keyboard keys to type into the search bar.
   */
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
