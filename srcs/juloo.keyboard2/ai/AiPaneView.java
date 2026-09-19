package juloo.keyboard2.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import juloo.keyboard2.Keyboard2;
import juloo.keyboard2.Logs;
import juloo.keyboard2.R;

/**
 * In-Keyboard embedded view for AI Actions.
 * Swaps out Keyboard2View directly within the keyboard container layout
 * (Gboard / Ridmik Keyboard style) and matches the keyboard theme.
 *
 * Implements Progressive Disclosure:
 * - Line 1: Main Category Selection.
 * - Line 2: Contextual Options for the chosen category (revealed after Line 1 is tapped).
 * - Line 3: Tone Selection (ONLY revealed if needed/requested).
 * - Full user control: No automatic generation until the user confirms their specific option!
 */
public class AiPaneView extends LinearLayout
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
  private boolean _isDark;

  // Context state
  private String _inputFieldText = "";
  private String _clipboardText = "";
  private boolean _hasSelection = false;
  private String _currentWorkingText = "";
  private boolean _usingClipboard = false;

  // Progressive AI execution state
  private AiActionEngine.Category _activeCategory = null; // Unselected by default for clean start
  private String _activeOptionId = "";
  private String _activeTone = "default";
  private String _lastGeneratedResult = "";
  private boolean _isGenerating = false;

  // Views
  private TextView _tvProviderBadge;
  private Button _btnEmojifyToggle;
  private TextView _tvSourceBadge;
  private Button _btnToggleSource;
  private TextView _tvInputSnippet;
  private LinearLayout _cardSetupNotice;

  private HorizontalScrollView _scrollQuick;
  private LinearLayout _rowQuickSuggestions;
  private HorizontalScrollView _scrollCat;
  private LinearLayout _rowCategories;
  private HorizontalScrollView _scrollSubOptions;
  private LinearLayout _rowSubOptions;
  private LinearLayout _llCustomPrompt;
  private TextView _tvCustomPromptDisplay;
  private Button _btnTypePrompt;
  private Button _btnCustomSend;
  private String _currentCustomPrompt = "";
  private HorizontalScrollView _scrollTone;
  private LinearLayout _rowToneChips;

  private ProgressBar _progressBar;
  private TextView _tvStatus;
  private ScrollView _scrollBody;
  private TextView _tvResultPreview;
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
    _colorKeyboard = resolveColor(context, R.attr.colorKeyboard, Color.parseColor("#1B1B1B"));
    _colorKey = resolveColor(context, R.attr.colorKey, Color.parseColor("#2C2C2C"));
    _colorKeyActivated = resolveColor(context, R.attr.colorKeyActivated, Color.parseColor("#2196F3"));
    _colorLabel = resolveColor(context, R.attr.colorLabel, Color.WHITE);
    _colorSubLabel = resolveColor(context, R.attr.colorSubLabel, Color.parseColor("#AAAAAA"));

    double darkness = 1 - (0.299 * Color.red(_colorKeyboard) + 0.587 * Color.green(_colorKeyboard) + 0.114 * Color.blue(_colorKeyboard)) / 255;
    _isDark = darkness >= 0.5;
  }

  public int getColorKeyboard() { return _colorKeyboard; }
  public int getColorKey() { return _colorKey; }
  public int getColorLabel() { return _colorLabel; }
  public int getColorKeyActivated() { return _colorKeyActivated; }

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
    // The system navigation bar / gesture navigation pill / IME switcher globe button / down arrow
    // typically occupy 36dp - 48dp at the bottom of the IME window.
    // We ensure a safe bottom margin so the action buttons NEVER collide with the system safe area.
    int minSafeBottom = dp(36);
    int newBottomSafety = Math.max(rawBottomSafety, minSafeBottom);
    _bottomSafety = newBottomSafety;
    if (_rowActionButtons != null)
    {
      _rowActionButtons.setPadding(dp(8), dp(6), dp(8), dp(6) + _bottomSafety);
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
    // 1. Sleek Top Header Bar (Single Line)
    // ==========================================
    LinearLayout headerBar = new LinearLayout(context);
    headerBar.setOrientation(HORIZONTAL);
    headerBar.setGravity(Gravity.CENTER_VERTICAL);
    headerBar.setPadding(dp(8), dp(2), dp(8), dp(2));

    // Single back arrow to return directly to keyboard keys
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
        if (_keyboard != null) _keyboard.closeAiPane();
      }
    });
    headerBar.addView(btnBack, new LinearLayout.LayoutParams(dp(36), dp(36)));

    TextView tvTitle = new TextView(context);
    tvTitle.setText("✨ AI Assistant");
    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    tvTitle.setTypeface(null, Typeface.BOLD);
    tvTitle.setTextColor(_colorLabel);
    tvTitle.setPadding(dp(6), 0, dp(6), 0);
    headerBar.addView(tvTitle);

    _tvProviderBadge = new TextView(context);
    _tvProviderBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
    _tvProviderBadge.setTextColor(_colorLabel);
    _tvProviderBadge.setTypeface(null, Typeface.BOLD);
    _tvProviderBadge.setPadding(dp(8), dp(3), dp(8), dp(3));
    _tvProviderBadge.setBackground(createPillBackground(
        adjustAlpha(_colorKeyActivated != 0 ? _colorKeyActivated : Color.parseColor("#2196F3"), 0.25f),
        dp(12),
        adjustAlpha(_colorKeyActivated != 0 ? _colorKeyActivated : Color.parseColor("#2196F3"), 0.6f)));
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

    // Emojify Toggle Button (Matching Telegram style toggle)
    _btnEmojifyToggle = new Button(context);
    _btnEmojifyToggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    _btnEmojifyToggle.setPadding(dp(8), 0, dp(8), 0);
    updateEmojifyButtonState();
    _btnEmojifyToggle.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        boolean cur = GeminiAiService.isEmojifyEnabled(getContext());
        GeminiAiService.setEmojifyEnabled(getContext(), !cur);
        updateEmojifyButtonState();
        Toast.makeText(getContext(), !cur ? "😀 Emojify Enabled" : "⚪ No Emojis (Plain Text)", Toast.LENGTH_SHORT).show();
      }
    });
    LinearLayout.LayoutParams lpEmo = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(28));
    lpEmo.setMargins(dp(2), 0, dp(4), 0);
    headerBar.addView(_btnEmojifyToggle, lpEmo);

    // Settings icon
    Button btnSettings = new Button(context);
    btnSettings.setText("⚙️");
    btnSettings.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    btnSettings.setTextColor(_colorLabel);
    btnSettings.setBackground(createPillBackground(Color.TRANSPARENT, dp(18), adjustAlpha(_colorLabel, 0.15f)));
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

    addView(headerBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

    // ==========================================
    // 2. Middle Scrollable Body
    // ==========================================
    _scrollBody = new ScrollView(context);
    _scrollBody.setFillViewport(true);
    _scrollBody.setVerticalScrollBarEnabled(false);
    _scrollBody.setHorizontalScrollBarEnabled(false);
    _scrollBody.setOverScrollMode(View.OVER_SCROLL_NEVER);

    LinearLayout bodyContent = new LinearLayout(context);
    bodyContent.setOrientation(VERTICAL);
    bodyContent.setPadding(dp(8), dp(2), dp(8), dp(4));

    // 2.1 API Key Setup Notice (shown if not configured)
    _cardSetupNotice = new LinearLayout(context);
    _cardSetupNotice.setOrientation(HORIZONTAL);
    _cardSetupNotice.setGravity(Gravity.CENTER_VERTICAL);
    _cardSetupNotice.setPadding(dp(10), dp(6), dp(10), dp(6));
    _cardSetupNotice.setBackground(createPillBackground(adjustAlpha(Color.parseColor("#FF9800"), 0.18f), dp(8), Color.parseColor("#FF9800")));

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
    btnSetup.setBackground(createPillBackground(Color.parseColor("#E65100"), dp(6), Color.TRANSPARENT));
    btnSetup.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        openSettingsDialog();
      }
    });
    _cardSetupNotice.addView(btnSetup, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(28)));
    _cardSetupNotice.setVisibility(GONE);
    bodyContent.addView(_cardSetupNotice);

    // 2.2 Context Inspector Card
    LinearLayout cardContext = new LinearLayout(context);
    cardContext.setOrientation(VERTICAL);
    cardContext.setPadding(dp(8), dp(5), dp(8), dp(5));
    cardContext.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.45f), dp(8), adjustAlpha(_colorLabel, 0.12f)));
    LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpCard.setMargins(0, dp(2), 0, dp(3));
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
    _btnToggleSource.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.7f), dp(10), adjustAlpha(_colorLabel, 0.2f)));
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
    rowContextTop.addView(_btnToggleSource, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(24)));
    cardContext.addView(rowContextTop);

    _tvInputSnippet = new TextView(context);
    _tvInputSnippet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _tvInputSnippet.setMaxLines(2);
    _tvInputSnippet.setEllipsize(TextUtils.TruncateAt.END);
    _tvInputSnippet.setPadding(0, dp(3), 0, 0);
    _tvInputSnippet.setTextColor(adjustAlpha(_colorLabel, 0.85f));
    cardContext.addView(_tvInputSnippet);
    bodyContent.addView(cardContext);

    // 2.3 Smart 1-Tap Quick Action Suggestions Strip
    _scrollQuick = new HorizontalScrollView(context);
    _scrollQuick.setHorizontalScrollBarEnabled(false);
    _scrollQuick.setVerticalScrollBarEnabled(false);
    _scrollQuick.setOverScrollMode(View.OVER_SCROLL_NEVER);
    _rowQuickSuggestions = new LinearLayout(context);
    _rowQuickSuggestions.setOrientation(HORIZONTAL);
    _rowQuickSuggestions.setGravity(Gravity.CENTER_VERTICAL);
    _scrollQuick.addView(_rowQuickSuggestions);
    LinearLayout.LayoutParams lpQuick = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpQuick.setMargins(0, dp(1), 0, dp(2));
    _scrollQuick.setLayoutParams(lpQuick);
    bodyContent.addView(_scrollQuick);

    // 2.4 [LINE 1] Main Category Tabs (Always Shown First)
    _scrollCat = new HorizontalScrollView(context);
    _scrollCat.setHorizontalScrollBarEnabled(false);
    _scrollCat.setVerticalScrollBarEnabled(false);
    _scrollCat.setOverScrollMode(View.OVER_SCROLL_NEVER);
    _rowCategories = new LinearLayout(context);
    _rowCategories.setOrientation(HORIZONTAL);
    _scrollCat.addView(_rowCategories);
    LinearLayout.LayoutParams lpScrollCat = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpScrollCat.setMargins(0, dp(2), 0, dp(2));
    _scrollCat.setLayoutParams(lpScrollCat);
    bodyContent.addView(_scrollCat);

    // 2.5 [LINE 2] Dynamic Sub-Options Strip (Revealed ONLY after Category is chosen)
    _scrollSubOptions = new HorizontalScrollView(context);
    _scrollSubOptions.setHorizontalScrollBarEnabled(false);
    _scrollSubOptions.setVerticalScrollBarEnabled(false);
    _scrollSubOptions.setOverScrollMode(View.OVER_SCROLL_NEVER);
    _rowSubOptions = new LinearLayout(context);
    _rowSubOptions.setOrientation(HORIZONTAL);
    _scrollSubOptions.addView(_rowSubOptions);
    _scrollSubOptions.setVisibility(GONE); // Initially hidden for clean look
    bodyContent.addView(_scrollSubOptions);

    // 2.5.1 Custom Ask AI Input Display (Tapping opens keyboard typing mode)
    _llCustomPrompt = new LinearLayout(context);
    _llCustomPrompt.setOrientation(HORIZONTAL);
    _llCustomPrompt.setGravity(Gravity.CENTER_VERTICAL);
    _llCustomPrompt.setPadding(0, dp(3), 0, dp(3));

    _tvCustomPromptDisplay = new TextView(context);
    _tvCustomPromptDisplay.setText("✏️ ক্লিক করে প্রম্পট লিখুন (কিবোর্ড ওপেন হবে)...");
    _tvCustomPromptDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _tvCustomPromptDisplay.setTextColor(adjustAlpha(_colorLabel, 0.5f));
    _tvCustomPromptDisplay.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.5f), dp(6), adjustAlpha(_colorLabel, 0.25f)));
    _tvCustomPromptDisplay.setPadding(dp(8), dp(6), dp(8), dp(6));
    _tvCustomPromptDisplay.setSingleLine(true);
    _tvCustomPromptDisplay.setEllipsize(TextUtils.TruncateAt.END);
    _tvCustomPromptDisplay.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        openKeyboardForPrompt();
      }
    });
    _llCustomPrompt.addView(_tvCustomPromptDisplay, new LinearLayout.LayoutParams(0, dp(34), 1.0f));

    _btnTypePrompt = new Button(context);
    _btnTypePrompt.setText("✏️ লিখুন");
    _btnTypePrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    _btnTypePrompt.setTextColor(_colorLabel);
    _btnTypePrompt.setPadding(dp(8), 0, dp(8), 0);
    _btnTypePrompt.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.7f), dp(6), adjustAlpha(_colorLabel, 0.2f)));
    _btnTypePrompt.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        openKeyboardForPrompt();
      }
    });
    LinearLayout.LayoutParams lpType = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
    lpType.setMargins(dp(4), 0, 0, 0);
    _llCustomPrompt.addView(_btnTypePrompt, lpType);

    _btnCustomSend = new Button(context);
    _btnCustomSend.setText("Generate 🚀");
    _btnCustomSend.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    _btnCustomSend.setTextColor(Color.WHITE);
    _btnCustomSend.setPadding(dp(8), 0, dp(8), 0);
    _btnCustomSend.setBackground(createPillBackground(Color.parseColor("#1976D2"), dp(6), Color.TRANSPARENT));
    _btnCustomSend.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_currentCustomPrompt.isEmpty())
        {
          openKeyboardForPrompt();
        }
        else
        {
          triggerExecution();
        }
      }
    });
    LinearLayout.LayoutParams lpSend = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
    lpSend.setMargins(dp(4), 0, 0, 0);
    _llCustomPrompt.addView(_btnCustomSend, lpSend);
    _llCustomPrompt.setVisibility(GONE);
    bodyContent.addView(_llCustomPrompt);

    // 2.6 [LINE 3] Tone Selector Strip (Revealed ONLY if user chooses Tone or requests it)
    _scrollTone = new HorizontalScrollView(context);
    _scrollTone.setHorizontalScrollBarEnabled(false);
    _scrollTone.setVerticalScrollBarEnabled(false);
    _scrollTone.setOverScrollMode(View.OVER_SCROLL_NEVER);
    _rowToneChips = new LinearLayout(context);
    _rowToneChips.setOrientation(HORIZONTAL);
    _rowToneChips.setGravity(Gravity.CENTER_VERTICAL);
    _scrollTone.addView(_rowToneChips);
    LinearLayout.LayoutParams lpTone = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpTone.setMargins(0, dp(2), 0, dp(3));
    _scrollTone.setLayoutParams(lpTone);
    _scrollTone.setVisibility(GONE); // Initially hidden for clean look
    bodyContent.addView(_scrollTone);

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
    _tvStatus.setText("Choose an action above to start");
    rowStatus.addView(_tvStatus);
    bodyContent.addView(rowStatus);

    // 2.8 Result Preview Box (TextView prevents IMM circular binding crash in IME window)
    _tvResultPreview = new TextView(context);
    _tvResultPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    _tvResultPreview.setTextColor(_colorLabel);
    _tvResultPreview.setHintTextColor(adjustAlpha(_colorLabel, 0.45f));
    _tvResultPreview.setHint("Generated result appears here. Use buttons below to copy or insert.");
    _tvResultPreview.setMinLines(3);
    _tvResultPreview.setMaxLines(14);
    _tvResultPreview.setGravity(Gravity.TOP);
    _tvResultPreview.setVerticalScrollBarEnabled(false);
    _tvResultPreview.setHorizontalScrollBarEnabled(false);
    _tvResultPreview.setOverScrollMode(View.OVER_SCROLL_NEVER);
    _tvResultPreview.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.35f), dp(8), adjustAlpha(_colorLabel, 0.25f)));
    _tvResultPreview.setPadding(dp(10), dp(8), dp(10), dp(8));
    _tvResultPreview.setFocusable(false);
    _tvResultPreview.setFocusableInTouchMode(false);

    LinearLayout.LayoutParams lpResult = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpResult.setMargins(0, dp(3), 0, dp(4));
    _tvResultPreview.setLayoutParams(lpResult);
    bodyContent.addView(_tvResultPreview);

    _scrollBody.addView(bodyContent);
    addView(_scrollBody, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

    // ==========================================
    // 3. Fixed Bottom Action Dock (Elevated safely above Gesture Bar & IME System Buttons)
    // ==========================================
    _bottomSafety = dp(36);
    _rowActionButtons = new LinearLayout(context);
    _rowActionButtons.setOrientation(HORIZONTAL);
    _rowActionButtons.setGravity(Gravity.CENTER_VERTICAL);
    _rowActionButtons.setBackgroundColor(adjustAlpha(_colorKeyboard, 0.98f));
    _rowActionButtons.setPadding(dp(8), dp(6), dp(8), dp(6) + _bottomSafety);

    // 3.1 Replace Button (Vibrant Emerald Pill)
    Button btnReplace = createPremiumButton("✓  Replace", Color.parseColor("#059669"), Color.parseColor("#34D399"), new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        applyResultToField(true);
      }
    });
    LinearLayout.LayoutParams lpReplace = new LinearLayout.LayoutParams(0, dp(38), 1.3f);
    lpReplace.setMargins(dp(3), 0, dp(3), 0);
    _rowActionButtons.addView(btnReplace, lpReplace);

    // 3.2 Insert Button (Royal Blue Pill)
    Button btnInsert = createPremiumButton("＋  Insert", Color.parseColor("#1D4ED8"), Color.parseColor("#60A5FA"), new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        applyResultToField(false);
      }
    });
    LinearLayout.LayoutParams lpInsert = new LinearLayout.LayoutParams(0, dp(38), 1.1f);
    lpInsert.setMargins(dp(3), 0, dp(3), 0);
    _rowActionButtons.addView(btnInsert, lpInsert);

    // 3.3 Copy Button (Sleek Slate Pill)
    Button btnCopy = createPremiumButton("📋  Copy", Color.parseColor("#374151"), Color.parseColor("#6B7280"), new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        String res = _tvResultPreview.getText().toString();
        if (res.trim().isEmpty()) return;
        ClipboardManager cm = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null)
        {
          cm.setPrimaryClip(ClipData.newPlainText("AI Generated", res));
          Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
      }
    });
    LinearLayout.LayoutParams lpCopy = new LinearLayout.LayoutParams(0, dp(38), 1.0f);
    lpCopy.setMargins(dp(3), 0, dp(3), 0);
    _rowActionButtons.addView(btnCopy, lpCopy);

    // 3.4 Regenerate Icon Button (Circular Slate Pill)
    Button btnRegen = createIconActionButton("↻", Color.parseColor("#374151"), Color.parseColor("#6B7280"), new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        triggerExecution();
      }
    });
    LinearLayout.LayoutParams lpRegen = new LinearLayout.LayoutParams(dp(38), dp(38));
    lpRegen.setMargins(dp(3), 0, dp(3), 0);
    _rowActionButtons.addView(btnRegen, lpRegen);

    addView(_rowActionButtons, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
  }

  public void open(int targetHeight, int bottomSafety)
  {
    try
    {
      Context context = getContext();
      initThemeColors(context);
      setBackgroundColor(_colorKeyboard);

      // Reset progressive disclosure state so the screen is completely clean
      _activeCategory = null;
      _activeOptionId = "";
      _activeTone = "default";
      _currentCustomPrompt = "";
      updatePromptDisplay();
      if (_scrollSubOptions != null) _scrollSubOptions.setVisibility(GONE);
      if (_scrollTone != null) _scrollTone.setVisibility(GONE);
      if (_llCustomPrompt != null) _llCustomPrompt.setVisibility(GONE);
      if (_tvStatus != null) _tvStatus.setText("Select an action above to start");

      // Calculate safety space for system gesture bar / navigation bar cleanly
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

      // Refresh provider badge
      if (_tvProviderBadge != null)
      {
        AiProvider provider = AiProvider.Manager.getActiveProvider(context);
        if (provider != null)
        {
          _tvProviderBadge.setText(" ⚡ " + provider.getName() + " ");
        }
      }
      updateEmojifyButtonState();

      // Check if API key is present
      boolean hasKey = AiProvider.Manager.hasConfiguredApiKey(context);
      if (_cardSetupNotice != null)
      {
        _cardSetupNotice.setVisibility(hasKey ? GONE : VISIBLE);
      }

      // Extract text from InputConnection
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
              CharSequence before = ic.getTextBeforeCursor(1500, 0);
              if (before != null && before.length() > 0)
              {
                _inputFieldText = before.toString();
              }
            }
          }
        }
        catch (Throwable t)
        {
          Logs.print_exception(t);
        }
      }

      // Extract clipboard
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
    }
    catch (Throwable t)
    {
      Logs.print_exception(t);
    }
  }

  private long _lastSettingsOpenTime = 0;
  private void openSettingsDialog()
  {
    long now = System.currentTimeMillis();
    if (now - _lastSettingsOpenTime < 1000) return;
    _lastSettingsOpenTime = now;
    if (_keyboard == null) return;
    AiSettingsDialog.show(getContext(), _keyboard, new AiSettingsDialog.OnSettingsSavedListener()
    {
      @Override
      public void onSaved()
      {
        Context ctx = getContext();
        AiProvider provider = AiProvider.Manager.getActiveProvider(ctx);
        _tvProviderBadge.setText(" ⚡ " + provider.getName() + " ");
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

  private void updateEmojifyButtonState()
  {
    if (_btnEmojifyToggle == null) return;
    boolean enabled = GeminiAiService.isEmojifyEnabled(getContext());
    if (enabled)
    {
      _btnEmojifyToggle.setText("😀 emojify");
      int activeBg = (_colorKeyActivated != 0) ? _colorKeyActivated : Color.parseColor("#2196F3");
      _btnEmojifyToggle.setBackground(createPillBackground(activeBg, dp(12), Color.TRANSPARENT));
      _btnEmojifyToggle.setTextColor(Color.WHITE);
    }
    else
    {
      _btnEmojifyToggle.setText("⚪ plain text");
      _btnEmojifyToggle.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.5f), dp(12), adjustAlpha(_colorLabel, 0.2f)));
      _btnEmojifyToggle.setTextColor(adjustAlpha(_colorLabel, 0.7f));
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
      chip.setBackground(createPillBackground(
          adjustAlpha(_colorKeyActivated != 0 ? _colorKeyActivated : Color.parseColor("#2196F3"), 0.2f),
          dp(12),
          adjustAlpha(_colorKeyActivated != 0 ? _colorKeyActivated : Color.parseColor("#2196F3"), 0.5f)));
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, dp(26));
      lp.setMargins(dp(2), 0, dp(4), 0);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          // Direct 1-tap execution of the intended action without hijacking Ask AI
          String lower = sugg.toLowerCase(java.util.Locale.ROOT);
          if (lower.contains("professional"))
          {
            _activeCategory = AiActionEngine.Category.REWRITE;
            _activeOptionId = "prof";
          }
          else if (lower.contains("natural") || lower.contains("human"))
          {
            _activeCategory = AiActionEngine.Category.NATURALIZE;
            _activeOptionId = "nat_human";
          }
          else if (lower.contains("grammar") || lower.contains("spelling"))
          {
            _activeCategory = AiActionEngine.Category.GRAMMAR;
            _activeOptionId = "grammar_all";
          }
          else if (lower.contains("bullet") || lower.contains("summary"))
          {
            _activeCategory = AiActionEngine.Category.SUMMARIZE;
            _activeOptionId = "sum_bullets";
          }
          else if (lower.contains("code"))
          {
            _activeCategory = AiActionEngine.Category.EXPLAIN;
            _activeOptionId = "exp_code";
          }
          else if (lower.contains("title"))
          {
            _activeCategory = AiActionEngine.Category.CONTENT;
            _activeOptionId = lower.contains("youtube") ? "content_yt_title" : "content_title";
          }
          else if (lower.contains("description"))
          {
            _activeCategory = AiActionEngine.Category.CONTENT;
            _activeOptionId = lower.contains("youtube") ? "content_yt_desc" : "content_desc";
          }
          else
          {
            _activeCategory = analysis.recommendedCategory;
            _activeOptionId = analysis.recommendedOptionId;
          }
          _scrollSubOptions.setVisibility(GONE);
          _scrollTone.setVisibility(GONE);
          _llCustomPrompt.setVisibility(GONE);
          buildCategoryChips();
          triggerExecution();
        }
      });

      _rowQuickSuggestions.addView(chip);
    }
  }

  /**
   * Builds [LINE 1] Category Chips.
   * Clicking a category highlights it and reveals [LINE 2] Sub-Options.
   * It DOES NOT automatically generate!
   */
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

      if (isSelected)
      {
        int activeBg = (_colorKeyActivated != 0) ? _colorKeyActivated : Color.parseColor("#2196F3");
        chip.setBackground(createPillBackground(activeBg, dp(12), Color.TRANSPARENT));
        chip.setTextColor(Color.WHITE);
      }
      else
      {
        chip.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.5f), dp(12), adjustAlpha(_colorLabel, 0.15f)));
        chip.setTextColor(adjustAlpha(_colorLabel, 0.8f));
      }

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
          _activeOptionId = "";
          _activeTone = "default";
          buildCategoryChips();

          // Progressive disclosure: Show Line 2 according to category
          if (_activeCategory == AiActionEngine.Category.ASK_AI)
          {
            _scrollSubOptions.setVisibility(GONE);
            _scrollTone.setVisibility(GONE);
            _llCustomPrompt.setVisibility(VISIBLE);
            updatePromptDisplay();
            _tvStatus.setText("নিচের বক্সে ক্লিক করে প্রম্পট লিখুন (কিবোর্ড ওপেন হবে) 🚀");
            if (_currentCustomPrompt.isEmpty())
            {
              openKeyboardForPrompt();
            }
          }
          else
          {
            _llCustomPrompt.setVisibility(GONE);
            _scrollTone.setVisibility(GONE);
            buildSubOptionChips();
            _scrollSubOptions.setVisibility(VISIBLE);
            _tvStatus.setText("Choose an option above to generate");
          }
          // Note: NO automatic generation on selecting Category!
        }
      });

      _rowCategories.addView(chip);
    }
  }

  /**
   * Builds [LINE 2] Sub-Option Chips.
   * If an option is self-contained (like Translate, Grammar, Summarize, Reply, etc.),
   * tapping it directly triggers generation!
   * If user taps "🎨 Select Tone...", [LINE 3] Tone Strip is revealed.
   */
  private void buildSubOptionChips()
  {
    _rowSubOptions.removeAllViews();
    if (_activeCategory == null || _activeCategory == AiActionEngine.Category.ASK_AI)
    {
      _scrollSubOptions.setVisibility(GONE);
      return;
    }

    List<AiActionEngine.ActionOption> options = AiActionEngine.getOptionsForCategory(_activeCategory);
    for (final AiActionEngine.ActionOption opt : options)
    {
      final boolean isSelected = opt.id.equals(_activeOptionId);
      Button chip = new Button(getContext());
      chip.setText(opt.label);
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
      chip.setPadding(dp(8), 0, dp(8), 0);

      if (isSelected)
      {
        int accent = (_colorKeyActivated != 0) ? _colorKeyActivated : Color.parseColor("#2196F3");
        chip.setBackground(createPillBackground(adjustAlpha(accent, 0.35f), dp(10), accent));
        chip.setTextColor(_colorLabel);
        chip.setTypeface(null, Typeface.BOLD);
      }
      else
      {
        chip.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.35f), dp(10), adjustAlpha(_colorLabel, 0.12f)));
        chip.setTextColor(adjustAlpha(_colorLabel, 0.75f));
      }

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

          // If user specifically requested to pick a tone under Rewrite:
          if ("select_tone".equals(opt.id))
          {
            buildToneChips();
            _scrollTone.setVisibility(VISIBLE);
            _tvStatus.setText("Choose a tone below to generate");
            return; // Wait for user to pick tone from Line 3
          }

          // Otherwise, Line 2 choice is final: Generate immediately!
          _scrollTone.setVisibility(GONE);
          triggerExecution();
        }
      });

      _rowSubOptions.addView(chip);
    }
  }

  /**
   * Builds [LINE 3] Tone Selector Chips.
   * Only shown when explicitly requested (e.g. "Select Tone...").
   * Tapping a tone sets the tone and immediately triggers generation!
   */
  private void buildToneChips()
  {
    _rowToneChips.removeAllViews();
    TextView lbl = new TextView(getContext());
    lbl.setText("🎭 Tone: ");
    lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
    lbl.setTextColor(adjustAlpha(_colorLabel, 0.65f));
    lbl.setGravity(Gravity.CENTER_VERTICAL);
    _rowToneChips.addView(lbl);

    String[] tones = {"Islamic", "Professional", "Friendly", "Casual", "Formal", "Confident", "Funny", "Empathetic", "Persuasive"};
    for (final String tone : tones)
    {
      final boolean isSelected = tone.equalsIgnoreCase(_activeTone);
      Button chip = new Button(getContext());
      chip.setText(tone);
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
      chip.setPadding(dp(6), 0, dp(6), 0);

      if (isSelected)
      {
        chip.setBackground(createPillBackground(adjustAlpha(Color.parseColor("#10B981"), 0.35f), dp(8), Color.parseColor("#10B981")));
        chip.setTextColor(Color.parseColor("#6EE7B7"));
        chip.setTypeface(null, Typeface.BOLD);
      }
      else
      {
        chip.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.25f), dp(8), adjustAlpha(_colorLabel, 0.1f)));
        chip.setTextColor(adjustAlpha(_colorLabel, 0.65f));
      }

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
          // Now that tone is chosen, trigger generation!
          triggerExecution();
        }
      });

      _rowToneChips.addView(chip);
    }
  }

  public void openKeyboardForPrompt()
  {
    if (_keyboard != null)
    {
      _keyboard.showAiPromptTypingMode(_currentCustomPrompt);
    }
  }

  public void onPromptTypedFromKeyboard(String promptText)
  {
    _currentCustomPrompt = (promptText != null) ? promptText.trim() : "";
    updatePromptDisplay();
    if (!_currentCustomPrompt.isEmpty())
    {
      _activeCategory = AiActionEngine.Category.ASK_AI;
      _activeOptionId = "custom";
      buildCategoryChips();
      triggerExecution();
    }
  }

  private void updatePromptDisplay()
  {
    if (_tvCustomPromptDisplay == null) return;
    if (_currentCustomPrompt.isEmpty())
    {
      _tvCustomPromptDisplay.setText("✏️ ক্লিক করে প্রম্পট লিখুন (কিবোর্ড ওপেন হবে)...");
      _tvCustomPromptDisplay.setTextColor(adjustAlpha(_colorLabel, 0.45f));
    }
    else
    {
      _tvCustomPromptDisplay.setText("🎯 " + _currentCustomPrompt);
      _tvCustomPromptDisplay.setTextColor(_colorLabel);
    }
  }

  private void triggerExecution()
  {
    if (_isGenerating) return;

    String custom = _currentCustomPrompt;
    boolean hasWorkingText = (_currentWorkingText != null && !_currentWorkingText.trim().isEmpty());
    boolean hasCustomPrompt = !custom.isEmpty();

    if (!hasWorkingText && !hasCustomPrompt)
    {
      openKeyboardForPrompt();
      return;
    }

    if (!AiProvider.Manager.hasConfiguredApiKey(getContext()))
    {
      openSettingsDialog();
      return;
    }

    if (_activeCategory == null)
    {
      _activeCategory = hasCustomPrompt ? AiActionEngine.Category.ASK_AI : AiActionEngine.Category.REWRITE;
    }

    _isGenerating = true;
    _progressBar.setVisibility(VISIBLE);
    _tvStatus.setText("Generating with " + AiProvider.Manager.getActiveProvider(getContext()).getName() + "...");

    String textToProcess = hasWorkingText ? _currentWorkingText : custom;
    boolean emojify = GeminiAiService.isEmojifyEnabled(getContext());

    AiActionEngine.executeAction(
        getContext(),
        textToProcess,
        _activeCategory,
        _activeOptionId,
        _activeTone,
        custom,
        emojify,
        new AiProvider.Callback()
        {
          @Override
          public void onSuccess(final String resultText)
          {
            _isGenerating = false;
            _progressBar.setVisibility(GONE);
            _lastGeneratedResult = resultText;
            _tvStatus.setText("✅ Generated successfully");
            _tvResultPreview.setText(resultText);
            _tvResultPreview.scrollTo(0, 0);

            _tvResultPreview.post(new Runnable()
            {
              @Override
              public void run()
              {
                if (_tvResultPreview != null)
                {
                  _tvResultPreview.scrollTo(0, 0);
                }
              }
            });

            if (_scrollBody != null)
            {
              _scrollBody.post(new Runnable()
              {
                @Override
                public void run()
                {
                  if (_tvResultPreview != null && _scrollBody != null)
                  {
                    _scrollBody.smoothScrollTo(0, _tvResultPreview.getTop() - dp(8));
                  }
                }
              });
            }
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
    String output = _tvResultPreview.getText().toString();
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

  private Button createPremiumButton(String label, int bgColor, int strokeColor, OnClickListener listener)
  {
    Button b = new Button(getContext());
    b.setText(label);
    b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    b.setTypeface(null, Typeface.BOLD);
    b.setTextColor(Color.WHITE);
    b.setSingleLine(true);
    b.setEllipsize(TextUtils.TruncateAt.END);
    b.setPadding(dp(4), 0, dp(4), 0);
    b.setIncludeFontPadding(false);

    GradientDrawable normalBg = createPillBackground(bgColor, dp(19), strokeColor);
    GradientDrawable pressedBg = createPillBackground(adjustAlpha(bgColor, 0.75f), dp(19), strokeColor);

    StateListDrawable sld = new StateListDrawable();
    sld.addState(new int[]{android.R.attr.state_pressed}, pressedBg);
    sld.addState(new int[]{}, normalBg);
    b.setBackground(sld);

    b.setOnClickListener(listener);
    return b;
  }

  private Button createIconActionButton(String iconText, int bgColor, int strokeColor, OnClickListener listener)
  {
    Button b = new Button(getContext());
    b.setText(iconText);
    b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
    b.setTypeface(null, Typeface.BOLD);
    b.setTextColor(Color.WHITE);
    b.setPadding(0, 0, 0, 0);

    GradientDrawable normalBg = createPillBackground(bgColor, dp(19), strokeColor);
    GradientDrawable pressedBg = createPillBackground(adjustAlpha(bgColor, 0.75f), dp(19), strokeColor);

    StateListDrawable sld = new StateListDrawable();
    sld.addState(new int[]{android.R.attr.state_pressed}, pressedBg);
    sld.addState(new int[]{}, normalBg);
    b.setBackground(sld);

    b.setOnClickListener(listener);
    return b;
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
