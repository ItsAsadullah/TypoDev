package juloo.keyboard2.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public interface AiProvider
{
  enum ProviderType
  {
    GEMINI("Google Gemini"),
    OPENAI_COMPATIBLE("OpenAI / DeepSeek / Groq");

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
    public static ProviderType getActiveProviderType(Context context)
    {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      String val = prefs.getString(PREF_AI_PROVIDER, "gemini");
      if ("openai".equalsIgnoreCase(val) || "openai_compatible".equalsIgnoreCase(val))
      {
        return ProviderType.OPENAI_COMPATIBLE;
      }
      return ProviderType.GEMINI;
    }

    public static void setActiveProviderType(Context context, ProviderType type)
    {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      prefs.edit()
          .putString(PREF_AI_PROVIDER, type == ProviderType.OPENAI_COMPATIBLE ? "openai" : "gemini")
          .apply();
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
      ProviderType type = getActiveProviderType(context);
      if (type == ProviderType.OPENAI_COMPATIBLE)
      {
        return !OpenAiCompatibleProvider.getApiKey(context).isEmpty();
      }
      return !GeminiAiService.getApiKey(context).isEmpty();
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
