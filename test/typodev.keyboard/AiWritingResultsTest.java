package typodev.keyboard;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;
import typodev.keyboard.ai.AiVisionHelper;

public class AiWritingResultsTest {
  @Test public void responseHasAtMostThreeDistinctCleanOptions() {
    assertEquals(Arrays.asList("প্রথম উত্তর", "দ্বিতীয় উত্তর", "তৃতীয় উত্তর"),
        AiVisionHelper.parseGeneratedComments("```text\n1. প্রথম উত্তর\n1. প্রথম উত্তর\n\n- দ্বিতীয় উত্তর\n“তৃতীয় উত্তর”\nচতুর্থ উত্তর\n```"));
  }

  @Test public void incompleteResponsesRemainIncompleteRatherThanDuplicatingResults() {
    assertTrue(AiVisionHelper.parseGeneratedComments(null).isEmpty());
    assertTrue(AiVisionHelper.parseGeneratedComments("```\n\n```").isEmpty());
    assertEquals(1, AiVisionHelper.parseGeneratedComments("একটি উত্তর\nএকটি উত্তর").size());
  }

  @Test public void writingStylesCarryTheirIntendedInstructions() {
    assertTrue(AiVisionHelper.getToneInstruction("প্রফেশনাল").contains("Professional"));
    assertTrue(AiVisionHelper.getToneInstruction("প্রুফরিড").contains("minimal changes"));
    assertTrue(AiVisionHelper.getToneInstruction("সংক্ষিপ্ত").contains("Concise"));
    assertTrue(AiVisionHelper.buildTextCommentPrompt("আমার পোস্ট", "প্রফেশনাল").contains("EXACTLY 3"));
  }
}
