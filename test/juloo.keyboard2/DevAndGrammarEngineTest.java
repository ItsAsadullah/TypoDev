package juloo.keyboard2;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import java.util.ArrayList;
import java.util.List;
import juloo.keyboard2.suggestions.AutocorrectDecision;
import juloo.keyboard2.suggestions.Candidate;
import juloo.keyboard2.suggestions.CandidateRanker;
import juloo.keyboard2.suggestions.CandidatesView;
import juloo.keyboard2.suggestions.DevSyntaxEngine;
import juloo.keyboard2.suggestions.EnglishGrammarEngine;
import juloo.keyboard2.suggestions.NextWordPredictor;
import juloo.keyboard2.suggestions.Suggestions;
import org.junit.Test;
import static org.junit.Assert.*;

public class DevAndGrammarEngineTest
{
  @Test
  public void testContractions()
  {
    EnglishGrammarEngine engine = EnglishGrammarEngine.instance();

    List<Candidate> cands = engine.queryCandidates("im", 5);
    assertFalse("im should produce candidates", cands.isEmpty());
    assertEquals("I'm", cands.get(0).word);
    assertEquals(Candidate.Source.AUTOCORRECT, cands.get(0).source);

    List<Candidate> candsDont = engine.queryCandidates("dont", 5);
    assertFalse("dont should produce candidates", candsDont.isEmpty());
    assertEquals("don't", candsDont.get(0).word);

    List<Candidate> candsImTitle = engine.queryCandidates("Im", 5);
    assertFalse("Im should produce candidates", candsImTitle.isEmpty());
    assertEquals("I'm", candsImTitle.get(0).word);
  }

  @Test
  public void testSpellingCorrection()
  {
    EnglishGrammarEngine engine = EnglishGrammarEngine.instance();

    List<Candidate> cands = engine.queryCandidates("assingment", 5);
    boolean foundAssignment = false;
    for (Candidate c : cands)
    {
      if ("assignment".equalsIgnoreCase(c.word))
      {
        foundAssignment = true;
        break;
      }
    }
    assertTrue("assingment must correct to assignment", foundAssignment);

    List<Candidate> candsKnow = engine.queryCandidates("knowlege", 5);
    boolean foundKnowledge = false;
    for (Candidate c : candsKnow)
    {
      if ("knowledge".equalsIgnoreCase(c.word))
      {
        foundKnowledge = true;
        break;
      }
    }
    assertTrue("knowlege must correct to knowledge", foundKnowledge);
  }

  @Test
  public void testMorphologicalPossessives()
  {
    EnglishGrammarEngine engine = EnglishGrammarEngine.instance();

    List<Candidate> cands = engine.queryCandidates("Father", 5);
    boolean foundFathersPossessive = false;
    for (Candidate c : cands)
    {
      if ("Father's".equals(c.word))
      {
        foundFathersPossessive = true;
        break;
      }
    }
    assertTrue("Father should generate Father's possessive candidate", foundFathersPossessive);
  }

  @Test
  public void testOrdinalsAndSuperscripts()
  {
    EnglishGrammarEngine engine = EnglishGrammarEngine.instance();

    List<Candidate> cands3rd = engine.queryCandidates("3rd", 5);
    boolean hasSuperscript = false;
    boolean hasPlain = false;
    for (Candidate c : cands3rd)
    {
      if ("3ʳᵈ".equals(c.word)) hasSuperscript = true;
      if ("3rd".equals(c.word)) hasPlain = true;
    }
    assertTrue("3rd must suggest 3ʳᵈ", hasSuperscript);
    assertTrue("3rd must suggest 3rd", hasPlain);

    List<Candidate> cands1st = engine.queryCandidates("1st", 5);
    boolean has1stSup = false;
    for (Candidate c : cands1st)
    {
      if ("1ˢᵗ".equals(c.word)) has1stSup = true;
    }
    assertTrue("1st must suggest 1ˢᵗ", has1stSup);

    List<Candidate> candsX2 = engine.queryCandidates("x2", 5);
    boolean hasX2 = false;
    for (Candidate c : candsX2)
    {
      if ("x²".equals(c.word)) hasX2 = true;
    }
    assertTrue("x2 must suggest x²", hasX2);
  }

