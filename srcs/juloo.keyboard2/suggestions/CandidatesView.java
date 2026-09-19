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
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import juloo.keyboard2.Config;
import juloo.keyboard2.KeyValue;
import juloo.keyboard2.Logs;
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
  private View _btn_magic_container;
  private TextView _btn_magic_autocomplete;
  private ProgressBar _magic_progress;
  private Button _btn_voice;
  private TextView _voice_live_banner;
  private TextView _btn_candidate_mic;
  private View _btn_candidate_mic_container;
  private boolean _is_voice_typing_active = false;
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
    if (_lang_name_view != null)
    {
      _lang_name_view.setOnClickListener(new View.OnClickListener()
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

    _btn_toolbar_toggle = (TextView)findViewById(R.id.btn_toolbar_toggle);
    _suggestions_scroll = (HorizontalScrollView)findViewById(R.id.suggestions_scroll);
    _suggestions_container = (LinearLayout)findViewById(R.id.suggestions_container);
    _toolbar_scroll = findViewById(R.id.toolbar_scroll);
    _autofill_scroll = findViewById(R.id.autofill_scroll);
    _autofill_container = (LinearLayout)findViewById(R.id.autofill_container);
    _voice_live_banner = (TextView)findViewById(R.id.voice_live_banner);
    _btn_candidate_mic = (TextView)findViewById(R.id.btn_candidate_mic);
    _btn_candidate_mic_container = findViewById(R.id.btn_candidate_mic_container);

    View.OnClickListener micClickListener = new View.OnClickListener()
    {
      @Override
      public void onClick(View v)
      {
        if (_is_voice_typing_active)
        {
          juloo.keyboard2.voice.OfflineVoiceTypingService.stopListening();
          onVoiceListeningStopped();
          return;
        }
        onVoiceListeningStarted();
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
    };

    if (_btn_candidate_mic != null)
    {
      _btn_candidate_mic.setOnClickListener(micClickListener);
    }
    if (_btn_candidate_mic_container != null)
    {
      _btn_candidate_mic_container.setOnClickListener(micClickListener);
    }


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

    _btn_magic_container = findViewById(R.id.btn_magic_autocomplete_container);
    _btn_magic_autocomplete = (TextView)findViewById(R.id.btn_magic_autocomplete);
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
          try
          {
            if (_keyboard2 != null)
            {
              if (_keyboard2.isAiPaneVisible())
                _keyboard2.closeAiPane();
              else
                _keyboard2.showAiPane();
            }
          }
          catch (Throwable t)
          {
            Logs.print_exception(t);
          }
        }
      });
    }

    View btnFancy = findViewById(R.id.toolbar_btn_fancy);
    if (btnFancy != null)
    {
      btnFancy.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          try
          {
            if (_keyboard2 != null)
            {
              if (_keyboard2.isFancyPaneVisible())
                _keyboard2.closeFancyPane();
              else
                _keyboard2.showFancyPane();
            }
          }
          catch (Throwable t)
          {
            Logs.print_exception(t);
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

    _btn_voice = findViewById(R.id.toolbar_btn_voice);
    if (_btn_voice != null)
    {
      _btn_voice.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (_is_voice_typing_active)
          {
            juloo.keyboard2.voice.OfflineVoiceTypingService.stopListening();
            onVoiceListeningStopped();
            return;
          }
          onVoiceListeningStarted();
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

    View btnEmoji = findViewById(R.id.toolbar_btn_emoji);
    if (btnEmoji != null)
    {
      btnEmoji.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (_keyboard2 != null)
          {
            _keyboard2.showEmojiPaneFromToolbar();
          }
        }
      });
    }

    final Button btnDev = findViewById(R.id.toolbar_btn_dev);
    if (btnDev != null)
    {
      updateDevButtonState(btnDev);
      btnDev.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          if (_config != null)
          {
            _config.developer_mode = !_config.developer_mode;
            try
            {
              android.preference.PreferenceManager.getDefaultSharedPreferences(getContext())
                  .edit().putBoolean("developer_mode", _config.developer_mode).apply();
            }
            catch (Throwable ignored) {}
            updateDevButtonState(btnDev);
            String msg = _config.developer_mode ? "👨‍💻 Developer Mode: ON" : "👨‍💻 Developer Mode: OFF";
            android.widget.Toast.makeText(getContext(), msg, android.widget.Toast.LENGTH_SHORT).show();
            if (_keyboard2 != null)
            {
              _keyboard2.refresh_candidates_view();
            }
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

  private void updateDevButtonState(Button btnDev)
  {
    if (btnDev == null) return;
    boolean isDev = (_config != null && _config.developer_mode);
    btnDev.setText(isDev ? "👨‍💻 Dev: ON" : "👨‍💻 Dev");
    btnDev.setAlpha(isDev ? 1.0f : 0.7f);
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
    if (_dictionary_switch_button != null) _dictionary_switch_button.setVisibility(View.GONE);
    if (_lang_name_view != null) _lang_name_view.setVisibility(View.GONE);

    if (_is_voice_typing_active)
    {
      if (_btn_toolbar_toggle != null) _btn_toolbar_toggle.setVisibility(View.GONE);
      if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
      if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.VISIBLE);
      if (_suggestions_container != null) _suggestions_container.setVisibility(View.VISIBLE);
      if (_autofill_scroll != null) _autofill_scroll.setVisibility(View.GONE);
      if (_status_no_dict != null) _status_no_dict.setVisibility(View.GONE);
      if (_btn_magic_container != null) _btn_magic_container.setVisibility(View.GONE);
      else if (_btn_magic_autocomplete != null) _btn_magic_autocomplete.setVisibility(View.GONE);
      if (_btn_candidate_mic_container != null) _btn_candidate_mic_container.setVisibility(View.VISIBLE);
      else if (_btn_candidate_mic != null) _btn_candidate_mic.setVisibility(View.VISIBLE);
      return;
    }

    if (_btn_toolbar_toggle != null)
    {
      _btn_toolbar_toggle.setVisibility(View.VISIBLE);
    }

    if (_autofill_scroll != null && _autofill_scroll.getVisibility() == View.VISIBLE)
    {
      if (_btn_magic_container != null) _btn_magic_container.setVisibility(View.GONE);
      else if (_btn_magic_autocomplete != null) _btn_magic_autocomplete.setVisibility(View.GONE);
      if (_btn_candidate_mic_container != null) _btn_candidate_mic_container.setVisibility(View.GONE);
      else if (_btn_candidate_mic != null) _btn_candidate_mic.setVisibility(View.GONE);
      return;
    }

    if (_btn_magic_container != null) _btn_magic_container.setVisibility(View.VISIBLE);
    else if (_btn_magic_autocomplete != null) _btn_magic_autocomplete.setVisibility(View.VISIBLE);
    if (_btn_candidate_mic_container != null) _btn_candidate_mic_container.setVisibility(View.VISIBLE);
    else if (_btn_candidate_mic != null) _btn_candidate_mic.setVisibility(View.VISIBLE);

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
        _status_no_dict.setVisibility(View.GONE);
      }
    }
  }

  public void set_candidates(Suggestions s)
  {
    if (_is_voice_typing_active || juloo.keyboard2.voice.OfflineVoiceTypingService.isListening())
    {
      return;
    }
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
        if (i == 0 && s.should_autocorrect)
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
    update_view_visibility(false);
  }

  public void refresh_config(Config config)
  {
    _config = config;
    clear_candidates();
    if (_status_no_dict != null)
      _status_no_dict.setVisibility(View.GONE);
    should_show_dictionary_switch = config.should_show_dictionary_switch;
    set_sizes(config);
    update_lang_name_view();
    updateDevButtonState((Button)findViewById(R.id.toolbar_btn_dev));
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
    // Make the candidates view about as high as a keyboard row.
    float row_height = config.keyboard_rows_height_pixels * (1 - config.key_vertical_margin);
    ViewGroup.MarginLayoutParams p =
      (ViewGroup.MarginLayoutParams)getLayoutParams();
    p.height = (int)row_height;
    setLayoutParams(p);
    // Suggestion chips use a sleek, compact font size matching modern keyboards (scaled to ~0.70x of key letters)
    float text_size = row_height * config.characterSize * config.labelTextSize * 0.70f;
    for (int i = 0; i < MAX_WORD_CANDIDATES; i++)
    {
      TextView v = _item_views[i];
      if (v == null) continue;
      v.setTextSize(TypedValue.COMPLEX_UNIT_PX, text_size);
    }
    if (_emoji_view != null)
    {
      _emoji_view.setTextSize(TypedValue.COMPLEX_UNIT_PX, row_height * config.characterSize * 0.85f);
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

    String apiKey = juloo.keyboard2.ai.GeminiAiService.getApiKey(getContext());
    if (apiKey != null && !apiKey.isEmpty())
    {
      juloo.keyboard2.ai.GeminiAiService.processAutocomplete(getContext(), text, new juloo.keyboard2.ai.GeminiAiService.AiCallback()
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
    juloo.keyboard2.suggestions.NextWordPredictor predictor =
        juloo.keyboard2.suggestions.NextWordPredictor.instance(getContext());
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
          Config.globalConfig().handler.key_up(
              KeyValue.getKeyByName("change_dictionary"),
              Pointers.Modifiers.EMPTY);
        }
      });
    }
  }

  /** Whether the candidates view should be shown for a given editor. */
  public static boolean should_show(EditorInfo info)
  {
    return should_show(info, false);
  }

  public static boolean should_show(EditorInfo info, boolean showInTerminals)
  {
    if (info == null) return false;
    int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
    int flags = info.inputType & InputType.TYPE_MASK_FLAGS;
    int inputClass = info.inputType & InputType.TYPE_MASK_CLASS;

    // Password fields should never show suggestions
    switch (variation)
    {
      case InputType.TYPE_TEXT_VARIATION_PASSWORD:
      case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
      case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
        return false;
      default:
        break;
    }

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
      return showInTerminals;
    }

    switch (inputClass)
    {
      case InputType.TYPE_CLASS_TEXT:
        // Always show candidate bar and toolbar for text fields, including
        // browser URL bars (TYPE_TEXT_VARIATION_URI), web forms (TYPE_TEXT_VARIATION_WEB_EDIT_TEXT),
        // and editors that set TYPE_TEXT_FLAG_NO_SUGGESTIONS (e.g. Chrome omnibox, web search).
        return true;
      case InputType.TYPE_CLASS_NUMBER:
        // Beware of TYPE_NUMBER_VARIATION_PASSWORD
        return false;
      default: return false;
    }
  }

  public void onVoiceListeningStarted()
  {
    _is_voice_typing_active = true;
    _toolbar_open = false;

    // 1. Update candidate bar mic to pulsing red 🔴
    if (_btn_candidate_mic != null)
    {
      _btn_candidate_mic.setText("🔴");
      try
      {
        android.view.animation.AlphaAnimation pulse = new android.view.animation.AlphaAnimation(0.3f, 1.0f);
        pulse.setDuration(500);
        pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
        pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
        _btn_candidate_mic.startAnimation(pulse);
      }
      catch (Throwable ignored) {}
    }
    if (_btn_voice != null)
    {
      _btn_voice.setText("🔴");
    }

    // 2. Open suggestions view, close toolbar, hide toggle and magic button
    if (_suggestions_scroll != null) _suggestions_scroll.setVisibility(View.VISIBLE);
    if (_suggestions_container != null) _suggestions_container.setVisibility(View.VISIBLE);
    if (_toolbar_scroll != null) _toolbar_scroll.setVisibility(View.GONE);
    if (_btn_toolbar_toggle != null) _btn_toolbar_toggle.setVisibility(View.GONE);
    if (_autofill_scroll != null) _autofill_scroll.setVisibility(View.GONE);
    if (_status_no_dict != null) _status_no_dict.setVisibility(View.GONE);
    if (_dictionary_switch_button != null) _dictionary_switch_button.setVisibility(View.GONE);
    if (_lang_name_view != null) _lang_name_view.setVisibility(View.GONE);
    if (_btn_magic_container != null) _btn_magic_container.setVisibility(View.GONE);
    else if (_btn_magic_autocomplete != null) _btn_magic_autocomplete.setVisibility(View.GONE);
    if (_btn_candidate_mic_container != null) _btn_candidate_mic_container.setVisibility(View.VISIBLE);
    else if (_btn_candidate_mic != null) _btn_candidate_mic.setVisibility(View.VISIBLE);

    // 3. Hide normal candidate chips and display live voice typing in the suggest bar
    for (int i = 0; i < MAX_WORD_CANDIDATES; i++)
    {
      if (_item_views[i] != null) _item_views[i].setVisibility(View.GONE);
    }
    if (_emoji_view != null) _emoji_view.setVisibility(View.GONE);

    if (_voice_live_banner != null)
    {
      _voice_live_banner.setText("🎙️ শুনছি... বলুন");
      _voice_live_banner.setVisibility(View.VISIBLE);
    }
    if (_suggestions_scroll != null)
    {
      _suggestions_scroll.scrollTo(0, 0);
    }
  }

  public void onVoicePartialResult(final String text)
  {
    if (_voice_live_banner != null && text != null && !text.trim().isEmpty())
    {
      _voice_live_banner.setText("🎙️ " + text.trim());
      _voice_live_banner.setVisibility(View.VISIBLE);
      if (_suggestions_scroll != null)
      {
        _suggestions_scroll.post(new Runnable()
        {
          @Override
          public void run()
          {
            if (_suggestions_scroll != null)
            {
              _suggestions_scroll.fullScroll(View.FOCUS_RIGHT);
            }
          }
        });
      }
    }
  }

  public void onVoiceListeningStopped()
  {
    _is_voice_typing_active = false;

    if (_btn_candidate_mic != null)
    {
      _btn_candidate_mic.clearAnimation();
      _btn_candidate_mic.setText("🎙️");
    }
    if (_btn_voice != null)
    {
      _btn_voice.setText("🎙️");
    }

    if (_voice_live_banner != null)
    {
      _voice_live_banner.setVisibility(View.GONE);
    }

    if (_btn_toolbar_toggle != null)
    {
      _btn_toolbar_toggle.setVisibility(View.VISIBLE);
    }
    if (_btn_magic_container != null)
    {
      _btn_magic_container.setVisibility(View.VISIBLE);
    }
    else if (_btn_magic_autocomplete != null)
    {
      _btn_magic_autocomplete.setVisibility(View.VISIBLE);
    }

    update_view_visibility(_has_suggestions);
  }
}
