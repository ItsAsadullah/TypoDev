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
import juloo.keyboard2.Config;

/**
 * High-performance Sentence-Level & Next-Word Predictor supporting both Bengali & English
 * collocations, sentence boundaries, trigram/4-gram matching, and adaptive sequence learning.
 */
public class NextWordPredictor
{
  private static final String PREF_LEARNED_BIGRAMS = "learned_bigrams_store";
  private static final int MAX_LEARNED_PER_WORD = 6;

  private static NextWordPredictor _instance;

  public static synchronized NextWordPredictor instance(Context context)
  {
    if (_instance == null)
    {
      _instance = new NextWordPredictor(context != null ? context.getApplicationContext() : null);
    }
    else if (_instance._context == null && context != null)
    {
      _instance._context = context.getApplicationContext();
      _instance.loadLearnedBigrams();
    }
    return _instance;
  }

  private Context _context;
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
    // =========================================================================
    // Bengali 4-gram and Multi-word Sentence Completions
    // =========================================================================
    putBigram("আমি তোমাকে অনেক", "ভালোবাসি", "ধন্যবাদ জানাই", "মনে করি", "বিশ্বাস করি");
    putBigram("আমি তোমাকে খুব", "ভালোবাসি", "পছন্দ করি", "শ্রদ্ধা করি");
    putBigram("আমি তোমাকে ভালো", "বাসি", "জানি", "রেখেছি");
    putBigram("আমি তোমাকে ভালোবাসি", "অনেক", "খুব", "সবসময়");
    putBigram("আজকে কি ছুটি", "আছে", "হবে", "নাকি");
    putBigram("আজকে কি তুমি", "যাবে", "আসবে", "ফ্রি আছো");
    putBigram("কালকে দেখা হবে", "ইনশা আল্লাহ", "আমাদের", "আবার");
    putBigram("দেখা হবে কালকে", "ইনশা আল্লাহ", "আবার");
    putBigram("কোনো সমস্যা নেই", "ভাই", "আপু", "ঠিক আছে");
    putBigram("ইনশা আল্লাহ সব", "ঠিক হবে", "ভালো হবে", "হয়ে যাবে");
    putBigram("আলহামদুলিল্লাহ আমি ভালো", "আছি", "সব ঠিক");
    putBigram("অনেক অনেক ধন্যবাদ", "আপনাকে", "তোমাকে", "ভাই");
    putBigram("অনেক শুভকামনা রইল", "তোমার জন্য", "আপনার জন্য");
    putBigram("শুভ জন্মদিন ভাই", "অনেক শুভেচ্ছা", "দোয়া রইল");

