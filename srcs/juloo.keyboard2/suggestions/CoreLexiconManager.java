package juloo.keyboard2.suggestions;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import juloo.keyboard2.R;

/**
 * Built-in, 100% offline core lexicon manager for English and Bengali.
 * Provides instant suggestions out-of-the-box on fresh install without internet.
 */
public class CoreLexiconManager
{
  public static class WordEntry
  {
    public final String word;
    public final int frequency; // 1 to 255

    public WordEntry(String word, int frequency)
    {
      this.word = word;
      this.frequency = frequency;
    }
  }

  private static CoreLexiconManager _instance;

  public static synchronized CoreLexiconManager instance(Context context)
  {
    if (_instance == null)
    {
      _instance = new CoreLexiconManager(context != null ? context.getApplicationContext() : null);
    }
    return _instance;
  }

  private final List<WordEntry> _englishWords = new ArrayList<>();
  private final List<WordEntry> _bengaliWords = new ArrayList<>();
  private volatile boolean _isLoaded = false;

  public CoreLexiconManager(Context context)
  {
    if (context != null)
    {
      loadLexicons(context);
    }
    else
    {
      loadEmbeddedFallback();
    }
  }

  public boolean isLoaded()
  {
    return _isLoaded;
  }

  private void loadLexicons(Context context)
  {
    try
    {
      // Load English
      InputStream enIs = context.getResources().openRawResource(R.raw.core_lexicon_en);
      loadStream(enIs, _englishWords);

      // Load Bengali
      InputStream bnIs = context.getResources().openRawResource(R.raw.core_lexicon_bn);
      loadStream(bnIs, _bengaliWords);

      sortLexicon(_englishWords);
      sortLexicon(_bengaliWords);
      _isLoaded = true;
    }
    catch (Exception e)
    {
      loadEmbeddedFallback();
    }
  }

  private void loadStream(InputStream is, List<WordEntry> dest)
  {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
    {
      String line;
      while ((line = br.readLine()) != null)
      {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
        int tab = trimmed.indexOf('\t');
        if (tab > 0)
        {
          String w = trimmed.substring(0, tab).trim();
          int freq = 1;
          try
          {
            freq = Integer.parseInt(trimmed.substring(tab + 1).trim());
          }
          catch (Exception ignored) {}
          if (!w.isEmpty()) dest.add(new WordEntry(w, freq));
        }
        else
        {
          dest.add(new WordEntry(trimmed, 100));
        }
      }
    }
    catch (Exception ignored) {}
  }

  private void sortLexicon(List<WordEntry> list)
  {
    Collections.sort(list, new Comparator<WordEntry>()
    {
      @Override
      public int compare(WordEntry a, WordEntry b)
      {
        return a.word.compareToIgnoreCase(b.word);
      }
    });
  }

  private void loadEmbeddedFallback()
  {
    // Minimal fallback for JVM unit testing without Android resources
    String[] en = {
        "the:255", "be:254", "to:253", "of:252", "and:251", "a:250", "in:249", "that:248",
        "have:247", "it:246", "for:245", "not:244", "on:243", "with:242", "he:241",
        "as:240", "you:239", "do:238", "at:237", "this:236", "but:235", "his:234",
        "by:233", "from:232", "they:231", "we:230", "say:229", "her:228", "she:227",
        "what:226", "who:225", "which:224", "when:223", "where:222", "why:221",
        "hello:220", "keyboard:210", "typing:200", "termux:190", "developer:185",
        "android:180", "smart:170", "update:160", "download:150", "message:140",
        "father:220", "assignment:210", "knowledge:215", "mother:210", "brother:200",
        "computer:195", "programming:190", "software:185", "coding:180"
    };
    for (String s : en)
    {
      String[] p = s.split(":");
      _englishWords.add(new WordEntry(p[0], Integer.parseInt(p[1])));
    }

    String[] bn = {
        "আমি:255", "তুমি:254", "আমরা:253", "আপনি:252", "তিনি:251", "সে:250", "তারা:249",
        "আমার:248", "তোমার:247", "আমাদের:246", "আপনার:245", "ভালো:238", "আছি:237",
        "আছো:236", "আছেন:235", "ভালোবাসি:233", "ধন্যবাদ:231", "অনেক:230", "খুব:229",
        "সুন্দর:228", "কেমন:227", "কি:226", "কোথায়:225", "কখন:224", "কেন:223", "সবাই:219"
    };
    for (String s : bn)
    {
      String[] p = s.split(":");
      _bengaliWords.add(new WordEntry(p[0], Integer.parseInt(p[1])));
    }

    sortLexicon(_englishWords);
    sortLexicon(_bengaliWords);
    _isLoaded = true;
  }

