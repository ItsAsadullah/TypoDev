package typodev.keyboard;

import android.os.Handler;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.InputConnection;
import typodev.keyboard.suggestions.AutocorrectDecision;
import typodev.keyboard.suggestions.Candidate;
import typodev.keyboard.suggestions.Suggestions;
import org.junit.Test;
import static org.junit.Assert.*;

public class AvroAndJatiyoTypingTest
{
  @Test
  public void testAvroSuggestionBarLayout()
  {
    Config conf = new Config();
    conf.editor_config.should_show_candidates_view = true;
    conf.is_avro_mode = true;
    conf.is_bengali_mode = true;

    Suggestions.Callback cb = new Suggestions.Callback()
    {
      @Override public void set_suggestions(Suggestions suggestions) {}
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return ""; }
      @Override public boolean isSelectionActive() { return false; }
    };

    Suggestions s = new Suggestions(cb, conf, null);
    s.started();

    s.currently_typed_word("khoma");

    assertTrue("isAvroBanglishActive must be true", s.isAvroBanglishActive());
    assertTrue("Must have at least 2 suggestions", s.count >= 2);
    // Slot 0: Exact typed Latin word
    assertEquals("khoma", s.suggestions[0]);
    // Slot 1: Top transliterated Bengali candidate
    assertEquals("ক্ষমা", s.suggestions[1]);
    assertEquals("ক্ষমা", s.getTopBanglaCandidate());
  }

  @Test
  public void testJatiyoAutocorrectDisabledInConfig()
  {
    Config conf = new Config();
    conf.editor_config.should_show_candidates_view = true;
    conf.is_avro_mode = false;
    conf.is_bengali_mode = true;
    conf.space_bar_auto_complete = false; // User turned off auto-correction in settings

    Suggestions.Callback cb = new Suggestions.Callback()
    {
      @Override public void set_suggestions(Suggestions suggestions) {}
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return ""; }
      @Override public boolean isSelectionActive() { return false; }
    };

    Suggestions s = new Suggestions(cb, conf, null);
    s.started();

    s.currently_typed_word("এমনি");

    assertFalse("should_autocorrect must be false when space_bar_auto_complete is disabled", s.should_autocorrect);
  }

  @Test
  public void testJatiyoAutocorrectNeverReplacesValidWordEvenIfConfigEnabled()
  {
    Config conf = new Config();
    conf.editor_config.should_show_candidates_view = true;
    conf.is_avro_mode = false;
    conf.is_bengali_mode = true;
    conf.space_bar_auto_complete = true;

    Suggestions.Callback cb = new Suggestions.Callback()
    {
      @Override public void set_suggestions(Suggestions suggestions) {}
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return ""; }
      @Override public boolean isSelectionActive() { return false; }
    };

    Suggestions s = new Suggestions(cb, conf, null);
    s.started();

    s.currently_typed_word("এমনি");

    // "এমনি" is a valid word, so spacebar should NOT autocorrect it to "এমনিতেও"
    assertFalse("Valid Bengali word 'এমনি' should never be autocorrected", s.should_autocorrect);
  }

  @Test
  public void testKeyEventHandlerAvroComposingAndSpacebar()
  {
    final StringBuilder committedBuffer = new StringBuilder();
    final StringBuilder composingBuffer = new StringBuilder();

    final InputConnection dummyIc = new BaseInputConnection(null, false)
    {
      @Override
      public boolean setComposingText(CharSequence text, int newCursorPosition)
      {
        composingBuffer.setLength(0);
        if (text != null)
        {
          composingBuffer.append(text);
        }
        return true;
      }

      @Override
      public boolean commitText(CharSequence text, int newCursorPosition)
      {
        composingBuffer.setLength(0);
        if (text != null)
        {
          committedBuffer.append(text);
        }
        return true;
      }

      @Override public boolean finishComposingText() { composingBuffer.setLength(0); return true; }
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return committedBuffer.toString(); }
    };

    KeyEventHandler.IReceiver receiver = new KeyEventHandler.IReceiver()
    {
      @Override public void handle_event_key(KeyValue.Event ev) {}
      @Override public void set_shift_state(boolean state, boolean lock) {}
      @Override public void set_compose_pending(boolean pending) {}
      @Override public void selection_state_changed(boolean selection_is_ongoing) {}
      @Override public InputConnection getCurrentInputConnection() { return dummyIc; }
      @Override public Handler getHandler() { return null; }
      @Override public void set_suggestions(Suggestions suggestions) {}
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return committedBuffer.toString(); }
    };

    Config conf = new Config();
    conf.editor_config.should_show_candidates_view = true;
    conf.is_avro_mode = true;
    conf.is_bengali_mode = true;
    conf.space_bar_auto_complete = true;

    Suggestions s = new Suggestions(receiver, conf, null);
    KeyEventHandler handler = new KeyEventHandler(receiver, s);
    handler.started(conf);

    // 1. Type "khoma" character by character
    handler.send_text("k");
    assertEquals("ক", composingBuffer.toString());

    handler.send_text("h");
    assertEquals("খ", composingBuffer.toString());

    handler.send_text("o");
    assertEquals("খো", composingBuffer.toString());

    handler.send_text("m");
    // Live composition uses the master lexicon's exact candidate for "khom".
    assertEquals("ক্ষোম", composingBuffer.toString());

    handler.send_text("a");
    assertEquals("ক্ষমা", composingBuffer.toString());
    assertTrue("Composing active", handler.isAvroComposingActive());

    // 2. Press space bar -> commits "ক্ষমা "
    handler.handle_space_bar();

    assertEquals("ক্ষমা ", committedBuffer.toString());
    assertEquals("", composingBuffer.toString());
    assertFalse("Composing buffer must be cleared after space", handler.isAvroComposingActive());
  }

  @Test
  public void testKeyEventHandlerAvroBackspace()
  {
    final StringBuilder composingBuffer = new StringBuilder();

    final InputConnection dummyIc = new BaseInputConnection(null, false)
    {
      @Override
      public boolean setComposingText(CharSequence text, int newCursorPosition)
      {
        composingBuffer.setLength(0);
        if (text != null) composingBuffer.append(text);
        return true;
      }
      @Override
      public boolean commitText(CharSequence text, int newCursorPosition)
      {
        composingBuffer.setLength(0);
        return true;
      }
      @Override public boolean finishComposingText() { composingBuffer.setLength(0); return true; }
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return ""; }
    };

    KeyEventHandler.IReceiver receiver = new KeyEventHandler.IReceiver()
    {
      @Override public void handle_event_key(KeyValue.Event ev) {}
      @Override public void set_shift_state(boolean state, boolean lock) {}
      @Override public void set_compose_pending(boolean pending) {}
      @Override public void selection_state_changed(boolean selection_is_ongoing) {}
      @Override public InputConnection getCurrentInputConnection() { return dummyIc; }
      @Override public Handler getHandler() { return null; }
      @Override public void set_suggestions(Suggestions suggestions) {}
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return ""; }
    };

    Config conf = new Config();
    conf.editor_config.should_show_candidates_view = true;
    conf.is_avro_mode = true;
    conf.is_bengali_mode = true;

    Suggestions s = new Suggestions(receiver, conf, null);
    KeyEventHandler handler = new KeyEventHandler(receiver, s);
    handler.started(conf);

    handler.send_text("k");
    handler.send_text("h");
    handler.send_text("o");
    handler.send_text("m");
    handler.send_text("a");
    assertEquals("ক্ষমা", composingBuffer.toString());

    // Backspace restores the same lexicon candidate used when typing "khom".
    handler.handle_backspace();
    assertEquals("ক্ষোম", composingBuffer.toString());

    // Backspace all the way to 0
    handler.handle_backspace();
    handler.handle_backspace();
    handler.handle_backspace();
    handler.handle_backspace();
    assertEquals("", composingBuffer.toString());
    assertFalse(handler.isAvroComposingActive());
  }

  @Test
  public void testJonSuggestionsNoNumbers()
  {
    typodev.keyboard.suggestions.BanglishEngine engine = typodev.keyboard.suggestions.BanglishEngine.instance();
    java.util.List<Candidate> cands = engine.generateCandidates("jon", 5);
    assertFalse(cands.isEmpty());
    assertEquals("Top candidate for 'jon' must be 'জন'", "জন", cands.get(0).word);
    for (Candidate c : cands)
    {
      assertFalse("Candidate must never contain numbers: " + c.word, c.word.matches(".*[0-9০-৯].*"));
    }
  }

  @Test
  public void testSonnoSuggestions()
  {
    typodev.keyboard.suggestions.BanglishEngine engine = typodev.keyboard.suggestions.BanglishEngine.instance();
    java.util.List<Candidate> cands = engine.generateCandidates("sonno", 5);
    assertFalse(cands.isEmpty());
    assertEquals("Top candidate for 'sonno' must be 'সৈন্য'", "সৈন্য", cands.get(0).word);
    boolean hasShunno = false;
    for (Candidate c : cands)
    {
      if ("শূন্য".equals(c.word)) hasShunno = true;
    }
    assertTrue("'sonno' candidates should offer 'শূন্য'", hasShunno);
  }

  @Test
  public void testAcheNotAcche()
  {
    typodev.keyboard.suggestions.BanglishEngine engine = typodev.keyboard.suggestions.BanglishEngine.instance();
    assertEquals("আছে", engine.getExactBangla("ache"));
    java.util.List<Candidate> cands = engine.generateCandidates("ache", 3);
    assertEquals("আছে", cands.get(0).word);
  }

  @Test
  public void testMoronbadhiCompound()
  {
    typodev.keyboard.suggestions.BanglishEngine engine = typodev.keyboard.suggestions.BanglishEngine.instance();
    java.util.List<Candidate> cands = engine.generateCandidates("moronbadhi", 5);
    assertFalse(cands.isEmpty());
    boolean hasMoronbadhi = false;
    for (Candidate c : cands)
    {
      if ("মরণব্যাধি".equals(c.word)) hasMoronbadhi = true;
    }
    assertTrue("Candidates for 'moronbadhi' must offer 'মরণব্যাধি'", hasMoronbadhi);
  }

  @Test
  public void testOsthisthoOstitwo()
  {
    typodev.keyboard.suggestions.BanglishEngine engine = typodev.keyboard.suggestions.BanglishEngine.instance();
    java.util.List<Candidate> cands = engine.generateCandidates("osthisto", 5);
    assertFalse(cands.isEmpty());
    boolean hasOstitwo = false;
    for (Candidate c : cands)
    {
      if ("অস্তিত্ব".equals(c.word)) hasOstitwo = true;
    }
    assertTrue("Candidates for 'osthisto' must offer 'অস্তিত্ব'", hasOstitwo);
  }

  @Test
  public void testJokjonQwertNeighborToJokhon()
  {
    typodev.keyboard.suggestions.BanglishEngine engine = typodev.keyboard.suggestions.BanglishEngine.instance();
    java.util.List<Candidate> cands = engine.generateCandidates("jokjon", 5);
    assertFalse(cands.isEmpty());
    boolean hasJokhon = false;
    for (Candidate c : cands)
    {
      if ("যখন".equals(c.word)) hasJokhon = true;
    }
    assertTrue("Candidates for 'jokjon' must offer 'যখন' via QWERTY proximity", hasJokhon);
  }

  @Test
  public void testAvroComposingSurvivesSelectionEchoWithoutCandidatesSpan()
  {
    final StringBuilder composingBuffer = new StringBuilder();
    final InputConnection dummyIc = new BaseInputConnection(null, false)
    {
      @Override
      public boolean setComposingText(CharSequence text, int newCursorPosition)
      {
        composingBuffer.setLength(0);
        if (text != null) composingBuffer.append(text);
        return true;
      }
      @Override public boolean finishComposingText() { composingBuffer.setLength(0); return true; }
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return composingBuffer.toString(); }
    };

    KeyEventHandler.IReceiver receiver = new KeyEventHandler.IReceiver()
    {
      @Override public void handle_event_key(KeyValue.Event ev) {}
      @Override public void set_shift_state(boolean state, boolean lock) {}
      @Override public void set_compose_pending(boolean pending) {}
      @Override public void selection_state_changed(boolean selection_is_ongoing) {}
      @Override public InputConnection getCurrentInputConnection() { return dummyIc; }
      @Override public Handler getHandler() { return null; }
      @Override public void set_suggestions(Suggestions suggestions) {}
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return composingBuffer.toString(); }
    };

    Config conf = new Config();
    conf.editor_config.should_show_candidates_view = true;
    conf.is_avro_mode = true;
    conf.is_bengali_mode = true;

    Suggestions s = new Suggestions(receiver, conf, null);
    KeyEventHandler handler = new KeyEventHandler(receiver, s);
    handler.started(conf);

    int cursor = 0;
    for (String ch : new String[]{"k", "h", "o", "m", "a"})
    {
      int oldCursor = cursor;
      handler.send_text(ch);
      cursor = composingBuffer.length();
      // WebView/custom editor reports candidatesStart=-1, candidatesEnd=-1 on setComposingText
      handler.selection_updated(oldCursor, cursor, cursor, -1, -1);
      assertTrue("Avro composing must stay active after '" + ch + "'", handler.isAvroComposingActive());
    }
    assertEquals("ক্ষমা", composingBuffer.toString());

    // Now simulate user tapping to start of field (cursor = 0)
    handler.selection_updated(cursor, 0, 0, -1, -1);
    assertFalse("Explicit cursor jump to 0 must end Avro composing", handler.isAvroComposingActive());
  }

  @Test
  public void testAvroDisabledInPasswordAndTypeNullFields()
  {
    KeyEventHandler.IReceiver receiver = new KeyEventHandler.IReceiver()
    {
      @Override public void handle_event_key(KeyValue.Event ev) {}
      @Override public void set_shift_state(boolean state, boolean lock) {}
      @Override public void set_compose_pending(boolean pending) {}
      @Override public void selection_state_changed(boolean selection_is_ongoing) {}
      @Override public InputConnection getCurrentInputConnection() { return null; }
      @Override public Handler getHandler() { return null; }
      @Override public void set_suggestions(Suggestions suggestions) {}
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return ""; }
    };

    Config conf = new Config();
    conf.is_avro_mode = true;
    conf.is_bengali_mode = true;
    conf.editor_config.is_password = true;

    Suggestions s = new Suggestions(receiver, conf, null);
    KeyEventHandler handler = new KeyEventHandler(receiver, s);
    handler.started(conf);
    assertFalse("Avro must be disabled in password fields", handler.isAvroActive());

    conf.editor_config.is_password = false;
    conf.editor_config.should_move_cursor_force_fallback = true;
    assertFalse("Avro must be disabled in TYPE_NULL / raw key fields", handler.isAvroActive());
  }

  @Test
  public void testMathSolverSuggestionOpensInAvroMode()
  {
    final String[] openedProblem = new String[]{null};
    final InputConnection dummyIc = new BaseInputConnection(null, false)
    {
      @Override public boolean setComposingText(CharSequence text, int newCursorPosition) { return true; }
      @Override public boolean commitText(CharSequence text, int newCursorPosition)
      {
        fail("Math solver suggestion must not be committed as text in Avro mode");
        return true;
      }
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return "(a+b)^2="; }
    };

    KeyEventHandler.IReceiver receiver = new KeyEventHandler.IReceiver()
    {
      @Override public void showMathSolver(String mathProblem) { openedProblem[0] = mathProblem; }
      @Override public void handle_event_key(KeyValue.Event ev) {}
      @Override public void set_shift_state(boolean state, boolean lock) {}
      @Override public void set_compose_pending(boolean pending) {}
      @Override public void selection_state_changed(boolean selection_is_ongoing) {}
      @Override public InputConnection getCurrentInputConnection() { return dummyIc; }
      @Override public Handler getHandler() { return null; }
      @Override public void set_suggestions(Suggestions suggestions) {}
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return "(a+b)^2="; }
    };

    Config conf = new Config();
    conf.is_avro_mode = true;
    conf.is_bengali_mode = true;
    Suggestions s = new Suggestions(receiver, conf, null);
    KeyEventHandler handler = new KeyEventHandler(receiver, s);
    handler.started(conf);
    handler.send_text("k");
    assertTrue(handler.isAvroComposingActive());

    handler.suggestion_entered("🧮 AI সমাধান (স্টেপ বাই স্টেপ)");
    assertEquals("(a+b)^2=", openedProblem[0]);
  }
}
