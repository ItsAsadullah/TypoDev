package juloo.keyboard2;

import java.util.concurrent.atomic.AtomicBoolean;
import juloo.keyboard2.suggestions.Suggestions;
import org.junit.Test;
import static org.junit.Assert.*;

public class SuggestionsSentenceTest
{
  private static class MockCallback implements Suggestions.Callback
  {
    String textBefore = null;
    boolean selectionActive = false;
    Suggestions lastSuggestions = null;

    @Override
    public void set_suggestions(Suggestions suggestions)
    {
      this.lastSuggestions = suggestions;
    }

    @Override
    public CharSequence getTextBeforeCursor(int n, int flags)
    {
      return textBefore;
    }

    @Override
    public boolean isSelectionActive()
    {
      return selectionActive;
    }
  }

  @Test
  public void testSuggestionsNotWipedOnEmptyWordWithSentenceContext()
  {
    MockCallback cb = new MockCallback();
    Config conf = new Config();
    conf.editor_config.should_show_candidates_view = true;

    Suggestions s = new Suggestions(cb, conf, null);
    s.started();

    // 1. User has typed "আমি তোমাকে " (trailing space)
    cb.textBefore = "আমি তোমাকে ";
    s.currently_typed_word("");

    // Verify suggestions were NOT cleared, but populated with sentence completions
    assertTrue("Suggestions count must be > 0 for active sentence context", s.count > 0);
    boolean foundBhalobashi = false;
    for (int i = 0; i < s.count; i++)
    {
      if (s.suggestions[i] != null && s.suggestions[i].contains("ভালোবাসি"))
      {
        foundBhalobashi = true;
        break;
      }
    }
    assertTrue("Should suggest ভালোবাসি for context 'আমি তোমাকে '", foundBhalobashi);
    assertFalse("Sentence completion must not trigger autocorrect", s.should_autocorrect);

    // 2. If text selection is active, suggestions should be cleared
    cb.selectionActive = true;
    s.currently_typed_word("");
    assertEquals("Suggestions should be cleared when selection is active", 0, s.count);

    // 3. User types in English: "how are you "
    cb.selectionActive = false;
    cb.textBefore = "how are you ";
    s.currently_typed_word("");
    assertTrue("English sentence context must yield predictions", s.count > 0);
    boolean foundDoing = false;
    for (int i = 0; i < s.count; i++)
    {
      if (s.suggestions[i] != null && (s.suggestions[i].contains("doing") || s.suggestions[i].contains("today")))
      {
        foundDoing = true;
        break;
      }
    }
    assertTrue("Should suggest doing/today for context 'how are you '", foundDoing);
  }
}
