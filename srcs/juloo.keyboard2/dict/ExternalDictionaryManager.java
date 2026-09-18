package juloo.keyboard2.dict;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;
import juloo.keyboard2.Logs;

/**
 * High-performance, low-memory dictionary & wordlist manager based on FrostKeys / HeliBoard architecture.
 * Uses atomic, sorted in-memory lists with binary search prefix querying (O(log N)) to guarantee zero UI thread lag.
 */
public class ExternalDictionaryManager
{
  public static final String PREFS_EXT_DICTS = "external_dictionaries_meta";

  // Pre-configured FrostKeys / HeliBoard & curated wordlist URLs
  public static final String PRESET_BN_URL =
      "https://raw.githubusercontent.com/MinhasKamal/BengaliDictionary/master/BengaliWordList_439.txt";
  public static final String PRESET_EN_AOSP_URL =
      "https://codeberg.org/Helium314/aosp-dictionaries/raw/branch/main/wordlists/main_en_US.combined";
  public static final String PRESET_EN_LARGE_URL =
      "https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt";

  public static class DictInfo
  {
    public final String id;
    public final String title;
    public final String fileName;
    public final int wordCount;

    public DictInfo(String id, String title, String fileName, int wordCount)
    {
      this.id = id;
      this.title = title;
      this.fileName = fileName;
      this.wordCount = wordCount;
    }
  }

  public interface DownloadCallback
  {
    void onProgress(int wordsImported);
    void onSuccess(int totalWords);
    void onError(String message);
  }

  private static ExternalDictionaryManager _instance;

  public static synchronized ExternalDictionaryManager instance(Context context)
  {
    if (_instance == null)
    {
      _instance = new ExternalDictionaryManager(context.getApplicationContext());
    }
    return _instance;
  }

  private final Context context;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  // Volatile reference to immutable sorted list enables non-blocking, zero-lock reads from the IME thread
  private volatile List<String> loadedWords = Collections.emptyList();
  private volatile int totalWordCount = 0;
  private volatile boolean isLoaded = false;

  private ExternalDictionaryManager(Context context)
  {
    this.context = context;
    loadAllDictionariesAsync();
  }

  public int getWordCount()
  {
    return totalWordCount;
  }

  public boolean isLoaded()
  {
    return isLoaded;
  }

  private File getDictsDir()
  {
    File dir = new File(context.getFilesDir(), "external_dicts");
    if (!dir.exists())
    {
      dir.mkdirs();
    }
    return dir;
  }

  public List<DictInfo> getInstalledDictionaries()
  {
    List<DictInfo> list = new ArrayList<>();
    SharedPreferences prefs = context.getSharedPreferences(PREFS_EXT_DICTS, Context.MODE_PRIVATE);
    File dir = getDictsDir();
    File[] files = dir.listFiles();
    if (files != null)
    {
      for (File f : files)
      {
        if (f.isFile() && f.getName().endsWith(".txt"))
        {
          String id = f.getName().replace(".txt", "");
          String defaultTitle = getTitleForId(id);
          String title = prefs.getString("title_" + id, defaultTitle);
          int count = prefs.getInt("count_" + id, 0);
          list.add(new DictInfo(id, title, f.getName(), count));
        }
      }
    }
    return list;
  }

  private String getTitleForId(String id)
  {
    if (id.equals("dict_bn")) return "🇧🇩 বাংলা ডিকশনারি (Bengali Wordlist)";
    if (id.equals("dict_en_aosp")) return "🇬🇧 English AOSP Wordlist (FrostKeys)";
    if (id.equals("dict_en_large")) return "🇬🇧 English Standard Wordlist";
    return "📂 কাস্টম ডিকশনারি (Custom Wordlist)";
  }

