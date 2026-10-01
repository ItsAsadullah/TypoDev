package typodev.keyboard;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import typodev.keyboard.dict.DictionariesActivity;

public class SettingsCategoryActivity extends PreferenceActivity
{
  public static final String EXTRA_CATEGORY = "category";

  public static final String CAT_LAYOUT = "layout";
  public static final String CAT_THEME = "theme";
  public static final String CAT_AI = "ai";
  public static final String CAT_SUGGESTIONS = "suggestions";
  public static final String CAT_TYPING = "typing";
  public static final String CAT_FEEDBACK = "feedback";
  public static final String CAT_CLIPBOARD = "clipboard";
  public static final String CAT_ABOUT = "about";

  private boolean _preferencesAvailable;
  private String _category;

  private TextView _tvCategoryTitle;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    try
    {
      Config.migrate(getPreferenceManager().getSharedPreferences());
      _preferencesAvailable = true;
    }
    catch (Exception e)
    {
      finish();
      return;
    }

    // Hide native action bar in favor of unified in-layout header
    if (getActionBar() != null)
    {
      getActionBar().hide();
    }

    // Style system navigation & status bar adaptively (Day/Night)
    SettingsThemeHelper.applyAdaptiveSystemBars(this);

    setContentView(R.layout.activity_settings_category);

