package typodev.keyboard;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import typodev.keyboard.symbol.CustomSymbolStore;
import static org.junit.Assert.*;

public class CustomSymbolStoreTest
{
  @Test
  public void testMappingSerialization()
  {
    CustomSymbolStore.Mapping m = new CustomSymbolStore.Mapping("b", 1, "৳");
    assertEquals("b", m.baseKey);
    assertEquals(1, m.pos);
    assertEquals("৳", m.symbol);

    String serialized = m.toSerializedString();
    assertNotNull(serialized);
    assertEquals("b|1|৳", serialized);

    CustomSymbolStore.Mapping restored = CustomSymbolStore.Mapping.fromSerializedString(serialized);
    assertNotNull(restored);
    assertEquals("b", restored.baseKey);
    assertEquals(1, restored.pos);
    assertEquals("৳", restored.symbol);
  }

  @Test
  public void testListSerializationAndDeserialization()
  {
    List<CustomSymbolStore.Mapping> list = new ArrayList<>();
    list.add(new CustomSymbolStore.Mapping("b", 1, "৳"));
    list.add(new CustomSymbolStore.Mapping("a", 2, "@"));
    list.add(new CustomSymbolStore.Mapping("s", 7, "$"));

    String data = CustomSymbolStore.serializeList(list);
    assertNotNull(data);
    assertTrue(data.contains("b|1|৳"));
    assertTrue(data.contains("a|2|@"));
    assertTrue(data.contains("s|7|$"));

    List<CustomSymbolStore.Mapping> restoredList = CustomSymbolStore.deserializeList(data);
    assertEquals(3, restoredList.size());
    assertEquals("b", restoredList.get(0).baseKey);
    assertEquals(1, restoredList.get(0).pos);
    assertEquals("৳", restoredList.get(0).symbol);

    assertEquals("a", restoredList.get(1).baseKey);
    assertEquals(2, restoredList.get(1).pos);
    assertEquals("@", restoredList.get(1).symbol);
  }

  @Test
  public void testPositionNamesAndIcons()
  {
    assertEquals("Top-Left", CustomSymbolStore.getPosName(1));
    assertEquals("↖", CustomSymbolStore.getPosIcon(1));

    assertEquals("Top-Right", CustomSymbolStore.getPosName(2));
    assertEquals("↗", CustomSymbolStore.getPosIcon(2));

    assertEquals("Bottom-Left", CustomSymbolStore.getPosName(3));
    assertEquals("↙", CustomSymbolStore.getPosIcon(3));

    assertEquals("Bottom-Right", CustomSymbolStore.getPosName(4));
    assertEquals("↘", CustomSymbolStore.getPosIcon(4));

    assertEquals("Top", CustomSymbolStore.getPosName(7));
    assertEquals("↑", CustomSymbolStore.getPosIcon(7));
  }

  @Test
  public void testApplyCustomSymbolsToKey()
  {
    KeyValue keyB = KeyValue.makeCharKey('b');
    KeyboardData.Key key = new KeyboardData.Key(
        new KeyValue[]{ keyB, null, null, null, null, null, null, null, null },
        null, 0, 1.0f, 0.0f, null, KeyboardData.Key.Role.Normal);

    // Initial check: top-left (index 1) is null
    assertNull(key.getKeyValue(1));

    // Custom mapping for 'b' at position 1 (top-left) with "৳"
    List<CustomSymbolStore.Mapping> mappings = new ArrayList<>();
    mappings.add(new CustomSymbolStore.Mapping("b", 1, "৳"));

    // Key with custom symbol applied directly via withKeyValue
    KeyValue taka = KeyValue.makeStringKey("৳");
    KeyboardData.Key updatedKey = key.withKeyValue(1, taka);

    assertNotNull(updatedKey.getKeyValue(1));
    assertEquals("৳", updatedKey.getKeyValue(1).getString());
    assertEquals("b", updatedKey.getKeyValue(0).getString());
  }