  @Test
  public void testHtmlTagCompletions()
  {
    DevSyntaxEngine engine = DevSyntaxEngine.instance();

    // Query for "<di" with text before cursor
    List<Candidate> candsDi = engine.queryCandidates("di", "<di", 10);
    boolean hasDivOpen = false;
    boolean hasDivClose = false;
    boolean hasDivPlain = false;
    boolean hasDivPair = false;

    for (Candidate c : candsDi)
    {
      if ("<div>".equals(c.word)) hasDivOpen = true;
      if ("</div>".equals(c.word)) hasDivClose = true;
      if ("div".equals(c.word)) hasDivPlain = true;
      if ("<div></div>".equals(c.word)) hasDivPair = true;
    }

    assertTrue("<di must suggest <div>", hasDivOpen);
    assertTrue("<di must suggest </div>", hasDivClose);
    assertTrue("<di must suggest div", hasDivPlain);
    assertTrue("<di must suggest <div></div>", hasDivPair);

    // Query for "<ht"
    List<Candidate> candsHt = engine.queryCandidates("ht", "<ht", 10);
    boolean hasHtmlOpen = false;
    boolean hasHtmlClose = false;
    boolean hasDocType = false;

    for (Candidate c : candsHt)
    {
      if ("<html>".equals(c.word)) hasHtmlOpen = true;
      if ("</html>".equals(c.word)) hasHtmlClose = true;
      if ("<!DOCTYPE html>".equals(c.word)) hasDocType = true;
    }

    assertTrue("<ht must suggest <html>", hasHtmlOpen);
    assertTrue("<ht must suggest </html>", hasHtmlClose);
    assertTrue("<ht must suggest <!DOCTYPE html>", hasDocType);

    // Query for closing tag "</di"
    List<Candidate> candsClose = engine.queryCandidates("di", "</di", 5);
    assertEquals("</div>", candsClose.get(0).word);
  }

  @Test
  public void testCodeKeywords()
  {
    DevSyntaxEngine engine = DevSyntaxEngine.instance();

    List<Candidate> candsPy = engine.queryCandidates("def", "", 5);
    boolean hasDef = false;
    for (Candidate c : candsPy)
    {
      if ("def".equals(c.word)) hasDef = true;
    }
    assertTrue("def should suggest python keyword", hasDef);

    List<Candidate> candsJs = engine.queryCandidates("con", "", 5);
    boolean hasConsole = false;
    for (Candidate c : candsJs)
    {
      if ("console.log()".equals(c.word)) hasConsole = true;
    }
    assertTrue("con should suggest console.log()", hasConsole);

    List<Candidate> candsCss = engine.queryCandidates("dis", "", 5);
    boolean hasDisplayFlex = false;
    for (Candidate c : candsCss)
    {
      if ("display: flex;".equals(c.word)) hasDisplayFlex = true;
    }
    assertTrue("dis should suggest display: flex;", hasDisplayFlex);

    List<Candidate> candsReact = engine.queryCandidates("useS", "", 5);
    boolean hasUseState = false;
    for (Candidate c : candsReact)
    {
      if ("useState()".equals(c.word)) hasUseState = true;
    }
    assertTrue("useS should suggest useState()", hasUseState);

    List<Candidate> candsJava = engine.queryCandidates("pub", "", 5);
    boolean hasPubClass = false;
    for (Candidate c : candsJava)
    {
      if ("public class".equals(c.word)) hasPubClass = true;
    }
    assertTrue("pub should suggest public class", hasPubClass);
  }