  /**
   * Fast prefix search returning Candidate objects with true frequencies.
   */
  public List<Candidate> queryPrefix(String prefix, int maxResults)
  {
    if (prefix == null || prefix.trim().isEmpty() || maxResults <= 0)
    {
      return Collections.emptyList();
    }

    String cleanPrefix = prefix.trim();
    boolean isBengali = BanglishEngine.isBengaliScript(cleanPrefix);
    List<WordEntry> list = isBengali ? _bengaliWords : _englishWords;

    if (list.isEmpty()) return Collections.emptyList();

    int len = list.size();
    int idx = Collections.binarySearch(list, new WordEntry(cleanPrefix, 0), new Comparator<WordEntry>()
    {
      @Override
      public int compare(WordEntry a, WordEntry b)
      {
        return a.word.compareToIgnoreCase(b.word);
      }
    });

    if (idx < 0)
    {
      idx = -(idx + 1);
    }

    int pLen = cleanPrefix.length();
    List<Candidate> matches = new ArrayList<>(Math.min(maxResults * 2, 32));

    for (int i = idx; i < len && matches.size() < maxResults * 3; i++)
    {
      WordEntry entry = list.get(i);
      if (entry.word.regionMatches(true, 0, cleanPrefix, 0, pLen))
      {
        float ratio = (float) pLen / (float) entry.word.length();
        matches.add(new Candidate(entry.word, Candidate.Source.BUILTIN_MAIN, entry.frequency, 0, ratio));
      }
      else
      {
        break;
      }
    }

    // Sort matching prefix results by frequency descending
    Collections.sort(matches, new Comparator<Candidate>()
    {
      @Override
      public int compare(Candidate a, Candidate b)
      {
        return Integer.compare(b.frequency, a.frequency);
      }
    });

    if (matches.size() > maxResults)
    {
      return matches.subList(0, maxResults);
    }
    return matches;
  }

  /**
   * Queries distance-1 edit typos from core lexicon for words length >= 3.
   */
  public List<Candidate> queryTypoCorrections(String word, int maxResults)
  {
    if (word == null || word.length() < 3 || maxResults <= 0)
    {
      return Collections.emptyList();
    }

    String clean = word.trim().toLowerCase(Locale.ROOT);
    boolean isBengali = BanglishEngine.isBengaliScript(clean);
    List<WordEntry> list = isBengali ? _bengaliWords : _englishWords;

    List<Candidate> typos = new ArrayList<>();
    for (WordEntry entry : list)
    {
      String dictWord = entry.word.toLowerCase(Locale.ROOT);
      // Fast length filter: Levenshtein distance 1 requires length difference <= 1
      if (Math.abs(dictWord.length() - clean.length()) > 1)
      {
        continue;
      }

      int dist = computeLevenshteinDistance1(clean, dictWord);
      if (dist == 1)
      {
        float ratio = (float) Math.min(clean.length(), dictWord.length()) / (float) Math.max(clean.length(), dictWord.length());
        typos.add(new Candidate(entry.word, Candidate.Source.TYPO_CORRECTION, entry.frequency, 1, ratio));
        if (typos.size() >= maxResults * 2) break;
      }
    }

    Collections.sort(typos, new Comparator<Candidate>()
    {
      @Override
      public int compare(Candidate a, Candidate b)
      {
        return Integer.compare(b.frequency, a.frequency);
      }
    });

    if (typos.size() > maxResults)
    {
      return typos.subList(0, maxResults);
    }
    return typos;
  }

  /**
   * Fast check whether word exists in core lexicon.
   */
  public boolean containsWord(String word)
  {
    if (word == null || word.trim().isEmpty()) return false;
    String clean = word.trim();
    boolean isBengali = BanglishEngine.isBengaliScript(clean);
    List<WordEntry> list = isBengali ? _bengaliWords : _englishWords;

    int idx = Collections.binarySearch(list, new WordEntry(clean, 0), new Comparator<WordEntry>()
    {
      @Override
      public int compare(WordEntry a, WordEntry b)
      {
        return a.word.compareToIgnoreCase(b.word);
      }
    });

    if (idx >= 0) return true;
    return false;
  }

  /**
   * Ultra-fast check whether Damerau-Levenshtein distance is exactly 1 (or 0, or > 1).
   * Supports insertions, deletions, substitutions, and adjacent transpositions.
   */
  public static int computeLevenshteinDistance1(String s1, String s2)
  {
    int len1 = s1.length();
    int len2 = s2.length();
    if (Math.abs(len1 - len2) > 1) return 2;

    // Equal lengths: substitution or adjacent transposition
    if (len1 == len2)
    {
      int mismatch1 = -1;
      int mismatch2 = -1;
      int mismatches = 0;
      for (int k = 0; k < len1; k++)
      {
        if (s1.charAt(k) != s2.charAt(k))
        {
          mismatches++;
          if (mismatch1 == -1) mismatch1 = k;
          else if (mismatch2 == -1) mismatch2 = k;
          else return 2;
        }
      }
      if (mismatches == 0) return 0;
      if (mismatches == 1) return 1; // Single substitution
      if (mismatches == 2 && mismatch2 == mismatch1 + 1)
      {
        // Adjacent transposition: "thsi" -> "this", "teh" -> "the"
        if (s1.charAt(mismatch1) == s2.charAt(mismatch2) && s1.charAt(mismatch2) == s2.charAt(mismatch1))
        {
          return 1;
        }
      }
      return 2;
    }

    // Single insertion or deletion
    int i = 0;
    int j = 0;
    int diff = 0;

    while (i < len1 && j < len2)
    {
      if (s1.charAt(i) != s2.charAt(j))
      {
        diff++;
        if (diff > 1) return 2;

        if (len1 > len2)
        {
          i++; // Deletion
        }
        else
        {
          j++; // Insertion
        }
      }
      else
      {
        i++;
        j++;
      }
    }

    if (i < len1 || j < len2)
    {
      diff++;
    }

    return diff == 1 ? 1 : (diff == 0 ? 0 : 2);
  }
}
