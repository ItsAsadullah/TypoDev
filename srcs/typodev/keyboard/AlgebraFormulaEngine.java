package typodev.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognizes and expands algebraic formulas, mathematical identities,
 * and detects math problems for step-by-step AI solving.
 */
public final class AlgebraFormulaEngine
{
  public static class AlgebraResult
  {
    public final String matchedText;
    public final String expansion;
    public final String equation;
    public final List<String> steps;

    public AlgebraResult(String matchedText, String expansion, String equation, List<String> steps)
    {
      this.matchedText = matchedText;
      this.expansion = expansion;
      this.equation = equation;
      this.steps = (steps != null) ? steps : Collections.emptyList();
    }
  }

  // Regex patterns for binomial squares/cubes: (u + v)2, (u - v)^2, (u + v)², (ক+খ)২ etc.
  private static final Pattern BINOMIAL_POWER_PATTERN = Pattern.compile(
      "^\\s*\\(([a-zA-Z0-9\u0980-\u09FF]+)\\s*([+\\-\u2212])\\s*([a-zA-Z0-9\u0980-\u09FF]+)\\)\\s*(\\^?[23]|[\u00B2\u00B3\u09E8\u09E9]|\\^?[\u09E8\u09E9])\\s*(=?)\\s*$"
  );

  // Difference of squares / binomial product: (u + v)(u - v) or (x + a)(x + b)
  private static final Pattern TWO_BRACKETS_PATTERN = Pattern.compile(
      "^\\s*\\(([a-zA-Z0-9\u0980-\u09FF]+)\\s*([+\\-\u2212])\\s*([a-zA-Z0-9\u0980-\u09FF]+)\\)\\s*\\(([a-zA-Z0-9\u0980-\u09FF]+)\\s*([+\\-\u2212])\\s*([a-zA-Z0-9\u0980-\u09FF]+)\\)\\s*(=?)\\s*$"
  );

  // Trinomial square: (u + v + w)2, (u + v + w)^2, etc.
  private static final Pattern TRINOMIAL_SQUARE_PATTERN = Pattern.compile(
      "^\\s*\\(([a-zA-Z0-9\u0980-\u09FF]+)\\s*([+\\-\u2212])\\s*([a-zA-Z0-9\u0980-\u09FF]+)\\s*([+\\-\u2212])\\s*([a-zA-Z0-9\u0980-\u09FF]+)\\)\\s*(\\^?2|[\u00B2\u09E8]|\\^?[\u09E8])\\s*(=?)\\s*$"
  );

  // Factoring: a^2 - b^2, a² - b²
  private static final Pattern DIFF_OF_SQUARES_FACTOR_PATTERN = Pattern.compile(
      "^\\s*([a-zA-Z\u0980-\u09FF]+)\\s*(?:\\^2|\u00B2|2|\u09E8)\\s*([\\-\u2212])\\s*([a-zA-Z\u0980-\u09FF]+)\\s*(?:\\^2|\u00B2|2|\u09E8)\\s*(=?)\\s*$"
  );

  // Unambiguous Bengali Math keywords that indicate a problem
  private static final String[] BN_MATH_KEYWORDS = {
      "মান কত", "ক্ষেত্রফল", "পরিসীমা", "উৎপাদক", "বর্গমূল", "ঘনমূল",
      "লসাগু", "গসাগু", "শতকরা", "ত্রিভুজ",
      "সমাধান করো", "মান নির্ণয়", "সমীকরণ", "জ্যামিতি", "পিথাগোরাস",
      "ব্যাসার্ধ", "কর্ণ", "আয়তক্ষেত্র", "বর্গক্ষেত্র", "উৎপাদকে বিশ্লেষণ"
  };

  // Context-dependent Bengali words that require numbers or question markers
  private static final String[] BN_CONTEXT_MATH_KEYWORDS = {
      "লাভ", "ক্ষতি", "সুদ", "কোণ", "বৃত্ত"
  };

  // Specific English Math keywords
  private static final String[] EN_MATH_KEYWORDS = {
      "factorize", "roots of", "derivative", "integral", "area of",
      "perimeter", "hypotenuse", "equation", "algebra", "radius",
      "triangle", "polynomial"
  };

