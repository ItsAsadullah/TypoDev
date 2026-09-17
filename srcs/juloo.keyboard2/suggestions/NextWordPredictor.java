package juloo.keyboard2.suggestions;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * High-performance Next-Word Predictor supporting both Bengali & English collocations,
 * along with adaptive learning from user typing patterns.
 */
public class NextWordPredictor
{
  private static final String PREF_LEARNED_BIGRAMS = "learned_bigrams_store";
  private static final int MAX_LEARNED_PER_WORD = 5;

  private static NextWordPredictor _instance;

  public static synchronized NextWordPredictor instance(Context context)
  {
    if (_instance == null)
    {
      _instance = new NextWordPredictor(context != null ? context.getApplicationContext() : null);
    }
    return _instance;
  }

  private final Context _context;
  private final Map<String, List<String>> _builtInBigrams = new HashMap<>();
  private final Map<String, List<String>> _learnedBigrams = new HashMap<>();

  private NextWordPredictor(Context context)
  {
    _context = context;
    initBuiltInCollocations();
    loadLearnedBigrams();
  }

  private void initBuiltInCollocations()
  {
    // --- Bengali Multi-word Phrases (Trigrams) ---
    putBigram("তোমাকে ভালো", "বাসি", "লাগে", "রেখে", "বাসবে");
    putBigram("খুব ভালো", "লাগল", "হয়েছে", "বাসি", "মানুষ", "ছিল", "হবে");
    putBigram("অনেক ভালো", "লেগেছে", "হয়েছে", "থাকবেন", "মানুষ", "থেকো");
    putBigram("আমি তোমাকে", "ভালো", "ভালোবাসি", "অনেক", "বিশ্বাস", "খুব", "বলছি");
    putBigram("আমি ভালো", "আছি", "নেই", "করব");
    putBigram("তুমি কেমন", "আছো", "বোধ", "করছ");
    putBigram("আপনি কেমন", "আছেন", "অনুভব", "করছেন");
    putBigram("তুই কেমন", "আছিস", "করছিস");
    putBigram("অনেক ধন্যবাদ", "আপনাকে", "তোমাকে", "ভাই", "সবাইকে");
    putBigram("ধন্যবাদ আপনাকে", "অনেক", "ভাই");
    putBigram("ধন্যবাদ তোমাকে", "অনেক", "বন্ধু");
    putBigram("শুভ সকাল", "সবাইকে", "বন্ধু", "ভাই");
    putBigram("শুভ জন্মদিন", "তোমাকে", "ভাই", "অনেক", "দোয়া");
    putBigram("শুভ রাত্রি", "সবাইকে", "ভালো", "ঘুম");
    putBigram("কি খবর", "তোমার", "আপনার", "ভাই", "কেমন");
    putBigram("কি অবস্থা", "তোমার", "আপনার", "ভাই");
    putBigram("দেখা হবে", "আবার", "কালকে", "কথা", "শীঘ্রই");
    putBigram("কথা হবে", "পরে", "কালকে", "আবার");
    putBigram("ভালোবাসি তোমাকে", "অনেক", "খুব", "সবসময়");

    // --- Bengali High-Frequency Transitions ---
    putBigram("ভালো", "বাসি", "লাগে", "থেকো", "থাকবেন", "আছি", "আছো", "আছেন", "লেগেছে", "করব", "মানুষ", "বাসার", "হবে", "বাসবে", "লাগল");
    putBigram("তোমাকে", "ভালো", "ভালোবাসি", "অনেক", "ধন্যবাদ", "বলতে", "ছাড়া", "নিয়ে", "একটি");
    putBigram("ভালোবাসি", "তোমাকে", "অনেক", "সবসময়", "খুব");
    putBigram("ভালোবাসা", "অবিরাম", "দিও", "নিও");
    putBigram("আমি", "তোমাকে", "ভালো", "তোমাদের", "আছি", "যাব", "করব", "চাই", "বলছি", "একটু", "এখন");
    putBigram("তুমি", "কেমন", "কি", "কোথায়", "আছো", "যাবে", "ভালো", "পারবে", "আমার", "কবে");
    putBigram("তুই", "কেমন", "কি", "কোথায়", "আছিস", "যাবি", "আয়", "কবে");
    putBigram("আপনি", "কেমন", "কি", "কোথায়", "আছেন", "বলুন", "ভালো", "পারবেন", "আসবেন");
    putBigram("আমরা", "সবাই", "করতে", "যাব", "চাই", "ভালো", "থাকব");
    putBigram("কেমন", "আছেন", "আছো", "আছিস", "হলো", "লাগল", "চলছে");
    putBigram("অনেক", "ধন্যবাদ", "ভালোবাসা", "সুন্দর", "ভালো", "দিন", "কষ্ট", "টাকা", "বেশি");
    putBigram("শুভ", "সকাল", "রাত্রি", "জন্মদিন", "কামনা", "দিন");
    putBigram("ধন্যবাদ", "আপনাকে", "তোমাকে", "ভাই", "সবাইকে", "অনেক");
    putBigram("খুব", "ভালো", "সুন্দর", "বেশি", "খারাপ", "সহজ", "কষ্ট", "তাড়াতাড়ি");
    putBigram("কি", "খবর", "অবস্থা", "করছ", "হয়েছে", "হলো", "করছেন", "ব্যাপার");
    putBigram("মনে", "হয়", "হচ্ছে", "পড়ে", "রেখো", "করি", "রাখবেন");
    putBigram("কোথায়", "আছো", "আছেন", "যাবেন", "যাবে", "গেলে");
    putBigram("কখন", "আসবে", "যাবে", "হবে", "আসবেন");
    putBigram("কেন", "এমন", "করছ", "হলো", "বললে");
    putBigram("সবাই", "কেমন", "ভালো", "আছেন", "মিলে");
    putBigram("একটু", "পরে", "শুনুন", "দাঁড়ান", "সাহায্য");
    putBigram("কোনো", "সমস্যা", "কথা", "ব্যাপার", "কিছু", "ভয়", "সন্দেহ");
    putBigram("কিছু", "বলতে", "করতে", "টাকা", "সময়", "হবে", "মনে");
    putBigram("সব", "সময়", "কিছু", "ঠিক", "মানুষ", "জায়গায়");
    putBigram("এক", "দিন", "বার", "সাথে", "জন", "টাকা");
    putBigram("এই", "বিষয়ে", "জন্য", "সময়", "কথা", "কাজে");
    putBigram("সেই", "সাথে", "দিন", "সময়", "কথা", "মানুষ");
    putBigram("না", "হলে", "পারলে", "করে", "পেয়ে", "চাইলে");
    putBigram("করতে", "হবে", "চাই", "পারব", "পারে", "গিয়ে");
    putBigram("হতে", "পারে", "চেয়ে", "হবে", "পারেনি");
    putBigram("যাই", "হোক", "না", "কেন");
    putBigram("আল্লাহ", "হাফেজ", "ভরসা", "সহায়", "রহম");
    putBigram("আসসালামু", "আলাইকুম");
    putBigram("ওয়ালাইকুম", "আসসালাম");
    putBigram("ইনশা", "আল্লাহ");
    putBigram("মাশা", "আল্লাহ");
    putBigram("আলহামদুলিল্লাহ", "ভালো", "সব", "ঠিক", "আমি");
    putBigram("বাংলাদেশ", "একটি", "ক্রিকেট", "সরকার", "আমার");
    putBigram("আমার", "সোনার", "নাম", "কাছে", "মনে", "দেশ", "কথা", "বন্ধু");
    putBigram("আপনার", "নাম", "জন্য", "কাছে", "কথা", "ফোন", "দয়া");
    putBigram("তোমার", "নাম", "জন্য", "কাছে", "কথা", "বাড়ি", "সাথে");
    putBigram("ভাই", "কেমন", "আছেন", "একটু", "শুনুন", "কোথায়");
    putBigram("আজকে", "কি", "যাব", "হবে", "বৃষ্টি", "ছুটি", "দেখা");
    putBigram("কালকে", "দেখা", "হবে", "যাব", "কথা", "আসব");

    // --- English Multi-word Phrases ---
    putBigram("i love", "you", "it", "this", "them");
    putBigram("thank you", "so", "very", "much", "for");
    putBigram("thanks for", "the", "your", "all");
    putBigram("how are", "you", "things", "they");
    putBigram("where are", "you", "we", "they");
    putBigram("what are", "you", "the", "we");
    putBigram("let me", "know", "see", "check");
    putBigram("take care", "of", "always", "bro");
    putBigram("good morning", "to", "everyone", "all");
    putBigram("good night", "sweet", "dreams", "all");
    putBigram("nice to", "meet", "see", "hear");
    putBigram("see you", "soon", "tomorrow", "later");
    putBigram("look forward", "to", "hearing", "seeing");
    putBigram("as soon", "as", "possible");

    // --- English High-Frequency Transitions ---
    putBigram("how", "are", "do", "can", "is", "about");
    putBigram("what", "is", "are", "do", "about", "a");
    putBigram("why", "did", "do", "are", "is", "not");
    putBigram("where", "are", "is", "can", "do", "were");
    putBigram("when", "will", "can", "is", "did", "are");
    putBigram("who", "is", "are", "was", "can", "will");
    putBigram("i", "am", "will", "have", "want", "would", "think");
    putBigram("you", "are", "can", "have", "will", "know", "want");
    putBigram("we", "are", "will", "have", "can", "need", "should");
    putBigram("they", "are", "will", "have", "were", "can");
    putBigram("he", "is", "was", "will", "has", "can", "said");
    putBigram("she", "is", "was", "will", "has", "can", "said");
    putBigram("it", "is", "was", "will", "would", "can", "looks");
    putBigram("that", "is", "was", "would", "will", "sounds", "means");
    putBigram("this", "is", "was", "will", "way", "week", "one");
    putBigram("thank", "you", "god", "heavens");
    putBigram("thanks", "for", "a", "to", "again", "bro");
    putBigram("please", "let", "help", "send", "find", "call");
    putBigram("good", "morning", "night", "afternoon", "job", "luck");
    putBigram("nice", "to", "job", "meeting", "work", "pic");
    putBigram("see", "you", "it", "that", "what", "how");
    putBigram("let", "me", "us", "it", "him", "her");
    putBigram("have", "a", "been", "to", "you", "any");
    putBigram("can", "you", "i", "we", "be", "help");
    putBigram("could", "you", "be", "have", "not", "we");
    putBigram("would", "you", "be", "like", "have", "love");
    putBigram("should", "be", "have", "we", "you", "i");
    putBigram("do", "you", "not", "it", "that", "we");
    putBigram("does", "not", "it", "he", "she", "that");
    putBigram("did", "you", "not", "it", "he", "they");
    putBigram("will", "be", "do", "have", "get", "call");
    putBigram("want", "to", "you", "a", "it", "more");
    putBigram("need", "to", "a", "you", "help", "more");
    putBigram("going", "to", "be", "there", "out", "well");
    putBigram("look", "forward", "at", "like", "into", "for");
    putBigram("take", "care", "a", "it", "your", "time");
    putBigram("best", "regards", "wishes", "way", "friend", "luck");
    putBigram("of", "the", "a", "course", "my", "this");
    putBigram("in", "the", "a", "my", "this", "our");
    putBigram("to", "the", "be", "do", "you", "see", "get");
    putBigram("at", "the", "home", "work", "all", "least");
    putBigram("on", "the", "my", "your", "time", "this");
    putBigram("for", "the", "you", "your", "me", "this");
    putBigram("with", "you", "the", "me", "my", "this");
    putBigram("about", "the", "that", "this", "it", "you");
  }

