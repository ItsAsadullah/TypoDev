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

    // Test Social Media Prompt
    String socialPrompt = AiActionEngine.buildSystemPrompt(
        AiActionEngine.Category.SOCIAL, "soc_li", "default", null);
    assertTrue(socialPrompt.contains("LinkedIn"));

    // Test Translate Prompt
    String trPrompt = AiActionEngine.buildSystemPrompt(
        AiActionEngine.Category.TRANSLATE, "tr_bn", "default", null);
    assertTrue(trPrompt.contains("Bengali"));

    // Test Naturalize Prompt
    String natPrompt = AiActionEngine.buildSystemPrompt(
        AiActionEngine.Category.NATURALIZE, "nat_human", "default", null);
    assertTrue(natPrompt.contains("Humanize and naturalize"));
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
}
