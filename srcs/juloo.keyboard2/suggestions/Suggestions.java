package juloo.keyboard2.suggestions;

import android.content.Context;
import java.util.Arrays;
import java.util.List;
import juloo.cdict.Cdict;
import juloo.keyboard2.dict.Dictionaries;
import juloo.keyboard2.Config;
import juloo.keyboard2.ComposeKey;
import juloo.keyboard2.ComposeKeyData;

/** Keep track of the word being typed and provide suggestions for
    [CandidatesView]. */
public final class Suggestions
{
  Callback _callback;
  Config _config;
  boolean _enabled;

  /** Current suggestions. The best suggestion is at index [0]. */
  public String[] suggestions = new String[MAX_COUNT];
  /** Number of suggestions at the beginning of the [suggestions] array that
      are not [null]. */
  public int count = 0;
  public String emoji_suggestion = null;
  /** Number of suggestions in [suggestions]. */
  public static final int MAX_COUNT = 15;

  Context _context;

  public Suggestions(Callback c, Config conf)
  {
    this(c, conf, null);
  }

  public Suggestions(Callback c, Config conf, Context context)
  {
    _callback = c;
    _config = conf;
    _context = context;
  }

  public void started()
  {
    _enabled = _config.editor_config.should_show_candidates_view;
    _second_last_word = "";
    _last_word = "";
    clear();
  }

  private String _second_last_word = "";
  private String _last_word = "";

  public void set_last_word(String w)
  {
    if (w != null && !w.trim().isEmpty())
    {
      String clean = w.trim();
      if (!_last_word.equalsIgnoreCase(clean))
      {
        _second_last_word = _last_word;
        _last_word = clean;
      }
    }
  }

  public String get_last_word()
  {
    return _last_word;
  }

  public String get_second_last_word()
  {
    return _second_last_word;
  }

  public void predict_next_words(String prevWord)
  {
    if (!_enabled)
      return;
    clear();
    String context = "";
    String priorWord = _last_word;
    String priorPriorWord = _second_last_word;

    if (priorWord != null && !priorWord.isEmpty() && !priorWord.equalsIgnoreCase(prevWord))
    {
      context = (priorWord + " " + prevWord).trim();
    }
    else if (priorPriorWord != null && !priorPriorWord.isEmpty() && !priorPriorWord.equalsIgnoreCase(prevWord))
    {
      context = (priorPriorWord + " " + prevWord).trim();
    }

    set_last_word(prevWord);
    if (_context != null)
    {
      // Learn transitions from prior words to this newly completed word
      if (priorWord != null && !priorWord.isEmpty() && !priorWord.equalsIgnoreCase(prevWord))
      {
        NextWordPredictor.instance(_context).learn(priorWord, prevWord);
        if (priorPriorWord != null && !priorPriorWord.isEmpty())
        {
          NextWordPredictor.instance(_context).learn(priorPriorWord + " " + priorWord, prevWord);
        }
      }

      List<String> nextWords = NextWordPredictor.instance(_context).predict(context, prevWord, MAX_COUNT);
      int i = 0;
      for (String nw : nextWords)
      {
        if (i >= MAX_COUNT) break;
        suggestions[i++] = nw;
      }
      count = i;
    }
    _callback.set_suggestions(this);
  }

  public void currently_typed_word(String word)
  {
    if (!_enabled)
      return;
    if (word == null || word.isEmpty())
    {
      if (_context != null && _last_word != null && !_last_word.isEmpty())
      {
        predict_next_words(_last_word);
      }
      else
      {
        clear();
        _callback.set_suggestions(this);
      }
      return;
    }
    set_last_word(word);
    boolean hasExt = (_context != null && juloo.keyboard2.dict.ExternalDictionaryManager.instance(_context).getWordCount() > 0);
    if (_config.current_dictionary == null && !hasExt)
    {
      clear();
    }
    else
    {
      query_suggestions(word);
    }
    _callback.set_suggestions(this);
  }

