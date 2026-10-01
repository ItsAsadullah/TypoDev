package typodev.keyboard;

import java.util.List;
import typodev.keyboard.suggestions.BanglishEngine;
import typodev.keyboard.suggestions.Candidate;
import org.junit.Test;
import static org.junit.Assert.*;

public class BanglishEngineTest
{
  @Test
  public void testCommonBanglishWords()
  {
    BanglishEngine engine = BanglishEngine.instance();

    // 1. "ami" -> "আমি"
    assertEquals("আমি", engine.getExactBangla("ami"));

    // 2. "tumi" -> "তুমি"
    assertEquals("তুমি", engine.getExactBangla("tumi"));

    // 3. "kemon" -> "কেমন"
    assertEquals("কেমন", engine.getExactBangla("kemon"));

    // 4. "bhalo" / "valo" -> "ভালো"
    assertEquals("ভালো", engine.getExactBangla("bhalo"));
    assertEquals("ভালো", engine.getExactBangla("valo"));

    // 5. "dhonnobad" -> "ধন্যবাদ"
    assertEquals("ধন্যবাদ", engine.getExactBangla("dhonnobad"));

    // 6. "bangladesh" -> "বাংলাদেশ"
    assertEquals("বাংলাদেশ", engine.getExactBangla("bangladesh"));

    // 7. National Anthem: "amar", "sonar", "bangla"
    assertEquals("আমার", engine.getExactBangla("amar"));
    assertEquals("সোনার", engine.getExactBangla("sonar"));
    assertEquals("সোনার", engine.getExactBangla("shonar"));
    assertEquals("বাংলা", engine.getExactBangla("bangla"));
  }

  @Test
  public void testGenerateCandidates()
  {
    BanglishEngine engine = BanglishEngine.instance();

    List<Candidate> cands = engine.generateCandidates("ami", 3);
    assertFalse("Should generate candidates for ami", cands.isEmpty());
    assertEquals("Top candidate for ami must be আমি", "আমি", cands.get(0).word);
    assertTrue(cands.get(0).source == Candidate.Source.AUTOCORRECT || cands.get(0).source == Candidate.Source.BANGLISH);

    // Test "amar", "sonar", "bangla" candidates
    List<Candidate> amarCands = engine.generateCandidates("amar", 3);
    assertEquals("আমার", amarCands.get(0).word);

    List<Candidate> sonarCands = engine.generateCandidates("sonar", 3);
    assertEquals("সোনার", sonarCands.get(0).word);

    List<Candidate> banglaCands = engine.generateCandidates("bangla", 3);
    assertEquals("বাংলা", banglaCands.get(0).word);

    // Test single-letter candidate generation
    List<Candidate> aCands = engine.generateCandidates("a", 4);
    assertFalse(aCands.isEmpty());
    assertEquals("আ", aCands.get(0).word);
  }

  @Test
  public void testPhoneticFallback()
  {
    BanglishEngine engine = BanglishEngine.instance();

    // Word not in high-frequency list: "koto"
    String phonetic = BanglishEngine.transliteratePhonetic("koto");
    assertTrue("Should contain Bengali script", BanglishEngine.isBengaliScript(phonetic));
  }

  @Test
  public void testAvroRAndDDA()
  {
    BanglishEngine engine = BanglishEngine.instance();

    // 1. Capital 'R' must generate 'ড়' as top candidate
    List<Candidate> capRCands = engine.generateCandidates("R", 4);
    assertFalse(capRCands.isEmpty());
    assertEquals("ড়", capRCands.get(0).word);

    // 2. Lowercase 'r' must offer 'ড়' as an alternative candidate
    List<Candidate> lowRCands = engine.generateCandidates("r", 4);
    assertFalse(lowRCands.isEmpty());
    assertEquals("র", lowRCands.get(0).word);
    boolean hasDDA = false;
    for (Candidate c : lowRCands)
    {
      if ("ড়".equals(c.word)) { hasDDA = true; break; }
    }
    assertTrue("Single 'r' must contain 'ড়' in suggestions", hasDDA);

    // 3. Common words with 'ড়' typed with both 'r' and 'R'
    assertEquals("গাড়ি", engine.getExactBangla("gari"));
    assertEquals("গাড়িওয়ালা", engine.getExactBangla("gariwala"));
    assertEquals("বাড়ি", engine.getExactBangla("bari"));
    assertEquals("বড়", engine.getExactBangla("boro"));
    assertEquals("পড়া", engine.getExactBangla("pora"));
    assertEquals("পড়াশোনা", engine.getExactBangla("porashona"));
    assertEquals("ছেড়ে", engine.getExactBangla("chere"));
    assertEquals("ছেড়ে", engine.getExactBangla("chheRe"));
    assertEquals("পাহাড়", engine.getExactBangla("pahar"));
    assertEquals("তাড়াতাড়ি", engine.getExactBangla("taratari"));
    assertEquals("দৌড়", engine.getExactBangla("dour"));

    // 4. Candidate generation for 'gari' and 'gaRi'
    List<Candidate> gariCands = engine.generateCandidates("gari", 3);
    assertEquals("গাড়ি", gariCands.get(0).word);

    List<Candidate> gaRiCands = engine.generateCandidates("gaRi", 3);
    assertEquals("গাড়ি", gaRiCands.get(0).word);

    List<Candidate> bariCands = engine.generateCandidates("bari", 3);
    assertEquals("বাড়ি", bariCands.get(0).word);
  }

