package juloo.keyboard2.fancy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * High-performance, 100% offline engine for converting text into:
 * 1. Unicode Fancy Letter Fonts (Bold, Italic, Script, Gothic, Circled, Squared, Subscript, etc.)
 * 2. ASCII Text Art Borders & Frames (【D】【a】【d】, 『D』『a』『d』, ≈D≈a≈d≈, ░D░a░d░, etc.)
 * 3. Categorized Kaomoji & ASCII Art (Love, Hug, Happy, Cute, Shrug, etc.)
 */
public final class FancyTextEngine
{
  public static class StyleItem
  {
    public final String id;
    public final String name;
    public final String category; // "fonts", "text_art", "kaomoji"
    public final String previewSample;

    public StyleItem(String id, String name, String category, String previewSample)
    {
      this.id = id;
      this.name = name;
      this.category = category;
      this.previewSample = previewSample;
    }
  }

  public static class KaomojiCategory
  {
    public final String name;
    public final List<String> items;

    public KaomojiCategory(String name, List<String> items)
    {
      this.name = name;
      this.items = items;
    }
  }

  // Unicode Alphabet Maps
  private static final Map<String, String[]> FONT_ALPHABETS = new LinkedHashMap<>();
  private static final List<StyleItem> FONT_STYLES = new ArrayList<>();
  private static final List<StyleItem> TEXT_ART_STYLES = new ArrayList<>();
  private static final List<KaomojiCategory> KAOMOJI_CATEGORIES = new ArrayList<>();

  static
  {
    initFontStyles();
    initTextArtStyles();
    initKaomoji();
  }

  private static void registerFont(String id, String name, String upper, String lower, String digits)
  {
    String[] map = new String[62];
    for (int i = 0; i < 26; i++)
    {
      map[i] = getCharOrSurrogate(upper, i);
    }
    for (int i = 0; i < 26; i++)
    {
      map[26 + i] = getCharOrSurrogate(lower, i);
    }
    for (int i = 0; i < 10; i++)
    {
      if (digits != null && !digits.isEmpty())
      {
        map[52 + i] = getCharOrSurrogate(digits, i);
      }
      else
      {
        map[52 + i] = String.valueOf((char)('0' + i));
      }
    }
    FONT_ALPHABETS.put(id, map);
    String sample = transformWithMap(map, "TypoDev");
    FONT_STYLES.add(new StyleItem(id, name, "fonts", sample));
  }

  private static String getCharOrSurrogate(String str, int charIndex)
  {
    if (str == null || str.isEmpty()) return "";
    int codePointIndex = 0;
    for (int i = 0; i < str.length(); )
    {
      int cp = str.codePointAt(i);
      if (codePointIndex == charIndex)
      {
        return new String(Character.toChars(cp));
      }
      codePointIndex++;
      i += Character.charCount(cp);
    }
    return "";
  }

