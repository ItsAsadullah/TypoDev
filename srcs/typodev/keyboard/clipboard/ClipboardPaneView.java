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
    OTP("Codes"),
    LINKS("Links"),
    CONTACTS("Contacts"),
    IMAGES("Images");

    public final String label;
    FilterTab(String label)
    {
      this.label = label;
    }
  }

  private Keyboard2 _keyboard;
  private ClipboardHistoryService _historyService;
  private PinnedClipboardStore _pinnedStore;

  // Header & Toolbar views
  private LinearLayout _topBar;
  private TextView _btnBack;
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
      _btnBack.setTextColor(_colorLabel);
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

    final float density = getResources().getDisplayMetrics().density;

    // =========================================================================
    // 1. Top Header & Search Toolbar
    // =========================================================================
    _topBar = new LinearLayout(context);
    _topBar.setOrientation(HORIZONTAL);
    _topBar.setGravity(Gravity.CENTER_VERTICAL);
    _topBar.setPadding(Math.round(8f * density), Math.round(5f * density), Math.round(8f * density), Math.round(5f * density));

    // 1.1 Back / Switch Back button
    _btnBack = new TextView(context);
    _btnBack.setText("← ABC");
    _btnBack.setTextColor(_colorLabel);
    _btnBack.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    _btnBack.setTypeface(Typeface.DEFAULT_BOLD);
    _btnBack.setGravity(Gravity.CENTER);
    _btnBack.setPadding(Math.round(8f * density), Math.round(6f * density), Math.round(8f * density), Math.round(6f * density));
    _btnBack.setBackground(createToolbarButtonBg(false));
    _btnBack.setOnClickListener(new OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_keyboard != null)
        {
          _keyboard.handle_event_key(KeyValue.Event.SWITCH_BACK_CLIPBOARD);
        }
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
    _etSearch.setHint("Search clipboard...");
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

    addView(_topBar);

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
    addView(_filterScrollView, lpFilterScroll);

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

    _scrollView.addView(_contentContainer);
    addView(_scrollView);

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
      tvTab.setPadding(Math.round(12f * density), Math.round(5f * density), Math.round(12f * density), Math.round(5f * density));
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
      bg.setCornerRadius(14f * density);
      if (isSelected)
      {
        bg.setColor(solidAccent);
        tvTab.setTextColor(getContrastingTextColor(solidAccent));
        tvTab.setTypeface(Typeface.DEFAULT_BOLD);
      }
      else
      {
        bg.setColor(blendSurface(0.10f));
        bg.setStroke(Math.round(1f * density), adjustAlpha(_colorLabel, 0.25f));
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

  public void refreshCards()
  {
    final Context context = getContext();
    final float density = getResources().getDisplayMetrics().density;

    if (_historyService != null)
    {
      _historyService.add_current_clip();
    }

    _pinnedCardsContainer.removeAllViews();
    _recentCardsContainer.removeAllViews();

    List<String> rawPinned = _pinnedStore.getPinnedClips();
    List<ClipboardItem> rawRecentItems = (_historyService != null)
        ? _historyService.clear_expired_and_get_history_items() : new ArrayList<ClipboardItem>();

    List<ClipboardItem> pinnedItems = new ArrayList<>();
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

    boolean showPinnedSec = (_currentFilter == FilterTab.ALL || _currentFilter == FilterTab.PINNED || (!pinnedItems.isEmpty() && _currentFilter != FilterTab.RECENT));
    boolean showRecentSec = (_currentFilter == FilterTab.ALL || _currentFilter == FilterTab.RECENT || (!recentItems.isEmpty() && _currentFilter != FilterTab.PINNED));

    _pinnedSection.setVisibility(showPinnedSec ? VISIBLE : GONE);
    _recentSection.setVisibility(showRecentSec ? VISIBLE : GONE);

    // 1. Render Pinned Cards
    _tvPinnedHeader.setText("PINNED (" + pinnedItems.size() + ")");
    _tvPinnedEmpty.setVisibility(pinnedItems.isEmpty() && showPinnedSec ? VISIBLE : GONE);
    for (final ClipboardItem item : pinnedItems)
    {
      _pinnedCardsContainer.addView(createCardView(item, density));
    }

    // 2. Render Recent Cards
    _tvRecentHeader.setText("RECENT (" + recentItems.size() + ")");
    _tvRecentEmpty.setVisibility(recentItems.isEmpty() && showRecentSec ? VISIBLE : GONE);
    _btnRecentClearAll.setVisibility(recentItems.isEmpty() ? GONE : VISIBLE);
    for (final ClipboardItem item : recentItems)
    {
      _recentCardsContainer.addView(createCardView(item, density));
    }

    updateSelectedCountText();
  }

  private boolean matchesFilterAndSearch(ClipboardItem item)
  {
    if (item == null) return false;

    // 1. Search Query
    if (_searchQuery != null && !_searchQuery.isEmpty())
    {
      if (item.content == null || !item.content.toLowerCase(Locale.ROOT).contains(_searchQuery.toLowerCase(Locale.ROOT)))
      {
        return false;
      }
    }

    // 2. Category Tab Filter
    switch (_currentFilter)
    {
      case ALL:
        return true;
      case PINNED:
        return item.isPinned;
      case RECENT:
        return !item.isPinned;
      case OTP:
        return item.hasOtp() || item.category == ClipboardItem.Category.CODE_OTP;
      case LINKS:
        return item.hasUrl() || item.category == ClipboardItem.Category.URL;
      case CONTACTS:
        return item.hasPhone() || item.hasEmail() || item.category == ClipboardItem.Category.PHONE || item.category == ClipboardItem.Category.EMAIL;
      case IMAGES:
        return item.isImage || item.category == ClipboardItem.Category.IMAGE;
      default:
        return true;
    }
  }

  private View createCardView(final ClipboardItem item, final float density)
  {
    final Context context = getContext();

    LinearLayout card = new LinearLayout(context);
    card.setOrientation(VERTICAL);
    LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    lpCard.bottomMargin = Math.round(8f * density);
    card.setLayoutParams(lpCard);

    GradientDrawable cardBg = new GradientDrawable();
    // blendSurface gives a card background clearly distinct from the keyboard bg
    // in both light (white bg) and dark (dark bg) themes.
    cardBg.setColor(blendSurface(0.08f));
    cardBg.setCornerRadius(10f * density);
    cardBg.setStroke(Math.round(1f * density), adjustAlpha(_colorLabel, 0.22f));
    card.setBackground(cardBg);
    card.setPadding(Math.round(12f * density), Math.round(10f * density), Math.round(12f * density), Math.round(10f * density));

    // Top metadata row (Badge on left, icons / checkbox on right)
    LinearLayout topRow = new LinearLayout(context);
    topRow.setOrientation(HORIZONTAL);
    topRow.setGravity(Gravity.CENTER_VERTICAL);

    if (item.category != ClipboardItem.Category.TEXT)
    {
      TextView tvBadge = new TextView(context);
      tvBadge.setText(item.category.label);
      tvBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
      int badgeColor = (item.category == ClipboardItem.Category.CODE_OTP) ? 0xFFE65100 : getAccentColor();
      tvBadge.setTextColor(getContrastingTextColor(badgeColor));
      tvBadge.setPadding(Math.round(6f * density), Math.round(2f * density), Math.round(6f * density), Math.round(2f * density));
      GradientDrawable badgeBg = new GradientDrawable();
      badgeBg.setColor(badgeColor);
      badgeBg.setCornerRadius(10f * density);
      tvBadge.setBackground(badgeBg);
      topRow.addView(tvBadge);
    }

    View spacer = new View(context);
    topRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

    if (_isEditMode)
    {
      final CheckBox cb = new CheckBox(context);
      cb.setChecked(_selectedClips.contains(item.historyKey()));
      cb.setClickable(false);
      topRow.addView(cb);
    }
    else
    {
      // AI Quick Assistant Button on Card
      if (!item.isImage && item.content != null && !item.content.trim().isEmpty())
      {
        int solidAccent = getAccentColor();
        TextView btnAi = new TextView(context);
        btnAi.setText("AI");
        btnAi.setTextColor(solidAccent);
        btnAi.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f);
        btnAi.setTypeface(Typeface.DEFAULT_BOLD);
        btnAi.setPadding(Math.round(8f * density), Math.round(2f * density), Math.round(8f * density), Math.round(2f * density));
        GradientDrawable aiBg = new GradientDrawable();
        aiBg.setColor(adjustAlpha(solidAccent, 0.15f));
        aiBg.setStroke(Math.round(1f * density), adjustAlpha(solidAccent, 0.60f));
        aiBg.setCornerRadius(6f * density);
        btnAi.setBackground(aiBg);
        btnAi.setOnClickListener(new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            showAiActionDialog(item);
          }
        });
        LinearLayout.LayoutParams lpAi = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpAi.rightMargin = Math.round(6f * density);
        topRow.addView(btnAi, lpAi);
      }

      // Professional Vector Pin Icon
      ImageView ivPin = new ImageView(context);
      ivPin.setImageResource(R.drawable.ic_clip_pin);
      int solidAccent = getAccentColor();
      ivPin.setColorFilter(item.isPinned ? solidAccent : adjustAlpha(_colorLabel, 0.75f));
      int actionIconSize = Math.round(24f * density);
      int actionIconPad = Math.round(3f * density);
      ivPin.setPadding(actionIconPad, actionIconPad, actionIconPad, actionIconPad);
      LinearLayout.LayoutParams lpPin = new LinearLayout.LayoutParams(actionIconSize, actionIconSize);
      lpPin.rightMargin = Math.round(4f * density);
      ivPin.setLayoutParams(lpPin);
      ivPin.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (item.isPinned)
          {
            _pinnedStore.unpinClip(item.content);
          }
          else
          {
            _pinnedStore.pinClip(item.content);
            if (_historyService != null)
            {
              _historyService.remove_history_entry(item.historyKey());
            }
          }
          refreshCards();
        }
      });
      // The pinned store persists text only; do not turn an image into "[Image]".
      if (!item.isImage) topRow.addView(ivPin);

      // Professional Vector Delete Icon
      ImageView ivDelete = new ImageView(context);
      ivDelete.setImageResource(R.drawable.ic_delete);
      ivDelete.setColorFilter(adjustAlpha(_colorLabel, 0.70f));
      ivDelete.setPadding(actionIconPad, actionIconPad, actionIconPad, actionIconPad);
      ivDelete.setLayoutParams(new LinearLayout.LayoutParams(actionIconSize, actionIconSize));
      ivDelete.setOnClickListener(new OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (item.isPinned)
          {
            _pinnedStore.unpinClip(item.content);
          }
          else if (_historyService != null)
          {
            _historyService.remove_history_entry(item.historyKey());
          }
          refreshCards();
        }
      });
      topRow.addView(ivDelete);
    }

    card.addView(topRow);

    // Image thumbnail preview if this is an image clip
    if (item.isImage && item.imageUri != null)
    {
      try
      {
        ImageView iv = new ImageView(context);
        iv.setImageURI(Uri.parse(item.imageUri));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setAdjustViewBounds(true);
        LinearLayout.LayoutParams lpIv = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, Math.round(110f * density));
        lpIv.topMargin = Math.round(6f * density);
        lpIv.bottomMargin = Math.round(6f * density);
        card.addView(iv, lpIv);
      }
      catch (Throwable ignored) {}
    }

    // Text content preview
    if (!item.isImage || (item.content != null && !item.content.equals("[Image]")))
    {
      TextView tvContent = new TextView(context);
      tvContent.setText(item.content);
      tvContent.setTextColor(_colorLabel);
      tvContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
      tvContent.setMaxLines(4);
      tvContent.setEllipsize(android.text.TextUtils.TruncateAt.END);
      LinearLayout.LayoutParams lpContent = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      lpContent.topMargin = Math.round(6f * density);
      card.addView(tvContent, lpContent);
    }

    // Contextual Action Chips Row (Codes, Links, WhatsApp, Call, Email)
    if (item.hasOtp() || item.hasUrl() || item.hasPhone() || item.hasEmail())
    {
      HorizontalScrollView actionScroll = new HorizontalScrollView(context);
      actionScroll.setHorizontalScrollBarEnabled(false);
      actionScroll.setOverScrollMode(OVER_SCROLL_NEVER);
      LinearLayout.LayoutParams lpActionScroll = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      lpActionScroll.topMargin = Math.round(8f * density);

      LinearLayout actionRow = new LinearLayout(context);
      actionRow.setOrientation(HORIZONTAL);
      actionRow.setGravity(Gravity.CENTER_VERTICAL);

      // 1. Prominent OTP Chip (Clean Text, No Emoji)
      if (item.hasOtp())
      {
        TextView btnOtp = new TextView(context);
        btnOtp.setText("Paste Code: " + item.extractedOtp);
        btnOtp.setTextColor(_colorKeyActivated);
        btnOtp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        btnOtp.setTypeface(Typeface.DEFAULT_BOLD);
        btnOtp.setPadding(Math.round(8f * density), Math.round(5f * density), Math.round(8f * density), Math.round(5f * density));
        GradientDrawable otpBg = new GradientDrawable();
        otpBg.setColor(adjustAlpha(_colorKeyActivated, 0.2f));
        otpBg.setStroke(Math.round(1.2f * density), _colorKeyActivated);
        otpBg.setCornerRadius(6f * density);
        btnOtp.setBackground(otpBg);
        btnOtp.setOnClickListener(new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
            if (_isMultiPasteMode)
            {
              ClipboardHistoryService.paste(item.extractedOtp + " ");
              _multiPasteCount++;
              updateMultiPasteBanner();
              Toast.makeText(context, "Pasted code: " + item.extractedOtp, Toast.LENGTH_SHORT).show();
            }
            else
            {
              ClipboardHistoryService.paste(item.extractedOtp);
              if (_keyboard != null)
              {
                _keyboard.handle_event_key(KeyValue.Event.SWITCH_BACK_CLIPBOARD);
              }
            }
          }
        });
        LinearLayout.LayoutParams lpOtp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpOtp.rightMargin = Math.round(6f * density);
        actionRow.addView(btnOtp, lpOtp);
      }

      // 2. Open Link Chip
      if (item.hasUrl())
      {
        TextView btnUrl = createActionChip("Open Link", new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            try
            {
              Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.extractedUrl));
              intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              context.startActivity(intent);
            }
            catch (Throwable t)
            {
              Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show();
            }
          }
        }, density);
        actionRow.addView(btnUrl);
      }

      // 3. WhatsApp Direct Chat & Call Chips
      if (item.hasPhone())
      {
        TextView btnWa = createActionChip("WhatsApp", new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            try
            {
              String num = item.extractedPhone.replaceAll("[^0-9+]", "");
              Intent waIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + num));
              waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              context.startActivity(waIntent);
            }
            catch (Throwable t)
            {
              Toast.makeText(context, "Cannot open WhatsApp", Toast.LENGTH_SHORT).show();
            }
          }
        }, density);
        actionRow.addView(btnWa);

        TextView btnCall = createActionChip("Call", new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            try
            {
              Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + item.extractedPhone));
              callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              context.startActivity(callIntent);
            }
            catch (Throwable t)
            {
              Toast.makeText(context, "Cannot start dialer", Toast.LENGTH_SHORT).show();
            }
          }
        }, density);
        actionRow.addView(btnCall);
      }

      // 4. Send Email Chip
      if (item.hasEmail())
      {
        TextView btnEmail = createActionChip("Email", new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            try
            {
              Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + item.extractedEmail));
              emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              context.startActivity(emailIntent);
            }
            catch (Throwable t)
            {
              Toast.makeText(context, "Cannot compose email", Toast.LENGTH_SHORT).show();
            }
          }
        }, density);
        actionRow.addView(btnEmail);
      }

      actionScroll.addView(actionRow);
      card.addView(actionScroll, lpActionScroll);
    }

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

    // Long Press Context Menu
    card.setOnLongClickListener(new OnLongClickListener()
    {
      @Override
      public boolean onLongClick(View v)
      {
        if (_isEditMode) return false;
        try { v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS); }
        catch (Throwable ignored) {}
        showCardContextMenu(item, v);
        return true;
      }
    });

    return card;
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

  private void showCardContextMenu(final ClipboardItem item, View anchor)
  {
    final Context context = getContext();
    final float density = getResources().getDisplayMetrics().density;
    final DialogTheme.Palette palette = DialogTheme.getPalette(context);

    final List<String> optionsList = new ArrayList<>();
    optionsList.add("Paste directly");
    if (item.hasOtp()) optionsList.add("Paste Code (" + item.extractedOtp + ")");
    if (item.hasUrl()) optionsList.add("Open Link");
    if (item.hasPhone()) optionsList.add("WhatsApp Chat");
    if (!item.isImage && item.content != null && !item.content.trim().isEmpty())
    {
      optionsList.add("Translate (EN ↔ BN)");
      optionsList.add("Grammar Fix & Polish");
      optionsList.add("Summarize Text");
    }
    if (!item.isImage)
    {
      optionsList.add(item.isPinned ? "Unpin from clipboard" : "Pin to clipboard");
      optionsList.add("Save as Snippet");
      optionsList.add("Copy text");
    }
    optionsList.add("Delete");

    final String[] options = optionsList.toArray(new String[0]);

    AlertDialog dialog = new AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        .setTitle("Clipboard Action")
        .setItems(options, new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface d, int which)
          {
            String opt = options[which];
            if (opt.startsWith("Paste directly"))
            {
              if (item.isImage && item.imageUri != null && _keyboard != null)
                _keyboard.pasteImageContent(Uri.parse(item.imageUri), "image/*");
              else if (!item.isImage)
                ClipboardHistoryService.paste(item.content);
              if (_keyboard != null) _keyboard.handle_event_key(KeyValue.Event.SWITCH_BACK_CLIPBOARD);
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
              if (item.isPinned) _pinnedStore.unpinClip(item.content);
              else
              {
                _pinnedStore.pinClip(item.content);
                if (_historyService != null) _historyService.remove_history_entry(item.historyKey());
              }
              refreshCards();
            }
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
              if (item.isPinned) _pinnedStore.unpinClip(item.content);
              else if (_historyService != null) _historyService.remove_history_entry(item.historyKey());
              refreshCards();
            }
          }
        })
        .setNegativeButton(android.R.string.cancel, null)
        .create();

    DialogTheme.applyDialogWindowStyle(dialog, palette, density);
    DialogTheme.styleButtons(dialog, palette);
    dialog.setOnShowListener(new DialogInterface.OnShowListener()
    {
      @Override
      public void onShow(DialogInterface d)
      {
        DialogTheme.styleButtonsNow(dialog, palette);
        DialogTheme.styleDialogListItems(dialog, palette);
      }
    });
    Utils.show_dialog_on_ime(dialog, anchor.getWindowToken());
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
    if (_historyService != null) _historyService.set_on_clipboard_history_change(this);
  }

  @Override
  protected void onDetachedFromWindow()
  {
    if (_historyService != null) _historyService.remove_on_clipboard_history_change(this);
    super.onDetachedFromWindow();
  }

  private void selectAllClips()
  {
    List<String> pinned = _pinnedStore.getPinnedClips();
    List<ClipboardItem> recent = (_historyService != null)
        ? _historyService.clear_expired_and_get_history_items() : new ArrayList<ClipboardItem>();

    if (_selectedClips.size() >= pinned.size() + recent.size())
    {
      _selectedClips.clear();
    }
    else
    {
      _selectedClips.addAll(pinned);
      for (ClipboardItem it : recent)
      {
        _selectedClips.add(it.historyKey());
      }
    }
    refreshCards();
  }

  private void executeBatchPin()
  {
    if (_selectedClips.isEmpty()) return;
    Set<String> imageKeys = new HashSet<>();
    if (_historyService != null)
      for (ClipboardItem item : _historyService.clear_expired_and_get_history_items())
        if (item.isImage) imageKeys.add(item.historyKey());
    int pinnedCount = 0;
    for (String s : _selectedClips)
    {
      if (imageKeys.contains(s)) continue;
      _pinnedStore.pinClip(s);
      pinnedCount++;
      if (_historyService != null)
      {
        _historyService.remove_history_entry(s);
      }
    }
    Toast.makeText(getContext(), pinnedCount + " text clips pinned", Toast.LENGTH_SHORT).show();
    setEditMode(false);
  }

  private void executeBatchDelete()
  {
    if (_selectedClips.isEmpty()) return;
    int count = _selectedClips.size();
    for (String s : _selectedClips)
    {
      _pinnedStore.unpinClip(s);
      if (_historyService != null)
      {
        _historyService.remove_history_entry(s);
      }
    }
    Toast.makeText(getContext(), count + " clips deleted", Toast.LENGTH_SHORT).show();
    setEditMode(false);
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
    if (visibility == VISIBLE)
    {
      refreshCards();
    }
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
      gd.setColor(blendSurface(0.08f));
      gd.setStroke(Math.round(1f * density), adjustAlpha(_colorLabel, 0.20f));
    }
    return gd;
  }
}
