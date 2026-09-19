package juloo.keyboard2.suggestions;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-performance, 100% on-device local store for learned user vocabulary,
 * usage frequencies, recency timestamps, and rejection penalties.
 */
public class UserVocabularyStore
{
  private static final String PREF_SETTINGS = "user_vocabulary_settings";
  private static final String KEY_LEARNING_ENABLED = "pref_learning_enabled";
  private static final String STORE_FILE_NAME = "user_vocabulary_v1.txt";

  public static class WordStats
  {
    public final String word;
    public int frequency;
    public long lastUsedTime;
    public int rejections;
    public boolean isUserAdded;

    public WordStats(String word, int frequency, long lastUsedTime, int rejections, boolean isUserAdded)
    {
      this.word = word;
      this.frequency = frequency;
      this.lastUsedTime = lastUsedTime;
      this.rejections = rejections;
      this.isUserAdded = isUserAdded;
    }
  }

  private static UserVocabularyStore _instance;

  public static synchronized UserVocabularyStore instance(Context context)
  {
    if (_instance == null)
    {
      _instance = new UserVocabularyStore(context != null ? context.getApplicationContext() : null);
    }
    return _instance;
  }

  private final Context _context;
  private final ConcurrentHashMap<String, WordStats> _words = new ConcurrentHashMap<>();
  private final ExecutorService _ioExecutor = Executors.newSingleThreadExecutor();
  private volatile boolean _learningEnabled = true;

