package typodev.keyboard.voice;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import typodev.keyboard.Keyboard2;
import typodev.keyboard.ai.AiProvider;

/**
 * Intelligent punctuation and formatting helper for voice typing:
 * Tier 1: Real-time voice command detection (দাঁড়ি, কমা, etc.) & instant offline sentence punctuation.
 * Tier 2: AI cloud auto-punctuation & grammar polishing via active AI Provider (Gemini / OpenAI).
 */
public class VoicePunctuationHelper
{
  public static final String PREF_VOICE_AI_PUNCTUATION = "pref_voice_ai_punctuation";
  public static final boolean DEFAULT_VOICE_AI_PUNCTUATION = true;

  public static final String PREF_VOICE_SPOKEN_PUNCTUATION = "pref_voice_spoken_punctuation";
  public static final boolean DEFAULT_VOICE_SPOKEN_PUNCTUATION = true;

  private static final Pattern PATTERN_EXTRA_SPACES = Pattern.compile("[ \\t]+");
  private static final Pattern PATTERN_SPACE_BEFORE_PUNCT = Pattern.compile("\\s+([।.,!?:;])");
  private static final Pattern PATTERN_SPACE_AFTER_PUNCT = Pattern.compile("([।.,!?:;])([\\p{L}\\p{N}])");

  public static boolean isVoiceSpokenPunctuationEnabled(Context context)
  {
    if (context == null) return DEFAULT_VOICE_SPOKEN_PUNCTUATION;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    return prefs.getBoolean(PREF_VOICE_SPOKEN_PUNCTUATION, DEFAULT_VOICE_SPOKEN_PUNCTUATION);
  }

  public static void setVoiceSpokenPunctuationEnabled(Context context, boolean enabled)
  {
    if (context == null) return;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    prefs.edit().putBoolean(PREF_VOICE_SPOKEN_PUNCTUATION, enabled).apply();
  }

  public static boolean isVoiceAiPunctuationEnabled(Context context)
  {
    if (context == null) return DEFAULT_VOICE_AI_PUNCTUATION;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    return prefs.getBoolean(PREF_VOICE_AI_PUNCTUATION, DEFAULT_VOICE_AI_PUNCTUATION);
  }

  public static void setVoiceAiPunctuationEnabled(Context context, boolean enabled)
  {
    if (context == null) return;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    prefs.edit().putBoolean(PREF_VOICE_AI_PUNCTUATION, enabled).apply();
  }

  /**
   * Replaces spoken punctuation commands with actual punctuation symbols if enabled.
   */
  public static String applyVoiceCommands(Context context, String input, String langCode)
  {
    if (context != null && !isVoiceSpokenPunctuationEnabled(context))
    {
      return input;
    }
    return applyVoiceCommands(input, langCode);
  }

  /**
   * Replaces spoken punctuation commands with actual punctuation symbols.
   * e.g. "আমি ভালো আছি দাঁড়ি" -> "আমি ভালো আছি।"
   */
  public static String applyVoiceCommands(String input, String langCode)
  {
    if (input == null || input.trim().isEmpty()) return input;

    String text = input;

    // Bengali voice commands
    text = text.replaceAll("(?<=^|\\s)(দাঁড়ি|দাড়ি|পূর্ণচ্ছেদ)(?=\\s|$|[.,?!।])", "।");
    text = text.replaceAll("(?<=^|\\s)(কমা)(?=\\s|$|[.,?!।])", ",");
    text = text.replaceAll("(?<=^|\\s)(প্রশ্নবোধক চিহ্ন|প্রশ্নবোধক|প্রশ্নচিহ্ন|জিজ্ঞাসা চিহ্ন)(?=\\s|$|[.,?!।])", "?");
    text = text.replaceAll("(?<=^|\\s)(বিস্ময়সূচক চিহ্ন|বিস্ময়সূচক চিহ্ন|বিস্ময়সূচক|বিস্ময়সূচক)(?=\\s|$|[.,?!।])", "!");
    text = text.replaceAll("(?<=^|\\s)(নতুন লাইন|পরের লাইন|নিউ লাইন)(?=\\s|$|[.,?!।])", "\n");

    // English voice commands
    text = text.replaceAll("(?i)\\b(full stop|period)\\b", ".");
    text = text.replaceAll("(?i)\\b(comma)\\b", ",");
    text = text.replaceAll("(?i)\\b(question mark)\\b", "?");
    text = text.replaceAll("(?i)\\b(exclamation mark|exclamation point)\\b", "!");
    text = text.replaceAll("(?i)\\b(new line|next line)\\b", "\n");

    // Fix any stray spaces before punctuation
    text = PATTERN_SPACE_BEFORE_PUNCT.matcher(text).replaceAll("$1");
    return text;
  }

