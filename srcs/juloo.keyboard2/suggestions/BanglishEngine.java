package juloo.keyboard2.suggestions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Phonetic Banglish-to-Bangla candidate generation and transliteration engine.
 * Generates Bengali words from Latin phonetic typing without forcing replacement on English text.
 */
public class BanglishEngine
{
  private static final Map<String, String> COMMON_BANGLISH = new HashMap<>(256);
  private static BanglishEngine _instance;

  public static synchronized BanglishEngine instance()
  {
    if (_instance == null)
    {
      _instance = new BanglishEngine();
    }
    return _instance;
  }

  static
  {
    // High-frequency pronouns & greetings
    put("ami", "আমি");
    put("amra", "আমরা");
    put("amar", "আমার");
    put("amader", "আমাদের");
    put("amake", "আমাকে");
    put("tumi", "তুমি");
    put("tomra", "তোমরা");
    put("tomar", "তোমার");
    put("tomader", "তোমাদের");
    put("tomake", "তোমাকে");
    put("tui", "তুই");
    put("tora", "তোরা");
    put("tor", "তোর");
    put("toder", "তোদের");
    put("toke", "তোকে");
    put("apni", "আপনি");
    put("apnara", "আপনারা");
    put("apnar", "আপনার");
    put("apnader", "আপনাদের");
    put("apnake", "আপনাকে");
    put("she", "সে");
    put("tini", "তিনি");
    put("tar", "তার");
    put("tader", "তাদের");
    put("take", "তাকে");

    // Questions & common verbs
    put("kemon", "কেমন");
    put("ki", "কি");
    put("kothay", "কোথায়");
    put("kothaye", "কোথায়");
    put("kokhon", "কখন");
    put("keno", "কেন");
    put("ke", "কে");
    put("kara", "কারা");
    put("kar", "কার");
    put("kake", "কাকে");
    put("kivabe", "কিভাবে");
    put("kemne", "কেমনে");
    put("koto", "কত");
    put("koi", "কই");

    put("acho", "আছো");
    put("asen", "আছেন");
    put("achen", "আছেন");
    put("achis", "আছিস");
    put("achi", "আছি");
    put("ache", "আছে");
    put("bhalo", "ভালো");
    put("valo", "ভালো");
    put("bhalobashi", "ভালোবাসি");
    put("bhalobasi", "ভালোবাসি");
    put("valobasi", "ভালোবাসি");
    put("valobashi", "ভালোবাসি");
    put("valobasha", "ভালোবাসা");
    put("bhalobasha", "ভালোবাসা");

    put("dhonnobad", "ধন্যবাদ");
    put("dhonnobaad", "ধন্যবাদ");
    put("shobai", "সবাই");
    put("sobai", "সবাই");
    put("shokol", "সকল");
    put("sokol", "সকল");
    put("shokal", "সকাল");
    put("sokal", "সকাল");
    put("shuvo", "শুভ");
    put("subho", "শুভ");
    put("rat", "রাত");
    put("raat", "রাত");
    put("ratri", "রাত্রি");
    put("shondha", "সন্ধ্যা");
    put("dupur", "দুপুর");

    put("khobor", "খবর");
    put("obostha", "অবস্থা");
    put("dekha", "দেখা");
    put("kotha", "কথা");
    put("hobe", "হবে");
    put("hoyeche", "হয়েছে");
    put("hoise", "হয়েছে");
    put("hobe na", "হবে না");
    put("hobena", "হবেনা");
    put("korbo", "করব");
    put("korbo na", "করব না");
    put("korcho", "করছ");
    put("korso", "করছ");
    put("korchen", "করছেন");
    put("korsen", "করছেন");
    put("korchi", "করছি");
    put("korsi", "করছি");
    put("korchis", "করছিস");
    put("korte", "করতে");
    put("kore", "করে");
    put("kora", "করা");

    put("bondhu", "বন্ধু");
    put("bhai", "ভাই");
    put("vai", "ভাই");
    put("apu", "আপু");
    put("apa", "আপা");
    put("bhaiya", "ভাইয়া");
    put("vauya", "ভাইয়া");
    put("shunon", "শুনুন");
    put("shunun", "শুনুন");
    put("sunun", "শুনুন");
    put("bolun", "বলুন");
    put("bolo", "বলো");
    put("bol", "বল");
    put("shundor", "সুন্দর");
    put("sundor", "সুন্দর");
    put("shomossha", "সমস্যা");
    put("somossha", "সমস্যা");

    put("onek", "অনেক");
    put("khub", "খুব");
    put("ektu", "একটু");
    put("beshi", "বেশি");
    put("besi", "বেশি");
    put("kom", "কম");
    put("thik", "ঠিক");
    put("thik ache", "ঠিক আছে");
    put("shomoy", "সময়");
    put("somoy", "সময়");
    put("din", "দিন");
    put("ajke", "আজকে");
    put("aajke", "আজকে");
    put("kaalke", "কালকে");
    put("kal", "কাল");
    put("pore", "পরে");
    put("akhon", "এখন");
    put("ekhon", "এখন");
    put("ekhoni", "এখনই");

    put("allah", "আল্লাহ");
    put("hafez", "হাফেজ");
    put("insha", "ইনশা");
    put("masha", "মাশা");
    put("alhamdulillah", "আলহামদুলিল্লাহ");
    put("assalamu", "আসসালামু");
    put("alaikum", "আলাইকুম");
    put("bangla", "বাংলা");
    put("bangladesh", "বাংলাদেশ");
    put("desh", "দেশ");
    put("manush", "মানুষ");
    put("taka", "টাকা");
    put("bari", "বাড়ি");
    put("basha", "বাসা");
    put("kaj", "কাজ");
    put("kaje", "কাজে");
    put("jani", "জানি");
    put("jano", "জানো");
    put("janen", "জানেন");
    put("janina", "জানিনা");
    put("parbo", "পারব");
    put("parbona", "পারবনা");
    put("parben", "পারবেন");
    put("parbe", "পারবে");
    put("jabo", "যাব");
    put("jabona", "যাবনা");
    put("jabe", "যাবে");
    put("jaben", "যাবেন");
    put("ashbo", "আসব");
    put("ashbe", "আসবে");
    put("ashben", "আসবেন");
    put("asho", "এসো");
    put("ashun", "আসুন");
    put("khawa", "খাওয়া");
    put("kheyecho", "খেয়েছ");
    put("kheyechen", "খেয়েছেন");
    put("kheyechi", "খেয়েছি");
    put("ghum", "ঘুম");
    put("shathey", "সাথে");
    put("shathe", "সাথে");
    put("sathe", "সাথে");
    put("jonno", "জন্য");
    put("karone", "কারণে");
    put("mone", "মনে");
    put("mon", "মন");
  }

