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

    // 3. "আমি তোমাকে" must have "ভালোবাসি" or "ভালো"
    List<String> predAmiTomake = predictor.predict("আমি তোমাকে", "তোমাকে", 5);
    assertTrue("Should contain ভালো or ভালোবাসি", predAmiTomake.contains("ভালো") || predAmiTomake.contains("ভালোবাসি"));

    // 4. English "thank you" -> "so" / "very" / "much" / "for"
    List<String> predThankYou = predictor.predict("thank you", "you", 5);
    assertTrue("Should contain much or so", predThankYou.contains("much") || predThankYou.contains("so") || predThankYou.contains("very"));
  }

  @Test
  public void testPredictFromSentenceBengali()
  {
    NextWordPredictor predictor = NextWordPredictor.instance(null);

    // Context: "আমি তোমাকে "
    List<String> pred1 = predictor.predictFromSentence("আমি তোমাকে ", 8);
    assertFalse(pred1.isEmpty());
    assertTrue("Must contain ভালোবাসি", pred1.contains("ভালোবাসি") || pred1.contains("অনেক ভালোবাসি"));

    // Context: "খুব ভালো "
    List<String> pred2 = predictor.predictFromSentence("খুব ভালো ", 5);
    assertFalse(pred2.isEmpty());
    assertTrue("Must predict suitable continuation", pred2.contains("লাগল") || pred2.contains("হয়েছে") || pred2.contains("বাসি"));

    // Context: "আজকে কি "
    List<String> pred3 = predictor.predictFromSentence("আজকে কি ", 5);
    assertFalse(pred3.isEmpty());
    assertTrue("Must predict question verbs", pred3.contains("যাবে") || pred3.contains("হবে") || pred3.contains("ছুটি"));

    // Context: "কালকে দেখা "
    List<String> pred4 = predictor.predictFromSentence("কালকে দেখা ", 5);
    assertFalse(pred4.isEmpty());
    assertTrue("Must predict হবে", pred4.contains("হবে"));

    // Context: "অনেক ধন্যবাদ "
    List<String> pred5 = predictor.predictFromSentence("অনেক ধন্যবাদ ", 5);
    assertFalse(pred5.isEmpty());
    assertTrue("Must predict recipient", pred5.contains("আপনাকে") || pred5.contains("তোমাকে"));

    // Context: "শুভ জন্মদিন "
    List<String> pred6 = predictor.predictFromSentence("শুভ জন্মদিন ", 5);
    assertFalse(pred6.isEmpty());
    assertTrue("Must predict birthday wish continuation", pred6.contains("তোমাকে") || pred6.contains("অনেক অনেক শুভকামনা"));

    // Context: "ইনশা আল্লাহ "
    List<String> pred7 = predictor.predictFromSentence("ইনশা আল্লাহ ", 5);
    assertFalse(pred7.isEmpty());
    assertTrue("Must predict সব ঠিক হবে or দেখা হবে", pred7.contains("সব ঠিক হবে") || pred7.contains("দেখা হবে") || pred7.contains("ভালো হবে"));

    // Context: "আসসালামু আলাইকুম "
    List<String> pred8 = predictor.predictFromSentence("আসসালামু আলাইকুম ", 5);
    assertFalse(pred8.isEmpty());
    assertTrue("Must predict ওয়া রাহমাতুল্লাহ or ভাই", pred8.contains("ওয়া রাহমাতুল্লাহ") || pred8.contains("ভাই"));
  }

  @Test
  public void testPredictFromSentenceEnglish()
  {
    NextWordPredictor predictor = NextWordPredictor.instance(null);

    // Context: "how are you "
    List<String> pred1 = predictor.predictFromSentence("how are you ", 5);
    assertFalse(pred1.isEmpty());
    assertTrue("Must predict doing or today", pred1.contains("doing") || pred1.contains("today") || pred1.contains("feeling"));

    // Context: "what are you "
    List<String> pred2 = predictor.predictFromSentence("what are you ", 5);
    assertFalse(pred2.isEmpty());
    assertTrue("Must predict doing", pred2.contains("doing"));

    // Context: "thank you "
    List<String> pred3 = predictor.predictFromSentence("thank you ", 5);
    assertFalse(pred3.isEmpty());
    assertTrue("Must predict so much or very much", pred3.contains("so much") || pred3.contains("so") || pred3.contains("very much"));

    // Context: "let me know "
    List<String> pred4 = predictor.predictFromSentence("let me know ", 5);
    assertFalse(pred4.isEmpty());
    assertTrue("Must predict if you need anything or soon", pred4.contains("if you need anything") || pred4.contains("soon") || pred4.contains("what you think"));

    // Context: "as soon as "
    List<String> pred5 = predictor.predictFromSentence("as soon as ", 5);
    assertFalse(pred5.isEmpty());
    assertTrue("Must predict possible", pred5.contains("possible"));

    // Context: "see you "
    List<String> pred6 = predictor.predictFromSentence("see you ", 5);
    assertFalse(pred6.isEmpty());
    assertTrue("Must predict soon or tomorrow", pred6.contains("soon") || pred6.contains("tomorrow"));
  }

  @Test
  public void testSentenceBoundaryAndStarters()
  {
    NextWordPredictor predictor = NextWordPredictor.instance(null);

    // Multi-sentence: boundary with Dari
    List<String> preds = predictor.predictFromSentence("গতকাল কাজ শেষ করেছি। আজকে কি ", 5);
    assertFalse(preds.isEmpty());
    assertTrue("Should isolate latest clause and predict question verbs", preds.contains("যাবে") || preds.contains("হবে") || preds.contains("ছুটি"));

    // Multi-sentence: English period boundary
    List<String> predEng = predictor.predictFromSentence("All tasks finished. How are you ", 5);
    assertFalse(predEng.isEmpty());
    assertTrue("Should isolate last sentence and predict doing", predEng.contains("doing") || predEng.contains("today"));

    // Right after sentence delimiter
    List<String> startersBn = predictor.predictFromSentence("কাজ শেষ। ", 5);
    assertFalse("Should offer Bengali sentence starters", startersBn.isEmpty());
    assertTrue(startersBn.contains("আমি") || startersBn.contains("তুমি") || startersBn.contains("আপনি"));
  }

  @Test
  public void testMultiWordSentenceLearning()
  {
    NextWordPredictor predictor = NextWordPredictor.instance(null);
    predictor.learnSentence("আমরা কালকে স্কুলে যাব");

    // Predict from "আমরা কালকে " -> should include "স্কুলে"
    List<String> pred1 = predictor.predictFromSentence("আমরা কালকে ", 5);
    assertTrue("Learned 3rd word must appear", pred1.contains("স্কুলে"));

    // Predict from "কালকে স্কুলে " -> should include "যাব"
    List<String> pred2 = predictor.predictFromSentence("কালকে স্কুলে ", 5);
    assertTrue("Learned 4th word must appear", pred2.contains("যাব"));

    // Predict from 4-gram "আমরা কালকে স্কুলে " -> should include "যাব"
    List<String> pred3 = predictor.predictFromSentence("আমরা কালকে স্কুলে ", 5);
    assertTrue("Learned 4-gram completion must appear", pred3.contains("যাব"));
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