  @Test
  public void testSpecialActionsHelpers()
  {
    assertTrue(CustomSymbolStore.isSpecialAction("copy"));
    assertTrue(CustomSymbolStore.isSpecialAction("paste"));
    assertTrue(CustomSymbolStore.isSpecialAction("selectAll"));
    assertTrue(CustomSymbolStore.isSpecialAction("undo"));
    assertTrue(CustomSymbolStore.isSpecialAction("redo"));
    assertTrue(CustomSymbolStore.isSpecialAction("esc"));
    assertFalse(CustomSymbolStore.isSpecialAction("৳"));
    assertFalse(CustomSymbolStore.isSpecialAction("@"));

    assertEquals("Copy (⎘)", CustomSymbolStore.getFriendlyActionLabel("copy"));
    assertEquals("Paste (📋)", CustomSymbolStore.getFriendlyActionLabel("paste"));
    assertEquals("Select All (▦)", CustomSymbolStore.getFriendlyActionLabel("selectAll"));
    assertEquals("Undo (↶)", CustomSymbolStore.getFriendlyActionLabel("undo"));
    assertEquals("Redo (↷)", CustomSymbolStore.getFriendlyActionLabel("redo"));

    // Regular symbol returns itself
    assertEquals("৳", CustomSymbolStore.getFriendlyActionLabel("৳"));
  }

  @Test
  public void testSpecialActionsMappingSerialization()
  {
    CustomSymbolStore.Mapping m = new CustomSymbolStore.Mapping("q", 1, "selectAll");
    String serialized = m.toSerializedString();
    assertEquals("q|1|selectAll", serialized);

    CustomSymbolStore.Mapping restored = CustomSymbolStore.Mapping.fromSerializedString(serialized);
    assertNotNull(restored);
    assertEquals("q", restored.baseKey);
    assertEquals(1, restored.pos);
    assertEquals("selectAll", restored.symbol);
  }

  @Test
  public void testSpecialActionKeyValueResolution()
  {
    KeyValue specialCopy = KeyValue.getSpecialKeyByName("copy");
    assertNotNull(specialCopy);
    assertEquals(KeyValue.Kind.Editing, specialCopy.getKind());
    assertEquals(KeyValue.Editing.COPY, specialCopy.getEditing());

    KeyValue specialSelectAll = KeyValue.getSpecialKeyByName("selectAll");
    assertNotNull(specialSelectAll);
    assertEquals(KeyValue.Kind.Editing, specialSelectAll.getKind());
    assertEquals(KeyValue.Editing.SELECT_ALL, specialSelectAll.getEditing());

    assertEquals("Copy (⎘)", CustomSymbolStore.formatKeyValueDisplay(specialCopy));
    assertEquals("Select All (▦)", CustomSymbolStore.formatKeyValueDisplay(specialSelectAll));
  }