  public void loadAllDictionariesAsync()
  {
    executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        reloadAllFromDisk();
      }
    });
  }

  private void reloadAllFromDisk()
  {
    TreeSet<String> uniqueWords = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    File dir = getDictsDir();
    File[] files = dir.listFiles();
    SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_EXT_DICTS, Context.MODE_PRIVATE).edit();

    if (files != null)
    {
      for (File f : files)
      {
        if (f.isFile() && f.getName().endsWith(".txt"))
        {
          int fileCount = 0;
          try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)))
          {
            String line;
            while ((line = br.readLine()) != null)
            {
              String word = cleanLine(line);
              if (word != null && !word.isEmpty())
              {
                uniqueWords.add(word);
                fileCount++;
              }
            }
          }
          catch (Exception e)
          {
            Logs.exn("ExternalDict", e);
          }
          String id = f.getName().replace(".txt", "");
          editor.putInt("count_" + id, fileCount);
        }
      }
    }
    editor.apply();

    List<String> list = new ArrayList<>(uniqueWords);
    // Atomic reference swap for instant non-blocking reads on UI thread
    this.loadedWords = Collections.unmodifiableList(list);
    this.totalWordCount = list.size();
    this.isLoaded = true;
  }

  public static String cleanLine(String line)
  {
    if (line == null) return null;
    String trimmed = line.trim();
    if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//"))
      return null;

    // 1. Helium314 / AOSP .combined format: word=hello,f=255,...
    if (trimmed.startsWith("word="))
    {
      int comma = trimmed.indexOf(',');
      if (comma > 5)
        trimmed = trimmed.substring(5, comma).trim();
      else
        trimmed = trimmed.substring(5).trim();
    }
    // 2. XML tag format: <w f="100">hello</w>
    else if (trimmed.startsWith("<w") && trimmed.contains(">") && trimmed.contains("</w>"))
    {
      int start = trimmed.indexOf('>') + 1;
      int end = trimmed.indexOf("</w>");
      if (end > start)
        trimmed = trimmed.substring(start, end).trim();
    }
    // 3. Tab or space separated: word\t100 or word 100
    else
    {
      int spaceIdx = trimmed.indexOf('\t');
      if (spaceIdx < 0) spaceIdx = trimmed.indexOf(' ');
      if (spaceIdx > 0)
      {
        trimmed = trimmed.substring(0, spaceIdx).trim();
      }
    }

    // Strip surrounding quotes
    if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() > 2)
    {
      trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
    }

    return trimmed.isEmpty() ? null : trimmed;
  }

  /**
   * Ultra-fast O(log N) prefix query using binary search.
   * Completely thread-safe and lock-free; cannot block the IME UI thread.
   */
  public List<String> query(String prefix, int maxResults)
  {
    if (prefix == null) return Collections.emptyList();
    String cleanPrefix = prefix.trim();
    if (cleanPrefix.isEmpty() || maxResults <= 0) return Collections.emptyList();

    List<String> words = this.loadedWords; // atomic read of immutable list
    if (words == null || words.isEmpty()) return Collections.emptyList();

    int len = words.size();
    int idx = Collections.binarySearch(words, cleanPrefix, String.CASE_INSENSITIVE_ORDER);
    if (idx < 0)
    {
      idx = -(idx + 1);
    }

    int prefixLen = cleanPrefix.length();
    List<String> results = new ArrayList<>(Math.min(maxResults, 8));

    for (int i = idx; i < len && results.size() < maxResults; i++)
    {
      String w = words.get(i);
      if (w.regionMatches(true, 0, cleanPrefix, 0, prefixLen))
      {
        results.add(w);
      }
      else
      {
        // Since words is ordered case-insensitively, no further words can match this prefix
        break;
      }
    }
    return results;
  }

  public void importFromStream(final InputStream inputStream, final String dictTitle, final DownloadCallback callback)
  {
    executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        String id = "dict_custom_" + System.currentTimeMillis();
        File outFile = new File(getDictsDir(), id + ".txt");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             FileOutputStream fos = new FileOutputStream(outFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8))
        {
          String line;
          int lineCount = 0;
          int imported = 0;
          while ((line = br.readLine()) != null)
          {
            String word = cleanLine(line);
            if (word != null && !word.isEmpty())
            {
              osw.write(word);
              osw.write("\n");
              imported++;
            }
            lineCount++;
            if (lineCount % 5000 == 0 && callback != null)
            {
              final int prog = imported;
              mainHandler.post(new Runnable()
              {
                @Override
                public void run() { callback.onProgress(prog); }
              });
            }
          }
          osw.flush();

          SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_EXT_DICTS, Context.MODE_PRIVATE).edit();
          editor.putString("title_" + id, dictTitle != null ? dictTitle : "📂 Custom Imported Words");
          editor.putInt("count_" + id, imported);
          editor.apply();

          reloadAllFromDisk();

          final int total = imported;
          mainHandler.post(new Runnable()
          {
            @Override
            public void run()
            {
              if (callback != null) callback.onSuccess(total);
            }
          });
        }
        catch (final Exception e)
        {
          mainHandler.post(new Runnable()
          {
            @Override
            public void run()
            {
              if (callback != null) callback.onError("Import failed: " + e.getMessage());
            }
          });
        }
      }
    });
  }

  public void downloadFromUrl(final String urlString, final String dictId, final String dictTitle, final DownloadCallback callback)
  {
    executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        HttpURLConnection conn = null;
        try
        {
          String currentUrl = urlString.trim();
          // Support following redirects (up to 5 hops for GitHub / Codeberg raw downloads)
          int redirects = 0;
          while (redirects < 5)
          {
            URL u = new URL(currentUrl);
            conn = (HttpURLConnection) u.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(25000);
            conn.setReadTimeout(45000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (TypoDev/Android)");

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == HttpURLConnection.HTTP_SEE_OTHER ||
                code == 307 || code == 308)
            {
              String location = conn.getHeaderField("Location");
              if (location != null && !location.isEmpty())
              {
                currentUrl = location;
                conn.disconnect();
                redirects++;
                continue;
              }
            }

            if (code < 200 || code >= 300)
            {
              throw new Exception("HTTP server error: " + code);
            }
            break;
          }

          InputStream in = conn.getInputStream();
          if (urlString.endsWith(".gz"))
          {
            in = new GZIPInputStream(in);
          }

          File outFile = new File(getDictsDir(), dictId + ".txt");
          int imported = 0;

          try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
               FileOutputStream fos = new FileOutputStream(outFile);
               OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8))
          {
            String line;
            int lineCount = 0;
            while ((line = br.readLine()) != null)
            {
              String word = cleanLine(line);
              if (word != null && !word.isEmpty())
              {
                osw.write(word);
                osw.write("\n");
                imported++;
              }
              lineCount++;
              if (lineCount % 5000 == 0 && callback != null)
              {
                final int prog = imported;
                mainHandler.post(new Runnable()
                {
                  @Override
                  public void run() { callback.onProgress(prog); }
                });
              }
            }
            osw.flush();
          }

          SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_EXT_DICTS, Context.MODE_PRIVATE).edit();
          editor.putString("title_" + dictId, dictTitle);
          editor.putInt("count_" + dictId, imported);
          editor.apply();

          reloadAllFromDisk();

          final int total = imported;
          mainHandler.post(new Runnable()
          {
            @Override
            public void run()
            {
              if (callback != null) callback.onSuccess(total);
            }
          });
        }
        catch (final Exception e)
        {
          mainHandler.post(new Runnable()
          {
            @Override
            public void run()
            {
              if (callback != null) callback.onError("Download error: " + e.getMessage());
            }
          });
        }
        finally
        {
          if (conn != null) conn.disconnect();
        }
      }
    });
  }

  public void deleteDictionary(final String id, final Runnable onDone)
  {
    executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        File f = new File(getDictsDir(), id + ".txt");
        if (f.exists())
        {
          f.delete();
        }
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_EXT_DICTS, Context.MODE_PRIVATE).edit();
        editor.remove("title_" + id);
        editor.remove("count_" + id);
        editor.apply();

        reloadAllFromDisk();

        if (onDone != null)
        {
          mainHandler.post(onDone);
        }
      }
    });
  }

  public void clearAllWords(final Runnable onDone)
  {
    executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        File dir = getDictsDir();
        File[] files = dir.listFiles();
        if (files != null)
        {
          for (File f : files)
          {
            if (f.isFile()) f.delete();
          }
        }
        context.getSharedPreferences(PREFS_EXT_DICTS, Context.MODE_PRIVATE).edit().clear().apply();
        loadedWords = Collections.emptyList();
        totalWordCount = 0;

        if (onDone != null)
        {
          mainHandler.post(onDone);
        }
      }
    });
  }

  public void downloadFromUrl(final String urlString, final DownloadCallback callback)
  {
    String dictId = "dict_custom_" + System.currentTimeMillis();
    String dictTitle = "🔗 Custom Wordlist";
    if (urlString.contains("bengali") || urlString.contains("Bengali"))
    {
      dictId = "dict_bn";
      dictTitle = "🇧🇩 বাংলা ডিকশনারি (Bengali Wordlist)";
    }
    else if (urlString.contains("Helium314") || urlString.contains("combined") || urlString.contains("frost"))
    {
      dictId = "dict_en_aosp";
      dictTitle = "🇬🇧 English AOSP Wordlist (FrostKeys)";
    }
    downloadFromUrl(urlString, dictId, dictTitle, callback);
  }

  public void importFromStream(final InputStream inputStream, final DownloadCallback callback)
  {
    importFromStream(inputStream, "📂 Imported File Wordlist", callback);
  }

  public void clearWords()
  {
    clearAllWords(null);
  }
}
