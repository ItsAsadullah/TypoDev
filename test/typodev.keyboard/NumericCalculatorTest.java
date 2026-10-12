package typodev.keyboard;
import org.junit.Test;
import static org.junit.Assert.*;
public class NumericCalculatorTest {
  @Test public void arithmeticUsesPrecedenceAndDecimalPrecision() {
    assertEquals("14", NumericCalculator.evaluate("2+3*4"));
    assertEquals("0.3", NumericCalculator.evaluate("0.1+0.2"));
    assertEquals("-3", NumericCalculator.evaluate("5-8"));
    assertEquals("2.5", NumericCalculator.evaluate("10/4"));
    assertEquals("6", NumericCalculator.evaluate("3*-2+12"));
  }
  @Test public void invalidExpressionsNeverProduceAnInsertableResult() {
    for (String value : new String[]{"", "1+", "2..4", "10/0", "2a", "NaN"}) {
      try { NumericCalculator.evaluate(value); fail(value); }
      catch (IllegalArgumentException | ArithmeticException expected) { }
    }
  }
}
