package typodev.keyboard.suggestions;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Privacy-first, 100% on-device persistent dictionary for Banglish (Romanized) -> Bangla mappings.
 * Learns ONLY when the user explicitly accepts or selects a suggestion ("Learn only when user accepts/corrects").
 * Protects passwords, numbers, and raw keystrokes from database pollution.
 */
public class BanglishUserDictionary
{
  private static final String STORE_FILE = "user_banglish_dictionary_v1.txt";
  private static final int MAX_ENTRIES_PER_WORD = 5;

  public static class Entry
  {
    public final String bangla;
    public int frequency;
    public long lastUsedTime;

    public Entry(String bangla, int frequency, long lastUsedTime)
    {
      this.bangla = bangla;
      this.frequency = frequency;
      this.lastUsedTime = lastUsedTime;
    }
  }

  private static volatile BanglishUserDictionary sInstance;
  private final Context _context;
  private final ConcurrentHashMap<String, List<Entry>> _mappings = new ConcurrentHashMap<>(256);
  private final ExecutorService _ioExecutor = Executors.newSingleThreadExecutor();

  public static BanglishUserDictionary instance(Context context)
  {
    if (sInstance == null)
    {
      synchronized (BanglishUserDictionary.class)
      {
        if (sInstance == null)
        {
          sInstance = new BanglishUserDictionary(context != null ? context.getApplicationContext() : null);
        }
      }
    }
    return sInstance;
  }

  public BanglishUserDictionary(Context context)
  {
    _context = context;
    if (_context != null)
    {
      loadFromDisk();
    }
  }

  /**
   * Learns a mapping between a Latin (Banglish) word and the selected Bengali word.
   * Only called when the user explicitly chooses or confirms a suggestion.
   */
  public void learnMapping(String latinWord, String bengaliWord)
  {
    if (latinWord == null || bengaliWord == null) return;

    String latin = latinWord.trim().toLowerCase(Locale.ROOT);
    String bangla = bengaliWord.trim();

    // Safety & Quality validations:
    // 1. Minimum 2 characters for Latin word (single letters like 'e' are language phonetics, not learned words)
    // 2. Must be pure Latin and pure Bengali
    // 3. Prevent passwords, numbers, URLs
    if (latin.length() < 2 || bangla.isEmpty()) return;
    if (!BanglishEngine.isLatinOnly(latin) || !BanglishEngine.isBengaliScript(bangla)) return;

    long now = System.currentTimeMillis();

    synchronized (_mappings)
    {
      List<Entry> list = _mappings.get(latin);
      if (list == null)
      {
        list = new ArrayList<>(2);
        list.add(new Entry(bangla, 1, now));
        _mappings.put(latin, list);
      }
      else
      {
        boolean found = false;
        for (Entry e : list)
        {
          if (e.bangla.equals(bangla))
          {
            e.frequency++;
            e.lastUsedTime = now;
            found = true;
            break;
          }
        }
        if (!found)
        {
          list.add(new Entry(bangla, 1, now));
        }

        // Sort descending by frequency and keep top N
        Collections.sort(list, new Comparator<Entry>()
        {
          @Override
          public int compare(Entry a, Entry b)
          {
            int cmp = Integer.compare(b.frequency, a.frequency);
            if (cmp != 0) return cmp;
            return Long.compare(b.lastUsedTime, a.lastUsedTime);
          }
        });

        if (list.size() > MAX_ENTRIES_PER_WORD)
        {
          list = new ArrayList<>(list.subList(0, MAX_ENTRIES_PER_WORD));
          _mappings.put(latin, list);
        }
      }
    }

    scheduleSave();
  }

  /**
   * Returns top Candidates for this Latin word based on user's learned choices.
   */
  public List<Candidate> getCandidates(String latinWord)
  {
    if (latinWord == null) return Collections.emptyList();
    String key = latinWord.trim().toLowerCase(Locale.ROOT);

    List<Entry> list = _mappings.get(key);
    if (list == null || list.isEmpty()) return Collections.emptyList();

    List<Candidate> results = new ArrayList<>(list.size());
    for (Entry e : list)
    {
      int freq = Math.min(255, 240 + Math.min(15, e.frequency * 3));
      Candidate c = new Candidate(e.bangla, Candidate.Source.AUTOCORRECT, freq, 0, 1.0f);
      c.recencyScore = 1.0f;
      results.add(c);
    }
    return results;
  }

  /**
   * Returns the single top Bengali word previously chosen for this Latin word, or null.
   */
  public String getTopBengali(String latinWord)
  {
    if (latinWord == null) return null;
    String key = latinWord.trim().toLowerCase(Locale.ROOT);
    List<Entry> list = _mappings.get(key);
    if (list != null && !list.isEmpty())
    {
      return list.get(0).bangla;
    }
    return null;
  }

  /**
   * Remove a specific mapping if user deletes it.
   */
  public void removeMapping(String latinWord, String bengaliWord)
  {
    if (latinWord == null || bengaliWord == null) return;
    String key = latinWord.trim().toLowerCase(Locale.ROOT);
    synchronized (_mappings)
    {
      List<Entry> list = _mappings.get(key);
      if (list != null)
      {
        for (int i = 0; i < list.size(); i++)
        {
          if (list.get(i).bangla.equals(bengaliWord.trim()))
          {
            list.remove(i);
            break;
          }
        }
        if (list.isEmpty())
        {
          _mappings.remove(key);
        }
      }
    }
    scheduleSave();
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
    File file = new File(_context.getFilesDir(), STORE_FILE);
    try (FileOutputStream fos = new FileOutputStream(file);
         OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8))
    {
      for (Map.Entry<String, List<Entry>> mapEntry : _mappings.entrySet())
      {
        String latin = mapEntry.getKey();
        for (Entry e : mapEntry.getValue())
        {
          osw.write(latin + "\t" + e.bangla + "\t" + e.frequency + "\t" + e.lastUsedTime + "\n");
        }
      }
      osw.flush();
    }
    catch (Throwable ignored) {}
  }

  private synchronized void loadFromDisk()
  {
    if (_context == null) return;
    File file = new File(_context.getFilesDir(), STORE_FILE);
    if (!file.exists()) return;

    try (FileInputStream fis = new FileInputStream(file);
         BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8)))
    {
      String line;
      while ((line = br.readLine()) != null)
      {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
        String[] parts = trimmed.split("\t");
        if (parts.length >= 2)
        {
          String latin = parts[0].trim().toLowerCase(Locale.ROOT);
          String bangla = parts[1].trim();
          int freq = 1;
          long time = System.currentTimeMillis();
          if (parts.length >= 3)
          {
            try { freq = Integer.parseInt(parts[2].trim()); } catch (Exception ignored) {}
          }
          if (parts.length >= 4)
          {
            try { time = Long.parseLong(parts[3].trim()); } catch (Exception ignored) {}
          }

          if (latin.length() >= 2 && BanglishEngine.isLatinOnly(latin) && BanglishEngine.isBengaliScript(bangla))
          {
            List<Entry> list = _mappings.get(latin);
            if (list == null)
            {
              list = new ArrayList<>(2);
              _mappings.put(latin, list);
            }
            list.add(new Entry(bangla, freq, time));
          }
        }
      }
    }
    catch (Throwable ignored) {}
  }
}
