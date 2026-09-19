package juloo.keyboard2;

import java.util.List;
import juloo.keyboard2.suggestions.Candidate;
import juloo.keyboard2.suggestions.CoreLexiconManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CoreLexiconManagerTest
{
  private CoreLexiconManager manager;

  @Before
  public void setUp()
  {
    manager = new CoreLexiconManager(null); // uses embedded fallback
  }

  @Test
  public void testEnglishPrefixQuery()
  {
    List<Candidate> cands = manager.queryPrefix("th", 5);
    assertFalse("Should return candidates for 'th'", cands.isEmpty());
    // The top candidate should be "the" (freq 255)
    assertEquals("the", cands.get(0).word);
    assertEquals(255, cands.get(0).frequency);
  }

  @Test
  public void testBengaliPrefixQuery()
  {
    List<Candidate> cands = manager.queryPrefix("ভা", 5);
    assertFalse("Should return candidates for 'ভা'", cands.isEmpty());
    assertTrue("Should contain ভালো", cands.get(0).word.contains("ভালো"));
  }

  @Test
  public void testLevenshteinTypo()
  {
    // "thsi" has edit distance 1 with "this"
    assertEquals(1, CoreLexiconManager.computeLevenshteinDistance1("thsi", "this"));
    assertEquals(1, CoreLexiconManager.computeLevenshteinDistance1("helol", "hello"));
    assertEquals(0, CoreLexiconManager.computeLevenshteinDistance1("hello", "hello"));
    assertEquals(2, CoreLexiconManager.computeLevenshteinDistance1("abc", "xyz"));
  }

  @Test
  public void testTypoCorrectionQuery()
  {
    List<Candidate> typos = manager.queryTypoCorrections("thsi", 3);
    assertFalse("Typo corrections for 'thsi' should include 'this'", typos.isEmpty());
    assertEquals("this", typos.get(0).word);
    assertEquals(1, typos.get(0).editDistance);
  }
}
