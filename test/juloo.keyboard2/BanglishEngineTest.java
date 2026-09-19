package juloo.keyboard2;

import java.util.List;
import juloo.keyboard2.suggestions.BanglishEngine;
import juloo.keyboard2.suggestions.Candidate;
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
  }

  @Test
  public void testGenerateCandidates()
  {
    BanglishEngine engine = BanglishEngine.instance();

    List<Candidate> cands = engine.generateCandidates("ami", 3);
    assertFalse("Should generate candidates for ami", cands.isEmpty());
    assertEquals("Top candidate for ami must be আমি", "আমি", cands.get(0).word);
    assertEquals(Candidate.Source.BANGLISH, cands.get(0).source);
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
  public void testEnglishPreservation()
  {
    // Ensure English words are not falsely altered by transliteration engine
    assertTrue(BanglishEngine.isLatinOnly("keyboard"));
    assertTrue(BanglishEngine.isLatinOnly("TypoDev"));
    assertFalse(BanglishEngine.isLatinOnly("বাংলা"));
  }
}
