package typodev.keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import typodev.keyboard.dict.DictionariesActivity;
import typodev.keyboard.snippet.SnippetManagerActivity;
import typodev.keyboard.symbol.KeySymbolCustomizerActivity;
import typodev.keyboard.theme.ThemeCustomizerActivity;

/**
 * Modern, beautifully styled Home & Setup Activity for TypoDev Keyboard.
 * Features dynamic IME enablement detection, 1-tap setup steps, interactive test pad,
 * and quick shortcuts to all keyboard customization hubs.
 */
public class LauncherActivity extends Activity implements Handler.Callback
{
  /** Text is replaced when receiving key events. */
  TextView _tryhere_text;
  EditText _tryhere_area;
  ImageView _btnClearTryhere;

  TextView _tvStatusBadge;
  TextView _badgeStep1;
  TextView _badgeStep2;
  Button _btnStepEnable;
  Button _btnStepSelect;
  TextView _tvStep1Done;
  TextView _tvStep2Done;

  View _tvSectionActivation;
  View _cardActivationSteps;
  View _cardTutorial;

  List<Animatable> _animations;
  Handler _handler;
  private ContentObserver _settingsObserver;
  private Runnable _imePickerPollRunnable;

  private float _density;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    // Global safety uncaught exception logging
    try
    {
      final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
      Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
      {
        @Override
        public void uncaughtException(Thread t, Throwable e)
        {
          try
          {
            Logs.exn("Uncaught exception in TypoDev launcher", e);
            java.io.File crashFile = new java.io.File(getFilesDir(), "last_crash.log");
            java.io.PrintWriter pw = new java.io.PrintWriter(crashFile);
            e.printStackTrace(pw);
            pw.flush();
            pw.close();
          }
          catch (Throwable ignored) {}

          if (defaultHandler != null)
          {
            defaultHandler.uncaughtException(t, e);
          }
        }
      });
    }
    catch (Throwable ignored) {}

    try
    {
      // Hide native action bar in favor of sleek in-layout header
      if (getActionBar() != null)
      {
        getActionBar().hide();
      }
    }
    catch (Throwable ignored) {}

    try
    {
      _density = getResources().getDisplayMetrics().density;
    }
    catch (Throwable t)
    {
      _density = 2.0f;
    }

    try
    {
      setContentView(R.layout.launcher_activity);
    }
    catch (Throwable t)
    {
      Logs.exn("Failed to inflate launcher_activity layout", t);
      try
      {
        Intent fallbackIntent = new Intent(this, SettingsActivity.class);
        fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(fallbackIntent);
        finish();
        return;
      }
      catch (Throwable ignored) {}
    }

    // Apply adaptive system bars (Day/Night) AFTER contentView is attached
    try
    {
      SettingsThemeHelper.applyAdaptiveSystemBars(this);
    }
    catch (Throwable ignored) {}

    try
    {
      _tryhere_text = findViewById(R.id.launcher_tryhere_text);
      _tryhere_area = findViewById(R.id.launcher_tryhere_area);
      _btnClearTryhere = findViewById(R.id.btn_clear_tryhere);

      _tvStatusBadge = findViewById(R.id.tv_launcher_status_badge);
      _badgeStep1 = findViewById(R.id.badge_step_1);
      _badgeStep2 = findViewById(R.id.badge_step_2);
      _btnStepEnable = findViewById(R.id.btn_step_enable);
      _btnStepSelect = findViewById(R.id.btn_step_select);
      _tvStep1Done = findViewById(R.id.tv_step_1_done);
      _tvStep2Done = findViewById(R.id.tv_step_2_done);

      _tvSectionActivation = findViewById(R.id.tv_section_activation);
      _cardActivationSteps = findViewById(R.id.card_activation_steps);
      _cardTutorial = findViewById(R.id.card_tutorial);

      _handler = new Handler(getMainLooper(), this);

      _settingsObserver = new ContentObserver(_handler)
      {
        @Override
        public void onChange(boolean selfChange)
        {
          super.onChange(selfChange);
          updateSetupStatus();
        }
      };
      try
      {
        getContentResolver().registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.DEFAULT_INPUT_METHOD),
            false,
            _settingsObserver);
        getContentResolver().registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS),
            false,
            _settingsObserver);
      }
      catch (Throwable ignored) {}

      // Clicking any tutorial row replays the animations and focuses test pad
      View.OnClickListener animClickListener = new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          playAnimationsOnce();
          if (_tryhere_area != null)
          {
            _tryhere_area.requestFocus();
            try
            {
              InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
              if (imm != null)
              {
                imm.showSoftInput(_tryhere_area, InputMethodManager.SHOW_IMPLICIT);
              }
            }
            catch (Throwable ignored) {}
          }
        }
      };
      View rowSwipe = findViewById(R.id.row_anim_swipe);
      if (rowSwipe != null) rowSwipe.setOnClickListener(animClickListener);
      View rowCircle = findViewById(R.id.row_anim_circle);
      if (rowCircle != null) rowCircle.setOnClickListener(animClickListener);
      View rowRoundTrip = findViewById(R.id.row_anim_round_trip);
      if (rowRoundTrip != null) rowRoundTrip.setOnClickListener(animClickListener);

      // Header Settings Cog
      View btnSettings = findViewById(R.id.btn_launcher_settings);
      if (btnSettings != null)
      {
        btnSettings.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            try
            {
              startActivity(new Intent(LauncherActivity.this, SettingsActivity.class));
            }
            catch (Throwable ignored) {}
          }
        });
      }

      // Step 1 Click
      View.OnClickListener enableListener = new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          launch_imesettings(v);
        }
      };
      if (_btnStepEnable != null) _btnStepEnable.setOnClickListener(enableListener);
      View rowStep1 = findViewById(R.id.row_step_enable);
      if (rowStep1 != null) rowStep1.setOnClickListener(enableListener);

      // Step 2 Click
      View.OnClickListener selectListener = new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          launch_imepicker(v);
        }
      };
      if (_btnStepSelect != null) _btnStepSelect.setOnClickListener(selectListener);
      View rowStep2 = findViewById(R.id.row_step_select);
      if (rowStep2 != null) rowStep2.setOnClickListener(selectListener);

      // Interactive Typing Test Pad
      final View containerTryhereInput = findViewById(R.id.container_tryhere_input);
      if (_tryhere_area != null)
      {
        if (containerTryhereInput != null)
        {
          _tryhere_area.setOnFocusChangeListener(new View.OnFocusChangeListener()
          {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
              containerTryhereInput.setSelected(hasFocus);
            }
          });
        }

        _tryhere_area.setOnKeyListener(new View.OnKeyListener()
        {
          @Override
          public boolean onKey(View v, int keyCode, KeyEvent event)
          {
            if (event.getAction() == KeyEvent.ACTION_DOWN)
            {
              displayKeyEvent(event);
              if (keyCode == KeyEvent.KEYCODE_TAB)
              {
                return true; // Keep focus in test pad
              }
            }
            return false;
          }
        });

        _tryhere_area.addTextChangedListener(new TextWatcher()
        {
          @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count)
          {
            if (count == 1 && s != null && start < s.length())
            {
              char c = s.charAt(start);
              if (c == '\t')
              {
                if (_tryhere_text != null) _tryhere_text.setText("TAB");
              }
              else if (c == '\n')
              {
                if (_tryhere_text != null) _tryhere_text.setText("ENTER");
              }
            }
          }
          @Override
          public void afterTextChanged(Editable s)
          {
            if (_btnClearTryhere != null)
            {
              _btnClearTryhere.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
            }
          }
        });
      }

      if (_btnClearTryhere != null)
      {
        _btnClearTryhere.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
            if (_tryhere_area != null) _tryhere_area.setText("");
          }
        });
      }

      // Quick Feature Hubs
      wireCard(R.id.card_launcher_theme, ThemeCustomizerActivity.class, null);
      wireCard(R.id.card_launcher_ai, SettingsCategoryActivity.class, SettingsCategoryActivity.CAT_AI);
      wireCard(R.id.card_launcher_snippets, SnippetManagerActivity.class, null);
      wireCard(R.id.card_launcher_dicts, DictionariesActivity.class, null);
      wireCard(R.id.card_launcher_keys, KeySymbolCustomizerActivity.class, null);
      wireCard(R.id.card_launcher_all_settings, SettingsActivity.class, null);
    }
    catch (Throwable t)
    {
      Logs.exn("Error initializing launcher views", t);
    }
  }

  private void wireCard(int cardId, final Class<?> targetClass, final String extraCategory)
  {
    View card = findViewById(cardId);
    if (card != null)
    {
      card.setOnClickListener(new View.OnClickListener()
      {
        @Override
        public void onClick(View v)
        {
          try
          {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            View focus = getCurrentFocus();
            if (imm != null && focus != null)
            {
              imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            }
          }
          catch (Throwable ignored) {}

          try
          {
            Intent intent = new Intent(LauncherActivity.this, targetClass);
            if (extraCategory != null)
            {
              intent.putExtra(SettingsCategoryActivity.EXTRA_CATEGORY, extraCategory);
            }
            startActivity(intent);
          }
          catch (Throwable ignored) {}
        }
      });
    }
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    try
    {
      updateSetupStatus();
    }
    catch (Throwable ignored) {}
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus)
  {
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus)
    {
      try
      {
        updateSetupStatus();
        if (_handler != null)
        {
          _handler.postDelayed(new Runnable()
          {
            @Override
            public void run()
            {
              try { updateSetupStatus(); } catch (Throwable ignored) {}
            }
          }, 200);
          _handler.postDelayed(new Runnable()
          {
            @Override
            public void run()
            {
              try { updateSetupStatus(); } catch (Throwable ignored) {}
            }
          }, 600);
          _handler.postDelayed(new Runnable()
          {
            @Override
            public void run()
            {
              try { updateSetupStatus(); } catch (Throwable ignored) {}
            }
          }, 1200);
        }
      }
      catch (Throwable ignored) {}
    }
  }

  @Override
  protected void onPause()
  {
    super.onPause();
    try
    {
      stopAnimations();
    }
    catch (Throwable ignored) {}
  }

  @Override
  protected void onDestroy()
  {
    super.onDestroy();
    try
    {
      if (_settingsObserver != null)
      {
        try
        {
          getContentResolver().unregisterContentObserver(_settingsObserver);
        }
        catch (Throwable ignored) {}
        _settingsObserver = null;
      }
      if (_handler != null && _imePickerPollRunnable != null)
      {
        _handler.removeCallbacks(_imePickerPollRunnable);
      }
    }
    catch (Throwable ignored) {}
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig)
  {
    super.onConfigurationChanged(newConfig);
    try
    {
      SettingsThemeHelper.applyAdaptiveSystemBars(this);
    }
    catch (Throwable ignored) {}
  }

  private void updateSetupStatus()
  {
    try
    {
      boolean enabled = isImeEnabled();
      boolean selected = isImeSelected();
      boolean isFullyActive = enabled && selected;

      boolean wasActive = (_cardTutorial != null && _cardTutorial.getVisibility() == View.VISIBLE);

      // Once keyboard is activated, hide activation options and show the 3 typing gesture animations!
      if (_tvSectionActivation != null)
      {
        _tvSectionActivation.setVisibility(isFullyActive ? View.GONE : View.VISIBLE);
      }
      if (_cardActivationSteps != null)
      {
        _cardActivationSteps.setVisibility(isFullyActive ? View.GONE : View.VISIBLE);
      }
      if (_cardTutorial != null)
      {
        _cardTutorial.setVisibility(isFullyActive ? View.VISIBLE : View.GONE);
      }

      if (isFullyActive)
      {
        if (!wasActive || _animations == null || _animations.isEmpty())
        {
          startAnimations();
        }
      }
      else
      {
        stopAnimations();
      }

      // Step 1 Badge & Button
      if (_badgeStep1 != null)
      {
        if (enabled)
        {
          _badgeStep1.setText("✓");
          _badgeStep1.setBackground(makeCircleDrawable(Color.parseColor("#34C759")));
        }
        else
        {
          _badgeStep1.setText("1");
          _badgeStep1.setBackgroundResource(R.drawable.settings_fab_bg);
        }
      }
      if (_btnStepEnable != null) _btnStepEnable.setVisibility(enabled ? View.GONE : View.VISIBLE);
      if (_tvStep1Done != null) _tvStep1Done.setVisibility(enabled ? View.VISIBLE : View.GONE);

      // Step 2 Badge & Button
      if (_badgeStep2 != null)
      {
        if (selected)
        {
          _badgeStep2.setText("✓");
          _badgeStep2.setBackground(makeCircleDrawable(Color.parseColor("#34C759")));
        }
        else
        {
          _badgeStep2.setText("2");
          _badgeStep2.setBackgroundResource(R.drawable.settings_fab_bg);
        }
      }
      if (_btnStepSelect != null) _btnStepSelect.setVisibility(selected ? View.GONE : View.VISIBLE);
      if (_tvStep2Done != null) _tvStep2Done.setVisibility(selected ? View.VISIBLE : View.GONE);

      // Status Banner Pill
      if (_tvStatusBadge != null)
      {
        int accentColor;
        try
        {
          accentColor = ContextCompat.getColor(this, R.color.settings_accent);
        }
        catch (Throwable t)
        {
          accentColor = 0xFFFF5733;
        }

        if (enabled && selected)
        {
          _tvStatusBadge.setText("🎉 TypoDev is Active & Ready to Use!");
          _tvStatusBadge.setTextColor(Color.parseColor("#34C759"));
        }
        else if (enabled)
        {
          _tvStatusBadge.setText("⚡ Almost Ready • Tap Step 2 to Switch");
          _tvStatusBadge.setTextColor(accentColor);
        }
        else
        {
          _tvStatusBadge.setText("⚙️ Setup Required • Tap Step 1 to Enable");
          _tvStatusBadge.setTextColor(accentColor);
        }
      }
    }
    catch (Throwable t)
    {
      Logs.exn("Error updating setup status", t);
    }
  }

  private void startAnimations()
  {
    try
    {
      if (_animations == null)
      {
        _animations = new ArrayList<Animatable>();
      }
      _animations.clear();
      addAnim(R.id.launcher_anim_swipe, R.drawable.doc_anim_swipe);
      addAnim(R.id.launcher_anim_circle, R.drawable.doc_anim_circle);
      addAnim(R.id.launcher_anim_round_trip, R.drawable.doc_anim_round_trip);

      if (_handler != null && !_animations.isEmpty())
      {
        _handler.removeMessages(0);
        _handler.sendEmptyMessageDelayed(0, 400);
      }
    }
    catch (Throwable ignored) {}
  }

  private void addAnim(int id, int animResId)
  {
    try
    {
      ImageView img = findViewById(id);
      if (img != null)
      {
        try
        {
          android.graphics.drawable.Drawable d = ContextCompat.getDrawable(this, animResId);
          if (d != null)
          {
            img.setImageDrawable(d);
            if (d instanceof Animatable)
            {
              _animations.add((Animatable) d);
            }
          }
        }
        catch (Throwable ignored) {}
      }
    }
    catch (Throwable ignored) {}
  }

  private void playAnimationsOnce()
  {
    try
    {
      if (_animations != null && !_animations.isEmpty())
      {
        for (Animatable anim : _animations)
        {
          if (anim != null)
          {
            try
            {
              if (anim.isRunning()) anim.stop();
              anim.start();
            }
            catch (Throwable ignored) {}
          }
        }
      }
      else
      {
        startAnimations();
      }
      if (_handler != null && _animations != null && !_animations.isEmpty())
      {
        _handler.removeMessages(0);
        _handler.sendEmptyMessageDelayed(0, 3000);
      }
    }
    catch (Throwable ignored) {}
  }

  private void stopAnimations()
  {
    try
    {
      if (_handler != null)
      {
        _handler.removeMessages(0);
      }
      if (_animations != null)
      {
        for (Animatable anim : _animations)
        {
          if (anim != null)
          {
            try
            {
              if (anim.isRunning()) anim.stop();
            }
            catch (Throwable ignored) {}
          }
        }
        _animations.clear();
      }
    }
    catch (Throwable ignored) {}
  }

  @Override
  public boolean handleMessage(Message msg)
  {
    try
    {
      if (_animations != null && !_animations.isEmpty())
      {
        for (Animatable anim : _animations)
        {
          if (anim != null)
          {
            try
            {
              if (anim.isRunning()) anim.stop();
              anim.start();
            }
            catch (Throwable ignored) {}
          }
        }
      }
      if (_handler != null)
      {
        _handler.sendEmptyMessageDelayed(0, 3000);
      }
    }
    catch (Throwable ignored) {}
    return true;
  }

  public boolean isImeEnabled()
  {
    try
    {
      InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
      if (imm == null) return false;
      List<InputMethodInfo> enabledImes = imm.getEnabledInputMethodList();
      if (enabledImes == null) return false;
      String myPkg = getPackageName();
      for (InputMethodInfo info : enabledImes)
      {
        if (info != null && info.getPackageName() != null && info.getPackageName().equalsIgnoreCase(myPkg))
        {
          return true;
        }
      }
    }
    catch (Throwable ignored) {}
    return false;
  }

  public boolean isImeSelected()
  {
    try
    {
      String defaultIme = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
      return defaultIme != null && defaultIme.toLowerCase(Locale.US).contains(getPackageName().toLowerCase(Locale.US));
    }
    catch (Throwable ignored) {}
    return false;
  }

  private GradientDrawable makeCircleDrawable(int color)
  {
    GradientDrawable gd = new GradientDrawable();
    gd.setShape(GradientDrawable.OVAL);
    gd.setColor(color);
    return gd;
  }

  private int dp(float v) { return (int)(v * _density + 0.5f); }

  @Override
  public final boolean onCreateOptionsMenu(Menu menu)
  {
    try
    {
      getMenuInflater().inflate(R.menu.launcher_menu, menu);
      return true;
    }
    catch (Throwable ignored)
    {
      return false;
    }
  }

  @Override
  public final boolean onOptionsItemSelected(MenuItem item)
  {
    if (item != null && item.getItemId() == R.id.btnLaunchSettingsActivity)
    {
      try
      {
        Intent intent = new Intent(LauncherActivity.this, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
      }
      catch (Throwable ignored) {}
    }
    return super.onOptionsItemSelected(item);
  }

  public void launch_imesettings(View _btn)
  {
    try
    {
      startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
    }
    catch (Throwable ignored) {}
  }

  public void launch_imepicker(View v)
  {
    try
    {
      InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
      if (imm != null)
      {
        imm.showInputMethodPicker();
      }
      startImeSelectionPolling();
    }
    catch (Throwable ignored) {}
  }

  private void startImeSelectionPolling()
  {
    if (_handler == null) return;
    if (_imePickerPollRunnable != null)
    {
      _handler.removeCallbacks(_imePickerPollRunnable);
    }
    _imePickerPollRunnable = new Runnable()
    {
      int attempts = 0;
      @Override
      public void run()
      {
        try
        {
          updateSetupStatus();
          attempts++;
          if (!isImeSelected() && attempts < 25)
          {
            if (_handler != null) _handler.postDelayed(this, 250);
          }
        }
        catch (Throwable ignored) {}
      }
    };
    _handler.postDelayed(_imePickerPollRunnable, 300);
  }

  public void launch_dictionaries_activity(View v)
  {
    try
    {
      startActivity(new Intent(this, DictionariesActivity.class));
    }
    catch (Throwable ignored) {}
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent ev)
  {
    if (ev != null && ev.getAction() == KeyEvent.ACTION_DOWN)
    {
      int keyCode = ev.getKeyCode();
      if (keyCode != KeyEvent.KEYCODE_BACK)
      {
        displayKeyEvent(ev);
        if (keyCode == KeyEvent.KEYCODE_TAB && _tryhere_area != null && _tryhere_area.hasFocus())
        {
          return true; // Keep focus in test pad
        }
      }
    }
    return super.dispatchKeyEvent(ev);
  }

  private void displayKeyEvent(KeyEvent ev)
  {
    if (_tryhere_text == null || ev == null) return;
    try
    {
      int keyCode = ev.getKeyCode();
      if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return;

      StringBuilder s = new StringBuilder();

      // If key itself is not a modifier, prepend active modifiers
      boolean isModifier = KeyEvent.isModifierKey(keyCode) || keyCode == KeyEvent.KEYCODE_FUNCTION;
      if (!isModifier)
      {
        if (ev.isCtrlPressed()) s.append("Ctrl+");
        if (ev.isAltPressed()) s.append("Alt+");
        if (ev.isShiftPressed()) s.append("Shift+");
        if (ev.isMetaPressed()) s.append("Meta+");
        if (ev.isFunctionPressed()) s.append("Fn+");
      }

      String cleanName;
      switch (keyCode)
      {
        case KeyEvent.KEYCODE_CTRL_LEFT:
        case KeyEvent.KEYCODE_CTRL_RIGHT:
          cleanName = "CTRL";
          break;
        case KeyEvent.KEYCODE_SHIFT_LEFT:
        case KeyEvent.KEYCODE_SHIFT_RIGHT:
          cleanName = "SHIFT";
          break;
        case KeyEvent.KEYCODE_ALT_LEFT:
        case KeyEvent.KEYCODE_ALT_RIGHT:
          cleanName = "ALT";
          break;
        case KeyEvent.KEYCODE_TAB:
          cleanName = "TAB";
          break;
        case KeyEvent.KEYCODE_FUNCTION:
          cleanName = "FN";
          break;
        case KeyEvent.KEYCODE_CAPS_LOCK:
          cleanName = "CAPS_LOCK";
          break;
        case KeyEvent.KEYCODE_ESCAPE:
          cleanName = "ESC";
          break;
        case KeyEvent.KEYCODE_DEL:
          cleanName = "BACKSPACE";
          break;
        case KeyEvent.KEYCODE_FORWARD_DEL:
          cleanName = "DELETE";
          break;
        case KeyEvent.KEYCODE_ENTER:
          cleanName = "ENTER";
          break;
        case KeyEvent.KEYCODE_SPACE:
          cleanName = "SPACE";
          break;
        case KeyEvent.KEYCODE_DPAD_UP:
          cleanName = "DPAD_UP";
          break;
        case KeyEvent.KEYCODE_DPAD_DOWN:
          cleanName = "DPAD_DOWN";
          break;
        case KeyEvent.KEYCODE_DPAD_LEFT:
          cleanName = "DPAD_LEFT";
          break;
        case KeyEvent.KEYCODE_DPAD_RIGHT:
          cleanName = "DPAD_RIGHT";
          break;
        case KeyEvent.KEYCODE_PAGE_UP:
          cleanName = "PAGE_UP";
          break;
        case KeyEvent.KEYCODE_PAGE_DOWN:
          cleanName = "PAGE_DOWN";
          break;
        case KeyEvent.KEYCODE_MOVE_HOME:
          cleanName = "HOME";
          break;
        case KeyEvent.KEYCODE_MOVE_END:
          cleanName = "END";
          break;
        default:
          String kc = KeyEvent.keyCodeToString(keyCode);
          cleanName = (kc != null) ? kc.replaceFirst("^KEYCODE_", "") : ("KEY_" + keyCode);
          break;
      }

      s.append(cleanName);
      _tryhere_text.setText(s.toString());
    }
    catch (Throwable ignored) {}
  }
}
