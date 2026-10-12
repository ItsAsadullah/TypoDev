package typodev.keyboard;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import typodev.keyboard.suggestions.ArabicSuggestionEngine;
import typodev.keyboard.suggestions.AutocorrectDecision;
import typodev.keyboard.suggestions.Candidate;

public class ArabicSuggestionEngineTest {
  private boolean has(String prefix, String expected) {
    for (Candidate c : ArabicSuggestionEngine.queryCandidates(prefix, 20)) if (expected.equals(c.word)) return true;
    return false;
  }
  @Test public void religiousAndCommonPhrases() {
    assertTrue(has("بسم", "بسم الله الرحمن الرحيم"));
    assertTrue(has("الله", "الله أكبر"));
    assertTrue(has("محمد", "محمد رسول الله ﷺ"));
    assertTrue(has("لا", "لا إله إلا الله"));
    assertTrue(has("السلام", "السلام عليكم"));
    assertTrue(has("شكرا", "شكرًا"));
  }
  @Test public void ligatureAndDiacritics() {
    assertTrue(has("صلى", "ﷺ"));
    assertTrue(has("صَلَّى", "ﷺ"));
    assertTrue(has("بِسْم", "بسم الله الرحمن الرحيم"));
    assertTrue(has("أستغفر", "أستغفر الله"));
  }
  @Test public void noForcedExpansionOrEnglishLeak() {
    assertTrue(ArabicSuggestionEngine.queryCandidates("hello", 6).isEmpty());
    assertTrue(ArabicSuggestionEngine.queryCandidates("ب", 6).isEmpty());
    assertTrue(ArabicSuggestionEngine.queryCandidates(null, 6).isEmpty());
    assertEquals(1, ArabicSuggestionEngine.queryCandidates("بسم", 1).size());
    for (Candidate c : ArabicSuggestionEngine.queryCandidates("صلى", 6)) {
      c.compositeScore = 200;
      assertFalse(AutocorrectDecision.shouldAutocorrect("صلى", c, null, false, false,
          AutocorrectDecision.Sensitivity.BALANCED));
    }
  }
  @Test public void arabicPcDigitsAndNumpad() throws Exception {
    String xml = new String(Files.readAllBytes(Paths.get("srcs/layouts/arab_pc.xml")), java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(xml.contains("numpad_script=\"hindu-arabic\""));
    for (int i = 0; i < 10; i++) {
      assertTrue(xml.contains("key2=\"" + (char)(0x660 + i) + "\""));
      assertFalse(xml.contains("key2=\"" + i + "\""));
    }
  }
}
