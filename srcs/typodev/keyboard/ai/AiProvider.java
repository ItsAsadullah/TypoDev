package typodev.keyboard.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public interface AiProvider
{
  enum ProviderType
  {
    GEMINI("Google Gemini"),
    OPENAI_COMPATIBLE("Groq / Grok / OpenAI / DeepSeek");

    private final String displayName;

    ProviderType(String displayName)
    {
      this.displayName = displayName;
    }

    public String getDisplayName()
    {
      return displayName;
    }
  }

  interface Callback
  {
    void onSuccess(String resultText);
    void onError(String errorMessage);
  }

  String getName();
  String getActiveModel(Context context);
  void generate(Context context, String systemPrompt, String userText, Callback callback);

  String PREF_AI_PROVIDER = "pref_ai_provider";
  String PREF_OPENAI_API_KEY = "openai_api_key";
  String PREF_OPENAI_BASE_URL = "openai_base_url";
  String PREF_OPENAI_MODEL = "openai_model";
  String PREF_AI_TEMPERATURE = "pref_ai_temperature";

  String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1";
  String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";

  class Manager
  {
    public static boolean isOpenAiCompatibleKey(String key)
    {
      if (key == null) return false;
      String k = key.trim();
      return k.startsWith("gsk_") || k.startsWith("xai-") || k.startsWith("sk-");
    }

    public static boolean isGeminiKey(String key)
    {
      if (key == null) return false;
      return key.trim().startsWith("AIza");
    }

    public static ProviderType getActiveProviderType(Context context)
    {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      String val = prefs.getString(PREF_AI_PROVIDER, "");
      if (val == null || val.isEmpty())
      {
        try
        {
          SharedPreferences dp = typodev.keyboard.DirectBootAwarePreferences.get_shared_preferences(context);
          val = dp.getString(PREF_AI_PROVIDER, "gemini");
        }
        catch (Throwable ignored)
        {
          val = "gemini";
        }
      }

      String openAiKey = OpenAiCompatibleProvider.getApiKey(context);
      String geminiKey = GeminiAiService.getGeminiApiKey(context);

      if ("openai".equalsIgnoreCase(val) || "openai_compatible".equalsIgnoreCase(val))
      {
        if (!openAiKey.isEmpty() || geminiKey.isEmpty())
        {
          return ProviderType.OPENAI_COMPATIBLE;
        }
      }

      // Auto-fallback: if Gemini key is empty (or actually a Groq/Grok/OpenAI key) and OpenAI-compatible key exists
      if (geminiKey.isEmpty() && !openAiKey.isEmpty())
      {
        return ProviderType.OPENAI_COMPATIBLE;
      }

      return ProviderType.GEMINI;
    }

    public static void setActiveProviderType(Context context, ProviderType type)
    {
      String val = (type == ProviderType.OPENAI_COMPATIBLE) ? "openai" : "gemini";
      PreferenceManager.getDefaultSharedPreferences(context)
          .edit()
          .putString(PREF_AI_PROVIDER, val)
          .apply();
      try
      {
        typodev.keyboard.DirectBootAwarePreferences.get_shared_preferences(context)
            .edit()
            .putString(PREF_AI_PROVIDER, val)
            .apply();
      }
      catch (Throwable ignored) {}
    }

    public static AiProvider getActiveProvider(Context context)
    {
      ProviderType type = getActiveProviderType(context);
      if (type == ProviderType.OPENAI_COMPATIBLE)
      {
        return OpenAiCompatibleProvider.instance();
      }
      return GeminiAiProvider.instance();
    }

    public static boolean hasConfiguredApiKey(Context context)
    {
      return !OpenAiCompatibleProvider.getApiKey(context).isEmpty()
          || !GeminiAiService.getGeminiApiKey(context).isEmpty();
    }

    public static float getTemperature(Context context)
    {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      try
      {
        return Float.parseFloat(prefs.getString(PREF_AI_TEMPERATURE, "0.7"));
      }
      catch (Exception ignored)
      {
        return 0.7f;
      }
    }

    public static void setTemperature(Context context, float temp)
    {
      PreferenceManager.getDefaultSharedPreferences(context)
          .edit()
          .putString(PREF_AI_TEMPERATURE, String.valueOf(temp))
          .apply();
    }
  }
}
