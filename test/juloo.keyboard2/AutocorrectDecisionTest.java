package juloo.keyboard2;

import juloo.keyboard2.suggestions.AutocorrectDecision;
import juloo.keyboard2.suggestions.Candidate;
import org.junit.Test;
import static org.junit.Assert.*;

public class AutocorrectDecisionTest
{
  @Test
  public void testValidWordNeverAutocorrected()
  {
    // If the user typed "this", it should never be autocorrected
    Candidate top = new Candidate("those", Candidate.Source.BUILTIN_MAIN, 200, 1, 0.8f);
    top.compositeScore = 150.0;

    boolean should = AutocorrectDecision.shouldAutocorrect(
        "this", top, null, true, false, AutocorrectDecision.Sensitivity.BALANCED);

    assertFalse("Valid typed word must not be autocorrected", should);
  }

  @Test
  public void testPrefixCompletionNotAutocorrectedOnSpace()
  {
    // User typed "th", top candidate is "the" (prefix completion)
    Candidate top = new Candidate("the", Candidate.Source.BUILTIN_MAIN, 255, 0, 0.66f);
    top.compositeScore = 180.0;

    boolean should = AutocorrectDecision.shouldAutocorrect(
        "th", top, null, false, false, AutocorrectDecision.Sensitivity.BALANCED);

    assertFalse("Prefix completion should not be forced on spacebar", should);
  }

  @Test
  public void testProperNounsProtected()
  {
    // Capitalized name "Asadullah"
    Candidate top = new Candidate("assault", Candidate.Source.BUILTIN_MAIN, 150, 1, 0.8f);
    top.compositeScore = 140.0;

    boolean should = AutocorrectDecision.shouldAutocorrect(
        "Asadullah", top, null, false, true, AutocorrectDecision.Sensitivity.BALANCED);

    assertFalse("Proper noun must not be autocorrected", should);
  }

  @Test
  public void testHighConfidenceTypoCorrected()
  {
    // Obvious typo: "teh" -> "the"
    Candidate top = new Candidate("the", Candidate.Source.BUILTIN_MAIN, 255, 1, 1.0f);
    top.compositeScore = 160.0; // High score

    Candidate second = new Candidate("ten", Candidate.Source.BUILTIN_MAIN, 50, 1, 1.0f);
    second.compositeScore = 100.0; // Margin is 60.0 > 30.0 required

    boolean should = AutocorrectDecision.shouldAutocorrect(
        "teh", top, second, false, false, AutocorrectDecision.Sensitivity.BALANCED);

    assertTrue("High confidence typo should be autocorrected", should);
  }

  @Test
  public void testBanglishNotForcedOnSpace()
  {
    // User typed "ami", Banglish candidate is "আমি"
    Candidate top = new Candidate("আমি", Candidate.Source.BANGLISH, 240, 0, 1.0f);
    top.compositeScore = 200.0;

    boolean should = AutocorrectDecision.shouldAutocorrect(
        "ami", top, null, false, false, AutocorrectDecision.Sensitivity.BALANCED);

    assertFalse("Banglish transliteration must be tap-to-select, never forced on space", should);
  }

  @Test
  public void testSensitivityOffDisablesAutocorrect()
  {
    Candidate top = new Candidate("the", Candidate.Source.BUILTIN_MAIN, 255, 1, 1.0f);
    top.compositeScore = 200.0;

    boolean should = AutocorrectDecision.shouldAutocorrect(
        "teh", top, null, false, false, AutocorrectDecision.Sensitivity.OFF);

    assertFalse("Sensitivity OFF must disable autocorrect", should);
  }
}
