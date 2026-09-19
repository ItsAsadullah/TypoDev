package juloo.keyboard2.fancy;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import juloo.keyboard2.Keyboard2;
import juloo.keyboard2.Logs;
import juloo.keyboard2.R;

/**
 * In-Keyboard embedded view for Fancy Letters, ASCII Fonts, and Text Art.
 * Replaces the keyboard layout directly inside the keyboard window.
 * 100% offline, zero latency, crash-proof.
 */
public class FancyPaneView extends LinearLayout
{
  private Keyboard2 _keyboard;
  private float _density;
  private int _bottomSafety = 0;

  // Theme colors
  private int _colorKeyboard;
  private int _colorKey;
  private int _colorKeyActivated;
  private int _colorLabel;
  private int _colorSubLabel;

  // Context & Text State
  private String _inputFieldText = "";
  private String _clipboardText = "";
  private boolean _hasSelection = false;
  private String _currentWorkingText = "TypoDev";
  private boolean _usingClipboard = false;

  // Current Active Tab: "fonts", "text_art", "kaomoji"
  private String _activeTab = "fonts";

  // Views
  private TextView _tvSourceBadge;
  private Button _btnToggleSource;
  private TextView _tvInputSnippet;
  private Button _btnTabFonts;
  private Button _btnTabTextArt;
  private Button _btnTabKaomoji;
  private ScrollView _scrollCards;
  private LinearLayout _containerCards;

  public FancyPaneView(Context context)
  {
    super(context);
    initLayout(context);
  }

