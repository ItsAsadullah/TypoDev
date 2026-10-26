package typodev.keyboard.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;
import typodev.keyboard.Keyboard2;
import typodev.keyboard.Logs;
import typodev.keyboard.R;

/**
 * Fast, icon-driven in-keyboard AI Assistant view.
 *
 * Designed for 1-tap speed and clarity:
 * - Zero emojis in UI controls (uses crisp vector drawables throughout).
 * - Top 1-Tap Quick Actions strip for instant execution (Fix Grammar, BN↔EN, Banglish→বাংলা, Professional, Islamic, Reply, Summarize, Math).
 * - Clean 3-column visual Category Grid when browsing tools, switching smoothly to a dedicated
 *   Result Preview + Bottom Action Dock when generating or viewing output.
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

  // AI execution state
  private AiActionEngine.Category _activeCategory = null;
  private String _activeOptionId = "";
  private String _activeTone = "default";
  private String _lastGeneratedResult = "";
  private boolean _isGenerating = false;
  private boolean _showingResultMode = false;
  private long _inputSessionId;
  private long _requestRevision;
  private String _currentCustomPrompt = "";

  // Header & Context Views
  private TextView _tvProviderBadge;
  private LinearLayout _btnEmojifyToggle;
  private ImageView _ivEmojifyIcon;
  private TextView _tvEmojifyLabel;
  private ImageView _ivSourceIcon;
  private TextView _tvSourceBadge;
  private TextView _tvInputSnippet;
  private TextView _btnToggleSource;
  private LinearLayout _cardSetupNotice;

  // Quick Actions & Custom Prompt Views
  private HorizontalScrollView _scrollQuick;
  private LinearLayout _rowQuickActions;
  private LinearLayout _llCustomPrompt;
  private TextView _tvCustomPromptDisplay;

  // Sub-options & Tone Strip
  private LinearLayout _sectionSubOptions;
  private TextView _tvSelectedCategoryTitle;
  private HorizontalScrollView _scrollSubOptions;
  private LinearLayout _rowSubOptions;
  private HorizontalScrollView _scrollTone;
  private LinearLayout _rowToneChips;

  // Tools Grid vs Result Preview containers
  private ScrollView _scrollBody;
  private GridLayout _gridCategories;
  private LinearLayout _resultContainer;
  private ProgressBar _progressBar;
  private ImageView _ivStatusIcon;
  private TextView _tvStatus;
  private TextView _btnShowToolsGrid;
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
    int minSafeBottom = dp(28);
    _bottomSafety = Math.max(rawBottomSafety, minSafeBottom);
    if (_rowActionButtons != null)
    {
      _rowActionButtons.setPadding(dp(8), dp(6), dp(8), dp(6) + _bottomSafety);
    }
    if (_gridCategories != null)
    {
      _gridCategories.setPadding(dp(2), dp(2), dp(2), dp(6) + _bottomSafety);
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
    // 1. Sleek Top Header Bar (Vector Icons Only)
    // ==========================================
    LinearLayout headerBar = new LinearLayout(context);
    headerBar.setOrientation(HORIZONTAL);
    headerBar.setGravity(Gravity.CENTER_VERTICAL);
    headerBar.setPadding(dp(8), dp(4), dp(8), dp(4));

    ImageView btnBack = createCircleIconButton(context, R.drawable.ic_ai_back, dp(34), dp(8), new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_showingResultMode && !_isGenerating)
        {
          showToolsGridMode();
          return;
        }
        if (_keyboard != null) _keyboard.closeAiPane();
      }
    });
    headerBar.addView(btnBack);

    ImageView ivBrand = new ImageView(context);
    ivBrand.setImageResource(R.drawable.ic_toolbar_ai);
    int accent = (_colorKeyActivated != 0) ? _colorKeyActivated : Color.parseColor("#2196F3");
    ivBrand.setColorFilter(accent, PorterDuff.Mode.SRC_IN);
    LinearLayout.LayoutParams lpBrand = new LinearLayout.LayoutParams(dp(18), dp(18));
    lpBrand.setMargins(dp(6), 0, dp(4), 0);
    headerBar.addView(ivBrand, lpBrand);

    TextView tvTitle = new TextView(context);
    tvTitle.setText("AI Assistant");
    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    tvTitle.setTypeface(null, Typeface.BOLD);
    tvTitle.setTextColor(_colorLabel);
    headerBar.addView(tvTitle);

    _tvProviderBadge = new TextView(context);
    _tvProviderBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
    _tvProviderBadge.setTextColor(_colorLabel);
    _tvProviderBadge.setTypeface(null, Typeface.BOLD);
    _tvProviderBadge.setPadding(dp(7), dp(2), dp(7), dp(2));
    _tvProviderBadge.setBackground(createPillBackground(
        adjustAlpha(accent, 0.22f),
        dp(10),
        adjustAlpha(accent, 0.55f)));
    _tvProviderBadge.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        openSettingsDialog();
      }
    });
    LinearLayout.LayoutParams lpBadge = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpBadge.setMargins(dp(6), 0, dp(4), 0);
    headerBar.addView(_tvProviderBadge, lpBadge);

    View spacerHeader = new View(context);
    headerBar.addView(spacerHeader, new LinearLayout.LayoutParams(0, 1, 1.0f));

    // 1-Tap "Ask AI" Prompt Button in Header
    LinearLayout btnAskHeader = createIconPillChip(
        context,
        R.drawable.ic_ai_send,
        "Ask AI",
        adjustAlpha(accent, 0.22f),
        adjustAlpha(accent, 0.55f),
        _colorLabel,
        accent,
        dp(28),
        new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            _activeCategory = AiActionEngine.Category.ASK_AI;
            _llCustomPrompt.setVisibility(VISIBLE);
            openKeyboardForPrompt();
          }
        });
    LinearLayout.LayoutParams lpAsk = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(28));
    lpAsk.setMargins(dp(2), 0, dp(4), 0);
    headerBar.addView(btnAskHeader, lpAsk);

    // Emojify Toggle Button (Vector Icon + Label)
    _btnEmojifyToggle = new LinearLayout(context);
    _btnEmojifyToggle.setOrientation(HORIZONTAL);
    _btnEmojifyToggle.setGravity(Gravity.CENTER_VERTICAL);
    _btnEmojifyToggle.setPadding(dp(7), 0, dp(7), 0);
    _ivEmojifyIcon = new ImageView(context);
    _ivEmojifyIcon.setImageResource(R.drawable.ic_ai_humor);
    _ivEmojifyIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(13), dp(13)));
    _tvEmojifyLabel = new TextView(context);
    _tvEmojifyLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    _tvEmojifyLabel.setPadding(dp(4), 0, 0, 0);
    _btnEmojifyToggle.addView(_ivEmojifyIcon);
    _btnEmojifyToggle.addView(_tvEmojifyLabel);
    updateEmojifyButtonState();
    _btnEmojifyToggle.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        boolean cur = GeminiAiService.isEmojifyEnabled(getContext());
        GeminiAiService.setEmojifyEnabled(getContext(), !cur);
        updateEmojifyButtonState();
        Toast.makeText(getContext(), !cur ? "Emoji in output: ON" : "Plain text output: ON", Toast.LENGTH_SHORT).show();
      }
    });
    LinearLayout.LayoutParams lpEmo = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(28));
    lpEmo.setMargins(dp(2), 0, dp(4), 0);
    headerBar.addView(_btnEmojifyToggle, lpEmo);

    // Settings Icon Button
    ImageView btnSettings = createCircleIconButton(context, R.drawable.ic_ai_settings, dp(34), dp(8), new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        openSettingsDialog();
      }
    });
    headerBar.addView(btnSettings);

    addView(headerBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

    // ==========================================
    // 2. Main Scrollable Body
    // ==========================================
    _scrollBody = new ScrollView(context);
    _scrollBody.setFillViewport(true);
    _scrollBody.setVerticalScrollBarEnabled(false);
    _scrollBody.setHorizontalScrollBarEnabled(false);
    _scrollBody.setOverScrollMode(View.OVER_SCROLL_NEVER);

    LinearLayout bodyContent = new LinearLayout(context);
    bodyContent.setOrientation(VERTICAL);
    bodyContent.setPadding(dp(8), dp(2), dp(8), dp(4));

    // 2.1 API Key Setup Notice (shown only if not configured)
    _cardSetupNotice = new LinearLayout(context);
    _cardSetupNotice.setOrientation(HORIZONTAL);
    _cardSetupNotice.setGravity(Gravity.CENTER_VERTICAL);
    _cardSetupNotice.setPadding(dp(10), dp(6), dp(10), dp(6));
    _cardSetupNotice.setBackground(createPillBackground(adjustAlpha(Color.parseColor("#FF9800"), 0.18f), dp(8), Color.parseColor("#FF9800")));

    ImageView ivWarn = new ImageView(context);
    ivWarn.setImageResource(R.drawable.ic_toolbar_key);
    ivWarn.setColorFilter(Color.parseColor("#FF9800"), PorterDuff.Mode.SRC_IN);
    LinearLayout.LayoutParams lpWarn = new LinearLayout.LayoutParams(dp(16), dp(16));
    lpWarn.setMargins(0, 0, dp(6), 0);
    _cardSetupNotice.addView(ivWarn, lpWarn);

    TextView tvNotice = new TextView(context);
    tvNotice.setText("API key required for Gemini, Groq, Grok or OpenAI");
    tvNotice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    tvNotice.setTextColor(_colorLabel);
    _cardSetupNotice.addView(tvNotice, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

    TextView btnSetup = new TextView(context);
    btnSetup.setText("Setup Key");
    btnSetup.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    btnSetup.setTypeface(null, Typeface.BOLD);
    btnSetup.setTextColor(Color.WHITE);
    btnSetup.setGravity(Gravity.CENTER);
    btnSetup.setPadding(dp(10), 0, dp(10), 0);
    btnSetup.setBackground(createPillBackground(Color.parseColor("#E65100"), dp(8), Color.TRANSPARENT));
    btnSetup.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        openSettingsDialog();
      }
    });
    _cardSetupNotice.addView(btnSetup, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(26)));
    _cardSetupNotice.setVisibility(GONE);
    bodyContent.addView(_cardSetupNotice);

    // 2.2 Sleek Single-Line Context Bar (Source Icon + Snippet + Switch Button)
    LinearLayout cardContext = new LinearLayout(context);
    cardContext.setOrientation(HORIZONTAL);
    cardContext.setGravity(Gravity.CENTER_VERTICAL);
    cardContext.setPadding(dp(8), dp(4), dp(6), dp(4));
    cardContext.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.45f), dp(8), adjustAlpha(_colorLabel, 0.12f)));
    LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
    lpCard.setMargins(0, dp(2), 0, dp(4));
    cardContext.setLayoutParams(lpCard);

    _ivSourceIcon = new ImageView(context);
    _ivSourceIcon.setImageResource(R.drawable.ic_ai_edit);
    _ivSourceIcon.setColorFilter(accent, PorterDuff.Mode.SRC_IN);
    LinearLayout.LayoutParams lpSrcIcon = new LinearLayout.LayoutParams(dp(14), dp(14));
    lpSrcIcon.setMargins(0, 0, dp(5), 0);
    cardContext.addView(_ivSourceIcon, lpSrcIcon);

    _tvSourceBadge = new TextView(context);
    _tvSourceBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    _tvSourceBadge.setTypeface(null, Typeface.BOLD);
    _tvSourceBadge.setTextColor(_colorLabel);
    _tvSourceBadge.setPadding(0, 0, dp(6), 0);
    cardContext.addView(_tvSourceBadge);

    _tvInputSnippet = new TextView(context);
    _tvInputSnippet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _tvInputSnippet.setSingleLine(true);
    _tvInputSnippet.setEllipsize(TextUtils.TruncateAt.END);
    _tvInputSnippet.setTextColor(adjustAlpha(_colorLabel, 0.80f));
    cardContext.addView(_tvInputSnippet, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

    _btnToggleSource = new TextView(context);
    _btnToggleSource.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    _btnToggleSource.setTypeface(null, Typeface.BOLD);
    _btnToggleSource.setTextColor(_colorLabel);
    _btnToggleSource.setGravity(Gravity.CENTER);
    _btnToggleSource.setPadding(dp(8), dp(2), dp(8), dp(2));
    _btnToggleSource.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.8f), dp(8), adjustAlpha(_colorLabel, 0.18f)));
    _btnToggleSource.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        _usingClipboard = !_usingClipboard;
        _currentWorkingText = _usingClipboard ? _clipboardText : _inputFieldText;
        updateContextDisplay();
        rebuildQuickActions();
      }
    });
    LinearLayout.LayoutParams lpToggle = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(24));
    lpToggle.setMargins(dp(6), 0, 0, 0);
    cardContext.addView(_btnToggleSource, lpToggle);
    bodyContent.addView(cardContext);

    // 2.3 Custom Prompt Bar (Visible when Ask AI is chosen or prompt is active)
    _llCustomPrompt = new LinearLayout(context);
    _llCustomPrompt.setOrientation(HORIZONTAL);
    _llCustomPrompt.setGravity(Gravity.CENTER_VERTICAL);
    _llCustomPrompt.setPadding(0, dp(2), 0, dp(4));

    _tvCustomPromptDisplay = new TextView(context);
    _tvCustomPromptDisplay.setText("Tap to type custom instruction for AI...");
    _tvCustomPromptDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _tvCustomPromptDisplay.setTextColor(adjustAlpha(_colorLabel, 0.55f));
    _tvCustomPromptDisplay.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.55f), dp(8), adjustAlpha(_colorLabel, 0.22f)));
    _tvCustomPromptDisplay.setPadding(dp(10), dp(6), dp(10), dp(6));
    _tvCustomPromptDisplay.setSingleLine(true);
    _tvCustomPromptDisplay.setEllipsize(TextUtils.TruncateAt.END);
    _tvCustomPromptDisplay.setGravity(Gravity.CENTER_VERTICAL);
    _tvCustomPromptDisplay.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        openKeyboardForPrompt();
      }
    });
    _llCustomPrompt.addView(_tvCustomPromptDisplay, new LinearLayout.LayoutParams(0, dp(34), 1.0f));

    LinearLayout btnSendPrompt = createIconPillChip(
        context,
        R.drawable.ic_ai_send,
        "Run",
        Color.parseColor("#1976D2"),
        Color.TRANSPARENT,
        Color.WHITE,
        Color.WHITE,
        dp(34),
        new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            if (_currentCustomPrompt.isEmpty()) openKeyboardForPrompt();
            else triggerExecution();
          }
        });
    LinearLayout.LayoutParams lpSend = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
    lpSend.setMargins(dp(6), 0, 0, 0);
    _llCustomPrompt.addView(btnSendPrompt, lpSend);
    _llCustomPrompt.setVisibility(GONE);
    bodyContent.addView(_llCustomPrompt);

    // 2.4 1-Tap Instant Quick Actions Strip (Always Accessible)
    _scrollQuick = new HorizontalScrollView(context);
    _scrollQuick.setHorizontalScrollBarEnabled(false);
    _scrollQuick.setVerticalScrollBarEnabled(false);
    _scrollQuick.setOverScrollMode(View.OVER_SCROLL_NEVER);
    _rowQuickActions = new LinearLayout(context);
    _rowQuickActions.setOrientation(HORIZONTAL);
    _rowQuickActions.setGravity(Gravity.CENTER_VERTICAL);
    _scrollQuick.addView(_rowQuickActions);
    LinearLayout.LayoutParams lpQuick = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpQuick.setMargins(0, 0, 0, dp(4));
    _scrollQuick.setLayoutParams(lpQuick);
    bodyContent.addView(_scrollQuick);

    // 2.5 Active Category Sub-Options Strip (Shown when a category is selected)
    _sectionSubOptions = new LinearLayout(context);
    _sectionSubOptions.setOrientation(VERTICAL);
    _sectionSubOptions.setPadding(dp(6), dp(5), dp(6), dp(5));
    _sectionSubOptions.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.38f), dp(10), adjustAlpha(accent, 0.35f)));
    LinearLayout.LayoutParams lpSubSec = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpSubSec.setMargins(0, 0, 0, dp(5));
    _sectionSubOptions.setLayoutParams(lpSubSec);

    LinearLayout subHeaderRow = new LinearLayout(context);
    subHeaderRow.setOrientation(HORIZONTAL);
    subHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
    subHeaderRow.setPadding(dp(2), 0, dp(2), dp(4));

    _tvSelectedCategoryTitle = new TextView(context);
    _tvSelectedCategoryTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _tvSelectedCategoryTitle.setTypeface(null, Typeface.BOLD);
    _tvSelectedCategoryTitle.setTextColor(_colorLabel);
    subHeaderRow.addView(_tvSelectedCategoryTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

    TextView btnCloseSub = new TextView(context);
    btnCloseSub.setText("All Categories");
    btnCloseSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    btnCloseSub.setTextColor(accent);
    btnCloseSub.setTypeface(null, Typeface.BOLD);
    btnCloseSub.setPadding(dp(6), dp(2), dp(6), dp(2));
    btnCloseSub.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        _activeCategory = null;
        _activeOptionId = "";
        _sectionSubOptions.setVisibility(GONE);
        showToolsGridMode();
        buildCategoryGrid();
      }
    });
    subHeaderRow.addView(btnCloseSub);
    _sectionSubOptions.addView(subHeaderRow);

    _scrollSubOptions = new HorizontalScrollView(context);
    _scrollSubOptions.setHorizontalScrollBarEnabled(false);
    _scrollSubOptions.setOverScrollMode(View.OVER_SCROLL_NEVER);
    _rowSubOptions = new LinearLayout(context);
    _rowSubOptions.setOrientation(HORIZONTAL);
    _scrollSubOptions.addView(_rowSubOptions);
    _sectionSubOptions.addView(_scrollSubOptions);

    // Optional Tone Row inside Sub-options
    _scrollTone = new HorizontalScrollView(context);
    _scrollTone.setHorizontalScrollBarEnabled(false);
    _scrollTone.setOverScrollMode(View.OVER_SCROLL_NEVER);
    _rowToneChips = new LinearLayout(context);
    _rowToneChips.setOrientation(HORIZONTAL);
    _rowToneChips.setGravity(Gravity.CENTER_VERTICAL);
    _scrollTone.addView(_rowToneChips);
    LinearLayout.LayoutParams lpTone = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpTone.setMargins(0, dp(4), 0, 0);
    _scrollTone.setLayoutParams(lpTone);
    _scrollTone.setVisibility(GONE);
    _sectionSubOptions.addView(_scrollTone);

    _sectionSubOptions.setVisibility(GONE);
    bodyContent.addView(_sectionSubOptions);

    // 2.6 [MODE A] 3-Column Visual Category Grid (Fast & Scannable)
    _gridCategories = new GridLayout(context);
    _gridCategories.setColumnCount(3);
    _gridCategories.setUseDefaultMargins(false);
    bodyContent.addView(_gridCategories, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    // 2.7 [MODE B] Result Preview Container (Shown when generating or viewing output)
    _resultContainer = new LinearLayout(context);
    _resultContainer.setOrientation(VERTICAL);
    _resultContainer.setVisibility(GONE);

    LinearLayout rowStatus = new LinearLayout(context);
    rowStatus.setOrientation(HORIZONTAL);
    rowStatus.setGravity(Gravity.CENTER_VERTICAL);
    rowStatus.setPadding(dp(2), dp(2), dp(2), dp(3));

    _progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
    _progressBar.setVisibility(GONE);
    rowStatus.addView(_progressBar);

    _ivStatusIcon = new ImageView(context);
    _ivStatusIcon.setImageResource(R.drawable.ic_ai_check);
    _ivStatusIcon.setColorFilter(Color.parseColor("#10B981"), PorterDuff.Mode.SRC_IN);
    _ivStatusIcon.setVisibility(GONE);
    LinearLayout.LayoutParams lpStIcon = new LinearLayout.LayoutParams(dp(14), dp(14));
    lpStIcon.setMargins(0, 0, dp(4), 0);
    rowStatus.addView(_ivStatusIcon, lpStIcon);

    _tvStatus = new TextView(context);
    _tvStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _tvStatus.setTextColor(adjustAlpha(_colorLabel, 0.85f));
    _tvStatus.setPadding(dp(4), 0, 0, 0);
    rowStatus.addView(_tvStatus, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

    _btnShowToolsGrid = new TextView(context);
    _btnShowToolsGrid.setText("Browse All Tools");
    _btnShowToolsGrid.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    _btnShowToolsGrid.setTypeface(null, Typeface.BOLD);
    _btnShowToolsGrid.setTextColor(accent);
    _btnShowToolsGrid.setPadding(dp(8), dp(3), dp(8), dp(3));
    _btnShowToolsGrid.setBackground(createPillBackground(adjustAlpha(accent, 0.16f), dp(8), adjustAlpha(accent, 0.4f)));
    _btnShowToolsGrid.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        showToolsGridMode();
      }
    });
    rowStatus.addView(_btnShowToolsGrid);
    _resultContainer.addView(rowStatus);

    _tvResultPreview = new TextView(context);
    _tvResultPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    _tvResultPreview.setTextColor(_colorLabel);
    _tvResultPreview.setHintTextColor(adjustAlpha(_colorLabel, 0.45f));
    _tvResultPreview.setHint("Generated text will appear here...");
    _tvResultPreview.setMinLines(4);
    _tvResultPreview.setMaxLines(14);
    _tvResultPreview.setGravity(Gravity.TOP);
    _tvResultPreview.setLineSpacing(dp(2), 1.0f);
    _tvResultPreview.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.45f), dp(10), adjustAlpha(_colorLabel, 0.22f)));
    _tvResultPreview.setPadding(dp(12), dp(10), dp(12), dp(10));
    _tvResultPreview.setFocusable(false);
    _tvResultPreview.setFocusableInTouchMode(false);
    LinearLayout.LayoutParams lpResult = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpResult.setMargins(0, dp(2), 0, dp(4));
    _tvResultPreview.setLayoutParams(lpResult);
    _resultContainer.addView(_tvResultPreview);

    bodyContent.addView(_resultContainer);

    _scrollBody.addView(bodyContent);
    addView(_scrollBody, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

    // ==========================================
    // 3. Fixed Bottom Action Dock (Shown only in Result Mode)
    // ==========================================
    _bottomSafety = dp(28);
    _rowActionButtons = new LinearLayout(context);
    _rowActionButtons.setOrientation(HORIZONTAL);
    _rowActionButtons.setGravity(Gravity.CENTER_VERTICAL);
    _rowActionButtons.setBackgroundColor(adjustAlpha(_colorKeyboard, 0.98f));
    _rowActionButtons.setPadding(dp(8), dp(6), dp(8), dp(6) + _bottomSafety);
    _rowActionButtons.setVisibility(GONE);

    // 3.1 Replace Button
    LinearLayout btnReplace = createActionDockButton(
        context,
        R.drawable.ic_ai_check,
        "Replace",
        Color.parseColor("#059669"),
        Color.parseColor("#34D399"),
        new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            applyResultToField(true);
          }
        });
    LinearLayout.LayoutParams lpReplace = new LinearLayout.LayoutParams(0, dp(38), 1.25f);
    lpReplace.setMargins(dp(3), 0, dp(3), 0);
    _rowActionButtons.addView(btnReplace, lpReplace);

    // 3.2 Insert Button
    LinearLayout btnInsert = createActionDockButton(
        context,
        R.drawable.ic_clip_add,
        "Insert",
        Color.parseColor("#1D4ED8"),
        Color.parseColor("#60A5FA"),
        new OnClickListener()
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

    // 3.3 Copy Button
    LinearLayout btnCopy = createActionDockButton(
        context,
        R.drawable.ic_clip_copy,
        "Copy",
        Color.parseColor("#374151"),
        Color.parseColor("#6B7280"),
        new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            String res = (_lastGeneratedResult != null && !_lastGeneratedResult.trim().isEmpty())
                ? _lastGeneratedResult
                : _tvResultPreview.getText().toString();
            if (res == null || res.trim().isEmpty())
            {
              Toast.makeText(context, "No generated text to copy", Toast.LENGTH_SHORT).show();
              return;
            }
            try
            {
              ClipboardManager cm = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
              if (cm != null)
              {
                cm.setPrimaryClip(ClipData.newPlainText("AI Generated", res));
              }
              typodev.keyboard.ClipboardHistoryService.setRecentCopiedClip(res);
              typodev.keyboard.ClipboardHistoryService hs = typodev.keyboard.ClipboardHistoryService.get_service(context);
              if (hs != null) hs.add_clip(res);
              Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
            catch (Throwable t)
            {
              Toast.makeText(context, "Failed to copy", Toast.LENGTH_SHORT).show();
            }
          }
        });
    LinearLayout.LayoutParams lpCopy = new LinearLayout.LayoutParams(0, dp(38), 1.0f);
    lpCopy.setMargins(dp(3), 0, dp(3), 0);
    _rowActionButtons.addView(btnCopy, lpCopy);

    // 3.4 Regenerate Icon Button
    ImageView btnRegen = createCircleIconButton(
        context,
        R.drawable.ic_ai_refresh,
        dp(38),
        dp(9),
        new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            triggerExecution();
          }
        });
    btnRegen.setBackground(createPillBackground(Color.parseColor("#374151"), dp(19), Color.parseColor("#6B7280")));
    btnRegen.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
    LinearLayout.LayoutParams lpRegen = new LinearLayout.LayoutParams(dp(38), dp(38));
    lpRegen.setMargins(dp(3), 0, dp(3), 0);
    _rowActionButtons.addView(btnRegen, lpRegen);

    addView(_rowActionButtons, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
  }

  private void showToolsGridMode()
  {
    _showingResultMode = false;
    if (_gridCategories != null) _gridCategories.setVisibility(VISIBLE);
    if (_resultContainer != null) _resultContainer.setVisibility(GONE);
    if (_rowActionButtons != null) _rowActionButtons.setVisibility(GONE);
  }

  private void showResultMode()
  {
    _showingResultMode = true;
    if (_gridCategories != null) _gridCategories.setVisibility(GONE);
    if (_resultContainer != null) _resultContainer.setVisibility(VISIBLE);
    if (_rowActionButtons != null) _rowActionButtons.setVisibility(VISIBLE);
  }

  public void open(int targetHeight, int bottomSafety)
  {
    ++_requestRevision;
    _isGenerating = false;
    _lastGeneratedResult = "";
    if (_tvResultPreview != null) _tvResultPreview.setText("");
    if (_progressBar != null) _progressBar.setVisibility(GONE);
    if (_ivStatusIcon != null) _ivStatusIcon.setVisibility(GONE);
    _inputSessionId = _keyboard != null ? _keyboard.getInputSessionId() : -1;
    try
    {
      Context context = getContext();
      initThemeColors(context);
      setBackgroundColor(_colorKeyboard);

      _activeCategory = null;
      _activeOptionId = "";
      _activeTone = "default";
      _currentCustomPrompt = "";
      updatePromptDisplay();
      if (_sectionSubOptions != null) _sectionSubOptions.setVisibility(GONE);
      if (_scrollTone != null) _scrollTone.setVisibility(GONE);
      if (_llCustomPrompt != null) _llCustomPrompt.setVisibility(GONE);
      showToolsGridMode();

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

      if (_tvProviderBadge != null)
      {
        AiProvider provider = AiProvider.Manager.getActiveProvider(context);
        if (provider != null)
        {
          _tvProviderBadge.setText(provider.getName());
        }
      }
      updateEmojifyButtonState();

      boolean hasKey = AiProvider.Manager.hasConfiguredApiKey(context);
      if (_cardSetupNotice != null)
      {
        _cardSetupNotice.setVisibility(hasKey ? GONE : VISIBLE);
      }

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
      rebuildQuickActions();
      buildCategoryGrid();
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
        if (provider != null) _tvProviderBadge.setText(provider.getName());
        _cardSetupNotice.setVisibility(AiProvider.Manager.hasConfiguredApiKey(ctx) ? GONE : VISIBLE);
      }
    });
  }

  private void updateContextDisplay()
  {
    int accent = (_colorKeyActivated != 0) ? _colorKeyActivated : Color.parseColor("#2196F3");
    if (_usingClipboard)
    {
      _ivSourceIcon.setImageResource(R.drawable.ic_toolbar_clipboard);
      _tvSourceBadge.setText("Clipboard:");
      _btnToggleSource.setText("Use Typed");
    }
    else if (_hasSelection)
    {
      _ivSourceIcon.setImageResource(R.drawable.ic_ai_edit);
      _tvSourceBadge.setText("Selected:");
      _btnToggleSource.setText("Clipboard");
    }
    else
    {
      _ivSourceIcon.setImageResource(R.drawable.ic_ai_edit);
      _tvSourceBadge.setText("Input:");
      _btnToggleSource.setText("Clipboard");
    }
    _ivSourceIcon.setColorFilter(accent, PorterDuff.Mode.SRC_IN);

    if (_currentWorkingText == null || _currentWorkingText.trim().isEmpty())
    {
      _tvInputSnippet.setText("Type in app, copy text, or tap Ask AI");
      _tvInputSnippet.setTextColor(adjustAlpha(_colorLabel, 0.45f));
    }
    else
    {
      _tvInputSnippet.setText(_currentWorkingText.trim().replace('\n', ' '));
      _tvInputSnippet.setTextColor(adjustAlpha(_colorLabel, 0.88f));
    }
  }

  private void updateEmojifyButtonState()
  {
    if (_btnEmojifyToggle == null) return;
    boolean enabled = GeminiAiService.isEmojifyEnabled(getContext());
    if (enabled)
    {
      int activeBg = (_colorKeyActivated != 0) ? _colorKeyActivated : Color.parseColor("#2196F3");
      _btnEmojifyToggle.setBackground(createPillBackground(activeBg, dp(12), Color.TRANSPARENT));
      _ivEmojifyIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
      _tvEmojifyLabel.setText("Emoji");
      _tvEmojifyLabel.setTextColor(Color.WHITE);
    }
    else
    {
      _btnEmojifyToggle.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.55f), dp(12), adjustAlpha(_colorLabel, 0.2f)));
      _ivEmojifyIcon.setColorFilter(adjustAlpha(_colorLabel, 0.7f), PorterDuff.Mode.SRC_IN);
      _tvEmojifyLabel.setText("Plain");
      _tvEmojifyLabel.setTextColor(adjustAlpha(_colorLabel, 0.7f));
    }
  }

  private static boolean containsBengaliChars(String s)
  {
    if (s == null) return false;
    for (int i = 0; i < s.length(); i++)
    {
      char c = s.charAt(i);
      if (c >= '\u0980' && c <= '\u09FF') return true;
    }
    return false;
  }

  /**
   * Builds the top 1-Tap Instant Actions strip so users can execute common tasks immediately in 1 tap.
   */
  private void rebuildQuickActions()
  {
    _rowQuickActions.removeAllViews();
    final Context ctx = getContext();
    final int accent = (_colorKeyActivated != 0) ? _colorKeyActivated : Color.parseColor("#2196F3");
    final boolean hasBn = containsBengaliChars(_currentWorkingText);

    // 1. Fix Grammar (1-Tap)
    addQuickActionChip(ctx, R.drawable.ic_ai_grammar, "Fix Grammar", accent, new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        runDirectAction(AiActionEngine.Category.GRAMMAR, "grammar_all");
      }
    });

    // 2. Smart Translate BN <-> EN (1-Tap)
    addQuickActionChip(ctx, R.drawable.ic_translate, hasBn ? "বাংলা → English" : "EN/Banglish → বাংলা", accent, new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        runDirectAction(AiActionEngine.Category.TRANSLATE, containsBengaliChars(_currentWorkingText) ? "tr_en" : "tr_bn");
      }
    });

    // 3. Professional Rewrite (1-Tap)
    addQuickActionChip(ctx, R.drawable.ic_ai_edit, "Professional", accent, new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        runDirectAction(AiActionEngine.Category.REWRITE, "prof");
      }
    });

    // 4. Islamic Style (1-Tap)
    addQuickActionChip(ctx, R.drawable.ic_ai_islamic, "Islamic", accent, new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        runDirectAction(AiActionEngine.Category.ISLAMIC, "islamic_standard");
      }
    });

    // 5. Smart Reply (1-Tap)
    addQuickActionChip(ctx, R.drawable.ic_ai_comment, "Smart Reply", accent, new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        runDirectAction(AiActionEngine.Category.REPLY, "reply_friendly");
      }
    });

    // 6. Summarize (1-Tap)
    addQuickActionChip(ctx, R.drawable.ic_ai_summarize, "Summarize", accent, new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        runDirectAction(AiActionEngine.Category.SUMMARIZE, "sum_short");
      }
    });

    // 7. Math Solver (1-Tap)
    addQuickActionChip(ctx, R.drawable.ic_ai_math, "Math Solver", accent, new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_keyboard != null)
        {
          String prob = (_currentWorkingText != null) ? _currentWorkingText.trim() : "";
          _keyboard.closeAiPane();
          _keyboard.showMathSolver(prob);
        }
      }
    });

    // 8. Vision / Screen Tools (1-Tap)
    addQuickActionChip(ctx, R.drawable.ic_ai_screen_scan, "Vision & Tools", accent, new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_keyboard != null)
        {
          _keyboard.closeAiPane();
          _keyboard.showMagicTools();
        }
      }
    });
  }

  private void addQuickActionChip(Context ctx, int iconRes, String label, int accent, OnClickListener listener)
  {
    LinearLayout chip = createIconPillChip(
        ctx,
        iconRes,
        label,
        adjustAlpha(accent, 0.16f),
        adjustAlpha(accent, 0.42f),
        _colorLabel,
        accent,
        dp(30),
        listener);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
    lp.setMargins(0, 0, dp(5), 0);
    chip.setLayoutParams(lp);
    _rowQuickActions.addView(chip);
  }

  private void runDirectAction(AiActionEngine.Category cat, String optionId)
  {
    _activeCategory = cat;
    _activeOptionId = optionId;
    _activeTone = "default";
    _llCustomPrompt.setVisibility(GONE);
    buildSubOptionChips();
    _sectionSubOptions.setVisibility(VISIBLE);
    buildCategoryGrid();
    triggerExecution();
  }

  /**
   * Builds the 3-column visual Category Grid with crisp vector icons and clear titles.
   */
  private void buildCategoryGrid()
  {
    if (_gridCategories == null) return;
    _gridCategories.removeAllViews();
    final Context ctx = getContext();
    final int accent = (_colorKeyActivated != 0) ? _colorKeyActivated : Color.parseColor("#2196F3");

    AiActionEngine.Category[] categories = AiActionEngine.Category.values();
    for (int i = 0; i < categories.length; i++)
    {
      final AiActionEngine.Category cat = categories[i];
      final boolean isSelected = (cat == _activeCategory);

      LinearLayout card = new LinearLayout(ctx);
      card.setOrientation(HORIZONTAL);
      card.setGravity(Gravity.CENTER_VERTICAL);
      card.setPadding(dp(8), dp(7), dp(8), dp(7));

      int bg = isSelected ? adjustAlpha(accent, 0.28f) : adjustAlpha(_colorKey, 0.55f);
      int stroke = isSelected ? accent : adjustAlpha(_colorLabel, 0.12f);
      card.setBackground(createPillBackground(bg, dp(10), stroke));

      ImageView iv = new ImageView(ctx);
      iv.setImageResource(cat.getIconResId());
      iv.setColorFilter(isSelected ? Color.WHITE : accent, PorterDuff.Mode.SRC_IN);
      LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp(16), dp(16));
      lpIcon.setMargins(0, 0, dp(6), 0);
      card.addView(iv, lpIcon);

      TextView tv = new TextView(ctx);
      tv.setText(cat.getTitle());
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
      tv.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
      tv.setTextColor(isSelected ? Color.WHITE : _colorLabel);
      tv.setSingleLine(true);
      tv.setEllipsize(TextUtils.TruncateAt.END);
      card.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

      GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
      glp.width = 0;
      glp.height = dp(38);
      glp.columnSpec = GridLayout.spec(i % 3, 1, 1.0f);
      glp.rowSpec = GridLayout.spec(i / 3);
      glp.setMargins(dp(3), dp(3), dp(3), dp(3));
      card.setLayoutParams(glp);

      card.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          onCategorySelected(cat);
        }
      });

      _gridCategories.addView(card);
    }
  }

  private void onCategorySelected(AiActionEngine.Category cat)
  {
    _activeCategory = cat;
    _activeOptionId = "";
    _activeTone = "default";
    buildCategoryGrid();

    if (cat == AiActionEngine.Category.ASK_AI)
    {
      _sectionSubOptions.setVisibility(GONE);
      _llCustomPrompt.setVisibility(VISIBLE);
      updatePromptDisplay();
      if (_currentCustomPrompt.isEmpty())
      {
        openKeyboardForPrompt();
      }
      return;
    }

    _llCustomPrompt.setVisibility(GONE);
    buildSubOptionChips();
    _sectionSubOptions.setVisibility(VISIBLE);
    if (_scrollBody != null)
    {
      _scrollBody.post(new Runnable()
      {
        @Override
        public void run()
        {
          if (_scrollBody != null && _sectionSubOptions != null)
          {
            _scrollBody.smoothScrollTo(0, _sectionSubOptions.getTop());
          }
        }
      });
    }
  }

  private void buildSubOptionChips()
  {
    _rowSubOptions.removeAllViews();
    if (_activeCategory == null || _activeCategory == AiActionEngine.Category.ASK_AI)
    {
      _sectionSubOptions.setVisibility(GONE);
      return;
    }

    _tvSelectedCategoryTitle.setText(_activeCategory.getTitle() + " — Choose Option:");
    final int accent = (_colorKeyActivated != 0) ? _colorKeyActivated : Color.parseColor("#2196F3");

    List<AiActionEngine.ActionOption> options = AiActionEngine.getOptionsForCategory(_activeCategory);
    for (final AiActionEngine.ActionOption opt : options)
    {
      final boolean isSelected = opt.id.equals(_activeOptionId);
      TextView chip = new TextView(getContext());
      chip.setText(opt.label);
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
      chip.setGravity(Gravity.CENTER);
      chip.setPadding(dp(10), 0, dp(10), 0);

      if (isSelected)
      {
        chip.setBackground(createPillBackground(accent, dp(14), Color.TRANSPARENT));
        chip.setTextColor(Color.WHITE);
        chip.setTypeface(null, Typeface.BOLD);
      }
      else
      {
        chip.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.7f), dp(14), adjustAlpha(_colorLabel, 0.16f)));
        chip.setTextColor(_colorLabel);
      }

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
      lp.setMargins(0, 0, dp(5), 0);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          _activeOptionId = opt.id;
          buildSubOptionChips();

          if ("select_tone".equals(opt.id))
          {
            buildToneChips();
            _scrollTone.setVisibility(VISIBLE);
            return;
          }

          _scrollTone.setVisibility(GONE);
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
    lbl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    lbl.setTypeface(null, Typeface.BOLD);
    lbl.setTextColor(adjustAlpha(_colorLabel, 0.75f));
    lbl.setGravity(Gravity.CENTER_VERTICAL);
    _rowToneChips.addView(lbl);

    String[] tones = {"Islamic", "Professional", "Friendly", "Casual", "Formal", "Confident", "Funny", "Empathetic", "Persuasive"};
    for (final String tone : tones)
    {
      final boolean isSelected = tone.equalsIgnoreCase(_activeTone);
      TextView chip = new TextView(getContext());
      chip.setText(tone);
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
      chip.setGravity(Gravity.CENTER);
      chip.setPadding(dp(8), 0, dp(8), 0);

      if (isSelected)
      {
        chip.setBackground(createPillBackground(adjustAlpha(Color.parseColor("#10B981"), 0.35f), dp(10), Color.parseColor("#10B981")));
        chip.setTextColor(Color.parseColor("#6EE7B7"));
        chip.setTypeface(null, Typeface.BOLD);
      }
      else
      {
        chip.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.45f), dp(10), adjustAlpha(_colorLabel, 0.14f)));
        chip.setTextColor(adjustAlpha(_colorLabel, 0.75f));
      }

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, dp(24));
      lp.setMargins(dp(2), 0, dp(3), 0);
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
      _llCustomPrompt.setVisibility(VISIBLE);
      buildCategoryGrid();
      triggerExecution();
    }
  }

  private void updatePromptDisplay()
  {
    if (_tvCustomPromptDisplay == null) return;
    if (_currentCustomPrompt.isEmpty())
    {
      _tvCustomPromptDisplay.setText("Tap to type custom instruction for AI...");
      _tvCustomPromptDisplay.setTextColor(adjustAlpha(_colorLabel, 0.5f));
    }
    else
    {
      _tvCustomPromptDisplay.setText(_currentCustomPrompt);
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

    showResultMode();
    _isGenerating = true;
    final long revision = ++_requestRevision;
    _progressBar.setVisibility(VISIBLE);
    _ivStatusIcon.setVisibility(GONE);
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
            if (revision != _requestRevision || _keyboard == null
                || _inputSessionId != _keyboard.getInputSessionId()) return;
            _isGenerating = false;
            _progressBar.setVisibility(GONE);
            _ivStatusIcon.setImageResource(R.drawable.ic_ai_check);
            _ivStatusIcon.setColorFilter(Color.parseColor("#10B981"), PorterDuff.Mode.SRC_IN);
            _ivStatusIcon.setVisibility(VISIBLE);
            _lastGeneratedResult = resultText;
            _tvStatus.setText("Ready to replace or insert");
            _tvResultPreview.setText(resultText);
            _tvResultPreview.scrollTo(0, 0);
          }

          @Override
          public void onError(final String errorMessage)
          {
            if (revision != _requestRevision || _keyboard == null
                || _inputSessionId != _keyboard.getInputSessionId()) return;
            _isGenerating = false;
            _progressBar.setVisibility(GONE);
            _ivStatusIcon.setImageResource(R.drawable.ic_ai_close);
            _ivStatusIcon.setColorFilter(Color.parseColor("#EF4444"), PorterDuff.Mode.SRC_IN);
            _ivStatusIcon.setVisibility(VISIBLE);
            _tvStatus.setText("Error: " + errorMessage);
            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
          }
        });
  }

  private void applyResultToField(final boolean replaceOriginal)
  {
    final String output = (_lastGeneratedResult != null && !_lastGeneratedResult.trim().isEmpty())
        ? _lastGeneratedResult
        : _tvResultPreview.getText().toString();
    if (output == null || output.trim().isEmpty())
    {
      Toast.makeText(getContext(), "No output to apply", Toast.LENGTH_SHORT).show();
      return;
    }

    try
    {
      ClipboardManager cm = (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
      if (cm != null)
      {
        cm.setPrimaryClip(ClipData.newPlainText("AI Generated", output));
      }
      typodev.keyboard.ClipboardHistoryService.setRecentCopiedClip(output);
      typodev.keyboard.ClipboardHistoryService hs = typodev.keyboard.ClipboardHistoryService.get_service(getContext());
      if (hs != null) hs.add_clip(output);
    }
    catch (Throwable ignored) {}

    if (_keyboard == null || _inputSessionId != _keyboard.getInputSessionId()) return;

    if (replaceOriginal && !_usingClipboard && !_inputFieldText.isEmpty()
        && !EditorTextGuard.matches(_keyboard.getTargetAppInputConnection(), _inputFieldText, _hasSelection))
    {
      Toast.makeText(getContext(), "Input changed. The generated text was copied.", Toast.LENGTH_SHORT).show();
      return;
    }

    _keyboard.closeAiPane();

    InputConnection ic = _keyboard.getTargetAppInputConnection();
    if (ic == null)
    {
      ic = _keyboard.getCurrentInputConnection();
    }

    boolean applied = false;
    if (ic != null)
    {
      try
      {
        ic.beginBatchEdit();
        if (replaceOriginal)
        {
          if (_hasSelection)
          {
            applied = ic.commitText(output, 1);
          }
          else if (!_usingClipboard && _inputFieldText != null && !_inputFieldText.isEmpty())
          {
            ic.deleteSurroundingText(_inputFieldText.length(), 0);
            applied = ic.commitText(output, 1);
          }
          else
          {
            applied = ic.commitText(output, 1);
          }
        }
        else
        {
          applied = ic.commitText(output, 1);
        }
        ic.endBatchEdit();
      }
      catch (Throwable t)
      {
        Logs.print_exception(t);
      }
    }

    if (!applied)
    {
      typodev.keyboard.ClipboardHistoryService.paste(output);
    }

    Toast.makeText(getContext(), "Text applied", Toast.LENGTH_SHORT).show();
  }

  private ImageView createCircleIconButton(Context context, int iconResId, int sizePx, int padPx, OnClickListener listener)
  {
    ImageView iv = new ImageView(context);
    iv.setImageResource(iconResId);
    iv.setColorFilter(_colorLabel, PorterDuff.Mode.SRC_IN);
    iv.setPadding(padPx, padPx, padPx, padPx);
    iv.setBackground(createPillBackground(Color.TRANSPARENT, sizePx / (2f * _density), adjustAlpha(_colorLabel, 0.16f)));
    iv.setLayoutParams(new LinearLayout.LayoutParams(sizePx, sizePx));
    iv.setOnClickListener(listener);
    return iv;
  }

  private LinearLayout createIconPillChip(
      Context context,
      int iconResId,
      String label,
      int bgColor,
      int strokeColor,
      int textColor,
      int iconTint,
      int heightPx,
      OnClickListener listener)
  {
    LinearLayout ll = new LinearLayout(context);
    ll.setOrientation(HORIZONTAL);
    ll.setGravity(Gravity.CENTER_VERTICAL);
    ll.setPadding(dp(9), 0, dp(9), 0);
    ll.setBackground(createPillBackground(bgColor, heightPx / (2f * _density), strokeColor));

    ImageView iv = new ImageView(context);
    iv.setImageResource(iconResId);
    iv.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN);
    LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp(14), dp(14));
    lpIcon.setMargins(0, 0, dp(5), 0);
    ll.addView(iv, lpIcon);

    TextView tv = new TextView(context);
    tv.setText(label);
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    tv.setTypeface(null, Typeface.BOLD);
    tv.setTextColor(textColor);
    tv.setSingleLine(true);
    ll.addView(tv);

    ll.setOnClickListener(listener);
    return ll;
  }

  private LinearLayout createActionDockButton(
      Context context,
      int iconResId,
      String label,
      int bgColor,
      int strokeColor,
      OnClickListener listener)
  {
    LinearLayout ll = new LinearLayout(context);
    ll.setOrientation(HORIZONTAL);
    ll.setGravity(Gravity.CENTER);
    ll.setPadding(dp(8), 0, dp(8), 0);

    GradientDrawable normalBg = createPillBackground(bgColor, dp(19), strokeColor);
    GradientDrawable pressedBg = createPillBackground(adjustAlpha(bgColor, 0.75f), dp(19), strokeColor);
    StateListDrawable sld = new StateListDrawable();
    sld.addState(new int[]{android.R.attr.state_pressed}, pressedBg);
    sld.addState(new int[]{}, normalBg);
    ll.setBackground(sld);

    ImageView iv = new ImageView(context);
    iv.setImageResource(iconResId);
    iv.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
    LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp(15), dp(15));
    lpIcon.setMargins(0, 0, dp(5), 0);
    ll.addView(iv, lpIcon);

    TextView tv = new TextView(context);
    tv.setText(label);
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    tv.setTypeface(null, Typeface.BOLD);
    tv.setTextColor(Color.WHITE);
    tv.setSingleLine(true);
    ll.addView(tv);

    ll.setOnClickListener(listener);
    return ll;
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
