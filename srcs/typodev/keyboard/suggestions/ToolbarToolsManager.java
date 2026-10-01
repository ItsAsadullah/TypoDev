package typodev.keyboard.suggestions;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import typodev.keyboard.Config;
import typodev.keyboard.DirectBootAwarePreferences;
import typodev.keyboard.Keyboard2;
import typodev.keyboard.KeyValue;
import typodev.keyboard.Logs;
import typodev.keyboard.R;
import typodev.keyboard.SettingsActivity;
import typodev.keyboard.autofill.PasswordAutofillHelper;
import typodev.keyboard.snippet.SnippetManagerActivity;
import typodev.keyboard.theme.ThemeCustomizerActivity;

/**
 * Manages the list of pinned tools in the top toolbar and all available tools in the More Tools grid.
 * Provides persistence, pin/unpin operations, and unified tool activation.
 */
public class ToolbarToolsManager
{
  public static final class ToolItem
  {
    public final String id;
    public final int iconResId;
    public final String label;
    public final int viewId;

    public ToolItem(String id, int iconResId, String label, int viewId)
    {
      this.id = id;
      this.iconResId = iconResId;
      this.label = label;
      this.viewId = viewId;
    }
  }

  public static final List<ToolItem> ALL_TOOLS = Arrays.asList(
      new ToolItem("ai",        R.drawable.ic_toolbar_ai,        "AI",        R.id.toolbar_btn_ai),
      new ToolItem("autofill",  R.drawable.ic_toolbar_key,       "Autofill",  R.id.toolbar_btn_autofill),
      new ToolItem("translate", R.drawable.ic_toolbar_translate, "Translate", R.id.toolbar_btn_translate),
      new ToolItem("clipboard", R.drawable.ic_toolbar_clipboard, "Clipboard", R.id.toolbar_btn_clipboard),
      new ToolItem("fancy",     R.drawable.ic_toolbar_fancy,     "Styles",    R.id.toolbar_btn_fancy),
      new ToolItem("one_hand",  R.drawable.ic_toolbar_onehand,   "One Hand",  R.id.toolbar_btn_onehand),
      new ToolItem("haptic",    R.drawable.ic_toolbar_haptic,    "Haptic",    R.id.toolbar_btn_haptic),
      new ToolItem("sound",     R.drawable.ic_toolbar_sound,     "Sound",     R.id.toolbar_btn_sound),
      new ToolItem("popup",     R.drawable.ic_toolbar_popup,     "Key Popup", R.id.toolbar_btn_popup),
      new ToolItem("mic",       R.drawable.ic_toolbar_mic,       "Voice",     R.id.toolbar_btn_mic),
      new ToolItem("theme",     R.drawable.ic_pref_style,        "Themes",    R.id.toolbar_btn_theme),
      new ToolItem("settings",  R.drawable.cog_outline,          "Settings",  R.id.toolbar_btn_settings),
      new ToolItem("snippets",  R.drawable.ic_clip_edit,         "Snippets",  R.id.toolbar_btn_snippets),
      new ToolItem("dev",       R.drawable.ic_toolbar_dev,       "Dev Mode",  R.id.toolbar_btn_dev)
  );

  public static final String DEFAULT_PINNED = "ai,autofill,translate,clipboard,fancy,dev";
  private static final String PREF_PINNED_TOOLS = "pref_pinned_toolbar_tools";
  private static final String PREF_GRID_ORDER = "toolbar_more_order";
  private static final String SEP = ",";

  public interface OnToolsChangedListener
  {
    void onToolsChanged();
  }

  private static ToolbarToolsManager sInstance;
  private final Context _context;
  private final List<OnToolsChangedListener> _listeners = new ArrayList<>();

  public static synchronized ToolbarToolsManager getInstance(Context context)
  {
    if (sInstance == null)
    {
      sInstance = new ToolbarToolsManager(context.getApplicationContext());
    }
    return sInstance;
  }

  private ToolbarToolsManager(Context context)
  {
    _context = context;
  }

  public void addListener(OnToolsChangedListener listener)
  {
    if (listener != null && !_listeners.contains(listener))
      _listeners.add(listener);
  }

  public void removeListener(OnToolsChangedListener listener)
  {
    _listeners.remove(listener);
  }

  public void notifyListeners()
  {
    for (OnToolsChangedListener l : new ArrayList<>(_listeners))
    {
      try { l.onToolsChanged(); } catch (Throwable ignored) {}
    }
  }

  public ToolItem findToolById(String id)
  {
    if (id == null) return null;
    for (ToolItem t : ALL_TOOLS)
    {
      if (t.id.equals(id)) return t;
    }
    return null;
  }

  public List<ToolItem> getPinnedTools()
  {
    List<ToolItem> list = new ArrayList<>();
    try
    {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
      String saved = prefs.getString(PREF_PINNED_TOOLS, DEFAULT_PINNED);
      // Empty is a valid customization: every tool has been unpinned.
      if (saved == null) saved = DEFAULT_PINNED;
      String[] ids = saved.split(SEP);
      for (String id : ids)
      {
        String cleanId = id.trim();
        ToolItem item = findToolById(cleanId);
        if (item != null && !list.contains(item))
        {
          list.add(item);
        }
      }
    }
    catch (Throwable t)
    {
      list.clear();
      for (String id : DEFAULT_PINNED.split(SEP))
      {
        ToolItem item = findToolById(id);
        if (item != null) list.add(item);
      }
    }
    return list;
  }

