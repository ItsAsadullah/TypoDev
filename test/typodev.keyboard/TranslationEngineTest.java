package typodev.keyboard;

import org.junit.BeforeClass;
import org.junit.Test;
import typodev.keyboard.translate.TranslationEngine;
import java.io.File;
import java.io.FileInputStream;
import static org.junit.Assert.*;

public class TranslationEngineTest
{
  @BeforeClass
  public static void setUp() throws Exception
  {
    File dictFile = new File("assets/offline_dict_en_bn.json");
    assertTrue("offline_dict_en_bn.json must exist in assets", dictFile.exists());
    FileInputStream fis = new FileInputStream(dictFile);
    TranslationEngine.loadOfflinePackFromStream(fis);
    fis.close();
  }

  @Test
  public void testOfflineTranslateBengaliToEnglishSinglePhrase()
  {
    String result = TranslationEngine.translateOffline(null, "এখনো সময় আছে", "bn", "en");
    assertNotNull("Translation should not be null", result);
    assertEquals("there is still time", result.trim());
  }

  @Test
  public void testOfflineTranslateBengaliToEnglishSentenceWithDari()
  {
    String result = TranslationEngine.translateOffline(null, "সময় এখনো শেষ হয়ে যায় নি।", "bn", "en");
    assertNotNull("Translation should not be null", result);
    assertTrue("Should translate sentence", result.contains("time is not over yet"));
  }

  @Test
  public void testOfflineTranslateEnglishToBengali()
  {
    String result = TranslationEngine.translateOffline(null, "there is still time", "en", "bn");
    assertNotNull("Translation should not be null", result);
    assertEquals("এখনো সময় আছে", result.trim());
  }

  @Test
  public void testOfflineTranslateWords()
  {
    String result = TranslationEngine.translateOffline(null, "ধন্যবাদ", "bn", "en");
    assertNotNull(result);
    assertEquals("thank you", result.trim());

    String resultEn = TranslationEngine.translateOffline(null, "welcome", "en", "bn");
    assertNotNull(resultEn);
    assertTrue(resultEn.contains("স্বাগতম"));
  }

  @Test
  public void testOfflineTranslateUserSentenceFromScreenshot()
  {
    String input = "Hello, My Name is asadullah, I'm a Software";
    String result = TranslationEngine.translateOffline(null, input, "en", "bn");
    assertNotNull("Translation result should not be null", result);
    assertTrue("Should translate Hello to হ্যালো", result.contains("হ্যালো"));
    assertTrue("Should translate My Name is to আমার নাম", result.contains("আমার নাম"));
    assertTrue("Should translate I'm a to আমি একজন", result.contains("আমি একজন"));
    assertTrue("Should translate Software to সফটওয়্যার", result.contains("সফটওয়্যার"));
  }
}