  private static void put(String banglish, String bangla)
  {
    COMMON_BANGLISH.put(banglish.toLowerCase(Locale.ROOT), bangla);
  }

  private BanglishEngine()
  {
  }

  /**
   * Generates Banglish transliteration candidates for a Latin input word.
   */
  public List<Candidate> generateCandidates(String latinWord, int maxResults)
  {
    if (latinWord == null || latinWord.trim().isEmpty() || maxResults <= 0)
    {
      return Collections.emptyList();
    }

    String lower = latinWord.trim().toLowerCase(Locale.ROOT);
    if (!isLatinOnly(lower))
    {
      return Collections.emptyList();
    }

    List<Candidate> results = new ArrayList<>();

    // 1. Direct high-frequency dictionary hit
    String directHit = COMMON_BANGLISH.get(lower);
    if (directHit != null)
    {
      Candidate c = new Candidate(directHit, Candidate.Source.BANGLISH, 230, 0, 1.0f);
      results.add(c);
    }

    // 2. Prefix matches in common Banglish dictionary
    if (results.size() < maxResults && lower.length() >= 2)
    {
      for (Map.Entry<String, String> entry : COMMON_BANGLISH.entrySet())
      {
        if (results.size() >= maxResults) break;
        if (entry.getKey().startsWith(lower) && !entry.getKey().equals(lower))
        {
          String bn = entry.getValue();
          boolean alreadyAdded = false;
          for (Candidate existing : results)
          {
            if (existing.word.equals(bn))
            {
              alreadyAdded = true;
              break;
            }
          }
          if (!alreadyAdded)
          {
            float ratio = (float) lower.length() / (float) entry.getKey().length();
            int freq = Math.max(100, (int) (210 * ratio));
            Candidate c = new Candidate(bn, Candidate.Source.BANGLISH, freq, 0, ratio);
            results.add(c);
          }
        }
      }
    }

    // 3. Phonetic rule-based transliteration fallback if no direct match
    if (results.isEmpty() && lower.length() >= 2)
    {
      String phonetic = transliteratePhonetic(lower);
      if (phonetic != null && !phonetic.isEmpty() && isBengaliScript(phonetic))
      {
        Candidate c = new Candidate(phonetic, Candidate.Source.BANGLISH, 130, 0, 1.0f);
        results.add(c);
      }
    }

    return results;
  }

  public boolean hasExactMatch(String latinWord)
  {
    if (latinWord == null) return false;
    return COMMON_BANGLISH.containsKey(latinWord.trim().toLowerCase(Locale.ROOT));
  }

  public String getExactBangla(String latinWord)
  {
    if (latinWord == null) return null;
    return COMMON_BANGLISH.get(latinWord.trim().toLowerCase(Locale.ROOT));
  }