  private void putBigram(String key, String... words)
  {
    _builtInBigrams.put(key.toLowerCase(Locale.ROOT), Arrays.asList(words));
  }

  private void loadLearnedBigrams()
  {
    if (_context == null) return;
    try
    {
      SharedPreferences prefs = _context.getSharedPreferences(PREF_LEARNED_BIGRAMS, Context.MODE_PRIVATE);
      Map<String, ?> all = prefs.getAll();
      for (Map.Entry<String, ?> entry : all.entrySet())
      {
        if (entry.getValue() instanceof String)
        {
          String raw = (String) entry.getValue();
          if (!raw.isEmpty())
          {
            String[] split = raw.split(",");
            List<String> list = new ArrayList<>();
            for (String s : split)
            {
              if (!s.trim().isEmpty()) list.add(s.trim());
            }
            if (!list.isEmpty())
            {
              _learnedBigrams.put(entry.getKey(), list);
            }
          }
        }
      }
    }
    catch (Exception ignored) {}
  }

  public synchronized void learn(String prevWord, String nextWord)
  {
    learnPhrase(prevWord, nextWord);
  }

  public synchronized void learn(String context, String prevWord, String nextWord)
  {
    if (context != null && !context.isEmpty())
    {
      learnPhrase(context, nextWord);
    }
    if (prevWord != null && !prevWord.isEmpty())
    {
      learnPhrase(prevWord, nextWord);
    }
  }

