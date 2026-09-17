package juloo.keyboard2.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import juloo.keyboard2.Keyboard2;
import juloo.keyboard2.R;

/**
 * In-Keyboard embedded view for AI Actions.
 * Swaps out Keyboard2View directly within the keyboard container layout
 * (like Gboard / Ridmik Keyboard) and matches the keyboard theme.
 */
public class AiPaneView extends LinearLayout
{
  private Keyboard2 _keyboard;
  private float _density;

  // Theme colors
  private int _colorKeyboard;
  private int _colorKey;
  private int _colorKeyActivated;
  private int _colorLabel;
  private int _colorSubLabel;
  private boolean _isDark;

  // Context state
  private String _inputFieldText = "";
  private String _clipboardText = "";
  private boolean _hasSelection = false;
  private String _currentWorkingText = "";
  private boolean _usingClipboard = false;

  // AI execution state
  private AiActionEngine.Category _activeCategory = AiActionEngine.Category.REWRITE;
  private String _activeOptionId = "rephrase";
  private String _activeTone = "default";
  private String _lastGeneratedResult = "";
  private boolean _isGenerating = false;

  // Views
  private TextView _tvProviderBadge;
  private TextView _tvSourceBadge;
  private Button _btnToggleSource;
  private TextView _tvInputSnippet;
  private LinearLayout _cardSetupNotice;

  private HorizontalScrollView _scrollQuick;
  private LinearLayout _rowQuickSuggestions;
  private LinearLayout _rowCategories;
  private HorizontalScrollView _scrollSubOptions;
  private LinearLayout _rowSubOptions;
  private LinearLayout _llCustomPrompt;
  private EditText _etCustomAsk;
  private Button _btnCustomSend;
  private LinearLayout _rowToneChips;

  private ProgressBar _progressBar;
  private TextView _tvStatus;
  private EditText _etResultPreview;
  private LinearLayout _rowActionButtons;

  public AiPaneView(Context context)
  {
    super(context);
    initLayout(context);
  }

