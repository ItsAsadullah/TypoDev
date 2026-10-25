package typodev.keyboard.clipboard;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import typodev.keyboard.ClipboardHistoryService;
import typodev.keyboard.Config;
import typodev.keyboard.DialogTheme;
import typodev.keyboard.Keyboard2;
import typodev.keyboard.KeyValue;
import typodev.keyboard.R;
import typodev.keyboard.Utils;
import typodev.keyboard.ai.AiSettingsDialog;
import typodev.keyboard.ai.GeminiAiService;
import typodev.keyboard.translate.TranslationEngine;

/**
 * Modern, card-based mobile clipboard suite modeled after GBoard and SwiftKey.
 * Professional styling strictly synced with active keyboard theme:
 * 1. Category Filter Tabs: Clean labels (All, Pinned, Recent, Codes, Links, Contacts, Images).
 * 2. Multi-Paste / Queue Mode: Continuous sequential paste without closing clipboard.
 * 3. Smart Quick Actions & OTP Extractor: Clean action chips (Paste Code, Open Link, WhatsApp, Call, Email).
 * 4. AI Assistant: Professional vector icons and dialogs matched to active keyboard theme.
 * 5. 10-minute Auto-expiry for sensitive OTPs.
 * 6. Backup & Restore (JSON export / import).
 * 7. Image & Screenshot Support (Thumbnail display + Rich Content Insertion).
 */