    // =========================================================================
    // Bengali Trigram Patterns (Last 2 Words)
    // =========================================================================
    putBigram("তোমাকে ভালো", "বাসি", "লাগে", "রেখে", "বাসবে");
    putBigram("খুব ভালো", "লাগল", "হয়েছে", "বাসি", "মানুষ", "ছিল", "হবে", "কাটুক");
    putBigram("অনেক ভালো", "লেগেছে", "হয়েছে", "থাকবেন", "মানুষ", "থেকো", "লাগছে");
    putBigram("আমি তোমাকে", "ভালোবাসি", "অনেক ভালোবাসি", "খুব ভালোবাসি", "বলছি", "ধন্যবাদ জানাই", "বিশ্বাস করি");
    putBigram("আমি আপনাকে", "ধন্যবাদ জানাই", "শ্রদ্ধা করি", "বলছি", "ফোন দেব");
    putBigram("আমি ভালো", "আছি", "নেই", "করব", "থাকব");
    putBigram("আমি এখন", "ব্যস্ত", "বাসায়", "অফিসে", "যাব", "আসি", "বের হচ্ছি", "একটু কাজে আছি");
    putBigram("আমি কি", "আসতে পারি", "করব", "জানতে পারি", "বলতে পারি", "সাহায্য করতে পারি");
    putBigram("আমি তো", "জানতাম না", "যাব", "আছি");
    putBigram("আমরা সবাই", "ভালো আছি", "যাব", "একসাথে", "আছি", "করব");
    putBigram("তুমি কেমন", "আছো", "বোধ করছ", "আছ");
    putBigram("তুমি কোথায়", "আছো", "যাবে", "যাচ্ছ", "গেলে");
    putBigram("তুমি কি", "যাবে", "করছ", "আসবে", "খাবে", "পারবে", "শুনেছ", "এখন ব্যস্ত");
    putBigram("তুমি কখন", "আসবে", "যাবে", "পৌঁছাবে", "ফ্রি হবে");
    putBigram("আপনি কেমন", "আছেন", "অনুভব করছেন");
    putBigram("আপনি কোথায়", "আছেন", "যাবেন", "থাকেন");
    putBigram("আপনি কি", "আসবেন", "করছেন", "বলবেন", "পারবেন", "ব্যস্ত আছেন");
    putBigram("আপনি কখন", "আসবেন", "ফ্রি হবেন", "পৌঁছাবেন");
    putBigram("তুই কেমন", "আছিস", "করছিস");
    putBigram("তুই কোথায়", "আছিস", "যাবি");
    putBigram("তুই কি", "যাবি", "করছিস", "আসবির");
    putBigram("অনেক ধন্যবাদ", "আপনাকে", "তোমাকে", "ভাই", "সবাইকে");
    putBigram("ধন্যবাদ আপনাকে", "অনেক", "ভাই", "সবাইকে");
    putBigram("ধন্যবাদ তোমাকে", "অনেক", "বন্ধু", "ভাই");
    putBigram("শুভ সকাল", "সবাইকে", "বন্ধু", "ভাই", "ভালো কাটুক দিনটি");
    putBigram("শুভ জন্মদিন", "তোমাকে", "অনেক অনেক শুভকামনা", "ভাই", "দোয়া রইল");
    putBigram("শুভ রাত্রি", "সবাইকে", "ভালো থেকো", "ঘুমিয়ে পড়ো");
    putBigram("ঈদ মোবারক", "সবাইকে", "আপনাকে", "বন্ধু");
    putBigram("কি খবর", "তোমার", "আপনার", "ভাই", "কেমন চলছে");
    putBigram("কি অবস্থা", "তোমার", "আপনার", "ভাই", "কাজের");
    putBigram("দেখা হবে", "আবার", "কালকে", "শীঘ্রই", "কথা হবে");
    putBigram("কথা হবে", "পরে", "কালকে", "আবার", "রাতে");
    putBigram("ভালোবাসি তোমাকে", "অনেক", "খুব", "সবসময়");
    putBigram("ভালো থেকো", "সবসময়", "দোয়া রইল", "বন্ধু");
    putBigram("ভালো থাকবেন", "সবসময়", "সুস্থ থাকবেন", "দোয়া করবেন");
    putBigram("একটু পরে", "ফোন দিচ্ছি", "কথা বলছি", "আসছি", "যাব");
    putBigram("একটু পর", "আসছি", "ফোন দিচ্ছি", "কথা বলছি", "যাব");
    putBigram("দেরি হয়ে", "গেছে", "গেল");
    putBigram("আজকে কি", "যাবে", "করবে", "ছুটি", "হবে", "বৃষ্টি হবে", "দেখা হবে");
    putBigram("আজকে অনেক", "কাজ", "গরম", "বৃষ্টি", "সুন্দর দিন");
    putBigram("কালকে দেখা", "হবে", "করব", "হতে পারে");
    putBigram("কালকে কি", "দেখা হবে", "যাবে", "আসবে", "ছুটি");
    putBigram("মনে হয়", "না", "হবে", "ঠিক", "হচ্ছে");
    putBigram("কোনো সমস্যা", "নেই", "হবে না", "আছে কি");
    putBigram("কিছু বলতে", "চাও", "চান", "হবে");
    putBigram("করতে হবে", "এখনই", "হবে", "পারে");
    putBigram("হতে পারে", "এমন", "না", "হবে");
    putBigram("যে কোনো", "সময়", "কিছু", "মূল্যে");
    putBigram("সব কিছু", "ঠিক আছে", "ভালো", "হয়ে যাবে");
    putBigram("সব ঠিক", "আছে", "হয়ে যাবে", "থাকবে");
    putBigram("ঠিক আছে", "ধন্যবাদ", "ভাই", "দেখা হবে");
    putBigram("ইনশা আল্লাহ", "সব ঠিক হবে", "দেখা হবে", "ভালো হবে", "যাব");
    putBigram("মাশা আল্লাহ", "অনেক সুন্দর", "খুব ভালো", "আলহামদুলিল্লাহ");
    putBigram("আলহামদুলিল্লাহ আমি", "ভালো আছি", "সব ঠিক");
    putBigram("আল্লাহ হাফেজ", "ভালো থেকো", "ভালো থাকবেন");
    putBigram("আসসালামু আলাইকুম", "ওয়া রাহমাতুল্লাহ", "ভাই", "কেমন আছেন");
    putBigram("ওয়ালাইকুম আসসালাম", "কেমন আছেন", "ওয়া রাহমাতুল্লাহ");
    putBigram("জাযাকাল্লাহ", "খাইরান");

    // =========================================================================
    // Bengali Bigram Patterns (Single Word Context)
    // =========================================================================
    putBigram("ভালো", "বাসি", "লাগে", "থেকো", "থাকবেন", "আছি", "আছো", "আছেন", "লেগেছে", "করব", "মানুষ", "বাসার", "হবে", "বাসবে", "কাটুক");
    putBigram("তোমাকে", "ভালো", "ভালোবাসি", "অনেক", "ধন্যবাদ", "বলতে", "ছাড়া", "নিয়ে", "একটি");
    putBigram("ভালোবাসি", "তোমাকে", "অনেক", "সবসময়", "খুব");
    putBigram("ভালোবাসা", "অবিরাম", "দিও", "নিও");
    putBigram("আমি", "তোমাকে", "ভালো", "তোমাদের", "আছি", "যাব", "করব", "চাই", "বলছি", "একটু", "এখন");
    putBigram("তুমি", "কেমন", "কি", "কোথায়", "আছো", "যাবে", "ভালো", "পারবে", "আমার", "কবে");
    putBigram("তুই", "কেমন", "কি", "কোথায়", "আছিস", "যাবি", "আয়", "কবে");
    putBigram("আপনি", "কেমন", "কি", "কোথায়", "আছেন", "বলুন", "ভালো", "পারবেন", "আসবেন");
    putBigram("আমরা", "সবাই", "করতে", "যাব", "চাই", "ভালো", "থাকব");
    putBigram("কেমন", "আছেন", "আছো", "আছিস", "হলো", "লাগল", "চলছে");
    putBigram("অনেক", "ধন্যবাদ", "ভালোবাসা", "সুন্দর", "ভালো", "দিন", "কষ্ট", "টাকা", "বেশি");
    putBigram("শুভ", "সকাল", "রাত্রি", "জন্মদিন", "কামনা", "দিন", "ঈদ");
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

