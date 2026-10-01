package typodev.keyboard;

import java.util.List;
import typodev.keyboard.suggestions.BlockedSuggestionsStore;
import typodev.keyboard.suggestions.Candidate;
import typodev.keyboard.suggestions.UserVocabularyStore;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlockedSuggestionsStoreTest
{
  private BlockedSuggestionsStore blockedStore;
  private UserVocabularyStore userStore;

  @Before
  public void setUp()
  {
    blockedStore = new BlockedSuggestionsStore(null);
    blockedStore.clearBlocked();

    userStore = new UserVocabularyStore(null);
    userStore.clearLearnedData();
  }

  @Test
  public void testBlockWordAndCaseInsensitivity()
  {
    blockedStore.blockWord("hgj kk");

    assertTrue("Blocked phrase should match exact", blockedStore.isBlocked("hgj kk"));
    assertTrue("Blocked phrase should match case-insensitive", blockedStore.isBlocked("HGJ KK"));
    assertTrue("Blocked phrase should match mixed case", blockedStore.isBlocked("Hgj Kk"));
    assertTrue("Blocked phrase should match with trailing punctuation", blockedStore.isBlocked("hgj kk."));

    assertFalse("Unrelated word should not be blocked", blockedStore.isBlocked("hello"));
  }

  @Test
  public void testUnblockAndClear()
  {
    blockedStore.blockWord("jfk");
    assertTrue(blockedStore.isBlocked("jfk"));

    blockedStore.unblockWord("jfk");
    assertFalse(blockedStore.isBlocked("jfk"));

    blockedStore.blockWord("word1");
    blockedStore.blockWord("word2");
    assertTrue(blockedStore.getBlockedCount() >= 2);

    blockedStore.clearBlocked();
    assertEquals(0, blockedStore.getBlockedCount());
    assertFalse(blockedStore.isBlocked("word1"));
  }

  @Test
  public void testUnblockWithDifferentCapitalization()
  {
    blockedStore.blockWord("Hello");
    blockedStore.blockWord("HELLO");
    assertEquals("Case variants represent one blocked word", 1, blockedStore.getBlockedCount());

    blockedStore.unblockWord("hello");
    assertFalse(blockedStore.isBlocked("Hello"));
    assertFalse(blockedStore.isBlocked("HELLO"));
    assertEquals(0, blockedStore.getBlockedCount());
  }

  @Test
  public void testUserVocabularyRemoveWord()
  {
    userStore.learnWord("customword");
    assertTrue(userStore.containsWord("customword"));

    userStore.removeWord("customword");
    assertFalse(userStore.containsWord("customword"));
  }
}