public class ClipboardPaneView extends LinearLayout
    implements ClipboardHistoryService.OnClipboardHistoryChange
{
  public enum FilterTab
  {
    ALL("All"),
    PINNED("Pinned"),
    RECENT("Recent"),
    IMAGES("Images"),
    SHORTCUTS("Shortcuts"),
    OTP("Codes"),
    LINKS("Links"),
    CONTACTS("Contacts");

    public final String label;
    FilterTab(String label)
    {
      this.label = label;
    }
  }

  private Keyboard2 _keyboard;
  private boolean _readingAiImage;
  private long _aiImageRequest;
  private ClipboardHistoryService _historyService;
  private PinnedClipboardStore _pinnedStore;
  private ClipboardImageStore _imageStore;
  private final Runnable _imageChanged = () -> refreshCards();
  private TextView _btnScreenshots;
  private TextView _btnImage;
  private LinearLayout _imageActions;
  private TextView _tvImageStatus;
  private TextView _tvEmpty;
  private TextView _tvTitle;
  private final LinearLayout[] _gridColumns = new LinearLayout[2];
  private int _cardWidth;

  // Header & Toolbar views
  private LinearLayout _topBar;
  private ImageView _btnBack;
  private LinearLayout _searchContainer;
  private EditText _etSearch;
  private TextView _btnClearSearch;
  private ImageView _btnAddClip;
  private TextView _btnMultiPaste;
  private TextView _btnEditMode;
  private ImageView _btnBackup;

  // Filter Tabs
  private HorizontalScrollView _filterScrollView;
  private LinearLayout _filterContainer;
  private FilterTab _currentFilter = FilterTab.ALL;
  private final List<TextView> _filterTabViews = new ArrayList<>();

  // Multi-Paste Active Banner
  private LinearLayout _multiPasteBanner;
  private TextView _tvMultiPasteInfo;
  private TextView _btnMultiPasteDone;
  private boolean _isMultiPasteMode = false;
  private int _multiPasteCount = 0;

  // Batch action bar (Edit mode)
  private LinearLayout _batchBar;
  private TextView _tvSelectedCount;
  private TextView _btnSelectAll;
  private TextView _btnBatchPin;
  private TextView _btnBatchDelete;
  private TextView _btnBatchDone;

  // Scrollable container
  private ScrollView _scrollView;
  private LinearLayout _contentContainer;

  // Sections
  private LinearLayout _pinnedSection;
  private TextView _tvPinnedHeader;
  private LinearLayout _pinnedCardsContainer;
  private TextView _tvPinnedEmpty;

  // Recent Section
  private LinearLayout _recentSection;
  private TextView _tvRecentHeader;
  private TextView _btnRecentClearAll;
  private LinearLayout _recentCardsContainer;
  private TextView _tvRecentEmpty;

  // State
  private String _searchQuery = "";
  private boolean _isEditMode = false;
  private final Set<String> _selectedClips = new HashSet<>();
  private ClipboardGridOrder _gridOrder;
  private String _draggedClip;
  private final android.os.Handler _gestures = new android.os.Handler(android.os.Looper.getMainLooper());
  private final java.util.Map<String, View> _cards = new java.util.LinkedHashMap<>();
  private final ClipboardPendingDeletion _pendingDeletes = new ClipboardPendingDeletion();
  private final Runnable _commitDeletes = () -> commitDeletes();
  private TextView _undoBar;
  private final java.util.Map<String, Integer> _undoHeights = new java.util.HashMap<>();
  private FrameLayout _clipboardBody;
  private ImageView _dragPreview;
  private LinearLayout _menuPanel;
  private Runnable _activeHold;
  private android.graphics.Bitmap _dragBitmap;
  private float _dragOffsetX, _dragOffsetY;
  private String _hoverKey;
  private boolean _rebuildingCards;


  private int _colorKeyboard = 0xFF151A23;
  private int _colorLabel = 0xFFFFFFFF;
  private int _colorKey = 0xFF2C2C2E;
  private int _colorKeyActivated = 0xFF2AABEE;
  private int _colorLabelActivated = 0;
  private int _colorSubLabel = 0xFF8E99A8;

  public ClipboardPaneView(Context context)
  {
    this(context, null);
  }

  public ClipboardPaneView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    init(context);
  }

  public void setKeyboard(Keyboard2 keyboard)
  {
    _keyboard = keyboard;
  }

  public void open(int height, int bottomSafety)
  {
    if (_historyService != null) _historyService.add_current_clip();
    dismissInlineMenu();
    _imageStore.scanScreenshots();
    resolveThemeColors(getContext());
    setBackgroundColor(_colorKeyboard);

    ViewGroup.LayoutParams lp = getLayoutParams();
    if (lp != null && height > 0)
    {
      lp.height = height;
      setLayoutParams(lp);
    }

    final float density = getResources().getDisplayMetrics().density;
    int minSafeBottom = Math.round(40f * density);
    int safeBottom = Math.max(bottomSafety, minSafeBottom);
    _imageActions.setPadding(Math.round(12f * density), Math.round(4f * density),
        Math.round(12f * density), safeBottom + Math.round(6f * density));
    if (_contentContainer != null)
    {
      _contentContainer.setPadding(
          Math.round(8f * density),
          Math.round(4f * density),
          Math.round(8f * density),
          safeBottom);
    }
    if (_scrollView != null)
    {
      _scrollView.setClipToPadding(false);
    }

    refreshCards();
  }

  public void applyTheme(int colorKeyboard, int colorKey, int colorLabel, int colorKeyActivated, int colorSubLabel)
  {
    _colorKeyboard = colorKeyboard;
    _colorKey = colorKey;
    _colorLabel = colorLabel;
    _colorKeyActivated = colorKeyActivated;
    _colorSubLabel = colorSubLabel;

    setBackgroundColor(_colorKeyboard);

    float density = getResources().getDisplayMetrics().density;
    if (_btnBack != null)
    {
      _btnBack.setColorFilter(_colorLabel);
      _btnBack.setBackground(createToolbarButtonBg(false));
    }
    if (_etSearch != null)
    {
      _etSearch.setTextColor(_colorLabel);
      _etSearch.setHintTextColor(adjustAlpha(_colorLabel, 0.50f));
    }
    if (_searchContainer != null)
    {
      GradientDrawable searchBg = new GradientDrawable();
      searchBg.setColor(blendSurface(0.07f));
      searchBg.setCornerRadius(16f * density);
      searchBg.setStroke(Math.round(1f * density), adjustAlpha(_colorLabel, 0.25f));
      _searchContainer.setBackground(searchBg);
    }
    if (_btnAddClip != null)
    {
      _btnAddClip.setColorFilter(_colorLabel);
      _btnAddClip.setBackground(createToolbarButtonBg(false));
    }
    if (_btnMultiPaste != null)
    {
      updateMultiPasteButtonStyle();
    }
    if (_btnEditMode != null)
    {
      _btnEditMode.setTextColor(_isEditMode ? getContrastingTextColor(getAccentColor()) : _colorLabel);
      _btnEditMode.setBackground(createToolbarButtonBg(_isEditMode));
    }
    if (_btnBackup != null)
    {
      _btnBackup.setColorFilter(_colorLabel);
      _btnBackup.setBackground(createToolbarButtonBg(false));
    }
    if (_tvPinnedHeader != null) _tvPinnedHeader.setTextColor(getAccentColor());
    if (_tvRecentHeader != null) _tvRecentHeader.setTextColor(_colorLabel);
    if (_btnRecentClearAll != null) _btnRecentClearAll.setTextColor(adjustAlpha(_colorLabel, 0.70f));
    if (_tvPinnedEmpty != null) _tvPinnedEmpty.setTextColor(adjustAlpha(_colorLabel, 0.55f));
    if (_tvRecentEmpty != null) _tvRecentEmpty.setTextColor(adjustAlpha(_colorLabel, 0.55f));

    updateFilterTabsStyle();
    updateMultiPasteBanner();
    refreshCards();
  }

  private void resolveThemeColors(Context context)
  {
    try
    {
      TypedValue tv = new TypedValue();
      if (context.getTheme().resolveAttribute(R.attr.colorKeyboard, tv, true))
      {
        _colorKeyboard = tv.data;
      }
      if (context.getTheme().resolveAttribute(R.attr.colorLabel, tv, true))
      {
        _colorLabel = tv.data;
      }
      if (context.getTheme().resolveAttribute(R.attr.colorKey, tv, true))
      {
        _colorKey = tv.data;
      }
      if (context.getTheme().resolveAttribute(R.attr.colorKeyActivated, tv, true))
      {
        _colorKeyActivated = tv.data;
      }
      if (context.getTheme().resolveAttribute(R.attr.colorLabelActivated, tv, true))
      {
        _colorLabelActivated = tv.data;
      }
      if (context.getTheme().resolveAttribute(R.attr.colorSubLabel, tv, true))
      {
        _colorSubLabel = tv.data;
      }
    }
    catch (Throwable ignored) {}
  }

  private void init(final Context context)
  {
    setOrientation(VERTICAL);
    resolveThemeColors(context);

    _historyService = ClipboardHistoryService.get_service(context);
    if (_historyService != null)
    {
      _historyService.set_on_clipboard_history_change(this);
    }
    _pinnedStore = PinnedClipboardStore.instance(context);
    _imageStore = ClipboardImageStore.get(context);
    try {
      _gridOrder = new ClipboardGridOrder(context.getSharedPreferences("clipboard_grid", Context.MODE_PRIVATE).getString("order", "[]"));
    } catch (RuntimeException ignored) { _gridOrder = new ClipboardGridOrder("[]"); }

    final float density = getResources().getDisplayMetrics().density;

    // =========================================================================
    // 1. Top Header & Search Toolbar
    // =========================================================================
    _topBar = new LinearLayout(context);
    _topBar.setOrientation(HORIZONTAL);
    _topBar.setGravity(Gravity.CENTER_VERTICAL);
    _topBar.setPadding(Math.round(8f * density), Math.round(5f * density), Math.round(8f * density), Math.round(5f * density));

    // 1.1 Back / Switch Back button
    _btnBack = new ImageView(context);
    _btnBack.setImageResource(R.drawable.ic_ai_back);
    _btnBack.setColorFilter(_colorLabel);
    _btnBack.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    _btnBack.setPadding(Math.round(9f * density), Math.round(9f * density), Math.round(9f * density), Math.round(9f * density));
    _btnBack.setBackground(createToolbarButtonBg(false));
    _btnBack.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_menuPanel != null && _menuPanel.getVisibility() == VISIBLE) dismissInlineMenu();
        else if (_keyboard != null) _keyboard.handle_event_key(KeyValue.Event.SWITCH_BACK_CLIPBOARD);
      }
    });
    _topBar.addView(_btnBack);

    // 1.2 Search box container
    _searchContainer = new LinearLayout(context);
    _searchContainer.setOrientation(HORIZONTAL);
    _searchContainer.setGravity(Gravity.CENTER_VERTICAL);
    GradientDrawable searchBg = new GradientDrawable();
    searchBg.setColor(blendSurface(0.07f));
    searchBg.setCornerRadius(16f * density);
    searchBg.setStroke(Math.round(1f * density), adjustAlpha(_colorLabel, 0.25f));
    _searchContainer.setBackground(searchBg);
    _searchContainer.setPadding(Math.round(10f * density), 0, Math.round(8f * density), 0);
    LinearLayout.LayoutParams lpSearch = new LinearLayout.LayoutParams(0, Math.round(34f * density), 1.0f);
    lpSearch.setMargins(Math.round(6f * density), 0, Math.round(6f * density), 0);

    _etSearch = new EditText(context);
    _etSearch.setHint(R.string.clipboard_search_hint);
    _etSearch.setTextColor(_colorLabel);
    _etSearch.setHintTextColor(adjustAlpha(_colorLabel, 0.50f));
    _etSearch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    _etSearch.setBackground(null);
    _etSearch.setSingleLine(true);
    _etSearch.setPadding(0, 0, 0, 0);
    _etSearch.setFocusable(false);
    _etSearch.setFocusableInTouchMode(false);
    _etSearch.setClickable(true);
    _etSearch.setCursorVisible(false);
    _etSearch.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

    OnClickListener openSearchListener = new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_keyboard != null)
        {
          _keyboard.showClipboardSearchMode(_searchQuery);
        }
      }
    };
    _etSearch.setOnClickListener(openSearchListener);
    _searchContainer.setOnClickListener(openSearchListener);
    _searchContainer.addView(_etSearch);

    _btnClearSearch = new TextView(context);
    _btnClearSearch.setText("✕");
    _btnClearSearch.setTextColor(adjustAlpha(_colorLabel, 0.6f));
    _btnClearSearch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
    _btnClearSearch.setGravity(Gravity.CENTER);
    _btnClearSearch.setPadding(Math.round(4f * density), 0, Math.round(4f * density), 0);
    _btnClearSearch.setVisibility(GONE);
    _btnClearSearch.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        _etSearch.setText("");
      }
    });
    _searchContainer.addView(_btnClearSearch);
    _topBar.addView(_searchContainer, lpSearch);

    _etSearch.addTextChangedListener(new TextWatcher()
    {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
      @Override
      public void afterTextChanged(Editable s)
      {
        _searchQuery = s != null ? s.toString().trim() : "";
        _btnClearSearch.setVisibility(_searchQuery.isEmpty() ? GONE : VISIBLE);
        refreshCards();
      }
    });

    // 1.3 Add Custom Clip button (Professional Vector Icon)
    _btnAddClip = new ImageView(context);
    _btnAddClip.setImageResource(R.drawable.ic_clip_add);
    _btnAddClip.setColorFilter(_colorLabel);
    int iconSize = Math.round(32f * density);
    int pad = Math.round(6f * density);
    _btnAddClip.setPadding(pad, pad, pad, pad);
    _btnAddClip.setBackground(createToolbarButtonBg(false));
    LinearLayout.LayoutParams lpAdd = new LinearLayout.LayoutParams(iconSize, iconSize);
    _btnAddClip.setLayoutParams(lpAdd);
    _btnAddClip.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_keyboard != null)
        {
          _keyboard.showAddClipMode();
        }
      }
    });
    _topBar.addView(_btnAddClip);

    // 1.4 Multi-Paste Toggle Button
    _btnMultiPaste = new TextView(context);
    _btnMultiPaste.setText("Multi");
    _btnMultiPaste.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
    _btnMultiPaste.setTypeface(Typeface.DEFAULT_BOLD);
    _btnMultiPaste.setGravity(Gravity.CENTER);
    _btnMultiPaste.setPadding(Math.round(8f * density), Math.round(6f * density), Math.round(8f * density), Math.round(6f * density));
    LinearLayout.LayoutParams lpMulti = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, iconSize);
    lpMulti.leftMargin = Math.round(4f * density);
    _btnMultiPaste.setLayoutParams(lpMulti);
    updateMultiPasteButtonStyle();
    _btnMultiPaste.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        toggleMultiPasteMode();
      }
    });
    _topBar.addView(_btnMultiPaste);

    // 1.5 Edit / Multi-select mode button
    _btnEditMode = new TextView(context);
    _btnEditMode.setText("Edit");
    _btnEditMode.setTextColor(_colorLabel);
    _btnEditMode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
    _btnEditMode.setTypeface(Typeface.DEFAULT_BOLD);
    _btnEditMode.setGravity(Gravity.CENTER);
    _btnEditMode.setPadding(Math.round(8f * density), Math.round(6f * density), Math.round(8f * density), Math.round(6f * density));
    _btnEditMode.setBackground(createToolbarButtonBg(false));
    LinearLayout.LayoutParams lpEdit = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, iconSize);
    lpEdit.leftMargin = Math.round(4f * density);
    _btnEditMode.setLayoutParams(lpEdit);
    _btnEditMode.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        setEditMode(!_isEditMode);
      }
    });
    _topBar.addView(_btnEditMode);

    // 1.6 Backup / Restore button (Professional Vector Icon)
    _btnBackup = new ImageView(context);
    _btnBackup.setImageResource(R.drawable.ic_clip_backup);
    _btnBackup.setColorFilter(_colorLabel);
    _btnBackup.setPadding(pad, pad, pad, pad);
    _btnBackup.setBackground(createToolbarButtonBg(false));
    LinearLayout.LayoutParams lpBackup = new LinearLayout.LayoutParams(iconSize, iconSize);
    lpBackup.leftMargin = Math.round(4f * density);
    _btnBackup.setLayoutParams(lpBackup);
    _btnBackup.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        showBackupRestoreDialog();
      }
    });
    _topBar.addView(_btnBackup);

    // A compact header keeps search and secondary actions out of the title row.
    _topBar.removeAllViews();
    _btnBack.setContentDescription("Back to keyboard");
    _topBar.addView(_btnBack, new LinearLayout.LayoutParams(Math.round(34f * density), Math.round(34f * density)));
    _tvTitle = new TextView(context); _tvTitle.setText(R.string.clipboard_title);
    _tvTitle.setTextSize(18); _tvTitle.setTypeface(Typeface.DEFAULT_BOLD); _tvTitle.setTextColor(_colorLabel);
    _tvTitle.setPadding(Math.round(6f * density), 0, 0, 0);
    _topBar.addView(_tvTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    _btnAddClip.setImageResource(R.drawable.ic_search);
    _btnAddClip.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    _btnAddClip.setPadding(Math.round(9f * density), Math.round(9f * density), Math.round(9f * density), Math.round(9f * density));
    _btnAddClip.setContentDescription("Search clipboard");
    _btnAddClip.setOnClickListener(v -> { if (_keyboard != null) _keyboard.showClipboardSearchMode(_searchQuery); });
    LinearLayout.LayoutParams searchButtonParams = new LinearLayout.LayoutParams(Math.round(34f * density), Math.round(34f * density));
    searchButtonParams.rightMargin = Math.round(8f * density);
    _topBar.addView(_btnAddClip, searchButtonParams);
    _btnBackup.setImageResource(R.drawable.ic_clip_more); _btnBackup.setContentDescription("Clipboard options");
    _btnBackup.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    _btnBackup.setPadding(Math.round(9f * density), Math.round(9f * density), Math.round(9f * density), Math.round(9f * density));
    _btnBackup.setOnClickListener(v -> {
      if (_menuPanel != null && _menuPanel.getVisibility() == VISIBLE) dismissInlineMenu();
      else showClipboardMenu();
    });
    _topBar.addView(_btnBackup, new LinearLayout.LayoutParams(Math.round(34f * density), Math.round(34f * density)));
    addView(_topBar);
    LinearLayout searchRow = new LinearLayout(context);
    searchRow.setPadding(Math.round(12f * density), 0, Math.round(12f * density), Math.round(6f * density));
    searchRow.addView(_searchContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(36f * density)));
    searchRow.setVisibility(GONE);
    addView(searchRow);
    _imageActions = new LinearLayout(context);
    _imageActions.setOrientation(HORIZONTAL);
    _imageActions.setPadding(Math.round(12f * density), Math.round(4f * density), Math.round(12f * density), Math.round(6f * density));
    _btnImage = createBatchButton(getResources().getString(R.string.clipboard_add_image), v -> launchImagePicker(false));
    _btnScreenshots = createBatchButton(getResources().getString(R.string.clipboard_capture_now), v -> captureScreenshot());
    _imageActions.addView(_btnScreenshots, new LinearLayout.LayoutParams(0, Math.round(40f * density), 1f));
    LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(0, Math.round(40f * density), 1f);
    imageParams.leftMargin = Math.round(8f * density);
    _imageActions.addView(_btnImage, imageParams);

    // =========================================================================
    // 2. Category Filter Tabs Bar (Clean Typography, No Emojis)
    // =========================================================================
    _filterScrollView = new HorizontalScrollView(context);
    _filterScrollView.setHorizontalScrollBarEnabled(false);
    _filterScrollView.setOverScrollMode(OVER_SCROLL_NEVER);
    LinearLayout.LayoutParams lpFilterScroll = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpFilterScroll.bottomMargin = Math.round(3f * density);

    _filterContainer = new LinearLayout(context);
    _filterContainer.setOrientation(HORIZONTAL);
    _filterContainer.setPadding(Math.round(8f * density), Math.round(2f * density), Math.round(8f * density), Math.round(4f * density));
    _filterScrollView.addView(_filterContainer);

    initFilterTabs(context, density);
    _filterScrollView.setVisibility(GONE);
    addView(_filterScrollView, lpFilterScroll);
    _tvImageStatus = new TextView(context); _tvImageStatus.setTextSize(12);
    _tvImageStatus.setSingleLine(true); _tvImageStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);
    _tvImageStatus.setPadding(Math.round(16f * density), 0, Math.round(12f * density), Math.round(4f * density));
    _tvImageStatus.setOnClickListener(v -> toggleScreenshotSaving());
    addView(_tvImageStatus);

    // =========================================================================
    // 3. Multi-Paste Active Banner
    // =========================================================================
    _multiPasteBanner = new LinearLayout(context);
    _multiPasteBanner.setOrientation(HORIZONTAL);
    _multiPasteBanner.setGravity(Gravity.CENTER_VERTICAL);
    _multiPasteBanner.setVisibility(GONE);
    _multiPasteBanner.setPadding(Math.round(12f * density), Math.round(6f * density), Math.round(10f * density), Math.round(6f * density));
    LinearLayout.LayoutParams lpBanner = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpBanner.setMargins(Math.round(8f * density), 0, Math.round(8f * density), Math.round(4f * density));
    _multiPasteBanner.setLayoutParams(lpBanner);

    _tvMultiPasteInfo = new TextView(context);
    _tvMultiPasteInfo.setText("Multi-Paste Active • Pasted: 0");
    _tvMultiPasteInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
    _tvMultiPasteInfo.setTypeface(Typeface.DEFAULT_BOLD);
    _tvMultiPasteInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
    _multiPasteBanner.addView(_tvMultiPasteInfo);

    _btnMultiPasteDone = new TextView(context);
    _btnMultiPasteDone.setText("Done");
    _btnMultiPasteDone.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
    _btnMultiPasteDone.setTypeface(Typeface.DEFAULT_BOLD);
    _btnMultiPasteDone.setPadding(Math.round(10f * density), Math.round(4f * density), Math.round(10f * density), Math.round(4f * density));
    _btnMultiPasteDone.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        toggleMultiPasteMode();
        if (_keyboard != null)
        {
          _keyboard.handle_event_key(KeyValue.Event.SWITCH_BACK_CLIPBOARD);
        }
      }
    });
    _multiPasteBanner.addView(_btnMultiPasteDone);

    addView(_multiPasteBanner);

    // =========================================================================
    // 4. Multi-Select Batch Action Bar
    // =========================================================================
    _batchBar = new LinearLayout(context);
    _batchBar.setOrientation(HORIZONTAL);
    _batchBar.setGravity(Gravity.CENTER_VERTICAL);
    _batchBar.setBackgroundColor(adjustAlpha(_colorKey, 0.85f));
    _batchBar.setPadding(Math.round(12f * density), Math.round(6f * density), Math.round(12f * density), Math.round(6f * density));
    _batchBar.setVisibility(GONE);

    _tvSelectedCount = new TextView(context);
    _tvSelectedCount.setText("0 selected");
    _tvSelectedCount.setTextColor(_colorLabel);
    _tvSelectedCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    _tvSelectedCount.setTypeface(Typeface.DEFAULT_BOLD);
    _tvSelectedCount.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
    _batchBar.addView(_tvSelectedCount);

    _btnSelectAll = createBatchButton("Select All", new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        selectAllClips();
      }
    });
    _batchBar.addView(_btnSelectAll);

    _btnBatchPin = createBatchButton("Pin", new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        executeBatchPin();
      }
    });
    _batchBar.addView(_btnBatchPin);

    _btnBatchDelete = createBatchButton("Delete", new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        executeBatchDelete();
      }
    });
    _batchBar.addView(_btnBatchDelete);

    _btnBatchDone = createBatchButton("Done", new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        setEditMode(false);
      }
    });
    _batchBar.addView(_btnBatchDone);

    addView(_batchBar);
    _undoBar = new TextView(context);
    _undoBar.setGravity(Gravity.CENTER); _undoBar.setTextSize(13);
    _undoBar.setPadding(8, Math.round(8f * density), 8, Math.round(8f * density));
    _undoBar.setVisibility(GONE);
    _undoBar.setOnClickListener(v -> {
      _gestures.removeCallbacks(_commitDeletes); _pendingDeletes.undo();
      _undoBar.setVisibility(GONE); refreshCards();
    });
    addView(_undoBar);
    for (TextView button : new TextView[]{_btnSelectAll, _btnBatchPin, _btnBatchDelete, _btnBatchDone}) {
      LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, Math.round(30f * density));
      buttonParams.leftMargin = Math.round(6f * density); button.setLayoutParams(buttonParams);
      button.setTextSize(11); button.setSingleLine(true);
      button.setPadding(Math.round(8f * density), 0, Math.round(8f * density), 0);
    }
    _tvSelectedCount.setTextSize(11);

    // =========================================================================
    // 5. ScrollView with Pinned & Recent Sections
    // =========================================================================
    _scrollView = new ScrollView(context);
    _scrollView.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
    _scrollView.setOverScrollMode(OVER_SCROLL_NEVER);
    _contentContainer = new LinearLayout(context);
    _contentContainer.setOrientation(VERTICAL);
    _contentContainer.setPadding(Math.round(8f * density), Math.round(4f * density), Math.round(8f * density), Math.round(16f * density));

    // 5.1 Pinned Section
    _pinnedSection = new LinearLayout(context);
    _pinnedSection.setOrientation(VERTICAL);

    _tvPinnedHeader = new TextView(context);
    _tvPinnedHeader.setText("PINNED (0)");
    _tvPinnedHeader.setTextColor(adjustAlpha(_colorLabel, 0.7f));
    _tvPinnedHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
    _tvPinnedHeader.setTypeface(Typeface.DEFAULT_BOLD);
    _tvPinnedHeader.setPadding(Math.round(4f * density), Math.round(6f * density), Math.round(4f * density), Math.round(4f * density));
    _pinnedSection.addView(_tvPinnedHeader);

    _tvPinnedEmpty = new TextView(context);
    _tvPinnedEmpty.setText("No pinned clips. Tap Pin on any clip to keep it saved.");
    _tvPinnedEmpty.setTextColor(adjustAlpha(_colorLabel, 0.45f));
    _tvPinnedEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
    _tvPinnedEmpty.setPadding(Math.round(8f * density), Math.round(8f * density), Math.round(8f * density), Math.round(8f * density));
    _tvPinnedEmpty.setVisibility(GONE);
    _pinnedSection.addView(_tvPinnedEmpty);

    _pinnedCardsContainer = new LinearLayout(context);
    _pinnedCardsContainer.setOrientation(VERTICAL);
    _pinnedSection.addView(_pinnedCardsContainer);

    _contentContainer.addView(_pinnedSection);

    // 5.2 Recent Section
    _recentSection = new LinearLayout(context);
    _recentSection.setOrientation(VERTICAL);
    LinearLayout.LayoutParams lpRecentSec = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpRecentSec.topMargin = Math.round(10f * density);

    LinearLayout recentHeaderRow = new LinearLayout(context);
    recentHeaderRow.setOrientation(HORIZONTAL);
    recentHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

    _tvRecentHeader = new TextView(context);
    _tvRecentHeader.setText("RECENT (0)");
    _tvRecentHeader.setTextColor(adjustAlpha(_colorLabel, 0.7f));
    _tvRecentHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
    _tvRecentHeader.setTypeface(Typeface.DEFAULT_BOLD);
    _tvRecentHeader.setPadding(Math.round(4f * density), Math.round(6f * density), Math.round(4f * density), Math.round(4f * density));
    _tvRecentHeader.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
    recentHeaderRow.addView(_tvRecentHeader);

    _btnRecentClearAll = new TextView(context);
    _btnRecentClearAll.setText("Clear all");
    _btnRecentClearAll.setTextColor(adjustAlpha(_colorLabel, 0.65f));
    _btnRecentClearAll.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
    _btnRecentClearAll.setPadding(Math.round(8f * density), Math.round(4f * density), Math.round(8f * density), Math.round(4f * density));
    _btnRecentClearAll.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        confirmClearAllRecent();
      }
    });
    recentHeaderRow.addView(_btnRecentClearAll);

    _recentSection.addView(recentHeaderRow);

    _tvRecentEmpty = new TextView(context);
    _tvRecentEmpty.setText("Clipboard history is empty. Copy any text or image to see it here.");
    _tvRecentEmpty.setTextColor(adjustAlpha(_colorLabel, 0.45f));
    _tvRecentEmpty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
    _tvRecentEmpty.setPadding(Math.round(8f * density), Math.round(8f * density), Math.round(8f * density), Math.round(8f * density));
    _recentSection.addView(_tvRecentEmpty);

    _recentCardsContainer = new LinearLayout(context);
    _recentCardsContainer.setOrientation(VERTICAL);
    _recentSection.addView(_recentCardsContainer);

    _contentContainer.addView(_recentSection, lpRecentSec);
    LinearLayout grid = new LinearLayout(context);
    grid.setOrientation(HORIZONTAL); grid.setClipChildren(false);
    _contentContainer.setClipChildren(false);
    for (int column = 0; column < 2; column++) {
      _gridColumns[column] = new LinearLayout(context);
      _gridColumns[column].setOrientation(VERTICAL); _gridColumns[column].setClipChildren(false);
      LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
      if (column == 1) columnParams.leftMargin = Math.round(8f * density);
      grid.addView(_gridColumns[column], columnParams);
    }
    _contentContainer.addView(grid);


    _tvEmpty = new TextView(context); _tvEmpty.setGravity(Gravity.CENTER);
    _tvEmpty.setTextSize(14); _tvEmpty.setLineSpacing(5f * density, 1f);
    _tvEmpty.setPadding(Math.round(24f * density), Math.round(30f * density), Math.round(24f * density), Math.round(24f * density));
    _contentContainer.addView(_tvEmpty);
    _scrollView.addView(_contentContainer);
    _clipboardBody = new FrameLayout(context);
    _clipboardBody.addView(_scrollView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    _dragPreview = new ImageView(context); _dragPreview.setScaleType(ImageView.ScaleType.FIT_XY);
    _dragPreview.setVisibility(GONE); _dragPreview.setAlpha(0.88f);
    _clipboardBody.addView(_dragPreview);
    _menuPanel = new LinearLayout(context); _menuPanel.setOrientation(VERTICAL); _menuPanel.setClickable(true); _menuPanel.setVisibility(GONE);
    _clipboardBody.addView(_menuPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    addView(_clipboardBody, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    addView(_imageActions);

    updateFilterTabsStyle();
    refreshCards();
  }

  private void initFilterTabs(final Context context, final float density)
  {
    _filterContainer.removeAllViews();
    _filterTabViews.clear();

    for (final FilterTab tab : FilterTab.values())
    {
      final TextView tvTab = new TextView(context);
      tvTab.setText(tab.label);
      tvTab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
      tvTab.setGravity(Gravity.CENTER);
      tvTab.setPadding(Math.round(14f * density), Math.round(9f * density), Math.round(14f * density), Math.round(9f * density));
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      lp.rightMargin = Math.round(6f * density);
      tvTab.setLayoutParams(lp);

      tvTab.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          _currentFilter = tab;
          if (_isEditMode) _selectedClips.clear();
          updateFilterTabsStyle();
          refreshCards();
        }
      });

      _filterTabViews.add(tvTab);
      _filterContainer.addView(tvTab);
    }
  }

  private void updateFilterTabsStyle()
  {
    float density = getResources().getDisplayMetrics().density;
    int solidAccent = getAccentColor();
    for (int i = 0; i < FilterTab.values().length; i++)
    {
      FilterTab tab = FilterTab.values()[i];
      if (i >= _filterTabViews.size()) break;
      TextView tvTab = _filterTabViews.get(i);
      boolean isSelected = (_currentFilter == tab);

      GradientDrawable bg = new GradientDrawable();
      bg.setCornerRadius(9f * density);
      if (isSelected)
      {
        bg.setColor(solidAccent);
        tvTab.setTextColor(getContrastingTextColor(solidAccent));
        tvTab.setTypeface(Typeface.DEFAULT_BOLD);
      }
      else
      {
        bg.setColor(Color.TRANSPARENT);

        tvTab.setTextColor(adjustAlpha(_colorLabel, 0.85f));
        tvTab.setTypeface(Typeface.DEFAULT);
      }
      tvTab.setBackground(bg);
    }
  }

  private void toggleMultiPasteMode()
  {
    _isMultiPasteMode = !_isMultiPasteMode;
    if (!_isMultiPasteMode)
    {
      _multiPasteCount = 0;
    }
    updateMultiPasteButtonStyle();
    updateMultiPasteBanner();
  }

  private void updateMultiPasteButtonStyle()
  {
    if (_btnMultiPaste == null) return;
    int accent = getAccentColor();
    if (_isMultiPasteMode)
    {
      _btnMultiPaste.setBackground(createToolbarButtonBg(true));
      _btnMultiPaste.setTextColor(getContrastingTextColor(accent));
    }
    else
    {
      _btnMultiPaste.setBackground(createToolbarButtonBg(false));
      _btnMultiPaste.setTextColor(_colorLabel);
    }
  }

  private void updateMultiPasteBanner()
  {
    if (_multiPasteBanner == null) return;
    float density = getResources().getDisplayMetrics().density;
    if (_isMultiPasteMode)
    {
      int accent = getAccentColor();
      _multiPasteBanner.setVisibility(VISIBLE);
      GradientDrawable bg = new GradientDrawable();
      bg.setColor(adjustAlpha(accent, 0.15f));
      bg.setStroke(Math.round(1f * density), accent);
      bg.setCornerRadius(8f * density);
      _multiPasteBanner.setBackground(bg);

      _tvMultiPasteInfo.setTextColor(_colorLabel);
      _tvMultiPasteInfo.setText("Multi-Paste Active • Pasted: " + _multiPasteCount);

      GradientDrawable doneBg = new GradientDrawable();
      doneBg.setColor(accent);
      doneBg.setCornerRadius(6f * density);
      _btnMultiPasteDone.setBackground(doneBg);
      _btnMultiPasteDone.setTextColor(getContrastingTextColor(accent));
    }
    else
    {
      _multiPasteBanner.setVisibility(GONE);
    }
  }

  private TextView createBatchButton(String text, OnClickListener listener)
  {
    final float density = getResources().getDisplayMetrics().density;
    TextView btn = new TextView(getContext());
    btn.setText(text);
    btn.setTextColor(_colorLabel);
    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
    btn.setTypeface(Typeface.DEFAULT_BOLD);
    btn.setGravity(Gravity.CENTER);
    btn.setPadding(Math.round(8f * density), Math.round(5f * density), Math.round(8f * density), Math.round(5f * density));
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(blendSurface(0.16f));
    bg.setStroke(Math.round(1f * density), adjustAlpha(_colorLabel, 0.25f));
    bg.setCornerRadius(6f * density);
    btn.setBackground(bg);
    btn.setOnClickListener(listener);
    return btn;
  }

  private void launchImagePicker(boolean screenshots) {
    Intent intent = new Intent(getContext(), ClipboardImageActivity.class);
    intent.putExtra("screenshots", screenshots);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    getContext().startActivity(intent);
  }

  private void editShortcut(ClipboardItem item) {
    Intent intent = new Intent(getContext(), ClipboardShortcutActivity.class);
    intent.putExtra("clip_text", item.content);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    getContext().startActivity(intent);
  }

  private void toggleScreenshotSaving() {
    if (_imageStore.screenshotsEnabled() && _imageStore.hasPhotoAccess()) {
      _imageStore.enableScreenshots(false); refreshCards();
    } else launchImagePicker(true);
  }

  private void showClipboardMenu() {
    String auto = getResources().getString(_imageStore.screenshotsEnabled()
        ? (_imageStore.hasPhotoAccess() ? R.string.clipboard_auto_on : R.string.clipboard_auto_permission)
        : R.string.clipboard_auto_off);
    String[] options = {"Add text", getResources().getString(R.string.clipboard_add_image),
        getResources().getString(R.string.clipboard_capture_now), auto,
        _isMultiPasteMode ? "Finish multi-paste" : "Multi-paste", "Select clips", "Backup / restore", "Clear recent",
        getResources().getString(R.string.clipboard_recent_screenshot)};
    showInlineMenu(getResources().getString(R.string.clipboard_title), options, which -> {
          switch (which) {
            case 0: if (_keyboard != null) _keyboard.showAddClipMode(); break;
            case 1: launchImagePicker(false); break;
            case 2: captureScreenshot(); break;
            case 3: toggleScreenshotSaving(); break;
            case 4: toggleMultiPasteMode(); break;
            case 5: setEditMode(true); break;
            case 6: showBackupRestoreDialog(); break;
            case 7: confirmClearAllRecent(); break;
            case 8:
              Intent recent = new Intent(getContext(), ClipboardImageActivity.class);
              recent.putExtra("recent", true); recent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              getContext().startActivity(recent); break;
          }
    });
  }

  private void captureScreenshot() {
    if (_keyboard != null) _keyboard.requestHideSelf(0);
    typodev.keyboard.ai.AiScreenCaptureActivity.startScreenScan(getContext(), "clipboard",
        new typodev.keyboard.ai.AiScreenCaptureActivity.AiVisionCallback() {
          @Override public void onImageCaptured(byte[] bytes, String tone) {
            _imageStore.importCapture(bytes, success -> {
              Toast.makeText(getContext(), success ? R.string.clipboard_image_saved : R.string.clipboard_image_failed,
                  Toast.LENGTH_LONG).show();
              reopenImages();
            });
          }
          @Override public void onError(String error) {
            Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show(); reopenImages();
          }
        });
  }

  private void reopenImages() {
    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
      _currentFilter = FilterTab.ALL;
      if (_keyboard != null) {
        try { _keyboard.showWindow(true); _keyboard.showClipboardImages(); } catch (RuntimeException ignored) { }
      }
      refreshCards();
    });
  }

  public void showImages() {
    _currentFilter = FilterTab.ALL;
    refreshCards();
  }

  public void refreshCards()
  {
    if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) { post(() -> refreshCards()); return; }
    if (_rebuildingCards || _draggedClip != null || _gridColumns[0] == null) return;
    _rebuildingCards = true;
    try {
    cancelHold();
    final Context context = getContext();
    final float density = getResources().getDisplayMetrics().density;


    _imageActions.setVisibility(GONE);
    _tvImageStatus.setVisibility(GONE);
    _tvImageStatus.setText(_imageStore.screenshotsEnabled()
        ? (_imageStore.hasPhotoAccess() ? R.string.clipboard_auto_on : R.string.clipboard_auto_permission)
        : R.string.clipboard_auto_off);
    _tvImageStatus.setTextColor(adjustAlpha(_colorLabel, 0.65f));
    _tvTitle.setTextColor(_colorLabel); _tvEmpty.setTextColor(adjustAlpha(_colorLabel, 0.65f));
    for (TextView button : new TextView[]{_btnImage, _btnScreenshots}) {
      button.setTextColor(_colorLabel);
      button.setBackground(createToolbarButtonBg(false));
    }
    _pinnedCardsContainer.removeAllViews();
    _recentCardsContainer.removeAllViews();

    List<String> rawPinned = _pinnedStore.getPinnedClips();
    List<ClipboardItem> rawRecentItems = (_historyService != null)
        ? _historyService.clear_expired_and_get_history_items() : new ArrayList<ClipboardItem>();

    List<ClipboardItem> pinnedItems = new ArrayList<>();
    for (ClipboardItem image : _imageStore.items()) {
      if (image.isPinned) {
        if (matchesFilterAndSearch(image)) pinnedItems.add(image);
      } else rawRecentItems.add(image);
    }
    for (String s : rawPinned)
    {
      ClipboardItem item = new ClipboardItem(s, true);
      if (matchesFilterAndSearch(item))
      {
        pinnedItems.add(item);
      }
    }

    List<ClipboardItem> recentItems = new ArrayList<>();
    for (ClipboardItem item : rawRecentItems)
    {
      if (!_pinnedStore.isPinned(item.content) && matchesFilterAndSearch(item))
      {
        recentItems.add(item);
      }
    }

    java.util.Collections.sort(recentItems, (x, y) -> Long.compare(y.timestamp, x.timestamp));

    _pinnedSection.setVisibility(GONE);
    _recentSection.setVisibility(GONE);
    for (LinearLayout column : _gridColumns) column.removeAllViews();
    _cards.clear();
    List<ClipboardItem> items = new ArrayList<>(pinnedItems);
    items.addAll(recentItems);
    List<String> available = new ArrayList<>();
    for (ClipboardItem item : items) available.add(item.historyKey());
    List<String> order = _gridOrder.reconcile(available);
    java.util.Collections.sort(items, (a, b) -> Integer.compare(order.indexOf(a.historyKey()), order.indexOf(b.historyKey())));
    saveGridOrder();
    _tvEmpty.setVisibility(items.isEmpty() ? VISIBLE : GONE);
    _tvEmpty.setText(_searchQuery.isEmpty() ? "No clips yet.\nCopy text or add an image from the settings menu." : getResources().getString(R.string.clipboard_empty_search));
    int width = getWidth() > 0 ? getWidth() : getResources().getDisplayMetrics().widthPixels;
    _cardWidth = Math.max(1, (width - _contentContainer.getPaddingLeft() - _contentContainer.getPaddingRight() - Math.round(8f * density)) / 2);
    int[] columnHeights = {0, 0};
    for (ClipboardItem item : items) {
      View card = _pendingDeletes.contains(item.historyKey()) ? createUndoCard(item, density) : createCardView(item, density);
      _cards.put(item.historyKey(), card);
      card.measure(View.MeasureSpec.makeMeasureSpec(_cardWidth, View.MeasureSpec.EXACTLY),
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
      int column = columnHeights[0] <= columnHeights[1] ? 0 : 1;
      _gridColumns[column].addView(card);
      columnHeights[column] += card.getMeasuredHeight() + Math.round(8f * density);
    }

    updateSelectedCountText();
    } finally { _rebuildingCards = false; }
  }

  private boolean matchesFilterAndSearch(ClipboardItem item)
  {
    if (item == null) return false;

    // 1. Search Query
    if (_searchQuery != null && !_searchQuery.isEmpty())
    {
      if ((item.content == null || !item.content.toLowerCase(Locale.ROOT).contains(_searchQuery.toLowerCase(Locale.ROOT)))
          && !_pinnedStore.getShortcut(item.content).contains(_searchQuery.toLowerCase(Locale.ROOT)))
      {
        return false;
      }
    }

    return true;
  }

  private View createUndoCard(ClipboardItem item, float density) {
    TextView undo = new TextView(getContext());
    undo.setText(R.string.clipboard_undo_card); undo.setTextSize(14); undo.setGravity(Gravity.CENTER);
    undo.setTextColor(getContrastingTextColor(getAccentColor()));
    GradientDrawable fill = new GradientDrawable(); fill.setColor(getAccentColor()); fill.setCornerRadius(10 * density);
    undo.setBackground(fill);
    Integer savedHeight = _undoHeights.get(item.historyKey());
    int height = savedHeight == null ? Math.round(86 * density) : savedHeight;
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
    params.bottomMargin = Math.round(8 * density); undo.setLayoutParams(params);
    undo.setOnClickListener(v -> {
      _pendingDeletes.undo(item.historyKey()); _undoHeights.remove(item.historyKey());
      if (_pendingDeletes.size() == 0) _gestures.removeCallbacks(_commitDeletes);
      refreshCards();
    });
    return undo;
  }
  private void rememberUndoHeight(String key) {
    View card = _cards.get(key);
    if (card != null) _undoHeights.put(key, Math.max(card.getHeight(), card.getMeasuredHeight()));
  }

  private View createCardView(final ClipboardItem item, final float density)
  {
    final Context context = getContext();
    FrameLayout card = new FrameLayout(context);
    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    cardParams.bottomMargin = Math.round(8f * density); card.setLayoutParams(cardParams);
    card.setMinimumHeight(Math.round((item.isPinned ? 86f : 64f) * density));
    GradientDrawable background = new GradientDrawable();
    background.setColor(blendSurface(0.07f)); background.setCornerRadius(10f * density);
    if (_selectedClips.contains(item.historyKey())) background.setStroke(Math.round(2f * density), getAccentColor());
    card.setBackground(background); card.setClipToOutline(true);
    LinearLayout body = new LinearLayout(context); body.setOrientation(VERTICAL);
    body.setPadding(Math.round(12f * density), Math.round(12f * density),
        Math.round((item.isImage ? 12f : 44f) * density), Math.round(12f * density));
    if (item.isImage && item.imageUri != null) {
      body.setPadding(0, 0, 0, 0);
      Uri preview = _imageStore.thumbnail(item.imageUri);
      ImageView image = new ImageView(context);
      if (preview != null) image.setImageURI(preview);
      image.setScaleType(ImageView.ScaleType.FIT_XY);
      int height = Math.round(160f * density);
      if (preview != null) {
        android.graphics.BitmapFactory.Options bounds = new android.graphics.BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeFile(preview.getPath(), bounds);
        if (bounds.outWidth > 0 && bounds.outHeight > 0)
          height = Math.max(1, Math.round((float)_cardWidth * bounds.outHeight / bounds.outWidth));
      }
      body.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
    } else {
      TextView text = new TextView(context); text.setText(item.content); text.setTextColor(_colorLabel);
      text.setTextSize(14); text.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
      text.setMaxLines(6); text.setEllipsize(android.text.TextUtils.TruncateAt.END);
      text.setLineSpacing(2f * density, 1f); body.addView(text);
      String shortcut = _pinnedStore.getShortcut(item.content);
      if (!shortcut.isEmpty()) {
        TextView hint = new TextView(context); hint.setText("⌘ " + shortcut);
        hint.setTextSize(11); hint.setTextColor(getAccentColor()); hint.setSingleLine(true);
        hint.setEllipsize(android.text.TextUtils.TruncateAt.END); hint.setPadding(0, Math.round(6f * density), 0, 0);
        body.addView(hint);
      }
    }
    card.addView(body, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    LinearLayout actions = new LinearLayout(context); actions.setOrientation(VERTICAL);
    actions.setPadding(0, Math.round(3f * density), Math.round(3f * density), 0);
    if (_isEditMode) {
      CheckBox check = new CheckBox(context); check.setChecked(_selectedClips.contains(item.historyKey()));
      check.setButtonTintList(android.content.res.ColorStateList.valueOf(getAccentColor()));
      check.setFocusable(false);
      check.setOnClickListener(v -> toggleSelection(item.historyKey()));
      actions.addView(check, new LinearLayout.LayoutParams(Math.round(32f * density), Math.round(32f * density)));
    } else {
      ImageView more = gridAction(R.drawable.ic_clip_more, "Clip actions", density);
      more.setOnClickListener(v -> showCardContextMenu(item, v)); actions.addView(more);
      if (item.isPinned) {
        ImageView pin = gridAction(R.drawable.ic_clip_pin, "Unpin", density); pin.setColorFilter(getAccentColor());
        pin.setOnClickListener(v -> {
          if (item.isImage) _imageStore.pin(item.historyKey(), false);
          else _pinnedStore.unpinClip(item.content);
          refreshCards();
        }); actions.addView(pin);
      }
    }
    card.addView(actions, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT));

    // 1-Tap Paste
    card.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_isEditMode)
        {
          if (_selectedClips.contains(item.historyKey()))
          {
            _selectedClips.remove(item.historyKey());
          }
          else
          {
            _selectedClips.add(item.historyKey());
          }
          refreshCards();
        }
        else
        {
          try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); }
          catch (Throwable ignored) {}

          if (item.isImage && item.imageUri != null)
          {
            if (_keyboard != null)
            {
              _keyboard.pasteImageContent(Uri.parse(item.imageUri), "image/*");
            }
            if (_isMultiPasteMode)
            {
              _multiPasteCount++;
              updateMultiPasteBanner();
              Toast.makeText(context, "Pasted image (" + _multiPasteCount + ")", Toast.LENGTH_SHORT).show();
            }
            else if (_keyboard != null)
            {
              _keyboard.handle_event_key(KeyValue.Event.SWITCH_BACK_CLIPBOARD);
            }
          }
          else
          {
            if (_isMultiPasteMode)
            {
              ClipboardHistoryService.paste(item.content + " ");
              _multiPasteCount++;
              updateMultiPasteBanner();
              Toast.makeText(context, "Pasted clip #" + _multiPasteCount, Toast.LENGTH_SHORT).show();
            }
            else
            {
              ClipboardHistoryService.paste(item.content);
              if (_keyboard != null)
              {
                _keyboard.handle_event_key(KeyValue.Event.SWITCH_BACK_CLIPBOARD);
              }
            }
          }
        }
      }
    });

    installCardGesture(card, item, density);
    return card;
  }

  private interface MenuAction { void run(int which); }
  private void showInlineMenu(String title, String[] options, MenuAction action) {
    showInlineMenu(title, options, null, action);
  }
  private void showInlineMenu(String title, String[] options, ClipboardItem preview, MenuAction action) {
    cancelHold(); finishPreview();
    _menuPanel.removeAllViews(); _menuPanel.setBackgroundColor(_colorKeyboard);
    float density = getResources().getDisplayMetrics().density;
    _tvTitle.setText(title);
    _btnAddClip.setVisibility(GONE);
    _btnBackup.setImageResource(R.drawable.ic_ai_close);
    _btnBackup.setContentDescription("Close menu");
    LinearLayout content = new LinearLayout(getContext()); content.setOrientation(HORIZONTAL);
    _menuPanel.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    int safeBottom = Math.max(_contentContainer.getPaddingBottom(), Math.round(48 * density));
    if (preview != null) {
      FrameLayout previewBox = new FrameLayout(getContext()); previewBox.setBackground(createToolbarButtonBg(false));
      LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0.46f);
      previewParams.setMargins(Math.round(8 * density), Math.round(4 * density), Math.round(4 * density), safeBottom);
      content.addView(previewBox, previewParams);
      if (preview.isImage) {
        ImageView image = new ImageView(getContext()); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setPadding(Math.round(6 * density), Math.round(6 * density), Math.round(6 * density), Math.round(6 * density));
        Uri thumbnail = _imageStore.thumbnail(preview.historyKey());
        if (thumbnail != null) image.setImageURI(thumbnail);
        previewBox.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      } else {
        ScrollView textScroll = new ScrollView(getContext()); TextView text = new TextView(getContext());
        text.setText(preview.content); text.setTextSize(14); text.setTextColor(_colorLabel);
        text.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        text.setPadding(Math.round(10 * density), Math.round(12 * density), Math.round(10 * density), Math.round(12 * density));
        textScroll.addView(text); previewBox.addView(textScroll);
      }
    }
    ScrollView scroll = new ScrollView(getContext());
    scroll.setPadding(0, 0, 0, safeBottom); scroll.setClipToPadding(true); scroll.setFillViewport(false);
    LinearLayout list = new LinearLayout(getContext()); list.setOrientation(VERTICAL);
    for (int i = 0; i < options.length; i++) {
      final int choice = i;
      TextView row = new TextView(getContext()); row.setText(options[i]); row.setTextSize(13); row.setTextColor(_colorLabel);
      row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(Math.round(16 * density), Math.round(10 * density), Math.round(12 * density), Math.round(10 * density));
      row.setMinimumHeight(Math.round(44 * density)); row.setBackground(createToolbarButtonBg(false));
      int icon = options[i].startsWith("Paste") ? R.drawable.ic_clip_paste
          : (options[i].equals(getResources().getString(R.string.clipboard_web_search)) || options[i].equals(getResources().getString(R.string.clipboard_visual_search))) ? R.drawable.ic_search
          : options[i].equals(getResources().getString(R.string.clipboard_ai_image_comments)) ? R.drawable.ic_pref_ai
          : options[i].equals("Delete") ? R.drawable.ic_delete
          : (options[i].startsWith("Pin") || options[i].startsWith("Unpin")) ? R.drawable.ic_clip_pin : 0;
      if (icon != 0) {
        Drawable graphic = getResources().getDrawable(icon, getContext().getTheme()).mutate(); graphic.setTint(_colorLabel);
        graphic.setBounds(0, 0, Math.round(16 * density), Math.round(16 * density));
        row.setCompoundDrawables(graphic, null, null, null); row.setCompoundDrawablePadding(Math.round(8 * density));
      }
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      params.setMargins(Math.round(8 * density), 0, Math.round(8 * density), Math.round(4 * density)); list.addView(row, params);
      row.setOnClickListener(v -> { dismissInlineMenu(); action.run(choice); });
    }
    scroll.addView(list); content.addView(scroll, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, preview == null ? 1f : 0.54f));
    _menuPanel.setVisibility(VISIBLE);
  }
  private void dismissInlineMenu() {
    if (_menuPanel != null) { _menuPanel.setVisibility(GONE); _menuPanel.removeAllViews(); }
    if (_tvTitle != null) _tvTitle.setText(R.string.clipboard_title);
    if (_btnAddClip != null) _btnAddClip.setVisibility(VISIBLE);
    if (_btnBackup != null) {
      _btnBackup.setImageResource(R.drawable.ic_clip_more);
      _btnBackup.setContentDescription("Clipboard options");
    }
  }
  private void cancelHold() {
    if (_activeHold != null) _gestures.removeCallbacks(_activeHold);
    _activeHold = null;
  }

  private void toggleSelection(String key) {
    if (!_selectedClips.remove(key)) _selectedClips.add(key);
    refreshCards();
  }

  private void queueDelete(String key) {
    rememberUndoHeight(key);
    _pendingDeletes.addAll(java.util.Collections.singleton(key), android.os.SystemClock.uptimeMillis()); _selectedClips.remove(key); showUndo(); refreshCards();
  }
  private void showUndo() {
    _undoBar.setText(getResources().getString(R.string.clipboard_undo_delete, _pendingDeletes.size()));
    _undoBar.setTextColor(getAccentColor()); _undoBar.setBackgroundColor(blendSurface(0.07f));
    _undoBar.setVisibility(GONE);
    _gestures.removeCallbacks(_commitDeletes); _gestures.postDelayed(_commitDeletes, ClipboardPendingDeletion.UNDO_MS);
  }
  private void commitDeletes() {
    for (String key : _pendingDeletes.takeExpired(android.os.SystemClock.uptimeMillis())) {
      _imageStore.delete(key); _pinnedStore.unpinClip(key);
      if (_historyService != null) _historyService.remove_history_entry(key);
    }
    _pendingDeletes.undo(); _undoHeights.clear(); _undoBar.setVisibility(GONE); refreshCards();
  }

  private void installCardGesture(View card, ClipboardItem item, float density) {
    card.setOnTouchListener(new OnTouchListener() {
      float downX, downY; boolean dragging, swiping, scrolling;
      final Runnable hold = () -> {
        _activeHold = null;
        if (scrolling || swiping || _draggedClip != null || !card.isAttachedToWindow()
            || card.getParent() == null || _cards.get(item.historyKey()) != card) return;
        dragging = true; _draggedClip = item.historyKey(); _hoverKey = item.historyKey();
        card.getParent().requestDisallowInterceptTouchEvent(true);
        card.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        try {
          _dragBitmap = android.graphics.Bitmap.createBitmap(card.getWidth(), card.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
          card.draw(new android.graphics.Canvas(_dragBitmap));
        } catch (RuntimeException | OutOfMemoryError failure) { dragging = false; finishPreview(); return; }
        _dragPreview.setImageBitmap(_dragBitmap);
        _dragPreview.setLayoutParams(new FrameLayout.LayoutParams(card.getWidth(), card.getHeight()));
        _dragPreview.setVisibility(VISIBLE);
        int[] position = new int[2]; card.getLocationOnScreen(position);
        _dragOffsetX = downX - position[0]; _dragOffsetY = downY - position[1];
        card.setAlpha(0.25f); movePreview(downX, downY);
      };
      @Override public boolean onTouch(View view, android.view.MotionEvent event) {
        if (card.getParent() == null || !card.isAttachedToWindow()) {
          _gestures.removeCallbacks(hold);
          if (dragging) { dragging = false; finishPreview(); }
          return true;
        }
        float x = event.getRawX(), y = event.getRawY();
        switch (event.getActionMasked()) {
          case android.view.MotionEvent.ACTION_DOWN:
            downX = x; downY = y; dragging = swiping = scrolling = false;
            cancelHold(); _activeHold = hold;
            _gestures.postDelayed(hold, android.view.ViewConfiguration.getLongPressTimeout()); return true;
          case android.view.MotionEvent.ACTION_MOVE:
            float dx = x - downX, dy = y - downY;
            if (dragging) { movePreview(x, y); previewOrder(x, y); return true; }
            if (!swiping && Math.abs(dy) > 10 * density && Math.abs(dy) > Math.abs(dx)) {
              scrolling = true; _gestures.removeCallbacks(hold); return true;
            }
            if (!scrolling && Math.abs(dx) > 10 * density && Math.abs(dx) > Math.abs(dy)) {
              swiping = true; _gestures.removeCallbacks(hold);
              card.getParent().requestDisallowInterceptTouchEvent(true);
            }
            if (swiping) { card.setTranslationX(dx); card.setAlpha(Math.max(0.25f, 1f - Math.abs(dx) / card.getWidth())); }
            return true;
          case android.view.MotionEvent.ACTION_UP:
          case android.view.MotionEvent.ACTION_CANCEL:
            _gestures.removeCallbacks(hold);
            if (card.getParent() != null) card.getParent().requestDisallowInterceptTouchEvent(false);
            // Removing a touch target delivers CANCEL synchronously. Clear state before any grid mutation.
            boolean wasDragging = dragging, wasSwiping = swiping, wasScrolling = scrolling;
            dragging = swiping = scrolling = false;
            if (event.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL) {
              if (wasDragging) { finishPreview(); if (!_rebuildingCards) post(() -> refreshCards()); }
              else { card.setTranslationX(0); card.setAlpha(1f); }
              return true;
            }
            if (wasDragging) { finishPreview(); saveGridOrder(); refreshCards(); }
            else if (wasSwiping && Math.abs(x - downX) > Math.max(48f * density, card.getWidth() * 0.3f)) queueDelete(item.historyKey());
            else { card.setTranslationX(0); card.setAlpha(1f);
              if (!wasSwiping && !wasScrolling) card.performClick(); }
            return true;
        }
        return true;
      }
    });
  }
  private void movePreview(float x, float y) {
    if (_dragBitmap == null) return;
    int[] origin = new int[2]; _clipboardBody.getLocationOnScreen(origin);
    int left = Math.round(x - origin[0] - _dragOffsetX), top = Math.round(y - origin[1] - _dragOffsetY);
    _dragPreview.setX(left); _dragPreview.setY(top);
  }
  private void previewOrder(float x, float y) {
    for (java.util.Map.Entry<String, View> entry : _cards.entrySet()) {
      if (entry.getKey().equals(_draggedClip)) continue;
      View target = entry.getValue(); int[] pos = new int[2]; target.getLocationOnScreen(pos);
      if (x >= pos[0] && x <= pos[0] + target.getWidth() && y >= pos[1] && y <= pos[1] + target.getHeight()) {
        if (!entry.getKey().equals(_hoverKey)) {
          _hoverKey = entry.getKey(); _gridOrder.moveInto(_draggedClip, _hoverKey); layoutExistingCards();
        }
        break;
      }
    }
    int[] pos = new int[2]; _scrollView.getLocationOnScreen(pos);
    int edge = Math.round(40 * getResources().getDisplayMetrics().density);
    if (y < pos[1] + edge) _scrollView.smoothScrollBy(0, -edge / 2);
    else if (y > pos[1] + _scrollView.getHeight() - edge) _scrollView.smoothScrollBy(0, edge / 2);
  }
  private void layoutExistingCards() {
    List<String> keys = _gridOrder.reconcile(new ArrayList<>(_cards.keySet()));
    int[] heights = {0, 0};
    int[][] columns = {new int[2], new int[2]};
    for (int i = 0; i < 2; i++) _gridColumns[i].getLocationOnScreen(columns[i]);
    for (String key : keys) {
      View card = _cards.get(key); if (card == null) continue;
      int column = heights[0] <= heights[1] ? 0 : 1;
      int[] current = new int[2]; card.getLocationOnScreen(current);
      float baseX = current[0] - card.getTranslationX(), baseY = current[1] - card.getTranslationY();
      card.setTranslationX(columns[column][0] - baseX);
      card.setTranslationY(columns[column][1] + heights[column] - baseY);
      heights[column] += card.getMeasuredHeight() + Math.round(8 * getResources().getDisplayMetrics().density);
    }
  }
  private void finishPreview() {
    if (_dragPreview != null) { _dragPreview.setVisibility(GONE); _dragPreview.setImageDrawable(null); }
    _dragBitmap = null; _draggedClip = null; _hoverKey = null;
    for (View card : _cards.values()) card.setAlpha(1f);
  }

  private void saveGridOrder() {
    try {
      getContext().getSharedPreferences("clipboard_grid", Context.MODE_PRIVATE).edit().putString("order", _gridOrder.serialize()).apply();
    } catch (RuntimeException ignored) { }
  }

  private ImageView gridAction(int icon, String description, float density) {
    ImageView button = new ImageView(getContext()); button.setImageResource(icon);
    button.setContentDescription(description); button.setColorFilter(adjustAlpha(_colorLabel, 0.8f));
    int padding = Math.round(8f * density); button.setPadding(padding, padding, padding, padding);
    button.setLayoutParams(new LinearLayout.LayoutParams(Math.round(34f * density), Math.round(34f * density)));
    GradientDrawable circle = new GradientDrawable(); circle.setShape(GradientDrawable.OVAL);
    circle.setColor(blendSurface(0.01f)); button.setBackground(circle);
    return button;
  }

  @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
    super.onSizeChanged(width, height, oldWidth, oldHeight);
    if (width != oldWidth && _gridColumns[0] != null) post(() -> refreshCards());
  }

  private TextView createActionChip(String text, OnClickListener listener, float density)
  {
    TextView tv = new TextView(getContext());
    tv.setText(text);
    tv.setTextColor(_colorLabel);
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
    tv.setPadding(Math.round(8f * density), Math.round(4f * density), Math.round(8f * density), Math.round(4f * density));
    GradientDrawable bg = new GradientDrawable();
    bg.setColor(blendSurface(0.12f));
    bg.setStroke(Math.round(1f * density), adjustAlpha(_colorLabel, 0.30f));
    bg.setCornerRadius(6f * density);
    tv.setBackground(bg);
    tv.setOnClickListener(listener);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lp.rightMargin = Math.round(5f * density);
    tv.setLayoutParams(lp);
    return tv;
  }

  private void analyzeClipboardImage(ClipboardItem item) {
    if (_keyboard == null || !item.isImage || item.imageUri == null || _readingAiImage) return;
    String apiKey = GeminiAiService.getApiKey(getContext());
    if (apiKey == null || apiKey.trim().isEmpty()) {
      Toast.makeText(getContext(), R.string.clipboard_ai_key_required, Toast.LENGTH_LONG).show();
      _keyboard.showAiPane();
      return;
    }
    final Keyboard2 keyboard = _keyboard;
    final long session = keyboard.getInputSessionId();
    final android.view.inputmethod.InputConnection connection = keyboard.getCurrentInputConnection();
    if (connection == null) return;
    final long request = ++_aiImageRequest;
    _readingAiImage = true;
    Toast.makeText(getContext(), R.string.clipboard_ai_image_loading, Toast.LENGTH_SHORT).show();
    _imageStore.readForAnalysis(item.historyKey(), jpeg -> {
      if (request != _aiImageRequest) return;
      _readingAiImage = false;
      // A delayed decode must never replace another editor or a pane the user has left.
      if (!isAttachedToWindow() || getVisibility() != View.VISIBLE || keyboard != _keyboard
          || session != keyboard.getInputSessionId() || connection != keyboard.getCurrentInputConnection()) return;
      if (jpeg == null || jpeg.length == 0) {
        Toast.makeText(getContext(), R.string.clipboard_image_failed, Toast.LENGTH_LONG).show();
        return;
      }
      keyboard.showWritingResults(null, jpeg, typodev.keyboard.ai.AiVisionHelper.AVAILABLE_TONES[0],
          typodev.keyboard.ai.AiWritingResultsView.Task.REPLY);
    });
  }

  private void showCardContextMenu(final ClipboardItem item, View anchor)
  {
    final Context context = getContext();
    final float density = getResources().getDisplayMetrics().density;
    final DialogTheme.Palette palette = DialogTheme.getPalette(context);

    final List<String> optionsList = new ArrayList<>();
    optionsList.add("Paste directly");
    optionsList.add(getResources().getString(item.isImage ? R.string.clipboard_visual_search : R.string.clipboard_web_search));
    if (item.isImage) optionsList.add(getResources().getString(R.string.clipboard_ai_image_comments));
    if (item.hasOtp()) optionsList.add("Paste Code (" + item.extractedOtp + ")");
    if (item.hasUrl()) optionsList.add("Open Link");
    if (item.hasPhone()) optionsList.add("WhatsApp Chat");
    if (item.hasPhone()) optionsList.add("Call number");
    if (item.hasEmail()) optionsList.add("Send email");
    if (!item.isImage && item.content != null && !item.content.trim().isEmpty())
    {
      optionsList.add("AI assistant");
      optionsList.add("Translate (EN ↔ BN)");
      optionsList.add("Grammar Fix & Polish");
      optionsList.add("Summarize Text");
    }
    optionsList.add(item.isPinned ? "Unpin from clipboard" : "Pin to clipboard");
    if (!item.isImage)
    {
      optionsList.add(getResources().getString(R.string.clipboard_edit_shortcut));
      optionsList.add("Save as Snippet");
      optionsList.add("Copy text");
    }
    optionsList.add("Delete");

    final String[] options = optionsList.toArray(new String[0]);

    showInlineMenu("Clipboard Action", options, item, which -> {
            String opt = options[which];
            if (opt.startsWith("Paste directly"))
            {
              if (item.isImage && item.imageUri != null && _keyboard != null)
                _keyboard.pasteImageContent(Uri.parse(item.imageUri), "image/*");
              else if (!item.isImage)
                ClipboardHistoryService.paste(item.content);
              if (_keyboard != null) _keyboard.handle_event_key(KeyValue.Event.SWITCH_BACK_CLIPBOARD);
            }
            else if (opt.equals(getResources().getString(R.string.clipboard_ai_image_comments))) {
              analyzeClipboardImage(item);
            }
            else if (opt.equals(getResources().getString(R.string.clipboard_visual_search))) {
              ClipboardSearchActions.lens(context, Uri.parse(item.imageUri), _imageStore.mimeType(item.historyKey()));
            }
            else if (opt.equals(getResources().getString(R.string.clipboard_web_search))) {
              ClipboardSearchActions.web(context, item.content);
            }
            else if (opt.startsWith("Paste Code"))
            {
              ClipboardHistoryService.paste(item.extractedOtp);
              if (_keyboard != null) _keyboard.handle_event_key(KeyValue.Event.SWITCH_BACK_CLIPBOARD);
            }
            else if (opt.startsWith("Open Link"))
            {
              try
              {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.extractedUrl));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
              }
              catch (Throwable ignored) {}
            }
            else if (opt.startsWith("WhatsApp"))
            {
              try
              {
                String num = item.extractedPhone.replaceAll("[^0-9+]", "");
                Intent waIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + num));
                waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(waIntent);
              }
              catch (Throwable ignored) {}
            }
            else if (opt.equals("Call number") || opt.equals("Send email"))
            {
              try {
                Intent intent = opt.equals("Call number")
                    ? new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + item.extractedPhone))
                    : new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + item.extractedEmail));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent);
              } catch (RuntimeException ignored) { }
            }
            else if (opt.equals("AI assistant")) { showAiActionDialog(item); }
            else if (opt.startsWith("Translate"))
            {
              performAiTranslate(item);
            }
            else if (opt.startsWith("Grammar Fix"))
            {
              performAiAction(item, GeminiAiService.Action.GRAMMAR_FIX, null, "Grammar Fix");
            }
            else if (opt.startsWith("Summarize"))
            {
              performAiAction(item, null, "Summarize the following text clearly and concisely. Output ONLY the summary:\n\n" + item.content, "AI Summary");
            }
            else if (opt.contains("Pin") || opt.contains("pin"))
            {
              if (item.isImage) _imageStore.pin(item.historyKey(), !item.isPinned);
              else if (item.isPinned) _pinnedStore.unpinClip(item.content);
              else
              {
                _pinnedStore.pinClip(item.content);
                if (_historyService != null) _historyService.remove_history_entry(item.historyKey());
              }
              refreshCards();
            }
            else if (opt.equals(getResources().getString(R.string.clipboard_edit_shortcut))) { editShortcut(item); }
            else if (opt.startsWith("Save as Snippet"))
            {
              Intent intent = new Intent(context, typodev.keyboard.snippet.SnippetEditActivity.class);
              intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
              intent.putExtra("clip_text", item.content);
              context.startActivity(intent);
            }
            else if (opt.startsWith("Copy"))
            {
              ClipboardManager cm = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
              if (cm != null)
              {
                cm.setPrimaryClip(ClipData.newPlainText("Copied", item.content));
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
              }
            }
            else if (opt.startsWith("Delete"))
            {
              queueDelete(item.historyKey());
            }    });
  }

  // ===========================================================================
  // AI Assistant Dialogs (Clean Themed Popups)
  // ===========================================================================
  private void showAiActionDialog(final ClipboardItem item)
  {
    final Context context = getContext();
    final float density = getResources().getDisplayMetrics().density;
    final DialogTheme.Palette palette = DialogTheme.getPalette(context);

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(VERTICAL);
    int pad = Math.round(18f * density);
    root.setPadding(pad, pad, pad, pad);

    GradientDrawable rootBg = new GradientDrawable();
    rootBg.setColor(palette.dialogBg);
    rootBg.setCornerRadius(16f * density);
    rootBg.setStroke(Math.round(1.5f * density), palette.dialogBorder);
    root.setBackground(rootBg);

    // Header row: Title + Subtitle
    LinearLayout headerLayout = new LinearLayout(context);
    headerLayout.setOrientation(VERTICAL);
    headerLayout.setPadding(0, 0, 0, Math.round(12f * density));

    TextView tvTitle = new TextView(context);
    tvTitle.setText("AI Assistant");
    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
    tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
    tvTitle.setTextColor(palette.textPrimary);
    headerLayout.addView(tvTitle);

    TextView tvSub = new TextView(context);
    String preview = item.content != null ? item.content.trim().replace('\n', ' ') : "";
    if (preview.length() > 40) preview = preview.substring(0, 37) + "...";
    tvSub.setText(preview.isEmpty() ? "Choose an action" : preview);
    tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
    tvSub.setTextColor(palette.textSecondary);
    tvSub.setSingleLine(true);
    tvSub.setEllipsize(TextUtils.TruncateAt.END);
    tvSub.setPadding(0, Math.round(2f * density), 0, 0);
    headerLayout.addView(tvSub);

    root.addView(headerLayout);

    // Divider
    View divider = new View(context);
    divider.setBackgroundColor(palette.inputBorder);
    LinearLayout.LayoutParams lpDiv = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(1f * density));
    lpDiv.bottomMargin = Math.round(12f * density);
    root.addView(divider, lpDiv);

    // Action items
    final String[] actions = {
      "Translate (EN ↔ BN)",
      "Fix Grammar & Polish",
      "Summarize Text",
      "Professional Tone"
    };
    final int[] icons = {
      R.drawable.ic_translate,
      R.drawable.ic_clip_edit,
      R.drawable.ic_toolbar_clipboard,
      R.drawable.ic_clip_paste
    };

    final AlertDialog[] dialogHolder = new AlertDialog[1];

    for (int i = 0; i < actions.length; i++)
    {
      final int index = i;
      LinearLayout row = new LinearLayout(context);
      row.setOrientation(HORIZONTAL);
      row.setGravity(Gravity.CENTER_VERTICAL);
      int rowPadH = Math.round(14f * density);
      int rowPadV = Math.round(10f * density);
      row.setPadding(rowPadH, rowPadV, rowPadH, rowPadV);

      GradientDrawable rowBg = new GradientDrawable();
      rowBg.setColor(palette.inputBg);
      rowBg.setCornerRadius(10f * density);
      rowBg.setStroke(Math.round(1f * density), palette.inputBorder);
      row.setBackground(rowBg);

      ImageView iv = new ImageView(context);
      iv.setImageResource(icons[i]);
      iv.setColorFilter(palette.accentColor);
      int iconSize = Math.round(20f * density);
      LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(iconSize, iconSize);
      lpIcon.rightMargin = Math.round(10f * density);
      row.addView(iv, lpIcon);

      TextView tvAction = new TextView(context);
      tvAction.setText(actions[i]);
      tvAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
      tvAction.setTypeface(Typeface.DEFAULT_BOLD);
      tvAction.setTextColor(palette.textPrimary);
      row.addView(tvAction);

      row.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (dialogHolder[0] != null) dialogHolder[0].dismiss();
          switch (index)
          {
            case 0:
              performAiTranslate(item);
              break;
            case 1:
              performAiAction(item, GeminiAiService.Action.GRAMMAR_FIX, null, "Grammar Fix");
              break;
            case 2:
              performAiAction(item, null, "Summarize the following text briefly and clearly. Output ONLY the summary:\n\n" + item.content, "AI Summary");
              break;
            case 3:
              performAiAction(item, GeminiAiService.Action.TONE_PROFESSIONAL, null, "Professional Tone");
              break;
          }
        }
      });

      LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      lpRow.bottomMargin = Math.round(8f * density);
      root.addView(row, lpRow);
    }

    // Cancel button inside card at bottom
    TextView btnCancel = new TextView(context);
    btnCancel.setText(android.R.string.cancel);
    btnCancel.setTextColor(palette.textSecondary);
    btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
    btnCancel.setGravity(Gravity.CENTER);
    btnCancel.setPadding(0, Math.round(10f * density), 0, Math.round(4f * density));
    btnCancel.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (dialogHolder[0] != null) dialogHolder[0].dismiss();
      }
    });
    root.addView(btnCancel);

    AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setView(root)
        .create();
    dialogHolder[0] = dialog;

    DialogTheme.applyDialogWindowStyle(dialog, palette, density);
    Utils.show_dialog_on_ime(dialog, getWindowToken());
  }

  private void performAiTranslate(final ClipboardItem item)
  {
    final Context context = getContext();
    boolean hasBengali = containsBengali(item.content);
    String src = hasBengali ? "bn" : "en";
    String target = hasBengali ? "en" : "bn";
    Toast.makeText(context, "Translating (" + src.toUpperCase() + " → " + target.toUpperCase() + ")...", Toast.LENGTH_SHORT).show();

    TranslationEngine.translate(context, item.content, src, target, new TranslationEngine.TranslationCallback()
    {
      @Override
      public void onSuccess(final String translatedText, final boolean isOffline)
      {
        post(new Runnable()
        {
          @Override
          public void run()
          {
            showAiResultDialog("Translation (" + (isOffline ? "Offline" : "Online") + ")", translatedText);
          }
        });
      }

      @Override
      public void onError(final String error)
      {
        post(new Runnable()
        {
          @Override
          public void run()
          {
            Toast.makeText(context, "Translation error: " + error, Toast.LENGTH_SHORT).show();
          }
        });
      }
    });
  }

  private void performAiAction(final ClipboardItem item, final GeminiAiService.Action action, final String promptOverride, final String title)
  {
    final Context context = getContext();
    String key = GeminiAiService.getApiKey(context);
    if (key == null || key.trim().isEmpty())
    {
      AlertDialog d = new AlertDialog.Builder(context)
          .setTitle("AI Setup Required")
          .setMessage("Please enter your free Google Gemini API Key in AI Settings to use AI features.")
          .setPositiveButton("Settings", new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(DialogInterface di, int which)
            {
              AiSettingsDialog.show(context, _keyboard, null);
            }
          })
          .setNegativeButton(android.R.string.cancel, null)
          .create();
      DialogTheme.Palette palette = DialogTheme.getPalette(context);
      DialogTheme.applyDialogWindowStyle(d, palette, getResources().getDisplayMetrics().density);
      DialogTheme.styleButtons(d, palette);
      Utils.show_dialog_on_ime(d, getWindowToken());
      return;
    }

    Toast.makeText(context, "Processing with AI...", Toast.LENGTH_SHORT).show();

    GeminiAiService.AiCallback callback = new GeminiAiService.AiCallback()
    {
      @Override
      public void onSuccess(final String result)
      {
        post(new Runnable()
        {
          @Override
          public void run()
          {
            showAiResultDialog(title, result);
          }
        });
      }

      @Override
      public void onError(final String error)
      {
        post(new Runnable()
        {
          @Override
          public void run()
          {
            Toast.makeText(context, "AI Error: " + error, Toast.LENGTH_SHORT).show();
          }
        });
      }
    };

    if (action != null)
    {
      GeminiAiService.processText(context, item.content, action, callback);
    }
    else if (promptOverride != null)
    {
      GeminiAiService.processPrompt(context, promptOverride, callback);
    }
  }

  /**
   * Displays the AI result popup styled strictly to match the keyboard's active theme,
   * using professional vector icons without emojis.
   */
  private void showAiResultDialog(String title, final String resultText)
  {
    final Context context = getContext();
    final float density = getResources().getDisplayMetrics().density;
    final DialogTheme.Palette palette = DialogTheme.getPalette(context);

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(VERTICAL);
    root.setPadding(Math.round(18f * density), Math.round(16f * density), Math.round(18f * density), Math.round(14f * density));

    // 1. Header Row (Sparkle Icon + Title)
    LinearLayout titleRow = new LinearLayout(context);
    titleRow.setOrientation(HORIZONTAL);
    titleRow.setGravity(Gravity.CENTER_VERTICAL);
    titleRow.setPadding(0, 0, 0, Math.round(12f * density));

    ImageView ivIcon = new ImageView(context);
    ivIcon.setImageResource(R.drawable.ic_pref_ai);
    ivIcon.setColorFilter(palette.accentColor);
    int iconSize = Math.round(20f * density);
    LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(iconSize, iconSize);
    lpIcon.rightMargin = Math.round(8f * density);
    titleRow.addView(ivIcon, lpIcon);

    TextView tvTitle = new TextView(context);
    tvTitle.setText(title);
    tvTitle.setTextColor(palette.textPrimary);
    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
    tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
    titleRow.addView(tvTitle);

    root.addView(titleRow);

    // 2. Result Text Box (Styled to match active keyboard input background)
    ScrollView sv = new ScrollView(context);
    sv.setOverScrollMode(OVER_SCROLL_NEVER);
    LinearLayout.LayoutParams lpSv = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpSv.bottomMargin = Math.round(16f * density);

    TextView tvResult = new TextView(context);
    tvResult.setText(resultText);
    tvResult.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
    tvResult.setTextColor(palette.textPrimary);
    tvResult.setTextIsSelectable(true);
    GradientDrawable tvBg = new GradientDrawable();
    tvBg.setColor(palette.inputBg);
    tvBg.setCornerRadius(10f * density);
    tvBg.setStroke(Math.round(1f * density), palette.inputBorder);
    tvResult.setBackground(tvBg);
    tvResult.setPadding(Math.round(14f * density), Math.round(12f * density), Math.round(14f * density), Math.round(12f * density));
    sv.addView(tvResult);
    root.addView(sv, lpSv);

    // 3. Action Buttons Row (Professional Vector Icons + Clean Labels)
    LinearLayout btnRow = new LinearLayout(context);
    btnRow.setOrientation(HORIZONTAL);
    btnRow.setGravity(Gravity.CENTER_VERTICAL);

    final AlertDialog[] dialogHolder = new AlertDialog[1];

    // 3.1 Paste Button (Primary Accent)
    int pasteTextColor = DialogTheme.getContrastingTextColor(palette.accentColor);
    View btnPaste = createThemedActionButton("Paste", R.drawable.ic_clip_paste,
        palette.accentColor, pasteTextColor, 0, new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (dialogHolder[0] != null) dialogHolder[0].dismiss();
        if (_isMultiPasteMode)
        {
          ClipboardHistoryService.paste(resultText + " ");
          _multiPasteCount++;
          updateMultiPasteBanner();
        }
        else
        {
          ClipboardHistoryService.paste(resultText);
          if (_keyboard != null)
          {
            _keyboard.handle_event_key(KeyValue.Event.SWITCH_BACK_CLIPBOARD);
          }
        }
      }
    }, density);
    LinearLayout.LayoutParams lpPaste = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f);
    lpPaste.rightMargin = Math.round(6f * density);
    btnRow.addView(btnPaste, lpPaste);

    // 3.2 Copy Button (Themed Surface)
    View btnCopy = createThemedActionButton("Copy", R.drawable.ic_clip_copy,
        palette.inputBg, palette.textPrimary, palette.inputBorder, new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (dialogHolder[0] != null) dialogHolder[0].dismiss();
        try
        {
          ClipboardManager cm = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
          if (cm != null)
          {
            cm.setPrimaryClip(ClipData.newPlainText("AI Result", resultText));
          }
          ClipboardHistoryService.setRecentCopiedClip(resultText);
          ClipboardHistoryService hs = ClipboardHistoryService.get_service(context);
          if (hs != null) hs.add_clip(resultText);
          Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
        catch (Throwable ignored) {}
      }
    }, density);
    LinearLayout.LayoutParams lpCopy = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    lpCopy.rightMargin = Math.round(6f * density);
    btnRow.addView(btnCopy, lpCopy);

    // 3.3 Pin Button (Themed Surface)
    View btnPin = createThemedActionButton("Pin", R.drawable.ic_clip_pin,
        palette.inputBg, palette.textPrimary, palette.inputBorder, new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (dialogHolder[0] != null) dialogHolder[0].dismiss();
        _pinnedStore.pinClip(resultText);
        refreshCards();
        Toast.makeText(context, "Pinned to clipboard", Toast.LENGTH_SHORT).show();
      }
    }, density);
    LinearLayout.LayoutParams lpPin = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    btnRow.addView(btnPin, lpPin);

    root.addView(btnRow);

    // 4. Cancel Button (Right aligned text matching palette secondary color)
    TextView btnCancel = new TextView(context);
    btnCancel.setText("Cancel");
    btnCancel.setTextColor(palette.textSecondary);
    btnCancel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
    btnCancel.setGravity(Gravity.RIGHT);
    btnCancel.setPadding(0, Math.round(14f * density), Math.round(4f * density), 0);
    btnCancel.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (dialogHolder[0] != null) dialogHolder[0].dismiss();
      }
    });
    root.addView(btnCancel);

    AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setView(root)
        .create();
    dialogHolder[0] = dialog;

    // Style root container with solid rounded card background
    GradientDrawable rootBg = new GradientDrawable();
    rootBg.setColor(palette.dialogBg);
    rootBg.setCornerRadius(16f * density);
    rootBg.setStroke(Math.round(1.5f * density), palette.dialogBorder);
    root.setBackground(rootBg);
    DialogTheme.applyDialogWindowStyle(dialog, palette, density);
    Utils.show_dialog_on_ime(dialog, getWindowToken());
  }

  private View createThemedActionButton(String text, int iconResId, int bgColor, int textColor, int strokeColor, OnClickListener listener, float density)
  {
    LinearLayout btn = new LinearLayout(getContext());
    btn.setOrientation(HORIZONTAL);
    btn.setGravity(Gravity.CENTER);
    GradientDrawable gd = new GradientDrawable();
    gd.setColor(bgColor);
    gd.setCornerRadius(8f * density);
    if (strokeColor != 0)
    {
      gd.setStroke(Math.round(1f * density), strokeColor);
    }
    btn.setBackground(gd);
    btn.setPadding(Math.round(10f * density), Math.round(9f * density), Math.round(10f * density), Math.round(9f * density));

    if (iconResId != 0)
    {
      ImageView iv = new ImageView(getContext());
      iv.setImageResource(iconResId);
      iv.setColorFilter(textColor);
      int iconSize = Math.round(16f * density);
      LinearLayout.LayoutParams lpIv = new LinearLayout.LayoutParams(iconSize, iconSize);
      lpIv.rightMargin = Math.round(6f * density);
      btn.addView(iv, lpIv);
    }

    TextView tv = new TextView(getContext());
    tv.setText(text);
    tv.setTextColor(textColor);
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    tv.setTypeface(Typeface.DEFAULT_BOLD);
    btn.addView(tv);

    btn.setOnClickListener(listener);
    return btn;
  }

  // ===========================================================================
  // Backup & Restore
  // ===========================================================================
  private void showBackupRestoreDialog()
  {
    final Context context = getContext();
    final float density = getResources().getDisplayMetrics().density;
    final DialogTheme.Palette palette = DialogTheme.getPalette(context);

    final String[] choices = {
      "Export Pinned to JSON (Copy)",
      "Import Pinned from JSON (Restore)"
    };

    AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setTitle("Backup & Restore Pinned Clips")
        .setItems(choices, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface d, int which)
          {
            if (which == 0)
            {
              if (_pinnedStore == null) _pinnedStore = new PinnedClipboardStore(context);
              String json = _pinnedStore.exportToJson();
              ClipboardManager cm = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
              if (cm != null)
              {
                cm.setPrimaryClip(ClipData.newPlainText("TypoDev Clipboard Backup", json));
              }
              Toast.makeText(context, "Backup JSON copied to clipboard", Toast.LENGTH_SHORT).show();
            }
            else if (which == 1)
            {
              showImportJsonDialog();
            }
          }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .create();

    DialogTheme.applyDialogWindowStyle(dialog, palette, density);
    DialogTheme.styleButtons(dialog, palette);
    Utils.show_dialog_on_ime(dialog, getWindowToken());
  }

  private void showImportJsonDialog()
  {
    final Context context = getContext();
    final float density = getResources().getDisplayMetrics().density;
    final DialogTheme.Palette palette = DialogTheme.getPalette(context);

    final EditText et = new EditText(context);
    et.setHint("Paste backup JSON here...");
    DialogTheme.styleEditText(et, palette, density);
    et.setMinLines(3);

    AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setTitle("Restore Pinned Clips")
        .setMessage("Paste your exported JSON backup below:")
        .setView(et)
        .setPositiveButton("Restore", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface d, int which)
          {
            String text = et.getText().toString();
            if (_pinnedStore == null) _pinnedStore = new PinnedClipboardStore(context);
            int imported = _pinnedStore.importFromJson(text);
            Toast.makeText(context, "Restored " + imported + " pinned clips!", Toast.LENGTH_SHORT).show();
            refreshCards();
          }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .create();

    DialogTheme.applyDialogWindowStyle(dialog, palette, density);
    DialogTheme.styleButtons(dialog, palette);
    Utils.show_dialog_on_ime(dialog, getWindowToken());
  }

  // ===========================================================================
  // Batch Mode Operations
  // ===========================================================================
  private void setEditMode(boolean edit)
  {
    _isEditMode = edit;
    _selectedClips.clear();
    _batchBar.setVisibility(edit ? VISIBLE : GONE);
    _btnEditMode.setText(edit ? "Done" : "Edit");
    _btnEditMode.setTextColor(edit ? getContrastingTextColor(getAccentColor()) : _colorLabel);
    _btnEditMode.setBackground(createToolbarButtonBg(edit));
    refreshCards();
  }

  @Override
  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    _imageStore.addListener(_imageChanged);
    if (_historyService != null) _historyService.set_on_clipboard_history_change(this);
  }

  @Override
  protected void onDetachedFromWindow()
  {
    if (_historyService != null) _historyService.remove_on_clipboard_history_change(this);
    _imageStore.removeListener(_imageChanged);
    cancelHold(); finishPreview(); dismissInlineMenu();
    _aiImageRequest++; _readingAiImage = false;
    super.onDetachedFromWindow();
  }

  private void selectAllClips()
  {
    Set<String> visible = new HashSet<>();
    for (String text : _pinnedStore.getPinnedClips())
      if (!_pendingDeletes.contains(text) && matchesFilterAndSearch(new ClipboardItem(text, true))) visible.add(text);
    if (_historyService != null)
      for (ClipboardItem item : _historyService.clear_expired_and_get_history_items())
        if (!_pendingDeletes.contains(item.historyKey()) && !_pinnedStore.isPinned(item.content) && matchesFilterAndSearch(item)) visible.add(item.historyKey());
    for (ClipboardItem image : _imageStore.items())
      if (!_pendingDeletes.contains(image.historyKey()) && matchesFilterAndSearch(image)) visible.add(image.historyKey());
    if (_selectedClips.containsAll(visible)) _selectedClips.removeAll(visible);
    else _selectedClips.addAll(visible);
    refreshCards();
  }

  private void executeBatchPin()
  {
    if (_selectedClips.isEmpty()) return;
    Set<String> imageKeys = new HashSet<>();
    for (ClipboardItem image : _imageStore.items()) imageKeys.add(image.historyKey());
    if (_historyService != null)
      for (ClipboardItem item : _historyService.clear_expired_and_get_history_items())
        if (item.isImage) imageKeys.add(item.historyKey());
    int pinnedCount = 0;
    for (String s : _selectedClips)
    {
      if (imageKeys.contains(s)) { _imageStore.pin(s, true); pinnedCount++; continue; }
      _pinnedStore.pinClip(s);
      pinnedCount++;
      if (_historyService != null)
      {
        _historyService.remove_history_entry(s);
      }
    }
    Toast.makeText(getContext(), pinnedCount + " clips pinned", Toast.LENGTH_SHORT).show();
    setEditMode(false);
  }

  private void executeBatchDelete() {
    if (_selectedClips.isEmpty()) return;
    for (String key : _selectedClips) rememberUndoHeight(key);
    _pendingDeletes.addAll(_selectedClips, android.os.SystemClock.uptimeMillis());
    setEditMode(false); showUndo();
  }

  private void updateSelectedCountText()
  {
    if (_tvSelectedCount != null)
    {
      _tvSelectedCount.setText(_selectedClips.size() + " selected");
    }
  }

  private void confirmClearAllRecent()
  {
    final Context context = getContext();
    final float density = getResources().getDisplayMetrics().density;
    final DialogTheme.Palette palette = DialogTheme.getPalette(context);

    AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setTitle("Clear Recent History")
        .setMessage("Are you sure you want to clear all unpinned recent clips? Pinned clips will not be deleted.")
        .setPositiveButton("Clear", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface d, int which)
          {
            _imageStore.clearRecent();
            if (_historyService != null)
            {
              _historyService.clear_all_history();
            }
            refreshCards();
            Toast.makeText(getContext(), "Recent history cleared", Toast.LENGTH_SHORT).show();
          }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .create();

    DialogTheme.applyDialogWindowStyle(dialog, palette, density);
    DialogTheme.styleButtons(dialog, palette);
    Utils.show_dialog_on_ime(dialog, getWindowToken());
  }

  @Override
  public void on_clipboard_history_change()
  {
    post(new Runnable()
    {
      @Override
      public void run()
      {
        refreshCards();
      }
    });
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility)
  {
    super.onWindowVisibilityChanged(visibility);
    if (visibility == VISIBLE) post(() -> refreshCards());
    else { cancelHold(); finishPreview(); }
  }

  private static boolean containsBengali(String s)
  {
    if (s == null) return false;
    for (int i = 0; i < s.length(); i++)
    {
      char c = s.charAt(i);
      if (c >= '\u0980' && c <= '\u09FF') return true;
    }
    return false;
  }

  private static int adjustAlpha(int color, float factor)
  {
    // Always treat the source as fully opaque (ignore its own alpha) when computing
    // the new alpha — this avoids double-multiplying alpha on semi-transparent colors.
    int alpha = Math.round(255 * factor);
    int red = Color.red(color);
    int green = Color.green(color);
    int blue = Color.blue(color);
    return Color.argb(alpha, red, green, blue);
  }

  /**
   * Returns a fully opaque version of [color] composited onto [background].
   */
  private static int ensureOpaque(int color, int background)
  {
    int alpha = Color.alpha(color);
    if (alpha == 0xFF) return color;
    float a = alpha / 255f;
    int r = Math.round(Color.red(color) * a + Color.red(background) * (1 - a));
    int g = Math.round(Color.green(color) * a + Color.green(background) * (1 - a));
    int b = Math.round(Color.blue(color) * a + Color.blue(background) * (1 - a));
    return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
  }

  /**
   * Returns a surface colour that is always visually distinct from _colorKeyboard,
   * by blending [factor] of the label colour into the keyboard background colour.
   *
   * Works for BOTH light and dark themes:
   *  - Dark theme  (dark keyboard, white label): slight lightening  → visible chip
   *  - Light theme (white keyboard, black label): slight darkening  → visible chip
   */
  private int blendSurface(float factor)
  {
    float inv = 1f - factor;
    int r = Math.round(Color.red(_colorKeyboard) * inv + Color.red(_colorLabel) * factor);
    int g = Math.round(Color.green(_colorKeyboard) * inv + Color.green(_colorLabel) * factor);
    int b = Math.round(Color.blue(_colorKeyboard) * inv + Color.blue(_colorLabel) * factor);
    return Color.rgb(Math.min(255, r), Math.min(255, g), Math.min(255, b));
  }

  /**
   * Resolves a vibrant, high-contrast accent color guaranteed to contrast against _colorKeyboard.
   * Prevents white-on-white or low-contrast accents in themes like White or Light.
   */
  private int getAccentColor()
  {
    double kbLum = (0.299 * Color.red(_colorKeyboard) + 0.587 * Color.green(_colorKeyboard) + 0.114 * Color.blue(_colorKeyboard)) / 255.0;
    int solidAct = ensureOpaque(_colorKeyActivated, _colorKeyboard);
    double actLum = (0.299 * Color.red(solidAct) + 0.587 * Color.green(solidAct) + 0.114 * Color.blue(solidAct)) / 255.0;

    if (Math.abs(actLum - kbLum) >= 0.22)
    {
      return solidAct;
    }

    if (_colorLabelActivated != 0)
    {
      int solidLabelAct = ensureOpaque(_colorLabelActivated, _colorKeyboard);
      double lblLum = (0.299 * Color.red(solidLabelAct) + 0.587 * Color.green(solidLabelAct) + 0.114 * Color.blue(solidLabelAct)) / 255.0;
      if (Math.abs(lblLum - kbLum) >= 0.22)
      {
        return solidLabelAct;
      }
    }

    return (kbLum >= 0.5) ? Color.parseColor("#1A73E8") : Color.parseColor("#60A5FA");
  }

  private int getContrastingTextColor(int backgroundColor)
  {
    double lum = (0.299 * Color.red(backgroundColor) + 0.587 * Color.green(backgroundColor) + 0.114 * Color.blue(backgroundColor)) / 255.0;
    return lum >= 0.55 ? 0xFF111827 : 0xFFFFFFFF;
  }

  private Drawable createToolbarButtonBg(boolean active)
  {
    float density = getResources().getDisplayMetrics().density;
    GradientDrawable gd = new GradientDrawable();
    gd.setCornerRadius(6f * density);
    if (active)
    {
      int accent = getAccentColor();
      gd.setColor(accent);
    }
    else
    {
      gd.setColor(blendSurface(0.045f));
    }
    return gd;
  }
}
