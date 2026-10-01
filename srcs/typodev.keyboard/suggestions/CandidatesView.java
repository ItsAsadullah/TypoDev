package typodev.keyboard.suggestions;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.graphics.Typeface;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import typodev.keyboard.Config;
import typodev.keyboard.CustomThemeStore;
import typodev.keyboard.KeyValue;
import typodev.keyboard.Logs;
import typodev.keyboard.Pointers;
import typodev.keyboard.R;
import typodev.keyboard.voice.VoiceSpectrumButton;

public class CandidatesView extends LinearLayout
{
  static final int MAX_WORD_CANDIDATES = Suggestions.MAX_COUNT; // 15
  static final int TOTAL_CANDIDATES = MAX_WORD_CANDIDATES + 1; // 16 (15 words + 1 emoji)

  /** Candidates currently visible. Entries can be [null] when there are less
      than [TOTAL_CANDIDATES] suggestions.
      - Entries at indexes [0] to [14] are word suggestions.
      - Entry at index [15] is the emoji suggestion. */
  String[] _items = new String[TOTAL_CANDIDATES];

  /** Text views showing the candidates in [_items]. Text views visibility is
      set to [GONE] when there are less than [TOTAL_CANDIDATES] suggestions. */
  TextView[] _item_views = new TextView[TOTAL_CANDIDATES];

  /** Message when no dictionary is installed. Visible when no candidates are
      shown. Might be [null]. */
  View _status_no_dict = null;

  View _dictionary_switch_button;
  boolean should_show_dictionary_switch = false;

  TextView _lang_name_view;
  private typodev.keyboard.Keyboard2 _keyboard2;
  private ImageView _btn_toolbar_toggle;
  private HorizontalScrollView _suggestions_scroll;
  private LinearLayout _suggestions_container;
  private TextView _emoji_view;
  private View _toolbar_scroll;
  private View _autofill_scroll;
  private LinearLayout _autofill_container;
  private View _btn_magic_container;
  private View _btn_magic_autocomplete;
  private ProgressBar _magic_progress;
  private View _btn_candidate_mic;
  private VoiceSpectrumButton _btn_candidate_mic_container;
  private View _btn_voice_punct_container;
  private CheckBox _cb_voice_punctuation;
  private View _voice_live_container;
  private TextView _tv_voice_live_text;
  private boolean _is_voice_typing_active = false;
  private boolean _toolbar_open = true;
  private boolean _has_suggestions = false;
  private boolean _userManuallyToggled = false;

  private SuggestionRemovalOverlay _removalOverlay = null;
  private boolean _isDraggingSuggestion = false;
  private int _draggedIndex = -1;
  private View _draggedView = null;
  private String _draggedWord = null;

  private View _clipboard_suggestion_container;
  private View _clipboard_suggestion_chip;
  private ImageView _clipboard_suggestion_icon;
  private TextView _clipboard_suggestion_text;
  private View _clipboard_suggestion_close;
  private String _activeClipboardSuggestion = null;

  private View _password_autofill_scroll;
  private View _btn_autofill_chip;
  private View _btn_generate_pwd_chip;
  private View _toolbar_btn_autofill;

  public void set_keyboard2(typodev.keyboard.Keyboard2 keyboard2)
  {
    _keyboard2 = keyboard2;
  }

  public CandidatesView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  private void safeSendKey(KeyValue kv)
  {
    if (kv == null) return;
    try
    {
      Config cfg = (_config != null) ? _config : Config.globalConfig();
      if (cfg != null && cfg.handler != null)
      {
        cfg.handler.key_up(kv, Pointers.Modifiers.EMPTY);
      }
    }
    catch (Throwable ignored) {}
  }

  private void safeEnterSuggestion(String it)
  {
    if (it == null) return;
    try
    {
      Config cfg = (_config != null) ? _config : Config.globalConfig();
      if (cfg != null && cfg.handler != null)
      {
        cfg.handler.suggestion_entered(it);
      }
    }
    catch (Throwable ignored) {}
  }

