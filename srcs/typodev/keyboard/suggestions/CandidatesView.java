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
import android.app.AlertDialog;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import typodev.keyboard.ai.SlashCommand;
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

  View _dictionary_switch_button;
  boolean should_show_dictionary_switch = false;

  TextView _lang_name_view;
  private typodev.keyboard.Keyboard2 _keyboard2;
  private ImageView _btn_toolbar_toggle;
  private HorizontalScrollView _suggestions_scroll;
  private LinearLayout _suggestions_container;
  private HorizontalScrollView _slash_scroll;
  private LinearLayout _slash_row;
  private String _slash_render_key, _dismissed_slash;
  private long _slash_session = -1;
  private SlashCommand.Input _slash_input;
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
  private long _magicRequestId = 0;

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
  private String _activeClipboardImage = null;
  private final Runnable _clipTimeout = () -> dismissClipboardPreview();
  private final Runnable _imageSuggestionChanged = () -> updateClipboardSuggestionChip();
  private final android.content.ClipboardManager.OnPrimaryClipChangedListener _previewClipChanged =
      () -> post(() -> updateClipboardSuggestionChip());

  private View _password_autofill_scroll;
  private View _btn_autofill_chip;
  private View _btn_generate_pwd_chip;
  private View _toolbar_btn_autofill;
  private View _ai_comments_scroll;
  private LinearLayout _ai_comments_container;
  private byte[] _lastAiImageBytes = null;
  private String _lastAiPostText = null;
  private String _lastAiTone = "স্বাভাবিক";
  private String _lastInsertedAiComment = null;
  private int _selectedAiCommentIndex = -1;
  private List<String> _currentAiComments = null;
  private AlertDialog _fullCommentsDialog = null;

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
    _slash_scroll = (HorizontalScrollView)findViewById(R.id.slash_commands_scroll);
    _slash_row = (LinearLayout)findViewById(R.id.slash_commands_row);
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
    _ai_comments_scroll = findViewById(R.id.ai_comments_scroll);
    _ai_comments_container = (LinearLayout)findViewById(R.id.ai_comments_container);

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
      try { _cb_voice_punctuation.setChecked(typodev.keyboard.voice.VoicePunctuationHelper.isVoiceSpokenPunctuationEnabled(getContext())); } catch (Throwable ignored) {}
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
          if (_activeClipboardImage != null && _keyboard2 != null) {
            _keyboard2.pasteImageContent(android.net.Uri.parse(_activeClipboardImage),
                typodev.keyboard.clipboard.ClipboardImageStore.get(getContext()).mimeType(_activeClipboardImage));
            dismissClipboardPreview();
            return;
          }
          if (_activeClipboardSuggestion != null && _keyboard2 != null)
          {
            InputConnection ic = _keyboard2.getCurrentInputConnection();
            if (ic == null || !ic.commitText(_activeClipboardSuggestion, 1)) return;
            try { v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP); }
            catch (Throwable ignored) {}
            dismissClipboardPreview();
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
          dismissClipboardPreview();
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
        try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
        if (_keyboard2 != null)
        {
          _keyboard2.showMagicTools();
        }
        else
        {
          trigger_magic_autocomplete();
        }
      }
    };
    View.OnLongClickListener magicLongClick = new View.OnLongClickListener()
    {
      @Override
      public boolean onLongClick(View v)
      {
        try { v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Throwable ignored) {}
        trigger_magic_autocomplete();
        return true;
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
          if (_ai_comments_scroll != null && _ai_comments_scroll.getVisibility() == View.VISIBLE)
          {
            hideAiCommentsStrip();
            return;
          }
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
      else if ("focus".equals(tool.id) && ((_keyboard2 != null && _keyboard2.isFocusModeActive()) || (activeCfg != null && activeCfg.focus_mode)))
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
    typodev.keyboard.clipboard.ClipboardImageStore.get(getContext()).addListener(_imageSuggestionChanged);
    android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
    if (clipboard != null) clipboard.addPrimaryClipChangedListener(_previewClipChanged);
    if (_tools_manager != null) _tools_manager.addListener(_toolsChangedListener);
    rebuildToolbarButtons();
  }

  @Override
  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    removeCallbacks(_clipTimeout);
    typodev.keyboard.clipboard.ClipboardImageStore.get(getContext()).removeListener(_imageSuggestionChanged);
    android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
    if (clipboard != null) clipboard.removePrimaryClipChangedListener(_previewClipChanged);
    cancelMagicAutocomplete();
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
    if (_slash_scroll != null) _slash_scroll.setVisibility(View.GONE);
    if (_dictionary_switch_button != null) _dictionary_switch_button.setVisibility(View.GONE);
    if (_lang_name_view != null) _lang_name_view.setVisibility(View.GONE);

    if (_is_voice_typing_active)
    {
      if (!typodev.keyboard.voice.OfflineVoiceTypingService.isListening())
      {
        _is_voice_typing_active = false;
      }
      else
      {
        if (_clipboard_suggestion_container != null) _clipboard_suggestion_container.setVisibility(View.GONE);
        if (_ai_comments_scroll != null) _ai_comments_scroll.setVisibility(View.GONE);
        if (_btn_toolbar_toggle != null) _btn_toolbar_toggle.setVisibility(View.GONE);
        if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
        if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
        if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
        if (_voice_live_container != null) _voice_live_container.setVisibility(View.VISIBLE);
        if (_autofill_scroll != null) _autofill_scroll.setVisibility(View.GONE);
        if (_password_autofill_scroll != null) _password_autofill_scroll.setVisibility(View.GONE);
        if (_btn_magic_container != null) _btn_magic_container.setVisibility(View.GONE);
        else if (_btn_magic_autocomplete != null) _btn_magic_autocomplete.setVisibility(View.GONE);
        if (_btn_voice_punct_container != null) _btn_voice_punct_container.setVisibility(View.VISIBLE);
        if (_btn_candidate_mic_container != null) _btn_candidate_mic_container.setVisibility(View.VISIBLE);
        else if (_btn_candidate_mic != null) _btn_candidate_mic.setVisibility(View.VISIBLE);
        return;
      }
    }
    else
    {
      if (_voice_live_container != null) _voice_live_container.setVisibility(View.GONE);
      if (_btn_voice_punct_container != null) _btn_voice_punct_container.setVisibility(View.GONE);
    }

    if (_clipboard_suggestion_container != null && _clipboard_suggestion_container.getVisibility() == View.VISIBLE) {
      hideOtherClipboardToolbarViews();
      return;
    }

    if (_btn_toolbar_toggle != null)
    {
      _btn_toolbar_toggle.setVisibility(View.VISIBLE);
    }

    if (_btn_candidate_mic_container != null) _btn_candidate_mic_container.setVisibility(View.VISIBLE);
    else if (_btn_candidate_mic != null) _btn_candidate_mic.setVisibility(View.VISIBLE);
    if (_btn_candidate_mic != null) _btn_candidate_mic.setVisibility(View.VISIBLE);

    updateMagicButtonVisibility();

    if (_ai_comments_scroll != null && _ai_comments_scroll.getVisibility() == View.VISIBLE)
    {
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
      if (_autofill_scroll != null) _autofill_scroll.setVisibility(View.GONE);
      if (_password_autofill_scroll != null) _password_autofill_scroll.setVisibility(View.GONE);
      updateToggleIconState();
      return;
    }

    // Suggestions and autofill can arrive asynchronously while the grid is open.
    // Keep the pinned toolbar and its Back button visible until the user leaves.
    if (_keyboard2 != null && _keyboard2.isToolsPaneVisible())
    {
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.VISIBLE);
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
      if (_autofill_scroll != null) _autofill_scroll.setVisibility(View.GONE);
      if (_password_autofill_scroll != null) _password_autofill_scroll.setVisibility(View.GONE);
      if (_btn_magic_container != null) _btn_magic_container.setVisibility(View.GONE);
      updateToggleIconState();
      return;
    }

    boolean isPassword = (_config != null && _config.editor_config != null && _config.editor_config.is_password);
    if (isPassword)
    {
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);

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

    if (showSlashCommands())
    {
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
      if (_clipboard_suggestion_container != null) _clipboard_suggestion_container.setVisibility(View.GONE);
      return;
    }

    if (_toolbar_open)
    {
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.VISIBLE);
    }
    else
    {
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.VISIBLE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.VISIBLE);
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
    }
    updateToggleIconState();
  }

  public boolean hasTypedText()
  {
    if (_has_suggestions)
      return true;
    if (_keyboard2 != null)
    {
      try
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
      catch (Throwable ignored) {}
    }
    return false;
  }

  public void onTextOrSelectionChanged()
  {
    if (_keyboard2 != null) _keyboard2.updateSlashCandidateVisibility();
    if (_is_voice_typing_active)
    {
      if (!typodev.keyboard.voice.OfflineVoiceTypingService.isListening())
      {
        onVoiceListeningStopped();
      }
      else
      {
        return;
      }
    }
    else if (typodev.keyboard.voice.OfflineVoiceTypingService.isListening())
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

  private boolean showSlashCommands()
  {
    if (_keyboard2 == null || _slash_scroll == null || _slash_row == null
        || (_autofill_scroll != null && _autofill_scroll.getVisibility() == View.VISIBLE)) return false;
    _slash_input = _keyboard2.getSlashCommandInput();
    long session = _keyboard2.getInputSessionId();
    if (session != _slash_session) {
      _slash_session = session; _dismissed_slash = null; _slash_render_key = null;
    }
    if (_slash_input == null) { _dismissed_slash = null; _slash_render_key = null; return false; }
    if (_slash_input.raw.equals(_dismissed_slash)) return false;
    _dismissed_slash = null;
    String key = _slash_input.prefix + ":" + !_slash_input.argument.isEmpty()
        + ":" + _keyboard2.getColorKey() + ":" + _keyboard2.getColorLabel();
    if (!key.equals(_slash_render_key)) {
      _slash_render_key = key;
      _slash_row.removeAllViews();
      _slash_scroll.scrollTo(0, 0);
      for (SlashCommand command : _slash_input.suggestions()) {
        String label = command.token();
        if (!_slash_input.argument.isEmpty()) label = getContext().getString(
            command == SlashCommand.REMEMBER ? R.string.slash_save : R.string.slash_generate, label);
        addSlashChip(label, () -> _keyboard2.chooseSlashCommand(_slash_input, command, _slash_session));
      }
      if (_slash_input.command == SlashCommand.REMEMBER)
        addSlashChip(getContext().getString(R.string.slash_saved_notes), () -> _keyboard2.showSavedSlashNotes());
      TextView close = addSlashChip("×", () -> {
        _dismissed_slash = _slash_input.raw;
        update_view_visibility(_has_suggestions);
      });
      close.setContentDescription(getContext().getString(R.string.slash_dismiss));
    }
    _slash_scroll.setVisibility(View.VISIBLE);
    return true;
  }

  private TextView addSlashChip(String text, Runnable action)
  {
    TextView chip = new TextView(getContext());
    float density = getResources().getDisplayMetrics().density;
    chip.setText(text); chip.setTextSize(13); chip.setSingleLine(true);
    chip.setTextColor(_keyboard2.getColorLabel());
    chip.setGravity(Gravity.CENTER);
    chip.setPadding(Math.round(10 * density), 0, Math.round(10 * density), 0);
    chip.setBackground(createThemedRippleDrawable(_keyboard2.getColorKey(), _keyboard2.getColorKeyActivated(), 16 * density));
    chip.setFocusable(true);
    chip.setOnClickListener(v -> action.run());
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -1);
    params.setMarginEnd(Math.round(4 * density));
    _slash_row.addView(chip, params);
    return chip;
  }

  public void updateMagicButtonVisibility()
  {
    View target = (_btn_magic_container != null) ? _btn_magic_container : _btn_magic_autocomplete;
    if (target == null) return;

    if (_is_voice_typing_active)
    {
      if (!typodev.keyboard.voice.OfflineVoiceTypingService.isListening())
      {
        _is_voice_typing_active = false;
        if (_voice_live_container != null) _voice_live_container.setVisibility(View.GONE);
        if (_tv_voice_live_text != null) _tv_voice_live_text.setText("");
        if (_btn_voice_punct_container != null) _btn_voice_punct_container.setVisibility(View.GONE);
        if (_btn_candidate_mic_container != null) _btn_candidate_mic_container.setListening(false);
        if (_btn_toolbar_toggle != null) _btn_toolbar_toggle.setVisibility(View.VISIBLE);
      }
      else
      {
        target.setVisibility(View.GONE);
        return;
      }
    }
    else if (typodev.keyboard.voice.OfflineVoiceTypingService.isListening())
    {
      target.setVisibility(View.GONE);
      return;
    }

    if (_autofill_scroll != null && _autofill_scroll.getVisibility() == View.VISIBLE)
    {
      target.setVisibility(View.GONE);
      return;
    }

    // Keep prompt shortcuts and their editor reachable even before typing.
    target.setVisibility(View.VISIBLE);
    if (_btn_magic_autocomplete != null)
      _btn_magic_autocomplete.setVisibility(_magic_progress != null && _magic_progress.getVisibility() == View.VISIBLE
          ? View.INVISIBLE : View.VISIBLE);
  }

  public void set_candidates(Suggestions s)
  {
    if (_is_voice_typing_active)
    {
      if (!typodev.keyboard.voice.OfflineVoiceTypingService.isListening())
      {
        onVoiceListeningStopped();
      }
      else
      {
        return;
      }
    }
    else if (typodev.keyboard.voice.OfflineVoiceTypingService.isListening())
    {
      return;
    }
    int s_count = s.count;
    _has_suggestions = (s_count > 0 || s.emoji_suggestion != null);
    if (_has_suggestions)
    {
      if (_ai_comments_scroll != null && _ai_comments_scroll.getVisibility() == View.VISIBLE)
      {
        hideAiCommentsStrip();
      }
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

    for (int i = 0; i < MAX_WORD_CANDIDATES; i++)
    {
      TextView v = _item_views[i];
      if (v == null) continue;
      if (_items[i] != null)
      {
        v.setText(_items[i]);
        v.setEllipsize(null); // Never truncate words like Knowledge with ellipsis
        boolean isAvro = s.isAvroBanglishActive();
        if (isAvro ? (i == 1) : (i == 0 && (s.should_autocorrect || s.isCalculatorActive() || s.isAlgebraActive())))
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
        if (_activeClipboardImage == null) _clipboard_suggestion_icon.setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
        else _clipboard_suggestion_icon.clearColorFilter();
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

    if (_is_voice_typing_active || (_keyboard2 != null && _keyboard2.isToolsPaneVisible())
        || (_ai_comments_scroll != null && _ai_comments_scroll.getVisibility() == View.VISIBLE)
        || (_autofill_scroll != null && _autofill_scroll.getVisibility() == View.VISIBLE)
        || (_config != null && _config.editor_config.is_password))
    {
      _clipboard_suggestion_container.setVisibility(View.GONE);
      return;
    }

    String recentClip = typodev.keyboard.ClipboardHistoryService.getRecentUnpastedClip();
    typodev.keyboard.clipboard.ClipboardImageStore images = typodev.keyboard.clipboard.ClipboardImageStore.get(getContext());
    String image = images.recentSuggestion(typodev.keyboard.ClipboardHistoryService.recentTextTimestamp());
    if (image != null) {
      typodev.keyboard.ClipboardHistoryService.markRecentClipPasted();
      boolean changed = !image.equals(_activeClipboardImage);
      _activeClipboardImage = image; _activeClipboardSuggestion = null;
      if (_clipboard_suggestion_icon != null) {
        _clipboard_suggestion_icon.clearColorFilter(); _clipboard_suggestion_icon.setImageTintList(null);
        android.net.Uri preview = images.thumbnail(image);
        android.graphics.Bitmap bitmap = preview == null ? null : android.graphics.BitmapFactory.decodeFile(preview.getPath());
        if (bitmap != null) _clipboard_suggestion_icon.setImageBitmap(bitmap);
        else _clipboard_suggestion_icon.setImageResource(R.drawable.ic_toolbar_clipboard);
        _clipboard_suggestion_icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        _clipboard_suggestion_icon.getLayoutParams().width = (int)(26 * getResources().getDisplayMetrics().density);
        _clipboard_suggestion_icon.getLayoutParams().height = (int)(26 * getResources().getDisplayMetrics().density);
        _clipboard_suggestion_icon.requestLayout();
      }
      if (_clipboard_suggestion_text != null) _clipboard_suggestion_text.setText(R.string.clipboard_suggestion_image);
      _clipboard_suggestion_container.setVisibility(View.VISIBLE);
      hideOtherClipboardToolbarViews();
      if (changed) scheduleClipboardTimeout();
      return;
    }
    if (recentClip != null && !recentClip.trim().isEmpty())
    {
      boolean changed = !recentClip.trim().equals(_activeClipboardSuggestion);
      _activeClipboardImage = null;
      _activeClipboardSuggestion = recentClip.trim();
      if (_clipboard_suggestion_icon != null) {
        _clipboard_suggestion_icon.setImageResource(R.drawable.ic_toolbar_clipboard);
        _clipboard_suggestion_icon.setColorFilter(_clipboard_suggestion_text.getCurrentTextColor());
      }
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
      hideOtherClipboardToolbarViews();
      if (changed) scheduleClipboardTimeout();
    }
    else
    {
      _activeClipboardSuggestion = null;
      _activeClipboardImage = null;
      removeCallbacks(_clipTimeout);
      _clipboard_suggestion_container.setVisibility(View.GONE);
      update_view_visibility(_has_suggestions);
    }
  }

  private void hideOtherClipboardToolbarViews() {
    View[] views = { _btn_toolbar_toggle, _toolbar_scroll, _suggestions_scroll, _suggestions_container,
        _btn_magic_container, _btn_magic_autocomplete, _btn_candidate_mic_container, _btn_candidate_mic,
        _password_autofill_scroll };
    for (View view : views) if (view != null) view.setVisibility(View.GONE);
  }
  private void scheduleClipboardTimeout() {
    removeCallbacks(_clipTimeout);
    if (isShown()) postDelayed(_clipTimeout, 8000);
  }

  public void dismissClipboardPreview() {
    if (_activeClipboardImage == null && _activeClipboardSuggestion == null) return;
    removeCallbacks(_clipTimeout);
    if (_activeClipboardImage != null) typodev.keyboard.clipboard.ClipboardImageStore.get(getContext()).dismissSuggestion(_activeClipboardImage);
    if (_activeClipboardSuggestion != null) {
      typodev.keyboard.ClipboardHistoryService.dismissClip(_activeClipboardSuggestion);
    }
    _activeClipboardImage = null; _activeClipboardSuggestion = null;
    if (_clipboard_suggestion_container != null) _clipboard_suggestion_container.setVisibility(View.GONE);
    _toolbar_open = false;
    update_view_visibility(_has_suggestions);
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility)
  {
    super.onWindowVisibilityChanged(visibility);
    if (visibility == View.VISIBLE)
    {
      updateClipboardSuggestionChip();
      if (_clipboard_suggestion_container != null && _clipboard_suggestion_container.getVisibility() == View.VISIBLE) {
        scheduleClipboardTimeout();
      }
    }
    else removeCallbacks(_clipTimeout);
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

  public void cancelMagicAutocomplete()
  {
    _magicRequestId++;
    setMagicLoading(false);
  }

  private boolean finishMagicRequest(long requestId, long sessionId,
      InputConnection connection, String originalBefore)
  {
    if (requestId != _magicRequestId) return false;
    setMagicLoading(false);
    return isAttachedToWindow() && _keyboard2 != null
        && _keyboard2.getInputSessionId() == sessionId
        && _keyboard2.getCurrentInputConnection() == connection
        && typodev.keyboard.ai.EditorTextGuard.matches(connection, originalBefore, false);
  }

  public void trigger_magic_autocomplete()
  {
    if (_keyboard2 != null) _keyboard2.startWritingTools(true);
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
      try { _cb_voice_punctuation.setChecked(typodev.keyboard.voice.VoicePunctuationHelper.isVoiceSpokenPunctuationEnabled(getContext())); } catch (Throwable ignored) {}
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
    if (_password_autofill_scroll != null) _password_autofill_scroll.setVisibility(View.GONE);
    if (_clipboard_suggestion_container != null) _clipboard_suggestion_container.setVisibility(View.GONE);
    if (_slash_scroll != null) _slash_scroll.setVisibility(View.GONE);
    if (_ai_comments_scroll != null) _ai_comments_scroll.setVisibility(View.GONE);
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

  public void onVoiceReadyForSpeech()
  {
    if (_tv_voice_live_text != null && _is_voice_typing_active)
    {
      _tv_voice_live_text.postDelayed(new Runnable()
      {
        @Override
        public void run()
        {
          if (_is_voice_typing_active && _tv_voice_live_text != null)
          {
            _tv_voice_live_text.setText(getContext().getString(R.string.voice_listening_hint));
            _tv_voice_live_text.setAlpha(0.65f);
          }
        }
      }, 700);
    }
  }

  public boolean isVoiceTypingActive()
  {
    return _is_voice_typing_active;
  }

  public void onVoiceListeningStopped()
  {
    _is_voice_typing_active = false;

    if (_voice_live_container != null)
    {
      _voice_live_container.setVisibility(View.GONE);
    }
    if (_tv_voice_live_text != null)
    {
      _tv_voice_live_text.setText("");
    }

    if (_cb_voice_punctuation != null)
    {
      try { _cb_voice_punctuation.setChecked(typodev.keyboard.voice.VoicePunctuationHelper.isVoiceSpokenPunctuationEnabled(getContext())); } catch (Throwable ignored) {}
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

  public void showAiCommentsStrip(final List<String> comments)
  {
    if (_ai_comments_container == null || _ai_comments_scroll == null) return;
    _ai_comments_container.removeAllViews();

    if (comments == null || comments.isEmpty())
    {
      hideAiCommentsStrip();
      return;
    }

    _currentAiComments = new ArrayList<>(comments);

    float density = getResources().getDisplayMetrics().density;
    int chipPadH = (int)(11 * density);
    int chipHeight = (int)(32 * density);
    int chipMarginEnd = (int)(6 * density);
    int iconSize = (int)(14 * density);
    int iconMargin = (int)(5 * density);
    int maxTextWidth = (int)(450 * density);

    android.content.res.TypedArray a = getContext().obtainStyledAttributes(new int[] { R.attr.colorLabel });
    int labelColor = a.getColor(0, Color.WHITE);
    a.recycle();

    // 1. [⛶ সব দেখুন] Full View Action Button
    LinearLayout btnExpand = new LinearLayout(getContext());
    btnExpand.setOrientation(LinearLayout.HORIZONTAL);
    btnExpand.setGravity(Gravity.CENTER_VERTICAL);
    btnExpand.setBackgroundResource(R.drawable.toolbar_chip_bg);
    btnExpand.setPadding((int)(9 * density), 0, (int)(10 * density), 0);
    btnExpand.setClickable(true);
    btnExpand.setFocusable(true);
    LinearLayout.LayoutParams lpExpand = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, chipHeight);
    lpExpand.gravity = Gravity.CENTER_VERTICAL;
    lpExpand.setMarginEnd(chipMarginEnd);
    btnExpand.setLayoutParams(lpExpand);

    ImageView ivExpand = new ImageView(getContext());
    LinearLayout.LayoutParams lpIvExpand = new LinearLayout.LayoutParams(iconSize, iconSize);
    lpIvExpand.setMarginEnd(iconMargin);
    ivExpand.setLayoutParams(lpIvExpand);
    ivExpand.setImageResource(R.drawable.ic_ai_expand);
    ivExpand.setColorFilter(Color.parseColor("#38BDF8"), PorterDuff.Mode.SRC_IN);
    btnExpand.addView(ivExpand);

    TextView tvExpand = new TextView(getContext());
    tvExpand.setText("সব দেখুন");
    tvExpand.setTextColor(Color.parseColor("#38BDF8"));
    tvExpand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
    tvExpand.setTypeface(Typeface.DEFAULT_BOLD);
    btnExpand.addView(tvExpand);

    btnExpand.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
        showFullCommentsDialog();
      }
    });
    _ai_comments_container.addView(btnExpand);

    // 2. [🔄 নতুন] Regenerate Button
    LinearLayout btnRegen = new LinearLayout(getContext());
    btnRegen.setOrientation(LinearLayout.HORIZONTAL);
    btnRegen.setGravity(Gravity.CENTER_VERTICAL);
    btnRegen.setBackgroundResource(R.drawable.toolbar_chip_bg);
    btnRegen.setPadding((int)(9 * density), 0, (int)(10 * density), 0);
    btnRegen.setClickable(true);
    btnRegen.setFocusable(true);
    LinearLayout.LayoutParams lpRegen = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, chipHeight);
    lpRegen.gravity = Gravity.CENTER_VERTICAL;
    lpRegen.setMarginEnd(chipMarginEnd);
    btnRegen.setLayoutParams(lpRegen);

    ImageView ivRegen = new ImageView(getContext());
    LinearLayout.LayoutParams lpIvRegen = new LinearLayout.LayoutParams(iconSize, iconSize);
    lpIvRegen.setMarginEnd(iconMargin);
    ivRegen.setLayoutParams(lpIvRegen);
    ivRegen.setImageResource(R.drawable.ic_ai_refresh);
    ivRegen.setColorFilter(labelColor, PorterDuff.Mode.SRC_IN);
    btnRegen.addView(ivRegen);

    TextView tvRegen = new TextView(getContext());
    tvRegen.setText("নতুন");
    tvRegen.setTextColor(labelColor);
    tvRegen.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
    btnRegen.addView(tvRegen);

    btnRegen.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
        regenerateAiComments();
      }
    });
    _ai_comments_container.addView(btnRegen);

    // 3. [🎭 টোন ▾] Tone Switcher Button
    LinearLayout btnTone = new LinearLayout(getContext());
    btnTone.setOrientation(LinearLayout.HORIZONTAL);
    btnTone.setGravity(Gravity.CENTER_VERTICAL);
    btnTone.setBackgroundResource(R.drawable.toolbar_chip_bg);
    btnTone.setPadding((int)(9 * density), 0, (int)(10 * density), 0);
    btnTone.setClickable(true);
    btnTone.setFocusable(true);
    LinearLayout.LayoutParams lpTone = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, chipHeight);
    lpTone.gravity = Gravity.CENTER_VERTICAL;
    lpTone.setMarginEnd(chipMarginEnd);
    btnTone.setLayoutParams(lpTone);

    ImageView ivTone = new ImageView(getContext());
    LinearLayout.LayoutParams lpIvTone = new LinearLayout.LayoutParams(iconSize, iconSize);
    lpIvTone.setMarginEnd(iconMargin);
    ivTone.setLayoutParams(lpIvTone);
    ivTone.setImageResource(R.drawable.ic_ai_tone);
    ivTone.setColorFilter(labelColor, PorterDuff.Mode.SRC_IN);
    btnTone.addView(ivTone);

    TextView tvTone = new TextView(getContext());
    tvTone.setText(_lastAiTone + " ▾");
    tvTone.setTextColor(labelColor);
    tvTone.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
    btnTone.addView(tvTone);

    btnTone.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
        showTonePickerMenu(v);
      }
    });
    _ai_comments_container.addView(btnTone);

    // Subtle divider
    View divider = new View(getContext());
    LinearLayout.LayoutParams lpDiv = new LinearLayout.LayoutParams((int)(1 * density), (int)(18 * density));
    lpDiv.gravity = Gravity.CENTER_VERTICAL;
    lpDiv.setMarginEnd(chipMarginEnd);
    divider.setLayoutParams(lpDiv);
    divider.setBackgroundColor(Color.parseColor("#334155"));
    _ai_comments_container.addView(divider);

    // 4. Candidate Comment Chips
    for (int i = 0; i < comments.size(); i++)
    {
      final int index = i;
      final String comment = comments.get(i);
      final boolean isSelected = (index == _selectedAiCommentIndex);

      LinearLayout chip = new LinearLayout(getContext());
      chip.setTag(Integer.valueOf(index));
      chip.setOrientation(LinearLayout.HORIZONTAL);
      chip.setGravity(Gravity.CENTER_VERTICAL);
      chip.setBackgroundResource(isSelected ? R.drawable.toolbar_chip_active_bg : R.drawable.toolbar_chip_bg);
      chip.setPadding(chipPadH, 0, chipPadH, 0);
      chip.setClickable(true);
      chip.setFocusable(true);

      LinearLayout.LayoutParams lpChip = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT, chipHeight);
      lpChip.gravity = Gravity.CENTER_VERTICAL;
      lpChip.setMarginEnd(chipMarginEnd);
      chip.setLayoutParams(lpChip);

      ImageView iv = new ImageView(getContext());
      LinearLayout.LayoutParams lpIv = new LinearLayout.LayoutParams(iconSize, iconSize);
      lpIv.setMarginEnd(iconMargin);
      iv.setLayoutParams(lpIv);
      iv.setImageResource(isSelected ? R.drawable.ic_ai_check : R.drawable.ic_ai_comment);
      iv.setColorFilter(isSelected ? Color.WHITE : Color.parseColor("#94A3B8"), PorterDuff.Mode.SRC_IN);
      chip.addView(iv);

      TextView tv = new TextView(getContext());
      tv.setText(comment);
      tv.setTextColor(isSelected ? Color.WHITE : labelColor);
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
      tv.setSingleLine(true);
      tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
      tv.setMaxWidth(maxTextWidth);
      chip.addView(tv);

      chip.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          insertOrSwapAiComment(comment, index);
        }
      });

      chip.setOnLongClickListener(new View.OnLongClickListener()
      {
        @Override
        public boolean onLongClick(View v)
        {
          try { v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); } catch (Throwable ignored) {}
          showFullCommentsDialog();
          return true;
        }
      });

      _ai_comments_container.addView(chip);
    }

    // 5. Clean Close pill [✕]
    TextView btnClose = new TextView(getContext());
    btnClose.setText("✕");
    btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    btnClose.setTextColor(Color.parseColor("#94A3B8"));
    btnClose.setGravity(Gravity.CENTER);
    btnClose.setPadding((int)(12 * density), 0, (int)(12 * density), 0);
    btnClose.setBackgroundResource(R.drawable.toolbar_chip_bg);
    LinearLayout.LayoutParams lpClose = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, chipHeight);
    lpClose.gravity = Gravity.CENTER_VERTICAL;
    lpClose.setMarginStart((int)(2 * density));
    btnClose.setLayoutParams(lpClose);
    btnClose.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
        hideAiCommentsStrip();
      }
    });
    _ai_comments_container.addView(btnClose);

    _ai_comments_scroll.setVisibility(View.VISIBLE);
    if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
    if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
    if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
    if (_autofill_scroll != null) _autofill_scroll.setVisibility(View.GONE);
    if (_password_autofill_scroll != null) _password_autofill_scroll.setVisibility(View.GONE);
    _ai_comments_scroll.scrollTo(0, 0);
  }

  public void insertOrSwapAiComment(final String comment, final int index)
  {
    if (comment == null || _keyboard2 == null) return;
    InputConnection ic = _keyboard2.getCurrentInputConnection();
    if (ic == null) return;

    try
    {
      performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }
    catch (Throwable ignored) {}

    boolean replaced = false;
    if (_lastInsertedAiComment != null)
    {
      replaced = _keyboard2.replaceVoiceSessionText(_lastInsertedAiComment, comment);
    }

    if (!replaced)
    {
      ic.commitText(comment + " ", 1);
    }

    _lastInsertedAiComment = comment;
    _selectedAiCommentIndex = index;

    refreshAiCommentsChipsState();
  }

  private void refreshAiCommentsChipsState()
  {
    if (_ai_comments_container == null) return;

    android.content.res.TypedArray a = getContext().obtainStyledAttributes(new int[] { R.attr.colorLabel });
    int defaultLabelColor = a.getColor(0, Color.WHITE);
    a.recycle();

    for (int i = 0; i < _ai_comments_container.getChildCount(); i++)
    {
      View child = _ai_comments_container.getChildAt(i);
      Object tag = child.getTag();
      if (tag instanceof Integer)
      {
        int index = ((Integer) tag).intValue();
        boolean isSelected = (index == _selectedAiCommentIndex);
        child.setBackgroundResource(isSelected ? R.drawable.toolbar_chip_active_bg : R.drawable.toolbar_chip_bg);

        if (child instanceof ViewGroup)
        {
          ViewGroup group = (ViewGroup) child;
          if (group.getChildCount() >= 2)
            {
            View iv = group.getChildAt(0);
            View tv = group.getChildAt(1);
            if (iv instanceof ImageView)
            {
              ((ImageView) iv).setImageResource(isSelected ? R.drawable.ic_ai_check : R.drawable.ic_ai_comment);
              ((ImageView) iv).setColorFilter(isSelected ? Color.WHITE : Color.parseColor("#94A3B8"), PorterDuff.Mode.SRC_IN);
            }
            if (tv instanceof TextView)
            {
              ((TextView) tv).setTextColor(isSelected ? Color.WHITE : defaultLabelColor);
            }
          }
        }
      }
    }
  }

  private void showTonePickerMenu(View anchorView)
  {
    if (anchorView == null) return;
    PopupMenu popup = new PopupMenu(getContext(), anchorView);
    final String[] tones = typodev.keyboard.ai.AiVisionHelper.AVAILABLE_TONES;
    for (int i = 0; i < tones.length; i++)
    {
      String prefix = tones[i].equals(_lastAiTone) ? "✓ " : "  ";
      popup.getMenu().add(0, i, i, prefix + tones[i]);
    }
    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
    {
      @Override
      public boolean onMenuItemClick(MenuItem item)
      {
        int id = item.getItemId();
        if (id >= 0 && id < tones.length)
        {
          _lastAiTone = tones[id];
          regenerateAiComments();
          return true;
        }
        return false;
      }
    });
    popup.show();
  }

  public void regenerateAiComments()
  {
    if (_lastAiImageBytes != null && _lastAiImageBytes.length > 0)
    {
      generateAndShowImageComments(_lastAiImageBytes, _lastAiTone);
    }
    else if (_lastAiPostText != null && !_lastAiPostText.trim().isEmpty())
    {
      generateAndShowTextComments(_lastAiPostText, _lastAiTone);
    }
    else
    {
      Toast.makeText(getContext(), "পুনরায় জেনারেট করার জন্য পূর্বের কোনো তথ্য পাওয়া যায়নি", Toast.LENGTH_SHORT).show();
    }
  }

  public void showFullCommentsDialog()
  {
    if (_keyboard2 == null || _currentAiComments == null || _currentAiComments.isEmpty()) return;

    if (_fullCommentsDialog != null && _fullCommentsDialog.isShowing())
    {
      try { _fullCommentsDialog.dismiss(); } catch (Throwable ignored) {}
    }

    final Context context = getContext();
    LayoutInflater inflater = LayoutInflater.from(context);
    View dialogView = inflater.inflate(R.layout.dialog_ai_comments_full, null);

    final AlertDialog dialog = new AlertDialog.Builder(context)
        .setView(dialogView)
        .create();
    _fullCommentsDialog = dialog;

    // Set tone text
    TextView tvTone = dialogView.findViewById(R.id.tv_full_dialog_tone);
    if (tvTone != null)
    {
      tvTone.setText("টোন: " + _lastAiTone);
    }

    // Close button
    View btnClose = dialogView.findViewById(R.id.btn_full_dialog_close);
    if (btnClose != null)
    {
      btnClose.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          dialog.dismiss();
        }
      });
    }

    // Done button
    View btnDone = dialogView.findViewById(R.id.btn_full_dialog_done);
    if (btnDone != null)
    {
      btnDone.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          dialog.dismiss();
        }
      });
    }

    // Regenerate button
    View btnRegen = dialogView.findViewById(R.id.btn_full_dialog_regenerate);
    if (btnRegen != null)
    {
      btnRegen.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          dialog.dismiss();
          regenerateAiComments();
        }
      });
    }

    // Populate Cards
    LinearLayout container = dialogView.findViewById(R.id.container_full_comments_list);
    if (container != null)
    {
      container.removeAllViews();
      for (int i = 0; i < _currentAiComments.size(); i++)
      {
        final int index = i;
        final String comment = _currentAiComments.get(i);
        final View cardView = inflater.inflate(R.layout.item_ai_full_comment_card, container, false);

        TextView tvLabel = cardView.findViewById(R.id.tv_card_label);
        if (tvLabel != null)
        {
          tvLabel.setText("বিকল্প " + (index + 1));
        }

        final TextView tvActiveBadge = cardView.findViewById(R.id.tv_card_active_badge);
        final boolean isSelected = (index == _selectedAiCommentIndex);
        if (tvActiveBadge != null)
        {
          tvActiveBadge.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        }
        cardView.setBackgroundResource(isSelected ? R.drawable.ai_comment_card_active_bg : R.drawable.ai_comment_card_bg);

        TextView tvText = cardView.findViewById(R.id.tv_card_comment_text);
        if (tvText != null)
        {
          tvText.setText(comment);
        }

        View btnCopy = cardView.findViewById(R.id.btn_card_copy);
        if (btnCopy != null)
        {
          btnCopy.setOnClickListener(new View.OnClickListener()
          {
            @Override
            public void onClick(View v)
            {
              try
              {
                android.content.ClipboardManager cm = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null)
                {
                  cm.setPrimaryClip(android.content.ClipData.newPlainText("AI Comment", comment));
                  Toast.makeText(context, "কমেন্ট কপি হয়েছে", Toast.LENGTH_SHORT).show();
                }
              }
              catch (Throwable t)
              {
                Toast.makeText(context, "কপি করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show();
              }
            }
          });
        }

        View btnInsert = cardView.findViewById(R.id.btn_card_insert);
        if (btnInsert != null)
        {
          btnInsert.setOnClickListener(new View.OnClickListener()
          {
            @Override
            public void onClick(View v)
            {
              insertOrSwapAiComment(comment, index);
              dialog.dismiss();
            }
          });
        }

        cardView.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            insertOrSwapAiComment(comment, index);
            dialog.dismiss();
          }
        });

        container.addView(cardView);
      }
    }

    Window win = dialog.getWindow();
    if (win != null)
    {
      win.setBackgroundDrawableResource(android.R.color.transparent);
      win.requestFeature(Window.FEATURE_NO_TITLE);
    }

    typodev.keyboard.Utils.show_dialog_on_ime(dialog, _keyboard2);
  }

  public void hideAiCommentsStrip()
  {
    if (_fullCommentsDialog != null && _fullCommentsDialog.isShowing())
    {
      try { _fullCommentsDialog.dismiss(); } catch (Throwable ignored) {}
      _fullCommentsDialog = null;
    }
    if (_ai_comments_scroll != null)
    {
      _ai_comments_scroll.setVisibility(View.GONE);
    }
    if (_ai_comments_container != null)
    {
      _ai_comments_container.removeAllViews();
    }
    _lastInsertedAiComment = null;
    _selectedAiCommentIndex = -1;
    update_view_visibility(_has_suggestions);
  }

  public void generateAndShowImageComments(final byte[] imageBytes, final String tone)
  {
    if (_keyboard2 == null || imageBytes == null || imageBytes.length == 0) return;
    _keyboard2.showWritingResults(null, imageBytes, tone,
        typodev.keyboard.ai.AiWritingResultsView.Task.REPLY);
  }

  public void generateAndShowTextComments(final String postText, final String tone)
  {
    if (_keyboard2 == null || postText == null || postText.trim().isEmpty()) return;
    _keyboard2.showWritingResults(postText, null, tone,
        typodev.keyboard.ai.AiWritingResultsView.Task.REPLY);
  }
}