    // =========================================================================
    // English 4-gram and Multi-word Sentence Completions
    // =========================================================================
    putBigram("what are you doing", "today", "now", "tonight");
    putBigram("what are you", "doing", "talking about", "thinking", "looking for");
    putBigram("where are you", "going", "now", "from", "located");
    putBigram("how are you doing", "today", "bro", "my friend");
    putBigram("how are you", "doing", "today", "feeling", "doing today");
    putBigram("thank you so much", "for your help", "for everything", "bro", "my friend");
    putBigram("thank you so", "much", "very much");
    putBigram("thank you for", "your help", "everything", "the support", "coming");
    putBigram("thanks for", "the help", "your help", "the support", "everything", "reaching out");
    putBigram("let me know if", "you need anything", "you have questions");
    putBigram("let me know", "if you need anything", "what you think", "when you are ready", "soon");
    putBigram("as soon as", "possible", "you can");
    putBigram("as soon as possible", "please");
    putBigram("looking forward to", "hearing from you", "seeing you", "meeting you");
    putBigram("look forward to", "hearing from you", "seeing you", "working with you");
    putBigram("nice to meet you", "too", "all", "finally");
    putBigram("it was nice to", "meet you", "talk to you", "see you");
    putBigram("i would like to", "thank you", "know", "see", "invite you");
    putBigram("i want to", "know", "see", "go", "thank you", "talk to you");
    putBigram("can you please", "help me", "send me", "let me know", "check");
    putBigram("could you please", "let me know", "send", "help me", "check");
    putBigram("please let me", "know", "see");
    putBigram("don't worry about", "it", "that");
    putBigram("don't worry", "about it", "be happy");
    putBigram("see you soon", "brother", "friend");
    putBigram("take care of", "yourself", "your health");
    putBigram("i will be there", "soon", "in 5 minutes");
    putBigram("i am on my way", "home", "now", "there");

    // =========================================================================
    // English Trigram Patterns (Last 2 Words)
    // =========================================================================
    putBigram("how are", "you", "things", "they");
    putBigram("where are", "you", "we", "they");
    putBigram("what are", "you", "the", "we", "your");
    putBigram("what do", "you", "we", "they");
    putBigram("let me", "know", "see", "check", "tell");
    putBigram("take care", "of", "always", "bro");
    putBigram("good morning", "everyone", "to you", "all", "have a great day");
    putBigram("good night", "sweet dreams", "everyone", "all", "sleep tight");
    putBigram("nice to", "meet you", "see you", "hear from you");
    putBigram("see you", "soon", "tomorrow", "later");
    putBigram("thank you", "so", "very", "much", "so much", "very much", "for your help", "for everything");
    putBigram("have a", "great day", "good day", "nice day", "great time", "good time");

    // =========================================================================
    // English Bigram Patterns (Single Word Context)
    // =========================================================================
    putBigram("how", "are", "do", "can", "is", "about");
    putBigram("what", "is", "are", "do", "about", "a", "happened");
    putBigram("why", "did", "do", "are", "is", "not");
    putBigram("where", "are", "is", "can", "do", "were");
    putBigram("when", "will", "can", "is", "did", "are");
    putBigram("who", "is", "are", "was", "can", "will");
    putBigram("i", "am", "will", "have", "want", "would", "think", "love");
    putBigram("you", "are", "can", "have", "will", "know", "want");
    putBigram("we", "are", "will", "have", "can", "need", "should");
    putBigram("they", "are", "will", "have", "were", "can");
    putBigram("he", "is", "was", "will", "has", "can", "said");
    putBigram("she", "is", "was", "will", "has", "can", "said");
    putBigram("it", "is", "was", "will", "would", "can", "looks");
    putBigram("that", "is", "was", "would", "will", "sounds", "means");
    putBigram("this", "is", "was", "will", "way", "week", "one");
    putBigram("thank", "you", "god", "heavens");
    putBigram("thanks", "for", "a lot", "to", "again", "bro");
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

    // Common English Pronouns, Nouns, and Verb transitions
    putBigram("father", "is", "was", "said", "in", "to", "told", "will", "has", "and", "called");
    putBigram("your father", "is", "was", "said", "told", "will", "called", "and");
    putBigram("my father", "is", "was", "said", "told", "will", "called", "and");
    putBigram("mother", "is", "was", "said", "in", "to", "told", "will", "has", "and");
    putBigram("brother", "is", "was", "and", "will", "said", "in");
    putBigram("sister", "is", "was", "and", "will", "said", "in");
    putBigram("friend", "is", "was", "and", "forever", "in", "will");
    putBigram("assignment", "is", "was", "done", "due", "submission", "ready", "completed", "today", "tomorrow");
    putBigram("knowledge", "is", "and", "base", "sharing", "about", "of");
    putBigram("school", "is", "was", "starts", "bus", "today", "tomorrow");
    putBigram("college", "is", "was", "starts", "life", "campus", "today");
    putBigram("university", "of", "is", "campus", "life", "exam");
    putBigram("office", "today", "tomorrow", "work", "hours", "meeting", "is");
    putBigram("work", "is", "from", "done", "hard", "together", "on");
    putBigram("job", "is", "done", "search", "well done", "offer");
    putBigram("project", "is", "work", "done", "report", "file", "deadline");
    putBigram("problem", "is", "solved", "with", "here");
    putBigram("system", "is", "was", "design", "update", "working");
    putBigram("website", "is", "design", "link", "online");
    putBigram("today", "is", "was", "at", "we", "i", "and", "night");
    putBigram("tomorrow", "is", "at", "morning", "we", "i", "night", "evening");
    putBigram("time", "to", "is", "for", "and", "will", "has");
    putBigram("day", "to", "is", "was", "by", "today", "ahead");
    putBigram("year", "old", "is", "was", "ago", "end");

