package juloo.keyboard2;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity
{
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    // The preferences can't be read when in direct-boot mode. Avoid crashing
    // and don't allow changing the settings.
    // Run the config migration on this prefs as it might be different from the
    // one used by the keyboard, which have been migrated.
    try
    {
      Config.migrate(getPreferenceManager().getSharedPreferences());
    }
    catch (Exception _e) { fallbackEncrypted(); return; }
    addPreferencesFromResource(R.xml.settings);

    boolean foldableDevice = FoldStateTracker.isFoldableDevice(this);
    findPreference("margin_bottom_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("margin_bottom_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_portrait_unfolded").setEnabled(foldableDevice);
    findPreference("horizontal_margin_landscape_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_unfolded").setEnabled(foldableDevice);
    findPreference("keyboard_height_landscape_unfolded").setEnabled(foldableDevice);

    android.preference.Preference frostkeysPref = findPreference("pref_dictionaries_frostkeys");
    if (frostkeysPref != null)
    {
      frostkeysPref.setOnPreferenceClickListener(new android.preference.Preference.OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(android.preference.Preference preference)
        {
          try
          {
            startActivity(new android.content.Intent(SettingsActivity.this, juloo.keyboard2.dict.DictionariesActivity.class));
          }
          catch (Exception e)
          {
            android.widget.Toast.makeText(SettingsActivity.this, "Cannot open dictionaries: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
          }
          return true;
        }
      });
    }
  }

  void fallbackEncrypted()
  {
    // Can't communicate with the user here.
    finish();
  }

  protected void onStop()
  {
    DirectBootAwarePreferences
      .copy_preferences_to_protected_storage(this,
          getPreferenceManager().getSharedPreferences());
    super.onStop();
  }
}