  @Test
  public void testAutocorrectDecisionForContractions()
  {
    Candidate topIm = new Candidate("I'm", Candidate.Source.AUTOCORRECT, 252, 0, 1.0f);
    topIm.compositeScore = 200.0;

    boolean shouldIm = AutocorrectDecision.shouldAutocorrect(
        "im", topIm, null, false, false, AutocorrectDecision.Sensitivity.BALANCED);
    assertTrue("im -> I'm should autocorrect on spacebar", shouldIm);

    // Common valid standalone words like "ill" should NOT be forced to "I'll" on space
    Candidate topIll = new Candidate("I'll", Candidate.Source.AUTOCORRECT, 250, 0, 1.0f);
    topIll.compositeScore = 190.0;
    boolean shouldIll = AutocorrectDecision.shouldAutocorrect(
        "ill", topIll, null, true, false, AutocorrectDecision.Sensitivity.BALANCED);
    assertFalse("ill -> I'll should not be forced on spacebar to protect the word ill", shouldIll);
  }

  @Test
  public void testBengaliFilter()
  {
    assertTrue(Suggestions.containsBengaliChar("ফাথের"));
    assertTrue(Suggestions.containsBengaliChar("আসসিংমেন্ট"));
    assertTrue(Suggestions.containsBengaliChar("কনোলwলেদগে"));
    assertFalse(Suggestions.containsBengaliChar("Father"));
    assertFalse(Suggestions.containsBengaliChar("Father's"));
    assertFalse(Suggestions.containsBengaliChar("assignment"));
    assertFalse(Suggestions.containsBengaliChar("3ʳᵈ"));
    assertFalse(Suggestions.containsBengaliChar("<div>"));
    assertFalse(Suggestions.containsBengaliChar("console.log()"));
  }

  @Test
  public void testDeveloperModeToggle()
  {
    Config conf = new Config();
    conf.editor_config.should_show_candidates_view = true;
    conf.developer_mode = false;

    Suggestions s = new Suggestions(new Suggestions.Callback() {
      @Override
      public void set_suggestions(Suggestions suggestions) {}
      @Override
      public CharSequence getTextBeforeCursor(int n, int flags) { return "<di"; }
    }, conf, null);
    s.started();

    // With developer_mode = false, <di should NOT produce HTML tags
    s.currently_typed_word("di");
    boolean hasTagWhenDevOff = false;
    for (int i = 0; i < s.count; i++)
    {
      if (s.suggestions[i] != null && s.suggestions[i].startsWith("<"))
      {
        hasTagWhenDevOff = true;
        break;
      }
    }
    assertFalse("HTML tags should NOT be suggested when developer_mode is OFF", hasTagWhenDevOff);

    // Turn developer_mode = true
    conf.developer_mode = true;
    s.currently_typed_word("di");
    boolean hasTagWhenDevOn = false;
    for (int i = 0; i < s.count; i++)
    {
      if (s.suggestions[i] != null && s.suggestions[i].startsWith("<"))
      {
        hasTagWhenDevOn = true;
        break;
      }
    }
    assertTrue("HTML tags MUST be suggested when developer_mode is ON", hasTagWhenDevOn);
  }

  @Test
  public void testNextWordPredictionAfterPickingWord()
  {
    NextWordPredictor predictor = NextWordPredictor.instance(null);

    // 1. After picking/typing "father "
    List<String> nextFather = predictor.predictFromSentence("im your father ", 8);
    assertFalse("Predictions after 'father' must not be empty", nextFather.isEmpty());
    assertTrue("Predictions after 'father' should contain natural verbs",
        nextFather.contains("is") || nextFather.contains("was") || nextFather.contains("said"));

    // 2. After picking/typing "assignment "
    List<String> nextAssignment = predictor.predictFromSentence("assignment ", 8);
    assertFalse("Predictions after 'assignment' must not be empty", nextAssignment.isEmpty());
    assertTrue("Predictions after 'assignment' should contain natural continuation",
        nextAssignment.contains("is") || nextAssignment.contains("was") || nextAssignment.contains("done") || nextAssignment.contains("due"));

    // 3. After picking/typing "help you "
    List<String> nextHelpYou = predictor.predictFromSentence("how can i help you ", 8);
    assertFalse("Predictions after 'help you' must not be empty", nextHelpYou.isEmpty());

    // 4. Bengali after picking "বাবা "
    List<String> nextBaba = predictor.predictFromSentence("বাবা ", 8);
    assertFalse("Predictions after 'বাবা' must not be empty", nextBaba.isEmpty());
    assertTrue("Predictions after 'বাবা' should contain verbs/connectives",
        nextBaba.contains("বললেন") || nextBaba.contains("কেমন") || nextBaba.contains("আছেন") || nextBaba.contains("এবং"));
  }