  @Test
  public void testNavigationArrowsAndModifiersSpecialActions()
  {
    assertTrue(CustomSymbolStore.isSpecialAction("up"));
    assertTrue(CustomSymbolStore.isSpecialAction("down"));
    assertTrue(CustomSymbolStore.isSpecialAction("left"));
    assertTrue(CustomSymbolStore.isSpecialAction("right"));
    assertTrue(CustomSymbolStore.isSpecialAction("ctrl"));
    assertTrue(CustomSymbolStore.isSpecialAction("fn"));
    assertTrue(CustomSymbolStore.isSpecialAction("shift"));

    assertEquals("Arrow Up (↑)", CustomSymbolStore.getFriendlyActionLabel("up"));
    assertEquals("Arrow Down (↓)", CustomSymbolStore.getFriendlyActionLabel("down"));
    assertEquals("Arrow Left (←)", CustomSymbolStore.getFriendlyActionLabel("left"));
    assertEquals("Arrow Right (→)", CustomSymbolStore.getFriendlyActionLabel("right"));
    assertEquals("Ctrl (Ctrl)", CustomSymbolStore.getFriendlyActionLabel("ctrl"));
    assertEquals("Fn (Fn)", CustomSymbolStore.getFriendlyActionLabel("fn"));

    KeyValue upKey = KeyValue.getSpecialKeyByName("up");
    assertNotNull(upKey);
    assertEquals("↑ (Up)", CustomSymbolStore.formatKeyValueDisplay(upKey));

    KeyValue leftKey = KeyValue.getSpecialKeyByName("left");
    assertNotNull(leftKey);
    assertEquals("← (Left)", CustomSymbolStore.formatKeyValueDisplay(leftKey));

    KeyValue ctrlKey = KeyValue.getSpecialKeyByName("ctrl");
    assertNotNull(ctrlKey);
    assertEquals(KeyValue.Kind.Modifier, ctrlKey.getKind());
    assertEquals(KeyValue.Modifier.CTRL, ctrlKey.getModifier());
    assertEquals("Ctrl", CustomSymbolStore.formatKeyValueDisplay(ctrlKey));

    KeyValue fnKey = KeyValue.getSpecialKeyByName("fn");
    assertNotNull(fnKey);
    assertEquals(KeyValue.Kind.Modifier, fnKey.getKind());
    assertEquals(KeyValue.Modifier.FN, fnKey.getModifier());
    assertEquals("Fn", CustomSymbolStore.formatKeyValueDisplay(fnKey));
  }

  @Test
  public void testApplyCustomSymbolsToModifierKey()
  {
    KeyValue ctrlVal = KeyValue.getSpecialKeyByName("ctrl");
    assertNotNull(ctrlVal);
    KeyboardData.Key ctrlKey = new KeyboardData.Key(
        new KeyValue[]{ ctrlVal, null, null, null, null, null, null, null, null },
        null, 0, 1.0f, 0.0f, null, KeyboardData.Key.Role.Action);

    List<KeyboardData.Key> keys = new ArrayList<>();
    keys.add(ctrlKey);
    KeyboardData.Row row = new KeyboardData.Row(keys, 1.0f, 0f);
    List<KeyboardData.Row> rows = new ArrayList<>();
    rows.add(row);
    KeyboardData kd = new KeyboardData(rows, 1.0f, null, null, null, "test", false, false, false);

    List<CustomSymbolStore.Mapping> mappings = new ArrayList<>();
    mappings.add(new CustomSymbolStore.Mapping("ctrl", 1, "esc"));

    KeyboardData modified = LayoutModifier.apply_custom_symbols(kd, mappings);
    assertNotNull(modified);
    KeyboardData.Key modKey = modified.rows.get(0).keys.get(0);
    assertNotNull(modKey.getKeyValue(1));
    assertEquals(KeyValue.Kind.Keyevent, modKey.getKeyValue(1).getKind());
    assertEquals(android.view.KeyEvent.KEYCODE_ESCAPE, modKey.getKeyValue(1).getKeyevent());
  }

  @Test
  public void testFormatKeyShortBadge()
  {
    KeyValue selectAll = KeyValue.getSpecialKeyByName("selectAll");
    assertEquals("▦", CustomSymbolStore.formatKeyShortBadge(selectAll));

    KeyValue copy = KeyValue.getSpecialKeyByName("copy");
    assertEquals("⎘", CustomSymbolStore.formatKeyShortBadge(copy));

    KeyValue paste = KeyValue.getSpecialKeyByName("paste");
    assertEquals("📋", CustomSymbolStore.formatKeyShortBadge(paste));

    KeyValue up = KeyValue.getSpecialKeyByName("up");
    assertEquals("↑", CustomSymbolStore.formatKeyShortBadge(up));

    KeyValue ctrl = KeyValue.getSpecialKeyByName("ctrl");
    assertEquals("Ctrl", CustomSymbolStore.formatKeyShortBadge(ctrl));

    KeyValue fn = KeyValue.getSpecialKeyByName("fn");
    assertEquals("Fn", CustomSymbolStore.formatKeyShortBadge(fn));

    KeyValue shift = KeyValue.getSpecialKeyByName("shift");
    assertEquals("⇧", CustomSymbolStore.formatKeyShortBadge(shift));
  }