  public FancyPaneView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    initLayout(context);
  }

  public FancyPaneView(Context context, AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    initLayout(context);
  }

  public void init(Keyboard2 keyboard)
  {
    _keyboard = keyboard;
  }

  private void initThemeColors(Context context)
  {
    _colorKeyboard = resolveColor(context, R.attr.colorKeyboard, Color.parseColor("#1B1B1B"));
    _colorKey = resolveColor(context, R.attr.colorKey, Color.parseColor("#2C2C2C"));
    _colorKeyActivated = resolveColor(context, R.attr.colorKeyActivated, Color.parseColor("#2196F3"));
    _colorLabel = resolveColor(context, R.attr.colorLabel, Color.WHITE);
    _colorSubLabel = resolveColor(context, R.attr.colorSubLabel, Color.parseColor("#AAAAAA"));
  }

  private int resolveColor(Context context, int attrId, int defaultVal)
  {
    try
    {
      TypedValue tv = new TypedValue();
      if (context.getTheme().resolveAttribute(attrId, tv, true))
      {
        if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT)
        {
          return tv.data;
        }
      }
    }
    catch (Exception ignored) {}
    return defaultVal;
  }

  private int dp(float dp)
  {
    return (int)(dp * _density + 0.5f);
  }

  private void updateBottomPadding(int rawBottomSafety)
  {
    int minSafeBottom = dp(36);
    int newBottomSafety = Math.max(rawBottomSafety, minSafeBottom);
    _bottomSafety = newBottomSafety;
    if (_containerCards != null)
    {
      _containerCards.setPadding(dp(8), dp(4), dp(8), dp(6) + _bottomSafety);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);
    if (heightMode == MeasureSpec.UNSPECIFIED || (heightMode == MeasureSpec.AT_MOST && heightSize <= 0))
    {
      int fallbackHeight = (int)(280 * _density + 0.5f);
      heightMeasureSpec = MeasureSpec.makeMeasureSpec(fallbackHeight, MeasureSpec.EXACTLY);
    }
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  private void initLayout(final Context context)
  {
    _density = context.getResources().getDisplayMetrics().density;
    initThemeColors(context);

    setOrientation(VERTICAL);
    setBackgroundColor(_colorKeyboard);

    // ==========================================
    // 1. Top Header Bar
    // ==========================================
    LinearLayout headerBar = new LinearLayout(context);
    headerBar.setOrientation(HORIZONTAL);
    headerBar.setGravity(Gravity.CENTER_VERTICAL);
    headerBar.setPadding(dp(8), dp(2), dp(8), dp(2));

    // Single back arrow to return to keyboard
    Button btnBack = new Button(context);
    btnBack.setText("←");
    btnBack.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
    btnBack.setTextColor(_colorLabel);
    btnBack.setBackground(createPillBackground(Color.TRANSPARENT, dp(18), adjustAlpha(_colorLabel, 0.15f)));
    btnBack.setPadding(0, 0, 0, 0);
    btnBack.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_keyboard != null) _keyboard.closeFancyPane();
      }
    });
    headerBar.addView(btnBack, new LinearLayout.LayoutParams(dp(36), dp(36)));

    TextView tvTitle = new TextView(context);
    tvTitle.setText("𝓕 Fancy Fonts & Text Art");
    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    tvTitle.setTypeface(null, Typeface.BOLD);
    tvTitle.setTextColor(_colorLabel);
    tvTitle.setPadding(dp(6), 0, dp(6), 0);
    headerBar.addView(tvTitle);

    View spacerHeader = new View(context);
    headerBar.addView(spacerHeader, new LinearLayout.LayoutParams(0, 1, 1.0f));

    Button btnClose = new Button(context);
    btnClose.setText("✕");
    btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    btnClose.setTextColor(_colorLabel);
    btnClose.setBackground(createPillBackground(Color.TRANSPARENT, dp(16), adjustAlpha(_colorLabel, 0.15f)));
    btnClose.setPadding(0, 0, 0, 0);
    btnClose.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_keyboard != null) _keyboard.closeFancyPane();
      }
    });
    headerBar.addView(btnClose, new LinearLayout.LayoutParams(dp(32), dp(32)));

    addView(headerBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

    // ==========================================
    // 2. Source Context Card
    // ==========================================
    LinearLayout cardContext = new LinearLayout(context);
    cardContext.setOrientation(HORIZONTAL);
    cardContext.setGravity(Gravity.CENTER_VERTICAL);
    cardContext.setPadding(dp(10), dp(4), dp(10), dp(4));
    cardContext.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.45f), dp(8), adjustAlpha(_colorLabel, 0.12f)));
    LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpCard.setMargins(dp(8), dp(1), dp(8), dp(2));
    cardContext.setLayoutParams(lpCard);

    _tvSourceBadge = new TextView(context);
    _tvSourceBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    _tvSourceBadge.setTypeface(null, Typeface.BOLD);
    _tvSourceBadge.setTextColor(_colorLabel);
    cardContext.addView(_tvSourceBadge);

    _tvInputSnippet = new TextView(context);
    _tvInputSnippet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _tvInputSnippet.setSingleLine(true);
    _tvInputSnippet.setEllipsize(TextUtils.TruncateAt.END);
    _tvInputSnippet.setPadding(dp(6), 0, dp(6), 0);
    _tvInputSnippet.setTextColor(adjustAlpha(_colorLabel, 0.85f));
    cardContext.addView(_tvInputSnippet, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

    _btnToggleSource = new Button(context);
    _btnToggleSource.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
    _btnToggleSource.setTextColor(_colorLabel);
    _btnToggleSource.setPadding(dp(6), dp(1), dp(6), dp(1));
    _btnToggleSource.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.7f), dp(8), adjustAlpha(_colorLabel, 0.2f)));
    _btnToggleSource.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        _usingClipboard = !_usingClipboard;
        _currentWorkingText = _usingClipboard ? _clipboardText : _inputFieldText;
        if (_currentWorkingText == null || _currentWorkingText.trim().isEmpty())
        {
          _currentWorkingText = "TypoDev";
        }
        updateContextDisplay();
        renderActiveTab();
      }
    });
    cardContext.addView(_btnToggleSource, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(24)));
    addView(cardContext);

    // ==========================================
    // 3. Category Tabs Bar
    // ==========================================
    HorizontalScrollView scrollTabs = new HorizontalScrollView(context);
    scrollTabs.setHorizontalScrollBarEnabled(false);
    scrollTabs.setVerticalScrollBarEnabled(false);
    scrollTabs.setOverScrollMode(View.OVER_SCROLL_NEVER);

    LinearLayout rowTabs = new LinearLayout(context);
    rowTabs.setOrientation(HORIZONTAL);
    rowTabs.setPadding(dp(8), dp(2), dp(8), dp(2));

    _btnTabFonts = createTabButton("𝓕 Fonts", "fonts");
    _btnTabTextArt = createTabButton("✦ Text Art", "text_art");
    _btnTabKaomoji = createTabButton("(✿◠‿◠) Kaomoji", "kaomoji");

    rowTabs.addView(_btnTabFonts);
    rowTabs.addView(_btnTabTextArt);
    rowTabs.addView(_btnTabKaomoji);

    scrollTabs.addView(rowTabs);
    addView(scrollTabs, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    // ==========================================
    // 4. Scrollable Content Container (Cards)
    // ==========================================
    _scrollCards = new ScrollView(context);
    _scrollCards.setFillViewport(true);
    _scrollCards.setVerticalScrollBarEnabled(false);
    _scrollCards.setHorizontalScrollBarEnabled(false);
    _scrollCards.setOverScrollMode(View.OVER_SCROLL_NEVER);

    _containerCards = new LinearLayout(context);
    _containerCards.setOrientation(VERTICAL);
    _bottomSafety = dp(36);
    _containerCards.setPadding(dp(8), dp(4), dp(8), dp(6) + _bottomSafety);

    _scrollCards.addView(_containerCards);
    addView(_scrollCards, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

    updateTabButtons();
  }

  private Button createTabButton(final String label, final String tabId)
  {
    Button btn = new Button(getContext());
    btn.setText(label);
    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    btn.setPadding(dp(10), 0, dp(10), 0);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(28));
    lp.setMargins(0, 0, dp(6), 0);
    btn.setLayoutParams(lp);

    btn.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        _activeTab = tabId;
        updateTabButtons();
        renderActiveTab();
      }
    });
    return btn;
  }

  private void updateTabButtons()
  {
    int activeBg = (_colorKeyActivated != 0) ? _colorKeyActivated : Color.parseColor("#2196F3");

    styleTabButton(_btnTabFonts, "fonts".equals(_activeTab), activeBg);
    styleTabButton(_btnTabTextArt, "text_art".equals(_activeTab), activeBg);
    styleTabButton(_btnTabKaomoji, "kaomoji".equals(_activeTab), activeBg);
  }

  private void styleTabButton(Button btn, boolean isSelected, int activeBg)
  {
    if (btn == null) return;
    if (isSelected)
    {
      btn.setBackground(createPillBackground(activeBg, dp(12), Color.TRANSPARENT));
      btn.setTextColor(Color.WHITE);
      btn.setTypeface(null, Typeface.BOLD);
    }
    else
    {
      btn.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.5f), dp(12), adjustAlpha(_colorLabel, 0.15f)));
      btn.setTextColor(adjustAlpha(_colorLabel, 0.8f));
      btn.setTypeface(null, Typeface.NORMAL);
    }
  }

  public void open(int targetHeight, int bottomSafety)
  {
    try
    {
      Context context = getContext();
      initThemeColors(context);
      setBackgroundColor(_colorKeyboard);

      updateBottomPadding(bottomSafety);

      if (targetHeight > 0)
      {
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (lp != null && lp.height != targetHeight)
        {
          lp.height = targetHeight;
          setLayoutParams(lp);
        }
      }

      // 1. Extract text from InputConnection
      _inputFieldText = "";
      _hasSelection = false;
      if (_keyboard != null)
      {
        try
        {
          InputConnection ic = _keyboard.getCurrentInputConnection();
          if (ic != null)
          {
            CharSequence sel = ic.getSelectedText(0);
            if (sel != null && sel.length() > 0)
            {
              _inputFieldText = sel.toString();
              _hasSelection = true;
            }
            else
            {
              CharSequence before = ic.getTextBeforeCursor(200, 0);
              if (before != null && before.length() > 0)
              {
                _inputFieldText = before.toString().trim();
              }
            }
          }
        }
        catch (Throwable t)
        {
          Logs.print_exception(t);
        }
      }

      // 2. Extract clipboard
      _clipboardText = "";
      try
      {
        ClipboardManager cm = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null && cm.hasPrimaryClip())
        {
          ClipData clip = cm.getPrimaryClip();
          if (clip != null && clip.getItemCount() > 0)
          {
            ClipData.Item item = clip.getItemAt(0);
            if (item != null)
            {
              CharSequence text = item.coerceToText(context);
              if (text != null)
              {
                _clipboardText = text.toString().trim();
              }
            }
          }
        }
      }
      catch (Throwable ignored) {}

      // Prefer selection, then typed text, then clipboard, then fallback
      if (_hasSelection && !_inputFieldText.isEmpty())
      {
        _currentWorkingText = _inputFieldText;
        _usingClipboard = false;
      }
      else if (!_inputFieldText.isEmpty())
      {
        _currentWorkingText = _inputFieldText;
        _usingClipboard = false;
      }
      else if (!_clipboardText.isEmpty())
      {
        _currentWorkingText = _clipboardText;
        _usingClipboard = true;
      }
      else
      {
        _currentWorkingText = "TypoDev";
        _usingClipboard = false;
      }

      updateContextDisplay();
      updateTabButtons();
      renderActiveTab();
    }
    catch (Throwable t)
    {
      Logs.print_exception(t);
    }
  }

  private void updateContextDisplay()
  {
    if (_usingClipboard)
    {
      _tvSourceBadge.setText("📋 Clip: ");
      _btnToggleSource.setText("Use Typed");
    }
    else if (_hasSelection)
    {
      _tvSourceBadge.setText("📝 Select: ");
      _btnToggleSource.setText("Use Clip");
    }
    else
    {
      _tvSourceBadge.setText("✍️ Text: ");
      _btnToggleSource.setText("Use Clip");
    }
    _tvInputSnippet.setText("\"" + _currentWorkingText + "\"");
  }

  private void renderActiveTab()
  {
    if (_containerCards == null) return;
    _containerCards.removeAllViews();

    if ("fonts".equals(_activeTab))
    {
      renderFontStyles();
    }
    else if ("text_art".equals(_activeTab))
    {
      renderTextArtStyles();
    }
    else if ("kaomoji".equals(_activeTab))
    {
      renderKaomojiLibrary();
    }
  }

  private void renderFontStyles()
  {
    List<FancyTextEngine.StyleItem> items = FancyTextEngine.getFontStyles();
    for (final FancyTextEngine.StyleItem item : items)
    {
      final String transformed = FancyTextEngine.applyStyle(item.id, _currentWorkingText);
      _containerCards.addView(createStyleCard(item.name, transformed));
    }
  }

  private void renderTextArtStyles()
  {
    List<FancyTextEngine.StyleItem> items = FancyTextEngine.getTextArtStyles();
    for (final FancyTextEngine.StyleItem item : items)
    {
      final String transformed = FancyTextEngine.applyStyle(item.id, _currentWorkingText);
      _containerCards.addView(createStyleCard(item.name, transformed));
    }
  }

  private void renderKaomojiLibrary()
  {
    List<FancyTextEngine.KaomojiCategory> categories = FancyTextEngine.getKaomojiCategories();
    Context context = getContext();

    for (final FancyTextEngine.KaomojiCategory cat : categories)
    {
      // Category Title
      TextView tvCatTitle = new TextView(context);
      tvCatTitle.setText(cat.name);
      tvCatTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      tvCatTitle.setTypeface(null, Typeface.BOLD);
      tvCatTitle.setTextColor(adjustAlpha(_colorLabel, 0.85f));
      tvCatTitle.setPadding(dp(4), dp(6), dp(4), dp(3));
      _containerCards.addView(tvCatTitle);

      // Horizontal flow of chips for this category
      HorizontalScrollView hScroll = new HorizontalScrollView(context);
      hScroll.setHorizontalScrollBarEnabled(false);
      hScroll.setVerticalScrollBarEnabled(false);
      hScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

      LinearLayout rowChips = new LinearLayout(context);
      rowChips.setOrientation(HORIZONTAL);
      rowChips.setPadding(dp(2), dp(2), dp(2), dp(4));

      for (final String kaomoji : cat.items)
      {
        Button chip = new Button(context);
        chip.setText(kaomoji);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        chip.setTextColor(_colorLabel);
        chip.setPadding(dp(10), 0, dp(10), 0);
        chip.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.45f), dp(10), adjustAlpha(_colorLabel, 0.15f)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        lp.setMargins(0, 0, dp(6), 0);
        chip.setLayoutParams(lp);

        chip.setOnClickListener(new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            applyTextToHostApp(kaomoji);
          }
        });

        rowChips.addView(chip);
      }

      hScroll.addView(rowChips);
      _containerCards.addView(hScroll);
    }
  }

  private View createStyleCard(String styleName, final String resultText)
  {
    final Context context = getContext();
    LinearLayout card = new LinearLayout(context);
    card.setOrientation(HORIZONTAL);
    card.setGravity(Gravity.CENTER_VERTICAL);
    card.setPadding(dp(10), dp(8), dp(10), dp(8));
    card.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.4f), dp(8), adjustAlpha(_colorLabel, 0.15f)));

    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.setMargins(0, dp(3), 0, dp(3));
    card.setLayoutParams(lp);

    // Left content: Title & Transformed Text
    LinearLayout leftCol = new LinearLayout(context);
    leftCol.setOrientation(VERTICAL);

    TextView tvStyle = new TextView(context);
    tvStyle.setText(styleName);
    tvStyle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    tvStyle.setTextColor(adjustAlpha(_colorLabel, 0.55f));
    leftCol.addView(tvStyle);

    TextView tvResult = new TextView(context);
    tvResult.setText(resultText);
    tvResult.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    tvResult.setTypeface(null, Typeface.BOLD);
    tvResult.setTextColor(_colorLabel);
    tvResult.setPadding(0, dp(2), 0, 0);
    leftCol.addView(tvResult);

    card.addView(leftCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

    // Right Action: Insert & Copy Pill Button
    Button btnInsert = new Button(context);
    btnInsert.setText("Insert ↵");
    btnInsert.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    btnInsert.setTypeface(null, Typeface.BOLD);
    btnInsert.setTextColor(Color.WHITE);
    btnInsert.setPadding(dp(10), 0, dp(10), 0);
    int activeBg = (_colorKeyActivated != 0) ? _colorKeyActivated : Color.parseColor("#2196F3");
    btnInsert.setBackground(createPillBackground(activeBg, dp(10), Color.TRANSPARENT));

    LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
    lpBtn.setMargins(dp(6), 0, 0, 0);
    btnInsert.setLayoutParams(lpBtn);

    btnInsert.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        applyTextToHostApp(resultText);
      }
    });

    card.addView(btnInsert);

    // Tapping the card body also triggers insertion
    card.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        applyTextToHostApp(resultText);
      }
    });

    return card;
  }

  private void applyTextToHostApp(String textToApply)
  {
    if (textToApply == null || textToApply.isEmpty()) return;
    Context context = getContext();

    // 1. Copy to clipboard automatically for convenience
    try
    {
      ClipboardManager cm = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm != null)
      {
        cm.setPrimaryClip(ClipData.newPlainText("Fancy Text", textToApply));
      }
    }
    catch (Throwable ignored) {}

    // 2. Commit text directly into active input connection
    if (_keyboard != null)
    {
      try
      {
        InputConnection ic = _keyboard.getCurrentInputConnection();
        if (ic != null)
        {
          if (_hasSelection)
          {
            ic.commitText(textToApply, 1);
          }
          else if (!_usingClipboard && _inputFieldText != null && !_inputFieldText.isEmpty())
          {
            ic.deleteSurroundingText(_inputFieldText.length(), 0);
            ic.commitText(textToApply, 1);
          }
          else
          {
            ic.commitText(textToApply, 1);
          }
          Toast.makeText(context, "✅ Inserted & Copied", Toast.LENGTH_SHORT).show();
          _keyboard.closeFancyPane();
          return;
        }
      }
      catch (Throwable t)
      {
        Logs.print_exception(t);
      }
      _keyboard.closeFancyPane();
    }
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
  }

  private GradientDrawable createPillBackground(int color, float radiusDp, int strokeColor)
  {
    GradientDrawable gd = new GradientDrawable();
    gd.setCornerRadius(radiusDp * _density);
    gd.setColor(color);
    if (strokeColor != Color.TRANSPARENT)
    {
      gd.setStroke(dp(1), strokeColor);
    }
    return gd;
  }

  private int adjustAlpha(int color, float factor)
  {
    int alpha = Math.round(Color.alpha(color) * factor);
    int red = Color.red(color);
    int green = Color.green(color);
    int blue = Color.blue(color);
    return Color.argb(alpha, red, green, blue);
  }
}
