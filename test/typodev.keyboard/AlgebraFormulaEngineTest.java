package typodev.keyboard;

import org.junit.Test;
import static org.junit.Assert.*;

public class AlgebraFormulaEngineTest
{
  @Test
  public void equalsRequiresContentOnItsLeftInCurrentClause()
  {
    for (String text : new String[]{"=", "  =  ", "===", "=x", "= 23", "+ - =",
        "solve x\n=", "a+b=43\n  =", "x=2; =", "সমাধান করো। =", "x=2\n"})
    {
      assertFalse(text, AlgebraFormulaEngine.isMathProblem(text));
      assertNull(text, AlgebraFormulaEngine.extractMathProblem(text));
    }
    for (String text : new String[]{"x=", "a+b=43", "২ক+৩=", "(a+b)^2="})
    {
      assertTrue(text, AlgebraFormulaEngine.isMathProblem(text));
      assertEquals(text, AlgebraFormulaEngine.extractMathProblem(text));
    }
    assertEquals("x=", AlgebraFormulaEngine.extractMathProblem("old question\nx="));
    assertNull(AlgebraFormulaEngine.evaluateInline("=(a+b)^2"));
    assertNull(AlgebraFormulaEngine.evaluateInline("solve x\n=(a+b)^2"));
  }

  @Test
  public void testSquareOfBinomialSum()
  {
    AlgebraFormulaEngine.AlgebraResult r1 = AlgebraFormulaEngine.evaluateDirect("(a+b)2");
    assertNotNull(r1);
    assertEquals("a\u00B2 + 2ab + b\u00B2", r1.expansion);
    assertEquals("(a + b)\u00B2 = a\u00B2 + 2ab + b\u00B2", r1.equation);
    assertFalse(r1.steps.isEmpty());

    AlgebraFormulaEngine.AlgebraResult r2 = AlgebraFormulaEngine.evaluateDirect("(a+b)^2");
    assertNotNull(r2);
    assertEquals("a\u00B2 + 2ab + b\u00B2", r2.expansion);

    AlgebraFormulaEngine.AlgebraResult r3 = AlgebraFormulaEngine.evaluateDirect("(x+y)\u00B2");
    assertNotNull(r3);
    assertEquals("x\u00B2 + 2xy + y\u00B2", r3.expansion);
  }

  @Test
  public void testSquareOfBinomialDifference()
  {
    AlgebraFormulaEngine.AlgebraResult r = AlgebraFormulaEngine.evaluateDirect("(a-b)2");
    assertNotNull(r);
    assertEquals("a\u00B2 - 2ab + b\u00B2", r.expansion);
    assertEquals("(a - b)\u00B2 = a\u00B2 - 2ab + b\u00B2", r.equation);
  }

  @Test
  public void testWithCoefficients()
  {
    AlgebraFormulaEngine.AlgebraResult r1 = AlgebraFormulaEngine.evaluateDirect("(2x+3)2");
    assertNotNull(r1);
    assertEquals("4x\u00B2 + 12x + 9", r1.expansion);

    AlgebraFormulaEngine.AlgebraResult r2 = AlgebraFormulaEngine.evaluateDirect("(x+5)2");
    assertNotNull(r2);
    assertEquals("x\u00B2 + 10x + 25", r2.expansion);
  }

  @Test
  public void testDifferenceOfSquares()
  {
    AlgebraFormulaEngine.AlgebraResult r1 = AlgebraFormulaEngine.evaluateDirect("(a+b)(a-b)");
    assertNotNull(r1);
    assertEquals("a\u00B2 - b\u00B2", r1.expansion);
    assertEquals("(a + b)(a - b) = a\u00B2 - b\u00B2", r1.equation);

    AlgebraFormulaEngine.AlgebraResult r2 = AlgebraFormulaEngine.evaluateDirect("a\u00B2-b\u00B2");
    assertNotNull(r2);
    assertEquals("(a + b)(a - b)", r2.expansion);
  }

  @Test
  public void testCubeOfBinomial()
  {
    AlgebraFormulaEngine.AlgebraResult r1 = AlgebraFormulaEngine.evaluateDirect("(a+b)3");
    assertNotNull(r1);
    assertEquals("a\u00B3 + 3a\u00B2b + 3ab\u00B2 + b\u00B3", r1.expansion);

    AlgebraFormulaEngine.AlgebraResult r2 = AlgebraFormulaEngine.evaluateDirect("(a-b)3");
    assertNotNull(r2);
    assertEquals("a\u00B3 - 3a\u00B2b + 3ab\u00B2 - b\u00B3", r2.expansion);
  }

  @Test
  public void testTrinomialSquare()
  {
    AlgebraFormulaEngine.AlgebraResult r = AlgebraFormulaEngine.evaluateDirect("(a+b+c)2");
    assertNotNull(r);
    assertEquals("a\u00B2 + b\u00B2 + c\u00B2 + 2ab + 2bc + 2ca", r.expansion);
  }

  @Test
  public void testBengaliAlgebra()
  {
    AlgebraFormulaEngine.AlgebraResult r1 = AlgebraFormulaEngine.evaluateDirect("(ক+খ)২");
    assertNotNull(r1);
    assertEquals("ক\u00B2 + ২কখ + খ\u00B2", r1.expansion);

    AlgebraFormulaEngine.AlgebraResult r2 = AlgebraFormulaEngine.evaluateDirect("(ক-খ)২");
    assertNotNull(r2);
    assertEquals("ক\u00B2 - ২কখ + খ\u00B2", r2.expansion);
  }

  @Test
  public void testInlineEvaluationInSentence()
  {
    AlgebraFormulaEngine.AlgebraResult r = AlgebraFormulaEngine.evaluateInline("আজকে আমরা ক্লাসে (a+b)2");
    assertNotNull(r);
    assertEquals("(a+b)2", r.matchedText);
    assertEquals("a\u00B2 + 2ab + b\u00B2", r.expansion);
  }

  @Test
  public void testMathProblemDetection()
  {
    assertTrue(AlgebraFormulaEngine.isMathProblem("(a+b)2"));
    assertTrue(AlgebraFormulaEngine.isMathProblem("2x + 5 = 15"));
    assertTrue(AlgebraFormulaEngine.isMathProblem("একটি ত্রিভুজের ক্ষেত্রফল কত?"));
    assertTrue(AlgebraFormulaEngine.isMathProblem("solve 3x^2 - 12 = 0"));
    assertTrue(AlgebraFormulaEngine.isMathProblem("sqrt(144) ="));

    assertFalse(AlgebraFormulaEngine.isMathProblem("hello"));
    assertFalse(AlgebraFormulaEngine.isMathProblem("আমি ভালো আছি"));
    assertFalse(AlgebraFormulaEngine.isMathProblem("how are you?"));
    assertFalse(AlgebraFormulaEngine.isMathProblem("kemon acho?"));
    assertFalse(AlgebraFormulaEngine.isMathProblem("what is your name?"));
    assertFalse(AlgebraFormulaEngine.isMathProblem("কোন লাভ নেই"));
    assertFalse(AlgebraFormulaEngine.isMathProblem("অনেক ক্ষতি হয়েছে"));
    assertFalse(AlgebraFormulaEngine.isMathProblem("can you solve my issue"));
  }
}
