package typodev.keyboard.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Base64;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class OpenAiCompatibleProvider implements AiProvider
{
  public static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1";
  public static final String GROQ_DEFAULT_MODEL = "llama-3.3-70b-versatile";
  public static final String GROQ_VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";

  public static final String XAI_BASE_URL = "https://api.x.ai/v1";
  public static final String XAI_DEFAULT_MODEL = "grok-3-mini";
  public static final String XAI_VISION_MODEL = "grok-2-vision-latest";

  public static final String DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1";
  public static final String DEEPSEEK_DEFAULT_MODEL = "deepseek-chat";

  public static final String OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1";

  private static final String[] GROQ_FALLBACK_MODELS = {
    "llama-3.3-70b-versatile",
    "llama-3.1-8b-instant",
    "openai/gpt-oss-120b",
    "openai/gpt-oss-20b",
    "meta-llama/llama-4-scout-17b-16e-instruct",
    "compound-beta"
  };

  private static final String[] XAI_FALLBACK_MODELS = {
    "grok-3-mini",
    "grok-3",
    "grok-2-latest"
  };

  private static volatile String _cachedModelsKey = "";
  private static volatile long _cachedModelsTimeMs = 0L;
  private static volatile List<String> _cachedModelsList = new ArrayList<>();

  private static OpenAiCompatibleProvider _instance;
  private static final ExecutorService _executor = Executors.newSingleThreadExecutor();
  private static volatile Handler _mainHandler;

  private static void runOnMainThread(Runnable r)
  {
    try
    {
      if (_mainHandler == null)
      {
        Looper main = Looper.getMainLooper();
        if (main != null) _mainHandler = new Handler(main);
      }
      if (_mainHandler != null)
      {
        _mainHandler.post(r);
        return;
      }
    }
    catch (Throwable ignored) {}
    r.run();
  }

  public static synchronized OpenAiCompatibleProvider instance()
  {
    if (_instance == null)
    {
      _instance = new OpenAiCompatibleProvider();
    }
    return _instance;
  }

  @Override
  public String getName()
  {
    return "Groq / OpenAI / Grok";
  }

  @Override
  public String getActiveModel(Context context)
  {
    return getModel(context);
  }

  public static String cleanApiKey(String raw)
  {
    if (raw == null) return "";
    String cleaned = raw.trim();
    if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2)
      cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
    if (cleaned.startsWith("key="))
      cleaned = cleaned.substring(4).trim();
    if (cleaned.regionMatches(true, 0, "Bearer ", 0, 7))
      cleaned = cleaned.substring(7).trim();
    return cleaned.trim();
  }

  public static String getRawSavedApiKey(Context context)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String raw = cleanApiKey(prefs.getString(PREF_OPENAI_API_KEY, ""));
    if (raw.isEmpty())
    {
      try
      {
        SharedPreferences dp = typodev.keyboard.DirectBootAwarePreferences.get_shared_preferences(context);
        raw = cleanApiKey(dp.getString(PREF_OPENAI_API_KEY, ""));
      }
      catch (Throwable ignored) {}
    }
    return raw;
  }

  public static String getApiKey(Context context)
  {
    String raw = getRawSavedApiKey(context);
    if (!raw.isEmpty())
    {
      return raw;
    }
    // Check if user pasted a Groq (gsk_), xAI Grok (xai-), or OpenAI (sk-) key in the Gemini field
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String geminiRaw = cleanApiKey(prefs.getString(GeminiAiService.PREF_GEMINI_API_KEY, ""));
    if (Manager.isOpenAiCompatibleKey(geminiRaw))
    {
      return geminiRaw;
    }
    return "";
  }

  public static void setApiKey(Context context, String key)
  {
    String clean = cleanApiKey(key);
    if (Manager.isGeminiKey(clean))
    {
      GeminiAiService.setGeminiApiKeyOnly(context, clean);
      Manager.setActiveProviderType(context, ProviderType.GEMINI);
      return;
    }

    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(PREF_OPENAI_API_KEY, clean)
        .apply();
    try
    {
      typodev.keyboard.DirectBootAwarePreferences.get_shared_preferences(context)
          .edit()
          .putString(PREF_OPENAI_API_KEY, clean)
          .apply();
    }
    catch (Throwable ignored) {}

    // Auto-configure Base URL and Model if key clearly belongs to Groq, xAI Grok, or OpenRouter
    if (clean.startsWith("gsk_"))
    {
      String curUrl = getRawBaseUrl(context);
      if (curUrl.isEmpty() || !curUrl.toLowerCase(Locale.ROOT).contains("groq.com"))
      {
        setBaseUrl(context, GROQ_BASE_URL);
      }
      String curModel = getRawModel(context);
      if (!isCompatibleGroqModel(curModel))
      {
        setModel(context, GROQ_DEFAULT_MODEL);
      }
    }
    else if (clean.startsWith("xai-"))
    {
      String curUrl = getRawBaseUrl(context);
      if (curUrl.isEmpty() || !curUrl.toLowerCase(Locale.ROOT).contains("x.ai"))
      {
        setBaseUrl(context, XAI_BASE_URL);
      }
      String curModel = getRawModel(context);
      if (curModel.isEmpty() || !curModel.toLowerCase(Locale.ROOT).contains("grok"))
      {
        setModel(context, XAI_DEFAULT_MODEL);
      }
    }
    else if (clean.startsWith("sk-or-"))
    {
      String curUrl = getRawBaseUrl(context);
      if (curUrl.isEmpty() || curUrl.contains("api.openai.com"))
      {
        setBaseUrl(context, OPENROUTER_BASE_URL);
      }
    }
  }

  private static String getRawBaseUrl(Context context)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String url = prefs.getString(PREF_OPENAI_BASE_URL, "").trim();
    if (url.isEmpty())
    {
      try
      {
        SharedPreferences dp = typodev.keyboard.DirectBootAwarePreferences.get_shared_preferences(context);
        url = dp.getString(PREF_OPENAI_BASE_URL, "").trim();
      }
      catch (Throwable ignored) {}
    }
    return url;
  }

  public static String normalizeBaseUrl(String url, String apiKey)
  {
    String u = (url != null) ? url.trim() : "";
    while (u.endsWith("/"))
    {
      u = u.substring(0, u.length() - 1).trim();
    }
    if (u.endsWith("/chat/completions"))
    {
      u = u.substring(0, u.length() - "/chat/completions".length());
      while (u.endsWith("/")) u = u.substring(0, u.length() - 1).trim();
    }

    String k = (apiKey != null) ? apiKey.trim() : "";
    if (k.startsWith("gsk_"))
    {
      return GROQ_BASE_URL;
    }
    else if (k.startsWith("xai-"))
    {
      return XAI_BASE_URL;
    }
    else if (k.startsWith("sk-or-"))
    {
      return OPENROUTER_BASE_URL;
    }
    else if (k.startsWith("sk-"))
    {
      String lower = u.toLowerCase(Locale.ROOT);
      if (u.isEmpty() || lower.contains("groq.com") || lower.contains("api.x.ai"))
      {
        return DEFAULT_OPENAI_BASE_URL;
      }
    }

    if (u.equalsIgnoreCase("https://api.groq.com") || u.equalsIgnoreCase("https://api.groq.com/v1"))
    {
      return GROQ_BASE_URL;
    }
    if (u.equalsIgnoreCase("https://api.x.ai"))
    {
      return XAI_BASE_URL;
    }

    return u.isEmpty() ? DEFAULT_OPENAI_BASE_URL : u;
  }

  public static String getBaseUrl(Context context)
  {
    return normalizeBaseUrl(getRawBaseUrl(context), getApiKey(context));
  }

  public static void setBaseUrl(Context context, String url)
  {
    String clean = normalizeBaseUrl(url, getRawSavedApiKey(context));
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(PREF_OPENAI_BASE_URL, clean)
        .apply();
    try
    {
      typodev.keyboard.DirectBootAwarePreferences.get_shared_preferences(context)
          .edit()
          .putString(PREF_OPENAI_BASE_URL, clean)
          .apply();
    }
    catch (Throwable ignored) {}
  }

  private static String getRawModel(Context context)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String model = prefs.getString(PREF_OPENAI_MODEL, "").trim();
    if (model.isEmpty())
    {
      try
      {
        SharedPreferences dp = typodev.keyboard.DirectBootAwarePreferences.get_shared_preferences(context);
        model = dp.getString(PREF_OPENAI_MODEL, "").trim();
      }
      catch (Throwable ignored) {}
    }
    return model;
  }

  public static boolean isCompatibleGroqModel(String model)
  {
    if (model == null || model.trim().isEmpty()) return false;
    String lower = model.trim().toLowerCase(Locale.ROOT);
    if (lower.startsWith("gpt-")
        || lower.startsWith("o1-")
        || lower.startsWith("o3-")
        || lower.equals("deepseek-chat")
        || lower.equals("deepseek-reasoner")
        || lower.startsWith("claude-")
        || lower.startsWith("gemini-")
        || lower.startsWith("grok-")
        || lower.equals("qwen/qwen3-32b")
        || lower.equals("llama3-70b-8192")
        || lower.equals("llama3-8b-8192")
        || lower.equals("mixtral-8x7b-32768")
        || lower.equals("gemma2-9b-it"))
    {
      return false;
    }
    return true;
  }

  public static String resolveModel(String rawModel, String baseUrl, String apiKey)
  {
    String m = (rawModel != null) ? rawModel.trim() : "";
    String u = (baseUrl != null) ? baseUrl.toLowerCase(Locale.ROOT) : "";
    String k = (apiKey != null) ? apiKey.trim() : "";

    if (k.startsWith("gsk_") || u.contains("groq.com"))
    {
      return isCompatibleGroqModel(m) ? m : GROQ_DEFAULT_MODEL;
    }
    if (k.startsWith("xai-") || u.contains("api.x.ai"))
    {
      if (m.isEmpty() || !m.toLowerCase(Locale.ROOT).contains("grok"))
      {
        return XAI_DEFAULT_MODEL;
      }
      return m;
    }
    if (u.contains("api.deepseek.com"))
    {
      if (m.isEmpty() || !m.toLowerCase(Locale.ROOT).contains("deepseek"))
      {
        return DEEPSEEK_DEFAULT_MODEL;
      }
      return m;
    }
    return m.isEmpty() ? DEFAULT_OPENAI_MODEL : m;
  }

  public static String getModel(Context context)
  {
    String apiKey = getApiKey(context);
    String baseUrl = getBaseUrl(context);
    return resolveModel(getRawModel(context), baseUrl, apiKey);
  }

  public static void setModel(Context context, String model)
  {
    String clean = (model != null && !model.trim().isEmpty()) ? model.trim() : DEFAULT_OPENAI_MODEL;
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(PREF_OPENAI_MODEL, clean)
        .apply();
    try
    {
      typodev.keyboard.DirectBootAwarePreferences.get_shared_preferences(context)
          .edit()
          .putString(PREF_OPENAI_MODEL, clean)
          .apply();
    }
    catch (Throwable ignored) {}
  }

  public static String stripReasoningThinkTags(String text)
  {
    if (text == null) return "";
    String cleaned = text.replaceAll("(?s)<think>.*?</think>", "").trim();
    return cleaned.isEmpty() ? text.trim() : cleaned;
  }

  public interface ModelsCallback
  {
    void onModelsFetched(List<String> models);
  }

  public static boolean isSupportedChatModelId(String id)
  {
    if (id == null || id.trim().isEmpty()) return false;
    String lower = id.trim().toLowerCase(Locale.ROOT);
    String[] excluded = {
      "whisper", "tts", "playai", "guard", "safeguard", "embed",
      "moderation", "orpheus", "allam", "rerank", "audio", "speech",
      "dall-e", "imagen", "babbage", "davinci", "realtime", "transcribe"
    };
    for (String ex : excluded)
    {
      if (lower.contains(ex)) return false;
    }
    return true;
  }

  private static int getModelPriorityScore(String id)
  {
    if (id == null) return 999;
    String lower = id.toLowerCase(Locale.ROOT);
    if (lower.equals("llama-3.3-70b-versatile")) return 1;
    if (lower.equals("openai/gpt-oss-120b")) return 2;
    if (lower.contains("llama-4-maverick")) return 3;
    if (lower.contains("llama-4-scout")) return 4;
    if (lower.contains("kimi-k2")) return 5;
    if (lower.equals("llama-3.1-8b-instant")) return 6;
    if (lower.equals("openai/gpt-oss-20b")) return 7;
    if (lower.contains("llama-3.3") || lower.contains("llama-3.1")) return 8;
    if (lower.contains("compound-beta")) return 9;
    if (lower.contains("grok-3-mini")) return 10;
    if (lower.contains("grok-3") || lower.contains("grok-2")) return 11;
    if (lower.equals("gpt-4o-mini")) return 12;
    if (lower.equals("gpt-4o")) return 13;
    if (lower.equals("deepseek-chat")) return 14;
    if (lower.contains("deepseek")) return 15;
    if (lower.contains("qwen")) return 16;
    if (lower.contains("llama") || lower.contains("gemma") || lower.contains("mixtral")) return 20;
    return 50;
  }

  public static List<String> fetchAccessibleChatModels(String baseUrl, String apiKey, boolean forceRefresh)
  {
    if (baseUrl == null || baseUrl.isEmpty() || apiKey == null || apiKey.isEmpty())
    {
      return new ArrayList<>();
    }
    String cacheKey = baseUrl + "|" + apiKey.hashCode();
    long now = System.currentTimeMillis();
    if (!forceRefresh
        && cacheKey.equals(_cachedModelsKey)
        && !_cachedModelsList.isEmpty()
        && (now - _cachedModelsTimeMs < 10L * 60 * 1000))
    {
      return new ArrayList<>(_cachedModelsList);
    }

    List<String> discovered = new ArrayList<>();
    HttpURLConnection conn = null;
    try
    {
      URL url = new URL(baseUrl + "/models");
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      conn.setConnectTimeout(8000);
      conn.setReadTimeout(10000);
      conn.setRequestProperty("Accept", "application/json");
      conn.setRequestProperty("User-Agent", "TypoBoard/1.0 (Android; OpenAI-Compatible)");
      conn.setRequestProperty("Authorization", "Bearer " + apiKey);

      if (conn.getResponseCode() == 200)
      {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)))
        {
          String line;
          while ((line = br.readLine()) != null) sb.append(line);
        }
        JSONObject root = new JSONObject(sb.toString());
        JSONArray data = root.optJSONArray("data");
        if (data != null)
        {
          for (int i = 0; i < data.length(); i++)
          {
            JSONObject m = data.optJSONObject(i);
            if (m == null) continue;
            if (m.has("active") && !m.optBoolean("active", true)) continue;
            String id = m.optString("id", "").trim();
            if (isSupportedChatModelId(id) && !discovered.contains(id))
            {
              discovered.add(id);
            }
          }
        }
        java.util.Collections.sort(discovered, new java.util.Comparator<String>()
        {
          @Override
          public int compare(String a, String b)
          {
            int pa = getModelPriorityScore(a);
            int pb = getModelPriorityScore(b);
            if (pa != pb) return pa - pb;
            return a.compareTo(b);
          }
        });
        if (!discovered.isEmpty())
        {
          _cachedModelsKey = cacheKey;
          _cachedModelsTimeMs = now;
          _cachedModelsList = new ArrayList<>(discovered);
        }
      }
    }
    catch (Exception ignored) {}
    finally
    {
      if (conn != null) conn.disconnect();
    }
    return discovered;
  }

  public static void fetchModelsAsync(
      final String baseUrl,
      final String apiKey,
      final ModelsCallback callback)
  {
    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        final List<String> list = fetchAccessibleChatModels(
            normalizeBaseUrl(baseUrl, apiKey),
            cleanApiKey(apiKey),
            true);
        runOnMainThread(new Runnable()
        {
          @Override
          public void run()
          {
            if (callback != null) callback.onModelsFetched(list);
          }
        });
      }
    });
  }

  @Override
  public void generate(final Context context, final String systemPrompt, final String userText, final Callback callback)
  {
    generateWithOptions(context, systemPrompt, userText, null, null, 2048, callback);
  }

  public void generateWithOptions(
      final Context context,
      final String systemPrompt,
      final String userText,
      final byte[] imageBytes,
      final String mimeType,
      final int maxOutputTokens,
      final Callback callback)
  {
    final String apiKey = getApiKey(context);
    if (apiKey.isEmpty())
    {
      if (callback != null) callback.onError("API key not set. Please set your Groq / OpenAI API key in AI Settings.");
      return;
    }

    final String baseUrl = getBaseUrl(context);
    final String configuredModel = getModel(context);
    final float temp = Manager.getTemperature(context);

    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        List<String> modelsToTry = new ArrayList<>();
        boolean isGroq = apiKey.startsWith("gsk_") || baseUrl.toLowerCase(Locale.ROOT).contains("groq.com");
        boolean isXai = apiKey.startsWith("xai-") || baseUrl.toLowerCase(Locale.ROOT).contains("api.x.ai");
        boolean hasImage = imageBytes != null && imageBytes.length > 0;

        // Dynamically query GET {baseUrl}/models to discover active models accessible by this API key
        List<String> liveModels = fetchAccessibleChatModels(baseUrl, apiKey, false);

        if (hasImage)
        {
          if (isGroq && (liveModels.isEmpty() || liveModels.contains(GROQ_VISION_MODEL)))
          {
            modelsToTry.add(GROQ_VISION_MODEL);
          }
          else if (isXai)
          {
            modelsToTry.add(XAI_VISION_MODEL);
          }
        }

        if (!liveModels.isEmpty())
        {
          if (liveModels.contains(configuredModel) && !modelsToTry.contains(configuredModel))
          {
            modelsToTry.add(configuredModel);
          }
          for (String live : liveModels)
          {
            if (!modelsToTry.contains(live))
            {
              modelsToTry.add(live);
              if (modelsToTry.size() >= 6) break;
            }
          }
        }
        else
        {
          if (!modelsToTry.contains(configuredModel))
          {
            modelsToTry.add(configuredModel);
          }
          if (isGroq)
          {
            for (String m : GROQ_FALLBACK_MODELS)
            {
              if (!modelsToTry.contains(m)) modelsToTry.add(m);
            }
          }
          else if (isXai)
          {
            for (String m : XAI_FALLBACK_MODELS)
            {
              if (!modelsToTry.contains(m)) modelsToTry.add(m);
            }
          }
        }

        String primaryErr = null;
        String lastErr = "Request failed";

        for (int i = 0; i < modelsToTry.size(); i++)
        {
          String model = modelsToTry.get(i);
          HttpURLConnection conn = null;
          try
          {
            String endpoint = baseUrl + "/chat/completions";
            URL url = new URL(endpoint);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "TypoBoard/1.0 (Android; OpenAI-Compatible)");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);

            JSONObject root = new JSONObject();
            root.put("model", model);
            root.put("temperature", Math.round(temp * 100.0) / 100.0);
            if (maxOutputTokens > 0)
            {
              int safeMaxTokens = isGroq ? Math.min(maxOutputTokens, 4096) : maxOutputTokens;
              root.put("max_tokens", safeMaxTokens);
            }

            JSONArray messages = new JSONArray();
            if (systemPrompt != null && !systemPrompt.trim().isEmpty())
            {
              JSONObject sysMsg = new JSONObject();
              sysMsg.put("role", "system");
              sysMsg.put("content", systemPrompt.trim());
              messages.put(sysMsg);
            }

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");

            if (hasImage)
            {
              String b64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
              String mime = (mimeType != null && !mimeType.isEmpty()) ? mimeType : "image/jpeg";
              JSONArray contentArr = new JSONArray();

              JSONObject textPart = new JSONObject();
              textPart.put("type", "text");
              textPart.put("text", userText != null ? userText : "");
              contentArr.put(textPart);

              JSONObject imgUrlObj = new JSONObject();
              imgUrlObj.put("url", "data:" + mime + ";base64," + b64);
              JSONObject imgPart = new JSONObject();
              imgPart.put("type", "image_url");
              imgPart.put("image_url", imgUrlObj);
              contentArr.put(imgPart);

              userMsg.put("content", contentArr);
            }
            else
            {
              userMsg.put("content", userText != null ? userText : "");
            }

            messages.put(userMsg);
            root.put("messages", messages);

            byte[] payload = root.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream())
            {
              os.write(payload);
              os.flush();
            }

            int statusCode = conn.getResponseCode();
            InputStream stream = (statusCode >= 200 && statusCode < 300)
                ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            if (stream != null)
            {
              try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
              {
                String line;
                while ((line = reader.readLine()) != null)
                {
                  sb.append(line).append('\n');
                }
              }
            }

            String respStr = sb.toString();

            if (statusCode >= 200 && statusCode < 300)
            {
              JSONObject respJson = new JSONObject(respStr);
              JSONArray choices = respJson.optJSONArray("choices");
              if (choices != null && choices.length() > 0)
              {
                JSONObject first = choices.getJSONObject(0);
                JSONObject message = first.optJSONObject("message");
                if (message != null)
                {
                  // Persist verified working model if different
                  if (!model.equals(configuredModel) && !hasImage)
                  {
                    setModel(context, model);
                  }
                  String content = stripReasoningThinkTags(message.optString("content", ""));
                  postSuccess(callback, content);
                  return;
                }
              }
              lastErr = "No text generated by model.";
            }
            else
            {
              String errMsg = "HTTP " + statusCode;
              try
              {
                JSONObject errJson = new JSONObject(respStr);
                if (errJson.has("error"))
                {
                  Object errNode = errJson.get("error");
                  if (errNode instanceof JSONObject)
                  {
                    errMsg = ((JSONObject) errNode).optString("message", errMsg);
                  }
                  else if (errNode instanceof String)
                  {
                    errMsg = (String) errNode;
                  }
                }
              }
              catch (Exception ignored) {}

              if (primaryErr == null) primaryErr = errMsg;
              lastErr = errMsg;

              String lowerErr = errMsg.toLowerCase(Locale.ROOT);
              boolean isModelOrRateErr = statusCode == 404
                  || statusCode == 429
                  || statusCode == 503
                  || (statusCode == 400 && (lowerErr.contains("model")
                      || lowerErr.contains("decommissioned")
                      || lowerErr.contains("not exist")
                      || lowerErr.contains("not supported")));

              if (isModelOrRateErr && i < modelsToTry.size() - 1)
              {
                continue;
              }
              postError(callback, primaryErr != null ? primaryErr : errMsg);
              return;
            }
          }
          catch (Exception e)
          {
            lastErr = "Connection failed: " + e.getMessage();
            if (e instanceof java.net.UnknownHostException)
            {
              postError(callback, "No internet connection. Check your mobile data or Wi-Fi.");
              return;
            }
          }
          finally
          {
            if (conn != null) conn.disconnect();
          }
        }

        postError(callback, primaryErr != null ? primaryErr : lastErr);
      }
    });
  }

  private static void postSuccess(final Callback cb, final String result)
  {
    if (cb == null) return;
    runOnMainThread(new Runnable()
    {
      @Override
      public void run()
      {
        cb.onSuccess(result);
      }
    });
  }

  private static void postError(final Callback cb, final String error)
  {
    if (cb == null) return;
    runOnMainThread(new Runnable()
    {
      @Override
      public void run()
      {
        cb.onError(error);
      }
    });
  }
}