  // Action verbs that require math symbols/digits in the clause
  private static final Pattern EN_ACTION_VERB_PATTERN = Pattern.compile(
      "\\b(solve|simplify|evaluate|calculate)\\b"
  );

  private AlgebraFormulaEngine() {}

  /**
   * Evaluates text before cursor to find any recognized algebraic formula.
   */
  public static AlgebraResult evaluateInline(String textBefore)
  {
    if (textBefore == null || textBefore.trim().isEmpty()) return null;
    if (hasEmptyEqualsLeftSide(currentClause(textBefore))) return null;

    int len = textBefore.length();
    int searchStart = Math.max(0, len - 80);
    String tail = textBefore.substring(searchStart);

    // Look for matching formula candidate from end of string
    for (int i = 0; i < tail.length(); i++)
    {
      if (i > 0)
      {
        char prev = tail.charAt(i - 1);
        if (Character.isLetterOrDigit(prev) && prev != ' ' && prev != '\n') continue;
      }

      String candidate = tail.substring(i);
      AlgebraResult res = evaluateDirect(candidate);
      if (res != null)
      {
        return res;
      }
    }

    return null;
  }

  /**
   * Attempts to match and expand an algebraic formula directly.
   */
  public static AlgebraResult evaluateDirect(String input)
  {
    if (input == null) return null;
    String trimmed = input.trim();
    if (trimmed.isEmpty()) return null;

    // 1. Binomial Power: (u + v)2, (u - v)^2, (u + v)3, etc.
    Matcher mBinomial = BINOMIAL_POWER_PATTERN.matcher(trimmed);
    if (mBinomial.matches())
    {
      String u = mBinomial.group(1);
      String op = mBinomial.group(2);
      String v = mBinomial.group(3);
      String powerStr = mBinomial.group(4);
      boolean isCube = powerStr.contains("3") || powerStr.contains("\u00B3") || powerStr.contains("\u09E9");
      boolean isMinus = "-".equals(op) || "\u2212".equals(op);
      boolean isBengali = isBengaliScript(u) || isBengaliScript(v) || isBengaliScript(powerStr);

      if (!isCube)
      {
        // (u ± v)² = u² ± 2uv + v²
        return expandBinomialSquare(trimmed, u, v, isMinus, isBengali);
      }
      else
      {
        // (u ± v)³ = u³ ± 3u²v + 3uv² ± v³
        return expandBinomialCube(trimmed, u, v, isMinus, isBengali);
      }
    }

    // 2. Two Brackets: (u + v)(u - v) or (x + a)(x + b)
    Matcher mTwoBrackets = TWO_BRACKETS_PATTERN.matcher(trimmed);
    if (mTwoBrackets.matches())
    {
      String u1 = mTwoBrackets.group(1);
      String op1 = mTwoBrackets.group(2);
      String v1 = mTwoBrackets.group(3);
      String u2 = mTwoBrackets.group(4);
      String op2 = mTwoBrackets.group(5);
      String v2 = mTwoBrackets.group(6);

      boolean isBengali = isBengaliScript(u1) || isBengaliScript(v1);

      // (u + v)(u - v) => u² - v²
      if (u1.equals(u2) && v1.equals(v2) && !op1.equals(op2))
      {
        return expandDifferenceOfSquares(trimmed, u1, v1, isBengali);
      }

      // (x + a)(x + b) => x² + (a+b)x + ab
      if (u1.equals(u2))
      {
        return expandBinomialProductCommonX(trimmed, u1, v1, op1, v2, op2, isBengali);
      }
    }

    // 3. Trinomial Square: (u + v + w)2
    Matcher mTrinomial = TRINOMIAL_SQUARE_PATTERN.matcher(trimmed);
    if (mTrinomial.matches())
    {
      String u = mTrinomial.group(1);
      String op1 = mTrinomial.group(2);
      String v = mTrinomial.group(3);
      String op2 = mTrinomial.group(4);
      String w = mTrinomial.group(5);
      boolean isBengali = isBengaliScript(u) || isBengaliScript(v) || isBengaliScript(w);
      return expandTrinomialSquare(trimmed, u, op1, v, op2, w, isBengali);
    }

    // 4. Difference of squares factoring: a^2 - b^2 => (a+b)(a-b)
    Matcher mDiffSquares = DIFF_OF_SQUARES_FACTOR_PATTERN.matcher(trimmed);
    if (mDiffSquares.matches())
    {
      String a = mDiffSquares.group(1);
      String b = mDiffSquares.group(3);
      String exp = "(" + a + " + " + b + ")(" + a + " - " + b + ")";
      String eq = a + "\u00B2 - " + b + "\u00B2 = " + exp;
      List<String> steps = new ArrayList<>();
      steps.add("১. সূত্র: A\u00B2 - B\u00B2 = (A + B)(A - B)");
      steps.add("২. এখানে A = " + a + " এবং B = " + b);
      steps.add("৩. উৎপাদকে বিশ্লেষণ: (" + a + " + " + b + ")(" + a + " - " + b + ")");
      return new AlgebraResult(trimmed, exp, eq, steps);
    }

    return null;
  }

