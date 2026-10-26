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
import typodev.keyboard.Keyboard2;
import typodev.keyboard.Logs;
import typodev.keyboard.R;

/**
 * Fast, icon-driven in-keyboard AI Assistant view.
 *
 * Designed for maximum speed and preview clarity (matching the Magic Tools preview style):
 * - No "AI Assistant" or provider name clutter in the header.
 * - High-contrast vector icons in both Dark Mode and Light Mode.
 * - In Tools Mode: Top bar (Back, Ask AI, Source toggle, Emoji toggle, Settings) + Quick/Sub-option strip + 3-column Category Grid.
 * - In Result Preview Mode: Top bar holds Back + Replace + Insert + Copy + Regenerate + Settings,
 *   followed by a single sub-option chip row and a full-height Result Preview card (no bottom bar or clipboard clutter).
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

  // Top Header Views
  private ImageView _btnBack;
  private LinearLayout _toolsHeaderGroup;
  private LinearLayout _btnAskHeader;
  private ImageView _ivAskHeaderIcon;
  private TextView _tvAskHeaderLabel;
  private LinearLayout _btnEmojifyToggle;
  private ImageView _ivEmojifyIcon;
  private TextView _tvEmojifyLabel;

  private LinearLayout _resultHeaderActionsGroup;
  private LinearLayout _btnTopReplace;
  private LinearLayout _btnTopInsert;
  private LinearLayout _btnTopCopy;
  private ImageView _btnTopRegen;
  private ImageView _btnSettings;

  // Setup notice, Clipboard/Input context preview & Custom prompt
  private LinearLayout _cardSetupNotice;
  private LinearLayout _cardContext;
  private ImageView _ivContextIcon;
  private TextView _tvSourceBadge;
  private TextView _tvInputSnippet;
  private TextView _btnToggleSource;
  private LinearLayout _llCustomPrompt;
  private TextView _tvCustomPromptDisplay;

  // Horizontal Chips Strip (Quick actions in Tools Mode / Sub-options in Category or Result Mode)
  private HorizontalScrollView _scrollSubOptions;
  private LinearLayout _rowSubOptions;
  private HorizontalScrollView _scrollTone;
  private LinearLayout _rowToneChips;

  // Tools Grid Mode container
  private ScrollView _scrollGrid;
  private GridLayout _gridCategories;

  // Full-Height Result Preview Mode container (Magic button preview style)
  private LinearLayout _resultCard;
  private LinearLayout _loadingRow;
  private ProgressBar _progressBar;
  private TextView _tvLoadingStatus;
  private ScrollView _scrollResult;
  private TextView _tvResultPreview;

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
    _colorLabel = resolveColor(context, R.attr.colorLabel, Color.WHITE);
    _colorSubLabel = resolveColor(context, R.attr.colorSubLabel, Color.parseColor("#AAAAAA"));

    try
    {
      typodev.keyboard.Config cfg = typodev.keyboard.Config.globalConfig();
      typodev.keyboard.CustomThemeStore cs = typodev.keyboard.CustomThemeStore.instance(context);
      if (cs != null && cs.isCustomThemeActive(cfg))
      {
        _colorKeyboard = cs.getKeyboardBg();
        _colorKey = cs.getKeyNormal();
        _colorLabel = cs.getLabelColor();
        _colorSubLabel = cs.getSubLabelColor();
      }
    }
    catch (Throwable ignored) {}

    double darkness = 1 - (0.299 * Color.red(_colorKeyboard) + 0.587 * Color.green(_colorKeyboard) + 0.114 * Color.blue(_colorKeyboard)) / 255.0;
    _isDark = darkness >= 0.5;
    // Always use a vibrant, visible accent color rather than R.attr.colorKeyActivated (which is dark gray in dark themes)
    _colorKeyActivated = _isDark ? Color.parseColor("#2563EB") : Color.parseColor("#1D4ED8");
  }

  /** High-contrast icon tint that is always bright and clear in Dark Mode and crisp in Light Mode. */
  private int getIconColor()
  {
    return _isDark ? Color.parseColor("#38BDF8") : Color.parseColor("#1D4ED8");
  }

  private int getPrimaryIconColor()
  {
    return _isDark ? Color.WHITE : _colorLabel;
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
    int minSafeBottom = dp(12);
    _bottomSafety = Math.max(rawBottomSafety, minSafeBottom);
    setPadding(dp(10), dp(6), dp(10), dp(6) + _bottomSafety);
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
    setPadding(dp(10), dp(6), dp(10), dp(18));

    // ==========================================
    // 1. Sleek Top Header Bar
    //    - No "AI Assistant" text
    //    - No "Gemini / Groq / OpenAI" badge
    //    - Holds Tools Controls in Tools Mode, and Replace / Insert / Copy / Regen in Result Mode!
    // ==========================================
    LinearLayout headerBar = new LinearLayout(context);
    headerBar.setOrientation(HORIZONTAL);
    headerBar.setGravity(Gravity.CENTER_VERTICAL);

    _btnBack = createCircleIconButton(context, R.drawable.ic_ai_back, dp(36), dp(9), new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_showingResultMode)
        {
          ++_requestRevision;
          _isGenerating = false;
          showToolsGridMode();
          return;
        }
        if (_activeCategory != null)
        {
          _activeCategory = null;
          _activeOptionId = "";
          _llCustomPrompt.setVisibility(GONE);
          _scrollTone.setVisibility(GONE);
          buildQuickOrSubOptionStrip();
          buildCategoryGrid();
          return;
        }
        if (_keyboard != null) _keyboard.closeAiPane();
      }
    });
    headerBar.addView(_btnBack);

    // 1A. Tools Mode Header Group (Ask AI + Source Toggle + Emoji Toggle)
    _toolsHeaderGroup = new LinearLayout(context);
    _toolsHeaderGroup.setOrientation(HORIZONTAL);
    _toolsHeaderGroup.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams lpToolsHeader = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    lpToolsHeader.setMargins(dp(6), 0, dp(4), 0);
    _toolsHeaderGroup.setLayoutParams(lpToolsHeader);

    // Ask AI Button
    _btnAskHeader = new LinearLayout(context);
    _btnAskHeader.setOrientation(HORIZONTAL);
    _btnAskHeader.setGravity(Gravity.CENTER_VERTICAL);
    _btnAskHeader.setPadding(dp(10), 0, dp(10), 0);
    _ivAskHeaderIcon = new ImageView(context);
    _ivAskHeaderIcon.setImageResource(R.drawable.ic_ai_send);
    LinearLayout.LayoutParams lpAskIcon = new LinearLayout.LayoutParams(dp(14), dp(14));
    lpAskIcon.setMargins(0, 0, dp(5), 0);
    _btnAskHeader.addView(_ivAskHeaderIcon, lpAskIcon);
    _tvAskHeaderLabel = new TextView(context);
    _tvAskHeaderLabel.setText("Ask AI");
    _tvAskHeaderLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _tvAskHeaderLabel.setTypeface(null, Typeface.BOLD);
    _btnAskHeader.addView(_tvAskHeaderLabel);
    _btnAskHeader.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        _activeCategory = AiActionEngine.Category.ASK_AI;
        _llCustomPrompt.setVisibility(VISIBLE);
        openKeyboardForPrompt();
      }
    });
    LinearLayout.LayoutParams lpAskBtn = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
    lpAskBtn.setMargins(0, 0, dp(6), 0);
    _toolsHeaderGroup.addView(_btnAskHeader, lpAskBtn);

    View toolsSpacer = new View(context);
    _toolsHeaderGroup.addView(toolsSpacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

    // Emojify Toggle Pill
    _btnEmojifyToggle = new LinearLayout(context);
    _btnEmojifyToggle.setOrientation(HORIZONTAL);
    _btnEmojifyToggle.setGravity(Gravity.CENTER_VERTICAL);
    _btnEmojifyToggle.setPadding(dp(9), 0, dp(9), 0);
    _ivEmojifyIcon = new ImageView(context);
    _ivEmojifyIcon.setImageResource(R.drawable.ic_ai_humor);
    LinearLayout.LayoutParams lpEmoIcon = new LinearLayout.LayoutParams(dp(14), dp(14));
    lpEmoIcon.setMargins(0, 0, dp(4), 0);
    _btnEmojifyToggle.addView(_ivEmojifyIcon, lpEmoIcon);
    _tvEmojifyLabel = new TextView(context);
    _tvEmojifyLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _tvEmojifyLabel.setTypeface(null, Typeface.BOLD);
    _btnEmojifyToggle.addView(_tvEmojifyLabel);
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
    LinearLayout.LayoutParams lpEmoBtn = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(32));
    _toolsHeaderGroup.addView(_btnEmojifyToggle, lpEmoBtn);

    headerBar.addView(_toolsHeaderGroup);

    // 1B. Result Mode Header Actions Group (Replace, Insert, Copy, Regenerate in Top Bar!)
    _resultHeaderActionsGroup = new LinearLayout(context);
    _resultHeaderActionsGroup.setOrientation(HORIZONTAL);
    _resultHeaderActionsGroup.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout.LayoutParams lpResHeader = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    lpResHeader.setMargins(dp(6), 0, dp(4), 0);
    _resultHeaderActionsGroup.setLayoutParams(lpResHeader);
    _resultHeaderActionsGroup.setVisibility(GONE);

    _btnTopReplace = createTopActionButton(
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
    LinearLayout.LayoutParams lpTopReplace = new LinearLayout.LayoutParams(0, dp(34), 1.15f);
    lpTopReplace.setMargins(0, 0, dp(5), 0);
    _resultHeaderActionsGroup.addView(_btnTopReplace, lpTopReplace);

    _btnTopInsert = createTopActionButton(
        context,
        R.drawable.ic_clip_add,
        "Insert",
        Color.parseColor("#2563EB"),
        Color.parseColor("#60A5FA"),
        new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            applyResultToField(false);
          }
        });
    LinearLayout.LayoutParams lpTopInsert = new LinearLayout.LayoutParams(0, dp(34), 1.05f);
    lpTopInsert.setMargins(0, 0, dp(5), 0);
    _resultHeaderActionsGroup.addView(_btnTopInsert, lpTopInsert);

    _btnTopCopy = createTopActionButton(
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
            copyResultToClipboard();
          }
        });
    LinearLayout.LayoutParams lpTopCopy = new LinearLayout.LayoutParams(0, dp(34), 0.95f);
    lpTopCopy.setMargins(0, 0, dp(5), 0);
    _resultHeaderActionsGroup.addView(_btnTopCopy, lpTopCopy);

    _btnTopRegen = createCircleIconButton(
        context,
        R.drawable.ic_ai_refresh,
        dp(34),
        dp(8),
        new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            triggerExecution();
          }
        });
    _resultHeaderActionsGroup.addView(_btnTopRegen);

    headerBar.addView(_resultHeaderActionsGroup);

    // Settings Icon Button (Rightmost)
    _btnSettings = createCircleIconButton(context, R.drawable.ic_ai_settings, dp(36), dp(9), new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        openSettingsDialog();
      }
    });
    LinearLayout.LayoutParams lpSettings = new LinearLayout.LayoutParams(dp(36), dp(36));
    lpSettings.setMargins(dp(4), 0, 0, 0);
    headerBar.addView(_btnSettings, lpSettings);

    addView(headerBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

    // ==========================================
    // 2. API Key Setup Notice (Only if no key is configured)
    // ==========================================
    _cardSetupNotice = new LinearLayout(context);
    _cardSetupNotice.setOrientation(HORIZONTAL);
    _cardSetupNotice.setGravity(Gravity.CENTER_VERTICAL);
    _cardSetupNotice.setPadding(dp(10), dp(5), dp(10), dp(5));
    _cardSetupNotice.setBackground(createPillBackground(adjustAlpha(Color.parseColor("#FF9800"), 0.18f), dp(8), Color.parseColor("#FF9800")));
    LinearLayout.LayoutParams lpNotice = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpNotice.setMargins(0, dp(4), 0, dp(2));
    _cardSetupNotice.setLayoutParams(lpNotice);

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
    addView(_cardSetupNotice);

    // ==========================================
    // 2B. Clipboard / Input Context Preview Bar
    //     Visible in Tools Mode when opening AI Assistant;
    //     Hidden in Result Mode after generating!
    // ==========================================
    _cardContext = new LinearLayout(context);
    _cardContext.setOrientation(HORIZONTAL);
    _cardContext.setGravity(Gravity.CENTER_VERTICAL);
    _cardContext.setPadding(dp(10), dp(5), dp(8), dp(5));
    LinearLayout.LayoutParams lpCtx = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
    lpCtx.setMargins(0, dp(4), 0, dp(2));
    _cardContext.setLayoutParams(lpCtx);

    _ivContextIcon = new ImageView(context);
    _ivContextIcon.setImageResource(R.drawable.ic_toolbar_clipboard);
    LinearLayout.LayoutParams lpCtxIcon = new LinearLayout.LayoutParams(dp(15), dp(15));
    lpCtxIcon.setMargins(0, 0, dp(6), 0);
    _cardContext.addView(_ivContextIcon, lpCtxIcon);

    _tvSourceBadge = new TextView(context);
    _tvSourceBadge.setText("Clipboard:");
    _tvSourceBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
    _tvSourceBadge.setTypeface(null, Typeface.BOLD);
    _tvSourceBadge.setPadding(0, 0, dp(6), 0);
    _cardContext.addView(_tvSourceBadge);

    _tvInputSnippet = new TextView(context);
    _tvInputSnippet.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    _tvInputSnippet.setSingleLine(true);
    _tvInputSnippet.setEllipsize(TextUtils.TruncateAt.END);
    _cardContext.addView(_tvInputSnippet, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

    _btnToggleSource = new TextView(context);
    _btnToggleSource.setText("Use Typed");
    _btnToggleSource.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    _btnToggleSource.setTypeface(null, Typeface.BOLD);
    _btnToggleSource.setGravity(Gravity.CENTER);
    _btnToggleSource.setPadding(dp(8), 0, dp(8), 0);
    _btnToggleSource.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        _usingClipboard = !_usingClipboard;
        _currentWorkingText = _usingClipboard ? _clipboardText : _inputFieldText;
        updateContextDisplay();
        if (_activeCategory == null) buildQuickOrSubOptionStrip();
      }
    });
    LinearLayout.LayoutParams lpToggleSrc = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, dp(24));
    lpToggleSrc.setMargins(dp(6), 0, 0, 0);
    _cardContext.addView(_btnToggleSource, lpToggleSrc);

    addView(_cardContext);

    // ==========================================
    // 3. Custom Prompt Bar (Only visible when Ask AI is active)
    // ==========================================
    _llCustomPrompt = new LinearLayout(context);
    _llCustomPrompt.setOrientation(HORIZONTAL);
    _llCustomPrompt.setGravity(Gravity.CENTER_VERTICAL);
    _llCustomPrompt.setPadding(0, dp(4), 0, dp(2));

    _tvCustomPromptDisplay = new TextView(context);
    _tvCustomPromptDisplay.setText("Tap to type custom instruction for AI...");
    _tvCustomPromptDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    _tvCustomPromptDisplay.setSingleLine(true);
    _tvCustomPromptDisplay.setEllipsize(TextUtils.TruncateAt.END);
    _tvCustomPromptDisplay.setGravity(Gravity.CENTER_VERTICAL);
    _tvCustomPromptDisplay.setPadding(dp(10), 0, dp(10), 0);
    _tvCustomPromptDisplay.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        openKeyboardForPrompt();
      }
    });
    _llCustomPrompt.addView(_tvCustomPromptDisplay, new LinearLayout.LayoutParams(0, dp(34), 1.0f));

    LinearLayout btnSendPrompt = createTopActionButton(
        context,
        R.drawable.ic_ai_send,
        "Run",
        Color.parseColor("#2563EB"),
        Color.TRANSPARENT,
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
    addView(_llCustomPrompt);

    // ==========================================
    // 4. Category Sub-Options Row (Only visible after selecting a category)
    // ==========================================
    _scrollSubOptions = new HorizontalScrollView(context);
    _scrollSubOptions.setHorizontalScrollBarEnabled(false);
    _scrollSubOptions.setVerticalScrollBarEnabled(false);
    _scrollSubOptions.setOverScrollMode(View.OVER_SCROLL_NEVER);
    _rowSubOptions = new LinearLayout(context);
    _rowSubOptions.setOrientation(HORIZONTAL);
    _rowSubOptions.setGravity(Gravity.CENTER_VERTICAL);
    _scrollSubOptions.addView(_rowSubOptions);
    LinearLayout.LayoutParams lpSubScroll = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dp(36));
    lpSubScroll.setMargins(0, dp(4), 0, dp(4));
    _scrollSubOptions.setLayoutParams(lpSubScroll);
    _scrollSubOptions.setVisibility(GONE);
    addView(_scrollSubOptions);

    // Optional Tone Strip
    _scrollTone = new HorizontalScrollView(context);
    _scrollTone.setHorizontalScrollBarEnabled(false);
    _scrollTone.setOverScrollMode(View.OVER_SCROLL_NEVER);
    _rowToneChips = new LinearLayout(context);
    _rowToneChips.setOrientation(HORIZONTAL);
    _rowToneChips.setGravity(Gravity.CENTER_VERTICAL);
    _scrollTone.addView(_rowToneChips);
    LinearLayout.LayoutParams lpTone = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dp(28));
    lpTone.setMargins(0, 0, 0, dp(4));
    _scrollTone.setLayoutParams(lpTone);
    _scrollTone.setVisibility(GONE);
    addView(_scrollTone);

    // ==========================================
    // 5A. [TOOLS MODE] 3-Column Visual Category Grid
    // ==========================================
    _scrollGrid = new ScrollView(context);
    _scrollGrid.setFillViewport(true);
    _scrollGrid.setVerticalScrollBarEnabled(false);
    _scrollGrid.setOverScrollMode(View.OVER_SCROLL_NEVER);

    _gridCategories = new GridLayout(context);
    _gridCategories.setColumnCount(3);
    _gridCategories.setUseDefaultMargins(false);
    _scrollGrid.addView(_gridCategories, new ScrollView.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    addView(_scrollGrid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

    // ==========================================
    // 5B. [RESULT MODE] Full-Height Result Preview Card (Magic Button Style!)
    // ==========================================
    _resultCard = new LinearLayout(context);
    _resultCard.setOrientation(VERTICAL);
    _resultCard.setPadding(dp(14), dp(10), dp(14), dp(10));
    _resultCard.setVisibility(GONE);

    _loadingRow = new LinearLayout(context);
    _loadingRow.setOrientation(HORIZONTAL);
    _loadingRow.setGravity(Gravity.CENTER_VERTICAL);
    _loadingRow.setPadding(0, 0, 0, dp(6));
    _loadingRow.setVisibility(GONE);

    _progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
    _loadingRow.addView(_progressBar, new LinearLayout.LayoutParams(dp(20), dp(20)));

    _tvLoadingStatus = new TextView(context);
    _tvLoadingStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    _tvLoadingStatus.setPadding(dp(8), 0, 0, 0);
    _loadingRow.addView(_tvLoadingStatus);
    _resultCard.addView(_loadingRow);

    _scrollResult = new ScrollView(context);
    _scrollResult.setFillViewport(true);
    _scrollResult.setVerticalScrollBarEnabled(true);
    _scrollResult.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

    _tvResultPreview = new TextView(context);
    _tvResultPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    _tvResultPreview.setGravity(Gravity.TOP);
    _tvResultPreview.setLineSpacing(dp(3), 1.0f);
    _tvResultPreview.setHint("Generated text will appear here...");
    _tvResultPreview.setClickable(false);
    _tvResultPreview.setLongClickable(false);
    _scrollResult.addView(_tvResultPreview, new ScrollView.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    _resultCard.addView(_scrollResult, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

    addView(_resultCard, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
    refreshHeaderAndCardThemes();
  }

  private void refreshHeaderAndCardThemes()
  {
    setBackgroundColor(_colorKeyboard);
    int iconWhiteOrDark = getPrimaryIconColor();
    int iconAccent = getIconColor();

    if (_btnBack != null)
    {
      _btnBack.setColorFilter(iconWhiteOrDark, PorterDuff.Mode.SRC_IN);
      _btnBack.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.7f), dp(18), adjustAlpha(_colorLabel, 0.22f)));
    }
    if (_btnSettings != null)
    {
      _btnSettings.setColorFilter(iconWhiteOrDark, PorterDuff.Mode.SRC_IN);
      _btnSettings.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.7f), dp(18), adjustAlpha(_colorLabel, 0.22f)));
    }
    if (_btnTopRegen != null)
    {
      _btnTopRegen.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
      _btnTopRegen.setBackground(createPillBackground(Color.parseColor("#374151"), dp(17), Color.parseColor("#6B7280")));
    }
    if (_btnAskHeader != null)
    {
      _btnAskHeader.setBackground(createPillBackground(
          adjustAlpha(_colorKeyActivated, 0.25f), dp(16), adjustAlpha(iconAccent, 0.65f)));
      _ivAskHeaderIcon.setColorFilter(iconAccent, PorterDuff.Mode.SRC_IN);
      _tvAskHeaderLabel.setTextColor(_colorLabel);
    }
    if (_tvCustomPromptDisplay != null)
    {
      _tvCustomPromptDisplay.setBackground(createPillBackground(
          adjustAlpha(_colorKey, 0.75f), dp(10), adjustAlpha(_colorLabel, 0.22f)));
    }
    if (_resultCard != null)
    {
      _resultCard.setBackground(createPillBackground(_colorKey, dp(16), adjustAlpha(_colorLabel, 0.14f)));
    }
    if (_tvResultPreview != null)
    {
      _tvResultPreview.setTextColor(_colorLabel);
      _tvResultPreview.setHintTextColor(adjustAlpha(_colorLabel, 0.45f));
    }
    if (_tvLoadingStatus != null)
    {
      _tvLoadingStatus.setTextColor(adjustAlpha(_colorLabel, 0.8f));
    }
    updateContextDisplay();
    updateEmojifyButtonState();
  }

  private void showToolsGridMode()
  {
    _showingResultMode = false;
    if (_toolsHeaderGroup != null) _toolsHeaderGroup.setVisibility(VISIBLE);
    if (_resultHeaderActionsGroup != null) _resultHeaderActionsGroup.setVisibility(GONE);
    if (_cardContext != null) _cardContext.setVisibility(VISIBLE);
    if (_scrollGrid != null) _scrollGrid.setVisibility(VISIBLE);
    if (_resultCard != null) _resultCard.setVisibility(GONE);
    buildQuickOrSubOptionStrip();
    buildCategoryGrid();
  }

  private void showResultMode()
  {
    _showingResultMode = true;
    if (_toolsHeaderGroup != null) _toolsHeaderGroup.setVisibility(GONE);
    if (_resultHeaderActionsGroup != null) _resultHeaderActionsGroup.setVisibility(VISIBLE);
    if (_cardContext != null) _cardContext.setVisibility(GONE);
    if (_llCustomPrompt != null) _llCustomPrompt.setVisibility(GONE);
    if (_scrollGrid != null) _scrollGrid.setVisibility(GONE);
    if (_resultCard != null) _resultCard.setVisibility(VISIBLE);
    buildQuickOrSubOptionStrip();
  }

  public void open(int targetHeight, int bottomSafety)
  {
    ++_requestRevision;
    _isGenerating = false;
    _lastGeneratedResult = "";
    if (_tvResultPreview != null) _tvResultPreview.setText("");
    if (_loadingRow != null) _loadingRow.setVisibility(GONE);
    _inputSessionId = _keyboard != null ? _keyboard.getInputSessionId() : -1;
    try
    {
      Context context = getContext();
      initThemeColors(context);
      refreshHeaderAndCardThemes();

      _activeCategory = null;
      _activeOptionId = "";
      _activeTone = "default";
      _currentCustomPrompt = "";
      updatePromptDisplay();
      if (_scrollTone != null) _scrollTone.setVisibility(GONE);
      if (_llCustomPrompt != null) _llCustomPrompt.setVisibility(GONE);

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
      showToolsGridMode();
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
        _cardSetupNotice.setVisibility(AiProvider.Manager.hasConfiguredApiKey(ctx) ? GONE : VISIBLE);
      }
    });
  }

  private void updateContextDisplay()
  {
    if (_cardContext == null) return;
    int iconColor = getIconColor();
    _cardContext.setBackground(createPillBackground(
        adjustAlpha(_colorKey, 0.7f), dp(10), adjustAlpha(_colorLabel, 0.16f)));
    _ivContextIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
    _tvSourceBadge.setTextColor(iconColor);

    if (_usingClipboard)
    {
      _ivContextIcon.setImageResource(R.drawable.ic_toolbar_clipboard);
      _tvSourceBadge.setText("Clipboard:");
      _btnToggleSource.setText("Use Typed");
    }
    else
    {
      _ivContextIcon.setImageResource(R.drawable.ic_ai_edit);
      _tvSourceBadge.setText(_hasSelection ? "Selected:" : "Input:");
      _btnToggleSource.setText("Use Clipboard");
    }

    _btnToggleSource.setTextColor(_colorLabel);
    _btnToggleSource.setBackground(createPillBackground(
        adjustAlpha(_colorKeyActivated, 0.25f), dp(8), adjustAlpha(iconColor, 0.55f)));

    if (_currentWorkingText == null || _currentWorkingText.trim().isEmpty())
    {
      _tvInputSnippet.setText(_usingClipboard
          ? "Clipboard is empty — copy text or switch to Typed"
          : "Type, select, or copy text first...");
      _tvInputSnippet.setTextColor(adjustAlpha(_colorLabel, 0.5f));
    }
    else
    {
      String singleLine = _currentWorkingText.replace('\n', ' ').replace('\r', ' ').trim();
      _tvInputSnippet.setText(singleLine);
      _tvInputSnippet.setTextColor(_colorLabel);
    }
  }

  private void updateEmojifyButtonState()
  {
    if (_btnEmojifyToggle == null) return;
    boolean enabled = GeminiAiService.isEmojifyEnabled(getContext());
    if (enabled)
    {
      _btnEmojifyToggle.setBackground(createPillBackground(_colorKeyActivated, dp(16), Color.TRANSPARENT));
      _ivEmojifyIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
      _tvEmojifyLabel.setText("Emoji");
      _tvEmojifyLabel.setTextColor(Color.WHITE);
    }
    else
    {
      _btnEmojifyToggle.setBackground(createPillBackground(
          adjustAlpha(_colorKey, 0.75f), dp(16), adjustAlpha(_colorLabel, 0.22f)));
      _ivEmojifyIcon.setColorFilter(getPrimaryIconColor(), PorterDuff.Mode.SRC_IN);
      _tvEmojifyLabel.setText("Plain");
      _tvEmojifyLabel.setTextColor(_colorLabel);
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
   * Populates the category sub-option strip only when a category is selected.
   * Hidden when no category is selected so no horizontal suggestion bar appears below Clipboard.
   */
  private void buildQuickOrSubOptionStrip()
  {
    if (_rowSubOptions == null || _scrollSubOptions == null) return;
    _rowSubOptions.removeAllViews();
    final Context ctx = getContext();

    if (_activeCategory != null && _activeCategory != AiActionEngine.Category.ASK_AI)
    {
      _scrollSubOptions.setVisibility(VISIBLE);
      List<AiActionEngine.ActionOption> options = AiActionEngine.getOptionsForCategory(_activeCategory);
      for (final AiActionEngine.ActionOption opt : options)
      {
        final boolean isSelected = opt.id.equals(_activeOptionId);
        TextView chip = new TextView(ctx);
        chip.setText(opt.label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(12), 0, dp(12), 0);

        if (isSelected)
        {
          chip.setBackground(createPillBackground(_colorKeyActivated, dp(17), Color.WHITE));
          chip.setTextColor(Color.WHITE);
          chip.setTypeface(null, Typeface.BOLD);
        }
        else
        {
          chip.setBackground(createPillBackground(_colorKey, dp(17), adjustAlpha(_colorLabel, 0.22f)));
          chip.setTextColor(_colorLabel);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        lp.setMargins(0, 0, dp(5), 0);
        chip.setLayoutParams(lp);

        chip.setOnClickListener(new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            _activeOptionId = opt.id;
            buildQuickOrSubOptionStrip();

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
      return;
    }

    _scrollSubOptions.setVisibility(GONE);
  }

  /**
   * Builds the 3-column visual Category Grid with high-contrast vector icons,
   * including Math Solver and Vision AI.
   */
  private void buildCategoryGrid()
  {
    if (_gridCategories == null) return;
    _gridCategories.removeAllViews();
    final Context ctx = getContext();

    AiActionEngine.Category[] categories = AiActionEngine.Category.values();
    int gridIdx = 0;
    for (int i = 0; i < categories.length; i++)
    {
      final AiActionEngine.Category cat = categories[i];
      final boolean isSelected = (cat == _activeCategory);
      addGridToolCard(ctx, gridIdx++, cat.getIconResId(), cat.getTitle(), isSelected, new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          onCategorySelected(cat);
        }
      });
    }

    // Add Math Solver to AI Tools Grid
    addGridToolCard(ctx, gridIdx++, R.drawable.ic_ai_math, "Math Solver", false, new OnClickListener()
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

    // Add Vision AI to AI Tools Grid
    addGridToolCard(ctx, gridIdx++, R.drawable.ic_ai_screen_scan, "Vision AI", false, new OnClickListener()
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

  private void addGridToolCard(
      Context ctx,
      int index,
      int iconResId,
      String title,
      boolean isSelected,
      OnClickListener listener)
  {
    int iconColor = getIconColor();
    LinearLayout card = new LinearLayout(ctx);
    card.setOrientation(HORIZONTAL);
    card.setGravity(Gravity.CENTER_VERTICAL);
    card.setPadding(dp(9), dp(7), dp(8), dp(7));

    int bg = isSelected ? _colorKeyActivated : _colorKey;
    int stroke = isSelected ? Color.WHITE : adjustAlpha(_colorLabel, 0.18f);
    card.setBackground(createPillBackground(bg, dp(12), stroke));

    ImageView iv = new ImageView(ctx);
    iv.setImageResource(iconResId);
    iv.setColorFilter(isSelected ? Color.WHITE : iconColor, PorterDuff.Mode.SRC_IN);
    LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp(18), dp(18));
    lpIcon.setMargins(0, 0, dp(7), 0);
    card.addView(iv, lpIcon);

    TextView tv = new TextView(ctx);
    tv.setText(title);
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    tv.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
    tv.setTextColor(isSelected ? Color.WHITE : _colorLabel);
    tv.setSingleLine(true);
    tv.setEllipsize(TextUtils.TruncateAt.END);
    card.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

    GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
    glp.width = 0;
    glp.height = dp(40);
    glp.columnSpec = GridLayout.spec(index % 3, 1, 1.0f);
    glp.rowSpec = GridLayout.spec(index / 3);
    glp.setMargins(dp(3), dp(3), dp(3), dp(3));
    card.setLayoutParams(glp);

    card.setOnClickListener(listener);
    _gridCategories.addView(card);
  }

  private void onCategorySelected(AiActionEngine.Category cat)
  {
    _activeCategory = cat;
    _activeOptionId = "";
    _activeTone = "default";
    buildCategoryGrid();

    if (cat == AiActionEngine.Category.ASK_AI)
    {
      _llCustomPrompt.setVisibility(VISIBLE);
      updatePromptDisplay();
      if (_currentCustomPrompt.isEmpty())
      {
        openKeyboardForPrompt();
      }
      return;
    }

    _llCustomPrompt.setVisibility(GONE);
    _scrollTone.setVisibility(GONE);
    buildQuickOrSubOptionStrip();
    if (_scrollSubOptions != null)
    {
      _scrollSubOptions.scrollTo(0, 0);
    }
  }

  private void buildToneChips()
  {
    _rowToneChips.removeAllViews();
    String[] tones = {"Islamic", "Professional", "Friendly", "Casual", "Formal", "Confident", "Funny", "Empathetic", "Persuasive"};
    for (final String tone : tones)
    {
      final boolean isSelected = tone.equalsIgnoreCase(_activeTone);
      TextView chip = new TextView(getContext());
      chip.setText(tone);
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
      chip.setGravity(Gravity.CENTER);
      chip.setPadding(dp(10), 0, dp(10), 0);

      if (isSelected)
      {
        chip.setBackground(createPillBackground(Color.parseColor("#059669"), dp(12), Color.WHITE));
        chip.setTextColor(Color.WHITE);
        chip.setTypeface(null, Typeface.BOLD);
      }
      else
      {
        chip.setBackground(createPillBackground(_colorKey, dp(12), adjustAlpha(_colorLabel, 0.18f)));
        chip.setTextColor(_colorLabel);
      }

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, dp(26));
      lp.setMargins(0, 0, dp(4), 0);
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
      triggerExecution();
    }
  }

  private void updatePromptDisplay()
  {
    if (_tvCustomPromptDisplay == null) return;
    if (_currentCustomPrompt.isEmpty())
    {
      _tvCustomPromptDisplay.setText("Tap to type custom instruction for AI...");
      _tvCustomPromptDisplay.setTextColor(adjustAlpha(_colorLabel, 0.55f));
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
    if (_loadingRow != null) _loadingRow.setVisibility(VISIBLE);
    if (_tvLoadingStatus != null) _tvLoadingStatus.setText("Generating...");
    if (_tvResultPreview != null) _tvResultPreview.setText("");

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
            if (_loadingRow != null) _loadingRow.setVisibility(GONE);
            _lastGeneratedResult = resultText;
            _tvResultPreview.setText(resultText);
            if (_scrollResult != null) _scrollResult.scrollTo(0, 0);
          }

          @Override
          public void onError(final String errorMessage)
          {
            if (revision != _requestRevision || _keyboard == null
                || _inputSessionId != _keyboard.getInputSessionId()) return;
            _isGenerating = false;
            if (_loadingRow != null) _loadingRow.setVisibility(GONE);
            _tvResultPreview.setText(errorMessage);
            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
          }
        });
  }

  private void copyResultToClipboard()
  {
    Context context = getContext();
    String res = (_lastGeneratedResult != null && !_lastGeneratedResult.trim().isEmpty())
        ? _lastGeneratedResult
        : (_tvResultPreview != null ? _tvResultPreview.getText().toString() : "");
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
    iv.setColorFilter(getPrimaryIconColor(), PorterDuff.Mode.SRC_IN);
    iv.setPadding(padPx, padPx, padPx, padPx);
    iv.setBackground(createPillBackground(adjustAlpha(_colorKey, 0.7f), sizePx / (2f * _density), adjustAlpha(_colorLabel, 0.22f)));
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
    ll.setPadding(dp(10), 0, dp(10), 0);
    ll.setBackground(createPillBackground(bgColor, heightPx / (2f * _density), strokeColor));

    ImageView iv = new ImageView(context);
    iv.setImageResource(iconResId);
    iv.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN);
    LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp(15), dp(15));
    lpIcon.setMargins(0, 0, dp(6), 0);
    ll.addView(iv, lpIcon);

    TextView tv = new TextView(context);
    tv.setText(label);
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    tv.setTypeface(null, Typeface.BOLD);
    tv.setTextColor(textColor);
    tv.setSingleLine(true);
    ll.addView(tv);

    ll.setOnClickListener(listener);
    return ll;
  }

  private LinearLayout createTopActionButton(
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

    GradientDrawable normalBg = createPillBackground(bgColor, dp(17), strokeColor);
    GradientDrawable pressedBg = createPillBackground(adjustAlpha(bgColor, 0.75f), dp(17), strokeColor);
    StateListDrawable sld = new StateListDrawable();
    sld.addState(new int[]{android.R.attr.state_pressed}, pressedBg);
    sld.addState(new int[]{}, normalBg);
    ll.setBackground(sld);

    ImageView iv = new ImageView(context);
    iv.setImageResource(iconResId);
    iv.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
    LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp(14), dp(14));
    lpIcon.setMargins(0, 0, dp(4), 0);
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
