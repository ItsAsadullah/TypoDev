package typodev.keyboard;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Bounded, decimal expression evaluator. Never evaluates executable code. */
public final class NumericCalculator {
  private static final int MAX_EXPRESSION_LENGTH = 128;
  private static final int MAX_EXPONENT = 1000;
  private static final int INLINE_MAX_SCALE = 8;
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  /** Result of evaluating a trailing inline math expression from editor text. */
  public static final class InlineResult {
    /** Exact substring at the end of the editor text that was matched (e.g. "২+৪" or "2 + 4 ="). */
    public final String matchedText;
    /** Compact normalized expression without spaces or trailing '=' (e.g. "২+৪" or "2+4"). */
    public final String compactExpression;
    /** Evaluated result formatted in matching numerals (e.g. "৬" or "6"). */
    public final String result;
    /** Full equation string (e.g. "২+৪=৬" or "2+4=6"). */
    public final String equation;

    public InlineResult(String matchedText, String compactExpression, String result, String equation) {
      this.matchedText = matchedText;
      this.compactExpression = compactExpression;
      this.result = result;
      this.equation = equation;
    }
  }

  private static final class TermVal {
    final BigDecimal value;
    final boolean directPercent;

    TermVal(BigDecimal value, boolean directPercent) {
      this.value = value;
      this.directPercent = directPercent;
    }
  }

  private final String expression;
  private int position;
  private boolean hasBinaryOrPowerOp;
  private boolean hasPostfixPercent;

  private NumericCalculator(String expression) {
    this.expression = expression;
  }

  public static String evaluate(String expression) {
    if (expression == null) throw new IllegalArgumentException();
    String trimmed = expression.trim();
    if (trimmed.isEmpty() || trimmed.length() > MAX_EXPRESSION_LENGTH) {
      throw new IllegalArgumentException();
    }
    boolean useBengaliDigits = containsBengaliDigits(trimmed);
    NumericCalculator parser = new NumericCalculator(trimmed);
    TermVal res = parser.sum();
    parser.skipWhitespace();
    if (parser.position != trimmed.length()) throw new IllegalArgumentException();
    BigDecimal value = normalizeZero(res.value);
    String output = value.stripTrailingZeros().toPlainString();
    if (output.length() > MAX_EXPRESSION_LENGTH) throw new IllegalArgumentException();
    return useBengaliDigits ? toBengaliDigits(output) : output;
  }