  public UserVocabularyStore(Context context)
  {
    _context = context;
    if (_context != null)
    {
      SharedPreferences prefs = _context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE);
      _learningEnabled = prefs.getBoolean(KEY_LEARNING_ENABLED, true);
      loadFromDisk();
    }
  }

  public boolean isLearningEnabled()
  {
    return _learningEnabled;
  }

  public void setLearningEnabled(boolean enabled)
  {
    _learningEnabled = enabled;
    if (_context != null)
    {
      _context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
          .edit()
          .putBoolean(KEY_LEARNING_ENABLED, enabled)
          .apply();
    }
  }

  /**
   * Learns a word typed or accepted by the user.
   */
  public void learnWord(String rawWord)
  {
    if (!_learningEnabled || rawWord == null) return;
    String word = cleanWord(rawWord);
    if (word.length() <= 1 || isSkipLearningWord(word)) return;

    String key = word.toLowerCase(Locale.ROOT);
    long now = System.currentTimeMillis();

    WordStats stats = _words.get(key);
    if (stats == null)
    {
      stats = new WordStats(word, 1, now, 0, false);
      _words.put(key, stats);
    }
    else
    {
      stats.frequency++;
      stats.lastUsedTime = now;
      if (stats.rejections > 0)
      {
        stats.rejections--; // Re-affirming the word offsets past rejection
      }
    }

    scheduleSave();
  }

  /**
   * Adds an explicit personal dictionary entry.
   */
  public void addPersonalWord(String rawWord)
  {
    if (rawWord == null) return;
    String word = cleanWord(rawWord);
    if (word.isEmpty()) return;

    String key = word.toLowerCase(Locale.ROOT);
    long now = System.currentTimeMillis();

    WordStats stats = _words.get(key);
    if (stats == null)
    {
      stats = new WordStats(word, 10, now, 0, true);
      _words.put(key, stats);
    }
    else
    {
      stats.frequency = Math.max(stats.frequency, 10);
      stats.isUserAdded = true;
      stats.lastUsedTime = now;
      stats.rejections = 0;
    }
    scheduleSave();
  }

  /**
   * Records that the user rejected [rejectedWord] (e.g., via backspacing immediately after autocorrect).
   */
  public void recordRejection(String rejectedWord, String originalTyped)
  {
    if (rejectedWord == null) return;
    String key = cleanWord(rejectedWord).toLowerCase(Locale.ROOT);
    if (key.isEmpty()) return;

    WordStats stats = _words.get(key);
    if (stats != null)
    {
      stats.rejections += 2; // Strong penalty for rejection
      scheduleSave();
    }
  }

  /**
   * Checks whether a word exists in personal vocabulary.
   */
  public boolean containsWord(String word)
  {
    if (word == null) return false;
    String key = cleanWord(word).toLowerCase(Locale.ROOT);
    return _words.containsKey(key);
  }

  /**
   * Checks whether a word is protected from autocorrection.
   */
  public boolean isProtectedWord(String word)
  {
    if (word == null) return false;
    String key = cleanWord(word).toLowerCase(Locale.ROOT);
    WordStats stats = _words.get(key);
    return stats != null && (stats.isUserAdded || stats.frequency >= 2);
  }

  /**
   * Finds matching candidates from user vocabulary for a prefix.
   */
  public List<Candidate> queryCandidates(String prefix, int maxResults)
  {
    if (prefix == null || prefix.trim().isEmpty() || maxResults <= 0)
    {
      return Collections.emptyList();
    }

    String cleanPrefix = prefix.trim().toLowerCase(Locale.ROOT);
    int pLen = cleanPrefix.length();
    long now = System.currentTimeMillis();

    List<Candidate> results = new ArrayList<>();
    for (WordStats ws : _words.values())
    {
      String wLower = ws.word.toLowerCase(Locale.ROOT);
      if (wLower.startsWith(cleanPrefix))
      {
        int scaledFreq = Math.min(255, ws.frequency * 25 + (ws.isUserAdded ? 60 : 0));
        float matchRatio = (float) pLen / (float) ws.word.length();
        Candidate c = new Candidate(ws.word, Candidate.Source.PERSONAL, scaledFreq, 0, matchRatio);

        // Recency score (decay: 1.0 within 1h, 0.7 within 1d, 0.4 within 1w, 0.1 older)
        long ageMs = now - ws.lastUsedTime;
        if (ageMs < 3600000L) c.recencyScore = 1.0f;
        else if (ageMs < 86400000L) c.recencyScore = 0.7f;
        else if (ageMs < 604800000L) c.recencyScore = 0.4f;
        else c.recencyScore = 0.1f;

        // Rejection penalty
        if (ws.rejections > 0)
        {
          c.rejectionPenalty = Math.min(1.0f, (float) ws.rejections / (1.0f + ws.frequency));
        }

        results.add(c);
      }
    }

    return results;
  }

  public float getRejectionPenalty(String word)
  {
    if (word == null) return 0f;
    WordStats ws = _words.get(cleanWord(word).toLowerCase(Locale.ROOT));
    if (ws == null || ws.rejections <= 0) return 0f;
    return Math.min(1.0f, (float) ws.rejections / (1.0f + ws.frequency));
  }

  public float getRecencyBoost(String word)
  {
    if (word == null) return 0f;
    WordStats ws = _words.get(cleanWord(word).toLowerCase(Locale.ROOT));
    if (ws == null) return 0f;
    long ageMs = System.currentTimeMillis() - ws.lastUsedTime;
    if (ageMs < 3600000L) return 1.0f;
    if (ageMs < 86400000L) return 0.7f;
    if (ageMs < 604800000L) return 0.4f;
    return 0.1f;
  }

  /**
   * Clears all learned words and statistics.
   */
  public void clearLearnedData()
  {
    _words.clear();
    if (_context != null)
    {
      File f = new File(_context.getFilesDir(), STORE_FILE_NAME);
      if (f.exists()) f.delete();
    }
  }

  public int getWordCount()
  {
    return _words.size();
  }

  private void scheduleSave()
  {
    if (_context == null) return;
    _ioExecutor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        saveToDisk();
      }
    });
  }

  private synchronized void saveToDisk()
  {
    if (_context == null) return;
    File file = new File(_context.getFilesDir(), STORE_FILE_NAME);
    try (FileOutputStream fos = new FileOutputStream(file);
         OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8))
    {
      for (WordStats ws : _words.values())
      {
        // Format: word\tfreq\tlastUsed\trejections\tisUserAdded\n
        osw.write(ws.word);
        osw.write('\t');
        osw.write(String.valueOf(ws.frequency));
        osw.write('\t');
        osw.write(String.valueOf(ws.lastUsedTime));
        osw.write('\t');
        osw.write(String.valueOf(ws.rejections));
        osw.write('\t');
        osw.write(ws.isUserAdded ? "1" : "0");
        osw.write('\n');
      }
      osw.flush();
    }
    catch (Exception ignored) {}
  }

  private synchronized void loadFromDisk()
  {
    if (_context == null) return;
    File file = new File(_context.getFilesDir(), STORE_FILE_NAME);
    if (!file.exists()) return;

    try (FileInputStream fis = new FileInputStream(file);
         BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8)))
    {
      String line;
      while ((line = br.readLine()) != null)
      {
        String[] parts = line.split("\t");
        if (parts.length >= 5)
        {
          String w = parts[0];
          int freq = Integer.parseInt(parts[1]);
          long lastUsed = Long.parseLong(parts[2]);
          int rej = Integer.parseInt(parts[3]);
          boolean userAdded = "1".equals(parts[4]);
          _words.put(w.toLowerCase(Locale.ROOT), new WordStats(w, freq, lastUsed, rej, userAdded));
        }
      }
    }
    catch (Exception ignored) {}
  }

  private static String cleanWord(String raw)
  {
    if (raw == null) return "";
    String w = raw.trim();
    while (!w.isEmpty())
    {
      char last = w.charAt(w.length() - 1);
      if (last == '.' || last == ',' || last == '?' || last == '!' || last == ';' || last == ':' || last == '।' || last == '"' || last == '\'')
      {
        w = w.substring(0, w.length() - 1).trim();
      }
      else
      {
        break;
      }
    }
    return w;
  }

  private static boolean isSkipLearningWord(String w)
  {
    // Skip URLs, emails, purely numeric tokens
    if (w.startsWith("http://") || w.startsWith("https://") || w.contains("@") || w.contains("://"))
      return true;
    boolean hasLetter = false;
    for (int i = 0; i < w.length(); i++)
    {
      if (Character.isLetter(w.charAt(i)))
      {
        hasLetter = true;
        break;
      }
    }
    return !hasLetter;
  }
}