  private static AlgebraResult expandBinomialSquare(String matched, String u, String v, boolean isMinus, boolean isBengali)
  {
    ParsedTerm t1 = parseTerm(u);
    ParsedTerm t2 = parseTerm(v);

    String sq1 = squareTerm(t1, isBengali);
    String mid = multiplyTerms(t1, t2, 2, isBengali);
    String sq2 = squareTerm(t2, isBengali);

    String sign = isMinus ? " - " : " + ";
    String expansion = sq1 + sign + mid + " + " + sq2;

    String uDisplay = "(" + u + (isMinus ? " - " : " + ") + v + ")\u00B2";
    String equation = uDisplay + " = " + expansion;

    List<String> steps = new ArrayList<>();
    String formula = isMinus
        ? (isBengali ? "(A - B)\u00B2 = A\u00B2 - ২AB + B\u00B2" : "(A - B)\u00B2 = A\u00B2 - 2AB + B\u00B2")
        : (isBengali ? "(A + B)\u00B2 = A\u00B2 + ২AB + B\u00B2" : "(A + B)\u00B2 = A\u00B2 + 2AB + B\u00B2");
    steps.add("১. প্রযোজ্য বীজগণিতীয় সূত্র: " + formula);
    steps.add("২. এখানে প্রথম পদ A = " + u + " এবং দ্বিতীয় পদ B = " + v);
    steps.add("৩. মান বসিয়ে পাই: (" + u + ")\u00B2" + sign + (isBengali ? "২·(" : "2·(") + u + ")·(" + v + ") + (" + v + ")\u00B2");
    steps.add("৪. হিসাব সম্পন্ন করে চূড়ান্ত রূপ: " + expansion);

    return new AlgebraResult(matched, expansion, equation, steps);
  }

  private static AlgebraResult expandBinomialCube(String matched, String u, String v, boolean isMinus, boolean isBengali)
  {
    ParsedTerm t1 = parseTerm(u);
    ParsedTerm t2 = parseTerm(v);

    String cb1 = cubeTerm(t1, isBengali);
    String mid1 = multiplyCubeMidTerm(t1, t2, true, 3, isBengali);  // 3 * a^2 * b
    String mid2 = multiplyCubeMidTerm(t1, t2, false, 3, isBengali); // 3 * a * b^2
    String cb2 = cubeTerm(t2, isBengali);

    String expansion;
    if (!isMinus)
    {
      expansion = cb1 + " + " + mid1 + " + " + mid2 + " + " + cb2;
    }
    else
    {
      expansion = cb1 + " - " + mid1 + " + " + mid2 + " - " + cb2;
    }

    String uDisplay = "(" + u + (isMinus ? " - " : " + ") + v + ")\u00B3";
    String equation = uDisplay + " = " + expansion;

    List<String> steps = new ArrayList<>();
    String formula = isMinus
        ? (isBengali ? "(A - B)\u00B3 = A\u00B3 - ৩A\u00B2B + ৩AB\u00B2 - B\u00B3" : "(A - B)\u00B3 = A\u00B3 - 3A\u00B2B + 3AB\u00B2 - B\u00B3")
        : (isBengali ? "(A + B)\u00B3 = A\u00B3 + ৩A\u00B2B + ৩AB\u00B2 + B\u00B3" : "(A + B)\u00B3 = A\u00B3 + 3A\u00B2B + 3AB\u00B2 + B\u00B3");
    steps.add("১. প্রযোজ্য বীজগণিতীয় সূত্র: " + formula);
    steps.add("২. এখানে A = " + u + " এবং B = " + v);
    steps.add("৩. মান বসিয়ে হিসাব করে পাই: " + expansion);

    return new AlgebraResult(matched, expansion, equation, steps);
  }