  @Test
  public void testClearedSlotCustomSymbol()
  {
    assertTrue(CustomSymbolStore.isSpecialAction(CustomSymbolStore.SYMBOL_EMPTY));
    assertEquals("Empty (Cleared)", CustomSymbolStore.getFriendlyActionLabel(CustomSymbolStore.SYMBOL_EMPTY));

    KeyValue charB = KeyValue.makeCharKey('b');
    KeyValue specialSelectAll = KeyValue.getSpecialKeyByName("selectAll");

    // Key with selectAll initially at position 3 (Bottom-Left)
    KeyboardData.Key key = new KeyboardData.Key(
        new KeyValue[]{ charB, null, null, specialSelectAll, null, null, null, null, null },
        null, 0, 1.0f, 0.0f, null, KeyboardData.Key.Role.Normal);

    List<KeyboardData.Key> keys = new ArrayList<>();
    keys.add(key);
    KeyboardData.Row row = new KeyboardData.Row(keys, 1.0f, 0f);
    List<KeyboardData.Row> rows = new ArrayList<>();
    rows.add(row);
    KeyboardData kd = new KeyboardData(rows, 1.0f, null, null, null, "test", false, false, false);

    assertNotNull(kd.rows.get(0).keys.get(0).getKeyValue(3));

    // Clear position 3 using SYMBOL_EMPTY
    List<CustomSymbolStore.Mapping> mappings = new ArrayList<>();
    mappings.add(new CustomSymbolStore.Mapping("b", 3, CustomSymbolStore.SYMBOL_EMPTY));

    KeyboardData clearedKd = LayoutModifier.apply_custom_symbols(kd, mappings);
    assertNotNull(clearedKd);
    assertNull(clearedKd.rows.get(0).keys.get(0).getKeyValue(3));
  }

  @Test
  public void testReservedKeysSerialization()
  {
    List<String> keys = new ArrayList<>();
    keys.add("selectAll");
    keys.add("tab");
    keys.add("ctrl");
    keys.add("alt");

    String serialized = CustomSymbolStore.serializeReservedKeys(keys);
    assertNotNull(serialized);
    assertTrue(serialized.contains("selectAll"));
    assertTrue(serialized.contains("tab"));
    assertTrue(serialized.contains("ctrl"));
    assertTrue(serialized.contains("alt"));

    List<String> restored = CustomSymbolStore.deserializeReservedKeys(serialized);
    assertNotNull(restored);
    assertEquals(4, restored.size());
    assertEquals("selectAll", restored.get(0));
    assertEquals("tab", restored.get(1));
    assertEquals("ctrl", restored.get(2));
    assertEquals("alt", restored.get(3));

    // Test empty
    assertEquals("", CustomSymbolStore.serializeReservedKeys(null));
    assertEquals("", CustomSymbolStore.serializeReservedKeys(new ArrayList<String>()));
    assertTrue(CustomSymbolStore.deserializeReservedKeys(null).isEmpty());
    assertTrue(CustomSymbolStore.deserializeReservedKeys("").isEmpty());
    assertTrue(CustomSymbolStore.deserializeReservedKeys("  \n  ").isEmpty());
  }

