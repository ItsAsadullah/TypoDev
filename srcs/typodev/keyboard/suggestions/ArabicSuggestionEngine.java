package typodev.keyboard.suggestions;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Offline Arabic phrase completions; expansion always requires a suggestion tap. */
public final class ArabicSuggestionEngine {
  private static final String[][] PHRASES = {
    {"بسم", "بسم الله الرحمن الرحيم", "﷽"},
    {"الله", "الله أكبر", "الله"},
    {"محمد", "محمد رسول الله ﷺ", "محمد"},
    {"صلى", "ﷺ", "صلى الله عليه وسلم"},
    {"صل", "ﷺ", "صلى الله عليه وسلم"},
    {"لا", "لا إله إلا الله"},
    {"السلام", "السلام عليكم", "السلام عليكم ورحمة الله وبركاته"},
    {"وعليكم", "وعليكم السلام", "وعليكم السلام ورحمة الله وبركاته"},
    {"الحمد", "الحمد لله", "الحمد لله رب العالمين"},
    {"سبحان", "سبحان الله", "سبحان الله وبحمده"},
    {"استغفر", "أستغفر الله", "أستغفر الله العظيم"},
    {"ان", "إن شاء الله"},
    {"ما", "ما شاء الله"},
    {"جزاك", "جزاك الله خيرًا"},
    {"بارك", "بارك الله فيك"},
    {"رضي", "رضي الله عنه", "رضي الله عنها"},
    {"رحمه", "رحمه الله"},
    {"مرحبا", "مرحبًا"},
    {"شكرا", "شكرًا", "شكرًا جزيلًا"},
    {"كيف", "كيف حالك"},
    {"نعم", "نعم"}, {"اهلا", "أهلًا وسهلًا"},
    {"شكرا", "شكرًا لك"}, {"عفوا", "عفوًا"},
    {"جيد", "جيد"}, {"اليوم", "اليوم"}, {"غدا", "غدًا"},
    {"صباح", "صباح الخير"}, {"مساء", "مساء الخير"}
  };

  private ArabicSuggestionEngine() { }

  public static List<Candidate> queryCandidates(String prefix, int limit) {
    List<Candidate> result = new ArrayList<>();
    if (prefix == null || limit <= 0) return result;
    String normalized = normalize(prefix);
    if (normalized.length() < 2) return result;
    Set<String> completions = new LinkedHashSet<>();
    for (String[] entry : PHRASES) {
      if (normalize(entry[0]).startsWith(normalized)) {
        for (int i = 1; i < entry.length; i++) if (!entry[i].equals(prefix)) completions.add(entry[i]);
      }
    }
    for (String completion : completions) {
      result.add(new Candidate(completion, Candidate.Source.SNIPPET, 245, 0, 1f));
      if (result.size() == limit) break;
    }
    return result;
  }

  static String normalize(String text) {
    String decomposed = Normalizer.normalize(text, Normalizer.Form.NFKD);
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < decomposed.length(); i++) {
      char c = decomposed.charAt(i);
      int type = Character.getType(c);
      if (type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK || c == 'ـ') continue;
      result.append(c == 'ى' ? 'ي' : c);
    }
    return result.toString().trim();
  }
}
