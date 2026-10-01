package typodev.keyboard.suggestions;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;

/**
 * Fast Online Transliteration & AI Engine for Romanized Bengali (Banglish).
 * Uses Google Input Tools Transliteration (fast, free, dedicated to Bengali phonetics)
 * with Gemini AI / Google Translate fallback.
 */
public class BanglishOnlineTransliterator
{
  public interface Callback
  {
    void onSuccess(String query, List<String> results);
    void onError(String query, String error);
  }

  private static final ExecutorService sExecutor = Executors.newFixedThreadPool(2);

  private static void postToMain(Runnable r)
  {
    try
    {
      Looper looper = Looper.getMainLooper();
      if (looper != null)
      {
        new Handler(looper).post(r);
        return;
      }
    }
    catch (Throwable ignored) {}
    r.run();
  }

  // 100-entry LRU Cache for recent online queries
  private static final Map<String, List<String>> sCache = new LinkedHashMap<String, List<String>>(100, 0.75f, true)
  {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, List<String>> eldest)
    {
      return size() > 100;
    }
  };

  /**
   * Asynchronously fetches online transliteration for a Banglish word.
   */
  public static void fetchTransliteration(final String latinWord, final Context context, final Callback callback)
  {
    if (latinWord == null)
    {
      if (callback != null) callback.onError("", "Empty word");
      return;
    }

    final String query = latinWord.trim().toLowerCase(Locale.ROOT);
    if (query.length() < 2 || !BanglishEngine.isLatinOnly(query))
    {
      if (callback != null) callback.onError(query, "Invalid query");
      return;
    }

    // Check memory cache first
    synchronized (sCache)
    {
      List<String> cached = sCache.get(query);
      if (cached != null && !cached.isEmpty())
      {
        if (callback != null) callback.onSuccess(query, new ArrayList<>(cached));
        return;
      }
    }

    sExecutor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        List<String> results = fetchFromGoogleInputTools(query);

        // If Google Input Tools didn't return results, try Google Translate fallback
        if (results == null || results.isEmpty())
        {
          results = fetchFromGoogleTranslate(query);
        }

        if (results != null && !results.isEmpty())
        {
          synchronized (sCache)
          {
            sCache.put(query, results);
          }
          final List<String> finalResults = results;
          postToMain(new Runnable()
          {
            @Override
            public void run()
            {
              if (callback != null) callback.onSuccess(query, finalResults);
            }
          });
        }
        else
        {
          postToMain(new Runnable()
          {
            @Override
            public void run()
            {
              if (callback != null) callback.onError(query, "No results found");
            }
          });
        }
      }
    });
  }

  /**
   * Primary: Google Input Tools Transliteration endpoint.
   * Free, instant (~120ms), returns accurate colloquial Bangla transliterations.
   */
  private static List<String> fetchFromGoogleInputTools(String query)
  {
    try
    {
      String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
      String urlString = "https://inputtools.google.com/request?text=" + encoded + "&itc=bn-t-i0-und&num=4";

      URL url = new URL(urlString);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("User-Agent", "Mozilla/5.0");
      conn.setConnectTimeout(2500);
      conn.setReadTimeout(3000);

      int code = conn.getResponseCode();
      if (code == 200)
      {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null)
        {
          sb.append(line);
        }

        JSONArray root = new JSONArray(sb.toString());
        if ("SUCCESS".equalsIgnoreCase(root.optString(0)))
        {
          JSONArray array1 = root.optJSONArray(1);
          if (array1 != null && array1.length() > 0)
          {
            JSONArray array2 = array1.optJSONArray(0);
            if (array2 != null && array2.length() > 1)
            {
              JSONArray candidates = array2.optJSONArray(1);
              if (candidates != null && candidates.length() > 0)
              {
                List<String> list = new ArrayList<>(candidates.length());
                for (int i = 0; i < candidates.length(); i++)
                {
                  String cand = candidates.optString(i);
                  if (cand != null && !cand.trim().isEmpty() && BanglishEngine.isBengaliScript(cand))
                  {
                    if (!list.contains(cand.trim()))
                    {
                      list.add(cand.trim());
                    }
                  }
                }
                return list;
              }
            }
          }
        }
      }
    }
    catch (Throwable ignored) {}
    return null;
  }

  /**
   * Fallback: Google Translate client=gtx endpoint.
   */
  private static List<String> fetchFromGoogleTranslate(String query)
  {
    try
    {
      String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
      String urlString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=bn&dt=t&q=" + encoded;

      URL url = new URL(urlString);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setRequestProperty("User-Agent", "Mozilla/5.0");
      conn.setConnectTimeout(2500);
      conn.setReadTimeout(3000);

      int code = conn.getResponseCode();
      if (code == 200)
      {
        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null)
        {
          sb.append(line);
        }

        JSONArray root = new JSONArray(sb.toString());
        JSONArray sentences = root.optJSONArray(0);
        if (sentences != null && sentences.length() > 0)
        {
          StringBuilder resultBuilder = new StringBuilder();
          for (int i = 0; i < sentences.length(); i++)
          {
            JSONArray sentence = sentences.optJSONArray(i);
            if (sentence != null)
            {
              resultBuilder.append(sentence.optString(0));
            }
          }
          String res = resultBuilder.toString().trim();
          if (!res.isEmpty() && BanglishEngine.isBengaliScript(res))
          {
            return Collections.singletonList(res);
          }
        }
      }
    }
    catch (Throwable ignored) {}
    return null;
  }
}
