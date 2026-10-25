package typodev.keyboard.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public class MathSolverResponseTest {
  @Test public void notebookActuallyMultipliesBeforeCollectingTerms() {
    MathSolverResponse result = MathSolverResponse.offline("(a+b)^2", MathSolverResponse.Mode.NOTEBOOK);
    assertNotNull(result);
    assertEquals("a² + 2ab + b²", result.finalAnswer);
    assertTrue(result.steps.contains("= (a + b)(a + b)"));
    assertTrue(result.steps.contains("= a(a + b) + b(a + b)"));
    assertTrue(result.steps.contains("= a² + ab + ab + b²"));
    assertTrue(result.steps.indexOf("a² + ab + ab + b²") < result.steps.lastIndexOf("a² + 2ab + b²"));
    assertTrue(result.fullText.contains("প্রমাণিত"));
  }
  @Test public void explanationAndNotebookAreDifferentAndMinusSignsAreCorrect() {
    MathSolverResponse explained = MathSolverResponse.offline("(a-b)²=", MathSolverResponse.Mode.EXPLAINED);
    MathSolverResponse notebook = MathSolverResponse.offline("(a-b)²=", MathSolverResponse.Mode.NOTEBOOK);
    assertEquals("a² - 2ab + b²", explained.finalAnswer);
    assertTrue(explained.steps.contains("a² - ab - ab + b²"));
    assertTrue(explained.steps.contains("১."));
    assertTrue(explained.steps.contains("ঋণাত্মক"));
    assertNotEquals(explained.steps, notebook.steps);
    assertNotNull(MathSolverResponse.offline("(ক+খ)২", MathSolverResponse.Mode.NOTEBOOK));
    for (String problem : new String[]{"(a+a)2", "(2+3)2", "(a+2b)/(a-2b)=", "2x+3=7"})
      assertNull(MathSolverResponse.offline(problem, MathSolverResponse.Mode.NOTEBOOK));
  }
  @Test public void promptsRequestTheSelectedStyleAndDoNotInventMissingData() {
    String notebook = MathSolverResponse.prompt("(a+2b)/(a-2b)=", MathSolverResponse.Mode.NOTEBOOK);
    String explained = MathSolverResponse.prompt("x+3=7", MathSolverResponse.Mode.EXPLAINED);
    assertTrue(notebook.contains("NOTEBOOK MODE"));
    assertTrue(notebook.contains("= a² + ab + ab + b²"));
    assertTrue(notebook.contains("Never invent missing values"));
    assertTrue(notebook.contains("denominator restrictions"));
    assertTrue(explained.contains("explain WHY"));
    assertTrue(explained.contains("numbered steps"));
  }
  @Test public void answerOnlyResponsesAreRejectedInsteadOfPretendingToShowSteps() {
    assertNull(MathSolverResponse.parse("### চূড়ান্ত উত্তর:\nx=2"));
    assertNull(MathSolverResponse.parse("x=2"));
    assertNull(MathSolverResponse.parse(null));
    MathSolverResponse r = MathSolverResponse.parse("### চূড়ান্ত উত্তর:\nx=2\n### সমাধান:\nx+1=3\n=2\n### সূত্র ও নিয়ম:\nবিয়োগ");
    assertNotNull(r);
    assertTrue(r.steps.contains("x+1=3"));
  }
}
