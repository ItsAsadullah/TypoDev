package typodev.keyboard.clipboard;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import typodev.keyboard.R;

/** Permission and picker UI is hosted outside the IME window. */
public final class ClipboardImageActivity extends Activity {
  private android.net.Uri importing;
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    if (state != null) {
      String savedUri = state.getString("importing_uri");
      if (savedUri != null) importUri(android.net.Uri.parse(savedUri));
      return;
    }
    if (getIntent().getBooleanExtra("screenshots", false) || getIntent().getBooleanExtra("recent", false)) {
      String permission = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
      if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
        requestPermissions(Build.VERSION.SDK_INT >= 34 ? new String[]{permission, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"} : new String[]{permission}, 1);
      else enable();
    } else pick();
  }
  private void enable() {
    if (getIntent().getBooleanExtra("recent", false)) {
      ClipboardImageStore.get(this).importLatestScreenshot(success -> {
        Toast.makeText(this, success ? R.string.clipboard_image_saved : R.string.clipboard_recent_missing,
            Toast.LENGTH_LONG).show();
        if (success) finish(); else pick();
      });
      return;
    }
    ClipboardImageStore.get(this).enableScreenshots(true);
    Toast.makeText(this, R.string.clipboard_screenshots_enabled, Toast.LENGTH_LONG).show();
    finish();
  }
  private void pick() {
    try {
      Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*")
          .addCategory(Intent.CATEGORY_OPENABLE);
      startActivityForResult(picker, 2);
    } catch (RuntimeException e) {
      Toast.makeText(this, R.string.clipboard_image_failed, Toast.LENGTH_LONG).show(); finish();
    }
  }
  @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(request, permissions, results);
    if (request != 1) return;
    if (ClipboardImageStore.get(this).hasPhotoAccess()) enable();
    else {
      Toast.makeText(this, R.string.clipboard_screenshot_permission, Toast.LENGTH_LONG).show();
      pick();
    }
  }
  @Override public void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request != 2 || result != RESULT_OK || data == null || data.getData() == null) { finish(); return; }
    importUri(data.getData());
  }
  private void importUri(android.net.Uri uri) {
    importing = uri;
    ClipboardImageStore.get(this).importImage(uri, success -> {
      Toast.makeText(this, success ? R.string.clipboard_image_saved : R.string.clipboard_image_failed,
          Toast.LENGTH_LONG).show();
      finish();
    });
  }
  @Override public void onSaveInstanceState(Bundle state) {
    if (importing != null) state.putString("importing_uri", importing.toString());
    super.onSaveInstanceState(state);
  }
}