  private static AlgebraResult expandDifferenceOfSquares(String matched, String u, String v, boolean isBengali)
  {
    ParsedTerm t1 = parseTerm(u);
    ParsedTerm t2 = parseTerm(v);

    String sq1 = squareTerm(t1, isBengali);
    String sq2 = squareTerm(t2, isBengali);
    String expansion = sq1 + " - " + sq2;
    String equation = "(" + u + " + " + v + ")(" + u + " - " + v + ") = " + expansion;

    List<String> steps = new ArrayList<>();
    steps.add("১. সূত্র: (A + B)(A - B) = A\u00B2 - B\u00B2");
    steps.add("২. এখানে A = " + u + " এবং B = " + v);
    steps.add("৩. গুণফল নির্ণয়: (" + u + ")\u00B2 - (" + v + ")\u00B2 = " + expansion);

    return new AlgebraResult(matched, expansion, equation, steps);
  }

  private static AlgebraResult expandBinomialProductCommonX(String matched, String x, String a, String opA, String b, String opB, boolean isBengali)
  {
    ParsedTerm tx = parseTerm(x);
    ParsedTerm ta = parseTerm(a);
    ParsedTerm tb = parseTerm(b);

    long valA = "-".equals(opA) || "\u2212".equals(opA) ? -ta.coeff : ta.coeff;
    long valB = "-".equals(opB) || "\u2212".equals(opB) ? -tb.coeff : tb.coeff;

    String xSq = squareTerm(tx, isBengali);
    long sumAB = valA + valB;
    long prodAB = valA * valB;

    StringBuilder sb = new StringBuilder(xSq);
    if (sumAB != 0)
    {
      if (sumAB > 0) sb.append(" + ");
      else sb.append(" - ");
      long absSum = Math.abs(sumAB);
      if (absSum != 1 || tx.vars.isEmpty())
      {
        sb.append(isBengali ? toBengaliNumber(absSum) : absSum);
      }
      sb.append(tx.vars);
    }

    if (prodAB != 0)
    {
      if (prodAB > 0) sb.append(" + ");
      else sb.append(" - ");
      long absProd = Math.abs(prodAB);
      sb.append(isBengali ? toBengaliNumber(absProd) : absProd);
    }

    String expansion = sb.toString();
    String equation = "(" + x + (valA >= 0 ? "+" : "") + valA + ")(" + x + (valB >= 0 ? "+" : "") + valB + ") = " + expansion;

    List<String> steps = new ArrayList<>();
    steps.add("১. সূত্র: (x + a)(x + b) = x\u00B2 + (a + b)x + ab");
    steps.add("২. এখানে a = " + valA + " এবং b = " + valB);
    steps.add("৩. হিসাব: " + x + "\u00B2 + (" + valA + " + " + valB + ")" + x + " + (" + valA + " \u00D7 " + valB + ")");
    steps.add("৪. চূড়ান্ত ফলাফল: " + expansion);

    return new AlgebraResult(matched, expansion, equation, steps);
  }

