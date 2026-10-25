package typodev.keyboard.ai;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The two user-facing solution styles share readable text for display and export. */
public final class MathSolverResponse {
  public enum Mode { NOTEBOOK, EXPLAINED }
  public final String finalAnswer, steps, formulas, fullText;

  private MathSolverResponse(String answer, String work, String rules) {
    finalAnswer = MathTextFormatter.format(answer);
    steps = MathTextFormatter.format(work);
    formulas = MathTextFormatter.format(rules);
    fullText = "সমাধান\n" + steps + "\n\nউত্তর: " + finalAnswer
        + (formulas.isEmpty() ? "" : "\n\nসূত্র ও শর্ত\n" + formulas);
  }

  public static String prompt(String problem, Mode mode) {
    String style = mode == Mode.NOTEBOOK
        ? "NOTEBOOK MODE: Write a school exercise-book solution, not a summary. State what is given and what must be found/proved. "
          + "For an identity, start from one side; expand products, show each intermediate algebraic line, collect like terms, then reach the other side. "
          + "Put EVERY equation/transformation on its own line, with successive lines starting '= '. Use short Bengali connectors between groups of equations. "
          + "Do not just quote the identity as its own proof. Finish a proof with বামপক্ষ = ডানপক্ষ, অতএব প্রমাণিত; use that conclusion ONLY for an actual proved identity.\n"
          + "Example notebook working for (a+b)^2:\nবামপক্ষ,\n(a + b)²\n= (a + b)(a + b)\n"
          + "= a(a + b) + b(a + b)\n= a² + ab + ab + b²\n= a² + 2ab + b²\n= ডানপক্ষ\nঅতএব প্রমাণিত।\n"
        : "EXPLANATION MODE: Teach the solution in numbered steps. For EACH step give the actual calculation or transformation "
          + "on a separate line and explain WHY that operation is valid in simple Bengali. Name the rule when it helps. "
          + "Show substitution, expansion, collecting like terms and checking the result where applicable; do not skip from the question to the final answer.\n";
    return "You are a careful school mathematics tutor. Give a worked mathematical solution in Bengali, unless the question explicitly requests another language.\n"
        + style
        + "Respect the exact question. Never invent missing values, equations or assumptions. Never cancel terms across a sum. "
        + "State denominator restrictions and check transformations. If an expression cannot simplify, SHOW why (for example no common factor), "
        + "state the domain and specify what information would be needed for a numerical value. Do not invent a proof or meaningless calculation to fill steps.\n"
        + "Use plain text and Unicode math (², ³, √, ×, ÷, π). No dollar delimiters, LaTeX commands, code fences or bold markers. "
        + "Write fractions as (numerator)/(denominator). Keep paragraphs short for a mobile keyboard panel.\n"
        + "Return ALL THREE sections with these exact headings:\n"
        + "### চূড়ান্ত উত্তর:\n<concise answer or precise statement that more information is needed>\n"
        + "### সমাধান:\n<complete worked solution in the selected mode; never omit the working>\n"
        + "### সূত্র ও নিয়ম:\n<rules and conditions used>\n\nQUESTION:\n" + problem;
  }

  public static MathSolverResponse parse(String raw) {
    if (raw == null) return null;
    String answer = "", work = "", rules = "";
    for (String section : raw.split("(?m)^\\s*#{2,3}\\s*")) {
      String p = section.trim().replace("**", "");
      int newline = p.indexOf('\n');
      if (newline < 0) continue;
      String title = p.substring(0, newline).toLowerCase(Locale.ROOT);
      String body = p.substring(newline + 1).trim();
      if (title.startsWith("চূড়ান্ত উত্তর") || title.startsWith("চূড়ান্ত উত্তর") || title.startsWith("final answer")) answer = body;
      else if (title.startsWith("সমাধান") || title.startsWith("ধাপে ধাপে") || title.startsWith("solution")
          || title.startsWith("step") || title.startsWith("working")) work = body;
      else if (title.startsWith("সূত্র") || title.startsWith("formula") || title.startsWith("rules")) rules = body;
    }
    // An answer-only response must not masquerade as a worked solution.
    if (answer.isEmpty() || work.isEmpty()) return null;
    MathSolverResponse result = new MathSolverResponse(answer, work, rules);
    return result.finalAnswer.isEmpty() || result.steps.isEmpty() ? null : result;
  }

  private static final Pattern SIMPLE_SQUARE = Pattern.compile(
      "^\\s*\\(([a-zA-Z\\p{IsBengali}])\\s*([+\\-−])\\s*([a-zA-Z\\p{IsBengali}])\\)\\s*(?:\\^?[2২]|²)\\s*=?\\s*$");

  /** A verified distributive proof for the common identity, available without AI. */
  public static MathSolverResponse offline(String problem, Mode mode) {
    if (problem == null) return null;
    Matcher m = SIMPLE_SQUARE.matcher(problem);
    if (!m.matches()) return null;
    String a = m.group(1), b = m.group(3);
    if (!Character.isLetter(a.charAt(0)) || !Character.isLetter(b.charAt(0)) || a.equals(b)) return null;
    boolean minus = !m.group(2).equals("+");
    String sign = minus ? " - " : " + ";
    String bracket = "(" + a + sign + b + ")";
    String expression = bracket + "²";
    String answer = a + "²" + sign + "2" + a + b + " + " + b + "²";
    String distribution = a + bracket + sign + b + bracket;
    String expanded = a + "²" + sign + a + b + sign + a + b + " + " + b + "²";
    String work;
    if (mode == Mode.NOTEBOOK) {
      work = "প্রমাণ করতে হবে\n" + expression + " = " + answer + "\n\nপ্রমাণ:\nবামপক্ষ,\n"
          + expression + "\n= " + bracket + bracket + "\n\nবণ্টন সূত্রে গুণ করে পাই,\n= "
          + distribution + "\n= " + expanded + "\n\nসমজাতীয় পদ একত্র করে পাই,\n= "
          + answer + "\n= ডানপক্ষ\n\nঅতএব বামপক্ষ = ডানপক্ষ। প্রমাণিত।";
    } else {
      work = "১. বর্গের অর্থ একই রাশিকে দুবার গুণ করা।\n" + expression + " = " + bracket + bracket
          + "\n\n২. প্রথম বন্ধনীর প্রতিটি পদ দিয়ে দ্বিতীয় বন্ধনীকে গুণ করি (বণ্টন সূত্র)।\n= " + distribution
          + "\n\n৩. বন্ধনী খুলে প্রতিটি গুণ করি। " + (minus ? "দুটি ঋণাত্মক পদের গুণফল ধনাত্মক হয়।" : "একই চলকের গুণফল তার বর্গ হয়।")
          + "\n= " + expanded + "\n\n৪. মাঝের সমজাতীয় পদ দুটি যোগ করি।\n= " + answer
          + "\n\nএটি ডানপক্ষের সমান, তাই সূত্রটি প্রমাণিত।";
    }
    return new MathSolverResponse(answer, work,
        "বণ্টন সূত্র: x(y + z) = xy + xz\nগুণের বিনিময় সূত্র: xy = yx\nসমজাতীয় পদ: xy + xy = 2xy");
  }
}