  private static void initFontStyles()
  {
    // 1. Negative Squared / Boxed Black (Requested by user: 🅳 🆂 🅵 🆂 🅳)
    registerFont("neg_squared", "Negative Squared 🆂🆀🆄🅰🆁🅴",
        "🅰🅱🅲🅳🅴🅵🅶🅷🅸🅹🅺🅻🼯🅽🅾🅿🆀🆁🆂🆃🆄🆅🆆🆇🆈🆉",
        "🅰🅱🅲🅳🅴🅵🅶🅷🅸🅹🅺🅻🼯🅽🅾🅿🆀🆁🆂🆃🆄🆅🆆🆇🆈🆉",
        "⓪①②③④⑤⑥⑦⑧⑨");

    // 2. Squared White / Boxed
    registerFont("squared", "Squared Box 🄱🄾🅇",
        "🄰🄱🄲🄳🄴🄵🄶🄷🄸🄹🄺🄻🄼🄽🄾🄿🅀🅁🅂🅃🅄🅅🅆🅇🅈🅉",
        "🄰🄱🄲🄳🄴🄵🄶🄷🄸🄹🄺🄻🄼🄽🄾🄿🅀🅁🅂🅃🅄🅅🅆🅇🅈🅉",
        "0123456789");

    // 3. Circled White (Requested by user: ⓓ ⓢ ⓕ ⓢ ⓓ)
    registerFont("circled", "Circled Ⓒⓘⓡⓒⓛⓔ",
        "ⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏ",
        "ⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩ",
        "⓪①②③④⑤⑥⑦⑧⑨");

    // 4. Circled Black / Negative Circled
    registerFont("neg_circled", "Black Circled 🅒🅘🅡🅒🅛🅔",
        "🅐🅑🅒🅓🅔🅕🅖🅗🅘🅙🅚🅛🅜🅝🅞🅟🅠🅡🅢🅣🅤🅥🅦🅧🅨🅩",
        "🅐🅑🅒🅓🅔🅕🅖🅗🅘🅙🅚🅛🅜🅝🅞🅟🅠🅡🅢🅣🅤🅥🅦🅧🅨🅩",
        "⓿❶❷❸❹❺❻❼❽❾");

    // 5. Mathematical Bold Serif
    registerFont("bold_serif", "Bold Serif 𝐁𝐨𝐥𝐝",
        "𝐀𝐁𝐂𝐃𝐄𝐅𝐆𝐇𝐈𝐉𝐊𝐋𝐌𝐍𝐎𝐏𝐐𝐑𝐒𝐓𝐔𝐕𝐖𝐗𝐘𝐙",
        "𝐚𝐛𝐜𝐝𝐞𝐟𝐠𝐡𝐢𝐣𝐤𝐥𝐦𝐧𝐨𝐩𝐪𝐫𝐬𝐭𝐮𝐯𝐰𝐱𝐲𝐳",
        "𝟎𝟏𝟐𝟑𝟒𝟓𝟔𝟕𝟖𝟗");

    // 6. Mathematical Bold Sans
    registerFont("bold_sans", "Bold Sans 𝗕𝗼𝗹𝗱",
        "𝗔𝗕𝗖𝗗𝗘𝗙𝗚𝗛𝗜𝗝𝗞𝗟𝗠𝗡𝗢𝗣𝗤𝗥𝗦𝗧𝗨𝗩𝗪𝗫𝗬𝗭",
        "𝗮𝗯𝗰𝗱𝗲𝗳𝗴𝗵𝗶𝗷𝗸𝗹𝗺𝗻𝗼𝗽𝗾𝗿𝘀𝘁𝘂𝘃𝘄𝘅𝘆𝘇",
        "𝟬𝟭𝟮𝟯𝟰𝟱𝟲𝟳𝟴𝟵");

    // 7. Mathematical Italic Serif
    registerFont("italic_serif", "Italic Serif 𝐼𝑡𝑎𝑙𝑖𝑐",
        "𝐴𝐵𝐶𝐷𝐸𝐹𝐺𝐻𝐼𝐽𝐾𝐿𝑀𝑁𝑂𝑃𝑄𝑅𝑆𝑇𝑈𝑉𝑊𝑋𝑌𝑍",
        "𝑎𝑏𝑐𝑑𝑒𝑓𝑔ℎ𝑖𝑗𝑘𝑙𝑚𝑛𝑜𝑝𝑞𝑟𝑠𝑡𝑢𝑣𝑤𝑥𝑦𝑧",
        "0123456789");

    // 8. Mathematical Italic Sans
    registerFont("italic_sans", "Italic Sans 𝘐𝘵𝘢𝘭𝘪𝘤",
        "𝘈𝘉𝘊𝘋𝘌𝘍𝘎𝘏𝘐𝘑𝘒𝘓𝘔𝘕𝘖𝘗𝘘𝘙𝘚𝘛𝘜𝘝𝘞𝘟𝘠𝘡",
        "𝘢𝘣𝘤𝘥𝘦𝘧𝘨𝘩𝘪𝘫𝘬𝘭𝘮𝘯𝘰𝘱𝘲𝘳𝘴𝘵𝘶𝘷𝘸𝘹𝘺𝘻",
        "0123456789");

    // 9. Bold Italic Serif
    registerFont("bold_italic_serif", "Bold Italic 𝑩𝒐𝒍𝒅 𝑰𝒕𝒂𝒍𝒊𝒄",
        "𝑨𝑩𝑪𝑫𝑬𝑭𝑮𝑯𝑰𝑱𝑲𝑳𝑴𝑵𝑶𝑷𝑸𝑹𝑺𝑻𝑼𝑽𝑾𝑿𝒀𝒁",
        "𝒂𝒃𝒄𝒅𝒆𝒇𝒈𝒉𝒊𝒋𝒌𝒍𝒎𝒏𝒐𝒑𝒒𝒓𝒔𝒕𝒖𝒗𝒘𝒙𝒚𝒛",
        "0123456789");

    // 10. Bold Italic Sans
    registerFont("bold_italic_sans", "Bold Italic Sans 𝘽𝙤𝙡𝙙 𝙄𝙩𝙖𝙡𝙞𝙘",
        "𝘼𝘽𝘾𝘿𝙀𝙁𝙂𝙃𝙄𝙅𝙆𝙇𝙈𝙉𝙊𝙋𝙌𝙍𝙎𝙏𝙐𝙑𝙒𝙓𝙔𝙕",
        "𝙖𝙗𝙘𝙙𝙚𝙛𝙜𝙝𝙞𝙟𝙠𝙡𝙢𝙣𝙤𝙥𝙦𝙧𝙨𝙩𝙪𝙫𝙬𝙭𝙮𝙯",
        "0123456789");

    // 11. Script / Cursive (Elegant)
    registerFont("script", "Script Cursive 𝒮𝒸𝓇𝒾𝓅𝓉",
        "𝒜𝐵𝒞𝒟𝐸𝐹𝒢𝐻𝐼𝒥𝒦𝐿𝑀𝒩𝒪𝒫𝒬𝑅𝒮𝒯𝒰𝒱𝒲𝒳𝒴𝒵",
        "𝒶𝒷𝒸𝒹𝑒𝒻𝑔𝒽𝒾𝒿𝓀𝓁𝓂𝓃𝑜𝓅𝓆𝓇𝓈𝓉𝓊𝓋𝓌𝓍𝓎𝓏",
        "0123456789");

    // 12. Bold Script / Fancy Cursive
    registerFont("bold_script", "Bold Script 𝓕𝓪𝓷𝓬𝔂",
        "𝓐𝓑𝓒𝓓𝓔𝓕𝓖𝓗𝓘𝓙𝓚𝓛𝓜𝓝𝓞𝓟𝓠𝓡𝓢𝓣𝓤𝓥𝓦𝓧𝓨𝓩",
        "𝓪𝓫𝓬𝓭𝓮𝓯𝓰𝓱𝓲𝓳𝓴𝓵𝓶𝓷𝓸𝓹𝓺𝓻𝓼𝓽𝓾𝓿𝔀𝔁𝔂𝔃",
        "0123456789");

    // 13. Fraktur / Gothic
    registerFont("fraktur", "Fraktur Gothic 𝔉𝔯𝔞𝔨𝔱𝔲𝔯",
        "𝔄𝔅ℭ𝔇𝔈𝔉𝔊ℌℑ𝔍𝔎𝔏𝔐𝔑𝔒𝔓𝔔ℜ𝔖𝔗𝔘𝔙𝔚𝔛𝔜ℨ",
        "𝔞𝔟𝔠𝔡𝔢𝔣𝔤𝔥𝔦𝔧𝔨𝔩𝔪𝔫𝔬𝔭𝔮𝔯𝔰𝔱𝔲𝔳𝔴𝔵𝔶𝔷",
        "0123456789");

    // 14. Bold Fraktur / Bold Gothic
    registerFont("bold_fraktur", "Bold Gothic 𝕭𝖔𝖑𝖉",
        "𝕬𝕭𝕮𝕯𝕰𝕱𝕲𝕳𝕴𝕵𝕶𝕷𝕸𝕹𝕺𝕻𝕼𝕽𝕾𝕿𝖀𝖁𝖂𝖃𝖄𝖅",
        "𝖆𝖇𝖈𝖉𝖊𝖋𝖌𝖍𝖎𝖏𝖐𝖑𝖒𝖓𝖔𝖕𝖖𝖗𝖘𝖙𝖚𝖛𝖜𝖝𝖞𝖟",
        "0123456789");

    // 15. Double Struck / Blackboard Bold
    registerFont("double_struck", "Double Struck 𝔻𝕠𝕦𝕓𝕝𝕖",
        "𝔸𝔹ℂ𝔻𝔼𝔽𝔾ℍ𝕀𝕁𝕂𝕃𝕄ℕ𝕆ℙℚℝ𝕊𝕋𝕌𝕍𝕎𝕏𝕐ℤ",
        "𝕒𝕓𝕔𝕕𝕖𝕗𝕘𝕙𝕚𝕛𝕜𝕝𝕞𝕟𝕠𝕡𝕢𝕣𝕤𝕥𝕦𝕧𝕨𝕩𝕪𝕫",
        "𝟘𝟙𝟚𝟛𝟜𝟝𝟞𝟟𝟠𝟡");

    // 16. Small Capitals
    registerFont("small_caps", "Small Caps ꜱᴍᴀʟʟ",
        "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ",
        "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ",
        "0123456789");

    // 17. Monospace Typewriter
    registerFont("monospace", "Monospace 𝙼𝚘𝚗𝚘",
        "𝙰𝙱𝙲𝙳𝙴𝙵𝙶𝙷𝙸𝙹𝙺𝙻𝙼𝙽𝙾𝙿𝚀𝚁𝚂𝚃𝚄𝚅𝚆𝚇𝚈𝚉",
        "𝚊𝚋𝚌𝚍𝚎𝚏𝚐𝚑𝚒𝚓𝚔𝚕𝚖𝚗𝚘𝚙𝚚𝚛𝚜𝚝𝚞𝚟𝚠𝚡𝚢𝚣",
        "0123456789");

    // 18. Fullwidth / Japanese Aesthetics
    registerFont("fullwidth", "Fullwidth Ｆｕｌｌ",
        "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ",
        "ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ",
        "０１２３４５６７８９");

    // 19. Subscript (Requested by user: dₛfₛd)
    registerFont("subscript", "Subscript ₜₑₓₜ",
        "ₐᵦ꜀𝒹ₑբ₉ₕᵢⱼₖₗₘₙₒₚᵩᵣₛₜᵤᵥ𝓌ₓᵧ𝓏",
        "ₐᵦ꜀𝒹ₑբ₉ₕᵢⱼₖₗₘₙₒₚᵩᵣₛₜᵤᵥ𝓌ₓᵧ𝓏",
        "₀₁₂₃₄₅₆₇₈₉");

    // 20. Superscript
    registerFont("superscript", "Superscript ᵗᵉˣᵗ",
        "ᴬᴮᶜᴰᴱᴳᴴᴵᴶᴷᴸᴹᴺᴼᴾᴼᴿˢᵀᵁⱽᵂˣʸᶻ",
        "ᵃᵇᶜᵈᵉᶠᵍʰⁱʲᵏˡᵐⁿᵒᵖᵠʳˢᵗᵘᵛʷˣʸᶻ",
        "⁰¹²³⁴⁵⁶⁷⁸⁹");

    // Dynamic transform fonts (Strikethrough, Underline, Slash, Upside-Down)
    FONT_STYLES.add(new StyleItem("strikethrough", "Strikethrough t̶e̶x̶t̶", "fonts", "T̶y̶p̶o̶D̶e̶v̶"));
    FONT_STYLES.add(new StyleItem("underline", "Underline t̲e̲x̲t̲", "fonts", "T̲y̲p̶o̲D̲e̲v̲"));
    FONT_STYLES.add(new StyleItem("double_underline", "Double Underline t̳e̳x̳t̳", "fonts", "T̳y̳p̳o̳D̳e̳v̳"));
    FONT_STYLES.add(new StyleItem("slashed", "Slash Through t̷e̷x̷t̷", "fonts", "T̷y̷p̷o̷D̷e̷v̷"));
    FONT_STYLES.add(new StyleItem("upside_down", "Upside Down ʇxǝʇ", "fonts", "ʌǝCodʎ⊥"));
  }

