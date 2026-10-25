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
