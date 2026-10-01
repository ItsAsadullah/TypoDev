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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Persistent store for blocked/removed suggestions.
 * Suggestions added here will never be presented in autocorrect,
 * predictions, or candidate strips.
 */
public class BlockedSuggestionsStore
{
  private static final String STORE_FILE_NAME = "blocked_suggestions_v1.txt";
  private static BlockedSuggestionsStore _instance;

  public static synchronized BlockedSuggestionsStore instance(Context context)
  {
    if (_instance == null)
    {
      _instance = new BlockedSuggestionsStore(context != null ? context.getApplicationContext() : null);
    }
    return _instance;
  }

  private final Context _context;
  private final ConcurrentHashMap<String, Boolean> _blocked = new ConcurrentHashMap<>();
  private final ExecutorService _ioExecutor = Executors.newSingleThreadExecutor();

  public BlockedSuggestionsStore(Context context)
  {
    _context = context;
    if (_context != null)
    {
      loadFromDisk();
    }
  }

  public void blockWord(String rawWord)
  {
    if (rawWord == null) return;
    String clean = clean(rawWord);
    if (clean.isEmpty()) return;

    _blocked.put(clean.toLowerCase(Locale.ROOT), Boolean.TRUE);

    // Also remove from user vocabulary store and prediction transitions
    if (_context != null)
    {
      try
      {
        UserVocabularyStore.instance(_context).removeWord(clean);
        NextWordPredictor.instance(_context).removeWord(clean);
      }
      catch (Throwable ignored) {}
    }

    scheduleSave();
  }

  public boolean isBlocked(String rawWord)
  {
    if (rawWord == null) return false;
    String clean = clean(rawWord);
    if (clean.isEmpty()) return false;
    return _blocked.containsKey(clean.toLowerCase(Locale.ROOT));
  }

  public void unblockWord(String rawWord)
  {
    if (rawWord == null) return;
    String clean = clean(rawWord);
    if (clean.isEmpty()) return;
    _blocked.remove(clean.toLowerCase(Locale.ROOT));
    scheduleSave();
  }

  public List<String> getBlockedWords()
  {
    List<String> list = new ArrayList<>(_blocked.keySet());
    Collections.sort(list);
    return list;
  }

  public void clearBlocked()
  {
    _blocked.clear();
    scheduleSave();
  }

  public int getBlockedCount()
  {
    return _blocked.size();
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
      for (String w : _blocked.keySet())
      {
        osw.write(w);
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
        String w = clean(line);
        if (!w.isEmpty())
        {
          _blocked.put(w.toLowerCase(Locale.ROOT), Boolean.TRUE);
        }
      }
    }
    catch (Exception ignored) {}
  }

  private static String clean(String raw)
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
}