  @Test
  public void testAvroZAndJa()
  {
    BanglishEngine engine = BanglishEngine.instance();

    // 1. 'z' and 'Z' must generate 'য' as top candidate
    List<Candidate> zCands = engine.generateCandidates("z", 4);
    assertFalse(zCands.isEmpty());
    assertEquals("য", zCands.get(0).word);

    List<Candidate> capZCands = engine.generateCandidates("Z", 4);
    assertFalse(capZCands.isEmpty());
    assertEquals("য", capZCands.get(0).word);

    // 2. 'y' and 'j' must also offer 'য' as an alternative candidate
    List<Candidate> yCands = engine.generateCandidates("y", 4);
    boolean yHasJa = false;
    for (Candidate c : yCands) { if ("য".equals(c.word)) { yHasJa = true; break; } }
    assertTrue("'y' must offer 'য'", yHasJa);

    List<Candidate> jCands = engine.generateCandidates("j", 4);
    boolean jHasJa = false;
    for (Candidate c : jCands) { if ("য".equals(c.word)) { jHasJa = true; break; } }
    assertTrue("'j' must offer 'য'", jHasJa);

    // 3. Words starting with 'য' typed with either 'z' or 'j'
    assertEquals("যদি", engine.getExactBangla("jodi"));
    assertEquals("যদি", engine.getExactBangla("zodi"));
    assertEquals("যেমন", engine.getExactBangla("jemon"));
    assertEquals("যেমন", engine.getExactBangla("zemon"));
    assertEquals("যখন", engine.getExactBangla("jokhon"));
    assertEquals("যখন", engine.getExactBangla("zokhon"));
    assertEquals("যাবে", engine.getExactBangla("jabe"));
    assertEquals("যাবে", engine.getExactBangla("zabe"));
    assertEquals("যাবো", engine.getExactBangla("jabo"));
    assertEquals("যাবো", engine.getExactBangla("zabo"));
    assertEquals("যাওয়া", engine.getExactBangla("jawa"));
    assertEquals("যাওয়া", engine.getExactBangla("zawa"));
    assertEquals("যায়", engine.getExactBangla("jay"));
    assertEquals("যায়", engine.getExactBangla("zay"));
    assertEquals("যারা", engine.getExactBangla("jara"));
    assertEquals("যারা", engine.getExactBangla("zara"));
    assertEquals("যাদের", engine.getExactBangla("jader"));
    assertEquals("যাদের", engine.getExactBangla("zader"));
    assertEquals("যাকে", engine.getExactBangla("jake"));
    assertEquals("যাকে", engine.getExactBangla("zake"));
    assertEquals("যাতে", engine.getExactBangla("jate"));
    assertEquals("যাতে", engine.getExactBangla("zate"));
    assertEquals("যত", engine.getExactBangla("joto"));
    assertEquals("যত", engine.getExactBangla("zoto"));
    assertEquals("যত্ন", engine.getExactBangla("jotno"));
    assertEquals("যুদ্ধ", engine.getExactBangla("juddho"));
    assertEquals("যোগাযোগ", engine.getExactBangla("jogajog"));
    assertEquals("যাত্রা", engine.getExactBangla("jatra"));
    assertEquals("যাত্রী", engine.getExactBangla("jatri"));
    assertEquals("যথেষ্ট", engine.getExactBangla("jothestho"));
    assertEquals("যা", engine.getExactBangla("ja"));
    assertEquals("যা", engine.getExactBangla("za"));
    assertEquals("যে", engine.getExactBangla("je"));
    assertEquals("যে", engine.getExactBangla("ze"));
  }

