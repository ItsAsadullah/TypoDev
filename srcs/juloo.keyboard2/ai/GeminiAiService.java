package juloo.keyboard2.ai;

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
    "gemini-2.0-flash-lite",
    "gemini-1.5-flash",
    "gemini-1.5-flash-8b",
    "gemini-1.5-pro"
  };

  private static final Object GENERATION_LOCK = new Object();
  private static boolean generationInFlight = false;
  private static long quotaCooldownUntilMs = 0L;

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

  public static class GrammarChange
  {
    public final String wrong;
    public final String fixed;
    public final String reason;

    public GrammarChange(String wrong, String fixed, String reason)
    {
      this.wrong = wrong;
      this.fixed = fixed;
      this.reason = reason;
    }
  }

  public static class GrammarDiffResult
  {
    public final String originalText;
    public final String fixedText;
    public final List<GrammarChange> changes;

    public GrammarDiffResult(String originalText, String fixedText, List<GrammarChange> changes)
    {
      this.originalText = originalText;
      this.fixedText = fixedText;
      this.changes = changes;
    }
  }

  public interface GrammarDiffCallback
  {
    void onSuccess(GrammarDiffResult result);
    void onError(String errorMessage);
  }

  private static final ExecutorService _executor = Executors.newSingleThreadExecutor();
  private static final Handler _mainHandler = new Handler(Looper.getMainLooper());

  public static String getApiKey(Context context)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String raw = prefs.getString(PREF_GEMINI_API_KEY, "").trim();
    if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2)
      raw = raw.substring(1, raw.length() - 1);
    if (raw.startsWith("key="))
      raw = raw.substring(4);
    if (raw.startsWith("Bearer "))
      raw = raw.substring(7);
    return raw.trim();
  }

  public static void setApiKey(Context context, String apiKey)
  {
    String cleaned = apiKey.trim();
    if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2)
      cleaned = cleaned.substring(1, cleaned.length() - 1);
    if (cleaned.startsWith("key="))
      cleaned = cleaned.substring(4);
    if (cleaned.startsWith("Bearer "))
      cleaned = cleaned.substring(7);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    prefs.edit().putString(PREF_GEMINI_API_KEY, cleaned.trim()).apply();
  }

  public static String getModel(Context context)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String model = prefs.getString(PREF_GEMINI_WORKING_MODEL, DEFAULT_MODEL);
    if (model == null || model.trim().isEmpty() || model.contains("pro-vision") || model.contains("gemini-3."))
    {
      model = DEFAULT_MODEL;
    }
    return model.trim();
  }

  public static void setModel(Context context, String model)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    prefs.edit().putString(PREF_GEMINI_WORKING_MODEL, model.trim()).apply();
  }

  public static boolean isEmojifyEnabled(Context context)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    return prefs.getBoolean(PREF_AI_EMOJIFY, true);
  }

  public static void setEmojifyEnabled(Context context, boolean enabled)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    prefs.edit().putBoolean(PREF_AI_EMOJIFY, enabled).apply();
  }

  public static String stripEmojis(String text)
  {
    if (text == null) return "";
    return text.replaceAll("[\\p{So}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+", "")
               .replaceAll("[\\u2600-\\u27BF\\uE000-\\uF8FF]", "")
               .replaceAll("\\s{2,}", " ")
               .trim();
  }

  public static void processText(final Context context, final String text, final Action action, final AiCallback callback)
  {
    if (text == null || text.trim().isEmpty())
    {
      callback.onError("No text to process");
      return;
    }
    boolean emojify = isEmojifyEnabled(context);
    StringBuilder sb = new StringBuilder();
    sb.append(action.getPrompt()).append("\n");
    if (emojify)
    {
      sb.append("EMOJI DIRECTIVE: Naturally enhance the message with fitting, expressive, and lively emojis.\n");
    }
    else
    {
      sb.append("STRICT EMOJI BAN: Do NOT include ANY emojis, emoticons, pictograms, or symbols under any circumstances. Output 100% plain text.\n");
    }
    sb.append("\nText:\n\"\"\"").append(text).append("\"\"\"");
    processPrompt(context, sb.toString(), new AiCallback()
    {
      @Override
      public void onSuccess(String resultText)
      {
        callback.onSuccess(emojify ? resultText : stripEmojis(resultText));
      }

      @Override
      public void onError(String errorMessage)
      {
        callback.onError(errorMessage);
      }
    });
  }

  public static void processStyle(
      final Context context,
      final String text,
      final String instruction,
      final boolean emojify,
      final AiCallback callback)
  {
    if (text == null || text.trim().isEmpty())
    {
      callback.onError("No text to process");
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("You are an expert AI writing assistant and text stylist.\n");
    sb.append("Rewrite instruction: ").append(instruction).append("\n");
    if (emojify)
    {
      sb.append("EMOJI DIRECTIVE: Add fitting, expressive, and natural emojis into the text.\n");
    }
    else
    {
      sb.append("STRICT EMOJI BAN: Do NOT include ANY emojis, emoticons, pictograms, or symbols under any circumstances. Output 100% pure plain text.\n");
    }
    sb.append("Rules: Preserve the original language and meaning. Output ONLY the rewritten text, without explanations, quotes, or preamble.\n\n");
    sb.append("Text:\n\"\"\"").append(text).append("\"\"\"");
    processPrompt(context, sb.toString(), new AiCallback()
    {
      @Override
      public void onSuccess(String resultText)
      {
        callback.onSuccess(emojify ? resultText : stripEmojis(resultText));
      }

      @Override
      public void onError(String errorMessage)
      {
        callback.onError(errorMessage);
      }
    });
  }

  public static void processFix(
      final Context context,
      final String text,
      final boolean emojify,
      final AiCallback callback)
  {
    if (text == null || text.trim().isEmpty())
    {
      callback.onError("No text to process");
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("Fix all spelling, punctuation, grammar, and typos in the following text. Preserve the original language and intended meaning.\n");
    if (emojify)
    {
      sb.append("EMOJI DIRECTIVE: Keep or enhance fitting expressive emojis.\n");
    }
    else
    {
      sb.append("STRICT EMOJI BAN: Do NOT include ANY emojis, emoticons, or symbols. Output pure text only.\n");
    }
    sb.append("Output ONLY the corrected text, with no explanations, notes, or preamble.\n\n");
    sb.append("Text:\n\"\"\"").append(text).append("\"\"\"");
    processPrompt(context, sb.toString(), new AiCallback()
    {
      @Override
      public void onSuccess(String resultText)
      {
        callback.onSuccess(emojify ? resultText : stripEmojis(resultText));
      }

      @Override
      public void onError(String errorMessage)
      {
        callback.onError(errorMessage);
      }
    });
  }

  public static void processGrammarDiff(
      final Context context,
      final String text,
      final boolean emojify,
      final GrammarDiffCallback callback)
  {
    if (text == null || text.trim().isEmpty())
    {
      callback.onError("No text to process");
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("You are an expert language and grammar proofreader.\n");
    sb.append("Task: Fix all grammatical mistakes, spelling errors, punctuation, and awkward phrasing in the given text.\n");
    if (emojify)
    {
      sb.append("EMOJI DIRECTIVE: Preserve or add fitting expressive emojis.\n");
    }
    else
    {
      sb.append("STRICT EMOJI BAN: Do NOT include ANY emojis or emoticons under any circumstances.\n");
    }
    sb.append("Output Format: Return ONLY a valid JSON object with no markdown code fences or preamble, matching this exact schema:\n");
    sb.append("{\n");
    sb.append("  \"fixed_text\": \"full corrected text\",\n");
    sb.append("  \"changes\": [\n");
    sb.append("    {\"wrong\": \"mistake in original text\", \"fixed\": \"corrected word or phrase\", \"reason\": \"brief explanation in Bengali/original language\"}\n");
    sb.append("  ]\n");
    sb.append("}\n\n");
    sb.append("Text:\n\"\"\"").append(text).append("\"\"\"");

    processPrompt(context, sb.toString(), new AiCallback()
    {
      @Override
      public void onSuccess(String resultText)
      {
        try
        {
          String clean = resultText.trim();
          if (clean.startsWith("```json")) clean = clean.substring(7);
          else if (clean.startsWith("```")) clean = clean.substring(3);
          if (clean.endsWith("```")) clean = clean.substring(0, clean.length() - 3);
          clean = clean.trim();

          JSONObject obj = new JSONObject(clean);
          String fixed = obj.optString("fixed_text", resultText);
          if (!emojify)
          {
            fixed = stripEmojis(fixed);
          }
          List<GrammarChange> changeList = new ArrayList<>();
          JSONArray arr = obj.optJSONArray("changes");
          if (arr != null)
          {
            for (int i = 0; i < arr.length(); i++)
            {
              JSONObject c = arr.getJSONObject(i);
              changeList.add(new GrammarChange(
                  c.optString("wrong", ""),
                  c.optString("fixed", ""),
                  c.optString("reason", "")
              ));
            }
          }
          callback.onSuccess(new GrammarDiffResult(text, fixed, changeList));
        }
        catch (Exception e)
        {
          callback.onSuccess(new GrammarDiffResult(text, emojify ? resultText : stripEmojis(resultText), new ArrayList<GrammarChange>()));
        }
      }

      @Override
      public void onError(String errorMessage)
      {
        callback.onError(errorMessage);
      }
    });
  }

  public static void processTranslate(
      final Context context,
      final String text,
      final String targetLanguage,
      final boolean emojify,
      final AiCallback callback)
  {
    if (text == null || text.trim().isEmpty())
    {
      callback.onError("No text to process");
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("Translate the following text accurately into ").append(targetLanguage).append(".\n");
    if (emojify)
    {
      sb.append("EMOJI DIRECTIVE: Naturally enhance the translation with fitting, expressive emojis.\n");
    }
    else
    {
      sb.append("STRICT EMOJI BAN: Do NOT add ANY emojis, pictograms, or emoticons. Output pure plain text only.\n");
    }
    sb.append("Output ONLY the translated text, without pronunciation guides, explanations, or preamble.\n\n");
    sb.append("Text:\n\"\"\"").append(text).append("\"\"\"");
    processPrompt(context, sb.toString(), new AiCallback()
    {
      @Override
      public void onSuccess(String resultText)
      {
        callback.onSuccess(emojify ? resultText : stripEmojis(resultText));
      }

      @Override
      public void onError(String errorMessage)
      {
        callback.onError(errorMessage);
      }
    });
  }

  public static void fetchAndCacheModels(final Context context, final String apiKey, final boolean forceRefresh)
  {
    if (apiKey == null || apiKey.trim().isEmpty()) return;
    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
          long lastFetch = prefs.getLong(PREF_GEMINI_MODELS_LAST_FETCH, 0L);
          long now = System.currentTimeMillis();
          if (!forceRefresh && (now - lastFetch < 7L * 24 * 3600 * 1000) && prefs.contains(PREF_CACHED_GEMINI_MODELS))
          {
            return;
          }

          String urlStr = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey.trim();
          HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
          conn.setRequestMethod("GET");
          conn.setConnectTimeout(8000);
          conn.setReadTimeout(12000);

          if (conn.getResponseCode() == 200)
          {
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);

            JSONObject json = new JSONObject(sb.toString());
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

              // Prioritize Lite / 1.5 models for lowest latency and highest free-tier quota
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

  public static void processPrompt(final Context context, final String promptText, final AiCallback callback)
  {
    final String apiKey = getApiKey(context);
    if (apiKey.isEmpty())
    {
      callback.onError("Gemini API key is missing. Please configure it in settings.");
      return;
    }

    synchronized (GENERATION_LOCK)
    {
      if (generationInFlight)
      {
        callback.onError("AI is currently generating. Please wait a moment.");
        return;
      }
      generationInFlight = true;
    }

    final long remainingCooldown = quotaCooldownUntilMs - System.currentTimeMillis();
    if (remainingCooldown > 0)
    {
      synchronized (GENERATION_LOCK) { generationInFlight = false; }
      long seconds = (remainingCooldown + 999L) / 1000L;
      callback.onError("⚠️ Google AI কোটা সাময়িকভাবে শেষ। দয়া করে " + seconds + " সেকেন্ড অপেক্ষা করুন।");
      return;
    }

    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
          fetchAndCacheModels(context, apiKey, false);

          String cachedModelsStr = prefs.getString(PREF_CACHED_GEMINI_MODELS, "");
          List<String> modelsToTry = new ArrayList<>();

          // 1. User selected model if set
          String selectedModel = getModel(context);
          if (selectedModel != null && !selectedModel.isEmpty() && isSupportedTextFlashModel(selectedModel))
          {
            modelsToTry.add(selectedModel);
          }

          // 2. Models discovered from Google API
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

          // 3. Fallback to tested official models (1.5-flash first for high quota!)
          for (String m : CANDIDATE_MODELS)
          {
            if (!modelsToTry.contains(m))
            {
              modelsToTry.add(m);
            }
          }

          String lastError = "";

          for (int i = 0; i < modelsToTry.size(); i++)
          {
            String modelName = modelsToTry.get(i);
            try
            {
              JSONObject partObj = new JSONObject();
              partObj.put("text", promptText);

              JSONArray partsArray = new JSONArray();
              partsArray.put(partObj);

              JSONObject contentObj = new JSONObject();
              contentObj.put("parts", partsArray);

              JSONArray contentsArray = new JSONArray();
              contentsArray.put(contentObj);

              JSONObject requestBody = new JSONObject();
              requestBody.put("contents", contentsArray);

              // GenerationConfig to speed up response and save tokens (FrostKeys style)
              JSONObject genConfig = new JSONObject();
              genConfig.put("temperature", 0.3);
              genConfig.put("maxOutputTokens", 1024);
              requestBody.put("generationConfig", genConfig);

              String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;
              URL url = new URL(urlStr);
              HttpURLConnection conn = (HttpURLConnection) url.openConnection();
              conn.setRequestMethod("POST");
              conn.setRequestProperty("Content-Type", "application/json; utf-8");
              conn.setRequestProperty("Accept", "application/json");
              conn.setDoOutput(true);
              conn.setConnectTimeout(10000);
              conn.setReadTimeout(15000);

              byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
              try (OutputStream os = conn.getOutputStream())
              {
                os.write(input, 0, input.length);
              }

              int responseCode = conn.getResponseCode();

              if (responseCode >= 200 && responseCode < 300)
              {
                // Success! Save working model
                prefs.edit().putString(PREF_GEMINI_WORKING_MODEL, modelName).apply();

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null)
                {
                  sb.append(responseLine.trim());
                }

                JSONObject jsonResp = new JSONObject(sb.toString());
                JSONArray candidates = jsonResp.optJSONArray("candidates");
                if (candidates != null && candidates.length() > 0)
                {
                  JSONObject candidate = candidates.getJSONObject(0);
                  JSONObject content = candidate.optJSONObject("content");
                  if (content != null)
                  {
                    JSONArray parts = content.optJSONArray("parts");
                    if (parts != null && parts.length() > 0)
                    {
                      final String outputText = parts.getJSONObject(0).optString("text", "").trim();
                      _mainHandler.post(new Runnable()
                      {
                        @Override
                        public void run()
                        {
                          callback.onSuccess(outputText);
                        }
                      });
                      return; // Done successfully!
                    }
                  }
                }
                throw new Exception("Unexpected response format from Gemini");
              }
              else if (responseCode == 404)
              {
                lastError = "Model " + modelName + " (404 Not Found)";
                continue;
              }
              else if (responseCode == 429)
              {
                // Parse retry delay or set fallback cooldown
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) sb.append(responseLine.trim());
                String errBody = sb.toString();

                long retryDelay = parseRetryDelayMs(errBody);
                quotaCooldownUntilMs = System.currentTimeMillis() + retryDelay;
                lastError = "Rate limit (429)";
                // Try next model if available (e.g. 1.5-flash or 2.0-flash-lite)
                continue;
              }
              else
              {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) sb.append(responseLine.trim());
                String errBody = sb.toString();

                if (responseCode == 400 && (errBody.contains("not found") || errBody.contains("not supported")))
                {
                  lastError = "Model " + modelName + " not supported";
                  continue;
                }

                final String err = formatErrorMessage(responseCode, errBody);
                _mainHandler.post(new Runnable()
                {
                  @Override
                  public void run()
                  {
                    callback.onError(err);
                  }
                });
                return;
              }
            }
            catch (Exception e)
            {
              lastError = e.getMessage();
            }
          }

          final String finalErrMsg;
          if (lastError != null && lastError.contains("429"))
          {
            finalErrMsg = "⚠️ Google AI রিকোয়েস্ট লিমিট (429)। দয়া করে ১৫-২০ সেকেন্ড অপেক্ষা করে আবার চেষ্টা করুন।";
          }
          else
          {
            finalErrMsg = "Gemini Error: " + lastError + ". Please verify your API key in settings.";
          }

          _mainHandler.post(new Runnable()
          {
            @Override
            public void run()
            {
              callback.onError(finalErrMsg);
            }
          });
        }
        finally
        {
          synchronized (GENERATION_LOCK)
          {
            generationInFlight = false;
          }
        }
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
    return 15000L; // default 15s cooldown
  }

  private static String formatErrorMessage(int code, String raw)
  {
    if (code == 429)
    {
      return "⚠️ Google AI রিকোয়েস্ট লিমিট (429) সাময়িকভাবে পূর্ণ হয়েছে। দয়া করে ১৫-২০ সেকেন্ড অপেক্ষা করুন।";
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
            return "⚠️ Google AI কোটা লিমিট (429) সাময়িকভাবে শেষ হয়েছে। দয়া করে কিছুক্ষণ অপেক্ষা করুন।";
          if (msg.contains("API key not valid"))
            return "❌ Gemini API Key সঠিক নয়। দয়া করে সেটিংসে সঠিক কি দিন।";
          return "API Error (" + code + "): " + msg;
        }
      }
    }
    catch (Exception ignored) {}
    if (raw.length() > 120)
      return "API Error (" + code + "): Request failed (" + raw.substring(0, 120) + "...)";
    return "API Error (" + code + "): " + raw;
  }
}
