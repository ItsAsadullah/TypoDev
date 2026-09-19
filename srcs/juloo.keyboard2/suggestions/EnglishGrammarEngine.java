package juloo.keyboard2.suggestions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance English Grammar, Contractions, Spelling Corrections,
 * Morphological Inflections (Possessives), and Ordinal/Superscript Engine.
 */
public class EnglishGrammarEngine
{
  private static EnglishGrammarEngine _instance;

  public static synchronized EnglishGrammarEngine instance()
  {
    if (_instance == null)
    {
      _instance = new EnglishGrammarEngine();
    }
    return _instance;
  }

  private final Map<String, String> _contractions = new HashMap<>();
  private final Map<String, String> _misspellings = new HashMap<>();
  private final Map<String, String> _superscriptMap = new HashMap<>();
  private static final Pattern ORDINAL_PATTERN = Pattern.compile("^(\\d+)(st|nd|rd|th)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern ORDINAL_PREFIX_PATTERN = Pattern.compile("^(\\d+)([snrt])$", Pattern.CASE_INSENSITIVE);

  private EnglishGrammarEngine()
  {
    initContractions();
    initMisspellings();
    initSuperscripts();
  }

  private void initContractions()
  {
    // Pronoun + Auxiliary contractions
    putContraction("im", "I'm");
    putContraction("ive", "I've");
    putContraction("id", "I'd");
    putContraction("ill", "I'll");
    putContraction("youre", "you're");
    putContraction("youve", "you've");
    putContraction("youd", "you'd");
    putContraction("youll", "you'll");
    putContraction("theyre", "they're");
    putContraction("theyve", "they've");
    putContraction("theyd", "they'd");
    putContraction("theyll", "they'll");
    putContraction("weve", "we've");
    putContraction("were", "we're"); // Handled contextually
    putContraction("wed", "we'd");
    putContraction("well", "we'll");
    putContraction("hes", "he's");
    putContraction("hed", "he'd");
    putContraction("hell", "he'll");
    putContraction("shes", "she's");
    putContraction("shed", "she'd");
    putContraction("shell", "she'll");
    putContraction("itll", "it'll");
    putContraction("wholl", "who'll");
    putContraction("whos", "who's");
    putContraction("whatll", "what'll");

    // Negations
    putContraction("dont", "don't");
    putContraction("cant", "can't");
    putContraction("wont", "won't");
    putContraction("didnt", "didn't");
    putContraction("doesnt", "doesn't");
    putContraction("isnt", "isn't");
    putContraction("arent", "aren't");
    putContraction("wasnt", "wasn't");
    putContraction("werent", "weren't");
    putContraction("hasnt", "hasn't");
    putContraction("havent", "haven't");
    putContraction("hadnt", "hadn't");
    putContraction("couldnt", "couldn't");
    putContraction("wouldnt", "wouldn't");
    putContraction("shouldnt", "shouldn't");
    putContraction("mustnt", "mustn't");
    putContraction("neednt", "needn't");
    putContraction("mightnt", "mightn't");

    // Demonstratives & Question tags
    putContraction("thats", "that's");
    putContraction("whats", "what's");
    putContraction("wheres", "where's");
    putContraction("hows", "how's");
    putContraction("theres", "there's");
    putContraction("heres", "here's");
    putContraction("lets", "let's");

    // Informal spoken English & Abbreviations
    putContraction("gonna", "going to");
    putContraction("wanna", "want to");
    putContraction("gotta", "got to");
    putContraction("kinda", "kind of");
    putContraction("sorta", "sort of");
    putContraction("lemme", "let me");
    putContraction("gimme", "give me");
    putContraction("cuz", "because");
    putContraction("u", "you");
    putContraction("ur", "your");
    putContraction("r", "are");
    putContraction("pls", "please");
    putContraction("plz", "please");
    putContraction("thx", "thanks");
    putContraction("ty", "thank you");
    putContraction("omg", "oh my god");
    putContraction("btw", "by the way");
    putContraction("idk", "I don't know");
    putContraction("tbh", "to be honest");
    putContraction("imo", "in my opinion");
    putContraction("fyi", "for your information");
    putContraction("brb", "be right back");
    putContraction("asap", "as soon as possible");
  }

  private void putContraction(String shortcut, String expansion)
  {
    _contractions.put(shortcut.toLowerCase(Locale.ROOT), expansion);
  }

  private void initMisspellings()
  {
    // High-frequency typos and spelling mistakes
    putMisspelling("assingment", "assignment");
    putMisspelling("assignemt", "assignment");
    putMisspelling("assigment", "assignment");
    putMisspelling("asignment", "assignment");
    putMisspelling("knowlege", "knowledge");
    putMisspelling("knowlede", "knowledge");
    putMisspelling("knwledge", "knowledge");
    putMisspelling("nowledge", "knowledge");
    putMisspelling("beleive", "believe");
    putMisspelling("belive", "believe");
    putMisspelling("recieve", "receive");
    putMisspelling("recive", "receive");
    putMisspelling("seperate", "separate");
    putMisspelling("separete", "separate");
    putMisspelling("definately", "definitely");
    putMisspelling("definatly", "definitely");
    putMisspelling("definitly", "definitely");
    putMisspelling("goverment", "government");
    putMisspelling("govment", "government");
    putMisspelling("occured", "occurred");
    putMisspelling("ocurred", "occurred");
    putMisspelling("untill", "until");
    putMisspelling("tommorow", "tomorrow");
    putMisspelling("tomorow", "tomorrow");
    putMisspelling("wierd", "weird");
    putMisspelling("calender", "calendar");
    putMisspelling("collegue", "colleague");
    putMisspelling("succesfull", "successful");
    putMisspelling("successfull", "successful");
    putMisspelling("alot", "a lot");
    putMisspelling("teh", "the");
    putMisspelling("thsi", "this");
    putMisspelling("fro", "for");
    putMisspelling("waht", "what");
    putMisspelling("hwo", "how");
    putMisspelling("woudl", "would");
    putMisspelling("coudl", "could");
    putMisspelling("shoudl", "should");
    putMisspelling("peopel", "people");
    putMisspelling("becuase", "because");
    putMisspelling("beacuse", "because");
    putMisspelling("enviroment", "environment");
    putMisspelling("truely", "truly");
    putMisspelling("neccessary", "necessary");
    putMisspelling("necesary", "necessary");
    putMisspelling("acommodate", "accommodate");
    putMisspelling("dissappoint", "disappoint");
    putMisspelling("embarass", "embarrass");
    putMisspelling("mispell", "misspell");
    putMisspelling("grammer", "grammar");
    putMisspelling("speeling", "spelling");
    putMisspelling("progam", "program");
    putMisspelling("progaming", "programming");
    putMisspelling("developement", "development");
    putMisspelling("langauge", "language");
    putMisspelling("fuction", "function");
    putMisspelling("funtion", "function");
    putMisspelling("interfce", "interface");
    putMisspelling("retrun", "return");
    putMisspelling("improt", "import");
    putMisspelling("exprot", "export");
    putMisspelling("clas", "class");
    putMisspelling("pakage", "package");
  }

  private void putMisspelling(String typo, String correct)
  {
    _misspellings.put(typo.toLowerCase(Locale.ROOT), correct);
  }

  private void initSuperscripts()
  {
    _superscriptMap.put("0", "⁰");
    _superscriptMap.put("1", "¹");
    _superscriptMap.put("2", "²");
    _superscriptMap.put("3", "³");
    _superscriptMap.put("4", "⁴");
    _superscriptMap.put("5", "⁵");
    _superscriptMap.put("6", "⁶");
    _superscriptMap.put("7", "⁷");
    _superscriptMap.put("8", "⁸");
    _superscriptMap.put("9", "⁹");
    _superscriptMap.put("+", "⁺");
    _superscriptMap.put("-", "⁻");
    _superscriptMap.put("st", "ˢᵗ");
    _superscriptMap.put("nd", "ⁿᵈ");
    _superscriptMap.put("rd", "ʳᵈ");
    _superscriptMap.put("th", "ᵗʰ");
  }

  /**
   * Generates grammar, spelling, contraction, possessive, and ordinal candidates
   * for the input word.
   */
  public List<Candidate> queryCandidates(String word, int maxResults)
  {
    if (word == null || word.trim().isEmpty() || maxResults <= 0)
    {
      return Collections.emptyList();
    }

    String raw = word.trim();
    String lower = raw.toLowerCase(Locale.ROOT);
    boolean isTitleCase = (raw.length() > 0 && Character.isUpperCase(raw.charAt(0)));
    boolean isAllUpper = isAllUpperCase(raw);

    List<Candidate> candidates = new ArrayList<>();

    // 1. Contractions (e.g. "im" -> "I'm", "dont" -> "don't")
    String contraction = _contractions.get(lower);
    if (contraction != null)
    {
      String formatted = applyCasing(contraction, isTitleCase, isAllUpper);
      candidates.add(new Candidate(formatted, Candidate.Source.AUTOCORRECT, 252, 0, 1.0f));
    }

    // 2. High-Frequency Misspelling Corrections (e.g. "assingment" -> "assignment")
    String corrected = _misspellings.get(lower);
    if (corrected != null)
    {
      String formatted = applyCasing(corrected, isTitleCase, isAllUpper);
      candidates.add(new Candidate(formatted, Candidate.Source.TYPO_CORRECTION, 245, 1, 0.95f));
    }

    // 3. Ordinal & Superscript Suggestions (e.g. "3rd" -> "3rd", "3ʳᵈ")
    Matcher ordMatcher = ORDINAL_PATTERN.matcher(raw);
    if (ordMatcher.matches())
    {
      String num = ordMatcher.group(1);
      String suffix = ordMatcher.group(2).toLowerCase(Locale.ROOT);
      String supSuffix = _superscriptMap.get(suffix);
      if (supSuffix != null)
      {
        candidates.add(new Candidate(num + supSuffix, Candidate.Source.CODE_SNIPPET, 248, 0, 1.0f));
      }
      candidates.add(new Candidate(num + suffix, Candidate.Source.CORE_LEXICON, 240, 0, 1.0f));
    }
    else
    {
      Matcher prefixMatcher = ORDINAL_PREFIX_PATTERN.matcher(raw);
      if (prefixMatcher.matches())
      {
        String num = prefixMatcher.group(1);
        char ch = Character.toLowerCase(prefixMatcher.group(2).charAt(0));
        String fullSuffix = null;
        if (ch == 's' && num.endsWith("1") && !num.endsWith("11")) fullSuffix = "st";
        else if (ch == 'n' && num.endsWith("2") && !num.endsWith("12")) fullSuffix = "nd";
        else if (ch == 'r' && num.endsWith("3") && !num.endsWith("13")) fullSuffix = "rd";
        else if (ch == 't') fullSuffix = "th";

        if (fullSuffix != null)
        {
          String sup = _superscriptMap.get(fullSuffix);
          if (sup != null) candidates.add(new Candidate(num + sup, Candidate.Source.CODE_SNIPPET, 246, 0, 1.0f));
          candidates.add(new Candidate(num + fullSuffix, Candidate.Source.CORE_LEXICON, 240, 0, 1.0f));
        }
      }
    }

    // Common Math Exponents & Units (e.g. "x2" -> "x²", "m2" -> "m²", "cm2" -> "cm²")
    if (lower.equals("x2")) candidates.add(new Candidate("x²", Candidate.Source.CODE_SNIPPET, 245, 0, 1.0f));
    else if (lower.equals("x3")) candidates.add(new Candidate("x³", Candidate.Source.CODE_SNIPPET, 245, 0, 1.0f));
    else if (lower.equals("m2")) candidates.add(new Candidate("m²", Candidate.Source.CODE_SNIPPET, 245, 0, 1.0f));
    else if (lower.equals("m3")) candidates.add(new Candidate("m³", Candidate.Source.CODE_SNIPPET, 245, 0, 1.0f));
    else if (lower.equals("cm2")) candidates.add(new Candidate("cm²", Candidate.Source.CODE_SNIPPET, 245, 0, 1.0f));
    else if (lower.equals("km2")) candidates.add(new Candidate("km²", Candidate.Source.CODE_SNIPPET, 245, 0, 1.0f));
    else if (lower.equals("deg")) candidates.add(new Candidate("°", Candidate.Source.CODE_SNIPPET, 245, 0, 1.0f));

    // 4. Morphological Inflections & Possessives (e.g. "Father" -> "Father's", "fathers")
    if (raw.length() >= 3 && !raw.contains("'") && !isDigitOnly(raw))
    {
      // Possessive: "Father's" / "father's"
      if (!raw.endsWith("s"))
      {
        candidates.add(new Candidate(raw + "'s", Candidate.Source.GRAMMAR, 235, 0, 0.92f));
        candidates.add(new Candidate(raw + "s", Candidate.Source.GRAMMAR, 225, 0, 0.90f));
      }
      else
      {
        candidates.add(new Candidate(raw + "'", Candidate.Source.GRAMMAR, 235, 0, 0.92f));
        candidates.add(new Candidate(raw + "'s", Candidate.Source.GRAMMAR, 230, 0, 0.90f));
      }

      // Consonant + y -> ies (e.g. "company" -> "companies")
      if (raw.endsWith("y") && raw.length() >= 4)
      {
        char beforeY = raw.charAt(raw.length() - 2);
        if (!isVowel(beforeY))
        {
          String stem = raw.substring(0, raw.length() - 1);
          candidates.add(new Candidate(stem + "ies", Candidate.Source.GRAMMAR, 220, 0, 0.90f));
        }
      }
    }

    return candidates;
  }

  private boolean isVowel(char c)
  {
    char l = Character.toLowerCase(c);
    return l == 'a' || l == 'e' || l == 'i' || l == 'o' || l == 'u';
  }

  private boolean isDigitOnly(String s)
  {
    for (int i = 0; i < s.length(); i++)
    {
      if (!Character.isDigit(s.charAt(i))) return false;
    }
    return true;
  }

  private boolean isAllUpperCase(String s)
  {
    if (s == null || s.length() <= 1) return false;
    for (int i = 0; i < s.length(); i++)
    {
      if (Character.isLetter(s.charAt(i)) && !Character.isUpperCase(s.charAt(i)))
      {
        return false;
      }
    }
    return true;
  }

  private String applyCasing(String target, boolean titleCase, boolean allUpper)
  {
    if (target == null || target.isEmpty()) return target;
    if (allUpper) return target.toUpperCase(Locale.ROOT);
    if (titleCase && Character.isLowerCase(target.charAt(0)))
    {
      return Character.toUpperCase(target.charAt(0)) + target.substring(1);
    }
    return target;
  }
}