  private static void initTextArtStyles()
  {
    // Exact requested styles from user screenshot:
    // 1. 【D】【a】【d】
    TEXT_ART_STYLES.add(new StyleItem("bracket_thick", "Thick Bracket 【T】【e】【x】【t】", "text_art", "【T】【y】【p】【o】【D】【e】【v】"));

    // 2. 『D』『a』『d』
    TEXT_ART_STYLES.add(new StyleItem("bracket_corner", "Corner Bracket 『T』『e』『x』『t』", "text_art", "『T』『y』『p』『o』『D』『e』『v』"));

    // 3. ≈D≈a≈d≈
    TEXT_ART_STYLES.add(new StyleItem("tilde_waves", "Wave Tilde ≈T≈e≈x≈t≈", "text_art", "≈T≈y≈p≈o≈D≈e≈v≈"));

    // 4. ░D░a░d░
    TEXT_ART_STYLES.add(new StyleItem("shaded_ascii", "Shaded ASCII ░T░e░x░t░", "text_art", "░T░y░p░o░D░e░v░"));

    // 5. (っ◔◡◔)っ ♥ Dad ♥
    TEXT_ART_STYLES.add(new StyleItem("kaomoji_love_hug", "Love Hug (っ◔◡◔)っ ♥", "text_art", "(っ◔◡◔)っ ♥ TypoDev ♥"));

    // 6. ★彡[ Dad ]彡★
    TEXT_ART_STYLES.add(new StyleItem("star_wings", "Star Wings ★彡", "text_art", "★彡[ TypoDev ]彡★"));

    // 7. ıllıllı[ Dad ]ıllıllı
    TEXT_ART_STYLES.add(new StyleItem("soundwave", "Soundwave ıllıllı", "text_art", "ıllıllı[ TypoDev ]ıllıllı"));

    // 8. ╰•★★ Dad ★★•╯
    TEXT_ART_STYLES.add(new StyleItem("winged_stars", "Winged Stars ╰•★★", "text_art", "╰•★★ TypoDev ★★•╯"));

    // 9. (✿◠‿◠) Dad (◠‿◠✿)
    TEXT_ART_STYLES.add(new StyleItem("flower_smile", "Flower Smile (✿◠‿◠)", "text_art", "(✿◠‿◠) TypoDev (◠‿◠✿)"));

    // 10. ✧･ﾟ: *✧･ﾟ:* Dad *:･ﾟ✧*:･ﾟ✧
    TEXT_ART_STYLES.add(new StyleItem("sparkle_magic", "Sparkles ✧･ﾟ:*", "text_art", "✧･ﾟ: *✧･ﾟ:* TypoDev *:･ﾟ✧*:･ﾟ✧"));

    // 11. (づ｡◕‿‿◕｡)づ Dad
    TEXT_ART_STYLES.add(new StyleItem("cute_hug", "Cute Hug (づ｡◕‿◕｡)づ", "text_art", "(づ｡◕‿‿◕｡)づ TypoDev"));

    // 12. ꧁༺ Dad ༻꧂
    TEXT_ART_STYLES.add(new StyleItem("royal_wings", "Royal Wings ꧁༺", "text_art", "꧁༺ TypoDev ༻꧂"));

    // 13. [̲̅D̲̅][̲̅a̲̅][̲̅d̲̅]
    TEXT_ART_STYLES.add(new StyleItem("boxed_lines", "Box Line [̲̅T̲̅]", "text_art", "[̲̅T̲̅][̲̅y̲̅][̲̅p̲̅][̲̅o̲̅][̲̅D̲̅][̲̅e̲̅][̲̅v̲̅]"));

    // 14. •?((¯°·._.• Dad •._.·°¯))؟•
    TEXT_ART_STYLES.add(new StyleItem("classic_swirl", "Classic Swirl •?((", "text_art", "•?((¯°·._.• TypoDev •._.·°¯))؟•"));

    // 15. ★·.·´¯`·.·★ Dad ★·.·´¯`·.·★
    TEXT_ART_STYLES.add(new StyleItem("star_dots", "Star Dots ★·.·´¯`·.·★", "text_art", "★·.·´¯`·.·★ TypoDev ★·.·´¯`·.·★"));

    // 16. ♥ Dad ♥
    TEXT_ART_STYLES.add(new StyleItem("hearts_border", "Hearts ♥ T ♥", "text_art", "♥ T ♥ y ♥ p ♥ o ♥ D ♥ e ♥ v ♥"));

    // 17. ▀▄▀▄▀▄ Dad ▄▀▄▀▄▀
    TEXT_ART_STYLES.add(new StyleItem("blocks_checker", "Checker Blocks ▀▄▀", "text_art", "▀▄▀▄▀▄ TypoDev ▄▀▄▀▄▀"));

    // 18. ╔═══*.·:·.☽✧ ✦ ✧☾.·:·.*═══╗
    TEXT_ART_STYLES.add(new StyleItem("fancy_frame", "Aesthetic Moon Frame", "text_art", "☽✧ TypoDev ✧☾"));
  }