  @Override
  protected void onFinishInflate()
  {
    super.onFinishInflate();
    setClipChildren(true);
    setClipToPadding(true);
    setup_dictionary_switch_button();
    _lang_name_view = (TextView)findViewById(R.id.candidates_lang_name);
    if (_lang_name_view != null)
    {
      _lang_name_view.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View _v)
        {
          safeSendKey(KeyValue.getKeyByName("change_dictionary"));
        }
      });
    }

    _btn_toolbar_toggle = (ImageView)findViewById(R.id.btn_toolbar_toggle);
    _suggestions_scroll = (HorizontalScrollView)findViewById(R.id.suggestions_scroll);
    _suggestions_container = (LinearLayout)findViewById(R.id.suggestions_container);
    if (_suggestions_scroll != null)
    {
      _suggestions_scroll.setClipChildren(true);
      _suggestions_scroll.setClipToPadding(true);
    }
    if (_suggestions_container != null)
    {
      _suggestions_container.setClipChildren(true);
      _suggestions_container.setClipToPadding(true);
    }
    _toolbar_scroll = findViewById(R.id.toolbar_scroll);
    _autofill_scroll = findViewById(R.id.autofill_scroll);
    _autofill_container = (LinearLayout)findViewById(R.id.autofill_container);
    _password_autofill_scroll = findViewById(R.id.password_autofill_scroll);
    _btn_autofill_chip = findViewById(R.id.btn_autofill_chip);
    _btn_generate_pwd_chip = findViewById(R.id.btn_generate_pwd_chip);
    _toolbar_btn_autofill = findViewById(R.id.toolbar_btn_autofill);

    if (_btn_autofill_chip != null)
    {
      _btn_autofill_chip.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
          typodev.keyboard.autofill.PasswordAutofillHelper.triggerSystemAutofill(_keyboard2);
        }
      });
      _btn_autofill_chip.setOnLongClickListener(new View.OnLongClickListener()
      {
        @Override
        public boolean onLongClick(View v)
        {
          typodev.keyboard.autofill.PasswordAutofillHelper.showPasswordMenu(getContext(), _keyboard2, v.getWindowToken());
          return true;
        }
      });
    }

    if (_btn_generate_pwd_chip != null)
    {
      _btn_generate_pwd_chip.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
          typodev.keyboard.autofill.PasswordAutofillHelper.generateAndInsertPassword(_keyboard2, getContext());
        }
      });
    }

    if (_toolbar_btn_autofill != null)
    {
      _toolbar_btn_autofill.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
          boolean ok = typodev.keyboard.autofill.PasswordAutofillHelper.triggerSystemAutofill(_keyboard2);
          if (!ok)
          {
            typodev.keyboard.autofill.PasswordAutofillHelper.showPasswordMenu(getContext(), _keyboard2, v.getWindowToken());
          }
        }
      });
      _toolbar_btn_autofill.setOnLongClickListener(new View.OnLongClickListener()
      {
        @Override
        public boolean onLongClick(View v)
        {
          typodev.keyboard.autofill.PasswordAutofillHelper.showPasswordMenu(getContext(), _keyboard2, v.getWindowToken());
          return true;
        }
      });
    }
    _btn_candidate_mic = findViewById(R.id.btn_candidate_mic);
    _btn_candidate_mic_container = (VoiceSpectrumButton)findViewById(R.id.btn_candidate_mic_container);
    if (_btn_candidate_mic_container != null && _btn_candidate_mic instanceof android.widget.ImageView)
    {
      _btn_candidate_mic_container.setMicIcon((android.widget.ImageView)_btn_candidate_mic);
    }

    _btn_voice_punct_container = findViewById(R.id.btn_voice_punct_container);
    _cb_voice_punctuation = (CheckBox)findViewById(R.id.cb_voice_punctuation);
    _voice_live_container = findViewById(R.id.voice_live_container);
    _tv_voice_live_text = (TextView)findViewById(R.id.tv_voice_live_text);
    if (_cb_voice_punctuation != null)
    {
      _cb_voice_punctuation.setChecked(typodev.keyboard.voice.VoicePunctuationHelper.isVoiceSpokenPunctuationEnabled(getContext()));
    }
    if (_btn_voice_punct_container != null)
    {
      _btn_voice_punct_container.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (_cb_voice_punctuation == null) return;
          boolean newState = !_cb_voice_punctuation.isChecked();
          _cb_voice_punctuation.setChecked(newState);
          typodev.keyboard.voice.VoicePunctuationHelper.setVoiceSpokenPunctuationEnabled(getContext(), newState);
          try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
          String msg = newState
              ? getContext().getString(R.string.toast_voice_punct_enabled)
              : getContext().getString(R.string.toast_voice_punct_disabled);
          Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
      });
      _btn_voice_punct_container.setOnLongClickListener(new View.OnLongClickListener()
      {
        @Override
        public boolean onLongClick(View v)
        {
          Toast.makeText(getContext(), R.string.pref_voice_spoken_punctuation_summary, Toast.LENGTH_SHORT).show();
          return true;
        }
      });
    }

    View.OnClickListener micClickListener = new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_keyboard2 != null)
        {
          _keyboard2.handle_event_key(KeyValue.Event.SWITCH_VOICE_TYPING);
        }
        else
        {
          safeSendKey(KeyValue.getKeyByName("voice_typing"));
        }
      }
    };
    if (_btn_candidate_mic != null)
    {
      _btn_candidate_mic.setOnClickListener(micClickListener);
    }
    if (_btn_candidate_mic_container != null)
    {
      _btn_candidate_mic_container.setOnClickListener(micClickListener);
    }

    _clipboard_suggestion_container = findViewById(R.id.clipboard_suggestion_container);
    _clipboard_suggestion_chip = findViewById(R.id.clipboard_suggestion_chip);
    _clipboard_suggestion_icon = (ImageView)findViewById(R.id.clipboard_suggestion_icon);
    _clipboard_suggestion_text = (TextView)findViewById(R.id.clipboard_suggestion_text);
    _clipboard_suggestion_close = findViewById(R.id.clipboard_suggestion_close);

    if (_clipboard_suggestion_chip != null)
    {
      _clipboard_suggestion_chip.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (_activeClipboardSuggestion != null && _keyboard2 != null)
          {
            InputConnection ic = _keyboard2.getCurrentInputConnection();
            if (ic != null)
            {
              try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); }
              catch (Throwable ignored) {}
              ic.commitText(_activeClipboardSuggestion, 1);
            }
            typodev.keyboard.ClipboardHistoryService.markClipPasted(_activeClipboardSuggestion);
            _activeClipboardSuggestion = null;
            if (_clipboard_suggestion_container != null)
            {
              _clipboard_suggestion_container.setVisibility(View.GONE);
            }
            onTextOrSelectionChanged();
          }
        }
      });
    }

    if (_clipboard_suggestion_close != null)
    {
      _clipboard_suggestion_close.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (_activeClipboardSuggestion != null)
          {
            typodev.keyboard.ClipboardHistoryService.dismissClip(_activeClipboardSuggestion);
          }
          else
          {
            typodev.keyboard.ClipboardHistoryService.dismissRecentClip();
          }
          _activeClipboardSuggestion = null;
          if (_clipboard_suggestion_container != null)
          {
            _clipboard_suggestion_container.setVisibility(View.GONE);
          }
        }
      });
    }


    _emoji_view = (TextView)findViewById(R.id.candidates_emoji);
    _item_views[MAX_WORD_CANDIDATES] = _emoji_view;
    if (_emoji_view != null)
    {
      _emoji_view.setVisibility(View.GONE);
      setupCandidateTouch(_emoji_view, MAX_WORD_CANDIDATES);
    }

    LayoutInflater inflater = LayoutInflater.from(getContext());
    for (int i = 0; i < MAX_WORD_CANDIDATES; i++)
    {
      final int index = i;
      TextView v = (TextView)inflater.inflate(R.layout.candidates_chip, _suggestions_container, false);
      v.setVisibility(View.GONE);
      setupCandidateTouch(v, index);
      _suggestions_container.addView(v);
      _item_views[i] = v;
    }

    _btn_magic_container = findViewById(R.id.btn_magic_autocomplete_container);
    _btn_magic_autocomplete = findViewById(R.id.btn_magic_autocomplete);
    _magic_progress = (ProgressBar)findViewById(R.id.magic_autocomplete_progress);

    View.OnClickListener magicClick = new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        trigger_magic_autocomplete();
      }
    };
    View.OnLongClickListener magicLongClick = new View.OnLongClickListener()
    {
      @Override
      public boolean onLongClick(View v)
      {
        if (_keyboard2 != null)
        {
          _keyboard2.showAiPane();
          return true;
        }
        return false;
      }
    };

    if (_btn_magic_container != null)
    {
      _btn_magic_container.setOnClickListener(magicClick);
      _btn_magic_container.setOnLongClickListener(magicLongClick);
    }
    if (_btn_magic_autocomplete != null)
    {
      _btn_magic_autocomplete.setOnClickListener(magicClick);
      _btn_magic_autocomplete.setOnLongClickListener(magicLongClick);
    }

    setup_toolbar_buttons();
  }

  private LinearLayout _toolbar_container;
  private ToolbarToolsManager _tools_manager;
  private final Runnable _rebuildToolbar = () -> rebuildToolbarButtons();
  private final ToolbarToolsManager.OnToolsChangedListener _toolsChangedListener = () -> {
    removeCallbacks(_rebuildToolbar);
    post(_rebuildToolbar);
  };

  private void setup_toolbar_buttons()
  {
    if (_btn_toolbar_toggle != null)
    {
      _btn_toolbar_toggle.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
          if (_keyboard2 != null)
          {
            if (_keyboard2.isToolsPaneVisible())
            {
              _keyboard2.closeToolsPane();
            }
            else
            {
              _toolbar_open = true;
              _userManuallyToggled = true;
              update_view_visibility(false);
              _keyboard2.showToolsPane();
            }
          }
          updateToggleIconState();
        }
      });
    }

    _toolbar_container = findViewById(R.id.toolbar_container);
    updateToggleIconState();
    rebuildToolbarButtons();
  }

  public void updateToggleIconState()
  {
    if (_btn_toolbar_toggle == null) return;
    boolean isToolsOpen = (_keyboard2 != null && _keyboard2.isToolsPaneVisible());
    CustomThemeStore store = CustomThemeStore.instance(getContext());
    int textColor = store.isCustomThemeActive(_config) ? store.getSuggestionTextColor() : Color.WHITE;
    if (!store.isCustomThemeActive(_config))
    {
      try
      {
        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(R.attr.colorLabel, tv, true))
          textColor = tv.data;
      }
      catch (Throwable ignored) {}
    }

    if (isToolsOpen)
    {
      _btn_toolbar_toggle.setImageResource(R.drawable.ic_toolbar_back);
      _btn_toolbar_toggle.setContentDescription(getContext().getString(R.string.toolbar_back_to_keyboard));
      _btn_toolbar_toggle.setBackgroundResource(R.drawable.toolbar_chip_active_bg);
      _btn_toolbar_toggle.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
    }
    else
    {
      _btn_toolbar_toggle.setImageResource(R.drawable.ic_toolbar_grid);
      _btn_toolbar_toggle.setContentDescription(getContext().getString(R.string.toolbar_more_tools));
      _btn_toolbar_toggle.setBackgroundResource(R.drawable.round_icon_chip_bg);
      _btn_toolbar_toggle.setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
    }
  }

  public void rebuildToolbarButtons()
  {
    if (_toolbar_container == null)
      _toolbar_container = findViewById(R.id.toolbar_container);
    if (_toolbar_container == null) return;
    _toolbar_container.removeAllViews();

    if (_tools_manager == null)
    {
      _tools_manager = ToolbarToolsManager.getInstance(getContext());
      _tools_manager.addListener(_toolsChangedListener);
    }

    final boolean isCustomizeMode = (_keyboard2 != null && _keyboard2.isToolsPaneEditMode());
    final List<ToolbarToolsManager.ToolItem> pinned = _tools_manager.getPinnedTools();

    float density = getResources().getDisplayMetrics().density;
    int btnW = (int)(34 * density + 0.5f);
    int btnH = (int)(32 * density + 0.5f);
    int marginH = (int)(2.5f * density + 0.5f);

    CustomThemeStore store = CustomThemeStore.instance(getContext());
    int textColor = store.isCustomThemeActive(_config) ? store.getSuggestionTextColor() : Color.WHITE;
    if (!store.isCustomThemeActive(_config))
    {
      try
      {
        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(R.attr.colorLabel, tv, true))
          textColor = tv.data;
      }
      catch (Throwable ignored) {}
    }

    for (int i = 0; i < pinned.size(); i++)
    {
      final ToolbarToolsManager.ToolItem tool = pinned.get(i);

      FrameLayout wrapper = new FrameLayout(getContext());
      LinearLayout.LayoutParams wrapLp = new LinearLayout.LayoutParams(btnW, btnH);
      wrapLp.setMargins(marginH, 0, marginH, 0);
      wrapLp.gravity = Gravity.CENTER_VERTICAL;
      wrapper.setLayoutParams(wrapLp);

      ImageView btn = new ImageView(getContext());
      btn.setId(tool.viewId);
      btn.setImageResource(tool.iconResId);
      btn.setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
      btn.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
      btn.setPadding((int)(5*density), (int)(4*density), (int)(5*density), (int)(4*density));
      btn.setBackgroundResource(R.drawable.toolbar_chip_bg);

      boolean isActive = false;
      if ("translate".equals(tool.id) && _keyboard2 != null && _keyboard2.isTranslateBarOpen())
      {
        isActive = true;
      }
      else if ("dev".equals(tool.id) && _config != null && _config.developer_mode)
      {
        isActive = true;
      }
      Config activeCfg = Config.globalConfig();
      if (activeCfg == null) activeCfg = _config;

      if ("one_hand".equals(tool.id) && _keyboard2 != null && _keyboard2.isOneHandModeActive())
      {
        isActive = true;
      }
      else if ("haptic".equals(tool.id) && activeCfg != null && activeCfg.vibrate_enabled)
      {
        isActive = true;
      }
      else if ("sound".equals(tool.id) && activeCfg != null && activeCfg.sound_on_keypress)
      {
        isActive = true;
      }
      else if ("popup".equals(tool.id) && activeCfg != null && activeCfg.popup_on_keypress)
      {
        isActive = true;
      }

      if (isActive)
      {
        btn.setBackgroundResource(R.drawable.toolbar_chip_active_bg);
        btn.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
      }

      FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
      btn.setLayoutParams(btnLp);
      wrapper.addView(btn);

      if (isCustomizeMode)
      {
        TextView badge = new TextView(getContext());
        badge.setText("−");
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setTextColor(Color.WHITE);
        badge.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.parseColor("#EF4444")); // Red badge for remove
        badge.setBackground(bg);

        int badgeSize = (int)(14 * density + 0.5f);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(badgeSize, badgeSize);
        badgeLp.gravity = Gravity.TOP | Gravity.END;
        badge.setLayoutParams(badgeLp);
        wrapper.addView(badge);

        wrapper.setClickable(true);
        wrapper.setOnClickListener(new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
            _tools_manager.unpinTool(tool.id);
            Toast.makeText(getContext(), tool.label + " removed from top toolbar", Toast.LENGTH_SHORT).show();
          }
        });
      }
      else
      {
        wrapper.setClickable(true);
        wrapper.setOnClickListener(new OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
            if (_keyboard2 != null && _keyboard2.isToolsPaneVisible())
              _keyboard2.closeToolsPane();
            _tools_manager.activateTool(getContext(), _keyboard2, tool.id);
          }
        });

        if ("autofill".equals(tool.id))
        {
          wrapper.setOnLongClickListener(new OnLongClickListener()
          {
            @Override
            public boolean onLongClick(View v)
            {
              try { v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Throwable ignored) {}
              typodev.keyboard.autofill.PasswordAutofillHelper.showPasswordMenu(getContext(), _keyboard2, null);
              return true;
            }
          });
        }
      }

      _toolbar_container.addView(wrapper);
    }

  }

  public void updateMoreButtonState(boolean isOpen)
  {
    _toolbar_open = isOpen || !hasTypedText();
    _userManuallyToggled = isOpen;
    update_view_visibility(_has_suggestions);
    updateToggleIconState();
    rebuildToolbarButtons();
  }

  @Override
  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    if (_tools_manager != null) _tools_manager.addListener(_toolsChangedListener);
    rebuildToolbarButtons();
  }

  @Override
  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    removeCallbacks(_rebuildToolbar);
    if (_tools_manager != null) _tools_manager.removeListener(_toolsChangedListener);
  }

  public void updateTranslateButtonState()
  {
    View btnTranslate = findViewById(R.id.toolbar_btn_translate);
    if (btnTranslate == null) return;
    boolean isActive = (_keyboard2 != null && _keyboard2.isTranslateBarOpen());
    if (isActive)
    {
      btnTranslate.setBackgroundResource(R.drawable.toolbar_chip_active_bg);
      if (btnTranslate instanceof ImageView)
      {
        ((ImageView)btnTranslate).setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
      }
      else if (btnTranslate instanceof TextView)
      {
        ((TextView)btnTranslate).setTextColor(0xFFFFFFFF);
      }
    }
    else
    {
      CustomThemeStore store = CustomThemeStore.instance(getContext());
      int chipBg = store.isCustomThemeActive(_config) ? store.getSuggestionChipBg() : 0x1A808080;
      int textColor = store.isCustomThemeActive(_config) ? store.getSuggestionTextColor() : Color.WHITE;
      if (!store.isCustomThemeActive(_config))
      {
        try
        {
          TypedValue tvLabel = new TypedValue();
          if (getContext().getTheme().resolveAttribute(R.attr.colorLabel, tvLabel, true))
          {
            textColor = tvLabel.data;
          }
          TypedValue tvKey = new TypedValue();
          if (getContext().getTheme().resolveAttribute(R.attr.colorKey, tvKey, true))
          {
            chipBg = tvKey.data;
          }
        }
        catch (Throwable ignored) {}
      }
      boolean isLight = CustomThemeStore.isColorLight(chipBg);
      int rippleColor = isLight ? 0x22000000 : 0x26FFFFFF;
      float buttonRadius = TypedValue.applyDimension(
          TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());
      btnTranslate.setBackground(createThemedRippleDrawable(chipBg, rippleColor, buttonRadius));
      if (btnTranslate instanceof ImageView)
      {
        ((ImageView)btnTranslate).setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
      }
      else if (btnTranslate instanceof TextView)
      {
        ((TextView)btnTranslate).setTextColor(textColor);
      }
    }
  }

  private void updateDevButtonState(View btnDev)
  {
    if (btnDev == null) return;
    boolean isDev = (_config != null && _config.developer_mode);
    btnDev.setAlpha(isDev ? 1.0f : 0.45f);
  }

  public void setInlineSuggestions(List<View> views)
  {
    if (_autofill_container == null) return;
    _autofill_container.removeAllViews();
    if (views != null && !views.isEmpty())
    {
      float density = getResources().getDisplayMetrics().density;
      int marginEnd = (int)(6 * density);
      for (View v : views)
      {
        if (v == null) continue;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT);
        lp.rightMargin = marginEnd;
        v.setLayoutParams(lp);
        _autofill_container.addView(v);
      }
      if (_autofill_scroll != null) _autofill_scroll.setVisibility(View.VISIBLE);
      if (_password_autofill_scroll != null) _password_autofill_scroll.setVisibility(View.GONE);
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
      if (_keyboard2 != null && _keyboard2.isToolsPaneVisible())
        update_view_visibility(_has_suggestions);
    }
    else
    {
      if (_autofill_scroll != null) _autofill_scroll.setVisibility(View.GONE);
      update_view_visibility(_has_suggestions);
    }
  }

  private Config _config;

  private boolean has_valid_dictionary()
  {
    if (_config != null && _config.current_dictionary != null)
      return true;
    try
    {
      if (typodev.keyboard.dict.ExternalDictionaryManager.instance(getContext()).getWordCount() > 0)
        return true;
    }
    catch (Exception ignored) {}
    return false;
  }

  private void update_view_visibility(boolean hasSuggestions)
  {
    if (_dictionary_switch_button != null) _dictionary_switch_button.setVisibility(View.GONE);
    if (_lang_name_view != null) _lang_name_view.setVisibility(View.GONE);

    if (_is_voice_typing_active)
    {
      if (_btn_toolbar_toggle != null) _btn_toolbar_toggle.setVisibility(View.GONE);
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
      if (_voice_live_container != null) _voice_live_container.setVisibility(View.VISIBLE);
      if (_autofill_scroll != null) _autofill_scroll.setVisibility(View.GONE);
      if (_password_autofill_scroll != null) _password_autofill_scroll.setVisibility(View.GONE);
      if (_status_no_dict != null) _status_no_dict.setVisibility(View.GONE);
      if (_btn_magic_container != null) _btn_magic_container.setVisibility(View.GONE);
      else if (_btn_magic_autocomplete != null) _btn_magic_autocomplete.setVisibility(View.GONE);
      if (_btn_voice_punct_container != null) _btn_voice_punct_container.setVisibility(View.VISIBLE);
      if (_btn_candidate_mic_container != null) _btn_candidate_mic_container.setVisibility(View.VISIBLE);
      else if (_btn_candidate_mic != null) _btn_candidate_mic.setVisibility(View.VISIBLE);
      return;
    }
    else
    {
      if (_voice_live_container != null) _voice_live_container.setVisibility(View.GONE);
      if (_btn_voice_punct_container != null) _btn_voice_punct_container.setVisibility(View.GONE);
    }

    if (_btn_toolbar_toggle != null)
    {
      _btn_toolbar_toggle.setVisibility(View.VISIBLE);
    }

    if (_btn_candidate_mic_container != null) _btn_candidate_mic_container.setVisibility(View.VISIBLE);
    else if (_btn_candidate_mic != null) _btn_candidate_mic.setVisibility(View.VISIBLE);

    updateMagicButtonVisibility();

    // Suggestions and autofill can arrive asynchronously while the grid is open.
    // Keep the pinned toolbar and its Back button visible until the user leaves.
    if (_keyboard2 != null && _keyboard2.isToolsPaneVisible())
    {
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.VISIBLE);
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
      if (_autofill_scroll != null) _autofill_scroll.setVisibility(View.GONE);
      if (_password_autofill_scroll != null) _password_autofill_scroll.setVisibility(View.GONE);
      if (_status_no_dict != null) _status_no_dict.setVisibility(View.GONE);
      if (_btn_magic_container != null) _btn_magic_container.setVisibility(View.GONE);
      updateToggleIconState();
      return;
    }

    boolean isPassword = (_config != null && _config.editor_config != null && _config.editor_config.is_password);
    if (isPassword)
    {
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
      if (_status_no_dict != null) _status_no_dict.setVisibility(View.GONE);

      boolean hasInline = (_autofill_scroll != null && _autofill_scroll.getVisibility() == View.VISIBLE);
      if (hasInline)
      {
        if (_password_autofill_scroll != null) _password_autofill_scroll.setVisibility(View.GONE);
        if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
      }
      else
      {
        if (_toolbar_open)
        {
          if (_password_autofill_scroll != null) _password_autofill_scroll.setVisibility(View.GONE);
          if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.VISIBLE);
        }
        else
        {
          if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
          if (_password_autofill_scroll != null) _password_autofill_scroll.setVisibility(View.VISIBLE);
        }
      }
      updateToggleIconState();
      return;
    }
    else
    {
      if (_password_autofill_scroll != null) _password_autofill_scroll.setVisibility(View.GONE);
    }

    if (_toolbar_open)
    {
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.VISIBLE);
      if (_status_no_dict != null) _status_no_dict.setVisibility(View.GONE);
    }
    else
    {
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.VISIBLE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.VISIBLE);
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
      if (_status_no_dict != null)
      {
        _status_no_dict.setVisibility(View.GONE);
      }
    }
    updateToggleIconState();
  }

  public boolean hasTypedText()
  {
    if (_has_suggestions)
      return true;
    if (_keyboard2 != null)
    {
      InputConnection ic = _keyboard2.getCurrentInputConnection();
      if (ic != null)
      {
        CharSequence textBefore = ic.getTextBeforeCursor(1, 0);
        if (textBefore != null && textBefore.length() > 0)
        {
          return true;
        }
        CharSequence textAfter = ic.getTextAfterCursor(1, 0);
        if (textAfter != null && textAfter.length() > 0)
        {
          return true;
        }
      }
    }
    return false;
  }

  public void onTextOrSelectionChanged()
  {
    if (_is_voice_typing_active || typodev.keyboard.voice.OfflineVoiceTypingService.isListening())
    {
      return;
    }
    boolean isPassword = (_config != null && _config.editor_config != null && _config.editor_config.is_password);
    boolean hasText = hasTypedText();
    if (isPassword)
    {
      if (!_userManuallyToggled)
      {
        _toolbar_open = false;
      }
    }
    else if (!hasText)
    {
      _toolbar_open = true;
      _userManuallyToggled = false;
    }
    else
    {
      if (!_userManuallyToggled)
      {
        _toolbar_open = false;
      }
    }
    updateMagicButtonVisibility();
    updateClipboardSuggestionChip();
    update_view_visibility(_has_suggestions);
  }

  public void updateMagicButtonVisibility()
  {
    View target = (_btn_magic_container != null) ? _btn_magic_container : _btn_magic_autocomplete;
    if (target == null) return;

    if (_is_voice_typing_active || typodev.keyboard.voice.OfflineVoiceTypingService.isListening())
    {
      target.setVisibility(View.GONE);
      return;
    }

    if (_autofill_scroll != null && _autofill_scroll.getVisibility() == View.VISIBLE)
    {
      target.setVisibility(View.GONE);
      return;
    }

    // Magic button is ONLY visible when user has typed something
    target.setVisibility(hasTypedText() ? View.VISIBLE : View.GONE);
  }

  public void set_candidates(Suggestions s)
  {
    if (_is_voice_typing_active || typodev.keyboard.voice.OfflineVoiceTypingService.isListening())
    {
      return;
    }
    int s_count = s.count;
    _has_suggestions = (s_count > 0 || s.emoji_suggestion != null);
    if (_has_suggestions)
    {
      _toolbar_open = false;
      _userManuallyToggled = false;
    }
    else if (!hasTypedText())
    {
      _toolbar_open = true;
      _userManuallyToggled = false;
    }
    for (int i = 0; i < MAX_WORD_CANDIDATES; i++)
      _items[i] = (i < s_count) ? s.suggestions[i] : null;
    _items[MAX_WORD_CANDIDATES] = s.emoji_suggestion;

    if (_status_no_dict != null)
    {
      if (s_count != 0 || has_valid_dictionary())
        _status_no_dict.setVisibility(View.GONE);
    }

    for (int i = 0; i < MAX_WORD_CANDIDATES; i++)
    {
      TextView v = _item_views[i];
      if (v == null) continue;
      if (_items[i] != null)
      {
        v.setText(_items[i]);
        v.setEllipsize(null); // Never truncate words like Knowledge with ellipsis
        boolean isAvro = s.isAvroBanglishActive();
        if (isAvro ? (i == 1) : (i == 0 && s.should_autocorrect))
        {
          v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }
        else
        {
          v.setTypeface(android.graphics.Typeface.DEFAULT);
        }
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)v.getLayoutParams();
        if (lp != null)
        {
          lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
          lp.weight = 0.0f;
          v.setLayoutParams(lp);
        }
        v.setVisibility(View.VISIBLE);
      }
      else
      {
        v.setVisibility(View.GONE);
      }
    }

    if (_emoji_view != null)
    {
      if (_items[MAX_WORD_CANDIDATES] != null)
      {
        _emoji_view.setText(_items[MAX_WORD_CANDIDATES]);
        _emoji_view.setVisibility(View.VISIBLE);
      }
      else
      {
        _emoji_view.setVisibility(View.GONE);
      }
    }

    if (_suggestions_scroll != null)
    {
      _suggestions_scroll.scrollTo(0, 0);
    }

    update_view_visibility(_has_suggestions);
    updateMagicButtonVisibility();
    updateClipboardSuggestionChip();

    update_lang_name_view();

    if (_dictionary_switch_button != null) _dictionary_switch_button.setVisibility(View.GONE);
    if (_lang_name_view != null) _lang_name_view.setVisibility(View.GONE);
  }

  void clear_candidates()
  {
    _has_suggestions = false;
    for (int i = 0; i < TOTAL_CANDIDATES; i++)
    {
      _items[i] = null;
      if (_item_views[i] != null)
        _item_views[i].setVisibility(View.GONE);
    }
    if (_suggestions_scroll != null)
    {
      _suggestions_scroll.scrollTo(0, 0);
    }
    boolean isPassword = (_config != null && _config.editor_config != null && _config.editor_config.is_password);
    if (!isPassword && !hasTypedText())
    {
      _toolbar_open = true;
      _userManuallyToggled = false;
    }
    else if (isPassword)
    {
      _toolbar_open = false;
    }
    update_view_visibility(false);
    updateMagicButtonVisibility();
    updateClipboardSuggestionChip();
  }

  public void refresh_config(Config config)
  {
    _config = config;
    clear_candidates();
    if (_status_no_dict != null)
      _status_no_dict.setVisibility(View.GONE);
    should_show_dictionary_switch = config.should_show_dictionary_switch;
    set_sizes(config);
    applyCustomTheme(config);
    update_lang_name_view();
    updateDevButtonState(findViewById(R.id.toolbar_btn_dev));
  }

  private Drawable createThemedRippleDrawable(int bgColor, int rippleColor, float radiusPx)
  {
    GradientDrawable content = new GradientDrawable();
    content.setShape(GradientDrawable.RECTANGLE);
    content.setColor(bgColor);
    content.setCornerRadius(radiusPx);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
    {
      GradientDrawable mask = new GradientDrawable();
      mask.setShape(GradientDrawable.RECTANGLE);
      mask.setColor(Color.WHITE);
      mask.setCornerRadius(radiusPx);
      return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }
    return content;
  }

  private Drawable createThemedCircleRippleDrawable(int bgColor, int rippleColor)
  {
    GradientDrawable content = new GradientDrawable();
    content.setShape(GradientDrawable.OVAL);
    content.setColor(bgColor);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
    {
      GradientDrawable mask = new GradientDrawable();
      mask.setShape(GradientDrawable.OVAL);
      mask.setColor(Color.WHITE);
      return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask);
    }
    return content;
  }

  public void applyCustomTheme(Config config)
  {
    try
    {
      CustomThemeStore store = CustomThemeStore.instance(getContext());
      boolean isCustom = store.isCustomThemeActive(config);

      int barBg;
      int chipBg;
      int textColor;

      if (isCustom)
      {
        barBg = store.getSuggestionBg();
        chipBg = store.getSuggestionChipBg();
        textColor = store.getSuggestionTextColor();
      }
      else
      {
        TypedValue tvBg = new TypedValue();
        TypedValue tvKey = new TypedValue();
        TypedValue tvLabel = new TypedValue();
        barBg = Color.TRANSPARENT;
        if (getContext().getTheme().resolveAttribute(R.attr.colorKeyboard, tvBg, true))
        {
          barBg = tvBg.data;
        }
        chipBg = 0x1A808080;
        if (getContext().getTheme().resolveAttribute(R.attr.colorKey, tvKey, true))
        {
          chipBg = tvKey.data;
        }
        textColor = Color.WHITE;
        if (getContext().getTheme().resolveAttribute(R.attr.colorLabel, tvLabel, true))
        {
          textColor = tvLabel.data;
        }
      }

      boolean isLight = CustomThemeStore.isColorLight(barBg != Color.TRANSPARENT ? barBg : (isCustom ? store.getKeyboardBg() : chipBg));
      int rippleColor = isLight ? 0x22000000 : 0x26FFFFFF;

      // Set Toolbar / Candidates background
      if (isCustom && store.hasGradient())
      {
        setBackgroundColor(Color.TRANSPARENT);
      }
      else if (isCustom)
      {
        setBackgroundColor(barBg);
      }
      else
      {
        setBackgroundColor(Color.TRANSPARENT);
      }

      float cornerRadius = TypedValue.applyDimension(
          TypedValue.COMPLEX_UNIT_DIP, 7, getResources().getDisplayMetrics());
      float buttonRadius = TypedValue.applyDimension(
          TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());

      // Suggestion word candidates
      for (int i = 0; i < MAX_WORD_CANDIDATES; i++)
      {
        if (_item_views[i] != null)
        {
          _item_views[i].setTextColor(textColor);
          _item_views[i].setBackground(createThemedRippleDrawable(chipBg, rippleColor, cornerRadius));
        }
      }
      if (_emoji_view != null)
      {
        _emoji_view.setTextColor(textColor);
        _emoji_view.setBackground(createThemedRippleDrawable(chipBg, rippleColor, cornerRadius));
      }

      // Toolbar toggle button
      if (_btn_toolbar_toggle != null)
      {
        updateToggleIconState();
      }

      // Rebuild & theme toolbar buttons
      rebuildToolbarButtons();

      // Magic container / button
      if (_btn_magic_container != null)
      {
        _btn_magic_container.setBackground(createThemedCircleRippleDrawable(chipBg, rippleColor));
      }
      if (_btn_magic_autocomplete instanceof ImageView)
      {
        ((ImageView)_btn_magic_autocomplete).setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
      }
      else if (_btn_magic_autocomplete instanceof TextView)
      {
        ((TextView)_btn_magic_autocomplete).setTextColor(textColor);
      }

      // Voice punctuation container & checkbox
      if (_btn_voice_punct_container != null)
      {
        _btn_voice_punct_container.setBackground(createThemedRippleDrawable(chipBg, rippleColor, buttonRadius));
      }
      if (_cb_voice_punctuation != null)
      {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
          _cb_voice_punctuation.setButtonTintList(ColorStateList.valueOf(textColor));
        }
      }
      TextView tvVoicePunct = (TextView)findViewById(R.id.tv_voice_punct_label);
      if (tvVoicePunct != null)
      {
        tvVoicePunct.setTextColor(textColor);
      }

      // Voice live container icon & text
      View ivVoiceIcon = findViewById(R.id.tv_voice_live_icon);
      if (ivVoiceIcon instanceof ImageView)
      {
        ((ImageView)ivVoiceIcon).setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
      }
      if (_tv_voice_live_text != null)
      {
        _tv_voice_live_text.setTextColor(textColor);
      }

      // Voice Mic button & container
      if (_btn_candidate_mic_container != null)
      {
        _btn_candidate_mic_container.setBackground(createThemedCircleRippleDrawable(chipBg, rippleColor));
      }
      if (_btn_candidate_mic instanceof ImageView)
      {
        ((ImageView)_btn_candidate_mic).setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
      }

      // Password Autofill chips
      View btnAutofillChip = findViewById(R.id.btn_autofill_chip);
      if (btnAutofillChip != null)
      {
        int accentColor = 0xFF2563EB;
        btnAutofillChip.setBackground(createThemedRippleDrawable(accentColor, 0x33FFFFFF, buttonRadius));
      }
      View btnGeneratePwdChip = findViewById(R.id.btn_generate_pwd_chip);
      if (btnGeneratePwdChip != null)
      {
        btnGeneratePwdChip.setBackground(createThemedRippleDrawable(chipBg, rippleColor, buttonRadius));
        ImageView icGen = btnGeneratePwdChip.findViewById(R.id.ic_generate_pwd_chip);
        if (icGen != null)
        {
          icGen.setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
        }
        if (btnGeneratePwdChip instanceof ViewGroup)
        {
          for (int c = 0; c < ((ViewGroup)btnGeneratePwdChip).getChildCount(); c++)
          {
            View child = ((ViewGroup)btnGeneratePwdChip).getChildAt(c);
            if (child instanceof TextView) ((TextView)child).setTextColor(textColor);
          }
        }
      }

      // Clipboard suggestion chip
      if (_clipboard_suggestion_chip != null)
      {
        _clipboard_suggestion_chip.setBackground(createThemedRippleDrawable(chipBg, rippleColor, buttonRadius));
      }
      if (_clipboard_suggestion_icon != null)
      {
        _clipboard_suggestion_icon.setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
      }
      if (_clipboard_suggestion_text != null)
      {
        _clipboard_suggestion_text.setTextColor(textColor);
      }
      if (_clipboard_suggestion_close instanceof TextView)
      {
        ((TextView)_clipboard_suggestion_close).setTextColor((textColor & 0x00FFFFFF) | 0x99000000);
      }

      // Ensure translate button highlights correctly if translate bar is currently open
      updateTranslateButtonState();
    }
    catch (Throwable ignored) {}
  }

  private void update_lang_name_view()
  {
    if (_lang_name_view != null && _config != null)
    {
      String shortName = _config.current_dictionary_short_name;
      if (shortName != null && !shortName.isEmpty())
      {
        _lang_name_view.setText(shortName);
      }
      else if (_config.current_dictionary_name != null)
      {
        _lang_name_view.setText(_config.current_dictionary_name);
      }
    }
  }


  /** Set the height of the suggestion row and the text size. */
  void set_sizes(Config config)
  {
    // Make the candidates view compact and snug, matching text height like modern keyboards (~36-38dp)
    float base_row_height = config.keyboard_rows_height_pixels * (1 - config.key_vertical_margin);
    int target_height = Math.round(base_row_height * 0.70f);
    int min_height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, getResources().getDisplayMetrics());
    int max_height = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
    target_height = Math.max(min_height, Math.min(target_height, max_height));

    ViewGroup.MarginLayoutParams p =
      (ViewGroup.MarginLayoutParams)getLayoutParams();
    p.height = target_height;
    setLayoutParams(p);

    // Suggestion chips text size: crisp, clean 14.5sp font size
    float text_size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14.5f, getResources().getDisplayMetrics());
    for (int i = 0; i < MAX_WORD_CANDIDATES; i++)
    {
      TextView v = _item_views[i];
      if (v == null) continue;
      v.setTextSize(TypedValue.COMPLEX_UNIT_PX, text_size);
    }
    if (_emoji_view != null)
    {
      _emoji_view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
    }
  }

  private void setupCandidateTouch(final TextView v, final int index)
  {
    final int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    final int longPressTimeout = ViewConfiguration.getLongPressTimeout();

    v.setOnTouchListener(new View.OnTouchListener()
    {
      private float downX, downY;
      private boolean longPressTriggered = false;
      private boolean isMoved = false;
      private final Runnable longPressRunnable = new Runnable()
      {
        @Override
        public void run()
        {
          String word = _items[index];
          if (word == null || word.trim().isEmpty()) return;

          longPressTriggered = true;
          _isDraggingSuggestion = true;
          _draggedIndex = index;
          _draggedView = v;
          _draggedWord = word;

          try
          {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
          }
          catch (Throwable ignored) {}

          if (_suggestions_scroll != null)
          {
            _suggestions_scroll.requestDisallowInterceptTouchEvent(true);
          }

          v.setAlpha(0.25f);

          if (_removalOverlay == null)
          {
            _removalOverlay = new SuggestionRemovalOverlay(getContext());
          }
          _removalOverlay.show(_keyboard2, CandidatesView.this, word, downX, downY);
        }
      };

      @Override
      public boolean onTouch(View view, MotionEvent event)
      {
        switch (event.getActionMasked())
        {
          case MotionEvent.ACTION_DOWN:
            downX = event.getRawX();
            downY = event.getRawY();
            longPressTriggered = false;
            isMoved = false;
            v.postDelayed(longPressRunnable, longPressTimeout);
            return true;

          case MotionEvent.ACTION_MOVE:
            if (_isDraggingSuggestion && _draggedIndex == index)
            {
              if (_removalOverlay != null)
              {
                _removalOverlay.updateDrag(event.getRawX(), event.getRawY());
              }
              return true;
            }
            else
            {
              float dx = event.getRawX() - downX;
              float dy = event.getRawY() - downY;
              if (Math.hypot(dx, dy) > touchSlop)
              {
                isMoved = true;
                v.removeCallbacks(longPressRunnable);
              }
            }
            break;

          case MotionEvent.ACTION_UP:
            v.removeCallbacks(longPressRunnable);
            if (_isDraggingSuggestion && _draggedIndex == index)
            {
              finishSuggestionDrag(event.getRawX(), event.getRawY());
              return true;
            }
            else if (!longPressTriggered && !isMoved)
            {
              if (index >= 0 && index < _items.length)
              {
                String it = _items[index];
                if (it != null)
                {
                  try
                  {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                  }
                  catch (Throwable ignored) {}
                  safeEnterSuggestion(it);
                }
              }
              return true;
            }
            break;

          case MotionEvent.ACTION_CANCEL:
            v.removeCallbacks(longPressRunnable);
            if (_isDraggingSuggestion && _draggedIndex == index)
            {
              cancelSuggestionDrag();
              return true;
            }
            break;
        }
        return false;
      }
    });
  }

  private void finishSuggestionDrag(float rawX, float rawY)
  {
    boolean removed = false;
    String removedWord = _draggedWord;
    int removedIndex = _draggedIndex;

    if (_removalOverlay != null)
    {
      removed = _removalOverlay.endDrag(rawX, rawY);
      _removalOverlay = null;
    }

    if (_draggedView != null)
    {
      _draggedView.setAlpha(1.0f);
      _draggedView = null;
    }

    if (_suggestions_scroll != null)
    {
      _suggestions_scroll.requestDisallowInterceptTouchEvent(false);
    }
    _isDraggingSuggestion = false;
    _draggedIndex = -1;
    _draggedWord = null;

    if (removed && removedWord != null)
    {
      try
      {
        performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM);
      }
      catch (Throwable ignored) {}

      BlockedSuggestionsStore.instance(getContext()).blockWord(removedWord);

      android.widget.Toast.makeText(getContext(), "Remove suggestion: \"" + removedWord + "\"", android.widget.Toast.LENGTH_SHORT).show();

      // Immediate in-place UI update so user sees it disappear instantly
      if (removedIndex >= 0 && removedIndex < MAX_WORD_CANDIDATES)
      {
        for (int k = removedIndex; k < MAX_WORD_CANDIDATES - 1; k++)
        {
          _items[k] = _items[k + 1];
        }
        _items[MAX_WORD_CANDIDATES - 1] = null;
        for (int k = 0; k < MAX_WORD_CANDIDATES; k++)
        {
          TextView iv = _item_views[k];
          if (iv != null)
          {
            if (_items[k] != null)
            {
              iv.setText(_items[k]);
              iv.setVisibility(View.VISIBLE);
            }
            else
            {
              iv.setVisibility(View.GONE);
            }
          }
        }
      }
      else if (removedIndex == MAX_WORD_CANDIDATES)
      {
        _items[MAX_WORD_CANDIDATES] = null;
        if (_emoji_view != null) _emoji_view.setVisibility(View.GONE);
      }

      // Re-query suggestions so dictionary & predictions update seamlessly
      if (_keyboard2 != null && _keyboard2.getKeyEventHandler() != null)
      {
        _keyboard2.getKeyEventHandler().dictionary_changed();
      }
    }
  }

  private void cancelSuggestionDrag()
  {
    if (_removalOverlay != null)
    {
      _removalOverlay.dismiss();
      _removalOverlay = null;
    }
    if (_draggedView != null)
    {
      _draggedView.setAlpha(1.0f);
      _draggedView = null;
    }
    if (_suggestions_scroll != null)
    {
      _suggestions_scroll.requestDisallowInterceptTouchEvent(false);
    }
    _isDraggingSuggestion = false;
    _draggedIndex = -1;
    _draggedWord = null;
  }

  public void updateClipboardSuggestionChip()
  {
    if (_clipboard_suggestion_container == null) return;

    if (_is_voice_typing_active || hasTypedText() || (_autofill_scroll != null && _autofill_scroll.getVisibility() == View.VISIBLE))
    {
      _clipboard_suggestion_container.setVisibility(View.GONE);
      return;
    }

    String recentClip = typodev.keyboard.ClipboardHistoryService.getRecentUnpastedClip();
    if (recentClip != null && !recentClip.trim().isEmpty())
    {
      _activeClipboardSuggestion = recentClip.trim();
      typodev.keyboard.clipboard.ClipboardItem.Category cat =
          typodev.keyboard.clipboard.ClipboardItem.detectCategory(_activeClipboardSuggestion);
      String badge = (cat != typodev.keyboard.clipboard.ClipboardItem.Category.TEXT) ? (cat.label + ": ") : "";

      String display = _activeClipboardSuggestion.replace('\n', ' ').replace('\r', ' ').trim();
      if (display.length() > 22)
      {
        display = display.substring(0, 22) + "…";
      }
      if (_clipboard_suggestion_text != null)
      {
        _clipboard_suggestion_text.setText(badge + display);
      }
      _clipboard_suggestion_container.setVisibility(View.VISIBLE);
    }
    else
    {
      _activeClipboardSuggestion = null;
      _clipboard_suggestion_container.setVisibility(View.GONE);
    }
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility)
  {
    super.onWindowVisibilityChanged(visibility);
    if (visibility == View.VISIBLE)
    {
      updateClipboardSuggestionChip();
    }
  }

  private void setMagicLoading(boolean loading)
  {
    if (_btn_magic_autocomplete != null)
    {
      _btn_magic_autocomplete.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
      _btn_magic_autocomplete.setEnabled(!loading);
    }
    if (_magic_progress != null)
    {
      if (loading)
      {
        _magic_progress.setVisibility(View.VISIBLE);
        android.view.animation.RotateAnimation spin = new android.view.animation.RotateAnimation(
            0f, 360f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
        spin.setDuration(750);
        spin.setRepeatCount(android.view.animation.Animation.INFINITE);
        spin.setInterpolator(new android.view.animation.LinearInterpolator());
        _magic_progress.startAnimation(spin);
      }
      else
      {
        _magic_progress.clearAnimation();
        _magic_progress.setVisibility(View.GONE);
      }
    }
    if (_btn_magic_container != null)
    {
      _btn_magic_container.setEnabled(!loading);
    }
  }

  public void trigger_magic_autocomplete()
  {
    android.view.inputmethod.InputConnection ic = (_keyboard2 != null)
        ? _keyboard2.getCurrentInputConnection() : null;
    if (ic == null) return;

    CharSequence before = ic.getTextBeforeCursor(350, 0);
    final String text = (before != null) ? before.toString().trim() : "";
    if (text.isEmpty())
    {
      android.widget.Toast.makeText(getContext(), "🪄 আগে কিছু টাইপ করুন (অটো-কমপ্লিট করার জন্য)", android.widget.Toast.LENGTH_SHORT).show();
      return;
    }

    setMagicLoading(true);

    // If typing in Avro/Banglish mode, first check if user is typing a Banglish word to fix
    boolean isAvro = (_keyboard2 != null && _keyboard2.getKeyEventHandler() != null && _keyboard2.getKeyEventHandler().isAvroActive());
    String currentLatin = null;
    if (_keyboard2 != null && _keyboard2.getKeyEventHandler() != null && _keyboard2.getKeyEventHandler().isAvroComposingActive())
    {
      currentLatin = _keyboard2.getKeyEventHandler().getAvroComposingLatin();
    }
    if (currentLatin == null || currentLatin.isEmpty())
    {
      int lastSpace = Math.max(text.lastIndexOf(' '), text.lastIndexOf('\n'));
      String lastWord = (lastSpace >= 0) ? text.substring(lastSpace + 1).trim() : text.trim();
      if (!lastWord.isEmpty() && BanglishEngine.isLatinOnly(lastWord))
      {
        currentLatin = lastWord;
      }
    }

    if (isAvro && currentLatin != null && currentLatin.length() >= 2)
    {
      final String wordToTransliterate = currentLatin;
      BanglishOnlineTransliterator.fetchTransliteration(wordToTransliterate, getContext(), new BanglishOnlineTransliterator.Callback()
      {
        @Override
        public void onSuccess(final String query, final List<String> results)
        {
          post(new Runnable()
          {
            @Override
            public void run()
            {
              setMagicLoading(false);
              if (results != null && !results.isEmpty())
              {
                display_custom_completions(results);
              }
              else
              {
                android.widget.Toast.makeText(getContext(), "কোনো বাংলা শব্দ পাওয়া যায়নি", android.widget.Toast.LENGTH_SHORT).show();
              }
            }
          });
        }

        @Override
        public void onError(final String query, final String error)
        {
          post(new Runnable()
          {
            @Override
            public void run()
            {
              setMagicLoading(false);
              android.widget.Toast.makeText(getContext(), "AI: " + error, android.widget.Toast.LENGTH_SHORT).show();
            }
          });
        }
      });
      return;
    }

    String apiKey = typodev.keyboard.ai.GeminiAiService.getApiKey(getContext());
    if (apiKey != null && !apiKey.isEmpty())
    {
      typodev.keyboard.ai.GeminiAiService.processAutocomplete(getContext(), text, new typodev.keyboard.ai.GeminiAiService.AiCallback()
      {
        @Override
        public void onSuccess(final String resultText)
        {
          post(new Runnable()
          {
            @Override
            public void run()
            {
              setMagicLoading(false);
              apply_ai_completions(resultText, text);
            }
          });
        }

        @Override
        public void onError(final String errorMessage)
        {
          post(new Runnable()
          {
            @Override
            public void run()
            {
              setMagicLoading(false);
              apply_offline_completions(text);
              android.widget.Toast.makeText(getContext(), "AI: " + errorMessage + " (অফলাইন সাজেশন দেখানো হয়েছে)", android.widget.Toast.LENGTH_SHORT).show();
            }
          });
        }
      });
    }
    else
    {
      setMagicLoading(false);
      apply_offline_completions(text);
      android.widget.Toast.makeText(getContext(), "🪄 অফলাইন কমপ্লিশন দেখানো হয়েছে। সম্পূর্ণ AI পেতে সেটিংসে Gemini API Key দিন।", android.widget.Toast.LENGTH_LONG).show();
    }
  }

  private void apply_ai_completions(String resultText, String originalText)
  {
    if (resultText == null || resultText.trim().isEmpty())
    {
      apply_offline_completions(originalText);
      return;
    }
    String[] lines = resultText.split("\n");
    List<String> list = new ArrayList<>();
    for (String line : lines)
    {
      String clean = line.replaceAll("^[0-9]+[.)\\-•*]\\s*", "")
                         .replace("\"", "").replace("'", "").trim();
      if (clean.length() > 0)
      {
        if (clean.toLowerCase(Locale.ROOT).startsWith(originalText.toLowerCase(Locale.ROOT)))
        {
          clean = clean.substring(originalText.length()).trim();
        }
        if (!clean.isEmpty())
        {
          list.add("🪄 " + clean);
        }
      }
    }
    if (list.isEmpty())
    {
      apply_offline_completions(originalText);
      return;
    }
    display_custom_completions(list);
  }

  private void apply_offline_completions(String text)
  {
    typodev.keyboard.suggestions.NextWordPredictor predictor =
        typodev.keyboard.suggestions.NextWordPredictor.instance(getContext());
    List<String> preds = predictor.predictFromSentence(text, 6);
    if (preds != null && !preds.isEmpty())
    {
      List<String> list = new ArrayList<>();
      for (String p : preds)
      {
        list.add("🪄 " + p);
      }
      display_custom_completions(list);
    }
  }

  public void display_custom_completions(List<String> list)
  {
    clear_candidates();
    int count = Math.min(list.size(), MAX_WORD_CANDIDATES);
    for (int i = 0; i < count; i++)
    {
      _items[i] = list.get(i);
    }
    _has_suggestions = (count > 0);
    _toolbar_open = false;

    for (int i = 0; i < MAX_WORD_CANDIDATES; i++)
    {
      TextView v = _item_views[i];
      if (v == null) continue;
      if (_items[i] != null)
      {
        v.setText(_items[i]);
        v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)v.getLayoutParams();
        if (lp != null)
        {
          lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
          lp.weight = 0.0f;
          v.setLayoutParams(lp);
        }
        v.setVisibility(View.VISIBLE);
      }
      else
      {
        v.setVisibility(View.GONE);
      }
    }
    if (_suggestions_scroll != null)
    {
      _suggestions_scroll.scrollTo(0, 0);
    }
    update_view_visibility(_has_suggestions);
  }

  void inflate_status_no_dict(Config config)
  {
    if (_status_no_dict != null)
      _status_no_dict.setVisibility(View.GONE);
  }

  void setup_dictionary_switch_button()
  {
    _dictionary_switch_button = findViewById(R.id.dictionary_switch);
    if (_dictionary_switch_button != null)
    {
      _dictionary_switch_button.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View _v)
        {
          safeSendKey(KeyValue.getKeyByName("change_dictionary"));
        }
      });
    }
  }

  /** Returns true if the editor is a known code/text editor or IDE environment. */
  public static boolean isCodeEditor(EditorInfo info)
  {
    if (info == null) return false;
    if (info.packageName != null)
    {
      String pkg = info.packageName.toLowerCase(Locale.ROOT);
      if (pkg.contains("acode")
          || pkg.contains("foxdebug")
          || pkg.contains("spck")
          || pkg.contains("dcoder")
          || pkg.contains("trebedit")
          || pkg.contains("aide")
          || pkg.contains("anacode")
          || pkg.contains("rhmsoft.edit")
          || pkg.contains("godotengine")
          || pkg.contains("droidedit")
          || pkg.contains("codeeditor")
          || pkg.contains("codestudio")
          || pkg.contains("neovim")
          || pkg.contains("vim")
          || pkg.contains("emacs")
          || pkg.contains("jecelyin")
          || pkg.contains("bin.mt.plus")
          || pkg.contains("compiler")
          || pkg.contains(".ide")
          || pkg.contains("ide.")
          || pkg.endsWith(".ide")
          || (pkg.contains("editor") && !pkg.contains("photo") && !pkg.contains("video") && !pkg.contains("image"))
          || (pkg.contains("code") && !pkg.contains("passcode") && !pkg.contains("barcode") && !pkg.contains("qrcode")))
      {
        return true;
      }
    }
    int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
    if (variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
    {
      return true;
    }
    return false;
  }

  /** Whether the candidates view should be shown for a given editor. */
  public static boolean should_show(EditorInfo info)
  {
    return should_show(info, false, false);
  }

  public static boolean should_show(EditorInfo info, boolean showInTerminals)
  {
    return should_show(info, showInTerminals, false);
  }

  public static boolean isPasswordField(EditorInfo info)
  {
    if (info == null) return false;
    int inputClass = info.inputType & InputType.TYPE_MASK_CLASS;
    int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
    if (inputClass == InputType.TYPE_CLASS_TEXT)
    {
      return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
          || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
          || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
    }
    else if (inputClass == InputType.TYPE_CLASS_NUMBER)
    {
      return variation == 16; // InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }
    return false;
  }

  public static boolean should_show(EditorInfo info, boolean showInTerminals, boolean developerMode)
  {
    if (info == null) return false;

    // Code editors (Acode, Spck, QuickEdit, etc.) must always show the toolbar & candidate bar
    if (isCodeEditor(info))
    {
      return true;
    }

    // Password fields: show candidate strip so password autofill and inline suggestions work!
    if (isPasswordField(info))
    {
      return true;
    }

    int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
    int flags = info.inputType & InputType.TYPE_MASK_FLAGS;
    int inputClass = info.inputType & InputType.TYPE_MASK_CLASS;

    // Check if this editor is a terminal emulator or TYPE_NULL shell
    boolean isTerminal = (inputClass == InputType.TYPE_NULL)
        || (info.packageName != null && (
            info.packageName.toLowerCase(Locale.ROOT).contains("termux")
            || info.packageName.toLowerCase(Locale.ROOT).contains("terminal")
            || info.packageName.toLowerCase(Locale.ROOT).contains("connectbot")
            || info.packageName.toLowerCase(Locale.ROOT).contains("juicessh")
            || info.packageName.toLowerCase(Locale.ROOT).contains("termius")
        ));

    if (isTerminal)
    {
      return showInTerminals || developerMode;
    }

    switch (inputClass)
    {
      case InputType.TYPE_CLASS_TEXT:
        return true;
      case InputType.TYPE_CLASS_NUMBER:
        return false;
      default: return false;
    }
  }

  public void onVoiceListeningStarted()
  {
    _is_voice_typing_active = true;
    _toolbar_open = false;

    if (_voice_live_container != null)
    {
      _voice_live_container.setVisibility(View.VISIBLE);
      if (_tv_voice_live_text != null)
      {
        _tv_voice_live_text.setText(getContext().getString(R.string.voice_listening_hint));
        _tv_voice_live_text.setAlpha(0.65f);
      }
    }

    if (_cb_voice_punctuation != null)
    {
      _cb_voice_punctuation.setChecked(typodev.keyboard.voice.VoicePunctuationHelper.isVoiceSpokenPunctuationEnabled(getContext()));
    }
    if (_btn_voice_punct_container != null)
    {
      _btn_voice_punct_container.setVisibility(View.VISIBLE);
    }

    if (_btn_candidate_mic_container != null)
    {
      _btn_candidate_mic_container.setListening(true);
      _btn_candidate_mic_container.setVisibility(View.VISIBLE);
    }

    if (_btn_magic_container != null)
    {
      _btn_magic_container.setVisibility(View.GONE);
    }
    else if (_btn_magic_autocomplete != null)
    {
      _btn_magic_autocomplete.setVisibility(View.GONE);
    }

    if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
    if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
    if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
    if (_btn_toolbar_toggle != null) _btn_toolbar_toggle.setVisibility(View.GONE);
    if (_autofill_scroll != null) _autofill_scroll.setVisibility(View.GONE);
    if (_status_no_dict != null) _status_no_dict.setVisibility(View.GONE);
    if (_dictionary_switch_button != null) _dictionary_switch_button.setVisibility(View.GONE);
    if (_lang_name_view != null) _lang_name_view.setVisibility(View.GONE);
  }

  public void onVoiceRmsChanged(float rmsdB)
  {
    if (_btn_candidate_mic_container != null)
    {
      _btn_candidate_mic_container.onRmsChanged(rmsdB);
    }
  }

  public void onVoicePartialResult(final String text)
  {
    if (_tv_voice_live_text != null && text != null && !text.trim().isEmpty())
    {
      _tv_voice_live_text.setText(text);
      _tv_voice_live_text.setAlpha(1.0f);
    }
  }

  public void onVoiceResult(final String text)
  {
    if (_tv_voice_live_text != null && text != null && !text.trim().isEmpty())
    {
      _tv_voice_live_text.setText(text);
      _tv_voice_live_text.setAlpha(1.0f);
    }
  }

  public void onVoiceListeningStopped()
  {
    _is_voice_typing_active = false;

    if (_voice_live_container != null)
    {
      _voice_live_container.setVisibility(View.GONE);
    }

    if (_cb_voice_punctuation != null)
    {
      _cb_voice_punctuation.setChecked(typodev.keyboard.voice.VoicePunctuationHelper.isVoiceSpokenPunctuationEnabled(getContext()));
    }
    if (_btn_voice_punct_container != null)
    {
      _btn_voice_punct_container.setVisibility(View.GONE);
    }

    if (_btn_candidate_mic_container != null)
    {
      _btn_candidate_mic_container.setListening(false);
    }

    if (_btn_toolbar_toggle != null)
    {
      _btn_toolbar_toggle.setVisibility(View.VISIBLE);
    }

    updateMagicButtonVisibility();
    update_view_visibility(_has_suggestions);
  }
}
