package typodev.keyboard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import typodev.keyboard.snippet.SnippetManagerActivity;
import typodev.keyboard.theme.ThemeCustomizerActivity;

public class SettingsActivity extends Activity
{
  private boolean _preferencesAvailable;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    // Run the config migration on this prefs as it might be different from the
    // one used by the keyboard, which have been migrated.
    try
    {
      Config.migrate(PreferenceManager.getDefaultSharedPreferences(this));
      _preferencesAvailable = true;
    }
    catch (Exception e)
    {
      finish();
      return;
    }

    // Direct routing if a specific category was requested
    String targetCat = getIntent().getStringExtra(SettingsCategoryActivity.EXTRA_CATEGORY);
    if (targetCat != null)
    {
      openCategory(targetCat);
      finish();
      return;
    }

    // Hide native action bar in favor of sleek in-layout header
    if (getActionBar() != null)
    {
      getActionBar().hide();
    }

    // Style system navigation & status bar adaptively (Day/Night)
    SettingsThemeHelper.applyAdaptiveSystemBars(this);

    setContentView(R.layout.activity_settings_hub);

    setupHeader();
    setupCards();
  }

  @Override
  public void onConfigurationChanged(android.content.res.Configuration newConfig)
  {
    super.onConfigurationChanged(newConfig);
    SettingsThemeHelper.applyAdaptiveSystemBars(this);
  }

  private void setupHeader()
  {
    View btnBack = findViewById(R.id.btn_settings_back);
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
  }

  private void setupCards()
  {
    setCardAction(R.id.card_pref_layout, SettingsCategoryActivity.CAT_LAYOUT);
    setCardAction(R.id.card_pref_suggestions, SettingsCategoryActivity.CAT_SUGGESTIONS);
    setCardAction(R.id.card_pref_typing, SettingsCategoryActivity.CAT_TYPING);

    View cardTheme = findViewById(R.id.card_pref_theme);
    if (cardTheme != null)
    {
      cardTheme.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          try
          {
            startActivity(new Intent(SettingsActivity.this, ThemeCustomizerActivity.class));
          }
          catch (Exception e)
          {
            Toast.makeText(SettingsActivity.this, "Cannot open theme customizer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          }
        }
      });
    }

    setCardAction(R.id.card_pref_feedback, SettingsCategoryActivity.CAT_FEEDBACK);
    setCardAction(R.id.card_pref_ai, SettingsCategoryActivity.CAT_AI);
    setCardAction(R.id.card_pref_clipboard, SettingsCategoryActivity.CAT_CLIPBOARD);
    setCardAction(R.id.card_pref_about, SettingsCategoryActivity.CAT_ABOUT);

    // Direct shortcut cards
    View cardSnippets = findViewById(R.id.card_pref_snippets);
    if (cardSnippets != null)
    {
      cardSnippets.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          try
          {
            startActivity(new Intent(SettingsActivity.this, SnippetManagerActivity.class));
          }
          catch (Exception e)
          {
            Toast.makeText(SettingsActivity.this, "Cannot open snippet manager: " + e.getMessage(), Toast.LENGTH_SHORT).show();
          }
        }
      });
    }

    View cardTest = findViewById(R.id.card_pref_test);
    if (cardTest != null)
    {
      cardTest.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          launchTestArea();
        }
      });
    }
  }

  private void setCardAction(int viewId, final String categoryKey)
  {
    View card = findViewById(viewId);
    if (card != null)
    {
      card.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          openCategory(categoryKey);
        }
      });
    }
  }

  private void launchTestArea()
  {
    try
    {
      startActivity(new Intent(SettingsActivity.this, LauncherActivity.class));
    }
    catch (Exception e)
    {
      Toast.makeText(SettingsActivity.this, "Cannot open test area: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

  private void openCategory(String categoryKey)
  {
    try
    {
      Intent intent = new Intent(this, SettingsCategoryActivity.class);
      intent.putExtra(SettingsCategoryActivity.EXTRA_CATEGORY, categoryKey);
      startActivity(intent);
    }
    catch (Exception e)
    {
      Toast.makeText(this, "Could not open settings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onStop()
  {
    if (_preferencesAvailable)
    {
      DirectBootAwarePreferences.copy_preferences_to_protected_storage(
          this, PreferenceManager.getDefaultSharedPreferences(this));
    }
    super.onStop();
  }
}
