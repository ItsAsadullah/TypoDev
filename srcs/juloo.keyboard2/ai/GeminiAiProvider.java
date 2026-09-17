package juloo.keyboard2.ai;

import android.content.Context;

public class GeminiAiProvider implements AiProvider
{
  private static GeminiAiProvider _instance;

  public static synchronized GeminiAiProvider instance()
  {
    if (_instance == null)
    {
      _instance = new GeminiAiProvider();
    }
    return _instance;
  }

  @Override
  public String getName()
  {
    return "Google Gemini";
  }

  @Override
  public String getActiveModel(Context context)
  {
    return GeminiAiService.getModel(context);
  }

  @Override
  public void generate(Context context, String systemPrompt, String userText, final Callback callback)
  {
    StringBuilder fullPrompt = new StringBuilder();
    if (systemPrompt != null && !systemPrompt.trim().isEmpty())
    {
      fullPrompt.append(systemPrompt.trim()).append("\n\n");
    }
    fullPrompt.append("Input:\n\"\"\"").append(userText != null ? userText : "").append("\"\"\"");

    GeminiAiService.processPrompt(context, fullPrompt.toString(), new GeminiAiService.AiCallback()
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
  }
}