  private void learnPhrase(String trigger, String nextWord)
  {
    if (trigger == null || nextWord == null) return;
    String cleanTrigger = cleanWord(trigger);
    String cleanNext = nextWord.trim();
    if (cleanTrigger.isEmpty() || cleanNext.isEmpty()) return;

    List<String> list = _learnedBigrams.get(cleanTrigger);
    if (list == null)
    {
      list = new ArrayList<>();
      _learnedBigrams.put(cleanTrigger, list);
    }
    list.remove(cleanNext);
    list.add(0, cleanNext); // Most recent first
    while (list.size() > MAX_LEARNED_PER_WORD)
    {
      list.remove(list.size() - 1);
    }

    if (_context != null)
    {
      try
      {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++)
        {
          if (i > 0) sb.append(",");
          sb.append(list.get(i));
        }
        _context.getSharedPreferences(PREF_LEARNED_BIGRAMS, Context.MODE_PRIVATE)
            .edit()
            .putString(cleanTrigger, sb.toString())
            .apply();
      }
      catch (Exception ignored) {}
    }
  }

  public List<String> predict(String prevWord, int maxResults)
  {
    return predict(null, prevWord, maxResults);
  }

  public List<String> predict(String context, String prevWord, int maxResults)
  {
    if (maxResults <= 0) return Collections.emptyList();
    LinkedHashSet<String> result = new LinkedHashSet<>();

    // 1. Check context phrase (e.g. "তোমাকে ভালো")
    if (context != null && !context.isEmpty())
    {
      String cleanCtx = cleanWord(context);
      if (!cleanCtx.isEmpty())
      {
        List<String> learnedCtx = _learnedBigrams.get(cleanCtx);
        if (learnedCtx != null)
        {
          for (String w : learnedCtx)
          {
            result.add(w);
            if (result.size() >= maxResults) return new ArrayList<>(result);
          }
        }
        List<String> builtInCtx = _builtInBigrams.get(cleanCtx);
        if (builtInCtx != null)
        {
          for (String w : builtInCtx)
          {
            result.add(w);
            if (result.size() >= maxResults) return new ArrayList<>(result);
          }
        }
      }
    }

    // 2. Check single word (e.g. "ভালো")
    String cleanPrev = cleanWord(prevWord);
    if (!cleanPrev.isEmpty())
    {
      List<String> learned = _learnedBigrams.get(cleanPrev);
      if (learned != null)
      {
        for (String w : learned)
        {
          result.add(w);
          if (result.size() >= maxResults) return new ArrayList<>(result);
        }
      }

      List<String> builtIn = _builtInBigrams.get(cleanPrev);
      if (builtIn != null)
      {
        for (String w : builtIn)
        {
          result.add(w);
          if (result.size() >= maxResults) return new ArrayList<>(result);
        }
      }
    }

    // 3. Fallbacks based on detected script (Bengali vs Latin)
    boolean isBengali = isBengaliScript(prevWord) || isBengaliScript(context);
    if (isBengali)
    {
      String[] defaults = {"এবং", "না", "ভালো", "করে", "একটি", "হবে", "আছি", "যাব", "হলে", "থেকে", "দিয়ে", "আছে", "করব", "চাই", "ছিল"};
      for (String d : defaults)
      {
        result.add(d);
        if (result.size() >= maxResults) break;
      }
    }
    else
    {
      String[] defaults = {"the", "to", "and", "is", "you", "in", "it", "of", "for", "that", "this", "on", "with", "be", "have"};
      for (String d : defaults)
      {
        result.add(d);
        if (result.size() >= maxResults) break;
      }
    }

    return new ArrayList<>(result);
  }

  private String cleanWord(String word)
  {
    if (word == null) return "";
    String w = word.trim();
    // Strip trailing punctuation
    while (!w.isEmpty())
    {
      char last = w.charAt(w.length() - 1);
      if (last == '.' || last == ',' || last == '?' || last == '!' || last == ';' || last == ':' || last == '।' || last == '"' || last == '\'')
      {
        w = w.substring(0, w.length() - 1).trim();
      }
      else
      {
        break;
      }
    }
    return w.toLowerCase(Locale.ROOT);
  }

  private boolean isBengaliScript(String word)
  {
    if (word == null) return false;
    for (int i = 0; i < word.length(); i++)
    {
      char c = word.charAt(i);
      if (c >= '\u0980' && c <= '\u09FF')
      {
        return true;
      }
    }
    return false;
  }
}
