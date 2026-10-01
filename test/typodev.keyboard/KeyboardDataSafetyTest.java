package typodev.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class KeyboardDataSafetyTest
{
  private KeyboardData layout(float keyWidth)
  {
    KeyValue[] values = new KeyValue[9];
    values[0] = KeyValue.getKeyByName("a");
    KeyboardData.Key key = new KeyboardData.Key(values, null, 0, keyWidth, 0f,
        null, KeyboardData.Key.Role.Normal);
    KeyboardData.Row row = new KeyboardData.Row(new ArrayList<>(Arrays.asList(key)), 1f, 0f);
    return new KeyboardData(Arrays.asList(row), 1f, null, null, null, null,
        false, false, true);
  }

  @Test
  public void extraKeysDoNotChangeCachedSourceLayout()
  {
    KeyboardData original = layout(1f);
    original.getKeys(); // Populate the source cache before applying settings.
    KeyValue extra = KeyValue.getKeyByName("b");
    Map<KeyValue, KeyboardData.PreferredPos> keys = new HashMap<>();
    keys.put(extra, KeyboardData.PreferredPos.ANYWHERE);
    KeyboardData modified = original.addExtraKeys(keys.entrySet().iterator());
    assertNull(original.rows.get(0).keys.get(0).keys[1]);
    assertNull(original.findKeyWithValue(extra));
    assertEquals(extra, modified.rows.get(0).keys.get(0).keys[1]);
    KeyboardData withoutExtras = original.addExtraKeys(
        Collections.<KeyValue, KeyboardData.PreferredPos>emptyMap().entrySet().iterator());
    assertNull(withoutExtras.findKeyWithValue(extra));
  }

  @Test
  public void invalidExtraKeyCoordinatesAreIgnored()
  {
    KeyboardData original = layout(1f);
    for (KeyboardData.KeyPos pos : Arrays.asList(
        new KeyboardData.KeyPos(-2, 0, 0), new KeyboardData.KeyPos(0, -2, 0),
        new KeyboardData.KeyPos(0, 0, -2), new KeyboardData.KeyPos(0, 0, 9)))
      assertFalse(original.add_key_to_pos(original.rows, KeyValue.SHIFT, pos));
  }

  @Test
  public void resizingZeroWidthRowDoesNotProduceNan()
  {
    KeyboardData.Row row = layout(0f).rows.get(0).updateWidth(10f);
    assertEquals(0f, row.keysWidth, 0f);
    assertEquals(0f, row.keys.get(0).width, 0f);
  }
}