  public boolean isPinned(String id)
  {
    if (id == null) return false;
    for (ToolItem t : getPinnedTools())
    {
      if (t.id.equals(id)) return true;
    }
    return false;
  }

  public void pinTool(String id)
  {
    if (id == null) return;
    List<ToolItem> current = getPinnedTools();
    for (ToolItem t : current)
    {
      if (t.id.equals(id)) return; // already pinned
    }
    ToolItem toAdd = findToolById(id);
    if (toAdd == null) return;
    current.add(toAdd);
    savePinnedList(current);
    notifyListeners();
  }

  public void unpinTool(String id)
  {
    if (id == null) return;
    List<ToolItem> current = getPinnedTools();
    int removeIdx = -1;
    for (int i = 0; i < current.size(); i++)
    {
      if (current.get(i).id.equals(id))
      {
        removeIdx = i;
        break;
      }
    }
    if (removeIdx >= 0)
    {
      current.remove(removeIdx);
      savePinnedList(current);
      notifyListeners();
    }
  }

  public void setPinnedOrder(List<String> orderedIds)
  {
    if (orderedIds == null) return;
    List<ToolItem> list = new ArrayList<>();
    for (String id : orderedIds)
    {
      ToolItem item = findToolById(id);
      if (item != null && !list.contains(item))
      {
        list.add(item);
      }
    }
    savePinnedList(list);
    notifyListeners();
  }

