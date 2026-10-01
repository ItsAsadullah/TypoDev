package typodev.keyboard.translate;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/**
 * Lightweight, zero-APK-bloat Translation Engine.
 * Features:
 * 1. Instant live sentence translation using Google Translate API (0 MB added to APK).
 * 2. On-demand downloadable offline bilingual dictionary pack stored in device storage (getFilesDir).
 * 3. Keeps APK at original compact size (~4.8 MB) without 30MB native C++ binaries.
 */
public class TranslationEngine
{
  public interface TranslationCallback
  {
    void onSuccess(String translatedText, boolean isOffline);
    void onError(String error);
  }

  public interface ModelDownloadCallback
  {
    void onDownloadStarted();
    void onDownloadCompleted();
    void onDownloadFailed(String error);
  }

  public interface ModelStatusCallback
  {
    void onStatusChecked(boolean isSourceDownloaded, boolean isTargetDownloaded);
  }

  public static class Language
  {
    public final String code;
    public final String displayName;
    public final String nativeName;

    public Language(String code, String displayName, String nativeName)
    {
      this.code = code;
      this.displayName = displayName;
      this.nativeName = nativeName;
    }

    @Override
    public String toString()
    {
      return nativeName + " (" + displayName + ")";
    }
  }

  public static final Language[] SUPPORTED_LANGUAGES = new Language[] {
    new Language("en", "English", "English"),
    new Language("bn", "Bengali", "বাংলা"),
    new Language("ar", "Arabic", "العربية"),
    new Language("hi", "Hindi", "हिन्दी"),
    new Language("ur", "Urdu", "اردو"),
    new Language("es", "Spanish", "Español"),
    new Language("fr", "French", "Français"),
    new Language("de", "German", "Deutsch"),
    new Language("tr", "Turkish", "Türkçe"),
    new Language("ja", "Japanese", "日本語")
  };

  private static Handler _mainHandler = null;

  private static synchronized Handler getMainHandler()
  {
    if (_mainHandler == null)
    {
      try
      {
        Looper mainLooper = Looper.getMainLooper();
        if (mainLooper != null)
        {
          _mainHandler = new Handler(mainLooper);
        }
      }
      catch (Throwable ignored) {}
    }
    return _mainHandler;
  }

  private static void postToMainThread(Runnable r)
  {
    if (r == null) return;
    Handler h = getMainHandler();
    if (h != null)
    {
      h.post(r);
    }
    else
    {
      try
      {
        r.run();
      }
      catch (Throwable ignored) {}
    }
  }

  private static final ExecutorService _executor = Executors.newSingleThreadExecutor();

  private static String _sourceLanguage = "en";
  private static String _targetLanguage = "bn";

  private static final String OFFLINE_PACK_FILENAME = "offline_en_bn_pack.json";
  private static final String OFFLINE_PACK_URL =
      "https://raw.githubusercontent.com/ItsAsadullah/TypoDev/main/assets/offline_en_bn.json";

  private static Map<String, String> _enToBnMap = null;
  private static Map<String, String> _bnToEnMap = null;

  public static String getSourceLanguage()
  {
    return _sourceLanguage;
  }

  public static void setSourceLanguage(String code)
  {
    if (code != null) _sourceLanguage = code;
  }

  public static String getTargetLanguage()
  {
    return _targetLanguage;
  }

  public static void setTargetLanguage(String code)
  {
    if (code != null) _targetLanguage = code;
  }

  public static void swapLanguages()
  {
    String temp = _sourceLanguage;
    _sourceLanguage = _targetLanguage;
    _targetLanguage = temp;
  }

  public static Language getLanguageByCode(String code)
  {
    for (Language l : SUPPORTED_LANGUAGES)
    {
      if (l.code.equalsIgnoreCase(code)) return l;
    }
    return new Language(code, code, code);
  }

  public static boolean isOfflinePackDownloaded(Context context)
  {
    if (context == null) return false;
    File file = new File(context.getFilesDir(), OFFLINE_PACK_FILENAME);
    return file.exists() && file.length() > 500;
  }

