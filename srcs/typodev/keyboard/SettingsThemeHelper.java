package typodev.keyboard;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.view.Window;

public final class SettingsThemeHelper
{
  public static boolean isNightMode(Activity activity)
  {
    try
    {
      if (activity == null || activity.getResources() == null || activity.getResources().getConfiguration() == null)
      {
        return false;
      }
      int nightModeFlags = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
      return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    catch (Throwable ignored)
    {
      return false;
    }
  }

  public static void applyAdaptiveSystemBars(Activity activity)
  {
    if (activity == null) return;
    try
    {
      applyAdaptiveSystemBars(activity, activity.getWindow());
    }
    catch (Throwable ignored) {}
  }

  public static void applyAdaptiveSystemBars(Activity activity, Window window)
  {
    if (activity == null || window == null || Build.VERSION.SDK_INT < 21) return;

    try
    {
      boolean isNight = isNightMode(activity);

      if (isNight)
      {
        window.setStatusBarColor(0xFF080C16);
        window.setNavigationBarColor(0xFF080C16);
        if (Build.VERSION.SDK_INT >= 23)
        {
          View decor = window.getDecorView();
          if (decor != null)
          {
            int flags = decor.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26)
            {
              flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decor.setSystemUiVisibility(flags);
          }
        }
      }
      else
      {
        window.setStatusBarColor(0xFFFFFFFF);
        window.setNavigationBarColor(0xFFFFFFFF);
        if (Build.VERSION.SDK_INT >= 23)
        {
          View decor = window.getDecorView();
          if (decor != null)
          {
            int flags = decor.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26)
            {
              flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decor.setSystemUiVisibility(flags);
          }
        }
      }
    }
    catch (Throwable ignored) {}
  }
}
