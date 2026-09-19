package juloo.keyboard2;

import java.util.List;
import juloo.keyboard2.suggestions.Candidate;
import juloo.keyboard2.suggestions.UserVocabularyStore;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class UserVocabularyStoreTest
{
  private UserVocabularyStore store;

  @Before
  public void setUp()
  {
    store = new UserVocabularyStore(null); // in-memory
    store.clearLearnedData();
  }

  @Test
  public void testLearnAndQuery()
  {
    store.learnWord("termux");
    store.learnWord("terminal");

    List<Candidate> cands = store.queryCandidates("term", 5);
    assertEquals("Should return both learned words", 2, cands.size());

    assertTrue(store.containsWord("termux"));
    assertTrue(store.containsWord("terminal"));
  }

  @Test
  public void testFrequencyIncrement()
  {
    store.learnWord("supercalifragilistic");
    store.learnWord("supercalifragilistic");
    store.learnWord("supercalifragilistic");

    List<Candidate> cands = store.queryCandidates("super", 2);
    assertFalse(cands.isEmpty());
    // Scaled frequency should reflect 3 occurrences
    assertTrue("Frequency should increase with repeated usage", cands.get(0).frequency >= 75);
  }

  @Test
  public void testRejectionPenalty()
  {
    store.learnWord("mistyped");
    store.recordRejection("mistyped", "original");

    float penalty = store.getRejectionPenalty("mistyped");
    assertTrue("Rejection penalty should be greater than zero", penalty > 0f);
  }

  @Test
  public void testLearningToggle()
  {
    store.setLearningEnabled(false);
    store.learnWord("ignoredword");

    assertFalse("Word should not be learned when learning is disabled", store.containsWord("ignoredword"));
  }

  @Test
  public void testClearData()
  {
    store.learnWord("word1");
    store.learnWord("word2");
    assertEquals(2, store.getWordCount());

    store.clearLearnedData();
    assertEquals(0, store.getWordCount());
    assertFalse(store.containsWord("word1"));
  }
}