    // English Contractions transitions
    putBigram("im", "your", "going", "sorry", "here", "fine", "ready", "sure", "trying", "waiting", "happy", "doing");
    putBigram("i'm", "your", "going", "sorry", "here", "fine", "ready", "sure", "trying", "waiting", "happy", "doing");
    putBigram("im your", "father", "friend", "brother", "boss", "fan");
    putBigram("i'm your", "father", "friend", "brother", "boss", "fan");
    putBigram("dont", "know", "worry", "think", "have", "care", "want", "like", "be");
    putBigram("don't", "know", "worry", "think", "have", "care", "want", "like", "be");
    putBigram("cant", "wait", "do", "believe", "see", "find", "stop");
    putBigram("can't", "wait", "do", "believe", "see", "find", "stop");
    putBigram("wont", "be", "do", "happen", "let", "work");
    putBigram("won't", "be", "do", "happen", "let", "work");
    putBigram("its", "a", "very", "good", "fine", "okay", "time", "not", "been");
    putBigram("it's", "a", "very", "good", "fine", "okay", "time", "not", "been");
    putBigram("thats", "great", "good", "nice", "right", "true", "fine", "cool", "it");
    putBigram("that's", "great", "good", "nice", "right", "true", "fine", "cool", "it");
    putBigram("youre", "welcome", "right", "great", "the best", "doing well");
    putBigram("you're", "welcome", "right", "great", "the best", "doing well");
    putBigram("ill", "be", "call", "do", "let", "see", "come", "send");
    putBigram("i'll", "be", "call", "do", "let", "see", "come", "send");
    putBigram("ive", "been", "got", "done", "seen", "had");
    putBigram("i've", "been", "got", "done", "seen", "had");

    // Possessives & Greetings
    putBigram("my", "father", "mother", "friend", "brother", "name", "phone", "dear", "love", "work", "home", "way", "life");
    putBigram("your", "father", "mother", "name", "help", "time", "phone", "message", "email", "order", "account", "friend", "welcome");
    putBigram("our", "team", "family", "work", "project", "country", "life");
    putBigram("his", "name", "father", "mother", "friend", "life", "work");
    putBigram("her", "name", "father", "mother", "friend", "life", "work");
    putBigram("hello", "how", "everyone", "brother", "friend", "there", "sir", "world");
    putBigram("hi", "how", "there", "everyone", "brother", "friend", "all");
    putBigram("hey", "how", "there", "what's up", "bro", "man", "buddy");
    putBigram("yes", "i", "you", "we", "it", "sure", "please", "of course");
    putBigram("no", "problem", "worries", "way", "one", "thanks");
    putBigram("ok", "sure", "thanks", "got it", "done", "i will");
    putBigram("okay", "sure", "thanks", "got it", "done", "no problem");
    putBigram("sure", "i", "thing", "no problem", "we can");
    putBigram("call", "me", "you", "back", "him", "her", "later");
    putBigram("message", "me", "you", "sent", "received");

    // Phrases and Question Starters
    putBigram("how can", "i", "we", "you", "help");
    putBigram("how can i", "help", "assist", "get", "do");
    putBigram("how can i help", "you", "you today", "today");
    putBigram("can i", "help", "get", "have", "call", "ask");
    putBigram("can i help", "you", "with something", "today");
    putBigram("help", "you", "me", "us", "with", "needed");
    putBigram("help you", "today", "with", "out");

    // Auxiliary & Linking Verbs
    putBigram("is", "a", "the", "not", "very", "good", "it", "there", "this", "ready", "available", "here");
    putBigram("are", "you", "the", "they", "we", "there", "not", "ready", "doing");
    putBigram("am", "a", "in", "at", "going", "doing", "very", "so", "ready", "here");
    putBigram("was", "a", "the", "very", "not", "good", "there", "great");
    putBigram("were", "you", "they", "we", "there");
    putBigram("the", "best", "first", "new", "way", "same", "world", "time", "day", "next");
    putBigram("a", "lot", "great", "good", "few", "new", "little", "big", "bit");
    putBigram("and", "the", "i", "you", "we", "also", "then", "it");
    putBigram("so", "much", "good", "that", "happy", "sorry", "many");
    putBigram("very", "much", "good", "nice", "happy", "well", "important");
    putBigram("there", "is", "are", "was", "will", "were");
    putBigram("here", "is", "are", "you", "to", "we");
    putBigram("just", "now", "a", "wanted", "let", "like", "need");
    putBigram("now", "i", "you", "we", "and");

    // Bengali Nouns, Family, and Conversational Bigrams
    putBigram("বাবা", "বললেন", "কেমন", "আছেন", "ছিলেন", "যাবেন", "এসেছেন", "এবং", "সাথে");
    putBigram("মা", "বললেন", "কেমন", "আছেন", "ছিলেন", "ডাকছেন", "এবং");
    putBigram("ভাই", "কেমন", "আছেন", "একটু", "শুনুন", "কোথায়", "হবে");
    putBigram("বন্ধু", "কেমন", "আছিস", "আছো", "চল", "দেখা", "হবে");
    putBigram("কাজ", "করছি", "হয়ে", "গেছে", "করব", "আছে", "শেষ");
    putBigram("ভালো", "বাসি", "আছি", "থাকবেন", "হয়েছে", "লাগল", "থেকো", "আছো", "আছেন");
    putBigram("ভালোবাসি", "তোমাকে", "অনেক", "খুব", "সবসময়");
    putBigram("তোমাকে ভালোবাসি", "অনেক", "সবসময়", "প্রিয়");
    putBigram("আমি তোমাকে ভালোবাসি", "অনেক", "সবসময়");
    putBigram("স্কুল", "ছুটি", "যাব", "শুরু", "হবে");
    putBigram("কলেজ", "জীবন", "ছুটি", "যাব", "হবে");
    putBigram("অফিস", "যাব", "আসব", "কাজ", "শেষ", "ছুটি");
    putBigram("বাড়ি", "যাব", "আসব", "আছে", "পৌঁছে");
    putBigram("বাসা", "যাব", "আসব", "পৌঁছে", "কোথায়");
    putBigram("সময়", "হবে", "নেই", "হলে", "মতো", "দিন");
    putBigram("টাকা", "দাও", "হবে", "নেই", "পাঠাও");
    putBigram("ফোন", "দাও", "করব", "করো", "নাম্বার");
    putBigram("কথা", "বলব", "বলুন", "হবে", "শুনুন");
    putBigram("দেখা", "হবে", "করব", "হয়েছে", "করুন");
    putBigram("দেখা হবে", "কালকে", "শীঘ্রই", "বন্ধু", "ভাই");

