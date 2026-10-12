package typodev.keyboard.clipboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;
import typodev.keyboard.Config;

/** Private durable copies: pins survive restarts and revoked gallery URI grants. */
public final class ClipboardImageStore {
  public interface AnalysisCallback { void onComplete(byte[] jpeg); }
  public interface ImportCallback { void onComplete(boolean success); }
  private static ClipboardImageStore instance;
  private final Context context;
  private SharedPreferences prefs;
  private File directory;
  private final ClipboardImageIndex index = new ClipboardImageIndex("[]");
  private final List<JSONObject> entries = index.entries;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler main = new Handler(Looper.getMainLooper());
  private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
  private final java.util.Map<String, List<ImportCallback>> pending = new java.util.HashMap<>();
  private final Runnable scan = () -> scanScreenshots();

  public static synchronized ClipboardImageStore get(Context context) {
    if (instance == null) instance = new ClipboardImageStore(context.getApplicationContext());
    instance.initialize();
    return instance;
  }

  private ClipboardImageStore(Context context) {
    this.context = context;
  }

  private synchronized boolean initialize() {
    if (prefs != null) return true;
    // The IME also starts on the lock screen before credential storage is available.
    if (Build.VERSION.SDK_INT >= 24) {
      android.os.UserManager user = (android.os.UserManager) context.getSystemService(Context.USER_SERVICE);
      if (user != null && !user.isUserUnlocked()) return false;
    }
    prefs = context.getSharedPreferences("clipboard_images", Context.MODE_PRIVATE);
    directory = new File(context.getFilesDir(), "clipboard_images");
    directory.mkdirs();
    entries.addAll(new ClipboardImageIndex(prefs.getString("entries", "[]")).entries);
    for (int i = entries.size() - 1; i >= 0; i--)
      if (!validFile(entries.get(i).optString("file"))) entries.remove(i);
    try { context.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        true, new ContentObserver(main) {
          @Override public void onChange(boolean selfChange) {
            main.removeCallbacks(scan);
            main.postDelayed(scan, 1500);
          }
        }); } catch (RuntimeException ignored) { }
    return true;
  }

  private boolean validFile(String name) {
    return ClipboardImageIndex.validName(name) && new File(directory, name).isFile();
  }

  private Uri uri(String name) {
    return FileProvider.getUriForFile(context, context.getPackageName() + ".clipboard.images",
        new File(directory, name));
  }

  public synchronized List<ClipboardItem> items() {
    List<ClipboardItem> result = new ArrayList<>();
    if (!initialize()) return result;
    long now = System.currentTimeMillis();
    Config config = Config.globalConfig();
    long ttl = (config == null ? 30 : config.clipboard_history_duration) * 60000L;
    for (int i = entries.size() - 1; i >= 0; i--) {
      JSONObject e = entries.get(i);
      if (ClipboardImageIndex.expired(e, now, ttl)) removeAt(i);
    }
    for (JSONObject e : entries) result.add(new ClipboardItem("[Image]", e.optLong("time"),
        e.optBoolean("pinned"), true, uri(e.optString("file")).toString()));
    return result;
  }

  /** Read the selected private image on the existing worker; no gallery or screen capture permission. */
  public void readForAnalysis(String key, AnalysisCallback callback) {
    worker.execute(() -> {
      Uri selected;
      synchronized (this) {
        selected = initialize() && find(key) != null ? Uri.parse(key) : null;
      }
      byte[] jpeg = selected == null ? null
          : typodev.keyboard.ai.AiVisionHelper.getBytesFromUri(context, selected, 1024);
      main.post(() -> callback.onComplete(jpeg));
    });
  }

  public synchronized boolean contains(String key) { return find(key) != null; }
  private JSONObject find(String key) {
    for (JSONObject e : entries) if (uri(e.optString("file")).toString().equals(key)) return e;
    return null;
  }

  public synchronized void pin(String key, boolean pinned) {
    JSONObject e = find(key);
    if (e == null) return;
    index.pin(e.optString("file"), pinned, System.currentTimeMillis());
    save(); changed();
  }

  public synchronized void delete(String key) {
    JSONObject e = find(key);
    if (e != null) { removeAt(entries.indexOf(e)); changed(); }
  }

  public synchronized void clearRecent() {
    for (int i = entries.size() - 1; i >= 0; i--)
      if (!entries.get(i).optBoolean("pinned")) removeAt(i);
    changed();
  }

  private void removeAt(int i) {
    JSONObject e = entries.remove(i);
    new File(directory, e.optString("file")).delete();
    new File(directory, e.optString("file") + ".png").delete();
    save();
  }
  private void save() { if (prefs != null) prefs.edit().putString("entries", index.serialize()).apply(); }
  public void addListener(Runnable listener) { listeners.add(listener); }
  public void removeListener(Runnable listener) { listeners.remove(listener); }
  private void changed() { for (Runnable listener : listeners) main.post(listener); }

  public synchronized Uri thumbnail(String key) {
    JSONObject e = find(key);
    return e == null ? null : Uri.fromFile(new File(directory, e.optString("file") + ".png"));
  }

  public synchronized String recentSuggestion(long textTimestamp) {
    if (!initialize()) return null;
    long time = prefs.getLong("suggestion_time", 0);
    String file = prefs.getString("suggestion_file", "");
    if (time < textTimestamp || System.currentTimeMillis() - time > 300000
        || prefs.getLong("dismissed_suggestion_time", 0) == time || !validFile(file)) return null;
    return uri(file).toString();
  }
  public synchronized void dismissSuggestion(String key) {
    if (initialize() && key != null && key.equals(uri(prefs.getString("suggestion_file", "")).toString()))
      prefs.edit().putLong("dismissed_suggestion_time", prefs.getLong("suggestion_time", 0)).apply();
  }
  private void suggest(String file) {
    prefs.edit().putString("suggestion_file", file).putLong("suggestion_time", System.currentTimeMillis()).apply();
  }

  public void importImage(Uri source, ImportCallback callback) {
    if (source == null || !initialize()) {
      if (callback != null) main.post(() -> callback.onComplete(false));
      return;
    }
    String key = source.toString();
    synchronized (this) {
      if (contains(key)) {
        if (callback != null) main.post(() -> callback.onComplete(true));
        return;
      }
      List<ImportCallback> waiting = pending.get(key);
      if (waiting != null) {
        if (callback != null) waiting.add(callback);
        return;
      }
      waiting = new ArrayList<>();
      if (callback != null) waiting.add(callback);
      pending.put(key, waiting);
    }
    worker.execute(() -> {
      boolean success = copyImage(source);
      List<ImportCallback> waiting;
      synchronized (this) {
        waiting = pending.remove(key);
        if (success) prefs.edit().putBoolean("seen:" + key, true).apply();
      }
      if (success) changed();
      for (ImportCallback completion : waiting) main.post(() -> completion.onComplete(success));
    });
  }

  public void importCopiedImage(Uri source) { importCopiedImage(source, false); }
  public void importCopiedImage(Uri source, boolean freshCopy) {
    if (source != null && initialize() && (freshCopy || !prefs.getBoolean("seen:" + source.toString(), false))) importImage(source, null);
  }

  public void importCapture(byte[] jpeg, ImportCallback callback) {
    if (!initialize() || jpeg == null || jpeg.length == 0) {
      if (callback != null) main.post(() -> callback.onComplete(false)); return;
    }
    worker.execute(() -> {
      File source = null; boolean saved = false;
      try {
        source = File.createTempFile("capture", ".jpg", directory);
        try (FileOutputStream output = new FileOutputStream(source)) { output.write(jpeg); }
        saved = copyImage(Uri.fromFile(source));
      } catch (Exception ignored) { }
      finally { if (source != null) source.delete(); }
      if (saved) changed();
      boolean success = saved;
      if (callback != null) main.post(() -> callback.onComplete(success));
    });
  }

  public boolean hasPhotoAccess() {
    if (Build.VERSION.SDK_INT < 23) return true;
    String permission = Build.VERSION.SDK_INT >= 33 ? android.Manifest.permission.READ_MEDIA_IMAGES : android.Manifest.permission.READ_EXTERNAL_STORAGE;
    return context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        || (Build.VERSION.SDK_INT >= 34 && context.checkSelfPermission("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
            == android.content.pm.PackageManager.PERMISSION_GRANTED);
  }

  private boolean copyImage(Uri source) {
    File temporary = null;
    Bitmap thumbnail = null;
    try {
      temporary = File.createTempFile("import", ".tmp", directory);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream input = context.getContentResolver().openInputStream(source);
           FileOutputStream output = new FileOutputStream(temporary)) {
        if (input == null) return false;
        byte[] buffer = new byte[8192]; int count; long total = 0;
        while ((count = input.read(buffer)) != -1) {
          total += count;
          if (total > 25L * 1024 * 1024) return false;
          digest.update(buffer, 0, count); output.write(buffer, 0, count);
        }
      }
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(temporary.toString(), options);
      if (options.outWidth <= 0 || options.outHeight <= 0) return false;
      options.inJustDecodeBounds = false;
      options.inSampleSize = 1;
      while (options.outWidth / options.inSampleSize > 320 || options.outHeight / options.inSampleSize > 320)
        options.inSampleSize *= 2;
      thumbnail = BitmapFactory.decodeFile(temporary.toString(), options);
      if (thumbnail == null) return false;
      StringBuilder hash = new StringBuilder();
      for (byte b : digest.digest()) hash.append(String.format(Locale.ROOT, "%02x", b & 255));
      String mime = options.outMimeType;
      String extension = "image/jpeg".equals(mime) ? ".jpg" : "image/webp".equals(mime) ? ".webp"
          : "image/gif".equals(mime) ? ".gif" : "image/png".equals(mime) ? ".png" : null;
      if (extension == null) return false;
      String name = hash + extension;
      synchronized (this) {
        for (JSONObject e : entries) if (name.equals(e.optString("file"))) {
          suggest(name);
          index.touch(name, System.currentTimeMillis()); save();
          return true;
        }
        File destination = new File(directory, name);
        if (!temporary.renameTo(destination)) return false;
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
          thumbnail.compress(Bitmap.CompressFormat.PNG, 100, output);
        }
        JSONObject e = new JSONObject();
        e.put("file", name); e.put("mime", options.outMimeType);
        e.put("time", System.currentTimeMillis()); e.put("pinned", false);
        entries.add(0, e);
        suggest(name);
        int recent = 0;
        for (int i = 0; i < entries.size(); i++) if (!entries.get(i).optBoolean("pinned") && ++recent > 30) {
          removeAt(i--);
        }
        save();
      }
      return true;
    } catch (Exception | OutOfMemoryError ignored) { return false; }
    finally { if (temporary != null) temporary.delete(); if (thumbnail != null) thumbnail.recycle(); }
  }

  public synchronized String mimeType(String key) {
    JSONObject e = find(key);
    return e == null ? "image/png" : e.optString("mime", "image/png");
  }

  public boolean screenshotsEnabled() { return initialize() && prefs.getBoolean("screenshots", false); }
  public void importLatestScreenshot(ImportCallback callback) {
    if (!initialize() || !hasPhotoAccess()) {
      main.post(() -> callback.onComplete(false)); return;
    }
    worker.execute(() -> {
      boolean success = false;
      String path = Build.VERSION.SDK_INT >= 29 ? MediaStore.Images.Media.RELATIVE_PATH : MediaStore.Images.Media.DATA;
      try (Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          new String[]{"_id", "_display_name", path}, Build.VERSION.SDK_INT >= 29 ? "is_pending=0" : null,
          null, "date_added DESC")) {
        int checked = 0;
        while (cursor != null && cursor.moveToNext() && checked++ < 500) {
          if (!isScreenshot(cursor.getString(1) + " " + cursor.getString(2))) continue;
          Uri source = android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0));
          success = copyImage(source);
          if (success) prefs.edit().putBoolean("seen:" + source, true).apply();
          break;
        }
      } catch (RuntimeException ignored) { }
      if (success) changed();
      boolean saved = success;
      main.post(() -> callback.onComplete(saved));
    });
  }
  public void enableScreenshots(boolean enabled) {
    if (!initialize()) return;
    prefs.edit().putBoolean("screenshots", enabled).apply();
    changed();
    if (enabled) { scanScreenshots(); importLatestScreenshot(success -> { }); }
  }
  public void scanScreenshots() {
    if (!screenshotsEnabled() || !hasPhotoAccess()) return;
    worker.execute(() -> {
      if (!screenshotsEnabled()) return;
      String path = Build.VERSION.SDK_INT >= 29 ? MediaStore.Images.Media.RELATIVE_PATH : MediaStore.Images.Media.DATA;
      String selection = MediaStore.Images.Media.DATE_ADDED + ">=?";
      if (Build.VERSION.SDK_INT >= 29) selection += " AND is_pending=0";
      // Publishing a new screenshot may complete after another media insert.
      long since = Math.max(prefs.getLong("scan_time", 0) - 120, System.currentTimeMillis() / 1000 - 86400);
      long newest = since;
      try (Cursor c = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          new String[]{"_id", "_display_name", path, "date_added"}, selection,
          new String[]{Long.toString(since)}, "date_added DESC")) {
        int imported = 0;
        List<Uri> screenshots = new ArrayList<>();
        if (c != null) while (screenshotsEnabled() && c.moveToNext()) {
          long date = c.getLong(3); newest = Math.max(newest, date);
          String name = (c.getString(1) + " " + c.getString(2)).toLowerCase(Locale.ROOT);
          if (isScreenshot(name) && imported++ < 30) {
            Uri source = android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0));
            screenshots.add(0, source);
          }
        }
        boolean updated = false;
        // Import oldest first so the newest screenshot appears at the top.
        for (Uri source : screenshots) {
          if (!screenshotsEnabled()) break;
          String key = source.toString();
          if (!prefs.getBoolean("seen:" + key, false) && copyImage(source)) {
            prefs.edit().putBoolean("seen:" + key, true).apply(); updated = true;
          }
        }
        if (updated) changed();
        prefs.edit().putLong("scan_time", newest).apply();
      } catch (RuntimeException ignored) { /* Permission revoked or storage unavailable. */ }
    });
  }

  public static boolean isScreenshot(String name) {
    if (name == null) return false;
    String normalized = name.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
    return normalized.contains("screenshot") || normalized.contains("screencapture")
        || normalized.contains("স্ক্রিনশট") || normalized.contains("لقطةشاشة")
        || normalized.contains("capturadepantalla") || normalized.contains("capturadetela")
        || normalized.contains("截屏") || normalized.contains("截图") || normalized.contains("截圖")
        || normalized.contains("스크린샷");
  }
}
