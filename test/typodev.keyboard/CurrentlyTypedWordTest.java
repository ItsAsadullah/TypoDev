package typodev.keyboard;

import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class CurrentlyTypedWordTest
{
  private CurrentlyTypedWord tracker(String before, String after)
  {
    Config config = new Config();
    config.editor_config.initial_text_before_cursor = before;
    config.editor_config.initial_text_after_cursor = after;
    config.editor_config.initial_sel_start = before.length();
    config.editor_config.initial_sel_end = before.length();
    CurrentlyTypedWord word = new CurrentlyTypedWord(null, value -> {});
    word.started(config, null);
    return word;
  }

  @Test
  public void testDeletingSuffixAllowsReplacementAtCursor()
  {
    CurrentlyTypedWord word = tracker("hel", "lo");
    word.remove_surrounding_text(3, 2);
    assertEquals(0, word.cursor_relative());
    word.typed("help");
    assertEquals("help", word.get());
    assertEquals(0, word.cursor_relative());
  }

  @Test
  public void testDeletingBeforeCursorPreservesSuffixOffset()
  {
    CurrentlyTypedWord word = tracker("hell", "o");
    word.remove_surrounding_text(2, 0);
    assertEquals("heo", word.get());
    assertEquals(-1, word.cursor_relative());
    word.typed("r");
    assertEquals("hero", word.get());
  }

  @Test
  public void testWordSeparatorInMiddlePreservesFollowingWord()
  {
    CurrentlyTypedWord word = tracker("hello", "world");
    word.typed(" ");
    assertEquals("world", word.get());
    assertEquals(-5, word.cursor_relative());
    assertEquals("hello", word.get_last_completed_word());
    word.typed("new");
    assertEquals("newworld", word.get());
  }

  @Test
  public void testSupplementaryLetterUsesAndroidCursorUnits()
  {
    CurrentlyTypedWord word = tracker("", "");
    String letter = new String(Character.toChars(0x10400));
    word.typed(letter);
    assertEquals(2, word._cursor);
    word.selection_updated(0, 2, 2);
    assertEquals(letter, word.get());
    assertEquals(0, word.cursor_relative());
  }

  @Test
  public void testBackspaceRemovesWholeSupplementaryLetter()
  {
    CurrentlyTypedWord word = tracker("a" + new String(Character.toChars(0x10400)), "");
    word.event_sent(KeyEvent.KEYCODE_DEL, 0);
    assertEquals("a", word.get());
    assertEquals(1, word._cursor);
  }

  @Test
  public void testFinishedIgnoresLateTyping()
  {
    CurrentlyTypedWord word = tracker("hello", "");
    word.finished();
    word.typed("later");
    assertEquals("", word.get());
    assertNull(word._ic);
  }

  @Test
  public void testNewSelectedEditorClearsPreviousWordAndContext()
  {
    CurrentlyTypedWord word = tracker("hello", "");
    word.typed(" next");
    Config config = new Config();
    config.editor_config.initial_sel_start = 0;
    config.editor_config.initial_sel_end = 4;
    word.started(config, null);
    assertEquals("", word.get());
    assertEquals("", word.get_last_completed_word());
    assertTrue(word.is_selection_not_empty());
  }

  @Test
  public void testMissingEditorTextPublishesEmptyWord()
  {
    List<String> callbacks = new ArrayList<>();
    CurrentlyTypedWord word = new CurrentlyTypedWord(null, callbacks::add);
    word.set_current_word("hello");
    word.set_current_word((CharSequence) null);
    assertEquals("", word.get());
    assertEquals("", callbacks.get(callbacks.size() - 1));
  }
}