    // =========================================================================
    // Conversational Question Starters & Everyday English Phrases
    // =========================================================================
    putBigram("how much", "is", "does", "will", "would", "for");
    putBigram("how long", "will it take", "have you", "is it", "ago");
    putBigram("how was", "your day", "the meeting", "the exam", "the trip", "it");
    putBigram("how is", "it going", "your family", "work", "everything", "your health");
    putBigram("how is it", "going", "working", "looking");
    putBigram("what time", "is it", "do we meet", "does it start", "will you come");
    putBigram("what happened", "to you", "there", "yesterday", "next");
    putBigram("what did", "you say", "he say", "you do", "you mean", "they want");
    putBigram("what do you", "think", "mean", "want", "suggest", "need", "do");
    putBigram("what kind of", "job", "work", "thing", "help", "project");
    putBigram("where can i", "find", "get", "see", "buy", "download");
    putBigram("where do you", "live", "work", "want to go", "stay");
    putBigram("where were you", "yesterday", "last night", "today");
    putBigram("when can we", "meet", "talk", "start", "discuss");
    putBigram("when are you", "coming", "leaving", "free", "available");
    putBigram("why did you", "do that", "say that", "leave", "call");
    putBigram("why are you", "late", "here", "calling", "crying", "so happy");
    putBigram("who told you", "that", "about this");
    putBigram("can i have", "your number", "a look", "some water", "this");
    putBigram("can we meet", "today", "tomorrow", "later", "this evening");
    putBigram("could you send", "me the file", "me the link", "it to me");
    putBigram("would you like", "to join", "to come", "something to drink", "to know");
    putBigram("should we", "go", "wait", "start", "call");
    putBigram("do you know", "what", "how", "who", "if", "where");
    putBigram("do you have", "time", "any questions", "a moment", "the link");
    putBigram("have you seen", "my phone", "this", "him", "her");
    putBigram("have you heard", "about", "the news");
    putBigram("are you sure", "about that", "you can", "you want");
    putBigram("are you ready", "to go", "for this", "now");

    // Everyday Polite Communication & Work
    putBigram("hope you are", "doing well", "having a great day", "fine", "safe");
    putBigram("hope all is", "well with you", "good");
    putBigram("sorry for the", "delay", "late reply", "inconvenience", "trouble");
    putBigram("sorry i am", "late", "busy right now", "not available");
    putBigram("i am sorry", "for that", "to hear that", "i cannot");
    putBigram("feel free to", "ask", "reach out", "contact me", "call");
    putBigram("please let me know", "if you have any questions", "your thoughts", "when you are available");
    putBigram("please find", "attached", "the document", "the link", "here");
    putBigram("i will let you know", "as soon as possible", "soon", "tomorrow");
    putBigram("i will be", "there soon", "waiting for you", "happy to help", "ready");
    putBigram("i will call you", "back later", "soon", "tonight", "when i arrive");
    putBigram("call me when", "you get home", "you are free", "you arrive");
    putBigram("talk to you", "later", "soon", "tomorrow");
    putBigram("keep in touch", "with me", "always");
    putBigram("all the best", "for your exam", "for the future", "brother", "friend");
    putBigram("best of luck", "with that", "for tomorrow", "to you");
    putBigram("happy new year", "to you and your family");
    putBigram("happy birthday to", "you my friend", "you", "the best");
    putBigram("congratulations on", "your success", "your new job", "your graduation");
    putBigram("sounds like a", "great plan", "good idea");
    putBigram("sounds good to", "me");
    putBigram("no problem at", "all");
    putBigram("take care of yourself", "and stay safe", "always");
    putBigram("have a great weekend", "ahead");
    putBigram("i would love", "to see you", "to come", "to help");
    putBigram("i think that", "it is a great idea", "we should go", "you are right");
    putBigram("i don't think", "so", "it will work", "we can");
    putBigram("i don't know", "what to say", "about that", "yet");
    putBigram("i am looking for", "a new", "the file", "my keys", "help");
    putBigram("i am trying to", "fix this", "understand", "finish this");
    putBigram("i am waiting for", "your reply", "the bus", "you");
    putBigram("we need to", "talk", "discuss", "finish this", "go");
    putBigram("we are going to", "start", "win", "the party");
    putBigram("he said that", "he will come", "it is okay", "he is ready");
    putBigram("she told me", "about it", "that she is busy", "yesterday");