  private static void initKaomoji()
  {
    // 1. Love & Affection
    List<String> love = new ArrayList<>();
    love.add("(っ◔◡◔)っ ♥");
    love.add("(♥ω♥*)");
    love.add("(♡-_-♡)");
    love.add("( ˘ ³˘)♥");
    love.add("(´♡‿♡`)");
    love.add("(✿ ♡‿♡)");
    love.add("(*˘︶˘*).｡.:*♡");
    love.add("(๑♡⌓♡๑)");
    love.add("(❤ω❤)");
    KAOMOJI_CATEGORIES.add(new KaomojiCategory("❤️ Love", love));

    // 2. Happy & Joy
    List<String> happy = new ArrayList<>();
    happy.add("(◕‿◕)");
    happy.add("(✿◠‿◠)");
    happy.add("(^o^)/");
    happy.add("(•‿•)");
    happy.add("＼(^_^)／");
    happy.add("(⌒‿⌒)");
    happy.add("(*^‿^*)");
    happy.add("(≧◡≦)");
    happy.add("(*¯︶¯*)");
    KAOMOJI_CATEGORIES.add(new KaomojiCategory("😊 Happy", happy));

    // 3. Hugs & Greetings
    List<String> hugs = new ArrayList<>();
    hugs.add("(つ≧▽≦)つ");
    hugs.add("(づ｡◕‿‿◕｡)づ");
    hugs.add("(づ￣ ³￣)づ");
    hugs.add("(つ✧ω✧)つ");
    hugs.add("(っ.❛ ᴗ ❛.)っ");
    hugs.add("(⊃｡•́‿•̀｡)⊃");
    hugs.add("(っ´∀｀)っ");
    KAOMOJI_CATEGORIES.add(new KaomojiCategory("🤗 Hugs", hugs));

    // 4. Cool & Flex
    List<String> cool = new ArrayList<>();
    cool.add("(⌐■_■)");
    cool.add("(•_•) ( •_•)>⌐■-■ (⌐■_■)");
    cool.add("ᕙ(⇀‸↼‶)ᕗ");
    cool.add("ᕙ( •̀ ᗜ •́ )ᕗ");
    cool.add("(¬‿¬)");
    cool.add("(ง'̀-'́)ง");
    cool.add("ᕦ(ò_óˇ)ᕤ");
    KAOMOJI_CATEGORIES.add(new KaomojiCategory("😎 Cool & Flex", cool));

    // 5. Shrug & Confused
    List<String> shrug = new ArrayList<>();
    shrug.add("¯\\_(ツ)_/¯");
    shrug.add("¯\\(°_o)/¯");
    shrug.add("┐(￣ヘ￣)┌");
    shrug.add("(⊙_⊙)");
    shrug.add("(•ิ_•ิ)?");
    shrug.add("(・_・;)");
    shrug.add("¯\\_( ͡° ͜ʖ ͡°)_/¯");
    KAOMOJI_CATEGORIES.add(new KaomojiCategory("🤷 Shrug", shrug));

    // 6. Cute Animals
    List<String> animals = new ArrayList<>();
    animals.add("(=^･ω･^=)");
    animals.add("( =①ω①=)");
    animals.add("(=^･ｪ･^=)");
    animals.add("ʕ•ᴥ•ʔ");
    animals.add("(ᵔᴥᵔ)");
    animals.add("ʕっ•ᴥ•ʔっ");
    animals.add("(◕ᴥ◕)");
    animals.add("ฅ^•ﻌ•^ฅ");
    KAOMOJI_CATEGORIES.add(new KaomojiCategory("🐱 Animals", animals));
  }

