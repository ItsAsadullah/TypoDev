package typodev.keyboard;

import android.view.KeyEvent;
import org.junit.Test;
import static org.junit.Assert.*;

public class SoundFeedbackManagerTest
{
  @Test
  public void testKeySoundTypeMapping()
  {
    // Spacebar
    KeyValue space = KeyValue.getKeyByName("space");
    assertNotNull(space);
    assertEquals(SoundFeedbackManager.KeySoundType.SPACEBAR, SoundFeedbackManager.getKeySoundType(space));

    // Backspace / Delete
    KeyValue backspace = KeyValue.getKeyByName("backspace");
    assertNotNull(backspace);
    assertEquals(SoundFeedbackManager.KeySoundType.DELETE, SoundFeedbackManager.getKeySoundType(backspace));

    KeyValue del = KeyValue.getKeyByName("delete");
    assertNotNull(del);
    assertEquals(SoundFeedbackManager.KeySoundType.DELETE, SoundFeedbackManager.getKeySoundType(del));

    // Return / Enter
    KeyValue enter = KeyValue.getKeyByName("enter");
    assertNotNull(enter);
    assertEquals(SoundFeedbackManager.KeySoundType.RETURN, SoundFeedbackManager.getKeySoundType(enter));

    KeyValue keyeventEnter = KeyValue.keyeventKey("enter", KeyEvent.KEYCODE_ENTER, 0);
    assertEquals(SoundFeedbackManager.KeySoundType.RETURN, SoundFeedbackManager.getKeySoundType(keyeventEnter));

    // Standard character keys
    KeyValue charA = KeyValue.makeCharKey('a');
    assertEquals(SoundFeedbackManager.KeySoundType.STANDARD, SoundFeedbackManager.getKeySoundType(charA));

    KeyValue charBengali = KeyValue.makeCharKey('ক');
    assertEquals(SoundFeedbackManager.KeySoundType.STANDARD, SoundFeedbackManager.getKeySoundType(charBengali));

    KeyValue nullKey = null;
    assertEquals(SoundFeedbackManager.KeySoundType.STANDARD, SoundFeedbackManager.getKeySoundType(nullKey));
  }

  @Test
  public void testConfigSoundDefaults()
  {
    // Ensure sound types exist
    assertNotNull(SoundFeedbackManager.KeySoundType.valueOf("STANDARD"));
    assertNotNull(SoundFeedbackManager.KeySoundType.valueOf("SPACEBAR"));
    assertNotNull(SoundFeedbackManager.KeySoundType.valueOf("DELETE"));
    assertNotNull(SoundFeedbackManager.KeySoundType.valueOf("RETURN"));
  }
}
