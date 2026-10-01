package typodev.keyboard.theme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;
import typodev.keyboard.Config;
import typodev.keyboard.CustomThemeStore;
import typodev.keyboard.DeviceLocales;
import typodev.keyboard.Keyboard2View;
import typodev.keyboard.KeyboardData;
import typodev.keyboard.LayoutModifier;
import typodev.keyboard.Logs;
import typodev.keyboard.DirectBootAwarePreferences;
import typodev.keyboard.prefs.LayoutsPreference;
import typodev.keyboard.R;

public class ThemeCustomizerActivity extends Activity
{
  private CustomThemeStore _store;

  // Active color states
  private int mKeyboardBg;
  private int mKeyNormal;
  private int mKeySpace;
  private int mKeyShift;
  private int mKeyCtrl;
  private int mKeyEnter;
  private int mKeyBackspace;
  private int mLabelColor;
  private int mSubLabelColor;
  private int mSuggestionBg;
  private int mSuggestionChipBg;
  private int mSuggestionTextColor;
  private int mActionLabelColor;
  private int mKeyPressedBgColor;
  private int mKeyPressedTextColor;
  private int mPopupBgColor;
  private int mPopupTextColor;
  private int mKeyShiftTextColor;
  private int mKeyCtrlTextColor;
  private int mKeyEnterTextColor;
  private int mKeyBackspaceTextColor;
  private int mLockedTextColor;
  private int mActivatedTextColor;

  // Full Border & Corner Radius states
  private boolean mBorderEnabled;
  private int mBorderColor;
  private float mBorderWidthDp;
  private float mBorderRadiusDp;
  private float mOuterCornerRadiusDp;

  // Key Bottom Border (Underline) states
  private boolean mBottomBorderEnabled;
  private int mBottomBorderColor;
  private float mBottomBorderHeightDp;
  private boolean mBottomBorderShift;
  private boolean mBottomBorderCtrl;
  private boolean mBottomBorderBackspace;
  private boolean mBottomBorderSpace;
  private boolean mBottomBorderEnter;

  // Gradient states
  private boolean mHasGradient;
  private int mGradientStart;
  private int mGradientEnd;

  // Dimension & Padding states
  private int mKeyboardHeightPercent;
  private int mMarginBottomDp;
  private int mHorizontalMarginDp;
  private float mCharacterSize;
  private float mKeyVerticalMargin;
  private float mKeyHorizontalMargin;

  // Opacity & Brightness states
  private int mLabelBrightness;
  private int mKeyboardOpacity;
  private int mKeyOpacity;
  private int mKeyActivatedOpacity;

  // General Appearance views
  private Switch mSwitchPopupPreview;

  // Full Border views
  private Switch mSwitchBorder;
  private TextView mTvBorderWidthValue;
  private SeekBar mSeekbarBorderWidth;
  private TextView mTvBorderRadiusValue;
  private SeekBar mSeekbarBorderRadius;
  private TextView mTvOuterCornerRadiusValue;
  private SeekBar mSeekbarOuterCornerRadius;

  // Bottom Border views
  private Switch mSwitchBottomBorder;
  private TextView mTvBottomBorderHeightValue;
  private SeekBar mSeekbarBottomBorderHeight;
  private Switch mSwitchBottomBorderShift;
  private Switch mSwitchBottomBorderCtrl;
  private Switch mSwitchBottomBorderBackspace;
  private Switch mSwitchBottomBorderSpace;
  private Switch mSwitchBottomBorderEnter;

  // Dimension & Padding views
  private TextView mTvKeyboardHeightValue;
  private SeekBar mSeekbarKeyboardHeight;
  private TextView mTvMarginBottomValue;
  private SeekBar mSeekbarMarginBottom;
  private TextView mTvHorizontalMarginValue;
  private SeekBar mSeekbarHorizontalMargin;
  private TextView mTvCharacterSizeValue;
  private SeekBar mSeekbarCharacterSize;
  private TextView mTvKeyVerticalSpaceValue;
  private SeekBar mSeekbarKeyVerticalSpace;
  private TextView mTvKeyHorizontalSpaceValue;
  private SeekBar mSeekbarKeyHorizontalSpace;

  // Opacity views
  private TextView mTvLabelBrightnessValue;
  private SeekBar mSeekbarLabelBrightness;
  private TextView mTvKeyboardOpacityValue;
  private SeekBar mSeekbarKeyboardOpacity;
  private TextView mTvKeyOpacityValue;
  private SeekBar mSeekbarKeyOpacity;
  private TextView mTvPressedKeyOpacityValue;
  private SeekBar mSeekbarPressedKeyOpacity;

  // Preview container views (Pinned sticky at top)
  private LinearLayout mPreviewKeyboardContainer;
  private LinearLayout mPreviewSuggestionBar;
  private TextView mPreviewBtnBackToolbar;
  private TextView mPreviewChip1;
  private TextView mPreviewChip2;
  private TextView mPreviewChip3;
  private TextView mPreviewChip4;
  private View mPreviewBtnAi;
  private View mPreviewBtnSnippet;
  private View mPreviewBtnMic;
  private Keyboard2View mPreviewKeyboardView;
  private Config mPreviewConfig;

  private LinearLayout mSavedThemesContainer;
  private LinearLayout mColorItemsContainer;
  private float mDensity;
  private String mActiveThemeOrPaletteName = "";

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    super.onCreate(savedInstanceState);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    // Only bootstrap when no IME exists. Never replace a live service config.
    if (Config.globalConfig() == null)
      Config.initGlobalConfig(DirectBootAwarePreferences.get_shared_preferences(this),
          getResources(), false, null);
    mPreviewConfig = Config.createPreviewConfig(prefs, getResources());
    if (mPreviewConfig.extra_keys_subtype == null)
    {
      try
      {
        DeviceLocales dl = DeviceLocales.load(this);
        mPreviewConfig.extra_keys_subtype = dl.extra_keys();
      }
      catch (Throwable ignored)
      {
      }
    }

    setContentView(R.layout.activity_theme_customizer);

    mDensity = getResources().getDisplayMetrics().density;
    _store = CustomThemeStore.instance(this);

    // Initialize from stored settings
    loadColorsAndSettingsFromStore();

