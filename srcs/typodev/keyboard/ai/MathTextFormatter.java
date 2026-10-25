package typodev.keyboard.ai;

import java.util.HashMap;
import java.util.Map;

/** Converts common AI math notation to readable, copyable Unicode text. */
public final class MathTextFormatter {
  private static final Map<String, String> SYMBOLS = new HashMap<>();
  static {
    String[][] symbols = {
      {"times", "×"}, {"cdot", "·"}, {"div", "÷"}, {"pm", "±"}, {"mp", "∓"},
      {"le", "≤"}, {"leq", "≤"}, {"ge", "≥"}, {"geq", "≥"}, {"ne", "≠"}, {"neq", "≠"},
      {"approx", "≈"}, {"equiv", "≡"}, {"infty", "∞"}, {"pi", "π"}, {"theta", "θ"},
      {"alpha", "α"}, {"beta", "β"}, {"gamma", "γ"}, {"delta", "δ"}, {"Delta", "Δ"},
      {"lambda", "λ"}, {"mu", "μ"}, {"sigma", "σ"}, {"omega", "ω"},
      {"sum", "∑"}, {"prod", "∏"}, {"int", "∫"}, {"partial", "∂"},
      {"rightarrow", "→"}, {"to", "→"}, {"Rightarrow", "⇒"}, {"therefore", "∴"},
      {"because", "∵"}, {"in", "∈"}, {"notin", "∉"}, {"cup", "∪"}, {"cap", "∩"},
      {"ldots", "…"}, {"cdots", "⋯"}, {"circ", "°"},
      {"quad", " "}, {"qquad", " "}, {"left", ""}, {"right", ""}, {"displaystyle", ""}
    };
    for (String[] symbol : symbols) SYMBOLS.put(symbol[0], symbol[1]);
  }

  private MathTextFormatter() {}

  public static String format(String input) {
    if (input == null || input.isEmpty()) return "";
    String text = input.replace("\r\n", "\n")
        .replaceAll("(?m)^\\s*```[^\\n]*$", "")
        .replaceAll("(?m)^\\s*#{1,6}\\s+", "")
        .replace("**", "").replace("__", "").replace("`", "");
    text = convert(text, 0);
    // Only strip paired dollar delimiters; keep literal prices and escaped '$'.
    text = text.replaceAll("(?s)\\$\\$(.+?)\\$\\$", "$1")
        .replaceAll("(?s)(?<!\\\\)\\$([^$]+?)(?<!\\\\)\\$", "$1")
        .replace("\\$", "$");
    return text.replaceAll("[ \\t]+\\n", "\n")
        .replaceAll("\\n{3,}", "\n\n").trim();
  }

