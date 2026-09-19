package juloo.keyboard2.suggestions;

/**
 * Represents a candidate word with comprehensive metadata for ranking and autocorrection.
 */
public class Candidate
{
  public enum Source
  {
    VERBATIM(100),
    CODE_SNIPPET(96),
    SNIPPET(95),
    AUTOCORRECT(92),
    PERSONAL(90),
    GRAMMAR(85),
    CORE_LEXICON(80),
    BUILTIN_MAIN(80),
    CDICT(75),
    BANGLISH(72),
    EXTERNAL(70),
    NEXT_WORD(60),
    TYPO_CORRECTION(50);

    public final int baseScore;
    Source(int base)
    {
      this.baseScore = base;
    }
  }

  public final String word;
  public Source source;
  public int frequency;          // 0 to 255
  public int editDistance;       // 0 for prefix/exact, 1 or 2 for typo
  public float prefixMatchRatio; // prefix.length / word.length
  public float contextScore;     // Next-word / collocation bonus
  public float recencyScore;     // User recent usage bonus
  public float rejectionPenalty; // Negative score if user recently rejected
  public double compositeScore;  // Final calculated score

  public Candidate(String word, Source source, int frequency)
  {
    this(word, source, frequency, 0, 1.0f);
  }

  public Candidate(String word, Source source, int frequency, int editDistance, float prefixMatchRatio)
  {
    this.word = word;
    this.source = source;
    this.frequency = Math.max(0, Math.min(255, frequency));
    this.editDistance = editDistance;
    this.prefixMatchRatio = prefixMatchRatio;
    this.contextScore = 0f;
    this.recencyScore = 0f;
    this.rejectionPenalty = 0f;
    this.compositeScore = 0.0;
  }

  public Candidate(String word, Source source, int frequency, int editDistance, float prefixMatchRatio, float contextScore)
  {
    this(word, source, frequency, editDistance, prefixMatchRatio);
    this.contextScore = contextScore;
  }

  @Override
  public String toString()
  {
    return word + " (" + source + ", score=" + String.format(java.util.Locale.ROOT, "%.2f", compositeScore) + ")";
  }
}