    initViews();
    updatePreview();
    buildPresetPalettesList();
    buildSavedThemesList();
    buildColorItemsList();
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    if (mPreviewKeyboardView != null)
    {
      mPreviewKeyboardView.setPreviewMode(true);
    }
    if (mSwitchPopupPreview != null)
    {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      mSwitchPopupPreview.setChecked(prefs.getBoolean("popup_on_keypress", false));
    }
  }

  private String findMatchingPaletteName()
  {
    for (CustomThemeStore.Palette p : CustomThemeStore.getAllBuiltinPresets(this))
    {
      if (matchesPalette(p)) return p.name;
    }
    for (CustomThemeStore.Palette p : _store.getSavedThemes())
    {
      if (matchesPalette(p)) return p.name;
    }
    return "";
  }

  private boolean matchesPalette(CustomThemeStore.Palette p)
  {
    if (p == null) return false;
    if (mHasGradient != p.hasGradient) return false;
    if (mHasGradient)
    {
      return mGradientStart == p.gradientStart && mGradientEnd == p.gradientEnd;
    }
    return mKeyboardBg == p.keyboardBg
        && mKeyNormal == p.keyNormal
        && mKeyShift == p.keyShift;
  }

  private void loadColorsAndSettingsFromStore()
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    String activeThemeName = prefs.getString("theme", "system");
    boolean hasSavedCustom = _store.hasSavedCustomTheme();

    if ("custom".equals(activeThemeName) && hasSavedCustom)
    {
      mKeyboardBg = _store.getKeyboardBg();
      mKeyNormal = _store.getKeyNormal();
      mKeySpace = _store.getKeySpace();
      mKeyShift = _store.getKeyShift();
      mKeyCtrl = _store.getKeyCtrl();
      mKeyEnter = _store.getKeyEnter();
      mKeyBackspace = _store.getKeyBackspace();
      mLabelColor = _store.getLabelColor();
      mSubLabelColor = _store.getSubLabelColor();
      mSuggestionBg = _store.getSuggestionBg();
      mSuggestionChipBg = _store.getSuggestionChipBg();
      mSuggestionTextColor = _store.getSuggestionTextColor();
      mBorderEnabled = _store.isBorderEnabled();
      mBorderColor = _store.getBorderColor();
      mBorderWidthDp = _store.getBorderWidthDp();
      mBorderRadiusDp = _store.getBorderRadiusDp();
      mOuterCornerRadiusDp = _store.getOuterCornerRadiusDp();
      mBottomBorderEnabled = _store.isBottomBorderEnabled();
      mBottomBorderColor = _store.getBottomBorderColor();
      mBottomBorderHeightDp = _store.getBottomBorderHeightDp();
      mActionLabelColor = _store.getActionLabelColor();
      mKeyShiftTextColor = _store.getShiftTextColor();
      mKeyCtrlTextColor = _store.getCtrlTextColor();
      mKeyEnterTextColor = _store.getEnterTextColor();
      mKeyBackspaceTextColor = _store.getBackspaceTextColor();
      mLockedTextColor = _store.getLockedTextColor();
      mActivatedTextColor = _store.getActivatedTextColor();
      mKeyPressedBgColor = _store.getKeyPressedBgColor();
      mKeyPressedTextColor = _store.getKeyPressedTextColor();
      mPopupBgColor = _store.getPopupBgColor();
      mPopupTextColor = _store.getPopupTextColor();
      mBottomBorderShift = _store.isBottomBorderShift();
      mBottomBorderCtrl = _store.isBottomBorderCtrl();
      mBottomBorderBackspace = _store.isBottomBorderBackspace();
      mBottomBorderSpace = _store.isBottomBorderSpace();
      mBottomBorderEnter = _store.isBottomBorderEnter();
      mHasGradient = _store.hasGradient();
      mGradientStart = _store.getGradientStart();
      mGradientEnd = _store.getGradientEnd();
      mActiveThemeOrPaletteName = findMatchingPaletteName();
    }
    else
    {
      CustomThemeStore.Palette p = CustomThemeStore.getPresetForThemeName(this, activeThemeName);
      if (p == null) p = CustomThemeStore.getDarkPreset();
      mKeyboardBg = p.keyboardBg;
      mKeyNormal = p.keyNormal;
      mKeySpace = p.keySpace;
      mKeyShift = p.keyShift;
      mKeyCtrl = p.keyCtrl;
      mKeyEnter = p.keyEnter;
      mKeyBackspace = p.keyBackspace;
      mLabelColor = p.labelColor;
      mSubLabelColor = p.subLabelColor;
      mSuggestionBg = p.suggestionBg;
      mSuggestionChipBg = p.suggestionChipBg;
      mSuggestionTextColor = p.suggestionTextColor;
      mBorderEnabled = p.hasBorder;
      mBorderColor = p.borderColor;
      mBorderWidthDp = p.borderWidthDp;
      mBorderRadiusDp = p.borderRadiusDp;
      mOuterCornerRadiusDp = p.outerCornerRadiusDp;
      mBottomBorderEnabled = p.hasBottomBorder;
      mBottomBorderColor = p.bottomBorderColor;
      mBottomBorderHeightDp = p.bottomBorderHeightDp;
      mActionLabelColor = p.actionLabelColor;
      mKeyShiftTextColor = p.shiftTextColor;
      mKeyCtrlTextColor = p.ctrlTextColor;
      mKeyEnterTextColor = p.enterTextColor;
      mKeyBackspaceTextColor = p.backspaceTextColor;
      mLockedTextColor = p.lockedTextColor;
      mActivatedTextColor = p.activatedTextColor;
      mKeyPressedBgColor = p.keyPressedBgColor;
      mKeyPressedTextColor = p.keyPressedTextColor;
      mPopupBgColor = p.popupBgColor;
      mPopupTextColor = p.popupTextColor;
      mBottomBorderShift = p.bottomBorderShift;
      mBottomBorderCtrl = p.bottomBorderCtrl;
      mBottomBorderBackspace = p.bottomBorderBackspace;
      mBottomBorderSpace = p.bottomBorderSpace;
      mBottomBorderEnter = p.bottomBorderEnter;
      mHasGradient = p.hasGradient;
      mGradientStart = p.gradientStart;
      mGradientEnd = p.gradientEnd;
      mActiveThemeOrPaletteName = p.name;
    }

    // Load Dimensions from prefs
    mKeyboardHeightPercent = prefs.getInt("keyboard_height", 35);
    mMarginBottomDp = prefs.getInt("margin_bottom_portrait", 7);
    mHorizontalMarginDp = prefs.getInt("horizontal_margin_portrait", 3);
    mCharacterSize = prefs.getFloat("character_size", 1.15f);
    mKeyVerticalMargin = prefs.getFloat("key_vertical_margin", 1.5f);
    mKeyHorizontalMargin = prefs.getFloat("key_horizontal_margin", 2.0f);

    // Load Opacity & Brightness from prefs
    mLabelBrightness = prefs.getInt("label_brightness", 100);
    mKeyboardOpacity = prefs.getInt("keyboard_opacity", 100);
    mKeyOpacity = prefs.getInt("key_opacity", 100);
    mKeyActivatedOpacity = prefs.getInt("key_activated_opacity", 100);
  }

  private void setupAccordion(int headerId, final int contentId, final int chevronId, boolean defaultOpen)
  {
    View header = findViewById(headerId);
    final View content = findViewById(contentId);
    final TextView chevron = (TextView)findViewById(chevronId);
    if (header == null || content == null) return;

    content.setVisibility(defaultOpen ? View.VISIBLE : View.GONE);
    if (chevron != null)
    {
      chevron.setText(defaultOpen ? "⌵" : "›");
      chevron.setTextColor(defaultOpen ? Color.parseColor("#38BDF8") : Color.parseColor("#94A3B8"));
    }

    header.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        boolean isOpen = content.getVisibility() == View.VISIBLE;
        content.setVisibility(isOpen ? View.GONE : View.VISIBLE);
        if (chevron != null)
        {
          chevron.setText(isOpen ? "›" : "⌵");
          chevron.setTextColor(isOpen ? Color.parseColor("#94A3B8") : Color.parseColor("#38BDF8"));
        }
      }
    });
  }

  private void initViews()
  {
    View btnBack = findViewById(R.id.btn_back);
    if (btnBack != null)
    {
      btnBack.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          finish();
        }
      });
    }

    View btnResetDefault = findViewById(R.id.btn_reset_dracula);
    if (btnResetDefault != null)
    {
      btnResetDefault.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          new AlertDialog.Builder(ThemeCustomizerActivity.this)
              .setTitle("Reset to Default Theme")
              .setMessage("Are you sure you want to restore the original default theme?")
              .setPositiveButton("Reset", new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                  _store.resetToDefaultTheme();
                  SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ThemeCustomizerActivity.this);
                  prefs.edit()
                      .putString("theme", "system")
                      .putBoolean("custom_enabled", false)
                      .putInt("keyboard_height", 35)
                      .putInt("margin_bottom_portrait", 7)
                      .putInt("horizontal_margin_portrait", 3)
                      .putFloat("character_size", 1.15f)
                      .putFloat("key_vertical_margin", 1.5f)
                      .putFloat("key_horizontal_margin", 2.0f)
                      .putInt("label_brightness", 100)
                      .putInt("keyboard_opacity", 100)
                      .putInt("key_opacity", 100)
                      .putInt("key_activated_opacity", 100)
                      .apply();

                  DirectBootAwarePreferences.copy_preferences_to_protected_storage(
                      ThemeCustomizerActivity.this, prefs);
                  mPreviewConfig.refresh(getResources(), false, null);

                  loadColorsAndSettingsFromStore();
                  updateBorderControlsUi();
                  updateDimensionsUi();
                  updateOpacityUi();
                  updatePreview();
                  buildColorItemsList();
                  buildPresetPalettesList();
                  buildSavedThemesList();

                  Toast.makeText(ThemeCustomizerActivity.this, "Theme reset to default", Toast.LENGTH_SHORT).show();
                }
              })
              .setNegativeButton("Cancel", null)
              .show();
        }
      });
    }

    View btnSaveAs = findViewById(R.id.btn_save_as_preset);
    if (btnSaveAs != null)
    {
      btnSaveAs.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          showSaveAsNewDialog();
        }
      });
    }

    // Pinned Sticky Live Keyboard Preview Views
    mPreviewKeyboardContainer = (LinearLayout)findViewById(R.id.preview_keyboard_container);
    mPreviewSuggestionBar = (LinearLayout)findViewById(R.id.preview_suggestion_bar);
    mPreviewBtnBackToolbar = (TextView)findViewById(R.id.preview_btn_back_toolbar);
    mPreviewChip1 = (TextView)findViewById(R.id.preview_chip_1);
    mPreviewChip2 = (TextView)findViewById(R.id.preview_chip_2);
    mPreviewChip3 = (TextView)findViewById(R.id.preview_chip_3);
    mPreviewChip4 = (TextView)findViewById(R.id.preview_chip_4);
    mPreviewBtnAi = findViewById(R.id.preview_btn_ai);
    mPreviewBtnSnippet = findViewById(R.id.preview_btn_snippet);
    mPreviewBtnMic = findViewById(R.id.preview_btn_mic);
    mPreviewKeyboardView = (Keyboard2View)findViewById(R.id.preview_keyboard_view);
    if (mPreviewKeyboardView != null)
    {
      mPreviewKeyboardView.setPreviewMode(true);
      mPreviewKeyboardView.setConfig(mPreviewConfig);
      KeyboardData rawKd = null;
      Config cfg = mPreviewConfig;
      if (cfg != null && cfg.layouts != null && !cfg.layouts.isEmpty())
      {
        int curIdx = cfg.get_current_layout();
        if (curIdx >= 0 && curIdx < cfg.layouts.size())
        {
          rawKd = cfg.layouts.get(curIdx);
        }
        if (rawKd == null)
        {
          rawKd = cfg.layouts.get(0);
        }
      }
      if (rawKd == null)
      {
        rawKd = LayoutsPreference.layout_of_string(getResources(), "latn_qwerty_us");
      }
      if (rawKd == null)
      {
        rawKd = KeyboardData.load(getResources(), R.xml.latn_qwerty_us);
      }
      KeyboardData baseKd = LayoutModifier.modify_layout(rawKd);
      if (baseKd == null) baseKd = LayoutModifier.modify_layout_for_preview(rawKd);
      if (baseKd == null) baseKd = rawKd;
      mPreviewKeyboardView.setKeyboard(baseKd);
    }

    mSavedThemesContainer = (LinearLayout)findViewById(R.id.saved_themes_container);
    mColorItemsContainer = (LinearLayout)findViewById(R.id.color_items_container);

    // Setup Collapsible Accordion Sections
    setupAccordion(R.id.header_presets, R.id.content_presets, R.id.chevron_presets, true);
    setupAccordion(R.id.header_borders, R.id.content_borders, R.id.chevron_borders, true);
    setupAccordion(R.id.header_dimensions, R.id.content_dimensions, R.id.chevron_dimensions, false);
    setupAccordion(R.id.header_opacity, R.id.content_opacity, R.id.chevron_opacity, false);
    setupAccordion(R.id.header_popup, R.id.content_popup, R.id.chevron_popup, false);
    setupAccordion(R.id.header_colors, R.id.content_colors, R.id.chevron_colors, false);

    // --- Keypress Popup Preview views & listeners ---
    mSwitchPopupPreview = (Switch)findViewById(R.id.switch_popup_on_keypress);
    View rowPopupOnKeypress = findViewById(R.id.row_popup_on_keypress);
    if (mSwitchPopupPreview != null)
    {
      final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      boolean popupEnabled = prefs.getBoolean("popup_on_keypress", false);
      mSwitchPopupPreview.setChecked(popupEnabled);
      mSwitchPopupPreview.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
          Config cfg = mPreviewConfig;
          if (cfg != null)
          {
            cfg.popup_on_keypress = isChecked;
          }
        }
      });

      if (rowPopupOnKeypress != null)
      {
        rowPopupOnKeypress.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            if (mSwitchPopupPreview != null)
            {
              mSwitchPopupPreview.setChecked(!mSwitchPopupPreview.isChecked());
            }
          }
        });
      }
    }

    // --- Bottom Border (Underline) views & listeners ---
    mSwitchBottomBorder = (Switch)findViewById(R.id.switch_bottom_border);
    View rowBottomBorder = findViewById(R.id.row_bottom_border);
    mTvBottomBorderHeightValue = (TextView)findViewById(R.id.tv_bottom_border_height_value);
    mSeekbarBottomBorderHeight = (SeekBar)findViewById(R.id.seekbar_bottom_border_height);

    if (mSwitchBottomBorder != null)
    {
      mSwitchBottomBorder.setChecked(mBottomBorderEnabled);
      mSwitchBottomBorder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
          if (mBottomBorderEnabled != isChecked)
          {
            mBottomBorderEnabled = isChecked;
            updateBorderControlsUi();
            updatePreview();
          }
        }
      });
    }

    if (rowBottomBorder != null)
    {
      rowBottomBorder.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (mSwitchBottomBorder != null)
          {
            mSwitchBottomBorder.setChecked(!mSwitchBottomBorder.isChecked());
          }
        }
      });
    }

    if (mSeekbarBottomBorderHeight != null)
    {
      mSeekbarBottomBorderHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mBottomBorderHeightDp = 0.5f + (progress * 0.1f);
            if (mTvBottomBorderHeightValue != null)
            {
              mTvBottomBorderHeightValue.setText(String.format(Locale.US, "%.1f dp", mBottomBorderHeightDp));
            }
            updatePreview();
          }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    // Per-key bottom border toggles
    mSwitchBottomBorderShift = (Switch)findViewById(R.id.switch_bottom_border_shift);
    View rowBottomBorderShift = findViewById(R.id.row_bottom_border_shift);
    if (mSwitchBottomBorderShift != null)
    {
      mSwitchBottomBorderShift.setChecked(mBottomBorderShift);
      mSwitchBottomBorderShift.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
          if (mBottomBorderShift != isChecked)
          {
            mBottomBorderShift = isChecked;
            updatePreview();
          }
        }
      });
    }
    if (rowBottomBorderShift != null)
    {
      rowBottomBorderShift.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (mSwitchBottomBorderShift != null)
          {
            mSwitchBottomBorderShift.setChecked(!mSwitchBottomBorderShift.isChecked());
          }
        }
      });
    }

    mSwitchBottomBorderCtrl = (Switch)findViewById(R.id.switch_bottom_border_ctrl);
    View rowBottomBorderCtrl = findViewById(R.id.row_bottom_border_ctrl);
    if (mSwitchBottomBorderCtrl != null)
    {
      mSwitchBottomBorderCtrl.setChecked(mBottomBorderCtrl);
      mSwitchBottomBorderCtrl.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
          if (mBottomBorderCtrl != isChecked)
          {
            mBottomBorderCtrl = isChecked;
            updatePreview();
          }
        }
      });
    }
    if (rowBottomBorderCtrl != null)
    {
      rowBottomBorderCtrl.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (mSwitchBottomBorderCtrl != null)
          {
            mSwitchBottomBorderCtrl.setChecked(!mSwitchBottomBorderCtrl.isChecked());
          }
        }
      });
    }

    mSwitchBottomBorderBackspace = (Switch)findViewById(R.id.switch_bottom_border_backspace);
    View rowBottomBorderBackspace = findViewById(R.id.row_bottom_border_backspace);
    if (mSwitchBottomBorderBackspace != null)
    {
      mSwitchBottomBorderBackspace.setChecked(mBottomBorderBackspace);
      mSwitchBottomBorderBackspace.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
          if (mBottomBorderBackspace != isChecked)
          {
            mBottomBorderBackspace = isChecked;
            updatePreview();
          }
        }
      });
    }
    if (rowBottomBorderBackspace != null)
    {
      rowBottomBorderBackspace.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (mSwitchBottomBorderBackspace != null)
          {
            mSwitchBottomBorderBackspace.setChecked(!mSwitchBottomBorderBackspace.isChecked());
          }
        }
      });
    }

    mSwitchBottomBorderSpace = (Switch)findViewById(R.id.switch_bottom_border_space);
    View rowBottomBorderSpace = findViewById(R.id.row_bottom_border_space);
    if (mSwitchBottomBorderSpace != null)
    {
      mSwitchBottomBorderSpace.setChecked(mBottomBorderSpace);
      mSwitchBottomBorderSpace.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
          if (mBottomBorderSpace != isChecked)
          {
            mBottomBorderSpace = isChecked;
            updatePreview();
          }
        }
      });
    }
    if (rowBottomBorderSpace != null)
    {
      rowBottomBorderSpace.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (mSwitchBottomBorderSpace != null)
          {
            mSwitchBottomBorderSpace.setChecked(!mSwitchBottomBorderSpace.isChecked());
          }
        }
      });
    }

    mSwitchBottomBorderEnter = (Switch)findViewById(R.id.switch_bottom_border_enter);
    View rowBottomBorderEnter = findViewById(R.id.row_bottom_border_enter);
    if (mSwitchBottomBorderEnter != null)
    {
      mSwitchBottomBorderEnter.setChecked(mBottomBorderEnter);
      mSwitchBottomBorderEnter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
          if (mBottomBorderEnter != isChecked)
          {
            mBottomBorderEnter = isChecked;
            updatePreview();
          }
        }
      });
    }
    if (rowBottomBorderEnter != null)
    {
      rowBottomBorderEnter.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (mSwitchBottomBorderEnter != null)
          {
            mSwitchBottomBorderEnter.setChecked(!mSwitchBottomBorderEnter.isChecked());
          }
        }
      });
    }

    // --- Full Border views & listeners ---
    mSwitchBorder = (Switch)findViewById(R.id.switch_border_enabled);
    View rowBorderEnabled = findViewById(R.id.row_border_enabled);
    mTvBorderWidthValue = (TextView)findViewById(R.id.tv_border_width_value);
    mSeekbarBorderWidth = (SeekBar)findViewById(R.id.seekbar_border_width);
    mTvBorderRadiusValue = (TextView)findViewById(R.id.tv_border_radius_value);
    mSeekbarBorderRadius = (SeekBar)findViewById(R.id.seekbar_border_radius);
    mTvOuterCornerRadiusValue = (TextView)findViewById(R.id.tv_outer_corner_radius_value);
    mSeekbarOuterCornerRadius = (SeekBar)findViewById(R.id.seekbar_outer_corner_radius);

    if (mSwitchBorder != null)
    {
      mSwitchBorder.setChecked(mBorderEnabled);
      mSwitchBorder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
      {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
        {
          if (mBorderEnabled != isChecked)
          {
            mBorderEnabled = isChecked;
            updateBorderControlsUi();
            updatePreview();
          }
        }
      });
    }

    if (rowBorderEnabled != null)
    {
      rowBorderEnabled.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (mSwitchBorder != null)
          {
            mSwitchBorder.setChecked(!mSwitchBorder.isChecked());
          }
        }
      });
    }

    if (mSeekbarBorderWidth != null)
    {
      mSeekbarBorderWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mBorderWidthDp = 0.5f + (progress * 0.1f);
            if (mTvBorderWidthValue != null)
            {
              mTvBorderWidthValue.setText(String.format(Locale.US, "%.1f dp", mBorderWidthDp));
            }
            updatePreview();
          }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    if (mSeekbarBorderRadius != null)
    {
      mSeekbarBorderRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mBorderRadiusDp = progress;
            if (mTvBorderRadiusValue != null)
            {
              mTvBorderRadiusValue.setText(progress + " dp");
            }
            updatePreview();
          }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    mTvOuterCornerRadiusValue = (TextView)findViewById(R.id.tv_outer_corner_radius_value);
    mSeekbarOuterCornerRadius = (SeekBar)findViewById(R.id.seekbar_outer_corner_radius);

    if (mSeekbarOuterCornerRadius != null)
    {
      mSeekbarOuterCornerRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mOuterCornerRadiusDp = progress;
            if (mTvOuterCornerRadiusValue != null)
            {
              mTvOuterCornerRadiusValue.setText(progress + " dp");
            }
            updatePreview();
          }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    // --- Dimensions, Height & Padding views & listeners ---
    mTvKeyboardHeightValue = (TextView)findViewById(R.id.tv_keyboard_height_value);
    mSeekbarKeyboardHeight = (SeekBar)findViewById(R.id.seekbar_keyboard_height);
    mTvMarginBottomValue = (TextView)findViewById(R.id.tv_margin_bottom_value);
    mSeekbarMarginBottom = (SeekBar)findViewById(R.id.seekbar_margin_bottom);
    mTvHorizontalMarginValue = (TextView)findViewById(R.id.tv_horizontal_margin_value);
    mSeekbarHorizontalMargin = (SeekBar)findViewById(R.id.seekbar_horizontal_margin);
    mTvCharacterSizeValue = (TextView)findViewById(R.id.tv_character_size_value);
    mSeekbarCharacterSize = (SeekBar)findViewById(R.id.seekbar_character_size);
    mTvKeyVerticalSpaceValue = (TextView)findViewById(R.id.tv_key_vertical_space_value);
    mSeekbarKeyVerticalSpace = (SeekBar)findViewById(R.id.seekbar_key_vertical_space);
    mTvKeyHorizontalSpaceValue = (TextView)findViewById(R.id.tv_key_horizontal_space_value);
    mSeekbarKeyHorizontalSpace = (SeekBar)findViewById(R.id.seekbar_key_horizontal_space);

    if (mSeekbarKeyboardHeight != null)
    {
      mSeekbarKeyboardHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mKeyboardHeightPercent = 15 + progress;
            if (mTvKeyboardHeightValue != null)
              mTvKeyboardHeightValue.setText(mKeyboardHeightPercent + "%");
            updatePreview();
          }
        }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    if (mSeekbarMarginBottom != null)
    {
      mSeekbarMarginBottom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mMarginBottomDp = progress;
            if (mTvMarginBottomValue != null)
              mTvMarginBottomValue.setText(mMarginBottomDp + " dp");
            updatePreview();
          }
        }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    if (mSeekbarHorizontalMargin != null)
    {
      mSeekbarHorizontalMargin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mHorizontalMarginDp = progress;
            if (mTvHorizontalMarginValue != null)
              mTvHorizontalMarginValue.setText(mHorizontalMarginDp + " dp");
            updatePreview();
          }
        }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    if (mSeekbarCharacterSize != null)
    {
      mSeekbarCharacterSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mCharacterSize = 0.75f + (progress * 0.01f);
            if (mTvCharacterSizeValue != null)
              mTvCharacterSizeValue.setText(String.format(Locale.US, "%.2fx", mCharacterSize));
            updatePreview();
          }
        }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    if (mSeekbarKeyVerticalSpace != null)
    {
      mSeekbarKeyVerticalSpace.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mKeyVerticalMargin = progress * 0.1f;
            if (mTvKeyVerticalSpaceValue != null)
              mTvKeyVerticalSpaceValue.setText(String.format(Locale.US, "%.1f%%", mKeyVerticalMargin));
            updatePreview();
          }
        }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    if (mSeekbarKeyHorizontalSpace != null)
    {
      mSeekbarKeyHorizontalSpace.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mKeyHorizontalMargin = progress * 0.1f;
            if (mTvKeyHorizontalSpaceValue != null)
              mTvKeyHorizontalSpaceValue.setText(String.format(Locale.US, "%.1f%%", mKeyHorizontalMargin));
            updatePreview();
          }
        }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    // --- Opacity & Brightness views & listeners ---
    mTvLabelBrightnessValue = (TextView)findViewById(R.id.tv_label_brightness_value);
    mSeekbarLabelBrightness = (SeekBar)findViewById(R.id.seekbar_label_brightness);
    mTvKeyboardOpacityValue = (TextView)findViewById(R.id.tv_keyboard_opacity_value);
    mSeekbarKeyboardOpacity = (SeekBar)findViewById(R.id.seekbar_keyboard_opacity);
    mTvKeyOpacityValue = (TextView)findViewById(R.id.tv_key_opacity_value);
    mSeekbarKeyOpacity = (SeekBar)findViewById(R.id.seekbar_key_opacity);
    mTvPressedKeyOpacityValue = (TextView)findViewById(R.id.tv_pressed_key_opacity_value);
    mSeekbarPressedKeyOpacity = (SeekBar)findViewById(R.id.seekbar_pressed_key_opacity);

    if (mSeekbarLabelBrightness != null)
    {
      mSeekbarLabelBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mLabelBrightness = 50 + progress;
            if (mTvLabelBrightnessValue != null)
              mTvLabelBrightnessValue.setText(mLabelBrightness + "%");
            updatePreview();
          }
        }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    if (mSeekbarKeyboardOpacity != null)
    {
      mSeekbarKeyboardOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mKeyboardOpacity = progress;
            if (mTvKeyboardOpacityValue != null)
              mTvKeyboardOpacityValue.setText(mKeyboardOpacity + "%");
            updatePreview();
          }
        }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    if (mSeekbarKeyOpacity != null)
    {
      mSeekbarKeyOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mKeyOpacity = progress;
            if (mTvKeyOpacityValue != null)
              mTvKeyOpacityValue.setText(mKeyOpacity + "%");
            updatePreview();
          }
        }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    if (mSeekbarPressedKeyOpacity != null)
    {
      mSeekbarPressedKeyOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
      {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
        {
          if (fromUser)
          {
            mKeyActivatedOpacity = progress;
            if (mTvPressedKeyOpacityValue != null)
              mTvPressedKeyOpacityValue.setText(mKeyActivatedOpacity + "%");
            updatePreview();
          }
        }
        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
      });
    }

    updateBorderControlsUi();
    updateDimensionsUi();
    updateOpacityUi();

    // Save & Apply Button
    Button btnSave = (Button)findViewById(R.id.btn_save_apply);
    if (btnSave != null)
    {
      GradientDrawable saveBg = new GradientDrawable(
          GradientDrawable.Orientation.LEFT_RIGHT,
          new int[] { Color.parseColor("#2563EB"), Color.parseColor("#3B82F6") });
      saveBg.setCornerRadius(dp(8));
      btnSave.setBackground(saveBg);
      btnSave.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          try
          {
            // Save Custom Theme colors & borders
            _store.saveColors(mKeyboardBg, mKeyNormal, mKeySpace, mKeyShift, mKeyCtrl, mKeyEnter,
                mKeyBackspace, mLabelColor, mSubLabelColor, mSuggestionBg,
                mSuggestionChipBg, mSuggestionTextColor,
                mBorderEnabled, mBorderColor, mBorderWidthDp, mBorderRadiusDp,
                mBottomBorderEnabled, mBottomBorderColor, mBottomBorderHeightDp,
                mHasGradient, mGradientStart, mGradientEnd, mOuterCornerRadiusDp,
                mActionLabelColor, mKeyPressedBgColor, mKeyPressedTextColor,
                mPopupBgColor, mPopupTextColor,
                mBottomBorderShift, mBottomBorderCtrl, mBottomBorderBackspace,
                mBottomBorderSpace, mBottomBorderEnter,
                mKeyShiftTextColor, mKeyCtrlTextColor, mKeyEnterTextColor, mKeyBackspaceTextColor,
                mLockedTextColor, mActivatedTextColor);

            // Save dimensions, padding, opacity & brightness into preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ThemeCustomizerActivity.this);
            boolean popupVal = (mSwitchPopupPreview != null) ? mSwitchPopupPreview.isChecked() : prefs.getBoolean("popup_on_keypress", false);
            prefs.edit()
                .putString("theme", "custom")
                .putBoolean("custom_enabled", true)
                .putBoolean("popup_on_keypress", popupVal)
                .putInt("keyboard_height", mKeyboardHeightPercent)
                .putInt("margin_bottom_portrait", mMarginBottomDp)
                .putInt("horizontal_margin_portrait", mHorizontalMarginDp)
                .putFloat("character_size", mCharacterSize)
                .putFloat("key_vertical_margin", mKeyVerticalMargin)
                .putFloat("key_horizontal_margin", mKeyHorizontalMargin)
                .putInt("label_brightness", mLabelBrightness)
                .putInt("keyboard_opacity", mKeyboardOpacity)
                .putInt("key_opacity", mKeyOpacity)
                .putInt("key_activated_opacity", mKeyActivatedOpacity)
                .apply();

            DirectBootAwarePreferences.copy_preferences_to_protected_storage(
                ThemeCustomizerActivity.this, prefs);

            Toast.makeText(ThemeCustomizerActivity.this, "Theme applied successfully", Toast.LENGTH_SHORT).show();
            finish();
          }
          catch (Throwable t)
          {
            Logs.print_exception(t);
            Toast.makeText(ThemeCustomizerActivity.this, R.string.theme_apply_failed, Toast.LENGTH_LONG).show();
          }
        }
      });
    }
  }

  private void updateDimensionsUi()
  {
    if (mTvKeyboardHeightValue != null)
      mTvKeyboardHeightValue.setText(mKeyboardHeightPercent + "%");
    if (mSeekbarKeyboardHeight != null)
      mSeekbarKeyboardHeight.setProgress(Math.max(0, Math.min(55, mKeyboardHeightPercent - 15)));

    if (mTvMarginBottomValue != null)
      mTvMarginBottomValue.setText(mMarginBottomDp + " dp");
    if (mSeekbarMarginBottom != null)
      mSeekbarMarginBottom.setProgress(Math.max(0, Math.min(80, mMarginBottomDp)));

    if (mTvHorizontalMarginValue != null)
      mTvHorizontalMarginValue.setText(mHorizontalMarginDp + " dp");
    if (mSeekbarHorizontalMargin != null)
      mSeekbarHorizontalMargin.setProgress(Math.max(0, Math.min(25, mHorizontalMarginDp)));

    if (mTvCharacterSizeValue != null)
      mTvCharacterSizeValue.setText(String.format(Locale.US, "%.2fx", mCharacterSize));
    if (mSeekbarCharacterSize != null)
      mSeekbarCharacterSize.setProgress(Math.max(0, Math.min(75, Math.round((mCharacterSize - 0.75f) / 0.01f))));

    if (mTvKeyVerticalSpaceValue != null)
      mTvKeyVerticalSpaceValue.setText(String.format(Locale.US, "%.1f%%", mKeyVerticalMargin));
    if (mSeekbarKeyVerticalSpace != null)
      mSeekbarKeyVerticalSpace.setProgress(Math.max(0, Math.min(50, Math.round(mKeyVerticalMargin / 0.1f))));

    if (mTvKeyHorizontalSpaceValue != null)
      mTvKeyHorizontalSpaceValue.setText(String.format(Locale.US, "%.1f%%", mKeyHorizontalMargin));
    if (mSeekbarKeyHorizontalSpace != null)
      mSeekbarKeyHorizontalSpace.setProgress(Math.max(0, Math.min(50, Math.round(mKeyHorizontalMargin / 0.1f))));
  }

  private void updateOpacityUi()
  {
    if (mTvLabelBrightnessValue != null)
      mTvLabelBrightnessValue.setText(mLabelBrightness + "%");
    if (mSeekbarLabelBrightness != null)
      mSeekbarLabelBrightness.setProgress(Math.max(0, Math.min(50, mLabelBrightness - 50)));

    if (mTvKeyboardOpacityValue != null)
      mTvKeyboardOpacityValue.setText(mKeyboardOpacity + "%");
    if (mSeekbarKeyboardOpacity != null)
      mSeekbarKeyboardOpacity.setProgress(Math.max(0, Math.min(100, mKeyboardOpacity)));

    if (mTvKeyOpacityValue != null)
      mTvKeyOpacityValue.setText(mKeyOpacity + "%");
    if (mSeekbarKeyOpacity != null)
      mSeekbarKeyOpacity.setProgress(Math.max(0, Math.min(100, mKeyOpacity)));

    if (mTvPressedKeyOpacityValue != null)
      mTvPressedKeyOpacityValue.setText(mKeyActivatedOpacity + "%");
    if (mSeekbarPressedKeyOpacity != null)
      mSeekbarPressedKeyOpacity.setProgress(Math.max(0, Math.min(100, mKeyActivatedOpacity)));
  }


  private void showSaveAsNewDialog()
  {
    final EditText input = new EditText(this);
    input.setHint("Theme Name");
    input.setTextColor(Color.WHITE);
    input.setHintTextColor(Color.parseColor("#64748B"));
    input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    GradientDrawable inputBg = new GradientDrawable();
    inputBg.setColor(Color.parseColor("#1F2937"));
    inputBg.setCornerRadius(dp(8));
    inputBg.setStroke(dp(1), Color.parseColor("#4B5563"));
    input.setBackground(inputBg);
    input.setPadding(dp(12), dp(10), dp(12), dp(10));

    LinearLayout container = new LinearLayout(this);
    container.setOrientation(LinearLayout.VERTICAL);
    container.setPadding(dp(20), dp(12), dp(20), dp(4));
    container.addView(input);

    new AlertDialog.Builder(this)
        .setTitle("Save Theme")
        .setMessage("Enter a name for your theme:")
        .setView(container)
        .setPositiveButton("Save", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) name = "Custom " + (System.currentTimeMillis() % 1000);
            CustomThemeStore.Palette p = new CustomThemeStore.Palette(
                name, mKeyboardBg, mKeyNormal, mKeySpace, mKeyShift, mKeyCtrl, mKeyEnter,
                mKeyBackspace, mLabelColor, mSubLabelColor, mSuggestionBg,
                mSuggestionChipBg, mSuggestionTextColor,
                mBorderEnabled, mBorderColor, mBorderWidthDp, mBorderRadiusDp,
                mBottomBorderEnabled, mBottomBorderColor, mBottomBorderHeightDp,
                mHasGradient, mGradientStart, mGradientEnd, mOuterCornerRadiusDp);
            p.actionLabelColor = mActionLabelColor;
            p.shiftTextColor = mKeyShiftTextColor;
            p.ctrlTextColor = mKeyCtrlTextColor;
            p.enterTextColor = mKeyEnterTextColor;
            p.backspaceTextColor = mKeyBackspaceTextColor;
            p.lockedTextColor = mLockedTextColor;
            p.activatedTextColor = mActivatedTextColor;
            p.keyPressedBgColor = mKeyPressedBgColor;
            p.keyPressedTextColor = mKeyPressedTextColor;
            p.popupBgColor = mPopupBgColor;
            p.popupTextColor = mPopupTextColor;
            p.bottomBorderShift = mBottomBorderShift;
            p.bottomBorderCtrl = mBottomBorderCtrl;
            p.bottomBorderBackspace = mBottomBorderBackspace;
            p.bottomBorderSpace = mBottomBorderSpace;
            p.bottomBorderEnter = mBottomBorderEnter;
            _store.saveNamedTheme(name, p);
            mActiveThemeOrPaletteName = name;
            buildSavedThemesList();
            buildPresetPalettesList();
            Toast.makeText(ThemeCustomizerActivity.this, "Theme saved: " + name, Toast.LENGTH_SHORT).show();
          }
        })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void buildSavedThemesList()
  {
    if (mSavedThemesContainer == null) return;
    mSavedThemesContainer.removeAllViews();
    List<CustomThemeStore.Palette> saved = _store.getSavedThemes();
    if (saved == null || saved.isEmpty())
    {
      TextView empty = new TextView(this);
      empty.setText("No custom themes saved");
      empty.setTextColor(Color.parseColor("#64748B"));
      empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
      empty.setPadding(dp(4), dp(6), dp(4), dp(6));
      mSavedThemesContainer.addView(empty);
      return;
    }

    for (final CustomThemeStore.Palette p : saved)
    {
      final boolean isSelected = (mActiveThemeOrPaletteName != null && mActiveThemeOrPaletteName.equalsIgnoreCase(p.name));
      Button chip = new Button(this);
      chip.setText(isSelected ? "✓ " + p.name : p.name);
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
      chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
      chip.setPadding(dp(14), 0, dp(14), 0);

      GradientDrawable gd = new GradientDrawable();
      gd.setShape(GradientDrawable.RECTANGLE);
      gd.setCornerRadius(dp(10));
      if (isSelected)
      {
        gd.setColor(Color.parseColor("#1E3554"));
        gd.setStroke(dp(1.5f), Color.parseColor("#38BDF8"));
        chip.setTextColor(Color.parseColor("#38BDF8"));
      }
      else
      {
        gd.setColor(Color.parseColor("#1A2436"));
        gd.setStroke(dp(1f), Color.parseColor("#2B3D56"));
        chip.setTextColor(Color.parseColor("#E2E8F0"));
      }
      chip.setBackground(gd);

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
      lp.setMargins(0, 0, dp(8), 0);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          applyPalette(p);
          Toast.makeText(ThemeCustomizerActivity.this, "Loaded: " + p.name, Toast.LENGTH_SHORT).show();
        }
      });

      chip.setOnLongClickListener(new View.OnLongClickListener()
      {
        @Override
        public boolean onLongClick(View v)
        {
          new AlertDialog.Builder(ThemeCustomizerActivity.this)
              .setTitle("Delete Theme")
              .setMessage("Are you sure you want to delete \"" + p.name + "\"?")
              .setPositiveButton("Delete", new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface d, int which)
                {
                  _store.deleteSavedTheme(p.name);
                  if (mActiveThemeOrPaletteName != null && mActiveThemeOrPaletteName.equalsIgnoreCase(p.name))
                  {
                    mActiveThemeOrPaletteName = "";
                  }
                  buildSavedThemesList();
                  buildPresetPalettesList();
                  Toast.makeText(ThemeCustomizerActivity.this, "Deleted: " + p.name, Toast.LENGTH_SHORT).show();
                }
              })
              .setNegativeButton("Cancel", null)
              .show();
          return true;
        }
      });

      mSavedThemesContainer.addView(chip);
    }
  }

  private void buildPresetPalettesList()
  {
    LinearLayout container = (LinearLayout)findViewById(R.id.preset_palettes_container);
    if (container == null) return;
    container.removeAllViews();

    List<CustomThemeStore.Palette> presets = CustomThemeStore.getAllBuiltinPresets(this);
    for (final CustomThemeStore.Palette p : presets)
    {
      final boolean isSelected = (mActiveThemeOrPaletteName != null && mActiveThemeOrPaletteName.equalsIgnoreCase(p.name));
      Button chip = new Button(this);
      chip.setText(isSelected ? "✓ " + p.name : p.name);
      chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
      chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
      chip.setPadding(dp(14), 0, dp(14), 0);

      GradientDrawable gd = new GradientDrawable();
      gd.setShape(GradientDrawable.RECTANGLE);
      gd.setCornerRadius(dp(10));
      if (isSelected)
      {
        gd.setColor(Color.parseColor("#1E3554"));
        gd.setStroke(dp(1.5f), Color.parseColor("#38BDF8"));
        chip.setTextColor(Color.parseColor("#38BDF8"));
      }
      else
      {
        gd.setColor(Color.parseColor("#1A2436"));
        gd.setStroke(dp(1f), Color.parseColor("#2B3D56"));
        chip.setTextColor(Color.parseColor("#E2E8F0"));
      }
      chip.setBackground(gd);

      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
      lp.setMargins(0, 0, dp(8), 0);
      chip.setLayoutParams(lp);

      chip.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          applyPalette(p);
          Toast.makeText(ThemeCustomizerActivity.this, "Preset: " + p.name, Toast.LENGTH_SHORT).show();
        }
      });

      container.addView(chip);
    }
  }

  private void applyPalette(CustomThemeStore.Palette p)
  {
    mActiveThemeOrPaletteName = p.name;
    mKeyboardBg = p.keyboardBg;
    mKeyNormal = p.keyNormal;
    mKeySpace = p.keySpace;
    mKeyShift = p.keyShift;
    mKeyCtrl = p.keyCtrl;
    mKeyEnter = p.keyEnter;
    mKeyBackspace = p.keyBackspace;
    mLabelColor = p.labelColor;
    mSubLabelColor = p.subLabelColor;
    mSuggestionBg = p.suggestionBg;
    mSuggestionChipBg = p.suggestionChipBg;
    mSuggestionTextColor = p.suggestionTextColor;

    mBorderEnabled = p.hasBorder;
    mBorderColor = p.borderColor;
    mBorderWidthDp = p.borderWidthDp;
    mBorderRadiusDp = p.borderRadiusDp;
    mOuterCornerRadiusDp = p.outerCornerRadiusDp;

    mBottomBorderEnabled = p.hasBottomBorder;
    mBottomBorderColor = p.bottomBorderColor;
    mBottomBorderHeightDp = p.bottomBorderHeightDp;
    mActionLabelColor = p.actionLabelColor;
    mKeyShiftTextColor = p.shiftTextColor;
    mKeyCtrlTextColor = p.ctrlTextColor;
    mKeyEnterTextColor = p.enterTextColor;
    mKeyBackspaceTextColor = p.backspaceTextColor;
    mLockedTextColor = p.lockedTextColor;
    mActivatedTextColor = p.activatedTextColor;
    mKeyPressedBgColor = p.keyPressedBgColor;
    mKeyPressedTextColor = p.keyPressedTextColor;
    mPopupBgColor = p.popupBgColor;
    mPopupTextColor = p.popupTextColor;
    mBottomBorderShift = p.bottomBorderShift;
    mBottomBorderCtrl = p.bottomBorderCtrl;
    mBottomBorderBackspace = p.bottomBorderBackspace;
    mBottomBorderSpace = p.bottomBorderSpace;
    mBottomBorderEnter = p.bottomBorderEnter;

    mHasGradient = p.hasGradient;
    mGradientStart = p.gradientStart;
    mGradientEnd = p.gradientEnd;

    updateBorderControlsUi();
    updatePreview();
    buildColorItemsList();
    buildPresetPalettesList();
    buildSavedThemesList();
  }

  private void updatePreview()
  {
    // Container BG
    if (mPreviewKeyboardContainer != null)
    {
      if (mHasGradient)
      {
        GradientDrawable kbBg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] { mGradientStart, mGradientEnd });
        kbBg.setCornerRadius(dp(8));
        kbBg.setStroke(dp(1), Color.parseColor("#4B5563"));
        mPreviewKeyboardContainer.setBackground(kbBg);
      }
      else
      {
        GradientDrawable kbBg = new GradientDrawable();
        kbBg.setShape(GradientDrawable.RECTANGLE);
        kbBg.setColor(mKeyboardBg);
        kbBg.setCornerRadius(dp(8));
        kbBg.setStroke(dp(1), Color.parseColor("#343746"));
        mPreviewKeyboardContainer.setBackground(kbBg);
      }
    }

    // Suggestion / Utility Bar
    if (mPreviewSuggestionBar != null)
    {
      if (mHasGradient)
      {
        GradientDrawable suggBg = new GradientDrawable();
        suggBg.setShape(GradientDrawable.RECTANGLE);
        suggBg.setColor(0x26000000);
        suggBg.setCornerRadius(dp(6));
        mPreviewSuggestionBar.setBackground(suggBg);
      }
      else
      {
        GradientDrawable suggBg = new GradientDrawable();
        suggBg.setShape(GradientDrawable.RECTANGLE);
        suggBg.setColor(mSuggestionBg);
        suggBg.setCornerRadius(dp(6));
        mPreviewSuggestionBar.setBackground(suggBg);
      }
    }

    if (mPreviewBtnBackToolbar != null)
    {
      GradientDrawable circle = new GradientDrawable();
      circle.setShape(GradientDrawable.OVAL);
      circle.setColor(mSuggestionChipBg);
      mPreviewBtnBackToolbar.setBackground(circle);
      mPreviewBtnBackToolbar.setTextColor(mSuggestionTextColor);
    }

    TextView[] chips = {mPreviewChip1, mPreviewChip2, mPreviewChip3, mPreviewChip4};
    for (TextView chip : chips)
    {
      if (chip != null)
      {
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setShape(GradientDrawable.RECTANGLE);
        chipBg.setColor(mSuggestionChipBg);
        chipBg.setCornerRadius(dp(14));
        chip.setBackground(chipBg);
        chip.setTextColor(mSuggestionTextColor);
      }
    }

    if (mPreviewBtnAi != null)
    {
      GradientDrawable circle = new GradientDrawable();
      circle.setShape(GradientDrawable.OVAL);
      circle.setColor(mSuggestionChipBg);
      mPreviewBtnAi.setBackground(circle);
      if (mPreviewBtnAi instanceof ImageView)
      {
        ((ImageView)mPreviewBtnAi).setColorFilter(mSuggestionTextColor, PorterDuff.Mode.SRC_IN);
      }
      else if (mPreviewBtnAi instanceof TextView)
      {
        ((TextView)mPreviewBtnAi).setTextColor(mSuggestionTextColor);
      }
    }
    if (mPreviewBtnSnippet != null)
    {
      mPreviewBtnSnippet.setVisibility(View.GONE);
    }
    if (mPreviewBtnMic != null)
    {
      GradientDrawable circle = new GradientDrawable();
      circle.setShape(GradientDrawable.OVAL);
      circle.setColor(mSuggestionChipBg);
      mPreviewBtnMic.setBackground(circle);
      if (mPreviewBtnMic instanceof ImageView)
      {
        ((ImageView)mPreviewBtnMic).setColorFilter(mSuggestionTextColor, PorterDuff.Mode.SRC_IN);
      }
      else if (mPreviewBtnMic instanceof TextView)
      {
        ((TextView)mPreviewBtnMic).setTextColor(mSuggestionTextColor);
      }
    }

    // Native Keyboard2View live preview update
    if (mPreviewKeyboardView != null)
    {
      Keyboard2View.PreviewThemeOverride pt = new Keyboard2View.PreviewThemeOverride();
      pt.keyboardBg = mKeyboardBg;
      pt.keyNormal = mKeyNormal;
      pt.keySpace = mKeySpace;
      pt.keyShift = mKeyShift;
      pt.keyCtrl = mKeyCtrl;
      pt.keyEnter = mKeyEnter;
      pt.keyBackspace = mKeyBackspace;
      pt.labelColor = mLabelColor;
      pt.subLabelColor = mSubLabelColor;
      pt.borderEnabled = mBorderEnabled;
      pt.borderColor = mBorderColor;
      pt.borderWidthDp = mBorderWidthDp;
      pt.borderRadiusDp = mBorderRadiusDp;
      pt.outerCornerRadiusDp = mOuterCornerRadiusDp;
      pt.bottomBorderEnabled = mBottomBorderEnabled;
      pt.bottomBorderColor = mBottomBorderColor;
      pt.bottomBorderHeightDp = mBottomBorderHeightDp;
      pt.actionLabelColor = mActionLabelColor;
      pt.shiftTextColor = mKeyShiftTextColor;
      pt.ctrlTextColor = mKeyCtrlTextColor;
      pt.enterTextColor = mKeyEnterTextColor;
      pt.backspaceTextColor = mKeyBackspaceTextColor;
      pt.lockedTextColor = mLockedTextColor;
      pt.activatedTextColor = mActivatedTextColor;
      pt.keyPressedBgColor = mKeyPressedBgColor;
      pt.keyPressedTextColor = mKeyPressedTextColor;
      pt.popupBgColor = mPopupBgColor;
      pt.popupTextColor = mPopupTextColor;
      pt.bottomBorderShift = mBottomBorderShift;
      pt.bottomBorderCtrl = mBottomBorderCtrl;
      pt.bottomBorderBackspace = mBottomBorderBackspace;
      pt.bottomBorderSpace = mBottomBorderSpace;
      pt.bottomBorderEnter = mBottomBorderEnter;
      pt.hasGradient = mHasGradient;
      pt.gradientStart = mGradientStart;
      pt.gradientEnd = mGradientEnd;

      // Update preview config dimensions & opacities
      Config cfg = mPreviewKeyboardView.getConfig();
      if (cfg != null)
      {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float base_height = Math.min(dm.heightPixels, dm.widthPixels * 16.f / 9.f);
        cfg.keyboard_rows_height_pixels = (int)(base_height * mKeyboardHeightPercent / 395);
        cfg.margin_bottom = (int)(mMarginBottomDp * dm.density);
        cfg.horizontal_margin = (int)(mHorizontalMarginDp * dm.density);
        cfg.characterSize = mCharacterSize;
        cfg.key_vertical_margin = mKeyVerticalMargin / 100.f;
        cfg.key_horizontal_margin = mKeyHorizontalMargin / 100.f;
        cfg.labelBrightness = (int)(mLabelBrightness * 255 / 100f);
        cfg.keyboardOpacity = (int)(mKeyboardOpacity * 255 / 100f);
        cfg.keyOpacity = (int)(mKeyOpacity * 255 / 100f);
        cfg.keyActivatedOpacity = (int)(mKeyActivatedOpacity * 255 / 100f);
        cfg.borderConfig = mBorderEnabled;
        cfg.customBorderLineWidth = mBorderWidthDp * dm.density;
      }

      mPreviewKeyboardView.setPreviewTheme(pt);
      mPreviewKeyboardView.requestLayout();
      mPreviewKeyboardView.invalidate();
    }
  }

  private void updateBorderControlsUi()
  {
    // Key Bottom Border UI
    if (mSwitchBottomBorder != null && mSwitchBottomBorder.isChecked() != mBottomBorderEnabled)
    {
      mSwitchBottomBorder.setChecked(mBottomBorderEnabled);
    }
    if (mTvBottomBorderHeightValue != null)
    {
      mTvBottomBorderHeightValue.setText(String.format(Locale.US, "%.1f dp", mBottomBorderHeightDp));
    }
    if (mSeekbarBottomBorderHeight != null)
    {
      int prog = Math.round((mBottomBorderHeightDp - 0.5f) / 0.1f);
      if (prog < 0) prog = 0;
      if (prog > 45) prog = 45;
      mSeekbarBottomBorderHeight.setProgress(prog);
    }

    if (mSwitchBottomBorderShift != null && mSwitchBottomBorderShift.isChecked() != mBottomBorderShift)
    {
      mSwitchBottomBorderShift.setChecked(mBottomBorderShift);
    }
    if (mSwitchBottomBorderCtrl != null && mSwitchBottomBorderCtrl.isChecked() != mBottomBorderCtrl)
    {
      mSwitchBottomBorderCtrl.setChecked(mBottomBorderCtrl);
    }
    if (mSwitchBottomBorderBackspace != null && mSwitchBottomBorderBackspace.isChecked() != mBottomBorderBackspace)
    {
      mSwitchBottomBorderBackspace.setChecked(mBottomBorderBackspace);
    }
    if (mSwitchBottomBorderSpace != null && mSwitchBottomBorderSpace.isChecked() != mBottomBorderSpace)
    {
      mSwitchBottomBorderSpace.setChecked(mBottomBorderSpace);
    }
    if (mSwitchBottomBorderEnter != null && mSwitchBottomBorderEnter.isChecked() != mBottomBorderEnter)
    {
      mSwitchBottomBorderEnter.setChecked(mBottomBorderEnter);
    }

    float alpha = mBottomBorderEnabled ? 1.0f : 0.45f;
    View[] perKeyRows = {
        findViewById(R.id.row_bottom_border_shift),
        findViewById(R.id.row_bottom_border_ctrl),
        findViewById(R.id.row_bottom_border_backspace),
        findViewById(R.id.row_bottom_border_space),
        findViewById(R.id.row_bottom_border_enter)
    };
    for (View r : perKeyRows)
    {
      if (r != null) r.setAlpha(alpha);
    }
    if (mSwitchBottomBorderShift != null) mSwitchBottomBorderShift.setEnabled(mBottomBorderEnabled);
    if (mSwitchBottomBorderCtrl != null) mSwitchBottomBorderCtrl.setEnabled(mBottomBorderEnabled);
    if (mSwitchBottomBorderBackspace != null) mSwitchBottomBorderBackspace.setEnabled(mBottomBorderEnabled);
    if (mSwitchBottomBorderSpace != null) mSwitchBottomBorderSpace.setEnabled(mBottomBorderEnabled);
    if (mSwitchBottomBorderEnter != null) mSwitchBottomBorderEnter.setEnabled(mBottomBorderEnabled);

    // Full Border UI
    if (mSwitchBorder != null && mSwitchBorder.isChecked() != mBorderEnabled)
    {
      mSwitchBorder.setChecked(mBorderEnabled);
    }
    if (mTvBorderWidthValue != null)
    {
      mTvBorderWidthValue.setText(String.format(Locale.US, "%.1f dp", mBorderWidthDp));
    }
    if (mSeekbarBorderWidth != null)
    {
      int prog = Math.round((mBorderWidthDp - 0.5f) / 0.1f);
      if (prog < 0) prog = 0;
      if (prog > 35) prog = 35;
      mSeekbarBorderWidth.setProgress(prog);
    }
    if (mTvBorderRadiusValue != null)
    {
      mTvBorderRadiusValue.setText(Math.round(mBorderRadiusDp) + " dp");
    }
    if (mSeekbarBorderRadius != null)
    {
      int prog = Math.round(mBorderRadiusDp);
      if (prog < 0) prog = 0;
      if (prog > 25) prog = 25;
      mSeekbarBorderRadius.setProgress(prog);
    }
    if (mTvOuterCornerRadiusValue != null)
    {
      mTvOuterCornerRadiusValue.setText(Math.round(mOuterCornerRadiusDp) + " dp");
    }
    if (mSeekbarOuterCornerRadius != null)
    {
      int prog = Math.round(mOuterCornerRadiusDp);
      if (prog < 0) prog = 0;
      if (prog > 30) prog = 30;
      mSeekbarOuterCornerRadius.setProgress(prog);
    }
  }

  private void addColorSectionHeader(String title)
  {
    TextView tv = new TextView(this);
    tv.setText(title);
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    tv.setTypeface(Typeface.DEFAULT_BOLD);
    tv.setTextColor(Color.parseColor("#38BDF8"));
    tv.setPadding(dp(16), dp(16), dp(16), dp(6));
    mColorItemsContainer.addView(tv);
  }

  private void buildColorItemsList()
  {
    if (mColorItemsContainer == null) return;
    mColorItemsContainer.removeAllViews();

    // 1. Toolbar & Suggestions Section
    addColorSectionHeader("Toolbar & Suggestions");

    addColorItem("Toolbar Background", mSuggestionBg, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mSuggestionBg = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Toolbar Button Background", mSuggestionChipBg, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mSuggestionChipBg = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Toolbar Icon & Text Color", mSuggestionTextColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mSuggestionTextColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    // 2. Keyboard Keys Section
    addColorSectionHeader("Keyboard Keys");

    addColorItem("Normal Keys", mKeyNormal, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mKeyNormal = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Space Bar", mKeySpace, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mKeySpace = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Enter / Action Key", mKeyEnter, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mKeyEnter = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Shift Key", mKeyShift, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mKeyShift = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Backspace Key", mKeyBackspace, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mKeyBackspace = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Control Key", mKeyCtrl, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mKeyCtrl = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    // 3. Pressed & Active States Section
    addColorSectionHeader("Pressed & Active States");

    addColorItem("Pressed Key Background", mKeyPressedBgColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mKeyPressedBgColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Pressed Key Text Color", mKeyPressedTextColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mKeyPressedTextColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Locked Key Text Color (Long Press)", mLockedTextColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mLockedTextColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Active Key Text Color (Latched)", mActivatedTextColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mActivatedTextColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    // 4. Keypress Popup Section
    addColorSectionHeader("Keypress Popup");

    addColorItem("Popup Background", mPopupBgColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mPopupBgColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Popup Text Color", mPopupTextColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mPopupTextColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    // 5. Labels & Text Section
    addColorSectionHeader("Labels & Text");

    addColorItem("Key Label Color", mLabelColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mLabelColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Action Key Label Color", mActionLabelColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        int oldColor = mActionLabelColor;
        mActionLabelColor = newColor;
        if (mKeyShiftTextColor == oldColor) mKeyShiftTextColor = newColor;
        if (mKeyCtrlTextColor == oldColor) mKeyCtrlTextColor = newColor;
        if (mKeyEnterTextColor == oldColor) mKeyEnterTextColor = newColor;
        if (mKeyBackspaceTextColor == oldColor) mKeyBackspaceTextColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Shift Key Text Color", mKeyShiftTextColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mKeyShiftTextColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Control Key Text Color", mKeyCtrlTextColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mKeyCtrlTextColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Enter Key Text Color", mKeyEnterTextColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mKeyEnterTextColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Backspace Key Text Color", mKeyBackspaceTextColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mKeyBackspaceTextColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Corner & Sub-label Color", mSubLabelColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mSubLabelColor = newColor;
        updatePreview();
        buildColorItemsList();
      }
    });

    // 6. Background & Borders Section
    addColorSectionHeader("Background & Borders");

    addGradientToggleRow();

    if (mHasGradient)
    {
      addColorItem("Gradient Top Color", mGradientStart, new OnColorChangedCallback()
      {
        @Override
        public void onColorChanged(int newColor)
        {
          mGradientStart = newColor;
          updatePreview();
          buildColorItemsList();
        }
      });

      addColorItem("Gradient Bottom Color", mGradientEnd, new OnColorChangedCallback()
      {
        @Override
        public void onColorChanged(int newColor)
        {
          mGradientEnd = newColor;
          updatePreview();
          buildColorItemsList();
        }
      });
    }
    else
    {
      addColorItem("Keyboard Background", mKeyboardBg, new OnColorChangedCallback()
      {
        @Override
        public void onColorChanged(int newColor)
        {
          mKeyboardBg = newColor;
          updatePreview();
          buildColorItemsList();
        }
      });
    }

    addColorItem("Border Outline Color", mBorderColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mBorderColor = newColor;
        updateBorderControlsUi();
        updatePreview();
        buildColorItemsList();
      }
    });

    addColorItem("Bottom Underline Color", mBottomBorderColor, new OnColorChangedCallback()
    {
      @Override
      public void onColorChanged(int newColor)
      {
        mBottomBorderColor = newColor;
        updateBorderControlsUi();
        updatePreview();
        buildColorItemsList();
      }
    });
  }

  private void addGradientToggleRow()
  {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(dp(16), dp(12), dp(16), dp(12));
    row.setClickable(true);
    row.setFocusable(true);

    TypedValue outValue = new TypedValue();
    if (getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true))
    {
      row.setBackgroundResource(outValue.resourceId);
    }

    LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    row.setLayoutParams(rowLp);

    TextView tvTitle = new TextView(this);
    tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
    tvTitle.setText("Gradient Background");
    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
    tvTitle.setTextColor(Color.WHITE);
    tvTitle.setTypeface(null, Typeface.BOLD);
    row.addView(tvTitle);

    final Switch sw = new Switch(this);
    sw.setChecked(mHasGradient);
    sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
    {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
      {
        if (mHasGradient != isChecked)
        {
          mHasGradient = isChecked;
          if (mHasGradient)
          {
            if (mGradientStart == 0) mGradientStart = 0xFF8A2BE2;
            if (mGradientEnd == 0) mGradientEnd = 0xFFAF6679;
          }
          updatePreview();
          buildColorItemsList();
        }
      }
    });
    row.addView(sw);

    row.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        sw.setChecked(!sw.isChecked());
      }
    });

    mColorItemsContainer.addView(row);

    View divider = new View(this);
    LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dp(0.6f));
    divLp.setMargins(dp(16), 0, 0, 0);
    divider.setLayoutParams(divLp);
    divider.setBackgroundColor(Color.parseColor("#202E46"));
    mColorItemsContainer.addView(divider);
  }

  private interface OnColorChangedCallback
  {
    void onColorChanged(int newColor);
  }

  private void addColorItem(final String title, final int color, final OnColorChangedCallback callback)
  {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(dp(16), dp(11), dp(16), dp(11));
    row.setClickable(true);
    row.setFocusable(true);

    TypedValue outValue = new TypedValue();
    if (getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true))
    {
      row.setBackgroundResource(outValue.resourceId);
    }

    LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    row.setLayoutParams(rowLp);

    TextView tvTitle = new TextView(this);
    tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
    tvTitle.setText(title);
    tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
    tvTitle.setTextColor(Color.parseColor("#F1F5F9"));
    row.addView(tvTitle);

    // Hex label badge container
    TextView tvHex = new TextView(this);
    tvHex.setText(String.format("#%06X", (0xFFFFFF & color)));
    tvHex.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
    tvHex.setTextColor(Color.parseColor("#94A3B8"));
    tvHex.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
    tvHex.setPadding(dp(7), dp(2.5f), dp(7), dp(2.5f));
    tvHex.setBackgroundResource(R.drawable.ios_value_badge_bg);
    row.addView(tvHex);

    // Color Swatch Circle
    View swatch = new View(this);
    LinearLayout.LayoutParams swatchLp = new LinearLayout.LayoutParams(dp(24), dp(24));
    swatchLp.setMargins(dp(8), 0, 0, 0);
    swatch.setLayoutParams(swatchLp);
    GradientDrawable swatchBg = new GradientDrawable();
    swatchBg.setShape(GradientDrawable.OVAL);
    swatchBg.setColor(color);
    swatchBg.setStroke(dp(1.2f), Color.parseColor("#475569"));
    swatch.setBackground(swatchBg);
    row.addView(swatch);

    // Subtle iOS Chevron indicator
    TextView tvChevron = new TextView(this);
    LinearLayout.LayoutParams chevLp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    chevLp.setMargins(dp(8), 0, 0, 0);
    tvChevron.setLayoutParams(chevLp);
    tvChevron.setText("›");
    tvChevron.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
    tvChevron.setTextColor(Color.parseColor("#64748B"));
    tvChevron.setTypeface(null, Typeface.BOLD);
    row.addView(tvChevron);

    row.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        ColorPickerDialog.show(ThemeCustomizerActivity.this, title, color, new ColorPickerDialog.OnColorSelectedListener()
        {
          @Override
          public void onColorSelected(int selectedColor)
          {
            callback.onColorChanged(selectedColor);
          }
        });
      }
    });

    mColorItemsContainer.addView(row);

    // Inset divider
    View divider = new View(this);
    LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dp(0.6f));
    divLp.setMargins(dp(16), 0, 0, 0);
    divider.setLayoutParams(divLp);
    divider.setBackgroundColor(Color.parseColor("#202E46"));
    mColorItemsContainer.addView(divider);
  }

  private int dp(int value)
  {
    return Math.round(value * mDensity);
  }

  private int dp(float value)
  {
    return Math.round(value * mDensity);
  }
}
