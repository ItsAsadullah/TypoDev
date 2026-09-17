package juloo.keyboard2.suggestions;

import android.content.Context;
import android.os.Build.VERSION;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.LayoutInflater;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import juloo.keyboard2.Config;
import juloo.keyboard2.KeyValue;
import juloo.keyboard2.Pointers;
import juloo.keyboard2.R;

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
  private juloo.keyboard2.Keyboard2 _keyboard2;
  private TextView _btn_toolbar_toggle;
  private HorizontalScrollView _suggestions_scroll;
  private LinearLayout _suggestions_container;
  private TextView _emoji_view;
  private View _toolbar_scroll;
  private View _autofill_scroll;
  private LinearLayout _autofill_container;
  private boolean _toolbar_open = true;
  private boolean _has_suggestions = false;

  public void set_keyboard2(juloo.keyboard2.Keyboard2 keyboard2)
  {
    _keyboard2 = keyboard2;
  }

  public CandidatesView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate()
  {
    super.onFinishInflate();
    setup_dictionary_switch_button();
    _lang_name_view = (TextView)findViewById(R.id.candidates_lang_name);

    _btn_toolbar_toggle = (TextView)findViewById(R.id.btn_toolbar_toggle);
    _suggestions_scroll = (HorizontalScrollView)findViewById(R.id.suggestions_scroll);
    _suggestions_container = (LinearLayout)findViewById(R.id.suggestions_container);
    _toolbar_scroll = findViewById(R.id.toolbar_scroll);
    _autofill_scroll = findViewById(R.id.autofill_scroll);
    _autofill_container = (LinearLayout)findViewById(R.id.autofill_container);

    _emoji_view = (TextView)findViewById(R.id.candidates_emoji);
    _item_views[MAX_WORD_CANDIDATES] = _emoji_view;
    if (_emoji_view != null)
    {
      _emoji_view.setVisibility(View.GONE);
      _emoji_view.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View _v)
        {
          String it = _items[MAX_WORD_CANDIDATES];
          if (it != null)
            Config.globalConfig().handler.suggestion_entered(it);
        }
      });
    }

    LayoutInflater inflater = LayoutInflater.from(getContext());
    for (int i = 0; i < MAX_WORD_CANDIDATES; i++)
    {
      final int index = i;
      TextView v = (TextView)inflater.inflate(R.layout.candidates_chip, _suggestions_container, false);
      v.setVisibility(View.GONE);
      v.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View _v)
        {
          String it = _items[index];
          if (it != null)
            Config.globalConfig().handler.suggestion_entered(it);
        }
      });
      _suggestions_container.addView(v);
      _item_views[i] = v;
    }

    setup_toolbar_buttons();
  }

  private void setup_toolbar_buttons()
  {
    if (_btn_toolbar_toggle != null)
    {
      _btn_toolbar_toggle.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          _toolbar_open = !_toolbar_open;
          update_view_visibility(_has_suggestions);
        }
      });
    }

    View btnAi = findViewById(R.id.toolbar_btn_ai);
    if (btnAi != null)
    {
      btnAi.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (_keyboard2 != null)
          {
            if (_keyboard2.isAiPaneVisible())
              _keyboard2.closeAiPane();
            else
              _keyboard2.showAiPane();
          }
        }
      });
    }

    View btnTranslate = findViewById(R.id.toolbar_btn_translate);
    if (btnTranslate != null)
    {
      btnTranslate.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (_keyboard2 != null)
            juloo.keyboard2.translate.TranslationDialog.showTranslateMenu(getContext(), _keyboard2);
        }
      });
    }

    View btnClipboard = findViewById(R.id.toolbar_btn_clipboard);
    if (btnClipboard != null)
    {
      btnClipboard.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          Config.globalConfig().handler.key_up(
              KeyValue.getKeyByName("switch_clipboard"),
              Pointers.Modifiers.EMPTY);
        }
      });
    }

    View btnVoice = findViewById(R.id.toolbar_btn_voice);
    if (btnVoice != null)
    {
      btnVoice.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (_keyboard2 != null)
          {
            juloo.keyboard2.voice.OfflineVoiceTypingService.toggleListening(getContext(), _keyboard2);
          }
          else
          {
            Config.globalConfig().handler.key_up(
                KeyValue.getKeyByName("voice_typing"),
                Pointers.Modifiers.EMPTY);
          }
        }
      });
    }

    View btnSettings = findViewById(R.id.toolbar_btn_settings);
    if (btnSettings != null)
    {
      btnSettings.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          Config.globalConfig().handler.key_up(
              KeyValue.getKeyByName("config"),
              Pointers.Modifiers.EMPTY);
        }
      });
    }
  }

  public void setInlineSuggestions(List<View> views)
  {
    if (_autofill_container == null) return;
    _autofill_container.removeAllViews();
    if (views != null && !views.isEmpty())
    {
      for (View v : views)
        _autofill_container.addView(v);
      if (_autofill_scroll != null) _autofill_scroll.setVisibility(View.VISIBLE);
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
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
      if (juloo.keyboard2.dict.ExternalDictionaryManager.instance(getContext()).getWordCount() > 0)
        return true;
    }
    catch (Exception ignored) {}
    return false;
  }

  private void update_view_visibility(boolean hasSuggestions)
  {
    if (_autofill_scroll != null && _autofill_scroll.getVisibility() == View.VISIBLE)
      return;

    if (_toolbar_open)
    {
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.GONE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.GONE);
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.VISIBLE);
      if (_status_no_dict != null) _status_no_dict.setVisibility(View.GONE);
      if (_btn_toolbar_toggle != null) _btn_toolbar_toggle.setText("‹");
    }
    else
    {
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.VISIBLE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.VISIBLE);
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
      if (_btn_toolbar_toggle != null) _btn_toolbar_toggle.setText("›");
      if (_status_no_dict != null)
      {
        _status_no_dict.setVisibility((has_valid_dictionary() || hasSuggestions) ? View.GONE : View.VISIBLE);
      }
    }
  }

  public void set_candidates(Suggestions s)
  {
    int s_count = s.count;
    _has_suggestions = (s_count > 0 || s.emoji_suggestion != null);
    if (_has_suggestions)
    {
      _toolbar_open = false;
    }
    for (int i = 0; i < MAX_WORD_CANDIDATES; i++)
      _items[i] = (i < s_count) ? s.suggestions[i] : null;
    _items[MAX_WORD_CANDIDATES] = s.emoji_suggestion;

    if (_status_no_dict != null)
    {
      if (s_count != 0 || has_valid_dictionary())
        _status_no_dict.setVisibility(View.GONE);
    }

    boolean useWeight = (s_count > 0 && s_count <= 3 && s.emoji_suggestion == null);
    for (int i = 0; i < MAX_WORD_CANDIDATES; i++)
    {
      TextView v = _item_views[i];
      if (v == null) continue;
      if (_items[i] != null)
      {
        v.setText(_items[i]);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)v.getLayoutParams();
        if (lp != null)
        {
          if (useWeight)
          {
            lp.width = 0;
            lp.weight = 1.0f;
          }
          else
          {
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.weight = 0.0f;
          }
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

    int dict_vis =
      (should_show_dictionary_switch && s.count == 0 && !_toolbar_open) ? View.VISIBLE : View.GONE;
    if (_dictionary_switch_button != null) _dictionary_switch_button.setVisibility(dict_vis);
    if (_lang_name_view != null) _lang_name_view.setVisibility(dict_vis);
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
    update_view_visibility(false);
  }

  public void refresh_config(Config config)
  {
    _config = config;
    clear_candidates();
    // The status message indicates whether the dictionaries should be
    // installed.
    if (!has_valid_dictionary())
      inflate_status_no_dict(config);
    else if (_status_no_dict != null)
      _status_no_dict.setVisibility(View.GONE);
    should_show_dictionary_switch = config.should_show_dictionary_switch;
    set_sizes(config);
    _lang_name_view.setText(config.current_dictionary_name);
  }

  /** Set the height of the suggestion row and the text size. */
  void set_sizes(Config config)
  {
    // Make the candidates view about as high as a keyboard row.
    float row_height = config.keyboard_rows_height_pixels * (1 - config.key_vertical_margin);
    ViewGroup.MarginLayoutParams p =
      (ViewGroup.MarginLayoutParams)getLayoutParams();
    p.height = (int)row_height;
    setLayoutParams(p);
    // Match the size of labels on the keyboard.
    float text_size = row_height * config.characterSize * config.labelTextSize;
    for (int i = 0; i < TOTAL_CANDIDATES; i++)
    {
      TextView v = _item_views[i];
      if (v == null) continue;
      v.setTextSize(TypedValue.COMPLEX_UNIT_PX, text_size);
    }
  }

  void inflate_status_no_dict(Config config)
  {
    if (has_valid_dictionary())
    {
      if (_status_no_dict != null)
        _status_no_dict.setVisibility(View.GONE);
      return;
    }
    if (_status_no_dict == null)
    {
      _status_no_dict = View.inflate(getContext(),
          R.layout.candidates_status_no_dict, null);
      addView(_status_no_dict);
    }
    Locale current_locale = (config.device_locales.default_ != null) ?
      Locale.forLanguageTag(config.device_locales.default_.lang_tag) : null;
    TextView tv = _status_no_dict.findViewById(android.R.id.text1);
    if (tv != null && current_locale != null)
      tv.setText(getResources().getString(
            R.string.candidates_status_click_to_install,
            current_locale.getDisplayName()));
    _status_no_dict.setVisibility(View.VISIBLE);
  }

  void setup_dictionary_switch_button()
  {
    _dictionary_switch_button = findViewById(R.id.dictionary_switch);
    _dictionary_switch_button.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View _v)
          {
            Config.globalConfig().handler.key_up(
                KeyValue.getKeyByName("change_dictionary"),
                Pointers.Modifiers.EMPTY);
          }
        });
  }

  /** Whether the candidates view should be shown for a given editor. */
  public static boolean should_show(EditorInfo info)
  {
    int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
    int flags = info.inputType & InputType.TYPE_MASK_FLAGS;
    switch (info.inputType & InputType.TYPE_MASK_CLASS)
    {
      case InputType.TYPE_CLASS_TEXT:
        switch (variation)
        {
          case InputType.TYPE_TEXT_VARIATION_PASSWORD:
          case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
          case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
            return false;
          default:
            /* Editor requested that we don't show suggestions. Enable
               suggestions anyway when the flags [NO_SUGGESTIONS] and
               [AUTO_CORRECT] are present at the same time. This happens with
               Google Keep. */
            if ((flags &
                  (InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                   | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT))
                == InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
              return false;
            return true;
        }
      case InputType.TYPE_CLASS_NUMBER:
        // Beware of TYPE_NUMBER_VARIATION_PASSWORD
        return false;
      default: return false;
    }
  }
}
