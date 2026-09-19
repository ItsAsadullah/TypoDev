package juloo.keyboard2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
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
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
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

    // Style the ListView with subtle dividers and uniform padding
    try
    {
      ListView list = getListView();
      if (list != null)
      {
        enforceConsistentPadding(list);
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

    // Active AI Provider dynamic summary
    final android.preference.ListPreference providerPref = (android.preference.ListPreference) findPreference("pref_ai_provider");
    if (providerPref != null)
    {
      if (providerPref.getEntry() != null)
        providerPref.setSummary(providerPref.getEntry());
      providerPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
          int idx = providerPref.findIndexOfValue(newValue != null ? newValue.toString() : "");
          if (idx >= 0 && idx < providerPref.getEntries().length)
            preference.setSummary(providerPref.getEntries()[idx]);
          return true;
        }
      });
    }

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

    // Gemini Working Model dynamic summary
    final android.preference.ListPreference geminiModelPref = (android.preference.ListPreference) findPreference("gemini_working_model");
    if (geminiModelPref != null)
    {
      if (geminiModelPref.getEntry() != null)
        geminiModelPref.setSummary(geminiModelPref.getEntry());
      geminiModelPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
          int idx = geminiModelPref.findIndexOfValue(newValue != null ? newValue.toString() : "");
          if (idx >= 0 && idx < geminiModelPref.getEntries().length)
            preference.setSummary(geminiModelPref.getEntries()[idx]);
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

    // OpenAI Base URL dynamic summary
    final EditTextPreference openAiUrlPref = (EditTextPreference) findPreference("openai_base_url");
    if (openAiUrlPref != null)
    {
      String url = openAiUrlPref.getText();
      if (url != null && !url.trim().isEmpty())
        openAiUrlPref.setSummary(url.trim());
      openAiUrlPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
          String u = (newValue != null) ? newValue.toString().trim() : "";
          preference.setSummary(u.isEmpty() ? "https://api.openai.com/v1" : u);
          return true;
        }
      });
    }

    // OpenAI API Key dynamic summary
    final EditTextPreference openAiKeyPref = (EditTextPreference) findPreference("openai_api_key");
    if (openAiKeyPref != null)
    {
      updateOpenAiKeySummary(openAiKeyPref);
      openAiKeyPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
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
            preference.setSummary("Enter your API key (sk-...)");
          }
          return true;
        }
      });
    }

    // OpenAI Model dynamic summary
    final android.preference.ListPreference openAiModelPref = (android.preference.ListPreference) findPreference("openai_model");
    if (openAiModelPref != null)
    {
      if (openAiModelPref.getEntry() != null)
        openAiModelPref.setSummary(openAiModelPref.getEntry());
      openAiModelPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
          int idx = openAiModelPref.findIndexOfValue(newValue != null ? newValue.toString() : "");
          if (idx >= 0 && idx < openAiModelPref.getEntries().length)
            preference.setSummary(openAiModelPref.getEntries()[idx]);
          return true;
        }
      });
    }

    // Quick AI Popup Settings preference
    Preference openAiDialogPref = findPreference("pref_open_ai_dialog");
    if (openAiDialogPref != null)
    {
      openAiDialogPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(Preference preference)
        {
          juloo.keyboard2.ai.AiSettingsDialog.show(SettingsActivity.this, null, new juloo.keyboard2.ai.AiSettingsDialog.OnSettingsSavedListener()
          {
            @Override
            public void onSaved()
            {
              // Refresh summaries
              if (apiKeyPref != null) updateApiKeySummary(apiKeyPref);
              if (openAiKeyPref != null) updateOpenAiKeySummary(openAiKeyPref);
              if (providerPref != null && providerPref.getEntry() != null) providerPref.setSummary(providerPref.getEntry());
              if (geminiModelPref != null && geminiModelPref.getEntry() != null) geminiModelPref.setSummary(geminiModelPref.getEntry());
              if (openAiModelPref != null && openAiModelPref.getEntry() != null) openAiModelPref.setSummary(openAiModelPref.getEntry());
            }
          });
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

    // My Snippets & Word Manager
    Preference snippetPref = findPreference("pref_snippet_manager");
    if (snippetPref != null)
    {
      snippetPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(Preference preference)
        {
          try
          {
            startActivity(new Intent(SettingsActivity.this,
                juloo.keyboard2.snippet.SnippetManagerActivity.class));
          }
          catch (Exception e)
          {
            Toast.makeText(SettingsActivity.this, "Cannot open snippet manager: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          }
          return true;
        }
      });
    }

    // Adaptive Learning Toggle
    android.preference.CheckBoxPreference learningPref =
        (android.preference.CheckBoxPreference) findPreference("pref_learning_enabled");
    if (learningPref != null)
    {
      learningPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
          boolean enabled = Boolean.TRUE.equals(newValue);
          juloo.keyboard2.suggestions.UserVocabularyStore.instance(SettingsActivity.this).setLearningEnabled(enabled);
          return true;
        }
      });
    }

    // Clear Learned Words & History
    Preference clearLearnedPref = findPreference("pref_clear_learned_data");
    if (clearLearnedPref != null)
    {
      clearLearnedPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(Preference preference)
        {
          new AlertDialog.Builder(SettingsActivity.this)
              .setTitle("Clear Learned Data")
              .setMessage("Are you sure you want to reset all personalized vocabulary, learned words, and prediction history?")
              .setPositiveButton("Clear", new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                  juloo.keyboard2.suggestions.UserVocabularyStore.instance(SettingsActivity.this).clearLearnedData();
                  juloo.keyboard2.suggestions.NextWordPredictor.instance(SettingsActivity.this).clearLearnedData();
                  Toast.makeText(SettingsActivity.this, "Learned vocabulary and predictions cleared", Toast.LENGTH_SHORT).show();
                }
              })
              .setNegativeButton(android.R.string.cancel, null)
              .show();
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
              .setTitle("TypoDev")
              .setMessage("type faster, write smarter\n\nVersion 2.1.0 (with Gemini AI & Bengali Suggestions)\n\n• Privacy-focused, lightweight virtual keyboard\n• Smart Gemini AI Assistant\n• 43,000+ Bengali & Multi-language wordlists\n\nOpen Source on GitHub:\nhttps://github.com/ItsAsadullah/TypoDev")
              .setPositiveButton("OK", null)
              .setNeutralButton("GitHub", (dialog, which) -> {
                try {
                  startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ItsAsadullah/TypoDev")));
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

  private void updateOpenAiKeySummary(EditTextPreference pref)
  {
    String key = pref.getText();
    if (key != null && key.trim().length() > 0)
    {
      String trimmed = key.trim();
      pref.setSummary("Key configured: ●●●●●●" + (trimmed.length() > 4 ? trimmed.substring(trimmed.length() - 4) : ""));
    }
    else
    {
      pref.setSummary("Enter your API key (sk-...)");
    }
  }

  private void enforceConsistentPadding(final ListView list)
  {
    if (list == null) return;
    list.setClipToPadding(false);
    int padB = (int)(24 * getResources().getDisplayMetrics().density);
    list.setPadding(0, 0, 0, padB);
    list.setDivider(new ColorDrawable(0x18ffffff));
    list.setDividerHeight((int)(1 * getResources().getDisplayMetrics().density));

    final int targetPadding = (int)(16 * getResources().getDisplayMetrics().density);

    list.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
    {
      @Override
      public boolean onPreDraw()
      {
        int count = list.getChildCount();
        for (int i = 0; i < count; i++)
        {
          View child = list.getChildAt(i);
          if (child == null) continue;

          int padL = child.getPaddingLeft();
          int padR = child.getPaddingRight();
          if (padL < targetPadding || padR < targetPadding)
          {
            child.setPadding(
                Math.max(padL, targetPadding),
                child.getPaddingTop(),
                Math.max(padR, targetPadding),
                child.getPaddingBottom()
            );
          }
        }
        return true;
      }
    });
  }

  @Override
  public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
  {
    boolean result = super.onPreferenceTreeClick(preferenceScreen, preference);
    if (preference instanceof PreferenceScreen)
    {
      final PreferenceScreen subScreen = (PreferenceScreen) preference;
      final Dialog dialog = subScreen.getDialog();
      if (dialog != null)
      {
        View list = dialog.findViewById(android.R.id.list);
        if (list instanceof ListView)
        {
          enforceConsistentPadding((ListView) list);
        }
        else
        {
          dialog.setOnShowListener(new DialogInterface.OnShowListener()
          {
            @Override
            public void onShow(DialogInterface d)
            {
              View l = dialog.findViewById(android.R.id.list);
              if (l instanceof ListView)
              {
                enforceConsistentPadding((ListView) l);
              }
            }
          });
        }
      }
    }
    return result;
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
