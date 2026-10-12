package typodev.keyboard;

import org.junit.Test;
import typodev.keyboard.voice.VoiceLanguageResolver;
import typodev.keyboard.voice.VoicePunctuationHelper;
import static org.junit.Assert.*;

public class VoiceLanguageResolverTest {
  @Test public void arabicLayoutsRequestArabicRecognition() {
    assertEquals("ar-SA", VoiceLanguageResolver.resolve("arabic", "Arabic PC"));
    assertEquals("ar-SA", VoiceLanguageResolver.resolve("arabic", "Arabic Alt"));
    assertEquals("ar-SA", VoiceLanguageResolver.resolve(null, "العربية"));
  }

  @Test public void preservesBengaliAndEnglishRecognition() {
    assertEquals("bn-BD", VoiceLanguageResolver.resolve("bengali", "Bengali"));
    assertEquals("bn-BD", VoiceLanguageResolver.resolve(null, "বাংলা"));
    assertEquals("en-US", VoiceLanguageResolver.resolve("latin", "QWERTY"));
    assertEquals("en-US", VoiceLanguageResolver.resolve(null, null));
  }

  @Test public void arabicLettersDoNotForceOtherLanguagesIntoArabic() {
    assertEquals("fa-IR", VoiceLanguageResolver.resolve("persian", "Persian PC"));
    assertEquals("ur-PK", VoiceLanguageResolver.resolve("arabic", "Urdu"));
    assertEquals("ckb-IQ", VoiceLanguageResolver.resolve("arabic", "Kurdish (کوردی) QWERTY"));
    assertEquals("tly-AZ", VoiceLanguageResolver.resolve("arabic", "Talysh"));
  }

  @Test public void arabicQuestionMarkIsAlreadyASentenceEnding() {
    assertEquals("كيف حالك؟", VoicePunctuationHelper.applyInstantFormatting("كيف حالك؟", "ar-SA"));
  }
}