  public AiPaneView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    initLayout(context);
  }

  public AiPaneView(Context context, AttributeSet attrs, int defStyleAttr)
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
    _colorKeyboard = resolveColor(context, R.attr.colorKeyboard, Color.parseColor("#202020"));
    _colorKey = resolveColor(context, R.attr.colorKey, Color.parseColor("#333333"));
    _colorKeyActivated = resolveColor(context, R.attr.colorKeyActivated, Color.parseColor("#2196F3"));
    _colorLabel = resolveColor(context, R.attr.colorLabel, Color.WHITE);
    _colorSubLabel = resolveColor(context, R.attr.colorSubLabel, Color.LTGRAY);

    // Calculate perceived brightness of background to ensure ideal contrast
    double darkness = 1 - (0.299 * Color.red(_colorKeyboard) + 0.587 * Color.green(_colorKeyboard) + 0.114 * Color.blue(_colorKeyboard)) / 255;
    _isDark = darkness >= 0.5;
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

  private void initLayout(final Context context)
  {
    _density = context.getResources().getDisplayMetrics().density;
    initThemeColors(context);

    setOrientation(VERTICAL);
    setBackgroundColor(_colorKeyboard);

    // ==========================================
    // 1. Fixed Header Bar (Compact & Sleek)
    // ==========================================
    LinearLayout headerBar = new LinearLayout(context);
    headerBar.setOrientation(HORIZONTAL);
    headerBar.setGravity(Gravity.CENTER_VERTICAL);
    headerBar.setPadding(dp(8), dp(4), dp(8), dp(4));

    // Back arrow to return to keyboard
    Button btnBack = new Button(context);
    btnBack.setText("←");
    btnBack.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    btnBack.setTextColor(_colorLabel);
    btnBack.setBackground(null);
    btnBack.setPadding(0, 0, 0, 0);
    btnBack.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_keyboard != null) _keyboard.closeAiPane();
      }
    });
    headerBar.addView(btnBack, new LinearLayout.LayoutParams(dp(36), dp(36)));

    TextView tvTitle = new TextView(context);
    tvTitle.setText("✨ AI Assistant");
    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    tvTitle.setTypeface(null, Typeface.BOLD);
    tvTitle.setTextColor(_colorLabel);
    tvTitle.setPadding(dp(4), 0, dp(6), 0);
    headerBar.addView(tvTitle);

    _tvProviderBadge = new TextView(context);
    _tvProviderBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
    _tvProviderBadge.setTextColor(_colorLabel);
    _tvProviderBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
    GradientDrawable badgeBg = new GradientDrawable();
    badgeBg.setCornerRadius(dp(10));
    badgeBg.setColor(_colorKeyActivated != 0 ? adjustAlpha(_colorKeyActivated, 0.25f) : Color.parseColor("#332196F3"));
    badgeBg.setStroke(1, adjustAlpha(_colorLabel, 0.2f));
    _tvProviderBadge.setBackground(badgeBg);
    _tvProviderBadge.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        openSettingsDialog();
      }
    });
    headerBar.addView(_tvProviderBadge);

    View spacerHeader = new View(context);
    headerBar.addView(spacerHeader, new LinearLayout.LayoutParams(0, 1, 1.0f));

    // Settings icon
    Button btnSettings = new Button(context);
    btnSettings.setText("⚙️");
    btnSettings.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    btnSettings.setTextColor(_colorLabel);
    btnSettings.setBackground(null);
    btnSettings.setPadding(0, 0, 0, 0);
    btnSettings.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        openSettingsDialog();
      }
    });
    headerBar.addView(btnSettings, new LinearLayout.LayoutParams(dp(36), dp(36)));

    // Close icon
    Button btnClose = new Button(context);
    btnClose.setText("✕");
    btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    btnClose.setTextColor(_colorLabel);
    btnClose.setBackground(null);
    btnClose.setPadding(0, 0, 0, 0);
    btnClose.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_keyboard != null) _keyboard.closeAiPane();
      }
    });
    headerBar.addView(btnClose, new LinearLayout.LayoutParams(dp(36), dp(36)));

    addView(headerBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

    // ==========================================
    // 2. Middle Scrollable Body (Takes Remaining Space)
    // ==========================================
    ScrollView scrollBody = new ScrollView(context);
    scrollBody.setFillViewport(true);
    scrollBody.setVerticalScrollBarEnabled(false);

    LinearLayout bodyContent = new LinearLayout(context);
    bodyContent.setOrientation(VERTICAL);
    bodyContent.setPadding(dp(8), dp(4), dp(8), dp(4));

    // 2.1 API Key Setup Notice (shown if not configured)
    _cardSetupNotice = new LinearLayout(context);
    _cardSetupNotice.setOrientation(HORIZONTAL);
    _cardSetupNotice.setGravity(Gravity.CENTER_VERTICAL);
    _cardSetupNotice.setPadding(dp(10), dp(8), dp(10), dp(8));
    GradientDrawable noticeBg = new GradientDrawable();
    noticeBg.setCornerRadius(dp(8));
    noticeBg.setColor(adjustAlpha(Color.parseColor("#FF9800"), 0.2f));
    noticeBg.setStroke(1, Color.parseColor("#FF9800"));
    _cardSetupNotice.setBackground(noticeBg);

    TextView tvNotice = new TextView(context);
    tvNotice.setText("⚠️ API key needed for Gemini or OpenAI");
    tvNotice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    tvNotice.setTextColor(_colorLabel);
    _cardSetupNotice.addView(tvNotice, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

    Button btnSetup = new Button(context);
    btnSetup.setText("Configure ⚙️");
    btnSetup.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    btnSetup.setTextColor(Color.WHITE);
    btnSetup.setPadding(dp(8), 0, dp(8), 0);
    GradientDrawable setupBtnBg = new GradientDrawable();
    setupBtnBg.setCornerRadius(dp(6));
    setupBtnBg.setColor(Color.parseColor("#E65100"));
    btnSetup.setBackground(setupBtnBg);
    btnSetup.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        openSettingsDialog();
      }
    });
    _cardSetupNotice.addView(btnSetup, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30)));
    _cardSetupNotice.setVisibility(GONE);
    bodyContent.addView(_cardSetupNotice);

    // 2.2 Context Inspector Card
    LinearLayout cardContext = new LinearLayout(context);
    cardContext.setOrientation(VERTICAL);
    cardContext.setPadding(dp(8), dp(6), dp(8), dp(6));
    GradientDrawable cardBg = new GradientDrawable();
    cardBg.setCornerRadius(dp(8));
    cardBg.setColor(adjustAlpha(_colorKey, 0.45f));
    cardBg.setStroke(1, adjustAlpha(_colorLabel, 0.12f));
    cardContext.setBackground(cardBg);
    LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpCard.setMargins(0, dp(4), 0, dp(4));
    cardContext.setLayoutParams(lpCard);

    LinearLayout rowContextTop = new LinearLayout(context);
    rowContextTop.setOrientation(HORIZONTAL);
    rowContextTop.setGravity(Gravity.CENTER_VERTICAL);

    _tvSourceBadge = new TextView(context);
    _tvSourceBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    _tvSourceBadge.setTypeface(null, Typeface.BOLD);
    _tvSourceBadge.setTextColor(_colorLabel);
    rowContextTop.addView(_tvSourceBadge);

    View spacerCard = new View(context);
    rowContextTop.addView(spacerCard, new LinearLayout.LayoutParams(0, 1, 1.0f));

    _btnToggleSource = new Button(context);
    _btnToggleSource.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    _btnToggleSource.setTextColor(_colorLabel);
    _btnToggleSource.setPadding(dp(8), dp(2), dp(8), dp(2));
    GradientDrawable toggleBg = new GradientDrawable();
    toggleBg.setCornerRadius(dp(10));
    toggleBg.setColor(adjustAlpha(_colorKey, 0.6f));
    toggleBg.setStroke(1, adjustAlpha(_colorLabel, 0.2f));
    _btnToggleSource.setBackground(toggleBg);
    _btnToggleSource.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        _usingClipboard = !_usingClipboard;
        _currentWorkingText = _usingClipboard ? _clipboardText : _inputFieldText;
        updateContextDisplay();
        rebuildQuickSuggestions();
      }
    });
    rowContextTop.addView(_btnToggleSource, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(26)));
    cardContext.addView(rowContextTop);

    _tvInputSnippet = new TextView(context);
    _tvInputSnippet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _tvInputSnippet.setMaxLines(2);
    _tvInputSnippet.setPadding(0, dp(4), 0, 0);
    _tvInputSnippet.setTextColor(adjustAlpha(_colorLabel, 0.85f));
    cardContext.addView(_tvInputSnippet);
    bodyContent.addView(cardContext);

    // 2.3 Smart 1-Tap Quick Action Suggestions Strip
    _scrollQuick = new HorizontalScrollView(context);
    _scrollQuick.setHorizontalScrollBarEnabled(false);
    _rowQuickSuggestions = new LinearLayout(context);
    _rowQuickSuggestions.setOrientation(HORIZONTAL);
    _rowQuickSuggestions.setGravity(Gravity.CENTER_VERTICAL);
    _scrollQuick.addView(_rowQuickSuggestions);
    LinearLayout.LayoutParams lpQuick = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpQuick.setMargins(0, dp(2), 0, dp(2));
    _scrollQuick.setLayoutParams(lpQuick);
    bodyContent.addView(_scrollQuick);

    // 2.4 Category Tabs (Horizontal Scroll)
    HorizontalScrollView scrollCat = new HorizontalScrollView(context);
    scrollCat.setHorizontalScrollBarEnabled(false);
    _rowCategories = new LinearLayout(context);
    _rowCategories.setOrientation(HORIZONTAL);
    scrollCat.addView(_rowCategories);
    LinearLayout.LayoutParams lpScrollCat = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpScrollCat.setMargins(0, dp(2), 0, dp(2));
    scrollCat.setLayoutParams(lpScrollCat);
    bodyContent.addView(scrollCat);

    // 2.5 Dynamic Sub-Options Strip
    _scrollSubOptions = new HorizontalScrollView(context);
    _scrollSubOptions.setHorizontalScrollBarEnabled(false);
    _rowSubOptions = new LinearLayout(context);
    _rowSubOptions.setOrientation(HORIZONTAL);
    _scrollSubOptions.addView(_rowSubOptions);
    bodyContent.addView(_scrollSubOptions);

    // 2.5.1 Custom Ask AI Input Box (Shown for ASK_AI)
    _llCustomPrompt = new LinearLayout(context);
    _llCustomPrompt.setOrientation(HORIZONTAL);
    _llCustomPrompt.setGravity(Gravity.CENTER_VERTICAL);
    _llCustomPrompt.setPadding(0, dp(4), 0, dp(4));

    _etCustomAsk = new EditText(context);
    _etCustomAsk.setHint("Ask anything or enter custom instruction...");
    _etCustomAsk.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _etCustomAsk.setTextColor(_colorLabel);
    _etCustomAsk.setHintTextColor(adjustAlpha(_colorLabel, 0.45f));
    GradientDrawable customInputBg = new GradientDrawable();
    customInputBg.setCornerRadius(dp(6));
    customInputBg.setColor(adjustAlpha(_colorKey, 0.5f));
    customInputBg.setStroke(1, adjustAlpha(_colorLabel, 0.25f));
    _etCustomAsk.setBackground(customInputBg);
    _etCustomAsk.setPadding(dp(8), dp(6), dp(8), dp(6));
    _llCustomPrompt.addView(_etCustomAsk, new LinearLayout.LayoutParams(0, dp(34), 1.0f));

    _btnCustomSend = new Button(context);
    _btnCustomSend.setText("Generate 🚀");
    _btnCustomSend.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    _btnCustomSend.setTextColor(Color.WHITE);
    _btnCustomSend.setPadding(dp(8), 0, dp(8), 0);
    GradientDrawable sendBg = new GradientDrawable();
    sendBg.setCornerRadius(dp(6));
    sendBg.setColor(Color.parseColor("#1976D2"));
    _btnCustomSend.setBackground(sendBg);
    _btnCustomSend.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        triggerExecution();
      }
    });
    LinearLayout.LayoutParams lpSend = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
    lpSend.setMargins(dp(6), 0, 0, 0);
    _llCustomPrompt.addView(_btnCustomSend, lpSend);
    _llCustomPrompt.setVisibility(GONE);
    bodyContent.addView(_llCustomPrompt);

    // 2.6 Universal Tone Modifier Strip
    HorizontalScrollView scrollTone = new HorizontalScrollView(context);
    scrollTone.setHorizontalScrollBarEnabled(false);
    _rowToneChips = new LinearLayout(context);
    _rowToneChips.setOrientation(HORIZONTAL);
    _rowToneChips.setGravity(Gravity.CENTER_VERTICAL);
    scrollTone.addView(_rowToneChips);
    LinearLayout.LayoutParams lpTone = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpTone.setMargins(0, dp(2), 0, dp(4));
    scrollTone.setLayoutParams(lpTone);
    bodyContent.addView(scrollTone);

    // 2.7 Status & Progress Indicator
    LinearLayout rowStatus = new LinearLayout(context);
    rowStatus.setOrientation(HORIZONTAL);
    rowStatus.setGravity(Gravity.CENTER_VERTICAL);
    rowStatus.setPadding(0, dp(2), 0, dp(2));

    _progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
    _progressBar.setVisibility(GONE);
    rowStatus.addView(_progressBar);

    _tvStatus = new TextView(context);
    _tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _tvStatus.setTextColor(adjustAlpha(_colorLabel, 0.85f));
    _tvStatus.setPadding(dp(6), 0, 0, 0);
    _tvStatus.setText("Tap an action above to generate");
    rowStatus.addView(_tvStatus);
    bodyContent.addView(rowStatus);

    // 2.8 Result Preview & Editor Box
    _etResultPreview = new EditText(context);
    _etResultPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    _etResultPreview.setTextColor(_colorLabel);
    _etResultPreview.setHintTextColor(adjustAlpha(_colorLabel, 0.45f));
    _etResultPreview.setHint("Generated result appears here. You can edit before replacing.");
    _etResultPreview.setMinLines(2);
    _etResultPreview.setMaxLines(6);
    _etResultPreview.setGravity(Gravity.TOP);
    GradientDrawable resultBg = new GradientDrawable();
    resultBg.setCornerRadius(dp(8));
    resultBg.setStroke(1, adjustAlpha(_colorLabel, 0.25f));
    resultBg.setColor(adjustAlpha(_colorKey, 0.35f));
    _etResultPreview.setBackground(resultBg);
    _etResultPreview.setPadding(dp(10), dp(8), dp(10), dp(8));
    LinearLayout.LayoutParams lpResult = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpResult.setMargins(0, dp(4), 0, dp(6));
    _etResultPreview.setLayoutParams(lpResult);
    bodyContent.addView(_etResultPreview);

    scrollBody.addView(bodyContent);
    addView(scrollBody, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

    // ==========================================
    // 3. Fixed Bottom Action Dock
    // ==========================================
    _rowActionButtons = new LinearLayout(context);
    _rowActionButtons.setOrientation(HORIZONTAL);
    _rowActionButtons.setGravity(Gravity.CENTER_VERTICAL);
    _rowActionButtons.setPadding(dp(6), dp(4), dp(6), dp(6));
    _rowActionButtons.setBackgroundColor(adjustAlpha(_colorKeyboard, 0.95f));

    Button btnReplace = createActionButton("✅ Replace", Color.parseColor("#2E7D32"), new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        applyResultToField(true);
      }
    });
    _rowActionButtons.addView(btnReplace, new LinearLayout.LayoutParams(0, dp(36), 1.2f));

    Button btnInsert = createActionButton("➕ Insert", Color.parseColor("#1565C0"), new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        applyResultToField(false);
      }
    });
    _rowActionButtons.addView(btnInsert, new LinearLayout.LayoutParams(0, dp(36), 1.0f));

    Button btnCopy = createActionButton("📋 Copy", Color.parseColor("#424242"), new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        String res = _etResultPreview.getText().toString();
        if (res.trim().isEmpty()) return;
        ClipboardManager cm = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null)
        {
          cm.setPrimaryClip(ClipData.newPlainText("AI Generated", res));
          Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
      }
    });
    _rowActionButtons.addView(btnCopy, new LinearLayout.LayoutParams(0, dp(36), 0.9f));

    Button btnRegen = createActionButton("🔄", Color.parseColor("#555555"), new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        triggerExecution();
      }
    });
    _rowActionButtons.addView(btnRegen, new LinearLayout.LayoutParams(dp(36), dp(36)));

    addView(_rowActionButtons, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
  }

  public void open(int targetHeight)
  {
    Context context = getContext();
    initThemeColors(context);
    setBackgroundColor(_colorKeyboard);

    if (targetHeight > 0)
    {
      ViewGroup.LayoutParams lp = getLayoutParams();
      if (lp != null)
      {
        lp.height = targetHeight;
        setLayoutParams(lp);
      }
    }

    // Refresh provider badge
    AiProvider provider = AiProvider.Manager.getActiveProvider(context);
    _tvProviderBadge.setText(" " + provider.getName() + " (" + provider.getActiveModel(context) + ") ");

    // Check if API key is present
    boolean hasKey = AiProvider.Manager.hasConfiguredApiKey(context);
    _cardSetupNotice.setVisibility(hasKey ? GONE : VISIBLE);

    // Extract text from InputConnection
    _inputFieldText = "";
    _hasSelection = false;
    if (_keyboard != null)
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
          CharSequence before = ic.getTextBeforeCursor(1500, 0);
          if (before != null && before.length() > 0)
          {
            _inputFieldText = before.toString();
          }
        }
      }
    }

    // Extract clipboard
    _clipboardText = "";
    try
    {
      ClipboardManager cm = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm != null && cm.hasPrimaryClip())
      {
        ClipData.Item item = cm.getPrimaryClip().getItemAt(0);
        if (item != null && item.getText() != null)
        {
          _clipboardText = item.getText().toString().trim();
        }
      }
    }
    catch (Exception ignored) {}

    if (_inputFieldText.trim().isEmpty() && !_clipboardText.isEmpty())
    {
      _currentWorkingText = _clipboardText;
      _usingClipboard = true;
    }
    else
    {
      _currentWorkingText = _inputFieldText;
      _usingClipboard = false;
    }

    updateContextDisplay();
    rebuildQuickSuggestions();
    buildCategoryChips();
    buildSubOptionChips();
    buildToneChips();
  }

  private void openSettingsDialog()
  {
    if (_keyboard == null) return;
    AiSettingsDialog.show(getContext(), _keyboard, new AiSettingsDialog.OnSettingsSavedListener()
    {
      @Override
      public void onSaved()
      {
        Context ctx = getContext();
        AiProvider provider = AiProvider.Manager.getActiveProvider(ctx);
        _tvProviderBadge.setText(" " + provider.getName() + " (" + provider.getActiveModel(ctx) + ") ");
        _cardSetupNotice.setVisibility(AiProvider.Manager.hasConfiguredApiKey(ctx) ? GONE : VISIBLE);
      }
    });
  }

  private void updateContextDisplay()
  {
    if (_usingClipboard)
    {
      _tvSourceBadge.setText("📋 Clipboard Text");
      _btnToggleSource.setText("Use Typed Text");
    }
    else if (_hasSelection)
    {
      _tvSourceBadge.setText("📝 Selected Text");
      _btnToggleSource.setText("Use Clipboard");
    }
    else
    {
      _tvSourceBadge.setText("✍️ Input Text");
      _btnToggleSource.setText("Use Clipboard");
    }

    if (_currentWorkingText == null || _currentWorkingText.trim().isEmpty())
    {
      _tvInputSnippet.setText("(No text available. Type in app or copy to clipboard)");
      _tvInputSnippet.setTextColor(adjustAlpha(_colorLabel, 0.4f));
    }
    else
    {
      _tvInputSnippet.setText("\"" + _currentWorkingText.trim() + "\"");
      _tvInputSnippet.setTextColor(adjustAlpha(_colorLabel, 0.85f));
    }
  }

  private void rebuildQuickSuggestions()
  {
    _rowQuickSuggestions.removeAllViews();
    AiActionEngine.ContextDetectionResult analysis = AiActionEngine.analyzeContext(_currentWorkingText);
    if (analysis.quickSuggestions.isEmpty())
    {
      _scrollQuick.setVisibility(GONE);
      return;
    }

    _scrollQuick.setVisibility(VISIBLE);
    TextView label = new TextView(getContext());
    label.setText("💡 Quick: ");
    label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    label.setTextColor(adjustAlpha(_colorLabel, 0.6f));
    label.setGravity(Gravity.CENTER_VERTICAL);
    _rowQuickSuggestions.addView(label);

    for (final String sugg : analysis.quickSuggestions)
    {
      Button chip = new Button(getContext());
      chip.setText(sugg);
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
      chip.setTextColor(_colorLabel);
      chip.setPadding(dp(8), 0, dp(8), 0);
      GradientDrawable bg = new GradientDrawable();
      bg.setCornerRadius(dp(12));
      bg.setColor(adjustAlpha(_colorKeyActivated != 0 ? _colorKeyActivated : Color.parseColor("#2196F3"), 0.2f));
      bg.setStroke(1, adjustAlpha(_colorKeyActivated != 0 ? _colorKeyActivated : Color.parseColor("#2196F3"), 0.5f));
      chip.setBackground(bg);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, dp(26));
      lp.setMargins(dp(2), 0, dp(4), 0);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          _activeCategory = AiActionEngine.Category.ASK_AI;
          buildCategoryChips();
          _etCustomAsk.setText(sugg);
          triggerExecution();
        }
      });

      _rowQuickSuggestions.addView(chip);
    }
  }

  private void buildCategoryChips()
  {
    _rowCategories.removeAllViews();
    for (final AiActionEngine.Category cat : AiActionEngine.Category.values())
    {
      final boolean isSelected = (cat == _activeCategory);
      Button chip = new Button(getContext());
      chip.setText(cat.getTitle());
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
      chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
      chip.setPadding(dp(8), 0, dp(8), 0);

      GradientDrawable bg = new GradientDrawable();
      bg.setCornerRadius(dp(12));
      if (isSelected)
      {
        int activeBg = (_colorKeyActivated != 0) ? _colorKeyActivated : Color.parseColor("#2196F3");
        bg.setColor(activeBg);
        chip.setTextColor(Color.WHITE);
      }
      else
      {
        bg.setColor(adjustAlpha(_colorKey, 0.45f));
        bg.setStroke(1, adjustAlpha(_colorLabel, 0.15f));
        chip.setTextColor(adjustAlpha(_colorLabel, 0.8f));
      }
      chip.setBackground(bg);

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, dp(28));
      lp.setMargins(dp(2), 0, dp(3), 0);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          _activeCategory = cat;
          List<AiActionEngine.ActionOption> opts = AiActionEngine.getOptionsForCategory(cat);
          if (!opts.isEmpty())
          {
            _activeOptionId = opts.get(0).id;
          }
          buildCategoryChips();
          buildSubOptionChips();
          if (_activeCategory != AiActionEngine.Category.ASK_AI)
          {
            triggerExecution();
          }
        }
      });

      _rowCategories.addView(chip);
    }
  }

  private void buildSubOptionChips()
  {
    _rowSubOptions.removeAllViews();
    if (_activeCategory == AiActionEngine.Category.ASK_AI)
    {
      _scrollSubOptions.setVisibility(GONE);
      _llCustomPrompt.setVisibility(VISIBLE);
      return;
    }

    _scrollSubOptions.setVisibility(VISIBLE);
    _llCustomPrompt.setVisibility(GONE);

    List<AiActionEngine.ActionOption> options = AiActionEngine.getOptionsForCategory(_activeCategory);
    for (final AiActionEngine.ActionOption opt : options)
    {
      final boolean isSelected = opt.id.equals(_activeOptionId);
      Button chip = new Button(getContext());
      chip.setText(opt.label);
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
      chip.setPadding(dp(7), 0, dp(7), 0);

      GradientDrawable bg = new GradientDrawable();
      bg.setCornerRadius(dp(10));
      if (isSelected)
      {
        int accent = (_colorKeyActivated != 0) ? _colorKeyActivated : Color.parseColor("#2196F3");
        bg.setColor(adjustAlpha(accent, 0.3f));
        bg.setStroke(1, accent);
        chip.setTextColor(_colorLabel);
        chip.setTypeface(null, Typeface.BOLD);
      }
      else
      {
        bg.setColor(adjustAlpha(_colorKey, 0.35f));
        bg.setStroke(1, adjustAlpha(_colorLabel, 0.12f));
        chip.setTextColor(adjustAlpha(_colorLabel, 0.7f));
      }
      chip.setBackground(bg);

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, dp(26));
      lp.setMargins(dp(2), 0, dp(3), 0);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          _activeOptionId = opt.id;
          buildSubOptionChips();
          triggerExecution();
        }
      });

      _rowSubOptions.addView(chip);
    }
  }

  private void buildToneChips()
  {
    _rowToneChips.removeAllViews();
    TextView lbl = new TextView(getContext());
    lbl.setText("Tone: ");
    lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
    lbl.setTextColor(adjustAlpha(_colorLabel, 0.55f));
    lbl.setGravity(Gravity.CENTER_VERTICAL);
    _rowToneChips.addView(lbl);

    String[] tones = {"Default", "Professional", "Friendly", "Casual", "Formal", "Confident", "Funny"};
    for (final String tone : tones)
    {
      final boolean isSelected = tone.equalsIgnoreCase(_activeTone);
      Button chip = new Button(getContext());
      chip.setText(tone);
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
      chip.setPadding(dp(5), 0, dp(5), 0);

      GradientDrawable bg = new GradientDrawable();
      bg.setCornerRadius(dp(8));
      if (isSelected)
      {
        bg.setColor(adjustAlpha(Color.parseColor("#4CAF50"), 0.3f));
        bg.setStroke(1, Color.parseColor("#4CAF50"));
        chip.setTextColor(Color.parseColor("#81C784"));
      }
      else
      {
        bg.setColor(adjustAlpha(_colorKey, 0.25f));
        bg.setStroke(1, adjustAlpha(_colorLabel, 0.1f));
        chip.setTextColor(adjustAlpha(_colorLabel, 0.6f));
      }
      chip.setBackground(bg);

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, dp(22));
      lp.setMargins(dp(2), 0, dp(2), 0);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          _activeTone = tone;
          buildToneChips();
          triggerExecution();
        }
      });

      _rowToneChips.addView(chip);
    }
  }

  private void triggerExecution()
  {
    if (_isGenerating) return;
    if (_currentWorkingText == null || _currentWorkingText.trim().isEmpty())
    {
      Toast.makeText(getContext(), "No text to process. Please type or copy text first.", Toast.LENGTH_SHORT).show();
      return;
    }

    if (!AiProvider.Manager.hasConfiguredApiKey(getContext()))
    {
      openSettingsDialog();
      return;
    }

    _isGenerating = true;
    _progressBar.setVisibility(VISIBLE);
    _tvStatus.setText("Generating with " + AiProvider.Manager.getActiveProvider(getContext()).getName() + "...");

    String custom = (_activeCategory == AiActionEngine.Category.ASK_AI && _etCustomAsk != null)
        ? _etCustomAsk.getText().toString().trim() : "";

    AiActionEngine.executeAction(
        getContext(),
        _currentWorkingText,
        _activeCategory,
        _activeOptionId,
        _activeTone,
        custom,
        new AiProvider.Callback()
        {
          @Override
          public void onSuccess(final String resultText)
          {
            _isGenerating = false;
            _progressBar.setVisibility(GONE);
            _lastGeneratedResult = resultText;
            _tvStatus.setText("✅ Generated successfully");
            _etResultPreview.setText(resultText);
            _etResultPreview.setSelection(resultText.length());
          }

          @Override
          public void onError(final String errorMessage)
          {
            _isGenerating = false;
            _progressBar.setVisibility(GONE);
            _tvStatus.setText("❌ Error: " + errorMessage);
            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
          }
        });
  }

  private void applyResultToField(boolean replaceOriginal)
  {
    String output = _etResultPreview.getText().toString();
    if (output == null || output.trim().isEmpty())
    {
      Toast.makeText(getContext(), "No output to apply", Toast.LENGTH_SHORT).show();
      return;
    }

    if (_keyboard == null) return;
    InputConnection ic = _keyboard.getCurrentInputConnection();
    if (ic == null)
    {
      ClipboardManager cm = (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm != null)
      {
        cm.setPrimaryClip(ClipData.newPlainText("AI Text", output));
        Toast.makeText(getContext(), "Copied to clipboard (no active input field)", Toast.LENGTH_SHORT).show();
      }
      _keyboard.closeAiPane();
      return;
    }

    if (replaceOriginal)
    {
      if (_hasSelection)
      {
        ic.commitText(output, 1);
      }
      else if (!_usingClipboard && _inputFieldText != null && !_inputFieldText.isEmpty())
      {
        ic.deleteSurroundingText(_inputFieldText.length(), 0);
        ic.commitText(output, 1);
      }
      else
      {
        ic.commitText(output, 1);
      }
    }
    else
    {
      ic.commitText(output, 1);
    }

    Toast.makeText(getContext(), "✅ Text applied", Toast.LENGTH_SHORT).show();
    _keyboard.closeAiPane();
  }

  private Button createActionButton(String label, int bgColor, OnClickListener listener)
  {
    Button b = new Button(getContext());
    b.setText(label);
    b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    b.setTextColor(Color.WHITE);
    b.setPadding(0, 0, 0, 0);
    GradientDrawable bg = new GradientDrawable();
    bg.setCornerRadius(dp(6));
    bg.setColor(bgColor);
    b.setBackground(bg);
    b.setOnClickListener(listener);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
    lp.setMargins(dp(2), 0, dp(2), 0);
    b.setLayoutParams(lp);
    return b;
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