  void clear()
  {
    count = 0;
    for (int i = 0; i < MAX_COUNT; i++)
      suggestions[i] = null;
    emoji_suggestion = null;
  }

  int query_suggestions(String word)
  {
    try
    {
      String rawWord = word;
      Cdict dict = _config.current_dictionary;
      boolean first_char_upper = (word != null && !word.isEmpty() && Character.isUpperCase(word.charAt(0)));
      String subWord = apply_substitutions(word);
      int i = 0;

      if (dict != null)
      {
        Cdict.Result r = dict.find(subWord);
        if (r.found)
          suggestions[i++] = dict.word(r.index);
        int[] suffixes = dict.suffixes(r, MAX_COUNT);
        // Disable distance search for small words
        int[] dist = (subWord.length() < 3 || i + 1 >= MAX_COUNT) ? NO_RESULTS :
          dict.distance(subWord, 1, MAX_COUNT);
        for (int j = 0; j < MAX_COUNT && i < MAX_COUNT; j++)
        {
          if (suffixes.length > j)
            suggestions[i++] = dict.word(suffixes[j]);
          if (dist.length > j && i < MAX_COUNT)
            suggestions[i++] = dict.word(dist[j]);
        }
      }

      // Merge external imported dictionary / wordlist words (FrostKeys / HeliBoard style)
      if (_context != null && i < MAX_COUNT)
      {
        juloo.keyboard2.dict.ExternalDictionaryManager ext =
            juloo.keyboard2.dict.ExternalDictionaryManager.instance(_context);
        // Query raw word first (preserves exact Unicode Bengali and Latin case)
        List<String> extWords = ext.query(rawWord, MAX_COUNT - i);
        for (String ew : extWords)
        {
          if (i >= MAX_COUNT) break;
          if (!containsSuggestion(ew, i))
          {
            suggestions[i++] = ew;
          }
        }

        // If still space, query substituted word
        if (i < MAX_COUNT && !rawWord.equals(subWord))
        {
          List<String> subExtWords = ext.query(subWord, MAX_COUNT - i);
          for (String ew : subExtWords)
          {
            if (i >= MAX_COUNT) break;
            if (!containsSuggestion(ew, i))
            {
              suggestions[i++] = ew;
            }
          }
        }
      }

      count = i;
      if (first_char_upper)
        capitalize_results();
      emoji_suggestion = query_emoji(subWord);
      return i;
    }
    catch (Exception e)
    {
      juloo.keyboard2.Logs.exn("Suggestions", e);
      return 0;
    }
  }

  private boolean containsSuggestion(String word, int currentCount)
  {
    for (int k = 0; k < currentCount; k++)
    {
      if (suggestions[k] != null && suggestions[k].equalsIgnoreCase(word))
      {
        return true;
      }
    }
    return false;
  }

  void capitalize_results()
  {
    for (int i = 0; i < count; i++)
    {
      if (suggestions[i] != null && !suggestions[i].isEmpty())
      {
        suggestions[i] = juloo.keyboard2.Utils.capitalize_string(suggestions[i]);
      }
    }
  }

  String query_emoji(String word)
  {
    Cdict dict = _config.emoji_dictionary;
    // Disable emoji suggestion for short words
    if (dict == null || word.length() < 3)
      return null;
    Cdict.Result r = dict.find(word);
    if (r.found)
      return dict.word(r.index);
    int[] s = dict.suffixes(r, 1);
    if (s.length > 0)
      return dict.word(s[0]);
    return null;
  }

  /** Apply the same substitutions that were used when building the
      dictionaries to find word aliases. This catches missing diacritics for
      example. */
  String apply_substitutions(String w)
  {
    StringBuilder b = new StringBuilder(w);
    int len = w.length();
    for (int i = 0; i < len; i++)
    {
      char r =
        ComposeKey.transform_char(ComposeKeyData.substitutions, b.charAt(i));
      if (r != 0) b.setCharAt(i, r);
    }
    return b.toString();
  }

  static final int[] NO_RESULTS = new int[0];

  public static interface Callback
  {
    public void set_suggestions(Suggestions suggestions);
  }
}