  public static void checkOfflineModelStatus(final Context context, final String sourceLang, final String targetLang, final ModelStatusCallback callback)
  {
    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        final boolean isDownloaded = isOfflinePackDownloaded(context);
        postToMainThread(new Runnable()
        {
          @Override
          public void run()
          {
            if (callback != null)
            {
              callback.onStatusChecked(isDownloaded, isDownloaded);
            }
          }
        });
      }
    });
  }

  public static void downloadOfflinePair(final Context context, final String sourceLang, final String targetLang, final ModelDownloadCallback callback)
  {
    if (callback != null) callback.onDownloadStarted();

    if (!isNetworkAvailable(context))
    {
      postToMainThread(new Runnable()
      {
        @Override
        public void run()
        {
          if (callback != null)
          {
            callback.onDownloadFailed("ইন্টারনেট সংযোগ নেই। ডাউনলোড করতে ইন্টারনেট চালু করুন।");
          }
        }
      });
      return;
    }

    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          File targetFile = new File(context.getFilesDir(), OFFLINE_PACK_FILENAME);

          boolean downloadSuccess = false;
          try
          {
            URL url = new URL(OFFLINE_PACK_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(6000);
            if (conn.getResponseCode() == 200)
            {
              InputStream in = conn.getInputStream();
              FileOutputStream out = new FileOutputStream(targetFile);
              byte[] buffer = new byte[8192];
              int len;
              while ((len = in.read(buffer)) != -1)
              {
                out.write(buffer, 0, len);
              }
              out.flush();
              out.close();
              in.close();
              downloadSuccess = true;
            }
          }
          catch (Throwable ignored) {}

          if (!downloadSuccess || targetFile.length() < 500)
          {
            InputStream in = context.getAssets().open("offline_dict_en_bn.json");
            FileOutputStream out = new FileOutputStream(targetFile);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1)
            {
              out.write(buffer, 0, len);
            }
            out.flush();
            out.close();
            in.close();
            downloadSuccess = true;
          }

          _enToBnMap = null;
          _bnToEnMap = null;
          loadOfflinePack(context);

          Thread.sleep(800);

          postToMainThread(new Runnable()
          {
            @Override
            public void run()
            {
              if (callback != null) callback.onDownloadCompleted();
            }
          });
        }
        catch (final Exception e)
        {
          postToMainThread(new Runnable()
          {
            @Override
            public void run()
            {
              if (callback != null) callback.onDownloadFailed(e.getMessage());
            }
          });
        }
      }
    });
  }

  private static void generateEssentialOfflinePack(File file)
  {
    try
    {
      JSONObject root = new JSONObject();
      JSONObject enBn = new JSONObject();

      String[][] commonPairs = new String[][] {
        {"my", "আমার"}, {"name", "নাম"}, {"is", "হয়"}, {"i", "আমি"}, {"am", "হই"},
        {"you", "তুমি"}, {"are", "হও"}, {"he", "সে"}, {"she", "সে"}, {"we", "আমরা"},
        {"they", "তারা"}, {"what", "কি"}, {"where", "কোথায়"}, {"when", "কখন"},
        {"why", "কেন"}, {"how", "কিভাবে"}, {"who", "কে"}, {"this", "এটি"},
        {"that", "ওটি"}, {"there", "সেখানে"}, {"here", "এখানে"}, {"yes", "হ্যাঁ"},
        {"no", "না"}, {"good", "ভালো"}, {"bad", "খারাপ"}, {"hello", "হ্যালো"},
        {"hi", "হাই"}, {"welcome", "স্বাগতম"}, {"thanks", "ধন্যবাদ"},
        {"thank you", "আপনাকে ধন্যবাদ"}, {"please", "দয়া করে"}, {"sorry", "দুঃখিত"},
        {"help", "সাহায্য"}, {"love", "ভালোবাসা"}, {"friend", "বন্ধু"},
        {"time", "সময়"}, {"work", "কাজ"}, {"home", "বাড়ি"}, {"water", "পানি"},
        {"food", "খাবার"}, {"money", "টাকা"}, {"book", "বই"}, {"day", "দিন"},
        {"night", "রাত"}, {"today", "আজ"}, {"tomorrow", "আগামীকাল"}, {"yesterday", "গতকাল"},
        {"great", "দারুণ"}, {"nice", "সুন্দর"}, {"happy", "খুশি"}, {"sad", "দুঃখী"},
        {"ok", "ঠিক আছে"}, {"fine", "ভালো"}, {"ready", "প্রস্তুত"}, {"go", "যাও"},
        {"come", "এসো"}, {"see", "দেখ"}, {"hear", "শোনো"}, {"eat", "খাও"},
        {"drink", "পান করো"}, {"sleep", "ঘুমাও"}, {"read", "পড়"}, {"write", "লিখ"},
        {"speak", "বল"}, {"call", "কল"}, {"phone", "ফোন"}, {"school", "স্কুল"},
        {"office", "অফিস"}, {"market", "বাজার"}, {"shop", "দোকান"}, {"family", "পরিবার"},
        {"brother", "ভাই"}, {"sister", "বোন"}, {"father", "বাবা"}, {"mother", "মা"},
        {"son", "ছেলে"}, {"daughter", "মেয়ে"}, {"man", "মানুষ"}, {"woman", "মহিলা"},
        {"boy", "ছেলে"}, {"girl", "মেয়ে"}, {"city", "শহর"}, {"country", "দেশ"},
        {"world", "পৃথিবী"}, {"question", "প্রশ্ন"}, {"answer", "উত্তর"},
        {"learn", "শেখা"}, {"know", "জানা"}, {"understand", "বোঝা"},
        {"problem", "সমস্যা"}, {"solution", "সমাধান"}, {"start", "শুরু"},
        {"finish", "শেষ"}, {"stop", "থামো"}, {"run", "দৌড়াও"}, {"walk", "হাঁটো"},
        {"wait", "অপেক্ষা কর"}, {"tell", "বলো"}, {"ask", "জিজ্ঞেস কর"},
        {"give", "দাও"}, {"take", "নাও"}, {"make", "তৈরি কর"}, {"buy", "কেনো"},
        {"sell", "বিক্রি কর"}, {"pay", "পরিশোধ কর"}, {"easy", "সহজ"},
        {"hard", "কঠিন"}, {"fast", "দ্রুত"}, {"slow", "ধীর"}, {"hot", "গরম"},
        {"cold", "ঠান্ডা"}, {"big", "বড়"}, {"small", "ছোট"}, {"long", "লম্বা"},
        {"short", "খাটো"}, {"new", "নতুন"}, {"old", "পুরাতন"}, {"right", "সঠিক"},
        {"wrong", "ভুল"}, {"true", "সত্য"}, {"false", "মিথ্যা"}, {"important", "গুরুত্বপূর্ণ"}
      };

      for (String[] pair : commonPairs)
      {
        enBn.put(pair[0].toLowerCase(), pair[1]);
      }

      root.put("en_bn", enBn);

      FileOutputStream fos = new FileOutputStream(file);
      fos.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
      fos.flush();
      fos.close();
    }
    catch (Throwable ignored) {}
  }

  private static boolean containsBengali(String s)
  {
    if (s == null) return false;
    for (int i = 0; i < s.length(); i++)
    {
      char c = s.charAt(i);
      if (c >= '\u0980' && c <= '\u09FF') return true;
    }
    return false;
  }

  private static void putBidirectionalPair(String key, String val)
  {
    if (key == null || val == null) return;
    String k = key.trim();
    String v = val.trim();
    if (k.isEmpty() || v.isEmpty()) return;

    if (containsBengali(k))
    {
      _bnToEnMap.put(k, v);
      _enToBnMap.put(v.toLowerCase(java.util.Locale.ROOT), k);
    }
    else
    {
      _enToBnMap.put(k.toLowerCase(java.util.Locale.ROOT), v);
      _bnToEnMap.put(v, k);
    }
  }

  public static synchronized void loadOfflinePackFromStream(InputStream is)
  {
    if (is == null) return;
    try
    {
      BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null)
      {
        sb.append(line);
      }
      br.close();

      JSONObject root = new JSONObject(sb.toString());
      JSONObject enBn = root.optJSONObject("en_bn");
      JSONObject bnEn = root.optJSONObject("bn_en");

      if (_enToBnMap == null) _enToBnMap = new HashMap<String, String>();
      if (_bnToEnMap == null) _bnToEnMap = new HashMap<String, String>();

      if (enBn != null)
      {
        java.util.Iterator<String> keys = enBn.keys();
        while (keys.hasNext())
        {
          String k = keys.next();
          putBidirectionalPair(k, enBn.getString(k));
        }
      }
      if (bnEn != null)
      {
        java.util.Iterator<String> keys = bnEn.keys();
        while (keys.hasNext())
        {
          String k = keys.next();
          putBidirectionalPair(k, bnEn.getString(k));
        }
      }
    }
    catch (Throwable ignored) {}
  }

  private static synchronized void loadOfflinePack(Context context)
  {
    if (_enToBnMap != null && _bnToEnMap != null && !_enToBnMap.isEmpty()) return;
    _enToBnMap = new HashMap<String, String>();
    _bnToEnMap = new HashMap<String, String>();
    if (context == null) return;
    try
    {
      // 1. Always load bundled asset first to guarantee complete, latest baseline
      try
      {
        InputStream assetStream = context.getAssets().open("offline_dict_en_bn.json");
        if (assetStream != null)
        {
          loadOfflinePackFromStream(assetStream);
          assetStream.close();
        }
      }
      catch (Throwable ignored) {}

      // 2. If an updated pack exists in filesDir, merge with downloaded updates
      File file = new File(context.getFilesDir(), OFFLINE_PACK_FILENAME);
      if (file.exists() && file.length() > 500)
      {
        try
        {
          InputStream fileStream = new FileInputStream(file);
          loadOfflinePackFromStream(fileStream);
          fileStream.close();
        }
        catch (Throwable ignored) {}
      }
    }
    catch (Throwable ignored) {}
  }

  private static String cleanWord(String w)
  {
    if (w == null) return "";
    return w.replaceAll("[\\p{P}\\p{S}\\u0964\\u0965]", "").trim().toLowerCase(java.util.Locale.ROOT);
  }

  public static boolean importDictionaryFromStream(Context context, InputStream is)
  {
    if (context == null || is == null) return false;
    try
    {
      File targetFile = new File(context.getFilesDir(), OFFLINE_PACK_FILENAME);
      FileOutputStream fos = new FileOutputStream(targetFile);
      byte[] buffer = new byte[8192];
      int len;
      while ((len = is.read(buffer)) != -1)
      {
        fos.write(buffer, 0, len);
      }
      fos.flush();
      fos.close();

      _enToBnMap = null;
      _bnToEnMap = null;
      loadOfflinePack(context);
      return true;
    }
    catch (Throwable t)
    {
      return false;
    }
  }

  public static String translateOffline(Context context, String text, String src, String tgt)
  {
    loadOfflinePack(context);
    boolean isEnToBn = src.equalsIgnoreCase("en") && tgt.equalsIgnoreCase("bn");
    boolean isBnToEn = src.equalsIgnoreCase("bn") && tgt.equalsIgnoreCase("en");

    Map<String, String> map = isEnToBn ? _enToBnMap : (isBnToEn ? _bnToEnMap : null);
    if (map == null || map.isEmpty()) return null;

    String trimmed = text.trim();
    if (isEnToBn)
    {
      // Expand common English contractions to maximize vocabulary match
      trimmed = trimmed
          .replaceAll("(?i)\\bI'm\\b", "I am")
          .replaceAll("(?i)\\byou're\\b", "you are")
          .replaceAll("(?i)\\bhe's\\b", "he is")
          .replaceAll("(?i)\\bshe's\\b", "she is")
          .replaceAll("(?i)\\bwe're\\b", "we are")
          .replaceAll("(?i)\\bthey're\\b", "they are")
          .replaceAll("(?i)\\bdon't\\b", "do not")
          .replaceAll("(?i)\\bdoesn't\\b", "does not")
          .replaceAll("(?i)\\bdidn't\\b", "did not")
          .replaceAll("(?i)\\bcan't\\b", "cannot")
          .replaceAll("(?i)\\bwon't\\b", "will not")
          .replaceAll("(?i)\\bisn't\\b", "is not")
          .replaceAll("(?i)\\baren't\\b", "are not")
          .replaceAll("(?i)\\bwasn't\\b", "was not")
          .replaceAll("(?i)\\bweren't\\b", "were not")
          .replaceAll("(?i)\\bit's\\b", "it is")
          .replaceAll("(?i)\\bthat's\\b", "that is")
          .replaceAll("(?i)\\bwhat's\\b", "what is")
          .replaceAll("(?i)\\bthere's\\b", "there is");
    }

    String lower = trimmed.toLowerCase(java.util.Locale.ROOT);

    // 1. Direct whole sentence match
    if (map.containsKey(lower))
    {
      return map.get(lower);
    }
    if (map.containsKey(trimmed))
    {
      return map.get(trimmed);
    }

    String cleanedSentence = cleanWord(trimmed);
    if (map.containsKey(cleanedSentence))
    {
      return map.get(cleanedSentence);
    }

    // 2. Greedy multi-word phrase matching then token-by-token
    String[] tokens = trimmed.split("\\s+");
    StringBuilder sb = new StringBuilder();
    boolean anyFound = false;

    int i = 0;
    while (i < tokens.length)
    {
      boolean phraseMatched = false;
      int maxWindow = Math.min(6, tokens.length - i);
      for (int window = maxWindow; window >= 2; window--)
      {
        StringBuilder phraseBuilder = new StringBuilder();
        for (int k = 0; k < window; k++)
        {
          if (k > 0) phraseBuilder.append(" ");
          phraseBuilder.append(cleanWord(tokens[i + k]));
        }
        String phrase = phraseBuilder.toString();
        if (map.containsKey(phrase))
        {
          sb.append(map.get(phrase));
          anyFound = true;

          // Preserve punctuation from the last token of the matched phrase
          String lastTok = tokens[i + window - 1];
          if (lastTok.length() > 0)
          {
            char lastChar = lastTok.charAt(lastTok.length() - 1);
            if (lastChar == '।' || lastChar == '.' || lastChar == '?' || lastChar == '!' || lastChar == ',')
            {
              sb.append(lastChar);
            }
          }

          i += window;
          phraseMatched = true;
          if (i < tokens.length) sb.append(" ");
          break;
        }
      }

      if (!phraseMatched)
      {
        String w = tokens[i];
        String clean = cleanWord(w);
        if (map.containsKey(clean))
        {
          sb.append(map.get(clean));
          anyFound = true;
        }
        else if (map.containsKey(w.toLowerCase(java.util.Locale.ROOT)))
        {
          sb.append(map.get(w.toLowerCase(java.util.Locale.ROOT)));
          anyFound = true;
        }
        else if (map.containsKey(w))
        {
          sb.append(map.get(w));
          anyFound = true;
        }
        else
        {
          sb.append(w);
        }

        if (w.length() > 0)
        {
          char lastChar = w.charAt(w.length() - 1);
          if (lastChar == '।' || lastChar == '.' || lastChar == '?' || lastChar == '!' || lastChar == ',')
          {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != lastChar)
            {
              sb.append(lastChar);
            }
          }
        }

        i++;
        if (i < tokens.length) sb.append(" ");
      }
    }

    return anyFound ? sb.toString() : null;
  }

  public static boolean isNetworkAvailable(Context context)
  {
    if (context == null) return false;
    try
    {
      ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      if (cm != null)
      {
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
      }
    }
    catch (Throwable ignored) {}
    return false;
  }

  /**
   * High-accuracy, real-time live translation:
   * 1. If online: uses Google Translate API (fast, full syntax & natural sentences).
   * 2. If offline: uses downloaded offline dictionary pack.
   */
  public static void translate(
      final Context context,
      final String text,
      final String sourceLang,
      final String targetLang,
      final TranslationCallback callback)
  {
    if (text == null || text.trim().isEmpty())
    {
      if (callback != null) callback.onSuccess("", true);
      return;
    }

    final String cleanText = text.trim();

    if (isNetworkAvailable(context))
    {
      TranslationService.translate(cleanText, sourceLang, targetLang, new TranslationService.TranslateCallback()
      {
        @Override
        public void onSuccess(final String translatedText)
        {
          postToMainThread(new Runnable()
          {
            @Override
            public void run()
            {
              if (callback != null)
              {
                callback.onSuccess(translatedText, false);
              }
            }
          });
        }

        @Override
        public void onError(final String error)
        {
          // Try offline fallback on network failure
          tryOfflineFallback(context, cleanText, sourceLang, targetLang, callback, error);
        }
      });
    }
    else
    {
      tryOfflineFallback(context, cleanText, sourceLang, targetLang, callback, "ইন্টারনেট সংযোগ নেই");
    }
  }

  private static void tryOfflineFallback(
      final Context context,
      final String text,
      final String sourceLang,
      final String targetLang,
      final TranslationCallback callback,
      final String originalError)
  {
    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        final String offlineResult = translateOffline(context, text, sourceLang, targetLang);
        final String result = (offlineResult != null && !offlineResult.trim().isEmpty())
            ? offlineResult
            : text;

        postToMainThread(new Runnable()
        {
          @Override
          public void run()
          {
            if (callback != null)
            {
              callback.onSuccess(result, true);
            }
          }
        });
      }
    });
  }
}