  private static AlgebraResult expandTrinomialSquare(String matched, String u, String op1, String v, String op2, String w, boolean isBengali)
  {
    String sq1 = u + "\u00B2";
    String sq2 = v + "\u00B2";
    String sq3 = w + "\u00B2";

    String signV = ("-".equals(op1) || "\u2212".equals(op1)) ? "-" : "+";
    String signW = ("-".equals(op2) || "\u2212".equals(op2)) ? "-" : "+";

    String two = isBengali ? "২" : "2";
    String expansion = sq1 + " + " + sq2 + " + " + sq3 + " + " + two + u + v + " + " + two + v + w + " + " + two + w + u;
    String equation = "(" + u + " " + op1 + " " + v + " " + op2 + " " + w + ")\u00B2 = " + expansion;

    List<String> steps = new ArrayList<>();
    steps.add("১. সূত্র: (A + B + C)\u00B2 = A\u00B2 + B\u00B2 + C\u00B2 + 2AB + 2BC + 2CA");
    steps.add("২. সূত্রে A=" + u + ", B=" + v + ", C=" + w + " বসিয়ে হিসাব সম্পন্ন করা হয়েছে।");

    return new AlgebraResult(matched, expansion, equation, steps);
  }

  // Term parsing: e.g. "2x" => coeff=2, vars="x"
  private static class ParsedTerm
  {
    final long coeff;
    final String vars;
    ParsedTerm(long coeff, String vars) { this.coeff = coeff; this.vars = vars; }
  }

  private static ParsedTerm parseTerm(String t)
  {
    t = t.trim();
    if (t.isEmpty()) return new ParsedTerm(1, "");

    StringBuilder numSb = new StringBuilder();
    StringBuilder varSb = new StringBuilder();

    for (int i = 0; i < t.length(); i++)
    {
      char c = t.charAt(i);
      if (c >= '0' && c <= '9')
      {
        numSb.append(c);
      }
      else if (c >= '\u09E6' && c <= '\u09EF')
      {
        numSb.append((char) ('0' + (c - '\u09E6')));
      }
      else
      {
        varSb.append(t.substring(i));
        break;
      }
    }

    long coeff = 1;
    if (numSb.length() > 0)
    {
      try { coeff = Long.parseLong(numSb.toString()); }
      catch (Throwable ignored) { coeff = 1; }
    }
    return new ParsedTerm(coeff, varSb.toString());
  }

  private static String squareTerm(ParsedTerm t, boolean isBengali)
  {
    long c2 = t.coeff * t.coeff;
    String numPart = (c2 == 1 && !t.vars.isEmpty()) ? "" : (isBengali ? toBengaliNumber(c2) : String.valueOf(c2));
    String varPart = t.vars.isEmpty() ? "" : t.vars + "\u00B2";
    return numPart + varPart;
  }

  private static String cubeTerm(ParsedTerm t, boolean isBengali)
  {
    long c3 = t.coeff * t.coeff * t.coeff;
    String numPart = (c3 == 1 && !t.vars.isEmpty()) ? "" : (isBengali ? toBengaliNumber(c3) : String.valueOf(c3));
    String varPart = t.vars.isEmpty() ? "" : t.vars + "\u00B3";
    return numPart + varPart;
  }

  private static String multiplyTerms(ParsedTerm t1, ParsedTerm t2, long factor, boolean isBengali)
  {
    long totalCoeff = factor * t1.coeff * t2.coeff;
    String vars = t1.vars + t2.vars;
    String numPart = (totalCoeff == 1 && !vars.isEmpty()) ? "" : (isBengali ? toBengaliNumber(totalCoeff) : String.valueOf(totalCoeff));
    return numPart + vars;
  }

  private static String multiplyCubeMidTerm(ParsedTerm t1, ParsedTerm t2, boolean squareFirst, long factor, boolean isBengali)
  {
    long totalCoeff = factor * (squareFirst ? (t1.coeff * t1.coeff * t2.coeff) : (t1.coeff * t2.coeff * t2.coeff));
    String var1 = t1.vars.isEmpty() ? "" : (squareFirst ? t1.vars + "\u00B2" : t1.vars);
    String var2 = t2.vars.isEmpty() ? "" : (squareFirst ? t2.vars : t2.vars + "\u00B2");
    String vars = var1 + var2;
    String numPart = (totalCoeff == 1 && !vars.isEmpty()) ? "" : (isBengali ? toBengaliNumber(totalCoeff) : String.valueOf(totalCoeff));
    return numPart + vars;
  }