  private static String convert(String text, int depth) {
    if (depth > 24) return text; // Malformed/model-generated nesting must stay bounded.
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < text.length();) {
      char c = text.charAt(i++);
      if (c == '^' || c == '_') {
        Group power = argument(text, i);
        // Plain-text model replies may use x^12 or x^-2 without TeX braces.
        int exponentStart = i;
        while (exponentStart < text.length() && Character.isWhitespace(text.charAt(exponentStart))) exponentStart++;
        int exponentEnd = exponentStart;
        if (exponentEnd < text.length() && (text.charAt(exponentEnd) == '+' || text.charAt(exponentEnd) == '-')) exponentEnd++;
        int digitsStart = exponentEnd;
        while (exponentEnd < text.length() && Character.isDigit(text.charAt(exponentEnd))) exponentEnd++;
        if (exponentEnd > digitsStart) power = new Group(text.substring(exponentStart, exponentEnd), exponentEnd);
        if (power != null) {
          String value = convert(power.value, depth + 1);
          String script = c == '^' && value.equals("°") ? "°" : script(value, c == '^');
          out.append(script != null ? script : c + "(" + value + ")");
          i = power.end;
        } else out.append(c);
        continue;
      }
      if (c != '\\') { out.append(c); continue; }
      if (i == text.length()) { out.append(c); break; }
      char next = text.charAt(i);
      if (next == '(' || next == ')' || next == '[' || next == ']') { i++; continue; }
      if (next == '\\') { out.append('\n'); i++; continue; }
      if (next == ',' || next == ';' || next == ':' || next == ' ') { out.append(' '); i++; continue; }
      if (next == '!') { i++; continue; }
      if (next == '%' || next == '#' || next == '&' || next == '_') { out.append(next); i++; continue; }
      int start = i;
      while (i < text.length() && isAsciiLetter(text.charAt(i))) i++;
      String command = text.substring(start, i);
      if (command.equals("frac") || command.equals("dfrac") || command.equals("tfrac")) {
        Group numerator = argument(text, i);
        Group denominator = numerator == null ? null : argument(text, numerator.end);
        if (denominator != null) {
          out.append(parenthesize(convert(numerator.value, depth + 1))).append('/')
              .append(parenthesize(convert(denominator.value, depth + 1)));
          i = denominator.end;
          continue;
        }
      } else if (command.equals("sqrt")) {
        String index = "";
        int argStart = i;
        if (argStart < text.length() && text.charAt(argStart) == '[') {
          int end = text.indexOf(']', argStart);
          if (end >= 0) { index = text.substring(argStart + 1, end); argStart = end + 1; }
        }
        Group radicand = argument(text, argStart);
        if (radicand != null) {
          String root = index.isEmpty() || index.equals("2") ? "√" : index.equals("3") ? "∛"
              : index.equals("4") ? "∜" : "root[" + convert(index, depth + 1) + "]";
          out.append(root).append('(').append(convert(radicand.value, depth + 1)).append(')');
          i = radicand.end;
          continue;
        }
      } else if (command.equals("text") || command.equals("mathrm") || command.equals("mathbf")
          || command.equals("operatorname") || command.equals("textbf")) {
        Group group = argument(text, i);
        if (group != null) { out.append(convert(group.value, depth + 1)); i = group.end; continue; }
      }
      String symbol = SYMBOLS.get(command);
      if (symbol != null) out.append(symbol);
      else if (command.equals("sin") || command.equals("cos") || command.equals("tan")
          || command.equals("log") || command.equals("ln") || command.equals("lim")) out.append(command);
      else {
        // Keep unsupported or incomplete commands intact instead of losing meaning.
        out.append('\\').append(command);
        if (i == start) out.append(text.charAt(i++));
      }
    }
    return out.toString();
  }

  private static boolean isAsciiLetter(char c) {
    return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
  }

  private static String parenthesize(String value) {
    return value.matches("[\\p{L}\\p{N}.]+") ? value : "(" + value + ")";
  }

  private static String script(String value, boolean superscript) {
    String from = "0123456789+-=()in";
    String to = superscript ? "⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾ⁱⁿ" : "₀₁₂₃₄₅₆₇₈₉₊₋₌₍₎ᵢₙ";
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < value.length(); i++) {
      int index = from.indexOf(value.charAt(i));
      if (index < 0) return null;
      out.append(to.charAt(index));
    }
    return out.toString();
  }

  private static Group argument(String text, int start) {
    while (start < text.length() && Character.isWhitespace(text.charAt(start))) start++;
    if (start == text.length()) return null;
    if (text.charAt(start) == '\\') {
      int end = start + 1;
      while (end < text.length() && isAsciiLetter(text.charAt(end))) end++;
      if (end > start + 1) return new Group(text.substring(start, end), end);
    }
    if (text.charAt(start) != '{') return new Group(text.substring(start, start + 1), start + 1);
    int balance = 1;
    for (int i = start + 1; i < text.length(); i++) {
      if (text.charAt(i) == '{') balance++;
      if (text.charAt(i) == '}' && --balance == 0)
        return new Group(text.substring(start + 1, i), i + 1);
    }
    return null;
  }

  private static final class Group {
    final String value;
    final int end;
    Group(String value, int end) { this.value = value; this.end = end; }
  }
}