  @Test
  public void testGetCanonicalCodeForKeyValue()
  {
    KeyValue selectAll = KeyValue.getSpecialKeyByName("selectAll");
    assertEquals("selectAll", CustomSymbolStore.getCanonicalCodeForKeyValue(selectAll));

    KeyValue copy = KeyValue.getSpecialKeyByName("copy");
    assertEquals("copy", CustomSymbolStore.getCanonicalCodeForKeyValue(copy));

    KeyValue tab = KeyValue.getSpecialKeyByName("tab");
    assertEquals("tab", CustomSymbolStore.getCanonicalCodeForKeyValue(tab));

    KeyValue esc = KeyValue.getSpecialKeyByName("esc");
    assertEquals("esc", CustomSymbolStore.getCanonicalCodeForKeyValue(esc));

    KeyValue ctrl = KeyValue.getSpecialKeyByName("ctrl");
    assertEquals("ctrl", CustomSymbolStore.getCanonicalCodeForKeyValue(ctrl));

    KeyValue fn = KeyValue.getSpecialKeyByName("fn");
    assertEquals("fn", CustomSymbolStore.getCanonicalCodeForKeyValue(fn));

    KeyValue alt = KeyValue.getSpecialKeyByName("alt");
    assertEquals("alt", CustomSymbolStore.getCanonicalCodeForKeyValue(alt));

    KeyValue meta = KeyValue.getSpecialKeyByName("meta");
    assertEquals("meta", CustomSymbolStore.getCanonicalCodeForKeyValue(meta));

    KeyValue superscript = KeyValue.getSpecialKeyByName("superscript");
    assertEquals("superscript", CustomSymbolStore.getCanonicalCodeForKeyValue(superscript));

    KeyValue subscript = KeyValue.getSpecialKeyByName("subscript");
    assertEquals("subscript", CustomSymbolStore.getCanonicalCodeForKeyValue(subscript));

    KeyValue shareText = KeyValue.getSpecialKeyByName("shareText");
    assertEquals("shareText", CustomSymbolStore.getCanonicalCodeForKeyValue(shareText));

    KeyValue menu = KeyValue.getSpecialKeyByName("menu");
    assertEquals("menu", CustomSymbolStore.getCanonicalCodeForKeyValue(menu));

    KeyValue zwj = KeyValue.getSpecialKeyByName("zwj");
    assertEquals("zwj", CustomSymbolStore.getCanonicalCodeForKeyValue(zwj));

    KeyValue zwnj = KeyValue.getSpecialKeyByName("zwnj");
    assertEquals("zwnj", CustomSymbolStore.getCanonicalCodeForKeyValue(zwnj));

    KeyValue nbsp = KeyValue.getSpecialKeyByName("nbsp");
    assertEquals("nbsp", CustomSymbolStore.getCanonicalCodeForKeyValue(nbsp));

    KeyValue nnbsp = KeyValue.getSpecialKeyByName("nnbsp");
    assertEquals("nnbsp", CustomSymbolStore.getCanonicalCodeForKeyValue(nnbsp));
  }

  @Test
  public void testExtraKeysBadgesAndDisplay()
  {
    KeyValue alt = KeyValue.getSpecialKeyByName("alt");
    assertEquals("Alt", CustomSymbolStore.formatKeyShortBadge(alt));
    assertEquals("Alt", CustomSymbolStore.formatKeyValueDisplay(alt));

    KeyValue meta = KeyValue.getSpecialKeyByName("meta");
    assertEquals("Meta", CustomSymbolStore.formatKeyShortBadge(meta));
    assertEquals("Meta", CustomSymbolStore.formatKeyValueDisplay(meta));

    KeyValue superscript = KeyValue.getSpecialKeyByName("superscript");
    assertEquals("x²", CustomSymbolStore.formatKeyShortBadge(superscript));
    assertEquals("Superscript (x²)", CustomSymbolStore.formatKeyValueDisplay(superscript));

    KeyValue subscript = KeyValue.getSpecialKeyByName("subscript");
    assertEquals("x₂", CustomSymbolStore.formatKeyShortBadge(subscript));
    assertEquals("Subscript (x₂)", CustomSymbolStore.formatKeyValueDisplay(subscript));

    KeyValue zwj = KeyValue.getSpecialKeyByName("zwj");
    assertEquals("ZWJ", CustomSymbolStore.formatKeyShortBadge(zwj));
    assertEquals("Zero-Width Joiner (ZWJ)", CustomSymbolStore.formatKeyValueDisplay(zwj));

    KeyValue zwnj = KeyValue.getSpecialKeyByName("zwnj");
    assertEquals("ZWNJ", CustomSymbolStore.formatKeyShortBadge(zwnj));
    assertEquals("Zero-Width Non-Joiner (ZWNJ)", CustomSymbolStore.formatKeyValueDisplay(zwnj));

    KeyValue nbsp = KeyValue.getSpecialKeyByName("nbsp");
    assertEquals("NBSP", CustomSymbolStore.formatKeyShortBadge(nbsp));
    assertEquals("Non-Breaking Space (NBSP)", CustomSymbolStore.formatKeyValueDisplay(nbsp));

    assertTrue(CustomSymbolStore.isSpecialAction("alt"));
    assertTrue(CustomSymbolStore.isSpecialAction("meta"));
    assertTrue(CustomSymbolStore.isSpecialAction("shareText"));
    assertTrue(CustomSymbolStore.isSpecialAction("zwj"));
    assertTrue(CustomSymbolStore.isSpecialAction("accent_aigu"));
  }

