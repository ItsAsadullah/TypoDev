package typodev.keyboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Lightweight GitHub Releases update checker for TypoDev Keyboard.
 * Automatically checks at most once every 12 hours when opening the app/settings
 * and supports manual "Check for Updates" from Settings -> About.
 */
public final class AppUpdateChecker
{
  public static final String GITHUB_RELEASES_API =
      "https://api.github.com/repos/ItsAsadullah/TypoDev/releases/latest";
  public static final String GITHUB_RELEASES_WEB =
      "https://github.com/ItsAsadullah/TypoDev/releases/latest";

  private static final String PREF_LAST_CHECK_MS = "app_update_last_check_ms";
  private static final String PREF_SKIPPED_VERSION = "app_update_skipped_version";
  private static final long AUTO_CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L; // 12 hours

  private static final ExecutorService _executor = Executors.newSingleThreadExecutor();
  private static final Handler _mainHandler = new Handler(Looper.getMainLooper());

  private AppUpdateChecker() {}

  public static final class ReleaseInfo
  {
    public final String version;
    public final String title;
    public final String notes;
    public final String downloadUrl;
    public final String releasePageUrl;

    public ReleaseInfo(String version, String title, String notes, String downloadUrl, String releasePageUrl)
    {
      this.version = version;
      this.title = title;
      this.notes = notes;
      this.downloadUrl = downloadUrl;
      this.releasePageUrl = releasePageUrl;
    }
  }

  public static String getCurrentVersionName(Context context)
  {
    if (context == null) return "2.1.7";
    try
    {
      PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      if (info != null && info.versionName != null && !info.versionName.trim().isEmpty())
      {
        return normalizeVersion(info.versionName);
      }
    }
    catch (Throwable ignored) {}
    return "2.1.7";
  }

  public static String normalizeVersion(String raw)
  {
    if (raw == null) return "";
    String clean = raw.trim();
    if (clean.startsWith("v") || clean.startsWith("V"))
    {
      clean = clean.substring(1).trim();
    }
    int dash = clean.indexOf('-');
    if (dash > 0)
    {
      clean = clean.substring(0, dash).trim();
    }
    return clean;
  }

  public static boolean isNewerVersion(String remoteRaw, String currentRaw)
  {
    String remote = normalizeVersion(remoteRaw);
    String current = normalizeVersion(currentRaw);
    if (remote.isEmpty() || current.isEmpty()) return false;

    String[] rParts = remote.split("\\.");
    String[] cParts = current.split("\\.");
    int maxLen = Math.max(rParts.length, cParts.length);
    for (int i = 0; i < maxLen; i++)
    {
      int rVal = i < rParts.length ? parseDigits(rParts[i]) : 0;
      int cVal = i < cParts.length ? parseDigits(cParts[i]) : 0;
      if (rVal > cVal) return true;
      if (rVal < cVal) return false;
    }
    return false;
  }

  private static int parseDigits(String part)
  {
    if (part == null || part.isEmpty()) return 0;
    StringBuilder digits = new StringBuilder();
    for (int i = 0; i < part.length(); i++)
    {
      char c = part.charAt(i);
      if (Character.isDigit(c)) digits.append(c);
      else break;
    }
    if (digits.length() == 0) return 0;
    try
    {
      return Integer.parseInt(digits.toString());
    }
    catch (NumberFormatException e)
    {
      return 0;
    }
  }

  public static ReleaseInfo parseLatestReleaseJson(String jsonText)
  {
    if (jsonText == null || jsonText.trim().isEmpty()) return null;
    try
    {
      JSONObject obj = new JSONObject(jsonText);
      if (obj.optBoolean("draft", false) || obj.optBoolean("prerelease", false))
      {
        return null;
      }
      String tagName = obj.optString("tag_name", "").trim();
      String version = normalizeVersion(tagName);
      if (version.isEmpty()) return null;

      String title = obj.optString("name", "").trim();
      if (title.isEmpty()) title = "TypoDev v" + version;

      String body = obj.optString("body", "").trim();
      String htmlUrl = obj.optString("html_url", GITHUB_RELEASES_WEB).trim();
      if (htmlUrl.isEmpty()) htmlUrl = GITHUB_RELEASES_WEB;

      String apkUrl = "";
      JSONArray assets = obj.optJSONArray("assets");
      if (assets != null)
      {
        for (int i = 0; i < assets.length(); i++)
        {
          JSONObject asset = assets.optJSONObject(i);
          if (asset == null) continue;
          String assetName = asset.optString("name", "").toLowerCase(java.util.Locale.ROOT);
          String dl = asset.optString("browser_download_url", "").trim();
          if (assetName.endsWith(".apk") && !dl.isEmpty())
          {
            apkUrl = dl;
            break;
          }
        }
      }
      if (apkUrl.isEmpty()) apkUrl = htmlUrl;
      return new ReleaseInfo(version, title, body, apkUrl, htmlUrl);
    }
    catch (Exception e)
    {
      return null;
    }
  }

  public static void checkAutomatically(final Activity activity)
  {
    if (activity == null || activity.isFinishing()) return;
    if ("robolectric".equalsIgnoreCase(Build.FINGERPRINT)) return;
    if (DirectBootAwarePreferences.isDirectBootLocked(activity)) return;
    if (!isNetworkConnected(activity)) return;

    final SharedPreferences prefs = getSafePrefs(activity);
    if (prefs == null) return;
    long now = System.currentTimeMillis();
    long lastCheck = prefs.getLong(PREF_LAST_CHECK_MS, 0L);
    if (now - lastCheck < AUTO_CHECK_INTERVAL_MS) return;

    prefs.edit().putLong(PREF_LAST_CHECK_MS, now).apply();
    runCheckAsync(activity, false);
  }