    View btnBack = findViewById(R.id.btn_category_back);
    if (btnBack != null)
    {
      btnBack.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          finish();
        }
      });
    }

    _tvCategoryTitle = (TextView) findViewById(R.id.tv_category_title);

    _category = getIntent().getStringExtra(EXTRA_CATEGORY);
    if (_category == null)
      _category = CAT_LAYOUT;

    setupCategory(_category);

    try
    {
      ListView list = getListView();
      if (list != null)
      {
        enforceConsistentPadding(list);
      }
    }
    catch (Exception ignored) {}
  }

  private void setupCategory(String cat)
  {
    if (CAT_LAYOUT.equals(cat))
    {
      if (_tvCategoryTitle != null) _tvCategoryTitle.setText("Keyboard Layout & Keys");
      addPreferencesFromResource(R.xml.settings_layout);
      setupLayoutListeners();
    }
    else if (CAT_THEME.equals(cat))
    {
      try
      {
        startActivity(new Intent(this, typodev.keyboard.theme.ThemeCustomizerActivity.class));
      }
      catch (Exception ignored) {}
      finish();
      return;
    }
    else if (CAT_AI.equals(cat))
    {
      if (_tvCategoryTitle != null) _tvCategoryTitle.setText("AI Assistant");
      addPreferencesFromResource(R.xml.settings_ai);
      setupAiListeners();
    }
    else if (CAT_SUGGESTIONS.equals(cat))
    {
      if (_tvCategoryTitle != null) _tvCategoryTitle.setText("Suggestions & Dictionaries");
      addPreferencesFromResource(R.xml.settings_suggestions);
      setupSuggestionsListeners();
    }
    else if (CAT_TYPING.equals(cat))
    {
      if (_tvCategoryTitle != null) _tvCategoryTitle.setText("Typing & Gestures");
      addPreferencesFromResource(R.xml.settings_typing);
    }
    else if (CAT_FEEDBACK.equals(cat))
    {
      if (_tvCategoryTitle != null) _tvCategoryTitle.setText("Sound & Vibration");
      addPreferencesFromResource(R.xml.settings_feedback);
    }
    else if (CAT_CLIPBOARD.equals(cat))
    {
      if (_tvCategoryTitle != null) _tvCategoryTitle.setText("Clipboard Manager");
      addPreferencesFromResource(R.xml.settings_clipboard);
    }
    else if (CAT_ABOUT.equals(cat))
    {
      if (_tvCategoryTitle != null) _tvCategoryTitle.setText("About TypoDev");
      addPreferencesFromResource(R.xml.settings_about);
      setupAboutListeners();
    }
    else
    {
      if (_tvCategoryTitle != null) _tvCategoryTitle.setText("Settings");
      addPreferencesFromResource(R.xml.settings_layout);
      setupLayoutListeners();
    }
  }

  private void setupLayoutListeners()
  {
    Preference.OnPreferenceClickListener symbolCustomizerListener = new Preference.OnPreferenceClickListener()
    {
      @Override
      public boolean onPreferenceClick(Preference preference)
      {
        try
        {
          startActivity(new Intent(SettingsCategoryActivity.this,
              typodev.keyboard.symbol.KeySymbolCustomizerActivity.class));
        }
        catch (Exception e)
        {
          Toast.makeText(SettingsCategoryActivity.this, "Cannot open symbol customizer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return true;
      }
    };

    Preference symbolPref = findPreference("pref_symbol_customizer");
    if (symbolPref != null) symbolPref.setOnPreferenceClickListener(symbolCustomizerListener);

    Preference symbolNestedPref = findPreference("pref_symbol_customizer_nested");
    if (symbolNestedPref != null) symbolNestedPref.setOnPreferenceClickListener(symbolCustomizerListener);
  }

  private void setupAiListeners()
  {
    final ListPreference providerPref = (ListPreference) findPreference("pref_ai_provider");
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

    final ListPreference geminiModelPref = (ListPreference) findPreference("gemini_working_model");
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
            Toast.makeText(SettingsCategoryActivity.this, "Could not open browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          }
          return true;
        }
      });
    }

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

    final ListPreference openAiModelPref = (ListPreference) findPreference("openai_model");
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

    Preference openAiDialogPref = findPreference("pref_open_ai_dialog");
    if (openAiDialogPref != null)
    {
      openAiDialogPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(Preference preference)
        {
          typodev.keyboard.ai.AiSettingsDialog.show(SettingsCategoryActivity.this, null, new typodev.keyboard.ai.AiSettingsDialog.OnSettingsSavedListener()
          {
            @Override
            public void onSaved()
            {
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
  }

  private void setupSuggestionsListeners()
  {
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
            startActivity(new Intent(SettingsCategoryActivity.this, DictionariesActivity.class));
          }
          catch (Exception e)
          {
            Toast.makeText(SettingsCategoryActivity.this, "Cannot open dictionaries: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          }
          return true;
        }
      });
    }


    Preference learningPref = findPreference("pref_learning_enabled");
    if (learningPref != null)
    {
      learningPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
      {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
          boolean enabled = Boolean.TRUE.equals(newValue);
          typodev.keyboard.suggestions.UserVocabularyStore.instance(SettingsCategoryActivity.this).setLearningEnabled(enabled);
          return true;
        }
      });
    }

    Preference clearLearnedPref = findPreference("pref_clear_learned_data");
    if (clearLearnedPref != null)
    {
      clearLearnedPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(Preference preference)
        {
          new AlertDialog.Builder(SettingsCategoryActivity.this)
              .setTitle("Clear Learned Data")
              .setMessage("Are you sure you want to reset all personalized vocabulary, learned words, and prediction history?")
              .setPositiveButton("Clear", new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                  typodev.keyboard.suggestions.UserVocabularyStore.instance(SettingsCategoryActivity.this).clearLearnedData();
                  typodev.keyboard.suggestions.NextWordPredictor.instance(SettingsCategoryActivity.this).clearLearnedData();
                  Toast.makeText(SettingsCategoryActivity.this, "Learned vocabulary and predictions cleared", Toast.LENGTH_SHORT).show();
                }
              })
              .setNegativeButton(android.R.string.cancel, null)
              .show();
          return true;
        }
      });
    }
  }

  private void setupAboutListeners()
  {
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
            startActivity(new Intent(SettingsCategoryActivity.this, LauncherActivity.class));
          }
          catch (Exception e)
          {
            Toast.makeText(SettingsCategoryActivity.this, "Cannot open test area: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          }
          return true;
        }
      });
    }

    Preference aboutPref = findPreference("pref_about_app");
    if (aboutPref != null)
    {
      aboutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
      {
        @Override
        public boolean onPreferenceClick(Preference preference)
        {
          new AlertDialog.Builder(SettingsCategoryActivity.this)
              .setTitle("TypoDev")
              .setMessage("type faster, write smarter\n\nVersion 2.1.0 (with Gemini AI & Bengali Suggestions)\n\n• Privacy-focused, lightweight virtual keyboard\n• Smart Gemini AI Assistant\n• 43,000+ Bengali & Multi-language wordlists\n\nOpen Source on GitHub:\nhttps://github.com/ItsAsadullah/TypoDev")
              .setPositiveButton("OK", null)
              .setNeutralButton("GitHub", new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                  try
                  {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ItsAsadullah/TypoDev")));
                  }
                  catch (Exception ignored) {}
                }
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

  @Override
  public void onConfigurationChanged(android.content.res.Configuration newConfig)
  {
    super.onConfigurationChanged(newConfig);
    SettingsThemeHelper.applyAdaptiveSystemBars(this);
    try
    {
      ListView list = getListView();
      if (list != null)
      {
        enforceConsistentPadding(list);
      }
    }
    catch (Exception ignored) {}
  }

  private static boolean isCardItem(Object item)
  {
    if (item == null) return false;
    if (item instanceof android.preference.PreferenceCategory) return false;
    if (item instanceof typodev.keyboard.prefs.ListGroupPreference) return false;
    if (item instanceof typodev.keyboard.prefs.ListGroupPreference.AddButton) return false;
    return (item instanceof android.preference.Preference);
  }

  private void enforceConsistentPadding(final ListView list)
  {
    if (list == null) return;
    int bgColor = getResources().getColor(R.color.settings_bg);
    list.setBackgroundColor(bgColor);
    list.setClipToPadding(false);
    list.setDivider(null);
    list.setDividerHeight(0);
    list.setScrollingCacheEnabled(false);
    list.setSmoothScrollbarEnabled(true);
    int padB = (int)(48 * getResources().getDisplayMetrics().density);
    list.setPadding(0, 0, 0, padB);

    final float density = getResources().getDisplayMetrics().density;
    final int padSide = (int)(32 * density);
    final int padVert = (int)(13 * density);
    final int padHeaderL = (int)(20 * density);
    final int padHeaderT = (int)(22 * density);
    final int padHeaderB = (int)(6 * density);
    final int padHeaderR = (int)(16 * density);

    list.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()
    {
      @Override
      public boolean onPreDraw()
      {
        android.widget.ListAdapter adapter = list.getAdapter();
        if (adapter == null) return true;
        int totalCount = adapter.getCount();
        int childCount = list.getChildCount();
        int firstPos = list.getFirstVisiblePosition();

        for (int i = 0; i < childCount; i++)
        {
          View child = list.getChildAt(i);
          if (child == null) continue;
          int pos = firstPos + i;
          if (pos < 0 || pos >= totalCount) continue;

          Object item = adapter.getItem(pos);

          if (item instanceof typodev.keyboard.prefs.ListGroupPreference)
          {
            // Empty container row - hide completely to avoid empty ghost cards and height jumps
            if (child.getVisibility() != View.GONE)
            {
              child.setVisibility(View.GONE);
            }
            android.view.ViewGroup.LayoutParams lp = child.getLayoutParams();
            if (lp != null && lp.height != 0)
            {
              lp.height = 0;
              child.setLayoutParams(lp);
            }
            continue;
          }
          else if (item instanceof typodev.keyboard.prefs.ListGroupPreference.AddButton)
          {
            // Let the add button retain its dedicated button layout and background
            continue;
          }
          else if (item instanceof android.preference.PreferenceCategory)
          {
            Integer currentBg = (Integer) child.getTag(R.id.tag_card_bg);
            if (currentBg == null || currentBg != -1)
            {
              child.setBackground(null);
              child.setTag(R.id.tag_card_bg, -1);
            }
            if (child.getPaddingLeft() != padHeaderL || child.getPaddingTop() != padHeaderT ||
                child.getPaddingRight() != padHeaderR || child.getPaddingBottom() != padHeaderB)
            {
              child.setPadding(padHeaderL, padHeaderT, padHeaderR, padHeaderB);
            }
          }
          else if (isCardItem(item))
          {
            boolean isTop = true;
            for (int prev = pos - 1; prev >= 0; prev--)
            {
              Object prevItem = adapter.getItem(prev);
              if (prevItem instanceof typodev.keyboard.prefs.ListGroupPreference) continue;
              if (isCardItem(prevItem))
              {
                isTop = false;
              }
              break;
            }

            boolean isBottom = true;
            for (int next = pos + 1; next < totalCount; next++)
            {
              Object nextItem = adapter.getItem(next);
              if (nextItem instanceof typodev.keyboard.prefs.ListGroupPreference) continue;
              if (isCardItem(nextItem))
              {
                isBottom = false;
              }
              break;
            }

            int targetRes;
            if (isTop && isBottom)
            {
              targetRes = R.drawable.ios_card_single;
            }
            else if (isTop)
            {
              targetRes = R.drawable.ios_card_top;
            }
            else if (isBottom)
            {
              targetRes = R.drawable.ios_card_bottom;
            }
            else
            {
              targetRes = R.drawable.ios_card_middle;
            }

            Integer currentBg = (Integer) child.getTag(R.id.tag_card_bg);
            if (currentBg == null || currentBg != targetRes)
            {
              child.setBackgroundResource(targetRes);
              child.setTag(R.id.tag_card_bg, targetRes);
            }

            if (child.getPaddingLeft() != padSide || child.getPaddingTop() != padVert ||
                child.getPaddingRight() != padSide || child.getPaddingBottom() != padVert)
            {
              child.setPadding(padSide, padVert, padSide, padVert);
            }
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
        if (dialog.getWindow() != null)
        {
          int dialogBg = getResources().getColor(R.color.settings_bg);
          dialog.getWindow().setBackgroundDrawable(new ColorDrawable(dialogBg));
          SettingsThemeHelper.applyAdaptiveSystemBars(this, dialog.getWindow());
        }
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

  @Override
  protected void onStop()
  {
    if (_preferencesAvailable)
      DirectBootAwarePreferences
        .copy_preferences_to_protected_storage(this,
            getPreferenceManager().getSharedPreferences());
    super.onStop();
  }
}
