package juloo.keyboard2;

import java.util.List;
import juloo.keyboard2.suggestions.NextWordPredictor;
import org.junit.Test;
import static org.junit.Assert.*;

public class NextWordPredictorTest
{
  @Test
  public void testBengaliCollocations()
  {
    NextWordPredictor predictor = NextWordPredictor.instance(null);

    // 1. "ভালো" must have "বাসি" as top candidate
    List<String> predValo = predictor.predict("ভালো", 5);
    assertFalse("Predictions for ভালো should not be empty", predValo.isEmpty());
    assertEquals("First prediction for ভালো must be বাসি", "বাসি", predValo.get(0));

    // 2. Phrase "তোমাকে ভালো" must have "বাসি" as top candidate
    List<String> predPhrase = predictor.predict("তোমাকে ভালো", "ভালো", 5);
    assertFalse("Predictions for তোমাকে ভালো should not be empty", predPhrase.isEmpty());
    assertEquals("First prediction for তোমাকে ভালো must be বাসি", "বাসি", predPhrase.get(0));

    // 3. "আমি তোমাকে" must have "ভালো" or "ভালোবাসি"
    List<String> predAmiTomake = predictor.predict("আমি তোমাকে", "তোমাকে", 5);
    assertTrue("Should contain ভালো", predAmiTomake.contains("ভালো") || predAmiTomake.contains("ভালোবাসি"));

    // 4. English "thank you" -> "so" / "very" / "much" / "for"
    List<String> predThankYou = predictor.predict("thank you", "you", 5);
    assertTrue("Should contain much or so", predThankYou.contains("much") || predThankYou.contains("so") || predThankYou.contains("very"));
  }

  @Test
  public void testAdaptiveLearning()
  {
    NextWordPredictor predictor = NextWordPredictor.instance(null);
    predictor.learn("বৃষ্টি", "পড়ছে");

    List<String> preds = predictor.predict("বৃষ্টি", 3);
    assertTrue("Learned word must be in predictions", preds.contains("পড়ছে"));
    assertEquals("Most recently learned word should be first", "পড়ছে", preds.get(0));
  }
}