  /**
   * Inspects text before the cursor and evaluates a trailing math expression if present.
   * Returns {@code null} when the text does not end with a complete calculation.
   */
  public static InlineResult evaluateInline(String textBeforeCursor) {
    if (textBeforeCursor == null || textBeforeCursor.isEmpty()) return null;

    int len = textBeforeCursor.length();
    int end = len;

    // Trim trailing horizontal spaces only if preceded by '='
    while (end > 0 && (textBeforeCursor.charAt(end - 1) == ' ' || textBeforeCursor.charAt(end - 1) == '\t')) {
      end--;
    }
    if (end == 0) return null;

    boolean hasTrailingEquals = false;
    if (textBeforeCursor.charAt(end - 1) == '=') {
      // Do not trigger on comparison operators like ==, !=, <=, >=
      if (end >= 2) {
        char beforeEq = textBeforeCursor.charAt(end - 2);
        if (beforeEq == '=' || beforeEq == '!' || beforeEq == '<' || beforeEq == '>') {
          return null;
        }
      }
      hasTrailingEquals = true;
      end--;
      while (end > 0 && (textBeforeCursor.charAt(end - 1) == ' ' || textBeforeCursor.charAt(end - 1) == '\t')) {
        end--;
      }
    } else if (end < len) {
      // User already typed a trailing space after a number/word without '='
      return null;
    }

    if (end == 0) return null;

    // Scan backwards over candidate math characters
    int scanLimit = Math.max(0, end - MAX_EXPRESSION_LENGTH);
    int start = end;
    while (start > scanLimit && isMathScanChar(textBeforeCursor.charAt(start - 1))) {
      start--;
    }

    // Advance past leading spaces
    while (start < end && (textBeforeCursor.charAt(start) == ' ' || textBeforeCursor.charAt(start) == '\t')) {
      start++;
    }
    if (start >= end) return null;

    // If two numbers are separated by whitespace with no operator between them (e.g. "12 2+4"),
    // advance start to the last contiguous math segment.
    start = advancePastDisconnectedWhitespace(textBeforeCursor, start, end);

    // Strip any leading unmatched '(' or bullet/separator operators (e.g. "- 2+4" or "* 2+4")
    start = advancePastLeadingNonUnaryOrUnmatched(textBeforeCursor, start, end);

    while (start < end && (textBeforeCursor.charAt(start) == ' ' || textBeforeCursor.charAt(start) == '\t')) {
      start++;
    }
    if (start >= end) return null;

    // If attached directly to a preceding word, identifier, or URL path (e.g. "abc2+4" or "http://a/1/2"),
    // reject so identifiers and URLs never trigger calculator chips.
    if (!isValidBoundaryBefore(textBeforeCursor, start)) {
      return null;
    }

    String exprPart = textBeforeCursor.substring(start, end);
    if (exprPart.isEmpty() || exprPart.length() > MAX_EXPRESSION_LENGTH) return null;

    try {
      NumericCalculator parser = new NumericCalculator(exprPart);
      TermVal evaluated = parser.sum();
      parser.skipWhitespace();
      if (parser.position != exprPart.length()) return null;

      // Require an actual calculation (binary/power operator, or postfix '%' with '=').
      if (!parser.hasBinaryOrPowerOp && !(parser.hasPostfixPercent && hasTrailingEquals)) {
        return null;
      }

      BigDecimal val = normalizeZero(evaluated.value);
      if (val.scale() > INLINE_MAX_SCALE) {
        val = normalizeZero(val.setScale(INLINE_MAX_SCALE, RoundingMode.HALF_UP));
      }

      String plainResult = val.stripTrailingZeros().toPlainString();
      if (plainResult.isEmpty() || plainResult.length() > MAX_EXPRESSION_LENGTH) return null;

      boolean useBengali = containsBengaliDigits(exprPart);
      String formattedResult = useBengali ? toBengaliDigits(plainResult) : plainResult;
      String compactExpr = compactExpression(exprPart);
      if (compactExpr.isEmpty() || compactExpr.equals(formattedResult)) return null;

      String matchedText = textBeforeCursor.substring(start, len);
      String equation = compactExpr + "=" + formattedResult;
      return new InlineResult(matchedText, compactExpr, formattedResult, equation);
    } catch (ArithmeticException | IllegalArgumentException ignored) {
      return null;
    }
  }

  private static BigDecimal normalizeZero(BigDecimal value) {
    return (value.compareTo(BigDecimal.ZERO) == 0) ? BigDecimal.ZERO : value;
  }

  private static boolean isMathScanChar(char c) {
    return isAsciiDigit(c)
        || isBengaliDigit(c)
        || c == '.'
        || isOperatorChar(c)
        || c == '('
        || c == ')'
        || c == ' '
        || c == '\t';
  }

  private static boolean isOperatorChar(char c) {
    return c == '+'
        || c == '-'
        || c == '\u2212'
        || c == '*'
        || c == '\u00D7'
        || c == '\u2715'
        || c == '/'
        || c == '\u00F7'
        || c == '%'
        || c == '^';
  }

  private static boolean isAsciiDigit(char c) {
    return c >= '0' && c <= '9';
  }

  public static boolean isBengaliDigit(char c) {
    return c >= '\u09E6' && c <= '\u09EF';
  }

  private static boolean isAnyDigit(char c) {
    return isAsciiDigit(c) || isBengaliDigit(c);
  }

  public static boolean containsBengaliDigits(String s) {
    if (s == null) return false;
    for (int i = 0; i < s.length(); i++) {
      if (isBengaliDigit(s.charAt(i))) return true;
    }
    return false;
  }

