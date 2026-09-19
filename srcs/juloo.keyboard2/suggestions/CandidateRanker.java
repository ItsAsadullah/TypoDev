package juloo.keyboard2.suggestions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * High-performance candidate scorer and ranker.
 * Merges multi-source candidates (Personal, Core, Cdict, External, Banglish, Typo)
 * and ranks them by a balanced composite scoring model.
 */
public class CandidateRanker
{
  /**
   * Weights for scoring components.
   */
  private static final double WEIGHT_FREQ = 18.0;
  private static final double WEIGHT_PREFIX = 40.0;
  private static final double WEIGHT_DIST_PENALTY = 55.0;
  private static final double WEIGHT_CONTEXT = 85.0;
  private static final double WEIGHT_RECENCY = 45.0;
  private static final double WEIGHT_REJECTION = 80.0;

  /**
   * Computes the composite score for a candidate.
   */
  public static double scoreCandidate(Candidate c)
  {
    double score = c.source.baseScore;

    // Log-scale frequency score: log2(1 + freq)
    if (c.frequency > 0)
    {
      double logFreq = Math.log(1.0 + c.frequency) / Math.log(2.0);
      score += logFreq * WEIGHT_FREQ;
    }

    // Prefix match bonus: higher when typed prefix covers more of the word
    score += c.prefixMatchRatio * WEIGHT_PREFIX;

    // Typo edit-distance penalty
    score -= c.editDistance * WEIGHT_DIST_PENALTY;

    // Context / collocation bonus
    score += c.contextScore * WEIGHT_CONTEXT;

    // User recency bonus
    score += c.recencyScore * WEIGHT_RECENCY;

    // Rejection penalty (from previous undo actions)
    score -= c.rejectionPenalty * WEIGHT_REJECTION;

    c.compositeScore = score;
    return score;
  }

  /**
   * Deduplicates candidates, computes scores, and returns a sorted list of top candidates.
   */
  public static List<Candidate> rank(List<Candidate> rawCandidates, int maxResults)
  {
    if (rawCandidates == null || rawCandidates.isEmpty() || maxResults <= 0)
    {
      return Collections.emptyList();
    }

    // Deduplicate by case-insensitive word (or exact Bengali text)
    Map<String, Candidate> uniqueMap = new HashMap<>(rawCandidates.size());
    for (Candidate c : rawCandidates)
    {
      if (c == null || c.word == null || c.word.trim().isEmpty()) continue;
      String key = c.word.toLowerCase(Locale.ROOT);

      Candidate existing = uniqueMap.get(key);
      if (existing == null)
      {
        uniqueMap.put(key, c);
      }
      else
      {
        // Merge attributes: keep highest source priority, highest frequency, best scores
        if (c.source.baseScore > existing.source.baseScore)
        {
          existing.source = c.source;
        }
        if (c.frequency > existing.frequency)
        {
          existing.frequency = c.frequency;
        }
        if (c.editDistance < existing.editDistance)
        {
          existing.editDistance = c.editDistance;
        }
        if (c.prefixMatchRatio > existing.prefixMatchRatio)
        {
          existing.prefixMatchRatio = c.prefixMatchRatio;
        }
        existing.contextScore = Math.max(existing.contextScore, c.contextScore);
        existing.recencyScore = Math.max(existing.recencyScore, c.recencyScore);
        existing.rejectionPenalty = Math.max(existing.rejectionPenalty, c.rejectionPenalty);
      }
    }

    List<Candidate> list = new ArrayList<>(uniqueMap.values());
    for (Candidate c : list)
    {
      scoreCandidate(c);
    }

    // Sort descending by compositeScore
    Collections.sort(list, new Comparator<Candidate>()
    {
      @Override
      public int compare(Candidate a, Candidate b)
      {
        return Double.compare(b.compositeScore, a.compositeScore);
      }
    });

    if (list.size() > maxResults)
    {
      return list.subList(0, maxResults);
    }
    return list;
  }
}