  private static boolean isBengaliScript(String s)
  {
    if (s == null) return false;
    for (int i = 0; i < s.length(); i++)
    {
      char c = s.charAt(i);
      if (c >= '\u0980' && c <= '\u09FF') return true;
    }
    return false;
  }

  private static String toBengaliNumber(long n)
  {
    String s = String.valueOf(n);
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++)
    {
      char c = s.charAt(i);
      if (c >= '0' && c <= '9') sb.append((char) ('\u09E6' + (c - '0')));
      else sb.append(c);
    }
    return sb.toString();
  }

  /**
   * Determines if the text appears to be a math problem or expression
   * suitable for step-by-step AI solving.
   */
  public static boolean isMathProblem(String text)
  {
    if (text == null) return false;
    String trimmed = currentClause(text).trim();
    // A bare '=' must never revive a problem from earlier text. Require an
    // actual operand on its left, including for '=x' and repeated '=' input.
    int equals = trimmed.indexOf('=');
    if (hasEmptyEqualsLeftSide(trimmed)) return false;
    if (trimmed.length() < 3 && equals < 0) return false;

    // 1. Direct algebra formula
    if (evaluateDirect(trimmed) != null) return true;

    // 2. Equation with variables or numbers (e.g., 2x + 5 = 15, x^2 - 4 = 0, x=)
    if (trimmed.contains("=") && (trimmed.matches(".*[0-9a-zA-Z\u0980-\u09FF]+.*")))
    {
      return true;
    }

    boolean hasMathTokens = trimmed.matches(".*[0-9+\\-*/\u00D7\u00F7^()\u00B2\u00B3\u09E6-\u09EF=%].*");

    // 3. Question with explicit math digits/operators ending with ?
    if (trimmed.endsWith("?") && hasMathTokens)
    {
      return true;
    }

    // 4. Bengali math keywords
    for (String kw : BN_MATH_KEYWORDS)
    {
      if (trimmed.contains(kw)) return true;
    }
    if (hasMathTokens || trimmed.endsWith("?") || trimmed.contains("কত") || trimmed.contains("নির্ণয়"))
    {
      for (String kw : BN_CONTEXT_MATH_KEYWORDS)
      {
        if (trimmed.contains(kw)) return true;
      }
    }

    // 5. English math keywords
    String lower = trimmed.toLowerCase(Locale.ROOT);
    for (String kw : EN_MATH_KEYWORDS)
    {
      if (lower.contains(kw)) return true;
    }
    if (hasMathTokens && EN_ACTION_VERB_PATTERN.matcher(lower).find())
    {
      return true;
    }

    // 6. Common patterns like sqrt(...), sin(...), cos(...), log(...)
    if (lower.matches(".*(sqrt|sin|cos|tan|log|ln|dx/dy|d/dx|lim)\\s*\\(.*"))
    {
      return true;
    }

    return false;
  }

  /**
   * Extracts a potential math problem from text before cursor.
   */
  public static String extractMathProblem(String textBefore)
  {
    if (textBefore == null || textBefore.trim().isEmpty()) return null;

    int len = textBefore.length();
    int searchStart = Math.max(0, len - 160);
    String tail = textBefore.substring(searchStart);

    // Only the clause at the cursor can supply a suggestion. Older clauses
    // must not leak into a new empty line or a standalone equals sign.
    String part = currentClause(tail).trim();
    return isMathProblem(part) ? part : null;
  }

  private static String currentClause(String text)
  {
    int start = 0;
    for (int i = 0; i < text.length(); i++)
    {
      char c = text.charAt(i);
      if (c == '\n' || c == '\r' || c == '।' || c == ';') start = i + 1;
    }
    return text.substring(start);
  }

  private static boolean containsOperand(String text)
  {
    for (int i = 0; i < text.length(); i++)
      if (Character.isLetterOrDigit(text.charAt(i))) return true;
    return false;
  }

  private static boolean hasEmptyEqualsLeftSide(String text)
  {
    int equals = text.indexOf('=');
    return equals >= 0 && !containsOperand(text.substring(0, equals));
  }
}