  public static String toBengaliDigits(String s) {
    if (s == null || s.isEmpty()) return s;
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c >= '0' && c <= '9') {
        sb.append((char) ('\u09E6' + (c - '0')));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static int advancePastDisconnectedWhitespace(String text, int start, int end) {
    int lastValidStart = start;
    int i = start;
    while (i < end) {
      char c = text.charAt(i);
      if (c == ' ' || c == '\t') {
        int wsStart = i;
        while (i < end && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
          i++;
        }
        if (wsStart > lastValidStart && i < end) {
          char left = text.charAt(wsStart - 1);
          char right = text.charAt(i);
          boolean leftEndsValue = isAnyDigit(left) || left == '.' || left == ')' || left == '%';
          boolean rightStartsValue = isAnyDigit(right) || right == '.' || right == '(';
          if (leftEndsValue && rightStartsValue) {
            lastValidStart = i;
          }
        }
      } else {
        i++;
      }
    }
    return lastValidStart;
  }

  private static int advancePastLeadingNonUnaryOrUnmatched(String text, int start, int end) {
    boolean changed = true;
    while (changed && start < end) {
      changed = false;
      while (start < end && (text.charAt(start) == ' ' || text.charAt(start) == '\t')) {
        start++;
        changed = true;
      }
      if (start >= end) break;
      char c = text.charAt(start);
      // Non-unary operators at the very start (e.g. "* 2+4" or "/ 2+4")
      if (c == '*' || c == '\u00D7' || c == '\u2715' || c == '/' || c == '\u00F7' || c == '%' || c == '^' || c == ')') {
        start++;
        changed = true;
        continue;
      }
      // Dash or plus followed by whitespace (e.g. bullet list "- 2+4" or "হিসাব - ২+৪")
      if ((c == '-' || c == '\u2212' || c == '+')
          && start + 1 < end
          && (text.charAt(start + 1) == ' ' || text.charAt(start + 1) == '\t')) {
        start++;
        changed = true;
        continue;
      }
      // Leading unmatched '('
      if (c == '(') {
        int balance = 0;
        for (int i = start; i < end; i++) {
          char ch = text.charAt(i);
          if (ch == '(') balance++;
          else if (ch == ')') balance--;
        }
        if (balance > 0) {
          start++;
          changed = true;
        }
      }
    }
    return start;
  }

  private static boolean isValidBoundaryBefore(String text, int start) {
    if (start <= 0) return true;
    char prev = text.charAt(start - 1);
    if (Character.isLetterOrDigit(prev)
        || prev == '_'
        || prev == '/'
        || prev == '.'
        || prev == '@'
        || prev == '#') {
      return false;
    }
    return true;
  }

  private static String compactExpression(String expr) {
    StringBuilder sb = new StringBuilder(expr.length());
    for (int i = 0; i < expr.length(); i++) {
      char c = expr.charAt(i);
      if (c != ' ' && c != '\t') {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private void skipWhitespace() {
    while (position < expression.length()) {
      char c = expression.charAt(position);
      if (c == ' ' || c == '\t') position++;
      else break;
    }
  }

  private boolean take(char c) {
    skipWhitespace();
    if (position < expression.length() && expression.charAt(position) == c) {
      position++;
      return true;
    }
    return false;
  }

  private TermVal sum() {
    TermVal left = product();
    BigDecimal value = left.value;
    boolean leftIsPercent = left.directPercent;

    while (true) {
      skipWhitespace();
      if (take('+')) {
        hasBinaryOrPowerOp = true;
        TermVal right = product();
        if (right.directPercent && !leftIsPercent) {
          value = value.add(value.multiply(right.value, MathContext.DECIMAL64), MathContext.DECIMAL64);
        } else {
          value = value.add(right.value, MathContext.DECIMAL64);
        }
        leftIsPercent = false;
      } else if (take('-') || take('\u2212')) {
        hasBinaryOrPowerOp = true;
        TermVal right = product();
        if (right.directPercent && !leftIsPercent) {
          value = value.subtract(value.multiply(right.value, MathContext.DECIMAL64), MathContext.DECIMAL64);
        } else {
          value = value.subtract(right.value, MathContext.DECIMAL64);
        }
        leftIsPercent = false;
      } else {
        break;
      }
    }
    return new TermVal(value, leftIsPercent);
  }

  private TermVal product() {
    TermVal first = power();
    BigDecimal value = first.value;
    boolean isDirectPercent = first.directPercent;

    while (true) {
      if (isImplicitMultiplyNext()) {
        hasBinaryOrPowerOp = true;
        TermVal rhs = power();
        value = value.multiply(rhs.value, MathContext.DECIMAL64);
        isDirectPercent = false;
        continue;
      }
      skipWhitespace();
      if (take('*') || take('\u00D7') || take('\u2715')) {
        hasBinaryOrPowerOp = true;
        TermVal rhs = power();
        value = value.multiply(rhs.value, MathContext.DECIMAL64);
        isDirectPercent = false;
      } else if (take('/') || take('\u00F7')) {
        hasBinaryOrPowerOp = true;
        TermVal rhs = power();
        if (rhs.value.compareTo(BigDecimal.ZERO) == 0) {
          throw new ArithmeticException("Division by zero");
        }
        value = value.divide(rhs.value, MathContext.DECIMAL64);
        isDirectPercent = false;
      } else if (isBinaryModuloNext()) {
        take('%');
        hasBinaryOrPowerOp = true;
        TermVal rhs = power();
        if (rhs.value.compareTo(BigDecimal.ZERO) == 0) {
          throw new ArithmeticException("Modulo by zero");
        }
        value = value.remainder(rhs.value, MathContext.DECIMAL64);
        isDirectPercent = false;
      } else {
        break;
      }
    }
    return new TermVal(value, isDirectPercent);
  }

  private boolean isImplicitMultiplyNext() {
    if (position >= expression.length()) return false;
    char next = expression.charAt(position);
    if (next == '(') {
      return true;
    }
    if (position > 0 && expression.charAt(position - 1) == ')' && isAnyDigit(next)) {
      return true;
    }
    return false;
  }

  private boolean isBinaryModuloNext() {
    skipWhitespace();
    if (position >= expression.length() || expression.charAt(position) != '%') return false;
    int lookahead = position + 1;
    while (lookahead < expression.length()
        && (expression.charAt(lookahead) == ' ' || expression.charAt(lookahead) == '\t')) {
      lookahead++;
    }
    if (lookahead >= expression.length()) return false;
    char next = expression.charAt(lookahead);
    return isAnyDigit(next) || next == '.' || next == '(';
  }

  private TermVal power() {
    TermVal base = unary();
    skipWhitespace();
    if (take('^')) {
      hasBinaryOrPowerOp = true;
      TermVal expTerm = power();
      BigDecimal exp = expTerm.value.stripTrailingZeros();
      if (exp.scale() > 0) {
        throw new IllegalArgumentException("Non-integer exponent");
      }
      int n;
      try {
        n = exp.intValueExact();
      } catch (ArithmeticException e) {
        throw new IllegalArgumentException("Exponent out of range");
      }
      if (Math.abs(n) > MAX_EXPONENT) {
        throw new IllegalArgumentException("Exponent too large");
      }
      if (n < 0) {
        if (base.value.compareTo(BigDecimal.ZERO) == 0) {
          throw new ArithmeticException("Zero to negative power");
        }
        BigDecimal posPow = base.value.pow(-n, MathContext.DECIMAL64);
        return new TermVal(BigDecimal.ONE.divide(posPow, MathContext.DECIMAL64), false);
      }
      return new TermVal(base.value.pow(n, MathContext.DECIMAL64), false);
    }
    return base;
  }

  private TermVal unary() {
    skipWhitespace();
    if (take('-') || take('\u2212')) {
      TermVal inner = unary();
      return new TermVal(inner.value.negate(), inner.directPercent);
    }
    if (take('+')) {
      return unary();
    }
    return postfix();
  }

  private TermVal postfix() {
    BigDecimal value = primary();
    boolean directPercent = false;
    while (true) {
      skipWhitespace();
      if (position < expression.length() && expression.charAt(position) == '%' && !isBinaryModuloNext()) {
        position++;
        value = value.divide(ONE_HUNDRED, MathContext.DECIMAL64);
        directPercent = true;
        hasPostfixPercent = true;
      } else {
        break;
      }
    }
    return new TermVal(value, directPercent);
  }

  private BigDecimal primary() {
    skipWhitespace();
    if (take('(')) {
      TermVal inner = sum();
      if (!take(')')) {
        throw new IllegalArgumentException("Missing closing parenthesis");
      }
      return inner.value;
    }
    int start = position;
    StringBuilder asciiNum = new StringBuilder();
    int dotCount = 0;
    int digitCount = 0;
    while (position < expression.length()) {
      char c = expression.charAt(position);
      if (isAsciiDigit(c)) {
        asciiNum.append(c);
        digitCount++;
        position++;
      } else if (isBengaliDigit(c)) {
        asciiNum.append((char) ('0' + (c - '\u09E6')));
        digitCount++;
        position++;
      } else if (c == '.') {
        dotCount++;
        if (dotCount > 1) throw new IllegalArgumentException("Multiple decimal points");
        asciiNum.append('.');
        position++;
      } else {
        break;
      }
    }
    if (start == position || digitCount == 0) {
      throw new IllegalArgumentException("Expected number");
    }
    return new BigDecimal(asciiNum.toString(), MathContext.DECIMAL64);
  }
}
