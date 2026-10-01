package typodev.keyboard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import java.util.Collections;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.util.LogPrinter;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.graphics.PorterDuff;
import android.util.DisplayMetrics;
import android.preference.PreferenceManager;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import juloo.cdict.Cdict;
import typodev.keyboard.dict.Dictionaries;
import typodev.keyboard.dict.DictionariesActivity;
import typodev.keyboard.dict.DictionarySwitcher;
import typodev.keyboard.dict.SupportedDictionaries;
import typodev.keyboard.prefs.LayoutsPreference;
import typodev.keyboard.suggestions.CandidatesView;
import typodev.keyboard.suggestions.Suggestions;

public class Keyboard2 extends InputMethodService
  implements SharedPreferences.OnSharedPreferenceChangeListener
{
  /** The view containing the keyboard and candidates view. */
  private ViewGroup _keyboard_container_view;
  private Keyboard2View _keyboard_layout_view;
  private CandidatesView _candidates_view;
  private Suggestions _suggestions;
  private KeyEventHandler _keyeventhandler;
  /** If not 'null', the layout to use instead of [_config.current_layout]. */
  private KeyboardData _currentSpecialLayout;
  /** Layout associated with the currently selected locale. Not 'null'. */
  private KeyboardData _localeTextLayout;
  /** Installed and current locales. */
  private Dictionaries _dictionaries;
  private ViewGroup _emojiPane = null;
  private ViewGroup _clipboard_pane = null;
  private typodev.keyboard.ai.AiPaneView _ai_pane_view = null;
  private typodev.keyboard.fancy.FancyPaneView _fancy_pane_view = null;
  private typodev.keyboard.suggestions.ToolsGridPaneView _tools_grid_pane = null;
  private typodev.keyboard.ai.AiPromptBarView _ai_prompt_bar = null;
  private boolean _isAiPromptInputMode = false;
  private InputConnection _aiPromptInputConnection = null;
  private EmojiSearchBarView _emoji_search_bar = null;
  private boolean _isEmojiSearchMode = false;
  private InputConnection _emojiSearchInputConnection = null;
  private typodev.keyboard.clipboard.ClipboardSearchBarView _clipboard_search_bar = null;
  private boolean _isClipboardSearchMode = false;
  private InputConnection _clipboardSearchInputConnection = null;
  private typodev.keyboard.clipboard.ClipboardAddBarView _clipboard_add_bar = null;
  private boolean _isAddClipMode = false;
  private InputConnection _addClipInputConnection = null;
  private typodev.keyboard.clipboard.ClipboardPaneView _clipboard_pane_view = null;
  private View _keyboard_wrapper = null;
  private View _one_hand_left_panel = null;
  private View _one_hand_right_panel = null;
  private ImageView _btn_one_hand_left_switch = null;
  private ImageView _btn_one_hand_left_expand = null;
  private ImageView _btn_one_hand_right_switch = null;
  private ImageView _btn_one_hand_right_expand = null;
  private boolean _isClipboardPaneOpen = false;
  private boolean _isAiSettingsOpen = false;
  private boolean _wasAiPaneVisibleBeforeSettings = false;
  private android.widget.EditText _dialogActiveEditText = null;
  private InputConnection _dialogInputConnection = null;
  private typodev.keyboard.translate.TranslateBarView _translate_bar_view = null;
  private boolean _isTranslateMode = false;
  private InputConnection _translateInputConnection = null;
  private Receiver _receiver = null;
  private Handler _handler;
  private long _inputSessionId = 0;
  private long _inlineResponseId = 0;

  public long getInputSessionId()
  {
    return _inputSessionId;
  }

  private void finishInputSession()
  {
    _inputSessionId++;
    _inlineResponseId++;
    typodev.keyboard.voice.OfflineVoiceTypingService.cancelListening();
    if (_isTranslateMode)
      hideTranslateBar();
    if (_keyeventhandler != null)
      _keyeventhandler.finished();
    if (_candidates_view != null)
      _candidates_view.setInlineSuggestions(null);
  }

  @Override
  public void onStartInput(EditorInfo info, boolean restarting)
  {
    finishInputSession();
    super.onStartInput(info, restarting);
  }

  @Override
  public void onFinishInput()
  {
    finishInputSession();
    super.onFinishInput();
  }

  private Config _config;

  private FoldStateTracker _foldStateTracker;

  /** Layout currently visible before it has been modified. */
  KeyboardData current_layout_unmodified()
  {
    if (_currentSpecialLayout != null)
      return _currentSpecialLayout;
    KeyboardData layout = null;
    int layout_i = _config.get_current_layout();
    if (layout_i >= _config.layouts.size())
      layout_i = 0;
    if (layout_i < _config.layouts.size())
      layout = _config.layouts.get(layout_i);
    if (layout == null)
      layout = _localeTextLayout;
    return layout;
  }

  /** Layout currently visible. */
  KeyboardData current_layout()
  {
    if (_currentSpecialLayout != null)
      return _currentSpecialLayout;
    return LayoutModifier.modify_layout(current_layout_unmodified());
  }

  void setTextLayout(int l)
  {
    _config.set_current_layout(l);
    _currentSpecialLayout = null;
    // The active dictionary depends on the current layout.
    refresh_current_dictionary();
    refresh_candidates_view();
    _keyboard_layout_view.setKeyboard(current_layout());
  }

  void incrTextLayout(int delta)
  {
    int s = _config.layouts.size();
    if (s == 0)
      return;
    setTextLayout((_config.get_current_layout() + delta + s) % s);
  }

  void setSpecialLayout(KeyboardData l)
  {
    _currentSpecialLayout = l;
    _keyboard_layout_view.setKeyboard(l);
  }

  KeyboardData loadLayout(int layout_id)
  {
    return KeyboardData.load(getResources(), layout_id);
  }

  /** Load a layout that contains a numpad. */
  KeyboardData loadNumpad(int layout_id)
  {
    return LayoutModifier.modify_numpad(KeyboardData.load(getResources(), layout_id),
        current_layout_unmodified());
  }

  KeyboardData loadNumericLayout()
  {
    return loadNumpad(_config.orientation_landscape ?
        R.xml.numeric_landscape : R.xml.numeric);
  }

  KeyboardData loadPinentry(int layout_id)
  {
    return LayoutModifier.modify_pinentry(KeyboardData.load(getResources(), layout_id),
        current_layout_unmodified());
  }

  @Override
  public void onCreate()
  {
    super.onCreate();
    SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(this);
    _handler = new Handler(getMainLooper());
    _foldStateTracker = new FoldStateTracker(this);
    _dictionaries = Dictionaries.instance(this);
    Config.initGlobalConfig(prefs, getResources(),
        _foldStateTracker.isUnfolded(), _dictionaries);
    _config = Config.globalConfig();
    _receiver = this.new Receiver();
    _suggestions = new Suggestions(_receiver, _config, this);
    _keyeventhandler = new KeyEventHandler(_receiver, _suggestions);
    KeyValue.Stateful._handler = _receiver;
    _config.handler = _keyeventhandler;
    Config.setGlobalHandler(_keyeventhandler);
    prefs.registerOnSharedPreferenceChangeListener(this);
    Logs.set_debug_logs(getResources().getBoolean(R.bool.debug_logs));
    refreshSubtypeImm();
    create_keyboard_view();
    ClipboardHistoryService.on_startup(this, _keyeventhandler);
    _foldStateTracker.setChangedCallback(() -> { refresh_config(); });
    typodev.keyboard.suggestions.BanglishLexiconManager.instance(this).init(this);
    SoundFeedbackManager.getInstance(this);
  }

  @Override
  public void onDestroy() {
    finishInputSession();
    if (_handler != null)
      _handler.removeCallbacksAndMessages(null);
    if (KeyValue.Stateful._handler == _receiver)
      KeyValue.Stateful._handler = null;
    super.onDestroy();
    try
    {
      SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(this);
      if (prefs != null)
      {
        prefs.unregisterOnSharedPreferenceChangeListener(this);
      }
    }
    catch (Throwable ignored) {}

    try
    {
      SoundFeedbackManager.getInstance(this).release();
    }
    catch (Throwable ignored) {}

    if (_foldStateTracker != null)
      _foldStateTracker.close();
  }

  @Override
  public void onWindowHidden() {
    super.onWindowHidden();
    typodev.keyboard.voice.OfflineVoiceTypingService.cancelListening();
  }

  private void create_keyboard_view()
  {
    finishInputSession();
    _isAiPromptInputMode = false;
    _aiPromptInputConnection = null;
    _isEmojiSearchMode = false;
    _emojiSearchInputConnection = null;
    _isClipboardSearchMode = false;
    _clipboardSearchInputConnection = null;
    _isAddClipMode = false;
    _addClipInputConnection = null;
    _isClipboardPaneOpen = false;
    _keyboard_container_view = (ViewGroup)inflate_view(R.layout.keyboard);
    _keyboard_layout_view = (Keyboard2View)_keyboard_container_view.findViewById(R.id.keyboard_view);
    if (_keyboard_layout_view != null)
    {
      _keyboard_layout_view.setConfig(_config);
    }
    _keyboard_wrapper = _keyboard_container_view.findViewById(R.id.keyboard_wrapper);
    _one_hand_left_panel = _keyboard_container_view.findViewById(R.id.one_hand_left_panel);
    _one_hand_right_panel = _keyboard_container_view.findViewById(R.id.one_hand_right_panel);
    _btn_one_hand_left_switch = (ImageView)_keyboard_container_view.findViewById(R.id.btn_one_hand_left_switch);
    _btn_one_hand_left_expand = (ImageView)_keyboard_container_view.findViewById(R.id.btn_one_hand_left_expand);
    _btn_one_hand_right_switch = (ImageView)_keyboard_container_view.findViewById(R.id.btn_one_hand_right_switch);
    _btn_one_hand_right_expand = (ImageView)_keyboard_container_view.findViewById(R.id.btn_one_hand_right_expand);

    if (_btn_one_hand_left_switch != null)
    {
      _btn_one_hand_left_switch.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
          switchOneHandSide();
        }
      });
    }
    if (_btn_one_hand_right_switch != null)
    {
      _btn_one_hand_right_switch.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
          switchOneHandSide();
        }
      });
    }
    if (_btn_one_hand_left_expand != null)
    {
      _btn_one_hand_left_expand.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
          setOneHandMode(false, null);
        }
      });
    }
    if (_btn_one_hand_right_expand != null)
    {
      _btn_one_hand_right_expand.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          try { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); } catch (Throwable ignored) {}
          setOneHandMode(false, null);
        }
      });
    }
    if (_keyboard_layout_view != null)
    {
      _keyboard_layout_view.setOnVisibilityChangeListener(new Keyboard2View.OnVisibilityChangeListener()
      {
        @Override
        public void onVisibilityChanged(int visibility)
        {
          updateOneHandModeUI();
        }
      });
    }
    _candidates_view = (CandidatesView)_keyboard_container_view.findViewById(R.id.candidates_view);
    if (_candidates_view != null)
      _candidates_view.set_keyboard2(this);
    _ai_pane_view = (typodev.keyboard.ai.AiPaneView)_keyboard_container_view.findViewById(R.id.ai_pane_view);
    if (_ai_pane_view != null)
      _ai_pane_view.init(this);
    _fancy_pane_view = (typodev.keyboard.fancy.FancyPaneView)_keyboard_container_view.findViewById(R.id.fancy_pane_view);
    if (_fancy_pane_view != null)
      _fancy_pane_view.init(this);
    // Seed demo snippets on first run (no-op on subsequent launches)
    try { typodev.keyboard.snippet.SnippetStore.instance(this).seedDefaults(); }
    catch (Throwable ignored) {}
    _ai_prompt_bar = (typodev.keyboard.ai.AiPromptBarView)_keyboard_container_view.findViewById(R.id.ai_prompt_bar);
    if (_ai_prompt_bar != null)
    {
      _ai_prompt_bar.setOnPromptActionListener(new typodev.keyboard.ai.AiPromptBarView.OnPromptActionListener()
      {
        @Override
        public void onConfirm(String promptText)
        {
          exitAiPromptTypingMode(promptText, true);
        }

        @Override
        public void onCancel()
        {
          exitAiPromptTypingMode(null, false);
        }
      });
    }

    _emoji_search_bar = (EmojiSearchBarView)_keyboard_container_view.findViewById(R.id.emoji_search_bar_view);
    if (_emoji_search_bar != null)
    {
      _emoji_search_bar.setOnEmojiSearchActionListener(new EmojiSearchBarView.OnEmojiSearchActionListener()
      {
        @Override
        public void onEmojiSelected(Emoji emoji)
        {
          onEmojiSelectedFromSearch(emoji);
        }

        @Override
        public void onCloseSearch()
        {
          exitEmojiSearchMode();
        }
      });
    }

    _translate_bar_view = (typodev.keyboard.translate.TranslateBarView)_keyboard_container_view.findViewById(R.id.translate_bar_view);
    if (_translate_bar_view != null)
    {
      _translate_bar_view.setKeyboard(this);
    }

    _clipboard_search_bar = (typodev.keyboard.clipboard.ClipboardSearchBarView)_keyboard_container_view.findViewById(R.id.clipboard_search_bar_view);
    if (_clipboard_search_bar != null)
    {
      _clipboard_search_bar.setOnClipboardSearchActionListener(new typodev.keyboard.clipboard.ClipboardSearchBarView.OnClipboardSearchActionListener()
      {
        @Override
        public void onClipSelected(String clipContent)
        {
          onClipSelectedFromSearch(clipContent);
        }

        @Override
        public void onCloseSearch()
        {
          exitClipboardSearchMode();
        }

        @Override
        public void onOpenFullClipboard()
        {
          exitClipboardSearchMode();
          showClipboardPane();
        }
      });
    }

    _clipboard_add_bar = (typodev.keyboard.clipboard.ClipboardAddBarView)_keyboard_container_view.findViewById(R.id.clipboard_add_bar_view);
    if (_clipboard_add_bar != null)
    {
      _clipboard_add_bar.setOnAddClipActionListener(new typodev.keyboard.clipboard.ClipboardAddBarView.OnAddClipActionListener()
      {
        @Override
        public void onPin(String text)
        {
          typodev.keyboard.clipboard.PinnedClipboardStore.instance(getApplicationContext()).pinClip(text);
          android.widget.Toast.makeText(getApplicationContext(), "Added to pinned clipboard", android.widget.Toast.LENGTH_SHORT).show();
          exitAddClipMode(true);
        }

        @Override
        public void onCancel()
        {
          exitAddClipMode(true);
        }
      });
    }

    _clipboard_pane_view = (typodev.keyboard.clipboard.ClipboardPaneView)_keyboard_container_view.findViewById(R.id.clipboard_pane_view);
    _tools_grid_pane = (typodev.keyboard.suggestions.ToolsGridPaneView)_keyboard_container_view.findViewById(R.id.tools_grid_pane);
    if (_tools_grid_pane != null)
      _tools_grid_pane.init(this);
  }

  public boolean isTranslateBarOpen()
  {
    return _isTranslateMode && _translate_bar_view != null && _translate_bar_view.getVisibility() == View.VISIBLE;
  }

  public void showTranslateBar()
  {
    if (_translate_bar_view == null) return;
    _isTranslateMode = true;
    InputConnection targetIc = super.getCurrentInputConnection();
    _translateInputConnection = _translate_bar_view.createInputConnection(targetIc);

    _translate_bar_view.onTranslateOpened();
    if (_candidates_view != null)
    {
      _candidates_view.setVisibility(View.VISIBLE);
      _candidates_view.updateTranslateButtonState();
    }
    if (_keyeventhandler != null)
      _keyeventhandler.started(_config);
  }

  public void hideTranslateBar()
  {
    _isTranslateMode = false;
    _translateInputConnection = null;
    if (_translate_bar_view != null)
    {
      _translate_bar_view.onTranslateClosed();
      _translate_bar_view.setVisibility(View.GONE);
    }
    if (_candidates_view != null)
    {
      _candidates_view.setVisibility(View.VISIBLE);
      _candidates_view.updateTranslateButtonState();
    }
    if (_keyeventhandler != null)
      _keyeventhandler.started(_config);
  }

  @Override
  public InputConnection getCurrentInputConnection()
  {
    if (_isAiSettingsOpen && _dialogInputConnection != null)
    {
      return _dialogInputConnection;
    }
    if (_isTranslateMode && _translateInputConnection != null)
    {
      return _translateInputConnection;
    }
    if (_isAiPromptInputMode && _aiPromptInputConnection != null)
    {
      return _aiPromptInputConnection;
    }
    if (_isEmojiSearchMode && _emojiSearchInputConnection != null)
    {
      return _emojiSearchInputConnection;
    }
    if (_isClipboardSearchMode && _clipboardSearchInputConnection != null)
    {
      return _clipboardSearchInputConnection;
    }
    if (_isAddClipMode && _addClipInputConnection != null)
    {
      return _addClipInputConnection;
    }
    return super.getCurrentInputConnection();
  }

  public InputConnection getTargetAppInputConnection()
  {
    return super.getCurrentInputConnection();
  }

  public void pasteImageContent(android.net.Uri uri, String mimeType)
  {
    if (uri == null) return;
    if (android.os.Build.VERSION.SDK_INT >= 25)
    {
      try
      {
        InputConnection ic = getTargetAppInputConnection();
        if (ic != null)
        {
          android.content.ClipDescription description = new android.content.ClipDescription(
              "Image", new String[]{ mimeType != null ? mimeType : "image/png" });
          android.view.inputmethod.InputContentInfo inputContentInfo =
              new android.view.inputmethod.InputContentInfo(uri, description, null);
          if (ic.commitContent(inputContentInfo, android.view.inputmethod.InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null))
            return;
        }
      }
      catch (Throwable t)
      {
        Logs.print_exception(t);
      }
    }
    ClipboardHistoryService.paste(uri.toString());
  }

  public void handle_event_key(KeyValue.Event ev)
  {
    if (_receiver != null)
    {
      _receiver.handle_event_key(ev);
    }
  }

  public boolean isAiPaneVisible()
  {
    return (_ai_pane_view != null && _ai_pane_view.getVisibility() == View.VISIBLE)
        || _isAiPromptInputMode;
  }

  public boolean isAiPromptInputMode()
  {
    return _isAiPromptInputMode;
  }

  public void showAiPromptTypingMode(String initialText)
  {
    if (_ai_prompt_bar == null || _keyboard_layout_view == null) return;

    _isAiPromptInputMode = true;

    if (_ai_pane_view != null)
      _ai_pane_view.setVisibility(View.GONE);
    if (_candidates_view != null)
      _candidates_view.setVisibility(View.GONE);

    int colorKeyboard = (_ai_pane_view != null) ? _ai_pane_view.getColorKeyboard() : 0;
    int colorKey = (_ai_pane_view != null) ? _ai_pane_view.getColorKey() : 0;
    int colorLabel = (_ai_pane_view != null) ? _ai_pane_view.getColorLabel() : 0;
    int colorKeyActivated = (_ai_pane_view != null) ? _ai_pane_view.getColorKeyActivated() : 0;
    _ai_prompt_bar.applyTheme(colorKeyboard, colorKey, colorLabel, colorKeyActivated);

    _ai_prompt_bar.setPromptText(initialText);
    _aiPromptInputConnection = _ai_prompt_bar.createInputConnection();

    _ai_prompt_bar.setVisibility(View.VISIBLE);
    _keyboard_layout_view.setVisibility(View.VISIBLE);
    _keyboard_layout_view.setKeyboard(current_layout());
  }

  public void exitAiPromptTypingMode(String promptText, boolean applyAndGenerate)
  {
    _isAiPromptInputMode = false;
    _aiPromptInputConnection = null;

    if (_ai_prompt_bar != null)
      _ai_prompt_bar.setVisibility(View.GONE);
    if (_keyboard_layout_view != null)
      _keyboard_layout_view.setVisibility(View.GONE);

    if (_ai_pane_view != null)
    {
      _ai_pane_view.setVisibility(View.VISIBLE);
      if (applyAndGenerate && promptText != null)
      {
        _ai_pane_view.onPromptTypedFromKeyboard(promptText);
      }
    }
  }

  public int getTargetPaneHeight()
  {
    int h = (_keyboard_container_view != null && _keyboard_container_view.getHeight() > 0)
        ? _keyboard_container_view.getHeight() : 0;
    if (h <= 0)
    {
      int kh = _keyboard_layout_view != null ? _keyboard_layout_view.getHeight() : 0;
      if (kh <= 0 && _keyboard_layout_view != null)
        kh = _keyboard_layout_view.getMeasuredHeight();
      int ch = (_candidates_view != null && _candidates_view.getVisibility() == View.VISIBLE)
          ? _candidates_view.getHeight() : 0;
      if (ch <= 0 && _candidates_view != null && _candidates_view.getVisibility() == View.VISIBLE)
        ch = _candidates_view.getMeasuredHeight();
      h = kh + ch;
    }
    if (h <= 0)
    {
      android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
      h = (int)(280 * dm.density + 0.5f);
    }
    return h;
  }

  public int getBottomSafety()
  {
    int bottomSafety = (_keyboard_layout_view != null) ? _keyboard_layout_view.getBottomMargin() : 0;
    if (bottomSafety <= 0 && _config != null)
      bottomSafety = (int)_config.margin_bottom;
    return bottomSafety;
  }

  public int getColorKeyboard() {
    CustomThemeStore cs = CustomThemeStore.instance(this);
    if (cs.isCustomThemeActive(_config)) return cs.getKeyboardBg();
    return (_ai_pane_view != null) ? _ai_pane_view.getColorKeyboard() : Color.parseColor("#151A23");
  }
  public int getColorKey() {
    CustomThemeStore cs = CustomThemeStore.instance(this);
    if (cs.isCustomThemeActive(_config)) return cs.getKeyNormal();
    return (_ai_pane_view != null) ? _ai_pane_view.getColorKey() : Color.parseColor("#212836");
  }
  public int getColorLabel() {
    CustomThemeStore cs = CustomThemeStore.instance(this);
    if (cs.isCustomThemeActive(_config)) return cs.getLabelColor();
    return (_ai_pane_view != null) ? _ai_pane_view.getColorLabel() : Color.WHITE;
  }
  public int getColorKeyActivated() {
    CustomThemeStore cs = CustomThemeStore.instance(this);
    if (cs.isCustomThemeActive(_config)) return cs.getKeyShift();
    return (_ai_pane_view != null) ? _ai_pane_view.getColorKeyActivated() : Color.parseColor("#2AABEE");
  }
  public int getColorSubLabel() {
    CustomThemeStore cs = CustomThemeStore.instance(this);
    if (cs.isCustomThemeActive(_config)) return cs.getSubLabelColor();
    return Color.parseColor("#8E99A8");
  }

  public void showAiPane()
  {
    try
    {
      if (_ai_pane_view == null) return;

      int h = getTargetPaneHeight();
      int bottomSafety = getBottomSafety();

      _ai_pane_view.open(h, bottomSafety);

      if (_candidates_view != null)
        _candidates_view.setVisibility(View.GONE);
      if (_keyboard_layout_view != null)
        _keyboard_layout_view.setVisibility(View.GONE);
      if (_tools_grid_pane != null)
        _tools_grid_pane.setVisibility(View.GONE);

      _ai_pane_view.setVisibility(View.VISIBLE);
    }
    catch (Throwable t)
    {
      Logs.print_exception(t);
      if (_keyboard_layout_view != null)
        _keyboard_layout_view.setVisibility(View.VISIBLE);
      refresh_candidates_view();
    }
  }

  public void closeAiPane()
  {
    try
    {
      if (_isAiPromptInputMode)
      {
        exitAiPromptTypingMode(null, false);
        return;
      }
      if (_ai_pane_view != null)
        _ai_pane_view.setVisibility(View.GONE);
      if (_keyboard_layout_view != null)
        _keyboard_layout_view.setVisibility(View.VISIBLE);
      refresh_candidates_view();
    }
    catch (Throwable t)
    {
      Logs.print_exception(t);
    }
  }

  public boolean isAiSettingsOpen()
  {
    return _isAiSettingsOpen;
  }

  public void notifyDialogEditTextFocused(android.widget.EditText et)
  {
    _dialogActiveEditText = et;
    if (et != null)
    {
      android.view.inputmethod.EditorInfo outAttrs = new android.view.inputmethod.EditorInfo();
      android.view.inputmethod.InputConnection ic = et.onCreateInputConnection(outAttrs);
      _dialogInputConnection = (ic != null) ? ic : new android.view.inputmethod.BaseInputConnection(et, true);
    }
    else
    {
      _dialogInputConnection = null;
    }
  }

  public void onAiSettingsOpened()
  {
    _isAiSettingsOpen = true;
    if (_candidates_view != null)
      _candidates_view.setVisibility(View.GONE);
  }

  public void onAiSettingsClosed()
  {
    _isAiSettingsOpen = false;
    _dialogInputConnection = null;
    _dialogActiveEditText = null;
    typodev.keyboard.ai.AiSettingsDialog.sActiveDialogEditText = null;
    if (isAiPaneVisible())
    {
      if (_candidates_view != null)
        _candidates_view.setVisibility(View.GONE);
      if (_ai_pane_view != null)
        _ai_pane_view.setVisibility(View.VISIBLE);
    }
    else
    {
      if (_keyboard_layout_view != null)
        _keyboard_layout_view.setVisibility(View.VISIBLE);
      refresh_candidates_view();
    }
  }

  // =========================================================
  // Fancy Pane (Unicode fonts, Text Art, Kaomoji)
  // =========================================================

  public boolean isFancyPaneVisible()
  {
    return _fancy_pane_view != null && _fancy_pane_view.getVisibility() == View.VISIBLE;
  }

  public void showFancyPane()
  {
    try
    {
      if (_fancy_pane_view == null) return;

      // Compute target height (same logic as showAiPane)
      int h = (_keyboard_container_view != null && _keyboard_container_view.getHeight() > 0)
          ? _keyboard_container_view.getHeight() : 0;
      if (h <= 0)
      {
        int kh = _keyboard_layout_view != null ? _keyboard_layout_view.getHeight() : 0;
        if (kh <= 0 && _keyboard_layout_view != null)
          kh = _keyboard_layout_view.getMeasuredHeight();
        int ch = (_candidates_view != null && _candidates_view.getVisibility() == View.VISIBLE)
            ? _candidates_view.getHeight() : 0;
        if (ch <= 0 && _candidates_view != null && _candidates_view.getVisibility() == View.VISIBLE)
          ch = _candidates_view.getMeasuredHeight();
        h = kh + ch;
      }
      if (h <= 0)
      {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        h = (int)(280 * dm.density + 0.5f);
      }
      int bottomSafety = (_keyboard_layout_view != null) ? _keyboard_layout_view.getBottomMargin() : 0;
      if (bottomSafety <= 0 && _config != null)
        bottomSafety = (int)_config.margin_bottom;

      // Hide AI pane if open
      if (_ai_pane_view != null) _ai_pane_view.setVisibility(View.GONE);
      if (_tools_grid_pane != null) _tools_grid_pane.setVisibility(View.GONE);

      // Open the fancy pane
      _fancy_pane_view.open(h, bottomSafety);

      if (_candidates_view != null)
        _candidates_view.setVisibility(View.GONE);
      if (_keyboard_layout_view != null)
        _keyboard_layout_view.setVisibility(View.GONE);

      _fancy_pane_view.setVisibility(View.VISIBLE);
    }
    catch (Throwable t)
    {
      Logs.print_exception(t);
      // Fall back to showing the keyboard
      if (_keyboard_layout_view != null)
        _keyboard_layout_view.setVisibility(View.VISIBLE);
      refresh_candidates_view();
    }
  }

  public void closeFancyPane()
  {
    try
    {
      if (_fancy_pane_view != null)
        _fancy_pane_view.setVisibility(View.GONE);
      if (_keyboard_layout_view != null)
        _keyboard_layout_view.setVisibility(View.VISIBLE);
      refresh_candidates_view();
    }
    catch (Throwable t)
    {
      Logs.print_exception(t);
    }
  }

  // =========================================================
  // Tools Grid Pane (More Tools in place of keyboard keys)
  // =========================================================

  public boolean isToolsPaneVisible()
  {
    return _tools_grid_pane != null && _tools_grid_pane.getVisibility() == View.VISIBLE;
  }

  public boolean isToolsPaneEditMode()
  {
    return _tools_grid_pane != null && _tools_grid_pane.isEditMode();
  }

  public void showToolsPane()
  {
    try
    {
      if (_tools_grid_pane == null) return;

      int h = (_keyboard_layout_view != null && _keyboard_layout_view.getHeight() > 0)
          ? _keyboard_layout_view.getHeight() : 0;
      if (h <= 0 && _keyboard_layout_view != null)
        h = _keyboard_layout_view.getMeasuredHeight();
      if (h <= 0)
        h = getTargetPaneHeight();
      int bottomSafety = getBottomSafety();

      if (_ai_pane_view != null) _ai_pane_view.setVisibility(View.GONE);
      if (_fancy_pane_view != null) _fancy_pane_view.setVisibility(View.GONE);
      if (_clipboard_pane_view != null) _clipboard_pane_view.setVisibility(View.GONE);
      if (_emoji_search_bar != null) _emoji_search_bar.setVisibility(View.GONE);
      if (_clipboard_search_bar != null) _clipboard_search_bar.setVisibility(View.GONE);
      if (_clipboard_add_bar != null) _clipboard_add_bar.setVisibility(View.GONE);

      _tools_grid_pane.applyTheme(getColorKeyboard(), getColorKey(), getColorLabel(), getColorKeyActivated(), getColorSubLabel());
      _tools_grid_pane.open(h, bottomSafety);

      if (_keyboard_layout_view != null)
        _keyboard_layout_view.setVisibility(View.GONE);

      _tools_grid_pane.setVisibility(View.VISIBLE);

      if (_candidates_view != null)
      {
        _candidates_view.setVisibility(View.VISIBLE);
        _candidates_view.updateMoreButtonState(true);
      }
    }
    catch (Throwable t)
    {
      Logs.print_exception(t);
      if (_keyboard_layout_view != null)
        _keyboard_layout_view.setVisibility(View.VISIBLE);
      refresh_candidates_view();
    }
  }

  public void closeToolsPane()
  {
    try
    {
      if (_tools_grid_pane != null)
        _tools_grid_pane.setVisibility(View.GONE);

      if (_keyboard_layout_view != null)
      {
        _keyboard_layout_view.setVisibility(View.VISIBLE);
        _keyboard_layout_view.setKeyboard(current_layout());
      }
      if (_candidates_view != null)
      {
        _candidates_view.setVisibility(View.VISIBLE);
        _candidates_view.updateMoreButtonState(false);
      }
      refresh_candidates_view();
    }
    catch (Throwable t)
    {
      Logs.print_exception(t);
    }
  }

  public Keyboard2View getKeyboardView()
  {
    return _keyboard_layout_view;
  }

  public ViewGroup getKeyboardContainer()
  {
    return _keyboard_container_view;
  }

  public boolean isOneHandModeActive()
  {
    return _config != null && _config.one_hand_enabled;
  }

  public void toggleOneHandMode()
  {
    if (_config == null) return;
    setOneHandMode(!_config.one_hand_enabled, _config.one_hand_side);
  }

  public void setOneHandMode(boolean enabled, String side)
  {
    if (_config == null) return;
    _config.one_hand_enabled = enabled;
    if (side != null && !side.isEmpty())
      _config.one_hand_side = side;

    try
    {
      SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(this);
      defPrefs.edit()
          .putBoolean("pref_one_hand_enabled", _config.one_hand_enabled)
          .putString("pref_one_hand_side", _config.one_hand_side)
          .apply();

      SharedPreferences protPrefs = DirectBootAwarePreferences.get_shared_preferences(this);
      protPrefs.edit()
          .putBoolean("pref_one_hand_enabled", _config.one_hand_enabled)
          .putString("pref_one_hand_side", _config.one_hand_side)
          .apply();

      DirectBootAwarePreferences.copy_preferences_to_protected_storage(this, defPrefs);
    }
    catch (Throwable ignored) {}

    updateOneHandModeUI();
    typodev.keyboard.suggestions.ToolbarToolsManager.getInstance(this).notifyListeners();
    if (_candidates_view != null)
      _candidates_view.rebuildToolbarButtons();
  }

  public void switchOneHandSide()
  {
    if (_config == null) return;
    String newSide = "left".equals(_config.one_hand_side) ? "right" : "left";
    setOneHandMode(true, newSide);
  }

  public void updateOneHandModeUI()
  {
    if (_one_hand_left_panel == null || _one_hand_right_panel == null)
      return;

    boolean isKeyboardVisible = (_keyboard_layout_view != null && _keyboard_layout_view.getVisibility() == View.VISIBLE);
    boolean enabled = (_config != null && _config.one_hand_enabled && isKeyboardVisible);

    if (!enabled)
    {
      _one_hand_left_panel.setVisibility(View.GONE);
      _one_hand_right_panel.setVisibility(View.GONE);
    }
    else
    {
      DisplayMetrics dm = getResources().getDisplayMetrics();
      int panelWidth = Math.max((int)(dm.widthPixels * 0.18f), (int)(54 * dm.density));

      if ("left".equals(_config.one_hand_side))
      {
        _one_hand_left_panel.setVisibility(View.GONE);
        ViewGroup.LayoutParams lp = _one_hand_right_panel.getLayoutParams();
        if (lp != null)
        {
          lp.width = panelWidth;
          _one_hand_right_panel.setLayoutParams(lp);
        }
        _one_hand_right_panel.setVisibility(View.VISIBLE);
      }
      else
      {
        _one_hand_right_panel.setVisibility(View.GONE);
        ViewGroup.LayoutParams lp = _one_hand_left_panel.getLayoutParams();
        if (lp != null)
        {
          lp.width = panelWidth;
          _one_hand_left_panel.setLayoutParams(lp);
        }
        _one_hand_left_panel.setVisibility(View.VISIBLE);
      }
      applyOneHandPanelTheme();
    }
  }

  public void applyOneHandPanelTheme()
  {
    try
    {
      int labelColor = getColorLabel();
      int keyColor = getColorKey();
      int bgKeyboard = getColorKeyboard();
      float density = getResources().getDisplayMetrics().density;

      android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
      btnBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
      btnBg.setCornerRadius(10 * density);
      btnBg.setColor((keyColor != 0) ? keyColor : Color.parseColor("#2A3241"));
      btnBg.setStroke((int)(1 * density), Color.argb(45, Color.red(labelColor), Color.green(labelColor), Color.blue(labelColor)));

      if (_btn_one_hand_left_switch != null)
      {
        _btn_one_hand_left_switch.setColorFilter(labelColor, PorterDuff.Mode.SRC_IN);
        if (btnBg.getConstantState() != null)
          _btn_one_hand_left_switch.setBackground(btnBg.getConstantState().newDrawable().mutate());
      }
      if (_btn_one_hand_left_expand != null)
      {
        _btn_one_hand_left_expand.setColorFilter(labelColor, PorterDuff.Mode.SRC_IN);
        if (btnBg.getConstantState() != null)
          _btn_one_hand_left_expand.setBackground(btnBg.getConstantState().newDrawable().mutate());
      }
      if (_btn_one_hand_right_switch != null)
      {
        _btn_one_hand_right_switch.setColorFilter(labelColor, PorterDuff.Mode.SRC_IN);
        if (btnBg.getConstantState() != null)
          _btn_one_hand_right_switch.setBackground(btnBg.getConstantState().newDrawable().mutate());
      }
      if (_btn_one_hand_right_expand != null)
      {
        _btn_one_hand_right_expand.setColorFilter(labelColor, PorterDuff.Mode.SRC_IN);
        if (btnBg.getConstantState() != null)
          _btn_one_hand_right_expand.setBackground(btnBg.getConstantState().newDrawable().mutate());
      }

      int sidePanelBg = Color.argb(230, Color.red(bgKeyboard), Color.green(bgKeyboard), Color.blue(bgKeyboard));
      if (_one_hand_left_panel != null)
        _one_hand_left_panel.setBackgroundColor(sidePanelBg);
      if (_one_hand_right_panel != null)
        _one_hand_right_panel.setBackgroundColor(sidePanelBg);
    }
    catch (Throwable ignored) {}
  }

  public void showEmojiPane()
  {
    if (_isEmojiSearchMode)
    {
      _isEmojiSearchMode = false;
      _emojiSearchInputConnection = null;
      if (_emoji_search_bar != null) _emoji_search_bar.setVisibility(View.GONE);
    }
    if (_emojiPane == null)
    {
      _emojiPane = (ViewGroup)inflate_view(R.layout.emoji_pane);
      wireEmojiPaneSearch();
    }
    setInputView(_emojiPane);
    EmojiGridView grid = (EmojiGridView)_emojiPane.findViewById(R.id.emoji_grid);
    if (grid != null)
    {
      grid.setEmojiGroup(EmojiGridView.GROUP_LAST_USE);
    }
  }

  public void showEmojiPaneFromToolbar()
  {
    showEmojiPane();
  }

  private void wireEmojiPaneSearch()
  {
    if (_emojiPane == null) return;
    View btnSearch = _emojiPane.findViewById(R.id.btn_open_emoji_search);
    if (btnSearch != null)
    {
      btnSearch.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          showEmojiSearchMode("");
        }
      });
    }
  }

  public void showEmojiSearchMode(String query)
  {
    _isEmojiSearchMode = true;
    if (_candidates_view != null) _candidates_view.setVisibility(View.GONE);
    if (_ai_pane_view != null) _ai_pane_view.setVisibility(View.GONE);
    if (_ai_prompt_bar != null) _ai_prompt_bar.setVisibility(View.GONE);

    if (_emoji_search_bar != null)
    {
      int colorKeyboard = (_ai_pane_view != null) ? _ai_pane_view.getColorKeyboard() : Color.parseColor("#151A23");
      int colorKey = (_ai_pane_view != null) ? _ai_pane_view.getColorKey() : Color.parseColor("#212836");
      int colorLabel = (_ai_pane_view != null) ? _ai_pane_view.getColorLabel() : Color.WHITE;
      int colorKeyActivated = (_ai_pane_view != null) ? _ai_pane_view.getColorKeyActivated() : Color.parseColor("#2AABEE");
      _emoji_search_bar.applyTheme(colorKeyboard, colorKey, colorLabel, colorKeyActivated);
      _emoji_search_bar.setQuery(query);
      _emojiSearchInputConnection = _emoji_search_bar.createInputConnection();
      _emoji_search_bar.setVisibility(View.VISIBLE);
    }
    if (_keyboard_layout_view != null)
    {
      _keyboard_layout_view.setVisibility(View.VISIBLE);
      _keyboard_layout_view.setKeyboard(current_layout());
    }
    setInputView(_keyboard_container_view);
  }

  public void exitEmojiSearchMode()
  {
    _isEmojiSearchMode = false;
    _emojiSearchInputConnection = null;
    if (_emoji_search_bar != null)
    {
      _emoji_search_bar.setVisibility(View.GONE);
    }
    showEmojiPane();
  }

  public void showClipboardPane()
  {
    if (_clipboard_pane_view == null) return;
    int h = getTargetPaneHeight();
    int bottomSafety = getBottomSafety();
    _clipboard_pane_view.setKeyboard(this);
    _clipboard_pane_view.applyTheme(getColorKeyboard(), getColorKey(), getColorLabel(), getColorKeyActivated(), getColorSubLabel());
    _clipboard_pane_view.open(h, bottomSafety);
    _isClipboardPaneOpen = true;

    if (_candidates_view != null) _candidates_view.setVisibility(View.GONE);
    if (_keyboard_layout_view != null) _keyboard_layout_view.setVisibility(View.GONE);
    if (_ai_pane_view != null) _ai_pane_view.setVisibility(View.GONE);
    if (_fancy_pane_view != null) _fancy_pane_view.setVisibility(View.GONE);
    if (_tools_grid_pane != null) _tools_grid_pane.setVisibility(View.GONE);
    if (_emoji_search_bar != null) _emoji_search_bar.setVisibility(View.GONE);
    if (_clipboard_search_bar != null) _clipboard_search_bar.setVisibility(View.GONE);
    if (_clipboard_add_bar != null) _clipboard_add_bar.setVisibility(View.GONE);

    _clipboard_pane_view.setVisibility(View.VISIBLE);
  }

  public void closeClipboardPane()
  {
    _isClipboardPaneOpen = false;
    _isClipboardSearchMode = false;
    _clipboardSearchInputConnection = null;
    _isAddClipMode = false;
    _addClipInputConnection = null;

    if (_clipboard_pane_view != null) _clipboard_pane_view.setVisibility(View.GONE);
    if (_clipboard_search_bar != null) _clipboard_search_bar.setVisibility(View.GONE);
    if (_clipboard_add_bar != null) _clipboard_add_bar.setVisibility(View.GONE);

    if (_keyboard_layout_view != null)
    {
      _keyboard_layout_view.setVisibility(View.VISIBLE);
      _keyboard_layout_view.setKeyboard(current_layout());
    }
    refresh_candidates_view();
  }

  public void showAddClipMode()
  {
    _isAddClipMode = true;
    _isClipboardPaneOpen = false;

    if (_candidates_view != null) _candidates_view.setVisibility(View.GONE);
    if (_ai_pane_view != null) _ai_pane_view.setVisibility(View.GONE);
    if (_fancy_pane_view != null) _fancy_pane_view.setVisibility(View.GONE);
    if (_clipboard_pane_view != null) _clipboard_pane_view.setVisibility(View.GONE);
    if (_emoji_search_bar != null) _emoji_search_bar.setVisibility(View.GONE);
    if (_clipboard_search_bar != null) _clipboard_search_bar.setVisibility(View.GONE);

    if (_clipboard_add_bar != null)
    {
      _clipboard_add_bar.applyTheme(getColorKeyboard(), getColorKey(), getColorLabel(), getColorKeyActivated());
      _clipboard_add_bar.reset();
      _addClipInputConnection = _clipboard_add_bar.createInputConnection();
      _clipboard_add_bar.setVisibility(View.VISIBLE);
    }
    if (_keyboard_layout_view != null)
    {
      _keyboard_layout_view.setVisibility(View.VISIBLE);
      _keyboard_layout_view.setKeyboard(current_layout());
    }
  }

  public void exitAddClipMode(boolean reopenClipboard)
  {
    _isAddClipMode = false;
    _addClipInputConnection = null;
    if (_clipboard_add_bar != null)
    {
      _clipboard_add_bar.setVisibility(View.GONE);
    }
    if (reopenClipboard)
    {
      showClipboardPane();
    }
    else
    {
      if (_keyboard_layout_view != null)
      {
        _keyboard_layout_view.setVisibility(View.VISIBLE);
        _keyboard_layout_view.setKeyboard(current_layout());
      }
      refresh_candidates_view();
    }
  }

  public void showClipboardSearchMode(String query)
  {
    _isClipboardSearchMode = true;
    _isClipboardPaneOpen = false;
    _isAddClipMode = false;
    _addClipInputConnection = null;

    if (_candidates_view != null) _candidates_view.setVisibility(View.GONE);
    if (_ai_pane_view != null) _ai_pane_view.setVisibility(View.GONE);
    if (_fancy_pane_view != null) _fancy_pane_view.setVisibility(View.GONE);
    if (_clipboard_pane_view != null) _clipboard_pane_view.setVisibility(View.GONE);
    if (_ai_prompt_bar != null) _ai_prompt_bar.setVisibility(View.GONE);
    if (_emoji_search_bar != null) _emoji_search_bar.setVisibility(View.GONE);
    if (_clipboard_add_bar != null) _clipboard_add_bar.setVisibility(View.GONE);

    if (_clipboard_search_bar != null)
    {
      _clipboard_search_bar.applyTheme(getColorKeyboard(), getColorKey(), getColorLabel(), getColorKeyActivated());
      _clipboard_search_bar.setQuery(query);
      _clipboardSearchInputConnection = _clipboard_search_bar.createInputConnection();
      _clipboard_search_bar.setVisibility(View.VISIBLE);
    }
    if (_keyboard_layout_view != null)
    {
      _keyboard_layout_view.setVisibility(View.VISIBLE);
      _keyboard_layout_view.setKeyboard(current_layout());
    }
  }

  public void exitClipboardSearchMode()
  {
    _isClipboardSearchMode = false;
    _clipboardSearchInputConnection = null;
    if (_clipboard_search_bar != null)
    {
      _clipboard_search_bar.setVisibility(View.GONE);
    }
    if (_keyboard_layout_view != null)
    {
      _keyboard_layout_view.setVisibility(View.VISIBLE);
      _keyboard_layout_view.setKeyboard(current_layout());
    }
    refresh_candidates_view();
  }

  public void onClipSelectedFromSearch(String clipText)
  {
    exitClipboardSearchMode();
    if (clipText != null && !clipText.isEmpty())
    {
      ClipboardHistoryService.paste(clipText);
    }
  }

  public void onEmojiSelectedFromSearch(Emoji emoji)
  {
    if (emoji == null) return;
    InputConnection ic = getCurrentInputConnection();
    if (ic != null)
    {
      ic.commitText(emoji.kv().getString(), 1);
    }
    recordEmojiUsed(emoji);
  }

  public void recordEmojiUsed(Emoji emoji)
  {
    if (emoji == null) return;
    if (_emojiPane != null)
    {
      EmojiGridView grid = (EmojiGridView)_emojiPane.findViewById(R.id.emoji_grid);
      if (grid != null)
      {
        grid.recordEmojiUsed(emoji);
        return;
      }
    }
    try
    {
      SharedPreferences prefs = getSharedPreferences("emoji_last_use", Context.MODE_PRIVATE);
      Set<String> set = new HashSet<>(prefs.getStringSet("emoji_last_use", Collections.<String>emptySet()));
      set.add("1-" + emoji.kv().getString());
      prefs.edit().putStringSet("emoji_last_use", set).apply();
    }
    catch (Throwable ignored) {}
  }

  InputMethodManager get_imm()
  {
    return (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
  }

  private void refreshSubtypeImm()
  {
    _config.shouldOfferVoiceTyping = true;
    KeyboardData default_layout = null;
    _config.device_locales = DeviceLocales.load(this);
    if (_config.device_locales.default_ != null)
    {
      String layout_name = _config.device_locales.default_.default_layout;
      if (layout_name != null)
        default_layout = LayoutsPreference.layout_of_string(getResources(), layout_name);
    }
    _config.extra_keys_subtype = _config.device_locales.extra_keys();
    if (default_layout == null)
      default_layout = loadLayout(R.xml.latn_qwerty_us);
    _localeTextLayout = default_layout;
  }

  private void refresh_current_dictionary()
  {
    KeyboardData layout = current_layout_unmodified();
    String layoutScript = (layout != null && layout.script != null) ? layout.script.toLowerCase(java.util.Locale.ROOT) : "";
    String layoutName = (layout != null && layout.name != null) ? layout.name.toLowerCase(java.util.Locale.ROOT) : "";

    boolean isBengali = layoutScript.contains("beng") || layoutName.contains("বাংলা");
    boolean isLatin = layoutScript.contains("latin") || layoutScript.contains("latn") || (!isBengali && layoutScript.isEmpty());

    java.util.Set<String> installed = _dictionaries.get_installed();
    String dict_name = null;

    if (isBengali)
    {
      dict_name = "bn";
    }
    else if (isLatin)
    {
      boolean prefersUk = layoutName.contains("uk") || layoutName.contains("gb");
      if (prefersUk)
      {
        if (installed.contains("en_GB"))
          dict_name = "en_GB";
        else if (installed.contains("en_US"))
          dict_name = "en_US";
        else if (installed.contains("en_AU"))
          dict_name = "en_AU";
        else if (installed.contains("en_CA"))
          dict_name = "en_CA";
        else
          dict_name = "en_GB";
      }
      else
      {
        if (installed.contains("en_US"))
          dict_name = "en_US";
        else if (installed.contains("en_GB"))
          dict_name = "en_GB";
        else if (installed.contains("en_AU"))
          dict_name = "en_AU";
        else if (installed.contains("en_CA"))
          dict_name = "en_CA";
        else
          dict_name = "en_US";
      }
    }
    else
    {
      String userSelected = _dictionaries.get_selected(_config);
      if (userSelected != null && installed.contains(userSelected))
      {
        dict_name = userSelected;
      }
      else if (_config.device_locales != null && _config.device_locales.default_ != null)
      {
        dict_name = _config.device_locales.default_.dictionary;
      }
    }

    String userSelected = _dictionaries.get_selected(_config);
    if (userSelected != null && installed.contains(userSelected))
    {
      if (isBengali && "bn".equals(userSelected))
      {
        dict_name = userSelected;
      }
      else if (isLatin && userSelected.startsWith("en_"))
      {
        dict_name = userSelected;
      }
      else if (!isBengali && !isLatin)
      {
        dict_name = userSelected;
      }
    }

    _dictionaries.set_current_dictionary(_config, dict_name);
    _config.current_dictionary_name =
      SupportedDictionaries.get(getResources()).get_display_name(dict_name);
    _config.current_dictionary_short_name =
      compute_short_dictionary_code(dict_name, isBengali, isLatin, layoutName);
    boolean isAvro = layoutName.toLowerCase(java.util.Locale.ROOT).contains("avro")
        || layoutName.contains("অভ্র")
        || layoutName.toLowerCase(java.util.Locale.ROOT).contains("banglish")
        || layoutName.toLowerCase(java.util.Locale.ROOT).contains("phonetic")
        || layoutName.toLowerCase(java.util.Locale.ROOT).contains("bn • en")
        || layoutName.toLowerCase(java.util.Locale.ROOT).contains("bn-en");
    _config.is_avro_mode = isAvro;
    _config.is_bengali_mode = isBengali || isAvro || "BN".equalsIgnoreCase(_config.current_dictionary_short_name)
      || (dict_name != null && dict_name.toLowerCase(java.util.Locale.ROOT).startsWith("bn"));
    _config.should_show_dictionary_switch = true;
  }

  private String compute_short_dictionary_code(String dictName, boolean isBengali, boolean isLatin, String layoutName)
  {
    if (isBengali || (dictName != null && "bn".equalsIgnoreCase(dictName)))
    {
      return "BN";
    }
    if (dictName != null)
    {
      if ("en_GB".equalsIgnoreCase(dictName))
        return "UK";
      if ("en_US".equalsIgnoreCase(dictName))
        return "US";
      if ("en_AU".equalsIgnoreCase(dictName))
        return "AU";
      if ("en_CA".equalsIgnoreCase(dictName))
        return "CA";
      if ("en_IN".equalsIgnoreCase(dictName))
        return "IN";
      if (dictName.contains("_"))
      {
        String country = dictName.substring(dictName.indexOf('_') + 1).toUpperCase(java.util.Locale.ROOT);
        if ("GB".equals(country)) return "UK";
        if (country.length() <= 3) return country;
      }
      if (dictName.length() == 2)
      {
        return dictName.toUpperCase(java.util.Locale.ROOT);
      }
    }
    if (isLatin)
    {
      return (layoutName != null && (layoutName.contains("uk") || layoutName.contains("gb"))) ? "UK" : "US";
    }
    return (dictName != null && dictName.length() >= 2) ? dictName.substring(0, 2).toUpperCase(java.util.Locale.ROOT) : "";
  }

  /** Remember and apply the dictionary chosen by the user for the current
      context. */
  private void select_dictionary(String dict_name)
  {
    _dictionaries.set_selected(_config, dict_name);
    refresh_current_dictionary();
    refresh_candidates_view();
  }

  public void refresh_candidates_view()
  {
    if (isToolsPaneVisible())
    {
      if (_candidates_view != null)
      {
        _candidates_view.setVisibility(View.VISIBLE);
        _candidates_view.updateMoreButtonState(true);
      }
      return;
    }
    if (isAiPaneVisible() || isFancyPaneVisible() || _isEmojiSearchMode || _isAiSettingsOpen)
    {
      if (_candidates_view != null)
        _candidates_view.setVisibility(View.GONE);
      return;
    }
    boolean should_show =
      _config.suggestions_enabled
      && _config.editor_config.should_show_candidates_view
      && !_config.split_layout;
    if (should_show)
    {
      _candidates_view.refresh_config(_config);
      _keyeventhandler.dictionary_changed();
    }
    if (_candidates_view != null)
    {
      _candidates_view.setVisibility(should_show ? View.VISIBLE : View.GONE);
      if (should_show)
      {
        _candidates_view.onTextOrSelectionChanged();
      }
    }
  }

  /** Might re-create the keyboard view. [_keyboard_layout_view.setKeyboard()] and
      [setInputView()] must be called soon after. */
  private void refresh_config()
  {
    Config global = Config.globalConfig();
    if (global != null)
    {
      _config = global;
    }
    if (_config != null && _keyeventhandler != null)
    {
      _config.handler = _keyeventhandler;
      Config.setGlobalHandler(_keyeventhandler);
    }
    if (_keyboard_layout_view != null)
    {
      _keyboard_layout_view.setConfig(_config);
    }
    int prev_theme = _config.theme;
    _config.refresh(getResources(), _foldStateTracker.isUnfolded(), _dictionaries);
    refresh_current_dictionary();
    // Refreshing the theme config requires re-creating the views
    if (prev_theme != _config.theme)
    {
      try
      {
        create_keyboard_view();
        _emojiPane = null;
        _clipboard_pane = null;
        if (isInputViewShown())
        {
          setInputView(_keyboard_container_view);
        }
      }
      catch (Throwable t)
      {
        Logs.print_exception(t);
      }
    }
    // Set keyboard background
    CustomThemeStore customStore = CustomThemeStore.instance(this);
    if (customStore.isCustomThemeActive(_config))
    {
      if (customStore.hasGradient())
      {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] { customStore.getGradientStart(), customStore.getGradientEnd() });
        gd.setAlpha(_config.keyboardOpacity);
        _keyboard_container_view.setBackground(gd);
      }
      else
      {
        _keyboard_container_view.setBackgroundColor(customStore.getKeyboardBg());
      }
    }
    else if (_keyboard_layout_view != null && _keyboard_layout_view.getTheme() != null && _keyboard_layout_view.getTheme().hasKeyboardGradient)
    {
      android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
          android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
          new int[] { _keyboard_layout_view.getTheme().keyboardGradientStart, _keyboard_layout_view.getTheme().keyboardGradientEnd });
      gd.setAlpha(_config.keyboardOpacity);
      _keyboard_container_view.setBackground(gd);
    }
    else
    {
      Drawable bg = _keyboard_container_view.getBackground();
      if (bg != null)
      {
        Drawable mutated = bg.mutate();
        mutated.setAlpha(_config.keyboardOpacity);
        _keyboard_container_view.setBackground(mutated);
      }
    }
    _keyboard_layout_view.reset();
    refresh_candidates_view();
    if (_candidates_view != null)
    {
      _candidates_view.applyCustomTheme(_config);
    }
    if (_keyeventhandler != null)
    {
      _keyeventhandler.refresh_config(_config);
    }
  }

  private KeyboardData refresh_special_layout()
  {
    if (_config.editor_config.numeric_layout)
    {
      switch (_config.selected_number_layout)
      {
        case PIN:
          return loadPinentry(_config.orientation_landscape ?
              R.xml.pin_landscape : R.xml.pin);
        case NUMBER:
          return loadNumericLayout();
      }
    }
    return null;
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting)
  {
    if (_isAiSettingsOpen)
    {
      return;
    }
    Config global = Config.globalConfig();
    if (global != null)
    {
      _config = global;
    }
    if (_config != null && _keyeventhandler != null)
    {
      _config.handler = _keyeventhandler;
      Config.setGlobalHandler(_keyeventhandler);
    }
    if (_keyboard_layout_view != null)
    {
      _keyboard_layout_view.setConfig(_config);
    }
    _config.editor_config.refresh(info, getResources(), _config);
    refresh_config();
    _currentSpecialLayout = refresh_special_layout();
    _keyboard_layout_view.setKeyboard(current_layout());
    updateOneHandModeUI();
    _keyeventhandler.started(_config);
    setInputView(_keyboard_container_view);
    Logs.debug_startup_input_view(info, _config);
    ClipboardHistoryService hs = ClipboardHistoryService.get_service(this);
    if (hs != null)
    {
      hs.add_current_clip();
    }
  }

  @Override
  public void onWindowShown()
  {
    super.onWindowShown();
    ClipboardHistoryService hs = ClipboardHistoryService.get_service(this);
    if (hs != null)
    {
      hs.add_current_clip();
    }
  }

  @Override
  public void setInputView(View v)
  {
    ViewParent parent = v.getParent();
    if (parent != null && parent instanceof ViewGroup)
      ((ViewGroup)parent).removeView(v);
    super.setInputView(v);
    updateSoftInputWindowLayoutParams();
    v.requestApplyInsets();
  }

  @Override
  public void updateFullscreenMode() {
    super.updateFullscreenMode();
    updateSoftInputWindowLayoutParams();
  }

  private void updateSoftInputWindowLayoutParams() {
    final Window window = getWindow().getWindow();
    // On API >= 35, Keyboard2View behaves as edge-to-edge
    // APIs 30 to 34 have visual artifact when edge-to-edge is enabled
    if (VERSION.SDK_INT >= 35)
    {
      WindowManager.LayoutParams wattrs = window.getAttributes();
      wattrs.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
      // Allow to draw behind system bars
      wattrs.setFitInsetsTypes(0);
      window.setDecorFitsSystemWindows(false);
    }
    updateLayoutHeightOf(window, ViewGroup.LayoutParams.MATCH_PARENT);
    final View inputArea = window.findViewById(android.R.id.inputArea);

    updateLayoutHeightOf(
            (View) inputArea.getParent(),
            isFullscreenMode()
                    ? ViewGroup.LayoutParams.MATCH_PARENT
                    : ViewGroup.LayoutParams.WRAP_CONTENT);
    updateLayoutGravityOf((View) inputArea.getParent(), Gravity.BOTTOM);

  }

  private static void updateLayoutHeightOf(final Window window, final int layoutHeight) {
    final WindowManager.LayoutParams params = window.getAttributes();
    if (params != null && params.height != layoutHeight) {
      params.height = layoutHeight;
      window.setAttributes(params);
    }
  }

  private static void updateLayoutHeightOf(final View view, final int layoutHeight) {
    final ViewGroup.LayoutParams params = view.getLayoutParams();
    if (params != null && params.height != layoutHeight) {
      params.height = layoutHeight;
      view.setLayoutParams(params);
    }
  }

  private static void updateLayoutGravityOf(final View view, final int layoutGravity) {
    final ViewGroup.LayoutParams lp = view.getLayoutParams();
    if (lp instanceof LinearLayout.LayoutParams) {
      final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lp;
      if (params.gravity != layoutGravity) {
        params.gravity = layoutGravity;
        view.setLayoutParams(params);
      }
    } else if (lp instanceof FrameLayout.LayoutParams) {
      final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
      if (params.gravity != layoutGravity) {
        params.gravity = layoutGravity;
        view.setLayoutParams(params);
      }
    }
  }

  @Override
  public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype)
  {
    refreshSubtypeImm();
    refresh_current_dictionary();
    refresh_candidates_view();
    _keyboard_layout_view.setKeyboard(current_layout());
  }

  @Override
  public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd)
  {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    if (!_isTranslateMode)
    {
      _keyeventhandler.selection_updated(oldSelStart, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    }
    if ((oldSelStart == oldSelEnd) != (newSelStart == newSelEnd))
      _keyboard_layout_view.set_selection_state(newSelStart != newSelEnd);
    if (_candidates_view != null)
      _candidates_view.onTextOrSelectionChanged();
  }

  @Override
  public void onFinishInputView(boolean finishingInput)
  {
    super.onFinishInputView(finishingInput);
    if (_isAiSettingsOpen)
    {
      return;
    }
    finishInputSession();
    if (_isEmojiSearchMode)
    {
      _isEmojiSearchMode = false;
      _emojiSearchInputConnection = null;
      if (_emoji_search_bar != null) _emoji_search_bar.setVisibility(View.GONE);
    }
    if (_isClipboardSearchMode)
    {
      _isClipboardSearchMode = false;
      _clipboardSearchInputConnection = null;
      if (_clipboard_search_bar != null) _clipboard_search_bar.setVisibility(View.GONE);
    }
    if (_isAddClipMode)
    {
      _isAddClipMode = false;
      _addClipInputConnection = null;
      if (_clipboard_add_bar != null) _clipboard_add_bar.setVisibility(View.GONE);
    }
    if (_isClipboardPaneOpen)
    {
      closeClipboardPane();
    }
    if (isToolsPaneVisible())
      closeToolsPane();
    if (isAiPaneVisible())
      closeAiPane();
    _keyboard_layout_view.reset();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event)
  {
    if (keyCode == KeyEvent.KEYCODE_BACK)
    {
      if (_isTranslateMode)
      {
        hideTranslateBar();
        return true;
      }
      if (_isAddClipMode)
      {
        exitAddClipMode(true);
        return true;
      }
      if (_isClipboardSearchMode)
      {
        exitClipboardSearchMode();
        return true;
      }
      if (_isClipboardPaneOpen)
      {
        closeClipboardPane();
        return true;
      }
      if (_isEmojiSearchMode)
      {
        exitEmojiSearchMode();
        return true;
      }
      if (isAiPaneVisible())
      {
        closeAiPane();
        return true;
      }
      if (isToolsPaneVisible())
      {
        closeToolsPane();
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences _prefs, String _key)
  {
    try
    {
      refresh_config();
      if (_keyboard_layout_view != null)
      {
        _keyboard_layout_view.setKeyboard(current_layout());
      }
      updateOneHandModeUI();
    }
    catch (Throwable t)
    {
      Logs.print_exception(t);
    }
  }

  @Override
  public boolean onEvaluateFullscreenMode()
  {
    /* Entirely disable fullscreen mode. */
    return false;
  }

  @Override
  public boolean onEvaluateInputViewShown()
  {
    // Since Android 16, this method returns [false] for unknown reasons.
    if (super.onEvaluateInputViewShown())
      return true;
    if (getResources().getConfiguration().hardKeyboardHidden
        == Configuration.HARDKEYBOARDHIDDEN_NO
        && _config.physical_keyboard_hide)
    {
      Logs.debug("Physical keyboard is present");
      return false;
    }
    return true;
  }

  public void launch_dictionaries_activity()
  {
    start_activity(DictionariesActivity.class);
  }

  /** Called from [onClick] attributes. */
  public void launch_dictionaries_activity(View v)
  {
    launch_dictionaries_activity();
  }

  void start_activity(Class cls)
  {
    Intent intent = new Intent(this, cls);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  /** Not static */
  public class Receiver implements KeyEventHandler.IReceiver,
         KeyValue.Stateful.Symbol_provider, DictionarySwitcher.Callback
  {
    public void handle_event_key(KeyValue.Event ev)
    {
      switch (ev)
      {
        case CONFIG:
          start_activity(SettingsActivity.class);
          break;

        case SWITCH_TEXT:
          _currentSpecialLayout = null;
          _keyboard_layout_view.setKeyboard(current_layout());
          break;

        case SWITCH_NUMERIC:
          setSpecialLayout(loadNumericLayout());
          break;

        case SWITCH_EMOJI:
          showEmojiPane();
          break;

        case SWITCH_CLIPBOARD:
          showClipboardPane();
          break;

        case SWITCH_BACK_EMOJI:
          if (_isEmojiSearchMode)
          {
            _isEmojiSearchMode = false;
            _emojiSearchInputConnection = null;
            if (_emoji_search_bar != null) _emoji_search_bar.setVisibility(View.GONE);
          }
          if (_keyboard_layout_view != null)
          {
            _keyboard_layout_view.setVisibility(View.VISIBLE);
            _keyboard_layout_view.setKeyboard(current_layout());
          }
          setInputView(_keyboard_container_view);
          refresh_candidates_view();
          break;

        case SWITCH_BACK_CLIPBOARD:
          closeClipboardPane();
          break;

        case CHANGE_METHOD_PICKER:
          get_imm().showInputMethodPicker();
          break;

        case CHANGE_METHOD_PREV:
          if (VERSION.SDK_INT < 28)
            get_imm().switchToLastInputMethod(getConnectionToken());
          else
            switchToPreviousInputMethod();
          break;

        case CHANGE_METHOD_NEXT:
          if (VERSION.SDK_INT < 28)
            get_imm().switchToNextInputMethod(getConnectionToken(), false);
          else
            switchToNextInputMethod(false);
          break;

        case ACTION:
          InputConnection conn = getCurrentInputConnection();
          if (conn != null)
            conn.performEditorAction(_config.editor_config.actionId);
          break;

        case SWITCH_FORWARD:
          incrTextLayout(1);
          break;

        case SWITCH_BACKWARD:
          incrTextLayout(-1);
          break;

        case SWITCH_GREEKMATH:
          setSpecialLayout(loadNumpad(R.xml.greekmath));
          break;

        case CAPS_LOCK:
          set_shift_state(true, true);
          break;

        case SWITCH_VOICE_TYPING:
          if (_candidates_view != null)
          {
            _candidates_view.onVoiceListeningStarted();
          }
          typodev.keyboard.voice.OfflineVoiceTypingService.toggleListening(Keyboard2.this, Keyboard2.this);
          break;

        case SWITCH_VOICE_TYPING_CHOOSER:
          VoiceImeSwitcher.choose_voice_ime(Keyboard2.this, get_imm(),
              Config.globalPrefs());
          break;

        case HIDE_SELF:
          Keyboard2.this.requestHideSelf(0);
          break;

        case CHANGE_DICTIONARY:
          new DictionarySwitcher(Keyboard2.this, _dictionaries, this).choose();
          break;
      }
    }

    public void set_shift_state(boolean state, boolean lock)
    {
      _keyboard_layout_view.set_shift_state(state, lock);
    }

    public void set_compose_pending(boolean pending)
    {
      _keyboard_layout_view.set_compose_pending(pending);
    }

    public void selection_state_changed(boolean selection_is_ongoing)
    {
      _keyboard_layout_view.set_selection_state(selection_is_ongoing);
    }

    public InputConnection getCurrentInputConnection()
    {
      return Keyboard2.this.getCurrentInputConnection();
    }

    public Handler getHandler()
    {
      return _handler;
    }

    public void set_suggestions(Suggestions suggestions)
    {
      _candidates_view.set_candidates(suggestions);
    }

    @Override
    public CharSequence getTextBeforeCursor(int n, int flags)
    {
      InputConnection conn = getCurrentInputConnection();
      if (conn != null)
      {
        return conn.getTextBeforeCursor(n, flags);
      }
      return null;
    }

    @Override
    public boolean isSelectionActive()
    {
      return _keyeventhandler != null && _keyeventhandler.is_selection_not_empty();
    }

    public String provide_stateful_key_symbol(KeyValue.Stateful q)
    {
      switch (q)
      {
        case Complete_first: return _suggestions.suggestions[0];
        case Complete_second: return _suggestions.suggestions[1];
        case Complete_third: return _suggestions.suggestions[2];
        case Complete_emoji: return _suggestions.emoji_suggestion;
      }
      return "";
    }

    public void on_change_dictionary(String dict_name)
    {
      select_dictionary(dict_name);
    }

    public void launch_dictionaries_activity()
    {
      Keyboard2.this.launch_dictionaries_activity();
    }
  }

  private IBinder getConnectionToken()
  {
    return getWindow().getWindow().getAttributes().token;
  }

  private View inflate_view(int layout)
  {
    return View.inflate(new ContextThemeWrapper(this, _config.theme), layout, null);
  }

  @Override
  public android.view.inputmethod.InlineSuggestionsRequest onCreateInlineSuggestionsRequest(android.os.Bundle uiExtras)
  {
    if (android.os.Build.VERSION.SDK_INT < 30)
      return null;

    float density = getResources().getDisplayMetrics().density;
    int minHeight = Math.max(1, Math.round(28 * density));
    int maxHeight = Math.max(1, Math.round(44 * density));
    int minWidth = Math.max(1, Math.round(60 * density));
    int maxWidth = Math.max(1, Math.round(400 * density));

    android.util.Size minSize = new android.util.Size(minWidth, minHeight);
    android.util.Size maxSize = new android.util.Size(maxWidth, maxHeight);

    android.widget.inline.InlinePresentationSpec spec =
      new android.widget.inline.InlinePresentationSpec.Builder(minSize, maxSize).build();

    java.util.List<android.widget.inline.InlinePresentationSpec> specs = new java.util.ArrayList<>();
    specs.add(spec);

    return new android.view.inputmethod.InlineSuggestionsRequest.Builder(specs)
      .setMaxSuggestionCount(6)
      .build();
  }

  @Override
  public boolean onInlineSuggestionsResponse(android.view.inputmethod.InlineSuggestionsResponse response)
  {
    if (android.os.Build.VERSION.SDK_INT < 30 || _candidates_view == null)
      return false;

    java.util.List<android.view.inputmethod.InlineSuggestion> inlineSuggestions = response.getInlineSuggestions();
    final long sessionId = _inputSessionId;
    final long responseId = ++_inlineResponseId;
    final CandidatesView candidatesView = _candidates_view;
    if (inlineSuggestions.isEmpty())
    {
      _candidates_view.setInlineSuggestions(null);
      return false;
    }

    final int total = inlineSuggestions.size();
    final java.util.List<android.view.View> suggestionViews = new java.util.ArrayList<>(
        java.util.Collections.nCopies(total, (android.view.View)null));
    final java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

    float density = getResources().getDisplayMetrics().density;
    int targetHeight = Math.max(1, Math.round(34 * density));
    final android.util.Size size = new android.util.Size(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, targetHeight);

    for (int index = 0; index < total; index++)
    {
      final int suggestionIndex = index;
      android.view.inputmethod.InlineSuggestion suggestion = inlineSuggestions.get(index);
      try
      {
        suggestion.inflate(this, size, getMainExecutor(), new java.util.function.Consumer<android.widget.inline.InlineContentView>()
        {
          @Override
          public void accept(android.widget.inline.InlineContentView view)
          {
            if (sessionId != _inputSessionId || responseId != _inlineResponseId
                || candidatesView != _candidates_view)
              return;
            if (view != null)
              suggestionViews.set(suggestionIndex, view);
            if (count.incrementAndGet() == total)
            {
              suggestionViews.removeAll(java.util.Collections.singleton(null));
              _candidates_view.setInlineSuggestions(suggestionViews);
            }
          }
        });
      }
      catch (Throwable t)
      {
        if (count.incrementAndGet() == total)
        {
          suggestionViews.removeAll(java.util.Collections.singleton(null));
          _candidates_view.setInlineSuggestions(suggestionViews);
        }
      }
    }
    return true;
  }

  public String getCurrentLanguageCode()
  {
    try
    {
      KeyboardData layout = current_layout_unmodified();
      String layoutScript = (layout != null && layout.script != null) ? layout.script.toLowerCase(java.util.Locale.ROOT) : "";
      String layoutName = (layout != null && layout.name != null) ? layout.name.toLowerCase(java.util.Locale.ROOT) : "";

      if (layoutScript.contains("beng") || layoutName.contains("বাংলা") || layoutName.contains("bengali"))
      {
        return "bn-BD";
      }
    }
    catch (Throwable ignored) {}
    return "en-US";
  }

  public CandidatesView getCandidatesView()
  {
    return _candidates_view;
  }

  public KeyEventHandler getKeyEventHandler()
  {
    return _keyeventhandler;
  }

  public Suggestions getSuggestions()
  {
    return _suggestions;
  }

  public void setVoiceComposingText(String text)
  {
    if (text == null) return;
    android.view.inputmethod.InputConnection ic = getCurrentInputConnection();
    if (ic != null)
    {
      ic.setComposingText(text, 1);
    }
  }

  public void commitVoiceSegment(String text)
  {
    if (text == null || text.trim().isEmpty()) return;
    String trimmed = text.trim();
    android.view.inputmethod.InputConnection ic = getCurrentInputConnection();
    if (ic != null)
    {
      ic.commitText(trimmed + " ", 1);
    }
    // Update word prediction and sentence learning context
    if (_suggestions != null)
    {
      _suggestions.set_last_word(trimmed);
      try
      {
        CharSequence before = (ic != null) ? ic.getTextBeforeCursor(120, 0) : null;
        String contextStr = (before != null) ? before.toString() : trimmed;
        typodev.keyboard.suggestions.NextWordPredictor predictor =
            typodev.keyboard.suggestions.NextWordPredictor.instance(this);
        predictor.learnSentence(contextStr);
        _suggestions.predict_sentence_completion(contextStr);
      }
      catch (Throwable ignored) {}
    }
  }

  public void commitVoiceText(String text)
  {
    commitVoiceSegment(text);
  }

  public void finishVoiceTyping()
  {
    android.view.inputmethod.InputConnection ic = getCurrentInputConnection();
    if (ic != null)
    {
      ic.finishComposingText();
    }
    if (_candidates_view != null)
    {
      _candidates_view.onVoiceListeningStopped();
    }
  }

  public boolean replaceVoiceSessionText(String oldText, String newText)
  {
    if (oldText == null || newText == null) return false;
    android.view.inputmethod.InputConnection ic = getCurrentInputConnection();
    if (ic == null) return false;

    try
    {
      ic.beginBatchEdit();
      int lookback = Math.min(oldText.length() + 100, 500);
      CharSequence before = ic.getTextBeforeCursor(lookback, 0);
      if (before != null)
      {
        String beforeStr = before.toString();
        String trimmedOld = oldText.trim();
        String trimmedNew = newText.trim();

        if (beforeStr.endsWith(oldText + " "))
        {
          ic.deleteSurroundingText(oldText.length() + 1, 0);
          ic.commitText(trimmedNew + " ", 1);
          ic.endBatchEdit();
          return true;
        }
        else if (beforeStr.endsWith(oldText))
        {
          ic.deleteSurroundingText(oldText.length(), 0);
          ic.commitText(trimmedNew + " ", 1);
          ic.endBatchEdit();
          return true;
        }
        else if (beforeStr.trim().endsWith(trimmedOld))
        {
          int spaces = beforeStr.length() - beforeStr.replaceAll("\\s+$", "").length();
          ic.deleteSurroundingText(trimmedOld.length() + spaces, 0);
          ic.commitText(trimmedNew + " ", 1);
          ic.endBatchEdit();
          return true;
        }
      }
      ic.endBatchEdit();
    }
    catch (Throwable ignored)
    {
      try { ic.endBatchEdit(); } catch (Throwable ignored2) {}
    }
    return false;
  }
}
