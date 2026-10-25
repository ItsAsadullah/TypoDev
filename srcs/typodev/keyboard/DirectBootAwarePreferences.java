package typodev.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public final class DirectBootAwarePreferences
{
  public static boolean isUserUnlocked(Context context)
  {
    if (VERSION.SDK_INT < 24) return true;
    android.os.UserManager manager = (android.os.UserManager)context.getSystemService(Context.USER_SERVICE);
    return manager != null && manager.isUserUnlocked();
  }

  /* On API >= 24, preferences are read from the device protected storage. This
   * storage is less protected than the default, no personnal or sensitive
   * information is stored there (only the keyboard settings). This storage is
   * accessible during boot and allow the keyboard to read its settings and
   * allow typing the storage password. */
  public static SharedPreferences get_shared_preferences(Context context)
  {
    if (VERSION.SDK_INT < 24)
      return PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences prefs = get_protected_prefs(context);
    check_need_migration(context, prefs);
    return prefs;
  }

  /* Copy shared preferences to device protected storage. Not using
   * [Context.moveSharedPreferencesFrom] because the settings activity still
   * use [PreferenceActivity], which can't work on a non-default shared
   * preference file. */
  public static void copy_preferences_to_protected_storage(Context context,
      SharedPreferences src)
  {
    if (VERSION.SDK_INT >= 24)
      copy_shared_preferences(src, get_protected_prefs(context));
  }

  /** Load the default preferences. */
  static SharedPreferences get_protected_prefs(Context context)
  {
    String pref_name = context.getPackageName() + "_preferences";
    return get_protected_prefs(context, pref_name);
  }

  /** Load the specified preferences. */
  public static SharedPreferences get_protected_prefs(Context context,
      String pref_name)
  {
    if (VERSION.SDK_INT >= 24)
      context = context.createDeviceProtectedStorageContext();
    return context.getSharedPreferences(pref_name, Context.MODE_PRIVATE);
  }

  static void check_need_migration(Context app_context,
      SharedPreferences protected_prefs)
  {
    if (!protected_prefs.getBoolean("need_migration", true))
      return;
    SharedPreferences prefs;
    try
    {
      prefs = PreferenceManager.getDefaultSharedPreferences(app_context);
    }
    catch (Exception e)
    {
      // Device is locked, migrate later.
      return;
    }
    prefs.edit().putBoolean("need_migration", false).apply();
    copy_shared_preferences(prefs, protected_prefs);
  }

  private static boolean isProtectedRuntimeKey(String k)
  {
    return "current_layout_portrait".equals(k)
        || "current_layout_landscape".equals(k)
        || "version".equals(k);
  }

  static void copy_shared_preferences(SharedPreferences src, SharedPreferences dst)
  {
    SharedPreferences.Editor e = dst.edit();
    Map<String, ?> entries = src.getAll();
    Map<String, ?> dstEntries = dst.getAll();
    boolean changed = false;
    // Mirror removals too: otherwise reset settings and deleted symbol mappings
    // survive indefinitely in the keyboard's device-protected copy.
    // Never remove runtime layout state keys that are persisted directly to protected prefs.
    for (String k : dstEntries.keySet())
    {
      if (!entries.containsKey(k) && !isProtectedRuntimeKey(k))
      {
        e.remove(k);
        changed = true;
      }
    }
    for (String k : entries.keySet())
    {
      Object v = entries.get(k);
      Object existing = dstEntries.get(k);
      if (java.util.Objects.equals(v, existing))
        continue;
      if (v instanceof Boolean)
      {
        e.putBoolean(k, (Boolean)v);
        changed = true;
      }
      else if (v instanceof Float)
      {
        e.putFloat(k, (Float)v);
        changed = true;
      }
      else if (v instanceof Integer)
      {
        e.putInt(k, (Integer)v);
        changed = true;
      }
      else if (v instanceof Long)
      {
        e.putLong(k, (Long)v);
        changed = true;
      }
      else if (v instanceof String)
      {
        e.putString(k, (String)v);
        changed = true;
      }
      else if (v instanceof Set)
      {
        e.putStringSet(k, new HashSet<String>((Set<String>)v));
        changed = true;
      }
    }
    if (changed)
      e.apply();
  }
}
