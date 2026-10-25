package typodev.keyboard;

import typodev.keyboard.voice.OfflineVoiceTypingService;
import org.junit.Test;
import static org.junit.Assert.*;

public class VoiceTypingTest
{
  @Test
  public void testInitialListeningState()
  {
    assertFalse("Voice typing should not be listening initially",
        OfflineVoiceTypingService.isListening());
  }

  @Test
  public void testSafeStopListeningWhenNotStarted()
  {
    try
    {
      OfflineVoiceTypingService.stopListening();
      assertFalse(OfflineVoiceTypingService.isListening());
    }
    catch (Exception e)
    {
      fail("stopListening should not throw exception when idle: " + e.getMessage());
    }
  }

  @Test
  public void testLanguageCodes()
  {
    String bn = "bn-BD";
    String en = "en-US";
    assertTrue(bn.startsWith("bn"));
    assertTrue(en.startsWith("en"));
  }

  @Test
  public void testBengaliVoiceCommands()
  {
    String input = "আমি ভালো আছি দাঁড়ি আপনি কেমন আছেন প্রশ্নবোধক";
    String result = typodev.keyboard.voice.VoicePunctuationHelper.applyVoiceCommands(input, "bn-BD");
    assertEquals("আমি ভালো আছি। আপনি কেমন আছেন?", result);
  }

  @Test
  public void testEnglishVoiceCommands()
  {
    String input = "hello world period how are you question mark";
    String result = typodev.keyboard.voice.VoicePunctuationHelper.applyVoiceCommands(input, "en-US");
    assertEquals("hello world. how are you?", result);
  }

  @Test
  public void testInstantFormattingBengali()
  {
    String input = "আমার সোনার বাংলা   আমি তোমায় ভালোবাসি";
    String formatted = typodev.keyboard.voice.VoicePunctuationHelper.applyInstantFormatting(input, "bn-BD");
    assertEquals("আমার সোনার বাংলা আমি তোমায় ভালোবাসি।", formatted);
  }

  @Test
  public void testInstantFormattingPreservesExistingPunctuation()
  {
    String input = "সব ঠিক আছে?";
    String formatted = typodev.keyboard.voice.VoicePunctuationHelper.applyInstantFormatting(input, "bn-BD");
    assertEquals("সব ঠিক আছে?", formatted);
  }

  @Test
  public void testExactEnglishWordsInBengaliModePreserved()
  {
    String input = "আমি গুড আছি";
    String result = typodev.keyboard.voice.VoicePunctuationHelper.applyVoiceCommands(input, "bn-BD");
    assertEquals("আমি গুড আছি", result);

    String inputGood = "very good";
    String resultGood = typodev.keyboard.voice.VoicePunctuationHelper.applyVoiceCommands(inputGood, "bn-BD");
    assertEquals("very good", resultGood);
  }

  @Test
  public void testSafetyWordPreservationLogic()
  {
    // Verifies that punctuation/spacing stripping comparison correctly distinguishes
    // harmless punctuation additions from word substitutions/translations.
    String raw = "আমি গুড আছি";
    String punctuated = "আমি গুড আছি।";
    String translated = "আমি ভালো আছি।";

    String strippedRaw = raw.replaceAll("[\\p{Punct}।\\s]+", "");
    String strippedPunct = punctuated.replaceAll("[\\p{Punct}।\\s]+", "");
    String strippedTrans = translated.replaceAll("[\\p{Punct}।\\s]+", "");

    assertTrue(strippedRaw.equalsIgnoreCase(strippedPunct));
    assertFalse(strippedRaw.equalsIgnoreCase(strippedTrans));
  }

  @Test
  public void testCancelListeningCleansUpState()
  {
    OfflineVoiceTypingService.cancelListening();
    assertFalse(OfflineVoiceTypingService.isListening());
  }

  @Test public void preservesDecimalsTimesAndParagraphBreaks() {
    assertEquals("দাম ৩.১৪ টাকা, সময় ১০:৩০।\nকাল আসব।",
        typodev.keyboard.voice.VoicePunctuationHelper.applyInstantFormatting(
            "দাম ৩.১৪ টাকা,সময় ১০:৩০।\nকাল আসব", "bn-BD"));
  }

  @Test public void punctuationCanChangeThroughoutWithoutChangingWords() {
    assertTrue(typodev.keyboard.voice.VoicePunctuationHelper.preservesSpokenWords(
        "তুমি কেমন আছ আমি গুড আছি চাল ডাল আর তেল কিনব।",
        "তুমি কেমন আছ? আমি গুড আছি। চাল, ডাল আর তেল কিনব।"));
    assertTrue(typodev.keyboard.voice.VoicePunctuationHelper.preservesSpokenWords(
        "how are you I am fine", "How are you? I am fine."));
  }

  @Test public void rejectsWordMergingTranslationDeletionAndNumberChanges() {
    assertFalse(typodev.keyboard.voice.VoicePunctuationHelper.preservesSpokenWords("do not", "donot."));
    assertFalse(typodev.keyboard.voice.VoicePunctuationHelper.preservesSpokenWords("আমি গুড আছি", "আমি ভালো আছি।"));
    assertFalse(typodev.keyboard.voice.VoicePunctuationHelper.preservesSpokenWords("আমি কাল যাব না", "আমি কাল যাব।"));
    assertFalse(typodev.keyboard.voice.VoicePunctuationHelper.preservesSpokenWords("দাম 3.14", "দাম 314।"));
    assertFalse(typodev.keyboard.voice.VoicePunctuationHelper.preservesSpokenWords("সময় 10:30", "সময় 10 30।"));
  }

  @Test public void acceptsEquivalentBengaliUnicodeWithoutTranslating() {
    assertTrue(typodev.keyboard.voice.VoicePunctuationHelper.preservesSpokenWords("দ্বিতীয় দিন", "দ্বিতীয় দিন।"));
  }

  @Test public void autoDetectsBengaliAndEnglishQuestionsInInstantFormatting() {
    assertEquals("তুমি কেমন আছো?",
        typodev.keyboard.voice.VoicePunctuationHelper.applyInstantFormatting("তুমি কেমন আছো", "bn-BD"));
    assertEquals("আপনি কি যাবেন?",
        typodev.keyboard.voice.VoicePunctuationHelper.applyInstantFormatting("আপনি কি যাবেন", "bn-BD"));
    assertEquals("how are you doing today?",
        typodev.keyboard.voice.VoicePunctuationHelper.applyInstantFormatting("how are you doing today", "en-US"));
  }

  @Test public void rejectsCorruptedMixedScriptAndHexSuffixTokensFromSuggestions() {
    assertTrue(typodev.keyboard.suggestions.UserVocabularyStore.isCorruptedMixedToken("আমাদের4650059ff"));
    assertTrue(typodev.keyboard.suggestions.UserVocabularyStore.isCorruptedMixedToken("ফিচার4650059ff"));
    assertFalse(typodev.keyboard.suggestions.UserVocabularyStore.isCorruptedMixedToken("আমাদের"));
    assertFalse(typodev.keyboard.suggestions.UserVocabularyStore.isCorruptedMixedToken("ফিচার"));
    assertFalse(typodev.keyboard.suggestions.BanglishEngine.isBengaliScript("আমাদের4650059ff"));
    assertTrue(typodev.keyboard.suggestions.BanglishEngine.isBengaliScript("আমাদের"));
  }
}