    // =========================================================================
    // Rich Conversational Bengali Multi-word & Trigrams
    // =========================================================================
    putBigram("কী অবস্থা", "সবার", "তোমার", "আপনার", "ভাই", "কাজের");
    putBigram("কী খবর", "তোমার", "আপনার", "দোস্ত", "কেমন আছো");
    putBigram("কী করছ", "এখন", "তুমি", "বলো", "দোস্ত");
    putBigram("কী করছেন", "এখন", "আপনি", "বলুন");
    putBigram("কোথায় আছ", "এখন", "তুমি", "বলো");
    putBigram("কোথায় আছেন", "এখন", "আপনি");
    putBigram("কোথায় যাচ্ছ", "তুমি", "এখন");
    putBigram("কখন আসবে", "তুমি", "বাসায়", "এখানে");
    putBigram("কখন আসবেন", "আপনি", "অফিসে");
    putBigram("কেমন চলছে", "সব কিছু", "পড়াশোনা", "কাজকর্মে");
    putBigram("দেরি হওয়ার জন্য", "দুঃখিত", "ক্ষমা করবেন");
    putBigram("দেরি হয়ে গেছে", "ভাই", "দেরি হয়ে গেল");
    putBigram("একটু পর আসছি", "একটু পর ফোন দিচ্ছি", "পরে কথা বলছি");
    putBigram("বাসায় পৌঁছে", "ফোন দিচ্ছি", "জানাব", "গেছি");
    putBigram("এখন আমি", "ব্যস্ত আছি", "অফিসে", "বাসায়", "বাইরে");
    putBigram("পরে কথা হবে", "কালকে সকালে", "রাতে", "বন্ধু");
    putBigram("সময় মতো", "পৌঁছে গেছি", "চলে আসব", "হবে");
    putBigram("আজকে কি দেখা হবে", "আমাদের");
    putBigram("কালকে সকালে দেখা করব", "ইনশাআল্লাহ");
    putBigram("একটু সাহায্য", "করতে পারবেন", "লাগবে", "চাই");
    putBigram("একটু শুনুন", "ভাই", "কথা ছিল");
    putBigram("কিছু মনে করবেন না", "প্লিজ");
    putBigram("দয়া করে একটু", "শুনুন", "বলবেন কি");
    putBigram("আমি তোমাকে অনেক ভালোবাসি", "সবসময় ভালোবাসি");
    putBigram("আমি তোমাকে খুব ভালোবাসি", "খুব মনে পড়ছে");
    putBigram("তোমাকে খুব মনে পড়ছে", "বন্ধু", "প্রিয়");
    putBigram("অনেক অনেক ধন্যবাদ", "আপনাকে", "তোমাকে", "সবাইকে");
    putBigram("অনেক শুভকামনা রইল", "তোমার জন্য", "আপনার ভবিষ্যতের জন্য");
    putBigram("শুভ জন্মদিন প্রিয়", "বন্ধু", "ভাই", "অনেক অনেক দোয়া রইল");
    putBigram("ঈদ মোবারক সবাইকে", "অনেক শুভেচ্ছা");
    putBigram("নিজের যত্ন নিও", "সবসময়");
    putBigram("সুস্থ থেকো ভালো থেকো", "সবসময়");
    putBigram("ইনশা আল্লাহ সব ঠিক হবে", "কোনো চিন্তা করো না");
    putBigram("মাশা আল্লাহ অনেক সুন্দর হয়েছে", "খুব ভালো লাগল");
    putBigram("আলহামদুলিল্লাহ আমি ভালো আছি", "সুস্থ আছি");
    putBigram("আল্লাহ আপনার মঙ্গল করুক", "সহায় হোন");
    putBigram("দোয়া করবেন আমার জন্য", "সবাই");
    putBigram("আল্লাহ হাফেজ ভালো থেকো", "সুস্থ থেকো");
    putBigram("কাজটি শেষ হয়ে গেছে", "কাজটি করতে হবে");
    putBigram("অ্যাসাইনমেন্ট জমা দিয়েছি", "অ্যাসাইনমেন্ট শেষ হয়েছে");
    putBigram("পরীক্ষা কেমন হলো", "পরীক্ষা খুব ভালো হয়েছে");
    putBigram("স্কুল ছুটি হয়ে গেছে", "কলেজে যাচ্ছি এখন");
    putBigram("অফিসে আছি এখন", "কাজে অনেক ব্যস্ত");
    putBigram("টাকা পাঠিয়ে দিয়েছি", "পেয়েছ কি");
    putBigram("ফোন নাম্বারটা দাও", "ফোন দিচ্ছি");
    putBigram("আমার প্রিয় বন্ধু", "আমার সাথে আসো", "আমার খুব ভালো লাগছে");
    putBigram("তোমার সাথে দেখা করব", "তোমার কথা খুব মনে পড়ছে");
    putBigram("আপনার সাথে কথা বলতে চাই", "আপনার সময় হবে কি");
  }

  private void putBigram(String key, String... words)
  {
    String k = key.toLowerCase(Locale.ROOT);
    List<String> existing = _builtInBigrams.get(k);
    if (existing == null)
    {
      _builtInBigrams.put(k, new ArrayList<>(Arrays.asList(words)));
    }
    else
    {
      List<String> merged = new ArrayList<>(existing);
      for (String w : words)
      {
        if (!merged.contains(w))
        {
          merged.add(w);
        }
      }
      _builtInBigrams.put(k, merged);
    }
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

  public synchronized void clearLearnedData()
  {
    _learnedBigrams.clear();
    if (_context != null)
    {
      try
      {
        _context.getSharedPreferences(PREF_LEARNED_BIGRAMS, Context.MODE_PRIVATE)
            .edit().clear().apply();
      }
      catch (Exception ignored) {}
    }
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

  /**
   * Learns transitions across 2-word, 3-word, and 4-word sequences.
   */
  public synchronized void learnSequence(List<String> words)
  {
    if (words == null || words.size() < 2) return;
    int len = words.size();
    for (int i = 1; i < len; i++)
    {
      String nextWord = words.get(i);
      String prevWord = words.get(i - 1);

      learnPhrase(prevWord, nextWord);

      if (i >= 2)
      {
        String prevPrevWord = words.get(i - 2);
        learnPhrase(prevPrevWord + " " + prevWord, nextWord);
      }

      if (i >= 3)
      {
        String prev3Word = words.get(i - 3);
        String prev2Word = words.get(i - 2);
        learnPhrase(prev3Word + " " + prev2Word + " " + prevWord, nextWord);
      }
    }
  }

  /**
   * Extracts active words from text preceding the cursor and learns the sequence.
   */
  public synchronized void learnSentence(String textBeforeCursor)
  {
    if (textBeforeCursor == null || textBeforeCursor.trim().isEmpty()) return;
    String activeSentence = extractActiveSentence(textBeforeCursor);
    List<String> tokens = tokenizeWords(activeSentence);
    if (tokens.size() >= 2)
    {
      learnSequence(tokens);
    }
  }

  private void learnPhrase(String trigger, String nextWord)
  {
    if (trigger == null || nextWord == null) return;
    if (_context != null && !UserVocabularyStore.instance(_context).isLearningEnabled()) return;

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

    if (context != null && !context.isEmpty())
    {
      String cleanCtx = cleanWord(context);
      addPredictionsForKey(cleanCtx, result, maxResults);
      if (result.size() >= maxResults) return new ArrayList<>(result);
    }

    String cleanPrev = cleanWord(prevWord);
    addPredictionsForKey(cleanPrev, result, maxResults);

    return new ArrayList<>(result);
  }

  /**
   * Completely analyzes the sentence typed before the cursor and generates
   * multi-tier next-word & phrase completions.
   */
  public List<String> predictFromSentence(String textBeforeCursor, int maxResults)
  {
    if (maxResults <= 0)
    {
      return Collections.emptyList();
    }

    if (textBeforeCursor == null || textBeforeCursor.isEmpty())
    {
      boolean isBn = false;
      try
      {
        Config cfg = Config.globalConfig();
        if (cfg != null && (cfg.is_bengali_mode || "BN".equalsIgnoreCase(cfg.current_dictionary_short_name)))
        {
          isBn = true;
        }
      }
      catch (Throwable ignored) {}
      return getSentenceStarters(isBn, maxResults);
    }

    if (textBeforeCursor.endsWith("\n") || textBeforeCursor.endsWith("\r"))
    {
      return Collections.emptyList();
    }

    String activeSentence = extractActiveSentence(textBeforeCursor);
    List<String> tokens = tokenizeWords(activeSentence);

    if (tokens.isEmpty())
    {
      String trimmed = (textBeforeCursor != null) ? textBeforeCursor.trim() : "";
      if (trimmed.isEmpty() || trimmed.endsWith("।") || trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?"))
      {
        boolean isBn = isBengaliScript(textBeforeCursor);
        if (!isBn)
        {
          try
          {
            Config cfg = Config.globalConfig();
            if (cfg != null && (cfg.is_bengali_mode || "BN".equalsIgnoreCase(cfg.current_dictionary_short_name)))
            {
              isBn = true;
            }
          }
          catch (Throwable ignored) {}
        }
        return getSentenceStarters(isBn, maxResults);
      }
      return Collections.emptyList();
    }

    int numTokens = tokens.size();
    String wLast = tokens.get(numTokens - 1);
    String wSecondLast = (numTokens >= 2) ? tokens.get(numTokens - 2) : null;
    String wThirdLast = (numTokens >= 3) ? tokens.get(numTokens - 3) : null;

    LinkedHashSet<String> result = new LinkedHashSet<>();

    // 1. Check 4-gram (last 3 words)
    if (wThirdLast != null)
    {
      String fourGramKey = cleanWord(wThirdLast + " " + wSecondLast + " " + wLast);
      addPredictionsForKey(fourGramKey, result, maxResults);
      if (result.size() >= maxResults) return filterPredictions(result, wLast, maxResults);
    }

    // 2. Check Trigram (last 2 words)
    if (wSecondLast != null)
    {
      String trigramKey = cleanWord(wSecondLast + " " + wLast);
      addPredictionsForKey(trigramKey, result, maxResults);
      if (result.size() >= maxResults) return filterPredictions(result, wLast, maxResults);
    }

    // 3. Check Bigram (last 1 word)
    String unigramKey = cleanWord(wLast);
    addPredictionsForKey(unigramKey, result, maxResults);

    // 4. Fallback continuations to ensure predictions ALWAYS appear
    if (result.size() < maxResults)
    {
      boolean isBn = isBengaliScript(textBeforeCursor) || isBengaliScript(wLast);
      List<String> fallbacks = getFallbackContinuations(wLast, isBn);
      for (String fb : fallbacks)
      {
        result.add(fb);
        if (result.size() >= maxResults) break;
      }
    }

    return filterPredictions(result, wLast, maxResults);
  }

  private List<String> getFallbackContinuations(String lastWord, boolean isBengali)
  {
    if (isBengali)
    {
      String bw = (lastWord != null) ? lastWord.trim() : "";
      if (bw.equals("আমি") || bw.equals("আমরা"))
      {
        return Arrays.asList("তোমাকে", "আপনাকে", "ভালো", "আছি", "যাব", "করব", "বলছি", "চাই", "এখন");
      }
      if (bw.equals("তুমি") || bw.equals("তুই"))
      {
        return Arrays.asList("কেমন", "কি", "কোথায়", "আছো", "যাবে", "করবে", "বলবে", "কখন");
      }
      if (bw.equals("আপনি"))
      {
        return Arrays.asList("কেমন", "কি", "কোথায়", "আছেন", "বলুন", "যাবেন", "করবেন");
      }
      if (bw.equals("আমার") || bw.equals("আমাদের") || bw.equals("তোমার") || bw.equals("আপনার"))
      {
        return Arrays.asList("বাবা", "মা", "ভাই", "বন্ধু", "নাম", "ফোন", "বাড়ি", "সাথে", "জন্য", "কাছে", "কাজ");
      }
      if (bw.equals("খুব") || bw.equals("অনেক"))
      {
        return Arrays.asList("ভালো", "সুন্দর", "ধন্যবাদ", "ভালোবাসা", "কষ্ট", "বেশি", "সহজ");
      }
      if (bw.equals("দেরি") || bw.equals("সময়"))
      {
        return Arrays.asList("হয়ে", "গেছে", "মতো", "নেই", "হলে");
      }
      if (bw.endsWith("ছি") || bw.endsWith("ছিলে") || bw.endsWith("ব") || bw.endsWith("বে") || bw.endsWith("ছেন"))
      {
        return Arrays.asList("এখন", "পরে", "একটু", "আজকে", "কালকে", "সবসময়");
      }
      return Arrays.asList("হবে", "আছে", "হয়েছে", "গেছে", "করব", "বলব", "যাব", "এবং", "আর", "না", "কি", "জন্য");
    }

    String lw = (lastWord != null) ? lastWord.toLowerCase(Locale.ROOT) : "";
    if (lw.equals("i") || lw.equals("we") || lw.equals("they") || lw.equals("you"))
    {
      return Arrays.asList("are", "will", "have", "want to", "would", "can", "need to", "think", "know", "see");
    }
    if (lw.equals("he") || lw.equals("she") || lw.equals("it"))
    {
      return Arrays.asList("is", "was", "will", "has", "said", "can", "wants to");
    }
    if (lw.endsWith("s") || lw.equals("is") || lw.equals("was") || lw.equals("are") || lw.equals("were")
        || lw.equals("am") || lw.equals("be") || lw.equals("been"))
    {
      return Arrays.asList("a", "the", "not", "very", "good", "there", "in", "to", "my", "your", "going to", "ready");
    }
    if (lw.equals("the") || lw.equals("a") || lw.equals("an") || lw.equals("this") || lw.equals("that")
        || lw.equals("my") || lw.equals("your") || lw.equals("his") || lw.equals("her") || lw.equals("our") || lw.equals("their"))
    {
      return Arrays.asList("best", "first", "new", "time", "day", "way", "friend", "work", "life", "name", "family", "car");
    }
    if (lw.equals("in") || lw.equals("on") || lw.equals("at") || lw.equals("to") || lw.equals("for")
        || lw.equals("with") || lw.equals("by") || lw.equals("from") || lw.equals("of") || lw.equals("about"))
    {
      return Arrays.asList("the", "a", "my", "your", "this", "our", "you", "me", "it", "all", "us");
    }
    if (lw.equals("can") || lw.equals("could") || lw.equals("would") || lw.equals("should") || lw.equals("will") || lw.equals("do") || lw.equals("did"))
    {
      return Arrays.asList("you", "i", "we", "be", "help", "have", "get", "see", "make", "call");
    }
    // Default continuations after nouns / other words
    return Arrays.asList("is", "was", "and", "in", "to", "with", "for", "will", "has", "the", "that", "you", "today");
  }

  private void addPredictionsForKey(String key, LinkedHashSet<String> result, int maxResults)
  {
    if (key == null || key.isEmpty()) return;

    List<String> learned = _learnedBigrams.get(key);
    if (learned != null)
    {
      for (String w : learned)
      {
        result.add(w);
        if (result.size() >= maxResults) return;
      }
    }

    List<String> builtIn = _builtInBigrams.get(key);
    if (builtIn != null)
    {
      for (String w : builtIn)
      {
        result.add(w);
        if (result.size() >= maxResults) return;
      }
    }
  }

  private List<String> filterPredictions(LinkedHashSet<String> raw, String precedingWord, int maxResults)
  {
    List<String> filtered = new ArrayList<>(raw.size());
    for (String cand : raw)
    {
      if (!cand.equalsIgnoreCase(precedingWord))
      {
        filtered.add(cand);
        if (filtered.size() >= maxResults) break;
      }
    }
    return filtered;
  }

  public String extractActiveSentence(String text)
  {
    if (text == null) return "";
    int lastBound = -1;
    for (int i = 0; i < text.length(); i++)
    {
      char c = text.charAt(i);
      if (c == '\n' || c == '\r' || c == '।' || c == '॥' || c == '.' || c == '?' || c == '!')
      {
        lastBound = i;
      }
    }
    if (lastBound >= 0 && lastBound < text.length() - 1)
    {
      return text.substring(lastBound + 1);
    }
    else if (lastBound == text.length() - 1)
    {
      return "";
    }
    return text;
  }

  public List<String> tokenizeWords(String text)
  {
    if (text == null || text.isEmpty()) return Collections.emptyList();
    String[] parts = text.split("[\\s,\\;\\:\\\"\\(\\)\\[\\]\\{\\}\\-\\–\\—]+");
    List<String> list = new ArrayList<>(parts.length);
    for (String p : parts)
    {
      String clean = cleanWord(p);
      if (!clean.isEmpty())
      {
        list.add(clean);
      }
    }
    return list;
  }

  public List<String> getSentenceStarters(boolean isBengali, int maxResults)
  {
    if (isBengali)
    {
      return Arrays.asList("আমি", "তুমি", "আপনি", "কেমন", "কি", "আজকে", "ধন্যবাদ", "হ্যালো", "ইনশাআল্লাহ", "আলহামদুলিল্লাহ", "শুভ", "সবাই");
    }
    else
    {
      return Arrays.asList("I", "How", "What", "Please", "Thanks", "Hello", "Hi", "Can", "Good", "The", "We", "Yes");
    }
  }

  private String cleanWord(String word)
  {
    if (word == null) return "";
    String w = word.trim();
    int start = 0;
    while (start < w.length() && isPunctuation(w.charAt(start)))
    {
      start++;
    }
    int end = w.length();
    while (end > start && isPunctuation(w.charAt(end - 1)))
    {
      end--;
    }
    return w.substring(start, end).trim().toLowerCase(Locale.ROOT);
  }

  private boolean isPunctuation(char c)
  {
    return c == '.' || c == ',' || c == '?' || c == '!' || c == ';' || c == ':'
        || c == '।' || c == '॥' || c == '"' || c == '\'' || c == '(' || c == ')'
        || c == '[' || c == ']' || c == '{' || c == '}' || c == '-' || c == '_';
  }

  public boolean isBengaliScript(String word)
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
