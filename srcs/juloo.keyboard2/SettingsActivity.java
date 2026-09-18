package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;
import juloo.keyboard2.dict.DictionariesActivity;

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

    // Setup Action Bar with title and back navigation
    if (getActionBar() != null)
    {
      getActionBar().setDisplayHomeAsUpEnabled(true);
      getActionBar().setTitle(R.string.app_name_release);
      getActionBar().setSubtitle("Settings & Customization");
    }

    // Style the ListView with subtle dividers and padding
    try
    {
      ListView list = getListView();
      if (list != null)
      {
        list.setDivider(new ColorDrawable(0x18ffffff));
        list.setDividerHeight((int)(1 * getResources().getDisplayMetrics().density));
        list.setClipToPadding(false);
        int padH = (int)(6 * getResources().getDisplayMetrics().density);
        int padB = (int)(24 * getResources().getDisplayMetrics().density);
        list.setPadding(padH, 0, padH, padB);
      }
    }
    catch (Exception ignored) {}

    boolean foldableDevice = FoldStateTracker.isFoldableDevice(this);
    setPreferenceEnabled("margin_bottom_portrait_unfolded", foldableDevice);
    setPreferenceEnabled("margin_bottom_landscape_unfolded", foldableDevice);
    setPreferenceEnabled("horizontal_margin_portrait_unfolded", foldableDevice);
    setPreferenceEnabled("horizontal_margin_landscape_unfolded", foldableDevice);
    setPreferenceEnabled("keyboard_height_unfolded", foldableDevice);
    setPreferenceEnabled("keyboard_height_landscape_unfolded", foldableDevice);

    // Gemini API Key dynamic summary and edit listener
    final EditTextPreference apiKeyPref = (EditTextPreference) findPreference("gemini_api_key");
    if (apiKeyPref != null)
    {
      updateApiKeySummary(apiKeyPref);
      apiKeyPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
          String key = (newValue != null) ? newValue.toString().trim() : "";
          if (key.length() > 0)
          {
            preference.setSummary("Key configured: ●●●●●●" + (key.length() > 4 ? key.substring(key.length() - 4) : ""));
          }
          else
          {
            preference.setSummary("Enter your Google Gemini API key (tap to edit)");
          }
          return true;
        }
      });
    }

    // Get Free Gemini API Key link
    Preference getApiKeyPref = findPreference("pref_get_api_key");
    if (getApiKeyPref != null)
    {
      getApiKeyPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(Preference preference)
        {
          try
          {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"));
            startActivity(browserIntent);
          }
          catch (Exception e)
          {
            Toast.makeText(SettingsActivity.this, "Could not open browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          }
          return true;
        }
      });
    }

    // FrostKeys Dictionaries
    Preference frostkeysPref = findPreference("pref_dictionaries_frostkeys");
    if (frostkeysPref != null)
    {
      frostkeysPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(Preference preference)
        {
          try
          {
            startActivity(new Intent(SettingsActivity.this, DictionariesActivity.class));
          }
          catch (Exception e)
          {
            Toast.makeText(SettingsActivity.this, "Cannot open dictionaries: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          }
          return true;
        }
      });
    }

    // Test Keyboard shortcut
    Preference testPref = findPreference("pref_test_keyboard");
    if (testPref != null)
    {
      testPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(Preference preference)
        {
          try
          {
            startActivity(new Intent(SettingsActivity.this, LauncherActivity.class));
          }
          catch (Exception e)
          {
            Toast.makeText(SettingsActivity.this, "Cannot open test area: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          }
          return true;
        }
      });
    }

    // About app dialog
    Preference aboutPref = findPreference("pref_about_app");
    if (aboutPref != null)
    {
      aboutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(Preference preference)
        {
          new AlertDialog.Builder(SettingsActivity.this)
              .setTitle("Unexpected Keyboard")
              .setMessage("Version 2.1.0 (Mod with Gemini AI & FrostKeys)\n\n• Privacy-focused, lightweight virtual keyboard\n• Smart Gemini AI Assistant\n• 43,000+ Bengali & Multi-language wordlists\n\nOpen Source on GitHub:\nhttps://github.com/Julow/Unexpected-Keyboard")
              .setPositiveButton("OK", null)
              .setNeutralButton("GitHub", (dialog, which) -> {
                try {
                  startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Julow/Unexpected-Keyboard")));
                } catch (Exception ignored) {}
              })
              .show();
          return true;
        }
      });
    }
  }

  private void setPreferenceEnabled(String key, boolean enabled)
  {
    Preference pref = findPreference(key);
    if (pref != null)
    {
      pref.setEnabled(enabled);
    }
  }

  private void updateApiKeySummary(EditTextPreference pref)
  {
    String key = pref.getText();
    if (key != null && key.trim().length() > 0)
    {
      String trimmed = key.trim();
      pref.setSummary("Key configured: ●●●●●●" + (trimmed.length() > 4 ? trimmed.substring(trimmed.length() - 4) : ""));
    }
    else
    {
      pref.setSummary("Enter your Google Gemini API key (tap to edit)");
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if (item.getItemId() == android.R.id.home)
    {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  void fallbackEncrypted()
  {
    // Can't communicate with the user here.
    finish();
  }

  @Override
  protected void onStop()
  {
    DirectBootAwarePreferences
      .copy_preferences_to_protected_storage(this,
          getPreferenceManager().getSharedPreferences());
    super.onStop();
  }
}
