package typodev.keyboard.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeminiAiService
{
  public static final String PREF_GEMINI_API_KEY = "gemini_api_key";
  public static final String PREF_GEMINI_WORKING_MODEL = "gemini_working_model";
  public static final String PREF_CACHED_GEMINI_MODELS = "pref_cached_gemini_models";
  public static final String PREF_GEMINI_MODELS_LAST_FETCH = "pref_gemini_models_last_fetch";
  public static final String PREF_AI_EMOJIFY = "pref_ai_emojify";

  public static final String DEFAULT_MODEL = "gemini-2.0-flash";

  // Official, high-performance Gemini models matching FrostKeys architecture
  private static final String[] CANDIDATE_MODELS = {
    "gemini-2.0-flash",
    "gemini-1.5-flash",
    "gemini-2.0-flash-lite",
    "gemini-1.5-flash-8b",
    "gemini-1.5-pro"
  };

  private static volatile long quotaCooldownUntilMs = 0L;

  public enum Action
  {
    GRAMMAR_FIX("Fix all spelling, punctuation, and grammatical errors in the following text. Preserve the original language and meaning. Output ONLY the corrected text, with no explanations or preamble."),
    TONE_PROFESSIONAL("Rewrite the following text in a professional, polite, and confident tone. Preserve the original language. Output ONLY the rewritten text."),
    TONE_CASUAL("Rewrite the following text in a casual, warm, and friendly tone. Preserve the original language. Output ONLY the rewritten text."),
    REWRITE_POLISH("Polish and improve the clarity, flow, and elegance of the following text. Preserve the original language. Output ONLY the polished text."),
    MOOD_FORMAL("Rewrite the following text with formal, elegant, and respectful phrasing. Output ONLY the result."),
    MOOD_ENTHUSIASTIC("Rewrite the following text with an upbeat, enthusiastic, and positive tone. Output ONLY the result."),
    CONTENT_TITLE("Generate 5 catchy, high-CTR, click-worthy titles or headlines based on the following text. Output ONLY the numbered list (1 to 5) of titles."),
    CONTENT_DESCRIPTION("Write a comprehensive, engaging, and clear description based on the following text. Preserve the target language. Output ONLY the description text.");

    private final String systemPrompt;

    Action(String prompt)
    {
      this.systemPrompt = prompt;
    }

    public String getPrompt()
    {
      return systemPrompt;
    }
  }

  public interface AiCallback
  {
    void onSuccess(String resultText);
    void onError(String errorMessage);
  }

  private static final ExecutorService _executor = Executors.newCachedThreadPool();
  private static final Handler _mainHandler = new Handler(Looper.getMainLooper());

  private static SharedPreferences getSafeDefaultPrefs(Context context)
  {
    try
    {
      return PreferenceManager.getDefaultSharedPreferences(context);
    }
    catch (Throwable ignored)
    {
      return typodev.keyboard.DirectBootAwarePreferences.get_shared_preferences(context);
    }
  }

  public static String getGeminiApiKey(Context context)
  {
    SharedPreferences prefs = getSafeDefaultPrefs(context);
    String raw = OpenAiCompatibleProvider.cleanApiKey(prefs != null ? prefs.getString(PREF_GEMINI_API_KEY, "") : "");
    if (raw.isEmpty())
    {
      try
      {
        SharedPreferences dp = typodev.keyboard.DirectBootAwarePreferences.get_shared_preferences(context);
        raw = OpenAiCompatibleProvider.cleanApiKey(dp.getString(PREF_GEMINI_API_KEY, ""));
      }
      catch (Throwable ignored) {}
    }
    if (AiProvider.Manager.isOpenAiCompatibleKey(raw))
    {
      return "";
    }
    return raw;
  }

  public static String getApiKey(Context context)
  {
    if (AiProvider.Manager.getActiveProviderType(context) == AiProvider.ProviderType.OPENAI_COMPATIBLE)
    {
      String openAiKey = OpenAiCompatibleProvider.getApiKey(context);
      if (!openAiKey.isEmpty()) return openAiKey;
    }
    String geminiKey = getGeminiApiKey(context);
    if (!geminiKey.isEmpty()) return geminiKey;
    return OpenAiCompatibleProvider.getApiKey(context);
  }

  public static void setGeminiApiKeyOnly(Context context, String apiKey)
  {
    String cleaned = OpenAiCompatibleProvider.cleanApiKey(apiKey);
    SharedPreferences prefs = getSafeDefaultPrefs(context);
    if (prefs != null)
    {
      prefs.edit().putString(PREF_GEMINI_API_KEY, cleaned).apply();
    }
    try
    {
      if (typodev.keyboard.DirectBootAwarePreferences.isUserUnlocked(context))
      {
        typodev.keyboard.DirectBootAwarePreferences.get_shared_preferences(context)
            .edit()
            .remove(PREF_GEMINI_API_KEY)
            .apply();
      }
    }
    catch (Throwable ignored) {}
  }

  public static void setApiKey(Context context, String apiKey)
  {
    String cleaned = OpenAiCompatibleProvider.cleanApiKey(apiKey);
    if (AiProvider.Manager.isOpenAiCompatibleKey(cleaned))
    {
      OpenAiCompatibleProvider.setApiKey(context, cleaned);
      AiProvider.Manager.setActiveProviderType(context, AiProvider.ProviderType.OPENAI_COMPATIBLE);
      SharedPreferences prefs = getSafeDefaultPrefs(context);
      String existingGem = OpenAiCompatibleProvider.cleanApiKey(prefs != null ? prefs.getString(PREF_GEMINI_API_KEY, "") : "");
      if (AiProvider.Manager.isOpenAiCompatibleKey(existingGem))
      {
        setGeminiApiKeyOnly(context, "");
      }
      return;
    }
    setGeminiApiKeyOnly(context, cleaned);
  }

  public static String getModel(Context context)
  {
    SharedPreferences prefs = getSafeDefaultPrefs(context);
    String model = prefs != null ? prefs.getString(PREF_GEMINI_WORKING_MODEL, DEFAULT_MODEL) : DEFAULT_MODEL;
    if (model == null || model.trim().isEmpty() || model.contains("pro-vision"))
    {
      model = DEFAULT_MODEL;
    }
    return model.trim();
  }

  public static void setModel(Context context, String model)
  {
    SharedPreferences prefs = getSafeDefaultPrefs(context);
    if (prefs != null && model != null)
    {
      prefs.edit().putString(PREF_GEMINI_WORKING_MODEL, model.trim()).apply();
    }
  }

  public static boolean isEmojifyEnabled(Context context)
  {
    SharedPreferences prefs = typodev.keyboard.DirectBootAwarePreferences.get_shared_preferences(context);
    return prefs.getBoolean(PREF_AI_EMOJIFY, true);
  }

  public static void setEmojifyEnabled(Context context, boolean enabled)
  {
    SharedPreferences prefs = getSafeDefaultPrefs(context);
    if (prefs != null)
    {
      prefs.edit().putBoolean(PREF_AI_EMOJIFY, enabled).apply();
    }
    typodev.keyboard.DirectBootAwarePreferences.get_shared_preferences(context)
        .edit().putBoolean(PREF_AI_EMOJIFY, enabled).apply();
  }

  public static String stripEmojis(String text)
  {
    if (text == null) return "";
    String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
    String stripped = normalized
        .replaceAll("[\\p{So}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+", "")
        .replaceAll("[\\u2600-\\u27BF\\uE000-\\uF8FF\\uFE0E\\uFE0F\\u200D\\u20E3]", "");
    String[] lines = stripped.split("\n", -1);
    StringBuilder sb = new StringBuilder(stripped.length());
    for (int i = 0; i < lines.length; i++)
    {
      if (i > 0) sb.append('\n');
      String line = lines[i].replaceAll("[ \\t\\x0B\\f]{2,}", " ").replaceAll("[ \\t\\x0B\\f]+$", "");
      if (!line.startsWith("  "))
      {
        line = line.replaceAll("^[ \\t\\x0B\\f]+", "");
      }
      sb.append(line);
    }
    return sb.toString().replaceAll("\\n{3,}", "\n\n").trim();
  }

  public static void fetchAndCacheModels(final Context context, final String apiKey, final boolean forceRefresh)
  {
    if (apiKey == null || apiKey.trim().isEmpty()) return;
    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        HttpURLConnection conn = null;
        try
        {
          SharedPreferences prefs = getSafeDefaultPrefs(context);
          if (prefs == null) return;
          long lastFetch = prefs.getLong(PREF_GEMINI_MODELS_LAST_FETCH, 0L);
          long now = System.currentTimeMillis();
          if (!forceRefresh && (now - lastFetch < 7L * 24 * 3600 * 1000) && prefs.contains(PREF_CACHED_GEMINI_MODELS))
          {
            return;
          }

          String urlStr = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey.trim();
          conn = (HttpURLConnection) new URL(urlStr).openConnection();
          conn.setRequestMethod("GET");
          conn.setConnectTimeout(8000);
          conn.setReadTimeout(12000);

          if (conn.getResponseCode() == 200)
          {
            String resp = readStreamSafely(conn.getInputStream());
            JSONObject json = new JSONObject(resp);
            JSONArray modelsArr = json.optJSONArray("models");
            if (modelsArr != null)
            {
              List<String> flashModels = new ArrayList<>();
              for (int i = 0; i < modelsArr.length(); i++)
              {
                JSONObject m = modelsArr.getJSONObject(i);
                String name = m.optString("name", "").replace("models/", "");
                JSONArray methods = m.optJSONArray("supportedGenerationMethods");
                boolean canGenerate = false;
                if (methods != null)
                {
                  for (int j = 0; j < methods.length(); j++)
                  {
                    if ("generateContent".equals(methods.optString(j)))
                    {
                      canGenerate = true;
                      break;
                    }
                  }
                }
                if (canGenerate && isSupportedTextFlashModel(name))
                {
                  flashModels.add(name);
                }
              }

              java.util.Collections.sort(flashModels, new java.util.Comparator<String>()
              {
                @Override
                public int compare(String a, String b)
                {
                  boolean aLite = a.contains("lite") || a.contains("8b");
                  boolean bLite = b.contains("lite") || b.contains("8b");
                  if (aLite && !bLite) return -1;
                  if (!aLite && bLite) return 1;
                  boolean a15 = a.contains("1.5");
                  boolean b15 = b.contains("1.5");
                  if (a15 && !b15) return -1;
                  if (!a15 && b15) return 1;
                  return b.compareTo(a);
                }
              });

              if (!flashModels.isEmpty())
              {
                StringBuilder cached = new StringBuilder();
                for (int i = 0; i < flashModels.size(); i++)
                {
                  if (i > 0) cached.append(",");
                  cached.append(flashModels.get(i));
                }
                prefs.edit()
                    .putString(PREF_CACHED_GEMINI_MODELS, cached.toString())
                    .putLong(PREF_GEMINI_MODELS_LAST_FETCH, now)
                    .apply();
              }
            }
          }
        }
        catch (Exception ignored) {}
        finally
        {
          if (conn != null) conn.disconnect();
        }
      }
    });
  }

  private static boolean isSupportedTextFlashModel(String model)
  {
    String lower = model.toLowerCase();
    if (!lower.contains("flash") && !lower.contains("pro")) return false;
    String[] unsupported = {"tts", "image", "imagen", "embedding", "embed", "live", "audio", "video"};
    for (String u : unsupported)
    {
      if (lower.contains(u)) return false;
    }
    return true;
  }

  public static String extractCandidateText(JSONObject candidate)
  {
    if (candidate == null) return null;
    JSONObject content = candidate.optJSONObject("content");
    if (content == null) return null;
    JSONArray parts = content.optJSONArray("parts");
    if (parts == null || parts.length() == 0) return null;

    StringBuilder nonThought = new StringBuilder();
    for (int i = 0; i < parts.length(); i++)
    {
      JSONObject part = parts.optJSONObject(i);
      if (part == null || part.optBoolean("thought", false)) continue;
      String text = part.optString("text", "");
      if (!text.isEmpty())
      {
        nonThought.append(text);
      }
    }
    String result = nonThought.toString().trim();
    if (!result.isEmpty()) return result;

    JSONObject first = parts.optJSONObject(0);
    return first != null ? first.optString("text", "").trim() : null;
  }

  public static void processPrompt(final Context context, final String promptText, final AiCallback callback)
  {
    processPromptWithImage(context, promptText, null, null, callback);
  }

  /** Worked math needs more room than a short keyboard writing suggestion. */
  public static void processMathPrompt(final Context context, final String promptText, final AiCallback callback)
  {
    processPromptWithImage(context, promptText, null, null, 0.3f, 8192, callback);
  }

  public static void processPromptWithImage(
      final Context context,
      final String promptText,
      final byte[] imageBytes,
      final String mimeType,
      final AiCallback callback)
  {
    processPromptWithImage(context, promptText, imageBytes, mimeType,
        imageBytes != null ? 0.7f : 0.3f, 2048, callback);
  }

  public static void extractImageText(Context context, byte[] image, AiCallback callback) {
    processPromptWithImage(context, ImageTextExtractor.PROMPT, image, "image/jpeg", 0f, 8192, callback);
  }

  private static String readStreamSafely(java.io.InputStream stream) throws java.io.IOException
  {
    if (stream == null) return "";
    StringBuilder sb = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
    {
      String line;
      while ((line = br.readLine()) != null)
      {
        sb.append(line).append('\n');
      }
    }
    return sb.toString().trim();
  }

  private static void processPromptWithImage(
      final Context context, final String promptText, final byte[] imageBytes,
      final String mimeType, final float temperature, final int maxOutputTokens,
      final AiCallback callback)
  {
    if (AiProvider.Manager.getActiveProviderType(context) == AiProvider.ProviderType.OPENAI_COMPATIBLE)
    {
      OpenAiCompatibleProvider.instance().generateWithOptions(
          context,
          null,
          promptText,
          imageBytes,
          mimeType,
          maxOutputTokens,
          new AiProvider.Callback()
          {
            @Override
            public void onSuccess(String resultText)
            {
              if (callback != null) callback.onSuccess(resultText);
            }

            @Override
            public void onError(String errorMessage)
            {
              if (callback != null) callback.onError(errorMessage);
            }
          });
      return;
    }

    final String apiKey = getGeminiApiKey(context);
    if (apiKey.isEmpty())
    {
      if (callback != null)
        callback.onError("AI API key is missing. Please configure your Gemini or Groq API key in settings.");
      return;
    }

    final long remainingCooldown = quotaCooldownUntilMs - System.currentTimeMillis();
    if (remainingCooldown > 0)
    {
      long seconds = (remainingCooldown + 999L) / 1000L;
      if (callback != null)
        callback.onError("The usage limit has been reached. Try again in " + seconds + " seconds.");
      return;
    }

    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        SharedPreferences prefs = getSafeDefaultPrefs(context);
        fetchAndCacheModels(context, apiKey, false);

        String cachedModelsStr = prefs != null ? prefs.getString(PREF_CACHED_GEMINI_MODELS, "") : "";
        List<String> modelsToTry = new ArrayList<>();

        String selectedModel = getModel(context);
        if (selectedModel != null && !selectedModel.isEmpty() && isSupportedTextFlashModel(selectedModel))
        {
          modelsToTry.add(selectedModel);
        }

        if (!cachedModelsStr.isEmpty())
        {
          String[] split = cachedModelsStr.split(",");
          for (String s : split)
          {
            String trimmed = s.trim();
            if (!trimmed.isEmpty() && !modelsToTry.contains(trimmed))
            {
              modelsToTry.add(trimmed);
            }
          }
        }

        for (String m : CANDIDATE_MODELS)
        {
          if (!modelsToTry.contains(m))
          {
            modelsToTry.add(m);
          }
        }

        String lastError = "";
        long pendingCooldownDelayMs = 0L;
        boolean primaryModelInvalid = false;

        for (int i = 0; i < modelsToTry.size(); i++)
        {
          String modelName = modelsToTry.get(i);
          HttpURLConnection conn = null;
          try
          {
            JSONObject partObj = new JSONObject();
            partObj.put("text", promptText);

            JSONArray partsArray = new JSONArray();
            partsArray.put(partObj);

            if (imageBytes != null && imageBytes.length > 0)
            {
              String b64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);
              JSONObject inlineData = new JSONObject();
              inlineData.put("mimeType", (mimeType != null && !mimeType.isEmpty()) ? mimeType : "image/jpeg");
              inlineData.put("data", b64);

              JSONObject imgPart = new JSONObject();
              imgPart.put("inlineData", inlineData);
              partsArray.put(imgPart);
            }

            JSONObject contentObj = new JSONObject();
            contentObj.put("parts", partsArray);

            JSONArray contentsArray = new JSONArray();
            contentsArray.put(contentObj);

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contentsArray);

            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", temperature);
            genConfig.put("maxOutputTokens", maxOutputTokens);
            requestBody.put("generationConfig", genConfig);

            String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);

            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream())
            {
              os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            if (responseCode >= 200 && responseCode < 300)
            {
              quotaCooldownUntilMs = 0L;
              if (prefs != null && (i == 0 || primaryModelInvalid))
              {
                prefs.edit().putString(PREF_GEMINI_WORKING_MODEL, modelName).apply();
              }

              String respBody = readStreamSafely(conn.getInputStream());
              JSONObject jsonResp = new JSONObject(respBody);
              JSONArray candidates = jsonResp.optJSONArray("candidates");
              if (candidates != null && candidates.length() > 0)
              {
                JSONObject candidate = candidates.getJSONObject(0);
                if (maxOutputTokens > 2048 && "MAX_TOKENS".equals(candidate.optString("finishReason")))
                {
                  _mainHandler.post(() -> { if (callback != null) callback.onError(context.getString(typodev.keyboard.R.string.math_incomplete)); });
                  return;
                }
                final String outputText = extractCandidateText(candidate);
                if (outputText != null)
                {
                  _mainHandler.post(new Runnable()
                  {
                    @Override
                    public void run()
                    {
                      if (callback != null) callback.onSuccess(outputText);
                    }
                  });
                  return;
                }
              }
              throw new Exception("Unexpected response format from Gemini");
            }
            else if (responseCode == 404)
            {
              if (i == 0) primaryModelInvalid = true;
              lastError = "Model " + modelName + " (404 Not Found)";
              continue;
            }
            else if (responseCode == 429)
            {
              String errBody = readStreamSafely(conn.getErrorStream());
              long retryDelay = parseRetryDelayMs(errBody);
              pendingCooldownDelayMs = Math.max(pendingCooldownDelayMs, retryDelay);
              lastError = "Rate limit (429)";
              continue;
            }
            else
            {
              String errBody = readStreamSafely(conn.getErrorStream());

              if (responseCode == 400 && (errBody.contains("not found") || errBody.contains("not supported")))
              {
                if (i == 0) primaryModelInvalid = true;
                lastError = "Model " + modelName + " not supported";
                continue;
              }

              final String err = formatErrorMessage(responseCode, errBody);
              _mainHandler.post(new Runnable()
              {
                @Override
                public void run()
                {
                  if (callback != null) callback.onError(err);
                }
              });
              return;
            }
          }
          catch (Exception e)
          {
            lastError = e.getMessage();
            if (e instanceof java.net.UnknownHostException
                || (lastError != null && (lastError.contains("Unable to resolve host") || lastError.contains("No address associated"))))
            {
              lastError = "NO_INTERNET";
              break;
            }
          }
          finally
          {
            if (conn != null) conn.disconnect();
          }
        }

        if (pendingCooldownDelayMs > 0L && lastError != null && lastError.contains("429"))
        {
          quotaCooldownUntilMs = System.currentTimeMillis() + pendingCooldownDelayMs;
        }

        final String finalErrMsg;
        if ("NO_INTERNET".equals(lastError) || (lastError != null && (lastError.contains("Unable to resolve host") || lastError.contains("No address associated"))))
        {
          finalErrMsg = "No internet connection. Check your mobile data or Wi-Fi.";
        }
        else if (lastError != null && (lastError.contains("timed out") || lastError.contains("SocketTimeoutException")))
        {
          finalErrMsg = "The request timed out. Please try again.";
        }
        else if (lastError != null && lastError.contains("429"))
        {
          finalErrMsg = "Too many requests. Wait 15–20 seconds and try again.";
        }
        else if (lastError != null && (lastError.contains("API key not valid") || lastError.contains("API_KEY_INVALID")))
        {
          finalErrMsg = "Your API key is invalid. Update it in settings.";
        }
        else
        {
          finalErrMsg = "Request failed: " + (lastError != null ? lastError : "Request failed") + ". Please check settings.";
        }

        _mainHandler.post(new Runnable()
        {
          @Override
          public void run()
          {
            if (callback != null) callback.onError(finalErrMsg);
          }
        });
      }
    });
  }

  private static long parseRetryDelayMs(String responseBody)
  {
    if (responseBody != null && responseBody.contains("retry in "))
    {
      try
      {
        int idx = responseBody.indexOf("retry in ");
        String sub = responseBody.substring(idx + 9);
        int sIdx = sub.indexOf('s');
        if (sIdx > 0)
        {
          double secs = Double.parseDouble(sub.substring(0, sIdx).trim());
          return (long) (secs * 1000.0);
        }
      }
      catch (Exception ignored) {}
    }
    return 15000L;
  }

  private static String formatErrorMessage(int code, String raw)
  {
    if (code == 429)
    {
      return "⚠️ Google AI রিকোয়েস্ট লিমিট (429) সাময়িকভাবে পূর্ণ হয়েছে। দয়া করে ১৫-২০ সেকেন্ড পর চেষ্টা করুন।";
    }
    try
    {
      JSONObject obj = new JSONObject(raw);
      JSONObject err = obj.optJSONObject("error");
      if (err != null)
      {
        String msg = err.optString("message", "");
        if (!msg.isEmpty())
        {
          if (msg.contains("Resource has been exhausted") || msg.contains("quota"))
            return "The usage limit has been reached. Please try again later.";
          if (msg.contains("API key not valid"))
            return "Your API key is invalid. Update it in settings.";
          return "API Error (" + code + "): " + msg;
        }
      }
    }
    catch (Exception ignored) {}
    if (raw != null && raw.length() > 120)
      return "API Error (" + code + "): Request failed (" + raw.substring(0, 120) + "...)";
    return "API Error (" + code + "): " + raw;
  }
}
