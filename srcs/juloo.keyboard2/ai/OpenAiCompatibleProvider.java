package juloo.keyboard2.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class OpenAiCompatibleProvider implements AiProvider
{
  private static OpenAiCompatibleProvider _instance;
  private static final ExecutorService _executor = Executors.newSingleThreadExecutor();
  private static final Handler _mainHandler = new Handler(Looper.getMainLooper());

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
    return "OpenAI-Compatible";
  }

  @Override
  public String getActiveModel(Context context)
  {
    return getModel(context);
  }

  public static String getApiKey(Context context)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String raw = prefs.getString(PREF_OPENAI_API_KEY, "").trim();
    if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2)
      raw = raw.substring(1, raw.length() - 1);
    if (raw.startsWith("Bearer "))
      raw = raw.substring(7);
    return raw.trim();
  }

  public static void setApiKey(Context context, String key)
  {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(PREF_OPENAI_API_KEY, key != null ? key.trim() : "")
        .apply();
  }

  public static String getBaseUrl(Context context)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String url = prefs.getString(PREF_OPENAI_BASE_URL, DEFAULT_OPENAI_BASE_URL).trim();
    if (url.isEmpty()) url = DEFAULT_OPENAI_BASE_URL;
    if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
    return url;
  }

  public static void setBaseUrl(Context context, String url)
  {
    String clean = (url != null) ? url.trim() : DEFAULT_OPENAI_BASE_URL;
    if (clean.endsWith("/")) clean = clean.substring(0, clean.length() - 1);
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(PREF_OPENAI_BASE_URL, clean)
        .apply();
  }

  public static String getModel(Context context)
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String model = prefs.getString(PREF_OPENAI_MODEL, DEFAULT_OPENAI_MODEL).trim();
    return model.isEmpty() ? DEFAULT_OPENAI_MODEL : model;
  }

  public static void setModel(Context context, String model)
  {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putString(PREF_OPENAI_MODEL, model != null ? model.trim() : DEFAULT_OPENAI_MODEL)
        .apply();
  }

  @Override
  public void generate(final Context context, final String systemPrompt, final String userText, final Callback callback)
  {
    final String apiKey = getApiKey(context);
    if (apiKey.isEmpty())
    {
      if (callback != null) callback.onError("OpenAI API key not set. Please set it in AI Settings.");
      return;
    }

    final String baseUrl = getBaseUrl(context);
    final String model = getModel(context);
    final float temp = Manager.getTemperature(context);

    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        HttpURLConnection conn = null;
        try
        {
          String endpoint = baseUrl + "/chat/completions";
          URL url = new URL(endpoint);
          conn = (HttpURLConnection)url.openConnection();
          conn.setRequestMethod("POST");
          conn.setConnectTimeout(25000);
          conn.setReadTimeout(35000);
          conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
          conn.setRequestProperty("Authorization", "Bearer " + apiKey);
          conn.setDoOutput(true);

          JSONObject root = new JSONObject();
          root.put("model", model);
          root.put("temperature", (double)temp);

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
          userMsg.put("content", userText != null ? userText : "");
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
                String content = message.optString("content", "");
                postSuccess(callback, content.trim());
                return;
              }
            }
            postError(callback, "No text generated by model.");
          }
          else
          {
            String errMsg = "HTTP " + statusCode;
            try
            {
              JSONObject errJson = new JSONObject(respStr);
              if (errJson.has("error"))
              {
                JSONObject errObj = errJson.getJSONObject("error");
                errMsg = errObj.optString("message", errMsg);
              }
            }
            catch (Exception ignored) {}
            postError(callback, errMsg);
          }
        }
        catch (Exception e)
        {
          postError(callback, "Connection failed: " + e.getMessage());
        }
        finally
        {
          if (conn != null) conn.disconnect();
        }
      }
    });
  }

  private static void postSuccess(final Callback cb, final String result)
  {
    if (cb == null) return;
    _mainHandler.post(new Runnable()
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
    _mainHandler.post(new Runnable()
    {
      @Override
      public void run()
      {
        cb.onError(error);
      }
    });
  }
}
