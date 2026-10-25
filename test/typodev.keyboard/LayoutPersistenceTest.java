package typodev.keyboard;

import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class LayoutPersistenceTest
{
  private static final String LAYOUTS = "[\"latn_qwerty_us\",\"beng_national\"]";

  @Test public void delayedSettingsCopyKeepsSelectedBengaliLayout() throws Exception
  {
    MemoryPreferences settings = new MemoryPreferences();
    settings.edit().putString("layouts", LAYOUTS)
        .putInt("current_layout_portrait", 0)
        .putInt("current_layout_landscape", 0).apply();
    MemoryPreferences keyboard = new MemoryPreferences();
    DirectBootAwarePreferences.copy_shared_preferences(settings, keyboard);
    Config config = new Config();
    Field prefs = Config.class.getDeclaredField("_prefs");
    prefs.setAccessible(true);
    prefs.set(config, keyboard);
    config.set_current_layout(1);
    config.wide_screen = true;
    config.set_current_layout(1);

    // A settings activity stops after the IME has selected Bengali. Its stale
    // copy must still apply settings, without changing the typing language.
    settings.edit().putBoolean("suggestions", false).apply();
    DirectBootAwarePreferences.copy_shared_preferences(settings, keyboard);
    assertEquals(1, keyboard.getInt("current_layout_portrait", -1));
    assertEquals(1, keyboard.getInt("current_layout_landscape", -1));
    assertFalse(keyboard.getBoolean("suggestions", true));
    // Repeated copies must not restore the old English selection either.
    DirectBootAwarePreferences.copy_shared_preferences(settings, keyboard);
    assertEquals(1, keyboard.getInt("current_layout_portrait", -1));
  }

  @Test public void changedLayoutListReplacesOldIndices()
  {
    MemoryPreferences settings = new MemoryPreferences();
    MemoryPreferences keyboard = new MemoryPreferences();
    keyboard.edit().putString("layouts", LAYOUTS)
        .putInt("current_layout_portrait", 1)
        .putInt("current_layout_landscape", 1).apply();
    settings.edit().putString("layouts", "[\"latn_qwerty_us\"]")
        .putInt("current_layout_portrait", 0).apply();
    DirectBootAwarePreferences.copy_shared_preferences(settings, keyboard);
    assertEquals(0, keyboard.getInt("current_layout_portrait", -1));
    assertFalse(keyboard.contains("current_layout_landscape"));
  }

  @Test public void explicitSwitchBackToEnglishSurvivesClipboardUpdates()
  {
    MemoryPreferences settings = new MemoryPreferences();
    settings.edit().putString("layouts", LAYOUTS)
        .putInt("current_layout_portrait", 1).apply();
    MemoryPreferences keyboard = new MemoryPreferences();
    DirectBootAwarePreferences.copy_shared_preferences(settings, keyboard);
    keyboard.edit().putInt("current_layout_portrait", 0).apply();
    settings.edit().putStringSet("clipboard_dismissed_clips",
        new HashSet<String>()).apply();
    DirectBootAwarePreferences.copy_shared_preferences(settings, keyboard);
    assertEquals(0, keyboard.getInt("current_layout_portrait", -1));
    assertTrue(keyboard.contains("clipboard_dismissed_clips"));
  }

  @Test public void resetStillRemovesLayoutSelectionAndOtherSettings()
  {
    MemoryPreferences keyboard = new MemoryPreferences();
    keyboard.edit().putString("layouts", LAYOUTS)
        .putInt("current_layout_portrait", 1)
        .putBoolean("suggestions", false).apply();
    DirectBootAwarePreferences.copy_shared_preferences(new MemoryPreferences(), keyboard);
    assertTrue(keyboard.getAll().isEmpty());
  }

  @Test public void unchangedLayoutsKeepRuntimeSelectionButMirrorOtherRemovals()
  {
    MemoryPreferences settings = new MemoryPreferences();
    settings.edit().putString("layouts", LAYOUTS).apply();
    MemoryPreferences keyboard = new MemoryPreferences();
    keyboard.edit().putString("layouts", LAYOUTS)
        .putInt("current_layout_portrait", 1)
        .putString("custom_symbols", "old symbols").apply();
    DirectBootAwarePreferences.copy_shared_preferences(settings, keyboard);
    assertEquals(1, keyboard.getInt("current_layout_portrait", -1));
    assertFalse(keyboard.contains("custom_symbols"));
  }

  @Test public void focusModeToolIsRegisteredInAllTools()
  {
    boolean foundFocus = false;
    for (typodev.keyboard.suggestions.ToolbarToolsManager.ToolItem item :
        typodev.keyboard.suggestions.ToolbarToolsManager.ALL_TOOLS)
    {
      if ("focus".equals(item.id) && "Focus Mode".equals(item.label))
      {
        foundFocus = true;
        break;
      }
    }
    assertTrue("Focus Mode must be present in ALL_TOOLS", foundFocus);
    Config cfg = new Config();
    assertFalse("Focus Mode should be off by default", cfg.focus_mode);
    cfg.focus_mode = true;
    assertTrue(cfg.focus_mode);
  }

  @Test public void focusModeKeepsBengaliSubKeysAndRemovesExtraSymbols() throws Exception
  {
    String[] keptBengali = { "আ", "অ", "ভ", "খ", "থ", "ৎ", "ধ", "ং", "ঁ", "ঃ", "র্", "্য", "ক্ষ", "জ্ঞ", "ৌ", "ৈ", "ৄ", "ৗ" };
    for (String s : keptBengali)
    {
      assertTrue("Bengali character should be kept in Focus Mode: " + s,
          LayoutModifier.isFocusModeAllowedSubKey(KeyValue.makeStringKey(s)));
    }

    String[] removedExtra = { "/", "\\", ">", "<", "]", "[", "|", "}", "{", "^", "√", "π", "¶", "~", "•", "°", "`", "-", "_", "=", "+", "1", "১", "৳", "।", "?", "!" };
    for (String s : removedExtra)
    {
      assertFalse("Extra symbol should be removed in Focus Mode: " + s,
          LayoutModifier.isFocusModeAllowedSubKey(KeyValue.makeStringKey(s)));
    }
    assertFalse(LayoutModifier.isFocusModeAllowedSubKey(KeyValue.getKeyByName("esc")));
    assertFalse(LayoutModifier.isFocusModeAllowedSubKey(KeyValue.getKeyByName("tab")));

    KeyValue[] karKs = new KeyValue[9];
    karKs[0] = KeyValue.makeStringKey("া");
    karKs[1] = KeyValue.makeStringKey("ৄ");
    karKs[2] = KeyValue.makeStringKey("-");
    karKs[3] = KeyValue.makeStringKey("_");
    karKs[7] = KeyValue.makeStringKey("আ");
    karKs[8] = KeyValue.makeStringKey("অ");
    KeyboardData.Key karInput = new KeyboardData.Key(karKs, null, 0, 1f, 0f, null, KeyboardData.Key.Role.Normal);

    KeyValue[] baKs = new KeyValue[9];
    baKs[0] = KeyValue.makeStringKey("ব");
    baKs[1] = KeyValue.makeStringKey("√");
    baKs[2] = KeyValue.makeStringKey("?");
    baKs[3] = KeyValue.makeStringKey("।");
    baKs[4] = KeyValue.makeStringKey("π");
    baKs[7] = KeyValue.makeStringKey("ভ");
    KeyboardData.Key baInput = new KeyboardData.Key(baKs, null, 0, 1f, 0f, null, KeyboardData.Key.Role.Normal);

    KeyValue[] bsKs = new KeyValue[9];
    bsKs[0] = KeyValue.getKeyByName("backspace");
    bsKs[2] = KeyValue.getKeyByName("delete");
    KeyboardData.Key bsInput = new KeyboardData.Key(bsKs, null, 0, 1.5f, 0f, null, KeyboardData.Key.Role.Action);

    KeyValue[] ctrlKs = new KeyValue[9];
    ctrlKs[0] = KeyValue.getKeyByName("ctrl");
    ctrlKs[4] = KeyValue.getKeyByName("switch_numeric");
    KeyboardData.Key ctrlInput = new KeyboardData.Key(ctrlKs, null, 0, 1.7f, 0f, null, KeyboardData.Key.Role.Action);

    KeyValue[] spaceKs = new KeyValue[9];
    spaceKs[0] = KeyValue.getKeyByName("space");
    spaceKs[5] = KeyValue.getKeyByName("cursor_left");
    spaceKs[6] = KeyValue.getKeyByName("cursor_right");
    KeyboardData.Key spaceInput = new KeyboardData.Key(spaceKs, null, 0, 4.4f, 0f, null, KeyboardData.Key.Role.Space_bar);

    KeyValue[] enterKs = new KeyValue[9];
    enterKs[0] = KeyValue.getKeyByName("enter");
    enterKs[1] = KeyValue.getKeyByName("voice_typing");
    KeyboardData.Key enterInput = new KeyboardData.Key(enterKs, null, 0, 1.7f, 0f, null, KeyboardData.Key.Role.Action);

    KeyboardData.Row mainRow = new KeyboardData.Row(
        java.util.Arrays.asList(karInput, baInput, bsInput), 1f, 0f);
    KeyboardData.Row bottomRowInput = new KeyboardData.Row(
        java.util.Arrays.asList(ctrlInput, spaceInput, enterInput), 0.95f, 0f);
    KeyboardData parsed = new KeyboardData(
        java.util.Arrays.asList(mainRow, bottomRowInput),
        10f, null, "bengali", "bengali", "TestBijoy", false, false, true);
    KeyboardData focused = LayoutModifier.apply_focus_mode(parsed);

    KeyboardData.Key karKey = focused.rows.get(0).keys.get(0);
    assertEquals(KeyValue.makeStringKey("া"), karKey.getKeyValue(0));
    assertEquals(KeyValue.makeStringKey("ৄ"), karKey.getKeyValue(1));
    assertNull(karKey.getKeyValue(2));
    assertNull(karKey.getKeyValue(3));
    assertEquals(KeyValue.makeStringKey("আ"), karKey.getKeyValue(7));
    assertEquals(KeyValue.makeStringKey("অ"), karKey.getKeyValue(8));

    KeyboardData.Key baKey = focused.rows.get(0).keys.get(1);
    assertEquals(KeyValue.makeStringKey("ব"), baKey.getKeyValue(0));
    assertNull(baKey.getKeyValue(1));
    assertNull(baKey.getKeyValue(2));
    assertNull(baKey.getKeyValue(3));
    assertNull(baKey.getKeyValue(4));
    assertEquals(KeyValue.makeStringKey("ভ"), baKey.getKeyValue(7));

    KeyboardData.Key backspaceKey = focused.rows.get(0).keys.get(2);
    assertNotNull(backspaceKey.getKeyValue(2));

    KeyboardData.Row bottomRow = focused.rows.get(1);
    assertNotNull(bottomRow.keys.get(0).getKeyValue(4));
    assertNotNull(bottomRow.keys.get(1).getKeyValue(5));
    assertNotNull(bottomRow.keys.get(1).getKeyValue(6));
    assertNotNull(bottomRow.keys.get(2).getKeyValue(1));
  }

  private static final class MemoryPreferences implements SharedPreferences
  {
    private final Map<String, Object> values = new HashMap<>();
    public Map<String, ?> getAll() { return new HashMap<>(values); }
    public boolean contains(String key) { return values.containsKey(key); }
    public String getString(String key, String def) { return (String)values.getOrDefault(key, def); }
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key, Set<String> def) { return (Set<String>)values.getOrDefault(key, def); }
    public int getInt(String key, int def) { return (Integer)values.getOrDefault(key, def); }
    public long getLong(String key, long def) { return (Long)values.getOrDefault(key, def); }
    public float getFloat(String key, float def) { return (Float)values.getOrDefault(key, def); }
    public boolean getBoolean(String key, boolean def) { return (Boolean)values.getOrDefault(key, def); }
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
    public Editor edit()
    {
      return new Editor()
      {
        private final Map<String, Object> updates = new HashMap<>();
        private final Set<String> removals = new HashSet<>();
        private boolean cleared;
        public Editor putString(String key, String value) { updates.put(key, value); return this; }
        public Editor putStringSet(String key, Set<String> value) { updates.put(key, new HashSet<>(value)); return this; }
        public Editor putInt(String key, int value) { updates.put(key, value); return this; }
        public Editor putLong(String key, long value) { updates.put(key, value); return this; }
        public Editor putFloat(String key, float value) { updates.put(key, value); return this; }
        public Editor putBoolean(String key, boolean value) { updates.put(key, value); return this; }
        public Editor remove(String key) { removals.add(key); return this; }
        public Editor clear() { cleared = true; return this; }
        public boolean commit() { apply(); return true; }
        public void apply()
        {
          if (cleared) values.clear();
          for (String key : removals) values.remove(key);
          values.putAll(updates);
        }
      };
    }
  }
}
