package typodev.keyboard.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public class MathTextFormatterTest {
  @Test public void screenshotAnswerIsReadableWithoutLatex() {
    String raw = "$a = 43 - 2b$ অথবা $b = \\frac{43 - a}{2}$";
    assertEquals("a = 43 - 2b অথবা b = (43 - a)/2", MathTextFormatter.format(raw));
    assertEquals("2b = 43 - a\nb = (43 - a)/2",
        MathTextFormatter.format("$$2b = 43 - a$$\n$$b = \\frac{43 - a}{2}$$"));
  }

  @Test public void nestedFractionsRootsAndPowersPreserveMeaning() {
    assertEquals("x² + y³ = √(a² + b²)",
        MathTextFormatter.format("\\(x^2 + y^{3} = \\sqrt{a^2 + b^2}\\)"));
    assertEquals("(1 + x)/((a + b)/2)", MathTextFormatter.format("\\frac{1 + x}{\\frac{a + b}{2}}"));
    assertEquals("∛(8) = 2; π × r²", MathTextFormatter.format("\\sqrt[3]{8} = 2; \\pi \\times r^2"));
    assertEquals("x₁ ≤ x₂", MathTextFormatter.format("x_1 \\leq x_2"));
    assertEquals("x^(a+b)", MathTextFormatter.format("x^{a+b}"));
    assertEquals("x⁻² + y¹²; 90°; 50%", MathTextFormatter.format("x^-2 + y^12; 90^\\circ; 50\\%"));
  }

  @Test public void plainTextMalformedInputAndCurrencyAreNotLost() {
    assertEquals("", MathTextFormatter.format(null));
    assertEquals("খরচ $5; ২+২=৪", MathTextFormatter.format("খরচ $5; ২+২=৪"));
    assertEquals("$5", MathTextFormatter.format("\\$5"));
    assertEquals("\\frac{a}", MathTextFormatter.format("\\frac{a}"));
    assertEquals("\\unknown{x}", MathTextFormatter.format("\\unknown{x}"));
    assertEquals("উত্তর\n1. x = 2", MathTextFormatter.format("### উত্তর\n1. **x = 2**"));
  }

  @Test public void dialogFormatsAnswerStepsAndCopiedSolutionConsistently() {
    MathSolverResponse result = MathSolverResponse.parse(
        "### চূড়ান্ত উত্তর:\n$b = \\frac{43-a}{2}$\n\n"
        + "### ধাপে ধাপে সমাধান:\n1. **বিয়োগ করি:** $2b = 43-a$\n"
        + "### সূত্র ও নিয়ম:\n$x^2 \\geq 0$");
    assertEquals("b = (43-a)/2", result.finalAnswer);
    assertEquals("1. বিয়োগ করি: 2b = 43-a", result.steps);
    assertEquals("x² ≥ 0", result.formulas);
    assertTrue(result.fullText.contains(result.finalAnswer));
    assertFalse(result.fullText.contains("$"));
    assertFalse(result.fullText.contains("\\frac"));
    assertFalse(result.fullText.contains("###"));
  }
}