  @Test
  public void testAvroRhaAndBisarga()
  {
    BanglishEngine engine = BanglishEngine.instance();

    assertEquals("ঢ়", BanglishEngine.transliteratePhonetic("Rh"));
    assertEquals("দৃঢ়", engine.getExactBangla("driro"));
    assertEquals("আষাঢ়", engine.getExactBangla("ashadh"));

    List<Candidate> hCands = engine.generateCandidates("H", 3);
    assertFalse(hCands.isEmpty());
    assertEquals("ঃ", hCands.get(0).word);
  }

  @Test
  public void testPopularVerbsAndDailySpeech()
  {
    BanglishEngine engine = BanglishEngine.instance();

    assertEquals("করছি", engine.getExactBangla("korchi"));
    String korboWord = engine.getExactBangla("korbo");
    assertTrue("korbo should be either করব or করবো", "করব".equals(korboWord) || "করবো".equals(korboWord));
    assertEquals("হবে", engine.getExactBangla("hobe"));
    assertEquals("হয়েছে", engine.getExactBangla("hoyeche"));
    assertEquals("হলো", engine.getExactBangla("holo"));
    assertEquals("হচ্ছে", engine.getExactBangla("hochhe"));
    assertEquals("ছিল", engine.getExactBangla("chilo"));
    assertEquals("থাকব", engine.getExactBangla("thakbo"));
    assertEquals("দেখতে", engine.getExactBangla("dekhte"));
    assertEquals("বলতে", engine.getExactBangla("bolte"));
    assertEquals("পারব", engine.getExactBangla("parbo"));
    assertEquals("দিতে", engine.getExactBangla("dite"));
    assertEquals("নিতে", engine.getExactBangla("nite"));
    assertEquals("জানি", engine.getExactBangla("jani"));
    assertEquals("জানিনা", engine.getExactBangla("janina"));
    assertEquals("ভাবছি", engine.getExactBangla("bhabchi"));
    assertEquals("বুঝতে", engine.getExactBangla("bujhte"));
    assertEquals("আসছি", engine.getExactBangla("ashchi"));
    assertEquals("গেলাম", engine.getExactBangla("gelam"));
    assertEquals("কিন্তু", engine.getExactBangla("kintu"));
    assertEquals("এবং", engine.getExactBangla("ebong"));
    assertEquals("ঢাকা", engine.getExactBangla("dhaka"));
    assertEquals("ডাক্তার", engine.getExactBangla("daktar"));
  }

  @Test
  public void testJeneAndRakhaFixes()
  {
    BanglishEngine engine = BanglishEngine.instance();

    // 1. "rakha" must produce "রাখা" as top candidate, NEVER "ড়আখা" or "ড়াখা"
    List<Candidate> rakhaCands = engine.generateCandidates("rakha", 5);
    assertFalse(rakhaCands.isEmpty());
    assertEquals("Top candidate for 'rakha' must be 'রাখা'", "রাখা", rakhaCands.get(0).word);
    for (Candidate c : rakhaCands)
    {
      assertNotEquals("Should never generate broken 'ড়আখা'", "ড়আখা", c.word);
      assertFalse("No candidate starting with 'r' should start with ড়", c.word.startsWith("ড়"));
    }

    // 2. "jene" must produce "জেনে" as top candidate, NEVER "যেনে"
    List<Candidate> jeneCands = engine.generateCandidates("jene", 5);
    assertFalse(jeneCands.isEmpty());
    assertEquals("Top candidate for 'jene' must be 'জেনে'", "জেনে", jeneCands.get(0).word);
    for (Candidate c : jeneCands)
    {
      assertNotEquals("Should never generate fake word 'যেনে'", "যেনে", c.word);
    }

    // 3. Avro capital 'R' produces 'ড়' with proper vowel combining (e.g. gaRi -> গাড়ি, not গাড়ই)
    assertEquals("গাড়ি", BanglishEngine.transliteratePhonetic("gaRi"));
    assertEquals("বাড়ি", BanglishEngine.transliteratePhonetic("baRi"));

    // 4. Other words starting with 'r' must never start with 'ড়'
    List<Candidate> rastaCands = engine.generateCandidates("rasta", 4);
    assertEquals("রাস্তা", rastaCands.get(0).word);

    List<Candidate> raatCands = engine.generateCandidates("raat", 4);
    assertEquals("রাত", raatCands.get(0).word);

    // 5. Normal 'j' words must not turn into 'য'
    List<Candidate> janiCands = engine.generateCandidates("jani", 4);
    assertEquals("জানি", janiCands.get(0).word);

    List<Candidate> jonnoCands = engine.generateCandidates("jonno", 4);
    assertEquals("জন্য", jonnoCands.get(0).word);

    // 6. Test full ranking pipeline with CandidateRanker
    List<Candidate> rankedRakha = typodev.keyboard.suggestions.CandidateRanker.rank(rakhaCands, 3);
    assertEquals("Ranked #1 for 'rakha' must be 'রাখা'", "রাখা", rankedRakha.get(0).word);

    List<Candidate> rankedJene = typodev.keyboard.suggestions.CandidateRanker.rank(jeneCands, 3);
    assertEquals("Ranked #1 for 'jene' must be 'জেনে'", "জেনে", rankedJene.get(0).word);
  }