  @Test
  public void testShowInTerminals()
  {
    // 1. Termux editor info with TYPE_NULL
    EditorInfo termuxInfo = new EditorInfo();
    termuxInfo.inputType = InputType.TYPE_NULL;
    termuxInfo.packageName = "com.termux";

    // When show_in_terminals is false (default):
    assertFalse("Termux should not show suggestions when show_in_terminals is false",
        CandidatesView.should_show(termuxInfo, false));

    // When show_in_terminals is true:
    assertTrue("Termux MUST show suggestions when show_in_terminals is true",
        CandidatesView.should_show(termuxInfo, true));

    // 2. Standard text editor (e.g. WhatsApp / Notes)
    EditorInfo textInfo = new EditorInfo();
    textInfo.inputType = InputType.TYPE_CLASS_TEXT;
    textInfo.packageName = "com.whatsapp";

    assertTrue("Normal text editor should show suggestions regardless of terminal pref",
        CandidatesView.should_show(textInfo, false));
    assertTrue("Normal text editor should show suggestions",
        CandidatesView.should_show(textInfo, true));

    // 3. Password field should never show suggestions even in terminal setting
    EditorInfo passInfo = new EditorInfo();
    passInfo.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
    assertFalse("Password field must NEVER show suggestions",
        CandidatesView.should_show(passInfo, false));
    assertFalse("Password field must NEVER show suggestions",
        CandidatesView.should_show(passInfo, true));
  }

  @Test
  public void testShowInBrowserAndWebForms()
  {
    // 1. Browser Omnibox / URL address bar (e.g. Chrome, Brave, Firefox)
    // Browsers set TYPE_TEXT_VARIATION_URI and TYPE_TEXT_FLAG_NO_SUGGESTIONS
    EditorInfo browserUrlInfo = new EditorInfo();
    browserUrlInfo.inputType = InputType.TYPE_CLASS_TEXT
        | InputType.TYPE_TEXT_VARIATION_URI
        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    browserUrlInfo.packageName = "com.android.chrome";

    assertTrue("Browser URL bar MUST show candidate bar & toolbar",
        CandidatesView.should_show(browserUrlInfo, false));

    // 2. Web input field with autocomplete=off / search / incognito
    EditorInfo webInputInfo = new EditorInfo();
    webInputInfo.inputType = InputType.TYPE_CLASS_TEXT
        | InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT
        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    webInputInfo.packageName = "org.mozilla.firefox";

    assertTrue("Browser web input with NO_SUGGESTIONS flag MUST show candidate bar & toolbar",
        CandidatesView.should_show(webInputInfo, false));
  }

