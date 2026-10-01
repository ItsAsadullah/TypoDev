package typodev.keyboard.suggestions;

/**
 * Evaluates whether a top candidate qualifies for automatic spacebar replacement.
 * Prevents unwanted replacements of valid words, proper nouns, abbreviations,
 * and previously rejected suggestions.
 */
public class AutocorrectDecision
{
  public enum Sensitivity
  {
    OFF(Double.MAX_VALUE),
    CONSERVATIVE(45.0),
    BALANCED(30.0),
    AGGRESSIVE(15.0);

    public final double requiredConfidenceMargin;
    Sensitivity(double margin)
    {
      this.requiredConfidenceMargin = margin;
    }
  }

  /**
   * Determines whether [top] should automatically replace [typedWord] when spacebar is tapped.
   */
  public static boolean shouldAutocorrect(
      String typedWord,
      Candidate top,
      Candidate second,
      boolean isTypedWordValid,
      boolean isProperNounOrProtected,
      Sensitivity sensitivity)
  {
    if (sensitivity == Sensitivity.OFF || top == null || typedWord == null)
    {
      return false;
    }

    String cleanTyped = typedWord.trim();
    if (cleanTyped.length() <= 1 && top.source != Candidate.Source.AUTOCORRECT)
    {
      return false; // Single characters are never autocorrected unless explicitly marked as AUTOCORRECT
    }

    boolean isAvroTransliteration = BanglishEngine.isBengaliScript(top.word) && BanglishEngine.isLatinOnly(cleanTyped);

    // Bengali script typing (e.g. Jatiyo layout):
    // Valid Bengali words and longer prefix completions must NEVER be autocorrected on spacebar!
    if (BanglishEngine.isBengaliScript(cleanTyped))
    {
      if (isTypedWordValid)
      {
        return false;
      }
      if (top.word.length() > cleanTyped.length())
      {
        return false;
      }
    }

    // Explicit autocorrect sources (e.g. contractions like "im" -> "I'm", "dont" -> "don't", or Avro phonetic)
    if (top.source == Candidate.Source.AUTOCORRECT)
    {
      // In Bengali / Avro phonetic mode, Latin words must transliterate to Bengali
      // even if the Latin word happens to match an English word (e.g. "sonar" -> "সোনার", "rat" -> "রাত")
      if (isAvroTransliteration)
      {
        return top.rejectionPenalty <= 0.2f;
      }
      String lower = cleanTyped.toLowerCase(java.util.Locale.ROOT);
      // Words that are common legitimate standalone English words should NOT be forced to contractions on space
      if (lower.equals("ill") || lower.equals("well") || lower.equals("wed") || lower.equals("were") || lower.equals("cant"))
      {
        return false;
      }
      return top.rejectionPenalty <= 0.2f;
    }

    // 1. If what the user typed is already a valid word in any dictionary, NEVER autocorrect it!
    if (isTypedWordValid)
    {
      return false;
    }

    // 2. Prevent automatic prefix completion on space (e.g. typing "th" should not force "the" on space)
    // Word completions should be chosen by tapping the chip, not by pressing space!
    if (top.editDistance == 0 && top.word.length() > cleanTyped.length())
    {
      return false;
    }

    // 3. Protect proper nouns, camelCase, technical acronyms, or numbers
    if (isProperNounOrProtected || isLikelyProperNounOrTechnical(cleanTyped))
    {
      return false;
    }

    // 4. Do not autocorrect if the candidate was previously rejected
    if (top.rejectionPenalty > 0.2f)
    {
      return false;
    }

    // 5. Banglish transliteration should be tap-to-select, not forced on spacebar
    if (top.source == Candidate.Source.BANGLISH)
    {
      return false;
    }

    // 6. Typo correction (edit distance 1 or 2):
    if (top.editDistance > 0)
    {
      // Short words (length 2 or 3) require higher confidence to avoid false corrections
      double minScore = cleanTyped.length() <= 3 ? 90.0 : 70.0;
      if (top.compositeScore < minScore)
      {
        return false;
      }

      // Check confidence margin over the second candidate if one exists
      if (second != null)
      {
        double margin = top.compositeScore - second.compositeScore;
        if (margin < sensitivity.requiredConfidenceMargin)
        {
          return false;
        }
      }
      return true;
    }

    return false;
  }

  public static boolean isLikelyProperNounOrTechnical(String word)
  {
    if (word == null || word.isEmpty()) return false;
    // Starts with upper case letter (e.g., Asadullah, Dhaka, London)
    if (Character.isUpperCase(word.charAt(0))) return true;

    // Contains digits or technical symbols
    for (int i = 0; i < word.length(); i++)
    {
      char c = word.charAt(i);
      if (Character.isDigit(c) || c == '_' || c == '-' || c == '@' || c == '/' || c == '.')
      {
        return true;
      }
    }
    return false;
  }
}