  @Test
  public void testSwapAndMoveCornerSymbols()
  {
    KeyValue charNga = KeyValue.makeStringKey("ঙ");
    KeyValue charAnusvara = KeyValue.makeStringKey("ং"); // pos 2 (Top-Right)
    KeyValue specialEsc = KeyValue.getSpecialKeyByName("esc"); // pos 3 (Bottom-Left)

    KeyboardData.Key key = new KeyboardData.Key(
        new KeyValue[]{ charNga, null, charAnusvara, specialEsc, null, null, null, null, null },
        null, 0, 1.0f, 0.0f, null, KeyboardData.Key.Role.Normal);

    List<KeyboardData.Key> keys = new ArrayList<>();
    keys.add(key);
    KeyboardData.Row row = new KeyboardData.Row(keys, 1.0f, 0f);
    List<KeyboardData.Row> rows = new ArrayList<>();
    rows.add(row);
    KeyboardData kd = new KeyboardData(rows, 1.0f, null, null, null, "test", false, false, false);

    // Initial check: pos 2 is "ং", pos 3 is "esc", pos 5 (Left) is null
    assertEquals("ং", kd.rows.get(0).keys.get(0).getKeyValue(2).getString());
    assertEquals("Esc", kd.rows.get(0).keys.get(0).getKeyValue(3).getString());
    assertNull(kd.rows.get(0).keys.get(0).getKeyValue(5));

    // Move "ং" from pos 2 (Top-Right) to pos 5 (Left)
    // and clear pos 2 (using SYMBOL_EMPTY)
    List<CustomSymbolStore.Mapping> moveMappings = new ArrayList<>();
    moveMappings.add(new CustomSymbolStore.Mapping("ঙ", 5, "ং"));
    moveMappings.add(new CustomSymbolStore.Mapping("ঙ", 2, CustomSymbolStore.SYMBOL_EMPTY));

    KeyboardData movedKd = LayoutModifier.apply_custom_symbols(kd, moveMappings);
    assertNotNull(movedKd);
    assertNull(movedKd.rows.get(0).keys.get(0).getKeyValue(2));
    assertNotNull(movedKd.rows.get(0).keys.get(0).getKeyValue(5));
    assertEquals("ং", movedKd.rows.get(0).keys.get(0).getKeyValue(5).getString());

    // Now test swap between pos 2 and pos 3:
    // pos 2 gets "esc", pos 3 gets "ং"
    List<CustomSymbolStore.Mapping> swapMappings = new ArrayList<>();
    swapMappings.add(new CustomSymbolStore.Mapping("ঙ", 2, "esc"));
    swapMappings.add(new CustomSymbolStore.Mapping("ঙ", 3, "ং"));

    KeyboardData swappedKd = LayoutModifier.apply_custom_symbols(kd, swapMappings);
    assertNotNull(swappedKd);
    assertEquals("Esc", swappedKd.rows.get(0).keys.get(0).getKeyValue(2).getString());
    assertEquals("ং", swappedKd.rows.get(0).keys.get(0).getKeyValue(3).getString());
  }
}