  @Test
  public void testRealisticConversationalPredictions()
  {
    NextWordPredictor predictor = NextWordPredictor.instance(null);

    // 1. English Conversational Trigrams & 4-grams
    List<String> howAreYou = predictor.predictFromSentence("how are you ", 5);
    assertTrue("how are you -> doing", howAreYou.contains("doing"));

    List<String> thankYouSo = predictor.predictFromSentence("thank you so ", 5);
    assertTrue("thank you so -> much", thankYouSo.contains("much"));

    List<String> sorryForThe = predictor.predictFromSentence("sorry for the ", 5);
    assertTrue("sorry for the -> delay", sorryForThe.contains("delay"));

    List<String> hopeYouAre = predictor.predictFromSentence("hope you are ", 5);
    assertTrue("hope you are -> doing well", hopeYouAre.contains("doing well"));

    // 2. Bengali Conversational Trigrams & 4-grams
    List<String> kiKorcho = predictor.predictFromSentence("কী করছ ", 5);
    assertTrue("কী করছ -> এখন / তুমি", kiKorcho.contains("এখন") || kiKorcho.contains("তুমি"));

    List<String> deriJonno = predictor.predictFromSentence("দেরি হওয়ার জন্য ", 5);
    assertTrue("দেরি হওয়ার জন্য -> দুঃখিত", deriJonno.contains("দুঃখিত"));

    List<String> ektuPor = predictor.predictFromSentence("একটু পর ", 5);
    assertTrue("একটু পর -> আসছি / ফোন দিচ্ছি", ektuPor.contains("আসছি") || ektuPor.contains("ফোন দিচ্ছি"));

    List<String> inshaAllah = predictor.predictFromSentence("ইনশা আল্লাহ সব ", 5);
    assertTrue("ইনশা আল্লাহ সব -> ঠিক হবে", inshaAllah.contains("ঠিক হবে"));

    List<String> alhamdulillah = predictor.predictFromSentence("আলহামদুলিল্লাহ আমি ", 5);
    assertTrue("আলহামদুলিল্লাহ আমি -> ভালো আছি", alhamdulillah.contains("ভালো আছি"));

    // 3. Sentence starters on empty input
    List<String> starters = predictor.predictFromSentence("", 8);
    assertFalse("Sentence starters must not be empty", starters.isEmpty());
    assertTrue("Starters should contain common opening words", starters.contains("I") || starters.contains("How") || starters.contains("আমি"));
  }

  @Test
  public void testContextAwareTypingPrefix()
  {
    Config conf = new Config();
    conf.editor_config.should_show_candidates_view = true;

    final String[] contextHolder = new String[] { "how can i h" };

    Suggestions s = new Suggestions(new Suggestions.Callback() {
      @Override
      public void set_suggestions(Suggestions suggestions) {}
      @Override
      public CharSequence getTextBeforeCursor(int n, int flags) { return contextHolder[0]; }
    }, conf, null);
    s.started();

    // Typing "h" after "how can i "
    s.currently_typed_word("h");
    assertNotNull("Top candidate should exist", s.top_candidate);
    assertEquals("Typing 'h' after 'how can i ' MUST rank 'help' as #1", "help", s.top_candidate.word.toLowerCase(java.util.Locale.ROOT));

    // Typing "m" after "thank you so "
    contextHolder[0] = "thank you so m";
    s.currently_typed_word("m");
    assertNotNull("Top candidate should exist", s.top_candidate);
    assertEquals("Typing 'm' after 'thank you so ' MUST rank 'much' as #1", "much", s.top_candidate.word.toLowerCase(java.util.Locale.ROOT));

    // Typing "আ" after "তুমি কেমন "
    contextHolder[0] = "তুমি কেমন আ";
    s.currently_typed_word("আ");
    assertNotNull("Top candidate should exist", s.top_candidate);
    assertTrue("Typing 'আ' after 'তুমি কেমন ' MUST rank 'আছো' as #1", s.top_candidate.word.startsWith("আছো"));
  }

  @Test
  public void testMagicAutocompleteAndFontScaling()
  {
    // 1. Verify AUTOCOMPLETE category in AiActionEngine
    assertTrue("AiActionEngine must contain AUTOCOMPLETE category",
        juloo.keyboard2.ai.AiActionEngine.Category.AUTOCOMPLETE != null);

    List<juloo.keyboard2.ai.AiActionEngine.ActionOption> opts =
        juloo.keyboard2.ai.AiActionEngine.getOptionsForCategory(juloo.keyboard2.ai.AiActionEngine.Category.AUTOCOMPLETE);
    assertNotNull(opts);
    assertFalse(opts.isEmpty());
    boolean hasContinue = false;
    for (juloo.keyboard2.ai.AiActionEngine.ActionOption opt : opts)
    {
      if ("auto_continue".equals(opt.id)) hasContinue = true;
    }
    assertTrue("AUTOCOMPLETE must include auto_continue option", hasContinue);

    String prompt = juloo.keyboard2.ai.AiActionEngine.buildSystemPrompt(
        juloo.keyboard2.ai.AiActionEngine.Category.AUTOCOMPLETE, "auto_continue", "default", null);
    assertTrue("System prompt must mention auto-complete", prompt.contains("auto-complete"));

    // 2. Verify Suggestion Font Scaling ratio
    Config conf = new Config();
    conf.keyboard_rows_height_pixels = 100;
    conf.key_vertical_margin = 0.1f;
    conf.characterSize = 0.4f;
    float row_height = conf.keyboard_rows_height_pixels * (1 - conf.key_vertical_margin);
    float standardKeySize = row_height * conf.characterSize * conf.labelTextSize;
    float scaledSuggestionSize = standardKeySize * 0.70f;
    assertTrue("Suggestion text size must be reduced (smaller than standard key label size)",
        scaledSuggestionSize < standardKeySize);
    assertEquals(standardKeySize * 0.70f, scaledSuggestionSize, 0.001f);
  }

