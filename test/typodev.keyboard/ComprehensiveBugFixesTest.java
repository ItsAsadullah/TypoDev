package typodev.keyboard;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import typodev.keyboard.ai.AiActionEngine;
import typodev.keyboard.ai.AiPromptBarView;
import typodev.keyboard.ai.GeminiAiService;
import typodev.keyboard.ai.MathSolverResponse;
import typodev.keyboard.clipboard.ClipboardItem;
import typodev.keyboard.fancy.FancyTextEngine;
import typodev.keyboard.snippet.Snippet;
import typodev.keyboard.snippet.SnippetCategory;
import typodev.keyboard.snippet.SnippetStore;
import typodev.keyboard.suggestions.AutocorrectDecision;
import typodev.keyboard.suggestions.AvroPhoneticParser;
import typodev.keyboard.suggestions.Candidate;
import typodev.keyboard.suggestions.EnglishGrammarEngine;
import typodev.keyboard.suggestions.UserVocabularyStore;
import typodev.keyboard.translate.TranslateBarView;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 34)
public class ComprehensiveBugFixesTest
{
  @Test
  public void inKeyboardEditTextConnectionReplacesComposingSpanAndDeletesSurrogatePairs()
  {
    Context ctx = RuntimeEnvironment.getApplication();
    EditText editText = new EditText(ctx);
    AtomicReference<String> submitted = new AtomicReference<>(null);
    InKeyboardEditTextConnection ic = new InKeyboardEditTextConnection(editText,
        () -> submitted.set(editText.getText().toString()));

    // Repeated Avro-style composing updates must replace, never duplicate
    ic.setComposingText("আ", 1);
    assertEquals("আ", editText.getText().toString());
    ic.setComposingText("আম", 1);
    assertEquals("আম", editText.getText().toString());
    ic.setComposingText("আমি", 1);
    assertEquals("আমি", editText.getText().toString());
    ic.finishComposingText();
    ic.commitText(" ", 1);

    // Surrogate-pair emoji deletion must delete full code point cleanly
    ic.commitText("😀", 1);
    assertEquals("আমি 😀", editText.getText().toString());
    ic.deleteSurroundingText(1, 0);
    assertEquals("আমি ", editText.getText().toString());

    // IME action triggers submit callback cleanly
    ic.performEditorAction(EditorInfo.IME_ACTION_GO);
    assertEquals("আমি ", submitted.get());
  }

  @Test
  public void aiPromptBarConfirmTriggeredByEditorActionWithoutDuplicatingComposingText()
  {
    Context ctx = RuntimeEnvironment.getApplication();
    AiPromptBarView bar = new AiPromptBarView(ctx);
    AtomicReference<String> confirmed = new AtomicReference<>(null);
    bar.setOnPromptActionListener(new AiPromptBarView.OnPromptActionListener()
    {
      @Override public void onConfirm(String promptText) { confirmed.set(promptText); }
      @Override public void onCancel() {}
    });

    InputConnection ic = bar.getPromptInputConnection();
    ic.setComposingText("ব", 1);
    ic.setComposingText("বা", 1);
    ic.setComposingText("বাংলা", 1);
    assertEquals("বাংলা", bar.getPromptText());

    ic.performEditorAction(EditorInfo.IME_ACTION_DONE);
    assertEquals("বাংলা", confirmed.get());
  }

