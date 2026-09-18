package juloo.keyboard2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * High-performance, memory-efficient in-memory Emoji Search Engine.
 * Indexes English and Bengali keywords mapped to standard Unicode emoji strings.
 */
public class EmojiSearchIndex
{
  private static final Map<String, List<String>> _index = new HashMap<>(512);
  private static final List<Emoji> _popularEmojis = new ArrayList<>();
  private static boolean _initialized = false;

  private static final String[] POPULAR_EMOJIS_DEF = {
    "😀", "😂", "🤣", "❤️", "👍", "🔥", "✨", "🙏", "🥰", "😍",
    "🥺", "👏", "🥳", "🎉", "💯", "😊", "🌸", "☕", "🤲", "🕌",
    "💔", "🤝", "🌹", "🐱", "🐶", "🚗", "🍕", "💡", "🌙", "😎",
    "👌", "😘", "🙌", "💪", "😭", "🤤", "👀", "🤦", "🤷", "🫡"
  };

  public static synchronized void init()
  {
    if (_initialized) return;

    // Build default popular emoji list
    for (String s : POPULAR_EMOJIS_DEF)
    {
      Emoji e = Emoji.getEmojiByString(s);
      if (e != null)
      {
        _popularEmojis.add(e);
      }
      else
      {
        _popularEmojis.add(new Emoji(s));
      }
    }

    // Index English and Bengali keywords
    // 1. Faces & Emotions
    addKeywords(new String[]{"smile", "smiling", "happy", "joy", "cheerful", "glad", "হাসি", "খুশি", "আনন্দ"},
        "😀", "😃", "😄", "😁", "😆", "😊", "🙂", "☺️", "😋", "😻");
    addKeywords(new String[]{"laugh", "laughing", "haha", "lol", "rofl", "lmao", "fun", "funny", "হাসাহাসি"},
        "😂", "🤣", "😆", "😹", "😸", "😁", "😜", "🤪");
    addKeywords(new String[]{"love", "crush", "romance", "adore", "heart", "ভালোবাসা", "প্রেম", "দিল", "হৃদয়"},
        "❤️", "🥰", "😍", "😘", "💖", "💗", "💓", "💕", "💘", "💝", "💞", "💟", "💌", "🫶");
    addKeywords(new String[]{"kiss", "blowing kiss", "muah", "চুম্বন", "চুমা"},
        "😘", "😗", "😚", "😙", "💋", "😽");
    addKeywords(new String[]{"wink", "playful", "চোখ টেপা", "ইশারা"},
        "😉", "😜", "😝", "🤪");
    addKeywords(new String[]{"cry", "crying", "tear", "tears", "sad", "weep", "sob", "কান্না", "কষ্ট", "দুঃখ"},
        "😭", "😢", "🥺", "😿", "😥", "😓", "💔", "😞", "😔", "☹️", "🙁");
    addKeywords(new String[]{"angry", "mad", "rage", "furious", "annoyed", "রাগ", "ক্রোধ", "গোস্বা"},
        "😡", "😠", "🤬", "😤", "😾", "👿", "💢");
    addKeywords(new String[]{"think", "thinking", "wonder", "hmm", "idea", "চিন্তা", "ভাবনা", "কী"},
        "🤔", "🧐", "💡", "💭");
    addKeywords(new String[]{"cool", "sunglasses", "swag", "awesome", "চশমা", "স্মার্ট"},
        "😎", "🕶️", "🤙", "🆒");
    addKeywords(new String[]{"nerd", "geek", "study", "books", "জ্ঞানী", "পড়াশোনা"},
        "🤓", "🧐", "📚", "📖");
    addKeywords(new String[]{"sleep", "sleeping", "sleepy", "tired", "zzz", "bed", "ঘুম", "ঘুমন্ত", "ক্লান্ত"},
        "😴", "😪", "🥱", "🛌", "💤");
    addKeywords(new String[]{"sick", "ill", "fever", "virus", "mask", "hospital", "অসুস্থ", "জ্বর", "মাস্ক"},
        "😷", "🤒", "🤕", "🤢", "🤮", "🤧");
    addKeywords(new String[]{"hot", "sweat", "summer", "গরম", "ঘাম"},
        "🥵", "😅", "💦", "🔥", "☀️");
    addKeywords(new String[]{"cold", "freeze", "freezing", "ice", "winter", "ঠান্ডা", "শীত", "বরফ"},
        "🥶", "❄️", "🧊", "⛄", "☃️");
    addKeywords(new String[]{"shock", "shocked", "omg", "wow", "surprised", "mindblown", "অবাক", "চমক"},
        "😱", "🤯", "😲", "😯", "😮", "😳", "🙀");
    addKeywords(new String[]{"party", "celebrate", "celebration", "birthday", "উৎসব", "পার্টি", "জন্মদিন"},
        "🥳", "🎉", "🎊", "🎂", "🍾", "🎁");
    addKeywords(new String[]{"fire", "flame", "hot", "lit", "আগুন", "জ্বলন্ত"},
        "🔥", "🧨", "💥");
    addKeywords(new String[]{"star", "stars", "sparkle", "shine", "তারা", "নক্ষত্র", "চকচকে"},
        "⭐", "🌟", "✨", "💫", "🌠");
    addKeywords(new String[]{"skull", "dead", "death", "ghost", "কঙ্কাল", "মৃত", "ভূত"},
        "💀", "☠️", "👻", "🧟");
    addKeywords(new String[]{"money", "cash", "dollar", "rich", "টাকা", "পয়সা", "ধনী"},
        "🤑", "💰", "💵", "💸", "💳", "💎");
    addKeywords(new String[]{"shh", "quiet", "silent", "secret", "চুপ", "শান্ত", "গোপন"},
        "🤫", "🤐", "😶");
    addKeywords(new String[]{"angel", "halo", "innocent", "ফেরেশতা", "পবিত্র", "ভালো"},
        "😇", "👼", "✨");
    addKeywords(new String[]{"devil", "evil", "demon", "শয়তান"},
        "😈", "👿");
    addKeywords(new String[]{"poop", "poo", "shit", "গোবর"},
        "💩");
    addKeywords(new String[]{"clown", "joker", "জোকার", "ক্লাউন"},
        "🤡");
    addKeywords(new String[]{"robot", "bot", "ai", "রোবট"},
        "🤖");
    addKeywords(new String[]{"monkey", "বানর"},
        "🐵", "🐒", "🙈", "🙉", "🙊");

    // 2. Hand Gestures & Body
    addKeywords(new String[]{"thumbs up", "like", "yes", "good", "лайк", "লাইক", "পছন্দ", "ভালো", "হ্যাঁ"},
        "👍", "👌", "✅", "✔️");
    addKeywords(new String[]{"thumbs down", "dislike", "no", "bad", "ডিসলাইক", "অপছন্দ", "খারাপ", "না"},
        "👎", "❌", "❎");
    addKeywords(new String[]{"clap", "clapping", "applause", "bravo", "তালি", "সাবাশ"},
        "👏", "🙌");
    addKeywords(new String[]{"wave", "waving", "hi", "hello", "bye", "সালাম", "বিদায়", "হ্যালো"},
        "👋", "🙋", "🙋‍♂️", "🙋‍♀️");
    addKeywords(new String[]{"pray", "prayer", "please", "thanks", "thank you", "namaste", "দোয়া", "প্রার্থনা", "ধন্যবাদ"},
        "🙏", "🤲", "📿");
    addKeywords(new String[]{"dua", "munajat", "islamic", "muslim", "দোয়া", "মোনাজাত", "ইসলামিক"},
        "🤲", "🕌", "🕋", "🌙", "📿", "☝️", "🙏");
    addKeywords(new String[]{"handshake", "deal", "agreement", "partner", "হ্যান্ডশেক", "চুক্তি", "বন্ধু"},
        "🤝");
    addKeywords(new String[]{"muscle", "bicep", "strong", "strength", "gym", "workout", "শক্তি", "শক্তিশালী", "ব্যায়াম"},
        "💪", "🏋️", "🦾");
    addKeywords(new String[]{"ok", "okay", "fine", "perfect", "ঠিক আছে", "পারফেক্ট"},
        "👌", "🆗", "👍");
    addKeywords(new String[]{"fist", "punch", "fight", "ঘুষি", "লড়াই"},
        "👊", "✊", "🤛", "🤜");
    addKeywords(new String[]{"victory", "peace", "two", "শান্তি", "জয়"},
        "✌️", "🕊️");
    addKeywords(new String[]{"point", "finger", "hand", "হাত", "আঙুল"},
        "👉", "👈", "👆", "👇", "☝️");
    addKeywords(new String[]{"eyes", "look", "see", "watch", "চোখ", "দেখা"},
        "👀", "👁️");

    // 3. Animals & Nature
    addKeywords(new String[]{"cat", "kitten", "kitty", "বিড়াল", "বিড়ালছানা"},
        "🐱", "🐈", "🐈‍⬛", "😹", "😻", "😼");
    addKeywords(new String[]{"dog", "puppy", "কুকুর", "কুকুরছানা"},
        "🐶", "🐕", "🦮", "🐩");
    addKeywords(new String[]{"flower", "rose", "blossom", "tulip", "ফুল", "গোলাপ"},
        "🌸", "🌹", "🌺", "🌻", "🌼", "🌷", "💐");
    addKeywords(new String[]{"bird", "dove", "eagle", "পাখি", "কবুতর", "ঈগল"},
        "🐦", "🕊️", "🦅", "🦆", "🦢", "🦜");
    addKeywords(new String[]{"fish", "shark", "whale", "dolphin", "মাছ", "হাঙর", "তিমি"},
        "🐟", "🐠", "🐡", "🦈", "🐬", "🐳");
    addKeywords(new String[]{"lion", "tiger", "সিংহ", "বাঘ"},
        "🦁", "🐯", "🐅", "🐆");
    addKeywords(new String[]{"cow", "ox", "bull", "গরু", "ষাঁড়"},
        "🐮", "🐄", "🐂");
    addKeywords(new String[]{"horse", "unicorn", "ঘোড়া"},
        "🐴", "🐎", "🦄");
    addKeywords(new String[]{"snake", "সাপ"},
        "🐍");
    addKeywords(new String[]{"tree", "plant", "nature", "গাছ", "প্রকৃতি"},
        "🌳", "🌲", "🌴", "🌿", "🍀", "🌱");

    // 4. Food & Drinks
    addKeywords(new String[]{"tea", "cha", "cup", "চা", "এক কাপ চা"},
        "☕", "🫖", "🍵");
    addKeywords(new String[]{"coffee", "cafe", "কফি"},
        "☕", "🧋");
    addKeywords(new String[]{"pizza", "পিৎজা"},
        "🍕");
    addKeywords(new String[]{"burger", "hamburger", "বার্গার"},
        "🍔");
    addKeywords(new String[]{"rice", "biryani", "food", "ভাত", "বিরিয়ানি", "খাবার"},
        "🍚", "🍛", "🍲", "🥘", "🍱");
    addKeywords(new String[]{"chicken", "meat", "মুরগি", "মাংস"},
        "🍗", "🥩", "🍖");
    addKeywords(new String[]{"cake", "birthday cake", "sweet", "কেক", "মিষ্টি"},
        "🎂", "🍰", "🧁", "🍩");
    addKeywords(new String[]{"chocolate", "candy", "চকলেট"},
        "🍫", "🍬", "🍭");
    addKeywords(new String[]{"ice cream", "আইসক্রিম"},
        "🍦", "🍧", "🍨");
    addKeywords(new String[]{"apple", "ফল", "আপেল"},
        "🍎", "🍏");
    addKeywords(new String[]{"mango", "আম"},
        "🥭");
    addKeywords(new String[]{"banana", "কলা"},
        "🍌");
    addKeywords(new String[]{"water", "drink", "পানি", "জল"},
        "💧", "🥤", "🧃", "🥛");

    // 5. Travel, Transport, Places
    addKeywords(new String[]{"car", "auto", "drive", "গাড়ি"},
        "🚗", "🚘", "🚙", "🏎️", "🛻");
    addKeywords(new String[]{"bike", "cycle", "motorcycle", "বাইক", "সাইকেল"},
        "🏍️", "🚲", "🛵");
    addKeywords(new String[]{"bus", "বাস"},
        "🚌", "🚍");
    addKeywords(new String[]{"train", "রেল", "ট্রেন"},
        "🚆", "🚂", "🚄");
    addKeywords(new String[]{"plane", "airplane", "flight", "বিমান", "প্লেন"},
        "✈️", "🛫", "🛬", "🚀");
    addKeywords(new String[]{"ship", "boat", "নৌকা", "জাহাজ"},
        "🚢", "⛵", "🚤", "🛥️");
    addKeywords(new String[]{"rocket", "space", "fast", "রকেট", "মহাকাশ"},
        "🚀", "🛸", "🪐");
    addKeywords(new String[]{"house", "home", "বাড়ি", "ঘর"},
        "🏠", "🏡");
    addKeywords(new String[]{"mosque", "masjid", "islam", "মসজিদ", "ইসলাম"},
        "🕌", "🕋", "🌙", "📿");

    // 6. Sports & Games
    addKeywords(new String[]{"football", "soccer", "ফুটবল"},
        "⚽");
    addKeywords(new String[]{"cricket", "cricket bat", "ক্রিকেট"},
        "🏏");
    addKeywords(new String[]{"game", "gaming", "play", "গেম", "খেলা"},
        "🎮", "🕹️", "🎲", "🎯");
    addKeywords(new String[]{"trophy", "cup", "winner", "win", "champion", "ট্রফি", "পুরস্কার", "জয়"},
        "🏆", "🥇", "🥈", "🥉", "🏅");
    addKeywords(new String[]{"music", "song", "audio", "গান", "সঙ্গীত"},
        "🎵", "🎶", "🎧", "🎤", "🎸", "🎹");

    // 7. Objects, Weather & Symbols
    addKeywords(new String[]{"phone", "mobile", "cell", "ফোন", "মোবাইল"},
        "📱", "📲", "☎️", "📞");
    addKeywords(new String[]{"laptop", "computer", "pc", "ল্যাপটপ", "কম্পিউটার"},
        "💻", "🖥️", "⌨️");
    addKeywords(new String[]{"sun", "sunny", "day", "সূর্য", "রোদ"},
        "☀️", "🌞", "🌤️");
    addKeywords(new String[]{"moon", "night", "crescent", "চাঁদ", "রাত"},
        "🌙", "🌕", "🌚");
    addKeywords(new String[]{"rain", "umbrella", "বৃষ্টি", "ছাতা"},
        "🌧️", "🌦️", "☔", "☂️");
    addKeywords(new String[]{"clock", "time", "hour", "সময়", "ঘড়ি"},
        "🕒", "⏰", "⏱️");
    addKeywords(new String[]{"100", "hundred", "perfect", "full", "একশত"},
        "💯");
    addKeywords(new String[]{"check", "correct", "done", "tick", "সঠিক", "টিক"},
        "✅", "✔️");
    addKeywords(new String[]{"cross", "wrong", "cancel", "error", "ভুল"},
        "❌", "❎");
    addKeywords(new String[]{"warning", "alert", "danger", "সাবধান", "বিপদ"},
        "⚠️", "🚨", "⛔");
    addKeywords(new String[]{"gift", "present", "উপহার"},
        "🎁", "🎀");

    _initialized = true;
  }

