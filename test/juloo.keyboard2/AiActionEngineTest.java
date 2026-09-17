package juloo.keyboard2;

import java.util.List;
import juloo.keyboard2.ai.AiActionEngine;
import org.junit.Test;
import static org.junit.Assert.*;

public class AiActionEngineTest
{
  @Test
  public void testContextDetection()
  {
    // 1. Question detection
    AiActionEngine.ContextDetectionResult qRes = AiActionEngine.analyzeContext("Are you coming to the party tonight?");
    assertEquals("Question / Invitation", qRes.detectedType);
    assertFalse(qRes.quickSuggestions.isEmpty());

    // 2. Request detection
    AiActionEngine.ContextDetectionResult reqRes = AiActionEngine.analyzeContext("Can you please send me the final report by 5 PM?");
    assertEquals("Action Request", reqRes.detectedType);
    assertFalse(reqRes.quickSuggestions.isEmpty());

    // 3. Code detection
    String code = "function calculateSum(a, b) {\n  return a + b;\n}";
    AiActionEngine.ContextDetectionResult codeRes = AiActionEngine.analyzeContext(code);
    assertEquals("Code Snippet", codeRes.detectedType);
    assertEquals(AiActionEngine.Category.EXPLAIN, codeRes.recommendedCategory);

    // 4. Bengali question
    AiActionEngine.ContextDetectionResult bnRes = AiActionEngine.analyzeContext("তুমি কি কালকে আসতে পারবে?");
    assertTrue(bnRes.detectedType.contains("Question"));
    assertFalse(bnRes.quickSuggestions.isEmpty());
  }

  @Test
  public void testPromptBuilding()
  {
    // Test Email Prompt
    String emailPrompt = AiActionEngine.buildSystemPrompt(
        AiActionEngine.Category.EMAIL, "email_meeting", "Professional", null);
    assertTrue(emailPrompt.contains("Subject:"));
    assertTrue(emailPrompt.contains("Meeting invitation"));
    assertTrue(emailPrompt.contains("Professional"));

    // Test Reply Prompt
    String replyPrompt = AiActionEngine.buildSystemPrompt(
        AiActionEngine.Category.REPLY, "reply_funny", "default", null);
    assertTrue(replyPrompt.contains("humorous reply"));

    // Test Social Media Prompt with Hashtag rule
    String socialPrompt = AiActionEngine.buildSystemPrompt(
        AiActionEngine.Category.SOCIAL, "soc_li", "default", null);
    assertTrue(socialPrompt.contains("LinkedIn"));
    assertTrue(socialPrompt.contains("HASHTAG"));

    // Test Translate Prompt (Bengali)
    String trPrompt = AiActionEngine.buildSystemPrompt(
        AiActionEngine.Category.TRANSLATE, "tr_bn", "default", null);
    assertTrue(trPrompt.contains("Bengali"));

    // Test Translate Prompt (Banglish)
    String banglishPrompt = AiActionEngine.buildSystemPrompt(
        AiActionEngine.Category.TRANSLATE, "tr_banglish", "default", null);
    assertTrue(banglishPrompt.contains("Banglish"));
    assertTrue(banglishPrompt.contains("Latin/English letters"));

    // Test Islamic Category Prompts
    String islamicPrompt = AiActionEngine.buildSystemPrompt(
        AiActionEngine.Category.ISLAMIC, "islamic_standard", "default", null);
    assertTrue(islamicPrompt.contains("সালাম"));
    assertTrue(islamicPrompt.contains("আসসালামু আলাইকুম"));
    assertTrue(islamicPrompt.contains("আলহামদুলিল্লাহ"));
    assertTrue(islamicPrompt.contains("মাশাআল্লাহ"));
    assertTrue(islamicPrompt.contains("ইনশাআল্লাহ্"));
    assertTrue(islamicPrompt.contains("ইন্না লিল্লাহ"));
    assertTrue(islamicPrompt.contains("জাযাকাল্লাহু খাইরান"));
    assertTrue(islamicPrompt.contains("খুশির সংবাদ"));
    assertTrue(islamicPrompt.contains("ভবিষ্যতের ইচ্ছা"));

    String islamicArabicPrompt = AiActionEngine.buildSystemPrompt(
        AiActionEngine.Category.ISLAMIC, "islamic_arabic", "default", null);
    assertTrue(islamicArabicPrompt.contains("Arabic script"));
    assertTrue(islamicArabicPrompt.contains("السلام عليكم"));

    String islamicPostPrompt = AiActionEngine.buildSystemPrompt(
        AiActionEngine.Category.ISLAMIC, "islamic_post", "default", null);
    assertTrue(islamicPostPrompt.contains("hashtags"));

    // Test Naturalize Prompt
    String natPrompt = AiActionEngine.buildSystemPrompt(
        AiActionEngine.Category.NATURALIZE, "nat_human", "default", null);
    assertTrue(natPrompt.contains("Humanize and naturalize"));
  }

  @Test
  public void testCategoryPriorityOrder()
  {
    AiActionEngine.Category[] cats = AiActionEngine.Category.values();
    assertEquals("First category must be GRAMMAR", AiActionEngine.Category.GRAMMAR, cats[0]);
    assertEquals("Second category must be TRANSLATE", AiActionEngine.Category.TRANSLATE, cats[1]);
    assertEquals("Third category must be REWRITE", AiActionEngine.Category.REWRITE, cats[2]);
    assertEquals("Fourth category must be ISLAMIC", AiActionEngine.Category.ISLAMIC, cats[3]);
  }

  @Test
  public void testCategoriesAndOptions()
  {
    for (AiActionEngine.Category cat : AiActionEngine.Category.values())
    {
      List<AiActionEngine.ActionOption> opts = AiActionEngine.getOptionsForCategory(cat);
      assertNotNull("Options for " + cat.name() + " should not be null", opts);
      assertFalse("Options for " + cat.name() + " should not be empty", opts.isEmpty());
    }
  }

  @Test
  public void testBengaliSanitization()
  {
    // Test broken conjunct with space around hasant: "ক ্ ষ" -> "ক্ষ"
    String brokenConjunct = "ক ্ ষ";
    String cleanConjunct = AiActionEngine.sanitizeBengaliAndUnicode(brokenConjunct);
    assertEquals("ক্ষ", cleanConjunct);

    // Test broken vowel sign with space: "ক া" -> "কা"
    String brokenVowel = "ক া";
    String cleanVowel = AiActionEngine.sanitizeBengaliAndUnicode(brokenVowel);
    assertEquals("কা", cleanVowel);
  }
}
