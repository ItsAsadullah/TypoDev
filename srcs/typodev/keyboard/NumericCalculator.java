package typodev.keyboard;

import java.math.BigDecimal;
import java.math.MathContext;

/** Bounded, decimal expression evaluator. Never evaluates executable code. */
public final class NumericCalculator {
  private final String expression;
  private int position;
  private NumericCalculator(String expression) { this.expression = expression; }
  public static String evaluate(String expression) {
    if (expression == null || expression.length() > 128) throw new IllegalArgumentException();
    NumericCalculator parser = new NumericCalculator(expression);
    BigDecimal result = parser.sum();
    if (parser.position != expression.length()) throw new IllegalArgumentException();
    String output = result.stripTrailingZeros().toPlainString();
    if (output.length() > 128) throw new IllegalArgumentException();
    return output;
  }
  private boolean take(char c) {
    if (position < expression.length() && expression.charAt(position) == c) { position++; return true; }
    return false;
  }
  private BigDecimal sum() {
    BigDecimal value = product();
    while (position < expression.length()) {
      if (take('+')) value = value.add(product(), MathContext.DECIMAL64);
      else if (take('-')) value = value.subtract(product(), MathContext.DECIMAL64);
      else break;
    }
    return value;
  }
  private BigDecimal product() {
    BigDecimal value = number();
    while (position < expression.length()) {
      if (take('*')) value = value.multiply(number(), MathContext.DECIMAL64);
      else if (take('/')) value = value.divide(number(), MathContext.DECIMAL64);
      else break;
    }
    return value;
  }
  private BigDecimal number() {
    if (take('-')) return number().negate();
    if (take('+')) return number();
    int start = position;
    while (position < expression.length()) {
      char c = expression.charAt(position);
      if ((c >= '0' && c <= '9') || c == '.') position++; else break;
    }
    if (start == position) throw new IllegalArgumentException();
    return new BigDecimal(expression.substring(start, position), MathContext.DECIMAL64);
  }
}