  @Test
  public void translateBarSeparatesComposingFromBufferAndPreservesTargetSelectionOnOpen()
  {
    Context ctx = RuntimeEnvironment.getApplication();
    TranslateBarView bar = (TranslateBarView) LayoutInflater.from(ctx)
        .inflate(R.layout.translate_bar, null);

    List<String> targetCalls = new ArrayList<>();
    InputConnection targetIc = (InputConnection) Proxy.newProxyInstance(
        InputConnection.class.getClassLoader(),
        new Class<?>[] { InputConnection.class },
        (proxy, method, args) -> {
          if ("setComposingText".equals(method.getName()))
          {
            targetCalls.add("setComposingText:" + args[0]);
            return true;
          }
          if ("commitText".equals(method.getName()))
          {
            targetCalls.add("commitText:" + args[0]);
            return true;
          }
          if ("getSelectedText".equals(method.getName()))
          {
            return "selected host text";
          }
          return true;
        });

    Keyboard2 fakeKb = new Keyboard2()
    {
      @Override public InputConnection getTargetAppInputConnection() { return targetIc; }
    };
    bar.setKeyboard(fakeKb);

    // Opening TranslateBar must NOT wipe existing selected text in host editor
    bar.onTranslateOpened();
    assertTrue("onTranslateOpened must not clear host selection", targetCalls.isEmpty());

    // Repeated composing strokes in TranslateBar must replace _composingText cleanly
    InputConnection transIc = bar.createInputConnection(targetIc);
    transIc.setComposingText("আ", 1);
    transIc.setComposingText("আম", 1);
    transIc.setComposingText("আমি", 1);
    transIc.finishComposingText();

    EditText etInput = bar.findViewById(R.id.tv_translate_input);
    assertEquals("আমি", etInput.getText().toString());
  }

  @Test
  public void editorConfigAndAutocapitalisationHandleWordsAndCharactersModes()
  {
    Context ctx = RuntimeEnvironment.getApplication();
    EditorConfig ecWords = new EditorConfig();
    EditorInfo infoWords = new EditorInfo();
    infoWords.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS;
    ecWords.refresh(infoWords, ctx.getResources());
    assertEquals(TextUtils.CAP_MODE_WORDS, ecWords.caps_mode);

    Config conf = new Config();
    conf.autocapitalisation = true;
    conf.editor_config = ecWords;
    conf.editor_config.caps_initially_enabled = true;

    boolean[] shiftState = { false };
    InputConnection mockIc = (InputConnection) Proxy.newProxyInstance(
        InputConnection.class.getClassLoader(),
        new Class<?>[] { InputConnection.class },
        (proxy, method, args) -> {
          if ("getCursorCapsMode".equals(method.getName()))
          {
            return TextUtils.CAP_MODE_WORDS;
          }
          return true;
        });

    Autocapitalisation autocap = new Autocapitalisation(null, (enable, disable) -> {
      if (enable) shiftState[0] = true;
      else if (disable) shiftState[0] = false;
    });
    autocap.started(conf, mockIc);
    assertTrue(shiftState[0]);
    autocap.typed("a");
    assertFalse(shiftState[0]);
    autocap.typed(" ");
    assertTrue("CAP_MODE_WORDS should re-enable shift after space", shiftState[0]);
  }

  @Test
  public void currentlyTypedWordKeepsZwnjAndZwjInsideBanglaWords()
  {
    Config conf = new Config();
    conf.editor_config.initial_sel_start = 0;
    conf.editor_config.initial_sel_end = 0;
    CurrentlyTypedWord word = new CurrentlyTypedWord(null, () -> {});
    InputConnection emptyIc = (InputConnection) Proxy.newProxyInstance(
        InputConnection.class.getClassLoader(),
        new Class<?>[] { InputConnection.class },
        (proxy, method, args) -> {
          if ("getTextBeforeCursor".equals(method.getName()) || "getTextAfterCursor".equals(method.getName()))
            return "";
          return null;
        });
    word.started(conf, emptyIc);
    word.typed("র");
    word.typed("\u200D"); // ZWJ
    word.typed("্য");
    word.typed("াব");
    assertEquals("র\u200D্যাব", word.get());

    word.typed(" ");
    word.typed("ক");
    word.typed("\u200C"); // ZWNJ
    word.typed("্ষ");
    assertEquals("ক\u200C্ষ", word.get());
  }