  public static void checkManually(final Activity activity)
  {
    if (activity == null || activity.isFinishing()) return;
    if (!isNetworkConnected(activity))
    {
      Toast.makeText(activity, "No internet connection", Toast.LENGTH_SHORT).show();
      return;
    }
    Toast.makeText(activity, "Checking for updates...", Toast.LENGTH_SHORT).show();
    SharedPreferences prefs = getSafePrefs(activity);
    if (prefs != null)
    {
      prefs.edit().putLong(PREF_LAST_CHECK_MS, System.currentTimeMillis()).apply();
    }
    runCheckAsync(activity, true);
  }

  private static void runCheckAsync(final Activity activity, final boolean manual)
  {
    final Context appContext = activity.getApplicationContext();
    final String currentVersion = getCurrentVersionName(appContext);

    _executor.execute(new Runnable()
    {
      @Override
      public void run()
      {
        HttpURLConnection conn = null;
        try
        {
          conn = (HttpURLConnection) new URL(GITHUB_RELEASES_API).openConnection();
          conn.setRequestMethod("GET");
          conn.setRequestProperty("Accept", "application/vnd.github+json");
          conn.setRequestProperty("User-Agent", "TypoDev-Keyboard/" + currentVersion);
          conn.setConnectTimeout(6000);
          conn.setReadTimeout(8000);

          int code = conn.getResponseCode();
          if (code != 200)
          {
            if (manual)
            {
              postToast(activity, "Could not check updates (HTTP " + code + ")");
            }
            return;
          }

          String json = readStream(conn.getInputStream());
          final ReleaseInfo release = parseLatestReleaseJson(json);
          if (release == null)
          {
            if (manual)
            {
              postToast(activity, "You are on the latest version (v" + currentVersion + ")");
            }
            return;
          }

          if (!isNewerVersion(release.version, currentVersion))
          {
            if (manual)
            {
              postToast(activity, "TypoDev is up to date (v" + currentVersion + ")");
            }
            return;
          }

          if (!manual)
          {
            SharedPreferences prefs = getSafePrefs(appContext);
            String skipped = prefs != null ? prefs.getString(PREF_SKIPPED_VERSION, "") : "";
            if (release.version.equals(skipped))
            {
              return;
            }
          }

          _mainHandler.post(new Runnable()
          {
            @Override
            public void run()
            {
              showUpdateDialog(activity, currentVersion, release, manual);
            }
          });
        }
        catch (Exception e)
        {
          if (manual)
          {
            postToast(activity, "Update check failed. Opening releases page...");
            _mainHandler.post(new Runnable()
            {
              @Override
              public void run()
              {
                openUrlSafely(activity, GITHUB_RELEASES_WEB);
              }
            });
          }
        }
        finally
        {
          if (conn != null) conn.disconnect();
        }
      }
    });
  }

  private static void showUpdateDialog(
      final Activity activity,
      final String currentVersion,
      final ReleaseInfo release,
      final boolean manual)
  {
    if (activity == null || activity.isFinishing()) return;
    if (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed()) return;

    StringBuilder msg = new StringBuilder();
    msg.append("New version available: v").append(release.version)
        .append(" (installed: v").append(currentVersion).append(")");
    if (release.notes != null && !release.notes.isEmpty())
    {
      String trimmedNotes = release.notes.length() > 600
          ? release.notes.substring(0, 600) + "..."
          : release.notes;
      msg.append("\n\n").append(trimmedNotes);
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity)
        .setTitle("Update Available — " + release.title)
        .setMessage(msg.toString())
        .setPositiveButton("Download APK", new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(DialogInterface dialog, int which)
          {
            openUrlSafely(activity, release.downloadUrl);
          }
        })
        .setNegativeButton("Later", null);

    if (!manual)
    {
      builder.setNeutralButton("Skip", new DialogInterface.OnClickListener()
      {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
          SharedPreferences prefs = getSafePrefs(activity);
          if (prefs != null)
          {
            prefs.edit().putString(PREF_SKIPPED_VERSION, release.version).apply();
          }
        }
      });
    }

    try
    {
      builder.show();
    }
    catch (Throwable ignored) {}
  }

  private static void openUrlSafely(Context context, String url)
  {
    try
    {
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
      if (!(context instanceof Activity))
      {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      }
      context.startActivity(intent);
    }
    catch (Throwable ignored) {}
  }

  private static void postToast(final Activity activity, final String message)
  {
    _mainHandler.post(new Runnable()
    {
      @Override
      public void run()
      {
        if (activity != null && !activity.isFinishing())
        {
          Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        }
      }
    });
  }

  private static boolean isNetworkConnected(Context context)
  {
    try
    {
      ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      if (cm == null) return false;
      NetworkInfo info = cm.getActiveNetworkInfo();
      return info != null && info.isConnected();
    }
    catch (Throwable t)
    {
      return false;
    }
  }

  private static SharedPreferences getSafePrefs(Context context)
  {
    if (context == null || DirectBootAwarePreferences.isDirectBootLocked(context)) return null;
    try
    {
      return PreferenceManager.getDefaultSharedPreferences(context);
    }
    catch (Throwable t)
    {
      return null;
    }
  }

  private static String readStream(InputStream is) throws Exception
  {
    if (is == null) return "";
    StringBuilder sb = new StringBuilder();
    BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    try
    {
      String line;
      while ((line = br.readLine()) != null)
      {
        sb.append(line).append('\n');
      }
    }
    finally
    {
      try { br.close(); } catch (Exception ignored) {}
    }
    return sb.toString();
  }
}
