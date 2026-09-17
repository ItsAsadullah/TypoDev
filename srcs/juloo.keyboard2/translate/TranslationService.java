package juloo.keyboard2.translate;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;

public class TranslationService
{
  public interface TranslateCallback
  {
    void onSuccess(String translatedText);
    void onError(String error);
  }

  private static final ExecutorService _executor = Executors.newSingleThreadExecutor();
  private static final Handler _mainHandler = new Handler(Looper.getMainLooper());

  /**
   * Translate text using the instant Google Translate API endpoint (client=gtx).
   * @param text The input text to translate.
   * @param sourceLang The source language code (e.g., "auto", "bn", "en").
   * @param targetLang The target language code (e.g., "en", "bn").
   */
  public static void translate(final String text, final String sourceLang, final String targetLang, final TranslateCallback callback)
  {
    if (text == null || text.trim().isEmpty())
    {
      callback.onError("No text to translate");
      return;
    }

    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8.name());
          String sl = (sourceLang == null || sourceLang.isEmpty()) ? "auto" : sourceLang;
          String tl = (targetLang == null || targetLang.isEmpty()) ? "en" : targetLang;

          String urlString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=" + sl + "&tl=" + tl + "&dt=t&q=" + encoded;
          URL url = new URL(urlString);
          HttpURLConnection conn = (HttpURLConnection) url.openConnection();
          conn.setRequestMethod("GET");
          conn.setRequestProperty("User-Agent", "Mozilla/5.0");
          conn.setConnectTimeout(10000);
          conn.setReadTimeout(15000);

          int responseCode = conn.getResponseCode();
          if (responseCode == 200)
          {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
            {
              sb.append(line);
            }

            JSONArray root = new JSONArray(sb.toString());
            JSONArray sentences = root.getJSONArray(0);
            StringBuilder resultBuilder = new StringBuilder();
            for (int i = 0; i < sentences.length(); i++)
            {
              JSONArray sentence = sentences.getJSONArray(i);
              resultBuilder.append(sentence.getString(0));
            }

            final String translated = resultBuilder.toString().trim();
            _mainHandler.post(new Runnable()
            {
              @Override
              public void run()
              {
                callback.onSuccess(translated);
              }
            });
          }
          else
          {
            final String err = "Translation failed with HTTP code: " + responseCode;
            _mainHandler.post(new Runnable()
            {
              @Override
              public void run()
              {
                callback.onError(err);
              }
            });
          }
        }
        catch (final Exception e)
        {
          _mainHandler.post(new Runnable()
          {
            @Override
            public void run()
            {
              callback.onError("Translation error: " + e.getMessage());
            }
          });
        }
      }
    });
  }
}