  @Test
  public void testBanglishConjunctsAndAvroRef()
  {
    BanglishEngine engine = BanglishEngine.instance();

    // 1. "khoma" -> "ক্ষমা" (Screenshot 1 issue)
    List<Candidate> khomaCands = engine.generateCandidates("khoma", 4);
    assertFalse("Candidates for 'khoma' must not be empty", khomaCands.isEmpty());
    assertEquals("Top candidate for 'khoma' must be 'ক্ষমা'", "ক্ষমা", khomaCands.get(0).word);

    // 2. "bortomane" -> "বর্তমানে" and "bortoman" -> "বর্তমান" (Screenshot 2 issue)
    List<Candidate> bortomaneCands = engine.generateCandidates("bortomane", 4);
    assertFalse("Candidates for 'bortomane' must not be empty", bortomaneCands.isEmpty());
    assertEquals("Top candidate for 'bortomane' must be 'বর্তমানে'", "বর্তমানে", bortomaneCands.get(0).word);
    assertEquals("Phonetic transliteration of 'bortomane' must be 'বর্তমানে'", "বর্তমানে", BanglishEngine.transliteratePhonetic("bortomane"));

    List<Candidate> bortomanCands = engine.generateCandidates("bortoman", 4);
    assertEquals("Top candidate for 'bortoman' must be 'বর্তমান'", "বর্তমান", bortomanCands.get(0).word);
    assertEquals("Phonetic transliteration of 'bortoman' must be 'বর্তমান'", "বর্তমান", BanglishEngine.transliteratePhonetic("bortoman"));

    // 3. "kkhoma" -> "ক্ষমা"
    List<Candidate> kkhomaCands = engine.generateCandidates("kkhoma", 4);
    assertEquals("Top candidate for 'kkhoma' must be 'ক্ষমা'", "ক্ষমা", kkhomaCands.get(0).word);
    assertEquals("ক্ষমা", BanglishEngine.transliteratePhonetic("kkhoma"));

    // 4. "kosto" -> "কষ্ট"
    List<Candidate> kostoCands = engine.generateCandidates("kosto", 4);
    assertEquals("Top candidate for 'kosto' must be 'কষ্ট'", "কষ্ট", kostoCands.get(0).word);

    // 5. "poriksha" and "porikkha" -> "পরীক্ষা"
    List<Candidate> porikshaCands = engine.generateCandidates("poriksha", 4);
    assertEquals("Top candidate for 'poriksha' must be 'পরীক্ষা'", "পরীক্ষা", porikshaCands.get(0).word);

    List<Candidate> porikkhaCands = engine.generateCandidates("porikkha", 4);
    assertEquals("Top candidate for 'porikkha' must be 'পরীক্ষা'", "পরীক্ষা", porikkhaCands.get(0).word);

    // 6. "shikkha" -> "শিক্ষা"
    List<Candidate> shikkhaCands = engine.generateCandidates("shikkha", 4);
    assertEquals("Top candidate for 'shikkha' must be 'শিক্ষা'", "শিক্ষা", shikkhaCands.get(0).word);
    assertEquals("শিক্ষা", BanglishEngine.transliteratePhonetic("shikkha"));

    // 7. "gyan" and "ggan" -> "জ্ঞান"
    List<Candidate> gyanCands = engine.generateCandidates("gyan", 4);
    assertEquals("Top candidate for 'gyan' must be 'জ্ঞান'", "জ্ঞান", gyanCands.get(0).word);

    List<Candidate> gganCands = engine.generateCandidates("ggan", 4);
    assertEquals("Top candidate for 'ggan' must be 'জ্ঞান'", "জ্ঞান", gganCands.get(0).word);
    assertEquals("জ্ঞান", BanglishEngine.transliteratePhonetic("ggan"));

    // 8. "shobdo", "dhormo", "kormo", "anondo", "shanto", "bondhu", "uttor", "proshno", "prothom"
    assertEquals("Top candidate for 'shobdo' must be 'শব্দ'", "শব্দ", engine.generateCandidates("shobdo", 3).get(0).word);
    assertEquals("Top candidate for 'dhormo' must be 'ধর্ম'", "ধর্ম", engine.generateCandidates("dhormo", 3).get(0).word);
    assertEquals("Top candidate for 'kormo' must be 'কর্ম'", "কর্ম", engine.generateCandidates("kormo", 3).get(0).word);
    assertEquals("Top candidate for 'anondo' must be 'আনন্দ'", "আনন্দ", engine.generateCandidates("anondo", 3).get(0).word);
    assertEquals("Top candidate for 'shanto' must be 'শান্ত'", "শান্ত", engine.generateCandidates("shanto", 3).get(0).word);
    assertEquals("Top candidate for 'bondhu' must be 'বন্ধু'", "বন্ধু", engine.generateCandidates("bondhu", 3).get(0).word);
    assertEquals("Top candidate for 'uttor' must be 'উত্তর'", "উত্তর", engine.generateCandidates("uttor", 3).get(0).word);
    assertEquals("Top candidate for 'proshno' must be 'প্রশ্ন'", "প্রশ্ন", engine.generateCandidates("proshno", 3).get(0).word);
    assertEquals("Top candidate for 'prothom' must be 'প্রথম'", "প্রথম", engine.generateCandidates("prothom", 3).get(0).word);
    assertEquals("Top candidate for 'juktakkhor' must be 'যুক্তাক্ষর'", "যুক্তাক্ষর", engine.generateCandidates("juktakkhor", 3).get(0).word);

    // 9. Conjunct phonetic rules in transliteratePhonetic:
    assertEquals("রক্ত", BanglishEngine.transliteratePhonetic("rokt"));
    assertEquals("শক্তি", BanglishEngine.transliteratePhonetic("shokti"));
    assertEquals("ভক্তি", BanglishEngine.transliteratePhonetic("bhokti"));
    assertEquals("মুক্তি", BanglishEngine.transliteratePhonetic("mukti"));
    assertEquals("ইচ্ছা", BanglishEngine.transliteratePhonetic("iccha"));
    assertEquals("স্বাধীন", engine.generateCandidates("swadhin", 3).get(0).word);
    assertEquals("স্বাধীন", BanglishEngine.transliteratePhonetic("swadhIn"));
    assertEquals("রাস্তা", BanglishEngine.transliteratePhonetic("rasta"));
  }

  @Test
  public void testNewBanglishVocabulary()
  {
    BanglishEngine engine = BanglishEngine.instance();

    // English Loanwords
    assertEquals("সাজেস্ট", engine.generateCandidates("suggest", 3).get(0).word);
    assertEquals("কীবোর্ড", engine.generateCandidates("keyboard", 3).get(0).word);
    assertEquals("অ্যাড", engine.generateCandidates("add", 3).get(0).word);

    // Conversational Banglish
    assertEquals("এইযে", engine.generateCandidates("eyje", 3).get(0).word);
    assertEquals("কিছু", engine.generateCandidates("kisu", 3).get(0).word);
    assertEquals("ক্ষেত্রে", engine.generateCandidates("khetre", 3).get(0).word);
    assertEquals("করছে", engine.generateCandidates("korse", 3).get(0).word);
    assertEquals("লিখতে", engine.generateCandidates("likhte", 3).get(0).word);
    assertEquals("কথা", engine.generateCandidates("kotha", 3).get(0).word);
  }
}

