package juloo.keyboard2;

import juloo.keyboard2.voice.OfflineVoiceTypingService;
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
}