  public static List<StyleItem> getFontStyles()
  {
    return Collections.unmodifiableList(FONT_STYLES);
  }

  public static List<StyleItem> getTextArtStyles()
  {
    return Collections.unmodifiableList(TEXT_ART_STYLES);
  }

  public static List<KaomojiCategory> getKaomojiCategories()
  {
    return Collections.unmodifiableList(KAOMOJI_CATEGORIES);
  }

  /**
   * Applies the selected style id to the given input text.
   */
  public static String applyStyle(String styleId, String text)
  {
    if (text == null) text = "";
    if (text.isEmpty()) text = "TypoDev";

    // 1. Direct Map Fonts
    String[] map = FONT_ALPHABETS.get(styleId);
    if (map != null)
    {
      return transformWithMap(map, text);
    }

    // 2. Combining / Modifier Fonts
    switch (styleId)
    {
      case "strikethrough":
        return addCombiningMark(text, "\u0336"); // Combining long stroke overlay

      case "underline":
        return addCombiningMark(text, "\u0332"); // Combining low line

      case "double_underline":
        return addCombiningMark(text, "\u0333"); // Combining double low line

      case "slashed":
        return addCombiningMark(text, "\u0337"); // Combining short solidus overlay

      case "upside_down":
        return toUpsideDown(text);

      // 3. Text Art Borders & Frames
      case "bracket_thick":
        return wrapEachChar(text, "【", "】");

      case "bracket_corner":
        return wrapEachChar(text, "『", "』");

      case "tilde_waves":
        return interleave(text, "≈");

      case "shaded_ascii":
        return interleave(text, "░");

      case "kaomoji_love_hug":
        return "(っ◔◡◔)っ ♥ " + text + " ♥";

      case "star_wings":
        return "★彡[ " + text + " ]彡★";

      case "soundwave":
        return "ıllıllı[ " + text + " ]ıllıllı";

      case "winged_stars":
        return "╰•★★ " + text + " ★★•╯";

      case "flower_smile":
        return "(✿◠‿◠) " + text + " (◠‿◠✿)";

      case "sparkle_magic":
        return "✧･ﾟ: *✧･ﾟ:* " + text + " *:･ﾟ✧*:･ﾟ✧";

      case "cute_hug":
        return "(づ｡◕‿‿◕｡)づ " + text;

      case "royal_wings":
        return "꧁༺ " + text + " ༻꧂";

      case "boxed_lines":
        return wrapEachChar(text, "[̲̅", "̲̅]");

      case "classic_swirl":
        return "•?((¯°·._.• " + text + " •._.·°¯))؟•";

      case "star_dots":
        return "★·.·´¯`·.·★ " + text + " ★·.·´¯`·.·★";

      case "hearts_border":
        return interleave(text, " ♥ ");

      case "blocks_checker":
        return "▀▄▀▄▀▄ " + text + " ▄▀▄▀▄▀";

      case "fancy_frame":
        return "☽✧ " + text + " ✧☾";

      default:
        return text;
    }
  }