  private static void addKeywords(String[] keywords, String... emojis)
  {
    for (String kw : keywords)
    {
      String clean = kw.toLowerCase(Locale.ROOT).trim();
      List<String> list = _index.get(clean);
      if (list == null)
      {
        list = new ArrayList<>();
        _index.put(clean, list);
      }
      for (String em : emojis)
      {
        if (!list.contains(em))
        {
          list.add(em);
        }
      }
    }
  }

  /**
   * Search matching emojis for the given user query.
   */
  public static List<Emoji> search(String query, int limit)
  {
    init();

    if (query == null || query.trim().isEmpty())
    {
      return getPopularEmojis();
    }

    String q = query.toLowerCase(Locale.ROOT).trim();
    Set<String> resultSet = new LinkedHashSet<>();

    // 1. Exact matches
    List<String> exact = _index.get(q);
    if (exact != null)
    {
      resultSet.addAll(exact);
    }

    // 2. Prefix matches (query is prefix of keyword e.g. "smi" -> "smile")
    for (Map.Entry<String, List<String>> entry : _index.entrySet())
    {
      if (entry.getKey().startsWith(q) && !entry.getKey().equals(q))
      {
        resultSet.addAll(entry.getValue());
        if (resultSet.size() >= limit * 2) break;
      }
    }

    // 3. Substring matches (keyword contains query e.g. "laugh" in "crying laughing")
    if (resultSet.size() < limit)
    {
      for (Map.Entry<String, List<String>> entry : _index.entrySet())
      {
        if (entry.getKey().contains(q) && !entry.getKey().startsWith(q))
        {
          resultSet.addAll(entry.getValue());
          if (resultSet.size() >= limit * 2) break;
        }
      }
    }

    // Convert string matches to Emoji objects
    List<Emoji> list = new ArrayList<>();
    for (String s : resultSet)
    {
      if (list.size() >= limit) break;
      Emoji e = Emoji.getEmojiByString(s);
      if (e != null)
      {
        list.add(e);
      }
      else
      {
        list.add(new Emoji(s));
      }
    }

    // Fallback: If no matches found, return popular emojis
    if (list.isEmpty())
    {
      return getPopularEmojis();
    }

    return list;
  }

  public static List<Emoji> getPopularEmojis()
  {
    init();
    return new ArrayList<>(_popularEmojis);
  }
}