  @Test
  public void algebraFormulaEngineFixesTrinomialSignsAndBinomialCoefficients()
  {
    AlgebraFormulaEngine.AlgebraResult tri = AlgebraFormulaEngine.evaluateInline("(a-b-c)^2");
    assertNotNull(tri);
    assertEquals("a² + b² + c² - 2ab + 2bc - 2ca", tri.result);

    AlgebraFormulaEngine.AlgebraResult bin = AlgebraFormulaEngine.evaluateInline("(2x+3)(2x+5)");
    assertNotNull(bin);
    assertEquals("4x² + 16x + 15", bin.result);

    AlgebraFormulaEngine.AlgebraResult binVars = AlgebraFormulaEngine.evaluateInline("(x+a)(x+b)");
    assertNotNull(binVars);
    assertTrue(binVars.result.contains("x²"));
    assertTrue(binVars.result.contains("ab"));
  }

  @Test
  public void aiSubsystemFixesOfflineBanglaRegexStripEmojisAndAskAiGrounding() throws Exception
  {
    // 1. MathSolverResponse.offline with Bengali letters must not throw PatternSyntaxException
    MathSolverResponse offline = MathSolverResponse.offline("(ক+খ)²", MathSolverResponse.Mode.NOTEBOOK);
    assertNotNull(offline);
    assertTrue(offline.finalAnswer.contains("ক²"));
    assertTrue(offline.finalAnswer.contains("খ²"));

    // 2. GeminiAiService.stripEmojis preserves line breaks while removing emojis & variation selectors
    String multiline = "1. First item 🎉\n2. Second item ❤️\uFE0F\n3. Done 🚀";
    String stripped = GeminiAiService.stripEmojis(multiline);
    assertEquals("1. First item\n2. Second item\n3. Done", stripped);

    // 3. GeminiAiService.extractCandidateText skips Gemini 2.5 thought parts
    JSONObject candidate = new JSONObject()
        .put("content", new JSONObject()
            .put("parts", new JSONArray()
                .put(new JSONObject().put("thought", true).put("text", "internal chain of thought"))
                .put(new JSONObject().put("text", "Actual final answer"))));
    assertEquals("Actual final answer", GeminiAiService.extractCandidateText(candidate));

    // 4. AiActionEngine standalone ASK_AI omits edit-only GROUNDING_RULES and REWRITE includes select_tone
    String standalonePrompt = AiActionEngine.buildSystemPrompt(
        AiActionEngine.Category.ASK_AI, "custom", "Neutral", "Write a poem about rain", false, false);
    assertFalse(standalonePrompt.contains("Rewrite, fix, translate, summarize, or format ONLY the input text"));

    boolean hasSelectTone = false;
    for (AiActionEngine.ActionOption opt : AiActionEngine.getOptionsForCategory(AiActionEngine.Category.REWRITE))
    {
      if ("select_tone".equals(opt.id))
      {
        hasSelectTone = true;
        break;
      }
    }
    assertTrue("REWRITE category must expose select_tone option", hasSelectTone);
  }