  private static String transformWithMap(String[] map, String text)
  {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < text.length(); )
    {
      int cp = text.codePointAt(i);
      char c = (char)cp;
      int idx = -1;
      if (c >= 'A' && c <= 'Z')
      {
        idx = c - 'A';
      }
      else if (c >= 'a' && c <= 'z')
      {
        idx = 26 + (c - 'a');
      }
      else if (c >= '0' && c <= '9')
      {
        idx = 52 + (c - '0');
      }

      if (idx >= 0 && idx < map.length && map[idx] != null && !map[idx].isEmpty())
      {
        sb.append(map[idx]);
      }
      else
      {
        sb.append(new String(Character.toChars(cp)));
      }
      i += Character.charCount(cp);
    }
    return sb.toString();
  }

  private static String addCombiningMark(String text, String mark)
  {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < text.length(); )
    {
      int cp = text.codePointAt(i);
      sb.append(new String(Character.toChars(cp)));
      if (!Character.isWhitespace(cp))
      {
        sb.append(mark);
      }
      i += Character.charCount(cp);
    }
    return sb.toString();
  }

  private static String wrapEachChar(String text, String prefix, String suffix)
  {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < text.length(); )
    {
      int cp = text.codePointAt(i);
      if (Character.isWhitespace(cp))
      {
        sb.append(" ");
      }
      else
      {
        sb.append(prefix);
        sb.append(new String(Character.toChars(cp)));
        sb.append(suffix);
      }
      i += Character.charCount(cp);
    }
    return sb.toString();
  }

  private static String interleave(String text, String separator)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(separator.trim());
    for (int i = 0; i < text.length(); )
    {
      int cp = text.codePointAt(i);
      sb.append(new String(Character.toChars(cp)));
      sb.append(separator);
      i += Character.charCount(cp);
    }
    return sb.toString().trim();
  }

  private static final Map<Character, Character> UPSIDE_DOWN_MAP = new HashMap<>();
  static
  {
    String normal = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789,.?!'\"_&";
    String flipped = "ɐqɔpǝɟɓɥıɾʞlɯuodbɹsʇnʌʍxʎz∀ᗺƆᗡƎℲ⅁HIſʞ˥WNOԀÒᴚS⊥∩ΛMX⅄Z0ƖᄅƐㄣϛ9ㄥ86'˙¿¡,„‾⅋";
    for (int i = 0; i < Math.min(normal.length(), flipped.length()); i++)
    {
      UPSIDE_DOWN_MAP.put(normal.charAt(i), flipped.charAt(i));
    }
  }

  private static String toUpsideDown(String text)
  {
    StringBuilder sb = new StringBuilder();
    for (int i = text.length() - 1; i >= 0; i--)
    {
      char c = text.charAt(i);
      Character f = UPSIDE_DOWN_MAP.get(c);
      sb.append(f != null ? f : c);
    }
    return sb.toString();
  }
}