  @Test
  public void testMagicWordPreservationWhenNoSpace()
  {
    final StringBuilder editorBuffer = new StringBuilder("কাজ করছি");
    final int[] deleteCount = new int[1];

    final android.view.inputmethod.InputConnection dummyIc = new android.view.inputmethod.BaseInputConnection(null, false)
    {
      @Override
      public CharSequence getTextBeforeCursor(int n, int flags)
      {
        return editorBuffer.toString();
      }

      @Override
      public boolean deleteSurroundingText(int beforeLength, int afterLength)
      {
        deleteCount[0] += beforeLength;
        if (beforeLength > 0 && editorBuffer.length() >= beforeLength)
        {
          editorBuffer.setLength(editorBuffer.length() - beforeLength);
        }
        return true;
      }

      @Override
      public boolean commitText(CharSequence text, int newCursorPosition)
      {
        editorBuffer.append(text);
        return true;
      }

      @Override public boolean beginBatchEdit() { return true; }
      @Override public boolean endBatchEdit() { return true; }
    };

    KeyEventHandler.IReceiver receiver = new KeyEventHandler.IReceiver()
    {
      @Override public void handle_event_key(KeyValue.Event ev) {}
      @Override public void set_shift_state(boolean state, boolean lock) {}
      @Override public void set_compose_pending(boolean pending) {}
      @Override public void selection_state_changed(boolean selection_is_ongoing) {}
      @Override public android.view.inputmethod.InputConnection getCurrentInputConnection() { return dummyIc; }
      @Override public android.os.Handler getHandler() { return null; }
      @Override public void set_suggestions(Suggestions suggestions) {}
      @Override public CharSequence getTextBeforeCursor(int n, int flags) { return editorBuffer.toString(); }
    };

    Config conf = new Config();
    Suggestions s = new Suggestions(receiver, conf, null);
    KeyEventHandler handler = new KeyEventHandler(receiver, s);
    handler.started(conf);

    // 1. User typed "কাজ করছি" with cursor directly after "করছি" (no space)
    handler._typedword.typed("করছি");

    // Enter magic suggestion: "🪄 একটু পর আসছি"
    handler.suggestion_entered("🪄 একটু পর আসছি");

    // "করছি" must NOT be deleted! (deleteCount must be 0)
    assertEquals("Preceding word 'করছি' must NOT be deleted", 0, deleteCount[0]);
    assertEquals("Full sentence must preserve 'কাজ করছি' and cleanly append completion with space",
        "কাজ করছি একটু পর আসছি ", editorBuffer.toString());

    // 2. User typed "কাজ কর" where "কর" is a prefix that completion finishes
    editorBuffer.setLength(0);
    editorBuffer.append("কাজ কর");
    deleteCount[0] = 0;
    handler._typedword.typed("কর");

    handler.suggestion_entered("🪄 করছি একটু পর");
    assertEquals("Prefix 'কর' should be replaced", 2, deleteCount[0]);
    assertEquals("Full sentence should be 'কাজ করছি একটু পর '",
        "কাজ করছি একটু পর ", editorBuffer.toString());
  }
}