  @Test
  public void avroAutocorrectOrdinalsAndRejectionPenaltiesBehaveCorrectly()
  {
    // 1. Avro capital O kar vs standalone O, and khanda-ta (ৎ) never taking hasanta (ৎ্)
    assertEquals("কো", AvroPhoneticParser.parse("kO"));
    assertEquals("লোক", AvroPhoneticParser.parse("lOk"));
    assertEquals("ও", AvroPhoneticParser.parse("O"));
    String khanda = AvroPhoneticParser.parse("hothat^");
    assertFalse("Khanda-ta must never form ৎ্", khanda.contains("ৎ্"));

    // 2. AutocorrectDecision protects valid short English words and pure prefix completions
    Candidate idContraction = new Candidate("I'd", Candidate.Source.AUTOCORRECT, 252, 0, 1.0f);
    assertFalse(AutocorrectDecision.shouldAutocorrect(
        "id", idContraction, null, true, false, AutocorrectDecision.Sensitivity.BALANCED));

    Candidate shellContraction = new Candidate("she'll", Candidate.Source.AUTOCORRECT, 252, 0, 1.0f);
    assertFalse(AutocorrectDecision.shouldAutocorrect(
        "shell", shellContraction, null, true, false, AutocorrectDecision.Sensitivity.BALANCED));

    Candidate prefixBoosted = new Candidate("help", Candidate.Source.AUTOCORRECT, 250, 0, 1.0f);
    assertFalse(AutocorrectDecision.shouldAutocorrect(
        "h", prefixBoosted, null, false, false, AutocorrectDecision.Sensitivity.BALANCED));

    Candidate imContraction = new Candidate("I'm", Candidate.Source.AUTOCORRECT, 252, 0, 1.0f);
    assertTrue(AutocorrectDecision.shouldAutocorrect(
        "im", imContraction, null, false, false, AutocorrectDecision.Sensitivity.BALANCED));

    // 3. EnglishGrammarEngine ordinals (2rd -> 2nd, 11st -> 11th, 12nd -> 12th, 13rd -> 13th)
    assertTrue(containsCandidateWord(EnglishGrammarEngine.instance().queryCandidates("2rd", 5), "2nd"));
    assertTrue(containsCandidateWord(EnglishGrammarEngine.instance().queryCandidates("11st", 5), "11th"));
    assertTrue(containsCandidateWord(EnglishGrammarEngine.instance().queryCandidates("12nd", 5), "12th"));
    assertTrue(containsCandidateWord(EnglishGrammarEngine.instance().queryCandidates("13rd", 5), "13th"));

    // 4. UserVocabularyStore tracks rejection penalty even for unlearned dictionary words
    UserVocabularyStore vocab = new UserVocabularyStore(null);
    vocab.recordRejection("unwantedword");
    assertTrue(vocab.getRejectionPenalty("unwantedword") > 0f);
    assertFalse(vocab.containsWord("unwantedword"));
    assertEquals(0, vocab.getWordCount());
  }

  @Test
  public void snippetsClipboardDeduplicationAndFancyTextFixesWork() throws Exception
  {
    // 1. Snippet.create generates strictly unique IDs in tight loops
    Set<Long> ids = new HashSet<>();
    for (int i = 0; i < 20; i++)
    {
      Snippet s = Snippet.create("/s" + i, "exp" + i, "Name" + i, SnippetCategory.GENERAL);
      assertTrue("Snippet IDs must be strictly unique", ids.add(s.id));
    }

    Context ctx = RuntimeEnvironment.getApplication();
    SnippetStore store = SnippetStore.instance(ctx);
    store.seedDefaults();
    assertTrue("All 3 default snippets must survive seeding", store.getAll().size() >= 3);

    // 2. ClipboardHistoryService deduplicates non-consecutive duplicates
    Field globalField = Config.class.getDeclaredField("_globalConfig");
    globalField.setAccessible(true);
    Config prev = (Config) globalField.get(null);
    try
    {
      Config cfg = new Config();
      cfg.clipboard_history_enabled = true;
      cfg.clipboard_history_duration = 30;
      globalField.set(null, cfg);

      ClipboardHistoryService history = new ClipboardHistoryService(null);
      history.add_clip("first");
      history.add_clip("second");
      history.add_clip("first");
      List<ClipboardItem> items = history.clear_expired_and_get_history_items();
      assertEquals(2, items.size());
      assertEquals("first", items.get(0).text);
      assertEquals("second", items.get(1).text);
    }
    finally
    {
      globalField.set(null, prev);
    }

    // 3. FancyTextEngine mappings & surrogate-pair preservation in upside_down
    assertEquals("🅼", FancyTextEngine.applyStyle("neg_squared", "M"));
    assertEquals("ᶠᵠ", FancyTextEngine.applyStyle("superscript", "FQ"));
    String upside = FancyTextEngine.applyStyle("upside_down", "Hi 😀");
    assertTrue("Surrogate-pair emoji must remain intact in upside_down", upside.contains("😀"));
  }

  private static boolean containsCandidateWord(List<Candidate> list, String target)
  {
    for (Candidate c : list)
    {
      if (target.equals(c.word)) return true;
    }
    return false;
  }
}
