package typodev.keyboard;

import android.app.Activity;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.annotation.Config;
import typodev.keyboard.ai.AiVisionHelper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AiVisionRegressionTest {
  private Activity activity;
  private void supply(MatrixCursor cursor) {
    ShadowContentResolver.registerProviderInternal("media", new android.content.ContentProvider() {
      @Override public boolean onCreate() { return true; }
      @Override public android.database.Cursor query(Uri uri, String[] columns, String selection, String[] args, String order) { return cursor; }
      @Override public String getType(Uri uri) { return "image/png"; }
      @Override public Uri insert(Uri uri, android.content.ContentValues values) { return null; }
      @Override public int delete(Uri uri, String selection, String[] args) { return 0; }
      @Override public int update(Uri uri, android.content.ContentValues values, String selection, String[] args) { return 0; }
    });
  }
  @Before public void setup() { activity = Robolectric.buildActivity(Activity.class).setup().get(); }
  private void gallery(Object[]... rows) {
    MatrixCursor cursor = new MatrixCursor(new String[]{"_id", "date_added", "_display_name", "relative_path"});
    for (Object[] row : rows) cursor.addRow(row);
    supply(cursor);
  }
  @Test public void newestPhotoIsNotMistakenForScreenshot() {
    gallery(new Object[]{1L, System.currentTimeMillis() / 1000, "IMG_1.jpg", "DCIM/Camera/"});
    assertNull(AiVisionHelper.getLatestScreenshotUri(activity));
  }
  @Test public void screenshotFolderIsRecognizedPastTenNewerPhotos() {
    MatrixCursor cursor = new MatrixCursor(new String[]{"_id", "date_added", "_display_name", "relative_path"});
    for (int i = 0; i < 12; i++) cursor.addRow(new Object[]{i, 1000L, "IMG.jpg", "DCIM/Camera/"});
    cursor.addRow(new Object[]{99L, 900L, "20261011.png", "Pictures/Screenshots/"});
    supply(cursor);
    assertEquals(Uri.parse("content://media/external/images/media/99"), AiVisionHelper.getLatestScreenshotUri(activity));
  }
  @Test public void narrowImagesAreStillDownscaledAndInvalidDimensionsRejected() {
    Bitmap original = Bitmap.createBitmap(1, 5000, Bitmap.Config.ARGB_8888);
    assertNull(AiVisionHelper.scaleAndCompressBitmap(original, 0, 80));
    assertNull(AiVisionHelper.getBytesFromUri(activity, Uri.parse("content://media/1"), -1));
    byte[] jpeg = AiVisionHelper.scaleAndCompressBitmap(original, 100, 80);
    android.graphics.BitmapFactory.Options dimensions = new android.graphics.BitmapFactory.Options();
    dimensions.inJustDecodeBounds = true;
    android.graphics.BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, dimensions);
    assertEquals(1, dimensions.outWidth); assertEquals(100, dimensions.outHeight);
    assertFalse(original.isRecycled()); original.recycle();
  }
}