  /**
   * Tier 1 Instant Rule-Based Formatter:
   * Normalizes spaces, handles punctuation boundaries, and terminates sentence if missing.
   */
  public static String applyInstantFormatting(String text, String langCode)
  {
    if (text == null || text.trim().isEmpty()) return text;

    String formatted = PATTERN_EXTRA_SPACES.matcher(text.trim()).replaceAll(" ");
    formatted = PATTERN_SPACE_BEFORE_PUNCT.matcher(formatted).replaceAll("$1");
    formatted = PATTERN_SPACE_AFTER_PUNCT.matcher(formatted).replaceAll("$1 $2");

    if (formatted.length() > 0)
    {
      char lastChar = formatted.charAt(formatted.length() - 1);
      boolean hasTerminalPunct = (lastChar == '।' || lastChar == '.' || lastChar == '?' ||
                                  lastChar == '!' || lastChar == '\n');
      if (!hasTerminalPunct)
      {
        boolean isBn = isBengali(formatted, langCode);
        formatted = formatted + (isBn ? "।" : ".");
      }
    }

    return formatted;
  }

  private static boolean isBengali(String text, String langCode)
  {
    if (langCode != null && langCode.toLowerCase().startsWith("bn"))
    {
      return true;
    }
    // Check if contains Bengali unicode characters
    for (int i = 0; i < text.length(); i++)
    {
      char c = text.charAt(i);
      if (c >= '\u0980' && c <= '\u09FF')
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Tier 2 AI cloud auto-punctuation & polishing.
   * Runs asynchronously in the background. On success, updates the text in the active editor.
   */
  public static void processAiPunctuation(final Context context, final Keyboard2 keyboard,
                                         final String rawText, final String langCode)
  {
    if (context == null || keyboard == null) return;
    if (!isVoiceAiPunctuationEnabled(context)) return;
    if (rawText == null || rawText.trim().length() < 3) return;

    if (!AiProvider.Manager.hasConfiguredApiKey(context))
    {
      // No AI API key configured; Tier 1 instant formatting is already applied.
      return;
    }

    final String trimmedRaw = rawText.trim();
    final long sessionId = keyboard.getInputSessionId();
    final android.view.inputmethod.InputConnection connection = keyboard.getCurrentInputConnection();

    final String systemPrompt =
        "You are a strict voice-typing punctuation assistant for a smartphone keyboard.\n"
        + "Task: ONLY add appropriate punctuation marks (Bengali dari '।', comma ',', question mark '?', exclamation '!') "
        + "and fix spacing or capitalization in the given transcribed voice text.\n"
        + "CRITICAL RULES:\n"
        + "1. NEVER translate any words between languages under any circumstances. If the text has English words (e.g. 'good', 'sorry', 'thanks') "
        + "or Bengali transliterations of English words (e.g. 'গুড', 'থ্যাঙ্কস', 'ওকে'), KEEP THEM EXACTLY AS TRANSCRIBED. "
        + "Never replace 'গুড' with 'ভাল' or 'ভালো', and never replace 'good' with 'ভালো'.\n"
        + "2. Preserve ALL words verbatim. Do NOT change vocabulary, paraphrase, replace synonyms, or substitute words.\n"
        + "3. Do NOT answer questions, chat, or add explanations.\n"
        + "4. Output ONLY the formatted text with NO explanations, quotes, or markdown.\n";

    AiProvider provider = AiProvider.Manager.getActiveProvider(context);
    if (provider == null) return;

    provider.generate(context, systemPrompt, trimmedRaw, new AiProvider.Callback()
    {
      @Override
      public void onSuccess(final String resultText)
      {
        if (resultText == null || resultText.trim().isEmpty()) return;

        String clean = resultText.trim();
        // Remove markdown code fences if present
        if (clean.startsWith("```") && clean.endsWith("```"))
        {
          clean = clean.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("\\n?```$", "").trim();
        }
        // Remove enclosing quotes if present
        if (clean.startsWith("\"") && clean.endsWith("\"") && clean.length() > 2)
        {
          clean = clean.substring(1, clean.length() - 1).trim();
        }

        final String finalClean = clean;

        // Safety check: ensure AI did NOT translate, substitute, or delete words.
        // Strip all punctuation and whitespace, then compare remaining characters.
        String strippedRaw = trimmedRaw.replaceAll("[\\p{Punct}।\\s]+", "");
        String strippedClean = finalClean.replaceAll("[\\p{Punct}।\\s]+", "");
        if (!strippedRaw.equalsIgnoreCase(strippedClean))
        {
          // AI attempted to translate, replace or alter spoken words (e.g. 'গুড' -> 'ভাল').
          // Discard alteration to preserve user's exact spoken words verbatim!
          return;
        }

        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(new Runnable()
        {
          @Override
          public void run()
          {
            if (sessionId != keyboard.getInputSessionId()
                || connection != keyboard.getCurrentInputConnection()) return;
            boolean replaced = keyboard.replaceVoiceSessionText(trimmedRaw, finalClean);
            if (replaced)
            {
              try
              {
                Toast.makeText(context, "✨ AI পাংচুয়েশন সম্পন্ন", Toast.LENGTH_SHORT).show();
              }
              catch (Throwable ignored) {}
            }
          }
        });
      }

      @Override
      public void onError(final String errorMessage)
      {
        // Graceful non-blocking failure: Instant Tier 1 punctuation is already present.
      }
    });
  }
}