  /**
   * Rule-based phonetic converter from Latin to Bengali script.
   */
  public static String transliteratePhonetic(String text)
  {
    if (text == null || text.isEmpty()) return "";
    StringBuilder sb = new StringBuilder();
    int len = text.length();
    int i = 0;

    while (i < len)
    {
      // Try 3-char matches
      if (i + 3 <= len)
      {
        String sub3 = text.substring(i, i + 3);
        if ("chh".equals(sub3)) { appendConsonant(sb, "ছ"); i += 3; continue; }
        if ("ngh".equals(sub3)) { appendConsonant(sb, "ঙ্ঘ"); i += 3; continue; }
      }

      // Try 2-char matches
      if (i + 2 <= len)
      {
        String sub2 = text.substring(i, i + 2);
        if ("kh".equals(sub2)) { appendConsonant(sb, "খ"); i += 2; continue; }
        if ("gh".equals(sub2)) { appendConsonant(sb, "ঘ"); i += 2; continue; }
        if ("ch".equals(sub2)) { appendConsonant(sb, "চ"); i += 2; continue; }
        if ("jh".equals(sub2)) { appendConsonant(sb, "ঝ"); i += 2; continue; }
        if ("th".equals(sub2)) { appendConsonant(sb, "থ"); i += 2; continue; }
        if ("dh".equals(sub2)) { appendConsonant(sb, "ধ"); i += 2; continue; }
        if ("ph".equals(sub2)) { appendConsonant(sb, "ফ"); i += 2; continue; }
        if ("bh".equals(sub2)) { appendConsonant(sb, "ভ"); i += 2; continue; }
        if ("sh".equals(sub2)) { appendConsonant(sb, "শ"); i += 2; continue; }
        if ("ng".equals(sub2)) { appendConsonant(sb, "ং"); i += 2; continue; }
        if ("ee".equals(sub2)) { appendVowel(sb, "ী", "ঈ"); i += 2; continue; }
        if ("oo".equals(sub2)) { appendVowel(sb, "ূ", "ঊ"); i += 2; continue; }
        if ("ou".equals(sub2)) { appendVowel(sb, "ৌ", "ঔ"); i += 2; continue; }
        if ("oi".equals(sub2)) { appendVowel(sb, "ৈ", "ঐ"); i += 2; continue; }
        if ("aa".equals(sub2)) { appendVowel(sb, "া", "আ"); i += 2; continue; }
      }

      char c = text.charAt(i);
      switch (c)
      {
        case 'a': appendVowel(sb, "া", "আ"); break;
        case 'i': appendVowel(sb, "ি", "ই"); break;
        case 'u': appendVowel(sb, "ু", "উ"); break;
        case 'e': appendVowel(sb, "ে", "এ"); break;
        case 'o': appendVowel(sb, "ো", "ও"); break;
        case 'k': appendConsonant(sb, "ক"); break;
        case 'g': appendConsonant(sb, "গ"); break;
        case 'j': appendConsonant(sb, "জ"); break;
        case 't': appendConsonant(sb, "ট"); break;
        case 'd': appendConsonant(sb, "দ"); break;
        case 'n': appendConsonant(sb, "ন"); break;
        case 'p': appendConsonant(sb, "প"); break;
        case 'f': appendConsonant(sb, "ফ"); break;
        case 'b': appendConsonant(sb, "ব"); break;
        case 'v': appendConsonant(sb, "ভ"); break;
        case 'm': appendConsonant(sb, "ম"); break;
        case 'r': appendConsonant(sb, "র"); break;
        case 'l': appendConsonant(sb, "ল"); break;
        case 's': appendConsonant(sb, "স"); break;
        case 'h': appendConsonant(sb, "হ"); break;
        case 'y': appendConsonant(sb, "য়"); break;
        case 'z': appendConsonant(sb, "য"); break;
        default: sb.append(c); break;
      }
      i++;
    }
    return sb.toString();
  }

  private static void appendVowel(StringBuilder sb, String kar, String full)
  {
    if (sb.length() == 0 || !isBengaliConsonant(sb.charAt(sb.length() - 1)))
    {
      sb.append(full);
    }
    else
    {
      sb.append(kar);
    }
  }

  private static void appendConsonant(StringBuilder sb, String cons)
  {
    sb.append(cons);
  }

  private static boolean isBengaliConsonant(char c)
  {
    return c >= '\u0995' && c <= '\u09B9';
  }

  public static boolean isBengaliScript(String str)
  {
    if (str == null) return false;
    for (int i = 0; i < str.length(); i++)
    {
      char c = str.charAt(i);
      if (c >= '\u0980' && c <= '\u09FF')
      {
        return true;
      }
    }
    return false;
  }

  public static boolean isLatinOnly(String str)
  {
    if (str == null || str.isEmpty()) return false;
    for (int i = 0; i < str.length(); i++)
    {
      char c = str.charAt(i);
      if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')))
      {
        return false;
      }
    }
    return true;
  }
}
