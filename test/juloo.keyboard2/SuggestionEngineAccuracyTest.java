package juloo.keyboard2;

import java.util.List;
import juloo.keyboard2.suggestions.BanglishEngine;
import juloo.keyboard2.suggestions.Candidate;
import juloo.keyboard2.suggestions.CandidateRanker;
import juloo.keyboard2.suggestions.CoreLexiconManager;
import juloo.keyboard2.suggestions.NextWordPredictor;
import juloo.keyboard2.suggestions.UserVocabularyStore;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SuggestionEngineAccuracyTest
{
  private CoreLexiconManager coreLexicon;
  private BanglishEngine banglish;
  private UserVocabularyStore userVocab;
  private NextWordPredictor nextWord;

  @Before
  public void setUp()
  {
    coreLexicon = new CoreLexiconManager(null);
    banglish = BanglishEngine.instance();
    userVocab = new UserVocabularyStore(null);
    nextWord = NextWordPredictor.instance(null);
  }

  @Test
  public void testTopSuggestionsAccuracyEnglish()
  {
    // 1. "th" should return "the" as top-1
    List<Candidate> candsTh = coreLexicon.queryPrefix("th", 3);
    assertFalse(candsTh.isEmpty());
    assertEquals("the", candsTh.get(0).word);

    // 2. "wh" should return "what" or "who" or "which" in Top-3
    List<Candidate> candsWh = coreLexicon.queryPrefix("wh", 3);
    assertFalse(candsWh.isEmpty());
    boolean hasCommonWh = false;
    for (Candidate c : candsWh)
    {
      if ("what".equals(c.word) || "who".equals(c.word) || "which".equals(c.word) || "when".equals(c.word))
      {
        hasCommonWh = true;
        break;
      }
    }
    assertTrue("Top-3 for 'wh' must include common interrogative words", hasCommonWh);
  }

  @Test
  public void testTopSuggestionsAccuracyBengali()
  {
    // 1. "ভা" should return "ভালো" as top-1
    List<Candidate> candsV = coreLexicon.queryPrefix("ভা", 3);
    assertFalse(candsV.isEmpty());
    assertEquals("ভালো", candsV.get(0).word);

    // 2. "ধ" should return "ধন্যবাদ" in Top-3
    List<Candidate> candsDh = coreLexicon.queryPrefix("ধ", 3);
    assertFalse(candsDh.isEmpty());
    boolean hasDhonnobad = false;
    for (Candidate c : candsDh)
    {
      if ("ধন্যবাদ".equals(c.word)) hasDhonnobad = true;
    }
    assertTrue("Top-3 for 'ধ' must contain 'ধন্যবাদ'", hasDhonnobad);
  }

  @Test
  public void testBanglishTransliterationTop1Accuracy()
  {
    assertEquals("আমি", banglish.generateCandidates("ami", 1).get(0).word);
    assertEquals("তুমি", banglish.generateCandidates("tumi", 1).get(0).word);
    assertEquals("কেমন", banglish.generateCandidates("kemon", 1).get(0).word);
    assertEquals("ভালো", banglish.generateCandidates("bhalo", 1).get(0).word);
    assertEquals("ধন্যবাদ", banglish.generateCandidates("dhonnobad", 1).get(0).word);
  }

  @Test
  public void testPersonalVocabularyInTop3()
  {
    userVocab.learnWord("Asadullah");
    userVocab.learnWord("Asadullah");
    userVocab.learnWord("Asadullah");

    List<Candidate> userCands = userVocab.queryCandidates("Asad", 3);
    assertFalse("Personal vocabulary must be retrieved", userCands.isEmpty());
    assertEquals("Asadullah", userCands.get(0).word);

    List<Candidate> ranked = CandidateRanker.rank(userCands, 3);
    assertEquals("Asadullah", ranked.get(0).word);
  }

  @Test
  public void testTypoCorrectionInTop3()
  {
    List<Candidate> typos = coreLexicon.queryTypoCorrections("thsi", 3);
    assertFalse("Typo correction must return matches", typos.isEmpty());
    assertEquals("this", typos.get(0).word);
  }

  @Test
  public void testQueryLatencyUnderFiveMilliseconds()
  {
    long start = System.nanoTime();
    int iterations = 100;

    for (int i = 0; i < iterations; i++)
    {
      coreLexicon.queryPrefix("th", 10);
      coreLexicon.queryPrefix("ভা", 10);
      banglish.generateCandidates("bhalo", 5);
      coreLexicon.queryTypoCorrections("thsi", 5);
    }

    long elapsedNanos = System.nanoTime() - start;
    double avgMsPerQuery = (elapsedNanos / 1_000_000.0) / iterations;

    assertTrue("Average query latency must be under 5ms, actual: " + avgMsPerQuery + "ms", avgMsPerQuery < 5.0);
  }
}
