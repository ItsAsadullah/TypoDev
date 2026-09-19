package juloo.keyboard2.suggestions;

import android.content.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

  public Context getContext()
  {
    return _context;
  }

  public void started()
  {
    _enabled = _config.editor_config.should_show_candidates_view;
    _second_last_word = "";
    _last_word = "";
    clear();
    _callback.set_suggestions(this);
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

  public boolean isBengaliMode()
  {
    if (_config != null)
    {
      if (_config.is_bengali_mode)
        return true;
      if ("BN".equalsIgnoreCase(_config.current_dictionary_short_name))
        return true;
      if (_config.current_dictionary_name != null &&
          _config.current_dictionary_name.toLowerCase(java.util.Locale.ROOT).contains("bengali"))
        return true;
    }
    return false;
  }

  public static boolean containsBengaliChar(String s)
  {
    if (s == null) return false;
    for (int i = 0; i < s.length(); i++)
    {
      char c = s.charAt(i);
      if (c >= '\u0980' && c <= '\u09FF')
      {
        return true;
      }
    }
    return false;
  }

  public void predict_sentence_completion(String textBefore)
  {
    if (!_enabled)
      return;

    if (textBefore == null || textBefore.isEmpty())
    {
      _last_word = "";
      _second_last_word = "";
      clear();
      _callback.set_suggestions(this);
      return;
    }

    if (textBefore.endsWith("\n") || textBefore.endsWith("\r"))
    {
      _last_word = "";
      _second_last_word = "";
      clear();
      _callback.set_suggestions(this);
      return;
    }

    clear();
    boolean bengaliMode = isBengaliMode() || containsBengaliChar(textBefore);

    // If text ends with angle bracket e.g. "<" or "</", show tag suggestions directly IF in developer mode
    if (_config != null && _config.developer_mode && (textBefore.endsWith("</") || textBefore.endsWith("<")))
    {
      List<Candidate> devCands = DevSyntaxEngine.instance().queryCandidates("", textBefore, MAX_COUNT);
      int i = 0;
      for (Candidate c : devCands)
      {
        if (i >= MAX_COUNT) break;
        suggestions[i++] = c.word;
      }
      count = i;
      _callback.set_suggestions(this);
      return;
    }

    NextWordPredictor predictor = NextWordPredictor.instance(_context);
    predictor.learnSentence(textBefore);

    List<String> nextWords = predictor.predictFromSentence(textBefore, MAX_COUNT);
    int i = 0;
    for (String nw : nextWords)
    {
      if (i >= MAX_COUNT) break;
      if (!bengaliMode && containsBengaliChar(nw))
        continue;
      suggestions[i++] = nw;
    }
    count = i;
    _callback.set_suggestions(this);
  }

  public void predict_next_words(String prevWord)
  {
    if (!_enabled)
      return;

    CharSequence textBefore = null;
    if (_callback != null)
    {
      try
      {
        textBefore = _callback.getTextBeforeCursor(120, 0);
      }
      catch (Throwable ignored) {}
    }

    if (textBefore != null && textBefore.length() > 0)
    {
      predict_sentence_completion(textBefore.toString());
      return;
    }

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
    NextWordPredictor predictor = NextWordPredictor.instance(_context);
    // Learn transitions from prior words to this newly completed word
    if (priorWord != null && !priorWord.isEmpty() && !priorWord.equalsIgnoreCase(prevWord))
    {
      predictor.learn(priorWord, prevWord);
      if (priorPriorWord != null && !priorPriorWord.isEmpty())
      {
        predictor.learn(priorPriorWord + " " + priorWord, prevWord);
      }
    }

    List<String> nextWords = predictor.predict(context, prevWord, MAX_COUNT);
    boolean bengaliMode = isBengaliMode() || containsBengaliChar(prevWord);
    int i = 0;
    for (String nw : nextWords)
    {
      if (i >= MAX_COUNT) break;
      if (!bengaliMode && containsBengaliChar(nw))
        continue;
      suggestions[i++] = nw;
    }
    count = i;
    _callback.set_suggestions(this);
  }

  public void currently_typed_word(String word)
  {
    if (!_enabled)
      return;

    if (word == null || word.isEmpty())
    {
      if (_callback != null && _callback.isSelectionActive())
      {
        _last_word = "";
        _second_last_word = "";
        clear();
        _callback.set_suggestions(this);
        return;
      }

      CharSequence textBefore = null;
      if (_callback != null)
      {
        try
        {
          textBefore = _callback.getTextBeforeCursor(120, 0);
        }
        catch (Throwable ignored) {}
      }

      if (textBefore != null && textBefore.length() > 0)
      {
        predict_sentence_completion(textBefore.toString());
      }
      else if (_last_word != null && !_last_word.isEmpty())
      {
        predict_sentence_completion(_last_word + " ");
      }
      else
      {
        predict_sentence_completion("");
      }
      return;
    }
    query_suggestions(word);
    _callback.set_suggestions(this);
  }

  public boolean should_autocorrect = false;
  public String verbatim_word = null;
  public Candidate top_candidate = null;

  void clear()
  {
    count = 0;
    for (int i = 0; i < MAX_COUNT; i++)
      suggestions[i] = null;
    emoji_suggestion = null;
    should_autocorrect = false;
    verbatim_word = null;
    top_candidate = null;
  }

  int query_suggestions(String word)
  {
    try
    {
      String rawWord = word;
      verbatim_word = rawWord;
      Cdict dict = _config.current_dictionary;
      boolean first_char_upper = (word != null && !word.isEmpty() && Character.isUpperCase(word.charAt(0)));
      String subWord = apply_substitutions(word);
      boolean bengaliMode = isBengaliMode() || containsBengaliChar(rawWord);

      List<Candidate> rawCandidates = new ArrayList<>(48);

      // 0. Developer Coding Syntax & Tag Suggestions (ONLY in developer_mode)
      if (_config != null && _config.developer_mode)
      {
        CharSequence textBefore = null;
        if (_callback != null)
        {
          try
          {
            textBefore = _callback.getTextBeforeCursor(120, 0);
          }
          catch (Throwable ignored) {}
        }
        String textBeforeStr = (textBefore != null) ? textBefore.toString() : "";
        List<Candidate> devCands = DevSyntaxEngine.instance().queryCandidates(rawWord, textBeforeStr, 8);
        rawCandidates.addAll(devCands);
      }

      // 1. English Grammar, Contractions, Spellings, Inflections & Ordinals
      if (!bengaliMode || !containsBengaliChar(rawWord))
      {
        List<Candidate> grammarCands = EnglishGrammarEngine.instance().queryCandidates(rawWord, 8);
        rawCandidates.addAll(grammarCands);
      }

      // 2. Snippet / Word Manager (high priority shortcuts)
      if (_context != null && word != null && word.length() >= 2)
      {
        try
        {
          java.util.List<juloo.keyboard2.snippet.Snippet> snippets =
              juloo.keyboard2.snippet.SnippetStore.instance(_context)
                  .findByShortcutPrefix(word);
          for (juloo.keyboard2.snippet.Snippet sn : snippets)
          {
            boolean isExact = word.equalsIgnoreCase(sn.shortcut);
            int minLen = Math.max(2, (int)Math.ceil(sn.shortcut.length() * 0.75));
            if (!isExact && word.length() < minLen) continue;
            String expansion = sn.expansion;
            if (expansion != null && !expansion.isEmpty())
            {
              rawCandidates.add(new Candidate(expansion, Candidate.Source.SNIPPET, 240, 0, 1.0f));
            }
          }
        }
        catch (Throwable ignored) {}
      }

      // 3. Personal & Learned Vocabulary (highest user affinity)
      if (_context != null)
      {
        try
        {
          List<Candidate> userCands = UserVocabularyStore.instance(_context).queryCandidates(rawWord, 6);
          for (Candidate c : userCands)
          {
            if (!bengaliMode && containsBengaliChar(c.word)) continue;
            rawCandidates.add(c);
          }
        }
        catch (Throwable ignored) {}
      }

      // 4. Offline Core Lexicon (built-in English & Bengali high-frequency words)
      try
      {
        List<Candidate> coreCands = CoreLexiconManager.instance(_context).queryPrefix(rawWord, 12);
        for (Candidate c : coreCands)
        {
          if (!bengaliMode && containsBengaliChar(c.word)) continue;
          rawCandidates.add(c);
        }
      }
      catch (Throwable ignored) {}

      // 5. Native Cdict dictionary (if loaded)
      if (dict != null)
      {
        try
        {
          Cdict.Result r = dict.find(subWord);
          if (r.found)
          {
            int f = Math.min(255, dict.freq(r.index) * 17);
            String dw = dict.word(r.index);
            if (bengaliMode || !containsBengaliChar(dw))
            {
              rawCandidates.add(new Candidate(dw, Candidate.Source.CDICT, f, 0, 1.0f));
            }
          }
          int[] suffixes = dict.suffixes(r, 10);
          for (int sIdx : suffixes)
          {
            String sw = dict.word(sIdx);
            if (!bengaliMode && containsBengaliChar(sw)) continue;
            int f = Math.min(255, dict.freq(sIdx) * 17);
            float ratio = (float) subWord.length() / (float) sw.length();
            rawCandidates.add(new Candidate(sw, Candidate.Source.CDICT, f, 0, ratio));
          }
          if (subWord.length() >= 3)
          {
            int[] dist = dict.distance(subWord, 1, 6);
            for (int dIdx : dist)
            {
              String dw = dict.word(dIdx);
              if (!bengaliMode && containsBengaliChar(dw)) continue;
              int f = Math.min(255, dict.freq(dIdx) * 17);
              rawCandidates.add(new Candidate(dw, Candidate.Source.TYPO_CORRECTION, f, 1, 0.8f));
            }
          }
        }
        catch (Throwable ignored) {}
      }

      // 6. External downloaded/imported dictionaries (frequency-aware)
      if (_context != null)
      {
        try
        {
          juloo.keyboard2.dict.ExternalDictionaryManager ext =
              juloo.keyboard2.dict.ExternalDictionaryManager.instance(_context);
          List<Candidate> extCands = ext.queryCandidates(rawWord, 10);
          for (Candidate c : extCands)
          {
            if (!bengaliMode && containsBengaliChar(c.word)) continue;
            rawCandidates.add(c);
          }

          if (!rawWord.equals(subWord))
          {
            List<Candidate> subExtCands = ext.queryCandidates(subWord, 6);
            for (Candidate c : subExtCands)
            {
              if (!bengaliMode && containsBengaliChar(c.word)) continue;
              rawCandidates.add(c);
            }
          }
        }
        catch (Throwable ignored) {}
      }

      // 7. Banglish-to-Bangla Transliteration Candidate Generation (ONLY IN BENGALI MODE)
      if (bengaliMode && BanglishEngine.isLatinOnly(rawWord) && rawWord.length() >= 2)
      {
        try
        {
          List<Candidate> bnCands = BanglishEngine.instance().generateCandidates(rawWord, 5);
          rawCandidates.addAll(bnCands);
        }
        catch (Throwable ignored) {}
      }

      // 8. Levenshtein Typo Corrections from Core Lexicon
      if (rawWord.length() >= 3)
      {
        try
        {
          List<Candidate> coreTypos = CoreLexiconManager.instance(_context).queryTypoCorrections(rawWord, 4);
          for (Candidate c : coreTypos)
          {
            if (!bengaliMode && containsBengaliChar(c.word)) continue;
            rawCandidates.add(c);
          }
        }
        catch (Throwable ignored) {}
      }

      // 9. Context Collocation & Predictive Boost from preceding words in sentence
      CharSequence textBefore = null;
      if (_callback != null)
      {
        try
        {
          textBefore = _callback.getTextBeforeCursor(120, 0);
        }
        catch (Throwable ignored) {}
      }
      String priorText = (textBefore != null) ? textBefore.toString() : "";
      if (!priorText.isEmpty() && rawWord != null && !rawWord.isEmpty())
      {
        if (priorText.endsWith(rawWord))
        {
          priorText = priorText.substring(0, priorText.length() - rawWord.length());
        }
      }
      if (priorText.isEmpty() && _last_word != null && !_last_word.equalsIgnoreCase(rawWord))
      {
        priorText = _last_word + " ";
      }

      if (!priorText.trim().isEmpty())
      {
        try
        {
          List<String> expectedNextWords = NextWordPredictor.instance(_context).predictFromSentence(priorText, 16);
          if (expectedNextWords != null && !expectedNextWords.isEmpty())
          {
            String lowerRaw = rawWord.toLowerCase(Locale.ROOT);
            for (int predIdx = 0; predIdx < expectedNextWords.size(); predIdx++)
            {
              String pred = expectedNextWords.get(predIdx);
              if (pred == null || pred.isEmpty()) continue;
              if (!bengaliMode && containsBengaliChar(pred)) continue;

              String lowerPred = pred.toLowerCase(Locale.ROOT);
              if (lowerPred.startsWith(lowerRaw))
              {
                float ctxScore = Math.max(0.5f, 1.0f - (predIdx * 0.05f));
                int freqBoost = Math.max(200, 255 - (predIdx * 2));
                boolean found = false;
                for (Candidate c : rawCandidates)
                {
                  if (c.word.equalsIgnoreCase(pred))
                  {
                    c.source = Candidate.Source.AUTOCORRECT;
                    c.contextScore = Math.max(c.contextScore, ctxScore);
                    c.frequency = Math.max(c.frequency, freqBoost);
                    found = true;
                    break;
                  }
                }
                if (!found)
                {
                  float ratio = (float)lowerRaw.length() / (float)pred.length();
                  rawCandidates.add(new Candidate(pred, Candidate.Source.AUTOCORRECT, freqBoost, 0, ratio, ctxScore));
                }
              }
            }
          }
        }
        catch (Throwable ignored) {}
      }

      // 10. Strict English filter: if not in Bengali mode, filter any candidate with Bengali characters
      List<Candidate> candidatesToRank = rawCandidates;
      if (!bengaliMode)
      {
        candidatesToRank = new ArrayList<>(rawCandidates.size());
        for (Candidate c : rawCandidates)
        {
          if (c != null && c.word != null && !containsBengaliChar(c.word))
          {
            candidatesToRank.add(c);
          }
        }
      }

      // 11. Composite Scoring & Ranking
      List<Candidate> ranked = CandidateRanker.rank(candidatesToRank, MAX_COUNT);

      // 12. Autocorrect Evaluation
      should_autocorrect = false;
      top_candidate = null;
      if (!ranked.isEmpty())
      {
        top_candidate = ranked.get(0);
        Candidate second = ranked.size() > 1 ? ranked.get(1) : null;

        boolean isWordValid = CoreLexiconManager.instance(_context).containsWord(rawWord);
        if (!isWordValid && _context != null)
        {
          isWordValid = UserVocabularyStore.instance(_context).containsWord(rawWord)
              || juloo.keyboard2.dict.ExternalDictionaryManager.instance(_context).containsWord(rawWord);
        }
        if (!isWordValid && dict != null)
        {
          isWordValid = dict.find(subWord).found;
        }

        boolean isProtected = (_context != null && UserVocabularyStore.instance(_context).isProtectedWord(rawWord));

        should_autocorrect = AutocorrectDecision.shouldAutocorrect(
            rawWord, top_candidate, second, isWordValid, isProtected, AutocorrectDecision.Sensitivity.BALANCED);
      }

      // 13. Populate Suggestions Array
      int i = 0;
      for (Candidate c : ranked)
      {
        if (i >= MAX_COUNT) break;
        suggestions[i++] = c.word;
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
        String s = suggestions[i];
        // Do not capitalize tags, code snippets, or formulas
        if (s.startsWith("<") || s.contains("(") || s.contains(";") || s.contains(":") || s.contains("="))
        {
          continue;
        }
        // Do not alter words that already have internal capitalization or apostrophes (e.g. I'm, Father's, 3ʳᵈ)
        if (s.startsWith("I'") || s.startsWith("i'"))
        {
          suggestions[i] = "I" + s.substring(1);
          continue;
        }
        suggestions[i] = juloo.keyboard2.Utils.capitalize_string(s);
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
    default CharSequence getTextBeforeCursor(int n, int flags) { return null; }
    default boolean isSelectionActive() { return false; }
  }
}
