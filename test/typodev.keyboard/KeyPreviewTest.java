package typodev.keyboard;

import org.junit.Test;
import static typodev.keyboard.TestUtils.*;
import static org.junit.Assert.*;

public class KeyPreviewTest
{
  @Test
  public void testCharacterKeyResolution()
  {
    KeyValue keyQ = KeyValue.makeCharKey('q');
    assertEquals(KeyValue.Kind.Char, keyQ.getKind());
    assertEquals("q", keyQ.getString());

    // Shift modification through eval
    KeyValue keyQUpper = eval("shift", "q");
    assertNotNull(keyQUpper);
    assertEquals("Q", keyQUpper.getString());
  }

  @Test
  public void testSecondarySwipeKeyResolution()
  {
    KeyValue keyNum1 = KeyValue.makeCharKey('1');
    assertEquals(KeyValue.Kind.Char, keyNum1.getKind());
    assertEquals("1", keyNum1.getString());

    KeyValue keyAt = KeyValue.makeCharKey('@');
    assertEquals(KeyValue.Kind.Char, keyAt.getKind());
    assertEquals("@", keyAt.getString());
  }

  @Test
  public void testNonPrintableKeysDoNotPreview()
  {
    KeyValue shift = key("shift");
    assertNotNull(shift);
    assertNotEquals(KeyValue.Kind.Char, shift.getKind());
    assertNotEquals(KeyValue.Kind.String, shift.getKind());

    KeyValue ctrl = key("ctrl");
    assertNotNull(ctrl);
    assertNotEquals(KeyValue.Kind.Char, ctrl.getKind());
    assertNotEquals(KeyValue.Kind.String, ctrl.getKind());

    KeyValue del = key("backspace");
    assertNotNull(del);
    assertNotEquals(KeyValue.Kind.Char, del.getKind());
    assertNotEquals(KeyValue.Kind.String, del.getKind());
  }
}