  private void savePinnedList(List<ToolItem> list)
  {
    try
    {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < list.size(); i++)
      {
        if (i > 0) sb.append(SEP);
        sb.append(list.get(i).id);
      }
      PreferenceManager.getDefaultSharedPreferences(_context)
          .edit().putString(PREF_PINNED_TOOLS, sb.toString()).apply();
    }
    catch (Throwable ignored) {}
  }

  public List<ToolItem> getGridTools()
  {
    List<ToolItem> list = new ArrayList<>();
    try
    {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
      String saved = prefs.getString(PREF_GRID_ORDER, "");
      if (saved != null && !saved.isEmpty())
      {
        String[] ids = saved.split(SEP);
        for (String id : ids)
        {
          ToolItem item = findToolById(id.trim());
          if (item != null && !list.contains(item))
            list.add(item);
        }
      }
      for (ToolItem t : ALL_TOOLS)
      {
        if (!list.contains(t)) list.add(t);
      }
    }
    catch (Throwable t)
    {
      list.clear();
      list.addAll(ALL_TOOLS);
    }
    return list;
  }

  public void saveGridOrder(List<ToolItem> list)
  {
    try
    {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < list.size(); i++)
      {
        if (i > 0) sb.append(SEP);
        sb.append(list.get(i).id);
      }
      PreferenceManager.getDefaultSharedPreferences(_context)
          .edit().putString(PREF_GRID_ORDER, sb.toString()).apply();
    }
    catch (Throwable ignored) {}
  }

  public void activateTool(Context context, Keyboard2 keyboard, String toolId)
  {
    if (keyboard == null || toolId == null) return;
    try
    {
      switch (toolId)
      {
        case "ai":
          if (keyboard.isAiPaneVisible()) keyboard.closeAiPane();
          else keyboard.showAiPane();
          break;
        case "autofill":
          PasswordAutofillHelper.showPasswordMenu(context, keyboard, null);
          break;
        case "translate":
          if (keyboard.isTranslateBarOpen()) keyboard.hideTranslateBar();
          else keyboard.showTranslateBar();
          break;
        case "clipboard":
          keyboard.handle_event_key(KeyValue.Event.SWITCH_CLIPBOARD);
          break;
        case "fancy":
          if (keyboard.isFancyPaneVisible()) keyboard.closeFancyPane();
          else keyboard.showFancyPane();
          break;
        case "mic":
          keyboard.handle_event_key(KeyValue.Event.SWITCH_VOICE_TYPING);
          break;
        case "theme":
          Intent themeIntent = new Intent(context, ThemeCustomizerActivity.class);
          themeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          context.startActivity(themeIntent);
          break;
        case "settings":
          Intent settingsIntent = new Intent(context, SettingsActivity.class);
          settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          context.startActivity(settingsIntent);
          break;
        case "snippets":
          Intent snippetIntent = new Intent(context, SnippetManagerActivity.class);
          snippetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          context.startActivity(snippetIntent);
          break;
        case "dev":
          Config cfg = Config.globalConfig();
          if (cfg != null)
          {
            cfg.developer_mode = !cfg.developer_mode;
            try
            {
              SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
              defPrefs.edit().putBoolean("developer_mode", cfg.developer_mode).apply();

              SharedPreferences protPrefs = DirectBootAwarePreferences.get_shared_preferences(context);
              protPrefs.edit().putBoolean("developer_mode", cfg.developer_mode).apply();

              DirectBootAwarePreferences.copy_preferences_to_protected_storage(context, defPrefs);
            }
            catch (Throwable ignored) {}

            if (keyboard != null)
            {
              try
              {
                keyboard.onSharedPreferenceChanged(DirectBootAwarePreferences.get_shared_preferences(context), "developer_mode");
              }
              catch (Throwable ignored) {}
            }

            String msg = cfg.developer_mode ? "Developer Mode: ON" : "Developer Mode: OFF";
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            if (keyboard != null) keyboard.refresh_candidates_view();
            notifyListeners();
          }
          break;
        case "one_hand":
          keyboard.toggleOneHandMode();
          boolean isNowOneHand = keyboard.isOneHandModeActive();
          String oneHandMsg = isNowOneHand ? "One-Handed Mode: ON" : "One-Handed Mode: OFF";
          Toast.makeText(context, oneHandMsg, Toast.LENGTH_SHORT).show();
          notifyListeners();
          break;
        case "haptic":
          Config cfgHaptic = Config.globalConfig();
          if (cfgHaptic != null)
          {
            cfgHaptic.vibrate_enabled = !cfgHaptic.vibrate_enabled;
            try
            {
              SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
              defPrefs.edit().putBoolean("vibrate_enabled", cfgHaptic.vibrate_enabled).apply();

              SharedPreferences protPrefs = DirectBootAwarePreferences.get_shared_preferences(context);
              protPrefs.edit().putBoolean("vibrate_enabled", cfgHaptic.vibrate_enabled).apply();

              DirectBootAwarePreferences.copy_preferences_to_protected_storage(context, defPrefs);
            }
            catch (Throwable ignored) {}

            if (keyboard != null)
            {
              try
              {
                keyboard.onSharedPreferenceChanged(DirectBootAwarePreferences.get_shared_preferences(context), "vibrate_enabled");
              }
              catch (Throwable ignored) {}
            }

            if (cfgHaptic.vibrate_enabled && keyboard != null && keyboard.getKeyboardView() != null)
            {
              typodev.keyboard.VibratorCompat.vibrate(keyboard.getKeyboardView(), cfgHaptic);
            }
            String msg = cfgHaptic.vibrate_enabled ? "Haptic Feedback: ON" : "Haptic Feedback: OFF";
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            notifyListeners();
          }
          break;
        case "sound":
          Config cfgSound = Config.globalConfig();
          if (cfgSound != null)
          {
            cfgSound.sound_on_keypress = !cfgSound.sound_on_keypress;
            if (cfgSound.sound_volume <= 0)
            {
              cfgSound.sound_volume = 60;
            }
            try
            {
              SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
              defPrefs.edit()
                  .putBoolean("sound_on_keypress", cfgSound.sound_on_keypress)
                  .putInt("sound_volume", cfgSound.sound_volume)
                  .apply();

              SharedPreferences protPrefs = DirectBootAwarePreferences.get_shared_preferences(context);
              protPrefs.edit()
                  .putBoolean("sound_on_keypress", cfgSound.sound_on_keypress)
                  .putInt("sound_volume", cfgSound.sound_volume)
                  .apply();

              DirectBootAwarePreferences.copy_preferences_to_protected_storage(context, defPrefs);
            }
            catch (Throwable ignored) {}

            if (keyboard != null)
            {
              try
              {
                keyboard.onSharedPreferenceChanged(DirectBootAwarePreferences.get_shared_preferences(context), "sound_on_keypress");
              }
              catch (Throwable ignored) {}
            }

            if (cfgSound.sound_on_keypress)
            {
              typodev.keyboard.SoundFeedbackManager.getInstance(context).play(KeyValue.ENTER, cfgSound);
            }
            String msg = cfgSound.sound_on_keypress ? "Sound Feedback: ON" : "Sound Feedback: OFF";
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            notifyListeners();
          }
          break;
        case "popup":
          Config cfgPopup = Config.globalConfig();
          if (cfgPopup != null)
          {
            cfgPopup.popup_on_keypress = !cfgPopup.popup_on_keypress;
            try
            {
              SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
              defPrefs.edit().putBoolean("popup_on_keypress", cfgPopup.popup_on_keypress).apply();

              SharedPreferences protPrefs = DirectBootAwarePreferences.get_shared_preferences(context);
              protPrefs.edit().putBoolean("popup_on_keypress", cfgPopup.popup_on_keypress).apply();

              DirectBootAwarePreferences.copy_preferences_to_protected_storage(context, defPrefs);
            }
            catch (Throwable ignored) {}

            if (keyboard != null)
            {
              try
              {
                keyboard.onSharedPreferenceChanged(DirectBootAwarePreferences.get_shared_preferences(context), "popup_on_keypress");
              }
              catch (Throwable ignored) {}
            }

            String msg = cfgPopup.popup_on_keypress ? "Key Popup: ON" : "Key Popup: OFF";
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            notifyListeners();
          }
          break;
      }
    }
    catch (Throwable t)
    {
      Logs.print_exception(t);
    }
  }
}
