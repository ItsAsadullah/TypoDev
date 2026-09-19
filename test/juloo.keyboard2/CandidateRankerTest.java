package juloo.keyboard2;

import java.util.ArrayList;
import java.util.List;
import juloo.keyboard2.suggestions.Candidate;
import juloo.keyboard2.suggestions.CandidateRanker;
import org.junit.Test;
import static org.junit.Assert.*;

public class CandidateRankerTest
{
  @Test
  public void testFrequencyRankingOverAlphabetical()
  {
    List<Candidate> raw = new ArrayList<>();
    // Simulated alphabetical scan where low-frequency word appeared first
    raw.add(new Candidate("thab", Candidate.Source.EXTERNAL, 10, 0, 0.5f));
    raw.add(new Candidate("thack", Candidate.Source.EXTERNAL, 5, 0, 0.4f));
    raw.add(new Candidate("the", Candidate.Source.BUILTIN_MAIN, 255, 0, 0.66f));
    raw.add(new Candidate("that", Candidate.Source.BUILTIN_MAIN, 240, 0, 0.5f));

    List<Candidate> ranked = CandidateRanker.rank(raw, 4);

    assertFalse(ranked.isEmpty());
    assertEquals("Top candidate must be 'the' due to overwhelming frequency", "the", ranked.get(0).word);
    assertEquals("Second candidate must be 'that'", "that", ranked.get(1).word);
  }

  @Test
  public void testTypoPenaltyVsExactPrefix()
  {
    List<Candidate> raw = new ArrayList<>();
    // Prefix completion: "pro" -> "program" (edit distance 0)
    raw.add(new Candidate("program", Candidate.Source.BUILTIN_MAIN, 150, 0, 0.43f));
    // Edit distance 1 typo: "pro" -> "pre" (edit distance 1)
    raw.add(new Candidate("pre", Candidate.Source.TYPO_CORRECTION, 160, 1, 0.7f));

    List<Candidate> ranked = CandidateRanker.rank(raw, 2);

    assertEquals("Prefix completion should outrank distance-1 edit", "program", ranked.get(0).word);
  }

  @Test
  public void testRejectionPenaltySuppressesWord()
  {
    List<Candidate> raw = new ArrayList<>();
    Candidate unwanted = new Candidate("teh", Candidate.Source.EXTERNAL, 200, 0, 1.0f);
    unwanted.rejectionPenalty = 1.0f; // User explicitly rejected it

    Candidate alternative = new Candidate("the", Candidate.Source.BUILTIN_MAIN, 250, 0, 1.0f);

    raw.add(unwanted);
    raw.add(alternative);

    List<Candidate> ranked = CandidateRanker.rank(raw, 2);

    assertEquals("Candidate with rejection penalty must be pushed down", "the", ranked.get(0).word);
  }

  @Test
  public void testUserRecencyAndAffinityBoost()
  {
    List<Candidate> raw = new ArrayList<>();
    Candidate normal = new Candidate("project", Candidate.Source.BUILTIN_MAIN, 100, 0, 0.4f);

    Candidate personal = new Candidate("proactive", Candidate.Source.PERSONAL, 120, 0, 0.33f);
    personal.recencyScore = 1.0f; // Recently typed

    raw.add(normal);
    raw.add(personal);

    List<Candidate> ranked = CandidateRanker.rank(raw, 2);

    assertEquals("Personal recent candidate should outrank normal candidate", "proactive", ranked.get(0).word);
  }
}
